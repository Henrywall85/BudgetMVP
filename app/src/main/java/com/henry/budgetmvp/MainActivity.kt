package com.henry.budgetmvp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.henry.budgetmvp.data.AppDatabase
import com.henry.budgetmvp.ui.navigation.AppNavigation
import com.henry.budgetmvp.ui.theme.BudgetAppTheme
import com.henry.budgetmvp.viewmodel.AuthViewModel
import com.henry.budgetmvp.viewmodel.BudgetViewModel

class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "budget_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    private val viewModel: BudgetViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetViewModel(db.budgetDao()) as T
            }
        }
    }

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val user by authViewModel.userState.collectAsState()
            val context = LocalContext.current
            
            val packageInfo = remember {
                try {
                    packageManager.getPackageInfo(packageName, 0)
                } catch (e: Exception) {
                    null
                }
            }
            val versionName = packageInfo?.versionName ?: "1.0"

            // Pass the userId to the ViewModel
            LaunchedEffect(user) {
                viewModel.setUserId(user?.uid, user?.email)
            }

            // Status message observer
            LaunchedEffect(Unit) {
                viewModel.statusMessage.collect { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }

            BudgetAppTheme {
                AppNavigation(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    versionName = versionName
                )
            }
        }
    }
}
