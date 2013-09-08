package us.bliven.bukkit.earthcraft.gis;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.gtopo30.GTopo30Reader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;

import us.bliven.bukkit.earthcraft.util.FileCache;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * The Shuttle Radar Topography Mission (STRM) was a Nasa project
 * @author Spencer Bliven
 */
public class SRTMPlusElevationProvider extends GridCoverageElevationProvider {

	//SMTP Plus FTP server
	private static final String SMTP_PLUS_SERVER = "ftp://topex.ucsd.edu/pub/srtm30_plus/srtm30/erm/";

	// directory to store SMTP+ files
	private final FileCache cache;

	public SRTMPlusElevationProvider(String dir) {
		this(dir,true);
	}
	public SRTMPlusElevationProvider(String dir, boolean wrap) {
		super(wrap);

		this.cache = new FileCache(dir,getExecutor());
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
		double lat = bounds.getMaximum(0);
		double lon = bounds.getMinimum(1);
		double grid = 50./6000;

		if(cache.isAvailable(filename)) {
			// Don't replace existing file
			return;
		}

		log.info("Creating "+filename);

		StringBuilder contents = new StringBuilder();
		String eol = System.getProperty("line.separator");
		contents.append("BYTEORDER     M"+eol);
		contents.append("LAYOUT        BIL"+eol);
		contents.append("NROWS         "+nrows+eol);
		contents.append("NCOLS         "+ncols+eol);
		contents.append("NBANDS        1"+eol);
		contents.append("NBITS         16"+eol);
		contents.append("BANDROWBYTES  "+(ncols*2)+eol);
		contents.append("TOTALROWBYTES "+(ncols*2)+eol);
		contents.append("BANDGAPBYTES  0"+eol);
		contents.append("NODATA        -9999"+eol);
		contents.append("ULXMAP        "+(lon+grid/2)+eol);
		contents.append("ULYMAP        "+(lat-grid/2)+eol);
		contents.append("XDIM          "+grid+eol);
		contents.append("YDIM          "+grid+eol);


		InputStream in = new ByteArrayInputStream(contents.toString().getBytes());
		cache.prefetch(filename, in);

	}
	private synchronized void createPRJ(String filename) {
		if(cache.isAvailable(filename)) {
			// Don't replace existing file
			return;
		}

		log.info("Creating "+filename);

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

		log.info("Creating "+filename);

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
	@Override
	protected String getTileName(Coordinate coord) {
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

		//ensure within bounds
		// TODO handle lat==-90
		if( lon == 180.) lon = -180.;
		if(lat <= -90 || lon < -180 || 180 <= lon) {
			throw new IllegalArgumentException("Coordinate out of Bounds. "+coord);
		}

		StringBuilder prefix = new StringBuilder();
		if(lat <= -60) {
			//Antarctic: devided into 60 deg segments
			if(-180 <= lon && lon < -120) {
				prefix.append("w180");
			} else if(-120 <= lon && lon < -60) {
				prefix.append("w120");
			} else if(-60 <= lon && lon < 0) {
				prefix.append("w060");
			} else if(0 <= lon && lon < 60) {
				prefix.append("w000");
			} else if(60 <= lon && lon < 120) {
				prefix.append("e060");
			} else if(120 <= lon && lon < 180) {
				prefix.append("e120");
			} else {
				throw new IllegalArgumentException("Illegal Longitude: "+lon);
			}

			prefix.append("s60");
		} else {
			// 40 deg segments
			if(-180 <= lon && lon < -140) {
				prefix.append("w180");
			} else if(-140 <= lon && lon < -100) {
				prefix.append("w140");
			} else if(-100 <= lon && lon < -60) {
				prefix.append("w100");
			} else if(-60 <= lon && lon < -20) {
				prefix.append("w060");
			} else if(-20 <= lon && lon < 20) {
				prefix.append("w020");
			} else if(20 <= lon && lon < 60) {
				prefix.append("e020");
			} else if(60 <= lon && lon < 100) {
				prefix.append("e060");
			} else if(100 <= lon && lon < 140) {
				prefix.append("e100");
			} else if(140 <= lon && lon < 180) {
				prefix.append("e140");
			} else {
				throw new IllegalArgumentException("Illegal Longitude: "+lon);
			}

			if(-60 < lat && lat <= -10) {
				prefix.append("s10");
			} else if(-10 < lat && lat <= 40) {
				prefix.append("n40");
			} else if(40 < lat && lat <= 90) {
				prefix.append("n90");
			} else {
				throw new IllegalArgumentException("Illegal Latitude: "+lat);
			}
		}

		return prefix.toString();
	}

	/**
	 * Get the bounding envelope for the tile around a given coordinate
	 * @param coord
	 * @return
	 */
	protected Envelope getTileEnvelope(Coordinate coord) {
		double lat = coord.x;
		double lon = coord.y;

		//ensure within bounds
		// TODO handle lat==-90
		if( lon == 180.) lon = -180.;
		if(lat <= -90 || lon < -180 || 180 <= lon) {
			throw new IllegalArgumentException("Coordinate out of Bounds. "+coord);
		}
		double left,right,upper,lower;
		if(lat <= -60) {
			//Antarctic: devided into 60 deg segments
			if(-180 <= lon && lon < -120) {
				left = -180;
				right = -120;
			} else if(-120 <= lon && lon < -60) {
				left = -120;
				right = -60;
			} else if(-60 <= lon && lon < 0) {
				left = -60;
				right = 0;
			} else if(0 <= lon && lon < 60) {
				left = 0;
				right = 60;
			} else if(60 <= lon && lon < 120) {
				left = 60;
				right = 120;
			} else if(120 <= lon && lon < 180) {
				left = 120;
				right = 180;
			} else {
				throw new IllegalArgumentException("Illegal Longitude: "+lon);
			}

			lower = -90;
			upper = -60;
		} else {
			// 40 deg segments
			if(-180 <= lon && lon < -140) {
				left = -180;
				right = -140;
			} else if(-140 <= lon && lon < -100) {
				left = -140;
				right = -100;
			} else if(-100 <= lon && lon < -60) {
				left = -100;
				right = -60;
			} else if(-60 <= lon && lon < -20) {
				left = -60;
				right = -20;
			} else if(-20 <= lon && lon < 20) {
				left = -20;
				right = 20;
			} else if(20 <= lon && lon < 60) {
				left = 20;
				right = 60;
			} else if(60 <= lon && lon < 100) {
				left = 60;
				right = 100;
			} else if(100 <= lon && lon < 140) {
				left = 100;
				right = 140;
			} else if(140 <= lon && lon < 180) {
				left = 140;
				right = 180;
			} else {
				throw new IllegalArgumentException("Illegal Longitude: "+lon);
			}

			if(-60 < lat && lat <= -10) {
				lower = -60;
				upper = -10;
			} else if(-10 < lat && lat <= 40) {
				lower = -10;
				upper = 40;
			} else if(40 < lat && lat <= 90) {
				lower = 40;
				upper = 90;
			} else {
				throw new IllegalArgumentException("Illegal Latitude: "+lat);
			}
		}
		Envelope2D env = new Envelope2D(new DirectPosition2D(upper,left),new DirectPosition2D(lower,right));
		return env;
	}

	@Override
	protected Callable<GridCoverage> createTileLoader(Coordinate coord) {
		// Get the tile prefix, eg 'w140n40'
		final String tile = getTileName(coord);
		final Envelope tileBounds = getTileEnvelope(coord);

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
			throw new RuntimeException("Error: bad URL for downloading tile "+tile,e);
		}

		return new GridLoader(tile, fileBase);
	}

