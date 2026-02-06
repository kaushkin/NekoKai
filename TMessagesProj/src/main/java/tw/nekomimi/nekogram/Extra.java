package tw.nekomimi.nekogram;

import org.telegram.messenger.BuildConfig;
import tw.nekomimi.nekogram.helpers.UserHelper;

public class Extra {
    public static final int APP_ID = 21569710;
    public static final String APP_HASH = "06c3de24f27f101a7c05e61ecf0327d2";

    public static final String PLAYSTORE_APP_URL = "https://play.google.com/store/apps/details?id=org.telegram.messenger";

    public static String WS_USER_AGENT = "NekoKai";
    public static String WS_CONN_HASH = "mock-hash";
    public static String WS_DEFAULT_DOMAIN = "google.com";

    public static String TWPIC_BOT_USERNAME = "TwPicBot"; 

    public static boolean FORCE_ANALYTICS = false;

    public static String TLV_URL = "https://google.com";

    public static String SENTRY_DSN = "";
    
    public static final long WEBVIEW_BOT_ID = 0L;

    public static boolean isDirectApp() {
        return true; 
    }

    public static UserHelper.BotInfo getHelperBot() {
        return null;
    }

    public static UserHelper.UserInfoBot getUserInfoBot(boolean fallback) {
        return null;
    }

    public static boolean isTrustedBot(long id) {
        return id == WEBVIEW_BOT_ID;
    }
}
