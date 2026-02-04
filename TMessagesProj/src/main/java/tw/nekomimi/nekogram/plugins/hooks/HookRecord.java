package tw.nekomimi.nekogram.plugins.hooks;

public interface HookRecord {
    void cleanup();

    boolean matches(Object obj);
}