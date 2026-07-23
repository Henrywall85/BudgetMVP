package com.henry.budgetmvp.util

import com.henry.budgetmvp.data.IncomeStream
import java.time.LocalDate
import java.time.YearMonth

data class ScheduledPaycheck(
    val incomeStreamId: String,
    val sourceName: String,
    val date: String, // ISO
    val amount: Double
)

object PaycheckScheduler {
    fun generateSchedule(streams: List<IncomeStream>, yearMonth: YearMonth): List<ScheduledPaycheck> {
        val schedule = mutableListOf<ScheduledPaycheck>()
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()

        streams.forEach { stream ->
            if (stream.frequency == "Irregular" || stream.lastPayday.isBlank()) {
                // Irregular income doesn't have a schedule, it shows up when received
                // But for planning, we can show one "Planned" slot if the amount > 0
                if (stream.amount > 0) {
                    schedule.add(
                        ScheduledPaycheck(
                            incomeStreamId = stream.id,
                            sourceName = stream.sourceName,
                            date = startOfMonth.toString(), // Place at start of month for planning
                            amount = stream.amount
                        )
                    )
                }
                return@forEach
            }

            try {
                var currentPayday = LocalDate.parse(stream.lastPayday)
                
                // Advance to the first payday of the target month or later
                while (currentPayday.isBefore(startOfMonth)) {
                    currentPayday = advancePayday(currentPayday, stream.frequency)
                }

                // Add all paydays that fall within the target month
                while (!currentPayday.isAfter(endOfMonth)) {
                    if (!currentPayday.isBefore(startOfMonth)) {
                        schedule.add(
                            ScheduledPaycheck(
                                incomeStreamId = stream.id,
                                sourceName = stream.sourceName,
                                date = currentPayday.toString(),
                                amount = stream.amount
                            )
                        )
                    }
                    currentPayday = advancePayday(currentPayday, stream.frequency)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return schedule.sortedBy { it.date }
    }

    private fun advancePayday(current: LocalDate, frequency: String): LocalDate {
        return when (frequency) {
            "Weekly" -> current.plusWeeks(1)
            "Bi-Weekly" -> current.plusWeeks(2)
            "Monthly" -> current.plusMonths(1)
            else -> current.plusMonths(1)
        }
    }
}
