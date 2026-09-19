package com.invoisuite.app.api;

import android.content.Context;
import android.content.Intent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.invoisuite.app.BuildConfig;
import com.invoisuite.app.common.SessionEvents;
import com.invoisuite.app.session.SessionManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/** Builds a singleton Retrofit/OkHttp client that attaches the JWT and reacts to 401s. */
public final class ApiClient {

    private static volatile ApiService service;

    private ApiClient() {
    }

    public static ApiService get(Context context) {
        if (service == null) {
            synchronized (ApiClient.class) {
                if (service == null) {
                    service = build(context.getApplicationContext());
                }
            }
        }
        return service;
    }

    private static ApiService build(Context appContext) {
        SessionManager session = SessionManager.getInstance(appContext);

        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            String token = session.getToken();
            if (token == null) {
                return chain.proceed(original);
            }
            Request authorized = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(authorized);
        };

        Interceptor sessionExpiryInterceptor = chain -> {
            Response response = chain.proceed(chain.request());
            if (response.code() == 401) {
                session.clear();
                appContext.sendBroadcast(new Intent(SessionEvents.ACTION_SESSION_EXPIRED)
                        .setPackage(appContext.getPackageName()));
            }
            return response;
        };

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(sessionExpiryInterceptor)
                .addInterceptor(logging)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(ApiService.class);
    }
}
