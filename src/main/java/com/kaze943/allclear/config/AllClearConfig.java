package com.kaze943.allclear.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class AllClearConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // 清理间隔（tick）
    public static final ModConfigSpec.IntValue CLEAN_INTERVAL_TICKS;
    // 倒计时秒数列表
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> COUNTDOWN_TIMES;
    // 清理前提示文本
    public static final ModConfigSpec.ConfigValue<String> BROADCAST_BEFORE;
    // 清理后提示文本
    public static final ModConfigSpec.ConfigValue<String> BROADCAST_AFTER;
    // 低 TPS 阈值
    public static final ModConfigSpec.DoubleValue LOW_TPS_THRESHOLD;
    // 自动清理冷却（秒）
    public static final ModConfigSpec.IntValue AUTO_CLEAN_COOLDOWN_SECONDS;
    // 低 TPS 自动清理提示消息
    public static final ModConfigSpec.ConfigValue<String> LOW_TPS_MESSAGE;
    // 黑名单列表
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;

    static {
        BUILDER.push("allclear");

        CLEAN_INTERVAL_TICKS = BUILDER
                .comment("清理间隔（tick），20 tick = 1 秒", "默认: 12000 tick (600秒)")
                .defineInRange("cleanIntervalTicks", 12000, 20, Integer.MAX_VALUE);

        COUNTDOWN_TIMES = BUILDER
                .comment("倒计时提示的秒数列表（在清理前这些秒数时发送提示）", "默认: [120, 60, 30, 10, 5, 4, 3, 2, 1]")
                .defineList("countdownTimes", List.of(120, 60, 30, 10, 5, 4, 3, 2, 1), it -> it instanceof Integer);

        BROADCAST_BEFORE = BUILDER
                .comment("清理前提示文本", "支持颜色代码 §", "默认: §e[服务器] §a将在 §b{seconds}秒 §a后清理地面物品！")
                .define("broadcastBefore", "§e[服务器] §a将在 §b{seconds}秒 §a后清理地面物品！");

        BROADCAST_AFTER = BUILDER
                .comment("清理后提示文本，使用 {count} 代表清理的物品数量", "支持颜色代码 §", "默认: §e[服务器] §a已清理 §c{count} §a个掉落物品。")
                .define("broadcastAfter", "§e[服务器] §a已清理 §c{count} §a个掉落物品。");

        LOW_TPS_THRESHOLD = BUILDER
                .comment("触发低 TPS 自动清理的阈值（例如 15.0 表示低于 15 TPS 触发）", "设置为 0 可禁用此功能")
                .defineInRange("lowTpsThreshold", 15.0, 0, 20.0);

        AUTO_CLEAN_COOLDOWN_SECONDS = BUILDER
                .comment("低 TPS 自动清理的最小间隔（秒），防止频繁触发")
                .defineInRange("autoCleanCooldown", 60, 10, 3600);

        LOW_TPS_MESSAGE = BUILDER
                .comment("低 TPS 自动清理时的提示消息，可用变量: {count} (清理数量), {threshold} (当前阈值)", "默认: §c检测到服务器负载过高（TPS低于 {threshold}），已自动清理 {count} 个物品。")
                .define("lowTpsMessage", "§c检测到服务器负载过高（TPS低于 {threshold}），已自动清理 {count} 个物品。");

        BLACKLIST = BUILDER
                .comment("黑名单物品列表（不会被清理）", "格式：可以是物品 ID，如 'minecraft:diamond'", "也可以是标签，如 '#minecraft:music_discs'")
                .defineList("blacklist", List.of(), it -> it instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

