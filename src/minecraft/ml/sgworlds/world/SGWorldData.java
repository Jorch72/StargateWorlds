package ml.sgworlds.world;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ml.sgworlds.SGWorlds;
import ml.sgworlds.api.world.IStaticWorld;
import ml.sgworlds.api.world.IWorldData;
import ml.sgworlds.api.world.feature.FeatureProvider;
import ml.sgworlds.api.world.feature.FeatureType;
import ml.sgworlds.api.world.feature.WorldFeature;
import ml.sgworlds.world.dimension.SGWorldProvider;
import ml.sgworlds.world.feature.FeatureManager;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldProvider;
import stargatetech2.api.StargateTechAPI;
import stargatetech2.api.stargate.Address;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Stores info for individual worlds generated by SGWorlds. e.g. Day Length, # of suns/moons, etc.
 * Pre-generated when a world is started.
 * @author Matchlighter
 */
public class SGWorldData implements IWorldData {

	private String name = "";
	private String designation;
	private Address primaryAddress;
	private int dimensionId = 0;
	private long seed;
	private Multimap<FeatureType, WorldFeature> features = HashMultimap.create();
	private SGWorldProvider worldProvider;
	private boolean dirty;
	
	private long worldTime;
	
	public SGWorldData(String designation, Address address) {
		this.designation = designation;
		this.primaryAddress = address;
		
		markDirty();
	}
	
	public SGWorldData(NBTTagCompound tag) {
		readFromNBT(tag);
	}

	public String getDisplayName() {
		return (name == null || name.equals("")) ? designation : name;
	}
	
	public void readFromNBT(NBTTagCompound nbt) {
		this.designation = nbt.getString("designation");
		this.name = nbt.getString("name");
		this.primaryAddress = StargateTechAPI.api().getStargateNetwork().parseAddress(nbt.getString("address"));
		this.dimensionId = nbt.getInteger("dim");
		this.seed = nbt.getLong("seed");
		this.worldTime = nbt.getLong("worldTime");
		
		features = HashMultimap.create();
		NBTTagList list = nbt.getTagList("features");
		for (int i=0; i<list.tagCount(); i++) {
			NBTTagCompound ftag = (NBTTagCompound)list.tagAt(i);
			String id = ftag.getString("identifier");
			
			FeatureProvider prov = FeatureManager.instance.getFeatureProvider(id);
			if (prov != null) {
				WorldFeature feature = prov.loadFromNBT(this, ftag.getCompoundTag("data"));
				features.put(prov.type, feature);
				
				List<FeatureType> secondaryTypes = new ArrayList<FeatureType>();
				feature.getSecondaryTypes(secondaryTypes);
				for (FeatureType stype : secondaryTypes) {
					if (stype == prov.type) continue;
					features.put(stype, feature);
				}
			} else {
				throw new RuntimeException(String.format("Missing feature for identifier \"%s\"", id, getDisplayName()));
			}
		}
	
		validateAndFix();
	}

	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setString("designation", designation);
		nbt.setString("name", name);
		nbt.setString("address", primaryAddress.toString());
		nbt.setInteger("dim", dimensionId);
		nbt.setLong("seed", seed);
		nbt.setLong("worldTime", worldTime);
		
