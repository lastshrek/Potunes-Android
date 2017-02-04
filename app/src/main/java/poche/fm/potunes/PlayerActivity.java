package poche.fm.potunes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
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
import com.google.gson.reflect.TypeToken;
import com.malinskiy.materialicons.IconDrawable;
import com.malinskiy.materialicons.Iconify;
import com.malinskiy.materialicons.widget.IconTextView;

import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.Model.Lrc;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.domain.AppConstant;
import poche.fm.potunes.fragment.PlayQueueFragment;
import poche.fm.potunes.lrc.LrcView;
import poche.fm.potunes.utils.MediaUtil;
import poche.fm.potunes.widgets.TintImageView;


public class PlayerActivity extends AppCompatActivity {

    // 传值用
    public static final String TAG = "PlayerActivity"; // 更新动作
    public static final String UPDATE_ACTION = "fm.poche.action.UPDATE_ACTION"; // 更新动作
    public static final String CTL_ACTION = "fm.poche.action.CTL_ACTION"; // 控制动作
    public static final String MUSIC_CURRENT = "fm.poche.action.MUSIC_CURRENT"; // 音乐当前时间改变动作
    public static final String MUSIC_DURATION = "fm.poche.action.MUSIC_DURATION";// 音乐播放长度改变动作
    public static final String SHUFFLE_ACTION = "fm.poche.action.SHUFFLE_ACTION";// 音乐随机播放动作

    private Toolbar toolbar;
    private TextView mToolbarTitle;
    private TextView mToolbarArtist;
    private ProgressBar mLoading;
    private SeekBar mSeekbar;
    private TintImageView mPlayPause;
    private TextView mStart;
    private TextView mEnd;
    private IconDrawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private IconTextView mSkipPrev;
    private IconTextView mSkipNext;
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
    private IconTextView mPlaylist;



    private int currentTime; // 当前歌曲播放时间
    private int position; // 播放歌曲在tracks的位置
    private String url; // 歌曲路径
    private ArrayList<Track> tracks;
    private boolean isPlaying; // 暂停
    private int duration;

    // 0:noshuffle, 1:single, 2:shuffle
    private int shuffle;


    private static final int PROCESSING = 1;
    private static final int FAILURE = -1;
    private PlayerReceiver playerReceiver;
    private Handler handler = new UIHandler();

