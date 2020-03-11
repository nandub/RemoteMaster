package com.hifiremote.jp1;

import javax.swing.table.TableCellEditor;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.JP1TableModel;
import com.hifiremote.jp1.rf.RfTools;

public class RFVendorTableModel extends JP1TableModel< DeviceUpgrade >
{  
  private static String[] colNames =
  {
    "#", "Property", "Hex Value", "Interpretation"
  };
  
  private static String[] rowNames = 
  {
    "Vendor ID", "Vendor String", "User String"
  };
  
  private static String[] colPrototypeNames =
  {
    " 00 ", "Name_of_the_property__", "Value_of_the_property____", "Interpretation_______"
  };
  
  private static final Class< ? >[] colClasses =
  {
    Integer.class, String.class, Hex.class, String.class
  };
  
  private static boolean[] colWidths =
  {
    true, false, false, false
  };
  
  @Override
  public int getRowCount()
  {
    return rowNames.length;
  }
 
  @Override
  public int getColumnCount()
  {
    return colNames.length;
  }
  
  public String getRowName( int row )
  {
    return rowNames[ row ];
  }
  
  @Override
  public String getColumnName( int col )
  {
    return colNames[ col ];
  }
  
  @Override
  public String getColumnPrototypeName( int col )
  {
    return colPrototypeNames[ col ];
  }
  
  @Override
  public Class< ? > getColumnClass( int col )
  {
    return colClasses[ col ];
  }
  
  @Override
  public boolean isColumnWidthFixed( int col )
  {
    return colWidths[ col ];
  }
  
  @Override
  public boolean isCellEditable( int row, int col )
  {
    hexEditor.setDefaultHex( getRowHex( row ) );
    return col == 2;
  }
  
  @Override
  public TableCellEditor getColumnEditor( int col )
  {
    if ( col == 2 )
    {
      return hexEditor;
    }
    return null;
  }
  
  private Hex getRowHex( int row )
  {
    if ( upgrade == null )
    {
      return null;
    }
    Hex extraData = upgrade.getExtraData();
    switch ( row )
    {
      case 0:
        return extraData.subHex( 0, 2 );
      case 1:
        return extraData.subHex( 2, 7 );
      case 2:
        return extraData.subHex( 9, 15 );
    }
    return null;
  }
  
  private String getInterpretation( int row )
  {
    Hex hex = getRowHex( row );
    if ( hex == null )
    {
      return null;
    }
    return row == 0 ? String.format( "%02X%02X", hex.getData()[ 0 ], hex.getData()[ 1 ] ) : new String( hex.toByteArray() );
  }

  @Override
  public Object getValueAt( int row, int col )
  {
    switch ( col )
    {
      case 0:
        return new Integer( row + 1 );
      case 1:
        return getRowName( row );
      case 2:
        return getRowHex( row );
      case 3:
        return getInterpretation( row );
    }
    return null;
  }
  
  @Override
  public void setValueAt( Object value, int row, int col )
  {
    if ( col != 2 )
    {
      return;
    }
    Hex extraData = upgrade.getExtraData();
    Hex hex = ( Hex )value;
    switch ( row )
    {
      case 0:
        extraData.put( hex, 0 );
        return;
      case 1:
        extraData.put( hex, 2 );
        return;
      case 2:
        extraData.put( hex, 9 );
        return;   
    }
  }

  public void setDeviceUpgrade( DeviceUpgrade upgrade )
  {
    this.upgrade = upgrade;
  }

  private DeviceUpgrade upgrade = null;
  private HexEditor hexEditor = new HexEditor();
  

}
