package fr.maximenarbaud.travelmap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManagerProblemActivity extends AppCompatActivity implements RecyclerViewAdapter.ItemClickListener {
    private String pointCollectionName = null;
    private String pointDocumentId = null;

    private List<ProblemObj> problemObjList;
    private RecyclerViewAdapterProblem adapter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get point info from calling activity
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            this.pointCollectionName = extras.getString("poi_type");
            this.pointDocumentId = extras.getString("poi_id");
        }

        setContentView(R.layout.activity_manager_city_problem);
        setReturnBtn();

        getPointProblem();
    }

    private void setReturnBtn() {
        Toolbar toolbar = findViewById(R.id.manager_activity_toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // https://stackoverflow.com/questions/17274441/java-how-to-handle-unchecked-cast-for-arraymyitem-from-object
    @SuppressWarnings("unchecked")
    private void getPointProblem() {
        FirebaseFirestore.getInstance().collection(pointCollectionName).document(pointDocumentId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();

                            if (document.exists()) {
                                List<Timestamp> notWorkingList = (List<Timestamp>)document.get("problem_not_working");
                                List<Timestamp> badMaintenanceList = (List<Timestamp>)document.get("problem_bad_maintenance");

                                problemObjList = new ArrayList<>();

                                if (notWorkingList != null) {
                                    for (Timestamp timestamp: notWorkingList) {
                                        problemObjList.add(new ProblemObj("problem_not_working", "Le point ne fonctionne pas", timestamp));
                                    }
                                }

                                if  (badMaintenanceList != null) {
                                    for (Timestamp timestamp: badMaintenanceList) {
                                        problemObjList.add(new ProblemObj("problem_bad_maintenance", "Le point est mal entretenu", timestamp));
                                    }
                                }

                                Collections.sort(problemObjList);
                                setRecyclerView();


                            } else {
                                Log.d("getPointProblem", "No such document");
                            }
                        } else {
                            Log.d("getPointProblem", "get failed with ", task.getException());
                        }
                    }
                });
    }

    private void setRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.manager_city_problem_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RecyclerViewAdapterProblem(this, problemObjList);
        adapter.setClickListener(this);

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(View view, final int position) {
        Timestamp timestamp = problemObjList.get(position).getTimestamp();
        String problemType = problemObjList.get(position).getProblemType();

        FirebaseFirestore.getInstance().collection(pointCollectionName).document(pointDocumentId).update(problemType, FieldValue.arrayRemove(timestamp))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        adapter.deleteItem(position);
                        FirebaseFirestore.getInstance().collection(pointCollectionName).document(pointDocumentId).update("problem", FieldValue.increment(-1));
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
