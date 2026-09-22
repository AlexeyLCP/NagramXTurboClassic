package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.DrawerLayoutContainer;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.lang.ref.WeakReference;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.ui.BookmarkManagerActivity;
import xyz.nextalone.nagram.NaConfig;

public final class ClassicSideMenu {

    private static WeakReference<LinearLayout> listRef;

    private ClassicSideMenu() {
    }

    public static void attach(LaunchActivity activity) {
        if (activity == null || activity.drawerLayoutContainer == null) {
            return;
        }
        DrawerLayoutContainer container = activity.drawerLayoutContainer;
        boolean classic = NaConfig.INSTANCE.getHideBottomNavigationBar().Bool();
        if (!classic) {
            container.setAllowOpenDrawer(false, true);
            return;
        }
        if (container.getDrawerLayout() == null) {
            FrameLayout drawer = new FrameLayout(activity);
            drawer.setTag("drawer");
            drawer.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
            ScrollView scroll = new ScrollView(activity);
            scroll.setFillViewport(true);
            LinearLayout list = new LinearLayout(activity);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(0, AndroidUtilities.statusBarHeight, 0, dp(8));
            scroll.addView(list, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            drawer.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            container.setDrawerLayout(drawer, list);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) drawer.getLayoutParams();
            int screen = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
            lp.width = Math.min(dp(320), Math.max(dp(240), screen - dp(56)));
            lp.height = LayoutHelper.MATCH_PARENT;
            drawer.setLayoutParams(lp);
            listRef = new WeakReference<>(list);
            container.setOnDrawerOpening(() -> fill(list, activity));
        }
        container.setAllowOpenDrawer(true, false);
        LinearLayout list = listRef != null ? listRef.get() : null;
        if (list != null) {
            fill(list, activity);
        }
    }

