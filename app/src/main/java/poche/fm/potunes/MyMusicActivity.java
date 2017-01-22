package poche.fm.potunes;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.malinskiy.materialicons.widget.IconTextView;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import poche.fm.potunes.Model.MusicFlowAdapter;
import poche.fm.potunes.Model.OverFlowItem;
import poche.fm.potunes.Model.Track;
import poche.fm.potunes.fragment.MoreFragment;
import poche.fm.potunes.fragment.QuciControlsFragment;

public class MyMusicActivity extends BaseActivity implements MoreFragment.OnFragmentInteractionListener {
    private String TAG = "MyMusicActivity";
    private ArrayList<OverFlowItem> mStatics = new ArrayList<>();
    private MusicFlowAdapter adapter;
    private IconTextView mDownloadHeader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseSetContentView(R.layout.activity_my_music);
        baseInit();
        getBaseActionBar().setTitle(R.string.my_music);


        mDownloadHeader = (IconTextView) findViewById(R.id.download_album_header);
        setMusicInfo();

        List<Track> list = DataSupport.findAll(Track.class);
        for (Track track: list) {
            Log.d(TAG, "onCreate: " + track.getIsDownloaded() + ":"  + track.getTitle());
        }

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

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.tracklist_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MusicFlowAdapter(getBaseContext(), mStatics, null);
        recyclerView.setAdapter(adapter);
    }


}
