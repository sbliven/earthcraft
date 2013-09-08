package us.bliven.bukkit.earthcraft.gis;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Utility class for common projection tasks
 * @author Spencer Bliven
 */
public final class ProjectionTools {

	// Can't instantiate
	private ProjectionTools() {}

	/**
	 * Convert coordinates to valid lat=(-90,90], lon=[-180,180)
	 * @param pos
	 * @return
	 */
	public static Coordinate wrapCoordinate(Coordinate pos) {
		// Convert coordinates to valid lat=(-90,90], lon=[-180,180)
		double wrapx = pos.x % 180;
		double wrapy = pos.y % 360;
		if( wrapx > 90) {
			wrapx -= 180;
		} else if(wrapx <=-90) {
			wrapx += 180;
		}
		if( wrapy >= 180) {
			wrapy -= 360;
		} else if(wrapy < -180) {
			wrapy += 360;
		}
		return new Coordinate(wrapx,wrapy,pos.z);
	}

	/**
	 * For wrapped coordinates, get the coordinates for the globe in which
	 * they would appear.
	 *
	 * <p>The x and y components of the result are integers. The z component is unused.
	 *
	 * <h3>Examples:</h3>
	 * <li> All normal lat,lon coordinates are on globe (0,0)
	 * <li> (0,200) lies on the equator of globe (0,1)
	 * <li> (-90,180) lies in globe (-1,1), since globes are defined from the northwest
	 *
	 * @param pos
	 * @return
	 */
	public static Coordinate getGlobeCoordinate(Coordinate pos) {
		int globelat = (int)Math.ceil((pos.x-90.)/180.);
		int globelon = (int)Math.floor((pos.y+180)/360.);
		return new Coordinate(globelat,globelon);
	}
	public static String latlonelevString(Coordinate coord) {
		return latlonelevString(coord,0,0,0);
	}
	public static String latlonelevString(Coordinate coord,
			Coordinate localScale) {
		int latprec = (int)Math.ceil(Math.log10(2*localScale.x));
		int lonprec = (int)Math.ceil(Math.log10(2*localScale.y));
		int elevprec = (int)Math.ceil(Math.log10(2*localScale.z));
		return latlonelevString(coord, latprec,lonprec,elevprec);
	}
	public static String latlonelevString(Coordinate coord,
			int latprec, int lonprec, int elevprec) {
		String latlon = latlonString(coord,latprec,lonprec);
		if( !Double.isNaN(coord.z) ) {
			if( elevprec < 1) {
				return String.format("%s %fm",latlon,coord.z);
			} else {
				return String.format("%s %."+elevprec+"fm",latlon,coord.z);
			}
		} else {
			return latlon;
		}
	}

	/**
	 * Convert a coordinate into a latitude-longitude string.
	 *
	 * <h3>Examples:</h3>
	 * "32.71N 117.15E" - San Diego
	 * "40.79N*5 73.96E*-2" - New York, on the globe 5 up and two left of the origin
	 * @param coord
	 */
	public static String latlonString(Coordinate coord) {
		return latlonString(coord,0,0);
	}
	/**
	 *
	 * @param coord Coordinate to express
	 * @param latprec Number of digits to print for latitude
	 * @param lonprec Number of digits to print for longitude
	 * @param elevprec Number of digits to print for elevation
	 * @return
	 */
	public static String latlonString(Coordinate coord, int latprec, int lonprec) {
		Coordinate wrappedPos = wrapCoordinate(coord);
		Coordinate globePos = getGlobeCoordinate(coord);

		StringBuilder str = new StringBuilder();
		if( latprec < 1) {
			str.append(Math.abs(wrappedPos.x));
		} else {
			str.append(String.format("%."+latprec+"f", Math.abs(wrappedPos.x)));
		}
		if(wrappedPos.x >=0) {
			str.append('N');
		} else {
			str.append('S');
		}

		if(globePos.x != 0) {
			str.append('*');
			str.append((int)globePos.x);
		}

		str.append(' ');

		if( lonprec < 1) {
			str.append(Math.abs(wrappedPos.y));
		} else {
			str.append(String.format("%."+lonprec+"f", Math.abs(wrappedPos.y)));
		}

		if(wrappedPos.y >= 0) {
			str.append('E');
		} else {
			str.append('W');
		}

		if(globePos.y != 0) {
			str.append('*');
			str.append((int)globePos.y);
		}

		return str.toString();
	}



}
