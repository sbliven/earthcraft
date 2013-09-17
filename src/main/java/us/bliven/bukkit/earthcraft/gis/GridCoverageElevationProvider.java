package us.bliven.bukkit.earthcraft.gis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.util.LRULinkedHashMap;
import org.opengis.geometry.DirectPosition;

import us.bliven.bukkit.earthcraft.ConfigManager;
import us.bliven.bukkit.earthcraft.Configurable;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * The Shuttle Radar Topography Mission (STRM) was a Nasa project
 * @author Spencer Bliven
 */
public abstract class GridCoverageElevationProvider extends AbstractElevationProvider
implements Configurable {

	//Number of tiles to store in memory simultaneously
	private static final int GRID_CACHE_SIZE = 32; // Decrease to reduce memory use
	private static final int THREADS = 2;

	// LRU set of Grids storing the SMTP tiles (partially) in memory
	private final LRULinkedHashMap<String,GridCoverage2D> grids;//a tile identifier -> Grid

	private boolean wrap;

	protected Logger log;

	private final ExecutorService executor;
	private final Map<String,Future<GridCoverage2D>> currentGrids; // List of grids being loaded from memory

	protected GridCoverageElevationProvider() {
		this(true);
	}
	protected GridCoverageElevationProvider(boolean wrap) {
		this.executor = Executors.newFixedThreadPool(THREADS);

		this.grids = new LRULinkedHashMap<String,GridCoverage2D>(GRID_CACHE_SIZE);
		this.currentGrids = new HashMap<String,Future<GridCoverage2D>>();

		this.wrap = wrap;

		this.log = Logger.getLogger(this.getClass().getName());
	}

	/**
	 * Handles the 'wrap' parameter.
	 * Unrecognized parameters are ignored.
	 */
	@Override
	public void initFromConfig(ConfigManager config, ConfigurationSection params) {
		for(String param : params.getKeys(false)) {
			if( param.equalsIgnoreCase("wrap") ) {
				wrap = params.getBoolean(param,wrap);
			}
		}
	}

	public synchronized GridCoverage2D loadGrid(Coordinate coord) throws DataUnavailableException {
		// Get the tile prefix, eg 'w140n40'
		String tile = getTileName(coord);

		// check if the grid is already cached
		if(grids.containsKey(tile)) {
			return grids.get(tile);
		}

		// Start asynchronous download, if needed
		prefetchGrid(coord);
		Future<GridCoverage2D> result = currentGrids.get(tile);

		try {
			// Wait for the download to finish & tile to load
			GridCoverage2D coverage = result.get();

			grids.put(tile,coverage);
		} catch (Exception e) {
			throw new DataUnavailableException("Unable to load grid "+tile,e);
		}

		return grids.get(tile);
	}


	/**
	 * Shut down background downloads
	 */
	@Override
	protected void finalize() {
		executor.shutdownNow(); // kill waiting thread loads
	}


	/**
	 * Helper function for managing the thread pool
	 *
	 * @param tile
	 * @return true if the tile is fully loaded, false if the tile is unloaded or in progress
	 */
	private synchronized boolean isAvailable(String tile) {
		boolean avail = !currentGrids.containsKey(tile) || currentGrids.get(tile).isDone();
		return avail && grids.containsKey(tile);
	}

	/**
	 * Pre-fetch files describing a grid tile
	 * @param coord A coordinate in the tile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public synchronized boolean prefetchGrid(Coordinate coord) throws DataUnavailableException {

		// Get the tile prefix, eg 'w140n40'
		final String tile = getTileName(coord);

		// check if the grid is already cached
		if( isAvailable(tile) ) {
			return true;
		}
		// already being loaded
		if(  currentGrids.containsKey(tile) ) {
			return false;
		}

		// Create thread to load Grid
		Future<GridCoverage2D> result = executor.submit( createTileLoader(coord) );

		currentGrids.put(tile,result);
		return false;
	}


	/**
	 * Get the tile name for a given coordinate
	 * @param coord
	 * @return
	 */
	protected abstract String getTileName(Coordinate coord);

	protected abstract Callable<GridCoverage2D> createTileLoader(Coordinate coord);

	/**
	 * Gets elevation for a coordinate giving (lat,lon). Note that latitude
	 * corresponds to the 'x' member of each coordinate, despite any unfortunate
	 * clash with the use of x for horizontal cartesian coordinates.
	 * @param l
	 * @return
	 * @throws DataUnavailableException If a non-recoverable error stops the data
	 *  from being accessed. Less serious errors simply result in null elevations
	 *  being returned.
	 * @throws
	 */
	@Override
	public Double fetchElevation(Coordinate point) throws DataUnavailableException {
		if( wrap ) {
			// Convert coordinates to valid lat=(-90,90], lon=[-180,180)
			point = ProjectionTools.wrapCoordinate(point);
		} else {
			if( point.x <= -90 || 90 < point.x ||
					point.y < -180 || 180 <= point.y ) {
				// Coordinates off the map
				return null;
			}
		}
		GridCoverage2D grid = loadGrid(point);
		// Change from (lat,lon) convention to (x,y)
		DirectPosition pos = new DirectPosition2D(point.y,point.x);
		double elev = grid.evaluate(pos,(double[])null)[0];
		return elev;
	}
	public boolean isWrap() {
		return wrap;
	}
	public ExecutorService getExecutor() {
		return executor;
	}

}
