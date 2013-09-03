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
    	// Set up elevation provider

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
