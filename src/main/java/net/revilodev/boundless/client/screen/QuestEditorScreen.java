
package net.revilodev.boundless.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.SharedConstants;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.QuestPanelClient;
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
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
    private static final ResourceLocation TOGGLE_TEX_ON =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/complete_filter.png");
    private static final ResourceLocation DUPLICATE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/duplicate_button.png");
    private static final ResourceLocation DELETE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter.png");
    private static final ResourceLocation DELETE_CONFIRM_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/are_you_sure_button.png");
    private static final ResourceLocation HEADER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/9-slice-header.png");
    private static final ResourceLocation TAB_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png");
    private static final ResourceLocation BUILTIN_PACK_ENABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/popup_confirmation.png");
    private static final ResourceLocation BUILTIN_PACK_DISABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/popup_reject.png");

    private static final int TOGGLE_SIZE = 20;
    private static final int SMALL_BTN_SIZE = 20;
    private static final int SMALL_BTN_GAP = 4;
    private static final int TAB_W = 35;
    private static final int TAB_H = 27;
    private static final int TAB_GAP = 2;
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
    private static final int FORMAT_BAR_H = 12;
    private static final int FORMAT_BTN_GAP = 1;

    private static final int PACK_FORMAT = 34;
    private static final ResourceLocation GENERATED_PACK_ICON =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/pack.png");
    private static final int DEFAULT_INPUT_TEXT_COLOR = 0xE0E0E0;
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
    private static final int EDITOR_SUBHEADER_H = 12;

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
    private EditorTabButton categoriesTabButton;
    private EditorTabButton subCategoriesTabButton;
    private EditorTabButton questsTabButton;

    private Mode mode = Mode.PACK_LIST;
    private EditorType editorType = EditorType.NONE;
    private QuestPack currentPack;
    private String selectedEntryId = "";

    private EditorListWidget leftList;
    private ActionButton saveButton;
    private IconButton duplicateButton;
    private IconButton deleteQuestButton;
    private IconButton deletePackButton;

    private float editorScroll = 0f;
    private String statusMessage = "";
    private int statusColor = 0xA0A0A0;
    private long deletePackConfirmUntil = 0L;
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
    private EditBox catOrderBox;
    private EditBox catDependencyBox;

    private EditBox subIdBox;
    private EditBox subCategoryBox;
    private EditBox subNameBox;
    private EditBox subIconBox;
    private EditBox subOrderBox;
    private ToggleButton subDefaultOpenToggle;

    private EditBox questIdBox;
    private EditBox questIndexBox;
    private EditBox questNameBox;
    private EditBox questIconBox;
    private ScaledMultiLineEditBox questDescriptionBox;
    private EditBox questCategoryBox;
    private EditBox questSubCategoryBox;
    private EditBox questDependenciesBox;
    private ToggleButton questOptionalToggle;
    private ToggleButton questRepeatableToggle;
    private ToggleButton questHiddenUnderDependencyToggle;
    private ScaledMultiLineEditBox questCompletionBox;
    private ScaledMultiLineEditBox questRewardBox;
    private final List<ScaledMultiLineEditBox> completionEntryBoxes = new ArrayList<>();
    private final List<ScaledMultiLineEditBox> rewardEntryBoxes = new ArrayList<>();
    private final List<EntryRemoveButton> completionEntryRemoveButtons = new ArrayList<>();
    private final List<EntryRemoveButton> rewardEntryRemoveButtons = new ArrayList<>();
    private Button exportDataPackButton;
    private Button openDataPackFolderButton;
    private boolean syncingEntryRows = false;
    private boolean entryRowsDirty = false;
    private final List<TextInsertButton> descriptionFormatButtons = new ArrayList<>();
    private CompactActionButton descriptionUndoButton;
    private CompactActionButton descriptionRedoButton;

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

    public QuestEditorScreen(Screen parent) {
        super(Component.literal("Quest Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
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

        leftList = new EditorListWidget(pxLeft, py, pw, listH, this::handleLeftClick, this::handleLeftAction, this::handleLeftSecondaryClick);
        addRenderableWidget(leftList);

        initFormFields();

        int barY = py + ph + (BOTTOM_BAR_H - 20) / 2;
        int saveX = pxRight + pw - 68;
        saveButton = new ActionButton(saveX, barY, 68, 20,
                Component.literal("Save"), this::saveCurrent);
        addRenderableWidget(saveButton);

        int deleteQuestX = saveX - SMALL_BTN_GAP - SMALL_BTN_SIZE;
        int duplicateX = deleteQuestX - SMALL_BTN_GAP - SMALL_BTN_SIZE;
        duplicateButton = new IconButton(duplicateX, barY, SMALL_BTN_SIZE, DUPLICATE_TEX, this::duplicateCurrent);
        deleteQuestButton = new IconButton(deleteQuestX, barY, SMALL_BTN_SIZE, DELETE_TEX, this::handleDeleteButtonPress);
        addRenderableWidget(duplicateButton);
        addRenderableWidget(deleteQuestButton);

        backButton = new BackButton(pxLeft + 2, barY, this::goBack);
        addRenderableWidget(backButton);
        createPackButton = Button.builder(Component.literal("create new pack"), button -> openPackCreate())
                .bounds(pxLeft + 2 + backButton.getWidth() + BOTTOM_CREATE_GAP + CREATE_PACK_BUTTON_OFFSET_X, barY, 100, 20)
                .build();
        addRenderableWidget(createPackButton);
        deletePackButton = new IconButton(pxLeft + 2 + 24 + SMALL_BTN_GAP, barY, SMALL_BTN_SIZE, DELETE_TEX, this::confirmDeletePack);
        addRenderableWidget(deletePackButton);
        categoriesTabButton = new EditorTabButton("Categories", "boundless:quest_book", Mode.CATEGORY_LIST);
        subCategoriesTabButton = new EditorTabButton("Sub-categories", "minecraft:book", Mode.SUBCATEGORY_LIST);
        questsTabButton = new EditorTabButton("Quests", "boundless:quest_completion_scroll", Mode.QUEST_LIST);
        questSearchBox = new EditBox(font, pxLeft + 2 + 24 + SMALL_BTN_GAP, barY, pw - 24 - SMALL_BTN_GAP - 2, 20, Component.literal("Search quests"));
        questSearchBox.setMaxLength(128);
        questSearchBox.setHint(Component.literal("Search quests"));
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
        catIconBox = createBox("Category icon", BOX_H);
        catOrderBox = createBox("Category order", BOX_H);
        catDependencyBox = createBox("Category dependency", BOX_H);

        subIdBox = createBox("Sub-category id", BOX_H);
        subCategoryBox = createBox("Parent category id", BOX_H);
        subNameBox = createBox("Sub-category name", BOX_H);
        subIconBox = createBox("Sub-category icon", BOX_H);
        subOrderBox = createBox("Sub-category order", BOX_H);
        subDefaultOpenToggle = createToggle(false);

        questIdBox = createBox("Quest id", BOX_H);
        questIndexBox = createBox("Quest index", BOX_H);
        questNameBox = createBox("Quest name", BOX_H);
        questIconBox = createBox("Quest icon", BOX_H);
        questDescriptionBox = createMultiLineBox("Quest description", BOX_H_TALL, Config.enableDescriptionColors());
        questCategoryBox = createBox("Quest category", BOX_H);
        questSubCategoryBox = createBox("Quest sub-category", BOX_H);
        questDependenciesBox = createBox("Dependencies (comma separated)", BOX_H);
        questOptionalToggle = createToggle(false);
        questRepeatableToggle = createToggle(false);
        questHiddenUnderDependencyToggle = createToggle(false);
        questCompletionBox = createMultiLineBox("Completion entries", BOX_H_TALL, false);
        questRewardBox = createMultiLineBox("Reward entries", BOX_H_TALL, false);
        initEntryRowBoxes();
        initDescriptionFormatterButtons();

        attachIdSanitizer(packNamespaceBox, false);
        attachIdSanitizer(catIdBox, false);
        attachIdSanitizer(catDependencyBox, false);
        attachIdSanitizer(subIdBox, false);
        attachIdSanitizer(subCategoryBox, false);
        attachIdSanitizer(questIdBox, false);
        attachIdSanitizer(questCategoryBox, false);
        attachIdSanitizer(questSubCategoryBox, false);
        attachIdSanitizer(questDependenciesBox, true);

        exportDataPackButton = Button.builder(Component.literal("Export as datapack"), button -> exportCurrentPackAsDataPack())
                .bounds(0, 0, 125, 20)
                .tooltip(Tooltip.create(Component.literal("Export as a datapack for server use.")))
                .build();
        exportDataPackButton.visible = false;
        exportDataPackButton.active = false;
        addRenderableWidget(exportDataPackButton);

        openDataPackFolderButton = Button.builder(Component.literal("Open datapack folder"), button -> openDataPackFolder())
                .bounds(0, 0, 125, 20)
                .build();
        openDataPackFolderButton.visible = false;
        openDataPackFolderButton.active = false;
        addRenderableWidget(openDataPackFolderButton);
    }

    private void initDescriptionFormatterButtons() {
        descriptionFormatButtons.clear();
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/r", 0xFFFF5555, 9));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/g", 0xFF55FF55, 9));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/b", 0xFF5555FF, 9));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/w", 0xFF55FFFF, 9));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/y", 0xFFFFFF55, 9));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/o", 0xFFFFAA00, 9));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/a", 0xFFAAAAAA, 9));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/p", 0xFFAA55FF, 9));
        descriptionFormatButtons.add(createDescriptionInsertButton("X", "/x", 0xFF4A4A4A, 11));
        descriptionUndoButton = createDescriptionActionButton("<", this::undoDescriptionFormat, 11);
        descriptionRedoButton = createDescriptionActionButton(">", this::redoDescriptionFormat, 11);
    }

    private TextInsertButton createDescriptionInsertButton(String label, String insertText, int fillColor, int width) {
        TextInsertButton button = new TextInsertButton(0, 0, width, label, insertText, fillColor, this::applyDescriptionFormat);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    private CompactActionButton createDescriptionActionButton(String label, Runnable action, int width) {
        CompactActionButton button = new CompactActionButton(0, 0, width, FORMAT_BAR_H, Component.literal(label), action);
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

    private void setMode(Mode next) {
        if (next == Mode.QUEST_LIST && mode != Mode.QUEST_LIST) {
            collapsedQuestCategories.clear();
            collapsedQuestSubCategories.clear();
        }
        mode = next;
        statusMessage = "";
        editorScroll = 0f;
        deletePackConfirmUntil = 0L;
        disarmDeleteConfirm();
        refreshLeftList();
        clearEditor();
        updateLeftPaneLayout();
        updateBackButtonVisibility();
    }

    private void refreshLeftList() {
        List<EditorEntry> entries = switch (mode) {
            case PACK_LIST, PACK_CREATE -> buildPackEntries();
            case PACK_MENU -> buildPackMenuEntries();
            case CATEGORY_LIST -> buildCategoryEntries();
            case SUBCATEGORY_LIST -> buildSubCategoryEntries();
            case QUEST_LIST -> buildQuestEntries();
        };
        leftList.setEntries(entries);
        leftList.setSelectedId(selectedEntryId);
        refreshPackIdCaches();
    }

    private List<EditorEntry> buildPackEntries() {
        List<EditorEntry> out = new ArrayList<>();
        boolean builtinEnabled = Config.enableBuiltinQuestPack();
        out.add(new EditorEntry(
                ENTRY_BUILTIN_PACK,
                "Built-in Quest Pack",
                builtinEnabled ? "boundless - enabled" : "boundless - disabled",
                "",
                builtinEnabled ? BUILTIN_PACK_ENABLED_TEX : BUILTIN_PACK_DISABLED_TEX,
                builtinEnabled ? "Enabled" : "Disabled"
        ));

        for (QuestPack pack : listPacks()) {
            String subtitle = pack.namespace.isBlank() ? "No namespace" : pack.namespace;
            boolean enabled = isPackEnabled(pack);
            out.add(new EditorEntry(
                    pack.name,
                    pack.name,
                    subtitle,
                    "",
                    enabled ? BUILTIN_PACK_ENABLED_TEX : BUILTIN_PACK_DISABLED_TEX,
                    enabled ? "Enabled" : "Disabled",
                    "Right click for options"
            ));
        }
        return out;
    }

    private List<EditorEntry> buildPackMenuEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_CATEGORIES, "Categories", "", ""));
        out.add(new EditorEntry(ENTRY_SUBCATEGORIES, "Sub-categories", "", ""));
        out.add(new EditorEntry(ENTRY_QUESTS, "Quests", "", ""));
        return out;
    }

    private List<EditorEntry> buildCategoryEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_NEW, "Create New Category", "", ""));
        if (currentPack == null) return out;
        for (NamedEntry entry : listCategoryEntries(currentPack)) {
            out.add(new EditorEntry(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    private List<EditorEntry> buildSubCategoryEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_NEW, "Create New Sub-category", "", ""));
        if (currentPack == null) return out;
        for (NamedEntry entry : listSubCategoryEntries(currentPack)) {
            out.add(new EditorEntry(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    private List<EditorEntry> buildQuestEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_NEW, "Create New Quest", "", ""));
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
            String categoryName = categoryNames.getOrDefault(categoryId, categoryId.isBlank() ? "Unassigned" : categoryId);
            boolean categoryCollapsed = collapsedQuestCategories.contains(categoryId);
            out.add(EditorEntry.categoryHeader(categoryId, categoryName, categoryCollapsed));
            if (categoryCollapsed) continue;

            for (Map.Entry<String, List<NamedEntry>> subEntry : categoryEntry.getValue().entrySet()) {
                String subId = safe(subEntry.getKey());
                String subKey = categoryId + "::" + subId;
                String subName = subCategoryNames.getOrDefault(subKey, subId.isBlank() ? "No Sub-category" : subId);
                boolean subCollapsed = collapsedQuestSubCategories.contains(subKey);
                out.add(EditorEntry.subCategoryHeader(subKey, subName, subCollapsed));
                if (subCollapsed) continue;
                for (NamedEntry quest : subEntry.getValue()) {
                    out.add(EditorEntry.quest(quest.id, quest.name, quest.sortKey, quest.icon));
                }
            }
        }
        return out;
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
            statusMessage = "Pack not found";
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
            QuestPanelClient.applyConfigChanges();
            statusMessage = next ? "Built-in quest pack enabled" : "Built-in quest pack disabled";
            statusColor = 0xA0FFA0;
            refreshLeftList();
            return;
        }
        QuestPack pack = findPackByName(entry.id);
        if (pack == null) return;
        boolean next = !isPackEnabled(pack);
        setPackEnabled(pack, next);
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
            statusMessage = "Pack not found";
            statusColor = 0xFF8080;
            return;
        }
        if (!ensurePackWorkspace(currentPack)) {
            setError("Failed to open pack");
            return;
        }
        String refreshedNamespace = findNamespace(currentPack.root);
        if (refreshedNamespace != null && !refreshedNamespace.isBlank()
                && !refreshedNamespace.equals(currentPack.namespace)) {
            currentPack = new QuestPack(currentPack.name, refreshedNamespace, currentPack.root);
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
        packNamespaceBox.setValue("");
        packIconPathBox.setValue("");
        packDescriptionBox.setValue("");

        setActiveFields(List.of(
                field("Pack name", packNameBox),
                field("Namespace", packNamespaceBox)
        ));
        saveButton.setMessage(Component.literal("Create"));
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
        packNamespaceBox.setValue(safe(pack.namespace));
        packIconPathBox.setValue(safe(meta.iconPath));
        packDescriptionBox.setValue(safe(meta.description));
        packDescriptionBox.scrollToTop();

        setActiveFields(List.of(
                field("Pack name", packNameBox),
                field("Icon path", packIconPathBox),
                field("Description", packDescriptionBox),
                field("Export", exportDataPackButton),
                field("Folder", openDataPackFolderButton)
        ));
        saveButton.setMessage(Component.literal("Save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(true);
        updateBackButtonVisibility();
    }

    private void showCategoryEditor(CategoryData data, Path sourcePath) {
        editorType = EditorType.CATEGORY;
        editingPath = sourcePath;

        catIdBox.setValue(safe(data.id));
        catNameBox.setValue(safe(data.name));
        catIconBox.setValue(safe(data.icon));
        catOrderBox.setValue(safe(data.order));
        catDependencyBox.setValue(safe(data.dependency));

        setActiveFields(List.of(
                field("Id", catIdBox),
                field("Name", catNameBox),
                field("Icon", catIconBox),
                field("Order", catOrderBox),
                field("Dependency", catDependencyBox)
        ));
        saveButton.setMessage(Component.literal("Save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    private void showSubCategoryEditor(SubCategoryData data, Path sourcePath) {
        editorType = EditorType.SUBCATEGORY;
        editingPath = sourcePath;

        subIdBox.setValue(safe(data.id));
        subCategoryBox.setValue(safe(data.category));
        subNameBox.setValue(safe(data.name));
        subIconBox.setValue(safe(data.icon));
        subOrderBox.setValue(safe(data.order));
        subDefaultOpenToggle.setState(parseBool(data.defaultOpen, true));

        setActiveFields(List.of(
                field("Id", subIdBox),
                field("Category id", subCategoryBox),
                field("Name", subNameBox),
                field("Icon", subIconBox),
                field("Order", subOrderBox),
                field("Default open (true/false)", subDefaultOpenToggle)
        ));
        saveButton.setMessage(Component.literal("Save"));
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
        String questIndex = safe(data.index);
        String questName = safe(data.name);
        if (questIndex.isBlank()) {
            IndexName fromName = splitIndexName(questName);
            if (!fromName.index.isBlank()) {
                questIndex = fromName.index;
                questName = fromName.name;
            }
        }
        questIndexBox.setValue(questIndex);
        questNameBox.setValue(questName);
        questIconBox.setValue(safe(data.icon));
        questDescriptionBox.setValue(safe(data.description));
        questCategoryBox.setValue(safe(data.category));
        questSubCategoryBox.setValue(safe(data.subCategory));
        questDependenciesBox.setValue(safe(data.dependencies));
        questOptionalToggle.setState(parseBool(data.optional, false));
        questRepeatableToggle.setState(parseBool(data.repeatable, false));
        questHiddenUnderDependencyToggle.setState(parseBool(data.hiddenUnderDependency, false));
        setEntryRowsFromRaw(false, completionJsonToEntries(safe(data.completionJson)));
        setEntryRowsFromRaw(true, rewardJsonToEntries(safe(data.rewardJson)));
        loadedQuestType = safe(data.type);

        questDescriptionBox.scrollToTop();
        questCompletionBox.scrollToTop();
        questRewardBox.scrollToTop();

        setActiveFields(List.of(
                field("Id", questIdBox),
                field("Index", questIndexBox),
                field("Name", questNameBox),
                field("Icon", questIconBox),
                field("Description", questDescriptionBox),
                field("Category", questCategoryBox),
                field("Sub-category", questSubCategoryBox),
                field("Dependencies (comma separated)", questDependenciesBox),
                field("Flags", questOptionalToggle),
                field("Completion", questCompletionBox),
                field("Reward", questRewardBox)
        ));
        saveButton.setMessage(Component.literal("Save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
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
        String namespace = safe(packNamespaceBox.getValue()).trim().toLowerCase(Locale.ROOT);

        if (name.isBlank() || namespace.isBlank()) {
            setError("Pack name and namespace required");
            return;
        }

        Path root = packsRoot().resolve(name);
        if (Files.exists(root)) {
            setError("Pack already exists");
            return;
        }

        try {
            Files.createDirectories(root);
            writePackMeta(root, name, "Boundless Quest Pack: " + name, "");
            writePackIcon(root, "");
            QuestPack pack = new QuestPack(name, namespace, root);
            pack.ensureDirs();
            currentPack = pack;
            setMode(Mode.PACK_MENU);
            stagePackChange(pack, "Pack staged");
        } catch (IOException e) {
            setError("Failed to create pack");
        }
    }

    private void savePackOptions() {
        if (currentPack == null) return;

        String requestedName = safe(packNameBox.getValue()).trim();
        String iconPath = safe(packIconPathBox.getValue()).trim();
        String description = safe(packDescriptionBox.getValue()).trim();
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
            writePackMeta(newRoot, requestedName, description.isBlank() ? "Boundless Quest Pack: " + requestedName : description, iconPath);
            writePackIcon(newRoot, iconPath);
            currentPack = new QuestPack(requestedName, currentPack.namespace, newRoot);
            currentPack.ensureDirs();
            selectedEntryId = currentPack.name;
            leftList.setSelectedId(selectedEntryId);
            if (!requestedName.equals(oldName)) {
                stagePackDeletion(new QuestPack(oldName, currentPack.namespace, oldRoot), "Pack options saved");
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
        addOptionalInt(obj, "order", catOrderBox.getValue());
        addOptional(obj, "dependency", catDependencyBox.getValue());
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
        addOptional(obj, "category", subCategoryBox.getValue());
        addOptional(obj, "name", subNameBox.getValue());
        addOptional(obj, "icon", subIconBox.getValue());
        addOptionalInt(obj, "order", subOrderBox.getValue());
        obj.addProperty("default_open", subDefaultOpenToggle.isOn());
        return obj;
    }

    private void saveQuest() {
        String id = safe(questIdBox.getValue()).trim();
        selectedEntryId = id;
        JsonObject obj = buildQuestJson(id);
        if (obj == null) return;

        Path target = currentPack.questsDir.resolve(questFileBaseName(id, questIndexBox.getValue()) + ".json");
        saveJson(obj, target, editingPath);
    }

    private JsonObject buildQuestJson(String id) {
        String questId = safe(id).trim();
        if (questId.isBlank()) {
            setError("Quest id required");
            return null;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("id", questId);
        String nameRaw = safe(questNameBox.getValue()).trim();
        addOptional(obj, "name", nameRaw);
        addOptional(obj, "icon", questIconBox.getValue());
        addOptional(obj, "description", questDescriptionBox.getValue());
        addOptional(obj, "category", questCategoryBox.getValue());
        addOptional(obj, "sub-category", questSubCategoryBox.getValue());
        obj.addProperty("optional", questOptionalToggle.isOn());
        obj.addProperty("repeatable", questRepeatableToggle.isOn());
        obj.addProperty("hiddenUnderDependency", questHiddenUnderDependencyToggle.isOn());
        if (loadedQuestType != null && !loadedQuestType.isBlank()) {
            obj.addProperty("type", loadedQuestType);
        }

        String depsRaw = safe(questDependenciesBox.getValue()).trim();
        if (!depsRaw.isBlank()) {
            String[] parts = depsRaw.split(",");
            List<String> deps = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) deps.add(trimmed);
            }
            if (deps.size() == 1) {
                obj.addProperty("dependencies", deps.get(0));
            } else if (!deps.isEmpty()) {
                var arr = new com.google.gson.JsonArray();
                for (String dep : deps) arr.add(dep);
                obj.add("dependencies", arr);
            }
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
        } catch (IOException e) {
            setError("Save failed");
        }
    }

    private void exportCurrentPackAsDataPack() {
        if (currentPack == null) return;
        try {
            if (!ensurePackWorkspace(currentPack)) {
                setError("Failed to open pack");
                return;
            }
            Path target = dataPacksRoot().resolve(currentPack.name);
            mirrorDirectory(currentPack.root, target);
            statusMessage = "Datapack exported";
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError("Datapack export failed");
        }
    }

    private void openDataPackFolder() {
        try {
            Path folder = dataPacksRoot();
            Files.createDirectories(folder);
            Util.getPlatform().openFile(folder.toFile());
        } catch (Exception e) {
            setError("Failed to open folder");
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

        Path target = currentPack.questsDir.resolve(questFileBaseName(newId, questIndexBox.getValue()) + ".json");
        selectedEntryId = newId;
        saveJson(obj, target, null);
        QuestEntryData data = loadQuest(currentPack, newId);
        if (data != null) {
            showQuestEditor(data, target);
        }
    }

    private void duplicateCategory() {
        if (currentPack == null || editorType != EditorType.CATEGORY) return;
        String baseId = safe(catIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError("Category id required");
            return;
        }
        String newId = nextAvailableId(baseId, currentPack.categoriesDir);
        JsonObject obj = buildCategoryJson(newId);
        if (obj == null) return;
        Path target = currentPack.categoriesDir.resolve(newId + ".json");
        selectedEntryId = newId;
        saveJson(obj, target, null);
        CategoryData data = loadCategory(currentPack, newId);
        if (data != null) showCategoryEditor(data, target);
    }

    private void duplicateSubCategory() {
        if (currentPack == null || editorType != EditorType.SUBCATEGORY) return;
        String baseId = safe(subIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError("Sub-category id required");
            return;
        }
        String newId = nextAvailableId(baseId, currentPack.subCategoriesDir);
        JsonObject obj = buildSubCategoryJson(newId);
        if (obj == null) return;
        Path target = currentPack.subCategoriesDir.resolve(newId + ".json");
        selectedEntryId = newId;
        saveJson(obj, target, null);
        SubCategoryData data = loadSubCategory(currentPack, newId);
        if (data != null) showSubCategoryEditor(data, target);
    }

    private void duplicateCurrent() {
        switch (editorType) {
            case QUEST -> duplicateQuest();
            case CATEGORY -> duplicateCategory();
            case SUBCATEGORY -> duplicateSubCategory();
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
        switch (editorType) {
            case QUEST -> deleteQuest();
            case CATEGORY -> deleteCategory();
            case SUBCATEGORY -> deleteSubCategory();
            default -> {}
        }
    }

    private void handleDeleteButtonPress() {
        if (editorType != EditorType.QUEST && editorType != EditorType.CATEGORY && editorType != EditorType.SUBCATEGORY) {
            disarmDeleteConfirm();
            return;
        }
        if (!deleteConfirmArmed) {
            deleteConfirmArmed = true;
            updateDeleteButtonTexture();
            statusMessage = "Are you sure? Click delete again to confirm";
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
            deleteQuestButton.setTexture(deleteConfirmArmed ? DELETE_CONFIRM_TEX : DELETE_TEX);
        }
    }

    private String nextAvailableQuestId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;

        return nextAvailableId(base, currentPack.questsDir);
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

    private void confirmDeletePack() {
        if (currentPack == null || mode != Mode.PACK_MENU) return;
        long now = Util.getMillis();
        if (now < deletePackConfirmUntil) {
            deletePackConfirmUntil = 0L;
            deletePack(currentPack);
            return;
        }
        deletePackConfirmUntil = now + 5000L;
        statusMessage = "Are you sure? Click delete again to confirm";
        statusColor = 0xFFD080;
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
        boolean zipped = zipCurrentPackSafe();
        ensurePackSelected();
        Minecraft.getInstance().reloadResourcePacks().thenRun(() -> Minecraft.getInstance().execute(() -> {
            if (pendingState != null) {
                ScreenState restore = pendingState;
                pendingState = null;
                restoreState(restore);
            }
            QuestData.loadClient(true);
        }));
        return mirrored || zipped;
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
            changed |= zipCurrentPackSafe();
            if (selectedPack == null) {
                selectedPack = pack;
            }
        }

        currentPack = selectedPack;
        if (selectedPack != null && !stagedDeletedPackNames.contains(selectedPack.name)) {
            ensurePackSelected();
        }
        stagedPacks.clear();
        stagedDeletedPackNames.clear();
        if (!changed) return;

        mc.reloadResourcePacks().thenRun(() ->
                mc.execute(() -> QuestData.loadClient(true)));
    }

    private boolean deleteAppliedPackArtifactsSafe(String packName) {
        if (packName == null || packName.isBlank()) return false;
        boolean changed = false;
        try {
            Path mirroredDir = resourcePacksRoot().resolve(packName);
            if (Files.exists(mirroredDir)) {
                deleteDirectory(mirroredDir);
                changed = true;
            }
            Path zipPath = packZipPath(packName);
            if (Files.deleteIfExists(zipPath)) {
                changed = true;
            }
        } catch (IOException ignored) {
        }
        return changed;
    }

    private boolean mirrorCurrentPackDirectorySafe() {
        if (currentPack == null || currentPack.root == null) return false;
        try {
            Path targetRoot = resourcePacksRoot().resolve(currentPack.name);
            mirrorDirectory(currentPack.root, targetRoot);
            return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    private void mirrorDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        if (sourceRoot == null || targetRoot == null) return;
        if (!Files.isDirectory(sourceRoot)) throw new IOException("Source pack folder missing");
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

    private void ensurePackSelected() {
        if (currentPack == null || currentPack.name == null || currentPack.name.isBlank()) return;
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        if (repo == null) return;
        try {
            repo.reload();
        } catch (Exception ignored) {
        }

        String packId = findPackId(repo, currentPack);
        if (packId == null || packId.isBlank()) return;
        applyPackSelectionToRepo(repo, packId);
        if (ensurePackSelectedInOptions(mc, packId)) {
            invokeOptionsSave(mc.options);
        }
    }

    private boolean isPackEnabled(QuestPack pack) {
        if (pack == null) return false;
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        if (repo == null) return false;
        try {
            repo.reload();
        } catch (Exception ignored) {
        }
        String packId = findPackId(repo, pack);
        if (packId == null || packId.isBlank()) return false;
        var selected = repo.getSelectedPacks();
        if (selected == null) return false;
        for (Object p : selected) {
            String id = packIdOf(p);
            if (packId.equals(id)) return true;
        }
        return false;
    }

    private void setPackEnabled(QuestPack pack, boolean enabled) {
        if (pack == null) return;
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        if (repo == null) return;
        try {
            repo.reload();
        } catch (Exception ignored) {
        }
        String packId = findPackId(repo, pack);
        if (packId == null || packId.isBlank()) return;
        applyPackSelectionToRepo(repo, packId, enabled);
        if (setPackSelectedInOptions(mc, packId, enabled)) {
            invokeOptionsSave(mc.options);
        }
        mc.reloadResourcePacks().thenRun(() ->
                mc.execute(() -> QuestData.loadClient(true)));
        statusMessage = enabled ? ("Enabled " + pack.name) : ("Disabled " + pack.name);
        statusColor = 0xA0FFA0;
        refreshLeftList();
    }

    private boolean ensurePackSelectedInOptions(Minecraft mc, String packId) {
        return setPackSelectedInOptions(mc, packId, true);
    }

    private boolean setPackSelectedInOptions(Minecraft mc, String packId, boolean enabled) {
        if (mc == null || mc.options == null || packId == null || packId.isBlank()) return false;
        Object options = mc.options;
        List<String> selected = getOptionsList(options, "resourcePacks");
        if (selected == null) return false;
        if (enabled) {
            if (!selected.contains(packId)) {
                return addOptionEntry(options, "resourcePacks", selected, packId);
            }
            return false;
        }
        return removeOptionEntry(options, "resourcePacks", selected, packId);
    }

    private boolean addOptionEntry(Object options, String fieldName, List<String> current, String entry) {
        if (current == null || entry == null) return false;
        try {
            current.add(preferredPackInsertIndex(current), entry);
            return true;
        } catch (UnsupportedOperationException ignored) {
        }
        List<String> copy = new ArrayList<>(current);
        if (copy.contains(entry)) return false;
        copy.add(preferredPackInsertIndex(copy), entry);
        setOptionsList(options, fieldName, copy);
        return true;
    }

    private boolean removeOptionEntry(Object options, String fieldName, List<String> current, String entry) {
        if (current == null || entry == null) return false;
        try {
            return current.remove(entry);
        } catch (UnsupportedOperationException ignored) {
        }
        if (!current.contains(entry)) return false;
        List<String> copy = new ArrayList<>(current);
        if (!copy.remove(entry)) return false;
        setOptionsList(options, fieldName, copy);
        return true;
    }

    private int preferredPackInsertIndex(List<String> selected) {
        if (selected == null || selected.isEmpty()) return 0;
        int idx = 0;
        for (String id : selected) {
            if (!"vanilla".equals(id) && !"mod_resources".equals(id)) {
                break;
            }
            idx++;
        }
        return Math.max(0, Math.min(idx, selected.size()));
    }

    private List<String> getOptionsList(Object options, String fieldName) {
        if (options == null || fieldName == null || fieldName.isBlank()) return null;
        try {
            Field field = options.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(options);
            if (value instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<String> out = (List<String>) list;
                return out;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void setOptionsList(Object options, String fieldName, List<String> next) {
        if (options == null || fieldName == null || fieldName.isBlank()) return;
        try {
            Field field = options.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(options, next);
        } catch (Exception ignored) {
        }
    }

    private void invokeOptionsSave(Object options) {
        if (options == null) return;
        try {
            Method save = options.getClass().getMethod("save");
            save.invoke(options);
        } catch (Exception ignored) {
        }
    }

    private void applyPackSelectionToRepo(PackRepository repo, String packId) {
        applyPackSelectionToRepo(repo, packId, true);
    }

    private void applyPackSelectionToRepo(PackRepository repo, String packId, boolean enabled) {
        if (repo == null || packId == null || packId.isBlank()) return;
        List<String> selectedIds = new ArrayList<>();
        var selected = repo.getSelectedPacks();
        if (selected != null) {
            for (Object p : selected) {
                String id = packIdOf(p);
                if (id != null && !id.isBlank() && !selectedIds.contains(id)) {
                    selectedIds.add(id);
                }
            }
        }
        if (enabled && !selectedIds.contains(packId)) {
            selectedIds.add(packId);
        } else if (!enabled) {
            selectedIds.remove(packId);
        }
        for (Method method : repo.getClass().getMethods()) {
            if (!"setSelected".equals(method.getName())) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1) continue;
            if (params[0].isAssignableFrom(selectedIds.getClass())) {
                try {
                    method.invoke(repo, selectedIds);
                    return;
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String findPackId(PackRepository repo, QuestPack pack) {
        if (repo == null || pack == null) return null;
        String name = safe(pack.name).trim();
        if (name.isBlank()) return null;
        String nameLower = name.toLowerCase(Locale.ROOT);
        String[] candidates = new String[] {
                "file/" + name,
                "file/" + name + ".zip",
                name,
                name + ".zip"
        };

        String expectedDescription = "Boundless Quest Pack: " + name;
        String fallback = null;
        var available = repo.getAvailablePacks();
        if (available == null) return null;
        for (Object p : available) {
            String id = packIdOf(p);
            if (id == null || id.isBlank()) continue;
            for (String candidate : candidates) {
                if (id.equals(candidate)) return id;
            }
            String idLower = id.toLowerCase(Locale.ROOT);
            if (idLower.equals(nameLower) || idLower.endsWith("/" + nameLower)
                    || idLower.endsWith("/" + nameLower + ".zip") || idLower.endsWith(nameLower + ".zip")) {
                fallback = id;
            }
        }

        if (fallback != null) return fallback;
        for (Object p : available) {
            String id = packIdOf(p);
            if (id == null || id.isBlank()) continue;
            String description = packDescriptionOf(p);
            String title = packTitleOf(p);
            if ((description != null && description.contains(expectedDescription))
                    || (title != null && title.contains(expectedDescription))) {
                return id;
            }
        }

        for (Object p : available) {
            String id = packIdOf(p);
            if (id == null || id.isBlank()) continue;
            if (id.toLowerCase(Locale.ROOT).contains(nameLower)) return id;
        }
        return null;
    }

    private String packIdOf(Object pack) {
        return invokeString(pack, "getId", "id");
    }

    private String packTitleOf(Object pack) {
        Object result = invokeObject(pack, "getTitle", "title");
        if (result instanceof Component component) {
            return component.getString();
        }
        return result instanceof String s ? s : null;
    }

    private String packDescriptionOf(Object pack) {
        Object result = invokeObject(pack, "getDescription", "description");
        if (result instanceof Component component) {
            return component.getString();
        }
        return result instanceof String s ? s : null;
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
        editorType = EditorType.NONE;
        editingPath = null;
        loadedQuestType = "";
        disarmDeleteConfirm();
        clearPendingDiscardState();
        savedEditorState = null;
        setActiveFields(List.of());
        saveButton.visible = false;
        saveButton.active = false;
        updateBackButtonVisibility();
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
        if (questHiddenUnderDependencyToggle != null) {
            questHiddenUnderDependencyToggle.visible = false;
            questHiddenUnderDependencyToggle.active = false;
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
        for (EntryRemoveButton button : rewardEntryRemoveButtons) {
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
        if (descriptionUndoButton != null) {
            descriptionUndoButton.visible = false;
            descriptionUndoButton.active = false;
        }
        if (descriptionRedoButton != null) {
            descriptionRedoButton.visible = false;
            descriptionRedoButton.active = false;
        }
    }

    private void initEntryRowBoxes() {
        resetEntryRows(completionEntryBoxes, List.of(), false);
        resetEntryRows(rewardEntryBoxes, List.of(), true);
        syncEntryBackingValues();
    }

    private ScaledMultiLineEditBox createEntryRowBox(boolean reward) {
        String hint = reward
                ? "item: minecraft:item 1"
                : "collect: minecraft:item 1";
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

    private void resetEntryRows(List<ScaledMultiLineEditBox> target, List<String> lines, boolean reward) {
        syncingEntryRows = true;
        try {
            for (ScaledMultiLineEditBox box : target) {
                removeWidget(box);
            }
            target.clear();
            List<EntryRemoveButton> removeButtons = reward ? rewardEntryRemoveButtons : completionEntryRemoveButtons;
            for (EntryRemoveButton button : removeButtons) {
                removeWidget(button);
            }
            removeButtons.clear();
            List<String> normalized = new ArrayList<>();
            if (lines != null) {
                for (String line : lines) {
                    String v = safe(line).trim();
                    if (!v.isBlank()) normalized.add(v);
                }
            }
            if (normalized.isEmpty()) normalized.add("");
            for (String line : normalized) {
                ScaledMultiLineEditBox box = createEntryRowBox(reward);
                box.setValue(line);
                target.add(box);
            }
            ensureTrailingEmptyRow(target, reward);
            syncEntryRemoveButtons(reward);
        } finally {
            syncingEntryRows = false;
        }
    }

    private void normalizeEntryRows(boolean reward) {
        List<ScaledMultiLineEditBox> rows = reward ? rewardEntryBoxes : completionEntryBoxes;
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
            ensureTrailingEmptyRow(rows, reward);
        } finally {
            syncingEntryRows = false;
        }
    }

    private void ensureTrailingEmptyRow(List<ScaledMultiLineEditBox> rows, boolean reward) {
        if (rows.isEmpty()) {
            rows.add(createEntryRowBox(reward));
            syncEntryRemoveButtons(reward);
            return;
        }
        ScaledMultiLineEditBox last = rows.get(rows.size() - 1);
        if (!safe(last.getValue()).trim().isBlank()) {
            rows.add(createEntryRowBox(reward));
        }
        while (rows.size() > 1) {
            ScaledMultiLineEditBox prev = rows.get(rows.size() - 2);
            if (!safe(prev.getValue()).trim().isBlank() || prev.isFocused()) break;
            removeWidget(prev);
            rows.remove(rows.size() - 2);
        }
        syncEntryRemoveButtons(reward);
    }

    private void syncEntryRemoveButtons(boolean reward) {
        List<ScaledMultiLineEditBox> rows = reward ? rewardEntryBoxes : completionEntryBoxes;
        List<EntryRemoveButton> buttons = reward ? rewardEntryRemoveButtons : completionEntryRemoveButtons;
        while (buttons.size() < rows.size()) {
            EntryRemoveButton button = new EntryRemoveButton(reward);
            button.visible = false;
            button.active = false;
            buttons.add(button);
            addRenderableWidget(button);
        }
        while (buttons.size() > rows.size()) {
            EntryRemoveButton last = buttons.remove(buttons.size() - 1);
            removeWidget(last);
        }
    }

    private void removeEntryRow(boolean reward, EntryRemoveButton button) {
        List<ScaledMultiLineEditBox> rows = reward ? rewardEntryBoxes : completionEntryBoxes;
        List<EntryRemoveButton> buttons = reward ? rewardEntryRemoveButtons : completionEntryRemoveButtons;
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
        removeWidget(removed);
        EntryRemoveButton removedButton = buttons.remove(idx);
        removeWidget(removedButton);

        ensureTrailingEmptyRow(rows, reward);
        if (!rows.isEmpty()) {
            int next = Math.min(idx, rows.size() - 1);
            rows.get(next).setFocused(true);
        }
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    private void setEntryRowsFromRaw(boolean reward, String raw) {
        resetEntryRows(reward ? rewardEntryBoxes : completionEntryBoxes, extractEntryLines(raw), reward);
        syncEntryBackingValues();
    }

    private String entryRowsToRaw(boolean reward) {
        List<ScaledMultiLineEditBox> rows = reward ? rewardEntryBoxes : completionEntryBoxes;
        List<String> out = new ArrayList<>();
        for (ScaledMultiLineEditBox box : rows) {
            String v = safe(box.getValue()).trim();
            if (!v.isBlank()) out.add(v);
        }
        return String.join("\n", out);
    }

    private int entryRowsHeight(boolean reward) {
        int count = Math.max(1, (reward ? rewardEntryBoxes : completionEntryBoxes).size());
        return (count * ENTRY_ROW_H) + ((count - 1) * ENTRY_ROW_GAP);
    }

    private int layoutEntryRows(boolean reward, int x, int y, int width, int clipTop, int clipBottom) {
        List<ScaledMultiLineEditBox> rows = reward ? rewardEntryBoxes : completionEntryBoxes;
        List<EntryRemoveButton> removeButtons = reward ? rewardEntryRemoveButtons : completionEntryRemoveButtons;
        int cursorY = y;
        for (int i = 0; i < rows.size(); i++) {
            ScaledMultiLineEditBox box = rows.get(i);
            EntryRemoveButton removeButton = i < removeButtons.size() ? removeButtons.get(i) : null;
            box.setX(x);
            box.setY(cursorY);
            box.setWidth(Math.max(10, width - ENTRY_REMOVE_BTN_W - 2));
            box.setHeight(ENTRY_ROW_H);
            boolean inside = cursorY + ENTRY_ROW_H > clipTop && cursorY < clipBottom;
            box.visible = inside;
            box.active = inside;
            if (removeButton != null) {
                removeButton.setPosition(x + width - ENTRY_REMOVE_BTN_W, cursorY);
                removeButton.setWidth(ENTRY_REMOVE_BTN_W);
                removeButton.setHeight(ENTRY_ROW_H);
                removeButton.visible = inside;
                removeButton.active = inside;
            }
            cursorY += ENTRY_ROW_H + ENTRY_ROW_GAP;
        }
        return Math.max(ENTRY_ROW_H, cursorY - y - ENTRY_ROW_GAP);
    }

    private void setEntryRowsColor(boolean reward, boolean invalid) {
        for (ScaledMultiLineEditBox box : reward ? rewardEntryBoxes : completionEntryBoxes) {
            box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
        }
    }

    private void syncEntryBackingValues() {
        normalizeCommandRewardRows();
        if (questCompletionBox != null) questCompletionBox.setValue(entryRowsToRaw(false));
        if (questRewardBox != null) questRewardBox.setValue(entryRowsToRaw(true));
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

    private void setError(String msg) {
        statusMessage = msg;
        statusColor = 0xFF8080;
    }

    private void applyDescriptionFormat(String formatCode) {
        if (questDescriptionBox == null) return;
        questDescriptionBox.insertText(formatCode);
        questDescriptionBox.setFocused(true);
    }

    private void undoDescriptionFormat() {
        if (questDescriptionBox == null) return;
        questDescriptionBox.undo();
        questDescriptionBox.setFocused(true);
    }

    private void redoDescriptionFormat() {
        if (questDescriptionBox == null) return;
        questDescriptionBox.redo();
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
                    + "|" + safe(catOrderBox == null ? "" : catOrderBox.getValue())
                    + "|" + safe(catDependencyBox == null ? "" : catDependencyBox.getValue());
            case SUBCATEGORY -> "subcategory|" + safe(subIdBox == null ? "" : subIdBox.getValue())
                    + "|" + safe(subCategoryBox == null ? "" : subCategoryBox.getValue())
                    + "|" + safe(subNameBox == null ? "" : subNameBox.getValue())
                    + "|" + safe(subIconBox == null ? "" : subIconBox.getValue())
                    + "|" + safe(subOrderBox == null ? "" : subOrderBox.getValue())
                    + "|" + (subDefaultOpenToggle != null && subDefaultOpenToggle.isOn());
            case QUEST -> "quest|" + safe(questIdBox == null ? "" : questIdBox.getValue())
                    + "|" + safe(questIndexBox == null ? "" : questIndexBox.getValue())
                    + "|" + safe(questNameBox == null ? "" : questNameBox.getValue())
                    + "|" + safe(questIconBox == null ? "" : questIconBox.getValue())
                    + "|" + safe(questDescriptionBox == null ? "" : questDescriptionBox.getValue())
                    + "|" + safe(questCategoryBox == null ? "" : questCategoryBox.getValue())
                    + "|" + safe(questSubCategoryBox == null ? "" : questSubCategoryBox.getValue())
                    + "|" + safe(questDependenciesBox == null ? "" : questDependenciesBox.getValue())
                    + "|" + (questOptionalToggle != null && questOptionalToggle.isOn())
                    + "|" + (questRepeatableToggle != null && questRepeatableToggle.isOn())
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
    private Path resourcePacksRoot() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("resourcepacks");
    }

    private Path packsRoot() {
        return resourcePacksRoot().resolve("boundless");
    }

    private Path dataPacksRoot() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("datapacks")
                .resolve("boundless");
    }

    private Path packZipPath(String packName) {
        return resourcePacksRoot().resolve(packName + ".zip");
    }

    private boolean isInvalidPackFolderName(String name) {
        String value = safe(name).trim();
        return value.isBlank() || value.matches(".*[<>:\"/\\\\|?*].*");
    }

    private List<QuestPack> listPacks() {
        List<QuestPack> packs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Path root = packsRoot();
        if (Files.exists(root)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path path : stream) {
                    if (!Files.isDirectory(path)) continue;
                    String name = path.getFileName().toString();
                    String namespace = findNamespace(path);
                    packs.add(new QuestPack(name, namespace, path));
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
                    packs.add(new QuestPack(name, namespace, packsRoot().resolve(name)));
                    seen.add(name);
                }
            } catch (IOException ignored) {
            }
        }

        packs.sort(Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)));
        return packs;
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
        if (Files.exists(root)) return false;

        Path zip = packZipPath(pack.name);
        if (!Files.exists(zip)) return false;

        try {
            unzipPack(zip, root);
            return true;
        } catch (IOException ignored) {
        }
        return false;
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
        if (root == null) return meta;
        Path packMeta = root.resolve("pack.mcmeta");
        if (!Files.exists(packMeta)) return meta;
        try (BufferedReader reader = Files.newBufferedReader(packMeta, StandardCharsets.UTF_8)) {
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
        } catch (Exception ignored) {
        }
        return meta;
    }

    private void writePackMeta(Path root, String name, String description, String iconPath) throws IOException {
        JsonObject pack = new JsonObject();
        JsonObject body = new JsonObject();
        body.addProperty("pack_format", PACK_FORMAT);
        body.addProperty("description", description);
        pack.add("pack", body);
        JsonObject boundless = new JsonObject();
        if (iconPath != null && !iconPath.isBlank()) {
            boundless.addProperty("icon_path", iconPath);
        }
        pack.add("boundless", boundless);

        Path meta = root.resolve("pack.mcmeta");
        try (BufferedWriter writer = Files.newBufferedWriter(meta, StandardCharsets.UTF_8)) {
            gson.toJson(pack, writer);
        }
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

    private boolean zipCurrentPackSafe() {
        if (currentPack == null) return true;
        try {
            zipPack(currentPack);
            return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    private void zipPack(QuestPack pack) throws IOException {
        if (pack == null || pack.root == null) return;
        if (!Files.isDirectory(pack.root)) throw new IOException("Pack folder missing");

        Path zipPath = packZipPath(pack.name);
        Files.createDirectories(zipPath.getParent());

        Path tmp = zipPath.resolveSibling(zipPath.getFileName().toString() + ".tmp");
        Files.deleteIfExists(tmp);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tmp))) {
            try (var walk = Files.walk(pack.root)) {
                walk.forEach(path -> {
                    String entryName = pack.root.relativize(path).toString().replace('\\', '/');
                    if (entryName.isEmpty()) return;
                    try {
                        if (Files.isDirectory(path)) {
                            if (!entryName.endsWith("/")) entryName += "/";
                            zos.putNextEntry(new ZipEntry(entryName));
                            zos.closeEntry();
                            return;
                        }
                        ZipEntry entry = new ZipEntry(entryName);
                        zos.putNextEntry(entry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        Files.move(tmp, zipPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void unzipPack(Path zipPath, Path destRoot) throws IOException {
        if (zipPath == null || destRoot == null) return;
        if (Files.exists(destRoot) && !Files.isDirectory(destRoot)) {
            throw new IOException("Destination is not a directory");
        }

        Path safeRoot = destRoot.toAbsolutePath().normalize();
        Files.createDirectories(safeRoot);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String rawName = entry.getName();
                if (rawName == null || rawName.isBlank()) continue;
                String cleanName = rawName.replace('\\', '/');
                while (cleanName.startsWith("/")) cleanName = cleanName.substring(1);

                Path out = safeRoot.resolve(cleanName).normalize();
                if (!out.startsWith(safeRoot)) continue;

                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                    continue;
                }

                Path parent = out.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
            }
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
        state.editorScroll = editorScroll;
        state.leftScroll = leftList.getScrollY();
        state.statusMessage = statusMessage;
        state.statusColor = statusColor;
        state.deletePackConfirmUntil = deletePackConfirmUntil;
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
        state.catOrder = safe(catOrderBox == null ? "" : catOrderBox.getValue());
        state.catDependency = safe(catDependencyBox == null ? "" : catDependencyBox.getValue());

        state.subId = safe(subIdBox == null ? "" : subIdBox.getValue());
        state.subCategory = safe(subCategoryBox == null ? "" : subCategoryBox.getValue());
        state.subName = safe(subNameBox == null ? "" : subNameBox.getValue());
        state.subIcon = safe(subIconBox == null ? "" : subIconBox.getValue());
        state.subOrder = safe(subOrderBox == null ? "" : subOrderBox.getValue());
        state.subDefaultOpen = subDefaultOpenToggle != null && subDefaultOpenToggle.isOn();

        state.questId = safe(questIdBox == null ? "" : questIdBox.getValue());
        state.questIndex = safe(questIndexBox == null ? "" : questIndexBox.getValue());
        state.questName = safe(questNameBox == null ? "" : questNameBox.getValue());
        state.questIcon = safe(questIconBox == null ? "" : questIconBox.getValue());
        state.questDescription = safe(questDescriptionBox == null ? "" : questDescriptionBox.getValue());
        state.questCategory = safe(questCategoryBox == null ? "" : questCategoryBox.getValue());
        state.questSubCategory = safe(questSubCategoryBox == null ? "" : questSubCategoryBox.getValue());
        state.questDependencies = safe(questDependenciesBox == null ? "" : questDependenciesBox.getValue());
        state.questOptional = questOptionalToggle != null && questOptionalToggle.isOn();
        state.questRepeatable = questRepeatableToggle != null && questRepeatableToggle.isOn();
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
        if (preservedStatusMessage == null || preservedStatusMessage.isBlank()) {
            statusMessage = state.statusMessage == null ? "" : state.statusMessage;
            statusColor = state.statusColor;
        } else {
            statusMessage = preservedStatusMessage;
            statusColor = preservedStatusColor;
        }
        deletePackConfirmUntil = state.deletePackConfirmUntil;
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
                data.order = state.catOrder;
                data.dependency = state.catDependency;
                showCategoryEditor(data, state.editingPath);
            }
            case SUBCATEGORY -> {
                SubCategoryData data = new SubCategoryData();
                data.id = state.subId;
                data.category = state.subCategory;
                data.name = state.subName;
                data.icon = state.subIcon;
                data.order = state.subOrder;
                data.defaultOpen = state.subDefaultOpen ? "true" : "false";
                showSubCategoryEditor(data, state.editingPath);
            }
            case QUEST -> {
                QuestEntryData data = new QuestEntryData();
                data.index = state.questIndex;
                data.id = state.questId;
                data.name = state.questName;
                data.icon = state.questIcon;
                data.description = state.questDescription;
                data.category = state.questCategory;
                data.subCategory = state.questSubCategory;
                data.dependencies = state.questDependencies;
                data.optional = state.questOptional ? "true" : "false";
                data.repeatable = state.questRepeatable ? "true" : "false";
                data.hiddenUnderDependency = state.questHiddenUnderDependency ? "true" : "false";
                data.type = state.loadedQuestType;
                data.completionJson = state.questCompletion;
                data.rewardJson = state.questReward;
                showQuestEditor(data, state.editingPath);
                setEntryRowsFromRaw(false, safe(state.questCompletion));
                setEntryRowsFromRaw(true, safe(state.questReward));
            }
            case NONE -> clearEditor();
        }

        loadedQuestType = state.loadedQuestType == null ? "" : state.loadedQuestType;
        editingPath = state.editingPath;
        editorScroll = state.editorScroll;
        updateBackButtonVisibility();
    }

    private String combineIndexName(String index, String name) {
        String i = safe(index).trim();
        String n = safe(name).trim();
        if (!i.isBlank() && !n.isBlank()) return i + "-" + n;
        if (!i.isBlank()) return i + "-";
        return n;
    }

    private List<NamedEntry> listCategoryEntries(QuestPack pack) {
        return listEntries(pack.categoriesDir);
    }

    private List<NamedEntry> listSubCategoryEntries(QuestPack pack) {
        return listEntries(pack.subCategoriesDir);
    }

    private List<NamedEntry> listQuestEntries(QuestPack pack) {
        List<NamedEntry> entries = listEntries(pack.questsDir);
        entries.sort(Comparator.comparing(a -> safe(a.sortKey).toLowerCase(Locale.ROOT)));
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
                entries.add(new NamedEntry(id, name, icon, path, fileId(path)));
            }
        } catch (IOException ignored) {
        }

        entries.sort(Comparator.comparing(a -> a.id.toLowerCase(Locale.ROOT)));
        return entries;
    }

    private CategoryData loadCategory(QuestPack pack, String id) {
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
        return data;
    }

    private SubCategoryData loadSubCategory(QuestPack pack, String id) {
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
        IndexName indexName = splitIndexName(fileId(path));
        data.index = indexName.index;
        data.id = optString(obj, "id", id);
        data.name = optString(obj, "name", "");
        data.icon = optString(obj, "icon", "");
        data.description = optString(obj, "description", "");
        data.category = optString(obj, "category", "");
        data.subCategory = optString(obj, "sub-category", optString(obj, "subCategory", ""));
        data.dependencies = formatDependencies(obj.get("dependencies"));
        data.optional = optStringFlexible(obj, "optional", "");
        data.repeatable = optStringFlexible(obj, "repeatable", "");
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

    private JsonObject readJson(Path path) {
        if (path == null || !Files.exists(path)) return null;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
        }
        return null;
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
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, leftX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
        gg.blit(PANEL_TEX, rightX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        renderEditorFields(gg, mouseX, mouseY);
        renderPanelHeader(gg, leftX, panelHeaderTitle());
        renderSideTabs(gg, mouseX, mouseY);

        boolean leftVisible = leftList != null && leftList.visible;
        boolean backVisible = backButton != null && backButton.visible;
        boolean saveVisible = saveButton != null && saveButton.visible;
        boolean duplicateVisible = duplicateButton != null && duplicateButton.visible;
        boolean deleteQuestVisible = deleteQuestButton != null && deleteQuestButton.visible;
        boolean deletePackVisible = deletePackButton != null && deletePackButton.visible;
        boolean questSearchVisible = questSearchBox != null && questSearchBox.visible;
        if (leftList != null) leftList.visible = false;
        if (backButton != null) backButton.visible = false;
        if (saveButton != null) saveButton.visible = false;
        if (duplicateButton != null) duplicateButton.visible = false;
        if (deleteQuestButton != null) deleteQuestButton.visible = false;
        if (deletePackButton != null) deletePackButton.visible = false;
        if (questSearchBox != null) questSearchBox.visible = false;

        gg.enableScissor(pxRight, py, pxRight + pw, py + ph);
        super.render(gg, mouseX, mouseY, partialTick);
        gg.disableScissor();

        if (leftList != null) leftList.visible = leftVisible;
        if (backButton != null) backButton.visible = backVisible;
        if (saveButton != null) saveButton.visible = saveVisible;
        if (duplicateButton != null) duplicateButton.visible = duplicateVisible;
        if (deleteQuestButton != null) deleteQuestButton.visible = deleteQuestVisible;
        if (deletePackButton != null) deletePackButton.visible = deletePackVisible;
        if (questSearchBox != null) questSearchBox.visible = questSearchVisible;

        if (leftList != null && leftList.visible) leftList.render(gg, mouseX, mouseY, partialTick);
        if (backButton != null && backButton.visible) backButton.render(gg, mouseX, mouseY, partialTick);
        if (saveButton != null && saveButton.visible) saveButton.render(gg, mouseX, mouseY, partialTick);
        if (duplicateButton != null && duplicateButton.visible) duplicateButton.render(gg, mouseX, mouseY, partialTick);
        if (deleteQuestButton != null && deleteQuestButton.visible) deleteQuestButton.render(gg, mouseX, mouseY, partialTick);
        if (deletePackButton != null && deletePackButton.visible) deletePackButton.render(gg, mouseX, mouseY, partialTick);
        if (questSearchBox != null && questSearchBox.visible) questSearchBox.render(gg, mouseX, mouseY, partialTick);
        if (createPackButton != null && createPackButton.visible) createPackButton.render(gg, mouseX, mouseY, partialTick);
        renderIconOverlays(gg);
        if (leftList != null) leftList.renderHoverTooltipOnTop(gg);
        renderTabTooltip(gg, mouseX, mouseY);

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
        }
    }

    private void renderPanelHeader(GuiGraphics gg, int panelX, String title) {
        String text = safe(title);
        if (text.isBlank()) return;
        int textW = font.width(text);
        int headerW = Math.max(22, textW + 10);
        int x = panelX + 5;
        int y = topY - 7;
        int middleW = Math.max(0, headerW - HEADER_SLICE * 2);
        gg.blit(HEADER_TEX, x, y, 0, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
        if (middleW > 0) {
            for (int i = 0; i < middleW; i++) {
                gg.blit(HEADER_TEX, x + HEADER_SLICE + i, y, HEADER_SLICE, 0, 1, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
            }
        }
        gg.blit(HEADER_TEX, x + HEADER_SLICE + middleW, y, HEADER_TEX_W - HEADER_SLICE, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
        gg.drawString(font, text, x + (headerW - textW) / 2, y + 4, 0x404040, false);
    }

    private String panelHeaderTitle() {
        return switch (mode) {
            case PACK_LIST -> "Quest Packs";
            case PACK_CREATE -> "Create Pack";
            case CATEGORY_LIST -> "Categories";
            case SUBCATEGORY_LIST -> "Sub-categories";
            case QUEST_LIST -> "Quests";
            case PACK_MENU -> "Quest Packs";
        };
    }

    private void renderSavedState(GuiGraphics gg) {
        if (editorType == EditorType.NONE || saveButton == null || !saveButton.visible) return;
        String text = hasUnsavedEditorChanges() ? "Not saved" : "Saved";
        int color = hasUnsavedEditorChanges() ? 0xFFD080 : 0xA0FFA0;
        int x = pxRight + pw - font.width(text) - 2;
        int y = py - font.lineHeight - 12;
        gg.drawString(font, text, x, y, color, false);
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
        if (questIconBox != null && questIconBox.visible && questIconBox.isFocused()) return questIconBox;
        if (catIconBox != null && catIconBox.visible && catIconBox.isFocused()) return catIconBox;
        if (subIconBox != null && subIconBox.visible && subIconBox.isFocused()) return subIconBox;
        if (catDependencyBox != null && catDependencyBox.visible && catDependencyBox.isFocused()) return catDependencyBox;
        if (subCategoryBox != null && subCategoryBox.visible && subCategoryBox.isFocused()) return subCategoryBox;
        if (questCategoryBox != null && questCategoryBox.visible && questCategoryBox.isFocused()) return questCategoryBox;
        if (questSubCategoryBox != null && questSubCategoryBox.visible && questSubCategoryBox.isFocused()) return questSubCategoryBox;
        if (questDependenciesBox != null && questDependenciesBox.visible && questDependenciesBox.isFocused()) return questDependenciesBox;
        return null;
    }

    private ScaledMultiLineEditBox focusedMultiIdSuggestionField() {
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
        String raw = safe(field.getValue());
        if (field == questDependenciesBox) {
            int comma = raw.lastIndexOf(',');
            return comma >= 0 ? raw.substring(comma + 1).trim() : raw.trim();
        }
        return raw.trim();
    }

    private List<String> idSuggestionValuesForField(EditBox field) {
        if (field == null) return List.of();
        if (isIconBox(field)) {
            return itemSuggestions();
        } else if (field == catDependencyBox || field == subCategoryBox || field == questCategoryBox) {
            return categorySuggestionCache;
        } else if (field == questSubCategoryBox) {
            String cat = safe(questCategoryBox == null ? "" : questCategoryBox.getValue()).trim();
            if (!cat.isBlank()) {
                List<String> scoped = subCategoryByCategorySuggestion.get(cat.toLowerCase(Locale.ROOT));
                if (scoped != null && !scoped.isEmpty()) return scoped;
            }
            return subCategorySuggestionCache;
        } else if (field == questDependenciesBox) {
            return questSuggestionCache;
        }
        return List.of();
    }

    private List<String> idSuggestionValuesForMultiField(ScaledMultiLineEditBox field) {
        if (field == null) return List.of();
        MultiLineEntryContext ctx = parseMultiLineEntryContext(field);
        if (ctx == null) return List.of();
        if (!ctx.hasTypeSeparator) {
            return isCompletionEntryField(field)
                    ? List.of("collect", "submit", "kill", "achieve", "effect", "xp", "levelup")
                    : List.of("item", "xp", "command", "loot");
        }
        return switch (ctx.type) {
            case "collect", "submit", "item" -> itemSuggestions();
            case "kill", "entity" -> entitySuggestions();
            case "effect" -> effectSuggestions();
            case "achieve", "advancement" -> advancementSuggestions();
            case "loot", "loottable" -> lootTableSuggestions();
            case "xp", "exp" -> List.of("points", "levels", "levelup");
            case "levelup" -> List.of("level");
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

    private String multiLineSuggestionPrefix(ScaledMultiLineEditBox field) {
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
        if (field == questDependenciesBox) {
            String raw = safe(field.getValue());
            int comma = raw.lastIndexOf(',');
            String next = comma >= 0 ? raw.substring(0, comma + 1).trim() + " " + suggestion : suggestion;
            field.setValue(next.trim());
        } else {
            field.setValue(suggestion);
        }
        field.setCursorPosition(field.getValue().length());
        field.setHighlightPos(field.getCursorPosition());
        field.setFocused(true);
        updateIdSuggestions();
    }

    private void applyMultiLineIdSuggestion(ScaledMultiLineEditBox field, String suggestion) {
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
            normalizeEntryRows(false);
            normalizeEntryRows(true);
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
            boolean completionEntriesField = field.widget == questCompletionBox;
            boolean rewardEntriesField = field.widget == questRewardBox;
            int widgetHeight;

            if (!completionEntriesField && !rewardEntriesField) {
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

            if (completionEntriesField || rewardEntriesField) {
                field.widget.visible = false;
                field.widget.active = false;
                widgetHeight = layoutEntryRows(rewardEntriesField, boxX, boxY, widgetWidth, clipTop, clipBottom);
            } else if (field.widget == questOptionalToggle) {
                widgetHeight = renderInlineQuestFlags(gg, mouseX, mouseY, boxX, boxY, widgetWidth, clipTop, clipBottom);
            } else {
                widgetHeight = field.widget.getHeight();
                boolean inside = boxY + widgetHeight > clipTop && boxY < clipBottom;
                field.widget.visible = inside;
                field.widget.active = inside;
            }
            if (field.widget == questDescriptionBox && Config.enableDescriptionColors()) {
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
                    if (hoverLabel || hoverWidget) {
                        gg.renderTooltip(font, Component.literal(tooltip), mouseX, mouseY);
                    }
                }
            }
            yCursor += font.lineHeight + FIELD_LABEL_GAP + widgetHeight + FIELD_ROW_GAP;
            if (field.widget == questDescriptionBox && Config.enableDescriptionColors()) {
                yCursor += FORMAT_BAR_GAP + FORMAT_BAR_H;
            }
        }

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
        if (scrollIdSuggestions(mouseX, mouseY, scrollY)) {
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

    private boolean isInsideSuggestionBox(double mouseX, double mouseY) {
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null) return false;
        return mouseX >= bounds.x && mouseX <= bounds.x + bounds.w
                && mouseY >= bounds.y && mouseY <= bounds.y + bounds.h;
    }

    private boolean isInsideSuggestionTargetField(double mouseX, double mouseY) {
        return isInsideBox(catIconBox, mouseX, mouseY)
                || isInsideBox(subIconBox, mouseX, mouseY)
                || isInsideBox(questIconBox, mouseX, mouseY)
                || isInsideBox(catDependencyBox, mouseX, mouseY)
                || isInsideBox(subCategoryBox, mouseX, mouseY)
                || isInsideBox(questCategoryBox, mouseX, mouseY)
                || isInsideBox(questSubCategoryBox, mouseX, mouseY)
                || isInsideBox(questDependenciesBox, mouseX, mouseY)
                || isInsideMultiBox(questCompletionBox, mouseX, mouseY)
                || isInsideMultiBox(questRewardBox, mouseX, mouseY)
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
        }
        return total;
    }

    private int fieldHeight(FormField field) {
        if (field == null || field.widget == null) return 0;
        if (field.widget == questCompletionBox) return entryRowsHeight(false);
        if (field.widget == questRewardBox) return entryRowsHeight(true);
        if (field.widget == questOptionalToggle) return TOGGLE_SIZE + INLINE_FLAG_LABEL_H + 3;
        if (field.widget instanceof ScaledMultiLineEditBox mb) {
            int baseHeight = computeMultilineHeight(mb, BOX_H_TALL);
            if (field.widget == questDescriptionBox && Config.enableDescriptionColors()) {
                return baseHeight + FORMAT_BAR_GAP + FORMAT_BAR_H;
            }
            return baseHeight;
        }
        return field.widget.getHeight();
    }

    private void updateDynamicFieldSizes() {
        for (FormField field : activeFields) {
            if (field.widget == questCompletionBox || field.widget == questRewardBox) continue;
            if (field.widget instanceof ScaledMultiLineEditBox mb) {
                mb.setWidth(pw - 4);
                mb.setHeight(computeMultilineHeight(mb, BOX_H_TALL));
            }
        }
    }

    private void updateInvalidFieldStyles() {
        setIdFieldColor(catDependencyBox, isInvalidCategoryDependency());
        setIdFieldColor(subCategoryBox, isInvalidParentCategory());
        setIdFieldColor(questCategoryBox, isInvalidQuestCategory());
        setIdFieldColor(questSubCategoryBox, isInvalidQuestSubCategory());
        setIdFieldColor(questDependenciesBox, isInvalidQuestDependencies());
        setEntryRowsColor(false, isInvalidCompletionEntries());
        setEntryRowsColor(true, isInvalidRewardEntries());
    }

    private void setIdFieldColor(EditBox box, boolean invalid) {
        if (box == null) return;
        box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
    }

    private void setJsonFieldColor(ScaledMultiLineEditBox box, boolean invalid) {
        if (box == null) return;
        box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
    }

    private String tooltipForField(FormField field) {
        if (field == null || field.widget == null) return "";
        if (field.widget == catDependencyBox && isInvalidCategoryDependency()) return INVALID_ID_TOOLTIP;
        if (field.widget == subCategoryBox && isInvalidParentCategory()) return INVALID_ID_TOOLTIP;
        if (field.widget == questCategoryBox && isInvalidQuestCategory()) return INVALID_ID_TOOLTIP;
        if (field.widget == questSubCategoryBox && isInvalidQuestSubCategory()) return INVALID_ID_TOOLTIP;
        if (field.widget == questDependenciesBox && isInvalidQuestDependencies()) return INVALID_ID_TOOLTIP;
        return field.tooltip == null ? "" : field.tooltip;
    }

    private int renderInlineQuestFlags(GuiGraphics gg, int mouseX, int mouseY, int x, int y, int width, int clipTop, int clipBottom) {
        ToggleButton[] toggles = new ToggleButton[] { questOptionalToggle, questRepeatableToggle, questHiddenUnderDependencyToggle };
        String[] titles = new String[] { "Optional", "Repeatable", "Hidden" };
        String[][] tooltips = new String[][] {
                { "Optional", "Players can skip this quest without blocking progression." },
                { "Repeatable", "Players can restart this quest after claiming it." },
                { "Hidden Under Dependency", "Keeps this quest hidden until its dependencies are met." }
        };
        int segmentGap = 3;
        int segmentWidth = Math.max(20, (width - segmentGap * 2) / 3);
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
                int labelW = (int) Math.ceil(font.width(titles[i]) * scale);
                gg.pose().pushPose();
                gg.pose().scale(scale, scale, 1f);
                float inv = 1f / scale;
                gg.drawString(font, titles[i], (int) ((segmentX + (segmentWidth - labelW) / 2) * inv), (int) (y * inv), 0xFFFFFF, false);
                gg.pose().popPose();
            }

            boolean hovered = mouseX >= segmentX && mouseX <= segmentX + segmentWidth
                    && mouseY >= y && mouseY <= y + totalHeight;
            if (hovered) {
                gg.renderComponentTooltip(font, List.of(Component.literal(tooltips[i][0]), Component.literal(tooltips[i][1])), mouseX, mouseY);
            }
        }
        return totalHeight;
    }

    private boolean isInvalidCategoryDependency() {
        return isMissingCategoryId(safe(catDependencyBox == null ? "" : catDependencyBox.getValue()));
    }

    private boolean isInvalidParentCategory() {
        return isMissingCategoryId(safe(subCategoryBox == null ? "" : subCategoryBox.getValue()));
    }

    private boolean isInvalidQuestCategory() {
        return isMissingCategoryId(safe(questCategoryBox == null ? "" : questCategoryBox.getValue()));
    }

    private boolean isInvalidQuestSubCategory() {
        return isMissingSubCategoryId(safe(questSubCategoryBox == null ? "" : questSubCategoryBox.getValue()));
    }

    private boolean isInvalidQuestDependencies() {
        String raw = safe(questDependenciesBox == null ? "" : questDependenciesBox.getValue());
        if (currentPack == null || raw.isBlank()) return false;
        String[] parts = raw.split(",");
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();
            if (trimmed.isBlank()) continue;
            if (!questIdCache.contains(trimmed)) return true;
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

    private boolean isMissingSubCategoryId(String raw) {
        if (currentPack == null) return false;
        String id = raw == null ? "" : raw.trim();
        if (id.isBlank()) return false;
        return !subCategoryIdCache.contains(id);
    }

    private void refreshPackIdCaches() {
        categoryIdCache.clear();
        subCategoryIdCache.clear();
        questIdCache.clear();
        categorySuggestionCache.clear();
        subCategorySuggestionCache.clear();
        questSuggestionCache.clear();
        subCategoryByCategorySuggestion.clear();
        advancementIdCache.clear();
        lootTableIdCache.clear();

        if (currentPack != null) {
            for (NamedEntry entry : listCategoryEntries(currentPack)) {
                if (entry != null && entry.id != null && !entry.id.isBlank()) categoryIdCache.add(entry.id);
            }
            for (NamedEntry entry : listSubCategoryEntries(currentPack)) {
                if (entry != null && entry.id != null && !entry.id.isBlank()) subCategoryIdCache.add(entry.id);
            }
            for (NamedEntry entry : listQuestEntries(currentPack)) {
                if (entry != null && entry.id != null && !entry.id.isBlank()) questIdCache.add(entry.id);
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
            if (!questSuggestionCache.contains(q.id)) questSuggestionCache.add(q.id);
        }
    }

    private void layoutDescriptionFormatterButtons(int startX, int y, int clipTop, int clipBottom) {
        boolean visible = editorType == EditorType.QUEST
                && questDescriptionBox != null
                && questDescriptionBox.visible
                && Config.enableDescriptionColors()
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
        if (descriptionUndoButton != null) {
            descriptionUndoButton.setX(x);
            descriptionUndoButton.setY(y);
            descriptionUndoButton.visible = visible;
            descriptionUndoButton.active = visible && questDescriptionBox.canUndo();
            x += descriptionUndoButton.getWidth() + FORMAT_BTN_GAP;
        }
        if (descriptionRedoButton != null) {
            descriptionRedoButton.setX(x);
            descriptionRedoButton.setY(y);
            descriptionRedoButton.visible = visible;
            descriptionRedoButton.active = visible && questDescriptionBox.canRedo();
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
                || editorType == EditorType.SUBCATEGORY;
        if (duplicateButton != null) {
            duplicateButton.visible = showQuestActions;
            duplicateButton.active = showQuestActions;
        }
        if (deleteQuestButton != null) {
            deleteQuestButton.visible = showQuestActions;
            deleteQuestButton.active = showQuestActions;
        }

        boolean showPackDelete = mode == Mode.PACK_MENU && currentPack != null;
        if (deletePackButton != null) {
            deletePackButton.visible = showPackDelete;
            deletePackButton.active = showPackDelete;
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
        pendingState = captureState();
        super.resize(minecraft, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
        if (button == 0 && clickIdSuggestion(mouseX, mouseY)) {
            return true;
        }
        Style style = getFooterStyleAt((int) mouseX, (int) mouseY);
        if (style != null && style.getClickEvent() != null
                && style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL) {
            this.handleComponentClicked(style);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickToolbarButtons(double mouseX, double mouseY) {
        if (createPackButton != null && createPackButton.visible && createPackButton.active && createPackButton.isMouseOver(mouseX, mouseY)) {
            createPackButton.onPress();
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
        if (deletePackButton != null && deletePackButton.visible && deletePackButton.active
                && deletePackButton.isMouseOver(mouseX, mouseY)) {
            deletePackButton.onPress();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        suppressIdSuggestions = false;
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
        suppressIdSuggestions = false;
        return super.charTyped(codePoint, modifiers);
    }

    private boolean isIconBox(EditBox box) {
        return box == questIconBox || box == catIconBox || box == subIconBox;
    }

    private EditBox focusedEditBox() {
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
            if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
                this.textField.setSelecting(Screen.hasShiftDown());
                this.seekCursorScreen(mouseX, mouseY);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
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
            StringBuilder segment = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                if (startsWithColorToken(text, i)) {
                    if (!segment.isEmpty()) {
                        if (guiGraphics != null) {
                            drawX = guiGraphics.drawString(this.font, segment.toString(), drawX, y, color, false);
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
                    color = formatColor(text.charAt(i + 1), this.textColor);
                    i += 1;
                    continue;
                }
                segment.append(text.charAt(i));
            }
            if (!segment.isEmpty()) {
                if (guiGraphics != null) {
                    drawX = guiGraphics.drawString(this.font, segment.toString(), drawX, y, color, false);
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
                case 'w', 'r', 'g', 'b', 'y', 'o', 'a', 'p', 'x' -> true;
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
                this.value = new StringBuilder(this.value).replace(selected.beginIndex, selected.endIndex, filtered).toString();
                this.cursor = selected.beginIndex + filtered.length();
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
            return new StringView(Math.min(this.selectCursor, this.cursor), Math.max(this.selectCursor, this.cursor));
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
                    || code == 'y' || code == 'o' || code == 'a' || code == 'p' || code == 'x';
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
        final String actionTooltip;
        final String rowTooltip;
        final EditorEntryKind kind;
        final int indent;

        EditorEntry(String id, String label, String subtitle, String icon) {
            this(id, label, subtitle, icon, null, "", "", EditorEntryKind.NORMAL, 0);
        }

        EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, "", EditorEntryKind.NORMAL, 0);
        }

        EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, rowTooltip, EditorEntryKind.NORMAL, 0);
        }

        EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip, EditorEntryKind kind, int indent) {
            this.id = id;
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon == null ? "" : icon;
            this.actionIcon = actionIcon;
            this.actionTooltip = actionTooltip == null ? "" : actionTooltip;
            this.rowTooltip = rowTooltip == null ? "" : rowTooltip;
            this.kind = kind == null ? EditorEntryKind.NORMAL : kind;
            this.indent = Math.max(0, indent);
        }

        static EditorEntry quest(String id, String label, String subtitle, String icon) {
            return new EditorEntry(id, label, subtitle, icon, null, "", "", EditorEntryKind.NORMAL, 18);
        }

        static EditorEntry categoryHeader(String id, String label, boolean collapsed) {
            return new EditorEntry(id, (collapsed ? "> " : "v ") + label, "", "", null, "", "", EditorEntryKind.CATEGORY_HEADER, 0);
        }

        static EditorEntry subCategoryHeader(String id, String label, boolean collapsed) {
            return new EditorEntry(id, (collapsed ? "> " : "v ") + label, "", "", null, "", "", EditorEntryKind.SUBCATEGORY_HEADER, 10);
        }
    }

    private enum EditorEntryKind {
        NORMAL,
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

    private static final class QuestPack {
        final String name;
        final String namespace;
        final Path root;
        final Path dataDir;
        final Path questsDir;
        final Path categoriesDir;
        final Path subCategoriesDir;

        QuestPack(String name, String namespace, Path root) {
            this.name = name;
            this.namespace = namespace == null ? "" : namespace;
            this.root = root;
            this.dataDir = root.resolve("data").resolve(this.namespace);
            this.questsDir = dataDir.resolve("quests");
            this.categoriesDir = questsDir.resolve("categories");
            this.subCategoriesDir = questsDir.resolve("sub-category");
        }

        void ensureDirs() throws IOException {
            Files.createDirectories(categoriesDir);
            Files.createDirectories(subCategoriesDir);
            Files.createDirectories(questsDir);
        }
    }

    private static final class PackMeta {
        String description = "";
        String iconPath = "";
    }

    private static final class CategoryData {
        Path path;
        String id = "";
        String name = "";
        String icon = "";
        String order = "";
        String dependency = "";
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
        String index = "";
        String id = "";
        String name = "";
        String icon = "";
        String description = "";
        String category = "";
        String subCategory = "";
        String dependencies = "";
        String optional = "";
        String repeatable = "";
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
        float editorScroll = 0f;
        float leftScroll = 0f;
        String statusMessage = "";
        int statusColor = 0xA0A0A0;
        long deletePackConfirmUntil = 0L;
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
        String catOrder = "";
        String catDependency = "";

        String subId = "";
        String subCategory = "";
        String subName = "";
        String subIcon = "";
        String subOrder = "";
        boolean subDefaultOpen = false;

        String questId = "";
        String questIndex = "";
        String questName = "";
        String questIcon = "";
        String questDescription = "";
        String questCategory = "";
        String questSubCategory = "";
        String questDependencies = "";
        boolean questOptional = false;
        boolean questRepeatable = false;
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

    private static final class ParsedEntry {
        final String type;
        final String id;
        final int count;

        ParsedEntry(String type, String id, int count) {
            this.type = type == null ? "" : type;
            this.id = id == null ? "" : id;
            this.count = count;
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
        private float scrollY = 0f;
        private String selectedId = "";
        private List<Component> pendingTooltip = List.of();
        private int pendingTooltipX;
        private int pendingTooltipY;

        EditorListWidget(int x, int y, int w, int h, Consumer<EditorEntry> onClick, Consumer<EditorEntry> onActionClick, Consumer<EditorEntry> onSecondaryClick) {
            super(x, y, w, h, Component.empty());
            this.onClick = onClick;
            this.onActionClick = onActionClick;
            this.onSecondaryClick = onSecondaryClick;
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

                if (top > getY() + height) break;
                if (top + rowH < getY()) {
                    yCursor += rowH + ROW_PAD;
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
                        gg.blit(ROW_TEX, getX(), top, 0, 0, width, ROW_H, width, ROW_H);
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
                        String sym = entry.label != null && entry.label.startsWith("v ") ? "-" : "+";
                        int symW = (int) (Minecraft.getInstance().font.width(sym) * textScale);
                        int symX = getX() + width - symW - 2;
                        int maxW = symX - textX - 2;
                        int maxWUnscaled = maxW > 0 ? (int) (maxW / textScale) : 0;
                        if (Minecraft.getInstance().font.width(name) > maxWUnscaled) {
                            name = Minecraft.getInstance().font.plainSubstrByWidth(name, Math.max(0, maxWUnscaled - Minecraft.getInstance().font.width("..."))) + "...";
                        }
                        if (entry.kind == EditorEntryKind.CATEGORY_HEADER) {
                            drawScaledComponent(gg, Component.literal(name).withStyle(style -> style.withBold(true)), textScale, textX, textY, 0xFFFFFF);
                            drawScaledString(gg, sym, textScale, symX, textY, 0xFFFFFF);
                        } else {
                            drawScaledString(gg, name, textScale, textX, textY, 0xD0D0D0);
                            drawScaledString(gg, sym, textScale, symX, textY, 0xD0D0D0);
                        }
                        yCursor += rowH + ROW_PAD;
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

                yCursor += rowH + ROW_PAD;
            }

            gg.disableScissor();

            int contentHeight = 0;
            for (EditorEntry entry : entries) {
                boolean headerEntry = entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER;
                contentHeight += (headerEntry ? EDITOR_SUBHEADER_H : ROW_H) + ROW_PAD;
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
                if (localY < yCursor || localY >= yCursor + rowH + ROW_PAD) {
                    yCursor += rowH + ROW_PAD;
                    continue;
                }
                int top = getY() - (int) scrollY + yCursor;
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
                contentHeight += (headerEntry ? EDITOR_SUBHEADER_H : ROW_H) + ROW_PAD;
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
        private final Mode targetMode;

        EditorTabButton(String tooltip, String iconId, Mode targetMode) {
            super(0, 0, TAB_W, TAB_H, Component.empty());
            this.tooltip = tooltip == null ? "" : tooltip;
            this.iconId = iconId == null ? "" : iconId;
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
            gg.blit(selected ? TAB_SELECTED_TEX : TAB_TEX, getX(), getY(), 0, 0, this.width, this.height, TAB_W, TAB_H);
            ItemStack icon = iconStackFromId(iconId);
            if (!icon.isEmpty()) {
                gg.renderItem(icon, getX() + (this.width - 16) / 2, getY() + 5);
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
        private final Consumer<String> onPress;

        TextInsertButton(int x, int y, int width, String label, String insertText, int fillColor, Consumer<String> onPress) {
            super(x, y, width, FORMAT_BAR_H, Component.literal(label));
            this.insertText = insertText == null ? "" : insertText;
            this.fillColor = fillColor;
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
                int textX = getX() + (this.width - font.width(getMessage())) / 2;
                int textY = getY() + (this.height - font.lineHeight) / 2;
                gg.drawString(font, getMessage(), textX, textY, textColor, false);
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
        private final Runnable onPress;

        public IconButton(int x, int y, int size, ResourceLocation texture, Runnable onPress) {
            super(x, y, size, size, Component.empty());
            this.texture = texture;
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

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            float alpha = this.active ? 1.0f : 0.5f;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            gg.blit(texture, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private final class EntryRemoveButton extends AbstractButton {
        private final boolean reward;

        EntryRemoveButton(boolean reward) {
            super(0, 0, ENTRY_REMOVE_BTN_W, ENTRY_ROW_H, Component.literal("X"));
            this.reward = reward;
        }

        @Override
        public void onPress() {
            QuestEditorScreen.this.removeEntryRow(reward, this);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            int bg = this.active ? (this.isMouseOver(mouseX, mouseY) ? 0xAA5A1A1A : 0xAA3A1212) : 0x553A1212;
            int fg = this.active ? 0xFFFF5555 : 0xFF804040;
            gg.fill(getX(), getY(), getX() + this.width, getY() + this.height, bg);
            gg.drawString(Minecraft.getInstance().font, "X", getX() + 2, getY() + 2, fg, false);
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
            ResourceLocation tex = on ? TOGGLE_TEX_ON : TOGGLE_TEX_OFF;
            gg.blit(tex, getX(), getY(), 0, 0, TOGGLE_SIZE, TOGGLE_SIZE, TOGGLE_SIZE, TOGGLE_SIZE);
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
