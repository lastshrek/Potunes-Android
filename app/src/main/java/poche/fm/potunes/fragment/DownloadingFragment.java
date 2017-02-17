package poche.fm.potunes.fragment;

import android.content.Context;
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
import com.lzy.okserver.task.ExecutorWithListener;
import com.malinskiy.materialicons.Iconify;
import com.malinskiy.materialicons.widget.IconButton;

import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;

public class DownloadingFragment extends Fragment {
    private String TAG = "DownloadingFragment";
    private ListView listView;
    private IconButton mOperationView;
    private IconButton mDeleteView;
    private DownloadManager downloadManager;
    private DownloadListener mDownloadListener;
    private List<DownloadInfo> allTask = new ArrayList<>();
    private Context mContext;
    private View view;
    private MyAdapter adatper;


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
        // 注册服务
        MainActivity main = (MainActivity) getActivity();
        downloadManager = main.getDownloadService().downloadManager;
        mDownloadListener = new DownloadListener() {
            @Override
            public void onProgress(DownloadInfo downloadInfo) {
                Log.d(TAG, "onProgress: " + downloadInfo.getProgress());
            }

            @Override
            public void onFinish(DownloadInfo downloadInfo) {

            }

            @Override
            public void onError(DownloadInfo downloadInfo, String errorMsg, Exception e) {

            }
        };
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

                String buttonText = mOperationView.getText().toString();
                if (buttonText.indexOf("开始") >= 0) {
                    mOperationView.setText("{zmdi-pause-circle-outline}  全部暂停");
                    downloadManager.startAllTask();
                } else {
                    mOperationView.setText("{zmdi-download}  全部开始");
                    downloadManager.stopAllTask();
                }
            }
        });
        mDeleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager.removeAllTask();
                adatper.notifyDataSetChanged();
            }
        });
        Iconify.addIcons(mOperationView);
        Iconify.addIcons(mDeleteView);
        listView = (ListView) view.findViewById(R.id.downloading_list_view);

        MainActivity main = (MainActivity) getActivity();
        main.setTitle("正在下载");

        for (DownloadInfo info: downloadManager.getAllTask()) {
            allTask.add(info);
        }

        adatper = new MyAdapter();
        listView.setAdapter(adatper);

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

            if (downloadInfo.getDownloadLength() > 0) {
                mOperationView.setText("" + "全部暂停");
            }

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
}
