package com.henry.budgetmvp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import java.util.UUID

@Entity(tableName = "multi_income_table")
data class IncomeStream(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val sourceName: String,
    val amount: Double,
    val frequency: String,
    val lastPayday: String
)

@Entity(tableName = "budget_category_table")
data class BudgetCategory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
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
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val categoryId: String,
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
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: TransactionType,
    val amount: Double,
    val date: String,
    val merchant: String = "",
    val note: String = "",
    val itemId: String? = null, // For Expenses
    val incomeStreamId: String? = null // For Income
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
