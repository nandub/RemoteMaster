package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.hifiremote.jp1.AssemblerTableModel.DisasmState;
import com.hifiremote.jp1.ProtocolDataPanel.Mode;
import com.hifiremote.jp1.JP2Analyzer;

public class RMProtocolBuilder extends JP1Frame implements ActionListener,
ChangeListener
{
  public RMProtocolBuilder( PropertyFile properties )
  {
    super( "RM Protocol Builder", properties );
    System.err.println( "RMProtocolBuilder opening" );
    me = this;
    setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
    
    addWindowListener( new WindowAdapter()
    {
      @Override
      public void windowClosing( WindowEvent event )
      {
        boolean doDispose = true;
        try
        {
          System.err.println( "RMProtocolBuilder.windowClosing() entered" );
          if ( !promptToSave( true ) || exitPrompt )
          {
            System.err.println( "RMProtocolBuilder.windowClosing() exited" );
            doDispose = false;
            return;
          }
          System.err.println( "RMProtocolBuilder.windowClosing() continuing" );
//          ProtocolManager.getProtocolManager().setAltPIDRemoteProperties( properties );
//          Remote remote = getRemote();
//          if ( remote != null )
//          {
//            preferences.setLastRemoteName( remote.getName() );
//            preferences.setLastRemoteSignature( remote.getSignature() );
//          }
          savePreferences();
          me = null;
        }
        catch ( Exception e )
        {
          System.err.println( "RMProtocolBuilder.windowClosing() caught an exception!" );
          e.printStackTrace( System.err );
        }
        finally
        {
          if ( doDispose )
          {
            dispose();
          }
        }
      }
    } );
    
    
    toolBar = new JToolBar();
    toolBar.setFloatable( false );
    createMenus();
    createToolbar();

    add( toolBar, BorderLayout.PAGE_START );
//    JPanel mainPanel = new JPanel( new BorderLayout() );
//    add( mainPanel, BorderLayout.CENTER );
    preferences.load( recentFileMenu, "RecentProtocols", this );
    
    clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    
    ManualProtocol mp = new ManualProtocol( null, null );
    
    ManualEditorPanel editorPanel = new ManualEditorPanel( this );
    add( editorPanel, BorderLayout.CENTER );
    
    manualSettingsPanel = editorPanel.getManualSettingsPanel();
    manualSettingsPanel.setLoadInProgress( true );
    manualSettingsPanel.setProtocol( mp, true );
    manualSettingsPanel.getProtocolName().setText( null );
    manualSettingsPanel.getAssemblerPanel().setRmpbSaveAsAction( saveAsAction );
    tabbedPane = editorPanel.getTabbedPane();
    deviceText = manualSettingsPanel.getDeviceText();
    tabbedPane.addTab( "Output Data", manualSettingsPanel.getOutputPanel() );
    
    String note = "Code imported from protocols.ini with the Import button on the toolbar "
        + "(or the Protocol > Import... menu item) is initially shown in GRAY.  To complete "
        + "the import process, select the entries you wish to import into the Assembler and "
        + "press the \"Import to Assembler\" button.  The form of the import is determined "
        + "by the settings of the \"Use predefined constants\" check boxes.\n\n"
        + "Code in BLACK is already imported to, or created by, the Assembler.\n"
        + "All entries can be edited or created directly as hex data by double-clicking.";
    manualSettingsPanel.getTablePanel().getNoteArea().setText( note );
    
    procBox = manualSettingsPanel.getProcBox();
       
    protNames.addActionListener( this );
    protList.setRenderer( protListRenderer );
    
    String[] names = protocolManager.getNames().toArray( new String[ 0 ] );
    protNames.setModel( new DefaultComboBoxModel< String >( names ) );
    List< Protocol > list = protocolManager.getByName().get( names[ 0 ] );
    if ( list != null && list.size() > 0 )
    {
      protList.setModel( new DefaultComboBoxModel< Protocol >( list.toArray( new Protocol[0]) ) );
    }    

    pack();
    Rectangle bounds = preferences.getPBBounds();
    procBox.setSelectedIndex( 0 );
    manualSettingsPanel.setMode( Mode.ASM );
    tabbedPane.addChangeListener( this );
    manualSettingsPanel.setLoadInProgress( false );
    if ( bounds != null )
    {
      setBounds( bounds );
    }
    setVisible( true );
  }
  
  public static ListCellRenderer<? super Protocol > protListRenderer = new DefaultListCellRenderer()
  {
    @Override
    public Component getListCellRendererComponent( JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus )
    {
      String text = null;
      if ( value instanceof Protocol )
      {
        Protocol p = ( Protocol )value;
        text = p.getID() != null ? p.getID().toString() : "<unset>";
        String varName = p.getVariantName();
        if ( varName != null && !varName.trim().isEmpty() )
        {
          text += " (" + varName + ")";
        }
      }
      return super.getListCellRendererComponent( list, text, index, isSelected, cellHasFocus );
    };
  };
  
  public class PBAction extends AbstractAction
  {
    public PBAction( String text, String action, ImageIcon icon, String description, Integer mnemonic )
    {
      super( text, icon );
      putValue( ACTION_COMMAND_KEY, action );
      putValue( SHORT_DESCRIPTION, description );
      putValue( MNEMONIC_KEY, mnemonic );
    }

    @Override
    public void actionPerformed( ActionEvent event )
    {
      // TODO Auto-generated method stub
      try
      {
        String command = event.getActionCommand();
        if ( command.equals( "NEW" ) )
        {
          if ( !promptToSave() )
          {
            return;
          }
          reset();
        }
        else if ( command.equals( "IMPORT" ) )
        {
          if ( !promptToSave() )
          {
            return;
          }
          Box box = Box.createVerticalBox();
          JPanel panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
          panel.add( new JLabel( "Select name:") );
          box.add( panel );
          box.add( protNames );
          panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
          panel.add( new JLabel( "Select by PID & variant name:") );
          box.add( panel );
          box.add( protList );
          String title = "Import Protocol";
          int result = JOptionPane.showConfirmDialog( RMProtocolBuilder.this, box, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );
          if ( result == JOptionPane.OK_OPTION )
          {
            manualSettingsPanel.setLoadInProgress( true );
            Protocol protocol = ( Protocol )protList.getSelectedItem();
            ManualProtocol mp = protocol.convertToManual( null, null, null );
//            for ( String key : protocol.getCode().keySet() )
//            {
//              mp.getCode().put( key, new Hex( protocol.getCode().get( key ) ) );
//            }
            mp.setName( protocol.getName() );
            mp.setVariantName( protocol.getVariantName() );
            Processor proc = null;
            for ( Processor pr : ManualCodePanel.getProcs() )
            {
              if ( protocol.getCode().get( pr.getEquivalentName() ) != null )
              {
                proc = pr;
                break;
              }
            }
            reset();
            manualSettingsPanel.setDisplayProtocol( protocol );
            manualSettingsPanel.setProtocol( mp, true );
//            procBox.setSelectedItem( proc );
//            // The action listener on procBox will now run
            deviceText.setText( manualSettingsPanel.getProtocolText( true, false ) );
            int ndx = tabbedPane.indexOfTab( "Hex Code" );
            tabbedPane.setSelectedIndex( ndx );
            manualSettingsPanel.setLoadInProgress( false );
          }
        }
        else if ( command.equals( "EXPORT" ) )
        {
          manualSettingsPanel.getOutputPanel().updatePBOutput();
          manualSettingsPanel.writeProtFile( manualSettingsPanel.getOutputPanel().getRmpbText().getText() );
        }
        else if ( command.equals( "OPEN" ) )
        {
          //      ProtocolManager.getProtocolManager().reset();
          File file = getProtocolFile( RMProtocolBuilder.this, false );
          try
          {
            loadProtocol( file );
          }
          catch ( Exception e1 )
          {
            e1.printStackTrace();
          }
        }
        else if ( command.equals( "SAVE" ) )
        {
          manualSettingsPanel.saveRMPB();
        }
        else if ( command.equals( "SAVEAS" ) )
        {
          saveAs();
        }
        else if ( command.equals( "COPYTOCLIPBOARD" ) )
        {
          manualSettingsPanel.getOutputPanel().updatePBOutput();
          String text = manualSettingsPanel.getOutputPanel().getPbText().getText();
          StringSelection data = new StringSelection( text );
          clipboard.setContents( data, data );
        }
        else if ( command.equals( "PASTEFROMCLIPBOARD" ) )
        {
          manualSettingsPanel.importFromClipboard();
        }
      }
      catch ( Exception ex )
      {
        ex.printStackTrace( System.err );
      }
    }
  }

  
  private void createMenus()
  {
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar( menuBar );
    JMenu menu = new JMenu( "File" );
    menu.setMnemonic( KeyEvent.VK_F );
    menuBar.add( menu );
    
    newAction = new PBAction( "New", "NEW", RemoteMaster.createIcon( "RMNew24" ), "Create new protocol", KeyEvent.VK_N );
    menu.add( newAction ).setIcon( null );

    openAction = new PBAction( "Open...", "OPEN", RemoteMaster.createIcon( "RMOpen24" ), "Open a file", KeyEvent.VK_O );
    menu.add( openAction ).setIcon( null );

    saveAction = new PBAction( "Save", "SAVE", RemoteMaster.createIcon( "Save24" ), "Save to file", KeyEvent.VK_S );
    saveAction.setEnabled( false );
    menu.add( saveAction ).setIcon( null );

    saveAsAction = new PBAction( "Save as...", "SAVEAS", RemoteMaster.createIcon( "SaveAs24" ), "Save to a different file",
        KeyEvent.VK_A );
    saveAsAction.setEnabled( false );
    JMenuItem menuItem = menu.add( saveAsAction );
    menuItem.setDisplayedMnemonicIndex( 5 );
    menuItem.setIcon( null );

    menu.addSeparator();
    recentFileMenu = new JMenu( "Recent" );
    recentFileMenu.setMnemonic( KeyEvent.VK_R );
    recentFileMenu.setEnabled( false );
    menu.add( recentFileMenu );
    menu.addSeparator();

    exitItem = new JMenuItem( "Exit" );
    exitItem.setMnemonic( KeyEvent.VK_X );
    exitItem.addActionListener( this );
    menu.add( exitItem );
    
    menu = new JMenu( "Protocol" );
    menu.setMnemonic( KeyEvent.VK_P );
    menuBar.add( menu );
    
    importAction = new PBAction( "Import...", "IMPORT", RemoteMaster.createIcon( "Import24" ),
        "Import from protocols.ini", KeyEvent.VK_I );
    menu.add( importAction ).setIcon( null );

    exportAction = new PBAction( "Export...", "EXPORT", RemoteMaster.createIcon( "Export24" ),
        "Export as protocols.ini add-on", KeyEvent.VK_E );
//    exportAction.setEnabled( false );
    menu.add( exportAction ).setIcon( null );
    menu.addSeparator();
    
    copyToClipboardAction = new PBAction( "Copy to clipboard", "COPYTOCLIPBOARD", RemoteMaster.createIcon( "Copy24" ),
        "Copy to clipboard in PB-style format", KeyEvent.VK_D );
    menu.add( copyToClipboardAction ).setIcon( null );
    
    pasteFromClipboardAction = new PBAction( "Paste from clipboard...", "PASTEFROMCLIPBOARD", RemoteMaster.createIcon( "Paste24" ),
        "Paste from clipboard in PB-style format", KeyEvent.VK_P );
    menu.add( pasteFromClipboardAction ).setIcon( null );
    menu.addSeparator();
    
    JMenu analysesSubMenu = new JMenu( "Analyze to file..." );
    analysesSubMenu.setMnemonic( KeyEvent.VK_A );
    analysesSubMenu.setToolTipText( 
        "<html>Output to text file an analysis of all executors in protocols.ini<br>"
        + "for a chosen JP2/JP3 processor</html>" );
    menu.add( analysesSubMenu );
    
    allMAXQItem = new JMenuItem( "All MAXQ protocols" );
    allMAXQItem.setMnemonic( KeyEvent.VK_M );
    allMAXQItem.setToolTipText( "Analyze to MAXQAnalyses.txt all MAXQ executors in protocols.ini" );
    allMAXQItem.addActionListener( this );
    analysesSubMenu.add( allMAXQItem );
    
    allTI2541Item = new JMenuItem( "All TI2541 protocols" );
    allTI2541Item.setMnemonic( KeyEvent.VK_T );
    allTI2541Item.setToolTipText( "Analyze to TI2541Analyses.txt all TI2541 executors in protocols.ini" );
    allTI2541Item.addActionListener( this );
    analysesSubMenu.add( allTI2541Item );
    
    menu = new JMenu( "Options" );
    menu.setMnemonic( KeyEvent.VK_O );
    menuBar.add( menu );

    JMenu submenu = new JMenu( "Look and Feel" );
    submenu.setMnemonic( KeyEvent.VK_L );
    menu.add( submenu );

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
              if ( JOptionPane.showConfirmDialog( RMProtocolBuilder.this, message, title, JOptionPane.OK_CANCEL_OPTION, 
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
              preferences.setLookAndFeel( lf );
              SwingUtilities.updateComponentTreeUI( me );
              preferences.setLookAndFeel( lf );
            }
            catch ( Exception x )
            {
              x.printStackTrace( System.err );
            }
          }
        } );
      }
    };

    ButtonGroup group = new ButtonGroup();
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
      submenu.add( item );
      if ( item.getActionCommand().equals( lookAndFeel ) )
      {
        item.setSelected( true );
      }
      item.addActionListener( al );
    }

    
    
    menu = new JMenu( "Help" );
    menu.setMnemonic( KeyEvent.VK_H );
    menuBar.add( menu );

    if ( Desktop.isDesktopSupported() )
    {
      desktop = Desktop.getDesktop();

      rmpbReadmeItem = new JMenuItem( "Using RMPB", KeyEvent.VK_U );
      rmpbReadmeItem.addActionListener( this );
      menu.add( rmpbReadmeItem );

      menu.addSeparator();

      homePageItem = new JMenuItem( "Home Page", KeyEvent.VK_H );
      homePageItem.addActionListener( this );
      menu.add( homePageItem );

      forumItem = new JMenuItem( "Forums", KeyEvent.VK_F );
      forumItem.addActionListener( this );
      menu.add( forumItem );

      wikiItem = new JMenuItem( "Wiki", KeyEvent.VK_W );
      wikiItem.addActionListener( this );
      menu.add( wikiItem );

      menu.addSeparator();
    }

    updateItem = new JMenuItem( "Check for updates", KeyEvent.VK_C );
    updateItem.addActionListener( this );
    menu.add( updateItem );

    aboutItem = new JMenuItem( "About..." );
    aboutItem.setMnemonic( KeyEvent.VK_A );
    aboutItem.addActionListener( this );
    menu.add( aboutItem );
  }
  
  private void createToolbar()
  {
    toolBar.add( newAction );
    toolBar.add( openAction );
    toolBar.add( saveAction );
    toolBar.add( saveAsAction );
    toolBar.addSeparator();
    toolBar.add( importAction );
    toolBar.add( exportAction );
    toolBar.addSeparator();
    toolBar.add( copyToClipboardAction );
    toolBar.add( pasteFromClipboardAction );
  }
  
  public ManualSettingsPanel getManualSettingsPanel()
  {
    return manualSettingsPanel;
  }

  @Override
  public void actionPerformed( ActionEvent e )
  {
    try
    {
      Object source = e.getSource();
      if ( source == exitItem )
      {
        dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
      }
      else if ( source == protNames ) 
      {
        String name = ( String )protNames.getSelectedItem();
        List< Protocol > list = protocolManager.getByName().get( name );
        if ( list != null && list.size() > 0 )
        {
          protList.setModel( new DefaultComboBoxModel< Protocol >( list.toArray( new Protocol[0]) ) );
        }
        else
        {
          protList.setModel( new DefaultComboBoxModel< Protocol >() );
        }
      }
      else if ( source == rmpbReadmeItem )
      {
        File rmpbReadme = new File( RemoteMaster.getWorkDir(), "RMPB_Readme.html" );
        desktop.browse( rmpbReadme.toURI() );
      }
      else if ( source == homePageItem )
      {
        URL url = new URL( "http://controlremote.sourceforge.net/" );
        desktop.browse( url.toURI() );
      }
      else if ( source == forumItem )
      {
        URL url = new URL( "http://www.hifi-remote.com/forums/" );
        desktop.browse( url.toURI() );
      }
      else if ( source == wikiItem )
      {
        URL url = new URL( "http://www.hifi-remote.com/wiki/index.php?title=Main_Page" );
        desktop.browse( url.toURI() );
      }
      else if ( source == updateItem )
      {
        UpdateChecker.checkUpdateAvailable( this );
      }
      else if ( source == aboutItem )
      {
        String text = "<html><b>RM Protocol Builder, "
            + RemoteMaster.version
            + " build " + RemoteMaster.getBuild()
            + "</b>"
            + "<p>Get the latest version at <a href=\"http://controlremote.sourceforge.net\">http://controlremote.sourceforge.net</a></p>"
            + "<p>Java version "
            + System.getProperty( "java.version" )
            + " from "
            + System.getProperty( "java.vendor" )
            + "</p>"
            + "<p>Home directory is <b>"
            + RemoteMaster.getWorkDir()
            + "</b></p>"
            + "<p>Add-Ons directory is <b>"
            + RemoteMaster.getAddonDir()
            + "</b></p>"
            + "<p>This application within the overall RemoteMaster program<br>"
            + "was written primarily by Graham&nbsp;Dixon, loosely based on the<br>"
            + "Excel-based Protocol Builder program by John Fine, Mark Pierson<br>"
            + "and others that is currently maintained by Mike England.</p>"
            + "<p>RemoteMaster itself was written primarily by Greg Bush with<br>"
            + "substantial additions and help from Graham&nbsp;Dixon</p>"
            + "<p>Other contributors to RemoteMaster include:<blockquote>"
            + "John&nbsp;S&nbsp;Fine, Nils&nbsp;Ekberg, Jon&nbsp;Armstrong, Robert&nbsp;Crowe, "
            + "Mark&nbsp;Pauker, Mark&nbsp;Pierson, Mike&nbsp;England</blockquote></html>";

        JEditorPane pane = new JEditorPane( "text/html", text );
        pane.addHyperlinkListener( this );
        pane.setEditable( false );
        pane.setBackground( getContentPane().getBackground() );
        new TextPopupMenu( pane );
        JScrollPane scroll = new JScrollPane( pane );
        Dimension d = pane.getPreferredSize();
        d.height = d.height * 5 / 4;
        d.width = d.width * 2 / 3;
        scroll.setPreferredSize( d );

        JOptionPane.showMessageDialog( this, scroll, "About RemoteMaster", JOptionPane.INFORMATION_MESSAGE );
      }
      else if ( source == allMAXQItem )
      {
        Processor p = ProcessorManager.getProcessor( "MAXQ610" );
        JP2Analyzer jp2 = new JP2Analyzer();
        jp2.analyze( p, "MAXQanalyses.txt" );
        String message = "<html>Analysis of all MAXQ protocols has been written to the file \"MAXQanalyses.txt\"<br>"
            + "in the RMIR installation folder.</html>";
        JOptionPane.showMessageDialog( this, message, "Analysis complete", JOptionPane.PLAIN_MESSAGE );
      }
      else if ( source == allTI2541Item )
      {
        Processor p = ProcessorManager.getProcessor( "TI2541" );
        JP2Analyzer jp2 = new JP2Analyzer();
        jp2.analyze( p, "TI2541analyses.txt" );
        String message = "<html>Analysis of all TI2541 protocols has been written to the file \"TI2541analyses.txt\"<br>"
            + "in the RMIR installation folder.</html>";
        JOptionPane.showMessageDialog( this, message, "Analysis complete", JOptionPane.PLAIN_MESSAGE );
      }
      else
        // must be a recent file
      {
        JMenuItem item = ( JMenuItem )source;
        File f = new File( item.getText() );
        try
        {
          loadProtocol( f );
        }
        catch ( Exception e1 )
        {
          e1.printStackTrace();
        }
      }
    }
    catch ( Exception ex )
    {
      ex.printStackTrace( System.err );
    }
  }
  
  private void saveAs()
  {
    File f = manualSettingsPanel.saveAs();
    saveAction.setEnabled( f != null );
    try
    {
      if ( f != null )
      {
        setTitle( "RM Protocol Builder: " + f.getCanonicalPath() );
      }
    }
    catch ( Exception ex )
    {
      ex.printStackTrace( System.err );
    }
  }
  
  public static File getProtocolFile( Component parent, boolean rmpbOnly )
  {
    File path = preferences.getProtocolPath();
    File file = null;
    RMFileChooser chooser = new RMFileChooser( path );
    try
    {
      chooser.setAcceptAllFileFilterUsed( false );
    }
    catch ( Exception e )
    {
      e.printStackTrace( System.err );
    }
    EndingFileFilter filter = null;
    if ( rmpbOnly )
    {
      filter = new EndingFileFilter( "RMPB files", RemoteMaster.rmpbEndings );
      chooser.addChoosableFileFilter( filter );
    }
    else
    {
      filter = new EndingFileFilter( "All protocol files", anyEndings );
      chooser.addChoosableFileFilter( filter );
      chooser.addChoosableFileFilter( new EndingFileFilter( "RMPB files", RemoteMaster.rmpbEndings ) );
      chooser.addChoosableFileFilter( new EndingFileFilter( "Excel PB files", pbEndings ) );
    }
    chooser.setFileFilter( filter );

    int returnVal = chooser.showOpenDialog( parent );
    if ( returnVal == RMFileChooser.APPROVE_OPTION )
    {
      file = chooser.getSelectedFile();

      if ( !file.exists() )
      {
        JOptionPane.showMessageDialog( parent, file.getName() + " doesn't exist.", "File doesn't exist.",
            JOptionPane.ERROR_MESSAGE );
      }
      else if ( file.isDirectory() )
      {
        JOptionPane.showMessageDialog( parent, file.getName() + " is a directory.", "File doesn't exist.",
            JOptionPane.ERROR_MESSAGE );
      }
      else
      {
        preferences.setProtocolPath( file.getParentFile() );
      }
    }
    return file;
  }
  
  public void loadProtocol( File file ) throws Exception
  { 
    if ( file == null || !file.exists() )
    {
      return;
    }

    System.err.println( "Opening " + file.getCanonicalPath() + ", last modified "
        + DateFormat.getInstance().format( new Date( file.lastModified() ) ) );

    if ( !promptToSave() )
    {
      return;
    }
    
    boolean isRMPB = file.getName().toLowerCase().endsWith( ".rmpb" );
    boolean isTxt = file.getName().toLowerCase().endsWith( ".txt" );
    reset();
    if ( isRMPB )
    {
      manualSettingsPanel.loadRMPB( file, false );
      saveAction.setEnabled( true );
    }
    else if ( isTxt )
    {
      manualSettingsPanel.loadPB( file );
    }
    saveAsAction.setEnabled( true );
    
    if ( isRMPB || isTxt )
    {
      updateRecentFiles( file );
      setTitle( "RM Protocol Builder: " + file.getCanonicalPath() );
    }
    else
    {
      setTitle( "RM Protocol Builder" );
    }
    preferences.setProtocolPath( file.getParentFile() );
  }
  
  private void updateRecentFiles( File file ) throws IOException
  {
    boolean isRMPB = file.getName().toLowerCase().endsWith( ".rmpb" );
    boolean isTxt = file.getName().toLowerCase().endsWith( ".txt" );

    if ( isRMPB || isTxt )
    {
      int i = recentFileMenu.getItemCount() - 1;
      while ( i >= 0 )
      {
        JMenuItem item = recentFileMenu.getItem( i );
        File f = new File( item.getText() );
        if ( f.getCanonicalPath().equals( file.getCanonicalPath() ) )
        {
          recentFileMenu.remove( i );
        }
        --i;
      }
      i = recentFileMenu.getItemCount();
      while ( i > 9 )
      {
        recentFileMenu.remove( --i );
      }

      JMenuItem item = new JMenuItem( file.getAbsolutePath() );
      item.addActionListener( this );
      recentFileMenu.add( item, 0 );

      recentFileMenu.setEnabled( true );
    }
  }
  
  private void savePreferences() throws Exception
  {
    int state = getExtendedState();
    if ( state != Frame.NORMAL )
    {
      setExtendedState( Frame.NORMAL );
    }
    preferences.setPBBounds( getBounds() );
    preferences.save( recentFileMenu, "RecentProtocols" );
  }
  
  private void reset()
  {
    manualSettingsPanel.reset();
    saveAction.setEnabled( false );
    saveAsAction.setEnabled( false );
  }
  
  public boolean promptToSave() throws IOException
  {
    return promptToSave( false );
  }
  
  public boolean promptToSave( boolean doExit )
  { 
    if ( /* suppressConfirmPrompts.isSelected() || */ !manualSettingsPanel.isChanged() )
    {
      return true;
    }
    int rc = JOptionPane.showConfirmDialog( this, "The data has changed.  Do you want to save\n"
        + "the current protocol before proceeding?", "Save protocol?", JOptionPane.YES_NO_CANCEL_OPTION );
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
      manualSettingsPanel.saveRMPB();
    }
    else
    {
      saveAs();
    }
    return true;
  }
  
  private static RMProtocolBuilder me = null;
  private boolean exitPrompt = false;
  private JMenuItem exitItem = null;
  private JRadioButtonMenuItem[] lookAndFeelItems = null;
  private ActionEvent lfEvent = null;
  private JTabbedPane tabbedPane = null;
  private JSplitPane outerPane = null;
  private JTextArea deviceText = null;
  private JComboBox< String > protNames = new JComboBox< String >();
  private JComboBox< Protocol > protList = new JComboBox< Protocol >();
  private ProtocolManager protocolManager = ProtocolManager.getProtocolManager();
  private ManualSettingsPanel manualSettingsPanel = null;
  private JComboBox< Processor > procBox = new JComboBox< Processor >();
