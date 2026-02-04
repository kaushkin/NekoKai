package tw.nekomimi.nekogram.plugins.hooks;

import android.content.Context;
import android.text.TextUtils;
import com.chaquo.python.PyObject;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.mvel2.MVEL;
import org.telegram.messenger.ApplicationLoader;

import tw.nekomimi.nekogram.plugins.PluginsConstants;
import tw.nekomimi.nekogram.plugins.utils.PyObjectUtils;

public class MenuItemRecord {
    private static final ConcurrentHashMap<String, Serializable> mvelExpressionCache = new ConcurrentHashMap<>();
    
    public final String conditionString;
    public final String iconName;
    public final int iconResId;
    public final String itemId;
    public final String menuType;
    public final PyObject onClickCallback;
    public final String pluginId;
    public final int priority;
    public final String subtext;
    public final String text;

    public MenuItemRecord(String pluginId, PyObject pyObject) {
        this.pluginId = pluginId;
        this.menuType = PyObjectUtils.getString(pyObject, PluginsConstants.MenuItemProperties.MENU_TYPE, null, true);
        this.text = PyObjectUtils.getString(pyObject, "text", null, true);
        
        this.onClickCallback = pyObject.callAttr("get", "on_click");
        
        String id = PyObjectUtils.getString(pyObject, PluginsConstants.MenuItemProperties.ITEM_ID, null, true);
        this.itemId = TextUtils.isEmpty(id) ? UUID.randomUUID().toString() : id;
        
        this.iconName = PyObjectUtils.getString(pyObject, "icon", null, true);
        this.subtext = PyObjectUtils.getString(pyObject, "subtext", null, true);
        this.conditionString = PyObjectUtils.getString(pyObject, "condition", null, true);
        this.priority = PyObjectUtils.getInt(pyObject, PluginsConstants.MenuItemProperties.PRIORITY, 0, true);

        int resId = 0;
        if (!TextUtils.isEmpty(this.iconName)) {
            try {
                Context context = ApplicationLoader.applicationContext;
                resId = context.getResources().getIdentifier(this.iconName, "drawable", context.getPackageName());
            } catch (Exception unused) {}
        }
        this.iconResId = resId;

        if (TextUtils.isEmpty(this.menuType) || TextUtils.isEmpty(this.text) || this.onClickCallback == null) {
            throw new IllegalArgumentException("MenuItemRecord missing essential fields: menuType, text, or onClickCallback.");
        }
    }

    public boolean checkCondition(Map<String, Object> map) {
        if (TextUtils.isEmpty(this.conditionString) || map == null) {
            return true;
        }
        try {
            Serializable expression = mvelExpressionCache.computeIfAbsent(this.conditionString, MVEL::compileExpression);
            
            Object result = MVEL.executeExpression(expression, map, Boolean.class);
            
            return result instanceof Boolean ? (Boolean) result : false;
        } catch (Exception unused) {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            MenuItemRecord other = (MenuItemRecord) obj;
            return Objects.equals(this.itemId, other.itemId) && 
                   Objects.equals(this.pluginId, other.pluginId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.itemId, this.pluginId);
    }
}