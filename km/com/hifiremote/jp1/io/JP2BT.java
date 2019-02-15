package com.hifiremote.jp1.io;

import com.fazecast.jSerialComm.SerialPort;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JOptionPane;

import rmirwin10ble.IBleInterface;
import rmirwin10ble.Win10BLE;
import net.sf.jni4net.Bridge;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.Remote;
import com.hifiremote.jp1.RemoteMaster;
import com.hifiremote.jp1.RemoteMaster.Use;
import com.hifiremote.jp1.ble.BlueGiga;
import com.hifiremote.jp1.io.UEIPacket.CmdPacket;

public class JP2BT extends IO
{
  
  public JP2BT() throws UnsatisfiedLinkError  
  {
    super( null );
  }

  public JP2BT( File folder ) throws UnsatisfiedLinkError  
  {
    super( folder, null );
  }

  @Override
  public String getInterfaceName()
  {
    return "JP2BT";
  }

  @Override
  public String getInterfaceVersion()
  {
    return "1.0";
  }
  
  @Override
  public int getInterfaceType() {
    return 0x601;
  }

  @Override
  public String[] getPortNames() 
  {
    ArrayList<String> portList = new ArrayList<String>();
    String osName = System.getProperty( "os.name" );
    if ( osName.startsWith( "Windows" ) 
        && RemoteMaster.testWindowsVersion( "10.0.15063" ) 
        && RemoteMaster.admin )
    {
      portList.add( win10n );
    }

    for (SerialPort serialPort : SerialPort.getCommPorts())
    {
      portList.add(serialPort.getSystemPortName());
    }
    String[] portNames  = portList.toArray( new String[0] );
    return portNames;
  }

  @Override
  public String openRemote( String portName )
  {
    return portName;
  }

  @Override
  public void closeRemote() {}
  
  @Override
  public String getRemoteSignature()
  {
    return bleRemote.signature;
  }

  @Override
  public int getRemoteEepromAddress()
  {
    return bleRemote.E2address;
  }

  @Override
  public int getRemoteEepromSize()
  {
    return bleRemote.E2size;
  }

  @Override
  public int readRemote( int address, byte[] buffer, int length )
  {
    int progress = 0;
    int blockSize = 0x80;
    int erasePageSize = 0x800;
    int extraSize = 0;
    if ( getRemoteEepromSize() % erasePageSize != 0 )
    {
      extraSize = erasePageSize - getRemoteEepromSize() % erasePageSize;
    }
    if ( ( address < getRemoteEepromAddress() ) 
        || (address + length > getRemoteEepromAddress() + getRemoteEepromSize() + extraSize ) ) 
      return -1;
    
    // Reset extraSize to be only extra bytes within requested range
    int extraStart = getRemoteEepromAddress() + getRemoteEepromSize();
    extraSize = Math.max( 0, address + length - extraStart );
    
    boolean segmentsOnly = address == getRemoteEepromAddress();
    if ( progressUpdater != null && ( getUse() == Use.DOWNLOAD || getUse() == Use.UPLOAD ) )
    {
      // There are two uses of readRemote during uploading.  Mainly it is used for verifying
      // the upload, but for remotes such as the URC7955 where the E2 does not end on an
      // erase page boundary, it is also used for reading these extra bytes.  These can
      // be distinguished by the segmentsOnly boolean.
      setProgressName( getUse() == Use.DOWNLOAD ? "DOWNLOADING:" 
          : segmentsOnly ? "VERIFYING:" : "PREPARING:" );
      progressUpdater.updateProgress( progress );
    }
    
    // Restrict downloading to actual segments if starting at E2 start address
    int segPos = segmentsOnly ? 20 : 0xFFFFFF;
    int segSize = 0;
    int remaining = length;
    int pos = 0;
    int extraIncs = extraSize > 0 ? 3 : 2;
    int progressIncrement = 100 / ( ( length - 1 ) / blockSize + extraIncs );
    Arrays.fill( buffer, ( byte )0xFF );

    if ( getUse() == Use.DOWNLOAD || getUse() == Use.RAWDOWNLOAD )
    {
      if ( !updateConnData( sequence++ ) )
        return -1;
      if ( bleRemote.batteryBars < 2 )
      {
        String message = "Battery level too low to download.";
        JOptionPane.showMessageDialog( null, message, "Download error", JOptionPane.ERROR_MESSAGE );
        return -1;
      }
    }

    progress += progressIncrement;
    if ( progressUpdater != null && ( getUse() == Use.DOWNLOAD || getUse() == Use.UPLOAD ) )
      progressUpdater.updateProgress( progress );
    
    while ( remaining > 0 )
    {
      while ( pos >= segPos + 2 )
      {
        segSize = ( buffer[ segPos ] << 8 & 0xFF00 ) + ( buffer[ segPos + 1 ] & 0xFF );
        if ( segSize == 0xFFFF ) 
          break;
        else
          segPos += segSize;
      }
      if ( segSize == 0xFFFF && pos > segPos ) 
        break;
      
      int size = remaining > blockSize ? blockSize : remaining;
      byte[] block = readRemoteBlock( address + pos, size );
      if ( block == null )
        return pos;
      progress += progressIncrement;
      if ( progressUpdater != null && ( getUse() == Use.DOWNLOAD || getUse() == Use.UPLOAD ) )
        progressUpdater.updateProgress( progress );
      System.arraycopy( block, 0, buffer, pos, size );
      pos += size;
      remaining -= size;
    }
    
    if ( segmentsOnly && extraSize > 0 )
    {
      byte[] block = readRemoteBlock( extraStart, extraSize );
      if ( block == null )
        return pos;
      progress += progressIncrement;
      if ( progressUpdater != null && ( getUse() == Use.DOWNLOAD || getUse() == Use.UPLOAD ) )
        progressUpdater.updateProgress( progress );
      System.arraycopy( block, 0, buffer, getRemoteEepromSize(), extraSize );
    }
    
    return buffer.length;
  }

