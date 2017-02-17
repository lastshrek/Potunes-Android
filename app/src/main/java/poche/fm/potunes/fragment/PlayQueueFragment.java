package poche.fm.potunes.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.handler.HandlerUtil;
import poche.fm.potunes.service.PlayerService;


/**
 * Created by wm on 2016/2/4.
 */
public class PlayQueueFragment extends AttachDialogFragment {
    private PlaylistAdapter adapter;
    private ArrayList<Track> playlist = new ArrayList<>();
    private TextView playlistNumber;
    private Track musicInfo;
    private int currentlyPlayingPosition = 0;
    private RecyclerView recyclerView;  //弹出的activity列表
    private LinearLayoutManager layoutManager;
    private int current;
    private Handler mHandler;
    private String TAG = "PlayQueueFragment";
    private PlayerService mPlayerService;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置样式
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomDatePickerDialog);
        mHandler = HandlerUtil.getInstance(mContext);
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //设置无标题
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置从底部弹出
        WindowManager.LayoutParams params = getDialog().getWindow()
                .getAttributes();
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setAttributes(params);


        View view = inflater.inflate(R.layout.fragment_queue, container);

        //布局
        playlistNumber = (TextView) view.findViewById(R.id.play_list_number);
        recyclerView = (RecyclerView) view.findViewById(R.id.play_list);
        layoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        current = PlayerService.mPlayState.getCurrentPosition();

        getActivity().bindService(new Intent(getContext(), PlayerService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                PlayerService.PlayBinder pb = (PlayerService.PlayBinder)service;
                mPlayerService = pb.getPlayerService();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected: ");
            }

        },Context.BIND_AUTO_CREATE);

        new loadSongs().execute();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        //设置fragment高度 、宽度
        int dialogHeight = (int) (mContext.getResources().getDisplayMetrics().heightPixels * 0.6);
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, dialogHeight);
        getDialog().setCanceledOnTouchOutside(true);
    }



    //异步加载recyclerview界面
    private class loadSongs extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (mContext != null) {
                playlist = PlayerService.tracks;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (playlist != null && playlist.size() > 0) {
                adapter = new PlaylistAdapter(playlist);
                recyclerView.setAdapter(adapter);
                playlistNumber.setText("播放列表（" + playlist.size() + "）");
                recyclerView.scrollToPosition(current);

            }
        }
    }

    class PlaylistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ArrayList<Track> playlist = new ArrayList<>();
        public PlaylistAdapter(ArrayList<Track> list) {
            playlist = list;
        }
        public void updateDataSet(ArrayList<Track> list) {
            this.playlist = list;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            return new ItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.fragment_playqueue_item, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            musicInfo = playlist.get(position);
            ((ItemViewHolder) holder).title.setText(playlist.get(position).getTitle());
            ((ItemViewHolder) holder).artist.setText(" - " + playlist.get(position).getArtist());
            //判断该条目音乐是否在播放
            if (current == position) {
                ((ItemViewHolder) holder).playstate.setVisibility(View.VISIBLE);
                Drawable icon = getResources().getDrawable(R.drawable.song_play_icon);
                Drawable tintIcon = DrawableCompat.wrap(icon);
                DrawableCompat.setTintList(tintIcon, getResources().getColorStateList(R.color.colorAccent));
                ((ItemViewHolder) holder).playstate.setImageDrawable(tintIcon);
                currentlyPlayingPosition = position;
            } else {
                ((ItemViewHolder) holder).playstate.setVisibility(View.GONE);
                ((ItemViewHolder) holder).title.setTextColor(getResources().getColor(R.color.black));
            }
        }

        @Override
        public int getItemCount() {
            return playlist == null ? 0 : playlist.size();
        }


        class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView title, artist;
            ImageView playstate;

            public ItemViewHolder(View itemView) {
                super(itemView);
                this.playstate = (ImageView) itemView.findViewById(R.id.play_state);
                this.title = (TextView) itemView.findViewById(R.id.play_list_musicname);
                this.artist = (TextView) itemView.findViewById(R.id.play_list_artist);
                itemView.setOnClickListener(this);

            }

            @Override
            public void onClick(View v) {

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final int position = getAdapterPosition();
                        if (position == -1) {
                            return;
                        }

                        mPlayerService.play(position);
                        dismiss();
                    }
                }, 70);

            }
        }

    }


}
