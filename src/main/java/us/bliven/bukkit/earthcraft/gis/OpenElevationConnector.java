package us.bliven.bukkit.earthcraft.gis;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Class for interfacing with MapQuest's Open Elevation API
 * 
 * Data is © 2012 MapQuest, Inc, and is accessed under the terms given at
 * http://developer.mapquest.com/web/products/open/elevation-service
 * 
 * @author Spencer Bliven
 *
 */
public class OpenElevationConnector implements ElevationProvider {

	public static final String USERAGENT = "SBGen v0.1";

	private int requestsMade = 0; //Number of API calls made by this instance
	
	@Override
	public List<Double> fetchElevations(List<Coordinate> l) throws DataUnavailableException {
		final int inputSize = l.size();
		if(inputSize < 1) {
			return new ArrayList<Double>();
		}

		// build URL from inputs
		final String baseURL = "http://open.mapquestapi.com/elevation/v1/profile?";
		final String params = "inShapeFormat=raw&outShapeFormat=none&outFormat=xml&latLngCollection=";
		StringBuilder uri = new StringBuilder(baseURL);
		uri.append(params);
		for(Coordinate p : l) {
			uri.append(p.x); // actually lat
			uri.append(',');
			uri.append(p.y); // actually lon
			uri.append(',');
		}

		URL url;
		try {
			url = new URL(uri.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		
		System.out.println(url);
		
		// Create SAX parser for the XML
		
		OpenElevationHandler handler = new OpenElevationHandler();
		
		
		// Process requested URL
		try {
			requestsMade++;
			OpenElevationConnector.handleRestRequest(url, handler);
		} catch (Exception e) {
			DataUnavailableException de = new DataUnavailableException(e.getMessage(),e);
			throw de;
		}
		
		List<Double> altitude = handler.altitude;
		
		// Check for errors
		if(handler.status != null && handler.status != 0) {
			if(inputSize == 1) { //Base case
				// Only requested one point. Return [null]
				System.err.println(String.format("MapQuest Error: Got response %d for %s%nMapQuest Error message: \"%s\"%n",handler.status,uri, handler.message));
				altitude = Lists.newArrayList((Double)null);
			} else {
				// Try individual requests, then merge them
				altitude = new ArrayList<Double>(inputSize);
				for(Coordinate p : l) {
					List<Double> results = fetchElevations(Lists.newArrayList(p));
					assert(results.size() == 1);
					altitude.addAll(results);
				}
			}
		}

		
		// Results should have one value per input
		assert( altitude.size() == l.size());
		
		return altitude;
	}

	private static void handleRestRequest(URL url, DefaultHandler handler) throws SAXException, IOException, ParserConfigurationException {
		// Fetch XML stream
		HttpURLConnection huc = null;

		huc = (HttpURLConnection) url.openConnection();
		huc.addRequestProperty("User-Agent", USERAGENT);
		
		int responseCode = huc.getResponseCode();
		
		if(responseCode != 200) {
			System.err.println("HTTP Error: Got response "+responseCode+" for "+url);
		}
		
		InputStream response = huc.getInputStream();
		

		InputSource xml = new InputSource(response);

		// Parse XML
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		saxParser.parse(xml, handler);
	}
	
	private static class OpenElevationHandler extends DefaultHandler {
		public List<Double> altitude = new ArrayList<Double>();
		public Integer status = null;
		public String message = null;
		private String data = "";
		
		// State machine
		private static enum State {
			IDLE,
			GET_HEIGHT,
			GET_STATUS,
			GET_MSG
		};
		private State state = State.IDLE;
		
		int pointNum = 0;
		@Override
		public void startElement(String uri, String localName,String qName, 
				Attributes attributes) throws SAXException {
			//System.out.println("START "+qName);
			if(qName.equalsIgnoreCase("height")) {
				state = State.GET_HEIGHT;
			} else if(qName.equalsIgnoreCase("statusCode")) {
				state = State.GET_STATUS;
			} else if(qName.equalsIgnoreCase("message")) {
				state = State.GET_MSG;
			} else if(qName.equalsIgnoreCase("distanceHeight")) {
				pointNum++;
			}
			
			data = "";
		}
		@Override
		public void endElement(String uri, String localName, String qName) {
			//System.out.println("END   "+qName);
			assert( state == State.GET_HEIGHT && qName.equalsIgnoreCase("height") ||
					state == State.GET_STATUS && qName.equalsIgnoreCase("statusCode") ||
					state == State.GET_MSG && qName.equalsIgnoreCase("message") ||
					state == State.IDLE);
			
			switch(state) {
			case GET_HEIGHT:
				Double d = new Double(data);
				altitude.add(d);
				if(pointNum != altitude.size()) {
					System.out.flush();
					System.err.format("Length mismatch. Should be on point %d but have %d points.",pointNum, altitude.size());
					System.err.flush();
				}
				break;
			case GET_STATUS:
				status = new Integer(data);
				break;
			case GET_MSG:
				if(message == null) {
					message = data;
				} else {
					message += "\n"+data;
				}
				break;
			}

			state = State.IDLE;
		}
		@Override
		public void characters(char[] ch, int start, int length) {
			//System.out.println("DATA  "+data);
			if(state != State.IDLE) {
				data += new String(ch,start,length);
			}
		}
	};
	public static void main(String[] a) {
		try {
			URL url = new URL("http://open.mapquestapi.com/elevation/v1/profile?inShapeFormat=raw&latLngCollection=32.839825,-117.244669&outShapeFormat=none&outFormat=xml");
			
			DefaultHandler handler = new DefaultHandler() {
				@Override
				public void startElement(String uri, String localName,String qName, 
						Attributes attributes) throws SAXException {
					System.out.println("Started "+qName);
				}
			};
			
			OpenElevationConnector.handleRestRequest(url, handler);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	/**
	 * Get the number of API requests made over the lifetime of this instance
	 */
	public int getRequestsMade() {
		return requestsMade;
	}

}