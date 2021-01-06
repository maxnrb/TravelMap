package fr.maximenarbaud.travelmap;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            TextView nameTextView = findViewById(R.id.user_name_textView);
            TextView mailTextView = findViewById(R.id.user_email_textView);
            ImageView profilePicture = findViewById(R.id.profile_picture_imageView);

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

            if (user.getDisplayName() != null) {
                nameTextView.setText(user.getDisplayName());
            }

            if (user.getEmail() != null) {
                mailTextView.setText(user.getEmail());
            }
        } else {
            finish();
        }

        Toolbar toolbar = findViewById(R.id.materialToolBar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public void disconnectBtnClick(View view) {
        firebaseDisconnect();
    }

    public void deleteAccountBtnClick(View view) {
        showDeleteAccountDialog(false);
    }

    private void firebaseDisconnect() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        public void onComplete(@NonNull Task<Void> task) {
                            // User is now signed out
                            Toast.makeText(getApplicationContext(), R.string.snack_disconnect_str, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        } else {
            // User is already not connected
            Toast.makeText(this, R.string.toast_not_connect_str, Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseDeleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user != null) {
            user.delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), R.string.toast_account_success_delete_str, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });
        }
    }

    private void showDeleteAccountDialog(boolean passwordError) {
        LayoutInflater inflater = getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.dialog_acount_delete, null);
        final TextInputEditText editText = dialogView.findViewById(R.id.password_confirmation_text_input);
        final TextInputLayout textInputLayout = dialogView.findViewById(R.id.outlinedTextField);

        if (passwordError) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError("Mot de passe incorrect");
        }

        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom)
                .setView(dialogView)
                .setTitle(R.string.delete_account_dialog_title)
                .setNegativeButton(R.string.delete_account_dialog_negative_btn, null)
                .setPositiveButton(R.string.delete_account_dialog_positive_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //closeKeyboard();

                        // Get password from pop-up
                        CharSequence password = editText.getText();

                        firebaseReAuthAndDelete(String.valueOf(password));
                    }
                })
                .show();

        //editText.requestFocus();
        //showKeyboard();
    }

//    public void showKeyboard() {
//        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
//    }
//
//    public void closeKeyboard() {
//        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
//    }

    private void firebaseReAuthAndDelete(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user != null && user.getEmail() != null) {
            if(!password.equals("")) {
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

                user.reauthenticate(credential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()) {
                                    firebaseDeleteAccount();
                                }
                            }
                        });
            }

            showDeleteAccountDialog(true);
        }
    }

}
