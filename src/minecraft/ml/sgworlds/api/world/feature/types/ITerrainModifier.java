package ml.sgworlds.api.world.feature.types;

import net.minecraft.world.World;

public interface ITerrainModifier {

	public void generate(World world, int chunkX, int chunkY, short[] blockIds, byte[] blockMetas);
	
}
