package com.dims.fastdesk.ui.client_view.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dims.fastdesk.R
import com.dims.fastdesk.adapters.TicketListPagedAdapter
import com.dims.fastdesk.ui.NoteInputFragment
import com.dims.fastdesk.ui.NoteUpdateInterface
import com.dims.fastdesk.utilities.FirebaseUtils
import com.dims.fastdesk.utilities.NetworkState
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    @Suppress("PrivatePropertyName")
    private val RC_SIGN_IN = 801
    private val recyclerAdapter = TicketListPagedAdapter()

    private lateinit var editDetailButton: ImageButton
    private lateinit var greetingTextView: TextView
    private lateinit var briefTextView: TextView
    private lateinit var complaintRecycler: RecyclerView
    private lateinit var complaintButton: Button
    private lateinit var logoutButton: ImageButton
    private lateinit var loader: ProgressBar
    private lateinit var errorTextView: TextView

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        //sign in if not yet signed in
        mAuth = FirebaseAuth.getInstance()
        attachAuthListener(mAuth)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(view){
            editDetailButton = findViewById(R.id.edit_detail_button)
            greetingTextView = findViewById(R.id.greeting_textview)
            briefTextView = findViewById(R.id.brief_textview)
            complaintButton = findViewById(R.id.complaint_button)
            logoutButton = findViewById(R.id.logout_button)
            complaintRecycler = findViewById(R.id.complaint_recycler)
            loader = findViewById(R.id.progress_bar)
            errorTextView = findViewById(R.id.error_textview)
        }

        complaintRecycler.layoutManager = LinearLayoutManager(requireContext())
        complaintRecycler.adapter = recyclerAdapter

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        viewModel.getPagedListLiveDataAvailable().observe(viewLifecycleOwner, Observer{ isAvailable ->
            if (isAvailable) {

                greetingTextView.text = viewModel.greetingText
                editDetailButton.visibility = View.VISIBLE

                viewModel.ticketPagedListLiveData.observe(viewLifecycleOwner, Observer { tickets ->
                    recyclerAdapter.submitList(tickets)
                })
            }
        })

        viewModel.getDataSourceAvailabilityLiveData().observe(viewLifecycleOwner, Observer { isAvailable ->
            if (isAvailable) {
                viewModel.getDataSourceLiveData().value!!.netStateLiveData.observe(viewLifecycleOwner, Observer { integer ->
                    when(integer){
                        //handle showing and hiding recycler indicators here
                        NetworkState.SUCCESS -> {
                            loader.visibility = View.GONE
                            errorTextView.visibility = View.GONE
                        }
                        NetworkState.LOADING -> {
                            loader.visibility = View.VISIBLE
                            errorTextView.visibility = View.GONE
                        }
                        NetworkState.FAILED -> {
                            loader.visibility = View.GONE
                            errorTextView.visibility = View.VISIBLE
                        }
                    }
                })
            }
        })

        viewModel.getTicketCreatedStatus().observe(viewLifecycleOwner, Observer {
            when(it){
                NetworkState.SUCCESS -> { viewModel.noteEntry.clear() }
                NetworkState.LOADING -> {/*do nothing*/}
                NetworkState.FAILED -> {
                    viewModel.noteEntry.clear()
                    Snackbar.make(requireView(), "Couldn't create ticket.Try again",
                            Snackbar.LENGTH_INDEFINITE
                    ).setAction("DISMISS") {}.show()
                }
            }
        })

        greetingTextView.text = viewModel.greetingText
        viewModel.getComplaintCountLiveData().observe(viewLifecycleOwner, Observer {count ->
            if (count >= 0){
                val brief = "You have $count open complaints"
                briefTextView.text = brief
                briefTextView.isVisible = true
            }
        })
        setButtonListeners()

        if (mAuth.currentUser != null)
            viewModel.load()
    }

    private fun setButtonListeners() {
        logoutButton.setOnClickListener {
            //unsubscribe from department notifications
            val prefs: SharedPreferences = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            mAuth.signOut()
            recyclerAdapter.submitList(null)
            briefTextView.text = ""
        }
        editDetailButton.setOnClickListener {  }
        complaintButton.setOnClickListener {
            val frag = childFragmentManager.findFragmentByTag("fragment_edit_name")
            if (frag != null) {
                childFragmentManager.beginTransaction().remove(frag).commit()
            }
            val noteInputFragment = NoteInputFragment(viewModel as NoteUpdateInterface)
            noteInputFragment.show(childFragmentManager, "fragment_edit_name")
        }
    }

    private fun attachAuthListener(auth: FirebaseAuth){
        mAuthListener = FirebaseAuth.AuthStateListener {
            if (it.currentUser == null)
                startActivityForResult(FirebaseUtils.getSignInIntent(), RC_SIGN_IN)
        }
        auth.addAuthStateListener(mAuthListener)
    }

    @SuppressLint("ApplySharedPref")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode != Activity.RESULT_OK) {
                //The user has pressed the back button or some other error
                Log.d("Log In Issue", response!!.error.toString())
                //todo test and choose one, also move these to a single function serving here and back press
                finishAffinity(requireActivity())
//                Process.killProcess(Process.myPid())
            } else {
                //clear preferences
                val prefs = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.clear().commit()
                //get viewmodel to retrieve ticket info
                viewModel.load()
            }
        }
    }
}