package poche.fm.potunes;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.Model.Playlist;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.Model.TrackAdapter;


public class TrackListActivity extends AppCompatActivity {

    public static final String PLAYLIST_ID = "playlist_id";

    private List<Track> tracks = new ArrayList<>();

    private TrackAdapter adapter;

    protected static final int TRACK = 1;


    private Handler sHandler = new Handler() {
        public void handleMessage(android.os.Message msg)
        {
            switch (msg.what)
            {
                case TRACK:

                    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.tracklist_recycler_view);

                    Log.d("RecyclerView加载完成", "onCreate: ");

                    GridLayoutManager layoutManager = new GridLayoutManager(TrackListActivity.this, 1);

                    recyclerView.setLayoutManager(layoutManager);

                    adapter = new TrackAdapter((List<Track>) msg.obj);

                    recyclerView.setAdapter(adapter);

                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.tracklist_layout);

        Intent intent = getIntent();

        int playlist_id = intent.getIntExtra(PLAYLIST_ID, -1);

        initTracks(playlist_id);

    }

    public void initTracks(final int playlist_id) {
        Log.d("加载歌曲列表", "initTracks: ");
        tracks.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url("https://poche.fm/api/app/playlists/" + playlist_id)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    parseJSONWithGSON(responseData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void parseJSONWithGSON(final String jsonData) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Gson gson = new Gson();
                    List<Track> datas = gson.fromJson(jsonData, new TypeToken<List<Track>>(){}.getType());

                    for (Track track : datas) {
                        tracks.add(track);
                    }
                    Log.d("歌曲获取完成", "onCreate: ");

                    Message msg = Message.obtain();
                    msg.what = TRACK;
                    msg.obj = tracks;
                    sHandler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }
}
