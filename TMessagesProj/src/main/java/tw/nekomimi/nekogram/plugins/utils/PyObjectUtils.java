package tw.nekomimi.nekogram.plugins.utils;

import android.text.TextUtils;
import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class PyObjectUtils {
    private PyObjectUtils() {
    }

    public static String getString(PyObject pyObject, String str, String str2) {
        return getString(pyObject, str, str2, false);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    public static String getString(PyObject pyObject, String str, String str2, boolean z) {
        if (pyObject != null && !TextUtils.isEmpty(str)) {
            try {
                PyObject pyObjectCallAttr = z ? pyObject.callAttr("get", str) : pyObject.get((Object) str);
                if (pyObjectCallAttr != null) {
                    try {
                        String string = pyObjectCallAttr.toString();
                        pyObjectCallAttr.close();
                        return string;
                    } catch (Throwable th) {
                        try {
                            pyObjectCallAttr.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                }
                if (pyObjectCallAttr != null) {
                    pyObjectCallAttr.close();
                    return str2;
                }
            } catch (PyException | ClassCastException unused) {
            }
        }
        return str2;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    public static boolean getBoolean(PyObject pyObject, String str, boolean z) {
        if (pyObject != null && !TextUtils.isEmpty(str)) {
            try {
                PyObject pyObject2 = pyObject.get((Object) str);
                if (pyObject2 != null) {
                    try {
                        boolean z2 = pyObject2.toBoolean();
                        pyObject2.close();
                        return z2;
                    } catch (Throwable th) {
                        try {
                            pyObject2.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                }
                if (pyObject2 != null) {
                    pyObject2.close();
                    return z;
                }
            } catch (PyException | ClassCastException unused) {
            }
        }
        return z;
    }

    public static int getInt(PyObject pyObject, String str, int i) {
        return getInt(pyObject, str, i, false);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    public static int getInt(PyObject pyObject, String str, int i, boolean z) {
        if (pyObject != null && !TextUtils.isEmpty(str)) {
            try {
                PyObject pyObjectCallAttr = z ? pyObject.callAttr("get", str) : pyObject.get((Object) str);
                if (pyObjectCallAttr != null) {
                    try {
                        int i2 = pyObjectCallAttr.toInt();
                        pyObjectCallAttr.close();
                        return i2;
                    } catch (Throwable th) {
                        try {
                            pyObjectCallAttr.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                }
                if (pyObjectCallAttr != null) {
                    pyObjectCallAttr.close();
                    return i;
                }
            } catch (PyException | ClassCastException unused) {
            }
        }
        return i;
    }

    public static String[] getStringArray(PyObject obj, String key, String[] defaultValue) {
        if (obj == null) return defaultValue;
        try {
            PyObject val = obj.get(key);
            if (val == null) return defaultValue;
            
            List<PyObject> list = val.asList();
            String[] result = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i).toString();
            }
            return result;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}