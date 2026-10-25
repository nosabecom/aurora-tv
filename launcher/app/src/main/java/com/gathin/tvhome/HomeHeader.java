package com.gathin.tvhome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;
import android.widget.TextView;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

/** One stable layout: scale the left-hand text and scroll the grid without changing fonts or reflowing. */
final class HomeHeader {
    private final ViewGroup root, block, header;
    private final View viewport, metadata;
    private float metadataFromY, metadataToY, metadataScale;
    private final RecyclerView grid;
    private final Runnable initializedCallback;
    private final List<Text> texts = new ArrayList<>();
    private final Rect clip = new Rect();
    private final float density;
    private final ViewTreeObserver.OnPreDrawListener initializer;
    private ValueAnimator animator;
    private boolean initialized, running;
    private float progress, target;
    private int expandedHeight, compactHeight, totalScroll, appliedScroll;

    HomeHeader(ViewGroup root, RecyclerView grid, boolean collapsible, Runnable initializedCallback) {
        this.root = root;
        this.grid = grid;
        this.initializedCallback = initializedCallback;
        block = root.findViewById(R.id.leftBlock);
        header = (ViewGroup) block.getParent();
        viewport = (View) grid.getParent();
        metadata = (View) root.findViewById(R.id.date).getParent();
        density = root.getResources().getDisplayMetrics().density;
        viewport.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (initialized) render();
        });
        int[] ids = {R.id.greeting, R.id.clock, R.id.ampm};
        for (int id : ids) {
            TextView view = root.findViewById(id);
            texts.add(new Text(view));
            for (ViewGroup parent = (ViewGroup) view.getParent(); parent != root; parent = (ViewGroup) parent.getParent()) {
                parent.setClipChildren(false);
                parent.setClipToPadding(false);
            }
        }
        initializer = this::initialize;
        if (collapsible) root.getViewTreeObserver().addOnPreDrawListener(initializer);
        root.<View>findViewById(R.id.clock).addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (initialized && (r - l != or - ol || b - t != ob - ot)) {
                measureText();
                render();
            }
        });
    }

    private boolean initialize() {
        if (block.getHeight() == 0) return true;
        root.getViewTreeObserver().removeOnPreDrawListener(initializer);
        expandedHeight = header.getHeight();
        measureText();
        // RelativeLayout caps an oversized child to its own height. Keep the full-size text in a
        // root overlay instead, so shrinking the reserved header never clips or remeasures its glyphs.
        Rect location = new Rect(0, 0, block.getWidth(), block.getHeight());
        root.offsetDescendantRectToMyCoords(block, location);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(block.getWidth(), block.getHeight());
        bp.leftMargin = location.left;
        bp.topMargin = location.top;
        header.removeView(block);
        root.addView(block, bp);
        View stats = root.findViewById(R.id.stats);
        header.removeView(stats);
        FrameLayout.LayoutParams statsParams = new FrameLayout.LayoutParams(block.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT);
        statsParams.leftMargin = location.left;
        statsParams.topMargin = location.bottom + Math.round(2 * density);
        root.addView(stats, statsParams);
        ViewGroup.LayoutParams hp = header.getLayoutParams();
        hp.height = compactHeight;
        header.setLayoutParams(hp);
        initialized = true;
        grid.setPadding(grid.getPaddingLeft(), grid.getPaddingTop() + expandedInset(),
                grid.getPaddingRight(), grid.getPaddingBottom());
        GridLayoutManager manager = (GridLayoutManager) grid.getLayoutManager();
        if (manager != null && manager.getOrientation() == RecyclerView.VERTICAL) {
            View child = grid.getFocusedChild();
            int position = child == null ? 0 : Math.max(0, grid.getChildAdapterPosition(child));
            int offset = progress == 0 ? 0 : Math.round(36 * density) - grid.getPaddingTop();
            manager.scrollToPositionWithOffset(position, offset);
        }
        render();
        grid.post(() -> {
            grid.requestLayout();
            initializedCallback.run();
        });
        return false; // Finish this single initial re-layout before presenting the first frame.
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, root.getResources().getDisplayMetrics());
    }

    private void measureText() {
        float[] sizes = {13, 44, 10};
        for (int i = 0; i < texts.size(); i++) {
            Text text = texts.get(i);
            View view = text.view;
            int x = 0, y = 0;
            while (view != block) {
                x += view.getLeft(); y += view.getTop();
                view = (View) view.getParent();
            }
            text.x = x; text.y = y;
            text.toX = x;
            text.scale = sp(sizes[i]) / text.view.getTextSize();
        }
        Text greeting = texts.get(0), clock = texts.get(1), ampm = texts.get(2);
        greeting.toY = greeting.y;
        clock.toY = greeting.view.getVisibility() == View.GONE ? 0
                : greeting.y + greeting.view.getHeight() * greeting.scale + 4 * density;
        ampm.toX = clock.x + clock.view.getWidth() * clock.scale + 6 * density;
        ampm.toY = clock.toY + clock.view.getBaseline() * clock.scale - ampm.view.getBaseline() * ampm.scale;
        metadataToY = clock.toY + clock.view.getHeight() * clock.scale + 6 * density;
        metadataFromY = 0;
        for (View v = metadata; v != block; v = (View) v.getParent()) metadataFromY += v.getTop();
        metadataScale = sp(11) / ((TextView) root.findViewById(R.id.date)).getTextSize();
        compactHeight = Math.max(Math.round(metadataToY + metadata.getHeight() * metadataScale + 4 * density),
                root.findViewById(R.id.topButtons).getHeight());
    }

    int expandedInset() { return initialized ? Math.max(0, expandedHeight - compactHeight) : 0; }
    boolean isRunning() { return running; }
    float fraction() { return progress; }

    void apply(boolean compact, boolean animate, int scrollBy, Runnable finished) {
        stopAnimation(); // Retain the current frame when reversing direction.
        target = compact ? 1 : 0;
        totalScroll = scrollBy;
        appliedScroll = 0;
        if (!initialized || !animate) {
            progress = target;
            grid.scrollBy(0, totalScroll);
            render();
            return;
        }
        float start = progress;
        running = true;
        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(Math.max(120, Math.round(360 * Math.abs(target - start))));
        animator.setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f));
        animator.addUpdateListener(value -> {
            float amount = (float) value.getAnimatedValue();
            progress = start + (target - start) * amount;
            int scroll = Math.round(totalScroll * amount);
            grid.scrollBy(0, scroll - appliedScroll);
            appliedScroll = scroll;
            render();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                animator = null;
                running = false;
                finished.run();
            }
        });
        animator.start();
    }

    private void render() {
        if (!initialized) return;
        for (Text text : texts) {
            text.view.setPivotX(0); text.view.setPivotY(0);
            float scale = 1 + (text.scale - 1) * progress;
            text.view.setScaleX(scale); text.view.setScaleY(scale);
            text.view.setTranslationX((text.toX - text.x) * progress);
            text.view.setTranslationY((text.toY - text.y) * progress);
        }
        metadata.setPivotX(0); metadata.setPivotY(0);
        float dateScale = 1 + (metadataScale - 1) * progress;
        metadata.setScaleX(dateScale); metadata.setScaleY(dateScale);
        metadata.setTranslationY((metadataToY - metadataFromY) * progress);
        clip.set(0, Math.round(expandedInset() * (1 - progress)), viewport.getWidth(), viewport.getHeight());
        viewport.setClipBounds(clip);
        grid.invalidate();
    }

    private void stopAnimation() {
        if (animator != null) { animator.removeAllListeners(); animator.cancel(); animator = null; }
        running = false;
    }

    /** Finish the selected state when leaving Home; a return cannot retain a half-collapsed header. */
    void cancel() {
        if (!running) return;
        stopAnimation();
        progress = target;
        grid.scrollBy(0, totalScroll - appliedScroll);
        render();
    }

    private static final class Text {
        final TextView view;
        float x, y, toX, toY, scale;
        Text(TextView view) { this.view = view; }
    }
}
