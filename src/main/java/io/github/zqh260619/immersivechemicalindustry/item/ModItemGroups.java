/*
 * Copyright 2026 zqh260619
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.zqh260619.immersivechemicalindustry.item;

import io.github.zqh260619.immersivechemicalindustry.ImmersiveChemicalIndustry;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static final ItemGroup ICI_TOOLS=Registry.register(Registries.ITEM_GROUP,Identifier.of(ImmersiveChemicalIndustry.MOD_ID,"ici_tools"),
            ItemGroup.create(null,-1).displayName(Text.translatable("itemGroup.ici_tools"))
                    .icon(()->new ItemStack(ModItems.FLINT_HAND_AXE))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.FLINT_HAND_AXE);
                    }).build());
    public static final ItemGroup ICI_INGREDIENTS=Registry.register(Registries.ITEM_GROUP,Identifier.of(ImmersiveChemicalIndustry.MOD_ID,"ici_ingredients"),
            ItemGroup.create(null,-1).displayName(Text.translatable("itemGroup.ici_ingredients"))
                    .icon(()->new ItemStack(ModItems.THIN_ROPE))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.THIN_ROPE);
                    }).build());
    public static void registerModItemGroups(){
        ImmersiveChemicalIndustry.LOGGER.info("Registering item groups...");
    }

}
