# RecentsAnimationController 架构分析

## 概述

在 Android 系统中，存在两个同名的 `RecentsAnimationController` 类，它们分别位于不同的包中，扮演着客户端和服务端的角色：

1. **客户端（Launcher3）**: `com.android.quickstep.RecentsAnimationController`
2. **服务端（WindowManager）**: `com.android.server.wm.RecentsAnimationController`

这两个类通过 AIDL（Android Interface Definition Language）接口进行通信，共同实现了 Android 的 Recent Apps（最近任务）动画功能。

---

## 1. 客户端：`com.android.quickstep.RecentsAnimationController`

### 1.1 位置与职责

**文件路径**: `quickstep/src/com/android/quickstep/RecentsAnimationController.java`

**主要职责**:
- 作为 Launcher3 中 recents 动画的控制器包装类
- 提供 UI 线程安全的接口
- 管理动画的生命周期（启动、完成、取消）
- 协调与 SystemUI 的交互

### 1.2 核心组件

```java
public class RecentsAnimationController {
    // 核心成员
    private final RecentsAnimationControllerCompat mController;
    private final Consumer<RecentsAnimationController> mOnFinishedListener;

    // 状态管理
    private boolean mUseLauncherSysBarFlags = false;
    private boolean mFinishRequested = false;
    private boolean mFinishTargetIsLauncher;
    private boolean mLauncherIsVisibleAtFinish;
    private RunnableList mPendingFinishCallbacks;
}
```

**关键成员说明**:

1. **`RecentsAnimationControllerCompat`**: 这是一个兼容层包装类，实际上是对 `IRecentsAnimationController` AIDL 接口的封装，用于与服务端通信。

2. **`mOnFinishedListener`**: 动画完成时的回调监听器。

3. **状态标志**:
   - `mFinishRequested`: 标记是否已请求完成动画
   - `mFinishTargetIsLauncher`: 动画结束时目标是否为 Launcher（而非应用）
   - `mUseLauncherSysBarFlags`: 是否使用 Launcher 的系统栏标志

### 1.3 主要功能方法

#### a. 完成动画

```java
@UiThread
public void finish(boolean toRecents, Runnable onFinishComplete, boolean sendUserLeaveHint) {
    finishController(toRecents, onFinishComplete, sendUserLeaveHint);
}

private void finishController(boolean toRecents, boolean launcherIsVisibleAtFinish,
        Runnable callback, boolean sendUserLeaveHint, boolean forceFinish) {
    mPendingFinishCallbacks.add(callback);
    mFinishRequested = true;
    mFinishTargetIsLauncher = toRecents;
    mLauncherIsVisibleAtFinish = launcherIsVisibleAtFinish;

    // 通过 AIDL 调用服务端的 finish 方法
    mController.finish(toRecents, sendUserLeaveHint, resultReceiver);
}
```

**参数说明**:
- `toRecents`: `true` 表示返回到 Launcher/Recents，`false` 表示返回到应用
- `sendUserLeaveHint`: 是否发送用户离开提示（用于 PiP 模式）
- `forceFinish`: 是否强制同步完成

#### b. 系统栏标志管理

```java
public void setUseLauncherSystemBarFlags(boolean useLauncherSysBarFlags) {
    if (mUseLauncherSysBarFlags != useLauncherSysBarFlags) {
        mUseLauncherSysBarFlags = useLauncherSysBarFlags;
        UI_HELPER_EXECUTOR.execute(() -> {
            WindowManagerGlobal.getWindowManagerService()
                .setRecentsAppBehindSystemBars(useLauncherSysBarFlags);
        });
    }
}
```

这个方法在手势跨越窗口边界阈值时被调用，通知系统更新系统栏标志。

#### c. 截图功能

```java
public ThumbnailData screenshotTask(int taskId) {
    return ActivityManagerWrapper.getInstance().takeTaskThumbnail(taskId);
}
```

同步获取正在动画的任务的缩略图。

#### d. 输入消费者控制

```java
public void enableInputConsumer() {
    UI_HELPER_EXECUTOR.submit(() -> {
        mController.setInputConsumerEnabled(true);
    });
}
```

启用输入消费者以开始拦截应用窗口中的触摸事件。

---

## 2. 服务端：`com.android.server.wm.RecentsAnimationController`

### 2.1 位置与职责

**所在包**: `frameworks/base/services/core/java/com/android/server/wm/`

