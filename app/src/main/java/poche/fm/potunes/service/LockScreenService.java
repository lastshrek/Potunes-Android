package poche.fm.potunes.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import poche.fm.potunes.Model.Track;
import poche.fm.potunes.domain.AppConstant;

/**
 * Created by purchas on 2017/2/13.
 */

public class LockScreenService extends Service {
    private BroadcastReceiver mReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();



        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        mReceiver = new LockScreenReceiver();
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    public class LockScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (PlayerService.tracks == null) return;
            Track track = PlayerService.tracks.get(PlayerService.mPlayState.getCurrentPosition());
            if (track == null) return;


            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Intent lockIntent = new Intent(AppConstant.LockScreen.LOCK_SCREEN_ACTION);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

                context.startActivity(lockIntent);
                Log.d("", "onReceive: " + "屏幕有动静");

            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // do other things if you need
            } else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                // do other things if you need
            }

        }

    }

}
