package com.henry.budgetmvp

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.core.view.WindowCompat

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// --- 1. DATA LAYER (V8: Multi-Row Income Streams & Expense Envelopes) ---

@Entity(tableName = "multi_income_table")
data class IncomeStream(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceName: String,
    val amount: Double,
    val frequency: String,
    val lastPayday: String
)

@Entity(tableName = "expense_envelope_table")
data class ExpenseEnvelope(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val allocatedAmount: Double = 0.0
)

@Dao
interface IncomeDao {
    @Query("SELECT * FROM multi_income_table ORDER BY id ASC")
    fun getAllIncomeStreams(): Flow<List<IncomeStream>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncomeStream(stream: IncomeStream)

    @Delete
    suspend fun deleteIncomeStream(stream: IncomeStream)

    @Query("SELECT * FROM expense_envelope_table ORDER BY name ASC")
    fun getAllEnvelopes(): Flow<List<ExpenseEnvelope>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEnvelope(envelope: ExpenseEnvelope)

    @Delete
    suspend fun deleteEnvelope(envelope: ExpenseEnvelope)
}

@Database(
    entities = [IncomeStream::class, ExpenseEnvelope::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
}

// --- 2. BUSINESS LOGIC (VIEWMODEL) ---

class BudgetViewModel(private val dao: IncomeDao) : ViewModel() {
    val incomeStreams = dao.getAllIncomeStreams()
    val envelopes = dao.getAllEnvelopes()

    fun saveIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { dao.upsertIncomeStream(stream) }
    }

    fun deleteIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { dao.deleteIncomeStream(stream) }
    }

    fun saveEnvelope(envelope: ExpenseEnvelope) {
        viewModelScope.launch { dao.upsertEnvelope(envelope) }
    }

    fun deleteEnvelope(envelope: ExpenseEnvelope) {
        viewModelScope.launch { dao.deleteEnvelope(envelope) }
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
            val envelopeList by viewModel.envelopes.collectAsState(initial = emptyList())

            var showSheet by remember { mutableStateOf(false) }
            var editingStream by remember { mutableStateOf<IncomeStream?>(null) }

            val unassignedFunds by remember(totalPoolAmount, envelopeList) {
                derivedStateOf {
                    totalPoolAmount - envelopeList.sumOf { it.allocatedAmount }
                }
            }

            var showEnvelopeSheet by remember { mutableStateOf(false) }
            var editingEnvelope by remember { mutableStateOf<ExpenseEnvelope?>(null) }

            val budgetColorScheme = lightColorScheme(
                background = Color(0xFFF9F8F3),
                surface = Color(0xFFF9F8F3),
                primary = Color(0xFF1B3B32),
                primaryContainer = Color(0xFFE2EDE4),
                onPrimaryContainer = Color(0xFF0F241F),
                surfaceVariant = Color(0xFFF0EDE4),
                onSurfaceVariant = Color(0xFF434946)
            )

            MaterialTheme(colorScheme = budgetColorScheme) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = budgetColorScheme.background.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                    }
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("PAYCHECK BUDGET", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium) },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = budgetColorScheme.background)
                        )
                    },
                    containerColor = budgetColorScheme.background
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
                            TotalPoolCard(unassignedFunds)
                        }

                        // (2) MULTIPLE INCOME DETAILS CARDS
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column {
                                    Text(
                                        text = "Income",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                                    )

                                    if (streams.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "No income sources configured yet.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )

                                            OutlinedButton(
                                                onClick = {
                                                    editingStream = null
                                                    showSheet = true
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Get Started")
                                            }
                                        }
                                    } else {
                                        streams.forEachIndexed { index, stream ->
                                            IncomeDetailsCard(
                                                stream = stream,
                                                onClick = {
                                                    editingStream = stream
                                                    showSheet = true
                                                },
                                                onDelete = { viewModel.deleteIncomeStream(stream) }
                                            )
                                            if (index < streams.lastIndex) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    editingStream = null
                                                    showSheet = true
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Income Source", style = MaterialTheme.typography.labelLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // (3) UNIFIED EXPENSE ENVELOPES MODULE
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column {
                                    Text(
                                        text = "Expense Envelopes",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                                    )

                                    if (envelopeList.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "No envelopes created yet.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )

                                            OutlinedButton(
                                                onClick = {
                                                    editingEnvelope = null
                                                    showEnvelopeSheet = true
                                                },
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Create Envelope")
                                            }
                                        }
                                    } else {
                                        // FIXED RENDERING LOOPS FOR ACTIVE CATEGORIES
                                        envelopeList.forEachIndexed { index, envelope ->
                                            EnvelopeDetailsRow(
                                                envelope = envelope,
                                                onClick = {
                                                    editingEnvelope = envelope
                                                    showEnvelopeSheet = true
                                                }
                                            )
                                            if (index < envelopeList.lastIndex) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    editingEnvelope = null
                                                    showEnvelopeSheet = true
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Expense Envelope", style = MaterialTheme.typography.labelLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showSheet) {
                        IncomeEntrySheet(
                            targetStream = editingStream,
                            onDismiss = { showSheet = false },
                            onConfirm = { sourceName, amount, frequency, selectedDate ->
                                val streamToSave = IncomeStream(
                                    id = editingStream?.id ?: 0,
                                    sourceName = sourceName,
                                    amount = amount,
                                    frequency = frequency,
                                    lastPayday = selectedDate
                                )
                                viewModel.saveIncomeStream(streamToSave)
                                showSheet = false
                            },
                            onDelete = {
                                editingStream?.let { viewModel.deleteIncomeStream(it)}
                                showSheet = false
                            }
                        )
                    }

                    if (showEnvelopeSheet) {
                        EnvelopeEntrySheet(
                            targetEnvelope = editingEnvelope,
                            onDismiss = { showEnvelopeSheet = false },
                            onConfirm = { name, target, allocated ->
                                val envelopeToSave = ExpenseEnvelope(
                                    id = editingEnvelope?.id ?: 0,
                                    name = name,
                                    targetAmount = target,
                                    allocatedAmount = allocated
                                )
                                viewModel.saveEnvelope(envelopeToSave)
                                showEnvelopeSheet = false
                            },
                            onDelete = {
                                editingEnvelope?.let { viewModel.deleteEnvelope(it) }
                                showEnvelopeSheet = false
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
            Text(
                text = "Total Funds Available (Ready to Assign)",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer (modifier = Modifier.height(4.dp))
            Text(
                text = "$${"%,.2f".format(total)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun IncomeDetailsCard(stream: IncomeStream, onClick: () -> Unit, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stream.sourceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Next: ${calculateNextPayday(stream.lastPayday, stream.frequency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text("Cycle Window: ${stream.frequency}", style = MaterialTheme.typography.bodyMedium)
            Text("Amount: $${"%,.2f".format(stream.amount)}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun EnvelopeDetailsRow(envelope: ExpenseEnvelope, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(envelope.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$${"%,.2f".format(envelope.allocatedAmount)} / $${"%,.2f".format(envelope.targetAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val remaining = envelope.targetAmount - envelope.allocatedAmount
            Text(
                text = if (remaining <= 0) "Fully Funded!" else "Needs $${"%,.2f".format(remaining)} more",
                style = MaterialTheme.typography.bodySmall,
                color = if (remaining <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
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
    var amount by remember { mutableStateOf(targetStream?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var frequency by remember { mutableStateOf(targetStream?.frequency ?: "Please Enter Frequency") }
    var lastPayday by remember { mutableStateOf(targetStream?.lastPayday ?: LocalDate.now().toString()) }
    var selectedDateIso by remember { mutableStateOf(targetStream?.lastPayday ?: LocalDate.now().toString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val frequencies = listOf("Weekly", "Bi-Weekly", "Monthly")
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            source.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && frequency != "Please Enter Frequency" && lastPayday.isNotBlank()
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) { amount = input }
                },
                label = { Text("Paycheck Amount") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeEntrySheet(
    targetEnvelope: ExpenseEnvelope?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(targetEnvelope?.name ?: "") }
    var targetAmount by remember { mutableStateOf(targetEnvelope?.targetAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var allocatedAmount by remember { mutableStateOf(targetEnvelope?.allocatedAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }

    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            name.isNotBlank() && (targetAmount.toDoubleOrNull() ?: 0.0) > 0
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
                text = if (targetEnvelope != null) "Edit ${targetEnvelope.name}" else "Add New Envelope",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Envelope Name (e.g., Rent, Groceries)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            OutlinedTextField(
                value = targetAmount,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) { targetAmount = input }
                },
                label = { Text("Monthly Target Amount") },
                placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                visualTransformation = commaTransformation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = allocatedAmount,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) { allocatedAmount = input }
                },
                label = { Text("Currently Allocated (Optional)") },
                placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                visualTransformation = commaTransformation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onConfirm(name, targetAmount.toDoubleOrNull() ?: 0.0, allocatedAmount.toDoubleOrNull() ?: 0.0) },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (targetEnvelope != null) "Update Envelope" else "Save Envelope")
            }

            if (targetEnvelope != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Envelope")
                }
            }
        }
    }
}

class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val parts = originalText.split(".")
        val integerPart = parts[0]
        val fractionalPart = if (parts.size > 1) "." + parts[1] else ""

        val formattedInteger = integerPart.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()

        val newText = formattedInteger + fractionalPart

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset > integerPart.length) {
                    return formattedInteger.length + (offset - integerPart.length)
                }
                var originalProcessed = 0
                var transformedProcessed = 0
                for (char in formattedInteger) {
                    if (char != ',') {
                        originalProcessed++
                    }
                    transformedProcessed++
                    if (originalProcessed == offset) return transformedProcessed
                }
                return transformedProcessed
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val actualOffset = offset.coerceAtMost(newText.length)
                var originalIdx = 0
                for (i in 0 until actualOffset) {
                    if (newText[i] != ',') {
                        originalIdx++
                    }
                }
                return originalIdx
            }
        }

        return TransformedText(AnnotatedString(newText), offsetMapping)
    }
}

fun calculateNextPayday(lastPaydayIso: String, frequency: String): String {
    return try {
        val lastDate = LocalDate.parse(lastPaydayIso)
        val nextDate = when (frequency) {
            "Weekly" -> lastDate.plusWeeks(1)
            "Bi-Weekly" -> lastDate.plusWeeks(2)
            "Monthly" -> lastDate.plusMonths(1)
            else -> lastDate
        }
        nextDate.format(DateTimeFormatter.ofPattern("MMM dd"))
    } catch (e: Exception) {
        "TBD"
    }
}
