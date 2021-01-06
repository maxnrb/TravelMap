package fr.maximenarbaud.travelmap;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PointDetailActivity extends AppCompatActivity {
    private final static String ACTION_CONFIRMATION = "confirmation";
    private final static String ACTION_NOT_FOUND = "not_found";

    private final static String PROBLEM_NOT_WORKING = "problem_not_working";
    private final static String PROBLEM_BAD_MAINTENANCE = "problem_bad_maintenance";

    private String pointCollectionName = null;
    private String pointDocumentId = null;

    private String coordinates;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Activity Transitions
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

        findViewById(android.R.id.content).setTransitionName("bottom_sheet_shared_container");

        // Attach a callback used to receive the shared elements from Activity A to be used by the container transform transition.
        setEnterSharedElementCallback(new MaterialContainerTransformSharedElementCallback());

        MaterialContainerTransform transform = new MaterialContainerTransform();

        transform.addTarget(android.R.id.content);
        transform.setAllContainerColors(MaterialColors.getColor(findViewById(android.R.id.content), R.attr.colorSurface));
        transform.setFadeMode(MaterialContainerTransform.FADE_MODE_THROUGH);
        transform.setDuration(350L);

        getWindow().setSharedElementEnterTransition(transform);

        setContentView(R.layout.activity_point_detail);

        // Get point info from calling activity
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            this.pointCollectionName = extras.getString("poi_type");
            this.pointDocumentId = extras.getString("poi_id");
        }

        ImageView pointImage = findViewById(R.id.point_detail_picture_imageView);

        // Prepare loading animation
        CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(this);
        circularProgressDrawable.setStrokeWidth(5f);
        circularProgressDrawable.setCenterRadius(30f);
        circularProgressDrawable.start();

        int wallpaperId = R.drawable.fountain_wallpaper;

        switch (pointCollectionName) {
            case "toilet":
                wallpaperId = R.drawable.toilet_wallpaper;
                break;

            case "bin":
                wallpaperId = R.drawable.bin_wallpaper;
                break;
        }

        Glide.with(this)
                .load(wallpaperId)
                .centerCrop()
                .placeholder(circularProgressDrawable)
                .into(pointImage);

        setReturnButton();
        setInitialInformation();
        realTimeFirestoreUpdate();
    }

    private void setReturnButton() {
        Toolbar toolbar = findViewById(R.id.point_detail_toolBar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void setInitialInformation() {
        final TextView pointName = findViewById(R.id.poi_detail_name_textView);
        final TextView pointAddDate = findViewById(R.id.activity_poi_add_data_textView);
        final TextView pointPrice = findViewById(R.id.point_detail_price_text);
        final TextView pointLocation = findViewById(R.id.activity_poi_location_textView);
        final ImageView pointImage = findViewById(R.id.point_detail_picture_imageView);

        if (pointCollectionName != null && pointDocumentId != null) {
            // Access a Cloud Firestore instance from your Activity
            final DocumentReference docRef = FirebaseFirestore.getInstance().collection(pointCollectionName).document(pointDocumentId);

            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists()) {
                            String pictureStorageName = document.getString("picture_name");

                            if (pictureStorageName != null) {
                                // Create a storage reference from our app
                                FirebaseStorage.getInstance().getReference(pictureStorageName).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Prepare loading animation
                                        CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(getApplicationContext());
                                        circularProgressDrawable.setStrokeWidth(5f);
                                        circularProgressDrawable.setCenterRadius(30f);
                                        circularProgressDrawable.start();

                                        Glide.with(getApplicationContext())
                                                .load(uri)
                                                .centerCrop()
                                                .placeholder(circularProgressDrawable)
                                                .into(pointImage);
                                    }
                                });
                            }

                            pointName.setText(document.getString("name"));

                            Timestamp timestamp = document.getTimestamp("date_add");

                            if (timestamp != null) {
                                String date = "Ajout : " + getDateFromTimestamp1(timestamp);
                                pointAddDate.setText(date);
                            }

                            String price = document.getString("price");

                            if (price != null) {
                                if (Float.parseFloat(price) == 0) {
                                    pointPrice.setText(R.string.price_free_str);
                                } else {
                                    pointPrice.setText(String.format("Prix : %s€", price).replaceAll("\\.", ","));
                                }
                            }

                            pointLocation.setText(document.getString("city"));

                            coordinates = document.get("latitude") + ", " + document.get("longitude");

                        } else {
                            Log.d("PointDetailActivity", "No such document");

                        }
                    } else {
                        Log.d("PointDetailActivity", "get failed with ", task.getException());
                    }
                }
            });
        }
}


    private static String getDateFromTimestamp1(Timestamp timestamp) {
        if (timestamp != null) {
            String dayText = new SimpleDateFormat("EEEE", Locale.FRANCE).format(timestamp.toDate());
            dayText = firstToUpper(dayText);

            String dayNumber = new SimpleDateFormat("d", Locale.FRANCE).format(timestamp.toDate());
            String monthText = new SimpleDateFormat("MMMM", Locale.FRANCE).format(timestamp.toDate());
            monthText = firstToUpper(monthText);

            String yearNumber = new SimpleDateFormat("yyyy", Locale.FRANCE).format(timestamp.toDate());

            return dayText + " " + dayNumber + " " + monthText + " " + yearNumber;
        }

        return null;
    }

    private static String getDateFromTimestamp2(Timestamp timestamp) {
        if (timestamp != null) {
            return new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE).format(timestamp.toDate());
        }

        return null;
    }

    private static String firstToUpper(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    public void realTimeFirestoreUpdate() {
        final TextView pointInformation = findViewById(R.id.poi_detail_information_textView);
        final TextView pointConfirmationNb = findViewById(R.id.point_detail_confirmation_nb);
        final TextView pointNotFoundNb = findViewById(R.id.point_detail_not_found_nb);
        final TextView pointProblemNb = findViewById(R.id.point_detail_report_nb);
        final TextView pointUpdateDate = findViewById(R.id.activity_poi_last_modification_textView);

        FirebaseFirestore.getInstance().collection(this.pointCollectionName).document(this.pointDocumentId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }

                if (snapshot != null) {
                    if(snapshot.exists()) {
                        pointInformation.setText(String.format("Approuvé par %s utilisateur(s)", snapshot.getLong("confirmation")));
                        pointConfirmationNb.setText(String.valueOf(snapshot.getLong("confirmation")));
                        pointNotFoundNb.setText(String.valueOf(snapshot.getLong("not_found")));
                        pointProblemNb.setText(String.valueOf(snapshot.getLong("problem")));

                        if (snapshot.contains("date_last_vote")) {
                            Timestamp timestamp = snapshot.getTimestamp("date_last_vote");

                            if (timestamp != null) {
                                String date = "Dernier vote : " + getDateFromTimestamp2(timestamp);
                                pointUpdateDate.setText(date);
                            }

                        } else {
                            pointUpdateDate.setText(R.string.no_vote_str);
                        }

                    } else {
                        // Close point details if removed
                        Toast.makeText(getApplicationContext(),
                                "Le point a été supprimé", Toast.LENGTH_LONG).show();
                        finish();

                    }
                } else {
                    Log.e("onEvent", "snapshot = null");

                }
            }
        });
    }


    public void directionPointBtnClick(View view) {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + coordinates + "&mode=w"));
        mapIntent.setPackage("com.google.android.apps.maps");

        Toast.makeText(this, "N'hésitez pas à revenir sur Travel Map pour noter ce point une fois arrivé", Toast.LENGTH_LONG).show();

        startActivity(mapIntent);
    }

    public void pointConfirmBtnClick(View view) {
        pointUserVote(ACTION_CONFIRMATION);
    }

    public void pointInvalidateBtnClick(View view) {
        pointUserVote(ACTION_NOT_FOUND);
    }

    public void pointSignalBtnClick(View view) {
        // Get current user from FirebaseAuth instance
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            showReportProblemDialog();
        } else {
            Toast.makeText(this, "Vous devez avoir un compte utilisateur pour signaler un problème", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReportProblemDialog() {
        LayoutInflater inflater = getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.dialog_report_problem, null);
        final RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);

        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom)
                .setView(dialogView)
                .setTitle("Signaler un problème :")
                .setNegativeButton("Fermer", null)
                .setPositiveButton("Envoyer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();

                        // Don't use switch/case because resource ID non-final
                        if (selectedRadioButtonId == R.id.radio_button_point_not_working) {
                            pointUserReport(PROBLEM_NOT_WORKING);

                        } else if (selectedRadioButtonId == R.id.radio_button_point_bad_maintenance) {
                            pointUserReport(PROBLEM_BAD_MAINTENANCE);
                        }

                    }
                })
                .show();
    }

    private void pointUserReport(String PROBLEM_TYPE) {
        FirebaseFirestore.getInstance().collection(pointCollectionName).document(pointDocumentId).update(PROBLEM_TYPE, FieldValue.arrayUnion(Timestamp.now()));
        FirebaseFirestore.getInstance().collection(pointCollectionName).document(pointDocumentId).update("problem", FieldValue.increment(1));
    }

    private void pointUserVote(final String ACTION) {
        try {
            // Verify connexion status
            if (isConnected()) {
                // Access a Cloud Firestore instance from Activity
                final FirebaseFirestore db = FirebaseFirestore.getInstance();

                // Get current user from FirebaseAuth instance
                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user != null) {
                    db.collection(pointCollectionName).document(pointDocumentId).collection("user_vote").document(user.getUid()).get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();

                                if (document.exists()) {
                                    String voteValue = String.valueOf(document.get("vote_value"));

                                    if (voteValue.equals(ACTION)) {
                                        Toast.makeText(getApplicationContext(), "Vous avez déjà soumis ce vote", Toast.LENGTH_SHORT).show();
                                        return;

                                    } else {
                                        // User vote already found, but changed
                                        db.collection(pointCollectionName).document(pointDocumentId).update(voteValue, FieldValue.increment(-1));
                                        db.collection(pointCollectionName).document(pointDocumentId).update(ACTION, FieldValue.increment(1));

                                        // Notify the user
                                        Toast.makeText(getApplicationContext(), "Merci d'avoir modifié votre vote", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    // No user vote found, so atomically increment confirmation or not found number
                                    db.collection(pointCollectionName).document(pointDocumentId).update(ACTION, FieldValue.increment(1));

                                    // Notify the user
                                    Toast.makeText(getApplicationContext(), "Merci d'avoir soumis votre vote", Toast.LENGTH_SHORT).show();
                                }

                                db.collection(pointCollectionName).document(pointDocumentId).update("date_last_vote", FieldValue.serverTimestamp());

                                Map<String, Object> voteDetail = new HashMap<>();
                                voteDetail.put("vote_value", ACTION);

                                // Add user vote
                                db.collection(pointCollectionName).document(pointDocumentId).collection("user_vote").document(user.getUid()).set(voteDetail);

                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Erreur lors de la récupération du vote. Veuillez réessayer", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    Toast.makeText(this, "Vous devez avoir un compte utilisateur pour voter", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Vous devez être connecté à internet pour voter", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors de la récupération de l'état de la connexion. Veuillez réessayer", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnected();
    }
}
