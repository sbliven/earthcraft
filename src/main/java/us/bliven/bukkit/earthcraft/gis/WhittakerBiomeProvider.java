package us.bliven.bukkit.earthcraft.gis;

import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.noise.PerlinOctaveGenerator;

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

	// Temperature parameters
	private double equatorTemp;
	private double lapseRate;
	private double latitudeGradient;
	private PerlinOctaveGenerator tempNoise;
	private double tempNoiseScale;
	// Precipitation parameters
	private PerlinOctaveGenerator precipNoise;
	private double precipNoiseScale;

	private Logger log;

	public WhittakerBiomeProvider() {
		// Default lapse rate from International Civil Aviation Organization
		// https://en.wikipedia.org/wiki/Lapse_rate#Environmental_lapse_rate
		lapseRate = 6.49/1000.; //in degC/m elevation
		// Weighted average of gradients in figure 3 of
		//   Baumann, H., & Doherty, O. (2013). Decadal Changes in the World's
		//   Coastal Latitudinal Temperature Gradients. PloS one, 8(6), e67596.
		// weighted by N
		latitudeGradient = 0.462439024; //in degC/deg lat toward equator
		// Typical max temp from Baumann
		equatorTemp = 30.; // in degC
		tempNoise = null; //initialize later
		tempNoiseScale = 1/64.;
		precipNoise = null;
		precipNoiseScale = 1/64.;


		log = Logger.getLogger(getClass().getName());
	}

	@Override
	public void initFromConfig(ConfigManager config, ConfigurationSection params) {
		for(String param : params.getKeys(false)) {
			if( param.equalsIgnoreCase("lapseRate") ) {
				lapseRate = params.getDouble(param,lapseRate);
			} else if( param.equalsIgnoreCase("equatorTemp") ) {
				equatorTemp = params.getDouble(param,equatorTemp);
			} else if( param.equalsIgnoreCase("equatorTemp") ) {
				equatorTemp = params.getDouble(param,equatorTemp);
			} else {
				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
			}
		}
	}

	public double getTemperature(Coordinate coord) {
		double temp = equatorTemp;
		temp -= latitudeGradient*Math.abs(coord.x);
		temp -= lapseRate*Math.abs(coord.z);

		return temp;
	}

	public double getPrecipitation(Coordinate coord) {
		double precip = 20;

		return precip;
	}

	@Override
	public Biome getBiome(EarthGen gen, World world, Coordinate coord) {
		ElevationProvider provider = gen.getElevationProvider();

		double elev = coord.z;
		if( Double.isNaN(elev)) {
			try {
				elev = provider.fetchElevation(coord);
			} catch (DataUnavailableException e) {
				// TODO handle errors consistently
				elev = Double.NEGATIVE_INFINITY; // Use ocean
			}
		}

		double seaLevel = 0;
		double sandLevel = 2;
		double mountainLevel = 2000;

		if(elev < seaLevel) {
			return Biome.OCEAN;
		} else if( elev <= sandLevel) {
			return Biome.BEACH;
		} else if( elev <= mountainLevel) {
			return Biome.FOREST_HILLS;
		} else {
			return Biome.ICE_MOUNTAINS;
		}

	}
}
