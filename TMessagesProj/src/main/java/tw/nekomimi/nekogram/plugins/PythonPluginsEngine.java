package tw.nekomimi.nekogram.plugins;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import androidx.core.content.FileProvider;
import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.plugins.hooks.PluginsHooks;
import tw.nekomimi.nekogram.plugins.models.DividerSetting;
import tw.nekomimi.nekogram.plugins.models.EditTextSetting;
import tw.nekomimi.nekogram.plugins.models.HeaderSetting;
import tw.nekomimi.nekogram.plugins.models.InputSetting;
import tw.nekomimi.nekogram.plugins.models.SelectorSetting;
import tw.nekomimi.nekogram.plugins.models.SettingItem;
import tw.nekomimi.nekogram.plugins.models.SwitchSetting;
import tw.nekomimi.nekogram.plugins.models.TextSetting;
import tw.nekomimi.nekogram.plugins.ui.PluginSettingsActivity;
import tw.nekomimi.nekogram.plugins.ui.components.InstallPluginBottomSheet;
import tw.nekomimi.nekogram.plugins.ui.components.PluginCell;
import tw.nekomimi.nekogram.plugins.utils.PyObjectUtils;
import tw.nekomimi.nekogram.utils.text.LocaleUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

public class PythonPluginsEngine implements PluginsController.PluginsEngine {
    public PyObject basePluginClass;
    public PyObject debuggerListener;
    private PyObject devServerClass;
    private volatile Python python;
    public final ConcurrentHashMap<String, PyObject> pluginInstances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> settingsCache = new ConcurrentHashMap<>();

    @FunctionalInterface
    interface PyMethodCaller<T> {
        PyObject call(PyObject pyObject, T t);
    }

    @Override
    public boolean canOpenInExternalApp() {
        return true; // я так не понял что это за хуйня и для чего она, но в декомпиле это так..
    }

    private PluginsController getPluginsController() {
        return PluginsController.getInstance();
    }

    private Python getPython() {
        if (this.python == null) {
            initPython();
            if (this.python == null) {
                FileLog.e("Python initialization failed, unable to proceed.");
                return null;
            }
            try {
                this.basePluginClass = this.python.getModule("base_plugin").get("BasePlugin");
            } catch (PyException e) {
                FileLog.e("Failed to load BasePlugin class", e);
            }
        }
        return this.python;
    }