    private static void fill(LinearLayout list, LaunchActivity activity) {
        list.removeAllViews();
        list.setPadding(0, AndroidUtilities.statusBarHeight, 0, dp(8));
        int account = UserConfig.selectedAccount;
        TLRPC.User me = UserConfig.getInstance(account).getCurrentUser();
        list.addView(header(activity, account, me), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (a == account || !UserConfig.getInstance(a).isClientActivated()) {
                continue;
            }
            final int other = a;
            TLRPC.User user = UserConfig.getInstance(a).getCurrentUser();
            list.addView(row(activity, 0, UserObject.getUserName(user), () -> {
                close(activity);
                activity.switchToAccount(other, true);
            }));
        }
        gap(list);
        list.addView(row(activity, R.drawable.outline_groups_24, getString(R.string.NewGroup), () -> {
            close(activity);
            activity.presentFragment(new GroupCreateActivity(new Bundle()));
        }));
        list.addView(row(activity, R.drawable.outline_channel_24, getString(R.string.NewChannel), () -> openChannel(activity)));
        list.addView(row(activity, R.drawable.msg_contacts, getString(R.string.Contacts), () -> {
            Bundle args = new Bundle();
            args.putBoolean("needPhonebook", true);
            close(activity);
            activity.presentFragment(new ContactsActivity(args));
        }));
        list.addView(row(activity, R.drawable.msg_calls, getString(R.string.Calls), () -> {
            close(activity);
            activity.presentFragment(new CallLogActivity(new Bundle()));
        }));
        list.addView(row(activity, R.drawable.outline_saved_24, getString(R.string.SavedMessages), () -> {
            Bundle args = new Bundle();
            args.putLong("user_id", UserConfig.getInstance(account).getClientUserId());
            close(activity);
            activity.presentFragment(new ChatActivity(args));
        }));
        if (NaConfig.INSTANCE.getShowAddToBookmark().Bool()) {
            list.addView(row(activity, R.drawable.msg_fave, getString(R.string.BookmarksManager), () -> {
                close(activity);
                activity.presentFragment(new BookmarkManagerActivity());
            }));
        }
        if (!BuildVars.TURBO_BASE && NekoConfig.showGhostInDrawer.Bool()) {
            boolean on = NekoConfig.isGhostModeActive();
            list.addView(row(activity, R.drawable.ayu_ghost, getString(on ? R.string.DisableGhostMode : R.string.EnableGhostMode), () -> {
                NekoConfig.toggleGhostMode();
                BulletinFactory.global().createSimpleBulletin(R.raw.contact_check, getString(NekoConfig.isGhostModeActive() ? R.string.GhostModeEnabled : R.string.GhostModeDisabled)).show();
                fill(list, activity);
            }));
        }
        gap(list);
        list.addView(row(activity, R.drawable.msg_list, getString(R.string.HideBottomNavigationBar), () -> toggle(activity)));
        boolean dark = Theme.isCurrentThemeDark();
        list.addView(row(activity, dark ? R.drawable.menu_day_mode_24 : R.drawable.menu_night_mode_24, getString(dark ? R.string.SwitchThemeToDay : R.string.SwitchThemeToNight), () -> {
            close(activity);
            activity.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC));
        }));
        list.addView(row(activity, R.drawable.msg_settings_old, getString(R.string.Settings), () -> {
            close(activity);
            activity.presentFragment(new SettingsActivity());
        }));
    }

    public static void toggle(LaunchActivity activity) {
        if (activity == null) {
            return;
        }
        close(activity);
        NaConfig.INSTANCE.getHideBottomNavigationBar().toggleConfigBool();
        attach(activity);
        if (activity.drawerLayoutContainer != null) {
            INavigationLayout layout = activity.drawerLayoutContainer.getParentActionBarLayout();
            if (layout != null) {
                layout.rebuildFragments(0);
            }
        }
    }

    private static void openChannel(LaunchActivity activity) {
        SharedPreferences prefs = MessagesController.getGlobalMainSettings();
        close(activity);
        if (!BuildVars.DEBUG_VERSION && prefs.getBoolean("channel_intro", false)) {
            Bundle args = new Bundle();
            args.putInt("step", 0);
            activity.presentFragment(new ChannelCreateActivity(args));
        } else {
            activity.presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANNEL_CREATE));
            prefs.edit().putBoolean("channel_intro", true).apply();
        }
    }

    private static void close(LaunchActivity activity) {
        if (activity.drawerLayoutContainer != null) {
            activity.drawerLayoutContainer.closeDrawer(false);
        }
    }

    private static View header(LaunchActivity activity, int account, TLRPC.User user) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        BackupImageView avatar = new BackupImageView(activity);
        avatar.setRoundRadius(dp(18));
        AvatarDrawable drawable = new AvatarDrawable();
        drawable.setInfo(account, user);
        Drawable thumb = user != null && user.photo != null && user.photo.strippedBitmap != null ? user.photo.strippedBitmap : drawable;
        avatar.setImage(ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL), "50_50", thumb, user);
        row.addView(avatar, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 16, 0, 12, 0));
        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView name = label(activity, user == null ? "" : UserObject.getUserName(user), 16, true);
        TextView phone = label(activity, user == null || TextUtils.isEmpty(user.phone) ? "" : "+" + user.phone, 13, false);
        phone.setTextColor(Theme.getColor(Theme.key_chats_menuItemIcon));
        text.addView(name);
        text.addView(phone);
        row.addView(text, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        row.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("user_id", UserConfig.getInstance(account).getClientUserId());
            args.putBoolean("my_profile", true);
            close(activity);
            activity.presentFragment(new ProfileActivity(args));
        });
        return row;
    }

    private static View row(LaunchActivity activity, int icon, String title, Runnable click) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        ImageView image = new ImageView(activity);
        if (icon != 0) {
            image.setImageResource(icon);
            image.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.SRC_IN));
        }
        row.addView(image, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 19, 12, 16, 12));
        row.addView(label(activity, title, 15, true), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 16, 0));
        row.setOnClickListener(v -> click.run());
        return row;
    }

    private static TextView label(LaunchActivity activity, String text, int size, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        view.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
        if (bold) {
            view.setTypeface(AndroidUtilities.bold());
        }
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    private static void gap(LinearLayout list) {
        View gap = new View(list.getContext());
        gap.setBackgroundColor(Theme.getColor(Theme.key_divider));
        list.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 8, 0, 8));
    }
}
