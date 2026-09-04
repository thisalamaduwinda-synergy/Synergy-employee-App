package lk.synergypharma.employee.ui.attendance;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.ItemCalendarDayBinding;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.domain.model.MonthSummary;

/**
 * The month grid. One colour per day, taken straight off {@code DayState}, so
 * the calendar can never disagree with the list below it or the counters above.
 */
public final class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.Holder> {

    /** Weeks start on Monday here, matching the roster and the header row. */
    public static final int COLUMNS = 7;

    public interface OnDayClick {
        void onDay(@NonNull AttendanceDay day);
    }

    /** A grid slot: either a real day, or blank padding before the 1st. */
    private static final class Cell {
        @Nullable
        final AttendanceDay day;
        final int dayOfMonth;
        final boolean today;

        Cell(@Nullable AttendanceDay day, int dayOfMonth, boolean today) {
            this.day = day;
            this.dayOfMonth = dayOfMonth;
            this.today = today;
        }

        static Cell padding() {
            return new Cell(null, 0, false);
        }
    }

    private final List<Cell> cells = new ArrayList<>();
    @Nullable
    private OnDayClick listener;

    public void setOnDayClick(@Nullable OnDayClick listener) {
        this.listener = listener;
    }

    public void submit(@NonNull MonthSummary summary) {
        cells.clear();

        YearMonth month = summary.month;
        LocalDate today = LocalDate.now();

        // Monday = 1, so the 1st of a month falling on a Thursday needs three
        // blank slots before it.
        int leading = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leading; i++) {
            cells.add(Cell.padding());
        }

        for (AttendanceDay day : summary.days) {
            cells.add(new Cell(day, day.date.getDayOfMonth(), day.date.equals(today)));
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCalendarDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(cells.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemCalendarDayBinding binding;

        Holder(@NonNull ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Cell cell, @Nullable OnDayClick listener) {
            View view = binding.cell;
            if (cell.day == null) {
                binding.cell.setText("");
                tint(R.color.calendar_off, R.color.calendar_off_text);
                view.setOnClickListener(null);
                view.setClickable(false);
                return;
            }

            binding.cell.setText(String.valueOf(cell.dayOfMonth));

            if (cell.today) {
                // Today outranks its own state: the employee is looking for it.
                tint(R.color.synergy_blue_700, R.color.white);
            } else {
                tint(cell.day.state.bgColorRes, cell.day.state.fgColorRes);
            }

            view.setClickable(true);
            view.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDay(cell.day);
                }
            });
        }

        private void tint(int backgroundRes, int textRes) {
            binding.cell.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(binding.getRoot().getContext(), backgroundRes)));
            binding.cell.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), textRes));
        }
    }
}
