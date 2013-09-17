package us.bliven.bukkit.earthcraft.gis;

import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;

import us.bliven.bukkit.earthcraft.ConfigManager;
import us.bliven.bukkit.earthcraft.Configurable;

public class LinearElevationProjection implements ElevationProjection, Configurable {

	private double origin;
	private double scale;

	private Logger log;

	public LinearElevationProjection() {
		this(-64.,1.);
	}

	public LinearElevationProjection(double origin, double scale) {
		log = Logger.getLogger(getClass().getName());
		this.origin = origin;
		this.scale = scale;
	}

	public LinearElevationProjection(ConfigManager config, ConfigurationSection params) {
		this();
		initFromConfig(config, params);
	}

	@Override
	public void initFromConfig(ConfigManager config, ConfigurationSection params) {

		// Either origin or sea is non-NaN at a time
		origin = Double.NaN;
		double sea = 64.;
		for(String param : params.getKeys(false)) {
			if( param.equalsIgnoreCase("scale") ) {
				scale = params.getDouble(param,scale);
			} else if( param.equalsIgnoreCase("elev") ) {
				origin = params.getDouble(param,origin);
				sea = Double.NaN;
			} else if( param.equalsIgnoreCase("seaLevel") ) {
				origin = Double.NaN;
				sea = params.getDouble(param,sea);
			} else {
				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
			}
		}

		// Either sea or origin must be defined
		if( Double.isNaN(origin) ) {
			assert( !Double.isNaN(sea) );

			// Take origin from sea level
			origin = -sea*scale;
		}
		assert( !Double.isNaN(origin));
	}

	@Override
	public double yToElevation(double y) {
		double elev = scale*y+origin; //elev

		return elev;
	}

	@Override
	public double elevationToY(double elev) {
		double locY = (elev-origin)/scale; // Up-ness

		return locY;
	}

}
