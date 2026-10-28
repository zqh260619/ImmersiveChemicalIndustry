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
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item FLINT_HAND_AXE=registerItems("flint_hand_axe",new Item(new Item.Settings()));
    public static final Item THIN_ROPE=registerItems("thin_rope",new Item(new Item.Settings()));

    private static Item registerItems(String id,Item item){
        return Registry.register(Registries.ITEM,Identifier.of(ImmersiveChemicalIndustry.MOD_ID,id),item);
    }

    private static void addItemToIG(FabricItemGroupEntries fabricItemGroupEntries){
        fabricItemGroupEntries.add(FLINT_HAND_AXE);
        fabricItemGroupEntries.add(THIN_ROPE);
    }

    public static void registerModItems(){
        ImmersiveChemicalIndustry.LOGGER.info("Registering items...");
    }

}
