package ml.sgworlds.api.world.feature.types;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

public interface IFeatureLocator {

	public ChunkPosition locateFeature(World world, String strucId, int x, int y, int z);
}
