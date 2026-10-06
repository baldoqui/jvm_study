package com.example.todo.ui

import com.example.todo.data.Task
import com.example.todo.data.TaskRepository
import com.example.todo.data.TaskStorageException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.util.UUID

class TodoViewModel(
    private val repository: TaskRepository,
    private val scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    // Conflated: if several edits happen while a write is in flight, only the latest snapshot is written.
    private val pendingSaves = Channel<List<Task>>(Channel.CONFLATED)

    private val writer = scope.launch(ioDispatcher) {
        for (snapshot in pendingSaves) {
            try {
                repository.save(snapshot)
            } catch (e: TaskStorageException) {
                showError(e.message ?: "Could not save tasks")
            }
        }
    }

    init {
        scope.launch {
            val result = runCatching { repository.load() }
            _uiState.update {
                it.copy(
                    tasks = result.getOrDefault(emptyList()),
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.let { e -> e.message ?: "Could not load tasks" },
                )
            }
        }
    }

    fun addTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val task = Task(id = idGenerator(), title = trimmed, createdAtEpochMillis = clock())
        mutateTasks { listOf(task) + it }
    }

    fun toggleTask(id: String) = mutateTasks { tasks ->
        tasks.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it }
    }

    fun deleteTask(id: String) = mutateTasks { tasks -> tasks.filterNot { it.id == id } }

    fun clearCompleted() = mutateTasks { tasks -> tasks.filterNot { it.isCompleted } }

    fun setFilter(filter: TaskFilter) = _uiState.update { it.copy(filter = filter) }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    /** Writes any pending changes and stops accepting new ones. Call once, before the app exits. */
    suspend fun flush() {
        pendingSaves.close()
        writer.join()
    }

    private fun mutateTasks(transform: (List<Task>) -> List<Task>) {
        // Ignore edits until the initial load finishes, otherwise they would overwrite the stored file.
        if (_uiState.value.isLoading) return
        val updated = _uiState.updateAndGet { it.copy(tasks = transform(it.tasks)) }
        pendingSaves.trySend(updated.tasks)
    }

    private fun showError(message: String) = _uiState.update { it.copy(errorMessage = message) }
}
