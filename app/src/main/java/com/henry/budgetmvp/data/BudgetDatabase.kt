package com.henry.budgetmvp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM user_profile_table WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfile?

    @Upsert
    suspend fun upsertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM multi_income_table WHERE householdId = :householdId AND monthYear = :monthYear ORDER BY id ASC")
    fun getAllIncomeStreams(householdId: String, monthYear: String): Flow<List<IncomeStream>>

    @Query("SELECT * FROM multi_income_table WHERE householdId = :householdId AND monthYear = :monthYear")
    suspend fun getIncomeStreamsSync(householdId: String, monthYear: String): List<IncomeStream>

    @Upsert
    suspend fun upsertIncomeStream(stream: IncomeStream)

    @Delete
    suspend fun deleteIncomeStream(stream: IncomeStream)

    @Transaction
    @Query("SELECT * FROM budget_category_table WHERE householdId = :householdId AND monthYear = :monthYear ORDER BY name ASC")
    fun getAllCategoriesWithItems(householdId: String, monthYear: String): Flow<List<CategoryWithItems>>

    @Query("SELECT * FROM budget_category_table WHERE householdId = :householdId AND monthYear = :monthYear")
    suspend fun getCategoriesSync(householdId: String, monthYear: String): List<BudgetCategory>

    @Query("SELECT * FROM envelope_item_table WHERE householdId = :householdId AND monthYear = :monthYear")
    suspend fun getEnvelopeItemsSync(householdId: String, monthYear: String): List<EnvelopeItem>

    @Upsert
    suspend fun upsertCategory(category: BudgetCategory)

    @Delete
    suspend fun deleteCategory(category: BudgetCategory)

    @Upsert
    suspend fun upsertEnvelopeItem(item: EnvelopeItem)

    @Delete
    suspend fun deleteEnvelopeItem(item: EnvelopeItem)

    @Query("SELECT * FROM transaction_table WHERE householdId = :householdId ORDER BY date DESC")
    fun getAllTransactions(householdId: String): Flow<List<BudgetTransaction>>

    @Upsert
    suspend fun upsertTransaction(transaction: BudgetTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: BudgetTransaction)

    @Query("DELETE FROM multi_income_table WHERE householdId = :householdId")
    suspend fun deleteIncomeStreamsForHousehold(householdId: String)

    @Query("DELETE FROM budget_category_table WHERE householdId = :householdId")
    suspend fun deleteCategoriesForHousehold(householdId: String)

    @Query("DELETE FROM envelope_item_table WHERE householdId = :householdId")
    suspend fun deleteItemsForHousehold(householdId: String)

    @Query("DELETE FROM transaction_table WHERE householdId = :householdId")
    suspend fun deleteTransactionsForHousehold(householdId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM multi_income_table WHERE householdId = :householdId LIMIT 1) OR EXISTS(SELECT 1 FROM budget_category_table WHERE householdId = :householdId LIMIT 1)")
    fun hasAnyBudgetData(householdId: String): Flow<Boolean>

    @Transaction
    suspend fun clearHouseholdData(householdId: String) {
        deleteTransactionsForHousehold(householdId)
        deleteItemsForHousehold(householdId)
        deleteCategoriesForHousehold(householdId)
        deleteIncomeStreamsForHousehold(householdId)
    }

    @Transaction
    suspend fun syncAllData(
        income: List<IncomeStream>,
        categories: List<BudgetCategory>,
        items: List<EnvelopeItem>,
        transactions: List<BudgetTransaction>
    ) {
        // 1. Insert Income Streams
        income.forEach { upsertIncomeStream(it) }

        // 2. Insert Categories First
        categories.forEach { upsertCategory(it) }
        val validCategoryIds = categories.map { it.id }.toSet()

        // 3. Filter Items to only those with valid Category parents
        val validItems = items.filter { it.categoryId.isNotBlank() && validCategoryIds.contains(it.categoryId) }
        validItems.forEach { upsertEnvelopeItem(it) }
        val validItemIds = validItems.map { it.id }.toSet()

        // 4. Sanitize Transactions so missing item references become null
        val sanitizedTransactions = transactions.map { tx ->
            if (tx.itemId != null && !validItemIds.contains(tx.itemId)) {
                tx.copy(itemId = null)
            } else {
                tx
            }
        }
        sanitizedTransactions.forEach { upsertTransaction(it) }
    }
}

@Database(
    entities = [IncomeStream::class, BudgetCategory::class, EnvelopeItem::class, BudgetTransaction::class, UserProfile::class],
    version = 24,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
}
