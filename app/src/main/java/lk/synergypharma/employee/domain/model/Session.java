package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;

/** What a successful login returns: a token plus who the token belongs to. */
public final class Session {

    @NonNull
    public final String accessToken;
    @NonNull
    public final String refreshToken;
    @NonNull
    public final Employee employee;

    public Session(@NonNull String accessToken,
                   @NonNull String refreshToken,
                   @NonNull Employee employee) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.employee = employee;
    }
}
