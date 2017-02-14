package poche.fm.potunes.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.MediaUtil;
import poche.fm.potunes.Model.PlayState;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.PlayerActivity;
import poche.fm.potunes.R;
import poche.fm.potunes.service.PlayerService;
import poche.fm.potunes.utils.AlbumArtCache;
import poche.fm.potunes.widgets.TintImageView;


public class QuickControlsFragment extends Fragment {

    private static final String TAG = "QuickControls";
    public static final String CTL_ACTION = "fm.poche.action.CTL_ACTION"; // 控制动作
    public static final String MUSIC_CURRENT = "fm.poche.action.MUSIC_CURRENT"; // 音乐当前时间改变动作
    public static final String MUSIC_DURATION = "fm.poche.action.MUSIC_DURATION";// 音乐播放长度改变动作
    public static final String UPDATE_ACTION = "fm.poche.action.UPDATE_ACTION"; // 更新动作

    public Activity mContext;
    private OnFragmentInteractionListener mListener;

    private TintImageView mPlayPause;
    private TextView mTitle;
    private TextView mArtist;
    private SimpleDraweeView mAlbumArt;
    private String mArtURL;
    private ImageView playQueue, next;
    private ProgressBar mProgress;


    private QuickReceiver quickReceiver;
    private ArrayList<Track> tracks = new ArrayList<Track>();
    private boolean isPlaying; // 暂停
    private PlayerService mPlayerService;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            QuickControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            QuickControlsFragment.this.onMetadataChanged(metadata);
        }
    };

    public QuickControlsFragment() {
        // Required empty public constructor
    }


    public static QuickControlsFragment newInstance() {
        return new QuickControlsFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.bottom_nav, container, false);


        mPlayPause = (TintImageView) rootView.findViewById(R.id.control);
        mProgress = (ProgressBar) rootView.findViewById(R.id.song_progress_normal);


        mTitle = (TextView) rootView.findViewById(R.id.playbar_info);
        mArtist = (TextView) rootView.findViewById(R.id.playbar_singer);
        mAlbumArt = (SimpleDraweeView) rootView.findViewById(R.id.playbar_img);
        next = (ImageView) rootView.findViewById(R.id.play_next);
        playQueue = (ImageView) rootView.findViewById(R.id.play_list);
        playQueue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PlayQueueFragment playQueueFragment = new PlayQueueFragment();
                        playQueueFragment.show(getFragmentManager(), "playqueueframent");
                    }
                }, 60);
            }
        });

        mPlayPause.setImageResource(R.drawable.playbar_btn_play);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayerService.mPlayState.isPlaying()) {
                    mPlayerService.pause();
                    PlayerService.mPlayState.setPlaying(false);
                    mPlayPause.setImageResource(R.drawable.playbar_btn_play);
                } else {
                    mPlayerService.resume();
                    PlayerService.mPlayState.setPlaying(true);
                    mPlayPause.setImageResource(R.drawable.playbar_btn_pause);
                }
            }
        });


        playQueue.setImageResource(R.drawable.playbar_btn_playlist);
        next.setImageResource(R.drawable.playbar_btn_next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerService != null) {
                    mPlayerService.next();
                }
            }
        });
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent playerIntent = new Intent(mContext, PlayerActivity.class);
                playerIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(playerIntent);
            }
        });
        
        mProgress.setMax(100);
        registeReceiver();
        return rootView;
    }

    private void registeReceiver() {
        //定义和注册广播接收器
        quickReceiver = new QuickReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_ACTION);
        filter.addAction(MUSIC_CURRENT);
        filter.addAction(MUSIC_DURATION);
        getActivity().registerReceiver(quickReceiver, filter);
    }

    public class QuickReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MUSIC_CURRENT)) {
                // 注册服务
                MainActivity main = (MainActivity) getActivity();
                mPlayerService = main.getPlayerService();

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
                        Glide.with(mContext).load(track.getCover()).into(mAlbumArt);
                    } else {
                        long albumid = track.getAlbumid();
                        long trackid = track.getID();
                        Bitmap bitmap = MediaUtil.getArtwork(mContext, trackid, albumid, true, true);
                        mAlbumArt.setImageBitmap(bitmap);
                    }
                }

                isPlaying = PlayerService.mPlayState.isPlaying();
                if (!isPlaying) {
                    mPlayPause.setImageResource(R.drawable.playbar_btn_play);
                } else {
                    mPlayPause.setImageResource(R.drawable.playbar_btn_pause);
                    //刷新播放进度
                    long position = PlayerService.mPlayState.getProgress();
                    long duration = PlayerService.mPlayState.getDuration();
                    if (duration > 0) {
                        int progress = (int)(position * 100 / duration);
                        mProgress.setProgress(progress);
                    }
                }
            }
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
            mContext = (Activity) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().unregisterReceiver(quickReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaControllerCompat controller = getActivity().getSupportMediaController();
        if (controller != null) {
            controller.unregisterCallback(mCallback);
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void onConnected() {
        MediaControllerCompat controller = getActivity().getSupportMediaController();
        if (controller != null) {
            onMetadataChanged(controller.getMetadata());
            onPlaybackStateChanged(controller.getPlaybackState());
            controller.registerCallback(mCallback);
        }
    }

    private void onPlaybackStateChanged(PlaybackStateCompat state) {
        if (getActivity() == null) {
            return;
        }
        if (state == null) {
            return;
        }
        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }

        if (enablePlay) {
            mPlayPause.setImageResource(R.drawable.playbar_btn_pause);
        } else {
            mPlayPause.setImageResource(R.drawable.playbar_btn_pause);
        }
    }

    private void onMetadataChanged(MediaMetadataCompat metadata) {
        if (getActivity() == null) {
            return;
        }
        if (metadata == null) {
            return;
        }

        mTitle.setText(metadata.getDescription().getTitle());
        mArtist.setText(metadata.getDescription().getSubtitle());
        String artUrl = null;
        if (metadata.getDescription().getIconUri() != null) {
            artUrl = metadata.getDescription().getIconUri().toString();
        }
        if (!TextUtils.equals(artUrl, mArtURL)) {
            mArtURL = artUrl;
            Bitmap art = metadata.getDescription().getIconBitmap();
            AlbumArtCache cache = AlbumArtCache.getInstance();
            if (art == null) {
                art = cache.getIconImage(mArtURL);
            }
            if (art != null) {
                mAlbumArt.setImageBitmap(art);
            } else {
                cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                            @Override
                            public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                                if (icon != null) {
                                    if (isAdded()) {
                                        mAlbumArt.setImageBitmap(icon);
                                    }
                                }
                            }
                        }
                );
            }
        }
    }
}
