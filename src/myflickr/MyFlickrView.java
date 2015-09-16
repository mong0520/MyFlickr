/*
 * MyFlickrView.java
 */

package myflickr;

import myflickr.core.Comm.Tag_Method;
import com.flickr4java.flickr.uploader.UploadMetaData;
import org.apache.log4j.*;
import com.flickr4java.flickr.*;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.contacts.Contact;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.groups.*;
import com.flickr4java.flickr.tags.Tag;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.DefaultCaret;
import myflickr.core.*;
import myflickr.core.UserWrapper;
import myflickr.util.*;
import org.jfree.ui.RefineryUtilities;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.Task;
import org.jdesktop.application.TaskService;
import org.xml.sax.SAXException;
import java.io.*;
import java.util.HashMap;
import java.util.Vector;


/**
 * The application's main frame.
 */
public class MyFlickrView extends FrameView {
    private final int MAX_WIDTH = 880;    
    private ArrayList<JComponent> myComponents;
    private ArrayList<PhotoWrapper> batchPhotoList = new ArrayList<PhotoWrapper>();  //used in Tag function, to store the batch list
    //private ArrayList<PhotoWrapper> dataInTable = new ArrayList<PhotoWrapper>();
    private MyFlickrController myController ;
    private UserWrapper myUser;    
    private Desktop myDesktop ;
    private PieChartFrame pieChart_Camera;
    private PieChartFrame pieChart_LensID;
    private PieChartFrame pieChart_FocolLength;
    private PieChartFrame pieChart_ShutterSpeed;
    private PieChartFrame pieChart_ApertureValue;
    private PieChartFrame pieChart_IsoSpeed;
    private StatisticData stat;
    private FilterSpec filterSpec;
    private FilterMananger filterManager;
    private DownloadManager downloadManager;
    private Logger logger;
    private File Storage_base;
    // For Exif Search using
    private boolean OrigianlPhotoListIsBackupable = true;
    private File defaultDownloadPath;
    private File currentDownloadPath;    
    //private File configFile = new File(Comm.STORAGE_BASE + System.getProperty("file.separator") + Comm.CONFIG_FILENAME);

    private Task taskInitUser;

    Thread progress1;
    Thread progress2;
    Thread myPhotoThread;
    Thread myTagThread;
    Thread myDownloadThread;
    ExifWorker exifWorker;
    DownloadWorker downloadWorker;
    BuildMyPhotoTask myPhotoTask;
    UpdateManager updateManager;

    ResourceMap resourceMap = getResourceMap();
    //private StringBuilder tempMsgBuffer;
    JTable activeTable;  //the active table is photo table or favorite table
    DropTarget dtPhotoUpload;
    java.util.Properties properties;
    ConfigManager configManager;

    public MyFlickrView(final SingleFrameApplication app) {
        super(app);

        URL url = ClassLoader.getSystemResource("myflickr/resources/flickr_icon.png");
        Toolkit kit = Toolkit.getDefaultToolkit();
        Image img = kit.createImage(url);
        this.getFrame().setIconImage(img);

        //this.getFrame().setResizable(true);
        this.getFrame().setPreferredSize(new Dimension(MAX_WIDTH,700));

        this.getFrame().addComponentListener(new ComponentAdapter(){
            @Override            
            public void componentResized(ComponentEvent e) {
                getFrame().setSize(new Dimension(MAX_WIDTH, getFrame().getHeight()));
                super.componentResized(e);
            }
        });
        initComponents();
        //panel_Auth.setVisible(false);
        
        //init logger
        logger = MyFlickrLogManager.getLogger();
        logger.log(Level.INFO, "Starting Flickr小幫手");
        
        //init storage base folder
        Storage_base = new File(Comm.STORAGE_BASE);        
        if(!Storage_base.exists()){
            if(Storage_base.mkdirs()){
                logger.log(Level.DEBUG, "Create "+Storage_base+" success!");
            }else{
                logger.log(Level.DEBUG, "Create "+Storage_base+" failed!");
                JOptionPane.showMessageDialog(null, "建立工作目錄 "+Storage_base+" 失敗!");
            }
        }else{
            logger.log(Level.INFO, "Check "+Storage_base +" success");
        }   
        
        // My componemtns        
        myController= new MyFlickrController();
        myDesktop = Desktop.getDesktop();
        myComponents = new ArrayList<JComponent>();
        downloadManager = new DownloadManager();
        filterManager = new FilterMananger();
        updateManager = new UpdateManager();        
        updateManager.setCurrentVersion(resourceMap.getString("Application.version"));        
        defaultDownloadPath = new File(Comm.STORAGE_BASE);        
        
        //load config and deploy config
        configManager = new ConfigManager();
        configManager.setConfigPath(Comm.STORAGE_BASE);
        configManager.loadConfig();              
        
        //username        
        txt_username.setText(configManager.getValue(ConfigManager.CONF_USERNAME, ""));    
        
        //thumbnail
        if(configManager.getValue(ConfigManager.CONF_THUMBNAIL, ConfigManager.CONF_DEF_THUMBNAIL).equalsIgnoreCase("true"))        
            subSettingShowThumbnail.setSelected(true);
        else
            subSettingShowThumbnail.setSelected(false);
        
        //download size
        if(configManager.getValue(ConfigManager.CONF_DOWNLOAD_SIZE, ConfigManager.CONF_DEF_DOWNLOAD_SIZE).equalsIgnoreCase("Large"))        
        {
            subSettingFileSizeLarge.setSelected(true);                    
        }
        else if(configManager.getValue(ConfigManager.CONF_DOWNLOAD_SIZE, ConfigManager.CONF_DEF_DOWNLOAD_SIZE).equalsIgnoreCase("Original"))        {
            //subSettingFileSizeLarge.setSelected(false);        
            subSettingFileSizeOriginal.setSelected(true);
        }

        //auto check update
        if(configManager.getValue(ConfigManager.CONF_CHECK_UPDATE, ConfigManager.CONF_DEF_CHECK_UPDATE).equalsIgnoreCase("true"))
            subSettingAutoUpdateOn.setSelected(true);
        else
            subSettingAutoUpdateOn.setSelected(false);

        //download path
        String downloadPath = configManager.getValue(ConfigManager.CONF_DOWNLOADPATH, Storage_base.getAbsolutePath());        
        if(configManager.getValue(ConfigManager.CONF_DOWNLOADPATH, Storage_base.getAbsolutePath())!=null && !downloadPath.equalsIgnoreCase("")){
            logger.log(Level.DEBUG, "Set download path to "+ downloadPath);
            currentDownloadPath = new File(downloadPath);
        }else{
            logger.log(Level.INFO, "Set download Path to default download path");
            currentDownloadPath = defaultDownloadPath;
        }
        
       

        //check update        
        if(configManager.getValue(ConfigManager.CONF_CHECK_UPDATE, "true").equalsIgnoreCase("true")){
            Thread updateCheck = new Thread(){
                public void run(){
                    updateManager.initUpdateStatus();
                    if(updateManager.CheckNeedToUpdate()){
                        if(JOptionPane.showConfirmDialog(null, "發現新版本"+updateManager.getLatestVersion()+"，請至官網下載\n更新內容：\n"+updateManager.getUpdateDesc(), "Oops..", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                            mnu_GoToMyBlogActionPerformed(null);
                    }
                }

            };
            updateCheck.start();
        }

        //tempMsgBuffer = new StringBuilder();
        _initComboBoxValue();
        txtArea_Tag.setLineWrap(true);
        txtArea_Tag.setWrapStyleWord(true);
        //DefaultCaret caret = (DefaultCaret)txtAera_DownloadLog.getCaret();
//        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
//
//        caret = (DefaultCaret)txtArea_DebugLog.getCaret();
//        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        txtArea_DebugLog.setEditable(false);
        TextAreaAppender.setTextArea(this.txtArea_DebugLog);
        
        myPhotoThread = new Thread();
        myTagThread = new Thread();
        myDownloadThread = new Thread();
        exifWorker = new ExifWorker();        
        downloadWorker = new DownloadWorker();

        //init properties
        properties = new java.util.Properties();

     
        

        //caret = (DefaultCaret)txtAera_TagLog.getCaret();
        //caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        //panel_Auth.setEnabled(false);
        //panel_Auth.setVisible(false);
        this._registComponents(btn_initUser);
        this._registComponents(btn_Start);
        this._registComponents(txt_username);
        this._registComponents(btn_Export);
        this._registComponents(btn_TagSet);

        dtPhotoUpload = new DropTarget(lbl_UploadArea, new DropTargetAdapter(list_UploadList));
        

        // end of my conponents
        // status bar initialization - message timeout, idle icon and busy animation, etc
        //ResourceMap resourceMap = getResourceMap();

        //enable internal config
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(true);        

        //initial tabbed pannel
        //this.tabbed_MainPanel.setSelectedIndex(0);
        int selectedTabIndex = this.tabbed_MainPanel.getSelectedIndex();
        if(selectedTabIndex == 0){activeTable = this.tb_PhotoList;}
        if(selectedTabIndex == 1){activeTable = this.tb_favPhoto;}
        if(selectedTabIndex == 2){activeTable = this.tb_groupPhoto;}

        //set cell border and color
//        CustomRenderer cr = new CustomRenderer(this.tb_favPhoto.getDefaultRenderer(Object.class), Color.GRAY);        
//        this.tb_favPhoto.setDefaultRenderer(Object.class, cr);
//        this.tb_favPhoto.setDefaultRenderer(Integer.class, cr);
//        this.tb_favPhoto.setDefaultRenderer(Float.class, cr);
//        this.tb_favPhoto.setDefaultRenderer(ImageIcon.class, cr);        
        
        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                //logger.log(Level.DEBUG, "Current status = "+propertyName);
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    JLabel test ;

                    progressBar.setIndeterminate(false);
                    //progressBar.setVisible(true);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    //statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    logger.log(Level.DEBUG, "progress value = "+value);
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = MyFlickrApp.getApplication().getMainFrame();
            aboutBox = new MyFlickrAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        MyFlickrApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        tabbed_MainPanel = new javax.swing.JTabbedPane();
        scPane_photoList = new javax.swing.JScrollPane();
        tb_PhotoList = new javax.swing.JTable();
        scPane_favoPhotoList = new javax.swing.JScrollPane();
        tb_favPhoto = new javax.swing.JTable();
        scPane_groupPhotoList = new javax.swing.JScrollPane();
        tb_groupPhoto = new javax.swing.JTable();
        txt_username = new javax.swing.JTextField();
        tab_FunctionPannel = new javax.swing.JTabbedPane();
        panel_PhotoCount = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        combo_RangeToList = new javax.swing.JComboBox();
        txt_PhotoCount = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        btn_Start = new javax.swing.JButton();
        ck_private = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        btn_Export = new javax.swing.JButton();
        btn_reload = new javax.swing.JButton();
        btn_Start1 = new javax.swing.JButton();
        panel_Backup = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane11 = new javax.swing.JScrollPane();
        list_photoset = new javax.swing.JList();
        btn_backup = new javax.swing.JButton();
        btn_backupSelectAll = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane9 = new javax.swing.JScrollPane();
        txtArea_PhotoDownloadLog = new javax.swing.JTextArea();
        panel_EditTag = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        btn_TagSet = new javax.swing.JButton();
        jScrollPane13 = new javax.swing.JScrollPane();
        txtArea_Tag = new javax.swing.JTextArea();
        btn_TagAdd = new javax.swing.JButton();
        jLabel20 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        btn_AddToBatch = new javax.swing.JButton();
        btn_RemoveFromBatch = new javax.swing.JButton();
        btn_RemoveAll = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        lbl_batchCount = new javax.swing.JLabel();
        btn_DisplayOriginal = new javax.swing.JButton();
        tgBtn_displayTempList = new javax.swing.JToggleButton();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane10 = new javax.swing.JScrollPane();
        list_TagList = new javax.swing.JList();
        btn_searchByTag = new javax.swing.JButton();
        panel_HtmlGen = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        ck_photoTitle = new javax.swing.JCheckBox();
        ck_phtoDescription = new javax.swing.JCheckBox();
        ck_CameraModel = new javax.swing.JCheckBox();
        ck_LensId = new javax.swing.JCheckBox();
        ck_Aperture = new javax.swing.JCheckBox();
        ck_Exposure = new javax.swing.JCheckBox();
        ck_FocalLength = new javax.swing.JCheckBox();
        ck_Iso = new javax.swing.JCheckBox();
        jScrollPane8 = new javax.swing.JScrollPane();
        txt_HtmlCode = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        combo_SizeSelection = new javax.swing.JComboBox();
        btn_HtmlCodeGen = new javax.swing.JButton();
        btn_SelectAll1 = new javax.swing.JButton();
        ck_HighResDisplay = new javax.swing.JCheckBox();
        panel_StatisticData = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        btn_ShowStasticChart = new javax.swing.JButton();
        comboList = new javax.swing.JComboBox();
        jLabel18 = new javax.swing.JLabel();
        btn_SelectAll = new javax.swing.JButton();
        panel_SearchByExif = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_Camera = new javax.swing.JList();
        jScrollPane3 = new javax.swing.JScrollPane();
        list_Aperture = new javax.swing.JList();
        jScrollPane4 = new javax.swing.JScrollPane();
        list_Lens = new javax.swing.JList();
        jScrollPane6 = new javax.swing.JScrollPane();
        list_Focal = new javax.swing.JList();
        jScrollPane7 = new javax.swing.JScrollPane();
        list_Exposure = new javax.swing.JList();
        btn_FilterQuery = new javax.swing.JButton();
        btn_FilterReset = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        list_Iso = new javax.swing.JList();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        panel_Upload = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_UploadList = new javax.swing.JList();
        btn_Clear = new javax.swing.JButton();
        btn_Upload = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        cmb_PhotoSet = new javax.swing.JComboBox();
        jPanel13 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        txt_Title = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jScrollPane14 = new javax.swing.JScrollPane();
        txtArea_Description = new javax.swing.JTextArea();
        txt_Tag = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        btn_Apply = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        lbl_UploadArea = new javax.swing.JLabel();
        jLable20 = new javax.swing.JLabel();
        lbl_uploadStatus = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        buddyIcon = new javax.swing.JLabel();
        btn_initUser = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        lbl_photo = new javax.swing.JLabel();
        lbl_icon = new javax.swing.JLabel();
        javax.swing.JSeparator statusPanelSeparator1 = new javax.swing.JSeparator();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        dirMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        contactMenu = new javax.swing.JMenu();
        groupMenu = new javax.swing.JMenu();
        settingMenu = new javax.swing.JMenu();
        subSettingMenu_Size = new javax.swing.JMenu();
        subSettingFileSizeLarge = new javax.swing.JRadioButtonMenuItem();
        subSettingFileSizeOriginal = new javax.swing.JRadioButtonMenuItem();
        subSettingMenu_Thumbnail = new javax.swing.JMenu();
        subSettingShowThumbnail = new javax.swing.JRadioButtonMenuItem();
        subSettingMenu_AutoUpdate = new javax.swing.JMenu();
        subSettingAutoUpdateOn = new javax.swing.JRadioButtonMenuItem();
        subSettingMenu_Location = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        debugMenuItm = new javax.swing.JMenuItem();
        mnu_GoToMyBlog = new javax.swing.JMenuItem();
        mnu_checkUpdate = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        lbl_PhotoListStatus = new javax.swing.JLabel();
        mnu_popMenu = new javax.swing.JPopupMenu();
        mnu_SelectAll = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        mnu_refresh = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        mnu_loadThumbnail = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        mnu_GetExtraData = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        mnu_DownloadPhoto = new javax.swing.JMenuItem();
        rdoGrp_Exif = new javax.swing.ButtonGroup();
        Frame_Debug = new javax.swing.JFrame();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane12 = new javax.swing.JScrollPane();
        txtArea_DebugLog = new javax.swing.JTextArea();
        btn_clear = new javax.swing.JButton();
        rdoGrp_DownloadSize = new javax.swing.ButtonGroup();

        mainPanel.setMaximumSize(new java.awt.Dimension(800, 600));
        mainPanel.setName("mainPanel"); // NOI18N

        tabbed_MainPanel.setFont(new java.awt.Font("新細明體", 0, 12));
        tabbed_MainPanel.setName("tabbed_MainPanel"); // NOI18N
        tabbed_MainPanel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabbed_MainPanelStateChanged(evt);
            }
        });

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(myflickr.MyFlickrApp.class).getContext().getResourceMap(MyFlickrView.class);
        scPane_photoList.setFont(resourceMap.getFont("scPane_photoList.font")); // NOI18N
        scPane_photoList.setName("scPane_photoList"); // NOI18N

