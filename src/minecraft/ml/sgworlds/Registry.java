package ml.sgworlds;

import ml.core.network.PacketHandler;
import cpw.mods.fml.common.network.NetworkRegistry;

public class Registry {
	
	public static SGWorldsConfig config;

	public static void registerBlocks() {
		
	}
	
	// Items //
	
	public static void registerItems() {

	}
	
	// TileEntities //
	public static void registerTileEntities() {
		
	}
	
	// Packets //
	public static void registerPackets() {
		PacketHandler pkh = new PacketHandler();
		NetworkRegistry.instance().registerChannel(pkh, SGWorlds.netChannel);
		
		// Packets

	}
	
	// Recipes //
	public static void registerRecipes() {

	}
}