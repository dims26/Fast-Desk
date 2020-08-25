package com.dims.fastdesk.ui

import android.net.Uri
import androidx.lifecycle.LiveData

interface NoteUpdateInterface {

    var imageDownloadUriList: List<String>//might need to be nullable
    var noteEntry: MutableMap<String, Any> //todo has to be initialized in any impl

    fun isTitleVisible(): Boolean
    fun isViewSwitchVisible(): Boolean
    fun getImageUploadProgress(): LiveData<Int>
    fun getImageUploadProgressBar(): LiveData<Int>
    fun setImageUploadProgress(progress: Int)
    fun setImageUploadProgressBar(progress: Int)

    fun setTicketCreatedStatus(state: Int)
    fun getTicketCreatedStatus(): LiveData<Int>

    fun uploadImages(selectedPictures: List<Uri>)
    fun setNote(updateMap: Map<String, Any>)

}