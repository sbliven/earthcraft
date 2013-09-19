package us.bliven.bukkit.earthcraft;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import us.bliven.bukkit.earthcraft.gis.DataUnavailableException;
import us.bliven.bukkit.earthcraft.gis.ElevationProjection;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.MapProjection;
import us.bliven.bukkit.earthcraft.gis.ProjectionTools;

import com.sun.media.jai.imageioimpl.ImageReadWriteSpi;
import com.vividsolutions.jts.geom.Coordinate;

public class EarthcraftPlugin extends JavaPlugin {
	private Logger log = null;
	private ConfigManager config = null;
	private Map<String,Coordinate> landmarks = null;

	// Create a new EarthGen for each world to allow configurability
	private final Map<String,EarthGen> generators = new HashMap<String, EarthGen>();

	// Permissions
	static final String PERM_TP_OTHERS = "earthcraft.tp.others";


    @Override
	public void onEnable(){
        log = this.getLogger();
        if(log == null) {
			System.setProperty("java.util.logging.SimpleFormatter.format","%4$s: %5$s%6$s%n");
        	log = Logger.getLogger(getClass().getName());
        }

        log.info("Earthcraft enabled.");
        log.info("CLASSPATH="+System.getProperty("java.class.path"));

        initJAI();

        // Create default config file if none exists
        saveDefaultConfig();

        config = new ConfigManager(this);

        landmarks = config.getLandmarks();
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

    	// Load info from config file
    	EarthGen gen = config.createEarthGen(worldName);

    	generators.put(worldName,gen);
    	log.info("Creating new Earthcraft Generator for "+worldName);

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

    		try {
    			new ImageReadWriteSpi().updateRegistry(registry);
    		} catch(IllegalArgumentException e) {
    			// Probably indicates it was already registered.
    		}
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
    		} else if(subcmd.equalsIgnoreCase("elev")) {
    			return onElevCommand(sender,subargs);
    		}
    	} else if(name.equalsIgnoreCase("earthpos")){
			return onPosCommand(sender,args);
    	} else if(name.equalsIgnoreCase("earthtp")){
			return onTPCommand(sender,args);
    	}else if(name.equalsIgnoreCase("earthelev")) {
			return onElevCommand(sender,args);
		}

    	return false;
    }


    /**
     * Handle elev command
     *
     * usage: /earthelev [lat lon]
     *
     * @param sender
     * @param args
     * @return
     */
    private boolean onElevCommand(CommandSender sender, String[] args) {

    	if(! (sender instanceof Player) ) {
			sender.sendMessage("Error: Not valid from the command line.");
			return false;
		}
    	Player player = (Player) sender;
    	ChunkGenerator cgen = player.getWorld().getGenerator();
    	if(!(cgen instanceof EarthGen)) {
    		sender.sendMessage("Error: you are not in an Earthcraft world.");
    		return true;
    	}
    	EarthGen gen = (EarthGen)cgen;

    	Coordinate coord;
		if( args.length == 0) {
			// use player's location
			Location loc = player.getLocation();
			MapProjection proj = gen.getMapProjection();
			coord = proj.locationToCoordinate(loc);
		} else if( args.length == 2) {
			try {
				double lat = Double.parseDouble(args[0]);
				double lon = Double.parseDouble(args[0]);
				coord = new Coordinate(lat,lon);
			} catch(NumberFormatException e) {
				sender.sendMessage("Error: Invalid coordinates");
				return false;
			}
		} else {
			return false;
		}

		// Get the elevation
		ElevationProvider elevation = gen.getElevationProvider();
		double elev;
		try {
			elev = elevation.fetchElevation(coord);
		} catch (DataUnavailableException e) {
			sender.sendMessage(Color.YELLOW+"Elevation: "+Color.RED+"Unavailable");
			return true;
		}

		sender.sendMessage(Color.YELLOW+"Elevation: "+elev+" m");
		return true;
	}


	/**
     * Handle tp command
     *
     * usage: /earthtp [player] lat lon [elev]
     * 		  /earthtp [player] landmark
     * @param sender
     * @param args
     * @return
     */
	private boolean onTPCommand(CommandSender sender, String[] args) {
		// Parse parameters
		String playerName = null;
		Double lat = null;
		Double lon = null;
		Double elev = null;
		String landmark = null;

		if(args.length < 1) {
			// invalid
			return false;

		} else if(args.length == 1) {
			// landmark
			landmark = args[0];

		} else if(args.length == 2) {
			// player landmark
			// lat lon

			try {
				// assume lat lon first
				lat = new Double(args[0]);
				lon = new Double(args[1]);
			} catch( NumberFormatException e) {
				// must be player landmark

				playerName = args[0];
				landmark = args[1];
			}

		} else if(args.length == 3) {
			// player lat lon
			// lat lon elev

			try {
				// assume lat lon elev first
				lat = new Double(args[0]);
				lon = new Double(args[1]);
				elev = new Double(args[2]);
			} catch( NumberFormatException e) {
				// must be player lat lon

				playerName = args[0];
				try {
					lat = new Double(args[1]);
					lon = new Double(args[2]);
				} catch( NumberFormatException f) {
					sender.sendMessage("Error: Invalid coordinates");
					return false;
				}
			}
		} else if(args.length == 4) {
			// player lat lon elev
			playerName = args[0];
			try {
				// assume lat lon elev first
				lat = new Double(args[1]);
				lon = new Double(args[2]);
				elev = new Double(args[3]);
			} catch( NumberFormatException e) {
				sender.sendMessage("Error: Invalid coordinates");
				return false;
			}
		} else {
			//invalid
			return false;
		}

		// Get teleporting player
		Player player;
		if( playerName != null ) {
			// Use specified player
			player = Bukkit.getPlayer(playerName);
			if( !player.isOnline() ) {
				sender.sendMessage("Error: "+playerName+" is offline");
				return true; // correct usage despite error
			}
		} else {
			// Use sender
			if( ! (sender instanceof Player) ) {
				sender.sendMessage("Error: Player required from console");
				return false;
			}
			player = (Player)sender;
		}
		// Check for valid world
		World world = player.getWorld();
		ChunkGenerator cgen = world.getGenerator();
		if(!(cgen instanceof EarthGen)) {
			sender.sendMessage(player.getDisplayName()+" is not in an Earthcraft world.");
			return false;
		}
		EarthGen gen = (EarthGen) cgen;

		// Get coordinate
		Coordinate coord;
		if( landmark != null) {
			// from landmark
			if( !landmarks.containsKey(landmark)) {
				sender.sendMessage("Error: Unrecognized landmark");
				// invalid landmark
				return false;
			}
			coord = landmarks.get(landmark);
		} else {
			// from coordinates
			coord = new Coordinate(lat,lon);
		}

		// assume permission to teleport yourself, since we got the command
		// check for permission to teleport others
		if(!sender.equals(player)) {
			if( ! sender.hasPermission(PERM_TP_OTHERS) ) {
				sender.sendMessage("You don't have permission to teleport others. Need "+PERM_TP_OTHERS);
			}
		}

		// Convert to Location
		MapProjection proj = gen.getMapProjection();
		Location loc = proj.coordinateToLocation(player.getWorld(), coord);

		if(elev != null) {
			//Specific location
			ElevationProjection eproj = gen.getElevationProjection();
			double y = eproj.elevationToY(elev);
			loc.setY(y);
		} else {
			// top of the world, for now
			if(Double.isNaN(loc.getY()) ){
				//TODO make sure this is a safe location
				loc.setY(world.getHighestBlockYAt(loc));
			}
		}

		log.info("Teleporting "+player.getName()+" to "+loc);
		player.teleport(loc);

		return true;
	}


	/**
	 * Handle pos command
	 *
	 * usage: /earthpos [player]
	 * @param sender
	 * @param args
	 * @return
	 */
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

		double elev = gen.getElevationProjection().yToElevation(loc.getY());
		coord.z = elev;

		Coordinate localScale = proj.getLocalScale(coord);
		String message = String.format("%s located at %s", player.getName(),
				ProjectionTools.latlonelevString(coord,localScale));
		sender.sendMessage(message);
		return true;
	}


	public static void main(String[] a) {
    	System.out.println("CLASSPATH="+System.getProperty("java.class.path"));
    	//Coordinate x = new Coordinate(1.,2.);

    	EarthcraftPlugin plugin = new EarthcraftPlugin();
    	plugin.getDefaultWorldGenerator("foo", "id");
    }


	public ConfigManager getConfigManager() {
		return config;
	}
}
