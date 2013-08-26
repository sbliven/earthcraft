package us.bliven.bukkit.earthcraft;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.BlockChangeDelegate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Difficulty;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.Vector;

import com.vividsolutions.jts.geom.Coordinate;

import us.bliven.bukkit.earthcraft.gis.DataUnavailableException;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.MapProjection;

/**
 * Generates real-world maps
 * @author Spencer Bliven
 */
public class EarthGen extends ChunkGenerator {
	private MapProjection projection;
	private ElevationProvider elevation;
	
	public EarthGen(MapProjection projection, ElevationProvider elevation) {
		super();
		this.projection = projection;
		this.elevation = elevation;
		
	}
	
	
	@Override
	public List<BlockPopulator> getDefaultPopulators(World world) {
		return Arrays.asList();//super.getDefaultPopulators(world); 
		//Arrays.asList((BlockPopulator) new BlankPopulator());
	}

	/**
	 * Spawn everywhere
	 */
	@Override
	public boolean canSpawn(World world, int x, int z) {
		return true;
	}
	
	/**
	 * This converts relative chunk locations to bytes that can be written to the chunk
	 */
	protected int xyzToByte(int x, int y, int z) {
		return (x * 16 + z) * 256 + y;
	}

	
	@Override
	public byte[] generate(World world, Random rand, int chunkx, int chunkz) {
		int chunkHeight = world.getMaxHeight();
		byte[] result = new byte[16*16*chunkHeight];

		
		

		for(int x=0; x<16; x++){
			for(int z=0; z<16; z++) {
				int y = 0;
				//This will set the floor of each chunk at bedrock level to bedrock
				result[xyzToByte(x,y,z)] = (byte) Material.BEDROCK.getId();

				int height = getBlockHeight(world, chunkx*16+x, chunkz*16+z);
				//System.out.println("Height of chunk"+chunkx+","+chunkz+"="+height);
				y++;
				for(;y<height && y<256;y++) {
					result[xyzToByte(x,y,z)] = (byte) Material.GRASS.getId();
				}
			}
		}
		return result;
	}
	
	public int getBlockHeight(World world, int worldx, int worldz) {
		int height = 1;
		Location root = new Location(world,worldx,0,worldz);
		// Translate x/z to lat/lon
		Coordinate coord = projection.locationToCoordinate(root);
		List<Double> heights;
		try {
			// get elevation in m
			heights = elevation.fetchElevations(Arrays.asList(coord));

			if(heights != null) {
				assert(heights.size() == 1);
				
				Double h = heights.get(0);
				if(h != null && h != Double.NaN) {
					coord.z = h;
				}
			}
			
			// translate elevation to blocks
			root = projection.coordinateToLocation(world, coord);
			height = (int) Math.floor(root.getY()+1);
			
			if(height>world.getMaxHeight()) height = world.getMaxHeight();
		} catch (DataUnavailableException e) {
			e.printStackTrace();
		}
//		return (int)(Math.random()*20);
		return height;
	}



	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		Location spawn = new Location(world, 0, 64, 0);
		return spawn;
	}
	
	

	/*public static void main(String[] args) {
		World world = new StubWorld();
		EarthcraftPlugin plugin = new EarthcraftPlugin();
		plugin.onEnable();
		plugin.onLoad();
			EarthGen gen = (EarthGen)plugin.getDefaultWorldGenerator("world", "foo");
			int h;
			h = gen.getBlockHeight(world,10,10);
			System.out.println(h);
		
	}*/
}