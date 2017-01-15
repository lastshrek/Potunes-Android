package poche.fm.potunes;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
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

public class PlayerActivity extends AppCompatActivity {

    private TextView mStart;
    // 传值用
    public static final String TRACKLIST = "tracklist";
    public static final String TRACKID = "trackid";
    public static final String ALBUM = "ALBUM";
    public static final String CTL_ACTION = "fm.poche.action.CTL_ACTION";        //控制动作


    private Player player; //播放器
    private ProgressBar mLoading;

    private static final int PROCESSING = 1;
    private static final int FAILURE = -1;

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

        //initials
        ImageView mBackgroundImage = (ImageView) findViewById(R.id.background_image);
        Drawable mPauseDrawable = ContextCompat.getDrawable(this, R.drawable.uamp_ic_pause_white_48dp);
        Drawable mPlayDrawable = ContextCompat.getDrawable(this, R.drawable.uamp_ic_play_arrow_white_48dp);
        ImageView mPlayPause = (ImageView) findViewById(R.id.play_pause);
        ImageView mSkipNext = (ImageView) findViewById(R.id.next);
        ImageView mSkipPrev = (ImageView) findViewById(R.id.prev);
        mStart = (TextView) findViewById(R.id.startText);
        TextView mEnd = (TextView) findViewById(R.id.endText);
        SeekBar mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        TextView mLine1 = (TextView) findViewById(R.id.line1);
        TextView mLine2 = (TextView) findViewById(R.id.line2);
        TextView mLine3 = (TextView) findViewById(R.id.line3);

        mLoading = (ProgressBar) findViewById(R.id.progressBar1);
        View mControllers = findViewById(R.id.controllers);
        mControllers.setVisibility(View.VISIBLE);


        Intent intent = getIntent();

        int track_id = intent.getIntExtra(TRACKID, -1);
        ArrayList<Track> tracks =  (ArrayList<Track>) intent.getSerializableExtra(TRACKLIST);
        Track track = tracks.get(track_id);
        // 设置专辑封面
        String thumb = track.getCover();
        Glide.with(getBaseContext()).load(thumb).into(mBackgroundImage);
        // 歌手名
        mLine1.setText(track.getTitle());
        mLine2.setText(track.getArtist());

        player = new Player(mSeekbar);

        player.pause();
        player.playUrl(track.getUrl());
        player.play();







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
//        if (player != null) {
//            player.stop();
//            player = null;
//        }
    }
}
