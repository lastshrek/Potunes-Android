package poche.fm.potunes.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import poche.fm.potunes.Model.LocalAlbumMessageEvent;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;

/**
 * Created by purchas on 2017/2/5.
 */

public class DownloadAlbumAdapter extends RecyclerView.Adapter<DownloadAlbumAdapter.ViewHolder> {
    private ArrayList<Track> mAlbumList;
    private Context mContext;
    private String TAG = "DownloadAlbumItem";
    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView cover;
        TextView name;
        ImageView menu;

        public ViewHolder(View view) {
            super(view);
            cover = (ImageView) view.findViewById(R.id.download_album_cover);
            name = (TextView) view.findViewById(R.id.download_album_title);
            menu = (ImageView) view.findViewById(R.id.download_album_menu);
        }
    }

    public DownloadAlbumAdapter(ArrayList<Track> trackList) {
        mAlbumList = trackList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.download_album_item, parent, false);
        TypedValue typedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(R.attr.selectableItemBackground, typedValue, true);
        view.setBackgroundResource(typedValue.resourceId);
        final ViewHolder holder = new ViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                Track track = mAlbumList.get(position);
                EventBus.getDefault().post(new LocalAlbumMessageEvent(track.getAlbum()));
//                MainActivity activity = (MainActivity) mContext;
//                activity.navigateToBrowser("local_album");
//                Track track = mAlbumList.get(position);
//                Intent intent = new Intent(mContext, DownloadedTracksActivity.class);
//                intent.putExtra("album_title", track.getAlbum());
//                mContext.startActivity(intent);
            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();

            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(final DownloadAlbumAdapter.ViewHolder holder, int position) {
        Track track = mAlbumList.get(position);
        holder.name.setText(track.getAlbum());
        Log.d(TAG, "onBindViewHolder: " + track.getAlbum());
        String thumb = track.getCover() + "!/fw/150";
        Glide.with(mContext).load(thumb).into(holder.cover);
        holder.itemView.setClickable(true);
    }

    @Override
    public int getItemCount() {
        return mAlbumList.size();
    }
}
