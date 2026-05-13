package com.henry.budgetmvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// 1. DATA MODEL: Essential for clean architecture and type safety
data class Transaction(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val amount: Double,
    val category: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BudgetApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetApp() {
    // STATE: Using the new Transaction data class
    val transactions = remember { mutableStateListOf<Transaction>() }
    val totalBalance = transactions.sumOf { it.amount }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FINANCE ARCHITECT", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item { BalanceCard(amount = totalBalance) }

            item {
                TransactionInput(onSave = { name, amount, category ->
                    transactions.add(Transaction(name = name, amount = amount, category = category))
                })
            }

            item {
                Text(
                    "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(transactions, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onDelete = { transactions.remove(transaction) }
                )
            }
        }
    }
}

@Composable
fun BalanceCard(amount: Double) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Total Balance", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            Text(
                text = "$${"%.2f".format(amount)}",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionInput(onSave: (String, Double, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Food", "Rent", "Salary", "Transport", "Entertainment")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add Transaction", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { selection ->
                        DropdownMenuItem(
                            text = { Text(selection) },
                            onClick = {
                                selectedCategory = selection
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (use - for expenses)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    if (label.isNotBlank() && value != null) {
                        onSave(label, value, selectedCategory)
                        label = ""
                        amount = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Confirm Transaction")
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction, onDelete: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.name, fontWeight = FontWeight.Bold)
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val formattedAmount = "%.2f".format(Math.abs(transaction.amount))
                Text(
                    text = if (transaction.amount >= 0) "+$$formattedAmount" else "-$$formattedAmount",
                    color = if (transaction.amount >= 0) Color(0xFF2E7D32) else Color.Red,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}