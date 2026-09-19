package com.invoisuite.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.invoisuite.app.R;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.company.CompanyListActivity;
import com.invoisuite.app.databinding.ActivityLoginBinding;
import com.invoisuite.app.model.AuthResponse;
import com.invoisuite.app.model.GoogleAuthRequest;
import com.invoisuite.app.model.LoginRequest;
import com.invoisuite.app.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_EXPIRED = "session_expired";

    private ActivityLoginBinding binding;
    private ApiService api;
    private SessionManager session;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data == null) {
                    return;
                }
                GoogleSignIn.getSignedInAccountFromIntent(data)
                        .addOnSuccessListener(this::onGoogleSignInSuccess)
                        .addOnFailureListener(e -> showError("Google sign-in failed: " + e.getLocalizedMessage()));
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        session = SessionManager.getInstance(this);

        if (session.isLoggedIn() && !getIntent().getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            goToCompanies();
            return;
        }
        if (getIntent().getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            Toast.makeText(this, "Your session expired, please sign in again", Toast.LENGTH_LONG).show();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.loginButton.setOnClickListener(v -> login());
        binding.googleButton.setOnClickListener(v -> googleLauncher.launch(googleSignInClient.getSignInIntent()));
        binding.registerButton.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String email = text(binding.emailInput.getText());
        String password = text(binding.passwordInput.getText());
        if (email.isEmpty() || password.isEmpty()) {
            showError("Enter your email and password");
            return;
        }

        setLoading(true);
        api.login(new LoginRequest(email, password)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    onAuthenticated(response.body());
                } else {
                    showError(ApiErrors.message(response, "Sign in failed"));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    private void onGoogleSignInSuccess(GoogleSignInAccount account) {
        String idToken = account.getIdToken();
        if (idToken == null) {
            showError("Google sign-in did not return a token");
            return;
        }
        setLoading(true);
        api.loginWithGoogle(new GoogleAuthRequest(idToken)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    onAuthenticated(response.body());
                } else {
                    showError(ApiErrors.message(response, "Google sign-in failed"));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    private void onAuthenticated(AuthResponse auth) {
        session.saveSession(auth.token(), auth.user());
        goToCompanies();
    }

    private void goToCompanies() {
        startActivity(new Intent(this, CompanyListActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.loginButton.setEnabled(!loading);
        binding.googleButton.setEnabled(!loading);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String text(@Nullable android.text.Editable editable) {
        return editable == null ? "" : editable.toString().trim();
    }
}
