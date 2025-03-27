package com.jackblack;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(JackBlack.MODID)
public class JackBlack {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "jackblack";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> FLINT_AND_STEEL_SOUND =
            SOUND_EVENTS.register("flint_and_steel", SoundEvent::createVariableRangeEvent);

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
            if (currentSlot != lastSlot) { // If the slot changed, play sound
                lastSlot = currentSlot;

                ItemStack selectedItem = player.getInventory().getItem(currentSlot); // Get the item in that slot

                if (selectedItem.is(Items.FLINT_AND_STEEL)) {
                    player.displayClientMessage(Component.literal("item is flint and steel"), false);

                    ResourceLocation rl = ResourceLocation.tryParse("jackblack:flint_and_steel");
                    SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(rl);

                    if (soundEvent == null) {
                        player.displayClientMessage(Component.literal("sound not found"), false);
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