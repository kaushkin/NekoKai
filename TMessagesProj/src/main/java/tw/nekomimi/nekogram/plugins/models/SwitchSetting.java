package tw.nekomimi.nekogram.plugins.models;

import com.chaquo.python.PyObject;
import tw.nekomimi.nekogram.plugins.PluginsConstants;

/* JADX INFO: loaded from: classes.dex */
public class SwitchSetting extends SettingItem {
    public boolean defaultValue;
    public String key;
    public PyObject onChangeCallback;
    public String subtext;
    public String text;

    public SwitchSetting(String str, String str2, boolean z, String str3, String str4, PyObject pyObject, PyObject pyObject2, String str5) {
        super(PluginsConstants.Settings.TYPE_SWITCH, str4, pyObject2, str5);
        this.key = str;
        this.text = str2;
        this.defaultValue = z;
        this.subtext = str3;
        this.onChangeCallback = pyObject;
    }
}