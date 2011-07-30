package com.hifiremote.jp1;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.Document;

// TODO: Auto-generated Javadoc
/**
 * The Class ManualSettingsDialog.
 */
public class ManualSettingsDialog extends JDialog implements ActionListener, PropertyChangeListener, DocumentListener,
    ChangeListener, ListSelectionListener
{

  /**
   * Instantiates a new manual settings dialog.
   * 
   * @param owner
   *          the owner
   * @param protocol
   *          the protocol
   */
  public ManualSettingsDialog( JDialog owner, ManualProtocol protocol )
  {
    super( owner, "Manual Settings", true );
    createGui( owner, protocol );
  }

  /**
   * Instantiates a new manual settings dialog.
   * 
   * @param owner
   *          the owner
   * @param protocol
   *          the protocol
   */
  public ManualSettingsDialog( JFrame owner, ManualProtocol protocol )
  {
    super( owner, "Manual Settings", true );
    createGui( owner, protocol );
  }

  /**
   * Creates the gui.
   * 
   * @param owner
   *          the owner
   * @param protocol
   *          the protocol
   */
  private void createGui( Component owner, ManualProtocol protocol )
  {
    setLocationRelativeTo( owner );
    Container contentPane = getContentPane();
    
    assemblerTable = new JP1Table( assemblerModel );
    JPanel rightPanel = new JPanel( new BorderLayout() );
    rightPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    
    JScrollPane codePane = new JScrollPane( assemblerTable );
    codePane.setBorder( BorderFactory.createTitledBorder( "Disassembly" ) );
    rightPanel.add( codePane, BorderLayout.CENTER );
    assemblerTable.initColumns( assemblerModel );
    JPanel leftPanel = new JPanel( new BorderLayout() );

    this.protocol = protocol;
    System.err.println( "protocol=" + protocol );

    double b = 5; // space between rows and around border
    double c = 10; // space between columns
    double pr = TableLayout.PREFERRED;
    double size[][] =
    {
        {
            b, pr, c, pr, b
        }, // cols
        {
            b, pr, b, pr, b, pr, b, pr, b, pr, b, pr, b, pr, b
        }
    // rows
    };
    TableLayout tl = new TableLayout( size );
    JPanel mainPanel = new JPanel( tl );
    
    JSplitPane outerPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel );
    outerPane.setResizeWeight( 0 );
    
    contentPane.add( outerPane, BorderLayout.CENTER );
    leftPanel.add( mainPanel, BorderLayout.CENTER );
    
    JLabel label = new JLabel( "Name:", SwingConstants.RIGHT );
    mainPanel.add( label, "1, 1" );
    name = new JTextField( protocol.getName() );
    name.setEditable( false );
    name.setEnabled( false );
    name.getDocument().addDocumentListener( this );
    mainPanel.add( name, "3, 1" );

    label = new JLabel( "Protocol ID:", SwingConstants.RIGHT );
    mainPanel.add( label, "1, 3" );

    pid = new JFormattedTextField( new HexFormat( 2, 2 ) );
    new TextPopupMenu( pid );
    pid.addPropertyChangeListener( "value", this );
    mainPanel.add( pid, "3, 3" );

    // Protocol Code Table
    JPanel tablePanel = new JPanel( new BorderLayout() );
    mainPanel.add( tablePanel, "1, 5, 3, 5" );
    tablePanel.setBorder( BorderFactory.createTitledBorder( "Protocol code" ) );
    codeModel = new CodeTableModel();
    codeTable = new JTableX( codeModel );
    tablePanel.add( new JScrollPane( codeTable ), BorderLayout.CENTER );
    DefaultTableCellRenderer r = ( DefaultTableCellRenderer )codeTable.getDefaultRenderer( String.class );
    r.setHorizontalAlignment( SwingConstants.CENTER );
    codeTable.setDefaultEditor( Hex.class, new HexCodeEditor() );   
    codeTable.getSelectionModel().addListSelectionListener( this );//new ListSelectionListener()

    JLabel l = ( JLabel )codeTable.getTableHeader().getDefaultRenderer().getTableCellRendererComponent( codeTable,
        colNames[ 0 ], false, false, 0, 0 );

    TableColumnModel columnModel = codeTable.getColumnModel();
    TableColumn column = columnModel.getColumn( 0 );
    int width = l.getPreferredSize().width;

    procs = ProcessorManager.getProcessors();
    int count = 0;
    for ( int i = 0; i < procs.length; i++ )
    {
      Processor proc = procs[ i ];
      if ( proc.getEquivalentName().equals( proc.getFullName() ) )
        ++count;
    }
    Processor[] uProcs = new Processor[ count ];
    count = 0;
    for ( int i = 0; i < procs.length; i++ )
    {
      Processor proc = procs[ i ];
      if ( proc.getEquivalentName().equals( proc.getFullName() ) )
        uProcs[ count++ ] = proc;
    }
    procs = uProcs;
    for ( int i = 0; i < procs.length; i++ )
    {
      l.setText( procs[ i ].getFullName() );
      width = Math.max( width, l.getPreferredSize().width );
    }
    for ( int i = 0; i < procs.length; i++ )
    {
      column.setMinWidth( width );
      column.setMaxWidth( width );
      column.setPreferredWidth( width );
    }
    codeTable.doLayout();
    codeTable.setPreferredScrollableViewportSize( codeTable.getPreferredSize() );

    JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
    importButton = new JButton( "Import Protocol Upgrade" );
    importButton.addActionListener( this );
    importButton.setToolTipText( "Import Protocol Upgrades(s) from the Clipboard" );
    buttonPanel.add( importButton );
    tablePanel.add( buttonPanel, BorderLayout.SOUTH );

    // Device Parameter Table
    deviceModel = new ParameterTableModel( protocol, ParameterTableModel.Type.DEVICE );

    deviceTable = new JTableX( deviceModel );
    SpinnerCellEditor editor = new SpinnerCellEditor( 0, 8, 1 );
    new TextPopupMenu( ( JTextField )( ( DefaultCellEditor )deviceTable.getDefaultEditor( String.class ) )
        .getComponent() );
    deviceTable.setDefaultEditor( Integer.class, editor );
    JScrollPane scrollPane = new JScrollPane( deviceTable );
    tablePanel = new JPanel( new BorderLayout() );
    tablePanel.setBorder( BorderFactory.createTitledBorder( "Device Parameters" ) );
    tablePanel.add( scrollPane, BorderLayout.CENTER );
    mainPanel.add( tablePanel, "1, 7, 3, 7" );
    Dimension d = deviceTable.getPreferredScrollableViewportSize();
    d.height = deviceTable.getRowHeight() * 4;
    deviceTable.setPreferredScrollableViewportSize( d );

    label = new JLabel( "Default Fixed Data:", SwingConstants.RIGHT );
    mainPanel.add( label, "1, 9" );
    rawHexData = new JTextField();
    rawHexData.getDocument().addDocumentListener( this );
    new TextPopupMenu( rawHexData );
    mainPanel.add( rawHexData, "3, 9" );

    // Command Parameter table
    commandModel = new ParameterTableModel( protocol, ParameterTableModel.Type.COMMAND );

    commandTable = new JTableX( commandModel );
    commandTable.setDefaultEditor( Integer.class, editor );
    new TextPopupMenu( ( JTextField )( ( DefaultCellEditor )commandTable.getDefaultEditor( String.class ) )
        .getComponent() );
    scrollPane = new JScrollPane( commandTable );
    tablePanel = new JPanel( new BorderLayout() );
    tablePanel.setBorder( BorderFactory.createTitledBorder( "Command Parameters" ) );
    tablePanel.add( scrollPane, BorderLayout.CENTER );
    mainPanel.add( tablePanel, "1, 11, 3, 11" );
    d = commandTable.getPreferredScrollableViewportSize();
    d.height = commandTable.getRowHeight() * 4;
    commandTable.setPreferredScrollableViewportSize( d );

    label = new JLabel( "Command Index:", SwingConstants.RIGHT );
    mainPanel.add( label, "1, 13" );
    cmdIndex = new JSpinner( new SpinnerNumberModel( protocol.getCmdIndex(), 0, protocol.getDefaultCmd().length() - 1,
        1 ) );
    cmdIndex.addChangeListener( this );
    mainPanel.add( cmdIndex, "3, 13" );

    buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );

    view = new JButton( "View Ini" );
    view.setToolTipText( "View the protocols.ini entry for this protocol." );
    view.addActionListener( this );
    view.setEnabled( false );
    buttonPanel.add( view );

    buttonPanel.add( Box.createHorizontalGlue() );

    ok = new JButton( "OK" );
    ok.addActionListener( this );
    buttonPanel.add( ok );

    cancel = new JButton( "Cancel" );
    cancel.addActionListener( this );
    buttonPanel.add( cancel );

    leftPanel.add( buttonPanel, BorderLayout.SOUTH );

    Hex id = protocol.getID();
    pid.setValue( id );
    rawHexData.setText( protocol.getFixedData( new Value[ 0 ] ).toString() );

    d = rightPanel.getPreferredSize();
    d.width = (int)(leftPanel.getPreferredSize().width * 0.75 );
    rightPanel.setPreferredSize( d );
    
    pack();
    Rectangle rect = getBounds();
    int x = rect.x - rect.width / 2;
    int y = rect.y - rect.height / 2;
    setLocation( x, y );
  }
  
  public void setForCustomCode()
  {
//    pid.setEditable( false );
//    pid.setEnabled( false );
    
    deviceTable.setEnabled( false );
    deviceTable.setForeground( Color.GRAY );

    commandTable.setEnabled( false );
    commandTable.setForeground( Color.GRAY );

    rawHexData.setEnabled( false );
    cmdIndex.setEnabled( false );
  
    enableButtons();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    if ( source == importButton )
    {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable clipData = clipboard.getContents( clipboard );
      if ( clipData != null )
      {
        try
        {
          if ( clipData.isDataFlavorSupported( DataFlavor.stringFlavor ) )
          {
            String s = ( String )( clipData.getTransferData( DataFlavor.stringFlavor ) );
            importProtocolCode( s );
          }
        }
        catch ( Exception ex )
        {
          ex.printStackTrace( System.err );
        }
      }
      if ( codeTable.getSelectedRowCount() == 1 )
      {
        assemblerModel.disassemble( protocol, procs[ codeTable.getSelectedRow() ] );
      }
    }
    else if ( source == view )
    {
      JTextArea ta = new JTextArea( protocol.getIniString( false, pid.isEnabled() ), 10, 70 );
      new TextPopupMenu( ta );
      ta.setEditable( false );
      JOptionPane.showMessageDialog( this, new JScrollPane( ta ), "Protocol.ini entry text", JOptionPane.PLAIN_MESSAGE );
    }
    else if ( source == ok )
    {
      userAction = JOptionPane.OK_OPTION;
      setVisible( false );
      dispose();
    }
    else if ( source == cancel )
    {
      userAction = JOptionPane.CANCEL_OPTION;
      setVisible( false );
      dispose();
    }
  }

  /**
   * Gets the protocol.
   * 
   * @return the protocol
   */
  public ManualProtocol getProtocol()
  {
    if ( userAction != JOptionPane.OK_OPTION )
      return null;

    // protocol.setDeviceParms( deviceParms );
    // protocol.setDeviceTranslators( deviceTranslators );
    // protocol.setCommandParms( cmdParms );
    // protocol.setCommandTranslators( cmdTranslators );
    protocol.setRawHex( new Hex( rawHexData.getText() ) );

    return protocol;
  }

  // PropertyChangeListener methods
  /*
   * (non-Javadoc)
   * 
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
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
            String message = "There is a Device Upgrade that is using a protocol upgrade with\n"
                           + "PID " + id + " Do you want to abort this PID choice and enter\n"
                           + "a different one?  If so, please press OK.\n\n"
                           + "If you want to edit that protocol code, also press OK, then exit\n"
                           + "this dialog, change to the Devices page and edit the protocol of\n"
                           + "the device upgrade from there.\n\n"
                           + "To continue, press CANCEL but you will be creating a Manual Protocol\n"
                           + "that cannot be accessed while that Device Upgrade is present.";
            exit = ( JOptionPane.showConfirmDialog( null, message, title, 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE ) == JOptionPane.OK_OPTION );
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
            enableButtons();
            return;
          }         
        }
      }
      protocol.setID( id );
    }
    enableButtons();
  }

  protected void enableButtons()
  {
    if ( deviceTable.isEnabled() )
    {
      // Normal Manual Settings usage
      Hex id = ( Hex )pid.getValue();
      boolean flag = ( id != null ) && ( id.length() != 0 ) && protocol.hasAnyCode();
      ok.setEnabled( flag );
      view.setEnabled( flag );
    }
    else
    {
      // Custom Code usage
      ok.setEnabled( true );
      view.setEnabled( false );
    }
  }

  // DocumentListener methods
  /**
   * Document changed.
   * 
   * @param e
   *          the e
   */
  public void documentChanged( DocumentEvent e )
  {
    Document doc = e.getDocument();

    if ( doc == name.getDocument() )
    {
      protocol.setName( name.getText() );
    }
    else if ( doc == rawHexData.getDocument() )
    {
      protocol.setRawHex( new Hex( rawHexData.getText() ) );
    }
    enableButtons();
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
   */
  public void changedUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
   */
  public void insertUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
   */
  public void removeUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  /**
   * The Class CodeTableModel.
   */
  public class CodeTableModel extends AbstractTableModel
  {

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount()
    {
      return procs.length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount()
    {
      return colNames.length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    public String getColumnName( int col )
    {
      return colNames[ col ];
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
     */
    public Class< ? > getColumnClass( int col )
    {
      return classes[ col ];
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
     */
    public boolean isCellEditable( int row, int col )
    {
      return ( col == 1 );
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt( int row, int col )
    {
      if ( col == 0 )
        return procs[ row ];
      else
        return protocol.getCode( procs[ row ] );
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
     */
    public void setValueAt( Object value, int row, int col )
    {
      if ( col == 1 )
      {
        Hex newCode = ( Hex )value;
        if ( ( newCode != null ) && ( newCode.length() != 0 ) )
        {
          if ( !protocol.hasAnyCode() )
          {
            int fixedDataLength = Protocol.getFixedDataLengthFromCode( procs[ row ].getEquivalentName(), newCode );
            rawHexData.setText( Hex.toString( new short[ fixedDataLength ] ) );
            ArrayList< Value > devParms = new ArrayList< Value >();
            Value zero = new Value( 0 );
            for ( int i = 0; i < fixedDataLength; ++i )
              devParms.add( zero );
            int cmdLength = Protocol.getCmdLengthFromCode( procs[ row ].getEquivalentName(), newCode );
            DefaultComboBoxModel comboModel = new DefaultComboBoxModel();
            for ( int i = 0; i < cmdLength; ++i )
            {
              comboModel.addElement( importButton );
            }
            SpinnerNumberModel spinnerModel = ( SpinnerNumberModel )cmdIndex.getModel();
            spinnerModel.setMaximum( cmdLength - 1 );
            protocol.createDefaultParmsAndTranslators( cmdLength << 4, false, false, 8, devParms, new short[ 0 ], 8 );
            deviceModel.fireTableDataChanged();
            commandModel.fireTableDataChanged();
          }
        }
        else if ( codeWhenNull != null )
        {
          String title = "Code deletion";
          String message = "This protocol is not built in to the remote.  Do you want to restore\n"
                         + "the code to the standard code for this protocol?\n\n"
                         + "If you select NO then the protocol upgrade for this device upgrade\n"
                         + "will be deleted.  The device upgrade will not function until you\n"
                         + "restore the protocol upgrade, which you may do by deleting this\n"
                         + "null entry and answering this question again.";
          boolean restore = ( JOptionPane.showConfirmDialog( null, message, title, JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE ) == JOptionPane.YES_OPTION );
          if ( restore )
          {
            value = codeWhenNull;
          }
          else
          {
            value = new Hex();
          }
        }
        protocol.setCode( ( Hex )value, procs[ row ] );
        fireTableRowsUpdated( row, row );
        enableButtons();
      }
    }
  }

  /**
   * Import protocol code.
   * 
   * @param string
   *          the string
   */
  private void importProtocolCode( String string )
  {
    StringTokenizer st = new StringTokenizer( string, "\n" );
    String text = null;
    String processor = null;
    String pidStr = null;
    while ( st.hasMoreTokens() )
    {
      while ( st.hasMoreTokens() )
      {
        text = st.nextToken().toUpperCase();
        System.err.println( "got '" + text );
        if ( text.startsWith( "UPGRADE PROTOCOL 0 =" ) )
        {
          StringTokenizer st2 = new StringTokenizer( text, "()=" );
          st2.nextToken(); // discard everything before the =
          pidStr = st2.nextToken().trim();
          System.err.println( "Imported pid is " + pidStr );
          processor = st2.nextToken().trim();
          System.err.println( "processorName is " + processor );
          if ( processor.startsWith( "S3C8" ) )
            processor = "S3C80";
          else if ( processor.startsWith( "S3F8" ) )
            processor = "S3F80";
          if ( st2.hasMoreTokens() )
          {
            String importedName = st2.nextToken().trim();
            System.err.println( "importedName is " + importedName );
          }
          break;
        }
      }
      if ( st.hasMoreTokens() )
      {
        text = st.nextToken(); // 1st line of code
        while ( st.hasMoreTokens() )
        {
          String temp = st.nextToken();
          if ( temp.trim().equals( "End" ) )
            break;
          text = text + ' ' + temp;
        }
        System.err.println( "getting processor with name " + processor );
        Processor p = ProcessorManager.getProcessor( processor );
        if ( p != null )
        {
          // processor = p.getFullName();
          processor = p.getEquivalentName();
          p = ProcessorManager.getProcessor( processor );
          pid.setValue( new Hex( pidStr ) );
        }
        System.err.println( "Adding code for processor " + processor );
        System.err.println( "Code is " + text );
        for ( int i = 0; i < procs.length; i++ )
        {
          if ( procs[ i ] == p )
            codeModel.setValueAt( new Hex( text ), i, 1 );
        }
      }
    }
  }

  /** The protocol. */
  private ManualProtocol protocol = null;

  /** The code model. */
  private CodeTableModel codeModel = null;

  /** The code table. */
  private JTableX codeTable = null;

  /** The device model. */
  private ParameterTableModel deviceModel = null;

  /** The device table. */
  private JTableX deviceTable = null;

  /** The command model. */
  private ParameterTableModel commandModel = null;

  /** The command table. */
  private JTableX commandTable = null;

  /** The name. */
  private JTextField name = null;

  /** The pid. */
  public JFormattedTextField pid = null;

  /** The raw hex data. */
  private JTextField rawHexData = null;

  private JSpinner cmdIndex = null;

  /** The import button. */
  private JButton importButton = null;

  /** The view. */
  private JButton view = null;

  /** The ok. */
  private JButton ok = null;

  /** The cancel. */
  private JButton cancel = null;

  /** The user action. */
  private int userAction = JOptionPane.CANCEL_OPTION;
  // private final static Object[] typeChoices = { "Numeric entry", "Drop-down list", "Check-box" };
  /** The Constant colNames. */
  private final static String[] colNames =
  {
      "Processor", "Protocol Code"
  };

  /** The Constant classes. */
  private final static Class< ? >[] classes =
  {
      Processor.class, Hex.class
  };

  /** The procs. */
  private static Processor[] procs = new Processor[ 0 ];

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  @Override
  public void stateChanged( ChangeEvent arg0 )
  {
    if ( protocol.setCmdIndex( ( ( Integer )cmdIndex.getValue() ).intValue() ) )
    {
      commandModel.fireTableDataChanged();
    }

  }
  
  public Hex codeWhenNull = null;
  
  public RemoteConfiguration remoteConfig = null;
  
  private AssemblerTableModel assemblerModel = new AssemblerTableModel();
  private JP1Table assemblerTable = null;

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if ( !e.getValueIsAdjusting() && codeTable.getSelectedRowCount() == 1 )
    {
      assemblerModel.disassemble( protocol, procs[ codeTable.getSelectedRow() ] );
    }
  }
  
}


