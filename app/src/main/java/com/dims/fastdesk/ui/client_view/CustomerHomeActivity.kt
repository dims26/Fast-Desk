package com.dims.fastdesk.ui.client_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dims.fastdesk.R
import com.dims.fastdesk.utilities.FirebaseUtils

class CustomerHomeActivity : AppCompatActivity() {
    companion object {
        const val loginType = "customer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_home)
        FirebaseUtils.initNoLogin()
    }
}