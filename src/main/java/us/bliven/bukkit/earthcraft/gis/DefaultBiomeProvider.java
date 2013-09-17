package us.bliven.bukkit.earthcraft.gis;

import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;

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
	public void setBiome(EarthGen gen, BiomeGrid biomes, Coordinate coord, int chunkx, int chunkz) {
		// Do nothing; use the default biome
	}

}
