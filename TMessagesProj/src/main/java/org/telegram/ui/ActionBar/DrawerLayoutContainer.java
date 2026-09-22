/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.ActionBar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.Keep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;

public class DrawerLayoutContainer extends FrameLayout {

    private INavigationLayout parentActionBarLayout;
    private ActionBarLayout actionBarLayout;
    private boolean inLayout;
    private FrameLayout drawerLayout;
    private View drawerListView;
    private boolean allowOpenDrawer;
    private boolean allowOpenDrawerBySwipe = true;
    private float drawerPosition;
    private boolean drawerOpened;
    private float scrimOpacity;
    private final Paint scrimPaint = new Paint();
    private final Paint shadowPaint = new Paint();
    private AnimatorSet currentAnimation;
    private boolean maybeStartTracking;
    private boolean startedTracking;
    private boolean beginTrackingSent;
    private int startedTrackingX;
    private int startedTrackingY;
    private int startedTrackingPointerId;
    private VelocityTracker velocityTracker;
    private final android.graphics.Rect rect = new android.graphics.Rect();
    private Runnable onDrawerOpening;

    public DrawerLayoutContainer(Context context) {
        super(context);

        ViewCompat.setOnApplyWindowInsetsListener(this, this::onApplyWindowInsets);
        setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public void setParentActionBarLayout(INavigationLayout layout) {
        parentActionBarLayout = layout;
    }

    public void setActionBarLayout(ActionBarLayout actionBarLayout) {
        this.actionBarLayout = actionBarLayout;
    }

    public void setDrawerLayout(FrameLayout layout, View drawerListView) {
        drawerLayout = layout;
        this.drawerListView = drawerListView;
        addView(drawerLayout);
        drawerLayout.setVisibility(INVISIBLE);
    }

    public FrameLayout getDrawerLayout() {
        return drawerLayout;
    }

    public void setOnDrawerOpening(Runnable callback) {
        onDrawerOpening = callback;
    }

    public void setAllowOpenDrawer(boolean value, boolean animated) {
        allowOpenDrawer = value;
        if (!allowOpenDrawer && drawerPosition != 0) {
            if (!animated) {
                setDrawerPosition(0);
                onDrawerAnimationEnd(false);
            } else {
                closeDrawer(true);
            }
        }
    }

    public boolean isDrawerOpened() {
        return drawerOpened;
    }

    @Keep
    public void setDrawerPosition(float value) {
        if (drawerLayout == null) {
            return;
        }
        drawerPosition = value;
        int width = drawerLayout.getMeasuredWidth();
        if (width > 0) {
            if (drawerPosition > width) {
                drawerPosition = width;
            } else if (drawerPosition < 0) {
                drawerPosition = 0;
            }
        }
        drawerLayout.setTranslationX(drawerPosition);
        int visibility = drawerPosition > 0 ? VISIBLE : INVISIBLE;
        if (drawerLayout.getVisibility() != visibility) {
            drawerLayout.setVisibility(visibility);
        }
        setScrimOpacity(width == 0 ? 0 : drawerPosition / width);
    }

    @Keep
    public float getDrawerPosition() {
        return drawerPosition;
    }

    public void openDrawer(boolean fast) {
        if (!allowOpenDrawer || drawerLayout == null) {
            return;
        }
        if (onDrawerOpening != null) {
            onDrawerOpening.run();
        }
        if (drawerLayout.getMeasuredWidth() == 0) {
            drawerLayout.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(320), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
        }
        cancelCurrentAnimation();
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(this, "drawerPosition", drawerLayout.getMeasuredWidth()));
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setDuration(fast ? 150 : 250);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                onDrawerAnimationEnd(true);
            }
        });
        animatorSet.start();
        currentAnimation = animatorSet;
    }

    public void closeDrawer(boolean fast) {
        if (drawerLayout == null) {
            return;
        }
        cancelCurrentAnimation();
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(this, "drawerPosition", 0));
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setDuration(fast ? 150 : 250);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                onDrawerAnimationEnd(false);
            }
        });
        animatorSet.start();
        currentAnimation = animatorSet;
    }

    private void cancelCurrentAnimation() {
        if (currentAnimation != null) {
            currentAnimation.cancel();
            currentAnimation = null;
        }
    }

    private void onDrawerAnimationEnd(boolean opened) {
        startedTracking = false;
        currentAnimation = null;
        drawerOpened = opened;
        if (drawerLayout != null) {
            drawerLayout.setVisibility(opened || drawerPosition > 0 ? VISIBLE : INVISIBLE);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void setScrimOpacity(float value) {
        scrimOpacity = value;
        invalidate();
    }

    private void moveDrawerByX(float dx) {
        setDrawerPosition(drawerPosition + dx);
    }

    public boolean isDrawCurrentPreviewFragmentAbove() {
        return false;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (drawerLayout == null || parentActionBarLayout == null || parentActionBarLayout.checkTransitionAnimation()) {
            return false;
        }
        if (drawerOpened && ev != null && ev.getX() > drawerPosition && !startedTracking) {
            if (ev.getAction() == MotionEvent.ACTION_UP) {
                closeDrawer(false);
            }
            return true;
        }
        boolean stackOk = parentActionBarLayout.getFragmentStack().size() == 1 && parentActionBarLayout.allowSwipe();
        boolean sheetOk = parentActionBarLayout.getLastFragment() == null || parentActionBarLayout.getLastFragment().getLastSheet() == null || !parentActionBarLayout.getLastFragment().getLastSheet().attachedToParent();
        if ((allowOpenDrawerBySwipe || drawerOpened) && allowOpenDrawer && stackOk && sheetOk) {
            if (ev != null && (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_MOVE) && !startedTracking && !maybeStartTracking) {
                parentActionBarLayout.getView().getHitRect(rect);
                startedTrackingX = (int) ev.getX();
                startedTrackingY = (int) ev.getY();
                if (rect.contains(startedTrackingX, startedTrackingY) && (drawerOpened || startedTrackingX < AndroidUtilities.dp(24))) {
                    startedTrackingPointerId = ev.getPointerId(0);
                    maybeStartTracking = true;
                    cancelCurrentAnimation();
                    if (velocityTracker != null) {
                        velocityTracker.clear();
                    }
                }
            } else if (ev != null && ev.getAction() == MotionEvent.ACTION_MOVE && ev.getPointerId(0) == startedTrackingPointerId) {
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                }
                float dx = ev.getX() - startedTrackingX;
                float dy = Math.abs(ev.getY() - startedTrackingY);
                velocityTracker.addMovement(ev);
                if (maybeStartTracking && !startedTracking && ((dx > 0 && dx / 3f > dy && Math.abs(dx) >= AndroidUtilities.getPixelsInCM(0.2f, true)) || (drawerOpened && dx < 0 && Math.abs(dx) >= dy && Math.abs(dx) >= AndroidUtilities.getPixelsInCM(0.4f, true)))) {
                    maybeStartTracking = false;
                    startedTracking = true;
                    startedTrackingX = (int) ev.getX();
                    requestDisallowInterceptTouchEvent(true);
                } else if (startedTracking) {
                    moveDrawerByX(dx);
                    startedTrackingX = (int) ev.getX();
                }
            } else if (ev == null || (ev.getPointerId(0) == startedTrackingPointerId && (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_POINTER_UP))) {
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                }
                velocityTracker.computeCurrentVelocity(1000);
                if (startedTracking || (drawerLayout.getMeasuredWidth() > 0 && drawerPosition != 0 && drawerPosition != drawerLayout.getMeasuredWidth())) {
                    float velX = velocityTracker.getXVelocity();
                    boolean backAnimation = drawerPosition < drawerLayout.getMeasuredWidth() / 2f && velX < 3500 || velX < 0 && Math.abs(velX) >= 3500;
                    if (!backAnimation) {
                        openDrawer(Math.abs(velX) >= 3500);
                    } else {
                        closeDrawer(Math.abs(velX) >= 3500);
                    }
                }
                startedTracking = false;
                maybeStartTracking = false;
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
            }
            return startedTracking;
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (parentActionBarLayout != null && parentActionBarLayout.checkTransitionAnimation()) {
            return true;
        }
        return onTouchEvent(ev);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        inLayout = true;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            try {
                if (child == drawerLayout) {
                    child.layout(-child.getMeasuredWidth(), lp.topMargin + getPaddingTop(), 0, lp.topMargin + child.getMeasuredHeight() + getPaddingTop());
                } else {
                    child.layout(lp.leftMargin, lp.topMargin + getPaddingTop(), lp.leftMargin + child.getMeasuredWidth(), lp.topMargin + child.getMeasuredHeight() + getPaddingTop());
                }
            } catch (Exception e) {
                FileLog.e(e);
                if (BuildVars.DEBUG_VERSION) {
                    throw e;
                }
            }
        }
        inLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!inLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);

        final int newDisplayWidth = widthSize
            - systemAndCutoutInsets.left
            - systemAndCutoutInsets.right;

        final int newDisplayHeight = heightSize
            - systemAndCutoutInsets.top
            - systemAndCutoutInsets.bottom;

        AndroidUtilities.displaySize.x = newDisplayWidth;
        AndroidUtilities.displaySize.y = newDisplayHeight;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (child == drawerLayout) {
                int drawerWidth = lp.width > 0 ? lp.width : AndroidUtilities.dp(320);
                child.measure(MeasureSpec.makeMeasureSpec(drawerWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY));
                continue;
            }
            final int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
            final int contentHeightSpec;
            if (lp.height > 0) {
                contentHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            } else {
                contentHeightSpec = MeasureSpec.makeMeasureSpec(heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
            }
            if (child instanceof ActionBarLayout) {
                ActionBarLayout actionBarLayout = (ActionBarLayout) child;
                //fix keyboard measuring
                if (actionBarLayout.storyViewerAttached()) {
                    child.forceLayout();
                }
            }
            child.measure(contentWidthSpec, contentHeightSpec);
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (actionBarLayout != null && actionBarLayout.getParent() == this) {
            actionBarLayout.parentDraw(this, canvas);
        }

        super.dispatchDraw(canvas);
        if (scrimOpacity > 0) {
            scrimPaint.setColor(0x99000000);
            scrimPaint.setAlpha((int) (0x99 * scrimOpacity));
            canvas.drawRect(drawerPosition, 0, getWidth(), getHeight(), scrimPaint);
            shadowPaint.setShader(new LinearGradient(drawerPosition - AndroidUtilities.dp(6), 0, drawerPosition, 0, 0x00000000, 0x44000000, Shader.TileMode.CLAMP));
            canvas.drawRect(drawerPosition - AndroidUtilities.dp(6), 0, drawerPosition, getHeight(), shadowPaint);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private final Paint internalNavbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Paint getInternalNavbarPaint() {
        return internalNavbarPaint;
    }

    public void setInternalNavigationBarColor(int color) {
        if (internalNavbarPaint.getColor() != color) {
            internalNavbarPaint.setColor(color);
            invalidate();

            for (int a = 0, N = getChildCount(); a < N; a++) {
                getChildAt(a).invalidate();
            }
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (lastWindowInsetsCompat != null) {
            dispatchApplyWindowInsetsInternal(child, lastWindowInsetsCompat);
        }
    }

    private @Nullable WindowInsetsCompat lastWindowInsetsCompat;
    private @NonNull Insets systemAndCutoutInsets = Insets.NONE;
    private @NonNull Insets systemAndCutoutAndImeInsets = Insets.NONE;

    private void dispatchApplyWindowInsetsInternal(View child, WindowInsetsCompat insets) {
        boolean canApplyInsets = child instanceof ActionBarLayout || child.getTag() == null;
        if (canApplyInsets) {
            ViewCompat.dispatchApplyWindowInsets(child, insets);
        }
    }

    @NonNull
    private WindowInsetsCompat onApplyWindowInsets(@NonNull View ignoredV, @NonNull WindowInsetsCompat insets) {
        lastWindowInsetsCompat = insets;

        final Insets systemInsets = AndroidUtilities.getDefaultWindowInsets(insets, false);
        final Insets systemAndImeInsets = AndroidUtilities.getDefaultWindowInsets(insets, true);

        if (!systemAndCutoutInsets.equals(systemInsets) || !systemAndCutoutAndImeInsets.equals(systemAndImeInsets)) {
            AndroidUtilities.statusBarHeight = systemInsets.top;
            AndroidUtilities.navigationBarHeight = systemInsets.bottom;

            systemAndCutoutInsets = systemInsets;
            systemAndCutoutAndImeInsets = systemAndImeInsets;
            requestLayout();
        }

        for (int a = 0, N = getChildCount(); a < N; a++) {
            final View child = getChildAt(a);
            dispatchApplyWindowInsetsInternal(child, insets);
        }

        invalidate();
        return WindowInsetsCompat.CONSUMED;
    }
}
