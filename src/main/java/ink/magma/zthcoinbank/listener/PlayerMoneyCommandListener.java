package ink.magma.zthcoinbank.listener;

import ink.magma.zthcoinbank.coin.CoinManager;
import ink.magma.zthcoinbank.ZthCoinBank;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerMoneyCommandListener implements Listener {
    List<String> moneyCommands = List.of("/money", "/bal", "/balance", "/eco", "/economy");

    Map<UUID, Long> lastDisplay = new HashMap<>();

    @EventHandler
    public void onPlayerUseMoneyCommand(PlayerCommandPreprocessEvent event) {
        if (moneyCommands.contains(event.getMessage())) {
            ZthCoinBank.INSTANCE.getServer().getScheduler().runTaskLater(ZthCoinBank.INSTANCE, () -> {
                Long lastTime = lastDisplay.get(event.getPlayer().getUniqueId());

                if (lastTime == null || lastTime + 1000 * 15 < System.currentTimeMillis()) {
                    event.getPlayer().sendMessage(Component.empty());

                    CoinManager.getBankTui().forEach(line -> event.getPlayer().sendMessage(line));
                    lastDisplay.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
                }
            }, 2);
        }
    }
}
