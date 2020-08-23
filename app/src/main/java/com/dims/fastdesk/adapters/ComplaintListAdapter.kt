package com.dims.fastdesk.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dims.fastdesk.R
import com.dims.fastdesk.models.Ticket
import com.google.android.flexbox.FlexboxLayout
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import java.util.*


@Suppress("PrivatePropertyName")
class ComplaintListAdapter(private val context: Context, private var notes: MutableList<Map<String, Any>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_MESSAGE_SENT = 1
    private val VIEW_TYPE_MESSAGE_RECEIVED = 2

    override fun getItemCount(): Int = notes.size

    // Determines the appropriate ViewType according to the sender of the message.
    override fun getItemViewType(position: Int): Int {
        val note: Map<String, Any> = this.notes[position]
        val department = note.getOrElse(Ticket.NOTES_DEPARTMENT){""} as String
        return if (department.toLowerCase(Locale.ROOT) == "customer" ) {
            // If the current user created the note
            VIEW_TYPE_MESSAGE_SENT
        } else {
            // If note comes from staff
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == VIEW_TYPE_MESSAGE_SENT){
            SentMessageHolder(
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.sent_message_item, parent, false)
            )
        } else {
            ReceivedMessageHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.received_message_item, parent, false)
            )
        }
    }

    // Passes the note to a ViewHolder so that the contents can be bound to UI.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val note: Map<String, Any> = notes[position]
        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageHolder).bind(note)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageHolder).bind(note)
        }
    }

    private fun getImageLoader(): ImageLoader<String?>? {
        return ImageLoader { imageView, image -> Glide.with(imageView.context).load(image).into(imageView) }
    }

    fun updateNotes(newNotes: List<Map<String, Any>>) {
        this.notes.clear()
        this.notes.addAll(newNotes)
        this.notifyDataSetChanged()
    }

    private fun loadInto(url: String, imageView: ImageView, index: Int, images: List<String>) {
        Glide.with(context)
                .load(url)
                .fitCenter()
                .placeholder(R.drawable.ic_insert_photo)
                .into(imageView)
        //setting listener on imageView
        imageView.setOnClickListener {
            StfalconImageViewer.Builder(context, images, getImageLoader())
                    .withStartPosition(index)
                    .show()
        }
    }

    private inner class SentMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val bodyTextView: TextView = itemView.findViewById(R.id.text_message_body)
        private val ticketImageContainer: FlexboxLayout = itemView.findViewById(R.id.ticketImageContainer)
        private val ticketImage1: ImageView = itemView.findViewById(R.id.ticketImageView1)
        private val ticketImage2: ImageView = itemView.findViewById(R.id.ticketImageView2)
        private val ticketImage3: ImageView = itemView.findViewById(R.id.ticketImageView3)

        fun bind(note: Map<String, Any>) {
            bodyTextView.text = note[Ticket.NOTES_BODY] as String?

            @Suppress("UNCHECKED_CAST")
            val images = note[Ticket.NOTES_IMAGES] as List<String>?
            if (!images.isNullOrEmpty()){
                ticketImageContainer.visibility = View.VISIBLE

                images.forEach {
                    when(images.indexOf(it)){
                        0 -> { loadInto(it, ticketImage1, 0, images) }
                        1 -> { loadInto(it, ticketImage2, 1, images) }
                        2 -> { loadInto(it, ticketImage3, 2, images) }
                    }
                }
            } else { ticketImageContainer.visibility = View.GONE }
        }
    }

    private inner class ReceivedMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val bodyTextView: TextView = itemView.findViewById(R.id.text_message_body)
        private val nameTextView:TextView = itemView.findViewById(R.id.name_text_view)
        private val departmentTextView:TextView = itemView.findViewById(R.id.department_text_view)

        private val ticketImageContainer: FlexboxLayout = itemView.findViewById(R.id.ticketImageContainer)
        private val ticketImage1: ImageView = itemView.findViewById(R.id.ticketImageView1)
        private val ticketImage2: ImageView = itemView.findViewById(R.id.ticketImageView2)
        private val ticketImage3: ImageView = itemView.findViewById(R.id.ticketImageView3)

        fun bind(note: Map<String, Any>) {
            bodyTextView.text = note[Ticket.NOTES_BODY] as String?
            nameTextView.text = note[Ticket.NOTES_AUTHOR] as String?
            departmentTextView.text = note[Ticket.NOTES_DEPARTMENT] as String?

            @Suppress("UNCHECKED_CAST")
            val images = note[Ticket.NOTES_IMAGES] as List<String>?
            if (!images.isNullOrEmpty()){
                ticketImageContainer.visibility = View.VISIBLE

                images.forEach {
                    when(images.indexOf(it)){
                        0 -> { loadInto(it, ticketImage1, 0, images) }
                        1 -> { loadInto(it, ticketImage2, 1, images) }
                        2 -> { loadInto(it, ticketImage3, 2, images) }
                    }
                }
            } else { ticketImageContainer.visibility = View.GONE }
        }
    }
}