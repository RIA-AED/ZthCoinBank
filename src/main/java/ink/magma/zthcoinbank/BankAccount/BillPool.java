package ink.magma.zthcoinbank.BankAccount;

import ink.magma.zthcoinbank.ZthCoinBank;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class BillPool {
    /**
     * 对银行账户提交一份账单 (异步)
     *
     * @param bill 金额，可为负数
     */
    public void submitNewBill(double bill) {
        if (!ZthCoinBank.configuration.getBoolean("settings.bank_account.enable")) return;

        BukkitScheduler scheduler = ZthCoinBank.INSTANCE.getServer().getScheduler();
        scheduler.runTask(ZthCoinBank.INSTANCE, () -> {
            if (bill == 0) return;

            EconomyResponse response;

            if (bill < 0) {
                response = ZthCoinBank.economy.withdrawPlayer(getBankAccount(), -bill);
            } else {
                response = ZthCoinBank.economy.depositPlayer(getBankAccount(), bill);
            }

            if (!response.transactionSuccess()) {
                ZthCoinBank.INSTANCE.getLogger().warning(MessageFormat.format(
                        "对银行账户进行经济操作时失败, 将在稍后重试。计划余额变动: {0}, 交易错误信息: {1}",
                        bill, response.errorMessage
                ));

                double billPool = ZthCoinBank.configuration.getDouble("settings.bank_account.bill_pool");
                ZthCoinBank.configuration.set("settings.bank_account.bill_pool", billPool + bill);
                ZthCoinBank.INSTANCE.saveConfig();
            } else {
                ZthCoinBank.INSTANCE.getLogger().info(MessageFormat.format(
                        "成功对银行账户 {0} 的余额操作 {1} .",
                        getBankAccountName(), bill
                ));
            }
        });
    }


    /**
     * 运行一次银行账户的账单同步
     */
    public void runBillSync() {
        if (!ZthCoinBank.configuration.getBoolean("settings.bank_account.enable")) return;

        double billPool = ZthCoinBank.configuration.getDouble("settings.bank_account.bill_pool");
        if (billPool != 0) {
            EconomyResponse response;

            if (billPool < 0) {
                response = ZthCoinBank.economy.withdrawPlayer(getBankAccount(), -billPool);
            } else {
                response = ZthCoinBank.economy.depositPlayer(getBankAccount(), billPool);
            }

            if (response.transactionSuccess()) {
                ZthCoinBank.INSTANCE.getLogger().info(MessageFormat.format(
                        "成功对银行账户 {0} 的余额操作 {1} .",
                        getBankAccountName(), billPool
                ));

                ZthCoinBank.configuration.set("settings.bank_account.bill_pool", 0);
                ZthCoinBank.INSTANCE.saveConfig();
            } else {
                ZthCoinBank.INSTANCE.getLogger().warning(MessageFormat.format(
                        "对银行账户进行经济操作时失败, 将在稍后重试。账单池目前为: {0}, 交易错误信息: {1}",
                        billPool, response.errorMessage
                ));
            }
        }
    }

    @NotNull
    private static OfflinePlayer getBankAccount() {
        return ZthCoinBank.INSTANCE.getServer().getOfflinePlayer(getBankAccountName());
    }

    public static String getBankAccountName() throws RuntimeException {
        String accountName = ZthCoinBank.configuration.getString("settings.bank_account.account_name");
        if (accountName == null) {
            throw new RuntimeException("未在 config.yml 中配置 account_name!");
        }
        return accountName;
    }
}
