package lk.synergypharma.employee.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;

import lk.synergypharma.employee.R;

/**
 * Small view helpers so a status colour is applied the same way everywhere.
 *
 * <p>Every state colour in this app is carried on an enum
 * ({@code DayState}, {@code GateDirection}, {@code ApprovalStatus}), and these
 * methods are how that colour reaches a view. Nothing should be setting chip
 * colours by hand in a fragment.
 */
public final class ViewUtils {

    private ViewUtils() {
    }

    public static int dp(@NonNull Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /** Paints a read-only status pill: "Present", "OT 1h 12m", "Pending". */
    public static void statusChip(@NonNull Chip chip,
                                  @NonNull CharSequence label,
                                  @ColorRes int fgColorRes,
                                  @ColorRes int bgColorRes) {
        Context ctx = chip.getContext();
        chip.setText(label);
        chip.setTextColor(ContextCompat.getColor(ctx, fgColorRes));
        chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, bgColorRes)));
        chip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(ctx, fgColorRes)));
    }

    public static void statusChip(@NonNull Chip chip,
                                  @StringRes int labelRes,
                                  @ColorRes int fgColorRes,
                                  @ColorRes int bgColorRes) {
        statusChip(chip, chip.getContext().getString(labelRes), fgColorRes, bgColorRes);
    }

    /** Rounded square avatar tile: tinted background, coloured initials. */
    public static void avatar(@NonNull TextView view,
                              @NonNull String initials,
                              @ColorRes int fgColorRes,
                              @ColorRes int bgColorRes) {
        Context ctx = view.getContext();
        view.setText(initials);
        view.setTextColor(ContextCompat.getColor(ctx, fgColorRes));
        view.setBackgroundResource(R.drawable.bg_rect_r10);
        view.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(ctx, bgColorRes)));
    }

    /** Same tile, but holding an icon instead of initials. */
    public static void iconTile(@NonNull ImageView view,
                                @DrawableRes int iconRes,
                                @ColorRes int fgColorRes,
                                @ColorRes int bgColorRes) {
        Context ctx = view.getContext();
        view.setImageResource(iconRes);
        view.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, fgColorRes)));
        view.setBackgroundResource(R.drawable.bg_rect_r10);
        view.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(ctx, bgColorRes)));
    }

    public static void tintBackground(@NonNull View view, @ColorRes int colorRes) {
        view.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(view.getContext(), colorRes)));
    }

    /**
     * Recolours the ring on the gate-log timeline. The drawable is mutated so
     * recycled rows do not inherit the previous event's colour.
     */
    public static void timelineNode(@NonNull View view, @ColorRes int strokeColorRes) {
        Drawable bg = view.getBackground();
        if (bg == null) {
            view.setBackgroundResource(R.drawable.bg_timeline_node);
            bg = view.getBackground();
        }
        Drawable mutable = bg.mutate();
        if (mutable instanceof GradientDrawable) {
            ((GradientDrawable) mutable).setStroke(
                    dp(view.getContext(), 3f),
                    ContextCompat.getColor(view.getContext(), strokeColorRes));
        }
    }

    /**
     * Pads a view by the system bar insets.
     *
     * <p>Needed because targetSdk 35+ draws every app edge to edge, so the blue
     * header would otherwise sit underneath the status bar clock. The header
     * keeps painting its gradient behind the bar — only its content moves down.
     *
     * <p>The view's padding at the time of the call is treated as the baseline,
     * so this is safe to call once per view and never double-applies.
     */
    public static void applySystemBarPadding(@NonNull View view,
                                             final boolean top,
                                             final boolean bottom) {
        final int baseTop = view.getPaddingTop();
        final int baseBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    v.getPaddingLeft(),
                    top ? baseTop + bars.top : baseTop,
                    v.getPaddingRight(),
                    bottom ? baseBottom + bars.bottom : baseBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Sizes a view to exactly the status bar height.
     *
     * <p>The tab screens scroll their blue header away, and targetSdk 35+ draws
     * every app edge to edge — so without a fixed strip of blue at the top, the
     * white status bar clock ends up on a pale background and vanishes. This
     * paints that strip; the content scrolls underneath it.
     */
    public static void applyStatusBarHeight(@NonNull View scrim) {
        ViewCompat.setOnApplyWindowInsetsListener(scrim, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params != null && params.height != top) {
                params.height = top;
                v.setLayoutParams(params);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(scrim);
    }

    public static void visible(@NonNull View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** Sets text, hiding the view entirely when there is nothing to say. */
    public static void textOrGone(@NonNull TextView view, @Nullable CharSequence text) {
        if (text == null || text.length() == 0) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
            view.setText(text);
        }
    }
}
