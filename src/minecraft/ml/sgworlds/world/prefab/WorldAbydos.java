package ml.sgworlds.world.prefab;

import java.util.List;
import java.util.Random;

import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;

import ml.sgworlds.api.world.IStaticWorld;
import ml.sgworlds.api.world.IWorldData;
import ml.sgworlds.api.world.feature.IFeatureBuilder;
import ml.sgworlds.api.world.feature.SGWFeatures;
import ml.sgworlds.api.world.feature.WorldFeature;
import ml.sgworlds.world.feature.FeatureBuilder;
import stargatetech2.api.StargateTechAPI;
import stargatetech2.api.stargate.Address;
import stargatetech2.api.stargate.IStargatePlacer;

public class WorldAbydos implements IStaticWorld {

	@Override
	public long getSeed() {
		return 0;
	}

	@Override
	public Address getAddress() {
		return StargateTechAPI.api().getStargateNetwork().parseAddress("Croecom Erpvabrei At");
	}

	@Override
	public String getDesignation() {
		return "P8X-873";
	}

	@Override
	public String getName() {
		return "Abydos";
	}

	@Override
	public List<WorldFeature> getWorldFeatureList(IWorldData worldData) {
		IFeatureBuilder fbuilder = new FeatureBuilder(worldData);
		Random rand = new Random();
		
		// Suns
		fbuilder.createFeatureReflection(SGWFeatures.SUN_NORMAL.name());
		
		// Moons
		fbuilder.createFeatureReflection(SGWFeatures.MOON_NORMAL.name(), "size", 15, "angle", 70, "orbitPeriod", 10000L);
		fbuilder.createFeatureReflection(SGWFeatures.MOON_NORMAL.name(), "size", 25, "angle", 10, "orbitPeriod", 32000L, "offset", -0.35F);
		fbuilder.createFeatureReflection(SGWFeatures.MOON_NORMAL.name(), "size", 20);
		
		// Biome
		fbuilder.createFeatureConstructor(SGWFeatures.BIOME_SINGLE.name(), BiomeGenBase.desert);
		
		// Populators
		fbuilder.createFeatureConstructor(SGWFeatures.POPULATE_ORE_NAQUADAH.name());
		
		// Clouds
		fbuilder.createFeatureConstructor(SGWFeatures.CLOUD_COLOR_NORMAL.name(), 0xEFEDCB);
		
		// Fog
		fbuilder.createFeatureConstructor(SGWFeatures.FOG_COLOR_NORMAL.name(), 0xCCBF8C);
		
		return fbuilder.getFeatureList();
	}

	@Override
	public boolean generateStargate(WorldServer world, IStargatePlacer seedingShip) {
		// TODO Auto-generated method stub
		return false;
	}

}