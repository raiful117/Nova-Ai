package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val userMessage: String,
    val assistantResponse: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "bn",
    val status: String = "SUCCESS"
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "PREFERENCE", "NOTE", "FACT", "TASK"
    val key: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val scheduledTimeMillis: Long,
    val isCompleted: Boolean = false,
    val isCancelled: Boolean = false,
    val recurrence: String = "NONE", // "NONE", "DAILY", "WEEKLY"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_executions")
data class TaskExecutionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val intentType: String,
    val targetApp: String,
    val stepsSummary: String,
    val status: String, // "CREATED", "PLANNING", "AWAITING_CONFIRMATION", "EXECUTING", "COMPLETED", "FAILED", "CANCELLED"
    val resultMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)
