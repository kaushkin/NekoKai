package tw.nekomimi.nekogram.plugins;

import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import com.chaquo.python.PyObject;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.plugins.hooks.EventHookRecord;
import tw.nekomimi.nekogram.plugins.hooks.HookRecord;
import tw.nekomimi.nekogram.plugins.hooks.MenuItemRecord;
import tw.nekomimi.nekogram.plugins.hooks.PluginsHooks;
import tw.nekomimi.nekogram.plugins.hooks.XposedHookRecord;
import tw.nekomimi.nekogram.plugins.models.SettingItem;
import tw.nekomimi.nekogram.plugins.ui.PluginsActivity;
import tw.nekomimi.nekogram.plugins.ui.components.InstallPluginBottomSheet;
import tw.nekomimi.nekogram.plugins.ui.components.SafeModeBottomSheet;
import tw.nekomimi.nekogram.plugins.utils.MenuContextBuilder;
import tw.nekomimi.nekogram.plugins.utils.NativeCrashHandler;
import tw.nekomimi.nekogram.utils.ChatUtils;
import de.robv.android.xposed.XC_MethodHook;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

public class PluginsController implements PluginsHooks {
    static final String PREF_PLUGIN_ENABLED_KEY_PREFIX = "plugin_enabled_";
    public static final ConcurrentHashMap<String, PluginsEngine> engines = new ConcurrentHashMap<>();

    static {
        engines.put(PluginsConstants.PYTHON, new PythonPluginsEngine());
    }

    private volatile Map<String, List<EventHookRecord>> exactMatchEventHooksCache;
    public File pluginsDir;
    private volatile List<EventHookRecord> substringMatchEventHooksCache;
    public final ConcurrentHashMap<String, Plugin> plugins = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, List<SettingItem>> settings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MenuItemRecord> menuItemsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<MenuItemRecord>> menuItemsByMenuType = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<HookRecord>> hooks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> interestedPluginsCache = new ConcurrentHashMap<>();
    private final Object hooksCacheLock = new Object();
    private volatile boolean hooksCacheDirty = true;
    public SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("plugin_settings", 0);
    
