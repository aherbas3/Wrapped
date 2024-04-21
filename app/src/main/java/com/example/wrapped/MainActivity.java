package com.example.wrapped;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static final String CLIENT_ID = "49fce8ab45a74080a08918a47c686295";
    public static final String REDIRECT_URI = "wrapped://auth";

    public static final int AUTH_TOKEN_REQUEST_CODE = 0;
    //public static final int AUTH_CODE_REQUEST_CODE = 1;

    private final OkHttpClient mOkHttpClient = new OkHttpClient();
    private String mAccessToken;
    private Call mCall;

    private TextView artistTextView, trackTextView, genreTextView, recArtistTextView;

    public String[] topArtists, topTracks, topGenres,
            topArtistPics, topTrackIds, topArtistIds, recArtists;

    String artistReqUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button getStarted = findViewById(R.id.start_btn);
        artistTextView = (TextView) findViewById(R.id.top_artists_text_view);
        trackTextView = (TextView) findViewById(R.id.top_tracks_text_view);
        genreTextView = (TextView) findViewById(R.id.top_genres_text_view);
        recArtistTextView = (TextView) findViewById(R.id.rec_artists_text_view);

        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TopArtistsActivity.class);
                intent.putExtra("topArtists", topArtists);
                intent.putExtra("topArtistPics", topArtistPics);
                intent.putExtra("recArtists", recArtists);
                intent.putExtra("topGenres", topGenres);
                intent.putExtra("topTracks", topTracks);
                startActivity(intent);
            }
        });

        getToken();
    }

    /**
     * even though getToken and getCode redirect to login page, spotify has single sign on mechanism so user only needs to login once.
     *we're aiming to use implicit grant flow, so getCode doesn't seem necessary but oh well
     *
     * Get token from Spotify
     * This method will open the Spotify login activity and get the token
     * What is token?
     * https://developer.spotify.com/documentation/general/guides/authorization-guide/
     */
    public void getToken() {
        final AuthorizationRequest request = getAuthenticationRequest(AuthorizationResponse.Type.TOKEN);
        AuthorizationClient.openLoginActivity(MainActivity.this, AUTH_TOKEN_REQUEST_CODE, request);
    }

    public void launchWrap() {
        if (mAccessToken == null) {
            Toast.makeText(this, "You need to get an access token first!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Request artistRequest = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/artists?limit=5&offset=0")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        final Request trackRequest = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/tracks?limit=5&offset=0")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        //gets list of top 20 artists. from this we'll extract top 5 genres later.
        final Request genreRequest = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/artists?limit=20&offset=0")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        cancelCall();
        mCall = mOkHttpClient.newCall(artistRequest);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("HTTP", "Failed to fetch artist data: " + e);
                Toast.makeText(MainActivity.this, "Failed to fetch artist data, watch Logcat for more details",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray itemsArray = jsonObject.getJSONArray("items");

                    topArtists = new String[5];
                    topArtistPics = new String[5];
                    topArtistIds = new String[5];


                    // Iterate through each artist object in the items array
                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject artistObject = itemsArray.getJSONObject(i);
                        String artistName = artistObject.getString("name");
                        topArtists[i] = artistName;
                        String artistId = artistObject.getString("id");
                        topArtistIds[i] = artistId;

                        JSONArray imagesArray = artistObject.getJSONArray("images");
                        for (int j = 0; j < imagesArray.length(); j++) {
                            JSONObject imageObject = imagesArray.getJSONObject(j);
                            int imageHeight = imageObject.getInt("height");
                            if (imageHeight == 640) {
                                String imageUrl = imageObject.getString("url");
                                topArtistPics[i] = imageUrl;
                            }
                        }

                    }

                    String joinedArtistNames = String.join(", ", topArtists);

                    artistReqUrl = "https://api.spotify.com/v1/recommendations?limit=20&seed_artists=" +
                            String.join(",", topArtistIds);

                    setTextAsync(joinedArtistNames, artistTextView);

                    createArtistRecRequest();

                } catch (JSONException e) {
                    Log.d("JSON", "Failed to parse artist data: " + e);
                    Toast.makeText(MainActivity.this, "Failed to parse artist data, watch Logcat for more details",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mCall = mOkHttpClient.newCall(trackRequest);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("HTTP", "Failed to fetch track data: " + e);
                Toast.makeText(MainActivity.this, "Failed to fetch track data, watch Logcat for more details",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray itemsArray = jsonObject.getJSONArray("items");

                    topTracks = new String[5];
                    topTrackIds = new String[5];

                    // Iterate through each track object in the items array
                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject trackObject = itemsArray.getJSONObject(i);
                        String trackName = trackObject.getString("name");
                        topTracks[i] = trackName;
                        String trackId = trackObject.getString("id");
                        topTrackIds[i] = trackId;
                    }

                    String joinedTrackNames = String.join(", ", topTracks);

                    setTextAsync(joinedTrackNames, trackTextView);
                } catch (JSONException e) {
                    Log.d("JSON", "Failed to parse track data: " + e);
                    Toast.makeText(MainActivity.this, "Failed to parse track data, watch Logcat for more details",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mCall = mOkHttpClient.newCall(genreRequest);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("HTTP", "Failed to fetch track data: " + e);
                Toast.makeText(MainActivity.this, "Failed to fetch track data, watch Logcat for more details",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray itemsArray = jsonObject.getJSONArray("items");

                    // Map to store genre counts
                    Map<String, Integer> genreCountMap = new HashMap<>();

                    // Iterate through each artist object in the items array
                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject artistObject = itemsArray.getJSONObject(i);
                        JSONArray genresArray = artistObject.getJSONArray("genres");

                        // Iterate through each genre of the artist
                        for (int j = 0; j < genresArray.length(); j++) {
                            String genre = genresArray.getString(j);
                            genreCountMap.put(genre, genreCountMap.getOrDefault(genre, 0) + 1);
                        }
                    }

                    List<Map.Entry<String, Integer>> sortedGenres = new ArrayList<>(genreCountMap.entrySet());
                    sortedGenres.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                    // Extract the top 5 genres
                    topGenres = new String[5];
                    for (int i = 0; i < Math.min(5, sortedGenres.size()); i++) {
                        topGenres[i]= sortedGenres.get(i).getKey();
                    }

                    String joinedGenreNames = String.join(", ", topGenres);

                    setTextAsync(joinedGenreNames, genreTextView);

                } catch (JSONException e) {
                    Log.d("JSON", "Failed to parse track data: " + e);
                    Toast.makeText(MainActivity.this, "Failed to parse track data, watch Logcat for more details",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });




    }

    private void createArtistRecRequest() {
        if (mAccessToken == null) {
            Toast.makeText(this, "You need to get an access token first!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Request artistRecRequest = new Request.Builder()
                .url(artistReqUrl)
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        mCall = mOkHttpClient.newCall(artistRecRequest);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("HTTP", "Failed to fetch track data: " + e);
                Toast.makeText(MainActivity.this, "Failed to fetch track data, watch Logcat for more details",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray tracksArray = jsonObject.getJSONArray("tracks");

                    // Map to store rec artist counts
                    Map<String, Integer> recArtistCountMap = new HashMap<>();
                    recArtists = new String[5];


                    // Iterate through each track object in the tracks array
                    for (int i = 0; i < tracksArray.length(); i++) {
                        JSONObject trackObject = tracksArray.getJSONObject(i);
                        JSONArray artistsArray = trackObject.getJSONArray("artists");

                        // Iterate through each artist in artistsArray
                        for (int j = 0; j < artistsArray.length(); j++) {
                            JSONObject artistObject = artistsArray.getJSONObject(j);
                            String artist = artistObject.getString("name");
                            recArtistCountMap.put(artist, recArtistCountMap.getOrDefault(artist, 0) + 1);
                        }
                    }

                    List<Map.Entry<String, Integer>> sortedRecArtists = new ArrayList<>(recArtistCountMap.entrySet());
                    sortedRecArtists.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                    Iterator<Map.Entry<String, Integer>> iterator = sortedRecArtists.iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Integer> entry = iterator.next();
                        String artist = entry.getKey();

                        // Check if the artist is in the topArtists array
                        if (Arrays.asList(topArtists).contains(artist)) {
                            iterator.remove();
                        }
                    }

                    recArtists = new String[5];
                    for (int i = 0; i < Math.min(5, sortedRecArtists.size()); i++) {
                        recArtists[i]= sortedRecArtists.get(i).getKey();
                    }

                    String joinedRecs = "";

                    if (recArtists != null) {
                        joinedRecs += String.join(", ", recArtists);
                    }

                    setTextAsync(joinedRecs, recArtistTextView);

                } catch (JSONException e) {
                    Log.d("JSON", "Failed to parse track data: " + e);
                    Toast.makeText(MainActivity.this, "Failed to parse track data, watch Logcat for more details",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * CHANGE
     * Get authentication request
     *
     * @param type the type of the request
     * @return the authentication request
     */
    private AuthorizationRequest getAuthenticationRequest(AuthorizationResponse.Type type) {
        return new AuthorizationRequest.Builder(CLIENT_ID, type, getRedirectUri().toString())
                .setShowDialog(false)
                .setScopes(new String[] { "user-read-email", "user-top-read" }) // <--- Change the scope of your requested token here
                .setCampaign("your-campaign-token")
                .build();
    }




//////////////////////////////////////////////IRRELEVANT



    /**
     * In summary, this method handles the result of the authentication activity launched for obtaining an access token or authorization code. It retrieves the relevant data from the result, such as the access token or authorization code, and updates the UI accordingly.
     *
     * When the app leaves this activity to momentarily get a token/code, this function
     * fetches the result of that external activity to get the response from Spotify
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);

        // Check which request code is present (if any)
        if (AUTH_TOKEN_REQUEST_CODE == requestCode) {
            mAccessToken = response.getAccessToken();
        }
        launchWrap();
    }

    /**
     * Creates a UI thread to update a TextView in the background
     * Reduces UI latency and makes the system perform more consistently
     *
     * @param text the text to set
     * @param textView TextView object to update
     */
    private void setTextAsync(final String text, TextView textView) {
        runOnUiThread(() -> textView.setText(text));
    }

    /**
     * Gets the redirect Uri for Spotify
     *
     * @return redirect Uri object
     */
    private Uri getRedirectUri() {
        return Uri.parse(REDIRECT_URI);
    }

    private void cancelCall() {
        if (mCall != null) {
            mCall.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        cancelCall();
        super.onDestroy();
    }
}