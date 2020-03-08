package com.hifiremote.jp1.rf;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.JP1TableModel;
import com.hifiremote.jp1.rf.Npdu.NSDUCommand;
import com.hifiremote.jp1.rf.Npdu.NSDUDirection;
import com.hifiremote.jp1.rf.Npdu.NSDUType;
import com.hifiremote.jp1.rf.Npdu.NSPrimitive;

public class NSPrimitiveTableModel extends JP1TableModel< NSPrimitive >
{
  private static String[] colNames =
  {
    "#", "Dir'n", "RF Remote", "<html>Rpt<br>count</html>", "<html>Frame<br>counter</html>", 
    "Secured", "Type", "Command", "<html>Profile<br>Id</html>", "<html>Vendor<br>Id</html>", 
    "NSDU"
  };
  
  private static String[] colPrototypeNames =
  {
    " 00 ", "OUT_", "Remote name__", "Count", "_00000000_", "Secured_", "COMMAND__", "COMMAND NAME__", 
    "Profile_", "00_00_", "_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00"
  };
  
  private static final Class< ? >[] colClasses =
  {
    Integer.class, NSDUDirection.class, String.class, Integer.class, Integer.class, 
    Boolean.class, NSDUType.class, NSDUCommand.class, Hex.class, String.class, String.class
  };
  
  private static boolean[] colWidths =
    {
      true, true, false, true, true, true, true, true, true, true, false
    };
   
  
  @Override
  public int getColumnCount()
  {
    return colNames.length;
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
  public Object getValueAt( int row, int col )
  {
    NSPrimitive prim = getRow( row );
    switch ( col )
    {
      case 0:
        return new Integer( row + 1 );
      case 1:
        return prim.direction;
      case 2:
        return prim.rfRemote != null ? prim.rfRemote.name : null;
      case 3:
        return prim.rptCount > 0 ? prim.rptCount : null ;
      case 4:
        return Long.parseLong( RfTools.getAddrString( prim.frameCtrHex ), 16 );
      case 5:
        return prim.secured;
      case 6:
        return prim.type;
      case 7:
        return prim.cmd;
      case 8:
        return prim.profileID;
      case 9:
        Hex vid = prim.vendorID;
        return vid == null ? null : RfTools.getAddrString( vid );
      case 10:
        return prim.valid ? prim.nsdu.toString() : prim.getErrorMessage();
    }
    return null;
  }

  
  

}
