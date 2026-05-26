
package net.revilodev.boundless.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.SharedConstants;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.QuestPanelClient;
import net.revilodev.boundless.compat.LevelUpCompat;
import net.revilodev.boundless.quest.QuestData;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class QuestEditorScreen extends Screen {
    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");
    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget.png");
    private static final ResourceLocation BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button.png");
    private static final ResourceLocation BTN_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_highlighted.png");
    private static final ResourceLocation BTN_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_disabled.png");
    private static final ResourceLocation CREATE_BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/button.png");
    private static final int CREATE_BTN_TEX_W = 130;
    private static final int CREATE_BTN_TEX_H = 20;
    private static final ResourceLocation VANILLA_BUTTON_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation VANILLA_BUTTON_HIGHLIGHTED_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation TOGGLE_TEX_OFF =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/x_button.png");
    private static final ResourceLocation TOGGLE_TEX_OFF_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/x_button-hovered.png");
    private static final ResourceLocation TOGGLE_TEX_ON =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/complete_filter.png");
    private static final ResourceLocation DUPLICATE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/duplicate_button.png");
    private static final ResourceLocation DUPLICATE_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/duplicate_button-hovered.png");
    private static final ResourceLocation DELETE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter.png");
    private static final ResourceLocation DELETE_CONFIRM_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/are_you_sure_button.png");
    private static final ResourceLocation DELETE_CONFIRM_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/are_you_sure_button-hovered.png");
    private static final ResourceLocation MOVE_UP_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/move_up.png");
    private static final ResourceLocation MOVE_DOWN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/move_down.png");
    private static final ResourceLocation MOVE_UP_HIGHLIGHTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/move_up_highlighted.png");
    private static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/move_down_highlighted.png");
    private static final ResourceLocation LOCK_TEX_ENABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter.png");
    private static final ResourceLocation LOCK_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter_disabled.png");
    private static final ResourceLocation LOCK_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter_hovered.png");
    private static final ResourceLocation HEADER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/9-slice-header.png");
    private static final ResourceLocation TAB_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png");
    private static final ResourceLocation QUEST_TAB_SCROLL_ICON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/scroll-icon.png");
    private static final ResourceLocation BUILTIN_PACK_ENABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/popup_confirmation.png");
    private static final ResourceLocation BUILTIN_PACK_DISABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/popup_reject.png");

    private static final int TOGGLE_SIZE = 20;
    private static final int SMALL_BTN_SIZE = 20;
    private static final int SMALL_BTN_GAP = 4;
    private static final int TAB_W = 35;
    private static final int TAB_H = 27;
    private static final int TAB_GAP = 3;
    private static final int TOP_ACTION_GAP = 3;
    private static final int BOTTOM_CREATE_GAP = 3;
    private static final int CREATE_PACK_BUTTON_OFFSET_X = -1;
    private static final int LIST_CREATE_BUTTON_W = 127;
    private static final int LIST_CREATE_BUTTON_H = 20;
    private static final int HEADER_TEX_W = 72;
    private static final int HEADER_TEX_H = 10;
    private static final int HEADER_SLICE = 3;
    private static final int INLINE_FLAG_LABEL_H = 8;

    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;
    private static final int ROW_H = 27;
    private static final int ROW_PAD = 1;
    private static final int BOTTOM_BAR_H = 24;
    private static final int FIELD_LABEL_GAP = 2;
    private static final int FIELD_ROW_GAP = 6;
    private static final int BOX_H = 20;
    private static final int BOX_H_TALL = 28;
    private static final float INPUT_TEXT_SCALE = 0.5f;
    private static final int FORMAT_BAR_GAP = 3;
    private static final int FORMAT_BAR_H = 9;
    private static final int FORMAT_BTN_GAP = 1;
    private static final int MOVE_ICON_SIZE = 14;
    private static final int MOVE_ICON_TEX_SIZE = 32;
    private static final int CATEGORY_ARROW_SIZE = 32;
    private static final int MOVE_ICON_INSET_RIGHT = 3;
    private static final int MOVE_ICON_TOP_OFFSET = 1;
    private static final int MOVE_ICON_BOTTOM_OFFSET = 13;
    private static final int DEP_LOCK_SIZE = 13;
    private static final int DEP_LOCK_GAP = 2;
    private static final int DEP_ENTRY_ICON_SPACE = 12;
    private static final float DEP_ENTRY_ICON_SCALE = 0.625f;

    private static final ResourceLocation GENERATED_PACK_ICON =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/pack.png");
    private static final ResourceLocation IMPORT_PACK_BUTTON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/import-button.png");
    private static final ResourceLocation IMPORT_PACK_BUTTON_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/import-button-hovered.png");
    private static final ResourceLocation PACK_ACTION_NEEDED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/quest_widget-action-needed.png");
    private static final ResourceLocation PACK_CHANGED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget_completed.png");
    private static final int DEFAULT_INPUT_TEXT_COLOR = 0xE0E0E0;
    private static final int DROPDOWN_INPUT_TEXT_COLOR = 0xFFFFFF;
    private static final int INVALID_INPUT_TEXT_COLOR = 0xFF4040;
    private static final String INVALID_ID_TOOLTIP = "Invalid ID";
    private static final int ID_SUGGESTION_MAX = 5000;
    private static final int ID_SUGGESTION_VISIBLE_ROWS = 6;
    private static final int ID_SUGGESTION_ROW_H = 8;
    private static final int ID_SUGGESTION_TEXT_TOP_PADDING = 2;
    private static final float ID_SUGGESTION_TEXT_SCALE = 0.5f;
    private static final int ID_SUGGESTION_BG_COLOR = 0xFF000000;
    private static final int ID_SUGGESTION_TEXT_COLOR = 0xFFFFFF00;
    private static final int ID_SUGGESTION_BORDER_COLOR = 0xFFFFFFFF;
    private static final int ID_SUGGESTION_SCROLL_TRACK_COLOR = 0xFF2C2C2C;
    private static final int ID_SUGGESTION_SCROLL_THUMB_COLOR = 0xFFFFFFFF;
    private static final int ID_SUGGESTION_SCROLL_W = 3;
    private static final int ENTRY_ROW_H = 12;
    private static final int ENTRY_ROW_GAP = 2;
    private static final float ENTRY_INPUT_TEXT_SCALE = 0.4f;
    private static final int ENTRY_REMOVE_BTN_W = 10;
    private static final int ENTRY_TYPE_BTN_W = 21;
    private static final int ENTRY_ITEM_PICK_BTN_W = 12;
    private static final int ITEM_PICKER_COLS = 9;
    private static final int ITEM_PICKER_ROWS = 4;
    private static final int ITEM_PICKER_CELL = 18;
    private static final int ITEM_PICKER_W = 172;
    private static final int ITEM_PICKER_H = 112;
    private static final float ENTRY_TYPE_TEXT_SCALE = 0.5f;
    private static final int EDITOR_SUBHEADER_H = 12;
    private static final int EDITOR_SUBHEADER_GAP = 3;

    private static final String ENTRY_CREATE_PACK = "__create_pack__";
    private static final String ENTRY_BUILTIN_PACK = "__builtin_pack__";
    private static final String ENTRY_NEW = "__new__";
    private static final String ENTRY_CATEGORIES = "__categories__";
    private static final String ENTRY_SUBCATEGORIES = "__subcategories__";
    private static final String ENTRY_QUESTS = "__quests__";
    private static final String FOOTER_PREFIX =
            "Welcome to the Quest editor BETA, if you require assistance head over to ";
    private static final String FOOTER_LINK = "https://discord.gg/DARzByw6VW";
    private static final int FOOTER_TEXT_COLOR = 0xFF5555;
    private static final int FOOTER_LINK_COLOR = 0x3B82F6;
    private static final long PENDING_INIT_TTL_MS = 15000L;
    private static ScreenState pendingInitState;
    private static long pendingInitUntil = 0L;

    private final Screen parent;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private int leftX;
    private int rightX;
    private int topY;
    private int pxLeft;
    private int pxRight;
    private int py;
    private int pw;
    private int ph;
    private int listH;
    private BackButton backButton;
    private EditBox questSearchBox;
    private Button createPackButton;
    private IconButton importQuestPackButton;
    private EditorTabButton categoriesTabButton;
    private EditorTabButton subCategoriesTabButton;
    private EditorTabButton questsTabButton;

    private Mode mode = Mode.PACK_LIST;
    private EditorType editorType = EditorType.NONE;
    private QuestPack currentPack;
    private String selectedEntryId = "";

    private EditorListWidget leftList;
    private ActionButton saveButton;
    private Button exportPackButton;
    private IconButton duplicateButton;
    private IconButton deleteQuestButton;

    private float editorScroll = 0f;
    private String statusMessage = "";
    private int statusColor = 0xA0A0A0;
    private boolean deleteConfirmArmed = false;
    private ScreenState pendingState;
    private final List<FormattedCharSequence> footerLines = new ArrayList<>();
    private final List<Integer> footerLineX = new ArrayList<>();
    private int footerStartY = 0;

    private final List<FormField> activeFields = new ArrayList<>();
    private final List<FormField> allFields = new ArrayList<>();

    private EditBox packNameBox;
    private EditBox packNamespaceBox;
    private EditBox packIconPathBox;
    private ScaledMultiLineEditBox packDescriptionBox;

    private EditBox catIdBox;
    private EditBox catNameBox;
    private EditBox catIconBox;
    private EditBox catDependencyBox;
    private ToggleButton catAutoCompleteToggle;

    private EditBox subIdBox;
    private EditBox subCategoryBox;
    private EditBox subNameBox;
    private EditBox subIconBox;
    private ToggleButton subDefaultOpenToggle;

    private EditBox questIdBox;
    private EditBox questNameBox;
    private EditBox questIconBox;
    private ScaledMultiLineEditBox questDescriptionBox;
    private EditBox questCategoryBox;
    private EditBox questSubCategoryBox;
    private ScaledMultiLineEditBox questDependenciesBox;
    private LockToggleButton questDependencyLockToggle;
    private ToggleButton questOptionalToggle;
    private ToggleButton questRepeatableToggle;
    private ToggleButton questAutoCompleteToggle;
    private ToggleButton questHiddenUnderDependencyToggle;
    private ScaledMultiLineEditBox questCompletionBox;
    private ScaledMultiLineEditBox questRewardBox;
    private final List<ScaledMultiLineEditBox> dependencyEntryBoxes = new ArrayList<>();
    private final List<ScaledMultiLineEditBox> completionEntryBoxes = new ArrayList<>();
    private final List<ScaledMultiLineEditBox> rewardEntryBoxes = new ArrayList<>();
    private final List<EntryRemoveButton> dependencyEntryRemoveButtons = new ArrayList<>();
    private final List<LockToggleButton> dependencyEntryLockButtons = new ArrayList<>();
    private final List<EntryRemoveButton> completionEntryRemoveButtons = new ArrayList<>();
    private final List<EntryRemoveButton> rewardEntryRemoveButtons = new ArrayList<>();
    private final List<EntryTypeButton> completionEntryTypeButtons = new ArrayList<>();
    private final List<EntryTypeButton> rewardEntryTypeButtons = new ArrayList<>();
    private final List<EntryItemPickerButton> completionEntryItemPickerButtons = new ArrayList<>();
    private final List<EntryItemPickerButton> rewardEntryItemPickerButtons = new ArrayList<>();
    private final Map<ScaledMultiLineEditBox, String> entryTypeByBox = new HashMap<>();
    private final Map<ScaledMultiLineEditBox, String> selectedItemIdByBox = new HashMap<>();
    private final Map<ScaledMultiLineEditBox, String> selectedItemComponentsByBox = new HashMap<>();
    private static final String LEGACY_PACK_TOOLTIP = "Incompatible pack";
    private boolean syncingEntryRows = false;
    private boolean entryRowsDirty = false;
    private final List<TextInsertButton> descriptionFormatButtons = new ArrayList<>();
    private String questOrderToken = "";

    private Path editingPath;
    private String loadedQuestType = "";
    private final List<String> itemIdCache = new ArrayList<>();
    private final List<String> entityIdCache = new ArrayList<>();
    private final List<String> effectIdCache = new ArrayList<>();
    private final List<String> advancementIdCache = new ArrayList<>();
    private final List<String> lootTableIdCache = new ArrayList<>();
    private final Set<String> categoryIdCache = new HashSet<>();
    private final Set<String> subCategoryIdCache = new HashSet<>();
    private final Set<String> questIdCache = new HashSet<>();
    private final Map<String, String> questIconByIdCache = new HashMap<>();
    private final List<String> categorySuggestionCache = new ArrayList<>();
    private final List<String> subCategorySuggestionCache = new ArrayList<>();
    private final List<String> questSuggestionCache = new ArrayList<>();
    private final Map<String, List<String>> subCategoryByCategorySuggestion = new HashMap<>();
    private final Map<String, QuestPack> stagedPacks = new LinkedHashMap<>();
    private final Set<String> stagedDeletedPackNames = new LinkedHashSet<>();
    private final Set<String> collapsedQuestCategories = new HashSet<>();
    private final Set<String> collapsedQuestSubCategories = new HashSet<>();
    private final List<String> activeIdSuggestions = new ArrayList<>();
    private EditBox idSuggestionField;
    private ScaledMultiLineEditBox idSuggestionMultiLineField;
    private int idSuggestionScroll = 0;
    private boolean suppressIdSuggestions = false;
    private boolean suppressIdSanitizer = false;
    private boolean closingEditor = false;
    private String savedEditorState;
    private String pendingDiscardEntryId = "";
    private Mode pendingDiscardMode;
    private String questSearchQuery = "";
    private EntryRowKind openTypeMenuKind;
    private int openTypeMenuRow = -1;
    private int openTypeMenuX;
    private int openTypeMenuY;
    private int openTypeMenuW;
    private int openTypeMenuH;
    private int openTypeMenuScroll = 0;
    private DropdownMenuTarget openDropdownTarget;
    private int openDropdownX;
    private int openDropdownY;
    private int openDropdownW;
    private int openDropdownH;
    private int openDropdownScroll = 0;
    private int subCategoryParentDropdownScroll = 0;
    private int questCategoryDropdownScroll = 0;
    private int questSubCategoryDropdownScroll = 0;
    private EntryRowKind itemPickerKind;
    private int itemPickerRow = -1;
    private ItemPickerTab itemPickerTab = ItemPickerTab.CREATIVE;
    private PickerMode pickerMode = PickerMode.ITEMS;
    private int itemPickerPage = 0;
    private EditBox itemPickerSearchBox;

    public QuestEditorScreen(Screen parent) {
        super(tr("title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        closeTransientMenus();
        leftX = (width / 2) - PANEL_W - 2;
        rightX = (width / 2) + 2;
        topY = (height / 2) - PANEL_H / 2;
        pxLeft = leftX + 10;
        pxRight = rightX + 10;
        py = topY + 10;
        pw = 127;
        int interiorH = PANEL_H - 20;
        listH = interiorH - BOTTOM_BAR_H;
        ph = listH;

        leftList = new EditorListWidget(pxLeft, py, pw, listH, this::handleLeftClick, this::handleLeftAction, this::handleLeftSecondaryClick, this::handleEntryMoveAction);
        addRenderableWidget(leftList);

        initFormFields();

        int barY = py + ph + (BOTTOM_BAR_H - 20) / 2;
        int saveX = pxRight + pw - 68;
        saveButton = new ActionButton(saveX, barY, 68, 20,
                tr("save"), this::saveCurrent);
        addRenderableWidget(saveButton);
        exportPackButton = Button.builder(tr("export"), button -> exportCurrentPack())
                .bounds(saveX - 70, barY, 68, 20)
                .build();
        exportPackButton.visible = false;
        exportPackButton.active = false;
        addRenderableWidget(exportPackButton);

        int deleteQuestX = saveX - SMALL_BTN_GAP - SMALL_BTN_SIZE;
        int duplicateX = deleteQuestX - SMALL_BTN_GAP - SMALL_BTN_SIZE;
        duplicateButton = new IconButton(duplicateX, barY, SMALL_BTN_SIZE, DUPLICATE_TEX, DUPLICATE_TEX_HOVER, this::duplicateCurrent);
        deleteQuestButton = new IconButton(deleteQuestX, barY, SMALL_BTN_SIZE, DELETE_TEX, DELETE_CONFIRM_TEX_HOVER, this::handleDeleteButtonPress);
        addRenderableWidget(duplicateButton);
        addRenderableWidget(deleteQuestButton);

        backButton = new BackButton(pxLeft + 2, barY, this::goBack);
        addRenderableWidget(backButton);
        createPackButton = Button.builder(tr("create_new"), button -> openPackCreate())
                .bounds(pxLeft + 2 + backButton.getWidth() + BOTTOM_CREATE_GAP + CREATE_PACK_BUTTON_OFFSET_X, barY, 72, 20)
                .build();
        addRenderableWidget(createPackButton);
        importQuestPackButton = new IconButton(createPackButton.getX() + createPackButton.getWidth() + 2, barY, SMALL_BTN_SIZE, IMPORT_PACK_BUTTON_TEX, IMPORT_PACK_BUTTON_TEX_HOVER, this::openImportQuestPackDirectory);
        addRenderableWidget(importQuestPackButton);
        categoriesTabButton = new EditorTabButton(trs("categories"), "boundless:quest_book", Mode.CATEGORY_LIST);
        subCategoriesTabButton = new EditorTabButton(trs("subcategories"), "minecraft:book", Mode.SUBCATEGORY_LIST);
        questsTabButton = new EditorTabButton(trs("quests"), QUEST_TAB_SCROLL_ICON_TEX, Mode.QUEST_LIST);
        questSearchBox = new EditBox(font, pxLeft + 2 + 24 + SMALL_BTN_GAP, barY, pw - 24 - SMALL_BTN_GAP - 2, 20, tr("search_quests"));
        questSearchBox.setMaxLength(128);
        questSearchBox.setHint(tr("search_quests"));
        questSearchBox.setValue(questSearchQuery);
        questSearchBox.setResponder(value -> {
            questSearchQuery = safe(value);
            if (mode == Mode.QUEST_LIST) refreshLeftList();
        });
        questSearchBox.visible = false;
        questSearchBox.active = false;
        addRenderableWidget(questSearchBox);

        if (pendingState != null) {
            ScreenState state = pendingState;
            pendingState = null;
            restoreState(state);
            return;
        }
        ScreenState initState = takePendingInitState();
        if (initState != null) {
            restoreState(initState);
            return;
        }
        setMode(Mode.PACK_LIST);
    }

    private void initFormFields() {
        packNameBox = createBox("Pack name", BOX_H);
        packNamespaceBox = createBox("Namespace", BOX_H);
        packIconPathBox = createBox("Pack icon path", BOX_H);
        packDescriptionBox = createMultiLineBox("Pack description", BOX_H_TALL, false);

        catIdBox = createBox("Category id", BOX_H);
        catNameBox = createBox("Category name", BOX_H);
        catNameBox.setMaxLength(22);
        catIconBox = createBox("Category icon", BOX_H);
        catDependencyBox = createBox("Category dependency", BOX_H);
        catAutoCompleteToggle = createToggle(false);

        subIdBox = createBox("Sub-category id", BOX_H);
        subCategoryBox = createBox("Parent category id", BOX_H);
        subNameBox = createBox("Sub-category name", BOX_H);
        subNameBox.setMaxLength(26);
        subIconBox = createBox("Sub-category icon", BOX_H);
        subDefaultOpenToggle = createToggle(false);

        questIdBox = createBox("Quest id", BOX_H);
        questNameBox = createBox("Quest name", BOX_H);
        questIconBox = createBox("Quest icon", BOX_H);
        questDescriptionBox = createMultiLineBox("Quest description", BOX_H_TALL, true);
        questCategoryBox = createBox("Quest category", BOX_H);
        questSubCategoryBox = createBox("Quest sub-category", BOX_H);
        questDependenciesBox = createMultiLineBox("Dependencies", BOX_H_TALL, false);
        questDependencyLockToggle = createDependencyLockToggle(false);
        questOptionalToggle = createToggle(false);
        questRepeatableToggle = createToggle(false);
        questAutoCompleteToggle = createToggle(false);
        questHiddenUnderDependencyToggle = createToggle(false);
        questCompletionBox = createMultiLineBox("Completion entries", BOX_H_TALL, false);
        questRewardBox = createMultiLineBox("Reward entries", BOX_H_TALL, false);
        initEntryRowBoxes();
        initDescriptionFormatterButtons();

        attachIdSanitizer(packNamespaceBox, false);
        attachIdSanitizer(packIconPathBox, false);
        attachIdSanitizer(catIdBox, false);
        attachIdSanitizer(catDependencyBox, false);
        attachIdSanitizer(subIdBox, false);
        subCategoryBox.setEditable(false);
        attachIdSanitizer(questIdBox, false);
        questCategoryBox.setEditable(false);
        questSubCategoryBox.setEditable(false);
        applyDropdownTextColor(subCategoryBox, false);
        applyDropdownTextColor(questCategoryBox, false);
        applyDropdownTextColor(questSubCategoryBox, false);
    }

    private void initDescriptionFormatterButtons() {
        descriptionFormatButtons.clear();
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/r", 0xFFFF5555, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/g", 0xFF55FF55, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/b", 0xFF5555FF, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/w", 0xFF55FFFF, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/y", 0xFFFFFF55, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/o", 0xFFFFAA00, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/a", 0xFFAAAAAA, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/p", 0xFFAA55FF, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("X", "/x", 0xFF4A4A4A, 11, 0.8f));
        descriptionFormatButtons.add(createDescriptionInsertButton("B", "/l", 0xFF555555, 11, 0.8f));
        descriptionFormatButtons.add(createDescriptionInsertButton("I", "/i", 0xFF555555, 11, 0.8f));
        descriptionFormatButtons.add(createDescriptionInsertButton("E", "/e", 0xFF555555, 11, 0.8f));
    }

    private TextInsertButton createDescriptionInsertButton(String label, String insertText, int fillColor, int width) {
        return createDescriptionInsertButton(label, insertText, fillColor, width, 1.0f);
    }

    private TextInsertButton createDescriptionInsertButton(String label, String insertText, int fillColor, int width, float textScale) {
        TextInsertButton button = new TextInsertButton(0, 0, width, label, insertText, fillColor, textScale, this::applyDescriptionFormat);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    private EditBox createBox(String hint, int height) {
        EditBox box = new EditBox(font, 0, 0, pw - 4, height, Component.literal(hint));
        box.setMaxLength(1024);
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    private ScaledMultiLineEditBox createMultiLineBox(String hint, int height) {
        return createMultiLineBox(hint, height, false);
    }

    private ScaledMultiLineEditBox createMultiLineBox(String hint, int height, boolean allowColorFormatting) {
        ScaledMultiLineEditBox box = new ScaledMultiLineEditBox(font, 0, 0, pw - 4, height, Component.literal(hint), Component.empty(), INPUT_TEXT_SCALE, allowColorFormatting);
        box.setCharacterLimit(4096);
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    private ToggleButton createToggle(boolean initial) {
        ToggleButton button = new ToggleButton(0, 0, TOGGLE_SIZE, TOGGLE_SIZE, initial);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    private LockToggleButton createDependencyLockToggle(boolean initial) {
        LockToggleButton button = new LockToggleButton(0, 0, DEP_LOCK_SIZE, DEP_LOCK_SIZE, initial);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    private void setMode(Mode next) {
        if (next == Mode.QUEST_LIST && mode != Mode.QUEST_LIST) {
            collapsedQuestCategories.clear();
            collapsedQuestSubCategories.clear();
        }
        closeTransientMenus();
        mode = next;
        statusMessage = "";
        editorScroll = 0f;
        disarmDeleteConfirm();
        refreshLeftList(false);
        clearEditor();
        updateLeftPaneLayout();
        updateBackButtonVisibility();
    }

    private void refreshLeftList() {
        refreshLeftList(true);
    }

    private void refreshLeftList(boolean preserveScroll) {
        float previousScroll = 0f;
        if (preserveScroll && leftList != null) {
            previousScroll = leftList.getScrollY();
        }
        List<EditorEntry> entries = switch (mode) {
            case PACK_LIST, PACK_CREATE -> buildPackEntries();
            case PACK_MENU -> buildPackMenuEntries();
            case CATEGORY_LIST -> buildCategoryEntries();
            case SUBCATEGORY_LIST -> buildSubCategoryEntries();
            case QUEST_LIST -> buildQuestEntries();
        };
        leftList.setEntries(entries);
        if (preserveScroll) {
            leftList.setScrollY(previousScroll);
        }
        leftList.setSelectedId(selectedEntryId);
        refreshPackIdCaches();
    }

    private List<EditorEntry> buildPackEntries() {
        List<EditorEntry> out = new ArrayList<>();
        boolean builtinEnabled = Config.enableBuiltinQuestPack();
        out.add(new EditorEntry(
                ENTRY_BUILTIN_PACK,
                trs("builtin_pack"),
                builtinEnabled ? trs("builtin_pack_enabled_subtitle") : trs("builtin_pack_disabled_subtitle"),
                "",
                builtinEnabled ? BUILTIN_PACK_ENABLED_TEX : BUILTIN_PACK_DISABLED_TEX,
                builtinEnabled ? trs("enabled") : trs("disabled")
        ));

        for (QuestPack pack : listPacks()) {
            PackMeta meta = readPackMeta(pack.root, pack.name);
            String packIconId = normalizePackIconId(meta.iconPath);
            boolean legacy = pack.legacy;
            boolean changed = stagedPacks.containsKey(pack.name);
            boolean enabled = !legacy && pack.enabled;
            ResourceLocation actionIcon = enabled ? BUILTIN_PACK_ENABLED_TEX : BUILTIN_PACK_DISABLED_TEX;
            String actionTooltip = legacy ? trs("legacy_pack") : (enabled ? trs("enabled") : trs("disabled"));
            ResourceLocation rowTexture = legacy ? PACK_ACTION_NEEDED_TEX : (changed ? PACK_CHANGED_TEX : ROW_TEX);
            out.add(new EditorEntry(
                    pack.name,
                    pack.name,
                    "",
                    packIconId,
                    actionIcon,
                    actionTooltip,
                    legacy ? LEGACY_PACK_TOOLTIP : trs("tooltip.right_click_options"),
                    rowTexture
            ));
        }
        return out;
    }

    private List<EditorEntry> buildPackMenuEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_CATEGORIES, trs("categories"), "", ""));
        out.add(new EditorEntry(ENTRY_SUBCATEGORIES, trs("subcategories"), "", ""));
        out.add(new EditorEntry(ENTRY_QUESTS, trs("quests"), "", ""));
        return out;
    }

    private List<EditorEntry> buildCategoryEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_NEW, trs("create_new_category"), "", ""));
        if (currentPack == null) return out;
        for (NamedEntry entry : listCategoryEntries(currentPack)) {
            out.add(EditorEntry.movable(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    private List<EditorEntry> buildSubCategoryEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_NEW, trs("create_new_subcategory"), "", ""));
        if (currentPack == null) return out;
        for (NamedEntry entry : listSubCategoryEntries(currentPack)) {
            out.add(EditorEntry.movable(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    private List<EditorEntry> buildQuestEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_NEW, trs("create_new_quest"), "", ""));
        if (currentPack == null) return out;
        Map<String, String> categoryNames = new HashMap<>();
        for (NamedEntry category : listCategoryEntries(currentPack)) {
            categoryNames.put(safe(category.id), safe(category.name));
        }
        Map<String, String> subCategoryNames = new HashMap<>();
        for (NamedEntry subCategory : listSubCategoryEntries(currentPack)) {
            SubCategoryData data = loadSubCategory(currentPack, subCategory.id);
            String parent = data == null ? "" : safe(data.category);
            subCategoryNames.put(parent + "::" + safe(subCategory.id), safe(subCategory.name));
        }

        Map<String, Map<String, List<NamedEntry>>> grouped = new LinkedHashMap<>();
        for (NamedEntry entry : listQuestEntries(currentPack)) {
            if (!matchesQuestSearch(entry)) continue;
            QuestEntryData data = loadQuest(currentPack, entry.id);
            String categoryId = data == null ? "" : safe(data.category);
            String subCategoryId = data == null ? "" : safe(data.subCategory);
            grouped.computeIfAbsent(categoryId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(subCategoryId, k -> new ArrayList<>())
                    .add(entry);
        }

        for (Map.Entry<String, Map<String, List<NamedEntry>>> categoryEntry : grouped.entrySet()) {
            String categoryId = safe(categoryEntry.getKey());
            String categoryName = categoryNames.getOrDefault(categoryId, categoryId.isBlank() ? trs("unassigned") : categoryId);
            boolean categoryCollapsed = collapsedQuestCategories.contains(categoryId);
            out.add(EditorEntry.categoryHeader(categoryId, categoryName, categoryCollapsed));
            if (categoryCollapsed) continue;

            for (Map.Entry<String, List<NamedEntry>> subEntry : categoryEntry.getValue().entrySet()) {
                String subId = safe(subEntry.getKey());
                String subKey = categoryId + "::" + subId;
                String subName = subCategoryNames.getOrDefault(subKey, subId.isBlank() ? trs("no_subcategory") : subId);
                boolean subCollapsed = collapsedQuestSubCategories.contains(subKey);
                out.add(EditorEntry.subCategoryHeader(subKey, subName, subCollapsed));
                if (subCollapsed) continue;
                for (NamedEntry quest : subEntry.getValue()) {
                    QuestEntryData questData = loadQuest(currentPack, quest.id);
                    String invalidReason = questEntryInvalidReason(questData);
                    boolean invalid = invalidReason != null;
                    out.add(invalid
                            ? EditorEntry.quest(quest.id, quest.name, quest.id, quest.icon, PACK_ACTION_NEEDED_TEX, invalidReason)
                            : EditorEntry.quest(quest.id, quest.name, quest.id, quest.icon));
                }
            }
        }
        return out;
    }

    private boolean isQuestEntryInvalid(QuestEntryData data) {
        return questEntryInvalidReason(data) != null;
    }

    private String questEntryInvalidReason(QuestEntryData data) {
        if (data == null) return null;
        String id = safe(data.id).trim();
        if (id.isBlank()) return "Missing quest id";
        if (isDuplicateQuestIdAcrossPack(id)) return "Duplicate quest id";
        String name = safe(data.name).trim();
        if (name.isBlank()) return "Missing quest name";
        String category = safe(data.category).trim();
        if (category.isBlank()) return "Missing quest category";
        if (!categoryIdCache.contains(category)) return "Invalid quest category";

        String subCategory = safe(data.subCategory).trim();
        if (!subCategory.isBlank()) {
            boolean hasExact = subCategoryIdCache.contains(category + "::" + subCategory);
            boolean hasWildcard = subCategoryIdCache.contains("::" + subCategory);
            if (!hasExact && !hasWildcard) return "Invalid quest sub-category";
        }

        for (String dep : extractEntryLines(safe(data.dependencies))) {
            if (!questIdCache.contains(dep)) return "Invalid quest dependency";
        }
        return null;
    }

    private boolean matchesQuestSearch(NamedEntry entry) {
        String query = safe(questSearchQuery).trim().toLowerCase(Locale.ROOT);
        if (query.isBlank() || entry == null) return true;
        return safe(entry.id).toLowerCase(Locale.ROOT).contains(query)
                || safe(entry.name).toLowerCase(Locale.ROOT).contains(query)
                || safe(entry.sortKey).toLowerCase(Locale.ROOT).contains(query);
    }

    private void handleLeftClick(EditorEntry entry) {
        if (entry == null) return;
        if (shouldWarnForUnsavedChanges(entry)) {
            return;
        }
        clearPendingDiscardState();
        selectedEntryId = entry.id;
        statusMessage = "";
        leftList.setSelectedId(selectedEntryId);

        switch (mode) {
            case PACK_LIST, PACK_CREATE -> handlePackEntry(entry);
            case PACK_MENU -> handlePackMenuEntry(entry);
            case CATEGORY_LIST -> handleCategoryEntry(entry);
            case SUBCATEGORY_LIST -> handleSubCategoryEntry(entry);
            case QUEST_LIST -> handleQuestEntry(entry);
        }
    }

    private void handleLeftSecondaryClick(EditorEntry entry) {
        if (entry == null || mode != Mode.PACK_LIST || ENTRY_BUILTIN_PACK.equals(entry.id)) return;
        if (shouldWarnForUnsavedChanges(entry)) {
            return;
        }
        clearPendingDiscardState();
        QuestPack pack = findPackByName(entry.id);
        if (pack == null) {
            statusMessage = trs("status.pack_not_found");
            statusColor = 0xFF8080;
            return;
        }
        currentPack = pack;
        selectedEntryId = pack.name;
        leftList.setSelectedId(selectedEntryId);
        showPackOptions(pack);
    }

    private void handleLeftAction(EditorEntry entry) {
        if (entry == null || mode != Mode.PACK_LIST) return;
        if (ENTRY_BUILTIN_PACK.equals(entry.id)) {
            boolean next = !Config.enableBuiltinQuestPack();
            Config.ENABLE_BUILTIN_QUEST_PACK.set(next);
            Config.SPEC.save();
            runBoundlessReloadInBackground();
            QuestPanelClient.applyConfigChanges();
            statusMessage = next ? trs("status.builtin_enabled") : trs("status.builtin_disabled");
            statusColor = 0xA0FFA0;
            refreshLeftList();
            return;
        }
        QuestPack pack = findPackByName(entry.id);
        if (pack == null) return;
        if (pack.legacy) {
            return;
        }
        boolean next = !pack.enabled;
        if (!setQuestPackEnabled(pack, next)) {
            setError(trs("error.update_questpack_failed"));
            return;
        }
        runBoundlessReloadInBackground();
        statusMessage = trs(next ? "status.questpack_enabled" : "status.questpack_disabled", pack.name);
        statusColor = 0xA0FFA0;
        refreshLeftList();
    }

    private void handleEntryMoveAction(EditorEntry entry, MoveDirection direction) {
        if (entry == null || direction == null || currentPack == null) return;
        if (entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER) return;

        try {
            String successMessage = null;
            if (mode == Mode.QUEST_LIST && entry.kind == EditorEntryKind.QUEST) {
                moveQuestWithinGroup(entry.id, direction);
                successMessage = direction == MoveDirection.UP ? "Quest moved up" : "Quest moved down";
            } else if (mode == Mode.CATEGORY_LIST && entry.kind == EditorEntryKind.NORMAL && !ENTRY_NEW.equals(entry.id)) {
                moveCategoryByOrder(entry.id, direction);
                successMessage = direction == MoveDirection.UP ? "Category moved up" : "Category moved down";
            } else if (mode == Mode.SUBCATEGORY_LIST && entry.kind == EditorEntryKind.NORMAL && !ENTRY_NEW.equals(entry.id)) {
                moveSubCategoryByOrder(entry.id, direction);
                successMessage = direction == MoveDirection.UP ? "Sub-category moved up" : "Sub-category moved down";
            } else {
                return;
            }

            selectedEntryId = entry.id;
            if (leftList != null) leftList.setSelectedId(selectedEntryId);
            refreshLeftList();
            statusMessage = successMessage;
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            if (mode == Mode.CATEGORY_LIST) {
                setError("Failed to reorder category");
            } else if (mode == Mode.SUBCATEGORY_LIST) {
                setError("Failed to reorder sub-category");
            } else {
                setError("Failed to reorder quest");
            }
        }
    }

    private void moveQuestWithinGroup(String questId, MoveDirection direction) throws IOException {
        if (currentPack == null || questId == null || questId.isBlank()) return;

        List<NamedEntry> all = listQuestEntries(currentPack);
        List<QuestMoveEntry> group = new ArrayList<>();
        for (NamedEntry entry : all) {
            QuestEntryData data = loadQuest(currentPack, entry.id);
            if (data == null) continue;
            group.add(new QuestMoveEntry(entry.id, entry.path, safe(data.category), safe(data.subCategory)));
        }

        QuestMoveEntry current = null;
        for (QuestMoveEntry entry : group) {
            if (entry.id.equals(questId)) {
                current = entry;
                break;
            }
        }
        if (current == null) return;

        List<QuestMoveEntry> siblings = new ArrayList<>();
        for (QuestMoveEntry entry : group) {
            if (entry.category.equals(current.category) && entry.subCategory.equals(current.subCategory)) {
                siblings.add(entry);
            }
        }
        if (siblings.size() < 2) return;

        int index = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).id.equals(questId)) {
                index = i;
                break;
            }
        }
        if (index < 0) return;

        int targetIndex = direction == MoveDirection.UP ? index - 1 : index + 1;
        if (targetIndex < 0 || targetIndex >= siblings.size()) return;

        QuestMoveEntry moving = siblings.remove(index);
        siblings.add(targetIndex, moving);

        applyQuestOrder(siblings);
    }

    private void applyQuestOrder(List<QuestMoveEntry> orderedEntries) throws IOException {
        if (currentPack == null || orderedEntries == null || orderedEntries.isEmpty()) return;
        if (usesSingleFilePack(currentPack)) {
            applySingleFileOrder(currentPack.questsFile, orderedEntries.stream().map(e -> e.id).toList(), "order");
            return;
        }

        Map<Path, Path> stagedMoves = new LinkedHashMap<>();
        for (int i = 0; i < orderedEntries.size(); i++) {
            QuestMoveEntry entry = orderedEntries.get(i);
            String orderToken = String.format(Locale.ROOT, "%02d", i + 1);
            Path target = currentPack.questsDir.resolve(questFileBaseName(entry.id, orderToken) + ".json");
            if (entry.path.equals(target)) continue;

            Path temp = entry.path.resolveSibling(entry.path.getFileName().toString() + ".reorder_tmp");
            int guard = 0;
            while (Files.exists(temp) || stagedMoves.containsKey(temp)) {
                guard++;
                temp = entry.path.resolveSibling(entry.path.getFileName().toString() + ".reorder_tmp_" + guard);
            }
            Files.move(entry.path, temp, StandardCopyOption.REPLACE_EXISTING);
            stagedMoves.put(temp, target);

            if (editingPath != null && editingPath.equals(entry.path)) {
                editingPath = target;
                questOrderToken = orderToken;
            }
        }

        for (Map.Entry<Path, Path> move : stagedMoves.entrySet()) {
            Files.move(move.getKey(), move.getValue(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void moveCategoryByOrder(String categoryId, MoveDirection direction) throws IOException {
        if (currentPack == null || categoryId == null || categoryId.isBlank()) return;
        List<NamedEntry> categories = listCategoryEntries(currentPack);
        moveOrderedEntry(categories, categoryId, direction, "order");
    }

    private void moveSubCategoryByOrder(String subCategoryId, MoveDirection direction) throws IOException {
        if (currentPack == null || subCategoryId == null || subCategoryId.isBlank()) return;
        List<NamedEntry> subCategories = listSubCategoryEntries(currentPack);
        moveOrderedEntry(subCategories, subCategoryId, direction, "order");
    }

    private void moveOrderedEntry(List<NamedEntry> entries, String id, MoveDirection direction, String key) throws IOException {
        if (entries == null || entries.size() < 2 || id == null || id.isBlank()) return;
        int index = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (id.equals(entries.get(i).id)) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        int targetIndex = direction == MoveDirection.UP ? index - 1 : index + 1;
        if (targetIndex < 0 || targetIndex >= entries.size()) return;

        NamedEntry moving = entries.remove(index);
        entries.add(targetIndex, moving);
        applyExplicitOrder(entries, key);
    }

    private void applyExplicitOrder(List<NamedEntry> orderedEntries, String key) throws IOException {
        if (orderedEntries == null || orderedEntries.isEmpty() || key == null || key.isBlank()) return;
        if (currentPack != null && usesSingleFilePack(currentPack)) {
            Path file = mode == Mode.CATEGORY_LIST ? currentPack.categoriesFile : currentPack.subCategoriesFile;
            applySingleFileOrder(file, orderedEntries.stream().map(e -> e.id).toList(), key);
            return;
        }
        for (int i = 0; i < orderedEntries.size(); i++) {
            NamedEntry entry = orderedEntries.get(i);
            if (entry == null || entry.path == null) continue;
            JsonObject obj = readJson(entry.path);
            if (obj == null) continue;
            obj.addProperty(key, i);
            try (BufferedWriter writer = Files.newBufferedWriter(entry.path, StandardCharsets.UTF_8)) {
                gson.toJson(obj, writer);
            }
        }
    }

    private void openPackCreate() {
        setMode(Mode.PACK_CREATE);
        showPackCreate();
    }

    private void handlePackEntry(EditorEntry entry) {
        if (ENTRY_BUILTIN_PACK.equals(entry.id)) {
            statusMessage = "Use the icon to enable or disable the built-in pack";
            statusColor = 0xA0A0A0;
            return;
        }

        currentPack = findPackByName(entry.id);
        if (currentPack == null) {
            statusMessage = trs("status.pack_not_found");
            statusColor = 0xFF8080;
            return;
        }
        if (currentPack.legacy) {
            return;
        }
        if (!ensurePackWorkspace(currentPack)) {
            setError("Failed to open pack");
            return;
        }
        String refreshedNamespace = findNamespace(currentPack.root);
        if (refreshedNamespace != null && !refreshedNamespace.isBlank()
                && !refreshedNamespace.equals(currentPack.namespace)) {
        currentPack = new QuestPack(currentPack.name, refreshedNamespace, currentPack.root, currentPack.legacy, currentPack.enabled);
        }
        if (usesSingleFilePack(currentPack)) {
            try {
                initializeSingleFilePack(currentPack);
            } catch (IOException e) {
                setError("Failed to initialize single-file pack");
                return;
            }
        }
        setMode(Mode.CATEGORY_LIST);
    }

    private void handlePackMenuEntry(EditorEntry entry) {
        if (ENTRY_CATEGORIES.equals(entry.id)) {
            setMode(Mode.CATEGORY_LIST);
            return;
        }
        if (ENTRY_SUBCATEGORIES.equals(entry.id)) {
            setMode(Mode.SUBCATEGORY_LIST);
            return;
        }
        if (ENTRY_QUESTS.equals(entry.id)) {
            setMode(Mode.QUEST_LIST);
        }
    }

    private void handleCategoryEntry(EditorEntry entry) {
        if (ENTRY_NEW.equals(entry.id)) {
            showCategoryEditor(new CategoryData(), null);
            return;
        }
        if (currentPack == null) return;
        CategoryData data = loadCategory(currentPack, entry.id);
        if (data == null) {
            statusMessage = "Failed to load category";
            statusColor = 0xFF8080;
            return;
        }
        showCategoryEditor(data, data.path);
    }

    private void handleSubCategoryEntry(EditorEntry entry) {
        if (ENTRY_NEW.equals(entry.id)) {
            showSubCategoryEditor(new SubCategoryData(), null);
            return;
        }
        if (currentPack == null) return;
        SubCategoryData data = loadSubCategory(currentPack, entry.id);
        if (data == null) {
            statusMessage = "Failed to load sub-category";
            statusColor = 0xFF8080;
            return;
        }
        showSubCategoryEditor(data, data.path);
    }

    private void handleQuestEntry(EditorEntry entry) {
        if (entry.kind == EditorEntryKind.CATEGORY_HEADER) {
            if (!collapsedQuestCategories.add(entry.id)) collapsedQuestCategories.remove(entry.id);
            refreshLeftList();
            return;
        }
        if (entry.kind == EditorEntryKind.SUBCATEGORY_HEADER) {
            if (!collapsedQuestSubCategories.add(entry.id)) collapsedQuestSubCategories.remove(entry.id);
            refreshLeftList();
            return;
        }
        if (ENTRY_NEW.equals(entry.id)) {
            showQuestEditor(new QuestEntryData(), null);
            return;
        }
        if (currentPack == null) return;
        QuestEntryData data = loadQuest(currentPack, entry.id);
        if (data == null) {
            statusMessage = "Failed to load quest";
            statusColor = 0xFF8080;
            return;
        }
        showQuestEditor(data, data.path);
    }

    private void showPackCreate() {
        editorType = EditorType.PACK_CREATE;
        editingPath = null;
        packNameBox.setValue("");
        packIconPathBox.setValue("");

        setActiveFields(List.of(
                field(trs("field.pack_name"), packNameBox),
                field(trs("field.icon"), packIconPathBox)
        ));
        saveButton.setMessage(tr("create"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorUnsaved();
        updateBackButtonVisibility();
    }

    private void showPackOptions(QuestPack pack) {
        if (pack == null) return;
        editorType = EditorType.PACK_OPTIONS;
        editingPath = null;

        PackMeta meta = readPackMeta(pack.root, pack.name);
        packNameBox.setValue(safe(pack.name));
        packIconPathBox.setValue(safe(meta.iconPath));

        List<FormField> fields = new ArrayList<>();
        fields.add(field(trs("field.pack_name"), packNameBox));
        fields.add(field(trs("field.icon"), packIconPathBox));
        setActiveFields(fields);
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        if (exportPackButton != null) {
            exportPackButton.visible = true;
            exportPackButton.active = true;
        }
        markCurrentEditorLoaded(true);
        updateBackButtonVisibility();
    }

    private void showCategoryEditor(CategoryData data, Path sourcePath) {
        editorType = EditorType.CATEGORY;
        editingPath = sourcePath;

        catIdBox.setValue(safe(data.id));
        catNameBox.setValue(safe(data.name));
        catIconBox.setValue(safe(data.icon));
        catDependencyBox.setValue(safe(data.dependency));
        catAutoCompleteToggle.setState(parseBool(data.autoComplete, false));

        setActiveFields(List.of(
                field(trs("field.id"), catIdBox),
                field(trs("field.name"), catNameBox),
                field(trs("field.icon"), catIconBox),
                field(trs("field.dependency"), catDependencyBox),
                field(trs("field.auto_complete") + " (" + trs("tooltip.auto_complete_category") + ")", catAutoCompleteToggle)
        ));
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    private void showSubCategoryEditor(SubCategoryData data, Path sourcePath) {
        editorType = EditorType.SUBCATEGORY;
        editingPath = sourcePath;

        subIdBox.setValue(safe(data.id));
        setDropdownBoxValue(subCategoryBox, safe(data.category));
        subNameBox.setValue(safe(data.name));
        subIconBox.setValue(safe(data.icon));
        subDefaultOpenToggle.setState(parseBool(data.defaultOpen, true));

        setActiveFields(List.of(
                field(trs("field.id"), subIdBox),
                field(trs("field.category_id"), subCategoryBox),
                field(trs("field.name"), subNameBox),
                field(trs("field.icon"), subIconBox),
                field(trs("field.default_open") + " (" + trs("tooltip.default_open") + ")", subDefaultOpenToggle)
        ));
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    private void showQuestEditor(QuestEntryData data, Path sourcePath) {
        editorType = EditorType.QUEST;
        editingPath = sourcePath;
        disarmDeleteConfirm();

        questIdBox.setValue(safe(data.id));
        questNameBox.setValue(safe(data.name));
        questIconBox.setValue(safe(data.icon));
        questDescriptionBox.setValue(safe(data.description));
        setDropdownBoxValue(questCategoryBox, safe(data.category));
        setDropdownBoxValue(questSubCategoryBox, safe(data.subCategory));
        setEntryRowsFromRaw(EntryRowKind.DEPENDENCY, safe(data.dependencies));
        setDependencyLockState(parseBool(data.lockAfterDependency, false));
        questOptionalToggle.setState(parseBool(data.optional, false));
        questRepeatableToggle.setState(parseBool(data.repeatable, false));
        questAutoCompleteToggle.setState(parseBool(data.autoComplete, false));
        questHiddenUnderDependencyToggle.setState(parseBool(data.hiddenUnderDependency, false));
        setEntryRowsFromRaw(EntryRowKind.COMPLETION, completionJsonToEntries(safe(data.completionJson)));
        setEntryRowsFromRaw(EntryRowKind.REWARD, rewardJsonToEntries(safe(data.rewardJson)));
        loadedQuestType = safe(data.type);

        questDescriptionBox.scrollToTop();
        questCompletionBox.scrollToTop();
        questRewardBox.scrollToTop();
        questOrderToken = questOrderTokenFromPath(sourcePath);

        applyQuestEditorFields();
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    private void applyQuestEditorFields() {
        float previousScroll = editorScroll;
        List<FormField> fields = new ArrayList<>();
        fields.add(field(trs("field.id_required"), questIdBox));
        fields.add(field(trs("field.name_required"), questNameBox));
        fields.add(field(trs("field.icon"), questIconBox));
        fields.add(field(trs("field.description"), questDescriptionBox));
        fields.add(field(trs("field.category_required"), questCategoryBox));
        if (!dropdownBoxValue(questCategoryBox).isBlank()) {
            fields.add(field(trs("field.subcategory"), questSubCategoryBox));
        } else if (questSubCategoryBox != null) {
            setDropdownBoxValue(questSubCategoryBox, "");
        }
        fields.add(field(trs("field.dependencies"), questDependenciesBox));
        fields.add(field(trs("field.flags"), questOptionalToggle));
        fields.add(field(trs("field.completion"), questCompletionBox));
        fields.add(field(trs("field.reward"), questRewardBox));
        setActiveFields(fields);
        editorScroll = previousScroll;
    }
    private void saveCurrent() {
        if (currentPack == null && editorType != EditorType.PACK_CREATE) return;
        statusMessage = "";
        statusColor = 0xA0A0A0;

        if (editorType == EditorType.PACK_CREATE) {
            savePackCreate();
            return;
        }
        if (editorType == EditorType.PACK_OPTIONS) {
            savePackOptions();
            return;
        }
        if (editorType == EditorType.CATEGORY) {
            saveCategory();
            return;
        }
        if (editorType == EditorType.SUBCATEGORY) {
            saveSubCategory();
            return;
        }
        if (editorType == EditorType.QUEST) {
            saveQuest();
        }
    }

    private void savePackCreate() {
        String name = safe(packNameBox.getValue()).trim();
        String namespace = namespaceFromPackName(name);
        if (name.isBlank()) {
            setError("Pack name required");
            return;
        }
        if (isInvalidPackFolderName(name)) {
            setError("Invalid pack name");
            return;
        }
        if (isInvalidNamespace(namespace)) {
            setError("Invalid namespace");
            return;
        }

        Path root = packsRoot().resolve(name);
        if (Files.exists(root)) {
            setError("Pack already exists");
            return;
        }

        try {
            Files.createDirectories(root.getParent());
            Files.createDirectories(root);
            String iconPath = normalizePackIconId(packIconPathBox.getValue());
            writePackMeta(root, name, "Boundless Quest Pack: " + name, iconPath, true);
            QuestPack pack = new QuestPack(name, namespace, root, false, true);
            initializeSingleFilePack(pack);
            currentPack = pack;
            setMode(Mode.PACK_MENU);
            stagePackChange(pack, "Pack staged");
        } catch (Exception e) {
            setError("Failed to create pack");
        }
    }

    private void savePackOptions() {
        if (currentPack == null) return;

        String requestedName = safe(packNameBox.getValue()).trim();
        String requestedNamespace = namespaceFromPackName(requestedName);
        if (requestedName.isBlank()) {
            setError("Pack name required");
            return;
        }
        if (isInvalidPackFolderName(requestedName)) {
            setError("Invalid pack name");
            return;
        }

        Path oldRoot = currentPack.root;
        String oldName = currentPack.name;
        String oldNamespace = currentPack.namespace;
        Path newRoot = oldRoot;
        if (!requestedName.equals(oldName)) {
            newRoot = packsRoot().resolve(requestedName);
            if (Files.exists(newRoot)) {
                setError("Pack already exists");
                return;
            }
        }

        try {
            if (!requestedName.equals(oldName)) {
                Files.move(oldRoot, newRoot);
            }
            if (!Objects.equals(currentPack.namespace, requestedNamespace) && !currentPack.namespace.isBlank()) {
                Path dataRoot = newRoot.resolve("data");
                Path oldNamespaceDir = dataRoot.resolve(currentPack.namespace);
                Path newNamespaceDir = dataRoot.resolve(requestedNamespace);
                if (Files.exists(oldNamespaceDir) && Files.exists(newNamespaceDir)) {
                    setError("Generated namespace already exists");
                    return;
                }
                if (Files.exists(oldNamespaceDir) && !Files.exists(newNamespaceDir)) {
                    Files.createDirectories(dataRoot);
                    Files.move(oldNamespaceDir, newNamespaceDir);
                }
            }
            String description = "Boundless Quest Pack: " + requestedName;
            String iconPath = normalizePackIconId(packIconPathBox.getValue());
            writePackMeta(newRoot, requestedName, description, iconPath, currentPack.enabled);
            currentPack = new QuestPack(requestedName, requestedNamespace, newRoot, currentPack.legacy, currentPack.enabled);
            currentPack.ensureDirs();
            selectedEntryId = currentPack.name;
            leftList.setSelectedId(selectedEntryId);
            if (!requestedName.equals(oldName)) {
            stagePackDeletion(new QuestPack(oldName, oldNamespace, oldRoot, currentPack.legacy, currentPack.enabled), "Pack options saved");
            }
            stagePackChange(currentPack, "Pack options saved");
            refreshLeftList();
            showPackOptions(currentPack);
            markCurrentEditorSaved();
        } catch (IOException e) {
            setError("Failed to save pack options");
        }
    }

    private void saveCategory() {
        String id = safe(catIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Category id required");
            return;
        }
        selectedEntryId = id;
        JsonObject obj = buildCategoryJson(id);
        if (obj == null) return;
        if (usesSingleFilePack(currentPack)) {
            saveSingleFileEntry(currentPack.categoriesFile, id, obj);
            return;
        }
        Path target = currentPack.categoriesDir.resolve(id + ".json");
        saveJson(obj, target, editingPath);
    }

    private void saveSubCategory() {
        String id = safe(subIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Sub-category id required");
            return;
        }
        selectedEntryId = id;
        JsonObject obj = buildSubCategoryJson(id);
        if (obj == null) return;
        if (usesSingleFilePack(currentPack)) {
            saveSingleFileEntry(currentPack.subCategoriesFile, id, obj);
            return;
        }

        Path target = currentPack.subCategoriesDir.resolve(id + ".json");
        saveJson(obj, target, editingPath);
    }

    private JsonObject buildCategoryJson(String id) {
        String categoryId = safe(id).trim();
        if (categoryId.isBlank()) {
            setError("Category id required");
            return null;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("id", categoryId);
        addOptional(obj, "name", catNameBox.getValue());
        addOptional(obj, "icon", catIconBox.getValue());
        obj.addProperty("order", resolveCategoryOrder(categoryId));
        addOptional(obj, "dependency", catDependencyBox.getValue());
        obj.addProperty("auto_complete", catAutoCompleteToggle.isOn());
        return obj;
    }

    private JsonObject buildSubCategoryJson(String id) {
        String subId = safe(id).trim();
        if (subId.isBlank()) {
            setError("Sub-category id required");
            return null;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("id", subId);
        addOptional(obj, "category", dropdownBoxValue(subCategoryBox));
        addOptional(obj, "name", subNameBox.getValue());
        addOptional(obj, "icon", subIconBox.getValue());
        obj.addProperty("order", resolveSubCategoryOrder(subId));
        obj.addProperty("default_open", subDefaultOpenToggle.isOn());
        return obj;
    }

    private int resolveCategoryOrder(String categoryId) {
        if (currentPack != null && usesSingleFilePack(currentPack)) {
            int existingSingle = readOrderFromSingleFileEntry(currentPack.categoriesFile, categoryId, -1);
            if (existingSingle >= 0) return existingSingle;
        }
        int existing = readOrderFromPath(editingPath, -1);
        if (existing >= 0) return existing;
        if (currentPack == null) return 0;
        int max = -1;
        for (NamedEntry entry : listCategoryEntries(currentPack)) {
            if (entry == null || entry.id == null || entry.id.equals(categoryId)) continue;
            int order = usesSingleFilePack(currentPack)
                    ? readOrderFromSingleFileEntry(currentPack.categoriesFile, entry.id, -1)
                    : readOrderFromPath(entry.path, -1);
            if (order > max) max = order;
        }
        return Math.max(0, max + 1);
    }

    private int resolveSubCategoryOrder(String subCategoryId) {
        if (currentPack != null && usesSingleFilePack(currentPack)) {
            int existingSingle = readOrderFromSingleFileEntry(currentPack.subCategoriesFile, subCategoryId, -1);
            if (existingSingle >= 0) return existingSingle;
        }
        int existing = readOrderFromPath(editingPath, -1);
        if (existing >= 0) return existing;
        if (currentPack == null) return 0;
        int max = -1;
        for (NamedEntry entry : listSubCategoryEntries(currentPack)) {
            if (entry == null || entry.id == null || entry.id.equals(subCategoryId)) continue;
            int order = usesSingleFilePack(currentPack)
                    ? readOrderFromSingleFileEntry(currentPack.subCategoriesFile, entry.id, -1)
                    : readOrderFromPath(entry.path, -1);
            if (order > max) max = order;
        }
        return Math.max(0, max + 1);
    }

    private int resolveQuestOrder(String questId) {
        if (currentPack == null || !usesSingleFilePack(currentPack)) return 0;
        int existing = readOrderFromSingleFileEntry(currentPack.questsFile, questId, -1);
        if (existing >= 0) return existing;
        int max = -1;
        for (NamedEntry entry : listQuestEntries(currentPack)) {
            if (entry == null || entry.id == null || entry.id.equals(questId)) continue;
            int order = readOrderFromSingleFileEntry(currentPack.questsFile, entry.id, -1);
            if (order > max) max = order;
        }
        return Math.max(0, max + 1);
    }

    private void saveQuest() {
        String id = safe(questIdBox.getValue()).trim();
        selectedEntryId = id;
        JsonObject obj = buildQuestJson(id);
        if (obj == null) return;
        if (usesSingleFilePack(currentPack)) {
            saveSingleFileEntry(currentPack.questsFile, id, obj);
            return;
        }

        String orderToken = questOrderTokenForSave(id);
        Path target = currentPack.questsDir.resolve(questFileBaseName(id, orderToken) + ".json");
        saveJson(obj, target, editingPath);
        questOrderToken = orderToken;
    }

    private JsonObject buildQuestJson(String id) {
        String questId = safe(id).trim();
        if (questId.isBlank()) {
            setError("Quest id required");
            return null;
        }
        if (isDuplicateQuestId(questId)) {
            setError("Quest id already exists");
            return null;
        }
        String nameRaw = safe(questNameBox.getValue()).trim();
        if (nameRaw.isBlank()) {
            setError("Quest name required");
            return null;
        }
        String categoryRaw = dropdownBoxValue(questCategoryBox);
        if (categoryRaw.isBlank()) {
            setError("Quest category required");
            return null;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("id", questId);
        addOptional(obj, "name", nameRaw);
        addOptional(obj, "icon", questIconBox.getValue());
        addOptional(obj, "description", questDescriptionBox.getValue());
        addOptional(obj, "category", categoryRaw);
        addOptional(obj, "sub-category", dropdownBoxValue(questSubCategoryBox));
        if (currentPack != null && usesSingleFilePack(currentPack)) {
            obj.addProperty("order", resolveQuestOrder(questId));
        }
        obj.addProperty("optional", questOptionalToggle.isOn());
        obj.addProperty("repeatable", questRepeatableToggle.isOn());
        obj.addProperty("auto_complete", questAutoCompleteToggle.isOn());
        obj.addProperty("hiddenUnderDependency", questHiddenUnderDependencyToggle.isOn());
        obj.addProperty("lock_after_dependency", dependencyLockState());
        if (loadedQuestType != null && !loadedQuestType.isBlank()) {
            obj.addProperty("type", loadedQuestType);
        }

        List<String> deps = collectDependencyEntries();
        if (deps.size() == 1) {
            obj.addProperty("dependencies", deps.get(0));
        } else if (!deps.isEmpty()) {
            var arr = new com.google.gson.JsonArray();
            for (String dep : deps) arr.add(dep);
            obj.add("dependencies", arr);
        }

        String completionRaw = safe(questCompletionBox.getValue()).trim();
        if (!completionRaw.isBlank()) {
            JsonObject completion = parseCompletionEntries(completionRaw, true);
            if (completion == null) {
                return null;
            }
            obj.add("completion", completion);
        }

        String rewardRaw = safe(questRewardBox.getValue()).trim();
        if (!rewardRaw.isBlank()) {
            JsonObject reward = parseRewardEntries(rewardRaw, true);
            if (reward == null) {
                return null;
            }
            obj.add("reward", reward);
        }

        return obj;
    }

    private void saveJson(JsonObject obj, Path target, Path original) {
        try {
            Files.createDirectories(target.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                gson.toJson(obj, writer);
            }
            if (original != null && !original.equals(target)) {
                Files.deleteIfExists(original);
            }
            editingPath = target;
            markCurrentEditorSaved();
            stageCurrentPackChange("Saved to staging");
            refreshLeftList();
            QuestPanelClient.applyConfigChanges();
        } catch (IOException e) {
            setError("Save failed: " + safe(e.getMessage()));
        }
    }

    private void saveSingleFileEntry(Path file, String id, JsonObject obj) {
        if (currentPack == null || file == null || obj == null) return;
        try {
            List<JsonObject> entries = readSingleFileObjects(file);
            boolean replaced = false;
            for (int i = 0; i < entries.size(); i++) {
                JsonObject existing = entries.get(i);
                String existingId = safe(optString(existing, "id", "")).trim();
                if (!existingId.equals(id)) continue;
                entries.set(i, obj);
                replaced = true;
                break;
            }
            if (!replaced) entries.add(obj);
            writeSingleFileObjects(file, entries);
            editingPath = file;
            markCurrentEditorSaved();
            stageCurrentPackChange("Saved to staging");
            refreshLeftList();
            QuestPanelClient.applyConfigChanges();
        } catch (IOException e) {
            setError("Save failed: " + safe(e.getMessage()));
        }
    }

    private void duplicateCurrentPackAsIs() {
        if (currentPack == null) return;
        try {
            if (!ensurePackWorkspace(currentPack)) {
                setError("Failed to open pack");
                return;
            }
            PackMeta meta = readPackMeta(currentPack.root, currentPack.name);
            String duplicateName = nextAvailablePackName(currentPack.name);
            Path target = packsRoot().resolve(duplicateName);
            mirrorDirectory(currentPack.root, target);
        writePackMeta(target, duplicateName, safe(meta.description), safe(meta.iconPath), meta.enabled);
        QuestPack duplicated = new QuestPack(duplicateName, currentPack.namespace, target, false, currentPack.enabled);
            duplicated.ensureDirs();
            currentPack = duplicated;
            selectedEntryId = duplicated.name;
            stagePackChange(duplicated, "Pack duplicated");
            refreshLeftList();
            leftList.setSelectedId(selectedEntryId);
            showPackOptions(duplicated);
            markCurrentEditorSaved();
        } catch (IOException e) {
            setError("Pack duplication failed");
        }
    }

    private void openPackDirectory() {
        try {
            if (currentPack == null || !ensurePackWorkspace(currentPack)) {
                setError("Failed to open pack");
                return;
            }
            Path folder = currentPack.root;
            Files.createDirectories(folder);
            Util.getPlatform().openFile(folder.toFile());
        } catch (Exception e) {
            setError("Failed to open folder");
        }
    }

    private void openImportQuestPackDirectory() {
        Path importRoot = modDataQuestPacksRoot();
        try {
            Files.createDirectories(importRoot);
            Util.getPlatform().openFile(importRoot.toFile());
        statusMessage = trs("status.place_questpacks");
            statusColor = 0xA0A0A0;
        } catch (Exception e) {
            setError("Failed to open import directory");
        }
    }

    private void duplicateQuest() {
        if (currentPack == null || editorType != EditorType.QUEST) return;
        String baseId = safe(questIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError("Quest id required");
            return;
        }

        String newId = nextAvailableQuestId(baseId);
        JsonObject obj = buildQuestJson(newId);
        if (obj == null) return;

        selectedEntryId = newId;
        if (usesSingleFilePack(currentPack)) {
            saveSingleFileEntry(currentPack.questsFile, newId, obj);
        } else {
            String orderToken = nextQuestOrderToken();
            Path target = currentPack.questsDir.resolve(questFileBaseName(newId, orderToken) + ".json");
            saveJson(obj, target, null);
        }
        QuestEntryData data = loadQuest(currentPack, newId);
        if (data != null) {
            showQuestEditor(data, data.path);
        }
    }

    private void duplicateCategory() {
        if (currentPack == null || editorType != EditorType.CATEGORY) return;
        String baseId = safe(catIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError("Category id required");
            return;
        }
        String newId = nextAvailableCategoryId(baseId);
        JsonObject obj = buildCategoryJson(newId);
        if (obj == null) return;
        selectedEntryId = newId;
        if (usesSingleFilePack(currentPack)) saveSingleFileEntry(currentPack.categoriesFile, newId, obj);
        else saveJson(obj, currentPack.categoriesDir.resolve(newId + ".json"), null);
        CategoryData data = loadCategory(currentPack, newId);
        if (data != null) showCategoryEditor(data, data.path);
    }

    private void duplicateSubCategory() {
        if (currentPack == null || editorType != EditorType.SUBCATEGORY) return;
        String baseId = safe(subIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError("Sub-category id required");
            return;
        }
        String newId = nextAvailableSubCategoryId(baseId);
        JsonObject obj = buildSubCategoryJson(newId);
        if (obj == null) return;
        selectedEntryId = newId;
        if (usesSingleFilePack(currentPack)) saveSingleFileEntry(currentPack.subCategoriesFile, newId, obj);
        else saveJson(obj, currentPack.subCategoriesDir.resolve(newId + ".json"), null);
        SubCategoryData data = loadSubCategory(currentPack, newId);
        if (data != null) showSubCategoryEditor(data, data.path);
    }

    private void duplicateCurrent() {
        if (mode == Mode.PACK_MENU && currentPack != null) {
            duplicateCurrentPackAsIs();
            return;
        }
        switch (editorType) {
            case QUEST -> duplicateQuest();
            case CATEGORY -> duplicateCategory();
            case SUBCATEGORY -> duplicateSubCategory();
            case PACK_OPTIONS -> duplicateCurrentPackAsIs();
            default -> {}
        }
    }

    private void deleteQuest() {
        if (currentPack == null || editorType != EditorType.QUEST) return;
        String id = safe(questIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Quest id required");
            return;
        }

        if (usesSingleFilePack(currentPack)) {
            deleteSingleFileEntry(currentPack.questsFile, id);
            return;
        }
        Path target = editingPath != null ? editingPath : currentPack.questsDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(target);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            if (deleted) {
                stageCurrentPackChange("Deletion staged");
            } else {
                statusMessage = "Nothing to delete";
                statusColor = 0xA0A0A0;
            }
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private void deleteCategory() {
        if (currentPack == null || editorType != EditorType.CATEGORY) return;
        String id = safe(catIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Category id required");
            return;
        }

        if (usesSingleFilePack(currentPack)) {
            deleteSingleFileEntry(currentPack.categoriesFile, id);
            return;
        }
        Path target = editingPath != null ? editingPath : currentPack.categoriesDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(target);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            if (deleted) {
                stageCurrentPackChange("Deletion staged");
            } else {
                statusMessage = "Nothing to delete";
                statusColor = 0xA0A0A0;
            }
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private void deleteSubCategory() {
        if (currentPack == null || editorType != EditorType.SUBCATEGORY) return;
        String id = safe(subIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Sub-category id required");
            return;
        }

        if (usesSingleFilePack(currentPack)) {
            deleteSingleFileEntry(currentPack.subCategoriesFile, id);
            return;
        }
        Path target = editingPath != null ? editingPath : currentPack.subCategoriesDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(target);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            if (deleted) {
                stageCurrentPackChange("Deletion staged");
            } else {
                statusMessage = "Nothing to delete";
                statusColor = 0xA0A0A0;
            }
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private void deleteCurrent() {
        if (mode == Mode.PACK_MENU && currentPack != null) {
            deletePack(currentPack);
            return;
        }
        switch (editorType) {
            case QUEST -> deleteQuest();
            case CATEGORY -> deleteCategory();
            case SUBCATEGORY -> deleteSubCategory();
            case PACK_OPTIONS -> deletePack(resolveCurrentPackForDelete());
            default -> {}
        }
    }

    private QuestPack resolveCurrentPackForDelete() {
        if (currentPack != null) return currentPack;
        String selected = safe(selectedEntryId).trim();
        if (!selected.isBlank()) {
            QuestPack bySelected = findPackByName(selected);
            if (bySelected != null) return bySelected;
        }
        if (packNameBox != null) {
            String byName = safe(packNameBox.getValue()).trim();
            if (!byName.isBlank()) return findPackByName(byName);
        }
        return null;
    }

    private void handleDeleteButtonPress() {
        if (mode != Mode.PACK_MENU
                && editorType != EditorType.QUEST
                && editorType != EditorType.CATEGORY
                && editorType != EditorType.SUBCATEGORY
                && editorType != EditorType.PACK_OPTIONS) {
            disarmDeleteConfirm();
            return;
        }
        if (!deleteConfirmArmed) {
            deleteConfirmArmed = true;
            updateDeleteButtonTexture();
            statusMessage = trs("status.confirm_delete");
            statusColor = 0xFFD080;
            return;
        }
        disarmDeleteConfirm();
        deleteCurrent();
    }

    private void disarmDeleteConfirm() {
        if (!deleteConfirmArmed) return;
        deleteConfirmArmed = false;
        updateDeleteButtonTexture();
    }

    private void updateDeleteButtonTexture() {
        if (deleteQuestButton != null) {
            if (deleteConfirmArmed) {
                deleteQuestButton.setTextures(DELETE_CONFIRM_TEX, DELETE_CONFIRM_TEX_HOVER);
            } else {
                deleteQuestButton.setTextures(DELETE_TEX, DELETE_CONFIRM_TEX_HOVER);
            }
        }
    }

    private String nextAvailableQuestId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        if (usesSingleFilePack(currentPack)) {
            Set<String> ids = new HashSet<>();
            for (NamedEntry entry : listQuestEntries(currentPack)) ids.add(safe(entry.id));
            return nextAvailableId(base, ids);
        }
        return nextAvailableId(base, currentPack.questsDir);
    }

    private String nextAvailableCategoryId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        if (usesSingleFilePack(currentPack)) {
            Set<String> ids = new HashSet<>();
            for (NamedEntry entry : listCategoryEntries(currentPack)) ids.add(safe(entry.id));
            return nextAvailableId(base, ids);
        }
        return nextAvailableId(base, currentPack.categoriesDir);
    }

    private String nextAvailableSubCategoryId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        if (usesSingleFilePack(currentPack)) {
            Set<String> ids = new HashSet<>();
            for (NamedEntry entry : listSubCategoryEntries(currentPack)) ids.add(safe(entry.id));
            return nextAvailableId(base, ids);
        }
        return nextAvailableId(base, currentPack.subCategoriesDir);
    }

    private String nextAvailablePackName(String baseName) {
        String base = safe(baseName).trim();
        if (base.isBlank()) return "New Pack";
        Set<String> existing = new HashSet<>();
        for (QuestPack pack : listPacks()) {
            if (pack != null && pack.name != null && !pack.name.isBlank()) {
                existing.add(pack.name);
            }
        }
        if (!existing.contains(base) && !Files.exists(packsRoot().resolve(base))) {
            return base;
        }
        String copyBase = base + " Copy";
        if (!existing.contains(copyBase) && !Files.exists(packsRoot().resolve(copyBase))) {
            return copyBase;
        }
        int index = 2;
        while (true) {
            String candidate = copyBase + " " + index;
            if (!existing.contains(candidate) && !Files.exists(packsRoot().resolve(candidate))) {
                return candidate;
            }
            index++;
        }
    }

    private String nextAvailableId(String baseId, Path dir) {
        String base = safe(baseId).trim();
        if (base.isBlank() || dir == null) return baseId;
        String candidate = base + "_copy";
        int counter = 2;
        while (Files.exists(dir.resolve(candidate + ".json"))) {
            candidate = base + "_copy" + counter;
            counter++;
        }
        return candidate;
    }

    private String nextAvailableId(String baseId, Set<String> existingIds) {
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        Set<String> existing = existingIds == null ? Set.of() : existingIds;
        String candidate = base + "_copy";
        int counter = 2;
        while (existing.contains(candidate)) {
            candidate = base + "_copy" + counter;
            counter++;
        }
        return candidate;
    }

    private void deletePack(QuestPack pack) {
        if (pack == null) return;
        try {
            if (Files.exists(pack.root)) {
                deleteDirectory(pack.root);
            }
            currentPack = null;
            selectedEntryId = "";
            setMode(Mode.PACK_LIST);
            stagePackDeletion(pack, "Pack deletion staged");
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private JsonElement parseJsonSilent(String raw) {
        try {
            return JsonParser.parseString(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonObject parseCompletionEntries(String raw, boolean raiseErrors) {
        List<String> lines = extractEntryLines(raw);
        com.google.gson.JsonArray targets = new com.google.gson.JsonArray();
        for (String line : lines) {
            JsonObject target = parseCompletionEntryLine(line, raiseErrors);
            if (target == null) return null;
            targets.add(target);
        }
        JsonObject wrapper = new JsonObject();
        wrapper.add("complete", targets);
        return wrapper;
    }

    private JsonObject parseCompletionEntryLine(String line, boolean raiseErrors) {
        ParsedEntry parsed = parseEntry(line);
        if (parsed == null) {
            if (raiseErrors) setError("Invalid completion entry: " + safe(line));
            return null;
        }
        String type = parsed.type;
        String id = parsed.id;
        int count = parsed.count;

        JsonObject obj = new JsonObject();
        switch (type) {
            case "collect", "item" -> {
                String normalizedId = normalizeNamespacedId(id, false);
                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors);
                obj.addProperty("collect", normalizedId);
                obj.addProperty("count", count);
            }
            case "submit" -> {
                String normalizedId = normalizeNamespacedId(id, false);
                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors);
                obj.addProperty("submit", normalizedId);
                obj.addProperty("count", count);
            }
            case "kill", "entity" -> {
                String normalizedId = normalizeNamespacedId(id, false);
                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors);
                obj.addProperty("kill", normalizedId);
                obj.addProperty("count", count);
            }
            case "achieve", "advancement" -> {
                String normalizedId = normalizeNamespacedId(id, false);
                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors);
                obj.addProperty("achieve", normalizedId);
            }
            case "effect" -> {
                String normalizedId = normalizeNamespacedId(id, false);
                if (normalizedId.isBlank()) return failCompletion(line, raiseErrors);
                obj.addProperty("effect", normalizedId);
            }
            case "xp" -> {
                String mode = safe(id).trim().toLowerCase(Locale.ROOT);
                if (!mode.equals("points") && !mode.equals("levels")) return failCompletion(line, raiseErrors);
                obj.addProperty("xp", mode);
                obj.addProperty("count", count);
            }
            case "levelup" -> {
                String mode = safe(id).trim().toLowerCase(Locale.ROOT);
                if (!mode.equals("level")) return failCompletion(line, raiseErrors);
                obj.addProperty("levelup_level", count);
            }
            case "field", "input" -> {
                String expected = safe(id).trim();
                if (expected.isBlank()) return failCompletion(line, raiseErrors);
                obj.addProperty("field", expected);
                if (parsed.hint != null && !parsed.hint.isBlank()) {
                    obj.addProperty("field_text", parsed.hint.trim());
                }
            }
            default -> {
                return failCompletion(line, raiseErrors);
            }
        }
        return obj;
    }

    private JsonObject failCompletion(String line, boolean raiseErrors) {
        if (raiseErrors) setError("Invalid completion entry: " + safe(line));
        return null;
    }

    private JsonObject parseRewardEntries(String raw, boolean raiseErrors) {
        List<String> lines = extractEntryLines(raw);
        com.google.gson.JsonArray items = new com.google.gson.JsonArray();
        com.google.gson.JsonArray commands = new com.google.gson.JsonArray();
        String expType = "";
        int expAmount = 0;

        for (String line : lines) {
            ParsedEntry parsed = parseEntry(line);
            if (parsed == null) {
                if (raiseErrors) setError("Invalid reward entry: " + safe(line));
                return null;
            }
            switch (parsed.type) {
                case "item", "submit" -> {
                    String normalizedId = normalizeNamespacedId(parsed.id, false);
                    if (normalizedId.isBlank()) return failReward(line, raiseErrors);
                    JsonObject item = new JsonObject();
                    item.addProperty("item", normalizedId);
                    item.addProperty("count", parsed.count);
                    items.add(item);
                }
                case "xp", "exp" -> {
                    String v = parsed.id.toLowerCase(Locale.ROOT);
                    if (v.isBlank()) v = "points";
                    if (!v.equals("points") && !v.equals("levels") && !v.equals("levelup")) return failReward(line, raiseErrors);
                    expType = v;
                    expAmount = parsed.count;
                }
                case "command" -> {
                    CommandReward parsedCommand = parseCommandReward(parsed.id);
                    if (parsedCommand.command.isBlank()) return failReward(line, raiseErrors);
                    JsonObject cmd = new JsonObject();
                    String commandValue = parsedCommand.command.startsWith("/")
                            ? parsedCommand.command.substring(1).trim()
                            : parsedCommand.command;
                    if (commandValue.isBlank()) return failReward(line, raiseErrors);
                    cmd.addProperty("command", commandValue);
                    if (!parsedCommand.icon.isBlank()) {
                        String normalizedIcon = normalizeNamespacedId(parsedCommand.icon, false);
                        if (normalizedIcon.isBlank()) return failReward(line, raiseErrors);
                        cmd.addProperty("icon", normalizedIcon);
                    }
                    cmd.addProperty("title", safe(parsedCommand.title));
                    commands.add(cmd);
                }
                case "loot", "loottable" -> {
                    CommandReward parsedLoot = parseCommandReward(parsed.id);
                    String lootTableId = normalizeNamespacedId(parsedLoot.command, false);
                    if (lootTableId.isBlank()) return failReward(line, raiseErrors);
                    JsonObject cmd = new JsonObject();
                    cmd.addProperty("command", "loot give @s loot " + lootTableId);
                    if (!parsedLoot.icon.isBlank()) {
                        String normalizedIcon = normalizeNamespacedId(parsedLoot.icon, false);
                        if (normalizedIcon.isBlank()) return failReward(line, raiseErrors);
                        cmd.addProperty("icon", normalizedIcon);
                    } else {
                        cmd.addProperty("icon", "minecraft:chest");
                    }
                    cmd.addProperty("title", safe(parsedLoot.title).isBlank() ? lootTableId : safe(parsedLoot.title));
                    commands.add(cmd);
                }
                default -> {
                    return failReward(line, raiseErrors);
                }
            }
        }

        JsonObject out = new JsonObject();
        if (!items.isEmpty()) out.add("items", items);
        if (!commands.isEmpty()) out.add("commands", commands);
        if (!expType.isBlank()) {
            out.addProperty("exp", expType);
            out.addProperty("count", expAmount);
        }
        return out;
    }

    private JsonObject failReward(String line, boolean raiseErrors) {
        if (raiseErrors) setError("Invalid reward entry: " + safe(line));
        return null;
    }

    private List<String> extractEntryLines(String raw) {
        List<String> lines = new ArrayList<>();
        if (raw == null || raw.isBlank()) return lines;
        String[] parts = raw.split("\\R");
        for (String part : parts) {
            String line = safe(part).trim();
            if (!line.isBlank()) lines.add(line);
        }
        return lines;
    }

    private String completionJsonToEntries(String json) {
        JsonElement el = parseJsonSilent(json);
        if (el == null || el.isJsonNull()) return "";
        List<String> out = new ArrayList<>();
        parseCompletionElementToLines(el, out);
        return String.join("\n", out);
    }

    private void parseCompletionElementToLines(JsonElement el, List<String> out) {
        if (el == null || out == null) return;
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("complete") && obj.get("complete").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("complete")) {
                    parseCompletionElementToLines(e, out);
                }
                return;
            }
            if (obj.has("targets") && obj.get("targets").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("targets")) {
                    parseCompletionElementToLines(e, out);
                }
                return;
            }
            if (obj.has("kind") && obj.has("id")) {
                String kind = optString(obj, "kind", "").toLowerCase(Locale.ROOT);
                String id = optString(obj, "id", "");
                int count = parseIntFlexible(obj, "count", 1);
                switch (kind) {
                    case "item" -> out.add("collect: " + id + " " + count);
                    case "submit" -> out.add("submit: " + id + " " + count);
                    case "entity" -> out.add("kill: " + id + " " + count);
                    case "advancement" -> out.add("achieve: " + id);
                    case "effect" -> out.add("effect: " + id);
                    case "stat" -> out.add("stat: " + id + " " + count);
                    case "xp" -> out.add("xp: " + id + " " + count);
                    case "levelup_level" -> out.add("levelup: level " + count);
                    case "field" -> {
                        String hint = optString(obj, "hint", "");
                        String escapedValue = id.replace("\\", "\\\\").replace("\"", "\\\"");
                        String escapedHint = hint.replace("\\", "\\\\").replace("\"", "\\\"");
                        out.add("field: \"" + escapedValue + "\" \"" + escapedHint + "\"");
                    }
                }
                return;
            }
            if (obj.has("collect")) {
                JsonElement collect = obj.get("collect");
                int count = parseIntFlexible(obj, "count", 1);
                if (collect.isJsonArray()) {
                    for (JsonElement c : collect.getAsJsonArray()) {
                        if (c.isJsonPrimitive()) out.add("collect: " + c.getAsString() + " " + count);
                    }
                } else if (collect.isJsonPrimitive()) {
                    out.add("collect: " + collect.getAsString() + " " + count);
                }
                return;
            }
            if (obj.has("item")) out.add("collect: " + optString(obj, "item", "") + " " + parseIntFlexible(obj, "count", 1));
            else if (obj.has("submit")) out.add("submit: " + optString(obj, "submit", "") + " " + parseIntFlexible(obj, "count", 1));
            else if (obj.has("kill")) out.add("kill: " + optString(obj, "kill", "") + " " + parseIntFlexible(obj, "count", 1));
            else if (obj.has("entity")) out.add("kill: " + optString(obj, "entity", "") + " " + parseIntFlexible(obj, "count", 1));
            else if (obj.has("achieve")) out.add("achieve: " + optString(obj, "achieve", ""));
            else if (obj.has("advancement")) out.add("achieve: " + optString(obj, "advancement", ""));
            else if (obj.has("effect")) out.add("effect: " + optString(obj, "effect", ""));
            else if (obj.has("stat")) out.add("stat: " + optString(obj, "stat", "") + " " + parseIntFlexible(obj, "count", 1));
            else if (obj.has("xp")) out.add("xp: " + optString(obj, "xp", "points") + " " + parseIntFlexible(obj, "count", 1));
            else if (obj.has("levelup_level")) out.add("levelup: level " + parseIntFlexible(obj, "levelup_level", 1));
            else if (obj.has("field")) {
                String value = optString(obj, "field", "");
                String hint = optString(obj, "field_text", optString(obj, "fieldText", ""));
                String escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"");
                String escapedHint = hint.replace("\\", "\\\\").replace("\"", "\\\"");
                out.add("field: \"" + escapedValue + "\" \"" + escapedHint + "\"");
            }
            return;
        }
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                parseCompletionElementToLines(e, out);
            }
        }
    }

    private String rewardJsonToEntries(String json) {
        JsonElement el = parseJsonSilent(json);
        if (el == null || !el.isJsonObject()) return "";
        JsonObject obj = el.getAsJsonObject();
        List<String> out = new ArrayList<>();
        if (obj.has("items") && obj.get("items").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("items")) {
                if (!e.isJsonObject()) continue;
                JsonObject item = e.getAsJsonObject();
                String id = optString(item, "item", "");
                int count = parseIntFlexible(item, "count", 1);
                if (!id.isBlank()) out.add("item: " + id + " " + count);
            }
        }
        if (obj.has("commands") && obj.get("commands").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("commands")) {
                if (e.isJsonPrimitive()) {
                    String cmd = e.getAsString();
                    if (!cmd.isBlank()) out.add("command: " + cmd);
                    continue;
                }
                if (!e.isJsonObject()) continue;
                JsonObject cmdObj = e.getAsJsonObject();
                String cmd = optString(cmdObj, "command", "");
                if (cmd.isBlank()) continue;
                String icon = optString(cmdObj, "icon", "");
                String title = optString(cmdObj, "title", "");
                String lootPrefix = "loot give @s loot ";
                if (cmd.startsWith(lootPrefix)) {
                    StringBuilder line = new StringBuilder("loot: ").append(cmd.substring(lootPrefix.length()).trim());
                    if (!icon.isBlank() && !"minecraft:chest".equals(icon)) line.append(" | icon: ").append(icon);
                    if (!title.isBlank() && !title.equals(cmd.substring(lootPrefix.length()).trim())) line.append(" | title: ").append(title);
                    out.add(line.toString());
                } else {
                    StringBuilder line = new StringBuilder("command: ").append(cmd);
                    if (!icon.isBlank()) line.append(" | icon: ").append(icon);
                    line.append(" | title: ").append(title);
                    out.add(line.toString());
                }
            }
        }
        if (obj.has("lootTables") && obj.get("lootTables").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("lootTables")) {
                if (!e.isJsonObject()) continue;
                JsonObject lootObj = e.getAsJsonObject();
                String lootTable = optString(lootObj, "lootTable", "");
                if (lootTable.isBlank()) continue;
                String icon = optString(lootObj, "icon", "");
                String title = optString(lootObj, "title", "");
                StringBuilder line = new StringBuilder("loot: ").append(lootTable);
                if (!icon.isBlank()) line.append(" | icon: ").append(icon);
                line.append(" | title: ").append(title);
                out.add(line.toString());
            }
        }
        String exp = optString(obj, "exp", "");
        if (!exp.isBlank()) {
            int count = parseIntFlexible(obj, "count", 0);
            out.add("xp: " + exp + " " + count);
        }
        return String.join("\n", out);
    }

    private int parseIntFlexible(JsonObject obj, String key, int def) {
        if (obj == null || !obj.has(key)) return def;
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) return def;
        try {
            return el.getAsInt();
        } catch (Exception ignored) {
            return def;
        }
    }

    private ParsedEntry parseEntry(String line) {
        if (line == null) return null;
        int colon = line.indexOf(':');
        if (colon <= 0) return null;
        String type = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        String remainder = line.substring(colon + 1).trim();
        if (type.isBlank() || remainder.isBlank()) return null;

        if (type.equals("command")) {
            return new ParsedEntry(type, remainder, 1);
        }
        if (type.equals("field") || type.equals("input")) {
            List<String> quoted = parseQuotedSegments(remainder);
            if (quoted.size() < 2) return null;
            String expected = quoted.get(0).trim();
            String hint = quoted.get(1).trim();
            if (expected.isBlank()) return null;
            return new ParsedEntry(type, expected, 1, hint);
        }

        String[] tokens = remainder.split("\\s+");
        if (tokens.length == 0) return null;
        String id = tokens[0].trim();
        int count = 1;
        if (tokens.length >= 2) {
            String last = tokens[tokens.length - 1].trim();
            try {
                count = Integer.parseInt(last);
                if (tokens.length > 2) {
                    StringBuilder idBuilder = new StringBuilder();
                    for (int i = 0; i < tokens.length - 1; i++) {
                        if (i > 0) idBuilder.append(' ');
                        idBuilder.append(tokens[i]);
                    }
                    id = idBuilder.toString();
                }
            } catch (NumberFormatException ignored) {
                if (tokens.length > 1) {
                    StringBuilder idBuilder = new StringBuilder();
                    for (int i = 0; i < tokens.length; i++) {
                        if (i > 0) idBuilder.append(' ');
                        idBuilder.append(tokens[i]);
                    }
                    id = idBuilder.toString();
                }
            }
        }
        if (count < 1) count = 1;
        return new ParsedEntry(type, id.trim(), count);
    }

    private List<String> parseQuotedSegments(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        boolean escaping = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaping) {
                current.append(ch);
                escaping = false;
                continue;
            }
            if (ch == '\\') {
                escaping = true;
                continue;
            }
            if (ch == '"') {
                if (inQuote) {
                    out.add(current.toString());
                    current.setLength(0);
                }
                inQuote = !inQuote;
                continue;
            }
            if (inQuote) current.append(ch);
        }
        return out;
    }

    private CommandReward parseCommandReward(String raw) {
        String payload = safe(raw).trim();
        if (payload.isBlank()) return new CommandReward("", "", "");

        String command = "";
        String icon = "";
        String title = "";
        String[] segments = payload.split("\\|");
        for (int i = 0; i < segments.length; i++) {
            String segment = safe(segments[i]).trim();
            if (segment.isBlank()) continue;
            String lower = segment.toLowerCase(Locale.ROOT);
            if (lower.startsWith("command:")) {
                command = segment.substring("command:".length()).trim();
            } else if (lower.startsWith("icon:")) {
                icon = segment.substring("icon:".length()).trim();
            } else if (lower.startsWith("title:")) {
                title = segment.substring("title:".length()).trim();
            } else if (i == 0 && command.isBlank()) {
                command = segment;
            }
        }
        icon = unquotePlaceholder(icon);
        title = unquotePlaceholder(title);
        return new CommandReward(command, icon, title);
    }

    private String unquotePlaceholder(String value) {
        String trimmed = safe(value).trim();
        if (trimmed.equals("\"\"")) return "";
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private String normalizeNamespacedId(String raw, boolean allowTags) {
        String id = safe(raw).trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) return "";
        if (allowTags && id.startsWith("#")) {
            String rest = id.substring(1).trim();
            if (rest.isBlank()) return "";
            return rest.contains(":") ? "#" + rest : "#minecraft:" + rest;
        }
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private boolean applyChanges() {
        ScreenState state = captureState();
        if (state != null) {
            pendingState = state;
            stashPendingInitState(state);
        }
        boolean mirrored = mirrorCurrentPackDirectorySafe();
        Minecraft.getInstance().execute(() -> {
            if (pendingState != null) {
                ScreenState restore = pendingState;
                pendingState = null;
                restoreState(restore);
            }
            QuestData.loadClient(true);
        });
        return mirrored;
    }

    private void stageCurrentPackChange(String message) {
        stagePackChange(currentPack, message);
    }

    private void stagePackChange(QuestPack pack, String message) {
        if (pack == null || pack.name == null || pack.name.isBlank()) return;
        stagedDeletedPackNames.remove(pack.name);
        stagedPacks.put(pack.name, pack);
        statusMessage = message;
        statusColor = 0xA0FFA0;
    }

    private void stagePackDeletion(QuestPack pack, String message) {
        if (pack == null || pack.name == null || pack.name.isBlank()) return;
        stagedPacks.remove(pack.name);
        stagedDeletedPackNames.add(pack.name);
        statusMessage = message;
        statusColor = 0xA0FFA0;
    }

    private boolean hasStagedChanges() {
        return !stagedPacks.isEmpty() || !stagedDeletedPackNames.isEmpty();
    }

    private void applyStagedChangesOnClose() {
        if (!hasStagedChanges()) return;
        Minecraft mc = Minecraft.getInstance();
        QuestPack selectedPack = currentPack;
        boolean changed = false;

        for (String packName : new ArrayList<>(stagedDeletedPackNames)) {
            changed |= deleteAppliedPackArtifactsSafe(packName);
        }
        for (QuestPack pack : new ArrayList<>(stagedPacks.values())) {
            if (pack == null) continue;
            currentPack = pack;
            changed |= mirrorCurrentPackDirectorySafe();
            if (selectedPack == null) {
                selectedPack = pack;
            }
        }

        currentPack = selectedPack;
        stagedPacks.clear();
        stagedDeletedPackNames.clear();
        if (!changed) return;

        mc.execute(() -> QuestData.loadClient(true));
    }

    private boolean deleteAppliedPackArtifactsSafe(String packName) {
        String normalizedName = safe(packName).trim();
        if (normalizedName.isBlank()) return false;

        boolean changed = false;
        try {
            Path packRoot = packsRoot().resolve(normalizedName);
            if (Files.exists(packRoot)) {
                deleteDirectory(packRoot);
                changed = true;
            }
        } catch (IOException ignored) {
        }

        try {
            Path legacyZip = resourcePacksRoot().resolve(normalizedName + ".zip");
            if (Files.exists(legacyZip) && Files.deleteIfExists(legacyZip)) {
                changed = true;
            }
        } catch (IOException ignored) {
        }

        try {
            Path legacyDir = resourcePacksRoot().resolve("boundless").resolve(normalizedName);
            if (Files.exists(legacyDir)) {
                deleteDirectory(legacyDir);
                changed = true;
            }
        } catch (IOException ignored) {
        }

        return changed;
    }

    private boolean mirrorCurrentPackDirectorySafe() {
        if (currentPack == null || currentPack.root == null) return false;
        try {
            Path targetRoot = packsRoot().resolve(currentPack.name);
            Path sourceReal = currentPack.root.toRealPath();
            Path targetReal = Files.exists(targetRoot) ? targetRoot.toRealPath() : targetRoot.toAbsolutePath().normalize();
            if (sourceReal.equals(targetReal)) {
                return false;
            }
            mirrorDirectory(currentPack.root, targetRoot);
            return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    private void mirrorDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        if (sourceRoot == null || targetRoot == null) return;
        if (!Files.isDirectory(sourceRoot)) throw new IOException("Source pack folder missing");
        Path sourceReal = sourceRoot.toRealPath();
        Path targetReal = Files.exists(targetRoot) ? targetRoot.toRealPath() : targetRoot.toAbsolutePath().normalize();
        if (sourceReal.equals(targetReal)) {
            return;
        }
        if (Files.exists(targetRoot) && !Files.isDirectory(targetRoot)) {
            Files.deleteIfExists(targetRoot);
        }
        if (Files.exists(targetRoot)) {
            deleteDirectory(targetRoot);
        }
        Files.createDirectories(targetRoot);
        try (var walk = Files.walk(sourceRoot)) {
            for (Path src : (Iterable<Path>) walk::iterator) {
                Path rel = sourceRoot.relativize(src);
                if (rel.toString().isEmpty()) continue;
                Path dst = targetRoot.resolve(rel.toString());
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dst);
                } else {
                    Path parent = dst.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void exportCurrentPack() {
        if (currentPack == null || currentPack.root == null || !Files.isDirectory(currentPack.root)) {
            setError("No questpack to export");
            return;
        }
        Path exportRoot = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("boundless")
                .resolve("exports");
        try {
            Files.createDirectories(exportRoot);
            String base = safe(currentPack.name).isBlank() ? "questpack" : currentPack.name;
            Path zipPath = exportRoot.resolve(base + ".zip");
            int suffix = 1;
            while (Files.exists(zipPath)) {
                zipPath = exportRoot.resolve(base + "-" + suffix + ".zip");
                suffix++;
            }
            zipDirectory(currentPack.root, zipPath);
            statusMessage = "Exported: " + zipPath.getFileName();
            statusColor = 0xA0FFA0;
            Util.getPlatform().openFile(exportRoot.toFile());
        } catch (Exception e) {
            setError("Failed to export questpack");
        }
    }

    private void zipDirectory(Path sourceRoot, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            try (var walk = Files.walk(sourceRoot)) {
                for (Path src : (Iterable<Path>) walk::iterator) {
                    if (Files.isDirectory(src)) continue;
                    Path rel = sourceRoot.relativize(src);
                    String entryName = rel.toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(src, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private String invokeString(Object target, String... methods) {
        Object result = invokeObject(target, methods);
        return result instanceof String s ? s : null;
    }

    private Object invokeObject(Object target, String... methods) {
        if (target == null || methods == null) return null;
        for (String method : methods) {
            if (method == null || method.isBlank()) continue;
            try {
                Method m = target.getClass().getMethod(method);
                Object value = m.invoke(target);
                if (value != null) return value;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void clearEditor() {
        closeTransientMenus();
        editorType = EditorType.NONE;
        editingPath = null;
        loadedQuestType = "";
        disarmDeleteConfirm();
        clearPendingDiscardState();
        savedEditorState = null;
        setActiveFields(List.of());
        saveButton.visible = false;
        saveButton.active = false;
        if (exportPackButton != null) {
            exportPackButton.visible = false;
            exportPackButton.active = false;
        }
        updateBackButtonVisibility();
    }

    private void closeTransientMenus() {
        openTypeMenuKind = null;
        openTypeMenuRow = -1;
        openTypeMenuScroll = 0;
        openDropdownTarget = null;
        openDropdownScroll = 0;
        if (itemPickerKind != null || itemPickerRow >= 0) {
            closeItemPicker();
        }
    }

    private void setActiveFields(List<FormField> fields) {
        for (FormField f : allFields) {
            f.widget.visible = false;
            f.widget.active = false;
        }
        if (questRepeatableToggle != null) {
            questRepeatableToggle.visible = false;
            questRepeatableToggle.active = false;
        }
        if (questAutoCompleteToggle != null) {
            questAutoCompleteToggle.visible = false;
            questAutoCompleteToggle.active = false;
        }
        if (questHiddenUnderDependencyToggle != null) {
            questHiddenUnderDependencyToggle.visible = false;
            questHiddenUnderDependencyToggle.active = false;
        }
        if (questDependencyLockToggle != null) {
            questDependencyLockToggle.visible = false;
            questDependencyLockToggle.active = false;
        }
        for (LockToggleButton button : dependencyEntryLockButtons) {
            button.visible = false;
            button.active = false;
        }
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            box.visible = false;
            box.active = false;
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            box.visible = false;
            box.active = false;
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            box.visible = false;
            box.active = false;
        }
        for (EntryRemoveButton button : completionEntryRemoveButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryRemoveButton button : dependencyEntryRemoveButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryRemoveButton button : rewardEntryRemoveButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryTypeButton button : completionEntryTypeButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryTypeButton button : rewardEntryTypeButtons) {
            button.visible = false;
            button.active = false;
        }
        hideDescriptionFormatterButtons();
        activeFields.clear();
        activeFields.addAll(fields);
        for (FormField f : activeFields) {
            if (!allFields.contains(f)) allFields.add(f);
            f.widget.visible = true;
            f.widget.active = true;
        }
        entryRowsDirty = true;
        editorScroll = 0f;
    }

    private void hideDescriptionFormatterButtons() {
        for (TextInsertButton button : descriptionFormatButtons) {
            button.visible = false;
            button.active = false;
        }
    }

    private void initEntryRowBoxes() {
        resetEntryRows(EntryRowKind.DEPENDENCY, List.of());
        resetEntryRows(EntryRowKind.COMPLETION, List.of());
        resetEntryRows(EntryRowKind.REWARD, List.of());
        syncEntryBackingValues();
    }

    private ScaledMultiLineEditBox createEntryRowBox(EntryRowKind kind) {
        String hint = switch (kind) {
            case DEPENDENCY -> "boundless:quest_id";
            case REWARD -> "item: minecraft:item 1";
            case COMPLETION -> "collect: minecraft:item 1";
        };
        ScaledMultiLineEditBox box = new ScaledMultiLineEditBox(font, 0, 0, pw - 4, ENTRY_ROW_H,
                Component.literal(hint), Component.empty(), ENTRY_INPUT_TEXT_SCALE, false);
        box.setCharacterLimit(4096);
        box.setValueListener(v -> {
            if (syncingEntryRows) return;
            entryRowsDirty = true;
            syncEntryBackingValues();
        });
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    private List<ScaledMultiLineEditBox> entryRows(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> dependencyEntryBoxes;
            case COMPLETION -> completionEntryBoxes;
            case REWARD -> rewardEntryBoxes;
        };
    }

    private List<EntryRemoveButton> entryRemoveButtons(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> dependencyEntryRemoveButtons;
            case COMPLETION -> completionEntryRemoveButtons;
            case REWARD -> rewardEntryRemoveButtons;
        };
    }

    private List<EntryTypeButton> entryTypeButtons(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> List.of();
            case COMPLETION -> completionEntryTypeButtons;
            case REWARD -> rewardEntryTypeButtons;
        };
    }

    private List<EntryItemPickerButton> entryItemPickerButtons(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> List.of();
            case COMPLETION -> completionEntryItemPickerButtons;
            case REWARD -> rewardEntryItemPickerButtons;
        };
    }

    private void resetEntryRows(EntryRowKind kind, List<String> lines) {
        List<ScaledMultiLineEditBox> target = entryRows(kind);
        List<EntryRemoveButton> removeButtons = entryRemoveButtons(kind);
        syncingEntryRows = true;
        try {
            for (ScaledMultiLineEditBox box : target) {
                selectedItemIdByBox.remove(box);
                selectedItemComponentsByBox.remove(box);
                entryTypeByBox.remove(box);
                removeWidget(box);
            }
            target.clear();
            for (EntryRemoveButton button : removeButtons) {
                removeWidget(button);
            }
            removeButtons.clear();
            if (kind == EntryRowKind.DEPENDENCY) {
                for (LockToggleButton button : dependencyEntryLockButtons) {
                    removeWidget(button);
                }
                dependencyEntryLockButtons.clear();
            } else {
                for (EntryTypeButton button : entryTypeButtons(kind)) removeWidget(button);
                entryTypeButtons(kind).clear();
                for (EntryItemPickerButton button : entryItemPickerButtons(kind)) removeWidget(button);
                entryItemPickerButtons(kind).clear();
            }
            List<String> normalized = new ArrayList<>();
            if (lines != null) {
                for (String line : lines) {
                    String v = safe(line).trim();
                    if (!v.isBlank()) normalized.add(v);
                }
            }
            if (normalized.isEmpty()) normalized.add("");
            for (String line : normalized) {
                ScaledMultiLineEditBox box = createEntryRowBox(kind);
                if (kind != EntryRowKind.DEPENDENCY) {
                    ParsedEntry parsed = parseEntry(line);
                    String normalizedType = normalizeRowType(kind, parsed == null ? "" : parsed.type);
                    entryTypeByBox.put(box, normalizedType);
                    if (parsed != null && hasRowBrowser(kind, normalizedType)) {
                        String parsedId = safe(parsed.id).trim();
                        String itemId = parsedId;
                        String components = "";
                        int compStart = parsedId.indexOf('[');
                        if (compStart > 0) {
                            itemId = parsedId.substring(0, compStart).trim();
                            components = parsedId.substring(compStart).trim();
                        }
                        itemId = normalizeNamespacedId(itemId, false);
                        selectedItemIdByBox.put(box, itemId);
                        if (!components.isBlank()) selectedItemComponentsByBox.put(box, components);
                        else selectedItemComponentsByBox.remove(box);
                        box.setValue(displayNameForItem(itemId) + (components.isBlank() ? "" : " " + components) + " " + Math.max(1, parsed.count));
                    } else {
                        selectedItemComponentsByBox.remove(box);
                        box.setValue(line);
                    }
                } else {
                    box.setValue(line);
                }
                target.add(box);
            }
            ensureTrailingEmptyRow(kind);
            syncEntryRemoveButtons(kind);
            if (kind == EntryRowKind.DEPENDENCY) syncDependencyEntryLockButtons();
        } finally {
            syncingEntryRows = false;
        }
    }

    private void normalizeEntryRows(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        syncingEntryRows = true;
        try {
            for (int i = rows.size() - 1; i >= 0; i--) {
                ScaledMultiLineEditBox box = rows.get(i);
                boolean blank = safe(box.getValue()).trim().isBlank();
                if (!blank) continue;
                boolean isLast = i == rows.size() - 1;
                if (!isLast && !box.isFocused()) {
                    removeWidget(box);
                    rows.remove(i);
                }
            }
            ensureTrailingEmptyRow(kind);
        } finally {
            syncingEntryRows = false;
        }
    }

    private void ensureTrailingEmptyRow(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (rows.isEmpty()) {
            rows.add(createEntryRowBox(kind));
            syncEntryRemoveButtons(kind);
            return;
        }
        ScaledMultiLineEditBox last = rows.get(rows.size() - 1);
        if (!safe(last.getValue()).trim().isBlank()) {
            rows.add(createEntryRowBox(kind));
        }
        while (rows.size() > 1) {
            ScaledMultiLineEditBox prev = rows.get(rows.size() - 2);
            if (!safe(prev.getValue()).trim().isBlank() || prev.isFocused()) break;
            removeWidget(prev);
            rows.remove(rows.size() - 2);
        }
        syncEntryRemoveButtons(kind);
        if (kind == EntryRowKind.DEPENDENCY) syncDependencyEntryLockButtons();
    }

    private void syncEntryRemoveButtons(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<EntryRemoveButton> buttons = entryRemoveButtons(kind);
        while (buttons.size() < rows.size()) {
            EntryRemoveButton button = new EntryRemoveButton(kind);
            button.visible = false;
            button.active = false;
            buttons.add(button);
            addRenderableWidget(button);
        }
        while (buttons.size() > rows.size()) {
            EntryRemoveButton last = buttons.remove(buttons.size() - 1);
            removeWidget(last);
        }
        if (kind != EntryRowKind.DEPENDENCY) {
            List<EntryTypeButton> typeButtons = entryTypeButtons(kind);
            while (typeButtons.size() < rows.size()) {
                int rowIndex = typeButtons.size();
                EntryTypeButton button = new EntryTypeButton(kind, rowIndex);
                button.visible = false;
                button.active = false;
                typeButtons.add(button);
                addRenderableWidget(button);
            }
        while (typeButtons.size() > rows.size()) removeWidget(typeButtons.remove(typeButtons.size() - 1));
            for (int i = 0; i < typeButtons.size(); i++) typeButtons.get(i).setRow(i);

            List<EntryItemPickerButton> pickerButtons = entryItemPickerButtons(kind);
            for (EntryItemPickerButton button : pickerButtons) removeWidget(button);
            pickerButtons.clear();
            for (int i = 0; i < rows.size(); i++) {
                EntryItemPickerButton button = new EntryItemPickerButton(kind, i);
                button.visible = false;
                button.active = false;
                pickerButtons.add(button);
                addRenderableWidget(button);
            }
        }
    }

    private void syncDependencyEntryLockButtons() {
        while (dependencyEntryLockButtons.size() < dependencyEntryBoxes.size()) {
            LockToggleButton button = createDependencyLockToggle(dependencyLockState());
            dependencyEntryLockButtons.add(button);
        }
        while (dependencyEntryLockButtons.size() > dependencyEntryBoxes.size()) {
            LockToggleButton last = dependencyEntryLockButtons.remove(dependencyEntryLockButtons.size() - 1);
            removeWidget(last);
        }
        boolean state = dependencyLockState();
        for (LockToggleButton button : dependencyEntryLockButtons) {
            button.setState(state);
        }
    }

    private boolean dependencyLockState() {
        if (!dependencyEntryLockButtons.isEmpty()) return dependencyEntryLockButtons.get(0).isOn();
        return questDependencyLockToggle != null && questDependencyLockToggle.isOn();
    }

    private void setDependencyLockState(boolean state) {
        if (questDependencyLockToggle != null) questDependencyLockToggle.setState(state);
        for (LockToggleButton button : dependencyEntryLockButtons) {
            button.setState(state);
        }
    }

    private void removeEntryRow(EntryRowKind kind, EntryRemoveButton button) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<EntryRemoveButton> buttons = entryRemoveButtons(kind);
        int idx = buttons.indexOf(button);
        if (idx < 0 || idx >= rows.size()) return;

        if (rows.size() <= 1) {
            rows.get(0).setValue("");
            rows.get(0).setCursorPosition(0);
            rows.get(0).setFocused(true);
            entryRowsDirty = true;
            syncEntryBackingValues();
            return;
        }

        ScaledMultiLineEditBox removed = rows.remove(idx);
        selectedItemIdByBox.remove(removed);
        selectedItemComponentsByBox.remove(removed);
        entryTypeByBox.remove(removed);
        removeWidget(removed);
        EntryRemoveButton removedButton = buttons.remove(idx);
        removeWidget(removedButton);

        ensureTrailingEmptyRow(kind);
        if (!rows.isEmpty()) {
            int next = Math.min(idx, rows.size() - 1);
            rows.get(next).setFocused(true);
        }
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    private void setEntryRowsFromRaw(EntryRowKind kind, String raw) {
        resetEntryRows(kind, extractEntryLines(raw));
        syncEntryBackingValues();
    }

    private void setEntryRowsFromRaw(boolean reward, String raw) {
        setEntryRowsFromRaw(reward ? EntryRowKind.REWARD : EntryRowKind.COMPLETION, raw);
    }

    private String entryRowsToRaw(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<String> out = new ArrayList<>();
        for (ScaledMultiLineEditBox box : rows) {
            String v = composeEntryRowLine(kind, box).trim();
            if (!v.isBlank()) out.add(v);
        }
        return String.join("\n", out);
    }

    private String composeEntryRowLine(EntryRowKind kind, ScaledMultiLineEditBox box) {
        String raw = safe(box == null ? "" : box.getValue()).trim();
        if (kind == EntryRowKind.DEPENDENCY) return raw;
        String type = effectiveRowType(kind, box);
        if (raw.isBlank()) return "";
        if (hasRowBrowser(kind, type)) {
            String itemId = safe(selectedItemIdByBox.get(box)).trim();
            String components = safe(selectedItemComponentsByBox.get(box)).trim();
            ParsedEntry parsedRaw = parseEntry(raw);
            int count = parsedRaw == null ? 1 : Math.max(1, parsedRaw.count);
            if (!itemId.isBlank()) return type + ": " + itemId + (components.isBlank() ? "" : components) + " " + count;
        }
        ParsedEntry parsed = parseEntry(raw);
        String body;
        if (parsed != null) {
            if ("field".equals(type)) {
                String hint = safe(parsed.hint).replace("\\", "\\\\").replace("\"", "\\\"");
                body = "\"" + safe(parsed.id).replace("\\", "\\\\").replace("\"", "\\\"") + "\" \"" + hint + "\"";
            } else if ("command".equals(type)) {
                body = parsed.id;
            } else {
                body = parsed.id + (parsed.count > 1 ? " " + parsed.count : "");
            }
        } else {
            body = entryBodyWithoutType(raw);
        }
        body = safe(body).trim();
        return body.isBlank() ? "" : type + ": " + body;
    }

    private String entryBodyWithoutType(String raw) {
        String line = safe(raw).trim();
        int colon = line.indexOf(':');
        return colon > 0 ? line.substring(colon + 1).trim() : line;
    }

    private String effectiveRowType(EntryRowKind kind, ScaledMultiLineEditBox box) {
        String cached = entryTypeByBox.get(box);
        if (cached != null && !cached.isBlank()) return cached;
        ParsedEntry parsed = parseEntry(safe(box == null ? "" : box.getValue()));
        String fallback = switch (kind) {
            case COMPLETION -> "collect";
            case REWARD -> "item";
            case DEPENDENCY -> "";
        };
        String detected = parsed == null ? fallback : normalizeRowType(kind, parsed.type);
        if (box != null && !detected.isBlank()) entryTypeByBox.put(box, detected);
        return detected;
    }

    private String normalizeRowType(EntryRowKind kind, String rawType) {
        String t = safe(rawType).trim().toLowerCase(Locale.ROOT);
        if (kind == EntryRowKind.COMPLETION) {
            return switch (t) {
                case "item" -> "collect";
                case "entity" -> "kill";
                case "advancement" -> "achieve";
                case "input" -> "field";
                default -> t;
            };
        }
        if (kind == EntryRowKind.REWARD) {
            return switch (t) {
                case "submit" -> "item";
                case "exp" -> "xp";
                case "loottable" -> "loot";
                default -> t;
            };
        }
        return t;
    }

    private String entryRowsToRaw(boolean reward) {
        return entryRowsToRaw(reward ? EntryRowKind.REWARD : EntryRowKind.COMPLETION);
    }

    private List<String> collectDependencyEntries() {
        List<String> out = new ArrayList<>();
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            String value = safe(box.getValue()).trim();
            if (!value.isBlank()) out.add(value);
        }
        return out;
    }

    private int entryRowsHeight(EntryRowKind kind) {
        int count = Math.max(1, entryRows(kind).size());
        return (count * ENTRY_ROW_H) + ((count - 1) * ENTRY_ROW_GAP);
    }

    private int layoutEntryRows(EntryRowKind kind, int x, int y, int width, int clipTop, int clipBottom) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<EntryRemoveButton> removeButtons = entryRemoveButtons(kind);
        boolean dependency = kind == EntryRowKind.DEPENDENCY;
        boolean typed = kind == EntryRowKind.COMPLETION || kind == EntryRowKind.REWARD;
        int iconSpace = dependency ? DEP_ENTRY_ICON_SPACE : 0;
        int typeSpace = typed ? (ENTRY_TYPE_BTN_W + 2) : 0;
        int lockSpace = dependency ? (DEP_LOCK_SIZE + DEP_LOCK_GAP) : 0;
        int cursorY = y;
        List<LockToggleButton> lockButtons = dependency ? dependencyEntryLockButtons : List.of();
        List<EntryTypeButton> typeButtons = typed ? entryTypeButtons(kind) : List.of();
        List<EntryItemPickerButton> pickerButtons = typed ? entryItemPickerButtons(kind) : List.of();
        for (int i = 0; i < rows.size(); i++) {
            ScaledMultiLineEditBox box = rows.get(i);
            EntryRemoveButton removeButton = i < removeButtons.size() ? removeButtons.get(i) : null;
            LockToggleButton lockButton = dependency && i < lockButtons.size() ? lockButtons.get(i) : null;
            EntryTypeButton typeButton = typed && i < typeButtons.size() ? typeButtons.get(i) : null;
            EntryItemPickerButton pickerButton = typed && i < pickerButtons.size() ? pickerButtons.get(i) : null;
            boolean canPickItem = typed && hasRowBrowser(kind, effectiveRowType(kind, box));
            int pickerSpace = canPickItem ? (ENTRY_ITEM_PICK_BTN_W + 2) : 0;
            box.setX(x + iconSpace + typeSpace);
            box.setY(cursorY);
            box.setWidth(Math.max(10, width - ENTRY_REMOVE_BTN_W - 2 - lockSpace - iconSpace - typeSpace - pickerSpace));
            box.setHeight(ENTRY_ROW_H);
            boolean inside = cursorY + ENTRY_ROW_H > clipTop && cursorY < clipBottom;
            box.visible = inside;
            box.active = inside;
            if (typeButton != null) {
                typeButton.setPosition(x + iconSpace, cursorY);
                typeButton.setWidth(ENTRY_TYPE_BTN_W);
                typeButton.setHeight(ENTRY_ROW_H);
                typeButton.visible = inside;
                typeButton.active = inside;
            }
            if (pickerButton != null) {
                pickerButton.setRow(i);
                int pickerX = x + width - ENTRY_REMOVE_BTN_W - 2 - lockSpace - ENTRY_ITEM_PICK_BTN_W;
                pickerButton.setPosition(pickerX, cursorY);
                pickerButton.setWidth(ENTRY_ITEM_PICK_BTN_W);
                pickerButton.setHeight(ENTRY_ROW_H);
                pickerButton.visible = inside && canPickItem;
                pickerButton.active = inside && canPickItem;
            }
            if (removeButton != null) {
                removeButton.setPosition(x + width - ENTRY_REMOVE_BTN_W - lockSpace, cursorY);
                removeButton.setWidth(ENTRY_REMOVE_BTN_W);
                removeButton.setHeight(ENTRY_ROW_H);
                removeButton.visible = inside;
                removeButton.active = inside;
            }
            if (dependency && lockButton != null) {
                int lockX = x + width - DEP_LOCK_SIZE;
                int lockY = cursorY;
                lockButton.setX(lockX);
                lockButton.setY(lockY);
                lockButton.setSize(DEP_LOCK_SIZE, ENTRY_ROW_H);
                lockButton.visible = inside;
                lockButton.active = inside;
            }
            cursorY += ENTRY_ROW_H + ENTRY_ROW_GAP;
        }
        if (dependency) {
            for (int i = rows.size(); i < lockButtons.size(); i++) {
                LockToggleButton lockButton = lockButtons.get(i);
                lockButton.visible = false;
                lockButton.active = false;
            }
        }
        return Math.max(ENTRY_ROW_H, cursorY - y - ENTRY_ROW_GAP);
    }

    private void renderDependencyRowIcons(GuiGraphics gg) {
        if (dependencyEntryBoxes.isEmpty()) return;
        int clipLeft = pxRight + 2;
        int clipRight = pxRight + pw - 2;
        int clipTop = py;
        int clipBottom = py + ph;
        gg.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box == null || !box.visible) continue;
            String dependencyId = safe(box.getValue()).trim();
            if (dependencyId.isBlank()) continue;
            ItemStack icon = dependencyIconStack(dependencyId);
            if (icon.isEmpty()) continue;
            int iconX = box.getX() - DEP_ENTRY_ICON_SPACE + 1;
            int iconY = box.getY() + Math.max(0, (box.getHeight() - Math.round(16f * DEP_ENTRY_ICON_SCALE)) / 2);
            gg.pose().pushPose();
            gg.pose().translate(iconX, iconY, 0.0f);
            gg.pose().scale(DEP_ENTRY_ICON_SCALE, DEP_ENTRY_ICON_SCALE, 1.0f);
            gg.renderItem(icon, 0, 0);
            gg.pose().popPose();
        }
        gg.disableScissor();
    }

    private void setEntryRowsColor(EntryRowKind kind, boolean invalid) {
        for (ScaledMultiLineEditBox box : entryRows(kind)) {
            box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
        }
    }

    private void syncEntryBackingValues() {
        normalizeCommandRewardRows();
        if (questDependenciesBox != null) questDependenciesBox.setValue(entryRowsToRaw(EntryRowKind.DEPENDENCY));
        if (questCompletionBox != null) questCompletionBox.setValue(entryRowsToRaw(EntryRowKind.COMPLETION));
        if (questRewardBox != null) questRewardBox.setValue(entryRowsToRaw(EntryRowKind.REWARD));
    }

    private void normalizeCommandRewardRows() {
        if (rewardEntryBoxes.isEmpty()) return;
        boolean wasSyncing = syncingEntryRows;
        syncingEntryRows = true;
        try {
            for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
                if (box == null) continue;
                String raw = safe(box.getValue());
                String normalized = ensureCommandRewardMetadata(raw);
                if (normalized.equals(raw)) continue;
                int cursor = box.getCursorPosition();
                box.setValue(normalized);
                box.setCursorPosition(Math.min(cursor, normalized.length()));
            }
        } finally {
            syncingEntryRows = wasSyncing;
        }
    }

    private String ensureCommandRewardMetadata(String raw) {
        String line = safe(raw).trim();
        if (line.isBlank()) return raw;
        int colon = line.indexOf(':');
        if (colon <= 0) return raw;
        String type = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        if (!"command".equals(type)) return raw;
        String payload = line.substring(colon + 1).trim();
        CommandReward parsed = parseCommandReward(payload);
        if (parsed.command.isBlank()) return raw;

        boolean hasTitle = hasCommandRewardKey(payload, "title");
        boolean hasIcon = hasCommandRewardKey(payload, "icon");
        if (hasTitle && hasIcon) return raw;

        StringBuilder out = new StringBuilder(line);
        if (!hasTitle) out.append(" | title: \"\"");
        if (!hasIcon) out.append(" | icon: \"\"");
        return out.toString();
    }

    private boolean hasCommandRewardKey(String payload, String key) {
        if (payload == null || payload.isBlank() || key == null || key.isBlank()) return false;
        String keyPrefix = key.toLowerCase(Locale.ROOT) + ":";
        String[] segments = payload.split("\\|");
        for (String segment : segments) {
            String trimmed = safe(segment).trim().toLowerCase(Locale.ROOT);
            if (trimmed.startsWith(keyPrefix)) return true;
        }
        return false;
    }

    private FormField field(String label, AbstractWidget widget) {
        FormField field = new FormField(label, widget);
        if (!allFields.contains(field)) allFields.add(field);
        return field;
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("ui.boundless.editor." + key, args);
    }

    private static String trs(String key, Object... args) {
        return tr(key, args).getString();
    }

    private void setError(String msg) {
        statusMessage = msg;
        statusColor = 0xFF8080;
    }

    private void applyDescriptionFormat(String formatCode) {
        if (questDescriptionBox == null) return;
        questDescriptionBox.insertText(formatCode);
        questDescriptionBox.setFocused(true);
    }

    private boolean shouldWarnForUnsavedChanges(EditorEntry entry) {
        if (entry == null || !hasUnsavedEditorChanges()) return false;
        if ((mode == Mode.PACK_LIST && editorType != EditorType.PACK_OPTIONS) || mode == Mode.PACK_MENU) return false;
        if (Objects.equals(selectedEntryId, entry.id)) return false;
        if (pendingDiscardMode == mode && Objects.equals(pendingDiscardEntryId, entry.id)) {
            return false;
        }
        pendingDiscardMode = mode;
        pendingDiscardEntryId = entry.id;
        statusMessage = "You have unsaved changes. Click again to discard them.";
        statusColor = 0xFFD080;
        leftList.setSelectedId(selectedEntryId);
        return true;
    }

    private void clearPendingDiscardState() {
        pendingDiscardEntryId = "";
        pendingDiscardMode = null;
    }

    private void markCurrentEditorLoaded(boolean existingEntry) {
        clearPendingDiscardState();
        savedEditorState = existingEntry ? currentEditorStateSignature() : null;
    }

    private void markCurrentEditorUnsaved() {
        clearPendingDiscardState();
        savedEditorState = null;
    }

    private void markCurrentEditorSaved() {
        clearPendingDiscardState();
        savedEditorState = currentEditorStateSignature();
    }

    private boolean hasUnsavedEditorChanges() {
        if (editorType == EditorType.NONE) return false;
        String current = currentEditorStateSignature();
        return savedEditorState == null || !savedEditorState.equals(current);
    }

    private String currentEditorStateSignature() {
        return switch (editorType) {
            case PACK_CREATE -> "pack|" + safe(packNameBox == null ? "" : packNameBox.getValue())
                    + "|" + safe(packNamespaceBox == null ? "" : packNamespaceBox.getValue());
            case PACK_OPTIONS -> "pack-options|" + safe(packNameBox == null ? "" : packNameBox.getValue())
                    + "|" + safe(packIconPathBox == null ? "" : packIconPathBox.getValue())
                    + "|" + safe(packDescriptionBox == null ? "" : packDescriptionBox.getValue());
            case CATEGORY -> "category|" + safe(catIdBox == null ? "" : catIdBox.getValue())
                    + "|" + safe(catNameBox == null ? "" : catNameBox.getValue())
                    + "|" + safe(catIconBox == null ? "" : catIconBox.getValue())
                    + "|" + safe(catDependencyBox == null ? "" : catDependencyBox.getValue())
                    + "|" + (catAutoCompleteToggle != null && catAutoCompleteToggle.isOn());
            case SUBCATEGORY -> "subcategory|" + safe(subIdBox == null ? "" : subIdBox.getValue())
                    + "|" + safe(subCategoryBox == null ? "" : subCategoryBox.getValue())
                    + "|" + safe(subNameBox == null ? "" : subNameBox.getValue())
                    + "|" + safe(subIconBox == null ? "" : subIconBox.getValue())
                    + "|" + (subDefaultOpenToggle != null && subDefaultOpenToggle.isOn());
            case QUEST -> "quest|" + safe(questIdBox == null ? "" : questIdBox.getValue())
                    + "|" + safe(questOrderToken)
                    + "|" + safe(questNameBox == null ? "" : questNameBox.getValue())
                    + "|" + safe(questIconBox == null ? "" : questIconBox.getValue())
                    + "|" + safe(questDescriptionBox == null ? "" : questDescriptionBox.getValue())
                    + "|" + safe(questCategoryBox == null ? "" : questCategoryBox.getValue())
                    + "|" + safe(questSubCategoryBox == null ? "" : questSubCategoryBox.getValue())
                    + "|" + safe(questDependenciesBox == null ? "" : questDependenciesBox.getValue())
                    + "|" + dependencyLockState()
                    + "|" + (questOptionalToggle != null && questOptionalToggle.isOn())
                    + "|" + (questRepeatableToggle != null && questRepeatableToggle.isOn())
                    + "|" + (questAutoCompleteToggle != null && questAutoCompleteToggle.isOn())
                    + "|" + (questHiddenUnderDependencyToggle != null && questHiddenUnderDependencyToggle.isOn())
                    + "|" + safe(questCompletionBox == null ? "" : questCompletionBox.getValue())
                    + "|" + safe(questRewardBox == null ? "" : questRewardBox.getValue())
                    + "|" + safe(loadedQuestType);
            case NONE -> "";
        };
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private void attachIdSanitizer(EditBox box, boolean commaSeparated) {
        if (box == null) return;
        box.setResponder(value -> {
            if (suppressIdSanitizer) return;
            String normalized = normalizeIdInput(value, commaSeparated);
            if (normalized.equals(value)) return;
            int cursor = box.getCursorPosition();
            suppressIdSanitizer = true;
            box.setValue(normalized);
            box.setCursorPosition(Math.min(cursor, normalized.length()));
            box.setHighlightPos(box.getCursorPosition());
            suppressIdSanitizer = false;
        });
    }

    private String normalizeIdInput(String value, boolean commaSeparated) {
        String raw = safe(value);
        if (raw.isEmpty()) return raw;
        if (!commaSeparated) {
            return raw.toLowerCase(Locale.ROOT).replace(' ', '-');
        }
        String[] parts = raw.split(",", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) out.append(',');
            String p = parts[i];
            String trimmedLeading = p.replaceAll("^\\s+", "");
            String normalized = trimmedLeading.toLowerCase(Locale.ROOT).replace(' ', '-');
            out.append(normalized);
        }
        return out.toString();
    }

    private void addOptional(JsonObject obj, String key, String value) {
        String v = safe(value).trim();
        if (!v.isBlank()) obj.addProperty(key, v);
    }

    private void addOptionalInt(JsonObject obj, String key, String value) {
        String v = safe(value).trim();
        if (v.isBlank()) return;
        try {
            obj.addProperty(key, Integer.parseInt(v));
        } catch (NumberFormatException e) {
            obj.addProperty(key, v);
        }
    }

    private void addOptionalBool(JsonObject obj, String key, String value) {
        String v = safe(value).trim().toLowerCase(Locale.ROOT);
        if (v.isBlank()) return;
        obj.addProperty(key, v.equals("true"));
    }

    private boolean parseBool(String raw, boolean def) {
        if (raw == null) return def;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.isBlank()) return def;
        if (v.equals("true")) return true;
        if (v.equals("false")) return false;
        return def;
    }

    private IndexName splitIndexName(String raw) {
        if (raw == null) return new IndexName("", "");
        String trimmed = raw.trim();
        if (trimmed.isBlank()) return new IndexName("", "");
        int dash = trimmed.indexOf('-');
        if (dash > 0) {
            String left = trimmed.substring(0, dash).trim();
            String right = trimmed.substring(dash + 1).trim();
            if (!left.isBlank() && left.chars().allMatch(Character::isDigit)) {
                return new IndexName(left, right);
            }
        }
        return new IndexName("", trimmed);
    }

    private static ItemStack iconStackFromId(String raw) {
        if (raw == null || raw.isBlank()) return ItemStack.EMPTY;
        try {
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(raw)));
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack dependencyIconStack(String dependencyId) {
        String id = safe(dependencyId).trim();
        if (id.isBlank()) return ItemStack.EMPTY;
        String iconId = safe(questIconByIdCache.get(id)).trim();
        if (!iconId.isBlank()) {
            ItemStack stack = iconStackFromId(iconId);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private int parseCount(String raw, int def) {
        String v = raw == null ? "" : raw.trim();
        if (v.isBlank()) return def;
        try { return Integer.parseInt(v); } catch (Exception ignored) { return def; }
    }

    private String questFileBaseName(String questId, String indexRaw) {
        String id = safe(questId).trim();
        if (id.isBlank()) return id;
        String index = safe(indexRaw).trim();
        if (index.isBlank()) return id;
        return index + "-" + id;
    }

    private String questOrderTokenFromPath(Path path) {
        if (path == null) return "";
        return splitIndexName(fileId(path)).index;
    }

    private String questOrderTokenForSave(String questId) {
        String token = safe(questOrderToken).trim();
        if (!token.isBlank()) return token;

        String id = safe(questId).trim();
        if (id.isBlank()) return "";

        if (editingPath != null) {
            String fromPath = questOrderTokenFromPath(editingPath);
            if (!fromPath.isBlank()) return fromPath;
        }

        return nextQuestOrderToken();
    }

    private String nextQuestOrderToken() {
        if (currentPack == null) return "01";
        int max = 0;
        for (NamedEntry entry : listQuestEntries(currentPack)) {
            String index = splitIndexName(fileId(entry.path)).index;
            if (index.isBlank()) continue;
            try {
                max = Math.max(max, Integer.parseInt(index));
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format(Locale.ROOT, "%02d", max + 1);
    }

    private Path resourcePacksRoot() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("resourcepacks");
    }

    private Path packsRoot() {
        return modDataQuestPacksRoot();
    }

    private Path modDataQuestPacksRoot() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("boundless")
                .resolve("questpacks");
    }

    private boolean isInvalidPackFolderName(String name) {
        String value = safe(name).trim();
        return value.isBlank() || value.matches(".*[<>:\"/\\\\|?*].*");
    }

    private boolean isInvalidNamespace(String namespace) {
        String value = safe(namespace).trim();
        return value.isBlank() || !value.matches("[a-z0-9_.-]+");
    }

    private String namespaceFromPackName(String packName) {
        String value = safe(packName).trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) return "pack";
        String normalized = value.replaceAll("[^a-z0-9_.-]", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^[_\\.-]+|[_\\.-]+$", "");
        if (normalized.isBlank()) return "pack";
        return normalized;
    }

    private String normalizePackIconId(String raw) {
        String value = safe(raw).trim();
        if (value.isBlank()) return "";
        String normalized = normalizeNamespacedId(value, false);
        return ResourceLocation.tryParse(normalized) == null ? "" : normalized;
    }

    private List<QuestPack> listPacks() {
        migrateLegacyResourcePackQuestPacks();
        List<QuestPack> packs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Path root = packsRoot();
        if (Files.exists(root)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path path : stream) {
                    if (!Files.isDirectory(path)) continue;
                    String name = path.getFileName().toString();
                    String namespace = findNamespace(path);
                    boolean enabled = readPackMeta(path, name).enabled;
                    packs.add(new QuestPack(name, namespace, path, false, enabled));
                    seen.add(name);
                }
            } catch (IOException ignored) {
            }
        }

        Path rpRoot = resourcePacksRoot();
        if (Files.exists(rpRoot)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rpRoot, "*.zip")) {
                for (Path zip : stream) {
                    String file = zip.getFileName().toString();
                    if (!file.toLowerCase(Locale.ROOT).endsWith(".zip")) continue;
                    String name = file.substring(0, file.length() - 4);
                    if (seen.contains(name)) continue;
                    String namespace = findNamespaceFromZip(zip);
                    if (namespace.isBlank()) continue;
                    packs.add(new QuestPack(name, namespace, packsRoot().resolve(name), true));
                    seen.add(name);
                }
            } catch (IOException ignored) {
            }
        }

        packs.sort(Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)));
        return packs;
    }

    private void migrateLegacyResourcePackQuestPacks() {
        Path legacyRoot = resourcePacksRoot().resolve("boundless");
        if (!Files.isDirectory(legacyRoot)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(legacyRoot)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) continue;
                String namespace = findNamespace(path);
                if (namespace.isBlank()) continue;
                Path questsDir = path.resolve("data").resolve(namespace).resolve("quests");
                if (!Files.isDirectory(questsDir)) continue;
                Path target = packsRoot().resolve(path.getFileName().toString());
                if (Files.exists(target)) continue;
                mirrorDirectory(path, target);
            }
        } catch (Exception ignored) {
        }
    }

    private QuestPack findPackByName(String name) {
        for (QuestPack pack : listPacks()) {
            if (pack.name.equals(name)) return pack;
        }
        return null;
    }

    private boolean ensurePackWorkspace(QuestPack pack) {
        if (pack == null) return false;
        Path root = pack.root;
        if (Files.isDirectory(root)) return true;
        return !Files.exists(root);
    }

    private String findNamespace(Path root) {
        Path data = root.resolve("data");
        if (!Files.isDirectory(data)) return "";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(data)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) return p.getFileName().toString();
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    private String findNamespaceFromZip(Path zipPath) {
        if (zipPath == null || !Files.exists(zipPath)) return "";
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            return zip.stream()
                    .map(ZipEntry::getName)
                    .filter(Objects::nonNull)
                    .map(name -> name.replace('\\', '/'))
                    .filter(name -> name.startsWith("data/"))
                    .map(name -> name.split("/"))
                    .filter(parts -> parts.length >= 3 && "data".equals(parts[0]) && "quests".equals(parts[2]))
                    .map(parts -> parts[1])
                    .filter(ns -> !ns.isBlank())
                    .findFirst()
                    .orElse("");
        } catch (IOException ignored) {
        }
        return "";
    }

    private PackMeta readPackMeta(Path root, String fallbackName) {
        PackMeta meta = new PackMeta();
        meta.description = "Boundless Quest Pack: " + safe(fallbackName);
        meta.enabled = true;
        if (root == null) return meta;
        Path packMeta = packMetaPath(root);
        Path legacyPackMeta = root.resolve("pack.mcmeta");
        Path source = Files.exists(packMeta) ? packMeta : legacyPackMeta;
        if (!Files.exists(source)) return meta;
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!(parsed instanceof JsonObject obj)) return meta;
            JsonObject pack = obj.has("pack") && obj.get("pack").isJsonObject() ? obj.getAsJsonObject("pack") : null;
            if (pack != null && pack.has("description") && pack.get("description").isJsonPrimitive()) {
                meta.description = safe(pack.get("description").getAsString());
            }
            JsonObject boundless = obj.has("boundless") && obj.get("boundless").isJsonObject() ? obj.getAsJsonObject("boundless") : null;
            if (boundless != null && boundless.has("icon_path") && boundless.get("icon_path").isJsonPrimitive()) {
                meta.iconPath = safe(boundless.get("icon_path").getAsString());
            }
            if (boundless != null && boundless.has("enabled")) {
                JsonElement enabled = boundless.get("enabled");
                if (enabled != null && enabled.isJsonPrimitive()) {
                    try {
                        meta.enabled = enabled.getAsJsonPrimitive().isBoolean()
                                ? enabled.getAsBoolean()
                                : Boolean.parseBoolean(safe(enabled.getAsString()));
                    } catch (Exception ignored) {
                        meta.enabled = true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return meta;
    }

    private boolean setQuestPackEnabled(QuestPack pack, boolean enabled) {
        if (pack == null || pack.root == null) return false;
        try {
            PackMeta meta = readPackMeta(pack.root, pack.name);
            writePackMeta(pack.root, pack.name, safe(meta.description), safe(meta.iconPath), enabled);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void runBoundlessReloadInBackground() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            if (mc.getSingleplayerServer() != null) {
                mc.getSingleplayerServer().execute(() -> {
                    try {
                        var source = mc.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput();
                        mc.getSingleplayerServer().getCommands().performPrefixedCommand(source, "boundless reload");
                    } catch (Throwable ignored) {
                    }
                });
                return;
            }
            if (mc.player != null && mc.player.connection != null) {
                try {
                    mc.player.connection.sendCommand("boundless reload");
                } catch (Throwable ignored) {
                    mc.player.connection.sendChat("/boundless reload");
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void writePackMeta(Path root, String name, String description, String iconPath, boolean enabled) throws IOException {
        JsonObject pack = new JsonObject();
        JsonObject body = new JsonObject();
        body.addProperty("pack_format", 0);
        body.addProperty("description", description);
        pack.add("pack", body);
        JsonObject boundless = new JsonObject();
        if (iconPath != null && !iconPath.isBlank()) {
            boundless.addProperty("icon_path", iconPath);
        }
        boundless.addProperty("enabled", enabled);
        pack.add("boundless", boundless);

        Path meta = packMetaPath(root);
        Files.createDirectories(meta.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(meta, StandardCharsets.UTF_8)) {
            gson.toJson(pack, writer);
        }
        Files.deleteIfExists(root.resolve("pack.mcmeta"));
    }

    private Path packMetaPath(Path root) {
        return root.resolve("boundless").resolve("pack.json");
    }

    private void writePackIcon(Path root, String sourcePath) throws IOException {
        if (root == null) return;
        Path iconPath = root.resolve("pack.png");
        String rawSource = safe(sourcePath).trim();
        if (!rawSource.isBlank()) {
            Path source = Path.of(rawSource);
            if (!Files.exists(source) || Files.isDirectory(source)) {
                throw new IOException("Icon file missing");
            }
            Files.copy(source, iconPath, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        try (var in = Minecraft.getInstance().getResourceManager().open(GENERATED_PACK_ICON)) {
            Files.copy(in, iconPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteDirectory(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private ScreenState captureState() {
        if (leftList == null) return null;
        ScreenState state = new ScreenState();
        state.mode = mode;
        state.editorType = editorType;
        state.currentPack = currentPack;
        state.selectedEntryId = selectedEntryId;
        state.editingPath = editingPath;
        state.loadedQuestType = loadedQuestType;
        state.questOrderToken = questOrderToken;
        state.editorScroll = editorScroll;
        state.leftScroll = leftList.getScrollY();
        state.statusMessage = statusMessage;
        state.statusColor = statusColor;
        state.deleteConfirmArmed = deleteConfirmArmed;
        state.savedEditorState = savedEditorState;
        state.pendingDiscardEntryId = pendingDiscardEntryId;
        state.pendingDiscardMode = pendingDiscardMode;
        state.questSearchQuery = questSearchQuery;

        state.packName = safe(packNameBox == null ? "" : packNameBox.getValue());
        state.packNamespace = safe(packNamespaceBox == null ? "" : packNamespaceBox.getValue());
        state.packIconPath = safe(packIconPathBox == null ? "" : packIconPathBox.getValue());
        state.packDescription = safe(packDescriptionBox == null ? "" : packDescriptionBox.getValue());

        state.catId = safe(catIdBox == null ? "" : catIdBox.getValue());
        state.catName = safe(catNameBox == null ? "" : catNameBox.getValue());
        state.catIcon = safe(catIconBox == null ? "" : catIconBox.getValue());
        state.catDependency = safe(catDependencyBox == null ? "" : catDependencyBox.getValue());
        state.catAutoComplete = catAutoCompleteToggle != null && catAutoCompleteToggle.isOn();

        state.subId = safe(subIdBox == null ? "" : subIdBox.getValue());
        state.subCategory = safe(subCategoryBox == null ? "" : subCategoryBox.getValue());
        state.subName = safe(subNameBox == null ? "" : subNameBox.getValue());
        state.subIcon = safe(subIconBox == null ? "" : subIconBox.getValue());
        state.subDefaultOpen = subDefaultOpenToggle != null && subDefaultOpenToggle.isOn();

        state.questId = safe(questIdBox == null ? "" : questIdBox.getValue());
        state.questName = safe(questNameBox == null ? "" : questNameBox.getValue());
        state.questIcon = safe(questIconBox == null ? "" : questIconBox.getValue());
        state.questDescription = safe(questDescriptionBox == null ? "" : questDescriptionBox.getValue());
        state.questCategory = safe(questCategoryBox == null ? "" : questCategoryBox.getValue());
        state.questSubCategory = safe(questSubCategoryBox == null ? "" : questSubCategoryBox.getValue());
        state.questDependencies = safe(questDependenciesBox == null ? "" : questDependenciesBox.getValue());
        state.questLockAfterDependency = dependencyLockState();
        state.questOptional = questOptionalToggle != null && questOptionalToggle.isOn();
        state.questRepeatable = questRepeatableToggle != null && questRepeatableToggle.isOn();
        state.questAutoComplete = questAutoCompleteToggle != null && questAutoCompleteToggle.isOn();
        state.questHiddenUnderDependency = questHiddenUnderDependencyToggle != null && questHiddenUnderDependencyToggle.isOn();
        state.questCompletion = safe(questCompletionBox == null ? "" : questCompletionBox.getValue());
        state.questReward = safe(questRewardBox == null ? "" : questRewardBox.getValue());
        return state;
    }

    private void restoreState(ScreenState state) {
        if (state == null) {
            setMode(Mode.PACK_LIST);
            return;
        }
        String preservedStatusMessage = statusMessage;
        int preservedStatusColor = statusColor;
        currentPack = state.currentPack;
        selectedEntryId = state.selectedEntryId == null ? "" : state.selectedEntryId;
        mode = state.mode == null ? Mode.PACK_LIST : state.mode;
        questOrderToken = state.questOrderToken == null ? "" : state.questOrderToken;
        if (preservedStatusMessage == null || preservedStatusMessage.isBlank()) {
            statusMessage = state.statusMessage == null ? "" : state.statusMessage;
            statusColor = state.statusColor;
        } else {
            statusMessage = preservedStatusMessage;
            statusColor = preservedStatusColor;
        }
        deleteConfirmArmed = state.deleteConfirmArmed;
        savedEditorState = state.savedEditorState;
        pendingDiscardEntryId = state.pendingDiscardEntryId == null ? "" : state.pendingDiscardEntryId;
        pendingDiscardMode = state.pendingDiscardMode;
        questSearchQuery = state.questSearchQuery == null ? "" : state.questSearchQuery;
        if (questSearchBox != null) questSearchBox.setValue(questSearchQuery);
        updateDeleteButtonTexture();
        refreshLeftList();
        leftList.setScrollY(state.leftScroll);

        switch (state.editorType) {
            case PACK_CREATE -> {
                showPackCreate();
                packNameBox.setValue(state.packName);
                packNamespaceBox.setValue(state.packNamespace);
            }
            case PACK_OPTIONS -> {
                if (currentPack != null) {
                    showPackOptions(currentPack);
                    packNameBox.setValue(state.packName);
                    packIconPathBox.setValue(state.packIconPath);
                    packDescriptionBox.setValue(state.packDescription);
                } else {
                    clearEditor();
                }
            }
            case CATEGORY -> {
                CategoryData data = new CategoryData();
                data.id = state.catId;
                data.name = state.catName;
                data.icon = state.catIcon;
                data.dependency = state.catDependency;
                data.autoComplete = state.catAutoComplete ? "true" : "false";
                showCategoryEditor(data, state.editingPath);
            }
            case SUBCATEGORY -> {
                SubCategoryData data = new SubCategoryData();
                data.id = state.subId;
                data.category = state.subCategory;
                data.name = state.subName;
                data.icon = state.subIcon;
                data.defaultOpen = state.subDefaultOpen ? "true" : "false";
                showSubCategoryEditor(data, state.editingPath);
            }
            case QUEST -> {
                QuestEntryData data = new QuestEntryData();
                data.id = state.questId;
                data.name = state.questName;
                data.icon = state.questIcon;
                data.description = state.questDescription;
                data.category = state.questCategory;
                data.subCategory = state.questSubCategory;
                data.dependencies = state.questDependencies;
                data.lockAfterDependency = state.questLockAfterDependency ? "true" : "false";
                data.optional = state.questOptional ? "true" : "false";
                data.repeatable = state.questRepeatable ? "true" : "false";
                data.autoComplete = state.questAutoComplete ? "true" : "false";
                data.hiddenUnderDependency = state.questHiddenUnderDependency ? "true" : "false";
                data.type = state.loadedQuestType;
                data.completionJson = state.questCompletion;
                data.rewardJson = state.questReward;
                showQuestEditor(data, state.editingPath);
                setEntryRowsFromRaw(EntryRowKind.COMPLETION, safe(state.questCompletion));
                setEntryRowsFromRaw(EntryRowKind.REWARD, safe(state.questReward));
            }
            case NONE -> clearEditor();
        }

        loadedQuestType = state.loadedQuestType == null ? "" : state.loadedQuestType;
        editingPath = state.editingPath;
        editorScroll = state.editorScroll;
        updateBackButtonVisibility();
    }

    private List<NamedEntry> listCategoryEntries(QuestPack pack) {
        List<NamedEntry> entries = usesSingleFilePack(pack)
                ? listSingleFileEntries(pack.categoriesFile)
                : listEntries(pack.categoriesDir);
        entries.sort(Comparator.comparing(a -> safe(a.sortKey).toLowerCase(Locale.ROOT)));
        return entries;
    }

    private List<NamedEntry> listSubCategoryEntries(QuestPack pack) {
        List<NamedEntry> entries;
        if (usesSingleFilePack(pack)) {
            entries = listSingleFileEntries(pack.subCategoriesFile);
        } else {
            entries = new ArrayList<>();
            Set<String> seenPaths = new HashSet<>();
            for (Path dir : subCategoryDirectories(pack)) {
                for (NamedEntry entry : listEntries(dir)) {
                    if (entry == null || entry.path == null) continue;
                    String pathKey = entry.path.toString();
                    if (seenPaths.add(pathKey)) entries.add(entry);
                }
            }
        }
        entries.sort(Comparator.comparing(a -> safe(a.sortKey).toLowerCase(Locale.ROOT)));
        return entries;
    }

    private List<Path> subCategoryDirectories(QuestPack pack) {
        if (pack == null || pack.questsDir == null) return List.of();
        List<Path> dirs = new ArrayList<>();
        dirs.add(pack.questsDir.resolve("sub-category"));
        dirs.add(pack.questsDir.resolve("subcategories"));
        dirs.add(pack.questsDir.resolve("sub_category"));
        dirs.add(pack.questsDir.resolve("subcategory"));
        return dirs;
    }

    private List<NamedEntry> listQuestEntries(QuestPack pack) {
        List<NamedEntry> entries = usesSingleFilePack(pack)
                ? listSingleFileEntries(pack.questsFile)
                : listEntries(pack.questsDir);
        entries.sort(Comparator.comparing(a -> safe(a.sortKey).toLowerCase(Locale.ROOT)));
        return entries;
    }

    private List<NamedEntry> listSingleFileEntries(Path file) {
        List<NamedEntry> entries = new ArrayList<>();
        for (JsonObject obj : readSingleFileObjects(file)) {
            String id = optString(obj, "id", "");
            if (id.isBlank()) continue;
            String name = optString(obj, "name", id);
            String icon = optString(obj, "icon", "");
            int order = parseIntFlexible(obj, "order", 0);
            String sortKey = String.format(Locale.ROOT, "%06d_%s", order, id);
            entries.add(new NamedEntry(id, name, icon, file, sortKey));
        }
        return entries;
    }

    private List<NamedEntry> listEntries(Path dir) {
        List<NamedEntry> entries = new ArrayList<>();
        if (dir == null || !Files.isDirectory(dir)) return entries;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path path : stream) {
                JsonObject obj = readJson(path);
                String id = obj == null ? fileId(path) : optString(obj, "id", fileId(path));
                String name = obj == null ? id : optString(obj, "name", id);
                String icon = obj == null ? "" : optString(obj, "icon", "");
                int order = parseIntFlexible(obj, "order", 0);
                String sortKey = String.format(Locale.ROOT, "%06d_%s", order, fileId(path));
                entries.add(new NamedEntry(id, name, icon, path, sortKey));
            }
        } catch (IOException ignored) {
        }
        return entries;
    }

    private CategoryData loadCategory(QuestPack pack, String id) {
        if (usesSingleFilePack(pack)) {
            JsonObject obj = findSingleFileObject(pack.categoriesFile, id);
            if (obj == null) return null;
            CategoryData data = new CategoryData();
            data.path = pack.categoriesFile;
            data.id = optString(obj, "id", id);
            data.name = optString(obj, "name", "");
            data.icon = optString(obj, "icon", "");
            data.order = optStringFlexible(obj, "order", "");
            data.dependency = optString(obj, "dependency", "");
            data.autoComplete = optStringFlexible(obj, "auto_complete",
                    optStringFlexible(obj, "autoComplete", ""));
            return data;
        }
        Path path = pack.categoriesDir.resolve(id + ".json");
        if (!Files.exists(path)) {
            for (NamedEntry entry : listCategoryEntries(pack)) {
                if (entry.id.equals(id)) {
                    path = entry.path;
                    break;
                }
            }
        }
        JsonObject obj = readJson(path);
        if (obj == null) return null;

        CategoryData data = new CategoryData();
        data.path = path;
        data.id = optString(obj, "id", id);
        data.name = optString(obj, "name", "");
        data.icon = optString(obj, "icon", "");
        data.order = optStringFlexible(obj, "order", "");
        data.dependency = optString(obj, "dependency", "");
        data.autoComplete = optStringFlexible(obj, "auto_complete",
                optStringFlexible(obj, "autoComplete", ""));
        return data;
    }

    private SubCategoryData loadSubCategory(QuestPack pack, String id) {
        if (usesSingleFilePack(pack)) {
            JsonObject obj = findSingleFileObject(pack.subCategoriesFile, id);
            if (obj == null) return null;
            SubCategoryData data = new SubCategoryData();
            data.path = pack.subCategoriesFile;
            data.id = optString(obj, "id", id);
            data.category = optString(obj, "category", "");
            data.name = optString(obj, "name", "");
            data.icon = optString(obj, "icon", "");
            data.order = optStringFlexible(obj, "order", "");
            data.defaultOpen = optStringFlexible(obj, "default_open", "");
            return data;
        }
        Path path = pack.subCategoriesDir.resolve(id + ".json");
        if (!Files.exists(path)) {
            for (NamedEntry entry : listSubCategoryEntries(pack)) {
                if (entry.id.equals(id)) {
                    path = entry.path;
                    break;
                }
            }
        }
        JsonObject obj = readJson(path);
        if (obj == null) return null;

        SubCategoryData data = new SubCategoryData();
        data.path = path;
        data.id = optString(obj, "id", id);
        data.category = optString(obj, "category", "");
        data.name = optString(obj, "name", "");
        data.icon = optString(obj, "icon", "");
        data.order = optStringFlexible(obj, "order", "");
        data.defaultOpen = optStringFlexible(obj, "default_open", "");
        return data;
    }

    private QuestEntryData loadQuest(QuestPack pack, String id) {
        if (usesSingleFilePack(pack)) {
            JsonObject obj = findSingleFileObject(pack.questsFile, id);
            if (obj == null) return null;
            QuestEntryData data = new QuestEntryData();
            data.path = pack.questsFile;
            data.id = optString(obj, "id", id);
            data.name = optString(obj, "name", "");
            data.icon = optString(obj, "icon", "");
            data.description = optString(obj, "description", "");
            data.category = optString(obj, "category", "");
            data.subCategory = optString(obj, "sub-category", optString(obj, "subCategory", ""));
            data.dependencies = formatDependenciesLines(obj.get("dependencies"));
            data.lockAfterDependency = optStringFlexible(obj, "lock_after_dependency",
                    optStringFlexible(obj, "lockAfterDependency", ""));
            data.optional = optStringFlexible(obj, "optional", "");
            data.repeatable = optStringFlexible(obj, "repeatable", "");
            data.autoComplete = optStringFlexible(obj, "auto_complete",
                    optStringFlexible(obj, "autoComplete", ""));
            data.hiddenUnderDependency = optStringFlexible(obj, "hiddenUnderDependency",
                    optStringFlexible(obj, "hidden_under_dependency", ""));
            data.type = optString(obj, "type", "");
            data.completionJson = obj.has("completion") ? gson.toJson(obj.get("completion")) : "";
            data.rewardJson = obj.has("reward") ? gson.toJson(obj.get("reward")) : "";
            return data;
        }
        Path path = pack.questsDir.resolve(id + ".json");
        if (!Files.exists(path)) {
            for (NamedEntry entry : listQuestEntries(pack)) {
                if (entry.id.equals(id)) {
                    path = entry.path;
                    break;
                }
            }
        }
        JsonObject obj = readJson(path);
        if (obj == null) return null;

        QuestEntryData data = new QuestEntryData();
        data.path = path;
        data.id = optString(obj, "id", id);
        data.name = optString(obj, "name", "");
        data.icon = optString(obj, "icon", "");
        data.description = optString(obj, "description", "");
        data.category = optString(obj, "category", "");
        data.subCategory = optString(obj, "sub-category", optString(obj, "subCategory", ""));
        data.dependencies = formatDependenciesLines(obj.get("dependencies"));
        data.lockAfterDependency = optStringFlexible(obj, "lock_after_dependency",
                optStringFlexible(obj, "lockAfterDependency", ""));
        data.optional = optStringFlexible(obj, "optional", "");
        data.repeatable = optStringFlexible(obj, "repeatable", "");
        data.autoComplete = optStringFlexible(obj, "auto_complete",
                optStringFlexible(obj, "autoComplete", ""));
        data.hiddenUnderDependency = optStringFlexible(obj, "hiddenUnderDependency",
                optStringFlexible(obj, "hidden_under_dependency", ""));
        data.type = optString(obj, "type", "");
        data.completionJson = obj.has("completion") ? gson.toJson(obj.get("completion")) : "";
        data.rewardJson = obj.has("reward") ? gson.toJson(obj.get("reward")) : "";
        return data;
    }

    private String formatDependencies(JsonElement el) {
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) return el.getAsString();
        if (el.isJsonArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonElement e : el.getAsJsonArray()) {
                if (e != null && e.isJsonPrimitive()) parts.add(e.getAsString());
            }
            return String.join(", ", parts);
        }
        return "";
    }

    private String formatDependenciesLines(JsonElement el) {
        String value = formatDependencies(el);
        if (value.isBlank()) return "";
        String[] parts = value.split(",");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String trimmed = safe(part).trim();
            if (!trimmed.isBlank()) out.add(trimmed);
        }
        return String.join("\n", out);
    }

    private JsonObject readJson(Path path) {
        if (path == null || !Files.exists(path)) return null;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<JsonObject> readSingleFileObjects(Path path) {
        List<JsonObject> out = new ArrayList<>();
        if (path == null || !Files.exists(path)) return out;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonArray()) {
                for (JsonElement el : parsed.getAsJsonArray()) {
                    if (el != null && el.isJsonObject()) out.add(el.getAsJsonObject());
                }
                return out;
            }
            if (parsed != null && parsed.isJsonObject()) {
                JsonObject obj = parsed.getAsJsonObject();
                if (obj.has("entries") && obj.get("entries").isJsonArray()) {
                    for (JsonElement el : obj.getAsJsonArray("entries")) {
                        if (el != null && el.isJsonObject()) out.add(el.getAsJsonObject());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private void writeSingleFileObjects(Path path, List<JsonObject> entries) throws IOException {
        if (path == null) return;
        Files.createDirectories(path.getParent());
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (JsonObject entry : entries) {
            if (entry != null) array.add(entry);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(array, writer);
        }
    }

    private JsonObject findSingleFileObject(Path file, String id) {
        String key = safe(id).trim();
        if (key.isBlank()) return null;
        for (JsonObject obj : readSingleFileObjects(file)) {
            String objId = safe(optString(obj, "id", "")).trim();
            if (key.equals(objId)) return obj;
        }
        return null;
    }

    private void deleteSingleFileEntry(Path file, String id) {
        String key = safe(id).trim();
        if (key.isBlank()) {
            setError("Id required");
            return;
        }
        try {
            List<JsonObject> entries = readSingleFileObjects(file);
            int before = entries.size();
            entries.removeIf(obj -> key.equals(safe(optString(obj, "id", "")).trim()));
            if (entries.size() == before) {
                statusMessage = "Nothing to delete";
                statusColor = 0xA0A0A0;
                return;
            }
            writeSingleFileObjects(file, entries);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            stageCurrentPackChange("Deletion staged");
            QuestPanelClient.applyConfigChanges();
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private boolean isSingleFilePack(QuestPack pack) {
        if (pack == null) return false;
        return Files.exists(pack.categoriesFile) || Files.exists(pack.subCategoriesFile) || Files.exists(pack.questsFile);
    }

    private boolean usesSingleFilePack(QuestPack pack) {
        if (pack == null) return false;
        if (isSingleFilePack(pack)) return true;
        return safe(pack.namespace).trim().isBlank();
    }

    private void initializeSingleFilePack(QuestPack pack) throws IOException {
        if (pack == null) return;
        Files.createDirectories(pack.singleRoot);
        if (!Files.exists(pack.categoriesFile)) writeSingleFileObjects(pack.categoriesFile, List.of());
        if (!Files.exists(pack.subCategoriesFile)) writeSingleFileObjects(pack.subCategoriesFile, List.of());
        if (!Files.exists(pack.questsFile)) writeSingleFileObjects(pack.questsFile, List.of());
    }

    private int readOrderFromPath(Path path, int fallback) {
        JsonObject obj = readJson(path);
        return parseIntFlexible(obj, "order", fallback);
    }

    private int readOrderFromSingleFileEntry(Path file, String id, int fallback) {
        JsonObject obj = findSingleFileObject(file, id);
        return parseIntFlexible(obj, "order", fallback);
    }

    private void applySingleFileOrder(Path file, List<String> orderedIds, String key) throws IOException {
        if (file == null || orderedIds == null || orderedIds.isEmpty()) return;
        List<JsonObject> entries = readSingleFileObjects(file);
        if (entries.isEmpty()) return;
        Map<String, Integer> orderById = new LinkedHashMap<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            String id = safe(orderedIds.get(i)).trim();
            if (!id.isBlank()) orderById.put(id, i);
        }
        boolean changed = false;
        for (JsonObject obj : entries) {
            if (obj == null) continue;
            String id = safe(optString(obj, "id", "")).trim();
            Integer idx = orderById.get(id);
            if (idx == null) continue;
            obj.addProperty(key, idx);
            changed = true;
        }
        if (changed) writeSingleFileObjects(file, entries);
    }

    private String optString(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) return def;
        return obj.get(key).getAsString();
    }

    private String optStringFlexible(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key)) return def;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return def;
        if (el.isJsonPrimitive()) return el.getAsString();
        return def;
    }

    private String fileId(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    private static void stashPendingInitState(ScreenState state) {
        pendingInitState = state;
        pendingInitUntil = Util.getMillis() + PENDING_INIT_TTL_MS;
    }

    private static ScreenState takePendingInitState() {
        if (pendingInitState == null) return null;
        long now = Util.getMillis();
        if (now > pendingInitUntil) {
            pendingInitState = null;
            pendingInitUntil = 0L;
            return null;
        }
        ScreenState state = pendingInitState;
        pendingInitState = null;
        pendingInitUntil = 0L;
        return state;
    }

    private static void clearPendingInitState() {
        pendingInitState = null;
        pendingInitUntil = 0L;
    }
    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        updateLeftPaneLayout();
        if (editorType == EditorType.PACK_CREATE || editorType == EditorType.PACK_OPTIONS) {
        }
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, leftX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
        gg.blit(PANEL_TEX, rightX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        renderEditorFields(gg, mouseX, mouseY);
        renderPanelHeader(gg, leftX, panelHeaderTitle());
        renderSideTabs(gg, mouseX, mouseY);

        boolean leftVisible = leftList != null && leftList.visible;
        boolean backVisible = backButton != null && backButton.visible;
        boolean saveVisible = saveButton != null && saveButton.visible;
        boolean exportVisible = exportPackButton != null && exportPackButton.visible;
        boolean duplicateVisible = duplicateButton != null && duplicateButton.visible;
        boolean deleteQuestVisible = deleteQuestButton != null && deleteQuestButton.visible;
        boolean questSearchVisible = questSearchBox != null && questSearchBox.visible;
        if (leftList != null) leftList.visible = false;
        if (backButton != null) backButton.visible = false;
        if (saveButton != null) saveButton.visible = false;
        if (exportPackButton != null) exportPackButton.visible = false;
        if (duplicateButton != null) duplicateButton.visible = false;
        if (deleteQuestButton != null) deleteQuestButton.visible = false;
        if (questSearchBox != null) questSearchBox.visible = false;

        gg.enableScissor(pxRight, py, pxRight + pw, py + ph);
        super.render(gg, mouseX, mouseY, partialTick);
        gg.disableScissor();

        if (leftList != null) leftList.visible = leftVisible;
        if (backButton != null) backButton.visible = backVisible;
        if (saveButton != null) saveButton.visible = saveVisible;
        if (exportPackButton != null) exportPackButton.visible = exportVisible;
        if (duplicateButton != null) duplicateButton.visible = duplicateVisible;
        if (deleteQuestButton != null) deleteQuestButton.visible = deleteQuestVisible;
        if (questSearchBox != null) questSearchBox.visible = questSearchVisible;

        if (leftList != null && leftList.visible) leftList.render(gg, mouseX, mouseY, partialTick);
        if (backButton != null && backButton.visible) backButton.render(gg, mouseX, mouseY, partialTick);
        if (saveButton != null && saveButton.visible) saveButton.render(gg, mouseX, mouseY, partialTick);
        if (exportPackButton != null && exportPackButton.visible) exportPackButton.render(gg, mouseX, mouseY, partialTick);
        if (duplicateButton != null && duplicateButton.visible) duplicateButton.render(gg, mouseX, mouseY, partialTick);
        if (deleteQuestButton != null && deleteQuestButton.visible) deleteQuestButton.render(gg, mouseX, mouseY, partialTick);
        if (questSearchBox != null && questSearchBox.visible) questSearchBox.render(gg, mouseX, mouseY, partialTick);
        if (createPackButton != null && createPackButton.visible) createPackButton.render(gg, mouseX, mouseY, partialTick);
        if (importQuestPackButton != null && importQuestPackButton.visible) importQuestPackButton.render(gg, mouseX, mouseY, partialTick);
        renderIconOverlays(gg);
        if (!isItemPickerOpen() && leftList != null) leftList.renderHoverTooltipOnTop(gg);
        if (!isItemPickerOpen()) renderTabTooltip(gg, mouseX, mouseY);
        renderTypeMenu(gg, mouseX, mouseY);
        renderDropdownMenu(gg, mouseX, mouseY);
        renderItemPicker(gg, mouseX, mouseY);

        if (!statusMessage.isBlank()) {
            int msgW = font.width(statusMessage);
            int msgX = (this.width - msgW) / 2;
            gg.drawString(font, statusMessage, msgX, topY + PANEL_H + 8, statusColor, false);
        }
        renderSavedState(gg);
        renderFooter(gg, mouseX, mouseY);
        renderIdSuggestions(gg, mouseX, mouseY);
    }

    private void updateLeftPaneLayout() {
        if (leftList != null) {
            leftList.setBounds(pxLeft, py, pw, listH);
        }
        if (createPackButton != null && backButton != null) {
            createPackButton.setX(backButton.getX() + backButton.getWidth() + BOTTOM_CREATE_GAP + CREATE_PACK_BUTTON_OFFSET_X);
            createPackButton.setY(backButton.getY());
            createPackButton.visible = mode == Mode.PACK_LIST;
            createPackButton.active = mode == Mode.PACK_LIST;
        }
        if (importQuestPackButton != null && createPackButton != null) {
            importQuestPackButton.setX(createPackButton.getX() + createPackButton.getWidth() + 2);
            importQuestPackButton.setY(createPackButton.getY());
            importQuestPackButton.visible = mode == Mode.PACK_LIST;
            importQuestPackButton.active = mode == Mode.PACK_LIST;
        }
        layoutTabButtons();
    }

    private void layoutTabButtons() {
        int x = leftX - TAB_W + 4;
        int startY = py + 6;
        layoutTabButton(categoriesTabButton, x, startY);
        layoutTabButton(subCategoriesTabButton, x, startY + TAB_H + TAB_GAP);
        layoutTabButton(questsTabButton, x, startY + (TAB_H + TAB_GAP) * 2);
    }

    private void layoutTabButton(EditorTabButton button, int x, int y) {
        if (button == null) return;
        button.setX(x);
        button.setY(y);
        boolean visible = currentPack != null && (mode == Mode.CATEGORY_LIST || mode == Mode.SUBCATEGORY_LIST || mode == Mode.QUEST_LIST);
        button.visible = visible;
        button.active = visible;
    }

    private void renderSideTabs(GuiGraphics gg, int mouseX, int mouseY) {
        if (categoriesTabButton != null && categoriesTabButton.visible) categoriesTabButton.render(gg, mouseX, mouseY, 0f);
        if (subCategoriesTabButton != null && subCategoriesTabButton.visible) subCategoriesTabButton.render(gg, mouseX, mouseY, 0f);
        if (questsTabButton != null && questsTabButton.visible) questsTabButton.render(gg, mouseX, mouseY, 0f);
    }

    private void renderTabTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        if (categoriesTabButton != null && categoriesTabButton.visible && categoriesTabButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, Component.literal(categoriesTabButton.tooltip()), mouseX, mouseY);
            return;
        }
        if (subCategoriesTabButton != null && subCategoriesTabButton.visible && subCategoriesTabButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, Component.literal(subCategoriesTabButton.tooltip()), mouseX, mouseY);
            return;
        }
        if (questsTabButton != null && questsTabButton.visible && questsTabButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, Component.literal(questsTabButton.tooltip()), mouseX, mouseY);
            return;
        }
        if (importQuestPackButton != null && importQuestPackButton.visible && importQuestPackButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, tr("tooltip.import_questpacks"), mouseX, mouseY);
            return;
        }
        if (duplicateButton != null && duplicateButton.visible && duplicateButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, tr("tooltip.duplicate"), mouseX, mouseY);
            return;
        }
        if (deleteQuestButton != null && deleteQuestButton.visible && deleteQuestButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, tr(deleteConfirmArmed ? "tooltip.confirm_delete" : "tooltip.delete"), mouseX, mouseY);
        }
    }

    private void renderPanelHeader(GuiGraphics gg, int panelX, String title) {
        String text = safe(title);
        if (text.isBlank()) return;
        int textW = font.width(text);
        int headerW = Math.max(22, textW + 10);
        int x = panelX + 5;
        int y = topY - 7;
        renderThreeSliceHeader(gg, x, y, headerW);
        gg.drawString(font, text, x + (headerW - textW) / 2, y + 4, 0x404040, false);
    }

    private void renderThreeSliceHeader(GuiGraphics gg, int x, int y, int width) {
        int middleW = Math.max(0, width - HEADER_SLICE * 2);
        gg.blit(HEADER_TEX, x, y, 0, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
        if (middleW > 0) {
            for (int i = 0; i < middleW; i++) {
                gg.blit(HEADER_TEX, x + HEADER_SLICE + i, y, HEADER_SLICE, 0, 1, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
            }
        }
        gg.blit(HEADER_TEX, x + HEADER_SLICE + middleW, y, HEADER_TEX_W - HEADER_SLICE, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
    }

    private String panelHeaderTitle() {
        return switch (mode) {
            case PACK_LIST -> trs("header.quest_packs");
            case PACK_CREATE -> trs("header.create_pack");
            case CATEGORY_LIST -> trs("header.categories");
            case SUBCATEGORY_LIST -> trs("header.subcategories");
            case QUEST_LIST -> trs("header.quests");
            case PACK_MENU -> trs("header.quest_packs");
        };
    }

    private void renderSavedState(GuiGraphics gg) {
        if (editorType == EditorType.NONE || saveButton == null || !saveButton.visible) return;
        String text = hasUnsavedEditorChanges() ? trs("state.unsaved") : trs("state.saved");
        int color = hasUnsavedEditorChanges() ? 0x7A5A20 : 0x4F7A4F;
        int textW = font.width(text);
        int headerW = Math.max(22, textW + 10);
        int x = pxRight + pw - headerW - 2;
        int y = py - font.lineHeight - 8;
        renderThreeSliceHeader(gg, x, y, headerW);
        gg.drawString(font, text, x + (headerW - textW) / 2, y + 4, color, false);
    }

    private void renderIconOverlays(GuiGraphics gg) {
        int clipLeft = pxRight + 2;
        int clipRight = pxRight + pw - 2;
        int clipTop = py;
        int clipBottom = py + ph;

        gg.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        renderIconOverlay(gg, questIconBox);
        renderIconOverlay(gg, catIconBox);
        renderIconOverlay(gg, subIconBox);
        // no extra reward fields
        gg.disableScissor();
    }

    private void renderIconOverlay(GuiGraphics gg, EditBox box) {
        if (box == null || !box.visible) return;

        ItemStack stack = iconStackFromId(box.getValue());
        if (!stack.isEmpty()) {
            int iconX = box.getX() + box.getWidth() - 18;
            int iconY = box.getY() + (box.getHeight() - 16) / 2;
            gg.renderItem(stack, iconX, iconY);
        }

        if (!box.isFocused()) return;
        String value = box.getValue();
        if (value == null || value.isBlank()) return;

        String suggestion = computeIconSuggestion(value);
        if (suggestion == null || suggestion.isBlank()) return;
        String valueLower = value.toLowerCase(Locale.ROOT);
        String suggestionLower = suggestion.toLowerCase(Locale.ROOT);
        if (!suggestionLower.startsWith(valueLower)) return;
        if (suggestion.length() <= value.length()) return;

        String remainder = suggestion.substring(value.length());
        int textX = box.getX() + 4 + font.width(value);
        int textY = box.getY() + (box.getHeight() - font.lineHeight) / 2 + 1;
        gg.drawString(font, remainder, textX, textY, 0x808080, false);
    }

    private void renderIdSuggestions(GuiGraphics gg, int mouseX, int mouseY) {
        updateIdSuggestions();
        if (activeIdSuggestions.isEmpty()) return;
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null || bounds.w <= 0 || bounds.h <= 0) return;
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 500);
        gg.enableScissor(pxRight + 1, py + 1, pxRight + pw - 1, py + ph - 1);
        int scrollArea = activeIdSuggestions.size() > ID_SUGGESTION_VISIBLE_ROWS ? (ID_SUGGESTION_SCROLL_W + 3) : 0;
        int textRightPadding = 6 + scrollArea;
        int end = Math.min(activeIdSuggestions.size(), idSuggestionScroll + ID_SUGGESTION_VISIBLE_ROWS);
        for (int i = idSuggestionScroll; i < end; i++) {
            int row = i - idSuggestionScroll;
            int top = bounds.y + (row * ID_SUGGESTION_ROW_H);
            int bottom = top + ID_SUGGESTION_ROW_H;
            gg.fill(bounds.x, top, bounds.x + bounds.w, bottom, ID_SUGGESTION_BG_COLOR);
            float inv = 1f / ID_SUGGESTION_TEXT_SCALE;
            int maxWidth = Math.max(4, (int) ((bounds.w - textRightPadding) * inv));
            String text = font.plainSubstrByWidth(activeIdSuggestions.get(i), maxWidth);
            gg.pose().pushPose();
            gg.pose().scale(ID_SUGGESTION_TEXT_SCALE, ID_SUGGESTION_TEXT_SCALE, 1f);
            gg.drawString(font, text, (int) ((bounds.x + 4) * inv), (int) ((top + ID_SUGGESTION_TEXT_TOP_PADDING) * inv), ID_SUGGESTION_TEXT_COLOR, false);
            gg.pose().popPose();
        }
        if (activeIdSuggestions.size() > ID_SUGGESTION_VISIBLE_ROWS) {
            int trackX0 = bounds.x + bounds.w - ID_SUGGESTION_SCROLL_W - 2;
            int trackX1 = bounds.x + bounds.w - 2;
            int trackY0 = bounds.y + 1;
            int trackY1 = bounds.y + bounds.h - 1;
            gg.fill(trackX0, trackY0, trackX1, trackY1, ID_SUGGESTION_SCROLL_TRACK_COLOR);
            int max = Math.max(1, activeIdSuggestions.size() - ID_SUGGESTION_VISIBLE_ROWS);
            float ratio = (float) ID_SUGGESTION_VISIBLE_ROWS / (float) activeIdSuggestions.size();
            int thumbH = Math.max(8, Math.round((trackY1 - trackY0) * ratio));
            float scrollRatio = (float) idSuggestionScroll / (float) max;
            int thumbTop = trackY0 + Math.round((trackY1 - trackY0 - thumbH) * scrollRatio);
            gg.fill(trackX0, thumbTop, trackX1, thumbTop + thumbH, ID_SUGGESTION_SCROLL_THUMB_COLOR);
        }
        drawSuggestionBorder(gg, bounds);
        gg.disableScissor();
        gg.pose().popPose();
    }

    private void drawSuggestionBorder(GuiGraphics gg, SuggestionBounds bounds) {
        if (bounds == null || bounds.w <= 1 || bounds.h <= 1) return;
        int left = bounds.x;
        int top = bounds.y;
        int right = bounds.x + bounds.w;
        int bottom = bounds.y + bounds.h;
        gg.fill(left, top, right, top + 1, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(left, bottom - 1, right, bottom, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(left, top, left + 1, bottom, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(right - 1, top, right, bottom, ID_SUGGESTION_BORDER_COLOR);
    }

    private void updateIdSuggestions() {
        EditBox focused = focusedIdSuggestionField();
        ScaledMultiLineEditBox focusedMulti = focusedMultiIdSuggestionField();
        if (suppressIdSuggestions || (focused == null && focusedMulti == null)) {
            idSuggestionField = null;
            idSuggestionMultiLineField = null;
            activeIdSuggestions.clear();
            return;
        }
        idSuggestionField = focused;
        idSuggestionMultiLineField = focusedMulti;
        activeIdSuggestions.clear();

        List<String> all = focused != null ? idSuggestionValuesForField(focused) : idSuggestionValuesForMultiField(focusedMulti);
        if (all.isEmpty()) return;

        String prefix = focused != null
                ? idSuggestionPrefix(focused).toLowerCase(Locale.ROOT)
                : multiLineSuggestionPrefix(focusedMulti).toLowerCase(Locale.ROOT);
        for (String id : all) {
            if (id == null || id.isBlank()) continue;
            if (!prefix.isBlank() && !matchesIdPrefix(id, prefix)) continue;
            activeIdSuggestions.add(id);
            if (activeIdSuggestions.size() >= ID_SUGGESTION_MAX) break;
        }
        idSuggestionScroll = Mth.clamp(idSuggestionScroll, 0, Math.max(0, activeIdSuggestions.size() - ID_SUGGESTION_VISIBLE_ROWS));
    }

    private EditBox focusedIdSuggestionField() {
        if (packIconPathBox != null && packIconPathBox.visible && packIconPathBox.isFocused()) return packIconPathBox;
        if (questIconBox != null && questIconBox.visible && questIconBox.isFocused()) return questIconBox;
        if (catIconBox != null && catIconBox.visible && catIconBox.isFocused()) return catIconBox;
        if (subIconBox != null && subIconBox.visible && subIconBox.isFocused()) return subIconBox;
        if (catDependencyBox != null && catDependencyBox.visible && catDependencyBox.isFocused()) return catDependencyBox;
        return null;
    }

    private ScaledMultiLineEditBox focusedMultiIdSuggestionField() {
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box.visible && box.isFocused()) return box;
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            if (box.visible && box.isFocused()) return box;
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            if (box.visible && box.isFocused()) return box;
        }
        if (questCompletionBox != null && questCompletionBox.visible && questCompletionBox.isFocused()) return questCompletionBox;
        if (questRewardBox != null && questRewardBox.visible && questRewardBox.isFocused()) return questRewardBox;
        return null;
    }

    private String idSuggestionPrefix(EditBox field) {
        if (field == null) return "";
        return safe(field.getValue()).trim();
    }

    private List<String> idSuggestionValuesForField(EditBox field) {
        if (field == null) return List.of();
        if (isIconBox(field)) {
            return itemSuggestions();
        } else if (field == catDependencyBox || field == subCategoryBox || field == questCategoryBox) {
            return categorySuggestionCache;
        } else if (field == questSubCategoryBox) {
            String cat = dropdownBoxValue(questCategoryBox);
            if (!cat.isBlank()) {
                List<String> scoped = subCategoryByCategorySuggestion.get(cat.toLowerCase(Locale.ROOT));
                if (scoped != null && !scoped.isEmpty()) return scoped;
            }
            return subCategorySuggestionCache;
        }
        return List.of();
    }

    private List<String> idSuggestionValuesForMultiField(ScaledMultiLineEditBox field) {
        if (field == null) return List.of();
        if (isDependencyEntryField(field)) {
            return questSuggestionCache;
        }
        EntryRowKind rowKind = rowKindForField(field);
        if ((rowKind == EntryRowKind.COMPLETION || rowKind == EntryRowKind.REWARD)
                && hasRowBrowser(rowKind, effectiveRowType(rowKind, field))) {
            return List.of();
        }
        MultiLineEntryContext ctx = parseMultiLineEntryContext(field);
        if (ctx == null) return List.of();
        if (!ctx.hasTypeSeparator) {
            return isCompletionEntryField(field)
                    ? (LevelUpCompat.isAvailable()
                        ? List.of("collect", "submit", "kill", "achieve", "effect", "xp", "levelup", "field")
                        : List.of("collect", "submit", "kill", "achieve", "effect", "xp", "field"))
                    : List.of("item", "xp", "command", "loot");
        }
        return switch (ctx.type) {
            case "collect", "submit", "item" -> itemSuggestions();
            case "kill", "entity" -> entitySuggestions();
            case "effect" -> effectSuggestions();
            case "achieve", "advancement" -> advancementSuggestions();
            case "loot", "loottable" -> lootTableSuggestions();
            case "xp", "exp" -> List.of("points", "levels");
            case "levelup" -> LevelUpCompat.isAvailable() ? List.of("level") : List.of();
            case "field", "input" -> List.of("\"expected text\" \"input hint text\"");
            case "icon" -> itemSuggestions();
            default -> List.of();
        };
    }

    private boolean isCompletionEntryField(ScaledMultiLineEditBox field) {
        return field == questCompletionBox || completionEntryBoxes.contains(field);
    }

    private boolean isRewardEntryField(ScaledMultiLineEditBox field) {
        return field == questRewardBox || rewardEntryBoxes.contains(field);
    }

    private boolean isDependencyEntryField(ScaledMultiLineEditBox field) {
        return field == questDependenciesBox || dependencyEntryBoxes.contains(field);
    }

    private EntryRowKind rowKindForField(ScaledMultiLineEditBox field) {
        if (field == null) return null;
        if (completionEntryBoxes.contains(field)) return EntryRowKind.COMPLETION;
        if (rewardEntryBoxes.contains(field)) return EntryRowKind.REWARD;
        if (dependencyEntryBoxes.contains(field)) return EntryRowKind.DEPENDENCY;
        return null;
    }

    private String multiLineSuggestionPrefix(ScaledMultiLineEditBox field) {
        if (field == null) return "";
        if (isDependencyEntryField(field)) {
            String value = safe(field.getValue());
            if (value.isEmpty()) return "";
            int cursor = Math.max(0, Math.min(field.getCursorPosition(), value.length()));
            int lineStart = cursor <= 0 ? 0 : value.lastIndexOf('\n', Math.max(0, cursor - 1));
            lineStart = lineStart < 0 ? 0 : lineStart + 1;
            int lineEnd = value.indexOf('\n', cursor);
            if (lineEnd < 0) lineEnd = value.length();
            lineStart = Math.max(0, Math.min(lineStart, value.length()));
            lineEnd = Math.max(lineStart, Math.min(lineEnd, value.length()));
            if (lineStart >= value.length()) return "";
            return value.substring(lineStart, lineEnd).trim().toLowerCase(Locale.ROOT);
        }
        MultiLineEntryContext ctx = parseMultiLineEntryContext(field);
        if (ctx == null) return "";
        return ctx.hasTypeSeparator ? ctx.idPrefix : ctx.typePrefix;
    }

    private boolean matchesIdPrefix(String suggestion, String prefix) {
        if (suggestion == null || prefix == null) return false;
        String lowSuggestion = suggestion.toLowerCase(Locale.ROOT);
        String lowPrefix = prefix.toLowerCase(Locale.ROOT);
        if (lowSuggestion.startsWith(lowPrefix)) return true;
        int colon = lowSuggestion.indexOf(':');
        return colon >= 0 && colon + 1 < lowSuggestion.length()
                && lowSuggestion.substring(colon + 1).startsWith(lowPrefix);
    }

    private boolean clickIdSuggestion(double mouseX, double mouseY) {
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null || activeIdSuggestions.isEmpty()) return false;
        if (mouseX < bounds.x || mouseX > bounds.x + bounds.w || mouseY < bounds.y || mouseY > bounds.y + bounds.h) return false;
        int idx = (int) ((mouseY - bounds.y) / ID_SUGGESTION_ROW_H) + idSuggestionScroll;
        if (idx < 0 || idx >= activeIdSuggestions.size()) return false;
        if (idSuggestionField != null) {
            applyIdSuggestion(idSuggestionField, activeIdSuggestions.get(idx));
        } else if (idSuggestionMultiLineField != null) {
            applyMultiLineIdSuggestion(idSuggestionMultiLineField, activeIdSuggestions.get(idx));
        }
        return true;
    }

    private boolean scrollIdSuggestions(double mouseX, double mouseY, double scrollY) {
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null || activeIdSuggestions.isEmpty()) return false;
        if (mouseX < bounds.x || mouseX > bounds.x + bounds.w || mouseY < bounds.y || mouseY > bounds.y + bounds.h) return false;
        int max = Math.max(0, activeIdSuggestions.size() - ID_SUGGESTION_VISIBLE_ROWS);
        if (max <= 0) return true;
        int next = idSuggestionScroll - (int) Math.signum(scrollY);
        idSuggestionScroll = Mth.clamp(next, 0, max);
        return true;
    }

    private SuggestionBounds suggestionBounds() {
        if ((idSuggestionField == null && idSuggestionMultiLineField == null) || activeIdSuggestions.isEmpty()) return null;
        int x = idSuggestionField != null ? idSuggestionField.getX() : idSuggestionMultiLineField.getX();
        int yBase = idSuggestionField != null ? idSuggestionField.getY() + idSuggestionField.getHeight() + 1
                : idSuggestionMultiLineField.getY() + idSuggestionMultiLineField.getHeight() + 1;
        int w = idSuggestionField != null ? idSuggestionField.getWidth() : idSuggestionMultiLineField.getWidth();
        int rows = Math.min(ID_SUGGESTION_VISIBLE_ROWS, activeIdSuggestions.size());
        int h = rows * ID_SUGGESTION_ROW_H;
        int panelTop = py + 1;
        int panelBottom = py + ph - 1;
        int y = yBase;
        if (y + h > panelBottom) {
            int above = (idSuggestionField != null ? idSuggestionField.getY() : idSuggestionMultiLineField.getY()) - h - 1;
            y = Math.max(panelTop, above);
        }
        return new SuggestionBounds(x, y, w, Math.min(h, Math.max(0, panelBottom - y)));
    }

    private void applyIdSuggestion(EditBox field, String suggestion) {
        if (field == null || suggestion == null) return;
        field.setValue(suggestion);
        field.setCursorPosition(field.getValue().length());
        field.setHighlightPos(field.getCursorPosition());
        field.setFocused(true);
        updateIdSuggestions();
    }

    private void applyMultiLineIdSuggestion(ScaledMultiLineEditBox field, String suggestion) {
        if (isDependencyEntryField(field)) {
            if (field == null || suggestion == null || suggestion.isBlank()) return;
            String value = safe(field.getValue());
            int cursor = Math.max(0, Math.min(field.getCursorPosition(), value.length()));
            int lineStart = cursor <= 0 ? 0 : value.lastIndexOf('\n', Math.max(0, cursor - 1));
            lineStart = lineStart < 0 ? 0 : lineStart + 1;
            int lineEnd = value.indexOf('\n', cursor);
            if (lineEnd < 0) lineEnd = value.length();
            lineStart = Math.max(0, Math.min(lineStart, value.length()));
            lineEnd = Math.max(lineStart, Math.min(lineEnd, value.length()));
            String next = value.substring(0, lineStart) + suggestion + value.substring(lineEnd);
            field.setValue(next);
            field.setCursorPosition(lineStart + suggestion.length());
            field.setFocused(true);
            entryRowsDirty = true;
            syncEntryBackingValues();
            updateIdSuggestions();
            return;
        }
        MultiLineEntryContext ctx = parseMultiLineEntryContext(field);
        if (field == null || ctx == null || suggestion == null || suggestion.isBlank()) return;
        String value = safe(field.getValue());
        String replacement = suggestion;
        if (ctx.hasTypeSeparator && field != null) {
            if (isCompletionEntryField(field) || isRewardEntryField(field)) {
                if (ctx.type.equals("collect") || ctx.type.equals("submit") || ctx.type.equals("item")
                        || ctx.type.equals("kill") || ctx.type.equals("entity") || ctx.type.equals("effect")
                        || ctx.type.equals("achieve") || ctx.type.equals("advancement")
                        || ctx.type.equals("icon")) {
                    replacement = normalizeNamespacedId(suggestion, false);
                }
            }
        }
        String next;
        int nextCursor;
        if (!ctx.hasTypeSeparator) {
            next = value.substring(0, ctx.typeStart) + replacement + ": " + value.substring(ctx.typeEnd);
            nextCursor = ctx.typeStart + replacement.length() + 2;
        } else {
            next = value.substring(0, ctx.idStart) + replacement + value.substring(ctx.idEnd);
            nextCursor = ctx.idStart + replacement.length();
        }
        field.setValue(next);
        field.setCursorPosition(nextCursor);
        field.setFocused(true);
        entryRowsDirty = true;
        syncEntryBackingValues();
        updateIdSuggestions();
    }

    private MultiLineEntryContext parseMultiLineEntryContext(ScaledMultiLineEditBox field) {
        if (field == null) return null;
        try {
            String value = safe(field.getValue());
            int cursor = Math.max(0, Math.min(field.getCursorPosition(), value.length()));
            int lineStart = cursor <= 0 ? 0 : value.lastIndexOf('\n', Math.max(0, cursor - 1));
            lineStart = lineStart < 0 ? 0 : lineStart + 1;
            int lineEnd = value.indexOf('\n', cursor);
            if (lineEnd < 0) lineEnd = value.length();
            lineStart = Math.max(0, Math.min(lineStart, value.length()));
            lineEnd = Math.max(lineStart, Math.min(lineEnd, value.length()));
            String line = value.substring(lineStart, lineEnd);
            int localCursor = Math.max(0, Math.min(cursor - lineStart, line.length()));

            int firstNonWs = 0;
            while (firstNonWs < line.length() && Character.isWhitespace(line.charAt(firstNonWs))) firstNonWs++;
            firstNonWs = Math.max(0, Math.min(firstNonWs, line.length()));
            int colon = line.indexOf(':');
            if (colon < 0 || localCursor <= colon) {
                EntryRowKind rowKind = rowKindForField(field);
                if (rowKind != null) {
                    String fixedType = effectiveRowType(rowKind, field);
                    int idStart = lineStart + firstNonWs;
                    int idEnd = lineStart + line.length();
                    int prefixEnd = Math.max(firstNonWs, Math.min(localCursor, line.length()));
                    String idPrefix = line.substring(firstNonWs, prefixEnd).trim().toLowerCase(Locale.ROOT);
                    return new MultiLineEntryContext(true, fixedType, "", -1, -1, idStart, idEnd, idPrefix);
                }
                int prefixEnd = Math.max(firstNonWs, Math.min(localCursor, line.length()));
                String typePrefix = line.substring(firstNonWs, prefixEnd).trim().toLowerCase(Locale.ROOT);
                return new MultiLineEntryContext(false, "", typePrefix, lineStart + firstNonWs, lineStart + prefixEnd, -1, -1, "");
            }

            int safeColon = Math.max(0, Math.min(colon, line.length()));
            int typeStart = Math.min(firstNonWs, safeColon);
            int typeEnd = Math.max(typeStart, safeColon);
            String type = line.substring(typeStart, typeEnd).trim().toLowerCase(Locale.ROOT);
            if (isRewardEntryField(field) && "command".equals(type)) {
                String lineLower = line.toLowerCase(Locale.ROOT);
                int iconKey = lineLower.indexOf("icon:");
                if (iconKey >= 0) {
                    int iconStart = iconKey + "icon:".length();
                    while (iconStart < line.length() && Character.isWhitespace(line.charAt(iconStart))) iconStart++;
                    int iconEnd = iconStart;
                    while (iconEnd < line.length()) {
                        char ch = line.charAt(iconEnd);
                        if (ch == '|' || Character.isWhitespace(ch)) break;
                        iconEnd++;
                    }
                    if (localCursor >= iconStart && localCursor <= iconEnd) {
                        int prefixEnd = Math.max(iconStart, Math.min(localCursor, iconEnd));
                        String idPrefix = line.substring(iconStart, prefixEnd).trim().toLowerCase(Locale.ROOT);
                        return new MultiLineEntryContext(true, "icon", "", -1, -1,
                                lineStart + iconStart, lineStart + iconEnd, idPrefix);
                    }
                }
            }
            int idStartLocal = Math.min(line.length(), safeColon + 1);
            while (idStartLocal < line.length() && Character.isWhitespace(line.charAt(idStartLocal))) idStartLocal++;
            int idEndLocal = idStartLocal;
            while (idEndLocal < line.length() && !Character.isWhitespace(line.charAt(idEndLocal))) idEndLocal++;
            int prefixEnd = Math.max(idStartLocal, Math.min(localCursor, idEndLocal));
            String idPrefix = line.substring(idStartLocal, prefixEnd).trim().toLowerCase(Locale.ROOT);
            return new MultiLineEntryContext(true, type, "", -1, -1, lineStart + idStartLocal, lineStart + idEndLocal, idPrefix);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void renderEditorFields(GuiGraphics gg, int mouseX, int mouseY) {
        if (entryRowsDirty) {
            normalizeEntryRows(EntryRowKind.DEPENDENCY);
            normalizeEntryRows(EntryRowKind.COMPLETION);
            normalizeEntryRows(EntryRowKind.REWARD);
            syncEntryBackingValues();
            entryRowsDirty = false;
        }
        updateDynamicFieldSizes();
        updateInvalidFieldStyles();
        int contentHeight = contentHeight();
        int maxScroll = Math.max(0, contentHeight - ph);
        editorScroll = Math.max(0f, Math.min(editorScroll, maxScroll));

        int clipTop = py;
        int clipBottom = py + ph - 2;

        int yCursor = py - (int) editorScroll;
        for (FormField field : activeFields) {
            int labelY = yCursor;
            int boxY = labelY + font.lineHeight + FIELD_LABEL_GAP;
            int boxX = pxRight + 2;
            int widgetWidth = pw - 4;
            boolean dependencyEntriesField = field.widget == questDependenciesBox;
            boolean completionEntriesField = field.widget == questCompletionBox;
            boolean rewardEntriesField = field.widget == questRewardBox;
            int widgetHeight;

            if (!dependencyEntriesField && !completionEntriesField && !rewardEntriesField) {
                field.widget.setX(boxX);
                field.widget.setY(boxY);
                if (field.widget instanceof EditBox eb) {
                    eb.setWidth(widgetWidth);
                } else if (field.widget instanceof ScaledMultiLineEditBox mb) {
                    mb.setWidth(widgetWidth);
                } else if (field.widget instanceof ToggleButton tb) {
                    tb.setSize(TOGGLE_SIZE, TOGGLE_SIZE);
                }
            }

            if (dependencyEntriesField || completionEntriesField || rewardEntriesField) {
                field.widget.visible = false;
                field.widget.active = false;
                EntryRowKind rowKind = dependencyEntriesField
                        ? EntryRowKind.DEPENDENCY
                        : (rewardEntriesField ? EntryRowKind.REWARD : EntryRowKind.COMPLETION);
                widgetHeight = layoutEntryRows(rowKind, boxX, boxY, widgetWidth, clipTop, clipBottom);
            } else if (field.widget == questOptionalToggle) {
                widgetHeight = renderInlineQuestFlags(gg, mouseX, mouseY, boxX, boxY, widgetWidth, clipTop, clipBottom);
            } else {
                widgetHeight = field.widget.getHeight();
                boolean inside = boxY + widgetHeight > clipTop && boxY < clipBottom;
                field.widget.visible = inside;
                field.widget.active = inside;
                if (inside && field.widget instanceof EditBox eb && isDropdownField(eb)) {
                    boolean hovered = mouseX >= eb.getX() && mouseX <= eb.getX() + eb.getWidth()
                            && mouseY >= eb.getY() && mouseY <= eb.getY() + eb.getHeight();
                    boolean selected = isOpenDropdownField(eb);
                    if (hovered || selected || eb.isFocused()) {
                        int x0 = eb.getX();
                        int y0 = eb.getY();
                        int x1 = x0 + eb.getWidth();
                        int y1 = y0 + eb.getHeight();
                        gg.fill(x0, y0, x1, y0 + 1, 0xFFFFFFFF);
                        gg.fill(x0, y1 - 1, x1, y1, 0xFFFFFFFF);
                        gg.fill(x0, y0, x0 + 1, y1, 0xFFFFFFFF);
                        gg.fill(x1 - 1, y0, x1, y1, 0xFFFFFFFF);
                    }
                }
            }
            if (field.widget == questDescriptionBox) {
                layoutDescriptionFormatterButtons(boxX, boxY + field.widget.getHeight() + FORMAT_BAR_GAP, clipTop, clipBottom);
            }

            if (labelY + font.lineHeight > clipTop && labelY < clipBottom) {
                gg.drawString(font, field.displayLabel, pxRight + 2, labelY, 0xFFFFFF, false);
                String tooltip = tooltipForField(field);
                if (!tooltip.isBlank()) {
                    int labelW = font.width(field.displayLabel);
                    boolean hoverLabel = mouseX >= pxRight + 2 && mouseX <= pxRight + 2 + labelW
                            && mouseY >= labelY && mouseY <= labelY + font.lineHeight;
                    boolean hoverWidget = mouseX >= boxX && mouseX <= boxX + (pw - 4)
                            && mouseY >= boxY && mouseY <= boxY + widgetHeight;
                    if (!isItemPickerOpen() && (hoverLabel || hoverWidget)) {
                        gg.renderTooltip(font, Component.literal(tooltip), mouseX, mouseY);
                    }
                }
            }
            if (dependencyEntriesField) {
                for (LockToggleButton lockButton : dependencyEntryLockButtons) {
                    if (lockButton == null || !lockButton.visible) continue;
                    if (!lockButton.isMouseOver(mouseX, mouseY)) continue;
                    String lockTooltip = dependencyLockState()
                            ? "Quest is locked after dependency completed"
                            : "quest is locked until dependency completed";
                    if (!isItemPickerOpen()) gg.renderTooltip(font, Component.literal(lockTooltip), mouseX, mouseY);
                    break;
                }
            }
            yCursor += font.lineHeight + FIELD_LABEL_GAP + widgetHeight + FIELD_ROW_GAP;
            if (editorType == EditorType.PACK_OPTIONS && field.widget == packIconPathBox && exportPackButton != null) {
                int exportX = boxX;
                int exportY = yCursor;
                exportPackButton.setX(exportX);
                exportPackButton.setY(exportY);
                exportPackButton.setWidth(pw - 4);
                exportPackButton.setHeight(20);
                exportPackButton.visible = true;
                exportPackButton.active = true;
                yCursor += exportPackButton.getHeight() + FIELD_ROW_GAP;
            }
            if (field.widget == questDescriptionBox) {
                yCursor += FORMAT_BAR_GAP + FORMAT_BAR_H;
            }
        }
        renderDependencyRowIcons(gg);

        if (contentHeight > ph) {
            float ratio = (float) ph / (float) contentHeight;
            int barH = Math.max(12, (int) (ph * ratio));
            float scrollRatio = maxScroll <= 0 ? 0f : editorScroll / maxScroll;
            int barY = py + (int) ((ph - barH) * scrollRatio);
            gg.fill(pxRight + pw + 4, barY, pxRight + pw + 6, barY + barH, 0xFF808080);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollTypeMenu(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (scrollDropdownMenu(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (scrollItemPicker(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (scrollIdSuggestions(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (scrollEntryFields(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (mouseX >= pxRight && mouseX <= pxRight + pw && mouseY >= py && mouseY <= py + ph) {
            int contentHeight = contentHeight();
            if (contentHeight > ph) {
                editorScroll = Math.max(0f, Math.min(editorScroll - (float) scrollY * 12f, contentHeight - ph));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean scrollEntryFields(double mouseX, double mouseY, double scrollY) {
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box != null && box.visible && box.isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            if (box != null && box.visible && box.isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            if (box != null && box.visible && box.isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        return false;
    }

    private boolean isInsideSuggestionBox(double mouseX, double mouseY) {
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null) return false;
        return mouseX >= bounds.x && mouseX <= bounds.x + bounds.w
                && mouseY >= bounds.y && mouseY <= bounds.y + bounds.h;
    }

    private boolean isInsideSuggestionTargetField(double mouseX, double mouseY) {
        return isInsideBox(packIconPathBox, mouseX, mouseY)
                || isInsideBox(catIconBox, mouseX, mouseY)
                || isInsideBox(subIconBox, mouseX, mouseY)
                || isInsideBox(questIconBox, mouseX, mouseY)
                || isInsideBox(catDependencyBox, mouseX, mouseY)
                || isInsideBox(subCategoryBox, mouseX, mouseY)
                || isInsideBox(questCategoryBox, mouseX, mouseY)
                || isInsideBox(questSubCategoryBox, mouseX, mouseY)
                || isInsideMultiBox(questCompletionBox, mouseX, mouseY)
                || isInsideMultiBox(questRewardBox, mouseX, mouseY)
                || isInsideMultiBox(questDependenciesBox, mouseX, mouseY)
                || anyVisibleMultiBoxContains(dependencyEntryBoxes, mouseX, mouseY)
                || anyVisibleMultiBoxContains(completionEntryBoxes, mouseX, mouseY)
                || anyVisibleMultiBoxContains(rewardEntryBoxes, mouseX, mouseY);
    }

    private boolean isInsideBox(EditBox box, double mouseX, double mouseY) {
        return box != null && box.visible
                && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth()
                && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight();
    }

    private boolean isInsideMultiBox(ScaledMultiLineEditBox box, double mouseX, double mouseY) {
        return box != null && box.visible
                && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth()
                && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight();
    }

    private boolean anyVisibleMultiBoxContains(List<ScaledMultiLineEditBox> boxes, double mouseX, double mouseY) {
        if (boxes == null || boxes.isEmpty()) return false;
        for (ScaledMultiLineEditBox box : boxes) {
            if (isInsideMultiBox(box, mouseX, mouseY)) return true;
        }
        return false;
    }

    private int contentHeight() {
        int total = 0;
        for (FormField field : activeFields) {
            total += font.lineHeight + FIELD_LABEL_GAP + fieldHeight(field) + FIELD_ROW_GAP;
            if (editorType == EditorType.PACK_OPTIONS && field.widget == packIconPathBox) {
                total += 20 + FIELD_ROW_GAP;
            }
        }
        return total;
    }

    private int fieldHeight(FormField field) {
        if (field == null || field.widget == null) return 0;
        if (field.widget == questDependenciesBox) return entryRowsHeight(EntryRowKind.DEPENDENCY);
        if (field.widget == questCompletionBox) return entryRowsHeight(EntryRowKind.COMPLETION);
        if (field.widget == questRewardBox) return entryRowsHeight(EntryRowKind.REWARD);
        if (field.widget == questOptionalToggle) return TOGGLE_SIZE + INLINE_FLAG_LABEL_H + 3;
        if (field.widget instanceof ScaledMultiLineEditBox mb) {
            int baseHeight = computeMultilineHeight(mb, BOX_H_TALL);
            if (field.widget == questDescriptionBox) {
                return baseHeight + FORMAT_BAR_GAP + FORMAT_BAR_H;
            }
            return baseHeight;
        }
        return field.widget.getHeight();
    }

    private void updateDynamicFieldSizes() {
        for (FormField field : activeFields) {
            if (field.widget == questDependenciesBox || field.widget == questCompletionBox || field.widget == questRewardBox) continue;
            if (field.widget instanceof ScaledMultiLineEditBox mb) {
                mb.setWidth(pw - 4);
                mb.setHeight(computeMultilineHeight(mb, BOX_H_TALL));
            }
        }
    }

    private void updateInvalidFieldStyles() {
        setIdFieldColor(questIdBox, isInvalidQuestIdField());
        setIdFieldColor(questNameBox, isInvalidQuestNameField());
        setIdFieldColor(catDependencyBox, isInvalidCategoryDependency());
        setIdFieldColor(subCategoryBox, isInvalidParentCategory());
        setIdFieldColor(questCategoryBox, isInvalidQuestCategory());
        setIdFieldColor(questSubCategoryBox, isInvalidQuestSubCategory());
        setEntryRowsColor(EntryRowKind.DEPENDENCY, isInvalidQuestDependencies());
        setEntryRowsColor(EntryRowKind.COMPLETION, isInvalidCompletionEntries());
        setEntryRowsColor(EntryRowKind.REWARD, isInvalidRewardEntries());
    }

    private void setIdFieldColor(EditBox box, boolean invalid) {
        if (box == null) return;
        if (isDropdownField(box)) {
            applyDropdownTextColor(box, invalid);
            return;
        }
        box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
    }

    private void applyDropdownTextColor(EditBox box, boolean invalid) {
        if (box == null) return;
        int color = invalid ? INVALID_INPUT_TEXT_COLOR : DROPDOWN_INPUT_TEXT_COLOR;
        box.setTextColor(color);
        box.setTextColorUneditable(color);
    }

    private void setJsonFieldColor(ScaledMultiLineEditBox box, boolean invalid) {
        if (box == null) return;
        box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
    }

    private String tooltipForField(FormField field) {
        if (field == null || field.widget == null) return "";
        if (field.widget == questIdBox && isInvalidQuestIdField()) {
            return isDuplicateQuestId(safe(questIdBox.getValue()).trim()) ? "Quest ID already exists" : "Quest ID required";
        }
        if (field.widget == questNameBox && isInvalidQuestNameField()) return "Quest name required";
        if (field.widget == catDependencyBox && isInvalidCategoryDependency()) return INVALID_ID_TOOLTIP;
        if (field.widget == subCategoryBox && isInvalidParentCategory()) return INVALID_ID_TOOLTIP;
        if (field.widget == questCategoryBox && isInvalidQuestCategory()) {
            return dropdownBoxValue(questCategoryBox).isBlank() ? "Quest category required" : INVALID_ID_TOOLTIP;
        }
        if (field.widget == questSubCategoryBox && isInvalidQuestSubCategory()) return INVALID_ID_TOOLTIP;
        if (field.widget == questDependenciesBox && isInvalidQuestDependencies()) return INVALID_ID_TOOLTIP;
        return field.tooltip == null ? "" : field.tooltip;
    }

    private boolean isInvalidQuestIdField() {
        if (editorType != EditorType.QUEST) return false;
        String questId = safe(questIdBox == null ? "" : questIdBox.getValue()).trim();
        return questId.isBlank() || isDuplicateQuestId(questId);
    }

    private boolean isInvalidQuestNameField() {
        if (editorType != EditorType.QUEST) return false;
        return safe(questNameBox == null ? "" : questNameBox.getValue()).trim().isBlank();
    }

    private int renderInlineQuestFlags(GuiGraphics gg, int mouseX, int mouseY, int x, int y, int width, int clipTop, int clipBottom) {
        ToggleButton[] toggles = new ToggleButton[] { questOptionalToggle, questRepeatableToggle, questAutoCompleteToggle, questHiddenUnderDependencyToggle };
        String[] titles = new String[] { "Optional", "Repeatable", "Redeem", "Hidden" };
        String[][] tooltips = new String[][] {
                { "Optional", "Players can skip this quest without blocking progression." },
                { "Repeatable", "Players can restart this quest after claiming it." },
                { "Redeem", "Automatically claims this quest when it becomes ready." },
                { "Hidden Under Dependency", "Keeps this quest hidden until its dependencies are met." }
        };
        int segmentGap = 3;
        int segmentWidth = Math.max(20, (width - segmentGap * 3) / 4);
        int totalHeight = TOGGLE_SIZE + INLINE_FLAG_LABEL_H + 3;
        boolean inside = y + totalHeight > clipTop && y < clipBottom;
        for (int i = 0; i < toggles.length; i++) {
            ToggleButton toggle = toggles[i];
            if (toggle == null) continue;
            int segmentX = x + i * (segmentWidth + segmentGap);
            int toggleX = segmentX + (segmentWidth - TOGGLE_SIZE) / 2;
            int toggleY = y + INLINE_FLAG_LABEL_H + 3;
            toggle.setX(toggleX);
            toggle.setY(toggleY);
            toggle.setSize(TOGGLE_SIZE, TOGGLE_SIZE);
            toggle.visible = inside;
            toggle.active = inside;

            if (inside) {
                float scale = 0.55f;
                int maxUnscaledWidth = Math.max(1, (int) Math.floor(segmentWidth / scale));
                String clippedTitle = font.plainSubstrByWidth(titles[i], maxUnscaledWidth);
                int labelW = (int) Math.ceil(font.width(clippedTitle) * scale);
                gg.pose().pushPose();
                gg.pose().scale(scale, scale, 1f);
                float inv = 1f / scale;
                gg.drawString(font, clippedTitle, (int) ((segmentX + (segmentWidth - labelW) / 2) * inv), (int) (y * inv), 0xFFFFFF, false);
                gg.pose().popPose();
            }

            boolean hovered = mouseX >= segmentX && mouseX <= segmentX + segmentWidth
                    && mouseY >= y && mouseY <= y + totalHeight;
            if (!isItemPickerOpen() && hovered) {
                int tooltipY = Math.max(py + 2, y - 10);
                gg.renderComponentTooltip(font, List.of(Component.literal(tooltips[i][0]), Component.literal(tooltips[i][1])), mouseX, tooltipY);
            }
        }
        return totalHeight;
    }

    private boolean isInvalidCategoryDependency() {
        return isMissingCategoryId(safe(catDependencyBox == null ? "" : catDependencyBox.getValue()));
    }

    private boolean isInvalidParentCategory() {
        return isMissingCategoryId(dropdownBoxValue(subCategoryBox));
    }

    private boolean isInvalidQuestCategory() {
        String value = dropdownBoxValue(questCategoryBox);
        return value.isBlank() || isMissingCategoryId(value);
    }

    private boolean isInvalidQuestSubCategory() {
        return isMissingSubCategoryId(dropdownBoxValue(questSubCategoryBox));
    }

    private boolean isInvalidQuestDependencies() {
        if (currentPack == null) return false;
        for (String dependency : collectDependencyEntries()) {
            if (!questIdCache.contains(dependency)) return true;
        }
        return false;
    }

    private boolean isInvalidCompletionEntries() {
        String raw = safe(questCompletionBox == null ? "" : questCompletionBox.getValue()).trim();
        if (raw.isBlank()) return false;
        return parseCompletionEntries(raw, false) == null;
    }

    private boolean isInvalidRewardEntries() {
        String raw = safe(questRewardBox == null ? "" : questRewardBox.getValue()).trim();
        if (raw.isBlank()) return false;
        return parseRewardEntries(raw, false) == null;
    }

    private boolean isMissingCategoryId(String raw) {
        if (currentPack == null) return false;
        String id = raw == null ? "" : raw.trim();
        if (id.isBlank()) return false;
        return !categoryIdCache.contains(id);
    }

    private void setDropdownBoxValue(EditBox box, String value) {
        if (box == null) return;
        String normalized = normalizeDropdownValue(value);
        box.setValue(normalized.isBlank() ? "None" : normalized);
    }

    private String dropdownBoxValue(EditBox box) {
        return normalizeDropdownValue(safe(box == null ? "" : box.getValue()));
    }

    private String normalizeDropdownValue(String raw) {
        String value = safe(raw).trim();
        return value.equalsIgnoreCase("none") ? "" : value;
    }

    private boolean isDropdownField(EditBox box) {
        return box == subCategoryBox || box == questCategoryBox || box == questSubCategoryBox;
    }

    private boolean isOpenDropdownField(EditBox box) {
        if (box == null || openDropdownTarget == null) return false;
        return switch (openDropdownTarget) {
            case SUBCATEGORY_PARENT -> box == subCategoryBox;
            case QUEST_CATEGORY -> box == questCategoryBox;
            case QUEST_SUBCATEGORY -> box == questSubCategoryBox;
        };
    }

    private boolean isMissingSubCategoryId(String raw) {
        if (currentPack == null) return false;
        String id = raw == null ? "" : raw.trim();
        if (id.isBlank()) return false;
        String category = dropdownBoxValue(questCategoryBox);
        if (!category.isBlank()) {
            boolean hasExact = subCategoryIdCache.contains(category + "::" + id);
            boolean hasWildcard = subCategoryIdCache.contains("::" + id);
            if (hasExact || hasWildcard) return false;
        }
        return !subCategoryIdCache.contains(id);
    }

    private boolean isDuplicateQuestId(String questIdRaw) {
        if (currentPack == null) return false;
        String questId = safe(questIdRaw).trim();
        if (questId.isBlank()) return false;
        int matches = 0;
        boolean matchedEditingPath = false;
        for (NamedEntry entry : listQuestEntries(currentPack)) {
            if (entry == null || entry.id == null || !questId.equals(entry.id)) continue;
            matches++;
            if (editingPath != null && entry.path != null && editingPath.equals(entry.path)) {
                matchedEditingPath = true;
            }
        }
        if (editingPath != null && matchedEditingPath) return matches > 1;
        return matches > 0;
    }

    private boolean isDuplicateQuestIdAcrossPack(String questIdRaw) {
        if (currentPack == null) return false;
        String questId = safe(questIdRaw).trim();
        if (questId.isBlank()) return false;
        int matches = 0;
        for (NamedEntry entry : listQuestEntries(currentPack)) {
            if (entry == null || entry.id == null || !questId.equals(entry.id)) continue;
            matches++;
            if (matches > 1) return true;
        }
        return false;
    }

    private void refreshPackIdCaches() {
        categoryIdCache.clear();
        subCategoryIdCache.clear();
        questIdCache.clear();
        questIconByIdCache.clear();
        categorySuggestionCache.clear();
        subCategorySuggestionCache.clear();
        questSuggestionCache.clear();
        subCategoryByCategorySuggestion.clear();
        advancementIdCache.clear();
        lootTableIdCache.clear();

        if (currentPack != null) {
            for (NamedEntry entry : listCategoryEntries(currentPack)) {
                if (entry == null || entry.id == null) continue;
                if ("all".equalsIgnoreCase(entry.id.trim())) continue;
                if (entry != null && entry.id != null && !entry.id.isBlank()) categoryIdCache.add(entry.id);
            }
            for (NamedEntry entry : listSubCategoryEntries(currentPack)) {
                if (entry == null || entry.id == null || entry.id.isBlank()) continue;
                subCategoryIdCache.add(entry.id);
                SubCategoryData data = loadSubCategory(currentPack, entry.id);
                String category = data == null ? "" : safe(data.category).trim();
                if (!category.isBlank()) {
                    subCategoryIdCache.add(category + "::" + entry.id);
                }
            }
            for (NamedEntry entry : listQuestEntries(currentPack)) {
                if (entry != null && entry.id != null && !entry.id.isBlank()) {
                    questIdCache.add(entry.id);
                    if (entry.icon != null && !entry.icon.isBlank()) {
                        questIconByIdCache.put(entry.id, entry.icon);
                    }
                }
            }
        }

        List<String> localCategories = sortedIds(categoryIdCache);
        List<String> localSubCategories = sortedIds(subCategoryIdCache);
        List<String> localQuests = sortedIds(questIdCache);
        categorySuggestionCache.addAll(localCategories);
        questSuggestionCache.addAll(localQuests);

        if (currentPack != null) {
            for (NamedEntry entry : listSubCategoryEntries(currentPack)) {
                if (entry == null || entry.id == null || entry.id.isBlank()) continue;
                SubCategoryData data = loadSubCategory(currentPack, entry.id);
                String cat = data == null ? "" : safe(data.category).trim();
                addSubCategorySuggestion(entry.id, cat);
            }
        }
        for (String localSub : localSubCategories) {
            if (!subCategorySuggestionCache.contains(localSub)) subCategorySuggestionCache.add(localSub);
        }

        for (QuestData.Category c : QuestData.categoriesOrdered()) {
            if (c == null || c.id == null || c.id.isBlank()) continue;
            if ("all".equalsIgnoreCase(c.id.trim())) continue;
            categoryIdCache.add(c.id);
            if (!categorySuggestionCache.contains(c.id)) categorySuggestionCache.add(c.id);
        }
        for (QuestData.SubCategory sc : QuestData.subCategoriesAllOrdered()) {
            if (sc == null || sc.id == null || sc.id.isBlank()) continue;
            subCategoryIdCache.add(sc.id);
            addSubCategorySuggestion(sc.id, sc.category);
        }
        for (QuestData.Quest q : QuestData.all()) {
            if (q == null || q.id == null || q.id.isBlank()) continue;
            questIdCache.add(q.id);
            if (q.icon != null && !q.icon.isBlank() && !questIconByIdCache.containsKey(q.id)) {
                questIconByIdCache.put(q.id, q.icon);
            }
            if (!questSuggestionCache.contains(q.id)) questSuggestionCache.add(q.id);
        }
    }

    private void layoutDescriptionFormatterButtons(int startX, int y, int clipTop, int clipBottom) {
        boolean visible = editorType == EditorType.QUEST
                && questDescriptionBox != null
                && questDescriptionBox.visible
                && y + FORMAT_BAR_H > clipTop
                && y < clipBottom;
        int x = startX;
        for (TextInsertButton button : descriptionFormatButtons) {
            button.setX(x);
            button.setY(y);
            button.visible = visible;
            button.active = visible;
            x += button.getWidth() + FORMAT_BTN_GAP;
        }
    }

    private List<String> sortedIds(Set<String> ids) {
        List<String> out = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return out;
        out.addAll(ids);
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    private void addSubCategorySuggestion(String subId, String categoryId) {
        if (subId == null || subId.isBlank()) return;
        if (!subCategorySuggestionCache.contains(subId)) subCategorySuggestionCache.add(subId);
        String key = safe(categoryId).trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) return;
        List<String> scoped = subCategoryByCategorySuggestion.computeIfAbsent(key, k -> new ArrayList<>());
        if (!scoped.contains(subId)) scoped.add(subId);
    }

    private int computeMultilineHeight(ScaledMultiLineEditBox box, int minHeight) {
        int lines = Math.max(1, box.getLineCount());
        double lineHeight = box.getLineHeight();
        int content = (int) Math.ceil(lines * lineHeight) + 8;
        int maxHeight = Math.max(minHeight, ph - (font.lineHeight + FIELD_LABEL_GAP + FIELD_ROW_GAP + 6));
        return Math.max(minHeight, Math.min(maxHeight, content));
    }

    private void updateBackButtonVisibility() {
        if (backButton == null) return;
        backButton.visible = true;
        backButton.active = true;

        boolean showQuestActions = editorType == EditorType.QUEST
                || editorType == EditorType.CATEGORY
                || editorType == EditorType.SUBCATEGORY
                || editorType == EditorType.PACK_OPTIONS
                || (mode == Mode.PACK_MENU && currentPack != null);
        if (duplicateButton != null) {
            duplicateButton.visible = showQuestActions;
            duplicateButton.active = showQuestActions;
        }
        if (deleteQuestButton != null) {
            deleteQuestButton.visible = showQuestActions;
            deleteQuestButton.active = showQuestActions;
        }
        if (exportPackButton != null) {
            boolean showExport = editorType == EditorType.PACK_OPTIONS && currentPack != null;
            exportPackButton.visible = showExport;
            exportPackButton.active = showExport;
        }

        if (questSearchBox != null) {
            questSearchBox.visible = mode == Mode.QUEST_LIST;
            questSearchBox.active = mode == Mode.QUEST_LIST;
        }
    }

    private void goBack() {
        switch (mode) {
            case PACK_LIST -> {
                if (editorType == EditorType.PACK_OPTIONS) {
                    selectedEntryId = "";
                    clearEditor();
                    refreshLeftList();
                } else {
                    exitEditor();
                }
            }
            case PACK_CREATE, PACK_MENU -> setMode(Mode.PACK_LIST);
            case CATEGORY_LIST, SUBCATEGORY_LIST, QUEST_LIST -> setMode(Mode.PACK_LIST);
        }
    }

    private void exitEditor() {
        if (closingEditor) return;
        closingEditor = true;
        clearPendingInitState();
        applyStagedChangesOnClose();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        exitEditor();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        closeTransientMenus();
        pendingState = captureState();
        super.resize(minecraft, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickItemPicker(mouseX, mouseY)) return true;
        if (button == 0 && clickTypeMenu(mouseX, mouseY)) return true;
        if (button == 0 && clickDropdownMenu(mouseX, mouseY)) return true;
        if (button == 0) {
            if (!isInsideSuggestionBox(mouseX, mouseY) && !isInsideSuggestionTargetField(mouseX, mouseY)) {
                suppressIdSuggestions = true;
                activeIdSuggestions.clear();
                idSuggestionField = null;
                idSuggestionMultiLineField = null;
            } else {
                suppressIdSuggestions = false;
            }
        }
        if (button == 0 && deleteConfirmArmed
                && (deleteQuestButton == null || !deleteQuestButton.visible || !deleteQuestButton.isMouseOver(mouseX, mouseY))) {
            disarmDeleteConfirm();
        }
        boolean clickedToolbar = button == 0 && clickToolbarButtons(mouseX, mouseY);
        boolean clickedLeftList = button == 0 && leftList != null && leftList.visible && leftList.isMouseOver(mouseX, mouseY);
        if (button == 0 && pendingDiscardMode != null && !clickedToolbar && !clickedLeftList) {
            clearPendingDiscardState();
        }
        if (clickedToolbar) {
            return true;
        }
        if (clickDependencyRows(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && clickIdSuggestion(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && clickDropdownFields(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && !isInsideSuggestionTargetField(mouseX, mouseY)) {
            clearEntryTextFocus();
        }
        Style style = getFooterStyleAt((int) mouseX, (int) mouseY);
        if (style != null && style.getClickEvent() != null
                && style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL) {
            this.handleComponentClicked(style);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clearEntryTextFocus() {
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) box.setFocused(false);
        for (ScaledMultiLineEditBox box : completionEntryBoxes) box.setFocused(false);
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) box.setFocused(false);
    }

    private boolean clickDependencyRows(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (editorType != EditorType.QUEST) return false;
        if (clickRowControls(EntryRowKind.COMPLETION, mouseX, mouseY, button)) return true;
        if (clickRowControls(EntryRowKind.REWARD, mouseX, mouseY, button)) return true;
        if (dependencyEntryBoxes.isEmpty()) return false;

        for (EntryRemoveButton removeButton : dependencyEntryRemoveButtons) {
            if (removeButton == null || !removeButton.visible || !removeButton.active) continue;
            if (!removeButton.isMouseOver(mouseX, mouseY)) continue;
            return removeButton.mouseClicked(mouseX, mouseY, button);
        }
        for (LockToggleButton lockButton : dependencyEntryLockButtons) {
            if (lockButton == null || !lockButton.visible || !lockButton.active) continue;
            if (!lockButton.isMouseOver(mouseX, mouseY)) continue;
            if (!lockButton.mouseClicked(mouseX, mouseY, button)) return false;
            setDependencyLockState(lockButton.isOn());
            return true;
        }
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box == null || !box.visible || !box.active) continue;
            if (!box.isMouseOver(mouseX, mouseY)) continue;
            if (box.mouseClicked(mouseX, mouseY, button)) return true;
            box.setFocused(true);
            return true;
        }
        return false;
    }

    private boolean clickRowControls(EntryRowKind kind, double mouseX, double mouseY, int button) {
        clearEntryTextFocus();
        for (EntryItemPickerButton pickerButton : entryItemPickerButtons(kind)) {
            if (pickerButton != null && pickerButton.visible && pickerButton.active && pickerButton.isMouseOver(mouseX, mouseY)) {
                pickerButton.onPress();
                return true;
            }
        }
        for (EntryTypeButton typeButton : entryTypeButtons(kind)) {
            if (typeButton != null && typeButton.visible && typeButton.active && typeButton.isMouseOver(mouseX, mouseY)) {
                typeButton.onPress();
                return true;
            }
        }
        for (EntryRemoveButton removeButton : entryRemoveButtons(kind)) {
            if (removeButton != null && removeButton.visible && removeButton.active && removeButton.isMouseOver(mouseX, mouseY)) {
                return removeButton.mouseClicked(mouseX, mouseY, button);
            }
        }
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        for (int i = 0; i < rows.size(); i++) {
            ScaledMultiLineEditBox box = rows.get(i);
            if (box == null || !box.visible || !box.active || !box.isMouseOver(mouseX, mouseY)) continue;
            if (box.mouseClicked(mouseX, mouseY, button)) return true;
            box.setFocused(true);
            return true;
        }
        return false;
    }

    private boolean clickDropdownFields(double mouseX, double mouseY) {
        if (editorType == EditorType.SUBCATEGORY && isInsideBox(subCategoryBox, mouseX, mouseY)) {
            openDropdownMenu(DropdownMenuTarget.SUBCATEGORY_PARENT, subCategoryBox);
            return true;
        }
        if (editorType == EditorType.QUEST && isInsideBox(questCategoryBox, mouseX, mouseY)) {
            openDropdownMenu(DropdownMenuTarget.QUEST_CATEGORY, questCategoryBox);
            return true;
        }
        if (editorType == EditorType.QUEST
                && !dropdownBoxValue(questCategoryBox).isBlank()
                && isInsideBox(questSubCategoryBox, mouseX, mouseY)) {
            openDropdownMenu(DropdownMenuTarget.QUEST_SUBCATEGORY, questSubCategoryBox);
            return true;
        }
        return false;
    }

    private boolean clickToolbarButtons(double mouseX, double mouseY) {
        if (createPackButton != null && createPackButton.visible && createPackButton.active && createPackButton.isMouseOver(mouseX, mouseY)) {
            createPackButton.onPress();
            return true;
        }
        if (importQuestPackButton != null && importQuestPackButton.visible && importQuestPackButton.active && importQuestPackButton.isMouseOver(mouseX, mouseY)) {
            importQuestPackButton.onPress();
            return true;
        }
        if (categoriesTabButton != null && categoriesTabButton.visible && categoriesTabButton.active && categoriesTabButton.isMouseOver(mouseX, mouseY)) {
            categoriesTabButton.onPress();
            return true;
        }
        if (subCategoriesTabButton != null && subCategoriesTabButton.visible && subCategoriesTabButton.active && subCategoriesTabButton.isMouseOver(mouseX, mouseY)) {
            subCategoriesTabButton.onPress();
            return true;
        }
        if (questsTabButton != null && questsTabButton.visible && questsTabButton.active && questsTabButton.isMouseOver(mouseX, mouseY)) {
            questsTabButton.onPress();
            return true;
        }
        if (saveButton != null && saveButton.visible && saveButton.active && saveButton.isMouseOver(mouseX, mouseY)) {
            saveButton.onPress();
            return true;
        }
        if (exportPackButton != null && exportPackButton.visible && exportPackButton.active && exportPackButton.isMouseOver(mouseX, mouseY)) {
            exportPackButton.onPress();
            return true;
        }
        if (backButton != null && backButton.visible && backButton.active && backButton.isMouseOver(mouseX, mouseY)) {
            backButton.onPress();
            return true;
        }
        if (duplicateButton != null && duplicateButton.visible && duplicateButton.active
                && duplicateButton.isMouseOver(mouseX, mouseY)) {
            duplicateButton.onPress();
            return true;
        }
        if (deleteQuestButton != null && deleteQuestButton.visible && deleteQuestButton.active
                && deleteQuestButton.isMouseOver(mouseX, mouseY)) {
            handleDeleteButtonPress();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (itemPickerSearchBox != null && itemPickerSearchBox.active && itemPickerSearchBox.isFocused()
                && itemPickerSearchBox.keyPressed(keyCode, scanCode, modifiers)) {
            itemPickerPage = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && isItemPickerOpen()) {
            closeItemPicker();
            return true;
        }
        suppressIdSuggestions = false;
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (!activeIdSuggestions.isEmpty() && (idSuggestionField != null || idSuggestionMultiLineField != null)) {
                String suggestion = activeIdSuggestions.get(0);
                if (idSuggestionField != null) {
                    applyIdSuggestion(idSuggestionField, suggestion);
                } else if (idSuggestionMultiLineField != null) {
                    applyMultiLineIdSuggestion(idSuggestionMultiLineField, suggestion);
                }
                return true;
            }
            EditBox focused = focusedEditBox();
            if (focused != null) {
                String suggestion = computeIconSuggestion(focused.getValue());
                if (suggestion != null && !suggestion.isBlank()) {
                    focused.setValue(suggestion);
                    focused.setCursorPosition(suggestion.length());
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (itemPickerSearchBox != null && itemPickerSearchBox.active && itemPickerSearchBox.isFocused()
                && itemPickerSearchBox.charTyped(codePoint, modifiers)) {
            itemPickerPage = 0;
            return true;
        }
        suppressIdSuggestions = false;
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    private PickerMode pickerModeForType(EntryRowKind kind, String type) {
        if (kind == EntryRowKind.REWARD && "item".equals(type)) return PickerMode.ITEMS;
        if (kind != EntryRowKind.COMPLETION) return PickerMode.NONE;
        return switch (type) {
            case "collect", "submit" -> PickerMode.ITEMS;
            case "kill", "entity" -> PickerMode.MOBS;
            case "effect" -> PickerMode.EFFECTS;
            default -> PickerMode.NONE;
        };
    }

    private boolean hasRowBrowser(EntryRowKind kind, String type) {
        return pickerModeForType(kind, type) != PickerMode.NONE;
    }

    private void openTypeMenu(EntryRowKind kind, int row, int x, int y, int w) {
        openTypeMenuKind = kind;
        openTypeMenuRow = row;
        openTypeMenuX = x;
        openTypeMenuY = y + ENTRY_ROW_H;
        openTypeMenuW = Math.max(56, w + 10);
        openTypeMenuH = Math.min(5, entryTypeOptions(kind).size()) * ENTRY_ROW_H;
        openTypeMenuScroll = 0;
    }

    private boolean clickTypeMenu(double mouseX, double mouseY) {
        if (openTypeMenuKind == null || openTypeMenuRow < 0) return false;
        if (mouseX < openTypeMenuX || mouseX > openTypeMenuX + openTypeMenuW || mouseY < openTypeMenuY || mouseY > openTypeMenuY + openTypeMenuH) {
            openTypeMenuKind = null;
            openTypeMenuRow = -1;
            return false;
        }
        int index = openTypeMenuScroll + (int) ((mouseY - openTypeMenuY) / ENTRY_ROW_H);
        List<String> options = entryTypeOptions(openTypeMenuKind);
        if (index >= 0 && index < options.size()) setRowType(openTypeMenuKind, openTypeMenuRow, options.get(index));
        openTypeMenuKind = null;
        openTypeMenuRow = -1;
        return true;
    }

    private void renderTypeMenu(GuiGraphics gg, int mouseX, int mouseY) {
        if (openTypeMenuKind == null || openTypeMenuRow < 0) return;
        List<String> options = entryTypeOptions(openTypeMenuKind);
        gg.fill(openTypeMenuX, openTypeMenuY, openTypeMenuX + openTypeMenuW, openTypeMenuY + openTypeMenuH, ID_SUGGESTION_BG_COLOR);
        gg.fill(openTypeMenuX, openTypeMenuY, openTypeMenuX + openTypeMenuW, openTypeMenuY + 1, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(openTypeMenuX, openTypeMenuY + openTypeMenuH - 1, openTypeMenuX + openTypeMenuW, openTypeMenuY + openTypeMenuH, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(openTypeMenuX, openTypeMenuY, openTypeMenuX + 1, openTypeMenuY + openTypeMenuH, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(openTypeMenuX + openTypeMenuW - 1, openTypeMenuY, openTypeMenuX + openTypeMenuW, openTypeMenuY + openTypeMenuH, ID_SUGGESTION_BORDER_COLOR);
        int visible = Math.min(5, options.size());
        int start = Mth.clamp(openTypeMenuScroll, 0, Math.max(0, options.size() - visible));
        int end = Math.min(options.size(), start + visible);
        for (int i = start; i < end; i++) {
            int row = i - start;
            int y = openTypeMenuY + row * ENTRY_ROW_H;
            boolean hovered = mouseX >= openTypeMenuX && mouseX <= openTypeMenuX + openTypeMenuW && mouseY >= y && mouseY < y + ENTRY_ROW_H;
            if (hovered) gg.fill(openTypeMenuX + 1, y, openTypeMenuX + openTypeMenuW - 1, y + ENTRY_ROW_H, 0xFF2D2D2D);
            gg.drawString(font, options.get(i), openTypeMenuX + 3, y + 2, ID_SUGGESTION_TEXT_COLOR, false);
        }
        if (options.size() > visible) {
            int trackX0 = openTypeMenuX + openTypeMenuW - ID_SUGGESTION_SCROLL_W - 1;
            int trackY0 = openTypeMenuY + 1;
            int trackH = openTypeMenuH - 2;
            gg.fill(trackX0, trackY0, trackX0 + ID_SUGGESTION_SCROLL_W, trackY0 + trackH, ID_SUGGESTION_SCROLL_TRACK_COLOR);
            int maxStart = options.size() - visible;
            int thumbH = Math.max(6, (trackH * visible) / options.size());
            int thumbTravel = Math.max(0, trackH - thumbH);
            int thumbY = trackY0 + (maxStart <= 0 ? 0 : (openTypeMenuScroll * thumbTravel) / maxStart);
            gg.fill(trackX0, thumbY, trackX0 + ID_SUGGESTION_SCROLL_W, thumbY + thumbH, ID_SUGGESTION_SCROLL_THUMB_COLOR);
        }
    }

    private boolean scrollTypeMenu(double mouseX, double mouseY, double delta) {
        if (openTypeMenuKind == null || openTypeMenuRow < 0) return false;
        if (mouseX < openTypeMenuX || mouseX > openTypeMenuX + openTypeMenuW || mouseY < openTypeMenuY || mouseY > openTypeMenuY + openTypeMenuH) return false;
        List<String> options = entryTypeOptions(openTypeMenuKind);
        int visible = Math.min(5, options.size());
        int maxStart = Math.max(0, options.size() - visible);
        openTypeMenuScroll = Mth.clamp(openTypeMenuScroll - (delta > 0 ? 1 : -1), 0, maxStart);
        return true;
    }

    private void openDropdownMenu(DropdownMenuTarget target, EditBox box) {
        if (target == null || box == null) return;
        List<String> options = dropdownOptions(target);
        if (options.isEmpty()) return;
        openDropdownTarget = target;
        openDropdownX = box.getX();
        openDropdownW = Math.max(70, box.getWidth());
        openDropdownH = Math.min(5, options.size()) * ENTRY_ROW_H;
        int preferredBelowY = box.getY() + box.getHeight();
        int preferredAboveY = box.getY() - openDropdownH;
        openDropdownY = preferredBelowY;
        if (preferredBelowY + openDropdownH > py + ph && preferredAboveY >= py) {
            openDropdownY = preferredAboveY;
        }
        int maxStart = Math.max(0, options.size() - Math.min(5, options.size()));
        openDropdownScroll = Mth.clamp(savedDropdownScroll(target), 0, maxStart);
    }

    private boolean clickDropdownMenu(double mouseX, double mouseY) {
        if (openDropdownTarget == null) return false;
        if (mouseX < openDropdownX || mouseX > openDropdownX + openDropdownW || mouseY < openDropdownY || mouseY > openDropdownY + openDropdownH) {
            rememberDropdownScroll(openDropdownTarget, openDropdownScroll);
            openDropdownTarget = null;
            return false;
        }
        List<String> options = dropdownOptions(openDropdownTarget);
        int index = openDropdownScroll + (int) ((mouseY - openDropdownY) / ENTRY_ROW_H);
        if (index >= 0 && index < options.size()) {
            applyDropdownSelection(openDropdownTarget, options.get(index));
        }
        rememberDropdownScroll(openDropdownTarget, openDropdownScroll);
        openDropdownTarget = null;
        return true;
    }

    private void renderDropdownMenu(GuiGraphics gg, int mouseX, int mouseY) {
        if (openDropdownTarget == null) return;
        List<String> options = dropdownOptions(openDropdownTarget);
        gg.fill(openDropdownX, openDropdownY, openDropdownX + openDropdownW, openDropdownY + openDropdownH, ID_SUGGESTION_BG_COLOR);
        gg.fill(openDropdownX, openDropdownY, openDropdownX + openDropdownW, openDropdownY + 1, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(openDropdownX, openDropdownY + openDropdownH - 1, openDropdownX + openDropdownW, openDropdownY + openDropdownH, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(openDropdownX, openDropdownY, openDropdownX + 1, openDropdownY + openDropdownH, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(openDropdownX + openDropdownW - 1, openDropdownY, openDropdownX + openDropdownW, openDropdownY + openDropdownH, ID_SUGGESTION_BORDER_COLOR);
        int visible = Math.min(5, options.size());
        int start = Mth.clamp(openDropdownScroll, 0, Math.max(0, options.size() - visible));
        int end = Math.min(options.size(), start + visible);
        for (int i = start; i < end; i++) {
            int row = i - start;
            int y = openDropdownY + row * ENTRY_ROW_H;
            boolean hovered = mouseX >= openDropdownX && mouseX <= openDropdownX + openDropdownW && mouseY >= y && mouseY < y + ENTRY_ROW_H;
            if (hovered) gg.fill(openDropdownX + 1, y, openDropdownX + openDropdownW - 1, y + ENTRY_ROW_H, 0xFF2D2D2D);
            gg.drawString(font, options.get(i), openDropdownX + 3, y + 2, ID_SUGGESTION_TEXT_COLOR, false);
        }
        if (options.size() > visible) {
            int trackX0 = openDropdownX + openDropdownW - ID_SUGGESTION_SCROLL_W - 1;
            int trackY0 = openDropdownY + 1;
            int trackH = openDropdownH - 2;
            gg.fill(trackX0, trackY0, trackX0 + ID_SUGGESTION_SCROLL_W, trackY0 + trackH, ID_SUGGESTION_SCROLL_TRACK_COLOR);
            int maxStart = options.size() - visible;
            int thumbH = Math.max(6, (trackH * visible) / options.size());
            int thumbTravel = Math.max(0, trackH - thumbH);
            int thumbY = trackY0 + (maxStart <= 0 ? 0 : (openDropdownScroll * thumbTravel) / maxStart);
            gg.fill(trackX0, thumbY, trackX0 + ID_SUGGESTION_SCROLL_W, thumbY + thumbH, ID_SUGGESTION_SCROLL_THUMB_COLOR);
        }
    }

    private boolean scrollDropdownMenu(double mouseX, double mouseY, double delta) {
        if (openDropdownTarget == null) return false;
        if (mouseX < openDropdownX || mouseX > openDropdownX + openDropdownW || mouseY < openDropdownY || mouseY > openDropdownY + openDropdownH) return false;
        List<String> options = dropdownOptions(openDropdownTarget);
        int visible = Math.min(5, options.size());
        int maxStart = Math.max(0, options.size() - visible);
        openDropdownScroll = Mth.clamp(openDropdownScroll - (delta > 0 ? 1 : -1), 0, maxStart);
        rememberDropdownScroll(openDropdownTarget, openDropdownScroll);
        return true;
    }

    private int savedDropdownScroll(DropdownMenuTarget target) {
        return switch (target) {
            case SUBCATEGORY_PARENT -> subCategoryParentDropdownScroll;
            case QUEST_CATEGORY -> questCategoryDropdownScroll;
            case QUEST_SUBCATEGORY -> questSubCategoryDropdownScroll;
        };
    }

    private void rememberDropdownScroll(DropdownMenuTarget target, int scroll) {
        if (target == null) return;
        int next = Math.max(0, scroll);
        switch (target) {
            case SUBCATEGORY_PARENT -> subCategoryParentDropdownScroll = next;
            case QUEST_CATEGORY -> questCategoryDropdownScroll = next;
            case QUEST_SUBCATEGORY -> questSubCategoryDropdownScroll = next;
        }
    }

    private List<String> dropdownOptions(DropdownMenuTarget target) {
        List<String> out = new ArrayList<>();
        out.add("None");
        if (target == DropdownMenuTarget.SUBCATEGORY_PARENT || target == DropdownMenuTarget.QUEST_CATEGORY) {
            for (String id : categorySuggestionCache) {
                if (id == null || id.isBlank()) continue;
                if ("all".equalsIgnoreCase(id.trim())) continue;
                if (!out.contains(id)) out.add(id);
            }
            return out;
        }
        String categoryId = dropdownBoxValue(questCategoryBox);
        if (categoryId.isBlank()) return out;
        String key = categoryId.toLowerCase(Locale.ROOT);
        List<String> scoped = subCategoryByCategorySuggestion.getOrDefault(key, List.of());
        for (String id : scoped) {
            if (id == null || id.isBlank()) continue;
            if (!out.contains(id)) out.add(id);
        }
        return out;
    }

    private void applyDropdownSelection(DropdownMenuTarget target, String value) {
        String normalized = normalizeDropdownValue(value);
        switch (target) {
            case SUBCATEGORY_PARENT -> setDropdownBoxValue(subCategoryBox, normalized);
            case QUEST_CATEGORY -> {
                String previous = dropdownBoxValue(questCategoryBox);
                setDropdownBoxValue(questCategoryBox, normalized);
                if (!Objects.equals(previous, normalized)) {
                    setDropdownBoxValue(questSubCategoryBox, "");
                }
                if (editorType == EditorType.QUEST) {
                    applyQuestEditorFields();
                }
            }
            case QUEST_SUBCATEGORY -> setDropdownBoxValue(questSubCategoryBox, normalized);
        }
    }

    private List<String> entryTypeOptions(EntryRowKind kind) {
        if (kind == EntryRowKind.COMPLETION) return List.of("collect", "submit", "kill", "achieve", "effect", "xp", "levelup", "field");
        if (kind == EntryRowKind.REWARD) return List.of("item", "xp", "command", "loot");
        return List.of();
    }

    private void setRowType(EntryRowKind kind, int row, String type) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (row < 0 || row >= rows.size()) return;
        ScaledMultiLineEditBox box = rows.get(row);
        String normalized = normalizeRowType(kind, type);
        entryTypeByBox.put(box, normalized);
        if (pickerModeForType(kind, normalized) == PickerMode.NONE) {
            selectedItemIdByBox.remove(box);
            selectedItemComponentsByBox.remove(box);
        }
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    private boolean isItemPickerOpen() {
        return itemPickerKind != null && itemPickerRow >= 0;
    }

    private void openItemPicker(EntryRowKind kind, int row) {
        if (row < 0 || row >= entryRows(kind).size()) return;
        String type = effectiveRowType(kind, entryRows(kind).get(row));
        PickerMode mode = pickerModeForType(kind, type);
        if (mode == PickerMode.NONE) return;
        itemPickerKind = kind;
        itemPickerRow = row;
        pickerMode = mode;
        itemPickerPage = 0;
        if (itemPickerSearchBox == null) {
            itemPickerSearchBox = new EditBox(font, 0, 0, ITEM_PICKER_W - 12, 14, Component.literal("Search"));
            itemPickerSearchBox.setMaxLength(128);
            addRenderableWidget(itemPickerSearchBox);
        }
        itemPickerSearchBox.setValue("");
        itemPickerSearchBox.setFocused(true);
        itemPickerSearchBox.visible = true;
        itemPickerSearchBox.active = true;
    }

    private void closeItemPicker() {
        itemPickerKind = null;
        itemPickerRow = -1;
        if (itemPickerSearchBox != null) {
            itemPickerSearchBox.visible = false;
            itemPickerSearchBox.active = false;
            itemPickerSearchBox.setFocused(false);
        }
    }

    private boolean clickItemPicker(double mouseX, double mouseY) {
        if (!isItemPickerOpen()) return false;
        int x = (width - ITEM_PICKER_W) / 2;
        int y = (height - ITEM_PICKER_H) / 2;
        int closeX = x + ITEM_PICKER_W - 11;
        int closeY = y + 3;
        if (mouseX >= closeX && mouseX <= closeX + 8 && mouseY >= closeY && mouseY <= closeY + 8) {
            closeItemPicker();
            return true;
        }
        if (itemPickerSearchBox != null && itemPickerSearchBox.isMouseOver(mouseX, mouseY)) {
            return itemPickerSearchBox.mouseClicked(mouseX, mouseY, 0);
        }
        if (mouseX < x || mouseX > x + ITEM_PICKER_W || mouseY < y || mouseY > y + ITEM_PICKER_H) {
            closeItemPicker();
            return true;
        }
        if (pickerMode == PickerMode.ITEMS && mouseY >= y + 18 && mouseY <= y + 30) {
            itemPickerTab = mouseX < x + ITEM_PICKER_W / 2 ? ItemPickerTab.CREATIVE : ItemPickerTab.INVENTORY;
            itemPickerPage = 0;
            return true;
        }
        int gridX = x + 5;
        int gridY = y + 24;
        if (mouseX >= gridX && mouseX < gridX + ITEM_PICKER_COLS * ITEM_PICKER_CELL
                && mouseY >= gridY && mouseY < gridY + ITEM_PICKER_ROWS * ITEM_PICKER_CELL) {
            int col = (int) ((mouseX - gridX) / ITEM_PICKER_CELL);
            int row = (int) ((mouseY - gridY) / ITEM_PICKER_CELL);
            int idx = row * ITEM_PICKER_COLS + col;
            List<ItemStack> items = itemPickerDisplayItems();
            int start = itemPickerPage * (ITEM_PICKER_COLS * ITEM_PICKER_ROWS);
            int at = start + idx;
            if (at >= 0 && at < items.size()) {
                if (pickerMode == PickerMode.MOBS) {
                    List<String> mobIds = mobPickerIds();
                    if (at < mobIds.size()) applyPickedMob(mobIds.get(at));
                } else {
                    applyPickedItem(items.get(at));
                }
                closeItemPicker();
            }
            return true;
        }
        return true;
    }

    private boolean scrollItemPicker(double mouseX, double mouseY, double delta) {
        if (!isItemPickerOpen()) return false;
        int x = (width - ITEM_PICKER_W) / 2;
        int y = (height - ITEM_PICKER_H) / 2;
        if (mouseX < x || mouseX > x + ITEM_PICKER_W || mouseY < y || mouseY > y + ITEM_PICKER_H) return false;
        int pageSize = ITEM_PICKER_COLS * ITEM_PICKER_ROWS;
        int maxPage = Math.max(0, (itemPickerDisplayItems().size() - 1) / pageSize);
        itemPickerPage = Mth.clamp(itemPickerPage - (delta > 0 ? 1 : -1), 0, maxPage);
        return true;
    }

    private void renderItemPicker(GuiGraphics gg, int mouseX, int mouseY) {
        if (!isItemPickerOpen()) return;
        int x = (width - ITEM_PICKER_W) / 2;
        int y = (height - ITEM_PICKER_H) / 2;
        gg.fill(x, y, x + ITEM_PICKER_W, y + ITEM_PICKER_H, 0xFF000000);
        gg.fill(x, y, x + ITEM_PICKER_W, y + 1, 0xFFFFFFFF);
        gg.fill(x, y + ITEM_PICKER_H - 1, x + ITEM_PICKER_W, y + ITEM_PICKER_H, 0xFFFFFFFF);
        gg.fill(x, y, x + 1, y + ITEM_PICKER_H, 0xFFFFFFFF);
        gg.fill(x + ITEM_PICKER_W - 1, y, x + ITEM_PICKER_W, y + ITEM_PICKER_H, 0xFFFFFFFF);
        String title = switch (pickerMode) {
            case EFFECTS -> "Effect Browser";
            case MOBS -> "Mob Browser";
            default -> "Item Browser";
        };
        gg.drawString(font, title, x + (ITEM_PICKER_W - font.width(title)) / 2, y + 4, 0xFFFFFFFF, false);
        gg.drawString(font, "X", x + ITEM_PICKER_W - 10, y + 3, 0xFFFFFF00, false);
        if (pickerMode == PickerMode.ITEMS) {
            renderPickerTab(gg, mouseX, mouseY, x + 4, y + 12, ITEM_PICKER_W / 2 - 6, "All Items", itemPickerTab == ItemPickerTab.CREATIVE);
            renderPickerTab(gg, mouseX, mouseY, x + ITEM_PICKER_W / 2 + 1, y + 12, ITEM_PICKER_W / 2 - 5, "Inventory", itemPickerTab == ItemPickerTab.INVENTORY);
        }
        if (itemPickerSearchBox != null) {
            itemPickerSearchBox.setX(x + 4);
            itemPickerSearchBox.setY(y + ITEM_PICKER_H - 16);
            itemPickerSearchBox.setWidth(ITEM_PICKER_W - 10);
            itemPickerSearchBox.setTextColor(0xFFFFFF00);
            itemPickerSearchBox.visible = true;
        }
        List<ItemStack> items = itemPickerDisplayItems();
        List<String> mobIds = pickerMode == PickerMode.MOBS ? mobPickerIds() : List.of();
        int gridX = x + 5;
        int gridY = y + 24;
        int start = itemPickerPage * (ITEM_PICKER_COLS * ITEM_PICKER_ROWS);
        ItemStack hovered = ItemStack.EMPTY;
        int hoveredIndex = -1;
        for (int i = 0; i < ITEM_PICKER_COLS * ITEM_PICKER_ROWS; i++) {
            int at = start + i;
            int col = i % ITEM_PICKER_COLS;
            int row = i / ITEM_PICKER_COLS;
            int sx = gridX + col * ITEM_PICKER_CELL;
            int sy = gridY + row * ITEM_PICKER_CELL;
            boolean cellHover = mouseX >= sx && mouseX <= sx + 16 && mouseY >= sy && mouseY <= sy + 16;
            gg.fill(sx - 1, sy - 1, sx + 17, sy + 17, cellHover ? 0xFFFFFFFF : 0xFF444444);
            gg.fill(sx, sy, sx + 16, sy + 16, 0xFF111111);
            if (at < items.size()) {
                ItemStack stack = items.get(at);
                if (pickerMode == PickerMode.EFFECTS) {
                    renderEffectPickerIcon(gg, stack, sx, sy);
                } else if (pickerMode == PickerMode.MOBS && at < mobIds.size()) {
                    renderMobPreview(gg, mobIds.get(at), sx, sy);
                } else {
                    gg.renderItem(stack, sx, sy);
                }
                if (cellHover) {
                    hovered = stack;
                    hoveredIndex = at;
                }
            }
        }
        if (!hovered.isEmpty()) {
            if (pickerMode == PickerMode.MOBS && hoveredIndex >= 0 && hoveredIndex < items.size()) {
                String mobId = hoveredIndex < mobIds.size() ? mobIds.get(hoveredIndex) : "";
                Component mobName = mobDisplayNameForMobId(mobId);
                gg.renderTooltip(font, mobName, mouseX, mouseY);
            } else if (pickerMode == PickerMode.EFFECTS) {
                gg.renderTooltip(font, effectDisplayNameForPickerItem(hovered), mouseX, mouseY);
            } else {
                gg.renderTooltip(font, hovered, mouseX, mouseY);
            }
        }
        if (itemPickerSearchBox != null) {
            itemPickerSearchBox.render(gg, mouseX, mouseY, 0f);
        }
    }

    private void renderPickerTab(GuiGraphics gg, int mouseX, int mouseY, int x, int y, int w, String label, boolean selected) {
        gg.fill(x, y, x + w, y + 10, selected ? 0xFF1C1C1C : 0xFF050505);
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 10;
        gg.drawString(font, label, x + 2, y + 1, hovered ? 0xFFFFFF00 : 0xFFFFFFFF, false);
    }

    private List<ItemStack> itemPickerItems() {
        String query = safe(itemPickerSearchBox == null ? "" : itemPickerSearchBox.getValue()).trim().toLowerCase(Locale.ROOT);
        List<ItemStack> out = new ArrayList<>();
        if (itemPickerTab == ItemPickerTab.CREATIVE) {
            ensureItemIdCache();
            for (String id : itemIdCache) {
                if ("minecraft:air".equals(id)) continue;
                if (!query.isBlank() && !id.contains(query)) continue;
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl == null) continue;
                Item item = BuiltInRegistries.ITEM.get(rl);
                if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
                out.add(new ItemStack(item));
            }
            return out;
        }
        if (minecraft.player == null) return out;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (stack == null || stack.isEmpty()) continue;
            if (stack.getItem() == net.minecraft.world.item.Items.AIR) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!query.isBlank() && !id.contains(query)) continue;
            out.add(stack.copy());
        }
        return out;
    }

    private List<ItemStack> itemPickerDisplayItems() {
        if (pickerMode == PickerMode.ITEMS) return itemPickerItems();
        String query = safe(itemPickerSearchBox == null ? "" : itemPickerSearchBox.getValue()).trim().toLowerCase(Locale.ROOT);
        List<ItemStack> out = new ArrayList<>();
        switch (pickerMode) {
            case EFFECTS -> {
                ensureEffectIdCache();
                for (String effectId : effectIdCache) {
                    if (!query.isBlank() && !effectId.toLowerCase(Locale.ROOT).contains(query)) continue;
                    out.add(effectIconStack(effectId));
                }
            }
            case MOBS -> {
                for (String ignored : mobPickerIds()) out.add(new ItemStack(net.minecraft.world.item.Items.EGG));
            }
            default -> {
            }
        }
        return out;
    }

    private List<String> mobPickerIds() {
        ensureEntityIdCache();
        String query = safe(itemPickerSearchBox == null ? "" : itemPickerSearchBox.getValue()).trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String entityId : entityIdCache) {
            if (!isSelectableMobEntityId(entityId)) continue;
            if (!query.isBlank() && !entityId.toLowerCase(Locale.ROOT).contains(query)) continue;
            out.add(entityId);
        }
        return out;
    }

    private boolean isSelectableMobEntityId(String entityId) {
        ResourceLocation rl = ResourceLocation.tryParse(safe(entityId).trim());
        if (rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) return false;
        if ("minecraft".equals(rl.getNamespace()) && "giant".equals(rl.getPath())) return false;
        MobCategory category = BuiltInRegistries.ENTITY_TYPE.get(rl).getCategory();
        return category != MobCategory.MISC;
    }

    private void applyPickedItem(ItemStack picked) {
        if (!isItemPickerOpen()) return;
        List<ScaledMultiLineEditBox> rows = entryRows(itemPickerKind);
        if (itemPickerRow < 0 || itemPickerRow >= rows.size() || picked == null || picked.isEmpty()) return;
        ScaledMultiLineEditBox box = rows.get(itemPickerRow);
        String id;
        if (pickerMode == PickerMode.EFFECTS) {
            id = effectIdFromStack(picked);
        } else if (pickerMode == PickerMode.MOBS) {
            id = mobIdFromStack(picked);
        } else {
            id = BuiltInRegistries.ITEM.getKey(picked.getItem()).toString();
        }
        if (id == null || id.isBlank()) return;
        selectedItemIdByBox.put(box, id);
        if (pickerMode == PickerMode.ITEMS && itemPickerTab == ItemPickerTab.INVENTORY) {
            String components = safe(describeStackComponents(picked)).trim();
            if (components.isBlank() || "{}".equals(components)) selectedItemComponentsByBox.remove(box);
            else selectedItemComponentsByBox.put(box, components);
        } else {
            selectedItemComponentsByBox.remove(box);
        }
        String itemName = picked.getHoverName().getString();
        String value = switch (pickerMode) {
            case MOBS -> itemName + " 1";
            case EFFECTS -> itemName;
            default -> itemName
                    + (safe(selectedItemComponentsByBox.get(box)).isBlank() ? "" : " " + safe(selectedItemComponentsByBox.get(box)).trim())
                    + " " + Math.max(1, picked.getCount());
        };
        box.setValue(value);
        box.setFocused(true);
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    private void applyPickedMob(String mobId) {
        if (!isItemPickerOpen()) return;
        List<ScaledMultiLineEditBox> rows = entryRows(itemPickerKind);
        if (itemPickerRow < 0 || itemPickerRow >= rows.size()) return;
        if (mobId == null || mobId.isBlank()) return;
        ScaledMultiLineEditBox box = rows.get(itemPickerRow);
        selectedItemIdByBox.put(box, mobId);
        selectedItemComponentsByBox.remove(box);
        box.setValue(mobDisplayNameForMobId(mobId).getString() + " 1");
        box.setFocused(true);
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    private void renderEffectPickerIcon(GuiGraphics gg, ItemStack stack, int x, int y) {
        String effectId = effectIdFromStack(stack);
        ResourceLocation rl = ResourceLocation.tryParse(effectId);
        if (rl != null) {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/effects/" + rl.getPath() + ".png");
            if (Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()) {
                gg.blit(tex, x, y, 0, 0, 16, 16, 16, 16);
                return;
            }
        }
        gg.renderItem(new ItemStack(net.minecraft.world.item.Items.POTION), x, y);
    }

    private ItemStack effectIconStack(String effectId) {
        ResourceLocation rl = ResourceLocation.tryParse(effectId);
        if (rl != null) {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(
                    "boundless",
                    "textures/gui/effects/" + rl.getPath() + ".png");
            if (Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()) {
                ItemStack stack = new ItemStack(net.minecraft.world.item.Items.POTION);
                stack.set(net.minecraft.core.component.DataComponents.ITEM_NAME, Component.literal(effectId));
                return stack;
            }
        }
        return new ItemStack(net.minecraft.world.item.Items.POTION);
    }

    private ItemStack mobEggIconStack(String entityId) {
        ResourceLocation entityRl = ResourceLocation.tryParse(entityId);
        if (entityRl == null) return ItemStack.EMPTY;
        ResourceLocation eggRl = ResourceLocation.fromNamespaceAndPath(entityRl.getNamespace(), entityRl.getPath() + "_spawn_egg");
        if (BuiltInRegistries.ITEM.containsKey(eggRl)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(eggRl));
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof SpawnEggItem)) continue;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key != null && key.getPath().contains(entityRl.getPath())) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    private Component mobDisplayNameForMobId(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            return BuiltInRegistries.ENTITY_TYPE.get(rl).getDescription();
        }
        return Component.literal(id);
    }

    private Component effectDisplayNameForPickerItem(ItemStack stack) {
        String id = effectIdFromStack(stack);
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && BuiltInRegistries.MOB_EFFECT.containsKey(rl)) {
            return BuiltInRegistries.MOB_EFFECT.get(rl).getDisplayName();
        }
        return Component.literal(id);
    }

    private String effectIdFromStack(ItemStack stack) {
        String name = stack.getHoverName().getString();
        if (name == null || name.isBlank()) return "";
        for (String effectId : effectIdCache) {
            if (effectId.equals(name)) return effectId;
        }
        return "";
    }

    private String mobIdFromStack(ItemStack stack) {
        Item item = stack.getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return "";
        String path = key.getPath();
        if (!path.endsWith("_spawn_egg")) return "";
        String entityPath = path.substring(0, path.length() - "_spawn_egg".length());
        String id = key.getNamespace() + ":" + entityPath;
        return entityIdCache.contains(id) ? id : "";
    }

    private void renderMobPreview(GuiGraphics gg, String mobId, int x, int y) {
        if (minecraft == null || minecraft.level == null) return;
        ResourceLocation rl = ResourceLocation.tryParse(mobId);
        if (rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) return;
        Entity entity = BuiltInRegistries.ENTITY_TYPE.get(rl).create(minecraft.level);
        if (!(entity instanceof LivingEntity living)) return;
        InventoryScreen.renderEntityInInventoryFollowsMouse(gg, x, y, x + 16, y + 16, 8, 0f, 0f, 0f, living);
    }

    private ItemStack selectedItemForRow(EntryRowKind kind, int row) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (row < 0 || row >= rows.size()) return ItemStack.EMPTY;
        ScaledMultiLineEditBox box = rows.get(row);
        String type = effectiveRowType(kind, box);
        String idPart = safe(selectedItemIdByBox.get(box)).trim();
        if (idPart.isBlank()) {
            ParsedEntry parsed = parseEntry(composeEntryRowLine(kind, box));
            if (parsed == null || parsed.id.isBlank()) return ItemStack.EMPTY;
            idPart = parsed.id.trim().split("\\s+")[0];
        }
        if ("effect".equals(type)) return effectIconStack(idPart);
        if ("kill".equals(type) || "entity".equals(type)) return mobEggIconStack(idPart);
        String normalized = normalizeNamespacedId(idPart, false);
        ResourceLocation rl = ResourceLocation.tryParse(normalized);
        if (rl == null) return ItemStack.EMPTY;
        if (!BuiltInRegistries.ITEM.containsKey(rl)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.get(rl));
    }

    private String displayNameForItem(String itemId) {
        ResourceLocation rl = ResourceLocation.tryParse(normalizeNamespacedId(itemId, false));
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return itemId;
        return new ItemStack(BuiltInRegistries.ITEM.get(rl)).getHoverName().getString();
    }

    private String describeStackComponents(ItemStack stack) {
        try {
            Method m = stack.getClass().getMethod("getComponentsPatch");
            Object patch = m.invoke(stack);
            return patch == null ? "" : patch.toString();
        } catch (Exception ignored) {
        }
        try {
            Method m = stack.getClass().getMethod("getComponents");
            Object comps = m.invoke(stack);
            return comps == null ? "" : comps.toString();
        } catch (Exception ignored) {
        }
        return "";
    }

    private boolean isIconBox(EditBox box) {
        return box == packIconPathBox || box == questIconBox || box == catIconBox || box == subIconBox;
    }

    private EditBox focusedEditBox() {
        if (packIconPathBox != null && packIconPathBox.isFocused()) return packIconPathBox;
        if (questIconBox != null && questIconBox.isFocused()) return questIconBox;
        if (catIconBox != null && catIconBox.isFocused()) return catIconBox;
        if (subIconBox != null && subIconBox.isFocused()) return subIconBox;
        return null;
    }

    private void ensureItemIdCache() {
        if (!itemIdCache.isEmpty()) return;
        for (ResourceLocation rl : BuiltInRegistries.ITEM.keySet()) {
            if (rl != null) itemIdCache.add(rl.toString());
        }
        itemIdCache.sort(String::compareTo);
    }

    private void ensureEntityIdCache() {
        if (!entityIdCache.isEmpty()) return;
        for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (rl != null) entityIdCache.add(rl.toString());
        }
        entityIdCache.sort(String::compareTo);
    }

    private void ensureEffectIdCache() {
        if (!effectIdCache.isEmpty()) return;
        for (ResourceLocation rl : BuiltInRegistries.MOB_EFFECT.keySet()) {
            if (rl != null) effectIdCache.add(rl.toString());
        }
        effectIdCache.sort(String::compareTo);
    }

    private List<String> itemSuggestions() {
        ensureItemIdCache();
        return itemIdCache;
    }

    private List<String> entitySuggestions() {
        ensureEntityIdCache();
        return entityIdCache;
    }

    private List<String> effectSuggestions() {
        ensureEffectIdCache();
        return effectIdCache;
    }

    private void ensureAdvancementIdCache() {
        if (!advancementIdCache.isEmpty()) return;
        for (QuestData.Quest q : QuestData.all()) {
            if (q == null || q.completion == null || q.completion.targets == null) continue;
            for (QuestData.Target t : q.completion.targets) {
                if (t != null && t.isAdvancement() && t.id != null && !t.id.isBlank() && !advancementIdCache.contains(t.id)) {
                    advancementIdCache.add(t.id);
                }
            }
        }
        if (Minecraft.getInstance().getConnection() != null) {
            try {
                Object advancements = Minecraft.getInstance().getConnection().getAdvancements();
                Object tree = invokeObject(advancements, "getTree", "tree");
                Object roots = invokeObject(tree, "roots", "getRoots");
                if (roots instanceof Iterable<?> iterable) {
                    for (Object node : iterable) {
                        Object holder = invokeObject(node, "holder", "getHolder", "advancement");
                        Object id = invokeObject(holder, "id", "getId");
                        if (id != null) {
                            String sid = id.toString();
                            if (!sid.isBlank() && !advancementIdCache.contains(sid)) advancementIdCache.add(sid);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        advancementIdCache.sort(String::compareTo);
    }

    private List<String> advancementSuggestions() {
        ensureAdvancementIdCache();
        return advancementIdCache;
    }

    private void ensureLootTableIdCache() {
        if (!lootTableIdCache.isEmpty()) return;
        Set<String> found = new LinkedHashSet<>();

        try {
            Minecraft.getInstance().getResourceManager()
                    .listResources("loot_tables", path -> path != null && path.getPath().endsWith(".json"))
                    .keySet()
                    .forEach(rl -> addLootTableResourceLocation(rl, found));
            Minecraft.getInstance().getResourceManager()
                    .listResources("loot_table", path -> path != null && path.getPath().endsWith(".json"))
                    .keySet()
                    .forEach(rl -> addLootTableResourceLocation(rl, found));
        } catch (Exception ignored) {
        }

        collectLootTablesFromVersionJar(found);

        if (currentPack != null && currentPack.root != null) {
            collectLootTablesFromDirectory(currentPack.root, found);
        }

        Path rpRoot = resourcePacksRoot();
        if (Files.isDirectory(rpRoot)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rpRoot)) {
                for (Path entry : stream) {
                    if (entry == null || !Files.exists(entry)) continue;
                    if (Files.isDirectory(entry)) {
                        collectLootTablesFromDirectory(entry, found);
                    } else if (entry.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                        collectLootTablesFromZip(entry, found);
                    }
                }
            } catch (IOException ignored) {
            }
        }

        lootTableIdCache.addAll(found);
        lootTableIdCache.sort(String::compareTo);
    }

    private void collectLootTablesFromDirectory(Path root, Set<String> out) {
        if (root == null || out == null || !Files.isDirectory(root)) return;
        try (var walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .forEach(path -> addLootTablePath(root.relativize(path).toString().replace('\\', '/'), out));
        } catch (IOException ignored) {
        }
    }

    private void collectLootTablesFromZip(Path zipPath, Set<String> out) {
        if (zipPath == null || out == null || !Files.exists(zipPath)) return;
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            zip.stream()
                    .map(ZipEntry::getName)
                    .filter(Objects::nonNull)
                    .map(name -> name.replace('\\', '/'))
                    .forEach(name -> addLootTablePath(name, out));
        } catch (IOException ignored) {
        }
    }

    private void collectLootTablesFromVersionJar(Set<String> out) {
        if (out == null) return;
        Path versionsRoot = Minecraft.getInstance().gameDirectory.toPath().resolve("versions");
        Path jarPath = null;
        try {
            String version = SharedConstants.getCurrentVersion().getName();
            if (version != null && !version.isBlank()) {
                Path exact = versionsRoot.resolve(version).resolve(version + ".jar");
                if (Files.exists(exact)) jarPath = exact;
            }
        } catch (Exception ignored) {
        }
        if (jarPath == null && Files.isDirectory(versionsRoot)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionsRoot)) {
                List<Path> candidates = new ArrayList<>();
                for (Path entry : stream) {
                    if (entry == null || !Files.isDirectory(entry)) continue;
                    String name = entry.getFileName().toString();
                    if (!name.startsWith("1.21")) continue;
                    Path candidate = entry.resolve(name + ".jar");
                    if (Files.exists(candidate)) candidates.add(candidate);
                }
                candidates.sort(Comparator.comparing(Path::toString).reversed());
                if (!candidates.isEmpty()) jarPath = candidates.get(0);
            } catch (IOException ignored) {
            }
        }
        if (jarPath != null) {
            collectLootTablesFromZip(jarPath, out);
        }
    }

    private void addLootTablePath(String path, Set<String> out) {
        if (path == null || out == null) return;
        String clean = path.replace('\\', '/');
        if (!clean.endsWith(".json") || !clean.startsWith("data/")) return;

        String marker;
        if (clean.contains("/loot_tables/")) marker = "/loot_tables/";
        else if (clean.contains("/loot_table/")) marker = "/loot_table/";
        else return;

        int namespaceStart = "data/".length();
        int markerIndex = clean.indexOf(marker, namespaceStart);
        if (markerIndex <= namespaceStart) return;

        String namespace = clean.substring(namespaceStart, markerIndex);
        String idPath = clean.substring(markerIndex + marker.length(), clean.length() - ".json".length());
        if (namespace.isBlank() || idPath.isBlank()) return;
        addLootTableId(namespace, idPath, out);
    }

    private void addLootTableResourceLocation(ResourceLocation rl, Set<String> out) {
        if (rl == null || out == null) return;
        String path = rl.getPath();
        if (path == null || path.isBlank()) return;

        String marker;
        if (path.startsWith("loot_tables/")) marker = "loot_tables/";
        else if (path.startsWith("loot_table/")) marker = "loot_table/";
        else return;

        String idPath = path.substring(marker.length());
        if (idPath.endsWith(".json")) {
            idPath = idPath.substring(0, idPath.length() - ".json".length());
        }
        addLootTableId(rl.getNamespace(), idPath, out);
    }

    private void addLootTableId(String namespace, String idPath, Set<String> out) {
        if (namespace == null || namespace.isBlank() || idPath == null || idPath.isBlank() || out == null) return;
        String normalizedPath = idPath.replace('\\', '/');
        if (!(normalizedPath.startsWith("chests/") || normalizedPath.startsWith("entities/"))) return;
        if ("minecraft".equals(namespace)) out.add(normalizedPath);
        else out.add(namespace + ":" + normalizedPath);
    }

    private List<String> lootTableSuggestions() {
        ensureLootTableIdCache();
        if (lootTableIdCache.isEmpty()) {
            populateFallbackLootTables(lootTableIdCache);
        }
        return lootTableIdCache;
    }

    private void populateFallbackLootTables(List<String> out) {
        if (out == null) return;
        Set<String> found = new LinkedHashSet<>(out);
        String[] chestDefaults = {
                "chests/abandoned_mineshaft",
                "chests/ancient_city",
                "chests/ancient_city_ice_box",
                "chests/bastion_bridge",
                "chests/bastion_hoglin_stable",
                "chests/bastion_other",
                "chests/bastion_treasure",
                "chests/buried_treasure",
                "chests/desert_pyramid",
                "chests/end_city_treasure",
                "chests/igloo_chest",
                "chests/jungle_temple",
                "chests/jungle_temple_dispenser",
                "chests/nether_bridge",
                "chests/pillager_outpost",
                "chests/ruined_portal",
                "chests/shipwreck_map",
                "chests/shipwreck_supply",
                "chests/shipwreck_treasure",
                "chests/simple_dungeon",
                "chests/spawn_bonus_chest",
                "chests/stronghold_corridor",
                "chests/stronghold_crossing",
                "chests/stronghold_library",
                "chests/trial_chambers/corridor",
                "chests/trial_chambers/entrance",
                "chests/trial_chambers/intersection",
                "chests/trial_chambers/reward",
                "chests/trial_chambers/reward_common",
                "chests/trial_chambers/reward_ominous",
                "chests/trial_chambers/supply",
                "chests/underwater_ruin_big",
                "chests/underwater_ruin_small",
                "chests/village/village_armorer",
                "chests/village/village_butcher",
                "chests/village/village_cartographer",
                "chests/village/village_desert_house",
                "chests/village/village_fisher",
                "chests/village/village_fletcher",
                "chests/village/village_mason",
                "chests/village/village_plains_house",
                "chests/village/village_savanna_house",
                "chests/village/village_shepherd",
                "chests/village/village_snowy_house",
                "chests/village/village_taiga_house",
                "chests/village/village_tannery",
                "chests/village/village_temple",
                "chests/village/village_toolsmith",
                "chests/village/village_weaponsmith",
                "chests/woodland_mansion"
        };
        for (String chest : chestDefaults) found.add(chest);
        for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (rl != null) found.add("entities/" + rl.getPath());
        }
        out.clear();
        out.addAll(found);
        out.sort(String::compareTo);
    }

    private String computeIconSuggestion(String raw) {
        if (raw == null) return "";
        String input = raw.trim().toLowerCase(Locale.ROOT);
        if (input.isBlank()) return "";
        ensureItemIdCache();
        String suggestion = findSuggestion(itemIdCache, input);
        return suggestion == null ? "" : suggestion;
    }

    private String findSuggestion(List<String> cache, String prefix) {
        if (cache == null || cache.isEmpty() || prefix == null || prefix.isBlank()) return "";
        String p = prefix.toLowerCase(Locale.ROOT);
        boolean namespaced = p.contains(":");
        for (String id : cache) {
            if (id == null || id.isBlank()) continue;
            String low = id.toLowerCase(Locale.ROOT);
            if (low.startsWith(p)) return id;
            if (!namespaced) {
                int colon = low.indexOf(':');
                if (colon >= 0 && colon + 1 < low.length() && low.substring(colon + 1).startsWith(p)) {
                    return id;
                }
            }
        }
        return "";
    }

    private void renderFooter(GuiGraphics gg, int mouseX, int mouseY) {
        Component footer = footerComponent();
        int maxWidth = Math.max(80, this.width - 12);
        footerLines.clear();
        footerLines.addAll(font.split(footer, maxWidth));
        footerLineX.clear();
        int totalHeight = footerLines.size() * font.lineHeight;
        footerStartY = this.height - totalHeight - 4;
        int y = footerStartY;
        for (FormattedCharSequence line : footerLines) {
            int lineWidth = font.width(line);
            int x = (this.width - lineWidth) / 2;
            footerLineX.add(x);
            gg.drawString(font, line, x, y, 0xFFFFFF, false);
            y += font.lineHeight;
        }

        Style style = getFooterStyleAt(mouseX, mouseY);
        if (style != null) {
            gg.renderComponentHoverEffect(font, style, mouseX, mouseY);
        }
    }

    private Component footerComponent() {
        Style red = Style.EMPTY.withColor(FOOTER_TEXT_COLOR);
        Style link = Style.EMPTY
                .withColor(FOOTER_LINK_COLOR)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, FOOTER_LINK))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(FOOTER_LINK)));
        return Component.literal(FOOTER_PREFIX).withStyle(red)
                .append(Component.literal(FOOTER_LINK).withStyle(link));
    }

    private Style getFooterStyleAt(int mouseX, int mouseY) {
        if (footerLines.isEmpty()) return null;
        int y = footerStartY;
        for (int i = 0; i < footerLines.size(); i++) {
            int lineHeight = font.lineHeight;
            if (mouseY >= y && mouseY < y + lineHeight) {
                int x = footerLineX.get(i);
                int width = font.width(footerLines.get(i));
                if (mouseX >= x && mouseX <= x + width) {
                    return Minecraft.getInstance().font.getSplitter()
                            .componentStyleAtWidth(footerLines.get(i), mouseX - x);
                }
                return null;
            }
            y += lineHeight;
        }
        return null;
    }

    private static final class ScaledMultiLineEditBox extends AbstractScrollWidget {
        private static final int CURSOR_INSERT_COLOR = -3092272;
        private static final int TEXT_COLOR = -2039584;
        private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
        private static final int CURSOR_BLINK_INTERVAL_MS = 300;
        private final Font font;
        private final Component placeholder;
        private final ScaledTextField textField;
        private final float textScale;
        private final double lineHeight;
        private final boolean allowColorFormatting;
        private long focusedTime = Util.getMillis();
        private int textColor = TEXT_COLOR;

        ScaledMultiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder, Component message, float textScale, boolean allowColorFormatting) {
            super(x, y, width, height, message);
            this.font = font;
            this.placeholder = placeholder;
            this.textScale = textScale <= 0f ? 1f : textScale;
            this.lineHeight = 9.0 * this.textScale;
            this.allowColorFormatting = allowColorFormatting;
            int fieldWidth = Math.max(20, Math.round((width - this.totalInnerPadding()) / this.textScale));
            this.textField = new ScaledTextField(font, fieldWidth, allowColorFormatting);
            this.textField.setCursorListener(this::scrollToCursor);
        }

        public void setCharacterLimit(int characterLimit) {
            this.textField.setCharacterLimit(characterLimit);
        }

        public void setValueListener(Consumer<String> valueListener) {
            this.textField.setValueListener(valueListener);
        }

        public void setValue(String fullText) {
            this.textField.setValue(fullText);
        }

        public void insertText(String text) {
            this.textField.insertText(text);
        }

        public boolean canUndo() {
            return this.textField.canUndo();
        }

        public boolean canRedo() {
            return this.textField.canRedo();
        }

        public void undo() {
            this.textField.undo();
        }

        public void redo() {
            this.textField.redo();
        }

        public void clearHistory() {
            this.textField.clearHistory();
        }

        public void setTextColor(int color) {
            this.textColor = color;
        }

        public String getValue() {
            return this.textField.value();
        }

        public int getCursorPosition() {
            return this.textField.cursor();
        }

        public void setCursorPosition(int cursor) {
            this.textField.seekCursor(Whence.ABSOLUTE, Mth.clamp(cursor, 0, this.textField.value().length()));
        }

        public int getLineCount() {
            return this.textField.getLineCount();
        }

        public double getLineHeight() {
            return this.lineHeight;
        }

        public void scrollToTop() {
            this.setScrollAmount(0.0);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.visible || !this.active) return false;
            if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
                this.setFocused(true);
                this.textField.setSelecting(Screen.hasShiftDown());
                this.seekCursorScreen(mouseX, mouseY);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (!this.visible || !this.active) return false;
            if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            } else if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
                this.textField.setSelecting(true);
                this.seekCursorScreen(mouseX, mouseY);
                this.textField.setSelecting(Screen.hasShiftDown());
                return true;
            }
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return this.textField.keyPressed(keyCode);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (this.visible && this.isFocused() && StringUtil.isAllowedChatCharacter(codePoint)) {
                this.textField.insertText(Character.toString(codePoint));
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
            if (!this.visible || !this.active || !this.isMouseOver(mouseX, mouseY)) return false;
            double maxScroll = Math.max(0.0, this.getInnerHeight() - (this.height - this.totalInnerPadding()));
            if (maxScroll <= 0.0) return false;
            this.setScrollAmount(Mth.clamp(this.scrollAmount() - (deltaY * this.lineHeight), 0.0, maxScroll));
            return true;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            this.renderBackground(guiGraphics);
            guiGraphics.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0, -this.scrollAmount(), 0.0);
            guiGraphics.pose().scale(this.textScale, this.textScale, 1.0f);
            this.renderContents(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();
            this.renderDecorations(guiGraphics);
        }

        @Override
        protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            String text = this.textField.value();
            float invScale = 1.0f / this.textScale;
            int baseX = Math.round((this.getX() + this.innerPadding()) * invScale);
            int baseY = Math.round((this.getY() + this.innerPadding()) * invScale);
            int wrapWidth = Math.round((this.width - this.totalInnerPadding()) * invScale);

            if (text.isEmpty() && !this.isFocused()) {
                guiGraphics.drawWordWrap(this.font, this.placeholder, baseX, baseY, wrapWidth, PLACEHOLDER_TEXT_COLOR);
                return;
            }

            int cursor = this.textField.cursor();
            boolean showCursor = this.isFocused() && (Util.getMillis() - this.focusedTime) / CURSOR_BLINK_INTERVAL_MS % 2L == 0L;
            boolean cursorInText = cursor < text.length();
            int drawX = baseX;
            int activeColor = this.textColor;
            double lineScreenY = this.getY() + this.innerPadding();
            double lastLineScreenY = lineScreenY;

            for (ScaledTextField.StringView line : this.textField.iterateLines()) {
                boolean visible = this.withinContentAreaTopBottom((int) lineScreenY, (int) (lineScreenY + this.lineHeight));
                if (showCursor && cursorInText && cursor >= line.beginIndex() && cursor <= line.endIndex()) {
                    if (visible) {
                        int drawY = Math.round((float) (lineScreenY * invScale));
                        FormattedRenderResult beforeCursor = drawFormattedString(guiGraphics, text.substring(line.beginIndex(), cursor), baseX, drawY, activeColor);
                        drawX = beforeCursor.endX() - 1;
                        activeColor = beforeCursor.finalColor();
                        guiGraphics.fill(drawX, drawY - 1, drawX + 1, drawY + 1 + this.font.lineHeight, CURSOR_INSERT_COLOR);
                        FormattedRenderResult afterCursor = drawFormattedString(guiGraphics, text.substring(cursor, line.endIndex()), drawX, drawY, activeColor);
                        activeColor = afterCursor.finalColor();
                    }
                } else {
                    if (visible) {
                        int drawY = Math.round((float) (lineScreenY * invScale));
                        FormattedRenderResult lineRender = drawFormattedString(guiGraphics, text.substring(line.beginIndex(), line.endIndex()), baseX, drawY, activeColor);
                        drawX = lineRender.endX() - 1;
                        activeColor = lineRender.finalColor();
                    } else {
                        activeColor = drawFormattedString(null, text.substring(line.beginIndex(), line.endIndex()), baseX, 0, activeColor).finalColor();
                    }
                }

                lastLineScreenY = lineScreenY;
                lineScreenY += this.lineHeight;
            }

            if (showCursor && !cursorInText
                    && this.withinContentAreaTopBottom((int) lastLineScreenY, (int) (lastLineScreenY + this.lineHeight))) {
                int drawY = Math.round((float) (lastLineScreenY * invScale));
                guiGraphics.drawString(this.font, "_", drawX, drawY, CURSOR_INSERT_COLOR, false);
            }

            if (this.textField.hasSelection()) {
                ScaledTextField.StringView selected = this.textField.getSelected();
                int startX = baseX;
                int maxLineWidth = Math.round((this.width - this.innerPadding()) * invScale);
                double selScreenY = this.getY() + this.innerPadding();

                for (ScaledTextField.StringView line : this.textField.iterateLines()) {
                    if (selected.beginIndex() > line.endIndex()) {
                        selScreenY += this.lineHeight;
                        continue;
                    }
                    if (line.beginIndex() > selected.endIndex()) break;

                    if (this.withinContentAreaTopBottom((int) selScreenY, (int) (selScreenY + this.lineHeight))) {
                        int offset = this.font.width(
                                text.substring(line.beginIndex(), Math.max(selected.beginIndex(), line.beginIndex()))
                        );
                        int endWidth;
                        if (selected.endIndex() > line.endIndex()) {
                            endWidth = maxLineWidth;
                        } else {
                            endWidth = this.font.width(text.substring(line.beginIndex(), selected.endIndex()));
                        }
                        int drawY = Math.round((float) (selScreenY * invScale));
                        this.renderHighlight(guiGraphics, startX + offset, drawY, startX + endWidth, drawY + this.font.lineHeight);
                    }
                    selScreenY += this.lineHeight;
                }
            }
        }

        @Override
        protected void renderDecorations(GuiGraphics guiGraphics) {
            super.renderDecorations(guiGraphics);
        }

        @Override
        public int getInnerHeight() {
            return Math.max(1, (int) Math.ceil(this.lineHeight * this.textField.getLineCount()));
        }

        @Override
        protected boolean scrollbarVisible() {
            return (double) this.textField.getLineCount() > this.getDisplayableLineCount()
                    && this.getMaxScrollAmount() > 0;
        }

        @Override
        protected double scrollRate() {
            return this.lineHeight / 2.0;
        }

        private void renderHighlight(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY) {
            guiGraphics.fill(RenderType.guiTextHighlight(), minX, minY, maxX, maxY, -16776961);
        }

        private void scrollToCursor() {
            if (this.lineHeight <= 0.0) return;
            if (this.textField.getLineCount() <= 0) {
                this.setScrollAmount(0.0);
                return;
            }
            double scroll = this.scrollAmount();
            ScaledTextField.StringView line = this.textField.getLineView((int) (scroll / this.lineHeight));
            int cursorLine = this.textField.getLineAtCursor();
            if (cursorLine < 0) {
                this.setScrollAmount(0.0);
                return;
            }
            if (this.textField.cursor() <= line.beginIndex()) {
                scroll = (double) cursorLine * this.lineHeight;
            } else {
                ScaledTextField.StringView lastVisible = this.textField.getLineView((int) ((scroll + (double) this.height) / this.lineHeight) - 1);
                if (this.textField.cursor() > lastVisible.endIndex()) {
                    scroll = (double) cursorLine * this.lineHeight - this.height + this.lineHeight + this.totalInnerPadding();
                }
            }

            this.setScrollAmount(scroll);
        }

        private double getDisplayableLineCount() {
            return (double) (this.height - this.totalInnerPadding()) / this.lineHeight;
        }

        private FormattedRenderResult drawFormattedString(GuiGraphics guiGraphics, String text, int x, int y, int initialColor) {
            if (!this.allowColorFormatting) {
                int drawX = x;
                if (guiGraphics != null) {
                    drawX = guiGraphics.drawString(this.font, text, drawX, y, initialColor, false);
                } else {
                    drawX += this.font.width(text);
                }
                return new FormattedRenderResult(drawX, initialColor);
            }
            int drawX = x;
            int color = initialColor;
            boolean bold = false;
            boolean italic = false;
            boolean encrypted = false;
            StringBuilder segment = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                if (startsWithColorToken(text, i)) {
                    if (!segment.isEmpty()) {
                        if (guiGraphics != null) {
                            drawX = guiGraphics.drawString(
                                    this.font,
                                    Component.literal(segment.toString()).withStyle(Style.EMPTY.withColor(color).withBold(bold).withItalic(italic).withObfuscated(encrypted)),
                                    drawX,
                                    y,
                                    color,
                                    false
                            );
                        } else {
                            drawX += this.font.width(segment.toString());
                        }
                        segment.setLength(0);
                    }
                    String escape = text.substring(i, Math.min(i + 2, text.length()));
                    if (guiGraphics != null) {
                        drawX = guiGraphics.drawString(this.font, escape, drawX, y, 0xFF9A9A9A, false);
                    } else {
                        drawX += this.font.width(escape);
                    }
                    char code = Character.toLowerCase(text.charAt(i + 1));
                    if (code == 'l') {
                        bold = !bold;
                    } else if (code == 'i') {
                        italic = !italic;
                    } else if (code == 'e') {
                        encrypted = !encrypted;
                    } else if (code == 'x') {
                        color = this.textColor & 0xFFFFFF;
                        bold = false;
                        italic = false;
                        encrypted = false;
                    } else {
                        color = formatColor(code, this.textColor);
                    }
                    i += 1;
                    continue;
                }
                segment.append(text.charAt(i));
            }
            if (!segment.isEmpty()) {
                if (guiGraphics != null) {
                    drawX = guiGraphics.drawString(
                            this.font,
                            Component.literal(segment.toString()).withStyle(Style.EMPTY.withColor(color).withBold(bold).withItalic(italic).withObfuscated(encrypted)),
                            drawX,
                            y,
                            color,
                            false
                    );
                } else {
                    drawX += this.font.width(segment.toString());
                }
            }
            return new FormattedRenderResult(drawX, color);
        }

        private boolean startsWithColorToken(String text, int index) {
            return index + 1 < text.length()
                    && text.charAt(index) == '/'
                    && isColorTokenCode(text.charAt(index + 1));
        }

        private int formatColor(char code, int fallback) {
            return switch (Character.toLowerCase(code)) {
                case 'w' -> 0x55FFFF;
                case 'r' -> 0xFF5555;
                case 'g' -> 0x55FF55;
                case 'b' -> 0x5555FF;
                case 'y' -> 0xFFFF55;
                case 'o' -> 0xFFAA00;
                case 'a' -> 0xAAAAAA;
                case 'p' -> 0xAA55FF;
                case 'x' -> this.textColor & 0xFFFFFF;
                default -> fallback;
            };
        }

        private boolean isColorTokenCode(char code) {
            return switch (Character.toLowerCase(code)) {
                case 'w', 'r', 'g', 'b', 'y', 'o', 'a', 'p', 'x', 'l', 'i', 'e' -> true;
                default -> false;
            };
        }

        private record FormattedRenderResult(int endX, int finalColor) {
        }

        private void seekCursorScreen(double mouseX, double mouseY) {
            double d0 = (mouseX - (double) this.getX() - (double) this.innerPadding()) / this.textScale;
            double d1 = (mouseY - (double) this.getY() - (double) this.innerPadding() + this.scrollAmount()) / this.textScale;
            this.textField.seekCursorToPoint(d0, d1);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (focused) {
                this.focusedTime = Util.getMillis();
            }
        }
    }

    private static final class ScaledTextField {
        private final Font font;
        private final List<StringView> displayLines = new ArrayList<>();
        private final int width;
        private final boolean colorTokenAwareDeletion;
        private String value = "";
        private int cursor;
        private int selectCursor;
        private boolean selecting;
        private int characterLimit = Integer.MAX_VALUE;
        private Consumer<String> valueListener = ignored -> {
        };
        private Runnable cursorListener = () -> {
        };
        private final Deque<HistoryState> undoHistory = new ArrayDeque<>();
        private final Deque<HistoryState> redoHistory = new ArrayDeque<>();
        private boolean restoringHistory = false;

        ScaledTextField(Font font, int width, boolean colorTokenAwareDeletion) {
            this.font = font;
            this.width = width;
            this.colorTokenAwareDeletion = colorTokenAwareDeletion;
            this.setValue("");
        }

        public int characterLimit() {
            return this.characterLimit;
        }

        public void setCharacterLimit(int characterLimit) {
            if (characterLimit < 0) {
                throw new IllegalArgumentException("Character limit cannot be negative");
            }
            this.characterLimit = characterLimit;
        }

        public boolean hasCharacterLimit() {
            return this.characterLimit != Integer.MAX_VALUE;
        }

        public void setValueListener(Consumer<String> valueListener) {
            this.valueListener = valueListener == null ? ignored -> {
            } : valueListener;
        }

        public void setCursorListener(Runnable cursorListener) {
            this.cursorListener = cursorListener == null ? () -> {
            } : cursorListener;
        }

        public void setValue(String fullText) {
            String next = fullText == null ? "" : fullText;
            this.value = this.truncateFullText(next);
            this.cursor = this.value.length();
            this.selectCursor = this.cursor;
            this.clearHistory();
            this.onValueChange();
        }

        public String value() {
            return this.value;
        }

        public void insertText(String text) {
            if (!text.isEmpty() || this.hasSelection()) {
                pushUndoState();
                String filtered = this.truncateInsertionText(StringUtil.filterText(text, true));
                StringView selected = this.getSelected();
                int len = this.value.length();
                int begin = Mth.clamp(selected.beginIndex, 0, len);
                int end = Mth.clamp(selected.endIndex, begin, len);
                this.value = new StringBuilder(this.value).replace(begin, end, filtered).toString();
                this.cursor = begin + filtered.length();
                this.selectCursor = this.cursor;
                this.onValueChange();
            }
        }

        public void deleteText(int length) {
            if (!this.hasSelection()) {
                if (this.colorTokenAwareDeletion && length < 0) {
                    int tokenStart = this.findColorTokenStartBeforeCursor();
                    if (tokenStart >= 0) {
                        this.selectCursor = tokenStart;
                        this.insertText("");
                        return;
                    }
                } else if (this.colorTokenAwareDeletion && length > 0) {
                    int tokenEnd = this.findColorTokenEndAtCursor();
                    if (tokenEnd >= 0) {
                        this.selectCursor = tokenEnd;
                        this.insertText("");
                        return;
                    }
                }
                this.selectCursor = Mth.clamp(this.cursor + length, 0, this.value.length());
            }
            this.insertText("");
        }

        public int cursor() {
            return this.cursor;
        }

        public boolean canUndo() {
            return !this.undoHistory.isEmpty();
        }

        public boolean canRedo() {
            return !this.redoHistory.isEmpty();
        }

        public void undo() {
            restoreFromHistory(this.undoHistory, this.redoHistory);
        }

        public void redo() {
            restoreFromHistory(this.redoHistory, this.undoHistory);
        }

        public void clearHistory() {
            this.undoHistory.clear();
            this.redoHistory.clear();
        }

        public void setSelecting(boolean selecting) {
            this.selecting = selecting;
        }

        public StringView getSelected() {
            int len = this.value.length();
            int start = Mth.clamp(Math.min(this.selectCursor, this.cursor), 0, len);
            int end = Mth.clamp(Math.max(this.selectCursor, this.cursor), start, len);
            return new StringView(start, end);
        }

        public int getLineCount() {
            return this.displayLines.size();
        }

        public int getLineAtCursor() {
            for (int i = 0; i < this.displayLines.size(); i++) {
                StringView line = this.displayLines.get(i);
                if (this.cursor >= line.beginIndex && this.cursor <= line.endIndex) {
                    return i;
                }
            }
            return -1;
        }

        public StringView getLineView(int lineNumber) {
            if (this.displayLines.isEmpty()) return StringView.EMPTY;
            return this.displayLines.get(Mth.clamp(lineNumber, 0, this.displayLines.size() - 1));
        }

        public void seekCursor(Whence whence, int position) {
            switch (whence) {
                case ABSOLUTE -> this.cursor = position;
                case RELATIVE -> this.cursor += position;
                case END -> this.cursor = this.value.length() + position;
            }

            this.cursor = Mth.clamp(this.cursor, 0, this.value.length());
            this.cursorListener.run();
            if (!this.selecting) {
                this.selectCursor = this.cursor;
            }
        }

        public void seekCursorLine(int offset) {
            if (offset != 0) {
                int width = this.font.width(this.value.substring(this.getCursorLineView().beginIndex, this.cursor)) + 2;
                StringView line = this.getCursorLineView(offset);
                int length = this.font
                        .plainSubstrByWidth(this.value.substring(line.beginIndex, line.endIndex), width)
                        .length();
                this.seekCursor(Whence.ABSOLUTE, line.beginIndex + length);
            }
        }

        public void seekCursorToPoint(double x, double y) {
            if (this.displayLines.isEmpty()) {
                this.seekCursor(Whence.ABSOLUTE, 0);
                return;
            }
            int xi = Mth.floor(x);
            int yi = Mth.floor(y / 9.0);
            StringView line = this.displayLines.get(Mth.clamp(yi, 0, this.displayLines.size() - 1));
            int length = this.font
                    .plainSubstrByWidth(this.value.substring(line.beginIndex, line.endIndex), xi)
                    .length();
            this.seekCursor(Whence.ABSOLUTE, line.beginIndex + length);
        }

        public boolean keyPressed(int keyCode) {
            this.selecting = Screen.hasShiftDown();
            if (Screen.isSelectAll(keyCode)) {
                this.cursor = this.value.length();
                this.selectCursor = 0;
                return true;
            } else if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
                this.undo();
                return true;
            } else if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
                this.redo();
                return true;
            } else if (Screen.isCopy(keyCode)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
                return true;
            } else if (Screen.isPaste(keyCode)) {
                this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                return true;
            } else if (Screen.isCut(keyCode)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
                this.insertText("");
                return true;
            } else {
                switch (keyCode) {
                    case 257, 335 -> {
                        this.insertText("\n");
                        return true;
                    }
                    case 259 -> {
                        if (Screen.hasControlDown()) {
                            StringView word = this.getPreviousWord();
                            this.deleteText(word.beginIndex - this.cursor);
                        } else {
                            this.deleteText(-1);
                        }
                        return true;
                    }
                    case 261 -> {
                        if (Screen.hasControlDown()) {
                            StringView word = this.getNextWord();
                            this.deleteText(word.beginIndex - this.cursor);
                        } else {
                            this.deleteText(1);
                        }
                        return true;
                    }
                    case 262 -> {
                        if (Screen.hasControlDown()) {
                            StringView word = this.getNextWord();
                            this.seekCursor(Whence.ABSOLUTE, word.beginIndex);
                        } else {
                            this.seekCursor(Whence.RELATIVE, 1);
                        }
                        return true;
                    }
                    case 263 -> {
                        if (Screen.hasControlDown()) {
                            StringView word = this.getPreviousWord();
                            this.seekCursor(Whence.ABSOLUTE, word.beginIndex);
                        } else {
                            this.seekCursor(Whence.RELATIVE, -1);
                        }
                        return true;
                    }
                    case 264 -> {
                        if (!Screen.hasControlDown()) {
                            this.seekCursorLine(1);
                        }
                        return true;
                    }
                    case 265 -> {
                        if (!Screen.hasControlDown()) {
                            this.seekCursorLine(-1);
                        }
                        return true;
                    }
                    case 266 -> {
                        this.seekCursor(Whence.ABSOLUTE, 0);
                        return true;
                    }
                    case 267 -> {
                        this.seekCursor(Whence.END, 0);
                        return true;
                    }
                    case 268 -> {
                        if (Screen.hasControlDown()) {
                            this.seekCursor(Whence.ABSOLUTE, 0);
                        } else {
                            this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().beginIndex);
                        }
                        return true;
                    }
                    case 269 -> {
                        if (Screen.hasControlDown()) {
                            this.seekCursor(Whence.END, 0);
                        } else {
                            this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().endIndex);
                        }
                        return true;
                    }
                    default -> {
                        return false;
                    }
                }
            }
        }

        public Iterable<StringView> iterateLines() {
            return this.displayLines;
        }

        public boolean hasSelection() {
            return this.selectCursor != this.cursor;
        }

        private String getSelectedText() {
            StringView selected = this.getSelected();
            return this.value.substring(selected.beginIndex, selected.endIndex);
        }

        private StringView getCursorLineView() {
            return this.getCursorLineView(0);
        }

        private StringView getCursorLineView(int offset) {
            if (this.displayLines.isEmpty()) {
                return StringView.EMPTY;
            }
            int line = this.getLineAtCursor();
            if (line < 0) {
                throw new IllegalStateException("Cursor is not within text (cursor = " + this.cursor + ", length = " + this.value.length() + ")");
            }
            return this.displayLines.get(Mth.clamp(line + offset, 0, this.displayLines.size() - 1));
        }

        private StringView getPreviousWord() {
            if (this.value.isEmpty()) {
                return StringView.EMPTY;
            }
            int i = Mth.clamp(this.cursor, 0, this.value.length() - 1);
            while (i > 0 && Character.isWhitespace(this.value.charAt(i - 1))) {
                i--;
            }
            while (i > 0 && !Character.isWhitespace(this.value.charAt(i - 1))) {
                i--;
            }
            return new StringView(i, this.getWordEndPosition(i));
        }

        private StringView getNextWord() {
            if (this.value.isEmpty()) {
                return StringView.EMPTY;
            }
            int i = Mth.clamp(this.cursor, 0, this.value.length() - 1);
            while (i < this.value.length() && !Character.isWhitespace(this.value.charAt(i))) {
                i++;
            }
            while (i < this.value.length() && Character.isWhitespace(this.value.charAt(i))) {
                i++;
            }
            return new StringView(i, this.getWordEndPosition(i));
        }

        private int getWordEndPosition(int cursor) {
            int i = cursor;
            while (i < this.value.length() && !Character.isWhitespace(this.value.charAt(i))) {
                i++;
            }
            return i;
        }

        private int findColorTokenStartBeforeCursor() {
            if (this.cursor < 2 || this.cursor > this.value.length()) return -1;
            int start = this.cursor - 2;
            return isColorTokenAt(start) && this.cursor == start + 2 ? start : -1;
        }

        private int findColorTokenEndAtCursor() {
            if (this.cursor < 0 || this.cursor + 2 > this.value.length()) return -1;
            return isColorTokenAt(this.cursor) ? this.cursor + 2 : -1;
        }

        private boolean isColorTokenAt(int index) {
            if (index < 0 || index + 1 >= this.value.length()) return false;
            if (this.value.charAt(index) != '/') return false;
            char code = Character.toLowerCase(this.value.charAt(index + 1));
            return code == 'w' || code == 'r' || code == 'g' || code == 'b'
                    || code == 'y' || code == 'o' || code == 'a' || code == 'p' || code == 'x'
                    || code == 'l' || code == 'i' || code == 'e';
        }

        private void onValueChange() {
            this.reflowDisplayLines();
            this.valueListener.accept(this.value);
            this.cursorListener.run();
        }

        private void pushUndoState() {
            if (this.restoringHistory) return;
            HistoryState current = snapshot();
            if (!this.undoHistory.isEmpty() && this.undoHistory.peekLast().sameAs(current)) return;
            this.undoHistory.addLast(current);
            while (this.undoHistory.size() > 100) {
                this.undoHistory.removeFirst();
            }
            this.redoHistory.clear();
        }

        private void restoreFromHistory(Deque<HistoryState> source, Deque<HistoryState> target) {
            if (source.isEmpty()) return;
            HistoryState current = snapshot();
            target.addLast(current);
            HistoryState next = source.removeLast();
            this.restoringHistory = true;
            this.value = next.value;
            this.cursor = Mth.clamp(next.cursor, 0, this.value.length());
            this.selectCursor = Mth.clamp(next.selectCursor, 0, this.value.length());
            this.restoringHistory = false;
            this.onValueChange();
        }

        private HistoryState snapshot() {
            return new HistoryState(this.value, this.cursor, this.selectCursor);
        }

        private void reflowDisplayLines() {
            this.displayLines.clear();
            if (this.value.isEmpty()) {
                this.displayLines.add(StringView.EMPTY);
            } else {
                this.font.getSplitter().splitLines(
                        this.value,
                        this.width,
                        Style.EMPTY,
                        false,
                        (style, begin, end) -> this.displayLines.add(new StringView(begin, end))
                );
                if (this.value.charAt(this.value.length() - 1) == '\n') {
                    this.displayLines.add(new StringView(this.value.length(), this.value.length()));
                }
            }
        }

        private String truncateFullText(String fullText) {
            return this.hasCharacterLimit() ? StringUtil.truncateStringIfNecessary(fullText, this.characterLimit, false) : fullText;
        }

        private String truncateInsertionText(String text) {
            if (this.hasCharacterLimit()) {
                int allowed = this.characterLimit - this.value.length();
                return StringUtil.truncateStringIfNecessary(text, allowed, false);
            }
            return text;
        }

        private static record StringView(int beginIndex, int endIndex) {
            static final StringView EMPTY = new StringView(0, 0);
        }

        private static record HistoryState(String value, int cursor, int selectCursor) {
            boolean sameAs(HistoryState other) {
                return other != null
                        && Objects.equals(this.value, other.value)
                        && this.cursor == other.cursor
                        && this.selectCursor == other.selectCursor;
            }
        }
    }

    private static final class EditorEntry {
        final String id;
        final String label;
        final String subtitle;
        final String icon;
        final ResourceLocation actionIcon;
        final ResourceLocation rowTexture;
        final String actionTooltip;
        final String rowTooltip;
        final EditorEntryKind kind;
        final int indent;
        final boolean showMoveArrows;

        EditorEntry(String id, String label, String subtitle, String icon) {
            this(id, label, subtitle, icon, null, "", "", ROW_TEX, EditorEntryKind.NORMAL, 0);
        }

        EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, "", ROW_TEX, EditorEntryKind.NORMAL, 0);
        }

        EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, rowTooltip, ROW_TEX, EditorEntryKind.NORMAL, 0);
        }

        EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip, ResourceLocation rowTexture) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, rowTooltip, rowTexture, EditorEntryKind.NORMAL, 0);
        }

        EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip, ResourceLocation rowTexture, EditorEntryKind kind, int indent) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, rowTooltip, rowTexture, kind, indent, false);
        }

        EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip, ResourceLocation rowTexture, EditorEntryKind kind, int indent, boolean showMoveArrows) {
            this.id = id;
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon == null ? "" : icon;
            this.actionIcon = actionIcon;
            this.rowTexture = rowTexture == null ? ROW_TEX : rowTexture;
            this.actionTooltip = actionTooltip == null ? "" : actionTooltip;
            this.rowTooltip = rowTooltip == null ? "" : rowTooltip;
            this.kind = kind == null ? EditorEntryKind.NORMAL : kind;
            this.indent = Math.max(0, indent);
            this.showMoveArrows = showMoveArrows;
        }

        static EditorEntry movable(String id, String label, String subtitle, String icon) {
            return new EditorEntry(id, label, subtitle, icon, null, "", "", ROW_TEX, EditorEntryKind.NORMAL, 0, true);
        }

        static EditorEntry normal(String id, String label, String subtitle, String icon) {
            return new EditorEntry(id, label, subtitle, icon, null, "", "", ROW_TEX, EditorEntryKind.NORMAL, 0, false);
        }

        static EditorEntry quest(String id, String label, String subtitle, String icon) {
            return new EditorEntry(id, label, subtitle, icon, null, "", "", ROW_TEX, EditorEntryKind.QUEST, 0, true);
        }

        static EditorEntry quest(String id, String label, String subtitle, String icon, ResourceLocation rowTexture, String rowTooltip) {
            return new EditorEntry(id, label, subtitle, icon, null, "", rowTooltip, rowTexture, EditorEntryKind.QUEST, 0, true);
        }

        static EditorEntry categoryHeader(String id, String label, boolean collapsed) {
            return new EditorEntry(id, (collapsed ? "+ " : "- ") + label, "", "", null, "", "", ROW_TEX, EditorEntryKind.CATEGORY_HEADER, 0);
        }

        static EditorEntry subCategoryHeader(String id, String label, boolean collapsed) {
            return new EditorEntry(id, (collapsed ? "+ " : "- ") + label, "", "", null, "", "", ROW_TEX, EditorEntryKind.SUBCATEGORY_HEADER, 10);
        }
    }

    private enum EditorEntryKind {
        NORMAL,
        QUEST,
        CATEGORY_HEADER,
        SUBCATEGORY_HEADER
    }

    private static final class NamedEntry {
        final String id;
        final String name;
        final Path path;
        final String icon;
        final String sortKey;

        NamedEntry(String id, String name, String icon, Path path, String sortKey) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.icon = icon == null ? "" : icon;
            this.sortKey = sortKey == null ? "" : sortKey;
        }
    }

    private static final class QuestMoveEntry {
        final String id;
        final Path path;
        final String category;
        final String subCategory;

        QuestMoveEntry(String id, Path path, String category, String subCategory) {
            this.id = id == null ? "" : id;
            this.path = path;
            this.category = category == null ? "" : category;
            this.subCategory = subCategory == null ? "" : subCategory;
        }
    }

    private static final class QuestPack {
        final String name;
        final String namespace;
        final Path root;
        final boolean legacy;
        final boolean enabled;
        final Path dataDir;
        final Path questsDir;
        final Path categoriesDir;
        final Path subCategoriesDir;
        final Path singleRoot;
        final Path categoriesFile;
        final Path subCategoriesFile;
        final Path questsFile;

        QuestPack(String name, String namespace, Path root) {
            this(name, namespace, root, false, true);
        }

        QuestPack(String name, String namespace, Path root, boolean legacy) {
            this(name, namespace, root, legacy, true);
        }

        QuestPack(String name, String namespace, Path root, boolean legacy, boolean enabled) {
            this.name = name;
            this.namespace = namespace == null ? "" : namespace;
            this.root = root;
            this.legacy = legacy;
            this.enabled = enabled;
            this.dataDir = root.resolve("data").resolve(this.namespace);
            this.questsDir = dataDir.resolve("quests");
            this.categoriesDir = questsDir.resolve("categories");
            this.subCategoriesDir = questsDir.resolve("sub-category");
            this.singleRoot = root.resolve("boundless").resolve(this.name);
            this.categoriesFile = singleRoot.resolve("categories.json");
            this.subCategoriesFile = singleRoot.resolve("sub-categories.json");
            this.questsFile = singleRoot.resolve("quests.json");
        }

        void ensureDirs() throws IOException {
            Files.createDirectories(root);
            Files.createDirectories(singleRoot);
        }
    }

    private static final class PackMeta {
        String description = "";
        String iconPath = "";
        boolean enabled = true;
    }

    private static final class CategoryData {
        Path path;
        String id = "";
        String name = "";
        String icon = "";
        String order = "";
        String dependency = "";
        String autoComplete = "";
    }

    private static final class SubCategoryData {
        Path path;
        String id = "";
        String category = "";
        String name = "";
        String icon = "";
        String order = "";
        String defaultOpen = "";
    }

    private static final class QuestEntryData {
        Path path;
        String id = "";
        String name = "";
        String icon = "";
        String description = "";
        String category = "";
        String subCategory = "";
        String dependencies = "";
        String lockAfterDependency = "";
        String optional = "";
        String repeatable = "";
        String autoComplete = "";
        String hiddenUnderDependency = "";
        String type = "";
        String completionJson = "";
        String rewardJson = "";
    }

    private static final class ScreenState {
        Mode mode = Mode.PACK_LIST;
        EditorType editorType = EditorType.NONE;
        QuestPack currentPack;
        String selectedEntryId = "";
        Path editingPath;
        String loadedQuestType = "";
        String questOrderToken = "";
        float editorScroll = 0f;
        float leftScroll = 0f;
        String statusMessage = "";
        int statusColor = 0xA0A0A0;
        boolean deleteConfirmArmed = false;
        String savedEditorState = null;
        String pendingDiscardEntryId = "";
        Mode pendingDiscardMode = null;
        String questSearchQuery = "";

        String packName = "";
        String packNamespace = "";
        String packIconPath = "";
        String packDescription = "";

        String catId = "";
        String catName = "";
        String catIcon = "";
        String catDependency = "";
        boolean catAutoComplete = false;

        String subId = "";
        String subCategory = "";
        String subName = "";
        String subIcon = "";
        boolean subDefaultOpen = false;

        String questId = "";
        String questName = "";
        String questIcon = "";
        String questDescription = "";
        String questCategory = "";
        String questSubCategory = "";
        String questDependencies = "";
        boolean questLockAfterDependency = false;
        boolean questOptional = false;
        boolean questRepeatable = false;
        boolean questAutoComplete = false;
        boolean questHiddenUnderDependency = false;
        String questCompletion = "";
        String questReward = "";
    }

    private static final class IndexName {
        final String index;
        final String name;

        IndexName(String index, String name) {
            this.index = index == null ? "" : index;
            this.name = name == null ? "" : name;
        }
    }

    private enum MoveDirection {
        UP,
        DOWN
    }

    private enum EntryRowKind {
        DEPENDENCY,
        COMPLETION,
        REWARD
    }

    private enum DropdownMenuTarget {
        SUBCATEGORY_PARENT,
        QUEST_CATEGORY,
        QUEST_SUBCATEGORY
    }

    private enum ItemPickerTab {
        CREATIVE,
        INVENTORY
    }

    private enum PickerMode {
        NONE,
        ITEMS,
        EFFECTS,
        MOBS
    }

    private static final class ParsedEntry {
        final String type;
        final String id;
        final int count;
        final String hint;

        ParsedEntry(String type, String id, int count) {
            this(type, id, count, "");
        }

        ParsedEntry(String type, String id, int count, String hint) {
            this.type = type == null ? "" : type;
            this.id = id == null ? "" : id;
            this.count = count;
            this.hint = hint == null ? "" : hint;
        }
    }

    private static final class CommandReward {
        final String command;
        final String icon;
        final String title;

        CommandReward(String command, String icon, String title) {
            this.command = command == null ? "" : command;
            this.icon = icon == null ? "" : icon;
            this.title = title == null ? "" : title;
        }
    }

    private static final class MultiLineEntryContext {
        final boolean hasTypeSeparator;
        final String type;
        final String typePrefix;
        final int typeStart;
        final int typeEnd;
        final int idStart;
        final int idEnd;
        final String idPrefix;

        MultiLineEntryContext(boolean hasTypeSeparator, String type, String typePrefix,
                              int typeStart, int typeEnd, int idStart, int idEnd, String idPrefix) {
            this.hasTypeSeparator = hasTypeSeparator;
            this.type = type == null ? "" : type;
            this.typePrefix = typePrefix == null ? "" : typePrefix;
            this.typeStart = typeStart;
            this.typeEnd = typeEnd;
            this.idStart = idStart;
            this.idEnd = idEnd;
            this.idPrefix = idPrefix == null ? "" : idPrefix;
        }
    }

    private static final class SuggestionBounds {
        final int x;
        final int y;
        final int w;
        final int h;

        SuggestionBounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private static final class FormField {
        final String displayLabel;
        final String tooltip;
        final AbstractWidget widget;

        FormField(String label, AbstractWidget widget) {
            String raw = label == null ? "" : label;
            String display = raw;
            String tip = "";
            int start = raw.indexOf('(');
            int end = raw.lastIndexOf(')');
            if (start >= 0 && end > start) {
                display = raw.substring(0, start).trim();
                tip = raw.substring(start + 1, end).trim();
            }
            this.displayLabel = display.isBlank() ? raw : display;
            this.tooltip = tip == null ? "" : tip;
            this.widget = widget;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FormField other)) return false;
            return widget == other.widget;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(widget);
        }
    }

    private static final class EditorListWidget extends AbstractButton {
        private final List<EditorEntry> entries = new ArrayList<>();
        private final Consumer<EditorEntry> onClick;
        private final Consumer<EditorEntry> onActionClick;
        private final Consumer<EditorEntry> onSecondaryClick;
        private final BiConsumer<EditorEntry, MoveDirection> onMoveClick;
        private float scrollY = 0f;
        private String selectedId = "";
        private List<Component> pendingTooltip = List.of();
        private int pendingTooltipX;
        private int pendingTooltipY;

        EditorListWidget(int x, int y, int w, int h, Consumer<EditorEntry> onClick, Consumer<EditorEntry> onActionClick, Consumer<EditorEntry> onSecondaryClick, BiConsumer<EditorEntry, MoveDirection> onMoveClick) {
            super(x, y, w, h, Component.empty());
            this.onClick = onClick;
            this.onActionClick = onActionClick;
            this.onSecondaryClick = onSecondaryClick;
            this.onMoveClick = onMoveClick;
        }

        void setBounds(int x, int y, int w, int h) {
            setX(x);
            setY(y);
            this.width = w;
            this.height = h;
        }

        void setEntries(List<EditorEntry> list) {
            entries.clear();
            entries.addAll(list);
            scrollY = 0f;
        }

        void setSelectedId(String id) {
            selectedId = id == null ? "" : id;
        }

        float getScrollY() {
            return scrollY;
        }

        void setScrollY(float value) {
            int contentHeight = entries.size() * (ROW_H + ROW_PAD);
            float max = Math.max(0f, contentHeight - height);
            scrollY = Math.max(0f, Math.min(value, max));
        }

        @Override
        public void onPress() {
        }

        void renderHoverTooltipOnTop(GuiGraphics gg) {
            if (pendingTooltip == null || pendingTooltip.isEmpty()) return;
            gg.pose().pushPose();
            gg.pose().translate(0.0F, 0.0F, 500.0F);
            gg.renderComponentTooltip(Minecraft.getInstance().font, pendingTooltip, pendingTooltipX, pendingTooltipY);
            gg.pose().popPose();
            pendingTooltip = List.of();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            pendingTooltip = List.of();
            gg.enableScissor(getX(), getY(), getX() + width, getY() + height);

            int yCursor = getY() - (int) scrollY;
            for (EditorEntry entry : entries) {
                boolean headerEntry = entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER;
                int top = yCursor;
                int rowH = headerEntry ? EDITOR_SUBHEADER_H : ROW_H;
                int rowGap = headerEntry ? EDITOR_SUBHEADER_GAP : ROW_PAD;

                if (top > getY() + height) break;
                if (top + rowH < getY()) {
                    yCursor += rowH + rowGap;
                    continue;
                }

                if (ENTRY_NEW.equals(entry.id)) {
                    int buttonWidth = LIST_CREATE_BUTTON_W;
                    int buttonHeight = LIST_CREATE_BUTTON_H;
                    int buttonX = getX() + (width - buttonWidth) / 2;
                    int buttonY = top + (rowH - buttonHeight) / 2;
                    boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                            && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
                    gg.blitSprite(hovered ? VANILLA_BUTTON_HIGHLIGHTED_SPRITE : VANILLA_BUTTON_SPRITE, buttonX, buttonY, buttonWidth, buttonHeight);
                    Font font = Minecraft.getInstance().font;
                    gg.drawCenteredString(font, entry.label, buttonX + buttonWidth / 2, buttonY + (buttonHeight - font.lineHeight) / 2 + 1, 0xFFFFFF);
                } else {
                    int textX = getX() + 6 + entry.indent;
                    ItemStack iconStack = iconStackFromId(entry.icon);

                    if (!headerEntry) {
                        gg.blit(entry.rowTexture, getX(), top, 0, 0, width, ROW_H, width, ROW_H);
                        if (!selectedId.isBlank() && selectedId.equals(entry.id)) {
                            gg.fill(getX() + 1, top + 1, getX() + width - 1, top + ROW_H - 1, 0x20FFFFFF);
                        }
                    } else {
                        float iconScale = 0.45f;
                        int iconSize = (int) (16 * iconScale);
                        int iconX = getX() + 2;
                        int iconY = top + (rowH - iconSize) / 2;
                        int textIconSize = iconStack.isEmpty() ? 0 : iconSize;

                        if (!iconStack.isEmpty()) {
                            gg.pose().pushPose();
                            gg.pose().translate(iconX, iconY, 0);
                            gg.pose().scale(iconScale, iconScale, 1f);
                            gg.renderItem(iconStack, 0, 0);
                            gg.pose().popPose();
                        }

                        float textScale = 0.66f;
                        String name = entry.label == null ? "" : entry.label.substring(Math.min(2, entry.label.length()));
                        textX = iconX + textIconSize + 2;
                        int textH = (int) (Minecraft.getInstance().font.lineHeight * textScale);
                        int textY = top + (rowH - textH) / 2 + 1;
                        int maxW = (getX() + width - 2) - textX - 2;
                        int maxWUnscaled = maxW > 0 ? (int) (maxW / textScale) : 0;
                        if (Minecraft.getInstance().font.width(name) > maxWUnscaled) {
                            name = Minecraft.getInstance().font.plainSubstrByWidth(name, Math.max(0, maxWUnscaled - Minecraft.getInstance().font.width("..."))) + "...";
                        }
                        if (entry.kind == EditorEntryKind.CATEGORY_HEADER) {
                            drawScaledComponent(gg, Component.literal(name).withStyle(style -> style.withBold(true)), textScale, textX, textY, 0xFFFFFF);
                        } else {
                            drawScaledString(gg, name, textScale, textX, textY, 0xD0D0D0);
                        }
                        yCursor += rowH + rowGap;
                        continue;
                    }

                    if (!iconStack.isEmpty()) {
                        gg.renderItem(iconStack, getX() + 6 + entry.indent, top + 5);
                        textX = getX() + 25 + entry.indent;
                    }
                    int labelColor = 0xFFFFFF;
                    gg.drawString(Minecraft.getInstance().font, entry.label, textX, top + 7, labelColor, false);

                    if (entry.kind == EditorEntryKind.NORMAL && entry.subtitle != null && !entry.subtitle.isBlank()) {
                        drawScaledString(gg, entry.subtitle, 0.65f, textX, top + 17, 0xB0B0B0);
                    }

                    if (entry.showMoveArrows) {
                        int moveX = getX() + width - MOVE_ICON_SIZE - MOVE_ICON_INSET_RIGHT;
                        int upY = top + MOVE_ICON_TOP_OFFSET;
                        int downY = top + MOVE_ICON_BOTTOM_OFFSET;
                        boolean hoverUp = mouseX >= moveX && mouseX <= moveX + MOVE_ICON_SIZE
                                && mouseY >= upY && mouseY <= upY + MOVE_ICON_SIZE;
                        boolean hoverDown = mouseX >= moveX && mouseX <= moveX + MOVE_ICON_SIZE
                                && mouseY >= downY && mouseY <= downY + MOVE_ICON_SIZE;
                        gg.blit(hoverUp ? MOVE_UP_HIGHLIGHTED_TEX : MOVE_UP_TEX, moveX, upY, 0, 0, MOVE_ICON_SIZE, MOVE_ICON_SIZE, MOVE_ICON_SIZE, MOVE_ICON_SIZE);
                        gg.blit(hoverDown ? MOVE_DOWN_HIGHLIGHTED_TEX : MOVE_DOWN_TEX, moveX, downY, 0, 0, MOVE_ICON_SIZE, MOVE_ICON_SIZE, MOVE_ICON_SIZE, MOVE_ICON_SIZE);
                    }

                    boolean hoverRow = mouseX >= getX() && mouseX <= getX() + width
                            && mouseY >= top && mouseY <= top + rowH;
                    if (hoverRow && entry.rowTooltip != null && !entry.rowTooltip.isBlank()) {
                        pendingTooltip = List.of(Component.literal(entry.rowTooltip));
                        pendingTooltipX = mouseX;
                        pendingTooltipY = mouseY;
                    }

                    if (entry.actionIcon != null) {
                        int iconX = getX() + width - 17;
                        int iconY = top + 7;
                        gg.blit(entry.actionIcon, iconX, iconY, 0, 0, 13, 13, 13, 13);
                        boolean hoverAction = mouseX >= iconX && mouseX <= iconX + 13
                                && mouseY >= iconY && mouseY <= iconY + 13;
                        if (hoverAction && entry.actionTooltip != null && !entry.actionTooltip.isBlank()) {
                            pendingTooltip = List.of(Component.literal(entry.actionTooltip));
                            pendingTooltipX = mouseX;
                            pendingTooltipY = mouseY;
                        }
                    }
                }

                yCursor += rowH + rowGap;
            }

            gg.disableScissor();

            int contentHeight = 0;
            for (EditorEntry entry : entries) {
                boolean headerEntry = entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER;
                contentHeight += (headerEntry ? EDITOR_SUBHEADER_H : ROW_H) + (headerEntry ? EDITOR_SUBHEADER_GAP : ROW_PAD);
            }
            if (contentHeight > height) {
                float ratio = (float) height / (float) contentHeight;
                int barH = Math.max(12, (int) (height * ratio));
                float scrollRatio = scrollY / (contentHeight - height);
                int barY = getY() + (int) ((height - barH) * scrollRatio);
                gg.fill(getX() + width + 4, barY, getX() + width + 6, barY + barH, 0xFF808080);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible || !active || (button != 0 && button != 1)) return false;
            if (mouseX < getX() || mouseX > getX() + width || mouseY < getY() || mouseY > getY() + height)
                return false;

            int localY = (int) (mouseY - getY() + scrollY);
            int yCursor = 0;
            for (EditorEntry entry : entries) {
                boolean headerEntry = entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER;
                int rowH = headerEntry ? EDITOR_SUBHEADER_H : ROW_H;
                int rowGap = headerEntry ? EDITOR_SUBHEADER_GAP : ROW_PAD;
                if (localY < yCursor || localY >= yCursor + rowH + rowGap) {
                    yCursor += rowH + rowGap;
                    continue;
                }
                int top = getY() - (int) scrollY + yCursor;
                if (entry.showMoveArrows && button == 0) {
                    int moveX = getX() + width - MOVE_ICON_SIZE - MOVE_ICON_INSET_RIGHT;
                    int upY = top + MOVE_ICON_TOP_OFFSET;
                    int downY = top + MOVE_ICON_BOTTOM_OFFSET;
                    if (mouseX >= moveX && mouseX <= moveX + MOVE_ICON_SIZE
                            && mouseY >= upY && mouseY <= upY + MOVE_ICON_SIZE) {
                        if (onMoveClick != null) onMoveClick.accept(entry, MoveDirection.UP);
                        return true;
                    }
                    if (mouseX >= moveX && mouseX <= moveX + MOVE_ICON_SIZE
                            && mouseY >= downY && mouseY <= downY + MOVE_ICON_SIZE) {
                        if (onMoveClick != null) onMoveClick.accept(entry, MoveDirection.DOWN);
                        return true;
                    }
                }
                if (entry.actionIcon != null) {
                    int iconX = getX() + width - 17;
                    int iconY = top + 7;
                    if (button == 0 && mouseX >= iconX && mouseX <= iconX + 13 && mouseY >= iconY && mouseY <= iconY + 13) {
                        if (onActionClick != null) onActionClick.accept(entry);
                        return true;
                    }
                }
                if (button == 1) {
                    if (onSecondaryClick != null) onSecondaryClick.accept(entry);
                    return true;
                }
                if (onClick != null) onClick.accept(entry);
                return true;
            }
            return false;
        }

        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!visible || !active) return false;
            int contentHeight = 0;
            for (EditorEntry entry : entries) {
                boolean headerEntry = entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER;
                contentHeight += (headerEntry ? EDITOR_SUBHEADER_H : ROW_H) + (headerEntry ? EDITOR_SUBHEADER_GAP : ROW_PAD);
            }
            if (contentHeight <= height) return false;
            scrollY = Math.max(0f, Math.min(scrollY - (float) (delta * 12), contentHeight - height));
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
            return mouseScrolled(mouseX, mouseY, deltaY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }

        private void drawScaledString(GuiGraphics gg, String text, float scale, int x, int y, int color) {
            if (text == null || text.isEmpty()) return;
            gg.pose().pushPose();
            gg.pose().scale(scale, scale, 1f);
            float inv = 1f / scale;
            gg.drawString(Minecraft.getInstance().font, text, (int) (x * inv), (int) (y * inv), color, false);
            gg.pose().popPose();
        }

        private void drawScaledComponent(GuiGraphics gg, Component text, float scale, int x, int y, int color) {
            if (text == null) return;
            gg.pose().pushPose();
            gg.pose().scale(scale, scale, 1f);
            float inv = 1f / scale;
            gg.drawString(Minecraft.getInstance().font, text, (int) (x * inv), (int) (y * inv), color, false);
            gg.pose().popPose();
        }
    }

    private static final class ActionButton extends AbstractButton {
        private final Runnable onPress;
        private final List<Component> tooltip;

        public ActionButton(int x, int y, int w, int h, Component text, Runnable onPress) {
            this(x, y, w, h, text, List.of(), onPress);
        }

        public ActionButton(int x, int y, int w, int h, Component text, List<Component> tooltip, Runnable onPress) {
            super(x, y, w, h, text);
            this.onPress = onPress;
            this.tooltip = tooltip == null ? List.of() : List.copyOf(tooltip);
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? BTN_TEX_DISABLED : (hovered ? BTN_TEX_HOVER : BTN_TEX);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);

            var font = Minecraft.getInstance().font;
            int textW = font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2 + 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            int color = this.active ? 0xFFFFFF : 0x808080;
            gg.drawString(font, getMessage(), textX, textY, color, false);
            if (hovered && !tooltip.isEmpty()) {
                gg.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private static final class CreateButton extends AbstractButton {
        private final Runnable onPress;

        CreateButton(int x, int y, int w, int h, Component text, Runnable onPress) {
            super(x, y, w, h, text);
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            gg.blit(CREATE_BTN_TEX, getX(), getY(), 0, 0, this.width, this.height, CREATE_BTN_TEX_W, CREATE_BTN_TEX_H);
            Font font = Minecraft.getInstance().font;
            int textW = font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            gg.drawString(font, getMessage(), textX, textY, 0xFFFFFF, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private final class EditorTabButton extends AbstractButton {
        private final String tooltip;
        private final String iconId;
        private final ResourceLocation iconTexture;
        private final Mode targetMode;

        EditorTabButton(String tooltip, String iconId, Mode targetMode) {
            this(tooltip, iconId, null, targetMode);
        }

        EditorTabButton(String tooltip, ResourceLocation iconTexture, Mode targetMode) {
            this(tooltip, "", iconTexture, targetMode);
        }

        private EditorTabButton(String tooltip, String iconId, ResourceLocation iconTexture, Mode targetMode) {
            super(0, 0, TAB_W, TAB_H, Component.empty());
            this.tooltip = tooltip == null ? "" : tooltip;
            this.iconId = iconId == null ? "" : iconId;
            this.iconTexture = iconTexture;
            this.targetMode = targetMode;
        }

        String tooltip() {
            return tooltip;
        }

        @Override
        public void onPress() {
            if (currentPack == null || mode == targetMode) return;
            setMode(targetMode);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean selected = mode == targetMode;
            int renderX = getX() + (selected ? 2 : 0);
            gg.blit(selected ? TAB_SELECTED_TEX : TAB_TEX, renderX, getY(), 0, 0, this.width, this.height, TAB_W, TAB_H);
            int iconX = renderX + (this.width - 16) / 2;
            int iconY = getY() + 5;
            if (iconTexture != null) {
                gg.blit(iconTexture, iconX, iconY, 0, 0, 16, 16, 16, 16);
                return;
            }
            ItemStack icon = iconStackFromId(iconId);
            if (!icon.isEmpty()) {
                gg.renderItem(icon, iconX, iconY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private static final class CompactActionButton extends AbstractButton {
        private final Runnable onPress;

        CompactActionButton(int x, int y, int w, int h, Component text, Runnable onPress) {
            super(x, y, w, h, text);
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            int bg = !this.active ? 0xFF3A3A3A : (this.isMouseOver(mouseX, mouseY) ? 0xFF6A6A6A : 0xFF555555);
            int border = this.active ? 0xFF9A9A9A : 0xFF666666;
            gg.fill(getX(), getY(), getX() + this.width, getY() + this.height, bg);
            gg.fill(getX(), getY(), getX() + this.width, getY() + 1, border);
            gg.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, border);
            gg.fill(getX(), getY(), getX() + 1, getY() + this.height, border);
            gg.fill(getX() + this.width - 1, getY(), getX() + this.width, getY() + this.height, border);

            var font = Minecraft.getInstance().font;
            int color = this.active ? 0xFFFFFFFF : 0xFF8A8A8A;
            int textX = getX() + (this.width - font.width(getMessage())) / 2;
            int textY = getY() + (this.height - font.lineHeight) / 2;
            gg.drawString(font, getMessage(), textX, textY, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private static final class TextInsertButton extends AbstractButton {
        private final String insertText;
        private final int fillColor;
        private final float textScale;
        private final Consumer<String> onPress;

        TextInsertButton(int x, int y, int width, String label, String insertText, int fillColor, float textScale, Consumer<String> onPress) {
            super(x, y, width, FORMAT_BAR_H, Component.literal(label));
            this.insertText = insertText == null ? "" : insertText;
            this.fillColor = fillColor;
            this.textScale = textScale <= 0f ? 1f : textScale;
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.accept(insertText);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            int bg = this.fillColor;
            if (!this.active) {
                bg = 0xFF4A4A4A;
            } else if (this.isMouseOver(mouseX, mouseY)) {
                bg = brighten(this.fillColor, 24);
            }
            int border = this.active ? 0xFFBEBEBE : 0xFF6E6E6E;
            gg.fill(getX(), getY(), getX() + this.width, getY() + this.height, bg);
            gg.fill(getX(), getY(), getX() + this.width, getY() + 1, border);
            gg.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, border);
            gg.fill(getX(), getY(), getX() + 1, getY() + this.height, border);
            gg.fill(getX() + this.width - 1, getY(), getX() + this.width, getY() + this.height, border);

            if (!getMessage().getString().isBlank()) {
                var font = Minecraft.getInstance().font;
                int textColor = this.active ? 0xFFFFFFFF : 0xFF8A8A8A;
                float scale = this.textScale;
                int textWidth = Math.round(font.width(getMessage()) * scale);
                int textHeight = Math.round(font.lineHeight * scale);
                int textX = getX() + (this.width - textWidth) / 2;
                int textY = getY() + (this.height - textHeight) / 2;
                gg.pose().pushPose();
                gg.pose().translate(textX, textY, 0.0f);
                gg.pose().scale(scale, scale, 1.0f);
                gg.drawString(font, getMessage(), 0, 0, textColor, false);
                gg.pose().popPose();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }

        private int brighten(int color, int amount) {
            int a = (color >>> 24) & 0xFF;
            int r = Math.min(255, ((color >>> 16) & 0xFF) + amount);
            int g = Math.min(255, ((color >>> 8) & 0xFF) + amount);
            int b = Math.min(255, (color & 0xFF) + amount);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private static final class IconButton extends AbstractButton {
        private ResourceLocation texture;
        private ResourceLocation hoverTexture;
        private final Runnable onPress;

        public IconButton(int x, int y, int size, ResourceLocation texture, ResourceLocation hoverTexture, Runnable onPress) {
            super(x, y, size, size, Component.empty());
            this.texture = texture;
            this.hoverTexture = hoverTexture == null ? texture : hoverTexture;
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        public void setTexture(ResourceLocation texture) {
            if (texture != null) {
                this.texture = texture;
            }
        }

        public void setTextures(ResourceLocation texture, ResourceLocation hoverTexture) {
            if (texture != null) this.texture = texture;
            this.hoverTexture = hoverTexture == null ? this.texture : hoverTexture;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            float alpha = this.active ? 1.0f : 0.5f;
            ResourceLocation tex = this.isMouseOver(mouseX, mouseY) ? (hoverTexture == null ? texture : hoverTexture) : texture;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private final class EntryRemoveButton extends AbstractButton {
        private final EntryRowKind kind;

        EntryRemoveButton(EntryRowKind kind) {
            super(0, 0, ENTRY_REMOVE_BTN_W, ENTRY_ROW_H, Component.literal("X"));
            this.kind = kind;
        }

        @Override
        public void onPress() {
            QuestEditorScreen.this.removeEntryRow(kind, this);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            int bg = this.active ? (this.isMouseOver(mouseX, mouseY) ? 0xAA5A1A1A : 0xAA3A1212) : 0x553A1212;
            int fg = this.active ? 0xFFFF5555 : 0xFF804040;
            gg.fill(getX(), getY(), getX() + this.width, getY() + this.height, bg);
            gg.fill(getX(), getY(), getX() + this.width, getY() + 1, 0xFFC0C0C0);
            gg.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, 0xFFC0C0C0);
            gg.fill(getX(), getY(), getX() + 1, getY() + this.height, 0xFFC0C0C0);
            gg.fill(getX() + this.width - 1, getY(), getX() + this.width, getY() + this.height, 0xFFC0C0C0);
            gg.drawString(Minecraft.getInstance().font, "X", getX() + 2, getY() + 2, fg, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private final class EntryTypeButton extends AbstractButton {
        private final EntryRowKind kind;
        private int row;

        EntryTypeButton(EntryRowKind kind, int row) {
            super(0, 0, ENTRY_TYPE_BTN_W, ENTRY_ROW_H, Component.empty());
            this.kind = kind;
            this.row = row;
        }

        void setRow(int row) {
            this.row = row;
        }

        @Override
        public void onPress() {
            openTypeMenu(kind, row, getX(), getY(), getWidth());
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            List<ScaledMultiLineEditBox> rows = entryRows(kind);
            if (row < 0 || row >= rows.size()) return;
            String text = effectiveRowType(kind, rows.get(row));
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            boolean focused = this.isFocused();
            int bg = hovered || focused ? 0xFF101010 : 0xFF000000;
            int border = hovered || focused ? 0xFFFFFFFF : 0xFF8A8A8A;
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
            gg.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
            gg.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
            gg.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);
            int textW = Math.round(font.width(text) * ENTRY_TYPE_TEXT_SCALE);
            int tx = getX() + (getWidth() - textW) / 2;
            gg.pose().pushPose();
            gg.pose().translate(tx, getY() + 3, 0f);
            gg.pose().scale(ENTRY_TYPE_TEXT_SCALE, ENTRY_TYPE_TEXT_SCALE, 1f);
            gg.drawString(font, text, 0, 0, 0xFFFFFF00, false);
            gg.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private final class EntryItemPickerButton extends AbstractButton {
        private final EntryRowKind kind;
        private int row;

        EntryItemPickerButton(EntryRowKind kind, int row) {
            super(0, 0, ENTRY_ITEM_PICK_BTN_W, ENTRY_ROW_H, Component.empty());
            this.kind = kind;
            this.row = row;
        }

        void setRow(int row) {
            this.row = row;
        }

        @Override
        public void onPress() {
            openItemPicker(kind, row);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            boolean focused = this.isFocused();
            int bg = hovered || focused ? 0xFF404040 : 0xFF3A3A3A;
            int border = hovered || focused ? 0xFFFFFFFF : 0xFF8A8A8A;
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
            gg.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
            gg.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
            gg.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);
            ItemStack stack = selectedItemForRow(kind, row);
            if (!stack.isEmpty()) {
                List<ScaledMultiLineEditBox> rows = entryRows(kind);
                String type = row >= 0 && row < rows.size() ? effectiveRowType(kind, rows.get(row)) : "";
                gg.pose().pushPose();
                gg.pose().translate(getX() + 2.0f, getY() + 2.0f, 0f);
                gg.pose().scale(0.5f, 0.5f, 1f);
                if ("effect".equals(type)) {
                    renderEffectPickerIcon(gg, stack, 0, 0);
                } else {
                    gg.renderItem(stack, 0, 0);
                }
                gg.pose().popPose();
                return;
            }
            gg.drawString(font, "?", getX() + 3, getY() + 2, 0xFFFFFF00, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private static final class ToggleButton extends AbstractButton {
        private boolean on;

        public ToggleButton(int x, int y, int w, int h, boolean initial) {
            super(x, y, w, h, Component.empty());
            this.on = initial;
        }

        @Override
        public void onPress() {
            on = !on;
        }

        public boolean isOn() {
            return on;
        }

        public void setState(boolean next) {
            on = next;
        }

        public void setSize(int w, int h) {
            this.width = w;
            this.height = h;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = on ? TOGGLE_TEX_ON : (this.isMouseOver(mouseX, mouseY) ? TOGGLE_TEX_OFF_HOVER : TOGGLE_TEX_OFF);
            gg.blit(tex, getX(), getY(), 0, 0, TOGGLE_SIZE, TOGGLE_SIZE, TOGGLE_SIZE, TOGGLE_SIZE);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private static final class LockToggleButton extends AbstractButton {
        private boolean on;

        LockToggleButton(int x, int y, int w, int h, boolean initial) {
            super(x, y, w, h, Component.empty());
            this.on = initial;
        }

        @Override
        public void onPress() {
            on = !on;
        }

        boolean isOn() {
            return on;
        }

        void setState(boolean next) {
            on = next;
        }

        public void setSize(int w, int h) {
            this.width = w;
            this.height = h;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex;
            if (this.isMouseOver(mouseX, mouseY)) {
                tex = LOCK_TEX_HOVER;
            } else {
                tex = on ? LOCK_TEX_ENABLED : LOCK_TEX_DISABLED;
            }
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private static final class BackButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_button.png");
        private static final ResourceLocation TEX_HOVER =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_highlighted.png");

        private final Runnable onPress;

        public BackButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = hovered ? TEX_HOVER : TEX_NORMAL;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private enum Mode {
        PACK_LIST,
        PACK_CREATE,
        PACK_MENU,
        CATEGORY_LIST,
        SUBCATEGORY_LIST,
        QUEST_LIST
    }

    private enum EditorType {
        NONE,
        PACK_CREATE,
        PACK_OPTIONS,
        CATEGORY,
        SUBCATEGORY,
        QUEST
    }
}
