package com.recipeeditor.config;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.*;
import java.util.stream.Stream;

public class CustomRecipeData {
    public String id = "";
    public String resultItemId = "minecraft:totem_of_undying";
    public int resultCount = 1;
    public RecipeTypeEnum type = RecipeTypeEnum.SHAPED_CRAFTING;
    public String[] patternSlots = new String[9];
    public float experience = 0.1f;
    public int cookingTime = 200;
    public boolean enabled = true;
    public boolean overrideExisting = false;
    public String overriddenId = null;
    public String overriddenKey = null;
    public boolean isShapeless = false;
    public Map<String, Integer> typeCounts = new HashMap<>();
    public transient String originalKey = null;

    public transient int patternWidth = 0;
    public transient int patternHeight = 0;
    public transient int minRow = 0;
    public transient int minCol = 0;
    public transient int maxRow = 0;
    public transient int maxCol = 0;
    private transient int patternBoundsHash = 0;
    private transient Ingredient[] cachedIngredients = null;
    private transient int cachedPatternHash = 0;
    private transient List<Ingredient> cachedNonEmptyIngredients = null;
    private transient ShapedRecipePattern cachedRawRecipe = null;
    private transient int cachedHash = 0;

    public CustomRecipeData() {
        Arrays.fill(patternSlots, "minecraft:air");
    }

    public CustomRecipeData(String id, String resultItemId, int resultCount, RecipeTypeEnum type) {
        this();
        this.id = id;
        this.resultItemId = resultItemId;
        this.resultCount = resultCount;
        this.type = type;
    }

    public int getResultCountForType(RecipeTypeEnum targetType) {
        if (targetType == null) return this.resultCount;
        if (typeCounts != null && typeCounts.containsKey(targetType.name())) {
            Integer count = typeCounts.get(targetType.name());
            if (count != null && count > 0) return count;
        }
        return this.resultCount;
    }

    public void setResultCountForType(RecipeTypeEnum targetType, int count) {
        if (targetType == null) return;
        if (typeCounts == null) typeCounts = new HashMap<>();
        typeCounts.put(targetType.name(), count);
        if (this.type == targetType) {
            this.resultCount = count;
        }
    }

    public String getPatternSignature() {
        if (patternSlots == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < patternSlots.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(patternSlots[i]);
        }
        return sb.toString();
    }

    public String getKey() {
        return resultItemId + "#" + type.name() + (isShapeless ? "#SL#" : "#") + getPatternSignature();
    }

    public void invalidateCache() {
        patternBoundsHash = 0;
        cachedIngredients = null;
        cachedPatternHash = 0;
        cachedNonEmptyIngredients = null;
        cachedRawRecipe = null;
        cachedHash = 0;
    }

