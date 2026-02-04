package tw.nekomimi.nekogram.plugins.ui.components.templates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import org.telegram.messenger.Utilities;

public class UniversalView extends View {
    private UniversalViewDelegate delegate;

    public interface UniversalViewDelegate {
        default void onAttachedToWindow() {}
        default void onDetachedFromWindow() {}
        
        void onDraw(Canvas canvas, Utilities.Callback<Canvas> callback);
        void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info, Utilities.Callback<AccessibilityNodeInfo> callback);
        void onMeasure(int widthMeasureSpec, int heightMeasureSpec, Utilities.Callback2<Integer, Integer> callback);
        boolean onTouchEvent(MotionEvent event, Utilities.CallbackReturn<MotionEvent, Boolean> callback);
    }

    public UniversalView(Context context) {
        super(context);
    }

    public UniversalView(Context context, UniversalViewDelegate delegate) {
        super(context);
        this.delegate = delegate;
    }

    public UniversalViewDelegate getDelegate() {
        return this.delegate;
    }

    public void setDelegate(UniversalViewDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.delegate != null) {
            this.delegate.onDraw(canvas, super::onDraw);
        } else {
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.delegate != null) {
            this.delegate.onAttachedToWindow();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.delegate != null) {
            this.delegate.onDetachedFromWindow();
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (this.delegate != null) {
            return this.delegate.onTouchEvent(event, super::onTouchEvent);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.delegate != null) {
            this.delegate.onMeasure(widthMeasureSpec, heightMeasureSpec, (w, h) -> super.onMeasure(w, h));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        if (this.delegate != null) {
            this.delegate.onInitializeAccessibilityNodeInfo(info, super::onInitializeAccessibilityNodeInfo);
        } else {
            super.onInitializeAccessibilityNodeInfo(info);
        }
    }
}