package poche.fm.potunes.adapter;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.service.PlayerService;

/**
 * Created by purchas on 2017/2/5.
 */

public class DownloadedTrackAdapter extends RecyclerView.Adapter<DownloadedTrackAdapter.ViewHolder> {
    private ArrayList<Track> mTrackList;
    private Context mContext;
    private String TAG = "TrackItem";
    private PlayerService mPlayerService;


    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView artist;
        TextView name;
        ImageView menu;


        public ViewHolder(View view) {
            super(view);
            cover = (ImageView) view.findViewById(R.id.track_cover);
            artist = (TextView) view.findViewById(R.id.track_artist);
            name = (TextView) view.findViewById(R.id.track_title);
            menu = (ImageView) view.findViewById(R.id.track_item_menu);

        }
    }

    public DownloadedTrackAdapter(ArrayList<Track> trackList) {
        mTrackList = trackList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.track_item, parent, false);
        TypedValue typedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(R.attr.selectableItemBackground, typedValue, true);
        view.setBackgroundResource(typedValue.resourceId);
        final ViewHolder holder = new ViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity main = (MainActivity) mContext;
                mPlayerService = main.getPlayerService();
                if (mPlayerService != null && mTrackList != null) {
                    mPlayerService.tracks = mTrackList;
                    mPlayerService.play(holder.getAdapterPosition());
                }
            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = holder.getAdapterPosition();
                final BottomSheetDialog dialog = new BottomSheetBuilder(mContext, R.style.AppTheme_BottomSheetDialog)
                        .setMode(BottomSheetBuilder.MODE_LIST)
                        .setMenu(R.menu.local_share_menu)
                        .setItemClickListener(new BottomSheetItemClickListener() {
                            Track track = mTrackList.get(position);

                            @Override
                            public void onBottomSheetItemClick(MenuItem item) {

                                switch (item.getItemId()) {
                                    case 0:
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                                            if (Settings.System.canWrite(mContext)) {
                                                setRingtone(track);
                                            }
                                            else {
                                                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                mContext.startActivity(intent);
                                            }
                                        } else {

                                            setRingtone(track);
                                        }

                                        break;
                                    default:
                                        break;
                                }
                            }
                        })
                        .createDialog();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        });
        return holder;
    }

    private void setRingtone(Track track) {
//
        String ringtoneuri = Environment.getExternalStorageDirectory().getAbsolutePath() + "/media/ringtone";
        File file1 = new File(ringtoneuri);
        file1.mkdirs();
        File newSoundFile = new File(ringtoneuri, track.getTitle() + ".AMR");


        Uri mUri = Uri.parse("file://" + track.getUrl());
        Log.d(TAG, "setRingtone: " + mUri);

        ContentResolver mCr = mContext.getContentResolver();
        AssetFileDescriptor soundFile;
        try {
            soundFile = mCr.openAssetFileDescriptor(mUri, "r");
        } catch (FileNotFoundException e) {
            Log.d(TAG, "setRingtone: " + e);
            soundFile = null;
        }

        try {
            byte[] readData = new byte[1024];
            FileInputStream fis = soundFile.createInputStream();
            FileOutputStream fos = new FileOutputStream(newSoundFile);
            int i = fis.read(readData);

            while (i != -1) {
                fos.write(readData, 0, i);
                i = fis.read(readData);
            }

            fos.close();
        } catch (IOException io) {
            Log.d(TAG, "setRingtone: " + io);
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, newSoundFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.TITLE, track.getTitle());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
        values.put(MediaStore.MediaColumns.SIZE, newSoundFile.length());
        values.put(MediaStore.Audio.Media.ARTIST, R.string.app_name);
        values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        values.put(MediaStore.Audio.Media.IS_ALARM, false);
        values.put(MediaStore.Audio.Media.IS_MUSIC, false);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(newSoundFile.getAbsolutePath());
        Uri newUri = mCr.insert(uri, values);
        try {
            Uri rUri = RingtoneManager.getValidRingtoneUri(mContext);
            if (rUri != null) {
                Log.d(TAG, "setRingtone: " + rUri);
            }
            RingtoneManager.setActualDefaultRingtoneUri(mContext.getApplicationContext(), RingtoneManager.TYPE_RINGTONE, newUri);
            Toast.makeText(mContext, R.string.set_ringtone_success, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Log.e("sanjay in catch", "catch exception"+t.getMessage());
        }
    }

    private static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Track track = mTrackList.get(position);
        holder.artist.setText(track.getArtist());
        holder.name.setText(track.getTitle());
        String thumb = track.getCover() + "!/fw/150";
        Glide
                .with(mContext)
                .load(thumb)
                .asBitmap()
                .placeholder(R.drawable.ic_launcher)
                .into(new SimpleTarget<Bitmap>(100, 100) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        holder.cover.setImageBitmap(resource);
                    }
                });
        holder.itemView.setClickable(true);
    }

    @Override
    public int getItemCount() {
        return mTrackList.size();
    }
}
