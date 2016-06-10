package us.bliven.bukkit.earthcraft.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkProviderOverworld;
import us.bliven.bukkit.earthcraft.EarthcraftMod;
import us.bliven.bukkit.earthcraft.biome.CoordBiomeProvider;
import us.bliven.bukkit.earthcraft.gis.DataUnavailableException;
import us.bliven.bukkit.earthcraft.gis.ElevationProjection;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.Location;
import us.bliven.bukkit.earthcraft.gis.MapProjection;
import us.bliven.bukkit.earthcraft.gis.ProjectionTools;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Generates real-world maps
 * @author Spencer Bliven
 */
public class EarthChunkProvider extends ChunkProviderOverworld {// implements Configurable {
	private static final double WORLD_HEIGHT = 256; //TODO parameterize?

	Logger log;

	private MapProjection mapProjection;
	private ElevationProjection elevationProjection;
	private ElevationProvider elevationProvider;
	private CoordBiomeProvider biomeProvider;

	private Coordinate spawn;

	private final int defaultBlockHeight = 1;

	private int seaLevel; // 1st block above water

	private boolean spawnOcean;

	private EarthcraftMod plugin;


	// Store populator names at init for later instantiation
	private String populatorSet;
	private List<String> populatorNames;

	public EarthChunkProvider(EarthcraftMod plugin, World world, long seed,
			boolean mapFeaturesEnabledIn, String settingsJson,
			MapProjection mapProjection,
			ElevationProjection elevProjection, ElevationProvider elevation,
			CoordBiomeProvider biome, Coordinate spawn) {
		super(world, seed, mapFeaturesEnabledIn, settingsJson);
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
			this.log = Logger.getLogger(EarthChunkProvider.class.getName());
		}

		this.seaLevel = (int) Math.floor(elevationProjection.elevationToY(0.));

		this.spawnOcean = true;
	}

//	public EarthGen(Plugin plugin) {
//		this(plugin, new EquirectangularProjection(), new LinearElevationProjection(),
//				new FlatElevationProvider(), new DefaultBiomeProvider(),
//				new Coordinate( 0, 0) );
//	}

//	/**
//	 * @deprecated Use only with {@link #initFromConfig}
//	 */
//	@Deprecated
//	public EarthGen() {
//		this(null);
//	}
//
//	@Override
//	public void initFromConfig(ConfigManager config, ConfigurationSection params) {
//		plugin = config.getPlugin();
//
//		for(String param : params.getKeys(false)) {
//			if( param.equalsIgnoreCase("mapProjection") ) {
//				mapProjection = config.createSingleConfigurable(MapProjection.class,
//						params.getConfigurationSection(param),
//						mapProjection);
//			} else if( param.equalsIgnoreCase("elevationProjection") ) {
//				elevationProjection = config.createSingleConfigurable(ElevationProjection.class,
//						params.getConfigurationSection(param),
//						elevationProjection);
//			} else if( param.equalsIgnoreCase("spawn") ) {
//				spawn = config.getCoordinate(params,param,spawn);
//			} else if( param.equalsIgnoreCase("sources") ) {
//				// initialize data sources
//				initSourcesFromConfig(config,params.getConfigurationSection(param));
//			} else if( param.equalsIgnoreCase("spawnOcean") ) {
//				spawnOcean = params.getBoolean(param);
//			} else {
//				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
//			}
//		}
//
//    	// Check that the ElevationProvider is working
//    	try {
//			elevationProvider.fetchElevation(spawn);
//		} catch (DataUnavailableException e) {
//			log.log(Level.SEVERE, "Unable to load elevation provider!",e);
//			elevationProvider = new FlatElevationProvider();
//		}
//
//		// Clean up after changes
//		postInit();
//	}
//
//	/**
//	 * Initialize data sources. For instance, elevation, biomes, hydrology, etc
//	 * @param config
//	 * @param params
//	 */
//	private void initSourcesFromConfig(ConfigManager config,
//			ConfigurationSection params) {
//		for(String param : params.getKeys(false)) {
//			if( param.equalsIgnoreCase("elevation") ) {
//				elevationProvider = config.createSingleConfigurable(ElevationProvider.class,
//						params.getConfigurationSection(param),
//						elevationProvider);
//			} else if( param.equalsIgnoreCase("biome") ) {
//				biomeProvider = config.createSingleConfigurable(CoordBiomeProvider.class,
//						params.getConfigurationSection(param),
//						biomeProvider);
//			} else if( param.equalsIgnoreCase("populatorSet") ) {
//				populatorSet = params.getString(param,populatorSet);
//			} else if( param.equalsIgnoreCase("populators") ) {
//				populatorNames.addAll(params.getStringList(param));
//			} else  {
//				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
//			}
//		}
//	}

	public int getSeaLevel() {
		return seaLevel;
	}

