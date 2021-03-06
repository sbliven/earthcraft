package us.bliven.bukkit.earthcraft;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;

import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import us.bliven.bukkit.earthcraft.config.ConfigManager;
import us.bliven.bukkit.earthcraft.worldgen.EarthChunkProvider;
import us.bliven.bukkit.earthcraft.worldgen.EarthWorld;

import com.sun.media.jai.imageioimpl.ImageReadWriteSpi;

@Mod(modid = EarthcraftMod.MODID, version = EarthcraftMod.VERSION)
public class EarthcraftMod {
    public static final String MODID = "earthcraft";
    public static final String VERSION = "0.1";

	private Logger log = Logger.getLogger(MODID);
    public static final WorldType EARTHCRAFT_WORLD = new EarthWorld("PYRAMID");

	private ConfigManager config = null;
//	private Map<String,Coordinate> landmarks = null;

	// Create a new EarthGen for each world to allow configurability
	private final Map<String,EarthChunkProvider> generators = new HashMap<String, EarthChunkProvider>();

	// Permissions
	static final String PERM_TP_OTHERS = "earthcraft.tp.others";

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new ConfigManager(this,event.getSuggestedConfigurationFile());

		config.load();

		// Configuration goes here.
			
		config.save();
	}

	@EventHandler
	public void init(FMLInitializationEvent event){
        if(log == null) {
			System.setProperty("java.util.logging.SimpleFormatter.format","%4$s: %5$s%6$s%n");
        	log = Logger.getLogger(getClass().getName());
        }

        log.info("Earthcraft enabled.");
        log.info("CLASSPATH="+System.getProperty("java.class.path"));

        initJAI();

        // Create default config file if none exists
        //saveDefaultConfig();//TODO reenable

//        landmarks = config.getLandmarks();
    }

