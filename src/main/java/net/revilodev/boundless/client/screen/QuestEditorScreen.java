
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
import net.minecraft.client.gui.components.EditBox;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

    private static final int TOGGLE_SIZE = 20;
    private static final int SMALL_BTN_SIZE = 20;
    private static final int SMALL_BTN_GAP = 4;

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

    private static final int PACK_FORMAT = 48;
    private static final int DEFAULT_INPUT_TEXT_COLOR = 0xE0E0E0;
    private static final int INVALID_INPUT_TEXT_COLOR = 0xFF4040;
    private static final String INVALID_ID_TOOLTIP = "Invalid ID";
    private static final int ID_SUGGESTION_MAX = 6;
    private static final int ID_SUGGESTION_ROW_H = 12;
    private static final int ID_SUGGESTION_BG_COLOR = 0xFF000000;
    private static final int ID_SUGGESTION_TEXT_COLOR = 0xFFFFFF00;

    private static final String DEFAULT_COMPLETION_JSON =
            "{ \"collect\": \"minecraft:id\", \"count\": 1 },\n" +
                    "{ \"submit\": \"minecraft:id\", \"count\": 1 },\n" +
                    "{ \"kill\": \"minecraft:id\", \"count\": 1 },\n" +
                    "{ \"achieve\": \"minecraft:id/id\" },\n" +
                    "{ \"effect\": \"minecraft:id\" }";

    private static final String DEFAULT_REWARD_JSON =
            "\"items\": [{ \"item\": \"minecraft:id\", \"count\": 1 }],\n" +
                    "\"commands\": [{ \"command\": \" \", \"icon\": \"minecraft:id\", \"title\": \"\" }],\n" +
                    "\"exp\": \"points\", \"count\": 1";

    private static final String ENTRY_CREATE_PACK = "__create_pack__";
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
    private ScaledMultiLineEditBox questCompletionBox;
    private ScaledMultiLineEditBox questRewardBox;

    private Path editingPath;
    private String loadedQuestType = "";
    private final List<String> itemIdCache = new ArrayList<>();
    private final Set<String> categoryIdCache = new HashSet<>();
    private final Set<String> subCategoryIdCache = new HashSet<>();
    private final Set<String> questIdCache = new HashSet<>();
    private final List<String> categorySuggestionCache = new ArrayList<>();
    private final List<String> subCategorySuggestionCache = new ArrayList<>();
    private final List<String> questSuggestionCache = new ArrayList<>();
    private final Map<String, List<String>> subCategoryByCategorySuggestion = new HashMap<>();
    private final List<String> activeIdSuggestions = new ArrayList<>();
    private EditBox idSuggestionField;
    private boolean suppressIdSanitizer = false;

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

        leftList = new EditorListWidget(pxLeft, py, pw, listH, this::handleLeftClick);
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
        deletePackButton = new IconButton(pxLeft + 2 + 24 + SMALL_BTN_GAP, barY, SMALL_BTN_SIZE, DELETE_TEX, this::confirmDeletePack);
        addRenderableWidget(deletePackButton);

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
        questDescriptionBox = createMultiLineBox("Quest description", BOX_H_TALL);
        questCategoryBox = createBox("Quest category", BOX_H);
        questSubCategoryBox = createBox("Quest sub-category", BOX_H);
        questDependenciesBox = createBox("Dependencies (comma separated)", BOX_H);
        questOptionalToggle = createToggle(false);
        questCompletionBox = createMultiLineBox("Completion JSON", BOX_H_TALL);
        questRewardBox = createMultiLineBox("Reward JSON", BOX_H_TALL);

        attachIdSanitizer(packNamespaceBox, false);
        attachIdSanitizer(catIdBox, false);
        attachIdSanitizer(catDependencyBox, false);
        attachIdSanitizer(subIdBox, false);
        attachIdSanitizer(subCategoryBox, false);
        attachIdSanitizer(questIdBox, false);
        attachIdSanitizer(questCategoryBox, false);
        attachIdSanitizer(questSubCategoryBox, false);
        attachIdSanitizer(questDependenciesBox, true);

        // no extra widgets
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
        ScaledMultiLineEditBox box = new ScaledMultiLineEditBox(font, 0, 0, pw - 4, height, Component.literal(hint), Component.empty(), INPUT_TEXT_SCALE);
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
        mode = next;
        statusMessage = "";
        editorScroll = 0f;
        deletePackConfirmUntil = 0L;
        disarmDeleteConfirm();
        refreshLeftList();
        clearEditor();
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
        out.add(new EditorEntry(ENTRY_CREATE_PACK, "Create New Pack", "resourcepacks/boundless", ""));

        for (QuestPack pack : listPacks()) {
            String subtitle = pack.namespace.isBlank() ? "No namespace" : pack.namespace;
            out.add(new EditorEntry(pack.name, pack.name, subtitle, ""));
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
        out.add(new EditorEntry(ENTRY_NEW, "New Category", "", ""));
        if (currentPack == null) return out;
        for (NamedEntry entry : listCategoryEntries(currentPack)) {
            out.add(new EditorEntry(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    private List<EditorEntry> buildSubCategoryEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_NEW, "New Sub-category", "", ""));
        if (currentPack == null) return out;
        for (NamedEntry entry : listSubCategoryEntries(currentPack)) {
            out.add(new EditorEntry(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    private List<EditorEntry> buildQuestEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_NEW, "New Quest", "", ""));
        if (currentPack == null) return out;
        for (NamedEntry entry : listQuestEntries(currentPack)) {
            out.add(new EditorEntry(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    private void handleLeftClick(EditorEntry entry) {
        if (entry == null) return;
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
    private void handlePackEntry(EditorEntry entry) {
        if (ENTRY_CREATE_PACK.equals(entry.id)) {
            setMode(Mode.PACK_CREATE);
            showPackCreate();
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
        setMode(Mode.PACK_MENU);
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

        setActiveFields(List.of(
                field("Pack name", packNameBox),
                field("Namespace", packNamespaceBox)
        ));
        saveButton.setMessage(Component.literal("Create"));
        saveButton.visible = true;
        saveButton.active = true;
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
        questCompletionBox.setValue(safe(data.completionJson));
        questRewardBox.setValue(safe(data.rewardJson));
        loadedQuestType = safe(data.type);

        if (sourcePath == null) {
            if (questCompletionBox.getValue().isBlank()) {
                questCompletionBox.setValue(DEFAULT_COMPLETION_JSON);
            }
            if (questRewardBox.getValue().isBlank()) {
                questRewardBox.setValue(DEFAULT_REWARD_JSON);
            }
        }
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
                field("Optional (true/false)", questOptionalToggle),
                field("Completion JSON", questCompletionBox),
                field("Reward JSON", questRewardBox)
        ));
        saveButton.setMessage(Component.literal("Save"));
        saveButton.visible = true;
        saveButton.active = true;
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
            writePackMeta(root, name);
            QuestPack pack = new QuestPack(name, namespace, root);
            pack.ensureDirs();
            currentPack = pack;
            applyChanges();
            setMode(Mode.PACK_MENU);
        } catch (IOException e) {
            setError("Failed to create pack");
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
            JsonElement completion = parseCompletionJson(completionRaw);
            if (completion == null) {
                setError("Invalid JSON");
                return null;
            }
            obj.add("completion", completion);
        }

        String rewardRaw = safe(questRewardBox.getValue()).trim();
        if (!rewardRaw.isBlank()) {
            JsonElement reward = parseRewardJson(rewardRaw);
            if (reward == null || !reward.isJsonObject()) {
                setError("Invalid JSON");
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
            boolean zipped = applyChanges();
            if (zipped) {
                statusMessage = "Saved and applied";
                statusColor = 0xA0FFA0;
            } else {
                statusMessage = "Saved, but failed to update zip";
                statusColor = 0xFF8080;
            }
            refreshLeftList();
        } catch (IOException e) {
            setError("Save failed");
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
            applyChanges();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            if (deleted) {
                statusMessage = "Deleted";
                statusColor = 0xA0FFA0;
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
            applyChanges();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            if (deleted) {
                statusMessage = "Deleted";
                statusColor = 0xA0FFA0;
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
            applyChanges();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            if (deleted) {
                statusMessage = "Deleted";
                statusColor = 0xA0FFA0;
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
            Files.deleteIfExists(packZipPath(pack.name));
            currentPack = null;
            selectedEntryId = "";
            setMode(Mode.PACK_LIST);
            statusMessage = "Pack deleted";
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private JsonElement parseJson(String raw) {
        try {
            return JsonParser.parseString(raw);
        } catch (Exception e) {
            setError("Invalid JSON");
            return null;
        }
    }

    private JsonElement parseJsonSilent(String raw) {
        try {
            return JsonParser.parseString(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonElement parseCompletionJson(String raw) {
        JsonElement direct = parseJsonSilent(raw);
        if (direct != null) return direct;
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isBlank()) return null;
        JsonElement asArray = parseJsonSilent("[" + trimmed + "]");
        if (asArray != null && asArray.isJsonArray()) {
            JsonObject wrapper = new JsonObject();
            wrapper.add("complete", asArray);
            return wrapper;
        }
        return null;
    }

    private JsonElement parseRewardJson(String raw) {
        JsonElement direct = parseJsonSilent(raw);
        if (direct != null) return direct;
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isBlank()) return null;
        return parseJsonSilent("{" + trimmed + "}");
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

    private boolean ensurePackSelectedInOptions(Minecraft mc, String packId) {
        if (mc == null || mc.options == null || packId == null || packId.isBlank()) return false;
        Object options = mc.options;
        boolean changed = false;
        List<String> selected = getOptionsList(options, "resourcePacks");
        if (selected != null && !selected.contains(packId)) {
            changed |= addOptionEntry(options, "resourcePacks", selected, packId);
        }
        return changed;
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
        if (!selectedIds.contains(packId)) {
            selectedIds.add(packId);
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
        activeFields.clear();
        activeFields.addAll(fields);
        for (FormField f : activeFields) {
            if (!allFields.contains(f)) allFields.add(f);
            f.widget.visible = true;
            f.widget.active = true;
        }
        editorScroll = 0f;
    }

    // removed dynamic entry builder

    private FormField field(String label, AbstractWidget widget) {
        FormField field = new FormField(label, widget);
        if (!allFields.contains(field)) allFields.add(field);
        return field;
    }

    private void setError(String msg) {
        statusMessage = msg;
        statusColor = 0xFF8080;
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

    private Path packZipPath(String packName) {
        return resourcePacksRoot().resolve(packName + ".zip");
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

    private void writePackMeta(Path root, String name) throws IOException {
        JsonObject pack = new JsonObject();
        JsonObject body = new JsonObject();
        body.addProperty("pack_format", PACK_FORMAT);
        body.addProperty("description", "Boundless Quest Pack: " + name);
        pack.add("pack", body);

        Path meta = root.resolve("pack.mcmeta");
        try (BufferedWriter writer = Files.newBufferedWriter(meta, StandardCharsets.UTF_8)) {
            gson.toJson(pack, writer);
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

        state.packName = safe(packNameBox == null ? "" : packNameBox.getValue());
        state.packNamespace = safe(packNamespaceBox == null ? "" : packNamespaceBox.getValue());

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
        updateDeleteButtonTexture();
        refreshLeftList();
        leftList.setScrollY(state.leftScroll);

        switch (state.editorType) {
            case PACK_CREATE -> {
                showPackCreate();
                packNameBox.setValue(state.packName);
                packNamespaceBox.setValue(state.packNamespace);
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
                data.type = state.loadedQuestType;
                data.completionJson = state.questCompletion;
                data.rewardJson = state.questReward;
                showQuestEditor(data, state.editingPath);
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
        return listEntries(pack.questsDir);
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
                entries.add(new NamedEntry(id, name, icon, path));
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
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, leftX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
        gg.blit(PANEL_TEX, rightX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        renderEditorFields(gg, mouseX, mouseY);

        boolean leftVisible = leftList != null && leftList.visible;
        boolean backVisible = backButton != null && backButton.visible;
        boolean saveVisible = saveButton != null && saveButton.visible;
        boolean duplicateVisible = duplicateButton != null && duplicateButton.visible;
        boolean deleteQuestVisible = deleteQuestButton != null && deleteQuestButton.visible;
        boolean deletePackVisible = deletePackButton != null && deletePackButton.visible;
        if (leftList != null) leftList.visible = false;
        if (backButton != null) backButton.visible = false;
        if (saveButton != null) saveButton.visible = false;
        if (duplicateButton != null) duplicateButton.visible = false;
        if (deleteQuestButton != null) deleteQuestButton.visible = false;
        if (deletePackButton != null) deletePackButton.visible = false;

        gg.enableScissor(pxRight, py, pxRight + pw, py + ph);
        super.render(gg, mouseX, mouseY, partialTick);
        gg.disableScissor();

        if (leftList != null) leftList.visible = leftVisible;
        if (backButton != null) backButton.visible = backVisible;
        if (saveButton != null) saveButton.visible = saveVisible;
        if (duplicateButton != null) duplicateButton.visible = duplicateVisible;
        if (deleteQuestButton != null) deleteQuestButton.visible = deleteQuestVisible;
        if (deletePackButton != null) deletePackButton.visible = deletePackVisible;

        if (leftList != null && leftList.visible) leftList.render(gg, mouseX, mouseY, partialTick);
        if (backButton != null && backButton.visible) backButton.render(gg, mouseX, mouseY, partialTick);
        if (saveButton != null && saveButton.visible) saveButton.render(gg, mouseX, mouseY, partialTick);
        if (duplicateButton != null && duplicateButton.visible) duplicateButton.render(gg, mouseX, mouseY, partialTick);
        if (deleteQuestButton != null && deleteQuestButton.visible) deleteQuestButton.render(gg, mouseX, mouseY, partialTick);
        if (deletePackButton != null && deletePackButton.visible) deletePackButton.render(gg, mouseX, mouseY, partialTick);
        renderIconOverlays(gg);
        renderIdSuggestions(gg, mouseX, mouseY);

        if (!statusMessage.isBlank()) {
            int msgW = font.width(statusMessage);
            int msgX = (this.width - msgW) / 2;
            gg.drawString(font, statusMessage, msgX, topY + PANEL_H + 8, statusColor, false);
        }
        renderFooter(gg, mouseX, mouseY);
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
        if (idSuggestionField == null || activeIdSuggestions.isEmpty()) return;
        int x = idSuggestionField.getX();
        int y = idSuggestionField.getY() + idSuggestionField.getHeight() + 1;
        int w = idSuggestionField.getWidth();
        for (int i = 0; i < activeIdSuggestions.size(); i++) {
            int top = y + (i * ID_SUGGESTION_ROW_H);
            int bottom = top + ID_SUGGESTION_ROW_H;
            gg.fill(x, top, x + w, bottom, ID_SUGGESTION_BG_COLOR);
            gg.drawString(font, activeIdSuggestions.get(i), x + 4, top + 2, ID_SUGGESTION_TEXT_COLOR, false);
        }
    }

    private void updateIdSuggestions() {
        EditBox focused = focusedIdSuggestionField();
        if (focused == null) {
            idSuggestionField = null;
            activeIdSuggestions.clear();
            return;
        }
        idSuggestionField = focused;
        activeIdSuggestions.clear();

        List<String> all = idSuggestionValuesForField(focused);
        if (all.isEmpty()) return;

        String prefix = idSuggestionPrefix(focused).toLowerCase(Locale.ROOT);
        for (String id : all) {
            if (id == null || id.isBlank()) continue;
            if (!prefix.isBlank() && !id.toLowerCase(Locale.ROOT).startsWith(prefix)) continue;
            activeIdSuggestions.add(id);
            if (activeIdSuggestions.size() >= ID_SUGGESTION_MAX) break;
        }
    }

    private EditBox focusedIdSuggestionField() {
        if (catDependencyBox != null && catDependencyBox.visible && catDependencyBox.isFocused()) return catDependencyBox;
        if (subCategoryBox != null && subCategoryBox.visible && subCategoryBox.isFocused()) return subCategoryBox;
        if (questCategoryBox != null && questCategoryBox.visible && questCategoryBox.isFocused()) return questCategoryBox;
        if (questSubCategoryBox != null && questSubCategoryBox.visible && questSubCategoryBox.isFocused()) return questSubCategoryBox;
        if (questDependenciesBox != null && questDependenciesBox.visible && questDependenciesBox.isFocused()) return questDependenciesBox;
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
        if (field == catDependencyBox || field == subCategoryBox || field == questCategoryBox) {
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

    private boolean clickIdSuggestion(double mouseX, double mouseY) {
        if (idSuggestionField == null || activeIdSuggestions.isEmpty()) return false;
        int x = idSuggestionField.getX();
        int y = idSuggestionField.getY() + idSuggestionField.getHeight() + 1;
        int w = idSuggestionField.getWidth();
        if (mouseX < x || mouseX > x + w || mouseY < y) return false;
        int idx = (int) ((mouseY - y) / ID_SUGGESTION_ROW_H);
        if (idx < 0 || idx >= activeIdSuggestions.size()) return false;
        applyIdSuggestion(idSuggestionField, activeIdSuggestions.get(idx));
        return true;
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

    private void renderEditorFields(GuiGraphics gg, int mouseX, int mouseY) {
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

            field.widget.setX(boxX);
            field.widget.setY(boxY);
            if (field.widget instanceof EditBox eb) {
                eb.setWidth(pw - 4);
            } else if (field.widget instanceof ScaledMultiLineEditBox mb) {
                mb.setWidth(pw - 4);
            } else if (field.widget instanceof ToggleButton tb) {
                tb.setSize(TOGGLE_SIZE, TOGGLE_SIZE);
            }

            boolean inside = boxY + field.widget.getHeight() > clipTop && boxY < clipBottom;
            field.widget.visible = inside;
            field.widget.active = inside;

            if (labelY + font.lineHeight > clipTop && labelY < clipBottom) {
                gg.drawString(font, field.displayLabel, pxRight + 2, labelY, 0xFFFFFF, false);
                String tooltip = tooltipForField(field);
                if (!tooltip.isBlank()) {
                    int labelW = font.width(field.displayLabel);
                    boolean hoverLabel = mouseX >= pxRight + 2 && mouseX <= pxRight + 2 + labelW
                            && mouseY >= labelY && mouseY <= labelY + font.lineHeight;
                    boolean hoverWidget = mouseX >= boxX && mouseX <= boxX + (pw - 4)
                            && mouseY >= boxY && mouseY <= boxY + field.widget.getHeight();
                    if (hoverLabel || hoverWidget) {
                        gg.renderTooltip(font, Component.literal(tooltip), mouseX, mouseY);
                    }
                }
            }
            yCursor += font.lineHeight + FIELD_LABEL_GAP + field.widget.getHeight() + FIELD_ROW_GAP;
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
        if (mouseX >= pxRight && mouseX <= pxRight + pw && mouseY >= py && mouseY <= py + ph) {
            int contentHeight = contentHeight();
            if (contentHeight > ph) {
                editorScroll = Math.max(0f, Math.min(editorScroll - (float) scrollY * 12f, contentHeight - ph));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        if (field.widget instanceof ScaledMultiLineEditBox mb) {
            return computeMultilineHeight(mb, BOX_H_TALL);
        }
        return field.widget.getHeight();
    }

    private void updateDynamicFieldSizes() {
        for (FormField field : activeFields) {
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
        setJsonFieldColor(questCompletionBox, isInvalidCompletionJson());
        setJsonFieldColor(questRewardBox, isInvalidRewardJson());
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

    private boolean isInvalidCompletionJson() {
        String raw = safe(questCompletionBox == null ? "" : questCompletionBox.getValue()).trim();
        if (raw.isBlank()) return false;
        return parseCompletionJson(raw) == null;
    }

    private boolean isInvalidRewardJson() {
        String raw = safe(questRewardBox == null ? "" : questRewardBox.getValue()).trim();
        if (raw.isBlank()) return false;
        JsonElement parsed = parseRewardJson(raw);
        return parsed == null || !parsed.isJsonObject();
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
    }

    private void goBack() {
        switch (mode) {
            case PACK_LIST -> Minecraft.getInstance().setScreen(parent);
            case PACK_CREATE, PACK_MENU -> setMode(Mode.PACK_LIST);
            case CATEGORY_LIST, SUBCATEGORY_LIST, QUEST_LIST -> setMode(Mode.PACK_MENU);
        }
    }

    @Override
    public void onClose() {
        clearPendingInitState();
        Minecraft.getInstance().setScreen(parent);
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
        if (button == 0 && deleteConfirmArmed
                && (deleteQuestButton == null || !deleteQuestButton.visible || !deleteQuestButton.isMouseOver(mouseX, mouseY))) {
            disarmDeleteConfirm();
        }
        if (button == 0 && clickToolbarButtons(mouseX, mouseY)) {
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
        if (keyCode == GLFW.GLFW_KEY_TAB) {
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

    private String computeIconSuggestion(String raw) {
        if (raw == null) return "";
        String input = raw.trim().toLowerCase(Locale.ROOT);
        if (input.isBlank()) return "";
        ensureItemIdCache();

        String suggestion = findSuggestion(itemIdCache, input);
        if ((suggestion == null || suggestion.isBlank()) && !input.contains(":")) {
            suggestion = findSuggestion(itemIdCache, "minecraft:" + input);
        }
        return suggestion == null ? "" : suggestion;
    }

    private String findSuggestion(List<String> cache, String prefix) {
        if (cache == null || cache.isEmpty() || prefix == null || prefix.isBlank()) return "";
        for (String id : cache) {
            if (id.startsWith(prefix)) return id;
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
        private long focusedTime = Util.getMillis();
        private int textColor = TEXT_COLOR;

        ScaledMultiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder, Component message, float textScale) {
            super(x, y, width, height, message);
            this.font = font;
            this.placeholder = placeholder;
            this.textScale = textScale <= 0f ? 1f : textScale;
            this.lineHeight = 9.0 * this.textScale;
            int fieldWidth = Math.max(20, Math.round((width - this.totalInnerPadding()) / this.textScale));
            this.textField = new ScaledTextField(font, fieldWidth);
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

        public void setTextColor(int color) {
            this.textColor = color;
        }

        public String getValue() {
            return this.textField.value();
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
            double lineScreenY = this.getY() + this.innerPadding();
            double lastLineScreenY = lineScreenY;

            for (ScaledTextField.StringView line : this.textField.iterateLines()) {
                boolean visible = this.withinContentAreaTopBottom((int) lineScreenY, (int) (lineScreenY + this.lineHeight));
                if (showCursor && cursorInText && cursor >= line.beginIndex() && cursor <= line.endIndex()) {
                    if (visible) {
                        int drawY = Math.round((float) (lineScreenY * invScale));
                        drawX = guiGraphics.drawString(
                                this.font, text.substring(line.beginIndex(), cursor), baseX, drawY, this.textColor, false
                            )
                            - 1;
                        guiGraphics.fill(drawX, drawY - 1, drawX + 1, drawY + 1 + this.font.lineHeight, CURSOR_INSERT_COLOR);
                        guiGraphics.drawString(this.font, text.substring(cursor, line.endIndex()), drawX, drawY, this.textColor, false);
                    }
                } else {
                    if (visible) {
                        int drawY = Math.round((float) (lineScreenY * invScale));
                        drawX = guiGraphics.drawString(
                                this.font,
                                text.substring(line.beginIndex(), line.endIndex()),
                                baseX,
                                drawY,
                                this.textColor,
                                false
                            )
                            - 1;
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
            if (this.textField.hasCharacterLimit()) {
                int limit = this.textField.characterLimit();
                Component component = Component.translatable("gui.multiLineEditBox.character_limit", this.textField.value().length(), limit);
                guiGraphics.drawString(this.font, component, this.getX() + this.width - this.font.width(component), this.getY() + this.height + 4, 10526880);
            }
        }

        @Override
        public int getInnerHeight() {
            return (int) Math.ceil(this.lineHeight * this.textField.getLineCount());
        }

        @Override
        protected boolean scrollbarVisible() {
            return (double) this.textField.getLineCount() > this.getDisplayableLineCount();
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
            double scroll = this.scrollAmount();
            ScaledTextField.StringView line = this.textField.getLineView((int) (scroll / this.lineHeight));
            if (this.textField.cursor() <= line.beginIndex()) {
                scroll = (double) this.textField.getLineAtCursor() * this.lineHeight;
            } else {
                ScaledTextField.StringView lastVisible = this.textField.getLineView((int) ((scroll + (double) this.height) / this.lineHeight) - 1);
                if (this.textField.cursor() > lastVisible.endIndex()) {
                    scroll = (double) this.textField.getLineAtCursor() * this.lineHeight - this.height + this.lineHeight + this.totalInnerPadding();
                }
            }

            this.setScrollAmount(scroll);
        }

        private double getDisplayableLineCount() {
            return (double) (this.height - this.totalInnerPadding()) / this.lineHeight;
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
        private String value = "";
        private int cursor;
        private int selectCursor;
        private boolean selecting;
        private int characterLimit = Integer.MAX_VALUE;
        private Consumer<String> valueListener = ignored -> {
        };
        private Runnable cursorListener = () -> {
        };

        ScaledTextField(Font font, int width) {
            this.font = font;
            this.width = width;
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
            this.onValueChange();
        }

        public String value() {
            return this.value;
        }

        public void insertText(String text) {
            if (!text.isEmpty() || this.hasSelection()) {
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
                this.selectCursor = Mth.clamp(this.cursor + length, 0, this.value.length());
            }
            this.insertText("");
        }

        public int cursor() {
            return this.cursor;
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

        private void onValueChange() {
            this.reflowDisplayLines();
            this.valueListener.accept(this.value);
            this.cursorListener.run();
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
    }

    private static final class EditorEntry {
        final String id;
        final String label;
        final String subtitle;
        final String icon;

        EditorEntry(String id, String label, String subtitle, String icon) {
            this.id = id;
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon == null ? "" : icon;
        }
    }

    private static final class NamedEntry {
        final String id;
        final String name;
        final Path path;
        final String icon;

        NamedEntry(String id, String name, String icon, Path path) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.icon = icon == null ? "" : icon;
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

        String packName = "";
        String packNamespace = "";

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
        private float scrollY = 0f;
        private String selectedId = "";

        EditorListWidget(int x, int y, int w, int h, Consumer<EditorEntry> onClick) {
            super(x, y, w, h, Component.empty());
            this.onClick = onClick;
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

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            gg.enableScissor(getX(), getY(), getX() + width, getY() + height);

            int yCursor = getY() - (int) scrollY;
            for (EditorEntry entry : entries) {
                int top = yCursor;
                int rowH = ROW_H;

                if (top > getY() + height) break;
                if (top + rowH < getY()) {
                    yCursor += ROW_H + ROW_PAD;
                    continue;
                }

                gg.blit(ROW_TEX, getX(), top, 0, 0, width, ROW_H, width, ROW_H);
                if (!selectedId.isBlank() && selectedId.equals(entry.id)) {
                    gg.fill(getX() + 1, top + 1, getX() + width - 1, top + ROW_H - 1, 0x20FFFFFF);
                }

                int textX = getX() + 6;
                ItemStack iconStack = iconStackFromId(entry.icon);
                if (!iconStack.isEmpty()) {
                    gg.renderItem(iconStack, getX() + 6, top + 5);
                    textX = getX() + 25;
                }
                gg.drawString(Minecraft.getInstance().font, entry.label, textX, top + 7, 0xFFFFFF, false);

                if (entry.subtitle != null && !entry.subtitle.isBlank()) {
                    drawScaledString(gg, entry.subtitle, 0.65f, textX, top + 17, 0xB0B0B0);
                }

                yCursor += ROW_H + ROW_PAD;
            }

            gg.disableScissor();

            int contentHeight = entries.size() * (ROW_H + ROW_PAD);
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
            if (!visible || !active || button != 0) return false;
            if (mouseX < getX() || mouseX > getX() + width || mouseY < getY() || mouseY > getY() + height)
                return false;

            int localY = (int) (mouseY - getY() + scrollY);
            int index = localY / (ROW_H + ROW_PAD);
            if (index >= 0 && index < entries.size()) {
                EditorEntry entry = entries.get(index);
                if (onClick != null) onClick.accept(entry);
                return true;
            }
            return false;
        }

        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!visible || !active) return false;
            int contentHeight = entries.size() * (ROW_H + ROW_PAD);
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
    }

    private static final class ActionButton extends AbstractButton {
        private final Runnable onPress;

        public ActionButton(int x, int y, int w, int h, Component text, Runnable onPress) {
            super(x, y, w, h, text);
            this.onPress = onPress;
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
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
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
        CATEGORY,
        SUBCATEGORY,
        QUEST
    }
}
