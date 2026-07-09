package com.henry.budgetmvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henry.budgetmvp.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val dao: BudgetDao,
    private val firestore: FirestoreSyncManager = FirestoreSyncManager()
) : ViewModel() {
    private val _userId = MutableStateFlow<String?>(null)
    private val _householdId = MutableStateFlow<String?>(null)
    private val _isSyncing = MutableStateFlow(false)
    
    val householdId: StateFlow<String?> = _householdId
    val isSyncing: StateFlow<Boolean> = _isSyncing

    fun setUserId(id: String?) {
        _userId.value = id
        if (id != null) {
            loadUserProfile(id)
        } else {
            _householdId.value = null
        }
    }

    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                // 1. Try Local Room
                var profile = dao.getUserProfile(userId)
                
                // 2. Try Firestore if not local
                if (profile == null) {
                    profile = firestore.getUserProfile(userId)
                }

                // 3. Create new if still null (New User)
                if (profile == null) {
                    profile = UserProfile(userId = userId, email = "", householdId = userId)
                    firestore.saveUserProfile(profile)
                }

                // 4. Save to Local & Set State
                dao.upsertUserProfile(profile)
                _householdId.value = profile.householdId
                
                // 5. Trigger Sync
                syncFromCloud(profile.householdId)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to own ID if error
                _householdId.value = userId
                syncFromCloud(userId)
            }
        }
    }

    private fun syncFromCloud(householdId: String) {
        val uid = _userId.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val data = firestore.fetchAllData(householdId, uid)
                
                (data["income"] as? List<IncomeStream>)?.forEach { stream ->
                    val updated = if (stream.householdId.isEmpty()) stream.copy(householdId = householdId) else stream
                    dao.upsertIncomeStream(updated)
                    // If it was legacy, push the fix back to cloud
                    if (stream.householdId.isEmpty()) {
                        try { firestore.saveIncomeStream(updated) } catch (e: Exception) {}
                    }
                }
                
                (data["categories"] as? List<BudgetCategory>)?.forEach { cat ->
                    val updated = if (cat.householdId.isEmpty()) cat.copy(householdId = householdId) else cat
                    dao.upsertCategory(updated)
                    if (cat.householdId.isEmpty()) {
                        try { firestore.saveCategory(updated) } catch (e: Exception) {}
                    }
                }
                
                (data["items"] as? List<EnvelopeItem>)?.forEach { item ->
                    val updated = if (item.householdId.isEmpty()) item.copy(householdId = householdId) else item
                    dao.upsertEnvelopeItem(updated)
                    if (item.householdId.isEmpty()) {
                        try { firestore.saveEnvelopeItem(updated) } catch (e: Exception) {}
                    }
                }
                
                (data["transactions"] as? List<BudgetTransaction>)?.forEach { tx ->
                    val updated = if (tx.householdId.isEmpty()) tx.copy(householdId = householdId) else tx
                    dao.upsertTransaction(updated)
                    if (tx.householdId.isEmpty()) {
                        try { firestore.saveTransaction(updated) } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomeStreams: Flow<List<IncomeStream>> = _householdId.flatMapLatest { hid ->
        if (hid == null) flowOf(emptyList()) else dao.getAllIncomeStreams(hid)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoriesWithItems: Flow<List<CategoryWithItems>> = _householdId.flatMapLatest { hid ->
        if (hid == null) flowOf(emptyList()) else dao.getAllCategoriesWithItems(hid)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<List<BudgetTransaction>> = _householdId.flatMapLatest { hid ->
        if (hid == null) flowOf(emptyList()) else dao.getAllTransactions(hid)
    }

    fun joinHousehold(newHouseholdId: String) {
        val uid = _userId.value ?: return
        viewModelScope.launch {
            try {
                val profile = UserProfile(userId = uid, email = "", householdId = newHouseholdId)
                firestore.saveUserProfile(profile)
                dao.upsertUserProfile(profile)
                _householdId.value = newHouseholdId
                syncFromCloud(newHouseholdId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun leaveHousehold() {
        val uid = _userId.value ?: return
        viewModelScope.launch {
            try {
                val profile = UserProfile(userId = uid, email = "", householdId = uid)
                firestore.saveUserProfile(profile)
                dao.upsertUserProfile(profile)
                _householdId.value = uid
                syncFromCloud(uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveIncomeStream(stream: IncomeStream) {
        val hid = _householdId.value ?: _userId.value ?: return
        val streamWithHid = if (stream.householdId.isEmpty()) stream.copy(householdId = hid) else stream
        viewModelScope.launch { 
            dao.upsertIncomeStream(streamWithHid)
            try { firestore.saveIncomeStream(streamWithHid) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { 
            dao.deleteIncomeStream(stream)
            try { firestore.deleteIncomeStream(stream.id) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveCategory(category: BudgetCategory) {
        val hid = _householdId.value ?: _userId.value ?: return
        val catWithHid = if (category.householdId.isEmpty()) category.copy(householdId = hid) else category
        viewModelScope.launch { 
            dao.upsertCategory(catWithHid)
            try { firestore.saveCategory(catWithHid) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteCategory(category: BudgetCategory) {
        viewModelScope.launch { 
            dao.deleteCategory(category)
            try { firestore.deleteCategory(category.id) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveEnvelopeItem(item: EnvelopeItem) {
        val hid = _householdId.value ?: _userId.value ?: return
        val itemWithHid = if (item.householdId.isEmpty()) item.copy(householdId = hid) else item
        viewModelScope.launch { 
            dao.upsertEnvelopeItem(itemWithHid)
            try { firestore.saveEnvelopeItem(itemWithHid) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteEnvelopeItem(item: EnvelopeItem) {
        viewModelScope.launch { 
            dao.deleteEnvelopeItem(item)
            try { firestore.deleteEnvelopeItem(item.id) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveTransaction(transaction: BudgetTransaction) {
        val hid = _householdId.value ?: _userId.value ?: return
        val transWithHid = if (transaction.householdId.isEmpty()) transaction.copy(householdId = hid) else transaction
        viewModelScope.launch { 
            dao.upsertTransaction(transWithHid)
            try { firestore.saveTransaction(transWithHid) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteTransaction(transaction: BudgetTransaction) {
        viewModelScope.launch { 
            dao.deleteTransaction(transaction)
            try { firestore.deleteTransaction(transaction.id) } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
