package lk.synergypharma.employee.domain.model.enums;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import lk.synergypharma.employee.R;

/** Direction of a single face-recognition event at a gate camera. */
public enum GateDirection {

    IN(R.string.gate_direction_in, R.color.state_present, R.color.state_present_bg),
    OUT(R.string.gate_direction_out, R.color.slate, R.color.slate_bg);

    @StringRes
    public final int labelRes;
    @ColorRes
    public final int fgColorRes;
    @ColorRes
    public final int bgColorRes;

    GateDirection(@StringRes int labelRes, @ColorRes int fgColorRes, @ColorRes int bgColorRes) {
        this.labelRes = labelRes;
        this.fgColorRes = fgColorRes;
        this.bgColorRes = bgColorRes;
    }

    public static GateDirection fromApi(String raw) {
        return raw != null && raw.trim().equalsIgnoreCase("OUT") ? OUT : IN;
    }
}
