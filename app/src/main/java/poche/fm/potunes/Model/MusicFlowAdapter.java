package poche.fm.potunes.Model;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.malinskiy.materialicons.Iconify;
import com.malinskiy.materialicons.widget.IconTextView;

import java.util.List;

import poche.fm.potunes.BaseActivity;
import poche.fm.potunes.DownloadingActivity;
import poche.fm.potunes.R;

/**
 * Created by wm on 2016/2/21.
 */
public class MusicFlowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private List<OverFlowItem> mList;
    private Context mContext;
    private Track musicInfo;

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
            Log.d(TAG, "setOnItemClickListener: 惦记");

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

                            break;
                        case 1:
                            Intent intent = new Intent();
                            intent.setClass(mContext, DownloadingActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(intent);
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
