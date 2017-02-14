package poche.fm.potunes;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import poche.fm.potunes.Model.PlayState;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.service.LockScreenService;
import poche.fm.potunes.service.PlayerService;
import poche.fm.potunes.utils.Validator;

public class LockScreenActivity extends Activity {

    public static boolean isLocked = false;
    private String TAG = "LockScreenActivity:";
    public static final String MUSIC_CURRENT = "fm.poche.action.MUSIC_CURRENT"; // 音乐当前时间改变动作
    // 0:noshuffle, 1:single, 2:shuffle
    private int shuffle;

    private TextView mArtist;
    private TextView mTitle;
    private ImageView mPlayPause;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private ImageView mSkipPrev;
    private ImageView mSkipNext;
    private ImageView mRepeat;
    private Drawable mNoShuffle;
    private Drawable mShuffle;
    private Drawable mSingle;
    private ImageView mPlayingCover;
    private TextView mUnlock;
    private LockReceiver mLockReceiver;
    private PlayerService mPlayerService;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setContentView(R.layout.activity_lock_screen);
        findViewById();



        isLocked = true;

        try {
            startService(new Intent(this, LockScreenService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void findViewById() {
        mArtist = (TextView) findViewById(R.id.lock_artist);
        mTitle = (TextView) findViewById(R.id.lock_title);
        mPlayingCover = (ImageView) findViewById(R.id.lock_playing_cover);
        mRepeat = (ImageView) findViewById(R.id.lock_play_repeat);
        mNoShuffle = ContextCompat.getDrawable(this, R.drawable.play_icn_loop_prs);
        mShuffle = ContextCompat.getDrawable(this, R.drawable.play_icn_shuffle);
        mSingle = ContextCompat.getDrawable(this, R.drawable.play_icn_one_prs);
        mRepeat.setImageDrawable(mNoShuffle);
        mRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(shuffle == 0) {
                    PlayerService.mPlayState.setMode(1);
                    mRepeat.setImageDrawable(mShuffle);
                    shuffle = 1;
                } else if (shuffle == 1) {
                    PlayerService.mPlayState.setMode(2);
                    mRepeat.setImageDrawable(mSingle);
                    shuffle = 2;
                } else if (shuffle == 2) {
                    mRepeat.setImageDrawable(mNoShuffle);
                    PlayerService.mPlayState.setMode(0);
                    shuffle = 0;
                }
            }
        });
        mPlayPause = (ImageView) findViewById(R.id.lock_play_pause);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayerService.mPlayState.isPlaying()) {
                    mPlayerService.pause();
                    PlayerService.mPlayState.setPlaying(false);
                    mPlayPause.setImageDrawable(mPlayDrawable);
                } else {
                    mPlayerService.resume();
                    PlayerService.mPlayState.setPlaying(true);
                    mPlayPause.setImageDrawable(mPauseDrawable);
                }
            }
        });
        mPlayDrawable = ContextCompat.getDrawable(this, R.drawable.play_btn_play);
        mPauseDrawable = ContextCompat.getDrawable(this, R.drawable.play_btn_pause);
        mSkipNext = (ImageView) findViewById(R.id.lock_play_next);
        mSkipPrev = (ImageView) findViewById(R.id.lock_play_prev);
        mSkipPrev.setImageDrawable(getResources().getDrawable(R.drawable.play_btn_prev));
        mSkipNext.setImageDrawable(getResources().getDrawable(R.drawable.play_btn_next));
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
        mUnlock = (TextView) findViewById(R.id.unlock_text);
        mUnlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                isLocked = false;
                finish();
            }
        });
    }
    public class LockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MUSIC_CURRENT)) {
                Track track = PlayState.getCurrentMusic(PlayerService.tracks, PlayerService.mPlayState.getCurrentPosition());
                if (track == null) {
                    return;
                }
                //当前标题
                String title = mTitle.getText().toString().trim();
                String artist = mArtist.getText().toString().trim();
                if(!title.equals(track.getTitle().trim()) || !artist.equals(track.getArtist().trim())) {
                    //刷新文本信息
                    mTitle.setText(track.getTitle());
                    mArtist.setText(track.getArtist());

                    if (track.getAlbumid() == 0) {
                        // 封面
                        String thumb = track.getCover();
//                        Glide.with(getBaseContext()).load(thumb).into(mBackgroundImage);
                        Glide.with(getBaseContext()).load(thumb).into(mPlayingCover);

                    } else {
                        long albumid = track.getAlbumid();
                        long trackid = track.getID();
                        Bitmap bitmap = poche.fm.potunes.Model.MediaUtil.getArtwork(getBaseContext(), trackid, albumid, true, false);
//                        mBackgroundImage.setImageBitmap(bitmap);
                        mPlayingCover.setImageBitmap(bitmap);
                    }

                }


                boolean isMediaPlaying = PlayerService.mPlayState.isPlaying();
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

    @Override 
    public void onResume() {
        super.onResume();
        //定义和注册广播接收器
        //绑定服务
        bindService(new Intent(this, PlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        mLockReceiver = null;
        mLockReceiver = new LockReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MUSIC_CURRENT);
        registerReceiver(mLockReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLockReceiver != null) {
            unregisterReceiver(mLockReceiver);
            mLockReceiver = null;
        }
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_BACK) {
            // Key code constant: Home key. This key is handled by the framework and is never delivered to applications.
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onBackPressed() {
        //return;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }




}
