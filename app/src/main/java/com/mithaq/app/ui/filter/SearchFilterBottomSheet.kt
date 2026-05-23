package com.mithaq.app.ui.filter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithaq.app.model.*
import com.mithaq.app.ui.theme.LocalMithaqStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterBottomSheet(
    onDismissRequest: () -> Unit,
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val filterCriteria by viewModel.filterCriteria.collectAsState()
    val scrollState = rememberScrollState()
    val strings = LocalMithaqStrings.current
    val isArabic = strings.appName == "ميثاق"

    // Temporary local state for modifications inside bottom sheet
    var ageRange by remember { mutableStateOf(filterCriteria.minAge.toFloat()..filterCriteria.maxAge.toFloat()) }
    var selectedSect by remember { mutableStateOf(filterCriteria.sect) }
    val selectedPrayers = remember { mutableStateListOf<PrayerFrequency>().apply { addAll(filterCriteria.prayerFrequencies) } }
    val selectedModesty = remember { mutableStateListOf<ModestyPreference>().apply { addAll(filterCriteria.modestyPreferences) } }
    val selectedRelocation = remember { mutableStateListOf<RelocationWillingness>().apply { addAll(filterCriteria.relocationWillingness) } }
    var polygamyAcceptance by remember { mutableStateOf(filterCriteria.polygamyAcceptance) }
    var countryText by remember { mutableStateOf(filterCriteria.country) }
    var cityText by remember { mutableStateOf(filterCriteria.city) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.searchFilters,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = if (isArabic) "إغلاق" else "Close")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Scrollable Filters
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
            ) {
                // 1. Age Range Filter
                val ageRangeLabel = if (isArabic) {
                    "الفئة العمرية: ${ageRange.start.toInt()} - ${ageRange.endInclusive.toInt()} سنة"
                } else {
                    "Age Range: ${ageRange.start.toInt()} - ${ageRange.endInclusive.toInt()} years"
                }
                Text(
                    text = ageRangeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                RangeSlider(
                    value = ageRange,
                    onValueChange = { ageRange = it },
                    valueRange = 18f..70f,
                    steps = 51,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Country & City Filters
                Text(
                    text = if (isArabic) "الموقع الجغرافي" else "Location Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = countryText,
                        onValueChange = { countryText = it },
                        label = { Text(if (isArabic) "الدولة" else "Country") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cityText,
                        onValueChange = { cityText = it },
                        label = { Text(if (isArabic) "المدينة" else "City") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 2. Sect Selection
                Text(
                    text = strings.selectSect,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Sect.values().forEach { sect ->
                        FilterChip(
                            selected = selectedSect == sect,
                            onClick = {
                                selectedSect = if (selectedSect == sect) null else sect
                            },
                            label = { Text(sect.getDisplayName(isArabic)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 3. Prayer Frequency Filter (Multiple Choice)
                Text(
                    text = strings.selectPrayer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    PrayerFrequency.values().forEach { freq ->
                        val isSelected = selectedPrayers.contains(freq)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedPrayers.remove(freq) else selectedPrayers.add(freq)
                            },
                            label = { Text(freq.getDisplayName(isArabic)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 4. Hijab/Niqab Modesty Preference (Multiple Choice)
                Text(
                    text = if (isArabic) "الالتزام بالزي الشرعي" else "Modesty Preference",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    ModestyPreference.values().forEach { pref ->
                        val isSelected = selectedModesty.contains(pref)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedModesty.remove(pref) else selectedModesty.add(pref)
                            },
                            label = { Text(pref.getDisplayName(isArabic)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 5. Relocation Willingness Filter (Multiple Choice)
                Text(
                    text = strings.selectRelocation,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    RelocationWillingness.values().forEach { willingness ->
                        val isSelected = selectedRelocation.contains(willingness)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedRelocation.remove(willingness) else selectedRelocation.add(willingness)
                            },
                            label = { Text(willingness.getDisplayName(isArabic)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 6. Polygamy Acceptance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.polygamyAcceptance,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isArabic) "إظهار الملفات الشخصية المقبولة لتعدد الزوجات" else "Show profiles open to polygamous setups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = polygamyAcceptance == true,
                        onCheckedChange = {
                            polygamyAcceptance = if (it) true else null
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Reset local selections
                        ageRange = 18f..70f
                        selectedSect = null
                        selectedPrayers.clear()
                        selectedModesty.clear()
                        selectedRelocation.clear()
                        polygamyAcceptance = null
                        countryText = ""
                        cityText = ""
                        
                        viewModel.resetFilters()
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isArabic) "مسح الكل" else "Clear All")
                }

                Button(
                    onClick = {
                        val newCriteria = FilterCriteria(
                            minAge = ageRange.start.toInt(),
                            maxAge = ageRange.endInclusive.toInt(),
                            sect = selectedSect,
                            prayerFrequencies = selectedPrayers.toSet(),
                            modestyPreferences = selectedModesty.toSet(),
                            relocationWillingness = selectedRelocation.toSet(),
                            polygamyAcceptance = polygamyAcceptance,
                            country = countryText,
                            city = cityText
                        )
                        viewModel.updateFilters(newCriteria)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isArabic) "تطبيق التصفية" else "Apply Filters")
                }
            }
        }
    }
}

/**
 * A simplified FlowRow helper to wrap items to the next line dynamically.
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val mainSpacingPx = mainAxisSpacing.roundToPx()
        val crossSpacingPx = crossAxisSpacing.roundToPx()
        
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        val rowHeights = mutableListOf<Int>()
        
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0
        
        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            if (currentRowWidth + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + mainSpacingPx
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowHeights.add(currentRowHeight)
        }
        
        val totalWidth = constraints.maxWidth
        val totalHeight = rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * crossSpacingPx
        
        layout(totalWidth, totalHeight) {
            var currentY = 0
            rows.forEachIndexed { rowIndex, row ->
                var currentX = 0
                row.forEach { placeable ->
                    placeable.placeRelative(currentX, currentY)
                    currentX += placeable.width + mainSpacingPx
                }
                currentY += rowHeights[rowIndex] + crossSpacingPx
            }
        }
    }
}
