package us.bliven.bukkit.earthcraft.gis;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
 * Data is (C) 2012 MapQuest, Inc, and is accessed under the terms given at
 * http://developer.mapquest.com/web/products/open/elevation-service
 *
 * @author Spencer Bliven
 *
 */
public class OpenElevationConnector implements ElevationProvider {//, Configurable {

	public static final String USERAGENT = "SBGen v0.1";

	private int requestsMade = 0; //Number of API calls made by this instance
	
	private String authKey;
	
	// Infrastructure for regular updates
	private OpenElevationMonitor monitor;
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> monitorHandle;

	private final Logger log;

	public OpenElevationConnector(String authKey) {
		if( authKey.length() != 32) {
			throw new IllegalArgumentException("Invalid MapQuest API consumer key."
					+ " Keys can be obtained from http://developer.mapquest.com");
		}
		this.authKey = authKey;
		monitor = new OpenElevationMonitor();
		executor = null;
		monitorHandle = null;
		log = Logger.getLogger(getClass().getName());
	}
//	@Override
//	public void initFromConfig(ConfigManager config, ConfigurationSection params) {
//		// No options currently
//		for(String param : params.getKeys(false)) {
//			log.severe("Unrecognized "+getClass().getSimpleName()+" configuration option '"+param+"'");
//		}
//	}
	@Override
	public Double fetchElevation(Coordinate query) throws DataUnavailableException{
		List<Double> q = fetchElevations(Lists.asList(query,new Coordinate[0]));
		return q.get(0);
	}

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
		uri.append("key=");
		uri.append(authKey);
		uri.append("&");
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
			throw new DataUnavailableException("Error constructing URL: "+uri.toString(),e);
		}

		//log.info(url);

		// Create SAX parser for the XML

		OpenElevationHandler handler = new OpenElevationHandler();


		// Process requested URL
		try {
			requestsMade++;
			handleRestRequest(url, handler);
		} catch (Exception e) {
			DataUnavailableException de = new DataUnavailableException(e.getMessage(),e);
			throw de;
		}

		List<Double> altitude = handler.altitude;

		// Check for errors
		if(handler.status != null && handler.status != 0) {
			if(inputSize == 1) { //Base case
				// Only requested one point. Return [null]
				log.warning(String.format("MapQuest Error: Got response %d for %s%nMapQuest Error message: \"%s\"%n",handler.status,uri, handler.message));
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

	private void handleRestRequest(URL url, DefaultHandler handler) throws SAXException, IOException, ParserConfigurationException {
		// Fetch XML stream
		HttpURLConnection huc = null;

		huc = (HttpURLConnection) url.openConnection();
		huc.addRequestProperty("User-Agent", USERAGENT);

		int responseCode = huc.getResponseCode();

		if(responseCode != 200) {
			// We're probably about to throw an exception
			log.warning("HTTP Error: Got response "+responseCode+" for "+url);
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
		protected final Logger log = Logger.getLogger(OpenElevationHandler.class.getName());

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
			assert( state == State.GET_HEIGHT && qName.equalsIgnoreCase("height") ||
					state == State.GET_STATUS && qName.equalsIgnoreCase("statusCode") ||
					state == State.GET_MSG && qName.equalsIgnoreCase("message") ||
					state == State.IDLE);

			switch(state) {
			case GET_HEIGHT:
				Double d = new Double(data);
				altitude.add(d);
				if(pointNum != altitude.size()) {
					log.severe(String.format("Length mismatch. Should be on point %d but have %d points.",pointNum, altitude.size()));
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
			case IDLE:
				break;
			default:
				log.severe("Unknown state "+state);
				break;
			}

			state = State.IDLE;
		}
		@Override
		public void characters(char[] ch, int start, int length) {
			if(state != State.IDLE) {
				data += new String(ch,start,length);
			}
		}
	};
	public static void main(String[] a) {
		try {
			String authKey = "YOUR_KEY_HERE";

			URL url = new URL("http://open.mapquestapi.com/elevation/v1/profile?key="+authKey+"&inShapeFormat=raw&latLngCollection=32.839825,-117.244669&outShapeFormat=none&outFormat=xml");

			DefaultHandler handler = new DefaultHandler() {
				@Override
				public void startElement(String uri, String localName,String qName,
						Attributes attributes) throws SAXException {
					System.out.println("Started "+qName);
				}
			};

			OpenElevationConnector oec = new OpenElevationConnector(authKey);
			oec.handleRestRequest(url, handler);
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	/**
	 * Get the number of API requests made over the lifetime of this instance
	 */
	public int getRequestsMade() {
		return requestsMade;
	}

	public void monitor() {
		if( executor == null)
			executor = Executors.newScheduledThreadPool(1);
		if( monitorHandle == null)
			monitorHandle = executor.scheduleAtFixedRate(monitor, 1, 5, TimeUnit.SECONDS);
	}
	public void stopMonitor() {
		monitorHandle.cancel(false);
	}
	@Override
	public void finalize() {
		executor.shutdownNow();
		executor = null;
	}

	private class OpenElevationMonitor implements Runnable {
		private long lastCheck;
		private int lastRequests;

		public OpenElevationMonitor() {
			this.lastRequests = 0;
			this.lastCheck = System.currentTimeMillis();
		}

		@Override
		public void run() {
			long currTime = System.currentTimeMillis();
			int currRequests = getRequestsMade();
			if( currRequests > lastRequests ) {
				// Requests were made
				log.info(String.format("Made %d requests in last %.2f sec.%n",
						currRequests-lastRequests,
						(currTime-lastCheck)/1000. ));
			}

			lastCheck = currTime;
			lastRequests = currRequests;
		}
	}

}