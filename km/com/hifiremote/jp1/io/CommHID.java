package com.hifiremote.jp1.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import com.codeminders.hidapi.HIDManager;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.Remote;
import com.hifiremote.jp1.RemoteConfiguration;
import com.hifiremote.jp1.RemoteManager;
import com.hifiremote.jp1.RemoteMaster;
import com.hifiremote.jp1.RemoteMaster.NegativeDefaultButtonJOptionPane;
import com.hifiremote.jp1.RemoteMaster.Use;
 
public class CommHID extends IO 
{
  /*
   *  Summary of XSight Touch packet types, hex numbering.  Packets are all 64 bytes,
   *  the first byte being the packet type and the last two being a CRC checksum.
   *  The structure of the remaining bytes depends on packet type and its use.
   *  Official UEI names in brackets at end.
   *    01  General packet carrying data in response to a specific request or
   *          sent as an acknowledgement of receipt. (RSP)
   *    04  Request firmware version data (for all 33 firmware files).  Response
   *          is an 01 packet giving number of files, followed by an 05 packet for
   *          each file that needs acknowledgement with an 01 packet. (GetInfo)
   *    05  Version data for specific firmware file. (GetInfoData)
   *    12  Request content of a named file.  Response is an 01 packet giving file
   *          length, followed by a series of type 14 packets giving file data as
   *          series of blocks.  Each type 14 packet needs acknowledgement with a
   *          type 01 packet. (GetFile)
   *    13  Write content of a named file.  Packet carries file name and length.
   *          A type 01 packet is received as acknowledgement and the file data is 
   *          then sent as a series of type 14 packets, each of which gets a type
   *          01 packet in acknowledgement. (PutFile)
   *    14  File content received or sent following type 12 or 13 packets. (FileData)
   *    15  Delete named file.  Response is a type 01 packet acknowledging success
   *          or responding that the file is absent. (RemoveFile)
   *    19  Request length of named file.  Response is a type 01 packet giving length
   *          or responding that the file is absent. (GeteFileSize)
   *    20  Request for the remote to enter update mode.  It carries no data
   *          and the response is a type 01 packet with no data, but the remote
   *          then disconnects its USB port followed by a reconnection. (StartUpdate)
   *    27  Request a 6-byte value that is possibly the complement of a hex serial
   *          number, returned in a type 01 packet.  There are no known side effects.
   *          (ReadSerialNumber) 
   */
  
  /*  
   *  Command Status Codes:
   *  Success = 0,
   *  InvalidCommand,
   *  ChecksumError,
   *  InvalidAddress,
   *  InvalidParameters,
   *  CommunicationError,
   *  UnableToPerform,
   *  MemoryFull,
   *  NoApplication,
   *  CapturedBufferOverflow,
   *  ReceivedPacketOutOfSequence,
   *  BatteryLow,
   *  InvalidProductInfo,
   *  InvalidUSBID,
   *  InvalidExternalFlash,
   *  InvalidModeNumber = 128u,
   *  InvalidDeviceType,
   *  InvalidDeviceCode,
   *  InvalidIRKeyCode,
   *  ReadError = 251u,
   *  DriverInternalError,
   *  OpenError,
   *  ChecksumNotMatch,
   *  TimeoutNoResponse
   */
  
  HIDManager hid_mgr;
  HIDDevice devHID;
  Remote remote = null;
  int thisPID;
  String deviceID;
  String signature;
  int firmwareAddress;
  int infoAddress;
  int E2address;
  int E2size;
  int addrSize;
  RemoteType remoteType = null;
  HIDDeviceInfo[] HIDinfo = new HIDDeviceInfo[10];
  HIDDeviceInfo HIDdevice = null;
  byte outReport[] = new byte[65];  // Note the asymmetry:  writes need outReport[0] to be an index 
  byte inReport[] = new byte[64];   // reads don't return the index byte
  byte dataRead[] = new byte[0x420];
  byte ssdIn[] = new byte[62];
  byte ssdOut[] = new byte[62];
  int interfaceType = -1;
  int firmwareFileCount = 0;
  int powerStatus = -1;  // Enhanced Power Management status, -1 = not used in this OS
  boolean upgradeSuccess = true;
  boolean isAppInfo2 = false;
  boolean newAppInfo2 =false;
  boolean forceUpgrade = false;
  boolean isPortUpg = false;
  LinkedHashMap< String, Hex > firmwareFileVersions = new LinkedHashMap< String, Hex >();
  LinkedHashMap< String, Hex > upgradeFileVersions = new LinkedHashMap< String, Hex >();
  LinkedHashMap< String, FileData > upgradeData = new LinkedHashMap< String, FileData >();
  List< String > sysNames = null;

  private int runningTotal = 0;
  
  private enum RemoteType
  {
    AVL, DIGITAL, XZITE, UNKNOWN
  }
  
  public class FileData
  {
    public String zName = null;
    public Hex version = null;
    public long versionNum = -1;
    public int address = -1;
  }
  
  int getPIDofAttachedRemote() {
    try  {
      hid_mgr = HIDManager.getInstance();
      System.err.println( "HIDManager " + hid_mgr + " devices are:" );
      HIDinfo = hid_mgr.listDevices();
      HIDdevice = null;
      if ( HIDinfo != null )
      {
        for (int i = 0; i<HIDinfo.length; i++)
        {
          System.err.println( "Device " + i + ": " + HIDinfo[i] );
          if (HIDdevice == null && HIDinfo[i].getVendor_id() == 0x06E7) {
            HIDdevice = HIDinfo[i];
          }
        }
        if ( HIDdevice != null )
        {
          String manString = HIDdevice.getManufacturer_string();
          String prodString = HIDdevice.getProduct_string();
          thisPID = HIDdevice.getProduct_id();
          addrSize = ( thisPID & 0x8000 ) == 0 ? 2 : 4;
          System.err.println( "Remote found: Manufacturer = " + manString + ", Product = " + prodString  
              + ", Product ID = " + String.format( "%04X", thisPID ) );
          return thisPID;
        }
      }
      else
      {
        System.err.println( "None" );
        return 0;
      }
    }  catch (Exception e) {
      return 0;
    }
    return 0;
  }

  public String getInterfaceName() {
    return "CommHID";
  }

  public String getInterfaceVersion() {
    return "0.5";
  }

  public String[] getPortNames() {
    String[] portNames  = {"HID"};
    return portNames;
  }

  public int getRemotePID() {
    return thisPID;
  }

//  public String getDeviceID()
//  {
//    return deviceID;
//  }

  byte jp12ComputeCheckSum( byte[] data, int start, int length ) {
    int sum = 0;
    int end = start + length;
    for (int i = start; i < end; i++)  {
      sum ^= (int)data[i] & 0xFF;
    }
    return (byte) sum;
  }

  void assembleFDRAreadAddress( int address, int addrSize, int blockLength, byte[] cmdBuff) {   
    // addrSize is the address size in bytes, 2 or 4
    cmdBuff[0] = 0x00;  //packet length
    cmdBuff[1] = ( byte )( 0x04 + addrSize );  //packet length
    cmdBuff[2] = 0x01;  //Read command
    cmdBuff[3] = (byte) ((address >> 24) & 0xff);  // overwritten if addrSize=2
    cmdBuff[4] = (byte) ((address >> 16) & 0xff);  // overwritten if addrSize=2
    cmdBuff[addrSize+1] = (byte) ((address >>  8) & 0xff);
    cmdBuff[addrSize+2] = (byte) (address & 0xff);
    cmdBuff[addrSize+3] = (byte) ((blockLength >>  8) & 0xff);
    cmdBuff[addrSize+4] = (byte) (blockLength & 0xff);
    cmdBuff[addrSize+5] = jp12ComputeCheckSum(cmdBuff, 0, addrSize+5);
  }
  
  boolean eraseFDRA_Lite( int startAddress, int endAddress ){
    byte[] cmdBuff = new byte[2*addrSize + 4];
    cmdBuff[0] = (byte) 0x00;  //packet length
    cmdBuff[1] = (byte) (2*addrSize + 2);  //packet length
    cmdBuff[2] = (byte) 0x03;  //erase command
    cmdBuff[3] = (byte)( (startAddress >> 24) & 0xff);
    cmdBuff[4] = (byte)((startAddress >> 16) & 0xff);
    cmdBuff[addrSize + 1] = (byte)((startAddress >>  8) & 0xff);
    cmdBuff[addrSize + 2] = (byte)(startAddress & 0xff);
    cmdBuff[addrSize + 3] = (byte)((endAddress >> 24) & 0xff);
    cmdBuff[addrSize + 4] = (byte)((endAddress >> 16) & 0xff);
    cmdBuff[2*addrSize + 1] = (byte)((endAddress >>  8) & 0xff);
    cmdBuff[2*addrSize + 2] = (byte)(endAddress & 0xff);
    cmdBuff[2*addrSize + 3] = jp12ComputeCheckSum(cmdBuff, 0, 2*addrSize + 3);
    System.arraycopy(cmdBuff, 0, outReport, 1, cmdBuff.length);
    try {
      devHID.write(outReport);
    } catch (Exception e) {
      return false;
    }
    if ( !readFDRAreport() || (dataRead[2] != 0) ) //Wait for remote to respond and check for error
      return false;
    return true;
  }
  
  /**
   *  Return codes: 0 = success, 1 = write failed, 2 = read failed, 3 = error returned 
   */
  private int enterService()
  {
    byte[] cmdBuff = {(byte)0x00, (byte)0x04, (byte)0x51, (byte)0x55,(byte)0xAA,(byte)0xAA };
    if ( !writeFDRAcmdReport(cmdBuff) )
    {
      return 1;
    }
    if ( !readFDRAreport() )  
    {
      return 2;
    }
    if ( dataRead[0] != 0 || dataRead[1] < 2 || dataRead[2] != 0 )
    {
      return 3;
    }
    return 0;
  }

  boolean writeFDRAblock( int address, int addrSize, byte[] buffer, int blockLength ) {
      byte[] cmdBuff = new byte[addrSize + 3]; 
      int pkgLen;
      if (blockLength > 0x3C - addrSize) 
        return false;
      pkgLen = blockLength + addrSize + 2;
      cmdBuff[0] = (byte) (pkgLen >> 8);  //packet length
      cmdBuff[1] = (byte) (pkgLen & 0xFF);  //packet length
      cmdBuff[2] = (byte) 0x02;  //write command
      cmdBuff[3] = (byte) ((address >> 24) & 0xff);
      cmdBuff[4] = (byte) ((address >> 16) & 0xff);
      cmdBuff[addrSize + 1] = (byte) ((address >>  8) & 0xff);
      cmdBuff[addrSize + 2] = (byte) (address & 0xff);
      System.arraycopy(cmdBuff, 0, outReport, 1, cmdBuff.length);  //outReport must contain an index byte
      System.arraycopy(buffer, 0, outReport, cmdBuff.length + 1, blockLength);
      outReport[blockLength + cmdBuff.length + 1] = jp12ComputeCheckSum(outReport, 1, blockLength + cmdBuff.length);
      try {
        devHID.write(outReport);
      } catch (Exception e) {
        return false;
      }
      return true;
    }

  boolean writeFDRAcmdReport(byte [] cmdBuff)  {
      System.arraycopy(cmdBuff, 0, outReport, 1, cmdBuff.length);
      try {
        devHID.write(outReport);
      } catch (Exception e) {
        return false;
      }
      return true;
  }
  
  boolean readFDRAreport()  {
    try {
      devHID.readTimeout(inReport, 3000);
      System.arraycopy(inReport, 0, dataRead, 0, 64);
    } catch (Exception e) {
      return false;
    }
    return true;
  }
  
  boolean FDRA_ReopenRemote()
  {
    byte[] cmdBuff = {(byte)0x00, (byte)0x02, (byte)0x51, (byte)0x53 };
    if ( !writeFDRAcmdReport(cmdBuff) )
    {
      return false;
    }
    if ( !readFDRAreport() || dataRead[0] != 0 )
    {
      return false;
    }
    return true;
  }
  
  int addrFromBytes( byte[] bArray, int offset )
  {
    int addr = 0;
    for ( int i = 0; i < addrSize; i++ )
    {
      addr = ( addr << 8 ) | ( bArray[ offset + i ] & 0xFF );
    }
    return addr;
  }
  
  int addrFromEntry( int entry )
  {
    if ( entry >= 0 )
      return entry;
    byte[] buffer = new byte[4];
    if (readFDRAnoUpdate( entry & 0x7FFFFFFF, buffer, addrSize ) != addrSize )
      return -1;
    return addrFromBytes( buffer, 0 );
  }
  