  @Override
  public int writeRemote( int address, byte[] buffer, int length )
  {
    int erasePageSize = 0x800;
    int writeWordSize = 4;
    int extraSize = 0;
    if ( length == 0 )
      return 0;
    if ( getRemoteEepromSize() % erasePageSize != 0 )
    {
      extraSize = erasePageSize - getRemoteEepromSize() % erasePageSize;
    }
    if ( address != getRemoteEepromAddress() || length > getRemoteEepromSize() + extraSize )
      return -1;
    
    // Reset extraSize to be only extra bytes within requested range
    int extraStart = address + getRemoteEepromSize();
    extraSize = Math.max( 0, address + length - extraStart );
       
    int eraseLength = bleRemote.E2size;
    Remote remote = owner.getRemoteConfiguration().getRemote();
    // We write only the data up to the end of segments.  The checksum end has already
    // been set to the address of the last segment byte, so we use this value to get
    // the length to write, but need to round it up to multiple of word size.
    int dataEnd = ( remote.getCheckSums()[ 0 ].getAddressRange().getEnd() 
        | ( writeWordSize - 1 ) ) + 1;
    // As a precaution, make sure this is not greater than supplied length
    length = Math.min( length, dataEnd );
    System.err.println( "Length of segment data is $" + Integer.toHexString( length ).toUpperCase() );
    boolean writeExtraBytes = false;
    for ( int i = length; i < buffer.length; i++ )
    {
      if ( ( buffer[ i ] & 0xFF ) != 0xFF )
      {
        if ( i < getRemoteEepromSize() )
        {
          // There are bytes other than $FF in the E2 area beyond the segments
          System.err.println( "There are bytes beyond the segments that are not $FF" );
          return -1;
        }
        else
        {
          writeExtraBytes = true;
          break;
        }
      }
    }
    
    int progress = 0;
    if ( progressUpdater != null && getUse() == Use.UPLOAD )
    {
      setProgressName( "UPLOADING:" );
      progressUpdater.updateProgress( progress );
    }
  
    int blockSize = 0x80;
    int remaining = length;
    int extraIncs = writeExtraBytes ? 3 : 2;
    int pos = 0;
    int progressIncrement = 100 / ( ( length - 1 ) / blockSize + extraIncs );

    if ( getUse() == Use.UPLOAD )
    {
      if ( !updateConnData( sequence++ ) )
        return -1;
      if ( bleRemote.batteryBars < 3 )
      {
        String message = "Battery level too low to upload.";
        JOptionPane.showMessageDialog( null, message, "Upload error", JOptionPane.ERROR_MESSAGE );
        return -1;
      }
    }

    progress += progressIncrement;
    if ( progressUpdater != null && ( getUse() == Use.DOWNLOAD || getUse() == Use.UPLOAD ) )
      progressUpdater.updateProgress( progress );
    
    if ( erase( address, address + eraseLength - 1 ) != 0 )
      return -1;
    
    while ( remaining > 0 )
    {
      int size = remaining > blockSize ? blockSize : remaining;
      if ( writeRemoteBlock( address + pos, Arrays.copyOfRange( buffer, pos, pos + size ) ) != 0 )
      {
        return pos;
      }
      progress += progressIncrement;
      if ( progressUpdater != null && getUse() == Use.UPLOAD )
        progressUpdater.updateProgress( progress );
      pos += size;
      remaining -= size;
    }
    
    if ( writeExtraBytes )
    {
      if ( writeRemoteBlock( extraStart, Arrays.copyOfRange( buffer, getRemoteEepromSize(), getRemoteEepromSize() + extraSize ) ) != 0 )
      {
        return pos;
      }
      progress += progressIncrement;
      if ( progressUpdater != null && getUse() == Use.UPLOAD )
        progressUpdater.updateProgress( progress );
    }
    
    return buffer.length;
  }