//  private JButton translationButton = null;
  private JMenu recentFileMenu = null;
  
  private JMenuItem rmpbReadmeItem = null;
  private JMenuItem homePageItem = null;
  private JMenuItem forumItem = null;
  private JMenuItem wikiItem = null;
  private JMenuItem aboutItem = null;
  private JMenuItem updateItem = null;
  private JMenuItem allMAXQItem = null;
  private JMenuItem allTI2541Item = null;
  
  private JToolBar toolBar = null;
  private PBAction newAction = null;
  private PBAction openAction= null;
  private PBAction saveAction= null;
  private PBAction saveAsAction= null;
  private PBAction importAction= null;
  private PBAction exportAction= null;
  private Clipboard clipboard = null;
  private PBAction copyToClipboardAction= null;
  private PBAction pasteFromClipboardAction= null;

  /** For help */
  private Desktop desktop = null;
  
  private final static String[] anyEndings =
    {
    ".txt", ".rmpb"
    };

  private final static String[] pbEndings =
    {
    ".txt"
    };

  @Override
  public void stateChanged( ChangeEvent e )
  {
    int indexAsm = tabbedPane.indexOfTab( "Assembler" );
    int indexDisasm = tabbedPane.indexOfTab( "Disassembler" );
    int indexOutput = tabbedPane.indexOfTab( "Output Data" );
    int index = tabbedPane.getSelectedIndex();
    if ( index < 0 )
    {
      return;
    }
    if ( index == indexAsm && indexDisasm >= 0 )
    {
      tabbedPane.setComponentAt( indexDisasm, null );
      tabbedPane.setComponentAt( indexAsm, manualSettingsPanel );
      manualSettingsPanel.setMode( Mode.ASM );
      manualSettingsPanel.getAssemblerPanel().optionsPanel.setVisible( false );
    }
    else if ( index == indexDisasm && indexAsm >= 0 )
    {
      tabbedPane.setComponentAt( indexAsm, null );
      tabbedPane.setComponentAt( indexDisasm, manualSettingsPanel );
      manualSettingsPanel.setMode( Mode.DISASM );
      manualSettingsPanel.getAssemblerPanel().optionsPanel.setVisible( true );
    }
    else if ( index == indexOutput )
    {
      manualSettingsPanel.getOutputPanel().updatePBOutput();
    }
  }

}
