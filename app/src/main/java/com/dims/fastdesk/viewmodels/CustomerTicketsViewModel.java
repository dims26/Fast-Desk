package com.dims.fastdesk.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.dims.fastdesk.datasource.TicketDataSource;
import com.dims.fastdesk.datasource.TicketDataSourceFactory;
import com.dims.fastdesk.models.Customer;
import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.utilities.FirebaseFunctionUtils;
import com.dims.fastdesk.utilities.FirebaseUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CustomerTicketsViewModel extends AndroidViewModel {

    private final Customer customer;
    private FirebaseAuth mAuth;
    private CustomerTicketsViewModel viewModel = this;
    private TicketDataSourceFactory ticketDataSourceFactory;
    public LiveData<PagedList<Ticket>> ticketPagedList;
    public boolean newerFirst = true;

    //Livedata for checking pagedList availability
    private MutableLiveData<Boolean> pagedListLiveDataAvailable = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> dataSourceAvailabilityLiveData = new MutableLiveData<>(false);
    private ListenerRegistration listenerRegistration;


    CustomerTicketsViewModel(Customer customer, Application application) {
        super(application);

        this.customer = customer;
        mAuth = FirebaseAuth.getInstance();
        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
            @Override
            public void onSuccess(GetTokenResult getTokenResult) {
                FirebaseFunctionUtils.getStaffTicketRef(getTokenResult.getToken(), getTicketRefCallback());
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
        dataSourceAvailabilityLiveData.postValue(progress);
    }

    private Callback getTicketRefCallback() {
        return new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                //run any UI related code from the UI thread
                FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                        .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                    @Override
                    public void onSuccess(GetTokenResult getTokenResult) {
                        FirebaseFunctionUtils.getStaffTicketRef(getTokenResult.getToken(), getTicketRefCallback());
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
                    DocumentReference customerReference = FirebaseFirestore.getInstance().document(customer.getPath());

                    reference = reference.whereEqualTo("customer", customerReference);
                    listenerRegistration = FirebaseUtils.ticketsSetListeners(reference, viewModel);

                    //DataSource pull ticket data
                    ticketDataSourceFactory = new TicketDataSourceFactory(reference, newerFirst, viewModel);
                    PagedList.Config pagedListConfig = new PagedList.Config.Builder()
                            .setPageSize(6)
                            .setEnablePlaceholders(false)
                            .build();
                    ticketPagedList = new LivePagedListBuilder<>(ticketDataSourceFactory, pagedListConfig).build();
                    pagedListLiveDataAvailable.postValue(true);
                }
            }
        };
    }

    private String extractStaffAndTicketData(String jsonBody) {
        try {
            JSONObject response = new JSONObject(jsonBody);

            return response.getString("path");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }

    }

    public boolean toggleSortOrder(boolean newerFirst) {
        if (ticketDataSourceFactory != null) {
            this.newerFirst = newerFirst;
            ticketDataSourceFactory.newerFirst = newerFirst;
            //invalidate datasource and trigger new load
            ticketPagedList.getValue().getDataSource().invalidate();
            return true;
        }
        return false;
    }

    public LiveData<TicketDataSource> getDataSourceLiveData() {
        return ticketDataSourceFactory.getTicketLiveDataSource();
    }
}
