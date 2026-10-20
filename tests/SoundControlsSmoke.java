package com.skyblocktoolkit;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.util.Arrays;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs in a disposable Fabric client directory, before any window or login. */
public final class SoundControlsSmoke implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        try {
            var engine = Class.forName("net.minecraft.client.sounds.SoundEngine", false, getClass().getClassLoader());
            var channel = Class.forName("com.mojang.blaze3d.audio.Channel", false, getClass().getClassLoader());
            check(Arrays.stream(engine.getDeclaredMethods()).anyMatch(m -> m.getName().contains("toolkit$alertGain")), "sound engine injection");
            check(Arrays.stream(channel.getDeclaredMethods()).anyMatch(m -> m.getName().contains("toolkit$gainCeiling")), "OpenAL channel injection");

            var configDir = FabricLoader.getInstance().getConfigDir();
            Files.createDirectories(configDir);
            Files.writeString(configDir.resolve("skyblocktoolkit.json"),
                "{\"ahSoundVolume\":0.42,\"readySoundGain\":0.8,\"snapSoundGain\":4,\"ahSoundGain\":-1}");
            var fishing=new com.fishingpings.FishingConfig();
            fishing.volume=3;fishing.save();
            check(new com.fishingpings.FishingConfig().volume==3,"300% fishing volume survives reload");
            var cfg = ToolkitConfig.get();
            check(cfg.readySoundGain == .8 && cfg.areaSoundGain == 1.9, "independent / default gain");
            check(cfg.snapSoundGain == 3 && cfg.ahSoundGain == 0, "gain limits");
            check(cfg.ahSoundVolume == .42, "existing volume preserved");
            cfg.readySoundGain = .5; cfg.areaSoundGain = 2; cfg.snapSoundGain = 0; cfg.ahSoundGain = 1.9;
            cfg.save();
            Constructor<ToolkitConfig> constructor = ToolkitConfig.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            var saved = constructor.newInstance();
            check(saved.readySoundGain == .5 && saved.areaSoundGain == 2 && saved.snapSoundGain == 0 && saved.ahSoundGain == 1.9,
                "all four gains survive save / reload independently");
            check(saved.ahSoundVolume == .42, "saved existing volume preserved");
            var unsafeClass=Class.forName("sun.misc.Unsafe");
            var unsafeField=unsafeClass.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
            Object unsafe=unsafeField.get(null);
            var allocate=unsafeClass.getMethod("allocateInstance",Class.class);
            Object engineInstance=allocate.invoke(unsafe,engine);
            // Exercise the initial-playback redirect, not just the update hook:
            // vanilla clamps the base to 1 and applies UI/master before our gain.
            var optionsClass=Class.forName("net.minecraft.client.Options");
            Object options=allocate.invoke(unsafe,optionsClass);
            var optionClass=Class.forName("net.minecraft.client.OptionInstance");
            Object halfVolume=allocate.invoke(unsafe,optionClass);
            var valueField=optionClass.getDeclaredField("value");valueField.setAccessible(true);valueField.set(halfVolume,.5);
            var volumesField=optionsClass.getDeclaredField("soundSourceVolumes");volumesField.setAccessible(true);
            volumesField.set(options,java.util.Map.of(net.minecraft.sounds.SoundSource.UI,halfVolume,net.minecraft.sounds.SoundSource.MASTER,halfVolume));
            var optionsField=engine.getDeclaredField("options");optionsField.setAccessible(true);optionsField.set(engineInstance,options);
            var gains=new it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap<net.minecraft.sounds.SoundSource>();
            gains.defaultReturnValue(1);
            var gainsField=engine.getDeclaredField("gainBySource");gainsField.setAccessible(true);gainsField.set(engineInstance,gains);
            var initialMethod=Arrays.stream(engine.getDeclaredMethods()).filter(m -> m.getName().contains("toolkit$initialAlertVolume")).findFirst().orElseThrow();
            initialMethod.setAccessible(true);
            var gainMethod=Arrays.stream(engine.getDeclaredMethods()).filter(m -> m.getName().contains("toolkit$alertGain")).findFirst().orElseThrow();
            gainMethod.setAccessible(true);
            var event=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit","test"));
            SoundInstance[] sounds={new AlertSound(event,1,1,1.9),new AlertSound(event,1,1,0),new AlertSound(event,1,1,9),SimpleSoundInstance.forUI(event,1,1)};
            float[] expected={.475f,0,2.25f,.25f};
            for(int i=0;i<sounds.length;i++){
                var result=new CallbackInfoReturnable<Float>("test",true,.25f);
                gainMethod.invoke(engineInstance,sounds[i],result);
                check(Math.abs(result.getReturnValueF()-expected[i])<.00001,"gain / mute / ordinary sound isolation "+i);
                float initial=(float)initialMethod.invoke(engineInstance,engineInstance,1f,net.minecraft.sounds.SoundSource.UI,sounds[i]);
                check(Math.abs(initial-expected[i])<.00001,"initial playback gain / mute / ordinary sound isolation "+i);
            }
            float normal=(float)initialMethod.invoke(engineInstance,engineInstance,1f,net.minecraft.sounds.SoundSource.UI,new AlertSound(event,1,1,1));
            float boosted=(float)initialMethod.invoke(engineInstance,engineInstance,1f,net.minecraft.sounds.SoundSource.UI,new AlertSound(event,1,1,3));
            check(Math.abs(boosted/normal-3)<.00001,"300% is three times 100% at initial playback");

            long device=ALC10.alcOpenDevice((java.nio.ByteBuffer)null);
            check(device!=0,"offline audio device");
            var caps=ALC.createCapabilities(device);
            long context=ALC10.alcCreateContext(device,(java.nio.IntBuffer)null);
            check(context!=0 && ALC10.alcMakeContextCurrent(context),"offline audio context");
            AL.createCapabilities(caps);
            check(AL.getCapabilities().AL_SOFT_gain_clamp_ex,"extended gain support");
            int source=AL10.alGenSources();
            Object channelInstance=allocate.invoke(unsafe,channel);
            var sourceField=channel.getDeclaredField("source");sourceField.setAccessible(true);sourceField.setInt(channelInstance,source);
            var volumeMethod=channel.getMethod("setVolume",float.class);
            volumeMethod.invoke(channelInstance,1.9f);
            check(Math.abs(AL10.alGetSourcef(source,AL10.AL_MAX_GAIN)-1.9f)<.00001 && AL10.alGetError()==AL10.AL_NO_ERROR,"native amplified gain");
            volumeMethod.invoke(channelInstance,9f);
            check(Math.abs(AL10.alGetSourcef(source,AL10.AL_MAX_GAIN)-9f)<.00001 && AL10.alGetError()==AL10.AL_NO_ERROR,"maximum combined fishing gain");
            volumeMethod.invoke(channelInstance,.4f);
            check(AL10.alGetSourcef(source,AL10.AL_MAX_GAIN)==1 && AL10.alGetError()==AL10.AL_NO_ERROR,"pooled channel gain reset");
            AL10.alDeleteSources(source);ALC10.alcMakeContextCurrent(0);ALC10.alcDestroyContext(context);ALC10.alcCloseDevice(device);
            System.out.println("PASS: real Fabric audio mixins, native gain, mute, game sound isolation, channel reuse, independent settings and persistence");
            System.exit(0);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
