package com.henry.budgetmvp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.BudgetCategory
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.EnvelopeItem
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.util.ThousandsSeparatorTransformation
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Composable
fun CategoryHeader(
    category: BudgetCategory,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditCategory: () -> Unit,
    onAddItem: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (isExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.name.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onAddItem) {
                    Icon(Lucide.Plus, contentDescription = "Add Item", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEditCategory) {
                    Icon(Lucide.EllipsisVertical, contentDescription = "Edit Category", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun EnvelopeItemRow(
    item: EnvelopeItem, 
    spentAmount: Double, 
    onClick: () -> Unit
) {
    val goalAmount = item.targetAmount
    val spentRatio = if (goalAmount > 0) (spentAmount / goalAmount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = spentRatio.coerceIn(0f, 1f),
        label = "spending_progress"
    )
    
    val isOverBudget = spentAmount > goalAmount + 0.001
    val percentSpent = (spentRatio * 100).toInt()
    
    val progressColor = when {
        spentRatio > 1.0f -> Color(0xFFC62828) // Red
        spentRatio > 0.8f -> Color(0xFFEF6C00) // Orange
        else -> Color(0xFF2E7D32) // Green
    }

    // Due Date Logic
    val today = java.time.LocalDate.now().dayOfMonth
    val dueStatus = remember(item.dueDay, today, spentAmount, goalAmount) {
        val dueDay = item.dueDay ?: return@remember null
        val daysUntil = dueDay - today
        
        when {
            daysUntil < 0 && spentAmount < goalAmount -> "Overdue"
            daysUntil == 0 -> "Due Today"
            daysUntil in 1..5 -> "Due in $daysUntil days"
            else -> "Due ${dueDay}${getOrdinalSuffix(dueDay)}"
        }
    }
    
    val dueBadgeColor = when (dueStatus) {
        "Overdue", "Due Today" -> Color(0xFFC62828)
        "Due in 1 days", "Due in 2 days", "Due in 3 days", "Due in 4 days", "Due in 5 days" -> Color(0xFFEF6C00)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Surface(
                            color = progressColor.copy(alpha = 0.1f),
                            contentColor = progressColor,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "$percentSpent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    if (dueStatus != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = dueBadgeColor.copy(alpha = 0.1f),
                            contentColor = dueBadgeColor,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = dueStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "planned",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${"%,.2f".format(item.targetAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = progressColor,
                trackColor = Color(0xFFE5E7EB),
                strokeCap = StrokeCap.Round
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$${"%,.2f".format(spentAmount)} spent of $${"%,.2f".format(goalAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (isOverBudget) {
                    Text(
                        text = "OVER BY $${"%,.2f".format(spentAmount - goalAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Black
                    )
                } else {
                    val remaining = goalAmount - spentAmount
                    Text(
                        text = "$${"%,.2f".format(if (kotlin.math.abs(remaining) < 0.001) 0.0 else remaining)} left",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getOrdinalSuffix(day: Int): String {
    return if (day in 11..13) "th"
    else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    item: EnvelopeItem,
    transactions: List<BudgetTransaction>,
    onDismiss: () -> Unit,
    onEditItem: () -> Unit,
    onEditTransaction: (BudgetTransaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spentAmount = transactions.sumOf { it.amount }
    val goalAmount = item.targetAmount
    val remaining = goalAmount - spentAmount

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
                    text = item.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onEditItem) {
                    Icon(Lucide.EllipsisVertical, contentDescription = "Edit Item")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Planned", style = MaterialTheme.typography.labelSmall)
                        Text("$${"%,.2f".format(goalAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Spent", style = MaterialTheme.typography.labelSmall)
                        Text("$${"%,.2f".format(spentAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Left", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "$${"%,.2f".format(if (kotlin.math.abs(remaining) < 0.001) 0.0 else remaining)}", 
                            fontWeight = FontWeight.Bold,
                            color = if (remaining < -0.001) Color(0xFFDC2626) else Color(0xFF059669)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TRANSACTION HISTORY",
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
                        "No transactions recorded yet.",
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
                                text = transaction.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (transaction.merchant.isNotBlank()) {
                                Text(
                                    text = transaction.merchant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "Transaction",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (transaction.note.isNotBlank()) {
                                Text(
                                    text = transaction.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "$${"%,.2f".format(transaction.amount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.type == TransactionType.INCOME) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
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
fun CategoryEntrySheet(
    targetCategory: BudgetCategory?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(targetCategory?.name ?: "") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (targetCategory != null) "Edit Category" else "Add New Category",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name (e.g., Housing, Food)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )

            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (targetCategory != null) "Update Category" else "Save Category")
            }

            if (targetCategory != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Lucide.Trash2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Category")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeItemEntrySheet(
    categoryId: String,
    targetItem: EnvelopeItem?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int?) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(targetItem?.name ?: "") }
    var targetAmount by remember { mutableStateOf(targetItem?.targetAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var selectedDueDay by remember { mutableStateOf(targetItem?.dueDay) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            name.isNotBlank() && ((targetAmount.toDoubleOrNull() ?: 0.0) > 0)
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
                text = if (targetItem != null) "Edit Item" else "Add New Item",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name (e.g., Rent, Groceries)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            OutlinedTextField(
                value = targetAmount,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && (input.all { it.isDigit() || it == '.' })) {
                        targetAmount = input
                    }
                },
                label = { Text("Planned Amount") },
                placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                visualTransformation = commaTransformation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = selectedDueDay?.let { "Day $it" } ?: "",
                    onValueChange = { },
                    label = { Text("Due Day of Month (Optional)") },
                    placeholder = { Text("Select date", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Lucide.Calendar, contentDescription = "Select or Change Due Date")
                        }
                    },
                    supportingText = { Text("Tap to select the recurring due day") }
                )
            }

            Button(
                onClick = { 
                    onConfirm(
                        name, 
                        targetAmount.toDoubleOrNull() ?: 0.0,
                        selectedDueDay
                    ) 
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (targetItem != null) "Update Item" else "Save Item")
            }

            if (targetItem != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Lucide.Trash2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Item")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val localDate = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        selectedDueDay = localDate.dayOfMonth
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (selectedDueDay != null) {
                        TextButton(onClick = {
                            selectedDueDay = null
                            showDatePicker = false
                        }) { Text("Clear Date") }
                    }
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