  boolean FDRA_USB_getInfoAndSig()  {
    // Following is based on GetIcInfo() in HidIf/UEI.USB.FCom/HidComm.cs.
    // There appear to be three formats here for the signature block data, with
    // two different sets of data represented.  The data sets are AppInfo1 and
    // AppInfo2, but there are two signature block formats for AppInfo2, 
    // distinguished by boolean newAppInfo2.  The code supports newAppInfo2==false
    // but it appears to be set to a fixed value newAppInfo2=true.
    byte[] cmdBuff = {(byte)0x00, (byte)0x02, (byte)0x50, (byte)0x52};
    int eBootVersion, icType, infoSize, infoOffset, infoCount;
    int sigStart, firmwareEntry, E2StartEntry, E2EndEntry, ALFStartEntry, temp;
    if (!writeFDRAcmdReport(cmdBuff))
      return false;
    if (!readFDRAreport() || (dataRead[0] != 0) || ( dataRead[1] != addrSize + 4 ) || (dataRead[2] != 0) )  
      return false;
    System.err.println( "Info read is:" );
    System.err.println( Hex.toString( dataRead, 32, 0, addrSize + 6 ) );
    eBootVersion = dataRead[ 3 ] & 0xFF;
    icType = dataRead[ 4 ] & 0xFF;
    System.err.println( "IC type = $" + Integer.toHexString( icType ) );
    isAppInfo2 = ( eBootVersion & 0x80 ) == 0x80;
    newAppInfo2 = true;
    infoOffset = isAppInfo2 ? 42 : 26;
    infoCount = isAppInfo2 ? 10 : 8;
    infoSize = infoOffset + infoCount * addrSize + 2;
    infoAddress = addrFromBytes( dataRead, 5 );
    sigStart = isAppInfo2 ? newAppInfo2 ? 6 : 16 : 0;
    
    if (readFDRAnoUpdate(infoAddress, dataRead, infoSize ) != infoSize)
      return false;
    System.err.println( "Sig block read is:" );
    System.err.println( Hex.toString( dataRead, 32, 0, infoSize ) );
    try {
      signature = new String(dataRead, sigStart, 6, "UTF-8");
    } catch (UnsupportedEncodingException e) {
    }
    if ( isAppInfo2 && newAppInfo2 )
    {
      infoCount = dataRead[ 42 ] & 0xFF;
      firmwareEntry = addrFromBytes( dataRead, 43 );
      firmwareAddress = addrFromEntry( firmwareEntry );
      E2StartEntry = addrFromBytes( dataRead, 43 + 2*addrSize );
      E2address = addrFromEntry( E2StartEntry );
      temp = 0;
      if ( infoCount < 4 )
      {
        E2size = 0x3000;
      }
      else 
      {
        ALFStartEntry = addrFromBytes( dataRead, 43 + 3*addrSize );
        temp = addrFromEntry( ALFStartEntry );
        E2size = temp - E2address;
      }
      if ( E2address < 0 || temp < 0 )
        return false;
    }
    else
    {
      firmwareEntry = addrFromBytes( dataRead, infoOffset );
      E2StartEntry = addrFromBytes( dataRead, infoOffset + 4*addrSize );
      E2EndEntry = addrFromBytes( dataRead, infoOffset + 5*addrSize );
      firmwareAddress = firmwareEntry;
      E2address = E2StartEntry;
      E2size = E2EndEntry - E2StartEntry + 1;
    }

//    E2StartPtr = ((dataRead[52] & 0xFF) << 16) + ((dataRead[53] & 0xFF) << 8) + (dataRead[54] & 0xFF);
//    E2EndPtr   = ((dataRead[56] & 0xFF) << 16) + ((dataRead[57] & 0xFF) << 8) + (dataRead[58] & 0xFF);
//    if (readFDRA(E2StartPtr, 4, dataRead, 0x04 )  != 0x04)
//      return false;
//    E2address = ((dataRead[0] & 0xFF) << 24) + ((dataRead[1] & 0xFF) << 16) + ((dataRead[2] & 0xFF) << 8) + (dataRead[3] & 0xFF);
//    if(readFDRA(E2EndPtr, 4, dataRead, 0x04 ) != 0x04)
//      return false;
//    temp = ((dataRead[0] & 0xFF) << 24) + ((dataRead[1] & 0xFF) << 16) + ((dataRead[2] & 0xFF) << 8) + (dataRead[3] & 0xFF);
//    E2size = temp - E2address;
    return true;
  }
  
