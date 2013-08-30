/**
 * 
 */
package us.bliven.bukkit.earthcraft.gis;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author Spencer Bliven
 */
public class FlatElevationProvider extends AbstractElevationProvider {
	private double elevation;
	
	public FlatElevationProvider() {
		this(64.);
	}
	public FlatElevationProvider(double elevation) {
		this.elevation = elevation;
	}
	
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
