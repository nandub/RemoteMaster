package com.hifiremote.jp1.io;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.LinkedHashMap;

import com.hifiremote.jp1.io.JP2BT.BLEService;

public class BLERemote
{
  public String address;
  public String name;
  public String ueiName;
  public int regIndex = -1;
  public boolean found = false;
  public int rssi = 0;
  public boolean supportsUpload = false;
  public boolean hasFinder = false;
  public String signature= null;
  public int firmwareAddress = 0;
  public int firmwareSize = 0;
  public int infoAddress = 0;
  public int infoSize = 0;
  public int irdbAddress = 0;
  public int irdbSize = 0; 
  public int E2address = 0;
  public int E2size = 0;
  public int batteryBars = 0;
  public int batteryPercent = 0;
  public double batteryVoltage = 0.0;
  public int signalStrength= 0;
  public LinkedHashMap<String, BLEService> services = new LinkedHashMap<String, BLEService>();
  public LinkedHashMap<String, Integer> attributeHandles = new LinkedHashMap<String, Integer>();

  public String getGATTDescription() 
  {
    String result = toString();
    for (BLEService s : services.values()) 
      result += "\n" + s.getDescription();
    return result;
  }

  public BLERemote(String name, String ueiName, String address)
  {
    this.name = name;
    this.ueiName = ueiName;
    this.address = address;
  }
  
  public boolean updateConnData( JP2BT btio, int sequence )
  {
    signalStrength = 0;
    long waitStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    btio.bgapi.send_connection_get_rssi( btio.connection );
    while ( btio.bleRemote.signalStrength == 0 )
    {
      delay = Calendar.getInstance().getTimeInMillis() - waitStart;
      if ( delay > 1000 )
      {
        System.err.println( "Unable to read signal strength" );
        return false;
      }
    }
    
    UEIPacket upkt = new UEIPacket( 0, sequence, 0x43, 0x42, null );
    if ( ( upkt = btio.getUEIPacketResponse( upkt, 0 ) ) == null || !interpret( null, upkt ) )
    {
      System.err.println( "Failed to read battery state" );
      return false;
    }
    
    System.err.println( "Connection data: Voltage = " + String.format( "%4.2f", batteryVoltage ) 
        + ", Signal: " + signalStrength + "dBm" );

    return true;
  }
  
  public boolean interpret( String cmd, UEIPacket upkt )
  {
    if( cmd == null )
    {
      byte[] payload = upkt.getPayload();
      switch ( upkt.getAppCode() )
      {
        case 0:  // UAPI
          if ( payload.length != payload[ 0 ] + 1 )
            return false;
          hasFinder = false;
          for ( int i = 1; i < payload.length; i++ )
          {
            if ( payload[ i ] == 37 ) // test for RemoteFinder appCode
            {
              hasFinder = true;
              break;
            }
          }
          System.err.println( upkt.toString() );
          break;
        case 0x43:  // Battery status
          if ( payload.length != 4 )
            return false;
          batteryBars = 3 - payload[ 0 ];
          batteryPercent = payload[ 1 ];
          batteryVoltage = ( payload[ 2 ] + ( payload[ 3 ] << 8 ) )/2048.0f;
          break;
        default:
          System.err.println( "App code " + upkt.getAppCode() + " returned " + upkt.toString() );
      }
      return true;
    }
    else
    {
      int cmdCode = UEIPacket.getCmdCode( cmd );
      if ( upkt.getAppCode() != 0x11 )
      {
        System.err.println( "UEI packet is not a remote command response" );
        return false;
      }
      if ( upkt.isValidCmd() != 0 )
      {
        System.err.println( "Incoming remote command packet is invalid" );
        return false;
      }
      byte[] cmdArgs = upkt.getCmdArgs();
      switch ( cmdCode )
      {
        case 0x8B:  // CMD_APPINFOGET
        {
          try 
          {
            signature = new String( cmdArgs, 8, 6, "UTF-8");
            System.err.println( "Signature = " + signature );
          } 
          catch (UnsupportedEncodingException e) 
          {
            System.err.println( "Error in reading signature block" );
            return false;
          }
          firmwareAddress = intFromBytes( cmdArgs, 0x2e );
          infoAddress = intFromBytes( cmdArgs, 0x32 );
          irdbAddress = intFromBytes( cmdArgs, 0x36 );
          E2address = intFromBytes( cmdArgs, 0x3a );
          firmwareSize = intFromBytes( cmdArgs, 0x46 );
          infoSize = intFromBytes( cmdArgs, 0x4a );
          irdbSize = intFromBytes( cmdArgs, 0x4e );
          E2size = intFromBytes( cmdArgs, 0x52 );
          System.err.println( "E2 address = " + String.format( "%04X", E2address ) );
          System.err.println( "E2 size = " + String.format( "%04X", E2size ) );
        }
      }
      return true;
    }
  }

  public String toString() 
  {
    return name + " [" + address + "] (" + rssi + " dBm)";
  }
  
  private int intFromBytes( byte[] bArray, int offset )
  {
    int val = 0;
    for ( int i = 0; i < 4; i++ )
    {
      val = ( val << 8 ) | ( bArray[ offset + i ] & 0xFF );
    }
    return val;
  }
}
