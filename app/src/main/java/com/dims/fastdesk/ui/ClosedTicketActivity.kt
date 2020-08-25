package com.dims.fastdesk.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dims.fastdesk.R
import com.dims.fastdesk.adapters.TicketListPagedAdapter
import com.dims.fastdesk.utilities.NetworkState.*
import com.dims.fastdesk.viewmodels.ClosedTicketsViewModel
import com.dims.fastdesk.viewmodels.*
import kotlinx.android.synthetic.main.activity_closed_ticket.*

class ClosedTicketActivity : AppCompatActivity() {
    private val recyclerAdapter = TicketListPagedAdapter()
    private lateinit var ticketsViewModel: ClosedTicketsViewModel
    private lateinit var dataAdapter : ArrayAdapter<String>
    private lateinit var queueSpinner : Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_closed_ticket)

        //navigate with toolbar spinner
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        queueSpinner = Spinner(supportActionBar?.themedContext)

        recyclerAdapter.isClosedTicket = true
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = recyclerAdapter

        ticketsViewModel = ViewModelProvider(this).get(ClosedTicketsViewModel::class.java)
        ticketsViewModel.views[ticketsViewModel.views.lastIndex] = intent.extras?.getString("department", "Open Tickets") as String

        //Setup spinner
        dataAdapter = ArrayAdapter(this, R.layout.spinner_item, ticketsViewModel.views)
        dataAdapter.setDropDownViewResource(R.layout.spinner_list)
        queueSpinner.adapter = dataAdapter
        queueSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{

            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (parent.getItemAtPosition(position) == ticketsViewModel.views.last())
                    finish()
                else if (parent.getItemAtPosition(position) == CUSTOMER_LIST)
                    startActivity(Intent(applicationContext, CustomerListActivity::class.java))
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
        toolbar.addView(queueSpinner)
    }

    override fun onStart() {
        super.onStart()
        queueSpinner.setSelection(0)
        ticketsViewModel.getDataSourceAvailabilityLiveData().observe(this, Observer {available ->
            if (available){
                ticketsViewModel.getDataSourceLiveData().value!!.netStateLiveData.observe( this, Observer {
                    when(it){
                        SUCCESS, FAILED -> progressBar.visibility = View.GONE
                        LOADING -> progressBar.visibility = View.VISIBLE
                    }
                })
            }
        })
        ticketsViewModel.getPagedListLiveDataAvailable().observe(this, Observer {available ->
            if (available) ticketsViewModel.ticketPagedList.observe(this, Observer {
                recyclerAdapter.submitList(it)
            })
        })
    }
}