    private final class UIHandler extends Handler{
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROCESSING: //更新进度
                    mLoading.setProgress(msg.getData().getInt("size"));
                    float num = (float) mLoading.getProgress() / (float) mLoading.getMax();
                    int result = (int) (num * 100);
                    if (mLoading.getProgress() == mLoading.getMax()) {
                        Toast.makeText(getApplicationContext(), "缓冲完成", Toast.LENGTH_LONG).show();
                    }
                    break;
                case FAILURE:
                    Toast.makeText(getApplicationContext(), "缓冲失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            ab.setHomeAsUpIndicator(R.drawable.actionbar_back);
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
        //获取本地Tracks数据
        SharedPreferences preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        String json = preferences.getString("Tracks", "Tracks");
        position = preferences.getInt("position", 0);
        Gson gson = new Gson();
        ArrayList<Track> datas = gson.fromJson(json, new TypeToken<List<Track>>(){}.getType());
        for (int i = 0; i < datas.size(); i++) {
            if (i == position) {
                Track track = datas.get(i);
                loadLrc("https://poche.fm/api/app/lyrics/" + track.getID());
            }
        }
        tracks = datas;

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
        mNoShuffle = new IconDrawable(this, Iconify.IconValue.zmdi_repeat).colorRes(R.color.white).sizeDp(40);
        mShuffle = new IconDrawable(this, Iconify.IconValue.zmdi_shuffle).colorRes(R.color.white).sizeDp(40);
        mSingle = new IconDrawable(this, Iconify.IconValue.zmdi_repeat_one).colorRes(R.color.white).sizeDp(40);
        mRepeat.setImageDrawable(mNoShuffle);
        mPlayPause = (TintImageView) findViewById(R.id.play_pause);
        mPauseDrawable = new IconDrawable(this, Iconify.IconValue.zmdi_pause_circle_outline).colorRes(R.color.white).sizeDp(40);
        mPlayDrawable = new IconDrawable(this, Iconify.IconValue.zmdi_play_circle_outline).colorRes(R.color.white).sizeDp(40);
        mSkipNext = (IconTextView) findViewById(R.id.play_next);
        mSkipPrev = (IconTextView) findViewById(R.id.play_prev);
        Iconify.addIcons(mSkipPrev);
        Iconify.addIcons(mSkipNext);

        mStart = (TextView) findViewById(R.id.startText);
        mEnd = (TextView) findViewById(R.id.endText);

        mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        duration = preferences.getInt("duration", -1);
        shuffle = preferences.getInt("shuffle", 0);
        if (shuffle == -1) {
            shuffle = 0;
        }
        if (shuffle == 0) {
            mRepeat.setImageDrawable(mNoShuffle);
        } else if (shuffle == 1) {
            mRepeat.setImageDrawable(mSingle);
        } else {
            mRepeat.setImageDrawable(mShuffle);
        }
        mSeekbar.setMax(duration);


        mLoading = (ProgressBar) findViewById(R.id.progressBar1);
        View mControllers = findViewById(R.id.controllers);
        mControllers.setVisibility(View.VISIBLE);

        Intent intent = getIntent();
        isPlaying = intent.getBooleanExtra("isPlaying", false);

        if (!isPlaying) {
            mPlayPause.setImageDrawable(mPlayDrawable);
        } else {
            mPlayPause.setImageDrawable(mPauseDrawable);
        }
        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next_music();
            }
        });
        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previous_music();
            }
        });
        dividerLine = (View) findViewById(R.id.view_line);
        mPlaylist = (IconTextView) findViewById(R.id.nowPlaying_list);
        Iconify.addIcons(mPlaylist);
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
            Intent intent = new Intent();
            intent.setAction("fm.poche.media.MUSIC_SERVICE");
            switch (v.getId()) {
                case R.id.play_pause:
                    if (!isPlaying) {
                        mPlayPause.setVisibility(View.VISIBLE);
                        mPlayPause.setImageDrawable(mPauseDrawable);
                        intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG);
                        isPlaying = true;
                    } else {
                        mPlayPause.setVisibility(View.VISIBLE);
                        mPlayPause.setImageDrawable(mPlayDrawable);
                        intent.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
                        isPlaying = false;
                    }
                    intent.putExtra("url", tracks.get(position).getUrl());
                    intent.putExtra("position", position);
                    intent.setPackage(getPackageName());
                    getBaseContext().startService(intent);
                    break;
                case R.id.play_repeat:
                    Intent shuffleIntent = new Intent(SHUFFLE_ACTION);

                    if(shuffle == 0) {
                        mRepeat.setImageDrawable(mSingle);
                        Toast.makeText(PlayerActivity.this, "单曲循环",
                                Toast.LENGTH_SHORT).show();
                        shuffle = 1;
                    } else if (shuffle == 1) {
                        mRepeat.setImageDrawable(mShuffle);
                        Toast.makeText(PlayerActivity.this, "随机播放",
                                Toast.LENGTH_SHORT).show();
                        shuffle = 2;
                    } else if (shuffle == 2) {
                        mRepeat.setImageDrawable(mNoShuffle);
                        Toast.makeText(PlayerActivity.this, "列表循环",
                                Toast.LENGTH_SHORT).show();
                        shuffle = 0;
                    }
                    shuffleMusic(shuffle);
                    sendBroadcast(shuffleIntent);
                    // 存储播放状态
                    SharedPreferences preference = getSharedPreferences("user",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preference.edit();
                    editor.putInt("shuffle", shuffle);
                    editor.apply();

                    break;
                case R.id.seekBar1:

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
            switch(seekBar.getId()) {
                case R.id.seekBar1:
                    if (fromUser) {
                         // 用户控制进度的改变
                    }
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            audioTrackChange(seekBar.getProgress());
        }
    }

    public void next_music() {
        Intent intent = new Intent();
        intent.setAction("fm.poche.media.MUSIC_SERVICE");
        intent.putExtra("TRACKS", tracks);
        intent.putExtra("MSG", AppConstant.PlayerMsg.NEXT_MSG);
        intent.setPackage(getPackageName());
        startService(intent);
    }

    public void previous_music() {
        Intent intent = new Intent();
        intent.setAction("fm.poche.media.MUSIC_SERVICE");
        intent.putExtra("TRACKS", tracks);
        intent.putExtra("MSG", AppConstant.PlayerMsg.PRIVIOUS_MSG);
        intent.setPackage(getPackageName());
        startService(intent);
    }


    public void shuffleMusic(int control) {
        Intent intent = new Intent(CTL_ACTION);
        intent.putExtra("control", control);
        sendBroadcast(intent);
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
                    mLrcView.loadLrc(lrc.getLrc(), lrc.getLrc_cn());

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }
    private void registeReceiver() {
        //定义和注册广播接收器
        playerReceiver = null;
        playerReceiver = new PlayerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_ACTION);
        filter.addAction(MUSIC_CURRENT);
        filter.addAction(MUSIC_DURATION);
        registerReceiver(playerReceiver, filter);
    }

    /**
     * 播放进度改变
     * @param progress
     */
    public void audioTrackChange(int progress) {
        Intent intent = new Intent();
        intent.setAction("fm.poche.media.MUSIC_SERVICE");
        intent.putExtra("MSG", AppConstant.PlayerMsg.PROGRESS_CHANGE);
        intent.putExtra("progress", progress);
        intent.setPackage(getPackageName());
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registeReceiver();
    }

    /**
     * 反注册广播
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    public class PlayerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            currentTime = intent.getIntExtra("currentTime", -1);
            if (mLrcView.hasLrc()) {
                mLrcView.updateTime(currentTime);
            }

            if (action.equals(MUSIC_CURRENT)) {
                mStart.setText(MediaUtil.formatTime(currentTime));
                mSeekbar.setProgress(currentTime);

                if (duration > 0) {
                    mEnd.setText(MediaUtil.formatTime(duration - currentTime));
                }
                String thumb = intent.getStringExtra("url");
                Glide.with(getBaseContext()).load(thumb).into(mBackgroundImage);
                Glide.with(getBaseContext()).load(thumb).into(mPlayingCover);
                mToolbarTitle.setText(intent.getStringExtra("title"));
                mToolbarArtist.setText(intent.getStringExtra("artist"));

                boolean isMediaPlaying = intent.getBooleanExtra("isPlaying", false);
                if (!isMediaPlaying) {
                    mPlayPause.setImageDrawable(mPlayDrawable);
                } else {
                    mPlayPause.setImageDrawable(mPauseDrawable);
                }

            }  else if (action.equals(UPDATE_ACTION)) {
                position = intent.getIntExtra("current", -1);
                Track track = tracks.get(position);
                url = track.getUrl();
                if (position >= 0) {
                    // 设置专辑封面
                    String thumb = track.getCover();
                    Glide.with(getBaseContext()).load(thumb).into(mBackgroundImage);
                    Glide.with(getBaseContext()).load(thumb).into(mPlayingCover);
                    // 歌手名
                    mToolbarTitle.setText(track.getTitle());
                    mToolbarArtist.setText(track.getArtist());
                    // 加载歌词
                    loadLrc("https://poche.fm/api/app/lyrics/" + track.getID());
                }

            } else if (action.equals(MUSIC_DURATION)) {
                duration = intent.getIntExtra("duration", -1);
                tracks = (ArrayList<Track>) intent.getSerializableExtra("TRACKS");
                mSeekbar.setMax(duration);
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
        try {
            unregisterReceiver(playerReceiver);
            playerReceiver = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
