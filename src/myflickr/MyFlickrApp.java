/*
 * MyFlickrApp.java
 *
 * Known Issues:
 * - When set bExif to false, NullPointerException is occured.  (fixed)
 * - Lens description of Exif of Canon is "Lens Model" which can not be parsed. (fixed)
 * - Sorting is incorrect (fixed, only shutter still has this issue) (fixed)
 * - Iso value = "Hi 6400", it will be converted to 0 due to atoi (fixed in 1.0.1006)
 * - Sorting table when exif data is not finished, null pointer exception will be thrown.
 *  - When tag is added, the data won't be updated in real time (fixed)
 *  - tag list can't reflect the latest data when tag is totally removed
 *
 * Feature Requests
 * - Use multi-thread to get photo list and exif data
 * - Export to excel format (done in 1.0.1007)
 * - Show photo set and download photos by photo sets. (_buildPhotoSetList())
 *
 * Reversion history
 * 1.0.1001 Initial version
 * 1.0.1002 add atoi, atof functin, minor UI chnages.
 * 1.0.1003 add statistic data function, minor UI changes.
 * 1.0.1004 add search by exif function, add timeout handling  2013/6/12
 * 1.0.1005 enhance photo display experience, use another thread to display photo even exif data is still under parsing  2013/6/13
 * 1.0.1006 add a new button to renew photo list manually, but not controlled by thread in 1005, minor UI changes.
 * 1.0.1007 Let user can input the desired photo count to be get. Add ExifComparator to sort the list in numeric way. Add ExcelExporter
 * 1.0.1008 Use PhotoWrapper to replace the access of Photo object. Add to get photo set feature. UI Changes. Add a feature to generate html code
 * 1.0.1009 Add tag editor function
 * 1.0.1010 Add tag search function, remove tag editor due to conflict with the desing concept (no need to login), UI changes
 * 1.0.1011 Add photo download function
 * 1.0.1012 Add real time refresh photo list table by click jlist, bug fix in display photo count
 * 1.0.1013 Add log mechanism by log4j
 * 1.0.1014 Fix use arrow key to select photo display the incorrect photo issue
 * 1.0.1015 Minor UI changes, first release for beta, 2013/7/1
 * 1.0.1016 Fix user-input value for photo count to parse issue, when input value >500, it automatically goes to 1000, add retry while invoke flickr api error
 * 1.0.1017 Complete tag management function
 * 1.0.1018 Fix an issue of "user input count for list photo", the parameter is incorrect which is a side effect of 1.0.1016 2013/8/6
 *          Fix defect of Tag Management
 * 1.0.1019 Minor UI changes, add a function to display exif by selected photo
 *          Move MouseClicked event to MousePressed for better user experience
 * 1.0.1020 Fix progress display issue caused by multithread. add new checking method to avoid get the old data
 *          Fix an issue of while getting exif, the reload function is incorrect.
 *          Fix an issue when remove the entity in batch list
 *
 * 1.0.1021 Fix an issue "If click exif search twice, then click reset, it reset to the last search result but not correct original photo list
 *
 * 1.0.1022 Add multithread function to get exif data, minor UI changes, remove option for exif(default to off)
 * 1.0.1023 Add thread checking mechanism, to protect two theads to update same photo list at the same time (Thread.isAlive())
 * 1.0.1024 Add download funtion in menu, that can download any photo by selected list, add debug console frame. fix display issue when connection time out.
 * 1.0.1025 Fix possible hang issue when parsing exif (hang in Thread.Sleep while query engine status)
 *          Add internal function to update user's tag list when add/update tag
 *          Use Task and TaskMonitor to update progress bar (only init user)
 *          Add normalize function to make 35.0 mm f1.4 to 35mm f1.4 (release)
 * 1.0.1026 add "select all" in pop-up menu
 *          add a check method to avoid the alread parsed exif data will not lost when reload the album
 *          use multithread to download photo
 *          Download the mid size insteand of original size to avoid privacy issue
 *          Add internal config to allow user to download original size of photo (see MyFlickrApp.properties) (release)

 * 1.0.1027 Refactor authorize process, use AuthManager to handle this.
 * 1.0.1028 Avaliable to download multiple photoset at the same time
 *
 *
 * 1.1.1001 Add list favorite photo function
 *          Display the first photo as the photoset cover
 *
 * 1.1.1002 User path_alias name to replace user name when download (release 2013/9/6)
 *          fix select all bug
 *          Add icon
 * 1.1.1003 fix html report missing ">" issue, fix incorrect exif display when exif parsing is done
 *
 * 1.1.1004 add group list function (only the first 500)
 *          add contact list function
 *          add userwrapper pool to reuse the existed user
 ** Reversion to 1.2
 * 1.2.1001 Modify download photo behavior, has a boolean value to determinie to download the largest photo and skip orignal size or not
 *          UI modify, to fit Mac OS X behavior (not change height of tab automatically)

 * 1.2.1002 Wrap jar to app for Mac
 *          Change default download dir to user.home instead of current working path
 *          set log path to user.home
 ** Revision to 1.2.1
 * 1.2.1003 Fix Mac's auth files location
 *          let user choose download size
 *          use HTTPS as REST endpoints (flickr's policy)
 * 1.2.1003 Upgrade Flickr4Java to 2.8 (from 2.5), need to patch REST.java to fix non-auth API return invalid API key issue
 * https://github.com/vermgit/Flickr4Java/commit/17eb40e0befea63252a810d2c2706f0eb47882aa
 *
 * 1.2.1004 Implement Photo Upload, 尚未完成
 * 1.2.2 加入上傳照片的icon, 完成上傳照片基本功能 (tile, description, tag, photoset) (2014/06/03)
 * 1.3.0 加入自訂下載路徑，表格加入日期，加入是否連回flickr的選項
 *       加入顯示縮圖功能 (2014/09/22)
 *       加入自動檢查更新 (2014/09/22)
 *       加入ConfigManager (2014/09/24)
 *       修正最愛照片顯示順序錯誤的問題 (2014/09/25)
 *       加入隱私照片認証功能  (2014/09/25)
 * 
 * 1.3.1 支援Retina display 貼圖
 *       HTML加入alt與title attribute
 *       加入建立相簿功能
 *       Upgrade Flickr4Java to v2.12
 *       新增SSLTool, call SSLTool.disableCertificateValidation to ignore ssl handshake issue exception 
            ref: https://www.flickr.com/groups/api/discuss/72157657943677126
 *          ref: http://stackoverflow.com/questions/9619030/resolving-javax-net-ssl-sslhandshakeexception-sun-security-validator-validatore
 *          ref: http://stackoverflow.com/questions/6047996/ignore-self-signed-ssl-cert-using-jersey-client
 *
 * 1.3.2 修復https認証不通過的問題
 *       增加是否要自動選擇更新的選項
 *
 * Known Issues
 * - For unkonwn reason, use multithread to add/set tag, always need to set auth for write permission again. (workaround)
 * - When set/add new tags, the Tag search function could not reflect the latest changes, that's a flickr API's limitation(known issue)
 * - When a thread for exif fetching is running, if another thread for exif fetching is triggered, it will cause incorrect message to display  (known issue)
 * - busy icon timer 在多個function中都有用到，有可能會因為a function 和 b function 都啟動了 busy icon, 但在 a fucntion中先被停止，造成 b function尚未結束，但busy icon
 *      卻已結束
 * - 在媒些情況下，一按取得exif，整個app 會hang住 (workaround)
 * - 可同時取得exif資訊和下載照片，只是顯示會不正確
 * - 當照片先被加到上傳batch中，但是在上傳之前原始照片已經被移除掉，會產生錯誤
 * - 某些人的username 無法透過flickr api 找到 user id，原因不明
 * - 使用嘸蝦米輸入法在使用者id的欄位會造成 Java Application Framework crash
 *
 * ToDo
 * - 加入 Multi-Thead 去更新exif (done)
 * - 使用 Task 來更新 progress bar (見 startMyTaskAction() )
 * - call with sign 的方法：要讓 requestToken 出現 (done)
 * - 加入Date在表格中 (done)
 * - 加入取消超連結的選項 (done)
 * - 上傳照片增加照片集功能
 * - 設定下載位置 (done)
 * - 增加日期欄位 (done)
 * - 使用title而非id當做預設檔名 (done)
 *
 * Defects List
 * 在某些情況下，Mac版會無法執行
 * 若沒有原始照片，取得html code會出現 產生 html code 錯誤  (user experience不佳)
 *
 *
 */

package myflickr;


import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class MyFlickrApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        show(new MyFlickrView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MyFlickrApp
     */
    public static MyFlickrApp getApplication() {
        return Application.getInstance(MyFlickrApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(MyFlickrApp.class, args);
    }
}
