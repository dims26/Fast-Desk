package com.dims.fastdesk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dims.fastdesk.R
import com.dims.fastdesk.ui.client_view.CustomerHomeActivity
import com.google.android.flexbox.FlexboxLayout

class LandingActivity : AppCompatActivity() {

    private lateinit var customerLayout: FlexboxLayout
    private lateinit var staffLayout: FlexboxLayout

    companion object{
        const val login = "loginType"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar) //Transition back to regular theme
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)
    }

    override fun onStart() {
        super.onStart()

        customerLayout = findViewById(R.id.customer_layout)
        staffLayout = findViewById(R.id.staff_layout)

        //clear preferences
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        when(prefs.getString(login, "")){
            CustomerHomeActivity.loginType ->moveToDestination(getCustomerIntent())
            TicketListActivity.loginType -> moveToDestination(getStaffIntent())
            else -> {/**/}
        }

        customerLayout.setOnClickListener {
            moveToDestination(getCustomerIntent())
        }
        staffLayout.setOnClickListener {
            moveToDestination(getStaffIntent())
        }
    }

    private fun getCustomerIntent(): Intent{
        return Intent(applicationContext, CustomerHomeActivity::class.java)
    }

    private fun getStaffIntent(): Intent{
        return Intent(applicationContext, TicketListActivity::class.java)
    }

    private fun moveToDestination(intent: Intent){
        startActivity(intent)
    }
}