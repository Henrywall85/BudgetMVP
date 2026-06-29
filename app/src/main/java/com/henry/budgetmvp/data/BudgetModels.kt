package com.henry.budgetmvp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "multi_income_table")
data class IncomeStream(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceName: String,
    val amount: Double,
    val frequency: String,
    val lastPayday: String
)

@Entity(tableName = "budget_category_table")
data class BudgetCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(
    tableName = "envelope_item_table",
    foreignKeys = [
        ForeignKey(
            entity = BudgetCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EnvelopeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val name: String,
    val targetAmount: Double,
    val allocatedAmount: Double = 0.0
)

@Entity(
    tableName = "transaction_table",
    foreignKeys = [
        ForeignKey(
            entity = EnvelopeItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class BudgetTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: TransactionType,
    val amount: Double,
    val date: String,
    val note: String = "",
    val itemId: Int? = null // For Expenses
)

enum class TransactionType {
    INCOME, EXPENSE
}

data class CategoryWithItems(
    @Embedded val category: BudgetCategory,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val items: List<EnvelopeItem>
)
