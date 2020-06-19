package com.dims.fastdesk.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.dims.fastdesk.datasource.CustomerDataSource;
import com.dims.fastdesk.datasource.CustomerDataSourceFactory;
import com.dims.fastdesk.models.Customer;

public class CustomerListViewModel extends ViewModel {

    //creating livedata for PagedList  and PagedKeyedDataSource
    private LiveData<PagedList<Customer>> customerPagedList;
    public LiveData<CustomerDataSource> liveDataSource = new MutableLiveData<>();

    //Livedata for checking pagedList availability
    public MutableLiveData<Boolean> pagedListLiveDataAvailable = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> progressLiveDataAvailable = new MutableLiveData<>(false);

    //DataSource Factory
    private CustomerDataSourceFactory customerDataSourceFactory;

    public CustomerListViewModel() {
        getCustomers(null);
    }

    public LiveData<PagedList<Customer>> accessRepo() {
        return customerPagedList;
    }

    //called from activity's button click or from search bar
    public void getCustomers(String term){
        if (term == null) {
            //DataSource pull ticket data
            customerDataSourceFactory = new CustomerDataSourceFactory(null, this);
            liveDataSource = customerDataSourceFactory.getCustomerLiveDataSource();
        }else{
            customerDataSourceFactory = new CustomerDataSourceFactory(term, this);
            liveDataSource = customerDataSourceFactory.getCustomerLiveDataSource();
        }

        PagedList.Config pagedListConfig = new PagedList.Config.Builder()
                .setPageSize(2)
                .setEnablePlaceholders(false)
                .build();
        customerPagedList = new LivePagedListBuilder<>(customerDataSourceFactory, pagedListConfig).build();
        pagedListLiveDataAvailable.postValue(true);
    }

    public LiveData<CustomerDataSource> getDataSourceLiveData() {
        return liveDataSource = customerDataSourceFactory.getCustomerLiveDataSource();

    }
}
