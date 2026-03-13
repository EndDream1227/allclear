package com.kaze943.allclear.command;

import com.kaze943.allclear.config.AllClearConfig;
import com.kaze943.allclear.event.CleanupHandler;
import com.kaze943.allclear.util.CleanupUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;

public class AllClearCommand {
    private static CleanupHandler handler;
    private static final Map<UUID, Integer> confirmMap = new HashMap<>();

    public static void setHandler(CleanupHandler h) { handler = h; }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("allclear")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("now").executes(AllClearCommand::runNow))
                .then(Commands.literal("on").executes(ctx -> setModEnabled(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> setModEnabled(ctx, false)))
                .then(Commands.literal("sleepmode")
                        .then(Commands.literal("on").executes(ctx -> setSleepMode(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setSleepMode(ctx, false))))
                .then(Commands.literal("sleep").executes(AllClearCommand::sleep))
                .then(Commands.literal("wakeup")
                        .executes(AllClearCommand::wakeup) // 无参数：解除手动静默
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(ctx -> wakeupWithDuration(ctx, StringArgumentType.getString(ctx, "duration")))))
                .then(Commands.literal("blacklist")
                        .then(Commands.literal("add")
                                .then(Commands.argument("entry", StringArgumentType.word())
                                        .executes(ctx -> blacklistAdd(ctx, StringArgumentType.getString(ctx, "entry")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("entry", StringArgumentType.word())
                                        .executes(ctx -> blacklistRemove(ctx, StringArgumentType.getString(ctx, "entry")))))
                )
                .then(Commands.literal("text")
                        .then(Commands.literal("dialog").executes(ctx -> setTextMode(ctx, "dialog")))
                        .then(Commands.literal("subtitle").executes(ctx -> setTextMode(ctx, "subtitle"))))
                .then(Commands.literal("chunks")
                        .then(Commands.literal("all").executes(ctx -> setChunkMode(ctx, AllClearConfig.ChunkScanMode.ALL)))
                        .then(Commands.literal("loaded").executes(ctx -> setChunkMode(ctx, AllClearConfig.ChunkScanMode.LOADED))))
        );
    }

    private static int runNow(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int cleaned = CleanupUtil.cleanupAll(source.getServer());
        source.sendSuccess(() -> Component.literal("已清理 " + cleaned + " 个掉落物（黑名单已过滤）"), true);
        return 1;
    }

    private static int setModEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        if (handler == null) return 0;
        handler.setModEnabled(enabled);
        ctx.getSource().sendSuccess(() -> Component.literal("Mod 已" + (enabled ? "启用" : "禁用")), true);
        return 1;
    }

    private static int setSleepMode(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        if (handler == null) return 0;
        handler.setSleepModeEnabled(enabled);
        String msg = enabled ? AllClearConfig.SLEEPMODE_ON_MESSAGE.get() : AllClearConfig.SLEEPMODE_OFF_MESSAGE.get();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    private static int sleep(CommandContext<CommandSourceStack> ctx) {
        if (handler == null) return 0;
        if (!handler.isSleepModeEnabled()) {
            String err = AllClearConfig.SLEEPMODE_NOT_AVAILABLE.get();
            ctx.getSource().sendFailure(Component.literal(err));
            return 0;
        }
        if (handler.isManuallySilenced()) {
            ctx.getSource().sendFailure(Component.literal("Mod 已处于手动静默状态"));
            return 0;
        }
        handler.setManuallySilenced(true);
        ctx.getSource().sendSuccess(() -> Component.literal("已手动静默 Mod"), true);
        return 1;
    }

    private static int wakeup(CommandContext<CommandSourceStack> ctx) {
        if (handler == null) return 0;
        if (!handler.isSleepModeEnabled()) {
            String err = AllClearConfig.SLEEPMODE_NOT_AVAILABLE.get();
            ctx.getSource().sendFailure(Component.literal(err));
            return 0;
        }
        if (!handler.isManuallySilenced()) {
            ctx.getSource().sendFailure(Component.literal("Mod 当前未处于手动静默状态"));
            return 0;
        }
        handler.setManuallySilenced(false);
        ctx.getSource().sendSuccess(() -> Component.literal("已解除手动静默"), true);
        return 1;
    }

    private static int wakeupWithDuration(CommandContext<CommandSourceStack> ctx, String durationArg) {
        if (handler == null) return 0;
        if (!handler.isSleepModeEnabled()) {
            String err = AllClearConfig.SLEEPMODE_NOT_AVAILABLE.get();
            ctx.getSource().sendFailure(Component.literal(err));
            return 0;
        }
        int ticks;
        try {
            ticks = parseTimeArgument(durationArg);
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.literal("无效的时间格式。支持: 数字(tick), 数字t(检测周期), 数字s(秒), 数字d(游戏天), 数字td(现实天)"));
            return 0;
        }
        // 退出手动静默（如果处于）
        handler.setManuallySilenced(false);
        // 设置强制不静默计时器
        handler.setForceNoSilenceTimer(ticks);
        ctx.getSource().sendSuccess(() -> Component.literal("已设置强制不静默 " + durationArg + " (" + ticks + " tick)"), true);
        return 1;
    }

    private static int blacklistAdd(CommandContext<CommandSourceStack> ctx, String entry) {
        List<String> current = new ArrayList<>(AllClearConfig.BLACKLIST.get());
        if (current.contains(entry)) {
            String fail = AllClearConfig.BLACKLIST_ADD_FAIL.get().replace("{item}", entry);
            ctx.getSource().sendFailure(Component.literal(fail));
            return 0;
        }
        current.add(entry);
        AllClearConfig.BLACKLIST.set(current);
        AllClearConfig.SPEC.save();
        String success = AllClearConfig.BLACKLIST_ADD_SUCCESS.get().replace("{item}", entry);
        ctx.getSource().sendSuccess(() -> Component.literal(success), true);
        return 1;
    }

    private static int blacklistRemove(CommandContext<CommandSourceStack> ctx, String entry) {
        List<String> current = new ArrayList<>(AllClearConfig.BLACKLIST.get());
        if (!current.remove(entry)) {
            String fail = AllClearConfig.BLACKLIST_REMOVE_FAIL.get().replace("{item}", entry);
            ctx.getSource().sendFailure(Component.literal(fail));
            return 0;
        }
        AllClearConfig.BLACKLIST.set(current);
        AllClearConfig.SPEC.save();
        String success = AllClearConfig.BLACKLIST_REMOVE_SUCCESS.get().replace("{item}", entry);
        ctx.getSource().sendSuccess(() -> Component.literal(success), true);
        return 1;
    }

    private static int setTextMode(CommandContext<CommandSourceStack> ctx, String mode) {
        AllClearConfig.TEXT_MODE.set(mode);
        AllClearConfig.SPEC.save();
        ctx.getSource().sendSuccess(() -> Component.literal("提示模式已切换为 " + mode), true);
        return 1;
    }

    private static int setChunkMode(CommandContext<CommandSourceStack> ctx, AllClearConfig.ChunkScanMode mode) {
        CommandSourceStack source = ctx.getSource();
        UUID uuid;
        try {
            uuid = source.getPlayerOrException().getUUID();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("该指令只能由玩家执行"));
            return 0;
        }

        if (mode == AllClearConfig.ChunkScanMode.ALL) {
            int confirmSeconds = AllClearConfig.CHUNK_MODE_CONFIRM_SECONDS.get();
            if (confirmMap.containsKey(uuid)) {
                int currentTick = source.getServer().getTickCount();
                int confirmTick = confirmMap.get(uuid);
                if (currentTick - confirmTick <= confirmSeconds * 20) {
                    confirmMap.remove(uuid);
                    AllClearConfig.CHUNK_SCAN_MODE.set(mode);
                    AllClearConfig.SPEC.save();
                    String msg = AllClearConfig.CHUNK_MODE_CHANGED.get().replace("{mode}", mode.name());
                    source.sendSuccess(() -> Component.literal(msg), true);
                    return 1;
                } else {
                    confirmMap.remove(uuid);
                    String timeout = AllClearConfig.CHUNK_MODE_TIMEOUT.get();
                    source.sendFailure(Component.literal(timeout));
                    return 0;
                }
            } else {
                int currentTick = source.getServer().getTickCount();
                confirmMap.put(uuid, currentTick);
                String confirmMsg = AllClearConfig.CHUNK_MODE_CONFIRM.get()
                        .replace("{seconds}", String.valueOf(confirmSeconds));
                source.sendSuccess(() -> Component.literal(confirmMsg), true);
                return 0;
            }
        } else {
            AllClearConfig.CHUNK_SCAN_MODE.set(mode);
            AllClearConfig.SPEC.save();
            String msg = AllClearConfig.CHUNK_MODE_CHANGED.get().replace("{mode}", mode.name());
            source.sendSuccess(() -> Component.literal(msg), true);
            return 1;
        }
    }

    // 解析时间参数辅助方法
    private static int parseTimeArgument(String arg) throws CommandSyntaxException {
        if (arg.matches("\\d+")) {
            return Integer.parseInt(arg); // ticks
        } else if (arg.matches("\\d+t")) {
            int count = Integer.parseInt(arg.substring(0, arg.length() - 1));
            return count * AllClearConfig.AUTO_SILENCE_CHECK_INTERVAL_TICKS.get(); // 检测周期倍数
        } else if (arg.matches("\\d+s")) {
            int seconds = Integer.parseInt(arg.substring(0, arg.length() - 1));
            return seconds * 20; // 秒转tick
        } else if (arg.matches("\\d+d")) {
            int days = Integer.parseInt(arg.substring(0, arg.length() - 1));
            return days * 24000; // 游戏天转tick
        } else if (arg.matches("\\d+td")) {
            int realDays = Integer.parseInt(arg.substring(0, arg.length() - 2));
            return realDays * 86400 * 20; // 现实天转tick (86400秒/天 * 20 tick/秒)
        } else {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
    }
}
