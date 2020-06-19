package com.dims.fastdesk.adapters;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.dims.fastdesk.R;
import com.dims.fastdesk.models.Customer;
import com.dims.fastdesk.ui.CustomerDetailActivity;
import com.dims.fastdesk.ui.CustomerListActivity;

import static android.app.Activity.RESULT_OK;

public class CustomerListPagedAdapter extends PagedListAdapter<Customer, CustomerListPagedAdapter.CustomerViewHolder> {

    private CustomerListActivity customerListActivity;
    public Boolean isForSelection = false;

    public CustomerListPagedAdapter(CustomerListActivity customerListActivity){
        super(DIFF_CALLBACK);
        this.customerListActivity = customerListActivity;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.customer_item, parent, false);
        return new CustomerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final CustomerViewHolder holder, final int position) {
        //Get current item from pagedList
        final Customer customer = getItem(position);

        //bind data to views
        if (customer != null){
            holder.addressTextView.setText(customer.getAddress());
            holder.customerNameTextView.setText(customer.getName());
            holder.emailTextView.setText(customer.getEmail());
            holder.idTextView.setText(customer.getId());

        }

        //set up click listener for view
        holder.customerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isForSelection){
                    //create confirmation dialog
                    AlertDialog.Builder selectDialogBuilder = new AlertDialog.Builder(customerListActivity, R.style.AppTheme_Dark_Dialog);
                    //end activity and send data to the calling activity
                    selectDialogBuilder.setPositiveButton("SELECT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.putExtra("path", getCurrentList().get(position).getPath());
                            intent.putExtra("name", getCurrentList().get(position).getName());
                            customerListActivity.setResult(RESULT_OK, intent);
                            customerListActivity.finish();
                        }
                    });
                    //dismiss the dialog
                    selectDialogBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    selectDialogBuilder.setTitle("Select \"" + getCurrentList().get(position).getName() +
                            "\"?");
                    AlertDialog dialog = selectDialogBuilder.create();
                    dialog.show();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                        Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                        negative.setTextColor(customerListActivity.getColor(R.color.colorPrimary));
                        negative.setBackground(null);
                        positive.setTextColor(customerListActivity.getColor(R.color.colorPrimary));
                        positive.setBackground(null);
                    }
                }else{
                    //launch Customer Detail Activity
                    Intent intent = new Intent(v.getContext(), CustomerDetailActivity.class);
                    intent.putExtra("customer", customer);
                    v.getContext().startActivity(intent);
                }
            }
        });
    }

    private static DiffUtil.ItemCallback<Customer> DIFF_CALLBACK = new DiffUtil.ItemCallback<Customer>() {
        @Override
        public boolean areItemsTheSame(@NonNull Customer oldItem, @NonNull Customer newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Customer oldItem, @NonNull Customer newItem) {
            return oldItem.equals(newItem);
        }
    };

    class CustomerViewHolder extends RecyclerView.ViewHolder{
        private View customerView;
        private final TextView emailTextView, idTextView, customerNameTextView, addressTextView;
        CustomerViewHolder(@NonNull View itemView) {
            super(itemView);

            customerView = itemView;

            addressTextView = itemView.findViewById(R.id.addressTextView);
            customerNameTextView = itemView.findViewById(R.id.nameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            idTextView = itemView.findViewById(R.id.idTextView);
        }
    }
}
