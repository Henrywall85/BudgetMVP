package com.henry.budgetmvp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(private val db: FirebaseFirestore) {

    suspend fun fetchAllData(householdId: String, userId: String): Map<String, List<Any>> = coroutineScope {
        android.util.Log.d("BudgetSync", "Starting universal cloud fetch for uid='$userId', householdId='$householdId'")
        
        val incomeDeferred = async { 
            try {
                val hDocs = db.collection("incomeStreams").whereEqualTo("householdId", householdId).get().await().toObjects<IncomeStream>()
                val uDocs = db.collection("incomeStreams").whereEqualTo("userId", userId).get().await().toObjects<IncomeStream>()
                (hDocs + uDocs).distinctBy { it.id }
            } catch (e: Exception) {
                android.util.Log.e("BudgetSync", "Error fetching income: ${e.message}")
                emptyList<IncomeStream>()
            }
        }
        
        val categoriesDeferred = async { 
            try {
                val hDocs = db.collection("categories").whereEqualTo("householdId", householdId).get().await().toObjects<BudgetCategory>()
                val uDocs = db.collection("categories").whereEqualTo("userId", userId).get().await().toObjects<BudgetCategory>()
                (hDocs + uDocs).distinctBy { it.id }
            } catch (e: Exception) {
                android.util.Log.e("BudgetSync", "Error fetching categories: ${e.message}")
                emptyList<BudgetCategory>()
            }
        }
        
        val itemsDeferred = async { 
            try {
                val hDocs = db.collection("envelopeItems").whereEqualTo("householdId", householdId).get().await().toObjects<EnvelopeItem>()
                val uDocs = db.collection("envelopeItems").whereEqualTo("userId", userId).get().await().toObjects<EnvelopeItem>()
                (hDocs + uDocs).distinctBy { it.id }
            } catch (e: Exception) {
                android.util.Log.e("BudgetSync", "Error fetching items: ${e.message}")
                emptyList<EnvelopeItem>()
            }
        }
        
        val transactionsDeferred = async { 
            try {
                val hDocs = db.collection("transactions").whereEqualTo("householdId", householdId).get().await().toObjects<BudgetTransaction>()
                val uDocs = db.collection("transactions").whereEqualTo("userId", userId).get().await().toObjects<BudgetTransaction>()
                (hDocs + uDocs).distinctBy { it.id }
            } catch (e: Exception) {
                android.util.Log.e("BudgetSync", "Error fetching transactions: ${e.message}")
                emptyList<BudgetTransaction>()
            }
        }

        mapOf(
            "income" to incomeDeferred.await(),
            "categories" to categoriesDeferred.await(),
            "items" to itemsDeferred.await(),
            "transactions" to transactionsDeferred.await()
        )
    }

    // --- User Profile ---
    suspend fun saveUserProfile(profile: UserProfile) {
        db.collection("users").document(profile.userId).set(profile).await()
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return db.collection("users").document(userId).get().await().toObject(UserProfile::class.java)
    }

    // --- Household Access & Invites ---
    suspend fun getHouseholdMembers(householdId: String): List<UserProfile> {
        return db.collection("users").whereEqualTo("householdId", householdId).get().await().toObjects<UserProfile>()
    }

    suspend fun sendInvite(invite: HouseholdInvite) {
        db.collection("invites").document(invite.id).set(invite).await()
    }

    suspend fun getPendingInvitesForEmail(email: String): List<HouseholdInvite> {
        return db.collection("invites")
            .whereEqualTo("toEmail", email)
            .whereEqualTo("status", "PENDING")
            .get().await().toObjects<HouseholdInvite>()
    }

    suspend fun updateInviteStatus(inviteId: String, status: String) {
        db.collection("invites").document(inviteId).update("status", status).await()
    }

    suspend fun deleteInvite(inviteId: String) {
        db.collection("invites").document(inviteId).delete().await()
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

    suspend fun clearHouseholdData(householdId: String) {
        val collections = listOf("incomeStreams", "categories", "envelopeItems", "transactions")
        
        collections.forEach { collectionName ->
            val snapshot = db.collection(collectionName)
                .whereEqualTo("householdId", householdId)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
        }
    }
}
