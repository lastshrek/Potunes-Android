package poche.fm.potunes;

import android.support.v4.app.Fragment;

/**
 * Created by purchas on 2017/2/12.
 */

public interface IFragmentCallBack {
    /**
     * 从一个Fragment 跳到另一个Fragment
     *
     * @param tag
     * @param current
     */
    void jump(String tag, Fragment current);

    /**
     * 返回第一个Fragment
     */
    void home();
}
