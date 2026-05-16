package com.henry.budgetmvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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

// Room Entity
@Entity
data class IncomeProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val amount: Double,
    val frequency: String
)

// Room DAO
@Dao
interface IncomeDao {
    @Query("SELECT * FROM IncomeProfile WHERE id = 1")
    fun getIncomeProfile(): Flow<IncomeProfile?>

    @Upsert
    suspend fun updateIncome(profile: IncomeProfile)
}

// Room Database
@Database(entities = [IncomeProfile::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
}

// ViewModel
class BudgetViewModel(private val dao: IncomeDao) : ViewModel() {
    val incomeProfile = dao.getIncomeProfile()

    fun updateIncome(name: String, amount: Double, frequency: String) {
        viewModelScope.launch {
            dao.updateIncome(IncomeProfile(name = name, amount = amount, frequency = frequency))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "budget_db")
            .fallbackToDestructiveMigration(true).build()
    }

    private val viewModel: BudgetViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BudgetViewModel(db.incomeDao()) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val profile by viewModel.incomeProfile.collectAsState(initial = null)
            var showSheet by remember { mutableStateOf(false) }

            // NEW LOGIC: Just take the amount as is.
            // The "Frequency" is now just a label for the user's context.
            val cycleTotal = profile?.amount ?: 0.0
            val frequencyLabel = profile?.frequency ?: "Cycle"

            MaterialTheme {
                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text("PAYCHECK BUDGET", fontWeight = FontWeight.Bold) }) }
                ) { padding ->
                    Column(
                        modifier = Modifier.padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // THE MAIN TOTAL CARD (Updated to show Cycle)
                        CurrentCycleCard(cycleTotal, frequencyLabel)

                        // THE FIXED INCOME SETTINGS CARD
                        IncomeSettingsCard(
                            profile = profile,
                            onEdit = { showSheet = true }
                        )

                        if (showSheet) {
                            IncomeEntrySheet(
                                currentProfile = profile,
                                onDismiss = { showSheet = false },
                                onConfirm = { name, amt, freq ->
                                    viewModel.updateIncome(name, amt, freq)
                                    showSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentCycleCard(total: Double, label: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ready to Assign ($label)", style = MaterialTheme.typography.labelLarge)
            Text("$${"%.2f".format(total)}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
            Text("Give every dollar a job for this $label", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun IncomeSettingsCard(profile: IncomeProfile?, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Income Settings", style = MaterialTheme.typography.titleMedium)
                Text(profile?.name ?: "Not Set", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeEntrySheet(
    currentProfile: IncomeProfile?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var source by remember { mutableStateOf(currentProfile?.name ?: "") }
        var amount by remember { mutableStateOf(currentProfile?.amount?.toString() ?: "") }
        var frequency by remember { mutableStateOf(currentProfile?.frequency ?: "Monthly") }

        // Dropdown State
        val frequencies = listOf("Weekly", "Bi-Weekly", "Monthly")
        var expanded by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Configure Income", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text("Income Source") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount per Paycheck") },
                // RESTORED: Specific numeric keyboard
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // THE DROPDOWN LIST
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pay Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Save Configuration") }
        }
    }
}
