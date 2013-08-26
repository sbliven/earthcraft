package us.bliven.bukkit.earthcraft.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Manages file downloads
 * @author Spencer Bliven
 */
public class FileCache {
	private final String dir;

	private ExecutorService executor;
	private boolean shutdownExecutorOnExit;
	
	// synchronized; Acts as lock on the downloads 
	private final Map<String,Future<Response>> currentDownloads;
	
	public FileCache() {
		this(System.getProperty("java.io.tmpdir"));
	}
	public FileCache(String cacheDir) {
		this( cacheDir, Executors.newFixedThreadPool(2) );
		this.shutdownExecutorOnExit = true;
	}
	/**
	 * Construct a FileCache. Note that when using this constructor, the calling
	 * code is responsible for shutting down the executor at the end of the
	 * instance's lifecycle. This may be accomplished by calling
	 * {@link #shutdown()} on this FileCache or by shutting down the executor
	 * directly. 
	 * @param cacheDir
	 * @param executor
	 */
	public FileCache(String cacheDir, ExecutorService executor) {
		this.dir = cacheDir;

		// Create base directory if it doesn't exist
		File dirFile = new File(dir);
		if(!dirFile.exists()) {
			dirFile.mkdir();
		}
		
		this.executor = executor;
		this.shutdownExecutorOnExit = false;
		
		// Used to synchronize threads to prevent multiple simultaneous downloads
		currentDownloads = Collections.synchronizedMap(new HashMap<String,Future<Response>>());
	}

