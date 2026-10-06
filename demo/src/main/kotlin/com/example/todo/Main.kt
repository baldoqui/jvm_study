package com.example.todo

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.todo.data.JsonTaskRepository
import com.example.todo.ui.TodoScreen
import com.example.todo.ui.TodoViewModel
import com.example.todo.ui.theme.TodoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

fun main() {
    // The view model outlives individual compositions, so it gets its own scope bound to the app lifetime.
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val viewModel = TodoViewModel(repository = JsonTaskRepository(), scope = appScope)

    application {
        Window(
            onCloseRequest = {
                // Pending writes run on the IO dispatcher, so blocking the UI thread here cannot deadlock.
                runBlocking { viewModel.flush() }
                appScope.cancel()
                exitApplication()
            },
            title = "Todo Desktop",
            state = rememberWindowState(size = DpSize(560.dp, 760.dp)),
        ) {
            TodoTheme {
                TodoScreen(viewModel)
            }
        }
    }
}