  @Override
  public String openRemote(String portName) {
    setProgressName( "PREPARING:" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );   
    isPortUpg = portName != null && portName.equalsIgnoreCase( "UPG" );
    try  
    {
      if ( devHID != null )
      {
        System.err.println( "Setting current device to NULL" );
        devHID = null;
        waitForMillis( 200 );
      }
      if ( HIDdevice != null )
      {
        try
        {
          devHID = HIDdevice.open();
          if ( devHID != null )
          {
            System.err.println( "Open existing device succeeded" );
          }
        }
        catch( Exception e )
        {
          System.err.println( "Open existing device failed" );
        }
      }
      if ( devHID == null )
      {
        getPIDofAttachedRemote();
        if ( progressUpdater != null )
          progressUpdater.updateProgress( 30 ); 
        try
        {
          if ( HIDdevice != null )
          {
            devHID = HIDdevice.open();
          }
          if ( devHID != null )
          {
            System.err.println( "Open found device succeeded" );
          }
        }
        catch( Exception e )
        {
          System.err.println( "Open found device failed" );
        }
      }
      if ( devHID == null )
      {
        if ( progressUpdater != null )
          progressUpdater.updateProgress( -1 ); 
        System.err.println( "Failed to open remote" );
        return "";
      }
//      devHID = hid_mgr.openById(0x06E7, thisPID, null);
      devHID.enableBlocking();
      
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 50 );  
      
      List< Remote > remotes = RemoteManager.getRemoteManager().findRemoteBySignature( getRemoteSignature() );
      if ( remotes.size() > 0 )
      {
        // Value only used to test if RDF exists, to force testRemote(...) to be used
        // on raw download for remotes without RDFs.
        remote = remotes.get( 0 );
      }

      if ( remote != null )
      {
        if ( thisPID == 7 )
        {
          interfaceType = 6;
          remoteType = RemoteType.AVL;
        }
        else if ( thisPID >= 0x8001 && thisPID <= 0x8007 && thisPID != 0x8003 )
        {
          interfaceType = 0x201;
          remoteType = RemoteType.XZITE;
          sysNames = xziteSysNames;
        }
        else if ( thisPID == 0x8008 || thisPID == 0x8009 || thisPID == 0x8010 || thisPID == 0x8011 )
        {
          interfaceType = 0x106;
          remoteType = RemoteType.DIGITAL;
          sysNames = digitalSysNames;
        }
      }
      else
      {
        // Unknown remote.
        E2address = 0;   // This will be changed on testing
        E2size = 0x400;  // Seek to read 1K during testing
        interfaceType = 0x10;  // Let this value signify "unknown"; it needs to be > 0
        remoteType = RemoteType.UNKNOWN;
        return "HID";
      }
      

//        remote.load();
//        interfaceType = remote.isSSD() ? 0x201 : ( thisPID & 0x8000 ) == 0 ? 6 : 0x106;
//      }
//      else
//      {
//        // Unknown remote.
//        remote = null;
//        E2address = 0;   // This will be changed on testing
//        E2size = 0x400;  // Seek to read 1K during testing
//        interfaceType = 0x10;  // Let this value signify "unknown"; it needs to be > 0
//        return "HID";
//      }
      
      if ( interfaceType == 0x106 || interfaceType == 6 )
      {
        if ( interfaceType == 0x106 && portName != null && portName.equals( "UPG" ) )
        {
          return FDRA_ReopenRemote() ? "UPG" : "";
        }
        FDRA_ReopenRemote();
        if ( progressUpdater != null )
          progressUpdater.updateProgress( 70 );  
        waitForMillis( 200 );
        if ( FDRA_USB_getInfoAndSig() && E2address > 0 )
        {
          System.err.println( "GetInfoAndSig succeeded" );
        }
        else
        {
          System.err.println( "GetInfoAndSig failed" );
          return "";
        } 
      }
      else // if ( interfaceType == 0x201 )
      {
        E2address = 0;
        E2size = 0x80000;
      }
      if ( interfaceType == 0x201 && portName != null && portName.equals( "UPG" ) )
      {
        return "UPG";
      }
    }  
    catch (Exception e) 
    {
      return "";
    }
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 100 );  
    return "HID";
  }

  @Override
  public void closeRemote() {
    try  {
      devHID.close();
      System.err.println( "closeRemote() succeeded" );
    } catch (Exception e) {
      System.err.println( "closeRemote() failed" );
    }
  }

  @Override
  public String getRemoteSignature() {
    return "USB" + String.format( "%04X", thisPID );
  }

  @Override
  public int getRemoteEepromAddress() {
    return E2address;
  }

  @Override
  public int getRemoteEepromSize() {
    return E2size;
  }
  
  @Override
  public boolean remoteUsesSSD()
  {
    return remoteType == RemoteType.XZITE;
  }
  
  @Override
  public int getInterfaceType() {
    return interfaceType;
  }
  
  public String getTouchVersion( Hex version )
  {
    Hex hex = firmwareFileVersions.get( "MCUFirmware" );
    if ( hex == null && getVersionsFromRemote( false ) )
    {
      hex = firmwareFileVersions.get( "MCUFirmware" );
    }
    if ( hex != null )
    {
      if ( version != null )
      {
        version.put( hex.subHex( 2 ) );
      }
      short[] v = hex.getData();
      return v == null ? "Unknown" : "" + v[5] + "." + v[4] + "." + v[2];
    }
    return null;
  }
  
  private Hex getVersionFromFDRAremote( int index )
  {
    if ( !isAppInfo2 || !newAppInfo2 )
    {
      return null;
    }
    byte[] cmdBuff = {(byte)0x00, (byte)0x03, (byte)0x53, (byte)index, (byte)( 0x50 ^ index ) };
    if ( !writeFDRAcmdReport(cmdBuff) )
    {
      return null;
    }
    if ( !readFDRAreport() || dataRead[0] != 0 )
    {
      return null;
    }
    Hex hex = new Hex( 6 );
    for ( int i = 0; i < 6; i++ )
    {
      hex.set( ( short )( inReport[ i + 3 ] & 0xFF ), i );
    }
    return hex;
//    char[] c = new char[6];
//    for ( int i = 0; i < 6; i++ )
//    {
//      c[i] = ( char )inReport[ i + 3 ];
//    }
//    String v = new String( c );
//    return v.substring( 0, 4 ) + "." + v.substring( 4 );
  }
  
  public LinkedHashMap< String, Integer > getXZITEfileList()
  {
    LinkedHashMap< String, Integer > listing = new LinkedHashMap< String, Integer >();   
    List< String > fileList = new ArrayList< String >();
    int written = writeTouchUSBReport( new byte[]{0x17}, 1 );  // Ls
    if ( written != 65 || readTouchUSBReport( ssdIn ) < 0 )
    {
      System.err.println( "List files failed to initiate" );
      return null;
    }
    System.err.println( "File list data:" );
    byte[] o = new byte[2];
    o[0] = 1;
    boolean more = true;
    int count = 0;
    while ( more )
    {
      // Read type 18 firmware version packet.
      if ( readTouchUSBReport( ssdIn ) < 0 )
      {
        System.err.println( "Read of file list failed" );
        return null;
      }
      if ( ssdIn[ 0 ] == 0x18 )
      {
        // List data
        if ( count != ( ( ssdIn[ 2 ] & 0xFF ) | ( ( ssdIn[ 3 ] & 0xFF ) << 8 ) ) )
        {
          System.err.println( "Sequence error in reading file list" );
          return null;
        }
        int dataLen = ssdIn[ 4 ] & 0xFF;
        more = ssdIn[ 5 ] != 0;
        Hex hex = new Hex( ssdIn );
        int pos = 6;
        while ( pos < dataLen + 6 )
        {
          int nameLen = hex.getData()[ pos++ ];
          String name = hex.subString( pos, nameLen );
          pos += nameLen;
          fileList.add( name );
        }
      }
      o[1] = ssdIn[ 1 ];
      // Send acknowledgement.
      writeTouchUSBReport( o, 2 );
      count++;
    }
    Collections.sort( fileList );
    for ( String name : fileList )
    {
      byte[] sizeData = readTouchFileBytes( name, true );
      Integer size = sizeData.length == 4 ? intFromHex( new Hex( sizeData ), 0, true ) : null;
      listing.put( name, size );
    }
    return listing;
  }
  
  public boolean reformatXZITE()
  {
    setProgressName( "FORMATTING:" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );
    String title = "Formatting XSight";
    int written = writeTouchUSBReport( new byte[]{0x16}, 1 );
    upgradeSuccess = written == 65;
    int read = readTouchUSBReport(ssdIn, 5000);
    if ( read != 64 || ssdIn[ 2 ] != 0 )
    {
      upgradeSuccess = false;
    }
    if ( !upgradeSuccess )
    {
      System.err.println( "Formatting of file system failed" );
      String message = "Formatting of file system failed.\n"
          + "Aborting the rebuild.";
      JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
      return false;
    }
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 40 );
    System.err.println( "Formatting of file system succeeded" );
    LinkedHashMap< String, Integer > fileList = getXZITEfileList();
    System.err.println( "List of remaining files (should be empty):" );
    for ( String name : fileList.keySet() )
    {
      System.err.println( name );
    }
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 100 );
    File sysFile = RemoteMaster.getRmirSys();
    List< String > filesToWrite = new ArrayList< String >();
    for ( String name : sysNames )
    {
      if ( name.indexOf( '.' ) > 0 )
      {
        filesToWrite.add(  name );
      }
    }
    writeSystemFiles( sysFile, filesToWrite, 3 ); // This updates upgradeSuccess
    String message = upgradeSuccess ? "System files successfully recreated.\n"
        + "Reformatting and rebuild complete."
        : "Error in reinstalling system files.  Rebuild failed.";  
    JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
    return upgradeSuccess;
  }
  
  public List< String > verifyXZITEfiles()
  {
    LinkedHashMap< String, Integer > fileMap = getXZITEfileList();
    List< String > ucSysNames = new ArrayList< String >();
    List< String > ucRemNames = new ArrayList< String >();
    List< String > comments = new ArrayList< String >();
    for ( String name : xziteSysNames )
    {
      ucSysNames.add( name.toUpperCase() );
    }
    File sysFile = RemoteMaster.getRmirSys();
    if ( !setFileData( sysFile ) )
    {
      comments.add( "Unable to verify file content." );
    }
    else try
    {
      ZipFile zipfile = new ZipFile( sysFile );
      setProgressName( "VERIFYING SYSTEM FILES:" );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 0 );
      int total = fileMap.size();
      int index = 0;
      for ( String name : fileMap.keySet() )
      {
        index++;
        if ( progressUpdater != null )
          progressUpdater.updateProgress( (int)((double)index / total * 100) );
        if ( !ucSysNames.contains( name.toUpperCase() ) )
        {
          // File is not a system file
          continue;
        }
        if ( RemoteMaster.admin && !sysNames.contains( name ) )
        {
          int ndx = ucSysNames.indexOf( name.toUpperCase() );
          String s = "Case error in name: file " + name + " should be " + sysNames.get( ndx  );
          comments.add( s );
        }
        ucRemNames.add( name.toUpperCase() );
        String zName = upgradeData.get( name.toUpperCase() ).zName;
        ZipEntry entry = zipfile.getEntry( zName );
        int length = ( int )entry.getSize();
        if ( length < 0 )
        {
          String s = "No data available to verify file " + name + ".";
          comments.add( s );
          continue;
        }
        InputStream zip = zipfile.getInputStream( entry );
        byte[] sysData = RemoteMaster.readBinary( zip, length );
        Hex sysHex = new Hex( sysData );
        RemoteConfiguration.decryptObjcode( sysHex );
        byte[] remData = readSystemFile( name );
        if ( remData == null )
        {
          String s = "Unable to read file " + name + " from remote.";
          comments.add( s );
          continue;
        }
        Hex remHex = new Hex( remData );
        if ( !sysHex.equals( remHex ) )
        {
          String s = "File " + name + " is corrupt.";
          comments.add( s );
        }
        else
        {
          System.err.println( "File " + name + " verified" );
        }
      }
      for ( int i = 0; i < ucSysNames.size(); i++ )
      {
        String ucName = ucSysNames.get( i );
        if ( ucName.indexOf( '.' ) > 0 && !ucRemNames.contains( ucName ) )
        {
          String s = "System file " + sysNames.get( i ) + " is missing.";
          comments.add( s );
        }
      }
      zipfile.close();
      if ( comments.isEmpty() )
      {
        comments.add( "All system files are correct." );
      }
    }
    catch ( Exception e )
    {
      String s = "Error in verification process.  Ending verification.";
      comments.add( s );
    }
    System.err.println( "File verification complete" );
    return comments;
  }


  private int testRemote( byte[] buffer, int length )
  {
    if ( RemoteMaster.admin )
    {
      System.err.println( "Read dialog starts:");
//      File sysFile = new File( RemoteMaster.getWorkDir(), "RMIR.sys" );
//      setFileData( sysFile ); 
//      convertZipFile( "RemoteFirmware.zip", "ConvertedFirmware.zip", true );
//      return 0;
    }
    String title = "Unknown remote";
    String message = null;
    int numRead = 0;
    System.err.println();
    System.err.println( "Starting diagnostics for unknown USB remote with PID = " + String.format( "%04X", thisPID ) );
    boolean identified = false;
    if ( thisPID > 0x8000 && thisPID <= 0x8007 )
    {
      if ( getVersionsFromRemote( false ) )
      {
        identified = true;
        message = "RMIR has found an XSight Touch style of remote with the following data:"
            + "\n    Signature = " + getRemoteSignature()
            + "\n    Processor = S3F80"
            + "\n    Firmware version = " + getTouchVersion( null );
        if ( RemoteMaster.admin )
        {
          message += "\n\nDo you want to run further tests in Upgrade mode?";
          if ( NegativeDefaultButtonJOptionPane.showConfirmDialog( null, message, title, 
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) == JOptionPane.YES_OPTION )
          {
            LinkedHashMap< String, Integer > listing = getXZITEfileList(); 
            if ( listing == null )
            {
              return 0;
            }
            for ( String name : listing.keySet() )
            {
              System.err.println( name + "   " + listing.get( name ) );
            }
//            byte[] file = readSystemFile( "firsttime" );
//            System.err.println( "firsttime data = " + ( new Hex( file ) ) );
            return length;
          }
        }
      }
    }
    else if ( thisPID > 0x8007 && thisPID <= 0x8011 || thisPID == 0x0007 )
    {
      System.err.println( "XSight FDRA remote" );
      if ( FDRA_USB_getInfoAndSig() && E2address > 0 )
      {
        identified = true;
        Hex version = getVersionFromFDRAremote( 0 );
        String versionString = "Unknown";
        if ( version != null )
        {
          versionString = version.subString( 0, 4 ) + "." + version.subString( 4, 2 );
        }
        message = "RMIR has found an XSight FDRA style of remote with the following data:"
            + "\n    Signature = " + getRemoteSignature()
            + "\n    Processor = " + ( remoteType == RemoteType.DIGITAL ? "MAXQ622" : "HCS08" )
            + "\n    Firmware version = " + versionString
            + "\n    Firmware address = $" + Integer.toHexString( firmwareAddress ).toUpperCase()
            + "\n    EEPROM address = $" + Integer.toHexString( E2address ).toUpperCase()
            + "\n    EEPROM size = $" + Integer.toHexString( E2size ).toUpperCase();
        //          numRead = readFDRA( E2address, buffer, length );
//        if ( RemoteMaster.admin )
//        {
//          message += "\n\nDo you want to download the firmware?";
//          if ( NegativeDefaultButtonJOptionPane.showConfirmDialog( null, message, title, 
//              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) == JOptionPane.YES_OPTION )
//          {
//            byte[] cmdBuff = {(byte)0x00, (byte)0x04, (byte)0x51, (byte)0x55,(byte)0xAA,(byte)0xAA };
//            if ( !writeFDRAcmdReport(cmdBuff) )
//            {
//              System.err.println( "Failed to write Service request" );
//              message = "Failed to write Service request.\n"
//                  + "Aborting test";
//              JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
//              return 0;
//            }
//            if ( !readFDRAreport() || dataRead[0] != 0 || dataRead[1] < 2 || dataRead[2] != 0 )
//            {
//              System.err.println( "Failed to enter Service mode" );
//              message = "Failed to enter Service mode.\n"
//                  + "Aborting test";
//              JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
//              return 0;
//            }
//
//            // These start and end values can be overridden, to read a different
//            // section of the flash.
//            int start = firmwareAddress;
//            int end = 0x10000;
//            message = "Enter end address as hex:";
//            boolean valid = false;
//            String reply = JOptionPane.showInputDialog( null, message, title, JOptionPane.QUESTION_MESSAGE );
//            if ( reply != null )
//            {
//              try {
//                end = Integer.parseInt( reply,  16 );
//                valid = end > start;
//                if ( !valid )
//                  end = start + 0x400;
//              } 
//              catch ( Exception e ){}
//            }
//            
//
////            int end = E2address;
////            int end = 0x10000;
//            
//            int dataSize = end - start;
//            byte[] dataBuffer = new byte[ dataSize ];
//            int tempSize =0x4000;
//            byte[] tempBuffer = new byte[ tempSize ];
//            int remaining = dataSize;
//            int pos = 0;
//            while ( valid && remaining > 0 )
//            {
//              waitForMillis( 100 );
//              int size = remaining > tempSize ? tempSize : remaining;
//              numRead = readFDRA( start + pos, tempBuffer, size );
//              if ( numRead == size )
//              {
//                for ( int i = 0; i < size; i++ )
//                {
//                  dataBuffer[ pos + i ] = tempBuffer[ i ];
//                }
//                pos += size;
//                remaining -= size;
//              }
//              else
//              {
//                break;
//              }
//            }
//            if ( pos == dataSize )
//            {
//              for ( int i = 0; i < length; i++ )
//              {
//                buffer[ i ] = dataBuffer[ i ];
//              }
//
//              System.err.println( "Firmware start address = " + String.format( "%04X", start ) 
//                  + ", size = " + String.format( "%04X", dataSize ) );
//              System.err.println( "Firmware data is:" );
//              //                System.err.println( "The data read from $" + String.format( "%04X", address ) + " is:" );
//              System.err.println( Hex.toString( dataBuffer, 32, 0, dataSize ) );
//              System.err.println();
//              message = "Firmware has been read.  Download panel will display\n"
//                  + "the start of its data.  The entire download is in\n"
//                  + "the rmaster.err file.";
//              JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
//            }
//            else
//            {
//              System.err.println( "Firmware not readable" );
//              message = "Firmware appears to be unreadable.";
//              JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
//              return 0;
//            }
//          }
//          return length;
//        }
      }
    }  
    if ( !identified )
    {
      message = "RMIR has found a remote with USB PID " + String.format( "%04X", thisPID )
          +"\nbut cannot identify it further.";
    }
    if ( !RemoteMaster.admin )
    {
      message += "\n\nYou may wish to post a message in the JP1 forums to seek help in\n"
          + "creating an RDF for this remote.  If so, please post the rmaster.err\n"
          + "file that you will find in the RMIR installation folder and include a\n"
          + "link to that file in your message.";
    }
    JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
    return length;
  }
  
  private int readFDRA( int address, byte[] buffer, int length )
  {
    setProgressName( getUse() == Use.DOWNLOAD ? "DOWNLOADING:" : "VERIFYING UPLOAD:" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );
    return readFDRAnoUpdate( address, buffer, length );
  }
  
  private int readFDRAnoUpdate( int address, byte[] buffer, int length )
  {
    byte[] chunkBuffer = new byte[ chunkSize ];
    int remaining = length;
    int pos = 0;
    while ( remaining > 0 )
    {
      waitForMillis( 100 );
      int size = remaining > chunkSize ? chunkSize : remaining;
      int numRead = readFDRAchunk( address, pos, chunkBuffer, size, length );
      if ( numRead == size )
      {
        System.arraycopy( chunkBuffer, 0, buffer, pos, size );
        pos += size;
        remaining -= size;
      }
      else
      {
        break;
      }
    }
    return pos;
  }

  int readFDRAchunk( int address, int delta, byte[] buffer, int length, int total ) {
    address += delta;
    System.err.println();
    System.err.println( "Starting FDRA read of $" + Integer.toHexString( length ).toUpperCase() + " bytes at $" + String.format( "%05X", address ) );
    byte[] cmdBuff = new byte[10];
    assembleFDRAreadAddress(address, addrSize, length, cmdBuff);
    int numToRead = length + 4;  // total packet  length plus error byte and checksum
    if (!writeFDRAcmdReport(cmdBuff))
    {
      System.err.println( "Failed to initiate FDRA read" );
      return -1;
    }
    Arrays.fill( buffer, (byte)0 );
    int numReports = 1 + numToRead/64;
    int dataIdx = 0;
    int totalRead = 0;
    int reportOffset = 3;  //First report has length and error bytes
    boolean success = true;
    for (int i=0; i < numReports; i++) {
      try 
      {
        Arrays.fill( inReport, ( byte )0xFF );
        // Set timeout to 200ms for testing unknown remote, but 3000ms in normal operation
        int timeout = remoteType == RemoteType.UNKNOWN ? 200 : 3000;
        int numRead = devHID.readTimeout(inReport, timeout);
        if ( numRead == 0 )
        {
          System.err.println( "Read attempt timed out for FDRA report " + i + " of " + numReports );
          success = false;
          break;
        }
        else if ( numRead != 64 )
        {
          System.err.println( "Incomplete read of FDRA report " + i + " of " + numReports + ", " + numRead + " bytes of 64 read" );;
          success = false;
          // return -1;
        }
        else if ( i == 0 && inReport[ 2 ] != 0 )
        {
          System.err.println( "Read of $" + Integer.toHexString( length ).toUpperCase() + " bytes at $" + String.format( "%05X", address )
              + " returned error code " + inReport[ 2 ] + "; full error report is:" );
          int size = inReport.length;
          short[] e2Data = new short[ size ];
          for ( int j = 0; j < size; j++ )
          {
            e2Data[ j ] = ( short )( inReport[ j ] & 0xFF );
          }
          System.err.println( Hex.toString( e2Data, 32 ) );
          success = false;
          // return -1;
        }
        System.arraycopy(inReport, reportOffset, buffer, dataIdx, 
                              Math.min(length - dataIdx, 64 - reportOffset));
        totalRead += numRead;
      } 
      catch (Exception e) 
      {
        System.err.println( "Failed at read of FDRA report " + i + " of " + numReports );
        return -1;
      }
      dataIdx += 64 - reportOffset;
      reportOffset = 0;
      int progress = Math.min( dataIdx, length );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( (int)((double)( delta + progress) / total * 100 ) );
    }
    return success ? length : totalRead;
  }
  
  private int writeFDRA( int address, byte[] buffer, int length, boolean erase )
  {
    setProgressName( "UPLOADING:" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );
    byte[] chunkBuffer = new byte[ chunkSize ];
    int remaining = length;
    int pos = 0;
    while ( remaining > 0 )
    {
//      waitForMillis( 100 );
      int size = remaining > chunkSize ? chunkSize : remaining;
      System.arraycopy( buffer, pos, chunkBuffer, 0, size );
      int numWritten = writeFDRAchunk( address, pos, chunkBuffer, size, length, erase );
      if ( numWritten == size )
      {
        pos += size;
        remaining -= size;
      }
      else
      {
        break;
      }
    }
    return pos;
  }
  
  int writeFDRAchunk( int address, int delta, byte[] buffer, int length, int total, boolean erase )  {
    address += delta;
    int writeBlockSize = 0x3C - addrSize;
    int erasePageSize = 0x200;
    int offset, endAdr;
    int blockLength = writeBlockSize;
    byte tempBuf[] = new byte[65];
    if ((address < E2address) || (address + length > E2address + E2size) )
      return -1;
    if ((length % erasePageSize) != 0)
      return -1;
    endAdr = address + length - 1;
    if ( erase && !eraseFDRA_Lite( address, endAdr ) )
      return -1;
    offset = 0;
    do {
      if (( offset + blockLength ) > length )
        blockLength = length - offset;
      System.arraycopy(buffer, offset, tempBuf, 0, blockLength);
      if ( !writeFDRAblock( address + offset, addrSize, tempBuf, blockLength ))
        return -1;
      if ( !readFDRAreport() || (dataRead[2] != 0) ) //Wait for remote to respond and check for error
        return -1;
      offset += blockLength;
      
      if ( progressUpdater != null )
        progressUpdater.updateProgress( (int)((double)( delta + offset) / total * 100 ) );
    }  while ( offset < length ); 
    return offset;
  }
  
  @Override
  public int readRemote( int address, byte[] buffer, int length ) 
  {
    int bytesRead = -1;
    if ( remoteType == RemoteType.UNKNOWN || remote == null )
    {
      String title = "Unknown remote";
      String message = "This remote is not recognised by RMIR but appears to be of the\n"
                     + "XSight type.  RMIR can run further diagnostics to help identify\n"
                     + "it further.  This will typically take less than 30 seconds but in\n"
                     + "exceptional circumstances can take up to 10 minutes or so.\n\n"
                     + "Would you like to run these diagnostics now?";
      if ( NegativeDefaultButtonJOptionPane.showConfirmDialog( null, message, title, 
          JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) == JOptionPane.YES_OPTION )
      {
        bytesRead = testRemote( buffer, length );
        return bytesRead;
      }
      else
      {
        return -1;
      }
    }
    
    forceUpgrade = false;
    boolean noUpgrade = RemoteMaster.noUpgradeItem.isSelected() || isPortUpg
        || remoteType != RemoteType.XZITE ;
    
    if ( getUse() == Use.DOWNLOAD && !noUpgrade )
    {
      if ( RemoteMaster.admin )
      {
        System.err.println( "Read dialog starts:");
      }
      forceUpgrade = RemoteMaster.forceUpgradeItem.isSelected();
      // Read firmware file versions from remote
      setProgressName( "CHECKING FOR UPGRADE:" );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 0 );
      firmwareFileVersions.clear();
      if ( !getVersionsFromRemote( true ) ) 
      {
        return 0;
      }
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 30 );
      // Test if system file required for upgrading is present
      boolean doUpgradeTest = false;
      String title = "Firmware upgrade";
      File sysFile = RemoteMaster.getRmirSys();
      if ( sysFile.exists() )
      {
        System.err.println( "Version numbers from remote:" );
        for ( String name : firmwareFileVersions.keySet() )
        {
          System.err.println( "  " + firmwareFileVersions.get( name ) + "  " + name );
        }

        if ( setFileData( sysFile ) )
        {
          if ( progressUpdater != null )
            progressUpdater.updateProgress( 40 );       
          if ( verifyFileVersions( sysFile ) )
          {
            if ( progressUpdater != null )
              progressUpdater.updateProgress( 50 );
            doUpgradeTest = true;
          }
        }
        if ( !doUpgradeTest )
        {
          String message = "The file RMIR.sys appears to be corrupt, so unable to\n"
              + "test for firmware upgrade.  Press OK to continue without\n" 
              + "this test, otherwise press Cancel.";

          if ( JOptionPane.showConfirmDialog( null, message, title, JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.WARNING_MESSAGE ) == JOptionPane.CANCEL_OPTION )
          {
            return 0;
          }
          else
          {
            if ( progressUpdater != null )
              progressUpdater.updateProgress( 70 ); 
          };
        }
      }

      if ( doUpgradeTest )
      {
        // Test for upgrade and perform it if required
        List< String > changed = new ArrayList< String >();
        List< String > newFiles = new ArrayList< String >();
        //      List< String > forDeletion = new ArrayList< String >();

        int[] upgNeeds = testForUpgrade( changed, newFiles );
        if ( progressUpdater != null )
          progressUpdater.updateProgress( 100 ); 
        if ( upgNeeds[ 0 ] > 0 )
        {
          String message = upgNeeds[ 2 ] == 1 ? 
              "There is a firmware upgrade available for this remote.  You may\n"
              + "install it now or you can continue the current operation without\n"
              + "installing it.\n\n" :
                upgNeeds[ 2 ] == 0 ?
                    "You already have the latest firmware installed but may perform\n"
                    + "a reinstallation if you wish.\n\n" :
                      upgNeeds[ 2 ] == -1 ?
                          "It appears that the current firmware in your remote is a later\n"
                          + "version than is available here as an upgrade, but you may install\n"
                          + "the upgrade version if you wish.\n\n" :
                            "There is a firmware revision available that will upgrade some files\n"
                            + "but downgrade others.  You should take advice from the JP1 forum\n"
                            + "before installing it, but you may do so if you wish.\n\n";
          message += "A firmware update should preserve the current setup, but it is\n" 
              + "recommended that you save the current setup as a .rmir file before\n"
              + "upgrading.\n\n";
          if ( RemoteMaster.ioNeedsPowerManagementCheck( this ) )
          {
            message += "You appear to be using Windows 8.1 or later, which support Enhanced\n"
                + "Power Management.  This may cause issues during a firmware upgrade.\n"
                + "You should make sure that you know how to use regedit to edit the\n"
                + "Windows registry before proceeding.  If registry changes are neeeded\n"
                + "then messages will pop up to tell you exactly what change to make and\n"
                + "how to proceed after making them.\n\n";
          }
          message += "What action do you want to take?";

          String[] buttons = new String[]{
              "<html>Continue without installation</html>",
          "<html>Install the firmware</html>" };

          int response = JOptionPane.showOptionDialog( null, message, title, JOptionPane.DEFAULT_OPTION, 
              JOptionPane.PLAIN_MESSAGE, null, buttons, buttons[ 0 ] );

          if ( response == 1 )
          {
            message =  "A firmware upgrade involves updating both the firmware of the central\n"
                + "processor and a series of support files.  Sometimes that of the central\n"
                + "processor is already up to date and only the support files need updating.\n"
                + "In that case the upgrade runs to completion as a single process.  If the\n"
                + "processor firmware needs updating, however, the upgrade takes place as a\n"
                + "series of stages, during which the remote will restart twice.  Each restart\n"
                + "involves the remote disconnecting from the PC then reconnecting.  Usually\n"
                + "this reconnection takes place automatically. However, with some remotes\n"
                + "and/or PCs a pop-up will ask the user to disconnect and then reconnect the USB\n"
                + "cable in the course of the upgrade and to press OK to continue when you have\n"
                + "done so.  Please follow any such instructions that appear on the PC.\n\n"
                + "If you get a message from Windows saying \"USB device not recognised\" or\n"
                + "something similar while the progress bar is saying \"Waiting for reconnection\",\n"
                + "please wait for the progress bar to reach its end, taking at most one minute.\n"
                + "The pop-up about disconnection and reconnection will then appear, and doing so\n"
                + "will resolve the problem.\n\n"
                + "Do you still want to continue with the firmware upgrade?";
            response = JOptionPane.showConfirmDialog( null, message, title, 
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
          }
          else
          {
            message = "To stop the firmware upgrade being offered in future without\n"
                + "installing it, check the item \"No XSight Firmware Upgrade\"\n"
                + "in the Options menu.  This will remain checked each time you\n"
                + "open RMIR until you specifically uncheck it.";
            JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
            response = JOptionPane.NO_OPTION;
          }

          if ( response == JOptionPane.YES_OPTION )
          {
            System.err.println( "Proceeding with firmware revision" );
            if ( remoteType == RemoteType.XZITE )
            {
              if ( !upgradeXZITE( upgNeeds, changed, newFiles ) )
              {
                return 0;
              }
            }
            else
            {
              RemoteMaster.forceUpgradeItem.setSelected( false );
              message = "Firmware upgrade for this remote is not yet implemented.\n"
                  + "Continuing with normal download.";
              JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
            }
          }  // if ( response == JOptionPane.YES_OPTION )
        } // if ( upgNeeds[ 0 ] > 0 )
      } // if ( doUpgradeTest )
    } // if ( getUse() == Use.DOWNLOAD )

    if ( remoteType == RemoteType.DIGITAL )
    {
      bytesRead = readFDRA(address, buffer, length);
    }
    else if ( remoteType == RemoteType.XZITE )
    { 
      bytesRead = readTouch( buffer );
    }
    else if ( remoteType == RemoteType.AVL )
    {
      bytesRead = readFDRA(address, buffer, length);
    }
    
    return bytesRead;
  }
  
  private boolean upgradeXZITE( int[] upgNeeds, List< String > changed, List< String > newFiles )
  {
    System.err.println( "Proceeding with firmware revision" );
    String title = "Firmware upgrade";
    String message = null;
    File sysFile = RemoteMaster.getRmirSys();
    upgradeSuccess = true;
    if ( upgNeeds[ 1 ] > 0 )
    {
      setProgressName( "ENTERING UPGRADE MODE:" );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 0 );
      // Test the MCUFirmware update file for validity
      try
      {
        ZipFile zipIn = new ZipFile( sysFile );
        String zName = upgradeData.get( "MCUFIRMWARE" ).zName;
        ZipEntry entry = zipIn.getEntry( zName );
        int eLength = ( int )entry.getSize();
        if ( eLength != 0x40008 )
        {
          System.err.println( "MCUFirmware file has invalid length" );
          zipIn.close();
          return false;
        }
        InputStream zip = zipIn.getInputStream( entry );
        byte[] data = RemoteMaster.readBinary( zip, eLength );
        zipIn.close();
        Hex hex = new Hex( data );
        RemoteConfiguration.decryptObjcode( hex );
        data = hex.toByteArray();
        int crc = 0;
        if ( ( crc = verifyCRC( data, 8 ) ) < 0 )
        {
          System.err.println( "MCUFirmware file has invalid CRC checksum" );
          return false;
        }

        if ( !getVersionsFromRemote( true ) )
        {
          return false;
        }
        if ( progressUpdater != null )
          progressUpdater.updateProgress( 20 );
        // Put remote into update mode with type 0x20 packet.
        // Change packet type to 0x27 for testing without entering update mode.
        writeTouchUSBReport( new byte[]{0x20}, 1 );
        if ( readTouchUSBReport( ssdIn ) < 0 || ssdIn[ 2 ] != 0 )
        {
          System.err.println( "Request to disconnect failed" );
          message = "Upgrade failed.  Request to disconnect failed.\n"
              + "Aborting firmware upgrade.";
          JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
          return false;
        }
        if ( progressUpdater != null )
          progressUpdater.updateProgress( 40 );
        waitForMillis( 7000 );
        boolean success = false;
        if ( waitForTouchReconnection()
            && writeFirmwareFile( data ) )
        {
          setProgressName( "CLOSING UPGRADE MODE:" );
          if ( progressUpdater != null )
            progressUpdater.updateProgress( 0 );
          Arrays.fill( ssdOut, ( byte )0 );
          ssdOut[ 0 ] = 0x23;
          ssdOut[ 2 ] = ( byte )( crc & 0xFF );
          ssdOut[ 3 ] = ( byte )( ( crc >> 8 ) & 0xFF );
          int n = -1;
          do {
            int written = writeTouchUSBReport( ssdOut, 62 );
            success = written == 65;
            int read = readTouchUSBReport(ssdIn);
            if ( read != 64 || ssdIn[ 2 ] != 0 )
            {
              success = false;
            }
            waitForMillis( 500 );
            n++;
          }  while ( !success && n < 20 );
        }
        if ( !success )
        {
          // If powerStatus == 1 then a message has already appeared
          if ( powerStatus != 1 )
          {
            System.err.println( "Writing of upgraded MCU firmware failed" );
            message = "Upgrade failed.  Unable to write MCU firmware.\n\n"
                + "Please disconnect the remote, remove the batteries, put them\n"
                + "back in, reconnect the remote and repeat the upgrade process.";
            JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
          }
          return false;
        }
      } // try
      catch( Exception e )
      {
        System.err.println( "MCU firmware upgrade error" );
        return false;
      }
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 100 );
      waitForMillis( 7000 );
    }   // if ( upgNeeds[ 1 ] > 0 )

    if ( ( upgNeeds[ 1 ] == 0 || waitForTouchReconnection() ) &&
        getVersionsFromRemote( false ) &&
        testForUpgrade( changed, newFiles ) != null &&
        writeSystemFiles( sysFile, changed, 1 ) &&
        waitForMillis( 300 ) &&
        getVersionsFromRemote( false ) &&
        testForUpgrade( changed, newFiles ) != null &&
        writeSystemFiles( sysFile, newFiles, 2 ) &&
        waitForMillis( 300 ) &&
//        deleteSystemFiles( forDeletion ) &&
//        waitForMillis( 300 ) &&
        getVersionsFromRemote( false ) &&
        upgradeSuccess )
    {
      message = "Upgrade succeeded.  Continuing with normal download.";
      JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
    }
    else
    {
      // If powerStatus == 1 then a message has already appeared
      if ( powerStatus != 1 )
      {
        message = "Upgrade failed. Unable to update all support files.\n\n"
            + "Please disconnect and reconnect the remote, then repeat the\n"
            + "upgrade process";
        JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
      }
      return false;
    }
    return true;
  }

  private int getEndPKG( int fileStart, byte[] buffer )
  {
    int pos = fileStart;
    int numIcons = ( buffer[ pos + 12 ] & 0xFF ) + 0x100 * ( buffer[ pos + 13 ] & 0xFF );
    int numEntries = ( buffer[ pos + 14 ] & 0xFF ) + 0x100 * ( buffer[ pos + 15 ] & 0xFF );   
    pos += 16;
    int startIndex = pos + 28 * numIcons;
    int iconEnd = 16 + 28 * numIcons + numEntries;
    for ( int i = 0; i < numEntries; i++ )
    {
      int j = buffer[ startIndex + i ] & 0xFF;
      if ( j == 0 )
      {
        continue;
      }
      int k = pos + 28 * ( j - 1 );
      int width = ( buffer[ k + 8 ] & 0xFF ) + 0x100 * ( buffer[ k + 9 ] & 0xFF );
      int height = ( buffer[ k + 10 ] & 0xFF ) + 0x100 * ( buffer[ k + 11 ] & 0xFF );
      int start = ( buffer[ k + 16 ] & 0xFF ) + 0x100 * ( buffer[ k + 17 ] & 0xFF ) + 0x10000 * ( buffer[ k + 18 ] & 0xFF );
      int start2 = ( buffer[ k + 20 ] & 0xFF ) + 0x100 * ( buffer[ k + 21 ] & 0xFF ) + 0x10000 * ( buffer[ k + 22 ] & 0xFF );
      int excess = start2 == 0 ? 0x100 : 0x200;
      int baseSize = ( buffer[ k + 24 ] & 0xFF ) + 0x100 * ( buffer[ k + 25 ] & 0xFF ) - excess;
      int pixSize = height * width;
      int byteWidth = baseSize / pixSize;
      int bufferSize = pixSize * byteWidth;

      start += bufferSize;
      iconEnd = start2 == start ? start + pixSize : start;
    }
    return fileStart + iconEnd;
  }
  
  private int getEndBXML( int fileStart, byte[] buffer )
  {
    int pos = fileStart;
    int itemsLength = ( buffer[ pos + 14 ] & 0xFF ) |  ( ( buffer[ pos + 15 ] & 0xFF ) << 8 );
    pos += 17 + itemsLength;
    List< Integer > tags = new ArrayList< Integer >();
    while ( true )
    {
      int tag = buffer[ pos++ ] & 0xFF;
      if ( ( tag & 0x80 ) == 0 )
      {
        tags.add( 0, tag );
        pos += ( buffer[ pos ] & 0xFF ) + 1;
      }
      else
      {
        int last = tags.remove( 0 );
        if ( tag != ( last | 0x80  ) )
        {
          System.err.println( "XCF file nesting error at " + Integer.toHexString( pos - 1 ) );
          break;
        }
        if ( tags.isEmpty() )
        {
          break;
        }
      }  
    }
    return pos;
  }
  
  private int ssdInCheck()
  {
    boolean res = ( ssdIn[ 0 ] == 1 ) && ( ssdIn[ 1 ] == 0 );
    for ( int i = 3; i < 62; i++ )
    {
      res &= ssdIn[ i ] == 0;
    }
    if ( !res )
    {
      System.err.println( "Input packet failure: " + ssdIn[ 0 ] + "" + ssdIn[ 1 ] + "" + ssdIn[ 2 ] + "" + ssdIn[ 3 ] + "" + ssdIn[ 4 ] + "" + ssdIn[ 5 ]);
      return -1;
    }
    return ssdIn[ 2 ];
  }
  
  int writeTouch( byte[] buffer )
  {
    if ( RemoteMaster.admin )
    {
      System.err.println( "Write dialog starts:");
    }
    setProgressName( "PREPARING TO UPLOAD" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );
    int mask = 0xFFF;
    int total = Remote.userFilenames.length;
    for ( int index = 0; index < total; index++ )
    {
      if ( progressUpdater != null )
        progressUpdater.updateProgress( (int)((double)(index+1) / total * 100) );
      String name = Remote.userFilenames[ index ];
      if ( name.equalsIgnoreCase( "SysIcons.pkg" ) )
      {
        // don't delete, or send, SysIcons.pkg
        mask ^= ( 1 << index );
        continue;
      }
      System.err.println( "Deleting file " + name );
      Arrays.fill( ssdOut, ( byte )0 );
      ssdOut[ 0 ] = 0x15;
      ssdOut[ 2 ] = ( byte )name.length();
      for ( int i = 0; i < name.length(); i++ )
      {
        ssdOut[ 4 + i ] = ( byte )name.charAt( i );
      }
      writeTouchUSBReport( ssdOut, 62 );
      int check = -1;
      if ( readTouchUSBReport( ssdIn ) < 0 || ( check = ssdInCheck() ) < 0 
          || ( check & 0xEF ) != 0 )
      {
        // Only valid values for check are 0x00 and 0x10
        System.err.println( "Deletion failed.  Aborting upload" );
        return 0;
      }
      System.err.println( check == 0 ? "  File present and deleted" : "  File absent");
    }
    
    int status = ( ( buffer[ 0 ] & 0xFF ) | ( ( buffer[ 1 ] & 0x0F ) << 8 ) ) & mask;
    int dataEnd = ( buffer[ 2 ] & 0xFF ) | ( ( buffer[ 3 ] & 0xFF ) << 8 ) | ( ( buffer[ 1 ] & 0xF0 ) << 12 );
    int pos = 4;
    int index = -1;
    setProgressName( "UPLOADING" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );
    while ( pos < dataEnd )
    {
      while ( index < 12 && ( status & ( 1 << ++index ) ) == 0 ) {}
      if ( index == 12 )
      {
        break;
      }
      String name = Remote.userFilenames[ index ];
      System.err.println( "Sending file " + name );
      int count = 0;
      int end = name.endsWith( ".xcf" ) ? getEndBXML( pos, buffer ) : getEndPKG( pos, buffer );
      System.err.println( "File start: " + Integer.toHexString( pos ) + ", end: " + Integer.toHexString( end ) );
      int len = end - pos;
      Arrays.fill( ssdOut, ( byte )0 );
      ssdOut[ 0 ] = 0x13;
      ssdOut[ 2 ] = ( byte )( len & 0xFF );
      ssdOut[ 3 ] = ( byte )( ( len >> 8 ) & 0xFF );
      ssdOut[ 6 ] = ( byte )name.length();
      for ( int i = 0; i < name.length(); i++ )
      {
        ssdOut[ 7 + i ] = ( byte )name.charAt( i );
      }
      int written = writeTouchUSBReport( ssdOut, 62 );
      System.err.println( "File header packet sent" );
      int read = readTouchUSBReport(ssdIn, 5000);
      boolean success = written == 65 && read == 64 && ssdInCheck() == 0;
      if ( !success )
      {
        System.err.println( "Error: File header packet failed" );
        return 0;
      }
      while ( pos < end )
      {
        int size = Math.min( end - pos, 56 );
        Arrays.fill( ssdOut, ( byte )0 );
        ssdOut[ 0 ] = 0x14;
        ssdOut[ 2 ] = ( byte )( count & 0xFF );
        ssdOut[ 3 ] = ( byte )( ( count >> 8 ) & 0xFF );
        ssdOut[ 4 ] = ( byte )size;
        System.arraycopy( buffer, pos, ssdOut, 6, size );
        if ( !writeTouchBufferOut() )
        {
          System.err.println( "Error: terminating at hex position " + Integer.toHexString( pos ) );
          return pos;
        }
        pos += size;
        count++;
        if ( progressUpdater != null )
          progressUpdater.updateProgress( (int)((double)(pos+1) / dataEnd * 100) );

//        System.err.println( "Packet " + count + " sent" );
      }
    }
    if ( RemoteMaster.admin )
    {
      System.err.println( "Write dialog ends");
    }
    return buffer.length;
  }
  
  public boolean writeSystemFiles( File sysFile, List< String > names, int type )
  {
    byte[] data = null;
    String typeName = type == 1 ? "UPDATING CHANGED FILES:" :
        type == 2 ? "WRITING NEW FILES:" :
        type == 3 ? "REBUILDING FILE SYSTEM:" : "UNKNOWN:";
    if ( type > 0 && !names.isEmpty() )
    {
      setProgressName( typeName );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 0 );
    }
    try
    {
      ZipFile zipfile = new ZipFile( sysFile );
      int total = names.size();
      int count = 0;
      for ( String name : names )
      {
        String zName = upgradeData.get( name.toUpperCase() ).zName;
        ZipEntry entry = zipfile.getEntry( zName );
        int length = ( int )entry.getSize();
        if ( length < 0 )
        {
          System.err.println( "File " + name + " has unknown length and could not be updated" );
          upgradeSuccess = false;
          continue;
        }
        InputStream zip = zipfile.getInputStream( entry );
        data = RemoteMaster.readBinary( zip, length );
        Hex hex = new Hex( data );
        RemoteConfiguration.decryptObjcode( hex );
        data = hex.toByteArray(); 
        System.err.println( "Writing file " + name + " to remote" );
        if ( !writeSystemFile( name, data ) )
        {
          System.err.println( "Failed to write system file " + name );
          upgradeSuccess = false;
        }
        count++;
        if ( type > 0 && progressUpdater != null )
        {
          progressUpdater.updateProgress( (int)((double)count / total * 100 ) );
        }

        // FOR TESTING ONLY (with the above writeSystemFile() commented out:
//        OutputStream output = null;
//        File outputDir = new File( RemoteMaster.getWorkDir(), "XSight" );
//
//        try 
//        {
//          output = new FileOutputStream( new File( outputDir, name + "out"  ), false );
//          output.write( data );
//        }
//        catch(Exception ex){
//          System.err.println( "Unable to open file " + name );
//        }
        // END TESTING
      }
      if ( zipfile != null )
        zipfile.close();
    }
    catch ( Exception e )
    {
      System.err.println( "Error in writing system support files" );
      return false;
    }
    return true;
  }
  
  public boolean deleteSystemFiles( List< String > forDeletion )
  {
    boolean result = true;
    for ( String name : forDeletion )
    {
      System.err.println( "Deleting system file " + name );
      Arrays.fill( ssdOut, ( byte )0 );
      ssdOut[ 0 ] = 0x15;
      ssdOut[ 2 ] = ( byte )name.length();
      for ( int i = 0; i < name.length(); i++ )
      {
        ssdOut[ 4 + i ] = ( byte )name.charAt( i );
      }
      writeTouchUSBReport( ssdOut, 62 );
      int check = -1;
      if ( readTouchUSBReport( ssdIn ) < 0 || ( check = ssdInCheck() ) < 0 
          || ( check & 0xEF ) != 0 )
      {
        // Only valid values for check are 0x00 and 0x10
        System.err.println( "  Deletion of file " + name + " failed" );
        result = false;
      }
      System.err.println( "  File " + name + ( check == 0 ? " present and deleted" : " absent" ) );
    }
    return result;
  }
  
  public boolean writeSystemFile( String name, byte[] data )
  {
    if ( data == null )
    {
      System.err.println( "Write System File aborting.  No data available for file " + name );
      return false;
    }
    
    int len = data.length;
    int pos = 0;
    int count = 0;
    Arrays.fill( ssdOut, ( byte )0 );
    ssdOut[ 0 ] = 0x13;
    ssdOut[ 2 ] = ( byte )( len & 0xFF );
    ssdOut[ 3 ] = ( byte )( ( len >> 8 ) & 0xFF );
    ssdOut[ 4 ] = ( byte )( ( len >> 16 ) & 0xFF );
    ssdOut[ 6 ] = ( byte )name.length();
    for ( int i = 0; i < name.length(); i++ )
    {
      ssdOut[ 7 + i ] = ( byte )name.charAt( i );
    }
    int written = writeTouchUSBReport( ssdOut, 62 );
    System.err.println( "File header packet sent" );
    int read = readTouchUSBReport(ssdIn, 5000);
    boolean success = written == 65 && read == 64 && ssdInCheck() == 0;
    if ( !success )
    {
      System.err.println( "Error: File header packet failed" );
      return false;
    }
    while ( pos < len )
    {
      if ( ssdInCheck() < 0 )
      {
        System.err.println( "Error: Write of file " + name + " terminating at hex position " + Integer.toHexString( pos ) );
        return false;
      }
      int size = Math.min( len - pos, 56 );
      Arrays.fill( ssdOut, ( byte )0 );
      ssdOut[ 0 ] = 0x14;
      ssdOut[ 2 ] = ( byte )( count & 0xFF );
      ssdOut[ 3 ] = ( byte )( ( count >> 8 ) & 0xFF );
      ssdOut[ 4 ] = ( byte )size;
      System.arraycopy( data, pos, ssdOut, 6, size );
      if ( !writeTouchBufferOut() )
      {
        System.err.println( "Error: terminating at hex position " + Integer.toHexString( pos ) );
        return false;
      }
      pos += size;
      count++;
      System.err.println( "Packet " + count + " sent" );
    }
    System.err.println( "Bytes written to " + name + ": " + pos );
    return true;
  }
  
  private boolean writeFirmwareFile( byte[] data )
  {
    if ( data == null )
    {
      System.err.println( "Write Firmware File aborting.  No data available." );
      return false;
    }
    RemoteMaster.forceUpgradeItem.setSelected( false );
    int len = data.length - 8;  // first 8 bytes are not sent
    int pos = 0;
    int count = 0;
    Arrays.fill( ssdOut, ( byte )0 );
    ssdOut[ 0 ] = 0x21;
    ssdOut[ 2 ] = ( byte )( len & 0xFF );
    ssdOut[ 3 ] = ( byte )( ( len >> 8 ) & 0xFF );
    ssdOut[ 4 ] = ( byte )( ( len >> 16 ) & 0xFF );
    ssdOut[ 5 ] = ( byte )( ( len >> 24 ) & 0xFF );
    int n = 0;
    boolean success = false;
    while ( n < 2 )
    {
      setProgressName( "WRITING NEW FIRMWARE:" );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 0 );
      long waitStart = Calendar.getInstance().getTimeInMillis();
      for ( int i = 0 ; i < 4; i++ )
      {
        int written = writeTouchUSBReport( ssdOut, 62 );
        System.err.println( "Firmware header packet " + ( i+1 ) + " sent" );
        int read = readTouchUSBReport(ssdIn, 5000);
        long delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
        System.err.println( "Response to header " + ( i+1 ) + " received after wait of " + delay + "ms" );

        if ( written == 65 && read == 64 && ssdIn[ 2 ] == 0 )
        {
          success = true;
          break;
        }
      }
      if ( success )
      {
        break;
      }
      n++;
      if ( n < 2 )
      {
        String title = "Writing updated firmware";
        String message = "Please disconnect the USB cable from the remote,\n"
            + "connect it again and then press OK to continue";
        JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
        waitForTouchReconnection();
      }
    }

    if ( !success )
    {
      System.err.println( "Error: Firmware header packet failed" );
      return false;
    }
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 20 );
    while ( pos < len )
    {
      int size = Math.min( len - pos, 56 );
      Arrays.fill( ssdOut, ( byte )0 );
      ssdOut[ 0 ] = 0x22;
      ssdOut[ 2 ] = ( byte )( count & 0xFF );
      ssdOut[ 3 ] = ( byte )( ( count >> 8 ) & 0xFF );
      ssdOut[ 5 ] = ( byte )size;
      System.arraycopy( data, pos + 8, ssdOut, 6, size );
      if ( !writeTouchBufferOut() )
      {
        System.err.println( "Error: Write of firmware terminating at hex position " + Integer.toHexString( pos ) );
        return false;
      }
      pos += size;
      count++;
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 20 + (int)((double)pos / len * 80 ));
    }
    System.err.println( "Bytes written to firmware: " + pos );
    return true;
  }
  
  boolean waitForMillis( int waitTime )
  {
    System.err.println( "Waiting for " + waitTime + "ms" );
    long waitStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    while ( delay < waitTime )
    { 
      delay = Calendar.getInstance().getTimeInMillis() - waitStart;
    }
    return true;
  }
  
  boolean waitForTouchReconnection()
  {
    powerStatus = -1;
    setProgressName( "WAITING FOR RECONNECTION:" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );
    long delay = 0;
    try
    {
      if ( devHID != null )
        devHID.close();
    }
    catch ( Exception e )
    {
      System.err.println( "Error in closing device after disconnection" );
      return false;
    }
    
    devHID = null;
    waitForMillis( 2000 );
    int n = 0;
    long timeOut = 60000;
    while ( ( n & 1 ) == 0 )
    {
      long waitStart = Calendar.getInstance().getTimeInMillis();
      while ( devHID == null )
      {
        delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
        if ( delay > timeOut )
        {
          if ( n < 2 )
          {
            String title = "Reconnection";
            String message = "Please disconnect the USB cable from the remote,\n"
                + "connect it again and then press OK to continue";
            JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
            n += 2;
            break;
          }
          else
          {
            System.err.println( "Reconnection maximum wait exceeded" );
            return false;
          }
        }
        if ( progressUpdater != null )
          progressUpdater.updateProgress( (int)((double)delay / timeOut * 100) );
        try
        {
          HIDinfo = hid_mgr.listDevices();
          if ( HIDinfo != null )
          {
            for ( int i = 0; i<HIDinfo.length; i++ )
            {
              if ( HIDinfo[i].getVendor_id() == 0x06E7 ) 
              {
                HIDdevice = HIDinfo[ i ];
                devHID = HIDdevice.open();
                //              devHID = hid_mgr.openById(0x06E7, thisPID, null);
                if ( devHID != null )
                {
                  devHID.enableBlocking();
                }
                n++;
                break;
              }
            }
          }
        }
        catch ( Exception e )
        {
          System.err.println( "Error in reopen attempt" );
          return false;
        }
        waitForMillis( 3000 );
      } // while ( devHID == null )
    }

    if ( devHID != null )
    {
      System.err.println( "Reopened device after wait of " + delay + "ms" );
      if ( RemoteMaster.ioNeedsPowerManagementCheck( this )
          && ( powerStatus = getEnhancedPowerManagementStatus() ) == 1 )
      {
        String title = "Firmware upgrade";
        String message = "The remote has opened in a new mode in which Enhanced Power Management\n"
            + "is still enabled.  Please use regedit to disable it. The key that needs\n"
            + "to be changed is at:\n\n" + getRegistryKey() + "\n\n"
            + "where EnhancedPowerManagementEnabled needs to be changed from 1 to 0.\n"
            + "Right-click the entry and select Modify, enter the new value 0 and press\n"
            + "OK.  After making this change, you need to disconnect and reconnect the\n"
            + "remote and run the update process again.\n\n"
            + "If the remote is still in Update Mode then remove and reinsert the\n"
            + "batteries to exit Update Mode.  The remote will then be as it was before\n"
            + "you started the upgrade procedure.  You may repeat the upgrade process now\n"
            + "or at a later time.\n\n"
            + "If the remote has exited Update Mode then the upgrade process has started\n"
            + "but is incomplete.  You should not need to remove and reinsert the batteries\n"
            + "but you do need to repeat the upgrade process to allow it to complete.";
        JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
        return false;
      }
      if ( powerStatus == -1 )
      {
        System.err.println( "Enhanced Power Management is not supported in this mode" );
      }
      else if ( powerStatus == 0 )
      {
        System.err.println( "Enhanced Power Management is disabled in this mode" );
      }
    }
    else
    {
      System.err.println( "Reopen attempt gave null device after wait of " + delay + "ms" );
      return false;
    }
    return true;
  }

  public String getRegistryKey()
  {
    return "HKEY_LOCAL_MACHINE\n"
        + "  SYSTEM\n"
        + "    CurrentControlSet\n"
        + "      Enum\n"
        + "        USB\n"
        + "          VID_06E7&PID_" + String.format( "%04X\n", thisPID )
        + "            " + deviceID + "\n"
        + "              Device Parameters";
  }

