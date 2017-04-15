## Launcher3

#### 1.基于最新Launcher_7.1.1_r38版本

原始版本在分支里面：[launcher3_6.0](https://github.com/yuchuangu85/Launcher3_mx/tree/launcher3_6.0)

#### 2.修改配置：

* Android studio 2.3.1

* classpath 'com.android.tools.build:gradle:2.3.1'

* distributionUrl=https\://services.gradle.org/distributions/gradle-3.3-all.zip

#### 3.如果不能安装，修改包名以及provider的设置名称

* build.gradle中的 applicationId 'com.android.launcher3'改成对应包名

* AndroidManifest.xml中的所有"com.android.launcher3"的改成对应的包名

* 代码中ProviderConfig.java中的"com.android.launcher3.settings"改成对应包名，同时修改AndroidManifest中的
provider中的android:authorities="com.android.launcher3.settings"以及 android:name="com.codemx.launcher3.LauncherProvider"
改成对应包名，以及provider中的writePermission和readPermission权限的包名也要改成对应包名，这个权限对应：

    <permission
        android:name="com.codemx.launcher3.permission.READ_SETTINGS"
        android:permissionGroup="android.permission-group.SYSTEM_TOOLS"
        android:protectionLevel="normal"
        android:label="@string/permlab_read_settings"
        android:description="@string/permdesc_read_settings"/>
        
    <permission
        android:name="com.codemx.launcher3.permission.WRITE_SETTINGS"
        android:permissionGroup="android.permission-group.SYSTEM_TOOLS"
        android:protectionLevel="signatureOrSystem"
        android:label="@string/permlab_write_settings"
        android:description="@string/permdesc_write_settings"/>

    这个权限和provider中的权限是对应的，都需要改包名，如果不改都不改。

* protos包中的launcher_log.proto中的option java_package = "com.android.launcher3.userevent.nano"改成对应包名

* xml文件夹中文件修改：所有文件中的xmlns:launcher="http://schemas.android.com/apk/res-auto/com.android.launcher3"
里面的"com.android.launcher3"改成你对应的包名，另外，backupscheme.xml中<include domain="sharedpref" path="com.android.launcher3.prefs.xml" />
中的"com.android.launcher3"改成对应包名

* 其实就是所有"com.android.launcher3"的都改，没有"3"的都不改

#### 4.预览图

<img width="320" src="/art/launcher1.jpg"/>    <img width="320" src="/art/launcher2.jpg"/>

<img width="320" src="/art/launcher3.jpg"/>    <img width="320" src="/art/launcher4.jpg"/>

<img width="320" src="/art/launcher5.jpg"/>


