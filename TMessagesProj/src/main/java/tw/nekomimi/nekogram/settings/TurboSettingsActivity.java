package tw.nekomimi.nekogram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.AyuData;
import com.radolyn.ayugram.messages.AyuMessagesController;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckBoxCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceWrapped;
import org.telegram.ui.Components.chat.WallpaperBitmapProvider;
import org.telegram.ui.Components.ActionButtonStyle;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ActionBar.BottomSheet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellCustom;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.config.cell.ConfigCellSelectBox;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheckIcon;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextInput;
import tw.nekomimi.nekogram.utils.ShareUtil;
import tw.nekomimi.nekogram.helpers.TypefaceHelper;
import xyz.nextalone.nagram.NaConfig;
import xyz.nextalone.nagram.helper.ProtectedForward;

@SuppressLint("RtlHardcoded")
@SuppressWarnings("unused")
public class TurboSettingsActivity extends BaseNekoXSettingsActivity {

    @Override
    protected RecyclerListView.SelectionAdapter getListAdapter() {
        return listAdapter;
    }

    @Override
    protected CellGroup getCellGroup() {
        return cellGroup;
    }

    @Override
    protected String getSettingsPrefix() {
        return "turbo";
    }

    private final CellGroup cellGroup = new CellGroup(this);

