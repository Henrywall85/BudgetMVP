package com.henry.budgetmvp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.henry.budgetmvp.data.AppDatabase
import com.henry.budgetmvp.data.ExpenseEnvelope
import com.henry.budgetmvp.data.IncomeStream
import com.henry.budgetmvp.ui.components.*
import com.henry.budgetmvp.viewmodel.BudgetViewModel

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
                    totalPoolAmount - envelopeList.sumOf { it.targetAmount }
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
                            onConfirm = { name, target ->
                                val envelopeToSave = ExpenseEnvelope(
                                    id = editingEnvelope?.id ?: 0,
                                    name = name,
                                    targetAmount = target,
                                    allocatedAmount = editingEnvelope?.allocatedAmount ?: 0.0
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
