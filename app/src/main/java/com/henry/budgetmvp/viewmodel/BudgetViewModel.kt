package com.henry.budgetmvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henry.budgetmvp.data.*
import kotlinx.coroutines.launch

class BudgetViewModel(private val dao: BudgetDao) : ViewModel() {
    val incomeStreams = dao.getAllIncomeStreams()
    val categoriesWithItems = dao.getAllCategoriesWithItems()

    fun saveIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { dao.upsertIncomeStream(stream) }
    }

    fun deleteIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { dao.deleteIncomeStream(stream) }
    }

    fun saveCategory(category: BudgetCategory) {
        viewModelScope.launch { dao.upsertCategory(category) }
    }

    fun deleteCategory(category: BudgetCategory) {
        viewModelScope.launch { dao.deleteCategory(category) }
    }

    fun saveEnvelopeItem(item: EnvelopeItem) {
        viewModelScope.launch { dao.upsertEnvelopeItem(item) }
    }

    fun deleteEnvelopeItem(item: EnvelopeItem) {
        viewModelScope.launch { dao.deleteEnvelopeItem(item) }
    }

    val transactions = dao.getAllTransactions()

    fun saveTransaction(transaction: BudgetTransaction) {
        viewModelScope.launch { dao.upsertTransaction(transaction) }
    }

    fun deleteTransaction(transaction: BudgetTransaction) {
        viewModelScope.launch { dao.deleteTransaction(transaction) }
    }
}
