package com.henry.budgetmvp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager {
    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchAllData(householdId: String, userId: String): Map<String, List<Any>> {
        // 1. Fetch by Household ID
        val income = db.collection("incomeStreams").whereEqualTo("householdId", householdId).get().await().toObjects<IncomeStream>()
        val categories = db.collection("categories").whereEqualTo("householdId", householdId).get().await().toObjects<BudgetCategory>()
        val items = db.collection("envelopeItems").whereEqualTo("householdId", householdId).get().await().toObjects<EnvelopeItem>()
        val transactions = db.collection("transactions").whereEqualTo("householdId", householdId).get().await().toObjects<BudgetTransaction>()
        
        // 2. Fetch by User ID to catch legacy data (missing householdId field)
        val userIncome = db.collection("incomeStreams").whereEqualTo("userId", userId).get().await().toObjects<IncomeStream>()
        val userCategories = db.collection("categories").whereEqualTo("userId", userId).get().await().toObjects<BudgetCategory>()
        val userItems = db.collection("envelopeItems").whereEqualTo("userId", userId).get().await().toObjects<EnvelopeItem>()
        val userTransactions = db.collection("transactions").whereEqualTo("userId", userId).get().await().toObjects<BudgetTransaction>()

        // Combine and deduplicate by ID
        return mapOf(
            "income" to (income + userIncome).distinctBy { it.id },
            "categories" to (categories + userCategories).distinctBy { it.id },
            "items" to (items + userItems).distinctBy { it.id },
            "transactions" to (transactions + userTransactions).distinctBy { it.id }
        )
    }

    // --- User Profile ---
    suspend fun saveUserProfile(profile: UserProfile) {
        db.collection("users").document(profile.userId).set(profile).await()
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return db.collection("users").document(userId).get().await().toObject(UserProfile::class.java)
    }

    // --- Household Access ---
    suspend fun getHouseholdMembers(householdId: String): List<UserProfile> {
        return db.collection("users").whereEqualTo("householdId", householdId).get().await().toObjects<UserProfile>()
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
