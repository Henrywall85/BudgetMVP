package com.henry.budgetmvp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.BudgetCategory
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.EnvelopeItem
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.util.ThousandsSeparatorTransformation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun CategoryHeader(
    category: BudgetCategory,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditCategory: () -> Unit
) {
    val categoryIcon = remember(category.name) {
        getCategoryIcon(category.name)
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Icon Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = category.name.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    imageVector = if (isExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
            Row {
                IconButton(onClick = onEditCategory) {
                    Icon(Lucide.EllipsisVertical, contentDescription = "Edit Category", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                }
            }
        }
    }
}

private fun getCategoryIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("housing") || lower.contains("rent") || lower.contains("mortgage") || lower.contains("home") -> Lucide.House
        lower.contains("food") || lower.contains("grocer") || lower.contains("dining") || lower.contains("eat") -> Lucide.Utensils
        lower.contains("transport") || lower.contains("car") || lower.contains("gas") || lower.contains("fuel") || lower.contains("auto") -> Lucide.Car
        lower.contains("utilit") || lower.contains("electric") || lower.contains("water") || lower.contains("gas") || lower.contains("trash") -> Lucide.Zap
        lower.contains("personal") || lower.contains("fun") || lower.contains("entertainment") || lower.contains("hobby") || lower.contains("gift") -> Lucide.Smile
        lower.contains("health") || lower.contains("insuranc") || lower.contains("medical") -> Lucide.Activity
        lower.contains("save") || lower.contains("invest") || lower.contains("debt") || lower.contains("loan") -> Lucide.TrendingUp
        else -> Lucide.Tag
    }
}

