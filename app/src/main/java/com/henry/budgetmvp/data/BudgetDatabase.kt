package com.henry.budgetmvp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM user_profile_table WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM multi_income_table WHERE householdId = :householdId ORDER BY id ASC")
    fun getAllIncomeStreams(householdId: String): Flow<List<IncomeStream>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncomeStream(stream: IncomeStream)

    @Delete
    suspend fun deleteIncomeStream(stream: IncomeStream)

    @Transaction
    @Query("SELECT * FROM budget_category_table WHERE householdId = :householdId ORDER BY name ASC")
    fun getAllCategoriesWithItems(householdId: String): Flow<List<CategoryWithItems>>

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

    @Query("SELECT * FROM paycheck_assignments WHERE householdId = :householdId")
    fun getAllPaycheckAssignments(householdId: String): Flow<List<PaycheckAssignment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaycheckAssignment(assignment: PaycheckAssignment)

    @Delete
    suspend fun deletePaycheckAssignment(assignment: PaycheckAssignment)

    @Query("DELETE FROM paycheck_assignments WHERE householdId = :householdId")
    suspend fun deleteAssignmentsForHousehold(householdId: String)

    @Query("DELETE FROM multi_income_table WHERE householdId = :householdId")
    suspend fun deleteIncomeStreamsForHousehold(householdId: String)

    @Query("DELETE FROM budget_category_table WHERE householdId = :householdId")
    suspend fun deleteCategoriesForHousehold(householdId: String)

    @Query("DELETE FROM envelope_item_table WHERE householdId = :householdId")
    suspend fun deleteItemsForHousehold(householdId: String)

    @Query("DELETE FROM transaction_table WHERE householdId = :householdId")
    suspend fun deleteTransactionsForHousehold(householdId: String)

    @Transaction
    suspend fun clearHouseholdData(householdId: String) {
        deleteAssignmentsForHousehold(householdId)
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
        transactions: List<BudgetTransaction>,
        assignments: List<PaycheckAssignment>
    ) {
        assignments.forEach { upsertPaycheckAssignment(it) }
        income.forEach { upsertIncomeStream(it) }
        categories.forEach { upsertCategory(it) }
        items.forEach { upsertEnvelopeItem(it) }
        transactions.forEach { upsertTransaction(it) }
    }
}

@Database(
    entities = [IncomeStream::class, BudgetCategory::class, EnvelopeItem::class, BudgetTransaction::class, UserProfile::class, PaycheckAssignment::class],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
}
