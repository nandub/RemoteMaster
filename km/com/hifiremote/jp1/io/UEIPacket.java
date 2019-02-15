package com.hifiremote.jp1.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.thingml.bglib.BGAPIPacket;

public class UEIPacket
{
  public static class CmdPacket
  {
    int cmdCode = 0;
    byte[] args = null;
    
    public CmdPacket( String cmd, byte[] args )
    {
      this.cmdCode = getCmdCode( cmd );
      this.args = args;
    }
    
    public byte[] getPayload()
    {
      byte[] pl = new byte[ args.length + 4];
      int size = args.length + 2;
      pl[ 0 ] = (byte)( (size >> 8) & 0xFF);
      pl[ 1 ] = (byte)( size & 0xFF );
      pl[ 2 ] = (byte)( cmdCode & 0xFF );
      System.arraycopy( args, 0, pl, 3, args.length );
      int chksum = doChecksum( pl );
      pl[ pl.length - 1 ] = (byte)( chksum & 0xFF );
      return pl;
    }
    
    public UEIPacket getUEIPacket( int sequence )
    {
      return new UEIPacket( 0, sequence, 0x11, 0x41, getPayload() );
    }
  }

  public static final String[][] frameTypes = {
    { "0x00", "None" },
    { "0x80", "AckRequest" },
    { "0x40", "AckResponse" },
    { "0x30", "FragmentStart" },
    { "0x20", "Fragmented" },
    { "0x10", "FragmentEnd" },
    { "0x08", "MoreData" },
    { "0x04", "Immediate" }
  };

  public static int getFrameType( String name )
  {
    int type = -1;
    for ( String[] currType : frameTypes )
    {
      if ( currType[ 1 ].equals( name ) )
      {
        type = Integer.parseInt( currType[ 0 ].substring( 2 ), 16  );
        break;
      }
    }
    return type;
  }

  public static final String[][] appCodes = {
    { "UAPI", "0" }, 
    { "OTA", "1" }, 
    { "Blaster", "16" }, 
    { "Remote", "17" }, 
    { "Activity", "32" }, 
    { "URC", "33" }, 
    { "Text", "34" }, 
    { "Alert", "36" }, 
    { "RemoteFinder", "37" }, 
    { "Motion", "48" }, 
    { "Orientation", "49" }, 
    { "Relative", "50" }, 
    { "RelativeHi", "51" }, 
    { "Absolute", "52" }, 
    { "AbsoluteHi", "53" }, 
    { "RawCompressedAudio", "54" }, 
    { "Tap", "64" }, 
    { "Accelerometer", "65" }, 
    { "VirtualKeys", "66" }, 
    { "BatteryStatus", "67" }, 
    { "HID", "80" }, 
    { "Audio", "96" }, 
    { "ConfigAppMain", "128" }, 
    { "ConfigTouchpad", "129" }, 
    { "ConfigAccelerometer", "130" }, 
    { "Invalid", "-1" }
  };

  public static final String[][] uapi_opCodes = {
    { "ProductInfo", "0x05" },
    { "AppInfoRequest", "0x44" }
  };

  public static final String[][] record_cmdCodes = {
    { "0x01", "CMD_DATAREAD" }, 
    { "0x02", "CMD_DATAWRITE" }, 
    { "0x03", "CMD_DATAERASE" }, 
    { "0x04", "CMD_CHECKSUM" }, 
    { "0x50", "CMD_ICINFO" }, 
    { "0x51", "CMD_ENTERBL" }, 
    { "0x52", "CMD_EXIT" }, 
    { "0x53", "CMD_GET_VERSION" }, 
    { "0x80", "CMD_DATARMWRITE" }, 
    { "0x81", "CMD_RECORDGET" }, 
    { "0x82", "CMD_RECORDSET" }, 
    { "0x83", "CMD_RECORDREMOVE" }, 
    { "0x84", "CMD_FDRAMEMGET" }, 
    { "0x85", "CMD_SENDIRFUNCTION" }, 
    { "0x86", "CMD_SENDKEY" }, 
    { "0x87", "CMD_CFGSET" }, 
    { "0x88", "CMD_CFGRESET" }, 
    { "0x89", "CMD_CFGGET" }, 
    { "0x8A", "CMD_REMOVE_UNUSED_UPG" }, 
    { "0x8B", "CMD_APPINFOGET" }, 
    { "0x8C", "CMD_LONGRECORDSET" }, 
    { "0x8D", "CMD_LIST_UPGRADE" }, 
    { "0x8E", "CMD_REMOVE_UPGRADE" }, 
    { "0x8F", "CMD_TEST_DEVICE_CODE" }
  };
  
  public static int getCmdCode( String name )
  {
    int code = -1;
    for ( String[] currCmdCode : record_cmdCodes )
    {
      if ( currCmdCode[ 1 ].equals( "CMD_" + name ) )
      {
        code = Integer.parseInt( currCmdCode[ 0 ].substring( 2 ), 16  );
        break;
      }
    }
    return code;
  }


  private int frameType;
  private int sequence;
  private int appCode;
  private int opCode;
  private int fragmentCount;
  private byte[] payload;

  public UEIPacket( int frameType, int sequence, int appCode, int opCode,
      byte[] payload )
  {
    this.frameType = frameType;
    this.sequence = sequence;
    this.appCode = appCode;
    this.opCode = opCode;
    this.fragmentCount = -1;
    this.payload = payload == null ? new byte[0] : payload;
  }

