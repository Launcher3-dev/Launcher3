/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.icons;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.util.Log;
import android.view.ViewDebug;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;

/**
 * Used to draw a notification dot on top of an icon.
 */
public class DotRenderer {

    private static final String TAG = "DotRenderer";

    // The dot size is defined as a percentage of the app icon size.
    private static final float SIZE_PERCENTAGE = 0.228f;

    private final float mCircleRadius;
    private final Paint mCirclePaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);

    private final Bitmap mBackgroundWithShadow;
    private final float mBitmapOffset;

    // Stores the center x and y position as a percentage (0 to 1) of the icon size
    private final float[] mRightDotPosition;
    private final float[] mLeftDotPosition;

    private static final int MIN_DOT_SIZE = 1;

    private static final float SIZE_NUM_PERCENTAGE = 0.328f;
    private final int mTextHeight;
    private final float mNumCircleRadius;
    private final float mNumBitmapOffset;
    private boolean mShowNumInDot = false;
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public DotRenderer(int iconSizePx, Path iconShapePath, int pathSize) {
        int size = Math.round(SIZE_PERCENTAGE * iconSizePx);
        if (size <= 0) {
            size = MIN_DOT_SIZE;
        }
        ShadowGenerator.Builder builder = new ShadowGenerator.Builder(Color.TRANSPARENT);
        builder.ambientShadowAlpha = 88;
        mBackgroundWithShadow = builder.setupBlurForSize(size).createPill(size, size);
        mCircleRadius = builder.radius;

        mBitmapOffset = -mBackgroundWithShadow.getHeight() * 0.5f; // Same as width.

        // Find the points on the path that are closest to the top left and right corners.
        mLeftDotPosition = getPathPoint(iconShapePath, pathSize, -1);
        mRightDotPosition = getPathPoint(iconShapePath, pathSize, 1);

        {
            int numSize = Math.round(SIZE_NUM_PERCENTAGE * iconSizePx);
            if (numSize <= 0) {
                numSize = MIN_DOT_SIZE;
            }
            mNumCircleRadius = numSize/2;//numBuilder.radius;
            mNumBitmapOffset = -mNumCircleRadius;//mNumBackgroundWithShadow.getHeight() * 0.5f;

            mTextPaint.setTextSize(mNumCircleRadius);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setColor(Color.WHITE);
            Rect tempTextHeight = new Rect();
            mTextPaint.getTextBounds("0", 0, 1, tempTextHeight);
            mTextHeight = tempTextHeight.height();
        }
    }

    private static float[] getPathPoint(Path path, float size, float direction) {
        float halfSize = size / 2;
        // Small delta so that we don't get a zero size triangle
        float delta = 1;

        float x = halfSize + direction * halfSize;
        Path trianglePath = new Path();
        trianglePath.moveTo(halfSize, halfSize);
        trianglePath.lineTo(x + delta * direction, 0);
        trianglePath.lineTo(x, -delta);
        trianglePath.close();

        trianglePath.op(path, Path.Op.INTERSECT);
        float[] pos = new float[2];
        new PathMeasure(trianglePath, false).getPosTan(0, pos, null);

        pos[0] = pos[0] / size;
        pos[1] = pos[1] / size;
        return pos;
    }

    public float[] getLeftDotPosition() {
        return mLeftDotPosition;
    }

    public float[] getRightDotPosition() {
        return mRightDotPosition;
    }

    /**
     * Draw a circle on top of the canvas according to the given params.
     */
    public void draw(Canvas canvas, DrawParams params) {
        if (params == null) {
            Log.e(TAG, "Invalid null argument(s) passed in call to draw.");
            return;
        }
        canvas.save();

        Rect iconBounds = params.iconBounds;
        float[] dotPosition = params.leftAlign ? mLeftDotPosition : mRightDotPosition;
        float dotCenterX = iconBounds.left + iconBounds.width() * dotPosition[0];
        float dotCenterY = iconBounds.top + iconBounds.height() * dotPosition[1];

        float bitmapOffset = mShowNumInDot ? mNumBitmapOffset : mBitmapOffset;
        // Ensure dot fits entirely in canvas clip bounds.
        Rect canvasBounds = canvas.getClipBounds();
        float offsetX = params.leftAlign
                ? Math.max(0, canvasBounds.left - (dotCenterX + bitmapOffset))
                : Math.min(0, canvasBounds.right - (dotCenterX - bitmapOffset));
        float offsetY = Math.max(0, canvasBounds.top - (dotCenterY + bitmapOffset));

        // We draw the dot relative to its center.
        canvas.translate(dotCenterX + offsetX, dotCenterY + offsetY);
        canvas.scale(params.scale, params.scale);

        mCirclePaint.setColor(Color.BLACK);
        canvas.drawBitmap(mBackgroundWithShadow, bitmapOffset, bitmapOffset, mCirclePaint);
        mCirclePaint.setColor(params.dotColor);
        canvas.drawCircle(0, 0, (mShowNumInDot ? mNumCircleRadius : mCircleRadius), mCirclePaint);
        if(mShowNumInDot) {
            canvas.drawText("" + params.num, 0, (float) mTextHeight / 2, mTextPaint);
        }
        canvas.restore();
    }

    public void setShowNumInDot(boolean showNumInDot) {
        mShowNumInDot = showNumInDot;
    }

    public static class DrawParams {
        /** The color (possibly based on the icon) to use for the dot. */
        @ViewDebug.ExportedProperty(category = "notification dot", formatToHexString = true)
        public int dotColor;
        /** The color (possibly based on the icon) to use for a predicted app. */
        @ViewDebug.ExportedProperty(category = "notification dot", formatToHexString = true)
        public int appColor;
        /** The bounds of the icon that the dot is drawn on top of. */
        @ViewDebug.ExportedProperty(category = "notification dot")
        public Rect iconBounds = new Rect();
        /** The progress of the animation, from 0 to 1. */
        @ViewDebug.ExportedProperty(category = "notification dot")
        public float scale;
        /** Whether the dot should align to the top left of the icon rather than the top right. */
        @ViewDebug.ExportedProperty(category = "notification dot")
        public boolean leftAlign;
        public int num;
    }
}
