# Launcher代码分析
---

## 一.概述

概述:[墨香带你学Launcher之（一）--概述](http://www.codemx.cn/2016/07/30/墨香带你学Launcher之-概述/)

## 二.数据加载流程分析:

详解:[墨香带你学Launcher之（二）-数据加载流程](http://www.codemx.cn/2016/08/05/墨香带你学Launcher之-数据加载流程/)

1.LauncherProvider:loadDefaultFavoritesIfNecessary

loadDefaultFavoritesIfNecessary-->

构造loader(AutoInstallsLayout)-->

loadFavorites(DatabaseHelper)——>

loadLayout(AutoInstallsLayout)-->

parseLayout-->parseAndAddNode-->

parseAndAdd(TagParser(接口):分为AppShortcutParser,AppWidgetParser,FolderParser等实现,在AutoInstallsLayout中)
-->将解析出来的xml中配置的应用数据存储到数据库中

2.LauncherProvider:loadWorkspace

loadWorkspace

-->loadDefaultFavoritesIfNecessary(解析xml中的配置文件写入数据库)

-->loadWorkspaceScreensDb(获取解析数据库中的屏幕id)

-->查询数据库,判断数据库中的App是否可用(系统中是否存在)

-->bindWorkspace(绑定过程要分类:ShortcutInfo在Hotseat中还是workspace中,widget,Folder)

loadAndBindAllApps

-->loadAllApps

-->bindAllApplications(加载所有应用并且绑定)

-->loadAndBindWidgetsAndShortcuts(加载widget并且绑定)

-->updateWidgetsModel


## 三.绑定屏幕、图标、小部件以及文件夹:

详解:[墨香带你学Launcher之（三）-绑定屏幕、图标、文件夹和Widget](http://www.codemx.cn/2016/08/14/墨香带你学Launcher之-绑定/)

1.bindWorkspaceScreens

2.bindWorkspaceItems

3.callbacks.bindAllApplications

4.loadAndBindWidgetsAndShortcuts(Widgets)

## 四.安装,更新,卸载应用数据加载

详解:[墨香带你学Launcher之(四)-应用安装、更新、卸载时的数据加载](http://www.codemx.cn/2016/08/21/墨香带你学Launcher之-应用安装、更新、卸载时的数据加载/)

## 五.Workspace的滑动

## 六.拖拽(应用拖拽到桌面,合成文件夹,桌面上图标和widget的拖拽)

Launcher:

-->onLongClick

-->mWorkspace.startDrag

-->beginDragShared

-->mDragController.startDrag

-->handleMoveEvent

-->checkTouchMove

-->dropTarget.onDragEnter

-->dropTarget.onDragOver(-->ButtonDropTarget&Folder&Workspace)


## 七.小部件的加载和添加

## 八.更换壁纸,更改icon以及界面布局