        tb_PhotoList.setAutoCreateRowSorter(true);
        tb_PhotoList.setModel(new MyTableModel());
        tb_PhotoList.setComponentPopupMenu(mnu_popMenu);
        tb_PhotoList.setDragEnabled(true);
        tb_PhotoList.setName("tb_PhotoList"); // NOI18N
        tb_PhotoList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tb_PhotoListMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tb_PhotoListMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tb_PhotoListMouseReleased(evt);
            }
        });
        tb_PhotoList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tb_PhotoListKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tb_PhotoListKeyReleased(evt);
            }
        });
        tb_PhotoList.getColumnModel().getColumn(0).setMinWidth(0);
        tb_PhotoList.getColumnModel().getColumn(0).setPreferredWidth(0);
        tb_PhotoList.getColumnModel().getColumn(0).setMaxWidth(0);
        tb_PhotoList.getColumnModel().getColumn(1).setMinWidth(60);
        tb_PhotoList.getColumnModel().getColumn(1).setPreferredWidth(60);
        tb_PhotoList.getColumnModel().getColumn(1).setMaxWidth(60);
        scPane_photoList.setViewportView(tb_PhotoList);

        tabbed_MainPanel.addTab(resourceMap.getString("scPane_photoList.TabConstraints.tabTitle"), scPane_photoList); // NOI18N

        scPane_favoPhotoList.setName("scPane_favoPhotoList"); // NOI18N
        scPane_favoPhotoList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sdfasf(evt);
            }
        });

        tb_favPhoto.setAutoCreateRowSorter(true);
        tb_favPhoto.setBorder(new javax.swing.border.MatteBorder(null));
        tb_favPhoto.setModel(new MyTableModel());
        tb_favPhoto.setComponentPopupMenu(mnu_popMenu);
        tb_favPhoto.setName("tb_favPhoto"); // NOI18N
        tb_favPhoto.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tb_favPhotoMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tb_favPhotoMouseReleased(evt);
            }
        });
        tb_favPhoto.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tb_favPhotoKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tb_favPhotoKeyReleased(evt);
            }
        });
        tb_favPhoto.getColumnModel().getColumn(0).setMinWidth(0);
        tb_favPhoto.getColumnModel().getColumn(0).setPreferredWidth(0);
        tb_favPhoto.getColumnModel().getColumn(0).setMaxWidth(0);
        tb_favPhoto.getColumnModel().getColumn(1).setMinWidth(60);
        tb_favPhoto.getColumnModel().getColumn(1).setPreferredWidth(60);
        tb_favPhoto.getColumnModel().getColumn(1).setMaxWidth(60);
        scPane_favoPhotoList.setViewportView(tb_favPhoto);

        tabbed_MainPanel.addTab(resourceMap.getString("scPane_favoPhotoList.TabConstraints.tabTitle"), scPane_favoPhotoList); // NOI18N

        scPane_groupPhotoList.setName("scPane_groupPhotoList"); // NOI18N

        tb_groupPhoto.setAutoCreateRowSorter(true);
        tb_groupPhoto.setModel(new MyTableModel());
        tb_groupPhoto.setComponentPopupMenu(mnu_popMenu);
        tb_groupPhoto.setName("tb_groupPhoto"); // NOI18N
        tb_groupPhoto.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tb_groupPhotoMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tb_groupPhotoMouseReleased(evt);
            }
        });
        tb_groupPhoto.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tb_groupPhotoKeyReleased(evt);
            }
        });
        tb_groupPhoto.getColumnModel().getColumn(0).setMinWidth(0);
        tb_groupPhoto.getColumnModel().getColumn(0).setPreferredWidth(0);
        tb_groupPhoto.getColumnModel().getColumn(0).setMaxWidth(0);
        tb_groupPhoto.getColumnModel().getColumn(1).setMinWidth(60);
        tb_groupPhoto.getColumnModel().getColumn(1).setPreferredWidth(60);
        tb_groupPhoto.getColumnModel().getColumn(1).setMaxWidth(60);
        scPane_groupPhotoList.setViewportView(tb_groupPhoto);

        tabbed_MainPanel.addTab(resourceMap.getString("scPane_groupPhotoList.TabConstraints.tabTitle"), scPane_groupPhotoList); // NOI18N

        txt_username.setText(resourceMap.getString("txt_username.text")); // NOI18N
        txt_username.setToolTipText(resourceMap.getString("txt_username.toolTipText")); // NOI18N
        txt_username.setFocusCycleRoot(true);
        txt_username.setFocusTraversalPolicyProvider(true);
        txt_username.setName("txt_username"); // NOI18N
        txt_username.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_usernameActionPerformed(evt);
            }
        });

        tab_FunctionPannel.setName("tab_FunctionPannel"); // NOI18N

        panel_PhotoCount.setFocusCycleRoot(true);
        panel_PhotoCount.setFocusTraversalPolicyProvider(true);
        panel_PhotoCount.setName("panel_PhotoCount"); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel1.border.title"))); // NOI18N
        jPanel1.setName("jPanel1"); // NOI18N

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N

        combo_RangeToList.setName("combo_RangeToList"); // NOI18N

        txt_PhotoCount.setText(resourceMap.getString("txt_PhotoCount.text")); // NOI18N
        txt_PhotoCount.setName("txt_PhotoCount"); // NOI18N

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        jLabel12.setText(resourceMap.getString("jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N

        jLabel13.setText(resourceMap.getString("jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N

        btn_Start.setText(resourceMap.getString("btn_Start.text")); // NOI18N
        btn_Start.setName("btn_Start"); // NOI18N
        btn_Start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_StartActionPerformed(evt);
            }
        });

        ck_private.setText(resourceMap.getString("ck_private.text")); // NOI18N
        ck_private.setActionCommand(resourceMap.getString("ck_private.actionCommand")); // NOI18N
        ck_private.setName("ck_private"); // NOI18N
        ck_private.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ck_privateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btn_Start)
                    .addComponent(ck_private)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txt_PhotoCount, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(combo_RangeToList, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel9))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel12)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(combo_RangeToList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jLabel13)
                    .addComponent(txt_PhotoCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ck_private)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_Start)
                .addContainerGap(23, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel5.border.title"))); // NOI18N
        jPanel5.setName("jPanel5"); // NOI18N

        btn_Export.setIcon(resourceMap.getIcon("btn_Export.icon")); // NOI18N
        btn_Export.setToolTipText(resourceMap.getString("btn_Export.toolTipText")); // NOI18N
        btn_Export.setLabel(resourceMap.getString("btn_Export.label")); // NOI18N
        btn_Export.setName("btn_Export"); // NOI18N
        btn_Export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ExportActionPerformed(evt);
            }
        });

        btn_reload.setIcon(resourceMap.getIcon("btn_reload.icon")); // NOI18N
        btn_reload.setToolTipText(resourceMap.getString("btn_reload.toolTipText")); // NOI18N
        btn_reload.setActionCommand(resourceMap.getString("btn_reload.actionCommand")); // NOI18N
        btn_reload.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btn_reload.setLabel(resourceMap.getString("btn_reload.label")); // NOI18N
        btn_reload.setName("btn_reload"); // NOI18N
        btn_reload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_reloadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_reload, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_Export, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(64, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_Export)
                    .addComponent(btn_reload))
                .addContainerGap(93, Short.MAX_VALUE))
        );

        btn_Start1.setText(resourceMap.getString("btn_Start1.text")); // NOI18N
        btn_Start1.setName("btn_Start1"); // NOI18N
        btn_Start1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Start1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panel_PhotoCountLayout = new javax.swing.GroupLayout(panel_PhotoCount);
        panel_PhotoCount.setLayout(panel_PhotoCountLayout);
        panel_PhotoCountLayout.setHorizontalGroup(
            panel_PhotoCountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_PhotoCountLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(496, 496, 496)
                .addComponent(btn_Start1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panel_PhotoCountLayout.setVerticalGroup(
            panel_PhotoCountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel_PhotoCountLayout.createSequentialGroup()
                .addGroup(panel_PhotoCountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panel_PhotoCountLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btn_Start1))
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(69, 69, 69))
        );

        jPanel1.getAccessibleContext().setAccessibleName(resourceMap.getString("jPanel1.AccessibleContext.accessibleName")); // NOI18N

        tab_FunctionPannel.addTab(resourceMap.getString("panel_PhotoCount.TabConstraints.tabTitle"), panel_PhotoCount); // NOI18N

        panel_Backup.setName("panel_Backup"); // NOI18N

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel9.border.title"))); // NOI18N
        jPanel9.setName("jPanel9"); // NOI18N

        jScrollPane11.setName("jScrollPane11"); // NOI18N

        list_photoset.setName("list_photoset"); // NOI18N
        list_photoset.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                list_photosetMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                list_photosetMousePressed(evt);
            }
        });
        jScrollPane11.setViewportView(list_photoset);

        btn_backup.setIcon(resourceMap.getIcon("btn_backup.icon")); // NOI18N
        btn_backup.setToolTipText(resourceMap.getString("btn_backup.toolTipText")); // NOI18N
        btn_backup.setLabel(resourceMap.getString("btn_backup.label")); // NOI18N
        btn_backup.setName("btn_backup"); // NOI18N
        btn_backup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_backupActionPerformed(evt);
            }
        });

        btn_backupSelectAll.setText(resourceMap.getString("btn_backupSelectAll.text")); // NOI18N
        btn_backupSelectAll.setName("btn_backupSelectAll"); // NOI18N
        btn_backupSelectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_backupSelectAllActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane11, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_backupSelectAll, javax.swing.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE)
                    .addComponent(btn_backup, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(btn_backup)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_backupSelectAll))
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(jScrollPane11, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel8.border.border.title")))); // NOI18N
        jPanel8.setName("jPanel8"); // NOI18N

        jScrollPane9.setName("jScrollPane9"); // NOI18N

        txtArea_PhotoDownloadLog.setColumns(20);
        txtArea_PhotoDownloadLog.setRows(5);
        txtArea_PhotoDownloadLog.setName("txtArea_PhotoDownloadLog"); // NOI18N
        jScrollPane9.setViewportView(txtArea_PhotoDownloadLog);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout panel_BackupLayout = new javax.swing.GroupLayout(panel_Backup);
        panel_Backup.setLayout(panel_BackupLayout);
        panel_BackupLayout.setHorizontalGroup(
            panel_BackupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel_BackupLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panel_BackupLayout.setVerticalGroup(
            panel_BackupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_BackupLayout.createSequentialGroup()
                .addGroup(panel_BackupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(35, Short.MAX_VALUE))
        );

        tab_FunctionPannel.addTab(resourceMap.getString("panel_Backup.TabConstraints.tabTitle"), panel_Backup); // NOI18N

        panel_EditTag.setEnabled(false);
        panel_EditTag.setFocusable(false);
        panel_EditTag.setName("panel_EditTag"); // NOI18N

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel11.border.title"))); // NOI18N
        jPanel11.setName("jPanel11"); // NOI18N

        btn_TagSet.setText(resourceMap.getString("btn_TagSet.text")); // NOI18N
        btn_TagSet.setName("btn_TagSet"); // NOI18N
        btn_TagSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_TagSetActionPerformed(evt);
            }
        });

        jScrollPane13.setName("jScrollPane13"); // NOI18N

        txtArea_Tag.setColumns(20);
        txtArea_Tag.setRows(1);
        txtArea_Tag.setName("txtArea_Tag"); // NOI18N
        jScrollPane13.setViewportView(txtArea_Tag);

        btn_TagAdd.setText(resourceMap.getString("btn_TagAdd.text")); // NOI18N
        btn_TagAdd.setName("btn_TagAdd"); // NOI18N
        btn_TagAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_TagAddActionPerformed(evt);
            }
        });

        jLabel20.setText(resourceMap.getString("jLabel20.text")); // NOI18N
        jLabel20.setName("jLabel20"); // NOI18N

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(jScrollPane13, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_TagSet, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_TagAdd))
                    .addComponent(jLabel20))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_TagSet)
                        .addComponent(btn_TagAdd))
                    .addComponent(jScrollPane13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel20)
                .addContainerGap())
        );

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel10.border.title"))); // NOI18N
        jPanel10.setName("jPanel10"); // NOI18N

        btn_AddToBatch.setIcon(resourceMap.getIcon("btn_AddToBatch.icon")); // NOI18N
        btn_AddToBatch.setLabel(resourceMap.getString("btn_AddToBatch.label")); // NOI18N
        btn_AddToBatch.setName("btn_AddToBatch"); // NOI18N
        btn_AddToBatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_AddToBatchActionPerformed(evt);
            }
        });
        jPanel10.add(btn_AddToBatch);

        btn_RemoveFromBatch.setIcon(resourceMap.getIcon("btn_RemoveFromBatch.icon")); // NOI18N
        btn_RemoveFromBatch.setLabel(resourceMap.getString("btn_RemoveFromBatch.label")); // NOI18N
        btn_RemoveFromBatch.setName("btn_RemoveFromBatch"); // NOI18N
        btn_RemoveFromBatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_RemoveFromBatchActionPerformed(evt);
            }
        });
        jPanel10.add(btn_RemoveFromBatch);

        btn_RemoveAll.setIcon(resourceMap.getIcon("btn_RemoveAll.icon")); // NOI18N
        btn_RemoveAll.setText(resourceMap.getString("btn_RemoveAll.text")); // NOI18N
        btn_RemoveAll.setName("btn_RemoveAll"); // NOI18N
        btn_RemoveAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_RemoveAllActionPerformed(evt);
            }
        });
        jPanel10.add(btn_RemoveAll);

        jLabel23.setText(resourceMap.getString("jLabel23.text")); // NOI18N
        jLabel23.setName("jLabel23"); // NOI18N
        jPanel10.add(jLabel23);

        lbl_batchCount.setText(resourceMap.getString("lbl_batchCount.text")); // NOI18N
        lbl_batchCount.setName("lbl_batchCount"); // NOI18N
        jPanel10.add(lbl_batchCount);

        btn_DisplayOriginal.setLabel(resourceMap.getString("btn_DisplayOriginal.label")); // NOI18N
        btn_DisplayOriginal.setName("btn_DisplayOriginal"); // NOI18N
        btn_DisplayOriginal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_DisplayOriginalActionPerformed(evt);
            }
        });
        jPanel10.add(btn_DisplayOriginal);

        tgBtn_displayTempList.setIcon(resourceMap.getIcon("tgBtn_displayTempList.icon")); // NOI18N
        tgBtn_displayTempList.setText(resourceMap.getString("tgBtn_displayTempList.text")); // NOI18N
        tgBtn_displayTempList.setName("tgBtn_displayTempList"); // NOI18N
        tgBtn_displayTempList.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tgBtn_displayTempListStateChanged(evt);
            }
        });
        tgBtn_displayTempList.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                tgBtn_displayTempListItemStateChanged(evt);
            }
        });
        tgBtn_displayTempList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgBtn_displayTempListActionPerformed(evt);
            }
        });
        jPanel10.add(tgBtn_displayTempList);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel7.border.title"))); // NOI18N
        jPanel7.setName("jPanel7"); // NOI18N

        jScrollPane10.setName("jScrollPane10"); // NOI18N

        list_TagList.setName("list_TagList"); // NOI18N
        jScrollPane10.setViewportView(list_TagList);

        btn_searchByTag.setIcon(resourceMap.getIcon("btn_searchByTag.icon")); // NOI18N
        btn_searchByTag.setToolTipText(resourceMap.getString("btn_searchByTag.toolTipText")); // NOI18N
        btn_searchByTag.setLabel(resourceMap.getString("btn_searchByTag.label")); // NOI18N
        btn_searchByTag.setName("btn_searchByTag"); // NOI18N
        btn_searchByTag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_searchByTagActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_searchByTag)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_searchByTag)
                    .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout panel_EditTagLayout = new javax.swing.GroupLayout(panel_EditTag);
        panel_EditTag.setLayout(panel_EditTagLayout);
        panel_EditTagLayout.setHorizontalGroup(
            panel_EditTagLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_EditTagLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel_EditTagLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel10, 0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(88, Short.MAX_VALUE))
        );
        panel_EditTagLayout.setVerticalGroup(
            panel_EditTagLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_EditTagLayout.createSequentialGroup()
                .addGroup(panel_EditTagLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panel_EditTagLayout.createSequentialGroup()
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        tab_FunctionPannel.addTab(resourceMap.getString("panel_EditTag.TabConstraints.tabTitle"), panel_EditTag); // NOI18N

        panel_HtmlGen.setName("panel_HtmlGen"); // NOI18N

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel6.border.title"))); // NOI18N
        jPanel6.setName("jPanel6"); // NOI18N

        ck_photoTitle.setText(resourceMap.getString("ck_photoTitle.text")); // NOI18N
        ck_photoTitle.setName("ck_photoTitle"); // NOI18N

        ck_phtoDescription.setText(resourceMap.getString("ck_phtoDescription.text")); // NOI18N
        ck_phtoDescription.setName("ck_phtoDescription"); // NOI18N

        ck_CameraModel.setText(resourceMap.getString("ck_CameraModel.text")); // NOI18N
        ck_CameraModel.setName("ck_CameraModel"); // NOI18N

        ck_LensId.setText(resourceMap.getString("ck_LensId.text")); // NOI18N
        ck_LensId.setName("ck_LensId"); // NOI18N

        ck_Aperture.setText(resourceMap.getString("ck_Aperture.text")); // NOI18N
        ck_Aperture.setName("ck_Aperture"); // NOI18N

        ck_Exposure.setText(resourceMap.getString("ck_Exposure.text")); // NOI18N
        ck_Exposure.setName("ck_Exposure"); // NOI18N

        ck_FocalLength.setText(resourceMap.getString("ck_FocalLength.text")); // NOI18N
        ck_FocalLength.setName("ck_FocalLength"); // NOI18N

        ck_Iso.setText(resourceMap.getString("ck_Iso.text")); // NOI18N
        ck_Iso.setName("ck_Iso"); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(ck_photoTitle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ck_phtoDescription))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ck_CameraModel)
                            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(jPanel6Layout.createSequentialGroup()
                                    .addComponent(ck_FocalLength)
                                    .addGap(12, 12, 12))
                                .addComponent(ck_Aperture)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ck_Iso)
                            .addComponent(ck_Exposure)
                            .addComponent(ck_LensId))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ck_photoTitle)
                    .addComponent(ck_phtoDescription))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ck_CameraModel)
                    .addComponent(ck_LensId))
                .addGap(9, 9, 9)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ck_Aperture)
                    .addComponent(ck_Exposure))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ck_FocalLength)
                    .addComponent(ck_Iso))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane8.setName("jScrollPane8"); // NOI18N

        txt_HtmlCode.setColumns(20);
        txt_HtmlCode.setEditable(false);
        txt_HtmlCode.setRows(5);
        txt_HtmlCode.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("txt_HtmlCode.border.title"))); // NOI18N
        txt_HtmlCode.setName("txt_HtmlCode"); // NOI18N
        txt_HtmlCode.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txt_HtmlCodeMouseClicked(evt);
            }
        });
        jScrollPane8.setViewportView(txt_HtmlCode);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel3.border.title"))); // NOI18N
        jPanel3.setName("jPanel3"); // NOI18N

        combo_SizeSelection.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "500 px", "640 px", "800 px", "1024 px", "原始大小 (如果有)" }));
        combo_SizeSelection.setName("combo_SizeSelection"); // NOI18N

        btn_HtmlCodeGen.setText(resourceMap.getString("btn_HtmlCodeGen.text")); // NOI18N
        btn_HtmlCodeGen.setName("btn_HtmlCodeGen"); // NOI18N
        btn_HtmlCodeGen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_HtmlCodeGenActionPerformed(evt);
            }
        });

        btn_SelectAll1.setText(resourceMap.getString("btn_SelectAll1.text")); // NOI18N
        btn_SelectAll1.setName("btn_SelectAll1"); // NOI18N
        btn_SelectAll1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_SelectAll1ActionPerformed(evt);
            }
        });

        ck_HighResDisplay.setText(resourceMap.getString("ck_HighResDisplay.text")); // NOI18N
        ck_HighResDisplay.setEnabled(false);
        ck_HighResDisplay.setName("ck_HighResDisplay"); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(combo_SizeSelection, 0, 179, Short.MAX_VALUE)
                    .addComponent(btn_SelectAll1, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                    .addComponent(ck_HighResDisplay)
                    .addComponent(btn_HtmlCodeGen, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(combo_SizeSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_SelectAll1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_HtmlCodeGen)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(ck_HighResDisplay)
                .addGap(33, 33, 33))
        );

        javax.swing.GroupLayout panel_HtmlGenLayout = new javax.swing.GroupLayout(panel_HtmlGen);
        panel_HtmlGen.setLayout(panel_HtmlGenLayout);
        panel_HtmlGenLayout.setHorizontalGroup(
            panel_HtmlGenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_HtmlGenLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                .addContainerGap())
        );
        panel_HtmlGenLayout.setVerticalGroup(
            panel_HtmlGenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_HtmlGenLayout.createSequentialGroup()
                .addGroup(panel_HtmlGenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panel_HtmlGenLayout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jScrollPane8, 0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panel_HtmlGenLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panel_HtmlGenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel3, 0, 137, Short.MAX_VALUE)
                            .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(79, Short.MAX_VALUE))
        );

        tab_FunctionPannel.addTab(resourceMap.getString("panel_HtmlGen.TabConstraints.tabTitle"), panel_HtmlGen); // NOI18N

        panel_StatisticData.setName("panel_StatisticData"); // NOI18N

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel4.border.title"))); // NOI18N
        jPanel4.setName("jPanel4"); // NOI18N

        btn_ShowStasticChart.setText(resourceMap.getString("btn_ShowStasticChart.text")); // NOI18N
        btn_ShowStasticChart.setName("btn_ShowStasticChart"); // NOI18N
        btn_ShowStasticChart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ShowStasticChartActionPerformed(evt);
            }
        });

        comboList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "相機型號", "鏡頭ID", "焦長", "快門", "光圈值", "ISO值" }));
        comboList.setName("comboList"); // NOI18N

        jLabel18.setForeground(resourceMap.getColor("jLabel18.foreground")); // NOI18N
        jLabel18.setText(resourceMap.getString("jLabel18.text")); // NOI18N
        jLabel18.setName("jLabel18"); // NOI18N

        btn_SelectAll.setText(resourceMap.getString("btn_SelectAll.text")); // NOI18N
        btn_SelectAll.setName("btn_SelectAll"); // NOI18N
        btn_SelectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_SelectAllActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(btn_SelectAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_ShowStasticChart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(205, 205, 205))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(comboList, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addContainerGap(106, Short.MAX_VALUE))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(comboList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_SelectAll)
                    .addComponent(btn_ShowStasticChart))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel18)
                .addGap(74, 74, 74))
        );

        javax.swing.GroupLayout panel_StatisticDataLayout = new javax.swing.GroupLayout(panel_StatisticData);
        panel_StatisticData.setLayout(panel_StatisticDataLayout);
        panel_StatisticDataLayout.setHorizontalGroup(
            panel_StatisticDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_StatisticDataLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(373, Short.MAX_VALUE))
        );
        panel_StatisticDataLayout.setVerticalGroup(
            panel_StatisticDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_StatisticDataLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(83, Short.MAX_VALUE))
        );

        tab_FunctionPannel.addTab(resourceMap.getString("panel_StatisticData.TabConstraints.tabTitle"), panel_StatisticData); // NOI18N

        panel_SearchByExif.setName("panel_SearchByExif"); // NOI18N
        panel_SearchByExif.setPreferredSize(new java.awt.Dimension(800, 227));

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        list_Camera.setName("list_Camera"); // NOI18N
        list_Camera.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                list_CameraValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(list_Camera);

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        list_Aperture.setName("list_Aperture"); // NOI18N
        list_Aperture.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                list_ApertureValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(list_Aperture);

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        list_Lens.setName("list_Lens"); // NOI18N
        list_Lens.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                list_LensValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(list_Lens);

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        list_Focal.setName("list_Focal"); // NOI18N
        list_Focal.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                list_FocalValueChanged(evt);
            }
        });
        jScrollPane6.setViewportView(list_Focal);

        jScrollPane7.setName("jScrollPane7"); // NOI18N

        list_Exposure.setName("list_Exposure"); // NOI18N
        list_Exposure.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                list_ExposureValueChanged(evt);
            }
        });
        jScrollPane7.setViewportView(list_Exposure);

        btn_FilterQuery.setText(resourceMap.getString("btn_FilterQuery.text")); // NOI18N
        btn_FilterQuery.setName("btn_FilterQuery"); // NOI18N
        btn_FilterQuery.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_FilterQueryActionPerformed(evt);
            }
        });

        btn_FilterReset.setText(resourceMap.getString("btn_FilterReset.text")); // NOI18N
        btn_FilterReset.setName("btn_FilterReset"); // NOI18N
        btn_FilterReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_FilterResetActionPerformed(evt);
            }
        });

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        list_Iso.setName("list_Iso"); // NOI18N
        list_Iso.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                list_IsoValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(list_Iso);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N

        javax.swing.GroupLayout panel_SearchByExifLayout = new javax.swing.GroupLayout(panel_SearchByExif);
        panel_SearchByExif.setLayout(panel_SearchByExifLayout);
        panel_SearchByExifLayout.setHorizontalGroup(
            panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_SearchByExifLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btn_FilterQuery, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_FilterReset, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        panel_SearchByExifLayout.setVerticalGroup(
            panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_SearchByExifLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3)
                    .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(jLabel5)
                        .addComponent(jLabel6)
                        .addComponent(jLabel7)
                        .addComponent(jLabel10)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel_SearchByExifLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)
                    .addGroup(panel_SearchByExifLayout.createSequentialGroup()
                        .addComponent(btn_FilterQuery)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_FilterReset))
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE))
                .addContainerGap())
        );

        tab_FunctionPannel.addTab(resourceMap.getString("panel_SearchByExif.TabConstraints.tabTitle"), panel_SearchByExif); // NOI18N

        panel_Upload.setName("panel_Upload"); // NOI18N

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel12.border.title"))); // NOI18N
        jPanel12.setName("jPanel12"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        list_UploadList.setModel(new DefaultListModel());
        list_UploadList.setCellRenderer(new FileRenderer());
        list_UploadList.setName("list_UploadList"); // NOI18N
        list_UploadList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                list_UploadListMousePressed(evt);
            }
        });
        list_UploadList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                list_UploadListKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                list_UploadListKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(list_UploadList);

        btn_Clear.setIcon(resourceMap.getIcon("btn_Clear.icon")); // NOI18N
        btn_Clear.setText(resourceMap.getString("btn_Clear.text")); // NOI18N
        btn_Clear.setName("btn_Clear"); // NOI18N
        btn_Clear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ClearActionPerformed(evt);
            }
        });

        btn_Upload.setIcon(resourceMap.getIcon("btn_Upload.icon")); // NOI18N
        btn_Upload.setText(resourceMap.getString("btn_Upload.text")); // NOI18N
        btn_Upload.setName("btn_Upload"); // NOI18N
        btn_Upload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_UploadActionPerformed(evt);
            }
        });

        jLabel17.setText(resourceMap.getString("jLabel17.text")); // NOI18N
        jLabel17.setName("jLabel17"); // NOI18N

        cmb_PhotoSet.setName("cmb_PhotoSet"); // NOI18N
        cmb_PhotoSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmb_PhotoSetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cmb_PhotoSet, 0, 174, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addComponent(btn_Upload, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Clear))
                    .addComponent(jLabel17))
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmb_PhotoSet, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_Clear)
                    .addComponent(btn_Upload))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel13.border.title"))); // NOI18N
        jPanel13.setName("jPanel13"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        txt_Title.setText(resourceMap.getString("txt_Title.text")); // NOI18N
        txt_Title.setName("txt_Title"); // NOI18N

        jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
        jLabel14.setName("jLabel14"); // NOI18N

        jScrollPane14.setName("jScrollPane14"); // NOI18N

        txtArea_Description.setColumns(5);
        txtArea_Description.setRows(2);
        txtArea_Description.setName("txtArea_Description"); // NOI18N
        jScrollPane14.setViewportView(txtArea_Description);

        txt_Tag.setText(resourceMap.getString("txt_Tag.text")); // NOI18N
        txt_Tag.setName("txt_Tag"); // NOI18N

        jLabel16.setText(resourceMap.getString("jLabel16.text")); // NOI18N
        jLabel16.setName("jLabel16"); // NOI18N

        btn_Apply.setIcon(resourceMap.getIcon("btn_Apply.icon")); // NOI18N
        btn_Apply.setText(resourceMap.getString("btn_Apply.text")); // NOI18N
        btn_Apply.setName("btn_Apply"); // NOI18N
        btn_Apply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ApplyActionPerformed(evt);
            }
        });

        jLabel15.setText(resourceMap.getString("jLabel15.text")); // NOI18N
        jLabel15.setName("jLabel15"); // NOI18N

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel13Layout.createSequentialGroup()
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                        .addComponent(btn_Apply))
                    .addGroup(jPanel13Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane14, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE))
                    .addGroup(jPanel13Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txt_Title, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel13Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txt_Tag, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txt_Title, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addComponent(jScrollPane14, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_Tag, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(btn_Apply))
                .addContainerGap())
        );

        jPanel14.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel14.border.title"))); // NOI18N
        jPanel14.setName("jPanel14"); // NOI18N

        lbl_UploadArea.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_UploadArea.setIcon(resourceMap.getIcon("lbl_UploadArea.icon")); // NOI18N
        lbl_UploadArea.setText(resourceMap.getString("lbl_UploadArea.text")); // NOI18N
        lbl_UploadArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        lbl_UploadArea.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lbl_UploadArea.setName("lbl_UploadArea"); // NOI18N
        lbl_UploadArea.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_UploadArea, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addComponent(lbl_UploadArea, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLable20.setText(resourceMap.getString("jLable20.text")); // NOI18N
        jLable20.setName("jLable20"); // NOI18N

        lbl_uploadStatus.setText(resourceMap.getString("lbl_uploadStatus.text")); // NOI18N
        lbl_uploadStatus.setName("lbl_uploadStatus"); // NOI18N

        javax.swing.GroupLayout panel_UploadLayout = new javax.swing.GroupLayout(panel_Upload);
        panel_Upload.setLayout(panel_UploadLayout);
        panel_UploadLayout.setHorizontalGroup(
            panel_UploadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_UploadLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(panel_UploadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panel_UploadLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jLable20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_uploadStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE))
                    .addGroup(panel_UploadLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(45, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        panel_UploadLayout.setVerticalGroup(
            panel_UploadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_UploadLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel_UploadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel13, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panel_UploadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panel_UploadLayout.createSequentialGroup()
                            .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panel_UploadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lbl_uploadStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 20, Short.MAX_VALUE)
                                .addComponent(jLable20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(jPanel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(36, Short.MAX_VALUE))
        );

        tab_FunctionPannel.addTab(resourceMap.getString("panel_Upload.TabConstraints.tabTitle"), panel_Upload); // NOI18N

        jLabel1.setIcon(resourceMap.getIcon("jLabel1.icon")); // NOI18N
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        buddyIcon.setText(resourceMap.getString("buddyIcon.text")); // NOI18N
        buddyIcon.setName("buddyIcon"); // NOI18N

        btn_initUser.setIcon(resourceMap.getIcon("btn_initUser.icon")); // NOI18N
        btn_initUser.setText(resourceMap.getString("btn_initUser.text")); // NOI18N
        btn_initUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_initUserActionPerformed(evt);
            }
        });

        jLabel21.setText(resourceMap.getString("jLabel21.text")); // NOI18N
        jLabel21.setName("jLabel21"); // NOI18N

        lbl_photo.setBackground(resourceMap.getColor("lbl_photo.background")); // NOI18N
        lbl_photo.setForeground(resourceMap.getColor("lbl_photo.foreground")); // NOI18N
        lbl_photo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_photo.setText(resourceMap.getString("lbl_photo.text")); // NOI18N
        lbl_photo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        lbl_photo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lbl_photo.setName("lbl_photo"); // NOI18N
        lbl_photo.setOpaque(true);
        lbl_photo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbl_photoMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                lbl_photoMousePressed(evt);
            }
        });
        lbl_photo.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                lbl_photoMouseMoved(evt);
            }
        });

        lbl_icon.setText(resourceMap.getString("lbl_icon.text")); // NOI18N
        lbl_icon.setName("lbl_icon"); // NOI18N
        lbl_icon.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbl_iconMouseClicked(evt);
            }
        });
        lbl_icon.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                lbl_iconMouseMoved(evt);
            }
        });

        statusPanelSeparator1.setName("statusPanelSeparator1"); // NOI18N

        //set tab height manually, because MAC OSX can't adjust height automatically
        tabbed_MainPanel.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override protected int calculateTabHeight(
                int tabPlacement, int tabIndex, int fontHeight) {
                return 32;
            }      
        });

        // Create bespoke component for rendering the tab.
        JLabel lbl_photolist2 = new JLabel("照片清單");
        lbl_photolist2.setIcon(resourceMap.getIcon("photolist.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_photolist2.setIconTextGap(5);
        lbl_photolist2.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tabbed_MainPanel.setTabComponentAt(0, lbl_photolist2);

        // Create bespoke component for rendering the tab.
        JLabel lbl_favo = new JLabel("最愛照片");
        lbl_favo.setIcon(resourceMap.getIcon("favo.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_favo.setIconTextGap(5);
        lbl_favo.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tabbed_MainPanel.setTabComponentAt(1, lbl_favo);

        // Create bespoke component for rendering the tab.
        JLabel lbl_photolist3 = new JLabel("群組照片");
        lbl_photolist3.setIcon(resourceMap.getIcon("group.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_photolist3.setIconTextGap(5);
        lbl_photolist3.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tabbed_MainPanel.setTabComponentAt(2, lbl_photolist3);
        //set tab height manually, because MAC OSX can't adjust height automatically
        tab_FunctionPannel.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override protected int calculateTabHeight(
                int tabPlacement, int tabIndex, int fontHeight) {
                return 32;
            }      
        });

        // Create bespoke component for rendering the tab.
        JLabel lbl_photolist = new JLabel("照片清單");
        lbl_photolist.setIcon(resourceMap.getIcon("photolist.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_photolist.setIconTextGap(5);
        lbl_photolist.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tab_FunctionPannel.setTabComponentAt(0, lbl_photolist);

        // Create bespoke component for rendering the tab.
        JLabel lbl_photoset = new JLabel("相片集");
        lbl_photoset.setIcon(resourceMap.getIcon("photoset.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_photoset.setIconTextGap(5);
        lbl_photoset.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tab_FunctionPannel.setTabComponentAt(1, lbl_photoset);

        // Create bespoke component for rendering the tab.
        JLabel lbl_tag = new JLabel("標簽籤管理");
        lbl_tag.setIcon(resourceMap.getIcon("tag.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_tag.setIconTextGap(5);
        lbl_tag.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tab_FunctionPannel.setTabComponentAt(2, lbl_tag);

        // Create bespoke component for rendering the tab.
        JLabel lbl_html = new JLabel("貼圖管理");
        lbl_html.setIcon(resourceMap.getIcon("html.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_html.setIconTextGap(5);
        lbl_html.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tab_FunctionPannel.setTabComponentAt(3, lbl_html);

        // Create bespoke component for rendering the tab.
        JLabel lbl_stat = new JLabel("統計資料");
        lbl_stat.setIcon(resourceMap.getIcon("stat.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_stat.setIconTextGap(5);
        lbl_stat.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tab_FunctionPannel.setTabComponentAt(4, lbl_stat);

        // Create bespoke component for rendering the tab.
        JLabel lbl_exif = new JLabel("Exif 過濾");
        lbl_exif.setIcon(resourceMap.getIcon("exif.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_exif.setIconTextGap(5);
        lbl_exif.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tab_FunctionPannel.setTabComponentAt(5, lbl_exif);

        // Create bespoke component for rendering the tab.
        JLabel lbl_upload = new JLabel("上傳管理員");
        lbl_upload.setIcon(resourceMap.getIcon("upload.icon")); // NOI18N
        // Add some spacing between text and icon, and position text to the RHS.
        lbl_upload.setIconTextGap(5);
        lbl_upload.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Assign bespoke tab component for first tab.
        tab_FunctionPannel.setTabComponentAt(6, lbl_upload);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                                .addComponent(lbl_icon, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                                .addComponent(jLabel21)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txt_username, javax.swing.GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_initUser))
                            .addComponent(lbl_photo, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tab_FunctionPannel, javax.swing.GroupLayout.PREFERRED_SIZE, 646, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(tabbed_MainPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 842, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buddyIcon, javax.swing.GroupLayout.DEFAULT_SIZE, 5, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(mainPanelLayout.createSequentialGroup()
                    .addGap(12, 12, 12)
                    .addComponent(statusPanelSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 843, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(22, Short.MAX_VALUE)))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addComponent(buddyIcon)
                                .addComponent(lbl_icon, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel1))
                        .addGap(21, 21, 21)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel21)
                            .addComponent(txt_username, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_initUser, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_photo, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(tab_FunctionPannel, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(21, 21, 21)
                .addComponent(tabbed_MainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE))
            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(mainPanelLayout.createSequentialGroup()
                    .addGap(275, 275, 275)
                    .addComponent(statusPanelSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(395, Short.MAX_VALUE)))
        );

        txt_username.getAccessibleContext().setAccessibleName(resourceMap.getString("txt_username.AccessibleContext.accessibleName")); // NOI18N

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        dirMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        dirMenuItem.setLabel(resourceMap.getString("dirMenuItem.label")); // NOI18N
        dirMenuItem.setName("dirMenuItem"); // NOI18N
        dirMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dirMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(dirMenuItem);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(myflickr.MyFlickrApp.class).getContext().getActionMap(MyFlickrView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        contactMenu.setText(resourceMap.getString("contactMenu.text")); // NOI18N
        contactMenu.setName("contactMenu"); // NOI18N
        menuBar.add(contactMenu);
        MenuScroller.setScrollerFor(contactMenu,35, 125);

        groupMenu.setText(resourceMap.getString("groupMenu.text")); // NOI18N
        groupMenu.setName("groupMenu"); // NOI18N
        menuBar.add(groupMenu);
        MenuScroller.setScrollerFor(groupMenu,35, 125);

        settingMenu.setText(resourceMap.getString("settingMenu.text")); // NOI18N
        settingMenu.setActionCommand(resourceMap.getString("settingMenu.actionCommand")); // NOI18N
        settingMenu.setName("settingMenu"); // NOI18N

        subSettingMenu_Size.setText(resourceMap.getString("subSettingMenu_Size.text")); // NOI18N
        subSettingMenu_Size.setName("subSettingMenu_Size"); // NOI18N

        rdoGrp_DownloadSize.add(subSettingFileSizeLarge);
        subSettingFileSizeLarge.setSelected(true);
        subSettingFileSizeLarge.setText(resourceMap.getString("subSettingFileSizeLarge.text")); // NOI18N
        subSettingFileSizeLarge.setName("subSettingFileSizeLarge"); // NOI18N
        subSettingFileSizeLarge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subSettingFileSizeLargeActionPerformed(evt);
            }
        });
        subSettingMenu_Size.add(subSettingFileSizeLarge);

        rdoGrp_DownloadSize.add(subSettingFileSizeOriginal);
        subSettingFileSizeOriginal.setText(resourceMap.getString("subSettingFileSizeOriginal.text")); // NOI18N
        subSettingFileSizeOriginal.setName("subSettingFileSizeOriginal"); // NOI18N
        subSettingFileSizeOriginal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subSettingFileSizeOriginalActionPerformed(evt);
            }
        });
        subSettingMenu_Size.add(subSettingFileSizeOriginal);

        settingMenu.add(subSettingMenu_Size);

        subSettingMenu_Thumbnail.setText(resourceMap.getString("subSettingMenu_Thumbnail.text")); // NOI18N
        subSettingMenu_Thumbnail.setName("subSettingMenu_Thumbnail"); // NOI18N
        subSettingMenu_Thumbnail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subSettingMenu_ThumbnailActionPerformed(evt);
            }
        });

        subSettingShowThumbnail.setSelected(true);
        subSettingShowThumbnail.setText(resourceMap.getString("subSettingShowThumbnail.text")); // NOI18N
        subSettingShowThumbnail.setName("subSettingShowThumbnail"); // NOI18N
        subSettingShowThumbnail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subSettingShowThumbnailActionPerformed(evt);
            }
        });
        subSettingMenu_Thumbnail.add(subSettingShowThumbnail);

        settingMenu.add(subSettingMenu_Thumbnail);

        subSettingMenu_AutoUpdate.setText(resourceMap.getString("subSettingMenu_AutoUpdate.text")); // NOI18N
        subSettingMenu_AutoUpdate.setName("subSettingMenu_AutoUpdate"); // NOI18N
        subSettingMenu_AutoUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subSettingMenu_AutoUpdateActionPerformed(evt);
            }
        });

        subSettingAutoUpdateOn.setSelected(true);
        subSettingAutoUpdateOn.setText(resourceMap.getString("subSettingAutoUpdateOn.text")); // NOI18N
        subSettingAutoUpdateOn.setName("subSettingAutoUpdateOn"); // NOI18N
        subSettingAutoUpdateOn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subSettingAutoUpdateOnActionPerformed(evt);
            }
        });
        subSettingMenu_AutoUpdate.add(subSettingAutoUpdateOn);

        settingMenu.add(subSettingMenu_AutoUpdate);

        subSettingMenu_Location.setText(resourceMap.getString("subSettingMenu_Location.text")); // NOI18N
        subSettingMenu_Location.setName("subSettingMenu_Location"); // NOI18N
        subSettingMenu_Location.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subSettingMenu_LocationActionPerformed(evt);
            }
        });
        settingMenu.add(subSettingMenu_Location);

        menuBar.add(settingMenu);
        settingMenu.getAccessibleContext().setAccessibleName(resourceMap.getString("settingMenu.AccessibleContext.accessibleName")); // NOI18N

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        debugMenuItm.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        debugMenuItm.setText(resourceMap.getString("debugMenuItm.text")); // NOI18N
        debugMenuItm.setName("debugMenuItm"); // NOI18N
        debugMenuItm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugMenuItmActionPerformed(evt);
            }
        });
        helpMenu.add(debugMenuItm);

        mnu_GoToMyBlog.setLabel(resourceMap.getString("mnu_GoToMyBlog.label")); // NOI18N
        mnu_GoToMyBlog.setName("mnu_GoToMyBlog"); // NOI18N
        mnu_GoToMyBlog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnu_GoToMyBlogActionPerformed(evt);
            }
        });
        helpMenu.add(mnu_GoToMyBlog);

        mnu_checkUpdate.setText(resourceMap.getString("mnu_checkUpdate.text")); // NOI18N
        mnu_checkUpdate.setName("mnu_checkUpdate"); // NOI18N
        mnu_checkUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnu_checkUpdateActionPerformed(evt);
            }
        });
        helpMenu.add(mnu_checkUpdate);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setPreferredSize(new java.awt.Dimension(801, 40));

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N
        statusAnimationLabel.setPreferredSize(new java.awt.Dimension(16, 16));

        progressBar.setName("progressBar"); // NOI18N

        lbl_PhotoListStatus.setText(resourceMap.getString("lbl_PhotoListStatus.text")); // NOI18N
        lbl_PhotoListStatus.setName("lbl_PhotoListStatus"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 733, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_PhotoListStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(63, 63, 63))
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 850, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(27, Short.MAX_VALUE))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(statusAnimationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbl_PhotoListStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 16, Short.MAX_VALUE))
                .addContainerGap())
        );

        mnu_popMenu.setName("mnu_popMenu"); // NOI18N

        mnu_SelectAll.setText(resourceMap.getString("mnu_SelectAll.text")); // NOI18N
        mnu_SelectAll.setName("mnu_SelectAll"); // NOI18N
        mnu_SelectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnu_SelectAllActionPerformed(evt);
            }
        });
        mnu_popMenu.add(mnu_SelectAll);

        jSeparator5.setName("jSeparator5"); // NOI18N
        mnu_popMenu.add(jSeparator5);

        mnu_refresh.setText(resourceMap.getString("mnu_refresh.text")); // NOI18N
        mnu_refresh.setName("mnu_refresh"); // NOI18N
        mnu_refresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnu_refreshActionPerformed(evt);
            }
        });
        mnu_popMenu.add(mnu_refresh);

        jSeparator3.setName("jSeparator3"); // NOI18N
        mnu_popMenu.add(jSeparator3);

        mnu_loadThumbnail.setText(resourceMap.getString("mnu_loadThumbnail.text")); // NOI18N
        mnu_loadThumbnail.setName("mnu_loadThumbnail"); // NOI18N
        mnu_loadThumbnail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnu_loadThumbnailActionPerformed(evt);
            }
        });
        mnu_popMenu.add(mnu_loadThumbnail);

        jSeparator2.setName("jSeparator2"); // NOI18N
        mnu_popMenu.add(jSeparator2);

        mnu_GetExtraData.setText(resourceMap.getString("mnu_GetExtraData.text")); // NOI18N
        mnu_GetExtraData.setName("mnu_GetExtraData"); // NOI18N
        mnu_GetExtraData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnu_GetExtraDataActionPerformed(evt);
            }
        });
        mnu_popMenu.add(mnu_GetExtraData);

        jSeparator4.setName("jSeparator4"); // NOI18N
        mnu_popMenu.add(jSeparator4);

        mnu_DownloadPhoto.setText(resourceMap.getString("mnu_DownloadPhoto.text")); // NOI18N
        mnu_DownloadPhoto.setName("mnu_DownloadPhoto"); // NOI18N
        mnu_DownloadPhoto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnu_DownloadPhotoActionPerformed(evt);
            }
        });
        mnu_popMenu.add(mnu_DownloadPhoto);

        Frame_Debug.setTitle(resourceMap.getString("Frame_Debug.title")); // NOI18N
        Frame_Debug.setName("Frame_Debug"); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        jScrollPane12.setName("jScrollPane12"); // NOI18N

        txtArea_DebugLog.setBackground(resourceMap.getColor("txtArea_DebugLog.background")); // NOI18N
        txtArea_DebugLog.setColumns(20);
        txtArea_DebugLog.setForeground(resourceMap.getColor("txtArea_DebugLog.foreground")); // NOI18N
        txtArea_DebugLog.setLineWrap(true);
        txtArea_DebugLog.setRows(5);
        txtArea_DebugLog.setWrapStyleWord(true);
        txtArea_DebugLog.setName("txtArea_DebugLog"); // NOI18N
        jScrollPane12.setViewportView(txtArea_DebugLog);

        btn_clear.setText(resourceMap.getString("btn_clear.text")); // NOI18N
        btn_clear.setName("btn_clear"); // NOI18N
        btn_clear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_clearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane12, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
                    .addComponent(btn_clear, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane12, javax.swing.GroupLayout.DEFAULT_SIZE, 429, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_clear)
                .addContainerGap())
        );

        javax.swing.GroupLayout Frame_DebugLayout = new javax.swing.GroupLayout(Frame_Debug.getContentPane());
        Frame_Debug.getContentPane().setLayout(Frame_DebugLayout);
        Frame_DebugLayout.setHorizontalGroup(
            Frame_DebugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        Frame_DebugLayout.setVerticalGroup(
            Frame_DebugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void tb_PhotoListMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tb_PhotoListMouseReleased
        // TODO add your handling code here:
        if (evt.isPopupTrigger()){
            mnu_popMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_tb_PhotoListMouseReleased

    private void tb_PhotoListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tb_PhotoListMouseClicked

    }//GEN-LAST:event_tb_PhotoListMouseClicked

    private void txt_usernameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_usernameActionPerformed
        // TODO add your handling code here:
        btn_initUserActionPerformed(evt);
}//GEN-LAST:event_txt_usernameActionPerformed


    private void _manualAuthProcess(Permission p){
         try {
            if(myController == null || myUser == null){return;}
            JOptionPane.showMessageDialog(null, "Current permission = "+ myController.getPermissionType());
            if(myController.authorizeEx(p) == Comm.RetrunCode.SUCCESS){
                JOptionPane.showMessageDialog(null, "認證成功! Permission = "+ myController.getPermissionType());
            }else{
                JOptionPane.showMessageDialog(null, "認證失敗");
            }
        } catch (URISyntaxException ex) {
            java.util.logging.Logger.getLogger(MyFlickrView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            logger.log(Level.ERROR, null, ex);
        } catch (IOException ex) {
            logger.log(Level.ERROR, null, ex);
        } catch (FlickrException ex) {
            logger.log(Level.ERROR, null, ex);
        }
     }
    private void _loadTagToList() {
        //this.myController.buildTagSetToUser();
        if(myUser.getTagSet() != null){
            ArrayList<String> data = new ArrayList<String>(myUser.getTagSet());
            Collections.sort(data);
            list_TagList.setListData(data.toArray());
        }
    }

    private void _loadContactList() {
        contactMenu.removeAll();        
        //MenuScroller menuScroller = new MenuScroller(contactMenu);
        if(myUser!= null && myUser.getContactList().size() >0){
            for(final Contact contact : myUser.getContactList()){
                final JMenuItem mnuItem = new JMenuItem(contact.getUsername());
                mnuItem.addActionListener(new ActionListener(){
                    public void actionPerformed(java.awt.event.ActionEvent evt) {                        
                        try {
                            if(taskInitUser != null && taskInitUser.isStarted()){return;}
                            if(JOptionPane.showConfirmDialog(null, "是否確定載入 "+ contact.getUsername() +" ?","Oops" ,JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
                                String pathAlias = myController.queryPathAliasName(contact.getId());
                                txt_username.setText(pathAlias);
                                btn_initUserActionPerformed(null);
                            }
                        } catch (FlickrException ex) {
                            logger.error(ex.getErrorMessage());
                        }

                    }
                });
                contactMenu.add(mnuItem);
            }            
        }
    }

    private void _loadGroupList(){
        groupMenu.removeAll();
        //MenuScroller menuScroller = new MenuScroller(contactMenu);
        if(myUser!= null && myUser.getGroupList().size() >0){
            for(final Group group : myUser.getGroupList()){
                final JMenuItem mnuItem = new JMenuItem(group.getName());
                mnuItem.addActionListener(new ActionListener(){
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        Thread t = new Thread(){
                            public void run(){
                                try {
                                    if(JOptionPane.showConfirmDialog(null, "是否確定載入 "+ group.getName() +" ?", "Oops", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
                                        //get photos from group
                                        busyIconTimer.start();

                                        ArrayList<PhotoWrapper> list = myController.getPhotosByGruop(group.getId());
                                        //load photos to table
                                        _loadListToTable(list, tb_groupPhoto);
                                        tabbed_MainPanel.setSelectedIndex(2);
                                        busyIconTimer.stop();
                                    }
                                } catch (FlickrException ex) {
                                    logger.error(ex.getErrorMessage());
                                    busyIconTimer.stop();
                                }
                            }
                        };
                        t.start();
                    }
                });
                groupMenu.add(mnuItem);
            }
        }
    }
    private void btn_initUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_initUserActionPerformed
        configManager.setValue(ConfigManager.CONF_USERNAME, txt_username.getText());
        if(taskInitUser != null && taskInitUser.isStarted()){return;}
        taskInitUser = initUserAction();
    }//GEN-LAST:event_btn_initUserActionPerformed

    private void tb_PhotoListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tb_PhotoListKeyPressed
     
    }//GEN-LAST:event_tb_PhotoListKeyPressed

    private void tb_PhotoListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tb_PhotoListKeyReleased
        lbl_photo.setText("");                
        if(evt.getKeyCode() == java.awt.event.KeyEvent.VK_UP || evt.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN){
            final PhotoWrapper selectedPhoto = this.getSelectedPhotoFromTableByIndex(0);
            if(selectedPhoto == null){return;}
            Thread t ;
            t = new Thread(){
                public void run(){
                    try {
                        logger.log(Level.INFO, "Set ["+selectedPhoto.getPhoto().getTitle()+"] to Image Preview Lable");
                        lbl_photo.setIcon(new ImageIcon(new URL(selectedPhoto.getPhoto().getSquareLargeUrl())));
                        _displayPhotoTag(selectedPhoto);                        
                    } catch (MalformedURLException ex) {
                        logger.log(Level.ERROR, null, ex);
                    }
                }
            };
            t.start();
            
        }         
    }//GEN-LAST:event_tb_PhotoListKeyReleased

    private void lbl_photoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_photoMouseClicked
      
    }//GEN-LAST:event_lbl_photoMouseClicked

    private void lbl_photoMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_photoMouseMoved
        lbl_photo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }//GEN-LAST:event_lbl_photoMouseMoved

    private void _downloadPhoto(ArrayList<PhotoWrapper> list, String folderName) {
        if(list == null || list.size()==0 || folderName==null || folderName.equalsIgnoreCase("")){
            logger.log(Level.ERROR, "Parameter error for _downloadPhoto()");
            return ;
        }        
            downloadWorker.setFolderName(folderName);
            downloadWorker.setPhotoList(list);
            downloadWorker.setCallback(txtArea_PhotoDownloadLog);
            downloadWorker.startDownload();                                     
    }


    private void lbl_iconMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_iconMouseClicked
        if(this.myUser != null){
            try {
                this.myDesktop.browse(new URI("http://www.flickr.com/"+this.myUser.getUser().getId()));
            } catch (URISyntaxException ex) {
                logger.log(Level.ERROR, null, ex);
            }catch (IOException ex) {
                logger.log(Level.ERROR, null, ex);
            }
        }
    }//GEN-LAST:event_lbl_iconMouseClicked

    private void lbl_iconMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_iconMouseMoved
        lbl_icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }//GEN-LAST:event_lbl_iconMouseMoved

    private void mnu_GetExtraDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnu_GetExtraDataActionPerformed
        _getExifData();
    }//GEN-LAST:event_mnu_GetExtraDataActionPerformed

    private void tb_PhotoListMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tb_PhotoListMousePressed

        if(activeTable.getSelectedRow() == -1){
            logger.log(Level.DEBUG, "No Row is selected in [tb_displayMouseClicked]");
            return ;
        }
        //logger.log(Level.DEBUG, "Row ["+tb_PhotoList.getModel().getValueAt(tb_PhotoList.getSelectedRow(), 0)+"] is selected"); //8 is the 8th column to represent url address
        //logger.log(Level.DEBUG, "Original Row is ["+tb_PhotoList.getRowSorter().convertRowIndexToModel(tb_PhotoList.getSelectedRow())+"] is selected"); //8 is the 7th column to represent url address
        final PhotoWrapper selectedPhoto = this.getSelectedPhotoFromTableByIndex(0);
        if (selectedPhoto != null)
        {
            lbl_photo.setText("");
            if(evt.getClickCount()>=2){
                try {
                    URI uri;
                    uri = new java.net.URI(selectedPhoto.getPhoto().getUrl());
                    this.myDesktop.browse(uri);
                } catch (IOException ex) {
                    logger.log(Level.ERROR, null, ex);
                } catch (URISyntaxException ex) {
                    logger.log(Level.ERROR, null, ex);
                }
                //show photo preview
            }else if(evt.getClickCount() == 1){
                //txtArea_Tags.setText("");
                Thread t ;
                t = new Thread(){
                    public void run(){
                        try {
                            //display photo icon
                            logger.log(Level.INFO, "Set ["+selectedPhoto.getPhoto().getSquareLargeUrl()+"] to Image Preview Lable...");
                            lbl_photo.setIcon(new ImageIcon(new URL(selectedPhoto.getPhoto().getSquareLargeUrl())));
                            logger.log(Level.INFO, "...Complete");

                            //logger.log(Level.INFO, "Start to display photo's tag");
                            _displayPhotoTag(selectedPhoto);
                            //logger.log(Level.INFO, "End to display photo's tag");
                        } catch (MalformedURLException ex) {
                            logger.log(Level.ERROR, null, ex);
                        }
                    }
                };
                t.start();
            }


        }
    }//GEN-LAST:event_tb_PhotoListMousePressed

    private void _refreshTable(){  //only refresh "value" but not visibility
        ArrayList<PhotoWrapper> list = this.getAllPhotoListFromTable();
        if(list == null || list.isEmpty()){
            return;
        }
    
        DefaultTableModel defaultModel = (DefaultTableModel)this.activeTable.getModel();
        //tb_PhotoList.
        for(int i=0;i<list.size();i++ ){
            //讓 _refreshTable() 後，資料不亂掉的關鍵之二，使用 setValue 而不是建立新的vector, 不然在 UI 中有排序的話，refresh後會亂掉
            PhotoWrapper p =list.get(i);            
            defaultModel.setValueAt(p, i, 0);
            defaultModel.setValueAt(p.getSequenceNumber(), i, 1);
            defaultModel.setValueAt(p.getPhoto().getTitle(), i, 2);
            defaultModel.setValueAt(p.getMeta().getExifDataWrapper().getCameraModel(), i, 3);
            defaultModel.setValueAt(p.getMeta().getExifDataWrapper().getLensModel(), i, 4);
            defaultModel.setValueAt(AsciiToDigit.atoi(p.getMeta().getExifDataWrapper().getFocalLength()), i, 5);
            defaultModel.setValueAt(AsciiToDigit.atof(p.getMeta().getExifDataWrapper().getAperture()), i, 6);
            defaultModel.setValueAt(p.getMeta().getExifDataWrapper().getExposure(), i, 7);
            defaultModel.setValueAt(AsciiToDigit.atoi(p.getMeta().getExifDataWrapper().getIsoSpeed()), i, 8);
            defaultModel.setValueAt(p.getMeta().getExifDataWrapper().getDateTaken(), i, 9);
            //temp.add(p.getMeta().getExifDataWrapper().getDateTaken());
            //defaultModel.setValueAt(p.getTagListToString(), i, 8);
            //defaultModel.setValueAt(new ImageIcon("https://farm4.staticflickr.com/3857/15094074837_308d9c103e_s.jpg"), i, 10);
        }
        defaultModel.fireTableDataChanged();  //即時更新正確資訊, 必要!
    }


    private void mnu_refreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnu_refreshActionPerformed
        // TODO add your handling code here:
        this._refreshTable();
    }//GEN-LAST:event_mnu_refreshActionPerformed

    private void debugMenuItmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugMenuItmActionPerformed
        Frame_Debug.setSize(640, 480);
        Frame_Debug.setVisible(true);
    }//GEN-LAST:event_debugMenuItmActionPerformed

    private void mnu_DownloadPhotoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnu_DownloadPhotoActionPerformed
        // TODO add your handling code here:
        if(this.myDownloadThread.isAlive()){
            JOptionPane.showMessageDialog(null, "檔案下載中，請稍後再試");
            return;
        }
        final ArrayList<PhotoWrapper> list = this.getSelectedPhotoListFromTable();
        myDownloadThread = new Thread(){
            @Override
            public void run(){
                busyIconTimer.start();
                _downloadPhoto(list, "Downloads");
                logger.log(Level.INFO, "Download complete");                
                //JOptionPane.showMessageDialog(null, "下載完成！路徑：" + Storage_base + System.getProperty("file.separator") + myUser.getUser().getPathAlias() + System.getProperty("file.separator") + "Downloads");
                JOptionPane.showMessageDialog(null, "下載完成！路徑：" + currentDownloadPath + System.getProperty("file.separator") + myUser.getUser().getPathAlias() + System.getProperty("file.separator") + "Downloads");
          
                busyIconTimer.stop();
            }
        };
        myDownloadThread.start();
        
    }//GEN-LAST:event_mnu_DownloadPhotoActionPerformed

    private void btn_clearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_clearActionPerformed
        txtArea_DebugLog.setText("");
    }//GEN-LAST:event_btn_clearActionPerformed

    private void lbl_photoMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_photoMousePressed
        PhotoWrapper selectedPhoto = this.getSelectedPhotoFromTableByIndex(0);
        if(selectedPhoto != null){
            try {
                this.myDesktop.browse(new URI(selectedPhoto.getPhoto().getUrl()));
            } catch (URISyntaxException ex) {
                logger.log(Level.ERROR, null, ex);
            }catch (IOException ex) {
                logger.log(Level.ERROR, null, ex);
            }
        }
    }//GEN-LAST:event_lbl_photoMousePressed

