package us.bliven.bukkit.earthcraft.gis;

import org.bukkit.Location;
import org.bukkit.World;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Implements an Equirectangular projection. This is a linear scaling of the
 * earth.
 *
 * <p>It preserves neither distance, area, nor angle, but is easy to compute.
 *
 * <p>Out-of-bounds elevations will be truncated to valid minecraft heights.
 *
 * <p>The poles cannot be
 *
 * @author Spencer Bliven
 */
public class EquirectangularProjection implements MapProjection {

	private Coordinate origin;
	private Coordinate scale;

	/**
	 * Origin and scale are relative to the real world.
	 *
	 * <p>For instance, an origin of Coordinate(32.759444,-117.253160,-40) would
	 * set (0,0) in minecraft to the entrance of Mission Beach in San Diego,
	 * and set the bedrock floor to 40 blocks below sea level.
	 *
	 * <p>A scale of Coordinate(1,1,1000) would scale the coordinates to one
	 * degree latitude/longitude per block and the elevation to one
	 * kilometer per block above sea level.
	 *
	 * <p>Note that the scale of the projection will be true at latitude
	 * <tt>acos(scale.x/scale.y)</tt>. For instance, a scale of 1,0.707,1 would
	 * be 1:1 at 45 degrees north or south (useful for Europeans and fans of
	 * Arno Peters).
	 *
	 *
	 * @param origin The origin of the map, as a (lat,lon, sea-level) pair
	 * @param scale The scale, in (degrees lat/block, deg lon/block,meters/block)
	 */
	public EquirectangularProjection(Coordinate origin, Coordinate scale) {
		if( origin == null) {
			throw new IllegalArgumentException("Origin must not be null.");
		}
		if( scale == null) {
			throw new IllegalArgumentException("Scale must not be null.");
		}
		this.origin = origin;
		this.scale = scale;
	}

	@Override
	public Location coordinateToLocation(World world, Coordinate coord) {
		// In MC, x points "East" (relative to a map), z points "South", and y give altitude.
		// In GeoTools, x denotes latitude northward, y denotes longitude eastward, and z gives altitude.
		double locX = (coord.y-origin.y)/scale.y; // East-ness
		double locZ = -(coord.x-origin.x)/scale.x; // South-ness

		// Truncate to bounds
		//if(y<0) y=0;
		//if(y>world.getMaxHeight()) y = world.getMaxHeight();

		Location loc = new Location(world, locX, Double.NaN, locZ);
		return loc;
	}

	@Override
	public Coordinate locationToCoordinate(Location loc) {
		// In MC, x points "East" (relative to a map), z points "South", and y give altitude.
		// In GeoTools, x denotes latitude northward, y denotes longitude eastward, and z gives altitude.
		//TODO Locations are generally relative to chunks. Decide how to handle that
		double locX = loc.getX(); // Eastness
		double locZ = loc.getZ(); // Southness

		double coordX = -scale.x*locZ+origin.x; //lat
		double coordY = scale.y*locX+origin.y; //lon

		Coordinate coord = new Coordinate(coordX,coordY);
		return coord;
	}

	public Coordinate getOrigin() {
		return origin;
	}

	public void setOrigin(Coordinate origin) {
		this.origin = origin;
	}

	/**
	 * Get the scale, in degrees per block
	 * @return
	 */
	public Coordinate getScale() {
		return scale;
	}

	public void setScale(Coordinate scale) {
		this.scale = scale;
	}

	/**
	 * Get the local map scale at a point, in degrees per block.
	 *
	 * This method approximates the globe by a sphere. But if you're using
	 * this projection then you probably don't care about accuracy anyways.
	 *
	 * @param coord position to take the scale near
	 * @return Local scale, in  degrees lat/block, degrees lon/block, meters elevation/block
	 * @throws UnsupportedOperationException If this method is not implemented
	 */
	@Override
	public Coordinate getLocalScale(Coordinate coord) {
		Coordinate wrapped = ProjectionTools.wrapCoordinate(coord);
		double lonScale;
		if( Math.abs(wrapped.y) == 90. ) {
			// Poorly defined at the poles!
			lonScale = Double.POSITIVE_INFINITY;
		} else {
			lonScale = 1.0/Math.cos(wrapped.y*Math.PI/180);
		}

		return new Coordinate(scale.x, scale.y*lonScale, scale.z);
	}
}
