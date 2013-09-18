package us.bliven.bukkit.earthcraft.gis;

import org.bukkit.World;
import org.bukkit.block.Biome;

import us.bliven.bukkit.earthcraft.EarthGen;

import com.vividsolutions.jts.geom.Coordinate;

public interface BiomeProvider {
	/**
	 * Get the biome at a particular location
	 * @param gen The generator for this world
	 * @param world World to get biomes for
	 * @param coord Location to get biomes
	 * @return The Biome type
	 */
	public Biome getBiome(EarthGen gen, World world, Coordinate coord);
}
