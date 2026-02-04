package tw.nekomimi.nekogram.plugins.models;

import tw.nekomimi.nekogram.plugins.PluginsConstants;

/* JADX INFO: loaded from: classes.dex */
public class DividerSetting extends SettingItem {
    public String text;

    public DividerSetting(String str) {
        super(PluginsConstants.Settings.TYPE_DIVIDER);
        this.text = str;
    }
}