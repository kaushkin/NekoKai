package tw.nekomimi.nekogram.plugins.ui.components;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.style.StrikethroughSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.EffectsTextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;
import org.telegram.ui.Stories.recorder.HintView2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.atomic.AtomicBoolean;

import tw.nekomimi.nekogram.plugins.Plugin;
import tw.nekomimi.nekogram.plugins.PluginsConstants;
import tw.nekomimi.nekogram.plugins.PluginsController;
import tw.nekomimi.nekogram.plugins.PythonPluginsEngine;
import tw.nekomimi.nekogram.plugins.ui.components.VerticalImageSpan;
import tw.nekomimi.nekogram.utils.ChatUtils;
import tw.nekomimi.nekogram.utils.text.LocaleUtils;

public class InstallPluginBottomSheet extends BottomSheet {
    private HintView2 currentHint;
    private boolean enableAfterInstallation = false;
    private FrameLayout container;

    @Override
    public void setLastVisible(boolean z) {
        super.setLastVisible(z);
    }

    public InstallPluginBottomSheet(final BaseFragment baseFragment, final PluginsController.PluginValidationResult result, final PluginInstallParams params) {
        super(baseFragment.getParentActivity(), false, baseFragment.getResourceProvider());
        
        Activity context = baseFragment.getParentActivity();
        fixNavigationBar();
        
        boolean isUpdate = PluginsController.getInstance().plugins.containsKey(result.plugin.getId());
        
        container = new FrameLayout(context);
        container.setClipChildren(false);
        container.setClipToPadding(false);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setClipChildren(false);
        linearLayout.setClipToPadding(false);
        container.addView(linearLayout);

        if (result.plugin.getPack() != null && result.plugin.getIndex() >= 0) {
            BackupImageView iconView = new BackupImageView(context) {
                private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
                private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
                private final Drawable badgeDrawable;
                {
                    paintStroke.setStyle(Paint.Style.STROKE);
                    paintStroke.setStrokeWidth(AndroidUtilities.dp(4.0f));
                    paintFill.setStyle(Paint.Style.FILL);
                    Drawable d = ContextCompat.getDrawable(getContext(), R.drawable.plugin_large);
                    if (d != null) {
                        badgeDrawable = d.mutate();
                    } else {
                        badgeDrawable = null;
                    }
                }

                @Override
                @SuppressLint("DrawAllocation")
                protected void onDraw(Canvas canvas) {
                    Path path = new Path();
                    float fDp = AndroidUtilities.dp(12.0f);
                    path.addRoundRect(new RectF(0.0f, 0.0f, getWidth(), getHeight()), fDp, fDp, Path.Direction.CW);
                    canvas.save();
                    canvas.clipPath(path);
                    super.onDraw(canvas);
                    canvas.restore();

                    paintStroke.setColor(getThemedColor(Theme.key_dialogBackground));
                    paintFill.setColor(getThemedColor(Theme.key_featuredStickers_addButton));
                    float badgeX = getMeasuredWidth() - AndroidUtilities.dp(10.0f);
                    float badgeY = getMeasuredHeight() - AndroidUtilities.dp(10.0f);
                    float badgeRadius = AndroidUtilities.dp(12.0f);
                    canvas.drawCircle(badgeX, badgeY, badgeRadius, paintStroke);
                    canvas.drawCircle(badgeX, badgeY, badgeRadius, paintFill);
                    if (badgeDrawable != null) {
                        badgeDrawable.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_featuredStickers_buttonText), PorterDuff.Mode.SRC_IN));
                        int size = AndroidUtilities.dp(16.0f); 
                        badgeDrawable.setBounds(
                                (int) (badgeX - size / 2),
                                (int) (badgeY - size / 2),
                                (int) (badgeX + size / 2),
                                (int) (badgeY + size / 2)
                        );
                        badgeDrawable.draw(canvas);
                    }
                }
            };
            iconView.setRoundRadius(AndroidUtilities.dp(12.0f));
            iconView.getImageReceiver().setAutoRepeat(1);
            iconView.getImageReceiver().setAutoRepeatCount(0);
            iconView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12.0f), getThemedColor(Theme.key_windowBackgroundGray)));
            linearLayout.addView(iconView, LayoutHelper.createLinear(78, 78, Gravity.CENTER_HORIZONTAL, 0, 28, 0, 0));
            MediaDataController.getInstance(UserConfig.selectedAccount).setPlaceholderImageByIndex(iconView, result.plugin.getPack(), result.plugin.getIndex(), "150_150");
        } else {
            RLottieImageView lottieView = new RLottieImageView(context);
            lottieView.setScaleType(ImageView.ScaleType.CENTER);
            lottieView.setImageResource(R.drawable.plugin_large);
            lottieView.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_featuredStickers_buttonText), PorterDuff.Mode.SRC_IN));
            lottieView.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(78.0f), getThemedColor(Theme.key_featuredStickers_addButton)));
            linearLayout.addView(lottieView, LayoutHelper.createLinear(78, 78, Gravity.CENTER_HORIZONTAL, 0, 28, 0, 0));
        }

        TextView titleView = new TextView(context);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(1, 18.0f);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setText(result.plugin.getName());
        linearLayout.addView(titleView, LayoutHelper.createLinear(-1, -2, 0, 40, 16, 40, 0));

        EffectsTextView subtitleView = new EffectsTextView(context);
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
        subtitleView.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
        subtitleView.setLinkTextColor(getThemedColor(Theme.key_dialogTextLink));
        subtitleView.setTextSize(1, 14.0f);
        int grayColor = Theme.key_windowBackgroundWhiteGrayText;
        subtitleView.setTextColor(getThemedColor(grayColor));

        SpannableStringBuilder ssb = new SpannableStringBuilder(LocaleController.getString(R.string.PluginVersion)).append(" ");
        if (isUpdate) {
            Plugin oldPlugin = PluginsController.getInstance().plugins.get(result.plugin.getId());
            if (oldPlugin != null) {
                int start = ssb.length();
                ssb.append(oldPlugin.getVersion()).append(" -> ").append(result.plugin.getVersion());
                ssb = VerticalImageSpan.createSpan(getContext(), R.drawable.msg_mini_arrow_mediathin, ssb.toString(), "->", grayColor, this.resourcesProvider);
                ssb.setSpan(new StrikethroughSpan(), start, start + oldPlugin.getVersion().length(), 33);
            } else {
                ssb.append(result.plugin.getVersion());
            }
        } else {
            ssb.append(result.plugin.getVersion());
        }
        ssb.append(" • ").append(LocaleUtils.formatWithUsernames(result.plugin.getAuthor(), baseFragment, this::dismiss));
        subtitleView.setText(ssb);
        linearLayout.addView(subtitleView, LayoutHelper.createLinear(-1, -2, 0, 21, 4, 21, 0));

        if (params.incompatible) {
            int badgeIcon = R.drawable.msg_warning;
            int badgeColor = getThemedColor(Theme.key_color_yellow);

            LinearLayout badgeLayout = new LinearLayout(context);
            ScaleStateListAnimator.apply(badgeLayout, 0.05f, 1.5f);
            badgeLayout.setOrientation(LinearLayout.HORIZONTAL);
            badgeLayout.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(20.0f), AndroidUtilities.dp(20.0f), AndroidUtilities.multiplyAlphaComponent(badgeColor, 0.1f)));
            badgeLayout.setPadding(AndroidUtilities.dp(12.0f), AndroidUtilities.dp(6.0f), AndroidUtilities.dp(16.0f), AndroidUtilities.dp(6.0f));
            badgeLayout.setGravity(Gravity.CENTER);

            ImageView badgeImg = new ImageView(context);
            badgeImg.setImageResource(badgeIcon);
            badgeImg.setColorFilter(new PorterDuffColorFilter(badgeColor, PorterDuff.Mode.SRC_IN));
            badgeLayout.addView(badgeImg, LayoutHelper.createLinear(14, 14, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));

            TextView badgeTxt = new TextView(context);
            badgeTxt.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
            badgeTxt.setTextColor(badgeColor);
            badgeTxt.setTextSize(1, 13.0f);
            badgeTxt.setText(LocaleController.formatString(R.string.PluginIncompatible));
            badgeLayout.addView(badgeTxt);

            linearLayout.addView(badgeLayout, LayoutHelper.createLinear(-2, -2, Gravity.CENTER, 0, 12, 0, 0));
            
            badgeLayout.setOnClickListener(v -> showIncompatibleHint(badgeLayout, params));
            AndroidUtilities.runOnUIThread(() -> showIncompatibleHint(badgeLayout, params), 600L);
        }

        EffectsTextView descView = new EffectsTextView(context);
        descView.setGravity(Gravity.CENTER_HORIZONTAL);
        descView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
        descView.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
        descView.setLinkTextColor(getThemedColor(Theme.key_dialogTextLink));
        descView.setTextSize(1, 15.0f);
        descView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        descView.setText(LocaleUtils.fullyFormatText(result.plugin.getDescription(), baseFragment, this::dismiss));
        linearLayout.addView(descView, LayoutHelper.createLinear(-1, -2, 0, 21, 28, 21, 0));

        ButtonWithCounterView installBtn = new ButtonWithCounterView(context, true, this.resourcesProvider);
        installBtn.setText(LocaleController.getString(isUpdate ? R.string.UpdatePlugin : R.string.InstallPlugin), false);
        installBtn.setOnClickListener(v -> installPlugin(params, result, baseFragment, isUpdate));
        linearLayout.addView(installBtn, LayoutHelper.createLinear(-1, 48, 0, 16, 28, 16, 16));

        if (!result.plugin.isEnabled()) {
            CheckBox2 checkBox = new CheckBox2(context, 21, this.resourcesProvider);
            checkBox.setColor(Theme.key_radioBackgroundChecked, Theme.key_checkboxDisabled, Theme.key_checkboxCheck);
            checkBox.setDrawUnchecked(true);
            checkBox.setChecked(this.enableAfterInstallation, false);
            checkBox.setDrawBackgroundAsArc(10);
            
            TextView checkText = new TextView(context);
            checkText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            checkText.setTextSize(1, 14.0f);
            checkText.setText(LocaleController.getString(R.string.EnableAfterInstallation));

            FrameLayout checkFrame = new FrameLayout(context);
            checkFrame.addView(checkBox, LayoutHelper.createFrame(21, 21.0f, Gravity.CENTER, 0.0f, 0.0f, 0.0f, 0.0f));

            LinearLayout checkLayout = new LinearLayout(context);
            checkLayout.setOrientation(LinearLayout.HORIZONTAL);
            checkLayout.setPadding(AndroidUtilities.dp(8.0f), AndroidUtilities.dp(6.0f), AndroidUtilities.dp(10.0f), AndroidUtilities.dp(6.0f));
            checkLayout.setGravity(Gravity.CENTER_VERTICAL);
            
            checkLayout.addView(checkFrame, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));
            checkLayout.addView(checkText, LayoutHelper.createLinear(-2, -2, Gravity.CENTER_VERTICAL));
            
            checkLayout.setOnClickListener(v -> {
                checkBox.setChecked(!checkBox.isChecked(), true);
                this.enableAfterInstallation = checkBox.isChecked();
            });
            
            ScaleStateListAnimator.apply(checkLayout, 0.05f, 1.2f);
            checkLayout.setBackground(Theme.createRadSelectorDrawable(getThemedColor(Theme.key_listSelector), 8, 8));
            linearLayout.addView(checkLayout, LayoutHelper.createLinear(-2, -2, Gravity.CENTER, 0, 0, 0, 8));
        }

        ImageView openBtn = new ImageView(context);
        ScaleStateListAnimator.apply(openBtn, 0.15f, 1.5f);
        openBtn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.msg_openin).mutate());
        openBtn.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
        openBtn.setScaleType(ImageView.ScaleType.CENTER);
        openBtn.setOnClickListener(v -> openSourceFile(params, baseFragment));
        openBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), 1, AndroidUtilities.dp(20.0f)));
        container.addView(openBtn, LayoutHelper.createFrame(40, 40.0f, Gravity.TOP | Gravity.END, 0.0f, 16.0f, 16.0f, 0.0f));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setClipChildren(false);
        scrollView.setClipToPadding(false);
        scrollView.addView(container);
        setCustomView(scrollView);
    }

    private void showIncompatibleHint(View anchor, PluginInstallParams params) {
        if (currentHint != null) {
            currentHint.hide();
            currentHint = null;
        }
        
        HintView2 hint = new HintView2(getContext(), 3);
        hint.setMultilineText(true);
        hint.setBgColor(getThemedColor(Theme.key_undo_background));
        hint.setTextColor(getThemedColor(Theme.key_undo_infoColor));
        
        hint.setText(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.PluginIncompatibleInfo)));
        
        hint.setTextAlign(Layout.Alignment.ALIGN_CENTER);
        hint.allowBlur(true);
        hint.setRounding(12.0f);
        hint.setMaxWidthPx(HintView2.cutInFancyHalf(hint.getText(), hint.getTextPaint()));
        
        this.container.addView(hint, LayoutHelper.createFrame(-1, 100.0f, Gravity.TOP | Gravity.LEFT, 32.0f, 0.0f, 32.0f, 0.0f));
        this.currentHint = hint;
        
        this.container.post(() -> {
            int[] anchorPos = new int[2];
            anchor.getLocationInWindow(anchorPos);
            int[] containerPos = new int[2];
            this.container.getLocationInWindow(containerPos);
            
            int relativeY = anchorPos[1] - containerPos[1];
            int relativeX = anchorPos[0] - containerPos[0];
            
            hint.setTranslationY(relativeY - AndroidUtilities.dp(100.0f) - AndroidUtilities.dp(6.0f));
            
            float jointX = -AndroidUtilities.dp(32.0f) + relativeX + (anchor.getMeasuredWidth() / 2.0f);
            hint.setJointPx(0.0f, jointX);
            
            hint.setDuration(5500L);
            hint.show();
        });
    }

    private void installPlugin(PluginInstallParams params, PluginsController.PluginValidationResult result, BaseFragment fragment, boolean update) {
        dismiss();
        PythonPluginsEngine engine = (PythonPluginsEngine) PluginsController.engines.get(PluginsConstants.PYTHON);
        if (engine == null) return;

        engine.loadPluginFromFile(params.filePath, result.plugin, error -> {
            AndroidUtilities.runOnUIThread(() -> {
                if (error != null) {
                    BulletinFactory.of(fragment).createSimpleBulletin(
                        R.raw.error, 
                        LocaleController.formatString(R.string.PluginInstallError, result.plugin.getName()), 
                        LocaleUtils.createCopySpan(fragment), 
                        () -> AndroidUtilities.addToClipboard(error)
                    ).show();
                } else if (enableAfterInstallation) {
                    PluginsController.getInstance().setPluginEnabled(result.plugin.getId(), true, err -> {
                        if (err == null) {
                            showSuccessBulletin(fragment, result.plugin, update);
                        } else {
                            BulletinFactory.of(fragment).createSimpleBulletin(
                                R.raw.error, 
                                LocaleController.formatString(R.string.PluginInstalledButFailedToEnable, result.plugin.getName()),
                                LocaleUtils.createCopySpan(fragment),
                                () -> AndroidUtilities.addToClipboard(err)
                            ).show();
                        }
                    });
                } else {
                    showSuccessBulletin(fragment, result.plugin, update);
                }
            });
        });
    }

    private void openSourceFile(PluginInstallParams params, BaseFragment fragment) {
        dismiss();
        File file = new File(params.filePath);
        if (file.exists()) {
            AndroidUtilities.openForView(file, file.getName(), "text/plain", fragment.getParentActivity(), fragment.getResourceProvider(), false);
        }
    }

    private void showSuccessBulletin(BaseFragment fragment, Plugin plugin, boolean update) {
        BulletinFactory factory = BulletinFactory.of(fragment);
        String name = plugin.getName();
        String text = LocaleController.formatString(update ? R.string.PluginUpdated : R.string.PluginInstalled, name);

        if (plugin.getPack() == null || plugin.getIndex() < 0) {
            factory.createSimpleBulletin(R.raw.contact_check, AndroidUtilities.replaceTags(text)).show();
            return;
        }

        TLRPC.TL_inputStickerSetShortName stickerSet = new TLRPC.TL_inputStickerSetShortName();
        stickerSet.short_name = plugin.getPack();
        
        AtomicBoolean shown = new AtomicBoolean(false);
        Runnable fallback = () -> {
            if (!shown.getAndSet(true)) {
                factory.createSimpleBulletin(R.raw.contact_check, text).show();
            }
        };
        AndroidUtilities.runOnUIThread(fallback, 300L);

        MediaDataController.getInstance(UserConfig.selectedAccount).getStickerSet(stickerSet, 0, true, (res) -> {
            AndroidUtilities.runOnUIThread(() -> {
                if (shown.get()) return;
                
                TLRPC.TL_messages_stickerSet set = (TLRPC.TL_messages_stickerSet) res;
                if (set != null && set.documents != null && plugin.getIndex() < set.documents.size()) {
                    TLRPC.Document doc = set.documents.get(plugin.getIndex());
                    if (doc != null && !shown.getAndSet(true)) {
                        AndroidUtilities.cancelRunOnUIThread(fallback);
                        factory.createSimpleBulletin(doc, AndroidUtilities.replaceTags(text)).show();
                    }
                }
            });
        });
    }

    @Override
    public void dismiss() {
        if (currentHint != null) {
            currentHint.hide();
            currentHint = null;
        }
        super.dismiss();
    }
    
    @Override
    protected void onSwipeStarts() {
        if (currentHint != null) {
            currentHint.hide();
            currentHint = null;
        }
    }

    public static class PluginInstallParams {
        public String filePath;
        public boolean incompatible;

        public PluginInstallParams(String path, boolean incompatible) {
            this.filePath = path;
            this.incompatible = incompatible;
        }

        public static PluginInstallParams of(MessageObject message) {
            String path = ChatUtils.getInstance().getPathToMessage(message);
            boolean incompatible = pluginCompatibilityCheck(path);
            return new PluginInstallParams(path, incompatible);
        }

        private static boolean pluginCompatibilityCheck(String path) {
            if (path == null) return false;
            File file = new File(path);
            if (!file.exists() || !file.canRead()) return false;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // это очень крутая проверка
                    if (line.contains("from com.exteragram.messenger")) {
                        return true;
                    }
                }
            } catch (Exception e) {}
            return false;
        }
    }
}