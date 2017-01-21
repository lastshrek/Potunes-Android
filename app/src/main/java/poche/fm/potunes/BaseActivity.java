package poche.fm.potunes;

import android.content.Context;
import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import poche.fm.potunes.fragment.QuciControlsFragment;

public class BaseActivity extends AppCompatActivity implements QuciControlsFragment.OnFragmentInteractionListener {

    private Toolbar toolbar;
    private ActionBar actionBar;
    private QuciControlsFragment quickControls;
    private String TAG = "BaseActivity";
    private LinearLayout llcontent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_activity);

    }

    public void baseInit() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.actionbar_back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (quickControls == null) {
            quickControls = QuciControlsFragment.newInstance();
            ft.add(R.id.bottom_container, quickControls).commitAllowingStateLoss();
        } else {
            ft.show(quickControls).commitAllowingStateLoss();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void baseSetContentView(int layoutResID) {
        llcontent = (LinearLayout) findViewById(R.id.llcontent);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(layoutResID, null);
        llcontent.addView(v);
    }

    public Toolbar getToolbar() {
        return this.toolbar;
    }

    public QuciControlsFragment getQuickControls() {
        return this.quickControls;
    }

    public ActionBar getBaseActionBar() {
        return actionBar;
    }

    // press back button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home: //对用户按home icon的处理，本例只需关闭activity，就可返回上一activity，即主activity。
                Log.d("", "onOptionsItemSelected: ");
                overridePendingTransition(0,0);
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