//    boolean wasAbsent = false;
//    while ( true )
//    {
//      long delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
//      if ( delay > 60000 )
//      {
//        System.err.println( "Reconnection maximum wait exceeded" );
//        return false;
//      }
//      boolean present = false;
//      try
//      {
//        HIDinfo = hid_mgr.listDevices();
//        HIDdevice = null;
//        int count = HIDinfo == null ? 0 : HIDinfo.length;
//        if ( count != lastCount )
//        {
//          System.err.println( "Number of devices = " + count + " after delay of " + delay + "ms" );
//          lastCount = count;
////          if ( HIDinfo != null )
////          {
////            System.err.println( "Devices are:" );
////            for (int i = 0; i<HIDinfo.length; i++)
////            {
////              System.err.println( "  Device " + i + ": " + HIDinfo[i] );
////            }
////          }
//        }
//        if ( HIDinfo != null )
//        {
//          for ( int i = 0; i<HIDinfo.length; i++ )
//          {
//            if ( HIDinfo[i] != null && HIDinfo[i].getVendor_id() == 0x06E7 ) 
//            {
//              HIDdevice = HIDinfo[i];
//              System.err.println( "Remote is connected" );
//              present = true;
//              if ( devHID != null && !wasAbsent )
//              {
//                System.err.println( "Closing connection" );              
//                try
//                {
//                  devHID.close();
//                }
//                catch ( IOException e )
//                {
//                  System.err.println( "IOException on attempting close" );
//                }
//                devHID = null;
//              }
//              break;
//            }
//          }
//        }
//        if ( !wasAbsent && !present )
//        {
//          System.err.println( "Disconnected after wait of " + delay + "ms" );
//          wasAbsent = true;
//        }
//
//        if ( present && wasAbsent )
//        {
//          System.err.println( "Reconnected after wait of " + delay + "ms" );
////          devHID = hid_mgr.openById(0x06E7, thisPID, null);
//          devHID = HIDdevice.open();
//          if ( devHID != null )
//          {
//            System.err.println( "Connection opened" );
//            devHID.enableBlocking();
//            return true;
//          }
//          else
//          {
//            System.err.println( "Connection failed to open" );
//            return false;
//          }
//        }
//        waitForMillis( 500 );
//      }
//      catch ( Exception e )
//      {
//        e.printStackTrace();
//        return false;        
//      }
//    }
//  }
  
  /**
   *  return array elements are:
   *    int[0] = 1 if needs upgrade;
   *    int[1] = 1 if needs MCU upgrade;
   *    int[2] = upgrade type, 0 = no change, 1 = upgrade, -1 = downgrade, -2 = up/down inconsistent
   */
  int[] testForUpgrade( List< String > changed, List< String > newFiles )
  {
    changed.clear();
    newFiles.clear();   
    if ( forceUpgrade )
    {
      changed.addAll( Arrays.asList( "lang.en", "lang.fr", "lang.ge", "lang.it",
          "lang.sp", "lang.no", "lang.se", "Splash.xmg" ) );
      newFiles.addAll( Arrays.asList("lang.dk", "lang.fi", "lang.nl" ) );
      return new int[]{1,1,1};
    }

    int[] out = new int[]{ 0, 0, 0 };
    int upgradeType = 0;
    List< String > fwNames = new ArrayList< String >();
    for ( String name : firmwareFileVersions.keySet() )
    {
      fwNames.add( name.toUpperCase() );
      if ( name.equalsIgnoreCase( "BlasterFirmware" ) )
      {
        // We are currently unable to update BlasterFirmware
        continue;
      }
      Hex currentVersion = firmwareFileVersions.get( name );
      String fdName = name;
      int currentOffset = 2;
      int[] testOrder = new int[]{ 0,1,2,3,4,5 };
      if ( remoteType == RemoteType.XZITE )
      {
        fdName = name.toUpperCase();
        testOrder = new int[]{ 3,2,1,0 };
      }
      else
      {
        currentOffset = 0;
        if ( name.equals( "ALF" ) && upgradeData.get( "alf.img" ) != null )
        {
          // The alf.img file, when present, is a later version than that included in app.img
          name = fdName = "alf.img";
        }
      }
      FileData fd = upgradeData.get( fdName );
      if ( fd == null )
      {
        // This case should never happen
        System.err.println( "File " + name + " missing from upgrade data and so will\n" 
            + "be retained unchanged" );
        continue;
      }
      
      Hex upgradeVersion = fd.version;
      int diff = 0;
      for ( int i : testOrder )
      {
        diff = upgradeVersion.getData()[ i ] - currentVersion.getData()[ i + currentOffset ];
        if ( diff != 0 )
        {
          out[ 0 ] = 1;
          if ( name.equalsIgnoreCase( "MCUFirmware" ) 
              || remoteType != RemoteType.XZITE && !name.equals( "alf.img" ) )
          {
            out[ 1 ] = 1;
          }
          else
          {
            changed.add( name );
          }
          break;
        }
      }

      if ( diff != 0 )
      {
        upgradeType = diff > 0 && upgradeType >= 0 ? 1
            : diff > 0 && upgradeType < 0 ? -2
                : diff < 0 && upgradeType > 0 ? -2
                    : diff < 0 && upgradeType > -2 ? -1 : -2;
      }
    }
    if ( remoteType != RemoteType.XZITE )
    {
      for ( String name : sysNames )
      {
        // newFiles should contain the system files with a non-empty extension
        // that are not currently in the remote but are in the upgrade file set
        // with a nonzero version ( ConnectUSB.xmg has a zero version and should
        // not be included )
        FileData fd = null;
        if ( !fwNames.contains( name.toUpperCase() )
            && name.indexOf( '.' ) > 0
            && ( fd = upgradeData.get( name.toUpperCase() ) ) != null
            && fd.versionNum > 0 )
        {
          newFiles.add( name );
          out[ 0 ] = 1;
          upgradeType = upgradeType >= 0 ? 1 : -2;
        }
      }
    }
    out[ 2 ] = upgradeType;
    return out;
  }
  
  int readTouch( byte[] buffer )
  {
    int status = 0;
    setProgressName( getUse() == Use.DOWNLOAD ? "DOWNLOADING:" : "VERIFYING UPLOAD:" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );
    // Continue by getting user file lengths.
    System.err.println();
    System.err.println( "User file length data:" );
    for ( String name : Remote.userFilenames )
    {
      Arrays.fill( ssdOut, ( byte )0 );
      ssdOut[ 0 ] = 0x19;
      ssdOut[ 2 ] = ( byte )name.length();
      for ( int i = 0; i < name.length(); i++ )
      {
        ssdOut[ 4 + i ] = ( byte )name.charAt( i );
      }
      writeTouchUSBReport( ssdOut, 62 );
      if ( readTouchUSBReport( ssdIn ) < 0 )
      {
        System.err.println( "Length of file " + name + " is unavailable" );
        return 0;
      }
      Hex hex = new Hex( 8 );
      for ( int i = 0; i < 8; i++ )
      {
        hex.set( ( short )ssdIn[ i ], i );
      }
      System.err.println( "  " + name + ( ( ssdIn[ 2 ] & 0x10 ) == 0x10 ? " (absent):" : ":" ) + hex );
    }
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 10 );
    int ndx = 4;
    runningTotal = ndx;
    if ( RemoteMaster.admin )
    {
      System.err.println( "Read user files:");
    }
    int total = Remote.userFilenames.length;
    for ( int n = 0; n < total; n++ )
    {
      String name = Remote.userFilenames[ n ];
      byte[] data = readTouchFileBytes( name, false );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 10 + (int)((double)(n+1)/total * 90));
      if ( data == null )
      {
        return ndx - 4;
      }
      int len = data.length;
      if ( len == 0 )
      {
        continue;
      }
      else if ( ndx + len < buffer.length )
      {
        status |= 1 << n;
        System.arraycopy( data, 0, buffer, ndx, len );
        ndx += len;
      }
      else
      {
        System.err.println( "RDF EEPROM size not large enough to hold the data of this remote" );
        return ndx - 4;
      }
    }
    Arrays.fill( buffer, ndx, buffer.length, ( byte )0xFF );
    buffer[ 0 ] = ( byte )( status & 0xFF );
    buffer[ 1 ] = ( byte )( ( ( status >> 8 ) & 0x0F ) | ( ( ndx >> 12 ) & 0xF0 ) );
    buffer[ 2 ] = ( byte )( ndx & 0xFF );
    buffer[ 3 ] = ( byte )( ( ndx >> 8 ) & 0xFF );
    
    if ( RemoteMaster.getSystemFiles() )
    {
      readSystemFiles();
    }
    if ( RemoteMaster.admin )
    {
      System.err.println( "Read dialog ends");
    }
    
    // Need to return the buffer length rather than bytesRead for
    // consistency with normal remotes, which do read the entire buffer
    return buffer.length;
  }
  
  public short[] readTouchFile( String name )
  {
    runningTotal = 0;
    byte[] bBuffer = readTouchFileBytes( name, false );
    if ( bBuffer == null )
    {
      return null;
    }
    short[] sBuffer = new short[ bBuffer.length ];
    for ( int i = 0; i < bBuffer.length; i++ )
    {
      sBuffer[ i ] = ( short )( bBuffer[ i ] & 0xFF );
    }
    return sBuffer;
  }
  
  public byte[] readTouchFileBytes( String name, boolean sizeOnly )
  {
    /* There seems to be an issue with reading the system file AC_conn.  I don't
    know what this file is, but if it is related to the USB connection then it
    may fail through being used while also being read.  Two attempts, out of 
    around 10, failed after reading the packet with sequence number $008C. To
    work round this, there seems no harm in having three attempts at reading
    any file, though this may be the only one needing more than one attempt.
    Edit:  Further testing has given errors during verification on other files
    as well.
     */
    int attempt = 0;
    byte[] result = null;
    while ( result == null && attempt++ < 4 )
    {
      result = readTouchFileBytesOnce( name, sizeOnly );
    }
    if ( attempt > 1 && result != null )
    {
      System.err.println( "File " + name + " took " + attempt + " attempts to read" );
    }
    return result;
  }
  
  private byte[] readTouchFileBytesOnce( String name, boolean sizeOnly )
  {
    Arrays.fill( ssdOut, ( byte )0 );
    ssdOut[ 0 ] = ( byte )( sizeOnly ? 0x19 : 0x12 );
    ssdOut[ 2 ] = ( byte )name.length();
    int offset = sizeOnly ? 4 : 3;
    for ( int i = 0; i < name.length(); i++ )
    {
      ssdOut[ offset + i ] = ( byte )name.charAt( i );
    }
    // Initialize file read
    writeTouchUSBReport( ssdOut, 62 );
    // First type 01 packet returned gives file length or sets absence flag.
    if ( readTouchUSBReport( ssdIn ) < 0 )
    {
      System.err.println( "Unable to read file \"" + name + "\"" );
      return null;
    }
    if ( ( ssdIn[ 2 ] & 0x10 ) == 0x10 )
    {
      System.err.println( "File " + name + " is absent" );
      return new byte[ 0 ];
    }
    if ( sizeOnly )
    {
      byte[] sizeData = new byte[ 4 ];
      System.arraycopy( ssdIn, 3, sizeData, 0, 4 );
      return sizeData;
    }
    
    // Get file length as integer from 3-byte little-endian data
    int count = ( ssdIn[ 3 ] & 0xFF ) + 0x100 * ( ssdIn[ 4 ] & 0xFF )+ 0x10000 * ( ssdIn[ 5 ] & 0xFF );
    int total = 0;
    ssdOut[ 0 ] = 1;
    ssdOut[ 2 ] = 0;
    int ndx = 0;
    byte[] buffer = new byte[ count ];
    int sequenceNumber = 0;
    int packetID = 0;
    while ( total < count )
    {
      // Read next segment of file data
      if ( readTouchUSBReport( ssdIn ) < 0 )
      {
        System.err.println( "Read error before end of file \"" + name + "\"" );
        return null;
      }
//      // *** TESTING
//      // This was a failed attempt to emulate getting a repeat of the previous
//      // packet by reporting an incorrect sequence number (Status valus 0x11)    
//      int n = 0;
//      if ( n == 0 && name.equals( "lang.fr" ) && sequenceNumber == 10 )
//      {
//        // pretend packet not received
//        ssdOut[ 1 ] = ( byte )(packetID );
//        ssdOut[ 2 ] = 0x11;
//        ssdOut[ 3 ] = (byte)( ( sequenceNumber ) & 0xFF );
//        ssdOut[ 4 ] = (byte)( ( sequenceNumber >> 8 ) & 0xFF );
//        waitForMillis( 250 );
//        writeTouchUSBReport( ssdOut, 5 );
//        n++;
//        ssdOut[ 2 ] = 0;
//        for ( int i = 0; i < name.length(); i++ )
//        {
//          ssdOut[ 3 + i ] = ( byte )name.charAt( i );
//        }
//        continue;
//      }
//      // *** END TESTING
      
      packetID = ssdIn[ 1 ];
      sequenceNumber = ( ssdIn[ 2 ] & 0xFF ) | ( ( ssdIn[ 3 ] & 0xFF ) << 8 ); 
      int len = ssdIn[ 4 ];
      total += len;
      System.arraycopy( ssdIn, 6, buffer, ndx, len );
      ndx += len;
      // Set packet serial in acknowledgement.
      ssdOut[ 1 ] = ( byte )packetID;
      // Send acknowledgement packet.
      writeTouchUSBReport( ssdOut, 62 );
    }
    if ( runningTotal >= 0 )
    {
      System.err.println( "File " + name + " has reported length " + count + ", actual length " + total );
      System.err.println( "  Start = " + Integer.toHexString( runningTotal ) + ", end = " + Integer.toHexString( runningTotal + total - 1 ) );
      runningTotal += total;
    }
    return buffer;
  }
  
  public byte[] readSystemFile( String name )
  {
    runningTotal = -1;
    return readTouchFileBytes( name, false );
  }
  
  private boolean readSystemFiles()
  {
    setProgressName( "SAVING SYSTEM FILES:" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 0 );
    System.err.println();
    System.err.println( "Saving system files to XSight subfolder of installation folder:" );
    OutputStream output = null;
    ZipOutputStream zip = null;
    File outputDir = new File( RemoteMaster.getWorkDir(), "XSight" );
    outputDir.mkdirs();
    boolean result = true;
    String zipName = RemoteMaster.getSystemZipName( remote );
    if ( zipName == null )
    {
      return false;
    }
    
    try 
    {
      output = new FileOutputStream( new File( outputDir, zipName  ), false );
      zip = new ZipOutputStream( output );
      int total = firmwareFileVersions.size();
      int count = 0;
      for ( String name : firmwareFileVersions.keySet() )
      {
        if ( name.indexOf( "." ) > 0 )
        {
          byte[] filedata = readSystemFile( name );
          if ( filedata == null || filedata.length == 0 )
          {
            continue;
          }
          System.err.println( "  Saving " + name );
          zip.putNextEntry( new ZipEntry( name ) );
          zip.write( filedata );
        }
        count++;
        if ( progressUpdater != null )
          progressUpdater.updateProgress( (int)((double)count / total * 100 ) );
      }
    }
    catch ( Exception e )
    {
      result = false;
      System.err.println( e );
    }
    finally 
    {
      try { zip.close(); }
      catch ( IOException e ) {};
    }
    String message = result ? "Firmware saved to " + zipName + " in XSight subfolder of \n"
        + "the RMIR installation folder" : "Firmware saving failed.  Download aborting.";
    String title = "Firmware operation";
    JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
    return result;
  }
  
  public int getEnhancedPowerManagementStatus()
  {
    if ( HIDdevice == null )
    {
      return -1;
    }
    
    int enabled = -1;
    String containerGUID = null;
    String deviceKey = null;
    try
    {
      String path = HIDdevice.getPath();
      int pos1 = path.indexOf( "hid" );
      int pos2 = path.lastIndexOf( '#' );
      if ( pos1 >= 0 && pos2 >= 0 )
      {
        path = path.substring( pos1, pos2 );
        pos2 = path.lastIndexOf( '#' );
        path = path.substring( 0, pos2 ).toUpperCase() + path.substring( pos2 );
        path = path.replace( "#", "\\" );
        String key = "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Enum\\" + path;
        ProcessBuilder builder = new ProcessBuilder( "reg", "query", key );         
        Process reg = builder.start();
        BufferedReader output = new BufferedReader( new InputStreamReader( reg.getInputStream() ) );
        reg.waitFor();
        String line = null;
        while ( ( line = output.readLine() ) != null )
        {  
          if ( line.contains( "ContainerID" ) )
          {
            pos1 = line.indexOf( '{' );
            pos2 = line.indexOf( '}' );
            if ( pos1 > 0 && pos2 > 0 )
            {
              containerGUID = line.substring( pos1, pos2 + 1 );
            }
            break;
          }
        }
      }
      if ( containerGUID != null )
      {
        String key = "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\DeviceContainers\\"
            + containerGUID + "\\BaseContainers\\" + containerGUID;
        ProcessBuilder builder = new ProcessBuilder( "reg", "query", key );         
        Process reg = builder.start();
        BufferedReader output = new BufferedReader( new InputStreamReader( reg.getInputStream() ) );
        reg.waitFor();
        String hidPid = String.format( "%04X", thisPID );
        String line = null;
        while ( ( line = output.readLine() ) != null )
        {  
          if ( ( pos1 = line.indexOf( "USB\\VID_06E7&PID_" + hidPid ) ) >= 0 )
          {
            line = line.substring( pos1 );
            pos2 = line.indexOf( " " );
            if ( pos2 > 0 )
            {
              line = line.substring( 0, pos2 );
            }
            deviceKey = line;
            break;
          }
        }
      }
      if ( deviceKey != null )
      {
        deviceID = deviceKey.substring( deviceKey.lastIndexOf( '\\' ) + 1 );
        String key = "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Enum\\" + deviceKey + "\\Device Parameters";
        ProcessBuilder builder = new ProcessBuilder( "reg", "query", key );         
        Process reg = builder.start();
        BufferedReader output = new BufferedReader( new InputStreamReader( reg.getInputStream() ) );
        reg.waitFor();
        String line = null;
        while ( ( line = output.readLine() ) != null )
        {  
          if ( line.contains( "EnhancedPowerManagementEnabled" ) )
          {
            int pos = line.indexOf( "0x" );
            if ( pos >= 0 )
            {
              line = line.substring( pos + 2 );
              pos = line.indexOf( " " );
              if ( pos > 0 )
              {
                line = line.substring( 0, pos );
              }
              enabled = Integer.parseInt( line, 16 );
              System.err.println( "Enhanced Power Management is " + ( enabled > 0 ? "enabled" : "disabled" ) );
            }
            break;
          }
        }
      }
    }
    catch ( Exception e )
    {
      System.err.println( "Error in accessing Enhanced Power Management Status" );
      return -1;
    }
    return enabled;
  }
  
  boolean getVersionsFromRemote( boolean getSerial )
  {
    if ( remoteType == RemoteType.DIGITAL )
    {
      for ( int index = 0; index < 7; index++ )
      {
        if ( index == 2 )
        {
          continue;  // skip E2 area
        }
        Hex hex = getVersionFromFDRAremote( index );
        if ( hex == null )
        {
          return false;
        }
        firmwareFileVersions.put( blockNames[ index ], hex );
      }
      return true;
    }
    if ( remoteType == RemoteType.AVL )
    {
      return false;  // Not yet implemented
    }
    // Continue for remoteType == RemoteType.XZITE
    byte[] o = new byte[2];
    o[0]=1;
    // Initiate read of firmware file versions.
    writeTouchUSBReport( new byte[]{4}, 1 );
    if ( readTouchUSBReport( ssdIn ) < 0 )
    {
      System.err.println( "Read versions from remote failed to initiate" );
      return false;
    }
    // Get number of firmware version packets to follow.
    firmwareFileCount = ssdIn[ 3 ];
    System.err.println( "Firmware file version data:" );
    for ( int i = 0; i < firmwareFileCount; i++ )
    {
      // Read type 05 firmware version packet.
      if ( readTouchUSBReport( ssdIn ) < 0 )
      {
        System.err.println( "Read versions from remote failed on file " + ( i+1 ) + " of " + firmwareFileCount );
        return false;
      }
      saveVersionData();
      // Get packet serial and set it in acknowledgement packet.
      o[1] = ssdIn[ 1 ];
      // Send acknowledgement.
      writeTouchUSBReport( o, 2 );
    }
    if ( !getSerial )
    {
      return true;
    }
    writeTouchUSBReport( new byte[]{0x27}, 1 );
    if ( readTouchUSBReport( ssdIn ) < 0 )
    {
      System.err.println( "Reading of serial number failed" );
      return false;
    }
    Hex serial = new Hex( 6 );
    for ( int i = 0; i < serial.length(); i++ )
    {
      serial.set( ( short)( ~ssdIn[ i + 3 ] & 0xFF ), i );
    }
    System.err.println( "Hex serial number: " + serial );
    return true;
  }
  
  void saveVersionData()
  {
    boolean absent = true;
    Hex hex = new Hex( 12 );
    // The four bytes that comprise the version number are all 00 if the
    // system file is missing.
    for ( int i = 0; i < 12; i++ )
    {
      short b = ( short )( ssdIn[ i ] & 0xFF );
      hex.set( b, i );
      if ( i > 1 && i < 6 && b > 0 )
      {
        absent = false;
      }
    }
    StringBuilder sb = new StringBuilder();
    for ( int i = 12; i < ssdIn.length && ssdIn[ i ] != 0 ; i++ )
    {
      sb.append( (char)ssdIn[ i ] );
    }
    String name = sb.toString();
    firmwareFileVersions.put( name, hex );
    System.err.println( "  " + name + " : " + hex.toString() + ( absent ? " (absent)" : "") );
  }
  
  @Override
  public int writeRemote( int address, byte[] buffer, int length ) {  //if Touch, must be 62 bytes or less
    int bytesWritten = -1;
    if ( interfaceType == 6 || interfaceType == 0x106 )
      bytesWritten = writeFDRA(address, buffer, length, true);
    else if ( interfaceType == 0x201 )
      bytesWritten = writeTouch( buffer );
    return bytesWritten;
  }
  
  int readTouchUSBReport(byte[] buffer)
  {
    return readTouchUSBReport( buffer, 3000 );
  }
  
  int readTouchUSBReport(byte[] buffer, int timeout ) 
  { 
    int bytesRead = -1;
    try {
      Arrays.fill( inReport, ( byte )0xFF );
      bytesRead = devHID.readTimeout(inReport, timeout);
      if ( inReport[ 0 ] == ( byte )0xFF )
      {
        return -2;  // signifies timed out as 0xFF is not a known packet type
      }
      if ( bytesRead == 64 && verifyCRC( inReport, 0 ) < 0 )
      {
        return -3;  // signifies CRC error
      }
      System.arraycopy(inReport,0, buffer, 0, 62);
      if ( RemoteMaster.admin )
      {
        System.err.println( "   " + bytesToString( buffer ) );
      }
    } catch (Exception e) {
      return -1;    // signifies error
    }
    return bytesRead;
    
//    // TESTING
//    // Uncomment this section and comment out the above to test by writing to
//    // rmaster.err but not to the remote    
//    Arrays.fill( buffer, ( byte )0 );
//    buffer[ 0 ] = 1;  // Set as response success packet
//    return 64;
  }
  
  int writeTouchUSBReport( byte[] buffer, int length ) {  //buffer must be <=62 bytes
    if ( RemoteMaster.admin )
    {
      System.err.println( bytesToString( buffer ) );
    }
    System.arraycopy(buffer,0, outReport, 1, length);  //outReport[0] is index byte
    if (length <= 62) 
      Arrays.fill(outReport, length + 1, 63, (byte)0);  
    else
      return -1;
    int crc =  CalcCRCofReport(outReport);
    int bytesWritten = -1;
    outReport[0] = (byte)0;
    outReport[63] = (byte) (crc & 0xFF);
    outReport[64] = (byte) (crc >> 8);
    try {
      bytesWritten = devHID.write(outReport);
    } catch (Exception e) {
      return -1;
    }
    return bytesWritten;
//  // TESTING
//  // Uncomment this section and comment out the above (except for the initial "if" clause 
//  // to test by writing to rmaster.err but not to the remote
//    return 65;
  }
  
  boolean writeTouchBufferOut()
  {
    int n = 0;
    boolean success = false;
    do {
      int written = writeTouchUSBReport( ssdOut, 62 );
      success = written == 65;
      int read = readTouchUSBReport(ssdIn);
      if ( read != 64 )
      {
        success = false;
      }
      if ( success && ssdIn[ 2 ] == 35 )
      {
        // I don't know the meaning of status value 35.  This is taken from _send_file(..)
        // in UEIC.Communication.Remote.Xsight\UEIC.Communication.Remote.Xsight\xxCom.cs
        waitForMillis( 10 );
        success = false;
      }
      else
      {
        n++;
      };
    }  while ( !success && n < 5 );
    
    if ( ssdIn[ 2 ] != 0 )
    {
      success = false;
    }
    return success;
  }
  
  String bytesToString( byte[] buffer )
  {
    int count = 0;
    StringBuilder sb = new StringBuilder();
    for ( byte b : buffer )
    {
      sb.append( String.format( count++ > 0 ? " %02X" : "%02X", b & 0xFF ) );
    }
    return sb.toString();
  }
  
  int CalcCRC(byte[] inBuf, int start, int end) {
      int poly = 0x8408; //0x1021 reversed
      int crc, i, j, byteVal;
          crc = 0xFFFF;
          for (i = start; i <= end; i++) {  // skip index byte
            byteVal = inBuf[i] & 0xFF; //bytes are always signed in Java;
              crc = crc ^ byteVal;
              for (j = 0; j < 8; j++) {
                  if ((crc & 1) == 1) 
                      crc = (crc >> 1) ^ poly;
                  else
                      crc = crc >> 1;
              }
          }
          return crc;
  }
 
  int CalcCRCofReport(byte[] inBuf) {
    return CalcCRC(inBuf, 1, 62);
  }
  
    /**
     * Instantiates a new CommHID.
     * 
     * @throws UnsatisfiedLinkError
     *           the unsatisfied link error
     */
    public CommHID() throws UnsatisfiedLinkError  {
      super( libraryName );  
    }

    /**
     * Instantiates a new CommHID.
     * 
     * @param folder
     *          the folder
     * @throws UnsatisfiedLinkError
     *           the unsatisfied link error
     */
    public CommHID( File folder ) throws UnsatisfiedLinkError  {
      super( folder, libraryName ); 
    }
    
    private String convertName( String name, int offset, boolean to )
    {
      int pos = name.lastIndexOf( '.' );
      name = name.substring( 0, pos );
      int len = name.length();
      char[] ca = new char[ len ];
      for ( int i = 0; i < len; i++ )
      {
        char c = name.charAt( i );
        int ndx = alphas.indexOf( c );
        int mod = alphas.length() - 1;
        int[] key = RemoteConfiguration.encryptionKey;
        int diff = key[ ( i + offset ) % key.length ];
        ndx = c == '.' || i == name.length() - 1 ? ndx : to ? ( ndx + diff ) % mod : ( ndx + 10*mod - diff ) % mod;
        ca[ i ] = alphas.charAt( ndx );
      }
      String str = String.valueOf( ca );
      return str + ".bin";
    }
    
