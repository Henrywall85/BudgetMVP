package com.henry.budgetmvp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.CategoryWithItems
import com.henry.budgetmvp.data.IncomeStream
import com.henry.budgetmvp.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun TransactionsScreen(
    transactions: List<BudgetTransaction>,
    categoriesWithItems: List<CategoryWithItems>,
    incomeStreams: List<IncomeStream>,
    currentDate: LocalDate = LocalDate.now(),
    onAddTransaction: () -> Unit,
    onEditTransaction: (BudgetTransaction) -> Unit,
    onClearAllTransactions: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableIntStateOf(0) } // 0: All, 1: Expenses, 2: Income
    var selectedMonthFilter by remember(currentDate) { mutableStateOf<YearMonth?>(YearMonth.from(currentDate)) }
    var showClearDialog by remember { mutableStateOf(false) }

    val allItems = remember(categoriesWithItems) { categoriesWithItems.flatMap { it.items }.associateBy { it.id } }
    val allStreams = remember(incomeStreams) { incomeStreams.associateBy { it.id } }

    val availableMonths = remember(transactions, currentDate) {
        val currentYM = YearMonth.from(currentDate)
        val set = mutableSetOf(currentYM)
        transactions.forEach { tx ->
            try {
                set.add(YearMonth.from(LocalDate.parse(tx.date)))
            } catch (ignored: Exception) {}
        }
        set.sortedDescending()
    }

    val filteredList = remember(transactions, searchQuery, filterType, selectedMonthFilter) {
        transactions.filter { tx ->
            val matchesMonth = if (selectedMonthFilter == null) true else {
                try {
                    YearMonth.from(LocalDate.parse(tx.date)) == selectedMonthFilter
                } catch (e: Exception) {
                    false
                }
            }
            val matchesType = when (filterType) {
                1 -> tx.type == TransactionType.EXPENSE
                2 -> tx.type == TransactionType.INCOME
                3 -> tx.type == TransactionType.EXPENSE && (tx.itemId == null || tx.itemId.isBlank())
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val itemName = tx.itemId?.let { allItems[it]?.name } ?: ""
                val streamName = tx.incomeStreamId?.let { allStreams[it]?.sourceName } ?: ""
                tx.merchant.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true) ||
                itemName.contains(searchQuery, ignoreCase = true) ||
                streamName.contains(searchQuery, ignoreCase = true)
            }
            matchesMonth && matchesType && matchesSearch
        }.sortedByDescending { it.date }
    }

    // Group transactions by Month (YearMonth)
    val groupedByMonth = remember(filteredList) {
        filteredList.groupBy { tx ->
            try {
                YearMonth.from(LocalDate.parse(tx.date))
            } catch (e: Exception) {
                YearMonth.now()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 170.dp)
        ) {
            // 1. SLEEK FLAT EMERALD GREEN BANNER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF059669))
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSACTIONS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        if (transactions.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    imageVector = Lucide.Trash2,
                                    contentDescription = "Clear All Transactions",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Search Bar & Filter Chips Area
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search merchant, note, or envelope...", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Lucide.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Lucide.X, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Month Selector Chips Row
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableMonths) { ym ->
                            val isSelected = selectedMonthFilter == ym
                            val monthLabel = ym.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMonthFilter = ym },
                                leadingIcon = {
                                    Icon(
                                        Lucide.Calendar,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(monthLabel, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        }

                        item {
                            val isAllSelected = selectedMonthFilter == null
                            FilterChip(
                                selected = isAllSelected,
                                onClick = { selectedMonthFilter = null },
                                label = { Text("All History", style = MaterialTheme.typography.labelSmall, fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                    }

                    val expenseCount = filteredList.count { it.type == TransactionType.EXPENSE }
                    val incomeCount = filteredList.count { it.type == TransactionType.INCOME }
                    val uncategorizedCount = transactions.count { it.type == TransactionType.EXPENSE && (it.itemId == null || it.itemId.isBlank()) }

                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = filterType == 0,
                                onClick = { filterType = 0 },
                                label = { Text("All (${filteredList.size})", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterType == 1,
                                onClick = { filterType = 1 },
                                label = { Text("Expenses ($expenseCount)", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterType == 2,
                                onClick = { filterType = 2 },
                                label = { Text("Income ($incomeCount)", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF059669).copy(alpha = 0.15f), selectedLabelColor = Color(0xFF059669))
                            )
                        }
                        if (uncategorizedCount > 0) {
                            item {
                                FilterChip(
                                    selected = filterType == 3,
                                    onClick = { filterType = 3 },
                                    label = { Text("Uncategorized ($uncategorizedCount)", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEF6C00).copy(alpha = 0.15f), selectedLabelColor = Color(0xFFEF6C00))
                                )
                            }
                        }
                    }
                }
            }

            if (groupedByMonth.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Lucide.ReceiptText, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching transactions found." else "No transactions logged yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tap the + button below to record your first transaction.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                groupedByMonth.forEach { (yearMonth, monthTransactions) ->
                    val monthSpent = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                    item(key = yearMonth.toString()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Month Card Header
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (monthSpent > 0) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Spent: $${"%,.2f".format(monthSpent)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Transaction Rows inside Month Card
                                monthTransactions.forEachIndexed { index, transaction ->
                                    val isIncome = transaction.type == TransactionType.INCOME
                                    val envelopeName = transaction.itemId?.let { allItems[it]?.name }
                                    val streamName = transaction.incomeStreamId?.let { allStreams[it]?.sourceName }

                                    val parsedDate = try { LocalDate.parse(transaction.date) } catch (e: Exception) { LocalDate.now() }
                                    val monthStr = parsedDate.format(DateTimeFormatter.ofPattern("MMM"))
                                    val dayStr = parsedDate.format(DateTimeFormatter.ofPattern("dd"))

                                    val subLabel = when {
                                        isIncome -> streamName ?: "Income"
                                        envelopeName != null -> envelopeName
                                        else -> "Uncategorized"
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onEditTransaction(transaction) }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // 1. Left Circular Date Ring
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color.Transparent)
                                                .border(BorderStroke(1.5.dp, Color(0xFF94A3B8)), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = monthStr,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF94A3B8),
                                                    lineHeight = 11.sp
                                                )
                                                Text(
                                                    text = dayStr,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF94A3B8),
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // 2. Center Merchant & Blue Envelope Subtitle
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (transaction.merchant.isNotBlank()) transaction.merchant else if (isIncome) "Income" else "Expense",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = subLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7)
                                            )
                                        }

                                        // 3. Right Amount
                                        Text(
                                            text = "${if (isIncome) "+" else "-"}$${"%,.2f".format(transaction.amount)}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                            fontWeight = FontWeight.Black,
                                            color = if (isIncome) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (index < monthTransactions.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                            thickness = 1.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Floating Action Button (FAB)
        FloatingActionButton(
            onClick = onAddTransaction,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 20.dp),
            containerColor = Color(0xFF059669),
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Lucide.Plus, contentDescription = "Add Transaction", modifier = Modifier.size(24.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Transactions?") },
            text = { Text("Are you sure you want to delete all transactions across every month? Your budget envelopes and income setup will remain intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllTransactions()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("YES, CLEAR TRANSACTIONS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
