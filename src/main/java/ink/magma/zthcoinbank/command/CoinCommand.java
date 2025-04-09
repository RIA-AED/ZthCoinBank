package ink.magma.zthcoinbank.command;

import ink.magma.zthcoinbank.coin.Coin;
import ink.magma.zthcoinbank.coin.CoinItemManager;
import ink.magma.zthcoinbank.coin.CoinManager;
import ink.magma.zthcoinbank.coin.error.NoCoinSetInConfigException;
import ink.magma.zthcoinbank.coin.error.NoEnoughItemException;
import ink.magma.zthcoinbank.coin.error.UnknowCoinNameException;
import ink.magma.zthcoinbank.ZthCoinBank;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该命令仅能由玩家使用!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            coinHelpCommand(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                coinHelpCommand(player);
                break;
            case "bank":
                if (!player.hasPermission("zth.coinbank.use")) {
                    player.sendMessage(ChatColor.RED + "你没有使用该命令的权限!");
                    return true;
                }
                bankCommand(player);
                break;
            case "set":
                if (!player.hasPermission("zth.coinbank.admin")) {
                    player.sendMessage(ChatColor.RED + "你没有使用该命令的权限!");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("用法: /coin set <item/value> <coinType/value>");
                    return true;
                }
                if ("item".equalsIgnoreCase(args[1])) {
                    setCoinCommand(player, args[2]);
                } else if ("value".equalsIgnoreCase(args[1])) {
                    try {
                        double value = Double.parseDouble(args[3]);
                        setValueCommand(player, args[2], value);
                    } catch (NumberFormatException e) {
                        player.sendMessage("无效的数值");
                    }
                } else {
                    player.sendMessage("无效的子命令 - 使用 'item' 或者 'value'.");
                }
                break;
            case "check":
                if (args.length < 2) {
                    player.sendMessage("用法: /coin check <hand/inventory>");
                    return true;
                }
                if ("hand".equalsIgnoreCase(args[1])) {
                    checkItemCommand(player);
                } else if ("inventory".equalsIgnoreCase(args[1])) {
                    checkInventoryWorth(player);
                } else {
                    player.sendMessage("无效的子命令 - 使用 'hand' 或者 'inventory'.");
                }
                break;
            case "withdraw":
                if (!player.hasPermission("zth.coinbank.use")) {
                    player.sendMessage(ChatColor.RED + "你没有使用该命令的权限!");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("用法: /coin withdraw <货币类型> <amount>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    withdrawCommand(player, args[1], amount);
                } catch (NumberFormatException e) {
                    player.sendMessage("无效的数量!");
                }
                break;
            case "deposit":
                if (!player.hasPermission("zth.coinbank.use")) {
                    player.sendMessage(ChatColor.RED + "你没有使用该命令的权限!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("用法: /coin deposit <货币类型> [数量]");
                    return true;
                }
                Integer depositAmount = null;
                if (args.length > 2) {
                    try {
                        depositAmount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("无效的数量!");
                        return true;
                    }
                }
                depositCommand(player, args[1], depositAmount);
                break;
            case "deposit-all":
                if (!player.hasPermission("zth.coinbank.use")) {
                    player.sendMessage(ChatColor.RED + "你没有使用该命令的权限!");
                    return true;
                }
                depositAllCommand(player);
                break;
            default:
                player.sendMessage("未知的子命令 - 使用 /coin help 以查看所有的子命令");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return new ArrayList<>();

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("help");
            completions.add("bank");
            completions.add("set");
            completions.add("check");
            completions.add("withdraw");
            completions.add("deposit");
            completions.add("deposit-all");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "set":
                    completions.add("item");
                    completions.add("value");
                    break;
                case "check":
                    completions.add("hand");
                    completions.add("inventory");
                    break;
            }
        } else if (args.length == 3 && "set".equalsIgnoreCase(args[0])) {
            try {
                completions.addAll(ZthCoinBank.coinManager.getAllCoins().keySet());
            } catch (NoCoinSetInConfigException e) {
                // Ignore
            }
        }
        return completions;
    }


    public CoinCommand() {
        // 注册自动补全
        ZthCoinBank.commandHandler.getAutoCompleter().registerSuggestion(
                "coinType",
                (args, sender, command) -> ZthCoinBank.coinManager.getAllCoins().keySet()
        );
    }

    public void coinHelpCommand(CommandSender sender) {
        Map<String, String> help = new HashMap<>();
        help.put("bank", "显示银行界面");
        help.put("set item <货币类型>", "将主手上的物品设置为某种货币");
        help.put("set value <价值>", "设置某种货币单个的价值");
        help.put("check hand", "检测手中的物品是否是一种货币");
        help.put("check inventory", "检测物品栏中有多少货币及其价值");
        help.put("withdraw <货币类型> [数量]", "取现 - 将余额兑换为实体货币");
        help.put("deposit <货币类型> [数量]", "存现 - 将实体货币兑换为余额");
        help.put("deposit-all", "全部存现 - 将背包中所有实体货币兑换为余额");

        String helpComponent = ChatColor.WHITE + "ZthCoinBank - 指令帮助文件\n" +
                ChatColor.GRAY + "By MagmaBlock\n";

        for (String command : help.keySet()) {
            helpComponent += "/coin " + command + " " + help.get(command) + "\n";
        }

        sender.sendMessage(helpComponent);
    }

    public void bankCommand(Player player) {
        List<TextComponent> bankTui = CoinManager.getBankTui();
        for (TextComponent component : bankTui) {
            player.spigot().sendMessage(component); // 使用spigot()方法发送TextComponent
        }
    }

    public void setCoinCommand(Player sender, String coinType) {
        if (!sender.hasPermission("zth.coinbank.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用这个命令！");
            return;
        }
        ItemStack mainHandItem = sender.getInventory().getItemInMainHand();

        if (mainHandItem.getType() == Material.AIR || mainHandItem.getAmount() == 0) {
            sender.sendMessage("不能将空气设置为货币!");
            return;
        }

        try {
            ZthCoinBank.coinManager.saveCoinItem(mainHandItem, coinType);
        } catch (UnknowCoinNameException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return;
        }

        String itemName = "[未命名物品]";
        ItemMeta meta = mainHandItem.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            itemName = meta.getDisplayName();
        }
        sender.sendMessage(
                ChatColor.GRAY + "设置成功, 已将货币 " +
                        ChatColor.WHITE + coinType +
                        ChatColor.GRAY + " 设置为 " +
                        itemName
        );
    }

    public void setValueCommand(Player sender, String coinType, double value) {
        if (!sender.hasPermission("zth.coinbank.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用这个命令！");
            return;
        }
        try {
            ZthCoinBank.coinManager.setCoinValue(coinType, value);
        } catch (UnknowCoinNameException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return;
        }

        sender.sendMessage(
                ChatColor.GRAY + "设置成功, 现在一份 " +
                        ChatColor.WHITE + coinType +
                        ChatColor.GRAY + " 的价值为 " +
                        ChatColor.WHITE + value
        );
    }

    public void checkItemCommand(Player sender) {
        try {
            ItemStack itemInMainHand = sender.getInventory().getItemInMainHand();
            Coin coin = ZthCoinBank.coinManager.getCoinByItemStack(itemInMainHand);

            if (coin == null) {
                sender.sendMessage(ChatColor.GRAY + "未能将您主手中的物品匹配为任何货币.");
                return;
            }

            BigDecimal value = BigDecimal.valueOf(coin.getWorthValue()).multiply(BigDecimal.valueOf(itemInMainHand.getAmount()));

            sender.sendMessage(
                    ChatColor.GRAY + "您主手中的物品识别为 " +
                            ChatColor.WHITE + coin.getCoinName() +
                            ChatColor.GRAY + " 价值 " +
                            ChatColor.WHITE + value.doubleValue()
            );

        } catch (NoCoinSetInConfigException e) {
            sender.sendMessage(e.getMessage());
        }
    }

    public void checkInventoryWorth(Player sender) {
        ItemStack[] contents = sender.getInventory().getContents();

        try {
            Map<Coin, Integer> coinAmount = CoinItemManager.getCoinAmountMapInItems(contents);
            double allItemsValue = CoinItemManager.getAllItemsValue(contents);

            String msg = coinAmount.isEmpty() ? ChatColor.GRAY + "背包中没有任何货币" : ChatColor.GRAY + "当前背包内有以下货币:\n";
            for (Coin coin : coinAmount.keySet()) {
                msg += "- 货币 " + ChatColor.WHITE + coin.getCoinName() +
                        ChatColor.GRAY + " 数量 " +
                        ChatColor.WHITE + coinAmount.get(coin) +
                        ChatColor.GRAY + " 个\n";
            }
            msg += ChatColor.GRAY + "总价值 " + ChatColor.WHITE + Math.round(allItemsValue * 100.0) / 100.0;

            sender.sendMessage(msg);
        } catch (NoCoinSetInConfigException e) {
            sender.sendMessage(ChatColor.RED + "管理员未配置货币, 无法使用");
        }
    }

    public void depositCommand(Player player, String coinType, Integer amount) {
        ItemStack[] contents = player.getInventory().getContents();

        if (!ZthCoinBank.coinManager.isCoinName(coinType)) {
            player.sendMessage(ChatColor.RED + "未知的货币名");
            return;
        }

        Coin coin;
        Map<Coin, Integer> coinAmountMap;

        try {
            coin = ZthCoinBank.coinManager.getAllCoins().get(coinType);
            coinAmountMap = CoinItemManager.getCoinAmountMapInItems(contents);
        } catch (NoCoinSetInConfigException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
            return;
        }

        if (amount != null && amount <= 0) {
            player.sendMessage(ChatColor.RED + "至少存入的货币数量为 1.");
            return;
        }

        Integer takeAmount;
        if (amount != null) {
            takeAmount = amount;
        } else {
            takeAmount = coinAmountMap.get(coin);
        }

        if (coinAmountMap.get(coin) < takeAmount) {
            player.sendMessage(ChatColor.RED + "物品栏中只有 " + coinAmountMap.get(coin) + " 个物品, 不足以扣除.");
            return;
        }

        double beforeBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

        BigDecimal totalValue = BigDecimal.valueOf(takeAmount).multiply(BigDecimal.valueOf(coin.getWorthValue()));

        EconomyResponse response = ZthCoinBank.economy.depositPlayer(player, totalValue.doubleValue());

        if (response.transactionSuccess()) {
            try {
                CoinItemManager.takeItem(contents, coin.getMaterial(), takeAmount);
            } catch (NoEnoughItemException e) {
                player.sendMessage(ChatColor.RED + "发生内部错误，请联系管理员：存款时实际扣除货币物品少于预估数量；" + e.getMessage());
                return;
            }

            double newBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

            player.sendMessage(
                    ChatColor.GRAY + "存现成功. 已存入 " +
                            ChatColor.WHITE + takeAmount +
                            ChatColor.GRAY + " 个 " +
                            coin.getDisplayName() +
                            ChatColor.GRAY + "，余额变化: " +
                            ChatColor.WHITE + beforeBal +
                            ChatColor.GRAY + " -> " +
                            ChatColor.WHITE + newBal
            );

            ZthCoinBank.billPool.submitNewBill(-totalValue.doubleValue());
        } else {
            player.sendMessage(ChatColor.RED + "入账失败, 经济系统返回错误: " + response.errorMessage);
        }
    }

    public void depositAllCommand(Player player) {
        ItemStack[] contents = player.getInventory().getContents();

        Map<Coin, Integer> coinAmountMap;

        try {
            coinAmountMap = CoinItemManager.getCoinAmountMapInItems(contents);
        } catch (NoCoinSetInConfigException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
            return;
        }

        double beforeBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

        BigDecimal totalValue = BigDecimal.valueOf(0);

        for (Coin coin : coinAmountMap.keySet()) {
            Integer amount = coinAmountMap.get(coin);
            BigDecimal thisCoinValue = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(coin.getWorthValue()));
            totalValue = totalValue.add(thisCoinValue);
        }

        if (totalValue.equals(BigDecimal.valueOf(0))) {
            player.sendMessage(ChatColor.RED + "身上一分钱也没有了喔.");
            return;
        }

        EconomyResponse response = ZthCoinBank.economy.depositPlayer(player, totalValue.doubleValue());

        if (response.transactionSuccess()) {
            try {
                for (Coin coin : coinAmountMap.keySet()) {
                    CoinItemManager.takeItem(contents, coin.getMaterial(), coinAmountMap.get(coin));
                }
            } catch (NoEnoughItemException e) {
                player.sendMessage(ChatColor.RED + "发生内部错误，请联系管理员：存款时实际扣除货币物品少于预估数量；" + e.getMessage());
                return;
            }

            double newBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

            StringBuilder successMsg = new StringBuilder(ChatColor.GRAY + "已存入 ");
            for (Coin coin : coinAmountMap.keySet()) {
                if (coinAmountMap.get(coin) == 0) continue;

                successMsg.append(ChatColor.WHITE).append(coinAmountMap.get(coin)).append(ChatColor.GRAY).append(" 个 ").append(coin.getDisplayName()).append(", ");
            }

            successMsg.append("\n" + ChatColor.GRAY + "余额变化: " + ChatColor.WHITE).append(beforeBal).append(ChatColor.GRAY).append(" -> ").append(ChatColor.WHITE).append(newBal);

            player.sendMessage(successMsg.toString());

            ZthCoinBank.billPool.submitNewBill(-totalValue.doubleValue());
        } else {
            player.sendMessage(ChatColor.RED + "入账失败, 经济系统返回错误: " + response.errorMessage);
        }
    }

    public void withdrawCommand(Player player, String coinType, Integer amount) {
        if (!ZthCoinBank.coinManager.isCoinName(coinType)) {
            player.sendMessage(ChatColor.RED + "未知的货币名");
            return;
        }

        if (amount == null) {
            player.sendMessage(ChatColor.RED + "您必须指定要取现的货币数量.");
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "至少取现的货币数量为 1.");
            return;
        }

        Coin coin;

        try {
            coin = ZthCoinBank.coinManager.getAllCoins().get(coinType);
        } catch (NoCoinSetInConfigException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
            return;
        }

        int remainSpace = CoinItemManager.getRemainSpace(player.getInventory(), coin.getMaterial());
        if (amount > remainSpace) {
            player.sendMessage(
                    ChatColor.RED + "您的背包只能容下 " +
                            remainSpace +
                            " 个 " +
                            coin.getDisplayName() +
                            ", 而您指定了 " +
                            amount +
                            " 个."
            );
            return;
        }

        double beforeBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

        BigDecimal totalValue = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(coin.getWorthValue()));

        EconomyResponse response = ZthCoinBank.economy.withdrawPlayer(player, totalValue.doubleValue());

        if (response.transactionSuccess()) {
            ItemStack newCoins = coin.getMaterial().clone();
            newCoins.setAmount(amount);

            HashMap<Integer, ItemStack> overFlowingItems = player.getInventory().addItem(newCoins);
            if (!overFlowingItems.isEmpty()) {
                for (Integer i : overFlowingItems.keySet()) {
                    player.getWorld().dropItem(player.getLocation(), overFlowingItems.get(i));
                }
                player.sendMessage(
                        ChatColor.RED + "给予物品到物品栏时出现意外，您的物品栏已满，多余的物品已经掉落在地面上。"
                );
            }

            double newBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

            player.sendMessage(
                    ChatColor.GRAY + "取现成功. 已取出 " +
                            ChatColor.WHITE + amount +
                            ChatColor.GRAY + " 个 " +
                            coin.getDisplayName() +
                            ChatColor.GRAY + "，余额变化: " +
                            ChatColor.WHITE + beforeBal +
                            ChatColor.GRAY + " -> " +
                            ChatColor.WHITE + newBal
            );

            ZthCoinBank.billPool.submitNewBill(totalValue.doubleValue());
        } else {
            player.sendMessage(
                    (response.errorMessage.equalsIgnoreCase("Insufficient balance!"))?
                            ChatColor.RED + "身上一分钱也没有了喔.":
                            ChatColor.RED + "扣账失败, 经济系统返回错误: " + response.errorMessage
            );
        }
    }
}