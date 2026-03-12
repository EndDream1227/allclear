package com.kaze943.allclear.util;

import com.kaze943.allclear.config.AllClearConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CleanupUtil {
    public static int cleanupAll(MinecraftServer server) {
        // 每次清理都重新加载黑名单，支持热重载
        Set<Item> blacklistItems = new HashSet<>();
        Set<TagKey<Item>> blacklistTags = new HashSet<>();
        for (String entry : AllClearConfig.BLACKLIST.get()) {
            if (entry.startsWith("#")) {
                ResourceLocation id = ResourceLocation.parse(entry.substring(1));
                blacklistTags.add(TagKey.create(Registries.ITEM, id));
            } else {
                ResourceLocation id = ResourceLocation.parse(entry);
                BuiltInRegistries.ITEM.getOptional(id).ifPresent(blacklistItems::add);
            }
        }

        int total = 0;
        for (ServerLevel level : server.getAllLevels()) {
            // 正确的方式：使用传统 for 循环获取所有 ItemEntity
            List<ItemEntity> items = new ArrayList<>();
            for (Entity entity : level.getEntities().getAll()) {
                if (entity instanceof ItemEntity itemEntity) {
                    items.add(itemEntity);
                }
            }

            for (ItemEntity itemEntity : items) {
                Item item = itemEntity.getItem().getItem();
                if (blacklistItems.contains(item)) continue;
                boolean tagged = false;
                for (TagKey<Item> tag : blacklistTags) {
                    if (item.builtInRegistryHolder().is(tag)) {
                        tagged = true;
                        break;
                    }
                }
                if (tagged) continue;
                itemEntity.kill();
                total++;
            }
        }
        return total;
    }
}
