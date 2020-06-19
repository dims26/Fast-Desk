package com.dims.fastdesk.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dims.fastdesk.utilities.FirebaseFunctionUtils;
import com.dims.fastdesk.utilities.FirebaseUtils;
import com.dims.fastdesk.utilities.NetworkState;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.DocumentReference;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class NewTicketViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> departmentListLoadedLiveData = new MutableLiveData<>(false);
    private MutableLiveData<Integer> ticketCreatedLiveData = new MutableLiveData<>(NetworkState.LOADING);
    public List<String> departments = new ArrayList<>();

    public DocumentReference customerReference;

    NewTicketViewModel(@NonNull Application application) {
        super(application);

        departments.add(0,"--");
        Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getIdToken(true)
                .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                    @Override
                    public void onSuccess(GetTokenResult getTokenResult) {
                        FirebaseFunctionUtils.getDepartments(getTokenResult.getToken(), getCallback());
                    }
                });
    }

    private Callback getCallback(){
        return  new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                //handle failures
                Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getIdToken(true)
                        .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                            @Override
                            public void onSuccess(GetTokenResult getTokenResult) {
                                FirebaseFunctionUtils.getDepartments(getTokenResult.getToken(), getCallback());
                            }
                        });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String body = response.body().string();
                if (response.code() == 200 && !body.isEmpty()) {
                    //get reference string
                    departments.clear();
                    departments.add(0,"--");
                    departments.addAll(extractJSONData(body));
                    departmentListLoadedLiveData.postValue(true);
                }
            }
        };
    }

    private ArrayList<String> extractJSONData(String body) {
        try {
            JSONObject response = new JSONObject(body);
            JSONArray depts = response.getJSONArray("departments");
            ArrayList<String> departments = new ArrayList<>();

            //populating list from array
            for (int i=0;i<depts.length();i++){
                departments.add(depts.getString(i).toUpperCase());
            }

            return departments;
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public LiveData<Boolean> getDepartments() {
        return departmentListLoadedLiveData;
    }

    public void setTicketCreatedStatus(Integer status){
        ticketCreatedLiveData.postValue(status);
    }

    public LiveData<Integer> getTicketCreatedStatus() {
        return ticketCreatedLiveData;
    }

    public void createTicket(String title, String description, String priority, String department) {
        //call FirebaseUtil method to create the ticket
        if (title.isEmpty() || description.isEmpty()){
            Toast.makeText(getApplication(), "All Fields are Required.", Toast.LENGTH_LONG).show();
            return;
        }
        if (customerReference == null){
            Toast.makeText(getApplication(), "Select a Customer.", Toast.LENGTH_LONG).show();
            return;
        }
        if (priority.isEmpty() || priority.equals("--")){
            Toast.makeText(getApplication(), "Select a Priority Level.", Toast.LENGTH_LONG).show();
            return;
        }
        if (department.isEmpty() || department.equals("--")){
            Toast.makeText(getApplication(), "Select a department.", Toast.LENGTH_LONG).show();
            return;
        }
        //saving strings to sharedPreferences
        SharedPreferences prefs = getApplication().getSharedPreferences("prefs", Context.MODE_PRIVATE);

        //retrieve staff data
        String creatorFName = prefs.getString("fname", "");
        String creatorLName = prefs.getString("lname", "");
        String creatorDepartment = prefs.getString("department", "");

        //call FirebaseUtil createTicket method
        FirebaseUtils.createTicket(
                creatorFName, creatorLName, creatorDepartment,
                customerReference,
                title, description,
                priority, department.toLowerCase(),
                this);
        setTicketCreatedStatus(NetworkState.LOADING);
    }
}
