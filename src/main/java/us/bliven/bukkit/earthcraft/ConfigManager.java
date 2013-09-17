package us.bliven.bukkit.earthcraft;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import us.bliven.bukkit.earthcraft.gis.ElevationProjection;
import us.bliven.bukkit.earthcraft.gis.MapProjection;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Reads and writes Earthcraft config.yml
 * @author Spencer Bliven
 */
public class ConfigManager {

	private EarthcraftPlugin plugin;

	private Logger log;

	public ConfigManager(EarthcraftPlugin plugin) {
		this.plugin=plugin;

		log = Logger.getLogger(getClass().getName());

		//TODO defaults
	}

	public Plugin getPlugin() {
		return plugin;
	}

	protected ConfigurationSection getWorld(String worldname) {
		FileConfiguration config = plugin.getConfig();
		ConfigurationSection params = config.getConfigurationSection("worlds."+worldname);
		if( params == null ) {
			params = new MemoryConfiguration();
			plugin.getConfig().set("worlds."+worldname, params);
		}
		return params;
	}

	/*
	public MapProjection getMapProjection(String worldName) {
		ConfigurationSection world = getWorld(worldName);
		ConfigurationSection projection = world.getConfigurationSection("projection");
		String type = projection.getString("type");
		ConfigurationSection parameters = projection.getConfigurationSection("parameters");

		if( type.equalsIgnoreCase( "EquirectangularProjection" )) {
			return createEquirectangularProjection(parameters);
		} else {
			//Bad configuation file
			log.severe("Unrecognized projection type "+type);
			return createEquirectangularProjection(parameters);
		}
	}

	public ElevationProjection getElevationProjection(String worldName) {
		//TODO stub
		return new LinearElevationProjection(-640., 10.);
	}

	private EquirectangularProjection createEquirectangularProjection(
			ConfigurationSection parameters) {
		// TODO set defaults
		Coordinate origin = createCoordinate(parameters.getConfigurationSection("origin"));
		Coordinate scale = createCoordinate(parameters.getConfigurationSection("scale"));
		if( Double.isNaN(scale.z)) {
			log.warning("No elevation scale set, using 1 m/block.");
			scale.z = 1.0;
		}
		if( Double.isNaN(origin.z) ) {
			// If no elevation, calculate from sea level
			int sea = parameters.getInt("origin.seaLevel",64);
			origin.z = -sea*scale.z;
		}

		return new EquirectangularProjection(origin,scale);
	}

	private ConfigurationSection getSources(String worldName) {
		ConfigurationSection world = getWorld(worldName);
		return world.getConfigurationSection("sources");
	}

	public ElevationProvider getProvider(String worldName) {
		ConfigurationSection sources = getSources(worldName);
		ConfigurationSection elevation = sources.getConfigurationSection("elevation");

		boolean wrap = getWrap(worldName);

		return createElevationProvider(elevation,wrap);
	}


	private ElevationProvider createElevationProvider(
			ConfigurationSection elevation, boolean wrap) {
		String type = elevation.getString("type");
		ConfigurationSection params = elevation.getConfigurationSection("parameters");

		if( type.equalsIgnoreCase("OpenElevationConnector")) {
			return createOpenElevationConnector();
		} else if( type.equalsIgnoreCase("InterpolatingElevationCache")) {
			return createInterpolatingElevationCache(params,wrap);
		} else if( type.equalsIgnoreCase("TestElevationProvider")) {
			return createTestElevationProvider(params);
		} else if( type.equalsIgnoreCase("FlatElevationProvider")) {
			return createFlatElevationProvider(params);
		} else if( type.equalsIgnoreCase("SRTMPlusElevationProvider")) {
			return createSRTMPlusElevationProvider(params,wrap);
		} else if( type.equalsIgnoreCase("InterpolatedCoverageElevationProvider")) {
			return createInterpolatedCoverageElevationProvider(params,wrap);
		} else {
			// Default to flat world
			log.severe("Error: unknown elevation type "+type);
			return createFlatElevationProvider(params);
		}
	}

	private SRTMPlusElevationProvider createSRTMPlusElevationProvider(
			ConfigurationSection params, boolean wrap) {
		String dir = params.getString("cache");
		return new SRTMPlusElevationProvider(dir,wrap);
	}
	private InterpolatedCoverageElevationProvider createInterpolatedCoverageElevationProvider(
			ConfigurationSection params, boolean wrap) {
		GridCoverageElevationProvider provider = createGridCoverageElevationProvider(
				params.getConfigurationSection("provider"), wrap );
		return new InterpolatedCoverageElevationProvider(provider);
	}

	private GridCoverageElevationProvider createGridCoverageElevationProvider(
			ConfigurationSection elevation, boolean wrap) {
		String type = elevation.getString("type");
		ConfigurationSection params = elevation.getConfigurationSection("parameters");

		if( type.equalsIgnoreCase("SRTMPlusElevationProvider")) {
			return createSRTMPlusElevationProvider(params,wrap);
		} else if( type.equalsIgnoreCase("InterpolatedCoverageElevationProvider")) {
			return createInterpolatedCoverageElevationProvider(params,wrap);
		} else {
			// Default to flat world
			log.severe("Error: unknown elevation type "+type);
			return null;
		}
	}

	private ElevationProvider createFlatElevationProvider(
			ConfigurationSection params) {
		if( params.contains("elev") ) {
			return new FlatElevationProvider(params.getDouble("elev"));
		} else {
			return new FlatElevationProvider();
		}
	}

	private InterpolatingElevationCache createInterpolatingElevationCache(
			ConfigurationSection params, boolean wrap) {
		ElevationProvider provider = createElevationProvider(params.getConfigurationSection("provider"),wrap);
		Coordinate scale = createCoordinate(params.getConfigurationSection("scale"));
		if( params.contains("origin")) {
			Coordinate origin = createCoordinate(params.getConfigurationSection("origin"));
			return new InterpolatingElevationCache(provider, origin, scale);
		} else {
			return new InterpolatingElevationCache(provider, scale);
		}
	}

	private TestElevationProvider createTestElevationProvider(
			ConfigurationSection params) {
		double south = params.getDouble("south");
		double north = params.getDouble("north");
		double west = params.getDouble("west");
		double east = params.getDouble("east");
		double min = params.getDouble("min-elev");
		double max = params.getDouble("max-elev");

		return new TestElevationProvider(south, north, west, east, min, max);
	}

	private OpenElevationConnector createOpenElevationConnector() {
		OpenElevationConnector oe = new OpenElevationConnector();
		oe.monitor();
		return oe;
	}

	private Coordinate createCoordinate(
			ConfigurationSection configurationSection) {
		double lat = configurationSection.getDouble("lat");
		double lon = configurationSection.getDouble("lon");
		double elev = configurationSection.getDouble("elev", Double.NaN);

		return new Coordinate(lat, lon, elev);
	}

	public Coordinate getSpawn(String worldName) {
		ConfigurationSection world = getWorld(worldName);
		return createCoordinate(world.getConfigurationSection("spawn"));
	}

	public boolean getWrap(String worldName) {
		ConfigurationSection world = getWorld(worldName);
		return world.getBoolean("wrap", true);
	}

	public boolean getSpawnOcean(String worldName) {
		ConfigurationSection world = getWorld(worldName);
		return world.getBoolean("spawnOcean", true);
	}
	*/

