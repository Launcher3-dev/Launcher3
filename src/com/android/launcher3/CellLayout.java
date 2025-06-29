/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import static com.android.launcher3.dragndrop.DraggableView.DRAGGABLE_ICON;
import static com.android.launcher3.icons.IconNormalizer.ICON_VISIBLE_AREA_FACTOR;
import static com.android.launcher3.util.MultiTranslateDelegate.INDEX_REORDER_PREVIEW_OFFSET;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;

import com.android.app.animation.Interpolators;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.celllayout.CellPosMapper.CellPos;
import com.android.launcher3.celllayout.DelegatedCellDrawing;
import com.android.launcher3.celllayout.ItemConfiguration;
import com.android.launcher3.celllayout.ReorderAlgorithm;
import com.android.launcher3.celllayout.ReorderParameters;
import com.android.launcher3.celllayout.ReorderPreviewAnimation;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.dragndrop.DraggableView;
import com.android.launcher3.folder.PreviewBackground;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.LauncherAppWidgetInfo;
import com.android.launcher3.util.CellAndSpan;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.MSDLPlayerWrapper;
import com.android.launcher3.util.MultiTranslateDelegate;
import com.android.launcher3.util.ParcelableSparseArray;
import com.android.launcher3.util.Themes;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.widget.LauncherAppWidgetHostView;

