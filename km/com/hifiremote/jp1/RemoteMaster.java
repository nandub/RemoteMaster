package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkListener;

import org.harctoolbox.irp.Version;

import com.hifiremote.LibraryLoader;
import com.hifiremote.jp1.FixedData.Location;
import com.hifiremote.jp1.JP2Analyzer;
import com.hifiremote.jp1.extinstall.BTExtInstall;
import com.hifiremote.jp1.extinstall.ExtInstall;
import com.hifiremote.jp1.extinstall.RMWavConverter;
import com.hifiremote.jp1.extinstall.IrHex;
import com.hifiremote.jp1.extinstall.RMExtInstall;
import com.hifiremote.jp1.extinstall.IrHexArray;
import com.hifiremote.jp1.extinstall.RMWavPlayer;
import com.hifiremote.jp1.io.BLERemote;
import com.hifiremote.jp1.io.CommHID;
import com.hifiremote.jp1.io.JP2BT;
import com.hifiremote.jp1.io.JPS;
import com.hifiremote.jp1.io.IO;
import com.hifiremote.jp1.io.JP12Serial;
import com.hifiremote.jp1.io.JP1Parallel;
import com.hifiremote.jp1.io.JP1USB;

/**
 * Description of the Class.
 * 
 * @author Greg
 * @created November 30, 2006
 */

