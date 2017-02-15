package poche.fm.potunes;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.umeng.analytics.MobclickAgent;

import org.litepal.crud.DataSupport;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.Model.Lrc;
import poche.fm.potunes.Model.PlayState;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.fragment.PlayQueueFragment;
import poche.fm.potunes.lrc.LrcView;
import poche.fm.potunes.service.PlayerService;
import poche.fm.potunes.utils.MediaUtil;
import poche.fm.potunes.widgets.TintImageView;


public class PlayerActivity extends AppCompatActivity {

    // 传值用
    public static final String TAG = "PlayerActivity"; // 更新动作
    public static final String MUSIC_CURRENT = "fm.poche.action.MUSIC_CURRENT"; // 音乐当前时间改变动作

    private Toolbar toolbar;
    private TextView mToolbarTitle;
    private TextView mToolbarArtist;
    private ProgressBar mLoading;
    private SeekBar mSeekbar;
    private ImageView mPlayPause;
    private TextView mStart;
    private TextView mEnd;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private ImageView mSkipPrev;
    private ImageView mSkipNext;
    private ImageView mBackgroundImage;
    private TintImageView mRepeat;
    private Drawable mNoShuffle;
    private Drawable mShuffle;
    private Drawable mSingle;
    private ActionBar ab;
    private RelativeLayout mCoverContainer;
    private ImageView mPlayingCover;
    private RelativeLayout mLrcContainer;
    private LrcView mLrcView;
    private View dividerLine;
    private ImageView mPlaylist;



    private PlayerService mPlayerService;
    private long currentTime; // 当前歌曲播放时间
    private long duration;
    private int track_id = 0;

