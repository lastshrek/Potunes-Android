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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.Model.Track;
import poche.fm.potunes.PlayerActivity;
import poche.fm.potunes.R;
import poche.fm.potunes.domain.AppConstant;
import poche.fm.potunes.fragment.QuciControlsFragment;

/**
 * Created by purchas on 2017/1/14.
 */

@SuppressLint("NewApi")
public class PlayerService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer; //媒体播放器对象
    private String path; // 音乐文件路径
    private int msg;
    private boolean isPause; //暂停
    private int current = 0; //记录当前在播放的音乐
    private ArrayList<Track> tracks = new ArrayList<>();
    private int status = 0; //播放状态，默认顺序播放
    private MyReceiver myReceiver; //自定义广播接收器
    private int currentTime;
    private int duration;
    private Bitmap bitmap;
    private NotificationManager mManager;
    private RemoteViews contentViews;
    private AudioManager mAudioManager;

    // 服务要发送的一些Action
    public static final String UPDATE_ACTION = "fm.poche.action.UPDATE_ACTION";  //更新动作
    public static final String CTL_ACTION = "fm.poche.action.CTL_ACTION";        //控制动作
    public static final String MUSIC_CURRENT = "fm.poche.action.MUSIC_CURRENT";  //当前音乐播放时间更新动作
    public static final String MUSIC_DURATION = "fm.poche.action.MUSIC_DURATION";//新音乐长度更新动作
    public static final String TAG = "PlayerService";

    /**
     * handler用来接收消息，来发送广播更新播放时间
     */
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if(mediaPlayer != null) {
                    currentTime = mediaPlayer.getCurrentPosition(); // 获取当前音乐播放的位置
                    Intent intent = new Intent();
                    intent.setAction(MUSIC_CURRENT);
                    Track track = tracks.get(current);
                    intent.putExtra("url", track.getCover());
                    intent.putExtra("artist", track.getArtist());
                    intent.putExtra("title", track.getTitle());
                    intent.putExtra("isPlaying", mediaPlayer.isPlaying());
                    intent.putExtra("currentTime", currentTime);
                    sendBroadcast(intent); // 给PlayerActivity发送广播
                    handler.sendEmptyMessageDelayed(1, 1000);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        //设置音乐播放完成时的监听器
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
            if (status == 1) {
                mediaPlayer.start();
            } else if (status == 0) {
                current++;
                if (current > tracks.size() -1) {
                    current = 0;
                }
                sendIntent(current);
                path = tracks.get(current).getUrl();
                play(0);
            }  else if (status == 2) {
                current = getRandomIndex(tracks.size() - 1);
                sendIntent(current);
                path = tracks.get(current).getUrl();
                play(0);
            }
            }
        });

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);


        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerActivity.CTL_ACTION);
        filter.addAction(QuciControlsFragment.CTL_ACTION);
        registerReceiver(myReceiver, filter);
    }

    /**
     * 获取随机位置
     * @param end
     * @return
     */
    protected int getRandomIndex(int end) {
        int index = (int) (Math.random() * end);
        return index;
    }
    protected void sendIntent(int current) {
        Intent sendIntent = new Intent(UPDATE_ACTION);
        sendIntent.putExtra("current", current);
        sendIntent.putExtra("TRACKS", tracks);
        sendBroadcast(sendIntent);
    }
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        int shuffle = preferences.getInt("shuffle", 0);
        status = shuffle;
        current = preferences.getInt("position", 0);
        path = intent.getStringExtra("url"); //歌曲路径
        msg = intent.getIntExtra("MSG", 0);
        Log.d(TAG, "onStartCommand: " + msg);

        mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //获取本地Tracks数据
        String json = preferences.getString("Tracks", "Tracks");
        Gson gson = new Gson();
        tracks.clear();
        tracks = gson.fromJson(json, new TypeToken<List<Track>>(){}.getType());

        if (msg == AppConstant.PlayerMsg.PLAY_MSG) {
            play(0);
        } else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) {
            //暂停
            pause();
        } else if (msg == AppConstant.PlayerMsg.STOP_MSG) {     //停止
            if (isPause) {
                resume();
            } else {
                pause();
            }
        } else if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) { //继续播放
            resume();
        } else if (msg == AppConstant.PlayerMsg.PRIVIOUS_MSG) { //上一首
            previous();
        } else if (msg == AppConstant.PlayerMsg.NEXT_MSG) {
            //下一首
            next();
        } else if (msg == AppConstant.PlayerMsg.PROGRESS_CHANGE) {  //进度更新
            currentTime = intent.getIntExtra("progress", -1);
            play(currentTime);
        } else if (msg == AppConstant.PlayerMsg.PLAYING_MSG) {
            handler.sendEmptyMessage(1);
        }
        getNotification();
        return super.onStartCommand(intent,flags, startId);
    }

    /**
     * 播放音乐
     */
    private void play(int currentTime) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.setOnPreparedListener(new PreparedListener(currentTime));
            handler.sendEmptyMessage(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPause = true;
            mAudioManager.abandonAudioFocus(this);
        }
    }
    private void resume() {
        if (isPause) {
            mediaPlayer.start();
            isPause = false;
        }
    }
    private void previous() {
        if (status == 0) {
            if (current == 0) {
                current = tracks.size() - 1;
            } else {
                current = current - 1;
            }
        }
        sendIntent(current);
        path = tracks.get(current).getUrl();
        sendIntent(current);
        play(0);
    }
    private void next() {
       if (status == 0) {
            if (current == tracks.size() - 1) {
                current = 0;
            } else {
                current = current + 1;
            }
        } else if (status == 2) {
            current = (int) (Math.random() * (tracks.size() - 1));
        }
        sendIntent(current);
        path = tracks.get(current).getUrl();
        play(0);
    }
    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private final class PreparedListener implements MediaPlayer.OnPreparedListener {
        private int currentTime;
        private PreparedListener(int currentTime) {
            this.currentTime = currentTime;
        }
        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer.start(); // 开始播放
            if (currentTime > 0) { // 如果音乐不是从头播放
                mediaPlayer.seekTo(currentTime);
            }
            //通过Intent来传递歌曲的总长度
            Intent intent = new Intent();
            intent.setAction(MUSIC_DURATION);
            duration = mediaPlayer.getDuration();
            intent.putExtra("duration", duration);
            intent.putExtra("TRACKS", tracks);
            sendBroadcast(intent);
            // 存储长度
            duration = mediaPlayer.getDuration();
            SharedPreferences preferences=getSharedPreferences("user",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor=preferences.edit();
            editor.putInt("duration", duration);
            editor.putInt("position", current);
            editor.apply();
            isPause = false;
            getNotification();
        }
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
                        .setSmallIcon(R.drawable.ic_notification);
                Notification notification = mBuilder.build();
                mManager.notify(1, notification);
            }
        });
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int control = intent.getIntExtra("control", -1);
            switch (control) {
                case 1:
                    status = 0; // 列表循环
                    break;
                case 2:
                    status = 1; // 单曲循环
                    break;
                case 3:
                    status = 2; // 随机播放
                    break;
                default:
                    break;
            }
        }
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