//	@Override
//	public List<BlockPopulator> getDefaultPopulators(World world) {
//		List<BlockPopulator> populators = new ArrayList<BlockPopulator>();
//		addPopulatorSet(world, populatorSet, populators);
//		addPopulators(world, populatorNames, populators);
//		return populators;
//	}
//
//	private void addPopulatorSet(World world, String setName, List<BlockPopulator> populators) {
//		PluginManager pm = plugin.getServer().getPluginManager();
//
//		log.info("No populators.");
//
//		if( setName.equalsIgnoreCase("Bare")) {
//			// No populators
//		} else if( setName.equalsIgnoreCase("BukkitPopulators")) {
//			BukkitPopulators bukkitpopulators = (BukkitPopulators)pm.getPlugin("BukkitPopulators");
//			if(bukkitpopulators != null) {
//				List<BlockPopulator> pops =  bukkitpopulators.getDefaultPopulators(world);
//				log.info("Loaded "+pops.size()+" populators from BukkitPopulators");
//				populators.addAll(pops);
//			} else {
//				log.severe("BukkitPopulators plugin not installed");
//			}
//		} else {
//			log.severe("Unknown populatorSet "+setName);
//		}
//	}
//	private void addPopulators(World world, List<String> classNames, List<BlockPopulator> populators) {
//		ClassLoader cl = plugin.getClass().getClassLoader();
//
//		// Load bukkitPopulators if installed
//		PluginManager pm = plugin.getServer().getPluginManager();
//		BukkitPopulators bukkitpopulators = (BukkitPopulators)pm.getPlugin("BukkitPopulators");
//
//		// Build list of known populators.
//		List<BlockPopulator> availablePopulators;
//		if(bukkitpopulators != null) {
//			availablePopulators =  bukkitpopulators.getAllPopulators(world);
//		} else {
//			availablePopulators = Arrays.asList();
//		}
//
//		// packages to search for classes, in order
//		List<String> packages = new ArrayList<String>();
//		packages.add("");
//		if(bukkitpopulators != null) {
//			packages.add(OrePopulator.class.getPackage().getName());
//			packages.add(GlowstoneReefPopulator.class.getPackage().getName());
//		}
//
//		for(String className: classNames) {
//
//			BlockPopulator populator = null;
//
//			// First search available populators for the className
//			for(BlockPopulator pop : availablePopulators) {
//				Class<? extends BlockPopulator> klass = pop.getClass();
//				// Match the full name case-sensitive, but ignore case for simple name
//				if( klass.getName().equals(className) ||
//						klass.getSimpleName().equalsIgnoreCase(className) ) {
//					populator = pop;
//					break;
//				}
//			}
//
//			if(populator != null) {
//				// Found instance among known populators
//				log.info("Loaded populator "+className);
//				populators.add(populator);
//				continue;
//			}
//
//			// Not found, so try to find class in known packages
//			Class<?> klass = null;
//			for(String pkg : packages) {
//				try {
//					String fullName = (pkg.isEmpty()?"":(pkg+"."))+className;
//					klass = cl.loadClass(fullName);
//					//log.info("Found "+pkg+className);
//					break;
//				} catch (ClassNotFoundException e) {
//					//not in this package
//					//log.info("Unable to find "+pkg+className);
//				}
//			}
//			if( klass == null) {
//				log.severe("Unable to find populator "+className);
//				continue;
//			}
//
//			// Create the instance
//			try {
//				Constructor<?> constructor = klass.getConstructor();
//				populator = (BlockPopulator) constructor.newInstance();
//			} catch (ClassCastException e) {
//				// Not a BlockPopulator
//				log.severe(className+" is not a BlockPopulator.");
//				continue;
//			} catch( NoSuchMethodException e) {
//				// No default constructor
//				log.severe("Unable to use "+className+" because it lacks a default constructor");
//				continue;
//			} catch (IllegalArgumentException e) {
//				// Shouldn't happen–bad argument types
//				log.log(Level.SEVERE,"[Bug] Error with constructor arguments to "+className);
//				continue;
//			} catch (InstantiationException e) {
//				// Abstract class
//				log.log(Level.SEVERE,"Can't instantiate abstract class "+className,e);
//				continue;
//			} catch (IllegalAccessException e) {
//				// constructor is private
//				log.log(Level.SEVERE,className+" lacks a public default constructor");
//				continue;
//			} catch (InvocationTargetException e) {
//				// Constructor threw an exception
//				log.log(Level.SEVERE, "Exception while creating "+className,e);
//				continue;
//			}
//			
//			log.info("Loaded populator "+className);
//			populators.add(populator);
//		}
//	}

//	/**
//	 * Spawn everywhere
//	 */
//	@Override
//	public boolean canSpawn(World world, int x, int z) {
//		Block highest = world.getHighestBlockAt(x, z);
//		if( highest.isLiquid() )
//			return false;
//		Material mat = highest.getType();
//		if( mat == Material.FIRE )
//			return false;
//		return true;
//	}
	
