package tw.nekomimi.nekogram.plugins.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chaquo.python.PyObject;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import tw.nekomimi.nekogram.plugins.Plugin;
import tw.nekomimi.nekogram.plugins.PluginsConstants;
import tw.nekomimi.nekogram.plugins.PluginsController;
import tw.nekomimi.nekogram.plugins.PythonPluginsEngine;
import tw.nekomimi.nekogram.plugins.models.DividerSetting;
import tw.nekomimi.nekogram.plugins.models.EditTextSetting;
import tw.nekomimi.nekogram.plugins.models.HeaderSetting;
import tw.nekomimi.nekogram.plugins.models.InputSetting;
import tw.nekomimi.nekogram.plugins.models.SelectorSetting;
import tw.nekomimi.nekogram.plugins.models.SettingItem;
import tw.nekomimi.nekogram.plugins.models.SwitchSetting;
import tw.nekomimi.nekogram.plugins.models.TextSetting;
import tw.nekomimi.nekogram.plugins.ui.components.PluginEditTextCell;
import tw.nekomimi.nekogram.preferences.BasePreferencesActivity;
import tw.nekomimi.nekogram.utils.system.VibratorUtils;

public class PluginSettingsActivity extends BasePreferencesActivity implements NotificationCenter.NotificationCenterDelegate {
    private final PyObject createSubFragmentCallback;
    private final String customTitle;
    private final Plugin plugin;
    private ActionBarMenuItem resetItem;
    private List<SettingItem> settingItems;
    private String settingsLinkPrefix;
    private Integer targetSettingItemId;
    private String targetSettingName;

    public PluginSettingsActivity(Plugin plugin) {
        this(plugin, null, null, null, null);
    }

    public PluginSettingsActivity(Plugin plugin, String str) {
        this(plugin, null, null, null, str);
    }

    public PluginSettingsActivity(Plugin plugin, String str, List<SettingItem> list, PyObject pyObject) {
        this(plugin, str, list, pyObject, null);
    }

    public PluginSettingsActivity(Plugin plugin, String str, List<SettingItem> list, PyObject pyObject, String str2) {
        this.plugin = plugin;
        this.customTitle = str;
        this.settingItems = list;
        this.createSubFragmentCallback = pyObject;
        this.targetSettingName = str2;
        this.targetSettingItemId = null;
        this.settingsLinkPrefix = null;
    }

