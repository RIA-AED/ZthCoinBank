package ink.magma.zthcoinbank.Command;

import ink.magma.zthcoinbank.Coin.Coin;
import ink.magma.zthcoinbank.Coin.CoinItemManager;
import ink.magma.zthcoinbank.Coin.CoinManager;
import ink.magma.zthcoinbank.Coin.Error.NoCoinSetInConfigException;
import ink.magma.zthcoinbank.Coin.Error.NoEnoughItemException;
import ink.magma.zthcoinbank.Coin.Error.UnknowCoinNameException;
import ink.magma.zthcoinbank.ZthCoinBank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Command("coin")
public class CoinCommand {
    public CoinCommand() {
        ZthCoinBank.commandHandler.getAutoCompleter().registerSuggestion(
                "coinType",
                (args, sender, command) -> ZthCoinBank.coinManager.getAllCoins().keySet()
        );
    }

    @Subcommand("help")
    public void coinHelpCommand(CommandSender sender) {
        Map<String, String> help = new HashMap<>();
        help.put("bank", "显示银行界面");
        help.put("set item <货币类型>", "将主手上的物品设置某种货币");
        help.put("set value <价值>", "设置某种货币单个的价值");
        help.put("check hand", "检测手中的物品是否是一种货币");
        help.put("check inventory", "检测物品栏中有多少货币及其价值");
        help.put("withdraw <货币类型> <数量>", "取现 - 将余额兑换为实体货币");
        help.put("deposit <货币类型> [数量]", "存现 - 将实体货币兑换为余额");
        help.put("deposit-all", "全部存现 - 将背包中所有实体货币兑换为余额");


        Component helpComponent = Component.text("ZthCoinBank - 指令帮助文件").color(NamedTextColor.WHITE).appendSpace().appendSpace();
        helpComponent = helpComponent.append(Component.text("By MagmaBlock").color(NamedTextColor.GRAY)).appendNewline();

        for (String command : help.keySet()) {
            Component thisCmdHelp = Component.text("/coin " + command).color(NamedTextColor.WHITE).appendSpace()
                    .append(Component.text(help.get(command)).color(NamedTextColor.GRAY));

            helpComponent = helpComponent.appendNewline();
            helpComponent = helpComponent.append(thisCmdHelp);
        }

        sender.sendMessage(helpComponent);
    }

    @Subcommand("bank")
    @CommandPermission("zth.coinbank.use")
    public void bandCommand(Player player) {
        CoinManager.getBankTui().forEach(player::sendMessage);
    }

    @Subcommand("set item")
    @AutoComplete("@coinType")
    @CommandPermission("zth.coinbank.admin")
    public void setCoinCommand(Player sender, @Named("货币类型") String coinType) {
        ItemStack mainHandItem = sender.getInventory().getItemInMainHand();

        if (mainHandItem.getType() == Material.AIR || mainHandItem.getAmount() == 0) {
            sender.sendMessage("不能将空气设置为货币!");
            return;
        }

        try {
            ZthCoinBank.coinManager.saveCoinItem(mainHandItem, coinType);
        } catch (UnknowCoinNameException e) {
            sender.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            return;
        }

        Component itemName = Component.text("(未设置名称的物品)");
        if (mainHandItem.getItemMeta().hasDisplayName()) {
            Component displayName = mainHandItem.getItemMeta().displayName();
            if (displayName != null) itemName = displayName;
        }
        sender.sendMessage(
                Component.text("设置成功, 已将货币").color(NamedTextColor.GRAY)
                        .appendSpace()
                        .append(Component.text(coinType).color(NamedTextColor.WHITE))
                        .appendSpace()
                        .append(Component.text("设置为").color(NamedTextColor.GRAY))
                        .appendSpace()
                        .append(itemName)
        );
    }

