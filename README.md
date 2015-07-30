# android-preferences-presets

Quick and Dirty Guide
=====
multiple preferenes presets for application

instead PreferenceManager.getDefaultSharedPreferences use
```java
SharedPreferencesPresets presets = new SharedPreferencesPresets(getApplicationContext(),name,mode);

// list presets (at least one item with name 'DEFAULT' returned)
Set<String> presetsNames = presets.getPresets();

// get sharedpreferences by preset name
SharedPreferences prefs = presets.get('DEFAULT')

// create new preset based on default and return SharedPreferences
SharedPreferences prefs = presets.addPreset('new-preset-name')

// save shared preferences as active preset
if (prefs instanceof SharedPreferencesPresets.SharedPreferencesWrapper)
    ((SharedPreferencesPresets.SharedPreferencesWrapper)prefs).saveAsActivePreset();

// restore active before preset (returns default
SharedPreference prefs = presets.restoreActivePreset()
```

Settings Activity
=====
to use presets with Settings Activity - use PresetsPreferenceActivity as superclass instead stock PreferenceActivity

Contacts
====
<a href="mailto:info@whs.su">info@whs.su</a>
