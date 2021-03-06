package com.hifiremote.jp1;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import com.hifiremote.jp1.ProtocolDataPanel.DisplayArea;

public class ManualDevicePanel extends JPanel implements ChangeListener, DocumentListener
{
  public ManualDevicePanel()
  {
    ManualProtocol protocol = new ManualProtocol( null, null );
    double scale = 0.75;
    double b = 5; // space between rows and around border
    double c = 10; // space between columns
    double pr = TableLayout.PREFERRED;
    double pf = TableLayout.FILL;
    double size2[][] =
      {
          {
              b, pr, c, pf, b
          }, // cols
          {
              b, pr, b, pr, b, pr, b, pr, b, pr, pf, pr, b
          }
      // rows
      };
    
    // Device Parameter Table on Device Data tab
    setLayout( new TableLayout( size2 ) );
    deviceModel = new ParameterTableModel( protocol, ParameterTableModel.Type.DEVICE );
    deviceTable = new JTableX( deviceModel );
    SpinnerCellEditor editor = new SpinnerCellEditor( 0, 8, 1 );
    new TextPopupMenu(
        ( JTextField )( ( DefaultCellEditor )deviceTable.getDefaultEditor( String.class ) ).getComponent() );
    deviceTable.setDefaultEditor( Integer.class, editor );
    JScrollPane scrollPane = new JScrollPane( deviceTable );
    JPanel tablePanel = new JPanel( new BorderLayout() );
    tablePanel.setBorder( BorderFactory.createTitledBorder( "Device Parameters" ) );
    tablePanel.add( scrollPane, BorderLayout.CENTER );
    add( tablePanel, "1, 1, 3, 1" );
    Dimension d = deviceTable.getPreferredScrollableViewportSize();
    d.height = deviceTable.getRowHeight() * 4;
    d.width = ( int )( d.width * scale );
    deviceTable.setPreferredScrollableViewportSize( d );
//    deviceTable.setLongToolTipTimeout();

    JLabel label = new JLabel( "Default Fixed Data:", SwingConstants.RIGHT );
    add( label, "1, 3" );
    rawHexData = new JTextField();
    rawHexData.getDocument().addDocumentListener( this );
    rawHexData.setText( protocol.getFixedData( new Value[ protocol.getFixedDataLength() ] ).toString() );
    new TextPopupMenu( rawHexData );
    add( rawHexData, "3, 3" );

    // Command Parameter table on Device Data tab
    commandModel = new ParameterTableModel( protocol, ParameterTableModel.Type.COMMAND );
    commandTable = new JTableX( commandModel );
    commandTable.setDefaultEditor( Integer.class, editor );
    new TextPopupMenu(
        ( JTextField )( ( DefaultCellEditor )commandTable.getDefaultEditor( String.class ) ).getComponent() );
    scrollPane = new JScrollPane( commandTable );
    tablePanel = new JPanel( new BorderLayout() );
    tablePanel.setBorder( BorderFactory.createTitledBorder( "Command Parameters" ) );
    tablePanel.add( scrollPane, BorderLayout.CENTER );
    add( tablePanel, "1, 5, 3, 5" );
    d = commandTable.getPreferredScrollableViewportSize();
    d.height = commandTable.getRowHeight() * 4;
    d.width = ( int )( d.width * scale );
    commandTable.setPreferredScrollableViewportSize( d );
//    commandTable.setLongToolTipTimeout();

    label = new JLabel( "Command Index:", SwingConstants.RIGHT );
    add( label, "1, 7" );
    cmdIndex = new JSpinner( new SpinnerNumberModel( protocol.getCmdIndex(), 0, protocol.getDefaultCmdLength() - 1,
        1 ) );
    cmdIndex.addChangeListener( this );
    add( cmdIndex, "3, 7" );
    
    String note = "If editing a Manual Protocol in RM, the Data Translators are "
        + "read-only and set automatically to match the parameter tables.\n\n"
        + "If editing a protocol in RMPB, the Data Translators panel is editable. "
        + "The parameter tables and Create Translators button are there to provide "
        + "an easy means to create simple translators.";
    DisplayArea noteArea = new DisplayArea( note, null );
    noteArea.setColumns( 40 );
    JPanel notePanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
    notePanel.add( noteArea );
    add( notePanel, "1, 9, 3, 9" );
  }
  
  public void setOwner( Object owner )
  {
    this.owner = owner;
    if ( !( owner instanceof ManualSettingsDialog ) )
    {
      return;
    }
    settingsDialog = ( ManualSettingsDialog )owner;
  }

  public void setProtocol( ManualProtocol protocol )
  {
    this.protocol = protocol;
    deviceModel = new ParameterTableModel( protocol, ParameterTableModel.Type.DEVICE );
    deviceTable.setModel( deviceModel );
    commandModel = new ParameterTableModel( protocol, ParameterTableModel.Type.COMMAND );
    commandTable.setModel( commandModel );
    SpinnerNumberModel spinnerModel = new SpinnerNumberModel( protocol.getCmdIndex(), 0, protocol.getDefaultCmdLength() - 1, 1 );
    cmdIndex.setModel( spinnerModel );
    rawHexData.setText( protocol.getFixedData( new Value[ protocol.getFixedDataLength() ] ).toString() );
    if ( settingsDialog != null )
    {
      deviceModel.setManualSettingsPanel( settingsDialog.getManualSettingsPanel() );
      commandModel.setManualSettingsPanel( settingsDialog.getManualSettingsPanel() );
    }
  }
  