private void mnu_selectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnu_selectAllActionPerformed
    activeTable.selectAll();
}//GEN-LAST:event_mnu_selectAllActionPerformed

private void mnu_SelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnu_SelectAllActionPerformed
    activeTable.selectAll();
}//GEN-LAST:event_mnu_SelectAllActionPerformed

private void list_IsoValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_list_IsoValueChanged
    JList list = list_Iso;
    if(! evt.getValueIsAdjusting()){
        ArrayList<String> tmp = new ArrayList<String>();
        if(list.isSelectionEmpty()){
            //System.out.println("[Debug] isSelectionEmpty() ="+ list.isSelectionEmpty());
            tmp = stat.getIsoaList();
        }else{
            for(Object o : list.getSelectedValues()){
                tmp.add((String) o);
            }
        }
        //            for(String t : tmp){
        //                System.out.println("Filter Spec of "+list.getName()+" : " + t);
        //            }
        filterSpec.setSelectedIso(tmp);
    }
}//GEN-LAST:event_list_IsoValueChanged

private void btn_FilterResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_FilterResetActionPerformed
    OrigianlPhotoListIsBackupable = true; //按下重設後，才能重新備份, 和 btn_FilterQueryActionPerformed 關聯
    _resetJListSelection();
    ArrayList<PhotoWrapper> list = filterManager.getOriginalPhotoList();
    if(list == null){return;}
    for(PhotoWrapper p:list){
        p.setVisiable(true);
    }
    logger.log(Level.DEBUG, "Restore original list");
    _loadListToTable(list, this.tb_PhotoList);
    tabbed_MainPanel.setSelectedIndex(0);
    //
    //        if(filterManager !=null){
    //            this._loadListToTable(filterManager.getOriginalPhotoList());
    //        }
        /*
        if(this.myUser !=null){
            _buildTable(this.myUser);
        }
         */
}//GEN-LAST:event_btn_FilterResetActionPerformed

