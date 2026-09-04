package lk.synergypharma.employee.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.gson.Gson;

import lk.synergypharma.employee.data.remote.dto.EmployeeDto;

/**
 * The only place anything is persisted on the phone.
 *
 * <p>Backed by {@link EncryptedSharedPreferences}, because this file holds the
 * access token and a cached copy of the employee's HR record. Plain
 * SharedPreferences on a rooted phone is a plain text file.
 *
 * <p>If the Android keystore is unusable — which does happen on a few cheap
 * devices after a factory reset — the class falls back to ordinary preferences
 * and <b>refuses to write the token to disk at all</b>, holding it in memory for
 * the session instead. The employee logs in again next launch; nothing leaks.
 */
public final class PrefsManager {

    private static final String TAG = "PrefsManager";
    private static final String FILE_SECURE = "synergy_secure_prefs";
    private static final String FILE_PLAIN = "synergy_prefs";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EMPLOYEE = "employee_json";
    private static final String KEY_LAST_EMPLOYEE_ID = "last_employee_id";
    private static final String KEY_LANGUAGE = "language_tag";
    private static final String KEY_BIOMETRIC = "biometric_enabled";

    @NonNull
    private final SharedPreferences prefs;
    /** False when we fell back to unencrypted storage. */
    private final boolean encrypted;
    @NonNull
    private final Gson gson = new Gson();

    @Nullable
    private String inMemoryAccessToken;
    @Nullable
    private String inMemoryRefreshToken;

    public PrefsManager(@NonNull Context context) {
        SharedPreferences resolved;
        boolean isEncrypted;
        Context app = context.getApplicationContext();
        try {
            String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            resolved = EncryptedSharedPreferences.create(
                    FILE_SECURE,
                    masterKey,
                    app,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            isEncrypted = true;
        } catch (Exception e) {
            Log.w(TAG, "Keystore unavailable — tokens will stay in memory only", e);
            resolved = app.getSharedPreferences(FILE_PLAIN, Context.MODE_PRIVATE);
            isEncrypted = false;
        }
        this.prefs = resolved;
        this.encrypted = isEncrypted;
    }

    // ---------------------------------------------------------------- tokens

    public void saveSession(@NonNull String accessToken,
                            @NonNull String refreshToken,
                            @NonNull EmployeeDto employee) {
        inMemoryAccessToken = accessToken;
        inMemoryRefreshToken = refreshToken;

        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_LAST_EMPLOYEE_ID, employee.employeeCode);
        if (encrypted) {
            editor.putString(KEY_ACCESS_TOKEN, accessToken)
                    .putString(KEY_REFRESH_TOKEN, refreshToken)
                    .putString(KEY_EMPLOYEE, gson.toJson(employee));
        }
        editor.apply();
    }

    @Nullable
    public String accessToken() {
        if (inMemoryAccessToken != null) {
            return inMemoryAccessToken;
        }
        return encrypted ? prefs.getString(KEY_ACCESS_TOKEN, null) : null;
    }

    @Nullable
    public String refreshToken() {
        if (inMemoryRefreshToken != null) {
            return inMemoryRefreshToken;
        }
        return encrypted ? prefs.getString(KEY_REFRESH_TOKEN, null) : null;
    }

    public boolean isLoggedIn() {
        return accessToken() != null;
    }

    /** Cached HR record, so the app can render before the first network call. */
    @Nullable
    public EmployeeDto cachedEmployee() {
        if (!encrypted) {
            return null;
        }
        String json = prefs.getString(KEY_EMPLOYEE, null);
        if (json == null) {
            return null;
        }
        try {
            return gson.fromJson(json, EmployeeDto.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Clears the session but keeps the language choice and the last employee
     * number — re-typing an EPF number every morning is exactly the friction
     * that stops people using an app.
     */
    public void clearSession() {
        inMemoryAccessToken = null;
        inMemoryRefreshToken = null;
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_EMPLOYEE)
                .remove(KEY_BIOMETRIC)
                .apply();
    }

    // -------------------------------------------------------------- settings

    @Nullable
    public String lastEmployeeId() {
        return prefs.getString(KEY_LAST_EMPLOYEE_ID, null);
    }

    /** Null means "follow the phone's language". */
    @Nullable
    public String languageTag() {
        return prefs.getString(KEY_LANGUAGE, null);
    }

    public void setLanguageTag(@Nullable String tag) {
        if (tag == null) {
            prefs.edit().remove(KEY_LANGUAGE).apply();
        } else {
            prefs.edit().putString(KEY_LANGUAGE, tag).apply();
        }
    }

    /**
     * Only ever true after at least one successful password login — biometric
     * unlock re-opens an existing session, it never creates one.
     */
    public boolean isBiometricEnabled() {
        return encrypted && prefs.getBoolean(KEY_BIOMETRIC, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        if (!encrypted) {
            return;
        }
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply();
    }
}
