package us.bliven.bukkit.earthcraft.gis;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.gtopo30.GTopo30Reader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.util.LRULinkedHashMap;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;

import us.bliven.bukkit.earthcraft.util.FileCache;

import com.vividsolutions.jts.geom.Coordinate;

public class SRTMPlusElevationProvider implements ElevationProvider {
	//SMTP Plus FTP server
	private static final String SMTP_PLUS_SERVER = "ftp://topex.ucsd.edu/pub/srtm30_plus/srtm30/erm/";
	//Number of tiles to store in memory simultaneously
	private static final int GRID_CACHE_SIZE = 32; // Decrease to reduce memory use

	// directory to store SMTP+ files
	private final FileCache cache;
	// LRU set of Grids storing the SMTP tiles (partially) in memory
	private final LRULinkedHashMap<String,GridCoverage2D> grids;//a tile identifier -> Grid

	private final ExecutorService executor;
	private Map<String,Future<?>> currentGrids; // List of grids being loaded from memory
	
	public SRTMPlusElevationProvider(String dir) {
		this.executor = Executors.newFixedThreadPool(2);

		this.cache = new FileCache(dir,executor);

		this.grids = new LRULinkedHashMap<String,GridCoverage2D>(GRID_CACHE_SIZE);
		this.currentGrids = new HashMap<String,Future<?>>();
	}
	

	
	public GridCoverage2D loadGrid(Coordinate coord) throws DataUnavailableException {		
		// Get the tile prefix, eg 'w140n40'
		String tile = getSRTMPlusTile(coord);

		// check if the grid is already cached
		if(grids.containsKey(tile)) {
			return grids.get(tile);
		}

		// Start asynchronous download, if needed
		System.out.println("prefetch");
		prefetchGrid(coord);
		System.out.println("waiting on prefetch of "+tile);
		Future<?> result = currentGrids.get(tile); 

		try {
			// Wait for the download to finish & tile to load
			result.get();
		} catch (Exception e) {
			throw new DataUnavailableException("Unable to load grid "+tile,e);
		}

		System.out.println("finished getting "+tile);
		return grids.get(tile);
	}


