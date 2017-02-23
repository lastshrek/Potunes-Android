package poche.fm.potunes;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.owen.tvrecyclerview.widget.TvRecyclerView;

import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.Model.Playlist;
import poche.fm.potunes.adapter.LayoutAdapter;
import poche.fm.potunes.bridge.MainUpView;
import poche.fm.potunes.bridge.RecyclerViewBridge;

public class MainTVActivity extends AppCompatActivity {
    private String TAG = "MainActivity";
    private Context mContext;
    private TvRecyclerView mRecyclerView;
    private MainUpView mainUpView;
    private LayoutAdapter mLayoutAdapter;
    private RecyclerViewBridge mRecyclerViewBridge;
    private View oldView;
    private View newView;
    protected static final int TEST = 1;
    private List<Playlist> playlists = new ArrayList<>();
    private Handler sHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case TEST:
                    Log.d(TAG, "handleMessage: " + msg.what);
                    mLayoutAdapter = new LayoutAdapter(mContext, (List<Playlist>) msg.obj);
                    mRecyclerView.setAdapter(mLayoutAdapter);
                    mLayoutAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initPlaylists();
    }

    private void initView() {
        setContentView(R.layout.activity_main_tv);
        ConnectivityManager mConnectivityManager = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        mContext = this;

        mRecyclerView = (TvRecyclerView) findViewById(R.id.tv_playlist);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setInterceptKeyEvent(true);

        mainUpView = (MainUpView) findViewById(R.id.main_up_view);
        mainUpView.setEffectBridge(new RecyclerViewBridge());
        mRecyclerViewBridge = (RecyclerViewBridge) mainUpView.getEffectBridge();
        mRecyclerViewBridge.setUpRectResource(R.drawable.test_rectangle);
        mainUpView.setDrawUpRectPadding(6);


        mRecyclerView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                mRecyclerViewBridge.setUnFocusView(itemView);
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                newView = itemView;
                mRecyclerViewBridge.setFocusView(itemView, 1.1f);
            }

            @Override
            public void onReviseFocusFollow(TvRecyclerView parent, View itemView, int position) {
                // 此处为了特殊情况时校正移动框
                mRecyclerViewBridge.setFocusView(itemView, 1.1f);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
//                mLayoutAdapter.removeItem(position);
            }
        });

        mRecyclerView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

            }
        });

        mRecyclerView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            @Override
            public boolean onInBorderKeyEvent(int direction, int keyCode, KeyEvent event) {
                return false;
            }
        });

        mRecyclerView.setOnLoadMoreListener(new TvRecyclerView.OnLoadMoreListener() {
            @Override
            public boolean onLoadMore() {
                return false;
            }
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        mRecyclerView.setSpacingWithMargins(18, 18);



    }

    private void initPlaylists() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playlists.clear();
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url("https://poche.fm/api/app/playlists/")
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

    private void parseJSONWithGSON(String jsonData) {
        Gson gson = new Gson();
        List<Playlist> datas = gson.fromJson(jsonData, new TypeToken<List<Playlist>>(){}.getType());

        for (Playlist playlist : datas) {
            Playlist mPlaylist = new Playlist(playlist.getTitle(), playlist.getPlaylist_id(), playlist.getCover());
            playlists.add(mPlaylist);
        }

        Message msg = Message.obtain();
        msg.what = TEST;
        msg.obj = playlists;
        sHandler.sendMessage(msg);
    }

}
