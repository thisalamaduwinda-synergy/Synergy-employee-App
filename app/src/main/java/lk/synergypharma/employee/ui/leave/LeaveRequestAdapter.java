package lk.synergypharma.employee.ui.leave;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.ItemLeaveRequestBinding;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.enums.ApprovalStatus;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.ViewUtils;

/** Leave history rows. */
public final class LeaveRequestAdapter
        extends RecyclerView.Adapter<LeaveRequestAdapter.Holder> {

    private final List<LeaveRequest> items = new ArrayList<>();

    public void submit(@NonNull List<LeaveRequest> requests) {
        items.clear();
        items.addAll(requests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemLeaveRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** "2 days", "1 day", "Half day" — never "1 days". */
    @NonNull
    public static String formatDays(@NonNull Context context, float days) {
        if (days > 0f && days < 1f) {
            return context.getString(R.string.leave_half_day);
        }
        String value = days == Math.rint(days)
                ? String.valueOf((int) days)
                : String.format(Locale.getDefault(), "%.1f", days);
        return context.getString(days == 1f
                ? R.string.leave_request_days
                : R.string.leave_request_days_plural, value);
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemLeaveRequestBinding binding;

        Holder(@NonNull ItemLeaveRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull LeaveRequest request) {
            Context context = binding.getRoot().getContext();

            binding.textTitle.setText(context.getString(R.string.leave_request_summary,
                    context.getString(request.type.labelRes),
                    formatDays(context, request.days)));

            binding.textSubtitle.setText(subtitleFor(context, request));

            ViewUtils.statusChip(binding.chipStatus, request.status.labelRes,
                    request.status.fgColorRes, request.status.bgColorRes);

            binding.icon.setImageResource(iconFor(request.status));
            binding.icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, request.status.fgColorRes)));
            ViewUtils.tintBackground(binding.icon, request.status.bgColorRes);
        }

        private static int iconFor(@NonNull ApprovalStatus status) {
            switch (status) {
                case APPROVED:
                    return R.drawable.ic_check;
                case REJECTED:
                    return R.drawable.ic_warning;
                default:
                    return R.drawable.ic_clock;
            }
        }

        @NonNull
        private static String subtitleFor(@NonNull Context context, @NonNull LeaveRequest r) {
            String span = r.isSingleDay()
                    ? DateTimeUtils.shortDate(r.fromDate)
                    : DateTimeUtils.shortDate(r.fromDate) + " – " + DateTimeUtils.shortDate(r.toDate);

            switch (r.status) {
                case APPROVED:
                    return r.approver == null
                            ? span
                            : context.getString(R.string.leave_approved_by, span, r.approver);
                case REJECTED:
                    // The reason for a rejection is the only useful part of it.
                    return r.decisionNote == null
                            ? span
                            : context.getString(R.string.leave_rejected_reason, span, r.decisionNote);
                default:
                    return r.approver == null
                            ? span
                            : context.getString(R.string.leave_with_approver, span, r.approver);
            }
        }
    }
}
