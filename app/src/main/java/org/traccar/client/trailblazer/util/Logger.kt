package org.traccar.client.trailblazer.util

import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private val logs = mutableListOf<LogEntry>()

    data class LogEntry(val title: String, val description: String, val timestamp: String)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun addLog(title: String, description: String) {
        val timestamp = dateFormat.format(Date())
        logs.add(LogEntry(title, description, timestamp))
    }

    fun getLogs(): List<LogEntry> {
        return logs
    }
}
