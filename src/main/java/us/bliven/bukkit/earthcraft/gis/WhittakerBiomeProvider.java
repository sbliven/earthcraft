package us.bliven.bukkit.earthcraft.gis;

import java.util.logging.Logger;

import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.plugin.Plugin;

import us.bliven.bukkit.earthcraft.ConfigManager;
import us.bliven.bukkit.earthcraft.Configurable;
import us.bliven.bukkit.earthcraft.EarthGen;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A basic Biome provider that just returns the default
 * minecraft Biome
 * @author Spencer Bliven
 */
public class WhittakerBiomeProvider implements BiomeProvider, Configurable {

	private Logger log;
	public WhittakerBiomeProvider() {
		log = Logger.getLogger(getClass().getName());
	}

	@Override
	public void initFromConfig(ConfigManager config, ConfigurationSection params) {
		for(String param : params.getKeys(false)) {
//			if( param.equalsIgnoreCase("elev") ) {
//				elevation = params.getDouble(param,elevation);
//			} else {
				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
//			}
		}
	}

	@Override
	public void setBiome(EarthGen gen, BiomeGrid biomes, Coordinate coord, int x, int z) {
		if( x < 0 || 16 <= x || z < 0 || 16 <= z) {
			throw new IllegalArgumentException("Coords must be in chunk, but are "+x+","+z);
		}
		ElevationProvider provider = gen.getElevationProvider();

		double elev;
		try {
			elev = provider.fetchElevation(coord);
		} catch (DataUnavailableException e) {
			// TODO handle errors consistently
			elev = Double.NEGATIVE_INFINITY;
		}

		double seaLevel = 0;
		double sandLevel = 2;
		double mountainLevel = 2000;

		if(elev < seaLevel) {
			biomes.setBiome(x, z, Biome.OCEAN);
		} else if( elev <= sandLevel) {
			biomes.setBiome(x, z, Biome.BEACH);
		} else if( elev <= mountainLevel) {
			biomes.setBiome(x, z, Biome.FOREST_HILLS);
		} else {
			biomes.setBiome(x, z, Biome.ICE_MOUNTAINS);
		}

	}
}
