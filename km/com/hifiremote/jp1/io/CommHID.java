package com.hifiremote.jp1.io;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import com.hifiremote.jp1.PropertyFile;
import com.hifiremote.jp1.Remote;
import com.hifiremote.jp1.RemoteConfiguration;
import com.hifiremote.jp1.RemoteManager;
import com.hifiremote.jp1.RemoteMaster;
import com.hifiremote.jp1.RemoteMaster.LanguageDescriptor;
import com.hifiremote.jp1.RemoteMaster.NegativeDefaultButtonJOptionPane;
import com.hifiremote.jp1.RemoteMaster.Use;
 
public class CommHID extends IO 
{
  /*
   *  Summary of XSight Touch packet types, hex numbering.  Packets are all 64 bytes,
   *  the first byte being the packet type and the last two being a CRC checksum.
   *  The structure of the remaining bytes depends on packet type and its use.
   *  Official UEI names in brackets at end.  It appears that UEI only uses hex
   *  values that are composed of decimal digits.
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
   *    16  Format the remote's file system.  This takes no parameters. (FormatFileSystem)
   *    17  Request list the files in the remote's file system.  The list is returned
   *          in a series of type 18 packets. (Ls)
   *    18  Response packet giving names of files in the remote's file system.  (LsData)
   *    19  Request length of named file.  Response is a type 01 packet giving length
   *          or responding that the file is absent. (GetFileSize)
   *    20  Request for the remote to enter upgrade mode.  It carries no data
   *          and the response is a type 01 packet with no data, but the remote
   *          then disconnects its USB port followed by a reconnection. (StartUpdate)
   *    21  In upgrade mode, request to start update.  Takes length of firmware
   *          file as parameter. (StartUpdate)
   *    22  Following acknowledgement of a type 21 packet, the firmware update is
   *          sent as a series of type 22 packets.  (SendUpdate)
   *    23  Exit update mode.  Takes the overall CRC of the firmware as parameter.
   *          Response is a type 01 packet followed by disconnection and
   *          reconnection.  (ExitUpdate)
   *    27  Request a 6-byte value that is possibly the complement of a hex serial
   *          number, returned in a type 01 packet.  There are no known side effects.
   *          (ReadSerialNumber) 
   */
  
  /*  
   *  Command Status Codes, XSight Lite/Plus:
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
  int internalFlashSize;
  int externalFlashSize;
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
  File sysFile = null;

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
    return "1.0";
  }

  public String[] getPortNames() {
    String[] portNames  = {"HID"};
    return portNames;
  }

  public int getRemotePID() {
    return thisPID;
  }

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
  
  boolean eraseFDRA( int startAddress, int endAddress )
  {
    if ( startAddress > endAddress )
      return true;
    
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
    if ( RemoteMaster.admin )
    {
      System.err.println( bytesToString( outReport ).substring( 3 ) );
    } 
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
    int result = 0;
    if ( !writeFDRAcmdReport(cmdBuff) )
    {
      result = 1;
    }
    else if ( !readFDRAreport() )  
    {
      result = 2;
    }
    else if ( dataRead[0] != 0 || dataRead[1] < 2 || dataRead[2] != 0 )
    {
      result = 3;
    }
    if ( RemoteMaster.admin )
    {
      System.err.println( "Enter service returned " + result );
    }
    return result;
  }
  
  /**
   *  Return codes: 0 = success, 1 = write failed, 2 = read failed, 3 = error returned 
   */
  private int exitBootstrap()
  {
    byte[] cmdBuff = {(byte)0x00, (byte)0x02, (byte)0x52, (byte)0x50 };
    int result = 0;
    if ( !writeFDRAcmdReport(cmdBuff) )
    {
      result = 1;
    }
    else if ( !readFDRAreport() )  
    {
      result = 2;
    }
    else if ( dataRead[0] != 0 || dataRead[1] < 2 || dataRead[2] != 0 )
    {
      result = 3;
    }
    if ( RemoteMaster.admin )
    {
      System.err.println( "Exit bootstrap returned " + result );
    }
    return result;
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
      if ( RemoteMaster.admin )
      {
        System.err.println( bytesToString( outReport ).substring( 3 ) );
      } 
      try {
        devHID.write(outReport);
      } catch (Exception e) {
        return false;
      }
      return true;
    }

  boolean writeFDRAcmdReport(byte [] cmdBuff)  
  {  
    System.arraycopy(cmdBuff, 0, outReport, 1, cmdBuff.length);
    if ( RemoteMaster.admin )
    {
      System.err.println( bytesToString( cmdBuff ) );
    }
    try {
      devHID.write(outReport);
    } catch (Exception e) {
      return false;
    }
    return true;
  }
  
