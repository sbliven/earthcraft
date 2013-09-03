package us.bliven.bukkit.earthcraft;

import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RenderedOp;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.gce.gtopo30.GTopo30Reader;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import us.bliven.bukkit.earthcraft.gis.DataUnavailableException;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.FlatElevationProvider;
import us.bliven.bukkit.earthcraft.gis.MapProjection;

import com.sun.media.imageioimpl.common.PackageUtil;
import com.sun.media.imageioimpl.plugins.raw.RawImageReaderSpi;
import com.sun.media.jai.imageioimpl.ImageReadWriteSpi;
import com.vividsolutions.jts.geom.Coordinate;

public class EarthcraftPlugin extends JavaPlugin {
	private Logger log;
	private ElevationProvider primaryProvider = null;

	private ConfigManager config;
    @Override
	public void onEnable(){

        //log = this.getLogger();
        log = this.getLogger();//Logger.getLogger("us.bliven.bukkit.earthcraft.EarthcraftPlugin");
        log.setLevel(Level.ALL);

        log.info("Earthcraft enabled.");
        log.info("CLASSPATH="+System.getProperty("java.class.path"));

        initJAI();

        // Create default config file if none exists
        saveDefaultConfig();

        config = new ConfigManager(this);

        logDebugInfo();
    }

    private void logDebugInfo() {
        // Test packaging
//		Class<PackageUtil> klass = PackageUtil.class;//javax.imageio.spi.IIOServiceProvider.class;
		Class<JAI> klass = JAI.class;//javax.imageio.spi.IIOServiceProvider.class;
//		CodeSource src = klass.getProtectionDomain().getCodeSource();
//		if (src != null) {
//			URL jar = src.getLocation();
//			log.info("Class location: "+jar);
//		} else {
//			log.info("No class location.");
//		}

		URL location = klass.getResource('/'+klass.getName().replace('.', '/')+".class");
		log.info("Jar location: "+location);

        log.info("Trying to make RawImageReaderSpi");
        log.info("Vendor: "+PackageUtil.getVendor());
        try {
        new RawImageReaderSpi();
        log.info("Success: RawImageReaderSpi");
        } catch(Exception e) {
        	log.log(Level.INFO,"Error: RawImageReaderSpi.",e);
        }

		try {
			CoordinateReferenceSystem epsg = CRS.decode("EPSG:4326", true);
			System.out.println("EPSG: "+epsg.getClass().getName());
			System.out.println("  "+epsg);
		} catch (Exception e) {
			log.log(Level.INFO,"Got an error constructing a geoCRS.",e);
		}

		try {
		final Hints hints = GeoTools.getDefaultHints();
		hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
		Set<CRSAuthorityFactory> rff = ReferencingFactoryFinder.getCRSAuthorityFactories(hints);

		System.out.println("RFF: "+rff.size());
		for( CRSAuthorityFactory r : rff) {
			System.out.println("  type: "+r.getClass().getName());
			System.out.println("  "+r);
		}
		} catch (Exception e) {
			log.log(Level.INFO,"Got an error fetching RFF.",e);
		}


		try {
			log.info("trying to load a full grid");
			File demFile = new File("/Users/blivens/dev/minecraft/srtm/w140n40.Bathymetry.srtm.dem");
			System.out.println("Exists: "+demFile.exists());
			GTopo30Reader reader = new GTopo30Reader( demFile );
			System.out.println("Remaining coverages: ");
			GridCoverage2D coverage = reader.read(null);
			System.out.println("No error during reading");

		} catch (Exception e) {
			log.log(Level.INFO,"Got an error loading w140n40 grid.",e);
		}

		try {
			RenderedOp image = JAI.create("ImageRead", new ParameterBlock(),
					new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_DEFAULT));
		} catch(Exception e) {
			log.log(Level.INFO,"Got an error reading an image.",e);
		}
    }

    @Override
	public void onDisable(){
    	log.info("Earthcraft disabled.");

    	//If the elevationProvider counts API calls, report them here
    	if(primaryProvider != null) {
    		Class<? extends ElevationProvider> cl = primaryProvider.getClass();

    		try {
    			// Try to report on the number of elevation requests, where appropriate
				Method getRequestsMade = cl.getMethod("getRequestsMade",(Class<?>[])null);
				int requests = (Integer) getRequestsMade.invoke(primaryProvider, (Object[])null);
				System.out.println("Earthcraft made "+requests+" calls to the Elevation API.");
			} catch (SecurityException e) {} //Ignore
			catch (NoSuchMethodException e) {} //Ignore
    		catch (IllegalAccessException e) {}
    		catch (InvocationTargetException e) {}
    	}
    }

    @Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
    	//log.info("Providing Earthcraft as generator for "+worldName);
    	//return new GlowstoneReefGen();

    	// Set up elevation provider

