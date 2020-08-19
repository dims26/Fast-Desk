package com.dims.fastdesk.utilities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.paging.PageKeyedDataSource;

import com.dims.fastdesk.datasource.CustomerDataSource;
import com.dims.fastdesk.datasource.TicketDataSource;
import com.dims.fastdesk.models.Customer;
import com.dims.fastdesk.models.Ticket;
import com.dims.fastdesk.ui.NoteUpdateInterface;
import com.dims.fastdesk.ui.client_view.home.HomeViewModel;
import com.dims.fastdesk.viewmodels.ClosedTicketsViewModel;
import com.dims.fastdesk.viewmodels.CustomerTicketsViewModel;
import com.dims.fastdesk.viewmodels.NewTicketViewModel;
import com.dims.fastdesk.viewmodels.TicketDetailViewModel;
import com.dims.fastdesk.viewmodels.TicketsListViewModel;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FirebaseUtils {

    private static FirebaseAuth mFirebaseAuth;
    private static FirebaseAuth.AuthStateListener mAuthListener;
    private static final int RC_SIGN_IN = 801;
    private static final String TICKET_STORE = "ticket_store";
    private static Query.Direction direction = Query.Direction.DESCENDING;
    private static StorageReference mTicketStorageReference;
    private static StorageReference mCustomerStorageReference;

    private FirebaseUtils(){ }

    public static synchronized void openFirebaseReference(final Activity callerActivity){
            mFirebaseAuth = FirebaseAuth.getInstance();
            //listener, if attached to FirebaseAuth, will call the sign in page if user isn't logged in
            mAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    if (firebaseAuth.getCurrentUser() == null) {
                        callerActivity.startActivityForResult(getSignInIntent(), RC_SIGN_IN);
                    }
                    String userId = firebaseAuth.getUid();
//                    checkAdmin(userId);
                }
            };

        connectStorage();
    }

    public static void initNoLogin(){
        mFirebaseAuth = FirebaseAuth.getInstance();
        mTicketStorageReference = FirebaseStorage.getInstance().getReference().child(TICKET_STORE);
    }

    private static void connectStorage(){
        FirebaseStorage mFirebaseStorage = FirebaseStorage.getInstance();
        mTicketStorageReference = mFirebaseStorage.getReference().child(TICKET_STORE);
        mCustomerStorageReference = mFirebaseStorage.getReference().child("customer_store");
    }

    public static Intent getSignInIntent(){
        //select providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().setAllowNewAccounts(false).build()
        );

        return AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build();
    }

    public static void pullStaffTicketsDataSource(final PageKeyedDataSource.LoadInitialCallback<Query, Ticket> initialCallback,
                                                  final Query ticketQuery,
                                                  boolean newerFirst, final TicketDataSource source){
        //set direction
        if (newerFirst) {
            direction = Query.Direction.DESCENDING;
        }else {
            direction = Query.Direction.ASCENDING;
        }
        final List<Ticket> ticketList = new ArrayList<>();
        ticketQuery
                .orderBy("creator.time", direction)
                .limit(6)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            final DocumentSnapshot lastDoc;
                            final Query nextQuery;
                            final List<DocumentSnapshot> documentSnapshotList = task.getResult().getDocuments();
                            if (documentSnapshotList.isEmpty())
                                source.netStateLiveData.postValue(NetworkState.SUCCESS);

                            //create nextQuery
                            GetQuery getQuery = new GetQuery(documentSnapshotList, ticketQuery, direction).invoke();
                            lastDoc = getQuery.getLastDoc();
                            nextQuery = getQuery.getNextQuery();

                            for (final QueryDocumentSnapshot document: task.getResult()) {
                                //extract initial data from document
                                final Ticket ticket = extractTicketData(document, new Ticket());

                                //Getting the customer reference within the ticket document
                                DocumentReference customerReference = document.getDocumentReference("customer");
                                customerReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()){
                                            DocumentSnapshot result = task.getResult();
                                            if (result.exists()){
                                                Customer customer = extractCustomerData(result, new Customer());
                                                ticket.setCustomerName(
                                                        result.getString("fname")
                                                                + " "
                                                                + result.getString("lname"));
                                                ticket.setCustomer(customer);
                                                //add to ticketList
                                                ticketList.add(ticket);
                                                if (ticketList.size() == documentSnapshotList.size()) {
                                                    Collections.sort(ticketList, new Comparator<Ticket>() {
                                                        @Override
                                                        public int compare(Ticket o1, Ticket o2) {
                                                            return o1.getDate().compareTo(o2.getDate());
                                                        }
                                                    });
                                                    if (direction == Query.Direction.DESCENDING)
                                                        Collections.reverse(ticketList);

                                                    source.netStateLiveData.postValue(NetworkState.SUCCESS);
                                                    initialCallback.onResult(new ArrayList<>(ticketList),0, ticketList.size(), null, nextQuery);
                                                }
                                            }else{
                                                //No such document
                                                ticket.setCustomerName("No customer");
                                                ticket.setCustomer(null);
                                                ticketList.add(ticket);
//                                                document.equals(lastDoc)
                                                if (ticketList.size() == documentSnapshotList.size()) {
                                                    Collections.sort(ticketList, new Comparator<Ticket>() {
                                                        @Override
                                                        public int compare(Ticket o1, Ticket o2) {
                                                            return o1.getDate().compareTo(o2.getDate());
                                                        }
                                                    });
                                                    if (direction == Query.Direction.DESCENDING)
                                                        Collections.reverse(ticketList);

                                                    source.netStateLiveData.postValue(NetworkState.SUCCESS);
                                                    initialCallback.onResult(new ArrayList<>(ticketList),0, ticketList.size(), null, nextQuery);

                                                }
                                            }
                                        }else{
                                            ticket.setCustomerName("Unable to retrieve customer");
                                            ticket.setCustomer(null);
                                            ticketList.add(ticket);
                                            if (ticketList.size() == documentSnapshotList.size()) {
                                                Collections.sort(ticketList, new Comparator<Ticket>() {
                                                    @Override
                                                    public int compare(Ticket o1, Ticket o2) {
                                                        return o1.getDate().compareTo(o2.getDate());
                                                    }
                                                });
                                                if (direction == Query.Direction.DESCENDING)
                                                    Collections.reverse(ticketList);

                                                source.netStateLiveData.postValue(NetworkState.SUCCESS);
                                                initialCallback.onResult(new ArrayList<>(ticketList),0, ticketList.size(), null, nextQuery);
                                            }
                                        }
                                    }
                                });
                            }
                        }else{
                            source.netStateLiveData.postValue(NetworkState.FAILED);
                            initialCallback.onResult(new ArrayList<>(ticketList),0, ticketList.size(), null, null);
                        }
                    }
                });
    }

    public static void pullRemainingStaffTicketsDataSource(
            final PageKeyedDataSource.LoadCallback<Query, Ticket> callback,
            final Query ticketQuery, final TicketDataSource source){
        final List<Ticket> ticketList = new ArrayList<>();
        ticketQuery
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            final DocumentSnapshot lastDoc;
                            final Query nextQuery;
                            final List<DocumentSnapshot> documentSnapshotList = task.getResult().getDocuments();
                            if (documentSnapshotList.isEmpty())
                                source.netStateLiveData.postValue(NetworkState.SUCCESS);

                            //get last visible document in query and build Query of the next page
                            GetQuery getQuery = new GetQuery(documentSnapshotList, ticketQuery).invoke();
                            lastDoc = getQuery.getLastDoc();
                            nextQuery = getQuery.getNextQuery();

                            for (final QueryDocumentSnapshot document: task.getResult()) {
                                final Ticket ticket = extractTicketData(document, new Ticket());

                                //Getting the customer reference within the ticket document
                                DocumentReference customerReference = document.getDocumentReference("customer");
                                customerReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()){
                                            DocumentSnapshot result = task.getResult();
                                            if (result.exists()){
                                                Customer customer = extractCustomerData(result, new Customer());
                                                ticket.setCustomerName(
                                                        result.getString(Customer.FNAME)
                                                                + " "
                                                                + result.getString(Customer.LNAME));
                                                ticket.setCustomer(customer);
                                                //add to ticketList
                                                ticketList.add(ticket);
                                                if (ticketList.size() == documentSnapshotList.size()) {
                                                    Collections.sort(ticketList, new Comparator<Ticket>() {
                                                        @Override
                                                        public int compare(Ticket o1, Ticket o2) {
                                                            return o1.getDate().compareTo(o2.getDate());
                                                        }
                                                    });
                                                    if (direction == Query.Direction.DESCENDING)
                                                        Collections.reverse(ticketList);

                                                    source.netStateLiveData.postValue(NetworkState.SUCCESS);
                                                    callback.onResult(new ArrayList<>(ticketList), nextQuery);
                                                }
                                            }else{
                                                //No such document
                                                ticket.setCustomerName("No customer");
                                                ticket.setCustomer(null);
                                                ticketList.add(ticket);
                                                if (ticketList.size() == documentSnapshotList.size()) {
                                                    Collections.sort(ticketList, new Comparator<Ticket>() {
                                                        @Override
                                                        public int compare(Ticket o1, Ticket o2) {
                                                            return o1.getDate().compareTo(o2.getDate());
                                                        }
                                                    });
                                                    if (direction == Query.Direction.DESCENDING)
                                                        Collections.reverse(ticketList);

                                                    source.netStateLiveData.postValue(NetworkState.SUCCESS);
                                                    callback.onResult(new ArrayList<>(ticketList), nextQuery);
                                                }
                                            }
                                        }else{
                                            ticket.setCustomerName("Unable to retrieve customer");
                                            ticket.setCustomer(null);
                                            ticketList.add(ticket);
                                            if (ticketList.size() == documentSnapshotList.size()) {
                                                Collections.sort(ticketList, new Comparator<Ticket>() {
                                                    @Override
                                                    public int compare(Ticket o1, Ticket o2) {
                                                        return o1.getDate().compareTo(o2.getDate());
                                                    }
                                                });
                                                if (direction == Query.Direction.DESCENDING)
                                                    Collections.reverse(ticketList);

                                                source.netStateLiveData.postValue(NetworkState.SUCCESS);
                                                callback.onResult(new ArrayList<>(ticketList), nextQuery);
                                            }
                                        }
                                    }
                                });
                            }
                        }else{
                            source.netStateLiveData.postValue(NetworkState.FAILED);
                            callback.onResult(new ArrayList<Ticket>(), null);
                        }
                    }
                });
    }

    public static void pullCustomerData(final PageKeyedDataSource.LoadInitialCallback<Query, Customer> initialCallback,
                                        final String term, final CustomerDataSource source){

        final List<Customer> customerList = new ArrayList<>();
        //creating query
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final Query customerQuery;
        //check if there is a search term
        if (term != null)
            customerQuery = db.collection("customers").whereArrayContains("key", term.toLowerCase());
        else
            customerQuery = db.collection("customers");

        customerQuery
                .limit(10)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            final Query nextQuery;
                            final List<DocumentSnapshot> documentSnapshotList = task.getResult().getDocuments();
                            if (documentSnapshotList.isEmpty())
                                source.netStateLiveData.postValue(NetworkState.SUCCESS);

                            //create nextQuery
                            GetQuery getQuery = new GetQuery(documentSnapshotList, customerQuery).invoke();
                            nextQuery = getQuery.getNextQuery();

                            for (final QueryDocumentSnapshot document : task.getResult()) {
                                //extract customer data from document
                                Customer customer = extractCustomerData(document, new Customer());

                                //add to list of results
                                customerList.add(customer);
                                if (customerList.size() == documentSnapshotList.size()) {
                                    source.netStateLiveData.postValue(NetworkState.SUCCESS);
                                    initialCallback.onResult(new ArrayList<>(customerList), 0, customerList.size(), null, nextQuery);
                                }
                            }
                        } else {
                            //handle unsuccessful retrieval
                            source.netStateLiveData.postValue(NetworkState.FAILED);
                            initialCallback.onResult(new ArrayList<>(customerList),0, customerList.size(), null, null);
                        }
                    }
                });
    }

    public static void pullRemainingCustomerData(
            final PageKeyedDataSource.LoadCallback<Query, Customer> callback,
            final Query customerQuery, final CustomerDataSource source){
        final List<Customer> customerList = new ArrayList<>();
        customerQuery
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            final Query nextQuery;
                            final List<DocumentSnapshot> documentSnapshotList = task.getResult().getDocuments();
                            if (documentSnapshotList.isEmpty())
                                source.netStateLiveData.postValue(NetworkState.SUCCESS);

                            //get last visible document in query and build Query of the next page
                            GetQuery getQuery = new GetQuery(documentSnapshotList, customerQuery).invoke();
                            nextQuery = getQuery.getNextQuery();

                            for (final QueryDocumentSnapshot document: task.getResult()) {
                                Customer customer = extractCustomerData(document, new Customer());

                                customerList.add(customer);
                                if (customerList.size() == documentSnapshotList.size()){
                                    source.netStateLiveData.postValue(NetworkState.SUCCESS);
                                    callback.onResult(new ArrayList<>(customerList), nextQuery);
                                }
                                else{
                                    source.netStateLiveData.postValue(NetworkState.FAILED);
                                    callback.onResult(new ArrayList<Customer>(), null);
                                }
                            }
                        }
                    }
                });
    }

    public static void attachListener(){
        mFirebaseAuth.addAuthStateListener(mAuthListener);
    }
    public static void detachListener(){
        mFirebaseAuth.removeAuthStateListener(mAuthListener);
    }

    private static Ticket extractTicketData(DocumentSnapshot document, Ticket ticket) {
        ticket.setTitle(document.getString("title"));
        ticket.setId(document.getId());
        ticket.setPath(document.getReference().getPath());

        List<Map<String, Object>> notes = (List<Map<String, Object>>) document.get(Ticket.NOTES);
        ticket.setDescription((String) notes.get(0).get(Ticket.NOTES_BODY));
        ticket.setNotes(notes);
        ticket.setPriority(document.getString(Ticket.PRIORITY));

        Map<String, Object> creator = (Map<String, Object>) document.get(Ticket.CREATOR);
        Timestamp timeStamp = (Timestamp) creator.get(Ticket.CREATOR_TIME);
        Date date = timeStamp.toDate();
        ticket.setDate(date);

        return ticket;
    }

    private static Customer extractCustomerData(DocumentSnapshot document, Customer customer) {
        customer.setName(document.getString(Customer.FNAME) + " " + document.getString(Customer.LNAME));
        customer.setId(document.getId());
        customer.setEmail(document.getString(Customer.EMAIL));
        customer.setAddress(document.getString(Customer.ADDRESS));
        customer.setPhone(document.getString(Customer.PHONE));
        customer.setPath(document.getReference().getPath());
        return customer;
    }

    public static void createTicket(String creatorFName, String creatorLName, String creatorDepartment,
                                    DocumentReference customerReference,
                                    String title, String description,
                                    String priority, String department, final NewTicketViewModel newTicketViewModel) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();

        //ticket creator information
        Map<String, Object> creator = new HashMap<>();
        creator.put(Ticket.CREATOR_FNAME, creatorFName);
        creator.put(Ticket.CREATOR_LNAME, creatorLName);
        creator.put(Ticket.CREATOR_TIME, new Timestamp(new Date()));

        //ticket description information
        Map<String, Object> note = new HashMap<>();
        note.put(Ticket.NOTES_BODY, description);
        note.put(Ticket.NOTES_AUTHOR, creatorFName + " " + creatorLName);
        note.put(Ticket.NOTES_DEPARTMENT, creatorDepartment.toLowerCase());

        data.put(Ticket.CREATOR, creator);
        data.put(Ticket.CUSTOMER, customerReference);
        data.put(Ticket.NOTES, Collections.singletonList(note));
        data.put(Ticket.PRIORITY, priority);
        data.put(Ticket.TITLE, title);

        db.collection("departments/" + department + "/tickets")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        newTicketViewModel.setTicketCreatedStatus(NetworkState.SUCCESS);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        newTicketViewModel.setTicketCreatedStatus(NetworkState.FAILED);
                    }
                });
    }

    public static void customerForwardTicket(Map<String, Object> data, final NoteUpdateInterface updater){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("departments/support/tickets")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        updater.setTicketCreatedStatus(NetworkState.SUCCESS);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        updater.setTicketCreatedStatus(NetworkState.FAILED);
                    }
                });
    }

    public static void updateTicket(Ticket ticket, Map<String, Object> updateMap, final NoteUpdateInterface updater) {
        FirebaseFirestore.getInstance().document(ticket.getPath())
                .update(updateMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updater.setTicketCreatedStatus(NetworkState.SUCCESS);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        updater.setTicketCreatedStatus(NetworkState.FAILED);
                    }
                });
    }

    public static void uploadTicketImages( final List<Uri> selectedPictures, final NoteUpdateInterface updater) {
        if (selectedPictures == null){
            updater.setImageUploadProgressBar(ImageUploadState.SUCCESS);
            return;
        }

        updater.setImageUploadProgressBar(ImageUploadState.LOADING);
        updater.setImageDownloadUriList(new ArrayList<String>());
        for (final Uri uri: selectedPictures) {
            final StorageReference storageReference = mTicketStorageReference.child(uri.getLastPathSegment());
            storageReference
                    .putFile(uri)
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                            //handle reporting progress
                            int progress = (int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                            updater.setImageUploadProgress(progress);
                            updater.setImageUploadProgressBar(selectedPictures.indexOf(uri) + 1);
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri2) {
                                    List<String> newList = updater.getImageDownloadUriList();
                                    newList.add(uri2.toString());
                                    updater.setImageDownloadUriList(newList);

                                    if (selectedPictures.indexOf(uri) == (selectedPictures.size() - 1) &&
                                            !Objects.equals(updater.getImageUploadProgressBar().getValue(), ImageUploadState.SUCCESS)){
                                        updater.setImageUploadProgressBar(ImageUploadState.SUCCESS);
                                    }
                                }
                            });

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //TODO handle failure
                }
            });
        }
    }

    public static void refreshTicket(String path, final TicketDetailViewModel ticketDetailViewModel) {
        FirebaseFirestore.getInstance().document(path)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        //extract initial data from document
                        if (documentSnapshot.exists()) {
                            final Ticket ticket = extractTicketData(documentSnapshot, new Ticket());
                            //Getting the customer reference within the ticket document
                            documentSnapshot.getDocumentReference("customer")
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if (documentSnapshot.exists()){
                                                ticket.setCustomerName(
                                                        documentSnapshot.getString("fname")
                                                                + " "
                                                                + documentSnapshot.getString("lname"));
                                                ticketDetailViewModel.ticket = ticket;
                                                ticketDetailViewModel.setTicketUpdatedStatus(NetworkState.SUCCESS);
                                            }else{
                                                ticket.setCustomerName("No customer");
                                                ticketDetailViewModel.ticket = ticket;
                                                ticketDetailViewModel.setTicketUpdatedStatus(NetworkState.SUCCESS);
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    ticket.setCustomerName("Customer load failed");
                                    ticketDetailViewModel.ticket = ticket;
                                    ticketDetailViewModel.setTicketUpdatedStatus(NetworkState.SUCCESS);
                                }
                            });
                        }else{
                            ticketDetailViewModel.setTicketUpdatedStatus(NetworkState.NOT_FOUND);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                ticketDetailViewModel.setTicketUpdatedStatus(NetworkState.FAILED);
            }
        });
    }

    public static ListenerRegistration ticketsSetListeners(final Query reference, final ViewModel viewModel) {
        return reference
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e("ticketSetListener()", "Listen failed", e);
                            return;
                        }

                        if (viewModel instanceof TicketsListViewModel){//safe against null viewModel
                            //invalidate the current DataSource result
                            TicketsListViewModel ticketsListViewModel = (TicketsListViewModel) viewModel;
                            ticketsListViewModel.toggleSortOrder(ticketsListViewModel.newerFirst);
                        }else if (viewModel instanceof CustomerTicketsViewModel){//safe against null viewModel
                            //invalidate the current DataSource result
                            CustomerTicketsViewModel customerTicketsViewModel = (CustomerTicketsViewModel) viewModel;
                            customerTicketsViewModel.toggleSortOrder(customerTicketsViewModel.newerFirst);
                        }else if (viewModel instanceof ClosedTicketsViewModel){
                            //invalidate the current DataSource result
                            ClosedTicketsViewModel closedTicketsViewModel = (ClosedTicketsViewModel) viewModel;
                            closedTicketsViewModel.toggleSortOrder(closedTicketsViewModel.newerFirst);
                        }else if (viewModel instanceof HomeViewModel){
                            HomeViewModel homeViewModel = (HomeViewModel) viewModel;
                            homeViewModel.toggleSortOrder(homeViewModel.newerFirst);
                            homeViewModel.setComplaintCountLiveData(value.size());
                        }
                    }
                });
    }

    private static class GetQuery {
        private List<DocumentSnapshot> documentSnapshotList;
        private Query ticketQuery;
        private Query.Direction direction;
        private DocumentSnapshot lastDoc;
        private Query nextQuery;


        GetQuery(List<DocumentSnapshot> documentSnapshotList, Query ticketQuery, Query.Direction direction) {
            this.documentSnapshotList = documentSnapshotList;
            this.ticketQuery = ticketQuery;
            this.direction = direction;
        }

        GetQuery(List<DocumentSnapshot> documentSnapshotList, Query ticketQuery){
            this.documentSnapshotList = documentSnapshotList;
            this.ticketQuery = ticketQuery;
        }

        DocumentSnapshot getLastDoc() {
            return lastDoc;
        }

        Query getNextQuery() {
            return nextQuery;
        }

        GetQuery invoke() {
            //get last visible document in query and build Query of the next page
            if (documentSnapshotList.size() > 0) {
                lastDoc = documentSnapshotList.get(
                        documentSnapshotList.size() - 1);
                if (direction == null){
                    nextQuery = ticketQuery
                            .startAfter(lastDoc)
                            .limit(2);
                }else {
                    nextQuery = ticketQuery
                            .orderBy("creator.time", direction)
                            .startAfter(lastDoc)
                            .limit(2);
                }
            }else{
                lastDoc = null;
                nextQuery = null;
            }
            return this;
        }
    }
}