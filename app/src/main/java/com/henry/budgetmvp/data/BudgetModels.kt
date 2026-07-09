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
    val userId: String = "",
    val householdId: String = "",
    val sourceName: String = "",
    val amount: Double = 0.0,
    val frequency: String = "",
    val lastPayday: String = ""
)

@Entity(tableName = "budget_category_table")
data class BudgetCategory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val householdId: String = "",
    val name: String = ""
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
    val userId: String = "",
    val householdId: String = "",
    val categoryId: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
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
    val userId: String = "",
    val householdId: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Double = 0.0,
    val date: String = "",
    val merchant: String = "",
    val note: String = "",
    val itemId: String? = null, // For Expenses
    val incomeStreamId: String? = null // For Income
)

@Entity(tableName = "user_profile_table")
data class UserProfile(
    @PrimaryKey val userId: String = "",
    val email: String = "",
    val householdId: String = "",
    val userName: String = ""
)

data class HouseholdInvite(
    val id: String = UUID.randomUUID().toString(),
    val fromEmail: String,
    val fromUserId: String,
    val toEmail: String,
    val householdId: String,
    val status: String = "PENDING" // PENDING, ACCEPTED, DECLINED
)

enum class TransactionType {
    INCOME, EXPENSE
}

data class CategoryWithItems(
    @Embedded val category: BudgetCategory = BudgetCategory(),
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val items: List<EnvelopeItem> = emptyList()
)

data class AppVersionInfo(
    val latestVersion: String = "1.0",
    val updateUrl: String = "",
    val isMandatory: Boolean = false
)
