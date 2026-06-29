package com.henry.budgetmvp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM multi_income_table ORDER BY id ASC")
    fun getAllIncomeStreams(): Flow<List<IncomeStream>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncomeStream(stream: IncomeStream)

    @Delete
    suspend fun deleteIncomeStream(stream: IncomeStream)

    @Transaction
    @Query("SELECT * FROM budget_category_table ORDER BY name ASC")
    fun getAllCategoriesWithItems(): Flow<List<CategoryWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: BudgetCategory)

    @Delete
    suspend fun deleteCategory(category: BudgetCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEnvelopeItem(item: EnvelopeItem)

    @Delete
    suspend fun deleteEnvelopeItem(item: EnvelopeItem)
}

@Database(
    entities = [IncomeStream::class, BudgetCategory::class, EnvelopeItem::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
}
