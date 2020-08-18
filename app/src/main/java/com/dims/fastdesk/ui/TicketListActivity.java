package com.dims.fastdesk.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.utilities.FirebaseUtils;
import com.dims.fastdesk.utilities.NetworkState;
import com.dims.fastdesk.viewmodels.TicketsListViewModel;
import com.dims.fastdesk.viewmodels.ViewModelFactory;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

import java.lang.reflect.Field;
import java.util.Objects;

import static com.dims.fastdesk.utilities.NetworkState.FAILED;


public class TicketListActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 801;
    private ProgressBar progressBar;
    private TicketListPagedAdapter recyclerAdapter;
    private TicketsListViewModel ticketsListViewModel;
    private Activity mainActivity = this;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayAdapter dataAdapter;
    private Spinner queueSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);//Transition back to regular theme
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_list);

        //set up actionbar spinner
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        queueSpinner = new Spinner(toolbar.getContext());
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        progressBar = findViewById(R.id.progressBar);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        RecyclerView ticketRecyclerView = findViewById(R.id.recyclerView);
        recyclerAdapter = new TicketListPagedAdapter();
        ticketRecyclerView.setAdapter(recyclerAdapter);

        if (mAuth.getCurrentUser() == null)
            startActivityForResult(FirebaseUtils.getSignInIntent(), RC_SIGN_IN);

        ViewModelFactory factory = new ViewModelFactory(getApplication(), this);
        ticketsListViewModel = new ViewModelProvider(this, factory).get(TicketsListViewModel.class);

        //Setup spinner
        dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, android.R.id.text1, ticketsListViewModel.views);
        dataAdapter.setDropDownViewResource(R.layout.spinner_list);
        queueSpinner.setAdapter(dataAdapter);
        queueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getItemAtPosition(position).equals(TicketsListViewModel.ALL_CLOSED_TICKETS)){
                    Intent intent = new Intent(getApplicationContext(), ClosedTicketActivity.class)
                            .putExtra("department", ticketsListViewModel.views.get(0));
                    startActivity(intent);
                }else if (parent.getItemAtPosition(position).equals(TicketsListViewModel.CUSTOMER_LIST)){
                    Intent intent = new Intent(getApplicationContext(), CustomerListActivity.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        toolbar.addView(queueSpinner);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        ticketRecyclerView.setLayoutManager(layoutManager);

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
                //reload tickets with current sort state
                ticketsListViewModel.toggleSortOrder(ticketsListViewModel.newerFirst);
            }
        });
        //hide drag animation for swipe refresh and show custom progressBar
        try {
            Field f = swipeRefreshLayout.getClass().getDeclaredField("mCircleView");
            f.setAccessible(true);
            ImageView img = (ImageView)f.get(swipeRefreshLayout);
            img.setAlpha(0.0f);
            progressBar.setVisibility(View.VISIBLE);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        queueSpinner.setSelection(0);
        ticketsListViewModel.getPagedListLiveDataAvailable().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    ticketsListViewModel.ticketPagedListLiveData.observe((LifecycleOwner) mainActivity, new Observer<PagedList<Ticket>>() {
                        @Override
                        public void onChanged(PagedList<Ticket> tickets) {
                            recyclerAdapter.submitList(tickets);
                        }
                    });
                }
            }
        });

        ticketsListViewModel.getDataSourceAvailabilityLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    Objects.requireNonNull
                            (ticketsListViewModel.getDataSourceLiveData().getValue())
                            .netStateLiveData
                            .observe((LifecycleOwner) mainActivity, new Observer<Integer>() {
                                @Override
                                public void onChanged(Integer integer) {
                                    if (integer.equals(NetworkState.SUCCESS))
                                        progressBar.setVisibility(View.GONE);
                                    else if (integer.equals(NetworkState.LOADING))
                                        progressBar.setVisibility(View.VISIBLE);
                                    else if (integer.equals(FAILED))
                                        progressBar.setVisibility(View.GONE);
                                }
                            });
                }
            }
        });
        ticketsListViewModel.getQueueSpinnerLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    getSupportActionBar().setDisplayShowCustomEnabled(true);
                }
                dataAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);

        //select the right icon to use
        if (ticketsListViewModel.newerFirst)
            menu.findItem(R.id.action_sort).setIcon(R.drawable.ic_sort_newer);
        else
            menu.findItem(R.id.action_sort).setIcon(R.drawable.ic_sort_older);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            //unsubscribe from department notifications
            SharedPreferences prefs = getApplication().getSharedPreferences("prefs", Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = prefs.edit();
            String department = prefs.getString("department", "");
            if (!prefs.getBoolean("isTopicSubscribed", false) && !department.isEmpty())
                FirebaseMessaging.getInstance().unsubscribeFromTopic(department).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        editor.clear().apply();
                    }
                });
            AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    recyclerAdapter.submitList(null);//clear the recyclerView
                    FirebaseUtils.attachListener();//listen for user authentication state and act appropriately
                }
            });
            FirebaseUtils.detachListener();
        }else if (item.getItemId() == R.id.action_sort){
            boolean isDataSourceReady;
            if (ticketsListViewModel.newerFirst) {
                isDataSourceReady = ticketsListViewModel.toggleSortOrder(false);
                if (isDataSourceReady)
                    item.setIcon(R.drawable.ic_sort_older);
            }else{
                isDataSourceReady = ticketsListViewModel.toggleSortOrder(true);
                if (isDataSourceReady)
                    item.setIcon(R.drawable.ic_sort_newer);
            }
        }else if (item.getItemId() == R.id.action_refresh){
            //reload tickets with current sort state
            ticketsListViewModel.toggleSortOrder(ticketsListViewModel.newerFirst);
        }
        return super.onOptionsItemSelected(item);
    }

    public void startCreateTicketActivity(View view) {
        Intent intent = new Intent(getApplicationContext(), NewTicketActivity.class);
        startActivity(intent);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode != RESULT_OK){
                //The user has pressed the back button or some other error
                Log.d("Log In Issue", response.getError().toString());
                finish();
            }else{
                //clear preferences
                SharedPreferences prefs = getApplication().getSharedPreferences("prefs", Context.MODE_PRIVATE);
                final SharedPreferences.Editor editor = prefs.edit();
                editor.clear().commit();
                ticketsListViewModel.refresh();
                ticketsListViewModel.views.set(0, "");
                dataAdapter.notifyDataSetChanged();
                recyclerAdapter.submitList(null);
            }
        }
    }
}
