package us.bliven.bukkit.earthcraft.demo;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.InterpolatedCoverageElevationProvider;
import us.bliven.bukkit.earthcraft.gis.SRTMPlusElevationProvider;

import com.vividsolutions.jts.geom.Coordinate;


public class ElevationGrid {
	public static void main(String[] a) {
//		ElevationProvider elevationProvider;
		ElevationProvider elevationCache;
//		double south = 32.73;
//		double west = -117.26;
//		double north = 32.80;
//		double east = -117.20;
//		double south = 32.65;
//		double west = -117.25;
//		double north = 32.75;
//		double east = -117.15;
		//hawaii
		double south = 18.5;
		double west = -161.0;
		double north = 22.75;
		double east = -154.5;
		Unit<Length> degLat = NonSI.NAUTICAL_MILE.times(60); //Nautical_mile = 1 arcminute
		double latRes = Measure.valueOf(30.0*100, SI.METER).doubleValue(degLat);
		double lonRes = latRes;
		System.out.format("Area %f deg^2 at %f res = %d points%n",
				(north-south)*(east-west), latRes,
				((int)((north-south)/latRes))*((int)((east-west)/lonRes)));
		//double latRes = (north-south)/60.;
		//double lonRes = (east-west)/100.;

		//OpenElevationConnector oec = new OpenElevationConnector();
		//elevationProvider = oec;
		//elevationProvider = new ElevationProviderStub(south+10*latRes,north-10*latRes, west+10*lonRes,east-10*lonRes,0.,255.);

		String dir = "/Users/blivens/dev/minecraft/srtm";
		if(! (new File(dir)).exists()) {
			dir = System.getProperty("java.io.tmpdir") + "SRTMPlus";
		}

		//elevationCache = new SRTMPlusElevationProvider(dir);

		elevationCache = new InterpolatedCoverageElevationProvider(new SRTMPlusElevationProvider(dir));
		//new InterpolatingElevationCache(elevationProvider, new Coordinate(latRes,lonRes));

		Double[][] elevations = elevationGrid(elevationCache, south,north,west,east, latRes/2, lonRes/2);

		/*
		Double[][] elevations = new Double[xpoints][ypoints];
		for(int i=0;i<xpoints;i++)
			for(int j=0;j<ypoints;j++)
				elevations[i][j] = (double)((3*i+2*j)%256);
		*/

		BufferedImage image = elevationImage(elevations);

		displayImage(image);
		/*
		for(int x=0;x<elevations.length;x++) {
			for(int y=0;y<elevations[x].length;y++) {
				System.out.format("%3.0f ",elevations[x][y]);
			}
			System.out.println();
		}
		*/
		//System.out.println("Made "+oec.getRequestsMade()+" API calls.");
	}

