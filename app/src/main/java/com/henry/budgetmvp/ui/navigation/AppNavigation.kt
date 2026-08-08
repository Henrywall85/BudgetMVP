package com.henry.budgetmvp.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.BudgetCategory
import com.henry.budgetmvp.data.IncomeStream
import com.henry.budgetmvp.data.EnvelopeItem
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.ui.screens.*
import com.henry.budgetmvp.ui.components.*
import com.henry.budgetmvp.data.StatusMessage
import com.henry.budgetmvp.data.MessageType
import com.henry.budgetmvp.viewmodel.AuthViewModel
import com.henry.budgetmvp.viewmodel.BudgetViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
private fun RowScope.NavigationTabItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 150),
        label = "tab_content"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disables ripple bleeding onto adjacent tabs
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: BudgetViewModel,
    authViewModel: AuthViewModel,
    versionName: String,
) {
    val user by authViewModel.userState.collectAsState()
    val authLoading by authViewModel.loading.collectAsState()
    val authError by authViewModel.error.collectAsState()
    
    val streams by viewModel.incomeStreams.collectAsState(initial = emptyList())
    val categoriesWithItems by viewModel.categoriesWithItems.collectAsState(initial = emptyList())
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val pendingInvites by viewModel.pendingInvites.collectAsState()
    val hasAnyBudgetData by viewModel.hasAnyBudgetData.collectAsState(initial = false)
    val isSyncing by viewModel.isSyncing.collectAsState()

    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(if (user != null) Screen.BUDGET else Screen.LOGIN) }
    
    // Sync currentScreen with pager state for TopAppBar title and Overlay logic
    LaunchedEffect(pagerState.currentPage, user) {
        if (user != null) {
            when (pagerState.currentPage) {
                0 -> currentScreen = Screen.BUDGET
                1 -> currentScreen = Screen.CALENDAR
                2 -> currentScreen = Screen.TRANSACTIONS
            }
        }
    }

    // Sync pager state with currentScreen (when navigated via non-pager logic, e.g. after login)
    LaunchedEffect(currentScreen) {
        val targetPage = when (currentScreen) {
            Screen.BUDGET -> 0
            Screen.CALENDAR -> 1
            Screen.TRANSACTIONS -> 2
            else -> null
        }
        if (targetPage != null && targetPage != pagerState.currentPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    val currentMonthYear = remember(currentDate) { 
        currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM")) 
    }

    // Sync screen state with auth state
    LaunchedEffect(user) {
        if (user == null) {
            currentScreen = Screen.LOGIN
        } else if ((currentScreen == Screen.LOGIN) || (currentScreen == Screen.SIGNUP)) {
            currentScreen = Screen.BUDGET
        }
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
        filteredTransactions.asSequence()
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }

    val totalPlannedIncomeForMonth = remember(streams) {
        streams.sumOf { it.monthlyAmount }
    }

    val unassignedFunds by remember(totalReceivedIncome, totalPlannedIncomeForMonth, categoriesWithItems) {
        derivedStateOf {
            val totalPlannedExpenses = categoriesWithItems.sumOf { cat ->
                cat.items.sumOf { it.targetAmount }
            }
            totalPlannedIncomeForMonth - totalPlannedExpenses
        }
    }

    // Bottom Sheet States
    var showIncomeSheet by remember { mutableStateOf(false) }
    var editingStream by remember { mutableStateOf<IncomeStream?>(null) }
    var showIncomeDetailSheet by remember { mutableStateOf(false) }
    var selectedStreamForDetail by remember { mutableStateOf<IncomeStream?>(null) }

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
    var forceShowBudgetForMonth by remember { mutableStateOf<String?>(null) }
    val collapsedCategories = remember { mutableStateListOf<String>() }

    // --- SNACKBAR / BANNER STATE ---
    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by viewModel.statusMessage.collectAsState(initial = null)

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg.message,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
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
                                Screen.CALENDAR -> "DUE DATE CALENDAR"
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
                                        imageVector = Lucide.ArrowLeft,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                IconButton(onClick = { currentScreen = Screen.SETTINGS }) {
                                    Icon(
                                        imageVector = Lucide.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        actions = {
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
                                        imageVector = Lucide.Bell,
                                        contentDescription = "Notifications",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            if (isSyncing) {
                                Icon(
                                    imageVector = Lucide.RefreshCw,
                                    contentDescription = "Syncing",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Lucide.Cloud,
                                    contentDescription = "Synced",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(onClick = { showLogoutDialog = true }) {
                                Icon(
                                    imageVector = Lucide.LogOut,
                                    contentDescription = "Logout",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        bottomBar = {
            if (currentScreen != Screen.LOGIN && currentScreen != Screen.SIGNUP) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .fillMaxWidth()
                ) {
                        Row(
                            modifier = Modifier
                                .height(68.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavigationTabItem(
                                selected = pagerState.currentPage == 0,
                                icon = Lucide.Wallet,
                                label = "Budget",
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                            )

                            NavigationTabItem(
                                selected = pagerState.currentPage == 1,
                                icon = Lucide.Calendar,
                                label = "Calendar",
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                            )

                            NavigationTabItem(
                                selected = pagerState.currentPage == 2,
                                icon = Lucide.ReceiptText,
                                label = "Transactions",
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                            )
                        }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp),
                snackbar = { data ->
                    val currentMessage = statusMessage
                    if (currentMessage != null) {
                        BudgetNotificationBanner(
                            statusMessage = currentMessage,
                            onDismiss = { data.dismiss() }
                        )
                    } else {
                        Snackbar(snackbarData = data)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
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
                        onSignupClick = { _, email, password ->
                            authViewModel.signUp(email, password) { success ->
                                if (success) currentScreen = Screen.BUDGET
                            }
                        },
                        onNavigateToLogin = { currentScreen = Screen.LOGIN }
                    )
                }
                Screen.HOUSEHOLD -> {
                    val members by viewModel.householdMembers.collectAsState()
                    HouseholdScreen(
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
                    SettingsScreen(
                        onNavigateToHousehold = { currentScreen = Screen.HOUSEHOLD },
                        onStartFromScratch = { 
                            viewModel.resetAllHouseholdData()
                            currentScreen = Screen.BUDGET
                        }
                    )
                }
                Screen.BUDGET, Screen.CALENDAR, Screen.TRANSACTIONS -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> {
                                BudgetScreen(
                                    unassignedFunds = unassignedFunds,
                                    currentDate = currentDate,
                                    streams = streams,
                                    categoriesWithItems = categoriesWithItems,
                                    filteredTransactions = filteredTransactions,
                                    hasAnyBudgetData = hasAnyBudgetData,
                                    isSyncing = isSyncing,
                                    versionName = versionName,
                                    collapsedCategories = collapsedCategories,
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
                                    },
                                    onCreateBudget = {
                                        val prevMonth = currentDate.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
                                        viewModel.copyBudget(prevMonth, currentMonthYear)
                                        forceShowBudgetForMonth = currentMonthYear
                                    },
                                    onStartFromScratch = { forceShowBudgetForMonth = currentMonthYear },
                                    onAddIncome = {
                                        editingStream = null
                                        showIncomeSheet = true
                                    },
                                    onEditIncome = { stream ->
                                        selectedStreamForDetail = stream
                                        showIncomeDetailSheet = true
                                    },
                                    onAddCategory = {
                                        editingCategory = null
                                        showCategorySheet = true
                                    },
                                    onEditCategory = { category ->
                                        editingCategory = category
                                        showCategorySheet = true
                                    },
                                    onAddItem = { categoryId ->
                                        activeCategoryId = categoryId
                                        editingItem = null
                                        showItemSheet = true
                                    },
                                    onEditItem = { item ->
                                        selectedItemForDetail = item
                                        showItemDetailSheet = true
                                    },
                                    onToggleCategory = { categoryId ->
                                        if (collapsedCategories.contains(categoryId)) {
                                            collapsedCategories.remove(categoryId)
                                        } else {
                                            collapsedCategories.add(categoryId)
                                        }
                                    }
                                )
                            }
                            1 -> {
                                CalendarScreen(
                                    categoriesWithItems = categoriesWithItems,
                                    filteredTransactions = filteredTransactions,
                                    currentDate = currentDate,
                                    onPreviousMonth = { 
                                        currentDate = currentDate.minusMonths(1)
                                        viewModel.setMonthYear(currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                                    },
                                    onNextMonth = { 
                                        currentDate = currentDate.plusMonths(1)
                                        viewModel.setMonthYear(currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                                    },
                                    onEditItem = { item ->
                                        selectedItemForDetail = item
                                        showItemDetailSheet = true
                                    }
                                )
                            }
                            2 -> {
                                TransactionsScreen(
                                    categoriesWithItems = categoriesWithItems,
                                    incomeStreams = streams,
                                    onSaveTransaction = { type, amount, date, merchant, note, itemId, incomeStreamId ->
                                        val transaction = BudgetTransaction(
                                            userId = user?.uid ?: "",
                                            householdId = "",
                                            type = type,
                                            amount = amount,
                                            date = date,
                                            merchant = merchant,
                                            note = note,
                                            itemId = itemId,
                                            incomeStreamId = incomeStreamId
                                        )
                                        viewModel.saveTransaction(transaction)
                                        // No need to switch screen manually, Pager handles it or user stays here
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // --- ALL DIALOGS AND SHEETS ---
        
        if (showIncomeSheet) {
            IncomeEntrySheet(
                targetStream = editingStream,
                onDismiss = { showIncomeSheet = false },
                onConfirm = { sourceName, amount ->
                    val streamToSave = editingStream?.copy(
                        sourceName = sourceName,
                        monthlyAmount = amount
                    ) ?: IncomeStream(
                        id = UUID.randomUUID().toString(),
                        userId = user?.uid ?: "",
                        householdId = "",
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
                    val categoryToSave = editingCategory?.copy(
                        name = name
                    ) ?: BudgetCategory(
                        id = UUID.randomUUID().toString(),
                        userId = user?.uid ?: "",
                        householdId = "",
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
                onConfirm = { name, target, dueDay ->
                    val itemToSave = editingItem?.copy(
                        name = name,
                        targetAmount = target,
                        dueDay = dueDay
                    ) ?: EnvelopeItem(
                        id = UUID.randomUUID().toString(),
                        userId = user?.uid ?: "",
                        householdId = "",
                        categoryId = activeCategoryId ?: "",
                        name = name,
                        targetAmount = target,
                        allocatedAmount = 0.0,
                        dueDay = dueDay
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
