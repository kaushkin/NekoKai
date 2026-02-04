package tw.nekomimi.nekogram.plugins;

import tw.nekomimi.nekogram.plugins.PluginsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

/* JADX INFO: loaded from: classes.dex */
public class Plugin {
    public transient PluginsController.PluginsEngine cachedEngine;
    private String engine;
    private final String id;
    private final String name;
    private String pack = null;
    private int index = -1;
    private Throwable error = null;
    private String version = "1.0";
    private String minVersion = "12.2.10";
    private String description = LocaleController.getString(R.string.PluginNoDescription);
    private String author = LocaleController.getString(R.string.PluginNoAuthor);
    private boolean isEnabled = false;

    public Plugin(String str, String str2) {
        this.id = str;
        this.name = str2;
    }

    private static boolean isIconValid(String str) {
        return str != null && str.matches("^[a-zA-Z][a-zA-Z0-9_]*/\\d+$");
    }

    public String getId() {
        return this.id;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String str) {
        this.description = str;
    }

    public String getEngine() {
        return this.engine;
    }

    public void setEngine(String str) {
        this.engine = str;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String str) {
        this.author = str;
    }

    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return !hasError() && this.isEnabled;
    }

    public void setEnabled(boolean z) {
        this.isEnabled = z && !hasError();
    }

    public Throwable getError() {
        return this.error;
    }

    public void setError(Throwable th) {
        this.error = th;
        if (hasError()) {
            this.isEnabled = false;
        }
    }

    public boolean hasError() {
        return this.error != null;
    }

    public String getPack() {
        return this.pack;
    }

    public int getIndex() {
        return this.index;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String str) {
        this.version = str;
    }

    public String getMinVersion() {
        return this.minVersion;
    }

    public void setMinVersion(String str) {
        this.minVersion = str;
    }

    public String getIcon() {
        if (this.pack == null || this.index < 0) {
            return null;
        }
        return this.pack + "/" + this.index;
    }

    public void setIcon(String str) {
        int iLastIndexOf;
        if (isIconValid(str) && (iLastIndexOf = str.lastIndexOf(47)) != -1) {
            this.pack = str.substring(0, iLastIndexOf);
            this.index = Integer.parseInt(str.substring(iLastIndexOf + 1));
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof Plugin) {
            return ((Plugin) obj).id.equals(this.id);
        }
        return super.equals(obj);
    }

    public int hashCode() {
        return this.id.hashCode();
    }
}