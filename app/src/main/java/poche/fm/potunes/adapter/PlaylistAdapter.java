package poche.fm.potunes.adapter;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.MessageEvent;
import poche.fm.potunes.Model.Playlist;
import poche.fm.potunes.R;
import poche.fm.potunes.fragment.PlaylistFragment;
import poche.fm.potunes.fragment.TrackListFragment;

/**
 * Created by purchas on 2017/1/7.
 */

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private Context mContext;
    private List<Playlist> mPlaylist;
    private FragmentManager mFragmentManager;
    private static final String FRAGMENT_TAG = "playlist_fragment";

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView cover;
        TextView title;

        public ViewHolder(View view) {
            super(view);
            cardView = (CardView) view;
            cover = (ImageView) view.findViewById(R.id.playlist_cover);
            title = (TextView) view.findViewById(R.id.playlist_title);
        }
    }

    public PlaylistAdapter(List<Playlist> playlist, FragmentManager fragmentManager) {
        mPlaylist = playlist;
        mFragmentManager = fragmentManager;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.playlist_item, parent, false);

        // 点击事件
        final ViewHolder holder = new ViewHolder(view);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                Playlist playlist = mPlaylist.get(position);
                EventBus.getDefault().post(new MessageEvent(playlist));
                MainActivity activity = (MainActivity) mContext;
                TrackListFragment mTrackListFragment = TrackListFragment.newInstance(playlist.getTitle(), playlist.getPlaylist_id());
                PlaylistFragment mPlaylistFragment = (PlaylistFragment) activity.getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
                activity.switchFragment(mPlaylistFragment , mTrackListFragment);
            }
        });



        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Playlist playlist = mPlaylist.get(position);
        holder.title.setText(playlist.getTitle());
        Glide.with(mContext).load(playlist.getCover()).into(holder.cover);
    }

    @Override
    public int getItemCount() {
        return mPlaylist.size();
    }

}
