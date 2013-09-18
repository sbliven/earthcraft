/**
 *
 */
package us.bliven.bukkit.earthcraft.gis;


import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.bukkit.World;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Spencer Bliven
 */
public class LinearElevationProjectionTest {
	LinearElevationProjection proj = null;
	World world = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		double origin = 0.;
		double scale = .5;
		proj = new LinearElevationProjection(origin, scale);
	}


	@Test
	public void testElevationToY() {
		double elev;
		double y;

		// Coordinates are (lat,lon,elev)
		// Locations are (east,elev,south) or (lon,elev,-lat)

		// Check origin
		elev = 0.;
		y = proj.elevationToY(elev);
		assertEquals("Wrong Y",0,y,.1);

		// Up
		elev = 1.;
		y = proj.elevationToY(elev);
		assertEquals("Wrong Y",2,y,.1);

		// Down
		elev = -1.;
		y = proj.elevationToY(elev);
		assertEquals("Wrong Y",-2,y,.1);

		// Diagonal
		elev = 3.;
		y = proj.elevationToY(elev);
		assertEquals("Wrong Y",6,y,.1);
	}

	@Test
	public void testYToElevation() {
		double elev;
		double y;

		// Origin
		y = 0.;
		elev = proj.yToElevation(y);
		assertEquals("Wrong Y",0,elev,.1);

		// Up
		y = 3.;
		elev = proj.yToElevation(y);
		assertEquals("Wrong Y",1.5,elev,.1);

		// Down
		y = -3.;
		elev = proj.yToElevation(y);
		assertEquals("Wrong Y",-1.5,elev,.1);

		// Diagonal
		y = 5.4;
		elev = proj.yToElevation(y);
		assertEquals("Wrong Y",2.7,elev,.1);

	}

	@Test
	public void testOrigin() {
		double elev;
		double y;
		double origin,scale;

		// Test 0,0,0 should be at the origin
		y = 0;

		origin = 0.;
		proj = new LinearElevationProjection(origin, 1.);
		elev = proj.yToElevation(y);
		assertEquals("Wrong elev",origin,elev,1e-5);

		origin = 20;
		proj = new LinearElevationProjection(origin, 1.);
		elev = proj.yToElevation(y);
		assertEquals("Wrong elev",origin,elev,1e-5);

		origin = -15;
		proj = new LinearElevationProjection(origin, 1.);
		elev = proj.yToElevation(y);
		assertEquals("Wrong elev",origin,elev,1e-5);

		origin = -40;
		proj = new LinearElevationProjection(origin, 1.);
		elev = proj.yToElevation(y);
		assertEquals("Wrong elev",origin,elev,1e-5);

		// Now include scaling
		y = 1;
		origin = 2.;
		scale = 3.;
		proj = new LinearElevationProjection(origin, scale);
		elev = proj.yToElevation(y);
		assertEquals("Wrong elev",5.,elev,1e-5);

		y = 4;
		origin = 2.;
		scale = 3.;
		proj = new LinearElevationProjection(origin, scale);
		elev = proj.yToElevation(y);
		assertEquals("Wrong elev",14.,elev,1e-5);

	}


	@Test public void testScale() {
		double elev;
		double y;
		double origin,scale;

		origin = 0;
		scale = .5;
		y = 1.;

		proj = new LinearElevationProjection(origin, scale);
		elev = proj.yToElevation(y);
		assertEquals("Wrong elev",.5,elev,1e-5);

		scale = 2;
		proj = new LinearElevationProjection(origin, scale);
		elev = proj.yToElevation(y);
		assertEquals("Wrong elev",2.,elev,1e-5);

		origin = 3;
		scale = 2;
		y= -4;
		proj = new LinearElevationProjection(origin, scale);
		elev = proj.yToElevation(y);
		assertEquals("Wrong elev",-5.,elev,1e-5);

	}

	@Test
	public void testInverse() {
		double tol = 1e-5;

		Random rand = new Random();
		for(int i=0;i<5;i++) {
			double start = rand.nextDouble()*256-128;
			double firstY = proj.elevationToY( start);
			double secondElev = proj.yToElevation(firstY);

			assertEquals("Elevation->MC->Elevation",start, secondElev, tol);

			double thirdY = proj.elevationToY(secondElev);

			assertEquals("MC->Elevation->MC failure", firstY, thirdY, tol);
		}
	}
}
