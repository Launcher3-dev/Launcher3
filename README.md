# Launcher代码分析

## 一.数据加载流程分析:

1.LauncherProvider:loadDefaultFavoritesIfNecessary
loadDefaultFavoritesIfNecessary-->构造loader(AutoInstallsLayout)-->loadFavorites(DatabaseHelper)——>
loadLayout(AutoInstallsLayout)-->parseLayout-->parseAndAddNode-->
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


## 二.绑定图标,小部件以及文件夹:

## 三.安装,更新,卸载应用数据加载

## 四.Workspace的滑动

## 五.拖拽(应用拖拽到桌面,合成文件夹,桌面上图标和widget的拖拽)

Launcher:
-->onLongClick
-->mWorkspace.startDrag
-->beginDragShared
-->mDragController.startDrag
-->handleMoveEvent
-->checkTouchMove
-->dropTarget.onDragEnter
-->dropTarget.onDragOver(-->ButtonDropTarget&Folder&Workspace)


## 六.小部件的加载和添加

## 七.更换壁纸,更改icon以及界面布局

## 八.