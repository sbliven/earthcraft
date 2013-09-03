package us.bliven.bukkit.earthcraft.gis;

import static org.junit.Assert.*;

import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

public class ProjectionToolsTest {

	@Test
	public void testWrapCoordinate() {
		Coordinate input,result,expected;

		input = new Coordinate(-80,50);
		expected = input;
		result = ProjectionTools.wrapCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(90.,-180.,50.5);
		expected = input;
		result = ProjectionTools.wrapCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(0,200, 10.);
		expected = new Coordinate(0,-160.,10.);
		result = ProjectionTools.wrapCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(0,-360, 10.);
		expected = new Coordinate(0,0.,10.);
		result = ProjectionTools.wrapCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(180*3+40,360*-5);
		expected = new Coordinate(40,0);
		result = ProjectionTools.wrapCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(-90,180);
		expected = new Coordinate(90,-180);
		result = ProjectionTools.wrapCoordinate(input);
		assertEquals(expected,result);

	}

	@Test
	public void testGetGlobalCoordinate() {
		Coordinate input,result,expected;

		input = new Coordinate(-80,50);
		expected = new Coordinate(0,0);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(90.,-180.,50.5);
		expected = new Coordinate(0,0);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(0,200, 10.);
		expected = new Coordinate(0,1);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(0,370, 10.);
		expected = new Coordinate(0,1);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(0,360+180, 10.);
		expected = new Coordinate(0,2);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(0,-360, 10.);
		expected = new Coordinate(0,-1);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(100,0);
		expected = new Coordinate(1,0);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(190,0);
		expected = new Coordinate(1,0);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(180*3+40,360*-5);
		expected = new Coordinate(3,-5);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

		input = new Coordinate(-90,180);
		expected = new Coordinate(-1,1);
		result = ProjectionTools.getGlobeCoordinate(input);
		assertEquals(expected,result);

	}

	@Test
	public void testLatlonString() {
		Coordinate input;
		String expected,result;

		input = new Coordinate(5,10);
		expected = "5.0N 10.0E";
		result = ProjectionTools.latlonString(input);
		assertEquals(expected, result);

		input = new Coordinate(-5.7,-10.);
		expected = "5.7S 10.0W";
		result = ProjectionTools.latlonString(input);
		assertEquals(expected, result);

		input = new Coordinate(3*180-5.7,-4*360-10.);
		expected = "5.7000000000000455S*3 10.0W*-4";
		result = ProjectionTools.latlonString(input);
		assertEquals(expected, result);


	}
}
