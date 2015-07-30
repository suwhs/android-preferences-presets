package su.whs.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by igor n. boulliev on 29.07.15.
 */

/**
 * add 'presets' support to android apps
 */

// TODO: static prefix:listener[] map
// TODO: add OnActivePresetChangedListener
public class SharedPreferencesPresets {
    private static final String TAG="SharedPrefsPresets";
    private static final String PRESETS_SET_CONF = "_DPRSCFG_";
    private static final String SELECTED_PREFIX_CONF = "_CURRPRESET_";
    private Context mContext;
    private String mFileName = null;
    private SharedPreferences mWrappedPrefs;

    public SharedPreferencesPresets(Context context) {
        mContext = context;
        mWrappedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public SharedPreferencesPresets(Context context, String fileName, int mode) {
        mContext = context;
        mWrappedPrefs = context.getSharedPreferences(fileName, mode);
        mFileName = fileName;
    }

    public Set<String> getPresets() {
        Set<String> result = getStringSetInternal(mWrappedPrefs,PRESETS_SET_CONF,new HashSet<String>());
        result.add("DEFAULT");
        return result;
    }

    public SharedPreferencesPresets addPreset(String name) {
        Set<String> existing = getStringSetInternal(mWrappedPrefs, PRESETS_SET_CONF, new HashSet<String>());
        if (existing.contains(name)) {
            throw new IllegalStateException(TAG+": preset '"+name+"' already exists");
        }
        synchronized (this) {
            SharedPreferences.Editor e = mWrappedPrefs.edit();
            existing.add(name);
            putStringSetInternal(e,PRESETS_SET_CONF, existing);
            e.commit();
        }
        return this;
    }

    private Set<String> getStringSetInternal(SharedPreferences prefs, String key, Set<String> defValues) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
            return prefs.getStringSet(key, defValues);
        } else {
            String json = prefs.getString(key,"");
            if (json==null||json.length()<1) return defValues;
            try {
                JSONArray ja = new JSONArray(json);
                Set<String> result = new HashSet<String>();
                for (int i=0; i<ja.length(); i++) {
                    String item = ja.optString(i);
                    if (item!=null)
                        result.add(item);
                }
                return result;
            } catch (JSONException e) {
                Log.e(TAG, "invalid Set<String> json format for key:'"+key+"'");
                return defValues;
            }
        }
    }

    private SharedPreferences.Editor putStringSetInternal(SharedPreferences.Editor editor, String key, Set<String> values) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
            return editor.putStringSet(key, values);
        } else {
            JSONArray ja = new JSONArray();
            for (String s : values) {
                ja.put(s);
            }
            return editor.putString(key,ja.toString());
        }
    }

    public SharedPreferencesPresets removePreset(String name) {
        Set<String> preferences = getStringSetInternal(mWrappedPrefs,PRESETS_SET_CONF,new HashSet<String>());
        if (!preferences.contains(name))
            return this;
        SharedPreferencesWrapper w = new SharedPreferencesWrapper(mWrappedPrefs,name,prefixForPreset(name));
        SharedPreferencesWrapper.Editor e = w.edit();
        e.clear();
        preferences.remove(name);
        putStringSetInternal(e,PRESETS_SET_CONF,preferences);
        e.commit();
        return this;
    }


    private static String prefixForPreset(String preset) {
        if ("DEFAULT".equals(preset))
            return SharedPreferencesWrapper.DEFAULT_PREFIX;
        return "_"+preset.toUpperCase()+"_";
    }

    public SharedPreferencesWrapper restoreActivePreset() {
        String name = mWrappedPrefs.getString(SELECTED_PREFIX_CONF,"DEFAULT");
        return getPreferencesPreset(name);
    }

    private void saveAsActivePreset(SharedPreferencesWrapper wrapper) {
        String name = wrapper.getName();
        SharedPreferences.Editor e = mWrappedPrefs.edit();
        e.putString(SELECTED_PREFIX_CONF,name);
        e.commit();
    }

    public SharedPreferencesWrapper getPreferencesPreset(String name) {
        return new SharedPreferencesWrapper(mWrappedPrefs,name, prefixForPreset(name));
    }

    public boolean isWrapper(SharedPreferences prefs) {
        return prefs instanceof SharedPreferencesWrapper;
    }

    public class SharedPreferencesWrapper implements SharedPreferences {
        private static final String DEFAULT_PREFIX = "_DF$P_";
        private String mPrefix = null;
        private SharedPreferences mPrefs;
        private boolean mDefaultPrefix = false;
        private String mName;

        private OnSharedPreferenceChangeListener mInternalOnChangeListener = new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.startsWith(mPrefix))
                    notifyOnChangeListeners(SharedPreferencesWrapper.this,removePrefix(key));
            }
        };

        private List<OnSharedPreferenceChangeListener> mRegisteredOnChangeListeners = new ArrayList<OnSharedPreferenceChangeListener>();

        public SharedPreferencesWrapper(SharedPreferences prefs, String name, String prefix) {
            mPrefs = prefs;
            mPrefix = prefix;
            mName = name;
        }

        public SharedPreferencesWrapper(SharedPreferences pref) {
            this(pref,"DEFAULT",DEFAULT_PREFIX);
            mDefaultPrefix = true;
        }

        public String getName() { return mName; }

        public void saveAsActivePreset() {
            SharedPreferencesPresets.this.saveAsActivePreset(this);
        }

        @Override
        public Map<String, ?> getAll() {
            Map<String, ?> defaults = mWrappedPrefs.getAll();
            Set<String> keys = defaults.keySet();
            Set<String> actuals = new HashSet<String>();
            for(String k : keys) {
                if (k.startsWith(mPrefix))
                    actuals.add(k);
            }
            HashMap<String, Object> result = new HashMap<String,Object>();
            for(String k: actuals) {
                Object o = defaults.get(k);
                result.put(k,o);
            }
            return result;
        }

        @Nullable
        @Override
        public String getString(String key, String defValue) {
            return mPrefs.getString(mPrefix + key,
                    mDefaultPrefix ? defValue : mPrefs.getString(DEFAULT_PREFIX + key, defValue));
        }

        @Nullable
        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return getStringSetInternal(mPrefs,mPrefix+key,
                            mDefaultPrefix ? defValues : getStringSetInternal(mPrefs, DEFAULT_PREFIX + key, defValues));
        }

        @Override
        public int getInt(String key, int defValue) {
            return mPrefs.getInt(mPrefix + key,
                    mDefaultPrefix ? defValue : mPrefs.getInt(DEFAULT_PREFIX + key, defValue));
        }

        @Override
        public long getLong(String key, long defValue) {
            return mPrefs.getLong(mPrefix + key,
                    mDefaultPrefix ? defValue : mPrefs.getLong(DEFAULT_PREFIX + key, defValue));
        }

        @Override
        public float getFloat(String key, float defValue) {
            return mPrefs.getFloat(mPrefix + key,
                    mDefaultPrefix ? defValue : mPrefs.getFloat(DEFAULT_PREFIX + key, defValue));
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return mPrefs.getBoolean(mPrefix + key,
                    mDefaultPrefix ? defValue : mPrefs.getBoolean(DEFAULT_PREFIX + key, defValue));
        }

        @Override
        public boolean contains(String key) {
            return mWrappedPrefs.contains(mPrefix+key) || (mDefaultPrefix ? false : mPrefs.contains(DEFAULT_PREFIX+key));
        }

        @Override
        public synchronized Editor edit() {
            return new SharedPreferencesWrapper.Editor();
        }

        @Override
        public synchronized void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            if (!mRegisteredOnChangeListeners.contains(listener))
                mRegisteredOnChangeListeners.add(listener);
        }

        @Override
        public synchronized void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            if (mRegisteredOnChangeListeners.contains(listener))
                mRegisteredOnChangeListeners.remove(listener);
        }

        private synchronized void notifyOnChangeListeners(SharedPreferences prefs, String key) {
            String normalizedKey = removePrefix(key);
            for(OnSharedPreferenceChangeListener listener : mRegisteredOnChangeListeners) {
                listener.onSharedPreferenceChanged(SharedPreferencesWrapper.this,normalizedKey);
            }
        }

        private String removePrefix(String prefixedKey) {
            return prefixedKey.substring(mPrefix.length());
        }

        public class Editor implements SharedPreferences.Editor {
            private SharedPreferences.Editor mWrappedEditor = mPrefs.edit();
            @Override
            public SharedPreferences.Editor putString(String key, String value) {
                return mWrappedEditor.putString(mPrefix+key,value);
            }

            @Override
            public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
                return putStringSetInternal(mWrappedEditor,mPrefix+key,values);
            }

            @Override
            public SharedPreferences.Editor putInt(String key, int value) {
                return mWrappedEditor.putInt(mPrefix + key, value);
            }

            @Override
            public SharedPreferences.Editor putLong(String key, long value) {
                return mWrappedEditor.putLong(mPrefix + key, value);
            }

            @Override
            public SharedPreferences.Editor putFloat(String key, float value) {
                return mWrappedEditor.putFloat(mPrefix + key, value);
            }

            @Override
            public SharedPreferences.Editor putBoolean(String key, boolean value) {
                return mWrappedEditor.putBoolean(mPrefix + key, value);
            }

            /**
             * remove overriding from prefix - you may need call removeDefault(key)
             * @param key
             * @return
             */
            @Override
            public SharedPreferences.Editor remove(String key) {
                return mWrappedEditor.remove(mPrefix + key);
            }

            /**
             * clears only current preset
             * @return
             */
            @Override
            public SharedPreferences.Editor clear() {
                for(String key : mPrefs.getAll().keySet()) {
                    if (key.startsWith(mPrefix)) {
                        mWrappedEditor.remove(mPrefix+key);
                    }
                }
                return this;
            }

            @Override
            public boolean commit() {
                return mWrappedEditor.commit();
            }

            @Override
            public void apply() {
                mWrappedEditor.apply();
            }
        }
    }
}
