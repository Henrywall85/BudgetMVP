package com.henry.budgetmvp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.CategoryWithItems
import com.henry.budgetmvp.data.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

data class DayStatus(
    val label: String,
    val isLogged: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean
)

@Composable
fun HomeScreen(
    userName: String,
    unassignedFunds: Double,
    totalPlanned: Double,
    totalSpent: Double,
    categoriesWithItems: List<CategoryWithItems>,
    filteredTransactions: List<BudgetTransaction>,
    onNavigateToBudget: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val today = LocalDate.now()
    val currentHour = remember { java.time.LocalTime.now().hour }
    val greetingText = when (currentHour) {
        in 4..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        in 17..21 -> "Good evening,"
        else -> "Good night,"
    }
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    
    // Set of dates where at least one transaction was recorded
    val transactionDates = remember(filteredTransactions) {
        filteredTransactions.asSequence().map { it.date }.toSet()
    }

    // 7-day week schedule (Monday to Sunday)
    val weekDays = remember(startOfWeek, transactionDates, today) {
        (0..6).map { offset ->
            val date = startOfWeek.plusDays(offset.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val isLogged = transactionDates.contains(dateStr)
            val label = when (offset) {
                0 -> "M"
                1 -> "T"
                2 -> "W"
                3 -> "T"
                4 -> "F"
                5 -> "S"
                else -> "S"
            }
            DayStatus(
                label = label,
                isLogged = isLogged,
                isToday = date == today,
                isFuture = date.isAfter(today)
            )
        }
    }

    // Calculate real consecutive streak
    val streakCount = remember(transactionDates, today) {
        var count = 0
        var checkDate = today
        // If today is not logged, the streak count starts checking from yesterday
        if (!transactionDates.contains(today.format(DateTimeFormatter.ISO_LOCAL_DATE))) {
            checkDate = today.minusDays(1)
        }
        
        while (transactionDates.contains(checkDate.format(DateTimeFormatter.ISO_LOCAL_DATE))) {
            count++
            checkDate = checkDate.minusDays(1)
        }
        count
    }
    
    // Calculate upcoming bills (Top 3)
    val upcomingBills = remember(categoriesWithItems, filteredTransactions) {
        categoriesWithItems.flatMap { it.items }
            .filter { item -> 
                val due = item.dueDay ?: return@filter false
                val spent = filteredTransactions.filter { it.itemId == item.id }.sumOf { it.amount }
                due >= today.dayOfMonth && spent < item.targetAmount
            }
            .sortedBy { it.dueDay }
            .take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 170.dp)
    ) {
        // 1. Top Green Greeting Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFF059669),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // Centers the icon with the 2-line title block
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = userName.ifBlank { "Henrywall85" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Circular Glassmorphic Hamburger Icon Button
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Lucide.Menu,
                        contentDescription = "Open Menu",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 2. Overlapping "Grow Your Streak" Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-24).dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Lucide.Zap, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grow your streak", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Text("$streakCount Days 🔥", fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF6C00), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Track your expenses daily to build your financial momentum.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                // Lightning Bolt Day Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    weekDays.forEach { status ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            status.isLogged -> Color(0xFF059669).copy(alpha = 0.1f)
                                            status.isToday -> Color(0xFFEF6C00).copy(alpha = 0.05f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(
                                        if (status.isToday && !status.isLogged) 
                                            Modifier.border(1.5.dp, Color(0xFFEF6C00).copy(alpha = 0.4f), CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Zap,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = when {
                                        status.isLogged -> Color(0xFF059669)
                                        status.isToday -> Color(0xFFEF6C00).copy(alpha = 0.4f)
                                        status.isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (status.isToday) FontWeight.Black else FontWeight.Bold,
                                color = if (status.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Monthly Summary
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Monthly Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${"%,.2f".format(totalSpent)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Planned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${"%,.2f".format(totalPlanned)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val progress = if (totalPlanned > 0) (totalSpent / totalPlanned).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (progress > 1f) MaterialTheme.colorScheme.error else Color(0xFF059669),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${(progress * 100).toInt()}% of budget used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Quick Actions / Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f).clickable { onNavigateToBudget() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Lucide.LayoutDashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ready to Assign", style = MaterialTheme.typography.labelSmall)
                        Text("$${"%,.2f".format(unassignedFunds)}", fontWeight = FontWeight.Bold)
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f).clickable { onNavigateToCalendar() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Lucide.CalendarClock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Upcoming Bills", style = MaterialTheme.typography.labelSmall)
                        Text("${upcomingBills.size} items", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Upcoming Bills List
            if (upcomingBills.isNotEmpty()) {
                Text(
                    text = "Next Bills Due",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                upcomingBills.forEach { item ->
                    val spent = filteredTransactions.filter { it.itemId == item.id }.sumOf { it.amount }
                    val remaining = item.targetAmount - spent
                    
                    ListItem(
                        headlineContent = { Text(item.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Due Day ${item.dueDay}") },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$${"%,.2f".format(remaining)}", fontWeight = FontWeight.Bold)
                                Text("Remaining", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.CreditCard, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable { onNavigateToCalendar() }
                    )
                }
            }
        }
    }
}