	/**
	 * Shuts down all background threads. No additional fetch or prefetch operations
	 * should be sent after calling shutdown().
	 */
	public synchronized void shutdown() {
		executor.shutdown(); // Disable new tasks from being submitted
		try {
			// allow time for small downloads to finish
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				executor.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!executor.awaitTermination(1, TimeUnit.SECONDS))
					System.err.println("Warning: Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			executor.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
		executor = null;
	}
	
	/**
	 * Shut down background downloads
	 */
	@Override
	protected void finalize() {
		System.out.println("Finalizing FileCache");
		if(shutdownExecutorOnExit) {
			// If we're using our internal executor, shutdown threads
			shutdown();
		}
	}
	
	/**
	 * @return The cache directory
	 */
	public String getDir() {
		return dir;
	}
	
	/**
	 * Downloads a file asynchronously
	 * 
	 * @param filename relative filename
	 * @param url
	 * @return Whether the file is fully downloaded
	 */
	public synchronized boolean prefetch(String filename, URL url) {
		
		// Check if another thread is downloading
		if(currentDownloads.containsKey(filename) && 
				!currentDownloads.get(filename).isDone()  ) {
			return false;
		}
		
		// Check if it is already downloaded
		File file = new File(this.dir,filename);
		if(file.exists()) {
			// Already cached
			return true;
		}
	
		// We are downloading
		System.out.format("Downloading %s from %s%n",filename,url);
	
		if(executor == null) {
			throw new RejectedExecutionException("FileCache is shutdown.");
		}
		Future<Response> response = executor.submit(new URLRequest(file,url));
		
		// Hold lock
		currentDownloads.put(filename, response);
		
		return false;
	}
	/**
	 * Writes a file asynchronously
	 * 
	 * @param filename relative filename
	 * @param in An input string with the file contents
	 * @return Whether the file previously existed
	 */
	public synchronized boolean prefetch(String filename, InputStream in) {
		
		// Check if another thread is already writing
		if(currentDownloads.containsKey(filename) && 
				!currentDownloads.get(filename).isDone()  ) {
			return false;
		}
		
		// Check if it is already downloaded
		File file = new File(this.dir,filename);
		if(file.exists()) {
			// Already cached
			return true;
		}
		
		if(executor == null) {
			throw new RejectedExecutionException("FileCache is shutdown.");
		}
		Future<Response> response = executor.submit(new StreamRequest(file,in));
		
		// Hold lock
		currentDownloads.put(filename, response);
		
		return false;
	}
	
	/**
	 * Sychronously create a file from the givin data, or return the cached version
	 * @param filename Cache key
	 * @param in Input data
	 * @throws FileNotFoundException If the data cannot be written for some reason
	 *  (eg. IO errors, thread execution errors, etc)
	 */
	public void fetch(String filename, InputStream in) throws FileNotFoundException {
		Future<Response> result;
		synchronized(this) {
			// Check if the file is immediately available
			if( isAvailable(filename) ) {
				return;
			}
			prefetch(filename,in);

			result = currentDownloads.get(filename);
		}
		//free lock while waiting for fetch to finish
		try {
			result.get();
		} catch (Exception e) {
			FileNotFoundException fnf = new FileNotFoundException("Unable to fetch "+filename);
			fnf.initCause(e);
			throw fnf;
		}
	}
	/**
	 * Sychronously download the given URL, or return the cached version
	 * @param filename Cache key
	 * @param url Download source
	 * @throws FileNotFoundException If the data cannot be written for some reason
	 *  (eg. IO errors, thread execution errors, etc)
	 */
	public void fetch(String filename, URL url) throws FileNotFoundException {
		Future<Response> result;
		synchronized(this) {
			// Check if the file is immediately available
			if( isAvailable(filename) ) {
				return;
			}
			prefetch(filename,url);

			result = currentDownloads.get(filename);
		}
		//free lock while waiting for fetch to finish
		try {
			result.get();
		} catch (Exception e) {
			FileNotFoundException fnf = new FileNotFoundException("Unable to fetch "+filename);
			fnf.initCause(e);
			throw fnf;
		}
	}
	
	/**
	 * Sychronously fetch a file from the cache.
	 * 
	 * The file data must previously have been specified via a call to
	 * <tt>prefetch(String,*)</tt>.
	 * 
	 * @param filename Cache key
	 * @param url Download source
	 * @throws FileNotFoundException If the file was not prefetched
	 */
	public void fetch(String filename) throws FileNotFoundException {
		Future<Response> result;
		synchronized(this) {
			// Check if the file is immediately available
			if( isAvailable(filename) ) {
				return;
			}
			if( !currentDownloads.containsKey(filename) ) {
				throw new FileNotFoundException("Unable to download "+filename);
			}
			result = currentDownloads.get(filename);
		}
		//free lock while waiting for fetch to finish
		try {
			result.get();
		} catch (Exception e) {
			FileNotFoundException fnf = new FileNotFoundException("Unable to fetch "+filename);
			fnf.initCause(e);
			throw fnf;
		}
	}
	
	/**
	 * remove a file from the cache
	 * 
	 * @param filename
	 * @return true if the file was successfully deleted
	 */
	public synchronized boolean delete(String filename) {
		if( currentDownloads.containsKey(filename) ) {
			// Cancel current download and remove tracking
			Future<Response> response = currentDownloads.get(filename);
			response.cancel(true);
			
			currentDownloads.remove(filename);
		}
		
		// Delete the file
		File f = new File(dir,filename);
		return f.delete();
	}
	/**
	 * Checks whether a file is fully downloaded
	 * @param filename
	 * @return
	 */
	public synchronized boolean isAvailable(String filename) {
		// Available if exists and no one is currently downloading it
		boolean avail = ! currentDownloads.containsKey(filename) || currentDownloads.get(filename).isDone();
		return avail && new File(this.dir,filename).exists();
	}
	
	protected class URLRequest implements Callable<Response> {
	    private URL url;
	    private File file;

	    /**
	     * 
	     * @param url
	     * @param filename Absolute path; dir should already exist
	     */
	    public URLRequest(File file,URL url) {
	        this.url = url;
	        this.file = file;
	    }

	    @Override
	    public Response call() throws IOException {
			BufferedInputStream in = new BufferedInputStream(url.openStream());
			FileOutputStream out = new FileOutputStream(file);
			
			int i = 0;
			byte[] bytesIn = new byte[1024];
			while ((i = in.read(bytesIn)) >= 0) {
				out.write(bytesIn, 0, i);
			}
			out.close();
			in.close();

	        return new Response(file.toString());
	    }
	}
	
	protected class StreamRequest implements Callable<Response> {
	    private InputStream in;
	    private File file;

	    /**
	     * 
	     * @param in
	     * @param filename Absolute path; dir should already exist
	     */
	    public StreamRequest(File file,InputStream in) {
	        this.in = in;
	        this.file = file;
	    }

	    @Override
	    public Response call() throws IOException {
			FileOutputStream out = new FileOutputStream(file);
			
			int i = 0;
			byte[] bytesIn = new byte[1024];
			while ((i = in.read(bytesIn)) >= 0) {
				out.write(bytesIn, 0, i);
			}
			out.close();
			in.close();

	        return new Response(file.toString());
	    }
	}
	
	protected class Response {
	    private String filename;

	    public Response(String filename) {
	        this.filename = filename;
	    }

	    public String getFilename() {
	    	return this.filename;
	    }
	}

	public static void main(String[] args) {
		// Initialize empty cache dir
		File dir = new File(System.getProperty("java.io.tmpdir"),"FileCache");
		// Try to delete he existing folder. Will fail if there were subdirs
		if(dir.exists()) {
			for( File file :dir.listFiles() ) {
				file.delete();
			}
			dir.delete();
		}
		FileCache cache = new FileCache(dir.toString());
		
		// download some big things
		try {
			long start = System.currentTimeMillis();
			// Take 35-40 seconds
			cache.prefetch("w100n40.Bathymetry.srtm",
					new URL("ftp://topex.ucsd.edu/pub/srtm30_plus/srtm30/erm/w100n40.Bathymetry.srtm"));
			cache.prefetch("w140n40.Bathymetry.srtm",
					new URL("ftp://topex.ucsd.edu/pub/srtm30_plus/srtm30/erm/w140n40.Bathymetry.srtm"));
			
			int complete=0;
			while(complete<3) {
				if( (complete&1) == 0 && cache.isAvailable("w100n40.Bathymetry.srtm")) {
					long time = System.currentTimeMillis()-start;
					System.out.println("w100 took "+time/1000.+" s");
					complete |= 1;
				}
				if( (complete&2) == 0 && cache.isAvailable("w140n40.Bathymetry.srtm")) {
					long time = System.currentTimeMillis()-start;
					System.out.println("w140 took "+time/1000.+" s");
					complete |= 2;
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