    @Subcommand("set value")
    @CommandPermission("zth.coinbank.admin")
    public void setValueCommand(Player sender, @Named("货币类型") String coinType, @Named("价值") double value) {
        try {
            ZthCoinBank.coinManager.setCoinValue(coinType, value);
        } catch (UnknowCoinNameException e) {
            sender.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            return;
        }

        sender.sendMessage(
                Component.text("设置成功, 现在一份").color(NamedTextColor.GRAY)
                        .appendSpace()
                        .append(Component.text(coinType).color(NamedTextColor.WHITE))
                        .appendSpace()
                        .append(Component.text("的价值为").color(NamedTextColor.GRAY))
                        .appendSpace()
                        .append(Component.text(value).color(NamedTextColor.WHITE))
        );
    }

    @Subcommand("check hand")
    public void checkItemCommand(Player sender) {
        try {
            ItemStack itemInMainHand = sender.getInventory().getItemInMainHand();
            Coin coin = ZthCoinBank.coinManager.getCoinByItemStack(itemInMainHand);

            if (coin == null) {
                sender.sendMessage(Component.text("未能将您主手中的物品匹配为任何货币.").color(NamedTextColor.GRAY));
                return;
            }

            BigDecimal value = BigDecimal.valueOf(coin.getWorthValue()).multiply(BigDecimal.valueOf(itemInMainHand.getAmount()));

            Component msg = Component.text("您主手中的物品识别为").color(NamedTextColor.GRAY)
                    .appendSpace()
                    .append(Component.text(coin.getCoinName()).color(NamedTextColor.WHITE))
                    .appendSpace()
                    .append(Component.text("价值").color(NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text(value.doubleValue()).color(NamedTextColor.WHITE));
            sender.sendMessage(msg);


        } catch (NoCoinSetInConfigException e) {
            sender.sendMessage(e.getMessage());
        }
    }

    @Subcommand("check inventory")
    public void checkInventoryWorth(Player sender) {
        ItemStack[] contents = sender.getInventory().getContents();

        try {
            Map<Coin, Integer> coinAmount = CoinItemManager.getCoinAmountMapInItems(contents);
            double allItemsValue = CoinItemManager.getAllItemsValue(contents);

            Component msg = coinAmount.isEmpty() ? Component.text("背包中没有任何货币") : Component.text("当前背包内有以下货币:");
            msg = msg.color(NamedTextColor.GRAY).appendNewline();
            for (Coin coin : coinAmount.keySet()) {
                Component coinMsgLine = Component.text("- 货币").color(NamedTextColor.GRAY)
                        .appendSpace()
                        .append(Component.text(coin.getCoinName()).color(NamedTextColor.WHITE))
                        .appendSpace()
                        .append(Component.text("数量").color(NamedTextColor.GRAY))
                        .appendSpace()
                        .append(Component.text(coinAmount.get(coin)).color(NamedTextColor.WHITE))
                        .appendSpace()
                        .append(Component.text("个"))
                        .appendNewline();
                msg = msg.append(coinMsgLine);
            }
            Component totalMsg = Component.text("总价值").color(NamedTextColor.GRAY)
                    .appendSpace()
                    .append(Component.text(Math.round(allItemsValue * 100.0) / 100.0).color(NamedTextColor.WHITE));
            msg = msg.append(totalMsg);

            sender.sendMessage(msg);
        } catch (NoCoinSetInConfigException e) {
            sender.sendMessage(Component.text("管理员未配置货币, 无法使用").color(NamedTextColor.RED));
        }
    }

    // 存款 - 兑换实体货币到余额
    @Subcommand("deposit")
    @AutoComplete("@coinType")
    @CommandPermission("zth.coinbank.use")
    public void depositCommand(Player player, @Named("货币类型") String coinType, @Named("存款数量") @Optional Integer amount) {
        ItemStack[] contents = player.getInventory().getContents();

        if (!CoinManager.isCoinName(coinType)) {
            player.sendMessage(Component.text("未知的货币名").color(NamedTextColor.RED));
            return;
        }

        Coin coin;
        Map<Coin, Integer> coinAmountMap;

        try {
            coin = ZthCoinBank.coinManager.getAllCoins().get(coinType);
            coinAmountMap = CoinItemManager.getCoinAmountMapInItems(contents);
        } catch (NoCoinSetInConfigException e) {
            player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            return;
        }

        // 如果输入了非法数量
        if (amount != null && amount <= 0) {
            player.sendMessage(Component.text("至少存入的货币数量为 1.").color(NamedTextColor.RED));
            return;
        }

        // 判断应该扣多少物品
        Integer takeAmount;
        if (amount != null) {
            takeAmount = amount;
        } else {
            takeAmount = coinAmountMap.get(coin);
        }

        // 判断物品栏中的物品够不够存入
        if (coinAmountMap.get(coin) < takeAmount) {
            player.sendMessage(Component.text("物品栏中只有 " + coinAmountMap.get(coin) + " 个物品, 不足以扣除.").color(NamedTextColor.RED));
            return;
        }

        // 记录一次之前的经济
        double beforeBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

        // 精确计算 货币量 * 货币单价
        BigDecimal totalValue = BigDecimal.valueOf(takeAmount).multiply(BigDecimal.valueOf(coin.getWorthValue()));

        // 给钱
        EconomyResponse response = ZthCoinBank.economy.depositPlayer(player, totalValue.doubleValue());

//        Component detailMsg = Component.text("尝试存入").color(NamedTextColor.GRAY).appendSpace()
//                .append(Component.text(takeAmount).color(NamedTextColor.WHITE).appendSpace())
//                .append(Component.text("个").color(NamedTextColor.GRAY)).appendSpace()
//                .append(getDisplayName(coin)).appendSpace()
//                .append(Component.text("(单个价值").color(NamedTextColor.GRAY)).appendSpace()
//                .append(Component.text(coin.getWorthValue()).color(NamedTextColor.WHITE))
//                .append(Component.text(")").color(NamedTextColor.GRAY)).appendSpace()
//                .append(Component.text("合计: ").color(NamedTextColor.GRAY)).appendSpace()
//                .append(Component.text(totalValue.doubleValue()).color(NamedTextColor.WHITE)).appendSpace();
//
//        player.sendMessage(detailMsg);

        if (response.transactionSuccess()) {
            // 尝试扣除物品
            try {
                CoinItemManager.takeItem(contents, coin.getMaterial(), takeAmount);
            } catch (NoEnoughItemException e) {
                player.sendMessage(Component.text("发生内部错误，请联系管理员：存款时实际扣除货币物品少于预估数量；" + e.getMessage()).color(NamedTextColor.RED));
                return;
            }

            double newBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

            Component successMsg = Component.text("存现成功. 已存入").color(NamedTextColor.GRAY).appendSpace()
                    .append(Component.text(takeAmount).color(NamedTextColor.WHITE)).appendSpace()
                    .append(Component.text("个").color(NamedTextColor.GRAY)).appendSpace()
                    .append(coin.getDisplayName()).appendSpace()
                    .append(Component.text("，余额变化: ").color(NamedTextColor.GRAY)).appendSpace()
                    .append(Component.text(beforeBal).color(NamedTextColor.WHITE)).appendSpace()
                    .append(Component.text("->").color(NamedTextColor.GRAY)).appendSpace()
                    .append(Component.text(newBal).color(NamedTextColor.WHITE));

            player.sendMessage(successMsg);

            // 对银行账户扣款
            ZthCoinBank.billPool.submitNewBill(-totalValue.doubleValue());
        } else {
            player.sendMessage(Component.text("入账失败, 经济系统返回错误: ").color(NamedTextColor.RED)
                    .append(Component.text(response.errorMessage))
            );
        }


    }

    // 存款全部
    @Subcommand("deposit-all")
    @CommandPermission("zth.coinbank.use")
    public void depositAllCommand(Player player) {
        ItemStack[] contents = player.getInventory().getContents();

        Map<Coin, Integer> coinAmountMap;

        try {
            coinAmountMap = CoinItemManager.getCoinAmountMapInItems(contents);
        } catch (NoCoinSetInConfigException e) {
            player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            return;
        }

        // 记录一次之前的经济
        double beforeBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

        // 所有货币的总价值
        BigDecimal totalValue = BigDecimal.valueOf(0);

        for (Coin coin : coinAmountMap.keySet()) {
            // 此货币在背包中的数量
            Integer amount = coinAmountMap.get(coin);
            // 数量 * 单个价值
            BigDecimal thisCoinValue = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(coin.getWorthValue()));
            totalValue = totalValue.add(thisCoinValue);
        }


        // 如果背包中一分钱也没有
        if (totalValue.equals(BigDecimal.valueOf(0))) {
            player.sendMessage(Component.text("身上一分钱也没有了喔.").color(NamedTextColor.RED));
            return;
        }

        // 给钱
        EconomyResponse response = ZthCoinBank.economy.depositPlayer(player, totalValue.doubleValue());

//        Component detailMsg = Component.text("尝试存入").color(NamedTextColor.GRAY).appendSpace()
//                .append(Component.text(takeAmount).color(NamedTextColor.WHITE).appendSpace())
//                .append(Component.text("个").color(NamedTextColor.GRAY)).appendSpace()
//                .append(getDisplayName(coin)).appendSpace()
//                .append(Component.text("(单个价值").color(NamedTextColor.GRAY)).appendSpace()
//                .append(Component.text(coin.getWorthValue()).color(NamedTextColor.WHITE))
//                .append(Component.text(")").color(NamedTextColor.GRAY)).appendSpace()
//                .append(Component.text("合计: ").color(NamedTextColor.GRAY)).appendSpace()
//                .append(Component.text(totalValue.doubleValue()).color(NamedTextColor.WHITE)).appendSpace();
//
//        player.sendMessage(detailMsg);

        if (response.transactionSuccess()) {
            // 尝试扣除物品
            try {
                for (Coin coin : coinAmountMap.keySet()) {
                    CoinItemManager.takeItem(contents, coin.getMaterial(), coinAmountMap.get(coin));
                }
            } catch (NoEnoughItemException e) {
                player.sendMessage(Component.text("发生内部错误，请联系管理员：存款时实际扣除货币物品少于预估数量；" + e.getMessage()).color(NamedTextColor.RED));
                return;
            }

            double newBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;


            Component successMsg = Component.text("已存入").color(NamedTextColor.GRAY).appendSpace();

            for (Coin coin : coinAmountMap.keySet()) {
                // 不显示没找到的货币
                if (coinAmountMap.get(coin) == 0) continue;

                successMsg = successMsg.append(
                        Component.text(coinAmountMap.get(coin)).color(NamedTextColor.WHITE).appendSpace()
                                .append(Component.text("个").color(NamedTextColor.GRAY)).appendSpace()
                                .append(coin.getDisplayName()).append(Component.text(",")).appendSpace()
                );
            }

            successMsg = successMsg.appendNewline().append(
                    Component.text("余额变化: ").color(NamedTextColor.GRAY).appendSpace()
                            .append(Component.text(beforeBal).color(NamedTextColor.WHITE)).appendSpace()
                            .append(Component.text("->").color(NamedTextColor.GRAY)).appendSpace()
                            .append(Component.text(newBal).color(NamedTextColor.WHITE))
            );

            player.sendMessage(successMsg);

            // 对银行账户扣款
            ZthCoinBank.billPool.submitNewBill(-totalValue.doubleValue());
        } else {
            player.sendMessage(Component.text("入账失败, 经济系统返回错误: ").color(NamedTextColor.RED)
                    .append(Component.text(response.errorMessage))
            );
        }
    }

