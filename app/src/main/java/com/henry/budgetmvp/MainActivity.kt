package com.henry.budgetmvp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
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
import java.time.LocalDate
import com.henry.budgetmvp.ui.components.*
import com.henry.budgetmvp.viewmodel.AuthViewModel
import com.henry.budgetmvp.viewmodel.BudgetViewModel
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class Screen {
    LOGIN, SIGNUP, BUDGET, TRANSACTIONS, HOUSEHOLD, SETTINGS
}

class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "budget_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    private val viewModel: BudgetViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetViewModel(db.budgetDao()) as T
            }
        }
    }

    private val authViewModel: AuthViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val streams by viewModel.incomeStreams.collectAsState(initial = emptyList())
            val categoriesWithItems by viewModel.categoriesWithItems.collectAsState(initial = emptyList())
            val transactions by viewModel.transactions.collectAsState(initial = emptyList())
            val pendingInvites by viewModel.pendingInvites.collectAsState()
            val hasAnyBudgetData by viewModel.hasAnyBudgetData.collectAsState(initial = false)

            var currentDate by remember { mutableStateOf(LocalDate.now()) }
            val currentMonthYear = remember(currentDate) { 
                currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM")) 
            }

            val totalPlannedIncomeForMonth = remember(streams) {
                streams.sumOf { it.monthlyAmount }
            }

            val filteredTransactions = remember(transactions, currentDate) {
                transactions.filter { tx ->
                    try {
                        val txDate = LocalDate.parse(tx.date)
                        txDate.month == currentDate.month && txDate.year == currentDate.year
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            val totalReceivedIncome = remember(filteredTransactions) {
                filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            }

            val user by authViewModel.userState.collectAsState()
            val authLoading by authViewModel.loading.collectAsState()
            val authError by authViewModel.error.collectAsState()

            val isSyncing by viewModel.isSyncing.collectAsState()
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

            val packageInfo = remember {
                try {
                    packageManager.getPackageInfo(packageName, 0)
                } catch (e: Exception) {
                    null
                }
            }
            val versionName = packageInfo?.versionName ?: "1.0"

            // Pass the userId to the ViewModel
            LaunchedEffect(user) {
                viewModel.setUserId(user?.uid, user?.email)
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.statusMessage.collect { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }

            var currentScreen by remember { mutableStateOf(if (user != null) Screen.BUDGET else Screen.LOGIN) }

            // Sync screen state with auth state
            LaunchedEffect(user) {
                if (user == null) {
                    currentScreen = Screen.LOGIN
                } else if (currentScreen == Screen.LOGIN || currentScreen == Screen.SIGNUP) {
                    currentScreen = Screen.BUDGET
                }
            }

            var showIncomeSheet by remember { mutableStateOf(false) }
            var editingStream by remember { mutableStateOf<IncomeStream?>(null) }
            
            var forceShowBudgetForMonth by remember { mutableStateOf<String?>(null) }

            val unassignedFunds by remember(totalReceivedIncome, totalPlannedIncomeForMonth, categoriesWithItems) {
                derivedStateOf {
                    val totalPlannedExpenses = categoriesWithItems.sumOf { cat ->
                        cat.items.sumOf { it.targetAmount }
                    }
                    totalPlannedIncomeForMonth - totalPlannedExpenses
                }
            }

            var showCategorySheet by remember { mutableStateOf(false) }
            var editingCategory by remember { mutableStateOf<BudgetCategory?>(null) }

            var showItemSheet by remember { mutableStateOf(false) }
            var editingItem by remember { mutableStateOf<EnvelopeItem?>(null) }
            var activeCategoryId by remember { mutableStateOf<String?>(null) }

            var showItemDetailSheet by remember { mutableStateOf(false) }
            var selectedItemForDetail by remember { mutableStateOf<EnvelopeItem?>(null) }

            var showTransactionEditSheet by remember { mutableStateOf(false) }
            var editingTransaction by remember { mutableStateOf<BudgetTransaction?>(null) }

            var showLogoutDialog by remember { mutableStateOf(false) }

            var showIncomeDetailSheet by remember { mutableStateOf(false) }
            var selectedStreamForDetail by remember { mutableStateOf<IncomeStream?>(null) }

            val collapsedCategories = remember { mutableStateListOf<String>() }

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
                        if (currentScreen != Screen.LOGIN && currentScreen != Screen.SIGNUP) {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = when (currentScreen) {
                                            Screen.BUDGET -> "PAYCHECK BUDGET"
                                            Screen.HOUSEHOLD -> "HOUSEHOLD MEMBERS"
                                            Screen.SETTINGS -> "SETTINGS"
                                            else -> "TRANSACTIONS"
                                        },
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                navigationIcon = {
                                    if (currentScreen == Screen.HOUSEHOLD || currentScreen == Screen.SETTINGS) {
                                        IconButton(onClick = { 
                                            currentScreen = if (currentScreen == Screen.HOUSEHOLD) Screen.SETTINGS else Screen.BUDGET 
                                        }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = { currentScreen = Screen.SETTINGS }) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    // Notifications Bell
                                    IconButton(onClick = { currentScreen = Screen.HOUSEHOLD }) {
                                        BadgedBox(
                                            badge = {
                                                if (pendingInvites.isNotEmpty()) {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (pendingInvites.isNotEmpty()) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                                contentDescription = "Notifications",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Sync Status Indicator
                                    if (isSyncing) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Syncing",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Synced",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(onClick = { showLogoutDialog = true }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Logout",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = budgetColorScheme.background)
                            )
                        }
                    },
                    bottomBar = {
                        if (currentScreen != Screen.LOGIN && currentScreen != Screen.SIGNUP) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .navigationBarsPadding()
                                        .height(90.dp)
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
                        }
                    },
                    containerColor = budgetColorScheme.background
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        when (currentScreen) {
                            Screen.LOGIN -> {
                                LoginPage(
                                    versionName = versionName,
                                    loading = authLoading,
                                    errorMessage = authError,
                                    onLoginClick = { email, password ->
                                        authViewModel.signIn(email, password) { success ->
                                            if (success) currentScreen = Screen.BUDGET
                                        }
                                    },
                                    onNavigateToSignup = { currentScreen = Screen.SIGNUP }
                                )
                            }
                            Screen.SIGNUP -> {
                                SignupPage(
                                    versionName = versionName,
                                    loading = authLoading,
                                    errorMessage = authError,
                                    onSignupClick = { name, email, password ->
                                        authViewModel.signUp(email, password) { success ->
                                            if (success) currentScreen = Screen.BUDGET
                                        }
                                    },
                                    onNavigateToLogin = { currentScreen = Screen.LOGIN }
                                )
                            }
                            Screen.HOUSEHOLD -> {
                                val members by viewModel.householdMembers.collectAsState()
                                
                                HouseholdPage(
                                    currentUserId = user?.uid ?: "",
                                    members = members,
                                    pendingInvites = pendingInvites,
                                    onInviteMember = { viewModel.inviteMember(it) },
                                    onAcceptInvite = { viewModel.acceptInvite(it) },
                                    onDeclineInvite = { viewModel.declineInvite(it) },
                                    onRefresh = { viewModel.refreshInvites() },
                                    onLeaveHousehold = { viewModel.leaveHousehold() },
                                    onResetData = { viewModel.resetAllHouseholdData() }
                                )
                            }
                            Screen.SETTINGS -> {
                                SettingsPage(
                                    onNavigateToHousehold = { currentScreen = Screen.HOUSEHOLD },
                                    onStartFromScratch = { 
                                        viewModel.resetAllHouseholdData()
                                        currentScreen = Screen.BUDGET
                                    }
                                )
                            }
                            Screen.BUDGET -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(20.dp),
                                        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                                    ) {
                                        // (1) THE TOTAL POOL CARD
                                        item {
                                            Spacer(modifier = Modifier.height(0.dp))
                                            TotalPoolCard(
                                                total = unassignedFunds,
                                                currentDate = currentDate,
                                                onPreviousMonth = { 
                                                    currentDate = currentDate.minusMonths(1)
                                                    viewModel.setMonthYear(currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                                                },
                                                onNextMonth = { 
                                                    currentDate = currentDate.plusMonths(1)
                                                    viewModel.setMonthYear(currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                                                },
                                                onMonthClick = {
                                                    currentDate = LocalDate.now()
                                                    viewModel.setMonthYear(currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                                                }
                                            )
                                        }

                                        // THE "CREATE BUDGET" SCREEN
                                        // Only show if the current month is empty AND the user has data in other months
                                        // AND they haven't explicitly asked to "start fresh" for this month.
                                        if (streams.isEmpty() && categoriesWithItems.isEmpty() && hasAnyBudgetData && forceShowBudgetForMonth != currentMonthYear) {
                                            item {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    Text(
                                                        text = "Lets create your ${currentDate.format(DateTimeFormatter.ofPattern("MMMM"))} budget.",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Button(
                                                        onClick = { 
                                                            val prevMonth = currentDate.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
                                                            viewModel.copyBudget(prevMonth, currentMonthYear)
                                                            // If copy results in nothing (e.g. first setup), we'll force the view to change
                                                            forceShowBudgetForMonth = currentMonthYear
                                                        },
                                                        modifier = Modifier.height(56.dp),
                                                        shape = MaterialTheme.shapes.medium
                                                    ) {
                                                        Text("Create ${currentDate.format(DateTimeFormatter.ofPattern("MMMM"))} budget")
                                                    }
                                                    
                                                    TextButton(onClick = { forceShowBudgetForMonth = currentMonthYear }) {
                                                        Text("Start from scratch")
                                                    }
                                                }
                                            }
                                        } else {
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
                                                                val streamTransactions = filteredTransactions
                                                                    .filter { it.type == TransactionType.INCOME && it.incomeStreamId == stream.id }
                                                                
                                                                val receivedAmount = streamTransactions.sumOf { it.amount }

                                                                IncomeDetailsCard(
                                                                    stream = stream,
                                                                    receivedAmount = receivedAmount,
                                                                    onClick = {
                                                                        selectedStreamForDetail = stream
                                                                        showIncomeDetailSheet = true
                                                                    }
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
                                                                    val spentAmount = filteredTransactions
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
                                        
                                        item {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Version $versionName",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // Sync Status Indicator
                                    if (isSyncing) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                            Screen.TRANSACTIONS -> {
                                TransactionPage(
                                    categoriesWithItems = categoriesWithItems,
                                    incomeStreams = streams,
                                    onConfirm = { type, amount, date, merchant, note, itemId, incomeStreamId ->
                                        val transaction = BudgetTransaction(
                                            userId = user?.uid ?: "",
                                            householdId = "", // ViewModel will fill this
                                            type = type,
                                            amount = amount,
                                            date = date,
                                            merchant = merchant,
                                            note = note,
                                            itemId = itemId,
                                            incomeStreamId = incomeStreamId
                                        )
                                        viewModel.saveTransaction(transaction)
                                        currentScreen = Screen.BUDGET
                                    }
                                )
                            }
                        }
                    }

                    if (showIncomeSheet) {
                        IncomeEntrySheet(
                            targetStream = editingStream,
                            onDismiss = { showIncomeSheet = false },
                            onConfirm = { sourceName, amount ->
                                val streamToSave = IncomeStream(
                                    id = editingStream?.id ?: java.util.UUID.randomUUID().toString(),
                                    userId = user?.uid ?: "",
                                    householdId = "", // ViewModel will fill this
                                    sourceName = sourceName,
                                    monthlyAmount = amount
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

                    if (selectedStreamForDetail != null && showIncomeDetailSheet) {
                        IncomeDetailSheet(
                            stream = selectedStreamForDetail!!,
                            transactions = filteredTransactions.filter { it.incomeStreamId == selectedStreamForDetail!!.id },
                            onDismiss = { showIncomeDetailSheet = false },
                            onEditStream = {
                                editingStream = selectedStreamForDetail
                                showIncomeSheet = true
                                showIncomeDetailSheet = false
                            },
                            onEditTransaction = { transaction ->
                                editingTransaction = transaction
                                showTransactionEditSheet = true
                                showIncomeDetailSheet = false
                            }
                        )
                    }

                    if (showCategorySheet) {
                        CategoryEntrySheet(
                            targetCategory = editingCategory,
                            onDismiss = { showCategorySheet = false },
                            onConfirm = { name ->
                                val categoryToSave = BudgetCategory(
                                    id = editingCategory?.id ?: java.util.UUID.randomUUID().toString(),
                                    userId = user?.uid ?: "",
                                    householdId = "", // ViewModel will fill this
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
                            categoryId = activeCategoryId ?: "",
                            targetItem = editingItem,
                            onDismiss = { showItemSheet = false },
                            onConfirm = { name, target ->
                                val itemId = editingItem?.id ?: java.util.UUID.randomUUID().toString()
                                val itemToSave = EnvelopeItem(
                                    id = itemId,
                                    userId = user?.uid ?: "",
                                    householdId = "", // ViewModel will fill this
                                    categoryId = activeCategoryId ?: "",
                                    name = name,
                                    targetAmount = target,
                                    allocatedAmount = 0.0 // Deprecated
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
                            transactions = filteredTransactions.filter { it.itemId == selectedItemForDetail!!.id },
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
                            incomeStreams = streams,
                            onDismiss = { showTransactionEditSheet = false },
                            onConfirm = { type, amount, date, merchant, note, itemId, incomeStreamId ->
                                val transaction = editingTransaction!!.copy(
                                    userId = user?.uid ?: "",
                                    type = type,
                                    amount = amount,
                                    date = date,
                                    merchant = merchant,
                                    note = note,
                                    itemId = itemId,
                                    incomeStreamId = incomeStreamId
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

                    if (showLogoutDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            title = { Text("Logout") },
                            text = { Text("Are you sure you want to log out?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showLogoutDialog = false
                                        authViewModel.signOut()
                                    }
                                ) {
                                    Text("LOGOUT", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLogoutDialog = false }) {
                                    Text("CANCEL")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
