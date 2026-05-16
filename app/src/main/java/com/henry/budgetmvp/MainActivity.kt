package com.henry.budgetmvp

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// --- 1. DATA LAYER (V6: Multi-Row Income Streams) ---

@Entity(tableName = "multi_income_table")
data class IncomeStream(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceName: String,
    val amount: Double,
    val frequency: String
)

@Dao
interface IncomeDao {
    @Query("SELECT * FROM multi_income_table ORDER BY id ASC")
    fun getAllIncomeStreams(): Flow<List<IncomeStream>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncomeStream(stream: IncomeStream)

    @Delete
    suspend fun deleteIncomeStream(stream: IncomeStream)
}

@Database(entities = [IncomeStream::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
}

// --- 2. BUSINESS LOGIC (VIEWMODEL) ---

class BudgetViewModel(private val dao: IncomeDao) : ViewModel() {
    val incomeStreams = dao.getAllIncomeStreams()

    fun saveIncomeStream(stream: IncomeStream) {
        viewModelScope.launch {
            dao.upsertIncomeStream(stream)
        }
    }

    fun deleteIncomeStream(stream: IncomeStream) {
        viewModelScope.launch {
            dao.deleteIncomeStream(stream)
        }
    }
}

// --- 3. PRESENTATION LAYER (UI) ---

class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "budget_db")
            .fallbackToDestructiveMigration().build()
    }

    private val viewModel: BudgetViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetViewModel(db.incomeDao()) as T
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val streams by viewModel.incomeStreams.collectAsState(initial = emptyList())
            val totalPoolAmount = streams.sumOf { it.amount }

            var showSheet by remember { mutableStateOf(false) }
            var editingStream by remember { mutableStateOf<IncomeStream?>(null) }

            MaterialTheme {
                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text("PAYCHECK BUDGET", fontWeight = FontWeight.Bold) }) }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // (1) THE TOTAL POOL CARD
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            TotalPoolCard(totalPoolAmount)
                        }

                        item { Text("Income Streams", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }

                        // (2) MULTIPLE INCOME DETAILS CARDS
                        if (streams.isEmpty()) {
                            item {
                                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("No income sources configured yet.", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        } else {
                            items(streams) { stream ->
                                IncomeDetailsCard(
                                    stream = stream,
                                    onClick = {
                                        editingStream = stream // Pass existing item to switch sheet to EDIT mode
                                        showSheet = true
                                    },
                                    onDelete = { viewModel.deleteIncomeStream(stream) }
                                )
                            }
                        }

                        // (3) THE BUTTON TO CREATE FROM SCRATCH
                        item {
                            Button(
                                onClick = {
                                    editingStream = null // Clear editing targets to force CREATE mode
                                    showSheet = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add New Income Source")
                            }
                        }
                    }

                    if (showSheet) {
                        IncomeEntrySheet(
                            targetStream = editingStream,
                            onDismiss = { showSheet = false },
                            onConfirm = { sourceName, amount, frequency ->
                                // Maintain existing ID if editing, or default to 0 for a fresh autogenerated insert
                                val streamToSave = IncomeStream(
                                    id = editingStream?.id ?: 0,
                                    sourceName = sourceName,
                                    amount = amount,
                                    frequency = frequency
                                )
                                viewModel.saveIncomeStream(streamToSave)
                                showSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TotalPoolCard(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Funds Available (Ready to Assign)", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$${"%.2f".format(total)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun IncomeDetailsCard(stream: IncomeStream, onClick: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stream.sourceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Cycle Window: ${stream.frequency}", style = MaterialTheme.typography.bodyMedium)
                Text("Amount: $${"%.2f".format(stream.amount)}", style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Source",
                        tint = MaterialTheme.colorScheme.error
                    )
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
    onConfirm: (String, Double, String) -> Unit
) {
    var source by remember { mutableStateOf(targetStream?.sourceName ?: "") }
    var amount by remember { mutableStateOf(targetStream?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var frequency by remember { mutableStateOf(targetStream?.frequency ?: "Monthly") }

    val frequencies = listOf("Weekly", "Bi-Weekly", "Monthly")
    var expanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                // INJECTION: Allows the form to scroll dynamically if the keyboard squeezes the screen space
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
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
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Paycheck Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Budget Cycle Window") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    frequencies.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                frequency = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (source.isNotBlank() && amt > 0) onConfirm(source, amt, frequency)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (targetStream != null) "Save Changes" else "Add Source to Pool")
            }
        }
    }
}