	/**
	 * Shut down background downloads
	 */
	@Override
	protected void finalize() {
		System.out.println("Finalizing SRTM");
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
		final String tile = getSRTMPlusTile(coord);
		final Envelope tileBounds = getSRTMPlusEnvelope(coord);

		// check if the grid is already cached
		if( isAvailable(tile) ) {
			return true;
		}
		// already being loaded
		if(  currentGrids.containsKey(tile) ) {
			return false;
		}
		
		// file to fetch from ftp
		final String fileBase = tile + ".Bathymetry.srtm";
		
		// Download the data files asynchronously
		// Should take about 40s to download dem file
		try {
			cache.prefetch(fileBase+".dem", new URL(SMTP_PLUS_SERVER+fileBase) );
			cache.prefetch(fileBase+".ers", new URL(SMTP_PLUS_SERVER+fileBase+".ers") );

			// Create bogus GTopo30 files for the inflexible Reader
			createHDR(fileBase+".hdr",tileBounds);
			createPRJ(fileBase+".prj");
			createSTX(fileBase+".stx");		
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error–bad URL for downloading tile "+tile,e);
		}
		
		// Create thread to load Grid
		Future<?> result = executor.submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				long start = System.currentTimeMillis();
				
				// fetch all the files synchronously
				cache.fetch(fileBase+".hdr");
				cache.fetch(fileBase+".prj");
				cache.fetch(fileBase+".stx");		
				cache.fetch(fileBase+".ers");		
				cache.fetch(fileBase+".dem");
				
				// Load the grid into memory
		    	GTopo30Reader reader = new GTopo30Reader(new File(cache.getDir(),fileBase+".dem").toString() );
		    	assert(reader.hasMoreGridCoverages());
				GridCoverage2D coverage = reader.read(null);

				grids.put(tile,coverage);
				
				System.out.format("Loading grid %s took %f s%n", tile, (System.currentTimeMillis()-start)/1000.);
				
				// Dummy result object
				return null;
			}
			
		});
		
		currentGrids.put(tile,result);
		return false;
	}

	private synchronized void createHDR(String filename,Envelope bounds) {
		/*
		BYTEORDER     M
		LAYOUT        BIL
		NROWS         6000
		NCOLS         4800
		NBANDS        1
		NBITS         16
		BANDROWBYTES         9600
		TOTALROWBYTES        9600
		BANDGAPBYTES         0
		NODATA        -9999
		ULXMAP        -140.0
		ULYMAP        40.0
		XDIM          0.00833333333333
		YDIM          0.00833333333333
		*/
		//TODO Wrong for Antarctica
		int nrows = 6000;
		int ncols = 4800;
		double lat = bounds.getMinimum(0);
		double lon = bounds.getMaximum(1);
		
		if(cache.isAvailable(filename)) {
			// Don't replace existing file
			return;
		}
		
		System.out.println("Creating "+filename);
		
		StringBuilder contents = new StringBuilder();
		String eol = System.getProperty("line.separator");
		contents.append("BYTEORDER     M"+eol);
		contents.append("LAYOUT        BIL"+eol);
		contents.append("NROWS         "+nrows+eol);
		contents.append("NCOLS         "+ncols+eol);
		contents.append("NBANDS        1"+eol);
		contents.append("NBITS         16"+eol);
		contents.append("BANDROWBYTES  "+(ncols*16)+eol);
		contents.append("TOTALROWBYTES "+(ncols*16)+eol);
		contents.append("BANDGAPBYTES  0"+eol);
		contents.append("NODATA        -9999"+eol);
		contents.append("ULXMAP        "+lat+eol);
		contents.append("ULYMAP        "+lon+eol);
		contents.append("XDIM          0.00833333333333"+eol);
		contents.append("YDIM          0.00833333333333"+eol);
		
		
		InputStream in = new ByteArrayInputStream(contents.toString().getBytes());
		cache.prefetch(filename, in);
		
	}
	private synchronized void createPRJ(String filename) {
		if(cache.isAvailable(filename)) {
			// Don't replace existing file
			return;
		}
		
		System.out.println("Creating "+filename);
		
		StringBuilder contents = new StringBuilder();
		String eol = System.getProperty("line.separator");
		contents.append("Projection    GEOGRAPHIC"+eol);
		contents.append("Datum         WGS84"+eol);
		contents.append("Zunits        METERS"+eol);
		contents.append("Units         DD"+eol);
		contents.append("Spheroid      WGS84"+eol);
		contents.append("Xshift        0.0000000000"+eol);
		contents.append("Yshift        0.0000000000"+eol);
		contents.append("Parameters    "+eol);

		InputStream in = new ByteArrayInputStream(contents.toString().getBytes());
		cache.prefetch(filename, in);
	}
	private synchronized void createSTX(String filename) {
		if(cache.isAvailable(filename)) {
			// Don't replace existing file
			return;
		}
		
		System.out.println("Creating "+filename);
		
		StringBuilder contents = new StringBuilder();
		String eol = System.getProperty("line.separator");
		// band min max avg sd
		contents.append("1 -9999 9999 0 100"+eol);


		InputStream in = new ByteArrayInputStream(contents.toString().getBytes());
		cache.prefetch(filename, in);
	}

	
	/**
	 * Get the tile name for a given coordinate
	 * @param coord
	 * @return
	 */
	private String getSRTMPlusTile(Coordinate coord) {
		/* 
		 *              Latitude          Longitude     
			 Tile    Minimum  Maximum   Minimum  Maximum 
			-------  ----------------   ---------------- 
			
			w180n90     40       90       -180    -140   
			w140n90     40       90       -140    -100   
			w100n90     40       90       -100     -60   
			w060n90     40       90        -60     -20   
			w020n90     40       90        -20      20   
			e020n90     40       90         20      60   
			e060n90     40       90         60     100   
			e100n90     40       90        100     140   
			e140n90     40       90        140     180   
			
			w180n40    -10       40       -180    -140   
			w140n40    -10       40       -140    -100   
			w100n40    -10       40       -100     -60   
			w060n40    -10       40        -60     -20   
			w020n40    -10       40        -20      20   
			e020n40    -10       40         20      60   
			e060n40    -10       40         60     100   
			e100n40    -10       40        100     140   
			e140n40    -10       40        140     180   
			
			w180s10    -60      -10       -180    -140   
			w140s10    -60      -10       -140    -100   
			w100s10    -60      -10       -100     -60   
			w060s10    -60      -10        -60     -20   
			w020s10    -60      -10        -20      20   
			e020s10    -60      -10         20      60   
			e060s10    -60      -10         60     100   
			e100s10    -60      -10        100     140   
			e140s10    -60      -10        140     180 
			  
			w180s60    -90      -60       -180    -120   
			w120s60    -90      -60       -120     -60   
			w060s60    -90      -60        -60       0   
			w000s60    -90      -60          0      60   
			e060s60    -90      -60         60     120   
			e120s60    -90      -60        120     180 
		 */
		double lat = coord.x;
		double lon = coord.y;
		//TODO ensure within bounds
		
		StringBuilder prefix = new StringBuilder();
		if(lon < -60) {
			//Antarctic: devided into 60 deg segments
			if(-180 <= lat && lat < -120) {
				prefix.append("w180");
			} else if(-120 <= lat && lat < -60) {
				prefix.append("w120");
			} else if(-60 <= lat && lat < 0) {
				prefix.append("w060");
			} else if(0 <= lat && lat < 60) {
				prefix.append("w000");
			} else if(60 <= lat && lat < 120) {
				prefix.append("e060");
			} else if(120 <= lat && lat <= 180) {
				prefix.append("e120");
			} else {
				throw new IllegalArgumentException("Illegal Latitude: "+lat);
			}
			
			prefix.append("s60");
		} else {
			// 40 deg segments
			if(-180 <= lat && lat < -140) {
				prefix.append("w180");
			} else if(-140 <= lat && lat < -100) {
				prefix.append("w140");
			} else if(-100 <= lat && lat < -60) {
				prefix.append("w100");
			} else if(-60 <= lat && lat < -20) {
				prefix.append("w060");
			} else if(-20 <= lat && lat < 20) {
				prefix.append("w020");
			} else if(20 <= lat && lat < 60) {
				prefix.append("e020");
			} else if(60 <= lat && lat < 100) {
				prefix.append("e060");
			} else if(100 <= lat && lat <= 140) {
				prefix.append("e100");
			} else if(140 <= lat && lat <= 180) {
				prefix.append("e140");
			} else {
				throw new IllegalArgumentException("Illegal Latitude: "+lat);
			}
			
			if(-60 <= lon && lon < -10) {
				prefix.append("s10");
			} else if(-10 <= lon && lon < 40) {
				prefix.append("n40");
			} else if(40 <= lon && lon <= 90) {
				prefix.append("n90");
			} else {
				throw new IllegalArgumentException("Illegal Longitude: "+lat);
			}
		}
		
		return prefix.toString();
	}
	
	/**
	 * Get the bounding envelope for the tile around a given coordinate
	 * @param coord
	 * @return
	 */
	private Envelope getSRTMPlusEnvelope(Coordinate coord) {
		/*
		 *              Latitude          Longitude     
			 Tile    Minimum  Maximum   Minimum  Maximum 
			-------  ----------------   ---------------- 
			
			w180n90     40       90       -180    -140   
			w140n90     40       90       -140    -100   
			w100n90     40       90       -100     -60   
			w060n90     40       90        -60     -20   
			w020n90     40       90        -20      20   
			e020n90     40       90         20      60   
			e060n90     40       90         60     100   
			e100n90     40       90        100     140   
			e140n90     40       90        140     180   
			
			w180n40    -10       40       -180    -140   
			w140n40    -10       40       -140    -100   
			w100n40    -10       40       -100     -60   
			w060n40    -10       40        -60     -20   
			w020n40    -10       40        -20      20   
			e020n40    -10       40         20      60   
			e060n40    -10       40         60     100   
			e100n40    -10       40        100     140   
			e140n40    -10       40        140     180   
			
			w180s10    -60      -10       -180    -140   
			w140s10    -60      -10       -140    -100   
			w100s10    -60      -10       -100     -60   
			w060s10    -60      -10        -60     -20   
			w020s10    -60      -10        -20      20   
			e020s10    -60      -10         20      60   
			e060s10    -60      -10         60     100   
			e100s10    -60      -10        100     140   
			e140s10    -60      -10        140     180 
			  
			w180s60    -90      -60       -180    -120   
			w120s60    -90      -60       -120     -60   
			w060s60    -90      -60        -60       0   
			w000s60    -90      -60          0      60   
			e060s60    -90      -60         60     120   
			e120s60    -90      -60        120     180 
		 */
		double lat = coord.x;
		double lon = coord.y;
		//TODO ensure within bounds
				
		double left,right,upper,lower;
		if(lon < -60) {
			//Antarctic: devided into 60 deg segments
			if(-180 <= lat && lat < -120) {
				left = -180;
				right = -120;
			} else if(-120 <= lat && lat < -60) {
				left = -120;
				right = -60;
			} else if(-60 <= lat && lat < 0) {
				left = -60;
				right = 0;
			} else if(0 <= lat && lat < 60) {
				left = 0;
				right = 60;
			} else if(60 <= lat && lat < 120) {
				left = 60;
				right = 120;
			} else if(120 <= lat && lat <= 180) {
				left = 120;
				right = 180;
			} else {
				throw new IllegalArgumentException("Illegal Latitude: "+lat);
			}
			
			lower = -90;
			upper = -60;
		} else {
			// 40 deg segments
			if(-180 <= lat && lat < -140) {
				left = -180;
				right = -140;
			} else if(-140 <= lat && lat < -100) {
				left = -140;
				right = -100;
			} else if(-100 <= lat && lat < -60) {
				left = -100;
				right = -60;
			} else if(-60 <= lat && lat < -20) {
				left = -60;
				right = -20;
			} else if(-20 <= lat && lat < 20) {
				left = -20;
				right = 20;
			} else if(20 <= lat && lat < 60) {
				left = 20;
				right = 60;
			} else if(60 <= lat && lat < 100) {
				left = 60;
				right = 100;
			} else if(100 <= lat && lat <= 140) {
				left = 100;
				right = 140;
			} else if(140 <= lat && lat <= 180) {
				left = 140;
				right = 180;
			} else {
				throw new IllegalArgumentException("Illegal Latitude: "+lat);
			}
			
			if(-60 <= lon && lon < -10) {
				lower = -60;
				upper = -10;
			} else if(-10 <= lon && lon < 40) {
				lower = -10;
				upper = 40;
			} else if(40 <= lon && lon <= 90) {
				lower = 40;
				upper = 90;
			} else {
				throw new IllegalArgumentException("Illegal Longitude: "+lat);
			}
		}
		Envelope2D env = new Envelope2D(new DirectPosition2D(left, upper),new DirectPosition2D(right,lower));
		return env;
	}
	/**
	 * A list of coordinates giving (lat,lon) pairs. Note that latitude 
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
	public List<Double> fetchElevations(List<Coordinate> l)
			throws DataUnavailableException {
		List<Double> coords = new ArrayList<Double>(l.size());
		for(Coordinate coord : l) {
			coords.add(fetchElevations(coord));
		}
		return coords;
	}
	
	public Double fetchElevations(Coordinate point) throws DataUnavailableException {
		GridCoverage2D grid = loadGrid(point);
		DirectPosition pos = new DirectPosition2D(point.x,point.y);
		double elev = grid.evaluate(pos,(double[])null)[0];
		return elev;
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		// Try to use my dev dir, or default to temp
		String dir = "/Users/blivens/dev/minecraft/srtm";
		if(! (new File(dir)).exists()) {
			dir = System.getProperty("java.io.tmpdir") + "SRTMPlus";
		}
		
		SRTMPlusElevationProvider srtm = new SRTMPlusElevationProvider(dir);
		try {
			// Create base directory if it doesn't exist
			File dirFile = new File(dir);
			if(!dirFile.exists()) {
				dirFile.mkdir();
			}
			
			// load the SD grid
			GridCoverage2D grid = srtm.loadGrid(new Coordinate(-117,34));
			GridGeometry2D geom = grid.getGridGeometry();
			try {
				DirectPosition origin = geom.gridToWorld(new GridCoordinates2D(0, 0));
				DirectPosition one = geom.gridToWorld(new GridCoordinates2D(1,1));
				GridCoordinates2D worigin = geom.worldToGrid(new DirectPosition2D(0., 0.));
				GridCoordinates2D wone = geom.worldToGrid(new DirectPosition2D(1., 1.));
				GridCoordinates2D sd = geom.worldToGrid(new DirectPosition2D(-117.,34.));
				GridCoordinates2D pac = geom.worldToGrid(new DirectPosition2D(-140,40));
				GridCoordinates2D pac1 = geom.worldToGrid(new DirectPosition2D(-140+0.008333333333329999,40-0.008333333333329999));
				GridCoordinates2D pac2 = geom.worldToGrid(new DirectPosition2D(-140+0.008333333333329999/2,40-0.008333333333329999/2));
				int i = 0;
			} catch (TransformException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			int elev = grid.evaluate((DirectPosition)new DirectPosition2D(-117,34), (int[]) null)[0];
			int elev2 = grid.evaluate((DirectPosition)new DirectPosition2D(-140,40), (int[]) null)[0];
			int elev3 = grid.evaluate((DirectPosition)new DirectPosition2D(-140+0.008333333333329999,40-0.008333333333329999), (int[]) null)[0];
			int elev4 = grid.evaluate((DirectPosition)new DirectPosition2D(-140,40-0.008333333333329999), (int[]) null)[0];
			int elev5 = grid.evaluate((DirectPosition)new DirectPosition2D(-140+0.008333333333329999,40), (int[]) null)[0];
			int elev6 = grid.evaluate((DirectPosition)new DirectPosition2D(-140+0.008333333333329999/2,40-0.008333333333329999/2), (int[]) null)[0];
			int elev7 = grid.evaluate((DirectPosition)new DirectPosition2D(-140+0.001,40-0.008333333333329999/2), (int[]) null)[0];
			int elev8 = grid.evaluate((DirectPosition)new DirectPosition2D(-140+0.008333333333329999,40-0.008333333333329999/2), (int[]) null)[0];
			int elev9 = grid.evaluate((DirectPosition)new DirectPosition2D(-140+0.008333/2,40-0.008333/2), (int[]) null)[0];
			int i=0;
		} catch (DataUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		srtm = null;
		System.out.println("Done");
		System.gc();
	}
}
