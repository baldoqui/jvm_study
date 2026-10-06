package com.example.todo.data

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAtEpochMillis: Long,
)
