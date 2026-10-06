package com.example.todo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todo.ui.components.EmptyState
import com.example.todo.ui.components.NewTaskInput
import com.example.todo.ui.components.ProgressSummary
import com.example.todo.ui.components.TaskFilterBar
import com.example.todo.ui.components.TaskItem

private val MaxContentWidth = 720.dp

@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    val state by viewModel.uiState.collectAsState()

    TodoScreen(
        state = state,
        onAddTask = viewModel::addTask,
        onToggleTask = viewModel::toggleTask,
        onDeleteTask = viewModel::deleteTask,
        onClearCompleted = viewModel::clearCompleted,
        onFilterChange = viewModel::setFilter,
        onErrorShown = viewModel::dismissError,
    )
}

/** Stateless variant: takes plain state and callbacks, which keeps it previewable and testable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    state: TodoUiState,
    onAddTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onErrorShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tasks", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NewTaskInput(
                    onAddTask = onAddTask,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.tasks.isNotEmpty()) {
                    ProgressSummary(
                        completed = state.completedCount,
                        total = state.tasks.size,
                        onClearCompleted = onClearCompleted,
                    )
                }

                TaskFilterBar(selected = state.filter, onSelect = onFilterChange)

                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    state.visibleTasks.isEmpty() -> EmptyState(state.filter)

                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(state.visibleTasks, key = { it.id }) { task ->
                            TaskItem(
                                task = task,
                                onToggle = { onToggleTask(task.id) },
                                onDelete = { onDeleteTask(task.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}
