package com.dims.fastdesk.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.dims.fastdesk.datasource.TicketDataSource
import com.dims.fastdesk.datasource.TicketDataSourceFactory
import com.dims.fastdesk.models.Ticket
import com.dims.fastdesk.utilities.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

const val ALL_CLOSED_TICKET = "All Closed Tickets"
const val CUSTOMER_LIST = "Customer List"

class ClosedTicketsViewModel(application: Application) : AndroidViewModel(application) {

    @JvmField
    var newerFirst: Boolean = true
    private val pagedListLiveDataAvailable = MutableLiveData(false)
    val ticketPagedList: LiveData<PagedList<Ticket>>
    private val dataSourceAvailabilityLiveData = MutableLiveData(false)

    private val listenerRegistration: ListenerRegistration?
    private val ticketDataSourceFactory: TicketDataSourceFactory

    val views : MutableList<String> = mutableListOf(ALL_CLOSED_TICKET, CUSTOMER_LIST, "")
    private val query : Query = FirebaseFirestore.getInstance().collection("general/closed/closed-tickets")
    init {
        //DataSource to pull ticket data
        ticketDataSourceFactory = TicketDataSourceFactory(query, true, this)
        listenerRegistration = FirebaseUtils.ticketsSetListeners(query, this)

        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(6)
                .setEnablePlaceholders(false)
                .build()
        ticketPagedList = LivePagedListBuilder(ticketDataSourceFactory, pagedListConfig).build()
        pagedListLiveDataAvailable.postValue(true)
    }

    fun setDataSourceAvailabilityLiveData(progress: Boolean){
        dataSourceAvailabilityLiveData.postValue(progress)
    }
    fun getDataSourceAvailabilityLiveData(): LiveData<Boolean> = dataSourceAvailabilityLiveData

    fun getDataSourceLiveData(): LiveData<TicketDataSource> = ticketDataSourceFactory.ticketLiveDataSource

    fun getPagedListLiveDataAvailable(): LiveData<Boolean> = pagedListLiveDataAvailable

    fun toggleSortOrder(newerFirst: Boolean) {
            this.newerFirst = newerFirst
            ticketDataSourceFactory.newerFirst = newerFirst
            ticketPagedList.value?.dataSource?.invalidate()
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }
}