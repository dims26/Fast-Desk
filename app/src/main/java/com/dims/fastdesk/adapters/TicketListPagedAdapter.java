package com.dims.fastdesk.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.dims.fastdesk.R;
import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.ui.TicketDetailActivity;

import java.text.DateFormat;

public class TicketListPagedAdapter extends PagedListAdapter<Ticket, TicketListPagedAdapter.TicketViewHolder> {
    public TicketListPagedAdapter(){
        super(DIFF_CALLBACK);
    }
    public boolean isClosedTicket = false;
    public boolean isFromCustomerDetail = false;

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.ticket_item, parent, false);
        return new TicketViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        //Get current item from pagedList
        final Ticket ticket = getItem(position);

        if (ticket != null){
            holder.customerNameTextView.setText(ticket.getCustomerName());
            holder.titleTextview.setText(ticket.getTitle());
            holder.descriptionTextView.setText((String)ticket.getNotes().get(0).get(Ticket.NOTES_BODY));

            DateFormat dateFormat = DateFormat.getDateTimeInstance();
            holder.dateTextView.setText(dateFormat.format(ticket.getDate()));
        }

        //set up click listener for view
        holder.ticketView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), TicketDetailActivity.class);
                intent.putExtra("ticket", ticket);
                if (isClosedTicket) intent.putExtra("isClosedTicket", true);
                if (isFromCustomerDetail) intent.putExtra("isFromCustomerDetail", true);
                v.getContext().startActivity(intent);
            }
        });
    }

    private static DiffUtil.ItemCallback<Ticket> DIFF_CALLBACK = new DiffUtil.ItemCallback<Ticket>() {
        @Override
        public boolean areItemsTheSame(@NonNull Ticket oldItem, @NonNull Ticket newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Ticket oldItem, @NonNull Ticket newItem) {
            return oldItem.getNotes().equals(newItem.getNotes());
        }
    };

    class TicketViewHolder extends RecyclerView.ViewHolder{
        private final CardView ticketView;
        private final TextView customerNameTextView, dateTextView, titleTextview, descriptionTextView;
        TicketViewHolder(@NonNull View itemView) {
            super(itemView);

            ticketView = itemView.findViewById(R.id.ticket);

            customerNameTextView = itemView.findViewById(R.id.nameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            titleTextview = itemView.findViewById(R.id.titleTextView);
            descriptionTextView = itemView.findViewById(R.id.brief_textview);
        }
    }
}
