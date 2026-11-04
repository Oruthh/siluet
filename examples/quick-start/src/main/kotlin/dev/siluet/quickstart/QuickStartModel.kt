package dev.siluet.quickstart

import java.time.LocalDateTime

data class QuickStartModel(
    val projectName: String,
    val date: LocalDateTime,
    val showDate: Boolean,
    val randomNumbers: List<Int>,
)
