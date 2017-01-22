package poche.fm.potunes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.request.GetRequest;
import com.lzy.okserver.download.DownloadInfo;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.download.DownloadService;
import com.lzy.okserver.listener.DownloadListener;
import com.malinskiy.materialicons.widget.IconButton;

import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.Model.TrackAdapter;
import poche.fm.potunes.fragment.MoreFragment;
import poche.fm.potunes.fragment.QuciControlsFragment;


public class TrackListActivity extends BaseActivity implements MoreFragment.OnFragmentInteractionListener{

    public static final String PLAYLIST_ID = "playlist_id";
    public static final String TITLE = "playlist_title";
    private List<Track> tracks = new ArrayList<>();
    private TrackAdapter adapter;
    protected static final int TRACK = 1;
    private String TAG = "TrackListActivity";
    private IconButton downloadAll;
    private DownloadManager downloadManager;



    private Handler sHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {

            switch (msg.what) {

                case TRACK:
                    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.tracklist_recycler_view);
                    GridLayoutManager layoutManager = new GridLayoutManager(TrackListActivity.this, 1);
                    recyclerView.setLayoutManager(layoutManager);
                    adapter = new TrackAdapter((ArrayList<Track>) msg.obj);
                    recyclerView.setAdapter(adapter);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        baseSetContentView(R.layout.tracklist_layout);
        baseInit();

        downloadAll = (IconButton) findViewById(R.id.download_all);

        downloadAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (final Track track: tracks) {
                    track.save();
                    String url = track.getUrl();
                    if(downloadManager.getDownloadInfo(url) != null) {
                        Toast.makeText(TrackListActivity.this, "任务已经在下载列表中", Toast.LENGTH_SHORT).show();
                    } else {
                        GetRequest request = OkGo.get(url);
                        downloadManager.addTask(url, track, request, new DownloadListener() {
                            @Override
                            public void onProgress(DownloadInfo downloadInfo) {

                            }

                            @Override
                            public void onFinish(DownloadInfo downloadInfo) {
                                // 重命名文件
                                String downloadTitle = track.getArtist() + " - " + track.getTitle() + ".mp3";
                                downloadTitle = downloadTitle.replace("/", " ");
                                File old = new File(downloadManager.getTargetFolder(), downloadInfo.getFileName());
                                File rename = new File(downloadManager.getTargetFolder(), downloadTitle);
                                old.renameTo(rename);
                                // 数据库保存
                                track.setIsDownloaded(1);
                                track.save();

                                //移除任务保留本地文件
                                if (downloadManager.getDownloadInfo(track.getUrl()) != null) {
                                    downloadManager.removeTask(track.getUrl(), false);
                                }

                                Toast.makeText(TrackListActivity.this,  "" + downloadTitle, Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onError(DownloadInfo downloadInfo, String errorMsg, Exception e) {
                                Toast.makeText(TrackListActivity.this,  "下载出现错误，请检查网络并重试", Toast.LENGTH_SHORT).show();

                            }
                        });
//                        Toast.makeText(TrackListActivity.this,  track.getTitle() + "已添加至下载队列", Toast.LENGTH_SHORT).show();

//                            Intent intent = new Intent();
//                            intent.setClass(mContext, DownloadingActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            mContext.startActivity(intent);

                    }
                }


            }
        });

        downloadManager = DownloadService.getDownloadManager();
        downloadManager.setTargetFolder(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music/");
        downloadManager.getThreadPool().setCorePoolSize(1);

        LitePal.initialize(this);


        Intent intent = getIntent();
        int playlist_id = intent.getIntExtra(PLAYLIST_ID, -1);

        CharSequence title = intent.getStringExtra(TITLE);
        getBaseActionBar().setTitle(title);
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
                    editor.putString("album", getBaseActionBar().getTitle().toString());
                    editor.commit();
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
                        Track mTrack = new Track(track.getTitle(),track.getID(),track.getCover(),track.getArtist(),track.getUrl(),getBaseActionBar().getTitle().toString(), 0);
                        tracks.add(mTrack);
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
