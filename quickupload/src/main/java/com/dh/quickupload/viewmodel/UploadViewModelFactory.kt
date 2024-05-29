package com.dh.quickupload.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 携带UploadRepository的ViewModel创建工厂
 */
class UploadViewModelFactory(private val uploadRepository: UploadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UploadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UploadViewModel(uploadRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}