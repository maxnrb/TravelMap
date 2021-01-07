# TravelMap
TravelMap is an application designed to clearly and centrally list all the fountains, but also the bins and toilets available in any city. While having reliable and up-to-date information.

The application will also serve to simplify the work of municipal services, by allowing them to visualise the different points in a city and the problems associated with them.

## Main features
* Real-time interactive maps with fountains, toilets and bins
* Possibility to add new points on the map by users
* User votes: confirmation or set as not found for each point
* Report problems (non-functional or poorly maintained point)
* Management of points and problems for municipal officers

## Screenshots
<img align="left" width= "250" alt="Screenshot 1" src="https://raw.githubusercontent.com/maximenrb/TravelMap/master/screenshots/Screenshot_1609946144.png">

<img align="right" width= "250" alt="Screenshot 3" src="https://raw.githubusercontent.com/maximenrb/TravelMap/master/screenshots/Screenshot_1609946240.png">

<p align="center">
  <img width="250" alt="Screenshot 2" src="https://raw.githubusercontent.com/maximenrb/TravelMap/master/screenshots/Screenshot_1609946183.png">
</p>
<img align="left" width= "250" alt="Screenshot 4" src="https://raw.githubusercontent.com/maximenrb/TravelMap/master/screenshots/Screenshot_1609946303.png">

<img align="right" width= "250" alt="Screenshot 6" src="https://raw.githubusercontent.com/maximenrb/TravelMap/master/screenshots/Screenshot_1609946597.png">

<p align="center">
  <img width="250" alt="Screenshot 5" src="https://raw.githubusercontent.com/maximenrb/TravelMap/master/screenshots/Screenshot_1609946348.png">
</p>

## Installation
Firebase:
1. Create a new projet in [Firebase Console](https://console.firebase.google.com/u/0/)
2. Add Firebase to the android app, with package name "*fr.maximenarbaud.travelmap*" ([See documentation](https://firebase.google.com/docs/android/setup?authuser=0))
3. Download "*google-services.json*" file and put in `app/` folder
4. Activate Authentification, Cloud Firestore and Cloud Storage services

Android Maps SDK:
1. Create a new Google Maps Platerform project, activate Maps SDK for Android and get your API key ([See documentation](https://developers.google.com/maps/gmp-get-started))
2. Put your API key in `app/src/main/res/values/google_maps_api.xml`, and in  `app/src/release/res/values/google_maps_api.xml` for release 

## Build With
[Google Firebase](https://firebase.google.com/):
* [Cloud Firestore](https://firebase.google.com/products/firestore) - Firestore is a flexible, scalable NoSQL cloud database to store and sync in realtime data for client and server-side development.
* [Firebase Authentification](https://firebase.google.com/products/auth) -  Authentication provides backend services, easy-to-use SDKs, and ready-made UI libraries to authenticate users to your app.
* [Cloud Storage](https://firebase.google.com/products/storage) - Cloud Storage is a powerful, simple, and cost-effective object (images, videos...) storage service.

[Google Maps Plateform](https://cloud.google.com/maps-platform/maps?hl=fr) - Build customized, agile experiences that bring the real world to your users with static and dynamic maps, Street View imagery, and 360° views.

## Author
[maximenrb](https://github.com/maximenrb)

## License

<img align="left" width= "150" alt="CC Attribution-NonCommercial-NoDerivatives 4.0 International Public License Logo" src="https://raw.githubusercontent.com/maximenrb/TravelMap/master/screenshots/by-nc-nd.eu.png">

This project is licensed under the CC Attribution-NonCommercial-NoDerivatives 4.0 International Public License

See the [LICENSE.md](https://github.com/maximenrb/TravelMap/blob/master/LICENSE.md) file for details