**主要职责**:
- 在 WindowManagerService 中真正执行动画控制
- 管理 Surface 事务和窗口层级
- 控制任务和活动的可见性
- 处理系统级的动画状态

### 2.2 核心功能

虽然我们无法直接查看服务端代码（它不在 Launcher3 仓库中），但根据 AIDL 接口定义，服务端控制器主要负责：

1. **窗口动画管理**:
   - 创建和管理动画目标（RemoteAnimationTarget）
   - 控制任务的 Surface 层级
   - 管理壁纸和非应用窗口的动画

2. **生命周期管理**:
   - 初始化 recents 动画
   - 提供动画目标给客户端
   - 处理动画完成和取消

3. **Surface 控制**:
   - 管理任务 Surface 的 leash（租约）
   - 应用最终的 Surface 事务
   - 处理 PiP（画中画）模式的 Surface 转换

4. **输入处理**:
   - 控制输入消费者的启用/禁用
   - 管理手势导航期间的触摸事件

---

## 3. AIDL 接口：通信桥梁

### 3.1 `IRecentsAnimationController`

这是定义在 `com.android.wm.shell.recents` 包中的 AIDL 接口，用于客户端与服务端通信。

**主要方法**（基于代码中的使用推断）:

```java
interface IRecentsAnimationController {
    // 完成动画
    void finish(boolean toHome, boolean sendUserLeaveHint, IResultReceiver resultReceiver);

    // 分离导航栏
    void detachNavigationBarFromApp(boolean moveHomeToTop);

    // 设置是否将要返回主屏幕
    void setWillFinishToHome(boolean willFinishToHome);

    // 设置任务完成时的 Surface 事务（用于 PiP）
    void setFinishTaskTransaction(int taskId,
        PictureInPictureSurfaceTransaction finishTransaction,
        SurfaceControl overlay);

    // 控制输入消费者
    void setInputConsumerEnabled(boolean enabled);

    // 交接动画控制权（用于长时间存活的返回动画）
    void handOffAnimation(RemoteAnimationTarget[] targets,
        WindowAnimationState[] states);
}
```

### 3.2 `IRecentsAnimationRunner`

客户端实现的 AIDL 接口，用于接收服务端的回调。

**在 Launcher3 中的实现**（`SystemUiProxy.kt:1223`）:

```kotlin
private class RecentsAnimationListenerStub(
    val listener: RecentsAnimationListener
) : IRecentsAnimationRunner.Stub() {

    override fun onAnimationStart(
        controller: IRecentsAnimationController,
        apps: Array<RemoteAnimationTarget>?,
        wallpapers: Array<RemoteAnimationTarget>?,
        homeContentInsets: Rect?,
        minimizedHomeBounds: Rect?,
        extras: Bundle?,
        transitionInfo: TransitionInfo?
    ) {
        // 接收动画启动回调
        val compat = RecentsAnimationControllerCompat(controller)
        listener.onAnimationStart(compat, apps, wallpapers, ...)
    }

    override fun onAnimationCanceled(thumbnailDatas: HashMap<Int, ThumbnailData>?) {
        // 接收动画取消回调
        listener.onAnimationCanceled(thumbnailDatas)
    }

    override fun onTasksAppeared(apps: Array<RemoteAnimationTarget>?) {
        // 任务出现回调
        listener.onTasksAppeared(apps)
    }
}
```

---

## 4. 连接机制：Launcher3 如何关联两个类

### 4.1 整体架构流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                        Launcher3 进程                             │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ TaskAnimationManager                                     │    │
│  │  - 管理手势状态                                          │    │
│  │  - 创建 RecentsAnimationCallbacks                        │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │ startRecentsAnimation()                    │
│                     ▼                                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ SystemUiProxy (单例)                                     │    │
│  │  - startRecentsActivity()                                │    │
│  │  - 持有 IRecentTasks 引用                                │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │ IPC Call                                   │
└─────────────────────┼────────────────────────────────────────────┘
                      │
                      │ Binder IPC
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SystemUI Shell 进程                          │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ RecentTasks (IRecentTasks.Stub)                          │    │
│  │  - startRecentsTransition()                              │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │                                             │
│                     ▼                                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ RecentsAnimationController (Shell 实现)                  │    │
│  │  - 管理 Shell 侧的动画逻辑                               │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │ 通过 WM Shell 接口                         │
└─────────────────────┼────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                   System Server 进程                             │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ WindowManagerService                                     │    │
│  │  └─ RecentsAnimationController (WM 实现)                 │    │
│  │     - 真正的窗口动画控制                                 │    │
│  │     - Surface 事务管理                                   │    │
│  │     - 窗口层级控制                                       │    │
│  └─────────────────┬────────────────────────────────────────┘    │
│                    │ 回调                                         │
└────────────────────┼──────────────────────────────────────────────┘
                     │
                     │ IRecentsAnimationController (AIDL)
                     ▼
         ┌───────────────────────────┐
         │  回到 Launcher3            │
         │  RecentsAnimationCallbacks │
         │  ├─ onAnimationStart()     │
         │  ├─ onAnimationCanceled()  │
         │  └─ onTasksAppeared()      │
         └───────────────────────────┘
