package com.hifiremote.jp1.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import com.codeminders.hidapi.HIDManager;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.Remote;
import com.hifiremote.jp1.RemoteManager;
import com.hifiremote.jp1.RemoteMaster;
import com.hifiremote.jp1.RemoteMaster.NegativeDefaultButtonJOptionPane;
 
public class CommHID extends IO 
{
	/*
	 *  Summary of XSight Touch packet types, hex numbering.  Packets are all 64 bytes,
	 *  the first byte being the packet type and the last two being a CRC checksum.
	 *  The structure of the remaining bytes depends on packet type and its use.
	 *    01  General packet carrying data in response to a specific request or
	 *          sent as an acknowledgement of receipt.
	 *    04  Request firmware version data (for all 33 firmware files).  Response
	 *          is an 01 packet giving number of files, followed by an 05 packet for
	 *          each file that needs acknowledgement with an 01 packet.
	 *    05  Version data for specific firmware file.
	 *    12  Request content of a named file.  Response is an 01 packet giving file
	 *          length, followed by a series of type 14 packets giving file data as
	 *          series of blocks.  Each type 14 packet needs acknowledgement with a
	 *          type 01 packet.
	 *    13  Write content of a named file.  Packet carries file name and length.
	 *          A type 01 packet is received as acknowledgement and the file data is 
	 *          then sent as a series of type 14 packets, each of which gets a type
	 *          01 packet in acknowledgement.
	 *    14  File content received or sent following type 12 or 13 packets.
	 *    15  Delete named file.  Response is a type 01 packet acknowledging success
	 *          or responding that the file is absent.
	 *    19  Request length of named file.  Response is a type 01 packet giving length
	 *          or responding that the file is absent.
	 *    20  Request for the remote to enter update mode.  It carries no data
	 *          and the response is a type 01 packet with no data, but the remote
	 *          then disconnects its USB port followed by a reconnection.
	 *    27  Request a 6-byte value that is possibly the complement of a hex serial
	 *          number, returned in a type 01 packet.  There are no known side effects. 
	 */
  
  HIDManager hid_mgr;
	HIDDevice devHID;
	Remote remote = null;
	int thisPID;
	String signature;
	int E2address;
	int E2size;
	HIDDeviceInfo[] HIDinfo = new HIDDeviceInfo[10];
	HIDDeviceInfo HIDdevice = null;
	byte outReport[] = new byte[65];  // Note the asymmetry:  writes need outReport[0] to be an index 
	byte inReport[] = new byte[64];   // reads don't return the index byte
	byte dataRead[] = new byte[0x420];
	byte ssdIn[] = new byte[62];
	byte ssdOut[] = new byte[62];
	int interfaceType = -1;
	int firmwareFileCount = 0;
	boolean upgradeSuccess = true;
	LinkedHashMap< String, Hex > firmwareFileVersions = new LinkedHashMap< String, Hex >();
	LinkedHashMap< String, Hex > upgradeFileVersions = new LinkedHashMap< String, Hex >();

