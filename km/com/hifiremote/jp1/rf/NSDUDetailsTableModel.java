package com.hifiremote.jp1.rf;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.JP1TableModel;
import com.hifiremote.jp1.rf.Npdu.NSDUCommand;
import com.hifiremote.jp1.rf.Npdu.NSDUType;
import com.hifiremote.jp1.rf.Npdu.NSPrimitive;

public class NSDUDetailsTableModel extends JP1TableModel< NSPrimitive >
{
  private static String[] colNames =
  {
    "#", "Property", "Hex Value", "Interpretation"
  };
  
  private static String[] rowNames = 
  {
    "Command ID", "Status", "Alloc. Network Address", "Network Address", "Node Capabilities",
    "Vendor ID", "Vendor String", "User String", "Dev Type List",
    "Profile ID List", "Requested Dev Type", "Key Exch Transfer Count",
    "Discovery Req LQI", "Sequence Number", "Seed Data", "Ping Options", "Ping Payload",
    "Data", "RAW NSDU:", "Encrypted Payload", "Msg Integrity Field", "Decrypted Payload",
    "", "DECRYPT PARSED:"
  };
  
  private static String[] deviceNames = 
  {
    "REMOTE CONTROL", "TELEVISION", "PROJECTOR", "PLAYER", "RECORDER", "VIDEO PLAYER RECORDER",
    "AUDIO PLAYER RECORDER", "AUDIO VIDEO RECORDER", "SET TOP BOX", "HOME THEATER SYSTEM", 
    "MEDIA CENTER PC", "GAME CONSOLE", "SATELLITE RADIO RECEIVER", "IR EXTENDER", "MONITOR", 
    "GENERIC", "WILDCARD"
  };
  
  private static String[] profileNames = 
  {
    "GDP", "ZRC", "ZID", "ZRC20", "UNKNOWN", "VENDOR"
  };
  
  private String getDeviceName( int devType )
  {
    if ( devType > 0 && devType < 16 )
      return deviceNames[ devType - 1 ];
    else if ( devType == 0xFE || devType == 0xFF )
      return deviceNames[ devType - 0xEF ];
    else
      return null;
  }
  
  private String getProfileName( int profileID )
  {
    if ( profileID < 4 )
      return profileNames[ profileID ];
    else if ( profileID < 0xC0 )
      return profileNames[ 4 ];
    else
      return profileNames[ 5 ];
  }
  
  public String getDeviceNameList( Hex devTypes )
  {
    StringBuilder sb = new StringBuilder();
    int n = 0;
    for ( int devType : devTypes.getData() )
    {
      String s = getDeviceName( devType );
      if ( s != null )
      {
        if ( n++ > 0 ) sb.append( ", " );
        sb.append( s );
      }
    }
    return sb.toString();
  }
  
  public String getProfileNameList( Hex profiles )
  {
    StringBuilder sb = new StringBuilder();
    int n = 0;
    for ( int profileID : profiles.getData() )
    {
      String s = getProfileName( profileID );
      if ( s != null )
      {
        if ( n++ > 0 ) sb.append( ", " );
        sb.append( s );
      }
    }
    return sb.toString();
  }
  
  public String getNodeCapabilities( int nodeCaps )
  {
    StringBuilder sb = new StringBuilder();
    sb.append( ( nodeCaps & 1 ) == 1 ? "Is Target, " : "Is Controller, " );
    sb.append( ( nodeCaps & 2 ) == 2 ? "Mains power, " : "Battery power, " );
    sb.append( ( nodeCaps & 4 ) == 4 ? "Can encrypt, " : "Cannot encrypt, " );
    sb.append( ( nodeCaps & 8 ) == 8 ? "Can switch channel" : "Cannot switch channel" );
    return sb.toString();
  }
  
  private String getInterpretation( int row )
  {
    String descr = null;
    Hex hex = getRowHex( row );
    if ( hex == null )
    {
      return null;
    }
    switch ( getEffectiveRow( row ) )
    {
      case 0:
        descr = NSDUCommand.values()[ hex.getData()[ 0 ] - 1 ].toString();
        break;
      case 1:
        int code = hex.getData()[ 0 ];
        descr = code == 0 ? "SUCCESS" : "ERROR CODE" + code;
        break;
      case 2:
      case 3:
      case 5:
        descr = RfTools.getAddrString( hex );
        break;
      case 4:
        descr = getNodeCapabilities( hex.getData()[ 0 ] );
        break;
      case 6:
      case 7:
        descr = new String( hex.toByteArray() );
        break;
      case 8:
      case 10:
        descr =  getDeviceNameList( hex );
        break;
      case 9:
        descr = getProfileNameList( hex );
        break;
      case 11:
        descr = "" + ( hex.getData()[ 0 ] + 1 );
        break;
      case 13:
        descr = "Key Seed " + ( hex.getData()[ 0 ] + 1 );
        break;
      case 14:
        descr = "Key Seed data (80 bytes)";
        break;
      default:
        descr = hex.toString();
    }
    return descr;
  }
  
  private int getEffectiveRow( int row )
  {
    NSDUType type = prim.type;
    NSDUCommand cmd = prim.cmd;
    if ( type == NSDUType.COMMAND )
    {
      if ( cmd == NSDUCommand.PING_REQ || cmd == NSDUCommand.PING_RSP )
      {
        return row < 6 ? row + 18 : row == 6 ? 0 : row + 8;
      }     
      if ( row == 0 )
      {
        return 0;
      }
      switch ( cmd )
      {
        case DISCOVERY_REQ:
          return row + 3;
        case DISCOVERY_RSP:
          return row == 1 ? 1 : row == 8 ? 12 : row + 2;
        case PAIR_REQ:
          return row == 1 ? 3 : row == 8 ? 11 : row + 2;
        case PAIR_RSP:
          return row;
        case KEYSEED:
          return row + 12;
        default:
           return 0; 
      }
    }
    else
    {
      return row < 6 ? row + 18 : 17; 
    }
  }
  
