package us.bliven.bukkit.earthcraft.gis;

import org.bukkit.generator.ChunkGenerator.BiomeGrid;

import us.bliven.bukkit.earthcraft.EarthGen;

import com.vividsolutions.jts.geom.Coordinate;

public interface BiomeProvider {
	/**
	 * Get the biome at a particular location
	 * @param gen TODO
	 * @param gen
	 * @param coord
	 * @param chunkx TODO
	 * @param chunkz TODO
	 * @return
	 */
	public void setBiome(EarthGen gen, BiomeGrid biomes, Coordinate coord, int chunkx, int chunkz);
}
