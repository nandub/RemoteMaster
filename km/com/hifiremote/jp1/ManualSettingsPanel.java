package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;

import com.hifiremote.jp1.ManualCodePanel.CodeTableModel;
import com.hifiremote.jp1.ProtocolDataPanel.EnableOps;
import com.hifiremote.jp1.ProtocolDataPanel.PDMainPanel;
import com.hifiremote.jp1.ProtocolDataPanel.PFMainPanel;
import com.hifiremote.jp1.ProtocolDataPanel.FunctionMainPanel;
import com.hifiremote.jp1.ProtocolDataPanel.Mode;

public class ManualSettingsPanel extends JPanel implements ActionListener, PropertyChangeListener, DocumentListener,
ChangeListener, ListSelectionListener, ItemListener
{
  public ManualSettingsPanel()
  {
    loadInProgress = true;
    setLayout( new BorderLayout() );
    JPanel leftPanel = new JPanel( new BorderLayout() );
    leftPanel.addComponentListener( new ComponentAdapter()
    {
      public void componentResized( ComponentEvent e )
      {
        interpretPFPD();
        pfMainPanel.set();
        pdMainPanel.set();
        fnMainPanel.set();
      }
    } );

    name = new JTextField();
    name.getDocument().addDocumentListener( this );
    variantName = new JTextField();
    variantName.getDocument().addDocumentListener( this );
    pid = new JFormattedTextField( new HexFormat( 2, 2 ) );
    new TextPopupMenu( pid );
    pid.addPropertyChangeListener( "value", this );
    
    availabilityPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
    availabilityLabel = new JLabel();
    availabilityLabel.setFont( availabilityLabel.getFont().deriveFont( Font.BOLD ) );
    availabilityLabel.setForeground( Color.RED );
    availabilityPanel.add( availabilityLabel );
    add( availabilityPanel, BorderLayout.PAGE_START );

    // Protocol Code Table.
    tablePanel = new ManualCodePanel( this );
    
    // Create the split pane that goes in the centre of this settings panel.
    assemblerPanel = new AssemblerPanel( this );
    assemblerModel.settingsPanel = this;
    outerPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, leftPanel, assemblerPanel );
    outerPane.setResizeWeight( 0 );
    add( outerPane, BorderLayout.CENTER );
    
    // Now create the midPanel that goes at the page end of the table panel.
    JPanel midPanel = new JPanel( new BorderLayout() );
    tablePanel.add( midPanel, BorderLayout.PAGE_END );
    
    buttonPanel = new JPanel();
    buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.LINE_AXIS ));
    midPanel.add( buttonPanel, BorderLayout.CENTER );

    buttonPanel.setBorder( BorderFactory.createEmptyBorder( 5,5,5,5 ) );
    buttonPanel.add( new JLabel( "Use predefined constants for: " ) );
    buttonPanel.add( useRegisterConstants );
    buttonPanel.add( useFunctionConstants );
    buttonPanel.add( Box.createHorizontalGlue() );
    buttonPanel.add( assemblerPanel.importHexButton );

    importButton = new JButton( "Import from Clipboard" );

    // These cannot be moved earlier, for reasons that are not clear.
    procs = ManualCodePanel.getProcs();
    procBox = ManualCodePanel.getProcBox();
    procBox.addActionListener( this );
    
    itemLists = new List[ procs.length ];
    for ( int i = 0; i < procs.length; i++ )
    {
      itemLists[ i ] = new ArrayList< AssemblerItem >();
      itemLists[ i ].add( new AssemblerItem() );
    }

    // Create left panel as tabbed pane
    devicePanel = new ManualDevicePanel();
    tabbedPane = new JTabbedPane();
    tabbedPane.addChangeListener( this );
    leftPanel.add( tabbedPane, BorderLayout.CENTER );
    
    // Protocol Data tab of left panel
    protDataPanel = new ProtocolDataPanel( this );
    protDataScrollPane = new JScrollPane( protDataPanel );
    protDataScrollPane.setPreferredSize( protDataPanel.getPrefSize() );  // needed to limit height of pane
    assemblerPanel.setProtDataPanel( protDataPanel );
    
    // PF Details tab of left panel (added to tabbed pane by valueChanged() when a protocol is selected)
    pfMainPanel = protDataPanel.pfMainPanel;

    // PD Details tab of left panel (added to tabbed pane by valueChanged() when a protocol is selected)
    pdMainPanel = protDataPanel.pdMainPanel;

    // Function tab of left panel (added to tabbed pane by valueChanged() when a protocol is selected)
    fnMainPanel = protDataPanel.fnMainPanel;
    
    tabbedPane.addTab( "Protocol Data", protDataScrollPane );
    tabbedPane.add( "Functions", fnMainPanel );

    useFunctionConstants.addItemListener( this );
    useRegisterConstants.addItemListener( this );
    setMode( Mode.DISASM );

    analyzerPanel = new JP2AnalyzerPanel();
    pfDescriptionPanel = new JP2AnalyzerPanel();
    pfDescriptionPanel.setDescription();
    outputPanel = new RMPBOutputPanel( this );

    // To remove constraints on position of divider in the split pane, set minimum size to 0.
    Dimension minimumSize = new Dimension(0, 0);
    leftPanel.setMinimumSize( minimumSize );
    assemblerPanel.setMinimumSize( minimumSize );

    // Position the divider according to the length of a long string from the tabbed panes.
    String s = "Use Extended Lead-Out OFF time, adding 0xFFFF to value in PD0A/PD0B?";
    int baseWidth = ( new JLabel( s ) ).getPreferredSize().width;
    dividerLocation = outerPane.getInsets().left + baseWidth;
    outerPane.setDividerLocation( dividerLocation );
    
    // Set the width of the assembler panel, to control width of protocol editor in RM
    // and initial width in RMPB.
    Dimension d = assemblerPanel.getPreferredSize();
    d.width = ( int )( baseWidth * 1.8 );
    assemblerPanel.setPreferredSize( d );
    
    deviceText.setLineWrap( true );
    deviceText.setWrapStyleWord( true );
    deviceText.getDocument().addDocumentListener( this );

    Rectangle rect = getBounds();
    int x = rect.x - rect.width / 2;
    int y = rect.y - rect.height / 2;
    setLocation( x, y );

    tabbedPane.setSelectedIndex( 0 );
    
    if ( mode == Mode.DISASM )
    {
      protDataPanel.doBoxEnableStates( EnableOps.DISABLE );
    }
    loadInProgress = false;
  }
  
  public void setProtocol( ManualProtocol protocol, boolean isClone )
  {
    this.protocol = protocol;
    tablePanel.setProtocol( protocol );
    System.err.println( "protocol=" + protocol );
    name.setText( protocol.getName() );
    name.setEditable( isClone );
    name.setEnabled( isClone );
    
    if ( protocol.getVariantName() != null )
    {
      variantName.setText( protocol.getVariantName() );
    }
    variantName.setEditable( isClone );
    variantName.setEnabled( isClone );
    
    devicePanel.setProtocol( protocol );
    Hex id = protocol.getID();
    pid.setValue( id );
    tablePanel.getCodeTable().repaint();
    validate();
  }
  
  public String getProtocolText( boolean deviceOnly, boolean fromData )
  {
    String protText = null;
    String ls = System.getProperty( "line.separator" );
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter( sw );
    if ( fromData )
    {    
      PropertyReader pr = protocol.getIniReader( false, pid.isEnabled() && pid.getText() != null
          && !pid.getText().trim().isEmpty() );
      Property p = null;
      int startAt = deviceOnly ? 2 : 1;
      int line = 0;     
      while ( ( p = pr.nextProperty() ) != null )
      {
        line++;
        if ( line > startAt && ( !deviceOnly || !p.name.startsWith( "Code." ) ) )
        {
          pw.println( p.name + "=" + p.value );
        }
      }
      String protBody = sw.toString();
      protText = !deviceOnly ? "[" + ManualProtocol.getDefaultName( protocol.getID() ) + "]" + ls : "";
      protText += protBody;
    }
    else
    {
      if ( !deviceOnly )
      {
        pw.println( "[" + protocol.getName() + "]" );
        pw.println( "PID=" + protocol.getID() );
        String variantName = protocol.getVariantName().trim();
        if ( variantName != null && !variantName.isEmpty() )
        {
          pw.println( "VariantName=" + variantName );
        }
        pw.print( displayProtocol.getIniIntro() );
        pw.print( getIniCode() );
      }
      else
      {
        pw.print( displayProtocol.getIniIntro() );
      }
      protText = sw.toString();
    }
    pw.close();
    return protText;
  }
  
  public ManualProtocol getProtocol()
  {
    return protocol;
  }

  public String getIniCode()
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter( sw );
    for ( Processor pr : procs )
    {
      String name = pr.getEquivalentName();
      Hex hex = protocol.getCode( pr );
      // We want dispHex to be the official code, not custom code when that is present.
      Hex dispHex = ( displayProtocol == null ) ? null : displayProtocol.code.get( name );
      if ( hex == null )
      {
        hex = dispHex;
      }
      if ( hex != null )
      {
        pw.println( "Code." + name + "=" + hex.toRawString() );
      }
    }
    return sw.toString();
  }

  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    if ( source == importButton )
    {
      importFromClipboard();
    }
    else if ( source == procBox )
    {
      Processor pr = ( Processor )procBox.getSelectedItem();
      selectProcessor( pr );
      setVisibility();
    }
  }
  
  public void setVisibility()
  {
    Processor pr = ( Processor )procBox.getSelectedItem();
    boolean showProc = mode == Mode.DISASM || displayProcessor == null 
        || displayProcessor.getEquivalentName().equals( pr.getEquivalentName() );
    if ( tabbedPane.isVisible() && !showProc )
    {
      dividerLocation = outerPane.getDividerLocation();
    }
    tabbedPane.setVisible( showProc );
    assemblerPanel.setVisible( showProc );
    if ( showProc )
    {
      outerPane.setDividerLocation( dividerLocation );
    }
    availabilityPanel.setVisible( !showProc );
  }
  
  public String readClipboard()
  {
    JPanel panel = new JPanel( new BorderLayout() );
    JLabel message = new JLabel( "Enter one or more PB-/KM-/IR-formatted protocol upgrades below." );
    message.setBorder( BorderFactory.createEmptyBorder( 5, 5, 10, 5 ) );
    panel.add( message, BorderLayout.NORTH );

    JTextArea textArea = new JTextArea( 10, 60 );
    new TextPopupMenu( textArea );

    JScrollPane scrollPane = new JScrollPane( textArea );
    scrollPane.setBorder( BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder( "Protocol Upgrade Code" ), scrollPane.getBorder() ) );
    panel.add( scrollPane, BorderLayout.CENTER );
    int rc = JOptionPane.showConfirmDialog( this, panel, "Import Protocol Upgrade", JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE, null );
    if ( rc == JOptionPane.OK_OPTION )
    {
      return textArea.getText();
    }
    return null;
  }
  
  public void importFromClipboard()
  {
    String clipboardText = readClipboard();
    if ( clipboardText != null )
    {
      String[] ident = tablePanel.importProtocolCode( clipboardText, false );
      name.setText( ident[ 0 ] );
      variantName.setText( ident[ 1 ] );
      pid.setValue( new Hex( ident[ 2 ] ) );
      Processor p = ProcessorManager.getProcessor( ident[ 3 ] );
      procBox.setSelectedItem( p );
      deviceText.setText( getProtocolText( true, true ) );
      outputPanel.updatePBOutput();
    }
  }
  
  public String nextSection( BufferedReader br, String line )
  {
    String section = null;
    try
    {
      // skip empty lines
      while ( line != null )
      {
        line = line.trim();
        if ( !line.isEmpty() && line.charAt( 0 ) == '[' )
        {
          section = line.substring( 1, line.length() - 1 );
          break;
        }       
        line = br.readLine();
      }
    }
    catch ( Exception ex )
    {
      ex.printStackTrace( System.err );
    }
    return section;
  }
  
  public void writeProtFile( String protText )
  {
    File addOnDir = RemoteMaster.getAddonDir();
    if ( !addOnDir.isDirectory() )
    {
      System.err.println( "Add-on folder is not a directory" );
      return;
    }
    PropertyFile properties = JP1Frame.getProperties();
    String lastName = properties.getProperty( "ProtFile" );
    List< String > protNames = new ArrayList< String >();
    for ( File f : addOnDir.listFiles() )
    {
      String fName = f.getName();
      if ( fName.endsWith( ".prot" ) )
      {
        protNames.add( fName.substring( 0, fName.length() - 5 ) );
      }
    }
    Collections.sort( protNames );
    if ( lastName != null && !protNames.contains( lastName ) )
    {
      lastName = null;
    }
    String title = "Save as Add-on protocol";
    String message = 
        "<html>The protocol will be saved in the AddOns folder in a<br>"
            + "file with a .prot extension.  The drop-down box lists<br>"
            + "the names of the existing add-on files.  To replace a<br>"
            + "file, select its name.  To create a new file, enter a<br> "
            + "name for it (without the extension).</html>";
    Box box = Box.createVerticalBox();
    JPanel panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
    panel.add( new JLabel( message ) );
    box.add( panel );
    JComboBox< String > fileBox = new JComboBox< String >( protNames.toArray( new String[ 0 ] ) );
    fileBox.setEditable( true );
    if ( lastName != null )
    {
      fileBox.setSelectedItem( lastName );
    }
    box.add( fileBox );
    int result = JOptionPane.showConfirmDialog( this, box, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );
    if ( result != JOptionPane.OK_OPTION )
    {
      return;
    }
    String name = ( String )fileBox.getSelectedItem();
    if ( name.toLowerCase().endsWith( ".prot" ) )
    {
      name = name.substring( 0, name.length() - 5 );
    }
    File file = new File( RemoteMaster.getAddonDir(), name + ".prot" );
    if ( file.exists() )
    {
      message = "File " + file.getAbsolutePath() + " already exists.\n"
          + "Do you wish to replace it?";
      result = JOptionPane.showConfirmDialog( this, message, "File conflict", 
          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
      if ( result != JOptionPane.YES_OPTION )
      {
        return;
      }
    }
    try
    {
      PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter( file ) ) );
      out.print( protText );
      out.close();
    }
    catch ( IOException ex )
    {
      title = "File write error";
      message = "Attempt to write to file " + file.getName() + " failed.";
      JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
      return;
    }
    properties.setProperty( "ProtFile", name );
  }
  
  public void loadPB( File loadFile )
  {
    boolean differs = false;
    try
    {
      BufferedReader in = new BufferedReader( new FileReader( loadFile ) );
      String line = in.readLine(); // line 1 "PB Version:"
      String token = line.substring( 0, 11 );
      if ( !token.equals( "PB Version:" ) )
      {
        JOptionPane.showMessageDialog( RemoteMaster.getFrame(), "The PB protocol you are trying to import is not valid!",
            "Import Failure", JOptionPane.ERROR_MESSAGE );
        in.close();
        return;
      }
      loadInProgress = true;
      String delim = line.substring( 11, 12 );
      List< String > fields = LineTokenizer.tokenize( line, delim );
      String pbVersion = fields.get( 1 );
      System.err.println( "PB version of imported file is '" + pbVersion + '\'' );
      String description = fields.get( 2 );
      name.setText( description );
      line = in.readLine();
      fields = LineTokenizer.tokenize( line, delim );
      String procName = fields.get( 1 );
      String procName2 = procName;
      if ( procName.startsWith( "S3" ) )
      {
        procName = "S3C80";
        procName2 = procName2.indexOf( '+' ) > 0 ? "S3F80" : "S3C80";
      }
      else if ( procName.startsWith( "P8/740" ) )
      {
        procName = "740";
        procName2 = procName;
      }
      Processor proc = ProcessorManager.getProcessor( procName );
      int ramAddress = ProcessorManager.getProcessor( procName2 ).getRAMAddress();  
      int ndx = Arrays.asList( procs ).indexOf( proc );
      List< AssemblerItem > list = itemLists[ ndx ];
      list.clear();
      list.add( new AssemblerItem( 0, "ORG", String.format( "%04XH", ramAddress ) ) );
      int i = 1;
      while ( i < fields.size() )
      {
        String s = fields.get( i++ );
        if ( s == null )
          continue;
        if ( s.contains( "BYTE" ) )
          break;
      }
      String codeText = fields.get( i );
      String f = null;
      
      while ( ( line = in.readLine() ) != null )
      {
        AssemblerItem item = new AssemblerItem();
        fields = LineTokenizer.tokenize( line, delim );        
        if ( ( f = fields.get( 0 ) ) != null && f.equals( "Protocol ID:" ) )
        {
          pid.setValue( new Hex( fields.get( 1 ) ) );
        }
        if ( 12 < fields.size() && ( f = fields.get( 12 ) ) != null )
        {
          item.setAddress( Integer.parseInt( f, 16 ) );
        }
        if ( 13 < fields.size() && ( f = fields.get( 13 ) ) != null && !f.startsWith( "=" ) )
        {
          item.setHex( new Hex( f ) );
        }
        if ( 14 < fields.size() && ( f = fields.get( 14 ) ) != null )
        {
          f += f.endsWith( ":" ) ? "" : ":";          
          item.setLabel( f );
        }
        if ( 15 < fields.size() && ( f = fields.get( 15 ) ) != null )
        {
          item.setOperation( f );
        }
        if ( 16 < fields.size() && ( f = fields.get( 16 ) ) != null )
        {
          item.setArgumentText( f );
        }
        if ( 17 < fields.size() && ( f = fields.get( 17 ) ) != null )
        {
          f = ( f.startsWith( ";" ) ? "" : ";" ) + f;
          item.setComments( f );
        }
        if ( i < fields.size() && fields.get( i ) != null )
        {
          codeText += " " + fields.get( i );
        }
        list.add( item );
      }
      in.close();
      list.add( new AssemblerItem() );
      assemblerModel.getData().clear();
      assemblerModel.getData().addAll( list );
      assemblerModel.setItemList( assemblerModel.getData() );
      differs = checkAssembly( list, proc );
      assemblerModel.fireTableDataChanged();
      assemblerPanel.setAssembled( true );
      Hex hex = new Hex( codeText );
      tablePanel.getCodeTable().setValueAt( hex, ndx, 1 );
      assemblerPanel.setAssembled( false );
      procBox.setSelectedItem( proc );
      deviceText.setText( getProtocolText( true, true ) );
      outputPanel.updatePBOutput();
      loadInProgress = false;
    }
    catch ( Exception ex )
    {
      ex.printStackTrace( System.err );
    }
    if ( differs )
    {
      showDifferenceMessage();
    }
  }

  public String[] loadRMPB( File loadFile, boolean idOnly, int modeIndex )
  {
    PropertyFile properties = JP1Frame.getProperties();
    JTableX codeTable = tablePanel.getCodeTable();
    CodeTableModel codeModel = ( CodeTableModel )codeTable.getModel();
    boolean differs = false;
    try
    {
      loadInProgress = true;
      file = loadFile;
      properties.setProperty( "RMPBPath", file.getParentFile() );
      DataInputStream in = new DataInputStream( new FileInputStream( file ) );
      BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
      String line = "";
      List< AssemblerItem > list = null;
      Processor firstProc = null;
      String section = null;
      int procIndex = 0;
      String[] idStrings = new String[ 3 ];
      while ( ( section = nextSection( br, line )) != null )
      {
        if ( section.equals( "Identification" ) )
        {
          while ( ( line = br.readLine() ) != null && !( line.startsWith( "[" ) ) )
          {
            if ( line.startsWith( "Name=" ) )
            {
              idStrings[ 0 ] = line.substring( 5 ).trim();
            }
            else if ( line.startsWith( "VariantName=" ) )
            {
              String vName = line.substring( 12 ).trim();
              idStrings[ 1 ] = modeIndex == 2 ? vName == null || vName.equals( "" ) ? "Custom" :  vName + "-Custom" : vName;
            }
            else if ( line.startsWith( "PID=" ) )
            {
              idStrings[ 2 ] = line.substring( 4 ).trim();
            }
          }
          if( modeIndex == 2 && idStrings[ 1 ] == null )
            idStrings[ 1 ] = "Custom";
          
          if ( idOnly )
          {
            return idStrings;
          }
          else
          {
            name.setText( idStrings[ 0 ] );
            variantName.setText( idStrings[ 1 ] );
            pid.setValue( new Hex( idStrings[ 2 ] ) );
          }
        }
        else if ( section.equals( "Translators" ) )
        {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter( sw );
          while ( ( line = br.readLine() ) != null && !( line.startsWith( "[" ) ) )
          {
            pw.println( line );
          }
          deviceText.setText( sw.toString() );
          pw.close();
          
          if ( modeIndex == 0 )
          {
            // This is loading a manual protocol, so the device and command
            // parameter tables need to be set from the translators.
            StringReader sr = new StringReader( deviceText.getText() );
            BufferedReader dbr = new BufferedReader( sr );
            PropertyReader pr = new PropertyReader( dbr );
            Properties props = new Properties();
            Property property = new Property();
            while ( ( property = pr.nextProperty() ) != null )
            {
              props.put( property.name, property.value );
            }
            props.put( "PID", pid.getText() );
            dbr.close();
            ManualProtocol temp = new ManualProtocol( props );
            protocol.setDeviceParms( Arrays.asList( temp.getDeviceParameters() ) );
            protocol.setCommandParms( Arrays.asList( temp.getCommandParameters() ) );
            protocol.setDeviceTranslators( Arrays.asList( temp.getDeviceTranslators() ) );
            protocol.setCommandTranslators( Arrays.asList( temp.getCmdTranslators() ) );
            protocol.setDefaultCmd( temp.getDefaultCmd() );
            protocol.setRawHex( temp.getFixedData( new Value[ 0 ] ) );
            protocol.setCmdIndex( temp.getCmdIndex() );
          }
        }
        else if ( section.equals( "Executor" ) )
        {
          while ( ( line = br.readLine() ) != null && !( line.startsWith( "[" ) ) )
          {
            if ( line.trim().isEmpty() )
            {
              continue;
            }
            AssemblerItem item = new AssemblerItem();
            StringTokenizer st = new StringTokenizer( line, "\t", true );
            int n = 0;
            while ( st.hasMoreTokens() )
            {
              String token = st.nextToken();
              if ( token.equals( "\t" ) )
              {
                n++;
                continue;
              }
              token = token.trim();
              if ( token.isEmpty() )
              {
                continue;
              }
              switch ( n )
              {
                case 0:
                  item.setAddress( Integer.parseInt( token, 16 ) );
                  break;
                case 1:
                  item.setHex( new Hex( token ) );
                  break;
                case 2:
                  item.setLabel( token );
                  break;
                case 3:
                  item.setOperation( token );
                  break;
                case 4:
                  item.setArgumentText( token );
                  break;
                case 5:
                  item.setComments( token );
                  break;
              }
            }
            if ( item.getOperation().equals( "PROC" ) )
            {
              for ( int k = 0; k < procs.length; k++ )
              {
                if ( procs[ k ].getEquivalentName().equals( item.getArgumentText() ) )
                {
                  if ( firstProc == null )
                  {
                    firstProc = procs[ k ];
                  }
                  procIndex = k;
                  list = itemLists[ k ];
                  list.clear();
                  break;
                }
              }
            }
            else
            {
              list.add( item );
            }
          }
          list.add( new AssemblerItem() );
        }
        else if ( section.equals( "Code" ) )
        {
          assemblerModel.getData().clear();
          assemblerModel.getData().addAll( list );
          differs = checkAssembly( list, procs[ procIndex ] ) || differs;
          while ( ( line = br.readLine() ) != null && !( line.startsWith( "[" ) ) )
          {
            if ( line.trim().isEmpty() )
            {
              continue;
            }
            assemblerPanel.setAssembled( true );
            codeModel.setValueAt( new Hex( line ), procIndex, 1 );
            assemblerPanel.setAssembled( false );
          }
        }
      }  
      in.close();
      procBox.setSelectedItem( firstProc );
      processor = firstProc;
//      processor = ( Processor )procBox.getSelectedItem();
      setProcessor( processor );
      setMode( Mode.ASM );

      int row = procBox.getSelectedIndex();
      assemblerModel.getData().clear();
      assemblerModel.getData().addAll( itemLists[ row ] );
      assemblerModel.setItemList( assemblerModel.getData() );
      assemblerModel.fireTableDataChanged();

      outputPanel.updatePBOutput();
      loadInProgress = false;
    }
    catch ( Exception ex )
    {
      ex.printStackTrace( System.err );
    }
    if ( differs )
    {
      showDifferenceMessage();
    }
    return null;
  }
  
  private void showDifferenceMessage()
  {
    String title = "Assembler difference";
    String message = 
        "The hex code in the file just loaded differs in some places from the code\n"
      + "that this assembler would generate from the same source.  The code that\n"
      + "differs is shown in RED.  Some processors have instructions that have more\n"
      + "than one valid hex translation, so this is not necessarily an error.\n\n"
      + "To replace this code with that from this assembler, press the Assemble\n"
      + "button after exiting this message.\n\n"
      + "Lines entirely in red mark instructions that cause assembler errors.";
    JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
  }
  
  private boolean checkAssembly( List< AssemblerItem > list, Processor proc )
  {
    boolean differs = false;
    Hex[] oldHex = new Hex[ list.size() ];
    for ( int n = 0; n < list.size(); n++ )
    {
      oldHex[ n ] = list.get( n ).getHex() == null ? new Hex( 0 ) : list.get( n ).getHex();
    }
    assemblerModel.assemble( proc );
    for ( int n = 0; n < list.size(); n++ )
    {
      Hex hex = list.get( n ).getHex() == null ? new Hex( 0 ) : list.get( n ).getHex();
      if ( !hex.equals( oldHex[ n ] ) )
      {
        list.get( n ).setChecked( false );
        list.get( n ).setHex( oldHex[ n ] );
        differs = true;
      }
    }
    return differs;
  }
  
  public File saveAs()
  {
    PropertyFile properties = JP1Frame.getProperties();
    RMFileChooser chooser = new RMFileChooser( properties.getFileProperty( "RMPBPath", RemoteMaster.getWorkDir() ) );
    EndingFileFilter protFilter = new EndingFileFilter( "Protocol files (*.rmpb)", RemoteMaster.rmpbEndings );
    chooser.setFileFilter( protFilter );

    if ( file != null )
    {
      chooser.setSelectedFile( file );
    }
    int returnVal = chooser.showSaveDialog( this );
    if ( returnVal == RMFileChooser.APPROVE_OPTION )
    {
      String ending = ( ( EndingFileFilter )chooser.getFileFilter() ).getEndings()[ 0 ];
      String fileName = chooser.getSelectedFile().getAbsolutePath();
      if ( !fileName.toLowerCase().endsWith( ending ) )
      {
        fileName = fileName + ending;
      }
      File newFile = new File( fileName );
      int rc = JOptionPane.YES_OPTION;
      if ( newFile.exists() )
      {
        rc = JOptionPane.showConfirmDialog( this, newFile.getName() + " already exists.  Do you want to replace it?",
            "Replace existing file?", JOptionPane.YES_NO_OPTION );
      }
      if ( rc == JOptionPane.YES_OPTION )
      {
        file = newFile;
        properties.setProperty( "RMPBPath", file.getParentFile() );
        saveRMPB();
      }
    }
    return file;
  }
  
  public void saveRMPB()
  {
    JTableX codeTable = tablePanel.getCodeTable();
    CodeTableModel codeModel = ( CodeTableModel )codeTable.getModel();
    if ( mode == Mode.ASM )
    {
      int row = procBox.getSelectedIndex();
      itemLists[ row ].clear();
      itemLists[ row ].addAll( assemblerModel.getItemList() );
    }
    try
    {
      String ls = System.getProperty( "line.separator" );
      PrintWriter pw = new PrintWriter( new FileWriter( file ) );
      pw.println( "[Identification]" );
      pw.println( "Name=" + name.getText().trim() );
      String vName = variantName.getText().trim();
      if ( !vName.isEmpty() )
      {
        pw.println( "VariantName=" + vName );
      }
      pw.println( "PID=" + pid.getText() );
      pw.println();
      pw.println( "[Translators]" );
      pw.print( deviceText.getText() );
      for ( int row = 0; row < itemLists.length; row++ )
      {
        List< AssemblerItem > list = itemLists[ row ];
        if ( list.size() <= 1 ) continue;
        pw.println();
        pw.println( "[Executor]" );
        String line = "\t\t\tPROC\t" + procs[ row ].getEquivalentName(); 
        pw.println( line );
        for ( int i = 0; i < itemLists[ row ].size(); i++ )
        {
          AssemblerItem item = itemLists[ row ].get( i );
          line = "";
          String addr = ( String )item.getElement( 0 );
          line += ( addr.isEmpty() ? "" : addr ) + "\t";  // address field
          Hex hex = ( Hex )item.getElement( 1 );
          line += ( hex == null ? "" : hex.toString() ) + "\t";  // hex code
          String str = item.getLabel().trim();
          line += str;
          if ( !str.isEmpty() && !str.equals( ";" ) && !str.endsWith( ":" ) )
            line += ":";
          line += "\t" + item.getOperation() + "\t" + item.getArgumentText();
          str = item.getComments();
          if ( !str.isEmpty() )
          {
            line += "\t";
            if ( !str.startsWith( ";" ) )
              line += ";";
            line += str;
          }
          line += ls;
          int j = 0;
          while ( j < line.length() && Character.isWhitespace( line.charAt( j ) ) )
            j++ ;
          if ( j < line.length() || i < assemblerModel.getItemList().size() - 1 )
          {
            pw.print( line );
          }
        }
        pw.println();
        pw.println( "[Code]" );
        Hex hex = ( Hex )codeModel.getValueAt( row, 1 );
        if ( hex != null )
        {
          pw.println( hex.toString() );
        }
      }
      pw.close();
    }
    catch ( IOException ex )
    {
      ex.printStackTrace( System.err );
    }
  }
  
  public void selectProcessor( Processor proc )
  { 
    Hex hex = protocol.getCode( proc );
    if ( ( hex == null || hex.length() == 0 ) && displayProtocol != null )
      hex = displayProtocol.getCode( proc );
    if ( proc.getDataStyle() < 0 )
    {
      analyzerPanel.set( proc, hex );
      return;
    }
    
    buttonPanel.setVisible( true );
    Hex oldHex = null;
    if ( processor != null )
    {
      if ( mode == Mode.ASM ) for ( int i = 0; i < procs.length; i++ )
      {
        if ( procs[ i ].getEquivalentName().equals( processor.getEquivalentName() ) )
        {
          assemblerPanel.stopEditing();
          itemLists[ i ].clear();
          itemLists[ i ].addAll( assemblerModel.getItemList() );
          break;
        }
      }
      oldHex = protocol.getCode( processor );
      if ( oldHex == null )  oldHex = ( displayProtocol == null ) ? null : displayProtocol.getCode( processor );
    }
    // If new processor has no code then preserve the Protocol Data fields.
    if ( hex == null || hex.length() == 0 )
    {
      protDataPanel.setInterpretations();
    }
    assemblerModel.disassemble( hex, proc );
    if ( hex == null || hex.length() == 0 )
    {    
      protDataPanel.restoreInterpretations();
    }
    
    interpretPFPD();
    pfMainPanel.set();
    pdMainPanel.set();
    fnMainPanel.set();
    
    if ( mode == Mode.ASM )
    {
      int row = procBox.getSelectedIndex();
      assemblerModel.getData().clear();
      assemblerModel.getData().addAll( itemLists[ row ] );
      assemblerModel.setItemList( assemblerModel.getData() );
      assemblerModel.fireTableDataChanged();
    }

    assemblerPanel.getEditorPanel().setAssemblerButtons( false );
    int tabCount = tabbedPane.getTabCount();
    String procName = proc.toString();
        
//    setProcessor( proc );  // TESTING - DIDN'T SEEM TO SET "processor"
//  Re above line: the field "processor" IS set, correctly, by assemblerModel.disassemble(...)
//  above, taking proper account of the difference between S3C80 and S3F80.  This "correction"
//  sets only S3C80 and so needs to be removed.
    
    if ( !procName.equals( "S3C80" ) && !procName.equals( "HCS08" ) )// && tabCount > 2 )
    {
      tabbedPane.remove( pfMainPanel );
      tabbedPane.remove( pdMainPanel );
    }
    else if ( ( procName.equals( "S3C80" ) || procName.equals( "HCS08" ) ) && tabCount <= 3 )
    {
      tabbedPane.add( "PF Details", pfMainPanel );
      tabbedPane.add( "PD Details", pdMainPanel );
    }
  }
  
  public void setSelectedCode( Processor proc )
  {
    for ( int i = 0; i < procs.length; i++ )
    {
      if ( procs[ i ].getEquivalentName().equals( proc.getEquivalentName() ) )
      {
        JTableX codeTable = tablePanel.getCodeTable();
        codeTable.getSelectionModel().setSelectionInterval( i, i );
        procBox.setSelectedItem( procs[ i ] );
        break;
      }
    }
  }
  
  public void setDisplayProtocol( Protocol displayProtocol )
  {
    this.displayProtocol = displayProtocol;
    tablePanel.setDisplayProtocol( displayProtocol );
  }

  public void setProcessor( Processor processor )
  {
    this.processor = processor;
    protDataPanel.setProcessor( processor );
    assemblerPanel.setProcessor( processor );
  }

  public void interpretPFPD()
  {
    protDataPanel.interpretPFPD( false );
  }
  
  public void reset( boolean forClone )
  {
    setDisplayProtocol( null );
    setProtocol( new ManualProtocol( null, null ), forClone  );
    name.setText( null );
    variantName.setText( null );
    pid.setText( null );
    deviceText.setText( null );
    for ( int i = procs.length; i > 0; i-- )
    {
      // Run through the processors backwards so that we finish up with the first one
      processor = procs[ i - 1 ];
      setProcessor( processor );
      itemLists[ i - 1 ].clear();
      itemLists[ i - 1 ].add( new AssemblerItem() );
      assemblerModel.disassemble( null, processor );
      if ( processor.getDataStyle() >= 0 )
      {
        procBox.setSelectedItem( processor );
      }
    }
    
    protDataPanel.reset();
    if ( outputPanel != null )
    {
      outputPanel.reset();
    }
    changed = false;
  }
  
  public boolean isChanged()
  {
    return changed;
  }

  public void setChanged( boolean changed )
  {
    if ( !loadInProgress )
    {
      this.changed = changed;
    }
  }

  public void propertyChange( PropertyChangeEvent e )
  {
    Object source = e.getSource();
    if ( source == pid )
    {
      Hex id = ( Hex )pid.getValue();
      boolean inDeviceUpgrade = false;
      Protocol p = null;
      DeviceUpgrade du = null;
      if ( id != null && id.length() != 0 && remoteConfig != null )
      {
        Remote remote = remoteConfig.getRemote();
        for ( DeviceUpgrade temp : remoteConfig.getDeviceUpgrades() )
        {
          du = temp;
          p = temp.getProtocol();

          if ( p.getID( remote ).equals( id ) )
          {
            inDeviceUpgrade = true;
            break;
          }
        }
        if ( inDeviceUpgrade )
        {
          String title = "Manual Settings";
          boolean exit = false;
          String starredID = du.getStarredID();
          boolean usesProtocolUpgrade = ( starredID.endsWith( "*" ) );

          if ( usesProtocolUpgrade )
          {
            String message = "There is a Device Upgrade that is using a protocol upgrade with\n" + "PID " + id
                + " Do you want to abort this PID choice and enter\n" + "a different one?  If so, please press OK.\n\n"
                + "If you want to edit that protocol code, also press OK, then exit\n"
                + "this dialog, change to the Devices page and edit the protocol of\n"
                + "the device upgrade from there.\n\n"
                + "To continue, press CANCEL but you will be creating a Manual Protocol\n"
                + "that cannot be accessed while that Device Upgrade is present.";
            exit = ( JOptionPane.showConfirmDialog( null, message, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE ) == JOptionPane.OK_OPTION );
          }
          else
          {
            String message = "There is a Device Upgrade with protocol with PID " + id + " that\n"
                + "is not yet using a protocol upgrade, so you cannot create a new\n"
                + "manual protocol with that PID.  If you want to create a manual\n"
                + "protocol then please choose a different PID.  If you want to\n"
                + "provide code for that device upgrade, please change to the\n"
                + "Devices page and edit the protocol from there.";
            JOptionPane.showMessageDialog( null, message, title, JOptionPane.WARNING_MESSAGE );
            exit = true;
          }

          if ( exit )
          {
            pid.setValue( null );
            devicePanel.enableButtons();
            return;
          }
        }
      }

      protocol.setID( id );
      name.getDocument().removeDocumentListener( this );
      ManualSettingsDialog dialog = devicePanel.getSettingsDialog();
      if ( dialog != null )
      {
        int modeIndex = dialog.getModeIndex();
        if ( modeIndex == 0 || modeIndex == 1 )
        {
          // Ensure that the name is set from PID, by setting it first to null
          protocol.setName( null );
          protocol.setName( protocol.getName() );
        }
      }
      name.setText( protocol.getName() );
      name.getDocument().addDocumentListener( this );
    }
    devicePanel.enableButtons();
  }
  
  @Override
  public void stateChanged( ChangeEvent event )
  {
    if ( event.getSource() == tabbedPane )
    { 
      pfMainPanel.setActive( tabbedPane.getSelectedComponent() == pfMainPanel );
      pdMainPanel.setActive( tabbedPane.getSelectedComponent() == pdMainPanel );
      fnMainPanel.setActive( tabbedPane.getSelectedComponent() == fnMainPanel );
      protDataPanel.setActive( tabbedPane.getSelectedComponent() == protDataScrollPane );
      interpretPFPD();
      pfMainPanel.set();
      pdMainPanel.set();
      fnMainPanel.set();
    }
  }
  
  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    // if ( e.getSource() == codeTable.getSelectionModel() ) 
    if ( !e.getValueIsAdjusting() )
    {
      JTableX codeTable = tablePanel.getCodeTable();
      int[] rows = codeTable.getSelectedRows();
      if ( displayProcessor != null )
      {
        boolean test = rows.length == 1 
            && procs[ rows[ 0 ] ].getEquivalentName().equals( displayProcessor.getEquivalentName() );
        assemblerPanel.importHexButton.setEnabled( test );
      }
    }
  }
  
  public Mode getMode()
  {
    return mode;
  }

  public void setMode( Mode mode )
  {
    if ( this.mode == mode )
    {
      return;
    }
    JTableX codeTable = tablePanel.getCodeTable();
    this.mode = mode;
    if ( mode == Mode.DISASM )
    {
      if (  protocol != null && codeTable != null )
      {
        protDataPanel.setMode( Mode.DISASM );
        assemblerPanel.stopEditing();
        int row = procBox.getSelectedIndex();
        itemLists[ row ].clear();
        itemLists[ row ].addAll( assemblerModel.getItemList() );
        
        Processor proc = procs[ row ];
        Hex hex = protocol.getCode( proc );
        if ( ( hex == null || hex.length() == 0 ) && displayProtocol != null )
          hex = displayProtocol.getCode( proc );
        assemblerModel.disassemble( hex, proc );
        assemblerPanel.getEditorPanel().setAssemblerButtons( true );
        protDataPanel.doBoxEnableStates( EnableOps.DISABLE );
      }
    }
    else if ( mode == Mode.ASM )
    {
      protDataPanel.setMode( Mode.ASM );
      protDataPanel.doBoxEnableStates( EnableOps.RESTORE );
      protDataPanel.doBoxEnableStates( EnableOps.SAVE );
      int row = procBox.getSelectedIndex();
      assemblerModel.getData().clear();
      assemblerModel.getData().addAll( itemLists[ row ] );
      assemblerModel.setItemList( assemblerModel.getData() );
      assemblerModel.fireTableDataChanged();
      assemblerPanel.getEditorPanel().setAssemblerButtons( true );
    }
  }

  @Override
  public void itemStateChanged( ItemEvent e )
  {
    Object source = e.getSource();
    if ( source == useFunctionConstants || source == useRegisterConstants )
    {
      assemblerPanel.saveOptionButtons();
    }
  }
  
  public boolean isLoadInProgress()
  {
    return loadInProgress;
  }

  public void setLoadInProgress( boolean loadInProgress )
  {
    this.loadInProgress = loadInProgress;
  }

  // DocumentListener methods
  public void documentChanged( DocumentEvent e )
  {
    Document doc = e.getDocument();

    if ( doc == name.getDocument() )
    {
      protocol.setName( name.getText() );
      devicePanel.enableButtons();
    }
    else if ( variantName != null && doc == variantName.getDocument() )
    {
      protocol.setVariantName( variantName.getText() );
      devicePanel.enableButtons();
    }
    setChanged( true );
  }

  public void changedUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  public void insertUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  public void removeUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }
 
  public ProtocolDataPanel getProtDataPanel()
  {
    return protDataPanel;
  }

  public AssemblerPanel getAssemblerPanel()
  {
    return assemblerPanel;
  }

  public AssemblerTableModel getAssemblerModel()
  {
    return assemblerModel;
  }
  
  public JTabbedPane getTabbedPane()
  {
    return tabbedPane;
  }

  public FunctionMainPanel getFnMainPanel()
  {
    return fnMainPanel;
  }

  public ManualCodePanel getTablePanel()
  {
    return tablePanel;
  }

  public ManualDevicePanel getDevicePanel()
  {
    return devicePanel;
  }

  public JP2AnalyzerPanel getAnalyzerPanel()
  {
    return analyzerPanel;
  }

  public JP2AnalyzerPanel getPfDescriptionPanel()
  {
    return pfDescriptionPanel;
  }

  public RMPBOutputPanel getOutputPanel()
  {
    return outputPanel;
  }

  public JComboBox< Processor > getProcBox()
  {
    return procBox;
  }

  public JTextArea getDeviceText()
  {
    return deviceText;
  }

  public JTextField getProtocolName()
  {
    return name;
  }

  public JTextField getVariantName()
  {
    return variantName;
  }

  public JFormattedTextField getPid()
  {
    return pid;
  }
  
  public static List< AssemblerItem >[] getItemLists()
  {
    return itemLists;
  }

  public void setDisplayProcessor( Processor displayProcessor )
  {
    this.displayProcessor = displayProcessor;
    tablePanel.setDisplayProcessor( displayProcessor );
    String s = "Only the executor for the " + displayProcessor.getEquivalentName()
        + " can be edited for this remote.";
    availabilityLabel.setText( s );
  }
  
  private ManualProtocol protocol = null;
  private Protocol displayProtocol = null;
  private ManualCodePanel tablePanel = null;
  private ManualDevicePanel devicePanel = null;
  private RMPBOutputPanel outputPanel = null;
  private JP2AnalyzerPanel analyzerPanel = null;
  private JP2AnalyzerPanel pfDescriptionPanel = null;
  private JPanel buttonPanel = null;
  private JTextField name = null;
  private JTextField variantName = null;
  public JFormattedTextField pid = null;
  private JButton importButton = null;
  private Processor processor = null;
  private JPanel availabilityPanel = null;
  private JLabel availabilityLabel = null;

  private File file = null;
  private ProtocolDataPanel protDataPanel = null;
  private JTabbedPane tabbedPane = null;
  private JSplitPane outerPane = null;
  private PFMainPanel pfMainPanel = null;
  private JComboBox< Processor > procBox = null;
  private JTextArea deviceText = new JTextArea();
  private boolean changed = false;
  private boolean loadInProgress = false;
  private PDMainPanel pdMainPanel = null;
  private FunctionMainPanel fnMainPanel = null;
  private JScrollPane protDataScrollPane = null;
  private AssemblerPanel assemblerPanel = null;
  private Processor displayProcessor = null;
  private int dividerLocation = 0;
  private Mode mode = Mode.ASM;
  
  private static Processor[] procs = new Processor[ 0 ];
  private static List< AssemblerItem >[] itemLists = new List[ 0 ];
  public RemoteConfiguration remoteConfig = null;
  private AssemblerTableModel assemblerModel = new AssemblerTableModel();
  public JCheckBox useRegisterConstants = new JCheckBox( "Registers" );
  public JCheckBox useFunctionConstants = new JCheckBox( "Functions" );

}