    public PluginSettingsActivity setSettingsLinkPrefix(String str) {
        this.settingsLinkPrefix = str;
        return this;
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginSettingsRegistered);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginSettingsUnregistered);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pluginSettingsRegistered);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pluginSettingsUnregistered);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.pluginSettingsRegistered) {
            String pluginId = (args.length > 0 && args[0] instanceof String) ? (String) args[0] : null;
            if (this.plugin != null && (pluginId == null || this.plugin.getId().equals(pluginId))) {
                if (this.createSubFragmentCallback != null) {
                    Utilities.pluginsQueue.postRunnable(this::lambda$didReceivedNotification$1);
                    return;
                }
                if (this.listView != null && this.listView.adapter != null) {
                    this.listView.adapter.update(true);
                    if (this.resetItem != null) {
                        AndroidUtilities.updateViewVisibilityAnimated(this.resetItem, PluginsController.getInstance().hasPluginSettingsPreferences(this.plugin.getId()), 0.5f, true);
                    }
                }
            }
        } else if (id == NotificationCenter.pluginSettingsUnregistered) {
            String pluginId = (args.length > 0 && args[0] instanceof String) ? (String) args[0] : null;
            if (this.plugin != null && (pluginId == null || this.plugin.getId().equals(pluginId))) {
                if (!PluginsController.getInstance().hasPluginSettings(this.plugin.getId())) {
                    finishFragment();
                }
            }
        }
    }

    private void lambda$didReceivedNotification$1() {
        final List<SettingItem> arrayList = new ArrayList<>();
        try {
            PyObject pyObjectCall = this.createSubFragmentCallback.call();
            if (pyObjectCall != null) {
                PluginsController.PluginsEngine pluginsEngine = PluginsController.engines.get(PluginsConstants.PYTHON);
                if (pluginsEngine instanceof PythonPluginsEngine) {
                    arrayList.addAll(((PythonPluginsEngine) pluginsEngine).parsePySettingDefinitions(pyObjectCall.asList()));
                }
            }
            AndroidUtilities.runOnUIThread(() -> {
                this.settingItems = arrayList;
                if (this.listView != null && this.listView.adapter != null) {
                    this.listView.adapter.update(true);
                }
            });
        } catch (Exception unused) {
        }
    }

    @Override
    public String getTitle() {
        return this.customTitle != null ? this.customTitle : (this.plugin != null ? this.plugin.getName() : "");
    }

    @Override
    public View createView(Context context) {
        super.createView(context);
        
        if (this.createSubFragmentCallback == null && this.plugin != null) {
            ActionBarMenuItem actionBarMenuItemAddItem = this.actionBar.createMenu().addItem(0, R.drawable.msg_reset);
            this.resetItem = actionBarMenuItemAddItem;
            actionBarMenuItemAddItem.setContentDescription(LocaleController.getString(R.string.Reset));
            AndroidUtilities.updateViewVisibilityAnimated(this.resetItem, PluginsController.getInstance().hasPluginSettingsPreferences(this.plugin.getId()), 0.5f, false);
            this.resetItem.setTag(null);
            this.resetItem.setOnClickListener(view -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
                builder.setTitle(LocaleController.getString(R.string.Reset));
                builder.setMessage(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.ResetPluginSettingsInfo, this.plugin.getName())));
                builder.setPositiveButton(LocaleController.getString(R.string.Reset), (alertDialog, i) -> {
                    AndroidUtilities.updateViewVisibilityAnimated(this.resetItem, false, 0.5f, true);
                    PluginsController.getInstance().clearPluginSettingsPreferences(this.plugin.getId());
                    PluginsController.getInstance().loadPluginSettings(this.plugin.getId());
                    AndroidUtilities.runOnUIThread(() -> BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.formatString(R.string.ResetPluginSettings, this.plugin.getName())).show());
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                AlertDialog alertDialogCreate = builder.create();
                showDialog(alertDialogCreate);
                TextView textView = (TextView) alertDialogCreate.getButton(-1);
                if (textView != null) {
                    textView.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
            });
        }
        return this.fragmentView;
    }

    public void checkTargetSetting() {
        if (this.targetSettingItemId != null && this.listView != null && this.listView.adapter != null) {
            final int pos = this.listView.findPositionByItemId(this.targetSettingItemId);
            if (pos >= 0 && pos < this.listView.adapter.getItemCount()) {
                this.listView.highlightRow(() -> {
                    this.layoutManager.scrollToPositionWithOffset(pos, AndroidUtilities.dp(60.0f));
                    return pos;
                });
            }
            this.targetSettingItemId = null;
        } 
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (this.plugin == null) return;

        List<SettingItem> currentSettings = this.settingItems;
        if (currentSettings == null) {
            currentSettings = PluginsController.getInstance().getPluginSettingsList(this.plugin.getId());
        }

        if (currentSettings == null || currentSettings.isEmpty()) return;

        for (SettingItem item : currentSettings) {
            if (item == null) continue;

            int iconRes = 0;
            if (!TextUtils.isEmpty(item.icon)) {
                try {
                    
                    Context context = ApplicationLoader.applicationContext;
                    iconRes = context.getResources().getIdentifier(item.icon, "drawable", context.getPackageName());
                } catch (Exception ignored) {}
            }

            UItem uItem = null;
            String type = item.type;

            switch (type) {
                case "divider":
                    DividerSetting ds = (DividerSetting) item;
                    uItem = UItem.asShadow(ds.text != null ? LocaleController.getString(ds.text, R.string.PluginNoDescription) : "");
                    break;
                case "selector":
                    SelectorSetting ss = (SelectorSetting) item;
                    if (ss.key != null && ss.text != null && ss.items != null && ss.items.length > 0) {
                        int val = PluginsController.getInstance().getPluginSettingInt(this.plugin.getId(), ss.key, ss.defaultValue);
                        if (val < 0 || val >= ss.items.length) {
                            val = Math.max(0, Math.min(ss.defaultValue, ss.items.length - 1));
                            PluginsController.getInstance().setPluginSetting(this.plugin.getId(), ss.key, val);
                        }
                        uItem = UItem.asButton(0, ss.text, ss.items[val]);
                        uItem.texts = ss.items;
                        uItem.intValue = val;
                        uItem.iconResId = iconRes;
                        uItem.object2 = ss.key;
                        uItem.settingItem = ss;
                    }
                    break;
                case "input":
                    InputSetting is = (InputSetting) item;
                    if (is.key != null && is.text != null) {
                        String val = PluginsController.getInstance().getPluginSettingString(this.plugin.getId(), is.key, is.defaultValue);
                        uItem = UItem.asButton(0, is.text, val);
                        uItem.iconResId = iconRes;
                        uItem.object2 = is.key;
                        uItem.settingItem = is;
                    }
                    break;
                case "text":
                    TextSetting ts = (TextSetting) item;
                    uItem = UItem.asButton(0, ts.text);
                    uItem.settingItem = ts;
                    uItem.iconResId = iconRes;
                    uItem.accent = ts.accent;
                    uItem.red = ts.red;
                    break;
                case "switch":
                    SwitchSetting sws = (SwitchSetting) item;
                    if (sws.key != null && sws.text != null) {
                        boolean val = PluginsController.getInstance().getPluginSettingBoolean(this.plugin.getId(), sws.key, sws.defaultValue);
                        if (sws.subtext != null) {
                            uItem = UItem.asButtonCheck(0, sws.text, sws.subtext);
                        } else {
                            uItem = UItem.asCheck(0, sws.text);
                        }
                        if (iconRes != 0) {
                            uItem.iconResId = iconRes;
                        }
                        uItem.setChecked(val);
                        uItem.object2 = sws.key;
                        uItem.settingItem = sws;
                    }
                    break;
                case "header":
                    HeaderSetting hs = (HeaderSetting) item;
                    if (hs.text != null) {
                        uItem = UItem.asHeader(hs.text);
                        uItem.settingItem = hs;
                    }
                    break;
                case "edit_text":
                    EditTextSetting ets = (EditTextSetting) item;
                    if (ets.key != null && ets.hint != null) {
                        uItem = PluginEditTextCell.Factory.as(this.plugin, ets);
                    }
                    break;
            }

            if (uItem != null) {
                uItem.id = getStableId(item);
                if (uItem.settingItem != null && !TextUtils.isEmpty(uItem.settingItem.linkAlias) && !TextUtils.isEmpty(this.targetSettingName)) {
                    if (uItem.settingItem.linkAlias.equals(this.targetSettingName)) {
                        this.targetSettingItemId = uItem.id;
                        this.targetSettingName = null;
                    }
                }
                items.add(uItem);
            }
        }
    }

    @Override
    protected void onClick(final UItem uItem, View view, int i, float f, float f2) {
        if (uItem == null || this.plugin == null) return;

        SettingItem settingItem = uItem.settingItem;
        if (settingItem instanceof TextSetting) {
            final TextSetting textSetting = (TextSetting) settingItem;
            if (textSetting.createSubFragmentCallback != null) {
                Utilities.pluginsQueue.postRunnable(() -> {
                    List<SettingItem> items = new ArrayList<>();
                    try {
                        PyObject res = textSetting.createSubFragmentCallback.call();
                        if (res != null) {
                            PluginsController.PluginsEngine engine = PluginsController.engines.get(PluginsConstants.PYTHON);
                            if (engine instanceof PythonPluginsEngine) {
                                items.addAll(((PythonPluginsEngine) engine).parsePySettingDefinitions(res.asList()));
                            }
                        }
                        AndroidUtilities.runOnUIThread(() -> {
                            if (!items.isEmpty()) {
                                String prefix = (this.settingsLinkPrefix == null ? "" : this.settingsLinkPrefix + ":") + uItem.settingItem.linkAlias;
                                PluginSettingsActivity sub = new PluginSettingsActivity(this.plugin, uItem.text.toString(), items, textSetting.createSubFragmentCallback);
                                presentFragment(sub.setSettingsLinkPrefix(prefix));
                            }
                        });
                    } catch (Exception ignored) {}
                });
                return;
            }
            if (textSetting.onClickCallback != null) {
                try {
                    textSetting.onClickCallback.call(view);
                } catch (Exception ignored) {}
                return;
            }
        }

        if (uItem.object2 instanceof String) {
            String key = (String) uItem.object2;
            if (view instanceof TextCheckCell) {
                boolean newState = !((TextCheckCell) view).isChecked();
                ((TextCheckCell) view).setChecked(newState);
                uItem.setChecked(newState);
                Utilities.pluginsQueue.postRunnable(() -> {
                    PluginsController.getInstance().setPluginSetting(this.plugin.getId(), key, newState);
                    if (settingItem instanceof SwitchSetting) {
                        triggerOnChange(((SwitchSetting) settingItem).onChangeCallback, key, newState);
                    }
                });
            } else if (view instanceof NotificationsCheckCell) {
                boolean newState = !((NotificationsCheckCell) view).isChecked();
                ((NotificationsCheckCell) view).setChecked(newState);
                uItem.setChecked(newState);
                Utilities.pluginsQueue.postRunnable(() -> {
                    PluginsController.getInstance().setPluginSetting(this.plugin.getId(), key, newState);
                    if (settingItem instanceof SwitchSetting) {
                        triggerOnChange(((SwitchSetting) settingItem).onChangeCallback, key, newState);
                    }
                });
            } else if (view instanceof TextCell) {
                if (settingItem instanceof SelectorSetting) {
                    showSelectorDialog(uItem, view, key);
                } else if (settingItem instanceof InputSetting) {
                    showStringInputDialog(uItem, view, key);
                }
            }
        }
    }

    @Override
    public boolean onLongClick(final UItem uItem, View view, int i, float f, float f2) {
        if (uItem != null && this.plugin != null) {
            String alias = uItem.settingItem.linkAlias;
            if (!TextUtils.isEmpty(alias)) {
                view.performHapticFeedback(VibratorUtils.getType(3), 1);
                ItemOptions.makeOptions(this, view).add(R.drawable.msg_copy, LocaleController.getString(R.string.CopyLink), () -> {
                    String link = uItem.settingItem.getLink(this.plugin.getId(), this.settingsLinkPrefix);
                    if (AndroidUtilities.addToClipboard(link)) {
                        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.LinkCopied)).show();
                    }
                }).show();
                return true;
            }
            if (uItem.settingItem.onLongClickCallback != null) {
                try {
                    uItem.settingItem.onLongClickCallback.call(view);
                } catch (Exception ignored) {}
                return true;
            }
        }
        return false;
    }

    private void showStringInputDialog(UItem uItem, final View view, final String key) {
        if (getParentActivity() == null) return;
        
        InputSetting setting = (InputSetting) uItem.settingItem;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
        builder.setTitle(uItem.text);
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        
        if (setting.subtext != null) {
            TextView textView = new TextView(getContext());
            textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
            textView.setTextSize(1, 16.0f);
            textView.setText(setting.subtext);
            layout.addView(textView, LayoutHelper.createLinear(-1, -2, 24.0f, 5.0f, 24.0f, 12.0f));
        }
        
        EditTextBoldCursor editText = new EditTextBoldCursor(getContext());
        editText.lineYFix = true;
        editText.setTextSize(1, 18.0f);
        editText.setText(PluginsController.getInstance().getPluginSettingString(this.plugin.getId(), key, setting.defaultValue));
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
        editText.setHintColor(Theme.getColor(Theme.key_groupcreate_hintText, getResourceProvider()));
        editText.setHintText("Enter value");
        editText.setFocusable(true);
        editText.setInputType(147457); 
        editText.setBackgroundDrawable(null);
        editText.setPadding(0, AndroidUtilities.dp(6.0f), 0, AndroidUtilities.dp(6.0f));
        layout.addView(editText, LayoutHelper.createLinear(-1, -2, 24.0f, 0.0f, 24.0f, 10.0f));
        
        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString(R.string.Done), (dialog, which) -> {
            String val = editText.getText().toString();
            ((TextCell) view).setValue(val, true);
            Utilities.pluginsQueue.postRunnable(() -> {
                PluginsController.getInstance().setPluginSetting(this.plugin.getId(), key, val);
                triggerOnChange(setting.onChangeCallback, key, val);
            });
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            editText.requestFocus();
            editText.setSelection(editText.length());
            AndroidUtilities.showKeyboard(editText);
        });
        showDialog(dialog);
    }

    private void showSelectorDialog(UItem uItem, final View view, final String key) {
        if (getParentActivity() == null) return;
        
        SelectorSetting setting = (SelectorSetting) uItem.settingItem;
        AtomicReference<AlertDialog> dialogRef = new AtomicReference<>();
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        
        for (int i = 0; i < setting.items.length; i++) {
            final int index = i;
            RadioColorCell cell = new RadioColorCell(getParentActivity());
            cell.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(setting.items[i], PluginsController.getInstance().getPluginSettingInt(this.plugin.getId(), key, setting.defaultValue) == i);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            layout.addView(cell);
            cell.setOnClickListener(v -> {
                if (dialogRef.get() != null) dialogRef.get().dismiss();
                ((TextCell) view).setValue(setting.items[index], true);
                Utilities.pluginsQueue.postRunnable(() -> {
                    PluginsController.getInstance().setPluginSetting(this.plugin.getId(), key, index);
                    triggerOnChange(setting.onChangeCallback, key, index);
                });
            });
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity()).setTitle(uItem.text).setView(layout).setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        dialogRef.set(builder.create());
        showDialog(dialogRef.get());
    }

    private int getStableId(SettingItem settingItem) {
        if (settingItem instanceof SwitchSetting) return Objects.hash(PluginsConstants.Settings.TYPE_SWITCH, ((SwitchSetting) settingItem).key);
        if (settingItem instanceof InputSetting) return Objects.hash(PluginsConstants.Settings.TYPE_INPUT, ((InputSetting) settingItem).key);
        if (settingItem instanceof EditTextSetting) return Objects.hash("edit", ((EditTextSetting) settingItem).key);
        if (settingItem instanceof SelectorSetting) return Objects.hash(PluginsConstants.Settings.TYPE_SELECTOR, ((SelectorSetting) settingItem).key);
        if (settingItem instanceof HeaderSetting) return Objects.hash(PluginsConstants.Settings.TYPE_HEADER, ((HeaderSetting) settingItem).text);
        if (settingItem instanceof DividerSetting) return Objects.hash(PluginsConstants.Settings.TYPE_DIVIDER, ((DividerSetting) settingItem).text);
        if (settingItem instanceof TextSetting) return Objects.hash("text", ((TextSetting) settingItem).text);
        return settingItem.hashCode();
    }

    private void triggerOnChange(final PyObject pyObject, final String str, final Object obj) {
        if (pyObject != null) {
            try {
                pyObject.call(obj);
            } catch (Exception e) {
                FileLog.e("Error executing on_change callback for " + this.plugin.getId() + "/" + str, e);
            }
        }
    }
}