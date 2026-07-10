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
