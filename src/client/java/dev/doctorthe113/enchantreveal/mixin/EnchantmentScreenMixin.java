package dev.doctorthe113.enchantreveal.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctorthe113.enchantreveal.EnchantPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.IdMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
	private EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	/** Replaces the "Enchantment . . . ?" clue line with one line per enchantment the slot will give. */
	@WrapOperation(
			method = "extractRenderState",
			at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
	private boolean enchantreveal$listAll(List<Component> lines, Object clueLine, Operation<Boolean> original,
			@Local(name = "i") int slot) {
		List<EnchantmentInstance> all = enchantreveal$preview(slot);
		if (all == null) {
			return original.call(lines, clueLine);
		}
		for (EnchantmentInstance e : all) {
			lines.add(Enchantment.getFullname(e.enchantment(), e.level()));
		}
		return true;
	}

	/** Returns null (keep the vanilla clue) unless the preview can be trusted. */
	private List<EnchantmentInstance> enchantreveal$preview(int slot) {
		Minecraft minecraft = Minecraft.getInstance();
		IntegratedServer server = minecraft.getSingleplayerServer();
		if (server == null || minecraft.player == null || minecraft.level == null) {
			return null; // Multiplayer: the full seed only exists on the server.
		}
		ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
		if (serverPlayer == null) {
			return null;
		}

		int seed = serverPlayer.getEnchantmentSeed();
		// The client copy holds the low 16 bits; a mismatch means a fresh seed hasn't synced yet.
		if ((short) seed != (short) this.menu.getEnchantmentSeed()) {
			return null;
		}

		RegistryAccess registries = minecraft.level.registryAccess();
		List<EnchantmentInstance> all = EnchantPreview.compute(
				registries, this.menu.getSlot(0).getItem(), seed, slot, this.menu.costs[slot]);

		// The server's clue is always one of the slot's enchantments; if it isn't, the state is stale.
		IdMap<Holder<Enchantment>> ids = registries.lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
		for (EnchantmentInstance e : all) {
			if (ids.getId(e.enchantment()) == this.menu.enchantClue[slot] && e.level() == this.menu.levelClue[slot]) {
				return all;
			}
		}
		return null;
	}
}
