package ink.magma.zthcoinbank;

import ink.magma.zthcoinbank.bankaccount.BillPool;
import ink.magma.zthcoinbank.coin.CoinManager;
import ink.magma.zthcoinbank.coin.error.NoCoinSetInConfigException;
import ink.magma.zthcoinbank.command.CoinCommand;
import ink.magma.zthcoinbank.listener.PlayerCoinHoldListener;
import ink.magma.zthcoinbank.listener.PlayerMoneyCommandListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

public final class ZthCoinBank extends JavaPlugin {
    public static ZthCoinBank INSTANCE = null;
    public static Configuration configuration = null;

    public static Economy economy = null;
    public static CoinManager coinManager = null;
    public static BillPool billPool = null;
    public static BukkitCommandHandler commandHandler = null;

    @Override
    public void onEnable() {
        // Plugin startup logic
        INSTANCE = this;

        // init vault
        boolean tryRegister = createVaultEcoAPI();
        if (!tryRegister) return;
        getLogger().info("Vault 初始化成功");

        // init config file
        saveDefaultConfig();
        configuration = getConfig();

        try {
            coinManager = new CoinManager();
        } catch (NoCoinSetInConfigException e) {
            getLogger().warning("货币配置不正确或未配置! 必须为配置货币后才能正常使用.");
        }

        // command
        commandHandler = BukkitCommandHandler.create(this);
        commandHandler.register(new CoinCommand());
        commandHandler.registerBrigadier();

        // event
        Bukkit.getPluginManager().registerEvents(new PlayerCoinHoldListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoneyCommandListener(), this);

        // 账单池 & 定时任务
        billPool = new BillPool();
        getServer().getScheduler().runTaskTimer(this, () -> billPool.runBillSync(), 1200, 1200);

    }

    private boolean createVaultEcoAPI() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("插件启动时无法找到 Vault 的安装，插件将不会运行");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("插件启动时无法找到 Vault 的安装，插件将不会运行");
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
