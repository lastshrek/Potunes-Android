package poche.fm.potunes.utils;

import android.content.Context;

import net.grandcentrix.tray.TrayPreferences;

/**
 * Created by Purchas on 17/2/19.
 */

public class SharedPreferencesUtil extends TrayPreferences {
    public String KEY_IS_FIRST_LAUNCH = "first_launch";

    public SharedPreferencesUtil(final Context context) {
        super(context, "Potunes", 1);
    }
}
