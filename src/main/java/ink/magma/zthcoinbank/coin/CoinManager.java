package ink.magma.zthcoinbank.coin;

import ink.magma.zthcoinbank.coin.error.NoCoinSetInConfigException;
import ink.magma.zthcoinbank.coin.error.UnknowCoinNameException;
import ink.magma.zthcoinbank.ZthCoinBank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinManager {
    Configuration configuration;

    public CoinManager() throws NoCoinSetInConfigException {
        configuration = ZthCoinBank.configuration;
    }

    public static List<Component> getBankTui() {
        MiniMessage mini = MiniMessage.miniMessage();
        Map<String, Coin> coins;

        try {
            coins = ZthCoinBank.coinManager.getAllCoins();
        } catch (NoCoinSetInConfigException e) {
            return new ArrayList<>();
        }

        Component head = mini.deserialize("<b><gradient:#f38181:#fce38a>---===</gradient> <color:#f9ca24>RIA Central bank</color> <gradient:#fce38a:#f38181>===---</gradient></b>");
        Component withdraw = mini.deserialize("<color:#ff85c0>$ 取现</color> > ");
        Component deposit = mini.deserialize("<color:#ffd666>$ 存现</color> > ");
        Component depositAll = mini.deserialize("<color:#faad14>$ 背包全部存现</color> > ");

        for (Coin coin : coins.values()) {
            withdraw = withdraw.append(mini.deserialize(MessageFormat.format(
                    "<click:suggest_command:''/coin withdraw {0} ''><hover:show_text:'输入此币种的数量'>{1}</hover></click>  ",
                    coin.getCoinName(),
                    mini.serialize(coin.getDisplayName())
            )));

            deposit = deposit.append(mini.deserialize(MessageFormat.format(
                    "<click:suggest_command:''/coin deposit {0} ''><hover:show_text:'输入此币种的数量'>{1}</hover></click>  ",
                    coin.getCoinName(),
                    mini.serialize(coin.getDisplayName())
            )));

            depositAll = depositAll.append(mini.deserialize(MessageFormat.format(
                    "<click:run_command:''/coin deposit {0}''><hover:show_text:'点击存现背包中所有此币种的货币'>{1}</hover></click>  ",
                    coin.getCoinName(),
                    mini.serialize(coin.getDisplayName())
            )));
        }

        depositAll = depositAll.append(mini.deserialize(
                "<click:run_command:'/coin deposit-all'><color:#faad14><hover:show_text:'点击存现背包中所有货币'>[存入所有币种]</hover></color></click>"
        ));

        return List.of(head, withdraw, deposit, depositAll);
    }

    public void saveCoinItem(ItemStack coin, String coinName) throws UnknowCoinNameException {
        if (!isCoinName(coinName)) {
            throw new UnknowCoinNameException("不是合法的货币名: " + coinName);
        }
        ItemStack clone = coin.clone();
        clone.setAmount(1);
        configuration.set("items." + coinName, clone);
        ZthCoinBank.INSTANCE.saveConfig();
    }

    public void setCoinValue(String coinName, double amount) throws UnknowCoinNameException {
        if (!isCoinName(coinName)) {
            throw new UnknowCoinNameException("不是合法的货币名: " + coinName);
        }

        configuration.set("settings.coin_value." + coinName, amount);
        ZthCoinBank.INSTANCE.saveConfig();
    }

    /**
     * 从配置文件读取并实例化所有货币
     *
     * @return 货币 Map
     */
    @Nonnull
    public Map<String, Coin> getAllCoins() throws NoCoinSetInConfigException {
        Map<String, Coin> coinMap = new HashMap<>();

        for (String coinName : getCoinNames()) {
            ItemStack itemStack = configuration.getItemStack("items." + coinName);
            double coinValue = configuration.getDouble("settings.coin_value." + coinName, -1);

            if (itemStack != null && coinValue >= 0) {
                coinMap.put(coinName, new Coin(coinName, itemStack, coinValue));
            } else {
                throw new NoCoinSetInConfigException("货币未配置");
            }
        }

        return coinMap;
    }

    @Nullable
    public Coin getCoinByItemStack(ItemStack itemStack) throws NoCoinSetInConfigException {
        Map<String, Coin> coins = getAllCoins();

        Coin whatCoinIsThis = null;
        for (String coin : coins.keySet()) {
            Coin thisCoin = coins.get(coin);
            boolean equals = thisCoin.getMaterial().isSimilar(itemStack);
            if (equals) {
                whatCoinIsThis = thisCoin;
                break;
            }
        }

        return whatCoinIsThis;
    }

    public static List<String> getCoinNames() {
        return List.of("ingot", "nugget", "powder");

    }

    public static Boolean isCoinName(String coinName) {
        return getCoinNames().contains(coinName);
    }


}
