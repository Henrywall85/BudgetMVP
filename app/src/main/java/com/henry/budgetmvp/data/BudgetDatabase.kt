package com.henry.budgetmvp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM multi_income_table WHERE userId = :userId ORDER BY id ASC")
    fun getAllIncomeStreams(userId: String): Flow<List<IncomeStream>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncomeStream(stream: IncomeStream)

    @Delete
    suspend fun deleteIncomeStream(stream: IncomeStream)

    @Transaction
    @Query("SELECT * FROM budget_category_table WHERE userId = :userId ORDER BY name ASC")
    fun getAllCategoriesWithItems(userId: String): Flow<List<CategoryWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: BudgetCategory)

    @Delete
    suspend fun deleteCategory(category: BudgetCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEnvelopeItem(item: EnvelopeItem)

    @Delete
    suspend fun deleteEnvelopeItem(item: EnvelopeItem)

    @Query("SELECT * FROM transaction_table WHERE userId = :userId ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<BudgetTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: BudgetTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: BudgetTransaction)
}

@Database(
    entities = [IncomeStream::class, BudgetCategory::class, EnvelopeItem::class, BudgetTransaction::class],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
}
