/**
 * 
 */
package us.bliven.bukkit.earthcraft.gis;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author Spencer Bliven
 */
public class InterpolatingElevationCacheTest {

	private InterpolatingElevationCache cache;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		ElevationProvider provider = new ElevationProviderStub();
		Coordinate origin = new Coordinate(0.,0.);
		Coordinate gridScale = new Coordinate(1.,1.);
		cache = new InterpolatingElevationCache(provider, origin, gridScale);
	}
	
	@Test
	public void testFetchCoordinates() throws Exception {
		List<Coordinate> query = new ArrayList<Coordinate>();
		List<Double> expected = new ArrayList<Double>();
		List<Double> results;
		
		query.add(new Coordinate(1.,1.));
		expected.add(1.);
		
		results = cache.fetchElevations(query);
		assertEquals("Results differ for "+query,expected,results);
		
		query.set(0, new Coordinate(1.1,1.9));
		expected.set(0, 1.);
		
		results = cache.fetchElevations(query);
		assertEquals("Results differ for "+query,expected,results);
		
		query.add(new Coordinate(2.5,2.5));
		expected.add(3.);
		
		results = cache.fetchElevations(query);
		assertEquals("Results differ for "+query,expected,results);
			
	}

}
