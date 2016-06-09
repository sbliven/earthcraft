/**
 *
 */
package us.bliven.bukkit.earthcraft.gis;

import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author Spencer Bliven
 */
public class FlatElevationProvider extends AbstractElevationProvider {// implements Configurable {
	private double elevation;

	private Logger log;
	public FlatElevationProvider() {
		this(64.);
	}
	public FlatElevationProvider(double elevation) {
		this.elevation = elevation;
		log = Logger.getLogger(getClass().getName());
	}
//	public FlatElevationProvider(ConfigManager config, ConfigurationSection params) {
//		this();
//		initFromConfig(config, params);
//	}
//	@Override
//	public void initFromConfig(ConfigManager config, ConfigurationSection params) {
//		for(String param : params.getKeys(false)) {
//			if( param.equalsIgnoreCase("elev") ) {
//				elevation = params.getDouble(param,elevation);
//			} else {
//				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
//			}
//		}
//	}

	/**
	 * Always return the same elevation
	 * @see us.bliven.bukkit.earthcraft.gis.ElevationProvider#fetchElevations(java.util.List)
	 */
	@Override
	public Double fetchElevation(Coordinate c)
			throws DataUnavailableException {
		return this.elevation;
	}

}