		NBTTagList list = new NBTTagList();
		Set<WorldFeature> featureSet = new HashSet<WorldFeature>(features.values());
		for (WorldFeature ft : featureSet) {
			NBTTagCompound ftag = new NBTTagCompound();
			ftag.setString("identifier", ft.getProvider().identifier);
			
			NBTTagCompound dataTag = new NBTTagCompound();
			ft.writeNBTData(dataTag);
			ftag.setCompoundTag("data", dataTag);
			
			list.appendTag(ftag);
		}
		nbt.setTag("features", list);
	}
	
	protected void validateAndFix() {
		for (FeatureType ftype : FeatureType.values()) {
			if (ftype == FeatureType.ALL) continue;
			
			fillFeatures(false);
			
			// Check that we meet the minimums.
			int count = getFeatures(ftype).size();
			if (count < ftype.getMinimumCount())
				throw new RuntimeException(
						String.format("Missing minimum number (%d/%d) of feature type %s and could not generate more.", count, ftype.getMaximumCount(), ftype.toString()));
			
			// Check that we only have 1 of singleton types.
			if (ftype.isSingleton() && count > 1) {
				throw new RuntimeException(
						String.format("Had more than one instance of singelton feature type %s", ftype.toString()));
			}
		}
	}
	
	public void markDirty() {
		this.dirty = true;
	}
	
	/**
	 * Checks if we meet the minimum count for each feature. If not, generate features to minimum.
	 * @param useDefaults If true, default features are preferred. It will only generate a random feature if there isn't a default.
	 * @return If any features were generated and added.
	 */
	public boolean fillFeatures(boolean useDefaults) {
		Random rand = new Random();
		
		boolean flag = false;
		for (FeatureType type : FeatureType.values()) {
			if (type == FeatureType.ALL) continue;

			int fcount = type.getMinimumCount() - this.getFeatures(type).size();
			if (fcount > 0) {
				flag = true;
				FeatureProvider provider = FeatureManager.instance.getDefaultFeatureProvider(type);
				if (provider != null) {
					this.features.put(type, provider.constructFeature(this));
				} else {
					List<WorldFeature> features = WorldDataGenerator.generateRandomTypeFeatures(this, type, fcount, rand);
					this.features.putAll(type, features);
				}
				markDirty();
			}
		}
		
		return flag;
	}
	
	public int getDimensionId() {
		return dimensionId;
	}
	
	public void setDimensionId(int dimensionId) {
		if (this.dimensionId == 0) this.dimensionId = dimensionId;
		markDirty();
	}
	
	public Address getPrimaryAddress() {
		return primaryAddress;
	}
	
	public String getDesignation() {
		return designation;
	}
	
	@Override
	public long getWorldSeed() {
		return seed;
	}
	
	public long getWorldTime() {
		return worldTime;
	}
	
	public void setWorldTime(long worldTime) {
		this.worldTime = worldTime;
		markDirty();
	}
	
	@Override
	public WorldProvider getWorldProvider() {
		return worldProvider;
	}
	
	public void setWorldProvider(SGWorldProvider pvdr) {
		this.worldProvider = pvdr;
		for (WorldFeature feature : features.values()) {
			feature.onProviderCreated(pvdr);
		}
	}
	
	public String getSaveFolderName() {
		return "SG_WORLD" + this.dimensionId;
	}
	
	@Override
	public WorldFeature getFeature(FeatureType type) {
		List<WorldFeature> tfeats = getFeatures(type);
		return tfeats.size() > 0 ? tfeats.get(0) : null;
	}
	
	@Override
	public List<WorldFeature> getFeatures(FeatureType type) {
		if (type == FeatureType.ALL) return new ArrayList<WorldFeature>(features.values());
		return new ArrayList<WorldFeature>(features.get(type));
	}

	@Override
	public boolean hasFeatureIdentifier(String identifier) {
		for (WorldFeature feature : features.values()) {
			if (feature.getProvider().identifier.equals(identifier)) {
				return true;
			}
		}
		return false;
	}
	
	private void addFeature(WorldFeature feature) {
		this.features.put(feature.getType(), feature);
		
		List<FeatureType> secondaryTypes = new ArrayList<FeatureType>();
		feature.getSecondaryTypes(secondaryTypes);
		for (FeatureType stype : secondaryTypes) {
			if (stype == feature.getType()) continue;
			if (stype.isSingleton()) {
				throw new IllegalArgumentException(String.format("The class \"%s\" tried to register \"%s\" (a singleton feature type) as a secondary feature type.",
						feature.getClass().getName(), stype.name()));
			}
			this.features.put(stype, feature);
		}
		
		markDirty();
	}
	
	private static File getWorldDataFile(String fname) {
		return SGWorlds.getSaveFile("SGWorlds/world_"+fname);
	}
	
	public void saveData() {
		try {
			File wdloc = getWorldDataFile(designation);

			if (wdloc != null) {
				NBTTagCompound nbttagcompound = new NBTTagCompound();
				this.writeToNBT(nbttagcompound);
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setCompoundTag("data", nbttagcompound);
				FileOutputStream fileoutputstream = new FileOutputStream(wdloc);
				CompressedStreamTools.writeCompressed(nbttagcompound1, fileoutputstream);
				fileoutputstream.close();
			}
			
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	public static SGWorldData loadData(String designation) {
		File wdloc = getWorldDataFile(designation);
		try {
			FileInputStream istream = new FileInputStream(wdloc);
			NBTTagCompound loadTag = CompressedStreamTools.readCompressed(istream);
			istream.close();
			return new SGWorldData(loadTag);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static SGWorldData generateRandom() {
		SGWorldData sgd = new SGWorldData(WorldDataGenerator.getRandomDesignation(), WorldDataGenerator.generateAddress());
		sgd.seed = (new Random()).nextLong();
		
		for (WorldFeature feat : WorldDataGenerator.generateRandomFeatures(sgd).values()) {
			sgd.addFeature(feat);
		}
		return sgd;
	}
	
	public static SGWorldData fromStaticWorld(IStaticWorld sworld) {
		SGWorldData sgd = new SGWorldData(sworld.getDesignation(), sworld.getAddress());
		sgd.seed = sworld.getSeed();
		sgd.name = sworld.getName();
		
		for (WorldFeature feat : sworld.getWorldFeatureList(sgd)) {
			sgd.addFeature(feat);
		}
		
		sgd.fillFeatures(true);
		
		return sgd;
	}
}
