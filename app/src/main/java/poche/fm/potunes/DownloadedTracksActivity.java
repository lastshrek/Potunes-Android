package poche.fm.potunes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import poche.fm.potunes.Model.DownloadedTrackAdapter;
import poche.fm.potunes.Model.MusicFlowAdapter;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.Model.TrackAdapter;

public class DownloadedTracksActivity extends BaseActivity {
    private String TAG = "DownloadedTracks";
    private DownloadedTrackAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseSetContentView(R.layout.downloaded_tracks_activity);
        baseInit();

        LitePal.initialize(this);

        initTracks();

    }

    private void initTracks() {
        Intent intent = getIntent();
        String album_title = intent.getStringExtra("album_title");
        List<Track> list = DataSupport.where("album = ?", album_title).find(Track.class);
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track track: list) {
            tracks.add(track);
        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.tracklist_recycler_view);
        GridLayoutManager layoutManager = new GridLayoutManager(DownloadedTracksActivity.this, 1);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DownloadedTrackAdapter(tracks);
        recyclerView.setAdapter(adapter);
    }
}
