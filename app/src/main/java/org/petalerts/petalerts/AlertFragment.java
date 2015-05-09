package org.petalerts.petalerts;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class AlertFragment extends Fragment {

    private ArrayAdapter<String> recentAlertsAdapter;

    public AlertFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.alertfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            new FetchAlertsTask().execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] animalArray = {
                "Dog - Doberman - Adult",
                "Dog - Collie - Puppy",
                "Cat - Siamese - Puppy",
                "Dog - Collie - Adult",
                "Dog - Bulldog - Puppy"
        };

        List<String> recentAnimals = new ArrayList<String>(Arrays.asList(animalArray));

        recentAlertsAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_animal,
                R.id.list_item_animal_textview,
                recentAnimals);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_animal);
        listView.setAdapter(recentAlertsAdapter);
        return rootView;
    }

    private class FetchAlertsTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchAlertsTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String alertsJsonStr = null;

            try {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("www.petalerts.org")
                        .appendPath("api")
                        .appendPath("alert")
                        .build();
                URL url = new URL(builder.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                alertsJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getAlertsDataFromJson(alertsJsonStr, 10);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        private String[] getAlertsDataFromJson(String AlertJsonStr, int numDays)
                throws JSONException {

            final String ALERT_ID = "id";
            final String ALERT_SPECIES = "species";
            final String ALERT_GENRE = "genre";
            final String ALERT_AGE = "age";
            final String ALERT_DATE = "date";

            JSONArray alertsArray = new JSONArray(AlertJsonStr);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < alertsArray.length(); i++) {
                JSONObject alert = alertsArray.getJSONObject(i);
                String species = alert.getString(ALERT_SPECIES);
                String age = alert.getString(ALERT_AGE);
                String genre = alert.getString(ALERT_GENRE);

                resultStrs[i] = species + " - " + age + " - " + genre;
            }
            return resultStrs;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                recentAlertsAdapter.clear();
                for (String alertStr : result) {
                        recentAlertsAdapter.add(alertStr);
                }
            }
        }
    }
}
