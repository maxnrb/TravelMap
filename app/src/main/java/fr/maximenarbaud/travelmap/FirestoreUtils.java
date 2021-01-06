package fr.maximenarbaud.travelmap;


import android.view.Menu;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class FirestoreUtils {
    public static final int BASIC_USER_LEVEL = 0;
    public static final int CITY_MANAGER_LEVEL = 1;
    public static final int SUPER_ADMIN_LEVEL = 2;


    public static void setInterfaceByRightLevel(final Menu menu) {
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

                                    if (right_level != null) {
                                        if (right_level >= SUPER_ADMIN_LEVEL) {
                                            menu.findItem(R.id.menu_item_user_manager).setVisible(true);
                                            menu.findItem(R.id.menu_item_city_manager).setVisible(true);

                                        } else if (right_level >= CITY_MANAGER_LEVEL) {
                                            menu.findItem(R.id.menu_item_city_manager).setVisible(true);

                                        } else {
                                            hideMenuManager(menu);
                                        }
                                    } else {
                                        hideMenuManager(menu);
                                    }
                                }
                            }
                        }
                    });
        } else {
            hideMenuManager(menu);
        }
    }

    private static void hideMenuManager(Menu menu) {
        menu.findItem(R.id.menu_item_city_manager).setVisible(false);
        menu.findItem(R.id.menu_item_user_manager).setVisible(false);
    }
}
