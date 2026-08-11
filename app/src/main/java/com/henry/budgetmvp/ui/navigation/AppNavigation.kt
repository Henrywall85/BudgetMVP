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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
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
    val userProfile by viewModel.userProfile.collectAsState()

    val pagerState = rememberPagerState(initialPage = 0) { 4 }
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentScreen by remember { mutableStateOf(if (user != null) Screen.HOME else Screen.LOGIN) }
    
    // Sync currentScreen with pager state for TopAppBar title and Overlay logic
    LaunchedEffect(pagerState.currentPage, user) {
        if (user != null) {
            when (pagerState.currentPage) {
                0 -> currentScreen = Screen.HOME
                1 -> currentScreen = Screen.BUDGET
                2 -> currentScreen = Screen.CALENDAR
                3 -> currentScreen = Screen.TRANSACTIONS
            }
        }
    }

    // Sync pager state with currentScreen (when navigated via non-pager logic, e.g. after login)
    LaunchedEffect(currentScreen) {
        val targetPage = when (currentScreen) {
            Screen.HOME -> 0
            Screen.BUDGET -> 1
            Screen.CALENDAR -> 2
            Screen.TRANSACTIONS -> 3
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
            currentScreen = Screen.HOME
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

    val totalSpentExpenses = remember(filteredTransactions) {
        filteredTransactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }

    val totalPlannedExpenses = remember(categoriesWithItems) {
        categoriesWithItems.sumOf { cat ->
            cat.items.sumOf { it.targetAmount }
        }
    }

    val unassignedFunds by remember(totalReceivedIncome, totalPlannedIncomeForMonth, categoriesWithItems) {
        derivedStateOf {
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

    val isOverlayScreen = currentScreen == Screen.HOUSEHOLD || currentScreen == Screen.SETTINGS
    val showDrawer = currentScreen != Screen.LOGIN && currentScreen != Screen.SIGNUP

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showDrawer && pagerState.currentPage == 0 && !isOverlayScreen,
            drawerContent = {
                if (showDrawer) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet(
                            modifier = Modifier.width(310.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.surface,
                            drawerShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
                        ) {
                            val profileName = userProfile?.userName?.takeIf { it.isNotBlank() }
                                ?: if (user?.email?.contains("henrywall", ignoreCase = true) == true) "Henry Wall"
                                else user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Friend"

                            val initials = profileName.split(" ").filter { it.isNotEmpty() }.take(2).map { it.first().uppercase() }.joinToString("")
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Profile Header
                            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = profileName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Sync Pill
                                Surface(
                                    color = if (isSyncing) Color(0xFFEF6C00).copy(alpha = 0.1f) else Color(0xFF059669).copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, if (isSyncing) Color(0xFFEF6C00).copy(alpha = 0.2f) else Color(0xFF059669).copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSyncing) Lucide.RefreshCw else Lucide.Cloud,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isSyncing) Color(0xFFEF6C00) else Color(0xFF059669)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSyncing) "Syncing..." else "Synced",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSyncing) Color(0xFFEF6C00) else Color(0xFF059669)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Navigation Items
                            NavigationDrawerItem(
                                label = { Text("Household & Family", fontWeight = FontWeight.Medium) },
                                selected = currentScreen == Screen.HOUSEHOLD,
                                onClick = { 
                                    currentScreen = Screen.HOUSEHOLD
                                    coroutineScope.launch { drawerState.close() }
                                },
                                icon = { 
                                    BadgedBox(
                                        badge = {
                                            if (pendingInvites.isNotEmpty()) {
                                                Badge(containerColor = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    ) {
                                        Icon(Lucide.Users, contentDescription = null, modifier = Modifier.size(22.dp))
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            
                            NavigationDrawerItem(
                                label = { Text("App Settings", fontWeight = FontWeight.Medium) },
                                selected = currentScreen == Screen.SETTINGS,
                                onClick = { 
                                    currentScreen = Screen.SETTINGS
                                    coroutineScope.launch { drawerState.close() }
                                },
                                icon = { Icon(Lucide.Settings, contentDescription = null, modifier = Modifier.size(22.dp)) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            
                            NavigationDrawerItem(
                                label = { Text("Sync with Cloud", fontWeight = FontWeight.Medium) },
                                selected = false,
                                onClick = { 
                                    viewModel.manualSync()
                                    coroutineScope.launch { drawerState.close() }
                                },
                                icon = { Icon(Lucide.RefreshCw, contentDescription = null, modifier = Modifier.size(22.dp)) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))

                            // App Version Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "App Version",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "v$versionName",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            NavigationDrawerItem(
                                label = { Text("Sign Out", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                                selected = false,
                                onClick = { 
                                    showLogoutDialog = true
                                    coroutineScope.launch { drawerState.close() }
                                },
                                icon = { 
                                    Icon(
                                        imageVector = Lucide.LogOut, 
                                        contentDescription = "Sign Out", 
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    ) 
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
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
                                        icon = Lucide.House,
                                        label = "Home",
                                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                                    )

                                    NavigationTabItem(
                                        selected = pagerState.currentPage == 1,
                                        icon = Lucide.Wallet,
                                        label = "Budget",
                                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                                    )

                                    NavigationTabItem(
                                        selected = pagerState.currentPage == 2,
                                        icon = Lucide.Calendar,
                                        label = "Calendar",
                                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                                    )

                                    NavigationTabItem(
                                        selected = pagerState.currentPage == 3,
                                        icon = Lucide.ReceiptText,
                                        label = "Transactions",
                                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(3) } }
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding())
                    ) {
                        // --- MAIN SCREENS ---
                        when (currentScreen) {
                            Screen.LOGIN -> {
                                LoginPage(
                                    versionName = versionName,
                                    loading = authLoading,
                                    errorMessage = authError,
                                    onLoginClick = { email, password ->
                                        authViewModel.signIn(email, password) { success ->
                                            if (success) currentScreen = Screen.HOME
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
                                    onSignupClick = { fullName, email, password ->
                                        authViewModel.signUp(fullName, email, password) { success ->
                                            if (success) currentScreen = Screen.HOME
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
                                        currentScreen = Screen.HOME
                                    }
                                )
                            }
                            Screen.HOME, Screen.BUDGET, Screen.CALENDAR, Screen.TRANSACTIONS -> {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    beyondViewportPageCount = 1
                                ) { page ->
                                    val profileName = userProfile?.userName?.takeIf { it.isNotBlank() }
                                        ?: if (user?.email?.contains("henrywall", ignoreCase = true) == true) "Henry Wall"
                                        else user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Friend"

                                    val firstName = profileName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Henry"

                                    val onMarkPaid: (EnvelopeItem, Double) -> Unit = { item, remainingAmount ->
                                        editingTransaction = BudgetTransaction(
                                            id = UUID.randomUUID().toString(),
                                            userId = user?.uid ?: "",
                                            householdId = userProfile?.householdId ?: "",
                                            type = TransactionType.EXPENSE,
                                            amount = remainingAmount,
                                            date = LocalDate.now().toString(),
                                            merchant = item.name,
                                            note = "Recurring Bill",
                                            itemId = item.id
                                        )
                                        showTransactionEditSheet = true
                                    }

                                    when (page) {
                                        0 -> {
                                            HomeScreen(
                                                userName = firstName,
                                                unassignedFunds = unassignedFunds,
                                                totalPlanned = totalPlannedExpenses,
                                                totalSpent = totalSpentExpenses,
                                                categoriesWithItems = categoriesWithItems,
                                                filteredTransactions = filteredTransactions,
                                                onNavigateToBudget = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                                                onNavigateToCalendar = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                                                onOpenMenu = { coroutineScope.launch { drawerState.open() } },
                                                onMarkPaid = onMarkPaid
                                            )
                                        }
                                        1 -> {
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
                                                },
                                                onAddTransaction = {
                                                    editingTransaction = null
                                                    showTransactionEditSheet = true
                                                }
                                            )
                                        }
                                        2 -> {
                                            CalendarScreen(
                                                categoriesWithItems = categoriesWithItems,
                                                filteredTransactions = filteredTransactions,
                                                streams = streams,
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
                                                },
                                                onMarkPaid = onMarkPaid
                                            )
                                        }
                                        3 -> {
                                            TransactionsScreen(
                                                transactions = transactions,
                                                categoriesWithItems = categoriesWithItems,
                                                incomeStreams = streams,
                                                onAddTransaction = {
                                                    editingTransaction = null
                                                    showTransactionEditSheet = true
                                                },
                                                onEditTransaction = { transaction ->
                                                    editingTransaction = transaction
                                                    showTransactionEditSheet = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // --- BACK BUTTON (For Overlays) ---
                        if (isOverlayScreen) {
                            IconButton(
                                onClick = { currentScreen = Screen.HOME },
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                                    .align(Alignment.TopStart)
                            ) {
                                Icon(
                                    imageVector = Lucide.ArrowLeft,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
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
                onConfirm = { sourceName, amount, scheduleType, anchor, payDays ->
                    val streamToSave = editingStream?.copy(
                        sourceName = sourceName,
                        monthlyAmount = amount,
                        payScheduleType = scheduleType,
                        anchorDate = anchor,
                        payDays = payDays
                    ) ?: IncomeStream(
                        id = UUID.randomUUID().toString(),
                        userId = user?.uid ?: "",
                        householdId = "",
                        sourceName = sourceName,
                        monthlyAmount = amount,
                        payScheduleType = scheduleType,
                        anchorDate = anchor,
                        payDays = payDays
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

        if (showTransactionEditSheet) {
            TransactionEntrySheet(
                targetTransaction = editingTransaction,
                categoriesWithItems = categoriesWithItems,
                incomeStreams = streams,
                onDismiss = { 
                    showTransactionEditSheet = false
                    editingTransaction = null
                },
                onConfirm = { type, amount, date, merchant, note, itemId, incomeStreamId ->
                    val transaction = editingTransaction?.copy(
                        userId = user?.uid ?: "",
                        type = type,
                        amount = amount,
                        date = date,
                        merchant = merchant,
                        note = note,
                        itemId = itemId,
                        incomeStreamId = incomeStreamId
                    ) ?: BudgetTransaction(
                        id = UUID.randomUUID().toString(),
                        userId = user?.uid ?: "",
                        householdId = userProfile?.householdId ?: "",
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
                    editingTransaction = null
                },
                onDelete = {
                    editingTransaction?.let { viewModel.deleteTransaction(it) }
                    showTransactionEditSheet = false
                    editingTransaction = null
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
