package com.henry.budgetmvp.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.CategoryWithItems
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.util.ThousandsSeparatorTransformation
import com.henry.budgetmvp.util.formatIsoDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionPage(
    categoriesWithItems: List<CategoryWithItems>,
    incomeStreams: List<com.henry.budgetmvp.data.IncomeStream>,
    onConfirm: (TransactionType, Double, String, String, String, String?, String?) -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var selectedIncomeStreamId by remember { mutableStateOf<String?>(null) }
    var selectedDateIso by remember { mutableStateOf(LocalDate.now().toString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var itemExpanded by remember { mutableStateOf(false) }
    var incomeSourceExpanded by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf<CategoryWithItems?>(null) }

    val focusManager = LocalFocusManager.current
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            val amountValue = amount.toDoubleOrNull() ?: 0.0
            amountValue > 0 && (
                (type == TransactionType.EXPENSE && selectedItemId != null) || 
                (type == TransactionType.INCOME && selectedIncomeStreamId != null)
            )
        }
    }

    val displayDateStr = remember(selectedDateIso) {
        try {
            LocalDate.parse(selectedDateIso).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        } catch (e: Exception) {
            selectedDateIso
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Record Transaction",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type Toggle
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Expense")
                    }
                    SegmentedButton(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Income")
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (input.count { it == '.' } <= 1 && (input.all { it.isDigit() || it == '.' })) {
                            amount = input
                        }
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("$0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                    visualTransformation = commaTransformation,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = displayDateStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transaction Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { focusManager.clearFocus(); showDatePicker = true })
                }

                if (type == TransactionType.EXPENSE) {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant") },
                        placeholder = { Text("e.g., Walmart, Amazon") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                        singleLine = true
                    )
                }

                if (type == TransactionType.INCOME) {
                    ExposedDropdownMenuBox(
                        expanded = incomeSourceExpanded,
                        onExpandedChange = { incomeSourceExpanded = it }
                    ) {
                        val selectedSource = incomeStreams.find { it.id == selectedIncomeStreamId }
                        OutlinedTextField(
                            value = selectedSource?.sourceName ?: "Select Income Source",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Income Source") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = incomeSourceExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = incomeSourceExpanded,
                            onDismissRequest = { incomeSourceExpanded = false }
                        ) {
                            incomeStreams.forEach { stream ->
                                DropdownMenuItem(
                                    text = { Text(stream.sourceName) },
                                    onClick = {
                                        selectedIncomeStreamId = stream.id
                                        merchant = stream.sourceName // Set merchant to source name for display
                                        incomeSourceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (type == TransactionType.EXPENSE) {
                    // Category Selector
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.category?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categoriesWithItems.forEach { catWithItems ->
                                DropdownMenuItem(
                                    text = { Text(catWithItems.category.name) },
                                    onClick = {
                                        selectedCategory = catWithItems
                                        selectedItemId = null
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Item Selector
                    if (selectedCategory != null) {
                        ExposedDropdownMenuBox(
                            expanded = itemExpanded,
                            onExpandedChange = { itemExpanded = it }
                        ) {
                            val selectedItem = selectedCategory?.items?.find { it.id == selectedItemId }
                            OutlinedTextField(
                                value = selectedItem?.name ?: "Select Item",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Item") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = itemExpanded,
                                onDismissRequest = { itemExpanded = false }
                            ) {
                                selectedCategory?.items?.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.name) },
                                        onClick = {
                                            selectedItemId = item.id
                                            itemExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true
                )
            }
        }

        Button(
            onClick = {
                onConfirm(
                    type,
                    amount.toDoubleOrNull() ?: 0.0,
                    selectedDateIso,
                    merchant,
                    note,
                    selectedItemId,
                    selectedIncomeStreamId
                )
                // Reset form
                amount = ""
                merchant = ""
                note = ""
                selectedItemId = null
                selectedIncomeStreamId = null
                selectedCategory = null
                selectedDateIso = LocalDate.now().toString()
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text("SAVE TRANSACTION", fontWeight = FontWeight.Bold)
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntrySheet(
    targetTransaction: BudgetTransaction? = null,
    categoriesWithItems: List<CategoryWithItems>,
    incomeStreams: List<com.henry.budgetmvp.data.IncomeStream>,
    onDismiss: () -> Unit,
    onConfirm: (TransactionType, Double, String, String, String, String?, String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(targetTransaction?.type ?: TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf(targetTransaction?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(targetTransaction?.merchant ?: "") }
    var note by remember { mutableStateOf(targetTransaction?.note ?: "") }
    var selectedItemId by remember { mutableStateOf(targetTransaction?.itemId) }
    var selectedIncomeStreamId by remember { mutableStateOf(targetTransaction?.incomeStreamId) }
    var selectedDateIso by remember { mutableStateOf(targetTransaction?.date ?: LocalDate.now().toString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var itemExpanded by remember { mutableStateOf(false) }
    var incomeSourceExpanded by remember { mutableStateOf(false) }
    
    var selectedCategory by remember { 
        mutableStateOf(
            if (targetTransaction?.itemId != null) {
                categoriesWithItems.find { cat -> cat.items.any { it.id == targetTransaction.itemId } }
            } else null
        )
    }

    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            val amountValue = amount.toDoubleOrNull() ?: 0.0
            amountValue > 0 && (type == TransactionType.INCOME || selectedItemId != null)
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
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (targetTransaction == null) "Add Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Type Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Expense")
                }
                SegmentedButton(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Income")
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && (input.all { it.isDigit() || it == '.' })) {
                        amount = input
                    }
                },
                label = { Text("Amount") },
                placeholder = { Text("$0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                visualTransformation = commaTransformation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = displayDateStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Transaction Date") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(modifier = Modifier.matchParentSize().clickable { focusManager.clearFocus(); showDatePicker = true })
            }

            if (type == TransactionType.EXPENSE) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    placeholder = { Text("e.g., Walmart, Amazon") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                    singleLine = true
                )
            }

            if (type == TransactionType.INCOME) {
                ExposedDropdownMenuBox(
                    expanded = incomeSourceExpanded,
                    onExpandedChange = { incomeSourceExpanded = it }
                ) {
                    val selectedSource = incomeStreams.find { it.id == selectedIncomeStreamId }
                    OutlinedTextField(
                        value = selectedSource?.sourceName ?: "Select Income Source",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Income Source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = incomeSourceExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = incomeSourceExpanded,
                        onDismissRequest = { incomeSourceExpanded = false }
                    ) {
                        incomeStreams.forEach { stream ->
                            DropdownMenuItem(
                                text = { Text(stream.sourceName) },
                                onClick = {
                                    selectedIncomeStreamId = stream.id
                                    merchant = stream.sourceName // Set merchant for display in history
                                    incomeSourceExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (type == TransactionType.EXPENSE) {
                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.category?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoriesWithItems.forEach { catWithItems ->
                            DropdownMenuItem(
                                text = { Text(catWithItems.category.name) },
                                onClick = {
                                    selectedCategory = catWithItems
                                    selectedItemId = null
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Item Selector
                if (selectedCategory != null) {
                    ExposedDropdownMenuBox(
                        expanded = itemExpanded,
                        onExpandedChange = { itemExpanded = it }
                    ) {
                        val selectedItem = selectedCategory?.items?.find { it.id == selectedItemId }
                        OutlinedTextField(
                            value = selectedItem?.name ?: "Select Item",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Item") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = itemExpanded,
                            onDismissRequest = { itemExpanded = false }
                        ) {
                            selectedCategory?.items?.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.name) },
                                    onClick = {
                                        selectedItemId = item.id
                                        itemExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true
            )



            Button(
                onClick = {
                    onConfirm(
                        type,
                        amount.toDoubleOrNull() ?: 0.0,
                        selectedDateIso,
                        merchant,
                        note,
                        selectedItemId,
                        selectedIncomeStreamId
                    )
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (targetTransaction == null) "Add Transaction" else "Update Transaction")
            }

            if (targetTransaction != null && onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Lucide.Trash2, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Transaction")
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
