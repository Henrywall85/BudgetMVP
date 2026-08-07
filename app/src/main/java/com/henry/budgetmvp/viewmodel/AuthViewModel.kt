package com.henry.budgetmvp.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _userState = MutableStateFlow(auth.currentUser)
    val userState: StateFlow<com.google.firebase.auth.FirebaseUser?> = _userState

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun signUp(email: String, password: String, onResult: (Boolean) -> Unit) {
        _loading.value = true
        _error.value = null
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loading.value = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                    _userState.value = user
                    onResult(true)
                } else {
                    _error.value = task.exception?.message ?: "Signup failed"
                    onResult(false)
                }
            }
    }

    fun signIn(email: String, password: String, onResult: (Boolean) -> Unit) {
        _loading.value = true
        _error.value = null
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loading.value = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                    _userState.value = user
                    onResult(true)
                } else {
                    _error.value = task.exception?.message ?: "Login failed"
                    onResult(false)
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _userState.value = null
    }
}