//    	double south = 32.73;
//    	double north = 32.80; // 84 blocks
//    	double west = -117.26;
//    	double east = -117.20; // 72 blocks
//
////		south = 0.0;
////		north = 0.64;
////		west = 0.0;
////		east = 0.64;
//
//    	// Define projection from degrees to blocks
//    	Coordinate scale = new Coordinate(3./60/60, 3./60/60, 10.); // 3"/block
//    	scale = new Coordinate(.001,.001,1);
////    	Coordinate origin = new Coordinate(0.,0.,-40.);
//    	Coordinate origin = new Coordinate(south, west, -40);
//    	MapProjection projection = new EquirectangularProjection(origin,scale);
//
//
////		primaryProvider = new TestElevationProvider(south,north,west,east,0.,64.);
//		primaryProvider = new OpenElevationConnector();
//    	Coordinate gridScale = new Coordinate(4*scale.x,4*scale.y); // One grid every chunk
//    	ElevationProvider provider = new InterpolatingElevationCache(primaryProvider,gridScale);

    	// Load info from config file
    	MapProjection projection = config.getProjection(worldName);
    	ElevationProvider provider = config.getProvider(worldName);
    	Coordinate spawn = config.getSpawn(worldName);

    	log.info("Setting spawn to "+spawn.x+","+spawn.y);

    	// Check that the ElevationProvider is working
    	try {
			provider.fetchElevation(spawn);
		} catch (DataUnavailableException e) {
			log.log(Level.SEVERE, "Unable to load elevation provider!",e);
			provider = new FlatElevationProvider();
		}

    	EarthGen gen = new EarthGen(projection,provider,spawn);

    	/*
    	//Some tests
    	World world = new StubWorld();
    	int[] testPts = new int[] {
    			-2,0,
    			-1,0,
    			0,0,
    			1,0,
    			2,0,
    			-2,1,
    			-1,1,
    			0,1,
    			1,1,
    			2,1,
    	};
    	for(int i=0;i<testPts.length-1;i+=2) {
    		int h = gen.getBlockHeight(world, testPts[i], testPts[i+1]);
    		System.out.println(String.format("Height(%2d,%2d) = %d",testPts[i], testPts[i+1], h));
    	}
    	*/

    	return gen;
    }

    /**
     * Since plugins get loaded late, the GeoTools JAI operators need to be
     * manually initialized.
     */
    protected void initJAI() {
        // http://docs.oracle.com/cd/E17802_01/products/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/OperationRegistry.html
    	OperationRegistry registry = JAI.getDefaultInstance().getOperationRegistry();
    	if( registry == null) {
    		log.warning("Error with JAI initialization (needed for GeoTools).");
    	} else {
    		// Load the two registry files we need

    		// Deserialization throws errors, so (for now), just add the specific registries manually
    		//initJAIFromFile("/META-INF/services/javax.media.jai.OperationRegistrySpi", registry);
    		//initJAIFromFile("/META-INF/registryFile.jai", registry);

    		new ImageReadWriteSpi().updateRegistry(registry);
    	}
    }

    /**
     * Initialize JAI from a registry file
     * @param resource Local path to the registry file relative to the jar
     * @param registry The OperationRegistry to update
     */
    @SuppressWarnings("unused")
	private void initJAIFromFile(String resource, OperationRegistry registry) {
    	InputStream in = EarthcraftPlugin.class.getResourceAsStream(resource);
    	if( in == null) {
    		log.warning("Error with JAI initialization. Unable to find "+resource);
    	} else {
    		try {
    			registry.updateFromStream(in);
    		} catch(IOException e) {
        		log.log(Level.WARNING,"Error with JAI initialization while reading "+resource, e);
    		}
    		try {
				in.close();
			} catch (IOException e) {
        		log.log(Level.WARNING,"Error with JAI initialization while closing "+resource, e);
			}
    	}
    }

	public static void main(String[] a) {
    	System.out.println("CLASSPATH="+System.getProperty("java.class.path"));
    	//Coordinate x = new Coordinate(1.,2.);

    	EarthcraftPlugin plugin = new EarthcraftPlugin();
    	plugin.getDefaultWorldGenerator("foo", "id");
    }
}
