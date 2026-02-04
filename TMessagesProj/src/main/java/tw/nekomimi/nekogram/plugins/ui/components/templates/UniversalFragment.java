package tw.nekomimi.nekogram.plugins.ui.components.templates;

import android.content.Context;
import android.view.View;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

public class UniversalFragment extends org.telegram.ui.Components.UniversalFragment {
    private UniversalFragmentDelegate delegate;

    public UniversalFragment(UniversalFragmentDelegate universalFragmentDelegate) {
        this.delegate = universalFragmentDelegate;
    }

    public UniversalFragmentDelegate getDelegate() {
        return this.delegate;
    }

    public void setDelegate(UniversalFragmentDelegate universalFragmentDelegate) {
        this.delegate = universalFragmentDelegate;
    }

    @Override
    public View createView(Context context) {
        UniversalFragmentDelegate delegate = this.delegate;
        View beforeView = delegate != null ? delegate.beforeCreateView() : null;
        
        if (beforeView != null) {
            return beforeView;
        }

        View view = super.createView(context);
        
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int i) {
                if (i == -1) {
                    // if (UniversalFragment.this.onBackPressed()) {
                        // UniversalFragment.this.finishFragment();
                    // }
                } else if (UniversalFragment.this.delegate != null) {
                    UniversalFragment.this.delegate.onMenuItemClick(i);
                }
            }
        });

        UniversalFragmentDelegate delegate2 = this.delegate;
        if (delegate2 != null) {
            View afterView = delegate2.afterCreateView(view);
            if (afterView != null) {
                return afterView;
            }
        }
        return view;
    }

    public ActionBarMenu getActionBarMenu() {
        return this.actionBar.createMenu();
    }

    @Override
    protected CharSequence getTitle() {
        if (this.delegate != null) {
            return this.delegate.getTitle();
        }
        return null;
    }

    public void setTitle(CharSequence title, boolean animated, long duration) {
        if (animated) {
            ActionBar actionBar = this.actionBar;
            if (duration <= 0) {
                duration = 300;
            }
            actionBar.setTitleAnimated(title, false, duration);
        } else {
            this.actionBar.setTitle(title);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> arrayList, UniversalAdapter universalAdapter) {
        if (this.delegate != null) {
            this.delegate.fillItems(arrayList, universalAdapter);
        }
    }

    @Override
    protected void onClick(UItem uItem, View view, int i, float f, float f2) {
        if (this.delegate != null) {
            this.delegate.onClick(uItem, view, i, f, f2);
        }
    }

    @Override
    protected boolean onLongClick(UItem uItem, View view, int i, float f, float f2) {
        if (this.delegate != null) {
            return this.delegate.onLongClick(uItem, view, i, f, f2);
        }
        return false;
    }

    @Override
    public boolean onFragmentCreate() {
        if (this.delegate != null) {
            this.delegate.onFragmentCreate();
        }
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        if (this.delegate != null) {
            this.delegate.onFragmentDestroy();
        }
        super.onFragmentDestroy();
    }

    // todo
    // @Override
    // public boolean onBackPressed() {
    //     if (this.delegate != null) {
    //         Boolean result = this.delegate.onBackPressed();
    //         if (result != null) {
    //             return result;
    //         }
    //     }
    //     return super.onBackPressed();
    // }

    public interface UniversalFragmentDelegate {
        default View beforeCreateView() { return null; }
        default View afterCreateView(View view) { return view; }
        
        void fillItems(ArrayList<UItem> arrayList, UniversalAdapter universalAdapter);
        
        default CharSequence getTitle() { return null; }
        default Boolean onBackPressed() { return null; }
        
        default void onClick(UItem uItem, View view, int i, float f, float f2) {}
        default void onFragmentCreate() {}
        default void onFragmentDestroy() {}
        default boolean onLongClick(UItem uItem, View view, int i, float f, float f2) { return false; }
        default void onMenuItemClick(int i) {}
    }
}