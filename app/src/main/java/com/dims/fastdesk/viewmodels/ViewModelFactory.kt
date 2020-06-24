package com.dims.fastdesk.viewmodels

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dims.fastdesk.models.Customer
import com.dims.fastdesk.models.Ticket
import com.dims.fastdesk.ui.MainActivity

class ViewModelFactory @JvmOverloads constructor(val application: Application, val activity: Activity = Activity(),
                       val ticket: Ticket = Ticket(), val customer: Customer = Customer()) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("unchecked_cast")
        return when{
            modelClass.isAssignableFrom(NewTicketViewModel::class.java)  -> NewTicketViewModel(application)
            modelClass.isAssignableFrom(TicketsViewModel::class.java) &&
                    activity::class.java.toString() == MainActivity::class.java.toString() -> TicketsViewModel(activity)
            modelClass.isAssignableFrom(TicketDetailViewModel::class.java) &&
                    ticket.id != null -> TicketDetailViewModel(application, ticket)
            modelClass.isAssignableFrom(CustomerTicketsViewModel::class.java) &&
                    customer.id != null -> CustomerTicketsViewModel(customer, application)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}