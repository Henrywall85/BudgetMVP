package com.henry.budgetmvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henry.budgetmvp.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BudgetViewModel(
    private val dao: BudgetDao,
    private val firestore: FirestoreSyncManager = FirestoreSyncManager()
) : ViewModel() {
    private val _userId = MutableStateFlow<String?>(null)
    private val _householdId = MutableStateFlow<String?>(null)
    private val _isSyncing = MutableStateFlow(false)
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private val _householdMembers = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _pendingInvites = MutableStateFlow<List<HouseholdInvite>>(emptyList())
    private val _statusMessage = MutableSharedFlow<String>()
    
    private val _selectedMonthYear = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
    val selectedMonthYear: StateFlow<String> = _selectedMonthYear

    val householdId: StateFlow<String?> = _householdId
    val isSyncing: StateFlow<Boolean> = _isSyncing
    val userProfile: StateFlow<UserProfile?> = _userProfile
    val householdMembers: StateFlow<List<UserProfile>> = _householdMembers
    val pendingInvites: StateFlow<List<HouseholdInvite>> = _pendingInvites
    val statusMessage: SharedFlow<String> = _statusMessage

    fun setUserId(id: String?, email: String? = null) {
        _userId.value = id
        if (id != null) {
            loadUserProfile(id, email)
        } else {
            _householdId.value = null
            _userProfile.value = null
            _householdMembers.value = emptyList()
            _pendingInvites.value = emptyList()
        }
    }

    private fun loadUserProfile(userId: String, email: String?) {
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
                    profile = UserProfile(userId = userId, email = email ?: "", householdId = userId)
                    firestore.saveUserProfile(profile)
                } else if (email != null && profile.email != email) {
                    // Update email if it changed or was missing
                    profile = profile.copy(email = email)
                    firestore.saveUserProfile(profile)
                }

                // 4. Save to Local & Set State
                dao.upsertUserProfile(profile)
                _userProfile.value = profile
                _householdId.value = profile.householdId
                
                // 5. Load Members & Invites
                refreshHouseholdData(profile.householdId)
                refreshPendingInvites(profile.email)
                
                // 6. Trigger Sync
                syncFromCloud(profile.householdId)
            } catch (e: Exception) {
                e.printStackTrace()
                _householdId.value = userId
                syncFromCloud(userId)
            }
        }
    }

    private fun refreshHouseholdData(householdId: String) {
        viewModelScope.launch {
            try {
                val members = firestore.getHouseholdMembers(householdId)
                _householdMembers.value = members
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun refreshPendingInvites(email: String, showStatus: Boolean = false) {
        if (email.isBlank()) return
        val cleanEmail = email.lowercase().trim()
        viewModelScope.launch {
            try {
                val invites = firestore.getPendingInvitesForEmail(cleanEmail)
                _pendingInvites.value = invites
                if (showStatus) {
                    if (invites.isEmpty()) {
                        _statusMessage.emit("No new invites for $cleanEmail")
                    } else {
                        _statusMessage.emit("Found ${invites.size} invite(s)")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (showStatus) {
                    _statusMessage.emit("Error checking invites: ${e.localizedMessage}")
                }
            }
        }
    }

    fun inviteMember(toEmail: String) {
        val fromProfile = _userProfile.value ?: return
        val hid = _householdId.value ?: return
        
        viewModelScope.launch {
            try {
                val invite = HouseholdInvite(
                    fromEmail = fromProfile.email.lowercase().trim(),
                    fromUserId = fromProfile.userId,
                    toEmail = toEmail.lowercase().trim(),
                    householdId = hid
                )
                firestore.sendInvite(invite)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshInvites() {
        val email = _userProfile.value?.email ?: return
        refreshPendingInvites(email, showStatus = true)
    }

    fun acceptInvite(invite: HouseholdInvite) {
        val uid = _userId.value ?: return
        viewModelScope.launch {
            try {
                // Update profile with new householdId
                val currentProfile = _userProfile.value ?: return@launch
                val updatedProfile = currentProfile.copy(householdId = invite.householdId)
                
                firestore.saveUserProfile(updatedProfile)
                dao.upsertUserProfile(updatedProfile)
                
                // Update invite status
                firestore.updateInviteStatus(invite.id, "ACCEPTED")
                
                // Update local state
                _householdId.value = invite.householdId
                _userProfile.value = updatedProfile
                
                // Cleanup: Refresh data
                refreshHouseholdData(invite.householdId)
                refreshPendingInvites(updatedProfile.email)
                syncFromCloud(invite.householdId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun declineInvite(invite: HouseholdInvite) {
        viewModelScope.launch {
            try {
                firestore.updateInviteStatus(invite.id, "DECLINED")
                val email = _userProfile.value?.email ?: ""
                refreshPendingInvites(email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun syncFromCloud(householdId: String) {
        val uid = _userId.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val data = firestore.fetchAllData(householdId, uid)
                
                val incomeList = (data["income"] as? List<IncomeStream>)?.map { stream ->
                    val updated = if (stream.householdId.isEmpty()) stream.copy(householdId = householdId) else stream
                    if (stream.householdId.isEmpty()) {
                        try { firestore.saveIncomeStream(updated) } catch (e: Exception) {}
                    }
                    updated
                } ?: emptyList()
                
                val categoriesList = (data["categories"] as? List<BudgetCategory>)?.map { cat ->
                    val updated = if (cat.householdId.isEmpty()) cat.copy(householdId = householdId) else cat
                    if (cat.householdId.isEmpty()) {
                        try { firestore.saveCategory(updated) } catch (e: Exception) {}
                    }
                    updated
                } ?: emptyList()
                
                val itemsList = (data["items"] as? List<EnvelopeItem>)?.map { item ->
                    val updated = if (item.householdId.isEmpty()) item.copy(householdId = householdId) else item
                    if (item.householdId.isEmpty()) {
                        try { firestore.saveEnvelopeItem(updated) } catch (e: Exception) {}
                    }
                    updated
                } ?: emptyList()
                
                val transactionsList = (data["transactions"] as? List<BudgetTransaction>)?.map { tx ->
                    val updated = if (tx.householdId.isEmpty()) tx.copy(householdId = householdId) else tx
                    if (tx.householdId.isEmpty()) {
                        try { firestore.saveTransaction(updated) } catch (e: Exception) {}
                    }
                    updated
                } ?: emptyList()

                // Perform all database updates in a single transaction
                dao.syncAllData(incomeList, categoriesList, itemsList, transactionsList)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun setMonthYear(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomeStreams: Flow<List<IncomeStream>> = _householdId.combine(_selectedMonthYear) { hid, monthYear ->
        hid to monthYear
    }.flatMapLatest { (hid, monthYear) ->
        if (hid == null) flowOf(emptyList()) else dao.getAllIncomeStreams(hid, monthYear)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoriesWithItems: Flow<List<CategoryWithItems>> = _householdId.combine(_selectedMonthYear) { hid, monthYear ->
        hid to monthYear
    }.flatMapLatest { (hid, monthYear) ->
        if (hid == null) flowOf(emptyList()) else dao.getAllCategoriesWithItems(hid, monthYear)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<List<BudgetTransaction>> = _householdId.flatMapLatest { hid ->
        if (hid == null) flowOf(emptyList()) else dao.getAllTransactions(hid)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val hasAnyBudgetData: Flow<Boolean> = _householdId.flatMapLatest { hid ->
        if (hid == null) flowOf(false) else dao.hasAnyBudgetData(hid)
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
        val currentMonth = _selectedMonthYear.value
        val streamWithHid = stream.copy(
            householdId = if (stream.householdId.isEmpty()) hid else stream.householdId,
            monthYear = if (stream.monthYear.isEmpty()) currentMonth else stream.monthYear
        )
        viewModelScope.launch { 
            _isSyncing.value = true
            dao.upsertIncomeStream(streamWithHid)
            try { 
                firestore.saveIncomeStream(streamWithHid) 
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { 
            _isSyncing.value = true
            dao.deleteIncomeStream(stream)
            try { 
                firestore.deleteIncomeStream(stream.id) 
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun saveCategory(category: BudgetCategory) {
        val hid = _householdId.value ?: _userId.value ?: return
        val currentMonth = _selectedMonthYear.value
        val catWithHid = category.copy(
            householdId = if (category.householdId.isEmpty()) hid else category.householdId,
            monthYear = if (category.monthYear.isEmpty()) currentMonth else category.monthYear
        )
        viewModelScope.launch { 
            _isSyncing.value = true
            dao.upsertCategory(catWithHid)
            try { 
                firestore.saveCategory(catWithHid) 
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteCategory(category: BudgetCategory) {
        viewModelScope.launch { 
            _isSyncing.value = true
            dao.deleteCategory(category)
            try { 
                firestore.deleteCategory(category.id) 
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun saveEnvelopeItem(item: EnvelopeItem) {
        val hid = _householdId.value ?: _userId.value ?: return
        val currentMonth = _selectedMonthYear.value
        val itemWithHid = item.copy(
            householdId = if (item.householdId.isEmpty()) hid else item.householdId,
            monthYear = if (item.monthYear.isEmpty()) currentMonth else item.monthYear
        )
        viewModelScope.launch { 
            _isSyncing.value = true
            dao.upsertEnvelopeItem(itemWithHid)
            try { 
                firestore.saveEnvelopeItem(itemWithHid) 
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteEnvelopeItem(item: EnvelopeItem) {
        viewModelScope.launch { 
            _isSyncing.value = true
            dao.deleteEnvelopeItem(item)
            try { 
                firestore.deleteEnvelopeItem(item.id) 
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun saveTransaction(transaction: BudgetTransaction) {
        val hid = _householdId.value ?: _userId.value ?: return
        val transWithHid = if (transaction.householdId.isEmpty()) transaction.copy(householdId = hid) else transaction
        viewModelScope.launch { 
            _isSyncing.value = true
            dao.upsertTransaction(transWithHid)
            try { 
                firestore.saveTransaction(transWithHid) 
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteTransaction(transaction: BudgetTransaction) {
        viewModelScope.launch { 
            _isSyncing.value = true
            dao.deleteTransaction(transaction)
            try { 
                firestore.deleteTransaction(transaction.id) 
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun resetAllHouseholdData() {
        val hid = _householdId.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                firestore.clearHouseholdData(hid)
                dao.clearHouseholdData(hid)
                _statusMessage.emit("Budget data reset successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.emit("Failed to reset data: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun copyBudget(fromMonth: String, toMonth: String) {
        val hid = _householdId.value ?: return
        val uid = _userId.value ?: ""
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val income = dao.getIncomeStreamsSync(hid, fromMonth)
                val categories = dao.getCategoriesSync(hid, fromMonth)
                val items = dao.getEnvelopeItemsSync(hid, fromMonth)

                // Copy Income
                income.forEach { stream ->
                    val newStream = stream.copy(id = java.util.UUID.randomUUID().toString(), monthYear = toMonth)
                    dao.upsertIncomeStream(newStream)
                    firestore.saveIncomeStream(newStream)
                }

                // Copy Categories and their Items
                categories.forEach { category ->
                    val newCategoryId = java.util.UUID.randomUUID().toString()
                    val newCategory = category.copy(id = newCategoryId, monthYear = toMonth)
                    dao.upsertCategory(newCategory)
                    firestore.saveCategory(newCategory)

                    items.filter { it.categoryId == category.id }.forEach { item ->
                        val newItem = item.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            categoryId = newCategoryId,
                            monthYear = toMonth
                        )
                        dao.upsertEnvelopeItem(newItem)
                        firestore.saveEnvelopeItem(newItem)
                    }
                }
                _statusMessage.emit("Budget created for ${toMonth}")
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.emit("Failed to create budget")
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
