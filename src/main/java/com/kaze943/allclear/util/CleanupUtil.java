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
        return cleanupAll(server, AllClearConfig.CHUNK_SCAN_MODE.get());
    }

    public static int cleanupAll(MinecraftServer server, AllClearConfig.ChunkScanMode mode) {
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
            List<ItemEntity> items = getItemsInLevel(level, mode);
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

    public static int countTotalItems(MinecraftServer server) {
        return countTotalItems(server, AllClearConfig.CHUNK_SCAN_MODE.get());
    }

    public static int countTotalItems(MinecraftServer server, AllClearConfig.ChunkScanMode mode) {
        int total = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (ItemEntity item : getItemsInLevel(level, mode)) {
                total += item.getItem().getCount();
            }
        }
        return total;
    }

    private static List<ItemEntity> getItemsInLevel(ServerLevel level, AllClearConfig.ChunkScanMode mode) {
        List<ItemEntity> items = new ArrayList<>();
        // ALL 和 LOADED 都处理所有已加载实体
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof ItemEntity item) {
                items.add(item);
            }
        }
        return items;
    }
}
