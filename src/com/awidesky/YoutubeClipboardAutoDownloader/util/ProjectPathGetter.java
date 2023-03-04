package com.awidesky.YoutubeClipboardAutoDownloader.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.YoutubeAudioDownloader;

/**
 * https://stackoverflow.com/a/12733172
 * */
public class ProjectPathGetter {

	//private static TaskLogger logger //TODO 
	private static PathFindCandidates[] projectpathCandidates = new PathFindCandidates[]
			{ new PathFindCandidates(ProjectPathGetter::classLocationBased, "YoutubeAudioDownloader.class location"),
					new PathFindCandidates(ProjectPathGetter::propertyBased, "System property java.class.path"),
					new PathFindCandidates(ProjectPathGetter::fileBased, "new File(\"\")") };
	
	public static String getProjectPath() {
		
		List<String> list = Arrays.stream(projectpathCandidates).map(candidate -> {
			String ret = candidate.method.get();
			File f = new File(ret);
			if(!f.isDirectory()) ret = f.getParentFile().getAbsolutePath();
			if (System.getProperty("jpackage.app-path") != null) {
				ret += File.separator + "app";
			}
			System.out.println("Project path candidate (method : " + candidate.name + ") : " + ret);
			return ret + File.separator + "YoutubeAudioAutoDownloader-resources";
		}).map(s -> new File(s)).filter(File::exists).map(File::getParentFile).map(File::getAbsolutePath).toList();
		
		if(list.isEmpty()) {
			SwingDialogs.error("Unable to get executing jar directory!", "Cannot find YoutubeAudioAutoDownloader-resources directory! please reinstall th app!", null, true);
			Main.kill(-100);
			return null; //Unreachable
		} else {
			list.stream().forEach(s -> System.out.println("Selected project path : " + s));
			return list.get(0);
		}

	}
	
	
	private static class PathFindCandidates {
		public Supplier<String> method;
		public String name;
		
		public PathFindCandidates(Supplier<String> method, String name) {
			this.method = method;
			this.name = name;
		}
	}
	
	/**
	 * Get project path by find Class path as an URL and decode as string
	 * 
	 * doesn't work in IDE(points bin folder of project root)
	 * */
	private static String classLocationBased() {
		return urlToFile(getLocation(YoutubeAudioDownloader.class)).getAbsolutePath();
	}
	/**
	 * Get project path by getting system property java.class.path
	 * 
	 * doesn't work in IDE(points bin folder of project root)
	 * */
	private static String propertyBased() {
		return System.getProperty("java.class.path");
	}
	/**
	 * Get project path by getting absolute path of new File("")
	 * 
	 * This actually get a working directory, not a path of actual working directory.
	 * It works at most cases, but not when running the jar by command prompt whose working directory is not where jar file located.   
	 * */
	private static String fileBased() {
		return new File("").getAbsolutePath();
	}
	
	
	
	
	/**
	 * Gets the base location of the given class.
	 * <p>
	 * If the class is directly on the file system (e.g.,
	 * "/path/to/my/package/MyClass.class") then it will return the base directory
	 * (e.g., "file:/path/to").
	 * </p>
	 * <p>
	 * If the class is within a JAR file (e.g.,
	 * "/path/to/my-jar.jar!/my/package/MyClass.class") then it will return the
	 * path to the JAR (e.g., "file:/path/to/my-jar.jar").
	 * </p>
	 *
	 * @param c The class whose location is desired.
	 * @see FileUtils#urlToFile(URL) to convert the result to a {@link File}.
	 */
	private static URL getLocation(final Class<?> c) {
	    if (c == null) return null; // could not load the class

	    // try the easy way first
	    try {
	        final URL codeSourceLocation =
	            c.getProtectionDomain().getCodeSource().getLocation();
	        if (codeSourceLocation != null) return codeSourceLocation;
	    }
	    catch (final SecurityException e) {
	        // NB: Cannot access protection domain.
	    }
	    catch (final NullPointerException e) {
	        // NB: Protection domain or code source is null.
	    }

	    // NB: The easy way failed, so we try the hard way. We ask for the class
	    // itself as a resource, then strip the class's path from the URL string,
	    // leaving the base path.

	    // get the class's raw resource path
	    final URL classResource = c.getResource(c.getSimpleName() + ".class");
	    if (classResource == null) return null; // cannot find class resource

	    final String url = classResource.toString();
	    final String suffix = c.getCanonicalName().replace('.', '/') + ".class";
	    if (!url.endsWith(suffix)) return null; // weird URL

	    // strip the class's path from the URL string
	    final String base = url.substring(0, url.length() - suffix.length());

	    String path = base;

	    // remove the "jar:" prefix and "!/" suffix, if present
	    if (path.startsWith("jar:")) path = path.substring(4, path.length() - 2);

	    try {
	        return new URL(path);
	    }
	    catch (final MalformedURLException e) {
	        e.printStackTrace();
	        return null;
	    }
	} 

	/**
	 * Converts the given {@link URL} to its corresponding {@link File}.
	 * <p>
	 * This method is similar to calling {@code new File(url.toURI())} except that
	 * it also handles "jar:file:" URLs, returning the path to the JAR file.
	 * </p>
	 * 
	 * @param url The URL to convert.
	 * @return A file path suitable for use with e.g. {@link FileInputStream}
	 * @throws IllegalArgumentException if the URL does not correspond to a file.
	 */
	private static File urlToFile(final URL url) {
	    return url == null ? null : urlToFile(url.toString());
	}

	/**
	 * Converts the given URL string to its corresponding {@link File}.
	 * 
	 * @param url The URL to convert.
	 * @return A file path suitable for use with e.g. {@link FileInputStream}
	 * @throws IllegalArgumentException if the URL does not correspond to a file.
	 */
	private static File urlToFile(final String url) {
	    String path = url;
	    if (path.startsWith("jar:")) {
	        // remove "jar:" prefix and "!/" suffix
	        final int index = path.indexOf("!/");
	        path = path.substring(4, index);
	    }
	    try {
	        if (System.getProperty("os.name").startsWith("Windows") && path.matches("file:[A-Za-z]:.*")) {
	            path = "file:/" + path.substring(5);
	        }
	        return new File(new URL(path).toURI());
	    }
	    catch (final MalformedURLException e) {
	        // NB: URL is not completely well-formed.
	    }
	    catch (final URISyntaxException e) {
	        // NB: URL is not completely well-formed.
	    }
	    if (path.startsWith("file:")) {
	        // pass through the URL as-is, minus "file:" prefix
	        path = path.substring(5);
	        return new File(path);
	    }
	    throw new IllegalArgumentException("Invalid URL: " + url);
	}
}
