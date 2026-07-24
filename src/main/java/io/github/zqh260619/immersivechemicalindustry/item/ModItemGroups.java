package io.github.zqh260619.immersivechemicalindustry.item;

import io.github.zqh260619.immersivechemicalindustry.ImmersiveChemicalIndustry;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

//    public static final RegistryKey<ItemGroup> ICI_TOOLS=register("ici_tools");
//
//    private static RegistryKey<ItemGroup> register(String id){
//        return RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(ImmersiveChemicalIndustry.MOD_ID,id));
//    }
//
//    public static void registerModItemGroups(){
//        Registry.register(Registries.ITEM_GROUP,ICI_TOOLS,
//                ItemGroup.create(ItemGroup.Row.TOP,7)
//                        .displayName(Text.translatable("itemGroup.immersivechemicalchemistry.ici_tools"))
//                        .icon(()->new ItemStack(ModItems.FLINT_HAND_AXE))
//                        .entries((displayContext, entries) -> {
//                            entries.add(ModItems.FLINT_HAND_AXE);
//                        }).build());
//        ImmersiveChemicalIndustry.LOGGER.info("Registering item groups...");
//    }

    public static final ItemGroup ICI_TOOLS=Registry.register(Registries.ITEM_GROUP,Identifier.of(ImmersiveChemicalIndustry.MOD_ID,"ici_tools"),
            ItemGroup.create(null,-1).displayName(Text.translatable("itemGroup.ici_tools"))
                    .icon(()->new ItemStack(ModItems.FLINT_HAND_AXE))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.FLINT_HAND_AXE);
                    }).build());
    public static void registerModItemGroups(){
        ImmersiveChemicalIndustry.LOGGER.info("Registering item groups...");
    }

}
