package tw.nekomimi.nekogram.plugins.ui.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EffectsTextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.LaunchActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.plugins.Plugin;
import tw.nekomimi.nekogram.plugins.PluginsController;
import tw.nekomimi.nekogram.utils.text.LocaleUtils;
import tw.nekomimi.nekogram.utils.ui.CanvasUtils;
@SuppressLint({"ViewConstructor"})
public class PluginCell extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {
    private final Switch checkBox;
    private boolean compact;
    private final ImageView deleteButton;
    private final EffectsTextView descriptionView;
    private final LinearLayout headerLayout;
    private final BackupImageView imageView;
    private final ImageView openInButton;
    private final ImageView pinButton;
    private Plugin plugin;
    private PluginCellDelegate pluginCellDelegate;
    private final TextView pluginNameView;
    private final ImageView settingsButton;
    private final ImageView shareButton;
    private final EffectsTextView subtitleView;

    public PluginCell(Context context) {
        super(context);
        ViewGroup viewGroup;
        ViewGroup viewGroup2;

        setClickable(false);
        setClipChildren(false);
        setClipToPadding(false);

        boolean isDoubleDivider = NekoConfig.dividerType == 2;
        int adaptedSurfaceColor = CanvasUtils.INSTANCE.getAdaptedSurfaceColor();

        if (isDoubleDivider) {
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            addView(linearLayout, LayoutHelper.createFrame(-1, -2.0f, 0, 12.0f, 4.0f, 12.0f, 4.0f));

            FrameLayout frameLayout = new FrameLayout(context);
            frameLayout.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16.0f), AndroidUtilities.dp(4.0f), adaptedSurfaceColor));
            linearLayout.addView(frameLayout, LayoutHelper.createLinear(-1, -2));

            FrameLayout frameLayout2 = new FrameLayout(context);
            frameLayout2.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4.0f), AndroidUtilities.dp(16.0f), adaptedSurfaceColor));
            linearLayout.addView(frameLayout2, LayoutHelper.createLinear(-1, -2, 0.0f, 2.0f, 0.0f, 0.0f));

            LinearLayout linearLayout2 = new LinearLayout(context);
            linearLayout2.setOrientation(LinearLayout.VERTICAL);
            frameLayout.addView(linearLayout2, LayoutHelper.createFrame(-1, -2.0f, Gravity.FILL, 16.0f, 16.0f, 16.0f, 12.0f));

            viewGroup = linearLayout2;
            viewGroup2 = frameLayout2;
        } else {
            FrameLayout frameLayout3 = new FrameLayout(context);
            frameLayout3.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16.0f), adaptedSurfaceColor));
            addView(frameLayout3, LayoutHelper.createFrame(-1, -2.0f, 0, 12.0f, 4.0f, 12.0f, 4.0f));

            LinearLayout linearLayout3 = new LinearLayout(context);
            linearLayout3.setOrientation(LinearLayout.VERTICAL);
            frameLayout3.addView(linearLayout3, LayoutHelper.createFrame(-1, -2.0f, Gravity.FILL, 16.0f, 16.0f, 16.0f, 8.0f));

            viewGroup = linearLayout3;
            viewGroup2 = linearLayout3;
        }

        LinearLayout linearLayout4 = new LinearLayout(context);
        this.headerLayout = linearLayout4;
        linearLayout4.setOrientation(LinearLayout.VERTICAL);
        viewGroup.addView(linearLayout4, LayoutHelper.createLinear(-1, -2));

        BackupImageView backupImageView = new BackupImageView(context) {
            @Override
            @SuppressLint({"DrawAllocation"})
            public void onDraw(Canvas canvas) {
                Path path = new Path();
                float fDp = AndroidUtilities.dp(8.0f);
                path.addRoundRect(new RectF(0.0f, 0.0f, getWidth(), getHeight()), fDp, fDp, Path.Direction.CW);
                canvas.save();
                canvas.clipPath(path);
                super.onDraw(canvas);
            }
        };
        this.imageView = backupImageView;
        backupImageView.setVisibility(View.GONE);
        backupImageView.setRoundRadius(AndroidUtilities.dp(8.0f));
        backupImageView.getImageReceiver().setAutoRepeat(1);
        backupImageView.getImageReceiver().setAutoRepeatCount(0);
        linearLayout4.addView(backupImageView, LayoutHelper.createLinear(56, 56, Gravity.TOP | Gravity.LEFT, 0, 0, 0, 12));

        LinearLayout linearLayout5 = new LinearLayout(context);
        linearLayout5.setOrientation(LinearLayout.VERTICAL);
        linearLayout4.addView(linearLayout5, LayoutHelper.createLinear(-1, -2));

        TextView textView = new TextView(context);
        this.pluginNameView = textView;
        textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(1, 18.0f);
        TextUtils.TruncateAt truncateAt = TextUtils.TruncateAt.END;
        textView.setEllipsize(truncateAt);
        textView.setTypeface(AndroidUtilities.bold());
        linearLayout5.addView(textView, LayoutHelper.createLinear(-1, -2));

        EffectsTextView effectsTextView = new EffectsTextView(context);
        this.subtitleView = effectsTextView;
        effectsTextView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        effectsTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
        effectsTextView.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
        int linkColor = Theme.key_windowBackgroundWhiteLinkText;
        effectsTextView.setLinkTextColor(Theme.getColor(linkColor));
        effectsTextView.setTextSize(1, 14.0f);
        effectsTextView.setEllipsize(truncateAt);
        effectsTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        linearLayout5.addView(effectsTextView, LayoutHelper.createLinear(-1, -2, 0.0f, 2.0f, 0.0f, 0.0f));

        EffectsTextView effectsTextView2 = new EffectsTextView(context);
        this.descriptionView = effectsTextView2;
        effectsTextView2.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        effectsTextView2.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
        effectsTextView2.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
        effectsTextView2.setLinkTextColor(Theme.getColor(linkColor));
        viewGroup.addView(effectsTextView2, LayoutHelper.createLinear(-1, -2, 0, 0, 12, 0, 0));

        if (!isDoubleDivider) {
            View view = new View(context);
            view.setBackgroundColor(Theme.getColor(Theme.key_divider));
            viewGroup.addView(view, LayoutHelper.createLinear(-1, 1.0f / AndroidUtilities.density, 0, 0, 12, 0, 8));
        }

        LinearLayout linearLayout6 = new LinearLayout(context);
        linearLayout6.setOrientation(LinearLayout.HORIZONTAL);

        ImageView imageViewCreateButton = createButton(context, R.drawable.msg_share, false, this::lambda$new$0);
        this.shareButton = imageViewCreateButton;
        linearLayout6.addView(imageViewCreateButton, LayoutHelper.createFrame(40, 40.0f, Gravity.TOP | Gravity.LEFT, 0.0f, 0.0f, 8.0f, 0.0f));

        ImageView imageViewCreateButton2 = createButton(context, R.drawable.msg_openin, false, this::lambda$new$1);
        this.openInButton = imageViewCreateButton2;
        linearLayout6.addView(imageViewCreateButton2, LayoutHelper.createFrame(40, 40.0f, Gravity.TOP | Gravity.LEFT, 0.0f, 0.0f, 8.0f, 0.0f));

        ImageView imageViewCreateButton3 = createButton(context, R.drawable.msg_pin, false, this::lambda$new$2);
        this.pinButton = imageViewCreateButton3;
        linearLayout6.addView(imageViewCreateButton3, LayoutHelper.createFrame(40, 40.0f, Gravity.TOP | Gravity.LEFT, 0.0f, 0.0f, 8.0f, 0.0f));

        ImageView imageViewCreateButton4 = createButton(context, R.drawable.msg_settings, false, this::lambda$new$3);
        this.settingsButton = imageViewCreateButton4;
        imageViewCreateButton4.setVisibility(View.GONE);
        linearLayout6.addView(imageViewCreateButton4, LayoutHelper.createFrame(40, 40.0f, Gravity.TOP | Gravity.LEFT, 0.0f, 0.0f, 8.0f, 0.0f));

        ImageView imageViewCreateButton5 = createButton(context, R.drawable.msg_delete, true, this::lambda$new$4);
        this.deleteButton = imageViewCreateButton5;

        if (isDoubleDivider) {
            viewGroup2.addView(linearLayout6, LayoutHelper.createFrame(-1, 40.0f, Gravity.CENTER_VERTICAL | Gravity.LEFT, 16.0f, 8.0f, 16.0f, 8.0f));
            viewGroup2.addView(imageViewCreateButton5, LayoutHelper.createFrame(40, 40.0f, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0.0f, 0.0f, 16.0f, 0.0f));
        } else {
            addView(imageViewCreateButton5, LayoutHelper.createFrame(40, 40.0f, Gravity.BOTTOM | Gravity.RIGHT, 0.0f, 0.0f, 16.0f, 8.0f));
            viewGroup2.addView(linearLayout6, LayoutHelper.createFrame(-1, 40, Gravity.BOTTOM | Gravity.LEFT));
        }

        Switch r3 = new Switch(context);
        this.checkBox = r3;
        int i2 = Theme.key_switchTrack;
        int i3 = Theme.key_switchTrackChecked;
        int i4 = Theme.key_windowBackgroundWhite;
        r3.setColors(i2, i3, i4, i4);
        r3.setFocusable(false);
        addView(r3, LayoutHelper.createFrame(37, 40.0f, Gravity.TOP | Gravity.RIGHT, 0.0f, 16.0f, 24.0f, 0.0f));
    
        setCompact(NekoConfig.pluginsCompactView);
    }

    private /* synthetic */ void lambda$new$0(View view) {
        if (this.pluginCellDelegate != null) {
            this.pluginCellDelegate.sharePlugin();
        }
    }

    private /* synthetic */ void lambda$new$1(View view) {
        if (this.pluginCellDelegate != null) {
            this.pluginCellDelegate.openInExternalApp();
        }
    }

    private /* synthetic */ void lambda$new$2(View view) {
        if (this.pluginCellDelegate != null) {
            this.pluginCellDelegate.pinPlugin(this);
        }
    }

    private /* synthetic */ void lambda$new$3(View view) {
        if (this.pluginCellDelegate != null) {
            this.pluginCellDelegate.openPluginSettings();
        }
    }

    private /* synthetic */ void lambda$new$4(View view) {
        if (this.pluginCellDelegate != null) {
            this.pluginCellDelegate.deletePlugin();
        }
    }

    public void setCompact(boolean z) {
        if (this.compact == z) {
            return;
        }
        this.compact = z;
        updateLayout();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(i), MeasureSpec.EXACTLY), i2);
    }

    private void updateLayout() {
        if (this.plugin == null) {
            return;
        }
        LinearLayout linearLayout = (LinearLayout) this.headerLayout.getChildAt(1);
        this.headerLayout.setOrientation(!this.compact ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.imageView.getLayoutParams();
        layoutParams.rightMargin = this.compact ? AndroidUtilities.dp(16.0f) : 0;
        layoutParams.bottomMargin = this.compact ? 0 : AndroidUtilities.dp(12.0f);
        layoutParams.width = this.compact ? AndroidUtilities.dp(49.0f) : AndroidUtilities.dp(56.0f);
        layoutParams.height = this.compact ? AndroidUtilities.dp(49.0f) : AndroidUtilities.dp(56.0f);
        
        LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) linearLayout.getLayoutParams();
        boolean z = this.compact;
        layoutParams2.gravity = z ? Gravity.CENTER_VERTICAL : Gravity.LEFT | Gravity.CENTER_VERTICAL;
        this.pluginNameView.setSingleLine(z);
        this.subtitleView.setSingleLine(this.compact);
        
        final int iDp = ((!this.compact || this.plugin.hasError()) && (this.compact || (this.plugin.getPack() != null && this.plugin.getIndex() >= 0) || this.plugin.hasError())) ? 0 : AndroidUtilities.dp(61.0f);
        this.pluginNameView.setPadding(0, 0, iDp, 0);
        this.pluginNameView.post(() -> lambda$updateLayout$5(iDp));
        requestLayout();
    }

    private /* synthetic */ void lambda$updateLayout$5(int i) {
        int paddingRight = i;
        if (this.pluginNameView.getLineCount() > 1) {
            paddingRight = 0;
        }
        this.subtitleView.setPadding(0, 0, paddingRight, 0);
    }

    public void set(Plugin plugin, final PluginCellDelegate pluginCellDelegate) {
        String str;
        if (plugin == null || pluginCellDelegate == null) {
            return;
        }
        this.pluginCellDelegate = pluginCellDelegate;
        this.plugin = plugin;
        setPinned(PluginsController.isPluginPinned(plugin.getId()));
        this.openInButton.setVisibility(pluginCellDelegate.canOpenInExternalApp() ? View.VISIBLE : View.GONE);
        boolean hasImage = plugin.getPack() != null && plugin.getIndex() >= 0;
        this.imageView.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        if (hasImage) {
           MediaDataController.getInstance(UserConfig.selectedAccount).setPlaceholderImageByIndex(this.imageView, plugin.getPack(), plugin.getIndex(), "100_100");
        } else {
            this.imageView.setImage((ImageLocation) null, (String) null, (Drawable) null, 0, (Object) null);
        }
        this.pluginNameView.setText(plugin.getName());
        EffectsTextView effectsTextView = this.subtitleView;
        if (this.compact) {
            str = "v";
        } else {
            str = LocaleController.getString(R.string.PluginVersion) + " ";
        }
        effectsTextView.setText(new SpannableStringBuilder(str).append(plugin.getVersion()).append(" • ").append(LocaleUtils.formatWithUsernames(plugin.getAuthor())));
        if (plugin.hasError()) {
            bindErrorState();
        } else {
            bindNormalState();
        }
        updateLayout();
        this.checkBox.setChecked(plugin.isEnabled(), false);
        this.checkBox.setOnClickListener(view -> lambda$set$6(pluginCellDelegate, view));
        AndroidUtilities.updateViewVisibilityAnimated(this.settingsButton, plugin.isEnabled() && PluginsController.getInstance().hasPluginSettings(plugin.getId()), 0.5f, true, false);
    }

    private /* synthetic */ void lambda$set$6(PluginCellDelegate pluginCellDelegate, View view) {
        pluginCellDelegate.togglePlugin(this);
    }

    private void bindErrorState() {
        this.descriptionView.setText(this.plugin.getError().getLocalizedMessage());
        this.descriptionView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
        this.descriptionView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MONO));
        this.descriptionView.setTextSize(1, 12.0f);
        this.descriptionView.setOnClickListener(this::lambda$bindErrorState$7);
        this.checkBox.setVisibility(View.GONE);
    }

    private /* synthetic */ void lambda$bindErrorState$7(View view) {
        if (AndroidUtilities.addToClipboard(stackTraceToString(this.plugin.getError()))) {
            BulletinFactory.of(LaunchActivity.getSafeLastFragment()).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
        }
    }

    public static String stackTraceToString(Throwable th) {
        StringWriter stringWriter = new StringWriter();
        th.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private void bindNormalState() {
        this.descriptionView.setText(LocaleUtils.fullyFormatText(this.plugin.getDescription()));
        this.descriptionView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        this.descriptionView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
        this.descriptionView.setTextSize(1, 15.0f);
        this.descriptionView.setOnClickListener(null);
        this.checkBox.setVisibility(View.VISIBLE);
    }

    public void setChecked(boolean z, boolean z2) {
        this.checkBox.setChecked(z, z2);
    }

    public void setPinned(boolean z) {
        this.pinButton.setImageResource(z ? R.drawable.msg_unpin : R.drawable.msg_pin);
    }

    private ImageView createButton(Context context, int i, boolean z, View.OnClickListener onClickListener) {
        AppCompatImageView appCompatImageView = new AppCompatImageView(context);
        applyClickAnimation(appCompatImageView);
        appCompatImageView.setScaleType(ImageView.ScaleType.CENTER);
        appCompatImageView.setImageDrawable(ContextCompat.getDrawable(context, i).mutate());
        appCompatImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(z ? Theme.key_text_RedRegular : Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
        appCompatImageView.setBackground(Theme.createSelectorDrawable(z ? Theme.multAlpha(Theme.getColor(Theme.key_text_RedRegular), 0.12f) : Theme.getColor(Theme.key_dialogButtonSelector), 1, AndroidUtilities.dp(20.0f)));
        appCompatImageView.setOnClickListener(onClickListener);
        return appCompatImageView;
    }

    private void applyClickAnimation(View view) {
        view.setOnTouchListener(this::onTouchAnimation);
    }

    private boolean onTouchAnimation(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            view.setPressed(true);
            view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80L).start();
            return true;
        }
        if (action != MotionEvent.ACTION_UP) {
            if (action != MotionEvent.ACTION_CANCEL) {
                return false;
            }
            view.setPressed(false);
            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(350L).setInterpolator(new OvershootInterpolator(1.5f)).start();
            return true;
        }
        view.setPressed(false);
        if (motionEvent.getX() >= 0.0f && motionEvent.getX() <= view.getWidth() && motionEvent.getY() >= 0.0f && motionEvent.getY() <= view.getHeight()) {
            view.performClick();
        }
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(350L).setInterpolator(new OvershootInterpolator(1.5f)).start();
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginSettingsRegistered);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginSettingsUnregistered);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pluginSettingsRegistered);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pluginSettingsUnregistered);
    }

    @Override
    public void didReceivedNotification(int i, int i2, Object... objArr) {
        if ((i == NotificationCenter.pluginSettingsRegistered || i == NotificationCenter.pluginSettingsUnregistered) && objArr.length > 0) {
            boolean z = false;
            Object obj = objArr[0];
            if ((obj instanceof String) && ((String) obj).equals(this.plugin.getId())) {
                ImageView imageView = this.settingsButton;
                if (i == NotificationCenter.pluginSettingsRegistered && this.plugin.isEnabled()) {
                    z = true;
                }
                AndroidUtilities.updateViewVisibilityAnimated(imageView, z, 0.5f, true, true);
            }
        }
    }

    public static class Factory extends UItem.UItemFactory {
        static {
            UItem.UItemFactory.setup(new Factory());
        }

   @Override
        public PluginCell createView(Context context, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new PluginCell(context);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            if (view instanceof PluginCell) {

                PluginCellDelegate delegate = (PluginCellDelegate) item.object;
                ((PluginCell) view).set(item.plugin, delegate);
            }
        }

        public static UItem as(Plugin plugin, PluginCellDelegate delegate) {
            UItem item = UItem.ofFactory(Factory.class);
            item.plugin = plugin;
            item.object = delegate;
            return item;
        }
    }
}