  public void updateFixedData()
  {
    protocol.setRawHex( new Hex( rawHexData.getText() ) );
  }
  
  public void setForCustomCode()
  {
    deviceTable.setEnabled( false );
    deviceTable.setForeground( Color.GRAY );

    commandTable.setEnabled( false );
    commandTable.setForeground( Color.GRAY );

    rawHexData.setEnabled( false );
    cmdIndex.setEnabled( false );
    
    enableButtons();
  }
  
  public void update( String procName, Hex hex )
  {
    ManualSettingsPanel msp = settingsDialog != null ? settingsDialog.getManualSettingsPanel() : null;

    if ( msp != null && msp.isLoadInProgress() )
      return;
    int oldFixedDataLength = protocol.getDeviceTranslators().length;
    int oldCmdLength = protocol.getCmdTranslators().length;
    int fixedDataLength = Protocol.getFixedDataLengthFromCode( procName, hex );
    int cmdLength = Protocol.getCmdLengthFromCode( procName, hex );
    if ( msp != null && !msp.isLoadInProgress() && fixedDataLength == oldFixedDataLength 
        && cmdLength == oldCmdLength )
    {
      return;
    }
    else if ( settingsDialog != null && settingsDialog.getModeIndex() >= 0 
        && settingsDialog.getModeIndex() <= 1 && msp != null )
    {
      // This message is active for a manual protocol either new (0) or edited (1)
      String title = "Code update";
      String message = "The device and command translators have been reset to default\n"
          + "due to a change in the number of parameters.";
      JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
    }
    rawHexData.setText( Hex.toString( new short[ fixedDataLength ] ) );
    ArrayList< Value > devParms = new ArrayList< Value >();
    Value zero = new Value( 0 );
    for ( int i = 0; i < fixedDataLength; ++i )
      devParms.add( zero );
    SpinnerNumberModel spinnerModel = ( SpinnerNumberModel )cmdIndex.getModel();
    spinnerModel.setMaximum( cmdLength - 1 );
    protocol.createDefaultParmsAndTranslators( cmdLength << 4, false, false, 8, devParms, new short[ 0 ], 8 );
    deviceModel.fireTableDataChanged();
    commandModel.fireTableDataChanged();
    if ( settingsDialog != null )
    {
      deviceModel.setProtocolText( settingsDialog.getModeIndex() );
      commandModel.setProtocolText( settingsDialog.getModeIndex() );
    }
  }
  
  @Override
  public void stateChanged( ChangeEvent e )
  {
    if ( protocol.setCmdIndex( ( ( Integer )cmdIndex.getValue() ).intValue() ) )
    {
      commandModel.fireTableDataChanged();
    }
  }
  
  @Override
  public void insertUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  @Override
  public void removeUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }


  @Override
  public void changedUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }
  
  public void documentChanged( DocumentEvent e )
  {
    Document doc = e.getDocument();
    if ( doc == rawHexData.getDocument() )
    {
      protocol.setRawHex( new Hex( rawHexData.getText() ) );
      enableButtons();
      ManualSettingsPanel msp = null;
      if ( owner instanceof ManualSettingsDialog )
      {
        msp = ( ( ManualSettingsDialog )owner ).getManualSettingsPanel();
        msp.setChanged( true );
      }
      else if ( owner instanceof RMProtocolBuilder )
      {
        msp = ( ( RMProtocolBuilder )owner ).getManualSettingsPanel();
        msp.setChanged( true );
      }
    }
  }
  
  public void enableButtons()
  {
    if ( settingsDialog == null )
    {
      return;
    }
    ManualSettingsPanel p = settingsDialog.getManualSettingsPanel();
    JFormattedTextField pid  = p.getPid();

    if ( deviceTable.isEnabled() )
    {
      // Normal Manual Settings or Protocol Clone usage
      Hex id = ( Hex )pid.getValue();
      boolean flag = ( id != null ) && ( id.length() != 0 ) && protocol.hasAnyCode();
      settingsDialog.ok.setEnabled( flag );
    }
    else
    {
      // Custom Code usage
      settingsDialog.ok.setEnabled( true );
    }
  }

  public ManualSettingsDialog getSettingsDialog()
  {
    return settingsDialog;
  }
  
  public void stopEditing()
  {
    if ( commandTable.getCellEditor() != null )
    {
      commandTable.getCellEditor().stopCellEditing();
    }
    if ( deviceTable.getCellEditor() != null )
    {
      deviceTable.getCellEditor().stopCellEditing();
    }
  }

  private ParameterTableModel deviceModel = null;
  private JTableX deviceTable = null;
  private ParameterTableModel commandModel = null;
  private JTableX commandTable = null;
  private JTextField rawHexData = null;
  private JSpinner cmdIndex = null;
  private ManualProtocol protocol = null;
  private Object owner = null;
  private ManualSettingsDialog settingsDialog = null;
}
