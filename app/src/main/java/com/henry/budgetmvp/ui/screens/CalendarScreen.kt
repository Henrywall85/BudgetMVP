package com.henry.budgetmvp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.henry.budgetmvp.data.EnvelopeItem
import com.henry.budgetmvp.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    categoriesWithItems: List<CategoryWithItems>,
    filteredTransactions: List<BudgetTransaction>,
    currentDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditItem: (EnvelopeItem) -> Unit
) {
    val yearMonth = remember(currentDate) { YearMonth.from(currentDate) }
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7 // 0=Sun, 1=Mon...
    
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    
    val allItemsWithDueDates = remember(categoriesWithItems) {
        categoriesWithItems.flatMap { it.items }.filter { it.dueDay != null }
    }

    // O(1) instant lookup maps
    val spentByItemId = remember(filteredTransactions) {
        filteredTransactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE && it.itemId != null }
            .groupBy { it.itemId!! }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }

    val itemsByDueDay = remember(allItemsWithDueDates) {
        allItemsWithDueDates.groupBy { it.dueDay }
    }
    
    val today = LocalDate.now()
    val isCurrentMonth = today.year == currentDate.year && today.month == currentDate.month

    // --- Calculations ---
    val totalBillsAmount = allItemsWithDueDates.sumOf { it.targetAmount }
    val totalPaidAmount = allItemsWithDueDates.sumOf { item ->
        val spent = spentByItemId[item.id] ?: 0.0
        spent.coerceAtMost(item.targetAmount)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 1. Month Switcher
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Lucide.ChevronLeft, contentDescription = "Previous", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onNextMonth) {
                Icon(Lucide.ChevronRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // 2. Summary Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Monthly Bills", style = MaterialTheme.typography.labelSmall)
                    Text("$${"%,.2f".format(totalBillsAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Funded/Paid", style = MaterialTheme.typography.labelSmall)
                    Text("$${"%,.2f".format(totalPaidAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                }
            }
        }

        // 3. Calendar Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Days of week header
                Row(modifier = Modifier.fillMaxWidth()) {
                    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Days
                val totalSlots = (daysInMonth + firstDayOfWeek + 6) / 7 * 7
                for (row in 0 until (totalSlots / 7)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val slotIndex = row * 7 + col
                            val day = slotIndex - firstDayOfWeek + 1
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedDay == day) MaterialTheme.colorScheme.primaryContainer 
                                        else if (isCurrentMonth && day == today.dayOfMonth) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        else Color.Transparent
                                    )
                                    .clickable(enabled = day in 1..daysInMonth) { 
                                        selectedDay = if (selectedDay == day) null else day 
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (day in 1..daysInMonth) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isCurrentMonth && day == today.dayOfMonth) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrentMonth && day == today.dayOfMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        // Status dots
                                        val itemsOnDay = itemsByDueDay[day] ?: emptyList()
                                        if (itemsOnDay.isNotEmpty()) {
                                            val statusColor = getCombinedStatusColor(itemsOnDay, spentByItemId, today, currentDate)
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(statusColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Timeline
        val filteredTimelineItems = remember(allItemsWithDueDates, itemsByDueDay, selectedDay) {
            if (selectedDay == null) allItemsWithDueDates.sortedBy { it.dueDay }
            else itemsByDueDay[selectedDay] ?: emptyList()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 170.dp)
        ) {
            if (filteredTimelineItems.isEmpty()) {
                item {
                    Text(
                        text = if (selectedDay == null) "No bills due this month." else "No bills due on the ${selectedDay}th.",
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                items(filteredTimelineItems) { item ->
                    val spent = spentByItemId[item.id] ?: 0.0
                    val isFunded = spent >= item.targetAmount - 0.001
                    
                    val urgencyColor = getItemStatusColor(item, spent, today, currentDate)

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onEditItem(item) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(urgencyColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Due ${item.dueDay}${getOrdinalSuffix(item.dueDay ?: 0)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$${"%,.2f".format(item.targetAmount)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                                if (isFunded) {
                                    Text("PAID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                } else {
                                    val left = item.targetAmount - spent
                                    Text("$${"%,.2f".format(left)} LEFT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = urgencyColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCombinedStatusColor(items: List<EnvelopeItem>, spentMap: Map<String, Double>, today: LocalDate, currentDate: LocalDate): Color {
    val statuses = items.map { getItemStatusColor(it, spentMap[it.id] ?: 0.0, today, currentDate) }
    return when {
        statuses.contains(Color(0xFFC62828)) -> Color(0xFFC62828) // Red if any are red
        statuses.contains(Color(0xFFEF6C00)) -> Color(0xFFEF6C00) // Orange if any are orange
        else -> Color(0xFF2E7D32) // Green if all are green
    }
}

private fun getItemStatusColor(item: EnvelopeItem, spent: Double, today: LocalDate, currentDate: LocalDate): Color {
    val dueDay = item.dueDay ?: return Color.Gray
    val isFunded = spent >= item.targetAmount - 0.001
    
    if (isFunded) return Color(0xFF2E7D32) // Green

    val isCurrentMonth = today.year == currentDate.year && today.month == currentDate.month
    if (!isCurrentMonth) {
        return if (currentDate.isBefore(today.withDayOfMonth(1))) Color(0xFFC62828) // Red if past month and unpaid
        else Color.Gray // Future month
    }

    val daysUntil = dueDay - today.dayOfMonth
    return when {
        daysUntil < 0 -> Color(0xFFC62828) // Red (Overdue)
        daysUntil == 0 -> Color(0xFFC62828) // Red (Due Today)
        daysUntil in 1..5 -> Color(0xFFEF6C00) // Orange (Due Soon)
        else -> Color.Gray // Future
    }
}

private fun getOrdinalSuffix(day: Int): String {
    return if (day in 11..13) "th"
    else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
