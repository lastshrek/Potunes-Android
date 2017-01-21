package poche.fm.potunes.fragment;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;

import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Response;
import poche.fm.potunes.Model.MusicFlowAdapter;
import poche.fm.potunes.Model.OverFlowItem;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.handler.HandlerUtil;

public class MoreFragment extends DialogFragment {


    private OnFragmentInteractionListener mListener;

    private int type;
    private double heightPercent;
    private TextView topTitle;
    private ArrayList<OverFlowItem> mTrackInfo = new ArrayList<>();
    //弹出的activity列表
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private String args;
    private String title, artist, cover, url, album;
    private int trackID;
    private Context mContext;
    private Handler mHandler;
    private long playlistId = -1;
    private MusicFlowAdapter musicFlowAdapter;
    private Track adapterMusicInfo;
    private SQLiteDatabase db;


    private String TAG = "MoreFragment:";


    public MoreFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static MoreFragment newInstance(Track track, int startFrom) {
        MoreFragment fragment = new MoreFragment();
        Bundle args = new Bundle();
        args.putParcelable("track", track);
        args.putInt("track_id", track.getID());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomDatePickerDialog);
        mContext = getContext();
        LitePal.initialize(getContext());
        db = LitePal.getDatabase();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mHandler = HandlerUtil.getInstance(mContext);
        //设置无标题
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置从底部弹出
        WindowManager.LayoutParams params = getDialog().getWindow()
                .getAttributes();
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setAttributes(params);

        //布局
        View view = inflater.inflate(R.layout.fragment_more, container);
        topTitle = (TextView) view.findViewById(R.id.pop_list_title);
        recyclerView = (RecyclerView) view.findViewById(R.id.pop_list);
        layoutManager = new LinearLayoutManager(mContext);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        getTracks();
        setClick();
//        setItemDecoration();
        return view;

    }

    //设置分割线
    private void setItemDecoration() {
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(mContext, poche.fm.potunes.widgets.DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);
    }

    private void getTracks() {
        adapterMusicInfo = getArguments().getParcelable("track");
//        int trackid = getArguments().getInt("track_id");
        if (adapterMusicInfo == null) {
            adapterMusicInfo = new Track();
        }
        artist = adapterMusicInfo.getArtist();
        title = adapterMusicInfo.getTitle();
        trackID = adapterMusicInfo.getID();
        cover = adapterMusicInfo.getCover();
        url = adapterMusicInfo.getUrl();
        topTitle.setText("选择您想进行的操作");
        heightPercent = 0.3;
        setMusicInfo();
        musicFlowAdapter = new MusicFlowAdapter(mContext, mTrackInfo, adapterMusicInfo);
        recyclerView.setAdapter(musicFlowAdapter);
    }

    //设置音乐overflow条目
    private void setMusicInfo() {
        //设置mlistInfo，listview要显示的内容
        setInfo("下载", "{zmdi-download}");
        setInfo("分享", "{zmdi-share}");
        setInfo("设为铃声","{zmdi-hearing}");
    }

    //为info设置数据，并放入mlistInfo
    public void setInfo(String title, String zString) {
        OverFlowItem information = new OverFlowItem();
        information.setTitle(title);
        information.setAvatar(zString);
        mTrackInfo.add(information); //将新的info对象加入到信息列表中
    }

    public void setClick() {
        musicFlowAdapter.setOnItemClickListener(new MusicFlowAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, String data) {
                switch (Integer.parseInt(data)) {
                    case 0:
                        Log.d(TAG, "onItemClick: 下载" + adapterMusicInfo.getID());
                        adapterMusicInfo.save();

                        String downloadTrackName = adapterMusicInfo.getArtist() + " - " + adapterMusicInfo.getTitle() + ".mp3";
                        downloadTrackName = downloadTrackName.replace("/", " ");
                        OkGo.get(adapterMusicInfo.getUrl())
                                .execute(new FileCallback(downloadTrackName) {
                                    @Override
                                    public void onSuccess(File file, Call call, Response response) {
                                        adapterMusicInfo.setIsDownloaded(1);
                                        adapterMusicInfo.save();
                                    }

                                    @Override
                                    public void downloadProgress(long currentSize, long totalSize, float progress, long networkSpeed) {
                                        //这里回调下载进度(该回调在主线程,可以直接更新ui)
                                        Log.d(TAG, "downloadProgress: 下载进度" + progress);
                                    }
                                });
                        Toast.makeText(mContext,  adapterMusicInfo.getTitle() + "已添加至下载队列", Toast.LENGTH_SHORT).show();
                        dismiss();


                        break;
                    case 1:
                        Log.d(TAG, "onItemClick: 分享");
                        dismiss();
                        break;
                    case 2:
                        dismiss();
                        Log.d(TAG, "onItemClick: 设为铃声");
                } 
            }
        });
    }

    // TODO: Rename method, update argument and hook method into UI event
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
            mContext = (Activity) context;
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
    public void onStart() {
        super.onStart();
        //设置fragment高度 、宽度
        int dialogHeight = (int) (mContext.getResources().getDisplayMetrics().heightPixels * heightPercent);
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, dialogHeight);
        getDialog().setCanceledOnTouchOutside(true);

    }



    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
