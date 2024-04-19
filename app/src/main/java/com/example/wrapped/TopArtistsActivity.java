package com.example.wrapped;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TopArtistsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_artists);

        String[] topArtists = getIntent().getStringArrayExtra("topArtists");
        String[] topArtistPics = getIntent().getStringArrayExtra("topArtistPics");
        String[] recArtists = getIntent().getStringArrayExtra("recArtists");
        String[] topGenres = getIntent().getStringArrayExtra("topGenres");
        String[] topTracks = getIntent().getStringArrayExtra("topTracks");

        TextView topArtistsTextView = findViewById(R.id.top_artists_text_view);
        Button toRecBtn = findViewById(R.id.to_rec_btn);

        toRecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TopArtistsActivity.this, RecArtistsActivity.class);
                intent.putExtra("topArtists", topArtists);
                intent.putExtra("topArtistPics", topArtistPics);
                intent.putExtra("recArtists", recArtists);
                intent.putExtra("topGenres", topGenres);
                intent.putExtra("topTracks", topTracks);
                startActivity(intent);
            }
        });

        if (topArtists != null) {
            // Concatenate topArtists array elements into a single string
            String joinedTopArtists = TextUtils.join(", ", topArtists);
            // Display the concatenated string in the TextView
            topArtistsTextView.setText(joinedTopArtists);
        } else {
            // Handle the case where topArtists array is null (optional)
            String ans="No top artists data available";
            topArtistsTextView.setText(ans);
        }
    }
}