    public Item getResultItem() {
        if (resultItemId == null || resultItemId.isEmpty() || resultItemId.equals("minecraft:air")) {
            return Items.AIR;
        }
        ResourceLocation id = ResourceLocation.tryParse(resultItemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.get(id);
    }

    public void setResultItem(Item item) {
        if (item == null || item == Items.AIR) {
            this.resultItemId = "minecraft:air";
        } else {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            this.resultItemId = id != null ? id.toString() : "minecraft:air";
        }
    }

    public Item getItemAt(int slot) {
        if (slot < 0 || slot >= 9 || patternSlots == null || slot >= patternSlots.length) {
            return Items.AIR;
        }
        String idStr = patternSlots[slot];
        if (idStr == null || idStr.isEmpty() || idStr.equals("minecraft:air")) {
            return Items.AIR;
        }
        if (idStr.startsWith("#")) {
            return com.recipeeditor.inspector.TagResolver.resolveTag(idStr);
        }
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.get(id);
    }

    public String getSlotString(int slot) {
        if (slot < 0 || slot >= 9 || patternSlots == null || slot >= patternSlots.length) {
            return "minecraft:air";
        }
        return patternSlots[slot] != null ? patternSlots[slot] : "minecraft:air";
    }

    public void setSlotString(int slot, String str) {
        if (slot >= 0 && slot < 9 && patternSlots != null && slot < patternSlots.length) {
            String val = (str == null || str.isEmpty()) ? "minecraft:air" : str;
            if (!val.equals(patternSlots[slot])) {
                patternSlots[slot] = val;
                invalidateCache();
            }
        }
    }

    public void setItemAt(int slot, Item item) {
        if (slot < 0 || slot >= 9 || patternSlots == null || slot >= patternSlots.length) {
            return;
        }
        if (item == null || item == Items.AIR) {
            setSlotString(slot, "minecraft:air");
        } else {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            setSlotString(slot, id != null ? id.toString() : "minecraft:air");
        }
    }

    public Ingredient getIngredientAt(int slot) {
        if (slot < 0 || slot >= 9 || patternSlots == null || slot >= patternSlots.length) {
            return null;
        }
        int currentHash = Arrays.hashCode(patternSlots);
        if (cachedIngredients == null || cachedPatternHash != currentHash) {
            Ingredient[] temp = new Ingredient[9];
            for (int i = 0; i < 9; i++) {
                temp[i] = computeIngredientForSlot(i);
            }
            cachedIngredients = temp;
            cachedPatternHash = currentHash;
        } else if (cachedIngredients[slot] != null && cachedIngredients[slot].isEmpty()) {
            String slotStr = getSlotString(slot);
            if (slotStr != null && slotStr.startsWith("#")) {
                Ingredient retry = computeIngredientForSlot(slot);
                if (retry != null && !retry.isEmpty()) {
                    cachedIngredients[slot] = retry;
                    cachedNonEmptyIngredients = null;
                }
            }
        }
        return cachedIngredients[slot];
    }

    public synchronized void computePatternBounds() {
        int currentHash = Arrays.hashCode(patternSlots);
        if (patternBoundsHash == currentHash && minRow != -1) return;
        int rMin = 3, rMax = -1, cMin = 3, cMax = -1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int slot = r * 3 + c;
                String slotStr = getSlotString(slot);
                if (!slotStr.isEmpty() && !slotStr.equals("minecraft:air")) {
                    if (r < rMin) rMin = r;
                    if (r > rMax) rMax = r;
                    if (c < cMin) cMin = c;
                    if (c > cMax) cMax = c;
                }
            }
        }
        if (rMax == -1) {
            minRow = 0;
            minCol = 0;
            maxRow = 0;
            maxCol = 0;
            patternWidth = 0;
            patternHeight = 0;
        } else {
            minRow = rMin;
            minCol = cMin;
            maxRow = rMax;
            maxCol = cMax;
            patternWidth = cMax - cMin + 1;
            patternHeight = rMax - rMin + 1;
        }
        patternBoundsHash = currentHash;
    }

    public int getPatternWidth() {
        computePatternBounds();
        return patternWidth;
    }

    public int getPatternHeight() {
        computePatternBounds();
        return patternHeight;
    }

    public int getMinRow() {
        computePatternBounds();
        return minRow;
    }

    public int getMinCol() {
        computePatternBounds();
        return minCol;
    }

    public int getMaxRow() {
        computePatternBounds();
        return maxRow;
    }

    public int getMaxCol() {
        computePatternBounds();
        return maxCol;
    }

    public Ingredient computeIngredientForSlot(int slot) {
        String slotStr = getSlotString(slot);
        if (slotStr.isEmpty() || slotStr.equals("minecraft:air")) {
            return null;
        }
        if (slotStr.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(slotStr.substring(1));
            if (tagId != null) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
                var entryList = BuiltInRegistries.ITEM.getTag(tagKey);
                if (entryList.isPresent() && entryList.get().size() > 0) {
                    return Ingredient.of(tagKey);
                }
                List<Item> items = com.recipeeditor.inspector.TagResolver.getAllItemsForTag(slotStr);
                if (!items.isEmpty()) {
                    return Ingredient.of(items.toArray(new Item[0]));
                }
                return Ingredient.of(tagKey);
            }
            TagKey<Item> emptyKey = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "unmatchable_empty"));
            return Ingredient.of(emptyKey);
        }
        Item item = getItemAt(slot);
        if (item != Items.AIR) {
            return Ingredient.of(item);
        }
        return null;
    }

    public Optional<Ingredient> createIngredientForSlot(int slot) {
        Ingredient ing = getIngredientAt(slot);
        return ing != null ? Optional.of(ing) : Optional.empty();
    }

    public synchronized ShapedRecipePattern getRawRecipe() {
        int hash = Arrays.hashCode(patternSlots);
        if (cachedRawRecipe == null || cachedHash != hash) {
            computePatternBounds();
            if (patternWidth == 0 || patternHeight == 0) {
                cachedRawRecipe = new ShapedRecipePattern(1, 1, NonNullList.of(Ingredient.EMPTY, Ingredient.EMPTY), Optional.empty());
            } else {
                NonNullList<Ingredient> ingredients = NonNullList.withSize(patternWidth * patternHeight, Ingredient.EMPTY);
                int idx = 0;
                for (int r = minRow; r <= maxRow; r++) {
                    for (int c = minCol; c <= maxCol; c++) {
                        Ingredient ing = getIngredientAt(r * 3 + c);
                        ingredients.set(idx++, ing != null ? ing : Ingredient.EMPTY);
                    }
                }
                cachedRawRecipe = new ShapedRecipePattern(patternWidth, patternHeight, ingredients, Optional.empty());
            }
            cachedHash = hash;
        }
        return cachedRawRecipe;
    }

    public CustomRecipeData copy() {
        CustomRecipeData copy = new CustomRecipeData();
        copy.id = this.id;
        copy.resultItemId = this.resultItemId;
        copy.resultCount = this.resultCount;
        copy.type = this.type;
        if (this.patternSlots != null) {
            copy.patternSlots = Arrays.copyOf(this.patternSlots, this.patternSlots.length);
        }
        copy.experience = this.experience;
        copy.cookingTime = this.cookingTime;
        copy.enabled = this.enabled;
        copy.overrideExisting = this.overrideExisting;
        copy.overriddenId = this.overriddenId;
        copy.overriddenKey = this.overriddenKey;
        copy.isShapeless = this.isShapeless;
        if (this.typeCounts != null) {
            copy.typeCounts = new HashMap<>(this.typeCounts);
        }
        copy.originalKey = this.originalKey;
        return copy;
    }

    public synchronized List<Ingredient> getNonEmptyIngredients() {
        if (cachedNonEmptyIngredients == null) {
            List<Ingredient> list = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                Ingredient ing = getIngredientAt(i);
                if (ing != null && !ing.isEmpty()) {
                    list.add(ing);
                }
            }
            cachedNonEmptyIngredients = Collections.unmodifiableList(list);
        }
        return cachedNonEmptyIngredients;
    }

    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static CustomRecipeData fromJson(String json) {
        return GSON.fromJson(json, CustomRecipeData.class);
    }

}