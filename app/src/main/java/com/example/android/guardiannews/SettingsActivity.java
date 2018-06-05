package com.example.android.guardiannews;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    public static class ArticlePreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_main);

            Preference minArticles = findPreference(getString(R.string.settings_min_articles_key));
            bindPreferenceSummaryToValue(minArticles);

            Preference orderBy = findPreference(getString(R.string.settings_order_by_key));
            bindPreferenceSummaryToValue(orderBy);

            Preference category = findPreference(getString(R.string.cat_sections_key));
            saveCatData(category);   // Need to save this separately
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(stringValue);
                if (prefIndex >= 0) {
                    CharSequence[] labels = listPreference.getEntries();
                    preference.setSummary(labels[prefIndex]);
                }
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }

        private void bindPreferenceSummaryToValue(Preference preference) {
            preference.setOnPreferenceChangeListener(this);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            String preferenceString = preferences.getString(preference.getKey(), "");
            onPreferenceChange(preference, preferenceString);
        }

        private void saveCatData(Preference preference) {
            preference.setOnPreferenceChangeListener(this);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            Set<String> cat_defaults = new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.cat_section_default_values)));
            Set<String> categories = preferences.getStringSet(getString(R.string.cat_sections_key), cat_defaults);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet(String.valueOf(categories), cat_defaults);
            editor.apply();
            // Update the Category labels with selected items (or defaults)
            preference.setSummary(String.valueOf(categories));

        }
    }

}
