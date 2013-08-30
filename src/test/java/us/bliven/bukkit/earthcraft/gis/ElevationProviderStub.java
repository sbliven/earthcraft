package us.bliven.bukkit.earthcraft.gis;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Stub elevation provider
 * @author Spencer Bliven
 */
public class ElevationProviderStub extends AbstractElevationProvider {

	/**
	 * @return abs(coordinate.x) for each coordinate given
	 */
	@Override
	public Double fetchElevation(Coordinate c) throws DataUnavailableException {
		return Math.abs(c.x);
	}

}
