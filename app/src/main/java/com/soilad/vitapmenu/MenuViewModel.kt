package com.soilad.vitapmenu

import android.view.Menu
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY

class MenuViewModel(private val menuRepository: MenuRepository): ViewModel() {
    val userMenuIntent = Channel<MenuIntent>(Channel.UNLIMITED)
    private val _menuState = MutableStateFlow(MenuState())
    val menuState: StateFlow<MenuState>
        get() = _menuState
    var loadJob: Job? = null
    var loadedMonth: YearMonth? = null

    private fun load(month: YearMonth, force: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _menuState.update {
                it.copy(
                    isLoading = !force,
                    isRefreshing = force,
                )
            }
            val menu = menuRepository.getMenu(month, force)
            var errorMessage: String?
            if (menu != null) {
                loadedMonth = month
                errorMessage = null
            } else {
                loadedMonth = null
                errorMessage = "Error loading menu"
            }
            _menuState.update {
                it.copy(
                    monthMenu = menu,
                    isLoading = false,
                    isRefreshing = false,
                    error = errorMessage,
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                MenuViewModel(MenuRepository(this[APPLICATION_KEY]!!.applicationContext))
            }
        }
    }

    init {
        load(YearMonth.from(_menuState.value.selectedDate), force = true)
    }

    fun onIntent(intent: MenuIntent) {
        when (intent) {
            is MenuIntent.SelectDate     -> {
                if (_menuState.value.selectedDate.monthValue != intent.date.monthValue) {
                    load(YearMonth.from(intent.date), force = true)
                }
                _menuState.update {
                    it.copy(selectedDate = intent.date)
                }
            }
            is MenuIntent.ToggleCredits  -> {
                _menuState.update {
                    it.copy(showCredits = !_menuState.value.showCredits)
                }
            }
            is MenuIntent.ToggleCalendar -> {
                _menuState.update {
                    it.copy(showCalendar = !_menuState.value.showCalendar)
                }
            }
            is MenuIntent.Refresh        -> {
                load(YearMonth.from(_menuState.value.selectedDate), force = true)
            }
        }
    }

}