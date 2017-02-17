package poche.fm.potunes.fragment;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;
import com.lzy.okserver.download.DownloadManager;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.litepal.LitePal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.MusicFlowAdapter;
import poche.fm.potunes.Model.OverFlowItem;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.domain.AppConstant;
import poche.fm.potunes.handler.HandlerUtil;
import poche.fm.potunes.service.DownloadService;

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
    private DownloadManager downloadManager;
    private Bitmap thumb;


    private String TAG = "MoreFragment:";


    public MoreFragment() {
        // Required empty public constructor
    }

    public static MoreFragment newInstance(Track track, int startFrom, byte[] byteArray) {
        MoreFragment fragment = new MoreFragment();
        Bundle args = new Bundle();
        args.putSerializable("track", track);
        args.putInt("track_id", track.getID());
        args.putByteArray("thumb", byteArray);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomDatePickerDialog);
        mContext = getContext();
        LitePal.initialize(getContext());

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
        return view;

    }

    //设置分割线
    private void setItemDecoration() {
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(mContext, poche.fm.potunes.widgets.DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);
    }

    private void getTracks() {
        adapterMusicInfo = (Track) getArguments().getSerializable("track");
        int trackid = getArguments().getInt("track_id");
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
                        // 数据库存储
                        Intent startIntent = new Intent(mContext, DownloadService.class);
                        startIntent.putExtra("MSG", AppConstant.DownloadMsg.SINGLE);
                        startIntent.putExtra("track", adapterMusicInfo);
                        Log.d(TAG, "onItemClick: " + adapterMusicInfo.getAlbum());
                        mContext.startService(startIntent);
                        Toast.makeText(mContext, "开始下载", Toast.LENGTH_SHORT).show();
                        dismiss();
                        break;
                    case 1:
                        dismiss();
                        // shareToWechat
                        BottomSheetDialog dialog = new BottomSheetBuilder(mContext, R.style.AppTheme_BottomSheetDialog)
                                .setMode(BottomSheetBuilder.MODE_GRID)
                                .setMenu(R.menu.share_menu)
                                .setItemClickListener(new BottomSheetItemClickListener() {
                                    @Override
                                    public void onBottomSheetItemClick(MenuItem item) {

                                        IWXAPI api = WXAPIFactory.createWXAPI(mContext, "wx0fc8d0673ec86694", true);
                                        api.registerApp("wx0fc8d0673ec86694");

                                        if (!api.isWXAppInstalled()) {
                                            Toast.makeText(mContext, "您没有安装微信", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        WXMusicObject track = new WXMusicObject();
                                        track.musicUrl = adapterMusicInfo.getUrl();

                                        WXMediaMessage msg = new WXMediaMessage();
                                        msg.mediaObject = track;
                                        msg.title = adapterMusicInfo.getTitle();
                                        msg.description = adapterMusicInfo.getArtist();
                                        Log.d(TAG, "onBottomSheetItemClick: " + getArguments().getByteArray("thumb"));
                                        msg.thumbData = getArguments().getByteArray("thumb");

                                        //构造一个Req
                                        SendMessageToWX.Req req = new SendMessageToWX.Req();
                                        req.transaction = String.valueOf(System.currentTimeMillis());
                                        req.message = msg;


                                        if (item.getTitle().equals("微信好友")) {
                                            req.scene = SendMessageToWX.Req.WXSceneSession;
                                        } else if (item.getTitle().equals("微信朋友圈")) {
                                            req.scene = SendMessageToWX.Req.WXSceneTimeline;
                                        }
                                        api.sendReq(req);
                                    }
                                })
                                .createDialog();
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();
                        break;
                } 
            }
        });
    }


    public static Bitmap getBitmap(String url) {
        Bitmap bitmap = null;
        InputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new URL(url).openStream(), 1024);
            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            out = new BufferedOutputStream(dataStream, 1024);
            copy(in, out);
            out.flush();
            byte[] data = dataStream.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            data = null;
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] b = new byte[1024];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
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
