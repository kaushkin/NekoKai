package tw.nekomimi.nekogram.plugins.ui;

import android.os.Build;
import android.text.Html;
import android.text.SpannableString;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.preferences.BasePreferencesActivity;
import tw.nekomimi.nekogram.utils.text.LocaleUtils;

public class PluginsInfoActivity extends BasePreferencesActivity {

    public enum PreferenceItem {
        DEVELOPER_MODE,
        COMPACT_VIEW,
        SAFE_MODE,
        DOCUMENTATION,
        TRUSTED_PLUGINS;

        public int getId() {
            return ordinal() + 1;
        }
    }

    @Override
    public String getTitle() {
        return LocaleController.getString(R.string.PluginsEngine);
    }

    @Override
    protected void fillItems(ArrayList<UItem> arrayList, UniversalAdapter universalAdapter) {
        SpannableString spannableString;
        arrayList.add(UItem.asHeader(LocaleController.getString(R.string.Settings)));
        arrayList.add(UItem.asCheck(PreferenceItem.DEVELOPER_MODE.getId(), LocaleController.getString(R.string.PluginsDevMode)).setChecked(NekoConfig.pluginsDevMode).setEnabled(NekoConfig.pluginsEngine));
        arrayList.add(UItem.asCheck(PreferenceItem.COMPACT_VIEW.getId(), LocaleController.getString(R.string.PluginsCompactView)).setChecked(NekoConfig.pluginsCompactView).setEnabled(NekoConfig.pluginsEngine));
        arrayList.add(UItem.asCheck(PreferenceItem.SAFE_MODE.getId(), LocaleController.getString(R.string.PluginsSafeMode)).setChecked(NekoConfig.pluginsSafeMode));
        arrayList.add(UItem.asShadow(LocaleController.getString(R.string.PluginsSafeModeInfo2)));
        arrayList.add(UItem.asHeader(LocaleController.getString(R.string.Links)));
        UItem linkAlias = UItem.asButton(PreferenceItem.DOCUMENTATION.getId(), LocaleController.getString(R.string.PluginsDocumentation));
        linkAlias.iconResId = R.drawable.menu_intro;
        arrayList.add(linkAlias);
        UItem linkAlias2 = UItem.asButton(PreferenceItem.TRUSTED_PLUGINS.getId(), LocaleController.getString(R.string.PluginsTrusted));
        linkAlias2.accent = true;
        linkAlias2.iconResId = R.drawable.msg2_policy;
        arrayList.add(linkAlias2);
        
        String string = LocaleController.getString(R.string.PluginsPoweredBy);
        if (Build.VERSION.SDK_INT >= 24) {
            spannableString = new SpannableString(Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY));
        } else {
            spannableString = new SpannableString(Html.fromHtml(string));
        }
        arrayList.add(UItem.asShadow(LocaleUtils.formatWithHtmlURLs(spannableString)));
    }

    @Override
    protected void onClick(UItem uItem, View view, int i, float f, float f2) {
        int i2 = uItem.id;
        if (i2 <= 0 || i2 > PreferenceItem.values().length) {
            return;
        }
        PreferenceItem preferenceItem = PreferenceItem.values()[uItem.id - 1];
        
        if (view instanceof TextCheckCell) {
            TextCheckCell checkCell = (TextCheckCell) view;
            
            if (NekoConfig.pluginsEngine || preferenceItem == PreferenceItem.SAFE_MODE) {
                switch (preferenceItem) {
                    case DEVELOPER_MODE:
                        NekoConfig.togglePluginsDevMode();
                        checkCell.setChecked(NekoConfig.pluginsDevMode);
                        uItem.checked = NekoConfig.pluginsDevMode;
                        
                        BulletinFactory.of(this).createSimpleBulletin(
                            NekoConfig.pluginsDevMode ? R.raw.contact_check : R.raw.error, 
                            LocaleController.getString(NekoConfig.pluginsDevMode ? R.string.PluginsDevServerLaunched : R.string.PluginsDevServerStopped)
                        ).show();
                        break;

                    case COMPACT_VIEW:
                        NekoConfig.togglePluginsCompactView();
                        checkCell.setChecked(NekoConfig.pluginsCompactView);
                        uItem.checked = NekoConfig.pluginsCompactView;
                        break;

                    case SAFE_MODE:
                        NekoConfig.togglePluginsSafeMode();
                        checkCell.setChecked(NekoConfig.pluginsSafeMode);
                        uItem.checked = NekoConfig.pluginsSafeMode;
                        break;
                }
                this.listView.adapter.update(true);
                return;
            }
        }

        if (preferenceItem == PreferenceItem.DOCUMENTATION || preferenceItem == PreferenceItem.TRUSTED_PLUGINS) {
            Browser.openUrl(getParentActivity(), preferenceItem == PreferenceItem.DOCUMENTATION ? "http://plugins.exteragram.app/" : "https://t.me/addlist/PeeXG3jk1V0wODgy");
        }
    }
}