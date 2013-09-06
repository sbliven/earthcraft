/**
 * Tests that the LRU cache works as expected.
 *
 * This is not really needed since we now use geotool's LRU cache
 * rather than a custom implementation, but leave it for documentation.
 */
package us.bliven.bukkit.earthcraft.util;


import static org.junit.Assert.*;

import org.geotools.util.LRULinkedHashMap;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Spencer Bliven
 */
@Deprecated
public class LruCacheTest {
	//LruCache<Integer,String> lru;
	LRULinkedHashMap<Integer, String> lru;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		//lru = new LruCache<Integer,String>(5);
		lru = LRULinkedHashMap.createForRecentAccess(5);
	}

	@Test
	public void testLRUUpdates() {
		lru.put(1, "First");
		lru.put(2, "Second");
		lru.put(3, "Third");
		lru.put(4, "Fourth");

		Integer i = 1;
		for(Integer key : lru.keySet()) {
			assertEquals(i,key);
			i++;
		}

		i = 1;
		for(Integer key : lru.keySet()) {
			assertEquals(i,key);
			i++;
		}
		// get updates access time
		lru.get(1);

		i = 2;
		for(Integer key : lru.keySet()) {
			assertEquals(i,key);
			i = (i%4)+1;
		}
		// repeat put updates access time
		lru.put(2, "Fifth");

		i = 3;
		for(Integer key : lru.keySet()) {
			assertEquals(i,key);
			i = (i%4)+1;
		}
	}

	@Test
	public void  testEviction() {
		lru.put(1, "First");
		lru.put(2, "Second");
		lru.put(3, "Third");
		lru.put(4, "Fourth");
		lru.put(5, "Fifth");

		Integer i = 1;
		for(Integer key : lru.keySet()) {
			assertEquals(i,key);
			i++;
		}

		lru.put(6,"Sixth"); // evicts 1

		assertEquals(5,lru.size());

		i = 2;
		for(Integer key : lru.keySet()) {
			assertEquals(i,key);
			i++;
		}
	}

}
