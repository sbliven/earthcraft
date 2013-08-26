package us.bliven.bukkit.earthcraft.gis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.factory.Hints;
import org.geotools.gce.gtopo30.GTopo30FormatFactory;
import org.geotools.gce.gtopo30.GTopo30Reader;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

public class SRTMReader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		URL url = ClassLoader.getSystemResource("w140n40.Bathymetry.srtm.dem");
		File file = new File(url.getFile());
		System.out.println(file.exists());
		
		
	    try {
	    	GTopo30Reader reader = new GTopo30Reader(file);
			GridCoverage2D coverage = reader.read(null);
			
			
			DirectPosition pos = new DirectPosition2D(-117.,32.);
			GridGeometry2D geom = coverage.getGridGeometry();
			
			GridCoordinates2D worldOrigin = geom.worldToGrid(new DirectPosition2D(0.,0.));
			GridCoordinates2D worldScale = geom.worldToGrid(new DirectPosition2D(1.,1.));
			
			DirectPosition gridOrigin = geom.gridToWorld(new GridCoordinates2D(0,0));
			DirectPosition gridScale = geom.gridToWorld(new GridCoordinates2D(1,1));
			
			
			int[] elevations = coverage.evaluate(pos, (int[])null);
			
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidGridGeometryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
