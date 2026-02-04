package client_utils;

import com.chaquo.python.PyCtorMarker;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.StaticProxy;
import org.telegram.messenger.NotificationCenter;

/* JADX INFO: loaded from: classes.dex */
public class NotificationCenterDelegate implements NotificationCenter.NotificationCenterDelegate, StaticProxy {
    private PyObject _chaquopyDict;

    static {
        Python.getInstance().getModule("client_utils").get((Object) "NotificationCenterDelegate");
    }

    public NotificationCenterDelegate() {
        PyObject pyObject_chaquopyCall = PyObject._chaquopyCall(this, "__init__", new Object[0]);
        if (pyObject_chaquopyCall != null) {
            pyObject_chaquopyCall.toJava(Void.TYPE);
        }
    }

    @Override // org.telegram.messenger.NotificationCenter.NotificationCenterDelegate
    public void didReceivedNotification(int i, int i2, Object[] objArr) {
        PyObject pyObject_chaquopyCall = PyObject._chaquopyCall(this, "didReceivedNotification", Integer.valueOf(i), Integer.valueOf(i2), objArr);
        if (pyObject_chaquopyCall != null) {
            pyObject_chaquopyCall.toJava(Void.TYPE);
        }
    }

    public NotificationCenterDelegate(PyCtorMarker pyCtorMarker) {
    }

    @Override // com.chaquo.python.PyProxy
    public PyObject _chaquopyGetDict() {
        return this._chaquopyDict;
    }

    @Override // com.chaquo.python.PyProxy
    public void _chaquopySetDict(PyObject pyObject) {
        this._chaquopyDict = pyObject;
    }
}