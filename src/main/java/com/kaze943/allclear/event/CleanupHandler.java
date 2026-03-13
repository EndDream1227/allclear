package com.kaze943.allclear.event;

import com.kaze943.allclear.config.AllClearConfig;
import com.kaze943.allclear.util.CleanupUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class CleanupHandler {
    private int ticksPassed = 0;
    private long lastCheckTime = System.currentTimeMillis();
    private int tickCounter = 0;
    private double currentTPS = 20.0;
    private long lastLowTpsCleanup = 0;

    private boolean modEnabled = true;
    private boolean manuallySilenced = false;
    private boolean autoSilenced = false;
    private boolean sleepModeEnabled = false;      // 静默模式总开关（默认关闭）
    private int autoSilenceTimer = 0;
    private int totalItemCount = 0;

    // 新增字段
    private int autoSilenceCheckTimer = 0;          // 距离下次自动检测的 tick
    private int forceNoSilenceTimer = 0;            // 强制不静默剩余 tick

    public CleanupHandler() {}

    // Getter/Setter
    public void setModEnabled(boolean enabled) { this.modEnabled = enabled; }
    public boolean isModEnabled() { return modEnabled; }
    public void setManuallySilenced(boolean silenced) { this.manuallySilenced = silenced; }
    public boolean isManuallySilenced() { return manuallySilenced; }
    public void setSleepModeEnabled(boolean enabled) {
        this.sleepModeEnabled = enabled;
        if (!enabled) {
            this.manuallySilenced = false;
            this.autoSilenced = false;
            this.autoSilenceTimer = 0;
            this.forceNoSilenceTimer = 0;
            this.autoSilenceCheckTimer = 0;
        }
    }
    public boolean isSleepModeEnabled() { return sleepModeEnabled; }
    public void setForceNoSilenceTimer(int ticks) { this.forceNoSilenceTimer = ticks; }

    private String getBroadcastBefore() {
        String mode = AllClearConfig.TEXT_MODE.get();
        if ("subtitle".equals(mode)) {
            String sub = AllClearConfig.SUBTITLE_BROADCAST_BEFORE.get();
            if (sub != null && !sub.isEmpty()) return sub;
        }
        String dialog = AllClearConfig.DIALOG_BROADCAST_BEFORE.get();
        if (dialog != null && !dialog.isEmpty()) return dialog;
        return AllClearConfig.BROADCAST_BEFORE.get();
    }

    private String getBroadcastAfter() {
        String mode = AllClearConfig.TEXT_MODE.get();
        if ("subtitle".equals(mode)) {
            String sub = AllClearConfig.SUBTITLE_BROADCAST_AFTER.get();
            if (sub != null && !sub.isEmpty()) return sub;
        }
        String dialog = AllClearConfig.DIALOG_BROADCAST_AFTER.get();
        if (dialog != null && !dialog.isEmpty()) return dialog;
        return AllClearConfig.BROADCAST_AFTER.get();
    }

    private String getLowTpsMessage() {
        String mode = AllClearConfig.TEXT_MODE.get();
        if ("subtitle".equals(mode)) {
            String sub = AllClearConfig.SUBTITLE_LOW_TPS_MESSAGE.get();
            if (sub != null && !sub.isEmpty()) return sub;
        }
        String dialog = AllClearConfig.DIALOG_LOW_TPS_MESSAGE.get();
        if (dialog != null && !dialog.isEmpty()) return dialog;
        return AllClearConfig.DIALOG_LOW_TPS_MESSAGE.get();
    }

    private void sendMessage(MinecraftServer server, String message) {
        if (message == null || message.isEmpty()) return;
        String mode = AllClearConfig.TEXT_MODE.get();
        if ("subtitle".equals(mode)) {
            server.getPlayerList().getPlayers().forEach(player -> {
                player.displayClientMessage(Component.literal(message), true);
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            });
        } else {
            server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        int currentTick = server.getTickCount();
        long now = System.currentTimeMillis();

        // 物品总数定期更新
        int updateInterval = AllClearConfig.ITEM_COUNT_UPDATE_INTERVAL_TICKS.get();
        if (currentTick % updateInterval == 0) {
            totalItemCount = CleanupUtil.countTotalItems(server);
        }

        // 强制不静默计时器递减
        if (forceNoSilenceTimer > 0) {
            forceNoSilenceTimer--;
        }

        // TPS 检测
        tickCounter++;
        if (now - lastCheckTime >= 1000) {
            currentTPS = tickCounter * 1000.0 / (now - lastCheckTime);
            tickCounter = 0;
            lastCheckTime = now;

            double threshold = AllClearConfig.LOW_TPS_THRESHOLD.get();
            int forceThreshold = AllClearConfig.FORCE_CLEAR_ITEM_THRESHOLD.get();
            int cooldownSec = AllClearConfig.AUTO_CLEAN_COOLDOWN_SECONDS.get();

            if (threshold > 0 && currentTPS < threshold && (now - lastLowTpsCleanup) > cooldownSec * 1000L) {
                if (forceThreshold > 0 && totalItemCount >= forceThreshold) {
                    int cleaned = CleanupUtil.cleanupAll(server);
                    lastLowTpsCleanup = now;
                    if (cleaned > 0) {
                        String msg = getLowTpsMessage()
                                .replace("{count}", String.valueOf(cleaned))
                                .replace("{threshold}", String.valueOf(threshold));
                        sendMessage(server, msg);
                    }
                }
            }
        }

        // === 自动静默检测（仅在总开关开启且强制不静默计时器为0时执行） ===
        if (sleepModeEnabled && forceNoSilenceTimer <= 0) {
            if (autoSilenceCheckTimer <= 0) {
                int autoThreshold = AllClearConfig.AUTO_SILENCE_ITEM_THRESHOLD.get();
                if (autoThreshold > 0) {
                    boolean wasSilenced = autoSilenced;
                    if (totalItemCount < autoThreshold) {
                        if (!autoSilenced) {
                            autoSilenced = true;
                            autoSilenceTimer = AllClearConfig.AUTO_SILENCE_DURATION_SECONDS.get() * 20;
                            // 发送进入静默提示
                            String msg = AllClearConfig.AUTO_SILENCE_ON_MESSAGE.get()
                                    .replace("{count}", String.valueOf(totalItemCount));
                            sendMessage(server, msg);
                        }
                    } else {
                        if (autoSilenced) {
                            autoSilenced = false;
                            autoSilenceTimer = 0;
                            // 发送解除静默提示（物品数量恢复）
                            String msg = AllClearConfig.AUTO_SILENCE_OFF_MESSAGE.get()
                                    .replace("{count}", String.valueOf(totalItemCount));
                            sendMessage(server, msg);
                        }
                    }
                }
                autoSilenceCheckTimer = AllClearConfig.AUTO_SILENCE_CHECK_INTERVAL_TICKS.get();
            } else {
                autoSilenceCheckTimer--;
            }

            if (autoSilenced) {
                autoSilenceTimer--;
                if (autoSilenceTimer <= 0) {
                    autoSilenced = false;
                    // 发送解除静默提示（持续时间结束）
                    String msg = AllClearConfig.AUTO_SILENCE_OFF_MESSAGE.get()
                            .replace("{count}", String.valueOf(totalItemCount));
                    sendMessage(server, msg);
                }
            }
        } else {
            autoSilenceCheckTimer = 0;
            if (!sleepModeEnabled && autoSilenced) {
                autoSilenced = false;
                autoSilenceTimer = 0;
                // 发送解除静默提示（总开关关闭）
                String msg = AllClearConfig.AUTO_SILENCE_OFF_MESSAGE.get()
                        .replace("{count}", String.valueOf(totalItemCount));
                sendMessage(server, msg);
            }
        }

        // 是否执行定时清理
        boolean shouldRun = modEnabled && !manuallySilenced && !autoSilenced;
        if (!shouldRun) {
            ticksPassed = 0;
            return;
        }

        ticksPassed++;
        int interval = AllClearConfig.CLEAN_INTERVAL_TICKS.get();
        int ticksLeft = interval - ticksPassed;

        if (ticksLeft > 0 && ticksLeft % 20 == 0) {
            int secondsLeft = ticksLeft / 20;
            if (AllClearConfig.COUNTDOWN_TIMES.get().contains(secondsLeft)) {
                String msg = getBroadcastBefore().replace("{seconds}", String.valueOf(secondsLeft));
                sendMessage(server, msg);
            }
        }

        if (ticksPassed >= interval) {
            int cleaned = CleanupUtil.cleanupAll(server);
            ticksPassed = 0;
            if (cleaned > 0) {
                String msg = getBroadcastAfter().replace("{count}", String.valueOf(cleaned));
                sendMessage(server, msg);
            }
        }
    }
}