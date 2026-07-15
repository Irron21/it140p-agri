package com.example.agriflow.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Date
import kotlin.math.ceil
import kotlin.math.min

/**
 * Configuration for the history filtering system.
 *
 * @param quickFilters List of labels for the horizontally scrollable FilterChips.
 */
data class HistoryFilterConfig(
    val quickFilters: List<String> = emptyList()
)

/**
 * A highly reusable history screen component designed for the AgriFlow app.
 *
 * @param T The type of data items being displayed.
 * @param historyItems The list of history data objects.
 * @param filterConfig Configuration for the quick filter chips.
 * @param onFilterToggled Callback for quick filter chips (label, isSelected).
 * @param onAdvancedFilterSave Callback when "Apply Filters" is clicked in the bottom sheet.
 * @param onDateRangeSelected Optional callback for when a date range is selected via the picker.
 * @param itemContent Composable lambda to render each item [T].
 * @param advancedFilterContent Slot for feature-specific filter UI (e.g., Sliders).
 * @param modifier Modifier for the container.
 * @param pageSize Number of items per page for pagination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GenericHistoryScreen(
    historyItems: List<T>,
    filterConfig: HistoryFilterConfig,
    onFilterToggled: (String, Boolean) -> Unit,
    onAdvancedFilterSave: () -> Unit,
    onDateRangeSelected: ((Long?, Long?) -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
    advancedFilterContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pageSize: Int = 10
) {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState()

    // Reset page when items change
    LaunchedEffect(historyItems.size) {
        currentPage = 1
    }

    val totalPages = maxOf(1, ceil(historyItems.size.toDouble() / pageSize).toInt())
    val paginatedItems = remember(historyItems, currentPage) {
        val start = (currentPage - 1) * pageSize
        val end = min(start + pageSize, historyItems.size)
        if (start < historyItems.size) historyItems.subList(start, end) else emptyList()
    }

    // Active filters state
    val activeFilters = remember { mutableStateListOf<String>() }

    Column(modifier = modifier) {
        // 1. Horizontally scrollable Row of FilterChips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onDateRangeSelected != null) {
                FilterChip(
                    selected = datePickerState.selectedStartDateMillis != null,
                    onClick = { showDatePicker = true },
                    label = { Text("Date Range", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(FilterChipDefaults.IconSize)) }
                )
            }

            filterConfig.quickFilters.forEach { label ->
                val isSelected = activeFilters.contains(label)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) activeFilters.remove(label) else activeFilters.add(label)
                        onFilterToggled(label, !isSelected)
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.FilterList, null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null
                )
            }

            FilterChip(
                selected = false,
                onClick = { showBottomSheet = true },
                label = { Text("Advanced...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Tune, null, Modifier.size(FilterChipDefaults.IconSize)) }
            )
        }

        // 2. Pagination Controls
        if (historyItems.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page $currentPage of $totalPages (${historyItems.size} total)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(
                        onClick = { if (currentPage > 1) currentPage-- },
                        enabled = currentPage > 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { if (currentPage < totalPages) currentPage++ },
                        enabled = currentPage < totalPages,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 3. LazyColumn for history data (Independently scrollable)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (paginatedItems.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No runs found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(paginatedItems) { item ->
                    itemContent(item)
                }
            }
        }
    }

    // 4. Modal Bottom Sheet for Advanced Filters
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text("Advanced Filters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                advancedFilterContent()

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showBottomSheet = false }) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onAdvancedFilterSave()
                        showBottomSheet = false
                    }) { Text("Apply Filters") }
                }
            }
        }
    }

    if (showDatePicker && onDateRangeSelected != null) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateRangeSelected(datePickerState.selectedStartDateMillis, datePickerState.selectedEndDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    datePickerState.setSelection(null, null)
                    onDateRangeSelected(null, null)
                    showDatePicker = false
                }) { Text("Clear") }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                title = { Text("Select History Range", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth().height(500.dp).padding(16.dp)
            )
        }
    }
}
