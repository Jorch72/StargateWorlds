package ml.sgworlds.dimension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ml.sgworlds.api.world.IWorldData;
import ml.sgworlds.api.world.FeatureProvider;
import ml.sgworlds.api.world.FeatureType;
import ml.sgworlds.api.world.IWorldFeature;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import stargatetech2.api.StargateTechAPI;
import stargatetech2.api.stargate.Address;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cpw.mods.fml.common.FMLLog;

/**
 * Stores info for individual worlds generated by SGWorlds. e.g. Day Length, # of suns/moons, etc.
 * Pre-generated when a world is started.
 * @author Matchlighter
 */
public class SGWorldData extends WorldSavedData implements IWorldData {

	private String name = "";
	private String designation;
	private Address primaryAddress;
	private int dimensionId = 0;
	private long seed;
	private Multimap<FeatureType, IWorldFeature> features = HashMultimap.create();
	private SGWorldProvider worldProvider;
	
	public SGWorldData(String designation, Address address) {
		super(getUID(designation));
		this.designation = designation;
		this.primaryAddress = address;
	}
	
	/** Constructor used when Loading from NBT */
	public SGWorldData(String uid) {
		super(uid);
	}

	public String getDisplayName() {
		return (name == null || name.equals("")) ? designation : name;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		this.designation = nbt.getString("designation");
		this.name = nbt.getString("name");
		this.primaryAddress = StargateTechAPI.api().getStargateNetwork().parseAddress(nbt.getString("address"));
		this.dimensionId = nbt.getInteger("dim");
		
		try {
			features = HashMultimap.create();
			NBTTagList list = nbt.getTagList("features");
			for (int i=0; i<list.tagCount(); i++) {
				NBTTagCompound ftag = (NBTTagCompound)list.tagAt(i);
				String id = ftag.getString("identifier");
				
				FeatureProvider prov = FeatureManager.instance.getFeatureProvider(id);
				if (prov != null) {
					IWorldFeature feature = prov.constructFeature(this);
					feature.readNBTData(ftag.getCompoundTag("data"));
					features.put(prov.type, feature);
					
					List<FeatureType> secondaryTypes = new ArrayList<FeatureType>();
					feature.getSecondaryTypes(secondaryTypes);
					for (FeatureType stype : secondaryTypes) {
						if (stype == prov.type) continue;
						features.put(stype, feature);
					}
				} else {
					// TODO Consider other options.
					throw new RuntimeException(String.format("Missing feature for identifier \"%s\"", id, getDisplayName()));
				}
			}
		
			validateAndFix();
		} catch (Exception e) {
			FMLLog.severe("World \"%s\" invalid because %s. The world will not be loaded!", getDisplayName(), e.getMessage());
			return;
		}
		
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setString("designation", designation);
		nbt.setString("name", name);
		nbt.setString("address", primaryAddress.toString());
		nbt.setInteger("dim", dimensionId);
		
		NBTTagList list = new NBTTagList();
		Set<IWorldFeature> featureSet = new HashSet<IWorldFeature>(features.values());
		for (IWorldFeature ft : featureSet) {
			NBTTagCompound ftag = new NBTTagCompound();
			ftag.setString("identifier", ft.getProvider().identifier);
			
			NBTTagCompound dataTag = new NBTTagCompound();
			ft.writeNBTData(dataTag);
			ftag.setCompoundTag("data", dataTag);
			
			list.appendTag(ftag);
		}
		nbt.setTag("features", list);
	}
	
	public void registerForSave() {
		DimensionManager.getWorld(0).setItemData(getUID(), this);
	}
	
	protected void validateAndFix() {
		for (FeatureType ftype : FeatureType.values()) {
			if (ftype == FeatureType.ALL) continue;
			
			// Make sure we have singleton classes
			if (ftype.isSingleton()) {
				int count = getFeatures(ftype).size();
				if (count == 0) {
					for (IWorldFeature feature : WorldDataGenerator.generateRandomFeatureType(this, ftype, 1, new Random())) {
						addFeature(ftype, feature);
					}
					if (getFeature(ftype) == null) throw new RuntimeException(
							String.format("Missing feature type %s and could not generate a new one", ftype.toString()));
					
				} else if (count > 1) {
					throw new RuntimeException(
							String.format("Had more than one instance of singelton feature type %s", ftype.toString()));
				}
			}
		}
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
	
	public long getWorldSeed() {
		return seed;
	}
	
	@Override
	public WorldProvider getWorldProvider() {
		return worldProvider;
	}
	
	public void setWorldProvider(SGWorldProvider pvdr) {
		this.worldProvider = pvdr;
	}
	
	/**
	 * Returns the first found feature of the specified type. Mainly for use when there is only one of that type.
	 */
	@Override
	public IWorldFeature getFeature(FeatureType type) { // TODO Consider using plural and for loop all the time. Keeps us safe when the client doesn't have everything yet.
		List<IWorldFeature> tfeats = getFeatures(type);
		return tfeats.size() > 0 ? tfeats.get(0) : null;
	}
	
	@Override
	public List<IWorldFeature> getFeatures(FeatureType type) {
		if (type == FeatureType.ALL) return new ArrayList<IWorldFeature>(features.values());
		return new ArrayList<IWorldFeature>(features.get(type));
	}

	public void addFeature(FeatureType type, IWorldFeature feature) {
		this.features.put(type, feature);
		
		List<FeatureType> secondaryTypes = new ArrayList<FeatureType>();
		feature.getSecondaryTypes(secondaryTypes);
		for (FeatureType stype : secondaryTypes) {
			if (stype == type) continue;
			if (stype.isSingleton()) {
				throw new IllegalArgumentException(String.format("The class \"%s\" tried to register \"%s\" (a singleton feature type) as a secondary feature type.",
						feature.getClass().getName(), stype.name()));
			}
			this.features.put(stype, feature);
		}
		
		markDirty();
	}
	
	public String getUID() {
		return getUID(designation);
	}
	
	public static String getUID(String designation) {
		return "sgworlddata_" + designation;
	}
	
	public static IWorldData loadData(String designation) {
		World overWorld = DimensionManager.getWorld(0);
		String ufn = getUID(designation);
		SGWorldData data = (SGWorldData)overWorld.loadItemData(SGWorldData.class, ufn);
		
		if (data == null) {
			FMLLog.severe("Could not load world data for SGWorlds world %s", designation);
			data = new WorldDataGenerator().generateRandomWorld();
			overWorld.setItemData(ufn, data);
			data.markDirty();
		}
		return data;
	}
}
