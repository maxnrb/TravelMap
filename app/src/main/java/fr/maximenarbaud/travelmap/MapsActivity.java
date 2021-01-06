package fr.maximenarbaud.travelmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.maximenarbaud.travelmap.BitmapUtils.bitmapDescriptorFromVector;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, NavigationView.OnNavigationItemSelectedListener {
    /**
     * Request code for location permission request.
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final int RC_SIGN_IN = 123;
    private static final int DEFAULT_ZOOM = 15;

    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocationClient;

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private List<Point> pointList;

    private String selectedPointType;
    private String selectedPointId;
    private String selectedPointCoordinates;

    private Bundle savedInstanceState = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable Activity Transitions
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

        // Attach a callback used to capture the shared elements from this Activity to be used by the container transform transition
        setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());

        // Keep system bars (status bar, navigation bar) persistent throughout the transition
        getWindow().setSharedElementsUseOverlay(false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        pointList = new ArrayList<>();

        pointList.add(new Point(true, R.drawable.ic_water_marker, R.id.menu_item_water_tap, R.drawable.ic_faucet, "water_point"));
        pointList.add(new Point(true, R.drawable.ic_toilet_hf_marker, R.id.menu_item_toilette,  R.drawable.ic_wc, "toilet"));
        pointList.add(new Point(true, R.drawable.ic_bin_marker, R.id.menu_item_bin, R.drawable.ic_trash, "bin"));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 6 - Configure all views
        this.configureToolBar();
        this.configureDrawerLayout();
        this.configureNavigationView();

        initBottomSheet();
        setFab();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirestoreUtils.setInterfaceByRightLevel(this.navigationView.getMenu());
        updateNavigationMenuUI();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // saving the last zoom, coordinates
        double lat = gMap.getCameraPosition().target.latitude;
        double lon = gMap.getCameraPosition().target.longitude;
        float zoom = gMap.getCameraPosition().zoom;

        savedInstanceState.putDouble("map_lat", lat);
        savedInstanceState.putDouble("map_lon", lon);
        savedInstanceState.putFloat("map_zoom", zoom);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        this.savedInstanceState = savedInstanceState;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // RC_SIGN_IN is the request code you passed into startAc                                                                                                                                                                                                                                                                                                             tivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK && response != null) {
                if (response.isNewUser()) {
                    putUserInfoInFirestore();
                }

                updateNavigationMenuUI();

            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    Log.e("onActivityResult", "connection failed");
                    return;
                }

                Log.e("onActivityResult", "Sign-in error: ", response.getError());
            }
        }
    }

    private void putUserInfoInFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // Access a Cloud Firestore instance from your Activity
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Map<String, Object> userInfo = new HashMap<>();

            userInfo.put("email", user.getEmail());
            userInfo.put("right_level", 0);

            db.collection("users").document(user.getUid()).set(userInfo);
        }
    }

    private void setFab() {
        final FloatingActionButton fabPosition = findViewById(R.id.fabPosition);
        final FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        // Set fab invisible by default, will be visible if permission location enable in enableLocationPermission
        fabPosition.setVisibility(View.GONE);
        fabAdd.setVisibility(View.GONE);

        fabPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { setCameraMapOnDeviceLocation(); }
        });

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user != null) {
                    Intent intent = new Intent(getApplicationContext(), AddPointActivity.class);
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(MapsActivity.this, fabAdd,"shared_container");

                    startActivity(intent, options.toBundle());

                } else {
                    // Open popup if user not login
                    new MaterialAlertDialogBuilder(MapsActivity.this, R.style.MaterialAlertDialogCustom)
                            .setTitle(R.string.need_account_dialog_title_str)
                            .setMessage(R.string.need_account_dialog_message_str)
                            .setNegativeButton(R.string.need_account_dialog_negativeButton_str, null)
                            .setPositiveButton(R.string.need_account_dialog_positiveButton_str, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) { createSignInIntent(); }})
                            .show();
                }
            }
        });
    }

    private void initBottomSheet() {
        View bottomSheetView = findViewById(R.id.point_detail_bottom_sheet_view);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
    }

    private void updateNavigationMenuUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        View headerView = this.navigationView.getHeaderView(0);
        TextView headerName = headerView.findViewById(R.id.navigation_header_name);
        TextView headerMail = headerView.findViewById(R.id.navigation_header_email);
        ImageView profilePicture = headerView.findViewById(R.id.profile_picture_imageView);

        if (user != null) {
            headerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent pendingIntent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(pendingIntent);
                }
            });

            // Prepare loading animation
            CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(this);
            circularProgressDrawable.setStrokeWidth(5f);
            circularProgressDrawable.setCenterRadius(30f);
            circularProgressDrawable.start();

            Glide.with(this)
                    .load(R.drawable.user)
                    .centerCrop()
                    .placeholder(circularProgressDrawable)
                    .into(profilePicture);

            headerView.findViewById(R.id.profile_picture_cardView).setVisibility(View.VISIBLE);
            headerView.findViewById(R.id.access_profile_hint_textView).setVisibility(View.VISIBLE);

            if(user.getDisplayName() != null) {
                headerName.setText(user.getDisplayName());
            }

            if(user.getEmail() != null) {
                headerMail.setText(user.getEmail());
            }
        } else {
            headerView.findViewById(R.id.profile_picture_cardView).setVisibility(View.GONE);
            headerView.findViewById(R.id.access_profile_hint_textView).setVisibility(View.GONE);

            headerName.setText(R.string.no_connection_header_title_str);
            headerMail.setText(R.string.no_connection_header_subtitle_str);

            headerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createSignInIntent();
                }
            });
        }
    }

    public void createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Collections.singletonList(new AuthUI.IdpConfig.EmailBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        //.setLogo(R.drawable.logo)
//                        .setTosAndPrivacyPolicyUrls(
//                                "https://example.com/terms.html",
//                                "https://example.com/privacy.html")
                        .build(),
                RC_SIGN_IN);
        // [END auth_fui_create_intent]
    }


    public void onBottomSheetClick(View view) {
        Intent intent = new Intent(this, PointDetailActivity.class);
        intent.putExtra("poi_type", selectedPointType);
        intent.putExtra("poi_id", selectedPointId);

        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(MapsActivity.this, view,"bottom_sheet_shared_container");

        startActivity(intent, options.toBundle());
    }

    public void directionBtnClick(View view) {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + selectedPointCoordinates + "&mode=w"));
        mapIntent.setPackage("com.google.android.apps.maps");

        Toast.makeText(this, "N'hésitez pas à revenir sur Travel Map pour noter ce point une fois arrivé", Toast.LENGTH_LONG).show();

        startActivity(mapIntent);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        gMap.getUiSettings().setMapToolbarEnabled(false);

        // Remove default my location button (replaced by floating action button)
        gMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Set a listener for click on marker
        gMap.setOnMarkerClickListener(this);

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48.873631428059525, 2.295086518830307), DEFAULT_ZOOM));

        if (this.savedInstanceState != null) {
            // get the last zoom, coordinates
            double restoreLat = savedInstanceState.getDouble("map_lat");
            double restoreLong = savedInstanceState.getDouble("map_lon");
            float restoreZoom = savedInstanceState.getFloat("map_zoom", 15);

            LatLng latLng = new LatLng(restoreLat, restoreLong);
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, restoreZoom));
        }

        enableLocationPermission();
        markerRealTimeUpdate();
    }

    /**
     * handle marker click event
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        gMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 250, null);

        TextView pointNameTextView = findViewById(R.id.bottom_sheet_point_name_textView);
        TextView pointDescTextView = findViewById(R.id.bottom_sheet_point_desc_textView);

        pointNameTextView.setText(marker.getTitle());
        pointDescTextView.setText(marker.getSnippet());

        selectedPointCoordinates = marker.getPosition().latitude + ", " + marker.getPosition().longitude;

        if (marker.getTag() != null) {
            int poiListPosition = (int)marker.getTag();

            findViewById(R.id.bottom_sheet_point_icon).setBackgroundResource(pointList.get(poiListPosition).getIconResDrawable());

            selectedPointType = pointList.get(poiListPosition).getPoiType();
            selectedPointId = pointList.get(poiListPosition).getMarkerId(marker);
        }

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        return true;
    }

    private void enableLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (gMap != null) {
                gMap.setMyLocationEnabled(true);

                findViewById(R.id.fabPosition).setVisibility(View.VISIBLE);
                findViewById(R.id.fabAdd).setVisibility(View.VISIBLE);

                if (this.savedInstanceState == null) {
                    setCameraMapOnDeviceLocation();
                }
            }
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableLocationPermission();

        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            PermissionUtils.PermissionDeniedDialog.newInstance(true).show(getSupportFragmentManager(), "Denied Dialog");
        }
    }


    private void setCameraMapOnDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        // 5 - Handle back click to close menu
        if (this.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.drawerLayout.closeDrawer(GravityCompat.START);

        } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        } else {
            super.onBackPressed();
        }

    }

    // 1 - Configure Toolbar
    private void configureToolBar() {
        this.toolbar = findViewById(R.id.activity_main_toolbar);
        setSupportActionBar(toolbar);
    }

    // 2 - Configure Drawer Layout
    private void configureDrawerLayout() {
        this.drawerLayout = findViewById(R.id.home_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.getDrawerArrowDrawable().setColor(ContextCompat.getColor(this, R.color.fabPositionBackground));

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    // 3 - Configure NavigationView
    private void configureNavigationView() {
        this.navigationView = findViewById(R.id.home_navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        Menu menu = navigationView.getMenu();

        // For each POI in poiList (water point, toilet, bin...)
        for (Point point : pointList) {

            // If POI is set as selected, set check icon in navigation menu
            if(point.isSelected()) {
                ImageView itemCheckedImageView = new ImageView(this);
                itemCheckedImageView.setImageResource(R.drawable.ic_baseline_check_24);

                MenuItem menuItem = menu.findItem(point.getMenuItemId());
                menuItem.setChecked(true);
                menuItem.setActionView(itemCheckedImageView);
            }
        }
    }

    // 4 - Handle Navigation Item Click
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        ImageView itemCheckedImageView = new ImageView(this);
        itemCheckedImageView.setImageResource(R.drawable.ic_baseline_check_24);

        if (item.getItemId() == R.id.menu_item_city_manager) {
            this.drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ManagerCityActivity.class));

        } else if (item.getItemId() == R.id.menu_item_user_manager) {
            this.drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ManagerUserActivity.class));
        }

        for (Point point : pointList) {
            if(point.getMenuItemId() == item.getItemId()) {

                if (item.isChecked()) {
                    item.setActionView(null);
                    item.setChecked(false);

                    point.setSelected(false);

                } else {
                    item.setActionView(itemCheckedImageView);
                    item.setChecked(true);

                    point.setSelected(true);
                }
            }
        }

        return true;
    }


    public void markerRealTimeUpdate() {
        // Access a Cloud Firestore instance from your Activity
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (final Point point : pointList) {
            final String collectionPath = point.getPoiType();

            db.collection(collectionPath)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {

                        @Override
                        public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                e.printStackTrace();
                                return;
                            }

                            if (snapshots != null) {
                                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                    switch (dc.getType()) {

                                        case ADDED:
                                            QueryDocumentSnapshot document = dc.getDocument();

                                            Double latitude = document.getDouble("latitude");
                                            Double longitude = document.getDouble("longitude");
                                            String name = document.getString("name");

                                            if (latitude != null && longitude != null && name != null) {
                                                Marker marker = gMap.addMarker(new MarkerOptions()
                                                        .position(new LatLng(latitude, longitude))
                                                        .title(name)
                                                        .icon(bitmapDescriptorFromVector(getApplicationContext(), point.getMarkerResDrawable()))
                                                        .snippet( getDescriptionString(document.getLong("confirmation")) )
                                                        .visible(point.isSelected()
                                                        ));

                                                marker.setTag(pointList.indexOf(point));
                                                point.addMarker(document.getId(), marker);
                                            }

                                            break;

                                        case MODIFIED:
                                            if (dc.getDocument().getId().equals(selectedPointId)) {
                                                TextView pointDescTextView = findViewById(R.id.bottom_sheet_point_desc_textView);
                                                pointDescTextView.setText( getDescriptionString(dc.getDocument().getLong("confirmation")) );
                                            }

                                            point.modifyMarker(dc.getDocument());
                                            break;

                                        case REMOVED:
                                            if (dc.getDocument().getId().equals(selectedPointId)) {
                                                // Close bottom sheet if point removed
                                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                                            }

                                            point.removeMarker(dc.getDocument().getId());
                                            break;
                                    }
                                }
                            }

                        }
                    });
        }
    }

    private String getDescriptionString(Long confirmationNb) {
        String description = "";

        if (confirmationNb != null) {
            description = String.format("Confirmé par %s utilisateur", confirmationNb);

            if (confirmationNb > 1) {
                description += "s";
            }
        }

        return description;
    }
}