```

### 4.2 详细连接步骤

#### 步骤 1: 启动 Recents 动画

**触发点**: 用户执行手势（如向上滑动）

```java
// TaskAnimationManager.java:133
public RecentsAnimationCallbacks startRecentsAnimation(
        @NonNull GestureState gestureState,
        Intent intent,
        RecentsAnimationCallbacks.RecentsAnimationListener listener) {

    // 1. 创建回调对象
    RecentsAnimationCallbacks newCallbacks =
        new RecentsAnimationCallbacks(getSystemUiProxy());

    // 2. 添加监听器
    mCallbacks = newCallbacks;
    mCallbacks.addListener(listener);
    mCallbacks.addListener(gestureState);

    // 3. 准备 ActivityOptions
    final ActivityOptions options = ActivityOptions.makeBasic();
    options.setTransientLaunch();
    options.setSourceInfo(ActivityOptions.SourceInfo.TYPE_RECENTS_ANIMATION, eventTime);

    // 4. 通过 SystemUiProxy 启动
    getSystemUiProxy().startRecentsActivity(intent, options, mCallbacks, false);

    return mCallbacks;
}
```

#### 步骤 2: SystemUiProxy 桥接

**SystemUiProxy.kt:1196**

```kotlin
fun startRecentsActivity(
    intent: Intent?,
    options: ActivityOptions,
    listener: RecentsAnimationListener,
    useSyntheticRecentsTransition: Boolean,
): Boolean {
    // 调用 Shell 层的 IRecentTasks 接口
    recentTasks?.startRecentsTransition(
        recentsPendingIntent,
        intent,
        options.toBundle(),
        context.iApplicationThread,
        RecentsAnimationListenerStub(listener),  // ← AIDL Stub 实现
    )
    return true
}
```

**关键组件**:

1. **`recentTasks: IRecentTasks`**: 这是与 SystemUI Shell 通信的 AIDL 接口引用

2. **`RecentsAnimationListenerStub`**: 将 Launcher3 的监听器包装成 AIDL Stub，用于接收服务端回调

#### 步骤 3: 服务端创建动画控制器

服务端（WindowManagerService）收到请求后：

1. 创建 `com.android.server.wm.RecentsAnimationController` 实例
2. 准备动画目标（RemoteAnimationTarget）
3. 通过 `IRecentsAnimationRunner.onAnimationStart()` 回调客户端

#### 步骤 4: 客户端接收回调

**RecentsAnimationCallbacks.java:104**

```java
@BinderThread
public final void onAnimationStart(
        RecentsAnimationControllerCompat animationController,
        RemoteAnimationTarget[] appTargets,
        RemoteAnimationTarget[] wallpaperTargets,
        Rect homeContentInsets,
        Rect minimizedHomeBounds,
        Bundle extras,
        @Nullable TransitionInfo transitionInfo) {

    // 1. 创建 Launcher3 的 RecentsAnimationController
    mController = new RecentsAnimationController(
        animationController,  // ← 包装了 IRecentsAnimationController
        this::onAnimationFinished
    );

    // 2. 创建动画目标包装
    final RecentsAnimationTargets targets = new RecentsAnimationTargets(
        appTargets, wallpaperTargets, nonAppTargets,
        homeContentInsets, minimizedHomeBounds, extras
    );

    // 3. 分发给所有监听器
    Utilities.postAsyncCallback(MAIN_EXECUTOR.getHandler(), () -> {
        for (RecentsAnimationListener listener : getListeners()) {
            listener.onRecentsAnimationStart(mController, targets, transitionInfo);
        }
    });
}
```

**关键点**:
- `RecentsAnimationControllerCompat` 是对 `IRecentsAnimationController` AIDL 接口的包装
- 客户端的 `RecentsAnimationController` 包装了这个 Compat 对象
- 所有操作通过 AIDL 转发到服务端

### 4.3 关键包装类

#### `RecentsAnimationControllerCompat`

这是 SystemUI Shared 库中的类，位于：
`com.android.systemui.shared.system.RecentsAnimationControllerCompat`

**作用**:
```java
public class RecentsAnimationControllerCompat {
    private final IRecentsAnimationController mAnimationController;

