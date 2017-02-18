package poche.fm.potunes.fragment;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.adapter.LocalTrackAdatper;


public class LocalTracksFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private String TAG = "LocalTracks";


    private View view;
    private LocalTrackAdatper adpter;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    public List<Track> tracks = new ArrayList<>();
    protected static final int TRACK = 1;

    private Handler sHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case TRACK:
                    adpter = new LocalTrackAdatper(tracks, getFragmentManager());
                    mRecyclerView.setAdapter(adpter);
                    adpter.notifyDataSetChanged();
                    swipeRefresh.setRefreshing(false);
                    break;
            }
        }
    };


    public LocalTracksFragment() {
        // Required empty public constructor
    }

    public static LocalTracksFragment newInstance() {
        return new LocalTracksFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_local_tracks, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.local_tracks_recycler_view);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setLayoutManager(layoutManager);
        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.track_swipe_refresh);
        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        swipeRefresh.setEnabled(false);
        swipeRefresh.setRefreshing(true);

        initTracks();
        return view;
    }

    public void initTracks() {
        tracks.clear();
        Track track;
        Cursor cursor = getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,null);
        if (cursor == null) return;
        while(cursor.moveToNext()){

            track = new Track();
            //扫描本地文件，得到歌曲的相关信息
            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)); //音乐id
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));  //时长
            long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));  //文件大小
            long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)); //albumid
            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            String mime_type = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
            if (path.indexOf(".mp3") > 0 || path.indexOf(".flac") > 0) {
                track.setName(title);
                track.setTrack_id((int)id);
                track.setArtist(artist);
                track.setUrl(path);
                track.setAlbum(album);
                track.setAlbumid(albumId);
                tracks.add(track);
            }
        }
        cursor.close();
        Message msg = Message.obtain();
        msg.what = TRACK;
        msg.obj = tracks;
        sHandler.sendMessage(msg);
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
