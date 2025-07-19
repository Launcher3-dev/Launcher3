/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.wm.shell.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.Configuration;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayTopology;
import android.os.RemoteException;
import android.platform.test.annotations.EnableFlags;
import android.testing.TestableContext;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.IDisplayWindowListener;
import android.view.IWindowManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.dx.mockito.inline.extended.StaticMockitoSession;
import com.android.window.flags.Flags;
import com.android.wm.shell.ShellTestCase;
import com.android.wm.shell.TestSyncExecutor;
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus;
import com.android.wm.shell.sysui.ShellInit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;

import java.util.function.Consumer;

/**
 * Tests for the display controller.
 *
 * Build/Install/Run:
 *  atest WMShellUnitTests:DisplayControllerTests
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class DisplayControllerTests extends ShellTestCase {
    @Mock private IWindowManager mWM;
    @Mock private ShellInit mShellInit;
    @Mock private DisplayManager mDisplayManager;
    @Mock private DisplayTopology mMockTopology;
    @Mock private DisplayController.OnDisplaysChangedListener mListener;
    private StaticMockitoSession mMockitoSession;
    private TestSyncExecutor mMainExecutor;
    private IDisplayWindowListener mDisplayContainerListener;
    private Consumer<DisplayTopology> mCapturedTopologyListener;
    private Display mMockDisplay;
    private DisplayController mController;
    private static final int DISPLAY_ID_0 = 0;
    private static final int DISPLAY_ID_1 = 1;
    private static final RectF DISPLAY_ABS_BOUNDS_0 = new RectF(10, 10, 20, 20);
    private static final RectF DISPLAY_ABS_BOUNDS_1 = new RectF(11, 11, 22, 22);

    @Before
    public void setUp() throws RemoteException {
        mMockitoSession =
                ExtendedMockito.mockitoSession()
                        .initMocks(this)
                        .mockStatic(DesktopModeStatus.class)
                        .strictness(Strictness.LENIENT)
                        .startMocking();

        mContext = spy(new TestableContext(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                        .getContext(), null));

        mMainExecutor = new TestSyncExecutor();
        mController = new DisplayController(
                mContext, mWM, mShellInit, mMainExecutor, mDisplayManager);

        mMockDisplay = mock(Display.class);
        when(mMockDisplay.getDisplayAdjustments()).thenReturn(
                new DisplayAdjustments(new Configuration()));
        when(mDisplayManager.getDisplay(anyInt())).thenReturn(mMockDisplay);
        when(mDisplayManager.getDisplayTopology()).thenReturn(mMockTopology);
        doAnswer(invocation -> {
            mDisplayContainerListener = invocation.getArgument(0);
            return new int[]{DISPLAY_ID_0};
        }).when(mWM).registerDisplayWindowListener(any());
        doAnswer(invocation -> {
            mCapturedTopologyListener = invocation.getArgument(1);
            return null;
        }).when(mDisplayManager).registerTopologyListener(any(), any());
        SparseArray<RectF> absoluteBounds = new SparseArray<>();
        absoluteBounds.put(DISPLAY_ID_0, DISPLAY_ABS_BOUNDS_0);
        absoluteBounds.put(DISPLAY_ID_1, DISPLAY_ABS_BOUNDS_1);
        when(mMockTopology.getAbsoluteBounds()).thenReturn(absoluteBounds);
    }

    @After
    public void tearDown() {
        if (mMockitoSession != null) {
            mMockitoSession.finishMocking();
        }
    }

    @Test
    public void instantiateController_addInitCallback() {
        verify(mShellInit, times(1)).addInitCallback(any(), eq(mController));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CONNECTED_DISPLAYS_WINDOW_DRAG)
    public void onInit_canEnterDesktopMode_registerListeners() throws RemoteException {
        ExtendedMockito.doReturn(true)
                .when(() -> DesktopModeStatus.canEnterDesktopMode(any()));

        mController.onInit();

        assertNotNull(mController.getDisplayContext(DISPLAY_ID_0));
        verify(mWM).registerDisplayWindowListener(any());
        verify(mDisplayManager).registerTopologyListener(eq(mMainExecutor), any());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CONNECTED_DISPLAYS_WINDOW_DRAG)
    public void onInit_canNotEnterDesktopMode_onlyRegisterDisplayWindowListener()
            throws RemoteException {
        ExtendedMockito.doReturn(false)
                .when(() -> DesktopModeStatus.canEnterDesktopMode(any()));

        mController.onInit();

        assertNotNull(mController.getDisplayContext(DISPLAY_ID_0));
        verify(mWM).registerDisplayWindowListener(any());
        verify(mDisplayManager, never()).registerTopologyListener(any(), any());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CONNECTED_DISPLAYS_WINDOW_DRAG)
    public void addDisplayWindowListener_notifiesExistingDisplaysAndTopology() {
        ExtendedMockito.doReturn(true)
                .when(() -> DesktopModeStatus.canEnterDesktopMode(any()));

        mController.onInit();
        mController.addDisplayWindowListener(mListener);

        verify(mListener).onDisplayAdded(eq(DISPLAY_ID_0));
        verify(mListener).onTopologyChanged(eq(mMockTopology));
    }

    @Test
    public void onDisplayAddedAndRemoved_updatesDisplayContexts() throws RemoteException {
        mController.onInit();
        mController.addDisplayWindowListener(mListener);

        mDisplayContainerListener.onDisplayAdded(DISPLAY_ID_1);

        verify(mListener).onDisplayAdded(eq(DISPLAY_ID_0));
        verify(mListener).onDisplayAdded(eq(DISPLAY_ID_1));
        assertNotNull(mController.getDisplayContext(DISPLAY_ID_1));
        verify(mContext).createDisplayContext(eq(mMockDisplay));

        mDisplayContainerListener.onDisplayRemoved(DISPLAY_ID_1);

        assertNull(mController.getDisplayContext(DISPLAY_ID_1));
        verify(mListener).onDisplayRemoved(eq(DISPLAY_ID_1));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CONNECTED_DISPLAYS_WINDOW_DRAG)
    public void onDisplayTopologyChanged_updateDisplayLayout() throws RemoteException {
        ExtendedMockito.doReturn(true)
                .when(() -> DesktopModeStatus.canEnterDesktopMode(any()));
        mController.onInit();
        mController.addDisplayWindowListener(mListener);
        mDisplayContainerListener.onDisplayAdded(DISPLAY_ID_1);

        mCapturedTopologyListener.accept(mMockTopology);

        assertEquals(DISPLAY_ABS_BOUNDS_0, mController.getDisplayLayout(DISPLAY_ID_0)
                .globalBoundsDp());
        assertEquals(DISPLAY_ABS_BOUNDS_1, mController.getDisplayLayout(DISPLAY_ID_1)
                .globalBoundsDp());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CONNECTED_DISPLAYS_WINDOW_DRAG)
    public void onDisplayTopologyChanged_topologyBeforeDisplayAdded_appliesBoundsOnAdd()
            throws RemoteException {
        ExtendedMockito.doReturn(true)
                .when(() -> DesktopModeStatus.canEnterDesktopMode(any()));
        mController.onInit();
        mController.addDisplayWindowListener(mListener);

        mCapturedTopologyListener.accept(mMockTopology);

        assertNull(mController.getDisplayLayout(DISPLAY_ID_1));

        mDisplayContainerListener.onDisplayAdded(DISPLAY_ID_1);

        assertEquals(DISPLAY_ABS_BOUNDS_0,
                mController.getDisplayLayout(DISPLAY_ID_0).globalBoundsDp());
        assertEquals(DISPLAY_ABS_BOUNDS_1,
                mController.getDisplayLayout(DISPLAY_ID_1).globalBoundsDp());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CONNECTED_DISPLAYS_WINDOW_DRAG)
    public void onDisplayConfigurationChanged_reInitDisplayLayout()
            throws RemoteException {
        ExtendedMockito.doReturn(true)
                .when(() -> DesktopModeStatus.canEnterDesktopMode(any()));
        mController.onInit();
        mController.addDisplayWindowListener(mListener);

        mCapturedTopologyListener.accept(mMockTopology);

        DisplayLayout displayLayoutBefore = mController.getDisplayLayout(DISPLAY_ID_0);
        mDisplayContainerListener.onDisplayConfigurationChanged(DISPLAY_ID_0, new Configuration());
        DisplayLayout displayLayoutAfter = mController.getDisplayLayout(DISPLAY_ID_0);

        assertNotSame(displayLayoutBefore, displayLayoutAfter);
        assertEquals(DISPLAY_ABS_BOUNDS_0,
                mController.getDisplayLayout(DISPLAY_ID_0).globalBoundsDp());
    }
}