    // 取款 - 兑换余额为实体货币
    @Subcommand("withdraw")
    @AutoComplete("@coinType")
    @CommandPermission("zth.coinbank.use")
    public void withdrawCommand(Player player, @Named("货币类型") String coinType, @Named("取款数量") @Optional Integer amount) {
        if (!CoinManager.isCoinName(coinType)) {
            player.sendMessage(Component.text("未知的货币名").color(NamedTextColor.RED));
            return;
        }

        // 取现数量校验
        if (amount == null) {
            player.sendMessage(Component.text("您必须指定要取现的货币数量.").color(NamedTextColor.RED));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(Component.text("至少取现的货币数量为 1.").color(NamedTextColor.RED));
            return;
        }

        Coin coin;

        try {
            coin = ZthCoinBank.coinManager.getAllCoins().get(coinType);
        } catch (NoCoinSetInConfigException e) {
            player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            return;
        }

        int remainSpace = CoinItemManager.getRemainSpace(player.getInventory(), coin.getMaterial());
        // 空间不足
        if (amount > remainSpace) {
            Component noSpaceMsg = Component.text("您的背包只能容下").appendSpace()
                    .append(Component.text(remainSpace)).appendSpace()
                    .append(Component.text("个")).appendSpace()
                    .append(coin.getDisplayName()).appendSpace()
                    .append(Component.text(", 而您指定了")).appendSpace()
                    .append(Component.text(amount)).appendSpace()
                    .append(Component.text("个.")).color(NamedTextColor.RED);

            player.sendMessage(noSpaceMsg);
            return;
        }

        // 记录一次之前的经济
        double beforeBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

        // 精确计算 货币量 * 货币单价
        BigDecimal totalValue = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(coin.getWorthValue()));

