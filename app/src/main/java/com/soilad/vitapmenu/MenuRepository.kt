package com.soilad.vitapmenu

import android.content.Context
import java.time.YearMonth

class MenuRepository(private val context: Context) {
    suspend fun getMenu(date: YearMonth, forceRefresh: Boolean): List<List<TimeEntry>>? {
        return getMenu(context, date, forceRefresh)
    }
}