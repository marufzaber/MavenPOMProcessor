package uci.ics.mondego;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenPomProcessor {
	private static final Logger logger = LogManager.getLogger(MavenPomProcessor.class);

	/**
	 * arg 0 ---- Project Location
	 * arg 1 ---- Name of the dependency of which the version is to be changed
	 * arg 2 ---- Version number to which it will be changed
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length < 3) {
			logger.error(
					"Insufficient arguments. Please pass : "
					+ "i) project directory "
					+ "ii) name of the library "
					+ "iii) version number");
		}
		
		String projectLocation = args[0];
		String dependencyName = args[1];
		String version = args[2];

		List<String> pomLocations = scanPOMFiles(projectLocation);
		
		for (int i = 0; i < pomLocations.size(); i++) {
			updateVersion(dependencyName, version, pomLocations.get(i));
		}
	}
	
	private static void updateVersion(String dependencyName, String version, String pomLocation) {
		
		String pom2Location = pomLocation.substring(0, pomLocation.indexOf(".xml")) + "-temp.xml";
		Reader reader = null;
		boolean changed = false;
		
		try {
			reader = new FileReader(pomLocation);
			
			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
		    Model model = xpp3Reader.read(reader);

		    Build build = model.getBuild();
		    List<Plugin> oldPlugins = new ArrayList<Plugin>();
		    if (build != null) {
		    	oldPlugins = build.getPlugins();
		    }		
		    		
		    List<Dependency> oldDependencies = model.getDependencies();
		    
	    	for( int i = 0; i < oldPlugins.size(); i++) {
	    		Plugin oldPlugin = oldPlugins.get(i);
		    	if (oldPlugin.getArtifactId().equals(dependencyName)) {
		    		
		    		Writer writer = new FileWriter(pom2Location);
		    		MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
		    		oldPlugins.get(i).setVersion(version);
		    		
		    		build.setPlugins(oldPlugins);
		    		// Update JUnit
			    	for (int j = 0; j < oldDependencies.size(); j++) {
				    	if (oldDependencies.get(j).getArtifactId().equals("junit")) {				    	
				    		if(oldDependencies.get(j).getVersion() == null 
				    			|| oldDependencies.get(j).getVersion().length() == 0) {
				    			oldDependencies.get(j).setVersion("4.11");
					    		model.setDependencies(oldDependencies);
				    		} else {
				    			String v = oldDependencies.get(j).getVersion();
				    			if (!(v.contains("4.11") || v.contains("4.12") ||
				    					v.contains("4.10") || v.contains("4.9") || 
				    					v.contains("4.8"))){
					    			oldDependencies.get(j).setVersion("4.8.1");
						    		model.setDependencies(oldDependencies);
				    			} else{
						    		model.setDependencies(oldDependencies);
				    			}
				    		}
				    	}
				    }
		    		
		    		model.setBuild(build);
				    xpp3Writer.write( writer, model );
				    
				    writer.close();
				    changed = true;
				    break;
		    	}
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} finally {
		    try {
				reader.close();
				if (changed) {
					File pom = new File(pomLocation);
					if(pom.delete()){
						File newPom = new File(pom2Location);
						newPom.renameTo(new File(pomLocation));
					} else{
						logger.error("PROBLEM IS DELETING OLD POM FILE : "+pomLocation);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static List<String> scanPOMFiles(String directoryName) {	
		List<String> pom = new ArrayList<String>();
		File directory = new File(directoryName);
	    File[] fList = directory.listFiles();	    	
	    if(fList != null) {
	    	for (File file : fList) {    	        	
	            if (file.isFile()) {
	            	String fileAbsolutePath = file.getAbsolutePath();	
	                if(fileAbsolutePath.endsWith("pom.xml")){
	                	pom.add(fileAbsolutePath);
	                }	                	         
	            } else if (file.isDirectory()) {
	            	pom.addAll(scanPOMFiles(file.getAbsolutePath()));
	            }
	        }
	    }
	    return pom;
    }
}
