package com.example.hardnetherite;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

public class HardNetherite extends JavaPlugin {

    private static final NamespacedKey VANILLA_RECIPE_KEY = NamespacedKey.minecraft("netherite_ingot");
    private static final NamespacedKey ARMOR_KEY = new NamespacedKey("hardnetherite", "armor_health");
    private static final NamespacedKey FULL_SET_KEY = new NamespacedKey("hardnetherite", "full_set_health");
    private static final NamespacedKey SCRAP_TO_NUGGET_KEY = new NamespacedKey("hardnetherite", "scrap_to_nugget");
    private static final NamespacedKey NUGGET_TO_INGOT_KEY = new NamespacedKey("hardnetherite", "nugget_to_ingot");
    private static final int EFFECT_DURATION = 100;

    private static final Set<Material> NETHERITE_ARMOR = Set.of(
        Material.NETHERITE_HELMET,
        Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS,
        Material.NETHERITE_BOOTS
    );

    private static final Set<Material> NETHERITE_TOOLS = Set.of(
        Material.NETHERITE_PICKAXE,
        Material.NETHERITE_AXE,
        Material.NETHERITE_SHOVEL,
        Material.NETHERITE_HOE
    );

    private static final Set<Material> NETHERITE_WEAPONS = Set.of(
        Material.NETHERITE_SWORD
    );

    @Override
    public void onEnable() {
        removeVanillaRecipe();
        registerScrapToNuggetRecipe();
        registerNuggetToIngotRecipe();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateArmorBonus(player);
                updateHeldEffects(player);
            }
        }, 0L, 10L);

        getLogger().info("HardNetherite enabled!");
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearEffects(player);
        }
        getLogger().info("HardNetherite disabled.");
    }

    private void removeVanillaRecipe() {
        Bukkit.removeRecipe(VANILLA_RECIPE_KEY);
    }

    private ItemStack createNetheriteNugget() {
        ItemStack nugget = new ItemStack(Material.NETHERITE_SCRAP);
        ItemMeta meta = nugget.getItemMeta();
        meta.setDisplayName("§rNetherite Nugget");
        meta.setLore(List.of("§7A fragment of a netherite ingot"));
        meta.setEnchantmentGlintOverride(true);
        nugget.setItemMeta(meta);
        return nugget;
    }

    private void registerScrapToNuggetRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(SCRAP_TO_NUGGET_KEY, createNetheriteNugget());
        recipe.shape("NNN", "NNN", "NNN");
        recipe.setIngredient('N', Material.NETHERITE_SCRAP);
        Bukkit.addRecipe(recipe);
    }

    private void registerNuggetToIngotRecipe() {
        ItemStack ingot = new ItemStack(Material.NETHERITE_INGOT);
        ShapelessRecipe recipe = new ShapelessRecipe(NUGGET_TO_INGOT_KEY, ingot);
        RecipeChoice.ExactChoice nuggetChoice = new RecipeChoice.ExactChoice(createNetheriteNugget());
        for (int i = 0; i < 4; i++) {
            recipe.addIngredient(nuggetChoice);
        }
        recipe.addIngredient(4, Material.GOLD_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    private int countNetheriteArmor(Player player) {
        int count = 0;
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getArmorContents()) {
            if (item != null && NETHERITE_ARMOR.contains(item.getType())) {
                count++;
            }
        }
        return count;
    }

    private void updateArmorBonus(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        attr.removeModifier(ARMOR_KEY);
        attr.removeModifier(FULL_SET_KEY);

        int pieces = countNetheriteArmor(player);
        if (pieces > 0) {
            attr.addTransientModifier(new AttributeModifier(
                ARMOR_KEY, pieces * 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY
            ));
        }
        if (pieces == 4) {
            attr.addTransientModifier(new AttributeModifier(
                FULL_SET_KEY, 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY
            ));
        }
    }

    private void updateHeldEffects(Player player) {
        PlayerInventory inv = player.getInventory();
        boolean hasTool = isNetheriteTool(inv.getItemInMainHand()) || isNetheriteTool(inv.getItemInOffHand());
        boolean hasWeapon = isNetheriteWeapon(inv.getItemInMainHand()) || isNetheriteWeapon(inv.getItemInOffHand());

        if (hasTool) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, EFFECT_DURATION, 0, false, false, true));
        }
        if (hasWeapon) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, EFFECT_DURATION, 0, false, false, true));
        }
    }

    private boolean isNetheriteTool(ItemStack item) {
        return item != null && NETHERITE_TOOLS.contains(item.getType());
    }

    private boolean isNetheriteWeapon(ItemStack item) {
        return item != null && NETHERITE_WEAPONS.contains(item.getType());
    }

    private void clearEffects(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(ARMOR_KEY);
            attr.removeModifier(FULL_SET_KEY);
        }
        player.removePotionEffect(PotionEffectType.HASTE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }
}