//	/**
//	 * helper function for generate
//	 */
//	@SuppressWarnings("deprecation")
//	private void setBlock(byte[][] result, int x, int y, int z, Material material) {
//	    if (result[y >> 4] == null) {
//	        result[y >> 4] = new byte[4096];
//	    }
//	    result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = (byte) material.getId();
//	}

	@Override
	public void setBlocksInChunk(int cx, int cz, ChunkPrimer primer) {
//	@Override
//	@Deprecated
//	public byte[][] generateBlockSections(World world, Random random, int chunkx,
//			int chunkz, BiomeGrid biomes) {
//		byte[][] result = new byte[16][];
//
//
		for(int lx=0; lx<16; lx++){
			for(int lz=0; lz<16; lz++) {
				int y = 0;
				//This will set the floor of each chunk at bedrock level to bedrock
				primer.setBlockState(lx,y,lz, Blocks.bedrock.getDefaultState());
				y++;

				// Get lat/lon
				int worldx = cx*16+lx;
				int worldz = cz*16+lz;
				Coordinate coord = getLatLon( worldx, worldz );

				// Get elevation
				int height;
				try {
					height = getBlockHeight(coord);
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
//
//				// Set the biome
//				Biome biome = biomeProvider.getBiome(this, world, coord);
//				biomes.setBiome(x, z, biome);
//
//
				int stoneHeight = height - 16;

				for(;y<stoneHeight; y++) {
					primer.setBlockState(lx,y,lz, Blocks.stone.getDefaultState());
				}
//
//				if( biome == Biome.BEACH ||
//						biome == Biome.OCEAN ||
//						biome == Biome.FROZEN_OCEAN ||
//						biome == Biome.DESERT ||
//						biome == Biome.DESERT_HILLS )
//				{
//					// Ocean or beach
//					if( y < height-1 ) {
//						setBlock(result,x,y,z, Material.SANDSTONE);
//						y++;
//					}
//					for(;y< height;y++) {
//						setBlock(result,x,y,z, Material.SAND );
//					}
//				} else if( biome == Biome.ICE_MOUNTAINS ) {
//					// Mountain
//					for(;y< height;y++) {
//						setBlock(result,x,y,z, Material.STONE );
//					}
//
//				} else {
					// Land
					for(;y< height-1;y++) {
						primer.setBlockState(lx,y,lz, Blocks.dirt.getDefaultState() );
					}
					if(y<height) {
						primer.setBlockState(lx,y,lz, Blocks.grass.getDefaultState() );
						y++;
					}
//				}
				for(;y<seaLevel-1 && spawnOcean ;y++) {
					primer.setBlockState(lx,y,lz, Blocks.water.getDefaultState() );
				}
				if( y==seaLevel-1 && spawnOcean) {
//					if(biome == Biome.FROZEN_OCEAN || biome == Biome.FROZEN_RIVER) {
//						setBlock(result,x,y,z, Material.ICE );
//					} else {
						primer.setBlockState(lx,y,lz, Blocks.water.getDefaultState() );
//					}
					y++;
				}
			}
		}
	}

	protected Coordinate getLatLon( int worldx, int worldz) {
		Location root = new Location(null,worldx,0,worldz);
		// Translate x/z to lat/lon
		Coordinate coord = mapProjection.locationToCoordinate(root);
		return coord;
	}

	/**
	 * Calculate the elevation for a position
	 * @param world
	 * @param coord lat/lon coordinate. coord.z will be set to the fetched elevation
	 * @return block height (y); number of solid blocks to generate
	 */
	public int getBlockHeight( Coordinate coord) throws DataUnavailableException{

		// get elevation in m
		Double elev  = elevationProvider.fetchElevation(coord);

		// Side effect: Store elevation into coord
		if(!Double.isNaN(elev)) {
			coord.z = elev;
		}

		// translate elevation to blocks
		double y = elevationProjection.elevationToY(elev);

		if( y == Double.NaN ) {
			return defaultBlockHeight;
		} else {
			return (int) Math.floor(Math.min(y, WORLD_HEIGHT));
		}
	}


//	@Override
//	public Location getFixedSpawnLocation(World world, Random random) {
//		// Project latlon
//		Location spawnloc = mapProjection.coordinateToLocation(world, new Coordinate(spawn.x,spawn.y));
//
//		// Project elevation
//		Double elev;
//		try {
//			elev = elevationProvider.fetchElevation(spawn);
//		} catch (DataUnavailableException e) {
//			elev = Double.NaN;
//		}
//		double y = elevationProjection.elevationToY(elev);
//
//		spawnloc.setY(y);
//
//		return spawnloc;
//	}

	public MapProjection getMapProjection() {
		return mapProjection;
	}


	public ElevationProvider getElevationProvider() {
		return elevationProvider;
	}

	public ElevationProjection getElevationProjection() {
		return elevationProjection;
	}

	public CoordBiomeProvider getBiomeProvider() {
		return biomeProvider;
	}

	public Coordinate getSpawn() {
		return spawn;
	}


}