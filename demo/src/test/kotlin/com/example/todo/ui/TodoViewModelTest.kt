package com.example.todo.ui

import com.example.todo.data.Task
import com.example.todo.data.TaskRepository
import com.example.todo.data.TaskStorageException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TodoViewModelTest {

    private class FakeRepository(
        initial: List<Task> = emptyList(),
        private val failOnLoad: Boolean = false,
    ) : TaskRepository {
        var stored: List<Task> = initial
        var saveCount = 0

        override suspend fun load(): List<Task> {
            if (failOnLoad) throw TaskStorageException("boom")
            return stored
        }

        override suspend fun save(tasks: List<Task>) {
            saveCount++
            stored = tasks
        }
    }

    private fun TestScope.createViewModel(repository: TaskRepository): TodoViewModel {
        var nextId = 0
        val dispatcher = StandardTestDispatcher(testScheduler)
        return TodoViewModel(
            repository = repository,
            scope = backgroundScope,
            ioDispatcher = dispatcher,
            clock = { 1_000L },
            idGenerator = { (++nextId).toString() },
        ).also { runCurrent() }
    }

    @Test
    fun `loads stored tasks on start`() = runTest {
        val existing = listOf(Task(id = "a", title = "Existing", createdAtEpochMillis = 0))
        val viewModel = createViewModel(FakeRepository(existing))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(existing, state.tasks)
    }

    @Test
    fun `addTask trims title, prepends task and persists`() = runTest {
        val repository = FakeRepository()
        val viewModel = createViewModel(repository)

        viewModel.addTask("  first  ")
        viewModel.addTask("second")
        runCurrent()

        assertEquals(listOf("second", "first"), viewModel.uiState.value.tasks.map { it.title })
        assertEquals(viewModel.uiState.value.tasks, repository.stored)
    }

    @Test
    fun `blank titles are ignored`() = runTest {
        val repository = FakeRepository()
        val viewModel = createViewModel(repository)

        viewModel.addTask("   ")
        runCurrent()

        assertTrue(viewModel.uiState.value.tasks.isEmpty())
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `toggle, delete and clearCompleted update tasks`() = runTest {
        val viewModel = createViewModel(FakeRepository())
        viewModel.addTask("a")
        viewModel.addTask("b")
        viewModel.addTask("c")

        viewModel.toggleTask("1")
        viewModel.toggleTask("2")
        assertEquals(2, viewModel.uiState.value.completedCount)

        viewModel.deleteTask("3")
        assertEquals(listOf("2", "1"), viewModel.uiState.value.tasks.map { it.id })

        viewModel.clearCompleted()
        assertTrue(viewModel.uiState.value.tasks.isEmpty())
    }

    @Test
    fun `filter controls visible tasks`() = runTest {
        val viewModel = createViewModel(FakeRepository())
        viewModel.addTask("a")
        viewModel.addTask("b")
        viewModel.toggleTask("1")

        viewModel.setFilter(TaskFilter.Active)
        assertEquals(listOf("b"), viewModel.uiState.value.visibleTasks.map { it.title })

        viewModel.setFilter(TaskFilter.Completed)
        assertEquals(listOf("a"), viewModel.uiState.value.visibleTasks.map { it.title })
    }

    @Test
    fun `flush writes the latest snapshot`() = runTest {
        val repository = FakeRepository()
        val viewModel = createViewModel(repository)

        repeat(5) { viewModel.addTask("task $it") }
        viewModel.flush()

        assertEquals(5, repository.stored.size)
    }

    @Test
    fun `load failure surfaces an error and leaves the list usable`() = runTest {
        val viewModel = createViewModel(FakeRepository(failOnLoad = true))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)

        viewModel.dismissError()
        viewModel.addTask("still works")
        assertEquals(1, viewModel.uiState.value.tasks.size)
    }
}
