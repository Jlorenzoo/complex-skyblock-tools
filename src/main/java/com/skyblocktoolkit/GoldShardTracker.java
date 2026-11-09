package com.skyblocktoolkit;

import com.moneybooster.BoosterStatsManager;
import com.moneybooster.RateTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.HoeItem;

/** Gold shards are estimated item value, never a cash or booster ledger entry. */
public final class GoldShardTracker {
    private static final GoldShardEarnings earnings=new GoldShardEarnings();
    private static long previousCount=-1;
    private GoldShardTracker() {}
    public static void reset(){earnings.reset(System.currentTimeMillis());previousCount=-1;}
    public static void chat(String text){
        earnings.chat(GoldShardEarnings.parseReward(text),System.currentTimeMillis(),RateTracker.getActiveTestId());
        deliver();
    }
    public static void tick(Minecraft mc){
        if(mc.player==null)return;
        long count=0;
        var inventory=mc.player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            var stack=inventory.getItem(i);
            String name=stack.getHoverName().getString().replaceAll("§.","").replaceAll("[^\\p{L}\\p{N} ]","").strip();
            if(name.matches("(?i)Gold Shards?"))count+=stack.getCount();
        }
        long now=System.currentTimeMillis();
        boolean farming=mc.gui.screen()==null&&mc.player.getMainHandItem().getItem() instanceof HoeItem
            &&(CropHarvestTracker.isActive(now)||mc.options.keyAttack.isDown());
        if(previousCount>=0&&count>previousCount&&ToolkitMod.FISHING.trackingOniShards(mc)){
            ItemStatsTracker.oniShards(count-previousCount,now);
        }
        previousCount=count;
        earnings.observe(count,farming,now,RateTracker.getActiveTestId());
        deliver();
    }
    private static void deliver(){for(var gain:earnings.drain()){
        RateTracker.onShardGain(gain.count(),gain.at(),gain.testId());
        ItemStatsTracker.shards(gain.count(),gain.at());
    }}
    public static double value(long count){var c=ToolkitConfig.get();return c.includeGoldShards?count*c.goldShardPrice:0;}
    public static double hourlyValue(){var c=ToolkitConfig.get();return c.includeGoldShards?earnings.perHour(System.currentTimeMillis())*c.goldShardPrice:0;}
    public static boolean ready(){return earnings.ready(System.currentTimeMillis());}
    public static String label(){return "Gold shards: "+earnings.total()+" · "+CropHarvestTracker.format(earnings.perHour(System.currentTimeMillis()))+"/h";}
}
