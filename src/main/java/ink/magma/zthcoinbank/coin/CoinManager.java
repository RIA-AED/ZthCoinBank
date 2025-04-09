package ink.magma.zthcoinbank.coin;

import ink.magma.zthcoinbank.coin.error.NoCoinSetInConfigException;
import ink.magma.zthcoinbank.coin.error.UnknowCoinNameException;
import ink.magma.zthcoinbank.ZthCoinBank;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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

    public static List<TextComponent> getBankTui() {
        List<TextComponent> components = new ArrayList<>();
        Map<String, Coin> coins;

        try {
            coins = ZthCoinBank.coinManager.getAllCoins();
        } catch (NoCoinSetInConfigException e) {
            return components;
        }

        // 创建头部信息
        TextComponent head = new TextComponent("§b§l---===§e RIA中央银行 §aFST分行 §b§l===---");
        components.add(head);

        // 创建取现组件
        TextComponent withdraw = new TextComponent("§6$ 取现 > ");
        for (String coinName : ZthCoinBank.coinManager.getCoinNames()) {
            Coin coin = coins.get(coinName);
            TextComponent coinComponent = new TextComponent("§6" + coin.getDisplayName());
            coinComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/coin withdraw " + coin.getCoinName()));
            coinComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("输入此币种的数量")}));
            withdraw.addExtra(coinComponent);
            withdraw.addExtra(" ");
        }
        components.add(withdraw);

        // 创建存现组件
        TextComponent deposit = new TextComponent("§7$ 存现 > ");
        for (String coinName : ZthCoinBank.coinManager.getCoinNames()) {
            Coin coin = coins.get(coinName);
            TextComponent coinComponent = new TextComponent("§7" + coin.getDisplayName());
            coinComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/coin deposit " + coin.getCoinName()));
            coinComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("输入此币种的数量")}));
            deposit.addExtra(coinComponent);
            deposit.addExtra(" ");
        }
        components.add(deposit);

        // 创建背包全部存现组件
        TextComponent depositAll = new TextComponent("§a$ 背包全部存现 > ");
        TextComponent depositAllButton = new TextComponent("§a§l"+ZthCoinBank.getText("button"));
        depositAllButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/coin deposit-all"));
        depositAllButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("点击存现背包中所有货币")}));
        depositAll.addExtra(depositAllButton);
        components.add(depositAll);

        return components;
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

    public String getCoinDisplayName(String coinName){
        String displayName = configuration.getString("display_names." + coinName);
        return displayName==null||displayName.isEmpty()?"未知币种":displayName;
    }

    public List<String> getCoinDisplayNames() {
        List<String> displayNames= new ArrayList<>(3);
        for(var name:getCoinNames()){
            displayNames.add(getCoinDisplayName(name));
        }
        return displayNames;
    }

    public List<String> getCoinNames() {
        return List.of("ingot", "nugget", "powder");

    }

    public Boolean isCoinName(String coinName) {
        return getCoinNames().contains(coinName);
    }
}
