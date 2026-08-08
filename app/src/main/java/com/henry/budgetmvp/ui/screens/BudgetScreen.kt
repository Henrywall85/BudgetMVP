package com.henry.budgetmvp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.CategoryWithItems
import com.henry.budgetmvp.data.IncomeStream
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BudgetScreen(
    unassignedFunds: Double,
    currentDate: LocalDate,
    streams: List<IncomeStream>,
    categoriesWithItems: List<CategoryWithItems>,
    filteredTransactions: List<BudgetTransaction>,
    hasAnyBudgetData: Boolean,
    isSyncing: Boolean,
    versionName: String,
    collapsedCategories: List<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    onCreateBudget: () -> Unit,
    onStartFromScratch: () -> Unit,
    onAddIncome: () -> Unit,
    onEditIncome: (IncomeStream) -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (com.henry.budgetmvp.data.BudgetCategory) -> Unit,
    onAddItem: (String) -> Unit,
    onEditItem: (com.henry.budgetmvp.data.EnvelopeItem) -> Unit,
    onToggleCategory: (String) -> Unit,
) {
    val spentByItemId = remember(filteredTransactions) {
        filteredTransactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE && it.itemId != null }
            .groupBy { it.itemId!! }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }
    val todayDay = remember { LocalDate.now().dayOfMonth }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
        ) {
            // (1) THE TOTAL POOL CARD
            item {
                Spacer(modifier = Modifier.height(0.dp))
                TotalPoolCard(
                    total = unassignedFunds,
                    currentDate = currentDate,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onMonthClick = onMonthClick
                )
            }

            // THE "CREATE BUDGET" SCREEN
            if (streams.isEmpty() && categoriesWithItems.isEmpty() && hasAnyBudgetData) {
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
                            onClick = onCreateBudget,
                            modifier = Modifier.height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Create ${currentDate.format(DateTimeFormatter.ofPattern("MMMM"))} budget")
                        }
                        
                        TextButton(onClick = onStartFromScratch) {
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
                                        onClick = onAddIncome,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    ) {
                                        Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Get Started")
                                    }
                                }
                            } else {
                                streams.forEachIndexed { index, stream ->
                                    val streamTransactions = filteredTransactions.asSequence()
                                        .filter { it.type == TransactionType.INCOME && it.incomeStreamId == stream.id }
                                        .toList()
                                    
                                    val receivedAmount = streamTransactions.sumOf { it.amount }

                                    IncomeDetailsCard(
                                        stream = stream,
                                        receivedAmount = receivedAmount,
                                        onClick = { onEditIncome(stream) }
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
                                        onClick = onAddIncome,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                    onClick = onAddCategory,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                ) {
                                    Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create Category")
                                }
                            }
                        }
                    }
                } else {
                    items(categoriesWithItems, key = { it.category.id }) { categoryWithItems ->
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
                                    onToggleExpand = { onToggleCategory(categoryWithItems.category.id) },
                                    onEditCategory = { onEditCategory(categoryWithItems.category) },
                                    onAddItem = { onAddItem(categoryWithItems.category.id) }
                                )

                                if (isExpanded) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 0.dp),
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    categoryWithItems.items.forEachIndexed { index, item ->
                                        val spentAmount = spentByItemId[item.id] ?: 0.0

                                        EnvelopeItemRow(
                                            item = item,
                                            spentAmount = spentAmount,
                                            todayDay = todayDay,
                                            onClick = { onEditItem(item) }
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
                                onClick = onAddCategory,
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
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

        if (isSyncing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
