package com.henry.budgetmvp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.IncomeStream
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.util.ThousandsSeparatorTransformation
import com.henry.budgetmvp.util.formatIsoDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun TotalPoolCard(
    total: Double,
    currentDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit
) {
    val dateText = remember(currentDate) { 
        currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
                
                Text(
                    text = dateText.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                    modifier = Modifier.clickable { onMonthClick() }
                )

                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Lucide.ArrowRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${"%,.2f".format(total)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Ready to Assign",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun IncomeDetailsCard(
    stream: IncomeStream,
    receivedAmount: Double,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stream.sourceName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Monthly Income",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (stream.monthlyAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                // Progress Section
                val progress = (receivedAmount / stream.monthlyAmount).toFloat().coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    label = "income_progress"
                )
                
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF2E7D32), // Consistency with Success Green
                    trackColor = Color(0xFFE5E7EB),
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (stream.monthlyAmount > 0) "Goal: $${"%,.2f".format(stream.monthlyAmount)}" else "No Monthly Goal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$${"%,.2f".format(receivedAmount)} received",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDetailSheet(
    stream: IncomeStream,
    transactions: List<BudgetTransaction>,
    onDismiss: () -> Unit,
    onEditStream: () -> Unit,
    onEditTransaction: (BudgetTransaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val receivedAmount = transactions.sumOf { it.amount }
    val remaining = stream.monthlyAmount - receivedAmount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stream.sourceName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onEditStream) {
                        Icon(Lucide.EllipsisVertical, contentDescription = "Edit Source")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (stream.monthlyAmount > 0) {
                            Column {
                                Text("Planned", style = MaterialTheme.typography.labelSmall)
                                Text("$${"%,.2f".format(stream.monthlyAmount)}", fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text(if (stream.monthlyAmount > 0) "Received" else "Total Received", style = MaterialTheme.typography.labelSmall)
                            Text("$${"%,.2f".format(receivedAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                        if (stream.monthlyAmount > 0) {
                            Column {
                                Text("Pending", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "$${"%,.2f".format(if (remaining > 0) remaining else 0.0)}", 
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF059669)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PAYMENT HISTORY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No payments recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    transactions.forEach { transaction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditTransaction(transaction) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = formatIsoDate(transaction.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (transaction.merchant.isNotBlank()) transaction.merchant else "Income Payment",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "+$${"%,.2f".format(transaction.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeEntrySheet(
    targetStream: IncomeStream?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, String) -> Unit,
    onDelete: () -> Unit
) {
    var source by remember { mutableStateOf(targetStream?.sourceName ?: "") }
    var amount by remember { mutableStateOf(targetStream?.monthlyAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    
    var payScheduleType by remember { mutableStateOf(targetStream?.payScheduleType ?: "BI_WEEKLY") }
    var anchorDate by remember { mutableStateOf(targetStream?.anchorDate ?: "") }
    var selectedPayDays by remember { 
        mutableStateOf(
            targetStream?.payDays?.split(",")?.filter { it.isNotBlank() }?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
        ) 
    }
    
    var showPaydayPicker by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            source.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && 
            (if (payScheduleType == "WEEKLY" || payScheduleType == "BI_WEEKLY") anchorDate.isNotBlank() else selectedPayDays.isNotEmpty())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = if (targetStream != null) "Edit ${targetStream.sourceName}" else "Add New Income Source",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Income Source Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (input.count { it.toString() == "." } <= 1 && input.all { it.isDigit() || it.toString() == "." }) { amount = input }
                    },
                    label = { Text("Planned Monthly Amount") },
                    placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    visualTransformation = commaTransformation,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Multi-Select Payday Trigger Card
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pay Schedule", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    
                    OutlinedCard(
                        onClick = { showPaydayPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Lucide.CalendarDays,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    val scheduleLabel = when(payScheduleType) {
                                        "WEEKLY" -> "Weekly (Starting ${anchorDate})"
                                        "BI_WEEKLY" -> "Every 2 Weeks (Starting ${anchorDate})"
                                        "TWICE_MONTHLY" -> "Twice Monthly"
                                        "MONTHLY" -> "Monthly"
                                        else -> "Custom Dates"
                                    }
                                    Text(
                                        text = scheduleLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (payScheduleType == "WEEKLY" || payScheduleType == "BI_WEEKLY") "Dynamic Schedule" 
                                               else if (selectedPayDays.isEmpty()) "No days selected" 
                                               else selectedPayDays.sorted().joinToString(", ") { "Day $it" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Lucide.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { 
                        onConfirm(
                            source, 
                            amount.toDoubleOrNull() ?: 0.0, 
                            payScheduleType,
                            anchorDate,
                            selectedPayDays.sorted().joinToString(",")
                        ) 
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (targetStream != null) "Update Income Source" else "Save Income Source")
                }

                if (targetStream != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Lucide.Trash2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Income Source")
                    }
                }
            }
        }
    }

    if (showPaydayPicker) {
        Material3MultiDatePickerDialog(
            initialSelectedDays = selectedPayDays,
            initialScheduleType = payScheduleType,
            initialAnchorDate = anchorDate,
            onDismiss = { showPaydayPicker = false },
            onConfirm = { days, type, anchor -> 
                selectedPayDays = days
                payScheduleType = type
                anchorDate = anchor
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3MultiDatePickerDialog(
    initialSelectedDays: Set<Int>,
    initialScheduleType: String,
    initialAnchorDate: String,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>, String, String) -> Unit
) {
    var selectedDays by remember { mutableStateOf(initialSelectedDays) }
    var scheduleType by remember { mutableStateOf(initialScheduleType) }
    var anchorDate by remember { mutableStateOf(initialAnchorDate) }
    
    var currentMonthDate by remember { mutableStateOf(LocalDate.now()) }
    
    val modes = listOf("BI_WEEKLY", "WEEKLY", "MONTHLY", "CUSTOM")
    val modeLabels = listOf("Bi-Weekly", "Weekly", "Monthly", "Custom")

    val daysInMonth = currentMonthDate.lengthOfMonth()
    val firstDayOfWeek = currentMonthDate.withDayOfMonth(1).dayOfWeek.value % 7 // Sunday = 0
    val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(360.dp)
                    .wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 1. Material 3 Header
                    Text(
                        text = "SET RECURRING SCHEDULE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when(scheduleType) {
                            "WEEKLY", "BI_WEEKLY" -> if (anchorDate.isBlank()) "Pick start date" else "Starting ${anchorDate}"
                            else -> if (selectedDays.isEmpty()) "Select paydays" else selectedDays.sorted().joinToString(", ") { "Day $it" }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Frequency Mode Selector Chips
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(modes.size) { index ->
                            val mode = modes[index]
                            FilterChip(
                                selected = scheduleType == mode,
                                onClick = { 
                                    if (scheduleType != mode) {
                                        scheduleType = mode
                                        selectedDays = emptySet()
                                        anchorDate = ""
                                    }
                                },
                                label = { Text(modeLabels[index], style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF059669).copy(alpha = 0.15f),
                                    selectedLabelColor = Color(0xFF059669)
                                )
                            )
                        }
                    }

                    // Helper Description Text
                    val helperText = when (scheduleType) {
                        "BI_WEEKLY" -> "Bi-Weekly: Tap the day you last got paid to set the anchor."
                        "WEEKLY" -> "Weekly: Tap any payday to set the cycle."
                        "MONTHLY" -> "Monthly: Tap your monthly payday."
                        else -> "Custom: Tap any combination of days."
                    }
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Month Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonthDate = currentMonthDate.minusMonths(1) }) {
                            Icon(Lucide.ChevronLeft, contentDescription = "Previous Month")
                        }
                        Text(
                            text = currentMonthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { currentMonthDate = currentMonthDate.plusMonths(1) }) {
                            Icon(Lucide.ChevronRight, contentDescription = "Next Month")
                        }
                    }

                    // 4. Weekday Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        dayNames.forEach { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // 5. Calendar Days Grid
                    val totalSlots = firstDayOfWeek + daysInMonth
                    val rows = (totalSlots + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0 until 7) {
                                    val dayIndex = row * 7 + col
                                    val dayNumber = dayIndex - firstDayOfWeek + 1

                                    if (dayNumber in 1..daysInMonth) {
                                        val date = currentMonthDate.withDayOfMonth(dayNumber)
                                        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        
                                        // Visual Logic
                                        val isHighlighted = when(scheduleType) {
                                            "WEEKLY" -> {
                                                if (anchorDate.isBlank()) false 
                                                else {
                                                    val anchor = LocalDate.parse(anchorDate)
                                                    java.time.temporal.ChronoUnit.DAYS.between(anchor, date) % 7 == 0L
                                                }
                                            }
                                            "BI_WEEKLY" -> {
                                                if (anchorDate.isBlank()) false 
                                                else {
                                                    val anchor = LocalDate.parse(anchorDate)
                                                    java.time.temporal.ChronoUnit.DAYS.between(anchor, date) % 14 == 0L
                                                }
                                            }
                                            else -> selectedDays.contains(dayNumber)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isHighlighted) Color(0xFF059669) else Color.Transparent)
                                                .clickable {
                                                    when (scheduleType) {
                                                        "WEEKLY", "BI_WEEKLY" -> {
                                                            anchorDate = dateStr
                                                        }
                                                        "MONTHLY" -> {
                                                            selectedDays = setOf(dayNumber)
                                                        }
                                                        else -> {
                                                            selectedDays = if (isHighlighted) selectedDays - dayNumber else selectedDays + dayNumber
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isHighlighted) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(34.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. Actions Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConfirm(selectedDays, scheduleType, anchorDate); onDismiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            enabled = if (scheduleType == "WEEKLY" || scheduleType == "BI_WEEKLY") anchorDate.isNotBlank() else selectedDays.isNotEmpty()
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}