    public RecentsAnimationControllerCompat(IRecentsAnimationController controller) {
        mAnimationController = controller;
    }

    public void finish(boolean toHome, boolean sendUserLeaveHint,
                      IResultReceiver resultReceiver) {
        try {
            mAnimationController.finish(toHome, sendUserLeaveHint, resultReceiver);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to finish recents animation", e);
        }
    }

    // 其他方法类似，都是对 AIDL 接口的简单包装
}
```

---

## 5. 数据流向

### 5.1 启动流程数据流

```
用户手势
   │
   ▼
TouchInteractionService (监听手势)
   │
   ▼
TaskAnimationManager.startRecentsAnimation()
   │
   ├─ 创建 RecentsAnimationCallbacks
   │
   ▼
SystemUiProxy.startRecentsActivity()
   │
   ├─ 封装 RecentsAnimationListenerStub (AIDL)
   │
   ▼
[Binder IPC]
   │
   ▼
SystemUI Shell: RecentTasks.startRecentsTransition()
   │
   ▼
WindowManagerService
   │
   ├─ 创建 RecentsAnimationController (服务端)
   ├─ 准备 RemoteAnimationTarget[]
   │
   ▼
[Binder IPC 回调]
   │
   ▼
IRecentsAnimationRunner.onAnimationStart()
   │
   ▼
RecentsAnimationListenerStub.onAnimationStart()
   │
   ├─ 创建 RecentsAnimationControllerCompat
   │     (包装 IRecentsAnimationController)
   │
   ▼
RecentsAnimationCallbacks.onAnimationStart()
   │
   ├─ 创建 RecentsAnimationController (客户端)
   │     (包装 RecentsAnimationControllerCompat)
   │
   ▼
分发给各个监听器:
   ├─ AbsSwipeUpHandler
   ├─ TaskAnimationManager
   └─ GestureState
```

### 5.2 完成流程数据流

```
Launcher3 UI 事件
   │
   ▼
RecentsAnimationController.finish()
   │
   ▼
RecentsAnimationControllerCompat.finish()
   │
   ▼
[Binder IPC]
   │
   ▼
IRecentsAnimationController.finish()
   │
   ▼
WindowManager RecentsAnimationController
   │
   ├─ 应用最终 Surface 事务
   ├─ 恢复窗口状态
   ├─ 清理动画资源
   │
   ▼
IResultReceiver.send() (回调完成)
   │
   ▼
[Binder IPC]
   │
   ▼
RecentsAnimationController (客户端)
   │
   ├─ 执行 mPendingFinishCallbacks
   │
   ▼
清理 Launcher3 侧资源
```

---

## 6. 关键交互场景

### 6.1 场景 1: 从应用滑动到 Launcher

```java
// 1. 用户开始滑动
AbsSwipeUpHandler.onGestureStarted()
    │
    ▼
// 2. 启动 recents 动画
TaskAnimationManager.startRecentsAnimation()
    │
    ▼
// 3. 接收动画开始回调
AbsSwipeUpHandler.onRecentsAnimationStart(controller, targets)
    │
    ├─ 获取应用窗口的 Surface
    ├─ 创建过渡动画
    │
    ▼
// 4. 用户完成滑动手势
AbsSwipeUpHandler.handleTaskAppeared()
    │
    ├─ 判断目标（Launcher 或 App）
    │
    ▼
// 5. 完成动画
controller.finish(toRecents=true)
    │
    ▼
// 6. 系统切换到 Launcher
WindowManager 完成窗口切换
```

### 6.2 场景 2: PiP（画中画）模式

```java
// 1. Launcher 通知即将进入 PiP
RecentsAnimationController.setFinishTaskTransaction(
    taskId,
    pipTransaction,  // PiP 的 Surface 事务
    overlay          // 覆盖层
)
    │
    ▼
