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
		System.out.println(" ******* ");
		
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
		
		// For each pom in the project update surefire
		for (int i = 0; i < pomLocations.size(); i++) {
			updateVersion(dependencyName, version, pomLocations.get(i));
		}
	}
	
	private static Plugin createPlugin (
			String groupId, 
			String artifactId, 
			String version) {	
		Plugin plugin = new Plugin();
		plugin.setGroupId(groupId);
		plugin.setArtifactId(artifactId);
		
		if(version != null) {
			plugin.setVersion(version);
		}
		return plugin;
	}
	
	private static void updateVersion(String dependencyName, String version, String pomLocation) {	
		String pom2Location = pomLocation.substring(0, pomLocation.indexOf(".xml")) + "-temp.xml";
		Reader reader = null;
		boolean changed = false;
	    boolean found = false;
	    
		try {
			reader = new FileReader(pomLocation);
			
			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
			MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
		    Model model = xpp3Reader.read(reader);
		    Writer writer = new FileWriter(pom2Location);
		    
		    Build build = model.getBuild();
		    List<Plugin> oldPlugins = new ArrayList<Plugin>();
		    
		    // pom might not have a build at all
		    if (build != null) {
		    	oldPlugins = build.getPlugins();
		    }		
		    		
		    List<Dependency> oldDependencies = model.getDependencies();
		    
	    	for ( int i = 0; i < oldPlugins.size(); i++) {
	    		Plugin oldPlugin = oldPlugins.get(i);
	    		
		    	if (oldPlugin.getArtifactId().equals(dependencyName)) {
		    		found = true;		    		
		    		oldPlugins.get(i).setVersion(version);
		    		
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
			    	
				    changed = true;
				    break;
		    	}
		    }
	    	if (!found) {
	    		oldPlugins.add(createPlugin("org.apache.maven.plugins", 
	        			"maven-surefire-plugin", "2.19.1"));
	    	    changed = true;
	    	}
	    	
	    	// If pom does not have a build, then build one and add Surefire
	    	if (build == null) {
	    		build = new Build();
	    	}

	    	build.setPlugins(oldPlugins);
    	    model.setBuild(build);
    	    xpp3Writer.write( writer, model );
    	    writer.close();

	  	} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
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