  public void setBleInterface( String port )
  {
    if ( port.equals( win10n ) )
    {
      File workdir = new File("rmirwin10ble");
      try
      {
        Bridge.init(workdir);
        Bridge.LoadAndRegisterAssemblyFrom(new java.io.File(workdir, "RMIRWin10BLE.j4n.dll"));
        blei = new Win10BLE();
      }
      catch ( IOException e )
      {
        e.printStackTrace();
      }
    }
    else
    {
      blei = new BlueGiga();
    }
  }
  
  public static SerialPort connectSerial(String portName) {
    try {
      System.err.println( "Trying to open serial port " + portName );
      SerialPort serialPort = SerialPort.getCommPort(portName);
      if (!serialPort.openPort()) {
        System.err.println("Error: Can't open port " + portName + ".");
        return null;
      }
      serialPort.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
      serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
      System.err.println("serial port = " + serialPort);
      return serialPort;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public String connectBLE( String portName )
  {
    return blei.ConnectBLE( portName );
  }
  
  public void disconnectBLE()
  {
    blei.DisconnectBLE();
  }
  
  public boolean connectUEI() throws InterruptedException
  {
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 10 );
    if ( !blei.ConnectUEI( bleRemote.address ) )
    {
      System.err.println("Failed at stage " + blei.GetStage());
      return false;
    }
    System.err.println( "Basic connection to remote succeeded");
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 20 );
    if ( !blei.DiscoverServices() )
    {
      System.err.println("Failed at stage " + blei.GetStage());
      return false;
    }
    System.err.println( "Services discovered" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 50 );
    if ( !blei.GetFeatures())
    {
      System.err.println("Failed at stage " + blei.GetStage());
      return false;
    }
    if ( needsCCCD() && !hasCCCD() )
    {
      String title = "Interface issue";
      String message =
            "RMIR cannot communicate with this remote through this Bluetooth interface.\n"
          + "This is due to an issue with the remote that can be corrected with an\n"
          + "extender.  However, the extender needs a JP1.x cable to install it.  Once\n"
          + "the extender is installed, all future communication can be made with the\n"
          + "Bluetooth interface without any further need for the cable.";
      JOptionPane.showMessageDialog( owner, message, title, JOptionPane.WARNING_MESSAGE );
      return false;
    }
    
    System.err.println( blei.GetSubscription());

    //Thread.sleep( 1000 );
    boolean didread = false;
    for ( int i = 0; i < 2; i++)
    {
      System.err.println( "Reading info and sig" );
      UEIPacket upkt = new CmdPacket( "APPINFOGET", new byte[]{} ).getUEIPacket( sequence++ );
      System.err.println( "AppInfoGet pkt: " + upkt );
      if ( ( upkt = getUEIPacketResponse( upkt, 75 ) ) == null || !bleRemote.interpret( "APPINFOGET", upkt ) )
      {
        System.err.println( "Failed to read info and sig" );
        continue;
        //return false;
      }
      didread = true;
      break;
    } 
    if ( !didread)
      return false;

    if ( !updateConnData( sequence++ ) )
    {
      return false;
    }
    
    UEIPacket upkt = new UEIPacket( 0, sequence++, 0, 0x44, null );
    if ( ( upkt = getUEIPacketResponse( upkt, 85 ) ) == null || !bleRemote.interpret( null, upkt ) )
    {
      System.err.println( "Failed to read AppInfoRequest" );
      return false;
    }
    System.err.println( bleRemote.hasFinder ? "Remote has finder" : "Remote does not have finder" );

    // Update the connection to the parameter values used by UEI phone app
    blei.UpdateConnection( 104, 120, 4, 550 );
    
    // This erase command has no effect, whether the extender is installed or not.
    // If it is not installed then the first parameter is interpreted as two valid 2-byte
    // addresses, start and end, with start > end, so the command is accepted (return code
    // 0) but erases nothing.  If it is installed then the parameters are two 4-byte
    // addresses with the first being invalid, giving a Bad Address return code of 3.
    bleRemote.supportsUpload = erase( 0xFF001F00, 0 ) == 3;
    System.err.println( bleRemote.supportsUpload ? "Remote supports uploading"
        : "Remote does not support uploading" );

    disconnecter = new BLEDisconnecter( this );
    return true;
    
  }
 
