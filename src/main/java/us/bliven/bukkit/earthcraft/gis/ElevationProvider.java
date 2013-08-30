package us.bliven.bukkit.earthcraft.gis;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A class which can return elevation data
 * @author blivens
 *
 */
public interface ElevationProvider {

	/**
	 * A list of coordinates giving (lat,lon) pairs. Note that latitude 
	 * corresponds to the 'x' member of each coordinate, despite any unfortunate
	 * clash with the use of x for horizontal cartesian coordinates. 
	 * @param l
	 * @return
	 * @throws DataUnavailableException If a non-recoverable error stops the data
	 *  from being accessed. Less serious errors simply result in null elevations
	 *  being returned.
	 * @throws  
	 */
	public List<Double> fetchElevations(List<Coordinate> l) throws DataUnavailableException;
	
	public Double fetchElevation(Coordinate c) throws DataUnavailableException;
}