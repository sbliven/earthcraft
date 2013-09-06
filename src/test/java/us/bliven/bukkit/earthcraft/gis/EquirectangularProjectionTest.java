/**
 *
 */
package us.bliven.bukkit.earthcraft.gis;


import static org.junit.Assert.*;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
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
		coord = new Coordinate(0,0,0);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",0,loc.getX(),.1);
		assertEquals("Wrong Z",0,loc.getZ(),.1);
		assertEquals("Wrong Y",0,loc.getY(),.1);

		// East
		coord = new Coordinate(0,.1,0);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",1,loc.getX(),.1);
		assertEquals("Wrong Z",0,loc.getZ(),.1);
		assertEquals("Wrong Y",0,loc.getY(),.1);

		// West
		coord = new Coordinate(0,-.1,0);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",-1,loc.getX(),.1);
		assertEquals("Wrong Z",0,loc.getZ(),.1);
		assertEquals("Wrong Y",0,loc.getY(),.1);

		// North
		coord = new Coordinate(.1,0,0);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",0,loc.getX(),.1);
		assertEquals("Wrong Z",-1,loc.getZ(),.1);
		assertEquals("Wrong Y",0,loc.getY(),.1);

		// South
		coord = new Coordinate(-.1,0,0);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",0,loc.getX(),.1);
		assertEquals("Wrong Z",1,loc.getZ(),.1);
		assertEquals("Wrong Y",0,loc.getY(),.1);

		// Up
		coord = new Coordinate(0,0,1);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",0,loc.getX(),.1);
		assertEquals("Wrong Z",0,loc.getZ(),.1);
		assertEquals("Wrong Y",2,loc.getY(),.1);

		// Down
		coord = new Coordinate(0,0,-1);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",0,loc.getX(),.1);
		assertEquals("Wrong Z",0,loc.getZ(),.1);
		assertEquals("Wrong Y",-2,loc.getY(),.1);


		// Diagonal
		coord = new Coordinate(.1,.5,3);
		loc = proj.coordinateToLocation(world, coord);
		assertEquals("Wrong X",5,loc.getX(),.1);
		assertEquals("Wrong Z",-1,loc.getZ(),.1);
		assertEquals("Wrong Y",6,loc.getY(),.1);
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
		assertEquals("Wrong elev",0,coord.z,1e-5);

		// East
		loc = new Location(world, 1, 0, 0);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",0,coord.x,1e-5);
		assertEquals("Wrong lon",.1,coord.y,1e-5);
		assertEquals("Wrong elev",0,coord.z,1e-5);

		// West
		loc = new Location(world, -1, 0, 0);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",0,coord.x,1e-5);
		assertEquals("Wrong lon",-.1,coord.y,1e-5);
		assertEquals("Wrong elev",0,coord.z,1e-5);

		// South
		loc = new Location(world, 0, 0, 1);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.1,coord.x,1e-5);
		assertEquals("Wrong lon",0,coord.y,1e-5);
		assertEquals("Wrong elev",0,coord.z,1e-5);

		// North
		loc = new Location(world, 0, 0, -1);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",.1,coord.x,1e-5);
		assertEquals("Wrong lon",0,coord.y,1e-5);
		assertEquals("Wrong elev",0,coord.z,1e-5);

		// Up
		loc = new Location(world, 0, 3, 0);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",0,coord.x,1e-5);
		assertEquals("Wrong lon",0,coord.y,1e-5);
		assertEquals("Wrong elev",1.5,coord.z,1e-5);

		// Down
		loc = new Location(world, 0, -3, 0);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",0,coord.x,1e-5);
		assertEquals("Wrong lon",0,coord.y,1e-5);
		assertEquals("Wrong elev",-1.5,coord.z,1e-5);

		// Diagonal
		loc = new Location(world, 3, 5, 4);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.4,coord.x,1e-5);
		assertEquals("Wrong lon",.3,coord.y,1e-5);
		assertEquals("Wrong elev",2.5,coord.z,1e-5);



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
		assertEquals("Wrong elev",origin.z,coord.z,1e-5);

		origin.x = 20;
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y,coord.y,1e-5);
		assertEquals("Wrong elev",origin.z,coord.z,1e-5);

		origin.y = -15;
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y,coord.y,1e-5);
		assertEquals("Wrong elev",origin.z,coord.z,1e-5);


		origin.z = -40;
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y,coord.y,1e-5);
		assertEquals("Wrong elev",origin.z,coord.z,1e-5);

		origin.x = 0;
		origin.y = 0;
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y,coord.y,1e-5);
		assertEquals("Wrong elev",origin.z,coord.z,1e-5);

		// Now include scaling
		loc = new Location(world, 1., 1., 1.);
		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",origin.x-scale.x,coord.x,1e-5);
		assertEquals("Wrong lon",origin.y+scale.y,coord.y,1e-5);
		assertEquals("Wrong elev",origin.z+scale.z,coord.z,1e-5);

	}


	@Test public void testScale() {
		Location loc;
		Coordinate coord;
		Coordinate scale = proj.getScale();


		loc = new Location(world, 1, 1, 1);

		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.1,coord.x,1e-5);
		assertEquals("Wrong lon",.1,coord.y,1e-5);
		assertEquals("Wrong elev",.5,coord.z,1e-5);

		scale.x = .2;

		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.2,coord.x,1e-5);
		assertEquals("Wrong lon",.1,coord.y,1e-5);
		assertEquals("Wrong elev",.5,coord.z,1e-5);

		scale.y = .2;

		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.2,coord.x,1e-5);
		assertEquals("Wrong lon",.2,coord.y,1e-5);
		assertEquals("Wrong elev",.5,coord.z,1e-5);

		scale.z = 1.;

		coord = proj.locationToCoordinate(loc);
		assertEquals("Wrong lat",-.2,coord.x,1e-5);
		assertEquals("Wrong lon",.2,coord.y,1e-5);
		assertEquals("Wrong elev",1.,coord.z,1e-5);

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
			assertEquals("LatLon->MC->LatLon failure (Z)",start.z, secondCoord.z, tol.z);

			Location thirdLoc = proj.coordinateToLocation(world, secondCoord);

			assertEquals("MC->LatLon->MC failure (X)", firstLoc.getX(), thirdLoc.getX(), tol.y);
			assertEquals("MC->LatLon->MC failure (Y)", firstLoc.getY(), thirdLoc.getY(), tol.z);
			assertEquals("MC->LatLon->MC failure (Z)", firstLoc.getZ(), thirdLoc.getZ(), tol.x);
		}
	}
}
