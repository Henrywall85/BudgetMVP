package com.henry.budgetmvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// --- 1. DATA LAYER (ROOM) ---
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}

// --- 2. BUSINESS LOGIC (VIEWMODEL) ---
class BudgetViewModel(private val dao: TransactionDao) : ViewModel() {
    val transactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    fun addTransaction(name: String, amount: Double, category: String) {
        viewModelScope.launch {
            dao.insert(TransactionEntity(name = name, amount = amount, category = category))
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            dao.delete(transaction)
        }
    }
}

// --- 3. PRESENTATION LAYER (MAIN ACTIVITY) ---
class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "budget_db")
            .fallbackToDestructiveMigration() // Critical for dev: wipes DB if schema changes
            .build()
    }

    private val viewModel: BudgetViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetViewModel(db.transactionDao()) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val transactions by viewModel.transactions.collectAsState(initial = emptyList())
            val totalBalance = transactions.sumOf { it.amount }

            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    BudgetApp(
                        transactions = transactions,
                        totalBalance = totalBalance,
                        onSave = { n, a, c -> viewModel.addTransaction(n, a, c) },
                        onDelete = { viewModel.deleteTransaction(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetApp(
    transactions: List<TransactionEntity>,
    totalBalance: Double,
    onSave: (String, Double, String) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FINANCE ARCHITECT", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { BalanceCard(totalBalance) }
            item { TransactionInput(onSave) }
            item { Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

            items(transactions, key = { it.id }) { item ->
                TransactionRow(item, onDelete = { onDelete(item) })
            }
        }
    }
}

// --- 4. UI COMPONENTS (THE MISSING LINKS) ---

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
            Text("Total Balance", color = Color.White.copy(alpha = 0.7f))
            Text(
                text = "$${"%.2f".format(amount)}",
                style = MaterialTheme.typography.displayMedium,
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
            Text("Add Transaction", fontWeight = FontWeight.Bold)

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
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { selection ->
                        DropdownMenuItem(
                            text = { Text(selection) },
                            onClick = { selectedCategory = selection; expanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (negative for expense)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val valDouble = amount.toDoubleOrNull()
                    if (label.isNotBlank() && valDouble != null) {
                        onSave(label, valDouble, selectedCategory)
                        label = ""; amount = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Confirm") }
        }
    }
}

@Composable
fun TransactionRow(transaction: TransactionEntity, onDelete: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(getCategoryIcon(transaction.category), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.name, fontWeight = FontWeight.Bold)
                Text(transaction.category, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = (if (transaction.amount >= 0) "+" else "-") + "$${"%.2f".format(Math.abs(transaction.amount))}",
                color = if (transaction.amount >= 0) Color(0xFF2E7D32) else Color.Red,
                fontWeight = FontWeight.Black
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector = when (category) {
    "Food" -> Icons.Default.Restaurant
    "Rent" -> Icons.Default.Home
    "Salary" -> Icons.Default.Payments
    "Transport" -> Icons.Default.DirectionsCar
    "Entertainment" -> Icons.Default.ConfirmationNumber
    else -> Icons.Default.Category
}