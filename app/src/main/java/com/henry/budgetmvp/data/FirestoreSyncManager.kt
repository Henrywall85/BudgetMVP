package com.henry.budgetmvp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager {
    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchAllData(userId: String): Map<String, List<Any>> {
        val income = db.collection("incomeStreams").whereEqualTo("userId", userId).get().await().toObjects<IncomeStream>()
        val categories = db.collection("categories").whereEqualTo("userId", userId).get().await().toObjects<BudgetCategory>()
        val items = db.collection("envelopeItems").whereEqualTo("userId", userId).get().await().toObjects<EnvelopeItem>()
        val transactions = db.collection("transactions").whereEqualTo("userId", userId).get().await().toObjects<BudgetTransaction>()
        
        return mapOf(
            "income" to income,
            "categories" to categories,
            "items" to items,
            "transactions" to transactions
        )
    }

    // --- Income Streams ---
    suspend fun saveIncomeStream(stream: IncomeStream) {
        db.collection("incomeStreams").document(stream.id).set(stream).await()
    }

    suspend fun deleteIncomeStream(id: String) {
        db.collection("incomeStreams").document(id).delete().await()
    }

    // --- Categories ---
    suspend fun saveCategory(category: BudgetCategory) {
        db.collection("categories").document(category.id).set(category).await()
    }

    suspend fun deleteCategory(id: String) {
        // In a real app, you'd also delete items under this category
        db.collection("categories").document(id).delete().await()
    }

    // --- Envelope Items ---
    suspend fun saveEnvelopeItem(item: EnvelopeItem) {
        db.collection("envelopeItems").document(item.id).set(item).await()
    }

    suspend fun deleteEnvelopeItem(id: String) {
        db.collection("envelopeItems").document(id).delete().await()
    }

    // --- Transactions ---
    suspend fun saveTransaction(transaction: BudgetTransaction) {
        db.collection("transactions").document(transaction.id).set(transaction).await()
    }

    suspend fun deleteTransaction(id: String) {
        db.collection("transactions").document(id).delete().await()
    }
}
