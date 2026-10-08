package com.soilad.vitapmenu

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.setSelectedDate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soilad.vitapmenu.ui.theme.ApplicationTheme
import java.time.LocalDate
import java.time.LocalDateTime


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        setContent {
            App()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeEntries(showCalendar: Boolean, onDismissCalendar: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val locale = LocalConfiguration.current.locales[0]
    var date by remember { mutableStateOf(LocalDate.now()) }

    val month = date.month.value
    val year  = date.year
    val icons = arrayOf("\uD83E\uDD66", "\uD83C\uDF56", "✨\uD83E\uDD66", "✨\uD83C\uDF56")
    val day = date.dayOfMonth

    val menuState = produceState<List<List<TimeEntry>>?>(initialValue = null, key1 = year, key2 = month) {
        value = try {
            getMenu(context, year.toString(), String.format("%02d", month)) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    val currentHour = LocalDateTime.now().hour
    val todayMenu = menuState.value?.getOrNull((day + 1) % 14)?.sortedBy { entry ->
        (entry.end.split(":")[0].toInt() - currentHour + 24) % 24
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var datePickerState = rememberDatePickerState()
    if (datePickerState.getSelectedDate() != null) {
        date = datePickerState.getSelectedDate()
    }

    if (showCalendar) {
        DatePickerDialog(
            onDismissRequest = onDismissCalendar,
            confirmButton = {
            }
        ) {
            DatePicker(
                state = datePickerState,
            )
        }
    }

    if (menuState.value == null) {
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            LoadingIndicator()
        }
        return
    }
    if (todayMenu == null) {
        Text(text = "No menu available", modifier = Modifier.padding(10.dp))
        return
    }

    Text(
        "$day/$month/$year",
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    LazyColumn(
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = {
                    offsetX = 0f
                },
                onDragEnd = {
                    if (offsetX < -50) {
                        date = date.plusDays(1)
                    } else if (offsetX > 50) {
                        date = date.minusDays(1)
                    }
                    datePickerState.setSelectedDate(date)
                }
            ) { _, dragAmount ->
                offsetX += dragAmount
            }
        }
    ) {
        items(todayMenu) { timeEntry ->
            Card (
                // color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(10.dp),
                // shape = RoundedCornerShape(CornerSize(10.dp))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = timeEntry.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
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
                            label = { Text("${icons[foodEntry.type]} ${foodEntry.name}") },
                            // selected = true,
                            colors   = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun App() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    var showCalendar by remember { mutableStateOf(false) }
    ApplicationTheme {
        Scaffold(
            topBar = {
                MediumTopAppBar(
                    title = {
                        Text("VIT-AP Mess Menu")
                    },
                    actions = {
                        IconButton(onClick = { /* do something */ }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Localized description"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                ),
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCalendar = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date",
                    )
                }
            }
        ) {
            paddingValues ->
            Column(modifier = Modifier.padding(
                top = paddingValues.calculateTopPadding(),
            )) {
                TimeEntries(showCalendar, onDismissCalendar = { showCalendar = false })
            }
        }
    }
}

