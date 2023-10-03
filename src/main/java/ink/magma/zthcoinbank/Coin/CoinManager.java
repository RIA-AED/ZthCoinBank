package ink.magma.zthcoinbank.Coin;

import ink.magma.zthcoinbank.Coin.Error.NoCoinSetInConfigException;
import ink.magma.zthcoinbank.Coin.Error.UnknowCoinNameException;
import ink.magma.zthcoinbank.ZthCoinBank;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinManager {
    Configuration configuration;

    public CoinManager() throws NoCoinSetInConfigException {
        configuration = ZthCoinBank.configuration;
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