import com.google.android.msdl.data.model.MSDLToken;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class CellLayout extends ViewGroup {
    private static final String TAG = "CellLayout";
    private static final boolean LOGD = true;

    /** The color of the "leave-behind" shape when a folder is opened from Hotseat. */
    private static final int FOLDER_LEAVE_BEHIND_COLOR = Color.argb(160, 245, 245, 245);

    protected final ActivityContext mActivity;
    @ViewDebug.ExportedProperty(category = "launcher")
    @Thunk int mCellWidth;
    @ViewDebug.ExportedProperty(category = "launcher")
    @Thunk int mCellHeight;
    private int mFixedCellWidth;
    private int mFixedCellHeight;
    @ViewDebug.ExportedProperty(category = "launcher")
    protected Point mBorderSpace;

    @ViewDebug.ExportedProperty(category = "launcher")
    protected int mCountX;
    @ViewDebug.ExportedProperty(category = "launcher")
    protected int mCountY;

    private boolean mDropPending = false;

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    @Thunk final int[] mTmpPoint = new int[2];
    @Thunk final int[] mTempLocation = new int[2];

    @Thunk final Rect mTempOnDrawCellToRect = new Rect();

    protected GridOccupancy mOccupied;
    public GridOccupancy mTmpOccupied;

    private OnTouchListener mInterceptTouchListener;

    private final ArrayList<DelegatedCellDrawing> mDelegatedCellDrawings = new ArrayList<>();
    final PreviewBackground mFolderLeaveBehind = new PreviewBackground(getContext());

    private static final int[] BACKGROUND_STATE_ACTIVE = new int[] { android.R.attr.state_active };
    private static final int[] BACKGROUND_STATE_DEFAULT = EMPTY_STATE_SET;
    protected final Drawable mBackground;

    // These values allow a fixed measurement to be set on the CellLayout.
    private int mFixedWidth = -1;
    private int mFixedHeight = -1;

    // If we're actively dragging something over this screen, mIsDragOverlapping is true
    private boolean mIsDragOverlapping = false;

    // These arrays are used to implement the drag visualization on x-large screens.
    // They are used as circular arrays, indexed by mDragOutlineCurrent.
    @Thunk final CellLayoutLayoutParams[] mDragOutlines = new CellLayoutLayoutParams[4];
    @Thunk final float[] mDragOutlineAlphas = new float[mDragOutlines.length];
    private final InterruptibleInOutAnimator[] mDragOutlineAnims =
            new InterruptibleInOutAnimator[mDragOutlines.length];

    // Used as an index into the above 3 arrays; indicates which is the most current value.
    private int mDragOutlineCurrent = 0;
    private final Paint mDragOutlinePaint = new Paint();

    @Thunk final ArrayMap<CellLayoutLayoutParams, Animator> mReorderAnimators = new ArrayMap<>();
    @Thunk final ArrayMap<Reorderable, ReorderPreviewAnimation> mShakeAnimators = new ArrayMap<>();

    private boolean mItemPlacementDirty = false;

    // Used to visualize the grid and drop locations
    private boolean mVisualizeCells = false;
    private boolean mVisualizeDropLocation = true;
    private RectF mVisualizeGridRect = new RectF();
    private Paint mVisualizeGridPaint = new Paint();
    private int mGridVisualizationRoundingRadius;
    private float mGridAlpha = 0f;
    private int mGridColor = 0;
    protected float mSpringLoadedProgress = 0f;
    private float mScrollProgress = 0f;

    // When a drag operation is in progress, holds the nearest cell to the touch point
    private final int[] mDragCell = new int[2];
    private final int[] mDragCellSpan = new int[2];

    private boolean mDragging = false;
    public boolean mHasOnLayoutBeenCalled = false;
    private boolean mPlayDragHaptics = false;

    private final TimeInterpolator mEaseOutInterpolator;
    protected final ShortcutAndWidgetContainer mShortcutsAndWidgets;
    @Px
    protected int mSpaceBetweenCellLayoutsPx = 0;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WORKSPACE, HOTSEAT, FOLDER})
    public @interface ContainerType{}
    public static final int WORKSPACE = 0;
    public static final int HOTSEAT = 1;
    public static final int FOLDER = 2;

    @ContainerType private final int mContainerType;

    public static final float DEFAULT_SCALE = 1f;

    public static final int MODE_SHOW_REORDER_HINT = 0;
    public static final int MODE_DRAG_OVER = 1;
    public static final int MODE_ON_DROP = 2;
    public static final int MODE_ON_DROP_EXTERNAL = 3;
    public static final int MODE_ACCEPT_DROP = 4;
    private static final boolean DESTRUCTIVE_REORDER = false;
    private static final boolean DEBUG_VISUALIZE_OCCUPIED = false;

    public static final float REORDER_PREVIEW_MAGNITUDE = 0.12f;
    public static final int REORDER_ANIMATION_DURATION = 150;
    @Thunk final float mReorderPreviewAnimationMagnitude;

    public final int[] mDirectionVector = new int[2];

    ItemConfiguration mPreviousSolution = null;

    private final Rect mTempRect = new Rect();

    private static final Paint sPaint = new Paint();

    private final MSDLPlayerWrapper mMSDLPlayerWrapper;

    // Related to accessible drag and drop
    DragAndDropAccessibilityDelegate mTouchHelper;

    CellLayoutContainer mCellLayoutContainer;

    public static final FloatProperty<CellLayout> SPRING_LOADED_PROGRESS =
            new FloatProperty<CellLayout>("spring_loaded_progress") {
                @Override
                public Float get(CellLayout cl) {
                    return cl.getSpringLoadedProgress();
                }

                @Override
                public void setValue(CellLayout cl, float progress) {
                    cl.setSpringLoadedProgress(progress);
                }
            };

    public CellLayout(Context context, CellLayoutContainer container) {
        this(context, (AttributeSet) null);
        this.mCellLayoutContainer = container;
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);
        mContainerType = a.getInteger(R.styleable.CellLayout_containerType, WORKSPACE);
        a.recycle();

        mMSDLPlayerWrapper = MSDLPlayerWrapper.INSTANCE.get(context);

        // A ViewGroup usually does not draw, but CellLayout needs to draw a rectangle to show
        // the user where a dragged item will land when dropped.
        setWillNotDraw(false);
        setClipToPadding(false);
        setClipChildren(false);
        mActivity = ActivityContext.lookupContext(context);
        DeviceProfile deviceProfile = mActivity.getDeviceProfile();

        resetCellSizeInternal(deviceProfile);

        mCountX = deviceProfile.inv.numColumns;
        mCountY = deviceProfile.inv.numRows;
        mOccupied =  new GridOccupancy(mCountX, mCountY);
        mTmpOccupied = new GridOccupancy(mCountX, mCountY);

        mFolderLeaveBehind.mDelegateCellX = -1;
        mFolderLeaveBehind.mDelegateCellY = -1;

        setAlwaysDrawnWithCacheEnabled(false);

        Resources res = getResources();

        mBackground = getContext().getDrawable(R.drawable.bg_celllayout);
        mBackground.setCallback(this);
        mBackground.setAlpha(0);

        mGridColor = Themes.getAttrColor(getContext(), R.attr.workspaceAccentColor);
        mGridVisualizationRoundingRadius =
                res.getDimensionPixelSize(R.dimen.grid_visualization_rounding_radius);
        mReorderPreviewAnimationMagnitude = (REORDER_PREVIEW_MAGNITUDE * deviceProfile.iconSizePx);

        // Initialize the data structures used for the drag visualization.
        mEaseOutInterpolator = Interpolators.DECELERATE_QUINT; // Quint ease out
        mDragCell[0] = mDragCell[1] = -1;
        mDragCellSpan[0] = mDragCellSpan[1] = -1;
        for (int i = 0; i < mDragOutlines.length; i++) {
            mDragOutlines[i] = new CellLayoutLayoutParams(0, 0, 0, 0);
        }
        mDragOutlinePaint.setColor(Themes.getAttrColor(context, R.attr.workspaceTextColor));

        // When dragging things around the home screens, we show a green outline of
        // where the item will land. The outlines gradually fade out, leaving a trail
        // behind the drag path.
        // Set up all the animations that are used to implement this fading.
        final int duration = res.getInteger(R.integer.config_dragOutlineFadeTime);
        final float fromAlphaValue = 0;
        final float toAlphaValue = (float)res.getInteger(R.integer.config_dragOutlineMaxAlpha);

        Arrays.fill(mDragOutlineAlphas, fromAlphaValue);

        for (int i = 0; i < mDragOutlineAnims.length; i++) {
            final InterruptibleInOutAnimator anim =
                    new InterruptibleInOutAnimator(duration, fromAlphaValue, toAlphaValue);
            anim.getAnimator().setInterpolator(mEaseOutInterpolator);
            final int thisIndex = i;
            anim.getAnimator().addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    // If an animation is started and then stopped very quickly, we can still
                    // get spurious updates we've cleared the tag. Guard against this.
                    mDragOutlineAlphas[thisIndex] = (Float) animation.getAnimatedValue();
                    CellLayout.this.invalidate();
                }
            });
            // The animation holds a reference to the drag outline bitmap as long is it's
            // running. This way the bitmap can be GCed when the animations are complete.
            mDragOutlineAnims[i] = anim;
        }

        mShortcutsAndWidgets = new ShortcutAndWidgetContainer(context, mContainerType);
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mCountX, mCountY,
                mBorderSpace);
        addView(mShortcutsAndWidgets);
    }

    public CellLayoutContainer getCellLayoutContainer() {
        return mCellLayoutContainer;
    }

    public void setCellLayoutContainer(CellLayoutContainer cellLayoutContainer) {
        mCellLayoutContainer = cellLayoutContainer;
    }

    /**
     * Sets or clears a delegate used for accessible drag and drop
     */
    public void setDragAndDropAccessibilityDelegate(DragAndDropAccessibilityDelegate delegate) {
        ViewCompat.setAccessibilityDelegate(this, delegate);

        mTouchHelper = delegate;
        int accessibilityFlag = mTouchHelper != null
                ? IMPORTANT_FOR_ACCESSIBILITY_YES : IMPORTANT_FOR_ACCESSIBILITY_NO;
        setImportantForAccessibility(accessibilityFlag);
        getShortcutsAndWidgets().setImportantForAccessibility(accessibilityFlag);
        // ExploreByTouchHelper sets focusability. Clear it when the delegate is cleared.
        setFocusable(delegate != null);
        // Invalidate the accessibility hierarchy
        if (getParent() != null) {
            getParent().notifySubtreeAccessibilityStateChanged(
                    this, this, AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE);
        }
    }

    /**
     * Returns the currently set accessibility delegate
     */
    public DragAndDropAccessibilityDelegate getDragAndDropAccessibilityDelegate() {
        return mTouchHelper;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // Always attempt to dispatch hover events to accessibility first.
        if (mTouchHelper != null && mTouchHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mTouchHelper != null
                || (mInterceptTouchListener != null && mInterceptTouchListener.onTouch(this, ev));
    }

    public void enableHardwareLayer(boolean hasLayer) {
        mShortcutsAndWidgets.setLayerType(hasLayer ? LAYER_TYPE_HARDWARE : LAYER_TYPE_NONE, sPaint);
    }

    public boolean isHardwareLayerEnabled() {
        return mShortcutsAndWidgets.getLayerType() == LAYER_TYPE_HARDWARE;
    }

    /**
     * Change sizes of cells
     *
     * @param width  the new width of the cells
     * @param height the new height of the cells
     */
    public void setCellDimensions(int width, int height) {
        mFixedCellWidth = mCellWidth = width;
        mFixedCellHeight = mCellHeight = height;
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mCountX, mCountY,
                mBorderSpace);
    }

    private void resetCellSizeInternal(DeviceProfile deviceProfile) {
        switch (mContainerType) {
            case FOLDER:
                mBorderSpace = new Point(deviceProfile.folderCellLayoutBorderSpacePx);
                break;
            case HOTSEAT:
                mBorderSpace = new Point(deviceProfile.hotseatBorderSpace,
                        deviceProfile.hotseatBorderSpace);
                break;
            case WORKSPACE:
            default:
                mBorderSpace = new Point(deviceProfile.cellLayoutBorderSpacePx);
                break;
        }

        mCellWidth = mCellHeight = -1;
        mFixedCellWidth = mFixedCellHeight = -1;
    }

    /**
     * Reset the cell sizes and border space
     */
    public void resetCellSize(DeviceProfile deviceProfile) {
        resetCellSizeInternal(deviceProfile);
        requestLayout();
    }

    public void setGridSize(int x, int y) {
        mCountX = x;
        mCountY = y;
        mOccupied = new GridOccupancy(mCountX, mCountY);
        mTmpOccupied = new GridOccupancy(mCountX, mCountY);
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mCountX, mCountY,
                mBorderSpace);
        requestLayout();
    }

    // Set whether or not to invert the layout horizontally if the layout is in RTL mode.
    public void setInvertIfRtl(boolean invert) {
        mShortcutsAndWidgets.setInvertIfRtl(invert);
    }

    public void setDropPending(boolean pending) {
        mDropPending = pending;
    }

    public boolean isDropPending() {
        return mDropPending;
    }

    void setIsDragOverlapping(boolean isDragOverlapping) {
        if (mIsDragOverlapping != isDragOverlapping) {
            mIsDragOverlapping = isDragOverlapping;
            mBackground.setState(mIsDragOverlapping
                    ? BACKGROUND_STATE_ACTIVE : BACKGROUND_STATE_DEFAULT);
            invalidate();
        }
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        ParcelableSparseArray jail = getJailedArray(container);
        super.dispatchSaveInstanceState(jail);
        container.put(R.id.cell_layout_jail_id, jail);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(getJailedArray(container));
    }

    /**
     * Wrap the SparseArray in another Parcelable so that the item ids do not conflict with our
     * our internal resource ids
     */
    private ParcelableSparseArray getJailedArray(SparseArray<Parcelable> container) {
        final Parcelable parcelable = container.get(R.id.cell_layout_jail_id);
        return parcelable instanceof ParcelableSparseArray ?
                (ParcelableSparseArray) parcelable : new ParcelableSparseArray();
    }

    public boolean getIsDragOverlapping() {
        return mIsDragOverlapping;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // When we're large, we are either drawn in a "hover" state (ie when dragging an item to
        // a neighboring page) or with just a normal background (if backgroundAlpha > 0.0f)
        // When we're small, we are either drawn normally or in the "accepts drops" state (during
        // a drag). However, we also drag the mini hover background *over* one of those two
        // backgrounds
        if (mBackground.getAlpha() > 0) {
            mBackground.draw(canvas);
        }

        if (DEBUG_VISUALIZE_OCCUPIED) {
            Rect cellBounds = new Rect();
            // Will contain the bounds of the cell including spacing between cells.
            Rect cellBoundsWithSpacing = new Rect();
            int[] targetCell = new int[2];
            int[] cellCenter = new int[2];
            Paint debugPaint = new Paint();
            debugPaint.setStrokeWidth(Utilities.dpToPx(1));
            for (int x = 0; x < mCountX; x++) {
                for (int y = 0; y < mCountY; y++) {
                    if (!mOccupied.cells[x][y]) {
                        continue;
                    }
                    targetCell[0] = x;
                    targetCell[1] = y;

                    boolean canCreateFolder = canCreateFolder(getChildAt(x, y));
                    cellToRect(x, y, 1, 1, cellBounds);
                    cellBoundsWithSpacing.set(cellBounds);
                    cellBoundsWithSpacing.inset(-mBorderSpace.x / 2, -mBorderSpace.y / 2);
                    getWorkspaceCellVisualCenter(x, y, cellCenter);

                    canvas.save();
                    canvas.clipRect(cellBoundsWithSpacing);

                    // Draw reorder drag target.
                    debugPaint.setColor(Color.RED);
                    canvas.drawCircle(cellCenter[0], cellCenter[1],
                            getReorderRadius(targetCell, 1, 1), debugPaint);

                    // Draw folder creation drag target.
                    if (canCreateFolder) {
                        debugPaint.setColor(Color.GREEN);
                        canvas.drawCircle(cellCenter[0], cellCenter[1],
                                getFolderCreationRadius(targetCell), debugPaint);
                    }

                    canvas.restore();
                }
            }
        }

        for (int i = 0; i < mDelegatedCellDrawings.size(); i++) {
            DelegatedCellDrawing cellDrawing = mDelegatedCellDrawings.get(i);
            canvas.save();
            if (cellDrawing.mDelegateCellX >= 0 && cellDrawing.mDelegateCellY >= 0) {
                cellToPoint(cellDrawing.mDelegateCellX, cellDrawing.mDelegateCellY, mTempLocation);
                canvas.translate(mTempLocation[0], mTempLocation[1]);
            }
            cellDrawing.drawUnderItem(canvas);
            canvas.restore();
        }

        if (mFolderLeaveBehind.mDelegateCellX >= 0 && mFolderLeaveBehind.mDelegateCellY >= 0) {
            cellToPoint(mFolderLeaveBehind.mDelegateCellX,
                    mFolderLeaveBehind.mDelegateCellY, mTempLocation);
            canvas.save();
            canvas.translate(mTempLocation[0], mTempLocation[1]);
            mFolderLeaveBehind.drawLeaveBehind(canvas, FOLDER_LEAVE_BEHIND_COLOR);
            canvas.restore();
        }

        if (mVisualizeCells || mVisualizeDropLocation) {
            visualizeGrid(canvas);
        }
    }

    /**
     * Returns whether dropping an icon on the given View can create (or add to) a folder.
     */
    private boolean canCreateFolder(View child) {
        return child instanceof DraggableView
                && ((DraggableView) child).getViewType() == DRAGGABLE_ICON;
    }

    /**
     * Indicates the progress of the Workspace entering the SpringLoaded state; allows the
     * CellLayout to update various visuals for this state.
     *
     * @param progress
     */
    public void setSpringLoadedProgress(float progress) {
        if (Float.compare(progress, mSpringLoadedProgress) != 0) {
            mSpringLoadedProgress = progress;
            updateBgAlpha();
            setGridAlpha(progress);
        }
    }

    /**
     * See setSpringLoadedProgress
     * @return progress
     */
    public float getSpringLoadedProgress() {
        return mSpringLoadedProgress;
    }

    protected void updateBgAlpha() {
        mBackground.setAlpha((int) (mSpringLoadedProgress * 255));
    }

    /**
     * Set the progress of this page's scroll
     *
     * @param progress 0 if the screen is centered, +/-1 if it is to the right / left respectively
     */
    public void setScrollProgress(float progress) {
        if (Float.compare(Math.abs(progress), mScrollProgress) != 0) {
            mScrollProgress = Math.abs(progress);
            updateBgAlpha();
        }
    }

    private void setGridAlpha(float gridAlpha) {
        if (Float.compare(gridAlpha, mGridAlpha) != 0) {
            mGridAlpha = gridAlpha;
            invalidate();
        }
    }

    protected void visualizeGrid(Canvas canvas) {
        DeviceProfile dp = mActivity.getDeviceProfile();
        int paddingX = Math.min((mCellWidth - dp.iconSizePx) / 2, dp.gridVisualizationPaddingX);
        int paddingY = Math.min((mCellHeight - dp.iconSizePx) / 2, dp.gridVisualizationPaddingY);

        mVisualizeGridPaint.setStrokeWidth(8);

        // This is used for debugging purposes only
        if (mVisualizeCells) {
            int paintAlpha = (int) (120 * mGridAlpha);
            mVisualizeGridPaint.setColor(ColorUtils.setAlphaComponent(mGridColor, paintAlpha));
            for (int i = 0; i < mCountX; i++) {
                for (int j = 0; j < mCountY; j++) {
                    cellToRect(i, j, 1, 1, mTempOnDrawCellToRect);
                    mVisualizeGridRect.set(mTempOnDrawCellToRect);
                    mVisualizeGridRect.inset(paddingX, paddingY);
                    mVisualizeGridPaint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(mVisualizeGridRect, mGridVisualizationRoundingRadius,
                            mGridVisualizationRoundingRadius, mVisualizeGridPaint);
                }
            }
        }

        if (mVisualizeDropLocation) {
            for (int i = 0; i < mDragOutlines.length; i++) {
                final float alpha = mDragOutlineAlphas[i];
                if (alpha <= 0) continue;
                CellLayoutLayoutParams params = mDragOutlines[i];
                cellToRect(params.getCellX(), params.getCellY(), params.cellHSpan, params.cellVSpan,
                        mTempOnDrawCellToRect);
                mVisualizeGridRect.set(mTempOnDrawCellToRect);
                mVisualizeGridRect.inset(paddingX, paddingY);

                mVisualizeGridPaint.setAlpha(255);
                mVisualizeGridPaint.setStyle(Paint.Style.STROKE);
                mVisualizeGridPaint.setColor(Color.argb((int) (alpha),
                        Color.red(mGridColor), Color.green(mGridColor), Color.blue(mGridColor)));

                canvas.save();
                canvas.translate(getMarginForGivenCellParams(params), 0);
                canvas.drawRoundRect(mVisualizeGridRect, mGridVisualizationRoundingRadius,
                        mGridVisualizationRoundingRadius, mVisualizeGridPaint);
                canvas.restore();
            }
        }
    }

    protected float getMarginForGivenCellParams(CellLayoutLayoutParams params) {
        return 0;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        for (int i = 0; i < mDelegatedCellDrawings.size(); i++) {
            DelegatedCellDrawing bg = mDelegatedCellDrawings.get(i);
            canvas.save();
            if (bg.mDelegateCellX >= 0 && bg.mDelegateCellY >= 0) {
                cellToPoint(bg.mDelegateCellX, bg.mDelegateCellY, mTempLocation);
                canvas.translate(mTempLocation[0], mTempLocation[1]);
            }
            bg.drawOverItem(canvas);
            canvas.restore();
        }
    }

    /**
     * Add Delegated cell drawing
     */
    public void addDelegatedCellDrawing(DelegatedCellDrawing bg) {
        mDelegatedCellDrawings.add(bg);
    }

    /**
     * Remove item from DelegatedCellDrawings
     */
    public void removeDelegatedCellDrawing(DelegatedCellDrawing bg) {
        mDelegatedCellDrawings.remove(bg);
    }

    public void setFolderLeaveBehindCell(int x, int y) {
        View child = getChildAt(x, y);
        mFolderLeaveBehind.setup(getContext(), mActivity, null,
                child.getMeasuredWidth(), child.getPaddingTop());

        mFolderLeaveBehind.mDelegateCellX = x;
        mFolderLeaveBehind.mDelegateCellY = y;
        invalidate();
    }

    public void clearFolderLeaveBehind() {
        mFolderLeaveBehind.mDelegateCellX = -1;
        mFolderLeaveBehind.mDelegateCellY = -1;
        invalidate();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void restoreInstanceState(SparseArray<Parcelable> states) {
        try {
            dispatchRestoreInstanceState(states);
        } catch (IllegalArgumentException ex) {
            if (FeatureFlags.IS_STUDIO_BUILD) {
                throw ex;
            }
            // Mismatched viewId / viewType preventing restore. Skip restore on production builds.
            Log.e(TAG, "Ignoring an error while restoring a view instance state", ex);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    public void setOnInterceptTouchListener(View.OnTouchListener listener) {
        mInterceptTouchListener = listener;
    }

    public int getCountX() {
        return mCountX;
    }

    public int getCountY() {
        return mCountY;
    }

    public boolean acceptsWidget() {
        return mContainerType == WORKSPACE;
    }

    /**
     * Adds the given view to the CellLayout
     *
     * @param child view to add.
     * @param index index of the CellLayout children where to add the view.
     * @param childId id of the view.
     * @param params represent the logic of the view on the CellLayout.
     * @param markCells if the occupied cells should be marked or not
     * @return if adding the view was successful
     */
    public boolean addViewToCellLayout(View child, int index, int childId,
            CellLayoutLayoutParams params, boolean markCells) {
        final CellLayoutLayoutParams lp = params;

        // Hotseat icons - remove text
        if (child instanceof BubbleTextView) {
            BubbleTextView bubbleChild = (BubbleTextView) child;
            bubbleChild.setTextVisibility(mContainerType != HOTSEAT);
        }

        child.setScaleX(DEFAULT_SCALE);
        child.setScaleY(DEFAULT_SCALE);

        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        if (lp.getCellX() >= 0 && lp.getCellX() <= mCountX - 1
                && lp.getCellY() >= 0 && lp.getCellY() <= mCountY - 1) {
            // If the horizontal or vertical span is set to -1, it is taken to
            // mean that it spans the extent of the CellLayout
            if (lp.cellHSpan < 0) lp.cellHSpan = mCountX;
            if (lp.cellVSpan < 0) lp.cellVSpan = mCountY;

            child.setId(childId);
            if (LOGD) {
                Log.d(TAG, "Adding view to ShortcutsAndWidgetsContainer: " + child);
            }
            mShortcutsAndWidgets.addView(child, index, lp);

            // Whenever an app is added, if Accessibility service is enabled, focus on that app.
            if (mActivity instanceof Launcher) {
                child.setTag(R.id.perform_a11y_action_on_launcher_state_normal_tag,
                        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            }

            if (markCells) markCellsAsOccupiedForView(child);

            return true;
        }
        return false;
    }

    @Override
    public void removeAllViews() {
        mOccupied.clear();
        mShortcutsAndWidgets.removeAllViews();
    }

    @Override
    public void removeAllViewsInLayout() {
        if (mShortcutsAndWidgets.getChildCount() > 0) {
            mOccupied.clear();
            mShortcutsAndWidgets.removeAllViewsInLayout();
        }
    }

    @Override
    public void removeView(View view) {
        markCellsAsUnoccupiedForView(view);
        mShortcutsAndWidgets.removeView(view);
    }

    @Override
    public void removeViewAt(int index) {
        markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(index));
        mShortcutsAndWidgets.removeViewAt(index);
    }

    @Override
    public void removeViewInLayout(View view) {
        markCellsAsUnoccupiedForView(view);
        mShortcutsAndWidgets.removeViewInLayout(view);
    }

    @Override
    public void removeViews(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i));
        }
        mShortcutsAndWidgets.removeViews(start, count);
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i));
        }
        mShortcutsAndWidgets.removeViewsInLayout(start, count);
    }

    /**
     * Given a point, return the cell that strictly encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    public void pointToCellExact(int x, int y, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        result[0] = (x - hStartPadding) / (mCellWidth + mBorderSpace.x);
        result[1] = (y - vStartPadding) / (mCellHeight + mBorderSpace.y);

        final int xAxis = mCountX;
        final int yAxis = mCountY;

        if (result[0] < 0) result[0] = 0;
        if (result[0] >= xAxis) result[0] = xAxis - 1;
        if (result[1] < 0) result[1] = 0;
        if (result[1] >= yAxis) result[1] = yAxis - 1;
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    void cellToPoint(int cellX, int cellY, int[] result) {
        cellToRect(cellX, cellY, 1, 1, mTempRect);
        result[0] = mTempRect.left;
        result[1] = mTempRect.top;
    }

    /**
     * Given a cell coordinate, return the point that represents the center of the cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    void cellToCenterPoint(int cellX, int cellY, int[] result) {
        regionToCenterPoint(cellX, cellY, 1, 1, result);
    }

    /**
     * Given a cell coordinate and span return the point that represents the center of the region
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    public void regionToCenterPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        cellToRect(cellX, cellY, spanX, spanY, mTempRect);
        result[0] = mTempRect.centerX();
        result[1] = mTempRect.centerY();
    }

    /**
     * Returns the distance between the given coordinate and the visual center of the given cell.
     */
    public float getDistanceFromWorkspaceCellVisualCenter(float x, float y, int[] cell) {
        getWorkspaceCellVisualCenter(cell[0], cell[1], mTmpPoint);
        return (float) Math.hypot(x - mTmpPoint[0], y - mTmpPoint[1]);
    }

    private void getWorkspaceCellVisualCenter(int cellX, int cellY, int[] outPoint) {
        View child = getChildAt(cellX, cellY);
        if (child instanceof DraggableView) {
            DraggableView draggableChild = (DraggableView) child;
            if (draggableChild.getViewType() == DRAGGABLE_ICON) {
                cellToPoint(cellX, cellY, outPoint);
                draggableChild.getWorkspaceVisualDragBounds(mTempRect);
                mTempRect.offset(outPoint[0], outPoint[1]);
                outPoint[0] = mTempRect.centerX();
                outPoint[1] = mTempRect.centerY();
                return;
            }
        }
        cellToCenterPoint(cellX, cellY, outPoint);
    }

    /**
     * Returns the max distance from the center of a cell that can accept a drop to create a folder.
     */
    public float getFolderCreationRadius(int[] targetCell) {
        DeviceProfile grid = mActivity.getDeviceProfile();
        float iconVisibleRadius = ICON_VISIBLE_AREA_FACTOR * grid.iconSizePx / 2;
        // Halfway between reorder radius and icon.
        return (getReorderRadius(targetCell, 1, 1) + iconVisibleRadius) / 2;
    }

    /**
     * Returns the max distance from the center of a cell that will start to reorder on drag over.
     */
    public float getReorderRadius(int[] targetCell, int spanX, int spanY) {
        int[] centerPoint = mTmpPoint;
        getWorkspaceCellVisualCenter(targetCell[0], targetCell[1], centerPoint);

        Rect cellBoundsWithSpacing = mTempRect;
        cellToRect(targetCell[0], targetCell[1], spanX, spanY, cellBoundsWithSpacing);
        cellBoundsWithSpacing.inset(-mBorderSpace.x / 2, -mBorderSpace.y / 2);

        if (canCreateFolder(getChildAt(targetCell[0], targetCell[1])) && spanX == 1 && spanY == 1) {
            // Take only the circle in the smaller dimension, to ensure we don't start reordering
            // too soon before accepting a folder drop.
            int minRadius = centerPoint[0] - cellBoundsWithSpacing.left;
            minRadius = Math.min(minRadius, centerPoint[1] - cellBoundsWithSpacing.top);
            minRadius = Math.min(minRadius, cellBoundsWithSpacing.right - centerPoint[0]);
            minRadius = Math.min(minRadius, cellBoundsWithSpacing.bottom - centerPoint[1]);
            return minRadius;
        }
        // Take up the entire cell, including space between this cell and the adjacent ones.
        // Multiply by span to scale radius
        return (float) Math.hypot(spanX * cellBoundsWithSpacing.width() / 2f,
                spanY * cellBoundsWithSpacing.height() / 2f);
    }

    public int getCellWidth() {
        return mCellWidth;
    }

    public int getCellHeight() {
        return mCellHeight;
    }

    public void setFixedSize(int width, int height) {
        mFixedWidth = width;
        mFixedHeight = height;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);
        int childWidthSize = widthSize - (getPaddingLeft() + getPaddingRight());
        int childHeightSize = heightSize - (getPaddingTop() + getPaddingBottom());

        if (mFixedCellWidth < 0 || mFixedCellHeight < 0) {
            int cw = DeviceProfile.calculateCellWidth(childWidthSize, mBorderSpace.x,
                    mCountX);
            int ch = DeviceProfile.calculateCellHeight(childHeightSize, mBorderSpace.y,
                    mCountY);
            if (cw != mCellWidth || ch != mCellHeight) {
                mCellWidth = cw;
                mCellHeight = ch;
                mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mCountX, mCountY,
                        mBorderSpace);
            }
        }

        int newWidth = childWidthSize;
        int newHeight = childHeightSize;
        if (mFixedWidth > 0 && mFixedHeight > 0) {
            newWidth = mFixedWidth;
            newHeight = mFixedHeight;
        } else if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        mShortcutsAndWidgets.measure(
                MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY));

        int maxWidth = mShortcutsAndWidgets.getMeasuredWidth();
        int maxHeight = mShortcutsAndWidgets.getMeasuredHeight();
        if (mFixedWidth > 0 && mFixedHeight > 0) {
            setMeasuredDimension(maxWidth, maxHeight);
        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mHasOnLayoutBeenCalled = true; // b/349929393 - is the required call to onLayout not done?
        int left = getPaddingLeft();
        left += (int) Math.ceil(getUnusedHorizontalSpace() / 2f);
        int right = r - l - getPaddingRight();
        right -= (int) Math.ceil(getUnusedHorizontalSpace() / 2f);

        int top = getPaddingTop();
        int bottom = b - t - getPaddingBottom();

        // Expand the background drawing bounds by the padding baked into the background drawable
        mBackground.getPadding(mTempRect);
        mBackground.setBounds(
                left - mTempRect.left - getPaddingLeft(),
                top - mTempRect.top - getPaddingTop(),
                right + mTempRect.right + getPaddingRight(),
                bottom + mTempRect.bottom + getPaddingBottom());

        mShortcutsAndWidgets.layout(left, top, right, bottom);
    }

    /**
     * Returns the amount of space left over after subtracting padding and cells. This space will be
     * very small, a few pixels at most, and is a result of rounding down when calculating the cell
     * width in {@link DeviceProfile#calculateCellWidth(int, int, int)}.
     */
    public int getUnusedHorizontalSpace() {
        return getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - (mCountX * mCellWidth)
                - ((mCountX - 1) * mBorderSpace.x);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || (who == mBackground);
    }

    public ShortcutAndWidgetContainer getShortcutsAndWidgets() {
        return mShortcutsAndWidgets;
    }

    public View getChildAt(int cellX, int cellY) {
        return mShortcutsAndWidgets.getChildAt(cellX, cellY);
    }

    public boolean animateChildToPosition(final View child, int cellX, int cellY, int duration,
            int delay, boolean permanent, boolean adjustOccupied) {
        ShortcutAndWidgetContainer clc = getShortcutsAndWidgets();

        if (clc.indexOfChild(child) != -1 && (child instanceof Reorderable)) {
            final CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            final ItemInfo info = (ItemInfo) child.getTag();
            final Reorderable item = (Reorderable) child;

            // We cancel any existing animations
            if (mReorderAnimators.containsKey(lp)) {
                mReorderAnimators.get(lp).cancel();
                mReorderAnimators.remove(lp);
            }


            if (adjustOccupied) {
                GridOccupancy occupied = permanent ? mOccupied : mTmpOccupied;
                occupied.markCells(lp.getCellX(), lp.getCellY(), lp.cellHSpan, lp.cellVSpan, false);
                occupied.markCells(cellX, cellY, lp.cellHSpan, lp.cellVSpan, true);
            }

            // Compute the new x and y position based on the new cellX and cellY
            // We leverage the actual layout logic in the layout params and hence need to modify
            // state and revert that state.
            final int oldX = lp.x;
            final int oldY = lp.y;
            lp.isLockedToGrid = true;
            if (permanent) {
                lp.setCellX(cellX);
                lp.setCellY(cellY);
            } else {
                lp.setTmpCellX(cellX);
                lp.setTmpCellY(cellY);
            }
            clc.setupLp(child);
            final int newX = lp.x;
            final int newY = lp.y;
            lp.x = oldX;
            lp.y = oldY;
            lp.isLockedToGrid = false;
            // End compute new x and y

            MultiTranslateDelegate mtd = item.getTranslateDelegate();
            float initPreviewOffsetX = mtd.getTranslationX(INDEX_REORDER_PREVIEW_OFFSET).getValue();
            float initPreviewOffsetY = mtd.getTranslationY(INDEX_REORDER_PREVIEW_OFFSET).getValue();
            final float finalPreviewOffsetX = newX - oldX;
            final float finalPreviewOffsetY = newY - oldY;

            // Exit early if we're not actually moving the view
            if (finalPreviewOffsetX == 0 && finalPreviewOffsetY == 0
                    && initPreviewOffsetX == 0 && initPreviewOffsetY == 0) {
                lp.isLockedToGrid = true;
                return true;
            }

            ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
            va.setDuration(duration);
            mReorderAnimators.put(lp, va);

            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = (Float) animation.getAnimatedValue();
                    float x = (1 - r) * initPreviewOffsetX + r * finalPreviewOffsetX;
                    float y = (1 - r) * initPreviewOffsetY + r * finalPreviewOffsetY;
                    item.getTranslateDelegate().setTranslation(INDEX_REORDER_PREVIEW_OFFSET, x, y);
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                boolean cancelled = false;
                public void onAnimationEnd(Animator animation) {
                    // If the animation was cancelled, it means that another animation
                    // has interrupted this one, and we don't want to lock the item into
                    // place just yet.
                    if (!cancelled) {
                        lp.isLockedToGrid = true;
                        item.getTranslateDelegate()
                                .setTranslation(INDEX_REORDER_PREVIEW_OFFSET, 0, 0);
                        child.requestLayout();
                    }
                    if (mReorderAnimators.containsKey(lp)) {
                        mReorderAnimators.remove(lp);
                    }
                }
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }
            });
            va.setStartDelay(delay);
            va.start();
            return true;
        }
        return false;
    }

    void visualizeDropLocation(int cellX, int cellY, int spanX, int spanY,
            DropTarget.DragObject dragObject) {
        if (mDragCell[0] != cellX || mDragCell[1] != cellY || mDragCellSpan[0] != spanX
                || mDragCellSpan[1] != spanY) {
            determineIfDragHapticsPlay();
            if (mPlayDragHaptics && Flags.msdlFeedback()) {
                mMSDLPlayerWrapper.playToken(MSDLToken.DRAG_INDICATOR_DISCRETE);
            }
            mDragCell[0] = cellX;
            mDragCell[1] = cellY;
            mDragCellSpan[0] = spanX;
            mDragCellSpan[1] = spanY;

            final int oldIndex = mDragOutlineCurrent;
            mDragOutlineAnims[oldIndex].animateOut();
            mDragOutlineCurrent = (oldIndex + 1) % mDragOutlines.length;

            CellLayoutLayoutParams cell = mDragOutlines[mDragOutlineCurrent];
            cell.setCellX(cellX);
            cell.setCellY(cellY);
            cell.cellHSpan = spanX;
            cell.cellVSpan = spanY;

            mDragOutlineAnims[mDragOutlineCurrent].animateIn();
            invalidate();

            if (dragObject.stateAnnouncer != null) {
                dragObject.stateAnnouncer.announce(getItemMoveDescription(cellX, cellY));
            }

        }
    }

    private void determineIfDragHapticsPlay() {
        if (mDragCell[0] != -1 || mDragCell[1] != -1
                || mDragCellSpan[0] != -1 || mDragCellSpan[1] != -1) {
            // The nearest cell is known and we can play haptics
            mPlayDragHaptics = true;
        }
    }

    @SuppressLint("StringFormatMatches")
    public String getItemMoveDescription(int cellX, int cellY) {
        if (mContainerType == HOTSEAT) {
            return getContext().getString(R.string.move_to_hotseat_position,
                    Math.max(cellX, cellY) + 1);
        } else {
            int row = cellY + 1;
            int col = Utilities.isRtl(getResources()) ? mCountX - cellX : cellX + 1;
            int panelCount = mCellLayoutContainer.getPanelCount();
            int pageIndex = mCellLayoutContainer.getCellLayoutIndex(this);
            if (panelCount > 1) {
                // Increment the column if the target is on the right side of a two panel home
                col += (pageIndex % panelCount) * mCountX;
            }
            return getContext().getString(R.string.move_to_empty_cell_description, row, col,
                    mCellLayoutContainer.getPageDescription(pageIndex));
        }
    }

    public void clearDragOutlines() {
        final int oldIndex = mDragOutlineCurrent;
        mDragOutlineAnims[oldIndex].animateOut();
        mDragCell[0] = mDragCell[1] = -1;
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestVacantArea(int pixelX, int pixelY, int minSpanX, int minSpanY,
            int spanX, int spanY, int[] result, int[] resultSpan) {
        return findNearestArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, false,
                result, resultSpan);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     * @param relativeXPos The X location relative to the Cell layout at which you want to search
     *                     for a vacant area.
     * @param relativeYPos The Y location relative to the Cell layout at which you want to search
     *                     for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    protected int[] findNearestArea(int relativeXPos, int relativeYPos, int minSpanX, int minSpanY,
            int spanX, int spanY, boolean ignoreOccupied, int[] result, int[] resultSpan) {
        // For items with a spanX / spanY > 1, the passed in point (relativeXPos, relativeYPos)
        // corresponds to the center of the item, but we are searching based on the top-left cell,
        // so we translate the point over to correspond to the top-left.
        relativeXPos = (int) (relativeXPos - (mCellWidth + mBorderSpace.x) * (spanX - 1) / 2f);
        relativeYPos = (int) (relativeYPos - (mCellHeight + mBorderSpace.y) * (spanY - 1) / 2f);

        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        double bestDistance = Double.MAX_VALUE;
        final Rect bestRect = new Rect(-1, -1, -1, -1);
        final Stack<Rect> validRegions = new Stack<>();

        final int countX = mCountX;
        final int countY = mCountY;

        if (minSpanX <= 0 || minSpanY <= 0 || spanX <= 0 || spanY <= 0 ||
                spanX < minSpanX || spanY < minSpanY) {
            return bestXY;
        }

        for (int y = 0; y < countY - (minSpanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (minSpanX - 1); x++) {
                int ySize = -1;
                int xSize = -1;
                if (!ignoreOccupied) {
                    // First, let's see if this thing fits anywhere
                    for (int i = 0; i < minSpanX; i++) {
                        for (int j = 0; j < minSpanY; j++) {
                            if (mOccupied.cells[x + i][y + j]) {
                                continue inner;
                            }
                        }
                    }
                    xSize = minSpanX;
                    ySize = minSpanY;

                    // We know that the item will fit at _some_ acceptable size, now let's see
                    // how big we can make it. We'll alternate between incrementing x and y spans
                    // until we hit a limit.
                    boolean incX = true;
                    boolean hitMaxX = xSize >= spanX;
                    boolean hitMaxY = ySize >= spanY;
                    while (!(hitMaxX && hitMaxY)) {
                        if (incX && !hitMaxX) {
                            for (int j = 0; j < ySize; j++) {
                                if (x + xSize > countX -1 || mOccupied.cells[x + xSize][y + j]) {
                                    // We can't move out horizontally
                                    hitMaxX = true;
                                }
                            }
                            if (!hitMaxX) {
                                xSize++;
                            }
                        } else if (!hitMaxY) {
                            for (int i = 0; i < xSize; i++) {
                                if (y + ySize > countY - 1 || mOccupied.cells[x + i][y + ySize]) {
                                    // We can't move out vertically
                                    hitMaxY = true;
                                }
                            }
                            if (!hitMaxY) {
                                ySize++;
                            }
                        }
                        hitMaxX |= xSize >= spanX;
                        hitMaxY |= ySize >= spanY;
                        incX = !incX;
                    }
                }
                final int[] cellXY = mTmpPoint;
                cellToCenterPoint(x, y, cellXY);

                // We verify that the current rect is not a sub-rect of any of our previous
                // candidates. In this case, the current rect is disqualified in favour of the
                // containing rect.
                Rect currentRect = new Rect(x, y, x + xSize, y + ySize);
                boolean contained = false;
                for (Rect r : validRegions) {
                    if (r.contains(currentRect)) {
                        contained = true;
                        break;
                    }
                }
                validRegions.push(currentRect);
                double distance = Math.hypot(cellXY[0] - relativeXPos,  cellXY[1] - relativeYPos);

                if ((distance <= bestDistance && !contained) ||
                        currentRect.contains(bestRect)) {
                    bestDistance = distance;
                    bestXY[0] = x;
                    bestXY[1] = y;
                    if (resultSpan != null) {
                        resultSpan[0] = xSize;
                        resultSpan[1] = ySize;
                    }
                    bestRect.set(currentRect);
                }
            }
        }

        // Return -1, -1 if no suitable location found
        if (bestDistance == Double.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        return bestXY;
    }

    public GridOccupancy getOccupied() {
        return mOccupied;
    }

    private void copySolutionToTempState(ItemConfiguration solution, View dragView) {
        mTmpOccupied.clear();

        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                lp.setTmpCellX(c.cellX);
                lp.setTmpCellY(c.cellY);
                lp.cellHSpan = c.spanX;
                lp.cellVSpan = c.spanY;
                mTmpOccupied.markCells(c, true);
            }
        }
        mTmpOccupied.markCells(solution, true);
    }

    private void animateItemsToSolution(ItemConfiguration solution, View dragView, boolean
            commitDragView) {

        GridOccupancy occupied = DESTRUCTIVE_REORDER ? mOccupied : mTmpOccupied;
        occupied.clear();

        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                animateChildToPosition(child, c.cellX, c.cellY, REORDER_ANIMATION_DURATION, 0,
                        DESTRUCTIVE_REORDER, false);
                occupied.markCells(c, true);
            }
        }
        if (commitDragView) {
            occupied.markCells(solution, true);
        }
    }


    // This method starts or changes the reorder preview animations
    private void beginOrAdjustReorderPreviewAnimations(ItemConfiguration solution,
            View dragView, int mode) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            boolean skip = mode == ReorderPreviewAnimation.MODE_HINT
                    && !solution.intersectingViews.contains(child);

            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            if (c != null && !skip && (child instanceof Reorderable)) {
                ReorderPreviewAnimation rha = new ReorderPreviewAnimation(child, mode,
                        lp.getCellX(), lp.getCellY(), c.cellX, c.cellY, c.spanX, c.spanY,
                        mReorderPreviewAnimationMagnitude, this, mShakeAnimators);
                rha.animate();
            }
        }
    }

    private void completeAndClearReorderPreviewAnimations() {
        for (ReorderPreviewAnimation a: mShakeAnimators.values()) {
            a.finishAnimation();
        }
        mShakeAnimators.clear();
    }

    private void commitTempPlacement(View dragView) {
        mTmpOccupied.copyTo(mOccupied);

        int screenId = mCellLayoutContainer.getCellLayoutId(this);
        int container = Favorites.CONTAINER_DESKTOP;

        if (mContainerType == HOTSEAT) {
            screenId = -1;
            container = Favorites.CONTAINER_HOTSEAT;
        }

        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            ItemInfo info = (ItemInfo) child.getTag();
            // We do a null check here because the item info can be null in the case of the
            // AllApps button in the hotseat.
            if (info != null && child != dragView) {
                CellPos presenterPos = mActivity.getCellPosMapper().mapModelToPresenter(info);
                final boolean requiresDbUpdate = (presenterPos.cellX != lp.getTmpCellX()
                        || presenterPos.cellY != lp.getTmpCellY() || info.spanX != lp.cellHSpan
                        || info.spanY != lp.cellVSpan || presenterPos.screenId != screenId);

                lp.setCellX(lp.getTmpCellX());
                lp.setCellY(lp.getTmpCellY());
                if (requiresDbUpdate) {
                    Launcher.cast(mActivity).getModelWriter().modifyItemInDatabase(info, container,
                            screenId, lp.getCellX(), lp.getCellY(), lp.cellHSpan, lp.cellVSpan);
                }
            }
        }
    }

    private void setUseTempCoords(boolean useTempCoords) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) mShortcutsAndWidgets.getChildAt(
                    i).getLayoutParams();
            lp.useTmpCoords = useTempCoords;
        }
    }

    /**
     * For a given region, return the rectangle of the overlapping cell and span with the given
     * region including the region itself. If there is no overlap the rectangle will be
     * invalid i.e. -1, 0, -1, 0.
     */
    @Nullable
    public Rect getIntersectingRectanglesInRegion(final Rect region, final View dragView) {
        Rect boundingRect = new Rect(region);
        Rect r1 = new Rect();
        boolean isOverlapping = false;
        final int count = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellLayoutLayoutParams
                    lp = (CellLayoutLayoutParams) child.getLayoutParams();
            r1.set(lp.getCellX(), lp.getCellY(), lp.getCellX() + lp.cellHSpan,
                    lp.getCellY() + lp.cellVSpan);
            if (Rect.intersects(region, r1)) {
                isOverlapping = true;
                boundingRect.union(r1);
            }
        }
        return isOverlapping ? boundingRect : null;
    }

    public boolean isNearestDropLocationOccupied(int pixelX, int pixelY, int spanX, int spanY,
            View dragView, int[] result) {
        result = findNearestAreaIgnoreOccupied(pixelX, pixelY, spanX, spanY, result);
        return getIntersectingRectanglesInRegion(
                new Rect(result[0], result[1], result[0] + spanX, result[1] + spanY),
                dragView
        ) != null;
    }

    void revertTempState() {
        completeAndClearReorderPreviewAnimations();
        if (isItemPlacementDirty() && !DESTRUCTIVE_REORDER) {
            final int count = mShortcutsAndWidgets.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = mShortcutsAndWidgets.getChildAt(i);
                CellLayoutLayoutParams
                        lp = (CellLayoutLayoutParams) child.getLayoutParams();
                if (lp.getTmpCellX() != lp.getCellX() || lp.getTmpCellY() != lp.getCellY()) {
                    lp.setTmpCellX(lp.getCellX());
                    lp.setTmpCellY(lp.getCellY());
                    animateChildToPosition(child, lp.getCellX(), lp.getCellY(),
                            REORDER_ANIMATION_DURATION, 0, false, false);
                }
            }
            setItemPlacementDirty(false);
        }
    }

    boolean createAreaForResize(int cellX, int cellY, int spanX, int spanY,
            View dragView, int[] direction, boolean commit) {
        int[] pixelXY = new int[2];
        regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY);

        // First we determine if things have moved enough to cause a different layout
        ItemConfiguration swapSolution = findReorderSolution(pixelXY[0], pixelXY[1], spanX, spanY,
                spanX,  spanY, direction, dragView,  true);

        setUseTempCoords(true);
        if (swapSolution != null && swapSolution.isSolution) {
            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            copySolutionToTempState(swapSolution, dragView);
            setItemPlacementDirty(true);
            animateItemsToSolution(swapSolution, dragView, commit);

            if (commit) {
                commitTempPlacement(null);
                completeAndClearReorderPreviewAnimations();
                setItemPlacementDirty(false);
            } else {
                beginOrAdjustReorderPreviewAnimations(swapSolution, dragView,
                        ReorderPreviewAnimation.MODE_PREVIEW);
            }
            mShortcutsAndWidgets.requestLayout();
        }
        return swapSolution.isSolution;
    }

    public ReorderAlgorithm createReorderAlgorithm() {
        return new ReorderAlgorithm(this);
    }

    protected ItemConfiguration findReorderSolution(int pixelX, int pixelY, int minSpanX,
            int minSpanY, int spanX, int spanY, int[] direction, View dragView, boolean decX) {
        ItemConfiguration configuration = new ItemConfiguration();
        copyCurrentStateToSolution(configuration);
        ReorderParameters parameters = new ReorderParameters(pixelX, pixelY, spanX, spanY, minSpanX,
                minSpanY, dragView, configuration);
        int[] directionVector = direction != null ? direction : mDirectionVector;
        return createReorderAlgorithm().findReorderSolution(parameters, directionVector, decX);
    }

    public void copyCurrentStateToSolution(ItemConfiguration solution) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            solution.add(child,
                    new CellAndSpan(lp.getCellX(), lp.getCellY(), lp.cellHSpan, lp.cellVSpan));
        }
    }

    /**
     * When the user drags an Item in the workspace sometimes we need to move the items already in
     * the workspace to make space for the new item, this function return a solution for that
     * reorder.
     *
     * @param pixelX X coordinate in the screen of the dragView in pixels
     * @param pixelY Y coordinate in the screen of the dragView in pixels
     * @param minSpanX minimum horizontal span the item can be shrunk to
     * @param minSpanY minimum vertical span the item can be shrunk to
     * @param spanX occupied horizontal span
     * @param spanY occupied vertical span
     * @param dragView the view of the item being draged
     * @return returns a solution for the given parameters, the solution contains all the icons and
     *         the locations they should be in the given solution.
     */
    public ItemConfiguration calculateReorder(int pixelX, int pixelY, int minSpanX, int minSpanY,
            int spanX, int spanY, View dragView) {
        ItemConfiguration configuration = new ItemConfiguration();
        copyCurrentStateToSolution(configuration);
        return createReorderAlgorithm().calculateReorder(
                new ReorderParameters(pixelX, pixelY, spanX, spanY,  minSpanX, minSpanY, dragView,
                        configuration)
        );
    }

    int[] performReorder(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View dragView, int[] result, int[] resultSpan, int mode) {
        if (resultSpan == null) {
            resultSpan = new int[]{-1, -1};
        }
        if (result == null) {
            result = new int[]{-1, -1};
        }

        ItemConfiguration finalSolution = null;
        // We want the solution to match the animation of the preview and to match the drop so we
        // only recalculate in mode MODE_SHOW_REORDER_HINT because that the first one to run in the
        // reorder cycle.
        if (mode == MODE_SHOW_REORDER_HINT || mPreviousSolution == null) {
            finalSolution = calculateReorder(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY,
                    dragView);
            mPreviousSolution = finalSolution;
        } else {
            finalSolution = mPreviousSolution;
            // We reset this vector after drop
            if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                mPreviousSolution = null;
            }
        }

        if (finalSolution == null || !finalSolution.isSolution) {
            result[0] = result[1] = resultSpan[0] = resultSpan[1] = -1;
        } else {
            result[0] = finalSolution.cellX;
            result[1] = finalSolution.cellY;
            resultSpan[0] = finalSolution.spanX;
            resultSpan[1] = finalSolution.spanY;
            performReorder(finalSolution, dragView, mode);
        }
        return result;
    }

    /**
     * Animates and submits in the DB the given ItemConfiguration depending of the mode.
     *
     * @param solution represents widgets on the screen which the Workspace will animate to and
     * would be submitted to the database.
     * @param dragView view which is being dragged over the workspace that trigger the reorder
     * @param mode depending on the mode different animations would be played and depending on the
     *             mode the solution would be submitted or not the database.
     *             The possible modes are {@link MODE_SHOW_REORDER_HINT}, {@link MODE_DRAG_OVER},
     *             {@link MODE_ON_DROP}, {@link MODE_ON_DROP_EXTERNAL}, {@link  MODE_ACCEPT_DROP}
     *             defined in {@link CellLayout}.
     */
    public void performReorder(ItemConfiguration solution, View dragView, int mode) {
        if (mode == MODE_SHOW_REORDER_HINT) {
            beginOrAdjustReorderPreviewAnimations(solution, dragView,
                    ReorderPreviewAnimation.MODE_HINT);
            return;
        }
        // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
        // committing anything or animating anything as we just want to determine if a solution
        // exists
        if (mode == MODE_DRAG_OVER || mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
            if (!DESTRUCTIVE_REORDER) {
                setUseTempCoords(true);
            }

            if (!DESTRUCTIVE_REORDER) {
                copySolutionToTempState(solution, dragView);
            }
            setItemPlacementDirty(true);
            animateItemsToSolution(solution, dragView, mode == MODE_ON_DROP);

            if (!DESTRUCTIVE_REORDER
                    && (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL)) {
                // Since the temp solution didn't update dragView, don't commit it either
                commitTempPlacement(dragView);
                completeAndClearReorderPreviewAnimations();
                setItemPlacementDirty(false);
            } else {
                beginOrAdjustReorderPreviewAnimations(solution, dragView,
                        ReorderPreviewAnimation.MODE_PREVIEW);
            }
        }

        if (mode == MODE_ON_DROP && !DESTRUCTIVE_REORDER) {
            setUseTempCoords(false);
        }

        mShortcutsAndWidgets.requestLayout();
    }

    void setItemPlacementDirty(boolean dirty) {
        mItemPlacementDirty = dirty;
    }
    boolean isItemPlacementDirty() {
        return mItemPlacementDirty;
    }

    /**
     * Find a starting cell position that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestAreaIgnoreOccupied(int pixelX, int pixelY, int spanX, int spanY,
            int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY, spanX, spanY, true, result, null);
    }

    boolean existsEmptyCell() {
        return findCellForSpan(null, 1, 1);
    }

    /**
     * Finds the upper-left coordinate of the first rectangle in the grid that can
     * hold a cell of the specified dimensions. If intersectX and intersectY are not -1,
     * then this method will only return coordinates for rectangles that contain the cell
     * (intersectX, intersectY)
     *
     * @param cellXY The array that will contain the position of a vacant cell if such a cell
     *               can be found.
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     *
     * @return True if a vacant cell of the specified dimension was found, false otherwise.
     */
    public boolean findCellForSpan(int[] cellXY, int spanX, int spanY) {
        if (cellXY == null) {
            cellXY = new int[2];
        }
        return mOccupied.findVacantCell(cellXY, spanX, spanY);
    }

    /**
     * A drag event has begun over this layout.
     * It may have begun over this layout (in which case onDragChild is called first),
     * or it may have begun on another layout.
     */
    void onDragEnter() {
        mDragging = true;
        mPreviousSolution = null;
    }

    /**
     * Called when drag has left this CellLayout or has been completed (successfully or not)
     */
    void onDragExit() {
        // This can actually be called when we aren't in a drag, e.g. when adding a new
        // item to this layout via the customize drawer.
        // Guard against that case.
        if (mDragging) {
            mDragging = false;
        }

        // Invalidate the drag data
        mPreviousSolution = null;
        mDragCell[0] = mDragCell[1] = -1;
        mDragCellSpan[0] = mDragCellSpan[1] = -1;
        mDragOutlineAnims[mDragOutlineCurrent].animateOut();
        mDragOutlineCurrent = (mDragOutlineCurrent + 1) % mDragOutlineAnims.length;
        revertTempState();
        setIsDragOverlapping(false);
    }

    /**
     * Mark a child as having been dropped.
     * At the beginning of the drag operation, the child may have been on another
     * screen, but it is re-parented before this method is called.
     *
     * @param child The child that is being dropped
     */
    void onDropChild(View child) {
        mPlayDragHaptics = false;
        if (child != null) {
            CellLayoutLayoutParams
                    lp = (CellLayoutLayoutParams) child.getLayoutParams();
            lp.dropped = true;
            child.requestLayout();
            markCellsAsOccupiedForView(child);
        }
    }

    /**
     * Computes a bounding rectangle for a range of cells
     *
     * @param cellX X coordinate of upper left corner expressed as a cell position
     * @param cellY Y coordinate of upper left corner expressed as a cell position
     * @param cellHSpan Width in cells
     * @param cellVSpan Height in cells
     * @param resultRect Rect into which to put the results
     */
    public void cellToRect(int cellX, int cellY, int cellHSpan, int cellVSpan, Rect resultRect) {
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;

        // We observe a shift of 1 pixel on the x coordinate compared to the actual cell coordinates
        final int hStartPadding = getPaddingLeft()
                + (int) Math.ceil(getUnusedHorizontalSpace() / 2f);
        final int vStartPadding = getPaddingTop();

        int x = hStartPadding + (cellX * mBorderSpace.x) + (cellX * cellWidth)
                + getTranslationXForCell(cellX, cellY);
        int y = vStartPadding + (cellY * mBorderSpace.y) + (cellY * cellHeight);

        int width = cellHSpan * cellWidth + ((cellHSpan - 1) * mBorderSpace.x);
        int height = cellVSpan * cellHeight + ((cellVSpan - 1) * mBorderSpace.y);

        resultRect.set(x, y, x + width, y + height);
    }

    /** Enables successors to provide an X adjustment for the cell. */
    protected int getTranslationXForCell(int cellX, int cellY) {
        return 0;
    }

    public void markCellsAsOccupiedForView(View view) {
        if (view instanceof LauncherAppWidgetHostView
                && view.getTag() instanceof LauncherAppWidgetInfo) {
            LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
            CellPos pos = mActivity.getCellPosMapper().mapModelToPresenter(info);
            mOccupied.markCells(pos.cellX, pos.cellY, info.spanX, info.spanY, true);
            return;
        }
        if (view == null || view.getParent() != mShortcutsAndWidgets) return;
        CellLayoutLayoutParams
                lp = (CellLayoutLayoutParams) view.getLayoutParams();
        mOccupied.markCells(lp.getCellX(), lp.getCellY(), lp.cellHSpan, lp.cellVSpan, true);
    }

    public void markCellsAsUnoccupiedForView(View view) {
        if (view instanceof LauncherAppWidgetHostView
                && view.getTag() instanceof LauncherAppWidgetInfo) {
            LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
            CellPos pos = mActivity.getCellPosMapper().mapModelToPresenter(info);
            mOccupied.markCells(pos.cellX, pos.cellY, info.spanX, info.spanY, false);
            return;
        }
        if (view == null || view.getParent() != mShortcutsAndWidgets) return;
        CellLayoutLayoutParams
                lp = (CellLayoutLayoutParams) view.getLayoutParams();
        mOccupied.markCells(lp.getCellX(), lp.getCellY(), lp.cellHSpan, lp.cellVSpan, false);
    }

    public int getDesiredWidth() {
        return getPaddingLeft() + getPaddingRight() + (mCountX * mCellWidth)
                + ((mCountX - 1) * mBorderSpace.x);
    }

    public int getDesiredHeight()  {
        return getPaddingTop() + getPaddingBottom() + (mCountY * mCellHeight)
                + ((mCountY - 1) * mBorderSpace.y);
    }

    public boolean isOccupied(int x, int y) {
        if (x >= 0 && x < mCountX && y >= 0 && y < mCountY) {
            return mOccupied.cells[x][y];
        }
        if (BuildConfig.IS_STUDIO_BUILD) {
            throw new RuntimeException("Position exceeds the bound of this CellLayout");
        }
        return true;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CellLayoutLayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CellLayoutLayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CellLayoutLayoutParams(p);
    }

    /**
     * Returns whether an item can be placed in this CellLayout (after rearranging and/or resizing
     * if necessary).
     */
    public boolean hasReorderSolution(ItemInfo itemInfo) {
        int[] cellPoint = new int[2];
        // Check for a solution starting at every cell.
        for (int cellX = 0; cellX < getCountX(); cellX++) {
            for (int cellY = 0; cellY < getCountY(); cellY++) {
                cellToPoint(cellX, cellY, cellPoint);
                if (findReorderSolution(cellPoint[0], cellPoint[1], itemInfo.minSpanX,
                        itemInfo.minSpanY, itemInfo.spanX, itemInfo.spanY, mDirectionVector, null,
                        true).isSolution) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds solution to accept hotseat migration to cell layout. commits solution if commitConfig
     */
    public boolean makeSpaceForHotseatMigration(boolean commitConfig) {
        int[] cellPoint = new int[2];
        int[] directionVector = new int[]{0, -1};
        cellToPoint(0, mCountY, cellPoint);
        ItemConfiguration configuration = findReorderSolution(
                cellPoint[0] /* pixelX */,
                cellPoint[1] /* pixelY */,
                mCountX /* minSpanX */,
                1 /* minSpanY */,
                mCountX /* spanX */,
                1 /* spanY */,
                directionVector /* direction */,
                null /* dragView */,
                false /* decX */
        );
        if (configuration.isSolution) {
            if (commitConfig) {
                copySolutionToTempState(configuration, null);
                commitTempPlacement(null);
                // undo marking cells occupied since there is actually nothing being placed yet.
                mOccupied.markCells(0, mCountY - 1, mCountX, 1, false);
            }
            return true;
        }
        return false;
    }

    /**
     * returns a copy of cell layout's grid occupancy
     */
    public GridOccupancy cloneGridOccupancy() {
        GridOccupancy occupancy = new GridOccupancy(mCountX, mCountY);
        mOccupied.copyTo(occupancy);
        return occupancy;
    }

    public boolean isRegionVacant(int x, int y, int spanX, int spanY) {
        return mOccupied.isRegionVacant(x, y, spanX, spanY);
    }

    public void setSpaceBetweenCellLayoutsPx(@Px int spaceBetweenCellLayoutsPx) {
        mSpaceBetweenCellLayoutsPx = spaceBetweenCellLayoutsPx;
    }
}
