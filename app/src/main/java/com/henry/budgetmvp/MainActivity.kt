package com.henry.budgetmvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.henry.budgetmvp.ui.navigation.AppNavigation
import com.henry.budgetmvp.ui.theme.BudgetAppTheme
import com.henry.budgetmvp.viewmodel.AuthViewModel
import com.henry.budgetmvp.viewmodel.BudgetViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: BudgetViewModel by viewModels()

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val user by authViewModel.userState.collectAsState()
            
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
