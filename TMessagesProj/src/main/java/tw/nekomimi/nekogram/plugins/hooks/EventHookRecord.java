package tw.nekomimi.nekogram.plugins.hooks;

import java.util.Objects;

public class EventHookRecord implements HookRecord {
    private final String hookName;
    private final boolean matchSubstring;
    private final String pluginId;
    private final int priority;

    public EventHookRecord(String pluginId, String hookName, boolean matchSubstring, int priority) {
        this.pluginId = pluginId;
        this.hookName = hookName;
        this.matchSubstring = matchSubstring;
        this.priority = priority;
    }

    public String getPluginId() {
        return this.pluginId;
    }

    public String getHookName() {
        return this.hookName;
    }

    public int getPriority() {
        return this.priority;
    }

    public boolean isMatchSubstring() {
        return this.matchSubstring;
    }

    @Override
    public void cleanup() {}

    @Override
    public boolean matches(Object obj) {
        if (obj instanceof String) {
            String str = (String) obj;
            if (this.hookName != null) {
                if (this.matchSubstring) {
                    return !this.hookName.isEmpty() && str.contains(this.hookName);
                }
                return this.hookName.equals(str);
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            EventHookRecord other = (EventHookRecord) obj;
            return this.matchSubstring == other.matchSubstring &&
                    Objects.equals(this.pluginId, other.pluginId) &&
                    Objects.equals(this.hookName, other.hookName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pluginId, this.hookName, this.matchSubstring);
    }
}