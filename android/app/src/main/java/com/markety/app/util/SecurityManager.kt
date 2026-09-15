package com.markety.app.util

import android.content.Context
import android.content.SharedPreferences
import com.markety.app.data.local.entity.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SecurityManager {

    private const val PREFS_NAME = "markety_security_prefs"
    private const val KEY_MANAGER_PIN = "manager_pin"
    private const val KEY_CASHIER_PIN = "cashier_pin"

    private lateinit var prefs: SharedPreferences

    private val _currentUserRole = MutableStateFlow<UserRole?>(null)
    val currentUserRole: StateFlow<UserRole?> = _currentUserRole.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Default to Manager unlocked for initial smooth onboarding
        if (_currentUserRole.value == null) {
            _currentUserRole.value = UserRole.MANAGER
        }
    }

    fun getManagerPin(): String = prefs.getString(KEY_MANAGER_PIN, "1234") ?: "1234"
    fun getCashierPin(): String = prefs.getString(KEY_CASHIER_PIN, "0000") ?: "0000"

    fun setManagerPin(pin: String) {
        prefs.edit().putString(KEY_MANAGER_PIN, pin).apply()
    }

    fun setCashierPin(pin: String) {
        prefs.edit().putString(KEY_CASHIER_PIN, pin).apply()
    }

    fun authenticate(enteredPin: String): Boolean {
        return when (enteredPin) {
            getManagerPin() -> {
                _currentUserRole.value = UserRole.MANAGER
                _isLocked.value = false
                true
            }
            getCashierPin() -> {
                _currentUserRole.value = UserRole.CASHIER
                _isLocked.value = false
                true
            }
            else -> false
        }
    }

    fun switchUser(role: UserRole) {
        _currentUserRole.value = role
    }

    fun lock() {
        _isLocked.value = true
    }

    fun isManager(): Boolean = _currentUserRole.value == UserRole.MANAGER
    fun canAccessReports(): Boolean = isManager()
    fun canAccessSettings(): Boolean = isManager()
    fun canAccessSuppliers(): Boolean = isManager()
    fun canManageInventory(): Boolean = isManager()
}
