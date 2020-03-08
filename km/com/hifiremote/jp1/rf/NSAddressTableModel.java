package com.hifiremote.jp1.rf;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.JP1TableModel;
import com.hifiremote.jp1.rf.Mpdu.MACAddrData;
import com.hifiremote.jp1.rf.Npdu.NSPrimitive;

public class NSAddressTableModel extends JP1TableModel< NSPrimitive >
{  
  private static String[] colNames =
  {
    "#", "Property", "Value"
  };
  
  private static String[] rowNames = 
  {
    "Channel", "Source Address", "Source PAN", "Destination Address", "Destination PAN"
  };
  
  private static String[] colPrototypeNames =
  {
    " 00 ", "Name_of_the_property__________", "Value_of_the_property____________"
  };
  
  private static final Class< ? >[] colClasses =
  {
    Integer.class, String.class, String.class
  };
  
  private static boolean[] colWidths =
  {
    true, false, false
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
  
  private Hex getRowHex( int row )
  {
    if ( prim == null || prim.addrData == null )
    {
      return null;
    }
    MACAddrData addrData = prim.addrData;
    switch ( row )
    {
      case 1:
        return addrData.srcAddr;
      case 2:
        return addrData.srcPAN;
      case 3:
        return addrData.destAddr;
      case 4:
        return addrData.destPAN;
    }
    return null;
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
        if ( prim == null )
        {
          return null;
        }
        if ( row == 0 )
        {
          int channelDesignator = prim.channelDesignator;
          if ( channelDesignator == 0 )
            return "Unspecified";
          else
            return "" + ( 10 + 5*channelDesignator );
        }
        Hex hex = getRowHex( row );
        return hex == null ? null : RfTools.getAddrString( hex );
    }
    return null;
  }

  public void setNSPrimitive( NSPrimitive prim )
  {
    this.prim = prim;
  }

  private NSPrimitive prim = null;
  

}
