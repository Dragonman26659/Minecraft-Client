package plog.fukntclient.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import plog.fukntclient.client.GUI.dropdown.DropdownGUI;
import plog.fukntclient.client.Managers.MarkManager;
import plog.fukntclient.client.Managers.SeedManager;
import plog.fukntclient.client.Managers.TargetManager;
import plog.fukntclient.client.Modules.ModuleManager;
import plog.fukntclient.client.Modules.impl.Visual.Hud;
import plog.fukntclient.client.Modules.impl.Visual.Tracers;
import plog.fukntclient.client.commands.CommandManager;
import plog.fukntclient.client.events.EventBus;
import plog.fukntclient.client.Managers.SocialManager;
import plog.fukntclient.client.settings.SettingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import plog.fukntclient.client.config.ConfigManager;
import plog.charlie.CharlieBot;

public class FukntClientClient implements ClientModInitializer {
	public static final Minecraft mc = Minecraft.getInstance();
	public static final Logger LOGGER = LoggerFactory.getLogger("fuknt");

	public static ModuleManager mm;
	public static SettingManager sm;
	public static CommandManager cm;
	public static SocialManager fm;
	public static TargetManager tm;
	public static SeedManager seedManager;
	public static MarkManager markManager;
	public static CharlieBot Charlie;

	public static final EventBus EVENT_BUS = new EventBus();

	// Define the Keybinding
	public static KeyMapping guiKeybind;

	@Override
	public void onInitializeClient() {
		mm = new ModuleManager();
		sm = new SettingManager();
		cm = new CommandManager();
		fm = new SocialManager();
		tm = new TargetManager();
		Charlie = new CharlieBot();
		seedManager = new SeedManager();
		markManager = new MarkManager();

		ConfigManager.loadConfig(); // Loads modules & settings on startup
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.saveConfig()); // Saves when game closes

		// Register the keybinding (Right Shift)
		guiKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"fuknt ClickGUI", // Translation key
				GLFW.GLFW_KEY_RIGHT_SHIFT, // Default key
				KeyMapping.Category.MISC // Category in controls menu
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			mm.onTick();
			sm.onTick();
			Charlie.onTick();

			// Check if the key was pressed
			while (guiKeybind.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new DropdownGUI());
				} else if (client.screen instanceof DropdownGUI) {
					client.setScreen(null);
				}
			}
		});

		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
			Hud hud = (Hud) mm.getModule(Hud.class);
			if (hud != null && hud.isToggled()) {
				hud.renderHud(graphics);
			}

			Tracers tracers = (Tracers) mm.getModule(Tracers.class);
			if (tracers != null && tracers.isToggled()) {
				tracers.onRenderHud(graphics, tickCounter);
			}
		});


		WorldRenderEvents.END_MAIN.register(context -> {
			if (mc.player != null && mc.level != null) {
				mm.onRender();
				Charlie.onRender();
			}
		});
	}
}