  public void disconnectUEI()
  {
    if ( disconnecter != null )
      disconnecter.stop();
    
    String result = blei.DisconnectUEI();
    System.err.println(blei.GetSubscription());
    System.err.println( result );
  }

  /** 
   * Returns -1 if sending timed out, else UEI error code, 0 on success
   */
  public int sendUEIPacket( UEIPacket upkt )
  {
    int n = 0;
    System.err.println( "Sending " + upkt.toString() );
    List< byte[] > bleList = upkt.toBLEpackets();
    for ( byte[] bleData : bleList )
    {
      n++;
      blei.SetSentState( 0 );
      blei.WritePacket( bleData );
      //transport.sendPacket( bpkt );
      long waitStart = Calendar.getInstance().getTimeInMillis();
      long delay = 0;
      while ( blei.GetSentState() == 0 )
      {
        // wait for acknowledgement of save receipt
        delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
        if ( delay > 2000 )
        {
          System.err.println( "Sending UEI packet timed out at BPI packet " + n + " of " + bleList.size() );
          return -1;
        }
      }
      if ( blei.GetSentState() > 1 )
      {
        System.err.println( "Sending UEI packet returned error code " + ( blei.GetSentState()-1) + " at BPI packet " + n + " of " + bleList.size() );
      }
      if ( n < bleList.size() )
      {
        // wait for Immediate-type packet that is request to send next fragment
        ueiInStart = Calendar.getInstance().getTimeInMillis();
        UEIPacket sendNext = null;
        while ( sendNext == null || sendNext.getFrameType() != 4 )
        {  
          sendNext = getUEIPacketIn();
          delay = Calendar.getInstance().getTimeInMillis() - ueiInStart; 
          if ( delay > 6000 )
          {
            System.err.println( "Wait for request for next fragment timed out after " + delay + "ms" );
            return -1;
          }
        }
      }
    }
    return blei.GetSentState()-1;
  }
  
  public UEIPacket getUEIPacketResponse( UEIPacket upkt, int progress )
  {
    // Progress updater is only updated when value supplied is > 0
    if ( sendUEIPacket( upkt ) != 0 )
      return null;
    System.err.println( "Packet sent");
    if ( progressUpdater != null && progress > 0 )
      progressUpdater.updateProgress( progress );
    UEIPacket upktRcvd = getUEIPacketIn();
    if ( upktRcvd == null )
      return null;
    if ( progressUpdater != null && progress > 0 )
      progressUpdater.updateProgress( progress + 2 );
    return upktRcvd;
  }
 
