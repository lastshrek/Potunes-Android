package poche.fm.potunes;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.idescout.sql.SqlScoutServer;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.Model.Playlist;
import poche.fm.potunes.Model.PlaylistAdapter;
import poche.fm.potunes.fragment.QuciControlsFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, QuciControlsFragment.OnFragmentInteractionListener {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private List<Playlist> playlists = new ArrayList<>();
    private PlaylistAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ImageView mMyMusic;
    private QuciControlsFragment quickControls;
    protected static final int TEST = 1;
    private long time = 0;

    private String TAG = "MainActivity";
    private SQLiteDatabase db;


    private Handler sHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case TEST:
                    //可以执行UI操作
                    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
                    for (Playlist playlist : playlists) {
                        playlist.save();
                    }

                    GridLayoutManager layoutManager = new GridLayoutManager(MainActivity.this, 1);
                    recyclerView.setLayoutManager(layoutManager);
                    adapter = new PlaylistAdapter((List<Playlist>) msg.obj);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fresco.initialize(MainActivity.this);

        // initial database
        LitePal.initialize(MainActivity.this);
        
        db = LitePal.getDatabase();

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mMyMusic = (ImageView) findViewById(R.id.my_music);

        mMyMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: 进入我的音乐");
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, MyMusicActivity.class);
                intent.putExtra("title", "我的音乐");
                startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        // init Playlists
        initPlaylists();
        // init Refresh
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeColors(R.color.colorAccent);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshPlaylists();
            }
        });

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (quickControls == null) {
            quickControls = QuciControlsFragment.newInstance();
            ft.add(R.id.bottom_container, quickControls).commitAllowingStateLoss();
        } else {
            ft.show(quickControls).commitAllowingStateLoss();
        }


    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - time > 1000)) {
                Toast.makeText(this, "再按一次返回桌面", Toast.LENGTH_SHORT).show();
                time = System.currentTimeMillis();
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // 设置右侧menu
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }



    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    public void initPlaylists() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playlists.clear();
                    playlists = loadLocalPlaylists();

                    if (playlists.size() == 0) {
                        Log.d(TAG, "run: 播放列表为空");
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url("https://poche.fm/api/app/playlists/")
                                .build();
                        Response response = client.newCall(request).execute();
                        String responseData = response.body().string();
                        parseJSONWithGSON(responseData);
                    }

                    Message msg = Message.obtain();
                    msg.what = TEST;
                    msg.obj = playlists;

                    sHandler.sendMessage(msg);




                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private List<Playlist> loadLocalPlaylists() {
        List<Playlist> mPlaylists = DataSupport.order("id asc").find(Playlist.class);
        Log.d(TAG, "loadLocalPlaylists: " + mPlaylists.size());
        return mPlaylists;
    }


    private void parseJSONWithGSON(String jsonData) {

        Gson gson = new Gson();
        List<Playlist> datas = gson.fromJson(jsonData, new TypeToken<List<Playlist>>(){}.getType());

        for (Playlist playlist : datas) {
            Playlist mPlaylist = new Playlist(playlist.getTitle(), playlist.getPlaylist_id(), playlist.getCover());
            playlists.add(mPlaylist);
            Log.d(TAG, "parseJSONWithGSON: =============" + playlist.getPlaylist_id());
        }


        Log.d("加载播放列表完成", "onCreate: ");


    }

    public void refreshPlaylists() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        OkHttpClient client = new OkHttpClient();
//                        Request request = new Request.Builder()
//                                .url("https://poche.fm/api/app/playlists/")
//                                .build();
//                        try {
//                            Response response = client.newCall(request).execute();
//                            String responseData = response.body().string();
//                            Log.d(TAG, "run:=============" + responseData);

//                            Gson gson = new Gson();
//                            List<Playlist> datas = gson.fromJson(responseData, new TypeToken<List<Playlist>>(){}.getType());
//
//                            for (Playlist playlist : datas) {
//                                playlists.add(playlist);
//                            }

//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }


//                        adapter.notifyDataSetChanged();
//                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        }).start();
    }
}
