package us.bliven.bukkit.earthcraft.gis;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;

/**
 * A test-pattern elevation provider
 *
 * Creates elevations within a rectangle increasing to the northeast.
 * The south of the box has uniform low elevation, and areas outside the box
 * have random elevation.
 *
 * @author Spencer Bliven
 */
public class TestElevationProvider implements ElevationProvider {

	private double south;
	private double north;
	private double west;
	private double east;
	private double minElev;
	private double maxElev;

	private int requestsMade;

	public TestElevationProvider(double south, double north, double west,
			double east, double minElev, double maxElev) {
		this.south = south;
		this.north = north;
		this.west = west;
		this.east = east;
		this.minElev = minElev;
		this.maxElev = maxElev;

		requestsMade = 0;
	}



	@Override
	public List<Double> fetchElevations(List<Coordinate> l) throws DataUnavailableException {
		requestsMade++;

		ArrayList<Double> elevations = new ArrayList<Double>(l.size());
		for(Coordinate c : l) {
			double latfrac = (c.x-south)/(north-south);
			double lonfrac = (c.y-west)/(east-west);

			double elev;
			// outside the box, return random values
			if(0>latfrac || latfrac>1 || 0>lonfrac || lonfrac>1) {
				elev = Math.random()*(maxElev-minElev)+minElev;
			} else {
				if(latfrac<.05)
					elev = minElev;
				else
					elev = (latfrac+lonfrac)/2*(maxElev-minElev)+minElev;
			}
			elevations.add(elev);
		}
		return elevations;
	}

	@Override
	public Double fetchElevation(Coordinate c) throws DataUnavailableException {
		requestsMade++;

		double latfrac = (c.x-south)/(north-south);
		double lonfrac = (c.y-west)/(east-west);

		double elev;
		// outside the box, return random values
		if(0>latfrac || latfrac>1 || 0>lonfrac || lonfrac>1) {
			elev = Math.random()*(maxElev-minElev)+minElev;
		} else {
			if(latfrac<.05)
				elev = minElev;
			else
				elev = (latfrac+lonfrac)/2*(maxElev-minElev)+minElev;
		}
		return elev;
	}

	public int getRequestsMade() {
		return requestsMade;
	}


}