	/**
	 * Create a new EarthGen instance for a particular world from the config
	 * @param world world name
	 * @return
	 */
	public EarthGen createEarthGen(String world) {
		ConfigurationSection params = getWorld(world);
		EarthGen gen = new EarthGen(plugin);
		gen.initFromConfig(this, params);
		return gen;
	}

	/**
	 * Create a coordinate from a specific configuration section
	 * @param params The ConfigurationSection
	 * @param paramName A key containing a list of doubles
	 * @param defaults A default Coordinate to return on parsing failures
	 * @return
	 */
	public Coordinate getCoordinate(ConfigurationSection params, String paramName,
			Coordinate defaults) {
		return getCoordinate(params.getDoubleList(paramName),defaults);
	}
	/**
	 * Create a coordinate from a list of doubles
	 * @param coords lat, lon, [elev]
	 * @param defaults Returned if coords contains other than 2-3 elements
	 * @return
	 */
	public Coordinate getCoordinate(List<Double> coords,Coordinate defaults) {
		if( coords.size() == 2) {
			return new Coordinate(coords.get(0),coords.get(1));
		} else if( coords.size() == 3) {
			return new Coordinate(coords.get(0),coords.get(1),coords.get(2));
		} else {
			return defaults;
		}
	}


	/**
	 * Instantiate a Configurable object from a section of a config file
	 *
	 * @param className The name of a class implementing Configurable and
	 * 	extending <code>supertype</code>
	 * @param supertype The type of the object to be returned
	 * @param params Initialization parameters, passed to the new object's
	 *  {@link Configurable#initFromConfig(ConfigManager, ConfigurationSection)
	 *  initFromConfig()} method
	 * @param def Default return value. Returned if any errors occur.
	 * @return A new object of type <code>className</code> and initialized
	 *  with the parameters in <code>params</code>, or <code>def</code> if
	 *  an error has occurred.
	 */
	@SuppressWarnings("unchecked")
	public <T> T createConfigurable(String className, Class<T> supertype,
			ConfigurationSection params, T def ) {

		// Load the class
		ClassLoader cl = plugin.getClass().getClassLoader();

		String[] packages = new String[] {
				"",
				ElevationProjection.class.getPackage().getName()+".",
				getClass().getPackage().getName()+".",
		};

		Class<?> klass = null;
		for(String pkg : packages) {
			try {
				klass = cl.loadClass(pkg+className);
				//log.info("Found "+pkg+className);
				break;
			} catch (ClassNotFoundException e) {
				//not in this package
				//log.info("Unable to find "+pkg+className);
			}
		}
		if( klass == null) {
			log.severe("Unable to find "+className);
			return def;
		}

		if( !Configurable.class.isAssignableFrom(klass)) {
			log.severe(className+" is not Configurable.");
			return def;
		}

		// instantiate
		T t;
		try {
			t = (T)klass.newInstance();
		} catch (ClassCastException e) {
			// Must be a Configurable T
			log.severe(className+" is not a "+supertype.getName()+".");
			return def;
		} catch (InstantiationException e) {
			log.log(Level.SEVERE,"Error instantiating "+className,e);
			return def;
		} catch (IllegalAccessException e) {
			log.log(Level.SEVERE,"Error instantiating "+className,e);
			return def;
		}

		// initialize
		((Configurable) t).initFromConfig(this, params);

		return t;
	}

	/**
	 * Helper function for creating Configurables for sections which should
	 * contain only a single key of the form:
	 *
	 * <pre>classname: {parameters...}</pre>
	 *
	 * If more than one key is present, all but one will be ignored. Generally
	 * the last key is used, although the order is not guaranteed by the API.
	 * @param supertype The type of the object to be returned
	 * @param section The section containing the single key
	 * @param def
	 * @return
	 */
	public <T> T createSingleConfigurable(Class<T> supertype,
			ConfigurationSection section, T def) {
		// Should contain only a single element
		// If more are present, use the last one
		Set<String> keys = section.getKeys(false);

		if( keys.size() < 1) {
			return def;
		}

		Iterator<String> keysIt = keys.iterator();
		String className = keysIt.next();
		while(keysIt.hasNext()) {
			log.warning(String.format("Ignoring section %s.%s",
					section.getCurrentPath(), className) );
			className = keysIt.next();
		}

		ConfigurationSection params = section.getConfigurationSection(className);

		return createConfigurable(className, supertype, params, def);
	}
}
