package us.bliven.bukkit.earthcraft;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import us.bliven.bukkit.earthcraft.gis.DataUnavailableException;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.FlatElevationProvider;
import us.bliven.bukkit.earthcraft.gis.MapProjection;
import us.bliven.bukkit.earthcraft.gis.ProjectionTools;

import com.sun.media.jai.imageioimpl.ImageReadWriteSpi;
import com.vividsolutions.jts.geom.Coordinate;

public class EarthcraftPlugin extends JavaPlugin {
	private Logger log = null;
	private ConfigManager config = null;

	// Create a new EarthGen for each world to allow configurability
	private final Map<String,EarthGen> generators = new HashMap<>();

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
    }

    @Override
	public synchronized ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
    	if(generators.containsKey(worldName)) {
    		return generators.get(worldName);
    	}

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

    	Location spawnLoc = gen.getFixedSpawnLocation(null, null);
    	log.info("Spawn is at block "+spawnLoc);

    	generators.put(worldName,gen);

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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
    	String name = cmd.getName();
    	if(name.equalsIgnoreCase("earth")) {
    		if( args.length<1 )
    			//no subcommand specified
    			return false;
    		String subcmd = args[0];
    		String[] subargs = Arrays.copyOfRange(args, 1, args.length);
    		if( subcmd.equalsIgnoreCase("pos")) {
    			return onPosCommand(sender,subargs);
    		} else if(subcmd.equalsIgnoreCase("tp")) {
    			return onTPCommand(sender,subargs);
    		}
    	} else if(name.equalsIgnoreCase("earthpos")){
			return onPosCommand(sender,args);
    	} else if(name.equalsIgnoreCase("earthtp")){
			return onTPCommand(sender,args);
    	}

    	return false;
    }


	private boolean onTPCommand(CommandSender sender, String[] args) {
		//todo check permissions
		log.info(String.format("%s ectp! %s",sender.getName(),Arrays.toString(args)));
		return true;
	}


	private boolean onPosCommand(CommandSender sender, String[] args) {
		if(args.length > 1) {
			return false;
		}
		Player player;
		if( args.length == 1) {
			// Send position of specified player
			String playername = args[0];
			player = Bukkit.getPlayer(playername);
			if( !player.isOnline() ) {
				sender.sendMessage("Error: "+playername+" is offline");
				return true;
			}

		} else {
			// Send position of current player
			if( sender instanceof Player ) {
				player = (Player) sender;
			} else {
				sender.sendMessage("Error: Player required from console");
				return false;
			}
		}
		String world = player.getWorld().getName();
		EarthGen gen = generators.get(world);
		if( gen == null) {
			sender.sendMessage("Player "+player.getName()+" not in an Earthcraft world.");
			return false;
		}

		Location loc = player.getLocation();
		MapProjection proj = gen.getMapProjection();

		Coordinate coord = proj.locationToCoordinate(loc);

		String message = String.format("%s located at %s", player.getName(),
				ProjectionTools.latlonString(coord));
		sender.sendMessage(message);
		return true;
	}


	public static void main(String[] a) {
    	System.out.println("CLASSPATH="+System.getProperty("java.class.path"));
    	//Coordinate x = new Coordinate(1.,2.);

    	EarthcraftPlugin plugin = new EarthcraftPlugin();
    	plugin.getDefaultWorldGenerator("foo", "id");
    }
}
