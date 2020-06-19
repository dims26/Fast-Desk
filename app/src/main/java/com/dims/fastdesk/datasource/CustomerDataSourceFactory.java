package com.dims.fastdesk.datasource;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;

import com.dims.fastdesk.models.Customer;
import com.dims.fastdesk.viewmodels.CustomerListViewModel;
import com.google.firebase.firestore.Query;

public class CustomerDataSourceFactory extends DataSource.Factory<Query, Customer> {

    private final CustomerListViewModel model;
    private String term;
    private MutableLiveData<CustomerDataSource> customerLiveDataSource = new MutableLiveData<>();

    public CustomerDataSourceFactory(String term, CustomerListViewModel model){
        this.term = term;
        this.model = model;
    }

    @NonNull
    @Override
    public DataSource<Query, Customer> create() {
        CustomerDataSource customerDataSource = new CustomerDataSource(term);

        customerLiveDataSource.postValue(customerDataSource);
        model.progressLiveDataAvailable.postValue(true);

        return customerDataSource;
    }

    //getter for customerLiveDataSource
    public MutableLiveData<CustomerDataSource> getCustomerLiveDataSource() {
        return customerLiveDataSource;
    }
}