  boolean readFDRAreport()  
  {
    try {
      devHID.readTimeout(inReport, 15000);
      System.arraycopy(inReport, 0, dataRead, 0, 64);
      if ( RemoteMaster.admin )
      {
        System.err.println( "   " + bytesToString( inReport ) );
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }
  
  boolean reopenFDRARemote()
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
  
  boolean getFDRAInfoAndSig()  {
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
    {
      return false;
    }
    System.err.println( "Sig block read is:" );
    System.err.println( Hex.toString( dataRead, 32, 0, infoSize ) );
    try 
    {
      signature = new String(dataRead, sigStart, 6, "UTF-8");
    } 
    catch (UnsupportedEncodingException e) 
    {
      System.err.println( "Error in reading signature block" );
      return false;
    }
    if ( isAppInfo2 && newAppInfo2 )
    {
      infoCount = dataRead[ 42 ] & 0xFF;
      if ( infoCount > 0x10 )
      {
        // There are not this many entries in any remote, so there is an error
        System.err.println( "Error: number of signature block entries " + infoCount + " is too big" );
        return false;
      }
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
        remote = remotes.get( 0 );
      }

      if ( remote != null )
      {
        File upgSource = RemoteMaster.getUpgradeSource();
        if ( upgSource != null && !RemoteMaster.isValidUpgradeSource( upgSource, remote ) )
        {
          RemoteMaster.clearUpgradeSource();
        }
        
        if ( thisPID == 7 )
        {
          interfaceType = 6;
          remoteType = RemoteType.AVL;
          sysNames = avlSysNames;
          internalFlashSize = 0x10000;
          externalFlashSize = 0;
          isAppInfo2 = true;
          newAppInfo2 = true;
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
          internalFlashSize = 0x20000;
          externalFlashSize = 0x20000;
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
      
      if ( interfaceType == 0x106 || interfaceType == 6 )
      {
        if ( interfaceType == 0x106 && portName != null && portName.equals( "UPG" ) )
        {
          return reopenFDRARemote() ? "UPG" : "";
        }
        reopenFDRARemote();
        if ( progressUpdater != null )
          progressUpdater.updateProgress( 70 );  
        waitForMillis( 200 );
        // In the Monster remote, getInfoAndSig can only be used in Service mode
        if ( interfaceType != 6 && getFDRAInfoAndSig() && E2address > 0 )
        {
          System.err.println( "GetInfoAndSig succeeded." );
        }
        else
        {
          if ( interfaceType != 6 )
          {
            System.err.println( "GetInfoAndSig failed.  Taking E2 data from RDF." );
          }
          E2address = remote.getBaseAddress();
          E2size = remote.getEepromSize();
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
  
  public String getXZITEVersion( Hex version )
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
    int written = writeXZITEUSBReport( new byte[]{0x17}, 1 );  // Ls
    if ( written != 65 || readXZITEUSBReport( ssdIn ) < 0 )
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
      if ( readXZITEUSBReport( ssdIn ) < 0 )
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
      writeXZITEUSBReport( o, 2 );
      count++;
    }
    Collections.sort( fileList );
    for ( String name : fileList )
    {
      byte[] sizeData = readXZITEFileBytes( name, true );
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
    int written = writeXZITEUSBReport( new byte[]{0x16}, 1 );
    upgradeSuccess = written == 65;
    int read = readXZITEUSBReport(ssdIn, 5000);
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

  /**
   *   Saves the firmware to a file whose name is SysPPPP_VVVVVV.bin or
   *   SysPPPP_VVVVVV.AAA.bin where PPPP is the USB PID of the remote, VVVVVV is the
   *   6-digit version number of the Application block in the firmware and AAA, if
   *   present, is the last three digits of the ALF block in the firmware (the first
   *   three always being 500).  The version numbers are read from the remote with
   *   the version command, not extracted from the firmware file.
   */
  public String saveFDRAfirmware()
  {
    Hex appVersion = null;
    Hex alfVersion = null;
    if ( enterService() != 0 )
    {
      return null;
    }
    if ( ( appVersion = getVersionFromFDRAremote( 0 ) ) == null
        || remoteType == RemoteType.DIGITAL && ( alfVersion = getVersionFromFDRAremote( 3 ) ) == null )
    {
      appVersion = new Hex( new short[]{0x30,0x30,0x30,0x30,0x30,0x30} );
    }
    if ( !getFDRAInfoAndSig() )
    {
      firmwareAddress = remoteType == RemoteType.DIGITAL ? 0x1600 : 0x846;
    }
    int firmwareSize = Math.min( internalFlashSize, E2address ) - firmwareAddress;
    String name = String.format( "Sys%04X", thisPID ) + "_" + appVersion.subString( 0, 6 );
    if ( alfVersion != null )
    {
      name += "." + alfVersion.subString( 3, 3 );
    }
    name += ".bin";
    File outputDir = new File( RemoteMaster.getWorkDir(), "XSight" );
    outputDir.mkdirs();
    File file = new File( outputDir, name );
    BufferedOutputStream out = null;
    try
    {
      out = new BufferedOutputStream( new FileOutputStream( file, false ));
      byte[] buffer = new byte[ internalFlashSize ];
      int read = readFDRAfirmware( firmwareAddress, buffer, firmwareSize );
      if ( read != firmwareSize )
      {
        out.close();
        return null;
      }
      out.write( buffer, 0, firmwareSize );
      if ( E2address > internalFlashSize )
      {
        int size = E2address - internalFlashSize;
        read = readFDRAfirmware( internalFlashSize, buffer, size );
        if ( read != size )
        {
          out.close();
          return null;
        }
        out.write( buffer, 0, size );
        Arrays.fill( buffer, 0, E2size, ( byte )0xFF );
        out.write( buffer, 0, E2size );
        size = internalFlashSize + externalFlashSize - E2address - E2size;
        read = readFDRAfirmware( E2address + E2size, buffer, size );
        if ( read != size )
        {
          out.close();
          return null;
        }
        out.write( buffer, 0, size );
      }
      out.close();
    }
    catch( Exception e )
    {
      return null;
    }
    return name;
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
            + "\n    Firmware version = " + getXZITEVersion( null );
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
      enterService();
      if ( getFDRAInfoAndSig() && E2address > 0 )
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
  
  private int readFDRAfirmware( int address, byte[] buffer, int length )
  {
    setProgressName( "READING FIRMWARE:" );
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
        if ( RemoteMaster.admin )
        {
          System.err.println( "   " + bytesToString( inReport ) );
        }
        if ( numRead == 0 )
        {
          System.err.println( "Read attempt timed out for FDRA report " + ( i + 1 ) + " of " + numReports );
          success = false;
          break;
        }
        else if ( numRead != 64 )
        {
          System.err.println( "Incomplete read of FDRA report " + ( i + 1 ) + " of " + numReports + ", " + numRead + " bytes of 64 read" );;
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
    return success ? length : totalRead - 3;
  }
  
  /**
   *   The erase argument determines both whether each block is erased before being written
   *   and whether the address range is checked to be within the E2 area.  This is set
   *   to true when writing to E2 area and false when updating firmware, as the erase is
   *   performed as a separate step during the updating process.
   */
  private int writeFDRA( int address, byte[] buffer, int length, boolean erase )
  {
    if ( length == 0 )
      return 0;
    if ( erase && ( ( address < E2address) || (address + length > E2address + E2size) ) )
      return -1;
    setProgressName( erase ? "UPLOADING:" : "WRITING NEW FIRMWARE:" );
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
    if ( RemoteMaster.admin )
    {
      System.err.println( "writeFDRA lengths: In " + length + ", Out " + pos );
    }
    return pos;
  }
  
  int writeFDRAchunk( int address, int delta, byte[] buffer, int length, int total, boolean erase )  {
    address += delta;
    System.err.println();
    System.err.println( "Starting FDRA write of $" + Integer.toHexString( length ).toUpperCase() + " bytes at $" + String.format( "%05X", address ) );   
    int writeBlockSize = 0x3C - addrSize;
    int erasePageSize = 0x200;
    int offset, endAdr;
    int blockLength = writeBlockSize;
    byte tempBuf[] = new byte[65];
    if ( erase && ( (length % erasePageSize) != 0 || (address % erasePageSize) != 0 ) )
      return -1;
    endAdr = address + length - 1;
    if ( erase && !eraseFDRA( address, endAdr ) )
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
    boolean noUpgrade = RemoteMaster.noUpgradeItem.isSelected() || isPortUpg;
    
    if ( getUse() == Use.DOWNLOAD && !noUpgrade )
    {
      if ( RemoteMaster.admin )
      {
        System.err.println( "Read dialog starts:");
      }
      forceUpgrade = remoteType == RemoteType.XZITE && RemoteMaster.forceUpgradeItem.isSelected()
          || remoteType != RemoteType.XZITE && RemoteMaster.forceFDRAUpgradeItem.isSelected();
      String title = forceUpgrade ? "Forced upgrade" : "Firmware upgrade";
      sysFile = RemoteMaster.getUpgradeSource();
      if ( sysFile == null )
      {
        sysFile = RemoteMaster.getRmirSys();
      }
      else
      {
        title += " from " + sysFile.getName();
      }
      
      // Read firmware file versions from remote
      setProgressName( "CHECKING FOR UPGRADE:" );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 0 );
      firmwareFileVersions.clear();
      if ( !forceUpgrade && !getVersionsFromRemote( true ) ) 
      {
        return 0;
      }
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 30 );
      // Test if system file required for upgrading is present
      boolean doUpgradeTest = false;
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
//        if ( upgNeeds[ 1 ] == 0 && changed.contains( "alf.img" ) 
//            && RemoteMaster.noLanguageUpgradeItem.isSelected() )
//        {
//          upgNeeds[ 0 ] = upgNeeds[ 2 ] = 0;
//          changed.clear();
//        }
        

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
          message += "A firmware upgrade should preserve the current setup, but it is\n" 
              + "recommended that you save the current setup as a .rmir file before\n"
              + "upgrading.\n\n";
          if ( RemoteMaster.ioNeedsPowerManagementCheck( this ) )
          {
            message += "You appear to be using Windows 8.1 or later, which support Enhanced\n"
                + "Power Management.  This may cause issues during a firmware upgrade.\n"
                + "You should make sure that you know how to use regedit to edit the\n"
                + "Windows registry before proceeding.  If registry changes are needed\n"
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
            if ( remoteType == RemoteType.XZITE )
            {
              message =  "A firmware upgrade involves updating both the firmware of the central\n"
                  + "processor and a series of support files.  Sometimes that of the central\n"
                  + "processor is already up to date and only the support files need updating.\n"
                  + "In that case the upgrade runs to completion as a single process.  If the\n"
                  + "processor firmware needs upgrading, however, the upgrade takes place as a\n"
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
            else if ( remoteType == RemoteType.DIGITAL )
            {
              if ( !newFiles.isEmpty() )
              {
                String defaultFile = newFiles.get( 0 );
                changed.add( defaultFile );
                int defaultLangCode = Integer.parseInt( defaultFile.substring( 3, 4 ) );

                String defaultStr = defaultLangCode == 0 ? "without additional" : "with " 
                    + RemoteMaster.getLanguage( defaultLangCode ).name;
//                String title = "Language support";
                message = "The additional language support currently installed is not available\n"
                    + "for this firmware upgrade.  If you continue, the upgrade will be\n"
                    + "installed " + defaultStr + " language support.\n\n"
                    + "Do you want to continue with the upgrade?";
                response = NegativeDefaultButtonJOptionPane.showConfirmDialog( null, message, title, 
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
              }
            }
            else
            {
              response = JOptionPane.YES_OPTION;
            }
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
            message = null;
            if ( remoteType == RemoteType.XZITE )
            {
              if ( !upgradeXZITE( upgNeeds, changed, newFiles ) )
              {
                message = "Upgrade failed.";
                JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
                return 0;
              }
            }
            else if ( remoteType == RemoteType.DIGITAL || remoteType == RemoteType.AVL )
            {
              if ( !upgradeFDRA( upgNeeds, changed ) )
              {
                message = "Upgrade failed.";
                JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
                return 0;
              }
            }
            else
            {
              RemoteMaster.forceUpgradeItem.setSelected( false );
              message = "Firmware upgrade for this remote is not yet implemented.\n\n";
            }
            
            if ( message == null )
            {
              message = "Upgrade succeeded.\n\n";
            }
            message += "Do you want to continue with a normal download?";
            int reply = JOptionPane.showConfirmDialog( null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
            if ( reply != JOptionPane.YES_OPTION )
            {
              return 0;
            }
          }  // if ( response == JOptionPane.YES_OPTION )
        } // if ( upgNeeds[ 0 ] > 0 )
      } // if ( doUpgradeTest )
    } // if ( getUse() == Use.DOWNLOAD )null

    if ( remoteType == RemoteType.DIGITAL )
    {
      bytesRead = readFDRA(address, buffer, length);
    }
    else if ( remoteType == RemoteType.XZITE )
    { 
      bytesRead = readXZITE( buffer );
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
//    File sysFile = RemoteMaster.getRmirSys();
    upgradeSuccess = true;
    if ( upgNeeds[ 1 ] > 0 )
    {
      setProgressName( "ENTERING UPGRADE MODE:" );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 0 );
      
      try
      {
        // Test the MCUFirmware upgrade file for validity
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
        // Put remote into upgrade mode with type 0x20 packet.
        // Change packet type to 0x27 for testing without entering upgrade mode.
        writeXZITEUSBReport( new byte[]{0x20}, 1 );
        if ( readXZITEUSBReport( ssdIn ) < 0 || ssdIn[ 2 ] != 0 )
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
        if ( waitForReconnection()
            && writeXZITEFirmwareFile( data ) )
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
            int written = writeXZITEUSBReport( ssdOut, 62 );
            success = written == 65;
            int read = readXZITEUSBReport(ssdIn);
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
            message = "Unable to write MCU firmware.\n\n"
                + "Please disconnect the remote, remove the batteries, put them\n"
                + "back in, reconnect the remote and repeat the upgrade process.";
            JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
          }
          return false;
        }
        System.err.println( "Exited upgrade mode" );
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

    if ( ( upgNeeds[ 1 ] == 0 || waitForReconnection() ) &&
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
      return true;
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
  }
  
  private boolean upgradeFDRA( int[] upgNeeds, List< String > changed )
  {
    System.err.println( "Proceeding with firmware revision" );
    upgradeSuccess = true;
    int firmwareAddress = 0;
    int firmwareSize = 0;
    int alfStart = 0;
    byte[] data = null;      // The full binary data of app.img
    byte[] alfData = null;   // The binary data of the alf.img file if present
    if ( upgNeeds[ 0 ] > 0 )
    {
      setProgressName( "PREPARING THE UPGRADE:" );
      if ( progressUpdater != null )
        progressUpdater.updateProgress( 0 );
      
      try
      {
        // Test the app.img and alf.img (if present) upgrade files for validity
        int eLength = 0;
        InputStream in = null;
        ZipFile zipIn = null;        
        if ( sysFile.getName().endsWith( ".sys" ) )
        {
          zipIn = new ZipFile( sysFile );
          String zName = upgradeData.get( "app.img" ).zName;
          ZipEntry entry = zipIn.getEntry( zName );
          eLength = ( int )entry.getSize();
          in = zipIn.getInputStream( entry );
          data = RemoteMaster.readBinary( in, eLength );
          if ( !changed.isEmpty() && !changed.get( 0 ).equals( "alf0.img" ) )
          {
            zName = upgradeData.get( changed.get( 0 ) ).zName;
            if ( zName != null )
            {
              entry = zipIn.getEntry( zName );
              int alfLength = ( int )entry.getSize();
              in = zipIn.getInputStream( entry );
              alfData = RemoteMaster.readBinary( in, alfLength );
            }
          }         
          zipIn.close();
        }
        else
        {
          eLength = ( int )sysFile.length();
          in = new FileInputStream( sysFile );
          data = RemoteMaster.readBinary( in, eLength );
          in.close();
        }
        
        // Calculate firmware address and size, as if existing firmware
        // is corrupt, they cannot be read from the remote.
        firmwareSize = eLength - externalFlashSize;
        firmwareAddress = Math.min( internalFlashSize, E2address ) - firmwareSize;
  
        Hex hex = new Hex( data );
        if ( sysFile.getName().endsWith( ".sys" ) )
        {
          RemoteConfiguration.decryptObjcode( hex );
          data = hex.toByteArray();
          if ( alfData != null )
          {
            // Overwrite the ALF block from app.img with data from alf.img.  This is
            // used when there is an MCU firmware update, to ensure the correct
            // language setting.
            Hex alfHex = new Hex( alfData );
            RemoteConfiguration.decryptObjcode( alfHex );
            alfData = alfHex.toByteArray();
            alfStart = upgradeData.get( blockNames[ 3 ] ).address;
            System.arraycopy( alfData, 0, data, alfStart - firmwareAddress, alfData.length );
            hex = new Hex( data );
          }
        }
        if ( alfData == null && upgradeData.get( blockNames[ 4 ] ) != null )
        {
          // Copy the ALF block from app.img to alfData.  This is used when only the
          // ALF block is updated.  This copying takes place in all cases for
          // sysFile ending in .bin, and when alf0.img is required for sysFile
          // ending in .sys.  For this sysFile and other language settings, alfData
          // has already been copied from the relevant alf.img file.
          alfStart = upgradeData.get( blockNames[ 3 ] ).address;
          int alfEnd = upgradeData.get( blockNames[ 4 ] ).address;
          alfData = Arrays.copyOfRange( data, alfStart - firmwareAddress, alfEnd - firmwareAddress );
        }
        
        // Verify checksums of app.img
        int pos = 0;
        while ( pos < firmwareSize + externalFlashSize )
        {
          if ( pos == E2address - firmwareAddress )
          {
            pos += E2size;
            continue;
          }
          int len = intFromHex( hex, pos + 2, false );
          short sum = 0;
          Hex stored = hex.subHex( pos, 2 );
          if ( remoteType == RemoteType.AVL && pos == 0 )
          {
            // Need to exclude the RAM area $1800-$182B from the checksum, but make
            // sure that the whole region beyond this is included and that the first
            // two bytes are not skipped.
            sum = jp12ComputeCheckSum( data, pos + 2, 0x1800 - firmwareAddress - pos - 2 );
            pos += 0x182C - firmwareAddress - 2;
            len -= 0x1800 - firmwareAddress - 2;
          }
          sum ^= jp12ComputeCheckSum( data, pos + 2, len - 2 );
          Hex calc = new Hex( new short[]{ sum, ( short )( ~sum & 0xFF ) } );
          if ( !calc.equals( stored ) )
          {
            return false;
          }
          pos += len;
        }
        if ( alfData != null )
        {
          // Verify checksum of alf.img
          short sum = jp12ComputeCheckSum( alfData, 2, alfData.length - 2 );
          Hex calc = new Hex( new short[]{ sum, ( short )( ~sum & 0xFF ) } );
          Hex stored = new Hex( Arrays.copyOf( alfData, 2 ) );
          if ( !calc.equals( stored ) )
          {
            return false;
          }
        }
      }
      catch ( Exception e )
      {
        System.err.println( "Error in firmware upgrade data" );
        return false;
      }
    }
      
    RemoteMaster.forceFDRAUpgradeItem.setSelected( false );
    if ( upgNeeds[ 1 ] > 0 )  // > 2 is for TESTING, > 0 is normal use
    {
      try
      {
        byte[] dataE2 = null;
        boolean success = false;
        
        int i = 0;        
        while ( !( success = enterService() == 0 ) 
            && i++ < 3 && waitForReconnection( 7000 ) ){};
        System.err.println( "Enter service on attempt " + ( i + 1 ) + " : " + success );
        if ( !success ) return false;

        if ( remoteType == RemoteType.DIGITAL )
        {
          // For RemoteType.AVL the E2 area is not erased and so does not have to be saved.
          i = 0;
          dataE2 = new byte[ E2size ];
          while ( !( success = readFDRAnoUpdate( E2address, dataE2, E2size ) == E2size )
              && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
          System.err.println( "Read E2 on attempt " + ( i + 1 ) + " : " + success );
          if ( !success ) return false;
          
          // RemoteType.AVL does not have external flash
          i = 0;
          while( !( success = eraseFDRA( internalFlashSize, internalFlashSize + externalFlashSize - 1 ) )
              && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
          System.err.println( "Erase external flash on attempt " + ( i + 1 ) + " : " + success );
          if ( !success ) return false;
          
          // RemoteType.AVL needs special handling of erasure of firmware, due to the 
          // high RAM block in the middle of its address range
          i = 0;
          while ( !( success = eraseFDRA( firmwareAddress, firmwareAddress + firmwareSize - 1 ) )
              && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
          System.err.println( "Erase MCU firmware on attempt " + ( i + 1 ) + " : " + success );
        }
        else // remoteType == RemoteType.AVL
        {
          // For HCS08 processor the firmware erasure is performed as two blocks separated by
          // the high RAM at $1800-$182B.
          while ( !( success = eraseFDRA( firmwareAddress, 0x17FF ) )
              && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
          System.err.println( "Erase MCU lower firmware on attempt " + ( i + 1 ) + " : " + success );
          if ( !success ) return false;
          
          i = 0;
          while ( !( success = eraseFDRA( 0x182C, firmwareAddress + firmwareSize - 1 ) )
              && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
          System.err.println( "Erase MCU upper firmware on attempt " + ( i + 1 ) + " : " + success );
        }
        if ( !success ) return false;
        
        byte[] dataFirmware = new byte[ firmwareSize ];
        System.arraycopy( data, 0, dataFirmware, 0, firmwareSize );
//        Hex test = new Hex( dataFirmware );
        
        i = 0;
        while ( !( success = writeFDRA( firmwareAddress, dataFirmware, firmwareSize, false ) == firmwareSize )
            && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
        System.err.println( "Write MCU firmware on attempt " + ( i + 1 ) + " : " + success );
        if ( !success ) return false;
        
        if ( remoteType == RemoteType.DIGITAL )
        {
          // For RemoteType.AVL there is no external flash
          byte[] dataExtFlash = new byte[ externalFlashSize ];
          System.arraycopy( data, internalFlashSize - firmwareAddress, dataExtFlash, 0, externalFlashSize );
//          Hex test = new Hex( dataExtFlash );
          
          i = 0;
          while ( !( success = writeFDRA( internalFlashSize, dataExtFlash, externalFlashSize, false ) == externalFlashSize )
              && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
          System.err.println( "Write external flash on attempt " + ( i + 1 ) + " : " + success );
          if ( !success ) return false;
        
          // For RemoteType.AVL the E2 area is not erased and so does not have to be rewritten.
          i = 0;
          while ( !( success = writeFDRA( E2address, dataE2, E2size, false ) == E2size )
              && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
          System.err.println( "Write E2 on attempt " + ( i + 1 ) + " : " + success );
          if ( !success ) return false;
        }
        else
        {
          // The file HidIf/UEI.USB.FCom/MonsterUsbApp.cs shows an 18 second delay
          // after writing the firmware, before exiting.
          setProgressName( "COMPLETING UPGRADE:" );
          if ( progressUpdater != null )
            progressUpdater.updateProgress( 0 );
          for ( i = 0; i < 90; i++ )
          {
            waitForMillis( 200, true );
            if ( progressUpdater != null )
              progressUpdater.updateProgress( (int)((double)(i+1)/90 * 100 ) );
          }
        }
        
        i = 0;
        while ( !( success = ( exitBootstrap() == 0 && waitForReconnection( 7000 ) ) ) 
            && i++ < 3 ){};
        System.err.println( "Exit upgrade on attempt " + ( i + 1 ) + " : " + success );
        reopenFDRARemote();
        if ( !success ) return false;
      }
      catch ( Exception e )
      {
        System.err.println( "Firmware upgrade error" );
        return false;
      }
    }
    else if ( !changed.isEmpty() && changed.get( 0 ).startsWith( "alf" ) && alfData != null )
    {
      boolean success = false;
      
      int i = 0;        
      while ( !( success = enterService() == 0 ) 
          && i++ < 3 && waitForReconnection( 7000 ) ){};
      System.err.println( "Enter service on attempt " + ( i + 1 ) + " : " + success );
      if ( !success ) return false;
      
      i = 0;
      while( !( success = eraseFDRA( alfStart, alfStart + alfData.length - 1 ) )
          && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
      System.err.println( "Erase ALF block on attempt " + ( i + 1 ) + " : " + success );
      if ( !success ) return false;
      
      i = 0;
      while ( !( success = writeFDRA( alfStart, alfData, alfData.length, false ) == alfData.length )
          && i++ < 3 && waitForReconnection( 7000 ) && enterService() == 0 ){};
      System.err.println( "Write ALF block on attempt " + ( i + 1 ) + " : " + success );
      if ( !success ) return false;
      
      i = 0;
      while ( !( success = ( exitBootstrap() == 0 && waitForReconnection( 7000 ) ) ) 
          && i++ < 3 ){};
      System.err.println( "Exit upgrade on attempt " + ( i + 1 ) + " : " + success );
      reopenFDRARemote();
      if ( !success ) return false;
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
  
  int writeXZITE( byte[] buffer )
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
      writeXZITEUSBReport( ssdOut, 62 );
      int check = -1;
      if ( readXZITEUSBReport( ssdIn ) < 0 || ( check = ssdInCheck() ) < 0 
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
    setProgressName( "UPLOADING:" );
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
      int written = writeXZITEUSBReport( ssdOut, 62 );
      System.err.println( "File header packet sent" );
      int read = readXZITEUSBReport(ssdIn, 5000);
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
        if ( !writeXZITEBufferOut() )
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
      writeXZITEUSBReport( ssdOut, 62 );
      int check = -1;
      if ( readXZITEUSBReport( ssdIn ) < 0 || ( check = ssdInCheck() ) < 0 
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
    int written = writeXZITEUSBReport( ssdOut, 62 );
    System.err.println( "File header packet sent" );
    int read = readXZITEUSBReport(ssdIn, 5000);
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
      if ( !writeXZITEBufferOut() )
      {
        System.err.println( "Error: terminating at hex position " + Integer.toHexString( pos ) );
        return false;
      }
      pos += size;
      count++;
//      System.err.println( "Packet " + count + " sent" );
    }
    System.err.println( "Bytes written to " + name + ": " + pos );
    return true;
  }
  
  private boolean writeXZITEFirmwareFile( byte[] data )
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
        int written = writeXZITEUSBReport( ssdOut, 62 );
        System.err.println( "Firmware header packet " + ( i+1 ) + " sent" );
        int read = readXZITEUSBReport(ssdIn, 5000);
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
        waitForReconnection();
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
      if ( !writeXZITEBufferOut() )
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
    return waitForMillis( waitTime, false );
  }
  
  boolean waitForMillis( int waitTime, boolean quiet )
  {
    if ( !quiet )
    {
      System.err.println( "Waiting for " + waitTime + "ms" );
    }
    long waitStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    while ( delay < waitTime )
    { 
      delay = Calendar.getInstance().getTimeInMillis() - waitStart;
    }
    return true;
  }
  
  boolean waitForReconnection()
  {
    return waitForReconnection( 2000 );
  }
  
  /**
   *   The wait parameter is the delay between closing the device and starting to look
   *   if it has reconnected.  This needs to be longer when closing the device is forcing
   *   disconnection than when the disconnection has already taken place from the remote.
   */
  boolean waitForReconnection( int wait )
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
      System.err.println( "Closing device failed" );
    }
    
    devHID = null;
    waitForMillis( wait );
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
        if ( devHID == null )
        {
          waitForMillis( 3000 );
        }
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
            + "remote and run the upgrade process again.\n\n"
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
  
  private String getFDRAlangFile()
  {
    Integer langCode = RemoteMaster.getUpgradeLanguage().code;
    if ( langCode == null )
    {
      // Request is to keep current additional language
      langCode = firmwareFileVersions.get( blockNames[ 3 ] ).getData()[ 3 ] - 0x30;
    }
    return "alf" + langCode + ".img"; 
  }
  
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
      if ( remoteType == RemoteType.XZITE )
      {
        changed.addAll( Arrays.asList( "lang.en", "lang.fr", "lang.ge", "lang.it",
            "lang.sp", "lang.no", "lang.se", "Splash.xmg" ) );
        newFiles.addAll( Arrays.asList("lang.dk", "lang.fi", "lang.nl" ) );
      }
      return new int[]{1,1,1};
    }
    
//    // TESTING
//    // This creates essentially the same file versions as in firmware 1.3.7 and
//    // should result in the same return values as forceUpgrade=true, but by running
//    // the test code rather than directly setting the result.
//    firmwareFileVersions.remove( "lang.dk" );
//    firmwareFileVersions.remove( "lang.fi" );
//    firmwareFileVersions.remove( "lang.nl" );
//    for ( String name : firmwareFileVersions.keySet() )
//    {
//      if ( name.startsWith( "lang" ) || name.equals( "Splash.xmg" ) || name.equals( "MCUFirmware" ) )
//      {
//        Hex ver = firmwareFileVersions.get( name );
//        ver.set( (short)0, 2 );
//        firmwareFileVersions.put( name, ver );
//      }
//    }
//    // END TESTING

    int[] out = new int[]{ 0, 0, 0 };
    int upgradeType = 0;
    List< String > fwNames = new ArrayList< String >();
    for ( String name : firmwareFileVersions.keySet() )
    {
      // The name values are those returned by the version command for XZITE remotes
      // or the names taken from the blockNames[] array for DIGITAL and AVL remotes.
      
      // For comparison purposes, fwNames holds the upper case form of the names.
      fwNames.add( name.toUpperCase() );
      if ( name.equalsIgnoreCase( "BlasterFirmware" ) )
      {
        // We are currently unable to update BlasterFirmware, although we have
        // the firmware for certain XZITE remotes.
        continue;
      }
      Hex currentVersion = firmwareFileVersions.get( name );
      String fdName = name;   // The form of the name in the upgradeData map
      int currentOffset = 2;  // The offset of the version as hex within currentVersion
      int[] testOrder = new int[]{ 0,1,2,3,4,5 };
      if ( remoteType == RemoteType.XZITE )
      {
        // For XZITE remotes the name in the upgradeData map is always upper case
        fdName = name.toUpperCase();
        // The hex version is 4 bytes, little-endian
        testOrder = new int[]{ 3,2,1,0 };
      }
      else
      {
        // For DIGITAL and AVL remotes the hex version is 6 bytes, big-endian, as
        // initially set.  The upgradeData and firmwareFileVersions maps use the same
        // name form, taken from blockNames[], so no change required.  However,
        // additional language support requires special treatment of the ALF block.
        currentOffset = 0;  // Offset is 0 for DIGITAL and AVL remotes
        if ( name.equals( blockNames[ 3 ] ) )
        {
          // If the requested language support is not available, add the available
          // file to newFiles.  If it is available, add the requested file to changedFiles.
          // If it is available but differs from current language then do not check
          // version, treat as an upgrade in any case.
          String langFile = getFDRAlangFile();  // The requested language file
          name = fdName = langFile;  // Set to requested language file
          if ( upgradeData.get( langFile ) == null )
          {
            // The requested language support is not available.
            int appImgLangCode = upgradeData.get( blockNames[ 3 ] ).version.getData()[ 3 ] - 0x30;
            String defaultFile = "alf" + appImgLangCode + ".img";
            newFiles.add( defaultFile );
            out[ 0 ] = 1;  // Upgrade required
            upgradeType = upgradeType >= 0 ? 1 : -2;  // Treat as upgrade
            continue;  // Go to next block
          }
          else
          {
            changed.add( langFile );
            int currentLangCode = firmwareFileVersions.get( blockNames[ 3 ] ).getData()[ 3 ] - 0x30;
            String currentLangFile = "alf" + currentLangCode + ".img";
            if ( !langFile.equals( currentLangFile ) )
            {
              out[ 0 ] = 1;  // Upgrade required
              upgradeType = upgradeType >= 0 ? 1 : -2;  // Treat as upgrade
              continue;  // Go to next block
            }
          }
        }
      }

      // For the ALF block of DIGITAL remotes, continue only when old and new
      // languages are the same. The name of the language file is already in
      // changed list in any case.  If another block needs upgrading then a
      // full firmware upgrade is required, out[0] and out[1] will be set to 1
      // by that need and the changed value specifies the language to include.
      // If no other block needs upgrading but this current language does, then
      // that need will set out[0] to 1 but leave out[1] at 0 so only the language
      // upgrade will take place.  If nothing needs upgrading then out[0] will be
      // left at 0 and so no upgrade will take place, despite the changed list
      // being non-empty.
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
          out[ 0 ] = 1;  // An upgrade is required
          if ( name.equalsIgnoreCase( "MCUFirmware" ) 
              || remoteType != RemoteType.XZITE && !name.startsWith( "alf" ) )
          {
            // An upgrade of the MCU firmware is required.  For XZITE remotes this is the
            // single MCUFirmware entry, for other remotes a version difference in any block
            // normally needs an entire firmware upgrade as all blocks are in a single file.
            // The one exception is the ALF block when a separate alf.img file is available.
            out[ 1 ] = 1;
          }
          else
          {
            // For DIGITAL remotes, the only name that can be added here is that of the
            // current language when an upgrade of it is required
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
    if ( remoteType == RemoteType.XZITE )
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
  
  int readXZITE( byte[] buffer )
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
      writeXZITEUSBReport( ssdOut, 62 );
      if ( readXZITEUSBReport( ssdIn ) < 0 )
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
      byte[] data = readXZITEFileBytes( name, false );
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
  
  public short[] readXZITEFile( String name )
  {
    runningTotal = 0;
    byte[] bBuffer = readXZITEFileBytes( name, false );
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
  
  public byte[] readXZITEFileBytes( String name, boolean sizeOnly )
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
      result = readXZITEFileBytesOnce( name, sizeOnly );
    }
    if ( attempt > 1 && result != null )
    {
      System.err.println( "File " + name + " took " + attempt + " attempts to read" );
    }
    return result;
  }
  
  private byte[] readXZITEFileBytesOnce( String name, boolean sizeOnly )
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
    writeXZITEUSBReport( ssdOut, 62 );
    // First type 01 packet returned gives file length or sets absence flag.
    if ( readXZITEUSBReport( ssdIn ) < 0 )
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
      if ( readXZITEUSBReport( ssdIn ) < 0 )
      {
        System.err.println( "Read error before end of file \"" + name + "\"" );
        return null;
      }
      
      packetID = ssdIn[ 1 ];
      sequenceNumber = ( ssdIn[ 2 ] & 0xFF ) | ( ( ssdIn[ 3 ] & 0xFF ) << 8 ); 
      int len = ssdIn[ 4 ];
      total += len;
      System.arraycopy( ssdIn, 6, buffer, ndx, len );
      ndx += len;
      // Set packet serial in acknowledgement.
      ssdOut[ 1 ] = ( byte )packetID;
      // Send acknowledgement packet.
      writeXZITEUSBReport( ssdOut, 62 );
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
    return readXZITEFileBytes( name, false );
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
  
  /**
   *   getVersionsFromRemote(...) uses the version command of the remote to get the
   *   versions of the MCU firmware and system files in the case of XZITE remotes,
   *   or of the blocks of the firmware block structure of the DIGITAL and AVL remotes.
   *   It populates the firmwareFileVersions map with the hex version in the form 
   *   returned by the command, the key being the name returned by the command in
   *   the case of XZITE remotes, or the name taken from blockNames[] array in the
   *   case of DIGITAL and AVL remotes.  
   */
  boolean getVersionsFromRemote( boolean getSerial )
  {
    firmwareFileVersions.clear();
    if ( remoteType == RemoteType.DIGITAL || remoteType == RemoteType.AVL )
    {
      int limit = remoteType == RemoteType.AVL ? 3 : 7;
      for ( int index = 0; index < limit; index++ )
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

    // Continue for remoteType == RemoteType.XZITE
    byte[] o = new byte[2];
    o[0]=1;
    // Initiate read of firmware file versions.
    writeXZITEUSBReport( new byte[]{4}, 1 );
    if ( readXZITEUSBReport( ssdIn ) < 0 )
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
      if ( readXZITEUSBReport( ssdIn ) < 0 )
      {
        System.err.println( "Read versions from remote failed on file " + ( i+1 ) + " of " + firmwareFileCount );
        return false;
      }
      saveXZITEVersionData();
      // Get packet serial and set it in acknowledgement packet.
      o[1] = ssdIn[ 1 ];
      // Send acknowledgement.
      writeXZITEUSBReport( o, 2 );
    }
    if ( !getSerial )
    {
      return true;
    }
    writeXZITEUSBReport( new byte[]{0x27}, 1 );
    if ( readXZITEUSBReport( ssdIn ) < 0 )
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
  
  void saveXZITEVersionData()
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
      bytesWritten = writeFDRA(address, buffer, length, true );
    else if ( interfaceType == 0x201 )
      bytesWritten = writeXZITE( buffer );
    return bytesWritten;
  }
  
  int readXZITEUSBReport(byte[] buffer)
  {
    return readXZITEUSBReport( buffer, 3000 );
  }
  
  int readXZITEUSBReport(byte[] buffer, int timeout ) 
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
  
  int writeXZITEUSBReport( byte[] buffer, int length ) {  //buffer must be <=62 bytes
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
  
  boolean writeXZITEBufferOut()
  {
    int n = 0;
    boolean success = false;
    do {
      int written = writeXZITEUSBReport( ssdOut, 62 );
      success = written == 65;
      int read = readXZITEUSBReport(ssdIn);
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
    
/**
 *  setFileData(...) populates the map upgradeData with pairs ( name, fileData ) where
 *    name = the name portion of the UEI upgrade files for the current remote (case unchanged,
 *      which is upper case for XZITE remotes and lower case for all other remotes; in the
 *      case of alf.img files the name body is postfixed with the 4th digit of the version
 *      number, which is the language identifier, as there may be multiple alf.img files
 *      for different languages;
 *    fileData.zName is the encrypted name as in RMIR.sys;
 *    fileData.versionNum is the version number from the UEI file name as a long integer;
 *    fileData.version is the hex form of the version number as stored in the file itself,
 *      which is a little-endian 4-byte integer for XZITE remotes, a 6-byte ASCII string
 *      for DIGITAL and AVL remotes;
 *    fileData.address is left unset for these entries.
 *  When sysFile is a .bin file rather than RMIR.sys, an equivalent map is created with
 *  one or two entries. There is always an entry for app.img, and also one for alf.img if
 *  an ALF block is present in the file.  The fileData.zName is unset for these entries.
 */
    public boolean setFileData( File sysFile )
    {
      List< String > ucSysNames = new ArrayList< String >();
      upgradeData.clear();
      for ( String name : sysNames )
      {
        ucSysNames.add( name.toUpperCase() );
      }
      try
      {
        if ( sysFile.getName().endsWith( ".sys" ) )
        {
          ZipFile zipIn = new ZipFile( sysFile );
          Enumeration< ? extends ZipEntry > zipEnum = zipIn.entries();
          String prefix = getPrefix( getRemoteSignature() );
          int i = 0;
          // System.err.println( "Reconverted names:");
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
            if ( nameOut.equals( "alf.img" ) )
            {
              // Add the language identifier digit into the name body
              nameOut = "alf" + versionStr.substring( 3, 4 ) + ".img";
            }
            FileData fileData = new FileData();
            fileData.zName = nameIn;
            setFileVersion( fileData, versionStr );
            upgradeData.put( nameOut, fileData );
            // System.err.println( nameOut + "   " + versionStr );
          }
          zipIn.close();
        }
        else if ( sysFile.getName().endsWith( ".bin" ) )
        {
          // RemoteType.DIGITAL and RemoteType.AVL only
          FileData fileData = new FileData();
          String nameOut = sysFile.getName();
          int pos = nameOut.lastIndexOf( '_' );
          String versionStr = nameOut.substring( pos + 1, nameOut.length() - 4 );
          setFileVersion( fileData, versionStr.substring( 0, 6 ) );
          upgradeData.put( "app.img", fileData );
          if ( versionStr.length() > 7 )
          {
            // The end part of the version number of the included alf.img block is postfixed
            // to the app.img version string
            fileData = new FileData();
            String aux = versionStr.substring( 7 );
            versionStr = "500000".substring( 0, 6 - aux.length() ) + aux;
            setFileVersion( fileData, versionStr.substring( 0, 6 ) );
            nameOut = "alf" + versionStr.substring( 3, 4 ) + ".img";
            upgradeData.put( nameOut, fileData );
          }
        }
      }
      catch( Exception e )
      {
        System.err.println( "File data creation failed" );
        return false;
      }
      boolean ok = true;
      for ( String name : ucSysNames )
      {
        if ( remoteType != RemoteType.XZITE )
        {
          name = name.toLowerCase();
        }
        if ( !upgradeData.containsKey( name ) && !name.equals( "alf.img" ) )
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
    
    /**
     *   setFileVersion(...) takes a version as a string of digits extracted from an
     *   upgrade filename and sets the corresponding fileData.versionNum long integer and 
     *   fileData.version hex parameters in the supplied fileData.
     */
    private void setFileVersion( FileData fileData, String versionStr )
    {
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
      else
      {
        short[] versionShort = new short[ 6 ];
        for ( int k = 0; k < 6; k++ )
        {
          versionShort[ 5 - k ] = ( short )( 0x30 + ( versionNum % 10 ) );
          versionNum /= 10;
        }
        fileData.version = new Hex( versionShort );
      }
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
    
/**
 *   setFileContentData(...) takes as input the hex data of the app.img file of the upgrade
 *   for a DIGITAL or AVL remote and returns further entries for the upgradeData map created
 *   by setFileData(...).  It locates the signature block in the file and reads the list of
 *   pointers from that block, skipping the E2 area.  Each pointer gives a new entry in
 *   the returned list, the name being taken from the blockNames[] array and with
 *     fileData.versionNum and fileData.zName unset;
 *     fileData.version = hex value given in the block concerned;
 *     fileData.address = address of block within the remote's flash memory. 
 */
    private LinkedHashMap< String, FileData > setFileContentData( Hex appImg )
    {
      // Only applies to type DIGITAL and AVL remotes
      if ( remoteType == RemoteType.XZITE )
      {
        return null;
      }
      LinkedHashMap< String, FileData > additions = new LinkedHashMap< String, FileData >();
      int sigOffset = intFromHex( appImg, 2, false );
      if ( remoteType == RemoteType.AVL )
      {
        // For HCS08 processor, need to add the length of the RAM block $1800-$182B
        sigOffset += 0x2C;
      }
      int numEntries = appImg.getData()[ sigOffset + 0x2A ] & 0xFF;
      byte[] data = appImg.toByteArray();
      int start = addrFromBytes( data, sigOffset + 0x2B );
      for ( int i = 0; i < numEntries; i++ )
      {
        if ( i == 2 )
        {
          continue;  // skip E2 area
        }
        FileData fd = new FileData();
        int addr = addrFromBytes( data, sigOffset + 0x2B + i * addrSize );
        if ( addr < 0 )
        {
          addr = intFromHex( appImg, ( addr & 0x7FFFFFFF ) - start, false );
        }
        fd.address=addr;
        fd.version = appImg.subHex( addr - start + 6, 6 );
        additions.put( blockNames[ i ], fd );
        if ( i == 3 )
        {
          int appLangCode = fd.version.getData()[ 3 ] - 0x30;
          String appLangName = "alf" + appLangCode + ".img";
          if ( !upgradeData.containsKey( appLangName ) )
          {
            additions.put( appLangName, fd );
          }
        }
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
    
    /**
     *   verifyFileVersions(...) tests each file in the upgrade to see if the version number
     *   given in the file name agrees with that in the file data, the former being taken
     *   from the upgradeData map.  If all agrees, then for DIGITAL and AVL remotes it adds
     *   additional entries to the upgradeData map corresponding to the various data blocks
     *   in the app.img file, these entries being extracted by setFileContentData(...).
     */
    private boolean verifyFileVersions( File sysFile )
    {
      LinkedHashMap< String, FileData > additions = null;
      try
      {
        if ( sysFile.getName().endsWith( ".sys" ) )
        {
          ZipFile zipIn = new ZipFile( sysFile );
          // System.err.println( "File versions from upgrade files:" );
          for ( String name : upgradeData.keySet() )
          {         
            FileData fd = upgradeData.get( name );
            if ( fd.zName == null )
            {
              continue;
            }
            // System.err.println( "zName = " + fd.zName );
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
            else if ( remoteType == RemoteType.DIGITAL || remoteType == RemoteType.AVL )
            {
              int len = name.equals( "app.img" ) ? ( int )entry.getSize() : 12;
              byte[] data = RemoteMaster.readBinary( zip, len, true );
              hex = new Hex( data );
              RemoteConfiguration.decryptObjcode( hex );
              fileVersion = hex.subHex( 6, 6 );
            }

            // System.err.println( fileVersion + "  " + name );
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
        else if ( sysFile.getName().endsWith( ".bin" ) )
        {
          // RemoteType.DIGITAL and RemoteType.AVL only
          FileInputStream fileIn = new FileInputStream( sysFile );
          int len = ( int )sysFile.length();
          byte[] data = RemoteMaster.readBinary( fileIn, len, true );
          fileIn.close();
          Hex hex = new Hex( data );
          Hex fileVersion = hex.subHex( 6, 6 );
          FileData fd = upgradeData.get( "app.img" );
          if ( !fileVersion.equals( fd.version ) )
          {
            return false;
          };
          additions = setFileContentData( hex );
          FileData alfFile = additions.get( blockNames[ 3 ] );
          FileData alfImg = null;
          for ( String s : upgradeData.keySet() )
          {
            // Handle the fact that the file name in upgradeData has a language
            // identifier digit postfixed to name body
            if ( s.startsWith( "alf" ) )
            {
              alfImg = upgradeData.get( s );
              break;
            }
          }
          if ( alfImg != null && !alfImg.version.equals( alfFile.version ) )
          {
            return false;
          }
        }
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
    
    private final static List< String > avlSysNames = Arrays.asList(
        "app.img" );
    
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

  