	private static JFrame displayImage(final BufferedImage image) {
		JFrame frame = new JFrame("image");
		JPanel main = new JPanel() {
			private static final long serialVersionUID = 3493399647386782094L;

			@Override
			public void paint(Graphics g) {
				super.paint(g);
				g.drawImage(image, 0, 0, getWidth(), getHeight(),  this);
			}
		};
		main.add(new JLabel(new ImageIcon(image)));
		main.setPreferredSize(new Dimension(image.getWidth()*4,image.getHeight()*4));
		frame.getContentPane().add(main);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE );
		frame.pack();
		frame.setVisible(true);
		return frame;
	}

	/**
	 * Creates a heatmap for a matix of doubles.
	 *
	 * The input should be in [y][x] order and have the origin at the bottom
	 * left of the resulting image. Thus [0][0] is the bottom left pixel,
	 * [0][n] is the bottom right, [m][0] the top left and [m][n] is the top right.
	 *
	 *
	 * @param elevations A matrix of doubles
	 * @return A BufferedImage containing one pixel per input element
	 */
	public static BufferedImage elevationImage(Double[][] elevations) {
		int height = elevations.length;
		int width = elevations[0].length;
		// This should be in row order starting at the top left
		int[] pixels = new int[width*height*3];
		for(int x=0;x<width;x++) {
			for(int y=0;y<height;y++) {
				//int i =3*x*height+3*y;
				int i = 3*((height-y-1)*width+x);
				// Nulls are black
				if(elevations[y][x] == null || Double.isNaN(elevations[y][x])) {
					pixels[i] = pixels[i+1] = pixels[i+2] = 0;

				}
				else {
					//pixels[i] = pixels[i+1] = pixels[i+2] = elevations[x][y].intValue();

//					int color = getSeaLevelColor(elevations[y][x]);
					int color = getColor(elevations[y][x], new int[] {-5000,0,0,4000,},
							new int[] {0x000033, 0x0000FF, 0x305A29, 0xFFFFFF,} );

					pixels[i] = (color & 0xFF0000) >> 16;
					pixels[i+1] = (color & 0x00FF00) >> 8;
					pixels[i+2] = (color & 0x0000FF);
					
				}

				// 10px white scale bar at top left
				if( 2 <= x && x<= 12 && y == 2 ||
						(x==2 || x==12) && y==3)
				{
					pixels[i] = 255;
					pixels[i+1] = 255;
					pixels[i+2] = 255;
				}
			}
		}

		/*
		// Create a data buffer using the byte buffer of pixel data.
		// The pixel data is not copied; the data buffer uses the byte buffer array.
		DataBuffer dbuf = new DataBufferByte(pixels, width*height, 0);

		// The number of banks should be 1
		int numBanks = dbuf.getNumBanks(); // 1

		ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
				new int[] {8,8,8}, false, false,
				Transparency.OPAQUE,
				DataBuffer.TYPE_INT);

		SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);

		// Create a raster using the sample model and data buffer
		WritableRaster raster = Raster.createWritableRaster(sampleModel, dbuf, null);


		// Combine the color model and raster into a buffered image
		BufferedImage image = new BufferedImage(colorModel, raster, false, null);//new java.util.Hashtable());
		*/

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = image.getRaster();
		raster.setPixels(0, 0, width, height, pixels);
		return image;
	}

	/**
	 * Gets the color for an elevation by interpolating between a list of
	 * reference points. For instance, a red-white-blue gradient could be defined with
	 * <tt>getColor(x, {-10,0,10}, {0xFF0000, 0xFFFFFF, 0x0000FF} )</tt>
	 * 
	 * Points may be duplicated to establish a sharp boundary between segments.
	 * @param elevation
	 * @param refPoints A sorted list of reference points. 
	 * @param refColors A corresponding list of rgb  colors for each refPoint
	 * @return
	 */
	private static int getColor(Double x, int[] refPoints, int[] refColors) {
		if( refPoints.length == 0 || refPoints.length != refColors.length) {
			throw new IllegalArgumentException("Invalid reference arrays");
		}
		
		// Don't extrapolate outside bounds
		if( x <= refPoints[0]) {
			return refColors[0];
		}
		if( refPoints[ refPoints.length-1] <= x) {
			return refColors[ refPoints.length-1 ];
		}
		
		// Interpolate between points

		// find index such that x<refPoints[pos]
		int pos = 1;
		while( refPoints[pos] <= x) {
			pos++;
		}
		
		double alpha = 1 - (x - refPoints[pos-1])/(refPoints[pos]-refPoints[pos-1]); //interpolation factor
		
		int leftR = (refColors[pos-1] & 0xFF0000) >> 16;
		int leftG = (refColors[pos-1] & 0x00FF00) >> 8;
		int leftB = (refColors[pos-1] & 0x0000FF);
		int rightR = (refColors[pos] & 0xFF0000) >> 16;
		int rightG = (refColors[pos] & 0x00FF00) >> 8;
		int rightB = (refColors[pos] & 0x0000FF);
		
		int r = (int)(leftR*alpha + rightR*(1-alpha));
		int g = (int)(leftG*alpha + rightG*(1-alpha));
		int b = (int)(leftB*alpha + rightB*(1-alpha));
		
		int color = (r << 16) | (g << 8) | b;
		return color;
	}
	
	private static int getSeaLevelColor(Double x) {
		return getColor(x, new int[] {-50,0,0,1,1,255},
				new int[] {0x000033, 0x0000FF, 0xDCDC10, 0xDCDC10, 0x00ff00, 0x804000} );
	
//		// Below 0 is blue
//		if(x < -50) {
//			return 0x000033;
//		} else if( x < 0) {
//			// Interpolate between blue (0x0000ff)
//			// and dark blue 0x000033 at -50
//			double a = -x/50.;
//			return (int) (a*0xff + (1-a)*0x33);
//		} else if( x <1 ) {
//			// Sandy beach
//			return 0xDCDC10;
//		} else {
//			// Interpolate between green (0x00ff00)
//			// and brown 0x804000
//			int v = x.intValue();
//			if(v>255)
//				v=255;
//			int c = v/2;
//			c <<= 8;
//			c |= 255-3*v/4;
//			c <<= 8;
//			return c;
//		}
	}

	/**
	 * Fetches the elevation for a rectangular chunk of the earth, sampling
	 * at regular intervals
	 * @param p	The ElevationProvider to supply elevations
	 * @param south South border of the rectangle to sample
	 * @param north North border of the rectangle to sample
	 * @param east East border of the rectangle to sample
	 * @param west West border of the rectangle to sample
	 * @param latRes North-south sampling frequency
	 * @param lonRes East-west sampling frequency
	 * @return A matrix of elevations indexed [lat][lon], starting with [0][0]
	 *  at the south-western corner, moving eastward through [0][x], and finally
	 *  to [y][x] at the north-eastern corner of the rectangle sampled.
	 */
	public static Double[][] elevationGrid( ElevationProvider p,
			double south, double north, double west, double east,
			double latRes, double lonRes
			)
	{
		if(latRes<=0 || lonRes<=0) throw new IllegalArgumentException("Too high resolution");
		int xpoints = (int)Math.ceil((east-west)/lonRes);
		int ypoints = (int)Math.ceil((north-south)/latRes);

		if(xpoints<=0 || ypoints <=0) throw new IllegalArgumentException("Empty Region");

		Double[][] elevation = new Double[ypoints][];
		for(int y=0;y<ypoints;y++) {
			// Same y coordinate for this whole row of elevations
			elevation[y] = new Double[xpoints];
			double ycoord = south + y*latRes;
			// Build list of x coordinates for this row
			List<Coordinate> points = new ArrayList<Coordinate>(xpoints);
			for(int x=0;x<xpoints;x++) {
				double xcoord = west + x*lonRes;
				points.add(new Coordinate(ycoord,xcoord));
			}

			// Fetch row of elevations
			List<Double> elevations = null;
			try {
				elevations = p.fetchElevations(points);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//  Store elevation results, if they returned correctly
			if(elevations != null && elevations.size() == xpoints) {
				elevations.toArray(elevation[y]);
			}else {
				Arrays.fill(elevation[y],null);
			}
		}

		return elevation;
	}
}
