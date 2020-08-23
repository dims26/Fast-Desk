package com.dims.fastdesk.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dims.fastdesk.R;
import com.dims.fastdesk.adapters.NotesAdapter;
import com.dims.fastdesk.models.Customer;
import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.utilities.MoveTicketState;
import com.dims.fastdesk.utilities.NetworkState;
import com.dims.fastdesk.viewmodels.TicketDetailViewModel;
import com.dims.fastdesk.viewmodels.ViewModelFactory;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FieldValue;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TicketDetailActivity extends AppCompatActivity{

    private TextView titleTextView, customerNameTextView, dateTimeTextView;
    private ImageButton priorityButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    NotesAdapter notesAdapter;

    private GradientDrawable background;
    private RelativeLayout loadingPanel;

    TicketDetailViewModel ticketDetailViewModel;
    TicketDetailActivity ticketDetailActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //get ticket data from calling activity and save it in viewModel object
        Intent intent = getIntent();
        ViewModelFactory factory = new ViewModelFactory(getApplication(), new Activity(),
                (Ticket) intent.getSerializableExtra("ticket"));
        ticketDetailViewModel = new ViewModelProvider(this, factory).get(TicketDetailViewModel.class);

        //set title of the toolbar
        getSupportActionBar().setTitle(ticketDetailViewModel.ticket.getId());

        titleTextView = findViewById(R.id.titleTextView);
        customerNameTextView = findViewById(R.id.nameTextView);
        dateTimeTextView = findViewById(R.id.dateTextView);
        RecyclerView notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesAdapter = new NotesAdapter(new ArrayList<>(ticketDetailViewModel.ticket.getNotes()));
        priorityButton = findViewById(R.id.priorityButton);
        loadingPanel = findViewById(R.id.loadingPanel);
        swipeRefreshLayout = findViewById(R.id.content_ticket_detail);

        loadingPanel.setVisibility(View.GONE);

        background = (GradientDrawable) priorityButton.getBackground().mutate();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        notesRecyclerView.setLayoutManager(layoutManager);
        notesRecyclerView.setAdapter(notesAdapter);

        //set values of textViews and priority ImageButton
        titleTextView.setText(ticketDetailViewModel.ticket.getTitle());
        customerNameTextView.setText(ticketDetailViewModel.ticket.getCustomerName());
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        dateTimeTextView.setText(dateFormat.format(ticketDetailViewModel.ticket.getDate()));
        setPriority();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FragmentManager manager = getSupportFragmentManager();
                Fragment frag = manager.findFragmentByTag("fragment_edit_name");
                if (frag != null) {
                    manager.beginTransaction().remove(frag).commit();
                }
                NoteInputFragment noteInputFragment =  new NoteInputFragment(ticketDetailViewModel);
                noteInputFragment.show(manager, "fragment_edit_name");
            }
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //refresh ticket
                ticketDetailViewModel.refreshTicket();
            }
        });

        //Disable buttons for interacting with tickets if ticket is closed
        if (getIntent().getBooleanExtra("isClosedTicket", false)){
            fab.setVisibility(View.GONE);
            ImageButton moveButton = findViewById(R.id.moveButton),
                    closeButton = findViewById(R.id.closeButton), priorityButton = findViewById(R.id.priorityButton);
            moveButton.setEnabled(false);
            closeButton.setEnabled(false);
            priorityButton.setEnabled(false);
        }
        if (getIntent().getBooleanExtra("isFromCustomerDetail", false)) {
            ImageButton customerButton = findViewById(R.id.customerButton);
            customerButton.setEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ticket_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh){
            //enable swipeRefresh indicator
            swipeRefreshLayout.setRefreshing(true);
            //refresh ticket
            ticketDetailViewModel.refreshTicket();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ticketDetailViewModel.getTicketCreatedStatus().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer.equals(NetworkState.SUCCESS)){
                    loadingPanel.setVisibility(View.GONE);
                    if (!ticketDetailViewModel.noteEntryMap.isEmpty()) {
                        ticketDetailViewModel.ticket.addNotes(new HashMap<>(ticketDetailViewModel.noteEntryMap));
                        notesAdapter.updateNotes(new ArrayList<>(ticketDetailViewModel.ticket.getNotes()));
                        notesAdapter.notifyDataSetChanged();
                        ticketDetailViewModel.noteEntryMap.clear();
                    }
                }
                else if (integer.equals(NetworkState.LOADING)) {
                    loadingPanel.setVisibility(View.VISIBLE);
                }
                else if (integer.equals(NetworkState.FAILED)) {
                    loadingPanel.setVisibility(View.GONE);
                    //handle update failure
                    ticketDetailViewModel.noteEntryMap.clear();
                }
            }
        });

        ticketDetailViewModel.getTicketUpdatedStatus().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer.equals(NetworkState.SUCCESS)){
                    //set values of textViews and priority ImageButton
                    titleTextView.setText(ticketDetailViewModel.ticket.getTitle());
                    customerNameTextView.setText(ticketDetailViewModel.ticket.getCustomerName());
                    DateFormat dateFormat = DateFormat.getDateTimeInstance();
                    dateTimeTextView.setText(dateFormat.format(ticketDetailViewModel.ticket.getDate()));
                    setPriority();
                    //refresh recycler
                    notesAdapter.updateNotes(new ArrayList<>(ticketDetailViewModel.ticket.getNotes()));
                    notesAdapter.notifyDataSetChanged();
                    //dismiss swipe to refresh
                    swipeRefreshLayout.setRefreshing(false);
                }else if (integer.equals(NetworkState.NOT_FOUND)){
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(ticketDetailActivity.getApplicationContext(), "Ticket Not Found", Toast.LENGTH_LONG)
                            .show();
                    ticketDetailActivity.finish();
                }else if (integer.equals(NetworkState.FAILED)){
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(ticketDetailActivity.getApplicationContext(), "Refresh failed", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        ticketDetailViewModel.getDepartmentsLiveData().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer.equals(NetworkState.SUCCESS)){
                    loadingPanel.setVisibility(View.GONE);
                    moveTicket();
                }
                else if (integer.equals(NetworkState.LOADING)) {
                    loadingPanel.setVisibility(View.VISIBLE);
                }
                else if (integer.equals(NetworkState.FAILED)) {
                    loadingPanel.setVisibility(View.GONE);
                }
            }
        });

        ticketDetailViewModel.getMoveState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer.equals(MoveTicketState.SUCCESS)){
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Move successful", Toast.LENGTH_LONG)
                            .show();
                    finish();
                }else if (integer.equals(MoveTicketState.LOADING)) {
                    loadingPanel.setVisibility(View.VISIBLE);
                }else if (integer.equals(MoveTicketState.BAD_PARAM)){
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Wrong Params - Exiting", Toast.LENGTH_LONG)
                            .show();
                    finish();
                }else if (integer.equals(MoveTicketState.UNAUTHORIZED)){
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Operation not allowed", Toast.LENGTH_LONG)
                            .show();
                }else if (integer.equals(MoveTicketState.NOT_FOUND)){
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Ticket not found - Exiting", Toast.LENGTH_LONG)
                            .show();
                    finish();
                }else if (integer.equals(MoveTicketState.ERROR)) {
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Error in moving ticket - Exiting", Toast.LENGTH_LONG)
                            .show();
                    finish();
                }
            }
        });
    }

    private void setPriority() {
        switch (ticketDetailViewModel.ticket.getPriority()){
            case "LOW":
                background.setColor(Color.GREEN);
                priorityButton.setBackground(background);
                break;
            case "MEDIUM":
                background.setColor(Color.YELLOW);
                priorityButton.setBackground(background);
                break;
            case "HIGH":
                background.setColor(Color.RED);
                priorityButton.setBackground(background);
                break;
        }
    }

    public void loadDepartmentList(View view) {
        ticketDetailViewModel.loadDepartments();
    }

    public void closeTicket(View view) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dark_Dialog);
        builder.setTitle("Close Ticket?");

        // define actions
        builder.setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //call ViewModel close method
                ticketDetailViewModel.closeTicket();
            }
        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        //setting margin on negative button
        Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0,0,20,0);
        negative.setLayoutParams(params);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            negative.setTextColor(getColor(R.color.colorPrimary));
            negative.setBackground(null);
            positive.setTextColor(getColor(R.color.colorPrimary));
            positive.setBackground(null);
        }
    }

    public void moveTicket(){
        //set department observer to idle
        ticketDetailViewModel.setDepartmentLiveData(NetworkState.IDLE);

        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dark_Dialog);
        builder.setTitle("Move Ticket To");

        // add a list
        String[] a = ticketDetailViewModel.departments.toArray(new String[0]);
        builder.setItems(a, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //call cloud function to move ticket to new department
                ticketDetailViewModel.moveTicket(ticketDetailViewModel.departments.get(which).toLowerCase());
            }
        });
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void changeTicketPriority(View view) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dark_Dialog);
        builder.setTitle("Select Priority Level");

        // add a list
        final List<String> levels = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.priority_spinner)));
        levels.remove(0);
        String[] a = levels.toArray(new String[0]);
        builder.setItems(a, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ticketDetailViewModel.ticket.setPriority(levels.get(which));
                setPriority();
                //update ticket in firestore
                Map<String, Object> updateMap = new HashMap<>();

                //saving strings to sharedPreferences
                SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);

                //retrieve staff data
                String creatorFName = prefs.getString("fname", "");
                String creatorLName = prefs.getString("lname", "");
                String creatorDepartment = prefs.getString("department", "");
                //ticket description information
                Map<String, Object> content = new HashMap<>();
                content.put(Ticket.NOTES_BODY, "Priority changed to " + ticketDetailViewModel.ticket.getPriority());
                content.put(Ticket.NOTES_AUTHOR, creatorFName + " " + creatorLName);
                content.put(Ticket.NOTES_DEPARTMENT, creatorDepartment.toLowerCase());

                updateMap.put(Ticket.NOTES, FieldValue.arrayUnion(content));
                updateMap.put(TicketDetailViewModel.PRIORITY_UPDATE_KEY, ticketDetailViewModel.ticket.getPriority());

                ticketDetailViewModel.noteEntryMap = content;
                ticketDetailViewModel.setNote(updateMap);
            }
        });
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showCustomer(View view) {
        if (ticketDetailViewModel.ticket.getCustomer() == null) {
            Toast.makeText(this, "No Customer information", Toast.LENGTH_LONG).show();
            return;
        }
        Customer customer = ticketDetailViewModel.ticket.getCustomer();
        //launch Customer Detail Activity
        Intent intent = new Intent(getApplicationContext(), CustomerDetailActivity.class);
        intent.putExtra("customer", customer);
        startActivity(intent);
    }
}