private void btn_FilterQueryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_FilterQueryActionPerformed
    //ArrayList<PhotoWrapper> filteredPhoto = null;
    ArrayList<PhotoWrapper> list = this.getAllPhotoListFromTable();
    if(list != null && filterSpec!=null){
        if(OrigianlPhotoListIsBackupable) {
            logger.log(Level.DEBUG, "Backup original list");
            filterManager.setOriginalPhotoList(list);
            OrigianlPhotoListIsBackupable = false;  //如果重覆search的話，避免將新的結果蓋掉最初的結果
        }else{
            logger.log(Level.DEBUG, "Can not backup original photo list, please do reset to make it backup-able");
        }
        //filteredPhoto = filterManager.getFilteredPhoto(this.myUser, filterSpec);
        //filterManager.filterPhoto(list, filterSpec);
        filterManager.filterPhoto(filterManager.getOriginalPhotoList(), filterSpec);
        _loadListToTable(filterManager.getOriginalPhotoList(), this.tb_PhotoList);
        //System.out.println("filteredPhoto.size = "+filteredPhoto.size());
        //if( !filteredPhoto.isEmpty()){
        //_loadListToTable(list);
        //}else{
        //JOptionPane.showMessageDialog(mainPanel, "查無資料", "Oops..", JOptionPane.OK_OPTION);
        //System.out.println("Not Found");
        //}
    }else{
        //JOptionPane.showMessageDialog(mainPanel, "請先至「我的照片清單」，並勾選「需要額外資訊」，建立照片清單", "Oops..", JOptionPane.OK_OPTION);
        JOptionPane.showMessageDialog(null, "查詢錯誤，請先載入照片並勾選查詢條件");
    }
}//GEN-LAST:event_btn_FilterQueryActionPerformed

private void list_ExposureValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_list_ExposureValueChanged
    JList list = list_Exposure;
    if(! evt.getValueIsAdjusting()){
        ArrayList<String> tmp = new ArrayList<String>();
        if(list.isSelectionEmpty()){
            //System.out.println("[Debug] isSelectionEmpty() ="+ list.isSelectionEmpty());
            tmp = stat.getShutterList();
        }else{
            for(Object o : list.getSelectedValues()){
                tmp.add((String) o);
            }
        }
        //            for(String t : tmp){
        //                System.out.println("Filter Spec of "+list.getName()+" : " + t);
        //            }
        filterSpec.setSelectedShutter(tmp);
    }
}//GEN-LAST:event_list_ExposureValueChanged

private void list_FocalValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_list_FocalValueChanged
    JList list = list_Focal;
    if(! evt.getValueIsAdjusting()){
        ArrayList<String> tmp = new ArrayList<String>();
        if(list.isSelectionEmpty()){
            //System.out.println("[Debug] isSelectionEmpty() ="+ list.isSelectionEmpty());
            tmp = stat.getFocalLengthList();
        }else{
            for(Object o : list.getSelectedValues()){
                tmp.add((String) o);
            }
        }
        //            for(String t : tmp){
        //                System.out.println("Filter Spec of "+list.getName()+" : " + t);
        //            }
        filterSpec.setSelectedFocalLength(tmp);
    }
}//GEN-LAST:event_list_FocalValueChanged

private void list_LensValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_list_LensValueChanged
    JList list = list_Lens;
    if(! evt.getValueIsAdjusting()){
        ArrayList<String> tmp = new ArrayList<String>();
        if(list.isSelectionEmpty()){
            //System.out.println("[Debug] isSelectionEmpty() ="+ list.isSelectionEmpty());
            tmp = stat.getLensIDList();
        }else{
            for(Object o : list.getSelectedValues()){
                tmp.add((String) o);
            }
        }
        //            for(String t : tmp){
        //                System.out.println("Filter Spec of "+list.getName()+" : " + t);
        //            }
        filterSpec.setSelectedLensId(tmp);
    }
}//GEN-LAST:event_list_LensValueChanged

private void list_ApertureValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_list_ApertureValueChanged
    JList list = list_Aperture;
    if(! evt.getValueIsAdjusting()){
        ArrayList<String> tmp = new ArrayList<String>();
        if(list.isSelectionEmpty()){
            //System.out.println("[Debug] isSelectionEmpty() ="+ list.isSelectionEmpty());
            tmp = stat.getApertureList();
        }else{
            for(Object o : list.getSelectedValues()){
                tmp.add((String) o);
            }
        }
        //            for(String t : tmp){
        //                System.out.println("Filter Spec of "+list.getName()+" : " + t);
        //            }
        filterSpec.setSelectedAperture(tmp);
    }
}//GEN-LAST:event_list_ApertureValueChanged

private void list_CameraValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_list_CameraValueChanged
    JList list = list_Camera;
    if(! evt.getValueIsAdjusting()){
        ArrayList<String> tmp = new ArrayList<String>();
        if(list.isSelectionEmpty()){
            //System.out.println("[Debug] isSelectionEmpty() ="+ list.isSelectionEmpty());
            tmp = stat.getCameraList();
        }else{
            for(Object o : list.getSelectedValues()){
                tmp.add((String) o);
            }
        }
        //            for(String t : tmp){
        //                System.out.println("Filter Spec of "+list.getName()+" : " + t);
        //            }
        filterSpec.setSelectedCameraModel(tmp);
    }
}//GEN-LAST:event_list_CameraValueChanged

private void btn_ShowStasticChartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ShowStasticChartActionPerformed
    //update user data to pie chart
    /*
    0,相機型號
    1,鏡頭ID
    2,焦長
    3,快門
    4,光圈值
    5,ISO值
     */

    ArrayList<PhotoWrapper> selectedPhoto = getSelectedPhotoListFromTable();
    if(this.myUser == null){
        JOptionPane.showMessageDialog(null, "使用者尚未初始化");
        return;
    }
    logger.log(Level.DEBUG, "selectedPhoto.size = "+selectedPhoto.size());
    _buildPieChartData(selectedPhoto);
    int index = comboList.getSelectedIndex();
    if(index == 0){
        this.pieChart_Camera.pack();
        RefineryUtilities.centerFrameOnScreen(this.pieChart_Camera);
        this.pieChart_Camera.setVisible(true);
        //        barChart_Camera.pack();
        //        RefineryUtilities.centerFrameOnScreen(barChart_Camera);
        //        barChart_Camera.setVisible(true);
    }else if(index ==1){

        this.pieChart_LensID.pack();
        RefineryUtilities.centerFrameOnScreen(this.pieChart_LensID);
        this.pieChart_LensID.setVisible(true);
    }else if(index ==2){
        this.pieChart_FocolLength.pack();
        RefineryUtilities.centerFrameOnScreen(this.pieChart_FocolLength);
        this.pieChart_FocolLength.setVisible(true);
    }else if(index ==3){
        this.pieChart_ShutterSpeed.pack();
        RefineryUtilities.centerFrameOnScreen(this.pieChart_ShutterSpeed);
        this.pieChart_ShutterSpeed.setVisible(true);
    }else if(index ==4){
        this.pieChart_ApertureValue.pack();
        RefineryUtilities.centerFrameOnScreen(this.pieChart_ApertureValue);
        this.pieChart_ApertureValue.setVisible(true);
    }else if(index ==5){
        this.pieChart_IsoSpeed.pack();
        RefineryUtilities.centerFrameOnScreen(this.pieChart_IsoSpeed);
        this.pieChart_IsoSpeed.setVisible(true);
    }else{
        logger.log(Level.ERROR,"Error in btnShowCameraChartActionPerformed");
    }
}//GEN-LAST:event_btn_ShowStasticChartActionPerformed

