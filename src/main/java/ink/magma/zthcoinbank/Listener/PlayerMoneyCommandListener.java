package ink.magma.zthcoinbank.Listener;

import ink.magma.zthcoinbank.Coin.CoinManager;
import ink.magma.zthcoinbank.ZthCoinBank;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.*;

public class PlayerMoneyCommandListener implements Listener {
    List<String> moneyCommands = List.of("/money", "/bal", "/balance", "/eco", "/economy");

    Map<UUID, Long> lastDisplay = new HashMap<>();

    private final org.bukkit.plugin.Plugin plugin;

    public PlayerMoneyCommandListener(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseMoneyCommand(PlayerCommandPreprocessEvent event) {
        // 获取玩家输入的命令
        String command = event.getMessage();

        // 检查是否是金币相关命令
        if (moneyCommands.contains(command)) {
            // 使用延迟任务来执行
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Long lastTime = lastDisplay.get(event.getPlayer().getUniqueId());

                // 检查是否超过15秒冷却时间
                if (lastTime == null || lastTime + 1000 * 15 < System.currentTimeMillis()) {
                    // 发送一个空消息（用于清空聊天框）
                    event.getPlayer().spigot().sendMessage(new TextComponent(""));

                    // 获取金币信息并发送
                    List<TextComponent> bankTui = CoinManager.getBankTui();
                    for (TextComponent line : bankTui) {
                        event.getPlayer().spigot().sendMessage(line);
                    }

                    // 更新玩家的显示时间
                    lastDisplay.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
                }
            }, 2L); // 延迟2个tick（100ms）
        }
    }
}
