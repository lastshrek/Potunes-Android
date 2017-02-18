package poche.fm.potunes.domain;

/**
 * Created by purchas on 2017/1/14.
 */

public class AppConstant {
    public class PlayerMsg {
        public static final int PLAY_MSG = 1;		//播放
        public static final int PAUSE_MSG = 2;		//暂停
        public static final int CONTINUE_MSG = 4;	//继续
        public static final int PRIVIOUS_MSG = 5;	//上一首
        public static final int NEXT_MSG = 6;		//下一首
        public static final int STOP_MSG = 7;
    }

    public class DownloadMsg {
        public static final int SINGLE = 1;
        public static final int ALBUM = 2;
        public static final int PAUSE = 3;
        public static final int RESUME = 4;
        public static final int DELETE = 5;
        public static final int RESTART = 6;
        public static final int SCAN = 7;
    }

    public class LockScreen {
        public static final String LOCK_SCREEN_ACTION = "android.intent.lockscreen";
    }
}
