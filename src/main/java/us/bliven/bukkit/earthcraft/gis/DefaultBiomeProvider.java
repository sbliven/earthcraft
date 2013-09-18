package us.bliven.bukkit.earthcraft.gis;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;

import us.bliven.bukkit.earthcraft.ConfigManager;
import us.bliven.bukkit.earthcraft.Configurable;
import us.bliven.bukkit.earthcraft.EarthGen;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A basic Biome provider that just returns the default
 * minecraft Biome
 * @author Spencer Bliven
 */
public class DefaultBiomeProvider implements BiomeProvider,Configurable {

	private Logger log;
	public DefaultBiomeProvider() {
		log = Logger.getLogger(getClass().getName());
	}

	@Override
	public void initFromConfig(ConfigManager config, ConfigurationSection params) {
		for(String param : params.getKeys(false)) {
			log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
		}
	}

	@Override
	public Biome getBiome(EarthGen gen, World world, Coordinate coord) {
		// Do nothing; use the default biome
		Location loc = gen.getMapProjection().coordinateToLocation(world, coord);
		return world.getBiome(loc.getBlockX(),loc.getBlockZ());
	}

}
