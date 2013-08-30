package us.bliven.bukkit.earthcraft.gis;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.map.LRUMap;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;


/**
 * A cache layer for an elevation provider.
 * 
 * <p>Reduces calls to the underlying provider by
 *  1. Only fetching points from a widely-spaced lattice, then interpolating between them
 *  2. Pre-fetching nearby grid points
 * @author Spencer Bliven
 */
public class InterpolatingElevationCache implements ElevationProvider {
	private static final int MAX_ELEVATIONS_PER_SUBREQUEST = 36;
	private static final int MAX_CACHE_SIZE = 1024;
	private static final int MAX_PREFETCH_SIZE = 512;
	private static final int PREFETCH_RADIUS = 2; // radius outward from queries to prefetch

	private ElevationProvider provider;
	private LRUMap<Point,Object> prefetchStack;// Actually a LRUSet
	private Lattice lattice;
	private LRUMap<Point,Double> cache;
	
	public InterpolatingElevationCache(ElevationProvider provider, Coordinate origin, Coordinate gridScale) {
		lattice = new Lattice(origin, gridScale);
		this.provider = provider;
		prefetchStack = new LRUMap<Point,Object>();
		cache = new LRUMap<Point,Double>(MAX_CACHE_SIZE);
		prefetchStack = new LRUMap<Point, Object>(MAX_PREFETCH_SIZE);
	}
	public InterpolatingElevationCache(ElevationProvider provider, Coordinate gridScale) {
		this(provider, new Coordinate(0.,0.),gridScale);
	}

	@Override
	public Double fetchElevation(Coordinate query) throws DataUnavailableException{
		List<Double> q = fetchElevations(Lists.asList(query,new Coordinate[0]));
		return q.get(0);
	}
	
	@Override
	public List<Double> fetchElevations(List<Coordinate> queries) throws DataUnavailableException {
		ArrayList<Double> results = new ArrayList<Double>(queries.size());
		ArrayList<Point> uncached = new ArrayList<Point>();
		ArrayList<Integer> uncachedIndices = new ArrayList<Integer>();
		
		// Find uncached points
		int qnum=0;
		for(Coordinate query : queries) {
			//TODO Linear interpolation, rather than nearest neighbor
			Point gridLoc = lattice.getNearestNeighbor(query);
			if(cache.containsKey(gridLoc)) {
				// Also updates recency of gridLoc
				Double elev = cache.get(gridLoc);
				results.add(elev);
			} else {
				uncached.add(gridLoc);
				uncachedIndices.add(qnum);
				results.add(null);
				
			}
			
			qnum++;
		}
		
		// Add neighbors to precache
		prefetchNeighbors(queries);
		
		if(uncached.size() == 0) {
			// Fully cached
			return results;
		}
		
		// remove requested points from prefetchStack to remove redundancy
		for(Point p : uncached) {
			prefetchStack.remove(p);
		}
		
		// Fill uncached queue with prefetch points
		for(int i=((uncached.size()-1)%MAX_ELEVATIONS_PER_SUBREQUEST)+1;
			i< MAX_ELEVATIONS_PER_SUBREQUEST && !prefetchStack.isEmpty(); i++)
		{
			// pop the first prefetch location
			Point gridLoc = prefetchStack.firstKey();
			prefetchStack.remove(gridLoc);
			// Add to next query
			uncached.add(gridLoc);
		}
		assert(uncached.size()%MAX_ELEVATIONS_PER_SUBREQUEST == 0 || prefetchStack.isEmpty());
		
		// Fetch results for uncached
		for(int page=0;page < uncached.size(); page += MAX_ELEVATIONS_PER_SUBREQUEST) {
			int pageEnd = Math.min(uncached.size(), page+MAX_ELEVATIONS_PER_SUBREQUEST);
			List<Point> currRequestPts = uncached.subList(page, pageEnd);
			int pageEndIndices = Math.min(uncachedIndices.size(),pageEnd);
			List<Integer> currIndices = uncachedIndices.subList(page, pageEndIndices);
			List<Coordinate> currRequest = lattice.getCoordinates(currRequestPts);
			// Actual call to provider
			List<Double> elevations = provider.fetchElevations(currRequest);
			
			assert(elevations.size() == currRequest.size());
			
			// Store results & cache them
			Iterator<Double> elevationsIt = elevations.iterator();
			Iterator<Integer> currIndexIt = currIndices.iterator();
			Iterator<Point> currRequestIt = currRequestPts.iterator();
			while(currIndexIt.hasNext()) {
				
				assert(currRequestIt.hasNext()); // Should be at least as large due to prefetching
				assert(elevationsIt.hasNext()); 
				
				int index = currIndexIt.next();
				Point gridLoc = currRequestIt.next();
				Double elevation = elevationsIt.next();
				
				// Store results
				results.set(index,elevation);
				cache.put(gridLoc, elevation);
			}
			
			// Remaining results were prefetched.
			while(elevationsIt.hasNext()) {
				assert(currRequestIt.hasNext());
				
				Point gridLoc = currRequestIt.next();
				Double elevation = elevationsIt.next();

				cache.put(gridLoc, elevation);
			}

		}
		
		
		return results;
	}


	/**
	 * Suggests a set of coordinates as likely candidates for future calls,
	 * allowing them to be pre-fetched in the background.
	 * 
	 * Useful for initially filling a new cache.
	 * @param queries
	 */
	public void prefetchNeighbors(List<Coordinate> queries) {
		for(Coordinate query: queries) {
			Set<Point> neighbors = lattice.getNeighbors(query, PREFETCH_RADIUS);
			for(Point neighbor : neighbors) {
				if(!cache.containsKey(neighbor)) {
					// either add neighbor or update recency
					prefetchStack.put(neighbor, null);
				}
			}
		}
	}

	public void setElevationProvider(ElevationProvider provider) {
		if(provider == null) {
			throw new IllegalArgumentException("ElevationProvider may not me null.");
		}
		this.provider = provider;
	}


	public ElevationProvider getElevationProvider() {
		return provider;
	}

}