    private final Runnable updateNotificationRunnable = () -> {
        NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.pluginsUpdated);
        NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.pluginMenuItemsUpdated);
    };

    public interface PluginsEngine {
        boolean canOpenInExternalApp();
        void checkDevServer();
        void clearPluginSettings(String str);
        void deletePlugin(String str, Utilities.Callback<String> callback);
        void executeOnAppEvent(String str);
        HookResult<PluginsHooks.PostRequestResult> executePostRequestHook(String str, int i, TLObject tLObject, TLRPC.TL_error tL_error, String str2);
        HookResult<TLObject> executePreRequestHook(String str, int i, TLObject tLObject, String str2);
        HookResult<SendMessagesHelper.SendMessageParams> executeSendMessageHook(int i, SendMessagesHelper.SendMessageParams sendMessageParams, String str);
        HookResult<TLRPC.Update> executeUpdateHook(String str, int i, TLRPC.Update update, String str2);
        HookResult<TLRPC.Updates> executeUpdatesHook(String str, int i, TLRPC.Updates updates, String str2);
        Map<String, ?> getAllPluginSettings(String str);
        String getPluginPath(String str);
        Object getPluginSetting(String str, String str2, Object obj);
        void init(Runnable runnable);
        boolean isEngineAvailable();
        boolean isPlugin(File file);
        List<SettingItem> loadPluginSettings(String str);
        void openInExternalApp(String str);
        void openPluginSetting(Plugin plugin, String str, BaseFragment baseFragment);
        void openPluginSetting(String str, String str2, BaseFragment baseFragment);
        void openPluginSettings(Plugin plugin, BaseFragment baseFragment);
        void openPluginSettings(String str, BaseFragment baseFragment);
        void setPluginEnabled(String str, boolean z, Utilities.Callback<String> callback);
        void setPluginSetting(String str, String str2, Object obj);
        void sharePlugin(String str);
        void showInstallDialog(BaseFragment baseFragment, InstallPluginBottomSheet.PluginInstallParams pluginInstallParams);
        void shutdown(Runnable runnable);
    }

    public static PluginsController getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static boolean isPluginEngineSupported() {
        return Build.VERSION.SDK_INT >= 24;
    }

    public static boolean isPluginEngineAvailable() {
        if (isPluginEngineSupported() && NekoConfig.pluginsEngine && !NekoConfig.pluginsSafeMode) {
            for (PluginsEngine pluginsEngine : engines.values()) {
                if (pluginsEngine != null) {
                    try {
                        if (pluginsEngine.isEngineAvailable()) {
                            return true;
                        }
                    } catch (Throwable th) {
                        FileLog.e("Error checking engine availability.", th);
                    }
                }
            }
        }
        return false;
    }

    public static boolean isPlugin(MessageObject messageObject) {
        String pathToMessage = ChatUtils.getInstance().getPathToMessage(messageObject);
        return (messageObject == null || messageObject.getDocumentName() == null || TextUtils.isEmpty(pathToMessage) || !isPlugin(new File(pathToMessage)) || !isPluginEngineSupported()) ? false : true;
    }

    public static boolean isPlugin(File file) {
        if (file == null) {
            return false;
        }
        for (PluginsEngine engine : engines.values()) {
            if (engine.isPlugin(file)) {
                return true;
            }
        }
        return false;
    }

    public static PluginsEngine getPluginEngine(File file) {
        if (file == null) {
            return null;
        }
        for (PluginsEngine pluginsEngine : engines.values()) {
            if (pluginsEngine.isPlugin(file)) {
                return pluginsEngine;
            }
        }
        return null;
    }

    public static void openPluginSetting(String str, String str2) {
        final BaseFragment lastFragment;
        if (TextUtils.isEmpty(str) || (lastFragment = LaunchActivity.getLastFragment()) == null) {
            return;
        }
        if (!NekoConfig.pluginsEngine) {
            BulletinFactory.of(lastFragment).createSimpleBulletin(R.raw.error, LocaleController.formatString(R.string.PluginEngineNotEnabled, str), LocaleController.getString(R.string.Enable), 2750, () -> lastFragment.presentFragment(new PluginsActivity())).show();
            return;
        }
        Plugin plugin = getInstance().plugins.get(str);
        if (plugin == null) {
            BulletinFactory.of(lastFragment).createEmojiBulletin("🤷\u200d♂️", LocaleController.formatString(R.string.PluginNotFound, str)).show();
            return;
        }
        if (!getInstance().hasPluginSettings(str)) {
            BulletinFactory.of(lastFragment).createEmojiBulletin("🤷\u200d♂️", LocaleController.formatString(R.string.PluginHasNoSettings, plugin.getName())).show();
            return;
        }
        PluginsEngine pluginEngine = getInstance().getPluginEngine(str);
        if (pluginEngine != null) {
            pluginEngine.openPluginSetting(str, str2, lastFragment);
        }
    }

    public File getPluginsDir() {
        if (pluginsDir == null) {
            File file = new File(ApplicationLoader.getFilesDirFixed(), PluginsConstants.PLUGINS);
            this.pluginsDir = file;
            if (!file.exists()) {
                this.pluginsDir.mkdirs();
            }
        }
        return pluginsDir;
    }

    public PluginsEngine getPluginEngine(String str) {
        PluginsEngine pluginsEngine = null;
        if (str != null && !TextUtils.isEmpty(str)) {
            Plugin plugin = this.plugins.get(str);
            if (plugin == null) {
                return null;
            }
            PluginsEngine pluginsEngine2 = plugin.cachedEngine;
            if (pluginsEngine2 != null) {
                return pluginsEngine2;
            }
            String engine = plugin.getEngine();
            if (engine == null) {
                return null;
            }
            pluginsEngine = engines.get(engine);
            if (pluginsEngine != null) {
                plugin.cachedEngine = pluginsEngine;
            }
        }
        return pluginsEngine;
    }

    public static boolean isPluginPinned(String str) {
        return !TextUtils.isEmpty(str) && NekoConfig.pinnedPlugins.contains(str);
    }

    public static void setPluginPinned(String str, boolean z) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        if (z) {
            NekoConfig.pinnedPlugins.add(str);
        } else {
            NekoConfig.pinnedPlugins.remove(str);
        }
        NekoConfig.savePinnedPlugins();
    }

    public void init() {
        init(null);
    }

    public void init(final Runnable runnable) {
        if (!isPluginEngineSupported() || !NekoConfig.pluginsEngine) {
            if (runnable != null) {
                runnable.run();
                return;
            }
            return;
        }
        NativeCrashHandler.checkAndHandleNativeCrash();
        
        if (!Utilities.pluginsQueue.isAlive()) {
            Utilities.pluginsQueue = new DispatchQueue("pluginsQueue");
        }

        if (this.preferences == null) {
            this.preferences = ApplicationLoader.applicationContext.getSharedPreferences("plugin_settings", 0);
        }
        
        try {
            boolean hadCrash = this.preferences.getBoolean("had_crash", false);
            String string = this.preferences.getString("crashed_plugin_id", null);
            boolean isManualSafeMode = string != null && string.equals("manual!");
            this.preferences.edit().remove("had_crash").remove("crashed_plugin_id").apply();

            if (hadCrash) {
                if (string != null && !isManualSafeMode) {
                    this.preferences.edit().putBoolean(PREF_PLUGIN_ENABLED_KEY_PREFIX + string, false).apply();
                } else {
                    NekoConfig.setPluginsSafeMode(true);
                }

                if (!isManualSafeMode) {
                    AndroidUtilities.runOnUIThread(() -> {
                        BaseFragment lastFragment = LaunchActivity.getLastFragment();
                        if (lastFragment != null) {
                            new SafeModeBottomSheet(lastFragment).show();
                        }
                    }, 800L);
                }
            } else {
                NekoConfig.setPluginsSafeMode(false);
            }
        } catch (Exception unused) {}
        
        File file = new File(ApplicationLoader.getFilesDirFixed(), PluginsConstants.PLUGINS);
        this.pluginsDir = file;
        if (!file.exists()) {
            this.pluginsDir.mkdirs();
        }
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        Runnable runnable2 = () -> {
            if (atomicInteger.addAndGet(1) < engines.size() || runnable == null) {
                return;
            }
            runnable.run();
        };
        for (PluginsEngine engine : engines.values()) {
            engine.init(runnable2);
        }
    }

    public void checkDevServers() {
        for (PluginsEngine engine : engines.values()) {
            engine.checkDevServer();
        }
    }

    public void shutdown(final Runnable runnable) {
        Utilities.pluginsQueue.postRunnable(() -> {
            final AtomicInteger atomicInteger = new AtomicInteger(0);
            Runnable runnable2 = () -> {
                if (atomicInteger.addAndGet(1) >= engines.size()) {
                    this.plugins.clear();
                    this.settings.clear();
                    FileLog.d("Plugin system shut down.");
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            };
            for (PluginsEngine engine : engines.values()) {
                engine.shutdown(runnable2);
            }
        });
    }

    public void restart() {
        FileLog.d("Restarting plugins engine...");
        shutdown(() -> {
            if (NekoConfig.pluginsEngine) {
                init(() -> FileLog.d("Plugins engine restarted."));
            }
        });
    }

    public List<SettingItem> getPluginSettingsList(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return this.settings.get(str);
    }

    public void setPluginEnabled(final String str, final boolean z, final Utilities.Callback<String> callback) {
        Utilities.pluginsQueue.postRunnable(() -> {
            PluginsEngine pluginEngine = getPluginEngine(str);
            if (pluginEngine != null) {
                pluginEngine.setPluginEnabled(str, z, callback);
                this.interestedPluginsCache.clear();
            }
        });
    }

    public void deletePlugin(final String str, final Utilities.Callback<String> callback) {
        Utilities.pluginsQueue.postRunnable(() -> {
            PluginsEngine pluginEngine = getPluginEngine(str);
            if (pluginEngine != null) {
                pluginEngine.deletePlugin(str, callback);
            }
        });
    }

    void cleanupPlugin(String str) {
        removeHooksByPluginId(str);
        invalidatePluginSettings(str);
        removeMenuItemsByPluginId(str);
    }

    public String getPluginPath(String str) {
        PluginsEngine pluginEngine;
        if (str == null || TextUtils.isEmpty(str) || (pluginEngine = getPluginEngine(str)) == null) {
            return null;
        }
        return pluginEngine.getPluginPath(str);
    }

    public void showInstallDialog(BaseFragment baseFragment, MessageObject messageObject) {
        showInstallDialog(baseFragment, InstallPluginBottomSheet.PluginInstallParams.of(messageObject));
    }

    public void showInstallDialog(BaseFragment baseFragment, String str, boolean z) {
        showInstallDialog(baseFragment, new InstallPluginBottomSheet.PluginInstallParams(str, z));
    }

    private void showInstallDialog(final BaseFragment baseFragment, InstallPluginBottomSheet.PluginInstallParams pluginInstallParams) {
        if (baseFragment == null || !AndroidUtilities.isActivityRunning(baseFragment.getParentActivity()) || TextUtils.isEmpty(pluginInstallParams.filePath)) {
            return;
        }
        File file = new File(pluginInstallParams.filePath);
        if (!NekoConfig.pluginsEngine) {
            BulletinFactory.of(baseFragment).createSimpleBulletin(R.raw.error, LocaleController.formatString(R.string.PluginNotEnabled, file.getName()), LocaleController.getString(R.string.Enable), 2750, () -> baseFragment.presentFragment(new PluginsActivity())).show();
            return;
        }
        PluginsEngine pluginEngine = getPluginEngine(file);
        if (pluginEngine == null) {
            return;
        }
        pluginEngine.showInstallDialog(baseFragment, pluginInstallParams);
    }

    public void loadPluginSettings() {
        loadPluginSettings(null);
    }

    public void loadPluginSettings(final String str) {
        if (TextUtils.isEmpty(str)) {
            for (String str2 : this.plugins.keySet()) {
                Plugin plugin = this.plugins.get(str2);
                if (plugin != null && plugin.isEnabled() && plugin.getError() == null) {
                    loadPluginSettings(str2);
                } else if (plugin != null) {
                    invalidatePluginSettings(str2);
                }
            }
            return;
        }
        Utilities.pluginsQueue.postRunnable(() -> {
            try {
                PluginsEngine pluginEngine = getPluginEngine(str);
                if (pluginEngine == null) {
                    return;
                }
                List<SettingItem> listLoadPluginSettings = pluginEngine.loadPluginSettings(str);
                if (listLoadPluginSettings == null) {
                    invalidatePluginSettings(str);
                    return;
                }
                this.settings.put(str, listLoadPluginSettings);
                FileLog.d("Registered settings for plugin " + str);
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.pluginSettingsRegistered, str));
            } catch (Throwable th) {
                FileLog.e(th);
                invalidatePluginSettings(str);
            }
        });
    }

    public boolean hasPluginSettings(String str) {
        return !TextUtils.isEmpty(str) && this.settings.containsKey(str);
    }

    public void invalidatePluginSettings(final String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        this.settings.remove(str);
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.pluginSettingsUnregistered, str));
    }

    public void clearPluginSettingsPreferences(String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        PluginsEngine pluginEngine = getPluginEngine(str);
        if (pluginEngine != null) {
            pluginEngine.clearPluginSettings(str);
        }
        if (this.preferences == null) {
            return;
        }
        String str2 = PREF_PLUGIN_ENABLED_KEY_PREFIX + str;
        if (this.preferences.contains(str2)) {
            this.preferences.edit().remove(str2).apply();
        }
    }

    public Map<String, ?> getPluginSettingsPreferences(String str) {
        PluginsEngine pluginEngine = getPluginEngine(str);
        if (pluginEngine != null) {
            return pluginEngine.getAllPluginSettings(str);
        }
        return null;
    }

    public boolean hasPluginSettingsPreferences(String str) {
        Map<String, ?> pluginSettingsPreferences = getPluginSettingsPreferences(str);
        return (pluginSettingsPreferences != null && !pluginSettingsPreferences.isEmpty());
    }

    public boolean getPluginSettingBoolean(String str, String str2, boolean z) {
        PluginsEngine pluginEngine = getPluginEngine(str);
        if (pluginEngine != null) {
            Object pluginSetting = pluginEngine.getPluginSetting(str, str2, Boolean.valueOf(z));
            if (pluginSetting instanceof Boolean) {
                return ((Boolean) pluginSetting).booleanValue();
            }
        }
        return z;
    }

    public String getPluginSettingString(String str, String str2, String str3) {
        Object pluginSetting;
        PluginsEngine pluginEngine = getPluginEngine(str);
        return (pluginEngine == null || (pluginSetting = pluginEngine.getPluginSetting(str, str2, str3)) == null) ? str3 : pluginSetting.toString();
    }

    public int getPluginSettingInt(String str, String str2, int i) {
        PluginsEngine pluginEngine = getPluginEngine(str);
        if (pluginEngine != null) {
            Object pluginSetting = pluginEngine.getPluginSetting(str, str2, Integer.valueOf(i));
            if (pluginSetting instanceof Number) {
                return ((Number) pluginSetting).intValue();
            }
        }
        return i;
    }

    public void setPluginSetting(String str, String str2, Object obj) {
        PluginsEngine pluginEngine = getPluginEngine(str);
        if (pluginEngine != null) {
            pluginEngine.setPluginSetting(str, str2, obj);
            loadPluginSettings(str);
        }
    }

    private void addHook(String str, HookRecord hookRecord, String str2) {
        if (TextUtils.isEmpty(str) || hookRecord == null) {
            return;
        }
        if (this.hooks.computeIfAbsent(str, k -> new CopyOnWriteArraySet<>()).add(hookRecord)) {
            FileLog.d(str2);
            this.interestedPluginsCache.clear();
            this.hooksCacheDirty = true;
        }
    }

    public void addEventHook(String str, String str2, boolean z, int i) {
        addHook(str, new EventHookRecord(str, str2, z, i), "Added event hook '" + str2 + "' for plugin " + str);
    }

    private void removeHook(String str, java.util.function.Predicate<HookRecord> predicate, String str2) {
        Set<HookRecord> set;
        if (TextUtils.isEmpty(str) || (set = this.hooks.get(str)) == null || set.isEmpty()) {
            return;
        }
        List<HookRecord> toRemove = set.stream().filter(predicate).collect(Collectors.toList());
        
        if (toRemove.isEmpty()) {
            return;
        }
        
        toRemove.forEach(HookRecord::cleanup);
        
        set.removeAll(toRemove);
        
        if (set.isEmpty()) {
            this.hooks.remove(str);
        }

        FileLog.d(str2);
        this.interestedPluginsCache.clear();
        this.hooksCacheDirty = true;
    }

    public void removeEventHook(String str, final String str2) {
        removeHook(str, hookRecord -> (hookRecord instanceof EventHookRecord) && java.util.Objects.equals(((EventHookRecord) hookRecord).getHookName(), str2), "Removed event hook(s) matching name '" + str2 + "' for plugin " + str);
    }

    public void addXposedHook(String str, XC_MethodHook.Unhook unhook) {
        addHook(str, new XposedHookRecord(unhook), "Added Xposed hook for plugin " + str);
    }

    public void addXposedHooks(String str, ArrayList<XC_MethodHook.Unhook> arrayList) {
        if (arrayList == null) {
            return;
        }
        for (XC_MethodHook.Unhook unhook : arrayList) {
            addXposedHook(str, unhook);
        }
    }

    public void removeXposedHook(String str, final XC_MethodHook.Unhook unhook) {
        removeHook(str, hookRecord -> (hookRecord instanceof XposedHookRecord) && hookRecord.matches(unhook), "Removed Xposed hook for plugin " + str);
    }

    public void removeHooksByPluginId(String str) {
        Set<HookRecord> setRemove;
        if (TextUtils.isEmpty(str) || (setRemove = this.hooks.remove(str)) == null) {
            return;
        }
        for (HookRecord hookRecord : setRemove) {
            hookRecord.cleanup();
        }
        FileLog.d("Removed all (" + setRemove.size() + ") hooks for plugin " + str);
        this.interestedPluginsCache.clear();
        this.hooksCacheDirty = true;
    }

    public String addMenuItem(String str, PyObject pyObject) {
        if (isPluginEngineAvailable() && pyObject != null) {
            try {
                final MenuItemRecord menuItemRecord = new MenuItemRecord(str, pyObject);
                if (menuItemRecord.menuType == null) {
                    return null;
                }
                MenuItemRecord menuItemRecord2 = this.menuItemsById.get(menuItemRecord.itemId);
                if (menuItemRecord2 != null && !menuItemRecord2.pluginId.equals(str)) {
                    FileLog.w(String.format("Plugin %s tried to add a menu item: %s, which is already used by plugin %s", str, menuItemRecord.itemId, menuItemRecord2.pluginId));
                    return null;
                }
                this.menuItemsById.put(menuItemRecord.itemId, menuItemRecord);
                
                this.menuItemsByMenuType.compute(menuItemRecord.menuType, (key, list) -> {
                     CopyOnWriteArrayList<MenuItemRecord> newList = list == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(list);
                     newList.removeIf(item -> item.itemId.equals(menuItemRecord.itemId));
                     newList.add(menuItemRecord);
                     Collections.sort(newList, (o1, o2) -> Integer.compare(o2.priority, o1.priority));
                     return newList;
                });

                FileLog.d("Added menu item: " + menuItemRecord.itemId + " for plugin " + str + " in type " + menuItemRecord.menuType);
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.pluginMenuItemsUpdated));
                return menuItemRecord.itemId;
            } catch (Exception unused) {
            }
        }
        return null;
    }

    public boolean removeMenuItem(String str, String str2) {
        MenuItemRecord menuItemRecordRemove;
        if (TextUtils.isEmpty(str2) || (menuItemRecordRemove = this.menuItemsById.remove(str2)) == null || menuItemRecordRemove.menuType == null) {
            return false;
        }
        if (!menuItemRecordRemove.pluginId.equals(str)) {
            this.menuItemsById.put(str2, menuItemRecordRemove);
            return false;
        }
        CopyOnWriteArrayList<MenuItemRecord> copyOnWriteArrayList = this.menuItemsByMenuType.get(menuItemRecordRemove.menuType);
        if (copyOnWriteArrayList != null) {
            copyOnWriteArrayList.remove(menuItemRecordRemove);
        }
        FileLog.d("Removed menu item: " + str2 + " for plugin " + str);
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.pluginMenuItemsUpdated));
        return true;
    }

    public void removeMenuItemsByPluginId(String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        List<String> toRemove = new ArrayList<>();
        for (MenuItemRecord menuItemRecord : this.menuItemsById.values()) {
            if (menuItemRecord.pluginId.equals(str)) {
                toRemove.add(menuItemRecord.itemId);
            }
        }
        for (String itemId : toRemove) {
            removeMenuItem(str, itemId);
        }
        FileLog.d("Removed all menu items for plugin: " + str);
    }

    public java.util.List<MenuItemRecord> getMenuItemsForLocation(String str, MenuContextBuilder menuContextBuilder) {
        if (menuContextBuilder == null) {
            return getMenuItemsForLocation(str, new HashMap<>());
        }
        return getMenuItemsForLocation(str, menuContextBuilder.build());
    }

    public java.util.List<MenuItemRecord> getMenuItemsForLocation(String str, Map<String, Object> map) {
        if (!isPluginEngineAvailable() || TextUtils.isEmpty(str)) {
            return Collections.emptyList();
        }
        CopyOnWriteArrayList<MenuItemRecord> copyOnWriteArrayList = this.menuItemsByMenuType.get(str);
        if (copyOnWriteArrayList == null || copyOnWriteArrayList.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<MenuItemRecord> arrayList = new ArrayList<>();
        for (MenuItemRecord menuItemRecord : copyOnWriteArrayList) {
            Plugin plugin = this.plugins.get(menuItemRecord.pluginId);
            if (plugin != null && plugin.isEnabled() && !plugin.hasError() && menuItemRecord.checkCondition(map)) {
                arrayList.add(menuItemRecord);
            }
        }
        return arrayList;
    }

    void notifyPluginsChanged() {
        AndroidUtilities.cancelRunOnUIThread(this.updateNotificationRunnable);
        AndroidUtilities.runOnUIThread(this.updateNotificationRunnable, 150L);
    }

    public void executeOnAppEvent(final String str) {
        if (isPluginEngineAvailable()) {
            FileLog.d("Execute scripts on app event " + str);
            engines.values().forEach(engine -> engine.executeOnAppEvent(str));
        }
    }

    java.util.List<String> getInterestedPluginIds(String str) {
        if (TextUtils.isEmpty(str)) {
            return Collections.emptyList();
        }
        java.util.List<String> list = this.interestedPluginsCache.get(str);
        if (list == null) {
            rebuildHooksCacheIfNeeded();
            HashMap<String, Integer> map = new HashMap<>();
            java.util.List<EventHookRecord> list2 = this.exactMatchEventHooksCache.get(str);
            if (list2 != null) {
                for (final EventHookRecord eventHookRecord : list2) {
                    map.merge(eventHookRecord.getPluginId(), eventHookRecord.getPriority(), Integer::max);
                }
            }
            for (final EventHookRecord eventHookRecord2 : this.substringMatchEventHooksCache) {
                if (eventHookRecord2.matches(str)) {
                     map.merge(eventHookRecord2.getPluginId(), eventHookRecord2.getPriority(), Integer::max);
                }
            }
            if (map.isEmpty()) {
                list = Collections.emptyList();
            } else {
                list = map.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                        .map(Map.Entry::getKey)
                        .filter(id -> {
                            Plugin plugin = this.plugins.get(id);
                            return plugin != null && plugin.isEnabled() && !plugin.hasError();
                        })
                        .collect(Collectors.toList());
            }
            this.interestedPluginsCache.put(str, list);
        }
        return list;
    }

    private void rebuildHooksCacheIfNeeded() {
        if (this.hooksCacheDirty) {
            synchronized (this.hooksCacheLock) {
                if (this.hooksCacheDirty) {
                    Map<String, List<EventHookRecord>> map = new HashMap<>();
                    List<EventHookRecord> arrayList = new ArrayList<>();
                    
                    for (Set<HookRecord> set : this.hooks.values()) {
                        for (HookRecord hookRecord : set) {
                            if (hookRecord instanceof EventHookRecord) {
                                EventHookRecord eventHookRecord = (EventHookRecord) hookRecord;
                                if (eventHookRecord.isMatchSubstring()) {
                                    arrayList.add(eventHookRecord);
                                } else {
                                    map.computeIfAbsent(eventHookRecord.getHookName(), k -> new ArrayList<>()).add(eventHookRecord);
                                }
                            }
                        }
                    }
                    this.exactMatchEventHooksCache = map;
                    this.substringMatchEventHooksCache = arrayList;
                    this.hooksCacheDirty = false;
                }
            }
        }
    }

    @Override
    public TLObject executePreRequestHook(String str, int i, TLObject tLObject) {
        if (isPluginEngineAvailable()) {
            java.util.List<String> interestedPluginIds = getInterestedPluginIds(str);
            if (!interestedPluginIds.isEmpty()) {
                for (String str2 : interestedPluginIds) {
                    PluginsEngine pluginEngine = getPluginEngine(str2);
                    if (pluginEngine != null) {
                        HookResult<TLObject> hookResultExecutePreRequestHook = pluginEngine.executePreRequestHook(str, i, tLObject, str2);
                        TLObject tLObject2 = hookResultExecutePreRequestHook.result;
                        if (hookResultExecutePreRequestHook.cancel) {
                            return null;
                        }
                        if (hookResultExecutePreRequestHook.isFinal) {
                            return tLObject2;
                        }
                        tLObject = tLObject2;
                    }
                }
                return tLObject;
            }
        }
        return tLObject;
    }

    @Override
    public PluginsHooks.PostRequestResult executePostRequestHook(String str, int i, TLObject tLObject, TLRPC.TL_error tL_error) {
        if (!isPluginEngineAvailable()) {
            return new PluginsHooks.PostRequestResult(tLObject, tL_error);
        }
        java.util.List<String> interestedPluginIds = getInterestedPluginIds(str);
        if (interestedPluginIds.isEmpty()) {
            return new PluginsHooks.PostRequestResult(tLObject, tL_error);
        }
        TLObject tLObject2 = tLObject;
        TLRPC.TL_error tL_error2 = tL_error;
        for (String str2 : interestedPluginIds) {
            PluginsEngine pluginEngine = getPluginEngine(str2);
            if (pluginEngine != null) {
                HookResult<PluginsHooks.PostRequestResult> hookResultExecutePostRequestHook = pluginEngine.executePostRequestHook(str, i, tLObject2, tL_error2, str2);
                PluginsHooks.PostRequestResult postRequestResult = hookResultExecutePostRequestHook.result;
                TLObject tLObject3 = postRequestResult.response;
                TLRPC.TL_error tL_error3 = postRequestResult.error;
                if (hookResultExecutePostRequestHook.cancel) {
                    return null;
                }
                if (hookResultExecutePostRequestHook.isFinal) {
                    return new PluginsHooks.PostRequestResult(tLObject3, tL_error3);
                }
                tL_error2 = tL_error3;
                tLObject2 = tLObject3;
            }
        }
        return new PluginsHooks.PostRequestResult(tLObject2, tL_error2);
    }

    @Override
    public TLRPC.Update executeUpdateHook(String str, int i, TLRPC.Update update) {
        if (isPluginEngineAvailable()) {
            java.util.List<String> interestedPluginIds = getInterestedPluginIds(str);
            if (!interestedPluginIds.isEmpty()) {
                for (String str2 : interestedPluginIds) {
                    PluginsEngine pluginEngine = getPluginEngine(str2);
                    if (pluginEngine != null) {
                        HookResult<TLRPC.Update> hookResultExecuteUpdateHook = pluginEngine.executeUpdateHook(str, i, update, str2);
                        TLRPC.Update update2 = hookResultExecuteUpdateHook.result;
                        if (hookResultExecuteUpdateHook.cancel) {
                            return null;
                        }
                        if (hookResultExecuteUpdateHook.isFinal) {
                            return update2;
                        }
                        update = update2;
                    }
                }
                return update;
            }
        }
        return update;
    }

    @Override
    public TLRPC.Updates executeUpdatesHook(String str, int i, TLRPC.Updates updates) {
        if (isPluginEngineAvailable()) {
            java.util.List<String> interestedPluginIds = getInterestedPluginIds(str);
            if (!interestedPluginIds.isEmpty()) {
                for (String str2 : interestedPluginIds) {
                    PluginsEngine pluginEngine = getPluginEngine(str2);
                    if (pluginEngine != null) {
                        HookResult<TLRPC.Updates> hookResultExecuteUpdatesHook = pluginEngine.executeUpdatesHook(str, i, updates, str2);
                        TLRPC.Updates updates2 = hookResultExecuteUpdatesHook.result;
                        if (hookResultExecuteUpdatesHook.cancel) {
                            return null;
                        }
                        if (hookResultExecuteUpdatesHook.isFinal) {
                            return updates2;
                        }
                        updates = updates2;
                    }
                }
                return updates;
            }
        }
        return updates;
    }

    @Override
    public SendMessagesHelper.SendMessageParams executeSendMessageHook(int i, SendMessagesHelper.SendMessageParams sendMessageParams) {
        if (isPluginEngineAvailable()) {
            java.util.List<String> interestedPluginIds = getInterestedPluginIds(PluginsConstants.SEND_MESSAGE_HOOK);
            if (!interestedPluginIds.isEmpty()) {
                for (String str : interestedPluginIds) {
                    PluginsEngine pluginEngine = getPluginEngine(str);
                    if (pluginEngine != null) {
                        HookResult<SendMessagesHelper.SendMessageParams> hookResultExecuteSendMessageHook = pluginEngine.executeSendMessageHook(i, sendMessageParams, str);
                        SendMessagesHelper.SendMessageParams sendMessageParams2 = hookResultExecuteSendMessageHook.result;
                        if (hookResultExecuteSendMessageHook.cancel) {
                            return null;
                        }
                        if (hookResultExecuteSendMessageHook.isFinal) {
                            return sendMessageParams2;
                        }
                        sendMessageParams = sendMessageParams2;
                    }
                }
                return sendMessageParams;
            }
        }
        return sendMessageParams;
    }

    private static class SingletonHolder {
        private static final PluginsController INSTANCE = new PluginsController();
    }

    public static class HookResult<T> {
        public boolean cancel;
        public boolean isFinal;
        public T result;

        public HookResult(T t, boolean z, boolean z2) {
            this.result = t;
            this.cancel = z;
            this.isFinal = z2;
        }
    }

    public static class PluginValidationResult {
        public String error;
        public Plugin plugin;

        public PluginValidationResult(Plugin plugin, String str) {
            this.plugin = plugin;
            this.error = str;
        }
    }
}