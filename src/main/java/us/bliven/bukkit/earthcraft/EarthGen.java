package us.bliven.bukkit.earthcraft;

import io.github.lucariatias.bukkitpopulators.BukkitPopulators;
import io.github.lucariatias.bukkitpopulators.OrePopulator;
import io.github.lucariatias.bukkitpopulators.populators.GlowstoneReefPopulator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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


	// Store populator names at init for later instantiation
	private String populatorSet;
	private List<String> populatorNames;

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
		this.populatorSet = "BukkitPopulators";
		this.populatorNames = new ArrayList<String>();

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
			} else if( param.equalsIgnoreCase("populatorSet") ) {
				populatorSet = params.getString(param,populatorSet);
			} else if( param.equalsIgnoreCase("populators") ) {
				populatorNames.addAll(params.getStringList(param));
			} else  {
				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
			}
		}
	}

	public int getSeaLevel() {
		return seaLevel;
	}

	@Override
	public List<BlockPopulator> getDefaultPopulators(World world) {
		List<BlockPopulator> populators = new ArrayList<BlockPopulator>();
		addPopulatorSet(world, populatorSet, populators);
		addPopulators(world, populatorNames, populators);
		return populators;
	}

	private void addPopulatorSet(World world, String setName, List<BlockPopulator> populators) {
		PluginManager pm = plugin.getServer().getPluginManager();

		log.info("No populators.");

		if( setName.equalsIgnoreCase("Bare")) {
			// No populators
		} else if( setName.equalsIgnoreCase("BukkitPopulators")) {
			BukkitPopulators bukkitpopulators = (BukkitPopulators)pm.getPlugin("BukkitPopulators");
			if(bukkitpopulators != null) {
				List<BlockPopulator> pops =  bukkitpopulators.getDefaultPopulators(world);
				log.info("Read "+pops.size()+" populators from BukkitPopulators");
				populators.addAll(pops);
			} else {
				log.severe("BukkitPopulators plugin not installed");
			}
		} else {
			log.severe("Unknown populatorSet "+setName);
		}
	}
	private void addPopulators(World world, List<String> classNames, List<BlockPopulator> populators) {
		ClassLoader cl = plugin.getClass().getClassLoader();

		// packages to search for classes, in order
		List<String> packages = new ArrayList<String>();
		packages.add("");
		PluginManager pm = plugin.getServer().getPluginManager();
		BukkitPopulators bukkitpopulators = (BukkitPopulators)pm.getPlugin("BukkitPopulators");
		if(bukkitpopulators != null) {
			packages.add(OrePopulator.class.getPackage().getName());
			packages.add(GlowstoneReefPopulator.class.getPackage().getName());
		}

		for(String className: classNames) {
			Class<?> klass = null;
			for(String pkg : packages) {
				try {
					String fullName = (pkg.isEmpty()?"":(pkg+"."))+className;
					klass = cl.loadClass(fullName);
					//log.info("Found "+pkg+className);
					break;
				} catch (ClassNotFoundException e) {
					//not in this package
					//log.info("Unable to find "+pkg+className);
				}
			}
			if( klass == null) {
				log.severe("Unable to find populator "+className);
				continue;
			}

			BlockPopulator pop;
			try {
				Constructor<?> constructor = klass.getConstructor();
				pop = (BlockPopulator) constructor.newInstance();
			} catch (ClassCastException e) {
				// Not a BlockPopulator
				log.severe(className+" is not a BlockPopulator.");
				continue;
			} catch( NoSuchMethodException e) {
				// No default constructor
				log.severe("Unable to use "+className+" because it lacks a default constructor");
				continue;
			} catch (IllegalArgumentException e) {
				// Shouldn't happen–bad argument types
				log.log(Level.SEVERE,"[Bug] Error with constructor arguments to "+className);
				continue;
			} catch (InstantiationException e) {
				// Abstract class
				log.log(Level.SEVERE,"Can't instantiate abstract class "+className,e);
				continue;
			} catch (IllegalAccessException e) {
				// constructor is private
				log.log(Level.SEVERE,className+" lacks a public default constructor");
				continue;
			} catch (InvocationTargetException e) {
				// Constructor threw an exception
				log.log(Level.SEVERE, "Exception while creating "+className,e);
				continue;
			}

			populators.add(pop);
		}
	}

	/**
	 * Spawn everywhere
	 */
	@Override
	public boolean canSpawn(World world, int x, int z) {
		Block highest = world.getHighestBlockAt(x, z);
		if( highest.isLiquid() )
			return false;
		Material mat = highest.getType();
		if( mat == Material.FIRE )
			return false;
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