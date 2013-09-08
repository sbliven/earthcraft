package us.bliven.bukkit.earthcraft.gis;

import java.util.concurrent.Callable;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;

import com.vividsolutions.jts.geom.Coordinate;

public class InterpolatedCoverageElevationProvider extends
		GridCoverageElevationProvider {

	GridCoverageElevationProvider provider;


	public InterpolatedCoverageElevationProvider(GridCoverageElevationProvider provider) {
		this.provider = provider;
	}

	@Override
	protected String getTileName(Coordinate coord) {
		return provider.getTileName(coord);
	}

	@Override
	protected Callable<GridCoverage2D> createTileLoader(Coordinate coord) {
		Callable<GridCoverage2D> provider = this.provider.createTileLoader(coord);

		return new InterpolatedCoverageLoader(provider);
	}

	protected static class InterpolatedCoverageLoader implements Callable<GridCoverage2D> {
		private Callable<GridCoverage2D> provider;

		public InterpolatedCoverageLoader(Callable<GridCoverage2D> provider) {
			this.provider = provider;
		}

		@Override
		public GridCoverage2D call() throws Exception {
			GridCoverage2D coverage = provider.call();

			GridCoverage2D interpolated = Interpolator2D.create(coverage);

			return interpolated;
		}

	}
}