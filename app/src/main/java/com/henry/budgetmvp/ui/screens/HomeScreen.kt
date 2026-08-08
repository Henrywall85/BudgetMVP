package com.henry.budgetmvp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var showInsightsSheet by remember { mutableStateOf(false) }
    
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.clickable { showInsightsSheet = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showInsightsSheet = true },
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
                        strokeCap = StrokeCap.Round
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

    if (showInsightsSheet) {
        var insightsTab by remember { mutableStateOf(0) }
        
        val categorySpendings = remember(categoriesWithItems, filteredTransactions) {
            val total = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            categoriesWithItems.map { cat ->
                val spent = filteredTransactions.asSequence()
                    .filter { it.type == TransactionType.EXPENSE && cat.items.any { item -> item.id == it.itemId } }
                    .sumOf { it.amount }
                val pct = if (total > 0) (spent / total).toFloat() else 0f
                Triple(cat.category.name, spent, pct)
            }.filter { it.second > 0 }.sortedByDescending { it.second }
        }

        val sliceColors = listOf(
            Color(0xFF0D9488), Color(0xFF3B82F6), Color(0xFFF59E0B), 
            Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF64748B)
        )

        ModalBottomSheet(
            onDismissRequest = { showInsightsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = { WindowInsets(0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Insights & Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- TAB SWITCHER ---
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = insightsTab == 0,
                        onClick = { insightsTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        label = { Text("Categories", style = MaterialTheme.typography.labelSmall) }
                    )
                    SegmentedButton(
                        selected = insightsTab == 1,
                        onClick = { insightsTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        label = { Text("Essentials", style = MaterialTheme.typography.labelSmall) }
                    )
                    SegmentedButton(
                        selected = insightsTab == 2,
                        onClick = { insightsTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        label = { Text("Trends", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                when (insightsTab) {
                    0 -> {
                        // --- CATEGORIES VIEW ---
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(200.dp)) {
                                var startAngle = -90f
                                categorySpendings.forEachIndexed { index, (_, _, pct) ->
                                    val sweepAngle = pct * 360f
                                    drawArc(
                                        color = sliceColors[index % sliceColors.size],
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    startAngle += sweepAngle
                                }
                                if (categorySpendings.isEmpty()) {
                                    drawArc(color = Color.LightGray.copy(alpha = 0.2f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round))
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$${"%,.2f".format(totalSpent)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                                Text(text = "Total Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            categorySpendings.forEachIndexed { index, (name, spent, pct) ->
                                val color = sliceColors[index % sliceColors.size]
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "$${"%,.2f".format(spent)}", fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                                Text(text = "${(pct * 100).toInt()}%", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth().height(4.dp), color = color, trackColor = color.copy(alpha = 0.1f), strokeCap = StrokeCap.Round)
                                }
                            }
                        }
                    }
                    1 -> {
                        // --- 50/30/20 RULE VIEW ---
                        val needsKeywords = listOf("housing", "rent", "mortgage", "food", "groceries", "utility", "electric", "water", "gas", "insurance", "transport", "car", "health", "bill")
                        val savingsKeywords = listOf("save", "saving", "invest", "investment", "emergency", "debt", "loan")
                        
                        val totals = remember(categorySpendings) {
                            var needs = 0.0
                            var wants = 0.0
                            var savings = 0.0
                            categorySpendings.forEach { (name, spent, _) ->
                                val lowerName = name.lowercase()
                                when {
                                    needsKeywords.any { lowerName.contains(it) } -> needs += spent
                                    savingsKeywords.any { lowerName.contains(it) } -> savings += spent
                                    else -> wants += spent
                                }
                            }
                            Triple(needs, wants, savings)
                        }
                        
                        val (needsSpent, wantsSpent, savingsSpent) = totals
                        val grandTotal = needsSpent + wantsSpent + savingsSpent
                        val needsPct = if (grandTotal > 0) (needsSpent / grandTotal).toFloat() else 0f
                        val wantsPct = if (grandTotal > 0) (wantsSpent / grandTotal).toFloat() else 0f
                        val savingsPct = if (grandTotal > 0) (savingsSpent / grandTotal).toFloat() else 0f

                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            RuleMeter(label = "Needs", actual = needsPct, target = 0.50f, color = Color(0xFF0D9488))
                            RuleMeter(label = "Wants", actual = wantsPct, target = 0.30f, color = Color(0xFF3B82F6))
                            RuleMeter(label = "Savings", actual = savingsPct, target = 0.20f, color = Color(0xFFF59E0B))
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The 50/30/20 rule suggests spending 50% on needs, 30% on wants, and 20% on savings or debt repayment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    2 -> {
                        // --- TRENDS VIEW (Simplified for MVP) ---
                        Text(
                            text = "Historical trends coming soon. This view will compare your total expenses over the last 3 months.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 32.dp).fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Icon(Lucide.TrendingUp, contentDescription = null, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleMeter(label: String, actual: Float, target: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${(actual * 100).toInt()}% of total",
                style = MaterialTheme.typography.labelMedium,
                color = if (actual > target + 0.05f && label != "Savings") MaterialTheme.colorScheme.error else color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth(target).fillMaxHeight().background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)))
            LinearProgressIndicator(progress = { actual.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxSize(), color = color, trackColor = Color.Transparent, strokeCap = StrokeCap.Round)
            Box(modifier = Modifier.fillMaxWidth(target).fillMaxHeight().padding(end = 1.dp).width(2.dp).background(color.copy(alpha = 0.5f)).align(Alignment.CenterStart))
        }
        Text(text = "Target: ${(target * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
    }
}
