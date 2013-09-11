/**
 *
 */
package us.bliven.bukkit.earthcraft.gis;


import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author Spencer Bliven
 */
public class InterpolatedCoverageElevationProviderTest {
	InterpolatedCoverageElevationProvider interp;
	double d = 50/6000.; //grid scale

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		String dir = "/Users/blivens/dev/minecraft/srtm";
		if(! (new File(dir)).exists()) {
			dir = System.getProperty("java.io.tmpdir") + "SRTMPlus";
		}

		SRTMPlusElevationProvider srtm = new SRTMPlusElevationProvider(dir);
		interp = new InterpolatedCoverageElevationProvider(srtm);
	}


	@Test
	public void testAlaska() throws Exception {
		double elev;
		Coordinate pos;

		pos = new Coordinate(60,-170);
		elev = interp.fetchElevation(pos);
		assertEquals(-46,elev,1e-6);
	}

	/**
	 * Test interpolation around tile boundaries
	 *
	 * This tends to fail or produce artifacts.
	 * @throws Exception
	 */
	@Test
	public void testTileBoundaries() throws Exception {
		double elev;
		Coordinate pos;

		pos = new Coordinate(60,-180);
		elev = interp.fetchElevation(pos);
		assertEquals(-2638.,elev,1e-6);

		pos = new Coordinate(60,180);
		elev = interp.fetchElevation(pos);
		assertEquals(-2638.,elev,1e-6);

		pos = new Coordinate(63.370000000000005,-180.0);
		elev = interp.fetchElevation(pos);
		assertEquals(-65.,elev,1e-6);

	}
}
