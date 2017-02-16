package poche.fm.potunes.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dinuscxj.progressbar.CircleProgressBar;
import com.lzy.okserver.download.DownloadInfo;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.download.DownloadService;
import com.lzy.okserver.listener.DownloadListener;
import com.lzy.okserver.task.ExecutorWithListener;

import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;

/**
 * Created by purchas on 2017/1/21.
 */

public class DownloadingTrackAdapter extends RecyclerView.Adapter<DownloadingTrackAdapter.ViewHolder> implements ExecutorWithListener.OnAllTaskEndListener{

    private List<Track> mTrackList;
    private ArrayList<ViewHolder> mHolders = new ArrayList<>();
    private String TAG = "DownloadingTrackAdapter";
    private Context mContext;
    private List<DownloadInfo> allTask;
    private DownloadManager downloadManager;




    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView position;
        TextView title;
        CircleProgressBar progressBar;

        public ViewHolder(View view) {
            super(view);
            position = (TextView) view.findViewById(R.id.downloading_track_postion);
            title = (TextView) view.findViewById(R.id.downloading_track_title);
            progressBar = (CircleProgressBar) view.findViewById(R.id.download_progress);
        }

    }

    public DownloadingTrackAdapter(List<Track> trackList) {
        downloadManager = DownloadService.getDownloadManager();
        allTask = downloadManager.getAllTask();
        mTrackList = trackList;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.downloading_item, parent, false);
        ViewHolder holder = new ViewHolder(view);
        mHolders.add(holder);
        return holder;

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Track track = mTrackList.get(position);
        holder.position.setText("" + (position + 1));
        holder.title.setText(track.getTitle());

        DownloadListener downloadListener = new MyDownloadListener();
        downloadListener.setUserTag(holder);
        allTask.get(position).setListener(downloadListener);

    }

    @Override
    public int getItemCount() {
        return mTrackList.size();
    }

    public void refresh(int position, int progress) {
        Log.d(TAG, "refresh: " + position + "==" + progress);
        ViewHolder holder = mHolders.get(position);
        holder.progressBar.setProgress(progress);
    }

    private class MyDownloadListener extends DownloadListener {
        @Override
        public void onProgress(DownloadInfo downloadInfo) {
            if (getUserTag() == null) return;
            ViewHolder holder = (ViewHolder) getUserTag();
            refresh(downloadInfo.getId(), (int)(downloadInfo.getProgress() * 100)); //这里不能使用传递进来的 DownloadInfo，否者会出现条目错乱的问题
        }

        @Override
        public void onFinish(DownloadInfo downloadInfo) {
            Toast.makeText(mContext, "下载完成:" + downloadInfo.getTargetPath(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(DownloadInfo downloadInfo, String errorMsg, Exception e) {
            if (errorMsg != null) Toast.makeText(mContext, errorMsg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAllTaskEnd() {

    }

}


