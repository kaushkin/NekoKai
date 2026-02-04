package tw.nekomimi.nekogram.preferences;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.utils.ui.PopupUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.Components.inset.WindowInsetsStateHolder;

public abstract class BasePreferencesActivity extends BaseFragment {
    private AnimatorSet actionBarAnimator;
    private View actionBarBackground;
    protected LinearLayoutManager layoutManager;
    public UniversalRecyclerView listView;
    private final int[] location = new int[2];
    protected final WindowInsetsStateHolder windowInsetsStateHolder = new WindowInsetsStateHolder(this::checkInsets);

    protected abstract void fillItems(ArrayList<UItem> arrayList, UniversalAdapter universalAdapter);

    public abstract String getTitle();

    protected boolean hasHeaderCell() {
        return false;
    }

    protected boolean hasWhiteActionBar() {
        return false;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    protected abstract void onClick(UItem uItem, View view, int i, float f, float f2);

    protected void checkInsets() {
        if (listView != null) {
            this.listView.setPadding(0, 0, 0, this.windowInsetsStateHolder.getCurrentNavigationBarInset());
        }
    }

    @Override
    public View createView(Context context) {
        this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        this.actionBar.setAllowOverlayTitle(false);
        this.actionBar.setTitle(getTitle());
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int i) {
                if (i == -1) {
                    finishFragment();
                }
            }
        });
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        ActionBar actionBar = this.actionBar;
        if (actionBar.menu == null) {
            actionBar.createMenu();
        }
        UniversalRecyclerView universalRecyclerView = new UniversalRecyclerView(this, 
            (obj, obj2) -> fillItems((ArrayList<UItem>) obj, (UniversalAdapter) obj2), 
            (obj, obj2, obj3, obj4, obj5) -> onClick((UItem) obj, (View) obj2, (Integer) obj3, (Float) obj4, (Float) obj5), 
            (obj, obj2, obj3, obj4, obj5) -> onLongClick((UItem) obj, (View) obj2, (Integer) obj3, (Float) obj4, (Float) obj5)
        );
        this.listView = universalRecyclerView;
        this.listView.setClipToPadding(false);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, 1, false);
        this.layoutManager = linearLayoutManager;
        this.listView.setLayoutManager(linearLayoutManager);
        frameLayout.addView(this.listView, LayoutHelper.createFrame(-1, -1.0f));
        
        if (hasHeaderCell()) {
            this.actionBar.setBackground(null);
            ActionBar actionBar2 = this.actionBar;
            int i = Theme.key_windowBackgroundWhiteBlackText;
            actionBar2.setTitleColor(Theme.getColor(i));
            this.actionBar.setItemsColor(Theme.getColor(i), false);
            this.actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_listSelector), false);
            this.actionBar.setCastShadows(false);
            this.actionBar.setAddToContainer(false);
            this.actionBar.getTitleTextView().setAlpha(0.0f);
            
            this.actionBarBackground = new View(context) {
                private final Paint paint = new Paint();
                @Override
                protected void onDraw(Canvas canvas) {
                    this.paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    int measuredHeight = getMeasuredHeight() - AndroidUtilities.dp(3.0f);
                    canvas.drawRect(0.0f, 0.0f, getMeasuredWidth(), measuredHeight, this.paint);
                    if (parentLayout != null) {
                        parentLayout.drawHeaderShadow(canvas, measuredHeight);
                    }
                }
            };
            this.actionBarBackground.setAlpha(0.0f);
            frameLayout.addView(this.actionBarBackground, LayoutHelper.createFrame(-1, -2.0f));
            frameLayout.addView(this.actionBar, LayoutHelper.createFrame(-1, -2.0f));
            this.listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int i2, int i3) {
                    super.onScrolled(recyclerView, i2, i3);
                    checkScroll(true);
                }
            });
            // CHANGED: Variable names to avoid conflict with 'i'
            frameLayout.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> checkScroll(false));
        }
        
        this.fragmentView = frameLayout;
        ViewCompat.setOnApplyWindowInsetsListener(frameLayout, (view, windowInsetsCompat) -> {
            this.windowInsetsStateHolder.setInsets(windowInsetsCompat);
            return WindowInsetsCompat.CONSUMED;
        });
        return this.fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.listView != null && this.listView.adapter != null) {
            this.listView.adapter.update(false);
        }
        
        Bulletin.addDelegate(this, new Bulletin.Delegate() {
            @Override
            public int getBottomOffset(int i) {
                return windowInsetsStateHolder.getCurrentNavigationBarInset();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Bulletin.removeDelegate(this);
    }

    public void scrollToItem(int i) {
        final int iFindPositionByItemId = this.listView.findPositionByItemId(i);
        if (iFindPositionByItemId >= 0 && iFindPositionByItemId < this.listView.adapter.getItemCount()) {
            this.listView.highlightRow(() -> {
                layoutManager.scrollToPositionWithOffset(iFindPositionByItemId, AndroidUtilities.dp(60.0f));
                return iFindPositionByItemId;
            });
        }
    }

    protected int[] unBox(Collection<Integer> collection) {
        return collection.stream().mapToInt(Integer::intValue).toArray();
    }

    protected void showListDialog(UItem uItem, CharSequence[] charSequenceArr, String str, int i, PopupUtils.OnItemClickListener onItemClickListener) {
        showListDialog(uItem, charSequenceArr, null, str, i, onItemClickListener);
    }

    protected void showListDialog(UItem uItem, CharSequence[] charSequenceArr, int[] iArr, String str, int i, PopupUtils.OnItemClickListener onItemClickListener) {
        showListDialog(uItem, charSequenceArr, iArr, str, i, onItemClickListener, iArr == null, true);
    }

    protected void showListDialog(final UItem uItem, final CharSequence[] charSequenceArr, int[] iArr, String str, final int i, final PopupUtils.OnItemClickListener onItemClickListener, boolean z, final boolean z2) {
        if (getParentActivity() == null) {
            return;
        }
        PopupUtils.showDialog(charSequenceArr, iArr, str, i, getContext(), i2 -> {
            if (z2 && i == i2) {
                return;
            }
            onItemClickListener.onClick(i2);
            View viewFindViewByItemId = this.listView.findViewByItemId(uItem.id);
            if (viewFindViewByItemId instanceof TextCell) {
                ((TextCell) viewFindViewByItemId).setValue(charSequenceArr[i2], true);
            }
            this.listView.adapter.update(true);
        }, getResourceProvider(), z);
    }

    protected void showRestartBulletin() {
        // CHANGED: Fixed Resource ID
        BulletinFactory.of(this).createSimpleBulletin(R.raw.info, "LocaleController.getString(R.string.LanguageRestart)", LocaleController.getString(R.string.Cancel), () -> {
            Context context = getContext();
            Intent launchIntentForPackage = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (launchIntentForPackage != null) {
                Intent intentMakeRestartActivityTask = Intent.makeRestartActivityTask(launchIntentForPackage.getComponent());
                intentMakeRestartActivityTask.setPackage(context.getPackageName());
                context.startActivity(intentMakeRestartActivityTask);
                Runtime.getRuntime().exit(0);
            }
        }).show();
    }

    @Override
    public boolean isLightStatusBar() {
        return !hasWhiteActionBar() ? super.isLightStatusBar() : ColorUtils.calculateLuminance(getThemedColor(Theme.key_windowBackgroundWhite)) > 0.699999988079071d;
    }

    protected boolean onLongClick(UItem uItem, View view, int i, float f, float f2) {
        return false;
    }

    public void checkScroll(boolean z) {
        if (this.actionBarAnimator != null) {
            this.actionBarAnimator.cancel();
            this.actionBarAnimator = null;
        }
        int i = this.layoutManager.findFirstVisibleItemPosition();

        float f = i > 0 ? 1.0f : 0.0f;
        
        if (z) {
            this.actionBarAnimator = new AnimatorSet();
            this.actionBarAnimator.playTogether(
                android.animation.ObjectAnimator.ofFloat(this.actionBarBackground, View.ALPHA, f),
                android.animation.ObjectAnimator.ofFloat(this.actionBar.getTitleTextView(), View.ALPHA, f)
            );
            this.actionBarAnimator.setDuration(250L);
            this.actionBarAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    if (animator.equals(actionBarAnimator)) {
                        actionBarAnimator = null;
                    }
                }
            });
            this.actionBarAnimator.start();
        } else {
            this.actionBarBackground.setAlpha(f);
            this.actionBar.getTitleTextView().setAlpha(f);
        }
    }
}