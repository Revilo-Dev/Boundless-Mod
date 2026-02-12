
package net.revilodev.boundless.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.quest.QuestData;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

    private static final int TOGGLE_SIZE = 20;

    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;
    private static final int ROW_H = 27;
    private static final int ROW_PAD = 1;
    private static final int BOTTOM_BAR_H = 24;
    private static final int FIELD_LABEL_GAP = 2;
    private static final int FIELD_ROW_GAP = 6;
    private static final int BOX_H = 20;
    private static final int BOX_H_TALL = 28;

    private static final int PACK_FORMAT = 48;

    private static final String DEFAULT_COMPLETION_JSON =
            "{ \"collect\": \"minecraft:crafting_table\", \"count\": 1 },\n" +
                    "{ \"submit\": \"minecraft:diamond\", \"count\": 5 },\n" +
                    "{ \"kill\": \"minecraft:zombie\", \"count\": 3 },\n" +
                    "{ \"achieve\": \"minecraft:story/mine_stone\" },\n" +
                    "{ \"effect\": \"minecraft:haste\" }";

    private static final String DEFAULT_REWARD_JSON =
            "\"items\": [{ \"item\": \"minecraft:diamond\", \"count\": 1 }],\n" +
                    "\"commands\": [{ \"command\": \"give @s minecraft:apple\", \"icon\": \"namespace:item\", \"title\": \"\" }],\n" +
                    "\"functions\": [{ \"function\": \"function_example:test\", \"icon\": \"namespace:item\", \"title\": \"\" }],\n" +
                    "\"exp\": \"points\", \"//EXP\": \"Either points or levels\", \"count\": 50";

    private static final String ENTRY_CREATE_PACK = "__create_pack__";
    private static final String ENTRY_NEW = "__new__";
    private static final String ENTRY_CATEGORIES = "__categories__";
    private static final String ENTRY_SUBCATEGORIES = "__subcategories__";
    private static final String ENTRY_QUESTS = "__quests__";

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

    private float editorScroll = 0f;
    private String statusMessage = "";
    private int statusColor = 0xA0A0A0;

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
    private MultiLineEditBox questDescriptionBox;
    private EditBox questCategoryBox;
    private EditBox questSubCategoryBox;
    private EditBox questDependenciesBox;
    private ToggleButton questOptionalToggle;
    private MultiLineEditBox questCompletionBox;
    private MultiLineEditBox questRewardBox;

    private Path editingPath;
    private String loadedQuestType = "";
    private final List<String> itemIdCache = new ArrayList<>();

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
        saveButton = new ActionButton(pxRight + pw - 68, barY, 68, 20,
                Component.literal("Save"), this::saveCurrent);
        addRenderableWidget(saveButton);

        backButton = new BackButton(pxLeft + 2, barY, this::goBack);
        addRenderableWidget(backButton);

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
    }

    private EditBox createBox(String hint, int height) {
        EditBox box = new EditBox(font, 0, 0, pw - 4, height, Component.literal(hint));
        box.setMaxLength(1024);
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    private MultiLineEditBox createMultiLineBox(String hint, int height) {
        MultiLineEditBox box = new MultiLineEditBox(font, 0, 0, pw - 4, height, Component.literal(hint), Component.empty());
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
    }

    private void showQuestEditor(QuestEntryData data, Path sourcePath) {
        editorType = EditorType.QUEST;
        editingPath = sourcePath;

        questIdBox.setValue(safe(data.id));
        IndexName indexName = splitIndexName(safe(data.name));
        questIndexBox.setValue(safe(indexName.index));
        questNameBox.setValue(safe(indexName.name));
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

        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        addOptional(obj, "name", catNameBox.getValue());
        addOptional(obj, "icon", catIconBox.getValue());
        addOptionalInt(obj, "order", catOrderBox.getValue());
        addOptional(obj, "dependency", catDependencyBox.getValue());
        Path target = currentPack.categoriesDir.resolve(id + ".json");
        saveJson(obj, target, editingPath);
    }

    private void saveSubCategory() {
        String id = safe(subIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Sub-category id required");
            return;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        addOptional(obj, "category", subCategoryBox.getValue());
        addOptional(obj, "name", subNameBox.getValue());
        addOptional(obj, "icon", subIconBox.getValue());
        addOptionalInt(obj, "order", subOrderBox.getValue());
        obj.addProperty("default_open", subDefaultOpenToggle.isOn());

        Path target = currentPack.subCategoriesDir.resolve(id + ".json");
        saveJson(obj, target, editingPath);
    }

    private void saveQuest() {
        String id = safe(questIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Quest id required");
            return;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        String nameRaw = safe(questNameBox.getValue()).trim();
        String indexRaw = safe(questIndexBox.getValue()).trim();
        String fullName = nameRaw;
        if (!indexRaw.isBlank() && !nameRaw.isBlank()) {
            fullName = indexRaw + "-" + nameRaw;
        } else if (!indexRaw.isBlank()) {
            fullName = indexRaw;
        }
        addOptional(obj, "name", fullName);
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
                return;
            }
            obj.add("completion", completion);
        }

        String rewardRaw = safe(questRewardBox.getValue()).trim();
        if (!rewardRaw.isBlank()) {
            JsonElement reward = parseRewardJson(rewardRaw);
            if (reward == null || !reward.isJsonObject()) {
                setError("Invalid JSON");
                return;
            }
            obj.add("reward", reward);
        }

        Path target = currentPack.questsDir.resolve(id + ".json");
        saveJson(obj, target, editingPath);
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
        boolean zipped = zipCurrentPackSafe();
        Minecraft.getInstance().reloadResourcePacks();
        QuestData.loadClient(true);
        return zipped;
    }

    private void clearEditor() {
        editorType = EditorType.NONE;
        editingPath = null;
        loadedQuestType = "";
        setActiveFields(List.of());
        saveButton.visible = false;
        saveButton.active = false;
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
    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, leftX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
        gg.blit(PANEL_TEX, rightX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        renderEditorFields(gg, mouseX, mouseY);

        super.render(gg, mouseX, mouseY, partialTick);
        renderIconOverlays(gg);

        if (!statusMessage.isBlank()) {
            gg.drawString(font, statusMessage, pxRight + 2, topY + PANEL_H + 8, statusColor, false);
        }
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

    private void renderEditorFields(GuiGraphics gg, int mouseX, int mouseY) {
        updateDynamicFieldSizes();
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
            } else if (field.widget instanceof MultiLineEditBox mb) {
                mb.setWidth(pw - 4);
            } else if (field.widget instanceof ToggleButton tb) {
                tb.setSize(TOGGLE_SIZE, TOGGLE_SIZE);
            }

            boolean inside = boxY >= clipTop && boxY + field.widget.getHeight() <= clipBottom;
            field.widget.visible = inside;
            field.widget.active = inside;

            if (labelY >= clipTop && labelY + font.lineHeight <= clipBottom) {
                gg.drawString(font, field.displayLabel, pxRight + 2, labelY, 0xFFFFFF, false);
                if (!field.tooltip.isBlank()) {
                    int labelW = font.width(field.displayLabel);
                    boolean hoverLabel = mouseX >= pxRight + 2 && mouseX <= pxRight + 2 + labelW
                            && mouseY >= labelY && mouseY <= labelY + font.lineHeight;
                    boolean hoverWidget = mouseX >= boxX && mouseX <= boxX + (pw - 4)
                            && mouseY >= boxY && mouseY <= boxY + field.widget.getHeight();
                    if (hoverLabel || hoverWidget) {
                        gg.renderTooltip(font, Component.literal(field.tooltip), mouseX, mouseY);
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
        if (field.widget instanceof MultiLineEditBox mb) {
            return computeMultilineHeight(mb, BOX_H_TALL);
        }
        return field.widget.getHeight();
    }

    private void updateDynamicFieldSizes() {
        for (FormField field : activeFields) {
            if (field.widget instanceof MultiLineEditBox mb) {
                mb.setWidth(pw - 4);
                mb.setHeight(computeMultilineHeight(mb, BOX_H_TALL));
            }
        }
    }

    private int computeMultilineHeight(MultiLineEditBox box, int minHeight) {
        int width = Math.max(20, box.getWidth() - 8);
        String text = box.getValue();
        int lines = text == null || text.isBlank()
                ? 1
                : Math.max(1, font.split(Component.literal(text), width).size());
        int content = lines * 9 + 8;
        return Math.max(minHeight, content);
    }

    private void updateBackButtonVisibility() {
        if (backButton == null) return;
        backButton.visible = true;
        backButton.active = true;
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
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            EditBox focused = focusedIconBox();
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

    private EditBox focusedIconBox() {
        if (questIconBox != null && questIconBox.isFocused()) return questIconBox;
        if (catIconBox != null && catIconBox.isFocused()) return catIconBox;
        if (subIconBox != null && subIconBox.isFocused()) return subIconBox;
        return null;
    }

    private boolean isIconBox(EditBox box) {
        return box == questIconBox || box == catIconBox || box == subIconBox;
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

        String suggestion = findSuggestion(input);
        if ((suggestion == null || suggestion.isBlank()) && !input.contains(":")) {
            suggestion = findSuggestion("minecraft:" + input);
        }
        return suggestion == null ? "" : suggestion;
    }

    private String findSuggestion(String prefix) {
        if (prefix == null || prefix.isBlank()) return "";
        for (String id : itemIdCache) {
            if (id.startsWith(prefix)) return id;
        }
        return "";
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
