package ink.magma.zthcoinbank.Coin;

import ink.magma.zthcoinbank.Coin.Error.NoCoinSetInConfigException;
import ink.magma.zthcoinbank.Coin.Error.NoEnoughItemException;
import ink.magma.zthcoinbank.ZthCoinBank;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CoinItemManager {

    /**
     * 获取物品堆数组中包含的货币和数量
     *
     * @param itemStacks 物品堆数组
     * @return 货币 -> 数量 的 Map, Coin 的数量可能为 0, 因此，Map 总是不为空.
     */
    public static Map<Coin, Integer> getCoinAmountMapInItems(ItemStack[] itemStacks) throws NoCoinSetInConfigException {
        // 结果 Map
        Map<Coin, Integer> map = new HashMap<>();
        // 获取所有货币的集合
        Collection<Coin> coinCollection = ZthCoinBank.coinManager.getAllCoins().values();
        // 遍历查找每种货币
        for (Coin coin : coinCollection) {
            int coinAmount = countItemInItems(itemStacks, coin.getMaterial());
            map.put(coin, coinAmount);
        }
        return map;
    }

    /**
     * 获取所有物品中货币的价值
     *
     * @param itemStacks 物品列表
     * @return 价值
     */
    public static double getAllItemsValue(ItemStack[] itemStacks) throws NoCoinSetInConfigException {
        BigDecimal totalWorth = new BigDecimal("0");

        Map<Coin, Integer> coinAmountMap = getCoinAmountMapInItems(itemStacks);

        // 计算每种货币分别的总价值
        for (Coin coin : coinAmountMap.keySet()) {
            // 使用 BigDecimal 计算，避免精度问题
            // 这里的算法实际上就是 totalWorth = totalWorth + coin.getWorthValue() * coinAmountMap.get(coin)
            BigDecimal coinValue = BigDecimal.valueOf(coin.getWorthValue());
            BigDecimal thisCoinTotalValue = coinValue.multiply(BigDecimal.valueOf(coinAmountMap.get(coin)));

            totalWorth = totalWorth.add(thisCoinTotalValue);
        }

        return totalWorth.floatValue();
    }

    /**
     * 获取物品堆中某物品所含的数量
     *
     * @param itemStacks 被寻找的物品列表
     * @param itemToFind 寻找的列表
     * @return 找到的数量
     */
    public static int countItemInItems(ItemStack[] itemStacks, ItemStack itemToFind) {
        int count = 0;

        for (ItemStack item : itemStacks) {
            if (item != null && item.isSimilar(itemToFind)) {
                count += item.getAmount();
            }
        }

        return count;
    }


    public static void takeItem(ItemStack[] itemStacks, ItemStack itemToTake, int amount) throws NoEnoughItemException {
        // 先确保背包中含有指定数量的物品
        int totalAmountInInv = getAmountInInv(itemStacks, itemToTake);

        // 数量不足以全部扣除
        if (totalAmountInInv < amount) {
            throw new NoEnoughItemException("物品栏中只有 " + totalAmountInInv + " 个物品, 不足以扣除.");
        }

        int remainAmount = amount;
        // 遍历物品栏中的物品，尝试扣除
        for (ItemStack item : itemStacks) {
            if (item != null && item.isSimilar(itemToTake)) {
                // 如果 当前物品堆的数量 > 剩余扣除的总量，扣除需要扣除的数量
                if (item.getAmount() > remainAmount) {
                    item.setAmount(item.getAmount() - remainAmount);
                    remainAmount = 0;
                    break;
                }
                // 如果 当前物品堆的数量 <= 剩余扣除的总量，则直接删除此物品，然后进入下一个循环
                else if (item.getAmount() <= remainAmount) {
                    remainAmount = remainAmount - item.getAmount();
                    item.setAmount(0);
                }
            }
            if (remainAmount == 0) break;
        }
        if (remainAmount > 0) {
            throw new NoEnoughItemException("CoinItemManager.takeItem() 实际扣除物品时遍历了全部物品格仍未扣完");
        }
    }

    public static int getAmountInInv(ItemStack[] itemStacks, ItemStack itemToTake) {
        int totalAmountInInv = 0;
        for (ItemStack item : itemStacks) {
            if (item != null && item.isSimilar(itemToTake)) {
                totalAmountInInv = totalAmountInInv + item.getAmount();
            }
        }
        return totalAmountInInv;
    }

    /**
     * 获取物品栏中还能放下多少某物品
     *
     * @param inventory 物品栏
     * @param itemToAdd 物品
     * @return 可以最多容得下的数量
     */
    public static int getRemainSpace(Inventory inventory, ItemStack itemToAdd) {
        int remainSpace = 0;
        // 计算空格子的容量
        int maxStackSize = itemToAdd.getMaxStackSize();
        if (maxStackSize == -1) {
            throw new RuntimeException("itemStack.getMaxStackSize() is -1");
        }

        // 在这里，我们只考虑玩家的快捷栏和主物品栏 (9*3)
        // index 0-8 代表快捷栏，9-35 代表主物品栏

        for (int i = 0; i <= 35; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR || item.getAmount() == 0) {
                remainSpace = remainSpace + maxStackSize;
            } else if (item.isSimilar(itemToAdd)) {
                remainSpace = remainSpace + (maxStackSize - item.getAmount());
            }
        }

        return remainSpace;
    }
}
