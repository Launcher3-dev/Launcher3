/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.launcher3.util

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.launcher3.R
import kotlin.IntArray

class TestResourceHelper(private val context: Context, specsFileId: Int) :
    ResourceHelper(context, specsFileId) {

    val responsiveStyleables = listOf(
            R.styleable.SizeSpec,
            R.styleable.WorkspaceSpec,
            R.styleable.FolderSpec,
            R.styleable.AllAppsSpec,
            R.styleable.ResponsiveSpecGroup
    )

    override fun obtainStyledAttributes(attrs: AttributeSet, styleId: IntArray): TypedArray {
        val clone =
                if (responsiveStyleables.any { styleId.contentEquals(it) }) {
                    convertStyleId(styleId)
                } else {
                    styleId.clone()
                }

        return context.obtainStyledAttributes(attrs, clone)
    }

    private fun convertStyleId(styleableArr: IntArray): IntArray {
        val targetContextRes = getInstrumentation().targetContext.resources
        val context = getInstrumentation().context
        return styleableArr
            .map { attrId -> targetContextRes.getResourceName(attrId).split(":").last() }
            .map { attrName ->
                // Get required attr from context instead of targetContext
                context.resources.getIdentifier(attrName, null, context.packageName)
            }
            .toIntArray()
    }
}