	private int runningTotal = 0;
	
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
	        if (HIDinfo[i].getVendor_id() == 0x06E7) {
            HIDdevice = HIDinfo[i];
	        }
	      }
	      if ( HIDdevice != null )
	      {
	        String manString = HIDdevice.getManufacturer_string();
	        String prodString = HIDdevice.getProduct_string();
	        thisPID = HIDdevice.getProduct_id();
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

	byte jp12ComputeCheckSum( byte[] data, int start, int length ) {
	  int sum = 0;
		int end = start + length;
		for (int i = start; i < end; i++)  {
			sum ^= (int)data[i] & 0xFF;
		}
		return (byte) sum;
	}

	void assembleMAXQreadAddress( int address, int blockLength, byte[] cmdBuff) {   
		cmdBuff[0] = 0x00;  //packet length
		cmdBuff[1] = 0x08;  //packet length
		cmdBuff[2] = 0x01;  //Read command
		cmdBuff[3] = (byte) ((address >> 24) & 0xff);
		cmdBuff[4] = (byte) ((address >> 16) & 0xff);
		cmdBuff[5] = (byte) ((address >>  8) & 0xff);
		cmdBuff[6] = (byte) (address & 0xff);
		cmdBuff[7] = (byte) ((blockLength >>  8) & 0xff);
		cmdBuff[8] = (byte) (blockLength & 0xff);
		cmdBuff[9] = jp12ComputeCheckSum(cmdBuff, 0, 9);
	}
	
	boolean eraseMAXQ_Lite( int startAddress, int endAddress ){
		byte[] cmdBuff = new byte[12];
		cmdBuff[0] = (byte) 0x00;  //packet length
		cmdBuff[1] = (byte) 0x0A;  //packet length
		cmdBuff[2] = (byte) 0x03;  //erase command
		cmdBuff[3] = (byte)( (startAddress >> 24) & 0xff);
		cmdBuff[4] = (byte)((startAddress >> 16) & 0xff);
		cmdBuff[5] = (byte)((startAddress >>  8) & 0xff);
		cmdBuff[6] = (byte)(startAddress & 0xff);
		cmdBuff[7] = (byte)((endAddress >> 24) & 0xff);
		cmdBuff[8] = (byte)((endAddress >> 16) & 0xff);
		cmdBuff[9] = (byte)((endAddress >>  8) & 0xff);
		cmdBuff[10] = (byte)(endAddress & 0xff);
		cmdBuff[11] = jp12ComputeCheckSum(cmdBuff, 0, 11);
		System.arraycopy(cmdBuff, 0, outReport, 1, cmdBuff.length);
		try {
			devHID.write(outReport);
		} catch (Exception e) {
			return false;
		}
		if ( !readMAXQreport() || (dataRead[2] != 0) ) //Wait for remote to respond and check for error
			return false;
		return true;
	}

	boolean writeMAXQ_Lite_Block( int address, byte[] buffer, int blockLength ) {
			byte[] cmdBuff = new byte[7]; 
			int pkgLen;
			if (blockLength > 0x38) 
				return false;
			pkgLen = blockLength + 6;
			cmdBuff[0] = (byte) (pkgLen >> 8);  //packet length
			cmdBuff[1] = (byte) (pkgLen & 0xFF);  //packet length
			cmdBuff[2] = (byte) 0x02;  //write command
			cmdBuff[3] = (byte) ((address >> 24) & 0xff);
			cmdBuff[4] = (byte) ((address >> 16) & 0xff);
			cmdBuff[5] = (byte) ((address >>  8) & 0xff);
			cmdBuff[6] = (byte) (address & 0xff);
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

	boolean writeMAXQcmdReport(byte [] cmdBuff)  {
		  System.arraycopy(cmdBuff, 0, outReport, 1, cmdBuff.length);
		  try {
		    devHID.write(outReport);
		  } catch (Exception e) {
		    return false;
		  }
		  return true;
	}
	
	boolean readMAXQreport()  {
		try {
			devHID.readTimeout(inReport, 3000);
			System.arraycopy(inReport, 0, dataRead, 0, 64);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	boolean MAXQ_ReopenRemote()
	{
	  byte[] cmdBuff = {(byte)0x00, (byte)0x02, (byte)0x51, (byte)0x53 };
	  if ( !writeMAXQcmdReport(cmdBuff) )
	  {
	    return false;
	  }
	  if ( !readMAXQreport() || dataRead[0] != 0 )
	  {
	    return false;
	  }
	  return true;
	}
	
	boolean MAXQ_USB_getInfoAndSig()  {
		byte[] cmdBuff = {(byte)0x00, (byte)0x02, (byte)0x50, (byte)0x52};
		int sigAdr, E2StartPtr, E2EndPtr, temp;
		if (!writeMAXQcmdReport(cmdBuff))
			return false;
		if (!readMAXQreport() || (dataRead[0] != 0) || (dataRead[1] != 8) || (dataRead[2] != 0) )  
			return false;
		sigAdr = ((dataRead[6] & 0xFF) << 16) + ((dataRead[7] & 0xFF) << 8) + (dataRead[8] & 0xFF);
		if (readMAXQ_Lite(sigAdr, dataRead, 0x54) != 0x54)
			return false;
		try {
			signature = new String(dataRead, 6,6, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		E2StartPtr = ((dataRead[52] & 0xFF) << 16) + ((dataRead[53] & 0xFF) << 8) + (dataRead[54] & 0xFF);
		E2EndPtr   = ((dataRead[56] & 0xFF) << 16) + ((dataRead[57] & 0xFF) << 8) + (dataRead[58] & 0xFF);
		if (readMAXQ_Lite(E2StartPtr, dataRead, 0x04 )  != 0x04)
			return false;
		E2address = ((dataRead[0] & 0xFF) << 24) + ((dataRead[1] & 0xFF) << 16) + ((dataRead[2] & 0xFF) << 8) + (dataRead[3] & 0xFF);
		if(readMAXQ_Lite(E2EndPtr,  dataRead, 0x04 ) != 0x04)
			return false;
		temp = ((dataRead[0] & 0xFF) << 24) + ((dataRead[1] & 0xFF) << 16) + ((dataRead[2] & 0xFF) << 8) + (dataRead[3] & 0xFF);
		E2size = temp - E2address;
		return true;
	}
	
	@Override
	public String openRemote(String portName) {
	  try  
	  {
	    devHID = null;
	    if ( HIDdevice != null )
	    {
	      devHID = HIDdevice.open();
	    }
	    if ( devHID == null )
	    {
	      getPIDofAttachedRemote();
	      devHID = HIDdevice.open();
	    }
	    if ( devHID == null )
	    {
	      return "";
	    }
//	    devHID = hid_mgr.openById(0x06E7, thisPID, null);

	    devHID.enableBlocking();
	    List< Remote > remotes = RemoteManager.getRemoteManager().findRemoteBySignature( getRemoteSignature() );
	    if ( remotes.size() > 0 )
	    {
	      remote = remotes.get( 0 );
	      remote.load();
	      interfaceType = remote.isSSD() ? 0x201 : 0x106;
	    }
	    else
	    {
	      // Unknown remote.  Treat for testing as XSight Lite style.
	      remote = null;
	      interfaceType = 0x106;
	      E2address = 0;   // This will be changed on testing
	      E2size = 0x400;  // Seek to read 1K during testing
	      return "HID";
	      
	      
//	      String title = "Unknown remote";
//	      String message = "RMIR has found a remote that appears to be of the XSight type with\n"
//	          + "signature " + getRemoteSignature() + ".  There is no RDF for this remote and without\n"
//	          + "one, RMIR is unable to open it.  You may wish to post a message in the\n"
//	          + "JP1 forums to seek help in creating an RDF for it.";
//	      JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
//	      return "";
	    }
	    if ( interfaceType == 0x106 )
	    {
	      if ( portName != null && portName.equals( "UPG" ) )
	      {
	        return MAXQ_ReopenRemote() ? "UPG" : "";
	      }
	      MAXQ_ReopenRemote();
	      waitForMillis( 200 );
	      if ( MAXQ_USB_getInfoAndSig() && E2address > 0 )
	      {
          System.err.println( "GetInfoAndSig succeeded" );
        }
	      else
	      {
	        System.err.println( "GetInfoAndSig failed, taking data from RDF" );
	        E2address = remote.getBaseAddress();
	        E2size = remote.getEepromSize();
	      } 
	    }
	    else
	    {
	      E2address = remote.getBaseAddress();
	      E2size = remote.getEepromSize();
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
	  return "HID";
	}

	@Override
	public void closeRemote() {
	  try  {
	    devHID.close();
	  } catch (Exception e) {

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
	  return ( remote != null ) && remote.isSSD();
	}
	
	@Override
	public int getInterfaceType() {
	  return interfaceType;
	}
	
	private String getTouchVersion()
	{
	  short[] v = firmwareFileVersions.get( "MCUFirmware" ).getData();
	  return v == null ? "Unknown" : "" + v[5] + "." + v[4] + "." + v[2];
	}
	
	private String getMAXQ_LiteVersion()
	{
	  byte[] cmdBuff = {(byte)0x00, (byte)0x03, (byte)0x53, (byte)0x00, (byte)0x50 };
    if ( !writeMAXQcmdReport(cmdBuff) )
    {
      return "Unknown";
    }
    if ( !readMAXQreport() || dataRead[0] != 0 )
    {
      return "Unknown";
    }
    char[] c = new char[6];
    for ( int i = 0; i < 6; i++ )
    {
      c[i] = ( char )inReport[ i + 3 ];
    }
    String v = new String( c );
    return v.substring( 0, 4 ) + "." + v.substring( 4 );
	}
	
	private int testRemote( byte[] buffer, int length )
	{
	  String title = "Unknown remote";
	  String message = null;
	  int numRead = 0;
	  System.err.println();
	  System.err.println( "Starting diagnostics for unknown XSight remote" );
	  if ( getVersionsFromRemote( false ) )
	  {
	    message = "RMIR has found an XSight Touch style of remote with the following data:"
	        + "\n    Signature = " + getRemoteSignature()
	        + "\n    Firmware version = " + getTouchVersion();
	  }
	  else
	  {
	    System.err.println( "Not a Touch-style remote.  Testing for Lite-style.");
	    waitForTouchReconnection();
	    MAXQ_ReopenRemote();
      waitForMillis( 200 );
      if ( MAXQ_USB_getInfoAndSig() && E2address > 0 )
      {
        message = "RMIR has found an XSight Lite style of remote with the following data:"
            + "\n    Signature = " + getRemoteSignature()
            + "\n    Firmware version = " + getMAXQ_LiteVersion()
            + "\n    EEPROM address = $" + Integer.toHexString( E2address )
            + "\n    EEPROM size = $" + Integer.toHexString( E2size );
        numRead = readMAXQ_Lite( E2address, buffer, length );
      }
      else
      {
        for ( int address = 0x00000; address < 0x30000; address += 0x400 )
        {
          waitForMillis( 100 );
          numRead = readMAXQ_Lite( address, buffer, length );
          if ( numRead > 0 )
          {
            message = "RMIR has found a remote possibly of the XSight Lite type the following data:"
                + "\n    Signature = " + getRemoteSignature()
                + "\n    Possible EEPROM address = $" + Integer.toHexString( address );
            break;
          }
        }
        if ( numRead <= 0 )
        {
          message = "RMIR has found a remote that appears to be of the XSight type with\n"
              + "signature " + getRemoteSignature() + " but is unable to read it." ;
        }
      }
	  }
	  System.err.println( message );
	  if ( numRead > 0 )
	  {
	    System.err.println( "The data read from the start of this area is:" );
	    short[] e2Data = new short[ numRead ];
	    for ( int i = 0; i < numRead; i++ )
	    {
	      e2Data[ i ] = ( short )( buffer[ i ] & 0xFF );
	    }
	    System.err.println( Hex.toString( e2Data, 32 ) );
	  }
	  message += "\n\nYou may wish to post a message in the JP1 forums to seek help in\n"
	             + "creating an RDF for this remote.  If so, please post the rmaster.err\n"
	             + "file that you will find in the RMIR installation folder and include a\n"
	             + "link to that file in your message.";
	  JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
	  return numRead;
	}
	
	int readMAXQ_Lite( int address, byte[] buffer, int length ) {  //MAXQ
		System.err.println( "Starting MAXQ read of $" + Integer.toHexString( length ) + " bytes at $" + Integer.toHexString( address ) );
	  byte[] cmdBuff = new byte[10];
		assembleMAXQreadAddress(address, length, cmdBuff);
		int numToRead = length + 4;  // total packet  length plus error byte and checksum
		if (!writeMAXQcmdReport(cmdBuff))
		{
		  System.err.println( "Failed to initiate MAXQ read" );
		  return -1;
		}
		int numReports = 1 + numToRead/64;
		int dataIdx = 0;
		int reportOffset = 3;  //First report has length and error bytes
		for (int i=0; i < numReports; i++) {
			try 
			{
			  Arrays.fill( inReport, ( byte )0xFF );
			  int numRead = devHID.readTimeout(inReport, 3000);
			  if ( inReport[ 0 ] == 0xFF )
			  {
			    System.err.println( "Read attempt timed out for MAXQ report " + i + " of " + numReports );
          return -1;
			  }
			  else if ( numRead != 64 )
				{
				  System.err.println( "Incomplete read of MAXQ report " + i + " of " + numReports + ", " + numRead + " bytes of 64 read" );
				  return -1;
				}
				if ( i == 0 && inReport[ 2 ] != 0 )
				{
				  System.err.println( "Read of $" + Integer.toHexString( length ) + " bytes at $" + Integer.toHexString( address )
				      + " returned error code " + inReport[ 2 ] );
				  return -1;
				}
				System.arraycopy(inReport,reportOffset, buffer, dataIdx, 
				                      Math.min(length - dataIdx, 64 - reportOffset));
			} 
			catch (Exception e) 
			{
			  System.err.println( "Failed at read of MAXQ report " + i + " of " + numReports );
			  return -1;
			}
			dataIdx += 64 - reportOffset;
			reportOffset = 0;
		}
		return length;
	}
	
	int writeMAXQ_Lite( int address,  byte[] buffer, int length )  {
		int writeBlockSize = 0x38;
		int erasePageSize = 0x200;
		int offset, endAdr;
		int blockLength = writeBlockSize;
		byte tempBuf[] = new byte[65];
		if ((address < E2address) || (address + length > E2address + E2size) )
			return -1;
		if ((length % erasePageSize) != 0)
			return -1;
		endAdr = address + length - 1;
		eraseMAXQ_Lite( address, endAdr );
		offset = 0;
		do {
			if (( offset + blockLength ) > length )
				blockLength = length - offset;
			System.arraycopy(buffer, offset, tempBuf, 0, blockLength);
			if ( !writeMAXQ_Lite_Block( address + offset, tempBuf, blockLength ))
				return -1;
			if ( !readMAXQreport() || (dataRead[2] != 0) ) //Wait for remote to respond and check for error
				return -1;
			offset += blockLength;
		}  while ( offset < length ); 
		return offset;
	}
	
	public int readRemote( int address, byte[] buffer, int length ) 
	{
		int bytesRead = -1;
		if ( remote == null )
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
		  }
		  else
		  {
		    return -1;
		  }
		}
		else if ( interfaceType == 0x106 )
		{
			bytesRead = readMAXQ_Lite(address,buffer, length);
		}
		else if ( interfaceType == 0x201 )
		{
		  bytesRead = readTouch( buffer );
		}
    return bytesRead;
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
	  int mask = 0xFFF;
	  for ( int index = 0; index < Remote.userFilenames.length; index++ )
	  {
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
      writeTouchUSBReport( ssdOut, 62 );
      System.err.println( "Header packet sent" );
      readTouchUSBReport(ssdIn);

      while ( pos < end )
      {
        if ( ssdInCheck() < 0 )
        {
          System.err.println( "Error: terminating at position " + Integer.toHexString( pos ) );
          return pos;
        }
        int size = Math.min( end - pos, 56 );
        Arrays.fill( ssdOut, ( byte )0 );
        ssdOut[ 0 ] = 0x14;
        ssdOut[ 2 ] = ( byte )( count & 0xFF );
        ssdOut[ 3 ] = ( byte )( ( count >> 8 ) & 0xFF );
        ssdOut[ 4 ] = ( byte )size;
        System.arraycopy( buffer, pos, ssdOut, 6, size );
        pos += size;
        writeTouchUSBReport( ssdOut, 62 );
        count++;
        System.err.println( "Packet " + count + " sent" );
        readTouchUSBReport(ssdIn);
      }
    }
	  if ( RemoteMaster.admin )
    {
      System.err.println( "Write dialog ends");
    }
	  return buffer.length;
	}
	
	private boolean writeSystemFiles( String zipName, List< String > names )
	{
    byte[] data = null;
    try
    {
      File inputDir = new File( RemoteMaster.getWorkDir(), "XSight" );
      ZipFile zipfile = new ZipFile( new File( inputDir, zipName ) );
      Enumeration< ? extends ZipEntry > zipEnum = zipfile.entries();
      while ( zipEnum.hasMoreElements() ) 
      { 
         ZipEntry entry = ( ZipEntry ) zipEnum.nextElement(); 
         String name = entry.getName();
         if ( !names.contains( name ) )
         {
           continue;
         }
         long length = entry.getSize();
         if ( length < 0 )
         {
           System.err.println( "File " + name + " has unknown length and could not be updated" );
           upgradeSuccess = false;
           continue;
         }
         InputStream zip = zipfile.getInputStream( entry );
         data = RemoteMaster.readBinary( zip, ( int )length );
         System.err.println( "Writing file " + name + " to remote" );
         if ( !writeSystemFile( name, data ) )
         {
           System.err.println( "Failed to write system file " + name );
           upgradeSuccess = false;
         }

         // FOR TESTING ONLY (with the above writeSystemFile() commented out:
//         OutputStream output = null;
//         File outputDir = new File( RemoteMaster.getWorkDir(), "XSight" );
//
//         try 
//         {
//           output = new FileOutputStream( new File( outputDir, name + "out"  ), false );
//           output.write( data );
//         }
//         catch(Exception ex){
//           System.err.println( "Unable to open file " + name );
//         }
         // END TESTING
      }
      if ( zipfile != null )
        zipfile.close();
    }
    catch ( Exception e )
    {
      System.err.println( e );
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
	  writeTouchUSBReport( ssdOut, 62 );
	  System.err.println( "Header packet sent" );
	  readTouchUSBReport(ssdIn);

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
	    pos += size;
	    writeTouchUSBReport( ssdOut, 62 );
	    count++;
	    System.err.println( "Packet " + count + " sent" );
	    readTouchUSBReport(ssdIn);
	  }
	  System.err.println( "Bytes written to " + name + ": " + pos );
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
    long waitStart = Calendar.getInstance().getTimeInMillis();	  
	  try
	  {
	    devHID.close();
	  }
	  catch ( Exception e )
	  {
	    System.err.println( "Error in closing device after disconnection" );
	  }
	  
	  devHID = null;
	  waitForMillis( 2000 );
	  while ( devHID == null )
	  {
	    long delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
      if ( delay > 60000 )
      {
        System.err.println( "Reconnection maximum wait exceeded" );
        return false;
      }
	    try
	    {
	      devHID = HIDdevice.open();
	      if ( devHID != null )
	      {
	        System.err.println( "Reopened device after wait of " + delay + "ms" );
	        return true;
	      }
	      else
	      {
	        System.err.println( "Reopen attempt gave null device after wait of " + delay + "ms" );
	      }
	    }
	    catch ( Exception e )
	    {
	      System.err.println( "Error in reopen attempt" );
	    }
	    waitForMillis( 500 );
	  }
	  return false;
	}
	


//	  boolean wasAbsent = false;
//	  while ( true )
//	  {
//	    long delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
//	    if ( delay > 60000 )
//	    {
//	      System.err.println( "Reconnection maximum wait exceeded" );
//	      return false;
//	    }
//	    boolean present = false;
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
//	}
	
	/**
	 *  return array elements are:
   *    int[0] = 1 if needs upgrade;
   *    int[1] = 1 if needs MCU upgrade;
   *    int[2] = upgrade type, 0 = no change, 1 = upgrade, -1 = downgrade, -2 = up/down inconsistent
	 */
	int[] testForUpgrade( List< String > changed, List< String > newFiles, List< String > forDeletion )
	{
	  int[] out = new int[]{ 0, 0, 0 };
	  int upgradeType = 0;
	  for ( String name : upgradeFileVersions.keySet() )
	  {
	    Hex currentVersion = firmwareFileVersions.get( name );
	    Hex upgradeVersion = upgradeFileVersions.get( name );
	    int diff = 0;
	    if ( currentVersion != null )
	    {
	      for ( int i = 3; i >= 0; i-- )
	      {
	        diff = upgradeVersion.getData()[ i ] - currentVersion.getData()[ i + 2 ];
	        if ( diff != 0 )
	        {
	          changed.add( name );
	          break;
	        }
	      }
	    }
	    else
	    {
	      newFiles.add( name );
	      diff = 1;
	    }
	    if ( diff != 0 )
	    {
	      out[ 0 ] = 1;
	      if ( name.startsWith( "lang" ) || name.startsWith( "Splash" ) )
	      {
	        out[ 1 ] = 1;
	      }
	      upgradeType = diff > 0 && upgradeType >= 0 ? 1
	          : diff > 0 && upgradeType < 0 ? -2
	              : diff < 0 && upgradeType > 0 ? -2
	                  : diff < 0 && upgradeType > -2 ? -1 : -2;
	    }
	  }
	  for ( String name : firmwareFileVersions.keySet() )
	  {
	    // Don't add MCUFirmware and BlasterFirmware to list of files for deletion
	    if ( !upgradeFileVersions.keySet().contains( name ) && name.indexOf( "." ) > 0 )
	    {
	      out[ 0 ] = 1;
	      forDeletion.add( name );
	    }
	  }
	  
	  out[ 2 ] = upgradeType;
	  return out;
	}
	
	int readTouch( byte[] buffer )
	{
	  if ( RemoteMaster.admin )
	  {
	    System.err.println( "Read dialog starts:");
	  }
	  int status = 0;
	  
	  // Read firmware file versions from remote
	  if ( !getVersionsFromRemote( true ) 
	      || !getVersionsFromRemote( false ) )
	  {
	    return 0;
	  }
	  
	  // Test for upgrade and perform it if required
    List< String > changed = new ArrayList< String >();
    List< String > newFiles = new ArrayList< String >();
    List< String > forDeletion = new ArrayList< String >();
    String zipName = "Upg" + String.format( "%04X", thisPID ) + ".zip";
    if ( getVersionsFromUpgrade( zipName ) )
    {
      int[] upgNeeds = testForUpgrade( changed, newFiles, forDeletion );
      
      // TESTING
//      upgNeeds = new int[]{1,1,1};
//      changed.clear();
//      changed = Arrays.asList( "lang.fr" );
//      newFiles.clear();
//      forDeletion.clear();
//      forDeletion = Arrays.asList( "Pacific.rgn" );
      // END TESTING
      
      if ( upgNeeds[ 0 ] > 0 )
      {
        String message = 
            "There is revised firmware available for this remote.  Version numbers\n"
                + "show this to be " 
                + ( upgNeeds[ 2 ] == 1 ? "an upgrade." : upgNeeds[ 2 ] == -1 ? "a downgrade."
                    : "in part an upgrade, in part a downgrade." ) + "\n\n"
                    + "Before installing this revision, you are strongly advised to save\n"
                    + "the existing firmware so that it can be restored if needed.  Note\n"
                    + "that this can take a minute or two; a message is displayed when\n"
                    + "the save process has finished.\n\n"
                    + "What action do you want to take?";
        String title = "Firmware revision";
        String[] buttons = new String[]{
            "<html>Save existing<br>firmware</html>",
            "<html>Install revised<br>firmware</html>",
            "<html>Continue normal<br>download</html>" };

        int response = JOptionPane.showOptionDialog( null, message, title, JOptionPane.DEFAULT_OPTION, 
            JOptionPane.PLAIN_MESSAGE, null, buttons, buttons[ 0 ] );

        if ( response == 0 )
        {
          if ( !readSystemFiles() )
          {
            return 0;
          }
        }
        else if ( response == 1 )
        {
          System.err.println( "Proceeding with firmware revision" );
          upgradeSuccess = true;
          if ( upgNeeds[ 1 ] > 0 )
          {
            if ( !getVersionsFromRemote( true ) 
                || !getVersionsFromRemote( false )
                || !getVersionsFromRemote( false ) )
            {
              return 0;
            }
            waitForMillis( 7000 );
            // Put remote into update mode with type 0x20 packet.
            // Change packet type to 0x27 for testing without entering update mode.
            writeTouchUSBReport( new byte[]{0x20}, 1 );
            if ( readTouchUSBReport( ssdIn ) < 0 )
            {
              System.err.println( "Request to disconnect failed" );
              message = "Upgrade failed.  Request to disconnect failed.\n"
                  + "Aborting download";
              JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
              return 0;
            }
          }

          boolean connectOK = true;
          if ( ( upgNeeds[ 1 ] == 0 || ( connectOK = waitForTouchReconnection() ) == true ) && 
              waitForMillis( upgNeeds[ 1 ] == 1 ? 60000 : 0 ) &&
              writeSystemFiles( zipName, changed ) &&
              waitForMillis( 300 ) &&
              getVersionsFromRemote( true ) &&
              writeSystemFiles( zipName, newFiles ) &&
              waitForMillis( 300 ) &&
              getVersionsFromRemote( false ) &&
              deleteSystemFiles( forDeletion ) &&
              waitForMillis( 300 ) &&
              getVersionsFromRemote( false ) &&
              upgradeSuccess )
          {
            message = "Upgrade succeeded.  Continuing with normal download.";
            JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE );
          }
          else
          {
            message = "Upgrade failed. ";
            message += connectOK ? "Unable to write all required files." : "Unable to enter update mode.";
            message += "\nAborting download";
            JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
            return 0;
          } 
        } 
      }
    }
    
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
    int ndx = 4;
    runningTotal = ndx;
    if ( RemoteMaster.admin )
    {
      System.err.println( "Read user files:");
    }
    for ( int n = 0; n < Remote.userFilenames.length; n++ )
    {
      String name = Remote.userFilenames[ n ];
      byte[] data = readTouchFileBytes( name );
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
	  byte[] bBuffer = readTouchFileBytes( name );
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
	
	private byte[] readTouchFileBytes( String name )
	{
    Arrays.fill( ssdOut, ( byte )0 );
    ssdOut[ 0 ] = 0x12;
    ssdOut[ 2 ] = ( byte )name.length();
    for ( int i = 0; i < name.length(); i++ )
    {
      ssdOut[ 3 + i ] = ( byte )name.charAt( i );
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
    // Get file length as integer from 3-byte little-endian data
    int count = ( ssdIn[ 3 ] & 0xFF ) + 0x100 * ( ssdIn[ 4 ] & 0xFF )+ 0x10000 * ( ssdIn[ 5 ] & 0xFF );
    int total = 0;
    ssdOut[ 0 ] = 1;
    ssdOut[ 2 ] = 0;
    int ndx = 0;
    byte[] buffer = new byte[ count ];
    while ( total < count )
    {
      // Read next segment of file data
      if ( readTouchUSBReport( ssdIn ) < 0 )
      {
        System.err.println( "Read error before end of file \"" + name + "\"" );
        return null;
      }
      int len = ssdIn[ 4 ];
      total += len;
      System.arraycopy( ssdIn, 6, buffer, ndx, len );
      ndx += len;
      // Set packet serial in acknowledgement.
      ssdOut[ 1 ] = ssdIn[ 1 ];
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
	  return readTouchFileBytes( name );
	}
	
	private boolean readSystemFiles()
	{
	  runningTotal = -1;
	  System.err.println();
	  System.err.println( "Saving system files to XSight subfolder of installation folder:" );
	  OutputStream output = null;
	  ZipOutputStream zip = null;
    File outputDir = new File( RemoteMaster.getWorkDir(), "XSight" );
    boolean result = true;
    String zipName = "Sys" + String.format( "%04X", thisPID ) + ".zip";
    
    try 
    {
      output = new FileOutputStream( new File( outputDir, zipName  ), false );
      zip = new ZipOutputStream( output );
      for ( String name : firmwareFileVersions.keySet() )
      {
        if ( name.indexOf( "." ) > 0 )
        {
          byte[] filedata = readTouchFileBytes( name );
          if ( filedata == null || filedata.length == 0 )
          {
            continue;
          }
          System.err.println( "  Saving " + name );
          zip.putNextEntry( new ZipEntry( name ) );
          zip.write( filedata );
        }
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
	
	private boolean getVersionsFromUpgrade( String zipName )
	{
	  boolean result = true;
	  File inputDir = new File( RemoteMaster.getWorkDir(), "XSight" );
	  if ( !inputDir.exists() )
    {
      return false;
    }

	  FileInputStream input = null;
	  ZipInputStream zip = null;
	  int count = 0;
    try
    {
      File file = new File( inputDir, zipName  );
      if ( !file.exists() )
      {
        return false;
      }
      input = new FileInputStream( file );
      zip = new ZipInputStream( input );
      ZipEntry entry = null;
      System.err.println( "Version data from " + zipName + ":" );
      while ( ( entry = zip.getNextEntry()) != null )
      {
        byte[] data = new byte[ 4 ];
        String name = entry.getName();
        int len = zip.read( data );
        if ( len == data.length )
        {
          count++;
          Hex hex = new Hex( data.length );
          for ( int i = 0; i < data.length; i++ )
          {
            hex.set( ( short )( data[ i ] & 0xFF ), i );
          }
          upgradeFileVersions.put( name, hex );
          System.err.println( "  " + name + " : " + hex.toString() );
        }
        else
        {
          System.err.println( "  Failed to read version data of file " + name );
          result = false;
        }
      }
      input.close();
      zip.close();
      System.err.println( "Versions read from " + count + " system files" );
    }
    catch ( Exception e ) 
    { 
      System.err.println( e );
      result = false; 
    }
    return result;
	}
	
	private boolean getVersionsFromRemote( boolean getSerial )
	{
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
    System.err.println( "Hex serial (?) number: " + serial );
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
//	  if ( !absent )
//	  {
	    firmwareFileVersions.put( name, hex );
//	  }
	  System.err.println( "  " + name + " : " + hex.toString() );
	}
	
	public int writeRemote( int address, byte[] buffer, int length ) {  //if Touch, must be 62 bytes or less
		int bytesWritten = -1;
		if ( interfaceType == 0x106 )
			bytesWritten = writeMAXQ_Lite(address, buffer, length);
		else if ( interfaceType == 0x201 )
		  bytesWritten = writeTouch( buffer );
		return bytesWritten;
	}
	
	int readTouchUSBReport(byte[] buffer) { 
	  int bytesRead = -1;
		try {
		  Arrays.fill( inReport, ( byte )0xFF );
		  bytesRead = devHID.readTimeout(inReport, 3000);
		  if ( inReport[ 0 ] == ( byte )0xFF )
		  {
		    return -2;  // signifies timed out as 0xFF is not a known packet type
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
    
    private final static String libraryName = "hidapi";
   
}

	
