package com.markety.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.MarketyDatabase
import com.markety.app.util.BackupRestoreHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class BackupViewModel(
    private val database: MarketyDatabase
) : ViewModel() {

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    private val _createdBackupFile = MutableStateFlow<File?>(null)
    val createdBackupFile: StateFlow<File?> = _createdBackupFile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun createBackup(context: Context) {
        viewModelScope.launch {
            _backupStatus.value = "جاري إنشاء النسخة الاحتياطية وتأكيد سلامة البيانات..."
            val result = BackupRestoreHelper.createBackup(context, database)
            result.onSuccess { file ->
                _createdBackupFile.value = file
                _backupStatus.value = "تم إنشاء النسخة الاحتياطية بنجاح: ${file.name} (${file.length() / 1024} KB)"
            }.onFailure { error ->
                _errorMessage.value = "فشل إنشاء النسخة الاحتياطية: ${error.message}"
                _backupStatus.value = null
            }
        }
    }

    fun restoreBackup(context: Context, backupFile: File) {
        viewModelScope.launch {
            _backupStatus.value = "جاري استعادة قاعدة البيانات..."
            val result = BackupRestoreHelper.restoreBackup(context, database, backupFile)
            result.onSuccess {
                _backupStatus.value = "تمت استعادة النسخة الاحتياطية بنجاح! يرجى إعادة تشغيل التطبيق لتحميل البيانات المستعادة."
            }.onFailure { error ->
                _errorMessage.value = "فشل استعادة البيانات: ${error.message}"
                _backupStatus.value = null
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _backupStatus.value = null
        _createdBackupFile.value = null
    }

    class Factory(private val db: MarketyDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
                return BackupViewModel(db) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
