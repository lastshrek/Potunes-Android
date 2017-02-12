package poche.fm.potunes.fragment;

import android.content.Context;
import android.os.Bundle;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.litepal.crud.DataSupport;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.Model.MessageEvent;
import poche.fm.potunes.Model.Playlist;
import poche.fm.potunes.R;

public class PlaylistFragment extends Fragment {

    private OnListFragmentInteractionListener mListener;
    private static final String ARG_MEDIA_ID = "media_id";
    protected static final int TEST = 1;
    private List<Playlist> playlists = new ArrayList<>();
    private String TAG = "PlaylistFragment";

    private SwipeRefreshLayout swipeRefresh;
    private View view;
    private PlaylistAdapter adapter;
    protected RecyclerView mRecyclerView;

    private Handler sHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case TEST:
                    adapter = new PlaylistAdapter((List<Playlist>) msg.obj, getFragmentManager());
                    mRecyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    public PlaylistFragment() {
    }

    public static PlaylistFragment newInstance() {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_playlist, container, false);

        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshPlaylists();
            }
        });

        // Set the adapter
        mRecyclerView = (RecyclerView) view.findViewById(R.id.playlist);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setLayoutManager(layoutManager);
        initPlaylists();

        return view;
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
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

    private void initPlaylists() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playlists.clear();
                    playlists = loadLocalPlaylists();
                    if (playlists.size() == 0) {
                        if (playlists.size() == 0) {
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url("https://poche.fm/api/app/playlists/")
                                    .build();
                            Response response = client.newCall(request).execute();
                            String responseData = response.body().string();
                            parseJSONWithGSON(responseData);
                        }

                        Message msg = Message.obtain();
                        msg.what = TEST;
                        msg.obj = playlists;
                        sHandler.sendMessage(msg);

                    }

                    Message msg = Message.obtain();
                    msg.what = TEST;
                    msg.obj = playlists;

                    sHandler.sendMessage(msg);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private List<Playlist> loadLocalPlaylists() {
        List<Playlist> mPlaylists = DataSupport.order("id asc").find(Playlist.class);
        return mPlaylists;
    }
    private void parseJSONWithGSON(String jsonData) {
        Gson gson = new Gson();
        List<Playlist> datas = gson.fromJson(jsonData, new TypeToken<List<Playlist>>(){}.getType());

        for (Playlist playlist : datas) {
            Playlist mPlaylist = new Playlist(playlist.getTitle(), playlist.getPlaylist_id(), playlist.getCover());
            playlists.add(mPlaylist);
            mPlaylist.save();
        }

    }
    public void refreshPlaylists() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://poche.fm/api/app/playlists/")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();

                    Gson gson = new Gson();
                    List<Playlist> datas = gson.fromJson(responseData, new TypeToken<List<Playlist>>(){}.getType());
                    int maxID = playlists.get(0).getPlaylist_id();
                    List<Playlist> tempLists = new ArrayList<>();
                    for (Playlist playlist : datas) {
                        if (playlist.getPlaylist_id() > maxID) {
                            tempLists.add(playlist);
                            playlist.save();
                        }
                    }
                    for (Playlist playlist: playlists) {
                        tempLists.add(playlist);
                    }

                    playlists.clear();
                    playlists = tempLists;


                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = Message.obtain();
                            msg.what = TEST;
                            msg.obj = playlists;

                            sHandler.sendMessage(msg);
                            adapter.notifyDataSetChanged();
                            swipeRefresh.setRefreshing(false);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
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
