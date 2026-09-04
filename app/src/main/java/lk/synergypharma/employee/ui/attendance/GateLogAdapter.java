package lk.synergypharma.employee.ui.attendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.ItemGateEventBinding;
import lk.synergypharma.employee.domain.model.GateEvent;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.ViewUtils;

/** The day's IN/OUT timeline. */
public final class GateLogAdapter extends RecyclerView.Adapter<GateLogAdapter.Holder> {

    private final List<GateEvent> items = new ArrayList<>();

    public void submit(@NonNull List<GateEvent> events) {
        items.clear();
        items.addAll(events);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemGateEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position), position == 0, position == items.size() - 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemGateEventBinding binding;

        Holder(@NonNull ItemGateEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull GateEvent event, boolean isFirst, boolean isLast) {
            binding.textTime.setText(DateTimeUtils.timeWithSeconds(event.timestamp)
                    + " · " + binding.getRoot().getContext().getString(event.direction.labelRes));

            // The camera's own name is what the employee recognises; the raw
            // similarity is there so it can be quoted to HR in a dispute.
            binding.textMeta.setText(binding.getRoot().getContext().getString(
                    R.string.gate_event_meta,
                    event.gateName,
                    event.matchLabel()));

            ViewUtils.statusChip(binding.chipDirection, event.direction.labelRes,
                    event.direction.fgColorRes, event.direction.bgColorRes);

            // The node ring carries the direction colour; the drawable is mutated
            // so a recycled row never keeps the previous event's colour.
            ViewUtils.timelineNode(binding.node, event.direction.fgColorRes);

            // The line must not dangle above the first node or below the last.
            binding.lineTop.setVisibility(isFirst ? View.INVISIBLE : View.VISIBLE);
            binding.lineBottom.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

            bindFrame(event);
        }

        /**
         * The recognition backend stores {@code image_path} as a path into its
         * capture store, not a URL, so every row shows the placeholder until the
         * employee API signs or proxies those files. When it does, load it here.
         */
        private void bindFrame(@NonNull GateEvent event) {
            binding.thumb.setImageResource(R.drawable.ic_face_frame);
            // Accepted, but close to the threshold the gate lets anyone through
            // on. Worth marking rather than hiding — a borderline scan on the
            // wrong side of a shift boundary is what costs someone an hour of OT.
            binding.thumb.setAlpha(event.isBorderlineMatch() ? 0.55f : 1f);
        }
    }
}
