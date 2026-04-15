package plog.fukntclient.client.Modules.impl.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;

@ModuleInfo(name = "AutoEscape", description = "Eats a chorus fruit to teleport when surrounded", category = Category.Combat)
public class AutoEscape extends Module {

    public final DoubleSetting entityThreshold  = new DoubleSetting("Entity Threshold",  3.0,  1.0, 10.0, 1.0);
    public final DoubleSetting entityRadius      = new DoubleSetting("Entity Radius",     4.0,  1.0, 10.0, 0.5);
    public final DoubleSetting blockThreshold    = new DoubleSetting("Block Threshold",   4.0,  1.0,  6.0, 1.0);
    public final DoubleSetting cooldown          = new DoubleSetting("Cooldown (ticks)",  40.0, 5.0, 200.0, 5.0);
    public final BooleanSetting checkEntities    = new BooleanSetting("Check Entities",   true);
    public final BooleanSetting checkBlocks      = new BooleanSetting("Check Blocks",     true);
    public final BooleanSetting checkLowHealth   = new BooleanSetting("Only Low Health",  false);
    public final DoubleSetting healthThreshold   = new DoubleSetting("Health Thresh",     10.0, 1.0, 20.0, 0.5);

    private int cooldownTimer = 0;

    public AutoEscape() {
        addSettings(checkEntities, entityThreshold, entityRadius,
                checkBlocks, blockThreshold,
                checkLowHealth, healthThreshold, cooldown);
    }

    @Override
    public void onEnable() {
        cooldownTimer = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        // Count down cooldown
        if (cooldownTimer > 0) {
            cooldownTimer--;
            return;
        }

        // Optional: only act when health is low
        if (checkLowHealth.isActive() && mc.player.getHealth() > healthThreshold.getVal()) return;

        boolean surrounded = false;

        if (checkEntities.isActive() && isSurroundedByEntities(mc)) surrounded = true;
        if (checkBlocks.isActive()   && isSurroundedByBlocks(mc))   surrounded = true;

        if (!surrounded) return;

        // Find and eat a chorus fruit
        int slot = findChorusFruit(mc);
        if (slot == -1) return;

        int originalSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        // Start using (eating) the chorus fruit
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);

        mc.player.getInventory().setSelectedSlot(originalSlot);

        cooldownTimer = (int) cooldown.getVal();
    }

    /**
     * True if there are >= entityThreshold hostile/player entities within entityRadius.
     */
    private boolean isSurroundedByEntities(Minecraft mc) {
        int count = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            // Only count hostile entities and other players, not passive mobs
            if (!(living instanceof net.minecraft.world.entity.monster.Monster) &&
                    !(living instanceof Player)) continue;
            // Skip friends
            if (living instanceof Player p &&
                    FukntClientClient.fm.isFriend(p.getGameProfile().name())) continue;

            if (mc.player.distanceTo(living) <= entityRadius.getVal()) count++;
        }
        return count >= (int) entityThreshold.getVal();
    }

    /**
     * True if >= blockThreshold of the 6 cardinal faces around the player are solid.
     * 6/6 = fully boxed in, 4/6 = three-sided trap, etc.
     */
    private boolean isSurroundedByBlocks(Minecraft mc) {
        BlockPos pos = mc.player.blockPosition();
        int solidFaces = 0;

        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.relative(dir);
            var state = mc.level.getBlockState(neighbour);
            if (!state.isAir() && state.isSolid()) solidFaces++;
        }

        return solidFaces >= (int) blockThreshold.getVal();
    }

    /**
     * Returns the hotbar slot (0–8) containing a chorus fruit, or -1 if none.
     * Checks the current slot first so we don't switch unnecessarily.
     */
    private int findChorusFruit(Minecraft mc) {
        int cur = mc.player.getInventory().getSelectedSlot();
        if (mc.player.getInventory().getItem(cur).is(Items.CHORUS_FRUIT)) return cur;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.CHORUS_FRUIT)) return i;
        }
        return -1;
    }
}