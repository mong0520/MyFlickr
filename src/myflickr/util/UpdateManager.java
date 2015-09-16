/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.Properties;
import myflickr.core.Comm;
import org.apache.log4j.*;
/**
 *
 * @author neil
 */
public class UpdateManager {
    final private String updateFile = "update.ini";
    final private String updateUrl = "http://lineage.twbbs.org/flickrminer/update.ini";
    //final private String updateUrl = "https://www.dropbox.com/s/tj21fldue97048w/update.ini?dl=1";

    
    private String latestVersion = "";    
    private String downloadPage = "";
    private String updateDesc = "";           

    private int currentVersion_Major = 0;
    private int currentVersion_Minor = 0;
    private int currentVersion_Build = 0;

    private int latestVersion_Major = 0;
    private int latestVersion_Minor = 0;
    private int latestVersion_Build = 0;
    private Logger logger;
    private Properties properties;
    private String currentVersion;

    public UpdateManager(){        
        logger = MyFlickrLogManager.getLogger(); 
        properties = new java.util.Properties();  
        logger.log(Level.DEBUG, "Update Manager is initialized");
    }
    
        
    public void initUpdateStatus(){                
        File updateIni = new File(Comm.STORAGE_BASE + System.getProperty("file.separator") + updateFile);
        try {
            logger.log(Level.DEBUG, "Start to download " + updateUrl);
            URL website = new URL(updateUrl);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(updateIni);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            //updateIni = new File(Comm.STORAGE_BASE + System.getProperty("file.separator") + updateFile);                 
            logger.log(Level.DEBUG, updateFile + " is downloaded to path: "+ updateIni.getAbsolutePath());
            if(updateIni.exists()){                                        
                properties.load(new InputStreamReader(new FileInputStream(updateIni), "UTF-8"));                
                latestVersion = properties.getProperty("main.LatestVersion");
                downloadPage = properties.getProperty("main.UpdateUrl");
                updateDesc = properties.getProperty("main.Desc");                
                logger.log(Level.DEBUG, "Latest Version = " + latestVersion);
                logger.log(Level.DEBUG, "UpdateUrl = " + downloadPage);
                logger.log(Level.DEBUG, "Description = " + getUpdateDesc());

            }else{                
                logger.log(Level.DEBUG, "Can't open file "+ updateIni.getAbsolutePath());
            }            
        } catch (MalformedURLException ex) {
            logger.log(Level.DEBUG, ex);           
        } catch (IOException ex) {
            logger.log(Level.DEBUG, ex);
        } 
                
    }
    
    public boolean CheckNeedToUpdate(){        
        if(latestVersion != null && !latestVersion.equalsIgnoreCase(""))
        {
            String[] v = latestVersion.split("\\.");            
            if(v.length !=3){                
                return false;
            }
            latestVersion_Major = Integer.parseInt(v[0]);
            latestVersion_Minor = Integer.parseInt(v[1]);
            latestVersion_Build = Integer.parseInt(v[2]);
        }

        if(latestVersion_Major > currentVersion_Major){
            return true;
        }else{
            if(latestVersion_Minor > currentVersion_Minor){
                return true;
            }else{
                if(latestVersion_Build > currentVersion_Build){
                    return true;
                }else{
                    return false;
                }
            }
        }

    }
    public void setCurrentVersion(String version){
        //System.out.println("Current Version="+version);        
        currentVersion = version;
        logger.log(Level.DEBUG, "Current Version = " + version);
        
        String[] v = currentVersion.split("\\.");
        if(v.length !=3){
            return;
        }
        currentVersion_Major = Integer.parseInt(v[0]);
        currentVersion_Minor = Integer.parseInt(v[1]);
        currentVersion_Build = Integer.parseInt(v[2]);
    }    
    public String getLatestVersion(){        
        return this.latestVersion;
    }



    public static void main(String[] args){
        UpdateManager u = new UpdateManager();        
        u.initUpdateStatus();        
        u.setCurrentVersion("1.3.0");
        System.out.println(u.CheckNeedToUpdate());
    }

    /**
     * @return the updateDesc
     */
    public String getUpdateDesc() {
        return updateDesc;
    }
}