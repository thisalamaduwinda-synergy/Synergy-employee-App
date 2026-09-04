package lk.synergypharma.employee.ui.absence;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.ItemAttachmentBinding;
import lk.synergypharma.employee.domain.model.Attachment;

/** Files queued for the absence reason, with a way to take one back off. */
public final class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.Holder> {

    public interface OnRemove {
        void onRemove(int position);
    }

    private final List<Attachment> items = new ArrayList<>();
    @Nullable
    private OnRemove onRemove;

    public void setOnRemove(@Nullable OnRemove onRemove) {
        this.onRemove = onRemove;
    }

    public void submit(@NonNull List<Attachment> attachments) {
        items.clear();
        items.addAll(attachments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAttachmentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position), onRemove);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemAttachmentBinding binding;

        Holder(@NonNull ItemAttachmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Attachment item, @Nullable OnRemove onRemove) {
            binding.textName.setText(item.fileName);
            binding.textSize.setText(item.readableSize());
            binding.icon.setImageResource(item.isImage()
                    ? R.drawable.ic_camera : R.drawable.ic_file);
            binding.buttonRemove.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (onRemove != null && position != RecyclerView.NO_POSITION) {
                    onRemove.onRemove(position);
                }
            });
        }
    }
}
