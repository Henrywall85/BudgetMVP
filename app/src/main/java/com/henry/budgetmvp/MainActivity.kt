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

// --- 1. DATA LAYER (V6: Multi-Row Income Streams) ---

@Entity(tableName = "multi_income_table")
data class IncomeStream(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceName: String,
    val amount: Double,
    val frequency: String,
    val lastPayday: String
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

@Database(entities = [IncomeStream::class], version = 7, exportSchema = false)
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
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = budgetColorScheme.background
                            )
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
                            TotalPoolCard(totalPoolAmount)
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
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
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
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                ),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                                )
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
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    editingStream = null
                                                    showSheet = true
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) // Adapts beautifully to text color
                                                ),
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
    val nextPayDate = calculateNextPayday(stream.lastPayday, stream.frequency)
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

    // PERF FIX 1: Cache the transformation object so it doesn't recreate allocation leaks during typing loops
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    // PERF FIX 2: Isolate validation checks with derivedStateOf to prevent frame recomposition stuttering
    val isFormValid by remember {
        derivedStateOf {
            source.isNotBlank() &&
                    (amount.toDoubleOrNull() ?: 0.0) > 0 &&
                    frequency != "Please Enter Frequency" &&
                    lastPayday.isNotBlank()
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
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) {
                        amount = input
                    }
                },
                label = { Text("Paycheck Amount") },
                placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                visualTransformation = commaTransformation, // Using optimized token reference
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
                // Transparent click catcher layer draped directly over the input box area
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            focusManager.clearFocus()
                            showDatePicker = true
                        }
                )
            }

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
                    if (isFormValid) onConfirm(source, amt, frequency, selectedDateIso)
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (targetStream != null) "Save Changes" else "Add Source to Pool")
            }

            if (targetStream != null) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = "Delete income",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    // INJECTION: Core Material 3 Overlay Calendar Modal Element
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(selectedDateIso)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateIso = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}



class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isBlank()) return TransformedText(text, OffsetMapping.Identity)

        val parts = originalText.split(".")
        val intPart = parts[0]
        val hasDecimal = originalText.contains(".")
        val decPart = if (parts.size > 1) parts[1] else ""

        val formattedInt = intPart.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()

        val formattedText = buildString {
            append(formattedInt)
            if (hasDecimal) append(".")
            append(decPart)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val subStr = originalText.substring(0, offset).split(".")[0]
                val commasBefore = (subStr.length - 1) / 3

                val decimalIndex = originalText.indexOf('.')
                return if (decimalIndex != -1 && offset > decimalIndex) {
                    val wholeNumberCommas = (decimalIndex - 1) / 3
                    offset + wholeNumberCommas
                } else {
                    offset + commasBefore
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val decimalIndexTransformed = formattedText.indexOf('.')
                val actualOffset = if (decimalIndexTransformed != -1 && offset > decimalIndexTransformed) {
                    val subStrInt = formattedText.substring(0, decimalIndexTransformed)
                    val commasBefore = subStrInt.count { it == ',' }
                    offset - commasBefore
                } else {
                    val subStr = formattedText.substring(0, offset.coerceAtMost(formattedText.length))
                    val commasBefore = subStr.count { it == ',' }
                    offset - commasBefore
                }
                return actualOffset.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(androidx.compose.ui.text.AnnotatedString(formattedText), offsetMapping)
    }
}

fun calculateNextPayday(lastPaydayStr: String, frequency: String): String {
    return try {
        val lastDate = LocalDate.parse(lastPaydayStr)
        val today = LocalDate.now()

        // Project forward until we find the first cycle date that lands in the future
        var nextDate = when (frequency) {
            "Weekly" -> lastDate.plusWeeks(1)
            "Bi-Weekly" -> lastDate.plusWeeks(2)
            "Monthly" -> lastDate.plusMonths(1)
            else -> lastDate
        }

        // Catch-up mechanic: If the last payday entered was weeks ago, project it forward
        while (nextDate.isBefore(today)) {
            nextDate = when (frequency) {
                "Weekly" -> nextDate.plusWeeks(1)
                "Bi-Weekly" -> nextDate.plusWeeks(2)
                "Monthly" -> nextDate.plusMonths(1)
                else -> nextDate
            }
        }

        val displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        nextDate.format(displayFormatter)
    } catch (e: Exception) {
        "Pending Date"
    }
}