package ink.magma.zthcoinbank.listener;

import ink.magma.zthcoinbank.coin.Coin;
import ink.magma.zthcoinbank.coin.CoinManager;
import ink.magma.zthcoinbank.coin.error.NoCoinSetInConfigException;
import ink.magma.zthcoinbank.ZthCoinBank;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerCoinHoldListener implements Listener {

    private boolean isACoin(ItemStack itemStack) {
        try {
            Coin coin = ZthCoinBank.coinManager.getCoinByItemStack(itemStack);
            return coin != null;
        } catch (NoCoinSetInConfigException ignored) {
            return false;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHoldCoin(PlayerItemHeldEvent event) {
        ItemStack itemInMainHand = event.getPlayer().getInventory().getItem(event.getNewSlot());

        if (isACoin(itemInMainHand)) {
            event.getPlayer().sendActionBar(Component.text("点按右键即可打开掌上莉亚中心银行"));
        }
    }

    Map<UUID, Long> lastDisplay = new HashMap<>();

    @EventHandler
    public void onPlayerClickCoin(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getItem() == null) return;

        ItemStack itemInHand = event.getItem();

        if (isACoin(itemInHand)) {
            Long lastTime = lastDisplay.get(event.getPlayer().getUniqueId());
            if (lastTime == null || lastTime + 1000 * 8 < System.currentTimeMillis()) {
                CoinManager.getBankTui().forEach(line -> event.getPlayer().sendMessage(line));
                lastDisplay.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
            }
        }
    }
}