public class RemoteMaster extends JP1Frame implements ActionListener, PropertyChangeListener, HyperlinkListener,
    ChangeListener
{
  public static final int MAX_RDF_SYNC = 5;
  public static final int MIN_RDF_SYNC = 3;

  public static final Color AQUAMARINE = new Color( 127, 255, 212 );

  public static boolean admin = false;
  public static boolean legacyMergeOK = false;
  
  /** The frame. */
  private static JP1Frame frame = null;

  /** Description of the Field. */
  public final static String version = "v2.09";
  public final static int buildVer = 5;
  
  public enum WavOp { NEW, MERGE, SAVE, PLAY };
  
  public static class SummarySection
  {
    public JP1TableModel< ? > model = null;
    public String title = null;
    public Activity activity = null;
    public String subtitle = null;
    public TableSorter sorter = null;
    public JP1Table table = null;
  }
  
  public static class LanguageDescriptor
  {
    public String name = null;
    public Integer code = null;
    
    public LanguageDescriptor( String name, Integer code )
    {
      this.name = name;
      this.code = code;
    }
    
    @Override
    public String toString()
    {
      return name;
    }
  }
  
  public static LanguageDescriptor defaultLanguage = new LanguageDescriptor( "Current", null );
  
  public static LinkedHashMap< Integer, LanguageDescriptor > languages = null;
  
  public static int getBuild()
  {
    int buildNumber = buildVer;
    File buildFile = new File( workDir, "buildRef.txt" );
    try
    {
      BufferedReader rdr = new BufferedReader( new FileReader ( buildFile ) );
      String str = rdr.readLine();
      rdr.close();
      buildNumber = Integer.parseInt( str );
      if ( buildNumber < buildVer )
      {
        buildNumber = buildVer;
      }
    }
    catch ( Exception ex ){};
    return buildNumber;
  }
  
  public static String getFullVersion()
  {
    return version.replaceAll("\\s","") + "build" + getBuild();
  }
    
  public static class BatteryBar extends JLabel
  {
    private int bars = 0;
    
    public BatteryBar()
    {
      setOpaque( true );
      setBackground( Color.WHITE );
      setBorder( BorderFactory.createLineBorder( Color.LIGHT_GRAY ) );
    }

    public void setBars( int bars )
    {
      this.bars = bars;
      repaint();
    }

    @Override
    public void paintComponent( Graphics g )
    {
      {
        Dimension d = getSize();
        int barWidth = (d.width + 2) / 3 - 2;
        for ( int i = 0; i < bars; i++ )
        {
          g.setColor( Color.GRAY );
          g.fillRect( ( barWidth + 2 ) * i, 0, barWidth, d.height );
        }
      }
    }
  }

  public enum Use
  {
    DOWNLOAD, READING, UPLOAD, SAVING, SAVEAS, EXPORT, RAWDOWNLOAD, CONNECT, SEARCH
  }
  
  public static int defaultToolTipTimeout = 5000;
  
  /** The dir. */
  private File dir = null;

  private File mergeDir = null;

  /** Description of the Field. */
  public File file = null;

  /** The remote config. */
  private RemoteConfiguration remoteConfig = null;

  private JToolBar toolBar = null;

  private RMAction newAction = null;
  private RMAction newUpgradeAction = null;
  private RMAction newProtocolAction = null;

  private RMAction codesAction = null;

  /** The open item. */
  private RMAction openAction = null;

  /** The save item. */
  private RMAction saveAction = null;

  /** The save as item. */
  private RMAction saveAsAction = null;

  private JMenuItem installExtenderItem = null;
  private JMenuItem importFromWavNewItem = null;
  private JMenuItem importFromWavMergeItem = null;
  
  private JMenu exportToWavSubMenu = null;
  private JMenuItem exportToWavImageItem = null;
  private JMenuItem exportToWavSettingsItem = null;
  private JMenuItem exportToWavMacrosEtcItem = null;
  private JMenuItem exportToWavTimedMacrosItem = null;
  private JMenuItem exportToWavUpgradesItem = null;
  private JMenuItem exportToWavLearnedItem = null;
  
  private JMenuItem createSummaryItem = null;
  private JMenuItem viewSummaryItem = null;
  private JMenuItem saveSummaryItem = null;

  private RMAction openRdfAction = null;

  protected RMAction highlightAction = null;

  private JMenuItem rdfPathItem = null;
  private JMenuItem mapPathItem = null;
  private JMenuItem addonPathItem = null;
  private JMenuItem setBaselineItem = null;
  private JMenuItem clearBaselineItem = null;

  /** The recent files. */
  private JMenu recentFiles = null;

  /** The exit item. */
  private JMenuItem exitItem = null;

  // Remote menu items
  /** The interfaces. */
  private ArrayList< IO > interfaces = new ArrayList< IO >();

  private RMAction bluetoothAction = null;
  private JToggleButton bluetoothButton = null;
  private JCheckBoxMenuItem bluetoothItem = null;
  private Box box = null;
  private ButtonGroup btGroup = null;
  private LinkedHashMap< String, BLERemote > bleMap = new LinkedHashMap< String, BLERemote >();
  private LinkedHashMap< JRadioButton, BLERemote > bleBtnMap = new LinkedHashMap< JRadioButton, BLERemote >();
  private JButton searchButton = null;
  private JButton registerButton = null;
  private JButton deregisterButton = null;
  private BatteryBar batteryBar = null;
  private JLabel batteryVoltage = null;
  private JProgressBar signalProgressBar = null;
  
  private RMAction finderAction = null;
  private JToggleButton finderButton = null;
  private JCheckBoxMenuItem finderItem = null;
  private boolean uploadable = false;
  
  private JP2BT btio = null;
  
  /** The download action. */
  private RMAction downloadAction = null;

  /** The upload action. */
  private RMAction uploadAction = null;
  
  private JMenuItem uploadWavItem = null;
  private JMenuItem cancelWavUploadItem = null;

  /** The raw download item */
  private JMenuItem downloadRawItem = null;

  /** The verify upload item */
  private JCheckBoxMenuItem verifyUploadItem = null;

  // Options menu items
  /** The look and feel items. */
  private JRadioButtonMenuItem[] lookAndFeelItems = null;
  
  private JRadioButtonMenuItem irpTransmogrifierItem = null;
  private JRadioButtonMenuItem decodeIRItem = null;

  protected JCheckBoxMenuItem highlightItem = null;

  private JCheckBoxMenuItem enablePreserveSelection = null;
  
  private JCheckBoxMenuItem showSlingboxProtocols = null;
  
  private JRadioButtonMenuItem defaultDelayItem = null;
  
  private JRadioButtonMenuItem specifiedDelayItem = null;
  
  public static JCheckBoxMenuItem noUpgradeItem = null;

  // Advanced menu items
  private JMenuItem cleanUpperMemoryItem = null;

  private JMenuItem clearAltPIDHistory = null;

  private JMenuItem initializeTo00Item = null;

  private JMenuItem initializeToFFItem = null;
  
  private JCheckBoxMenuItem useSavedDataItem = null;
  
  private static JCheckBoxMenuItem getSystemFilesItem = null;
  
  private static JMenuItem putSystemFileItem = null;
  
  private static JMenuItem saveFDRAfirmware = null;
  
  private static JMenuItem parseIRDBItem = null;
  
  private static JMenuItem xziteOpsItem = null;
  
  private static JMenuItem xziteReformatItem = null;
  
  private static JMenuItem verifyXZITEfilesItem = null;
  
  private static JMenuItem upgradeSourceItem = null;
  
  private static JMenu xziteOps = null;
  
  private static JMenu digitalOps = null;
  
  private static JMenuItem extractSSItem = null;
  
  public static JCheckBoxMenuItem forceUpgradeItem = null;
  
  public static JCheckBoxMenuItem forceFDRAUpgradeItem = null;
  
  public static JCheckBoxMenuItem suppressTimingSummaryInfo = null;
  
  public static JCheckBoxMenuItem suppressConfirmPrompts = null;
  
  private static JMenuItem analyzeMAXQprotocols = null;

  // Help menu items
  /** The update item. */
  private JMenuItem updateItem = null;

  private JMenuItem readmeItem = null;
  
  private JMenuItem rmpbReadmeItem = null;

  private JMenuItem tutorialItem = null;

  private JMenuItem homePageItem = null;

  private JMenuItem learnedSignalItem = null;

  private JMenuItem wikiItem = null;

  private JMenuItem forumItem = null;
  
  private JMenuItem powerManagementItem = null;
  
  private int tooltipDelay = 0;
  private int tooltipDefaultDelay = 0;

  /** The about item. */
  private JMenuItem aboutItem = null;

  /** The tabbed pane. */
  private JTabbedPane tabbedPane = null;

  private RMPanel currentPanel = null;

  /** The general panel. */
  private GeneralPanel generalPanel = null;

  /** The key move panel. */
  private KeyMovePanel keyMovePanel = null;

  /** The macro panel. */
  private MacroPanel macroPanel = null;

  /** The special function panel. */
  private SpecialFunctionPanel specialFunctionPanel = null;

  private TimedMacroPanel timedMacroPanel = null;

  private FavScanPanel favScanPanel = null;
  
  private FavoritesPanel favoritesPanel = null;

  /** The device panel. */
  private DeviceUpgradePanel devicePanel = null;

  /** The protocol panel. */
  private ProtocolUpgradePanel protocolPanel = null;
  
  private ActivityPanel activityPanel = null;

  /** The learned panel. */
  private LearnedSignalPanel learnedPanel = null;

  private RawDataPanel rawDataPanel = null;
  private SegmentPanel segmentPanel = null;

  /** The adv progress bar. */
  private JProgressBar advProgressBar = null;
  private JLabel advProgressLabel = null;

  /** The upgrade progress bar. */
  private JProgressBar upgradeProgressBar = null;
  private JProgressBar devUpgradeProgressBar = null;

  private JPanel upgradeProgressPanel = null;

  /** The learned progress bar. */
  private JProgressBar learnedProgressBar = null;

  private JPanel memoryStatus = null;
  private JPanel extraStatus = null;
  private JPanel bleStatus = null;

  private JPanel interfaceStatus = null;
  private JPanel warningStatus = null;
  private String interfaceText = null;
  private DeviceUpgradeEditor duEditor = null;

  private JProgressBar interfaceState = null;
  
  private boolean exitPrompt = false;

  private JPanel statusBar = null;

  private boolean hasInvalidCodes = false;

  private CodeSelectorDialog codeSelectorDialog = null;

  private JDialog colorDialog = null;
  
  private ActionEvent lfEvent = null;

  public JDialog getColorDialog()
  {
    return colorDialog;
  }

  private JColorChooser colorChooser = null;

  public JColorChooser getColorChooser()
  {
    return colorChooser;
  }

  private TextFileViewer rdfViewer = null;
  private RMWavPlayer wavPlayer = null;

  private List< AssemblerItem > clipBoardItems = new ArrayList< AssemblerItem >();
  
  public static class NegativeDefaultButtonJOptionPane 
  {
    // From http://stackoverflow.com/questions/1395707/how-to-make-joptionpane-showconfirmdialog-have-no-selected-by-default
    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType,
        int messageType ) 
    {
      List<Object> options = new ArrayList<Object>();
      Object defaultOption;
      switch(optionType){
        case JOptionPane.OK_CANCEL_OPTION:
          options.add(UIManager.getString("OptionPane.okButtonText"));
          options.add(UIManager.getString("OptionPane.cancelButtonText"));
          defaultOption = UIManager.getString("OptionPane.cancelButtonText");
          break;
        case JOptionPane.YES_NO_OPTION:
          options.add(UIManager.getString("OptionPane.yesButtonText"));
          options.add(UIManager.getString("OptionPane.noButtonText"));
          defaultOption = UIManager.getString("OptionPane.noButtonText");
          break;
        case JOptionPane.YES_NO_CANCEL_OPTION:
          options.add(UIManager.getString("OptionPane.yesButtonText"));
          options.add(UIManager.getString("OptionPane.noButtonText"));
          options.add(UIManager.getString("OptionPane.cancelButtonText"));
          defaultOption = UIManager.getString("OptionPane.cancelButtonText");
          break;
        default:
          throw new IllegalArgumentException("Unknown optionType "+optionType);
      }
      return JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, null, options.toArray(), defaultOption);
    }
  }
  
  public class Preview extends JPanel
  {
    Preview()
    {
      super();
      sample.setPreferredSize( new Dimension( 90, 30 ) );
      sample.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
      JPanel p = new JPanel();
      p.add( sample );
      add( p );
      add( Box.createHorizontalStrut( 20 ) );
      ButtonGroup grp = new ButtonGroup();
      grp.add( devices );
      grp.add( protocols );
      devices.setSelected( true );
      devices.addActionListener( new ActionListener()
      {
        @Override
        public void actionPerformed( ActionEvent e )
        {
          if ( colorCol != 0 )
          {
            Color color = getInitialHighlight( devicePanel.table, 0 );
            colorChooser.setColor( color );
            sample.setBackground( color );
            colorCol = 0;
          }
        }
      } );
      protocols.addActionListener( new ActionListener()
      {
        @Override
        public void actionPerformed( ActionEvent e )
        {
          if ( colorCol != 1 )
          {
            Color color = getInitialHighlight( devicePanel.table, 1 );
            colorChooser.setColor( color );
            sample.setBackground( color );
            colorCol = 1;
          }
        }
      } );
      selectors.add( devices );
      selectors.add( protocols );
      add( selectors );
    }

    public void reset( boolean disableProtocol )
    {
      colorCol = 0;
      devices.setSelected( true );
      protocols.setEnabled( !disableProtocol );
    }

    public Color getColor()
    {
      return result;
    }

    public JPanel getSelectors()
    {
      return selectors;
    }

    private JPanel sample = new JPanel();
    private Color result = null;
    private JPanel selectors = new JPanel( new GridLayout( 2, 1 ) );
    private int colorCol = 0;
    private JRadioButton devices = new JRadioButton( "Device" );
    private JRadioButton protocols = new JRadioButton( "Protocol" );
  }

  private class DownloadTask extends SwingWorker< RemoteConfiguration, Void > implements ProgressUpdater
  {
    private File file = null;
    private Use use = null;
    private IO io = null;
    
    public DownloadTask()
    {
      file = null;
      use = Use.DOWNLOAD;
    }
    
    public DownloadTask( File file )
    {
      this.file = file;
      use = Use.READING;
    }
    
    @Override
    public void updateProgress( int value )
    {
      if ( value < 0 )
      {
        setInterfaceState( "DOWNLOADING..." );
      }
      else
      {
        String name = io != null ? io.getProgressName() : null;
        setInterfaceState( name != null ? name : "PREPARING:", value );
      }
    }

    @Override
    protected RemoteConfiguration doInBackground() throws Exception
    {
      clearAllInterfaces();
      IO io = getOpenInterface( file, use, this );
      if ( io == null )
      {
        setInterfaceState( null );
        JOptionPane.showMessageDialog( RemoteMaster.this, "No remotes found!" );
        return null;
      }
      System.err.println( "Interface opened successfully" );

      // See comment in Hex.getRemoteSignature( short[] ) for why the line below was not safe
      // String sig = io.getRemoteSignature();
      this.io = io;
      int baseAddress = io.getRemoteEepromAddress();
      System.err.println( "Base address = $" + Integer.toHexString( baseAddress ).toUpperCase() );
      String sigString = getIOsignature( io, baseAddress );
      String sig = null;
      String sig2 = null;
      short[] sigData = null;
      Remote remote = null;
      List< Remote > remotes = null;
      RemoteManager rm = RemoteManager.getRemoteManager();
      byte[] jp2info = new byte[ 0 ];
      if ( sigString.length() > 8 ) // JP1.4/JP2 full signature block
      {
        sig = sigString.substring( 0, 6 );
//      int infoLen = 6 + 6 * remote.getProcessor().getAddressLength();
        int infoLen = 64;
        jp2info = new byte[ infoLen ];
        if ( !io.getJP2info( jp2info, infoLen ) )
        {
          jp2info = null;
        }

        sigData = new short[ sigString.length() + ( jp2info != null ? jp2info.length : 0 ) ];
        int index = 0;
        for ( int i = 0; i < sigString.length(); i++ )
        {
          sigData[ index++ ] = ( short )sigString.charAt( i );
        };
        if ( jp2info != null )
        {
          for ( int i = 0; i < jp2info.length; i++ )
          {
            sigData[ index++ ] = ( short )( jp2info[ i ] & 0xFF );
          }
        }
      }
      else if ( io.getInterfaceName().equals( "JPS" ) )
      {
        sig = sigString;
        int infoLen = 64;
        jp2info = new byte[ infoLen ];
        if ( !io.getJP2info( jp2info, infoLen ) )
        {
          jp2info = null;
        }
        sigData = new short[ 64 ];
        if ( jp2info != null )
        {
          for ( int i = 0; i < jp2info.length; i++ )
          {
            sigData[ i ] = ( short )( jp2info[ i ] & 0xFF );
          }
        }
      }
      else
      {
        sig = sigString;
      }
      
      if ( remoteConfig != null && remoteConfig.getRemote() != null )
      {
        sig2 = remoteConfig.getRemote().getSignature();
        if ( sig2.length() <= sig.length() && sig2.equals( sig.substring( 0, sig2.length() ) ) )
        {
          // Current and download remotes have same signature. Note that if current signature length
          // is less than 8 then we only test the corresponding substring of download signature.
          remotes = rm.findRemoteBySignature( sig2 );
          sig = sig2;
          if ( remotes.size() == 1 )
          {
            // There is only one remote with current signature so this must be the download remote.
            remote = remotes.get( 0 );
          }
          else if ( sameSigSameRemote() )
          {
            // We are required to assume it is the same remote as it has the same signature
            for ( Remote r : remotes )
            {
              if ( remoteConfig.getRemote().compareTo( r ) == 0 )
              {
                remote = r;
                break;
              }
            }
          }
        }
      }
      if ( remote == null )
      {
        System.err.println( "Searching for RDF" );
        if ( remotes == null )
        {
          int minLength = sig.startsWith( "USB" ) ? 7 : 4;
          for ( int i = sig.length(); i >= minLength; i-- )
          {
            sig2 = sig.substring( 0, i );
            remotes = rm.findRemoteBySignature( sig2 );
            if ( !remotes.isEmpty() )
            {
              break;
            }
          }
          sig = sig2;
          System.err.println( "Final signature sought = " + sig );
        }
        if ( remotes.isEmpty() )
        {
          System.err.println( "No matching RDF found" );
          JOptionPane.showMessageDialog( RemoteMaster.this, "No RDF matches signature starting " + sig );
          io.closeRemote();
          setInterfaceState( null );
          return null;
        }
        else if ( remotes.size() == 1 )
        {
          remote = remotes.get( 0 );
        }
        else
        {// ( remotes.length > 1 )
          int maxFixedData = 0;
          for ( Remote r : remotes )
          {
            r.load();
            for ( FixedData fixedData : r.getRawFixedData() )
            {
              if ( fixedData.getLocation() == Location.E2 )
              {
                maxFixedData = Math.max( maxFixedData, fixedData.getAddress() + fixedData.getData().length );
              }
            }
          }

          int eepromSize = io.getRemoteEepromSize();
          if ( eepromSize > 0 && maxFixedData > eepromSize )
          {
            maxFixedData = eepromSize;
          }
          short[] buffer = new short[ maxFixedData ];
          if ( maxFixedData > 0 )
          {
            io.readRemote( baseAddress, buffer );
          }
          Remote[] choices = FixedData.filter( remotes, buffer, sigData );
          if ( choices.length == 0 )
          {
            // None of the remotes match on fixed data, so offer whole list
            choices = remotes.toArray( choices );
          }
          if ( choices.length == 1 )
          {
            remote = choices[ 0 ];
          }
          else
          {
            String message = "Please pick the best match to your remote from the following list:";
            Object rc = JOptionPane.showInputDialog( null, message, "Ambiguous Remote", JOptionPane.ERROR_MESSAGE,
                null, choices, choices[ 0 ] );
            if ( rc == null )
            {
              io.closeRemote();
              setInterfaceState( null );
              return null;
            }
            else
            {
              remote = ( Remote )rc;
            }
          }
        }
        System.err.println( "Remote identified as: " + remote.getName() );
      }
      Remote newRemote = new Remote( remote, remote.getNameIndex() );
      RemoteManager.getRemoteManager().replaceRemote( remote, newRemote );
      remote = newRemote;
      remote.load();
      RemoteConfiguration rc = new RemoteConfiguration( remote, RemoteMaster.this );
      recreateToolbar();
      int count = io.readRemote( remote.getBaseAddress(), rc.getData() );
      System.err.println( "Number of bytes read  = $" + Integer.toHexString( count ).toUpperCase() );
      io.closeRemote();
      System.err.println( "Ending normal download" );
      if ( count != rc.getData().length )
      {
        System.err.println( "Download aborting due to incomplete read" );
        return null;
      }
      if ( sigData != null )
      { 
        if ( !io.getInterfaceName().equals( "JPS" ) )
        {
          sigData = Arrays.copyOf( sigData, 6 + 6 * remote.getProcessor().getAddressLength() );
        }
        rc.setSigData( sigData );
      }
//      try
//      {
        rc.parseData();
        rc.setSavedData();
//      }
//      catch ( IOException e )
//      {
//        e.printStackTrace();
//      }
      rc.updateImage();
      return rc;
    }
    
    @Override
    public void done()
    {
      try
      {
        RemoteConfiguration rc = get();
        if ( rc != null )
        {
          remoteConfig = rc;
        }
        else
        {
          setInterfaceState( null );
          String title = file != null ? "File load" : "Download";
          String message = title + " aborted.";
          JOptionPane.showMessageDialog( RemoteMaster.this, message, "Download", JOptionPane.WARNING_MESSAGE );
          return;
        }
      } 
      catch ( InterruptedException ignore ) {}
      catch ( java.util.concurrent.ExecutionException e ) 
      {
        String why = null;
        Throwable cause = e.getCause();
        if ( cause != null ) 
        {
          why = cause.getMessage();
        } 
        else 
        {
          why = e.getMessage();
        }
        System.err.println( "Download error: " + why );
        setInterfaceState( null );
        String message = "<html>Error downloading from remote.<br><br>"
            + "This may well be the result of a bug in the RMIR software.  To help us, please<br>"
            + "do a raw download as follows.  Close RMIR, re-open it and click on &quot;Raw download&quot;<br>"
            + "on the Remote menu.  Click the Download button on the window that then opens.  <br>"
            + "When the dowload finishes, click Save.  Accept the default filename that is offered.<br>"
            + "Then post the resulting file in the JP1 Software forum for the experts to examine.</html><br>";
        JOptionPane.showMessageDialog( RemoteMaster.this, message, "Task error", JOptionPane.ERROR_MESSAGE );
        e.printStackTrace();
      }
      saveAction.setEnabled( file != null );
      saveAsAction.setEnabled( true );
      openRdfAction.setEnabled( true );
      installExtenderItem.setEnabled( file == null || admin );
      cleanUpperMemoryItem.setEnabled( true );
      initializeTo00Item.setEnabled( true );
      initializeToFFItem.setEnabled( true );
      setBaselineItem.setEnabled( true );
      uploadable = true;
      uploadAction.setEnabled( allowUpload() );
      resetSegmentPanel();
      update();
      changed = remoteConfig != null ? !Hex.equals( remoteConfig.getSavedData(), remoteConfig.getData() ) : false;
      if ( file != null )
      {
        setTitleFile( file );
      }
      setInterfaceState( null );
      return;
    }
  };

  private class XziteFileTask extends SwingWorker< Void, Void > implements ProgressUpdater
  {
    private String op = null;
    private IO io = null;
    
    public XziteFileTask( String op )
    {
      this.op = op;
    }
    
    @Override
    protected Void doInBackground() throws Exception
    {
      IO io = getOpenInterface( null, Use.UPLOAD, this );
      if ( io == null )  
      {
        JOptionPane.showMessageDialog( RemoteMaster.this, "No remotes found!" );
        setInterfaceState( null );
        return null;
      }
      System.err.println( "Interface opened successfully" );
      this.io = io;
      
      if ( !( io instanceof CommHID ) )
      {
        JOptionPane.showMessageDialog( RemoteMaster.this, "Not an XSight remote!" );
        setInterfaceState( null );
        return null;
      }
      CommHID ioHID = ( CommHID )io;

      if ( !op.equals( "Delete/Save" ) && !op.equals( "FdraFirmware" ) )
      {
        File sysFile = RemoteMaster.getRmirSys();
        String title = "XSight operation";
        String message = null;
        if ( forceUpgradeItem.isSelected() && op.equals( "Reformat" ) )
        {
          message = "You have checked the \"Force XSight Firmware Upgrade\" menu\n"
                  + "item so the reformat and system file restore process will\n"
                  + "be performed without checking that the MCU firmware is the\n"
                  + "latest version.  If it is not the latest version then this\n"
                  + "process may corrupt the remote.\n\n"
                  + "Are you sure you wish to continue?";
          int reply = JOptionPane.showConfirmDialog( RemoteMaster.this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
          if ( reply == JOptionPane.YES_OPTION )
          {
            message = null;
          }
          else
          {
            setInterfaceState( null );
            return null;
          }
        }
        else if ( ioHID.setFileData( sysFile ) )
        {
          Hex upgVersion = ioHID.getUpgradeData().get( "MCUFIRMWARE" ).version;
          Hex remoteVersion = new Hex( 4 );
          ioHID.getXZITEVersion( remoteVersion );
          if ( !upgVersion.equals( remoteVersion ) )
          {
            message = "You do not have the latest firmware.  You need to\n"
                + "upgrade to the latest version before you can use\n"
                + "this option."; 
          }
        }
        else
        {
          message = "Unable to verify the firmware verion of your remote.\n"
              + "This option is only available for the latest firmware.";
        }
        if ( message != null )
        {
          JOptionPane.showMessageDialog( RemoteMaster.this, message, title, JOptionPane.INFORMATION_MESSAGE );
          setInterfaceState( null );
          return null;
        }
      }

      if ( op.equals( "Upload" ) )
      {
        RMListChooser.showDialog( RemoteMaster.this, ioHID, true );
      }
      else if ( op.equals( "Delete/Save" ) )
      {
        RMListChooser.showDialog( RemoteMaster.this, ioHID, false );
      }
      else if ( op.equals( "Reformat" ) )
      {
        ioHID.reformatXZITE();
        ioHID.closeRemote();
      }
      else if ( op.equals( "Verify" ) )
      {
        List< String > comments = ioHID.verifyXZITEfiles();
        ioHID.closeRemote();
        System.err.println( "Verification of system files:" );
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for ( String s : comments )
        {
          if ( n++ > 0 )
          {
            sb.append( '\n' );
          }
          sb.append( s );
          System.err.println( "  " + s );
        }
        String title = "System file verification";
        JOptionPane.showMessageDialog( null, sb.toString(), title, JOptionPane.INFORMATION_MESSAGE );
      }
      else if ( op.equals( "FdraFirmware" ) )
      {
        String name = ioHID.saveFDRAfirmware();
        ioHID.closeRemote();
        String title = "Save FDRA Firmware";
        String message = name != null ? 
            "The firmware of the remote has been saved to a file named " + name
            + "\n in the XSight subfolder of the RMIR installation folder.\n\n"
            + "The \"Set upgrade source file...\" item may be used to select this\n"
            + "file for reinstalling into the remote." :
              "The Save FDRA Firmware operation failed.";
        JOptionPane.showMessageDialog( RemoteMaster.this, message, title, JOptionPane.INFORMATION_MESSAGE );
      }
      setInterfaceState( null );
      return null;
    }
    
    @Override
    public void updateProgress( int value )
    {
      String name = io != null ? io.getProgressName() : null;
      setInterfaceState( name != null ? name : "PREPARING:", value );
    }
  }
  
  private class UploadTask extends WriteTask implements ProgressUpdater
  {
    private short[] data;
    private boolean allowClockSet;
    private File file = null;
    private Use use = null;
    private IO io = null;

    private UploadTask( short[] data, boolean allowClockSet )
    {
      this.data = data;
      this.allowClockSet = allowClockSet;
      this.file = null;
      use = Use.UPLOAD;
    }
    
    private UploadTask( File file, short[] data, boolean allowClockSet, boolean forSaveAs )
    {
      this.data = data;
      this.allowClockSet = allowClockSet;
      this.file = file;
      use = forSaveAs ? Use.SAVEAS : Use.SAVING;
    }

    @Override
    protected Void doInBackground() throws Exception
    {
      resetInterfaceState();
      Remote remote = remoteConfig.getRemote();
      IO io = getOpenInterface( file, use, this );
      if ( io == null )  
      {
        JOptionPane.showMessageDialog( RemoteMaster.this, "No remotes found!" );
        setInterfaceState( null );
        return null;
      }
      System.err.println( "Interface opened successfully" );
      this.io = io;
      int eepromSize = remote.getEepromSize();
      int extraSize = 0;
      short[] extraData = null;
      
      if ( use == Use.UPLOAD )
      {
        int baseAddress = io.getRemoteEepromAddress();
        System.err.println( "Base address = $" + Integer.toHexString( baseAddress ).toUpperCase() );
        String sig = getIOsignature( io, baseAddress );
        if ( sig.length() > 8 ) // JP1.4/JP2 full signature
        {
          sig = sig.substring( 0, 6 );
        }
        String rSig = remote.getSignature();
        String message = null;
        if ( sig.length() < rSig.length() || !rSig.equals( sig.substring( 0, rSig.length() ) ) )
        {
          message = "The signature of the attached remote does not match the signature you are trying to upload.  The image\n"
              + "you are trying to upload may not be compatible with attached remote, and uploading it may damage the\n"
              + "remote.  Copying the contents of one remote to another is only safe when the remotes are identical.\n\n"
              + "This message will be displayed when installing an extender in your remote, which is the only time it is\n"
              + "safe to upload to a remote when the signatures do not match.\n\n"
              + "How would you like to proceed?";
        }
        else if ( !suppressConfirmPrompts.isSelected() )
        {
          message = "An upload overwrites the entire memory area for setup data in the remote and cannot\n"
              + "be undone.  Are you sure that you want to do this?";
        }
        Object[] options =
          {
            "Upload to the remote", "Cancel the upload"
          };
        if ( message != null )
        {
          int rc = JOptionPane.showOptionDialog( RemoteMaster.this, message,
              "Upload Confirmation", JOptionPane.DEFAULT_OPTION,
              JOptionPane.WARNING_MESSAGE, null, options, options[ 1 ] );
          if ( rc == 1 || rc == JOptionPane.CLOSED_OPTION )
          {
            io.closeRemote();
            setInterfaceState( null );
            return null;
          }
        }
        
        int pageSize = remote.getProcessor().getPageSize();
        short[] extraRemote = null;
        if ( remote.isJP2style() && eepromSize % pageSize != 0 )
        {
          extraSize = pageSize - eepromSize % pageSize;
          extraRemote = new short[ extraSize ];
          io.readRemote( baseAddress + eepromSize, extraRemote );
          extraData = Arrays.copyOfRange( data, eepromSize, eepromSize + extraSize );
          System.err.println( "Remote/Data extra bytes: " + ( new Hex( extraRemote ) )
              + " / " + ( new Hex( extraData ) ) );
          // Replace extra data by the values read from the remote
          System.arraycopy( extraRemote, 0, data, eepromSize, extraSize );
        }
      }
      AutoClockSet autoClockSet = remote.getAutoClockSet();
      if ( allowClockSet && autoClockSet != null )
      {
        autoClockSet.saveTimeBytes( data );
        autoClockSet.setTimeBytes( data );
        remoteConfig.updateCheckSums();
      }

      int rc = io.writeRemote( remote.getBaseAddress(), data );

      if ( rc != data.length )
      {
        io.closeRemote();
        System.err.println( "Data writing phase failed, bytes written = " + rc + "instead of " + data.length + "." );
        JOptionPane.showMessageDialog( RemoteMaster.this, "writeRemote returned " + rc );
        setInterfaceState( null );
        if ( extraData != null )
        {
          // Restore extra data
          System.arraycopy( extraData, 0, data, eepromSize, extraSize );
        }
        return null;
      }
      else
      {
        System.err.println( "Data writing phase succeeded, bytes written = " + rc + "." );
      }
      if ( verifyUploadItem.isSelected() )
      {
        System.err.println( "Upload verification phase starting." );
        if ( io.getInterfaceType() == 0x106 )
        {
          io.closeRemote();
          io = getOpenInterface( null, Use.READING, this );
        }
        short[] readBack = new short[ data.length ];
        rc = io.readRemote( remote.getBaseAddress(), readBack );
        io.closeRemote();
        if ( rc != data.length )
        {
          System.err.println( "Upload verify failed: read back " + rc
              + " bytes, but expected " + data.length + "." );
          JOptionPane.showMessageDialog( RemoteMaster.this, "Upload verify failed: read back " + rc
              + " bytes, but expected " + data.length );

        }
        else if ( !Hex.equals( data, readBack ) )
        {
          System.err.println( "Upload verify failed: data read back doesn't match data written." );
          JOptionPane.showMessageDialog( RemoteMaster.this,
              "Upload verify failed: data read back doesn't match data written." );
        }
        else
        {
          System.err.println( "Upload verification succeeded." );
        }
      }
      else
      {
        io.closeRemote();
        JOptionPane.showMessageDialog( RemoteMaster.this, "Upload complete!" );
      }
      
      if ( extraData != null )
      {
        // Restore extra data
        System.arraycopy( extraData, 0, data, eepromSize, extraSize );
      }
      if ( allowClockSet && autoClockSet != null )
      {
        autoClockSet.restoreTimeBytes( data );
        remoteConfig.updateCheckSums();
      }
      
      System.err.println( "Ending upload" );
      setInterfaceState( null );
      if ( use == Use.SAVEAS )
      {
        RemoteMaster.this.file = file;
        setTitleFile( file );
        updateRecentFiles( file );
        saveAction.setEnabled( true );
      }
      return null;
    }
    
    @Override
    public void done()
    {
      super.done();
      if ( !ok )
      {
        String message = file != null ? "Error saving Simpleset file " + file.getName() : "Error uploading to remote";
        JOptionPane.showMessageDialog( RemoteMaster.this, message, "Task error", JOptionPane.ERROR_MESSAGE );
      }
      if ( exitPrompt )
      {
        exitPrompt = false;
        dispatchEvent( new WindowEvent( RemoteMaster.this, WindowEvent.WINDOW_CLOSING ) );
      }
    }

    @Override
    public void updateProgress( int value )
    {
      if ( value < 0 )
      {
        setInterfaceState( "UPLOADING..." );
      }
      else
      {
        String name = io != null ? io.getProgressName() : null;
        setInterfaceState( name != null ? name : "PREPARING:", value );
      }
    }
  }

  public static File getJreExecutable()
  { 
    String jreDirectory = System.getProperty( "java.home" ); 
    File javaExe = null; 
    if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
    { 
      javaExe = new File( jreDirectory, "bin/javaw.exe" ); 
    }
    else 
    { 
      javaExe = new File( jreDirectory, "bin/javaw" ); 
    } 
    if ( !javaExe.exists() )
    { 
      return null; 
    } 
    return javaExe;
  } 
  
  private static void runKM( String filename )
  {
    File javaExe = getJreExecutable();
    if ( javaExe == null )
    {
      System.err.println( "Unable to find java executable" );
      return;
    }
    try
    {
      Runtime r = Runtime.getRuntime();
      String classPath = System.getProperty( "java.class.path" );
      r.exec( new String[] { javaExe.getCanonicalPath(), "-cp", classPath, "com.hifiremote.jp1.RemoteMaster", "-rm", "-home", workDir.getAbsolutePath(), filename } );
    }
    catch ( IOException e )
    {
      e.printStackTrace();
    }
  }
  
  private static void runPB( String filename )
  {
    File javaExe = getJreExecutable();
    if ( javaExe == null )
    {
      System.err.println( "Unable to find java executable" );
      return;
    }
    try
    {
      Runtime r = Runtime.getRuntime();
      String classPath = System.getProperty( "java.class.path" );
      r.exec( new String[] { javaExe.getCanonicalPath(), "-cp", classPath, "com.hifiremote.jp1.RemoteMaster", "-pb", "-home", workDir.getAbsolutePath(), filename } );
    }
    catch ( IOException e )
    {
      e.printStackTrace();
    }
  }

  protected class RMAction extends AbstractAction
  {
    public RMAction( String text, String action, ImageIcon icon, String description, Integer mnemonic )
    {
      super( text, icon );
      putValue( ACTION_COMMAND_KEY, action );
      putValue( SHORT_DESCRIPTION, description );
      putValue( MNEMONIC_KEY, mnemonic );
      
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed( ActionEvent event )
    {
      finishEditing();
      try
      {
        String command = event.getActionCommand();
        if ( command.equals( "NEW" ) )
        {
          String title = "File > New";
          String message = "The menu option File > New is primarily intended for creating setups for remotes\n"
                         + "that you do not have, such as for sending to another person to test.  If you have the\n"
                         + "remote, it is strongly recommended that you start a new setup by doing a factory reset\n"
                         + "(981 command) and downloading it.  Although the ideal situation would be for\n"
                         + "File > New to create a factory reset state, for many remotes there are hidden settings\n"
                         + "that are not visible in RMIR whose initial values are not correctly set by File > New.\n"
                         + "These may adversely affect the operation of the remote when a setup created this way\n"
                         + "is uploaded.\n\nDo you wish to continue?";
          if ( !suppressConfirmPrompts.isSelected() && JOptionPane.showConfirmDialog( RemoteMaster.this, message, title, JOptionPane.YES_NO_OPTION, 
              JOptionPane.WARNING_MESSAGE ) == JOptionPane.NO_OPTION )
          {
            return;
          }
          if ( !promptToSave() )
          {
            return;
          }
          Remote remote = RMNewDialog.showDialog( RemoteMaster.this );
          if ( remote == null )
          {
            return;
          }
          
          if ( remote.isLoaded() )
          {
            remote.needsLayoutWarning();
          }
          remote.load();
          if ( remote.isSSD() )
          {
            title = "New Remote Image";
            message = "RMIR cannot create a new remote image for this remote.";
            JOptionPane.showMessageDialog( RemoteMaster.this, message, title, JOptionPane.INFORMATION_MESSAGE );
            return;
          }
          resetConfig( remote, null );
        }
        else if ( command.equals( "NEWDEVICE" ) )
        {
          System.err.println( "RMIR opening new RM instance" );
          
          // Opening KM as a new instance of KeyMapMaster makes it share the same
          // ProtocolManager as RM, which results in crosstalk when one edits the protocol
          // used by the other.  Replaced now by opening KM as a new application.
          
//          new KeyMapMaster( properties );
          runKM( "" );
        }
        else if ( command.equals( "NEWPROTOCOL" ) )
        {
          System.err.println( "RMIR opening new RMPB instance" );
          runPB( "" );
        }
        else if ( command.equals( "OPEN" ) )
        {
          openFile();
        }
        else if ( command.equals( "SAVE" ) )
        {
          save( file, false );
        }
        else if ( command.equals( "SAVEAS" ) )
        {
          if ( !allowSave( Remote.SetupValidation.WARN ) )
          {
            return;
          }
          saveAs();
        }
        else if ( command.equals( "DOWNLOAD" ) )
        {
          if ( !promptToSave() )
          {
            return;
          }
          System.err.println( "Starting normal download" );
          setInterfaceState( "DOWNLOADING..." );
          ( new DownloadTask() ).execute();
        }
        else if ( command.equals( "UPLOAD" ) )
        {
          boolean validConfiguration = updateUsage();
          if ( !validConfiguration )
          {
            String title = "Invalid Configuration";
            String message = "This configuration is not valid.  It cannot be uploaded as it\n"
                + "could cause the remote to crash.";
            JOptionPane.showMessageDialog( RemoteMaster.this, message, title, JOptionPane.WARNING_MESSAGE );
            return;
          }

          Remote remote = remoteConfig.getRemote();
          if ( !allowSave( remote.getSetupValidation() ) )
          {
            return;
          }
          remoteConfig.saveAltPIDs();
          System.err.println( "Starting upload" );
          setInterfaceState( "UPLOADING..." );
          ( new UploadTask( RemoteMaster.this.useSavedData() ? remoteConfig.getSavedData() : remoteConfig.getData(), true ) ).execute();
        }
        else if ( command == "OPENRDF" )
        {
          String title = "View/Edit RDF";
          rdfViewer = TextFileViewer.showFile( RemoteMaster.this, remoteConfig.getRemote(), title, false );
        }
        else if ( command == "OPENCODES" )
        {
          JP1Table deviceButtonTable = generalPanel.getDeviceButtonTable();
          if ( deviceButtonTable.getCellEditor() != null )
          {
            deviceButtonTable.getCellEditor().stopCellEditing();
          }
          codeSelectorDialog = CodeSelectorDialog.showDialog( RemoteMaster.this );
          codeSelectorDialog.enableAssign( currentPanel == generalPanel );
        }
        else if ( command == "HIGHLIGHT" )
        {
          JP1Table table = null;
          JP1TableModel< ? > model = null;
          TableSorter sorter = null;
          if ( currentPanel instanceof RMTablePanel< ? > )
          {
            RMTablePanel< ? > panel = ( RMTablePanel< ? > )currentPanel;
            table = panel.table;
            model = panel.model;
            sorter = panel.sorter;
          }
          else if ( currentPanel == generalPanel )
          {
            table = generalPanel.getActiveTable();
            model = ( JP1TableModel< ? > )table.getModel();
          }
          else if ( currentPanel == activityPanel )
          {
            table = activityPanel.getActiveTable();
            model = ( JP1TableModel< ? > )table.getModel();
          }
          else if ( currentPanel == favoritesPanel )
          {
            table = favoritesPanel.getActiveTable();
            model = ( JP1TableModel< ? > )table.getModel();
          }
          Color color = getInitialHighlight( table, 0 );
          Preview preview = ( Preview )colorChooser.getPreviewPanel();
          preview.reset( ( currentPanel == devicePanel ) && ( getInitialHighlight( table, 1 ) == null ) );
          preview.selectors.setVisible( currentPanel == devicePanel );
          colorChooser.setColor( color );
          colorDialog.pack();
          colorDialog.setVisible( true );
          color = preview.result;
          if ( table != null && color != null )
          {
            for ( int i : table.getSelectedRows() )
            {
              if ( currentPanel == keyMovePanel )
              {
                // Special case needed to handle attached keymoves
                model.setValueAt( color, sorter.modelIndex( i ), 9 );
              }
              else if ( currentPanel == devicePanel && preview.colorCol == 1 )
              {
                DeviceUpgrade du = devicePanel.getRowObject( i );
                if ( du.needsProtocolCode() )
                {
                  // Special case needed to handle consequential highlights
                  model.setValueAt( color, sorter.modelIndex( i ), model.getColumnCount() - 1 );
                }
              }
              else
              {
                Highlight rowObject = getTableRow( table, i );
                rowObject.setHighlight( color );
              }
            }
            model.fireTableDataChanged();
            model.propertyChangeSupport.firePropertyChange( "data", null, null );
            highlightAction.setEnabled( false );
          }
        }
        else if ( command == "BLUETOOTH" )
        {
          if ( bluetoothButton.isSelected() )
          {
            BLERemote selectedRemote = null;
            bleBtnMap.clear();
            getRegisteredRemotes();
            JPanel panel = new JPanel( new BorderLayout() );
            JLabel lbl = new JLabel( "Text" );
            int height = lbl.getPreferredSize().height;
            String info =
                "Select a remote and press Connect to connect.  To find other remotes,\n"
              + "press Search.  To find a remote new to RMIR, then press and hold its\n"
              + "Devices and Activity buttons until the LED starts to flash.\n\n"
              + "To register a remote with RMIR with a user-friendly name, or to change\n"
              + "the name of a registered remote, select it and press Register.  To\n"
              + "deregister a registered remote, select it and press Deregister.  To\n"
              + "check if a registered remote is available, press Search.  Registered\n"
              + "remotes will then be disabled and only re-enabled if found.";
            JTextArea infoArea = new JTextArea( info );
            infoArea.setFont( lbl.getFont() );
            infoArea.setBackground( lbl.getBackground() );
            infoArea.setEditable( false );
            infoArea.setBorder( BorderFactory.createEmptyBorder( 0, 2, 10, 2 ) );
            panel.add( infoArea, BorderLayout.PAGE_START );
            
            panel.add( Box.createVerticalStrut( 10*height ), BorderLayout.LINE_START );
            
            box = Box.createVerticalBox();
            btGroup = new ButtonGroup();
            String lastRemote = properties.getProperty( "LastBLERemote" );
            for ( String addr : bleMap.keySet() )
            {
              JRadioButton btn = new JRadioButton( bleMap.get( addr ).name );
              btGroup.add( btn );
              if ( lastRemote != null && addr.equals( lastRemote ) )
                btn.setSelected( true );
              bleBtnMap.put( btn, bleMap.get( addr ) );
              box.add( btn );
            }
            JScrollPane scroll = new JScrollPane( box );
            scroll.getViewport().setPreferredSize( new Dimension( 0,0 ));
            scroll.setBorder( BorderFactory.createTitledBorder( "Select remote: "  ) );
            panel.add( scroll, BorderLayout.CENTER );
            
            searchButton = new JButton( "Search" );
            registerButton = new JButton( "Register" );
            deregisterButton = new JButton( "Deregister" );
            JPanel btnPanel = new JPanel( new BorderLayout() );
            JPanel regPanel = new JPanel( new FlowLayout() );
            regPanel.add( searchButton );           
            btnPanel.add( regPanel, BorderLayout.LINE_START );
            regPanel = new JPanel( new FlowLayout() );
            regPanel.add( registerButton );
            regPanel.add( deregisterButton );
            btnPanel.add( regPanel, BorderLayout.LINE_END );
            btnPanel.setBorder( BorderFactory.createTitledBorder( "Actions: " ) );
            panel.add( btnPanel, BorderLayout.PAGE_END );
 
            searchButton.addActionListener( new ActionListener() 
            {
              public void actionPerformed( ActionEvent e )
              { 
                if ( btio == null || !btio.isScanning() )
                {
                  for ( JRadioButton btn : bleBtnMap.keySet() )
                  {
                    btn.setEnabled( false );
                  }
                  ( new ConnectTask( null, Use.SEARCH ) ).execute();
                }
              }
            } );

            ActionListener regListener = new ActionListener() 
            {
              public void actionPerformed( ActionEvent e )
              {
                String name = null;
                BLERemote r = null;
                JRadioButton rBtn = null;
                int regCount = 0;
                for ( JRadioButton btn : bleBtnMap.keySet() )
                {
                  if ( btn.isSelected() )
                  {
                    rBtn = btn;
                    r = bleBtnMap.get( btn );
                    name = r.name;          
                  }
                  if ( bleBtnMap.get( btn ).regIndex >= 0 )
                    regCount++;
                }
                if ( name == null )
                  return;
                Object source = e.getSource();
                if ( source == registerButton )
                {
                  String message = "Enter user-friendly name for this remote:";
                  String result = JOptionPane.showInputDialog( null, message, name );
                  if ( result != null )
                  {
                    r.name = result;
                    if ( r.regIndex < 0 )
                      r.regIndex = regCount;
                    String propName = "RegisteredBTRemotes." + r.regIndex;
                    String propValue = "Name=" + r.name + "UEIName=" + r.ueiName
                        + "Address=" + r.address;
                    properties.setProperty( propName, propValue );
                    rBtn.setText( r.name );
                  }
                }
                else if ( source == deregisterButton )
                {
                  int ndx = r.regIndex;
                  r.regIndex = -1;
                  r.name = r.ueiName + " " + r.address.substring( 9 );
                  rBtn.setText( r.name );
                  for ( int n = ndx; n < regCount - 1; n++ )
                  {
                    String propName = "RegisteredBTRemotes." + ( n + 1 );
                    String propValue = properties.getProperty( propName );
                    propName = "RegisteredBTRemotes." + n;
                    properties.setProperty( propName, propValue );
                  }
                  String propName = "RegisteredBTRemotes." + ( regCount - 1 );
                  properties.remove( propName );
                }
              }
            };

            registerButton.addActionListener( regListener );
            deregisterButton.addActionListener( regListener );
            String[] options = { "Connect", "Close" };
            int result = JOptionPane.showOptionDialog( null, panel, "Remote chooser", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[ 0 ] );
            if ( btio != null && btio.isScanning() )
            {
              btio.discoverUEI( false );
              setInterfaceState( null );
              searchButton.setEnabled( true );
            }
            if ( btio != null )
              btio.setDisconnecting( false );
            if ( result == 0 )
            {
              for ( JRadioButton btn : bleBtnMap.keySet() )
              {
                if ( btn.isSelected() )
                {
                  selectedRemote = bleBtnMap.get( btn );;
                  break;           
                }
              }
              if ( selectedRemote != null )
              {
                properties.setProperty( "LastBLERemote", selectedRemote.address );
                ( new ConnectTask( selectedRemote, Use.CONNECT ) ).execute();
              }
              else
                disconnectBLE();
            }    
            else
            {
              disconnectBLE();
            }
            
          }
          else
          {
            // Disconnect
            disconnectBLE();
          }
        }
        else if ( command == "FINDER" )
        {
          if ( finderButton.isSelected() )
          {
            btio.finderOn( true );
            finderButton.setBorder( BorderFactory.createLoweredBevelBorder() );
          }
          else
          {
            btio.finderOn( false );
            finderButton.setBorder( BorderFactory.createRaisedBevelBorder() );
          }
        }
      }
      catch ( Exception ex )
      {
        ex.printStackTrace( System.err );
      }
    }
  }

  public short[] getInitializationData( int value )
  {
    short[] data = null;
    String title = "Initialize EEPROM Area";
    String message = "This will fill your remote's EEPROM with $" + Hex.asString( value ) + "\n\n"
        + "Doing so will likely cause the remote to stop working until you\n"
        + "perform a hard reset.  Are you sure you want to do this?\n"
        + "(Make sure your current configuration is saved before proceeding.)";
    if ( NegativeDefaultButtonJOptionPane.showConfirmDialog( this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) == JOptionPane.YES_OPTION )
    {
      data = new short[ remoteConfig.getRemote().getEepromSize() ];
      Arrays.fill( data, 0, data.length, ( short )value );
    }
    return data;
  }

  private Highlight getTableRow( JP1Table table, int row )
  {
    Object obj;
    if ( row == -1 )
    {
      return null;
    }
    if ( currentPanel instanceof RMTablePanel< ? > )
    {
      obj = ( ( RMTablePanel< ? > )currentPanel ).getRowObject( row );
    }
    else
    {
      obj = ( ( JP1TableModel< ? > )table.getModel() ).getRow( row );
    }
    if ( obj instanceof Highlight )
    {
      return ( Highlight )obj;
    }
    return null;
  }

  private Color getInitialHighlight( JP1Table table, int colorCol )
  {
    Color color = null;
    if ( table != null )
    {
      int[] rows = table.getSelectedRows();
      if ( rows.length > 0 && getTableRow( table, rows[ 0 ] ) != null )
      {
        if ( currentPanel == devicePanel && colorCol == 1 )
        {
          for ( int i : rows )
          {
            DeviceUpgrade du = devicePanel.getRowObject( i );
            if ( !du.needsProtocolCode() )
            {
              continue;
            }
            if ( color == null )
            {
              color = du.getProtocolHighlight();
            }
            else if ( !du.getProtocolHighlight().equals( color ) )
            {
              return Color.WHITE;
            }
          }
        }
        else
        {
          color = getTableRow( table, rows[ 0 ] ).getHighlight();
          for ( int i : rows )
          {
            if ( !getTableRow( table, i ).getHighlight().equals( color ) )
            {
              return Color.WHITE;
            }
          }
        }
      }
    }
    return color;
  }

  /**
   * Constructor for the RemoteMaster object.
   * 
   * @param workDir
   *          the work dir
   * @param prefs
   *          the prefs
   * @throws Exception
   *           the exception
   * @exception Exception
   *              Description of the Exception
   */
  public RemoteMaster( File workDir, PropertyFile prefs ) throws Exception
  {
    super( "RMIR", prefs );
    dir = properties.getFileProperty( "IRPath", workDir );
    defaultToolTipTimeout = ToolTipManager.sharedInstance().getDismissDelay();

    toolBar = new JToolBar();
    toolBar.setFloatable( false );
    bleStatus = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 0 ) );
    createMenus();
    createToolbar();

    setDefaultCloseOperation( DISPOSE_ON_CLOSE );
    setDefaultLookAndFeelDecorated( true );

    ProtocolManager.getProtocolManager().loadAltPIDRemoteProperties( properties );

    final Preview preview = new Preview();
    // If a non-empty border is not set then the preview panel does not appear. This sets
    // an invisible but non-empty border.
    preview.setBorder( BorderFactory.createLineBorder( preview.getBackground() ) );

    colorChooser = new JColorChooser();
    colorChooser.setPreviewPanel( preview );
    colorChooser.getSelectionModel().addChangeListener( new ChangeListener()
    {
      @Override
      public void stateChanged( ChangeEvent evt )
      {
        ColorSelectionModel model = ( ColorSelectionModel )evt.getSource();
        preview.sample.setBackground( model.getSelectedColor() );
      }
    } );

    colorDialog = JColorChooser.createDialog( this, "Highlight Color", true, colorChooser, new ActionListener()
    { // OK button listener
          @Override
          public void actionPerformed( ActionEvent event )
          {
            preview.result = colorChooser.getColor();
          }
        }, new ActionListener()
        { // Cancel button listener
          @Override
          public void actionPerformed( ActionEvent event )
          {
            preview.result = null;
          }
        } );

    setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
    addWindowListener( new WindowAdapter()
    {
      @Override
      public void windowClosing( WindowEvent event )
      {
        boolean doDispose = true;
        try
        {
          System.err.println( "RemoteMaster.windowClosing() entered" );
          if ( btio != null )
          {
            if ( btio.isScanning() )
            {
              btio.discoverUEI( false );
              setInterfaceState( null );
            }
            // BGAPITransport runs a separate thread, which needs to be stopped.
            btio.setDisconnecting( false );
            disconnectBLE();
          }
          boolean quit = false;
          if ( interfaceText != null )
          {
            String title = "Request to exit";
            String message = "A \"" + interfaceText + "\" task is in progress.  You risk corrupting it if you exit\n"
                           + "before it completes.  Are you sure you wish to continue?";
            quit = NegativeDefaultButtonJOptionPane.showConfirmDialog( RemoteMaster.this, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE ) == JOptionPane.NO_OPTION;
          }
          if ( !promptToSave( true ) || exitPrompt || quit )
          {
            System.err.println( "RemoteMaster.windowClosing() exited" );
            doDispose = false;
            return;
          }
          downloadAction = null;
          uploadAction = null;
          for ( int i = 0; i < recentFiles.getItemCount(); ++i )
          {
            JMenuItem item = recentFiles.getItem( i );
            properties.setProperty( "RecentIRs." + i, item.getActionCommand() );
          }
          properties.remove( "Primacy" );
          ProtocolManager.getProtocolManager().setAltPIDRemoteProperties( properties );
          int state = getExtendedState();
          if ( state != Frame.NORMAL )
          {
            setExtendedState( Frame.NORMAL );
          }
          Rectangle bounds = getBounds();
          properties
              .setProperty( "RMBounds", "" + bounds.x + ',' + bounds.y + ',' + bounds.width + ',' + bounds.height );

          properties.save();

          if ( generalPanel.getDeviceUpgradeEditor() != null )
          {
            generalPanel.getDeviceUpgradeEditor().dispose();
          }
          if ( keyMovePanel.getDeviceUpgradeEditor() != null )
          {
            keyMovePanel.getDeviceUpgradeEditor().dispose();
          }
          if ( devicePanel.getDeviceUpgradeEditor() != null )
          {
            devicePanel.getDeviceUpgradeEditor().dispose();
          }
        }
        catch ( Exception exc )
        {
          exc.printStackTrace( System.err );
        }
        finally
        {
          if ( doDispose )
          {
            dispose();
            // For some unknown reason, if map or rdf file folders had to be
            // set on startup, RMIR does not terminate on dispose(), so force
            // it in this case only.  It should not terminate if a RM
            // instance has been launched from it, so cannot force exit in
            // all cases.
            if ( RemoteManager.getRemoteManager().isFilesSet() )
            {
              System.exit( ABORT );
            }
          }
        }
      }
    } );

    addWindowStateListener( new WindowAdapter()
    {
      @Override
      public void windowStateChanged( WindowEvent e )
      {
        DeviceUpgradeEditor editor = devicePanel.getDeviceUpgradeEditor();
        if ( e.getNewState() == JFrame.NORMAL && editor != null && editor.getState() == JFrame.ICONIFIED )
        {
          editor.setExtendedState( NORMAL );
          editor.toFront();
        }
        else if ( e.getNewState() == JFrame.ICONIFIED && editor != null )
        {
          editor.setExtendedState( ICONIFIED );
        }
      }
    } );

    Container mainPanel = getContentPane();

    mainPanel.add( toolBar, BorderLayout.PAGE_START );

    // Set color for text on Progress Bars
    UIManager.put( "ProgressBar.selectionBackground", new javax.swing.plaf.ColorUIResource( Color.BLUE ) );
    UIManager.put( "ProgressBar.selectionForeground", new javax.swing.plaf.ColorUIResource( Color.BLUE ) );
    UIManager.put( "ProgressBar.foreground", new javax.swing.plaf.ColorUIResource( AQUAMARINE ) );

    tabbedPane = new JTabbedPane();
    mainPanel.add( tabbedPane, BorderLayout.CENTER );

    generalPanel = new GeneralPanel();
    tabbedPane.addTab( "General", generalPanel );
    generalPanel.addRMPropertyChangeListener( this );

    keyMovePanel = new KeyMovePanel();
    keyMovePanel.addRMPropertyChangeListener( this );

    macroPanel = new MacroPanel();
    macroPanel.addRMPropertyChangeListener( this );

    specialFunctionPanel = new SpecialFunctionPanel();
    specialFunctionPanel.addRMPropertyChangeListener( this );

    timedMacroPanel = new TimedMacroPanel();
    timedMacroPanel.addRMPropertyChangeListener( this );

    favScanPanel = new FavScanPanel();
    favScanPanel.addRMPropertyChangeListener( this );
    
    favoritesPanel = new FavoritesPanel();
    favoritesPanel.addRMPropertyChangeListener( this );

    devicePanel = new DeviceUpgradePanel();
    devicePanel.addRMPropertyChangeListener( this );

    protocolPanel = new ProtocolUpgradePanel();
    protocolPanel.addRMPropertyChangeListener( this );
    
    activityPanel = new ActivityPanel();
    activityPanel.addRMPropertyChangeListener( this );

    learnedPanel = new LearnedSignalPanel();
    learnedPanel.addRMPropertyChangeListener( this );

    rawDataPanel = new RawDataPanel();
    tabbedPane.addTab( "Raw Data", rawDataPanel );
    rawDataPanel.addRMPropertyChangeListener( this );
    
    segmentPanel = new SegmentPanel();
    segmentPanel.addRMPropertyChangeListener( this );

    tabbedPane.addChangeListener( this );

    statusBar = new JPanel( new CardLayout() );
    mainPanel.add( statusBar, BorderLayout.SOUTH );

    memoryStatus = new JPanel();
    extraStatus = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 0 ));
    interfaceStatus = new JPanel();
    warningStatus = new JPanel();
    JLabel warningMessage = new JLabel( "DO NOT EDIT THIS MAIN WINDOW WHILE THE DEVICE EDITOR IS OPEN" );
    Font font = warningMessage.getFont().deriveFont( Font.BOLD );
    warningMessage.setFont( font );
    warningStatus.add( warningMessage );
    
    interfaceState = new JProgressBar();

    interfaceStatus.add( interfaceState );
    interfaceState.setIndeterminate( true );
    interfaceState.setStringPainted( true );
    interfaceState.setString( "" );
    Dimension d = interfaceState.getPreferredSize();
    d.width *= 3;
    interfaceState.setPreferredSize( d );

    statusBar.add( memoryStatus, "MEMORY" );
    statusBar.add( interfaceStatus, "INTERFACE" );
    statusBar.add(  warningStatus, "WARNING" );
    ( ( CardLayout )statusBar.getLayout() ).first( statusBar );

    advProgressLabel = new JLabel( "Move/Macro:" );
    memoryStatus.add( advProgressLabel );

    advProgressBar = new JProgressBar();
    advProgressBar.setStringPainted( true );
    advProgressBar.setString( "N/A" );
    memoryStatus.add( advProgressBar );

    extraStatus.add( Box.createHorizontalStrut( 1 ) );
    JSeparator sep = new JSeparator( SwingConstants.VERTICAL );
    d = sep.getPreferredSize();
    d.height = advProgressBar.getPreferredSize().height;
    sep.setPreferredSize( d );
    extraStatus.add( sep );
    interfaceStatus.add( Box.createVerticalStrut( d.height ) );
    
    bleStatus.add( Box.createHorizontalStrut( 5 ) );
    sep = new JSeparator( SwingConstants.VERTICAL );
    sep.setPreferredSize( d );
    bleStatus.add( sep );
    bleStatus.add( Box.createHorizontalStrut( 5 ) );
    bleStatus.add( new JLabel( "Battery:" ) );
    batteryBar = new BatteryBar();
    int sizeUnit = d.height / 2;
    batteryBar.setPreferredSize( new Dimension( 6*sizeUnit - 2, sizeUnit ) );
    bleStatus.add( batteryBar );
    batteryVoltage = new JLabel();
    bleStatus.add( batteryVoltage );
    bleStatus.add( Box.createHorizontalStrut( 5 ) );
    sep = new JSeparator( SwingConstants.VERTICAL );
    sep.setPreferredSize( d );
    bleStatus.add( sep );
    bleStatus.add( Box.createHorizontalStrut( 5 ) );
    bleStatus.add( new JLabel( "Signal:" ) );
    signalProgressBar = new JProgressBar();
    signalProgressBar.setStringPainted( true );
    signalProgressBar.setPreferredSize( advProgressBar.getPreferredSize() );
    signalProgressBar.setFont( advProgressBar.getFont() );
    signalProgressBar.setMinimum( -91 );
    signalProgressBar.setMaximum( -38 );
    signalProgressBar.setForeground( AQUAMARINE );
    bleStatus.add( signalProgressBar );
    bleStatus.setVisible( false );

    memoryStatus.add( bleStatus );

    extraStatus.add( new JLabel( "Upgrade:" ) );

    upgradeProgressBar = new JProgressBar();
    upgradeProgressBar.setStringPainted( true );
    upgradeProgressBar.setString( "N/A" );

    upgradeProgressPanel = new JPanel();
    upgradeProgressPanel.setPreferredSize( advProgressBar.getPreferredSize() );
    upgradeProgressPanel.setLayout( new BorderLayout() );

    devUpgradeProgressBar = new JProgressBar();
    devUpgradeProgressBar.setStringPainted( true );
    devUpgradeProgressBar.setString( "N/A" );
    devUpgradeProgressBar.setVisible( false );

    upgradeProgressPanel.add( upgradeProgressBar, BorderLayout.NORTH );
    upgradeProgressPanel.add( devUpgradeProgressBar, BorderLayout.SOUTH );

    extraStatus.add( upgradeProgressPanel );

    extraStatus.add( Box.createHorizontalStrut( 5 ) );
    sep = new JSeparator( SwingConstants.VERTICAL );
    sep.setPreferredSize( d );
    extraStatus.add( sep );

    extraStatus.add( new JLabel( "Learned:" ) );

    learnedProgressBar = new JProgressBar();
    learnedProgressBar.setStringPainted( true );
    learnedProgressBar.setString( "N/A" );
    extraStatus.add( learnedProgressBar );
    memoryStatus.add( extraStatus );
    
    languages = new LinkedHashMap< Integer, LanguageDescriptor >();
    languages.put( 0, new LanguageDescriptor( "None", 0 ) );
    languages.put( 9, new LanguageDescriptor( "Danish", 9 ) );

    String temp = properties.getProperty( "RMBounds" );
    if ( temp != null )
    {
      Rectangle bounds = new Rectangle();
      StringTokenizer st = new StringTokenizer( temp, "," );
      bounds.x = Integer.parseInt( st.nextToken() );
      bounds.y = Integer.parseInt( st.nextToken() );
      bounds.width = Integer.parseInt( st.nextToken() );
      bounds.height = Integer.parseInt( st.nextToken() );
      setBounds( bounds );
    }
    else
    {
      pack();
    }
    currentPanel = generalPanel;
    setVisible( true );
  }

  /**
   * Gets the frame attribute of the RemoteMaster class.
   * 
   * @return The frame value
   */
  public static JP1Frame getFrame()
  {
    return frame;
  }
  
  public static LanguageDescriptor getLanguage( int code )
  {
    return languages.get( code );
  }
  
  public boolean useSavedData()
  {
    return useSavedDataItem.isSelected();
  }
  
  public static boolean getSystemFiles()
  {
    return getSystemFilesItem.isSelected();
  }
  
  public static String getSystemZipName( Remote remote )
  {
    String sig = remote.getSignature();
    return "Sys" + sig.substring( 3 ) + ".zip";
  }
  
  public static ZipFile getSystemZipFile( Remote remote )
  {
    ZipFile zipfile = null;
    try
    {
      String zipName = getSystemZipName( remote );
      File inputDir = new File( RemoteMaster.getWorkDir(), "XSight" );
      File file = new File( inputDir, zipName );
      if ( file.exists() )
      {
        zipfile = new ZipFile( new File( inputDir, zipName ) );
      }
    }
    catch( Exception e )
    {
      return null;
    }
    return zipfile;
  }
  
  public static void setSystemFilesItems( RemoteMaster rm, Remote remote )
  {
    if ( remote != null && remote.isSSD() )
    {
      xziteOps.setVisible( rmirSys.exists() );
      digitalOps.setVisible( false );
      extractSSItem.setVisible( false );
      ZipFile zipfile = getSystemZipFile( remote );
      parseIRDBItem.setEnabled( zipfile != null && zipfile.getEntry( "irdb.bin" ) != null );
      try {
        if ( zipfile != null )
          zipfile.close();
      }
      catch ( Exception e ){}
      return;
    }
    if ( remote != null && remote.isFDRA() )
    {
      xziteOps.setVisible( false );
      digitalOps.setVisible( rmirSys.exists() );
      upgradeSourceItem.setVisible( admin || !remote.getSignature().equals( "USB0007" ) );
      extractSSItem.setVisible( false );
      return;
    }
    
    xziteOps.setVisible( false );
    digitalOps.setVisible( false );
    extractSSItem.setVisible( admin && ( remote != null && remote.usesSimpleset() || rm.binLoaded() != null ) );
    extractSSItem.setEnabled( admin && rm.binLoaded() != null );
  }
  
  public static byte[] readBinary( File file )
  {
    byte[] data = null;
    try
    {
      int length = ( int )file.length();
      if ( length == 0 )
      {
        System.err.println( "File " + file.getAbsolutePath() + " empty or not found" );
        return null;
      }
      InputStream in = new FileInputStream( file );
      data = readBinary( in, length );
      in.close();
    }
    catch (IOException ex) {
      System.err.println( ex );
      return null;
    }
    return data;
  }
  
  public static byte[] readBinary( InputStream in, int length )
  {
    return readBinary( in, length, false );
  }
  
  public static byte[] readBinary( InputStream in, int length, boolean quiet )
  {
    if ( in == null || length == 0 ) return null;
    int totalBytesRead = 0;
    byte[] data = new byte[ length ];
    InputStream input = null;
    try 
    {
      input = new BufferedInputStream( in );
      while( totalBytesRead < length )
      {
        int bytesRemaining = data.length - totalBytesRead;
        //input.read() returns -1, 0, or more :
        int bytesRead = input.read( data, totalBytesRead, bytesRemaining ); 
        if ( bytesRead > 0 )
        {
          totalBytesRead = totalBytesRead + bytesRead;
        }
      }
      input.close();
      /*
           the while loop usually has a single iteration only.
       */
      if ( totalBytesRead != length )
      {
        System.err.println( "File read error: file length = " + length + ", bytes read = " + totalBytesRead );
        return null;
      }
      if ( !quiet )
      {
        System.err.println( "Bytes read from file: " + totalBytesRead );
      }
    }
    catch ( Exception ex ) {
      System.err.println( ex );
      return null;
    }
    return data;
  }

  public static ImageIcon createIcon( String imageName )
  {
    String imgLocation = "toolbarButtonGraphics/general/" + imageName + ".gif";
    java.net.URL imageURL = DynamicURLClassLoader.getInstance().getResource( imgLocation );

    if ( imageURL == null )
    {
      imgLocation = "toolbarButtonGraphics/media/" + imageName + ".gif";
      imageURL = DynamicURLClassLoader.getInstance().getResource( imgLocation );
    }
    if ( imageURL == null )
    {
      System.err.println( "Resource not found: " + imgLocation );
      return null;
    }
    else
    {
      return new ImageIcon( imageURL );
    }
  }

  /**
   * Description of the Method.
   */
  private void createMenus()
  {
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar( menuBar );

    JMenu menu = new JMenu( "File" );
    menu.setMnemonic( KeyEvent.VK_F );
    menuBar.add( menu );

    JMenu newMenu = new JMenu( "New" );
    newMenu.setMnemonic( KeyEvent.VK_N );
    menu.add( newMenu );

    newAction = new RMAction( "Remote Image...", "NEW", createIcon( "RMNew24" ), "Create new file", KeyEvent.VK_R );
    newMenu.add( newAction ).setIcon( null );

    newUpgradeAction = new RMAction( "Device Upgrade", "NEWDEVICE", null, "Create new Device Upgrade", KeyEvent.VK_D );
    newMenu.add( newUpgradeAction );

    if ( rmpbIcon.exists() )
    {
      newProtocolAction = new RMAction( "Protocol", "NEWPROTOCOL", null, "Create new Protocol", KeyEvent.VK_P );
      newMenu.add( newProtocolAction );
    }

    openAction = new RMAction( "Open...", "OPEN", createIcon( "RMOpen24" ), "Open a file", KeyEvent.VK_O );
    menu.add( openAction ).setIcon( null );

    saveAction = new RMAction( "Save", "SAVE", createIcon( "Save24" ), "Save to file", KeyEvent.VK_S );
    saveAction.setEnabled( false );
    menu.add( saveAction ).setIcon( null );

    saveAsAction = new RMAction( "Save as...", "SAVEAS", createIcon( "SaveAs24" ), "Save to a different file",
        KeyEvent.VK_A );
    saveAsAction.setEnabled( false );
    JMenuItem menuItem = menu.add( saveAsAction );
    menuItem.setDisplayedMnemonicIndex( 5 );
    menuItem.setIcon( null );

    // revertItem = new JMenuItem( "Revert to saved" );
    // revertItem.setMnemonic( KeyEvent.VK_R );
    // revertItem.addActionListener( this );
    // menu.add( revertItem );

    menu.addSeparator();

    installExtenderItem = new JMenuItem( "Install Extender..." );
    installExtenderItem.setMnemonic( KeyEvent.VK_I );
    installExtenderItem.addActionListener( this );
    installExtenderItem.setEnabled( false );
    menu.add( installExtenderItem );
    
    JMenu importFromWavSubMenu = new JMenu( "Import from Wav" );
    importFromWavSubMenu.setMnemonic( KeyEvent.VK_M );
    importFromWavSubMenu.setToolTipText( "Load into RMIR a .wav file created for modem upgrade." );
    menu.add( importFromWavSubMenu );
    
    importFromWavNewItem = new JMenuItem( "New Image..." );
    importFromWavNewItem.setMnemonic( KeyEvent.VK_N );
    importFromWavNewItem.addActionListener( this );
    importFromWavSubMenu.add( importFromWavNewItem );
    
    importFromWavMergeItem = new JMenuItem( "Merge with Current..." );
    importFromWavMergeItem.setMnemonic( KeyEvent.VK_M );
    importFromWavMergeItem.addActionListener( this );
    importFromWavMergeItem.setEnabled( false );
    importFromWavSubMenu.add( importFromWavMergeItem );
    
    exportToWavSubMenu = new JMenu( "Export to Wav" );
    exportToWavSubMenu.setMnemonic( KeyEvent.VK_E );
    exportToWavSubMenu.setToolTipText( 
        "<html>Export the whole or part of the current setup of a modem-enabled remote<br>"
            + "as a .wav file for modem upgrade.  The item &quot;Upload using Wav&quot; on the<br>"
            + "Remote menu may be used to play the file for uploading to the remote.</html>" );
    exportToWavSubMenu.setEnabled( false );
    menu.add( exportToWavSubMenu );
    
    exportToWavImageItem = new JMenuItem( "Entire Image..." );
    exportToWavImageItem.setMnemonic( KeyEvent.VK_E );
    exportToWavImageItem.addActionListener( this );
    exportToWavSubMenu.add( exportToWavImageItem );
    
    exportToWavSettingsItem = new JMenuItem( "Settings..." );
    exportToWavSettingsItem.setMnemonic( KeyEvent.VK_S );
    exportToWavSettingsItem.addActionListener( this );
    exportToWavSubMenu.add( exportToWavSettingsItem );

    exportToWavMacrosEtcItem = new JMenuItem( "KeyMoves, Macros, Fav Lists..." );
    exportToWavMacrosEtcItem.setMnemonic( KeyEvent.VK_K );
    exportToWavMacrosEtcItem.addActionListener( this );
    exportToWavSubMenu.add( exportToWavMacrosEtcItem );
    
    exportToWavTimedMacrosItem = new JMenuItem( "Timed Macros..." );
    exportToWavTimedMacrosItem.setMnemonic( KeyEvent.VK_T );
    exportToWavTimedMacrosItem.addActionListener( this );
    exportToWavSubMenu.add( exportToWavTimedMacrosItem );
 
    exportToWavUpgradesItem = new JMenuItem( "Upgrades..." );
    exportToWavUpgradesItem.setMnemonic( KeyEvent.VK_U );
    exportToWavUpgradesItem.addActionListener( this );
    exportToWavSubMenu.add( exportToWavUpgradesItem );
    
    exportToWavLearnedItem = new JMenuItem( "Learned Signals..." );
    exportToWavLearnedItem.setMnemonic( KeyEvent.VK_L );
    exportToWavLearnedItem.addActionListener( this );
    exportToWavSubMenu.add( exportToWavLearnedItem );
    
    menu.addSeparator();

    JMenu menuSetDirectory = new JMenu( "Set Directory" );
    menuSetDirectory.setMnemonic( KeyEvent.VK_D );
    menu.add( menuSetDirectory );

    rdfPathItem = new JMenuItem( "RDF Path..." );
    rdfPathItem.setMnemonic( KeyEvent.VK_R );
    rdfPathItem.addActionListener( this );
    menuSetDirectory.add( rdfPathItem );

    mapPathItem = new JMenuItem( "Image Path..." );
    mapPathItem.setMnemonic( KeyEvent.VK_I );
    mapPathItem.addActionListener( this );
    menuSetDirectory.add( mapPathItem );

    addonPathItem = new JMenuItem( "AddOns Path..." );
    addonPathItem.setMnemonic( KeyEvent.VK_A );
    addonPathItem.addActionListener( this );
    menuSetDirectory.add( addonPathItem );
    
    menu.addSeparator();
    
    JMenu menuSummary = new JMenu( "Summary" );
    menuSummary.setMnemonic( KeyEvent.VK_Y );
    menu.add( menuSummary );

    createSummaryItem = new JMenuItem( "Create summary" );
    createSummaryItem.setMnemonic( KeyEvent.VK_C );
    createSummaryItem.addActionListener( this );
    createSummaryItem.setToolTipText( "<html>Opens a printable summary of the current setup in the default web<br>"
        + "browser, showing all RMIR tables in tabular form.</html>" );
    createSummaryItem.setEnabled( false );
    menuSummary.add( createSummaryItem );
    
    viewSummaryItem = new JMenuItem( "View last summary" );
    viewSummaryItem.setMnemonic( KeyEvent.VK_V );
    viewSummaryItem.addActionListener( this );
    viewSummaryItem.setToolTipText( "Opens the most recently created summary in the default web browser." );
    menuSummary.add( viewSummaryItem );
    
    saveSummaryItem = new JMenuItem( "Save last summary..." );
    saveSummaryItem.setMnemonic( KeyEvent.VK_S );
    saveSummaryItem.addActionListener( this );
    saveSummaryItem.setToolTipText( "<html>Opens a dialog to save the most recently created summary as an HTML<br>"
          + "file, whether or not that summary is still open in the browser.</html>");
    menuSummary.add( saveSummaryItem );
    
    menu.addSeparator();

    recentFiles = new JMenu( "Recent" );
    menu.add( recentFiles );
    recentFiles.setEnabled( false );
    for ( int i = 0; i < 10; i++ )
    {
      String propName = "RecentIRs." + i;
      String temp = properties.getProperty( propName );
      if ( temp == null )
      {
        break;
      }
      properties.remove( propName );
      File f = new File( temp );
      if ( f.canRead() )
      {
        JMenuItem item = new JMenuItem( temp );
        item.setActionCommand( temp );
        item.addActionListener( this );
        recentFiles.add( item );
      }
    }
    if ( recentFiles.getItemCount() > 0 )
    {
      recentFiles.setEnabled( true );
    }
    menu.addSeparator();

    exitItem = new JMenuItem( "Exit", KeyEvent.VK_X );
    exitItem.addActionListener( this );
    menu.add( exitItem );

    menu = new JMenu( "Remote" );
    menu.setMnemonic( KeyEvent.VK_R );
    menuBar.add( menu );

    File userDir = workDir;
    try
    {
      JP12Serial jp12Serial = new JP12Serial( userDir );
      interfaces.add( jp12Serial );
      System.err.println( "    JP12Serial version " + jp12Serial.getInterfaceVersion() );
    }
    catch ( LinkageError le )
    {
      System.err.println( "Unable to create JP12Serial object: " + le.getMessage() );
    }

    try
    {
      CommHID commHID = new CommHID( userDir );
      interfaces.add( commHID );
      System.err.println( "    CommHID version " + commHID.getInterfaceVersion() );
    }
    catch ( LinkageError le )
    {
      System.err.println( "Unable to create CommHID object: " + le.getMessage() );
    }

    try
    {
      JP2BT jp2bt = new JP2BT( userDir );
      interfaces.add( jp2bt );
      System.err.println( "    JP2BT version " + jp2bt.getInterfaceVersion() );
    }
    catch ( LinkageError le )
    {
      System.err.println( "Unable to create JP2BT object: " + le.getMessage() );
    }

    try
    {
      JP1USB jp1usb = new JP1USB( userDir );
      interfaces.add( jp1usb );
      System.err.println( "    JP1USB version " + jp1usb.getInterfaceVersion() );
//      System.err.println( "    EEPROM size returns " + jp1usb.getRemoteEepromSize() );
//      System.err.println( "    EEPROM address returns " + jp1usb.getRemoteEepromAddress() );
    }
    catch ( LinkageError le )
    {
      System.err.println( "Unable to create JP1USB object: " + le.getMessage() );
    }
    
    try
    {
      JPS jps = new JPS( userDir );
      interfaces.add( jps );
      System.err.println( "    JPS version " + jps.getInterfaceVersion() );
    }
    catch ( LinkageError le )
    {
      System.err.println( "Unable to create JPS object: " + le.getMessage() );
    }

    try
    {
      JP1Parallel jp1Parallel = new JP1Parallel( userDir );
      interfaces.add( jp1Parallel );
      System.err.println( "    JP1Parallel version " + jp1Parallel.getInterfaceVersion() );
      if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
      {        
        double parallelVer = 0.0;
        try
        {
          parallelVer = Double.parseDouble( jp1Parallel.getInterfaceVersion() );
        }
        catch ( NumberFormatException e ) {}
        if ( parallelVer > 0.10 )
        {     
          boolean isInpOutDriverOpen= jp1Parallel.testIsInpOutDriverOpen();
          System.err.println( "    JP1Parallel InpOutDriver is " + ( isInpOutDriverOpen ? "open" : "not open" ) );
//          System.err.println( "    EEPROM size returns " + jp1Parallel.getRemoteEepromSize() );
//          System.err.println( "    EEPROM address returns " + jp1Parallel.getRemoteEepromAddress() );
          if ( !isInpOutDriverOpen )
          {
            System.err.println( "    *** To use the parallel port interface, RMIR must be run once as adminstrator.\n"
                + "    This enables it to install the InpOut driver, a once-only task needed for\n"
                + "    the interface to access the port." );
          }
        }
      }
    }
    catch ( LinkageError le )
    {
      System.err.println( "Unable to create JP1Parallel object: " + le.getMessage() );
    }

    /*
     * try { JP2Serial jp2Serial = new JP2Serial( userDir ); interfaces.add( jp2Serial ); System.err.println(
     * "    JP12Serial version " + jp2Serial.getInterfaceVersion() ); } catch ( LinkageError le ) { System.err.println(
     * "Unable to create JP12Serial object: " + le.getMessage() ); }
     */

    ActionListener interfaceListener = new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        String command = event.getActionCommand();
        if ( command.equals( "autodetect" ) )
        {
          properties.remove( "Interface" );
          properties.remove( "Port" );
          bluetoothItem.setVisible( false );
          finderItem.setVisible( false );
          uploadAction.setEnabled( uploadable );
          recreateToolbar();
          return;
        }

        for ( IO io : interfaces )
        {
          if ( io.getInterfaceName().equals( command ) )
          {
            String defaultPort = null;
            if ( command.equals( properties.getProperty( "Interface" ) ) )
            {
              defaultPort = properties.getProperty( "Port" );
            }

            String[] availablePorts = io.getPortNames();

            PortDialog d = new PortDialog( RemoteMaster.this, availablePorts, defaultPort );
            if ( command.equals( "JPS" ) && defaultPort == null && ( ( JPS )io ).isOpen() )
            {
              d.setOtherPort( ( ( JPS )io ).getFilePath() );
            }
            d.getAutodetect().setVisible( !command.equals( "JP2BT" ) );
            
            d.setVisible( true );
            if ( d.getUserAction() == JOptionPane.OK_OPTION )
            {
              String port = d.getPort();
              properties.setProperty( "Interface", io.getInterfaceName() );
              if ( port == null || port.equals( PortDialog.AUTODETECT ) )
              {
                properties.remove( "Port" );
              }
              else
              {
                properties.setProperty( "Port", port );
              }
              uploadAction.setEnabled( uploadable && allowUpload() );
              recreateToolbar();
            }

            break;
          }
        }
      }
    };

    if ( !interfaces.isEmpty() )
    {
      JMenu subMenu = new JMenu( "Interface" );
      menu.add( subMenu );
      subMenu.setMnemonic( KeyEvent.VK_I );
      ButtonGroup group = new ButtonGroup();
      String preferredInterface = properties.getProperty( "Interface" );
      JRadioButtonMenuItem item = new JRadioButtonMenuItem( "Auto-detect" );
      item.setActionCommand( "autodetect" );
      item.setSelected( preferredInterface == null );
      subMenu.add( item );
      group.add( item );
      item.setMnemonic( KeyEvent.VK_A );
      item.addActionListener( interfaceListener );

      ListIterator< IO > it = interfaces.listIterator();
      while ( it.hasNext() )
      {
        IO io = it.next();
        try
        {
          String ioName = io.getInterfaceName();
          item = new JRadioButtonMenuItem( ioName + "..." );
          item.setActionCommand( ioName );
          item.setSelected( ioName.equals( preferredInterface ) );
          subMenu.add( item );
          group.add( item );
          item.addActionListener( interfaceListener );
        }
        catch ( UnsatisfiedLinkError ule )
        {
          it.remove();
          String className = io.getClass().getName();
          int dot = className.lastIndexOf( '.' );
          if ( dot != -1 )
          {
            className = className.substring( dot + 1 );
          }
          JOptionPane.showMessageDialog( this, "An incompatible version of the " + className
              + " driver was detected.  You will not be able to download or upload using that driver.",
              "Incompatible Driver", JOptionPane.ERROR_MESSAGE );
          ule.printStackTrace( System.err );
        }
      }
    }
        
    bluetoothAction = new RMAction( "Connect by Bluetooth", "BLUETOOTH", createIcon( "RMBTOn24" ),
        "Connect to the remote by Bluetooth", KeyEvent.VK_B );
    bluetoothAction.putValue( Action.SELECTED_KEY, true );
    
    bluetoothItem = new JCheckBoxMenuItem();
    bluetoothItem.setAction( bluetoothAction );
    bluetoothItem.setIcon( null );
    bluetoothItem.setSelected( false );
    menu.add( bluetoothItem );
    
    bluetoothButton = new JToggleButton();
    bluetoothButton.setAction( bluetoothAction );
    bluetoothButton.setHideActionText( true );
    bluetoothButton.setBorder( BorderFactory.createRaisedBevelBorder() );
    
    finderAction = new RMAction( "Find remote", "FINDER", createIcon( "Volume24" ),
        "Sound the remote's finder", KeyEvent.VK_F );
    finderAction.putValue( Action.SELECTED_KEY, true );
    
    finderItem = new JCheckBoxMenuItem();
    finderItem.setAction( finderAction );
    finderItem.setIcon( null );
    finderItem.setSelected( false );
    menu.add( finderItem );
    
    finderButton = new JToggleButton();
    finderButton.setAction( finderAction );
    finderButton.setHideActionText( true );
    finderButton.setBorder( BorderFactory.createRaisedBevelBorder() );

    downloadAction = new RMAction( "Download from Remote", "DOWNLOAD", createIcon( "Import24" ),
        "Download from the attached remote", KeyEvent.VK_D );
    downloadAction.setEnabled( !interfaces.isEmpty() );
    menu.add( downloadAction ).setIcon( null );

    uploadAction = new RMAction( "Upload to Remote", "UPLOAD", createIcon( "Export24" ),
        "Upload to the attached remote", KeyEvent.VK_U );
    uploadable = false;
    uploadAction.setEnabled( false );
    menu.add( uploadAction ).setIcon( null );

    openRdfAction = new RMAction( "Open RDF...", "OPENRDF", createIcon( "RMOpenRDF24" ), "Open RDF to view or edit",
        null );
    openRdfAction.setEnabled( false );

    codesAction = new RMAction( "Code Selector...", "OPENCODES", createIcon( "RMCodes24" ), "Open Code Selector", null );
    codesAction.setEnabled( false );

    highlightAction = new RMAction( "Highlight...", "HIGHLIGHT", createIcon( "RMHighlight24" ),
        "Select highlight color", null );
    highlightAction.setEnabled( false );

    menu.addSeparator();
    downloadRawItem = new JMenuItem( "Raw download", KeyEvent.VK_R );
    downloadRawItem.setEnabled( true );
    downloadRawItem.addActionListener( this );
    menu.add( downloadRawItem );
    
    uploadWavItem = new JMenuItem( "Upload using Wav...", KeyEvent.VK_W );
    uploadWavItem.setToolTipText( 
        "<html>Play a .wav file for uploading to a remote with modem upgrade<br>"
            + "capability.  See the Wiki on the JP1 website for guidance.  You<br>"
            + "can use the item &quot;Export to Wav&quot; on the File menu to<br>"
            + "create a .wav file from an existing setup for such a remote.</html>" );
    uploadWavItem.addActionListener( this );
    menu.add( uploadWavItem );
    
    cancelWavUploadItem = new JMenuItem( "Cancel Wav Upload", KeyEvent.VK_C );
    cancelWavUploadItem.setToolTipText( "Cancel the playback of a .wav file during a modem upgrade." );
    cancelWavUploadItem.addActionListener( this );
    cancelWavUploadItem.setEnabled( false );
    menu.add( cancelWavUploadItem );

    menu.addSeparator();
    verifyUploadItem = new JCheckBoxMenuItem( "Verify after upload" );
    verifyUploadItem.setMnemonic( KeyEvent.VK_V );
    verifyUploadItem.setSelected( Boolean.parseBoolean( properties.getProperty( "verifyUpload", "true" ) ) );
    verifyUploadItem.addActionListener( this );
    menu.add( verifyUploadItem );

    menu = new JMenu( "Options" );
    menu.setMnemonic( KeyEvent.VK_O );
    menuBar.add( menu );

    JMenu subMenu = new JMenu( "Look and Feel" );
    subMenu.setMnemonic( KeyEvent.VK_L );
    menu.add( subMenu );
    
    boolean useDecodeIR = LearnedSignal.hasDecodeIR()
        && Boolean.parseBoolean( RemoteMaster.getProperties().getProperty( "UseDecodeIR", "false" ) );
    System.err.println( "Using " + ( useDecodeIR ? "DecodeIR" : "IrpTransmogrifier" )
        + " as decoder of learned signals");
    JMenu irSubMenu = new JMenu( "Set IR Decoder" );
    ButtonGroup group = new ButtonGroup();
    irpTransmogrifierItem = new JRadioButtonMenuItem( "IrpTransmogrifier" );
    irpTransmogrifierItem.addActionListener( this );
    irpTransmogrifierItem.setSelected( !useDecodeIR );
    irpTransmogrifierItem.setMnemonic( KeyEvent.VK_I );
    group.add( irpTransmogrifierItem );
    irSubMenu.add( irpTransmogrifierItem );
    decodeIRItem = new JRadioButtonMenuItem( "DecodeIR" );
    decodeIRItem.addActionListener( this );
    decodeIRItem.setEnabled( LearnedSignal.hasDecodeIR() );
    decodeIRItem.setSelected( useDecodeIR );
    decodeIRItem.setMnemonic( KeyEvent.VK_D );
    group.add( decodeIRItem );
    irSubMenu.add( decodeIRItem );
    menu.add( irSubMenu );

    highlightItem = new JCheckBoxMenuItem( "Highlighting" );
    highlightItem.setMnemonic( KeyEvent.VK_H );
    highlightItem.setSelected( Boolean.parseBoolean( properties.getProperty( "highlighting", "false" ) ) );
    highlightItem.addActionListener( this );
    menu.add( highlightItem );

    enablePreserveSelection = new JCheckBoxMenuItem( "Allow Preserve Control" );
    enablePreserveSelection.setMnemonic( KeyEvent.VK_A );
    enablePreserveSelection.setSelected( Boolean.parseBoolean( properties.getProperty( "enablePreserveSelection", "false" ) ) );
    enablePreserveSelection.addActionListener( this );
    enablePreserveSelection.setToolTipText( "<html>Allow control of which function data is preserved when changing the protocol used in a device upgrade.<br>Do not use this unless you know what you are doing and why.</html>" );
    menu.add( enablePreserveSelection );
    
    showSlingboxProtocols = new JCheckBoxMenuItem( "Show Slingbox protocols" );
    showSlingboxProtocols.setMnemonic( KeyEvent.VK_X );
    showSlingboxProtocols.setSelected( Boolean.parseBoolean( properties.getProperty( "ShowSlingboxProtocols", "false" ) ) );
    showSlingboxProtocols.addActionListener( this );
    showSlingboxProtocols.setToolTipText( "<html>Include the no-repeat protocols that are specific to Slingbox usage.<br>"
        + "Note that a change to this option only takes effect when RM or RMIR<br>is next opened.</html>" );
    menu.add( showSlingboxProtocols );
    
    JMenu tooltipSubMenu = new JMenu( "Set Tooltip Delay" );
    tooltipSubMenu.setMnemonic( KeyEvent.VK_T );
    tooltipSubMenu.setToolTipText( "<html>Set the delay between mouse pointer entering an object and the tooltip (help message)<br>"
        + "appearing.  This delay does not apply to cells in tables, where tooltips are primarily used<br>"
        + "to show the full content when it does not fit into the cell concerned.</html>" );
    menu.add( tooltipSubMenu );
    ToolTipManager tm = ToolTipManager.sharedInstance();
    tooltipDefaultDelay = tm.getInitialDelay();
    String temp = properties.getProperty( "TooltipDelay" );
    tooltipDelay = temp != null ? Integer.parseInt( temp ) : tooltipDefaultDelay;
    tm.setInitialDelay( tooltipDelay );
    
    group = new ButtonGroup();
    defaultDelayItem = new JRadioButtonMenuItem( "Default delay" );
    defaultDelayItem.setSelected( temp == null );
    defaultDelayItem.addActionListener( this );
    tooltipSubMenu.add( defaultDelayItem );
    group.add( defaultDelayItem );
    specifiedDelayItem = new JRadioButtonMenuItem( "Specified delay..." );
    specifiedDelayItem.setSelected( temp != null );
    specifiedDelayItem.addActionListener( this );
    tooltipSubMenu.add( specifiedDelayItem );
    group.add( specifiedDelayItem );
    
    noUpgradeItem = new JCheckBoxMenuItem( "No XSight Firmware Upgrade" );
    noUpgradeItem.setSelected( Boolean.parseBoolean( properties.getProperty( "NoUpgrade", "false" ) ) );
    noUpgradeItem.addActionListener( this );
    noUpgradeItem.setToolTipText( "<html>Selecting this option will stop download of an XSight<br>"
        + "Touch-style or Nevo remote offering to upgrade the remote,<br>"
        + "even if an upgrade is available.</html>" );
    menu.add( noUpgradeItem );

    appendAdvancedOptions( menu );

    ActionListener al = new ActionListener()
    {
      public void actionPerformed( ActionEvent e )
      {
        lfEvent = e;       
        SwingUtilities.invokeLater( new Runnable()
        {
          public void run()
          {
            try
            {
              String title = "Look and Feel";
              String message = "Due to a bug in Java, you may find it necessary to close and then re-open RMIR\n"
                  + "for it to work properly after a change of Look and Feel.  Moreover, you may need\n"
                  + "to use the menu item File > Exit to close it.  To abort the change press Cancel,\n"
                  + "otherwise press OK to continue.";
              if ( JOptionPane.showConfirmDialog( RemoteMaster.this, message, title, JOptionPane.OK_CANCEL_OPTION, 
                  JOptionPane.INFORMATION_MESSAGE ) == JOptionPane.CANCEL_OPTION )
              {
                String lf = UIManager.getLookAndFeel().getName();
                for ( JRadioButtonMenuItem item : lookAndFeelItems )
                {
                  if ( item.getText().equals( lf ) )
                  {
                    item.setSelected( true );
                    break;
                  }
                }
                return;
              }
              JRadioButtonMenuItem item = ( JRadioButtonMenuItem )lfEvent.getSource();
              String lf = item.getActionCommand();
              UIManager.setLookAndFeel( lf );
              KeyMoveTableModel.normalSelectedBGColor = UIManager.getColor( "Table.selectionBackground" );
              SwingUtilities.updateComponentTreeUI( RemoteMaster.this );
              RemoteMaster.this.pack();
              properties.setProperty( "LookAndFeel", lf );
            }
            catch ( Exception x )
            {
              x.printStackTrace( System.err );
            }
          }
        } );
      }
    };

    group = new ButtonGroup();
    String lookAndFeel = UIManager.getLookAndFeel().getClass().getName();
    UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
    lookAndFeelItems = new JRadioButtonMenuItem[ info.length ];
    for ( int i = 0; i < info.length; i++ )
    {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem( info[ i ].getName() );
      lookAndFeelItems[ i ] = item;
      item.setMnemonic( item.getText().charAt( 0 ) );
      item.setActionCommand( info[ i ].getClassName() );
      group.add( item );
      subMenu.add( item );
      if ( item.getActionCommand().equals( lookAndFeel ) )
      {
        item.setSelected( true );
      }
      item.addActionListener( al );
    }

    menu = new JMenu( "Advanced" );
    menu.setMnemonic( KeyEvent.VK_A );
    menuBar.add( menu );

    cleanUpperMemoryItem = new JMenuItem( "Blast Upper Memory...", KeyEvent.VK_B );
    cleanUpperMemoryItem.setEnabled( false );
    cleanUpperMemoryItem.addActionListener( this );
    menu.add( cleanUpperMemoryItem );

    clearAltPIDHistory = new JMenuItem( "Clear Alt PID History...", KeyEvent.VK_H );
    clearAltPIDHistory.addActionListener( this );
    menu.add( clearAltPIDHistory );

    menu.addSeparator();

    initializeTo00Item = new JMenuItem( "Initialize to $00", KeyEvent.VK_0 );
    initializeTo00Item.setEnabled( false );
    initializeTo00Item.addActionListener( this );
    menu.add( initializeTo00Item );

    initializeToFFItem = new JMenuItem( "Initialize to $FF", KeyEvent.VK_F );
    initializeToFFItem.setEnabled( false );
    initializeToFFItem.addActionListener( this );
    menu.add( initializeToFFItem );
    
    menu.addSeparator();
    
    useSavedDataItem = new JCheckBoxMenuItem( "Preserve original data" );
    useSavedDataItem.setSelected( false );
    useSavedDataItem.addActionListener( this );
    useSavedDataItem.setToolTipText( 
        "<html>When selected, the Raw Data tab displays the data as downloaded from the remote<br>"
        + "or loaded from a file, free from any changes made to it by RMIR.  An upload to the<br>"
        + "remote also uploads this unmodified data.</html>");
    menu.add( useSavedDataItem );
    
    setBaselineItem = new JMenuItem( "Set baseline" );
    setBaselineItem.addActionListener( this );
    setBaselineItem.setEnabled( false );
    setBaselineItem.setToolTipText( 
        "<html>Sets baseline to either normal or original data, depending on whether or not<br>"
        + "\"Preserve original data\" is selected.  When a baseline is set, the Raw Data tab<br>"
        + "highlights differences between current and baseline data in RED.</html>" );
    menu.add( setBaselineItem );
    clearBaselineItem = new JMenuItem( "Clear baseline" );
    clearBaselineItem.addActionListener( this );
    clearBaselineItem.setEnabled( false );
    clearBaselineItem.setToolTipText( 
        "Clears the baseline so that the Raw Data tab no longer highlights differences from it" );
    menu.add( clearBaselineItem );
    
    menu.addSeparator();
    
    xziteOps = new JMenu( "XSight operations" );
    menu.add( xziteOps );
    xziteOps.setVisible( false );

    xziteOpsItem = new JMenuItem( "List/Remove/Save XSight files..." );
    xziteOpsItem.setToolTipText( "<html>Lists all files in the XSight file system and gives<br>"
        + "option to remove or save to PC selected items one<br>"
        + "at a time.</html>" );
    xziteOpsItem.addActionListener( this );
    xziteOpsItem.setVisible( admin );
    xziteOps.add( xziteOpsItem );
    
    verifyXZITEfilesItem = new JMenuItem( "Verify XSight system files" );
    verifyXZITEfilesItem.setToolTipText( "<html>Validates the content of all XSight system files<br>"
        + "and returns a description of any discrepancies" );
    verifyXZITEfilesItem.addActionListener( this );
    xziteOps.add( verifyXZITEfilesItem );
    
    putSystemFileItem = new JMenuItem( "Upload XSight system file..." );
    putSystemFileItem.setToolTipText( "<html>Uploads system files to the remote one at a time, selected from<br>"
        + "the files of the latest available firmware version.  Note that<br>"
        + "this option is only available if the MCU firmware is already<br>"
        + "the latest version.  This option is intended to provide a means<br>"
        + "to restore accidentally corrupted or deleted files.</html>" );
    putSystemFileItem.addActionListener( this );
    xziteOps.add( putSystemFileItem );
    
    xziteReformatItem = new JMenuItem( "Format and rebuild XSight file system..." );
    xziteReformatItem.setToolTipText( "<html>Formats the file system of the remote and restores the system files.<br>"
        + "The MCU firmware is not affected.  The remote is left in a factory<br>"
        + "reset state." );
    xziteReformatItem.addActionListener( this );
    xziteOps.add( xziteReformatItem );
 
    xziteOps.addSeparator();
    
    forceUpgradeItem = new JCheckBoxMenuItem( "Force XSight Firmware Upgrade" );
    forceUpgradeItem.setSelected( false );
    forceUpgradeItem.setToolTipText( "<html>Selecting this option will force download of an XSight<br>"
        + "Touch-style or Nevo remote to offer to upgrade the remote,<br>"
        + "even if the current firmware is up-to-date.</html>" );
    xziteOps.add( forceUpgradeItem );
    
    getSystemFilesItem = new JCheckBoxMenuItem( "Get XSight system files" );
    getSystemFilesItem.setToolTipText( "<html>When checked, a download from the remote also copies<br>"
        + "the system files of the remote to a zip file in the XSight<br>"
        + "subfolder of the installation folder, creating this subfolder<br>"
        + "if it does not exist.  The name of the zip file is Sys followed<br>"
        + "by the USB PID of the remote.</html>" );
    getSystemFilesItem.setSelected( false );
    getSystemFilesItem.setVisible( admin );
    getSystemFilesItem.addActionListener( this );
    xziteOps.add( getSystemFilesItem ); 
    
    parseIRDBItem = new JMenuItem( "Extract from irdb.bin" );
    parseIRDBItem.setToolTipText( "<html>Extracts data for an RDF from the copy of the irdb.bin<br>"
        + "system file in the zip file for this remote created by<br>"
        + "the \"Get XSight system files\" option.</html>" );
    parseIRDBItem.addActionListener( this );
    parseIRDBItem.setVisible( admin );
    xziteOps.add( parseIRDBItem );
    
    digitalOps = new JMenu( "XSight operations" );
    menu.add( digitalOps );
    digitalOps.setVisible( false );
    
    forceFDRAUpgradeItem = new JCheckBoxMenuItem( "Force XSight Firmware Upgrade" );
    forceFDRAUpgradeItem.setSelected( false );
    forceFDRAUpgradeItem.setToolTipText( "<html>Selecting this option will force download of any XSight<br>"
        + "or Nevo remote to offer to upgrade the remote, even if<br>"
        + "the current firmware is up-to-date.</html>" );
    digitalOps.add( forceFDRAUpgradeItem );

    saveFDRAfirmware = new JMenuItem( "Save FDRA firmware" );
    saveFDRAfirmware.setToolTipText( "<html>Saves the firmware of XSight FDRA remote to a file in the XSight<br>"
        + "subfolder of the installation folder, creating this subfolder<br>"
        + "if it does not exist. The file name is Sys followed by the USB<br>"
        + "PID of the remote, an underscore then the firmware version as<br>"
        + "a 6-digit integer.  Further digits after a decimal point give<br>"
        + "the version of the additional language support.</html>" );
    saveFDRAfirmware.addActionListener( this );
    saveFDRAfirmware.setVisible( admin );
    digitalOps.add( saveFDRAfirmware );

    upgradeSourceItem = new JMenuItem( "Set upgrade source file..." );
    upgradeSourceItem.setToolTipText( admin ? 
        "<html>Sets the source for XSight firmware upgrades for the<br>"
        + "current remote, giving a choice between the system-supplied<br>"
        + "upgrades and restoring firmware previously saved with the<br>"
        + "\"Save FDRA firmware\" option.  Includes the ability to<br>"
        + "install optional additional language support.</html>" : 
          "<html>Sets the source for XSight firmware upgrades for the<br>"
          + "current remote, giving in particular the ability to<br>"
          + "install optional additional language support." );
    upgradeSourceItem.addActionListener( this );
    upgradeSourceItem.setVisible( admin );
    digitalOps.add( upgradeSourceItem );
    
    extractSSItem = new JMenuItem( "Extract from simpleset binary" );
    extractSSItem.setToolTipText( "<html>Extracts data for an RDF from the currently loaded<br>"
        + "simpleset .bin file.</html>" );
    extractSSItem.setVisible( false );
    extractSSItem.addActionListener( this );
    menu.add( extractSSItem );
      
    analyzeMAXQprotocols = new JMenuItem( "Analyze MAXQ protocols" );
    analyzeMAXQprotocols.setVisible( admin );
    analyzeMAXQprotocols.addActionListener( this );
    menu.add( analyzeMAXQprotocols );

    menu = new JMenu( "Help" );
    menu.setMnemonic( KeyEvent.VK_H );
    menuBar.add( menu );

    if ( desktop != null )
    {
      readmeItem = new JMenuItem( "Readme", KeyEvent.VK_R );
      readmeItem.addActionListener( this );
      menu.add( readmeItem );

      tutorialItem = new JMenuItem( "Tutorial", KeyEvent.VK_T );
      tutorialItem.addActionListener( this );
      menu.add( tutorialItem );
      
      rmpbReadmeItem = new JMenuItem( "Using RMPB", KeyEvent.VK_U );
      rmpbReadmeItem.addActionListener( this );
      menu.add( rmpbReadmeItem );

      learnedSignalItem = new JMenuItem( "Interpreting Decoded IR Signals", KeyEvent.VK_I );
      learnedSignalItem.addActionListener( this );
      menu.add( learnedSignalItem );

      menu.addSeparator();

      homePageItem = new JMenuItem( "Home Page", KeyEvent.VK_H );
      homePageItem.addActionListener( this );
      menu.add( homePageItem );

      wikiItem = new JMenuItem( "Wiki", KeyEvent.VK_W );
      wikiItem.addActionListener( this );
      menu.add( wikiItem );

      forumItem = new JMenuItem( "Forums", KeyEvent.VK_F );
      forumItem.addActionListener( this );
      menu.add( forumItem );
      
      powerManagementItem = new JMenuItem( "Enhanced Power Management info", KeyEvent.VK_E );
      powerManagementItem.addActionListener( this );
      menu.add( powerManagementItem );

      menu.addSeparator();
    }

    updateItem = new JMenuItem( "Check for updates", KeyEvent.VK_C );
    updateItem.addActionListener( this );
    menu.add( updateItem );

    aboutItem = new JMenuItem( "About...", KeyEvent.VK_A );
    aboutItem.addActionListener( this );
    menu.add( aboutItem );
  }

  private void appendAdvancedOptions( JMenu menu )
  {
    ActionListener listener = new ActionListener()
    {
      public void actionPerformed( ActionEvent e )
      {
        try
        {
          JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
          properties.setProperty( item.getActionCommand(), Boolean.toString( item.isSelected() ) );
          refreshTabbedPanes( item.getActionCommand().equals(  "ShowSegments" ) );
          if ( item.getActionCommand().equals( "NonModalDeviceEditor") )
          {
            setNonModalWarning( Boolean.parseBoolean( properties.getProperty( item.getActionCommand(), "false" ) ), null );
          }
          else if ( item.getActionCommand().equals( "SuppressDeletePrompts") )
          {
            TablePanel.suppressDeletePrompts = item.isSelected();
            RMTablePanel.suppressDeletePrompts = item.isSelected();
          }
        }
        catch ( Exception x )
        {
          x.printStackTrace( System.err );
        }
      }
    };

    // Advanced sub menu
    JMenu advancedSubMenu = new JMenu( "Advanced" );
    advancedSubMenu.setMnemonic( KeyEvent.VK_D );
    menu.addSeparator();
    menu.add( advancedSubMenu );
    JCheckBoxMenuItem item;

    item = new JCheckBoxMenuItem( "Show Segment Editor" );
    item.setActionCommand( "ShowSegments" );
    item.setSelected( Boolean.parseBoolean( properties.getProperty( item.getActionCommand(), "false" ) ) );
    item.addActionListener( listener );
    advancedSubMenu.add( item );
    
    item = new JCheckBoxMenuItem( "Use non-modal Device Editor" );
    item.setActionCommand( "NonModalDeviceEditor" );
    item.setSelected( Boolean.parseBoolean( properties.getProperty( item.getActionCommand(), "false" ) ) );
    item.addActionListener( listener );
    advancedSubMenu.add( item );
    
    item = new JCheckBoxMenuItem( "Same sig = same remote" );
    item.setActionCommand( "SameSigSameRemote" );
    item.setSelected( Boolean.parseBoolean( properties.getProperty( item.getActionCommand(), "false" ) ) );
    item.addActionListener( listener );
    advancedSubMenu.add( item );
    
    advancedSubMenu.addSeparator();
    
    item = new JCheckBoxMenuItem( "Learned Signal Timing Analysis" );
    item.setActionCommand( "LearnedSignalTimingAnalysis" );
    item.setSelected( Boolean.parseBoolean( properties.getProperty( item.getActionCommand(), "false" ) ) );
    item.addActionListener( listener );
    advancedSubMenu.add( item );

    // Remove property no longer used
    properties.remove( "LearnUpgradeConversion" );    
    
    // Suppress Messages sub menu
    JMenu suppressSubMenu = new JMenu( "Suppress Messages" );
    suppressSubMenu.setMnemonic( KeyEvent.VK_S );
    menu.add( suppressSubMenu );

    item = new JCheckBoxMenuItem( "Key Move Detach/Delete" );
    item.setActionCommand( "SuppressKeyMovePrompts" );
    item.setSelected( Boolean.parseBoolean( properties.getProperty( item.getActionCommand(), "false" ) ) );
    item.addActionListener( listener );
    suppressSubMenu.add( item );
    
    item = new JCheckBoxMenuItem( "All table deletes" );
    item.setActionCommand( "SuppressDeletePrompts" );
    Boolean bval = Boolean.parseBoolean( properties.getProperty( item.getActionCommand(), "false" ) );
    item.setSelected( bval );
    TablePanel.suppressDeletePrompts = bval;
    RMTablePanel.suppressDeletePrompts = bval;
    item.addActionListener( listener );
    suppressSubMenu.add( item );
    
    item = new JCheckBoxMenuItem( "Confirmation prompts" );
    item.setActionCommand( "SuppressConfirmPrompts" );
    item.setSelected( Boolean.parseBoolean( properties.getProperty( item.getActionCommand(), "false" ) ) );
    item.addActionListener( listener );
    suppressSubMenu.add( item );
    suppressConfirmPrompts = item;
    
    item = new JCheckBoxMenuItem( "Timing summary info" );
    item.setActionCommand( "SuppressTimingSummaryInfo" );
    item.setSelected( Boolean.parseBoolean( properties.getProperty( item.getActionCommand(), "false" ) ) );
    item.addActionListener( listener );
    suppressSubMenu.add( item );
    suppressTimingSummaryInfo = item;
  }

  private void createToolbar()
  {
    toolBar.add( newAction );
    toolBar.add( openAction );
    toolBar.add( saveAction );
    toolBar.add( saveAsAction );
    toolBar.addSeparator();

    String selectedInterface = properties.getProperty( "Interface" );
    if ( selectedInterface != null && selectedInterface.equals( "JP2BT" ) )
    {
      toolBar.add( bluetoothButton );
      bluetoothButton.setBorder( bluetoothButton.isSelected() ? BorderFactory.createLoweredBevelBorder()
          : BorderFactory.createRaisedBevelBorder());
      if ( btio != null && btio.getBleRemote() != null && btio.getBleRemote().hasFinder )
      {
        toolBar.add( Box.createHorizontalStrut( 5 ) );
        toolBar.add( finderButton );
        finderButton.setBorder( finderButton.isSelected() ? BorderFactory.createLoweredBevelBorder()
            : BorderFactory.createRaisedBevelBorder());
        finderItem.setVisible( true );
      }
      else
      {
        finderItem.setVisible( false );
      }
      bluetoothItem.setVisible( true );
    }
    else
    {
      bluetoothItem.setVisible( false );
      finderItem.setVisible( false );
    }
    toolBar.add( downloadAction );
    toolBar.add( uploadAction );
    toolBar.addSeparator();
    toolBar.add( openRdfAction );
    toolBar.add( codesAction );
    if ( remoteConfig != null && remoteConfig.allowHighlighting() )
    {
      toolBar.add( highlightAction );
//      highlightAction.setEnabled( true );
    }
    if ( selectedInterface != null && selectedInterface.equals( "JP2BT" ) )
    {
      downloadAction.setEnabled( bluetoothButton.isSelected() );
      uploadAction.setEnabled( uploadable && allowUpload() );
    }
    else
    {
      disconnectBLE();
      downloadAction.setEnabled( !interfaces.isEmpty() );
      uploadAction.setEnabled( uploadable );
      bleStatus.setVisible( false );
    }
  }
  
  public void recreateToolbar()
  {
    highlightItem.setEnabled( remoteConfig != null && !remoteConfig.getRemote().isSSD() );
    Container mainPanel = getContentPane();
    mainPanel.remove( toolBar );
    toolBar = new JToolBar();
    toolBar.setFloatable( false );
    createToolbar();
    mainPanel.add( toolBar, BorderLayout.PAGE_START );
    mainPanel.validate();
  }
  
  /**
   * allowUpload() returns true if the remote allows uploads with the current
   * interface.  This is normally so, but the JP2BT Bluetooth interface in some
   * remotes supports downloads but needs an extender to fix the DATAWRITE code
   * in order to support uploads.
   */
  public boolean allowUpload()
  {
    String temp = properties.getProperty( "Interface" );
    return temp != null && temp.equals( "JP2BT" ) ? 
        btio != null && btio.getBleRemote() != null && btio.getBleRemote().supportsUpload
          && btio.isConnected() : true; 
  }

  /**
   * Gets the fileChooser attribute of the RemoteMaster object.
   * 
   * @return The fileChooser value
   */
  public RMFileChooser getFileChooser()
  {
    RMFileChooser chooser = new RMFileChooser( dir );
    EndingFileFilter irFilter = new EndingFileFilter( "All supported files", admin ? allAdminEndings : allEndings );
    chooser.addChoosableFileFilter( irFilter );
    chooser.addChoosableFileFilter( new EndingFileFilter( "RMIR files (*.rmir)", rmirEndings ) );
    chooser.addChoosableFileFilter( new EndingFileFilter( "IR files (*.ir)", irEndings ) );
    chooser.addChoosableFileFilter( new EndingFileFilter( "RM Device Upgrades (*.rmdu)", rmduEndings ) );
    chooser.addChoosableFileFilter( new EndingFileFilter( "KM Device Upgrades (*.txt)", txtEndings ) );
    chooser.addChoosableFileFilter( new EndingFileFilter( "Protocol files (*.rmpb)", rmpbEndings ) );
    chooser.addChoosableFileFilter( new EndingFileFilter( "Simpleset files (*.bin)", binEndings ) );
    chooser.addChoosableFileFilter( new EndingFileFilter( "Sling Learned Signals (*.xml)", slingEndings ) );
    chooser.setFileFilter( irFilter );

    return chooser;
  }

  public RMFileChooser getExtenderFileChooser()
  {
    RMFileChooser chooser = new RMFileChooser( mergeDir == null ? dir : mergeDir );
    EndingFileFilter irFilter = new EndingFileFilter( "Extender merge files (*.hex)", extenderEndings );
    chooser.setDialogTitle( "Select the file to merge" );
    chooser.addChoosableFileFilter( irFilter );
    chooser.addChoosableFileFilter( new EndingFileFilter( "Other merge files (*.ir, *.txt)", otherMergeEndings ) );
    chooser.addChoosableFileFilter( new EndingFileFilter( "All merge files", allMergeEndings ) );
    chooser.setFileFilter( irFilter );

    return chooser;
  }
  
  public RMFileChooser getWavFileChooser( WavOp wavOp )
  {
    RMFileChooser chooser = new RMFileChooser( dir );
    EndingFileFilter wavFilter = new EndingFileFilter( "Sound files (*.wav)", wavEndings );
    chooser.setDialogTitle( "Select the sound file to " + ( wavOp == WavOp.PLAY ? "play" : "import" ) );
    chooser.addChoosableFileFilter( wavFilter );
    chooser.setFileFilter( wavFilter );

    return chooser;
  }

  public RMFileChooser getFileSaveChooser( boolean validConfiguration )
  {
    RMFileChooser chooser = new RMFileChooser( dir );
    chooser.setAcceptAllFileFilterUsed( false );
    EndingFileFilter rmirFilter = new EndingFileFilter( "RM Remote Image (*.rmir)", rmirEndings );
    EndingFileFilter binFilter = new EndingFileFilter( "Simpleset file (*.bin)", binEndings );
    EndingFileFilter useFilter = rmirFilter;
    chooser.addChoosableFileFilter( rmirFilter );
    if ( validConfiguration )
    {
      chooser.addChoosableFileFilter( new EndingFileFilter( "IR file (*.ir)", irEndings ) );
    }
    if ( binLoaded() != null )
    {
      chooser.addChoosableFileFilter( binFilter );
      if ( file != null && file.getName().toLowerCase().endsWith( binEndings[ 0 ] ) )
      {
        useFilter = binFilter;
      }
    }
    chooser.setFileFilter( useFilter );
    return chooser;
  }
  
  public RMFileChooser getFileSaveChooser( EndingFileFilter filter )
  {
    RMFileChooser chooser = new RMFileChooser( dir );
    chooser.setAcceptAllFileFilterUsed( false );
 //   EndingFileFilter wavFilter = new EndingFileFilter( "Sound files (*.wav)", wavEndings );
    chooser.addChoosableFileFilter( filter );
    chooser.setFileFilter( filter );
    return chooser;
  }

  private File getRDFPathChoice()
  {
    File result = null;
    File dir = properties.getFileProperty( "RDFPath" );
    RMDirectoryChooser chooser = new RMDirectoryChooser( dir, ".rdf", "RDF" );
    ChoiceArea area = new ChoiceArea( chooser );
    chooser.setAccessory( area );
    chooser.setDialogTitle( "Select RDF Directory" );
    if ( chooser.showDialog( this, "OK" ) == RMDirectoryChooser.APPROVE_OPTION )
    {
      result = chooser.getSelectedFile();
      if ( result.equals( dir ) )
      {
        result = null; // Not changed
      }
    }
    chooser.removePropertyChangeListener( area );
    return result;
  }

  private File getMapPathChoice()
  {
    File result = null;
    File dir = properties.getFileProperty( "ImagePath" );
    RMDirectoryChooser chooser = new RMDirectoryChooser( dir, ".map", "Map and Image" );
    ChoiceArea area = new ChoiceArea( chooser );
    chooser.setAccessory( area );
    chooser.setDialogTitle( "Select Map and Image Directory" );
    if ( chooser.showDialog( this, "OK" ) == RMDirectoryChooser.APPROVE_OPTION )
    {
      result = chooser.getSelectedFile();
      if ( result.equals( dir ) )
      {
        result = null; // Not changed
      }
    }
    chooser.removePropertyChangeListener( area );
    return result;
  }
  
  private File getAddonPathChoice()
  {
    File result = null;
    File defaultDir = new File( workDir, "AddOns" );
    File dir = properties.getFileProperty( "AddonPath", defaultDir );
    RMDirectoryChooser chooser = new RMDirectoryChooser( dir, null, null );
    chooser.setDialogTitle( "Select AddOns Directory" );
    if ( chooser.showDialog( this, "OK" ) == RMDirectoryChooser.APPROVE_OPTION )
    {
      result = chooser.getSelectedFile();
      if ( result.equals( dir ) )
      {
        result = null; // Not changed
      }
    }
    if ( result != null && result.equals( defaultDir  ) )
    {
      properties.remove( "AddonPath" );
      result = null;
    }
    else if ( result != null )
    {
      properties.setProperty( "AddonPath", result );
    }
    return result;
  }
  
  public JPS binLoaded()
  {
    for ( IO io : interfaces )
    {
      if ( io.getInterfaceName().equals( "JPS" ) )
      {
        JPS jps = ( JPS )io;
        if ( jps.isOpen() )
        {
          return jps;
        }
        else
        {
          return null;
        }
      }
    }
    return null;
  }

  /**
   * Description of the Method.
   * 
   * @return Description of the Return Value
   * @throws Exception
   *           the exception
   * @exception Exception
   *              Description of the Exception
   */
  public void openFile() throws Exception
  {
    openFile( null );
  }

  public void openFile( File file ) throws Exception
  {
    openFile( file, null );
  }
  
  public void openFile( File file, WavOp wavOp ) throws Exception
  {
    if ( ( wavOp == null || wavOp != WavOp.PLAY ) && !promptToSave() )
    {
      return;
    }
    while ( file == null )
    {
      RMFileChooser chooser = wavOp == null ? getFileChooser() : getWavFileChooser( wavOp );
      int returnVal = chooser.showOpenDialog( this );
      if ( returnVal == RMFileChooser.APPROVE_OPTION )
      {
        file = chooser.getSelectedFile();

        if ( !file.exists() )
        {
          JOptionPane.showMessageDialog( this, file.getName() + " doesn't exist.", "File doesn't exist.",
              JOptionPane.ERROR_MESSAGE );
        }
        else if ( file.isDirectory() )
        {
          JOptionPane.showMessageDialog( this, file.getName() + " is a directory.", "File doesn't exist.",
              JOptionPane.ERROR_MESSAGE );
        }
      }
      else
      {
        return;
      }
    }

    System.err.println();
    System.err.println( "Opening " + file.getCanonicalPath() + ", last modified "
        + DateFormat.getInstance().format( new Date( file.lastModified() ) ) );

    String ext = file.getName().toLowerCase();
    int dot = ext.lastIndexOf( '.' );
    ext = ext.substring( dot );

    dir = file.getParentFile();
    properties.setProperty( "IRPath", dir );

    if ( ext.equals( ".rmir" ) || ext.equals( ".bin" ) || ext.equals( ".ir" ) )
    {
      updateRecentFiles( file );
    }

    if ( ext.equals( ".rmdu" ) || ext.equals( ".txt" ) )
    {
      // Opening KM as a new instance of KeyMapMaster makes it share the same
      // ProtocolManager as RM, which results in crosstalk when one edits the protocol
      // used by the other.  Replaced now by opening KM as a new application.
      
//    KeyMapMaster km = new KeyMapMaster( properties );
//    km.loadUpgrade( file );
      
      runKM( file.getCanonicalPath() );
      return;
    }
    
    if ( ext.equals( ".rmpb" ) )
    {
      runPB( file.getCanonicalPath() );
      return;
    }

    if ( ext.equals( ".xml" ) )
    {
      List< Remote > remotes = RemoteManager.getRemoteManager().findRemoteBySignature( "XMLLEARN" );
      if ( remotes.isEmpty() )
      {
        JOptionPane.showMessageDialog( RemoteMaster.getFrame(),
            "The RDF for XML Slingbox Learns was not found.  Please place it in the RDF folder and try again.",
            "Missing RDF File", JOptionPane.ERROR_MESSAGE );
        return;
      }
      Remote remote = remotes.get( 0 );
      remote.load();
      clearAllInterfaces();
      remoteConfig = new RemoteConfiguration( remote, this );
      recreateToolbar();
      remoteConfig.initializeSetup( 0 );
      remoteConfig.updateImage();
      remoteConfig.setDateIndicator();
      remoteConfig.setSavedData();
      SlingLearnParser.parse( file, remoteConfig );
      remoteConfig.updateImage();
      update();
      saveAction.setEnabled( false );
      saveAsAction.setEnabled( true );
      uploadable = !interfaces.isEmpty();
      uploadAction.setEnabled( uploadable );
      openRdfAction.setEnabled( true );
      installExtenderItem.setEnabled( true );
      cleanUpperMemoryItem.setEnabled( true );
      initializeTo00Item.setEnabled( !interfaces.isEmpty() );
      initializeToFFItem.setEnabled( !interfaces.isEmpty() );
      setBaselineItem.setEnabled( true );
      return;
    }
    
    if ( ext.equals( ".bin" ) )
    {
      ProtocolManager.getProtocolManager().reset();
      System.err.println( "Starting Simpleset load" );
      setInterfaceState( "LOADING SIMPLESET..." );
      this.file = file;
      ( new DownloadTask( file ) ).execute();
      return;
    }
    
    if ( wavOp != null && ext.equals( ".wav" ) )
    {
      if ( wavOp == WavOp.PLAY )
      {
        wavPlayer = new RMWavPlayer();
        wavPlayer.open( file );
        int duration = wavPlayer.getDuration();  // in tenths of a second
        String durStr = duration >= 600 ? "" + duration / 600 + "min " : "";
        int secStr = duration % 600;
        durStr += secStr / 10 + "." + secStr % 10 + "sec";
        String title = "Modem upload";
        String message = 
            "The duration of this audio file for modem upload is " + durStr + ".  Before\n"
                + "continuing, you need to set your remote ready to receive the upgrade.\n"
                + "See the guide \"Modem Upgrade Procedure\" in the Wiki of the JP1 website\n"
                + "for information on how to do this.\n\n"
                + "Do you wish to continue?";
        if ( JOptionPane.showConfirmDialog( this, message, title, JOptionPane.YES_NO_OPTION, 
            JOptionPane.INFORMATION_MESSAGE ) != JOptionPane.YES_OPTION )
          return;
        uploadWavItem.setEnabled( false );
        cancelWavUploadItem.setEnabled( true );
        TimingTask timer = new TimingTask( "PLAYING WAV (" + durStr + "):", duration );
        wavPlayer.setTimer( timer );
        wavPlayer.play();
      }
      else
        ( new LoadTask( file, wavOp ) ).execute();     
      return;
    }
    
    if ( admin && ext.equals( ".ctl" ) )
    {
      String title = "Control file updater";
      String message = "Enter the hexadecimal offset to be applied to line numbers:";
      String offsetStr = JOptionPane.showInputDialog( this, message, title, JOptionPane.QUESTION_MESSAGE );
      int offset = Integer.parseInt( offsetStr, 16 );
      String outname = file.getName().substring( 0, dot ) + ".out";
      File outFile = new File(dir, outname);
      if ( outFile.exists() )
        outFile.delete();
      outFile.createNewFile();
      BufferedWriter out = new BufferedWriter( new FileWriter(outFile) );     
      BufferedReader in = new BufferedReader( new FileReader( file ) );
      String line = in.readLine();
      while ( line != null )
      {
        if ( line.length() > 5 )
        {
          try
          {
            String num = line.substring( 2, 6 );
            int val = Integer.parseInt( num, 16 );
            val += offset;
            line = line.substring( 0, 2 ) + String.format( "%04x", val ) + line.substring( 6 );
          }
          catch ( Exception ex ){};
        }
//        if ( !line.startsWith( "b" ) && !line.startsWith( "!" ) && !line.startsWith( "#" ) )
//        if ( line.startsWith( "b" ) )
        out.write( line + System.lineSeparator() );
        line = in.readLine();
      }
      out.flush();
      out.close();
      in.close();
      return;
    }

    // ext.equals( ".rmir" ) || ext.equals( ".ir" )
    ProtocolManager.getProtocolManager().reset();
    saveAction.setEnabled( ext.equals( ".rmir" ) );
    saveAsAction.setEnabled( true );
    openRdfAction.setEnabled( true );
    installExtenderItem.setEnabled( true );
    cleanUpperMemoryItem.setEnabled( true );
    initializeTo00Item.setEnabled( !interfaces.isEmpty() );
    initializeToFFItem.setEnabled( !interfaces.isEmpty() );
    setBaselineItem.setEnabled( true );
    uploadable = !interfaces.isEmpty();
    uploadAction.setEnabled( uploadable && allowUpload() );
    resetSegmentPanel();
    setInterfaceState( "LOADING..." );
    ( new LoadTask( file ) ).execute();
    return;
  }
   
  private void resetConfig( Remote remote, RMWavConverter converter )
  {
    if ( remote != null )
    {
      ProtocolManager.getProtocolManager().reset();
      clearAllInterfaces();
      remoteConfig = new RemoteConfiguration( remote, RemoteMaster.this );
      recreateToolbar();
      remoteConfig.initializeSetup( 0 );
      remoteConfig.updateImage();
    }
    if ( converter != null )
    {
      converter.mergeData( remoteConfig.getData() );
      try
      {
        remoteConfig.importIR( null, true );
      }
      catch ( IOException e )
      {
        System.err.println( "Unable to parse imported data" );
      }
      remoteConfig.updateImage();
    }
    
    remoteConfig.setDateIndicator();
    remoteConfig.setSavedData();
    update();
    saveAction.setEnabled( false );
    saveAsAction.setEnabled( true );
    openRdfAction.setEnabled( true );
    installExtenderItem.setEnabled( true );
    cleanUpperMemoryItem.setEnabled( true );
    initializeTo00Item.setEnabled( !interfaces.isEmpty() );
    initializeToFFItem.setEnabled( !interfaces.isEmpty() );
    uploadable = !interfaces.isEmpty();
    uploadAction.setEnabled( uploadable && allowUpload() );
  }
  

  private void installExtender() throws Exception
  {
    if ( !promptToSave() )
    {
      return;
    }
    String version = ExtInstall.class.getPackage().getImplementationVersion();
    System.err.print( "Starting Java ExtInstall" );
    if ( version != null )
    {
      System.err.println( ", version " + version );
    }
    else
    {
      System.err.println();
    }

    File file = null;
    while ( file == null )
    {
      RMFileChooser chooser = getExtenderFileChooser();
      int returnVal = chooser.showOpenDialog( this );
      if ( returnVal == RMFileChooser.APPROVE_OPTION )
      {
        file = chooser.getSelectedFile();

        if ( !file.exists() )
        {
          JOptionPane.showMessageDialog( this, file.getName() + " doesn't exist.", "File doesn't exist.",
              JOptionPane.ERROR_MESSAGE );
        }
        else if ( file.isDirectory() )
        {
          JOptionPane.showMessageDialog( this, file.getName() + " is a directory.", "File doesn't exist.",
              JOptionPane.ERROR_MESSAGE );
        }
      }
      else
      {
        System.err.println( "ExtInstall cancelled by user." );
        return;
      }
    }

    System.err.println( "Merge file is " + file.getCanonicalPath() + ", last modified "
        + DateFormat.getInstance().format( new Date( file.lastModified() ) ) );

    String interfaceName = properties.getProperty( "Interface" );
    if ( interfaceName != null && interfaceName.equals( "JP2BT" ) )
    {
      if ( btio != null )
      {
        ( new InstallTask( file, btio ) ).execute();
        return;

        //      BTExtInstall installer = new BTExtInstall( file, btio );
        //      installer.install();
        //      return;
      }
      else 
      {
        String message = "The JP2BT interface is not connected to a remote.";
        JOptionPane.showMessageDialog( this, message, "Extender Install", JOptionPane.ERROR_MESSAGE );
        return;
      }
    }

    mergeDir = file.getParentFile();
    String[] oldDevBtnNotes = remoteConfig.getDeviceButtonNotes();
    List< DeviceUpgrade > oldDevUpgrades = remoteConfig.getDeviceUpgrades();
    List< ProtocolUpgrade > oldProtUpgrades = remoteConfig.getProtocolUpgrades();

    RMExtInstall installer = new RMExtInstall( file.getAbsolutePath(), remoteConfig );
    installer.install();
    if ( binLoaded() != null || RMExtInstall.remoteConfig == null )
    {
      return;
    }
    if ( remoteConfig != RMExtInstall.remoteConfig )
    {
      remoteConfig = RMExtInstall.remoteConfig;
      recreateToolbar();
      String[] newDevBtnNotes = remoteConfig.getDeviceButtonNotes();
      Remote newRemote = remoteConfig.getRemote();
      List< DeviceUpgrade > newDevUpgrades = remoteConfig.getDeviceUpgrades();
      List< ProtocolUpgrade > newProtUpgrades = remoteConfig.getProtocolUpgrades();
      // Copy the old device button notes, as the installer deletes them.
      for ( int i = 0; i < Math.min( oldDevBtnNotes.length, newDevBtnNotes.length ); i++ )
      {
        newDevBtnNotes[ i ] = oldDevBtnNotes[ i ];
      }

      System.err.println( "Restoring .rmir data lost in conversion to .ir format." );

      int devCount = 0;
      int protCount = 0;
      int index = installer.isExtenderMerge() ? installer.getDevUpgradeCodes().size() : 0;
      for ( DeviceUpgrade duOld : oldDevUpgrades )
      {
        int codeOld = duOld.getHexSetupCodeValue();
        if ( installer.getDevUpgradeCodes().contains( Integer.valueOf( codeOld ) ) )
        {
          continue;
        }
        // Upgrade retained in merge, so restore it. The order is preserved by the merge
        // process but those to be restored may not be consecutive.
        boolean found = false;
        for ( ; index < newDevUpgrades.size(); index++ )
        {
          DeviceUpgrade duNew = newDevUpgrades.get( index );
          if ( duNew.getHexSetupCodeValue() == codeOld )
          {
            duOld.protocol = duNew.protocol;
            duOld.setNewRemote( newRemote );
            newDevUpgrades.set( index, duOld );
            devCount++ ;
            found = true;
            break;
          }
        }
        if ( !found )
        {
          System.err.println( "Error restoring device upgrades: failed at setup code = " + codeOld + "." );
        }
      }
      System.err.println( "Restored " + devCount + " device upgrades." );

      index = installer.isExtenderMerge() ? installer.getProtUpgradeIDs().size() : 0;
      for ( ProtocolUpgrade puOld : oldProtUpgrades )
      {
        int pidOld = puOld.getPid();
        if ( installer.getProtUpgradeIDs().contains( Integer.valueOf( pidOld ) ) )
        {
          continue;
        }
        boolean found = false;
        for ( ; index < newProtUpgrades.size(); index++ )
        {
          ProtocolUpgrade puNew = newProtUpgrades.get( index );
          if ( puNew.getPid() == pidOld )
          {
            // Only restore if not used by device upgrade. Restoring a used one would remove
            // the isUsed mark.
            if ( !puNew.isUsed() )
            {
              newProtUpgrades.set( index, puOld );
              protCount++ ;
            }
            found = true;
            break;
          }
        }
        if ( !found )
        {
          System.err.println( "Error restoring protocol upgrades: failed at PID = " + pidOld + "." );
        }
      }
      System.err.println( "Restored " + protCount + " protocol upgrades." );

      for ( Iterator< ProtocolUpgrade > it = newProtUpgrades.iterator(); it.hasNext(); )
      {
        ProtocolUpgrade pu = it.next();
        if ( pu.isUsed() )
        {
          it.remove();
        }
        else if ( installer.getProtUpgradeIDs().contains( Integer.valueOf( pu.getPid() ) ) )
        {
          // Add to ProtocolManager as manual protocol. It is only those in getProtUpgradeIDs()
          // that were removed before the merge, so it is only those that need to be put back and
          // the import process will have handled the ones that are used by device upgrades.
          pu.setManualProtocol( newRemote );
        }
      }

      remoteConfig.setDeviceUpgrades( newDevUpgrades );
      remoteConfig.setProtocolUpgrades( newProtUpgrades );
    }
    remoteConfig.updateImage();
    update();
    saveAction.setEnabled( false );
    System.err.println( "ExtInstall merge completed." );
  }

  /**
   * Description of the Method.
   * 
   * @param file
   *          the file
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   * @exception IOException
   *              Description of the Exception
   */
  private void updateRecentFiles( File file ) throws IOException
  {
    JMenuItem item = null;
    String path = file.getCanonicalPath();
    for ( int i = 0; i < recentFiles.getItemCount(); ++i )
    {
      File temp = new File( recentFiles.getItem( i ).getText() );

      if ( temp.getCanonicalPath().equals( path ) )
      {
        item = recentFiles.getItem( i );
        recentFiles.remove( i );
        break;
      }
    }
    if ( item == null )
    {
      item = new JMenuItem( path );
      item.setActionCommand( path );
      item.addActionListener( this );
    }
    recentFiles.insert( item, 0 );
    while ( recentFiles.getItemCount() > 10 )
    {
      recentFiles.remove( 10 );
    }
    recentFiles.setEnabled( true );
    dir = file.getParentFile();
    properties.setProperty( "IRPath", dir );
  }

  public void save( File file, boolean forSaveAs ) throws IOException
  {
    if ( file == null )
    {
      saveAs();
      return;
    }
    
    if ( file.exists() && !file.canWrite() )
    {
      String title = "File Save Error";
      String message = "The file\n" + file.getCanonicalPath()
          + "\nis read-only.  Please use \"Save As...\" to save to a different file.";
      JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
      return;
    }
    
    String ext = file.getName().toLowerCase();
    int dot = ext.lastIndexOf( '.' );
    ext = ext.substring( dot );
    if ( ext.equals( ".bin" ) )
    {
      boolean validConfiguration = updateUsage();
      if ( !validConfiguration )
      {
        String title = "Invalid Configuration";
        String message = "This configuration is not valid.  It cannot be saved as\n"
            + "copying it to the remote could cause the remote to crash.";
        JOptionPane.showMessageDialog( RemoteMaster.this, message, title, JOptionPane.WARNING_MESSAGE );
        return;
      }

      Remote remote = remoteConfig.getRemote();
      if ( !allowSave( remote.getSetupValidation() ) )
      {
        return;
      }
      remoteConfig.saveAltPIDs();
      System.err.println( "Saving Simpleset" );
      setInterfaceState( "SAVING SIMPLESET..." );
      changed = false;
      ( new UploadTask( file, RemoteMaster.this.useSavedData() ? remoteConfig.getSavedData() : remoteConfig.getData(), false, forSaveAs ) ).execute();
    }
    else
    {
      boolean validConfiguration = updateUsage();
      if ( !allowSave( Remote.SetupValidation.WARN ) )
      {
        return;
      }
      if ( !validConfiguration )
      {
        String title = "Invalid Configuration";
        String message = "This configuration is not valid, but it can be saved and then\n"
            + "re-loaded to give again this same invalid configuration.\n\n" + "Do you wish to continue?";
        if ( NegativeDefaultButtonJOptionPane.showConfirmDialog( RemoteMaster.this, message, title, JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE ) == JOptionPane.NO_OPTION )
        {
          return;
        }
      }
      setInterfaceState( "SAVING..." );
      ( new SaveTask( file, forSaveAs ? Use.SAVEAS : Use.SAVING ) ).execute();
    }
  }

  public void saveAs() throws IOException
  {
    saveAs( null, null, 0 );
  }
  
  public void saveAs( WavOp wavOp, int wavIndex ) throws IOException
  {
    saveAs( null, wavOp, wavIndex );
  }

  private void saveAs( File sourceFile, WavOp wavOp, int wavIndex ) throws IOException
  {   
    boolean validConfiguration = sourceFile == null ? updateUsage() : true;
    if ( sourceFile == null && !validConfiguration )
    {
      String title = "Invalid Configuration";
      String message = "This configuration is not valid.  It can be saved as a .rmir file\n"
          + "which can be re-loaded to give again this same invalid configuration,\n"
          + "but it cannot be saved as a .ir or .wav file as it could cause the\n"
          + "remote to crash if it were uploaded to it by another application.";
      JOptionPane.showMessageDialog( RemoteMaster.this, message, title, JOptionPane.WARNING_MESSAGE );
      if ( wavOp != null )
        return;
    }
    
    EndingFileFilter filter = wavOp != null ? new EndingFileFilter( "Sound files (*.wav)", wavEndings )
      : sourceFile != null ?  new EndingFileFilter( "Summary files (*.html)", summaryEndings ) : null;    
    RMFileChooser chooser = filter == null ? getFileSaveChooser( validConfiguration ) : getFileSaveChooser( filter );
    File oldFile = file;
    if ( sourceFile == null && wavOp == null && oldFile != null )
    {
      String name = oldFile.getName();
      if ( name.toLowerCase().endsWith( ".ir" ) || name.toLowerCase().endsWith( ".txt" ) )
      {
        int dot = name.lastIndexOf( '.' );
        name = name.substring( 0, dot ) + ".rmir";
        oldFile = new File( name );
      }
      chooser.setSelectedFile( oldFile );
    }
    
    int returnVal = -1;
    try
    {
      returnVal = chooser.showSaveDialog( this );
    }
    catch ( HeadlessException e ){}
    if ( returnVal == RMFileChooser.APPROVE_OPTION )
    {
      String ending = ( ( EndingFileFilter )chooser.getFileFilter() ).getEndings()[ 0 ];
      String name = chooser.getSelectedFile().getAbsolutePath();
      if ( !name.toLowerCase().endsWith( ending ) )
      {
        if ( name.toLowerCase().endsWith( ".rmir" ) || name.toLowerCase().endsWith( ".bin" ) )
        {
          int dot = name.lastIndexOf( '.' );
          name = name.substring( 0, dot );
        }
        name = name + ending;
      }
      File newFile = new File( name );
      int rc = JOptionPane.YES_OPTION;
      if ( newFile.exists() )
      {
        rc = JOptionPane.showConfirmDialog( this, newFile.getName() + " already exists.  Do you want to replace it?",
            "Replace existing file?", JOptionPane.YES_NO_OPTION );
      }

      if ( rc != JOptionPane.YES_OPTION )
      {
        return;
      }

      if ( newFile.exists() && !newFile.canWrite() )
      {
        String title = "File Save Error";
        String message = "The file\n" + newFile.getCanonicalPath()
            + "\nis read-only.  Please save to a different file.";
        JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
        return;
      }
      
      dir = newFile.getParentFile();
      properties.setProperty( "IRPath", dir );

      if ( ending.equals( irEndings[ 0 ] ) )
      {
        remoteConfig.saveAltPIDs();
        setInterfaceState( "EXPORTING..." );
        ( new SaveTask( newFile, Use.EXPORT ) ).execute();
      }
      else if ( ending.equals( binEndings[ 0 ] ) )
      {
        save( newFile, true );
      }
      else if ( ending.equals( wavEndings[ 0 ] ) )
      {
        Remote remote = remoteConfig.getRemote();
        AddressRange settingsAddress = new AddressRange( 0, remote.getAdvancedCodeAddress().getStart() - 1 );
        settingsAddress.setFreeStart( settingsAddress.getEnd() + 1 );
        AddressRange entireAddress = new AddressRange( 0, remote.getEepromSize() - 1 );
        entireAddress.setFreeStart( entireAddress.getEnd() + 1 );
        AddressRange[] ranges = { entireAddress, settingsAddress, remote.getAdvancedCodeAddress(), 
            remote.getTimedMacroAddress(), remote.getUpgradeAddress(), remote.getLearnedAddress() };
        ( new SaveTask( newFile, Use.EXPORT, ranges[ wavIndex] ) ).execute();
      }
      else if ( ending.equals( summaryEndings[ 0 ] ) )
      {
        if ( sourceFile != null )
          Files.copy( sourceFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
      }
      else
      {
        setInterfaceState( "SAVING..." );
        ( new SaveTask( newFile, Use.SAVEAS ) ).execute();
      }
//      uploadAction.setEnabled( !interfaces.isEmpty() );
    }
  }
  
  private class LoadTask extends SwingWorker< RemoteConfiguration, Void > implements ProgressUpdater
  {
    private File loadFile;
    private WavOp wavOp = null;
    private RMWavConverter converter = null;
    
    public LoadTask( File file )
    {
      loadFile = file;
    }
    
    public LoadTask( File file, WavOp wavOp )
    {
      loadFile = file;
      this.wavOp = wavOp;
      if ( wavOp != null )
      {
        converter = new RMWavConverter( wavOp == WavOp.MERGE ? remoteConfig : null );
        converter.setProgressUpdater( this );
      }
    }
    
    @Override
    protected RemoteConfiguration doInBackground() throws Exception
    {
      if ( wavOp == null )
      {
        return new RemoteConfiguration( loadFile, RemoteMaster.this );
      }
      else
      {
        converter.importWav( loadFile );
        String importedSig = converter.getImportedSignature();
        String currentSig = remoteConfig == null ? null
            : remoteConfig.getRemote().getSignature();
        boolean mismatch = importedSig != null && currentSig != null
            && !importedSig.substring( 0, 4 ).equals( currentSig.substring( 0, 4 ) );
        if ( wavOp == WavOp.MERGE && mismatch )
        {
          String title = "Inconsistent merge";
          String message = 
                 "The imported data includes a signature that differs from that of\n"
              +  "the current setup and so cannot be merged into it.  Do you want\n"
              +  "instead to create a new image from the imported data?";
          if ( JOptionPane.showConfirmDialog( RemoteMaster.this, message, title, JOptionPane.YES_NO_OPTION, 
              JOptionPane.QUESTION_MESSAGE ) != JOptionPane.YES_OPTION )
            return remoteConfig;
        }
        resetConfig( wavOp == WavOp.MERGE && !mismatch ? null : converter.getRemote(), converter );
        return remoteConfig;
      }
    }
    
    @Override
    public void done()
    {
      try
      {
        RemoteConfiguration rc = get();
        if ( rc != null )
        {
          remoteConfig = rc;
        }
      } 
      catch ( InterruptedException ignore ) {}
      catch ( java.util.concurrent.ExecutionException e ) 
      {
        String why = null;
        Throwable cause = e.getCause();
        if ( cause != null ) 
        {
          why = cause.getMessage();
        } 
        else 
        {
          why = e.getMessage();
        }
        System.err.println( "Error setting new RemoteConfiguration: " + why );
        setInterfaceState( null );
        JOptionPane.showMessageDialog( RemoteMaster.this, "Error loading file " + loadFile.getName(), "Task error", JOptionPane.ERROR_MESSAGE );
        e.printStackTrace();
      }
      recreateToolbar();
      update();
      changed = remoteConfig != null ? !Hex.equals( remoteConfig.getSavedData(), remoteConfig.getData() ) : false;
      setTitleFile( loadFile );
      setInterfaceState( null );
      file = loadFile;
    }
    
    @Override
    public void updateProgress( int value )
    {
      if ( value < 0 )
      {
        setInterfaceState( "DOWNLOADING..." );
      }
      else
      {
        String name = converter != null ? converter.getProgressName() : null;
        setInterfaceState( name != null ? name : "PREPARING:", value );
      }
    }
  }
  
  private class WriteTask extends SwingWorker< Void, Void >
  {
    private String text = null;
    protected boolean ok = true;
    
    public WriteTask()
    {
      this.text = null;
    }
    
    public WriteTask( String text )
    {
      this.text = text;
    }    
    
    @Override
    protected Void doInBackground() throws Exception
    {
      setInterfaceState( text );
      return null;
    }
    
    @Override
    public void done()
    {
      ok = true;
      try
      {
        get();
      } 
      catch ( InterruptedException ignore ) {}
      catch ( java.util.concurrent.ExecutionException e ) 
      {
        ok = false;
        String why = null;
        Throwable cause = e.getCause();
        if ( cause != null ) 
        {
          why = cause.getMessage();
        } 
        else 
        {
          why = e.getMessage();
        }
        System.err.println( "Error in write task: " + why );
        e.printStackTrace();
      }
    }
    
    public void resetInterfaceState()
    {
      if ( exitPrompt && interfaceText != null )
      {
        ( new WriteTask( interfaceText.substring( 0, interfaceText.length() - 3 ) + " AND EXIT" ) ).execute();
      }
    }
  }
  
  public class TimingTask extends WriteTask implements ProgressUpdater
  {
    String name = null; // Name to display on progress bar
    int duration = 0;   // Duration of task in tenths of a second
    boolean cancelled = false;
    
    public TimingTask( String name, int duration )
    {
      this.name = name;
      this.duration = duration + 1;
    }
    
    @Override
    protected Void doInBackground() throws Exception
    {
      long start = Calendar.getInstance().getTimeInMillis();
      long current = start;
      int progress = 0;
      do
      {
        updateProgress( progress );
        Thread.sleep( 200 );
        current = Calendar.getInstance().getTimeInMillis();
        progress = Math.min( ( int )( ( current - start ) / duration ), 100 );
      }
      while ( !cancelled );
      return null;
    }
    
    @Override
    public void done()
    {
      super.done();
      setInterfaceState( null );
      uploadWavItem.setEnabled( true );
      cancelWavUploadItem.setEnabled( false );
    }
    
    public void setCancelled( boolean cancelled )
    {
      this.cancelled = cancelled;
    }

    @Override
    public void updateProgress( int value )
    {     
      setInterfaceState( name, value );
    }
  }
  
  private class InstallTask extends WriteTask implements ProgressUpdater
  {
    private File file = null;
    private JP2BT btio = null;
    
    public InstallTask( File file, JP2BT btio )
    {
      this.file = file;
      this.btio = btio;
    }
    
    @Override
    protected Void doInBackground() throws Exception
    {
      BTExtInstall installer = new BTExtInstall( file, btio, this );
      installer.install();
//      String title = "Bluetooth Extender Install";
//      String message = "Installation succeeded.";
//      int error = installer.install();
//      if ( error != 0 );
//        message = "Installation failed with error code " + error;
//      JOptionPane.showMessageDialog( null, message, title, JOptionPane.PLAIN_MESSAGE );
      return null;
    }
    
    @Override
    public void done()
    {
      super.done();
      setInterfaceState( null );
      if ( !ok )
      {
        return;
      }
    }
    
    @Override
    public void updateProgress( int value )
    {     
      setInterfaceState( "INSTALLING:", value );
    }
  }
  
  private class ConnectTask extends WriteTask implements ProgressUpdater
  {
    private BLERemote bleRemote = null;
    private Use use = null;
    
    public ConnectTask( BLERemote bleRemote, Use use )
    {
      this.bleRemote = bleRemote;
      this.use = use;
    }
    
    @Override
    protected Void doInBackground() throws Exception
    {
      if ( btio == null )
      {
        if ( use == Use.CONNECT )
          setInterfaceState( "CONNECTING:", 5 );
        btio = ( JP2BT )getOpenInterface( null, Use.CONNECT, this );
        btio.setOwner( RemoteMaster.this );
      }
      if ( btio == null )
      {
        String message = "Failed to open BLE interface on port " + properties.getProperty( "Port" );
        JOptionPane.showMessageDialog( null, message, "Connection error", JOptionPane.PLAIN_MESSAGE );
        bluetoothButton.setSelected( false );
        setInterfaceState( null );
      }
      else
      {
        btio.setDisconnecting( false );
        btio.setProgressUpdater( this );;
        if ( use == Use.CONNECT && bleRemote != null )
        {
          btio.setBleRemote( bleRemote );
          try
          {
            if ( btio.connectUEI() )
            {
              System.err.println( "Connection complete" );
              bluetoothButton.setBorder( BorderFactory.createLoweredBevelBorder() );
              downloadAction.setEnabled( true );
              installExtenderItem.setEnabled( true );
              extraStatus.setVisible( false );
              advProgressLabel.setText( "Memory usage:" );
              bleStatus.setVisible( true );
              updateBleStatus();
              recreateToolbar();
            }
            else
            {
              disconnectBLE();
              System.err.println( "Connection failed to complete" );
              String message = "Attempt to connect to remote " + bleRemote.name + " failed";
              JOptionPane.showMessageDialog( null, message, "Connection error", JOptionPane.ERROR_MESSAGE );                  
            }
          }
          catch ( InterruptedException e )
          {
            e.printStackTrace();
          }
          setInterfaceState( null );
        }
        else if ( use == Use.SEARCH && bleRemote == null )
        {
          setInterfaceState( "SEARCHING..." );
//          btio.setBleMap( bleMap );
          btio.discoverUEI( true );
          searchButton.setEnabled( false );
          String lastRemote = properties.getProperty( "LastBLERemote" );
          int listStart = 0;
          long waitStart = Calendar.getInstance().getTimeInMillis();
          // Allow a maximum scan time of 15 minutes
          while ( btio.isScanning() && ( Calendar.getInstance().getTimeInMillis() - waitStart) < 900000L )
          {
            int size = btio.getListSize();
            if ( size > listStart )
            {
              System.err.println( "Size now = " + size);
              for ( int i = listStart; i < size; i++)
              {
                String addr = btio.getListItem( i );
                if ( !bleMap.containsKey( addr ) )
                {
                  String ueiName = btio.getItemName( i );
                  BLERemote dev = new BLERemote( ueiName + " " + addr.substring( 9 ), ueiName, addr );
                  dev.found = true;
                  dev.rssi = btio.getRssi( i );
                  System.err.println("Create remote: " + dev.toString());
                  bleMap.put( addr, dev );
                }
                else
                {
                  bleMap.get( addr ).found = true;
                }
              }
              listStart = size;
            }
            
            for ( JRadioButton btn : bleBtnMap.keySet() )
            {
              BLERemote rem = bleBtnMap.get( btn );
              if ( rem.found && !btn.isEnabled() )
              {
                btn.setEnabled( true );
              }
            }
            
            if ( bleMap.size() > bleBtnMap.size() )
            {
              for ( BLERemote dev : bleMap.values() )
              {
                if ( !bleBtnMap.values().contains( dev ) )
                {
                  JRadioButton rb = new JRadioButton( dev.name );
                  if ( lastRemote != null && lastRemote.equals( dev.address ) )
                    rb.setSelected( true );
                  bleBtnMap.put( rb, dev );
                  btGroup.add( rb );
                  box.add( rb );
                  changed = true;
                }
              }
              box.revalidate();
            }
            Thread.sleep( 200 );
          }
          searchButton.setEnabled( true );
          btio.discoverUEI( false );
          setInterfaceState( null );
        }
      }
      return null;
    }

    @Override
    public void done()
    {
      super.done();
      setInterfaceState( null );
      if ( use == Use.CONNECT && btio != null && btio.getBleRemote() != null && !btio.getBleRemote().supportsUpload )
      {
        String title = "Connection";
        String message = "Please note that this remote needs an extender in order to\n"
            + "support uploading via its Bluetooth interface.";                
        JOptionPane.showMessageDialog( RemoteMaster.this, message, title, JOptionPane.INFORMATION_MESSAGE );
      }
      if ( !ok )
      {
        bluetoothButton.setSelected( false );
        uploadable = false;
        uploadAction.setEnabled( false );
        downloadAction.setEnabled( false );
        JOptionPane.showMessageDialog( RemoteMaster.this, "Error connecting to remote " + bleRemote.name, "Task error", JOptionPane.ERROR_MESSAGE );
        return;
      }
    }
    
    @Override
    public void updateProgress( int value )
    {     
//        String name = io != null ? io.getProgressName() : null;
//        setInterfaceState( name != null ? name : "PREPARING:", value );
      setInterfaceState( "CONNECTING:", value );
    }
    
  }
  
  private class SaveTask extends WriteTask implements ProgressUpdater
  {
    private File file = null;
    private Use use = null;
    private RMWavConverter converter = null;
    private AddressRange range = null;
    
    public SaveTask( File file, Use use )
    {
      this.file = file;
      this.use = use;
    }
    
    public SaveTask( File file, Use use, AddressRange range )
    {
      this.file = file;
      this.use = use;
      converter = new RMWavConverter( remoteConfig );
      converter.setProgressUpdater( this );
      this.range = range;
    }
    
    @Override
    protected Void doInBackground() throws Exception
    {
      resetInterfaceState();
      if ( use == Use.EXPORT && range == null )
      {
        remoteConfig.exportIR( file );
        if ( exitPrompt )
        {
          changed = false;
        }
        updateRecentFiles( file );
      }
      else if ( use == Use.EXPORT )
      {
        converter.exportWav( file, range );
      }
      else
      {
        remoteConfig.save( file );
        changed = false;
        if ( use == Use.SAVEAS && binLoaded() == null )
        {
          RemoteMaster.this.file = file;
          setTitleFile( file );
          updateRecentFiles( file );
          saveAction.setEnabled( true );
        }
      }
      setInterfaceState( null );
      return null;
    }
    
    @Override
    public void done()
    {
      super.done();
      setInterfaceState( null );
      if ( !ok )
      {
        JOptionPane.showMessageDialog( RemoteMaster.this, "Error saving file " + file.getName(), "Task error", JOptionPane.ERROR_MESSAGE );
      }
      if ( exitPrompt )
      {
        exitPrompt = false;
        dispatchEvent( new WindowEvent( RemoteMaster.this, WindowEvent.WINDOW_CLOSING ) );
      }
    }
    
    public void updateProgress( int value )
    {
      if ( value < 0 )
      {
        setInterfaceState( "DOWNLOADING..." );
      }
      else
      {
        String name = converter != null ? converter.getProgressName() : null;
        setInterfaceState( name != null ? name : "PREPARING:", value );
      }
    }
  }
  
  /**
   * Sets the titleFile attribute of the RemoteMaster object.
   * 
   * @param file
   *          the file
   */
  private void setTitleFile( File file )
  {
    if ( file == null || remoteConfig == null )
    {
      setTitle( "RMIR" );
    }
    else
    {
      setTitle( "RMIR: " + file.getName() + " - " + remoteConfig.getRemote().getName() );
    }
  }

  public ArrayList< IO > getInterfaces()
  {
    return interfaces;
  }
  
  /**
   * Gets the open interface.
   * 
   * @return the open interface
   */
  public IO getOpenInterface( File file, Use use, ProgressUpdater progressUpdater )
  {
    String interfaceName = properties.getProperty( "Interface" );
    System.err.println( "Interface Name = " + ( interfaceName == null ? "NULL" : interfaceName ) );
    String portName = properties.getProperty( "Port" );
    System.err.println( "Port Name = " + ( portName == null ? "NULL" : portName ) );
    IO ioOut = null;
    if ( interfaceName != null && ( use == Use.DOWNLOAD || use == Use.UPLOAD 
        || use == Use.RAWDOWNLOAD || use == Use.CONNECT ) )
    {
      for ( IO temp : interfaces )
      {
        String tempName = temp.getInterfaceName();
        System.err.println( "Testing interface: " + ( tempName == null ? "NULL" : tempName ) );
        if ( tempName.equals( interfaceName ) )
        {
          String item = use == Use.CONNECT ? "BLE interface" : "remote";
          System.err.println( "Interface matched.  Trying to open " + item );
          ioOut = testInterface( temp, portName, file, use, progressUpdater );
          if ( ioOut == null )
          {
            System.err.println( "Failed to open" );
          }
          break;
        }
      }
    }
    else
    {
      for ( IO temp : interfaces )
      {
        if ( temp instanceof JP2BT )
        {
          // The BT interface must be selected explicitly.
          continue;
        }
        
        if ( temp instanceof JP1Parallel && System.getProperty( "os.name" ).equals( "Linux" ) )
        {
          String title = "Linux Parallel Port Interface";
          String message = 
                "Auto-detect has not been able to find a remote with any other interface, so is about to try the\n"
              + "Linux parallel port driver, which is unsafe.  It may cause RMIR to crash.  It uses \"bitbanging\"\n"
              + "which is incompatible with modern operating systems and so can only be run with root privileges.\n"
              + "It is not supported.  Use it at your own risk.\n\n"
              + "To use it without getting this message every time, select it explicitly with the Interface item\n"
              + "on the Remote menu rather than using Auto-detect.\n\n"
              + "Do you wish to continue?";
          if ( NegativeDefaultButtonJOptionPane.showConfirmDialog( this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE )
                 != JOptionPane.YES_OPTION )
          {
            continue;
          }
        }
        String tempName = temp.getInterfaceName();
        System.err.println( "Testing interface: " + ( tempName == null ? "NULL" : tempName ) );
        ioOut = testInterface( temp, null, file, use, progressUpdater );
        if ( ioOut != null )
        {
          System.err.println( "Opened interface type " + Integer.toHexString( ioOut.getInterfaceType() ) );
          break;
        }
      }
    }
    
    if ( ioOut != null )
    {
      ioOut.setUse( use );
    }
    
    if ( ioNeedsPowerManagementCheck( ioOut ) )
    {
      CommHID ioHID = ( CommHID )ioOut;
      int enabled = ioHID.getEnhancedPowerManagementStatus();
      if ( enabled < 0 )
      {
        System.err.println( "Enhanced Power Management is not supported" );
      }
      else if ( enabled > 0 )
      {
        String title = "Enhanced Power Management";
        String message = 
            "Uploading or downloading from this remote requires Enhanced Power Management\n"
                + "to be disabled in Windows.  A zip file with instructions on how to do this,\n"
                + "including registry patches to do so and an explanation of how to do so manually\n"
                + "with regedit, is available in the JP1 forum.  There is a link to this file from\n"
                + "the Help menu.  It is preferable to use regedit to edit the Windows registry\n"
                + "directly if you feel confident in doing so.  The registry key you need to change\n"
                + "is at:\n\n" + ( ( CommHID )ioOut ).getRegistryKey() + "\n\n"
                + "where you need to change the value of \"EnhancedPowerManagementEnabled\" from 1 to 0\n"
                + "by right-clicking and selecting Modify.\n\n"
                + "After this change, you need to disconnect and reconnect the remote from the PC.";
        JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
        ioOut = null;
      }
    }
    return ioOut;
  }
  
  private IO testInterface( IO ioIn, String portName, File file, Use use, ProgressUpdater progressUpdater )
  {
    String ioName = ioIn.getInterfaceName();
//    String osName = System.getProperty( "os.name" );
    if ( file != null && !ioName.equals( "JPS" ) )
    {
      return null;
    }

    if ( ioName.equals( "JP2BT" ) )
    {
      if ( use == Use.DOWNLOAD || use == Use.UPLOAD)
      {
        if ( btio != null )
          System.err.println( "JP2BT interface is already open" );
      }
      else if ( use == Use.CONNECT )
      {
        JP2BT iobt = ( JP2BT )ioIn;
        iobt.setBleInterface( portName );
        String portResult = iobt.connectBLE( portName );
        String bleStack = iobt.getBleStack();
        if ( portResult == null )
        {
          System.err.println( "Failed to connect to BLE interface on port " +  portName );
          portName = "";
        }
        else if ( bleStack == null )
        {
          System.err.println( "Connected BLE interface but no supported BLE stack found" );
          portName = "";
        }
        else if ( bleStack.equals( "Microsoft" ) && !testWindowsVersion( "10.0.0" ) )
        {
          System.err.println( "Connected BLE interface but Microsoft BLE stack needs Windows 10" );
          portName = "";
        }
        else
        {
          System.err.println( "Connected BLE interface on port " +  portName );
          System.err.println( "BLE stack is " + bleStack );
        }
      }
    }
    
    if ( ioName.equals( "JPS" ) )
    {
      JPS jps = ( JPS )ioIn;
      if ( jps.isOpen() && ( use == Use.SAVING || use == Use.SAVEAS ) )
      {
        portName = jps.getFilePath();
        System.err.println( "Already open on Port " + portName );
        if ( use == Use.SAVEAS )
        {
          portName = file.getAbsolutePath();
          System.err.println( "Changing Port to " + portName );
          jps.setFilePath( portName );
        }
        return ioIn;
      }
    }
    ioIn.setProgressUpdater( progressUpdater );
    if ( use != Use.CONNECT )
      portName = ioIn.openRemote( portName != null ? portName : file != null ? file.getAbsolutePath() : null );
    if ( portName == null ) portName = "";
    System.err.println( "Port Name = " + ( portName.isEmpty() ? "NULL" : portName ) );
    if ( !portName.isEmpty() )
    {
      System.err.println( "Opened on Port " + portName );
      return ioIn;
    }
    else
    {
      ioIn.setProgressUpdater( null );
      return null;
    }
  }
  
  public static boolean ioNeedsPowerManagementCheck( IO io )
  {
    // True if interface is CommHID and os.version >= 6.3, which is Windows 8.1
    return io != null && io.getInterfaceName().equals( "CommHID" ) 
        && System.getProperty( "os.name" ).startsWith( "Windows" )
        && Float.parseFloat( System.getProperty( "os.version" ) ) >= 6.3f;
  }
  
  public boolean usesNonModalDeviceEditor()
  {
    return Boolean.parseBoolean( properties.getProperty( "NonModalDeviceEditor", "false" ) );
  }
  
  public boolean sameSigSameRemote()
  {
    return Boolean.parseBoolean( properties.getProperty( "SameSigSameRemote", "false" ) );
  }
  
  public void setNonModalWarning( boolean warn, DeviceUpgradeEditor duEditor )
  {
    if ( duEditor != null )
    {
      // comes from DeviceUpgradeEditor
      this.duEditor = warn ? duEditor : null;
    }
    else
    {
      // comes from menu
      if ( this.duEditor != null && !warn )
      {
        // editor is open in non-modal state, being changed to modal
        if ( this.duEditor.getState() == Frame.ICONIFIED )
        {
          this.duEditor.setExtendedState( Frame.NORMAL );
        }
        this.duEditor.toFront();
        this.duEditor = null;
        setEnabled( false );
      }
      else
      {
        setEnabled( true );
      }
    }
    setInterfaceState( interfaceText );
  }

  public void setInterfaceState( String state )
  {
    setInterfaceState( state, null );
  }

  private void setInterfaceState( String state, Integer progress )
  {
    interfaceText = state;
    if ( duEditor != null )
    {
      ( ( CardLayout )statusBar.getLayout() ).show( statusBar, "WARNING" );
    }
    else if ( state != null )
    {
      if ( progress != null ) {
        if (interfaceState.isIndeterminate())
          interfaceState.setIndeterminate( false );
        interfaceState.setValue( progress );
        state = state + " " + progress + "%";
      }
      else if (!interfaceState.isIndeterminate())
      {
        javax.swing.SwingUtilities.invokeLater( new Runnable()
        {
          public void run()
          {
            interfaceState.setIndeterminate( true );
            interfaceState.setValue( 0 );
          }
        } );
      }

      interfaceState.setString( state );
      ( ( CardLayout )statusBar.getLayout() ).show( statusBar, "INTERFACE" );
    }
    else
    {
      ( ( CardLayout )statusBar.getLayout() ).show( statusBar, "MEMORY" );
    }
  }
  
  public void clearAllInterfaces()
  {
    for ( IO io : interfaces )
    {
      io.clear();
    }
  }
  
  public static String getIOsignature( IO io, int baseAddress )
  {
    String sig = null;
    if ( io.getInterfaceType() > 0 ) // JP12Serial versions later than 0.18a
    {
      sig = io.getRemoteSignature();
    }
    else // Other interfaces and earlier JP12Serial versions
    {
      short[] sigData = new short[ 10 ];
      int count = io.readRemote( baseAddress, sigData );
      System.err.println( "Read first " + count + " bytes: " + Hex.toString( sigData ) );
      sig = Hex.getRemoteSignature( sigData );
    }
    if ( sig != null )
    {
      while ( sig.endsWith( "_" ) )
      {
        sig = sig.substring( 0, sig.length() - 1 );
      }
    }
    return sig;
  }
  
  public List< SummarySection > getSummarySections( JTabbedPane pane, int index )
  {
    if ( index < 0 || index >= pane.getTabCount() )
      return null;

    Remote remote = remoteConfig.getRemote();
    List< SummarySection > list = new ArrayList< SummarySection >();
    SummarySection ss = new SummarySection();
    Component c = pane.getComponentAt( index );
    if ( c == null )
    {
      return null;
    }
    else if ( c instanceof RMTablePanel< ? > )
    {
      ss.model = ( ( RMTablePanel< ? > )c ).getModel();
      ss.title = pane.getTitleAt( index );
      ss.sorter = ( ( RMTablePanel< ? > )c ).getSorter();
      ss.table = ( ( RMTablePanel< ? > )c ).getTable();
      list.add( ss );
      return list;
    }
    else if ( c instanceof GeneralPanel )
    {
      ss.model = ( ( GeneralPanel )c ).getDeviceButtonTableModel();
      ss.title = "Device Buttons";
      list.add( ss );

      if ( remoteConfig != null && remote.hasSettings() )
      {
        ss = new SummarySection();
        ss.model = ( ( GeneralPanel )c ).getSettingModel();
        ss.title = "Other Settings";
        list.add( ss );
      }
      return list;
    }
    else if ( c instanceof FavoritesPanel )
    {
      FavoritesPanel fp = ( FavoritesPanel )c;
      ss.model = fp.getFavModel();
      ss.title = "Favorites Macros";
      list.add( ss );
      ss = new SummarySection();
      ss.model = fp.getActivityGroupModel();
      ss.title = "Favorites Group Assignments";
      list.add( ss );
      return list;     
    }
    else if ( c instanceof ActivityPanel )
    {
      ActivityPanel ap = ( ActivityPanel )c;
      List< Activity > aList = ap.getActivityList();
      ActivityFunctionTableModel aftm = ap.getActivityFunctionModel();
      ActivityGroupTableModel agtm = ap.getActivityGroupModel();
      for ( Activity activity : aList )
      {
        if ( !remote.hasActivityAlgorithm() )
        {
          ss = new SummarySection();
          ss.model = aftm;
          ss.subtitle = activity.getName();
          ss.activity = activity;
          list.add( ss );
        }
        ss = new SummarySection();
        ss.model = agtm;
        ss.activity = activity;
        list.add( ss );
      }
      if ( list.size() > 0 )
        list.get( 0 ).title = "Activities";
      return list;
    }
    else return null;
  }

  /**
   * Description of the Method.
   * 
   * @param e
   *          the e
   */
  public void actionPerformed( ActionEvent e )
  {
    finishEditing();
    List< JMenuItem > exportToWavList = Arrays.asList( exportToWavImageItem, 
        exportToWavSettingsItem, exportToWavMacrosEtcItem, exportToWavTimedMacrosItem, 
        exportToWavUpgradesItem, exportToWavLearnedItem );
    int wavIndex = -1;
    try
    {
      Object source = e.getSource(); 
      if ( source == installExtenderItem )
      {
        installExtender();
      }
      else if ( source == importFromWavNewItem || source == importFromWavMergeItem )
      {
        openFile( null, source == importFromWavNewItem ? WavOp.NEW : WavOp.MERGE );
      }
      else if ( ( wavIndex = exportToWavList.indexOf( source ) ) >= 0 )
      {
        saveAs( WavOp.SAVE, wavIndex );
      }
      else if ( source == uploadWavItem )
      {
        openFile( null, WavOp.PLAY );
      }
      else if ( source == cancelWavUploadItem )
      {
        if ( wavPlayer != null )
        {
          wavPlayer.close();
        }
      }
      else if ( source == createSummaryItem )
      {
        List< SummarySection > ssList = new ArrayList< SummarySection >();
        for ( int i = 0; i < tabbedPane.getTabCount(); i++ )
        {
          if ( getSummarySections( tabbedPane, i ) != null )
          {
            ssList.addAll( getSummarySections( tabbedPane, i ) );
          }
        }

        HtmlGenerator htmlGen = new HtmlGenerator( remoteConfig );
        if ( desktop != null && htmlGen.makeHtml( ssList ) )
        {
          desktop.browse( summaryFile.toURI() );
        }
      }
      else if ( source == viewSummaryItem )
      {
        if ( !summaryFile.exists() )
        {
          String message = "There is no summary available to view.";
          String title = "View Summary";
          JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
        }
        else if ( desktop != null )
        {
          desktop.browse( summaryFile.toURI() );
        }
      }
      else if ( source == saveSummaryItem )
      {
        if ( !summaryFile.exists() )
        {
          String message = "There is no summary available to save.";
          String title = "Save Summary";
          JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
        }
        else
        {
          saveAs( summaryFile, null, 0 );
        }
      }
      else if ( source == exitItem )
      {
        dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
      }
      else if ( source == downloadRawItem )
      {
        if ( !promptToSave() )
        {
          return;
        }
        RawDataDialog dlg = new RawDataDialog( this );
        dlg.setVisible( true );
      }
      else if ( source == verifyUploadItem )
      {
        properties.setProperty( "verifyUpload", Boolean.toString( verifyUploadItem.isSelected() ) );
      }
      else if ( source == highlightItem )
      {
        properties.setProperty( "highlighting", Boolean.toString( highlightItem.isSelected() ) );
        if ( currentPanel instanceof RMTablePanel< ? > )
        {
          RMTablePanel< ? > panel = ( RMTablePanel< ? > )currentPanel;
          panel.getModel().fireTableStructureChanged();
        }
        else if ( currentPanel == generalPanel )
        {
          generalPanel.getDeviceButtonTableModel().fireTableStructureChanged();
          generalPanel.getSettingModel().fireTableStructureChanged();
        }
        currentPanel.set( remoteConfig );
        recreateToolbar();
      }
      else if ( source == enablePreserveSelection )
      {
        properties.setProperty( "enablePreserveSelection", Boolean.toString( enablePreserveSelection.isSelected() ) );
      }
      else if ( source == showSlingboxProtocols )
      {
        if ( !showSlingboxProtocols.isSelected() )
        {
          properties.remove( "ShowSlingboxProtocols" );
        }
        else
        {
          properties.setProperty( "ShowSlingboxProtocols", Boolean.toString( showSlingboxProtocols.isSelected() ) );
        }
      }
      else if ( source == defaultDelayItem )
      {
        properties.remove( "TooltipDelay" );
        tooltipDelay = tooltipDefaultDelay;
        ToolTipManager.sharedInstance().setInitialDelay( tooltipDefaultDelay );
      }
      else if ( source == specifiedDelayItem )
      {
        int val = 0;
        do
        {
          String ret = ( String )JOptionPane.showInputDialog( this, "Specify tooltip delay in milliseconds:", "Tooltip Delay", 
              JOptionPane.PLAIN_MESSAGE, null, null, ""+tooltipDelay );
          if ( ret == null )
          {
            val = -1;
            break;
          }
          try
          {
            val = Integer.parseInt( ret );
          }
          catch ( NumberFormatException nfe )
          {
            val = -1;
          }
          if ( val < 0 )
          {
            JOptionPane.showMessageDialog( this, "You must enter a valid non-negative integer", "Tooltip Delay", JOptionPane.ERROR_MESSAGE );
          }
        }
        while ( val < 0 );
        if ( val >= 0 )
        {
          tooltipDelay = val;
          ToolTipManager.sharedInstance().setInitialDelay( tooltipDelay );
          properties.setProperty( "TooltipDelay", ""+tooltipDelay );
        }
      }
      else if ( source == noUpgradeItem )
      {
        if ( !noUpgradeItem.isSelected() )
        {
          properties.remove( "NoUpgrade" );
        }
        else
        {
          properties.setProperty( "NoUpgrade", Boolean.toString( noUpgradeItem.isSelected() ) );
        }
      }
      else if ( source == cleanUpperMemoryItem )
      {
        String title = "Clean Upper Memory";
        String message = "";
        if ( remoteConfig.hasSegments() )
        {
          message += "Please ";
        }
        else
        {
          message += "Do you want to retain all data in the first $100 (i.e. 256) bytes of memory?\n\n"
            + "If you answer No then the memory will be set as if your present setup was\n"
            + "installed on a reset state created in accordance with the RDF alone.  This\n"
            + "is the cleanest option but most RDFs at present do not create a true factory\n" + "reset state.\n\n"
            + "If you answer Yes then any data in the first $100 bytes not set by the RDF\n"
            + "will be retained.  This should include any data set by a factory reset that\n"
            + "is missing from the RDF, but it may also include other data that could be\n" + "usefully cleaned.\n\n"
            + "Please also ";
        }
        message += "be aware that blasting the memory will destroy most extenders, as\n"
            + "they place at least part of their code in the memory that will be cleared.\n"
            + "Press Cancel to exit without blasting the memory.";
        int result = NegativeDefaultButtonJOptionPane.showConfirmDialog( this, message, title, 
            remoteConfig.hasSegments() ? JOptionPane.OK_CANCEL_OPTION : JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE );
        if ( result == JOptionPane.CANCEL_OPTION )
        {
          return;
        }
        
        if ( result == JOptionPane.OK_OPTION )
        {
          // Remote has segments
          Remote remote = remoteConfig.getRemote();
          short[] data = remoteConfig.getData();
          Arrays.fill( data, remote.getUsageRange().getFreeStart(), data.length, ( short )0xFF );
          remoteConfig.updateCheckSums();
          update();
          return;
        }
        
        // Continue for remotes without segments.
        // Save the data that is stored only in the remote image.
        Remote remote = remoteConfig.getRemote();
        DeviceButton[] devBtns = remote.getDeviceButtons();
        int[] devBtnData = new int[ 2 * devBtns.length ];
        DeviceLabels devLabels = remote.getDeviceLabels();
        String[] devLabelText = new String[ 2 * devBtns.length ];
        SoftDevices softDevices = remote.getSoftDevices();
        int[] softSequence = new int[ devBtns.length + 1 ];
        Setting[] settings = remote.getSettings();
        int[] settingValues = new int[ settings.length ];
        short[] data = remoteConfig.getData();
        for ( int i = 0; i < devBtns.length; i++ )
        {
          devBtnData[ 2 * i ] = devBtns[ i ].getDeviceSlot( data );
          devBtnData[ 2 * i + 1 ] = devBtns[ i ].getDeviceGroup( data );
          if ( devLabels != null )
          {
            devLabelText[ 2 * i ] = devLabels.getText( data, i );
            devLabelText[ 2 * i + 1 ] = devLabels.getDefaultText( data, i );
          }
          if ( softDevices != null )
          {
            softSequence[ i ] = softDevices.getSequenceIndex( i, data );
          }
        }
        if ( softDevices != null )
        {
          softSequence[ devBtns.length ] = softDevices.getFilledSlotCount( data );
        }
        for ( int i = 0; i < settings.length; i++ )
        {
          settingValues[ i ] = settings[ i ].getValue();
        }
        remote.setFixedData( remote.getRawFixedData() );

        // Create clean reset state
        remoteConfig.initializeSetup( result == JOptionPane.YES_OPTION ? 0x100 : 0 );

        // Restore the data that is stored only in the remote image
        for ( int i = 0; i < devBtns.length; i++ )
        {
          devBtns[ i ].setDeviceSlot( devBtnData[ 2 * i ], data );
          devBtns[ i ].setDeviceGroup( ( short )devBtnData[ 2 * i + 1 ], data );
          if ( devLabels != null )
          {
            devLabels.setText( devLabelText[ 2 * i ], i, data );
            devLabels.setDefaultText( devLabelText[ 2 * i + 1 ], i, data );
          }
          if ( softDevices != null )
          {
            softDevices.setSequenceIndex( softSequence[ i ], i, data );
          }
        }
        if ( softDevices != null )
        {
          softDevices.setFilledSlotCount( softSequence[ devBtns.length ], data );
        }
        for ( int i = 0; i < settings.length; i++ )
        {
          settings[ i ].setValue( settingValues[ i ] );
        }

        // Update
        if ( result == JOptionPane.NO_OPTION )
        {
          // The state has now been constructed solely from the RDF, so set date indicator
          remoteConfig.setDateIndicator();
        }
        remoteConfig.updateImage();
        update();
      }
      else if ( source == clearAltPIDHistory )
      {
        ProtocolManager pm = ProtocolManager.getProtocolManager();
        int count = pm.countAltPIDRemoteEntries();
        String title = "Clear Alt PID History";
        String message = "The Alt PID History is used only to help a protocol to be recognised when other means\n"
            + "fail, such as in a download from a remote when it has been uploaded with an Alternate\n"
            + "PID instead of the standard one for that protocol.  There is seldom any need to clear\n"
            + "this history unless its size is becoming excessive.\n\n" + "It currently has " + count;
        message += count == 1 ? " entry." : " entries.";
        message += "\n\nAre you sure you want to clear this history?";
        int ans = JOptionPane.showConfirmDialog( this, message, title, JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE );
        if ( ans == JOptionPane.YES_OPTION )
        {
          pm.clearAltPIDRemoteEntries();
          message = "Cleared!";
          JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
        }
      }
      else if ( source == initializeTo00Item )
      {
        short[] data = getInitializationData( 0 );
        if ( data != null )
        {
          System.err.println( "Starting upload to initialize to FF" );
          setInterfaceState( "INITIALIZING TO 00..." );
          ( new UploadTask( data, false ) ).execute();
        }
      }
      else if ( source == initializeToFFItem )
      {
        short[] data = getInitializationData( 0xFF );
        if ( data != null )
        {
          System.err.println( "Starting upload to initialize to FF" );
          setInterfaceState( "INITIALIZING TO FF..." );
          ( new UploadTask( data, false ) ).execute();
        }
      }
      else if ( source == useSavedDataItem )
      {
        if ( currentPanel == rawDataPanel )
        {
          rawDataPanel.set( remoteConfig );
        }
        else if ( currentPanel == segmentPanel )
        {
          segmentPanel.set( remoteConfig );
        }
      }
      else if ( source == putSystemFileItem )
      {
          setInterfaceState( "GETTING FILE LIST..." );
          ( new XziteFileTask( "Upload" ) ).execute();
      }
      else if ( source == verifyXZITEfilesItem )
      {
          setInterfaceState( "VERIFYING..." );
          ( new XziteFileTask( "Verify" ) ).execute();
      }
      else if ( source == xziteOpsItem )
      {
        setInterfaceState( "GETTING FILE LIST..." );
        ( new XziteFileTask( "Delete/Save" ) ).execute();
      }
      else if ( source == xziteReformatItem )
      {
        String title = "Reformat XSight";
        String message = "<html>Are you sure you want to format the file system of the remote<br>"
            + "and reinstall the system files?  This operation will leave the<br>"
            + "remote in factory reset state, all setup data having been deleted.<br>"
            + "If you continue, be aware that during the rebuild phase it is normal<br>"
            + "for the progress indicator to stay at 15% for an extended period.<br><br>"
            + "Make sure you have saved any setup that you want to reload after<br>"
            + "the rebuild.<br><br>Do you want to continue?</html>";
        int reply = NegativeDefaultButtonJOptionPane.showConfirmDialog( this, message, title, 
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
        if ( reply != JOptionPane.YES_OPTION )
        {
          return;
        }
        setInterfaceState( "FORMATTING..." );
        ( new XziteFileTask( "Reformat" ) ).execute();
      }
      else if ( source == saveFDRAfirmware )
      {
        setInterfaceState( "SAVING FIRMWARE..." );
        ( new XziteFileTask( "FdraFirmware" ) ).execute();
      }
      else if ( source == upgradeSourceItem )
      {
        Remote remote = remoteConfig != null ? remoteConfig.getRemote() : null;
        UpgradeSourceSelector selector = new UpgradeSourceSelector( this, remote, upgradeSource, upgradeLanguage );
        selector.setVisible( true );
        upgradeSource = selector.getSource();
        upgradeLanguage = selector.getSelectedLanguage();
      }
      else if ( source == setBaselineItem )
      {
        remoteConfig.setBaselineData();
        clearBaselineItem.setEnabled( true );
        if ( currentPanel == rawDataPanel )
        {
          rawDataPanel.set( remoteConfig );
        }
      }
      else if ( source == clearBaselineItem )
      {
        remoteConfig.clearBaselineData();
        clearBaselineItem.setEnabled( false );
        if ( currentPanel == rawDataPanel )
        {
          rawDataPanel.set( remoteConfig );
        }
      }
      else if ( source == parseIRDBItem )
      {
        extractIrdb();
        setSystemFilesItems( this, remoteConfig.getRemote() );
      }
      else if ( source == getSystemFilesItem )
      {
        if ( getSystemFilesItem.isSelected() )
        {
          String title = "System files";
          String message = "<html>The system files will be saved to a zip package in an<br>"
              + "XSight subfolder of the RMIR installation folder on the<br>"
              + "next download.  This subfolder will be created if it does<br>"
              + "not exist.</html>";
          JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
        }
      }
      else if ( source == extractSSItem )
      {
        extractSS();
      }
      else if ( source == analyzeMAXQprotocols )
      {
        JP2Analyzer maxq = new JP2Analyzer();
        maxq.analyze();
      }
      else if ( source == rdfPathItem )
      {
        File path = getRDFPathChoice();
        if ( path == null )
        {
          return;
        }

        int opt = JOptionPane.NO_OPTION;
        if ( remoteConfig != null )
        {
          String message = "Do you want to apply this directory change immediately?\n\n"
              + "Yes = the present setup will be reinterpreted with an RDF from the new directory;\n"
              + "No = the change will take place when you next open a remote, even within this session;\n"
              + "Cancel = the change will be cancelled.\n\n"
              + "Note that if you answer Yes, the setup will still have been loaded with the old RDF.\n"
              + "You can achieve a similar result by answering No, using File/Save As to save the setup\n"
              + "with the old RDF and then opening the saved file, which will open with the new RDF.\n"
              + "The best choice between these two methods can depend on how different the RDFs are,\n"
              + "and what you are trying to achieve.";

          String title = "Change of RDF Directory";
          opt = JOptionPane.showConfirmDialog( this, message, title, JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE );
        }
        if ( opt == JOptionPane.CANCEL_OPTION )
        {
          return;
        }
        properties.setProperty( "RDFPath", path );
        RemoteManager mgr = RemoteManager.getRemoteManager();
        mgr.reset();
        mgr.loadRemotes( properties );
        if ( opt == JOptionPane.NO_OPTION )
        {
          return;
        }
        String rmTitle = getTitle();
        remoteConfig.setSavedData();
        Remote oldRemote = remoteConfig.getRemote();
        // Save the setting values
        Setting[] oldSettings = oldRemote.getSettings();
        int[] settingValues = new int[ oldSettings.length ];
        for ( int i = 0; i < oldSettings.length; i++ )
        {
          settingValues[ i ] = oldSettings[ i ].getValue();
        }
        Remote newRemote = RemoteManager.getRemoteManager().findRemoteByName( oldRemote.getName() );
        remoteConfig.setRemote( newRemote );
        for ( DeviceUpgrade du : remoteConfig.getDeviceUpgrades() )
        {
          du.setRemote( newRemote );
        }
        Setting[] newSettings = newRemote.getSettings();
        for ( int i = 0; i < Math.min( oldSettings.length, newSettings.length ); i++ )
        {
          newSettings[ i ].setValue( settingValues[ i ] );
        }
        SetupCode.setMax( newRemote );
        remoteConfig.updateImage();
        RemoteConfiguration.resetDialogs();
        update();
        int index = rmTitle.lastIndexOf( oldRemote.getName() );
        setTitle( rmTitle.substring( 0, index ) + newRemote.getName() );
      }
      else if ( source == mapPathItem )
      {
        File path = getMapPathChoice();
        if ( path == null )
        {
          return;
        }
        int opt = JOptionPane.NO_OPTION;
        if ( remoteConfig != null )
        {
          String message = "Do you want to apply this directory change immediately?\n\n"
              + "Yes = a map and image from the new directory will be used in the present setup;\n"
              + "No = the change will take place when you next open a remote, even within this session;\n"
              + "Cancel = the change will be cancelled.";

          String title = "Change of Map and Image Directory";
          opt = JOptionPane.showConfirmDialog( this, message, title, JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE );
        }
        if ( opt == JOptionPane.CANCEL_OPTION )
        {
          return;
        }
        properties.setProperty( "ImagePath", path );
        if ( remoteConfig == null )
        {
          return;
        }
        Remote remote = remoteConfig.getRemote();
        remote.resetImageMaps( path );
      }
      else if ( source == addonPathItem )
      {
        File path = getAddonPathChoice();
        if ( path != null )
        {
          String message = "The default path for Add-ons is the AddOns subfolder of the \n"
              + "RMIR installation folder.  You have changed it to:\n\n"
              + path.getAbsolutePath() + "\n\n"
              + "This change will take place when you next open RMIR.";
          String title = "Change of Add-ons Directory";
          JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
        }
      }
      else if ( source == updateItem )
      {
        UpdateChecker.checkUpdateAvailable( this );
      }
      else if ( source == aboutItem )
      {
        StringBuilder sb = new StringBuilder( 1000 );
        sb.append( "<html><b>RemoteMaster " );
        sb.append( version );     
        sb.append( " build " + getBuild() );
        sb.append( admin ? " (admin mode)" : "" );
        sb.append( "</b>" );
        sb.append( "<p>Written primarily by Greg&nbsp;Bush " );
        sb.append( "with substantial additions and help<br>from Graham&nbsp;Dixon</p>" );
        sb.append( "<p>Additional help was provided by:<blockquote>" );
        sb.append( "John&nbsp;S&nbsp;Fine, Nils&nbsp;Ekberg, Jon&nbsp;Armstrong, Robert&nbsp;Crowe, " );
        sb.append( "Mark&nbsp;Pauker, Mark&nbsp;Pierson, Mike&nbsp;England</blockquote></p>" );

        sb.append( "<p>RDFs loaded from <b>" );
        sb.append( properties.getProperty( "RDFPath" ) );
        sb.append( "</b></p>" );

        sb.append( "</p><p>Images and Maps loaded from <b>" );
        sb.append( properties.getProperty( "ImagePath" ) );
        sb.append( "</b></p>" );
        if ( LearnedSignal.hasDecodeIR() )
        {
          sb.append( "<p>DecodeIR version " );
          sb.append( LearnedSignal.getDecodeIRVersion() );
          sb.append( "</p>" );
        }
        else
        {
          sb.append( "<p><b>DecodeIR is not available!</b></p>" );
        }
        
        sb.append( "<p>IrpTransmogrifier version " );
        sb.append( Version.version );
        sb.append( "<br/>" );
        sb.append( "IrpDatabase version " );
        sb.append( LearnedSignal.getTmDatabase().getConfigFileVersion() );
        sb.append( "</p>" );

        if ( !interfaces.isEmpty() )
        {
          sb.append( "<p>Interfaces:<ul>" );
          for ( IO io : interfaces )
          {
            sb.append( "<li>" );
            sb.append( io.getInterfaceName() );
            sb.append( " version " );
            sb.append( io.getInterfaceVersion() );
            sb.append( "</li>" );
          }
          sb.append( "</ul></p>" );
        }

        String[] propertyNames =
        {
            "java.version", "java.vendor", "os.name", "os.arch"
        };

        sb.append( "<p>System Properties:<ul>" );
        for ( String name : propertyNames )
        {
          sb.append( "<li>" );
          sb.append( name );
          sb.append( " = \"" );
          sb.append( System.getProperty( name ) );
          sb.append( "\"</li>" );
        }
        sb.append( "</ul>" );

        sb.append( "<p>Libraries loaded from " );
        sb.append( LibraryLoader.getLibraryFolder() );
        sb.append( "</p></html>" );

        JEditorPane pane = new JEditorPane( "text/html", sb.toString() );
        pane.addHyperlinkListener( this );
        pane.setEditable( false );
        pane.setBackground( getContentPane().getBackground() );
        new TextPopupMenu( pane );
        JScrollPane scroll = new JScrollPane( pane );
        Dimension d = pane.getPreferredSize();
        d.height = d.height * 5 / 4;
        d.width = d.width * 2 / 3;
        scroll.setPreferredSize( d );

        JOptionPane.showMessageDialog( this, scroll, "About RM/RMIR", JOptionPane.INFORMATION_MESSAGE, null );
      }
      else if ( source == readmeItem )
      {
        File readme = new File( workDir, "Readme.html" );
        desktop.browse( readme.toURI() );
      }
      else if ( source == tutorialItem )
      {
        URL url = new URL(
            "http://www.hifi-remote.com/wiki/index.php?title=JP1_-_Just_How_Easy_Is_It%3F_-_RM-IR_Version" );
        desktop.browse( url.toURI() );
      }
      else if ( source == rmpbReadmeItem )
      {
        File rmpbReadme = new File( workDir, "RMPB_Readme.html" );
        desktop.browse( rmpbReadme.toURI() );
      }
      else if ( source == learnedSignalItem )
      {
        File file = new File( workDir, "DecodeIR.html" );
        desktop.browse( file.toURI() );
      }
      else if ( source == homePageItem )
      {
        URL url = new URL( "http://controlremote.sourceforge.net/" );
        desktop.browse( url.toURI() );
      }
      else if ( source == wikiItem )
      {
        URL url = new URL( "http://www.hifi-remote.com/wiki/index.php?title=Main_Page" );
        desktop.browse( url.toURI() );
      }
      else if ( source == forumItem )
      {
        URL url = new URL( "http://www.hifi-remote.com/forums/" );
        desktop.browse( url.toURI() );
      }
      else if ( source == powerManagementItem )
      {
        URL url = new URL( "http://www.hifi-remote.com/forums/dload.php?action=file&file_id=14564" );
        desktop.browse( url.toURI() );
      }
      else if ( source == irpTransmogrifierItem )
      {
        System.err.println( "Setting IrpTransmogrifier as decoder for Learned Signals" );
        if ( irpTransmogrifierItem.isSelected() )
          properties.remove( "UseDecodeIR" );
        if ( learnedPanel != null )
        {
          learnedPanel.refresh();
          learnedPanel.getModel().fireTableDataChanged();
        } 
      }
      else if ( source == decodeIRItem )
      {
        System.err.println( "Setting DecodeIR as decoder for Learned Signals" );
        if ( decodeIRItem.isSelected() )
          properties.setProperty( "UseDecodeIR", "true" );
        if ( learnedPanel != null )
        {
          learnedPanel.refresh();
          learnedPanel.getModel().fireTableDataChanged();
        } 
      }
      else
      {
        JMenuItem item = ( JMenuItem )source;
        File file = new File( item.getActionCommand() );
        recentFiles.remove( item );
        if ( file.canRead() )
        {
          openFile( file );
        }
      }
    }
    catch ( Exception ex )
    {
      ex.printStackTrace( System.err );
    }
  }

  /**
   * Description of the Method.
   */
  public void update()
  {
    if ( remoteConfig != null )
    {
      setTitle( "RMIR - " + remoteConfig.getRemote().getName() );
      importFromWavMergeItem.setEnabled( remoteConfig.getRemote().supportWaveUpgrade() );
      exportToWavSubMenu.setEnabled( remoteConfig.getRemote().supportWaveUpgrade() );
      createSummaryItem.setEnabled( true );
    }
    else
    {
      setTitle( "RMIR" );
      importFromWavMergeItem.setEnabled( false );
      exportToWavSubMenu.setEnabled( false );
      createSummaryItem.setEnabled( false );
      return;
    }

    resetTabbedPanes();
    
    generalPanel.set( remoteConfig );
    keyMovePanel.set( remoteConfig );
    macroPanel.set( remoteConfig );
    specialFunctionPanel.set( remoteConfig );
    timedMacroPanel.set( remoteConfig );
    favScanPanel.set( remoteConfig );
    favoritesPanel.set( remoteConfig );
    devicePanel.set( remoteConfig );
    protocolPanel.set( remoteConfig );
    activityPanel.set( remoteConfig );
    segmentPanel.set( remoteConfig );
    learnedPanel.set( remoteConfig );

    Remote remote = remoteConfig.getRemote();
    codesAction.setEnabled( remote.getSetupCodes().size() > 0 );
    if ( codeSelectorDialog != null )
    {
      if ( codeSelectorDialog.isDisplayable() )
      {
        codeSelectorDialog.dispose();
      }
      codeSelectorDialog = null;
    }

    if ( rdfViewer != null )
    {
      if ( rdfViewer.isDisplayable() )
      {
        rdfViewer.dispose();
      }
      rdfViewer = null;
    }
    exportToWavUpgradesItem.setVisible( remote.getUpgradeAddress() != null );
    exportToWavTimedMacrosItem.setVisible( remote.getTimedMacroAddress() != null );
    exportToWavLearnedItem.setVisible( remote.getLearnedAddress() != null );

    updateUsage();

    rawDataPanel.set( remoteConfig );
  }
  
  private void resetTabbedPanes()
  {
    Remote remote = remoteConfig.getRemote();
    int index = checkTabbedPane( "Key Moves", keyMovePanel, remote.hasKeyMoveSupport(), 1 );
    index = checkTabbedPane( "Macros", macroPanel, remote.hasMacroSupport(), index );
    index = checkTabbedPane( "Special Functions", specialFunctionPanel, !remote.getSpecialProtocols().isEmpty(), index );
    index = checkTabbedPane( "Timed Macros", timedMacroPanel, remote.hasTimedMacroSupport(), index );
    index = checkTabbedPane( "Fav/Scan", favScanPanel, remote.hasFavKey() && !remote.hasFavorites() && !remote.isSSD(), index );
    index = checkTabbedPane( "Favorites", favoritesPanel, remote.hasFavorites(), index );
    index = checkTabbedPane( "Devices", devicePanel, true, index );
    index = checkTabbedPane( "Protocols", protocolPanel, remote.hasFreeProtocols(), index );
    index = checkTabbedPane( "Activities", activityPanel, remote.hasActivitySupport(), index );
    index = checkTabbedPane( "Learned Signals", learnedPanel, remote.hasLearnedSupport() && learnedPanel != null, index );
    index = checkTabbedPane( "Segments", segmentPanel, Boolean.parseBoolean( properties.getProperty( "ShowSegments", "false" ) ) &&  remote.getSegmentTypes() != null && !remote.isSSD(), index );
  }
  
  protected void refreshTabbedPanes( boolean reset )
  {
    if ( reset )
    {
      resetTabbedPanes();
    }

    for ( int i = 0; i < tabbedPane.getTabCount(); i++ )
      ((RMPanel) tabbedPane.getComponentAt( i )).refresh();
  }
  private int checkTabbedPane( String name, Component c, boolean test, int index )
  {
    return checkTabbedPane( name, c, test, index, null, true );
  }
  private int checkTabbedPane( String name, Component c, boolean test, int index, String tooltip, boolean enabled )
  {
    if ( c == null )
    {
      return index;
    }
    int tabIndex = getTabIndex( c );
    if ( test )
    {
      if ( tabIndex < 0 )
      {
        tabbedPane.insertTab( name, null, c, tooltip, index );
        tabbedPane.setEnabledAt( index, enabled );
      }
      index++;
    }
    else if ( tabIndex > 0 )
    {
      tabbedPane.remove( index );
    }
    return index;
  }

  private boolean updateUsage( JProgressBar bar, AddressRange range )
  {
    if ( range != null )
    {
      int used = range.getFreeStart() - range.getStart() + range.getEnd() - range.getFreeEnd();
      int available = range.getSize();
      bar.setMinimum( 0 );
      bar.setMaximum( available );
      bar.setValue( used );
      bar.setString( Integer.toString( available - used ) + " free" );
      if ( range == remoteConfig.getRemote().getDeviceUpgradeAddress() )
      {
        // Device Upgrade area is filled from top down, so freeEnd != end in normal use
        bar.setForeground( AQUAMARINE );
      }
      else
      {
        bar.setForeground( ( range.getFreeEnd() == range.getEnd() ) ? AQUAMARINE : Color.YELLOW );
      }

      return available >= used;
    }
    else
    {
      bar.setMinimum( 0 );
      bar.setMaximum( 0 );
      bar.setValue( 0 );
      bar.setString( "N/A" );
      bar.setForeground( AQUAMARINE );

      return true;
    }
  }
  
  private void updateBleStatus()
  {
    BLERemote rem = btio.getBleRemote();
    batteryBar.setBars( rem.batteryBars );
    batteryVoltage.setText( String.format( "(%4.2fv)", rem.batteryVoltage ) );
    int sigValue = rem.signalStrength == 1 ? signalProgressBar.getMinimum() : rem.signalStrength;
    String sigString = rem.signalStrength == 1 ? "N/A" : rem.signalStrength + "dBm";
    signalProgressBar.setValue( sigValue );
    signalProgressBar.setString( sigString );
  }

  /**
   * Updates the progress bars and returns a boolean specifying whether the configuration is valid, i.e. whether all
   * sections fit in their available space.
   */
  private boolean updateUsage()
  {
    boolean valid = true;
    Remote remote = remoteConfig.getRemote();
    Dimension d = advProgressBar.getPreferredSize();
    Font font = advProgressBar.getFont();
    bleStatus.setVisible( btio != null );
    if ( remoteConfig.hasSegments() )
    {
      extraStatus.setVisible( false );
      advProgressLabel.setText( "Memory usage:" );
    }
    else if ( remote.getDeviceUpgradeAddress() == null )
    {
      upgradeProgressBar.setVisible( false );
      upgradeProgressBar.setPreferredSize( d );
      upgradeProgressBar.setFont( font );
      upgradeProgressBar.setVisible( true );
      devUpgradeProgressBar.setVisible( false );
      extraStatus.setVisible( true );
      advProgressLabel.setText( "Move/Macro:" );
    }
    else
    {
      d.height /= 2;
      Font font2 = font.deriveFont( ( float )font.getSize() * 0.75f );
      upgradeProgressBar.setVisible( false );
      upgradeProgressBar.setPreferredSize( d );
      upgradeProgressBar.setFont( font2 );
      upgradeProgressBar.setVisible( true );
      devUpgradeProgressBar.setVisible( false );
      devUpgradeProgressBar.setPreferredSize( d );
      devUpgradeProgressBar.setFont( font2 );
      devUpgradeProgressBar.setVisible( true );
      extraStatus.setVisible( true );
      advProgressLabel.setText( "Move/Macro:" );
    }
    
    if ( btio != null && btio.getBleRemote() != null )
    {
      updateBleStatus();
    }

    String title = "Available Space Exceeded";
    String message = "";
    AddressRange range = remoteConfig.hasSegments() ? remote.getUsageRange() : remote.getAdvancedCodeAddress();
    if ( !updateUsage( advProgressBar, range ) )
    {
      valid = false;
      if ( range.getFreeEnd() == range.getEnd() )
      {
        message = "The defined advanced codes (keymoves, macros, special functions etc.) use more space than is available.  Please remove some.";
      }
      else
      {
        message = "There is insufficient space in the advanced codes section for both the defined\n"
            + "advanced codes (keymoves, macros, special functions etc.) and the device\n"
            + "upgrades that have overflowed from their own section.  Please remove some entries.";
      }
      showErrorMessage( message, title );
    }
    if ( !updateUsage( timedMacroPanel.timedMacroProgressBar, remote.getTimedMacroAddress() ) )
    {
      valid = false;
      message = "The defined timed macros use more space than is available.  Please remove some.";
      showErrorMessage( message, title );
    }
    if ( !updateUsage( upgradeProgressBar, remote.getUpgradeAddress() ) )
    {
      // Note that this section can only be full if there are no sections that can take overflow from it.
      // Otherwise, excessive device upgrades cause the overflow section, not this one, to be full.
      valid = false;
      message = "The defined device upgrades use more space than is available. Please remove some.";
      showErrorMessage( message, title );
    }
    if ( !updateUsage( devUpgradeProgressBar, remote.getDeviceUpgradeAddress() ) )
    {
      valid = false;
      message = "The defined button-dependent device upgrades use more space than is available. Please remove some.";
      showErrorMessage( message, title );
    }
    range = remote.getLearnedAddress();
    if ( !updateUsage( learnedProgressBar, range ) )
    {
      valid = false;
      if ( range.getFreeEnd() == range.getEnd() )
      {
        message = "The defined learned signals use more space than is available.  Please remove some.";
      }
      else
      {
        message = "There is insufficient space in the learned signals section for both the defined\n"
            + "learned signals and the device upgrades that have overflowed from their own\n"
            + "section.  Please remove some entries.";
      }
      showErrorMessage( message, title );
    }
    if ( btio != null && btio.getBleRemote() != null )
    {
      batteryBar.setBars( btio.getBleRemote().batteryBars );
    }
    return valid;
  }

  private void showErrorMessage( String message, String title )
  {
    JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
  }

  /**
   * Description of the Method.
   * 
   * @param event
   *          the event
   */
  public void propertyChange( PropertyChangeEvent event )
  {
    // No need to check unassigned upgrades as keymoves are saved in .rmir file
    // remoteConfig.checkUnassignedUpgrades();
    if ( currentPanel == keyMovePanel )
    {
      ( ( KeyMoveTableModel )keyMovePanel.getModel() ).resetKeyMoves();
    }
    if ( currentPanel == activityPanel && event.getPropertyName().equals( "tabs" ) )
    {
      activityPanel.set( remoteConfig );
    }
    if ( currentPanel == generalPanel &&  generalPanel.getCreateUpgradesButton().isVisible() )
    {
      generalPanel.getCreateUpgradesButton().setEnabled( remoteConfig.getCreatableMissingCodes() != null );
    }
    remoteConfig.updateImage();
    updateUsage();
    hasInvalidCodes = generalPanel.setWarning();
    if ( !event.getPropertyName().equals( "highlight" ) )
    {
      changed = true;
    }
    if ( currentPanel == generalPanel && remoteConfig.hasSegments() && remoteConfig.getRemote().hasSettings() )
    {
      // Changing settings requires consequential changes to the Device Buttons panel 
      // for remotes such as the URC7935
      generalPanel.repaint();
    }
  }

  private boolean allowSave( Remote.SetupValidation setupValidation )
  {
    if ( !hasInvalidCodes )
    {
      return true;
    }
    Remote remote = remoteConfig.getRemote();
    String title = "Setup configuration";
    if ( setupValidation == Remote.SetupValidation.WARN )
    {
      String message = "The current setup contains invalid device codes";
      if ( remote.usesEZRC() )
      {
        message += "\nor devices without a matching device upgrade";
      }      
      message +=  ".\n\nAre you sure you wish to continue?";
      return NegativeDefaultButtonJOptionPane.showConfirmDialog( this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE ) == JOptionPane.YES_OPTION;
    }
    else if ( setupValidation == Remote.SetupValidation.ENFORCE )
    {
      String message = "The current setup contains invalid device codes\n"
          + "which would cause this remote to malfunction.\n" + "Please correct these codes and try again.";
      JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
    }
    return false;
  }

  private static File getWritableFile( File workDir, String fileName )
  {
    File dir = workDir;
    File file = new File( dir, fileName );
    boolean canWrite = true;
    if ( !file.exists() )
    {
      try
      {
        PrintWriter pw = new PrintWriter( new FileWriter( file ) );
        pw.println( "Write Test" );
        pw.flush();
        pw.close();
        file.delete();
      }
      catch ( IOException ioe )
      {
        canWrite = false;
      }
    }
    else
    {
      canWrite = file.canWrite();
    }

    if ( !canWrite )
    {
      String baseFolderName = null;

      if ( System.getProperty( "os.name" ).startsWith( "Windows" )
          && Float.parseFloat( System.getProperty( "os.version" ) ) >= 6.0f )
      {
        baseFolderName = System.getenv( "APPDATA" );
      }
      if ( baseFolderName == null || "".equals( baseFolderName ) )
      {
        baseFolderName = System.getProperty( "user.home" );
      }

      dir = new File( baseFolderName, "RemoteMaster" );
      if ( !dir.exists() )
      {
        dir = new File( baseFolderName, ".RemoteMaster" );
        if ( !dir.exists() )
        {
          dir.mkdirs();
        }
      }
      file = new File( dir, fileName );
    }

    return file;
  }

  public static boolean isValidUpgradeSource( File f, Remote remote )
  {
    if ( remote == null )
      return false;
    if ( f.isDirectory() )
      return true;
    String name = f.getName();
    String prefix = "Sys" + remote.getSignature().substring( 3 ) + "_";
    if ( !name.startsWith( prefix ) || !name.endsWith( ".bin" ) )
      return false;
    String body = name.substring( 8, name.length() - 4 );
    if ( !body.matches( "\\d{6}(\\.\\d+)??" ) )
      return false;

    return true;
  }
  
  /**
   * 
   * Taken from http://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
   * Said there to work with Windows, Linux and Mac.
   */
  public static File getJarContainingFolder( Class<?> aclass ) throws Exception 
  {
    CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();

    File jarFile;

    if ( codeSource.getLocation() != null ) 
    {
      jarFile = new File(codeSource.getLocation().toURI());
    }
    else 
    {
      String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
      String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
      jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
      jarFile = new File(jarFilePath);
    }
    return jarFile.getParentFile();
  }

  /**
   * Description of the Method.
   * 
   * @param args
   *          the args
   */
  private static void createAndShowGUI( ArrayList< String > args )
  {
    try
    {
      workDir = getJarContainingFolder( RemoteMaster.class );
      rmirSys = new File( workDir, "RMIR.sys" );
      rmpbIcon = new File( workDir, "RMPB.ico" );
      summaryFile = new File( workDir, "summary.html" );
      LearnedSignal.getTmDecoder();
      File propertiesFile = null;
      File errorsFile = null;
      File fileToOpen = null;
      boolean launchRMIR = true;
      boolean launchPB = false;
      for ( int i = 0; i < args.size(); ++i )
      {
        String parm = args.get( i );
        if ( parm.equalsIgnoreCase( "-ir" ) )
        {
          launchRMIR = true;
        }
        else if ( parm.equalsIgnoreCase( "-rm" ) )
        {
          launchRMIR = false;
        }
        else if ( parm.equalsIgnoreCase( "-pb" ) )
        {
          if ( rmpbIcon.exists() )
          {
            launchRMIR = false;
            launchPB = true;
          }
        }
        else if ( parm.equalsIgnoreCase( "-admin" ) )
        {
          admin = true;
        }
        else if ( "-home".startsWith( parm ) )
        {
          String dirName = args.get( ++i );
          System.err.println( parm + " applies to \"" + dirName + '"' );
          workDir = new File( dirName );
        }
        else if ( "-properties".startsWith( parm ) )
        {
          String fileName = args.get( ++i );
          System.err.println( "Properties file name is \"" + fileName + '"' );
          propertiesFile = new File( fileName );
        }
        else if ( "-errors".startsWith( parm ) )
        {
          String fileName = args.get( ++i );
          System.err.println( "Errors file name is \"" + fileName + '"' );
          errorsFile = new File( fileName );
        }
        else
        {
          fileToOpen = new File( parm );
        }
      }
      
      if ( errorsFile == null )
      {
        errorsFile = getWritableFile( workDir, "rmaster.err" );
      }

      try
      {
        System.setErr( new PrintStream( new FileOutputStream( errorsFile ) ) );
      }
      catch ( Exception e )
      {
        e.printStackTrace( System.err );
      }

      System.err.println( "RemoteMaster " + RemoteMaster.version + " build " + getBuild() );
      System.err.println( "Legacy merge set = " + legacyMergeOK );
      String[] propertyNames =
      {
          "java.version", "java.vendor", "os.name", "os.arch", "java.home", "java.class.path"
      };

      System.err.println( "System Properties:" );
      for ( String name : propertyNames )
      {
        System.err.println( "   " + name + " = " + System.getProperty( name ) );
      }
      System.err.println();
      
      DynamicURLClassLoader.getInstance().addFile( workDir );

      FilenameFilter filter = new FilenameFilter()
      {
        public boolean accept( File dir, String name )
        {
          String temp = name.toLowerCase();
          return temp.endsWith( ".jar" ) && !temp.endsWith( "remotemaster.jar" ) && !temp.endsWith( "setup.jar" );
        }
      };

      File[] jarFiles = workDir.listFiles( filter );
      DynamicURLClassLoader.getInstance().addFiles( jarFiles );

      if ( propertiesFile == null )
      {
        propertiesFile = getWritableFile( workDir, "RemoteMaster.properties" );
      }
      PropertyFile properties = new PropertyFile( propertiesFile );

      String lookAndFeel = properties.getProperty( "LookAndFeel", UIManager.getSystemLookAndFeelClassName() );
      try
      {
        UIManager.setLookAndFeel( lookAndFeel );
        KeyMoveTableModel.normalSelectedBGColor = UIManager.getColor( "Table.selectionBackground" );
      }
      catch ( Exception ex )
      {
        ex.printStackTrace( System.err );
      }

      addonDir = properties.getFileProperty( "AddonPath", new File( workDir, "AddOns" ) );
      
      RemoteManager.getRemoteManager().loadRemotes( properties );

      ProtocolManager.getProtocolManager().load( new File( workDir, "protocols.ini" ), properties );
      System.err.println();

      DigitMaps.load( new File( workDir, "digitmaps.bin" ) );

      if ( launchRMIR )
      {
        RemoteMaster rm = new RemoteMaster( workDir, properties );
        if ( fileToOpen != null )
        {
          rm.openFile( fileToOpen );
        }
        frame = rm;
      }
      else if ( launchPB )
      {
        RMProtocolBuilder pb = new RMProtocolBuilder( properties );
        pb.loadProtocol( fileToOpen );
        frame = pb;
      }
      else
      {
        KeyMapMaster km = new KeyMapMaster( properties );
        km.loadUpgrade( fileToOpen );
        frame = km;
      }
    }
    catch ( Exception e )
    {
      System.err.println( "Caught exception in RemoteMaster.main()!" );
      e.printStackTrace( System.err );
      System.err.flush();
      System.exit( 0 );
    }
    System.err.flush();
  }

  public List< AssemblerItem > getClipBoardItems()
  {
    return clipBoardItems;
  }

  /**
   * The main program for the RemoteMaster class.
   * 
   * @param args
   *          the args
   */
  public static void main( String[] args )
  {
    java.lang.reflect.Method m;
    try
    {
      m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
      m.setAccessible(true);
      ClassLoader cl = ClassLoader.getSystemClassLoader();
      Object test1 = m.invoke(cl, "java.util.Arrays");
      legacyMergeOK = test1 == null;
      System.setProperty( "java.util.Arrays.useLegacyMergeSort", "true" );
    }
    catch ( Exception e ) {}
   
    JDialog.setDefaultLookAndFeelDecorated( true );
    JFrame.setDefaultLookAndFeelDecorated( true );
    Toolkit.getDefaultToolkit().setDynamicLayout( true );

    for ( String arg : args )
    {
      if ( "-version".startsWith( arg ) )
      {
        System.out.println( getFullVersion() );
        return;
      }
      else
      {
        parms.add( arg );
      }
    }
    javax.swing.SwingUtilities.invokeLater( new Runnable()
    {
      public void run()
      {
        createAndShowGUI( parms );
      }
    } );
  }

  /** The parms. */
  private static ArrayList< String > parms = new ArrayList< String >();
  
  private static File workDir = null;
  private static File rmirSys = null;
  private static File rmpbIcon = null;
  private static File addonDir = null;
  private static File summaryFile = null;
  private static File upgradeSource = null;
  private static LanguageDescriptor upgradeLanguage = defaultLanguage;
  private static int[] parsedWindowsVersion = null;

  public static File getWorkDir()
  {
    return workDir;
  }

  public static File getRmirSys()
  {
    return rmirSys;
  }

  public static File getSummaryFile()
  {
    return summaryFile;
  }

  public static File getAddonDir()
  {
    return addonDir;
  }

  public static File getUpgradeSource()
  {
    return upgradeSource;
  }

  public static void clearUpgradeSource()
  {
    RemoteMaster.upgradeSource = null;
  }

  public static LanguageDescriptor getUpgradeLanguage()
  {
    return upgradeLanguage;
  }

  private final static String[] rmirEndings =
  {
    ".rmir"
  };

  private final static String[] rmduEndings =
  {
    ".rmdu"
  };
  
  public final static String[] rmpbEndings =
    {
      ".rmpb"
    };

  private final static String[] irEndings =
  {
    ".ir"
  };
  
  private final static String[] binEndings =
    {
      ".bin"
    };

  /** The Constant txtEndings. */
  public final static String[] txtEndings =
  {
    ".txt"
  };

  private final static String[] slingEndings =
  {
    ".xml"
  };

  private final static String[] allEndings =
  {
      ".rmir", ".ir", ".rmdu", ".rmpb", ".txt", ".xml", ".bin"
  };
  
  private final static String[] allAdminEndings =
    {
        ".rmir", ".ir", ".rmdu", ".rmpb", ".txt", ".xml", ".bin", "ctl"
    };

  private final static String[] allMergeEndings =
  {
      ".hex", ".ir", ".txt"
  };

  private final static String[] extenderEndings =
  {
    ".hex"
  };
  
  private final static String[] wavEndings =
  {
    ".wav"
  };
  
  private final static String[] summaryEndings =
    {
      ".html"
    };

  private final static String[] otherMergeEndings =
  {
      ".ir", ".txt"
  };

  @Override
  public void stateChanged( ChangeEvent event )
  {
    finishEditing();
    RMPanel newPanel = ( RMPanel )tabbedPane.getSelectedComponent();
    if ( newPanel != currentPanel )
    {
      newPanel.set( remoteConfig );
      currentPanel = newPanel;
      highlightAction.setEnabled( false );
    }
    if ( codeSelectorDialog != null )
    {
      codeSelectorDialog.enableAssign( currentPanel == generalPanel );
    }
  }

  private int getTabIndex( Component c )
  {
    for ( int i = 0; i < tabbedPane.getTabCount(); i++ )
    {
      if ( tabbedPane.getComponentAt( i ).equals( c ) )
      {
        return i;
      }
    }
    return -1;
  }

  public RemoteConfiguration getRemoteConfiguration()
  {
    return remoteConfig;
  }

  public GeneralPanel getGeneralPanel()
  {
    return generalPanel;
  }

  public KeyMovePanel getKeyMovePanel()
  {
    return keyMovePanel;
  }

  public DeviceUpgradePanel getDeviceUpgradePanel()
  {
    return devicePanel;
  }
  
  public FavoritesPanel getFavoritesPanel()
  {
    return favoritesPanel;
  }

  public ActivityPanel getActivityPanel()
  {
    return activityPanel;
  }
  private boolean changed = false;

  public boolean isChanged()
  {
    return changed;
  }

  public void setChanged( boolean changed )
  {
    this.changed = changed;
  }

  public boolean promptToSave() throws IOException
  {
    return promptToSave( false );
  }
  
  public boolean promptToSave( boolean doExit ) throws IOException
  {
    if ( suppressConfirmPrompts.isSelected() || !changed )
    {
      return true;
    }
    int rc = JOptionPane.showConfirmDialog( this, "The data has changed.  Do you want to save\n"
        + "the current configuration before proceeding?", "Save upgrade?", JOptionPane.YES_NO_CANCEL_OPTION );
    if ( rc == JOptionPane.CANCEL_OPTION || rc == JOptionPane.CLOSED_OPTION )
    {
      return false;
    }
    if ( rc == JOptionPane.NO_OPTION )
    {
      return true;
    }
    exitPrompt = doExit;
    if ( saveAction.isEnabled() )
    {
      save( file, false );
    }
    else
    {
      saveAs();
    }
    return true;
  }
  
  private void finishEditing()
  {
    if ( currentPanel instanceof RMTablePanel< ? > )
    {
      RMTablePanel< ? > panel = ( RMTablePanel< ? > )currentPanel;
      panel.finishEditing();
    }
    else if ( currentPanel == generalPanel )
    {
      generalPanel.finishEditing();
    }
    else if ( currentPanel == activityPanel )
    {
      activityPanel.finishEditing();
    }
  }
  
  private void extractSS()
  {
    JPS io = binLoaded();
    if ( io == null )
    {
      return;
    }
    Scanner s = io.getScanner();
    if ( s == null )
    {
      String title = "Parsing error";
      String message = "Unable to interpret settings data.  Extraction failed.";
      JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
      return;
    }
    LinkedHashMap< Integer, List< Integer > > setups = new LinkedHashMap< Integer, List<Integer> >();
    LinkedHashMap< Integer, Integer > pidLenBytes = new LinkedHashMap< Integer, Integer >();
    LinkedHashMap< Integer, Integer > mapNumBytes = new LinkedHashMap< Integer, Integer >();
    List< Integer > prots = new ArrayList< Integer >();
    List< Integer > maps = new ArrayList< Integer >();
    short[] bufSetup = new short[ 4 * s.getSetupCodeCount() ];
    short[] bufType = new short[ 2 * s.getSetupTypeCount() + 2 ];
    short[] bufExec = new short[ 4 * s.getExecutorCount() ];
    short[] bufNum = new short[ 10 * s.getNumberTableSize() ];
    io.readRemote( s.getSetupCodeIndexAddress() + 2, bufSetup );
    io.readRemote( s.getSetupTypeIndexAddress(), bufType );
    io.readRemote( s.getExecutorIndexAddress() + 2, bufExec );
    io.readRemote( s.getNumberTableAddress(), bufNum );
    for ( int i = 0; i < s.getExecutorCount(); i++ )
    {
      int pid = bufExec[ 2 * i ] | bufExec[ 2 * i + 1 ] << 8;
      prots.add( pid );
      int n = 2 * ( s.getExecutorCount() + i );
      int protAddress = ( bufExec[ n ] | bufExec[ n + 1 ] << 8 ) + s.getIndexTablesOffset();
      short[] buf2 = new short[ 2 ];
      if ( io.readRemote( protAddress + 2, buf2 ) != 2 )
      {
        continue;
      }
      pidLenBytes.put( pid, ( int )buf2[ 0 ] );
    }

    int type = -1;
    int typeLimit = ( bufType[ 2 * type + 2 ] | bufType[ 2 * type + 3 ] << 8 ) * 2;
    int mask = s.setupCodeIncludesType() ? 0x0FFF : 0xFFFF;
    for ( int i = 0; i < s.getSetupCodeCount(); i++ )
    {
      int codeAddress = s.getSetupCodeIndexAddress() + 2 + 2 * i;
      if ( codeAddress == typeLimit )
      {
        type++;
        typeLimit = ( bufType[ 2 * type + 2 ] | bufType[ 2 * type + 3 ] << 8 ) * 2;
      }
      int setupCode = bufSetup[ 2 * i ] | bufSetup[ 2 * i + 1 ] << 8;
      List< Integer > codeList = setups.get( type );
      if ( codeList == null )
      {
        codeList = new ArrayList< Integer >();
        setups.put( type, codeList );
      }
      codeList.add( setupCode & mask );
      int n = 2 * ( s.getSetupCodeCount() + i );
      int setupAddress = ( bufSetup[ n ] | bufSetup[ n + 1 ] << 8 ) + s.getIndexTablesOffset();
      short[] buf = new short[ 4 ];
      io.readRemote( setupAddress, buf );
      int pid = buf[ 0 ] << 8 | buf[ 1 ];
      int map = buf[ 2 ];
      if ( map > 0 && pidLenBytes.get( pid ) != null )
      {
        mapNumBytes.put( map - 1, pidLenBytes.get( pid ) & 0x0F );
      }
    }
    int n = 0;
    while ( n < bufNum.length / 10 )
    {
      int numBytes = mapNumBytes.get( n ) != null ? mapNumBytes.get( n ) : 1;
      short[] digitKeyCodes = Arrays.copyOfRange( bufNum, 10 * n, 10 * ( n + numBytes ) );
      int m = DigitMaps.findDigitMapNumber( digitKeyCodes );
      for ( int j = 0; j < numBytes; j++ )
      {
        maps.add( m >= 0 ? m + j : -1 );
      }
      n += numBytes;
    }
    System.err.println();
    System.err.println( "DATA EXTRACT FOR RDF FOR SIGNATURE " + io.getRemoteSignature() + ":"  );
    System.err.println( String.format( "EepromSize=$%04X", io.getRemoteEepromSize() ) );
    System.err.println( String.format( "BaseAddress=$%04X", io.getRemoteEepromAddress() ) );
    printExtract( setups, pidLenBytes, prots, maps );
    System.err.println( "Raw number table data:" );
    n = 0;
    while ( n < bufNum.length / 10 )
    {
      int numBytes = mapNumBytes.get( n ) != null ? mapNumBytes.get( n ) : 1;
      for ( int i = 0; i < 10 * numBytes; i++ )
      {
        System.err.print( String.format( "%02x ", bufNum[ 10 * n + i ] ) );
      }
      System.err.println();
      n += numBytes;
    }
    System.err.println();
    String title = "Extract for RDF";
    String message = 
        "Extract data, including [Protocols] and [SetupCodes] sections\n"
        + "for the RDF, have been output to rmaster.err"; 
    JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
  }
  
  private void extractIrdb()
  {
    Remote remote = remoteConfig.getRemote();
    ZipFile zipfile = getSystemZipFile( remote );
    ZipEntry entry = null;
    byte[] data = null;
    if ( zipfile != null && ( entry = zipfile.getEntry( "irdb.bin" ) ) != null )
    {
      try
      {
        data = readBinary( zipfile.getInputStream( entry ), ( int )entry.getSize() );
        zipfile.close();
      }
      catch( Exception e )
      {
        entry = null;
      }
    }
    if ( entry == null )
    {
      String title = "Extract irdb.bin";
      String message = 
            "File irdb.bin not found so extract process has been aborted"; 
      JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
      return;
    }
    int pos = 0;
    LinkedHashMap< Integer, List< Integer > > setups = new LinkedHashMap< Integer, List<Integer> >();
    LinkedHashMap< Integer, Integer > pidLenBytes = new LinkedHashMap< Integer, Integer >();
    List< Integer > prots = new ArrayList< Integer >();
    List< Integer > distinctTags = new ArrayList< Integer >();
    String name = file.getName();
    List< String > tagNames = new ArrayList< String >();
    System.err.println( name + " tags:" );
    int itemsLength = ( data[ pos + 14 ] & 0xFF ) + 0x100 * ( data[ pos + 15 ] & 0xFF );
    pos += 16;
    int itemCount = data[ pos++ ] & 0xFF;
    int itemsEnd = pos + itemsLength;
    char ch;
    for ( int i = 0; i < itemCount; i++ )
    {
      StringBuilder sb = new StringBuilder();
      while ( ( ch = ( char )( data[ pos++ ] & 0xFF ) ) != 0 )
      {
        sb.append( ch );
      }
      String tag = Integer.toHexString( i );
      if ( tag.length() == 1 )
      {
        tag = "0" + tag;
      }
      String tagName = sb.toString();
      tagNames.add( tagName );
      System.err.println( "  " + tag + "  " + tagName );
    }
    if ( pos != itemsEnd )
    {
      System.err.println( "Parsing error in " + name );
      return;
    }

    List< Integer > tags = new ArrayList< Integer >();
    int devLen = 0;
    int cmdLen = 0;
    int pid = -1;
    while ( true )
    {
      int tag = data[ pos++ ] & 0xFF;
      if ( ( tag & 0x80 ) == 0 )
      {
        tags.add( 0, tag );
        if ( !distinctTags.contains( tag ) )
        {
          distinctTags.add( tag );
        }
        if ( tag == 0x0B )
        {
          int type = data[ pos + 1 ];
          char[] chs = new char[ 4 ];
          for ( int i = 2; i < 6; i++ )
          {
            chs[ i - 2 ] = ( char )data[ pos + i ];
          }
          int val = Integer.parseInt( new String( chs ) );
          List< Integer > list = setups.get( type );
          if ( list == null )
          {
            list = new ArrayList< Integer >();
            setups.put( type, list );
          }
          if ( !list.contains( val ) )
          {
            list.add( val );
          }
        }
        else if ( tag == 0x0D )
        {
          devLen = data[ pos ];
        }
        else if ( tag == 0x11 )
        {
          pid = ( data[ pos + 1 ] & 0xFF ) + 0x100 * ( data[ pos + 2 ] & 0xFF );
          if ( !prots.contains( pid ) )
          {
            prots.add( pid );
          }
        }
        else if ( tag == 0x10 )
        {
          if ( cmdLen == 0 )
          {
            cmdLen = data[ pos ] - 1;
          }
          else if ( cmdLen != data[ pos ] - 1 )
          {
            System.err.println( "Inconsistent cmdLen in pid = " + Integer.toHexString( pid ) );
          }
        }
        int len = data[ pos++ ] & 0xFF;
        pos += len;
      }
      else
      {
        int last = tags.remove( 0 );
        if ( tag != ( last | 0x80  ) )
        {
          System.err.println( "XCF file nesting error at " + Integer.toHexString( pos - 1 ) );
          return;
        }
        else if ( tag == 0x8B )
        {
          int val = 0xFFFF;
          if ( devLen < 16 && cmdLen < 16 )
          {
            val = cmdLen | ( devLen << 4 );
          }
          Integer oldVal = pidLenBytes.get( pid );
          if ( oldVal == null )
          {
            pidLenBytes.put( pid, val );
          }
          else if ( oldVal != val )
          {
            System.err.println( "Inconsistent occurrences of pid " + Integer.toHexString( pid ) );
          }
          devLen = 0;
          cmdLen = 0;
          pid = -1;
        }

        if ( tags.isEmpty() )
        {
          System.err.println( "irdb parsing terminating at position " + Integer.toHexString( pos ) );
          break;
        }
      }  
    }

    Collections.sort( distinctTags );
    System.err.println();
    System.err.print( "Distinct tags: " );
    for ( int tag : distinctTags )
    {
      System.err.print( String.format( "%02X ", tag ) );
    }
    System.err.println();
    System.err.println();
    printExtract( setups, pidLenBytes, prots, null );
    String title = "Extract irdb.bin";
    String message = 
        "Extract data, including [Protocols] and [SetupCodes] sections for the\n"
        + "RDF for " + remote.getName() + " have been output to rmaster.err"; 
    JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
  }
  
  private void printExtract( LinkedHashMap< Integer, List< Integer > > setups,
      LinkedHashMap< Integer, Integer > pidLenBytes, List< Integer > prots,
      List< Integer > maps )
  {
    Remote remote = remoteConfig.getRemote();
    System.err.println( "RDF data for " + remote.getName() + " follows:" );
    System.err.println();
    System.err.println( "[SetupCodes]");
    List< Integer > types = new ArrayList< Integer >( setups.keySet() );
    Collections.sort( types );
    Collections.sort( prots );
    for ( int type : types )
    {
      List< Integer > list = setups.get( type );
      Collections.sort( list );
      if ( type < 0x10)
      {
        System.err.print( type );
      }
      else
      {
        System.err.print( ( char )type );
      }
      System.err.print( " = " );
      int i = -1;
      for ( int val : list )
      {
        if ( ++i > 0 )
        {
          System.err.print( ", " );

          if ( ( i % 10 ) == 0 )
          {
            System.err.println();
            System.err.print( "    " );
          }
        }
        System.err.print( new SetupCode( val ) );
      }
      System.err.println();
    }
    if ( maps != null )
    {
      System.err.println();
      System.err.println( "[DigitMaps]");
      int i = -1;
      for ( int m : maps )
      {
        if ( ++i > 0 )
        {
          if ( ( i % 16 ) == 0 )
          {
            System.err.println();
          }
        }
        if ( m >= 0 )
        {
          System.err.print( String.format( "%03d ", m ) );
        }
        else
        {
          System.err.print( "??? " );
        }
      }
      System.err.println();
    }
    System.err.println();
    System.err.println( "[Protocols]" );
    int i = -1;
    for ( int p : prots )
    {
      if ( ++i > 0 )
      {
        System.err.print( ", " );

        if ( ( i % 10 ) == 0 )
        {
          System.err.println();
        }
      }
      System.err.print( String.format( "%04X", p ) );
    }
    System.err.println();
    System.err.println();
    i = -1;
    System.err.println( "Dev/Cmd lengths by protocol");
    for ( int p : prots )
    {
      if ( ++i > 0 )
      {
        System.err.print( "; " );

        if ( ( i % 8 ) == 0 )
        {
          System.err.println();
        }
      }
      System.err.print( String.format( "%04X %02X", p, pidLenBytes.get( p ) ) );
    }
    System.err.println();
    System.err.println();
  }
  
  public void resetSegmentPanel()
  {
    if ( segmentPanel != null )
    {
      segmentPanel.resetLastSorted();
    }
  }
  
  private int getRegisteredRemotes()
  {
    bleMap.clear();
    int n = 0;
    while( true )
    {
      String propName = "RegisteredBTRemotes." + n;
      String temp = properties.getProperty( propName );
      if ( temp == null )
        break;
      int namePos = temp.indexOf( "Name=" );
      int ueiNamePos = temp.indexOf( "UEIName=" );
      int addrPos = temp.indexOf( "Address=" );
      if ( namePos >= 0 && ueiNamePos >= namePos + 5 && addrPos > ueiNamePos + 8 )
      {
        String name = temp.substring( namePos + 5, ueiNamePos );
        String ueiName = temp.substring( ueiNamePos + 8, addrPos);
        String address = temp.substring( addrPos + 8 );
        BLERemote dev = new BLERemote( name, ueiName, address );
        dev.regIndex = n;
        bleMap.put( address, dev );
      }
      n++;
    }
    return n - 1;
  }
  
  public void disconnectBLE()
  {
    boolean forced = false;
    if ( btio != null )
    {
      // btio.disconnecting will be false unless this is called from 
      // receive_connection_disconnected(), which is when the remote has initiated
      // the disconnection.
      if ( !btio.isDisconnecting() )
        btio.disconnectUEI();
      else
        forced = true;
      btio.disconnectBLE();
      btio = null;
    }
    bluetoothButton.setSelected( false );
    String selectedInterface = properties.getProperty( "Interface" );
    if ( selectedInterface != null && selectedInterface.equals( "JP2BT" ) )
    {
      recreateToolbar();
      if ( forced )
      {
        String message = "Connection terminated by the connected remote.";
        JOptionPane.showMessageDialog( this, message, "Disconnection", JOptionPane.INFORMATION_MESSAGE );
      }
    }
  }
  
  private static int[] parseVersion( String version )
  {
    int[] parsedVersion = new int[ 3 ];
    StringTokenizer st = new StringTokenizer( version.trim(), ". " );
    for ( int i = 0; i < 3; i++ )
    {
      if ( st.hasMoreTokens() )
        parsedVersion[ i ] = Integer.parseInt( st.nextToken() );
      else
        return null;
    }
    return parsedVersion;
  }
  
  public static boolean testWindowsVersion( String base )
  {
    Runtime rt;
    Process pr;
    BufferedReader in;
    String line = "";
    String fullVersion = "";
    int[] parsedBase = null;
    final String SEARCH_TERM = "OS Version:";

    String osName = System.getProperty( "os.name" );
    if ( !osName.startsWith( "Windows" ) )
      return false;
    if ( parsedWindowsVersion == null )
    {
      try
      {
        rt = Runtime.getRuntime();
        pr = rt.exec( "SYSTEMINFO" );
        in = new BufferedReader( new InputStreamReader( pr.getInputStream( ) ) );

        while( ( line = in.readLine() ) != null )
        {
          if( line.contains( SEARCH_TERM ) )
          {
            fullVersion = line.substring( line.lastIndexOf( SEARCH_TERM ) 
                + SEARCH_TERM.length(), line.length()-1) ;
            break;
          } 
        }
      }
      catch( IOException ioe )      
      {   
        System.err.println( ioe.getMessage() );
        return false;
      }
      parsedWindowsVersion = parseVersion( fullVersion.trim() );
    }
    
    parsedBase = parseVersion( base );
    if ( parsedWindowsVersion == null || parsedBase == null )
      return false;
    for ( int i = 0; i < 3; i++ )
    {
      if ( parsedWindowsVersion[ i ] < parsedBase[ i ] )
        return false;
      else if ( parsedWindowsVersion[ i ] > parsedBase[ i ] )
        return true;
    }
    return true;
  }

}
