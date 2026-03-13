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
    // 清理前提示文本（兼容旧配置）
    public static final ModConfigSpec.ConfigValue<String> BROADCAST_BEFORE;
    public static final ModConfigSpec.ConfigValue<String> BROADCAST_AFTER;
    // 低 TPS 阈值
    public static final ModConfigSpec.DoubleValue LOW_TPS_THRESHOLD;
    // 自动清理冷却（秒）
    public static final ModConfigSpec.IntValue AUTO_CLEAN_COOLDOWN_SECONDS;
    // 黑名单列表
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;

    // 文本模式
    public static final ModConfigSpec.ConfigValue<String> TEXT_MODE;
    // 聊天栏模式文本
    public static final ModConfigSpec.ConfigValue<String> DIALOG_BROADCAST_BEFORE;
    public static final ModConfigSpec.ConfigValue<String> DIALOG_BROADCAST_AFTER;
    public static final ModConfigSpec.ConfigValue<String> DIALOG_LOW_TPS_MESSAGE;
    // 标题模式文本
    public static final ModConfigSpec.ConfigValue<String> SUBTITLE_BROADCAST_BEFORE;
    public static final ModConfigSpec.ConfigValue<String> SUBTITLE_BROADCAST_AFTER;
    public static final ModConfigSpec.ConfigValue<String> SUBTITLE_LOW_TPS_MESSAGE;

    // 智能静默
    public static final ModConfigSpec.IntValue AUTO_SILENCE_ITEM_THRESHOLD;
    public static final ModConfigSpec.IntValue AUTO_SILENCE_DURATION_SECONDS;
    public static final ModConfigSpec.IntValue AUTO_SILENCE_CHECK_INTERVAL_TICKS; // 新增检测间隔

    // 自动静默提示文本
    public static final ModConfigSpec.ConfigValue<String> AUTO_SILENCE_ON_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> AUTO_SILENCE_OFF_MESSAGE;

    // 强制清除阈值
    public static final ModConfigSpec.IntValue FORCE_CLEAR_ITEM_THRESHOLD;
    // 物品数量更新间隔（tick）
    public static final ModConfigSpec.IntValue ITEM_COUNT_UPDATE_INTERVAL_TICKS;

    // 区块扫描模式枚举
    public static enum ChunkScanMode {
        ALL, LOADED
    }
    public static final ModConfigSpec.EnumValue<ChunkScanMode> CHUNK_SCAN_MODE;

    // 静默模式开关提示
    public static final ModConfigSpec.ConfigValue<String> SLEEPMODE_ON_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> SLEEPMODE_OFF_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> SLEEPMODE_NOT_AVAILABLE;

    // 黑名单管理提示
    public static final ModConfigSpec.ConfigValue<String> BLACKLIST_ADD_SUCCESS;
    public static final ModConfigSpec.ConfigValue<String> BLACKLIST_ADD_FAIL;
    public static final ModConfigSpec.ConfigValue<String> BLACKLIST_REMOVE_SUCCESS;
    public static final ModConfigSpec.ConfigValue<String> BLACKLIST_REMOVE_FAIL;

    // 区块模式切换提示
    public static final ModConfigSpec.ConfigValue<String> CHUNK_MODE_CHANGED;
    public static final ModConfigSpec.ConfigValue<String> CHUNK_MODE_CONFIRM;
    public static final ModConfigSpec.IntValue CHUNK_MODE_CONFIRM_SECONDS;
    public static final ModConfigSpec.ConfigValue<String> CHUNK_MODE_TIMEOUT;

    static {
        BUILDER.push("allclear");

        CLEAN_INTERVAL_TICKS = BUILDER
                .comment("清理间隔（tick），20 tick = 1 秒", "默认: 1800 tick (90秒)")
                .defineInRange("cleanIntervalTicks", 1800 , 20, Integer.MAX_VALUE);

        COUNTDOWN_TIMES = BUILDER
                .comment("倒计时提示的秒数列表", "默认: [30, 10, 5, 4, 3, 2, 1]")
                .defineList("countdownTimes", List.of(30, 10, 5, 4, 3, 2, 1), it -> it instanceof Integer);

        BROADCAST_BEFORE = BUILDER
                .comment("(旧版)清理前提示文本，使用 {seconds}")
                .define("broadcastBefore", "§e[AllClear] §a将在 §b{seconds}秒 §a后清理地面物品！");

        BROADCAST_AFTER = BUILDER
                .comment("(旧版)清理后提示文本，使用 {count}")
                .define("broadcastAfter", "§e[AllClear] §a已清理 §c{count} §a个掉落物品");

        LOW_TPS_THRESHOLD = BUILDER
                .comment("触发低 TPS 自动清理的阈值（例如 15.0）", "设置为 0 禁用")
                .defineInRange("lowTpsThreshold", 15.0, 0, 20.0);

        AUTO_CLEAN_COOLDOWN_SECONDS = BUILDER
                .comment("低 TPS 自动清理的最小间隔（秒）")
                .defineInRange("autoCleanCooldown", 60, 10, 3600);

        BLACKLIST = BUILDER
                .comment("黑名单物品列表", "格式：物品ID或标签，如 'minecraft:diamond', '#minecraft:music_discs'")
                .defineList("blacklist", List.of(), it -> it instanceof String);

        // === 文本模式 ===
        TEXT_MODE = BUILDER
                .comment("提示文本模式: 'dialog' 或 'subtitle'")
                .define("textMode", "dialog");

        DIALOG_BROADCAST_BEFORE = BUILDER
                .comment("聊天栏清理前提示，可用 {seconds}")
                .define("dialogBroadcastBefore", "§e[AllClear] §a将在 §b{seconds}秒 §a后清理地面物品！");
        DIALOG_BROADCAST_AFTER = BUILDER
                .comment("聊天栏清理后提示，可用 {count}")
                .define("dialogBroadcastAfter", "§e[AllClear] §a已清理 §c{count} §a个掉落物品。");
        DIALOG_LOW_TPS_MESSAGE = BUILDER
                .comment("聊天栏低TPS提示，可用 {count}, {threshold}")
                .define("dialogLowTpsMessage", "§e[AllClear] §c检测到服务器负载过高（TPS低于 {threshold}），已自动清理 {count} 个物品");

        SUBTITLE_BROADCAST_BEFORE = BUILDER
                .comment("标题清理前提示，可用 {seconds}")
                .define("subtitleBroadcastBefore", "§e[AllClear] §a将在 §b{seconds}秒 §a后清理地面物品！");
        SUBTITLE_BROADCAST_AFTER = BUILDER
                .comment("标题清理后提示，可用 {count}")
                .define("subtitleBroadcastAfter", "§e[AllClear] §a已清理 §c{count} §a个掉落物品");
        SUBTITLE_LOW_TPS_MESSAGE = BUILDER
                .comment("标题低TPS提示，可用 {count}, {threshold}")
                .define("subtitleLowTpsMessage", "§e[AllClear] §c检测到服务器负载过高（TPS低于 {threshold}），已自动清理 {count} 个物品");

        // === 智能静默 ===
        AUTO_SILENCE_ITEM_THRESHOLD = BUILDER
                .comment("自动静默的物品总数阈值（低于此值则自动暂停定时清理）")
                .defineInRange("autoSilenceItemThreshold", 10, 0, Integer.MAX_VALUE);
        AUTO_SILENCE_DURATION_SECONDS = BUILDER
                .comment("自动静默持续时间（秒）")
                .defineInRange("autoSilenceDurationSeconds", 60, 5, 3600);
        AUTO_SILENCE_CHECK_INTERVAL_TICKS = BUILDER
                .comment("自动静默检测间隔（tick）", "例如 10分钟 = 12000 tick", "默认: 12000 (10分钟)")
                .defineInRange("autoSilenceCheckIntervalTicks", 12000, 20, Integer.MAX_VALUE);

        // 自动静默提示
        AUTO_SILENCE_ON_MESSAGE = BUILDER
                .comment("自动静默启用时的提示文本", "可用 {count}")
                .define("autoSilenceOnMessage", "§e[AllClear] §a物品数量较少，自动静默已启用");
        AUTO_SILENCE_OFF_MESSAGE = BUILDER
                .comment("自动静默解除时的提示文本")
                .define("autoSilenceOffMessage", "§e[AllClear] §a自动静默已解除");

        // === 强制清除阈值 ===
        FORCE_CLEAR_ITEM_THRESHOLD = BUILDER
                .comment("强制清除的物品总数阈值（TPS低且超过此值时强制清理）")
                .defineInRange("forceClearItemThreshold", 500, 0, Integer.MAX_VALUE);

        // === 物品数量更新间隔 ===
        ITEM_COUNT_UPDATE_INTERVAL_TICKS = BUILDER
                .comment("物品总数统计更新间隔（tick）")
                .defineInRange("itemCountUpdateIntervalTicks", 200, 20, 1200);

        // === 区块扫描模式 ===
        CHUNK_SCAN_MODE = BUILDER
                .comment("区块遍历模式: ALL（所有区块）, LOADED（已加载区块）")
                .defineEnum("chunkScanMode", ChunkScanMode.LOADED);

        // === 静默模式开关提示 ===
        SLEEPMODE_ON_MESSAGE = BUILDER
                .comment("启用静默模式时的提示")
                .define("sleepmodeOnMessage", "§a[AllClear] §a静默模式已启用");
        SLEEPMODE_OFF_MESSAGE = BUILDER
                .comment("禁用静默模式时的提示")
                .define("sleepmodeOffMessage", "§c[AllClear] §c静默模式已禁用");
        SLEEPMODE_NOT_AVAILABLE = BUILDER
                .comment("静默模式未启用时执行 sleep/wakeup 的报错信息")
                .define("sleepmodeNotAvailable", "§c[AllClear] §c静默模式不可用，请尝试/allclear sleepingmode on");

        // === 黑名单管理提示 ===
        BLACKLIST_ADD_SUCCESS = BUILDER
                .comment("添加物品到黑名单成功")
                .define("blacklistAddSuccess", "§a[AllClear] §a已添加 {item} 到黑名单");
        BLACKLIST_ADD_FAIL = BUILDER
                .comment("添加物品到黑名单失败")
                .define("blacklistAddFail", "§c[AllClear] §c添加失败：{item} 已在黑名单中或无效");
        BLACKLIST_REMOVE_SUCCESS = BUILDER
                .comment("从黑名单移除物品成功")
                .define("blacklistRemoveSuccess", "§a[AllClear] §a已从黑名单移除 {item}");
        BLACKLIST_REMOVE_FAIL = BUILDER
                .comment("从黑名单移除物品失败")
                .define("blacklistRemoveFail", "§c[AllClear] §c移除失败：{item} 不在黑名单中");

        // === 区块模式切换提示 ===
        CHUNK_MODE_CHANGED = BUILDER
                .comment("区块遍历模式切换成功")
                .define("chunkModeChanged", "§a[AllClear] §a区块扫描模式已切换为: {mode}");
        CHUNK_MODE_CONFIRM = BUILDER
                .comment("切换到 all 模式时的确认提示")
                .define("chunkModeConfirm", "§c警告：切换到全区块扫描模式会消耗更多性能，可能造成服务器卡顿。请在 {seconds} 秒内再次输入此指令以确认");
        CHUNK_MODE_CONFIRM_SECONDS = BUILDER
                .comment("全区块扫描模式确认等待时间（秒）")
                .defineInRange("chunkModeConfirmSeconds", 10, 1, 60);
        CHUNK_MODE_TIMEOUT = BUILDER
                .comment("全区块扫描模式切换已取消（超时）")
                .define("chunkModeTimeout", "§c[AllClear] §c全区块扫描模式切换已取消（超时）");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
