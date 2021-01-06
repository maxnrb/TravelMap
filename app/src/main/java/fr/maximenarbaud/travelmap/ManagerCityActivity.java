package fr.maximenarbaud.travelmap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ManagerCityActivity extends AppCompatActivity implements RecyclerViewAdapter.ItemClickListener, AdapterView.OnItemClickListener {
    private RecyclerViewAdapter adapter;
    private List<RecyclerElement> recyclerElementList;

    private final Map<String, Point> poiMap = new LinkedHashMap<>();

    private List<String> cityList = null;
    private String selectedCity = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle("City Manager");

        setContentView(R.layout.activity_manager_city);
        setReturnButton();

        this.recyclerElementList = new ArrayList<>();

        poiMap.put("Point d'eau", new Point(true, R.drawable.ic_water_marker, R.id.menu_item_water_tap, R.drawable.ic_faucet, "water_point"));
        poiMap.put("Toilette", new Point(true, R.drawable.ic_toilet_hf_marker, R.id.menu_item_toilette,  R.drawable.ic_wc, "toilet"));
        poiMap.put("Poubelle", new Point(false, R.drawable.ic_bin_marker, R.id.menu_item_bin, R.drawable.ic_trash, "bin"));

        setCitySelector();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (selectedCity != null) {
            setPointList();
        }
    }

    private void setReturnButton() {
        Toolbar toolbar = findViewById(R.id.materialToolBar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // https://stackoverflow.com/questions/17274441/java-how-to-handle-unchecked-cast-for-arraymyitem-from-object
    @SuppressWarnings("unchecked")
    private void setCitySelector() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();

                                if (document.exists()) {
                                    Long right_level = document.getLong("right_level");

                                    if (right_level == null || right_level < FirestoreUtils.CITY_MANAGER_LEVEL) {
                                        finish();
                                    }

                                    cityList = (List<String>)document.get("city");
                                    setCityAdapter();
                                }
                            }
                        }
                    });
        } else {
            finish();
        }
    }

    private void setCityAdapter() {
        if (this.cityList != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_menu_item, this.cityList.toArray(new String[0]));

            AutoCompleteTextView editTextFilledExposedDropdown = findViewById(R.id.add_point_selector_textView);

            editTextFilledExposedDropdown.setAdapter(adapter);
            editTextFilledExposedDropdown.setOnItemClickListener(this);
        }
    }

    // Respond to "onItemClick" from city selector Adapter
    @Override
    public void onItemClick(final AdapterView<?> parent, View view, int position, long id) {
        selectedCity = cityList.get(position);
        setPointList();
    }

    private void setPointList() {
        if (adapter != null) {
            this.recyclerElementList.clear();
            adapter.notifyDataSetChanged();
        }

        for (final String key : poiMap.keySet()) {

            final Point point = poiMap.get(key);
            final String collectionPath;

            if (point != null) {
                point.setCity(selectedCity);
                point.initCounter();

                collectionPath = point.getPoiType();

                FirebaseFirestore.getInstance().collection(collectionPath)
                        .whereEqualTo("city", selectedCity)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                Long nbProblem = 0L;
                                String problemLine = "Aucun problème signalé";
                                int backgroundTint = 0xFF9CCC65;

                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        nbProblem += document.getLong("problem");

                                        point.incrementCounter();
                                    }
                                } else {
                                    Log.d("Get city point", "Error getting documents: ", task.getException());
                                }

                                if (nbProblem == 1) {
                                    problemLine = "1 problème signalé";
                                    backgroundTint = 0xFFFFA726;

                                } else if (nbProblem > 1) {
                                    problemLine = nbProblem + " problèmes signalés";
                                    backgroundTint = 0xFFDB4437;

                                }

                                recyclerElementList.add(new RecyclerElement(key, problemLine,
                                        point.getIconResDrawable(), "Nb : " + point.getCounter(), backgroundTint, null));

                                setRecyclerView();
                            }
                        });

            }
        }
    }


    private void setRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.manager_home_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RecyclerViewAdapter(this, recyclerElementList);
        adapter.setClickListener(this);

        recyclerView.setAdapter(adapter);
    }

    // Respond to "onItemClick" from RecyclerViewAdapter (selected point type)
    @Override
    public void onItemClick(View view, int position) {

        Intent intent = new Intent(this, ElementListActivity.class);

        Point point = poiMap.get(recyclerElementList.get(position).getFirstLine());

        if (point != null) {
            intent.putExtra("type", point.getPoiType());
            intent.putExtra("city", point.getCity());
            intent.putExtra("iconResId", point.getIconResDrawable());
        }

        startActivity(intent);
    }

}