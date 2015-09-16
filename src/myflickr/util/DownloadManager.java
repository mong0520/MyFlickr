/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import myflickr.core.Comm;

/**
 *
 * @author neil
 */
public class DownloadManager {
    private final JGet jGet;
    public int finishedCount = 0;
    public DownloadManager(){
        jGet = new JGet();
    }

    public Comm.RetrunCode createPath(File path){
        if(!path.exists()){
            if(path.mkdirs()==true){
                return Comm.RetrunCode.SUCCESS;
            }else{
                return Comm.RetrunCode.ERROR;
            }
        }else{
            return Comm.RetrunCode.SUCCESS;
        }
    }
     public Comm.RetrunCode downloadFile(String urlSrc, File fPath, File filename) throws MalformedURLException, IOException{
         String targetPath = fPath.getPath().concat(File.separator).concat(filename.toString());
         Comm.RetrunCode rt = jGet.doDownload(urlSrc, targetPath) ;
         if(rt== Comm.RetrunCode.SUCCESS){
             finishedCount++;
         }
         return rt;
     }

    public void resetCounter() {
        finishedCount = 0;
    }
}
