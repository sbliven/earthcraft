/**
 * 
 */
package us.bliven.bukkit.earthcraft.gis;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author Spencer Bliven
 */
public abstract class AbstractElevationProvider implements ElevationProvider {	
	/**
	 * Always return the same elevation
	 * @see us.bliven.bukkit.earthcraft.gis.ElevationProvider#fetchElevations(java.util.List)
	 */
	@Override
	public List<Double> fetchElevations(List<Coordinate> l)
			throws DataUnavailableException {
		List<Double> elevations = new ArrayList<Double>(l.size());
		for(int i=0;i<l.size();i++) {
			elevations.add(fetchElevation(l.get(i)));
		}
		return elevations;
	}

}
