package lk.synergypharma.employee.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lk.synergypharma.employee.BuildConfig;
import lk.synergypharma.employee.data.local.PrefsManager;
import lk.synergypharma.employee.data.remote.dto.ApiResponse;
import lk.synergypharma.employee.data.remote.dto.LoginDto;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds the one {@link ApiService} the app talks to.
 *
 * <p>Two OkHttp hooks do the token work so no repository has to think about it:
 * <ul>
 *   <li>an interceptor adds {@code Authorization: Bearer …} from
 *       {@link PrefsManager} to every call except the auth ones;</li>
 *   <li>an {@link Authenticator} answers a 401 by calling
 *       {@code POST me/auth/refresh} once, saving the new pair and retrying.
 *       If the refresh fails too the session is cleared, which sends the
 *       employee back to the login screen.</li>
 * </ul>
 */
public final class ApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private ApiClient() {
    }

    @NonNull
    public static ApiService create(@NonNull PrefsManager prefs) {
        Gson gson = new Gson();

        // A bare client for the refresh call, so a refresh never triggers
        // another refresh.
        OkHttpClient plain = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        ApiService refreshService = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(plain)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService.class);

        Interceptor bearer = chain -> {
            Request original = chain.request();
            String token = prefs.accessToken();
            if (token == null || isAuthCall(original)) {
                return chain.proceed(original);
            }
            return chain.proceed(original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build());
        };

        Authenticator refresher = new Authenticator() {
            @Nullable
            @Override
            public Request authenticate(@Nullable Route route, @NonNull Response response) {
                if (isAuthCall(response.request()) || responseCount(response) >= 2) {
                    return null; // give up; the 401 propagates to the caller
                }
                String refreshToken = prefs.refreshToken();
                if (refreshToken == null) {
                    return null;
                }
                synchronized (ApiClient.class) {
                    // Another call may have refreshed while we waited.
                    String current = prefs.accessToken();
                    String sent = response.request().header("Authorization");
                    if (current != null && sent != null && !sent.equals("Bearer " + current)) {
                        return withToken(response.request(), current);
                    }
                    try {
                        RequestBody body = RequestBody.create(
                                "{\"refresh_token\":\"" + refreshToken + "\"}", JSON);
                        retrofit2.Response<ApiResponse<LoginDto.Response>> r =
                                refreshService.refresh(body).execute();
                        ApiResponse<LoginDto.Response> env = r.body();
                        if (!r.isSuccessful() || env == null || env.data == null) {
                            prefs.clearSession();
                            return null;
                        }
                        prefs.saveSession(env.data.accessToken, env.data.refreshToken,
                                env.data.employee);
                        return withToken(response.request(), env.data.accessToken);
                    } catch (IOException e) {
                        return null;
                    }
                }
            }
        };

        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(bearer)
                .authenticator(refresher);
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            // Headers would print the bearer token into logcat.
            log.setLevel(HttpLoggingInterceptor.Level.BASIC);
            client.addInterceptor(log);
        }

        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService.class);
    }

    private static boolean isAuthCall(@NonNull Request request) {
        return request.url().encodedPath().contains("/me/auth/");
    }

    @NonNull
    private static Request withToken(@NonNull Request request, @NonNull String token) {
        return request.newBuilder().header("Authorization", "Bearer " + token).build();
    }

    private static int responseCount(@NonNull Response response) {
        int n = 1;
        Response prior = response.priorResponse();
        while (prior != null) {
            n++;
            prior = prior.priorResponse();
        }
        return n;
    }
}
