package net.revilodev.boundless.compat;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class JeiCompat {
    private static Object runtime;

    private JeiCompat() {}

    public static boolean isJeiInstalled() {
        return ModList.get().isLoaded("jei");
    }

    public static void setRuntime(Object jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static boolean showItem(ItemStack stack, boolean showRecipe) {
        if (stack == null || stack.isEmpty() || runtime == null) return false;
        try {
            Class<?> ingredientRoleClass = Class.forName("mezz.jei.api.recipe.RecipeIngredientRole");
            Object role = Enum.valueOf((Class<Enum>) ingredientRoleClass, showRecipe ? "OUTPUT" : "INPUT");
            Object vanillaItemStackType = Class.forName("mezz.jei.api.constants.VanillaTypes")
                    .getField("ITEM_STACK")
                    .get(null);
            Object jeiHelpers = runtime.getClass().getMethod("getJeiHelpers").invoke(runtime);
            Object focusFactory = jeiHelpers.getClass().getMethod("getFocusFactory").invoke(jeiHelpers);
            Object focus = focusFactory.getClass()
                    .getMethod("createFocus", ingredientRoleClass, Class.forName("mezz.jei.api.ingredients.IIngredientType"), Object.class)
                    .invoke(focusFactory, role, vanillaItemStackType, stack.copy());
            Object recipesGui = runtime.getClass().getMethod("getRecipesGui").invoke(runtime);
            recipesGui.getClass()
                    .getMethod("show", Class.forName("mezz.jei.api.recipe.IFocus"))
                    .invoke(recipesGui, focus);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
