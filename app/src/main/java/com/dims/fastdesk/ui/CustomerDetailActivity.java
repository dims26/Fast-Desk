package com.dims.fastdesk.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dims.fastdesk.R;
import com.dims.fastdesk.adapters.TicketListPagedAdapter;
import com.dims.fastdesk.models.Customer;
import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.utilities.NetworkState;
import com.dims.fastdesk.viewmodels.CustomerTicketsViewModel;
import com.dims.fastdesk.viewmodels.ViewModelFactory;

import java.util.Objects;

import static com.dims.fastdesk.utilities.NetworkState.FAILED;

public class CustomerDetailActivity extends AppCompatActivity {

    private Customer customer;
    private ProgressBar progressBar;
    private TicketListPagedAdapter recyclerAdapter;
    private CustomerTicketsViewModel ticketsViewModel;
    private Activity customerDetailActivity = this;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progressBar);
        RecyclerView ticketRecyclerView = findViewById(R.id.recyclerView);
        recyclerAdapter = new TicketListPagedAdapter();
        recyclerAdapter.isFromCustomerDetail = true;
        ticketRecyclerView.setAdapter(recyclerAdapter);

        TextView customerNameTextView, emailTextView, addressTextView, phoneTextView;
        customerNameTextView = findViewById(R.id.nameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        addressTextView = findViewById(R.id.addressTextView);
        phoneTextView = findViewById(R.id.phoneTextView);

        //get ticket data from calling activity and save it in viewModel object
        Intent intent = getIntent();
        customer = (Customer) intent.getSerializableExtra("customer");
        if (customer == null){
            Toast.makeText(getApplicationContext(), "Unable to Retrieve Customer Information", Toast.LENGTH_LONG).show();
            finish();
        }else{
            customerNameTextView.setText(customer.getName());
            emailTextView.setText(customer.getEmail());
            addressTextView.setText(customer.getAddress());
            phoneTextView.setText(customer.getPhone());
            getSupportActionBar().setTitle(customer.getId());
        }
        ViewModelFactory factory = new ViewModelFactory(getApplication(), new Activity(),new Ticket() , customer);
        ticketsViewModel = new ViewModelProvider(this, factory).get(CustomerTicketsViewModel.class);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        ticketRecyclerView.setLayoutManager(layoutManager);

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //reload tickets with current sort state
                ticketsViewModel.toggleSortOrder(ticketsViewModel.newerFirst);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ticketsViewModel.getPagedListLiveDataAvailable().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    ticketsViewModel.ticketPagedList.observe((LifecycleOwner) customerDetailActivity, new Observer<PagedList<Ticket>>() {
                        @Override
                        public void onChanged(PagedList<Ticket> tickets) {
                            recyclerAdapter.submitList(tickets);
                        }
                    });
                }
            }
        });

        ticketsViewModel.getDataSourceAvailabilityLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    Objects.requireNonNull
                            (ticketsViewModel.getDataSourceLiveData().getValue())
                            .netStateLiveData
                            .observe((LifecycleOwner) customerDetailActivity, new Observer<Integer>() {
                                @Override
                                public void onChanged(Integer integer) {
                                    if (integer.equals(NetworkState.SUCCESS)) {
                                        progressBar.setVisibility(View.GONE);
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                    else if (integer.equals(NetworkState.LOADING))
                                        progressBar.setVisibility(View.VISIBLE);
                                    else if (integer.equals(FAILED))
                                        progressBar.setVisibility(View.GONE);
                                }
                            });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_customer_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh){
            //enable swipeRefresh indicator
            swipeRefreshLayout.setRefreshing(true);
            //refresh ticket
            ticketsViewModel.toggleSortOrder(ticketsViewModel.newerFirst);
        }
        return super.onOptionsItemSelected(item);
    }
}