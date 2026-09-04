package lk.synergypharma.employee.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.ItemAnnouncementBinding;
import lk.synergypharma.employee.domain.model.Announcement;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.ViewUtils;

/** Announcement feed rows on the home screen. */
public final class AnnouncementAdapter
        extends RecyclerView.Adapter<AnnouncementAdapter.Holder> {

    private final List<Announcement> items = new ArrayList<>();

    public void submit(@NonNull List<Announcement> announcements) {
        items.clear();
        items.addAll(announcements);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAnnouncementBinding.inflate(
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

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemAnnouncementBinding binding;

        Holder(@NonNull ItemAnnouncementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Announcement item) {
            binding.textTitle.setText(item.title);
            binding.textMeta.setText(binding.getRoot().getContext().getString(
                    R.string.announcement_meta,
                    item.category,
                    DateTimeUtils.relative(item.publishedAt, LocalDateTime.now())));
            ViewUtils.textOrGone(binding.textBody, item.body);

            // Always the megaphone. A pinned item briefly used a map pin, which
            // reads as a location on a card that has nothing to do with one.
            binding.icon.setImageResource(R.drawable.ic_megaphone);

            // Only two states are worth a chip: something is owed, or nothing is.
            if (item.actionNeeded) {
                binding.chipAction.setVisibility(View.VISIBLE);
                ViewUtils.statusChip(binding.chipAction,
                        R.string.announcement_action_needed,
                        R.color.slate, R.color.slate_bg);
            } else {
                binding.chipAction.setVisibility(View.GONE);
            }
        }
    }
}
