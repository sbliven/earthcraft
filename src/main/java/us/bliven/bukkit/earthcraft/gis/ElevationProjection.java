package us.bliven.bukkit.earthcraft.gis;


/**
 * Converts minecraft y coordinates into elevations.
 *
 * For converting elevation, implement {@link ElevationProjection}
 *
 * @author Spencer Bliven
 */
public interface ElevationProjection {

	/**
	 * Convert a minecraft location to a real-world coordinate
	 * @param loc Minecraft location
	 * @return The location in the real world, as degrees lat, degrees lon, meters elevation
	 */
	public double yToElevation(double y);
	/**
	 * Convert a real-world coordinate to a minecraft location
	 * @param coord The location in the real world, as degrees lat, degrees lon, meters elevation
	 * @return Minecraft location
	 */
	public double elevationToY(double elev);

}
