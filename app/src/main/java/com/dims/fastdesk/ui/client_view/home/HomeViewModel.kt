package com.dims.fastdesk.ui.client_view.home

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.dims.fastdesk.datasource.TicketDataSource
import com.dims.fastdesk.datasource.TicketDataSourceFactory
import com.dims.fastdesk.models.Customer
import com.dims.fastdesk.models.Ticket
import com.dims.fastdesk.utilities.FirebaseFunctionUtils
import com.dims.fastdesk.utilities.FirebaseUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val pagedListLiveDataAvailable = MutableLiveData(false)
    private val dataSourceAvailabilityLiveData = MutableLiveData(false)
    private val complaintCountLiveData = MutableLiveData(-1)
    private var fname = ""
    var greetingText = "Hi $fname"

    lateinit var ticketPagedListLiveData: LiveData<PagedList<Ticket>>
    private lateinit var listenerRegistration: ListenerRegistration
    private lateinit var ticketDataSourceFactory: TicketDataSourceFactory
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var customer: Customer

    @JvmField
    var newerFirst: Boolean = true

    fun load() {
        pagedListLiveDataAvailable.postValue(false)
        mAuth.currentUser!!.getIdToken(true).addOnSuccessListener { getTokenResult ->
            FirebaseFunctionUtils.getCustomerData(getTokenResult.token, getTicketsCallback())
        }
    }

    fun getComplaintCountLiveData(): LiveData<Int> = complaintCountLiveData
    fun getPagedListLiveDataAvailable(): LiveData<Boolean> = pagedListLiveDataAvailable
    fun getDataSourceAvailabilityLiveData(): LiveData<Boolean> = dataSourceAvailabilityLiveData
    fun getDataSourceLiveData(): LiveData<TicketDataSource> = ticketDataSourceFactory.ticketLiveDataSource


    fun setDataSourceAvailabilityLiveData(progress: Boolean){
        dataSourceAvailabilityLiveData.postValue(progress)
    }
    fun setComplaintCountLiveData(count: Int){
        complaintCountLiveData.postValue(count)
    }

    fun toggleSortOrder(newerFirst: Boolean) {
        this.newerFirst = newerFirst
        ticketDataSourceFactory.newerFirst = newerFirst
        ticketPagedListLiveData.value?.dataSource?.invalidate()
    }

    private fun getTicketsCallback(): Callback? {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // todo notify there's a possible network failure, check connection
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                //Can't access response.body().string() multiple times, save it in a variable
                val body = response.body!!.string()
                if (response.code == 200 && body.isNotEmpty()) {
                    //get reference string
                    val collection: String = extractCustomerInfo(body)

                    if (collection == "") {
                        //todo maybe load an error message here, couldn't get name of collection group
                        return
                    }

                    val customerRef = FirebaseFirestore.getInstance()
                            .document(customer.path)

                    //retrieve tickets using the path in response
                    val reference: Query = FirebaseFirestore.getInstance()
                            .collectionGroup(collection).whereEqualTo(Ticket.CUSTOMER, customerRef)

                    //DataSource pull ticket data
                    ticketDataSourceFactory = TicketDataSourceFactory(reference, true, this@HomeViewModel)
                    listenerRegistration = FirebaseUtils.ticketsSetListeners(reference, this@HomeViewModel)
                    val pagedListConfig = PagedList.Config.Builder()
                            .setPageSize(6)
                            .setEnablePlaceholders(false)
                            .build()
                    ticketPagedListLiveData = LivePagedListBuilder(ticketDataSourceFactory, pagedListConfig).build()
                    pagedListLiveDataAvailable.postValue(true)
                }
            }
        }
    }


    private fun extractCustomerInfo(jsonBody: String): String {
        return try {
            val response = JSONObject(jsonBody)
            val customerData = response.getJSONObject("customer")
            val customerPath = response.getString("customerPath")
            val collectionName = response.getString("collectionName")

            customer = Customer()
            with(customerData){
                customer.id = getString("userID")
                customer.name = "${getString(Customer.FNAME)} ${getString(Customer.LNAME)}"
                customer.address = getString(Customer.ADDRESS)
                customer.email = getString(Customer.EMAIL)
                customer.phone = getString(Customer.PHONE)
                customer.path = customerPath

                fname = getString(Customer.FNAME)
                greetingText = "Hi $fname"
            }


//            //saving strings to sharedPreferences
            val prefs: SharedPreferences = getApplication<Application>().getSharedPreferences("prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("customerName", customer.name)
            editor.apply()
//            if (!prefs.getBoolean("isTopicSubscribed", false)) FirebaseMessaging.getInstance().subscribeToTopic(department.toLowerCase()).addOnSuccessListener {
//                Log.d("TicketsViewModel", "successfully subscribed to notifications for: $department")
//                editor.putBoolean("isTopicSubscribed", true).commit()
//            }
            collectionName
        } catch (e: JSONException) {
            e.printStackTrace()
            ""
        }
    }

    override fun onCleared() {
        listenerRegistration.remove()
        super.onCleared()
    }
}