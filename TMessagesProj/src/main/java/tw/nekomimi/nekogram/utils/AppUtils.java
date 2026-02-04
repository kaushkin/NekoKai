package tw.nekomimi.nekogram.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.Keep;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.Calendar;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

public class AppUtils {

    @Keep
    public static void log(String str) {
        logInternal(str, null, 5);
    }

    @Keep
    public static void log(Throwable th) {
        logInternal("", th, 5);
    }

    @Keep
    public static void log(String str, Throwable th) {
        logInternal(str, th, 5);
    }

    private static void logInternal(String str, Throwable th, int i) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement stackTraceElement = stackTrace[Math.max(3, Math.min(i, stackTrace.length - 1))];
        String className = stackTraceElement.getClassName();
        if (className.contains(".")) {
            className = className.substring(className.lastIndexOf(46) + 1);
        }
        if (className.contains("$")) {
            className = className.substring(className.lastIndexOf(36) + 1);
        }
        String str2 = "[" + className + "]";
        String str3 = String.format("[%s] %s", stackTraceElement.getMethodName(), str);
        if (th != null) {
            Log.e(str2, str3, th);
        } else {
            Log.d(str2, str3);
        }
    }
}