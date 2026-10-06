package com.example.todo.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** Stores tasks as a JSON array in a single file. */
class JsonTaskRepository(
    private val file: Path = AppDirectories.dataDir().resolve(FILE_NAME),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TaskRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Guards the file so overlapping load/save calls never interleave.
    private val mutex = Mutex()

    override suspend fun load(): List<Task> = withContext(ioDispatcher) {
        mutex.withLock {
            if (!file.exists()) return@withLock emptyList()

            val content = try {
                file.readText()
            } catch (e: IOException) {
                throw TaskStorageException("Could not read $file", e)
            }

            try {
                json.decodeFromString<List<Task>>(content)
            } catch (e: SerializationException) {
                // Keep the unreadable file around instead of silently overwriting the user's data.
                val backup = file.resolveSibling("${file.fileName}.corrupt-${System.currentTimeMillis()}")
                Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING)
                throw TaskStorageException("Task file was corrupted and has been moved to $backup", e)
            }
        }
    }

    override suspend fun save(tasks: List<Task>) = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                Files.createDirectories(file.parent)
                // Write to a temp file first so a crash mid-write cannot leave a truncated JSON file.
                val tmp = file.resolveSibling("${file.fileName}.tmp")
                tmp.writeText(json.encodeToString(tasks))
                moveReplacing(tmp, file)
            } catch (e: IOException) {
                throw TaskStorageException("Could not write $file", e)
            }
        }
    }

    private fun moveReplacing(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        const val FILE_NAME = "tasks.json"
    }
}
