package com.dims.fastdesk.viewmodels;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.dims.fastdesk.datasource.TicketDataSource;
import com.dims.fastdesk.datasource.TicketDataSourceFactory;
import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.utilities.FirebaseFunctionUtils;
import com.dims.fastdesk.utilities.FirebaseUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TicketsViewModel extends AndroidViewModel {

    public static final String CUSTOMER_LIST = "Customer List";
    public static final String ALL_CLOSED_TICKETS = "All Closed Tickets";
    //creating livedata for PagedList  and PagedKeyedDataSource
    public LiveData<PagedList<Ticket>> ticketPagedListLiveData;
    public boolean newerFirst = true;
    private TicketsViewModel viewModel = this;

    //Livedata for checking pagedList availability
    private MutableLiveData<Boolean> pagedListLiveDataAvailable = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> dataSourceAvailabilityLiveData = new MutableLiveData<>(false);

    //Livedata for view information
    private MutableLiveData<Boolean> queueSpinnerLiveData = new MutableLiveData<>(false);

    public List<String> views = Arrays.asList("", ALL_CLOSED_TICKETS, CUSTOMER_LIST);
    private TicketDataSourceFactory ticketDataSourceFactory;
    private final FirebaseAuth mAuth;
    private ListenerRegistration listenerRegistration;

    TicketsViewModel(Activity activity){
        super(activity.getApplication());
        //initialize Firebase auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUtils.openFirebaseReference(activity);
        FirebaseUtils.attachListener();

        mAuth.getCurrentUser().getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
            @Override
            public void onSuccess(GetTokenResult getTokenResult) {
                FirebaseFunctionUtils.getStaffTicketRef(getTokenResult.getToken(), getTicketsCallback());
            }
        });
    }

    @Override
    protected void onCleared() {
        listenerRegistration.remove();
        super.onCleared();
    }

    public LiveData<Boolean> getPagedListLiveDataAvailable(){
        return pagedListLiveDataAvailable;
    }

    public LiveData<Boolean> getDataSourceAvailabilityLiveData() {
        return dataSourceAvailabilityLiveData;
    }

    public void setDataSourceAvailabilityLiveData(boolean progress) {
        this.dataSourceAvailabilityLiveData.postValue(progress);
    }

    private Callback getTicketsCallback() {
        return new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                //run any UI related code from the UI thread
                mAuth.getCurrentUser().getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                    @Override
                    public void onSuccess(GetTokenResult getTokenResult) {
                        FirebaseFunctionUtils.getStaffTicketRef(getTokenResult.getToken(), getTicketsCallback());
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull final Response response) throws IOException {
                //Can't access response.body().string() multiple times, OkHttp doesn't keep a reference to
                //it, save it in a variable instead
                final String body = response.body().string();
                if (response.code() == 200 && !body.isEmpty()) {
                    //get reference string
                    String ref = extractStaffAndTicketData(body);

                    //retrieved tickets using the path in response
                    Query reference = FirebaseFirestore.getInstance()
                            .collection(ref);

                    //DataSource pull ticket data
                    ticketDataSourceFactory = new TicketDataSourceFactory(reference, newerFirst, viewModel);
                    listenerRegistration = FirebaseUtils.ticketsSetListeners(reference, viewModel);


                    PagedList.Config pagedListConfig = new PagedList.Config.Builder()
                            .setPageSize(6)
                            .setEnablePlaceholders(false)
                            .build();
                    ticketPagedListLiveData = new LivePagedListBuilder<>(ticketDataSourceFactory, pagedListConfig).build();
                    pagedListLiveDataAvailable.postValue(true);
                }
            }
        };
    }

    private String extractStaffAndTicketData(String jsonBody) {
        try {
            JSONObject response = new JSONObject(jsonBody);

            final String fname = response.getString("fname"),
                    lname = response.getString("lname"),
                    path = response.getString("path"),
                    staffPath = response.getString("staffPath");

            //extract department name from String
            int end = path.indexOf("/", 12);
            final String department = path.substring(12, end).toUpperCase();

            //saving strings to sharedPreferences
            SharedPreferences prefs = getApplication().getSharedPreferences("prefs", Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString("fname", fname);
            editor.putString("lname", lname);
            editor.putString("path", path);
            editor.putString("staffPath", staffPath);
            editor.putString("department", department);
            editor.apply();

            if (!prefs.getBoolean("isTopicSubscribed", false))
                FirebaseMessaging.getInstance().subscribeToTopic(department.toLowerCase()).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("TicketsViewModel", "successfully subscribed to notifications for: " + department);
                        editor.putBoolean("isTopicSubscribed", true).commit();
                    }
                });

            String s = department.substring(0, 1).toUpperCase() + department.substring(1).toLowerCase();
            if (!views.get(0).equals(s)){
                views.set(0, s);
                queueSpinnerLiveData.postValue(true);
            }

            return path;
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }

    }

    public boolean toggleSortOrder(boolean newerFirst) {
        if (ticketDataSourceFactory != null) {
            this.newerFirst = newerFirst;
            ticketDataSourceFactory.newerFirst = newerFirst;
            ticketPagedListLiveData.getValue().getDataSource().invalidate();
            return true;
        }
        return false;
    }

    public LiveData<TicketDataSource> getDataSourceLiveData() {
        return ticketDataSourceFactory.getTicketLiveDataSource();
    }

    public LiveData<Boolean> getQueueSpinnerLiveData() {
        return queueSpinnerLiveData;
    }
}