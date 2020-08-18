package com.dims.fastdesk.ui

import android.net.Uri
import androidx.lifecycle.LiveData

interface NoteUpdateInterface {

    var imageDownloadUriList: List<String>
    var noteEntry: Map<String, Any> //todo has to be initialized in any impl

    fun getImageUploadProgress(): LiveData<Int>
    fun getImageUploadProgressBar(): LiveData<Int>
    fun setImageUploadProgressBar(progress: Int)

    fun uploadImages(selectedPictures: List<Uri>)
    fun updateTicket(updateMap: Map<String, Any>)

}