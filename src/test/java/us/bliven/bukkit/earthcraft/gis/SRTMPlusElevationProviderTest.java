/**
 *
 */
package us.bliven.bukkit.earthcraft.gis;


import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.gtopo30.GTopo30Reader;
import org.geotools.geometry.DirectPosition2D;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.geometry.DirectPosition;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author Spencer Bliven
 */
public class SRTMPlusElevationProviderTest {
	SRTMPlusElevationProvider srtm;
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

		srtm = new SRTMPlusElevationProvider(dir);
	}

	/**
	 * Test that the grid is oriented right-side-up
	 * by comparing the corners to known elevations
	 * @throws Exception
	 */
	@Test
	public void testGridOrientation() throws Exception {
		/*
		 * The w140n40 grid has north america in the top right,
		 * and extends down to indonesia in the bottom left.
		 *
		 * The following positions should be the same regardless
		 * of the grid's geometry. The expected values are taken
		 * directly from the file hex and are consistent with
		 * google earth.
		 *
		 * 0,0 is the northwest
		 * It increases eastward to 4799,0
		 * It wraps around to 1,0 (cell 4800) in the northwest
		 * It ends at 4799,5999 in the southeast
		 */
		//GridCoverage2D grid = srtm.loadGrid(new Coordinate(34,-117));

		File file = new File("/Users/blivens/dev/minecraft/srtm/w140n40.Bathymetry.srtm.dem");
		GTopo30Reader reader = new GTopo30Reader( file );
		GridCoverage2D grid = reader.read(null);

			
		GridCoordinates2D pos;
		pos = new GridCoordinates2D(0,0);
		assertEquals(-4338,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(1,0);
		assertEquals(-4377,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(100,0);
		assertEquals(-4106,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(479,0);
		assertEquals(-4466,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(480,0);
		assertEquals(-4337,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(500,0);
		assertEquals(-3406,grid.evaluate(pos,(int[])null)[0]);


		pos = new GridCoordinates2D(1000,0);
		assertEquals(-4366,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(2000,0);
		assertEquals(410,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(4799,0);
		assertEquals(693,grid.evaluate(pos,(int[])null)[0]);
		pos = new GridCoordinates2D(0,1);
		assertEquals(-4392,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(4799,5999);
		assertEquals(-4339,grid.evaluate(pos,(int[])null)[0]);
	}

	@Test
	public void testAntarctica() throws Exception {
		GridCoverage2D grid = srtm.loadGrid(new Coordinate(-70,-170));

		/*
		(0, 0)	0xf062	-3998
		(1, 0)	0xf063	-3997
		7199	0xede7	-4633
		7200	0xf069	-3991
		(0, 1)	0xf069	-3991
		(0, 3199)	0x0b69	2921
		(7199, 3199)	0x0993	2451
		 */

		GridCoordinates2D pos;
		pos = new GridCoordinates2D(0,0);
		assertEquals(-3998,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(1,0);
		assertEquals(-3997,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(7199,0);
		assertEquals(-4633,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(0,1);
		assertEquals(-3991,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(0,3199);
		assertEquals(2921,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(7199, 3199);
		assertEquals(2451,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(0, 3599);
		assertEquals(2774,grid.evaluate(pos,(int[])null)[0]);

		pos = new GridCoordinates2D(7199, 3599);
		assertEquals(2774,grid.evaluate(pos,(int[])null)[0]);


	}

	/**
	 * Test that exceptions are thrown where expected
	 * @throws Exception
	 */
	@Test
	public void testMissingData() throws Exception {
		GridCoverage2D grid = srtm.loadGrid(new Coordinate(34,-117));

		GridCoordinates2D gridPos;
		DirectPosition pos;

		try {
			gridPos = new GridCoordinates2D(-1,0);
			grid.evaluate(gridPos,(int[])null);
			fail("Failed to throw PointOutsideCoverageException");
		} catch( PointOutsideCoverageException e) {
			//expected
		}

		try {
			pos = new DirectPosition2D(-140-d/4,40);
			grid.evaluate(pos);
			fail("Failed to throw PointOutsideCoverageException");
		} catch( PointOutsideCoverageException e) {
			//expected
		}

		try {
			pos = new DirectPosition2D(-100,40);
			grid.evaluate(pos);
			fail("Failed to throw PointOutsideCoverageException");
		} catch( PointOutsideCoverageException e) {
			//expected
		}
		try {
			pos = new DirectPosition2D(-140,-10);
			grid.evaluate(pos);
			fail("Failed to throw PointOutsideCoverageException");
		} catch( PointOutsideCoverageException e) {
			//expected
		}
	}

	/**
	 * Test that grids are being correctly loaded with the proper geometry
	 *
	 * Doesn't test any actual elevations
	 * @throws Exception
	 */
	@Test
	public void testGridGeometry() throws Exception {
		// load the SD grid
		GridCoverage2D grid = srtm.loadGrid(new Coordinate(34,-117));
		GridGeometry2D geom = grid.getGridGeometry();

		GridCoordinates2D pos;
		DirectPosition result;

		pos = new GridCoordinates2D(0, 0);
		result = geom.gridToWorld(pos);
		//System.out.println(geom.gridToWorld(pos));
		assertEquals(-140.+d/2, result.getOrdinate(0),1e-5);
		assertEquals(40.-d/2, result.getOrdinate(1),1e-5);

		pos = new GridCoordinates2D(4799, 0);
		result = geom.gridToWorld(pos);
		//System.out.println(geom.gridToWorld(pos));
		assertEquals(-100.-d+d/2, result.getOrdinate(0),1e-5);
		assertEquals(40.-d/2, result.getOrdinate(1),1e-5);

		pos = new GridCoordinates2D(0, 1);
		result = geom.gridToWorld(pos);
		//System.out.println(geom.gridToWorld(pos));
		assertEquals(-140.+d/2, result.getOrdinate(0),1e-5);
		assertEquals(40.-d-d/2, result.getOrdinate(1),1e-5);

		pos = new GridCoordinates2D(4799,5999);
		result = geom.gridToWorld(pos);
		//System.out.println(geom.gridToWorld(pos));
		assertEquals(-100.-d+d/2, result.getOrdinate(0),1e-5);
		assertEquals(-10+d-d/2, result.getOrdinate(1),1e-5);

		// Test bounds
		// Pixels give elevation at center of a square

		result = new DirectPosition2D(-140.,40.);
		pos = geom.worldToGrid(result);
		assertEquals(0, pos.getCoordinateValue(0));
		assertEquals(0, pos.getCoordinateValue(1));

		result = new DirectPosition2D(-140-d/4,40+d/4);
		pos = geom.worldToGrid(result);
		assertEquals(-1, pos.getCoordinateValue(0));
		assertEquals(-1, pos.getCoordinateValue(1));

		result = new DirectPosition2D(-140+d*3/4,40-d*3/4);
		pos = geom.worldToGrid(result);
		assertEquals(0, pos.getCoordinateValue(0));
		assertEquals(0, pos.getCoordinateValue(1));

		result = new DirectPosition2D(-140+d,40-d);
		pos = geom.worldToGrid(result);
		assertEquals(1, pos.getCoordinateValue(0));
		assertEquals(1, pos.getCoordinateValue(1));



		grid = srtm.loadGrid(new Coordinate(-70,-60));
		geom = grid.getGridGeometry();

		pos = new GridCoordinates2D(0,0);
		result = geom.gridToWorld(pos);
		assertEquals(-60.+d/2, result.getOrdinate(0),1e-5);
		assertEquals(-60-d/2, result.getOrdinate(1),1e-5);

		result = new DirectPosition2D(-60+d/2,-60-d/2);
		pos = geom.worldToGrid(result);
		assertEquals(0, pos.getCoordinateValue(0));
		assertEquals(0, pos.getCoordinateValue(1));

		result = new DirectPosition2D(-60,-60);
		pos = geom.worldToGrid(result);
		assertEquals(0, pos.getCoordinateValue(0));
		assertEquals(0, pos.getCoordinateValue(1));

		result = new DirectPosition2D(-60,-70);
		pos = geom.worldToGrid(result);
		assertEquals(0, pos.getCoordinateValue(0));
		assertEquals(1200, pos.getCoordinateValue(1));

	}

	/**
	 * Full test that SRTM elevations are correct
	 * @throws Exception
	 */
	@Test
	public void testElevations() throws Exception {
		Coordinate pos;
		Double result;
		List<Coordinate> poses = new ArrayList<Coordinate>();
		List<Double> expecteds = new ArrayList<Double>();


		pos = new Coordinate(40.,-140.);
		result = srtm.fetchElevation(pos);
		poses.add(pos);
		expecteds.add(result);
		assertEquals(-4338,result,1e-6);

		pos = new Coordinate(40,-100-d);
		result = srtm.fetchElevation(pos);
		poses.add(pos);
		expecteds.add(result);
		assertEquals(693,result,1e-6);

		pos = new Coordinate(40-d,-140.);
		result = srtm.fetchElevation(pos);
		poses.add(pos);
		expecteds.add(result);
		assertEquals(-4392,result,1e-6);

		pos = new Coordinate(-10+d,-100.-d);
		result = srtm.fetchElevation(pos);
		poses.add(pos);
		expecteds.add(result);
		assertEquals(-4339,result,1e-6);

		//Elevations should be constant within a square with topleft 40,-140
		pos = new Coordinate(40.-d/2,-140.+d/2);//.5,.5
		result = srtm.fetchElevation(pos);
		poses.add(pos);
		expecteds.add(result);
		assertEquals(-4338,result,1e-6);

		pos = new Coordinate(40.-d*3/4,-140.+d*3/4);//.75,.75
		result = srtm.fetchElevation(pos);
		poses.add(pos);
		expecteds.add(result);
		assertEquals(-4338,result,1e-6);

		pos = new Coordinate(40.-d,-140.+d);//1,1
		result = srtm.fetchElevation(pos);
		poses.add(pos);
		expecteds.add(result);
		assertEquals(-4428.,result,1e-6);

		pos = new Coordinate(-70, -60);//1,1
		result = srtm.fetchElevation(pos);
		poses.add(pos);
		expecteds.add(result);
		assertEquals(-584.,result,1e-6);

		List<Double> results = srtm.fetchElevations(poses);
		for(int i=0;i<expecteds.size();i++) {
			assertEquals(expecteds.get(i), results.get(i),1e-6);
		}

	}

	@Test
	public void testAlaska() throws Exception {
		double elev;
		Coordinate pos;

		pos = new Coordinate(60,-170);
		elev = srtm.fetchElevation(pos);
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
		elev = srtm.fetchElevation(pos);
		assertEquals(-2638.,elev,1e-6);

		pos = new Coordinate(60,180);
		elev = srtm.fetchElevation(pos);
		assertEquals(-2638.,elev,1e-6);

		pos = new Coordinate(63.370000000000005,-180.0);
		elev = srtm.fetchElevation(pos);
		assertEquals(-65.,elev,1e-6);

		pos = new Coordinate(-180.,-60.);
		elev = srtm.fetchElevation(pos);
		assertEquals(140.0, elev, 1e-6);

		pos = new Coordinate(-69.7,-60.);
		elev = srtm.fetchElevation(pos);

		pos = new Coordinate(110.3,-60.);
		elev = srtm.fetchElevation(pos);

		//System.out.println(elev);
	}
}
