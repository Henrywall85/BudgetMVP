package com.henry.budgetmvp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
    var viewMode by remember { mutableStateOf(0) } // 0: Planned, 1: Spent, 2: Remaining

    val spentByItemId = remember(filteredTransactions) {
        filteredTransactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE && it.itemId != null }
            .groupBy { it.itemId!! }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }
    val todayDay = remember { LocalDate.now().dayOfMonth }
    
    val totalIncome = remember(streams) { streams.sumOf { it.monthlyAmount } }
    val totalPlanned = remember(categoriesWithItems) {
        categoriesWithItems.sumOf { cat -> cat.items.sumOf { it.targetAmount } }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 170.dp)
        ) {
            // (1) COMPACT EMERALD BANNER (Matching Home Page)
            item {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFF059669),
                                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                            )
                            .statusBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp)
                    ) {
                        Column {
                            // Month Switcher Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(32.dp)) {
                                    Icon(Lucide.ChevronLeft, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.clickable { onMonthClick() }
                                )
                                IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                                    Icon(Lucide.ChevronRight, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Side-by-Side Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "$${"%,.2f".format(unassignedFunds)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = if (unassignedFunds < -0.001) Color(0xFFFFCDD2) else Color.White
                                    )
                                    Surface(
                                        color = if (unassignedFunds < -0.001) Color(0xFFC62828) else Color.White.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                unassignedFunds < -0.001 -> "OVER BUDGET"
                                                unassignedFunds > 0.001 -> "LEFT TO BUDGET"
                                                else -> "✨ ALLOCATED ($0.00)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Income: $${"%,.0f".format(totalIncome)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Planned: $${"%,.0f".format(totalPlanned)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Slim 4dp Progress Bar
                            val allocatedRatio = if (totalIncome > 0) (totalPlanned / totalIncome).toFloat().coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(
                                progress = { allocatedRatio },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = if (unassignedFunds < -0.001) Color(0xFFFFCDD2) else Color.White,
                                trackColor = Color.White.copy(alpha = 0.22f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // (2) 3-WAY MODE SWITCHER (Planned | Spent | Remaining)
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = viewMode == 0,
                                onClick = { viewMode = 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                label = { Text("Planned", style = MaterialTheme.typography.labelSmall) }
                            )
                            SegmentedButton(
                                selected = viewMode == 1,
                                onClick = { viewMode = 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                label = { Text("Spent", style = MaterialTheme.typography.labelSmall) }
                            )
                            SegmentedButton(
                                selected = viewMode == 2,
                                onClick = { viewMode = 2 },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                label = { Text("Remaining", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // THE "CREATE BUDGET" SCREEN
            if (streams.isEmpty() && categoriesWithItems.isEmpty() && hasAnyBudgetData) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
                // (3) MULTIPLE INCOME DETAILS CARDS
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "INCOME SOURCES",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column {
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
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(
                                            onClick = onAddIncome,
                                            modifier = Modifier.fillMaxWidth()
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
                }

                // (4) BUDGET CATEGORIES & ENVELOPE ITEMS
                if (categoriesWithItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(bottom = if (isExpanded) 8.dp else 0.dp)) {
                                CategoryHeader(
                                    category = categoryWithItems.category,
                                    isExpanded = isExpanded,
                                    onToggleExpand = { onToggleCategory(categoryWithItems.category.id) },
                                    onEditCategory = { onEditCategory(categoryWithItems.category) }
                                )

                                if (isExpanded) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 0.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(
                                            onClick = { onAddItem(categoryWithItems.category.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add Item", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
