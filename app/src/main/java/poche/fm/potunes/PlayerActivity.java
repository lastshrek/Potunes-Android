package poche.fm.potunes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import poche.fm.potunes.Model.Player;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.domain.AppConstant;
import poche.fm.potunes.utils.MediaUtil;

public class PlayerActivity extends AppCompatActivity {

    // 传值用
    public static final String TAG = "PlayerActivity"; // 更新动作

    public static final String TRACKLIST = "tracklist";
    public static final String TRACKID = "trackid";
    public static final String ALBUM = "ALBUM";
    public static final String UPDATE_ACTION = "fm.poche.action.UPDATE_ACTION"; // 更新动作
    public static final String CTL_ACTION = "fm.poche.action.CTL_ACTION"; // 控制动作
    public static final String MUSIC_CURRENT = "fm.poche.action.MUSIC_CURRENT"; // 音乐当前时间改变动作
    public static final String MUSIC_DURATION = "fm.poche.action.MUSIC_DURATION";// 音乐播放长度改变动作
    public static final String MUSIC_PLAYING = "fm.poche.action.MUSIC_PLAYING"; // 音乐正在播放动作
    public static final String REPEAT_ACTION = "fm.poche.action.REPEAT_ACTION"; // 音乐重复播放动作
    public static final String SHUFFLE_ACTION = "fm.poche.action.SHUFFLE_ACTION";// 音乐随机播放动作
    public static final String SHOW_LRC = "fm.poche..action.SHOW_LRC"; // 通知显示歌词


    private ProgressBar mLoading;
    private SeekBar mSeekbar;
    private TextView mTitle;
    private TextView mArtist;
    private ImageView mPlayPause;
    private TextView mStart;
    private TextView mEnd;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private ImageView mSkipPrev;
    private ImageView mSkipNext;
    private ImageView mBackgroundImage;

    private int currentTime; // 当前歌曲播放时间
    private SeekBar music_progressBar; // 歌曲进度
    private TextView currentProgress; // 当前进度消耗的时间
    private TextView finalProgress; // 歌曲时间
    private int position; // 播放歌曲在tracks的位置
    private String title; // 歌曲标题
    private String artist; // 歌曲艺术家
    private String url; // 歌曲路径
    private ArrayList<Track> tracks;
    private boolean isPause; // 暂停


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

        setContentView(R.layout.player_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");


        findViewById();
        // 设置监听器
        setViewOnclickListener();

        registeReceiver();
    }

    private void findViewById() {
        //initials
        mBackgroundImage = (ImageView) findViewById(R.id.background_image);
        mPauseDrawable   = ContextCompat.getDrawable(this, R.drawable.uamp_ic_pause_white_48dp);
        mPlayDrawable = ContextCompat.getDrawable(this, R.drawable.uamp_ic_play_arrow_white_48dp);
        mPlayPause = (ImageView) findViewById(R.id.play_pause);
        mPlayPause.setImageDrawable(mPauseDrawable);
        mSkipNext = (ImageView) findViewById(R.id.next);
        mSkipPrev = (ImageView) findViewById(R.id.prev);
        mStart = (TextView) findViewById(R.id.startText);
        mEnd = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        mTitle = (TextView) findViewById(R.id.line1);
        mArtist = (TextView) findViewById(R.id.line2);
        TextView mLine3 = (TextView) findViewById(R.id.line3);

        mLoading = (ProgressBar) findViewById(R.id.progressBar1);
        View mControllers = findViewById(R.id.controllers);
        mControllers.setVisibility(View.VISIBLE);

        // 设置页面信息
        Intent intent = getIntent();
        position = intent.getIntExtra(TRACKID, -1);
        tracks =  (ArrayList<Track>) intent.getSerializableExtra(TRACKLIST);
        showTrackInfo(position);

        isPause = false;

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
    }

    private void showTrackInfo(int position) {
        Track track = tracks.get(position);
        // 设置专辑封面
        String thumb = track.getCover();
        Glide.with(getBaseContext()).load(thumb).into(mBackgroundImage);
        // 歌手名
        mTitle.setText(track.getTitle());
        mArtist.setText(track.getArtist());
    }

