package com.invoisuite.app.session;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.invoisuite.app.model.User;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;

/** Persists the JWT, the signed-in user, and the last-selected company across app restarts. */
public class SessionManager {

    private static final String PREFS_NAME = "invoisuite_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER = "user";
    private static final String KEY_SELECTED_COMPANY_ID = "selected_company_id";

    private static volatile SessionManager instance;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    private SessionManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to create encrypted session storage", e);
        }
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void saveSession(String token, User user) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER, gson.toJson(user))
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        return json != null ? gson.fromJson(json, User.class) : null;
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void setSelectedCompanyId(Long companyId) {
        prefs.edit().putLong(KEY_SELECTED_COMPANY_ID, companyId).apply();
    }

    public long getSelectedCompanyId() {
        return prefs.getLong(KEY_SELECTED_COMPANY_ID, -1);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
