/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import myflickr.core.Comm;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author neil
 */
public class MyFlickrLogManager {
    static Logger logger;

    public static Logger getLogger(){
        if (logger == null){
            Properties pro = new Properties();
            InputStream is = MyFlickrLogManager.class.getClassLoader().getResourceAsStream("myflickr/resources/log4j.properties");
            try
            {
                pro.setProperty("log4j.appender.logfile.File", Comm.STORAGE_BASE + System.getProperty("file.separator") + "MyLog.log");
                pro.load(is);
            } catch (IOException e)
            {
                BasicConfigurator.configure();
                e.printStackTrace();
            }

            PropertyConfigurator.configure(pro);
            logger = Logger.getRootLogger();
        }
        return logger;
    }

}
