package android_utils;

import com.chaquo.python.PyCtorMarker;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.StaticProxy;

/*
    this piece of shit has to be here
    all because TMessagesProj is an android library
    and there you cant create a class named "R" (it will be deleted during the build)

    i wish the guy who named this class like that gets eaten by a shark

    client_utils.NotificationCenterDelegate is fine, i just threw it to same project as well
*/
public class R implements Runnable, StaticProxy {
    private PyObject _chaquopyDict;

    static {
        Python.getInstance().getModule("android_utils").get((Object) "R");
    }

    public R() {
        PyObject pyObject_chaquopyCall = PyObject._chaquopyCall(this, "__init__", new Object[0]);
        if (pyObject_chaquopyCall != null) {
            pyObject_chaquopyCall.toJava(Void.TYPE);
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        PyObject pyObject_chaquopyCall = PyObject._chaquopyCall(this, "run", new Object[0]);
        if (pyObject_chaquopyCall != null) {
            pyObject_chaquopyCall.toJava(Void.TYPE);
        }
    }

    public R(PyCtorMarker pyCtorMarker) {
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