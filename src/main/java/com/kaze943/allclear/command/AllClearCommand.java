package com.kaze943.allclear.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.kaze943.allclear.util.CleanupUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class AllClearCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("allclear")
                .requires(source -> source.hasPermission(2)) // OP 权限
                .then(Commands.literal("now")
                        .executes(AllClearCommand::runNow))
        );
    }

    private static int runNow(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int cleaned = CleanupUtil.cleanupAll(source.getServer());
        source.sendSuccess(() -> Component.literal("已清理 " + cleaned + " 个掉落物（黑名单已过滤）"), true);
        return 1;
    }
}