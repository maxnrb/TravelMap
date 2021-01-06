package fr.maximenarbaud.travelmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.platform.MaterialArcMotion;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddPointActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;

    private AutoCompleteTextView editTextFilledExposedDropdown;
    private final BottomSheetFragment bottomSheetFragment = new BottomSheetFragment();

    private boolean isLocationSet;
    private double latitude;
    private double longitude;

    // It maintains a linked list of the entries in the map, in the order in which they were inserted.
    private final Map<String, String> poiMap = new LinkedHashMap<>();

    private Uri imageUri = null;
    private Boolean tempPic = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Activity Transitions
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

        findViewById(android.R.id.content).setTransitionName("shared_container");

        // Attach a callback used to receive the shared elements from Activity A to be used by the container transform transition.
        setEnterSharedElementCallback(new MaterialContainerTransformSharedElementCallback());

        MaterialContainerTransform transform = new MaterialContainerTransform();

        transform.addTarget(android.R.id.content);
        transform.setAllContainerColors(MaterialColors.getColor(findViewById(android.R.id.content), R.attr.colorSurface));
        transform.setFitMode(MaterialContainerTransform.FIT_MODE_AUTO);
        transform.setDuration(400L);
        transform.setPathMotion(new MaterialArcMotion());
        transform.setInterpolator(new FastOutSlowInInterpolator());
        //transform.setScrimColor(Color.TRANSPARENT);

        getWindow().setSharedElementEnterTransition(transform);

        poiMap.put("Point d'eau", "water_point");
        poiMap.put("Toilette", "toilet");
        poiMap.put("Poubelle", "bin");

        isLocationSet = false;

        setContentView(R.layout.activity_add_point);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_menu_item, poiMap.keySet().toArray(new String[0]));

        this.editTextFilledExposedDropdown = findViewById(R.id.add_point_selector_textView);
        this.editTextFilledExposedDropdown.setAdapter(adapter);

        Toolbar toolbar = findViewById(R.id.add_point_toolBar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        toolbar.setOnMenuItemClickListener(this);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        isLocationSet = true;

                        TextInputEditText textInputEditText = findViewById(R.id.point_location_edit_text);
                        textInputEditText.setText(String.format("Lat: %s , Long: %s", latitude, longitude));
                    } else {
                        Toast.makeText(getApplicationContext(), "Impossible de déterminer votre position. Veuillez réessayer", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            });
        }
    }

    // Activity result for picture selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ImageView imageView = findViewById(R.id.add_point_picture_imageView);

        if (resultCode == RESULT_OK) {
            bottomSheetFragment.dismiss();

            // Prepare loading animation
            CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(this);
            circularProgressDrawable.setStrokeWidth(5f);
            circularProgressDrawable.setCenterRadius(30f);
            circularProgressDrawable.start();

            if (requestCode == REQUEST_IMAGE_GALLERY) {
                this.imageUri = data.getData();
                this.tempPic = false;
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                this.tempPic = true;
            }

            try {
                final InputStream inputStream = getContentResolver().openInputStream(this.imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(inputStream);

                Glide.with(this)
                        .load(selectedImage)
                        .centerCrop()
                        .placeholder(circularProgressDrawable)
                        .into(imageView);

                //imageView.setImageBitmap(selectedImage);
                findViewById(R.id.add_point_picture_cardView).setVisibility(View.VISIBLE);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Chargement de la photo échoué. Merci de réessayer", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addPictureFieldClick(View view) {
        // Display BottomSheetDialogFragment
        bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());
    }

    // Start camera intent
    public void appPhotoBtnClick(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                this.imageUri = FileProvider.getUriForFile(this, "fr.maximenarbaud.travelmap.fileprovider", photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, this.imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(fileName,".jpg", storageDir);
    }

    // Start gallery intent
    public void galleryBtnClick(View view) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");

        if (photoPickerIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(photoPickerIntent, REQUEST_IMAGE_GALLERY);
        }
    }

    // Detect click on send button top app bar
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.send_btn) {
            sendBtnClick();
            //uploadPicture();
        }

        return false;
    }

    @Nullable
    private String getFirestoreDocumentFromSelector() {
        TextInputLayout textInputLayout = findViewById(R.id.add_point_selector_textLayout);
        String selectedPoiString = String.valueOf(this.editTextFilledExposedDropdown.getText());

        textInputLayout.setErrorEnabled(false);

        if ( poiMap.containsKey(selectedPoiString) ) {
            return poiMap.get(selectedPoiString);

        } else {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(getString(R.string.add_point_category_error_str));

            return null;
        }
    }

    @Nullable
    private String getPointName() {
        TextInputEditText textInputEditText = findViewById(R.id.add_point_name_editText);
        TextInputLayout textInputLayout = findViewById(R.id.add_point_name_textLayout);
        String name = String.valueOf(textInputEditText.getText());

        textInputLayout.setErrorEnabled(false);

        if ( !name.equals("") ) {
            return name;

        } else {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(getString(R.string.add_point_name_error_str));

            return null;
        }
    }

    private String getPrice() {
        TextInputEditText textInputEditText1 = findViewById(R.id.add_point_price_editText);
        return String.valueOf(textInputEditText1.getText());
    }

    @Nullable
    private String getCityFromCoordinates() {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            return addresses.get(0).getLocality();

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();

            Toast.makeText(getApplicationContext(), R.string.add_point_city_determine_error_str, Toast.LENGTH_SHORT).show();

            return null;
        }
    }

    public void sendBtnClick() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // Notify user
            Toast.makeText(getApplicationContext(), "Erreur de connexion à votre compte", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Get selected point type
        String firestoreDocumentName = getFirestoreDocumentFromSelector();
        String name = getPointName();
        String price = getPrice();
        String cityName = getCityFromCoordinates();

        if ((firestoreDocumentName != null) && (name != null) && isLocationSet && (cityName != null)) {
            if (this.imageUri != null) {
                // Generate picture name for storage base on timestamp and random UUID
                String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString();
                uploadPicture(fileName, name, cityName, price, firestoreDocumentName);

            } else {
                uploadPoint(null, name, cityName, price, firestoreDocumentName);
            }
        }
    }


    private void uploadPicture(final String fileName, final String name, final String cityName, final String price, final String firestoreDocumentName) {
        // Create a storage reference from our app
        StorageReference storageRef = FirebaseStorage.getInstance().getReference(fileName);

        // Notify user
        Toast.makeText(getApplicationContext(), "Envoie de la photo en cours...", Toast.LENGTH_SHORT).show();

        UploadTask uploadTask = storageRef.putFile(this.imageUri);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Notify user
                Toast.makeText(getApplicationContext(),"Echec de l'envoie de la photo. Merci de réessayer", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if (tempPic) {
                    getApplicationContext().getContentResolver().delete(imageUri, null, null);
                }

                uploadPoint(fileName, name, cityName, price, firestoreDocumentName);
            }
        });
    }

    private void uploadPoint(String fileName, String name, String cityName, String price, String firestoreDocumentName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Access a Cloud Firestore instance from your Activity
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Put point information in Map
        Map<String, Object> markerData = new HashMap<>();

        markerData.put("latitude", latitude);
        markerData.put("longitude", longitude);
        markerData.put("name", name);
        markerData.put("city", cityName);
        markerData.put("confirmation", 0);
        markerData.put("not_found", 0);
        markerData.put("problem", 0);
        markerData.put("price", price);
        markerData.put("date_add", FieldValue.serverTimestamp());

        if (fileName != null) {
            markerData.put("picture_name", fileName);
        }

        assert user != null;    // Already test before is user == null
        markerData.put("user_id", user.getUid());

        Map<String, Object> city = new HashMap<>();
        city.put("null", null);

        // Add point to firestore
        db.collection(firestoreDocumentName).add(markerData);

        // Add city name to "city" collection
        db.collection("city").document(cityName).set(city);

        // Notify user
        Toast.makeText(getApplicationContext(), R.string.add_point_success_add_str, Toast.LENGTH_SHORT).show();

        // Return to map Activity
        finish();
    }
}
