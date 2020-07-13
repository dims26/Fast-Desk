package com.dims.fastdesk.datasource;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.DataSource;

import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.viewmodels.ClosedTicketsViewModel;
import com.dims.fastdesk.viewmodels.CustomerTicketsViewModel;
import com.dims.fastdesk.viewmodels.TicketsViewModel;
import com.google.firebase.firestore.Query;

public class TicketDataSourceFactory extends DataSource.Factory<Query, Ticket> {

    private final Object viewModel;
    public boolean newerFirst;
    private Query reference;
    private MutableLiveData<TicketDataSource> ticketLiveDataSource = new MutableLiveData<>();

    public TicketDataSourceFactory(Query reference, boolean newerFirst, ViewModel viewModel){
        this.reference = reference;
        this.newerFirst = newerFirst;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public DataSource<Query, Ticket> create() {
        TicketDataSource ticketDataSource = new TicketDataSource(reference, newerFirst);

        ticketLiveDataSource.postValue(ticketDataSource);
        if (viewModel instanceof TicketsViewModel) {
            ((TicketsViewModel) viewModel).setDataSourceAvailabilityLiveData(true);
        }else if (viewModel instanceof CustomerTicketsViewModel){
            ((CustomerTicketsViewModel) viewModel).setDataSourceAvailabilityLiveData(true);
        }else if (viewModel instanceof ClosedTicketsViewModel){
            ((ClosedTicketsViewModel) viewModel).setDataSourceAvailabilityLiveData(true);
        }
        return ticketDataSource;
    }

    //getter for TicketLiveDataSource
    public LiveData<TicketDataSource> getTicketLiveDataSource() {
        return ticketLiveDataSource;
    }
}
