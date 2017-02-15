package poche.fm.potunes.Model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.SyncStateContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.download.DownloadService;
import com.malinskiy.materialicons.Iconify;
import com.malinskiy.materialicons.widget.IconTextView;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import poche.fm.potunes.Manifest;
import poche.fm.potunes.R;

/**
 * Created by wm on 2016/2/21.
 */
public class MusicFlowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private List<OverFlowItem> mList;
    private Context mContext;
    private Track musicInfo;
    private DownloadManager downloadManager;

    private OnRecyclerViewItemClickListener mOnItemClickListener = null;
    private String TAG = "MusicFlowAdapter";


    public MusicFlowAdapter(Context context, List<OverFlowItem> list, Track info) {
        mList = list;
        mContext = context;
        musicInfo = info;
    }

    @Override
    public void onClick(View v) {
        if (mOnItemClickListener != null) {
            //注意这里使用getTag方法获取数据
            mOnItemClickListener.onItemClick(v, (String) v.getTag());
            Log.d(TAG, "onClick: 点击+++" + v.getTag());
        }
    }

    public void setOnItemClickListener(OnRecyclerViewItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_flow_layout, parent, false);
        ListItemViewHolder vh = new ListItemViewHolder(view);
        //将创建的View注册点击事件
        view.setOnClickListener(this);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        OverFlowItem item = mList.get(position);
        ((ListItemViewHolder) holder).icon.setText(item.getAvatar());
        ((ListItemViewHolder) holder).title.setText(item.getTitle());
        //设置tag
        ((ListItemViewHolder) holder).itemView.setTag(position + "");

        ((ListItemViewHolder) holder).itemView.setTag(position + "");
        // 设置下载页面
        if (musicInfo == null) {
            ((ListItemViewHolder) holder).itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (position) {
                        case 0:
                            int permissionCheck = ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.READ_EXTERNAL_STORAGE);

                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                final int REQUEST_EXTERNAL_STORAGE = 1;
                                ActivityCompat.requestPermissions(
                                        (Activity)mContext, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
                            } else {
                                EventBus.getDefault().post(new LocalTracksEvent("localtracks"));
                            }

                            break;
                        case 1:
                            downloadManager = DownloadService.getDownloadManager();
                            if (downloadManager.getAllTask().size() == 0) {
                                Toast.makeText(mContext, "当前并无下载任务", Toast.LENGTH_SHORT).show();
                                return;
                            }
//                            Intent intent = new Intent();
//                            intent.setClass(mContext, DownloadingActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            mContext.startActivity(intent);
                            break;

                    }
                }
            });
        }
    }



    @Override
    public int getItemCount() {
        return mList.size();
    }

    //定义接口
    public interface OnRecyclerViewItemClickListener {
        void onItemClick(View view, String data);
    }

    public class ListItemViewHolder extends RecyclerView.ViewHolder {
        IconTextView icon;
        TextView title;

        ListItemViewHolder(View view) {
            super(view);
            this.icon = (IconTextView) view.findViewById(R.id.pop_list_view);
            Iconify.addIcons(this.icon);
            this.title = (TextView) view.findViewById(R.id.pop_list_item);

        }


    }

}