  public UEIPacket getUEIPacketIn()
  {
    ueiInStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    System.err.println( "Getting UEI packet.  Incoming list size = " + incoming.size());
    int pktCount = -1;
    while ( true )
    {
      int bCount = blei.GetInCount();
      if ( pktCount != bCount)
      {
        pktCount = bCount;
        System.err.println( "Current incoming count = " + pktCount );
      }
      
      if ( blei.GetInDataSize() > 0 )
      {
        System.err.println("Incoming ble pkt");
        byte[] value = blei.GetInData( 0 );
        UEIPacket upkt = null;
        boolean ueiInOk = true;
      
        int frameType = value[ 0 ];
        int sequence = value[ 1 ];
        int state = frameType & UEIPacket.getFrameType( "FragmentStart" );
        if ( state == 0 )
        {
          // Packet is unfragmented
          ueiInStart = Calendar.getInstance().getTimeInMillis();
          upkt = new UEIPacket( frameType, sequence, value[ 2 ], 
              value[ 3 ], Arrays.copyOfRange( value, 4, value.length ) );
          System.err.println( upkt.toString() );
          incoming.add( upkt );
          System.err.println( "Queueing UEIPacket with id " + System.identityHashCode( upkt ) );
          break;
        }
        else if ( state == UEIPacket.getFrameType( "FragmentStart" ) )
        {
          ueiInOk = true;
          ueiInStart = Calendar.getInstance().getTimeInMillis();
          upkt = new UEIPacket( frameType, sequence, value[ 2 ], 
              value[ 4 ], value[ 3 ], Arrays.copyOfRange( value, 5, value.length ) );
          System.err.println( "Start " + upkt.toString() );
          ueiIn.put( sequence, upkt );
        }
        else if ( state == UEIPacket.getFrameType( "Fragmented" ) )
        {
          ueiInStart = Calendar.getInstance().getTimeInMillis();
          upkt = ueiIn.get( sequence );
          if ( ueiInOk && ( upkt == null || !upkt.update( frameType, sequence, value[ 2 ], 
              value[ 3 ], Arrays.copyOfRange( value, 4, value.length ) ) ) )
          {
            System.err.println( "Error in incoming packet fragment" );
            ueiIn.remove( sequence );
            ueiInOk = false;
            break;
          }
          System.err.println( "Incomplete " + upkt.toString() );
        }
        else if ( state == UEIPacket.getFrameType( "FragmentEnd" ) )
        {
          ueiInStart = Calendar.getInstance().getTimeInMillis();
          upkt = ueiIn.remove( sequence );
          if ( ueiInOk && ( upkt == null || !upkt.update( frameType, sequence, value[ 2 ], 
              value[ 3 ], Arrays.copyOfRange( value, 4, value.length ) ) ) )
          {
            ueiInOk = false;
            System.err.println( "Error in incoming end fragment" );
            break;
          }
          else if ( ueiInOk )
          {
            System.err.println( "Complete " + upkt.toString() );
            incoming.add( upkt );
            System.err.println( "Queueing UEIPacket with id " + System.identityHashCode( upkt ) );
            break;
          }
        }
      }

      delay = Calendar.getInstance().getTimeInMillis() - ueiInStart;
      if ( delay > 6000 )
      {
        System.err.println( "Incoming UEI packet timed out, incoming queue size " + incoming.size() );
        return null;
      }
    }
    System.err.println( "Incoming UEI packet received" );
    UEIPacket upkt = incoming.remove( 0 );
    System.err.println( "Removing UEIPacket with id " + System.identityHashCode( upkt ) );
    System.err.println("Packet data: " + upkt);
    return upkt;
  }
  
  public void finderOn( boolean setOn )
  {
    byte[] args = { 5, setOn ? ( byte )0xa0 : 8, 0 };
    UEIPacket p = new UEIPacket( 0, sequence++, 0x25, 0x44, args );
    if ( getUEIPacketResponse( p, 0 ) != null )
      System.err.println( "Finder turned " + ( setOn ? "On" : "Off") );
    else
      System.err.println( "Error in setting finder " + ( setOn ? "On" : "Off") );
  }
  
