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
    
    fun setUserId(id: String?) {
        _userId.value = id
        if (id != null) {
            syncFromCloud(id)
        }
    }

    private fun syncFromCloud(userId: String) {
        viewModelScope.launch {
            try {
                val data = firestore.fetchAllData(userId)
                (data["income"] as? List<IncomeStream>)?.forEach { dao.upsertIncomeStream(it) }
                (data["categories"] as? List<BudgetCategory>)?.forEach { dao.upsertCategory(it) }
                (data["items"] as? List<EnvelopeItem>)?.forEach { dao.upsertEnvelopeItem(it) }
                (data["transactions"] as? List<BudgetTransaction>)?.forEach { dao.upsertTransaction(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomeStreams: Flow<List<IncomeStream>> = _userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.getAllIncomeStreams(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoriesWithItems: Flow<List<CategoryWithItems>> = _userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.getAllCategoriesWithItems(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<List<BudgetTransaction>> = _userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.getAllTransactions(id)
    }

    fun saveIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { 
            dao.upsertIncomeStream(stream)
            try { firestore.saveIncomeStream(stream) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { 
            dao.deleteIncomeStream(stream)
            try { firestore.deleteIncomeStream(stream.id) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveCategory(category: BudgetCategory) {
        viewModelScope.launch { 
            dao.upsertCategory(category)
            try { firestore.saveCategory(category) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteCategory(category: BudgetCategory) {
        viewModelScope.launch { 
            dao.deleteCategory(category)
            try { firestore.deleteCategory(category.id) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveEnvelopeItem(item: EnvelopeItem) {
        viewModelScope.launch { 
            dao.upsertEnvelopeItem(item)
            try { firestore.saveEnvelopeItem(item) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteEnvelopeItem(item: EnvelopeItem) {
        viewModelScope.launch { 
            dao.deleteEnvelopeItem(item)
            try { firestore.deleteEnvelopeItem(item.id) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveTransaction(transaction: BudgetTransaction) {
        viewModelScope.launch { 
            dao.upsertTransaction(transaction)
            try { firestore.saveTransaction(transaction) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteTransaction(transaction: BudgetTransaction) {
        viewModelScope.launch { 
            dao.deleteTransaction(transaction)
            try { firestore.deleteTransaction(transaction.id) } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
