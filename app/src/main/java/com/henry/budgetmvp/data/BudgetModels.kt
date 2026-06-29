package com.henry.budgetmvp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "multi_income_table")
data class IncomeStream(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceName: String,
    val amount: Double,
    val frequency: String,
    val lastPayday: String
)

@Entity(tableName = "expense_envelope_table")
data class ExpenseEnvelope(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val allocatedAmount: Double = 0.0
)
