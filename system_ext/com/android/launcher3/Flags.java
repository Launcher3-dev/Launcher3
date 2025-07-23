package com.android.launcher3;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)

//import android.compat.annotation.UnsupportedAppUsage;
//import com.android.aconfig.annotations.AssumeFalseForR8;

public final class Flags {
    public static final String FLAG_ENABLE_ADD_APP_WIDGET_VIA_CONFIG_ACTIVITY_V2 = "com.android.launcher3.enable_add_app_widget_via_config_activity_v2";
    public static final String FLAG_ENABLE_ADDITIONAL_HOME_ANIMATIONS = "com.android.launcher3.enable_additional_home_animations";
    public static final String FLAG_ENABLE_CATEGORIZED_WIDGET_SUGGESTIONS = "com.android.launcher3.enable_categorized_widget_suggestions";
    public static final String FLAG_ENABLE_CURSOR_HOVER_STATES = "com.android.launcher3.enable_cursor_hover_states";
    public static final String FLAG_ENABLE_EXPANDING_PAUSE_WORK_BUTTON = "com.android.launcher3.enable_expanding_pause_work_button";
    public static final String FLAG_ENABLE_FALLBACK_OVERVIEW_IN_WINDOW = "com.android.launcher3.enable_fallback_overview_in_window";
    public static final String FLAG_ENABLE_FIRST_SCREEN_BROADCAST_ARCHIVING_EXTRAS = "com.android.launcher3.enable_first_screen_broadcast_archiving_extras";
    public static final String FLAG_ENABLE_FOCUS_OUTLINE = "com.android.launcher3.enable_focus_outline";
    public static final String FLAG_ENABLE_GENERATED_PREVIEWS = "com.android.launcher3.enable_generated_previews";
    public static final String FLAG_ENABLE_GRID_MIGRATION_FIX = "com.android.launcher3.enable_grid_migration_fix";
    public static final String FLAG_ENABLE_GRID_ONLY_OVERVIEW = "com.android.launcher3.enable_grid_only_overview";
    public static final String FLAG_ENABLE_HANDLE_DELAYED_GESTURE_CALLBACKS = "com.android.launcher3.enable_handle_delayed_gesture_callbacks";
    public static final String FLAG_ENABLE_HOME_TRANSITION_LISTENER = "com.android.launcher3.enable_home_transition_listener";
    public static final String FLAG_ENABLE_LAUNCHER_BR_METRICS_FIXED = "com.android.launcher3.enable_launcher_br_metrics_fixed";
    public static final String FLAG_ENABLE_NARROW_GRID_RESTORE = "com.android.launcher3.enable_narrow_grid_restore";
    public static final String FLAG_ENABLE_OVERVIEW_ICON_MENU = "com.android.launcher3.enable_overview_icon_menu";
    public static final String FLAG_ENABLE_PREDICTIVE_BACK_GESTURE = "com.android.launcher3.enable_predictive_back_gesture";
    public static final String FLAG_ENABLE_PRIVATE_SPACE = "com.android.launcher3.enable_private_space";
    public static final String FLAG_ENABLE_PRIVATE_SPACE_INSTALL_SHORTCUT = "com.android.launcher3.enable_private_space_install_shortcut";
    public static final String FLAG_ENABLE_REBOOT_UNLOCK_ANIMATION = "com.android.launcher3.enable_reboot_unlock_animation";
    public static final String FLAG_ENABLE_RECENTS_IN_TASKBAR = "com.android.launcher3.enable_recents_in_taskbar";
    public static final String FLAG_ENABLE_REFACTOR_TASK_THUMBNAIL = "com.android.launcher3.enable_refactor_task_thumbnail";
    public static final String FLAG_ENABLE_RESPONSIVE_WORKSPACE = "com.android.launcher3.enable_responsive_workspace";
    public static final String FLAG_ENABLE_SCALING_REVEAL_HOME_ANIMATION = "com.android.launcher3.enable_scaling_reveal_home_animation";
    public static final String FLAG_ENABLE_SHORTCUT_DONT_SUGGEST_APP = "com.android.launcher3.enable_shortcut_dont_suggest_app";
    public static final String FLAG_ENABLE_SMARTSPACE_AS_A_WIDGET = "com.android.launcher3.enable_smartspace_as_a_widget";
    public static final String FLAG_ENABLE_SMARTSPACE_REMOVAL_TOGGLE = "com.android.launcher3.enable_smartspace_removal_toggle";
    public static final String FLAG_ENABLE_SUPPORT_FOR_ARCHIVING = "com.android.launcher3.enable_support_for_archiving";
    public static final String FLAG_ENABLE_TABLET_TWO_PANE_PICKER_V2 = "com.android.launcher3.enable_tablet_two_pane_picker_v2";
    public static final String FLAG_ENABLE_TASKBAR_CUSTOMIZATION = "com.android.launcher3.enable_taskbar_customization";
    public static final String FLAG_ENABLE_TASKBAR_NO_RECREATE = "com.android.launcher3.enable_taskbar_no_recreate";
    public static final String FLAG_ENABLE_TASKBAR_PINNING = "com.android.launcher3.enable_taskbar_pinning";
    public static final String FLAG_ENABLE_TWO_PANE_LAUNCHER_SETTINGS = "com.android.launcher3.enable_two_pane_launcher_settings";
    public static final String FLAG_ENABLE_TWOLINE_ALLAPPS = "com.android.launcher3.enable_twoline_allapps";
    public static final String FLAG_ENABLE_TWOLINE_TOGGLE = "com.android.launcher3.enable_twoline_toggle";
    public static final String FLAG_ENABLE_UNFOLD_STATE_ANIMATION = "com.android.launcher3.enable_unfold_state_animation";
    public static final String FLAG_ENABLE_UNFOLDED_TWO_PANE_PICKER = "com.android.launcher3.enable_unfolded_two_pane_picker";
    public static final String FLAG_ENABLE_WIDGET_TAP_TO_ADD = "com.android.launcher3.enable_widget_tap_to_add";
    public static final String FLAG_ENABLE_WORKSPACE_INFLATION = "com.android.launcher3.enable_workspace_inflation";
    public static final String FLAG_ENABLED_FOLDERS_IN_ALL_APPS = "com.android.launcher3.enabled_folders_in_all_apps";
    public static final String FLAG_FLOATING_SEARCH_BAR = "com.android.launcher3.floating_search_bar";
    public static final String FLAG_FORCE_MONOCHROME_APP_ICONS = "com.android.launcher3.force_monochrome_app_icons";
    public static final String FLAG_PRIVATE_SPACE_ADD_FLOATING_MASK_VIEW = "com.android.launcher3.private_space_add_floating_mask_view";
    public static final String FLAG_PRIVATE_SPACE_ANIMATION = "com.android.launcher3.private_space_animation";
    public static final String FLAG_PRIVATE_SPACE_APP_INSTALLER_BUTTON = "com.android.launcher3.private_space_app_installer_button";
    public static final String FLAG_PRIVATE_SPACE_RESTRICT_ACCESSIBILITY_DRAG = "com.android.launcher3.private_space_restrict_accessibility_drag";
    public static final String FLAG_PRIVATE_SPACE_RESTRICT_ITEM_DRAG = "com.android.launcher3.private_space_restrict_item_drag";
    public static final String FLAG_PRIVATE_SPACE_SYS_APPS_SEPARATION = "com.android.launcher3.private_space_sys_apps_separation";
    public static final String FLAG_USE_ACTIVITY_OVERLAY = "com.android.launcher3.use_activity_overlay";
    private static FeatureFlags FEATURE_FLAGS = new FeatureFlagsImpl();

