package com.kaze943.allclear;

import com.kaze943.allclear.config.AllClearConfig;
import com.kaze943.allclear.command.AllClearCommand;
import com.kaze943.allclear.event.CleanupHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Allclear.MODID)
public class Allclear {
    public static final String MODID = "allclear";

    public Allclear(IEventBus modEventBus, ModContainer modContainer) {
        // 注册配置文件（通用配置）
        modContainer.registerConfig(ModConfig.Type.COMMON, AllClearConfig.SPEC);

        // 注册事件监听器
        NeoForge.EVENT_BUS.register(new CleanupHandler());
        NeoForge.EVENT_BUS.register(AllClearCommand.class);
    }
}
