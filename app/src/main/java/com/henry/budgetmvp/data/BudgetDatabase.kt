package com.henry.budgetmvp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM multi_income_table ORDER BY id ASC")
    fun getAllIncomeStreams(): Flow<List<IncomeStream>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncomeStream(stream: IncomeStream)

    @Delete
    suspend fun deleteIncomeStream(stream: IncomeStream)

    @Query("SELECT * FROM expense_envelope_table ORDER BY name ASC")
    fun getAllEnvelopes(): Flow<List<ExpenseEnvelope>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEnvelope(envelope: ExpenseEnvelope)

    @Delete
    suspend fun deleteEnvelope(envelope: ExpenseEnvelope)
}

@Database(
    entities = [IncomeStream::class, ExpenseEnvelope::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
}
