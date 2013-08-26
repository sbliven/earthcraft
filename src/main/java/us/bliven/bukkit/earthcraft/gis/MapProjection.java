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

	public Coordinate locationToCoordinate(Location loc);
	public Location coordinateToLocation(World world, Coordinate coord);
}
