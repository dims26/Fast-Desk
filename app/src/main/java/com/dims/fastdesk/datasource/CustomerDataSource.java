package com.dims.fastdesk.datasource;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PageKeyedDataSource;

import com.dims.fastdesk.models.Customer;
import com.dims.fastdesk.utilities.FirebaseUtils;
import com.dims.fastdesk.utilities.NetworkState;
import com.google.firebase.firestore.Query;

public class CustomerDataSource extends PageKeyedDataSource<Query, Customer> {

    private String term;
    public MutableLiveData<Integer> netStateLiveData = new MutableLiveData<>();

    CustomerDataSource(String term){
        this.term = term;
        netStateLiveData.postValue(NetworkState.SUCCESS);
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Query> params, @NonNull LoadInitialCallback<Query, Customer> callback) {
        netStateLiveData.postValue(NetworkState.LOADING);
        FirebaseUtils.pullCustomerData(callback, term, this);
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Query> params, @NonNull LoadCallback<Query, Customer> callback) {
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Query> params, @NonNull LoadCallback<Query, Customer> callback) {
        netStateLiveData.postValue(NetworkState.LOADING);
        FirebaseUtils.pullRemainingCustomerData(callback, params.key, this);
    }
}
