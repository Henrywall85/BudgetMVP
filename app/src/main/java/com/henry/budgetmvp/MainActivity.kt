package com.henry.budgetmvp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AccountBalanceWallet
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

enum class Screen {
    BUDGET, TRANSACTIONS
}

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

            var currentScreen by remember { mutableStateOf(Screen.BUDGET) }

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

            var showItemDetailSheet by remember { mutableStateOf(false) }
            var selectedItemForDetail by remember { mutableStateOf<EnvelopeItem?>(null) }

            var showTransactionEditSheet by remember { mutableStateOf(false) }
            var editingTransaction by remember { mutableStateOf<BudgetTransaction?>(null) }

            val collapsedCategories = remember { mutableStateListOf<Int>() }

            val budgetColorScheme = lightColorScheme(
                background = Color(0xFFF3F4F6),
                surface = Color(0xFFFFFFFF),
                primary = Color(0xFF111827),
                primaryContainer = Color(0xFFF9FAFB),
                onPrimaryContainer = Color(0xFF111827),
                surfaceVariant = Color(0xFFFFFFFF),
                onSurfaceVariant = Color(0xFF374151),
                onSurface = Color(0xFF111827),
                outline = Color(0xFFE5E7EB)
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
                            title = {
                                Text(
                                    text = if (currentScreen == Screen.BUDGET) "PAYCHECK BUDGET" else "TRANSACTIONS",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = budgetColorScheme.background)
                        )
                    },
                    bottomBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .height(80.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Budget Button
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { currentScreen = Screen.BUDGET },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Budget",
                                        modifier = Modifier.size(28.dp),
                                        tint = if (currentScreen == Screen.BUDGET) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Budget",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (currentScreen == Screen.BUDGET) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Transactions Button
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { currentScreen = Screen.TRANSACTIONS },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = "Transactions",
                                        modifier = Modifier.size(28.dp),
                                        tint = if (currentScreen == Screen.TRANSACTIONS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Transactions",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (currentScreen == Screen.TRANSACTIONS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    containerColor = budgetColorScheme.background
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        if (currentScreen == Screen.BUDGET) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
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
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Column {
                                            Text(
                                                text = "INCOME SOURCES",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                            )

                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 0.dp),
                                                color = MaterialTheme.colorScheme.outline
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
                                                            color = MaterialTheme.colorScheme.outline
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
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                                        modifier = Modifier.padding(horizontal = 0.dp),
                                                        color = MaterialTheme.colorScheme.outline
                                                    )

                                                    categoryWithItems.items.forEachIndexed { index, item ->
                                                        val spentAmount = transactions
                                                            .filter { it.type == TransactionType.EXPENSE && it.itemId == item.id }
                                                            .sumOf { it.amount }

                                                        EnvelopeItemRow(
                                                            item = item,
                                                            spentAmount = spentAmount,
                                                            onClick = {
                                                                selectedItemForDetail = item
                                                                showItemDetailSheet = true
                                                            }
                                                        )
                                                        if (index < categoryWithItems.items.lastIndex) {
                                                            HorizontalDivider(
                                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                                color = MaterialTheme.colorScheme.outline
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
                        } else {
                            TransactionPage(
                                categoriesWithItems = categoriesWithItems,
                                onConfirm = { type, amount, date, itemId ->
                                    val transaction = BudgetTransaction(
                                        type = type,
                                        amount = amount,
                                        date = date,
                                        itemId = itemId
                                    )
                                    viewModel.saveTransaction(transaction)
                                    // Optionally show a confirmation toast or navigate back
                                    currentScreen = Screen.BUDGET
                                }
                            )
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

                    if (selectedItemForDetail != null && showItemDetailSheet) {
                        ItemDetailSheet(
                            item = selectedItemForDetail!!,
                            transactions = transactions.filter { it.itemId == selectedItemForDetail!!.id },
                            onDismiss = { showItemDetailSheet = false },
                            onEditItem = {
                                editingItem = selectedItemForDetail
                                activeCategoryId = selectedItemForDetail!!.categoryId
                                showItemSheet = true
                                showItemDetailSheet = false
                            },
                            onEditTransaction = { transaction ->
                                editingTransaction = transaction
                                showTransactionEditSheet = true
                                showItemDetailSheet = false
                            }
                        )
                    }

                    if (showTransactionEditSheet && editingTransaction != null) {
                        TransactionEntrySheet(
                            targetTransaction = editingTransaction,
                            categoriesWithItems = categoriesWithItems,
                            onDismiss = { showTransactionEditSheet = false },
                            onConfirm = { type, amount, date, itemId ->
                                val transaction = editingTransaction!!.copy(
                                    type = type,
                                    amount = amount,
                                    date = date,
                                    itemId = itemId
                                )
                                viewModel.saveTransaction(transaction)
                                showTransactionEditSheet = false
                            },
                            onDelete = {
                                viewModel.deleteTransaction(editingTransaction!!)
                                showTransactionEditSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}