private void btn_HtmlCodeGenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_HtmlCodeGenActionPerformed
    // TODO add your handling code here:
    boolean bHyperlinkToPage = true; //fit flickr policy
    boolean bHighRes = ck_HighResDisplay.isSelected();
    ArrayList<PhotoWrapper> selectedPhoto = this.getSelectedPhotoListFromTable();
    if(selectedPhoto == null || selectedPhoto.isEmpty()){
        return;
    }
    Comm.PhotoSize photoSize = Comm.PhotoSize.Default;    
    if(combo_SizeSelection.getSelectedIndex()==0){
        photoSize = Comm.PhotoSize.Medium_500;
        //retinaPhotoSize = Comm.PhotoSize.Large;
    }
    if(combo_SizeSelection.getSelectedIndex()==1){
        photoSize = Comm.PhotoSize.Medium_640;
        //retinaPhotoSize = Comm.PhotoSize.Large;
    }
    if(combo_SizeSelection.getSelectedIndex()==2){
        photoSize = Comm.PhotoSize.Medium_800;
        //retinaPhotoSize = Comm.PhotoSize.Large_1600;
    }
    if(combo_SizeSelection.getSelectedIndex()==3){
        photoSize = Comm.PhotoSize.Large;
        //retinaPhotoSize = photoSize;
    }
    if(combo_SizeSelection.getSelectedIndex()==4){
        photoSize = Comm.PhotoSize.Original;
        //retinaPhotoSize = photoSize;
    }    
    if(this.myController.generatorHtml(myUser.getUsername(), selectedPhoto, photoSize, ck_photoTitle.isSelected(), ck_phtoDescription.isSelected(),
            ck_CameraModel.isSelected(), ck_LensId.isSelected(), ck_Aperture.isSelected(), ck_Exposure.isSelected(), ck_FocalLength.isSelected(), ck_Iso.isSelected(), bHyperlinkToPage, bHighRes) == Comm.RetrunCode.SUCCESS){
        txt_HtmlCode.setText(myController.getHtmlSourceCode());
    }else{
        txt_HtmlCode.setText("產生 Html 失敗，無法取得選取的照片尺寸");
    }
}//GEN-LAST:event_btn_HtmlCodeGenActionPerformed

private void txt_HtmlCodeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txt_HtmlCodeMouseClicked
    // TODO add your handling code here:
    txt_HtmlCode.selectAll();
    txt_HtmlCode.copy();
}//GEN-LAST:event_txt_HtmlCodeMouseClicked

private void btn_searchByTagActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_searchByTagActionPerformed

    if(myController == null || myUser == null){JOptionPane.showMessageDialog(null, "請先輸入使用者名稱");return;}
    if(myTagThread.isAlive()){
        JOptionPane.showMessageDialog(null, "資料處理中…");
    }else{
        myTagThread = new Thread(){
            @Override
            public void run(){
                try {
                    Object[] selectedValue = list_TagList.getSelectedValues();
                    if(selectedValue.length == 0){return;}
                    ArrayList<String> aryPara = new ArrayList<String>();

                    for (Object o : selectedValue) {
                        aryPara.add((String) o);
                    }

                    String[] para = new String[aryPara.size()];
                    para = aryPara.toArray(para);

                    logger.log(Level.INFO, "Start to Search photo by tags...");
                    //                    txtAera_TagLog.append("開始搜尋標籤：");
                    //                    for(String p:para){
                    //                        txtAera_TagLog.append("["+p+"], ");
                    //                    }
                    //                    txtAera_TagLog.append("...\n");
                    busyIconTimer.start();
                    ArrayList<PhotoWrapper> tagSearchResultPhoto = myController.SearchPhotoByTag(para);
                    //txtAera_TagLog.append("...完成\n");
                    logger.log(Level.INFO, "Start to update table by found tags...");
                    if(tagSearchResultPhoto.size()>0){
                        _loadListToTable(tagSearchResultPhoto, tb_PhotoList);
                        tabbed_MainPanel.setSelectedIndex(0);
                        JOptionPane.showMessageDialog(null, "完成！");
                    }else{
                        JOptionPane.showMessageDialog(null, "查無資料");
                        logger.log(Level.INFO, "No photo found by tag search");
                    }
                    logger.log(Level.INFO, "Table is udpated");
                } catch (FlickrException ex) {
                    JOptionPane.showMessageDialog(null, "查詢失敗: "+ex.getErrorMessage());
                    logger.log(Level.ERROR, null, ex);
                } catch (FlickrRuntimeException  ex){
                    JOptionPane.showMessageDialog(null, "查詢失敗: "+ex.getMessage());
                    logger.log(Level.FATAL, "FlickrRuntimeException occurs: +ex", ex);
                } finally{
                    busyIconTimer.stop();
                }
            }
        };
        myTagThread.start();
    }
}//GEN-LAST:event_btn_searchByTagActionPerformed

private void tgBtn_displayTempListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgBtn_displayTempListActionPerformed

}//GEN-LAST:event_tgBtn_displayTempListActionPerformed

private void tgBtn_displayTempListItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_tgBtn_displayTempListItemStateChanged

    if(evt.getStateChange() == java.awt.event.ItemEvent.SELECTED){
        myController.setBackupList(this.getAllPhotoListFromTable(this.tb_PhotoList)); //backup original phot list
        this._loadListToTable(batchPhotoList, this.tb_PhotoList);
        this.tabbed_MainPanel.setSelectedIndex(0);
    }else{        
        this._loadListToTable(myController.getBackupList(), this.tb_PhotoList);
        this.tabbed_MainPanel.setSelectedIndex(0);
    }
}//GEN-LAST:event_tgBtn_displayTempListItemStateChanged

private void tgBtn_displayTempListStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tgBtn_displayTempListStateChanged
    // TODO add your handling code here:
}//GEN-LAST:event_tgBtn_displayTempListStateChanged

private void btn_DisplayOriginalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_DisplayOriginalActionPerformed
    if(this.myController == null || this.myUser == null || this.myUser.getPhotoWrapperList()==null || this.myUser.getPhotoWrapperList().isEmpty()){
        logger.log(Level.ERROR, "Data is not ready to reload");
        JOptionPane.showMessageDialog(null, "尚無資料！請先至「照片清單」建立清單");
    }else{
        logger.log(Level.INFO, "Reload Table...");
        _loadListToTable(this.myUser.getPhotoWrapperList(), this.tb_PhotoList);
        logger.log(Level.INFO, "...Complete");
    }
    lbl_batchCount.setText(String.valueOf(batchPhotoList.size()));
}//GEN-LAST:event_btn_DisplayOriginalActionPerformed

private void btn_RemoveAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_RemoveAllActionPerformed

    if(JOptionPane.showConfirmDialog(null, "是否真的要清空批次清單？", "Oops", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
        this.batchPhotoList.clear();
        lbl_batchCount.setText(String.valueOf(batchPhotoList.size()));
        this._refreshTable();
    }
}//GEN-LAST:event_btn_RemoveAllActionPerformed

private void btn_RemoveFromBatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_RemoveFromBatchActionPerformed
    ArrayList<PhotoWrapper> selectedPhotos= this.getSelectedPhotoListFromTable();
    if(selectedPhotos != null){
        for(PhotoWrapper selectedPhoto : selectedPhotos){
            if(this.batchPhotoList.contains(selectedPhoto)){
                this.batchPhotoList.remove(selectedPhoto);
            }
        }
        //this._loadListToTable(batchPhotoList);
    }
    lbl_batchCount.setText(String.valueOf(batchPhotoList.size()));
}//GEN-LAST:event_btn_RemoveFromBatchActionPerformed

private void btn_AddToBatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_AddToBatchActionPerformed
    ArrayList<PhotoWrapper> selectedPhoto= this.getSelectedPhotoListFromTable();
    if(selectedPhoto != null){
        for(PhotoWrapper p : selectedPhoto){
            if(!this.batchPhotoList.contains(p)){
                this.batchPhotoList.add(p);
            }
        }
    }
    lbl_batchCount.setText(String.valueOf(batchPhotoList.size()));
}//GEN-LAST:event_btn_AddToBatchActionPerformed

private void btn_TagAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_TagAddActionPerformed

    if(myTagThread.isAlive()){
        JOptionPane.showMessageDialog(null, "資料處理中...請稍後再試");
    }else{
        myTagThread = new Thread(){
            public void run(){
                ArrayList<PhotoWrapper> list = getSelectedPhotoListFromTable();
                if(list.size()>0){
                    busyIconTimer.start();
                    if(_updatePhotoTag(list, Comm.Tag_Method.ADD) == Comm.RetrunCode.SUCCESS){
                            try {
                                myController.updateUserTagSet();
                                _loadTagToList();
                                JOptionPane.showMessageDialog(null, "設定完成！");
                                busyIconTimer.stop();
                            } catch (FlickrException ex) {
                                logger.error("Unknown exception "+ex.getErrorMessage(), ex);
                                busyIconTimer.stop();
                            }
                    }else{
                        JOptionPane.showMessageDialog(null, "設定結束，其中可能有錯誤，請查閱log！");
                        busyIconTimer.stop();
                    }
                }
            }
        };
        myTagThread.start();
    }
}//GEN-LAST:event_btn_TagAddActionPerformed

private void btn_TagSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_TagSetActionPerformed
    if(myPhotoThread.isAlive() || myTagThread.isAlive()){
        JOptionPane.showMessageDialog(null, "忙碌中，請稍後再試..");
        return;
    }

    if( JOptionPane.YES_OPTION ==JOptionPane.showConfirmDialog(null,"該動作會覆蓋原先的標籤，改以新的取代，是否繼續？", "Oops", JOptionPane.YES_NO_OPTION)){
        myTagThread = new Thread(){
            public void run(){
                ArrayList<PhotoWrapper> list = getSelectedPhotoListFromTable();
                if(list.size()>0){
                    busyIconTimer.start();
                    Comm.RetrunCode rt = _updatePhotoTag(list, Comm.Tag_Method.SET);
                    if(rt == Comm.RetrunCode.SUCCESS){
                            try {
                                //update tag list
                                myController.updateUserTagSet();
                                _loadTagToList();
                                JOptionPane.showMessageDialog(null, "設定結束！");
                                busyIconTimer.stop();
                            } catch (FlickrException ex) {
                                logger.error("Unknown error:"+ex.getErrorMessage(), ex);
                                JOptionPane.showMessageDialog(null, "未知的錯誤："+ex.getErrorMessage());
                                busyIconTimer.stop();
                            }
                    }else if(rt == Comm.RetrunCode.PERMISSION_DENIED){
                        JOptionPane.showMessageDialog(null, "認証失敗，請輸入正確的認證碼或確認Flickr登入帳號和求帳號相同");
                        busyIconTimer.stop();
                    }else{
                        JOptionPane.showMessageDialog(null, "過程發生錯誤，未完全設定正確，請查閱 Log");
                        busyIconTimer.stop();
                    }
                }
            }
        };
        myTagThread.start();
    }
}//GEN-LAST:event_btn_TagSetActionPerformed

private void btn_backupSelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_backupSelectAllActionPerformed
    int start = 0;
    int end = list_photoset.getModel().getSize() - 1;
    if (end >= 0) {
        list_photoset.setSelectionInterval(start, end);
    }
}//GEN-LAST:event_btn_backupSelectAllActionPerformed

private void btn_backupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_backupActionPerformed


    //一定要檔，不檔的話會有不可預期的結果。例如按了"下載"兩次，同時啟動兩個下載的 thread, 這樣可能照片會下載錯資料夾
    if(this.myDownloadThread.isAlive()){
        JOptionPane.showMessageDialog(null, "檔案下載中，請稍後再試");
        return;
    }

    //get selected Photoset and set display message
    Object[] selectedValues = list_photoset.getSelectedValues();
    if(selectedValues.length == 0){return;}
    StringBuffer msg = new StringBuffer();
    final ArrayList<PhotoSetWrapper> photosetWraperLsit = new ArrayList<PhotoSetWrapper>();
    
    //message display
    int toBeDownloadedPhotoCount = 0;
    for(Object o:selectedValues){
        PhotoSetWrapper temp = (PhotoSetWrapper)o;
        photosetWraperLsit.add(temp);
        if(toBeDownloadedPhotoCount <=10){
                msg.append(">> ").append(temp.toString()).append("\n");
        }
        toBeDownloadedPhotoCount++;
    }
    if(toBeDownloadedPhotoCount>=10){
            msg.append("...\n共 ").append(toBeDownloadedPhotoCount).append(" 本相片集？");
    }
    myDownloadThread = new Thread(){
        public void run(){
            busyIconTimer.start();
            //PhotoSetWrapper psw = (PhotoSetWrapper) list_photoset.getSelectedValue();
            for(PhotoSetWrapper psw : photosetWraperLsit){
                String title = psw.toString().replaceAll("[\\/*?\"<>|]", "-");  // windows does not accept \ / * ? " < > | as folder path
                title = title.replaceAll("\\.", "");
                try {
                    ArrayList<PhotoWrapper> list = myController.queryPhotoInPhotoset(psw);
                        try {
                            //稍等一下讓前面的東西完全跑完再開始下一次的download, 不然顯示可能會有誤
                            Thread.sleep(500); //稍等一下讓前面的東西完全跑完再開始下一次的download, 不然顯示可能會有誤
                        } catch (InterruptedException ex) {
                            java.util.logging.Logger.getLogger(MyFlickrView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                        }
                    _downloadPhoto(list, title);
                    //}
                } catch (FlickrException ex) {
                    logger.log(Level.ERROR, "queryPhotoInPhotoset rror Message = "+ex.getErrorMessage());
                    JOptionPane.showMessageDialog(null, "未知的錯誤：Error Message = "+ex.getErrorMessage());
                }
            }
            logger.log(Level.INFO, "Download complete");            
            JOptionPane.showMessageDialog(null, "下載完成！路徑：" + currentDownloadPath + System.getProperty("file.separator") + myUser.getUser().getPathAlias() + System.getProperty("file.separator")
            +"{相片集名稱}");
            busyIconTimer.stop();
        }
    };
    myDownloadThread.setName("Album downloader Thread");
    int result = JOptionPane.showConfirmDialog(null, "是否確定下載相片集("+photosetWraperLsit.size()+")\n"+msg+"\n", "Oops", JOptionPane.YES_NO_OPTION);
    if (result==JOptionPane.YES_OPTION){
        myDownloadThread.start();
    }
}//GEN-LAST:event_btn_backupActionPerformed

private void list_photosetMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_list_photosetMousePressed
    // TODO add your handling code here:
    if(list_photoset.getSelectedValue() == null){return;}
    //        if(myPhotoThread.isAlive() || myTagThread.isAlive()){
    //            JOptionPane.showMessageDialog(null, "忙碌中，請稍後再試..");
    //            return;
    //        }

    final PhotoSetWrapper psw = (PhotoSetWrapper)list_photoset.getSelectedValue();
    if(evt.getClickCount()>=2){ //雙擊
        try {
            myDesktop.browse(new URI(psw.getPhotoSet().getUrl()));
        } catch (IOException ex) {
            logger.log(Level.ERROR, null, ex);
        } catch (URISyntaxException ex) {
            logger.log(Level.ERROR, null, ex);
        }
    }else if(evt.getClickCount() == 1){ //單擊
        //單擊
        Thread t = new Thread(){
            public void run(){
                try {
                    busyIconTimer.start();
                    ArrayList<PhotoWrapper> photos = myController.queryPhotoInPhotoset(psw);
                    //display the fisrt photo as the photo set cover                    
                    lbl_photo.setText("");
                    //int indexPhotoSetCover = new Random().nextInt(photos.size());//0 ~ photo size
                    lbl_photo.setIcon(new ImageIcon(new URL(photos.get(0).getPhoto().getSquareLargeUrl())));
                    _loadListToTable(photos, tb_PhotoList);
                    tabbed_MainPanel.setSelectedIndex(0);
                    busyIconTimer.stop();
                } catch (FlickrException ex) {
                    JOptionPane.showMessageDialog(null, "載入相片集失敗: "+ex.getErrorMessage());
                    logger.log(Level.ERROR, null, ex);
                    busyIconTimer.stop();
                } catch (Exception ex){
                    JOptionPane.showMessageDialog(null, "載入相片集失敗: "+ex.getMessage());
                    logger.log(Level.ERROR, null, ex);
                    busyIconTimer.stop();
                } finally{
                    busyIconTimer.stop();
                }
            }
        };
        t.start();
    }
}//GEN-LAST:event_list_photosetMousePressed

private void list_photosetMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_list_photosetMouseClicked

}//GEN-LAST:event_list_photosetMouseClicked

private void btn_ExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ExportActionPerformed
    if(this.myUser != null && tb_PhotoList.getRowCount() !=0){
        String filename = this.myUser.getUsername()+".xls";
        ExcelExporter exporter = new ExcelExporter();
        if(exporter.fillData(tb_PhotoList, new File(filename)) == Comm.RetrunCode.SUCCESS){
            JOptionPane.showMessageDialog(null, filename+" 匯出成功！");
            logger.log(Level.INFO, "匯出excel成功");
        }else{
            JOptionPane.showMessageDialog(null, filename+" 匯出失敗！");
            logger.log(Level.ERROR, "匯出excel失敗");
        }
    }else{
        JOptionPane.showMessageDialog(null, "請先選擇照片");
    }
}//GEN-LAST:event_btn_ExportActionPerformed

private void btn_reloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_reloadActionPerformed
    // TODO add your handling code here:
    //        if(this.myController == null || this.myUser == null || this.myUser.getPhotoWrapperList()==null || this.myUser.getPhotoWrapperList().size()==0){
    //            logger.log(Level.ERROR, "Data is not ready to reload");
    //            JOptionPane.showMessageDialog(null, "尚無資料！請先至「照片清單」中分析照片");
    //        }else{
    //            logger.log(Level.INFO, "Reload Table...");
    //            //_updateTable(this.myUser.getPhotoWrapperList());
    //            _reloadPhotoListToTable(this.myUser.getPhotoWrapperList());
    //            logger.log(Level.INFO, "...Complete");
    //        }
    btn_DisplayOriginalActionPerformed(null);
}//GEN-LAST:event_btn_reloadActionPerformed

private void btn_StartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_StartActionPerformed
    this.buildMyPhotoAction();
}//GEN-LAST:event_btn_StartActionPerformed

private void tabbed_MainPanelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabbed_MainPanelStateChanged
    // TODO add your handling code here:
    JTabbedPane sourceTabbedPane = (JTabbedPane) evt.getSource();
    int index = sourceTabbedPane.getSelectedIndex();
    if(index==0){this.activeTable = this.tb_PhotoList;}
    if(index==1){this.activeTable = this.tb_favPhoto;}
    if(index==2){this.activeTable = this.tb_groupPhoto;}
    this._refreshTable();
}//GEN-LAST:event_tabbed_MainPanelStateChanged

private void sdfasf(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sdfasf
    // TODO add your handling code here:
}//GEN-LAST:event_sdfasf

private void tb_favPhotoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tb_favPhotoKeyPressed
    
}//GEN-LAST:event_tb_favPhotoKeyPressed

private void tb_favPhotoMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tb_favPhotoMousePressed
    tb_PhotoListMousePressed(evt);
}//GEN-LAST:event_tb_favPhotoMousePressed

private void tb_favPhotoMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tb_favPhotoMouseReleased
    tb_PhotoListMouseReleased(evt);
}//GEN-LAST:event_tb_favPhotoMouseReleased

private void btn_SelectAll1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_SelectAll1ActionPerformed

    this.activeTable.selectAll();
}//GEN-LAST:event_btn_SelectAll1ActionPerformed

private void btn_SelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_SelectAllActionPerformed
    activeTable.selectAll();
}//GEN-LAST:event_btn_SelectAllActionPerformed

private void mnu_GoToMyBlogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnu_GoToMyBlogActionPerformed
    
    try {
            Desktop.getDesktop().browse(new URI("http://mong0520.blogspot.tw/2013/08/flickr-v10.html"));
        } catch (URISyntaxException ex) {
            logger.log(Level.ERROR, null, ex);
        } catch (IOException ex) {
            logger.log(Level.ERROR, null, ex);
        }

}//GEN-LAST:event_mnu_GoToMyBlogActionPerformed

private void tb_groupPhotoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tb_groupPhotoKeyReleased
    tb_PhotoListKeyReleased(evt);
}//GEN-LAST:event_tb_groupPhotoKeyReleased

private void tb_groupPhotoMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tb_groupPhotoMouseReleased
    tb_PhotoListMouseReleased(evt);
}//GEN-LAST:event_tb_groupPhotoMouseReleased

private void tb_groupPhotoMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tb_groupPhotoMousePressed
    tb_PhotoListMousePressed(evt);    // TODO add your handling code here:
}//GEN-LAST:event_tb_groupPhotoMousePressed

