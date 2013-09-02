package us.bliven.bukkit.earthcraft.gis;

import static org.junit.Assert.assertNotNull;

import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.Set;

import javax.imageio.spi.IIOServiceProvider;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.gce.gtopo30.GTopo30Reader;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.junit.Test;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.sun.media.imageioimpl.common.PackageUtil;
import com.sun.media.imageioimpl.plugins.raw.RawImageReaderSpi;


/**
 * This test was created to replicate a 'vendor == null' exception thrown while instantiating
 * a RawImageReaderSpi
 *
 * It was working fine from eclipse, but not from the jar.
 * @author Spencer Bliven
 */
public class TestSpiVendor {

	@Test
	public void testCreation() {
		System.out.println("Trying to make RawImageReaderSpi");
		System.out.println("Vendor: "+PackageUtil.getVendor());
		System.out.println("Version: "+PackageUtil.getVersion());

		assertNotNull(new RawImageReaderSpi());
		System.out.println("Success: RawImageReaderSpi");
	}

	@Test
	public void testJarLocation() {
		//http://stackoverflow.com/questions/1983839/determine-which-jar-file-a-class-is-from

		Class<IIOServiceProvider> klass = javax.imageio.spi.IIOServiceProvider.class;
		CodeSource src = klass.getProtectionDomain().getCodeSource();
		if (src != null) {
			URL jar = src.getLocation();
			System.out.println("Class location: "+jar);
		} else {
			System.out.println("No class location.");
		}

		URL location = klass.getResource('/'+klass.getName().replace('.', '/')+".class");
		System.out.println("Jar location: "+location);
	}

	@Test
	public void testCRS() throws Exception{
		CoordinateReferenceSystem epsg = CRS.decode("EPSG:4326", true);
		System.out.println("EPSG: "+epsg.getClass().getName());
		System.out.println("  "+epsg);


		final Hints hints = GeoTools.getDefaultHints();
		hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
		Set<CRSAuthorityFactory> rff = ReferencingFactoryFinder.getCRSAuthorityFactories(hints);

		System.out.println("RFF: "+rff.size());
		for( CRSAuthorityFactory r : rff) {
			System.out.println("  type: "+r.getClass().getName());
			System.out.println("  "+r);
		}
	}

	@Test
	public void testGrid() throws Exception{
		try {
			RenderedOp image = JAI.create("ImageRead", new ParameterBlock(), new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_DEFAULT));
		} catch(NullPointerException e) {}
		catch(IllegalArgumentException e) {
			e.printStackTrace();
		}

		File demFile = new File("/Users/blivens/dev/minecraft/srtm/w140n40.Bathymetry.srtm.dem");
		System.out.println("Exists: "+demFile.exists());
		GTopo30Reader reader = new GTopo30Reader( demFile );
		System.out.println("Remaining coverages: ");
		GridCoverage2D coverage = reader.read(null);
		System.out.println("No error during reading");
	}
}
