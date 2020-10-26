# Fast-Desk
Android Issue management client following MVVM pattern with back-end implemented with Firebase. Communicates with back-end using Firebase SDK and exposed HTTP endpoints.

<img src=https://github.com/dims26/Fast-Desk/blob/master/app/src/main/res/drawable/splash_screen.png width="144" height="256" />

## Features
* Authentication
* Role based actions
* Create and assign tickets
* View, Update and Close tickets
* Attach and upload media
* Attach client information to ticket
* Client search
* Notifications on events

## Screens
#### Staff Module
<table border="0">
 <tr>
    <td><b style="font-size:30px">Queue view</b></td>
    <td><b style="font-size:30px">Closed Tickets</b></td>
    <td><b style="font-size:30px">Search</b></td>
    <td><b style="font-size:30px">Detail</b></td>
 </tr>
 <tr>
    <td>
     <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/queue-overlay.png width="180" height="320" />
   </td>
   <td>
    <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/closed-list-overlay.png width="180" height="320" />
  </td>
   <td>
    <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/customer-list-overlay.png width="180" height="320" />
  </td>
   <td>
    <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/detail-moved-overlay.png width="180" height="320" />
  </td>
 </tr>
 <tr>
  <td>View all open tickets in queue.</td>
  <td>All closed tickets.</td>
  <td>Search by email or name.</td>
  <td>Detail view</td>
 </tr>
 <tr>
    <td><b style="font-size:30px">Update</b></td>
    <td><b style="font-size:30px">Notification</b></td>
    <td><b style="font-size:30px">Splash screen</b></td>
    <td><b style="font-size:30px">Detail - Closed ticket</b></td>
 </tr>
 <tr>
    <td>
     <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/note-input-overlay.png width="180" height="320" />
   </td>
   <td>
    <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/notification-overlay.png width="180" height="320" />
  </td>
   <td>
    <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/splash-screen-overlay.png width="180" height="320" />
  </td>
   <td>
    <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/detail-closed-overlay.png width="180" height="320" />
  </td>
 </tr>
 <tr>
  <td>Update ticket, Attach images.</td>
  <td>Receive push notifications.</td>
  <td>Splash screen</td>
  <td>Closed ticket detail view.</td>
 </tr>
</table>

#### Client Module
<table border="0">
 <tr>
    <td><b style="font-size:30px">Home View</b></td>
    <td><b style="font-size:30px">Create Complaint</b></td>
    <td><b style="font-size:30px">Complaint Detail</b></td>
    <td><b style="font-size:30px">Add Note</b></td>
 </tr>
 <tr>
    <td>
     <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/customer-home-overlay.png width="180" height="320" />
   </td>
   <td>
    <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/customer-new-complaint-overlay.png width="180" height="320" />
  </td>
   <td>
    <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/customer-detail-overlay.png width="180" height="320" />
  </td>
   <td>
    <img src=https://github.com/dims26/Fast-Desk/blob/master/screens/customer-add-note-overlay.png width="180" height="320" />
  </td>
 </tr>
 <tr>
  <td>View all open tickets in queue.</td>
  <td>All closed tickets.</td>
  <td>Search by email or name.</td>
  <td>Detail view</td>
 </tr>
 </table>

## Built using

* [Android Jetpack](https://developer.android.com/jetpack/?gclid=Cj0KCQjwhJrqBRDZARIsALhp1WQBmjQ4WUpnRT4ETGGR1T_rQG8VU3Ta_kVwiznZASR5y4fgPDRYFqkaAhtfEALw_wcB) - Official suite of libraries, tools, and guidance to help developers write high-quality apps.
  * [Android KTX](https://developer.android.com/kotlin/ktx)
  * [LiveData](https://developer.android.com/topic/libraries/architecture/livedata)
  * [Paging](https://developer.android.com/jetpack/androidx/releases/paging)
  * [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
* [Firebase](https://firebase.google.com/) - Backend-as-a-Service solution.
  * [Authentication](https://firebase.google.com/products/auth)
  * [Cloud Firestore](https://firebase.google.com/products/firestore)
  * [Cloud Messaging](https://firebase.google.com/products/cloud-messaging)
  * [Cloud Functions](https://developer.android.com/topic/libraries/architecture/viewmodel)
  * [Cloud Storage](https://firebase.google.com/products/storage)
* [FirebaseUI](https://github.com/firebase/FirebaseUI-Android) - Optimized UI components for Firebase.
* [Glide](https://github.com/bumptech/glide) - A media management and image loading framework for Android.
* [StfalconImageViewer](https://github.com/stfalcon-studio/StfalconImageViewer) - An Android full-screen image viewer.
* [Matisse](https://github.com/zhihu/Matisse) - A local image and video selector for Android.
* [FlexboxLayout](https://github.com/google/flexbox-layout) - A library bringing capabilities of CSS Flexible Box Layout Module to Android.
