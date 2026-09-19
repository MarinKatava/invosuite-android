package com.invoisuite.app.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.invoisuite.app.auth.LoginActivity;

/** Every screen other than login/register extends this so an expired/invalid JWT bounces to LoginActivity. */
public abstract class BaseActivity extends AppCompatActivity {

    private final BroadcastReceiver sessionExpiredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent loginIntent = new Intent(BaseActivity.this, LoginActivity.class)
                    .putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, true)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(
                this,
                sessionExpiredReceiver,
                new IntentFilter(SessionEvents.ACTION_SESSION_EXPIRED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(sessionExpiredReceiver);
    }
}