    public Flags() {
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableAddAppWidgetViaConfigActivityV2() {
        return FEATURE_FLAGS.enableAddAppWidgetViaConfigActivityV2();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableAdditionalHomeAnimations() {
        return FEATURE_FLAGS.enableAdditionalHomeAnimations();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableCategorizedWidgetSuggestions() {
        return FEATURE_FLAGS.enableCategorizedWidgetSuggestions();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableCursorHoverStates() {
        return FEATURE_FLAGS.enableCursorHoverStates();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableExpandingPauseWorkButton() {
        return FEATURE_FLAGS.enableExpandingPauseWorkButton();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableFallbackOverviewInWindow() {
        return FEATURE_FLAGS.enableFallbackOverviewInWindow();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableFirstScreenBroadcastArchivingExtras() {
        return FEATURE_FLAGS.enableFirstScreenBroadcastArchivingExtras();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableFocusOutline() {
        return FEATURE_FLAGS.enableFocusOutline();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableGeneratedPreviews() {
        return FEATURE_FLAGS.enableGeneratedPreviews();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableGridMigrationFix() {
        return FEATURE_FLAGS.enableGridMigrationFix();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableGridOnlyOverview() {
        return FEATURE_FLAGS.enableGridOnlyOverview();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableHandleDelayedGestureCallbacks() {
        return FEATURE_FLAGS.enableHandleDelayedGestureCallbacks();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableHomeTransitionListener() {
        return FEATURE_FLAGS.enableHomeTransitionListener();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableLauncherBrMetricsFixed() {
        return FEATURE_FLAGS.enableLauncherBrMetricsFixed();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableNarrowGridRestore() {
        return FEATURE_FLAGS.enableNarrowGridRestore();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableOverviewIconMenu() {
        return FEATURE_FLAGS.enableOverviewIconMenu();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enablePredictiveBackGesture() {
        return FEATURE_FLAGS.enablePredictiveBackGesture();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enablePrivateSpace() {
        return FEATURE_FLAGS.enablePrivateSpace();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enablePrivateSpaceInstallShortcut() {
        return FEATURE_FLAGS.enablePrivateSpaceInstallShortcut();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableRebootUnlockAnimation() {
        return FEATURE_FLAGS.enableRebootUnlockAnimation();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableRecentsInTaskbar() {
        return FEATURE_FLAGS.enableRecentsInTaskbar();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableRefactorTaskThumbnail() {
        return FEATURE_FLAGS.enableRefactorTaskThumbnail();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableResponsiveWorkspace() {
        return FEATURE_FLAGS.enableResponsiveWorkspace();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableScalingRevealHomeAnimation() {
        return FEATURE_FLAGS.enableScalingRevealHomeAnimation();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableShortcutDontSuggestApp() {
        return FEATURE_FLAGS.enableShortcutDontSuggestApp();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableSmartspaceAsAWidget() {
        return FEATURE_FLAGS.enableSmartspaceAsAWidget();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableSmartspaceRemovalToggle() {
        return FEATURE_FLAGS.enableSmartspaceRemovalToggle();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableSupportForArchiving() {
        return FEATURE_FLAGS.enableSupportForArchiving();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableTabletTwoPanePickerV2() {
        return FEATURE_FLAGS.enableTabletTwoPanePickerV2();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableTaskbarCustomization() {
        return FEATURE_FLAGS.enableTaskbarCustomization();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableTaskbarNoRecreate() {
        return FEATURE_FLAGS.enableTaskbarNoRecreate();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableTaskbarPinning() {
        return FEATURE_FLAGS.enableTaskbarPinning();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableTwoPaneLauncherSettings() {
        return FEATURE_FLAGS.enableTwoPaneLauncherSettings();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableTwolineAllapps() {
        return FEATURE_FLAGS.enableTwolineAllapps();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableTwolineToggle() {
        return FEATURE_FLAGS.enableTwolineToggle();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableUnfoldStateAnimation() {
        return FEATURE_FLAGS.enableUnfoldStateAnimation();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableUnfoldedTwoPanePicker() {
        return FEATURE_FLAGS.enableUnfoldedTwoPanePicker();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableWidgetTapToAdd() {
        return FEATURE_FLAGS.enableWidgetTapToAdd();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enableWorkspaceInflation() {
        return FEATURE_FLAGS.enableWorkspaceInflation();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean enabledFoldersInAllApps() {
        return FEATURE_FLAGS.enabledFoldersInAllApps();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean floatingSearchBar() {
        return FEATURE_FLAGS.floatingSearchBar();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean forceMonochromeAppIcons() {
        return FEATURE_FLAGS.forceMonochromeAppIcons();
    }

    //@AssumeFalseForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean privateSpaceAddFloatingMaskView() {
        return FEATURE_FLAGS.privateSpaceAddFloatingMaskView();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean privateSpaceAnimation() {
        return FEATURE_FLAGS.privateSpaceAnimation();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean privateSpaceAppInstallerButton() {
        return FEATURE_FLAGS.privateSpaceAppInstallerButton();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean privateSpaceRestrictAccessibilityDrag() {
        return FEATURE_FLAGS.privateSpaceRestrictAccessibilityDrag();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean privateSpaceRestrictItemDrag() {
        return FEATURE_FLAGS.privateSpaceRestrictItemDrag();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean privateSpaceSysAppsSeparation() {
        return FEATURE_FLAGS.privateSpaceSysAppsSeparation();
    }

    //@AssumeTrueForR8
    //@AconfigFlagAccessor
    //@UnsupportedAppUsage
    public static boolean useActivityOverlay() {
        return FEATURE_FLAGS.useActivityOverlay();
    }

    public static boolean enableDesktopWindowingMode() {
        return false;
    }

}