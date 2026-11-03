package dev.doctorthe113.enchantreveal.test;

import dev.doctorthe113.enchantreveal.EnchantPreview;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

/** Opens a real enchanting table in singleplayer and screenshots the hovered slot's tooltip. */
public class EnchantRevealClientTest implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("enchantreveal-test");

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			screenshotSlot(context, singleplayer, Items.DIAMOND_SWORD, 2, "sword-slot3");
			screenshotSlot(context, singleplayer, Items.DIAMOND_CHESTPLATE, 1, "chestplate-slot2");
			screenshotSlot(context, singleplayer, Items.BOOK, 2, "book-slot3");
		}
	}

	private static void screenshotSlot(ClientGameTestContext context, TestSingleplayerContext singleplayer,
			Item item, int slot, String name) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			ServerLevel level = player.level();
			BlockPos table = player.blockPosition().offset(0, 0, 4);
			level.setBlockAndUpdate(table, Blocks.ENCHANTING_TABLE.defaultBlockState());
			for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
				level.setBlockAndUpdate(table.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
			}
			player.openMenu(level.getBlockState(table).getMenuProvider(level, table));
			EnchantmentMenu menu = (EnchantmentMenu) player.containerMenu;
			menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
			menu.getSlot(0).set(new ItemStack(item));
			for (int s = 0; s < 3; s++) {
				LOGGER.info("[{}] server slot {} (cost {}): {}", name, s + 1, menu.costs[s], EnchantPreview.compute(
						level.registryAccess(), menu.getSlot(0).getItem(), player.getEnchantmentSeed(), s, menu.costs[s]));
			}
		});
		context.waitForScreen(EnchantmentScreen.class);
		singleplayer.getConnection().waitForClientboundPackets();
		context.waitTicks(5);

		double[] cursor = context.computeOnClient(mc -> {
			int scale = mc.getWindow().getGuiScale();
			int left = (mc.getWindow().getGuiScaledWidth() - 176) / 2;
			int top = (mc.getWindow().getGuiScaledHeight() - 166) / 2;
			return new double[] {(left + 110) * scale, (top + 14 + 19 * slot + 8) * scale};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
		context.waitTicks(3);
		LOGGER.info("[{}] screenshot: {}", name, context.takeScreenshot(name));

		context.runOnClient(mc -> mc.player.closeContainer());
		context.waitTicks(2);
	}
}
