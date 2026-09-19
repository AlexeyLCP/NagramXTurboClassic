package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import xyz.nextalone.nagram.NaConfig;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        LauncherIcon firstEnabled = null;
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (icon == LauncherIcon.TURBO && pm.getComponentEnabledSetting(icon.getComponentName(ctx)) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                continue;
            }
            boolean keyEnabled = pm.getComponentEnabledSetting(icon.getComponentName(ctx)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            boolean modernEnabled = icon.modernKey != null && pm.getComponentEnabledSetting(component(ctx, icon.modernKey)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            if (keyEnabled && modernEnabled) {
                // interrupted switch left both aliases of one icon on: keep the modern one
                applyComponentState(pm, ctx, icon.getComponentName(ctx), PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                keyEnabled = false;
            }
            if (keyEnabled || modernEnabled) {
                if (firstEnabled == null) {
                    firstEnabled = icon;
                } else {
                    applyComponentState(pm, ctx, icon.getComponentName(ctx), PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                    if (icon.modernKey != null) {
                        applyComponentState(pm, ctx, component(ctx, icon.modernKey), PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                    }
                }
            }
        }
        if (firstEnabled == null) {
            if (!hasEnabledAlias(pm, ctx, LauncherIcon.TURBO)) {
                setIcon(LauncherIcon.TURBO);
            }
            return;
        }
        if (firstEnabled != LauncherIcon.TURBO) {
            applyComponentState(pm, ctx, LauncherIcon.TURBO.getComponentName(ctx), PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        }
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        if (hasEnabledAlias(pm, ctx, icon)) {
            return true;
        }
        return pm.getComponentEnabledSetting(icon.getComponentName(ctx)) == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.TURBO;
    }

    private static boolean hasEnabledAlias(PackageManager pm, Context ctx, LauncherIcon icon) {
        if (pm.getComponentEnabledSetting(icon.getComponentName(ctx)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return true;
        }
        return icon.modernKey != null && pm.getComponentEnabledSetting(component(ctx, icon.modernKey)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private static final long ALIAS_DISABLE_DELAY_MS = 500;
    private static Runnable deferredAliasDisableRunnable;

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        String target = icon.modernKey != null && NaConfig.INSTANCE.getModernClassicIcons().Bool() ? icon.modernKey : icon.key;
        if (deferredAliasDisableRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(deferredAliasDisableRunnable);
        }
        // OEM launchers refresh the drawer on the first PACKAGE_CHANGED and
        // debounce the rest: enable the new alias alone, disable the previous
        // one slightly later so the drawer snapshot is never mid-switch
        applyComponentState(pm, ctx, component(ctx, target), PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        deferredAliasDisableRunnable = () -> {
            for (LauncherIcon i : LauncherIcon.values()) {
                if (!i.key.equals(target)) {
                    applyComponentState(pm, ctx, component(ctx, i.key), PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                }
                if (i.modernKey != null && !i.modernKey.equals(target)) {
                    applyComponentState(pm, ctx, component(ctx, i.modernKey), PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                }
            }
            deferredAliasDisableRunnable = null;
        };
        if (ApplicationLoader.applicationHandler == null) {
            deferredAliasDisableRunnable.run();
        } else {
            AndroidUtilities.runOnUIThread(deferredAliasDisableRunnable, ALIAS_DISABLE_DELAY_MS);
        }
    }

    private static LauncherIcon pendingIcon;

    public static void setPendingIcon(LauncherIcon icon) {
        pendingIcon = icon;
    }

    public static LauncherIcon getPendingIcon() {
        return pendingIcon;
    }

    public static boolean hasPendingIcon() {
        return pendingIcon != null;
    }

    public static void applyPendingIcon() {
        if (pendingIcon != null) {
            setIcon(pendingIcon);
            pendingIcon = null;
            // deferred alias disable keeps the old icon enabled briefly: rebuild after it settles
            AndroidUtilities.runOnUIThread(() -> NotificationsController.rebuildAllAccounts(), ALIAS_DISABLE_DELAY_MS + 100);
        }
    }

    public static LauncherIcon getActiveIcon() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return icon;
            }
        }
        return LauncherIcon.TURBO;
    }

    public static int resolveNotificationIconResId(int configValue) {
        if (configValue == 1 || NaConfig.INSTANCE.getNotificationIconAsAppIcon().Bool()) {
            return getActiveIcon().notification;
        }
        switch (configValue) {
            case 0:
                return R.drawable.notification;
            case 2:
                return R.drawable.neko_notification;
        }
        return R.drawable.ic_notification_turbo;
    }

    private static ComponentName component(Context ctx, String key) {
        return new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
    }

    private static void applyComponentState(PackageManager pm, Context ctx, ComponentName cn, int state) {
        if (pm.getComponentEnabledSetting(cn) != state) {
            pm.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconTelegramOriginal, R.drawable.notification, "TelegramIconModern", R.drawable.ic_telegram_modern_background, R.drawable.ic_telegram_modern_foreground),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage, R.drawable.ic_notification_turbo, "VintageIconModern", R.drawable.ic_vintage_modern_background, R.drawable.ic_vintage_modern_foreground),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua, R.drawable.ic_notification_turbo, "AquaIconModern", R.drawable.ic_aqua_modern_background, R.drawable.ic_aqua_modern_foreground),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium, R.drawable.ic_notification_turbo, "PremiumIconModern", R.drawable.ic_premium_modern_background, R.drawable.ic_premium_modern_foreground),
        CLASSIC("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconClassic, R.drawable.ic_notification_turbo, "TurboIconModern", R.drawable.ic_classic_modern_background, R.drawable.ic_classic_modern_foreground),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox, R.drawable.ic_notification_turbo, "NoxIconModern", R.drawable.ic_nox_modern_background, R.drawable.ic_nox_modern_foreground),
        TURBO("TurboDefaultIcon", R.drawable.ic_turbo_background, R.drawable.ic_turbo_foreground, R.string.AppIconTurbo, R.drawable.ic_notification_turbo, null, 0, 0),
        SKY("SkyIcon", R.drawable.ic_sky_background, R.drawable.ic_sky_foreground, R.string.AppIconSky, R.drawable.ic_notification_sky, null, 0, 0),
        SUNSET("SunsetIcon", R.drawable.ic_sunset_background, R.drawable.ic_sunset_foreground, R.string.AppIconSunset, R.drawable.ic_notification_sunset, null, 0, 0),
        BLUE_NIGHT("BlueNightIcon", R.drawable.ic_blue_night_background, R.drawable.ic_blue_night_foreground, R.string.AppIconBlueNight, R.drawable.ic_notification_blue_night, null, 0, 0),
        HALLOWEEN("HalloweenIcon", R.drawable.ic_halloween_background, R.drawable.ic_halloween_foreground, R.string.AppIconHalloween, R.drawable.ic_notification_halloween, null, 0, 0),
        PAPER_BOX("PaperBoxIcon", R.drawable.ic_paper_box_background, R.drawable.ic_paper_box_foreground, R.string.AppIconPaperBox, R.drawable.ic_notification_paper_box, null, 0, 0),
        PAPER_FIRE("PaperFireIcon", R.drawable.ic_paper_fire_background, R.drawable.ic_paper_fire_foreground, R.string.AppIconPaperFire, R.drawable.ic_notification_paper_fire, null, 0, 0),
        CARBON("CarbonIcon", R.drawable.ic_carbon_background, R.drawable.ic_carbon_foreground, R.string.AppIconCarbon, R.drawable.ic_notification_carbon, null, 0, 0),
        GOLD("GoldIcon", R.drawable.ic_gold_background, R.drawable.ic_gold_foreground, R.string.AppIconGold, R.drawable.ic_notification_gold, null, 0, 0, true),
        MATRIX("MatrixIcon", R.drawable.ic_matrix_background, R.drawable.ic_matrix_foreground, R.string.AppIconMatrix, R.drawable.ic_notification_matrix, null, 0, 0, true),
        NEON("NeonIcon", R.drawable.ic_neon_background, R.drawable.ic_neon_foreground, R.string.AppIconNeon, R.drawable.ic_notification_neon, null, 0, 0, true),
        SPACE("SpaceIcon", R.drawable.ic_space_background, R.drawable.ic_space_foreground, R.string.AppIconSpace, R.drawable.ic_notification_space, null, 0, 0, true),
        HEXAGON("HexagonIcon", R.drawable.ic_hexagon_background, R.drawable.ic_hexagon_foreground, R.string.AppIconHexagon, R.drawable.ic_notification_hexagon, null, 0, 0, true),
        PIXEL("PixelIcon", R.drawable.ic_pixel_background, R.drawable.ic_pixel_foreground, R.string.AppIconPixel, R.drawable.ic_notification_pixel, null, 0, 0, true),
        GLITCH("GlitchIcon", R.drawable.ic_glitch_background, R.drawable.ic_glitch_foreground, R.string.AppIconGlitch, R.drawable.ic_notification_glitch, null, 0, 0, true);

        public final String key;
        public final String modernKey;
        public final int background;
        public final int foreground;
        public final int modernBackground;
        public final int modernForeground;
        public final int title;
        public final boolean premium;
        public final boolean isHidden;
        public final int notification;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title, int notification, String modernKey, int modernBackground, int modernForeground) {
            this(key, background, foreground, title, false, notification, modernKey, modernBackground, modernForeground, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, int notification, String modernKey, int modernBackground, int modernForeground, boolean isHidden) {
            this(key, background, foreground, title, false, notification, modernKey, modernBackground, modernForeground, isHidden);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium, int notification, String modernKey, int modernBackground, int modernForeground, boolean isHidden) {
            this.key = key;
            this.modernKey = modernKey;
            this.background = background;
            this.foreground = foreground;
            this.modernBackground = modernBackground;
            this.modernForeground = modernForeground;
            this.title = title;
            this.premium = premium;
            this.isHidden = isHidden;
            this.notification = notification;
        }
    }
}
