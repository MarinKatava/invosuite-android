package com.invoisuite.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.company.CompanyListActivity;
import com.invoisuite.app.databinding.ActivityRegisterBinding;
import com.invoisuite.app.model.AuthResponse;
import com.invoisuite.app.model.RegisterRequest;
import com.invoisuite.app.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private ApiService api;
    private SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        session = SessionManager.getInstance(this);

        binding.registerButton.setOnClickListener(v -> register());
    }

    private void register() {
        String fullName = text(binding.fullNameInput.getText());
        String email = text(binding.emailInput.getText());
        String password = text(binding.passwordInput.getText());

        if (fullName.isEmpty() || email.isEmpty() || password.length() < 8) {
            Toast.makeText(this, "Fill in your name, email and an 8+ character password", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        api.register(new RegisterRequest(email, password, fullName)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    session.saveSession(response.body().token(), response.body().user());
                    startActivity(new Intent(RegisterActivity.this, CompanyListActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, ApiErrors.message(response, "Registration failed"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.registerButton.setEnabled(!loading);
    }

    private String text(@Nullable android.text.Editable editable) {
        return editable == null ? "" : editable.toString().trim();
    }
}