  private static String[] colPrototypeNames =
  {
    " 00 ", "Name_of_the_property__________", "Value_of_the_property__",
    "Interpretation_____________"
  };
  
  private static final Class< ? >[] colClasses =
  {
    Integer.class, String.class, String.class, String.class
  };
  
  private static boolean[] colWidths =
  {
    true, true, false, false
  };
  
  @Override
  public int getRowCount()
  {
    if ( prim == null )
    {
      return 0;
    }
    NSDUType type = prim.type;
    NSDUCommand cmd = prim.cmd;
    if ( type == NSDUType.COMMAND )
    {
      switch ( cmd )
      {
        case DISCOVERY_REQ:
          return 8;
        case DISCOVERY_RSP:
        case PAIR_REQ:
          return 9;
        case PAIR_RSP:
          return 10;
        case KEYSEED:
          return 3;
        case PING_REQ:
        case PING_RSP:
          return 9;
        default:
           return 0; 
      }
    }
    else
    {
      return 7;
    }
  }
   
  @Override
  public int getColumnCount()
  {
    return colNames.length;
  }
  
  public String getRowName( int row )
  {
    return rowNames[ getEffectiveRow( row ) ];
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
  
  private Hex[] getInfo( Hex in )
  {
    Hex[] out = new Hex[ 7 ];
    out[ 0 ] = in.subHex( 0, 1 );
    out[ 1 ] = in.subHex( 1, 2 );
    out[ 2 ] = in.subHex( 3, 7 );
    int appCaps = in.getData()[ 10 ];
    int pos = 11;
    if ( ( appCaps & 1 ) == 1 )
    {
      out[ 3 ] = in.subHex( pos, 15 );
      pos += 15;
    }
    int len = ( appCaps >> 1 ) & 3;
    if ( len > 0 )
    {
      out[ 4 ] = in.subHex( pos, len );
      pos += len;
    }
    len = ( appCaps >> 4 ) & 7;
    if ( len > 0 )
    {
      out[ 5 ] = in.subHex( pos, len );
      pos += len;
    }
    out[ 6 ] = new Hex( String.format( "%02X", pos ) );
    return out;
  }
  
  private Hex getRowHex( int row )
  {
    Hex nsdu = prim.nsdu;
    NSDUType type = prim.type;
    NSDUCommand cmd = prim.cmd;
    Hex[] info = null;
    int pos = 0;
    Hex val = null;
    if ( type == NSDUType.COMMAND )
    {
      if ( cmd == NSDUCommand.PING_REQ || cmd == NSDUCommand.PING_RSP )
      {
        int len = nsdu.length() - 2;
        val = row == 0 ? null : row == 1 ? prim.rawNsdu : row == 2 ? prim.authData
            : row == 3 ? nsdu : row < 6 ? null : row == 6 ? nsdu.subHex( 0, 1 )
            : row == 7 ? nsdu.subHex( 1, 1 ) : nsdu.subHex( 2, len );
        return val;
      }
      else if ( row == 0 )
      {
        return nsdu.subHex( 0, 1 );
      }
      switch ( cmd )
      {  
        case DISCOVERY_REQ:
          info = getInfo( nsdu.subHex( 1 ) );
          pos = info[ 6 ].getData()[ 0 ];
          val = row < 7 ? info[ row - 1 ] : nsdu.subHex( pos + 1, 1 );
          break;
        case DISCOVERY_RSP:
          info = getInfo( nsdu.subHex( 2 ) );
          pos = info[ 6 ].getData()[ 0 ];
          val = row == 1 ? nsdu.subHex( 1, 1 ) : row < 8 ? info[ row - 2 ] 
              : nsdu.subHex( pos + 2, 1 );
          break;
        case PAIR_REQ:
          info = getInfo( nsdu.subHex( 3 ) );
          pos = info[ 6 ].getData()[ 0 ];
          val = row == 1 ? nsdu.subHex( 1, 2 ) : row < 8 ? info[ row - 2 ] 
              : nsdu.subHex( pos + 3, 1 );
          break;
        case PAIR_RSP:
          info = getInfo( nsdu.subHex( 6 ) );
          pos = info[ 6 ].getData()[ 0 ];
          val = row == 1 ? nsdu.subHex( 1, 1 ) : row == 2 ? nsdu.subHex( 2, 2 )
              : row == 3 ? nsdu.subHex( 4, 2 ) : info[ row - 4 ]; 
          break;
        case KEYSEED:
          val = row == 1 ? nsdu.subHex( 1, 1 ) : nsdu.subHex( 2 );
          break;
      }
      return val;
    }
    else
    {
      val = row == 0 ? null : row == 1 ? prim.rawNsdu : row == 2 ? prim.authData
          : row == 3 ? nsdu : row < 6 ? null : nsdu;
      return val;
    }
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
        Hex hex = getRowHex( row );
        return hex == null ? null : hex.toString();
      case 3:
        return getInterpretation( row );
    }
    return null;
  }
  
  public void setNSPrimitive( NSPrimitive prim )
  {
    this.prim = prim;
  }

  private NSPrimitive prim = null;
}
