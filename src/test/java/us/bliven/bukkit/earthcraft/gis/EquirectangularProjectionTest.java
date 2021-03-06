/**
 *
 */
package us.bliven.bukkit.earthcraft.gis;


import static org.junit.Assert.assertEquals;

import java.util.Random;

import net.minecraft.world.World;

import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author Spencer Bliven
 */
public class EquirectangularProjectionTest {
	EquirectangularProjection proj = null;
	World world = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		Coordinate origin = new Coordinate(0.,0.,0.);
		Coordinate scale = new Coordinate(.1,.1,.5);
		proj = new EquirectangularProjection(origin, scale);
	}


	@Test
	public void testCoordinateToLocation() {
		Coordinate coord;
		Location loc;

		// Coordinates are (lat,lon,elev)
		// Locations are (east,elev,south) or (lon,elev,-lat)

		// Check origin
		coord = new Coordinate(0,0);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",0,loc.getX(),.1);
		assertEquals("Wrong Z",0,loc.getZ(),.1);

		// East
		coord = new Coordinate(0,.1);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",1,loc.getX(),.1);
		assertEquals("Wrong Z",0,loc.getZ(),.1);

		// West
		coord = new Coordinate(0,-.1);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",-1,loc.getX(),.1);
		assertEquals("Wrong Z",0,loc.getZ(),.1);

		// North
		coord = new Coordinate(.1,0);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",0,loc.getX(),.1);
		assertEquals("Wrong Z",-1,loc.getZ(),.1);

		// South
		coord = new Coordinate(-.1,0);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",0,loc.getX(),.1);
		assertEquals("Wrong Z",1,loc.getZ(),.1);

		// Diagonal
		coord = new Coordinate(.1,.5);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",5,loc.getX(),.1);
		assertEquals("Wrong Z",-1,loc.getZ(),.1);

		// Border
		coord = new Coordinate(90,0);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",0,loc.getX(),.1);
		assertEquals("Wrong Z",-900,loc.getZ(),.1);
	}

	@Test
	public void testLocationToCoordinate() {
		Location loc;
		Coordinate coord;

		// Origin
		loc = new Location(world, 0, 0, 0);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",0,coord.x,1e-5);
		assertEquals("Wrong lon",0,coord.y,1e-5);

		// East
		loc = new Location(world, 1, 0, 0);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",0,coord.x,1e-5);
		assertEquals("Wrong lon",.1,coord.y,1e-5);

		// West
		loc = new Location(world, -1, 0, 0);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",0,coord.x,1e-5);
		assertEquals("Wrong lon",-.1,coord.y,1e-5);

		// South
		loc = new Location(world, 0, 0, 1);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.1,coord.x,1e-5);
		assertEquals("Wrong lon",0,coord.y,1e-5);

		// North
		loc = new Location(world, 0, 0, -1);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",.1,coord.x,1e-5);
		assertEquals("Wrong lon",0,coord.y,1e-5);

		// Diagonal
		loc = new Location(world, 3, 5, 4);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.4,coord.x,1e-5);
		assertEquals("Wrong lon",.3,coord.y,1e-5);

		// Border
		loc = new Location(world, 0,0,-900);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",90,coord.x,1e-5);
		assertEquals("Wrong lon",0,coord.y,1e-5);
	}

	@Test
	public void testOrigin() {
		Location loc;
		Coordinate coord;

		Coordinate origin = proj.getOrigin();
		Coordinate scale = proj.getScale();

		// Test 0,0,0 should be at the origin
		loc = new Location(world, 0, 0, 0);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y,coord.y,1e-5);

		origin.x = 20;
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y,coord.y,1e-5);

		origin.y = -15;
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y,coord.y,1e-5);

		origin.x = 0;
		origin.y = 0;
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y,coord.y,1e-5);

		// Now include scaling
		loc = new Location(world, 1., 1., 1.);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x-scale.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y+scale.y,coord.y,1e-5);
	}


	@Test public void testScale() {
		Location loc;
		Coordinate coord;
		Coordinate scale = proj.getScale();


		loc = new Location(world, 1, 0, 1);

		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.1,coord.x,1e-5);
		assertEquals("Wrong lon",.1,coord.y,1e-5);

		scale.x = .2;

		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.2,coord.x,1e-5);
		assertEquals("Wrong lon",.1,coord.y,1e-5);

		scale.y = .2;

		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.2,coord.x,1e-5);
		assertEquals("Wrong lon",.2,coord.y,1e-5);

		scale.z = 1.;

		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.2,coord.x,1e-5);
		assertEquals("Wrong lon",.2,coord.y,1e-5);
	}

	@Test
	public void testInverse() {
		Coordinate tol = proj.getScale();

		Random rand = new Random();
		for(int i=0;i<5;i++) {
			Coordinate start = new Coordinate(
					rand.nextDouble()*360-180,
					rand.nextDouble()*180-90,
					rand.nextDouble()*256-128);
			Location firstLoc = proj.coordinateToLocation(world, start);
			Coordinate secondCoord = proj.locationToCoordinate(firstLoc);

			assertEquals("LatLon->MC->LatLon failure (X)",start.x, secondCoord.x, tol.x);
			assertEquals("LatLon->MC->LatLon failure (Y)",start.y, secondCoord.y, tol.y);
			assertEquals("LatLon->MC->LatLon failure (Z)",Double.NaN, secondCoord.z, tol.z);

			Location thirdLoc = proj.coordinateToLocation(world, secondCoord);

			assertEquals("MC->LatLon->MC failure (X)", firstLoc.getX(), thirdLoc.getX(), tol.y);
			assertEquals("MC->LatLon->MC failure (Y)", firstLoc.getY(), thirdLoc.getY(), tol.z);
			assertEquals("MC->LatLon->MC failure (Z)", firstLoc.getZ(), thirdLoc.getZ(), tol.x);
		}
	}
}
