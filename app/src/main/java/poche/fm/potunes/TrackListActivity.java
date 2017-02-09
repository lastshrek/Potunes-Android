package poche.fm.potunes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lzy.okserver.download.DownloadManager;
import com.malinskiy.materialicons.widget.IconButton;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.fragment.TrackAdapter;


public class TrackListActivity extends BaseActivity {

    public static final String PLAYLIST_ID = "playlist_id";
    public static final String TITLE = "playlist_title";
    private List<Track> tracks = new ArrayList<>();
    private TrackAdapter adapter;
    protected static final int TRACK = 1;
    private String TAG = "TrackListActivity";
    private IconButton downloadAll;
    private DownloadManager downloadManager;
    private SwipeRefreshLayout swipeRefresh;
    private String album_title;




    private Handler sHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case TRACK:
//                    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.tracklist_recycler_view);
//                    GridLayoutManager layoutManager = new GridLayoutManager(TrackListActivity.this, 1);
//                    recyclerView.setLayoutManager(layoutManager);
//                    adapter = new TrackAdapter((ArrayList<Track>) msg.obj);
//                    recyclerView.setAdapter(adapter);
                    break;
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        baseSetContentView(R.layout.tracklist_layout);
//        baseInit();



        LitePal.initialize(this);

        Intent intent = getIntent();
        int playlist_id = intent.getIntExtra(PLAYLIST_ID, -1);

        album_title = intent.getStringExtra(TITLE);
//        getBaseActionBar().setTitle(album_title);
        initTracks(playlist_id);

    }


    public void initTracks(final int playlist_id) {
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
                    //将json存到本地
                    SharedPreferences preference = getSharedPreferences("user", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preference.edit();
                    editor.putString("Tracks", responseData);
//                    editor.putString("album", getBaseActionBar().getTitle().toString());
                    editor.apply();
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
                    ArrayList<Track> datas = gson.fromJson(jsonData, new TypeToken<List<Track>>(){}.getType());
                    for (Track track : datas) {
//                        Track mTrack = new Track(track.getTitle(),track.getID(),track.getCover(),track.getArtist(),track.getUrl(),getBaseActionBar().getTitle().toString(), 0);
//                        tracks.add(mTrack);
                    }
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
