package poche.fm.potunes.fragment;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;

import poche.fm.potunes.Model.PlayerService;
import poche.fm.potunes.R;
import poche.fm.potunes.utils.ThemeUtils;
import poche.fm.potunes.widgets.TintImageView;
import poche.fm.potunes.widgets.TintProgressBar;


public class QuciControlsFragment extends Fragment {
    private TintProgressBar mProgress;
    public Runnable mUpdateProgress = new Runnable() {
        @Override
        public void run() {
//            long position = MusicPlayer.position();
//            mProgress.setMax((int) MusicPlayer.duration());
//            mProgress.setProgress((int) position);
//
//            if (MusicPlayer.isPlaying()) {
//                mProgress.postDelayed(mUpdateProgress, 50);
//            } else {
//                mProgress.removeCallbacks(this);
//            }
        }
    };
    public Activity mContext;

    private TintImageView mPlayPause;
    private TextView mTitle;
    private TextView mArtist;
    private SimpleDraweeView mAlbumArt;
    private View rootView;
    private ImageView playQueue, next;
    private LinearLayout layout;
    private boolean duetoplaypause = false;
    private static  QuciControlsFragment fragment;

    private OnFragmentInteractionListener mListener;

    public QuciControlsFragment() {
        // Required empty public constructor
    }


    public static QuciControlsFragment newInstance() {
        return new QuciControlsFragment();
    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//
//        }
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bottom_nav, container, false);
        this.rootView = rootView;
        mPlayPause = (TintImageView) rootView.findViewById(R.id.control);
        mProgress = (TintProgressBar) rootView.findViewById(R.id.song_progress_normal);
        mTitle = (TextView) rootView.findViewById(R.id.playbar_info);
        mArtist = (TextView) rootView.findViewById(R.id.playbar_singer);
        mAlbumArt = (SimpleDraweeView) rootView.findViewById(R.id.playbar_img);
        next = (ImageView) rootView.findViewById(R.id.play_next);
        playQueue = (ImageView) rootView.findViewById(R.id.play_list);

        mProgress.setMax(100);
        mProgress.setProgress(50);
        mProgress.setProgressTintList(ThemeUtils.getThemeColorStateList(mContext, R.color.colorAccent));
        mPlayPause.setImageResource(R.drawable.playbar_btn_play);

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mPlayPause.setImageResource(R.drawable.playbar_btn_play);
                mPlayPause.setImageTintList(R.color.theme_color_primary);

//                mPlayPause.setImageResource(MusicPlayer.isPlaying() ? R.drawable.playbar_btn_pause
//                        : R.drawable.playbar_btn_play);
//                mPlayPause.setImageTintList(R.color.theme_color_primary);
//
//                if (MusicPlayer.getQueueSize() == 0) {
//                    Toast.makeText(MainApplication.context, getResources().getString(R.string.queue_is_empty),
//                            Toast.LENGTH_SHORT).show();
//                } else {
//                    HandlerUtil.getInstance(MainApplication.context).postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            MusicPlayer.playOrPause();
//                        }
//                    }, 60);
//                }

//                if (MusicPlayer.getQueueSize() == 0) {
//
//                } else {
//
//                }

            }
        });


        playQueue.setImageResource(R.drawable.playbar_btn_playlist);
        next.setImageResource(R.drawable.playbar_btn_next);

        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
