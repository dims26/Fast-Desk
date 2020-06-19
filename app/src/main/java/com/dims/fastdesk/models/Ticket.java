package com.dims.fastdesk.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Ticket implements Serializable {

    //define keys for ticket contents
    public static final String CREATOR = "creator";
    public static final String CREATOR_FNAME = "fname";
    public static final String CREATOR_LNAME = "lname";
    public static final String CREATOR_TIME = "time";
    public static final String CUSTOMER = "customer";
    public static final String PRIORITY = "priority";
    public static final String TITLE = "title";
    public static final String NOTES = "notes";
    public static final String NOTES_BODY = "body";
    public static final String NOTES_AUTHOR = "author";
    public static final String NOTES_DEPARTMENT = "department";
    public static final String NOTES_IMAGES = "images";

    private Date date;
    private String customerName, title, description, id, priority;
    private List<Map<String, Object>> notes;
    private String path;
    private Customer customer;


    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setNotes(List<Map<String, Object>> notes) {
        this.notes = notes;
    }
    public void addNotes(Map<String, Object> note){
        this.notes.add(note);
    }

    public List<Map<String, Object>> getNotes() {
        return new ArrayList<>(this.notes);
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPriority() {
        return priority;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
