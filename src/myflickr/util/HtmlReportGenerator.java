/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import myflickr.core.Comm;
import myflickr.core.PhotoWrapper;

/**
 *
 * @author Mong
 */
public class HtmlReportGenerator {
    StringBuilder strb ;
    String contentToBeWrite;

    String fileName;

    public HtmlReportGenerator(){
        strb = new StringBuilder();
        contentToBeWrite = "";
    }
    public void setFileName(String fname){
        this.fileName = fname;
    }
    public void addContentToHtml(String str) {
        strb.append(str).append("\n");
    }

    public void cleanContent(){
        strb.replace(0, strb.length(), "");
    }

    public Comm.RetrunCode commitWrite(String appendTxt) {
        contentToBeWrite = strb.toString();
        PrintWriter printWriter = null;
        try {
            //Storage_base + System.getProperty("file.separator") + myUser.getUser().getPathAlias() + System.getProperty("file.separator")
            printWriter = new PrintWriter(new File(Comm.STORAGE_BASE + System.getProperty("file.separator") + fileName+"_"+appendTxt+"."+"html"));
            printWriter.write(contentToBeWrite);
            return Comm.RetrunCode.SUCCESS;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HtmlReportGenerator.class.getName()).log(Level.SEVERE, null, ex);
            return Comm.RetrunCode.FILE_NOT_FOUND;
        } finally {
            printWriter.close();
            return Comm.RetrunCode.SUCCESS;
        }
    }

    public Comm.RetrunCode genHtmlByPhotoList(String userID, ArrayList<PhotoWrapper> selectedPhoto, Comm.PhotoSize size, boolean hyperlink) throws FlickrException {
        this.cleanContent();
        this.setFileName(userID);
        for(PhotoWrapper pw : selectedPhoto){
            Photo p = pw.getPhoto();
            String photoStaticUrl = p.getMedium640Url(); //default value
            if(size == Comm.PhotoSize.Medium_500){photoStaticUrl = p.getMediumUrl();}
            if(size == Comm.PhotoSize.Medium_640){photoStaticUrl = p.getMedium640Url();}
            if(size == Comm.PhotoSize.Medium_800){photoStaticUrl = p.getMedium800Url();}
            if(size == Comm.PhotoSize.Large){photoStaticUrl = p.getLargeUrl();}
            if(size == Comm.PhotoSize.Original){photoStaticUrl = p.getOriginalUrl();}
            this.addContentToHtml("<p>");
            if(hyperlink == true){
                this.addContentToHtml("<a href=\""+p.getUrl()+"\">");
            }
            this.addContentToHtml("<img src=\""+photoStaticUrl +"\"></img>");
            if(hyperlink == true){
                this.addContentToHtml("</a>");
            }
            this.addContentToHtml("</p>");
            this.addContentToHtml("<p></p>");
        }
        return this.commitWrite(size.toString());
    }

    public Comm.RetrunCode genHtmlByPhotoList(String userID, final ArrayList<PhotoWrapper> selectedPhoto, Comm.PhotoSize size,
            boolean title, boolean description, boolean camera, boolean lens, boolean aperture, boolean exposure, boolean focalLength, boolean iso, boolean hyperlink, boolean bHighRes, final PhotosInterface ps) throws FlickrException {        
        this.cleanContent();
        this.setFileName(userID);
        double retinaWidth = 0;


        //如果需要HighResolution的圖的話，會需要發request給flickr去取得實際照片的size，再用大size的圖去縮小，會影響performance
        if(bHighRes){
           _getSelectedPhotoSize(selectedPhoto, ps);
        }


        for(PhotoWrapper pw : selectedPhoto){
            Photo p = pw.getPhoto();
            String photoStaticUrl = p.getMedium640Url(); //default value
            String retinaPhotoStaticUrl = p.getMedium640Url(); //default value
            if(size == Comm.PhotoSize.Medium_500){                
                photoStaticUrl = p.getMediumUrl();
                if(bHighRes){
                    retinaPhotoStaticUrl = p.getLargeUrl();
                    retinaWidth = p.getLargeSize().getWidth() / 2;
                }
            }
            if(size == Comm.PhotoSize.Medium_640){
                photoStaticUrl = p.getMedium640Url();
                if(bHighRes){
                    retinaPhotoStaticUrl = p.getLargeUrl();
                    retinaWidth = p.getLargeSize().getWidth() / 2;
                }
            }
            if(size == Comm.PhotoSize.Medium_800){
                photoStaticUrl = p.getMedium800Url();
                if(bHighRes){
                    retinaPhotoStaticUrl = p.getLarge1600Url();
                    retinaWidth = p.getLarge1600Size().getWidth() / 2;
                }
            }
            if(size == Comm.PhotoSize.Large){
                photoStaticUrl = p.getLargeUrl();
                if(bHighRes){
                    retinaPhotoStaticUrl = photoStaticUrl;
                    retinaWidth = p.getLargeSize().getWidth();
                }
            }
            if(size == Comm.PhotoSize.Large_1600){
                photoStaticUrl = p.getLarge1600Url();
                if(bHighRes){
                    retinaPhotoStaticUrl = photoStaticUrl;
                    retinaWidth = p.getLarge1600Size().getWidth();
                }
            }
            if(size == Comm.PhotoSize.Original){
                photoStaticUrl = p.getOriginalUrl();
                if(bHighRes){
                    retinaPhotoStaticUrl = photoStaticUrl;
                    retinaWidth = p.getOriginalSize().getWidth();
                }
            }

            this.addContentToHtml("<p>");
            if(hyperlink == true){
                this.addContentToHtml("<a href=\""+p.getUrl()+"\">");
            }
            //support retina display
            if(bHighRes){
                this.addContentToHtml("<script>");
                this.addContentToHtml("var retina = window.devicePixelRatio > 1;");
                this.addContentToHtml("if (retina) {");
                this.addContentToHtml("document.write(\"<img src="+retinaPhotoStaticUrl +" width = "+ retinaWidth +" alt="+p.getTitle()+" title="+p.getTitle()+"></img>\");");
                this.addContentToHtml("}");
                this.addContentToHtml("else{");
                this.addContentToHtml("document.write(\"<img src="+photoStaticUrl +" alt="+p.getTitle()+" title="+p.getTitle()+"></img>\");");
                this.addContentToHtml("}");
                this.addContentToHtml("</script>");
            }else{
                this.addContentToHtml("<img src=\""+photoStaticUrl +"\" alt=\""+p.getTitle()+"\" title=\""+p.getTitle()+"\"></img>");
            }
            if(hyperlink == true){
                this.addContentToHtml("</a>");
            }
            this.addContentToHtml("<br>");
            if(title && !p.getTitle().equalsIgnoreCase("")){this.addContentToHtml(p.getTitle()+"<br>");}
            if(description && !(p.getDescription()==null) && !p.getDescription().equalsIgnoreCase("")){this.addContentToHtml(p.getDescription()+"<br>");}
            if(camera){this.addContentToHtml("相機型號: "+pw.getMeta().getExifDataWrapper().getCameraModel()+"<br>");}
            if(lens){this.addContentToHtml("鏡頭資料: "+pw.getMeta().getExifDataWrapper().getLensModel()+"<br>");}
            if(aperture){this.addContentToHtml("光圈值: "+pw.getMeta().getExifDataWrapper().getAperture()+"<br>");}
            if(exposure){this.addContentToHtml("曝光時間: "+ pw.getMeta().getExifDataWrapper().getExposure()+"<br>");}
            if(focalLength){this.addContentToHtml("實際焦長: "+pw.getMeta().getExifDataWrapper().getFocalLength()+"<br>");}
            if(iso){this.addContentToHtml("ISO值: " + pw.getMeta().getExifDataWrapper().getIsoSpeed()+"<br>");}
            this.addContentToHtml("</p>");
            this.addContentToHtml("<p></p>");
        }
        return this.commitWrite(size.toString());
    }

    public String getHtmlSourceCode() {
        return this.contentToBeWrite;
    }

    private void _getSelectedPhotoSize(final ArrayList<PhotoWrapper> selectedPhoto, final PhotosInterface ps) {                         
        ExecutorService service = Executors.newFixedThreadPool(20);
            for(PhotoWrapper pw : selectedPhoto){
                final Photo p = pw.getPhoto();
                Runnable run = new Runnable() {
                    @Override
                    public void run() {
                        Collection<Size> sizes = null;
                        try {
                            sizes = ps.getSizes(p.getId(), true);
                        } catch (FlickrException ex) {
                        }
                        p.setSizes(sizes);
                    }
                };
                service.execute(run);
            }
            service.shutdown();
            try {
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
            }
    }

}