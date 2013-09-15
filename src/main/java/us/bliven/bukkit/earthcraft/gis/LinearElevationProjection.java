package us.bliven.bukkit.earthcraft.gis;

public class LinearElevationProjection implements ElevationProjection {

	private double origin;
	private double scale;

	public LinearElevationProjection(double origin, double scale) {
		this.origin = origin;
		this.scale = scale;
	}
	@Override
	public double yToElevation(double y) {
		double elev = scale*y+origin; //elev

		return elev;
	}

	@Override
	public double elevationToY(double elev) {
		double locY = (elev-origin)/scale; // Up-ness

		return locY;
	}

}
