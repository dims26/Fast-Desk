package com.dims.fastdesk.datasource;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PageKeyedDataSource;

import com.dims.fastdesk.utilities.NetworkState;
import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.utilities.FirebaseUtils;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;

public class TicketDataSource extends PageKeyedDataSource<Query, Ticket> {

    private Query reference;
    private boolean newerFirst;
    public MutableLiveData<Integer> netStateLiveData = new MutableLiveData<>();

    TicketDataSource(Query reference, boolean newerFirst){
        this.reference = reference;
        this.newerFirst = newerFirst;
        netStateLiveData.postValue(NetworkState.SUCCESS);
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Query> params, @NonNull LoadInitialCallback<Query, Ticket> callback) {
        netStateLiveData.postValue(NetworkState.LOADING);
        FirebaseUtils.pullStaffTicketsDataSource(callback, reference, newerFirst, this);
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Query> params, @NonNull LoadCallback<Query, Ticket> callback) {
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Query> params, @NonNull LoadCallback<Query, Ticket> callback) {
        netStateLiveData.postValue(NetworkState.LOADING);
        FirebaseUtils.pullRemainingStaffTicketsDataSource(callback, params.key, this);
    }
}
