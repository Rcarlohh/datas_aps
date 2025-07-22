package com.example.datasimulator

import java.text.SimpleDateFormat
import java.util.*

data class DataItem(
    val id: Long,
    val type: String,
    val value: Double,
    val timestamp: Long
) {
    fun getFormattedTimestamp(): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
    
    fun getFormattedValue(): String {
        return String.format(Locale.getDefault(), "%d", value.toInt())
    }
} 