        // 扣钱
        EconomyResponse response = ZthCoinBank.economy.withdrawPlayer(player, totalValue.doubleValue());

//        Component detailMsg = Component.text("尝试取出").color(NamedTextColor.GRAY).appendSpace()
//                .append(Component.text(amount).color(NamedTextColor.WHITE).appendSpace())
//                .append(Component.text("个").color(NamedTextColor.GRAY)).appendSpace()
//                .append(getDisplayName(coin)).appendSpace()
//                .append(Component.text("(单个价值").color(NamedTextColor.GRAY)).appendSpace()
//                .append(Component.text(coin.getWorthValue()).color(NamedTextColor.WHITE))
//                .append(Component.text(")").color(NamedTextColor.GRAY)).appendSpace()
//                .append(Component.text("合计: ").color(NamedTextColor.GRAY)).appendSpace()
//                .append(Component.text(totalValue.doubleValue()).color(NamedTextColor.WHITE)).appendSpace();
//
//        player.sendMessage(detailMsg);

        if (response.transactionSuccess()) {
            // 给予物品
            ItemStack newCoins = coin.getMaterial().clone();
            newCoins.setAmount(amount);

            HashMap<Integer, ItemStack> overFlowingItems = player.getInventory().addItem(newCoins);
            // 发生意外情况：背包可用空间在给予物品前减少
            if (!overFlowingItems.isEmpty()) {
                for (Integer i : overFlowingItems.keySet()) {
                    player.getWorld().dropItem(player.getLocation(), overFlowingItems.get(i));
                }
                player.sendMessage(Component.text("给予物品到物品栏时出现意外，您的物品栏已满，多余的物品已经掉落在地面上。").color(NamedTextColor.RED));
            }

            double newBal = Math.round(ZthCoinBank.economy.getBalance(player) * 100.0) / 100.0;

            Component successMsg = Component.text("取现成功. 已取出").color(NamedTextColor.GRAY).appendSpace()
                    .append(Component.text(amount).color(NamedTextColor.WHITE)).appendSpace()
                    .append(Component.text("个").color(NamedTextColor.GRAY)).appendSpace()
                    .append(coin.getDisplayName()).appendSpace()
                    .append(Component.text("，余额变化: ").color(NamedTextColor.GRAY)).appendSpace()
                    .append(Component.text(beforeBal).color(NamedTextColor.WHITE)).appendSpace()
                    .append(Component.text("->").color(NamedTextColor.GRAY)).appendSpace()
                    .append(Component.text(newBal).color(NamedTextColor.WHITE));

            player.sendMessage(successMsg);

            // 对银行账户加款
            ZthCoinBank.billPool.submitNewBill(totalValue.doubleValue());
        } else {
            player.sendMessage(Component.text("扣账失败, 经济系统返回错误: ").color(NamedTextColor.RED)
                    .append(Component.text(response.errorMessage))
            );
        }


    }
}
