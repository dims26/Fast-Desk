package com.dims.fastdesk.ui;

import android.app.SearchManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dims.fastdesk.R;
import com.dims.fastdesk.adapters.CustomerListPagedAdapter;
import com.dims.fastdesk.models.Customer;
import com.dims.fastdesk.utilities.NetworkState;
import com.dims.fastdesk.viewmodels.CustomerListViewModel;

import java.lang.reflect.Field;
import java.util.Objects;

import static com.dims.fastdesk.utilities.NetworkState.FAILED;

public class CustomerListActivity extends AppCompatActivity {

    private CustomerListPagedAdapter customerAdapter;
    private CustomerListViewModel customerListViewModel;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private CustomerListActivity customerListActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        progressBar = findViewById(R.id.progressBar);
        Button button = findViewById(R.id.showAllCustomersButton);

        RecyclerView customerRecyclerView = findViewById(R.id.customerRecycler);
        customerAdapter = new CustomerListPagedAdapter(this);
        customerRecyclerView.setAdapter(customerAdapter);

        if (getIntent().getBooleanExtra("isForSelection", false))
            customerAdapter.isForSelection = true;

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        customerRecyclerView.setLayoutManager(layoutManager);

        //getting reference to ViewModel
        customerListViewModel = new ViewModelProvider(this).get(CustomerListViewModel.class);

        //set up listener for clicks on button and load customer list
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customerListViewModel.getCustomers(null);
            }
        });

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
                //refresh customer list
                customerListViewModel.getCustomers(null);
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
        //Observe for list data
        customerListViewModel.pagedListLiveDataAvailable.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    customerListViewModel.accessRepo().observe(customerListActivity, new Observer<PagedList<Customer>>() {
                        @Override
                        public void onChanged(PagedList<Customer> customers) {
                            customerAdapter.submitList(customers);
                        }
                    });
                }
            }
        });

        customerListViewModel.progressLiveDataAvailable.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    Objects.requireNonNull
                            (customerListViewModel.getDataSourceLiveData().getValue())
                            .netStateLiveData
                            .observe(customerListActivity, new Observer<Integer>() {
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_customer_list, menu);
        final MenuItem searchItem = menu.findItem(R.id.search);

        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        LinearLayout searchEditFrame = searchView.findViewById(R.id.search_edit_frame); // Get the Linear Layout
        // Get the associated LayoutParams and set leftMargin
        ((LinearLayout.LayoutParams) searchEditFrame.getLayoutParams()).leftMargin = 0;

        //Remove search icon to the left of the search edit text
        ImageView searchViewIcon = searchView.findViewById(R.id.search_mag_icon);
        ViewGroup linearLayoutSearchView =(ViewGroup) searchViewIcon.getParent();
        linearLayoutSearchView.removeView(searchViewIcon);

        searchView.setQueryHint("First name, Last name, or Email");
        searchView.requestFocus();//sets the focus on searchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.equals("")){
                    //search db with query
                    customerListViewModel.getCustomers(query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}