package us.bliven.bukkit.earthcraft.gis;

import static org.junit.Assert.assertEquals;

import java.awt.Point;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;


public class LatticeTest {
	Lattice identity, offset;
	
	@Before
	public void setup() {
		identity = new Lattice(new Coordinate(0.,0.),new Coordinate(1.,1.));
		
		offset = new Lattice(new Coordinate(.5,1.),new Coordinate(1.,2.));

	}
	@Test
	public void testNearestNeighbors() {
		Set<Point> neighbors;
		Coordinate query;
		
		query = new Coordinate(5.,5.);
		neighbors = identity.getNeighbors(query, 0);
		assertEquals("Wrong size for "+query,1,neighbors.size());
		assertEquals("Wrong neighbor for "+query,new Point(5,5),neighbors.iterator().next());
		
		query = new Coordinate(5.4,5.4);
		neighbors = identity.getNeighbors(query, 0);
		assertEquals("Wrong size for "+query,1,neighbors.size());
		assertEquals("Wrong neighbor for "+query,new Point(5,5),neighbors.iterator().next());
		
		query = new Coordinate(4.5,5.1);
		neighbors = identity.getNeighbors(query, 0);
		assertEquals("Wrong size for "+query,1,neighbors.size());
		assertEquals("Wrong neighbor for "+query,new Point(5,5),neighbors.iterator().next());

		query = new Coordinate(-5.5,-4.9);
		neighbors = identity.getNeighbors(query, 0);
		assertEquals("Wrong size for "+query,1,neighbors.size());
		assertEquals("Wrong neighbor for "+query,new Point(-5,-5),neighbors.iterator().next());
	}
	
	@Test
	public void testNearestKNeighbors() {
		Set<Point> neighbors;
		Coordinate query;
		Set<Point> expected;
		
		expected = new LinkedHashSet<Point>();
		expected.add(new Point(6,6));
		expected.add(new Point(5,6));
		expected.add(new Point(5,5));
		expected.add(new Point(6,5));
		
		query = new Coordinate(5.,5.);
		neighbors = identity.getNeighbors(query, 1);
		assertEquals("Wrong size for "+query,expected.size(),neighbors.size());
		assertEquals("Wrong neighbor for "+query,expected,neighbors);
		

		
		expected.add(new Point(6,7));
		expected.add(new Point(5,7));
		expected.add(new Point(5,4));
		expected.add(new Point(6,4));

		expected.add(new Point(7,6));
		expected.add(new Point(4,6));
		expected.add(new Point(4,5));
		expected.add(new Point(7,5));
		
		query = new Coordinate(5.,5.);
		neighbors = identity.getNeighbors(query, 2);
		assertEquals("Wrong size for "+query,expected.size(),neighbors.size());
		assertEquals("Wrong neighbor for "+query,expected,neighbors);

		query = new Coordinate(5.9,5.9);
		neighbors = identity.getNeighbors(query, 2);
		assertEquals("Wrong size for "+query,expected.size(),neighbors.size());
		assertEquals("Wrong neighbor for "+query,expected,neighbors);

		query = new Coordinate(5.,5.);
		neighbors = identity.getNeighbors(query, 5);
		assertEquals("Wrong size for "+query,2*5*(5+1),neighbors.size());
	
	}
	
	@Test
	public void testGetCoordinates() {
		Point query;
		Coordinate result, expected;

		query = new Point(5,5);
		expected = new Coordinate(5.,5.);
		result = identity.getCoordinate(query);
		assertEquals("Bad coordinate for "+query,expected,result);
		
		query = new Point(5,-5);
		expected = new Coordinate(5.,-5.);
		result = identity.getCoordinate(query);
		assertEquals("Bad coordinate for "+query,expected,result);
		
	}
	
	@Test
	public void testOffsetGetCoordinates() {
		
		Point query;
		Coordinate result, expected;

		query = new Point(0,0);
		expected = new Coordinate(.5,1.);
		result = offset.getCoordinate(query);
		assertEquals("Bad coordinate for "+query,expected,result);

		query = new Point(5,5);
		expected = new Coordinate(5.5,11.);
		result = offset.getCoordinate(query);
		assertEquals("Bad coordinate for "+query,expected,result);
	}
	
	@Test
	public void testOffsetNeighbors() {
		Set<Point> neighbors;
		Coordinate query;
		Set<Point> expected;
		
		expected = new LinkedHashSet<Point>();
		expected.add(new Point(2,1));
		expected.add(new Point(1,1));
		expected.add(new Point(1,0));
		expected.add(new Point(2,0));
		
		query = new Coordinate(2.,2.);
		neighbors = offset.getNeighbors(query, 1);
		assertEquals("Wrong size for "+query,expected.size(),neighbors.size());
		assertEquals("Wrong neighbor for "+query,expected,neighbors);
		
		query = new Coordinate(1.5,1.);
		neighbors = offset.getNeighbors(query, 1);
		assertEquals("Wrong size for "+query,expected.size(),neighbors.size());
		assertEquals("Wrong neighbor for "+query,expected,neighbors);
	
	}
}
