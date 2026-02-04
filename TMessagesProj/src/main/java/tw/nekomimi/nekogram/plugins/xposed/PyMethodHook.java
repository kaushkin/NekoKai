package tw.nekomimi.nekogram.plugins.xposed;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;

import org.telegram.messenger.FileLog;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import tw.nekomimi.nekogram.plugins.PluginsConstants;
import tw.nekomimi.nekogram.plugins.hooks.HookFilter;

/* JADX INFO: loaded from: classes.dex */
public class PyMethodHook extends XC_MethodHook {
    private ArrayList<HookFilter> afterHookedFilters;
    private ArrayList<HookFilter> beforeHookedFilters;
    private final boolean hasAfterHook;
    private final boolean hasBeforeHook;
    private final String pluginId;
    private final PyObject pythonCallback;

    public PyMethodHook(String str, PyObject pyObject) {
        this.beforeHookedFilters = new ArrayList<>();
        this.afterHookedFilters = new ArrayList<>();
        if (pyObject == null) {
            throw new IllegalArgumentException("Python callback object cannot be null");
        }
        this.pluginId = str;
        this.pythonCallback = pyObject;
        this.hasBeforeHook = pyObject.containsKey(PluginsConstants.Xposed.BEFORE_HOOKED_METHOD);
        this.hasAfterHook = pyObject.containsKey(PluginsConstants.Xposed.AFTER_HOOKED_METHOD);
    }

    public PyMethodHook(String str, PyObject pyObject, int i) {
        super(i);
        this.beforeHookedFilters = new ArrayList<>();
        this.afterHookedFilters = new ArrayList<>();
        if (pyObject == null) {
            throw new IllegalArgumentException("Python callback object cannot be null");
        }
        this.pluginId = str;
        this.pythonCallback = pyObject;
        this.hasBeforeHook = pyObject.containsKey(PluginsConstants.Xposed.BEFORE_HOOKED_METHOD);
        this.hasAfterHook = pyObject.containsKey(PluginsConstants.Xposed.AFTER_HOOKED_METHOD);
    }

    public void setBeforeHookedFilters(ArrayList<HookFilter> arrayList) {
        this.beforeHookedFilters = arrayList;
    }

    public void setAfterHookedFilters(ArrayList<HookFilter> arrayList) {
        this.afterHookedFilters = arrayList;
    }

    public ArrayList<HookFilter> getBeforeHookedFilters() {
        return this.beforeHookedFilters;
    }

    public ArrayList<HookFilter> getAfterHookedFilters() {
        return this.afterHookedFilters;
    }

    @Override // de.robv.android.xposed.XC_MethodHook
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
        if (this.hasBeforeHook) {
            try {
                if (!this.beforeHookedFilters.isEmpty()) {
                    ArrayList<HookFilter> arrayList = this.beforeHookedFilters;
                    int size = arrayList.size();
                    int i = 0;
                    while (i < size) {
                        HookFilter hookFilter = arrayList.get(i);
                        i++;
                        if (!hookFilter.execute(methodHookParam, true)) {
                            return;
                        }
                    }
                }
                this.pythonCallback.callAttr(PluginsConstants.Xposed.BEFORE_HOOKED_METHOD, methodHookParam);
            } catch (Throwable th) {
                handleHookError("beforeHookedMethod", th);
            }
        }
    }

    @Override // de.robv.android.xposed.XC_MethodHook
    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
        if (this.hasAfterHook) {
            try {
                if (!this.afterHookedFilters.isEmpty()) {
                    ArrayList<HookFilter> arrayList = this.afterHookedFilters;
                    int size = arrayList.size();
                    int i = 0;
                    while (i < size) {
                        HookFilter hookFilter = arrayList.get(i);
                        i++;
                        if (!hookFilter.execute(methodHookParam, false)) {
                            return;
                        }
                    }
                }
                this.pythonCallback.callAttr(PluginsConstants.Xposed.AFTER_HOOKED_METHOD, methodHookParam);
            } catch (Throwable th) {
                handleHookError("afterHookedMethod", th);
            }
        }
    }

    private void handleHookError(String str, Throwable th) {
        if ((th instanceof PyException) && th.getMessage() != null && th.getMessage().contains("closed")) {
            FileLog.e("Attempted to call a closed PyObject callback in " + this.pluginId);
            return;
        }
        FileLog.e("Plugin '" + this.pluginId + "' crashed in " + str + ": " + th.getMessage(), th);
    }
}