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

    @Query("SELECT * FROM multi_income_table")
    suspend fun getAllLocalIncomeStreamsRaw(): List<IncomeStream>

    @Query("SELECT * FROM budget_category_table")
    suspend fun getAllLocalCategoriesRaw(): List<BudgetCategory>

    @Query("SELECT * FROM envelope_item_table")
    suspend fun getAllLocalEnvelopeItemsRaw(): List<EnvelopeItem>

    @Query("SELECT * FROM transaction_table")
    suspend fun getAllLocalTransactionsRaw(): List<BudgetTransaction>

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
    suspend fun replaceLocalDataWithCloudData(
        householdId: String,
        income: List<IncomeStream>,
        categories: List<BudgetCategory>,
        items: List<EnvelopeItem>,
        transactions: List<BudgetTransaction>
    ) {
        clearHouseholdData(householdId)
        syncAllData(income, categories, items, transactions)
    }

    @Transaction
    suspend fun syncAllData(
        income: List<IncomeStream>,
        categories: List<BudgetCategory>,
        items: List<EnvelopeItem>,
        transactions: List<BudgetTransaction>
    ) {
        // 1. Deduplicate Income Streams by (householdId, monthYear, sourceName)
        val deduplicatedIncome = income
            .groupBy { Triple(it.householdId, it.monthYear, it.sourceName.trim().lowercase()) }
            .map { (_, group) ->
                group.maxByOrNull { it.monthlyAmount } ?: group.first()
            }

        val duplicateIncomeIdsToDelete = income.map { it.id }.toSet() - deduplicatedIncome.map { it.id }.toSet()

        deduplicatedIncome.forEach { upsertIncomeStream(it) }
        income.filter { duplicateIncomeIdsToDelete.contains(it.id) }.forEach { deleteIncomeStream(it) }

        // 2. Deduplicate Categories by (householdId, monthYear, name)
        val categoryIdRemap = mutableMapOf<String, String>()
        val deduplicatedCategories = mutableListOf<BudgetCategory>()

        categories
            .groupBy { Triple(it.householdId, it.monthYear, it.name.trim().lowercase()) }
            .forEach { (_, group) ->
                val primaryCat = group.first()
                deduplicatedCategories.add(primaryCat)
                group.forEach { dupCat ->
                    if (dupCat.id != primaryCat.id) {
                        categoryIdRemap[dupCat.id] = primaryCat.id
                    }
                }
            }

        val duplicateCatIdsToDelete = categories.map { it.id }.toSet() - deduplicatedCategories.map { it.id }.toSet()

        deduplicatedCategories.forEach { upsertCategory(it) }
        categories.filter { duplicateCatIdsToDelete.contains(it.id) }.forEach { deleteCategory(it) }

        val validCategoryIds = deduplicatedCategories.map { it.id }.toSet()

        // 3. Remap EnvelopeItems categoryId to primaryCat.id, then deduplicate EnvelopeItems
        val remappedItems = items.map { item ->
            val remappedCatId = categoryIdRemap[item.categoryId] ?: item.categoryId
            item.copy(categoryId = remappedCatId)
        }.filter { it.categoryId.isNotBlank() && validCategoryIds.contains(it.categoryId) }

        val itemIdRemap = mutableMapOf<String, String>()
        val deduplicatedItems = mutableListOf<EnvelopeItem>()

        remappedItems
            .groupBy { Triple(it.categoryId, it.monthYear, it.name.trim().lowercase()) }
            .forEach { (_, group) ->
                val primaryItem = group.maxByOrNull { it.targetAmount } ?: group.first()
                deduplicatedItems.add(primaryItem)
                group.forEach { dupItem ->
                    if (dupItem.id != primaryItem.id) {
                        itemIdRemap[dupItem.id] = primaryItem.id
                    }
                }
            }

        val duplicateItemIdsToDelete = remappedItems.map { it.id }.toSet() - deduplicatedItems.map { it.id }.toSet()

        deduplicatedItems.forEach { upsertEnvelopeItem(it) }
        remappedItems.filter { duplicateItemIdsToDelete.contains(it.id) }.forEach { deleteEnvelopeItem(it) }

        val validItemIds = deduplicatedItems.map { it.id }.toSet()

        // 4. Remap Transactions to valid primary itemId and incomeStreamId
        val sanitizedTransactions = transactions.map { tx ->
            val remappedItemId = tx.itemId?.let { itemIdRemap[it] ?: it }
            val finalItemId = if (remappedItemId != null && validItemIds.contains(remappedItemId)) remappedItemId else null

            val remappedStreamId = tx.incomeStreamId?.let { streamId ->
                if (duplicateIncomeIdsToDelete.contains(streamId)) {
                    val dupStream = income.find { it.id == streamId }
                    if (dupStream != null) {
                        deduplicatedIncome.find { 
                            it.householdId == dupStream.householdId && 
                            it.monthYear == dupStream.monthYear && 
                            it.sourceName.equals(dupStream.sourceName, ignoreCase = true) 
                        }?.id
                    } else null
                } else streamId
            }

            tx.copy(itemId = finalItemId, incomeStreamId = remappedStreamId)
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
