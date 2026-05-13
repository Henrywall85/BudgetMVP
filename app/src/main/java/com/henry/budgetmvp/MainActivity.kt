// Ensure your package name matches the one in your terminal from Screenshot 2026-05-12 at 10.23.05 PM.jpg
package com.henry.budgetmvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BudgetApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetApp() {
    // 1. STATE: Using mutableStateListOf to allow the UI to react to additions/deletions
    val transactions = remember { mutableStateListOf<Pair<String, Double>>() }
    val totalBalance = transactions.sumOf { it.second }

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
        // 2. PERFORMANCE: LazyColumn only renders items currently visible on screen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BalanceCard(amount = totalBalance) }

            item {
                TransactionInput(onSave = { name, amount ->
                    transactions.add(name to amount)
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

            // 3. DYNAMIC LIST: This maps the state list to individual UI rows
            items(transactions) { transaction ->
                TransactionRow(
                    name = transaction.first,
                    amount = transaction.second,
                    onDelete = { transactions.remove(transaction) }
                )
            }
        }
    }
}

@Composable
fun BalanceCard(amount: Double) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Total Balance", color = Color.White.copy(alpha = 0.7f))
            Text("$$amount", style = MaterialTheme.typography.displayMedium, color = Color.White)
        }
    }
}

@Composable
fun TransactionRow(name: String, amount: Double, onDelete: () -> Unit) {
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
                Text(text = name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (amount < 0) "Expense" else "Income",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val formattedAmount = "%.2f".format(Math.abs(amount))
                Text(
                    text = if (amount >= 0) "+$$formattedAmount" else "-$$formattedAmount",
                    color = if (amount >= 0) Color(0xFF2E7D32) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionInput(onSave: (String, Double) -> Unit) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add Transaction", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Description (e.g. Groceries)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    // 3. VALIDATION: Ensure inputs are valid before saving
                    val value = amount.toDoubleOrNull()
                    if (label.isNotBlank() && value != null) {
                        onSave(label, value)
                        label = "" // Clear the field
                        amount = "" // Clear the field
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Confirm Transaction")
            }
        }
    }
}