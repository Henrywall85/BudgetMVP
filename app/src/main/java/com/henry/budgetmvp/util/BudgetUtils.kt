package com.henry.budgetmvp.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val parts = originalText.split(".")
        val integerPart = parts[0]
        val fractionalPart = if (parts.size > 1) "." + parts[1] else ""

        val formattedInteger = integerPart.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()

        val newText = formattedInteger + fractionalPart

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset > integerPart.length) {
                    return formattedInteger.length + (offset - integerPart.length)
                }
                var originalProcessed = 0
                var transformedProcessed = 0
                for (char in formattedInteger) {
                    if (char != ',') {
                        originalProcessed++
                    }
                    transformedProcessed++
                    if (originalProcessed == offset) return transformedProcessed
                }
                return transformedProcessed
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val actualOffset = offset.coerceAtMost(newText.length)
                var originalIdx = 0
                for (i in 0 until actualOffset) {
                    if (newText[i] != ',') {
                        originalIdx++
                    }
                }
                return originalIdx
            }
        }

        return TransformedText(AnnotatedString(newText), offsetMapping)
    }
}

fun calculateNextPayday(lastPaydayIso: String, frequency: String): String {
    if (frequency == "Irregular") return "As Received"
    return try {
        val lastDate = LocalDate.parse(lastPaydayIso)
        val nextDate = when (frequency) {
            "Weekly" -> lastDate.plusWeeks(1)
            "Bi-Weekly" -> lastDate.plusWeeks(2)
            "Monthly" -> lastDate.plusMonths(1)
            else -> lastDate
        }
        nextDate.format(DateTimeFormatter.ofPattern("MMM dd"))
    } catch (e: Exception) {
        "TBD"
    }
}

fun formatIsoDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("MMM dd"))
    } catch (e: Exception) {
        isoDate
    }
}

fun calculatePayDaysForMonth(
    type: String,
    anchorDate: String,
    fixedDays: String,
    currentYearMonth: java.time.YearMonth
): List<Int> {
    return when (type) {
        "WEEKLY", "BI_WEEKLY" -> {
            if (anchorDate.isBlank()) emptyList()
            else {
                try {
                    val anchor = LocalDate.parse(anchorDate)
                    val interval = if (type == "WEEKLY") 7L else 14L
                    val days = mutableListOf<Int>()
                    var current = anchor
                    val startOfMonth = currentYearMonth.atDay(1)
                    val endOfMonth = currentYearMonth.atEndOfMonth()
                    while (current.isAfter(endOfMonth)) current = current.minusDays(interval)
                    while (current.isBefore(startOfMonth)) current = current.plusDays(interval)
                    while (!current.isAfter(endOfMonth)) {
                        if (!current.isBefore(startOfMonth)) { days.add(current.dayOfMonth) }
                        current = current.plusDays(interval)
                    }
                    days
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
        else -> {
            fixedDays.split(",").filter { it.isNotBlank() }.mapNotNull { it.trim().toIntOrNull() }
        }
    }
}

fun com.henry.budgetmvp.data.IncomeStream.getPayDaysCountForMonth(yearMonth: java.time.YearMonth): Int {
    val days = calculatePayDaysForMonth(payScheduleType, anchorDate, payDays, yearMonth)
    return if (days.isNotEmpty()) days.size else when (payScheduleType) {
        "WEEKLY" -> 4
        "BI_WEEKLY" -> 2
        "TWICE_MONTHLY" -> 2
        "MONTHLY" -> 1
        else -> payDays.split(",").filter { it.isNotBlank() }.size.coerceAtLeast(1)
    }
}

fun com.henry.budgetmvp.data.IncomeStream.getMonthlyGoalForMonth(yearMonth: java.time.YearMonth): Double {
    val count = getPayDaysCountForMonth(yearMonth)
    return monthlyAmount * count
}
