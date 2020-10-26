package com.dims.fastdesk.utilities;

import com.google.gson.stream.JsonReader;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FirebaseFunctionUtils{

    private static String URL_STAFF_TICKET_REF;
    private static String URL_DEPARTMENT;
    private static String URL_MOVE;
    private static String URL_CLOSE;
    private static String URL_CUSTOMER;
    private static String URL_CUSTOMER_CLOSE;

    static {
        try {
            InputStreamReader reader = new InputStreamReader(
                    Objects.requireNonNull(
                            FirebaseFunctionUtils.class.getClassLoader()
                    ).getResourceAsStream("res/raw/urls.json"));
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                jsonReader.nextName();
                URL_STAFF_TICKET_REF = jsonReader.nextString();
                jsonReader.nextName();
                URL_DEPARTMENT = jsonReader.nextString();
                jsonReader.nextName();
                URL_MOVE = jsonReader.nextString();
                jsonReader.nextName();
                URL_CLOSE = jsonReader.nextString();
                jsonReader.nextName();
                URL_CUSTOMER = jsonReader.nextString();
                jsonReader.nextName();
                URL_CUSTOMER_CLOSE = jsonReader.nextString();
            }
            jsonReader.endObject();
            jsonReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private FirebaseFunctionUtils(){}

    /**Called to get path to the currently logged in user's tickets
     *
     * @param token An auth token from the currently logged in user to enable backend authentication
     * @param callback The callback which defines the actions to be taken depending on the success
     *                 of the network call
     */
    public static void getStaffTicketRef(String token, Callback callback) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(URL_STAFF_TICKET_REF
                        + "?token=" + token)
                .get()
                .build();

        client.newCall(request)
                //enqueue enables an asynchronous call and retrieval of the result via a callback
                .enqueue(callback);
    }

    /**Called to get the customer data as well as the name of the collection group for customer tickets
     *
     * @param token An auth token from the currently logged in user to enable backend authentication
     * @param callback The callback which defines the actions to be taken depending on the success
     *                 of the network call
     */
    public static void getCustomerData(String token, Callback callback) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(URL_CUSTOMER
                        + "?token=" + token)
                .get()
                .build();

        client.newCall(request)
                //enqueue enables an asynchronous call and retrieval of the result via a callback
                .enqueue(callback);
    }

    /**Called to get list of views
     *
     * @param token An auth token from the currently logged in user to enable backend authentication
     * @param callback The callback which defines the actions to be taken depending on the success
     *                 of the network call
     */
    public static void getDepartments(String token, Callback callback) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(URL_DEPARTMENT
                        + "?token=" + token)
                .get()
                .build();

        client.newCall(request)
                //enqueue enables an asynchronous call and retrieval of the result via a callback
                .enqueue(callback);
    }

    /**Called to move ticket to specified department
     *
     * @param token An auth token from the currently logged in user to enable backend authentication
     * @param path reference to the ticket in the database
     * @param intendedDepartment the department that the ticket is to be moved to
     * @param callback The callback which defines the actions to be taken depending on the success
     */
    public static void moveTicket(String token, String path, String intendedDepartment, Callback callback) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(URL_MOVE
                        + "?token=" + token
                        + "&path=" + path
                        + "&intendedDepartment=" + intendedDepartment
                )
                .get()
                .build();
        client.newCall(request)
                //enqueue enables an asynchronous call and retrieval of the result via a callback
                .enqueue(callback);
    }

    /**Called to close the ticket
     *
     * @param token An auth token from the currently logged in user to enable backend authentication
     * @param path reference to the ticket in the database
     * @param callback The callback which defines the actions to be taken depending on the success
     */
    public static void closeTicket(String token, String path, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(URL_CLOSE
                        + "?token=" + token
                        + "&path=" + path
                )
                .get()
                .build();
        client.newCall(request)
                //enqueue enables an asynchronous call and retrieval of the result via a callback
                .enqueue(callback);
    }

    /**Called by signed in customer to close the ticket
     *
     * @param token An auth token from the currently logged in user to enable backend authentication
     * @param path reference to the ticket in the database
     * @param callback The callback which defines the actions to be taken depending on the success
     */
    public static void customerCloseTicket(String token, String path, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(URL_CUSTOMER_CLOSE
                        + "?token=" + token
                        + "&path=" + path
                )
                .get()
                .build();
        client.newCall(request)
                //enqueue enables an asynchronous call and retrieval of the result via a callback
                .enqueue(callback);
    }
}