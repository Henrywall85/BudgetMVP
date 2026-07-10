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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.filled.MoreVert
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.IncomeStream
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.util.ThousandsSeparatorTransformation
import com.henry.budgetmvp.util.calculateNextPayday
import com.henry.budgetmvp.util.formatIsoDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun TotalPoolCard(total: Double) {
    val dateText = remember { 
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dateText.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${"%,.2f".format(total)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Total Funds Available",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun IncomeDetailsCard(
    stream: IncomeStream,
    receivedAmount: Double,
    lastPaymentDate: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stream.sourceName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = if (stream.frequency == "Irregular") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                ) {
                    val label = if (stream.frequency == "Irregular") {
                        if (lastPaymentDate != null) "Last: ${formatIsoDate(lastPaymentDate)}" else "Variable Income"
                    } else {
                        "Next: ${calculateNextPayday(stream.lastPayday, stream.frequency)}"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (stream.frequency == "Irregular") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (stream.amount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                // Progress Section
                val progress = (receivedAmount / stream.amount).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF059669),
                    trackColor = Color(0xFFE5E7EB),
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stream.frequency,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (stream.amount > 0) "Goal: $${"%,.2f".format(stream.amount)}" else "No Monthly Goal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$${"%,.2f".format(receivedAmount)} received",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDetailSheet(
    stream: IncomeStream,
    transactions: List<BudgetTransaction>,
    onDismiss: () -> Unit,
    onEditStream: () -> Unit,
    onEditTransaction: (BudgetTransaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val receivedAmount = transactions.sumOf { it.amount }
    val remaining = stream.amount - receivedAmount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stream.sourceName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onEditStream) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Edit Source")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (stream.amount > 0) {
                        Column {
                            Text("Planned", style = MaterialTheme.typography.labelSmall)
                            Text("$${"%,.2f".format(stream.amount)}", fontWeight = FontWeight.Bold)
                        }
                    }
                    Column {
                        Text(if (stream.amount > 0) "Received" else "Total Received", style = MaterialTheme.typography.labelSmall)
                        Text("$${"%,.2f".format(receivedAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                    if (stream.amount > 0) {
                        Column {
                            Text("Pending", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "$${"%,.2f".format(if (remaining > 0) remaining else 0.0)}", 
                                fontWeight = FontWeight.Bold,
                                color = if (remaining > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PAYMENT HISTORY",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No payments recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                transactions.forEach { transaction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditTransaction(transaction) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = formatIsoDate(transaction.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (transaction.merchant.isNotBlank()) transaction.merchant else "Income Payment",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "+$${"%,.2f".format(transaction.amount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
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
    var amount by remember { mutableStateOf(targetStream?.amount?.let { if (it == 0.0) "0" else it.toString() } ?: "") }
    var frequency by remember { mutableStateOf(targetStream?.frequency ?: "Please Enter Frequency") }
    var selectedDateIso by remember { mutableStateOf(targetStream?.lastPayday ?: LocalDate.now().toString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val frequencies = listOf("Weekly", "Bi-Weekly", "Monthly", "Irregular")
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            source.isNotBlank() && (amount.toDoubleOrNull() ?: -1.0) >= 0 && frequency != "Please Enter Frequency" && selectedDateIso.isNotBlank()
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
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) { amount = input }
                },
                label = { Text("Planned Amount (Set to 0 if irregular)") },
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

