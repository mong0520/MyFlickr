/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myflickr.util;

import java.io.*;
import java.util.Properties;
import myflickr.core.Comm;
import myflickr.core.Comm.RetrunCode;
import org.apache.log4j.*;

/**
 *
 * @author neil
 */
public class ConfigManager {
    private final String CONFIG_NAME = "flickrminer.ini";
    private File CONFIG_FILE_ABS_PATH;
    private final String PathSep = System.getProperty("file.separator");
    private Properties properties;
    private OutputStreamWriter writer; 
    private InputStreamReader reader;
    private Logger logger ;
    
    public static final String CONF_USERNAME = "Username";
    public static final String CONF_DOWNLOADPATH = "DownloadPath";
    public static final String CONF_THUMBNAIL = "Thumbnail";    
    public static final String CONF_DOWNLOAD_SIZE = "DownloadSize";

    public static final String CONF_CHECK_UPDATE = "CheckUpdate";
    
    public static final String CONF_DEF_THUMBNAIL = "true";
    public static final String CONF_DEF_CHECK_UPDATE = "true";
    public static final String CONF_DEF_DOWNLOAD_SIZE = "large";    
    
    
    public ConfigManager(){
        properties = new Properties();        
        logger = MyFlickrLogManager.getLogger(); 
        logger.log(Level.DEBUG, "ConfigManager is initialized.");
    }
    
    public Comm.RetrunCode setConfigPath(String path){
        RetrunCode ret = Comm.RetrunCode.ERROR;
        CONFIG_FILE_ABS_PATH = new File (path + PathSep + CONFIG_NAME);            
        if(CONFIG_FILE_ABS_PATH.exists()){   
            logger.log(Level.DEBUG, "Config file is set to "+CONFIG_FILE_ABS_PATH);
            ret = Comm.RetrunCode.SUCCESS;
        }             
        return ret;
    }
    public Comm.RetrunCode loadConfig(){        
        try {
            if(!CONFIG_FILE_ABS_PATH.exists())
            {
                logger.log(Level.DEBUG, "Can't found config file, try to initiate a new config file");
                if(_createConfigFile()==true){
                    logger.log(Level.DEBUG, "Config file is created, start to load config file");
                    reader = new InputStreamReader(new FileInputStream(CONFIG_FILE_ABS_PATH), "UTF-8");                        
                    properties.load(reader);
                    logger.log(Level.DEBUG, "Load config file success");
                    return Comm.RetrunCode.SUCCESS;
                }else{
                    logger.log(Level.DEBUG, "Create config file failed!");
                    return Comm.RetrunCode.ERROR;
                }
            }else{
                logger.log(Level.DEBUG, "Found config file, start to load config file");
                reader = new InputStreamReader(new FileInputStream(CONFIG_FILE_ABS_PATH), "UTF-8");                        
                properties.load(reader);
                logger.log(Level.DEBUG, "Load config file success");
                return Comm.RetrunCode.SUCCESS;
            }
        } catch (IOException ex) {
            logger.log(Level.ERROR, "Load Config "+CONFIG_FILE_ABS_PATH+" Error "+ ex.getMessage());
            return Comm.RetrunCode.ERROR;
        }
    }
    
    private boolean _createConfigFile() throws IOException{
        if(!CONFIG_FILE_ABS_PATH.exists()){
            logger.log(Level.INFO, "Trying to reating new config file: "+CONFIG_FILE_ABS_PATH.getAbsolutePath());
            return CONFIG_FILE_ABS_PATH.createNewFile();
        }else{
            return true;
        }
    }
    
    public String getValue(String key){
        return properties.getProperty(key);        
    }
    
    public String getValue(String key, String defaultValue){
        return properties.getProperty(key, defaultValue);
    }
    
    public void setValue(String key, String value){
        try {            
            writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE_ABS_PATH), "UTF-8");                        
            properties.load(reader);
            properties.setProperty(key, value);
            properties.store(writer, null);            
            logger.log(Level.INFO, "Writing config, key = "+key+ ", value = "+value);
        } catch (IOException ex) {
            logger.log(Level.ERROR, "Set Config value Error:"+ ex.getMessage());
        }
    }
    
    public void unloadConfig(){
        try {
            if(writer != null){
                writer.flush();
                writer.close();
            }
            if(reader != null){
                reader.close();
            }
        } catch (IOException ex) {
            logger.log(Level.ERROR, "Unload Config Error:"+ ex.getMessage());
        }
    }        
        
    
    public static void main(String[] args){
        ConfigManager configManager = new ConfigManager();
        configManager.setConfigPath(Comm.STORAGE_BASE);
        configManager.loadConfig();
        configManager.setValue("DownloadPath", "abc");
        String downloadPath = configManager.getValue("DownloadPath");
        System.out.println(downloadPath);
        configManager.unloadConfig();        
    }
}