  public byte[] readRemoteBlock( int address, int length )
  {
    // Args of DATAREAD are 4-byte msb address and 2-byte msb length
    byte[] args = new byte[ 6 ];
    for ( int i = 0; i < 4; i++ )
      args[ 3 - i ] = ( byte )( ( address >> 8*i ) & 0xFF );
    for( int i = 0; i < 2; i++ )
      args[ 5 - i ] = ( byte )( ( length >> 8*i ) & 0xFF );
    CmdPacket cpkt = new CmdPacket( "DATAREAD", args );
    if ( sendUEIPacket( cpkt.getUEIPacket( sequence++ ) ) != 0 )
    {
      System.err.println( "Outgoing UEI packet failed to send" );
      return null;
    }
    UEIPacket upkt = null;
    ueiInStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    while ( upkt == null || upkt.getFrameType() == 4 || upkt.getOpCode() != 0x40 
        || upkt.getPayload().length == 4 )
    {  
      upkt = getUEIPacketIn();
      if ( upkt != null && ( upkt.getFrameType() == 4 || upkt.getOpCode() != 0x40 
        || upkt.getPayload().length == 4 ) )
      {
        System.err.println( "Unexpected " + upkt.toString() );
      }
      delay = Calendar.getInstance().getTimeInMillis() - ueiInStart;
      if ( delay > 6000 )
      {
        System.err.println( "Read Block timed out after delay of " + delay + "ms" );
        return null;
      }
    }

    if ( upkt == null || upkt.isValidCmd() != 0 )
      return null;
    byte[] result = upkt.getCmdArgs();
//    System.err.println( bytesToString( result ) );
    return result;
  }
  
  /**
   * End address is inclusive.  Return value is error code
   */
  public int erase( int start, int end )
  {
    byte[] args = new byte[ 8 ];
    for ( int i = 0; i < 4; i++ )
      args[ 3 - i ] = ( byte )( ( start >> 8*i ) & 0xFF );
    for( int i = 0; i < 4; i++ )
      args[ 7 - i ] = ( byte )( ( end >> 8*i ) & 0xFF );
    CmdPacket cpkt = new CmdPacket( "DATAERASE", args );
    if ( sendUEIPacket( cpkt.getUEIPacket( sequence++ ) ) != 0 )
    {
      System.err.println( "Outgoing UEI packet failed to send" );
      return -1;
    }

    UEIPacket upkt = getUEIPacketIn();
    if ( upkt == null )
      return -2;
//    byte[] result = upkt.getCmdArgs();
//    System.err.println( "Erase args rcvd: " + bytesToString( result ) );
    int n = upkt.isValidCmd();
    return n;
  }
  
  public int sendRecord( Hex record )
  {
//    System.err.println( "Start sending record" );
//    long recordStart = Calendar.getInstance().getTimeInMillis();
    byte[] args = new byte[ record.length() + 2 ];
    args[ 0 ] = args[ 1 ] = 0;
    System.arraycopy( record.toByteArray(), 0, args, 2, record.length() );
    CmdPacket cpkt = new CmdPacket( "RECORDSET", args );
    if ( sendUEIPacket( cpkt.getUEIPacket( sequence++ ) ) != 0 )
    {
      System.err.println( "Record failed to send" );
      return -1;
    }
//    long duration = Calendar.getInstance().getTimeInMillis() - recordStart;
//    System.err.println( "Record data sent after " + duration + "ms" );
    UEIPacket upkt = null;
    ueiInStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    while ( upkt == null || upkt.getFrameType() == 4 || upkt.getOpCode() != 0x40  )
    {  
      upkt = getUEIPacketIn();
      delay = Calendar.getInstance().getTimeInMillis() - ueiInStart;
      if ( delay > 6000 )
      {
        System.err.println( "Send record timed out after delay of " + delay + "ms" );
        return -2;
      }
    }
    byte[] result = upkt.getCmdArgs();
    if ( result == null )
      return -3;
//    System.err.println( "Send args rcvd: " + bytesToString( result ) );
    int n = upkt.isValidCmd();
//    duration = Calendar.getInstance().getTimeInMillis() - recordStart;
//    System.err.println( "Record send complete after " + duration + "ms" );
    return n;
  }
  
