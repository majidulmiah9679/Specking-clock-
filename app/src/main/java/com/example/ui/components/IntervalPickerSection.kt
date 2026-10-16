package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.IntervalPreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntervalPickerSection(
    selectedPreset: IntervalPreset,
    customMinutes: Int,
    onSelectInterval: (IntervalPreset, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interval_picker_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Speaking Interval",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Presets row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPreset == IntervalPreset.EVERY_15_MIN,
                    onClick = { onSelectInterval(IntervalPreset.EVERY_15_MIN, 15) },
                    label = { Text("15 min") },
                    leadingIcon = if (selectedPreset == IntervalPreset.EVERY_15_MIN) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("interval_chip_15m")
                )

                FilterChip(
                    selected = selectedPreset == IntervalPreset.EVERY_30_MIN,
                    onClick = { onSelectInterval(IntervalPreset.EVERY_30_MIN, 30) },
                    label = { Text("30 min") },
                    leadingIcon = if (selectedPreset == IntervalPreset.EVERY_30_MIN) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("interval_chip_30m")
                )

                FilterChip(
                    selected = selectedPreset == IntervalPreset.EVERY_1_HOUR,
                    onClick = { onSelectInterval(IntervalPreset.EVERY_1_HOUR, 60) },
                    label = { Text("1 hour") },
                    leadingIcon = if (selectedPreset == IntervalPreset.EVERY_1_HOUR) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("interval_chip_1h")
                )

                val customLabel = if (selectedPreset == IntervalPreset.CUSTOM) {
                    if (customMinutes >= 60) {
                        val h = customMinutes / 60
                        val m = customMinutes % 60
                        if (m == 0) "Custom ($h hr)" else "Custom (${h}h ${m}m)"
                    } else {
                        "Custom ($customMinutes min)"
                    }
                } else {
                    "Custom..."
                }

                FilterChip(
                    selected = selectedPreset == IntervalPreset.CUSTOM,
                    onClick = {
                        showCustomDialog = true
                    },
                    label = { Text(customLabel) },
                    leadingIcon = {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("interval_chip_custom")
                )
            }
        }
    }

    if (showCustomDialog) {
        CustomIntervalDialog(
            initialMinutes = customMinutes,
            onDismiss = { showCustomDialog = false },
            onConfirm = { minutes ->
                showCustomDialog = false
                onSelectInterval(IntervalPreset.CUSTOM, minutes)
            }
        )
    }
}

@Composable
fun CustomIntervalDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hoursText by remember { mutableStateOf((initialMinutes / 60).let { if (it > 0) it.toString() else "" }) }
    var minutesText by remember { mutableStateOf((initialMinutes % 60).toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Custom Interval", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Configure how often the speaking clock announces the time aloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 2) {
                                hoursText = input
                                errorMessage = null
                            }
                        },
                        label = { Text("Hours") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_hours_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                minutesText = input
                                errorMessage = null
                            }
                        },
                        label = { Text("Minutes") },
                        placeholder = { Text("15") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_minutes_input"),
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    val h = hoursText.toIntOrNull() ?: 0
                    val m = minutesText.toIntOrNull() ?: 0
                    val total = (h * 60) + m
                    Text(
                        text = "Total interval: $total minutes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = hoursText.toIntOrNull() ?: 0
                    val m = minutesText.toIntOrNull() ?: 0
                    val total = (h * 60) + m

                    if (total <= 0) {
                        errorMessage = "Interval must be at least 1 minute."
                    } else if (total > 720) {
                        errorMessage = "Interval cannot exceed 12 hours (720 min)."
                    } else {
                        onConfirm(total)
                    }
                },
                modifier = Modifier.testTag("confirm_custom_interval_button")
            ) {
                Text("Apply Interval", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
