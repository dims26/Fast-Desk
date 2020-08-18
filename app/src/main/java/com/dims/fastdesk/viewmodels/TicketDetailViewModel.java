package com.dims.fastdesk.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.ui.NoteUpdateInterface;
import com.dims.fastdesk.utilities.FirebaseFunctionUtils;
import com.dims.fastdesk.utilities.FirebaseUtils;
import com.dims.fastdesk.utilities.MoveTicketState;
import com.dims.fastdesk.utilities.NetworkState;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TicketDetailViewModel extends AndroidViewModel implements NoteUpdateInterface {

    public static final String PRIORITY_UPDATE_KEY = "priority";
    public static final String NOTES_UPDATE_KEY = "notes";

    public List<String> departments = new ArrayList<>();

    public Ticket ticket;
    public Map<String, Object> noteEntry = new HashMap<>();
    public List<String> imageDownloadUriList;
    private MutableLiveData<Integer> ticketCreatedLiveData = new MutableLiveData<>(NetworkState.IDLE);
    private MutableLiveData<Integer> ticketUpdatedLiveData = new MutableLiveData<>(NetworkState.IDLE);
    private MutableLiveData<Integer> departmentLiveData = new MutableLiveData<>(NetworkState.IDLE);
    private MutableLiveData<Integer> moveTicketLiveData = new MutableLiveData<>(MoveTicketState.IDLE);
    //image upload
    private MutableLiveData<Integer> imageUploadProgressLiveData = new MutableLiveData<>(-1);
    private MutableLiveData<Integer> imageUploadProgressBarLiveData = new MutableLiveData<>(-3);

    TicketDetailViewModel(@NonNull Application application, Ticket ticket) {
        super(application);
        this.ticket = ticket;
    }

    @NotNull
    @Override
    public List<String> getImageDownloadUriList() { return imageDownloadUriList; }

    @Override
    public void setImageDownloadUriList(@NotNull List<String> imageDownloadUriList) { this.imageDownloadUriList = imageDownloadUriList; }

    @NotNull
    @Override
    public Map<String, Object> getNoteEntry() { return noteEntry; }

    @SuppressWarnings("unchecked")
    @Override
    public void setNoteEntry(@NotNull Map<String, ?> noteEntry) { this.noteEntry = (Map<String, Object>) noteEntry; }

//    public void updateTicket(Map<String, Object> updateMap) {
//        setTicketCreatedStatus(NetworkState.LOADING);
//        FirebaseUtils.updateTicket(ticket, updateMap, this);
//    }todo make sure this works when testing staff module
    @SuppressWarnings("unchecked")
    @Override
    public void updateTicket(@NotNull Map<String, ?> updateMap) {
        setTicketCreatedStatus(NetworkState.LOADING);
        FirebaseUtils.updateTicket(ticket, (Map<String, Object>) updateMap, this);
    }

    public void setDepartmentLiveData(Integer integer){
        departmentLiveData.postValue(integer);
    }

    public void setTicketCreatedStatus(Integer status) {
        ticketCreatedLiveData.postValue(status);
    }

    public void setImageUploadProgress(Integer progress){
        imageUploadProgressLiveData.postValue(progress);
    }

//    public void setImageUploadProgressBar(Integer progress){
//        imageUploadProgressBarLiveData.postValue(progress);
//    }
    @Override
    public void setImageUploadProgressBar(int progress) {
        imageUploadProgressBarLiveData.postValue(progress);
    }

    public void setTicketUpdatedStatus(Integer status){
        ticketUpdatedLiveData.postValue(status);
    }

    public LiveData<Integer> getTicketUpdatedStatus(){
        return ticketUpdatedLiveData;
    }

    @Override
    public LiveData<Integer> getImageUploadProgressBar(){
        return imageUploadProgressBarLiveData;
    }

    @Override
    public LiveData<Integer> getImageUploadProgress(){
        return imageUploadProgressLiveData;
    }

    public LiveData<Integer> getMoveState(){
        return moveTicketLiveData;
    }

    public LiveData<Integer> getTicketCreatedStatus() {
        return ticketCreatedLiveData;
    }

    public LiveData<Integer> getDepartmentsLiveData() {
        return departmentLiveData;
    }

    public void loadDepartments() {
        if (departments.size() > 0){
            //trigger dialog launch
            departmentLiveData.postValue(NetworkState.LOADING);
            departmentLiveData.postValue(NetworkState.SUCCESS);
            return;
        }
        departmentLiveData.postValue(NetworkState.LOADING);
        //trigger loading department list from network
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
                //reload request
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
                    departments.addAll(extractJSONData(body));
                    departmentLiveData.postValue(NetworkState.SUCCESS);
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

            String path = ticket.getPath();
            int end = path.indexOf("/", 12);
            String currentQueue = path.substring(12, end);//current department holding ticket

            departments.remove(currentQueue.toUpperCase());

            return departments;
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void moveTicket(final String intendedDepartment) {
        moveTicketLiveData.postValue(MoveTicketState.LOADING);
        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                    @Override
                    public void onSuccess(GetTokenResult getTokenResult) {
                        FirebaseFunctionUtils.moveTicket(getTokenResult.getToken(),
                                ticket.getPath(), intendedDepartment, getMoveTicketCallback());
                    }
                });
    }

    public void closeTicket() {
        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                    @Override
                    public void onSuccess(GetTokenResult getTokenResult) {
                        moveTicketLiveData.postValue(MoveTicketState.LOADING);
                        FirebaseFunctionUtils.closeTicket(getTokenResult.getToken(),
                                ticket.getPath(), getMoveTicketCallback());
                    }
                });
    }

    private Callback getMoveTicketCallback() {
        return new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                //reload http request
                call.enqueue(getMoveTicketCallback());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                //handle response
                final String body = response.body().string();
                if (response.code() == 200 && !body.isEmpty()) {
                    //good
                    moveTicketLiveData.postValue(MoveTicketState.SUCCESS);
                }else if (response.code() == 400){
                    //bad request, wrong param
                    moveTicketLiveData.postValue(MoveTicketState.BAD_PARAM);
                }else if (response.code() == 403){
                    //unauthorized
                    moveTicketLiveData.postValue(MoveTicketState.UNAUTHORIZED);
                }else if (response.code() == 404){
                    //not found
                    moveTicketLiveData.postValue(MoveTicketState.NOT_FOUND);
                }else if (response.code() == 500){
                    //internal error, little chance of the ticket being deleted; finish activity
                    moveTicketLiveData.postValue(MoveTicketState.ERROR);
                }
            }
        };
    }


//    public void uploadImages(List<Uri> selectedPictures) {
//        FirebaseUtils.uploadTicketImages(selectedPictures, this);
//    }
    @SuppressWarnings("unchecked")
    @Override
    public void uploadImages(@NotNull List<? extends Uri> selectedPictures) {
        FirebaseUtils.uploadTicketImages((List<Uri>) selectedPictures, this);
    }

    public void refreshTicket() {
        FirebaseUtils.refreshTicket(ticket.getPath(), this);
    }
}
