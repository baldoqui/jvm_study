package com.example.todo.ui

import com.example.todo.data.Task

enum class TaskFilter(val label: String) {
    All("All"),
    Active("Active"),
    Completed("Completed"),
}

data class TodoUiState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.All,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val visibleTasks: List<Task>
        get() = when (filter) {
            TaskFilter.All -> tasks
            TaskFilter.Active -> tasks.filterNot { it.isCompleted }
            TaskFilter.Completed -> tasks.filter { it.isCompleted }
        }

    val completedCount: Int get() = tasks.count { it.isCompleted }
}
