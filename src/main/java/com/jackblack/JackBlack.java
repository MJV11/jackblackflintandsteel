package com.jackblack;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;

import java.util.ArrayList;
import java.util.Arrays;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(JackBlack.MODID)
public class JackBlack {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "jackblack";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    // custom item announcement sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> FLINT_AND_STEEL_SOUND =
            SOUND_EVENTS.register("flint_and_steel", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BUCKET_SOUND =
            SOUND_EVENTS.register("bucket", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> COOKED_CHICKEN_SOUND =
            SOUND_EVENTS.register("cooked_chicken", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENDER_PEARL_SOUND =
            SOUND_EVENTS.register("ender_pearl", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAFTING_TABLE_SOUND =
            SOUND_EVENTS.register("crafting_table", SoundEvent::createVariableRangeEvent);

    private static ArrayList<String> ALLOWED_ITEMS = new ArrayList<String>();

    // custom dimension sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERWORLD_SOUND =
            SOUND_EVENTS.register("overworld", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_NETHER_SOUND =
            SOUND_EVENTS.register("the_nether", SoundEvent::createVariableRangeEvent);

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public JackBlack(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        SOUND_EVENTS.register(modEventBus); // Register the sound

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (JackBlack) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        String[] queuedItems = {"flint_and_steel", "bucket", "crafting_table", "cooked_chicken", "ender_pearl"};
        ALLOWED_ITEMS.addAll(Arrays.asList(queuedItems));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class HotbarSoundHandler {

        private static int lastSlot = -1; // Stores the previous selected slot

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event) {

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return; // Ensure player exists

            int currentSlot = player.getInventory().getSelectedSlot(); // Get the selected hotbar slot
            if (currentSlot != lastSlot) {
                lastSlot = currentSlot;

                ItemStack selectedItem = player.getInventory().getItem(currentSlot); // Get the item in that slot
                String item = selectedItem.toString().substring(12);
                //player.displayClientMessage(Component.literal(item), false); // this is called bugfixing
                if (ALLOWED_ITEMS.contains(item)) {
                    ResourceLocation rl = ResourceLocation.tryParse("jackblack:" + item);
                    SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(rl);
                    if (soundEvent == null) {
                        player.displayClientMessage(Component.literal("sound not found: jackblack:" + item), false);
                        return;
                    }
                    try {
                        player.level().playLocalSound(
                                player.getX(), player.getY(), player.getZ(),
                                soundEvent,
                                SoundSource.PLAYERS,
                                1.0F, 1.0F, false
                        );
                    } catch (Exception e) {
                        player.displayClientMessage(Component.literal(e.toString()), false);
                    }
                }
            }
        }
    }

    // TODO: THIS DOES NOT WORK AT THE MOMENT. I DO NOT KNOW WHY. THE PLAY SOUND LINE FIRES BUT IS SILENT
    // theory is that the nether portal sound overrides all other sounds played before the portal sound.
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class DimensionChangeHandler {
        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
            Player player = event.getEntity();

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer localplayer = mc.player;

            if (!localplayer.getName().getString().equals(player.getName().getString())) return;

            String place = event.getTo().location().toString();
            place = place.substring(place.indexOf(":") + 1);
            if (place.equals("overworld") || place.equals("the_nether")) {
                ResourceLocation rl = ResourceLocation.tryParse("jackblack:" + place);
                SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(rl);
                if (soundEvent == null) {
                    localplayer.displayClientMessage(Component.literal("Sound not found: jackblack:" + place), false);
                    return;
                }

                try {
                    localplayer.level().playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            soundEvent,
                            SoundSource.PLAYERS,
                            1.0F, 1.0F, false
                    );
                } catch (Exception e) {
                    player.displayClientMessage(Component.literal("Error: " + e.getMessage()), false);
                }
            }
        }
    }

    // TODO: FIX THE ONE ABOVE SO THAT WE CAN DELETE THE ONE THAT RUNS EVERY TICK
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class DimensionChangeHandlerBurdensome {
        public static String lastDimension = "";
        @SubscribeEvent
        public static void onPlayerChangedDimensionBurdensome(ClientTickEvent.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            if (player == null) return;

            String place = player.level().dimension().toString();
            place = place.substring(place.indexOf("/ minecraft:") + 12, place.indexOf("]"));

            if (place.equals(lastDimension)) return;
            else lastDimension = place;
            if (place.equals("overworld") || place.equals("the_nether")) {
                ResourceLocation rl = ResourceLocation.tryParse("jackblack:" + place);
                SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(rl);
                if (soundEvent == null) {
                    player.displayClientMessage(Component.literal("Sound not found: jackblack:" + place), false);
                    return;
                }

                try {
                    player.level().playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            soundEvent,
                            SoundSource.PLAYERS,
                            1.0F, 1.0F, false
                    );
                } catch (Exception e) {
                    player.displayClientMessage(Component.literal("Error: " + e.getMessage()), false);
                }
            }
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    // NOTE: THIS SEEMS TO BE CRITICAL FOR SOME REASON
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}