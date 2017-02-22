package poche.fm.potunes.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.cocosw.bottomsheet.BottomSheet;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;
import com.sdsmdg.tastytoast.TastyToast;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.greenrobot.eventbus.EventBus;
import org.litepal.crud.DataSupport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.DownloadSingleMessage;
import poche.fm.potunes.Model.MediaScanner;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.fragment.MoreFragment;
import poche.fm.potunes.service.PlayerService;
import poche.fm.potunes.utils.AlbumArtCache;

/**
 * Created by purchas on 2017/1/8.
 */

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {
    private ArrayList<Track> mTrackList = new ArrayList<>();
    private Context mContext;
    private FragmentManager mFragmentManager;
    private String TAG = "TrackItem";
    private PlayerService mPlayerService;
    private BottomSheetDialog shareDialog;




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

    public TrackAdapter(List<Track> trackList, FragmentManager fragmentManager) {
        for (Track track: trackList) {
            mTrackList.add(track);
        }
        mFragmentManager = fragmentManager;
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
                    mPlayerService.play(holder.getAdapterPosition(), mContext);
                }
            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                final Track track = mTrackList.get(position);
                new BottomSheet.Builder(mContext, R.style.BottomSheet_Dialog).title("选择您想进行的操作").sheet(R.menu.online_track_share_menu)
                        .listener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case R.id.download:
                                        TastyToast.makeText(mContext, "开始下载", TastyToast.LENGTH_SHORT, TastyToast.SUCCESS);
                                        EventBus.getDefault().post(new DownloadSingleMessage(track));
                                        break;
                                    case R.id.share_to_wechat:
                                        shareDialog = new BottomSheetBuilder(mContext, R.style.AppTheme_BottomSheetDialog)
                                                .setMode(BottomSheetBuilder.MODE_GRID)
                                                .setMenu(R.menu.share_menu)
                                                .setItemClickListener(new BottomSheetItemClickListener() {
                                                    @Override
                                                    public void onBottomSheetItemClick(final MenuItem item) {
                                                        shareDialog.dismiss();
                                                        final MaterialDialog dialog = new MaterialDialog.Builder(mContext)
                                                                .title("请稍候")
                                                                .content("正在加载歌曲封面")
                                                                .progress(true, 0)
                                                                .show();

                                                        final IWXAPI api = WXAPIFactory.createWXAPI(mContext, "wx0fc8d0673ec86694", true);
                                                        api.registerApp("wx0fc8d0673ec86694");

                                                        if (!api.isWXAppInstalled()) {
                                                            Toast.makeText(mContext, "您没有安装微信", Toast.LENGTH_SHORT).show();
                                                            return;
                                                        }

                                                        WXMusicObject sharedTrack = new WXMusicObject();
                                                        sharedTrack.musicUrl = track.getUrl();

                                                        final WXMediaMessage msg = new WXMediaMessage();
                                                        msg.mediaObject = sharedTrack;
                                                        msg.title = track.getTitle();
                                                        msg.description = track.getArtist();



                                                        Glide.with(mContext).load(track.getCover()).asBitmap().into(new SimpleTarget<Bitmap>(100, 100) {
                                                            @Override
                                                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                                                dialog.dismiss();
                                                                msg.thumbData = bmpToByteArray(resource, false);
                                                                Log.d(TAG, "onResourceReady: " + msg.thumbData.toString());
                                                                //构造一个Req
                                                                final SendMessageToWX.Req req = new SendMessageToWX.Req();
                                                                req.transaction = String.valueOf(System.currentTimeMillis());
                                                                req.message = msg;
                                                                if (item.getTitle().equals("微信好友")) {
                                                                    req.scene = SendMessageToWX.Req.WXSceneSession;
                                                                } else if (item.getTitle().equals("微信朋友圈")) {
                                                                    req.scene = SendMessageToWX.Req.WXSceneTimeline;
                                                                }
                                                                api.sendReq(req);
                                                            }
                                                        });

//


                                                    }
                                                })
                                                .createDialog();
                                        shareDialog.setCanceledOnTouchOutside(true);
                                        shareDialog.show();

                                        break;
                                }
                            }
                        }).show();
            }
        });
        return holder;
    }
    // 转换微信朋友圈图片
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
        holder.cover.setImageResource(R.drawable.ic_launcher);
        AlbumArtCache.getInstance().fetch(thumb, new AlbumArtCache.FetchListener() {
            @Override
            public void onFetched(String artUrl, Bitmap bigImage, Bitmap iconImage) {
                holder.cover.setImageBitmap(iconImage);
            }
            @Override
            public void onError(String artUrl, Exception e) {
                Toast.makeText(mContext, "获取专辑封面图失败", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setClickable(true);
    }



    @Override
    public int getItemCount() {
        return mTrackList.size();
    }
}
