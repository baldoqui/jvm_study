package com.example.todo.data

/** Persistence boundary for tasks. Implementations must be safe to call from any coroutine. */
interface TaskRepository {
    /** Returns all stored tasks, or an empty list when nothing has been saved yet. */
    suspend fun load(): List<Task>

    /** Replaces the stored tasks with [tasks]. */
    suspend fun save(tasks: List<Task>)
}

class TaskStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
