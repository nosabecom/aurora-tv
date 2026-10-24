package com.gathin.tvhome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Two header sizes. Animate transforms after one layout, rather than remeasuring the grid every frame. */
final class HomeHeader {
    private final ViewGroup root, block;
    private final View controls, grid;
    private final RelativeLayout.LayoutParams homeBlock, homeControls;
    private final List<TextView> texts = new ArrayList<>();
    private final List<Float> homeSizes = new ArrayList<>();
    private final List<Integer> homeGravities = new ArrayList<>();
    private final LinearLayout clockRow;
    private final TextView ampm;
    private final int homeAmpmMargin;
    private final float density;
    private ValueAnimator animator;
    private ViewTreeObserver.OnPreDrawListener pending;
    private boolean running;

    HomeHeader(ViewGroup root, View grid) {
        this.root = root;
        this.grid = grid;
        block = root.findViewById(R.id.leftBlock);
        controls = root.findViewById(R.id.topButtons);
        homeBlock = new RelativeLayout.LayoutParams((RelativeLayout.LayoutParams) block.getLayoutParams());
        homeControls = new RelativeLayout.LayoutParams((RelativeLayout.LayoutParams) controls.getLayoutParams());
        density = root.getResources().getDisplayMetrics().density;
        int[] ids = {R.id.greeting, R.id.clock, R.id.ampm, R.id.date, R.id.shamsiDate, R.id.lunarDate};
        for (int id : ids) {
            TextView text = root.findViewById(id);
            texts.add(text);
            homeSizes.add(text.getTextSize());
            homeGravities.add(text.getGravity());
            // Moving/scaling glyphs must be allowed outside their newly compact layout bounds.
            for (ViewGroup parent = (ViewGroup) text.getParent(); parent != root; parent = (ViewGroup) parent.getParent()) {
                parent.setClipChildren(false);
                parent.setClipToPadding(false);
            }
        }
        clockRow = (LinearLayout) root.findViewById(R.id.clock).getParent();
        ampm = root.findViewById(R.id.ampm);
        homeAmpmMargin = ((LinearLayout.LayoutParams) ampm.getLayoutParams()).bottomMargin;
    }

    boolean isRunning() { return running; }

    void apply(boolean compact, boolean animate, Runnable finished) {
        cancel();
        List<Pose> before = new ArrayList<>();
        for (TextView text : texts) before.add(new Pose(text, root));
        Rect oldGrid = bounds(grid, root), oldControls = bounds(controls, root);
        RelativeLayout.LayoutParams bp = new RelativeLayout.LayoutParams(homeBlock);
        RelativeLayout.LayoutParams cp = new RelativeLayout.LayoutParams(homeControls);
        if (compact) {
            bp.width = Math.round(250 * density);
            bp.removeRule(RelativeLayout.START_OF);
            bp.removeRule(RelativeLayout.ALIGN_PARENT_START);
            bp.addRule(RelativeLayout.ALIGN_PARENT_END);
            bp.setMarginEnd(0);
            cp.removeRule(RelativeLayout.ALIGN_PARENT_END);
            cp.addRule(RelativeLayout.ALIGN_PARENT_START);
        }
        block.setLayoutParams(bp);
        controls.setLayoutParams(cp);
        clockRow.setGravity(compact ? Gravity.RIGHT : Gravity.LEFT);
        float[] compactSp = {12, 32, 10, 10, 10, 10};
        for (int i = 0; i < texts.size(); i++) {
            TextView text = texts.get(i);
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX, compact
                    ? TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, compactSp[i], root.getResources().getDisplayMetrics())
                    : homeSizes.get(i));
            text.setGravity(compact ? Gravity.RIGHT : homeGravities.get(i));
        }
        LinearLayout.LayoutParams ap = (LinearLayout.LayoutParams) ampm.getLayoutParams();
        ap.bottomMargin = compact ? Math.round(6 * density) : homeAmpmMargin;
        ampm.setLayoutParams(ap);
        if (!animate || !root.isLaidOut()) return;
        running = true;
        pending = () -> {
            root.getViewTreeObserver().removeOnPreDrawListener(pending);
            pending = null;
            List<Pose> after = new ArrayList<>();
            for (TextView text : texts) after.add(new Pose(text, root));
            Rect newGrid = bounds(grid, root), newControls = bounds(controls, root);
            animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(320);
            animator.setInterpolator(new PathInterpolator(0.2f, 0f, 0.2f, 1f));
            animator.addUpdateListener(value -> {
                float remaining = 1 - (float) value.getAnimatedValue();
                for (int i = 0; i < texts.size(); i++) {
                    TextView text = texts.get(i);
                    Pose a = before.get(i), b = after.get(i);
                    text.setPivotX(b.pivotX);
                    text.setPivotY(text.getBaseline());
                    float scale = 1 + (a.size / b.size - 1) * remaining;
                    text.setScaleX(scale);
                    text.setScaleY(scale);
                    text.setTranslationX((a.x - b.x) * remaining);
                    text.setTranslationY((a.baseline - b.baseline) * remaining);
                }
                grid.setTranslationY((oldGrid.top - newGrid.top) * remaining);
                controls.setTranslationX((oldControls.left - newControls.left) * remaining);
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    resetTransforms();
                    running = false;
                    animator = null;
                    finished.run();
                }
            });
            animator.start();
            return true;
        };
        root.getViewTreeObserver().addOnPreDrawListener(pending);
    }

    void cancel() {
        if (pending != null && root.getViewTreeObserver().isAlive()) {
            root.getViewTreeObserver().removeOnPreDrawListener(pending);
            pending = null;
        }
        if (animator != null) { animator.removeAllListeners(); animator.cancel(); animator = null; }
        resetTransforms();
        running = false;
    }

    private void resetTransforms() {
        for (TextView text : texts) {
            text.setTranslationX(0); text.setTranslationY(0);
            text.setScaleX(1); text.setScaleY(1);
        }
        grid.setTranslationY(0);
        controls.setTranslationX(0);
    }

    private static Rect bounds(View view, ViewGroup root) {
        Rect r = new Rect(0, 0, view.getWidth(), view.getHeight());
        root.offsetDescendantRectToMyCoords(view, r);
        return r;
    }

    private static final class Pose {
        final float x, baseline, size, pivotX;
        Pose(TextView view, ViewGroup root) {
            Rect r = bounds(view, root);
            pivotX = view.getCompoundPaddingLeft() + (view.getLayout() == null ? 0 : view.getLayout().getLineLeft(0));
            x = r.left + pivotX;
            baseline = r.top + view.getBaseline();
            size = view.getTextSize();
        }
    }
}
