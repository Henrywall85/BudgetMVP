package com.henry.budgetmvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henry.budgetmvp.data.*
import com.henry.budgetmvp.repository.BudgetRepository
import com.henry.budgetmvp.util.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {
    private val _userId = MutableStateFlow<String?>(null)
    private val _householdId = MutableStateFlow<String?>(null)
    private val _isSyncing = MutableStateFlow(value = false)
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private val _householdMembers = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _pendingInvites = MutableStateFlow<List<HouseholdInvite>>(emptyList())
    private val _statusMessage = MutableSharedFlow<StatusMessage>()
    
    private val _selectedMonthYear = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
    // Expose as public val if needed for external observation, currently unused
    // val selectedMonthYear: StateFlow<String> = _selectedMonthYear

    // Expose as public val if needed for external observation, currently unused
    // val householdId: StateFlow<String?> = _householdId
    val isSyncing: StateFlow<Boolean> = _isSyncing
    // val userProfile: StateFlow<UserProfile?> = _userProfile
    val householdMembers: StateFlow<List<UserProfile>> = _householdMembers
    val pendingInvites: StateFlow<List<HouseholdInvite>> = _pendingInvites
    val statusMessage: SharedFlow<StatusMessage> = _statusMessage

    init {
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        connectivityObserver.isOnline
            .drop(1) // Skip initial state to avoid redundant messages on boot
            .onEach { isOnline ->
                if (isOnline) {
                    _statusMessage.emit(StatusMessage("Back online — syncing data with cloud...", MessageType.SUCCESS))
                } else {
                    _statusMessage.emit(StatusMessage("You are offline. Changes will be saved locally.", MessageType.OFFLINE))
                }
            }
            .launchIn(viewModelScope)
    }

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
                var profile = repository.getUserProfile(userId)

                if (profile == null) {
                    profile = UserProfile(userId = userId, email = email ?: "", householdId = userId)
                    handleSyncResult(repository.upsertUserProfile(profile))
                } else if ((email != null) && (profile.email != email)) {
                    profile = profile.copy(email = email)
                    handleSyncResult(repository.upsertUserProfile(profile))
                }

                _userProfile.value = profile
                _householdId.value = profile.householdId
                
                refreshHouseholdData(profile.householdId)
                refreshPendingInvites(profile.email)
                syncFromCloud(profile.householdId)
            } catch (e: Exception) {
                e.printStackTrace()
                _householdId.value = userId
                _statusMessage.emit(StatusMessage("Profile load error. Working offline.", MessageType.ERROR))
                syncFromCloud(userId)
            }
        }
    }

    private fun refreshHouseholdData(householdId: String) {
        viewModelScope.launch {
            try {
                val members = repository.getHouseholdMembers(householdId)
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
                val invites = repository.getPendingInvitesForEmail(cleanEmail)
                _pendingInvites.value = invites
                if (showStatus) {
                    if (invites.isEmpty()) {
                        _statusMessage.emit(StatusMessage("No new invites for $cleanEmail", MessageType.INFO))
                    } else {
                        _statusMessage.emit(StatusMessage("Found ${invites.size} invite(s)", MessageType.INFO))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (showStatus) {
                    _statusMessage.emit(StatusMessage("Error checking invites: ${e.localizedMessage}", MessageType.ERROR))
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
                repository.sendInvite(invite)
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
                val currentProfile = _userProfile.value ?: return@launch
                val updatedProfile = currentProfile.copy(householdId = invite.householdId)
                
                repository.upsertUserProfile(updatedProfile)
                repository.updateInviteStatus(invite.id, "ACCEPTED")
                
                _householdId.value = invite.householdId
                _userProfile.value = updatedProfile
                
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
                repository.updateInviteStatus(invite.id, "DECLINED")
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
                val data = repository.fetchAllDataFromCloud(householdId, uid)
                
                @Suppress("UNCHECKED_CAST")
                val incomeList = (data["income"] as? List<IncomeStream>)?.map { stream ->
                    val updated = if (stream.householdId.isEmpty()) stream.copy(householdId = householdId) else stream
                    if (stream.householdId.isEmpty()) {
                        repository.saveIncomeStream(updated)
                    }
                    updated
                } ?: emptyList()
                
                @Suppress("UNCHECKED_CAST")
                val categoriesList = (data["categories"] as? List<BudgetCategory>)?.map { cat ->
                    val updated = if (cat.householdId.isEmpty()) cat.copy(householdId = householdId) else cat
                    if (cat.householdId.isEmpty()) {
                        repository.saveCategory(updated)
                    }
                    updated
                } ?: emptyList()
                
                @Suppress("UNCHECKED_CAST")
                val itemsList = (data["items"] as? List<EnvelopeItem>)?.map { item ->
                    val updated = if (item.householdId.isEmpty()) item.copy(householdId = householdId) else item
                    if (item.householdId.isEmpty()) {
                        repository.saveEnvelopeItem(updated)
                    }
                    updated
                } ?: emptyList()
                
                @Suppress("UNCHECKED_CAST")
                val transactionsList = (data["transactions"] as? List<BudgetTransaction>)?.map { tx ->
                    val updated = if (tx.householdId.isEmpty()) tx.copy(householdId = householdId) else tx
                    if (tx.householdId.isEmpty()) {
                        repository.saveTransaction(updated)
                    }
                    updated
                } ?: emptyList()

                repository.syncAllLocalData(incomeList, categoriesList, itemsList, transactionsList)
            } catch (e: TimeoutCancellationException) {
                e.printStackTrace()
                _statusMessage.emit(StatusMessage("Cloud sync timed out. Working in offline mode.", MessageType.OFFLINE))
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.emit(StatusMessage("Cloud sync failed. Working in offline mode.", MessageType.OFFLINE))
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
        if (hid == null || monthYear == null) flowOf(emptyList()) 
        else repository.getAllIncomeStreams(hid, monthYear)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoriesWithItems: Flow<List<CategoryWithItems>> = _householdId.combine(_selectedMonthYear) { hid, monthYear ->
        hid to monthYear
    }.flatMapLatest { (hid, monthYear) ->
        if (hid == null || monthYear == null) flowOf(emptyList()) 
        else repository.getAllCategoriesWithItems(hid, monthYear)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<List<BudgetTransaction>> = _householdId.flatMapLatest { hid ->
        if (hid == null) flowOf(emptyList()) else repository.getAllTransactions(hid)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val hasAnyBudgetData: Flow<Boolean> = _householdId.flatMapLatest { hid ->
        if (hid == null) flowOf(false) else repository.hasAnyBudgetData(hid)
    }

    fun leaveHousehold() {
        val uid = _userId.value ?: return
        viewModelScope.launch {
            try {
                val profile = UserProfile(userId = uid, email = "", householdId = uid)
                handleSyncResult(repository.upsertUserProfile(profile))
                _householdId.value = uid
                syncFromCloud(uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveIncomeStream(stream: IncomeStream) {
        val hid = _householdId.value ?: _userId.value ?: return
        val currentMonth = _selectedMonthYear.value ?: ""
        val streamWithHid = stream.copy(
            householdId = stream.householdId.ifEmpty { hid },
            monthYear = stream.monthYear.ifEmpty { currentMonth }
        )
        viewModelScope.launch { 
            _isSyncing.value = true
            try {
                handleSyncResult(repository.saveIncomeStream(streamWithHid))
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
            try {
                handleSyncResult(repository.deleteIncomeStream(stream))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun saveCategory(category: BudgetCategory) {
        val hid = _householdId.value ?: _userId.value ?: return
        val currentMonth = _selectedMonthYear.value ?: ""
        val catWithHid = category.copy(
            householdId = category.householdId.ifEmpty { hid },
            monthYear = category.monthYear.ifEmpty { currentMonth }
        )
        viewModelScope.launch { 
            _isSyncing.value = true
            try {
                handleSyncResult(repository.saveCategory(catWithHid))
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
            try {
                handleSyncResult(repository.deleteCategory(category))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun saveEnvelopeItem(item: EnvelopeItem) {
        val hid = _householdId.value ?: _userId.value ?: return
        val currentMonth = _selectedMonthYear.value ?: ""
        val itemWithHid = item.copy(
            householdId = item.householdId.ifEmpty { hid },
            monthYear = item.monthYear.ifEmpty { currentMonth }
        )
        viewModelScope.launch { 
            _isSyncing.value = true
            try {
                handleSyncResult(repository.saveEnvelopeItem(itemWithHid))
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
            try {
                handleSyncResult(repository.deleteEnvelopeItem(item))
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
            try {
                handleSyncResult(repository.saveTransaction(transWithHid))
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
            try {
                handleSyncResult(repository.deleteTransaction(transaction))
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
                val result = repository.clearHouseholdData(hid)
                handleSyncResult(result, successMessage = "Budget data reset successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.emit(StatusMessage("Failed to reset data: ${e.message}", MessageType.ERROR))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun handleSyncResult(result: SyncResult, successMessage: String? = null) {
        when (result) {
            is SyncResult.Synced -> {
                successMessage?.let { _statusMessage.emit(StatusMessage(it, MessageType.SUCCESS)) }
            }
            is SyncResult.LocalOnly -> {
                _statusMessage.emit(StatusMessage("Saved locally (Cloud sync unavailable)", MessageType.OFFLINE))
            }
            is SyncResult.Error -> {
                _statusMessage.emit(StatusMessage("Error: ${result.message}", MessageType.ERROR))
            }
        }
    }

    fun copyBudget(fromMonth: String, toMonth: String) {
        val hid = _householdId.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = repository.copyBudget(hid, fromMonth, toMonth)
                handleSyncResult(result, successMessage = "Budget created for $toMonth")
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.emit(StatusMessage("Failed to create budget", MessageType.ERROR))
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
