package poche.fm.potunes.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lzy.okserver.download.DownloadManager;
import com.lzy.okserver.download.DownloadService;
import com.malinskiy.materialicons.widget.IconButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.MainActivity;
import poche.fm.potunes.Model.MessageEvent;
import poche.fm.potunes.Model.Playlist;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.R;
import poche.fm.potunes.domain.AppConstant;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class TrackListFragment extends Fragment {

    private OnListFragmentInteractionListener mListener;
    private static final String ARG_MEDIA_ID = "media_id";
    private String TAG = "TrackListFragment";
    public Playlist playlist;
    public List<Track> tracks = new ArrayList<>();
    protected static final int TRACK = 1;

    private View view;
    private TrackAdapter adpter;
    private RecyclerView mRecyclerView;
    private IconButton downloadAll;
    private DownloadManager downloadManager;
    private SwipeRefreshLayout swipeRefresh;

    private Handler sHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case TRACK:
                    adpter = new TrackAdapter(tracks, getFragmentManager());
                    mRecyclerView.setAdapter(adpter);
                    adpter.notifyDataSetChanged();
                    swipeRefresh.setRefreshing(false);
                    break;
            }
        }
    };
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TrackListFragment() {
    }

    public static TrackListFragment newInstance() {
        return new TrackListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_track_list, container, false);
        // Set the adapter
        mRecyclerView = (RecyclerView) view.findViewById(R.id.tracklist_recycler_view);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setLayoutManager(layoutManager);
        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.track_swipe_refresh);
        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        swipeRefresh.setEnabled(false);
        swipeRefresh.setRefreshing(true);
        downloadAll = (IconButton) view.findViewById(R.id.download_all);
        downloadAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "开始下载", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction("fm.poche.media.DOWNLOAD_SERVICE");
                intent.putExtra("MSG", AppConstant.DownloadMsg.ALBUM);
                intent.setPackage(getActivity().getPackageName());
                getContext().startService(intent);
            }
        });

        downloadManager = DownloadService.getDownloadManager();
        downloadManager.setTargetFolder(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music/");
        downloadManager.getThreadPool().setCorePoolSize(1);

        initTracks(playlist.getPlaylist_id());

        return view;
    }

    public void initTracks(final int playlist_id) {
        tracks.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url("https://poche.fm/api/app/playlists/" + playlist_id)
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    parseJSONWithGSON(responseData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void parseJSONWithGSON(final String jsonData) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Gson gson = new Gson();
                    ArrayList<Track> datas = gson.fromJson(jsonData, new TypeToken<List<Track>>(){}.getType());
                    MainActivity main = (MainActivity) getActivity();
                    for (Track track : datas) {
                        track.setAlbum(main.mToolbarTitle.getText().toString());
                        tracks.add(track);
                    }
                    Message msg = Message.obtain();
                    msg.what = TRACK;
                    msg.obj = tracks;
                    sHandler.sendMessage(msg);

                } catch (Exception e) {
                    Log.d(TAG, "歌单解析失败");
                    e.printStackTrace();
                }
            }
        }).start();
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(MessageEvent event) {
        Log.d(TAG, "onMessageEvent: ");
        playlist = event.playlist;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
    }
}