    // Input Bar
    private final AbstractConfigCell headerInputBar = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.InputBar)));
    private final AbstractConfigCell inputBarPreviewRow = cellGroup.appendCell(new ConfigCellCustom("InputBarPreview", ConfigCellCustom.CUSTOM_ITEM_InputBarPreview, false));
    private final AbstractConfigCell iosButtonPlacementRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getIosButtonPlacement()));
    private final AbstractConfigCell iosInputAppearanceRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getIosInputAppearance()));
    private final AbstractConfigCell compactInputSizeRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getCompactInputSize()));
    private final AbstractConfigCell actionButtonStyleRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getActionButtonStyle(), new String[]{
            getString(R.string.ActionButtonStyleAccent),
            getString(R.string.ActionButtonStyleNeutral),
            getString(R.string.ActionButtonStyleWhite)
    }, new int[]{
            ActionButtonStyle.ACCENT,
            ActionButtonStyle.NEUTRAL,
            ActionButtonStyle.WHITE
    }, null));
    private final AbstractConfigCell dividerInputBar = cellGroup.appendCell(new ConfigCellDivider());

    // Media
    private final AbstractConfigCell headerMedia = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.MediaSettings)));
    private final AbstractConfigCell swipeAllMediaRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getSwipeAllMedia(), getString(R.string.SwipeAllMediaAbout)));
    private final AbstractConfigCell seamlessVideoHandoffRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getSeamlessVideoHandoff(), getString(R.string.SeamlessVideoHandoffAbout)));
    private final AbstractConfigCell mediaAutoRotateModeRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getMediaAutoRotateMode(), new String[]{
            getString(R.string.MediaAutoRotateOff),
            getString(R.string.MediaAutoRotateFill),
            getString(R.string.MediaAutoRotateGyro),
    }, null, new int[]{
            R.raw.media_rotate_off,
            R.raw.media_rotate_fill,
            R.raw.media_rotate_gyro,
    }, null));
    private final AbstractConfigCell showMediaRotateButtonRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getShowMediaRotateButton()));
    private final AbstractConfigCell scrollToCurrentPhotoRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getScrollToCurrentPhoto(), getString(R.string.ScrollToCurrentPhotoAbout)));
    private final AbstractConfigCell photoViewerHdrRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.photoViewerHdr, getString(R.string.PhotoViewerHdrAbout)));
    private final AbstractConfigCell dividerMedia = cellGroup.appendCell(new ConfigCellDivider());

    // Forwarding
    private final AbstractConfigCell headerForwarding = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.ForwardingSettings)));
    private final AbstractConfigCell forwardProtectedModeRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getForwardProtectedMode(), new String[]{
            getString(R.string.ForwardProtectedModeAsk),
            getString(R.string.ForwardProtectedModeAlways),
            getString(R.string.ForwardProtectedModeNever)
    }, new int[]{
            ProtectedForward.FORWARD_PROTECTED_ASK,
            ProtectedForward.FORWARD_PROTECTED_ALWAYS,
            ProtectedForward.FORWARD_PROTECTED_NEVER
    }, null));
    private final AbstractConfigCell dividerForwarding = cellGroup.appendCell(new ConfigCellDivider());

    // Fonts
    private final AbstractConfigCell headerFonts = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.FontsSettings)));
    private final AbstractConfigCell typefaceRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.typeface));
    private final AbstractConfigCell fontRegularRow = cellGroup.appendCell(new ConfigCellCustom("FontRegular", ConfigCellCustom.CUSTOM_ITEM_FontRegular, true));
    private final AbstractConfigCell fontBoldRow = cellGroup.appendCell(new ConfigCellCustom("FontBold", ConfigCellCustom.CUSTOM_ITEM_FontBold, true));
    private final AbstractConfigCell fontItalicRow = cellGroup.appendCell(new ConfigCellCustom("FontItalic", ConfigCellCustom.CUSTOM_ITEM_FontItalic, true));
    private final AbstractConfigCell fontMonoRow = cellGroup.appendCell(new ConfigCellCustom("FontMono", ConfigCellCustom.CUSTOM_ITEM_FontMono, true));
    private final AbstractConfigCell fontResetRow = cellGroup.appendCell(new ConfigCellCustom("FontReset", ConfigCellCustom.CUSTOM_ITEM_FontReset, true));
    private final AbstractConfigCell dividerFonts = cellGroup.appendCell(new ConfigCellDivider());

    // Deleted Messages
    private final AbstractConfigCell headerSavedDeletedMessages = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.DeletedMessages)));
    private final AbstractConfigCell enableSaveDeletedMessagesRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getEnableSaveDeletedMessages(), getString(R.string.SaveDeletedMessagesHint)));
    private final AbstractConfigCell messageSavingSaveMediaRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getMessageSavingSaveMedia(), getString(R.string.MessageSavingSaveMediaHint)));
    private final AbstractConfigCell saveDeletedMessageForBotsUserRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getSaveDeletedMessageForBotUser()));
    private final AbstractConfigCell saveDeletedMessageInBotChatRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getSaveDeletedMessageForBot()));
    private final AbstractConfigCell translucentDeletedMessagesRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getTranslucentDeletedMessages()));
    private final AbstractConfigCell useDeletedIconRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getUseDeletedIcon()));
    private final AbstractConfigCell customDeletedMarkRow = cellGroup.appendCell(new ConfigCellTextInput(null, NaConfig.INSTANCE.getCustomDeletedMark(), "", null));
    private final AbstractConfigCell enableSaveEditsHistoryRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getEnableSaveEditsHistory()));
    private final AbstractConfigCell clearMessageDatabaseRow = cellGroup.appendCell(new ConfigCellTextCheckIcon(null, "ClearMessageDatabase", null, AyuData.totalSize > 0 ? AndroidUtilities.formatFileSize(AyuData.totalSize) : "...", R.drawable.msg_clear, false, () -> new AlertDialog.Builder(getContext(), getResourceProvider())
            .setTitle(getString(R.string.ClearMessageDatabase))
            .setMessage(getString(R.string.AreYouSure))
            .setPositiveButton(getString(R.string.Clear), (dialog, which) -> {
                AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
                progressDialog.setCanCancel(false);
                progressDialog.show();
                Utilities.globalQueue.postRunnable(() -> {
                    AyuMessagesController.getInstance().clean();
                    AndroidUtilities.runOnUIThread(() -> {
                        progressDialog.dismiss();
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.done, getString(R.string.ClearMessageDatabaseNotification)).show();
                    });
                    AyuData.loadSizes(this::refreshAyuDataSize);
                });
            })
            .setNegativeButton(getString(R.string.Cancel), (d, w) -> d.dismiss())
            .makeRed(AlertDialog.BUTTON_POSITIVE)
            .show()));
    private final AbstractConfigCell dividerClear = cellGroup.appendCell(new ConfigCellDivider());

    private ListAdapter listAdapter;
    private InputBarPreviewCell inputBarPreviewCell;

    public TurboSettingsActivity() {
        if (!NaConfig.INSTANCE.getIosInputAppearance().Bool()) {
            cellGroup.rows.remove(compactInputSizeRow);
        }
        if (NaConfig.INSTANCE.getUseDeletedIcon().Bool()) {
            cellGroup.rows.remove(customDeletedMarkRow);
        }
        if (!NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().Bool()) {
            cellGroup.rows.remove(saveDeletedMessageInBotChatRow);
        }
        checkFontsRows();
        checkUseDeletedIconRows();
        checkSaveBotMsgRows();
        checkSaveDeletedRows();
        addRowsToMap(cellGroup);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        AyuData.loadSizes(this::refreshAyuDataSize);
        return true;
    }

    @Override
    protected BlurredRecyclerView createListView(Context context) {
        return new BlurredRecyclerView(context) {
            @Override
            public Integer getSelectorColor(int position) {
                if (position == cellGroup.rows.indexOf(clearMessageDatabaseRow)) {
                    return Theme.multAlpha(getThemedColor(Theme.key_text_RedRegular), .1f);
                }
                return getThemedColor(Theme.key_listSelector);
            }
        };
    }

    @SuppressLint("NewApi")
    @Override
    public View createView(Context context) {
        View superView = super.createView(context);

        listAdapter = new ListAdapter(context);

        listView.setAdapter(listAdapter);

        setupDefaultListeners();

        // Cells: Set OnSettingChanged Callbacks
        cellGroup.callBackSettingsChanged = (key, newValue) -> {
            if (key.equals(NaConfig.INSTANCE.getIosButtonPlacement().getKey())
                    || key.equals(NaConfig.INSTANCE.getIosInputAppearance().getKey())
                    || key.equals(NaConfig.INSTANCE.getCompactInputSize().getKey())
                    || key.equals(NaConfig.INSTANCE.getActionButtonStyle().getKey())) {
                if (inputBarPreviewCell != null) {
                    inputBarPreviewCell.updateInputBarState();
                }
            }
            if (key.equals(NaConfig.INSTANCE.getIosInputAppearance().getKey())) {
                boolean iosOn = NaConfig.INSTANCE.getIosInputAppearance().Bool();
                if (iosOn) {
                    if (!cellGroup.rows.contains(compactInputSizeRow)) {
                        cellGroup.rows.add(cellGroup.rows.indexOf(dividerInputBar), compactInputSizeRow);
                    }
                } else {
                    cellGroup.rows.remove(compactInputSizeRow);
                }
                listAdapter.notifyDataSetChanged();
            } else if (key.equals(NaConfig.INSTANCE.getEnableSaveDeletedMessages().getKey())) {
                checkSaveDeletedRows();
            } else if (key.equals(NaConfig.INSTANCE.getUseDeletedIcon().getKey())) {
                checkUseDeletedIconRows();
            } else if (key.equals(NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().getKey())) {
                checkSaveBotMsgRows();
            } else if (key.equals(NekoConfig.typeface.getKey())) {
                tooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
                checkFontsRows();
            } else if (key.startsWith("CustomFont")) {
                TypefaceHelper.clearAllFontCaches();
                AndroidUtilities.runOnUIThread(() -> {
                    if (LaunchActivity.instance != null) {
                        LaunchActivity.instance.recreate();
                    }
                }, 100);
            }
        };

        return superView;
    }

    @Override
    protected void handleCellClick(View view, int position, float x, float y) {
        if (position < 0 || position >= cellGroup.rows.size()) {
            return;
        }
        AbstractConfigCell a = cellGroup.rows.get(position);
        if (a instanceof ConfigCellTextCheck) {
            if (position == cellGroup.rows.indexOf(messageSavingSaveMediaRow) && (LocaleController.isRTL && x > AndroidUtilities.dp(76) || !LocaleController.isRTL && x < (view.getMeasuredWidth() - AndroidUtilities.dp(76)))) {
                showBottomSheet();
                return;
            }
            if (position == cellGroup.rows.indexOf(enableSaveDeletedMessagesRow) && (LocaleController.isRTL && x > AndroidUtilities.dp(76) || !LocaleController.isRTL && x < (view.getMeasuredWidth() - AndroidUtilities.dp(76)))) {
                showDeletedCategoriesSheet();
                return;
            }
        }
        super.handleCellClick(view, position, x, y);
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        AbstractConfigCell a = cellGroup.rows.get(position);
        if (a == clearMessageDatabaseRow) {
            ItemOptions options = makeLongClickOptions(view);
            options.add(R.drawable.msg_instant_link_solar, getString(R.string.ExportAyuDB), this::exportAyuDB);
            addDefaultLongClickOptions(options, "turbo", position);
            showLongClickOptions(view, options);
            return true;
        }
        return false;
    }

    @Override
    protected void onCustomCellClick(View view, int position, float x, float y) {
        if (position == cellGroup.rows.indexOf(fontRegularRow)) {
            presentFragment(new FontPickerActivity(TypefaceHelper.FONT_CATEGORY_REGULAR));
        } else if (position == cellGroup.rows.indexOf(fontBoldRow)) {
            presentFragment(new FontPickerActivity(TypefaceHelper.FONT_CATEGORY_BOLD));
        } else if (position == cellGroup.rows.indexOf(fontItalicRow)) {
            presentFragment(new FontPickerActivity(TypefaceHelper.FONT_CATEGORY_ITALIC));
        } else if (position == cellGroup.rows.indexOf(fontMonoRow)) {
            presentFragment(new FontPickerActivity(TypefaceHelper.FONT_CATEGORY_MONO));
        } else if (position == cellGroup.rows.indexOf(fontResetRow)) {
            if (getParentActivity() == null) return;
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString(R.string.FontReset));
            builder.setMessage(getString(R.string.FontResetConfirm));
            builder.setPositiveButton(getString(R.string.FontApplyButton), (dialog, which) -> {
                TypefaceHelper.resetAllFonts();
                TypefaceHelper.clearAllFontCaches();
                AndroidUtilities.runOnUIThread(() -> {
                    if (LaunchActivity.instance != null) {
                        LaunchActivity.instance.recreate();
                    }
                }, 100);
            });
            builder.setNegativeButton(getString(R.string.Cancel), null);
            showDialog(builder.create());
        }
    }

    @Override
    public int getBaseGuid() {
        return 14000;
    }

    @Override
    public int getDrawable() {
        return R.drawable.msg_rocket;
    }

    @Override
    public String getTitle() {
        return getString(R.string.NagramXTurbo);
    }

    private void checkFontsRows() {
        boolean hasCustom = TypefaceHelper.hasAnyCustomFont();
        if (hasCustom && cellGroup.rows.contains(typefaceRow)) {
            cellGroup.rows.remove(typefaceRow);
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (!hasCustom && !cellGroup.rows.contains(typefaceRow)) {
            int idx = cellGroup.rows.indexOf(fontRegularRow);
            if (idx > 0) {
                cellGroup.rows.add(idx, typefaceRow);
                if (listAdapter != null) listAdapter.notifyDataSetChanged();
            }
        }
    }

    public void refreshAyuDataSize() {
        if (listAdapter != null) {
            ((ConfigCellTextCheckIcon) clearMessageDatabaseRow).setValue(AyuData.totalSize > 0 ? AndroidUtilities.formatFileSize(AyuData.totalSize) : "...");
            listAdapter.notifyItemChanged(cellGroup.rows.indexOf(clearMessageDatabaseRow));
        }
    }

    private void exportAyuDB() {
        if (getParentActivity() == null) return;
        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.show();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                File dbFile = ApplicationLoader.applicationContext.getDatabasePath(AyuConstants.AYU_DATABASE);
                File exportFile = new File(AndroidUtilities.getCacheDir(), AyuConstants.AYU_DATABASE_EXPORT);
                AyuData.checkpointDatabase();
                if (!AndroidUtilities.copyFile(dbFile, exportFile)) {
                    if (!exportFile.delete()) exportFile.deleteOnExit();
                    throw new IOException("Failed to copy Ayu database");
                }
                AndroidUtilities.runOnUIThread(() -> {
                    Context parentActivity = getParentActivity();
                    progressDialog.dismiss();
                    if (parentActivity != null) {
                        ShareUtil.shareFile(parentActivity, exportFile);
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    if (getParentActivity() != null) {
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.error, getString(R.string.ErrorOccurred)).show();
                    }
                });
            }
        });
    }

    private void checkSaveDeletedRows() {
        final boolean isSaveEnabled = NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool();
        final List<AbstractConfigCell> allManagedRows = Arrays.asList(
                messageSavingSaveMediaRow,
                saveDeletedMessageForBotsUserRow,
                saveDeletedMessageInBotChatRow,
                translucentDeletedMessagesRow,
                useDeletedIconRow,
                customDeletedMarkRow
        );
        if (listAdapter == null) {
            if (!isSaveEnabled) {
                cellGroup.rows.removeAll(allManagedRows);
            }
            return;
        }
        final int anchorIndex = cellGroup.rows.indexOf(enableSaveDeletedMessagesRow);
        int firstManagedRowIndex = -1;
        int lastManagedRowIndex = -1;
        for (int i = anchorIndex + 1; i < cellGroup.rows.size(); i++) {
            if (allManagedRows.contains(cellGroup.rows.get(i))) {
                if (firstManagedRowIndex == -1) {
                    firstManagedRowIndex = i;
                }
                lastManagedRowIndex = i;
            }
        }
        if (firstManagedRowIndex != -1) {
            int count = lastManagedRowIndex - firstManagedRowIndex + 1;
            cellGroup.rows.subList(firstManagedRowIndex, lastManagedRowIndex + 1).clear();
            listAdapter.notifyItemRangeRemoved(firstManagedRowIndex, count);
        }
        if (isSaveEnabled) {
            final List<AbstractConfigCell> rowsToAdd = new ArrayList<>();
            rowsToAdd.add(messageSavingSaveMediaRow);
            rowsToAdd.add(saveDeletedMessageForBotsUserRow);
            if (NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().Bool()) {
                rowsToAdd.add(saveDeletedMessageInBotChatRow);
            }
            rowsToAdd.add(translucentDeletedMessagesRow);
            rowsToAdd.add(useDeletedIconRow);
            if (!NaConfig.INSTANCE.getUseDeletedIcon().Bool()) {
                rowsToAdd.add(customDeletedMarkRow);
            }
            cellGroup.rows.addAll(anchorIndex + 1, rowsToAdd);
            listAdapter.notifyItemRangeInserted(anchorIndex + 1, rowsToAdd.size());
        }
        addRowsToMap(cellGroup);
    }

    private void checkSaveBotMsgRows() {
        boolean enabled = NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().Bool();
        if (listAdapter == null) {
            if (!enabled) {
                cellGroup.rows.remove(saveDeletedMessageInBotChatRow);
            }
            return;
        }
        if (enabled) {
            final int index = cellGroup.rows.indexOf(saveDeletedMessageForBotsUserRow);
            if (!cellGroup.rows.contains(saveDeletedMessageInBotChatRow)) {
                cellGroup.rows.add(index + 1, saveDeletedMessageInBotChatRow);
                listAdapter.notifyItemInserted(index + 1);
            }
        } else {
            final int index = cellGroup.rows.indexOf(saveDeletedMessageInBotChatRow);
            if (index != -1) {
                cellGroup.rows.remove(saveDeletedMessageInBotChatRow);
                listAdapter.notifyItemRemoved(index);
            }
        }
        addRowsToMap(cellGroup);
    }

    private void checkUseDeletedIconRows() {
        boolean enabled = NaConfig.INSTANCE.getUseDeletedIcon().Bool();
        if (listAdapter == null) {
            if (enabled) {
                cellGroup.rows.remove(customDeletedMarkRow);
            }
            return;
        }
        if (!enabled) {
            final int index = cellGroup.rows.indexOf(useDeletedIconRow);
            if (!cellGroup.rows.contains(customDeletedMarkRow)) {
                cellGroup.rows.add(index + 1, customDeletedMarkRow);
                listAdapter.notifyItemInserted(index + 1);
            }
        } else {
            final int index = cellGroup.rows.indexOf(customDeletedMarkRow);
            if (index != -1) {
                cellGroup.rows.remove(customDeletedMarkRow);
                listAdapter.notifyItemRemoved(index);
            }
        }
        addRowsToMap(cellGroup);
    }

    private void showBottomSheet() {
        if (getParentActivity() == null) {
            return;
        }
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setApplyTopPadding(false);
        builder.setApplyBottomPadding(false);
        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setCustomView(linearLayout);

        HeaderCell headerCell = new HeaderCell(getParentActivity(), Theme.key_dialogTextBlue2, 21, 15, false);
        headerCell.setText(getString(R.string.MessageSavingSaveMedia).toUpperCase());
        linearLayout.addView(headerCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextCheckBoxCell[] cells = new TextCheckBoxCell[5];
        for (int a = 0; a < cells.length; a++) {
            TextCheckBoxCell checkBoxCell = cells[a] = new TextCheckBoxCell(getParentActivity(), true, false);
            boolean mediaFlag;
            boolean deletedEnabled;
            if (a == 0) {
                mediaFlag = NaConfig.INSTANCE.getSaveMediaInPrivateChats().Bool();
                deletedEnabled = NaConfig.INSTANCE.getSaveDeletedInPrivateChats().Bool();
                cells[a].setTextAndCheck(getString(R.string.MessageSavingSaveMediaInPrivateChats), mediaFlag && deletedEnabled, true);
            } else if (a == 1) {
                mediaFlag = NaConfig.INSTANCE.getSaveMediaInPublicChannels().Bool();
                deletedEnabled = NaConfig.INSTANCE.getSaveDeletedInPublicChannels().Bool();
                cells[a].setTextAndCheck(getString(R.string.MessageSavingSaveMediaInPublicChannels), mediaFlag && deletedEnabled, true);
            } else if (a == 2) {
                mediaFlag = NaConfig.INSTANCE.getSaveMediaInPrivateChannels().Bool();
                deletedEnabled = NaConfig.INSTANCE.getSaveDeletedInPrivateChannels().Bool();
                cells[a].setTextAndCheck(getString(R.string.MessageSavingSaveMediaInPrivateChannels), mediaFlag && deletedEnabled, true);
            } else if (a == 3) {
                mediaFlag = NaConfig.INSTANCE.getSaveMediaInPublicGroups().Bool();
                deletedEnabled = NaConfig.INSTANCE.getSaveDeletedInPublicGroups().Bool();
                cells[a].setTextAndCheck(getString(R.string.MessageSavingSaveMediaInPublicGroups), mediaFlag && deletedEnabled, true);
            } else { // a == 4
                mediaFlag = NaConfig.INSTANCE.getSaveMediaInPrivateGroups().Bool();
                deletedEnabled = NaConfig.INSTANCE.getSaveDeletedInPrivateGroups().Bool();
                cells[a].setTextAndCheck(getString(R.string.MessageSavingSaveMediaInPrivateGroups), mediaFlag && deletedEnabled, true);
            }
            // Media requires deleted messages: a category disabled for deleted messages cannot be enabled for media.
            if (!deletedEnabled) {
                cells[a].setEnabled(false);
                cells[a].setAlpha(0.5f);
            }
            cells[a].setBackground(Theme.getSelectorDrawable(false));
            cells[a].setOnClickListener(v -> {
                if (!v.isEnabled()) {
                    return;
                }
                checkBoxCell.setChecked(!checkBoxCell.isChecked());
            });
            linearLayout.addView(cells[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50));
        }

        FrameLayout buttonsLayout = new FrameLayout(getParentActivity());
        buttonsLayout.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        linearLayout.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52));

        TextView textView = new TextView(getParentActivity());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setText(getString(R.string.Cancel).toUpperCase());
        textView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        buttonsLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36, Gravity.TOP | Gravity.LEFT));
        textView.setOnClickListener(v14 -> builder.getDismissRunnable().run());

        textView = new TextView(getParentActivity());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setText(getString(R.string.Save).toUpperCase());
        textView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        buttonsLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36, Gravity.TOP | Gravity.RIGHT));
        textView.setOnClickListener(v1 -> {
            // Media requires deleted messages: only persist media flags for categories where deleted messages
            // are enabled. Disabled categories keep their stored value (intent) untouched, so
            // re-enabling deleted later restores media with its previous setting.
            if (NaConfig.INSTANCE.getSaveDeletedInPrivateChats().Bool()) {
                NaConfig.INSTANCE.getSaveMediaInPrivateChats().setConfigBool(cells[0].isChecked());
            }
            if (NaConfig.INSTANCE.getSaveDeletedInPublicChannels().Bool()) {
                NaConfig.INSTANCE.getSaveMediaInPublicChannels().setConfigBool(cells[1].isChecked());
            }
            if (NaConfig.INSTANCE.getSaveDeletedInPrivateChannels().Bool()) {
                NaConfig.INSTANCE.getSaveMediaInPrivateChannels().setConfigBool(cells[2].isChecked());
            }
            if (NaConfig.INSTANCE.getSaveDeletedInPublicGroups().Bool()) {
                NaConfig.INSTANCE.getSaveMediaInPublicGroups().setConfigBool(cells[3].isChecked());
            }
            if (NaConfig.INSTANCE.getSaveDeletedInPrivateGroups().Bool()) {
                NaConfig.INSTANCE.getSaveMediaInPrivateGroups().setConfigBool(cells[4].isChecked());
            }

            builder.getDismissRunnable().run();
        });
        showDialog(builder.create());
    }

    private void showDeletedCategoriesSheet() {
        if (getParentActivity() == null) {
            return;
        }
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setApplyTopPadding(false);
        builder.setApplyBottomPadding(false);
        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setCustomView(linearLayout);

        HeaderCell headerCell = new HeaderCell(getParentActivity(), Theme.key_dialogTextBlue2, 21, 15, false);
        headerCell.setText(getString(R.string.SaveDeletedMessagesSettings).toUpperCase());
        linearLayout.addView(headerCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextCheckBoxCell[] cells = new TextCheckBoxCell[5];
        for (int a = 0; a < cells.length; a++) {
            TextCheckBoxCell checkBoxCell = cells[a] = new TextCheckBoxCell(getParentActivity(), true, false);
            if (a == 0) {
                cells[a].setTextAndCheck(getString(R.string.SaveDeletedInPrivateChats), NaConfig.INSTANCE.getSaveDeletedInPrivateChats().Bool(), true);
            } else if (a == 1) {
                cells[a].setTextAndCheck(getString(R.string.SaveDeletedInPublicChannels), NaConfig.INSTANCE.getSaveDeletedInPublicChannels().Bool(), true);
            } else if (a == 2) {
                cells[a].setTextAndCheck(getString(R.string.SaveDeletedInPrivateChannels), NaConfig.INSTANCE.getSaveDeletedInPrivateChannels().Bool(), true);
            } else if (a == 3) {
                cells[a].setTextAndCheck(getString(R.string.SaveDeletedInPublicGroups), NaConfig.INSTANCE.getSaveDeletedInPublicGroups().Bool(), true);
            } else { // a == 4
                cells[a].setTextAndCheck(getString(R.string.SaveDeletedInPrivateGroups), NaConfig.INSTANCE.getSaveDeletedInPrivateGroups().Bool(), true);
            }
            cells[a].setBackground(Theme.getSelectorDrawable(false));
            cells[a].setOnClickListener(v -> checkBoxCell.setChecked(!checkBoxCell.isChecked()));
            linearLayout.addView(cells[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50));
        }

        FrameLayout buttonsLayout = new FrameLayout(getParentActivity());
        buttonsLayout.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        linearLayout.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52));

        TextView textView = new TextView(getParentActivity());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setText(getString(R.string.Cancel).toUpperCase());
        textView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        buttonsLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36, Gravity.TOP | Gravity.LEFT));
        textView.setOnClickListener(v14 -> builder.getDismissRunnable().run());

        textView = new TextView(getParentActivity());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setText(getString(R.string.Save).toUpperCase());
        textView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        buttonsLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36, Gravity.TOP | Gravity.RIGHT));
        textView.setOnClickListener(v1 -> {
            NaConfig.INSTANCE.getSaveDeletedInPrivateChats().setConfigBool(cells[0].isChecked());
            NaConfig.INSTANCE.getSaveDeletedInPublicChannels().setConfigBool(cells[1].isChecked());
            NaConfig.INSTANCE.getSaveDeletedInPrivateChannels().setConfigBool(cells[2].isChecked());
            NaConfig.INSTANCE.getSaveDeletedInPublicGroups().setConfigBool(cells[3].isChecked());
            NaConfig.INSTANCE.getSaveDeletedInPrivateGroups().setConfigBool(cells[4].isChecked());

            builder.getDismissRunnable().run();
        });
        showDialog(builder.create());
    }

    private class InputBarPreviewCell extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

        private static final int BUBBLE_RADIUS_DP = 22;
        private static final int BAR_HEIGHT_DP = 44;
        private static final int GAP_NORMAL_DP = 8;
        private static final int GAP_COMPACT_DP = 2;
        private static final int CELL_VERTICAL_PADDING_DP = 12;
        private static final int CELL_BOTTOM_PADDING_DP = 20;

        private final Theme.ResourcesProvider resourcesProvider;
        private final WallpaperBitmapProvider wallpaperBitmapProvider = new WallpaperBitmapProvider();

        private BlurredBackgroundDrawableViewFactory glassFactory;
        private BlurredBackgroundColorProviderThemed colorProvider;
        private BlurredBackgroundColorProviderThemed whiteColorProvider;
        private BlurredBackgroundColorProviderThemed accentColorProvider;
        private final Paint sendCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private BlurredBackgroundDrawable oneBlockDrawable;
        private BlurredBackgroundDrawable capsuleDrawable;
        private BlurredBackgroundDrawable leftBubbleDrawable;
        private BlurredBackgroundDrawable rightBubbleDrawable;

        private final ImageView attachIconView;
        private final ImageView emojiIconView;
        private final ImageView sendIconView;

        private boolean isPlacementEnabled;
        private boolean isAppearanceEnabled;
        private boolean isCompactEnabled;
        private int gapPx;

        private Drawable lastWallpaper;
        private boolean lastIsBlurEnabled;
        private boolean lastIsLiquidGlassEnabled;
        private Drawable lastDrawnWallpaper;

        public InputBarPreviewCell(Context context, Theme.ResourcesProvider provider) {
            super(context);
            resourcesProvider = provider;
            setWillNotDraw(false);
            setClipChildren(false);
            setPadding(0, AndroidUtilities.dp(CELL_VERTICAL_PADDING_DP), 0, AndroidUtilities.dp(CELL_BOTTOM_PADDING_DP));

            int iconColor = Theme.getColor(Theme.key_glass_defaultIcon, provider);
            int sendColor = Theme.getColor(Theme.key_chat_messagePanelSend, provider);

            attachIconView = createIconView(context, R.drawable.msg_input_attach2, iconColor);
            emojiIconView = createIconView(context, R.drawable.smiles_tab_smiles, iconColor);
            sendIconView = createIconView(context, R.drawable.send_plane_24, sendColor);

            buildGlassFactory();
            updateInputBarState();
        }

        private ImageView createIconView(Context context, int resId, int color) {
            ImageView view = new ImageView(context);
            view.setImageResource(resId);
            view.setScaleType(ImageView.ScaleType.CENTER);
            view.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            addView(view);
            return view;
        }

        private void buildGlassFactory() {
            Drawable wallpaper = Theme.getCachedWallpaperNonBlocking();
            boolean isBlurEnabled = SharedConfig.chatBlurEnabled() && LiteMode.isEnabled(LiteMode.FLAG_CHAT_BLUR);
            boolean isLiquidGlassEnabled = LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS);
            if (glassFactory != null && wallpaper == lastWallpaper && isBlurEnabled == lastIsBlurEnabled && isLiquidGlassEnabled == lastIsLiquidGlassEnabled) {
                return;
            }
            lastWallpaper = wallpaper;
            lastIsBlurEnabled = isBlurEnabled;
            lastIsLiquidGlassEnabled = isLiquidGlassEnabled;

            BlurredBackgroundSource source;
            if (isBlurEnabled && wallpaper != null) {
                source = wallpaperBitmapProvider.updateSourceFromBackgroundViewDrawable(wallpaper);
            } else {
                source = new BlurredBackgroundSourceColor();
            }
            BlurredBackgroundSourceWrapped wrappedSource = new BlurredBackgroundSourceWrapped();
            wrappedSource.setSource(source);
            glassFactory = new BlurredBackgroundDrawableViewFactory(wrappedSource);
            colorProvider = new BlurredBackgroundColorProviderThemed(resourcesProvider, Theme.key_chat_messagePanelBackground);
            final boolean previewBlurEnabled = isBlurEnabled;
            final boolean previewLiquidGlass = isLiquidGlassEnabled;
            whiteColorProvider = new BlurredBackgroundColorProviderThemed(resourcesProvider, Theme.key_windowBackgroundWhite) {
                @Override
                public int getBackgroundColor() {
                    if (!previewBlurEnabled) return 0xFFFFFFFF;
                    return previewLiquidGlass ? 0xD9FFFFFF : 0xC2FFFFFF;
                }
            };
            accentColorProvider = new BlurredBackgroundColorProviderThemed(resourcesProvider, Theme.key_chat_messagePanelSend);
            if (!isBlurEnabled) {
                colorProvider.setAlpha(1.0f);
            }
            glassFactory.setLiquidGlassEffectAllowed(isLiquidGlassEnabled);
            oneBlockDrawable = createBubble();
            capsuleDrawable = createBubble();
            leftBubbleDrawable = createBubble();
            rightBubbleDrawable = createBubble();
        }

        private BlurredBackgroundDrawable createBubble() {
            BlurredBackgroundDrawable drawable = glassFactory.create(this, colorProvider);
            drawable.setRadius(AndroidUtilities.dp(BUBBLE_RADIUS_DP));
            return drawable;
        }

        public void updateInputBarState() {
            isPlacementEnabled = NaConfig.INSTANCE.getIosButtonPlacement().Bool();
            isAppearanceEnabled = NaConfig.INSTANCE.getIosInputAppearance().Bool();
            isCompactEnabled = NaConfig.INSTANCE.getCompactInputSize().Bool() && isAppearanceEnabled;
            int gapDp = isCompactEnabled ? GAP_COMPACT_DP : GAP_NORMAL_DP;
            gapPx = AndroidUtilities.dp(gapDp);

            ImageView leftIcon = isPlacementEnabled ? attachIconView : emojiIconView;
            ImageView rightIcon = isPlacementEnabled ? emojiIconView : attachIconView;
            int edgeInsetDp = isAppearanceEnabled ? gapDp : CELL_VERTICAL_PADDING_DP;
            leftIcon.setLayoutParams(LayoutHelper.createFrame(BAR_HEIGHT_DP, BAR_HEIGHT_DP, Gravity.BOTTOM | Gravity.LEFT, edgeInsetDp, 0, 0, 0));
            rightIcon.setLayoutParams(LayoutHelper.createFrame(BAR_HEIGHT_DP, BAR_HEIGHT_DP, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, BAR_HEIGHT_DP + edgeInsetDp + gapDp, 0));
            sendIconView.setLayoutParams(LayoutHelper.createFrame(BAR_HEIGHT_DP, BAR_HEIGHT_DP, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, edgeInsetDp, 0));

            if (rightBubbleDrawable != null) {
                rightBubbleDrawable.setColorProvider(ActionButtonStyle.resolveBubbleColorProvider(whiteColorProvider, colorProvider, accentColorProvider));
            }
            sendIconView.setColorFilter(new PorterDuffColorFilter(
                    ActionButtonStyle.resolveIconColor(resourcesProvider),
                    PorterDuff.Mode.SRC_IN));

            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = AndroidUtilities.dp(BAR_HEIGHT_DP + CELL_VERTICAL_PADDING_DP + CELL_BOTTOM_PADDING_DP);
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            Drawable wallpaper = Theme.getCachedWallpaperNonBlocking();
            if (wallpaper != null) {
                if (wallpaper != lastDrawnWallpaper) {
                    lastDrawnWallpaper = wallpaper;
                    invalidate();
                }
                wallpaper.setBounds(0, 0, getWidth(), getHeight());
                wallpaper.draw(canvas);
            }
            int padding = AndroidUtilities.dp(CELL_VERTICAL_PADDING_DP);
            int fieldTop = attachIconView.getTop();
            int fieldBottom = attachIconView.getBottom();
            if (isAppearanceEnabled) {
                ImageView leftIcon = isPlacementEnabled ? attachIconView : emojiIconView;
                int pillLeft = leftIcon.getRight() + gapPx;
                int pillRight = sendIconView.getLeft() - gapPx;
                capsuleDrawable.setBounds(pillLeft, fieldTop, pillRight, fieldBottom);
                capsuleDrawable.draw(canvas);
                leftBubbleDrawable.setBounds(leftIcon.getLeft(), leftIcon.getTop(), leftIcon.getRight(), leftIcon.getBottom());
                leftBubbleDrawable.draw(canvas);
                rightBubbleDrawable.setBounds(sendIconView.getLeft(), sendIconView.getTop(), sendIconView.getRight(), sendIconView.getBottom());
                rightBubbleDrawable.draw(canvas);
            } else {
                oneBlockDrawable.setBounds(padding, fieldTop, getWidth() - padding, fieldBottom);
                oneBlockDrawable.draw(canvas);
                sendCirclePaint.setColor(ActionButtonStyle.resolveBackgroundColor(resourcesProvider));
                float sendCx = (sendIconView.getLeft() + sendIconView.getRight()) / 2f;
                float sendCy = (sendIconView.getTop() + sendIconView.getBottom()) / 2f;
                canvas.drawCircle(sendCx, sendCy, (sendIconView.getRight() - sendIconView.getLeft()) / 2f - AndroidUtilities.dp(3), sendCirclePaint);
            }
            super.dispatchDraw(canvas);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetNewWallpapper);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewWallpapper);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.didSetNewWallpapper) {
                buildGlassFactory();
                invalidate();
            }
        }
    }

    //impl ListAdapter
    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        protected void onBindDefaultViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            CellGroup cellGroup = getCellGroup();
            if (cellGroup == null) return;
            AbstractConfigCell a = cellGroup.rows.get(position);
            if (a instanceof ConfigCellTextCheckIcon) {
                if (holder.itemView instanceof TextCell textCell) {
                    if (position == cellGroup.rows.indexOf(clearMessageDatabaseRow)) {
                        textCell.setColors(Theme.key_text_RedRegular, Theme.key_text_RedRegular);
                    }
                }
            }
        }

        @Override
        protected View onCreateCustomViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = null;
            if (viewType == ConfigCellCustom.CUSTOM_ITEM_InputBarPreview) {
                view = inputBarPreviewCell = new InputBarPreviewCell(mContext, getResourceProvider());
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_FontRegular ||
                    viewType == ConfigCellCustom.CUSTOM_ITEM_FontBold ||
                    viewType == ConfigCellCustom.CUSTOM_ITEM_FontItalic ||
                    viewType == ConfigCellCustom.CUSTOM_ITEM_FontMono) {
                view = new TextSettingsCell(mContext);
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_FontReset) {
                view = new TextCell(mContext);
            }
            return view;
        }

        @Override
        protected void onBindCustomViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.itemView instanceof TextSettingsCell textCell) {
                String category = null;
                String title = null;
                if (position == cellGroup.rows.indexOf(fontRegularRow)) {
                    category = TypefaceHelper.FONT_CATEGORY_REGULAR;
                    title = getString(R.string.FontCategoryRegular);
                } else if (position == cellGroup.rows.indexOf(fontBoldRow)) {
                    category = TypefaceHelper.FONT_CATEGORY_BOLD;
                    title = getString(R.string.FontCategoryBold);
                } else if (position == cellGroup.rows.indexOf(fontItalicRow)) {
                    category = TypefaceHelper.FONT_CATEGORY_ITALIC;
                    title = getString(R.string.FontCategoryItalic);
                } else if (position == cellGroup.rows.indexOf(fontMonoRow)) {
                    category = TypefaceHelper.FONT_CATEGORY_MONO;
                    title = getString(R.string.FontCategoryMono);
                }
                if (category != null) {
                    String fontName = TypefaceHelper.getCustomFontName(category);
                    textCell.setTextAndValue(title, fontName.isEmpty() ? getString(R.string.FontDefault) : fontName, true);
                }
            } else if (holder.itemView instanceof TextCell textCell) {
                if (position == cellGroup.rows.indexOf(fontResetRow)) {
                    textCell.setText(getString(R.string.FontReset), true);
                    textCell.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
            }
        }
    }
}