@Composable
fun EnvelopeItemRow(
    item: EnvelopeItem, 
    spentAmount: Double, 
    todayDay: Int,
    onClick: () -> Unit
) {
    val goalAmount = item.targetAmount
    val spentRatio = if (goalAmount > 0) (spentAmount / goalAmount).toFloat() else 0f
    
    val isOverBudget = spentAmount > goalAmount + 0.001
    val percentSpent = (spentRatio * 100).toInt()
    
    val progressColor = when {
        spentRatio > 1.0f -> Color(0xFFC62828) // Red
        spentRatio > 0.8f -> Color(0xFFEF6C00) // Orange
        else -> Color(0xFF2E7D32) // Green
    }

    // Due Date Logic
    val dueStatus = remember(item.dueDay, todayDay, spentAmount, goalAmount) {
        val dueDay = item.dueDay ?: return@remember null
        val daysUntil = dueDay - todayDay
        
        when {
            daysUntil < 0 && spentAmount < goalAmount -> "Overdue"
            daysUntil == 0 -> "Due Today"
            daysUntil in 1..5 -> "Due in $daysUntil days"
            else -> "Due ${dueDay}${getOrdinalSuffix(dueDay)}"
        }
    }
    
    val dueBadgeColor = when (dueStatus) {
        "Overdue", "Due Today" -> Color(0xFFC62828)
        "Due in 1 days", "Due in 2 days", "Due in 3 days", "Due in 4 days", "Due in 5 days" -> Color(0xFFEF6C00)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Surface(
                            color = progressColor.copy(alpha = 0.1f),
                            contentColor = progressColor,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "$percentSpent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    if (dueStatus != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = dueBadgeColor.copy(alpha = 0.1f),
                            contentColor = dueBadgeColor,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = dueStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "planned",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${"%,.2f".format(item.targetAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            LinearProgressIndicator(
                progress = { spentRatio.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = progressColor,
                trackColor = Color(0xFFE5E7EB),
                strokeCap = StrokeCap.Round
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$${"%,.2f".format(spentAmount)} spent of $${"%,.2f".format(goalAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (isOverBudget) {
                    Text(
                        text = "OVER BY $${"%,.2f".format(spentAmount - goalAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Black
                    )
                } else {
                    val remaining = goalAmount - spentAmount
                    Text(
                        text = "$${"%,.2f".format(if (kotlin.math.abs(remaining) < 0.001) 0.0 else remaining)} left",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDateSheet(
    initialDueDay: Int?,
    currentDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSaveDueDay: (Int?) -> Unit,
    onDeleteDueDate: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedDueDay by remember { mutableIntStateOf(initialDueDay ?: 1) }
    var repeatMonthly by remember { mutableStateOf(true) }
    var remindMe by remember { mutableStateOf(false) }

    var showCalendarPicker by remember { mutableStateOf(false) }

    val initialMillis = remember(currentDate, selectedDueDay) {
        val day = selectedDueDay.coerceIn(1, currentDate.lengthOfMonth())
        currentDate.withDayOfMonth(day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        initialDisplayedMonthMillis = initialMillis
    )

    val suffix = when {
        selectedDueDay in 11..13 -> "th"
        selectedDueDay % 10 == 1 -> "st"
        selectedDueDay % 10 == 2 -> "nd"
        selectedDueDay % 10 == 3 -> "rd"
        else -> "th"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title & Close Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Due Date",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Lucide.X, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Grouped Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        // Row 1: Date Picker Trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCalendarPicker = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$selectedDueDay$suffix",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7) // EveryDollar Blue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Lucide.ChevronDown,
                                    contentDescription = "Select Date",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Row 2: Repeat Monthly
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Repeat Monthly",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = repeatMonthly,
                                onCheckedChange = { repeatMonthly = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0F172A) // Dark Navy
                                )
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Row 3: Remind Me
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Remind Me",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "We'll notify you three days before it's due.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = remindMe,
                                onCheckedChange = { remindMe = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button
                Button(
                    onClick = {
                        onSaveDueDay(selectedDueDay)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)) // Dark Navy
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delete Due Date Button
                TextButton(
                    onClick = {
                        onDeleteDueDate()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Delete Due Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showCalendarPicker) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            DatePickerDialog(
                onDismissRequest = { showCalendarPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val localDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            selectedDueDay = localDate.dayOfMonth
                        }
                        showCalendarPicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showCalendarPicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    item: EnvelopeItem,
    transactions: List<BudgetTransaction>,
    unassignedFunds: Double = 0.0,
    currentDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onEditItem: () -> Unit,
    onUpdateTargetAmount: (Double) -> Unit = {},
    onUpdateDueDay: (Int?) -> Unit = {},
    onDeleteItem: () -> Unit = {},
    onEditTransaction: (BudgetTransaction) -> Unit,
    onAddTransaction: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var targetAmountState by remember(item.targetAmount) { mutableDoubleStateOf(item.targetAmount) }
    
    val spentAmount = transactions.sumOf { it.amount }
    
    // Inline Edit Mode & Sheet States
    var editMode by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDueDateSheet by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember(targetAmountState) {
        mutableStateOf(
            TextFieldValue(
                text = "%.2f".format(targetAmountState),
                selection = TextRange(0, "%.2f".format(targetAmountState).length)
            )
        )
    }

    val remaining = targetAmountState - spentAmount
    val isOverspent = remaining < -0.001

    var isFavorite by remember { mutableStateOf(false) }

    val dueDayText = remember(item.dueDay) {
        item.dueDay?.let { day ->
            val suffix = when {
                day in 11..13 -> "th"
                day % 10 == 1 -> "st"
                day % 10 == 2 -> "nd"
                day % 10 == 3 -> "rd"
                else -> "th"
            }
            "Day $day$suffix, repeats monthly"
        } ?: "No due date set"
    }

    // Real-time projected balance for the keyboard bar
    val newAmountTyped = textFieldValue.text.toDoubleOrNull() ?: 0.0
    val projectedUnassigned = unassignedFunds + (item.targetAmount - newAmountTyped)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize().imePadding()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // 1. Top Emerald Banner with Back Button
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .background(Color(0xFF059669))
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)) {
                                Icon(
                                    imageVector = Lucide.ChevronLeft,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // 2. Overlapping Avatar & Hero Title / Remaining Balance
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                            // Circular Overlapping Avatar
                            Box(
                                modifier = Modifier
                                    .offset(y = (-32).dp)
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(
                                        BorderStroke(
                                            3.dp,
                                            if (isOverspent) Color(0xFFDC2626) else Color(0xFF059669)
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.DollarSign,
                                    contentDescription = null,
                                    tint = if (isOverspent) Color(0xFFDC2626) else Color(0xFF059669),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Title & Remaining Balance Row
                            Row(
                                modifier = Modifier.fillMaxWidth().offset(y = (-16).dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                editMode = true
                                            }
                                            .padding(vertical = 2.dp, horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = "$${"%,.2f".format(spentAmount)} spent of ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        if (!editMode) {
                                            Surface(
                                                color = Color(0xFF0284C7).copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "$${"%,.2f".format(targetAmountState)}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF0284C7)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        Lucide.Pencil,
                                                        contentDescription = "Edit Amount",
                                                        tint = Color(0xFF0284C7),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("$", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                                                BasicTextField(
                                                    value = textFieldValue,
                                                    onValueChange = { textFieldValue = it },
                                                    modifier = Modifier
                                                        .width(IntrinsicSize.Min)
                                                        .focusRequester(focusRequester),
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF0284C7)
                                                    ),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                                    keyboardActions = KeyboardActions(onDone = {
                                                        val newAmount = textFieldValue.text.toDoubleOrNull() ?: targetAmountState
                                                        targetAmountState = newAmount
                                                        onUpdateTargetAmount(newAmount)
                                                        editMode = false
                                                    }),
                                                    cursorBrush = SolidColor(Color(0xFF0284C7)),
                                                    singleLine = true
                                                )
                                            }
                                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Remaining",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$${"%,.2f".format(remaining)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (isOverspent) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Settings Grouped Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                // Row 1: Planned Budget Amount
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editMode = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Lucide.DollarSign, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Planned Budget", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            Text("$${"%,.2f".format(targetAmountState)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Icon(Lucide.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Row 2: Schedule
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onEditItem() }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Lucide.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    Icon(Lucide.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Row 3: Due Date
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showDueDateSheet = true }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Lucide.Calendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Due Date", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            Text(dueDayText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Icon(Lucide.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Row 4: Fund
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Lucide.PiggyBank, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Fund", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    Icon(Lucide.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Row 5: Favorite Switch
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Lucide.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Favorite", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    Switch(checked = isFavorite, onCheckedChange = { isFavorite = it })
                                }
                            }
                        }
                    }

                    // 4. Note Card
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onEditItem() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Add a Note...",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // 5. Activity This Month
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Activity This Month",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }

                    if (transactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "No activity for this item this month.",
                                    modifier = Modifier.padding(20.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(transactions) { tx ->
                            val parsedDate = try { LocalDate.parse(tx.date) } catch (_: Exception) { LocalDate.now() }
                            val monthStr = parsedDate.format(DateTimeFormatter.ofPattern("MMM"))
                            val dayStr = parsedDate.format(DateTimeFormatter.ofPattern("d"))

                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onEditTransaction(tx) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .border(BorderStroke(1.2.dp, Color(0xFF94A3B8)), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(monthStr, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color(0xFF94A3B8))
                                                Text(dayStr, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = if (tx.merchant.isNotBlank()) tx.merchant else item.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }

                                    Text(
                                        text = "-$${"%,.2f".format(tx.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            }
                        }
                        
                        // "Planned This Month" virtual entry
                        if (targetAmountState > 0) {
                            item {
                                val monthStr = try { LocalDate.now().format(DateTimeFormatter.ofPattern("MMM")) } catch (_: Exception) { "M" }
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .border(BorderStroke(1.2.dp, Color(0xFF059669).copy(alpha = 0.5f)), shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(monthStr, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color(0xFF059669))
                                                    Text("1", style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = "Planned This Month",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color(0xFF059669)
                                            )
                                        }

                                        Text(
                                            text = "+$${"%,.2f".format(targetAmountState)}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                }
                            }
                        }
                    }

                    // 5. Delete Budget Item Button
                    item {
                        Spacer(modifier = Modifier.height(28.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = { showDeleteConfirmation = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                            ) {
                                Text(
                                    text = "Delete Budget Item",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }

                // 6. Floating Action Button (Quick Add for this item)
                if (!editMode) {
                    FloatingActionButton(
                        onClick = onAddTransaction,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp),
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Lucide.Plus, contentDescription = "Add Transaction", modifier = Modifier.size(24.dp))
                    }
                }

                // 7. KEYBOARD ACCESSORY BAR (Left to Budget)
                if (editMode) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (projectedUnassigned < -0.001) "OVER BUDGET" else "LEFT TO BUDGET",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (projectedUnassigned < -0.001) Color(0xFFDC2626) else Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$${"%,.2f".format(projectedUnassigned)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (projectedUnassigned < -0.001) Color(0xFFDC2626) else Color(0xFF059669)
                                )
                            }
                            
                            Button(
                                onClick = {
                                    val newAmount = textFieldValue.text.toDoubleOrNull() ?: targetAmountState
                                    targetAmountState = newAmount
                                    onUpdateTargetAmount(newAmount)
                                    editMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showDueDateSheet) {
                    DueDateSheet(
                        initialDueDay = item.dueDay,
                        currentDate = currentDate,
                        onDismiss = { showDueDateSheet = false },
                        onSaveDueDay = { newDay: Int? ->
                            onUpdateDueDay(newDay)
                        },
                        onDeleteDueDate = {
                            onUpdateDueDay(null)
                        }
                    )
                }

                if (showDeleteConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmation = false },
                        title = { Text("Delete Budget Item", fontWeight = FontWeight.Bold) },
                        text = { Text("Are you sure you want to delete '${item.name}'? This cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteConfirmation = false
                                    onDeleteItem()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Text("Delete", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmation = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEntrySheet(
    targetCategory: BudgetCategory?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(targetCategory?.name ?: "") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier.padding(16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (targetCategory != null) "Edit Category" else "Add New Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name (e.g., Housing, Food)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true
                )

                Button(
                    onClick = { onConfirm(name) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (targetCategory != null) "Update Category" else "Save Category")
                }

                if (targetCategory != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Lucide.Trash2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Category")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeItemEntrySheet(
    categoryId: String,
    targetItem: EnvelopeItem?,
    currentDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int?) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(targetItem?.name ?: "") }
    var targetAmount by remember { mutableStateOf(targetItem?.targetAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var selectedDueDay by remember { mutableStateOf(targetItem?.dueDay) }

    var showDatePicker by remember { mutableStateOf(false) }
    
    val initialMillis = remember(currentDate, selectedDueDay) {
        val day = (selectedDueDay ?: 1).coerceIn(1, currentDate.lengthOfMonth())
        currentDate.withDayOfMonth(day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        initialDisplayedMonthMillis = initialMillis
    )

    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            name.isNotBlank() && ((targetAmount.toDoubleOrNull() ?: 0.0) > 0)
        }
    }

    val monthName = remember(currentDate) { currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")) }
    val monthShort = remember(currentDate) { currentDate.format(DateTimeFormatter.ofPattern("MMM")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (targetItem != null) "Edit Item ($monthShort)" else "Add New Item ($monthShort)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g., Rent, Groceries)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { input ->
                        if (input.count { it == '.' } <= 1 && (input.all { it.isDigit() || it == '.' })) {
                            targetAmount = input
                        }
                    },
                    label = { Text("Planned Amount") },
                    placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    visualTransformation = commaTransformation,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = selectedDueDay?.let { "Day $it ($monthShort $it)" } ?: "",
                        onValueChange = { },
                        label = { Text("Due Date in $monthName") },
                        placeholder = { Text("Select due day", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Lucide.Calendar, contentDescription = "Select or Change Due Date")
                            }
                        },
                        supportingText = { Text("Recurring bill due day for $monthName") }
                    )
                }

                Button(
                    onClick = { 
                        onConfirm(
                            name, 
                            targetAmount.toDoubleOrNull() ?: 0.0,
                            selectedDueDay
                        ) 
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (targetItem != null) "Update Item" else "Save Item")
                }

                if (targetItem != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Lucide.Trash2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Item")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val localDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            selectedDueDay = localDate.dayOfMonth
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    Row {
                        if (selectedDueDay != null) {
                            TextButton(onClick = {
                                selectedDueDay = null
                                showDatePicker = false
                            }) { Text("Clear Date") }
                        }
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
