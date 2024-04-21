package com.example.wrapped;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SummaryActivity extends AppCompatActivity {

    private String mAccessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        String bgUrl = "https://img.freepik.com/free-vector/gradient-galaxy-background_23-2148983655.jpg"; // Replace with your image URL



        String[] topArtists = getIntent().getStringArrayExtra("topArtists");
        String[] topArtistPics = getIntent().getStringArrayExtra("topArtistPics");
        String[] recArtists = getIntent().getStringArrayExtra("recArtists");
        String[] topGenres = getIntent().getStringArrayExtra("topGenres");
        String[] topTracks = getIntent().getStringArrayExtra("topTracks");
        String[] trackURIs = getIntent().getStringArrayExtra("trackURIs");

        mAccessToken = getIntent().getStringExtra("token");

        playTrack(trackURIs[0]);

        TextView sumArtist1 = findViewById(R.id.sumArtist1);
        TextView sumArtist2 = findViewById(R.id.sumArtist2);
        TextView sumArtist3 = findViewById(R.id.sumArtist3);
        TextView sumArtist4 = findViewById(R.id.sumArtist4);
        TextView sumArtist5 = findViewById(R.id.sumArtist5);
        Button shareBtn = findViewById(R.id.shareBtn);
        Button homeBtn = findViewById(R.id.homeBtn);
        TextView sumTrack1 = findViewById(R.id.sumTrack1);
        TextView sumTrack2 = findViewById(R.id.sumTrack2);
        TextView sumTrack3 = findViewById(R.id.sumTrack3);
        TextView sumTrack4 = findViewById(R.id.sumTrack4);
        TextView sumTrack5 = findViewById(R.id.sumTrack5);
        TextView sumGenre1 = findViewById(R.id.sumGenre1);

        ImageView firstPic = findViewById(R.id.firstPic);
        String imageUrl = topArtistPics[0];
        Picasso.get().load(imageUrl).into(firstPic);

        sumArtist1.setText("  1. "+topArtists[0]);
        sumArtist2.setText("  2. "+topArtists[1]);
        sumArtist3.setText("  3. "+topArtists[2]);
        sumArtist4.setText("  4. "+topArtists[3]);
        sumArtist5.setText("  5. "+topArtists[4]);

        sumTrack1.setText("  1. "+topTracks[0]);
        sumTrack2.setText("  2. "+topTracks[1]);
        sumTrack3.setText("  3. "+topTracks[2]);
        sumTrack4.setText("  4. "+topTracks[3]);
        sumTrack5.setText("  5. "+topTracks[4]);

        sumGenre1.setText("  "+ topGenres[0]);

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Capture the screen as a Bitmap
                View rootView = getWindow().getDecorView().getRootView();
                rootView.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
                rootView.setDrawingCacheEnabled(false);

// Save the Bitmap to a file
                File imagePath = new File(getExternalFilesDir(null), "screenshot.png");
                try {
                    FileOutputStream fos = new FileOutputStream(imagePath);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    Log.e("Error saving image", e.getMessage(), e);
                }

// Share the image file
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                Uri uri = FileProvider.getUriForFile(SummaryActivity.this, getPackageName() + ".fileprovider", imagePath);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(shareIntent, "Share screenshot"));


            }
        });
    }

    public void playTrack(String trackURI) {

        if (trackURI == null) {
            Toast.makeText(this, "Track URI missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String playUrl = "https://api.spotify.com/v1/me/player/play";

        JSONObject payload = new JSONObject();
        try {
            JSONArray uris = new JSONArray();
            uris.put(trackURI);
            payload.put("uris", uris);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(playUrl)
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Spotify", "Error playing track", e);
                runOnUiThread(() -> Toast.makeText(SummaryActivity.this, "Failed to play track", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("Spotify", "Failed to command Spotify: " + response);
                    // Log response body for debugging
                    Log.e("Spotify", "Response body: " + response.body().string());
                    runOnUiThread(() -> Toast.makeText(SummaryActivity.this, "Failed to command Spotify", Toast.LENGTH_SHORT).show());
                } else {
                    Log.d("Spotify", "Successfully played track");
                    // Log response body for debugging
                    Log.d("Spotify", "Response body: " + response.body().string());
                }
            }
        });
    }
}