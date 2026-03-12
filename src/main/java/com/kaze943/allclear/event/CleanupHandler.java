package com.kaze943.allclear.event;

import com.kaze943.allclear.config.AllClearConfig;
import com.kaze943.allclear.util.CleanupUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class CleanupHandler {
    // 定时清理相关
    private int ticksPassed = 0;

    // TPS 检测相关
    private long lastCheckTime = System.currentTimeMillis();
    private int tickCounter = 0;
    private double currentTPS = 20.0;
    private long lastLowTpsCleanup = 0; // 毫秒

    public CleanupHandler() {
        // 构造函数中不再访问配置
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        // 每秒更新 TPS 并检查低 TPS 保护
        tickCounter++;
        long now = System.currentTimeMillis();
        if (now - lastCheckTime >= 1000) {
            currentTPS = tickCounter * 1000.0 / (now - lastCheckTime);
            tickCounter = 0;
            lastCheckTime = now;

            double threshold = AllClearConfig.LOW_TPS_THRESHOLD.get();
            int cooldownSec = AllClearConfig.AUTO_CLEAN_COOLDOWN_SECONDS.get();
            if (threshold > 0 && currentTPS < threshold && (now - lastLowTpsCleanup) > cooldownSec * 1000L) {
                int cleaned = CleanupUtil.cleanupAll(server);
                lastLowTpsCleanup = now;
                if (cleaned > 0) {
                    String msg = AllClearConfig.LOW_TPS_MESSAGE.get()
                            .replace("{count}", String.valueOf(cleaned))
                            .replace("{threshold}", String.valueOf(threshold));
                    server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
                }
            }
        }

        // 定时清理逻辑
        ticksPassed++;
        int interval = AllClearConfig.CLEAN_INTERVAL_TICKS.get();
        int ticksLeft = interval - ticksPassed;

        // 倒计时广播（每秒）
        if (ticksLeft > 0 && ticksLeft % 20 == 0) {
            int secondsLeft = ticksLeft / 20;
            // 直接从配置读取列表并检查是否包含
            if (AllClearConfig.COUNTDOWN_TIMES.get().contains(secondsLeft)) {
                String msg = AllClearConfig.BROADCAST_BEFORE.get().replace("{seconds}", String.valueOf(secondsLeft));
                server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
            }
        }

        // 清理时刻
        if (ticksPassed >= interval) {
            int cleaned = CleanupUtil.cleanupAll(server);
            ticksPassed = 0;
            if (cleaned > 0 && AllClearConfig.BROADCAST_AFTER.get() != null) {
                String msg = AllClearConfig.BROADCAST_AFTER.get().replace("{count}", String.valueOf(cleaned));
                server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
            }
        }
    }
}
