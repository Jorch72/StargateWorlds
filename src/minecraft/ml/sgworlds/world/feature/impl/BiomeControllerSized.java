package ml.sgworlds.world.feature.impl;

import static net.minecraft.world.biome.BiomeGenBase.forest;
import static net.minecraft.world.biome.BiomeGenBase.forestHills;
import static net.minecraft.world.biome.BiomeGenBase.jungle;
import static net.minecraft.world.biome.BiomeGenBase.jungleHills;
import static net.minecraft.world.biome.BiomeGenBase.plains;
import static net.minecraft.world.biome.BiomeGenBase.taiga;
import static net.minecraft.world.biome.BiomeGenBase.taigaHills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import ml.sgworlds.api.world.IWorldData;
import ml.sgworlds.api.world.feature.FeatureProvider;
import ml.sgworlds.api.world.feature.prefab.BaseBiomeController;
import ml.sgworlds.api.world.feature.types.IBiomeController;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerAddIsland;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerIsland;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;

public class BiomeControllerSized extends BaseBiomeController implements IBiomeController {

	public List<BiomeGenBase> allowedBiomes;
	private GenLayer genBiomes;
	private GenLayer biomeIndexLayer;
	private List biomesToSpawnIn = new ArrayList<BiomeGenBase>(Arrays.asList(forest, plains, taiga, taigaHills, forestHills, jungle, jungleHills));;
	protected int zoomFactor;

	public BiomeControllerSized(FeatureProvider provider, IWorldData worldData, int zoom, List<BiomeGenBase> biomes) {
		super(provider, worldData);
		this.zoomFactor = zoom;
		this.allowedBiomes = biomes;
	}
	
	public BiomeControllerSized(FeatureProvider provider, IWorldData worldData, int zoom) {
		super(provider, worldData);
		this.zoomFactor = zoom;
		this.allowedBiomes = new ArrayList<BiomeGenBase>(Arrays.asList(WorldType.DEFAULT.getBiomesForWorldType()));;
	}
	
	public BiomeControllerSized(FeatureProvider provider, IWorldData worldData, Random rnd) {
		super(provider, worldData);
		
		this.zoomFactor = rnd.nextInt(7);
		
		List<BiomeGenBase> vbiomes = new ArrayList<BiomeGenBase>(Arrays.asList(WorldType.DEFAULT.getBiomesForWorldType()));
		Collections.shuffle(vbiomes);
		this.allowedBiomes = vbiomes.subList(0, rnd.nextInt(vbiomes.size()-3)+3);
	}
	
	public BiomeControllerSized(FeatureProvider provider, IWorldData worldData, NBTTagCompound nbtData) {
		super(provider, worldData);
		
		this.zoomFactor = nbtData.getInteger("zoomFactor");
		this.allowedBiomes = new ArrayList<BiomeGenBase>();
		for (int bioId : nbtData.getIntArray("biomes")) {
			allowedBiomes.add(BiomeGenBase.biomeList[bioId]);
		}
	}
	
	@Override
	public void onProviderCreated(WorldProvider wprovider) {
		super.onProviderCreated(wprovider);
		GenLayer[] agenlayer = initializeAllBiomeGenerators(worldData.getWorldSeed(), WorldType.DEFAULT);
		this.genBiomes = agenlayer[0];
		this.biomeIndexLayer = agenlayer[1];
	}
	
	@Override
	public void writeNBTData(NBTTagCompound tag) {
		int[] biomeIds = new int[allowedBiomes.size()];
		for (int i=0; i<allowedBiomes.size(); i++) {
			biomeIds[i] = allowedBiomes.get(i).biomeID;
		}
		tag.setIntArray("biomes", biomeIds);
		tag.setInteger("zoomFactor", zoomFactor);
	}

	@Override
	public List<BiomeGenBase> getBiomesForSpawn() {
		return biomesToSpawnIn;
	}
	
	@Override
	public BiomeGenBase calcBiomeAt(int x, int z) {
		return BiomeGenBase.biomeList[biomeIndexLayer.getInts(x, z, 1, 1)[0]];
	}

