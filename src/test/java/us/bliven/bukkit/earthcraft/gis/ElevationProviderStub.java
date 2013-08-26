package us.bliven.bukkit.earthcraft.gis;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;

/**
 * Stub elevation provider
 * @author Spencer Bliven
 */
public class ElevationProviderStub implements ElevationProvider {

	/**
	 * @return abs(coordinate.x) for each coordinate given
	 */
	@Override
	public List<Double> fetchElevations(List<Coordinate> l) throws DataUnavailableException {
		System.out.println("Cache miss! Requesting "+l);
		ArrayList<Double> elevations = new ArrayList<Double>(l.size());
		for(Coordinate c : l) {
			elevations.add(Math.abs(c.x));
		}
		return elevations;
	}

}
