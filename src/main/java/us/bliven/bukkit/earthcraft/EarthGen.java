package us.bliven.bukkit.earthcraft;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import us.bliven.bukkit.earthcraft.gis.DataUnavailableException;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.MapProjection;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Generates real-world maps
 * @author Spencer Bliven
 */
public class EarthGen extends ChunkGenerator {
	private MapProjection projection;
	private ElevationProvider elevation;
	private Coordinate spawn;
	
	public EarthGen( MapProjection projection, ElevationProvider elevation, Coordinate spawn) {
		super();
		this.projection = projection;
		this.elevation = elevation;
		this.spawn = spawn;
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
			
		} catch (DataUnavailableException e) {
			System.out.println("Data unavailable at "+worldx+","+worldz);
			coord.z = 1; //default to just above sea level
		}
		// translate elevation to blocks
		root = projection.coordinateToLocation(world, coord);
		height = (int) Math.floor(root.getY()+1);
		
		if(height>world.getMaxHeight()) height = world.getMaxHeight();
//		return (int)(Math.random()*20);
		return height;
	}



	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		Double elev;
		try {
			elev = elevation.fetchElevation(spawn);
		} catch (DataUnavailableException e) {
			elev = 1.;//1 m above sea level
		}
		Location spawnloc = projection.coordinateToLocation(world, new Coordinate(spawn.x,elev,spawn.z));
		return spawnloc;
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