package ml.sgworlds.world.gen.structure.deserthold;

import java.util.List;
import java.util.Random;

import ml.sgworlds.world.gen.StructureBuilder;
import net.minecraft.block.Block;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;


public class ComponentHallHub extends ComponentDesertHold {

	public ComponentHallHub() {}
	
	public ComponentHallHub(ChunkCoordinates position, int rotation) {
		super(position, rotation);
		setLocalBoundingBox(-3, -1, -3, 3, 4, 3);
	}
	
	@Override
	public void buildComponent(StructureComponent par1StructureComponent, List par2List, Random par3Random) {
		ComponentHoldStart start = (ComponentHoldStart) par1StructureComponent;
		
		
		
		super.buildComponent(par1StructureComponent, par2List, par3Random);
	}
	
	@Override
	protected boolean addComponentParts(StructureBuilder b, World world, Random rand, StructureBoundingBox chunkBox) {
		b.fillArea(-3, 0, -3, 3, 3, 3, null, 0);
		
		b.wallArea(-3, 0, -3, 3, 4, 3, Block.sandStone, 2);
		b.borderArea(-2, 3, -2, 2, 3, 2, Block.sandStone, 2);
		
		b.symmetryX = b.symmetryZ = true;
		b.setBlockAt(1, 1, 2, Block.sandStone, 2);
		b.setBlockAt(1, 2, 2, Block.sandStone, 1);
		b.setBlockAt(2, 2, 3, Block.sandStone, 1);
		
		b.setBlockAt(2, 1, 1, Block.sandStone, 2);
		b.setBlockAt(2, 2, 1, Block.sandStone, 1);
		b.setBlockAt(3, 2, 2, Block.sandStone, 1);
		b.symmetryX = b.symmetryZ = false;
		
		if (componentNorth) b.fillArea(-1, 1, 2, 1, 3, 3, null, 0);
		if (componentSouth) b.fillArea(-1, 1,-3, 1, 3,-2, null, 0);
		if (componentEast) b.fillArea( 2, 1,-1, 3, 3, 1, null, 0);
		if (componentWest) b.fillArea(-3, 1,-1,-2, 3, 1, null, 0);
		
		return true;
	}

}