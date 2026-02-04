package tw.nekomimi.nekogram.plugins.models;

import com.chaquo.python.PyObject;

/* JADX INFO: loaded from: classes.dex */
public class TextSetting extends SettingItem {
    public boolean accent;
    public PyObject createSubFragmentCallback;
    public PyObject onClickCallback;
    public boolean red;
    public String text;

    public TextSetting(String str, String str2, boolean z, boolean z2, PyObject pyObject, PyObject pyObject2, PyObject pyObject3, String str3) {
        super("text", str2, pyObject3, str3);
        this.text = str;
        this.accent = z;
        this.red = z2;
        this.onClickCallback = pyObject;
        this.createSubFragmentCallback = pyObject2;
    }
}