    private void initPython() {
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(ApplicationLoader.applicationContext));
            }
            this.python = Python.getInstance();
        } catch (Exception e) {
            FileLog.e("Failed to initialize Python", e);
        }
    }

    @Override
    public boolean isPlugin(File file) {
        return file != null && file.getName().toLowerCase().endsWith(PluginsConstants.PLUGINS_EXT);
    }

    @Override
    public boolean isEngineAvailable() {
        return getPython() != null && Python.isStarted();
    }

    @Override
    public void init(Runnable runnable) {
        if (getPython() == null) {
            if (runnable != null) {
                runnable.run();
                return;
            }
            return;
        }
        try {
            String[] strArr = getPython().getModule("plugin_settings").callAttr("init", getPluginsController().pluginsDir.getAbsolutePath(), getPluginsController().preferences.getAll()).toJava(String[].class);
            if (strArr.length > 0) {
                SharedPreferences.Editor editorEdit = getPluginsController().preferences.edit();
                for (String str : strArr) {
                    editorEdit.remove(str);
                }
                editorEdit.apply();
                FileLog.d("Migrated " + strArr.length + " plugin settings from SharedPreferences to JSON.");
            }
        } catch (PyException e) {
            FileLog.e("Failed to initialize plugin_settings module", e);
        }
        loadPlugins(runnable);
        checkDevServer();
    }

    @Override
    public void checkDevServer() {
        if (NekoConfig.pluginsDevMode) {
            runDevServer();
        } else {
            stopDevServer();
        }
    }

    private void runDevServer() {
        if (getPython() == null) {
            return;
        }
        if (this.devServerClass != null) {
            stopDevServer();
        }
        try {
            PyObject pyObject = getPython().getModule(PluginsConstants.DevServer.MODULE).get(PluginsConstants.DevServer.CLASS);
            this.devServerClass = pyObject;
            if (pyObject == null) {
                return;
            }
            pyObject.callAttrThrows(PluginsConstants.DevServer.START_SERVER);
            FileLog.d("Dev server started successfully.");
        } catch (Throwable th) {
            FileLog.e("Failed to initialize dev server", th);
            this.devServerClass = null;
        }
    }

    private void stopDevServer() {
        PyObject pyObject = this.devServerClass;
        if (pyObject == null) {
            return;
        }
        try {
            pyObject.callAttrThrows(PluginsConstants.DevServer.STOP_SERVER);
            FileLog.d("Dev server stopped successfully.");
        } catch (Throwable th) {
            try {
                FileLog.e("Failed to stop dev server", th);
            } finally {
                this.devServerClass = null;
            }
        }
    }

    @Override
    public void shutdown(Runnable runnable) {
        if (getPython() == null) {
            if (runnable != null) {
                runnable.run();
                return;
            }
            return;
        }
        try {
            Iterator<String> it = this.pluginInstances.keySet().iterator();
            while (it.hasNext()) {
                unloadPlugin(it.next());
            }
            PyObject pyObject = this.debuggerListener;
            if (pyObject != null) {
                pyObject.close();
                this.debuggerListener = null;
            }
            this.pluginInstances.clear();
            this.python = null;
            FileLog.d("Python plugin engine shut down.");
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (runnable != null) {
            runnable.run();
        }
    }

    public void loadPlugins(final Runnable runnable) {
        Utilities.pluginsQueue.postRunnable(() -> {
            Plugin plugin;
            if (getPython() == null) {
                if (runnable != null) {
                    AndroidUtilities.runOnUIThread(runnable);
                    return;
                }
                return;
            }
            try {
                PyObject module = getPython().getModule("sys");
                try {
                    PyObject pyObject = module.get("path");
                    if (pyObject != null) {
                         pyObject.callAttr("append", getPluginsController().pluginsDir.getAbsolutePath());
                    }
                    module.callAttr("setswitchinterval", 0.001d);
                    if (pyObject != null) {
                        pyObject.close();
                    }
                    module.close();
                    File[] fileArrListFiles = getPluginsController().pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".py"));
                    if (fileArrListFiles == null) {
                        getPluginsController().notifyPluginsChanged();
                        if (runnable != null) {
                            AndroidUtilities.runOnUIThread(runnable);
                            return;
                        }
                        return;
                    }
                    for (File file : fileArrListFiles) {
                        String strSubstring = file.getName().substring(0, file.getName().length() - 3);
                        PluginsController.PluginValidationResult pluginValidationResultValidatePluginFromFile = null;
                        try {
                            pluginValidationResultValidatePluginFromFile = validatePluginFromFile(file.getAbsolutePath());
                            if (pluginValidationResultValidatePluginFromFile.error != null) {
                                throw new Exception(pluginValidationResultValidatePluginFromFile.error);
                            }
                            loadPlugin(strSubstring, file.getAbsolutePath(), pluginValidationResultValidatePluginFromFile.plugin);
                        } catch (Throwable th) {
                            FileLog.e("Failed to load plugin " + file.getName() + ". Reason: " + th.getMessage(), th);
                            if (pluginValidationResultValidatePluginFromFile == null || (plugin = pluginValidationResultValidatePluginFromFile.plugin) == null) {
                                plugin = new Plugin(strSubstring, strSubstring);
                                plugin.setAuthor(LocaleController.getString(R.string.PluginNoAuthor));
                                plugin.setVersion("1.0");
                                plugin.setEngine(PluginsConstants.PYTHON);
                            }
                            plugin.setError(th);
                            plugin.setEnabled(false);
                            getPluginsController().plugins.put(strSubstring, plugin);
                        }
                    }
                    getPluginsController().notifyPluginsChanged();
                    long enabledCount = getPluginsController().plugins.values().stream().filter(p -> p.isEnabled() && !p.hasError()).count();
                    FileLog.d("Python plugin system initialized. Total: " + getPluginsController().plugins.size() + ", Enabled: " + enabledCount);
                    if (runnable != null) {
                        AndroidUtilities.runOnUIThread(runnable);
                    }
                } finally {
                }
            } catch (PyException e) {
                FileLog.e("Failed to setup Python environment for plugins", e);
                if (runnable != null) {
                    AndroidUtilities.runOnUIThread(runnable);
                }
            }
        });
    }

    public void loadPlugin(String str, String str2) throws Exception {
        loadPlugin(str, str2, null);
    }

    public void loadPlugin(String str, String str2, Plugin plugin) throws Exception {
        boolean z = getPluginsController().preferences.getBoolean("plugin_enabled_" + str, false);
        File file = new File(str2);
        if (!file.exists() || !file.isFile()) {
            throw new Exception("Plugin file not found: " + str2);
        }
        if (plugin == null) {
            PluginsController.PluginValidationResult pluginValidationResultValidatePluginFromFile = validatePluginFromFile(str2);
            if (pluginValidationResultValidatePluginFromFile.error != null) {
                throw new Exception(pluginValidationResultValidatePluginFromFile.error);
            }
            plugin = pluginValidationResultValidatePluginFromFile.plugin;
        }
        if (!str.equals(plugin.getId())) {
            throw new Exception(String.format("Plugin ID mismatch. Expected: %s, but found: %s in metadata.", str, plugin.getId()));
        }
        if (this.pluginInstances.containsKey(str)) {
            unloadPlugin(str);
        }
        try {
            PyObject pyObjectFindPluginClass = findPluginClass(getPython().getModule(str));
            if (pyObjectFindPluginClass == null) {
                throw new Exception("Could not find a class inheriting from BasePlugin in " + str + ".py. Make sure your main plugin class extends BasePlugin.");
            }
            PyObject pyObjectCall = pyObjectFindPluginClass.call();
            pyObjectCall.put("id", plugin.getId());
            pyObjectCall.put("name", plugin.getName());
            pyObjectCall.put("description", plugin.getDescription());
            pyObjectCall.put("author", plugin.getAuthor());
            pyObjectCall.put("version", plugin.getVersion());
            pyObjectCall.put("icon", plugin.getIcon());
            pyObjectCall.put("min_version", plugin.getMinVersion());
            pyObjectCall.put("enabled", false);
            pyObjectCall.put("initialized", false);
            pyObjectCall.put("error_message", null);
            getPluginsController().plugins.put(str, plugin);
            this.pluginInstances.put(str, pyObjectCall);
            if (z) {
                setPluginEnabled(str, true, null);
            }
        } catch (PyException e) {
            throw new Exception("Failed to import plugin module: " + e.getMessage(), e);
        }
    }

    private PyObject findPluginClass(PyObject pyObject) {
        if (this.basePluginClass == null) {
            FileLog.e("BasePlugin class is not loaded, cannot find plugin class in " + pyObject.get("__name__"));
            return null;
        }
        try {
            PyObject builtins = getPython().getBuiltins();
            PyObject pyObject2 = pyObject.get("__dict__");
            if (pyObject2 == null) {
                return null;
            }
            for (PyObject pyObject3 : pyObject2.asMap().values()) {
                if (builtins.callAttr("isinstance", pyObject3, builtins.get(PluginsConstants.Settings.TYPE)).toBoolean() && !pyObject3.equals(this.basePluginClass) && builtins.callAttr("issubclass", pyObject3, this.basePluginClass).toBoolean()) {
                    return pyObject3;
                }
            }
        } catch (PyException e) {
            FileLog.e("Error while searching for a BasePlugin subclass in module " + pyObject.get("__name__"), e);
        }
        return null;
    }

    public void unloadPlugin(String str) {
        this.settingsCache.remove(str);
        try {
            PyObject pyObjectRemove = this.pluginInstances.remove(str);
            if (pyObjectRemove == null) {
                if (pyObjectRemove != null) {
                    pyObjectRemove.close();
                    return;
                }
                return;
            }
            try {
                if (PyObjectUtils.getBoolean(pyObjectRemove, "initialized", false)) {
                    try {
                        pyObjectRemove.callAttr(PluginsConstants.ON_PLUGIN_UNLOAD);
                    } catch (Throwable th) {
                        FileLog.e("Error during on_plugin_unload for " + str, th);
                    }
                }
                getPluginsController().cleanupPlugin(str);
                PyObject pyObject = getPython().getModule("sys").get("modules");
                if (pyObject != null && pyObject.callAttr("get", str) != null) {
                    pyObject.callAttr("pop", str);
                }
                pyObjectRemove.close();
            } finally {
            }
        } catch (PyException e) {
            FileLog.e("Failed to remove module " + str + " from sys.modules", e);
        }
    }

    @Override
    public void setPluginEnabled(String str, boolean z, final Utilities.Callback<String> callback) {
        try {
            Plugin plugin = getPluginsController().plugins.get(str);
            PyObject pyObject = this.pluginInstances.get(str);
            if (plugin == null || pyObject == null) {
                throw new Exception("Plugin not found: " + str);
            }
            if (PyObjectUtils.getBoolean(pyObject, "initialized", false) == z && !plugin.hasError()) {
                if (callback != null) {
                    callback.run(null);
                    return;
                }
                return;
            }
            if (z) {
                getPluginsController().cleanupPlugin(str);
                pyObject.callAttr(PluginsConstants.ON_PLUGIN_LOAD);
                pyObject.put("initialized", true);
                pyObject.put("error_message", null);
                plugin.setError(null);
            } else {
                if (PyObjectUtils.getBoolean(pyObject, "initialized", false)) {
                    try {
                        pyObject.callAttr(PluginsConstants.ON_PLUGIN_UNLOAD);
                    } catch (Throwable th) {
                        FileLog.e("Error during on_plugin_unload for " + str, th);
                    }
                }
                pyObject.put("initialized", false);
                getPluginsController().cleanupPlugin(str);
            }
            plugin.setEnabled(z);
            pyObject.put("enabled", z);
            getPluginsController().preferences.edit().putBoolean("plugin_enabled_" + str, z).apply();
            if (z) {
                getPluginsController().loadPluginSettings(str);
            } else {
                getPluginsController().invalidatePluginSettings(str);
            }
            getPluginsController().notifyPluginsChanged();
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
            }
        } catch (Throwable th2) {
            FileLog.e("Unexpected error setting enabled state for " + str, th2);
            if (z) {
                Plugin plugin2 = getPluginsController().plugins.get(str);
                if (plugin2 != null) {
                    plugin2.setEnabled(false);
                    plugin2.setError(th2);
                }
                PyObject pyObject2 = this.pluginInstances.get(str);
                if (pyObject2 != null) {
                    pyObject2.put("enabled", false);
                    pyObject2.put("error_message", th2.getMessage());
                }
                getPluginsController().preferences.edit().putBoolean("plugin_enabled_" + str, false).apply();
                getPluginsController().cleanupPlugin(str);
            }
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(PluginCell.stackTraceToString(th2)));
            }
        }
    }

    @Override
    public void deletePlugin(String str, final Utilities.Callback<String> callback) {
        if (this.pluginInstances.containsKey(str)) {
            unloadPlugin(str);
        }
        getPluginsController().plugins.remove(str);
        File file = new File(getPluginsController().pluginsDir, str + ".py");
        if (file.exists()) {
            file.delete();
        }
        if (PluginsController.isPluginPinned(str)) {
            PluginsController.setPluginPinned(str, false);
        }
        getPluginsController().clearPluginSettingsPreferences(str);
        getPluginsController().notifyPluginsChanged();
        if (callback != null) {
            AndroidUtilities.runOnUIThread(() -> callback.run(null));
        }
    }

    @Override
    public String getPluginPath(String str) {
        return getPluginsController().pluginsDir.getAbsolutePath() + File.separator + str + ".py";
    }

    @Override
    public void openInExternalApp(String str) {
        BaseFragment safeLastFragment = LaunchActivity.getSafeLastFragment();
        if (safeLastFragment == null) {
            return;
        }
        File file = new File(getPluginPath(str));
        if (file.exists()) {
            AndroidUtilities.openForView(file, file.getName(), "text/plain", safeLastFragment.getParentActivity(), safeLastFragment.getResourceProvider(), false);
        }
    }

    @Override
    public void sharePlugin(String str) {
        BaseFragment safeLastFragment = LaunchActivity.getSafeLastFragment();
        if (safeLastFragment == null) {
            return;
        }
        String pluginPath = getPluginPath(str);
        File file = new File(ApplicationLoader.getFilesDirFixed(), "temp");
        if (!file.exists()) {
            file.mkdirs();
        }
        File file2 = new File(file, str + PluginsConstants.PLUGINS_EXT);
        try {
            FileInputStream fileInputStream = new FileInputStream(pluginPath);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                try {
                    fileOutputStream.getChannel().transferFrom(fileInputStream.getChannel(), 0L, fileInputStream.getChannel().size());
                    fileOutputStream.close();
                    fileInputStream.close();
                    Uri uriForFile = FileProvider.getUriForFile(safeLastFragment.getContext(), ApplicationLoader.getApplicationId() + ".provider", file2);
                    Intent intent = new Intent("android.intent.action.SEND");
                    intent.setFlags(1);
                    intent.putExtra("android.intent.extra.STREAM", uriForFile);
                    intent.setType("application/x-plugin");
                    safeLastFragment.startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)), 500);
                    file2.deleteOnExit();
                } finally {
                }
            } finally {
            }
        } catch (IOException | IllegalArgumentException e) {
            FileLog.e(e);
        }
    }

    public void loadPluginFromFile(String str, Plugin plugin, final Utilities.Callback<String> callback) {
        Plugin finalPlugin = plugin;
        if (finalPlugin == null) {
            PluginsController.PluginValidationResult result = validatePluginFromFile(str);
            if (result.error != null) {
                if (callback != null) {
                     AndroidUtilities.runOnUIThread(() -> callback.run(result.error));
                }
                return;
            }
            finalPlugin = result.plugin;
        }
        
        final Plugin p = finalPlugin;
        String id = p.getId();
        File destFile = new File(getPluginsController().getPluginsDir(), id + ".py");
        File backupFile = null;

        try {
            if (destFile.exists()) {
                unloadPlugin(id);
                backupFile = new File(getPluginsController().getPluginsDir(), id + ".py.bak");
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                if (!destFile.renameTo(backupFile)) {
                     throw new IOException("Failed to backup existing plugin file.");
                }
            }

            try (FileInputStream in = new FileInputStream(str);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                 byte[] buf = new byte[1024];
                 int len;
                 while ((len = in.read(buf)) > 0) {
                     out.write(buf, 0, len);
                 }
            }

            loadPlugin(id, destFile.getAbsolutePath(), p);
            if (backupFile != null && backupFile.exists()) {
                backupFile.delete();
            }
            getPluginsController().notifyPluginsChanged();
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
            }

        } catch (Throwable e) {
            FileLog.e("Unexpected error loading plugin from file: " + str, e);
            if (destFile.exists()) {
                destFile.delete();
            }
            if (backupFile != null && backupFile.exists()) {
                if (backupFile.renameTo(destFile)) {
                     try {
                         loadPlugin(id, destFile.getAbsolutePath());
                     } catch (Exception reloadEx) {
                         FileLog.e("Failed to reload original plugin after update failure for " + id, reloadEx);
                     }
                } else {
                     FileLog.e("Failed to restore backup for plugin " + id);
                }
            }
            
            getPluginsController().cleanupPlugin(id);
            getPluginsController().plugins.remove(id);
            PyObject instance = pluginInstances.remove(id);
            if (instance != null) instance.close();
            getPluginsController().clearPluginSettingsPreferences(id);
            getPluginsController().notifyPluginsChanged();
            
            if (callback != null) {
                 AndroidUtilities.runOnUIThread(() -> callback.run(e.getMessage()));
            }
        }
    }

    public PluginsController.PluginValidationResult validatePluginFromFile(String str) {
        if (!new File(str).exists()) {
            return new PluginsController.PluginValidationResult(null, "Plugin file not found.");
        }
        try {
            Map<String, String> pluginMetadata = parsePluginMetadata(str);
            String str2 = pluginMetadata.get("id");
            String str3 = pluginMetadata.get("name");
            if (!TextUtils.isEmpty(str2) && !TextUtils.isEmpty(str3)) {
                if (!str2.matches("^[a-zA-Z][a-zA-Z0-9_-]{1,31}$")) {
                    return new PluginsController.PluginValidationResult(null, "Plugin '__id__' must be 2-32 characters long, start with a letter, and contain only latin letters, numbers, dashes and underscores.");
                }
                String str4 = pluginMetadata.get("min_version");
                if (str4 != null && !SharedConfig.versionBiggerOrEqual(BuildVars.BUILD_VERSION_STRING, str4)) {
                    return new PluginsController.PluginValidationResult(null, "Plugin requires app version " + str4 + " or higher. Current is " + BuildVars.BUILD_VERSION_STRING);
                }
                Plugin plugin = new Plugin(str2, str3);
                plugin.setEngine(PluginsConstants.PYTHON);
                plugin.setAuthor(pluginMetadata.getOrDefault("author", LocaleController.getString(R.string.PluginNoAuthor)));
                plugin.setDescription(pluginMetadata.getOrDefault("description", LocaleController.getString(R.string.PluginNoDescription)));
                plugin.setIcon(pluginMetadata.get("icon"));
                plugin.setVersion(pluginMetadata.getOrDefault("version", "1.0"));
                plugin.setMinVersion(str4);
                plugin.setEnabled(getPluginsController().preferences.getBoolean("plugin_enabled_" + str2, false));
                return new PluginsController.PluginValidationResult(plugin, null);
            }
            return new PluginsController.PluginValidationResult(null, "Plugin metadata must contain non-empty '__id__' and '__name__'.");
        } catch (PyException e) {
            FileLog.e("Failed to parse metadata from " + str + ". Error: " + e.getMessage(), e);
            return new PluginsController.PluginValidationResult(null, e.getMessage());
        } catch (Throwable th) {
            FileLog.e("Unexpected error validating plugin " + str, th);
            return new PluginsController.PluginValidationResult(null, th.getMessage());
        }
    }

    public List<SettingItem> parsePySettingDefinitions(List<PyObject> list) {
        ArrayList<SettingItem> arrayList = new ArrayList<>(list.size());
        for (PyObject pyObject : list) {
            if (pyObject != null) {
                try {
                    SettingItem item = null;
                    String type = PyObjectUtils.getString(pyObject, PluginsConstants.Settings.TYPE, null);
                    if (type == null) {
                        FileLog.w("A setting item in a plugin is missing its 'type'. Skipping.");
                    } else {
                        String key = PyObjectUtils.getString(pyObject, PluginsConstants.Settings.KEY, null);
                        String text = PyObjectUtils.getString(pyObject, "text", null);
                        String subtext = PyObjectUtils.getString(pyObject, "subtext", null);
                        String icon = PyObjectUtils.getString(pyObject, "icon", null);
                        PyObject onChange = pyObject.get(PluginsConstants.Settings.ON_CHANGE);
                        PyObject onLongClick = pyObject.get(PluginsConstants.Settings.ON_LONG_CLICK);
                        String linkAlias = PyObjectUtils.getString(pyObject, PluginsConstants.Settings.LINK_ALIAS, null);
                        PyObject defVal = pyObject.get(PluginsConstants.Settings.DEFAULT);

                        switch (type) {
                            case PluginsConstants.Settings.TYPE_EDIT_TEXT:
                                String hint = PyObjectUtils.getString(pyObject, PluginsConstants.Settings.HINT, null);
                                boolean multiline = PyObjectUtils.getBoolean(pyObject, PluginsConstants.Settings.MULTILINE, false);
                                int maxLength = PyObjectUtils.getInt(pyObject, PluginsConstants.Settings.MAX_LENGTH, 256);
                                String mask = PyObjectUtils.getString(pyObject, PluginsConstants.Settings.MASK, null);
                                if (key != null && hint != null) {
                                    item = new EditTextSetting(key, hint, defVal != null ? defVal.toString() : "", multiline, maxLength, mask, onChange);
                                }
                                break;
                            case PluginsConstants.Settings.TYPE_HEADER:
                                if (text != null) {
                                    item = new HeaderSetting(text);
                                }
                                break;
                            case PluginsConstants.Settings.TYPE_SWITCH:
                                if (key != null && text != null && defVal != null) {
                                    item = new SwitchSetting(key, text, defVal.toBoolean(), subtext, icon, onChange, onLongClick, linkAlias);
                                }
                                break;
                            case PluginsConstants.Settings.TYPE_TEXT:
                                boolean accent = PyObjectUtils.getBoolean(pyObject, PluginsConstants.Settings.ACCENT, false);
                                boolean red = PyObjectUtils.getBoolean(pyObject, PluginsConstants.Settings.RED, false);
                                PyObject onClick = pyObject.get("on_click");
                                PyObject createSubFragment = pyObject.get(PluginsConstants.Settings.CREATE_SUB_FRAGMENT);
                                if (text != null) {
                                    item = new TextSetting(text, icon, accent, red, onClick, createSubFragment, onLongClick, linkAlias);
                                }
                                break;
                            case PluginsConstants.Settings.TYPE_INPUT:
                                if (key != null && text != null) {
                                    item = new InputSetting(key, text, defVal != null ? defVal.toString() : "", subtext, icon, onChange, onLongClick, linkAlias);
                                }
                                break;
                            case PluginsConstants.Settings.TYPE_SELECTOR:
                                String[] items = PyObjectUtils.getStringArray(pyObject, PluginsConstants.Settings.ITEMS, null);
                                if (key != null && text != null && items != null && items.length != 0 && defVal != null) {
                                    item = new SelectorSetting(key, text, defVal.toInt(), items, icon, onChange, onLongClick, linkAlias);
                                }
                                break;
                            case PluginsConstants.Settings.TYPE_DIVIDER:
                                item = new DividerSetting(text);
                                break;
                        }
                        if (item != null) {
                            arrayList.add(item);
                        }
                    }
                } catch (Exception e) { 
                    FileLog.e("Error parsing specific setting item", e);
                }
            }
        }
        return arrayList;
    }

    @Override
    public List<SettingItem> loadPluginSettings(String str) {
        try {
            Plugin plugin = getPluginsController().plugins.get(str);
            PyObject pyObject = this.pluginInstances.get(str);
            if (plugin != null && plugin.isEnabled() && !plugin.hasError() && pyObject != null) {
                PyObject pyObjectCallAttr = pyObject.callAttr(PluginsConstants.CREATE_SETTINGS);
                if (pyObjectCallAttr == null) {
                    return null;
                }
                List<PyObject> listAsList = pyObjectCallAttr.asList();
                if (listAsList.isEmpty()) {
                    return null;
                }
                return parsePySettingDefinitions(listAsList);
            }
            getPluginsController().invalidatePluginSettings(str);
            return null;
        } catch (Exception e) {
            FileLog.e("Failed to load plugin settings", e);
            return null;
        }
    }

    @Override
    public void executeOnAppEvent(String str) {
        PyObject pyObject = getPython().getModule("base_plugin").get("AppEvent");
        if (pyObject == null) {
            return;
        }
        PyObject pyObjectCall = pyObject.call(str);
        try {
            PyObject pyObject2 = this.debuggerListener;
            if (pyObject2 != null) {
                try {
                    pyObject2.callAttr(PluginsConstants.ON_APP_EVENT, pyObjectCall);
                } catch (PyException e) {
                    FileLog.e("Failed to execute app event for debugger listener", e);
                }
            }
            for (PyObject pyObject3 : this.pluginInstances.values()) {
                if (PyObjectUtils.getBoolean(pyObject3, "enabled", false) && PyObjectUtils.getString(pyObject3, "error_message", null) == null) {
                    try {
                        pyObject3.callAttr(PluginsConstants.ON_APP_EVENT, pyObjectCall);
                    } catch (PyException e2) {
                        FileLog.e("Failed to execute app " + str + " for " + PyObjectUtils.getString(pyObject3, "id", null), e2);
                    }
                }
            }
        } finally {
            if (pyObjectCall != null) {
                pyObjectCall.close();
            }
        }
    }

    public <T> PluginsController.HookResult<T> executeHook(PyObject pyObject, T t, Class<T> cls, String str, PyMethodCaller<T> pyMethodCaller, Utilities.Callback<PyException> callback) {
        if (pyObject != null) {
            try {
                PyObject pyObjectCall = pyMethodCaller.call(pyObject, t);
                if (pyObjectCall != null) {
                    String string = PyObjectUtils.getString(pyObjectCall, PluginsConstants.STRATEGY, PluginsConstants.Strategy.DEFAULT);
                    if (string.endsWith(PluginsConstants.Strategy.CANCEL)) {
                        return new PluginsController.HookResult<>(null, true, false);
                    }
                    if (string.endsWith(PluginsConstants.Strategy.MODIFY) || string.endsWith(PluginsConstants.Strategy.MODIFY_FINAL)) {
                        PyObject pyObject2 = pyObjectCall.get(str);
                        if (pyObject2 != null) {
                            t = pyObject2.toJava(cls);
                        }
                        if (string.endsWith(PluginsConstants.Strategy.MODIFY_FINAL)) {
                            return new PluginsController.HookResult<>(t, false, true);
                        }
                    }
                }
            } catch (PyException e) {
                callback.run(e);
            }
        }
        return new PluginsController.HookResult<>(t, false, false);
    }

    private <T> PluginsController.HookResult<T> executeHook(String str, T t, Class<T> cls, String str2, PyMethodCaller<T> pyMethodCaller, Utilities.Callback<PyException> callback) {
        return executeHook(this.pluginInstances.get(str), t, cls, str2, pyMethodCaller, callback);
    }

    @Override
    public PluginsController.HookResult<TLObject> executePreRequestHook(final String str, final int i, TLObject tLObject, final String str2) {
        return executeHook(str2, tLObject, TLObject.class, PluginsConstants.REQUEST, (pyObject, obj) -> pyObject.callAttr("pre_request_hook", str, Integer.valueOf(i), obj), obj -> FileLog.e("Failed to execute pre_request_hook in " + str2 + " for " + str, (PyException) obj));
    }

    public PluginsController.HookResult<PluginsHooks.PostRequestResult> executePostRequestHook(String str, int i, TLObject tLObject, TLRPC.TL_error tL_error, PyObject pyObject) {
        if (pyObject != null) {
            try {
                PyObject pyObjectCallAttr = pyObject.callAttr("post_request_hook", str, Integer.valueOf(i), tLObject, tL_error);
                if (pyObjectCallAttr != null) {
                    String string = PyObjectUtils.getString(pyObjectCallAttr, PluginsConstants.STRATEGY, "");
                    if (string.endsWith(PluginsConstants.Strategy.MODIFY) || string.endsWith(PluginsConstants.Strategy.MODIFY_FINAL)) {
                        PyObject pyObject2 = pyObjectCallAttr.get(PluginsConstants.RESPONSE);
                        if (pyObject2 != null) {
                            tLObject = pyObject2.toJava(TLObject.class);
                        }
                        PyObject pyObject3 = pyObjectCallAttr.get(PluginsConstants.ERROR);
                        if (pyObject3 != null) {
                            tL_error = pyObject3.toJava(TLRPC.TL_error.class);
                        }
                        if (string.endsWith(PluginsConstants.Strategy.MODIFY_FINAL)) {
                            return new PluginsController.HookResult<>(new PluginsHooks.PostRequestResult(tLObject, tL_error), false, true);
                        }
                    }
                }
            } catch (PyException e) {
                FileLog.e("Failed to execute post_request_hook for " + str, e);
            }
        }
        return new PluginsController.HookResult<>(new PluginsHooks.PostRequestResult(tLObject, tL_error), false, false);
    }

    @Override
    public PluginsController.HookResult<PluginsHooks.PostRequestResult> executePostRequestHook(String str, int i, TLObject tLObject, TLRPC.TL_error tL_error, String str2) {
        return executePostRequestHook(str, i, tLObject, tL_error, this.pluginInstances.get(str2));
    }

    @Override
    public PluginsController.HookResult<TLRPC.Update> executeUpdateHook(final String str, final int i, TLRPC.Update update, String str2) {
        return executeHook(str2, update, TLRPC.Update.class, PluginsConstants.UPDATE, (pyObject, obj) -> pyObject.callAttr("on_update_hook", str, Integer.valueOf(i), obj), obj -> FileLog.e("Failed to execute on_update_hook for " + str, (PyException) obj));
    }

    @Override
    public PluginsController.HookResult<TLRPC.Updates> executeUpdatesHook(final String str, final int i, TLRPC.Updates updates, String str2) {
        return executeHook(str2, updates, TLRPC.Updates.class, PluginsConstants.UPDATES, (pyObject, obj) -> pyObject.callAttr("on_updates_hook", str, Integer.valueOf(i), obj), obj -> FileLog.e("Failed to execute on_updates_hook for " + str, (PyException) obj));
    }

    @Override
    public PluginsController.HookResult<SendMessagesHelper.SendMessageParams> executeSendMessageHook(final int i, SendMessagesHelper.SendMessageParams sendMessageParams, final String str) {
        return executeHook(str, sendMessageParams, SendMessagesHelper.SendMessageParams.class, PluginsConstants.PARAMS, (pyObject, obj) -> pyObject.callAttr("on_send_message_hook", Integer.valueOf(i), obj), obj -> FileLog.e("Failed to execute on_send_message_hook for " + str, (PyException) obj));
    }

    public String fetchParameterValue(String str, String str2) {
        if (str == null) {
            return null;
        }
        try {
            File file = new File(str);
            if (file.exists() && file.isFile()) {
                return parsePluginMetadata(str).get(str2);
            }
        } catch (Exception unused) {
        }
        return null;
    }

    public java.util.Map<String, String> parsePluginMetadata(String str) {
        HashMap<String, String> map = new HashMap<>();
        if (str != null) {
            File file = new File(str);
            if (file.exists() && file.isFile()) {
                if (getPython() == null) {
                    FileLog.e("Python engine not initialized, cannot parse metadata for " + str);
                    return map;
                }
                try {
                    PyObject pyObjectCallAttr = getPython().getModule("utils.metadata_parser").callAttr("get_metadata", str);
                    if (pyObjectCallAttr != null) {
                        for (Map.Entry<PyObject, PyObject> entry : pyObjectCallAttr.asMap().entrySet()) {
                            map.put(entry.getKey().toString(), entry.getValue().toString());
                        }
                    }
                } catch (PyException e) {
                    FileLog.e("Failed to parse metadata from " + str + ". Error: " + e.getMessage(), e);
                    throw e;
                }
            }
        }
        return map;
    }

    @Override
    public Object getPluginSetting(String str, String str2, Object obj) {
        Object java2;
        ConcurrentHashMap<String, Object> concurrentHashMap = this.settingsCache.get(str);
        if (concurrentHashMap != null && concurrentHashMap.containsKey(str2)) {
            return concurrentHashMap.get(str2);
        }
        if (getPython() != null) {
            try {
                PyObject pyObjectCallAttr = getPython().getModule("plugin_settings").callAttr("get_setting", str, str2, obj);
                if (pyObjectCallAttr != null) {
                    if (obj instanceof Boolean) {
                        java2 = Boolean.valueOf(pyObjectCallAttr.toBoolean());
                    } else if (obj instanceof Integer) {
                        java2 = Integer.valueOf(pyObjectCallAttr.toInt());
                    } else if (obj instanceof String) {
                        java2 = pyObjectCallAttr.toString();
                    } else if (obj instanceof Float) {
                        java2 = Float.valueOf(pyObjectCallAttr.toFloat());
                    } else if (obj instanceof Long) {
                        java2 = Long.valueOf(pyObjectCallAttr.toLong());
                    } else {
                        java2 = pyObjectCallAttr.toJava(obj.getClass());
                    }
                    this.settingsCache.computeIfAbsent(str, k -> new ConcurrentHashMap<>()).put(str2, java2);
                    return java2;
                }
            } catch (PyException e) {
                FileLog.e("Failed to get plugin setting " + str + "/" + str2, e);
                return obj;
            }
        }
        return obj;
    }

    @Override
    public void setPluginSetting(String str, String str2, Object obj) {
        this.settingsCache.computeIfAbsent(str, k -> new ConcurrentHashMap<>()).put(str2, obj);
        if (getPython() == null) {
            return;
        }
        try {
            getPython().getModule("plugin_settings").callAttr("set_setting", str, str2, obj);
        } catch (PyException e) {
            FileLog.e("Failed to set plugin setting " + str + "/" + str2, e);
        }
    }

    @Override
    public void clearPluginSettings(String str) {
        this.settingsCache.remove(str);
        if (getPython() == null) {
            return;
        }
        try {
            getPython().getModule("plugin_settings").callAttr("clear_settings", str);
        } catch (PyException e) {
            FileLog.e("Failed to clear plugin settings for " + str, e);
        }
    }

    @Override
    public java.util.Map<String, ?> getAllPluginSettings(String str) {
        if (getPython() == null) {
            return null;
        }
        try {
            PyObject pyObjectCallAttr = getPython().getModule("plugin_settings").callAttr("get_all_settings", str);
            if (pyObjectCallAttr != null) {
                HashMap<String, Object> map = new HashMap<>();
                for (Map.Entry<PyObject, PyObject> entry : pyObjectCallAttr.asMap().entrySet()) {
                    if (entry.getKey() != null) {
                        map.put(entry.getKey().toString(), entry.getValue() != null ? entry.getValue().toJava(Object.class) : null);
                    }
                }
                this.settingsCache.put(str, new ConcurrentHashMap<>(map));
                return map;
            }
        } catch (PyException e) {
            FileLog.e("Failed to get all plugin settings for " + str, e);
        }
        return null;
    }

    @Override
    public void showInstallDialog(final BaseFragment baseFragment, InstallPluginBottomSheet.PluginInstallParams pluginInstallParams) {
        File file = new File(pluginInstallParams.filePath);
        final String strFetchParameterValue = fetchParameterValue(pluginInstallParams.filePath, "name");
        final String displayName = (TextUtils.isEmpty(strFetchParameterValue) && file.exists()) ? file.getName() : strFetchParameterValue;
        
        final PluginsController.PluginValidationResult pluginValidationResultValidatePluginFromFile = validatePluginFromFile(pluginInstallParams.filePath);
        if (pluginValidationResultValidatePluginFromFile.plugin != null) {
            new InstallPluginBottomSheet(baseFragment, pluginValidationResultValidatePluginFromFile, pluginInstallParams).show();
        } else {
            AndroidUtilities.runOnUIThread(() -> BulletinFactory.of(baseFragment).createSimpleBulletin(R.raw.error, LocaleController.formatString(R.string.PluginInstallError, displayName), LocaleUtils.createCopySpan(baseFragment), () -> {
                if (AndroidUtilities.addToClipboard(pluginValidationResultValidatePluginFromFile.error)) {
                    BulletinFactory.of(baseFragment).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                }
            }).show());
        }
    }

    @Override
    public void openPluginSettings(String str, BaseFragment baseFragment) {
        Plugin plugin = getPluginsController().plugins.get(str);
        if (plugin != null) {
            openPluginSettings(plugin, baseFragment);
        }
    }

    @Override
    public void openPluginSettings(final Plugin plugin, final BaseFragment baseFragment) {
        if (plugin == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> baseFragment.presentFragment(new PluginSettingsActivity(plugin)));
    }

    @Override
    public void openPluginSetting(final Plugin plugin, final String str, final BaseFragment baseFragment) {
        if (plugin == null) {
            return;
        }
        Utilities.pluginsQueue.postRunnable(() -> {
            final PluginSettingsActivity pluginSettingsActivity;
            FileLog.d("Opening plugin setting: " + plugin.getId() + "/" + str);
            if (str == null || !str.contains(":")) {
                pluginSettingsActivity = new PluginSettingsActivity(plugin, str);
            } else {
                List<SettingItem> list = getPluginsController().settings.get(plugin.getId());
                if (list == null) {
                    return;
                }
                String[] strArrSplit = str.split(":");
                TextSetting textSetting = null;
                List<SettingItem> pySettingDefinitions = list;
                for (int i = 0; i < strArrSplit.length - 1; i++) {
                    String str2 = strArrSplit[i];
                    Iterator<SettingItem> it = pySettingDefinitions.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        SettingItem next = it.next();
                        if (next instanceof TextSetting) {
                            TextSetting textSetting2 = (TextSetting) next;
                            if (str2.equals(textSetting2.linkAlias)) {
                                try {
                                    PyObject pyObjectCall = textSetting2.createSubFragmentCallback.call();
                                    if (pyObjectCall != null) {
                                        pySettingDefinitions = parsePySettingDefinitions(pyObjectCall.asList());
                                    }
                                } catch (Exception unused) {
                                }
                                textSetting = textSetting2;
                            }
                        }
                    }
                    if (textSetting == null && pySettingDefinitions.isEmpty()) {
                        return;
                    }
                }
                if (textSetting == null) {
                    return;
                } else {
                    String prefix = TextUtils.join(":", Arrays.copyOf(strArrSplit, strArrSplit.length - 1));
                    pluginSettingsActivity = new PluginSettingsActivity(plugin, textSetting.text, pySettingDefinitions, textSetting.createSubFragmentCallback, strArrSplit[strArrSplit.length - 1]).setSettingsLinkPrefix(prefix);
                }
            }
            AndroidUtilities.runOnUIThread(() -> {
                baseFragment.presentFragment(pluginSettingsActivity);
                pluginSettingsActivity.checkTargetSetting();
            });
        });
    }

    @Override
    public void openPluginSetting(String str, String str2, BaseFragment baseFragment) {
        Plugin plugin = getPluginsController().plugins.get(str);
        if (plugin != null) {
            openPluginSetting(plugin, str2, baseFragment);
        }
    }

    public void setDebuggerListener(PyObject pyObject) {
        this.debuggerListener = pyObject;
    }
}