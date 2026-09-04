package lk.synergypharma.employee.domain.model.enums;

/**
 * Who is holding the phone.
 *
 * <p>There is deliberately <b>one</b> app and <b>one</b> login. A manager is also
 * an employee — they still need their own gate log, OT and leave balance — so a
 * separate manager app would mean two credential sets and two Play Store
 * listings for the same person. The role arrives from the server on login (and
 * later, as a JWT claim) and simply reveals extra destinations.
 *
 * <p>Never trust this for anything security-critical: the server must re-check
 * the role on every approval call. Here it only decides what is on screen.
 */
public enum UserRole {

    EMPLOYEE,

    /** Sees the Approvals queue on top of everything an EMPLOYEE sees. */
    SUPERVISOR;

    public static UserRole fromApi(String raw) {
        if (raw == null) {
            return EMPLOYEE;
        }
        switch (raw.trim().toUpperCase()) {
            case "SUPERVISOR":
            case "MANAGER":
            case "HOD":
                return SUPERVISOR;
            default:
                return EMPLOYEE;
        }
    }

    public boolean canApprove() {
        return this == SUPERVISOR;
    }
}
