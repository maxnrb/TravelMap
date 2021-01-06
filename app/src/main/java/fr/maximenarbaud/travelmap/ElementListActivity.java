package fr.maximenarbaud.travelmap;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ElementListActivity extends AppCompatActivity implements RecyclerViewAdapter.ItemClickListener {
    private List<RecyclerElement> recyclerElementList;
    private RecyclerViewAdapter adapter;

    private String pointCollectionName;
    private String cityName;
    private int iconResId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            this.pointCollectionName = extras.getString("type");
            this.cityName = extras.getString("city");
            this.iconResId = extras.getInt("iconResId");
        }

        setContentView(R.layout.activity_manager_city_detail);

        Toolbar toolbar = findViewById(R.id.manager_activity_toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        this.recyclerElementList = new ArrayList<>();

        setFromFirestore();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (cityName != null) {
            setFromFirestore();
        }
    }

    private void setFromFirestore() {
        if (adapter != null) {
            this.recyclerElementList.clear();
            adapter.notifyDataSetChanged();
        }
        
        FirebaseFirestore.getInstance().collection(this.pointCollectionName)
                .whereEqualTo("city", this.cityName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Long nbProblem = document.getLong("problem");
                                String problemLine = "Aucun problème signalé";
                                int backgroundTint = 0xFF9CCC65;

                                if (nbProblem != null && nbProblem == 1) {
                                    problemLine = "1 problème signalé";
                                    backgroundTint = 0xFFFFA726;

                                } else if (nbProblem != null && nbProblem > 1) {
                                    problemLine = nbProblem + " problèmes signalés";
                                    backgroundTint = 0xFFDB4437;
                                }

                                recyclerElementList.add(new RecyclerElement( String.valueOf(document.getData().get("name")),
                                        problemLine, iconResId, "", backgroundTint, document.getId()));
                            }
                        } else {
                            Log.d("Get city point", "Error getting documents: ", task.getException());
                        }

                        setRecyclerView();
                    }
                });

    }


    private void setRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.manager_home_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        this.adapter = new RecyclerViewAdapter(this, recyclerElementList);
        this.adapter.setClickListener(this);

        recyclerView.setAdapter(this.adapter);
    }

    @Override
    public void onItemClick(View view, int position) {
        showPointClickDialog(position);
    }

    private void showPointClickDialog(final int position) {
        LayoutInflater inflater = getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.dialog_city_manager, null);
        final RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);

        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom)
                .setView(dialogView)
                .setTitle("Sélectionner une action :")
                .setNegativeButton("Fermer", null)
                .setPositiveButton("Confirmer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();

                        // Don't use switch/case because resource ID non-final
                        if (selectedRadioButtonId == R.id.radio_button_delete_point) {
                            deletePoint(position);

                        } else if(selectedRadioButtonId == R.id.radio_button_info_point) {
                            Intent intent = new Intent(getApplicationContext(), PointDetailActivity.class);
                            intent.putExtra("poi_type", pointCollectionName);
                            intent.putExtra("poi_id", recyclerElementList.get(position).getPointId());

                            startActivity(intent);

                        } else if(selectedRadioButtonId == R.id.radio_button_see_problem_point) {
                            Intent intent = new Intent(getApplicationContext(), ManagerProblemActivity.class);
                            intent.putExtra("poi_type", pointCollectionName);
                            intent.putExtra("poi_id", recyclerElementList.get(position).getPointId());

                            startActivity(intent);
                        }
                    }
                })
                .show();
    }

    private void deletePoint(final int position) {
        String pointId = recyclerElementList.get(position).getPointId();

        FirebaseFirestore.getInstance().collection(pointCollectionName).document(pointId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        adapter.deleteItem(position);
                    }
                })

                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("deletePoint", "Error deleting document", e);
                    }
                });
    }
}
