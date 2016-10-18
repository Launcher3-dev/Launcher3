# Launcher代码分析
---

## 一.概述

概述:[墨香带你学Launcher之（一）--概述](http://www.codemx.cn/2016/07/30/Launcher01/)

## 二.数据加载流程分析:

详解:[墨香带你学Launcher之（二）-数据加载流程](http://www.codemx.cn/2016/08/05/Launcher02/)

## 三.绑定屏幕、图标、小部件以及文件夹:

详解:[墨香带你学Launcher之（三）-绑定屏幕、图标、文件夹和Widget](http://www.codemx.cn/2016/08/14/Launcher03/)

## 四.安装,更新,卸载应用数据加载

详解:[墨香带你学Launcher之(四)-应用安装、更新、卸载时的数据加载](http://www.codemx.cn/2016/08/21/Launcher04/)

## 五.Workspace的滑动

详解:[墨香带你学Launcher之（五）-Workspace滑动](http://www.codemx.cn/2016/10/16/Launcher05/)

## 六.拖拽(应用拖拽到桌面,合成文件夹,桌面上图标和widget的拖拽)

1.Launcher:

-->onLongClick

-->mWorkspace.startDrag

-->beginDragShared

-->mDragController.startDrag

-->handleMoveEvent

-->checkTouchMove

-->dropTarget.onDragEnter

-->dropTarget.onDragOver(-->ButtonDropTarget&Folder&Workspace)

2.WidgetsContainerView:

-->onLongClick

-->beginDragging

-->beginDraggingWidget

-->mDragController.startDrag

-->startDrag

-->DragListener.onDragStart

-->handleMoveEvent

-->checkTouchMove

-->DropTarget.onDragExit

-->DropTarget.onDragEnter

-->DropTarget.onDragOver


## 七.小部件的加载和添加

## 八.更换壁纸,更改icon以及界面布局

## 九.Widget大小调节:

DragLayer-->addResizeFrame
