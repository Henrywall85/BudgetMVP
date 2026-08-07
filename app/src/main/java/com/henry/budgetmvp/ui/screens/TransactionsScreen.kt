package com.henry.budgetmvp.ui.screens

import androidx.compose.runtime.Composable
import com.henry.budgetmvp.data.BudgetTransaction
import com.henry.budgetmvp.data.CategoryWithItems
import com.henry.budgetmvp.data.IncomeStream
import com.henry.budgetmvp.data.TransactionType
import com.henry.budgetmvp.ui.components.TransactionPage

@Composable
fun TransactionsScreen(
    categoriesWithItems: List<CategoryWithItems>,
    incomeStreams: List<IncomeStream>,
    onSaveTransaction: (TransactionType, Double, String, String, String, String?, String?) -> Unit
) {
    TransactionPage(
        categoriesWithItems = categoriesWithItems,
        incomeStreams = incomeStreams,
        onConfirm = onSaveTransaction
    )
}
