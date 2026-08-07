package com.henry.budgetmvp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM user_profile_table WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM multi_income_table WHERE householdId = :householdId AND monthYear = :monthYear ORDER BY id ASC")
    fun getAllIncomeStreams(householdId: String, monthYear: String): Flow<List<IncomeStream>>

    @Query("SELECT * FROM multi_income_table WHERE householdId = :householdId AND monthYear = :monthYear")
    suspend fun getIncomeStreamsSync(householdId: String, monthYear: String): List<IncomeStream>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: BudgetCategory)

    @Delete
    suspend fun deleteCategory(category: BudgetCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEnvelopeItem(item: EnvelopeItem)

    @Delete
    suspend fun deleteEnvelopeItem(item: EnvelopeItem)

    @Query("SELECT * FROM transaction_table WHERE householdId = :householdId ORDER BY date DESC")
    fun getAllTransactions(householdId: String): Flow<List<BudgetTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
        income.forEach { upsertIncomeStream(it) }
        categories.forEach { upsertCategory(it) }
        items.forEach { upsertEnvelopeItem(it) }
        transactions.forEach { upsertTransaction(it) }
    }
}

@Database(
    entities = [IncomeStream::class, BudgetCategory::class, EnvelopeItem::class, BudgetTransaction::class, UserProfile::class],
    version = 20,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
}