    private void setViewOnclickListener() {
        ViewOnclickListener clickListener = new ViewOnclickListener();
        mPlayPause.setOnClickListener(clickListener);
    }
    
    private class ViewOnclickListener implements View.OnClickListener {
        Intent intent = new Intent();
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.play_pause:
                    if (isPause) {
                        mPlayPause.setVisibility(View.VISIBLE);
                        mPlayPause.setImageDrawable(mPauseDrawable);
                        intent.setAction("fm.poche.media.MUSIC_SERVICE");
                        intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG);
                        intent.setPackage(getPackageName());
                        getBaseContext().startService(intent);
                        isPause = false;
                    } else {
                        mPlayPause.setVisibility(View.VISIBLE);
                        mPlayPause.setImageDrawable(mPlayDrawable);
                        intent.setAction("fm.poche.media.MUSIC_SERVICE");
                        intent.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
                        intent.putExtra("url", tracks.get(position).getUrl());
                        intent.putExtra("position", position);
                        intent.putExtra("TRACKS", tracks);
                        intent.setPackage(getPackageName());
                        startService(intent);
                        isPause = true;
                    }
                    break;

                default:
                    break;
            }
        }
        
    }
    public void next_music() {
        position = position + 1;
        if (position <= tracks.size() - 1) {
            showTrackInfo(position);
            Intent intent = new Intent();
            intent.setAction("fm.poche.media.MUSIC_SERVICE");
            intent.putExtra("url", tracks.get(position).getUrl());
            intent.putExtra("position", position);
            intent.putExtra("MSG", AppConstant.PlayerMsg.NEXT_MSG);
            intent.putExtra("TRACKS", tracks);
            intent.setPackage(getPackageName());
            startService(intent);

        } else {
            position = tracks.size() - 1;
            Toast.makeText(PlayerActivity.this, "没有下一首了", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void previous_music() {
        position = position - 1;
        if (position >= 0) {
            showTrackInfo(position);
            url = tracks.get(position).getUrl();
            Intent intent = new Intent();
            intent.setAction("fm.poche.media.MUSIC_SERVICE");
            intent.putExtra("url", url);
            intent.putExtra("TRACKS", tracks);

            intent.putExtra("position", position);
            intent.putExtra("MSG", AppConstant.PlayerMsg.PRIVIOUS_MSG);
            intent.setPackage(getPackageName());
            startService(intent);
        } else {
            position = 0;
            Toast.makeText(PlayerActivity.this, "没有上一首了", Toast.LENGTH_SHORT)
                    .show();
        }
    }
    
    

    private void registeReceiver() {
        //定义和注册广播接收器
        playerReceiver = new PlayerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_ACTION);
        filter.addAction(MUSIC_CURRENT);
        filter.addAction(MUSIC_DURATION);
        registerReceiver(playerReceiver, filter);
    }
    @Override
    protected void onResume() {
        super.onResume();
        registeReceiver();
        Log.d(TAG, "onResume: " + "onResume");
    }

    /**
     * 反注册广播
     */
    @Override
    protected void onStop() {
        unregisterReceiver(playerReceiver);
        Log.d(TAG, "onStop: ");

        super.onStop();
    }

    public class PlayerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            currentTime = intent.getIntExtra("currentTime", -1);

            if (action.equals(MUSIC_CURRENT)) {
                mStart.setText(MediaUtil.formatTime(currentTime));
                mSeekbar.setProgress(currentTime);
                int duration = intent.getIntExtra("duration", -1);
                mSeekbar.setMax(duration);
                mEnd.setText(MediaUtil.formatTime(duration - currentTime));
            }  else if (action.equals(UPDATE_ACTION)) {
                position = intent.getIntExtra("current", -1);
                Track track = tracks.get(position);
                url = track.getUrl();
                if (position >= 0) {
                    // 设置专辑封面
                    String thumb = track.getCover();
                    Glide.with(getBaseContext()).load(thumb).into(mBackgroundImage);
                    // 歌手名
                    mTitle.setText(track.getTitle());
                    mArtist.setText(track.getArtist());
                }
                if (position == 0) {
                    isPause = true;
                }
            }
        }
    }
    // 返回上个页面
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                Log.d("", "onOptionsItemSelected: ");
//                onDestroy();
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
        super.onDestroy();
    }
    
    
}