@SuppressWarnings("unused")
    private void convertZipFile( String inName, String outName, boolean to )
    {
      byte[] data = null;
      File zipOut = new File( RemoteMaster.getWorkDir(), outName );
      try
      {
        ZipFile zipIn = new ZipFile( new File( RemoteMaster.getWorkDir(), inName ) ); 
        ZipOutputStream streamOut = new ZipOutputStream(new FileOutputStream( zipOut ));
        Enumeration< ? extends ZipEntry > zipEnum = zipIn.entries();
        int i = 0;
        while ( zipEnum.hasMoreElements() ) 
        { 
          ZipEntry entryIn = ( ZipEntry ) zipEnum.nextElement();
          String nameIn = entryIn.getName();
          String nameOut = convertName( nameIn, i, to );
          System.err.println( nameOut );
          ZipEntry entryOut = new ZipEntry( nameOut );
          streamOut.putNextEntry( entryOut );
          long length = entryIn.getSize();
          if ( length < 0 )
          {
            System.err.println( "File " + nameIn + " has unknown length and could not be converted" );
            continue;
          }
          InputStream zip = zipIn.getInputStream( entryIn );
          data = RemoteMaster.readBinary( zip, ( int )length );
          Hex hex = new Hex( data );
          RemoteConfiguration.decryptObjcode( hex );
          streamOut.write( hex.toByteArray() );
          streamOut.closeEntry();
          i++;
        }
        streamOut.close();
        zipIn.close();
      }
      catch( Exception e )
      {
        System.err.println( "Zip conversion failed" );
      }
      setFileData( zipOut );
      verifyFileVersions( zipOut );
    }
    
    public boolean setFileData( File sysFile )
    {
      List< String > ucSysNames = new ArrayList< String >();
      for ( String name : sysNames )
      {
        ucSysNames.add( name.toUpperCase() );
      }
      try
      {
        ZipFile zipIn = new ZipFile( sysFile );
        Enumeration< ? extends ZipEntry > zipEnum = zipIn.entries();
        String prefix = getPrefix( getRemoteSignature() );
        int i = 0;
//        System.err.println( "Reconverted names:");
        while ( zipEnum.hasMoreElements() ) 
        { 
          ZipEntry entryIn = ( ZipEntry ) zipEnum.nextElement();
          String nameIn = entryIn.getName();
          String nameOut = convertName( nameIn, i, false );
          i++;
          if ( !nameOut.startsWith( prefix ) )
          {
            continue;
          }
          nameOut = nameOut.substring( prefix.length() + 1 );
          int pos = nameOut.lastIndexOf( '_' );
          String versionStr = nameOut.substring( pos + 1, nameOut.length() - 4 );
          nameOut = nameOut.substring( 0, pos );
          if ( !ucSysNames.contains( nameOut.toUpperCase() ) )
          {
            System.err.println( "File " + nameOut + " in upgrade but not in sysNames" );
          }
          FileData fileData = new FileData();
          fileData.zName = nameIn;
          long versionNum = Long.parseLong( versionStr );
          fileData.versionNum = versionNum;
          if ( remoteType == RemoteType.XZITE )
          {
            short[] versionShort = new short[ 4 ];
            for ( int k = 0; k < 4; k++ )
            {
              versionShort[ k ] = ( short )( versionNum & 0xFF );
              versionNum >>= 8;
            }
            fileData.version = new Hex( versionShort );
          }
          else if ( remoteType == RemoteType.DIGITAL )
          {
            short[] versionShort = new short[ 6 ];
            for ( int k = 0; k < 6; k++ )
            {
              versionShort[ 5 - k ] = ( short )( 0x30 + ( versionNum % 10 ) );
              versionNum /= 10;
            }
            fileData.version = new Hex( versionShort );
          }
          upgradeData.put( nameOut, fileData );
          //          System.err.println( nameOut + "   " + versionStr );
        }
        zipIn.close();
      }
      catch( Exception e )
      {
        System.err.println( "File data creation failed" );
        return false;
      }
      boolean ok = true;
      for ( String name : ucSysNames )
      {
        if ( remoteType == RemoteType.DIGITAL )
        {
          name = name.toLowerCase();
        }
        if ( !upgradeData.containsKey( name ) )
        {
          System.err.println( "File " + name + " in sysNames but not in upgrade" );
          // BlasterFirmware is the only file that is not present in the set of 
          // upgrade files for all Touch-style remotes
          if ( !name.equals( "BLASTERFIRMWARE" ) )
          {
            ok = false;
          }
        }
      }
      return ok;
    }
    
    public LinkedHashMap< String, FileData > getUpgradeData()
    {
      return upgradeData;
    }

    private int intFromHex( Hex hex, int offset, boolean littleEndian )
    {
      int n = 0;
      for ( int i = 0; i < 4; i++ )
      {
        int ndx = littleEndian ? 3 - i : i;
        n = ( n << 8 ) | hex.getData()[ offset + ndx ];
      }
      return n;
    }
    
    private LinkedHashMap< String, FileData > setFileContentData( Hex appImg )
    {
      // Only applies to type DIGITAL
      if ( remoteType != RemoteType.DIGITAL )
      {
        return null;
      }
      LinkedHashMap< String, FileData > additions = new LinkedHashMap< String, FileData >();
      int sigOffset = intFromHex( appImg, 2, false );
      int numEntries = appImg.getData()[ sigOffset + 0x2A ] & 0xFF;
      int start = intFromHex( appImg, sigOffset + 0x2B, false );
      for ( int i = 0; i < numEntries; i++ )
      {
        if ( i == 2 )
        {
          continue;  // skip E2 area
        }
        FileData fd = new FileData();
        int addr = intFromHex( appImg, sigOffset + 0x2B + i * addrSize, false );
        if ( addr < 0 )
        {
          addr = intFromHex( appImg, ( addr & 0x7FFFFFFF ) - start, false );
        }
        fd.address=addr;
        fd.version = appImg.subHex( addr - start + 6, 6 );
        additions.put( blockNames[ i ], fd );
      }
      return additions;
    }
    
    /**
     * Returns the calculated CRC as integer, with top bit set if 
     * verification failed.
     */
    private int verifyCRC( byte[] data, int offset )
    {
      int length = data.length;
      int crc = CalcCRC( data, offset, length - 3 );
      if ( ( data[ length - 1 ] & 0xFF ) != ( ( crc >> 8 ) & 0xFF )
          || ( data[ length - 2 ] & 0xFF ) != ( crc & 0xFF ) )
      {
        crc |= 1 << 31;
      }
      return crc;
    }
    
    private boolean verifyFileVersions( File sysFile )
    {
      LinkedHashMap< String, FileData > additions = null;
      try
      {
        ZipFile zipIn = new ZipFile( sysFile );
//        System.err.println( "File versions from upgrade files:" );
        for ( String name : upgradeData.keySet() )
        {         
          FileData fd = upgradeData.get( name );
          if ( fd.zName == null )
          {
            continue;
          }
//          System.err.println( "zName = " + fd.zName );
          ZipEntry entry = zipIn.getEntry( fd.zName );
          InputStream zip = zipIn.getInputStream( entry );
          Hex fileVersion = null;
          Hex hex = null;
          if ( remoteType == RemoteType.XZITE )
          {
            byte[] data = RemoteMaster.readBinary( zip, 4, true );
            fileVersion = new Hex( data );
            RemoteConfiguration.decryptObjcode( fileVersion );
          }
          else if ( remoteType == RemoteType.DIGITAL )
          {
            int len = name.equals( "app.img" ) ? ( int )entry.getSize() : 12;
            byte[] data = RemoteMaster.readBinary( zip, len, true );
            hex = new Hex( data );
            RemoteConfiguration.decryptObjcode( hex );
            fileVersion = hex.subHex( 6, 6 );
          }
          
//          System.err.println( fileVersion + "  " + name );
          if ( !fileVersion.equals( fd.version ) )
          {
            zipIn.close();
            return false;
          };
          if ( name.equals( "app.img" ) )
          {
            additions = setFileContentData( hex );
          }
        }
        System.err.println( "Upgrade file versions verified" );
        zipIn.close();
      }
      catch( Exception e )
      {
        System.err.println( "Upgrade file version error" );
        return false;
      }
      if ( additions != null )
      {
        upgradeData.putAll( additions );
      }
      System.err.println( "Version numbers from upgrade:" );
      for ( String name : upgradeData.keySet() )
      {
        FileData fd = upgradeData.get( name );
        System.err.println( "  " + fd.version + "  " + name );
      }
      return true;
    }
    
    private static String getPrefix( String signature )
    {
      int index = signature.equals( "USB0007" ) ? 3 : Integer.parseInt( signature.substring( 5 ) );
      return prefixes[ index - 1 ];
    }
    
    public final static List< String > xziteSysNames = Arrays.asList(
        "MCUFirmware", "BlasterFirmware", "AC_conn.xmg", "Asia.rgn", "Buttons.btn", "ConnectUSB.xmg", 
        "Europe.rgn", "irdb.bin", "lang.dk", "lang.en", "lang.fi", "lang.fr", "lang.ge", "lang.it",
        "lang.nl", "lang.no", "lang.se", "lang.sp", "Latin.rgn", "Learning.xmg", "MidEast.rgn",
        "NAmerica.rgn", "Pacific.rgn", "SetupBtn.btn", "Splash.xmg", "SysIcons.pkg", "USB_conn.xmg",
        "X01tour.xmg", "X02tour.xmg", "X03tour.xmg", "X04tour.xmg", "X05tour.xmg", "X06tour.xmg", 
        "X07tour.xmg" );
    
    private final static List< String > digitalSysNames = Arrays.asList(
        "app.img", "alf.img" );
    
    
    private final static String libraryName = "hidapi";
    private final static String alphas = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz_.";
    private final static String[] prefixes = new String[] {
      "XZITE_X3", "XZITE_X2", "AVL_MONSTER", "XZITE_AR_X2_AR", "XZITE_AR_X3_AR", "XZITE_NEVO_C2", 
      "XZITE_NEVO_C3", "DIGITAL_OFA_DIGITAL_OFA", "DIGITAL_PLUS_OFA_DIGITAL_PLUS_OFA",
      "DIGITAL_AR_DIGITAL_AR", "DIGITAL_PLUS_AR_DIGITAL_PLUS_AR" };
    private final static String[] blockNames = new String[] {
      "Application", // MCU Firmware 
      "Library",     // IR database
      "Fdra",        // The E2 area
      "ALF",         // Additional language font
      "DLF",         // Default language font
      "Graphics",    // Graphic info
      "Screen"       // Screen info 
      };
    
    /**
     *   The maximum size of an FDRA read or write
     */
    private final static int chunkSize = 0x4000;

   
}

  
