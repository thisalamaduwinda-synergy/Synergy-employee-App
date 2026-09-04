package lk.synergypharma.employee.ui.notification;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.databinding.ItemNotificationBinding;
import lk.synergypharma.employee.databinding.ItemSectionHeaderBinding;
import lk.synergypharma.employee.domain.model.AppNotification;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Notifications grouped under Today / This week / Earlier.
 *
 * <p>Grouping by age rather than by type is deliberate: what an employee wants
 * to know is whether something new happened, and how urgent it is.
 */
public final class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface OnClick {
        void onNotification(@NonNull AppNotification notification);
    }

    /** Either a section title or a notification — never both. */
    private static final class Row {
        @Nullable
        final String header;
        @Nullable
        final AppNotification notification;

        Row(@Nullable String header, @Nullable AppNotification notification) {
            this.header = header;
            this.notification = notification;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    @Nullable
    private OnClick listener;

    public void setOnClick(@Nullable OnClick listener) {
        this.listener = listener;
    }

    /**
     * @param labels three section titles, already translated:
     *               today, this week, earlier
     */
    public void submit(@NonNull List<AppNotification> notifications, @NonNull String[] labels) {
        rows.clear();

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        addSection(labels[0], notifications, today, null);
        addSection(labels[1], notifications, weekAgo, today);
        addSection(labels[2], notifications, null, weekAgo);

        notifyDataSetChanged();
    }

    /**
     * @param from inclusive lower bound, or null for "anything older"
     * @param to   exclusive upper bound, or null for "up to now"
     */
    private void addSection(@NonNull String label,
                            @NonNull List<AppNotification> all,
                            @Nullable LocalDate from,
                            @Nullable LocalDate to) {
        List<AppNotification> matching = new ArrayList<>();
        for (AppNotification n : all) {
            LocalDate date = n.timestamp.toLocalDate();
            boolean afterFrom = from == null || !date.isBefore(from);
            boolean beforeTo = to == null || date.isBefore(to);
            if (afterFrom && beforeTo) {
                matching.add(n);
            }
        }
        if (matching.isEmpty()) {
            return;
        }
        rows.add(new Row(label, null));
        for (AppNotification n : matching) {
            rows.add(new Row(null, n));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header != null ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(ItemSectionHeaderBinding.inflate(inflater, parent, false));
        }
        return new ItemHolder(ItemNotificationBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder && row.header != null) {
            ((HeaderHolder) holder).bind(row.header);
        } else if (holder instanceof ItemHolder && row.notification != null) {
            ((ItemHolder) holder).bind(row.notification, listener);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        private final ItemSectionHeaderBinding binding;

        HeaderHolder(@NonNull ItemSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull String label) {
            binding.textHeader.setText(label);
        }
    }

    static final class ItemHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        ItemHolder(@NonNull ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AppNotification item, @Nullable OnClick listener) {
            ViewUtils.iconTile(binding.icon, item.type.iconRes,
                    item.type.fgColorRes, item.type.bgColorRes);

            binding.textTitle.setText(item.title);
            ViewUtils.textOrGone(binding.textBody, item.body);
            ViewUtils.visible(binding.dotUnread, !item.read);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotification(item);
                }
            });
        }
    }
}
