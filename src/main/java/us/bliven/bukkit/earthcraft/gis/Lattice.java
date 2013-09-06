/**
 *
 */
package us.bliven.bukkit.earthcraft.gis;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Represents a way to transform continuous coordinates to points on a discrete lattice.
 *
 * Lattice points have integer coordinates starting with (0,0) at the origin.
 *
 * This class is mostly redundant to geotools' MapCoverage, and should possibly be replaced.
 */
class Lattice {
	private Coordinate origin; // coordinate of point (0,0)
	private Coordinate scale; //coordinate relative to the origin of point (1,1); the scale of the lattice

	public Lattice(Coordinate origin, Coordinate scale) {
		this.origin = origin;
		this.scale = scale;
	}

	/**
	 * Gets a list of points surrounding the query.
	 *
	 * If distance = 0, the closest lattice point to the query is returned.
	 * If distance = 1, the 4 lattice points surrounding the query are returned.
	 * If distance > 1, all points within (distance-1) manhattan units of
	 * the central 4 lattice points are returned (2*d*(d-1) points overall).
	 *
	 * Points which lie along the grid are treated as belonging to the
	 * next larger grid square.
	 *
	 * @param query A query point
	 * @param distance
	 * @return
	 * @throws
	 */
	public Set<Point> getNeighbors(Coordinate query, int distance) {
		if(distance < 0) {
			throw new IllegalArgumentException("Distance must be positive");
		}

		double scaledX = (query.x-origin.x)/scale.x;
		double scaledY = (query.y-origin.y)/scale.y;

		Set<Point> points = new LinkedHashSet<Point>();
		if(distance == 0) {
			// Find nearest neighbor
			int x = (int) Math.round(scaledX);
			int y = (int) Math.round(scaledY);
			points.add(new Point(x,y));
			return points;
		}

		//The points returned form a right triangle off each of the 4 surrounding points.
		//Enumerate them in order of increasing distance
		for(int d=1;d<=distance;d++) {
			for(int x=0;x<d;x++) {
				int y=d-1-x;
				points.add(new Point((int)scaledX+x+1,(int)scaledY+y+1));
				points.add(new Point((int)scaledX-x,(int)scaledY+y+1));
				points.add(new Point((int)scaledX-x,(int)scaledY-y));
				points.add(new Point((int)scaledX+x+1,(int)scaledY-y));
			}
		}

		return points;
	}

	/**
	 * Converts a point on the grid to a coordinate of the original system.
	 * @param gridPoint
	 * @return
	 */
	public Coordinate getCoordinate(Point gridPoint) {
		double x = gridPoint.getX()*scale.x+origin.x;
		double y = gridPoint.getY()*scale.y+origin.y;

		return new Coordinate(x,y);
	}

	/**
	 * Helper method to get coordinates for a whole list. Calls
	 * {@link #getCoordinate(Point)} for each list element.
	 * @param gridPoints
	 * @return
	 */
	public List<Coordinate> getCoordinates(List<Point> gridPoints) {
		ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>(gridPoints.size());
		for(Point pt : gridPoints) {
			coordinates.add(getCoordinate(pt));
		}
		return coordinates;
	}

	public Point getNearestNeighbor(Coordinate query) {
		double scaledX = (query.x-origin.x)/scale.x;
		double scaledY = (query.y-origin.y)/scale.y;
		// Find nearest neighbor
		int x = (int) Math.round(scaledX);
		int y = (int) Math.round(scaledY);
		return new Point(x,y);
	}
}