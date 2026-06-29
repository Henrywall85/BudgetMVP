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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.henry.budgetmvp.data.CategoryWithItems
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.util.ThousandsSeparatorTransformation
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionPage(
    categoriesWithItems: List<CategoryWithItems>,
    onConfirm: (TransactionType, Double, String, Int?) -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedItemId by remember { mutableStateOf<Int?>(null) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var itemExpanded by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf<CategoryWithItems?>(null) }

    val focusManager = LocalFocusManager.current
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            val amountValue = amount.toDoubleOrNull() ?: 0.0
            amountValue > 0 && (type == TransactionType.INCOME || selectedItemId != null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
                    LocalDate.now().toString(),
                    selectedItemId
                )
                // Reset form
                amount = ""
                note = ""
                selectedItemId = null
                selectedCategory = null
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text("SAVE TRANSACTION", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntrySheet(
    categoriesWithItems: List<CategoryWithItems>,
    onDismiss: () -> Unit,
    onConfirm: (TransactionType, Double, String, Int?) -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var itemExpanded by remember { mutableStateOf(false) }
    
    var selectedCategory by remember { mutableStateOf<CategoryWithItems?>(null) }

    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            val amountValue = amount.toDoubleOrNull() ?: 0.0
            amountValue > 0 && (type == TransactionType.INCOME || selectedItemId != null)
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
                text = "Add Transaction",
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true
            )

            Button(
                onClick = {
                    onConfirm(
                        type,
                        amount.toDoubleOrNull() ?: 0.0,
                        LocalDate.now().toString(),
                        selectedItemId
                    )
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Transaction")
            }
        }
    }
}
