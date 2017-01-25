package poche.fm.potunes;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;




import android.widget.ListView;

import android.widget.TextView;
import android.widget.Toast;


import com.dinuscxj.progressbar.CircleProgressBar;

import com.lzy.okserver.download.DownloadInfo;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.download.DownloadService;
import com.lzy.okserver.listener.DownloadListener;
import com.lzy.okserver.task.ExecutorWithListener;
import com.malinskiy.materialicons.Iconify;
import com.malinskiy.materialicons.widget.IconButton;


import org.litepal.LitePal;


import java.io.File;
import java.util.ArrayList;
import java.util.List;


import poche.fm.potunes.Model.Track;

public class DownloadingActivity extends BaseActivity implements View.OnClickListener, ExecutorWithListener.OnAllTaskEndListener{

    private String TAG = "DownloadingActivity";
    private ListView listView;
    private MyAdapter adatper;
    private IconButton mOperationView;
    private DownloadManager downloadManager;
    private List<DownloadInfo> allTask = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseSetContentView(R.layout.activity_downloading);
        baseInit();
        getBaseActionBar().setTitle(R.string.downloading);
        LitePal.initialize(this);


        mOperationView = (IconButton) findViewById(R.id.download_start);
        Iconify.addIcons(mOperationView);
        listView = (ListView) findViewById(R.id.downloading_list_view);
        downloadManager = DownloadService.getDownloadManager();

        
        adatper = new MyAdapter();
        listView.setAdapter(adatper);
        

        for (DownloadInfo info: downloadManager.getAllTask()) {
            allTask.add(info);
        }


        downloadManager.getThreadPool().getExecutor().addOnAllTaskEndListener(this);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //记得移除，否者会回调多次
        downloadManager.getThreadPool().getExecutor().removeOnAllTaskEndListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adatper.notifyDataSetChanged();
    }

    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.removeAll:
//                downloadManager.removeAllTask();
//                adapter.notifyDataSetChanged();  //移除的时候需要调用
//                break;
//            case R.id.pauseAll:
//                downloadManager.pauseAllTask();
//                break;
//            case R.id.stopAll:
//                downloadManager.stopAllTask();
//                break;
//            case R.id.startAll:
//                downloadManager.startAllTask();
//                break;
        }
    }


    @Override
    public void onAllTaskEnd() {
        for (DownloadInfo downloadInfo : allTask) {
            if (downloadInfo.getState() != DownloadManager.FINISH) {
                Toast.makeText(DownloadingActivity.this, "所有下载线程结束，部分下载未完成", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(DownloadingActivity.this, "所有下载任务完成", Toast.LENGTH_SHORT).show();
        adatper.notifyDataSetChanged();
    }


    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return allTask.size();
        }

        @Override
        public DownloadInfo getItem(int position) {
            return allTask.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            DownloadInfo downloadInfo = getItem(position);
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(DownloadingActivity.this, R.layout.downloading_item, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
                holder.progressBar.setMax(100);
                holder.progressBar.setProgress(0);
            }
            holder.refresh(downloadInfo);

            //对于非进度更新的ui放在这里，对于实时更新的进度ui，放在holder中
            Track track = (Track) downloadInfo.getData();
            holder.position.setText("" + (position + 1));
            holder.title.setText(track.getTitle());
            holder.progressBar.setOnClickListener(holder);


            DownloadListener downloadListener = new MyDownloadListener();
            downloadListener.setUserTag(holder);
            downloadInfo.setListener(downloadListener);
            return convertView;
        }

    }

    private class ViewHolder implements View.OnClickListener {
        private DownloadInfo downloadInfo;
        TextView position;
        TextView title;
        CircleProgressBar progressBar;


        public ViewHolder(View convertView) {
            position = (TextView) convertView.findViewById(R.id.downloading_track_postion);
            title = (TextView) convertView.findViewById(R.id.downloading_track_title);
            progressBar = (CircleProgressBar) convertView.findViewById(R.id.download_progress);
        }

        public void refresh(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
            refresh();
        }

        //RunTime UI Refresh
        private void refresh() {
            progressBar.setMax(100);

            if (downloadInfo.getTotalLength() > 0) {
                progressBar.setProgress((int) (downloadInfo.getDownloadLength() * 100 / downloadInfo.getTotalLength()));
            }

            adatper.notifyDataSetChanged();

        }

        @Override
        public void onClick(View v) {
            if (v.getId() == progressBar.getId()) {
                Log.d(TAG, "onClick: 点击了进度条" + v.getId());
                Track track = (Track) downloadInfo.getData();
                downloadManager.restartTask(track.getUrl());

//                switch (downloadInfo.getState()) {
//                    case DownloadManager.PAUSE:
//                    case DownloadManager.NONE:
//                    case DownloadManager.ERROR:
//                        downloadManager.addTask(downloadInfo.getUrl(), downloadInfo.getRequest(), downloadInfo.getListener());
//                        break;
//                    case DownloadManager.DOWNLOADING:
//                        downloadManager.pauseTask(downloadInfo.getUrl());
//                        break;
//                    case DownloadManager.FINISH:
//
//                        break;
//                }
//                refresh();
            }
        }
    }

    private class MyDownloadListener extends DownloadListener {

        @Override
        public void onProgress(DownloadInfo downloadInfo) {
            if (getUserTag() == null) return;
            ViewHolder holder = (ViewHolder) getUserTag();
            holder.refresh();  //这里不能使用传递进来的 DownloadInfo，否者会出现条目错乱的问题
        }

        @Override
        public void onFinish(DownloadInfo downloadInfo) {
            Track adapterMusicInfo = (Track) downloadInfo.getData();

            // 重命名文件
            String downloadTitle = adapterMusicInfo.getArtist() + " - " + adapterMusicInfo.getTitle() + ".mp3";
            downloadTitle = downloadTitle.replace("/", " ");
            File old = new File(downloadManager.getTargetFolder(), downloadInfo.getFileName());
            File rename = new File(downloadManager.getTargetFolder(), downloadTitle);
            old.renameTo(rename);
            // 数据库保存
            adapterMusicInfo.setIsDownloaded(1);
            adapterMusicInfo.save();

            Toast.makeText(DownloadingActivity.this,  "" + downloadTitle, Toast.LENGTH_SHORT).show();

            //移除任务保留本地文件
            if (downloadManager.getDownloadInfo(adapterMusicInfo.getUrl()) != null) {
                downloadManager.removeTask(adapterMusicInfo.getUrl(), false);
            }

            if(allTask.size() == 1) {
                allTask.clear();
                adatper.notifyDataSetChanged();
            }
        }

        @Override
        public void onError(DownloadInfo downloadInfo, String errorMsg, Exception e) {
            if (errorMsg != null) Toast.makeText(DownloadingActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    }
}
