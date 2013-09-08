package us.bliven.bukkit.earthcraft.gis;

import org.bukkit.Location;
import org.bukkit.World;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Converts minecraft x/y/z coordinates into real-world lat/lon/elevation coordinates
 *
 * @author Spencer Bliven
 */
public interface MapProjection {

	/**
	 * Convert a minecraft location to a real-world coordinate
	 * @param loc Minecraft location
	 * @return The location in the real world, as degrees lat, degrees lon, meters elevation
	 */
	public Coordinate locationToCoordinate(Location loc);
	/**
	 * Convert a real-world coordinate to a minecraft location
	 * @param coord The location in the real world, as degrees lat, degrees lon, meters elevation
	 * @return Minecraft location
	 */
	public Location coordinateToLocation(World world, Coordinate coord);

	/**
	 * Get the local map scale at a point, in degrees per block.
	 *
	 * For instance, a Mercator would have constant longitudinal scale, but the
	 * latitudinal scale would increase as coord nears the poles.
	 *
	 * This method is optional, and implementations may throw an UnsupportedOperationException.
	 *
	 * @param coord position to take the scale near
	 * @return Local scale, in  degrees lat/block, degrees lon/block, meters elevation/block
	 * @throws UnsupportedOperationException If this method is not implemented
	 */
	public Coordinate getLocalScale(Coordinate coord);
}
