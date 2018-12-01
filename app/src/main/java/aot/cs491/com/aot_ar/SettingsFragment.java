package aot.cs491.com.aot_ar;

import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        int colorAttr = android.R.attr.textColorPrimary;

        TypedArray ta = getContext().getTheme().obtainStyledAttributes(new int[]{colorAttr});
        int iconColor = ta.getColor(0, 0);
        ta.recycle();
        tintIcons(getPreferenceScreen(), iconColor);
    }

    private static void tintIcons(Preference preference, int color) {
        if (preference instanceof PreferenceGroup) {
            PreferenceGroup group = ((PreferenceGroup) preference);
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                tintIcons(group.getPreference(i), color);
            }
        } else {
            Drawable icon = preference.getIcon();
            if (icon != null) {
                icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }
}
