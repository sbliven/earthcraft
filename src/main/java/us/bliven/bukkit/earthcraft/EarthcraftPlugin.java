package us.bliven.bukkit.earthcraft;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;

import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.ElevationProviderStub;
import us.bliven.bukkit.earthcraft.gis.EquirectangularProjection;
import us.bliven.bukkit.earthcraft.gis.InterpolatingElevationCache;
import us.bliven.bukkit.earthcraft.gis.MapProjection;
import us.bliven.bukkit.earthcraft.gis.OpenElevationConnector;
import us.bliven.bukkit.earthcraft.gis.TestElevationProvider;

import com.vividsolutions.jts.geom.Coordinate;

public class EarthcraftPlugin extends JavaPlugin {
	private Logger log;
	private ElevationProvider primaryProvider = null;
	
    @Override
	public void onEnable(){ 
        //log = this.getLogger();
        log = Logger.getAnonymousLogger();

        log.info("Earthcraft enabled.");
        log.info("CLASSPATH="+System.getProperty("java.class.path"));
    }
     
    @Override
	public void onDisable(){ 
    	log.info("Earthcraft disabled.");
    	
    	//If the elevationProvider counts API calls, report them here
    	if(primaryProvider != null) {
    		Class<? extends ElevationProvider> cl = primaryProvider.getClass();
    		
    		try {
				Method getRequestsMade = cl.getMethod("getRequestsMade",(Class<?>[])null);
				int requests = (Integer) getRequestsMade.invoke(primaryProvider, (Object[])null);
				System.out.println("Earthcraft made "+requests+" calls to the Elevation API.");
			} catch (SecurityException e) {} //Ignore
			catch (NoSuchMethodException e) {} //Ignore
			catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    @Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
    	//log.info("Providing Earthcraft as generator for "+worldName);
    	//return new GlowstoneReefGen();
    	
    	// Set up elevation provider
    	double south = 32.73;
    	double north = 32.80; // 84 blocks
    	double west = -117.26;
    	double east = -117.20; // 72 blocks
    	
//		south = 0.0;
//		north = 0.64;
//		west = 0.0;
//		east = 0.64;
    	
    	// Define projection from degrees to blocks
    	Coordinate scale = new Coordinate(3./60/60, 3./60/60, 10.); // 3"/block
    	scale = new Coordinate(.001,.001,1);
//    	Coordinate origin = new Coordinate(0.,0.,-40.);
    	Coordinate origin = new Coordinate(south, west, -40);
    	MapProjection projection = new EquirectangularProjection(origin,scale);

		
//		primaryProvider = new TestElevationProvider(south,north,west,east,0.,64.);
		primaryProvider = new OpenElevationConnector();
    	Coordinate gridScale = new Coordinate(4*scale.x,4*scale.y); // One grid every chunk
    	ElevationProvider provider = new InterpolatingElevationCache(primaryProvider,gridScale);
    	
    	EarthGen gen = new EarthGen(projection,provider);
    	
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
    
    public static void main(String[] a) {
    	System.out.println("CLASSPATH="+System.getProperty("java.class.path"));
    	Coordinate x = new Coordinate(1.,2.);
    	
    	EarthcraftPlugin plugin = new EarthcraftPlugin();
    	plugin.getDefaultWorldGenerator("foo", "id");
    }
}
