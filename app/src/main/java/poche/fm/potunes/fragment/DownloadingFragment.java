package poche.fm.potunes.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dinuscxj.progressbar.CircleProgressBar;
import com.lzy.okserver.download.DownloadInfo;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.listener.DownloadListener;
import com.malinskiy.materialicons.Iconify;
import com.malinskiy.materialicons.widget.IconButton;

import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.domain.AppConstant;

public class DownloadingFragment extends Fragment {
    private String TAG = "DownloadingFragment";
    private ListView listView;
    private IconButton mOperationView;
    private IconButton mDeleteView;
    private List<DownloadInfo> allTask = new ArrayList<>();
    private Context mContext;
    private View view;
    private MyAdapter adatper;
    private MainActivity main;



    private OnFragmentInteractionListener mListener;

    public DownloadingFragment() {
        // Required empty public constructor
    }


    public static DownloadingFragment newInstance() {
        DownloadingFragment fragment = new DownloadingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LitePal.initialize(getContext());
        mContext = getContext();
        main = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_downloading, container, false);

        mOperationView = (IconButton) view.findViewById(R.id.download_start);
        mDeleteView = (IconButton) view.findViewById(R.id.download_delete_all);
        mOperationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("fm.poche.media.DOWNLOAD_SERVICE");
                intent.setPackage(getActivity().getPackageName());
                String buttonText = mOperationView.getText().toString();
                if (buttonText.indexOf("开始") >= 0) {
                    mOperationView.setText("{zmdi-pause-circle-outline}  全部暂停");
                    intent.putExtra("MSG", AppConstant.DownloadMsg.RESUME);
                } else {
                    mOperationView.setText("{zmdi-download}  全部开始");
                    intent.putExtra("MSG", AppConstant.DownloadMsg.PAUSE);

                }
                getContext().startService(intent);
            }
        });
        mDeleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("fm.poche.media.DOWNLOAD_SERVICE");
                intent.setPackage(main.getPackageName());
                intent.putExtra("MSG", AppConstant.DownloadMsg.DELETE);
                getContext().startService(intent);

                main.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        allTask = DownloadManager.getInstance().getAllTask();
                        adatper.notifyDataSetChanged();
                    }
                });

            }
        });
        Iconify.addIcons(mOperationView);
        Iconify.addIcons(mDeleteView);
        listView = (ListView) view.findViewById(R.id.downloading_list_view);
        adatper = new MyAdapter();
        listView.setAdapter(adatper);
        allTask = DownloadManager.getInstance().getAllTask();
        adatper.notifyDataSetChanged();
        MainActivity main = (MainActivity) getActivity();
        main.setTitle("正在下载");

        return view;
    }
    

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
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
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
                convertView = View.inflate(mContext, R.layout.downloading_item, null);
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
            int state = downloadInfo.getState();
            if (state == DownloadManager.PAUSE || state == DownloadManager.NONE) {
                mOperationView.setText("{zmdi-download}  全部开始");
            } else {
                mOperationView.setText("{zmdi-pause-circle-outline}  全部暂停");
            }
            refresh();
        }

        //RunTime UI Refresh
        private void refresh() {
            progressBar.setMax(100);
            if (downloadInfo.getTotalLength() > 0) {
                progressBar.setProgress((int) (downloadInfo.getDownloadLength() * 100 / downloadInfo.getTotalLength()));
            }
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == progressBar.getId()) {
                reDownload(downloadInfo);
            }
        }
    }

    private void reDownload(DownloadInfo downloadInfo) {
        Intent intent = new Intent();
        intent.setAction("fm.poche.media.DOWNLOAD_SERVICE");
        intent.setPackage(main.getPackageName());
        intent.putExtra("MSG", AppConstant.DownloadMsg.RESTART);
        intent.putExtra("URL", downloadInfo.getUrl());
        getContext().startService(intent);
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
            Track track = (Track) downloadInfo.getData();
            final DownloadManager downloadManager = DownloadManager.getInstance();
            if (downloadManager.getDownloadInfo(track.getUrl()) != null) {
                downloadManager.removeTask(track.getUrl(), false);
            }
            // 重命名文件
            String downloadTitle = track.getArtist() + " - " + track.getTitle() + ".mp3";
            downloadTitle = downloadTitle.replace("/", " ");
            // 将数据库的已下载修改状态
            track.setIsDownloaded(1);
            track.setUrl(downloadManager.getTargetFolder() + downloadTitle);
            track.save();


            File old = new File(downloadManager.getTargetFolder(), downloadInfo.getFileName());
            File rename = new File(downloadManager.getTargetFolder(), downloadTitle);
            old.renameTo(rename);

            // 文件入媒体库
            String filename = downloadManager.getTargetFolder() + downloadTitle;
            Intent intent = new Intent();
            intent.setAction("fm.poche.media.DOWNLOAD_SERVICE");
            intent.setPackage(main.getPackageName());
            intent.putExtra("MSG", AppConstant.DownloadMsg.SCAN);
            intent.putExtra("SCAN", filename);
            main.startService(intent);

            main.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    allTask = downloadManager.getAllTask();
                    adatper.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onError(DownloadInfo downloadInfo, String errorMsg, Exception e) {
            Track track = (Track) downloadInfo.getData();
            if (errorMsg != null) Toast.makeText(getContext(), track.getTitle() + "下载失败，尝试重新下载", Toast.LENGTH_SHORT).show();
            reDownload(downloadInfo);
        }
    }
}
