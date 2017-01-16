package poche.fm.potunes.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.PlayerActivity;
import poche.fm.potunes.R;
import poche.fm.potunes.domain.AppConstant;
import poche.fm.potunes.widgets.TintImageView;


public class QuciControlsFragment extends Fragment {
    public Activity mContext;
    private TintImageView mPlayPause;
    private TextView mTitle;
    private TextView mArtist;
    private SimpleDraweeView mAlbumArt;
    private View rootView;
    private ImageView playQueue, next;
    private static  QuciControlsFragment fragment;
    private ProgressBar mProgress;


    private OnFragmentInteractionListener mListener;
    private QuickReceiver quickReceiver;

    private int currentTime; // 当前歌曲播放时间
    private int position; // 播放歌曲在tracks的位置
    private int duration;
    private String url;
    private ArrayList<Track> tracks;
    private boolean isPause; // 暂停



    public static final String CTL_ACTION = "fm.poche.action.CTL_ACTION"; // 控制动作
    public static final String MUSIC_CURRENT = "fm.poche.action.MUSIC_CURRENT"; // 音乐当前时间改变动作
    public static final String MUSIC_DURATION = "fm.poche.action.MUSIC_DURATION";// 音乐播放长度改变动作
    public static final String UPDATE_ACTION = "fm.poche.action.UPDATE_ACTION"; // 更新动作
    public static final String TAG = "QuickControlsFragment"; // 更新动作


    public QuciControlsFragment() {
        // Required empty public constructor
    }


    public static QuciControlsFragment newInstance() {
        return new QuciControlsFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bottom_nav, container, false);
        this.rootView = rootView;
        mPlayPause = (TintImageView) rootView.findViewById(R.id.control);
        mProgress = (ProgressBar) rootView.findViewById(R.id.song_progress_normal);


        mTitle = (TextView) rootView.findViewById(R.id.playbar_info);
        mArtist = (TextView) rootView.findViewById(R.id.playbar_singer);
        mAlbumArt = (SimpleDraweeView) rootView.findViewById(R.id.playbar_img);
        next = (ImageView) rootView.findViewById(R.id.play_next);
        playQueue = (ImageView) rootView.findViewById(R.id.play_list);

        mPlayPause.setImageResource(R.drawable.playbar_btn_play);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("fm.poche.media.MUSIC_SERVICE");
                if (isPause == true) {
                    mPlayPause.setImageResource(R.drawable.playbar_btn_play);
                    intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG);
                    isPause = false;
                } else {
                    mPlayPause.setImageResource(R.drawable.playbar_btn_pause);
                    intent.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
                    isPause = true;
                }
                intent.putExtra("url", tracks.get(position).getUrl());
                intent.putExtra("position", position);
                intent.putExtra("TRACKS", tracks);
                intent.setPackage(getActivity().getPackageName());
                mContext.startService(intent);
            }
        });


        playQueue.setImageResource(R.drawable.playbar_btn_playlist);
        next.setImageResource(R.drawable.playbar_btn_next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next_music();
            }
        });

        rootView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent playerIntent = new Intent(mContext, PlayerActivity.class);

                playerIntent.putExtra(PlayerActivity.TRACKLIST, tracks);

                playerIntent.putExtra(PlayerActivity.TRACKID, position);

                mContext.startActivity(playerIntent);

            }
        });


        registeReceiver();

        return rootView;
    }

    public void next_music() {
        position = position + 1;
        if (position <= tracks.size() - 1) {
            Intent intent = new Intent();
            intent.setAction("fm.poche.media.MUSIC_SERVICE");
            intent.putExtra("url", tracks.get(position).getUrl());
            intent.putExtra("position", position);
            intent.putExtra("MSG", AppConstant.PlayerMsg.NEXT_MSG);
            intent.putExtra("TRACKS", tracks);
            intent.setPackage(((Activity) mContext).getPackageName());
            mContext.startService(intent);

        } else {
            position = tracks.size() - 1;
            Toast.makeText(mContext, "没有下一首了", Toast.LENGTH_SHORT)
                    .show();
        }
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

                currentTime = intent.getIntExtra("currentTime", -1);

                boolean isPlaying = intent.getBooleanExtra("isplaying", false);

                isPause = !isPlaying;

                if (isPause == true) {
                    mPlayPause.setImageResource(R.drawable.playbar_btn_play);

                } else {
                    mPlayPause.setImageResource(R.drawable.playbar_btn_pause);

                }
                duration = intent.getIntExtra("duration", -1);

                if (duration > 0) {
                    int progress = currentTime * 100 / duration;
                    mProgress.setProgress(progress);
                }

                position = intent.getIntExtra("position", -1);
                tracks =  (ArrayList<Track>) intent.getSerializableExtra("tracks");
                if (tracks != null) {
                    Track track = tracks.get(position);
                    String thumb = track.getCover();
                    Glide.with(mContext).load(thumb).into(mAlbumArt);
                    // 歌手名
                    mTitle.setText(track.getTitle());
                    mArtist.setText(track.getArtist());
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

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
