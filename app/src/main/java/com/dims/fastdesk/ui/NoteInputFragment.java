package com.dims.fastdesk.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;
import com.dims.fastdesk.R;
import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.utilities.ImageUploadState;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class NoteInputFragment extends DialogFragment implements View.OnClickListener {

    private NoteUpdateInterface updater;
    private static final int REQUEST_CODE_CHOOSE = 442;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 433;
    private TextInputEditText mEditText;
    private ImageButton attachmentButton;
    private Button submitButton;
    private List<Uri> mSelected;
    private List<Integer> ids;
    private RelativeLayout progressLayout;
    private ProgressBar determinateProgressBar;
    private TextView imageNumberTextView, totalImageCountTextView;

    public NoteInputFragment(NoteUpdateInterface updater){ this.updater = updater; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input_note, container);
        mEditText = view.findViewById(R.id.noteEditText);
        submitButton = view.findViewById(R.id.submitButton);
        attachmentButton = view.findViewById(R.id.attachmentButton);
        progressLayout = view.findViewById(R.id.progressLayout);
        determinateProgressBar = view.findViewById(R.id.determinateProgressBar);
        imageNumberTextView = view.findViewById(R.id.imageNumberTextView);
        totalImageCountTextView = view.findViewById(R.id.totalImageCountTextView);
        mSelected = new ArrayList<>();

        submitButton.setOnClickListener(this);
        attachmentButton.setOnClickListener(this);
        progressLayout.setVisibility(View.GONE);

        ids = Arrays.asList(R.id.imageView1, R.id.imageView2, R.id.imageView3);

        //prevent cancelling the DialogFragment by touching outside it's bounds
        getDialog().setCanceledOnTouchOutside(false);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        //update progressBar
        updater.getImageUploadProgress().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer >= 0){
                    //update progressbar with value of integer
                    determinateProgressBar.setProgress(integer);
                }
            }
        });

        //update current upload info
        updater.getImageUploadProgressBar().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer.equals(ImageUploadState.SUCCESS)){
                    //upload ticket
                    updateTicket();
                }else if (integer.equals(ImageUploadState.LOADING)){
                    progressLayout.setVisibility(View.VISIBLE);
                    determinateProgressBar.setProgress(0);
                    totalImageCountTextView.setText(String.valueOf(mSelected.size()));
                    imageNumberTextView.setText("1");
                }else if (integer > 0){
                    imageNumberTextView.setText(String.valueOf(integer));
                }
            }
        });
    }

    public void onResume() {
        // Store access variables for window and blank point
        Window window = getDialog().getWindow();
        Point size = new Point();
        // Store dimensions of the screen in `size`
        Display display = window.getWindowManager().getDefaultDisplay();
        display.getSize(size);
        // Set the width of the dialog to match the screen width
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        // Call super onResume after sizing
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.submitButton:
                if (!mEditText.getText().toString().trim().isEmpty()) {
                    submitButton.setEnabled(false);
                    attachmentButton.setEnabled(false);
                    mEditText.setEnabled(false);
                    uploadImages();
                }
                break;
            case R.id.attachmentButton:
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted, request for permission
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        Toast.makeText(getActivity(), R.string.permission_rationale, Toast.LENGTH_LONG)
                                .show();
                    }
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_STORAGE);
                }else {
                    Matisse.from(this)
                            .choose(MimeType.ofImage())
                            .countable(true)
                            .maxSelectable(3)
                            .thumbnailScale(0.85f)
                            .imageEngine(new GlideEngine())
                            .theme(R.style.Matisse_Dracula)
                            .forResult(REQUEST_CODE_CHOOSE);
                }
                break;
        }
    }


    private void uploadImages() {
        if (mSelected.isEmpty()){
            updateTicket();
            return;
        }
        //upload images to firebaseStorage
        updater.uploadImages(mSelected);
    }

    private void updateTicket() {
        //then update ticket with the urls
        //saving strings to sharedPreferences
        SharedPreferences prefs = getActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);

        //retrieve staff data
        String creatorFName = prefs.getString("fname", "");
        String creatorLName = prefs.getString("lname", "");
        String creatorDepartment = prefs.getString("department", "");

        //create note map
        Map<String, Object> note = new HashMap<>();

        //ticket description information
        Map<String, Object> content = new HashMap<>();
        content.put(Ticket.NOTES_BODY, mEditText.getText().toString());
        content.put(Ticket.NOTES_AUTHOR, creatorFName + " " + creatorLName);
        content.put(Ticket.NOTES_DEPARTMENT, creatorDepartment.toLowerCase());
        if (!mSelected.isEmpty()) {
            content.put(Ticket.NOTES_IMAGES, updater.getImageDownloadUriList());
        }

        note.put(Ticket.NOTES, FieldValue.arrayUnion(content));

        //hide progressBar and associated views, and set progress listeners to idle
        progressLayout.setVisibility(View.GONE);
        updater.setImageUploadProgressBar(ImageUploadState.IDLE);//important

        updater.setNoteEntry(content);
        updater.updateTicket(note);

        this.dismiss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            assert data != null;
            mSelected = Matisse.obtainResult(data);
            for (Integer id : ids){
                Glide
                        .with(this)
                        .load(R.drawable.ic_insert_photo)
                        .fitCenter()
                        .into((ImageView) getView().findViewById(id));
            }
            for (int i = 0; i < mSelected.size(); i++ ){
                Glide
                        .with(this)
                        .load(mSelected.get(i))
                        .fitCenter()
                        .placeholder(R.drawable.ic_insert_photo)
                        .into((ImageView) getView().findViewById(ids.get(i)));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                Matisse.from(this)
                        .choose(MimeType.ofImage())
                        .countable(true)
                        .maxSelectable(3)
                        .thumbnailScale(0.85f)
                        .imageEngine(new GlideEngine())
                        .theme(R.style.Matisse_Dracula)
                        .forResult(REQUEST_CODE_CHOOSE);
            }
        }
    }
}