    // 0:noshuffle, 1:single, 2:shuffle
    private int shuffle;
    private PlayerReceiver playerReceiver;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: ");
            PlayerService.PlayBinder pb = (PlayerService.PlayBinder)service;
            mPlayerService = pb.getPlayerService();
        }
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: ");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //绑定服务
        bindService(new Intent(this, PlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        // initial subviews
        findViewById();
        // Set on clicklistener
        setViewOnclickListener();
    }

    private void findViewById() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.player_layout);
        //initials
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ab = getSupportActionBar();
            ab.setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            int statusHeight = -1;
            try {
                Class clazz = Class.forName("com.android.internal.R$dimen");
                Object object = clazz.newInstance();
                int height = Integer.parseInt(clazz.getField("status_bar_height")
                        .get(object).toString());
                statusHeight = getBaseContext().getResources().getDimensionPixelSize(height);
            } catch (Exception e) {
                e.printStackTrace();
            }
            toolbar.setTitle("");
            toolbar.setPadding(0, statusHeight, 0 , 0);
        }
        mToolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        mToolbarArtist = (TextView) findViewById(R.id.toolbar_artist);

        mBackgroundImage = (ImageView) findViewById(R.id.background_image);
        mCoverContainer = (RelativeLayout) findViewById(R.id.cover_container);
        mPlayingCover = (ImageView) findViewById(R.id.playing_cover);
        mPlayingCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCoverContainer.getVisibility() == View.VISIBLE) {
                    mCoverContainer.setVisibility(View.INVISIBLE);
                    mLrcContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        mLrcContainer = (RelativeLayout) findViewById(R.id.lrcContainer);
        mLrcContainer.setVisibility(View.INVISIBLE);
        mLrcView = (LrcView) findViewById(R.id.lrcview);
        mLrcView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLrcView.getVisibility() == View.VISIBLE) {
                    mLrcContainer.setVisibility(View.INVISIBLE);
                    mCoverContainer.setVisibility(View.VISIBLE);
                }
            }
        });
        mRepeat = (TintImageView) findViewById(R.id.play_repeat);
        mNoShuffle = ContextCompat.getDrawable(this, R.drawable.play_icn_loop_prs);
        mShuffle = ContextCompat.getDrawable(this, R.drawable.play_icn_shuffle);
        mSingle = ContextCompat.getDrawable(this, R.drawable.play_icn_one_prs);
        mRepeat.setImageDrawable(mNoShuffle);
        mPlayPause = (ImageView) findViewById(R.id.play_pause);
        mPlayDrawable = ContextCompat.getDrawable(this, R.drawable.play_btn_play);
        mPauseDrawable = ContextCompat.getDrawable(this, R.drawable.play_btn_pause);
        mSkipNext = (ImageView) findViewById(R.id.play_next);
        mSkipPrev = (ImageView) findViewById(R.id.play_prev);
        mSkipPrev.setImageDrawable(getResources().getDrawable(R.drawable.play_btn_prev));
        mSkipNext.setImageDrawable(getResources().getDrawable(R.drawable.play_btn_next));
        mStart = (TextView) findViewById(R.id.startText);
        mEnd = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        mLoading = (ProgressBar) findViewById(R.id.progressBar1);
        View mControllers = findViewById(R.id.controllers);
        mControllers.setVisibility(View.VISIBLE);

        mPlayPause.setImageDrawable(mPlayDrawable);
        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerService != null) {
                    mPlayerService.next();
                }
            }
        });
        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerService != null) {
                    mPlayerService.previous();
                }
            }
        });
        dividerLine = (View) findViewById(R.id.view_line);
        mPlaylist = (ImageView) findViewById(R.id.nowPlaying_list);
        mPlaylist.setImageDrawable(getResources().getDrawable(R.drawable.play_icn_src_prs));
    }
    private void setViewOnclickListener() {
        ViewOnclickListener clickListener = new ViewOnclickListener();
        mPlayPause.setOnClickListener(clickListener);
        mRepeat.setOnClickListener(clickListener);
        mSeekbar.setOnSeekBarChangeListener(new SeekBarChangeListener());
        mPlaylist.setOnClickListener(new ViewOnclickListener());
    }
    private class ViewOnclickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.play_pause:
                    if (PlayerService.mPlayState.isPlaying()) {
                        mPlayerService.pause();
                        PlayerService.mPlayState.setPlaying(false);
                        mPlayPause.setImageDrawable(mPlayDrawable);
                    } else {
                        mPlayerService.resume();
                        PlayerService.mPlayState.setPlaying(true);
                        mPlayPause.setImageDrawable(mPauseDrawable);
                    }
                    break;
                case R.id.play_repeat:
                    if(shuffle == 0) {
                        PlayerService.mPlayState.setMode(1);
                        mRepeat.setImageDrawable(mShuffle);
                        Toast.makeText(PlayerActivity.this, "随机播放",
                                Toast.LENGTH_SHORT).show();
                        shuffle = 1;
                    } else if (shuffle == 1) {
                        PlayerService.mPlayState.setMode(2);
                        mRepeat.setImageDrawable(mSingle);
                        Toast.makeText(PlayerActivity.this, "单曲循环",
                                Toast.LENGTH_SHORT).show();
                        shuffle = 2;
                    } else if (shuffle == 2) {
                        mRepeat.setImageDrawable(mNoShuffle);
                        PlayerService.mPlayState.setMode(0);
                        Toast.makeText(PlayerActivity.this, "列表循环",
                                Toast.LENGTH_SHORT).show();
                        shuffle = 0;
                    }
                    break;
                case R.id.nowPlaying_list:
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            PlayQueueFragment playQueueFragment = new PlayQueueFragment();
                            playQueueFragment.show(getSupportFragmentManager(), "playqueuefragment");
                         }
                    }, 60);
                    break;
                default:
                    break;
            }
        }

    }
    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {

            if (progress == PlayerService.mPlayState.getDuration()) {
                mLrcView.setNextTime();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mPlayerService.seekTo(seekBar.getProgress());
            mLrcView.onDrag(seekBar.getProgress());
        }
    }
    public void loadLrc(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(url)
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    Gson gson = new Gson();
                    Lrc lrc = gson.fromJson(responseData, Lrc.class);
                    Log.d(TAG, "run: " + lrc.getLrc());
                    mLrcView.loadLrc(lrc.getLrc(), lrc.getLrc_cn());
                } catch (Exception e) {
                    Log.d(TAG, "歌词解析失败");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public class PlayerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MUSIC_CURRENT)) {
                Track track = PlayState.getCurrentMusic(PlayerService.tracks, PlayerService.mPlayState.getCurrentPosition());
                if (track == null) {
                    return;
                }
                //当前标题
                String title = mToolbarTitle.getText().toString().trim();
                String artist = mToolbarArtist.getText().toString().trim();
                if(!title.equals(track.getTitle().trim()) || !artist.equals(track.getArtist().trim())) {
                    //刷新文本信息
                    mToolbarTitle.setText(track.getTitle());
                    mToolbarArtist.setText(track.getArtist());

                    if (track.getAlbumid() == 0) {
                        // 封面
                        String thumb = track.getCover();
                        Glide.with(getBaseContext()).load(thumb).into(mBackgroundImage);
                        Glide.with(getBaseContext()).load(thumb).into(mPlayingCover);
                        loadLrc("https://poche.fm/api/app/lyrics/" + track.getID());
                    } else {
                        long albumid = track.getAlbumid();
                        long trackid = track.getID();
                        Bitmap bitmap = poche.fm.potunes.Model.MediaUtil.getArtwork(getBaseContext(), trackid, albumid, true, false);
                        mBackgroundImage.setImageBitmap(bitmap);
                        mPlayingCover.setImageBitmap(bitmap);
                    }

                    List<Track> tracks = DataSupport.select("track_id").where("artist = ? and name = ?", track.getArtist(), track.getTitle()).find(Track.class);
                    if (tracks.size() > 0) {
                        Log.d(TAG, "onReceive: " + "本地音乐" + tracks.get(0).getID());
                        loadLrc("https://poche.fm/api/app/lyrics/" + tracks.get(0).getID());
                    } else {
                        mLrcView.loadLrc(null, null);
                    }

                }

                currentTime = PlayerService.mPlayState.getProgress();
                if (mLrcView.hasLrc()) {
                    mLrcView.updateTime(currentTime);
                }
                mStart.setText(MediaUtil.formatTime(currentTime));
                mSeekbar.setProgress((int)currentTime);
                mSeekbar.setMax((int)PlayerService.mPlayState.getDuration());
                duration = PlayerService.mPlayState.getDuration();
                if (duration > 0) {
                    mEnd.setText(MediaUtil.formatTime(duration - currentTime));
                }

                boolean isMediaPlaying = PlayerService.mPlayState.isPlaying();
                Log.d(TAG, "onReceive: " + isMediaPlaying);
                if (!isMediaPlaying) {
                    mPlayPause.setImageDrawable(mPlayDrawable);
                } else {
                    mPlayPause.setImageDrawable(mPauseDrawable);
                }

                shuffle = PlayerService.mPlayState.getMode();
                if (shuffle == 0) {
                    mRepeat.setImageDrawable(mNoShuffle);
                } else if (shuffle == 1) {
                    mRepeat.setImageDrawable(mShuffle);
                } else {
                    mRepeat.setImageDrawable(mSingle);
                }
            }
        }
    }
    // 返回上个页面
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    // 销毁
    @Override
    protected  void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }
    private void registeReceiver() {
        //定义和注册广播接收器
        playerReceiver = null;
        playerReceiver = new PlayerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MUSIC_CURRENT);
        registerReceiver(playerReceiver, filter);
    }
    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        Track track = PlayState.getCurrentMusic(PlayerService.tracks , PlayerService.mPlayState.getCurrentPosition());
        if (track != null) {
            registeReceiver();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        if (playerReceiver != null) {
            unregisterReceiver(playerReceiver);
            playerReceiver = null;
        }
        MobclickAgent.onPause(this);
        super.onPause();
    }

}
