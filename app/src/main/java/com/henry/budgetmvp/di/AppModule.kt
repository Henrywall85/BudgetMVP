package com.henry.budgetmvp.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.henry.budgetmvp.data.AppDatabase
import com.henry.budgetmvp.data.BudgetDao
import com.henry.budgetmvp.data.FirestoreSyncManager
import com.henry.budgetmvp.repository.BudgetRepository
import com.henry.budgetmvp.util.ConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "budget_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    @Singleton
    fun provideFirestoreSyncManager(firestore: FirebaseFirestore): FirestoreSyncManager {
        return FirestoreSyncManager(firestore)
    }

    @Provides
    @Singleton
    fun provideBudgetRepository(
        @ApplicationContext context: Context,
        dao: BudgetDao,
        firestoreSyncManager: FirestoreSyncManager,
    ): BudgetRepository {
        return BudgetRepository(context, dao, firestoreSyncManager)
    }

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return ConnectivityObserver(context)
    }
}
