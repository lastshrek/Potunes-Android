package poche.fm.potunes;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;


import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.tencent.mm.opensdk.openapi.IWXAPI;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import poche.fm.potunes.Model.MessageEvent;
import poche.fm.potunes.Model.Playlist;
import poche.fm.potunes.fragment.MoreFragment;
import poche.fm.potunes.fragment.MyMusicFragment;
import poche.fm.potunes.fragment.PlaylistAdapter;
import poche.fm.potunes.fragment.PlaylistFragment;
import poche.fm.potunes.fragment.TrackListFragment;
import poche.fm.potunes.service.PlayerService;

public class MainActivity extends BaseActivity implements PlaylistFragment.OnListFragmentInteractionListener,
        TrackListFragment.OnListFragmentInteractionListener,
        MoreFragment.OnFragmentInteractionListener, MyMusicFragment.OnFragmentInteractionListener {


    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
            "fm.poche.potunes.CURRENT_MEDIA_DESCRIPTION";
    private static final String SAVED_MEDIA_ID="fm.poche.potunes.MEDIA_ID";

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private PlaylistAdapter adapter;
    private ImageView mMyMusic;
    private long time = 0;

    private String TAG = "MainActivity";
    private SQLiteDatabase db;
    private static final String APP_ID = "wx0fc8d0673ec86694";
    private IWXAPI api;
    private static final String FRAGMENT_TAG = "potunes_list_container";
    private Playlist playlist;

    private PlayerService playerService;
    public PlayerService getPlayerService() {
        return playerService;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initializeToolbar();
        mMyMusic = (ImageView) findViewById(R.id.my_music);
        mMyMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToBrowser("my_music");
            }
        });
        bindService(new Intent(this, PlayerService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected: 绑定服务成功");
                PlayerService.PlayerServiceBinder playerServiceBinder = (PlayerService.PlayerServiceBinder) service;
                playerService = playerServiceBinder.getPlayService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected: 绑定服务失败");
            }
        }, Context.BIND_AUTO_CREATE);

        initializeFromParams(savedInstanceState, getIntent());
        Fresco.initialize(MainActivity.this);
        // initial database
        LitePal.initialize(MainActivity.this);
        db = LitePal.getDatabase();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(MessageEvent event) {
        if (event.playlist != null) {
            playlist = event.playlist;
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String mediaId = getMediaId();
        if (mediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mediaId);
        }
        super.onSaveInstanceState(outState);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void navigateToBrowser(String mediaId) {
        PlaylistFragment fragment = getPlaylistFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (fragment == null) {
            fragment = new PlaylistFragment();
            fragment.setMediaId(mediaId);
            transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
            transaction.commit();
            return;
        }


        if (!TextUtils.equals(mediaId, fragment.getMediaId())) {
            if (mediaId.equals("online_tracks")) {
                // If this is not the top level media (root), we add it to the fragment back stack,
                // so that actionbar toggle and Back will work appropriately:
                TrackListFragment tracklistFragment = TrackListFragment.newInstance();
                tracklistFragment.playlist = playlist;
                tracklistFragment.setMediaId(mediaId);
                transaction.replace(R.id.container, tracklistFragment, mediaId);
                transaction.addToBackStack(null);
                transaction.commit();
                setTitle(playlist.getTitle());
            }
            if (mediaId.equals("my_music")) {
                MyMusicFragment myMusic = MyMusicFragment.newInstance();

                myMusic.setMediaId(mediaId);
                transaction.replace(R.id.container, myMusic, mediaId);
                transaction.addToBackStack(null);
                transaction.commit();
                setTitle(R.string.my_music);
            }
        }
    }
    private void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        String mediaId = null;
        if (savedInstanceState != null) {
            // If there is a saved media ID, use it
            mediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
        }
        Log.d(TAG, "initializeFromParams: " + mediaId);
        navigateToBrowser(mediaId);
    }
    public String getMediaId() {
        PlaylistFragment fragment = getPlaylistFragment();
        if (fragment == null) {
            return null;
        }
        return fragment.getMediaId();
    }
    private PlaylistFragment getPlaylistFragment() {
        return (PlaylistFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
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

}
