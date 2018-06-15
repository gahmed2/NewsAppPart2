package com.example.gehad.guardiannews2;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements LoaderCallbacks<List<News>> {

    /** URL for News data from the Guardian dataset */
    /**
     * "q" is the query for a topic that you are interested in
     * "api-key" is the developer unique key
     * "trailText" is the summary of the article
     * "byline" is the author's name
     * "thumbnail is the photo attahced with the article"*/

    //The original (hardcoded) URL is:
    //"https://content.guardianapis.com/search?" +
    //            "q=technology&api-key=test&show-fields=trailText,byline,thumbnail&" +
    //            "order-by=newest&format=json&type=article"
    private static final String GUARDIAN_REQUEST_URL = "https://content.guardianapis.com/search";

    /**
     * Constant value for the news loader ID. We can choose any integer.
     * This really only comes into play if you're using multiple loaders.
     */
    private static final int NEWS_LOADER_ID = 1;



    /** TextView that is displayed when the list is empty */
    private TextView mEmptyStateTextView;

    RecyclerView newsListView;
    NewsAdapter mAdapter;

    private List<News> newsList = new ArrayList<>();

    Boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        newsListView = findViewById(R.id.recycler_view);

        /** Adapter for the list of news */
        mAdapter = new NewsAdapter(newsList);

        mEmptyStateTextView = findViewById(R.id.empty_view);

        // vertical RecyclerView
        // keep movie_list_row.xml width to "match_parent"
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        newsListView.setLayoutManager(mLayoutManager);
        newsListView.setItemAnimator(new DefaultItemAnimator());

        // adding inbuilt divider line
        newsListView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        newsListView.setAdapter(mAdapter);

        //Recycler View Item onClick Listener
        newsListView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(),
                newsListView, new RecyclerTouchListener.ClickListener(){
            @Override
            public void onClick(View view, int position) {

                //find the article that was clicked on
                News article = newsList.get(position);

                // Convert the String URL into a URI object (to pass into the Intent constructor)
                Uri articleUri = Uri.parse(article.getUrl());

                // Create a new intent to view the article URI
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, articleUri);

                // Send the intent to launch a new activity
                startActivity(websiteIntent);
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));

        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }

        isConnected = networkInfo != null && networkInfo.isConnected();

        // If there is a network connection, fetch data
        if (isConnected) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();

            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
            // because this activity implements the LoaderCallbacks interface).
            loaderManager.initLoader(NEWS_LOADER_ID, null, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            View loadingIndicator = findViewById(R.id.progress_indicator);
            loadingIndicator.setVisibility(View.GONE);

            newsListView.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mEmptyStateTextView.setText(R.string.noNetwork);
        }
    }

    // Create a new loader for the given URL
    //Append URL to include user preferences'
    @Override
    public Loader<List<News>> onCreateLoader(int i, Bundle bundle) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // getString retrieves a String value from the preferences.
        // The second parameter is the default value for this preference.
        //get the entered text from EditText field which is the user's preferred topic
        String preferredQuery = sharedPrefs.getString(
                getString(R.string.query_key), getString(R.string.query_default));

        //get the chosen choice from the radio button list for Order by preference
        String orderBy  = sharedPrefs.getString(
                getString(R.string.order_by_key),
                getString(R.string.order_by_default));

        // parse breaks apart the URI string that's passed into its parameter
        Uri baseUri = Uri.parse(GUARDIAN_REQUEST_URL);

        // buildUpon prepares the baseUri that we just parsed so we can add query parameters to it.
        Uri.Builder uriBuilder = baseUri.buildUpon();

        //Append the query string in the URL to include all fields
        uriBuilder.appendQueryParameter("q",preferredQuery);
        uriBuilder.appendQueryParameter("api-key","test");
        uriBuilder.appendQueryParameter("show-fields","trailText,byline,thumbnail");
        uriBuilder.appendQueryParameter("order-by",orderBy);
        uriBuilder.appendQueryParameter("format","json");
        uriBuilder.appendQueryParameter("type","article");

        Log.v("Final URL", uriBuilder.toString());

        // Return the completed uri
        return new NewsLoader(this, uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> news) {
        // Hide loading indicator because the data has been loaded
        View loadingIndicator = findViewById(R.id.progress_indicator);
        loadingIndicator.setVisibility(View.GONE);

        // If there is a valid list of {@link News}s, then add them to the adapter's
        // data set. This will trigger the ListView to update.
        if (news != null && !news.isEmpty()) {
            mEmptyStateTextView.setVisibility(View.GONE);
            newsListView.setVisibility(View.VISIBLE);
            newsList.clear();
            newsList.addAll(news);
            mAdapter.notifyDataSetChanged();
        }

        else{
            // Set empty state text to display "No articles found."
            newsListView.setVisibility(View.GONE);
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mEmptyStateTextView.setText(R.string.noArticles);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<News>> loader) {
        newsList.clear();
    }

    //Defining Settings Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}