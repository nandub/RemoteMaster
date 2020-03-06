package com.hifiremote.jp1.rf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.rf.Rf4ceAuthenticator.Source;

public class RfRemote
{
  public String name = null;
  public Hex extAddr = null;
  public int nodeCaps = 0;
  public Hex vendorID = null;
  public Hex vendorString = null;
  public Hex userString = null;
  public Hex devTypes = null;
  public Hex profiles = null;
  public List< Pairing > pairings = null;
  
  public boolean changed = false;  
  
  public static class Pairing
  {
    private Hex table;
    public Hex peerVendorString = null;
    public Hex peerUserString = null;
    public Hex peerProfiles = null;

    public Pairing()
    {
      table = new Hex( 0x27 );
      Arrays.fill( table.getData(), ( short )0 );
    }
    
    public Pairing( Hex table )
    {
      this.table = table;
    }
    
    public Hex getTable()
    {
      return table;
    }

    public void setTable( Hex table )
    {
      this.table = table;
    }
    
    public int getPairRef()
    {
      return table.getData()[ 0 ];
    }
    
    public void setPairRef( int ref )
    {
      table.getData()[ 0 ] = ( short )ref;
    }
    
    public Hex getNwkAddr()
    {
      return table.subHex( 1, 2 );
    }
    
    public void setNwkAddr( Hex nwkAddr )
    {
      table.put( nwkAddr, 1 );
    }
    
    public int getChannel()
    {
      return table.getData()[ 3 ];
    }
    
    public void setChannel( int channel )
    {
      table.getData()[ 3 ] = ( short )channel;
    }
    
    public Hex getPeerExtAddr()
    {
      return table.subHex( 4, 8 );
    }
    
    public void setPeerExtAddr( Hex extAddr )
    {
      table.put( extAddr, 4 );
    }
    
    public Hex getPanID()
    {
      return table.subHex( 12, 2 );
    }
    
    public void setPanID( Hex panID )
    {
      table.put( panID, 12 );
    }
    
    public Hex getPeerNwkAddr()
    {
      return table.subHex( 14, 2 );
    }
    
    public void setPeerNwkAddr( Hex nwkAddr )
    {
      table.put( nwkAddr, 14 );
    }
    
    public int getPeerNodeCaps()
    {
      return table.getData()[ 16 ];
    }
    
    public void setPeerNodeCaps( int nodeCaps)
    {
      table.getData()[ 16 ] = ( short )nodeCaps;
    }
    
    public Hex getSecurityKey()
    {
      if ( table.getData()[ 17 ] == 1 )
        return table.subHex( 18, 16 );
      else
        return null;
    }
    
    public void setSecurityKey( Hex key )
    {
      if ( key == null )
        table.getData()[ 17 ] = 0;
      else
      {
        table.getData()[ 17 ] = 1;
        table.put( key, 18 );
      }
    }
    
    public Hex getPeerVendorID()
    {
      return table.subHex( 34, 2 );
    }
    
    public void setPeerVendorID( Hex vid )
    {
      table.put( vid, 34 );
    }
    
    public Hex getPeerDevTypes()
    {
      return table.subHex( 36, 3 );
    }
    
    public void setPeerDevTypes( Hex devTypes )
    {
      // This construction allows devTypes size to be less than 3
      Hex empty = new Hex( "00 00 00" );
      table.put( empty, 36 );
      table.put( devTypes, 36 );
    }
  }
  
  public RfRemote()
  {
    pairings = new ArrayList< Pairing >();
  }

  public RfRemote( Hex extAddr )
  {
    this.extAddr = extAddr;
    pairings = new ArrayList< Pairing >();
  }

