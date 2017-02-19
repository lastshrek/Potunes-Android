package poche.fm.potunes.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.adapter.DownloadedTrackAdapter;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LocalDownloadAlbumFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LocalDownloadAlbumFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LocalDownloadAlbumFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    public String mAlbum;
    private String TAG = "LocalAlbumFragment";
    private DownloadedTrackAdapter adapter;

    private View view;

    public LocalDownloadAlbumFragment() {
        // Required empty public constructor
    }

    public static LocalDownloadAlbumFragment newInstance() {
        LocalDownloadAlbumFragment fragment = new LocalDownloadAlbumFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity main = (MainActivity) getActivity();
        mAlbum = main.mLocalAlbum;
        LitePal.initialize(getContext());
    }

    private void initTracks() {
        List<Track> list = DataSupport.where("album = ?", mAlbum).find(Track.class);
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track track: list) {
            if (track.getIsDownloaded() == 0) continue;
            Log.d(TAG, "initTracks: " + track.getIsDownloaded());
            tracks.add(track);
        }
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.local_tracks_recycler_view);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DownloadedTrackAdapter(tracks);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_local_download_album, container, false);
        initTracks();
        return view;
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
        void onFragmentInteraction(Uri uri);
    }
}
