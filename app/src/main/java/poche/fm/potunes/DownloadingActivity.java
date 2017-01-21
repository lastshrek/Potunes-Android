package poche.fm.potunes;

import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import poche.fm.potunes.fragment.QuciControlsFragment;

public class DownloadingActivity extends BaseActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseSetContentView(R.layout.activity_downloading);
        baseInit();
        getBaseActionBar().setTitle(R.string.downloading);
    }


}
