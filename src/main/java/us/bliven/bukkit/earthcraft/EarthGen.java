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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.NumberConversions;

import us.bliven.bukkit.earthcraft.gis.BiomeProvider;
import us.bliven.bukkit.earthcraft.gis.DataUnavailableException;
import us.bliven.bukkit.earthcraft.gis.DefaultBiomeProvider;
import us.bliven.bukkit.earthcraft.gis.ElevationProjection;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.EquirectangularProjection;
import us.bliven.bukkit.earthcraft.gis.FlatElevationProvider;
import us.bliven.bukkit.earthcraft.gis.LinearElevationProjection;
import us.bliven.bukkit.earthcraft.gis.MapProjection;
import us.bliven.bukkit.earthcraft.gis.ProjectionTools;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Generates real-world maps
 * @author Spencer Bliven
 */
public class EarthGen extends ChunkGenerator implements Configurable {
	Logger log;

	private MapProjection mapProjection;
	private ElevationProjection elevationProjection;
	private ElevationProvider elevationProvider;
	private BiomeProvider biomeProvider;

	private Coordinate spawn;

	private final int defaultBlockHeight = 1;

	private int seaLevel; // 1st block above water
	private int sandLevel; // 1st block above the beach

	private boolean spawnOcean;

	private Plugin plugin;

	public EarthGen(Plugin plugin, MapProjection mapProjection,
			ElevationProjection elevProjection, ElevationProvider elevation,
			BiomeProvider biome, Coordinate spawn) {
		super();
		this.plugin = plugin;
		this.mapProjection = mapProjection;
		this.elevationProjection = elevProjection;
		this.elevationProvider = elevation;
		this.biomeProvider = biome;
		this.spawn = spawn;

		postInit();
	}

	/**
	 * Reset parameters after initializing the main parameters
	 * @param plugin
	 */
	private void postInit() {
		this.log = null;
		if( plugin != null ) {
			this.log = plugin.getLogger();
		}
		if(this.log == null) {
			this.log = Logger.getLogger(EarthGen.class.getName());
		}

		this.seaLevel = NumberConversions.floor(elevationProjection.elevationToY(0.));
		this.sandLevel = NumberConversions.floor(elevationProjection.elevationToY(2.));

		this.spawnOcean = true;
	}

	public EarthGen(Plugin plugin) {
		this(plugin, new EquirectangularProjection(), new LinearElevationProjection(),
				new FlatElevationProvider(), new DefaultBiomeProvider(),
				new Coordinate( 0, 0) );
	}

	/**
	 * @deprecated Use only with {@link #initFromConfig}
	 */
	@Deprecated
	public EarthGen() {
		this(null);
	}

	@Override
	public void initFromConfig(ConfigManager config, ConfigurationSection params) {
		plugin = config.getPlugin();

		for(String param : params.getKeys(false)) {
			if( param.equalsIgnoreCase("mapProjection") ) {
				mapProjection = config.createSingleConfigurable(MapProjection.class,
						params.getConfigurationSection(param),
						mapProjection);
			} else if( param.equalsIgnoreCase("elevationProjection") ) {
				elevationProjection = config.createSingleConfigurable(ElevationProjection.class,
						params.getConfigurationSection(param),
						elevationProjection);
			} else if( param.equalsIgnoreCase("spawn") ) {
				spawn = config.getCoordinate(params,param,spawn);
			} else if( param.equalsIgnoreCase("sources") ) {
				// initialize data sources
				initSourcesFromConfig(config,params.getConfigurationSection(param));
			} else if( param.equalsIgnoreCase("spawnOcean") ) {
				spawnOcean = params.getBoolean(param);
			} else {
				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
			}
		}

    	// Check that the ElevationProvider is working
    	try {
			elevationProvider.fetchElevation(spawn);
		} catch (DataUnavailableException e) {
			log.log(Level.SEVERE, "Unable to load elevation provider!",e);
			elevationProvider = new FlatElevationProvider();
		}

		// Clean up after changes
		postInit();
	}

	/**
	 * Initialize data sources. For instance, elevation, biomes, hydrology, etc
	 * @param config
	 * @param params
	 */
	private void initSourcesFromConfig(ConfigManager config,
			ConfigurationSection params) {
		for(String param : params.getKeys(false)) {
			if( param.equalsIgnoreCase("elevation") ) {
				elevationProvider = config.createSingleConfigurable(ElevationProvider.class,
						params.getConfigurationSection(param),
						elevationProvider);
			} else if( param.equalsIgnoreCase("biome") ) {
				biomeProvider = config.createSingleConfigurable(BiomeProvider.class,
						params.getConfigurationSection(param),
						biomeProvider);
			} else {
				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
			}
		}
	}

	public int getSeaLevel() {
		return seaLevel;
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
		//TODO spawn on safe land
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

				// Get lat/lon
				int worldx = chunkx*16+x;
				int worldz = chunkz*16+z;
				Coordinate coord = getLatLon(world, worldx, worldz );

				// Get elevation
				int height;
				try {
					height = getBlockHeight(world,coord);
				} catch (DataUnavailableException e) {
					// Severe but expected exception
					log.log(Level.SEVERE,"Data unavailable at "+worldx+","+worldz,e);
					height = defaultBlockHeight;
				} catch (Exception e) {
					// Unexpected exception; indicates a bug
					log.log(Level.SEVERE,"[Bug] Unexpected error fetching height at " +
							worldx+","+worldz +
							" (" + ProjectionTools.latlonString(coord) + ")", e);
					height = defaultBlockHeight;
				}

				// Set the biome
				//setBiome(x,height,z,biomes);
				biomeProvider.setBiome(this, biomes, coord, x,z);



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

	protected Coordinate getLatLon(World world, int worldx, int worldz) {
		Location root = new Location(world,worldx,0,worldz);
		// Translate x/z to lat/lon
		Coordinate coord = mapProjection.locationToCoordinate(root);
		return coord;
	}

	/**
	 * Calculate the elevation for a position
	 * @param world
	 * @param worldx
	 * @param worldz
	 * @return
	 */
	public int getBlockHeight(World world, Coordinate coord) throws DataUnavailableException{

		// get elevation in m
		Double elev  = elevationProvider.fetchElevation(coord);

		// translate elevation to blocks
		double y = elevationProjection.elevationToY(elev);

		if( y == Double.NaN ) {
			return defaultBlockHeight;
		} else {
			return NumberConversions.floor(Math.min(y, world.getMaxHeight()));
		}
	}


	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		// Project latlon
		Location spawnloc = mapProjection.coordinateToLocation(world, new Coordinate(spawn.x,spawn.y));

		// Project elevation
		Double elev;
		try {
			elev = elevationProvider.fetchElevation(spawn);
		} catch (DataUnavailableException e) {
			elev = Double.NaN;
		}
		double y = elevationProjection.elevationToY(elev);

		spawnloc.setY(y);

		return spawnloc;
	}

	public MapProjection getMapProjection() {
		return mapProjection;
	}


	public ElevationProvider getElevationProvider() {
		return elevationProvider;
	}

	public ElevationProjection getElevationProjection() {
		return elevationProjection;
	}

	public BiomeProvider getBiomeProvider() {
		return biomeProvider;
	}

	public Coordinate getSpawn() {
		return spawn;
	}


}