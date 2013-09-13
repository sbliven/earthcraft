package us.bliven.bukkit.earthcraft;

import io.github.lucariatias.bukkitpopulators.BukkitPopulators;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import us.bliven.bukkit.earthcraft.gis.DataUnavailableException;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.MapProjection;
import us.bliven.bukkit.earthcraft.gis.ProjectionTools;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Generates real-world maps
 * @author Spencer Bliven
 */
public class EarthGen extends ChunkGenerator {
	Logger log;

	private final MapProjection projection;
	private final ElevationProvider elevation;
	private final Coordinate spawn;

	private final int defaultBlockHeight = 1;

	private int seaLevel; // 1st block above water
	private int sandLevel; // 1st block above the beach

	private boolean spawnOcean;

	private Plugin plugin;

	public EarthGen(Plugin plugin, MapProjection projection, ElevationProvider elevation, Coordinate spawn) {
		super();
		this.plugin = plugin;
		this.projection = projection;
		this.elevation = elevation;
		this.spawn = spawn;
		this.log = plugin.getLogger();
		if(this.log == null) this.log = Logger.getLogger(EarthGen.class.getName());

		Location origin = projection.coordinateToLocation(null, new Coordinate(0,0,0));
		this.seaLevel = origin.getBlockY();

		Location sand = projection.coordinateToLocation(null, new Coordinate(0,0,2));
		this.sandLevel = sand.getBlockY();

//		log.info("Sea level at "+seaLevel);
//		log.info("Beach level at "+sandLevel);

		this.spawnOcean = true;
	}


	public int getSeaLevel() {
		return seaLevel;
	}


	public void setSeaLevel(int seaLevel) {
		this.seaLevel = seaLevel;
	}


	public int getSandLevel() {
		return sandLevel;
	}


	public void setSandLevel(int sandLevel) {
		this.sandLevel = sandLevel;
	}


	public boolean isSpawnOcean() {
		return spawnOcean;
	}


	public void setSpawnOcean(boolean spawnOcean) {
		this.spawnOcean = spawnOcean;
	}


	public MapProjection getProjection() {
		return projection;
	}


	public ElevationProvider getElevation() {
		return elevation;
	}


	public int getDefaultBlockHeight() {
		return defaultBlockHeight;
	}


	@Override
	public List<BlockPopulator> getDefaultPopulators(World world) {
		PluginManager pm = plugin.getServer().getPluginManager();
		BukkitPopulators bukkitpopulators = (BukkitPopulators)pm.getPlugin("BukkitPopulators");
		if(bukkitpopulators != null) {
			List<BlockPopulator> pops =  bukkitpopulators.getDefaultPopulators(world);
			log.info("Read "+pops.size()+" populators from BukkitPopulators");
			return pops;
		}
		log.info("No populators.");

		return super.getDefaultPopulators(world);
	}

	/**
	 * Spawn everywhere
	 */
	@Override
	public boolean canSpawn(World world, int x, int z) {
		return true;
	}

	/**
	 * helper function for generate
	 */
	private void setBlock(byte[][] result, int x, int y, int z, byte blkid) {
	    if (result[y >> 4] == null) {
	        result[y >> 4] = new byte[4096];
	    }
	    result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
	}

	@Override
	@Deprecated
	public byte[][] generateBlockSections(World world, Random random, int chunkx,
			int chunkz, BiomeGrid biomes) {
		byte[][] result = new byte[16][];


		for(int x=0; x<16; x++){
			for(int z=0; z<16; z++) {
				int y = 0;
				//This will set the floor of each chunk at bedrock level to bedrock
				setBlock(result,x,y,z, (byte) Material.BEDROCK.getId() );
				y++;

				int height = getBlockHeight(world, chunkx*16+x, chunkz*16+z);

				setBiome(x,height,z,biomes);
				int stoneHeight = height - 16;

				for(;y<stoneHeight; y++) {
					setBlock(result,x,y,z, (byte) Material.STONE.getId() );
				}

				if( height > this.sandLevel ) {
					// Land
					for(;y< height-1;y++) {
						setBlock(result,x,y,z, (byte) Material.DIRT.getId() );
					}
					if(y<height) {
						setBlock(result,x,y,z, (byte) Material.GRASS.getId() );
						y++;
					}

				} else {
					// Ocean or beach
					if( y < height-1 ) {
						setBlock(result,x,y,z, (byte) Material.SANDSTONE.getId());
						y++;
					}
					for(;y< height;y++) {
						setBlock(result,x,y,z, (byte) Material.SAND.getId() );
					}
				}
				for(;y<seaLevel && spawnOcean ;y++) {
					setBlock(result,x,y,z, (byte) Material.STATIONARY_WATER.getId() );
				}
			}
		}
		return result;
	}

	private void setBiome(int x, int height, int z, BiomeGrid biomes) {
		int mountainLevel = (seaLevel+256)/2;
		if(height < seaLevel) {
			biomes.setBiome(x, z, Biome.OCEAN);
		} else if( height <= sandLevel) {
			biomes.setBiome(x, z, Biome.BEACH);
		} else if( height <= mountainLevel) {
			biomes.setBiome(x, z, Biome.FOREST_HILLS);
		} else {
			biomes.setBiome(x, z, Biome.ICE_MOUNTAINS);
		}

	}


	/**
	 * Calculate the elevation for a position
	 * @param world
	 * @param worldx
	 * @param worldz
	 * @return
	 */
	public int getBlockHeight(World world, int worldx, int worldz) {
		int height = 1;
		Location root = new Location(world,worldx,0,worldz);
		// Translate x/z to lat/lon
		Coordinate coord = projection.locationToCoordinate(root);
		try {
			// get elevation in m
			Double h = elevation.fetchElevation(coord);

			if(h != null) {
				coord.z = h;
			} else {
				coord.z = Double.NaN;
			}

		} catch (DataUnavailableException e) {
			// Severe but expected exception
			log.log(Level.SEVERE,"Data unavailable at "+worldx+","+worldz,e);
			coord.z = Double.NaN;
		} catch (Exception e) {
			// Unexpected exception; indicates a bug
			log.log(Level.SEVERE,"[Bug] Unexpected error fetching height at " +
					worldx + "," + worldz +
					" (" + ProjectionTools.latlonString(coord) + ")", e);
			coord.z = Double.NaN;
		}
		// translate elevation to blocks
		root = projection.coordinateToLocation(world, coord);

		if(root.getY() == Double.NaN ) {
			height = defaultBlockHeight;
		} else {
			height = root.getBlockY();
		}

		if(height>world.getMaxHeight()) {
			height = world.getMaxHeight();
		}

		if(worldx == -170000 && worldz == -60000) {
			log.info(String.format("Height at %d,%d (%s) = %f -> %d (Maybe %d?)",
					worldx,worldz, ProjectionTools.latlonString(coord),
					root.getY(), height, root.getBlockY()) );
		}


		return height;
	}


	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		Double elev;
		try {
			elev = elevation.fetchElevation(spawn);
		} catch (DataUnavailableException e) {
			elev = Double.NaN;
		}
		Location spawnloc = projection.coordinateToLocation(world, new Coordinate(spawn.x,spawn.y,elev));
		if(Double.isNaN(spawnloc.getY())) {
			spawnloc.setY( defaultBlockHeight );
		}

		return spawnloc;
	}

	public MapProjection getMapProjection() {
		return projection;
	}


	public ElevationProvider getElevationProvider() {
		return elevation;
	}


	public Coordinate getSpawn() {
		return spawn;
	}



}