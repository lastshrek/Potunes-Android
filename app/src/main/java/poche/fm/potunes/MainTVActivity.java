package poche.fm.potunes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import com.owen.tvrecyclerview.widget.TvRecyclerView;

import java.util.ArrayList;
import java.util.List;


import poche.fm.potunes.adapter.LayoutAdapter;

public class MainTVActivity extends AppCompatActivity {
    private String TAG = "MainActivity";
    private Context mContext;
    private TvRecyclerView mRecyclerView;
    private LayoutAdapter mLayoutAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        setContentView(R.layout.activity_main_tv);
        mContext = this;

        mRecyclerView = (TvRecyclerView) findViewById(R.id.tv_playlist);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setInterceptKeyEvent(true);


        mRecyclerView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {

            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {

            }

            @Override
            public void onReviseFocusFollow(TvRecyclerView parent, View itemView, int position) {

            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {

            }
        });

        mRecyclerView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

            }
        });

        mRecyclerView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            @Override
            public boolean onInBorderKeyEvent(int direction, int keyCode, KeyEvent event) {
                return false;
            }
        });

        mRecyclerView.setOnLoadMoreListener(new TvRecyclerView.OnLoadMoreListener() {
            @Override
            public boolean onLoadMore() {
                return false;
            }
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        mRecyclerView.setSpacingWithMargins(18, 18);
        mLayoutAdapter = new LayoutAdapter(this, mRecyclerView, R.layout.activity_main_tv);
        mRecyclerView.setAdapter(mLayoutAdapter);


    }





}
