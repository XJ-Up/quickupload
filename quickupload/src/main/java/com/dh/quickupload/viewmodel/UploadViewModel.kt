package com.dh.quickupload.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap

/**
 * 当需要对上传数据进行监听时获取这个 ViewModel监听 dataLiveData
 */
class UploadViewModel(private val uploadRepository: UploadRepository) :ViewModel() {

    val dataLiveData = uploadRepository.uploadData().switchMap {
        MutableLiveData(it)
    }

}