package us.bliven.bukkit.earthcraft.gis;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import us.bliven.bukkit.earthcraft.EarthGen;

import com.vividsolutions.jts.geom.Coordinate;

public interface CoordBiomeProvider {
	/**
	 * Get the biome at a particular location
	 * @param gen The generator for this world
	 * @param world World to get biomes for
	 * @param coord Location to get biomes
	 * @return The Biome type
	 */
	public BiomeGenBase getBiome(EarthGen gen, World world, Coordinate coord);
}
