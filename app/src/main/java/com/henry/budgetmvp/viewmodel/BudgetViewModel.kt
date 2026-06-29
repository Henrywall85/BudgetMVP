package com.henry.budgetmvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henry.budgetmvp.data.ExpenseEnvelope
import com.henry.budgetmvp.data.IncomeDao
import com.henry.budgetmvp.data.IncomeStream
import kotlinx.coroutines.launch

class BudgetViewModel(private val dao: IncomeDao) : ViewModel() {
    val incomeStreams = dao.getAllIncomeStreams()
    val envelopes = dao.getAllEnvelopes()

    fun saveIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { dao.upsertIncomeStream(stream) }
    }

    fun deleteIncomeStream(stream: IncomeStream) {
        viewModelScope.launch { dao.deleteIncomeStream(stream) }
    }

    fun saveEnvelope(envelope: ExpenseEnvelope) {
        viewModelScope.launch { dao.upsertEnvelope(envelope) }
    }

    fun deleteEnvelope(envelope: ExpenseEnvelope) {
        viewModelScope.launch { dao.deleteEnvelope(envelope) }
    }
}
