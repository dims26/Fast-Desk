package com.dims.fastdesk.ui.client_view.complaint_detail

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dims.fastdesk.R
import com.dims.fastdesk.adapters.ComplaintListAdapter
import com.dims.fastdesk.ui.NoteInputFragment
import com.dims.fastdesk.utilities.NetworkState
import com.dims.fastdesk.viewmodels.TicketDetailViewModel
import com.dims.fastdesk.viewmodels.ViewModelFactory
import com.google.android.material.snackbar.Snackbar

class ComplaintDetailFragment : Fragment() {

    private lateinit var titleTextView: TextView
    private lateinit var notesRecycler: RecyclerView
    private lateinit var addNoteButton: Button

    private lateinit var viewModel: TicketDetailViewModel
    private lateinit var adapter: ComplaintListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_complaint_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(view){
            notesRecycler = findViewById(R.id.notes_list_recycler)
            addNoteButton = findViewById(R.id.add_note_button)
            titleTextView = findViewById(R.id.title_text_view)
        }

        val myArgs = ComplaintDetailFragmentArgs.fromBundle(
                arguments ?: error("no arguments")
        )

        val factory = ViewModelFactory(requireActivity().application, Activity(), myArgs.ticket)
        viewModel = ViewModelProvider(this, factory).get(TicketDetailViewModel::class.java)
        viewModel.isCustomerView = true
        viewModel.registerTicketChangeListener()

        adapter = ComplaintListAdapter(requireContext(), viewModel.ticket.notes)
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        layoutManager.stackFromEnd = true
        notesRecycler.layoutManager = layoutManager
        notesRecycler.adapter = adapter

        titleTextView.text = viewModel.ticket.title

        addNoteButton.setOnClickListener {
            val manager: FragmentManager = childFragmentManager
            val frag = manager.findFragmentByTag("fragment_edit_name")
            if (frag != null) {
                manager.beginTransaction().remove(frag).commit()
            }

            val noteInputFragment = NoteInputFragment(viewModel)
            noteInputFragment.show(manager, "fragment_edit_name")
        }

        setObservers()
    }

    private fun setObservers() {
        // used for note uploads, not ticket creation. The livedata and its accessors should be renamed to reflect that
        viewModel.getTicketCreatedStatus().observe(viewLifecycleOwner, Observer { integer ->
            when (integer) {
                NetworkState.SUCCESS, NetworkState.FAILED -> {
                    viewModel.noteEntry.clear()
                }
                NetworkState.LOADING -> {
                    /*Might show loading indicator here*/
                }
            }
        })

        viewModel.ticketUpdatedStatus.observe(viewLifecycleOwner, Observer { integer ->
            when (integer) {
                NetworkState.SUCCESS -> {
                    titleTextView.text = viewModel.ticket.title
                    adapter.updateNotes(viewModel.ticket.notes)
                    notesRecycler.scrollToPosition(adapter.itemCount - 1)
                }
                NetworkState.NOT_FOUND -> {
                    viewModel.noteEntry.clear()
                    Snackbar.make(requireActivity().window.decorView.rootView, "Ticket might have been closed",
                            Snackbar.LENGTH_SHORT
                    ).show()
                    NavHostFragment.findNavController(this).popBackStack()
                }
                NetworkState.FAILED -> {
                    viewModel.noteEntry.clear()
                    Snackbar.make(requireActivity().window.decorView.rootView, "Could not add note, try again",
                            Snackbar.LENGTH_INDEFINITE
                    ).setAction("DISMISS") {}.show()
                }
            }
        })
    }
}