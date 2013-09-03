package us.bliven.bukkit.earthcraft;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.EquirectangularProjection;
import us.bliven.bukkit.earthcraft.gis.FlatElevationProvider;
import us.bliven.bukkit.earthcraft.gis.InterpolatingElevationCache;
import us.bliven.bukkit.earthcraft.gis.MapProjection;
import us.bliven.bukkit.earthcraft.gis.OpenElevationConnector;
import us.bliven.bukkit.earthcraft.gis.SRTMPlusElevationProvider;
import us.bliven.bukkit.earthcraft.gis.TestElevationProvider;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Reads and writes Earthcraft config.yml
 * @author Spencer Bliven
 */
public class ConfigManager {

	EarthcraftPlugin plugin;

	public ConfigManager(EarthcraftPlugin plugin) {
		this.plugin=plugin;

		//TODO defaults
	}

	public ConfigurationSection getWorld(String worldname) {
		FileConfiguration config = plugin.getConfig();
		return config.getConfigurationSection("worlds."+worldname);
	}

	public MapProjection getProjection(String worldName) {
		ConfigurationSection world = getWorld(worldName);
		ConfigurationSection projection = world.getConfigurationSection("projection");
		String type = projection.getString("type");
		ConfigurationSection parameters = projection.getConfigurationSection("parameters");

		if( type.equalsIgnoreCase( "EquirectangularProjection" )) {
			return createEquirectangularProjection(parameters);
		} else {
			//TODO handle error
			System.out.println("Unrecognized projection type "+type);
			return createEquirectangularProjection(parameters);
		}
	}

	private EquirectangularProjection createEquirectangularProjection(
			ConfigurationSection parameters) {
		// TODO set defaults
		Coordinate origin = createCoordinate(parameters.getConfigurationSection("origin"));
		Coordinate scale = createCoordinate(parameters.getConfigurationSection("scale"));

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
		} else {
			// Default to flat world
			System.out.println("Error: unknown elevation type "+type);
			return createFlatElevationProvider(params);
		}
	}

	private ElevationProvider createSRTMPlusElevationProvider(
			ConfigurationSection params, boolean wrap) {
		String dir = params.getString("cache");
		return new SRTMPlusElevationProvider(dir,wrap);
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
		// TODO use this value for creating elevation providers
		ConfigurationSection world = getWorld(worldName);
		return world.getBoolean("wrap", true);
	}
}
