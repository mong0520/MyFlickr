/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;



import java.io.*;
import java.net.MalformedURLException;
import java.net.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import myflickr.core.Comm;

/**
 *
 * @author Mong
 */
public class JGet
{

  public Comm.RetrunCode doDownload(String url, String fileName) throws MalformedURLException, IOException{
        //String fileExtensiont = ".jpg";
        URL u = new URL(url);
        URLConnection uc = u.openConnection();
        String contentType = uc.getContentType();
        int contentLength = uc.getContentLength();
//        if (contentType.startsWith("text/") || contentLength == -1) {
//          throw new IOException("This is not a binary file.");
//        }
        InputStream raw = uc.getInputStream();
        InputStream in = new BufferedInputStream(raw);
        byte[] data = new byte[contentLength];
        int bytesRead = 0;
        int offset = 0;
        while (offset < contentLength) {
          bytesRead = in.read(data, offset, data.length - offset);
          if (bytesRead == -1)
            break;
          offset += bytesRead;
        }
        in.close();

        if (offset != contentLength) {
          throw new IOException("Only read " + offset + " bytes; Expected " + contentLength + " bytes");
        }
        String outpuFileName = "";
        if(fileName == null)
            outpuFileName = url.substring(url.lastIndexOf('/') +1);
        else
            outpuFileName = fileName;
        //如果檔名已存在，就重新命名
        System.out.println(outpuFileName);
        while(new File(outpuFileName).exists()){
            outpuFileName = outpuFileName.replace(".jpg", ""); //remove .jpg
            Date date = new Date();
            outpuFileName += "_"+date.getTime()+".jpg";
        }


        FileOutputStream out = new FileOutputStream(outpuFileName);
        out.write(data);
        out.flush();
        out.close();
        return Comm.RetrunCode.SUCCESS;
     }

  public void JGet()
  {

  }

  public static void main(String[] args){
        try {
            JGet jget = new JGet();
            jget.doDownload("http://farm8.staticflickr.com/7022/6581762753_5fee462e39_b.jpg", "test.jpg");
        } catch (MalformedURLException ex) {
            Logger.getLogger(JGet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JGet.class.getName()).log(Level.SEVERE, null, ex);
        }
  }

}