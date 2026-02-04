package tw.nekomimi.nekogram.plugins.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.plugins.Plugin;
import tw.nekomimi.nekogram.plugins.PluginsController;
import tw.nekomimi.nekogram.plugins.ui.components.EmptyPluginsView;
import tw.nekomimi.nekogram.plugins.ui.components.PluginCell;
import tw.nekomimi.nekogram.plugins.ui.components.PluginCellDelegate;
import tw.nekomimi.nekogram.preferences.BasePreferencesActivity;
import tw.nekomimi.nekogram.utils.text.LocaleUtils;

public class PluginsActivity extends BasePreferencesActivity implements NotificationCenter.NotificationCenterDelegate {
    private static final int PLUGIN = 1;
    private static final int TOGGLE_BUTTON = 2;
    private ActionBarMenuItem infoItem;
    private boolean isSwitchingEngineState = false;
    private String query;
    private ActionBarMenuItem searchItem;
    private boolean searching;

    @Override
    public View createView(Context context) {
        View viewCreateView = super.createView(context);
        ActionBarMenuItem actionBarMenuItemSearchListener = this.actionBar.menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                PluginsActivity.this.searching = true;
                PluginsActivity.this.listView.adapter.update(true);
                PluginsActivity.this.listView.scrollToPosition(0);
                if (PluginsActivity.this.infoItem != null) {
                    PluginsActivity.this.infoItem.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSearchCollapse() {
                PluginsActivity.this.searching = false;
                PluginsActivity.this.query = null;
                PluginsActivity.this.listView.adapter.update(true);
                PluginsActivity.this.listView.scrollToPosition(0);
                if (PluginsActivity.this.infoItem != null) {
                    PluginsActivity.this.infoItem.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTextChanged(EditText editText) {
                PluginsActivity.this.query = editText.getText().toString();
                PluginsActivity.this.listView.adapter.update(true);
                PluginsActivity.this.listView.scrollToPosition(0);
            }
        });
        this.searchItem = actionBarMenuItemSearchListener;
        actionBarMenuItemSearchListener.setSearchFieldHint(LocaleController.getString(R.string.Search));
        AndroidUtilities.updateViewVisibilityAnimated(this.searchItem, NekoConfig.pluginsEngine && !PluginsController.getInstance().plugins.isEmpty(), 0.5f, false);
        ActionBarMenuItem actionBarMenuItemAddItem = this.actionBar.menu.addItem(1, R.drawable.msg_info);
        this.infoItem = actionBarMenuItemAddItem;
        actionBarMenuItemAddItem.setOnClickListener(view -> presentFragment(new PluginsInfoActivity()));
        
        this.listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int i) {
                if (i == 1) {
                    AndroidUtilities.hideKeyboard(PluginsActivity.this.getParentActivity().getCurrentFocus());
                }
            }
        });
        this.fragmentView = viewCreateView;
        return viewCreateView;
    }

    @Override
    public String getTitle() {
        return LocaleController.getString(R.string.Plugins);
    }

    @Override
    protected void fillItems(ArrayList<UItem> arrayList, UniversalAdapter universalAdapter) {
        Plugin plugin;
        if (!this.searching) {
            arrayList.add(UItem.asRippleCheck(2, LocaleController.getString(R.string.EnablePluginsEngine)).setChecked(NekoConfig.pluginsEngine));
        }
        if (NekoConfig.pluginsEngine) {
            HashMap<String, Plugin> map = new HashMap<>(PluginsController.getInstance().plugins);
            UItem uItemAsSpace = UItem.asSpace(AndroidUtilities.dp(8.0f));
            uItemAsSpace.transparent = true;
            arrayList.add(uItemAsSpace);
            
            if (this.searching && !TextUtils.isEmpty(this.query)) {
                map.values().removeIf(p -> !p.getName().toLowerCase().contains(this.query.toLowerCase()));
                if (!map.isEmpty()) {
                    for (Plugin p : map.values()) {
                        arrayList.add(createPluginItem(p));
                    }
                    return;
                }
            }
            if (map.isEmpty()) {
                EmptyPluginsView emptyPluginsView = new EmptyPluginsView(getContext());
                if (this.searching) {
                    MediaDataController.getInstance(UserConfig.selectedAccount).setPlaceholderImage(emptyPluginsView.getBackupImageView(), "AnimatedEmojies", "🔎", "100_100");
                    emptyPluginsView.setText(LocaleController.getString(R.string.PluginsNotFound));
                } else {
                    MediaDataController.getInstance(UserConfig.selectedAccount).setPlaceholderImage(emptyPluginsView.getBackupImageView(), "AnimatedEmojies", "📂", "100_100");
                    emptyPluginsView.setText(LocaleUtils.formatWithUsernames(LocaleController.getString(R.string.PluginsInfo)));
                }
                arrayList.add(UItem.asFullscreenCustom(emptyPluginsView, AndroidUtilities.dp(72.0f) + ActionBar.getCurrentActionBarHeight() + AndroidUtilities.statusBarHeight));
            } else {
                if (!NekoConfig.pinnedPlugins.isEmpty()) {
                    for (String str : NekoConfig.pinnedPlugins) {
                        if (map.containsKey(str) && (plugin = map.get(str)) != null) {
                            arrayList.add(createPluginItem(plugin));
                        }
                    }
                }
                List<Plugin> arrayList2 = new ArrayList<>(map.values());
                Collections.sort(arrayList2, Comparator.comparing(Plugin::getName));
                
                for (Plugin plugin2 : arrayList2) {
                    if (!PluginsController.isPluginPinned(plugin2.getId())) {
                        arrayList.add(createPluginItem(plugin2));
                    }
                }
            }
            UItem uItemAsSpace2 = UItem.asSpace(AndroidUtilities.dp(12.0f));
            uItemAsSpace2.transparent = true;
            arrayList.add(uItemAsSpace2);
        }
    }

    private UItem createPluginItem(Plugin plugin) {
        return PluginCell.Factory.as(plugin, new PluginCellDelegate() {
            @Override
            public void sharePlugin() {
                PluginsController.PluginsEngine pluginEngine = PluginsController.getInstance().getPluginEngine(plugin.getId());
                if (pluginEngine != null) {
                    pluginEngine.sharePlugin(plugin.getId());
                }
            }

            @Override
            public void openInExternalApp() {
                PluginsController.PluginsEngine pluginEngine = PluginsController.getInstance().getPluginEngine(plugin.getId());
                if (pluginEngine != null) {
                    pluginEngine.openInExternalApp(plugin.getId());
                }
            }

            @Override
            public void deletePlugin() {
                AlertDialog.Builder message = new AlertDialog.Builder(PluginsActivity.this.getParentActivity(), PluginsActivity.this.getResourceProvider()).setTitle(LocaleController.getString(R.string.PluginDelete)).setMessage(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.PluginDeleteInfo, plugin.getName())));
                String string = LocaleController.getString(R.string.Delete);
                AlertDialog alertDialogCreate = message.setPositiveButton(string, (alertDialog, i) -> 
                    PluginsController.getInstance().deletePlugin(plugin.getId(), str -> 
                        AndroidUtilities.runOnUIThread(() -> {
                            if (fragmentView != null) {
                                PluginsActivity.this.listView.adapter.update(true);
                                if (str != null) {
                                    BulletinFactory.of(PluginsActivity.this).createSimpleBulletin(R.raw.error, str).show();
                                }
                            }
                        })
                    )
                ).setNegativeButton(LocaleController.getString(R.string.Cancel), null).create();
                alertDialogCreate.show();
                TextView textView = (TextView) alertDialogCreate.getButton(-1);
                if (textView != null) {
                    textView.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
            }

            @Override
            public void togglePlugin(View view) {
                final PluginCell pluginCell = (PluginCell) view;
                final boolean z = !plugin.isEnabled();
                PluginsController.getInstance().setPluginEnabled(plugin.getId(), z, str -> 
                    AndroidUtilities.runOnUIThread(() -> {
                        if (str != null) {
                            BulletinFactory.of(PluginsActivity.this).createSimpleBulletin(R.raw.error, LocaleController.formatString(z ? R.string.PluginEnableError : R.string.PluginDisableError, plugin.getName()), LocaleUtils.createCopySpan(PluginsActivity.this), () -> {
                                if (AndroidUtilities.addToClipboard(str)) {
                                    BulletinFactory.of(PluginsActivity.this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                                }
                            }).show();
                        } else {
                            pluginCell.setChecked(z, true);
                        }
                    })
                );
            }

            @Override
            public void openPluginSettings() {
                PluginsController.PluginsEngine pluginEngine;
                if (!PluginsController.getInstance().hasPluginSettings(plugin.getId()) || (pluginEngine = PluginsController.getInstance().getPluginEngine(plugin.getId())) == null) {
                    return;
                }
                pluginEngine.openPluginSettings(plugin, PluginsActivity.this);
            }

            @Override
            public void pinPlugin(View view) {
                boolean zIsPluginPinned = PluginsController.isPluginPinned(plugin.getId());
                PluginsController.setPluginPinned(plugin.getId(), !zIsPluginPinned);
                ((PluginCell) view).setPinned(!zIsPluginPinned);
                PluginsActivity.this.listView.adapter.update(true);
                PluginsActivity.this.listView.smoothScrollToPosition(0);
            }

            @Override
            public boolean canOpenInExternalApp() {
                PluginsController.PluginsEngine pluginEngine = PluginsController.getInstance().getPluginEngine(plugin.getId());
                return pluginEngine != null && pluginEngine.canOpenInExternalApp();
            }
        });
    }

    @Override
    protected void onClick(UItem uItem, View view, int i, float f, float f2) {
        if (uItem.id == 2) {
            togglePluginsEngine(view, uItem);
        }
    }

    private void togglePluginsEngine(View view, UItem uItem) {
        if (this.isSwitchingEngineState) {
            return;
        }
        this.isSwitchingEngineState = true;
        
        NekoConfig.togglePluginsEngine();
        boolean z = NekoConfig.pluginsEngine;

        TextCheckCell textCheckCell = (TextCheckCell) view;
        uItem.checked = z;
        textCheckCell.setChecked(z);
        textCheckCell.setBackgroundColorAnimated(z, Theme.getColor(z ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
        Runnable runnable = () -> AndroidUtilities.runOnUIThread(() -> {
            if (this.fragmentView == null) {
                return;
            }
            if (this.searching) {
                this.actionBar.closeSearchField();
            }
            AndroidUtilities.updateViewVisibilityAnimated(this.searchItem, NekoConfig.pluginsEngine && !PluginsController.getInstance().plugins.isEmpty(), 0.5f, true);
            this.listView.adapter.update(true);
            this.isSwitchingEngineState = false;
        });
        
        if (NekoConfig.pluginsEngine) {
            PluginsController.getInstance().init(runnable);
        } else {
            PluginsController.getInstance().shutdown(runnable);
        }
    }

    @Override
    public int getNavigationBarColor() {
        return Theme.getColor(Theme.key_windowBackgroundGray);
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginsUpdated);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.reloadInterface);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pluginsUpdated);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.reloadInterface);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int i, int i2, Object... objArr) {
        if (i == NotificationCenter.pluginsUpdated) {
            AndroidUtilities.updateViewVisibilityAnimated(this.searchItem, NekoConfig.pluginsEngine && !PluginsController.getInstance().plugins.isEmpty(), 0.5f, true);
            this.listView.adapter.update(true);
        } else if (i == NotificationCenter.reloadInterface) {
            this.listView.invalidateViews();
        }
    }
}