	@Override
	public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] reuseArray, int x, int z, int width, int length) {
		IntCache.resetIntCache();

		if (reuseArray == null || reuseArray.length < width * length) {
			reuseArray = new BiomeGenBase[width * length];
		}

		int[] aint = this.genBiomes.getInts(x, z, width, length);

		for (int i1 = 0; i1 < width * length; ++i1) {
			reuseArray[i1] = BiomeGenBase.biomeList[aint[i1]];
		}

		return reuseArray;
	}
	
	@Override
	public float[] getRainfall(float[] reuseArray, int x, int z, int width, int length) {
		IntCache.resetIntCache();

		if (reuseArray == null || reuseArray.length < width * length) {
			reuseArray = new float[width * length];
		}
		
		int[] aint = this.biomeIndexLayer.getInts(x, z, width, length);

		for (int lx=0; lx<width; lx++) {
			for (int lz=0; lz<length; lz++) {
				float f = BiomeGenBase.biomeList[aint[lx + lz*width]].rainfall;
				if (f>1.0F) f=1.0F;
				reuseArray[lx + lz*width] = f;
			}
		}

		return reuseArray;
	}

	@Override
	public float[] getTemperatures(float[] reuseArray, int x, int z, int width, int length) {
		IntCache.resetIntCache();

		if (reuseArray == null || reuseArray.length < width * length) {
			reuseArray = new float[width * length];
		}

		int[] aint = this.biomeIndexLayer.getInts(x, z, width, length);
		
		for (int lx=0; lx<width; lx++) {
			for (int lz=0; lz<length; lz++) {
				float f = BiomeGenBase.biomeList[aint[lx + lz*width]].temperature;
				if (f>1.0F) f=1.0F;
				reuseArray[lx + lz*width] = f;
			}
		}

		return reuseArray;
	}

	private GenLayer[] initializeAllBiomeGenerators(long worldSeed, WorldType par2WorldType) {
		GenLayerIsland genlayerisland = new GenLayerIsland(1L);
		GenLayerFuzzyZoom genlayerfuzzyzoom = new GenLayerFuzzyZoom(2000L, genlayerisland);
		GenLayerAddIsland genlayeraddisland = new GenLayerAddIsland(1L, genlayerfuzzyzoom);
		GenLayerZoom genlayerzoom = new GenLayerZoom(2001L, genlayeraddisland);
		genlayeraddisland = new GenLayerAddIsland(2L, genlayerzoom);

		genlayerzoom = new GenLayerZoom(2002L, genlayeraddisland);
		genlayeraddisland = new GenLayerAddIsland(3L, genlayerzoom);
		genlayerzoom = new GenLayerZoom(2003L, genlayeraddisland);
		genlayeraddisland = new GenLayerAddIsland(4L, genlayerzoom);

		GenLayer genlayer = GenLayerZoom.magnify(1000L, genlayeraddisland, 0);
		genlayer = GenLayerZoom.magnify(1000L, genlayer, zoomFactor + 1);

		GenLayerSmooth genlayersmooth = new GenLayerSmooth(1000L, genlayer);

		GenLayer genlayer1 = GenLayerZoom.magnify(1000L, genlayeraddisland, 0);
		GenLayerBiome genlayerbiome = new GenLayerBiome(200L, genlayer1);
		genlayer1 = GenLayerZoom.magnify(1000L, genlayerbiome, 2);
		
		genlayer1 = GenLayerZoom.magnify(1000L, genlayer1, zoomFactor);

		GenLayerSmooth genlayersmooth1 = new GenLayerSmooth(1000L, (GenLayer)genlayer1);
		GenLayerVoronoiZoom genlayervoronoizoom = new GenLayerVoronoiZoom(10L, genlayersmooth1);
		genlayervoronoizoom.initWorldGenSeed(worldSeed);
		return new GenLayer[] {genlayersmooth, genlayervoronoizoom};
	}

	private class GenLayerBiome extends GenLayer {
		public GenLayerBiome(long par1, GenLayer par3GenLayer) {
			super(par1);
			this.parent = par3GenLayer;
		}

		public int[] getInts(int x, int z, int width, int length) {
			int[] aint1 = IntCache.getIntCache(width * length);

			for (int zsub = 0; zsub < length; ++zsub) {
				for (int xsub = 0; xsub < width; ++xsub) {
					this.initChunkSeed((long)(xsub + x), (long)(zsub + z));

					aint1[xsub + zsub * width] = allowedBiomes.get(this.nextInt(allowedBiomes.size())).biomeID;
				}
			}

			return aint1;
		}

	}
	
}
