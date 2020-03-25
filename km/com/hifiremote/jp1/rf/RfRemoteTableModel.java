package com.hifiremote.jp1.rf;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.JP1TableModel;
import com.hifiremote.jp1.rf.RfRemote.Pairing;

public class RfRemoteTableModel extends JP1TableModel< RfRemote >
{
  private static String[] colNames =
  {
    "#", "Property", "Value"
  };
  
  private static String[] rowNames = 
  {
    "PROPERTIES OF REMOTE:", "IEEE Address", "PAN ID", "Network Address", "Vendor ID", 
    "Vendor String", "User String", "",
    "PROPERTIES OF DEVICE:", "IEEE Address", "PAN ID", "Network Address", "Vendor ID",
    "Vendor String", "User String", "",
    "COMMON PROPERTIES:", "Expected Channel", "Security Key"
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
    true, true, false
  };
  
  @Override
  public int getRowCount()
  {
    return rowNames.length;
  }
  
  @Override
  public RfRemote getRow( int row )
  {
    return array[ 0 ];
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
  
  private String getRowValue( int row, RfRemote rfRemote )
  {
    if ( rfRemote == null )
      return null;
    Pairing pair = pairIndex == null || pairIndex < 0 ? null : rfRemote.pairings.get( pairIndex );
    if ( row == 1 )
      return rfRemote.extAddr != null ? RfTools.getAddrString( rfRemote.extAddr )
          : "<not known: provisional registration>";
    if ( pair == null )
      return null;
    
    switch ( row )
    {
      case 2:
        return RfTools.getAddrString( pair.getPanID() );
      case 3:
        return RfTools.getAddrString( pair.getNwkAddr() );
      case 4:
        return RfTools.getAddrString( rfRemote.vendorID );
      case 5:
        return rfRemote.vendorString.toString() + " ("
            + RfTools.getASCIIString( rfRemote.vendorString ) + ")";
      case 6:
        return rfRemote.userString.toString() + " ("
            + RfTools.getASCIIString( rfRemote.userString ) + ")";
      case 9:
        return RfTools.getAddrString( pair.getPeerExtAddr() );
      case 10:
        return RfTools.getAddrString( pair.getPanID() );
      case 11:
        return RfTools.getAddrString( pair.getPeerNwkAddr() );
      case 12:
        return RfTools.getAddrString( pair.getPeerVendorID() );
      case 13:
        Hex pvs = pair.peerVendorString;
        return pvs == null ? "<not available>" : pvs.toString() + " ("
            + RfTools.getASCIIString( pvs ) + ")";
      case 14:
        Hex pus = pair.peerUserString;
        return pus == null ? "<not available>" : pus.toString() + " ("
            + RfTools.getASCIIString( pus ) + ")";
      case 17:
        return "" + pair.getChannel();
      case 18:
        return pair.getSecurityKey().toString();
        
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
        return getRowValue( row, rfRemote );
    }
    return null;
  }

  public void setPairIndex( Integer pairIndex )
  {
    this.pairIndex = pairIndex;
  }

  public void setRfRemote( RfRemote rfRemote )
  {
    this.rfRemote = rfRemote;
  }

  private Integer pairIndex = null;
  private RfRemote rfRemote = null;

}
