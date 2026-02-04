package tw.nekomimi.nekogram.plugins.models;

import tw.nekomimi.nekogram.plugins.PluginsConstants;

/* JADX INFO: loaded from: classes.dex */
public class HeaderSetting extends SettingItem {
    public String text;

    public HeaderSetting(String str) {
        super(PluginsConstants.Settings.TYPE_HEADER);
        this.text = str;
    }
}