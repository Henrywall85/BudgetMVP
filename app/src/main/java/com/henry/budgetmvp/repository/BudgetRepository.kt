package com.henry.budgetmvp.repository

import android.content.Context
import com.henry.budgetmvp.data.*
import com.henry.budgetmvp.util.NetworkChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class BudgetRepository @Inject constructor(
    private val context: Context,
    private val dao: BudgetDao,
    private val firestore: FirestoreSyncManager,
) {
    // --- User Profile ---
    suspend fun getUserProfile(userId: String): UserProfile? {
        return dao.getUserProfile(userId) ?: firestore.getUserProfile(userId)
    }

    suspend fun upsertUserProfile(profile: UserProfile): SyncResult {
        return try {
            dao.upsertUserProfile(profile)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.saveUserProfile(profile)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    // --- Household & Invites ---
    suspend fun getHouseholdMembers(householdId: String): List<UserProfile> {
        return firestore.getHouseholdMembers(householdId)
    }

    suspend fun sendInvite(invite: HouseholdInvite) {
        firestore.sendInvite(invite)
    }

    suspend fun getPendingInvitesForEmail(email: String): List<HouseholdInvite> {
        return firestore.getPendingInvitesForEmail(email)
    }

    suspend fun updateInviteStatus(inviteId: String, status: String) {
        firestore.updateInviteStatus(inviteId, status)
    }

    // --- Sync Operations ---
    fun listenToRealtimeData(householdId: String, userId: String): Flow<Map<String, List<Any>>> {
        return firestore.listenToRealtimeData(householdId, userId)
    }

    suspend fun fetchAllDataFromCloud(householdId: String, userId: String): Map<String, List<Any>> {
        return withTimeout(15.seconds) {
            firestore.fetchAllData(householdId, userId)
        }
    }

    suspend fun syncAllLocalData(
        income: List<IncomeStream>,
        categories: List<BudgetCategory>,
        items: List<EnvelopeItem>,
        transactions: List<BudgetTransaction>
    ) {
        dao.syncAllData(income, categories, items, transactions)
    }

    suspend fun replaceLocalDataWithCloudData(
        householdId: String,
        income: List<IncomeStream>,
        categories: List<BudgetCategory>,
        items: List<EnvelopeItem>,
        transactions: List<BudgetTransaction>
    ) {
        dao.replaceLocalDataWithCloudData(householdId, income, categories, items, transactions)
    }

    suspend fun pushLocalDataToCloud(householdId: String, userId: String, currentMonth: String) {
        if (!NetworkChecker.isNetworkAvailable(context)) return

        try {
            val localIncome = dao.getAllLocalIncomeStreamsRaw()
            val localCategories = dao.getAllLocalCategoriesRaw()
            val localItems = dao.getAllLocalEnvelopeItemsRaw()
            val localTransactions = dao.getAllLocalTransactionsRaw()

            localIncome.forEach { stream ->
                val updated = stream.copy(
                    householdId = stream.householdId.ifEmpty { householdId },
                    userId = stream.userId.ifEmpty { userId },
                    monthYear = stream.monthYear.ifEmpty { currentMonth }
                )
                dao.upsertIncomeStream(updated)
                try { firestore.saveIncomeStream(updated) } catch (ignored: Exception) {}
            }

            localCategories.forEach { cat ->
                val updated = cat.copy(
                    householdId = cat.householdId.ifEmpty { householdId },
                    userId = cat.userId.ifEmpty { userId },
                    monthYear = cat.monthYear.ifEmpty { currentMonth }
                )
                dao.upsertCategory(updated)
                try { firestore.saveCategory(updated) } catch (ignored: Exception) {}
            }

            localItems.forEach { item ->
                val updated = item.copy(
                    householdId = item.householdId.ifEmpty { householdId },
                    userId = item.userId.ifEmpty { userId },
                    monthYear = item.monthYear.ifEmpty { currentMonth }
                )
                dao.upsertEnvelopeItem(updated)
                try { firestore.saveEnvelopeItem(updated) } catch (ignored: Exception) {}
            }

            localTransactions.forEach { tx ->
                val updated = tx.copy(
                    householdId = tx.householdId.ifEmpty { householdId },
                    userId = tx.userId.ifEmpty { userId }
                )
                dao.upsertTransaction(updated)
                try { firestore.saveTransaction(updated) } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- CRUD: Income Streams ---
    fun getAllIncomeStreams(householdId: String, monthYear: String): Flow<List<IncomeStream>> {
        return dao.getAllIncomeStreams(householdId, monthYear)
    }

    suspend fun saveIncomeStream(stream: IncomeStream): SyncResult {
        return try {
            dao.upsertIncomeStream(stream)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.saveIncomeStream(stream)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    suspend fun deleteIncomeStream(stream: IncomeStream): SyncResult {
        return try {
            dao.deleteIncomeStream(stream)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.deleteIncomeStream(stream.id)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    // --- CRUD: Categories & Items ---
    fun getAllCategoriesWithItems(householdId: String, monthYear: String): Flow<List<CategoryWithItems>> {
        return dao.getAllCategoriesWithItems(householdId, monthYear)
    }

    suspend fun saveCategory(category: BudgetCategory): SyncResult {
        return try {
            dao.upsertCategory(category)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.saveCategory(category)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    suspend fun deleteCategory(category: BudgetCategory): SyncResult {
        return try {
            dao.deleteCategory(category)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.deleteCategory(category.id)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    suspend fun saveEnvelopeItem(item: EnvelopeItem): SyncResult {
        return try {
            dao.upsertEnvelopeItem(item)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.saveEnvelopeItem(item)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    suspend fun deleteEnvelopeItem(item: EnvelopeItem): SyncResult {
        return try {
            dao.deleteEnvelopeItem(item)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.deleteEnvelopeItem(item.id)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    // --- CRUD: Transactions ---
    fun getAllTransactions(householdId: String): Flow<List<BudgetTransaction>> {
        return dao.getAllTransactions(householdId)
    }

    suspend fun saveTransaction(transaction: BudgetTransaction): SyncResult {
        return try {
            dao.upsertTransaction(transaction)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.saveTransaction(transaction)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    suspend fun deleteTransaction(transaction: BudgetTransaction): SyncResult {
        return try {
            dao.deleteTransaction(transaction)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.deleteTransaction(transaction.id)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    // --- Global Operations ---
    fun hasAnyBudgetData(householdId: String): Flow<Boolean> {
        return dao.hasAnyBudgetData(householdId)
    }

    suspend fun clearHouseholdData(householdId: String, userId: String = ""): SyncResult {
        return try {
            dao.clearHouseholdData(householdId)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.clearHouseholdData(householdId, userId)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    suspend fun clearAllTransactionsForHousehold(householdId: String, userId: String = ""): SyncResult {
        return try {
            dao.deleteTransactionsForHousehold(householdId)
            if (!NetworkChecker.isNetworkAvailable(context)) return SyncResult.LocalOnly
            try {
                firestore.clearAllTransactionsForHousehold(householdId, userId)
                SyncResult.Synced
            } catch (ignored: Exception) {
                SyncResult.LocalOnly
            }
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Local database error")
        }
    }

    suspend fun copyBudget(householdId: String, fromMonth: String, toMonth: String): SyncResult {
        return try {
            val income = dao.getIncomeStreamsSync(householdId, fromMonth)
            val categories = dao.getCategoriesSync(householdId, fromMonth)
            val items = dao.getEnvelopeItemsSync(householdId, fromMonth)

            income.forEach { stream ->
                val newStream = stream.copy(id = java.util.UUID.randomUUID().toString(), monthYear = toMonth)
                saveIncomeStream(newStream)
            }

            categories.forEach { category ->
                val newCategoryId = java.util.UUID.randomUUID().toString()
                val newCategory = category.copy(id = newCategoryId, monthYear = toMonth)
                saveCategory(newCategory)

                items.filter { it.categoryId == category.id }.forEach { item ->
                    val newItem = item.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        categoryId = newCategoryId,
                        monthYear = toMonth
                    )
                    saveEnvelopeItem(newItem)
                }
            }
            SyncResult.Synced
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Failed to copy budget")
        }
    }
}
