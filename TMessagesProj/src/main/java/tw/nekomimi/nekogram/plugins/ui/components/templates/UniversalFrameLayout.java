package tw.nekomimi.nekogram.plugins.ui.components.templates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

import org.telegram.messenger.Utilities;

public class UniversalFrameLayout extends FrameLayout {
    private UniversalFrameLayoutListener universalFrameLayoutListener;

    public interface UniversalFrameLayoutListener {
        void dispatchDraw(Canvas canvas, Utilities.Callback<Canvas> callback);
        boolean drawChild(Canvas canvas, View view, long drawingTime, Utilities.Callback3Return<Canvas, View, Long, Boolean> callback);
        void invalidate(int l, int t, int r, int b, Runnable superCall);
        void invalidate(Runnable superCall);
        void onAttachedToWindow(Runnable superCall);
        void onDetachedFromWindow(Runnable superCall);
        void onDraw(Canvas canvas, Utilities.Callback<Canvas> callback);
        void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info, Utilities.Callback<AccessibilityNodeInfo> callback);
        boolean onInterceptTouchEvent(MotionEvent ev, Utilities.CallbackReturn<MotionEvent, Boolean> callback);
        void onLayout(boolean changed, int left, int top, int right, int bottom, Utilities.Callback5<Boolean, Integer, Integer, Integer, Integer> callback);
        void onMeasure(int widthMeasureSpec, int heightMeasureSpec, Utilities.Callback2<Integer, Integer> callback);
        boolean onTouchEvent(MotionEvent event, Utilities.CallbackReturn<MotionEvent, Boolean> callback);
        void requestLayout(Runnable superCall);
        void setTranslationX(float translationX, Utilities.Callback<Float> callback);
        void setTranslationY(float translationY, Utilities.Callback<Float> callback);
        void setVisibility(int visibility, Utilities.Callback<Integer> callback);
    }

    public UniversalFrameLayout(Context context) {
        super(context);
        this.universalFrameLayoutListener = null;
    }

    public UniversalFrameLayout(Context context, UniversalFrameLayoutListener listener) {
        super(context);
        this.universalFrameLayoutListener = listener;
    }

    public void setUniversalFrameLayoutListener(UniversalFrameLayoutListener listener) {
        this.universalFrameLayoutListener = listener;
    }

    public UniversalFrameLayoutListener getUniversalFrameLayoutListener() {
        return this.universalFrameLayoutListener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.onLayout(changed, left, top, right, bottom, (c, l, t, r, b) -> super.onLayout(c, l, t, r, b));
        } else {
            super.onLayout(changed, left, top, right, bottom);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.onMeasure(widthMeasureSpec, heightMeasureSpec, (w, h) -> super.onMeasure(w, h));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void setTranslationX(float translationX) {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.setTranslationX(translationX, super::setTranslationX);
        } else {
            super.setTranslationX(translationX);
        }
    }

    @Override
    public void setTranslationY(float translationY) {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.setTranslationY(translationY, super::setTranslationY);
        } else {
            super.setTranslationY(translationY);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.onAttachedToWindow(super::onAttachedToWindow);
        } else {
            super.onAttachedToWindow();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.onDetachedFromWindow(super::onDetachedFromWindow);
        } else {
            super.onDetachedFromWindow();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.dispatchDraw(canvas, super::dispatchDraw);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    @Override
    public void requestLayout() {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.requestLayout(super::requestLayout);
        } else {
            super.requestLayout();
        }
    }

    @Override
    public void invalidate() {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.invalidate(super::invalidate);
        } else {
            super.invalidate();
        }
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.invalidate(l, t, r, b, () -> super.invalidate(l, t, r, b));
        } else {
            super.invalidate(l, t, r, b);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.onDraw(canvas, super::onDraw);
        } else {
            super.onDraw(canvas);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.onInitializeAccessibilityNodeInfo(info, super::onInitializeAccessibilityNodeInfo);
        } else {
            super.onInitializeAccessibilityNodeInfo(info);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.universalFrameLayoutListener != null) {
            return this.universalFrameLayoutListener.onInterceptTouchEvent(ev, super::onInterceptTouchEvent);
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (this.universalFrameLayoutListener != null) {
            return this.universalFrameLayoutListener.onTouchEvent(event, super::onTouchEvent);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (this.universalFrameLayoutListener != null) {
            return this.universalFrameLayoutListener.drawChild(canvas, child, drawingTime, (c, v, t) -> super.drawChild(c, v, t));
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void setVisibility(int visibility) {
        if (this.universalFrameLayoutListener != null) {
            this.universalFrameLayoutListener.setVisibility(visibility, super::setVisibility);
        } else {
            super.setVisibility(visibility);
        }
    }
}