package poche.fm.potunes.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.MediaUtil;
import poche.fm.potunes.Model.PlayState;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.PlayerActivity;
import poche.fm.potunes.R;
import poche.fm.potunes.domain.AppConstant;

/**
 * Created by purchas on 2017/1/14.
 */

@SuppressLint("NewApi")
public class PlayerService extends Service implements MediaPlayer.OnCompletionListener {

    private MediaPlayer mediaPlayer; //媒体播放器对象
    private int msg;
    public static ArrayList<Track> tracks;
    private NotificationManager mManager;
    private RemoteViews mBigContentViews;
    private RemoteViews mContentViews;
    private Bitmap bitmap;
    private Notification notification;
    private AudioManager mAudioManager;
    private HeadsetPlugReceiver headsetPlugReceiver;

    // 服务要发送的一些Action
    public static final String MUSIC_CURRENT = "fm.poche.action.MUSIC_CURRENT";  //当前音乐播放时间更新动作
    public static final String TAG = "PlayerService";


    public static final int MODE_ORDER = 0;
    public static final int MODE_RANDOM = 1;
    public static final int MODE_REPEAT = 2;

    public static PlayState mPlayState = new PlayState();

    public PlayerService() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        mExecutorService.execute(updateStatusRunnable);
        return new PlayBinder();
    }
    public class PlayBinder extends Binder {
        public PlayerService getPlayerService() {
            return PlayerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        msg = intent.getIntExtra("MSG", 0);
        if (msg == AppConstant.PlayerMsg.PLAY_MSG) {
            play(0);
        } else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) {
            //暂停
            pause();
        } else if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) { //继续播放
            resume();
        } else if (msg == AppConstant.PlayerMsg.PRIVIOUS_MSG) { //上一首
            previous();
        } else if (msg == AppConstant.PlayerMsg.NEXT_MSG) {
            //下一首
            next();
        }
        getNotification();
        if (msg == AppConstant.PlayerMsg.STOP_MSG) {
            mManager.cancel(1);
        }
        return super.onStartCommand(intent, flags, startId);
    }



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
        if(mediaPlayer != null ) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //播放完成时进行下一曲
        if (!mediaPlayer.isLooping()) {
            mAudioManager.abandonAudioFocus(audioFocusChangeListener);
        }
        next();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        registerHeadsetPlugReceiver();
    }

    /**
     * 播放音乐
     */
    public void play(final int position) {
        if (requestFocus()) {
            Track track = tracks.get(position);
            String url;
            List<Track> tracks = DataSupport.select("url").where("artist = ? and name = ? and isDownloaded = ?", track.getArtist(), track.getTitle(), "1").find(Track.class);
            if (tracks.size() > 0) {
                url = tracks.get(0).getUrl();
            } else {
                url = track.getUrl();
            }
            try {
                mediaPlayer.reset();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mp, int percent) {
                        Log.d(TAG, "onBufferingUpdate: " + percent);
                    }
                });
                mPlayState.setCurrentPosition(position);
                mPlayState.setPlaying(true);

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mPlayState.setDuration(mediaPlayer.getDuration());
                        mediaPlayer.start();
                        getNotification();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mPlayState.setPlaying(false);
            Log.d(TAG, "pause: " + mPlayState.isPlaying());
            mediaPlayer.pause();
            getNotification();
        }
    }
    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mPlayState.setPlaying(true);
            mediaPlayer.start();
            getNotification();
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
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }
    public void seekTo(int position) {
        if (mediaPlayer != null) mediaPlayer.seekTo(position);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }

        if (headsetPlugReceiver != null) {
            unregisterReceiver(headsetPlugReceiver);
        }
        return false;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
    }
    private void getNotification() {
        if (tracks == null) return;
        //通知栏
        Track track = tracks.get(mPlayState.getCurrentPosition());
        mBigContentViews = new RemoteViews(getPackageName(), R.layout.layout_notification);
        mBigContentViews.setTextViewText(R.id.notification_title, track.getTitle());
        mBigContentViews.setTextViewText(R.id.notification_artist, track.getArtist());
        mBigContentViews.setImageViewResource(R.id.notification_cover, R.drawable.placeholder_disk_210);
        //上一首
        Intent prev = new Intent();
        prev.setAction("fm.poche.media.MUSIC_SERVICE");
        prev.putExtra("MSG", AppConstant.PlayerMsg.PRIVIOUS_MSG);
        PendingIntent intent_prev = PendingIntent.getService(this, 1, prev, PendingIntent.FLAG_UPDATE_CURRENT);
        mBigContentViews.setOnClickPendingIntent(R.id.notification_prev, intent_prev);
        // 下一首
        Intent next = new Intent();
        next.setAction("fm.poche.media.MUSIC_SERVICE");
        next.putExtra("MSG", AppConstant.PlayerMsg.NEXT_MSG);
        PendingIntent intent_next = PendingIntent.getService(this, 2, next, PendingIntent.FLAG_UPDATE_CURRENT);
        mBigContentViews.setOnClickPendingIntent(R.id.notification_next, intent_next);

        // 暂停
        Intent playOrPause = new Intent();
        playOrPause.setAction("fm.poche.media.MUSIC_SERVICE");
        if (mPlayState.isPlaying()) {
            playOrPause.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
        } else {
            playOrPause.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG);
        }
        mBigContentViews.setImageViewResource(R.id.notification_pause, mPlayState.isPlaying() ? R.drawable.noti_pause : R.drawable.noti_play);
        PendingIntent intent_play = PendingIntent.getService(this, 3, playOrPause, PendingIntent.FLAG_UPDATE_CURRENT);
        mBigContentViews.setOnClickPendingIntent(R.id.notification_pause, intent_play);
        //删除通知
        Intent close = new Intent();
        close.setAction("fm.poche.media.MUSIC_SERVICE");
        close.putExtra("MSG", AppConstant.PlayerMsg.STOP_MSG);
        PendingIntent intent_close = PendingIntent.getService(this, 5, close, PendingIntent.FLAG_UPDATE_CURRENT);
        mBigContentViews.setOnClickPendingIntent(R.id.notification_close, intent_close);
        // small
        mContentViews = new RemoteViews(getPackageName(), R.layout.layout_notification_small);
        mContentViews.setTextViewText(R.id.notification_title_small, track.getTitle());
        mContentViews.setTextViewText(R.id.notification_artist_small, track.getArtist());
        mContentViews.setImageViewResource(R.id.notification_cover_small, R.drawable.placeholder_disk_210);
        mContentViews.setInt(R.id.layout, "setBackgroundColor", R.color.colorAccent);
        mContentViews.setOnClickPendingIntent(R.id.notification_next_small, intent_next);
        mContentViews.setOnClickPendingIntent(R.id.notification_prev_small, intent_prev);
        mContentViews.setImageViewResource(R.id.notification_pause_small, mPlayState.isPlaying() ? R.drawable.noti_pause : R.drawable.noti_play);
        mContentViews.setOnClickPendingIntent(R.id.notification_pause_small, intent_play);
        mContentViews.setOnClickPendingIntent(R.id.notification_close_small, intent_close);

        if (track.getAlbumid() == 0) {
            //设置封面
            Glide.with(getBaseContext()).load(track.getCover()).asBitmap().into(new SimpleTarget<Bitmap>(200, 200) {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {

                    mBigContentViews.setImageViewBitmap(R.id.notification_cover, resource);
                    mContentViews.setImageViewBitmap(R.id.notification_cover_small, resource);
                    bitmap = resource;
                    Intent remoteIntent = new Intent(getBaseContext(), PlayerActivity.class);
                    remoteIntent.putExtra("isPlaying", mPlayState.isPlaying());
                    PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 4, remoteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext());

                    mBuilder.setCustomBigContentView(mBigContentViews)
                            .setCustomContentView(mContentViews)
                            .setTicker("正在播放")
                            .setWhen(System.currentTimeMillis())
                            .setOngoing(true)
                            .setContentIntent(pi)
                            .setSmallIcon(R.drawable.actionbar_discover_selected);
                    notification = mBuilder.build();
                    mManager.notify(1, notification);
                }
            });
        } else {
            long albumid = track.getAlbumid();
            long track_id = track.getID();
            bitmap = MediaUtil.getArtwork(getBaseContext(), track_id, albumid, true, false);
            mBigContentViews.setImageViewBitmap(R.id.notification_cover, bitmap);
            Intent remoteIntent = new Intent(getBaseContext(), PlayerActivity.class);
            remoteIntent.putExtra("isPlaying", mPlayState.isPlaying());
            PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 4, remoteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext())
                    .setCustomContentView(mBigContentViews)
                    .setCustomBigContentView(mBigContentViews)
                    .setTicker("正在播放")
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark);
            notification = mBuilder.build();
            mManager.notify(1, notification);
        }
    }

    // 来电监听
    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                // Pause playback
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                Log.d(TAG, "onAudioFocusChange: " + focusChange);
                // Resume playback
                resume();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // mAm.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                mAudioManager.abandonAudioFocus(audioFocusChangeListener);
                // Stop playback
                stop();
            }
        }
    };

    private boolean requestFocus() {
        // Request audio focus for playback
        int result = mAudioManager.requestAudioFocus(audioFocusChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    // 耳机插拔
    public class HeadsetPlugReceiver extends BroadcastReceiver {
        private static final String TAG = "HeadsetPlugReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mediaPlayer == null) return;
            if (intent.hasExtra("state")){
                if (intent.getIntExtra("state", 0) == 0){
                    // 拔出耳机
                    if (mediaPlayer.isPlaying())  {
                        pause();
                    }
                    Toast.makeText(context, R.string.earphone_unplugged, Toast.LENGTH_LONG).show();
                }
                else if (intent.getIntExtra("state", 0) == 1){
                    if (!mediaPlayer.isPlaying()) {
                        resume();
                    }
                    Toast.makeText(context, R.string.earphone_plugged, Toast.LENGTH_LONG).show();
                }
            }
        }

    }

    private void registerHeadsetPlugReceiver() {
        headsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(headsetPlugReceiver, intentFilter);
    }

}

