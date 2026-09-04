package lk.synergypharma.employee.util;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Money is always rupees here, always grouped, always two decimals.
 *
 * <p>Grouping symbols are pinned to {@link Locale#US} on purpose: a Sinhala
 * locale would otherwise render the digits in a form the payroll printout does
 * not use, and employees compare the two side by side.
 */
public final class CurrencyUtils {

    private static final String PREFIX = "LKR ";

    private CurrencyUtils() {
    }

    /** "LKR 186,450.00" */
    @NonNull
    public static String format(@NonNull BigDecimal amount) {
        return PREFIX + plain(amount);
    }

    /** "186,450.00" — for table rows where the LKR prefix is in the header. */
    @NonNull
    public static String plain(@NonNull BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(amount);
    }
}