  public UEIPacket( int frameType, int sequence, int appCode, int opCode,
      int fragmentCount, byte[] payload )
  {
    this.frameType = frameType;
    this.sequence = sequence;
    this.appCode = appCode;
    this.opCode = opCode;
    this.fragmentCount = fragmentCount;
    this.payload = payload == null ? new byte[0] : payload;
  }
  
  public int getAppCode()
  {
    return appCode;
  }

  public int getOpCode()
  {
    return opCode;
  }

  public byte[] getPayload()
  {
    return payload;
  }

  public int getFrameType()
  {
    return frameType;
  }

  public boolean update( int frameType, int sequence, int appCode,
      int fragmentCount, byte[] payload )
  {
    // Mask out the fragment flags in frame type
    this.frameType = frameType & ~getFrameType( "FragmentStart" );
    if ( this.appCode != appCode )
    {
      System.err.println( "App code difference in UEIPacket, sequence = " + sequence );
      return false;
    }
    if ( this.fragmentCount != fragmentCount + 1 )
    {
      System.err.println( "Fragment sequence error in UEIPacket, sequence = " + sequence );
      return false;
    }
    this.fragmentCount = fragmentCount;
    byte[] newload = new byte[ this.payload.length + payload.length ];
    System.arraycopy( this.payload, 0, newload, 0, this.payload.length );
    System.arraycopy( payload, 0, newload, this.payload.length, payload.length );
    this.payload = newload;
    return true;
  }

  public List< byte[] > toBLEpackets()
  {
    List< byte[] > out = new ArrayList< byte[] >();
    List< UEIPacket > list = new ArrayList< UEIPacket >();
    int immed = getFrameType( "Immediate" );
    if ( payload.length < 17 )
    {
      fragmentCount = -1;
      list.add( this );
    }
    else
    {
      int index = 0;
      int remaining = ( payload.length ) / 16;  // number remaining after this
      for (int r = remaining; r >= 0; r--)
      {
        int currentType = frameType | ( index == 0 ? getFrameType( "FragmentStart" )
            : r == 0 ? getFrameType( "FragmentEnd" ) : getFrameType( "Fragmented" ) );
        currentType &= ~immed;
        if ( r > 0 )
          currentType |= getFrameType( "MoreData" );
        if ( r == 0 && ( frameType & immed ) == immed )
          currentType |= immed;
        byte[] currentPayload = Arrays.copyOfRange( payload, Math.max( 0, 16*index-1),
            Math.min( payload.length , 16*(index+1)-1 ) );
        list.add( new UEIPacket( currentType, sequence, appCode, opCode, r, currentPayload ) );
        index++;
      }
    }
    for ( UEIPacket upkt : list )
    {
      int first = getFrameType( "FragmentStart" );
      int headerSize = ( upkt.frameType & first ) == first ? 5 : 4;
      byte[] data = new byte[ upkt.payload.length + headerSize ];
      int n = 0;
      //data[ n++ ] = ( byte ) connection;
      //data[ n++ ] = ( byte ) ( atthandle & 0xFF );
      //data[ n++ ] = ( byte ) ( ( atthandle >> 8 ) & 0xFF );
      data[ n++ ] = ( byte )(upkt.frameType & 0xFF);
      data[ n++ ] = ( byte )(upkt.sequence & 0xFF);
      data[ n++ ] = ( byte )(upkt.appCode & 0xFF);
      if ( upkt.fragmentCount >= 0 )
        data[ n++ ] = ( byte )(upkt.fragmentCount & 0xFF);
      if ( upkt.fragmentCount < 0 || headerSize == 5 )
        data[ n++ ] = ( byte )(upkt.opCode & 0xFF);
      System.arraycopy( upkt.payload, 0, data, n, upkt.payload.length );
      // Now create a new BGAPI send_attclient_write_command packet to hold UEI packet
      /*
      BGAPIPacket bpkt = new BGAPIPacket(0, 4, 6);
      bpkt.w_uint8(connection);
      bpkt.w_uint16(atthandle);
      bpkt.w_uint8array(data);
      */
      out.add( data );
    }
    return out;
  }
  
  @Override
  public String toString()
  {
    return "UEIPacket: type=" + frameType + ", sequence=" + sequence
        + ", appcode=" + appCode + ", opcode=" + opCode + "\n[" +
        bytesToString( payload ) + "]";
  }
  
  public static String bytesToString( byte[] bytes ) 
  {
    StringBuffer result = new StringBuffer();
    for( byte b : bytes ) 
      result.append( Integer.toHexString( b & 0xFF ) + " ");
    return result.toString();        
  }
  
  public static int doChecksum( byte[] payload )
  {
    int chk = 0;
    for ( int i = 0; i < payload.length - 1; i++ )
    {
      chk ^= payload[ i ] & 0xFF;
    }
    return chk;
  }
  
  /*
   * Returns 0 if command is valid, else error code.
   * Code -1 = error in payload length.
   * Code -2 = checksum error
   * Codes >0 are UEI error codes.
   */
  public int isValidCmd()
  {
    int ueiLen = ( 16*(payload[ 0 ]&0xFF) + (payload[ 1 ]&0xFF) );
    int payLen = payload.length - 2;
    if ( ueiLen != payLen )
      return -1;
    int chk = doChecksum( payload );
    if ( chk != ( payload[ ueiLen + 1 ]&0xFF ) )
      return -2;
    return payload[ 2 ];
  }
  
  public byte[] getCmdArgs()
  {   
    return payload.length < 4 ? null : Arrays.copyOfRange( payload, 3, payload.length - 1  );
  }

}
