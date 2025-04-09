package ink.magma.zthcoinbank.coin;
import ink.magma.zthcoinbank.ZthCoinBank;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class Coin {
    private final String coinName;
    private final ItemStack material;
    private final Double value;

    public Coin(String coinName, ItemStack itemStack, Double coinValue) {
        this.coinName = coinName;
        itemStack.setAmount(1);
        this.material = itemStack;
        this.value = coinValue;
    }

    /**
     * 返回 Coin 的 displayName 或货币类型名
     */
    public String getDisplayName() {
        return ZthCoinBank.coinManager.getCoinDisplayName(coinName);
    }

    public String getCoinName() {
        return coinName;
    }

    public ItemStack getMaterial() {
        return material;
    }

    public Double getWorthValue() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Coin thatCoin)) return false;
        if (object == this) return true;

        if (!Objects.equals(this.coinName, thatCoin.coinName)) return false;
        if (!Objects.equals(this.material, thatCoin.material)) return false;
        return Double.compare(this.value, thatCoin.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.coinName, this.material, this.value);
    }
}
