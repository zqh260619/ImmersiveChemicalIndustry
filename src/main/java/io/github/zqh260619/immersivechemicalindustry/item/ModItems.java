package io.github.zqh260619.immersivechemicalindustry.item;

import io.github.zqh260619.immersivechemicalindustry.ImmersiveChemicalIndustry;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item FLINT_HAND_AXE=registerItems("flint_hand_axe",new Item(new Item.Settings()));

    private static Item registerItems(String id,Item item){
        return Registry.register(Registries.ITEM,Identifier.of(ImmersiveChemicalIndustry.MOD_ID,id),item);
    }

    public static void registerModItems(){
        ImmersiveChemicalIndustry.LOGGER.info("Registering items...");
    }

}
