package com.example.android.guardiannews;

import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Article>>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String LOG_TAG = MainActivity.class.getName();
    private static final String API_URL = "https://content.guardianapis.com/search?";

    // Custom Tabs variables
    public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";
    CustomTabsClient mClient;
    CustomTabsSession mCustomTabsSession;
    CustomTabsServiceConnection mCustomTabsServiceConnection;
    static CustomTabsIntent customTabsIntent;

    private ProgressBar mspinner;
    private TextView mEmptyTextView;
    private ImageView noData;
    RecyclerView recyclerView;
    SharedPreferences sharedPrefs;

    /**
     * Adapter for the list of articles
     */
    private ArticleAdapter mAdapter;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(LOG_TAG, "Test OnCreate() called");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_recycler);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);   // (Use toolbarmain as per https://guides.codepath.com/android/using-the-app-toolbar
        setSupportActionBar(toolbar);

        try {

            getSupportActionBar().setDisplayShowTitleEnabled(false);

        } catch (NullPointerException ne) {
            Log.e(TAG, ne.getMessage());
        }

        //Initialise Custom Tabs
        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                mClient = customTabsClient;
                mClient.warmup(0L);
                mCustomTabsSession = mClient.newSession(null);
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mClient = null;
            }
        };
        CustomTabsClient.bindCustomTabsService(MainActivity.this, CUSTOM_TAB_PACKAGE_NAME, mCustomTabsServiceConnection);
        customTabsIntent = new CustomTabsIntent.Builder(mCustomTabsSession)
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorBar))
                .setShowTitle(true)
                .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back))
                .build();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.setHasFixedSize(true);

        mEmptyTextView = findViewById(R.id.empty_state);
        mspinner = findViewById(R.id.loading_spinner);
        noData = findViewById(R.id.no_data);

        // Create a new adapter that takes an empty list of articles as input
        mAdapter = new ArticleAdapter(this, new ArrayList<Article>());

        // setEmptyView not avail for a recyclerView
        recyclerView.setAdapter(mAdapter);

        Log.i(LOG_TAG, "Calling initLoader()");

        // Obtain a reference to the SharedPreferences file for this app
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // And register to be notified of preference changes
        // So we know when the user has adjusted the query settings
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        //Check for internet connectivity
        ConnectivityManager cm = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            getLoaderManager().initLoader(1, null, this).forceLoad();
        } else {
            mspinner.setVisibility(View.GONE);
            mEmptyTextView.setVisibility(View.VISIBLE);
            mEmptyTextView.setText(R.string.no_internet);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(getString(R.string.settings_min_articles_key)) ||
                key.equals(getString(R.string.settings_order_by_key)) ||
                key.equals(getString(R.string.cat_sections_key))) {

            // Clear the View as a new query will be kicked off
            mAdapter.clearArticles();

            // Hide the empty state text view as the loading indicator will be displayed
            mEmptyTextView.setVisibility(View.GONE);

            // Show the loading indicator while new data is being fetched
            mspinner.setVisibility(View.VISIBLE);

            // Restart the loader to requery the USGS as the query settings have been updated
            getLoaderManager().restartLoader(1, null, this);
        }
    }


    @Override
    public Loader<List<Article>> onCreateLoader(int id, Bundle args) {

        Log.i(LOG_TAG, "Test OnCreateLoader() called");

        // getString retrieves a String value from the preferences. The second parameter is the default value for this preference.
        String pageSize = sharedPrefs.getString(
                getString(R.string.settings_min_articles_key),
                getString(R.string.settings_min_articles_default));

        String orderBy  = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default));

        // parse breaks apart the URI string that's passed into its parameter
        Uri baseUri = Uri.parse(API_URL);

        // buildUpon prepares the baseUri that we just parsed so we can add query parameters to it
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Append query parameter and its value. For example, the `format=json`
        uriBuilder.appendQueryParameter("section", sectionsFormatted(getApplicationContext()));
        uriBuilder.appendQueryParameter("format", "json");
        uriBuilder.appendQueryParameter("show-fields", "headline,thumbnail");
        uriBuilder.appendQueryParameter("show-tags", "contributor");
        uriBuilder.appendQueryParameter("page-size", pageSize);
        uriBuilder.appendQueryParameter("order-by", orderBy);
        uriBuilder.appendQueryParameter("api-key", "test");

        // Return the completed uri i.e. (With 3 default categories set in arrays.xml)
        // Separator is | symbol.
        // https://content.guardianapis.com/search?
        // &section=world|technology|politics&format=json&show-fields=headline,thumbnail&show-tags=contributor&order-by=newest&api-key=test

        return new ArticleLoader(this, uriBuilder.toString());
    }


    // Info from https://stackoverflow.com/questions/7057845/save-arraylist-to-sharedpreferences

    private String sectionsFormatted (Context context) {
        ArrayList<String> list = getData(context);

        StringBuilder sb = new StringBuilder();
        String delim = "";

        for (String s : list)
        {
            sb.append(delim);
            sb.append(s);
            delim = "|";
        }
        return sb.toString();
    }

    private ArrayList<String> getData (Context context) {

        ArrayList<String> sectionsArray = new ArrayList<>();
        Set<String> options = sharedPrefs.getStringSet(getString(R.string.cat_sections_key), null);

        if(options != null ) {
            for(String s: options) {
                Log.d(TAG, "option  :  " + s);
                sectionsArray = new ArrayList<>(options);
            }
        }

        // If no selections made ie first launch then send the default categories for the url builder
        if (sectionsArray.isEmpty()) {
            ArrayList<String> cat_defaults = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.cat_section_default_values)));
            sectionsArray.addAll(cat_defaults);
        }
        return sectionsArray;
    }


    @Override
    public void onLoadFinished(Loader<List<Article>> loader, List<Article> data) {

        //Hide loading spinner as loading is completed
        mspinner.setVisibility(View.GONE);

        Log.i(LOG_TAG, "Test OnLoadFinished() called");

        // Clear the adapter of previous articles
        mAdapter.clearArticles();

        // If there is a valid list of articles, then add them to the adapter's
        // data set.
        if (data != null && !data.isEmpty()) {
            mAdapter.addData(data);
        }

        // Check for data, if none received, enable empty views
        if (mAdapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            noData.setImageResource(R.drawable.nodata);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Article>> loader) {

        Log.i(LOG_TAG, "Test OnLoadReset() called");

        // Loader reset, so we can clear out our existing data.
        mAdapter.clearArticles();
    }

    /**
     *
     * Method to send intent to browse full article on Guardian website
     */
    public static void onArticleClick(Context context, String webUrl) {
        customTabsIntent.launchUrl(context, Uri.parse(webUrl));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh) {
            refreshData();
            return true;
        }
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshData() {
        Toast.makeText(this, R.string.checkNewData, Toast.LENGTH_SHORT).show();
        mAdapter.clearArticles();
        getLoaderManager().initLoader(1, null, this).forceLoad();
    }
}
