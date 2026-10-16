package com.soilad.vitapmenu

import java.time.LocalDateTime

sealed class MenuIntent {
    data class SelectDate(val date: LocalDateTime) : MenuIntent()
    data object Refresh : MenuIntent()
    data object ToggleCredits : MenuIntent()
    data object ToggleCalendar : MenuIntent()
}