package poche.fm.potunes;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.MediaBrowserCompat;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import poche.fm.potunes.fragment.QuickControlsFragment;
import poche.fm.potunes.service.MusicService;
import poche.fm.potunes.utils.LogHelper;
import poche.fm.potunes.utils.ResourceHelper;

public abstract class BaseActivity extends ActionBarCastActivity implements QuickControlsFragment.OnFragmentInteractionListener {

    private static final String TAG = LogHelper.makeLogTag(BaseActivity.class);
    private QuickControlsFragment mControlsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            // Since our app icon has the same color as colorPrimary, our entry in the Recent Apps
            // list gets weird. We need to change either the icon or the color
            // of the TaskDescription.
            ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(
                    getTitle().toString(),
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher),
                    ResourceHelper.getThemeColor(this, R.attr.colorPrimary,
                            android.R.color.darker_gray));
            setTaskDescription(taskDesc);
        }



    }

    @Override
    protected void onStart() {
        super.onStart();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (mControlsFragment == null) {
            mControlsFragment = QuickControlsFragment.newInstance();
            ft.add(R.id.bottom_container, mControlsFragment).commitAllowingStateLoss();
        } else {
            ft.show(mControlsFragment).commitAllowingStateLoss();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        LogHelper.d(TAG, "Activity onStop");
    }




    protected void onMediaControllerConnected() {
        // empty implementation, can be overridden by clients.
    }
    @Override
    public void onFragmentInteraction(Uri uri) {

    }






}