// 2. 执行 PiP 动画
// ... 动画过程 ...
    │
    ▼
// 3. 完成并应用 PiP 事务
controller.finish(toRecents=true, sendUserLeaveHint=true)
    │
    ▼
// 4. WindowManager 应用 pipTransaction
//    将任务调整为 PiP 大小和位置
```

### 6.3 场景 3: 动画取消

```java
// 1. 用户中途取消手势
InputConsumer.onTouchEvent(ACTION_CANCEL)
    │
    ▼
// 2. 请求取消动画
SystemUiProxy.cancelRecentsAnimation()
    │
    ▼
[Binder IPC]
    │
    ▼
// 3. 服务端发送取消回调
IRecentsAnimationRunner.onAnimationCanceled(thumbnailDatas)
    │
    ▼
// 4. 客户端处理取消
RecentsAnimationCallbacks.onAnimationCanceled()
    │
    ├─ 恢复原始窗口状态
    ├─ 使用 thumbnailData 显示截图
    └─ 清理资源
```

---

## 7. 线程模型

### 7.1 客户端线程

Launcher3 的 `RecentsAnimationController` 严格要求在 UI 线程操作：

```java
@UiThread
public void finish(boolean toRecents, Runnable onFinishComplete) {
    Preconditions.assertUIThread();  // ← 断言必须在 UI 线程
    finishController(toRecents, onFinishComplete, false);
}
```

但实际的 IPC 调用在后台线程执行：

```java
UI_HELPER_EXECUTOR.execute(() -> {
    mController.finish(toRecents, sendUserLeaveHint, resultReceiver);
});
```

### 7.2 服务端线程

服务端的 `RecentsAnimationController` 运行在 WindowManager 的同步事务线程上，确保窗口状态的一致性。

### 7.3 回调线程

AIDL 回调发生在 Binder 线程池，因此需要切换到主线程：

```java
@BinderThread
public final void onAnimationStart(...) {
    // 切换到主线程
    Utilities.postAsyncCallback(MAIN_EXECUTOR.getHandler(), () -> {
        for (RecentsAnimationListener listener : getListeners()) {
            listener.onRecentsAnimationStart(mController, targets, transitionInfo);
        }
    });
}
```

---

## 8. 总结

### 8.1 两个 RecentsAnimationController 的关系

| 方面 | 客户端 (Launcher3) | 服务端 (WindowManager) |
|------|-------------------|----------------------|
| **包名** | `com.android.quickstep` | `com.android.server.wm` |
| **职责** | UI 控制、手势处理、回调管理 | 窗口管理、Surface 控制、系统动画 |
| **线程** | UI 线程 + UI Helper 线程 | WM 同步事务线程 |
| **生命周期** | 由手势触发，跟随动画周期 | 由 WMS 管理，独立于应用 |
| **通信方式** | 通过 AIDL 调用服务端 | 通过 AIDL 回调客户端 |

### 8.2 关键设计模式

1. **代理模式**: `RecentsAnimationControllerCompat` 代理 `IRecentsAnimationController`

2. **观察者模式**: `RecentsAnimationCallbacks` 管理多个监听器

3. **门面模式**: `SystemUiProxy` 统一管理所有系统 UI 交互

4. **包装器模式**: 客户端的 `RecentsAnimationController` 包装 Compat 对象

### 8.3 核心交互链路

```
Launcher3 Gesture
    ↓
TaskAnimationManager
    ↓
SystemUiProxy (IRecentTasks)
    ↓ [Binder IPC]
Shell RecentsAnimationController
    ↓
WindowManagerService RecentsAnimationController
    ↓ [AIDL Callback]
RecentsAnimationListenerStub
    ↓
RecentsAnimationCallbacks
    ↓
Launcher3 RecentsAnimationController
    ↓
AbsSwipeUpHandler (处理手势)
```

### 8.4 关键要点

1. **进程隔离**: 客户端在 Launcher3 进程，服务端在 System Server 进程

2. **AIDL 通信**: 通过 `IRecentsAnimationController` 和 `IRecentsAnimationRunner` 双向通信

3. **线程安全**: 客户端确保 UI 线程安全，服务端在 WM 事务线程

4. **生命周期管理**: 动画从启动到完成/取消的完整生命周期管理

5. **异步回调**: 所有操作都是异步的，通过回调通知结果

这种设计实现了职责分离、进程隔离和高性能的动画系统，是 Android 现代手势导航的核心基础设施。
