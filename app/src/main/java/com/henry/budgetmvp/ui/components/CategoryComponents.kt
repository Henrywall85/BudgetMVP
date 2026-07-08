package com.henry.budgetmvp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
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
import com.henry.budgetmvp.data.BudgetCategory
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.EnvelopeItem
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.util.ThousandsSeparatorTransformation

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
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
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
                    Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEditCategory) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Edit Category", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun EnvelopeItemRow(item: EnvelopeItem, spentAmount: Double, onClick: () -> Unit) {
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
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
            
            val progress = if (item.targetAmount > 0) (spentAmount / item.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (spentAmount > item.targetAmount) Color(0xFFDC2626) else Color(0xFF059669),
                trackColor = Color(0xFFE5E7EB),
                strokeCap = StrokeCap.Round
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$${"%,.2f".format(spentAmount)} spent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (spentAmount > item.targetAmount) {
                    Text(
                        text = "Over by $${"%,.2f".format(spentAmount - item.targetAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val remaining = item.targetAmount - spentAmount
                    Text(
                        text = "$${"%,.2f".format(remaining)} left",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
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
    val remaining = item.targetAmount - spentAmount

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
                    Icon(Icons.Default.MoreVert, contentDescription = "Edit Item")
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
                        Text("$${"%,.2f".format(item.targetAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Spent", style = MaterialTheme.typography.labelSmall)
                        Text("$${"%,.2f".format(spentAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Left", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "$${"%,.2f".format(remaining)}", 
                            fontWeight = FontWeight.Bold,
                            color = if (remaining < 0) Color(0xFFDC2626) else Color(0xFF059669)
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
                            color = if (transaction.type == TransactionType.EXPENSE) Color(0xFFDC2626) else Color(0xFF059669)
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
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
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
    onConfirm: (String, Double) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(targetItem?.name ?: "") }
    var targetAmount by remember { mutableStateOf(targetItem?.targetAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }

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
                label = { Text("Monthly Target Amount") },
                placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                visualTransformation = commaTransformation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { onConfirm(name, targetAmount.toDoubleOrNull() ?: 0.0) },
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
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Item")
                }
            }
        }
    }
}
