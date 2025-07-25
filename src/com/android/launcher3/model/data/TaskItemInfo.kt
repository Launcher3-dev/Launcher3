/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.launcher3.model.data

/**
 * A Task info class holding a Task ID to allow us to reference a Task when clicking a hotseat item.
 * This is also used to help identify the shortcuts shown in the long-press menu.
 */
class TaskItemInfo(val taskId: Int, itemInfo: WorkspaceItemInfo) : WorkspaceItemInfo(itemInfo)
