package com.dims.fastdesk.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.dims.fastdesk.R;
import com.dims.fastdesk.utilities.NetworkState;
import com.dims.fastdesk.viewmodels.NewTicketViewModel;
import com.dims.fastdesk.viewmodels.ViewModelFactory;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class NewTicketActivity extends AppCompatActivity {

    private EditText titleEditText, descriptionEditText;
    private TextView customerNameTextView;
    private Spinner prioritySpinner, departmentSpinner;

    private RelativeLayout loadingPanel;

    final int MY_REQUEST_CODE = 419;


    ArrayAdapter<String> dataAdapter;
    private NewTicketViewModel newTicketViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_ticket);

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        customerNameTextView = findViewById(R.id.nameTextView);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        departmentSpinner = findViewById(R.id.departmentSpinner);
        loadingPanel = findViewById(R.id.loadingPanel);

        ViewModelFactory factory = new ViewModelFactory(getApplication());
        newTicketViewModel = new ViewModelProvider(this, factory).get(NewTicketViewModel.class);

        dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, newTicketViewModel.departments);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        departmentSpinner.setAdapter(dataAdapter);

        //loading data from savedInstance state
        if (savedInstanceState != null){
            titleEditText.setText(savedInstanceState.getString("TITLE"));
            descriptionEditText.setText(savedInstanceState.getString("DESCRIPTION"));
            customerNameTextView.setText(savedInstanceState.getString("CUSTOMER_NAME"));
            prioritySpinner.setSelection(savedInstanceState.getInt("PRIORITY", 0));
            if (newTicketViewModel.departments.size()>0)
                departmentSpinner.setSelection(savedInstanceState.getInt("DEPARTMENT", 0));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        newTicketViewModel.getDepartments().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    //refresh departmentSpinner and remove loadingPanel
                    dataAdapter.notifyDataSetChanged();
                    newTicketViewModel.setTicketCreatedStatus(NetworkState.FAILED);
                }
            }
        });

        newTicketViewModel.getTicketCreatedStatus().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer.equals(NetworkState.SUCCESS)){
                    loadingPanel.setVisibility(View.GONE);
                    finish();
                }
                else if (integer.equals(NetworkState.LOADING)) {
                    loadingPanel.setVisibility(View.VISIBLE);
                }
                else if (integer.equals(NetworkState.FAILED)) {
                    loadingPanel.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == MY_REQUEST_CODE && resultCode == RESULT_OK) {
            //get the extras from the intent
            customerNameTextView.setText(intent.getStringExtra("name"));
            newTicketViewModel.customerReference = FirebaseFirestore.getInstance().document(Objects.requireNonNull(intent.getStringExtra("path")));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("TITLE", titleEditText.getText().toString());
        outState.putString("DESCRIPTION", descriptionEditText.getText().toString());
        outState.putString("CUSTOMER_NAME", customerNameTextView.getText().toString());
        outState.putInt("PRIORITY", prioritySpinner.getSelectedItemPosition());
        outState.putInt("DEPARTMENT", departmentSpinner.getSelectedItemPosition());
        super.onSaveInstanceState(outState);
    }

    public void getCustomer(View view) {
        Intent intent = new Intent();
        intent.setClassName(this,CustomerListActivity.class.getName());
        intent.putExtra("isForSelection", true);
        startActivityForResult(intent, MY_REQUEST_CODE);
    }

    public void createTicket(View view) {
        //collect all data and create ticket
        AlertDialog.Builder createTicketDialogBuilder = new AlertDialog.Builder(this, R.style.AppTheme_Dark_Dialog);
        //end activity and send data to the calling activity
        createTicketDialogBuilder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //call createTicket() in ViewModel
                newTicketViewModel.createTicket(
                        titleEditText.getText().toString(),
                        descriptionEditText.getText().toString(),
                        (String) prioritySpinner.getSelectedItem(),
                        newTicketViewModel.departments.get(departmentSpinner.getSelectedItemPosition())
                );
            }
        });
        //dismiss the dialog
        createTicketDialogBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        createTicketDialogBuilder.setTitle("Create Ticket?");
        AlertDialog dialog = createTicketDialogBuilder.create();
        dialog.show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            negative.setTextColor(getColor(R.color.colorPrimary));
            negative.setBackground(null);
            positive.setTextColor(getColor(R.color.colorPrimary));
            positive.setBackground(null);
        }
    }
}