  private int writeRemoteBlock( int address, byte[] data )
  {
    if ( ( address & 3 ) != 0 )
      return -3;
    if ( ( data.length & 3 ) != 0 )
      return -4;
    byte[] args = new byte[ data.length + 4 ];
    for ( int i = 0; i < 4; i++ )
      args[ 3 - i ] = ( byte )( ( address >> 8*i ) & 0xFF );
    System.arraycopy( data, 0, args, 4, data.length );
    CmdPacket cpkt = new CmdPacket( "DATAWRITE", args );
    if ( sendUEIPacket( cpkt.getUEIPacket( sequence++ ) ) != 0 )
    {
      System.err.println( "Write block failed to send" );
      return -1;
    }

    UEIPacket upkt = null;
    ueiInStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    while ( upkt == null || upkt.getFrameType() == 4 || upkt.getOpCode() != 0x40  )
    {  
      upkt = getUEIPacketIn();
      if ( upkt != null && ( upkt.getFrameType() == 4 || upkt.getOpCode() != 0x40 ) )
      {
        System.err.println( "Unexpected " + upkt.toString() );
      }
      delay = Calendar.getInstance().getTimeInMillis() - ueiInStart;
      if ( delay > 6000 )
      {
        System.err.println( "Write Block timed out after delay of " + delay + "ms" );
        return -2;
      }
    }
//    byte[] result = upkt.getCmdArgs();
//    System.err.println( "Send args rcvd: " + bytesToString( result ) );
    return upkt.isValidCmd();
  }
  
  public boolean updateConnData( int sequence )
  {
    int newSignalStrength = blei.ReadSignalStrength();

    if ( newSignalStrength == 0 )
    {
      System.err.println( "Unable to read signal strength" );
      return false;
    }
    bleRemote.signalStrength = newSignalStrength;

    UEIPacket upkt = new UEIPacket( 0, sequence, 0x43, 0x42, null );
    if ( ( upkt = getUEIPacketResponse( upkt, 0 ) ) == null || !bleRemote.interpret( null, upkt ) )
    {
      System.err.println( "Failed to read battery state" );
      return false;
    }
    
    String strength = newSignalStrength == 1 ? "N/A" : newSignalStrength + "dBm";
    
    System.err.println( "Connection data: Voltage = " + String.format( "%4.2f", bleRemote.batteryVoltage ) 
        + ", Signal: " + strength );

    return true;
  }
  
  public boolean isScanning()
  {
    return blei.IsScanning();
  }
  
  public String bytesToString(byte[] bytes) {
    StringBuffer result = new StringBuffer();
    result.append("[ ");
    for(byte b : bytes) result.append( String.format( "%02X  ", b & 0xFF ) );
    result.append("]");
    return result.toString();        
  }

  public void setOwner( RemoteMaster owner )
  {
    this.owner = owner;
  }

  public RemoteMaster getOwner()
  {
    return owner;
  }
  
  public boolean isDisconnecting()
  {
    return blei.IsDisconnecting();
  }
  
  public void setDisconnecting( boolean disconnecting )
  {
    blei.SetDisconnecting( disconnecting );
  }
  
  public void discoverUEI( boolean start )
  {
    blei.DiscoverUEI( start );
  }
  
  public int getListSize()
  {
    return blei.GetListSize();
  }
  
  public String getListItem(int ndx)
  {
    return blei.GetListItem( ndx );
  }
  public String getItemName(int ndx)
  {
    return blei.GetItemName( ndx );
  }
  
  public int getRssi( int ndx )
  {
    return blei.GetRssi( ndx );
  }
 
  private boolean hasCCCD()
  {
    return blei.HasCCCD();
  }
  
  private boolean needsCCCD()
  {
    return blei.NeedsCCCD();
  }

  public BLERemote getBleRemote()
  {
    return bleRemote;
  }

  public void setBleRemote( BLERemote bleRemote )
  {
    this.bleRemote = bleRemote;
  }
  
  public boolean isConnected()
  {
    return blei.IsConnected();
  }

  private long ueiInStart = 0;  // Timer start for incoming UEI packet fragments
  private LinkedHashMap< Integer, UEIPacket > ueiIn = new LinkedHashMap< Integer, UEIPacket >();
  private BLERemote bleRemote = null;
  public int sequence = 1;
  private RemoteMaster owner = null;
  private ArrayList< UEIPacket > incoming = new ArrayList< UEIPacket >();
  private IBleInterface blei = null;
  private BLEDisconnecter disconnecter = null;
  public String win10n = "Win10 Native";
}
