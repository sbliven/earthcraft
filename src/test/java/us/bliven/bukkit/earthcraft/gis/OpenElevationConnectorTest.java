package us.bliven.bukkit.earthcraft.gis;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;

public class OpenElevationConnectorTest {
	private ArrayList<Coordinate> latlong;
	private List<Double> elevations;
	OpenElevationConnector oec;
	@Before
	public void setup() {
		String authKey = "YOUR_KEY_HERE";
		oec = new OpenElevationConnector(authKey);
		
		latlong = new ArrayList<Coordinate>();
		elevations = new ArrayList<Double>();

		// Colorado state capital building
		latlong.add(new Coordinate(39.740112,-104.984856));
		elevations.add(1616.0);

		// gulf of thailand
		latlong.add(new Coordinate(12.024642,100.168254));
		elevations.add(0.0);

		// Summit of Mt. St. Helens
		latlong.add(new Coordinate(46.191454,-122.195850));
		elevations.add(2518.0);

		// Mt. Everest
		latlong.add(new Coordinate(27.98806, 86.92528));
		elevations.add(null); // -32768.0 response code for missing data?

		// South pole
		latlong.add(new Coordinate(-89, 0));
		elevations.add(null);

		// Byrd Station
		latlong.add(new Coordinate(-80.0166667, -119.5333333));
		elevations.add(null);

		// Challenger Deep
		latlong.add(new Coordinate(11.3733, 142.5917));
		elevations.add( null );


	}

	@Test
	public void testFetchElevationIndiv() throws DataUnavailableException {

		for(int i=0;i<latlong.size();i++) {
			List<Coordinate> l = Lists.newArrayList(latlong.get(i));
			List<Double> e = oec.fetchElevations(l);
			assertEquals("Incorrect number of elevations",1,e.size());
			if(e.get(0) == null || elevations.get(i) == null) {
				assertEquals( elevations.get(i),e.get(0));
			} else {
				// Both doubles;
				assertEquals( elevations.get(i),e.get(0),1e-10);
			}
		}
	}

	@Test
	public void testFetchElevationFull() throws DataUnavailableException {
		List<Double> e = oec.fetchElevations(latlong);
		assertEquals("Incorrect number of elevations",elevations.size(),e.size());
		for(int i=0;i<e.size();i++) {
			if(e.get(i) == null || elevations.get(i) == null) {
				assertEquals( elevations.get(i),e.get(i));
			} else {
				// Both doubles;
				assertEquals( elevations.get(i),e.get(i),1e-10);
			}
		}
	}

}
