/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

import com.flickr4java.flickr.*;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.util.FileAuthStore;
import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import javax.swing.JOptionPane;
import myflickr.util.MyFlickrLogManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.xml.sax.SAXException;

/**
 *
 * @author neil
 */
public class AuthManager {
    private RequestContext rc;
    private Logger logger;
    private Auth auth;
    private String authLocation;
    private FileAuthStore authStore;
    private AuthInterface authInterface;
    //private Token requestToken;
    //private Token accessToken;
    private UserWrapper userWrapper;
    private Permission requestedPermission = Permission.WRITE;

    public AuthManager(UserWrapper u, AuthInterface aif){
        logger = MyFlickrLogManager.getLogger();
        rc = RequestContext.getRequestContext();
        authInterface = aif;
        userWrapper = u;
        //this.authLocation = System.getProperty("user.dir");
        this.authLocation = Comm.STORAGE_BASE;
    }

//    //Step1: whole process for oAuth
//    public Comm.RetrunCode authorizeGui() throws IOException, SAXException, FlickrException, URISyntaxException {
//        rc = RequestContext.getRequestContext();
//        Token requestToken;
//        String tokenKey = "0000000000";
//        //check if already authorized.
//        if(! (this.getLocalAuth()==null))
//        {
//            logger.log(Level.INFO, "Get local auth successfully");
//            rc.setAuth(auth);
//            return Comm.RetrunCode.SUCCESS;
//        }else{
//            logger.log(Level.INFO, "Start to do authorication process");
//            if (this.authLocation != null) {
//                this.authStore = new FileAuthStore(new File(this.authLocation));
//            }
//            //can make this token private?
//            //step one
//            requestToken = authInterface.getRequestToken();
//
//            //step two
//            String url = this._getAuthUrl(requestedPermission, requestToken);
//            logger.log(Level.DEBUG, "Follow this URL to authorise yourself on Flickr: ["+url+"]");
//            Desktop.getDesktop().browse(new URI(url));
//            /*
//            System.out.println("Follow this URL to authorise yourself on Flickr");
//            System.out.println(url);
//            System.out.println("Paste in the token it gives you:");
//            System.out.print(">>");
//             */
//
//            //step three
//            //String tokenKey = new Scanner(System.in).nextLine();
//            tokenKey = JOptionPane.showInputDialog(null, "請輸入認證碼:");
//            return this._doTradeAccessToken(tokenKey, requestToken);
//        }        
//    }
    
    //Step1: whole process for oAuth
    public Comm.RetrunCode authorizeGuiEx(Permission p) throws IOException, SAXException, FlickrException, URISyntaxException {
        this.requestedPermission = p;
        rc = RequestContext.getRequestContext();
        Token requestToken;
        String tokenKey = "0000000000";
        //check if already authorized.
        if(! (this.getLocalAuth()==null) && auth.getPermission().getType()>=p.getType())
        {
            logger.log(Level.INFO, "Get local auth successfully");
            rc.setAuth(auth);
            return Comm.RetrunCode.SUCCESS;
        }else{
            logger.log(Level.INFO, "Start to do authorication process");
            if (this.authLocation != null) {
                this.authStore = new FileAuthStore(new File(this.authLocation));
            }
            //can make this token private?
            //step one
            requestToken = authInterface.getRequestToken();

            //step two
            String url = this._getAuthUrl(requestedPermission, requestToken);
            logger.log(Level.DEBUG, "Follow this URL to authorise yourself on Flickr: ["+url+"]");
            Desktop.getDesktop().browse(new URI(url));
            /*
            System.out.println("Follow this URL to authorise yourself on Flickr");
            System.out.println(url);
            System.out.println("Paste in the token it gives you:");
            System.out.print(">>");
             */

            //step three
            //String tokenKey = new Scanner(System.in).nextLine();
            tokenKey = JOptionPane.showInputDialog(null, "請輸入認證碼:");
            return this._doTradeAccessToken(tokenKey, requestToken);
        }        
    }

     //Step2 Get the auth URL, this is step two of authorization.
    private String _getAuthUrl(Permission p, Token token) throws FlickrException{
        String url = authInterface.getAuthorizationUrl(token, p);
        return url;
    }

    //Step 3: Trade the request token for an access token, this is step three of authorization.
    private Comm.RetrunCode _doTradeAccessToken(String code, Token requestToken) throws FlickrException, IOException{        
        Token accessToken;
        if(code==null || code.equalsIgnoreCase("") || code.length() ==0){return Comm.RetrunCode.ERROR;}
        logger.log(Level.INFO, "開始認證程序");
        //AuthInterface authInterface = flickr.getAuthInterface();
        //String tokenKey = new Scanner(System.in).nextLine();
        String tokenKey = code;
        accessToken = authInterface.getAccessToken(requestToken, new Verifier(tokenKey));
        if(accessToken == null){
            logger.log(Level.INFO, "認證碼輸入錯誤");
            return Comm.RetrunCode.ERROR;
        }
        auth = authInterface.checkToken(accessToken);
        if(auth == null || auth.getPermission().getType() < requestedPermission.getType()){
            logger.log(Level.INFO, "認證錯誤");
            return Comm.RetrunCode.ERROR;
        }
        else{
            logger.log(Level.INFO, "認證成功，權限 = ["+auth.getPermission()+"]");
            rc.setAuth(auth);
            authStore.store(auth);
            logger.log(Level.INFO, "儲存認證檔案:" + auth.getUser().getId());    
            //check if current auth does not match to saved auth            
            if(getLocalAuth()==null){
                logger.log(Level.INFO, "認證檢查錯誤，請確定您輸入的是本人帳號");
                return Comm.RetrunCode.AUTH_NOT_MATCHED;    
            }
            return Comm.RetrunCode.SUCCESS;
        }
    }


    //Verify local auth status
    private Auth getLocalAuth() throws FlickrException{
        this.authStore = new FileAuthStore(new File(this.authLocation));
        if (this.authStore != null) {
            logger.log(Level.DEBUG, "Get local permission information from path "+ this.authLocation);
            auth = this.authStore.retrieve(this.userWrapper.getUser().getId());
        }
        if (auth !=null){logger.log(Level.DEBUG, "Current permission = "+auth.getPermission());}
        else{logger.log(Level.DEBUG, "Can not get local permission information: "+ this.userWrapper.getUser().getId());}
        return auth;
    }

    public void setAuthLocation(String path) {
        this.authLocation = path;
    }
    public Permission getPermissionType(){
        if(auth!=null){
            return auth.getPermission();
        }else{
            return Permission.NONE;
        }
    }
}
