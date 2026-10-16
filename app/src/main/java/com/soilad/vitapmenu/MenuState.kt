package com.soilad.vitapmenu

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class FoodEntry(val name: String, val type: Int)
@Serializable
data class TimeEntry(val title: String, val start: String, val end: String, val foodEntries: List<FoodEntry>)

data class MenuState (
    val monthMenu: List<List<TimeEntry>>? = null,
    var selectedDate: LocalDateTime = LocalDateTime.now(),
    var error: String? = null,
    var isLoading: Boolean = true,
    var isRefreshing: Boolean = false,
    var showCalendar: Boolean = false,
    var showCredits: Boolean = false,
) {
    val dayMenu: List<TimeEntry>? get() {
        val hour = LocalDateTime.now().hour
        return monthMenu?.getOrNull((selectedDate.dayOfMonth - 1) % 14)?.sortedBy {
            (it.end.split(":")[0].toInt() - hour + 24) % 24
        }
    }
}
