package poche.fm.potunes;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
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
import com.tencent.mm.opensdk.openapi.IWXAPI;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import java.util.List;

import poche.fm.potunes.Model.LocalAlbumMessageEvent;
import poche.fm.potunes.Model.LocalTracksEvent;
import poche.fm.potunes.Model.MessageEvent;
import poche.fm.potunes.Model.Playlist;
import poche.fm.potunes.fragment.LocalDownloadAlbumFragment;
import poche.fm.potunes.fragment.LocalTracksFragment;
import poche.fm.potunes.fragment.MoreFragment;
import poche.fm.potunes.fragment.MyMusicFragment;
import poche.fm.potunes.fragment.PlaylistAdapter;
import poche.fm.potunes.fragment.PlaylistFragment;
import poche.fm.potunes.fragment.TrackListFragment;
import poche.fm.potunes.service.LockScreenService;
import poche.fm.potunes.service.PlayerService;
import poche.fm.potunes.utils.Validator;

public class MainActivity extends BaseActivity implements PlaylistFragment.OnListFragmentInteractionListener,
        TrackListFragment.OnListFragmentInteractionListener,
        MoreFragment.OnFragmentInteractionListener,
        MyMusicFragment.OnFragmentInteractionListener,
        LocalDownloadAlbumFragment.OnFragmentInteractionListener,
        LocalTracksFragment.OnFragmentInteractionListener{


    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
            "fm.poche.potunes.CURRENT_MEDIA_DESCRIPTION";
    private static final String SAVED_MEDIA_ID="fm.poche.potunes.MEDIA_ID";

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private PlaylistAdapter adapter;
    private long time = 0;
    private ImageView mMyMusic;
    private static final String APP_ID = "wx0fc8d0673ec86694";
    private static final String FRAGMENT_TAG = "playlist_fragment";
    private String TAG = "MainActivity";
    private SQLiteDatabase db;
    private IWXAPI api;
    public Playlist playlist;
    public String mLocalAlbum;

    private PlayerService playerService;
    public PlayerService getPlayerService() {
        return playerService;
    }

    public Fragment currentFragment;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: 绑定服务成功");
            PlayerService.PlayBinder playerServiceBinder = (PlayerService.PlayBinder) service;
            playerService = playerServiceBinder.getPlayerService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: 绑定服务失败");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeToolbar();
        mMyMusic = (ImageView) findViewById(R.id.my_music);
        mMyMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyMusicFragment mMyMusicFragment = MyMusicFragment.newInstance();
                switchFragment(currentFragment, mMyMusicFragment);
            }
        });

        PlaylistFragment mPlaylistFragment = getPlaylistFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (mPlaylistFragment == null) {
            mPlaylistFragment = new PlaylistFragment();
            currentFragment = mPlaylistFragment;
            transaction.replace(R.id.container, mPlaylistFragment, FRAGMENT_TAG);
            transaction.commit();
        }


        //绑定服务
        bindService(new Intent(this, PlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        // 锁屏服务
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, LockScreenService.class);
        startService(intent);
        Fresco.initialize(MainActivity.this);
        // initial database
        LitePal.initialize(MainActivity.this);
        db = LitePal.getDatabase();
        EventBus.getDefault().register(this);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unbindService(serviceConnection);
        Log.d(TAG, "onDestroy: 毁");
    }
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(MessageEvent event) {
        if (event.playlist != null) {
            playlist = event.playlist;
        }
    }
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(LocalAlbumMessageEvent event) {
        if (event.album != null) {
            mLocalAlbum = event.album;
            LocalDownloadAlbumFragment mLocalDownloadAlbumFragment = LocalDownloadAlbumFragment.newInstance();
            switchFragment(currentFragment, mLocalDownloadAlbumFragment);
            setTitle(mLocalAlbum);
        }
    }
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(LocalTracksEvent event) {
        LocalTracksFragment mLocalTracksFragment = LocalTracksFragment.newInstance();
        switchFragment(currentFragment, mLocalTracksFragment);
        setTitle("本地音乐");
    }
    public void switchFragment(Fragment from, Fragment to) {
        Log.d(TAG, "currentFragment: " + currentFragment);
        if (currentFragment != to) {
            currentFragment = to;
            FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, R.anim.fade_out);
            if (!to.isAdded()) {	// 先判断是否被add过
                transaction.hide(from).add(R.id.container, to);
                transaction.addToBackStack(null);
                transaction.commit(); // 隐藏当前的fragment，add下一个到Activity中
            } else {
                transaction.hide(from).show(to).commit(); // 隐藏当前的fragment，显示下一个
            }
        }
        if (!getToolbarTitle().equals(R.string.app_name)) {
            mMyMusic.setVisibility(View.INVISIBLE);
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
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.getBackStackEntryCount() == 0) {
                moveTaskToBack(true);
            }
            fragmentManager.popBackStackImmediate();
            if (currentFragment instanceof LocalDownloadAlbumFragment && fragmentManager.getBackStackEntryCount() > 0
                    || getToolbarTitle().equals("本地音乐")) {
                setTitle(R.string.my_music);
            }
            Log.d(TAG, "onKeyDown: " + currentFragment);
            if (fragmentManager.getBackStackEntryCount() == 0) {
                mMyMusic.setVisibility(View.VISIBLE);
                currentFragment = getPlaylistFragment();
            }
        }
        return false;
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
