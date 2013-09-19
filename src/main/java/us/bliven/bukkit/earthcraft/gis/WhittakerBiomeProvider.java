package us.bliven.bukkit.earthcraft.gis;

import static org.bukkit.block.Biome.BEACH;
import static org.bukkit.block.Biome.DESERT;
import static org.bukkit.block.Biome.DESERT_HILLS;
import static org.bukkit.block.Biome.EXTREME_HILLS;
import static org.bukkit.block.Biome.FOREST;
import static org.bukkit.block.Biome.FOREST_HILLS;
import static org.bukkit.block.Biome.FROZEN_OCEAN;
import static org.bukkit.block.Biome.ICE_MOUNTAINS;
import static org.bukkit.block.Biome.ICE_PLAINS;
import static org.bukkit.block.Biome.JUNGLE;
import static org.bukkit.block.Biome.JUNGLE_HILLS;
import static org.bukkit.block.Biome.OCEAN;
import static org.bukkit.block.Biome.PLAINS;
import static org.bukkit.block.Biome.TAIGA;
import static org.bukkit.block.Biome.TAIGA_HILLS;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.noise.OctaveGenerator;
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
	private OctaveGenerator tempNoise;
	// Precipitation parameters
	private OctaveGenerator precipNoise;
	private double biomeScale;

	private Logger log;

	public WhittakerBiomeProvider() {
		// Default lapse rate from International Civil Aviation Organization
		// https://en.wikipedia.org/wiki/Lapse_rate#Environmental_lapse_rate
		lapseRate = 6.49/1000.; //in degC/m elevation
		// Weighted average of gradients in figure 3 of
		//   Baumann, H., & Doherty, O. (2013). Decadal Changes in the World's
		//   Coastal Latitudinal Temperature Gradients. PloS one, 8(6), e67596.
		// weighted by N
		// This gives a polar temperature of -11.6
		latitudeGradient = 0.462439024; //in degC/deg lat toward equator
		// Typical max temp from Baumann
		equatorTemp = 30.; // in degC
		tempNoise = null; //initialize later
		precipNoise = null;

		// Number of blocks between biomes given constant elevation and latitude
		biomeScale = 128.; // in blocks


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

	protected double getTemperature(Coordinate coord) {
		double temp = equatorTemp;
		temp -= latitudeGradient*Math.abs(coord.x);
		temp -= lapseRate*Math.abs(coord.z);

		double r = tempNoise.noise(coord.x, coord.y, .5, 1/Math.sqrt(2)); //roughtly -1 to 1
		temp += r*2.; // +/- ~2 deg

		return temp;
	}

	protected double getPrecipitation(Coordinate coord, double temp) {
		double r = precipNoise.noise(coord.x,coord.y, .5, 1/Math.sqrt(2));//roughly -1 to 1
		double maxPrecip = 10.*temp+100;
		double precip = (r+1)/2.*maxPrecip; // roughly 0 to maxPrecip
		return Math.max(precip, 0);
	}

	/**
	 *
	 * @param coord
	 * @param scale The scale to evaluate the slope. Ideally, should be about 1 block
	 * @param provider
	 * @return The magnitude of the gradient, in m/deg
	 */
	protected double getSlope(Coordinate coord,Coordinate scale,ElevationProvider provider) {
		// Make sure we have a starting elevation
		if( Double.isNaN(coord.z)) {
			try {
				coord.z = provider.fetchElevation(coord);
			} catch (DataUnavailableException e) {
				//
				return 0.;
			}
		}

		// Define grid around coord=e:
		// a b c
		// d e f
		// g h i

		// use kernel dz/dx = (f-e)/scale.x
		//			  dz/dy = (b-e)/scale.y

		// Alternately, could use central finite differences
		// See http://www.ce.utexas.edu/prof/maidment/giswr2011/docs/Slope.pdf
		// 	dz/dx = ( (a+2d+g)-(c+2f+i) )/(8*scale.x)
		// 	dz/dy = ( (a+2b+c)-(g+2h+i) )/(8*scale.y)

		try {
			double b = provider.fetchElevation(new Coordinate(coord.x,coord.y+scale.y));
			double e = coord.z;
			double f = provider.fetchElevation(new Coordinate(coord.x+scale.x,coord.y));

			double dx = (f-e)/scale.x;
			double dy = (b-e)/scale.y;

			return Math.sqrt(dx*dx+dy*dy); // in m/deg
		} catch (DataUnavailableException e) {
			// default to infinite slope
			return Double.POSITIVE_INFINITY;
		}
	}

	@Override
	public Biome getBiome(EarthGen gen, World world, Coordinate coord) {
		ElevationProvider provider = gen.getElevationProvider();

		if(tempNoise == null || precipNoise == null) {
			// Create noise generators

			Coordinate scale = gen.getMapProjection().getLocalScale(new Coordinate(0,0)); // deg/block

			double xscale = 1/biomeScale/scale.x; // in 1/deg lat
			double yscale = 1/biomeScale/scale.y; // in 1/deg lon

			// Don't use octaves with frequencies smaller than 1 block
			int octaves = (int)Math.min( Math.ceil(Math.log(biomeScale)/Math.log(2)), 8 );

			tempNoise = new PerlinOctaveGenerator(world.getSeed(), octaves);
			tempNoise.setXScale(xscale);
			tempNoise.setYScale(yscale);

			precipNoise = new PerlinOctaveGenerator(world.getSeed(), octaves);
			precipNoise.setXScale(xscale);
			precipNoise.setYScale(yscale);
		}

		double temp = getTemperature(coord);
		double precip = getPrecipitation(coord, temp);

		Coordinate scale = gen.getMapProjection().getLocalScale(coord); // deg (for 1 block changes)
		double slope = getSlope(coord,scale,provider); // m/deg


		double elev = coord.z;
		if( Double.isNaN(elev) )
			elev = Double.NEGATIVE_INFINITY; // Use ocean

		double seaLevel = 0;
		double sandLevel = 2;
		//double mountainLevel = 2000;

		double hillSlope = 2.0; //In blocks/block
		double elevScale = gen.getElevationProjection().getLocalScale(elev); //m/block
		boolean hilly = slope > hillSlope*elevScale*scale.x;

		// Special cases based on elevation
		if(elev < seaLevel) {
			//TODO swamps
			if(temp <= 0)
				return FROZEN_OCEAN;
			else
				return OCEAN;
		} else if( elev <= sandLevel) {
			return BEACH;
		}

		// Whittaker diagram
		if(temp < -5 ) {
			// Cold Tundra
			if(hilly) {
				return ICE_MOUNTAINS;
			} else {
				return ICE_PLAINS;
			}
		} else if( temp < 8 ) {
			// chilly Taiga
			if(hilly) {
				return TAIGA_HILLS;
			} else {
				return TAIGA;
			}
		} else if( precip < 40 || 10*temp-210 >= precip ) {
			// Dry or hot Desert
			if(hilly) {
				return DESERT_HILLS;
			} else {
				return DESERT;
			}
		} else if( precip < 100 ) {
			// dry plains
			if(hilly) {
				return EXTREME_HILLS;
			} else {
				return PLAINS;
			}
		} else if( precip < 220 ) {
			// wet forest
			if(hilly) {
				return FOREST_HILLS;
			} else {
				return FOREST;
			}
		} else {
			// hot wet jungle
			if(hilly) {
				return JUNGLE_HILLS;
			} else {
				return JUNGLE;
			}
		}

	}

	public Map<String,String> getClimateInfo(EarthGen gen, World world, Coordinate coord) {
		Map<String,String> info = new HashMap<String, String>();

		ElevationProvider provider = gen.getElevationProvider();

		double temp = getTemperature(coord);
		double precip = getPrecipitation(coord, temp);

		Coordinate scale = gen.getMapProjection().getLocalScale(coord); // deg (for 1 block changes)
		double slope = getSlope(coord,scale,provider); // m/deg


		double elev = coord.z;
		if( Double.isNaN(elev) )
			elev = Double.NEGATIVE_INFINITY; // Use ocean

		double hillSlope = 2.0; //In blocks/block
		double elevScale = gen.getElevationProjection().getLocalScale(elev); //m/block
		double minHillSlope = hillSlope*elevScale*scale.x; // m/deg

		info.put("Temperature", Double.toString(temp) );
		info.put("Precipitation", Double.toString(precip) );
		info.put("Slope", Double.toString(slope) );
		info.put("Hill Slope", Double.toString(minHillSlope) );

		return info;

	}
}
