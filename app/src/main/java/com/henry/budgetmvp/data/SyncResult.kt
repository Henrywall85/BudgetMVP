package com.henry.budgetmvp.data

sealed class SyncResult {
    object Synced : SyncResult()
    object LocalOnly : SyncResult()
    data class Error(val message: String) : SyncResult()
}
