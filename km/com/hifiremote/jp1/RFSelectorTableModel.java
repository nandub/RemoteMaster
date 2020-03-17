package com.hifiremote.jp1;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.hifiremote.jp1.Remote.RFSelector;

public class RFSelectorTableModel extends JP1TableModel< RFSelector >
{
  private static final String[] colNames =
  {
    "#", "Selector", "IR Type", "<html>IR Setup<br>Code</html>", "RF Type", "<html>RF Setup<br>Code</html>",
  };
  
  private static String[] colPrototypeNames =
  {
    " 00 ", "Button____", "__VCR/DVD__", "Setup", "__VCR/DVD__", "Setup"
  };
  
  private static final Class< ? >[] colClasses =
  {
    Integer.class, String.class, DeviceType.class, SetupCode.class, DeviceType.class, SetupCode.class
  };
  
  public RFSelectorTableModel()
  {
    deviceTypeEditor = new DefaultCellEditor( deviceTypeBox );
    deviceTypeEditor.setClickCountToStart( RMConstants.ClickCountToStart );
  }
  
  @Override
  public String getColumnName( int col )
  {
    return colNames[ col ];
  }
  
  @Override
  public Class< ? > getColumnClass( int col )
  {
    return colClasses[  col ];
  }

  @Override
  public int getColumnCount()
  {
    return 6;
  }

  @Override
  public Object getValueAt( int row, int col )
  {
    RFSelector rfSel = getRow( row );
    switch ( col )
    {
      case 0:
        return new Integer( row + 1 );
      case 1:
        return rfSel.btn;
      case 2:
        return rfSel.irDevType;
      case 3:
        return rfSel.irCode;
      case 4:
        return rfSel.rfDevType;
      case 5:
        return rfSel.rfCode;
      default:
        return null;
    }
  }
  
  @Override
  public void setValueAt( Object value, int row, int col )
  {
    RFSelector rfSel = getRow( row );
    switch ( col )
    {
      case 2:
        rfSel.irDevType = ( DeviceType )value;
        return;
      case 3:
        if ( value instanceof String )
          rfSel.irCode = new SetupCode( ( String )value, false );
        else
          rfSel.irCode = ( SetupCode )value;
        return;
      case 4:
        rfSel.rfDevType = ( DeviceType )value;
        return;
      case 5:
        if ( value instanceof String )
          rfSel.rfCode = new SetupCode( ( String )value, false );
        else
          rfSel.rfCode = ( SetupCode )value;
        return;
    }
    return;
  }

  @Override
  public String getColumnPrototypeName( int col )
  {
    return colPrototypeNames[ col ];
  }
  
  @Override
  public TableCellRenderer getColumnRenderer( int col )
  {
    switch ( col )
    {
      case 0:
        return new RowNumberRenderer();
      case 3:
      case 5:
        return setupCodeRenderer;
    }
    return null;
  }
  
  public void set( RemoteConfiguration remoteConfig, DeviceButton devBtn )
  {
    this.devBtn = devBtn;
    Remote remote = remoteConfig.getRemote();
    setupCodeRenderer = new SetupCodeRenderer( remoteConfig );
    setupCodeEditor = new SetupCodeEditor( setupCodeRenderer );
    DefaultComboBoxModel< DeviceType > comboModel = new DefaultComboBoxModel< DeviceType >( remote.getAllDeviceTypes() );
    deviceTypeBox.setModel( comboModel );
    if ( devBtn == null || devBtn.getRfSelectors() == null )
    {
      setData( new RFSelector[ 0 ] );
    }
    else
    {
      setData( devBtn.getRfSelectors() );
    }
  }
  
  @Override
  public boolean isCellEditable( int row, int col )
  {
    return col > 1;
  }
  
  @Override
  public TableCellEditor getColumnEditor( int col )
  { 
    switch ( col )
    {
      case 2:
      case 4:
        return deviceTypeEditor;
      case 3:
      case 5:
        return setupCodeEditor;
    }
    return null;
  }
  
  public DeviceButton getDevBtn()
  {
    return devBtn;
  }

  private DeviceButton devBtn = null;
  private SetupCodeRenderer setupCodeRenderer = null;
  private DefaultCellEditor deviceTypeEditor = null;
  private JComboBox< DeviceType > deviceTypeBox = new JComboBox< DeviceType >();
  private SetupCodeEditor setupCodeEditor = null;
  
}
