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
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.CategoryWithItems
import com.henry.budgetmvp.data.EnvelopeItem
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.util.calculatePayDaysForMonth
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

sealed class CalendarEventEntry(open val dayOfMonth: Int) {
    data class BillEntry(val item: EnvelopeItem) : CalendarEventEntry(item.dueDay ?: 1)
    data class PaydayEntry(
        val sourceName: String,
        val paydayNum: Int,
        val paycheckAmount: Double,
        val isReceived: Boolean,
        val periodStart: LocalDate,
        val periodEnd: LocalDate
    ) : CalendarEventEntry(paydayNum)
}

@Composable
fun CalendarScreen(
    categoriesWithItems: List<CategoryWithItems>,
    filteredTransactions: List<BudgetTransaction>,
    streams: List<com.henry.budgetmvp.data.IncomeStream> = emptyList(),
    currentDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditItem: (EnvelopeItem) -> Unit,
    onMarkPaid: (EnvelopeItem, Double) -> Unit,
) {
    var calendarViewMode by remember { mutableIntStateOf(0) } // 0: Calendar Grid, 1: Paycheck Planner

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

    val today = LocalDate.now()

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

        // --- MODE SWITCHER ---
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            SegmentedButton(
                selected = calendarViewMode == 0,
                onClick = { calendarViewMode = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Calendar Grid", style = MaterialTheme.typography.labelSmall) }
            )
            SegmentedButton(
                selected = calendarViewMode == 1,
                onClick = { calendarViewMode = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Paycheck Planner", style = MaterialTheme.typography.labelSmall) }
            )
        }

        if (calendarViewMode == 0) {
            // --- CALENDAR GRID VIEW ---
            val yearMonth = remember(currentDate) { YearMonth.from(currentDate) }
            val daysInMonth = yearMonth.lengthOfMonth()
            val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7
            var selectedDay by remember { mutableStateOf<Int?>(null) }
            val itemsByDueDay = remember(allItemsWithDueDates) {
                allItemsWithDueDates.groupBy { it.dueDay }
            }
            val isCurrentMonth = today.year == currentDate.year && today.month == currentDate.month

            val totalBillsAmount = allItemsWithDueDates.sumOf { it.targetAmount }
            val totalPaidAmount = allItemsWithDueDates.sumOf { item ->
                val spent = spentByItemId[item.id] ?: 0.0
                spent.coerceAtMost(item.targetAmount)
            }

            // Calculate monthly paydays & actual/planned income entries
            val monthlyPaydayEntries = remember(streams, filteredTransactions, currentDate) {
                val list = mutableListOf<CalendarEventEntry.PaydayEntry>()
                val incomeTransactions = filteredTransactions.filter { it.type == TransactionType.INCOME }
                val matchedTxIds = mutableSetOf<String>()

                streams.forEach { stream ->
                    val days = calculatePayDaysForMonth(stream.payScheduleType, stream.anchorDate, stream.payDays, yearMonth)
                    days.forEach { dayNum ->
                        val paydayDate = yearMonth.atDay(dayNum)
                        val info = calculatePayPeriodDetails(stream, paydayDate)
                        val start = info?.nextPayday ?: paydayDate
                        val end = info?.nextPeriodEnd ?: paydayDate

                        val matchingTx = incomeTransactions.find { tx ->
                            try {
                                val txDate = LocalDate.parse(tx.date)
                                txDate.dayOfMonth == dayNum && (tx.incomeStreamId == stream.id || tx.incomeStreamId.isNullOrBlank()) && !matchedTxIds.contains(tx.id)
                            } catch (e: Exception) {
                                false
                            }
                        }

                        if (matchingTx != null) {
                            matchedTxIds.add(matchingTx.id)
                        }

                        val isReceived = matchingTx != null
                        val actualOrPlannedAmount = matchingTx?.amount ?: stream.monthlyAmount

                        list.add(
                            CalendarEventEntry.PaydayEntry(
                                sourceName = if (matchingTx != null && matchingTx.merchant.isNotBlank()) matchingTx.merchant else stream.sourceName,
                                paydayNum = dayNum,
                                paycheckAmount = actualOrPlannedAmount,
                                isReceived = isReceived,
                                periodStart = start,
                                periodEnd = end
                            )
                        )
                    }
                }

                incomeTransactions.filter { !matchedTxIds.contains(it.id) }.forEach { tx ->
                    try {
                        val txDate = LocalDate.parse(tx.date)
                        val dayNum = txDate.dayOfMonth
                        list.add(
                            CalendarEventEntry.PaydayEntry(
                                sourceName = if (tx.merchant.isNotBlank()) tx.merchant else "Income",
                                paydayNum = dayNum,
                                paycheckAmount = tx.amount,
                                isReceived = true,
                                periodStart = txDate,
                                periodEnd = txDate
                            )
                        )
                    } catch (ignored: Exception) {}
                }

                list
            }

            val paydaysByDay = remember(monthlyPaydayEntries) {
                monthlyPaydayEntries.groupBy { it.paydayNum }
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
                                            val itemsOnDay = itemsByDueDay[day] ?: emptyList()
                                            val paydaysOnDay = paydaysByDay[day] ?: emptyList()

                                            if (paydaysOnDay.isNotEmpty() || itemsOnDay.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (paydaysOnDay.isNotEmpty()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF059669))
                                                        )
                                                    }
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
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredTimelineEntries = remember(allItemsWithDueDates, monthlyPaydayEntries, selectedDay) {
                val allEntries = mutableListOf<CalendarEventEntry>()
                allItemsWithDueDates.forEach { allEntries.add(CalendarEventEntry.BillEntry(it)) }
                allEntries.addAll(monthlyPaydayEntries)

                val list = if (selectedDay == null) allEntries
                else allEntries.filter { it.dayOfMonth == selectedDay }
                
                list.sortedBy { it.dayOfMonth }
            }

            // --- FILTER HEADER & SHOW ALL CHIP ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedDay == null) {
                        "ALL EVENTS THIS MONTH (${filteredTimelineEntries.size})"
                    } else {
                        "EVENTS ON ${currentDate.format(DateTimeFormatter.ofPattern("MMM"))} $selectedDay (${filteredTimelineEntries.size})"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                )

                if (selectedDay != null) {
                    AssistChip(
                        onClick = { selectedDay = null },
                        label = { Text("Show All", style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(Lucide.X, contentDescription = "Clear Filter", modifier = Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 170.dp)
            ) {
                if (filteredTimelineEntries.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (selectedDay == null) "No scheduled events this month." else "No events scheduled for Day $selectedDay.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedDay != null) {
                                    TextButton(onClick = { selectedDay = null }) {
                                        Text("View all events for ${currentDate.format(DateTimeFormatter.ofPattern("MMMM"))}")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredTimelineEntries) { entry ->
                        when (entry) {
                            is CalendarEventEntry.BillEntry -> {
                                val item = entry.item
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
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(urgencyColor))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                Text(
                                                    text = "Due Day ${item.dueDay}${getOrdinalSuffix(item.dueDay ?: 0)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$${"%,.2f".format(item.targetAmount)}",
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { onMarkPaid(item, item.targetAmount) },
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = if (isFunded) Color(0xFF059669).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            ) {
                                                Icon(
                                                    Lucide.Check,
                                                    contentDescription = "Mark Paid",
                                                    tint = if (isFunded) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is CalendarEventEntry.PaydayEntry -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (entry.isReceived) Color(0xFF059669).copy(alpha = 0.12f) else Color(0xFF059669).copy(alpha = 0.06f)
                                    ),
                                    border = BorderStroke(1.dp, if (entry.isReceived) Color(0xFF059669).copy(alpha = 0.4f) else Color(0xFF059669).copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF059669).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Lucide.HandCoins,
                                                    contentDescription = "Payday",
                                                    tint = Color(0xFF059669),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${if (entry.isReceived) "Income" else "Payday"}: ${entry.sourceName.ifBlank { "Income Stream" }}",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                val periodStr = if (entry.periodStart == entry.periodEnd) "Received" else "Pay Period: ${entry.periodStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${entry.periodEnd.format(DateTimeFormatter.ofPattern("MMM d"))}"
                                                Text(
                                                    text = periodStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (entry.isReceived) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "+$${"%,.2f".format(entry.paycheckAmount)}",
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color(0xFF059669)
                                            )
                                            Surface(
                                                color = if (entry.isReceived) Color(0xFF059669).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (entry.isReceived) "✓ Received" else "Planned",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (entry.isReceived) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        } else {
            // --- EVERYDOLLAR PAYCHECK PLANNER VIEW ---
            val currentYearMonth = remember(currentDate) { YearMonth.from(currentDate) }

            data class PaycheckBucket(
                val index: Int,
                val sourceName: String,
                val payday: Int,
                val incomeAmount: Double,
                val periodStartDay: Int,
                val periodEndDay: Int
            )

            val paycheckBuckets = remember(streams, currentDate) {
                val events = mutableListOf<Triple<String, Int, Double>>()
                streams.forEach { stream ->
                    val days = calculatePayDaysForMonth(stream.payScheduleType, stream.anchorDate, stream.payDays, currentYearMonth)
                    if (days.isEmpty()) {
                        events.add(Triple(stream.sourceName, 1, stream.monthlyAmount))
                    } else {
                        val perDay = stream.monthlyAmount
                        days.forEach { d -> events.add(Triple(stream.sourceName, d, perDay)) }
                    }
                }
                val sortedEvents = events.sortedBy { it.second }
                val totalDays = currentYearMonth.lengthOfMonth()

                sortedEvents.mapIndexed { idx, (source, day, amount) ->
                    val start = day
                    val end = if (idx == sortedEvents.lastIndex) totalDays else sortedEvents[idx + 1].second - 1
                    PaycheckBucket(
                        index = idx + 1,
                        sourceName = source,
                        payday = day,
                        incomeAmount = amount,
                        periodStartDay = start,
                        periodEndDay = end.coerceAtLeast(start)
                    )
                }
            }

            var selectedBucketIndex by remember { mutableStateOf(0) }
            val activeBucket = paycheckBuckets.getOrNull(selectedBucketIndex) ?: paycheckBuckets.firstOrNull()

            if (paycheckBuckets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No income sources configured for ${currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}.\nAdd an income source in Budget to plan by paycheck.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (activeBucket != null) {
                val assignedBills = remember(allItemsWithDueDates, activeBucket) {
                    allItemsWithDueDates.filter { item ->
                        val due = item.dueDay ?: 1
                        due in activeBucket.periodStartDay..activeBucket.periodEndDay
                    }.sortedBy { it.dueDay }
                }
                val totalAssignedBills = assignedBills.sumOf { it.targetAmount }
                val safeBuffer = activeBucket.incomeAmount - totalAssignedBills

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 170.dp)
                ) {
                    item {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(paycheckBuckets.size) { idx ->
                                val bucket = paycheckBuckets[idx]
                                val isSelected = idx == selectedBucketIndex
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedBucketIndex = idx },
                                    leadingIcon = { Icon(Lucide.HandCoins, null, modifier = Modifier.size(16.dp), tint = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant) },
                                    label = { Text("Paycheck ${bucket.index} • Day ${bucket.payday} ($${"%,.0f".format(bucket.incomeAmount)})", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF059669).copy(alpha = 0.15f), selectedLabelColor = Color(0xFF059669))
                                )
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = if (safeBuffer >= 0) Color(0xFF059669).copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            border = BorderStroke(1.5.dp, if (safeBuffer >= 0) Color(0xFF059669).copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("${activeBucket.sourceName} (Paycheck ${activeBucket.index})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        val startLocalDate = currentYearMonth.atDay(activeBucket.periodStartDay.coerceIn(1, currentYearMonth.lengthOfMonth()))
                                        val endLocalDate = currentYearMonth.atDay(activeBucket.periodEndDay.coerceIn(1, currentYearMonth.lengthOfMonth()))
                                        val dateRangeText = "${startLocalDate.format(DateTimeFormatter.ofPattern("MMM d"))} – ${endLocalDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
                                        Text("Funding Period: $dateRangeText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("+$${"%,.2f".format(activeBucket.incomeAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Assigned Bills", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("-$${"%,.2f".format(totalAssignedBills)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(if (safeBuffer >= 0) "🛡️ Safe Cash Buffer" else "⚠️ Cash Deficit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (safeBuffer >= 0) Color(0xFF059669) else MaterialTheme.colorScheme.error)
                                        Text("$${"%,.2f".format(safeBuffer)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = if (safeBuffer >= 0) Color(0xFF059669) else MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    item { Text("BILLS DUE IN THIS PERIOD (${assignedBills.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp)) }
                    if (assignedBills.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                                Text("No bills due between Day ${activeBucket.periodStartDay} and Day ${activeBucket.periodEndDay}.\nAll $${"%,.2f".format(activeBucket.incomeAmount)} is available for everyday spending!", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(assignedBills) { item ->
                            val spent = spentByItemId[item.id] ?: 0.0
                            val isFunded = spent >= item.targetAmount - 0.001
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onEditItem(item) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
                                            Text("Day ${item.dueDay}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(if (isFunded) "✓ Paid / Funded" else "$${"%,.2f".format(spent)} of $${"%,.2f".format(item.targetAmount)}", style = MaterialTheme.typography.labelSmall, color = if (isFunded) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("$${"%,.2f".format(item.targetAmount)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { onMarkPaid(item, item.targetAmount) },
                                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isFunded) Color(0xFF059669).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                        ) { Icon(Lucide.Check, null, tint = if (isFunded) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                                    }
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
        statuses.contains(Color(0xFFC62828)) -> Color(0xFFC62828)
        statuses.contains(Color(0xFFEF6C00)) -> Color(0xFFEF6C00)
        else -> Color(0xFF2E7D32)
    }
}

private fun getItemStatusColor(item: EnvelopeItem, spent: Double, today: LocalDate, currentDate: LocalDate): Color {
    val dueDay = item.dueDay ?: return Color.Gray
    val isFunded = spent >= item.targetAmount - 0.001
    if (isFunded) return Color(0xFF2E7D32)
    val isCurrentMonth = today.year == currentDate.year && today.month == currentDate.month
    if (!isCurrentMonth) {
        return if (currentDate.isBefore(today.withDayOfMonth(1))) Color(0xFFC62828) else Color.Gray
    }
    val daysUntil = dueDay - today.dayOfMonth
    return when {
        daysUntil < 0 -> Color(0xFFC62828)
        daysUntil == 0 -> Color(0xFFC62828)
        daysUntil in 1..5 -> Color(0xFFEF6C00)
        else -> Color.Gray
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



data class PayPeriodDetails(
    val streamName: String,
    val scheduleType: String,
    val paycheckAmount: Double,
    val currentPeriodStart: LocalDate?,
    val currentPeriodEnd: LocalDate?,
    val nextPayday: LocalDate,
    val nextPeriodEnd: LocalDate,
    val daysUntilNextPayday: Long
)

private fun calculatePayPeriodDetails(
    stream: com.henry.budgetmvp.data.IncomeStream,
    referenceDate: LocalDate
): PayPeriodDetails? {
    val type = stream.payScheduleType
    val anchorStr = stream.anchorDate
    val fixedDaysStr = stream.payDays

    return when (type) {
        "WEEKLY", "BI_WEEKLY" -> {
            val interval = if (type == "WEEKLY") 7L else 14L
            val anchor = try {
                if (anchorStr.isNotBlank()) LocalDate.parse(anchorStr) else referenceDate
            } catch (e: Exception) {
                referenceDate
            }

            var p = anchor
            if (p.isBefore(referenceDate)) {
                val daysBetween = ChronoUnit.DAYS.between(p, referenceDate)
                val cycles = daysBetween / interval
                p = p.plusDays(cycles * interval)
            } else {
                while (p.minusDays(interval) >= referenceDate) {
                    p = p.minusDays(interval)
                }
            }

            val currentStart: LocalDate?
            val currentEnd: LocalDate?
            val nextPayday: LocalDate
            val nextEnd: LocalDate

            if (p <= referenceDate) {
                if (p == referenceDate) {
                    currentStart = p
                    currentEnd = p.plusDays(interval - 1)
                    nextPayday = p
                    nextEnd = p.plusDays(interval - 1)
                } else {
                    currentStart = p
                    currentEnd = p.plusDays(interval - 1)
                    nextPayday = p.plusDays(interval)
                    nextEnd = nextPayday.plusDays(interval - 1)
                }
            } else {
                currentStart = p.minusDays(interval)
                currentEnd = p.minusDays(1)
                nextPayday = p
                nextEnd = p.plusDays(interval - 1)
            }

            val daysUntil = if (nextPayday >= referenceDate) {
                ChronoUnit.DAYS.between(referenceDate, nextPayday)
            } else 0L

            val yearMonth = YearMonth.from(nextPayday)
            val payDaysInMonth = calculatePayDaysForMonth(type, anchorStr, fixedDaysStr, yearMonth).size
            val paycheckAmount = stream.monthlyAmount

            PayPeriodDetails(
                streamName = stream.sourceName,
                scheduleType = type,
                paycheckAmount = paycheckAmount,
                currentPeriodStart = currentStart,
                currentPeriodEnd = currentEnd,
                nextPayday = nextPayday,
                nextPeriodEnd = nextEnd,
                daysUntilNextPayday = daysUntil
            )
        }
        else -> {
            val fixedDays = fixedDaysStr.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .sorted()
                .ifEmpty { listOf(1) }

            val paydays = mutableListOf<LocalDate>()
            var ym = YearMonth.from(referenceDate).minusMonths(1)
            for (i in 0..4) {
                fixedDays.forEach { d ->
                    val dayClamped = d.coerceAtMost(ym.lengthOfMonth())
                    paydays.add(ym.atDay(dayClamped))
                }
                ym = ym.plusMonths(1)
            }
            paydays.sort()

            val lastPayday = paydays.lastOrNull { it <= referenceDate }
            val nextPayday = paydays.firstOrNull { if (lastPayday == referenceDate) it >= referenceDate else it > referenceDate }
                ?: referenceDate
            val followingPayday = paydays.firstOrNull { it > nextPayday }
                ?: nextPayday.plusMonths(1)

            val currentStart: LocalDate?
            val currentEnd: LocalDate?
            val nextEnd: LocalDate

            if (lastPayday != null) {
                currentStart = lastPayday
                currentEnd = nextPayday.minusDays(1)
            } else {
                currentStart = null
                currentEnd = null
            }

            nextEnd = followingPayday.minusDays(1)

            val daysUntil = if (nextPayday >= referenceDate) {
                ChronoUnit.DAYS.between(referenceDate, nextPayday)
            } else 0L

            val yearMonth = YearMonth.from(nextPayday)
            val payDaysInMonth = calculatePayDaysForMonth(type, anchorStr, fixedDaysStr, yearMonth).size
            val paycheckAmount = stream.monthlyAmount

            PayPeriodDetails(
                streamName = stream.sourceName,
                scheduleType = type,
                paycheckAmount = paycheckAmount,
                currentPeriodStart = currentStart,
                currentPeriodEnd = currentEnd,
                nextPayday = nextPayday,
                nextPeriodEnd = nextEnd,
                daysUntilNextPayday = daysUntil
            )
        }
    }
}

