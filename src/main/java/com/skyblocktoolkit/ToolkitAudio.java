package com.skyblocktoolkit;

import com.fishingpings.FishingSignals;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

public final class ToolkitAudio {
    public enum Sample {
        GLASS("Glass tap", "studio_glass"),
        PLUCK("Soft pluck", "studio_pluck"),
        COINS("Coin flick", "studio_coins"),
        CLICK("Latch click", "studio_click"),
        SNAP("Line cut", "studio_snap"),
        FALL("Falling notes", "studio_fall"),
        SOFT("Soft pulse", "studio_soft"),
        RISE("Rising pluck", "studio_rise"),
        REWARD("Reward chime", "studio_reward"),
        CHARGE_LOW("Charge I", "studio_charge_low"),
        CHARGE_HIGH("Charge II", "studio_charge_high"),
        CHARGE_FULL("Charge complete", "studio_charge_full"),
        PLING("Pling"),
        LEVEL_UP("Level Up"),
        ORB("Orb Pickup"),
        AMETHYST("Amethyst Chime"),
        BELL("Bell"),
        CLASSIC("Classic");

        public final String label;
        private final String asset;

        Sample(String label) { this(label, null); }
        Sample(String label, String asset) {
            this.label = label;
            this.asset = asset;
        }
    }

    public enum Cue {
        BITE("Fishing bite"),
        READY("Ready to reel", Sample.PLUCK, Sample.GLASS, Sample.SOFT, Sample.CLASSIC),
        SNAP("Snapped line", Sample.SNAP, Sample.CLICK, Sample.FALL, Sample.CLASSIC),
        DEPLETED("Depleted area", Sample.FALL, Sample.SOFT, Sample.SNAP, Sample.CLASSIC),
        ONI_TWO("Oni · two away", Sample.CHARGE_LOW, Sample.SOFT, Sample.PLUCK, Sample.CLASSIC),
        ONI_ONE("Oni · one away", Sample.CHARGE_HIGH, Sample.GLASS, Sample.RISE, Sample.CLASSIC),
        ONI_FULL("Oni · full energy", Sample.CHARGE_FULL, Sample.REWARD, Sample.RISE, Sample.CLASSIC),
        COCONUT("Coconut booster", Sample.REWARD, Sample.RISE, Sample.GLASS, Sample.CLASSIC),
        AUCTION("Auction sale", Sample.COINS, Sample.REWARD, Sample.GLASS,
            Sample.PLING, Sample.LEVEL_UP, Sample.ORB, Sample.AMETHYST, Sample.BELL),
        SPEED("Bone Leggings speed", Sample.RISE, Sample.GLASS, Sample.SOFT, Sample.CLASSIC),
        COPY("Gift code copied", Sample.CLICK, Sample.PLUCK, Sample.GLASS, Sample.CLASSIC);

        public final String label;
        final List<Sample> choices;
        Cue(String label, Sample... choices) {
            this.label = label;
            this.choices = List.of(choices);
        }
    }

    private ToolkitAudio() {}

    public static SoundEvent event(String asset) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit", asset));
    }

    public static void play(Cue cue, Minecraft mc, SoundEvent classic, float pitch, float volume, double gain) {
        if (!playSelected(cue, mc, volume, gain)) {
            mc.getSoundManager().play(new AlertSound(classic, pitch, volume, gain));
        }
    }

    public static boolean playSelected(Cue cue, Minecraft mc, float volume, double gain) {
        Sample sample = ToolkitConfig.get().audioChoice(cue);
        if (sample == Sample.CLASSIC) return false;
        if (sample.asset != null) {
            mc.getSoundManager().play(new AlertSound(event(sample.asset), 1, volume * 0.60f, gain));
            return true;
        }
        SoundEvent sound = switch (sample) {
            case PLING -> SoundEvents.NOTE_BLOCK_PLING.value();
            case LEVEL_UP -> SoundEvents.PLAYER_LEVELUP;
            case ORB -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case AMETHYST -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case BELL -> SoundEvents.BELL_BLOCK;
            default -> throw new IllegalStateException("Missing sound for " + sample);
        };
        float pitch = switch (sample) {
            case PLING -> 1.6f;
            case ORB -> 1.2f;
            case AMETHYST -> 1.4f;
            default -> 1;
        };
        mc.getSoundManager().play(new AlertSound(sound, pitch, volume, gain));
        return true;
    }

    public static void playSpeed(Minecraft mc) {
        play(Cue.SPEED, mc, SoundEvents.AMETHYST_BLOCK_CHIME, 1.8f, 0.7f, 1);
    }

    public static void playCopy(Minecraft mc) {
        play(Cue.COPY, mc, SoundEvents.NOTE_BLOCK_PLING.value(), 1.8f, 1, 1);
    }

    public static void preview(Cue cue) {
        var fishing = ToolkitMod.FISHING;
        switch (cue) {
            case BITE -> fishing.previewBite();
            case READY -> fishing.previewDepth();
            case SNAP -> fishing.previewSnap();
            case DEPLETED -> fishing.previewWarning();
            case ONI_TWO -> fishing.previewOniCue(FishingSignals.OniCue.TWO_AWAY);
            case ONI_ONE -> fishing.previewOniCue(FishingSignals.OniCue.ONE_AWAY);
            case ONI_FULL -> fishing.previewOniCue(FishingSignals.OniCue.FULL);
            case COCONUT -> CoconutNotifier.preview();
            case AUCTION -> AhSaleNotifier.playPreviewSound();
            case SPEED -> playSpeed(Minecraft.getInstance());
            case COPY -> playCopy(Minecraft.getInstance());
        }
    }
}
