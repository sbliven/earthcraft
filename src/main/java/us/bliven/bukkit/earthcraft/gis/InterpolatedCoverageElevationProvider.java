package us.bliven.bukkit.earthcraft.gis;

import java.util.concurrent.Callable;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic;

import org.bukkit.configuration.ConfigurationSection;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.opengis.coverage.PointOutsideCoverageException;

import us.bliven.bukkit.earthcraft.ConfigManager;
import us.bliven.bukkit.earthcraft.Configurable;

import com.vividsolutions.jts.geom.Coordinate;

public class InterpolatedCoverageElevationProvider
extends GridCoverageElevationProvider implements Configurable {

	private GridCoverageElevationProvider provider;
	private Interpolation interpolation;


	public InterpolatedCoverageElevationProvider(GridCoverageElevationProvider provider) {
		this.provider = provider;

		interpolation = new InterpolationBicubic(16);
		//interpolation = new InterpolationBilinear();
		//interpolation = new InterpolationNearest();

	}
	/**
	 * @deprecated For use with initFromConfig
	 */
	@Deprecated
	public InterpolatedCoverageElevationProvider() {
		this(null);
	}

	public InterpolatedCoverageElevationProvider(ConfigManager config, ConfigurationSection params) {
		this();
		initFromConfig(config, params);
	}

	@Override
	public void initFromConfig(ConfigManager config, ConfigurationSection params) {
		super.initFromConfig(config, params);

		for(String param : params.getKeys(false)) {
			if( param.equalsIgnoreCase("provider") ) {
				provider = config.createSingleConfigurable(GridCoverageElevationProvider.class,
						params.getConfigurationSection(param), provider);
			} else {
				log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
			}
		}
		if( provider == null) {
			provider = new SRTMPlusElevationProvider();
		}
	}

	@Override
	protected String getTileName(Coordinate coord) {
		return provider.getTileName(coord);
	}

	@Override
	protected Callable<GridCoverage2D> createTileLoader(Coordinate coord) {
		Callable<GridCoverage2D> provider = this.provider.createTileLoader(coord);

		return new InterpolatedCoverageLoader(provider,interpolation);
	}

	@Override
	public Double fetchElevation(Coordinate coord) throws DataUnavailableException {
		try {
			return super.fetchElevation(coord);
		} catch( PointOutsideCoverageException e) {
			// Can't interpolate at boundaries of tiles
			// Use nearest neighbor, which will leave a hard edge at tile boundaries
			return provider.fetchElevation(coord);
			// TODO Do manual interpolation or whatever GeoTools suggests
		}
	}

	protected static class InterpolatedCoverageLoader implements Callable<GridCoverage2D> {
		private Callable<GridCoverage2D> provider;
		private Interpolation interpolation;

		public InterpolatedCoverageLoader(Callable<GridCoverage2D> provider, Interpolation interpolation) {
			this.provider = provider;
			this.interpolation = interpolation;
		}

		@Override
		public GridCoverage2D call() throws Exception {
			GridCoverage2D coverage = provider.call();


			GridCoverage2D interpolated;
			interpolated = Interpolator2D.create(coverage,this.interpolation);
			return interpolated;
		}

	}
}
