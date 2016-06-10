package us.bliven.bukkit.earthcraft.biome;

import java.util.logging.Logger;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeProvider;
import us.bliven.bukkit.earthcraft.gis.Location;
import us.bliven.bukkit.earthcraft.worldgen.EarthChunkProvider;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A basic Biome provider that just returns the default
 * minecraft Biome
 * @author Spencer Bliven
 */
public class DefaultBiomeProvider implements CoordBiomeProvider {//,Configurable {

	private static final Logger log = Logger.getLogger(DefaultBiomeProvider.class.getName());;
	private BiomeProvider provider;
	public DefaultBiomeProvider() {
		provider = null;
	}

	@Override
	public BiomeGenBase getBiome(EarthChunkProvider gen, World world, Coordinate coord) {
		if(provider == null) {
			provider = new BiomeProvider(world.getWorldInfo());
		}
		// Do nothing; use the default biome
		Location loc = gen.getMapProjection().coordinateToLocation(world, coord);
		return provider.getBiomeGenerator(loc);
	}

}
