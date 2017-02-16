package poche.fm.potunes;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;


import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.umeng.analytics.MobclickAgent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import cn.jpush.android.api.JPushInterface;
import poche.fm.potunes.Model.LocalAlbumMessageEvent;
import poche.fm.potunes.Model.LocalTracksEvent;
import poche.fm.potunes.Model.MessageEvent;
import poche.fm.potunes.Model.Playlist;
import poche.fm.potunes.fragment.LocalDownloadAlbumFragment;
import poche.fm.potunes.fragment.LocalTracksFragment;
import poche.fm.potunes.fragment.MoreFragment;
import poche.fm.potunes.fragment.MyMusicFragment;
import poche.fm.potunes.adapter.PlaylistAdapter;
import poche.fm.potunes.fragment.PlaylistFragment;
import poche.fm.potunes.fragment.TrackListFragment;
import poche.fm.potunes.service.LockScreenService;
import poche.fm.potunes.service.PlayerService;
import poche.fm.potunes.utils.ExampleUtil;
import poche.fm.potunes.utils.UpdateUtil;

public class MainActivity extends BaseActivity implements PlaylistFragment.OnListFragmentInteractionListener,
        TrackListFragment.OnListFragmentInteractionListener,
        MoreFragment.OnFragmentInteractionListener,
        MyMusicFragment.OnFragmentInteractionListener,
        LocalDownloadAlbumFragment.OnFragmentInteractionListener,
        LocalTracksFragment.OnFragmentInteractionListener{


    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
            "fm.poche.potunes.CURRENT_MEDIA_DESCRIPTION";
    private static final String SAVED_MEDIA_ID="fm.poche.potunes.MEDIA_ID";
    public static final String MESSAGE_RECEIVED_ACTION = "fm.poche.potunes.MESSAGE_RECEIVED_ACTION";

    public static final String KEY_TITLE = "title";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_EXTRAS = "extras";
    //for receive customer msg from jpush server
    private MessageReceiver mMessageReceiver;

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
    public static boolean isForeground = false;


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
        init();

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

        registerMessageReceiver();  // used for receive msg

        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            final int REQUEST_EXTERNAL_STORAGE = 1;
            ActivityCompat.requestPermissions(
                    this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
        }

        // 检查更新
        UpdateUtil update = new UpdateUtil(this);
        update.checkUpdate(true);
        Log.d(TAG, "onCreate: MainActivity");

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }


    public void registerMessageReceiver() {
        mMessageReceiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(MESSAGE_RECEIVED_ACTION);
        registerReceiver(mMessageReceiver, filter);
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MESSAGE_RECEIVED_ACTION.equals(intent.getAction())) {
                String messge = intent.getStringExtra(KEY_MESSAGE);
                String extras = intent.getStringExtra(KEY_EXTRAS);
                StringBuilder showMsg = new StringBuilder();
                showMsg.append(KEY_MESSAGE + " : " + messge + "\n");
                if (!ExampleUtil.isEmpty(extras)) {
                    showMsg.append(KEY_EXTRAS + " : " + extras + "\n");
                }
                setCostomMsg(showMsg.toString());
            }
        }
    }

    private void setCostomMsg(String msg){
        Log.d(TAG, "setCostomMsg: " + msg);
    }

    // 初始化 JPush。如果已经初始化，但没有登录成功，则执行重新登录。
    private void init(){
        JPushInterface.init(getApplicationContext());
    }
    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        unbindService(serviceConnection);
        unregisterReceiver(mMessageReceiver);
        MobclickAgent.onKillProcess(this);
        // 程序结束时销毁状态栏
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(1);
        super.onDestroy();
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
        if (event.local.equals("local")) return;
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
                transaction.commitAllowingStateLoss(); // 隐藏当前的fragment，add下一个到Activity中
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.getBackStackEntryCount() == 0) {
                return super.onKeyDown(keyCode, event);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
    public void onResume() {
        isForeground = true;
        MobclickAgent.onResume(this);
        super.onResume();
    }
    @Override
    public void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
    @Override
    public void onPause() {
        isForeground = false;
        MobclickAgent.onPause(this);
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMessageEvent(new LocalTracksEvent("local"));
            } else {
                Toast.makeText(this, "您没有赋予扫描本地音乐权限，无权查看本地音乐以及下载内容", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