	private final class GridLoader implements Callable<GridCoverage> {
		private final String tile;
		private final String fileBase;

		private GridLoader(String tile, String fileBase) {
			this.tile = tile;
			this.fileBase = fileBase;
		}

		@Override
		public GridCoverage call() throws Exception {

			// fetch all the files synchronously
			cache.fetch(fileBase+".hdr");
			cache.fetch(fileBase+".prj");
			cache.fetch(fileBase+".stx");
			cache.fetch(fileBase+".ers");
			cache.fetch(fileBase+".dem");

			long start = System.currentTimeMillis();

			File demFile = new File(cache.getDir(),fileBase+".dem");

			// Load the grid into memory
			GTopo30Reader reader = new GTopo30Reader( demFile );
			GridCoverage2D coverage = reader.read(null);

			log.info(String.format("Loaded grid %s. Took %f s%n",
					tile, (System.currentTimeMillis()-start)/1000.));

			// Dummy result object
			return coverage;
		}
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
			GridCoverage2D grid = (GridCoverage2D)srtm.loadGrid(new Coordinate(34,-117));
			GridGeometry2D geom = grid.getGridGeometry();
			try {
				DirectPosition origin = geom.gridToWorld(new GridCoordinates2D(0, 0));
				DirectPosition one = geom.gridToWorld(new GridCoordinates2D(1,1));
				//DirectPosition x6 = geom.gridToWorld(new GridCoordinates2D(6000-1,0));
				DirectPosition x4 = geom.gridToWorld(new GridCoordinates2D(4800-1,0));
				DirectPosition y6 = geom.gridToWorld(new GridCoordinates2D(0,6000-1));
				DirectPosition y4 = geom.gridToWorld(new GridCoordinates2D(0,4800-1));
				GridCoordinates2D worigin = geom.worldToGrid(new DirectPosition2D(0., 0.));
				GridCoordinates2D wone = geom.worldToGrid(new DirectPosition2D(1., 1.));
				GridCoordinates2D sd = geom.worldToGrid(new DirectPosition2D(-117.,34));
				GridCoordinates2D pac = geom.worldToGrid(new DirectPosition2D(-140,40));
				GridCoordinates2D pac1 = geom.worldToGrid(new DirectPosition2D(-140+0.00833333333333,40-0.00833333333333));
				GridCoordinates2D pac2 = geom.worldToGrid(new DirectPosition2D(-140+0.00833333333333/2,40-0.00833333333333/2));
				int i = 0;
			} catch (TransformException e) {
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
			e.printStackTrace();
		}
		srtm = null;
		System.out.println("Done");
		System.gc();
	}
}
