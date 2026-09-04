package lk.synergypharma.employee.ui.overtime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.ItemAttendanceDayBinding;
import lk.synergypharma.employee.domain.model.Overtime;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * OT per day.
 *
 * <p>Shares {@code item_attendance_day} with the attendance list on purpose —
 * the two rows carry the same four things (day number, date, one line of
 * detail, a status pill), and keeping one layout keeps them looking identical.
 */
public final class OvertimeAdapter extends RecyclerView.Adapter<OvertimeAdapter.Holder> {

    public interface OnEntryClick {
        void onEntry(@NonNull LocalDate date);
    }

    private final List<Overtime.Entry> items = new ArrayList<>();
    @Nullable
    private OnEntryClick listener;

    public void setOnEntryClick(@Nullable OnEntryClick listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<Overtime.Entry> entries) {
        items.clear();
        items.addAll(entries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAttendanceDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemAttendanceDayBinding binding;

        Holder(@NonNull ItemAttendanceDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Overtime.Entry entry, @Nullable OnEntryClick listener) {
            Context context = binding.getRoot().getContext();

            ViewUtils.avatar(binding.textDay,
                    String.format(Locale.getDefault(), "%02d", entry.date.getDayOfMonth()),
                    entry.status.fgColorRes, entry.status.bgColorRes);

            binding.textTitle.setText(DateTimeUtils.dayAndMonth(entry.date));
            binding.textSubtitle.setText(context.getString(R.string.overtime_day_summary,
                    DateTimeUtils.time(entry.outTime),
                    DateTimeUtils.duration(entry.minutes)));

            ViewUtils.statusChip(binding.chipState, entry.status.labelRes,
                    entry.status.fgColorRes, entry.status.bgColorRes);

            // Every OT figure traces back to a gate OUT time — let them check it.
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEntry(entry.date);
                }
            });
        }
    }
}
