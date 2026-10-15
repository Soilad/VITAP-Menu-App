package com.soilad.vitapmenu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.setSelectedDate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soilad.vitapmenu.ui.theme.ApplicationTheme
import java.time.LocalDateTime

private val foodIcons = arrayOf("🥦", "🍖", "✨🥦", "✨🍖")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        setContent {
            ApplicationTheme {
                MenuRoute()
            }
        }
    }
}

// Stateful entry point: owns the ViewModel and forwards state down, intents up.
@Composable
fun MenuRoute(viewModel: MenuViewModel = viewModel(factory = MenuViewModel.Factory)) {
    val state by viewModel.menuState.collectAsStateWithLifecycle()
    MenuScreen(state = state, onIntent = viewModel::onIntent)
}

// Stateless: renders MenuState, reports user actions as MenuIntents.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(state: MenuState, onIntent: (MenuIntent) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("VIT-AP Mess Menu") },
                actions = {
                    IconButton(onClick = { onIntent(MenuIntent.ToggleCredits) }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Credits")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(MenuIntent.ToggleCalendar) }) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            MenuContent(state, onIntent)
        }
    }

    if (state.showCredits) {
        CreditsDialog(onDismiss = { onIntent(MenuIntent.ToggleCredits) })
    }

    if (state.showCalendar) {
        CalendarDialog(
            selectedDate = state.selectedDate,
            onConfirm = { onIntent(MenuIntent.SelectDate(it)) },
            onDismiss = { onIntent(MenuIntent.ToggleCalendar) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuContent(state: MenuState, onIntent: (MenuIntent) -> Unit) {
    if (state.isLoading) {
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            LoadingIndicator()
        }
        return
    }

    val date = state.selectedDate
    Text(
        "${date.dayOfMonth}/${date.monthValue}/${date.year}",
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    // pointerInput(Unit) keeps its first lambda, so read the latest values through these.
    val currentDate by rememberUpdatedState(state.selectedDate)
    val currentOnIntent by rememberUpdatedState(onIntent)
    // Drag offset is gesture-local UI state, not screen state.
    var offsetX by remember { mutableFloatStateOf(0f) }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onIntent(MenuIntent.Refresh) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offsetX = 0f },
                    onDragEnd = {
                        if (offsetX < -50) {
                            currentOnIntent(MenuIntent.SelectDate(currentDate.plusDays(1)))
                        } else if (offsetX > 50) {
                            currentOnIntent(MenuIntent.SelectDate(currentDate.minusDays(1)))
                        }
                    },
                ) { _, dragAmount ->
                    offsetX += dragAmount
                }
            },
        ) {
            state.error?.let { error ->
                item { Text(text = error, modifier = Modifier.padding(10.dp)) }
            }
            val dayMenu = state.dayMenu
            if (dayMenu == null && state.error == null) {
                item { Text(text = "No menu available", modifier = Modifier.padding(10.dp)) }
            }
            items(dayMenu.orEmpty()) { timeEntry ->
                TimeEntryCard(timeEntry)
            }
        }
    }
}

@Composable
private fun TimeEntryCard(timeEntry: TimeEntry) {
    Card(modifier = Modifier.padding(10.dp)) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = timeEntry.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "(${timeEntry.start} - ${timeEntry.end})",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        HorizontalDivider()
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(4.dp),
        ) {
            for (foodEntry in timeEntry.foodEntries) {
                SuggestionChip(
                    onClick = { Log.d("Suggestion chip", "hello world") },
                    label = { Text("${foodIcons.getOrElse(foodEntry.type) { "" }} ${foodEntry.name}") },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDialog(
    selectedDate: LocalDateTime,
    onConfirm: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
) {
    // Picker state is a draft; it only reaches MenuState when the user confirms.
    val pickerState = rememberDatePickerState()
    LaunchedEffect(Unit) { pickerState.setSelectedDate(selectedDate.toLocalDate()) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = pickerState.getSelectedDate()
                if (picked != null) onConfirm(picked.atTime(selectedDate.toLocalTime()))
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun CreditsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Soilad", textAlign = TextAlign.Left, fontSize = 24.sp)
                Button(onClick = { open("https://github.com/Soilad") }) { Text("GH") }
                Button(onClick = { open("https://www.youtube.com/@Soilad") }) { Text("YT") }
                Button(onClick = { open("https://instagram.com/_soilad_") }) { Text("Insta") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuScreenPreview() {
    val sample = List(14) {
        listOf(
            TimeEntry(
                title = "Lunch",
                start = "12:30",
                end = "14:00",
                foodEntries = listOf(FoodEntry("Rice", 0), FoodEntry("Chicken curry", 1)),
            ),
        )
    }
    ApplicationTheme {
        MenuScreen(
            state = MenuState(monthMenu = sample, isLoading = false),
            onIntent = {},
        )
    }
}