private void dirMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dirMenuItemActionPerformed
        if(!currentDownloadPath.exists()){
            currentDownloadPath.mkdirs();
        }
        try {
            if(currentDownloadPath.isDirectory())
                Desktop.getDesktop().open(currentDownloadPath);
            else{
                Desktop.getDesktop().open(defaultDownloadPath);
                currentDownloadPath = defaultDownloadPath;
            }
        } catch (IOException ex) {
            logger.log(Level.ERROR, "Open dir failed:"+ currentDownloadPath);    
            currentDownloadPath = defaultDownloadPath;
        }
}//GEN-LAST:event_dirMenuItemActionPerformed

private void list_UploadListMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_list_UploadListMousePressed
    
    File f = (File) list_UploadList.getSelectedValue();
    if(f==null){return;}
    lbl_photo.setText("");
    BufferedImage image = null;
    try {
        image = ImageIO.read(f);
    } catch (IOException ex) {
        logger.log(Level.ERROR, null, ex);
    }
    int width = image.getWidth();
    int height = image.getHeight();
    int ratio = width/150;
    height = height / ratio;
    width = width / ratio;
    BufferedImage resizedImage=ImageResizer.resize(image,width,height);
    lbl_photo.setIcon(new ImageIcon(resizedImage));
    UploadMetaData meta = myController.getUploadMeta(f);
    
    //restore meta data
    txt_Title.setText(meta.getTitle());
    txtArea_Description.setText(meta.getDescription());
    if(meta.getTags()!=null){
        StringBuffer strb = new StringBuffer();
        for(String tag : meta.getTags()){
            strb.append(tag + ",");
        }
        if(strb.length() > 0){
            txt_Tag.setText(strb.toString().substring(0, strb.length()-1)); //remove the tailing ","
        }
    }else{
        txt_Tag.setText("");
    }
    //cmb_PhotoSet.setSelectedItem(myController.getPhotoUploadToPhotoSet(f));

}//GEN-LAST:event_list_UploadListMousePressed

private void btn_ApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ApplyActionPerformed
    if(list_UploadList.getSelectedValues().length == 0 ){
        JOptionPane.showMessageDialog(null, "請先選擇照片");
    }else{
        String[] tags = null;
        LinkedList<String> tagList = null;
        for(Object o : list_UploadList.getSelectedValues()){
            File f = (File)o;
            //System.out.println("Selected file = "+f.getAbsolutePath());
            UploadMetaData meta = myController.getUploadMeta(f);
            meta.setPublicFlag(true);
            meta.setTitle(txt_Title.getText());
            meta.setDescription(txtArea_Description.getText());
            //tag
            tags = txt_Tag.getText().split(",");
            tagList = new LinkedList<String>(Arrays.asList(tags));
            meta.setTags(tagList);
            //System.out.println("Seta title =["+ meta.getTitle() +"] to meta");
            myController.setPhotoUploadMeta(f, meta);
//            myController.setPhotoUploadToPhotoSet(f, (PhotoSetWrapper)cmb_PhotoSet.getSelectedItem());
        }
    }
    
}//GEN-LAST:event_btn_ApplyActionPerformed

private void btn_UploadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_UploadActionPerformed
    // TODO add your handling code here:
    if(myController == null || myUser == null){
        JOptionPane.showMessageDialog(null, "請先輸入使用者名稱");
        return;
    }
    final PhotoSetWrapper psw = (PhotoSetWrapper)cmb_PhotoSet.getSelectedItem();    
    int start = 0;
    int end = list_UploadList.getModel().getSize() - 1;
    if (end >= 0) {
      list_UploadList.setSelectionInterval(start, end); //force to select all
    }
    if(list_UploadList.getSelectedValues().length == 0 ){
        JOptionPane.showMessageDialog(null, "請先選擇照片");
    }else{
        Thread t = new Thread(){
            public void run()
            {
               String SelectedPSID = psw.getPhotoSet().getId();
               int cnt = 0;
               int failCnt = 0;
               int total =  list_UploadList.getSelectedValues().length;
               String pid = "";
               boolean photosetIsCreated = false;
               String newPhotoSetTitle = "";

               
               //start to get auth
               while(myController.getPermissionType().getType() < Permission.WRITE_TYPE){
                   try {
                       if (myController.authorizeEx(Permission.WRITE) != Comm.RetrunCode.SUCCESS) {
                           logger.log(Level.ERROR, "Auth failed!, please re-try");
                           JOptionPane.showMessageDialog(null, "認證失敗，請重試");
                           break;
                       }
                   } catch (IOException ex) {
                       logger.log(Level.ERROR, null, ex);
                   } catch (FlickrException ex) {
                       logger.log(Level.ERROR, null, ex);
                   } catch (SAXException ex) {
                       logger.log(Level.ERROR, null, ex);
                   } catch (URISyntaxException ex) {
                       logger.log(Level.ERROR, null, ex);
                   }
               }
               //start to upload image
               if(myController.getPermissionType().getType() >= Permission.WRITE_TYPE)
               {
                   lbl_uploadStatus.setText("開始上傳…");

                    if(!busyIconTimer.isRunning()){
                        busyIconTimer.start();
                    }
                    btn_Upload.setEnabled(false);
                    btn_Clear.setEnabled(false);
                    if(psw.getNeedCreateNewPhotoSet())
                        newPhotoSetTitle = JOptionPane.showInputDialog("請輸入相片集名稱");
                    
                    //因為建立PhotoSet一定要指定Primary PhotoID, 所以需要先上傳一張照片，再建立PhotoSet
                   for(Object o : list_UploadList.getSelectedValues()){
                        File f = (File)o;
                        if(!f.exists()){
                            logger.log(Level.ERROR, "照片不存在:"+ f.getName());
                            continue;
                        }
                        UploadMetaData meta = myController.getUploadMeta(f);
                        try {                            
                            if(meta.getTitle() == null || meta.getTitle().equalsIgnoreCase("") ){
                                meta.setTitle(f.getName()); //default value is file name
                            }

                            //Upload Photo, 如果SelectedPSID有設值，也會做assign
                            logger.log(Level.INFO, "Upload File["+f.getAbsolutePath()+"] to PhotoSet["+SelectedPSID+"]");
                            pid = myController.uploadPhoto(f, meta, SelectedPSID);
                            logger.log(Level.INFO, "Uploading successful, PhotoId = ["+pid+"]");
                            //Create and assign
                            if(psw.getNeedCreateNewPhotoSet()){
                                //create new photoset
                                if(!photosetIsCreated){
                                    logger.log(Level.INFO, "Ready to create a new photoset");                                    
                                    try {
                                        //會做assing的動作了, 只有第一張需要
                                        SelectedPSID = myController.createNewPhotoSet(newPhotoSetTitle, pid);
                                        photosetIsCreated = true;                                        
                                    } catch (FlickrException ex) {
                                        logger.log(Level.ERROR, ex.getErrorMessage());
                                        JOptionPane.showMessageDialog(null, "建立相片集失敗:"+ ex.getErrorMessage());
                                        //已上傳的第一張照片會成功上傳，剩下的會停止
                                        break;
                                    }
                                }
                            }//end of craete
                       
                            lbl_uploadStatus.setText((++cnt) +"/"+total); //status bar
                            //Thread.sleep(500);
                        //}   catch (InterruptedException ex) {
                        //    java.util.logging.Logger.getLogger(MyFlickrView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                        } catch (FlickrException ex) {
                            logger.log(Level.ERROR, ex.getErrorMessage(), ex);
                            failCnt++;
                        } catch (IOException ex) {
                            logger.log(Level.ERROR, null, ex);
                            failCnt++;
                        } catch (SAXException ex) {
                            logger.log(Level.ERROR, null, ex);
                            failCnt++;
                        } catch (URISyntaxException ex) {
                            logger.log(Level.ERROR, null, ex);
                            failCnt++;
                        }
                    }
                    JOptionPane.showMessageDialog(null, "上傳結束！\n總共："+total+"\n失敗："+failCnt);



               }               
               btn_Upload.setEnabled(true);
               btn_Clear.setEnabled(true);
                if(busyIconTimer.isRunning()){
                    busyIconTimer.stop();
                }
            }
        };
        logger.log(Level.INFO, "Start to upload image, current permission  = "+ myController.getPermissionType().toString());        
        try{
            t.start();
        }catch(Exception e){
            logger.log(Level.ERROR, "Something error in the upload thread: "+e.getMessage());
             btn_Upload.setEnabled(true);
             btn_Clear.setEnabled(true);
        }
    }
}//GEN-LAST:event_btn_UploadActionPerformed

private void btn_ClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ClearActionPerformed

    DefaultListModel listModel = (DefaultListModel) list_UploadList.getModel();
    if(listModel.getSize() > 0 && JOptionPane.showConfirmDialog(null, "是否移除全部照片？", "Oops", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
    {         
         listModel.removeAllElements();
         txt_Title.setText("");
         txtArea_Description.setText("");
         txt_Tag.setText("");
         if(myController != null){
             myController.cleanUploadMeta();
             lbl_photo.setText("");
         }
    }
}//GEN-LAST:event_btn_ClearActionPerformed

private void list_UploadListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_list_UploadListKeyPressed
    // TODO add your handling code here:
    DefaultListModel model = (DefaultListModel) list_UploadList.getModel();
    if(evt.getKeyChar() == java.awt.event.KeyEvent.VK_DELETE){
        int[] tmp = this.list_UploadList.getSelectedIndices();
        int[] selectedIndices = this.list_UploadList.getSelectedIndices();
        for (int i = tmp.length-1; i >=0; i--) {
            selectedIndices = list_UploadList.getSelectedIndices();
            model.removeElementAt(selectedIndices[i]);
  } // end-for
    }
}//GEN-LAST:event_list_UploadListKeyPressed

private void list_UploadListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_list_UploadListValueChanged
    //System.out.println("From Index = "+evt.getFirstIndex());
    //System.out.println("To Index = "+evt.getLastIndex());
}//GEN-LAST:event_list_UploadListValueChanged

private void list_UploadListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_list_UploadListKeyReleased
    if(evt.getKeyCode() == java.awt.event.KeyEvent.VK_UP || evt.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN){
        list_UploadListMousePressed(null);
//        lbl_photo.setText("");
//        File f = (File) list_UploadList.getSelectedValue();
//        if(f==null){return;}
//        BufferedImage image = null;
//        try {
//            image = ImageIO.read(f);
//        } catch (IOException ex) {
//            logger.log(Level.ERROR, null, ex);
//        }
//        BufferedImage resizedImage=ImageResizer.resize(image,200,200);
//        //System.out.println(f.getName());
//        lbl_photo.setIcon(new ImageIcon(resizedImage));
//        UploadMetaData meta = myController.getUploadMeta(f);
//        //restore data
//        txt_Title.setText(meta.getTitle());
//        txtArea_Description.setText(meta.getDescription());
//        if(meta.getTags()!=null){
//            StringBuffer strb = new StringBuffer();
//            for(String tag : meta.getTags()){
//                strb.append(tag);
//                strb.append(",");
//            }
//            txt_Tag.setText(strb.toString());
//        }else{
//            txt_Tag.setText("");
//        }
//        //cmb_PhotoSet.setSelectedItem(myController.getPhotoUploadToPhotoSet(f));
    }
}//GEN-LAST:event_list_UploadListKeyReleased

private void subSettingMenu_LocationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subSettingMenu_LocationActionPerformed
        
        JFileChooser chooser = new JFileChooser();
        
        chooser.setCurrentDirectory(new File(Comm.STORAGE_BASE));
        chooser.setDialogTitle("請選擇下載位置");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        chooser.setAcceptAllFileFilterUsed(false);            
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            //System.out.println("getCurrentDirectory(): " + chooser.getCurrentDirectory());
            //System.out.println("getSelectedFile() : " + chooser.getSelectedFile());
            configManager.setValue(ConfigManager.CONF_DOWNLOADPATH, chooser.getSelectedFile().toString());
        }
        currentDownloadPath = new File(configManager.getValue(ConfigManager.CONF_DOWNLOADPATH, Comm.STORAGE_BASE));
        if(!currentDownloadPath.isDirectory() || !currentDownloadPath.canWrite()){
            logger.log(Level.ERROR, "Selected folder error");
            JOptionPane.showMessageDialog(null, "所選資料夾錯誤");
            currentDownloadPath = defaultDownloadPath;
            configManager.setValue(ConfigManager.CONF_DOWNLOADPATH, Comm.STORAGE_BASE);
        }

}//GEN-LAST:event_subSettingMenu_LocationActionPerformed

private void btn_Start1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Start1ActionPerformed
    // TODO add your handling code here:
}//GEN-LAST:event_btn_Start1ActionPerformed

private void mnu_loadThumbnailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnu_loadThumbnailActionPerformed
    this._loadThumbnailToTable();
}//GEN-LAST:event_mnu_loadThumbnailActionPerformed

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
    // TODO add your handling code here:
}//GEN-LAST:event_aboutMenuItemActionPerformed

private void mnu_checkUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnu_checkUpdateActionPerformed
    // TODO add your handling code here:
    Thread updateCheck = new Thread(){
            public void run(){
                updateManager.initUpdateStatus();                
                if(updateManager.CheckNeedToUpdate()){
                    if(JOptionPane.showConfirmDialog(null, "發現新版本"+updateManager.getLatestVersion()+"，請至官網下載\n更新內容：\n"+updateManager.getUpdateDesc(), "Oops..", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                        mnu_GoToMyBlogActionPerformed(null);
                }else{
                    JOptionPane.showMessageDialog(null, "已經是最新版本，不用更新");
                }
            }
    };
    updateCheck.start();
}//GEN-LAST:event_mnu_checkUpdateActionPerformed

private void subSettingFileSizeLargeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subSettingFileSizeLargeActionPerformed
    configManager.setValue(ConfigManager.CONF_DOWNLOAD_SIZE, "Large");
}//GEN-LAST:event_subSettingFileSizeLargeActionPerformed

private void subSettingFileSizeOriginalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subSettingFileSizeOriginalActionPerformed
    configManager.setValue(ConfigManager.CONF_DOWNLOAD_SIZE, "Original");
}//GEN-LAST:event_subSettingFileSizeOriginalActionPerformed

private void subSettingMenu_ThumbnailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subSettingMenu_ThumbnailActionPerformed
    
}//GEN-LAST:event_subSettingMenu_ThumbnailActionPerformed

private void subSettingShowThumbnailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subSettingShowThumbnailActionPerformed
    Boolean bSelected = subSettingShowThumbnail.isSelected();
    configManager.setValue(ConfigManager.CONF_THUMBNAIL, bSelected.toString());
}//GEN-LAST:event_subSettingShowThumbnailActionPerformed

private void ck_privateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ck_privateActionPerformed
// TODO add your handling code here:
    if(ck_private.isSelected()){
        JOptionPane.showMessageDialog(null, "此選項僅能顯示您自己的私人照片，無法顯示他人的!\n認證流程將透過Flickr官方機制，請放心使用");
    }
}//GEN-LAST:event_ck_privateActionPerformed

private void tb_favPhotoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tb_favPhotoKeyReleased
    tb_PhotoListKeyReleased(evt);
}//GEN-LAST:event_tb_favPhotoKeyReleased

private void cmb_PhotoSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmb_PhotoSetActionPerformed
    ArrayList<PhotoSetWrapper> data = this.myUser.getPhotoSetListEx();
    PhotoSetWrapper psw = (PhotoSetWrapper)cmb_PhotoSet.getSelectedItem();
    logger.log(Level.DEBUG, "PhotoSet["+psw.getPhotoSet().getTitle()+"], id["+psw.getPhotoSet().getId()+"] is selected");

}//GEN-LAST:event_cmb_PhotoSetActionPerformed

private void subSettingAutoUpdateOnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subSettingAutoUpdateOnActionPerformed
    Boolean bSelected = subSettingAutoUpdateOn.isSelected();
    configManager.setValue(ConfigManager.CONF_CHECK_UPDATE, bSelected.toString());
}//GEN-LAST:event_subSettingAutoUpdateOnActionPerformed