  public Hex getRfData( Source source, int pairIndex )
  {
    int appCaps = 0;
    int rfDataSize = 11;
    int len = 0;
    Hex[] data = null;
    Hex nodeHex = null;
    if ( source == Source.CONTROLLER )
    {
      nodeHex = new Hex( new short[]{ ( short )nodeCaps } );
      data = new Hex[]{ nodeHex, vendorID, vendorString, userString, devTypes, profiles };
    }
    else if ( source == Source.TARGET )
    {
      Pairing pair = pairings.get( pairIndex );
      nodeHex = new Hex( new short[]{ ( short )pair.getPeerNodeCaps() } );
      data = new Hex[]{ nodeHex, pair.getPeerVendorID(), pair.peerVendorString, pair.peerUserString,
          pair.getPeerDevTypes(), pair.peerProfiles };
    }
    if ( data[ 3 ] != null )
    {
      appCaps = 1;
      rfDataSize += 15;
    }
    if ( data[ 4 ] != null )
    {
      len = data[ 4 ].length();
      if ( len > 3 ) return null;  // illegal size
      appCaps |= len << 1;
      rfDataSize += len;
    }
    if ( data[ 5 ] != null )
    {
      len = data[ 5 ].length();
      if ( len > 7 ) return null;  // illegal size
      appCaps |= len << 4;
      rfDataSize += len;
    }
    Hex rfData = new Hex( rfDataSize );
    if ( data[ 0 ] != null ) rfData.put( data[ 0 ], 0 );
    if ( data[ 1 ] != null ) rfData.put( data[ 1 ], 1 );
    if ( data[ 2 ] != null ) rfData.put( data[ 2 ], 3 );
    rfData.getData()[ 10 ] = ( short )appCaps;
    int pos = 11;
    if ( data[ 3 ] != null )
    {
      rfData.put( data[ 3 ], pos );
      pos += 15;
    }
    if ( data[ 4 ] != null )
    {
      rfData.put( data[ 4 ], pos );
      pos += data[ 4 ].length();
    }
    if ( data[ 5 ] != null )
    {
      rfData.put( data[ 5 ], pos );
      pos += data[ 5 ].length();
    }
    if ( data[ 0 ] == null && data[ 1 ] == null && data[ 2 ] == null && 
        data[ 3 ] == null && data[ 4 ] == null && data[ 5 ] == null )
    {
      return null;
    }
    return rfData;
  }

  public void importRfData( Hex rfData, Source source, int pairIndex )
  {
    if ( rfData == null )
      return;
    Hex[] data = new Hex[ 6 ];
    data[ 0 ] = rfData.subHex( 0, 1 );
    data[ 1 ] = rfData.subHex( 1, 2 );
    data[ 2 ] = rfData.subHex( 3, 7 );
    int appCaps = rfData.getData()[ 10 ];
    int pos = 11;
    if ( ( appCaps & 1 ) == 1 )
    {
      data[ 3 ] = rfData.subHex( pos, 15 );
      pos += 15;
    }
    int len = ( appCaps >> 1 ) & 3;
    if ( len > 0 )
    {
      data[ 4 ] = rfData.subHex( pos, len );
      pos += len;
    }
    len = ( appCaps >> 4 ) & 7;
    if ( len > 0 )
    {
      data[ 5 ] = rfData.subHex( pos, len );
      pos += len;
    }
    if ( source == Source.CONTROLLER )
    {
      nodeCaps = data[ 0 ].getData()[ 0 ];
      vendorID = data[ 1 ];
      vendorString = data[ 2 ];
      userString = data[ 3 ];
      devTypes = data[ 4 ];
      profiles = data[ 5 ];
    }
    else if ( source == Source.TARGET )
    {
      Pairing pair = pairings.get( pairIndex );
      pair.setPeerNodeCaps( data[ 0 ].getData()[ 0 ] );
      pair.setPeerVendorID( data[ 1 ] );
      pair.peerVendorString = data[ 2 ];
      pair.peerUserString = data[ 3 ];
      pair.setPeerDevTypes( data[ 4 ] );
      pair.peerProfiles = data[ 5 ];
    }
  }
  
  @Override
  public String toString()
  {
    return name;
  }

//  public static String getAddrString( Hex extAddr )
//  {
//    short[] a = extAddr.getData();
//    return String.format( "%02X%02X%02X%02X%02X%02X%02X%02X", 
//        a[7], a[6], a[5], a[4], a[3], a[2], a[1], a[0]);
//  }
}


