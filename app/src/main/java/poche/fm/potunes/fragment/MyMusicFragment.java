package poche.fm.potunes.fragment;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.malinskiy.materialicons.widget.IconTextView;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;

import poche.fm.potunes.Model.DownloadAlbumAdapter;
import poche.fm.potunes.Model.MusicFlowAdapter;
import poche.fm.potunes.Model.OverFlowItem;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;

public class MyMusicFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private static final String ARG_MEDIA_ID = "media_id";

    private View view;
    private ArrayList<OverFlowItem> mStatics = new ArrayList<>();
    private MusicFlowAdapter adapter;
    private DownloadAlbumAdapter albumAdapter;
    private IconTextView mDownloadHeader;
    private ArrayList<Track> tracks = new ArrayList<>();


    public MyMusicFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static MyMusicFragment newInstance() {
        return new MyMusicFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
        mDownloadHeader = (IconTextView) view.findViewById(R.id.download_album_header);
        setMusicInfo();

        //获取已下载专辑
        setDownloadedAlbum();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_my_music, container, false);
        mDownloadHeader = (IconTextView) view.findViewById(R.id.download_album_header);

        return view;
    }
    //设置音乐overflow条目
    private void setMusicInfo() {
        setInfo("本地音乐", "{zmdi-collection-music}");
        setInfo("下载管理", "{zmdi-square-down}");
    }
    //为info设置数据，并放入mlistInfo
    public void setInfo(String title, String zString) {
        OverFlowItem information = new OverFlowItem();
        information.setTitle(title);
        information.setAvatar(zString);
        mStatics.add(information); //将新的info对象加入到信息列表中

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.tracklist_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MusicFlowAdapter(getContext(), mStatics, null);
        recyclerView.setAdapter(adapter);
    }

    private void setDownloadedAlbum() {
        Cursor cursor = DataSupport.findBySQL("select distinct album from TRACK");
        ArrayList<String> titleArray = new ArrayList<>();
        if (cursor.moveToFirst() && cursor != null) {
            do {
                titleArray.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        for (String title: titleArray) {
            Track track = DataSupport.where("album = ?", title).limit(1).find(Track.class).get(0);
            tracks.add(track);
        }
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.download_album_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        albumAdapter = new DownloadAlbumAdapter(tracks);
        recyclerView.setAdapter(albumAdapter);
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
    public String getMediaId() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(ARG_MEDIA_ID);
        }
        return null;
    }
    public void setMediaId(String mediaId) {
        Bundle args = new Bundle(1);
        args.putString(ARG_MEDIA_ID, mediaId);
        setArguments(args);
    }
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