private void subSettingMenu_AutoUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subSettingMenu_AutoUpdateActionPerformed
    // TODO add your handling code here:
}//GEN-LAST:event_subSettingMenu_AutoUpdateActionPerformed
    private void _rebuildExifToList(ArrayList<PhotoWrapper> list){
        stat = new StatisticData();            
            if(stat.initStatisticData(list)!= Comm.RetrunCode.SUCCESS){
                logger.log(Level.ERROR, "Init StatisticData is failed, it might be no photo for this user");
            }else{
                filterSpec =  new FilterSpec();

                // Set initial value of the list
                list_Camera.setListData(stat.getCameraList().toArray());
                list_Lens.setListData(stat.getLensIDList().toArray());
                list_Focal.setListData(stat.getFocalLengthList().toArray());
                list_Exposure.setListData(stat.getShutterList().toArray());
                list_Aperture.setListData(stat.getApertureList().toArray());
                list_Iso.setListData(stat.getIsoaList().toArray());

                // Filter spec and filter manager
                filterSpec.setSelectedCameraModel(stat.getCameraList());
                filterSpec.setSelectedLensId(stat.getLensIDList());
                filterSpec.setSelectedFocalLength(stat.getFocalLengthList());
                filterSpec.setSelectedAperture(stat.getApertureList());
                filterSpec.setSelectedShutter(stat.getShutterList());
                filterSpec.setSelectedIso(stat.getIsoaList());
                filterManager.setFilterSpec(filterSpec);
            }
    }
    private void _buildTable(UserWrapper user){
        _loadListToTable(user.getPhotoWrapperList(), this.tb_PhotoList);
        tabbed_MainPanel.setSelectedIndex(0);
        //_loadListToTable(user.getFavoritePhotos(), this.tb_favPhoto);
    }

    private void _loadPhotoSetToList(){
        ArrayList<PhotoSetWrapper> data = this.myUser.getPhotoSetListEx();
        list_photoset.setListData(data.toArray());
                
        
    }

    private void _loadPhotoSetToComboBox(){
        ArrayList<PhotoSetWrapper> data = this.myUser.getPhotoSetListEx();
        PhotoSetWrapper dummyPs_1 = myController.createNewPhotoSetWrapper("== 不建立相片集 =", null, false);
        PhotoSetWrapper dummyPs_2 = myController.createNewPhotoSetWrapper("== 建立新相片集 ==", null, true);
        cmb_PhotoSet.addItem(dummyPs_1);
        cmb_PhotoSet.addItem(dummyPs_2);
        for(PhotoSetWrapper ps : data){
            cmb_PhotoSet.addItem(ps);
        }
    }

    private void _loadThumbnailToTable(){
        Thread t = new Thread(){
        public void run(){
            int threadCount = 20;
            activeTable.setRowHeight(80);  //
            ArrayList<PhotoWrapper> list = getAllPhotoListFromTable();
            if(list == null || list.isEmpty()){
                return;
            }
            final DefaultTableModel defaultModel = (DefaultTableModel)activeTable.getModel();
            ExecutorService service = Executors.newFixedThreadPool(threadCount);
                for(int i=0;i<list.size();i++ ){
                    final PhotoWrapper p =list.get(i);
                    final HashMap<PhotoWrapper, Integer> map = new HashMap<PhotoWrapper, Integer>();
                    map.put(p, i); //use hash map to store the index of table
                    Runnable run = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String photoUrl = p.getPhoto().getThumbnailUrl();
                                int index = map.get(p);                                
                                logger.log(Level.DEBUG, "正在更新索引 " + index +" 的縮圖");
                                defaultModel.setValueAt(new ImageIcon(new URL(photoUrl)), index, 10); //10 is the column of thumbnail
                            } catch (MalformedURLException ex) {
                                logger.log(Level.ERROR, null, ex);
                            }
                        }
                    };
                    // 在未来某个时间执行给定的命令
                    service.execute(run);
                }
                    defaultModel.fireTableDataChanged();  //即時更新正確資訊, 必要!
            }
       };
       t.start();
    }
    
    private void _loadThumbnailToTable(final JTable table){
        Thread t = new Thread(){
        public void run(){
            int threadCount = 20;
            table.setRowHeight(80);  //
            ArrayList<PhotoWrapper> list = getAllPhotoListFromTable(table);
            if(list == null || list.isEmpty()){
                return;
            }
            final DefaultTableModel defaultModel = (DefaultTableModel)table.getModel();
            ExecutorService service = Executors.newFixedThreadPool(threadCount);
                for(int i=0;i<list.size();i++ ){
                    final PhotoWrapper p =list.get(i);
                    final HashMap<PhotoWrapper, Integer> map = new HashMap<PhotoWrapper, Integer>();
                    map.put(p, i); //use hash map to store the index of table
                    Runnable run = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String photoUrl = p.getPhoto().getThumbnailUrl();
                                int index = map.get(p);                                
                                logger.log(Level.DEBUG, "正在下載 " + p.getPhoto().getTitle() +" 的縮圖(id = "+p.getPhoto().getId()+"), url = ["+photoUrl+"]");
                                defaultModel.setValueAt(new ImageIcon(new URL(photoUrl)), index, 10); //10 is the column of thumbnail
                            } catch (MalformedURLException ex) {
                                logger.log(Level.ERROR, null, ex);
                            }
                        }
                    };
                    // 在未来某个时间执行给定的命令
                    service.execute(run);
                }
                    defaultModel.fireTableDataChanged();  //即時更新正確資訊, 必要!
            }
       };
       t.start();
    }

    private void _loadListToTable(ArrayList<PhotoWrapper> list, JTable table){        
        DefaultTableModel defaultModel = (DefaultTableModel)table.getModel();
        if(list == null || list.isEmpty() || list.isEmpty()){            
            return;
        }
        defaultModel.setRowCount(0);
        int number = 1;
        for(PhotoWrapper p: list){
            //logger.log(Level.DEBUG, p.getPhoto().getTitle() +" is visible? " +p.isVisiable());
            if(!p.isVisiable()){continue;}
            Vector<Object> temp = new Vector<Object>();
            temp.add(p);
            temp.add(number);
            p.setSequenceNumber(number);//讓 _refreshTable() 不會亂掉的關鍵之一！每個photowrapper都有自己的編號，而不是每次都用流水號
            number++;
            temp.add(p.getPhoto().getTitle());
            temp.add(p.getMeta().getExifDataWrapper().getCameraModel());
            temp.add(p.getMeta().getExifDataWrapper().getLensModel());
            temp.add(AsciiToDigit.atoi(p.getMeta().getExifDataWrapper().getFocalLength()));  //integer
            temp.add(AsciiToDigit.atof(p.getMeta().getExifDataWrapper().getAperture())); //float
            temp.add(p.getMeta().getExifDataWrapper().getExposure());
            temp.add(AsciiToDigit.atoi(p.getMeta().getExifDataWrapper().getIsoSpeed()));
            temp.add(p.getMeta().getExifDataWrapper().getDateTaken());
            //temp.add(p.getTagListToString());            
//            ImageIcon tmpIcon = null;
//            try {
//                tmpIcon = new ImageIcon(new URL(p.getPhoto().getThumbnailUrl()));
//            } catch (MalformedURLException ex) {
//                java.util.logging.Logger.getLogger(MyFlickrView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//            }
//            temp.add(tmpIcon);
            defaultModel.addRow(temp);            
        }                
        if(this.subSettingShowThumbnail.isSelected()){
            logger.log(Level.INFO, "Start to load thumbnails to table "+ table.getName());
            this._loadThumbnailToTable(table);            
        }else{
            //System.out.println(table.getRowHeight());
            //reset to default height
            table.setRowHeight(16);
        }
    }

    private  ArrayList<PhotoWrapper> getSelectedPhotoListFromTable(){
        ArrayList<PhotoWrapper> selectedUrls = new ArrayList<PhotoWrapper>();
        //dataInTable.clear();
        int[] selectedRows = activeTable.getSelectedRows();
        int selectedCount = selectedRows.length;
        if(selectedRows.length != 0){
            for(int i = 0;i<selectedCount;i++){
                PhotoWrapper tmpPhoto = (PhotoWrapper) activeTable.getModel().getValueAt(activeTable.getRowSorter().convertRowIndexToModel(selectedRows[i]), 0);   //photo object is in the 1th column
                if(tmpPhoto == null){return null;}
                //logger.log(Level.DEBUG, "Selected Photo Url = "+tmpPhoto.getPhoto().getUrl());
                selectedUrls.add(tmpPhoto);
            }
            logger.log(Level.DEBUG, "Selected Photo count = "+ selectedUrls.size());
            return selectedUrls;
        }else{
            logger.log(Level.DEBUG, "None of photo is selected");
            JOptionPane.showMessageDialog(null, "請先選擇照片!");
            return selectedUrls;
        }
    }

    private PhotoWrapper getSelectedPhotoFromTableByIndex(int i){
        try{
            return this.getSelectedPhotoListFromTable().get(i);
        } catch(IndexOutOfBoundsException e){
            logger.log(Level.ERROR, "IndexOutOfBoundsException");
            return null;
        } catch(NullPointerException e){
            logger.log(Level.ERROR, "Null pointer, Cant not get Photo Instance from JTable");
            return null;
        }
    }

    private  ArrayList<PhotoWrapper> getAllPhotoListFromTable(){
        //用統一的變數，會造成multithread同時在更新exif與點選table時產生錯誤
        //例如：選了5張照片更新exif, 更新到第二張時，點選table任意一張照片，因為選擇了"一張"照片，使得更新exif的thread抓到錯的資料 (以為只要更新一張)
        //故必需獨立出來
        ArrayList<PhotoWrapper> selectedUrls = new ArrayList<PhotoWrapper>();
        //dataInTable.clear();
        int selectedCount = activeTable.getRowCount();
        for(int i = 0;i<selectedCount;i++){
            PhotoWrapper tmpPhoto = (PhotoWrapper) activeTable.getModel().getValueAt(activeTable.getRowSorter().convertRowIndexToModel(i), 0);   //photo object is in the 9th column
            //logger.log(Level.DEBUG, "tmpPhoto url = "+tmpPhoto.getPageUrl());
            //Integer originalIndex = (Integer) tb_PhotoList.getModel().getValueAt(tb_PhotoList.getRowSorter().convertRowIndexToModel(i), 1);
            selectedUrls.add(tmpPhoto);
        }
        return selectedUrls;
    }
    private  ArrayList<PhotoWrapper> getAllPhotoListFromTable(JTable table){
        //用統一的變數，會造成multithread同時在更新exif與點選table時產生錯誤
        //例如：選了5張照片更新exif, 更新到第二張時，點選table任意一張照片，因為選擇了"一張"照片，使得更新exif的thread抓到錯的資料 (以為只要更新一張)
        //故必需獨立出來
        ArrayList<PhotoWrapper> selectedUrls = new ArrayList<PhotoWrapper>();
        //dataInTable.clear();
        int selectedCount = table.getRowCount();
        for(int i = 0;i<selectedCount;i++){
            PhotoWrapper tmpPhoto = (PhotoWrapper) table.getModel().getValueAt(table.getRowSorter().convertRowIndexToModel(i), 0);   //photo object is in the 9th column
            //logger.log(Level.DEBUG, "tmpPhoto url = "+tmpPhoto.getPageUrl());
            //Integer originalIndex = (Integer) tb_PhotoList.getModel().getValueAt(tb_PhotoList.getRowSorter().convertRowIndexToModel(i), 1);
            selectedUrls.add(tmpPhoto);
        }
        return selectedUrls;
    }

    private void _setMyComponentEnable(boolean b){
        for(JComponent c : this.myComponents){
            c.setEnabled(b);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JFrame Frame_Debug;
    javax.swing.JButton btn_AddToBatch;
    javax.swing.JButton btn_Apply;
    javax.swing.JButton btn_Clear;
    javax.swing.JButton btn_DisplayOriginal;
    javax.swing.JButton btn_Export;
    javax.swing.JButton btn_FilterQuery;
    javax.swing.JButton btn_FilterReset;
    javax.swing.JButton btn_HtmlCodeGen;
    javax.swing.JButton btn_RemoveAll;
    javax.swing.JButton btn_RemoveFromBatch;
    javax.swing.JButton btn_SelectAll;
    javax.swing.JButton btn_SelectAll1;
    javax.swing.JButton btn_ShowStasticChart;
    javax.swing.JButton btn_Start;
    javax.swing.JButton btn_Start1;
    javax.swing.JButton btn_TagAdd;
    javax.swing.JButton btn_TagSet;
    javax.swing.JButton btn_Upload;
    javax.swing.JButton btn_backup;
    javax.swing.JButton btn_backupSelectAll;
    javax.swing.JButton btn_clear;
    javax.swing.JButton btn_initUser;
    javax.swing.JButton btn_reload;
    javax.swing.JButton btn_searchByTag;
    javax.swing.JLabel buddyIcon;
    javax.swing.JCheckBox ck_Aperture;
    javax.swing.JCheckBox ck_CameraModel;
    javax.swing.JCheckBox ck_Exposure;
    javax.swing.JCheckBox ck_FocalLength;
    javax.swing.JCheckBox ck_HighResDisplay;
    javax.swing.JCheckBox ck_Iso;
    javax.swing.JCheckBox ck_LensId;
    javax.swing.JCheckBox ck_photoTitle;
    javax.swing.JCheckBox ck_phtoDescription;
    javax.swing.JCheckBox ck_private;
    javax.swing.JComboBox cmb_PhotoSet;
    javax.swing.JComboBox comboList;
    javax.swing.JComboBox combo_RangeToList;
    javax.swing.JComboBox combo_SizeSelection;
    javax.swing.JMenu contactMenu;
    javax.swing.JMenuItem debugMenuItm;
    javax.swing.JMenuItem dirMenuItem;
    javax.swing.JMenu groupMenu;
    javax.swing.JLabel jLabel1;
    javax.swing.JLabel jLabel10;
    javax.swing.JLabel jLabel12;
    javax.swing.JLabel jLabel13;
    javax.swing.JLabel jLabel14;
    javax.swing.JLabel jLabel15;
    javax.swing.JLabel jLabel16;
    javax.swing.JLabel jLabel17;
    javax.swing.JLabel jLabel18;
    javax.swing.JLabel jLabel2;
    javax.swing.JLabel jLabel20;
    javax.swing.JLabel jLabel21;
    javax.swing.JLabel jLabel23;
    javax.swing.JLabel jLabel3;
    javax.swing.JLabel jLabel4;
    javax.swing.JLabel jLabel5;
    javax.swing.JLabel jLabel6;
    javax.swing.JLabel jLabel7;
    javax.swing.JLabel jLabel8;
    javax.swing.JLabel jLabel9;
    javax.swing.JLabel jLable20;
    javax.swing.JPanel jPanel1;
    javax.swing.JPanel jPanel10;
    javax.swing.JPanel jPanel11;
    javax.swing.JPanel jPanel12;
    javax.swing.JPanel jPanel13;
    javax.swing.JPanel jPanel14;
    javax.swing.JPanel jPanel2;
    javax.swing.JPanel jPanel3;
    javax.swing.JPanel jPanel4;
    javax.swing.JPanel jPanel5;
    javax.swing.JPanel jPanel6;
    javax.swing.JPanel jPanel7;
    javax.swing.JPanel jPanel8;
    javax.swing.JPanel jPanel9;
    javax.swing.JScrollPane jScrollPane1;
    javax.swing.JScrollPane jScrollPane10;
    javax.swing.JScrollPane jScrollPane11;
    javax.swing.JScrollPane jScrollPane12;
    javax.swing.JScrollPane jScrollPane13;
    javax.swing.JScrollPane jScrollPane14;
    javax.swing.JScrollPane jScrollPane2;
    javax.swing.JScrollPane jScrollPane3;
    javax.swing.JScrollPane jScrollPane4;
    javax.swing.JScrollPane jScrollPane5;
    javax.swing.JScrollPane jScrollPane6;
    javax.swing.JScrollPane jScrollPane7;
    javax.swing.JScrollPane jScrollPane8;
    javax.swing.JScrollPane jScrollPane9;
    javax.swing.JSeparator jSeparator2;
    javax.swing.JSeparator jSeparator3;
    javax.swing.JSeparator jSeparator4;
    javax.swing.JSeparator jSeparator5;
    javax.swing.JLabel lbl_PhotoListStatus;
    javax.swing.JLabel lbl_UploadArea;
    javax.swing.JLabel lbl_batchCount;
    javax.swing.JLabel lbl_icon;
    javax.swing.JLabel lbl_photo;
    javax.swing.JLabel lbl_uploadStatus;
    javax.swing.JList list_Aperture;
    javax.swing.JList list_Camera;
    javax.swing.JList list_Exposure;
    javax.swing.JList list_Focal;
    javax.swing.JList list_Iso;
    javax.swing.JList list_Lens;
    javax.swing.JList list_TagList;
    javax.swing.JList list_UploadList;
    javax.swing.JList list_photoset;
    javax.swing.JPanel mainPanel;
    javax.swing.JMenuBar menuBar;
    javax.swing.JMenuItem mnu_DownloadPhoto;
    javax.swing.JMenuItem mnu_GetExtraData;
    javax.swing.JMenuItem mnu_GoToMyBlog;
    javax.swing.JMenuItem mnu_SelectAll;
    javax.swing.JMenuItem mnu_checkUpdate;
    javax.swing.JMenuItem mnu_loadThumbnail;
    javax.swing.JPopupMenu mnu_popMenu;
    javax.swing.JMenuItem mnu_refresh;
    javax.swing.JPanel panel_Backup;
    javax.swing.JPanel panel_EditTag;
    javax.swing.JPanel panel_HtmlGen;
    javax.swing.JPanel panel_PhotoCount;
    javax.swing.JPanel panel_SearchByExif;
    javax.swing.JPanel panel_StatisticData;
    javax.swing.JPanel panel_Upload;
    javax.swing.JProgressBar progressBar;
    javax.swing.ButtonGroup rdoGrp_DownloadSize;
    javax.swing.ButtonGroup rdoGrp_Exif;
    javax.swing.JScrollPane scPane_favoPhotoList;
    javax.swing.JScrollPane scPane_groupPhotoList;
    javax.swing.JScrollPane scPane_photoList;
    javax.swing.JMenu settingMenu;
    javax.swing.JLabel statusAnimationLabel;
    javax.swing.JPanel statusPanel;
    javax.swing.JRadioButtonMenuItem subSettingAutoUpdateOn;
    javax.swing.JRadioButtonMenuItem subSettingFileSizeLarge;
    javax.swing.JRadioButtonMenuItem subSettingFileSizeOriginal;
    javax.swing.JMenu subSettingMenu_AutoUpdate;
    javax.swing.JMenuItem subSettingMenu_Location;
    javax.swing.JMenu subSettingMenu_Size;
    javax.swing.JMenu subSettingMenu_Thumbnail;
    javax.swing.JRadioButtonMenuItem subSettingShowThumbnail;
    javax.swing.JTabbedPane tab_FunctionPannel;
    javax.swing.JTabbedPane tabbed_MainPanel;
    javax.swing.JTable tb_PhotoList;
    javax.swing.JTable tb_favPhoto;
    javax.swing.JTable tb_groupPhoto;
    javax.swing.JToggleButton tgBtn_displayTempList;
    javax.swing.JTextArea txtArea_DebugLog;
    javax.swing.JTextArea txtArea_Description;
    javax.swing.JTextArea txtArea_PhotoDownloadLog;
    javax.swing.JTextArea txtArea_Tag;
    javax.swing.JTextArea txt_HtmlCode;
    javax.swing.JTextField txt_PhotoCount;
    javax.swing.JTextField txt_Tag;
    javax.swing.JTextField txt_Title;
    javax.swing.JTextField txt_username;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;

    private void _initComboBoxValue() {
        int defaultSelectedIndex = 1;
        
        //combo_RangeToList.addItem(Comm.LABEL_1);
        combo_RangeToList.addItem(Comm.LABEL_10);
        combo_RangeToList.addItem(Comm.LABEL_50);
        combo_RangeToList.addItem(Comm.LABEL_100);
        combo_RangeToList.addItem(Comm.LABEL_300);
        combo_RangeToList.addItem(Comm.LABEL_500);
        combo_RangeToList.addItem(Comm.LABEL_1000);
        //combo_RangeToList.addItem(Comm.LABEL_1500);
        combo_RangeToList.addItem(Comm.LABEL_3000);
        combo_RangeToList.addItem(Comm.LABEL_ALL);
        combo_RangeToList.setSelectedIndex(defaultSelectedIndex);
    }

    private void _buildPieChartData(ArrayList<PhotoWrapper> list) {
        if(list == null){
            logger.log(Level.DEBUG, "Input list = null, use ALL user photo list instead. : _buildPieChartData(list)");
            list = this.getAllPhotoListFromTable(); //如果是null, 代表沒選，則用所有的list
        }
        if(this.myUser == null){
            logger.log(Level.ERROR, "User = null");
        }
        pieChart_Camera = new PieChartFrame("相機型號統計", Comm.SelectType.CAMERA_MODEL, list);
        pieChart_LensID = new PieChartFrame("鏡頭ID統計", Comm.SelectType.LENS_ID, list);
        pieChart_FocolLength = new PieChartFrame("焦長資料統計", Comm.SelectType.FOCAL_LENGTH, list);
        pieChart_ShutterSpeed = new PieChartFrame("快門速度統計", Comm.SelectType.SHUTTER_SPEED, list);
        pieChart_ApertureValue =new PieChartFrame("光圈值統計", Comm.SelectType.APERUTRE_VALUE, list);
        pieChart_IsoSpeed = new PieChartFrame("感光度值統計", Comm.SelectType.ISO_SPEED, list);
    }

    private void _resetJListSelection() {
        list_Camera.clearSelection();
        list_Aperture.clearSelection();
        list_Iso.clearSelection();
        list_Exposure.clearSelection();
        list_Focal.clearSelection();
        list_Lens.clearSelection();
    }

    private void _registComponents(JComponent c){
        this.myComponents.add(c);
        c.setEnabled(true);
    }

    private void _displayPhotoTag(PhotoWrapper p) {        
        if(p == null){return;}
        //do not move the stringbuilder to member field, it will cuase incorrenct result in multithread. (duplicated display)
        StringBuilder tempMsgBuffer = new StringBuilder();
        tempMsgBuffer.delete(0, tempMsgBuffer.length());
        //display tag
        //txtAera_TagLog.append("顯示相片["+selectedPhoto.getPhoto().getId()+"] 的標籤...\n");
        ArrayList<Tag> tags = myController.getTagsByPhoto(p.getPhoto());
        for(Tag t : tags){
            //txtArea_Tag.append(t.getRaw()+",");
            tempMsgBuffer.append(t.getRaw()).append(",");
            //tempMsgBuffer.append(t.getValue()+",");
            //logger.log(Level.DEBUG, "Tag = "+ t.getRaw());
            //logger.log(Level.DEBUG, "tempMsgBuffer = "+tempMsgBuffer.toString());
            //txtAera_TagLog.append("Tag: "+t.getRaw()+"\n");
            //txtAera_TagLog.append("Tag: "+t.getValue()+"\n");
        }
        //logger.log(Level.DEBUG, "Final tempMsgBuffer = "+tempMsgBuffer.toString());
        txtArea_Tag.setText("");
        txtArea_Tag.setText(tempMsgBuffer.toString());
    }

    private Comm.RetrunCode _updatePhotoTag(ArrayList<PhotoWrapper> list, Tag_Method tag_Method) {
        Comm.RetrunCode rt = Comm.RetrunCode.UNDEF;
        Comm.RetrunCode final_rt = Comm.RetrunCode.SUCCESS;
        if (myController == null || myUser == null || list == null || list.size()==0) {
            JOptionPane.showMessageDialog(null, "請先選擇照片");
            return Comm.RetrunCode.ERROR;
        }
        try {
            Permission p = myController.getPermissionType();
            logger.log(Level.DEBUG, "Current permission = "+p.getType());
            //if(p.getType() < Permission.WRITE_TYPE){
                //logger.log(Level.INFO, "Insufficient permission: "+ p.getType() +", request Write permission: "+ Permission.WRITE_TYPE);
                //使用multithread每次都要重新取得認證
            //if (myController.authorize() == Comm.RetrunCode.SUCCESS) {
            if (myController.authorizeEx(Permission.WRITE) == Comm.RetrunCode.SUCCESS) {
                logger.log(Level.INFO, "Write permission is granted successfully");
            }else{
                logger.log(Level.ERROR, "Grant Write permission is failed");
                return Comm.RetrunCode.PERMISSION_DENIED;
            }
            String rawTags = txtArea_Tag.getText();
            String[] tags = rawTags.split(",");
            for(PhotoWrapper pw : list) {
                if(tag_Method == Comm.Tag_Method.SET){
                    rt = myController.setTag(pw.getPhoto(), tags);
                }
                if(tag_Method == Comm.Tag_Method.ADD){
                    rt = myController.addTag(pw.getPhoto(), tags);
                }
                if (rt == Comm.RetrunCode.SUCCESS) {
                    myController.updatePhoto(pw);
                    logger.log(Level.INFO, "Add/Set Tag: ["+ rawTags +"] to Photo ["+pw.getPhoto().getTitle()+"] is successed!");
                } else {
                    logger.log(Level.INFO, "Set Tag: ["+ rawTags +"] to Photo ["+pw.getPhoto().getTitle()+"] is failed!");
                    final_rt = Comm.RetrunCode.ERROR;
                    //JOptionPane.showMessageDialog(null, "寫入標籤失敗，可能是權限不足或是網路連線中斷: 照片 ["+pw.getPhoto().getTitle()+"]");
                }
            }
        } catch (URISyntaxException ex) {
            logger.log(Level.ERROR, "認證失數，請再試一次", ex);
        } catch (SAXException ex) {
            logger.log(Level.ERROR, "寫入標籤失敗，請重新設定", ex);
        } catch (IOException ex) {
            logger.log(Level.ERROR, "寫入標籤失敗，請重新設定", ex);
            return Comm.RetrunCode.ERROR;
        } catch (FlickrException ex) {
            logger.log(Level.ERROR, "錯誤："+ex.getErrorMessage(), ex);
            return Comm.RetrunCode.ERROR;
        }
        return final_rt;
    }

    private void _getExifData() {
         ArrayList<PhotoWrapper> selectedPhoto = getSelectedPhotoListFromTable();
        if(selectedPhoto == null || selectedPhoto.size()==0) {return;}
        int threadCount = 8; //too many threads will cause connection timeout
        if( myPhotoTask!=null && !(myPhotoTask.isDone())){
            JOptionPane.showMessageDialog(null, "資料處理中..");
            return;
        }
        if(myPhotoThread.isAlive()){
            JOptionPane.showMessageDialog(null, "資料處理中..");
            return;
        }

        if(selectedPhoto !=null && selectedPhoto.size()>0)
        {            
            exifWorker.setPhotoList(selectedPhoto);
            exifWorker.setThreadCount(threadCount);
            myPhotoThread = new Thread(exifWorker);
            myPhotoThread.start();            
            Thread updater = new Thread(new ProgressUpdater(Comm.PROGRESS_TYPE_PHOTO_EXIF, selectedPhoto.size()));
//                //wait until the engine is ready, otherwise, it will cause incorrect display
            //this method will cause unknow always-sleep status and let the app hang.
//            while(true){
//                //for unknown reason, add this line will skip hang issue on some machine
//                logger.log(Level.DEBUG, "Engine status:"+ myController.queryCurrentStatus());
//                if(myController.queryCurrentStatus() == FlickrMiner.STATUS_READY){
//                    logger.log(Level.DEBUG, "Engine status:"+ myController.queryCurrentStatus());
//                    break;
//                }
//            }
            updater.start();
        }
    }




    public class FlickrMinerEngineWorker implements Runnable{
        int rangeToList;
        public FlickrMinerEngineWorker(String range){            
            try {
                this.rangeToList = Integer.parseInt(range);
                System.out.println("Range To List = "+this.rangeToList);
            } catch(NumberFormatException e) {
                // "ALL" is selected                
                this.rangeToList = Integer.MAX_VALUE;
            }

        }
        public void run() {
//            if(txt_username.getText().isEmpty()){
//                JOptionPane.showMessageDialog(mainPanel, "請輸入使用者帳號!");
//                return;
//            }
//            // Progress bar thread
//            _setMyComponentEnable(false);
//            try{
//                progress1 = new Thread(new ProgressUpdater(Comm.PROGRESS_TYPE_PHOTO, rangeToList));
//                //progress2 = new Thread(new ProgressUpdater(Comm.PROGRESS_TYPE_PHOTO_EXIF, rangeToList));
//                busyIconTimer.start();
//                myUser = myController.initUser(txt_username.getText());
//                progress1.start();
//                Boolean bExif = false;
//                if(myUser != null){
//                    if(myController.buildUserData(this.rangeToList, bExif) == Comm.RetrunCode.SUCCESS){
//                        _rebuildExifToList(myUser.getPhotoWrapperList());
//                        _buildTable(myUser);  //renew table for final data
//                        JOptionPane.showMessageDialog(mainPanel, "完成！");
//                    }else{
//                        JOptionPane.showMessageDialog(mainPanel, "buildUserData() failed, please report your username to mongcheng@gmail.com.");
//                    }
//                }else{
//                    JOptionPane.showMessageDialog(mainPanel, "查無使用者/尚未初始化，或是連線逾時");
//                }
//            }catch (Exception e){
//                logger.log(Level.ERROR, "Error occurs in FlickrMinerEngineWorker.run()", e);
//                _setMyComponentEnable(true);
//                busyIconTimer.stop();
//            }
//            _setMyComponentEnable(true);
//            busyIconTimer.stop();
        }
    }

@Action
public Task buildMyPhotoAction(){
    myPhotoTask = new BuildMyPhotoTask(Application.getInstance());
    ApplicationContext C = getApplication().getContext();
    TaskMonitor M = C.getTaskMonitor();
    TaskService S = C.getTaskService();
    S.execute(myPhotoTask);
    M.setForegroundTask(myPhotoTask);
    return myPhotoTask;
}

private class BuildMyPhotoTask extends Task<Void, Void> { // this is the Task
    BuildMyPhotoTask(Application app) {
        super(app);
    }

    @Override
    protected Void doInBackground() {
        int rangeToList;
        String strPhotoCount = "";        
        if(myUser == null || myController == null){
            JOptionPane.showMessageDialog(null, "使用者尚未初始化");
            return null;
        }
        if(myPhotoThread.isAlive() || myTagThread.isAlive()){
            JOptionPane.showMessageDialog(null, "忙碌中，請稍後再試..");
            return null;
        }
        
        if(txt_PhotoCount.getText().equalsIgnoreCase("")){
            strPhotoCount = combo_RangeToList.getSelectedItem().toString();
        }else{
            strPhotoCount = txt_PhotoCount.getText();
        }
        try {
            rangeToList = Integer.parseInt(strPhotoCount);
        } catch(NumberFormatException e) {
            // "ALL" is selected
            rangeToList = Integer.MAX_VALUE;
        }
        if(txt_username.getText().isEmpty()){
                JOptionPane.showMessageDialog(mainPanel, "請輸入使用者帳號!");
                return null;
            }            
        _setMyComponentEnable(false);
        try{
            progress1 = new Thread(new ProgressUpdater(Comm.PROGRESS_TYPE_PHOTO, rangeToList));
            progress1.start();            
            setMessage("starting up");// status message
            //progress1.start();
            Boolean bExif = false;
            Boolean bPrivate = ck_private.isSelected();
            Comm.RetrunCode rt = Comm.RetrunCode.SUCCESS;
            if(myUser != null){                
                //if(myController.buildUserData(rangeToList, bExif) == Comm.RetrunCode.SUCCESS){
                if(bPrivate){
                    rt = myController.authorizeEx(Permission.READ);
                }
                if(rt == Comm.RetrunCode.SUCCESS){
                    if(myController.buildUserDataEx(rangeToList, bExif, bPrivate) == Comm.RetrunCode.SUCCESS){
                        _rebuildExifToList(myUser.getPhotoWrapperList());
                        _buildTable(myUser);  //renew table for final data
                        JOptionPane.showMessageDialog(mainPanel, "完成！");
                    }else{
                        JOptionPane.showMessageDialog(mainPanel, "無法取得使用者資料，請重試 (可能是連線中斷)");
                    }
                }else if(rt == Comm.RetrunCode.AUTH_NOT_MATCHED){
                    JOptionPane.showMessageDialog(mainPanel, "認証失敗，無法找到 " + myUser.getUsername() + " 的認證授權");
                }
                else{
                    JOptionPane.showMessageDialog(mainPanel, "無法取得授權");
                }
            }else{
                JOptionPane.showMessageDialog(mainPanel, "查無使用者/尚未初始化，或是連線逾時");
            }
        }catch (Exception e){
            logger.log(Level.ERROR, "Error occurs in FlickrMinerEngineWorker.run()", e);
            _setMyComponentEnable(true);
        }
        _setMyComponentEnable(true);
        setMessage("done");// status message
        return null;
    }
}

@Action
public Task initUserAction() { // this sets up the Task and TaskMonitor
    InitUser task = new InitUser(Application.getInstance());

    ApplicationContext C = getApplication().getContext();
    TaskMonitor M = C.getTaskMonitor();
    TaskService S = C.getTaskService();
    S.execute(task);
    M.setForegroundTask(task);

    return task;
}

private class InitUser extends Task<Void, Void> { // this is the Task
    InitUser(Application app) {
        super(app);
        this.setTitle("Init user thread");
    }

    @Override
    protected Void doInBackground() {
        try {
            // specific code for your task
            // this code shows progress bar with status message for a few seconds
            setMessage("starting up");// status message
            
            if(myPhotoThread.isAlive() || myTagThread.isAlive()){
                JOptionPane.showMessageDialog(null, "忙碌中，請稍後再試..");
                return null;
            }
            list_photoset.setListData(new Object[0]);
            if(txt_username.getText().equalsIgnoreCase("")){
                JOptionPane.showMessageDialog(null, "請輸入使用者名稱");
                return null;
            }
            lbl_photo.setIcon(null);
            lbl_photo.setText("初使化使用者資料開始..請稍待");        
            btn_initUser.setEnabled(false);
            logger.log(Level.INFO, "初使化使用者資料開始..."+txt_username.getText());
            //clear tatble
            DefaultTableModel defaultModel = (DefaultTableModel)tb_PhotoList.getModel();
            defaultModel.setRowCount(0);

            defaultModel = (DefaultTableModel)tb_favPhoto.getModel();
            defaultModel.setRowCount(0);

            defaultModel = (DefaultTableModel)tb_groupPhoto.getModel();
            defaultModel.setRowCount(0);
                myUser = myController.initUser(txt_username.getText());
                if(myUser == null || myUser.getUser() == null || myUser.getUsername().equalsIgnoreCase("")){
                    JOptionPane.showMessageDialog(null, "初始化失敗!請檢查使用者名稱或網路連線");
                    lbl_photo.setText("初始化失敗");
                }else{
                    logger.log(Level.INFO, "初使化使用者資料完成");
                    lbl_photo.setText("照片預覽");
                    Thread t ;
                    t = new Thread(){
                        @Override
                        public void run(){
                            try {
                                lbl_icon.setIcon(new ImageIcon(new URL(myUser.getBuddyIconUrl())));
                            } catch (MalformedURLException ex) {
                                logger.log(Level.ERROR, null, ex);
                            }
                        }
                    };
                    t.start();
                    _loadPhotoSetToList();
                    _loadPhotoSetToComboBox();
                    _loadTagToList();
                    _loadListToTable(myUser.getFavoritePhotos(), tb_favPhoto);
                    _loadContactList();
                    _loadGroupList();
                    JOptionPane.showMessageDialog(null, "Hi, "+myUser.getUsername()+", 歡迎使用 Flickr 小幫手 :)");
                }
                btn_initUser.setEnabled(true);


//            for(int progress=0; progress<100; progress += (int)(Math.random()*10)) {
//                setProgress(progress); // progress bar (0-100)
//                setMessage("prog: "+progress); // status message
//                try {
//                    Thread.sleep((long)500); // sleep 500ms
//                } catch (InterruptedException ignore) {
//                }
//            }
            setMessage("done");// status message
        }catch(FlickrException e){
            JOptionPane.showMessageDialog(null, "初始化失敗!\n請輸入正確的名稱\n或是檢查帳號是否允許公開搜尋\n(設定網頁：https://www.flickr.com/account/prefs/optout/?from=privacy)\n"+e.getErrorMessage());
            logger.log(Level.ERROR, e.getErrorMessage(), e);
            btn_initUser.setEnabled(true);
        }catch(java.lang.Exception e) {
            JOptionPane.showMessageDialog(null, "初始化失敗!\n"+e);
            e.printStackTrace();
            logger.log(Level.ERROR, e);
            btn_initUser.setEnabled(true);
        }
        return null;
    }

    protected void succeeded() {
    }
    }

    public class ProgressUpdater implements Runnable{
        int type = 0;
        int totalValue = 0;

        public ProgressUpdater(int type, int totalValue){
            progressBar.setIndeterminate(false);
            this.type = type;
            this.totalValue = totalValue;
        }
        public void run() {
            try {
                Thread.sleep(500); //workaround to avoid app hang or get the incorrect display (get the old data) from other thread
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(MyFlickrView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
            int currentValue = 0;

            progressBar.setValue(0);
            lbl_PhotoListStatus.setText(0 + "/"+totalValue);            

            if(totalValue == Integer.MAX_VALUE || totalValue > myUser.getPhotosCount()){
                totalValue = myUser.getPhotosCount();
            }
            progressBar.setVisible(true);
            progressBar.setEnabled(true);

            //exifProgressBar.setVisible(true);
            //exifProgressBar.setEnabled(true);

            progressBar.setMaximum(totalValue);
            //exifProgressBar.setMaximum(totalValue);
                        
            while(true){
                if(type == Comm.PROGRESS_TYPE_PHOTO){currentValue = myController.queryCurrentProcessedPhotoCount() ;}
                if(type == Comm.PROGRESS_TYPE_PHOTO_EXIF){currentValue = myController.queryCurrentProcessedExifPhotoCount() ;}
                if(type == Comm.PROGRESS_TYPE_DOWNLOAD){currentValue = downloadManager.finishedCount ;}
                //put here to define your current value
                lbl_PhotoListStatus.setText(currentValue + "/"+totalValue);
                progressBar.setValue(currentValue);

                //中止條件
                //下載照片的終止條件
                if(type == Comm.PROGRESS_TYPE_DOWNLOAD){
                    if(currentValue >= totalValue ){
                        logger.log(Level.DEBUG, "Break ProgressUpdater due to, and current Value = "+ currentValue+", total value = "+totalValue);
                        break;
                    }
                }else{ //exif和照片清單的終止條件
                    if(myController.queryCurrentStatus() == FlickrMiner.STATUS_FINISH  && currentValue >= totalValue ){
                        //有可能拿到舊的值，造成break
                        //兩個條件都要存在，不然 mutltithread 可能會發生"任一thread結束，本thread就結束的情況"
                        logger.log(Level.DEBUG, "Break ProgressUpdater due to engine's status, and current Value = "+ currentValue+", total value = "+totalValue);
                        break;
                    }
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    logger.log(Level.ERROR, null, ex);
                }
            }
            //exifProgressBar.setValue(0);
            //progressBar.setValue(0);
        }
    }

    public class ExifWorker implements Runnable{
        private ArrayList<PhotoWrapper> selectedPhoto;
        private int threadCount = 1; //default value
        public void setPhotoList(ArrayList<PhotoWrapper> list){
            this.selectedPhoto = list;
        }
        public void setThreadCount(int count){
            this.threadCount = count;
        }
        public void run(){
            if(this.selectedPhoto == null || myController==null){return;}
                busyIconTimer.start();
                FlickrMiner.TOTAL_FAIL_COUNT_ON_EXIF_FETCHING = 0; //reset counter, it will be updated in FlickrMiner
                if(myController.fetchExifData(selectedPhoto,threadCount)==Comm.RetrunCode.SUCCESS){
                    //btn_reloadActionPerformed(null);
                    logger.log(Level.INFO, "Exif is paresed complete");
                    _refreshTable();
                    _rebuildExifToList(getAllPhotoListFromTable());
                    if(FlickrMiner.TOTAL_FAIL_COUNT_ON_EXIF_FETCHING > 0){
                        JOptionPane.showMessageDialog(null, "完成, 失敗數目 = "+FlickrMiner.TOTAL_FAIL_COUNT_ON_EXIF_FETCHING+"\n(可能是連線逾時或是照片不開放 Exif 資訊)");
                    }else{
                        JOptionPane.showMessageDialog(null, "完成");
                    }

                }else{
                    JOptionPane.showMessageDialog(null, "失敗，請查閱Log");
                    logger.log(Level.INFO, "Exif is paresed failed");
                }
                busyIconTimer.stop();
            }
    }
    
    private class DownloadTask implements Runnable {

    private String toBeSavedUrl;
    private File albumPath;
    private File savedFile;
    private int retryCount = 10;

    public DownloadTask(String toBeSavedUrl, File albumPath, File savedFile) {
        this.toBeSavedUrl = toBeSavedUrl;
        this.albumPath = albumPath;
        this.savedFile = savedFile;
    }

    @Override
    public void run() {
        while(retryCount>=0){                    
            try {                
                logger.log(Level.INFO, "Start to download ["+toBeSavedUrl+"] to ["+albumPath+"]...");
                downloadManager.downloadFile(toBeSavedUrl, albumPath, savedFile);
                logger.log(Level.INFO, "Download ["+toBeSavedUrl+"] to ["+albumPath+"] complete");
                break;
            } catch(FileNotFoundException ex){
                //file name is invalid

            } catch (MalformedURLException ex) {
                logger.log(Level.ERROR, "URL is invalid", ex);
                break;
            } catch (IOException ex) {                
                retryCount--;
                logger.log(Level.ERROR, "Retry..remaining = "+retryCount, ex);
            } catch (Exception ex) {                
                retryCount--;
                logger.log(Level.ERROR, "Unknown exception, retry", ex);
            }
        }
    }
}

    public class DownloadWorker /*implements Runnable*/{
        private ArrayList<PhotoWrapper> selectedPhoto;
        private String folderName ;
        private JTextArea textArea;
        private File albumPath;
        public void setPhotoList(ArrayList<PhotoWrapper> list){
            this.selectedPhoto = list;
        }
        public void setFolderName(String name){
            this.folderName = name;
        }

        public void setCallback(JTextArea log){
            textArea = log;
            DefaultCaret myCaret = (DefaultCaret)textArea.getCaret();
            myCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        }

        public void startDownload(){
            //Progress updater
            downloadManager.resetCounter();
            Thread updater = new Thread(new ProgressUpdater(Comm.PROGRESS_TYPE_DOWNLOAD, selectedPhoto.size()));
            try {
                //ArrayList<PhotoWrapper> list = getSelectedPhotoListFromTable(tb_PhotoList);
                logger.log(Level.INFO, "Download thread is started");
                //10 thread to download
                ExecutorService pool = Executors.newFixedThreadPool(10);
                updater.start();
                textArea.setText("");
                textArea.append("正在下載相片集：" + folderName+"\n");
                if(selectedPhoto == null || selectedPhoto.size() ==0){return;}
                File savedFile = null;
                String defaultFileName = "";
                //File albumPath = null;
                String toBeSavedUrl = "";

//                if(fConfig.exists()){
//                    properties.load(new InputStreamReader(new FileInputStream(fConfig),"UTF-8"));
//                    customizedPath = properties.getProperty("DownladPath");
//                    File path = new File(customizedPath);
//                    if(path.exists() && path.canWrite() && path.isDirectory()){
//                        albumPath = new File(customizedPath + System.getProperty("file.separator") + myUser.getUser().getPathAlias() + System.getProperty("file.separator")+ this.folderName);
//                    }else{
//                        albumPath = new File(Storage_base + System.getProperty("file.separator") + myUser.getUser().getPathAlias() + System.getProperty("file.separator")+ this.folderName);
//                    }
//                }else{
//                    albumPath = new File(Storage_base + System.getProperty("file.separator") + myUser.getUser().getPathAlias() + System.getProperty("file.separator")+ this.folderName);
//                }
                albumPath = new File(currentDownloadPath + System.getProperty("file.separator") + myUser.getUser().getPathAlias() + System.getProperty("file.separator")+ this.folderName);
                //albumPath = new File(myUser.getUser().getPathAlias() + System.getProperty("file.separator")+ this.folderName);
                if(downloadManager.createPath(albumPath)!=Comm.RetrunCode.SUCCESS){
                    logger.log(Level.INFO, "路徑建失敗:"+ albumPath);
                }
                //JOptionPane.showMessageDialog(null, "開始下載照片");                
                int i=0;
                int retryCount = 5;
                boolean isInRetryLoop = false;
                //boolean skipOrignal = resourceMap.getBoolean("Application.SikpOrignalSize");
                boolean skipOrignal = subSettingFileSizeLarge.isSelected();
                for(i=0;i<selectedPhoto.size();i++){
                    if(!isInRetryLoop){retryCount=5;}
                    else{
                        logger.log(Level.DEBUG, "Retry to download photo "+selectedPhoto.get(i).getPhoto().getId()+" again");
                    }
                    if(retryCount<=0){continue;}
                    PhotoWrapper pw = selectedPhoto.get(i);
                    Photo p = pw.getPhoto();
                    try {
                        //logger.log(Level.DEBUG, ""+ p.getTitle());
                        //toBeSavedUrl = myController.getLargestSizePhotoUrl(p.getId());
                        //toBeSavedUrl = myController.getLargeSizePhotoUrl(p.getId());                        
                        toBeSavedUrl = myController.getLargestSizePhotoUrl(p.getId(), skipOrignal);
                        if(toBeSavedUrl == null) {
                            logger.log(Level.ERROR, "無法取得照片網址! PID = " + pw.getPhoto().getId());
                            continue;
                        }
                        logger.log(Level.INFO, "將[" + p.getId() +"] 加入下載佇列, URL = [" + toBeSavedUrl + "]...");
                        textArea.append("將[" + p.getTitle() +"], 加入下載佇列...\n");                        
                        //savedFile = new File(p.getId() + ".jpg");
                        if(p.getTitle().equalsIgnoreCase("")){
                            defaultFileName = "Empty_" + p.getId();
                        }else{
                            defaultFileName = p.getTitle();
                        }
                        defaultFileName += ".jpg";
                        //remove invalid chars
                        //in windows, \ / : * ? " < > | are invalid chars
                        defaultFileName = defaultFileName.replaceAll("[\\/:*?\"<>|]", "");
                        savedFile = new File(defaultFileName);
                        pool.submit(new DownloadTask(toBeSavedUrl, albumPath, savedFile));
                        isInRetryLoop = false;
    //                        if (downloadManager.downloadFile(toBeSavedUrl, albumPath, savedFile) == Comm.RetrunCode.SUCCESS) {
    //                            logger.log(Level.INFO, "...完成!");
    //                        }else{
    //                            logger.log(Level.INFO, "下載失敗");
    //                        }

                    } catch (FlickrException ex) {
                        i--;
                        retryCount--;
                        logger.log(Level.ERROR, "取得照片網址失敗, retry, remaining = "+ retryCount+", Error Message = "+ex.getMessage());
                        isInRetryLoop = true;
                    }
                    catch (Exception ex) {
                        retryCount--;
                        i--;
                        logger.log(Level.ERROR, "未知的錯誤，照片ID="+p.getId()+", retry, remaining = "+retryCount+", Error Message = "+ex.getMessage());
                        isInRetryLoop = true;
                    }
                }
                 pool.shutdown();
                 pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                //JOptionPane.showMessageDialog(null, "下載完成，位置：\n"+albumPath.getAbsolutePath());
                logger.log(Level.INFO, "相簿下載完成，位置："+albumPath.getAbsolutePath());

                //認證相關exception
            }catch (InterruptedException ex) {
                logger.log(Level.ERROR, null, ex);
            } 
        }
    }

}


