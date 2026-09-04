package lk.synergypharma.employee.ui.attendance;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.ItemAttendanceDayBinding;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.domain.model.enums.DayState;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.ViewUtils;

/** The "Daily record" list under the calendar. */
public final class AttendanceDayAdapter
        extends RecyclerView.Adapter<AttendanceDayAdapter.Holder> {

    public interface OnDayClick {
        void onDay(@NonNull AttendanceDay day);
    }

    private final List<AttendanceDay> items = new ArrayList<>();
    @Nullable
    private OnDayClick listener;

    public void setOnDayClick(@Nullable OnDayClick listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<AttendanceDay> days) {
        items.clear();
        items.addAll(days);
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

        void bind(@NonNull AttendanceDay day, @Nullable OnDayClick listener) {
            ViewUtils.avatar(binding.textDay,
                    String.format(java.util.Locale.getDefault(), "%02d", day.date.getDayOfMonth()),
                    day.state.fgColorRes, day.state.bgColorRes);

            binding.textTitle.setText(DateTimeUtils.dayAndMonth(day.date));
            binding.textSubtitle.setText(subtitleFor(day));

            // An absent day already explained reads differently from one still
            // owed — same colour, different message.
            if (day.state == DayState.ABSENT && day.reasonSubmitted) {
                ViewUtils.statusChip(binding.chipState, R.string.status_pending,
                        R.color.slate, R.color.slate_bg);
            } else if (day.state == DayState.WORKED && day.overtimeMinutes > 0) {
                ViewUtils.statusChip(binding.chipState,
                        binding.getRoot().getContext().getString(R.string.gate_chip_ot,
                                DateTimeUtils.duration(day.overtimeMinutes)),
                        R.color.slate, R.color.slate_bg);
            } else {
                ViewUtils.statusChip(binding.chipState, day.state.labelRes,
                        day.state.fgColorRes, day.state.bgColorRes);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDay(day);
                }
            });
        }

        @NonNull
        private String subtitleFor(@NonNull AttendanceDay day) {
            if (day.note != null && !day.note.isEmpty()) {
                return day.note;
            }
            if (day.state == DayState.ABSENT) {
                return binding.getRoot().getContext()
                        .getString(R.string.attendance_reason_pending);
            }
            if (!day.hasGateRecord()) {
                return binding.getRoot().getContext().getString(day.state.labelRes);
            }

            String span = DateTimeUtils.time(day.firstIn()) + " – "
                    + (day.isStillInside()
                    ? binding.getRoot().getContext().getString(R.string.value_placeholder)
                    : DateTimeUtils.time(day.lastOut()));
            String worked = DateTimeUtils.duration(day.workedMinutes());
            if (day.overtimeMinutes > 0) {
                return span + " · " + worked + " · OT "
                        + DateTimeUtils.duration(day.overtimeMinutes);
            }
            return span + " · " + worked;
        }
    }
}
