package com.henry.budgetmvp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import com.henry.budgetmvp.data.BudgetCategory
import com.henry.budgetmvp.data.EnvelopeItem
import com.henry.budgetmvp.data.IncomeStream
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.TransactionType
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
                return BudgetViewModel(db.budgetDao()) as T
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val streams by viewModel.incomeStreams.collectAsState(initial = emptyList())
            val totalPoolAmount = streams.sumOf { it.amount }
            val categoriesWithItems by viewModel.categoriesWithItems.collectAsState(initial = emptyList())
            val transactions by viewModel.transactions.collectAsState(initial = emptyList())

            var showIncomeSheet by remember { mutableStateOf(false) }
            var editingStream by remember { mutableStateOf<IncomeStream?>(null) }

            val unassignedFunds by remember(totalPoolAmount, categoriesWithItems) {
                derivedStateOf {
                    val totalPlanned = categoriesWithItems.sumOf { cat ->
                        cat.items.sumOf { it.targetAmount }
                    }
                    totalPoolAmount - totalPlanned
                }
            }

            var showCategorySheet by remember { mutableStateOf(false) }
            var editingCategory by remember { mutableStateOf<BudgetCategory?>(null) }

            var showItemSheet by remember { mutableStateOf(false) }
            var editingItem by remember { mutableStateOf<EnvelopeItem?>(null) }
            var activeCategoryId by remember { mutableStateOf<Int?>(null) }

            var showTransactionSheet by remember { mutableStateOf(false) }

            val collapsedCategories = remember { mutableStateListOf<Int>() }

            val budgetColorScheme = lightColorScheme(
                background = Color(0xFFF9F8F3),
                surface = Color(0xFFF9F8F3),
                primary = Color(0xFF1B3B32),
                primaryContainer = Color(0xFFD6E4D9),
                onPrimaryContainer = Color(0xFF0F241F),
                surfaceVariant = Color(0xFFECEADF),
                onSurfaceVariant = Color(0xFF3F4441)
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
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showTransactionSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Transaction")
                            }
                        }
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                                    showIncomeSheet = true
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
                                                    showIncomeSheet = true
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
                                                    showIncomeSheet = true
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

                        // (3) BUDGET CATEGORIES & ENVELOPE ITEMS
                        if (categoriesWithItems.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "No categories created yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                editingCategory = null
                                                showCategorySheet = true
                                            },
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Create Category")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(categoriesWithItems) { categoryWithItems ->
                                val isExpanded = !collapsedCategories.contains(categoryWithItems.category.id)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(bottom = if (isExpanded) 8.dp else 0.dp)) {
                                        CategoryHeader(
                                            category = categoryWithItems.category,
                                            isExpanded = isExpanded,
                                            onToggleExpand = {
                                                if (isExpanded) {
                                                    collapsedCategories.add(categoryWithItems.category.id)
                                                } else {
                                                    collapsedCategories.remove(categoryWithItems.category.id)
                                                }
                                            },
                                            onEditCategory = {
                                                editingCategory = categoryWithItems.category
                                                showCategorySheet = true
                                            },
                                            onAddItem = {
                                                activeCategoryId = categoryWithItems.category.id
                                                editingItem = null
                                                showItemSheet = true
                                            }
                                        )

                                        if (isExpanded) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                                            )

                                            categoryWithItems.items.forEachIndexed { index, item ->
                                                val spentAmount = transactions
                                                    .filter { it.type == TransactionType.EXPENSE && it.itemId == item.id }
                                                    .sumOf { it.amount }

                                                EnvelopeItemRow(
                                                    item = item,
                                                    spentAmount = spentAmount,
                                                    onClick = {
                                                        activeCategoryId = categoryWithItems.category.id
                                                        editingItem = item
                                                        showItemSheet = true
                                                    }
                                                )
                                                if (index < categoryWithItems.items.lastIndex) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            editingCategory = null
                                            showCategorySheet = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add New Category", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }

                    if (showIncomeSheet) {
                        IncomeEntrySheet(
                            targetStream = editingStream,
                            onDismiss = { showIncomeSheet = false },
                            onConfirm = { sourceName, amount, frequency, selectedDate ->
                                val streamToSave = IncomeStream(
                                    id = editingStream?.id ?: 0,
                                    sourceName = sourceName,
                                    amount = amount,
                                    frequency = frequency,
                                    lastPayday = selectedDate
                                )
                                viewModel.saveIncomeStream(streamToSave)
                                showIncomeSheet = false
                            },
                            onDelete = {
                                editingStream?.let { viewModel.deleteIncomeStream(it)}
                                showIncomeSheet = false
                            }
                        )
                    }

                    if (showCategorySheet) {
                        CategoryEntrySheet(
                            targetCategory = editingCategory,
                            onDismiss = { showCategorySheet = false },
                            onConfirm = { name ->
                                val categoryToSave = BudgetCategory(
                                    id = editingCategory?.id ?: 0,
                                    name = name
                                )
                                viewModel.saveCategory(categoryToSave)
                                showCategorySheet = false
                            },
                            onDelete = {
                                editingCategory?.let { viewModel.deleteCategory(it) }
                                showCategorySheet = false
                            }
                        )
                    }

                    if (showItemSheet) {
                        EnvelopeItemEntrySheet(
                            categoryId = activeCategoryId ?: 0,
                            targetItem = editingItem,
                            onDismiss = { showItemSheet = false },
                            onConfirm = { name, target ->
                                val itemToSave = EnvelopeItem(
                                    id = editingItem?.id ?: 0,
                                    categoryId = activeCategoryId ?: 0,
                                    name = name,
                                    targetAmount = target,
                                    allocatedAmount = editingItem?.allocatedAmount ?: 0.0
                                )
                                viewModel.saveEnvelopeItem(itemToSave)
                                showItemSheet = false
                            },
                            onDelete = {
                                editingItem?.let { viewModel.deleteEnvelopeItem(it) }
                                showItemSheet = false
                            }
                        )
                    }

                    if (showTransactionSheet) {
                        TransactionEntrySheet(
                            categoriesWithItems = categoriesWithItems,
                            onDismiss = { showTransactionSheet = false },
                            onConfirm = { type, amount, date, itemId ->
                                val transaction = BudgetTransaction(
                                    type = type,
                                    amount = amount,
                                    date = date,
                                    itemId = itemId
                                )
                                viewModel.saveTransaction(transaction)
                                showTransactionSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}
