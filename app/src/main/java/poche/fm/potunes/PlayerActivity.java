package poche.fm.potunes;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
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

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

import poche.fm.potunes.Model.Track;

public class PlayerActivity extends AppCompatActivity {

    private TextView mStart;

    public static final String TRACKLIST = "tracklist";
    public static final String TRACKID = "trackid";
    public static final String ALBUM = "ALBUM";

    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

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
        ProgressBar mLoading = (ProgressBar) findViewById(R.id.progressBar1);
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

        try {
            mediaPlayer.setDataSource(track.getUrl());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }


        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                        getSupportMediaController().getTransportControls();
                controls.skipToNext();
            }
        });

        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                        getSupportMediaController().getTransportControls();
                controls.skipToPrevious();
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackStateCompat state = getSupportMediaController().getPlaybackState();
                if (state != null) {
                    MediaControllerCompat.TransportControls controls =
                            getSupportMediaController().getTransportControls();
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING: // fall through
                        case PlaybackStateCompat.STATE_BUFFERING:
                            controls.pause();
//                            stopSeekbarUpdate();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                        case PlaybackStateCompat.STATE_STOPPED:
                            controls.play();
//                            scheduleSeekbarUpdate();
                            break;
                        default:
//                            LogHelper.d(TAG, "onClick with state ", state.getState());
                    }
                }
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStart.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getSupportMediaController().getTransportControls().seekTo(seekBar.getProgress());
//                scheduleSeekbarUpdate();
            }
        });

        // Only update from the intent if we are not recreating from a config change:
        if (savedInstanceState == null) {
//            updateFromParams(getIntent());
        }

//        mMediaBrowser = new MediaBrowserCompat(this,
//                new ComponentName(this, MusicService.class), mConnectionCallback, null);


    }
    // 返回上个页面
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                Log.d("", "onOptionsItemSelected: ");
                onDestroy();
                onBackPressed();
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
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }
}
