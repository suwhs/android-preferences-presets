package su.whs.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceActivity;

/**
 * Created by geek on 30.07.15.
 */
public class PresetsPreferenceActivity  extends PreferenceActivity {
    private SharedPreferencesPresets mPresets;

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        mPresets = new SharedPreferencesPresets(getApplicationContext(),name,mode);
        return mPresets.restoreActivePreset();
    }
}