//	public synchronized ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
//    	if(generators.containsKey(worldName)) {
//    		return generators.get(worldName);
//    	}
//
//    	// Load info from config file
//    	EarthGen gen = config.createEarthGen(worldName);
//
//    	generators.put(worldName,gen);
//    	log.info("Creating new Earthcraft Generator for "+worldName);
//
//    	return gen;
//    }

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
    	InputStream in = EarthcraftMod.class.getResourceAsStream(resource);
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

   
//    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
//    	String name = cmd.getName();
//    	if(name.equalsIgnoreCase("earth")) {
//    		if( args.length<1 )
//    			//no subcommand specified
//    			return false;
//    		String subcmd = args[0];
//    		String[] subargs = Arrays.copyOfRange(args, 1, args.length);
//    		if( subcmd.equalsIgnoreCase("pos")) {
//    			return onPosCommand(sender,subargs);
//    		} else if(subcmd.equalsIgnoreCase("tp")) {
//    			return onTPCommand(sender,subargs);
//    		} else if(subcmd.equalsIgnoreCase("elev")) {
//    			return onElevCommand(sender,subargs);
//    		} else if(subcmd.equalsIgnoreCase("climate") ||
//    				subcmd.equalsIgnoreCase("info") ) {
//    			return onClimateCommand(sender,subargs);
//    		}
//    	} else if(name.equalsIgnoreCase("earthpos")){
//			return onPosCommand(sender,args);
//    	} else if(name.equalsIgnoreCase("earthtp")){
//			return onTPCommand(sender,args);
//    	} else if(name.equalsIgnoreCase("earthelev")) {
//			return onElevCommand(sender,args);
//		} else if(name.equalsIgnoreCase("earthclimate") ||
//				name.equalsIgnoreCase("earthinfo")) {
//			return onClimateCommand(sender,args);
//		}
//
//    	return false;
//    }
//
//
//	/**
//     * Handle climate command
//     *
//     * usage: /earthclimate [lat lon]
//     *		  /earthclimate [landmark]
//     * @param sender
//     * @param args
//     * @return
//     */
//    private boolean onClimateCommand(CommandSender sender, String[] args) {
//
//    	if(! (sender instanceof Player) ) {
//			sender.sendMessage("Error: Not valid from the command line.");
//			return false;
//		}
//    	Player player = (Player) sender;
//    	World world = player.getWorld();
//    	ChunkGenerator cgen = world.getGenerator();
//    	if(!(cgen instanceof EarthGen)) {
//    		sender.sendMessage("Error: you are not in an Earthcraft world.");
//    		return true;
//    	}
//    	EarthGen gen = (EarthGen)cgen;
//
//    	Coordinate coord;
//		if( args.length == 0) {
//			// use player's location
//			Location loc = player.getLocation();
//			MapProjection proj = gen.getMapProjection();
//			coord = proj.locationToCoordinate(loc);
//		} else if( args.length == 1) {
//			// use landmark
//			String landmark = args[0];
//			if( !landmarks.containsKey(landmark)) {
//				sender.sendMessage("Error: Unrecognized landmark");
//				// invalid landmark
//				return false;
//			}
//			coord = landmarks.get(landmark);
//		} else if( args.length == 2) {
//			// use coordinates
//			try {
//				double lat = Double.parseDouble(args[0]);
//				double lon = Double.parseDouble(args[1]);
//				coord = new Coordinate(lat,lon);
//			} catch(NumberFormatException e) {
//				sender.sendMessage("Error: Invalid coordinates");
//				return false;
//			}
//		} else {
//			return false;
//		}
//
//		// Assemble data
//		List<String> info = new ArrayList<String>();
//		String c = ChatColor.GRAY.toString();
//		String d = ChatColor.YELLOW.toString();
//
//		// Location
//		info.add(c+"Location: "+d+ProjectionTools.latlonelevString(coord));
//
//		// Elevation
//		ElevationProvider elevation = gen.getElevationProvider();
//		try {
//			double elev;
//			elev = elevation.fetchElevation(coord);
//			info.add(c+"Elevation: "+d+elev+" m");
//		} catch (DataUnavailableException e) {
//			info.add(c+"Elevation: "+Color.RED+"Unavailable");
//		}
//
//		// Scale
//		MapProjection mapProj = gen.getMapProjection();
//		ElevationProjection elevProj = gen.getElevationProjection();
//		Coordinate scale = mapProj.getLocalScale(coord);
//		scale.z = elevProj.getLocalScale(coord.z);
//		info.add(String.format("%sScale: %s%.4f degLat/-z,  %.4f degLon/x, %.1f m/y",
//				c,d,scale.x,scale.y,scale.z));
//
//		// Biome
//		CoordBiomeProvider biome = gen.getBiomeProvider();
//		info.add(c+"Biome: "+d+ biome.getBiome(gen, world, coord));
//
//		if(biome instanceof WhittakerBiomeProvider) {
//			Map<String,String> climateInfo = ((WhittakerBiomeProvider)biome).getClimateInfo(gen, world, coord);
//			for(String prop : climateInfo.keySet()) {
//				info.add(c+prop+": "+d+climateInfo.get(prop));
//			}
//		}
//
//		sender.sendMessage(info.toArray(new String[0]));
//
//		return true;
//	}
//
//
//	/**
//     * Handle elev command
//     *
//     * usage: /earthelev [lat lon]
//     *		  /earthelev [landmark]
//     *
//     * @param sender
//     * @param args
//     * @return
//     */
//    private boolean onElevCommand(CommandSender sender, String[] args) {
//
//    	if(! (sender instanceof Player) ) {
//			sender.sendMessage("Error: Not valid from the command line.");
//			return false;
//		}
//    	Player player = (Player) sender;
//    	ChunkGenerator cgen = player.getWorld().getGenerator();
//    	if(!(cgen instanceof EarthGen)) {
//    		sender.sendMessage("Error: you are not in an Earthcraft world.");
//    		return true;
//    	}
//    	EarthGen gen = (EarthGen)cgen;
//
//    	Coordinate coord;
//		if( args.length == 0) {
//			// use player's location
//			Location loc = player.getLocation();
//			MapProjection proj = gen.getMapProjection();
//			coord = proj.locationToCoordinate(loc);
//		} else if( args.length == 1) {
//			// use landmark
//			String landmark = args[0];
//			if( !landmarks.containsKey(landmark)) {
//				sender.sendMessage("Error: Unrecognized landmark");
//				// invalid landmark
//				return false;
//			}
//			coord = landmarks.get(landmark);
//		} else if( args.length == 2) {
//			// use coordinates
//			try {
//				double lat = Double.parseDouble(args[0]);
//				double lon = Double.parseDouble(args[1]);
//				coord = new Coordinate(lat,lon);
//			} catch(NumberFormatException e) {
//				sender.sendMessage("Error: Invalid coordinates");
//				return false;
//			}
//		} else {
//			return false;
//		}
//
//		// Get the elevation
//		ElevationProvider elevation = gen.getElevationProvider();
//		double elev;
//		try {
//			elev = elevation.fetchElevation(coord);
//		} catch (DataUnavailableException e) {
//			sender.sendMessage(Color.YELLOW+"Elevation: "+Color.RED+"Unavailable");
//			return true;
//		}
//
//		sender.sendMessage(Color.YELLOW+"Elevation: "+elev+" m");
//		return true;
//	}
//
//
//	/**
//     * Handle tp command
//     *
//     * usage: /earthtp [player] lat lon [elev]
//     * 		  /earthtp [player] landmark
//     * @param sender
//     * @param args
//     * @return
//     */
//	private boolean onTPCommand(CommandSender sender, String[] args) {
//		// Parse parameters
//		String playerName = null;
//		Double lat = null;
//		Double lon = null;
//		Double elev = null;
//		String landmark = null;
//
//		if(args.length < 1) {
//			// invalid
//			return false;
//
//		} else if(args.length == 1) {
//			// landmark
//			landmark = args[0];
//
//		} else if(args.length == 2) {
//			// player landmark
//			// lat lon
//
//			try {
//				// assume lat lon first
//				lat = new Double(args[0]);
//				lon = new Double(args[1]);
//			} catch( NumberFormatException e) {
//				// must be player landmark
//
//				playerName = args[0];
//				landmark = args[1];
//			}
//
//		} else if(args.length == 3) {
//			// player lat lon
//			// lat lon elev
//
//			try {
//				// assume lat lon elev first
//				lat = new Double(args[0]);
//				lon = new Double(args[1]);
//				elev = new Double(args[2]);
//			} catch( NumberFormatException e) {
//				// must be player lat lon
//
//				playerName = args[0];
//				try {
//					lat = new Double(args[1]);
//					lon = new Double(args[2]);
//				} catch( NumberFormatException f) {
//					sender.sendMessage("Error: Invalid coordinates");
//					return false;
//				}
//			}
//		} else if(args.length == 4) {
//			// player lat lon elev
//			playerName = args[0];
//			try {
//				// assume lat lon elev first
//				lat = new Double(args[1]);
//				lon = new Double(args[2]);
//				elev = new Double(args[3]);
//			} catch( NumberFormatException e) {
//				sender.sendMessage("Error: Invalid coordinates");
//				return false;
//			}
//		} else {
//			//invalid
//			return false;
//		}
//
//		// Get teleporting player
//		Player player;
//		if( playerName != null ) {
//			// Use specified player
//			player = Bukkit.getPlayer(playerName);
//			if( !player.isOnline() ) {
//				sender.sendMessage("Error: "+playerName+" is offline");
//				return true; // correct usage despite error
//			}
//		} else {
//			// Use sender
//			if( ! (sender instanceof Player) ) {
//				sender.sendMessage("Error: Player required from console");
//				return false;
//			}
//			player = (Player)sender;
//		}
//		// Check for valid world
//		World world = player.getWorld();
//		ChunkGenerator cgen = world.getGenerator();
//		if(!(cgen instanceof EarthGen)) {
//			sender.sendMessage(player.getDisplayName()+" is not in an Earthcraft world.");
//			return false;
//		}
//		EarthGen gen = (EarthGen) cgen;
//
//		// Get coordinate
//		Coordinate coord;
//		if( landmark != null) {
//			// from landmark
//			if( !landmarks.containsKey(landmark)) {
//				sender.sendMessage("Error: Unrecognized landmark");
//				// invalid landmark
//				return false;
//			}
//			coord = landmarks.get(landmark);
//		} else {
//			// from coordinates
//			coord = new Coordinate(lat,lon);
//		}
//
//		// assume permission to teleport yourself, since we got the command
//		// check for permission to teleport others
//		if(!sender.equals(player)) {
//			if( ! sender.hasPermission(PERM_TP_OTHERS) ) {
//				sender.sendMessage("You don't have permission to teleport others. Need "+PERM_TP_OTHERS);
//			}
//		}
//
//		// Convert to Location
//		MapProjection proj = gen.getMapProjection();
//		Location loc = proj.coordinateToLocation(player.getWorld(), coord);
//
//		if(elev != null) {
//			//Specific location
//			ElevationProjection eproj = gen.getElevationProjection();
//			double y = eproj.elevationToY(elev);
//			loc.setY(y);
//		} else {
//			// top of the world, for now
//			if(Double.isNaN(loc.getY()) ){
//				//TODO make sure this is a safe location
//				loc.setY(world.getHighestBlockYAt(loc));
//			}
//		}
//
//		log.info("Teleporting "+player.getName()+" to "+loc);
//		player.teleport(loc);
//
//		return true;
//	}
//
//
//	/**
//	 * Handle pos command
//	 *
//	 * usage: /earthpos [player]
//	 * @param sender
//	 * @param args
//	 * @return
//	 */
//	private boolean onPosCommand(CommandSender sender, String[] args) {
//		if(args.length > 1) {
//			return false;
//		}
//		Player player;
//		if( args.length == 1) {
//			// Send position of specified player
//			String playername = args[0];
//			player = Bukkit.getPlayer(playername);
//			if( !player.isOnline() ) {
//				sender.sendMessage("Error: "+playername+" is offline");
//				return true;
//			}
//
//		} else {
//			// Send position of current player
//			if( sender instanceof Player ) {
//				player = (Player) sender;
//			} else {
//				sender.sendMessage("Error: Player required from console");
//				return false;
//			}
//		}
//		String world = player.getWorld().getName();
//		EarthGen gen = generators.get(world);
//		if( gen == null) {
//			sender.sendMessage("Player "+player.getName()+" not in an Earthcraft world.");
//			return false;
//		}
//
//		Location loc = player.getLocation();
//		MapProjection proj = gen.getMapProjection();
//		Coordinate coord = proj.locationToCoordinate(loc);
//
//		double elev = gen.getElevationProjection().yToElevation(loc.getY());
//		coord.z = elev;
//
//		Coordinate localScale = proj.getLocalScale(coord);
//		String message = String.format("%s located at %s", player.getName(),
//				ProjectionTools.latlonelevString(coord,localScale));
//		sender.sendMessage(message);
//		return true;
//	}

	public ConfigManager getConfigManager() {
		return config;
	}

	public Logger getLogger() {
		return log;
	}
}
