package com.dims.fastdesk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dims.fastdesk.R;
import com.dims.fastdesk.models.Ticket;
import com.google.android.flexbox.FlexboxLayout;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder>{

    private List<Map<String, Object>> notes;

    public NotesAdapter(List<Map<String, Object>> notes){
        this.notes = notes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.ticket_detail_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Map<String, Object> note = this.notes.get(position);

        holder.bodyTextView.setText((String) note.get(Ticket.NOTES_BODY));
        holder.nameTextView.setText((String) note.get(Ticket.NOTES_AUTHOR));
        holder.departmentTextView.setText((String) note.get(Ticket.NOTES_DEPARTMENT));

        @SuppressWarnings("unchecked")
        final List<String> images = (List<String>) note.get(Ticket.NOTES_IMAGES);
        if (images != null){
            if (!images.isEmpty()) {
                holder.ticketImageContainer.setVisibility(View.VISIBLE);
                holder.ticketImage1.setImageDrawable(null);
                holder.ticketImage2.setImageDrawable(null);
                holder.ticketImage3.setImageDrawable(null);

                for (final String imageUrl : images) {
                    switch (images.indexOf(imageUrl)) {
                        case 0:
                            Glide.with(holder.itemView.getContext())
                                    .load(imageUrl)
                                    .fitCenter()
                                    .placeholder(R.drawable.ic_insert_photo)
                                    .into(holder.ticketImage1);
                            //setting listener on imageView
                            holder.ticketImage1.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new StfalconImageViewer.Builder<>(holder.itemView.getContext(), images, getImageLoader())
                                            .withStartPosition(images.indexOf(imageUrl))
                                            .show();
                                }
                            });
                            break;
                        case 1:
                            Glide.with(holder.itemView.getContext())
                                    .load(imageUrl)
                                    .fitCenter()
                                    .placeholder(R.drawable.ic_insert_photo)
                                    .into(holder.ticketImage2);
                            //setting listener on imageView
                            holder.ticketImage2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new StfalconImageViewer.Builder<>(holder.itemView.getContext(), images, getImageLoader())
                                            .withStartPosition(images.indexOf(imageUrl))
                                            .show();
                                }
                            });
                            break;
                        case 2:
                            Glide.with(holder.itemView.getContext())
                                    .load(imageUrl)
                                    .fitCenter()
                                    .placeholder(R.drawable.ic_insert_photo)
                                    .into(holder.ticketImage3);
                            //setting listener on imageView
                            holder.ticketImage3.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new StfalconImageViewer.Builder<>(holder.itemView.getContext(), images, getImageLoader())
                                            .withStartPosition(images.indexOf(imageUrl))
                                            .show();
                                }
                            });
                            break;
                    }
                }
            }
        }else{
            holder.ticketImageContainer.setVisibility(View.GONE);
        }
    }

    public void updateNotes(@NotNull List<Map<String, Object>> newNotes){
        this.notes.clear();
        this.notes.addAll(newNotes);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    private ImageLoader<String> getImageLoader(){
        return new ImageLoader<String>() {
            @Override
            public void loadImage(ImageView imageView, String image) {
                Glide.with(imageView.getContext()).load(image).into(imageView);
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        private TextView bodyTextView, nameTextView, departmentTextView;
        private FlexboxLayout ticketImageContainer;
        private ImageView ticketImage1, ticketImage2, ticketImage3;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            bodyTextView = itemView.findViewById(R.id.bodyTextView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            departmentTextView = itemView.findViewById(R.id.departmentTextView);
            ticketImageContainer = itemView.findViewById(R.id.ticketImageContainer);
            ticketImage1 = itemView.findViewById(R.id.ticketImageView1);
            ticketImage2 = itemView.findViewById(R.id.ticketImageView2);
            ticketImage3 = itemView.findViewById(R.id.ticketImageView3);
        }
    }
}
