package com.henry.budgetmvp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.henry.budgetmvp.data.IncomeStream
import com.henry.budgetmvp.util.ThousandsSeparatorTransformation
import com.henry.budgetmvp.util.calculateNextPayday
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun TotalPoolCard(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Funds Available (Ready to Assign)",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${"%,.2f".format(total)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun IncomeDetailsCard(stream: IncomeStream, onClick: () -> Unit, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stream.sourceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Next: ${calculateNextPayday(stream.lastPayday, stream.frequency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text("Cycle Window: ${stream.frequency}", style = MaterialTheme.typography.bodyMedium)
            Text("Amount: $${"%,.2f".format(stream.amount)}", style = MaterialTheme.typography.labelMedium)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeEntrySheet(
    targetStream: IncomeStream?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit,
    onDelete: () -> Unit
) {
    var source by remember { mutableStateOf(targetStream?.sourceName ?: "") }
    var amount by remember { mutableStateOf(targetStream?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var frequency by remember { mutableStateOf(targetStream?.frequency ?: "Please Enter Frequency") }
    var selectedDateIso by remember { mutableStateOf(targetStream?.lastPayday ?: LocalDate.now().toString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val frequencies = listOf("Weekly", "Bi-Weekly", "Monthly")
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            source.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && frequency != "Please Enter Frequency" && selectedDateIso.isNotBlank()
        }
    }

    val displayDateStr = remember(selectedDateIso) {
        try {
            LocalDate.parse(selectedDateIso).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        } catch (e: Exception) {
            selectedDateIso
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (targetStream != null) "Edit ${targetStream.sourceName}" else "Add New Income Source",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text("Income Source Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) { amount = input }
                },
                label = { Text("Paycheck Amount") },
                placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                visualTransformation = commaTransformation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = displayDateStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Last Payday Date") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                )
                Box(modifier = Modifier.matchParentSize().clickable { focusManager.clearFocus(); showDatePicker = true })
            }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    frequencies.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                frequency = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onConfirm(source, amount.toDoubleOrNull() ?: 0.0, frequency, selectedDateIso) },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (targetStream != null) "Update Income Source" else "Save Income Source")
            }

            if (targetStream != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Income Source")
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = try {
                    LocalDate.parse(selectedDateIso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateIso = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

