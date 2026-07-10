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
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private val _householdMembers = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _pendingInvites = MutableStateFlow<List<HouseholdInvite>>(emptyList())
    private val _updateInfo = MutableStateFlow<AppVersionInfo?>(null)
    
    val householdId: StateFlow<String?> = _householdId
    val isSyncing: StateFlow<Boolean> = _isSyncing
    val userProfile: StateFlow<UserProfile?> = _userProfile
    val householdMembers: StateFlow<List<UserProfile>> = _householdMembers
    val pendingInvites: StateFlow<List<HouseholdInvite>> = _pendingInvites
    val updateInfo: StateFlow<AppVersionInfo?> = _updateInfo

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

    fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch {
            val latestInfo = firestore.getLatestVersionInfo()
            if (latestInfo != null && latestInfo.latestVersion != currentVersion) {
                _updateInfo.value = latestInfo
            }
        }
    }

    fun dismissUpdatePopup() {
        _updateInfo.value = null
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

    private fun refreshPendingInvites(email: String) {
        if (email.isBlank()) return
        val cleanEmail = email.lowercase().trim()
        viewModelScope.launch {
            try {
                val invites = firestore.getPendingInvitesForEmail(cleanEmail)
                _pendingInvites.value = invites
            } catch (e: Exception) {
                e.printStackTrace()
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
        refreshPendingInvites(email)
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
