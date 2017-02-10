package poche.fm.potunes.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import poche.fm.potunes.Model.PlayState;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.PlayerActivity;
import poche.fm.potunes.R;
import poche.fm.potunes.domain.AppConstant;
import poche.fm.potunes.fragment.QuickControlsFragment;

/**
 * Created by purchas on 2017/1/14.
 */

@SuppressLint("NewApi")
public class PlayerService extends Service implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer; //媒体播放器对象
    private int msg;
    private boolean isPause; //暂停
    private int current = 0; //记录当前在播放的音乐
    public static ArrayList<Track> tracks;
    private int status = 0; //播放状态，默认顺序播放
    private int currentTime;
    private int duration;
    private Bitmap bitmap;
    private NotificationManager mManager;
    private RemoteViews contentViews;
    private AudioManager mAudioManager;

    // 服务要发送的一些Action
    public static final String MUSIC_CURRENT = "fm.poche.action.MUSIC_CURRENT";  //当前音乐播放时间更新动作
    public static final String TAG = "PlayerService";


    public static final int MODE_ORDER = 0;
    public static final int MODE_RANDOM = 1;
    public static final int MODE_REPEAT = 2;

    public static PlayState mPlayState = new PlayState();


    //创建单个线程池
    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    private Runnable updateStatusRunnable = new Runnable() {
        @Override
        public void run() {
            while(true){
                mPlayState.setProgress(getCurrentProgress());
                Intent intent = new Intent();
                intent.setAction(MUSIC_CURRENT);
                sendBroadcast(intent);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public int getCurrentProgress(){
        if(mediaPlayer != null){
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public class PlayerServiceBinder extends Binder {
        public PlayerService getPlayService(){
            return PlayerService.this;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //播放完成时进行下一曲
        next();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        mExecutorService.execute(updateStatusRunnable);
        return new PlayerServiceBinder();
    }

    /**
     * 播放音乐
     */
    public void play(final int position) {
        Track track = tracks.get(position);
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(track.getUrl());
            mediaPlayer.prepareAsync();

            mPlayState.setCurrentPosition(position);
            mPlayState.setPlaying(true);

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mPlayState.setDuration(mediaPlayer.getDuration());
                    mediaPlayer.start();
                }
            });

        } catch (Exception e) {
            Log.d(TAG, "play: 音乐地址解析错误");
            e.printStackTrace();
        }

    }
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mPlayState.setPlaying(false);
            mediaPlayer.pause();
            isPause = true;
            mAudioManager.abandonAudioFocus(this);
        }
    }
    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mPlayState.setPlaying(true);
            mediaPlayer.start();
        }
    }
    public void previous() {
        int currentPosition = mPlayState.getCurrentPosition();
        if (currentPosition == 0) {
            currentPosition = tracks.size() - 1;
        } else {
            currentPosition--;
        }
        mPlayState.setCurrentPosition(currentPosition);
        play(currentPosition);
    }
    public void next() {
        int currentPosition = mPlayState.getCurrentPosition();
        int mode = mPlayState.getMode();
        if(tracks != null) {
            switch (mode) {
                case MODE_ORDER:
                    if (currentPosition == tracks.size() - 1) {
                        currentPosition = 0;
                    } else {
                        currentPosition++;
                    }
                    break;
                case MODE_RANDOM:
                    Random random = new Random(System.currentTimeMillis());
                    currentPosition = random.nextInt(tracks.size());
                    break;
                case MODE_REPEAT:
                    break;
            }

            mPlayState.setCurrentPosition(currentPosition);
            play(currentPosition);
        }
    }
    public void seekTo(int position) {
        if (mediaPlayer != null) mediaPlayer.seekTo(position);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutorService.shutdown();
    }

    private void getNotification() {
        //通知栏
        Track track = tracks.get(current);
        contentViews = new RemoteViews(getPackageName(), R.layout.layout_notification);
        contentViews.setTextViewText(R.id.notification_title, track.getTitle());
        contentViews.setTextViewText(R.id.notification_artist, track.getArtist());
        contentViews.setImageViewResource(R.id.notification_cover, R.drawable.placeholder_disk_210);
        //上一首
        Intent prev = new Intent();
        prev.setAction("fm.poche.media.MUSIC_SERVICE");
        prev.putExtra("MSG", AppConstant.PlayerMsg.PRIVIOUS_MSG);
        PendingIntent intent_prev = PendingIntent.getService(getBaseContext(), 1, prev, PendingIntent.FLAG_UPDATE_CURRENT);
        contentViews.setOnClickPendingIntent(R.id.notification_prev, intent_prev);
        // 下一首
        Intent next = new Intent();
        next.setAction("fm.poche.media.MUSIC_SERVICE");
        next.putExtra("MSG", AppConstant.PlayerMsg.NEXT_MSG);
        PendingIntent intent_next = PendingIntent.getService(getBaseContext(), 2, next, PendingIntent.FLAG_UPDATE_CURRENT);
        contentViews.setOnClickPendingIntent(R.id.notification_next, intent_next);
        // 暂停
        Intent playOrPause = new Intent();
        playOrPause.setAction("fm.poche.media.MUSIC_SERVICE");
        if (isPause) {
            playOrPause.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG);
        } else {
            playOrPause.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
        }
        contentViews.setImageViewResource(R.id.notification_pause, isPause ? R.drawable.note_btn_play : R.drawable.note_btn_pause);
        PendingIntent intent_play = PendingIntent.getService(getBaseContext(), 3, playOrPause, PendingIntent.FLAG_UPDATE_CURRENT);
        contentViews.setOnClickPendingIntent(R.id.notification_pause, intent_play);

        // 设置封面
        Glide.with(getBaseContext()).load(track.getCover()).asBitmap().into(new SimpleTarget<Bitmap>(200, 200) {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                Intent remoteIntent = new Intent(getBaseContext(), PlayerActivity.class);
                remoteIntent.putExtra("isPlaying", !isPause);
                PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 4, remoteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                contentViews.setImageViewBitmap(R.id.notification_cover, resource);
                bitmap = resource;

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext())
                        .setCustomContentView(contentViews)
                        .setCustomBigContentView(contentViews)
                        .setTicker("正在播放")
                        .setWhen(System.currentTimeMillis())
                        .setOngoing(true)
                        .setContentIntent(pi)
                        .setSmallIcon(R.drawable.ic_cast_dark);
                Notification notification = mBuilder.build();
                mManager.notify(1, notification);
            }
        });
    }

    //捕获、丢弃音乐焦点
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (!isPause) {
                    pause();
                }
                break;
        }
    }
}

