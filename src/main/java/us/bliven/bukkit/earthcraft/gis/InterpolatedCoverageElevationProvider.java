package us.bliven.bukkit.earthcraft.gis;

import java.util.concurrent.Callable;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.opengis.coverage.PointOutsideCoverageException;

import com.vividsolutions.jts.geom.Coordinate;

public class InterpolatedCoverageElevationProvider extends
		GridCoverageElevationProvider {

	private final GridCoverageElevationProvider provider;
	private final Interpolation interpolation;


	public InterpolatedCoverageElevationProvider(GridCoverageElevationProvider provider) {
		this.provider = provider;

		interpolation = new InterpolationBicubic(16);
		//interpolation = new InterpolationBilinear();
		//interpolation = new InterpolationNearest();

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
