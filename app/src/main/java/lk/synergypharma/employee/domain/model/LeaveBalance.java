package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import lk.synergypharma.employee.domain.model.enums.LeaveType;

/**
 * Entitlement and usage per bucket for one calendar year.
 *
 * <p>Every number here comes down from the HR database. Nothing is calculated on
 * the phone — if payroll and the app ever disagree, payroll must win.
 */
public final class LeaveBalance {

    public static final class Bucket {
        @NonNull
        public final LeaveType type;
        /** Days already taken or approved. */
        public final float used;
        /** Days granted for the year. */
        public final float entitled;

        public Bucket(@NonNull LeaveType type, float used, float entitled) {
            this.type = type;
            this.used = used;
            this.entitled = entitled;
        }

        public float remaining() {
            return Math.max(0f, entitled - used);
        }

        /** 0–100, for the progress bar. */
        public int usedPercent() {
            if (entitled <= 0f) {
                return 0;
            }
            return Math.round(Math.min(1f, used / entitled) * 100f);
        }
    }

    public final int year;
    @NonNull
    public final List<Bucket> buckets;

    public LeaveBalance(int year, @NonNull List<Bucket> buckets) {
        this.year = year;
        this.buckets = Collections.unmodifiableList(buckets);
    }

    @Nullable
    public Bucket bucketOf(@NonNull LeaveType type) {
        for (Bucket b : buckets) {
            if (b.type == type) {
                return b;
            }
        }
        return null;
    }

    /** Total days left across every bucket that has an entitlement. */
    public float totalRemaining() {
        float total = 0f;
        for (Bucket b : buckets) {
            if (b.type.hasBalance()) {
                total += b.remaining();
            }
        }
        return total;
    }
}
