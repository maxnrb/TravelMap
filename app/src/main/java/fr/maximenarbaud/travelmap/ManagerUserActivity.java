package fr.maximenarbaud.travelmap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManagerUserActivity extends AppCompatActivity implements RecyclerViewAdapterCity.ItemClickListener, AdapterView.OnItemClickListener {
    private AutoCompleteTextView editTextDropdown;
    private List<CityObj> cityList;
    private List<String> userSelectedCity;

    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manager_user);

        setReturnButton();

        String[] rightLevelArray = new String[] {
                "Utilisateur lambda",               // right_level: 0
                "Agent municipal",                  // right_level: 1
                "Administrateur"                    // right_level: 2
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_menu_item, rightLevelArray);

        this.editTextDropdown = findViewById(R.id.right_level_selector_textView);
        this.editTextDropdown.setAdapter(adapter);

        this.editTextDropdown.setOnItemClickListener(this);

        cityList = new ArrayList<>();
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

    private void setRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.manager_city_user_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        RecyclerViewAdapterCity adapter = new RecyclerViewAdapterCity(this, cityList);
        adapter.setClickListener(this);

        recyclerView.setAdapter(adapter);
    }

    // https://stackoverflow.com/questions/17274441/java-how-to-handle-unchecked-cast-for-arraymyitem-from-object
    @SuppressWarnings("unchecked")
    public void searchUser(View view) {
        final TextInputLayout textInputLayout = findViewById(R.id.email_textLayout);
        textInputLayout.setErrorEnabled(false);

        findViewById(R.id.right_level_selector_textLayout).setVisibility(View.GONE);
        findViewById(R.id.manager_user_city_select_title_textView).setVisibility(View.GONE);
        findViewById(R.id.manager_city_user_recycler_view).setVisibility(View.GONE);


        EditText editText = findViewById(R.id.email_editText);
        String email = editText.getText().toString();

        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                Log.e("Search user", "No user found");

                                textInputLayout.setErrorEnabled(true);
                                textInputLayout.setError(getString(R.string.no_user_found_str));

                            } else {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Toast.makeText(getApplicationContext(), "Utilisateur trouvé", Toast.LENGTH_SHORT).show();

                                    currentUserId = document.getId();

                                    findViewById(R.id.email_textLayout).clearFocus();
                                    closeKeyboard();

                                    Long rightLevel = document.getLong("right_level");

                                    if (rightLevel != null) {
                                        editTextDropdown.setText(editTextDropdown.getAdapter().getItem(Math.toIntExact(rightLevel)).toString(), false);
                                    }

                                    findViewById(R.id.right_level_selector_textLayout).setVisibility(View.VISIBLE);

                                    userSelectedCity = (List<String>)document.get("city");
                                    loadCityList();

                                    findViewById(R.id.manager_user_city_select_title_textView).setVisibility(View.VISIBLE);
                                    findViewById(R.id.manager_city_user_recycler_view).setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            Log.e("Search user", "Error getting documents: ", task.getException());
                        }
                    }
                });

    }


    public void loadCityList() {
        FirebaseFirestore.getInstance().collection("city").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                Log.e("loadCityList", "Aucune ville trouvée");

                            } else {
                                cityList.clear();

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String cityName = document.getId();

                                    if (userSelectedCity != null) {
                                        cityList.add(new CityObj(cityName, userSelectedCity.contains(cityName)));

                                    } else {
                                        cityList.add(new CityObj(cityName, false));
                                    }
                                }

                                setRecyclerView();
                            }
                        } else {
                            Log.e("Search user", "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

    public void closeKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    @Override
    public void onItemClick(View view, int position) {
        CityObj city = cityList.get(position);

        if (city.getSelected()) {
            FirebaseFirestore.getInstance().collection("users").document(currentUserId).update("city", FieldValue.arrayRemove(city.getCityName()));
            city.setSelected(false);

        } else {
            FirebaseFirestore.getInstance().collection("users").document(currentUserId).update("city", FieldValue.arrayUnion(city.getCityName()));
            city.setSelected(true);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FirebaseFirestore.getInstance().collection("users").document(currentUserId).update("right_level", position);
    }
}
