package com.example.todo.data

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JsonTaskRepositoryTest {
    private lateinit var dir: Path
    private lateinit var file: Path
    private lateinit var repository: JsonTaskRepository

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("todo-test")
        file = dir.resolve("nested").resolve(JsonTaskRepository.FILE_NAME)
        repository = JsonTaskRepository(file)
    }

    @AfterTest
    fun tearDown() {
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `load returns empty list when file does not exist`() = runTest {
        assertEquals(emptyList(), repository.load())
    }

    @Test
    fun `save then load round-trips tasks and creates parent directories`() = runTest {
        val tasks = listOf(
            Task(id = "1", title = "Write tests", isCompleted = true, createdAtEpochMillis = 10),
            Task(id = "2", title = "Ship it", createdAtEpochMillis = 20),
        )

        repository.save(tasks)

        assertEquals(tasks, JsonTaskRepository(file).load())
    }

    @Test
    fun `corrupted file is backed up and reported`() = runTest {
        Files.createDirectories(file.parent)
        file.writeText("{ not valid json")

        assertFailsWith<TaskStorageException> { repository.load() }

        val entries = file.parent.listDirectoryEntries().map { it.fileName.toString() }
        assertTrue(entries.any { it.startsWith("${JsonTaskRepository.FILE_NAME}.corrupt-") })
        assertEquals(emptyList(), repository.load())
    }
}
