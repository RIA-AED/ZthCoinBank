package ink.magma.zthcoinbank.Listener;

import ink.magma.zthcoinbank.Coin.Coin;
import ink.magma.zthcoinbank.Coin.CoinManager;
import ink.magma.zthcoinbank.Coin.Error.NoCoinSetInConfigException;
import ink.magma.zthcoinbank.ZthCoinBank;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
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
        Player player = event.getPlayer();
        ItemStack itemInMainHand = player.getInventory().getItem(event.getNewSlot());

        if (isACoin(itemInMainHand)) {
            sendActionBar(player, "点按右键即可打开掌上莉亚中心银行");
        }
    }

    private void sendActionBar(Player player, String message) {
        // Spigot 1.12+ 支持 Action Bar，需要使用 JSON 格式
        String actionBarText = ChatColor.translateAlternateColorCodes('&', message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder(actionBarText).create());
    }

    Map<UUID, Long> lastDisplay = new HashMap<>();

    @EventHandler
    public void onPlayerClickCoin(PlayerInteractEvent event) {
        // 检查是否是主手操作
        if (event.getHand() != EquipmentSlot.HAND) return;
        // 检查玩家手中是否有物品
        if (event.getItem() == null) return;

        ItemStack itemInHand = event.getItem();

        // 检查是否是金币
        if (isACoin(itemInHand)) {
            // 获取玩家的唯一标识
            UUID playerUniqueId = event.getPlayer().getUniqueId();
            Long lastTime = lastDisplay.get(playerUniqueId);

            // 检查是否超过8秒冷却时间
            if (lastTime == null || lastTime + 1000 * 8 < System.currentTimeMillis()) {
                List<TextComponent> bankTui = CoinManager.getBankTui();
                for (TextComponent component : bankTui) {
                    // 将TextComponent转换为字符串并发送
                    event.getPlayer().sendMessage(component.toLegacyText());
                }
                // 更新玩家的显示时间
                lastDisplay.put(playerUniqueId, System.currentTimeMillis());
            }
        }
    }
}
