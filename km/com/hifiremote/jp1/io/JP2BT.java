package com.hifiremote.jp1.io;

import com.fazecast.jSerialComm.SerialPort;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JOptionPane;

import org.thingml.bglib.BDAddr;
import org.thingml.bglib.BGAPI;
import org.thingml.bglib.BGAPIDefaultListener;
import org.thingml.bglib.BGAPIListener;
import org.thingml.bglib.BGAPIPacket;
import org.thingml.bglib.BGAPITransport;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.RemoteMaster;
import com.hifiremote.jp1.RemoteMaster.Use;
import com.hifiremote.jp1.io.UEIPacket.CmdPacket;

public class JP2BT extends IO
{
  /* *****************************************************************************
   * Classes BLEService and BLEAttribute.
   * Copied from org.thingml.bglib.gui by <franck.fleurey@sintef.no>
   *   under GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007,
   *   obtainable at http://www.gnu.org/licenses/lgpl-3.0.txt
   *******************************************************************************/
  
  public static class BLEService 
  {
    public static Hashtable<String, String> profiles = new Hashtable<String, String>();
    static {
      profiles.put("0x180A", "");
    }

    protected int start, end;
    protected byte[] uuid;

    protected ArrayList<BLEAttribute> attributes = new ArrayList<BLEAttribute>();

    public BLEService(byte[] uuid, int start, int end) {
      this.uuid = uuid;
      this.start = start;
      this.end = end;
    }

    public int getStart() {
      return start;
    }

    public int getEnd() {
      return end;
    }

    public byte[] getUuid() {
      return uuid;
    }

    public ArrayList<BLEAttribute> getAttributes() {
      return attributes;
    }

    public String getUuidString() {
      String result = "";
      for(int i = 0; i<uuid.length; i++) {
        result = String.format("%02X", uuid[i]) + result;
      }
      result = "0x" + result;
      return result;
    }

    public String toString() {
      return "BLEService " + getUuidString() + " (" + start + ".." + end + ")";
    }

    public String getDescription() {
      String result = toString();
      for (BLEAttribute a : attributes) {
        result += "\n\t" + a.toString();
      }
      return result;
    }
  }
  
  public static class BLEAttribute 
  {
    protected byte[] uuid;
    protected int handle;

    public BLEAttribute(byte[] uuid, int handle) {
      this.uuid = uuid;
      this.handle = handle;
    }

    public byte[] getUuid() {
      return uuid;
    }

    public String getUuidString() {
      String result = "";
      for(int i = 0; i<uuid.length; i++) {
        result = String.format("%02X", uuid[i]) + result;
      }
      result = "0x" + result;
      return result;
    }

    public String toString() {
      return "ATT " + getUuidString() + " => 0x" + Integer.toHexString(handle).toUpperCase();
    }
  }
  
  /* *****************************************************************************
   * End of copied classes.
   *******************************************************************************/
  
  public JP2BT() throws UnsatisfiedLinkError  {
    super( null );
  }

  public JP2BT( File folder ) throws UnsatisfiedLinkError  {
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
    return "0.1";
  }
  
  @Override
  public int getInterfaceType() {
    return 0x601;
  }

  @Override
  public String[] getPortNames() 
  {
    ArrayList<String> portList = new ArrayList<String>();
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
    if ( progressUpdater != null && ( getUse() == Use.DOWNLOAD || getUse() == Use.UPLOAD ) )
    {
      setProgressName( getUse() == Use.DOWNLOAD ? "DOWNLOADING:" : "VERIFYING:" );
      progressUpdater.updateProgress( progress );
    }
    int blockSize = 0x80;
    int remaining = length;
    int pos = 0;
    int progressIncrement = ( 100 * blockSize ) / ( length + blockSize );

    if ( getUse() == Use.DOWNLOAD )
    {
      if ( !bleRemote.updateConnData( this, sequence++ ) )
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
      int size = remaining > blockSize ? blockSize : remaining;
      byte[] block = readRemoteBlock( address + pos, size );
      progress += progressIncrement;
      if ( progressUpdater != null && ( getUse() == Use.DOWNLOAD || getUse() == Use.UPLOAD ) )
        progressUpdater.updateProgress( progress );
      
      if ( block == null )
        break;
      System.arraycopy( block, 0, buffer, pos, size );
      pos += size;
      remaining -= size;
    }
    return pos;
  }

  @Override
  public int writeRemote( int address, byte[] buffer, int length )
  {
    int erasePageSize = 0x800;
    if ( length == 0 )
      return 0;
    if ( ( address < getRemoteEepromAddress() ) 
        || (address + length > getRemoteEepromAddress() + getRemoteEepromSize() ) 
        || (length % erasePageSize) != 0 || (address % erasePageSize) != 0 )
      return -1;
    int progress = 0;
    if ( progressUpdater != null && getUse() == Use.UPLOAD )
    {
      setProgressName( "UPLOADING:" );
      progressUpdater.updateProgress( progress );
    }
  
    int blockSize = 0x80;
    int remaining = length;
    int pos = 0;
    int progressIncrement = ( 100 * blockSize ) / ( length + blockSize );

    if ( getUse() == Use.UPLOAD )
    {
      if ( !bleRemote.updateConnData( this, sequence++ ) )
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
    
    if ( erase( address, address + length - 1 ) != 0 )
      return -1;
    
    while ( remaining > 0 )
    {
      try
      {
        Thread.sleep( 200 );
      }
      catch ( InterruptedException e )
      {
        e.printStackTrace();
      }
      int size = remaining > blockSize ? blockSize : remaining;
      if ( writeRemoteBlock( address + pos, Arrays.copyOfRange( buffer, pos, pos + size ) ) != 0 )
      {
        break;
      }
      progress += progressIncrement;
      if ( progressUpdater != null && getUse() == Use.UPLOAD )
        progressUpdater.updateProgress( progress );
      pos += size;
      remaining -= size;
    }
    return pos;
  }

  public void setBleMap( LinkedHashMap< String, BLERemote > bleMap )
  {
    this.bleMap = bleMap;
  }
  
  /* *****************************************************************************
   * Serial port utilities.
   * Copied from org.thingml.bglib.gui.BLED112.java by <franck.fleurey@sintef.no>
   *   under GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007,
   *   obtainable at http://www.gnu.org/licenses/lgpl-3.0.txt
   *******************************************************************************/

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
  
  /* *****************************************************************************
   * End of copied Serial port utilities.
   *******************************************************************************/
  
  public String connectBLED112( String portName )
  {
    port = connectSerial(portName);
    String message = null;
    if (port != null) 
    {
      try 
      {
        transport = new BGAPITransport(port.getInputStream(), port.getOutputStream());
        bgapi = new BGAPI(transport);
        System.err.println("Trying to connect to BLED on port " + portName);
        bgapi.addListener(bgl);
        Thread.sleep(250);
        bledConn = false;
        bgapi.send_system_get_info();
        long waitStart = Calendar.getInstance().getTimeInMillis();
        long delay = 0;
        while ( !bledConn )
        {
          delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
          if ( delay > 1000 )
          {
            message = "Connection request timed out after 1000ms";
            disconnectBLED112();
            break;
          }
        }
        if ( bledConn )
          System.err.println( "Connected to BLED after " + delay + "ms" );
      } 
      catch (Exception ex) 
      {
        System.err.println("Exception while connecting to " + port);
        message= "Error in connecting to BLED";
      }
    }
    else 
    {
      System.err.println("Failed to find port " + portName );
      message = "Failed to find port " + portName;
    }
    if ( message != null )
    {
      JOptionPane.showMessageDialog( null, message, "Connection error", JOptionPane.ERROR_MESSAGE );
      return null;
    }
    return portName;
  }
  
  public void disconnectBLED112()
  {
    if (bgapi != null)
    {
      bgapi.removeListener(bgl);
      bgapi.send_system_reset(0);
      bgapi.disconnect();
    }
    if (port != null) 
    {
      port.closePort();
    }
    bgapi = null;
    port = null;
    connection = -1;
  }
  
  public void discoverUEI( boolean start )
  {
    if ( start )
    {
      scanning = true;
      bgapi.send_gap_set_scan_parameters(10, 250, 1);
      bgapi.send_gap_discover(2);
    }
    else
    {
      bgapi.send_gap_end_procedure();
      long waitStart = Calendar.getInstance().getTimeInMillis();
      long delay = 0;
      while ( scanning )
      {
        delay = Calendar.getInstance().getTimeInMillis() - waitStart;
        if ( delay > 1000 )
        {
          System.err.println( "Scanning failed to end" );
          scanning = false;
          break;
        }
      }
    }
  }
  
  public boolean connectUEI() throws InterruptedException
  {
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 10 );
    connection = -1;
    BDAddr addr = BDAddr.fromString( bleRemote.address );
    bgapi.send_gap_connect_direct( addr, 0, 60, 76, 500, 0 );
    long waitStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    while ( connection == -1 )
    {
      delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
      if ( delay > 5000 )
      {
        return false;
      }
    }
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 20 );
    System.err.println( "Connected after " + delay + "ms with handle " + connection );
    System.err.println( "Starting service discovery" );
    discovery_state = SERVICES;
    waitStart = Calendar.getInstance().getTimeInMillis();
    delay = 0;
    bgapi.send_attclient_read_by_group_type( connection, 1, 0xffff, new byte[]{0x00, 0x28} );
    while ( discovery_state != IDLE )
    {
      delay = Calendar.getInstance().getTimeInMillis() - waitStart;
      if ( delay > 10000 )
      {
        System.err.println( "Service discovery failed to end" );
        discovery_state = IDLE;
        return false;
      }
    }
    System.err.println( "Services discovered after " + delay + "ms" );
    if ( progressUpdater != null )
      progressUpdater.updateProgress( 50 );
    for ( BLEService serv : bleRemote.services.values() )
    {
      System.err.println( serv.toString() );
    }

    UEIPacket upkt = new CmdPacket( "APPINFOGET", new byte[]{} ).getUEIPacket( sequence++ );
    if ( ( upkt = getUEIPacketResponse( upkt, 75 ) ) == null || !bleRemote.interpret( "APPINFOGET", upkt ) )
    {
      System.err.println( "Failed to read info and sig" );
      return false;
    }

    if ( !bleRemote.updateConnData( this, sequence++ ) )
      return false;
   
    upkt = new UEIPacket( 0, sequence++, 0, 0x44, null );
    if ( ( upkt = getUEIPacketResponse( upkt, 85 ) ) == null || !bleRemote.interpret( null, upkt ) )
    {
      System.err.println( "Failed to read AppInfoRequest" );
      return false;
    }
    System.err.println( bleRemote.hasFinder ? "Remote has finder" : "Remote does not have finder" );

    bgapi.send_connection_update( connection, 104, 120, 4, 550 );
    
    
    
    
    // This erase command has no effect, whether the extender is installed or not.
    // If it is not installed then the first parameter is interpreted as two valid 2-byte
    // addresses, start and end, with start > end, so the command is accepted (return code
    // 0) but erases nothing.  If it is installed then the parameters are two 4-byte
    // addresses with the first being invalid, giving a Bad Address return code of 3.
    bleRemote.supportsUpload = erase( 0xFF001F00, 0 ) == 3;
    System.err.println( bleRemote.supportsUpload ? "Remote supports uploading"
        : "Remote does not support uploading" );

    return true;
  }
  
  public void disconnectUEI()
  {
    if ( connection < 0 ) return;
    disconnecting = true;
    bgapi.send_connection_disconnect( connection );
    long waitStart = Calendar.getInstance().getTimeInMillis();
    while ( connection >= 0 )
    {
      long delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
      if ( delay > 1000 )
      {
        break;
      }
    }
    System.err.println( connection >= 0 ? "Disconnection failed" : "Disconnection succeeded" );
  }

  /*
   * GATT DISCOVERY
   */
  private static final int IDLE = 0;
  private static final int SERVICES = 1;
  private static final int ATTRIBUTES = 2;
  private Iterator<BLEService> discovery_it = null;
  private BLEService discovery_srv = null;
  private int discovery_state = IDLE;

  BGAPIListener bgl = new BGAPIDefaultListener(){
    // Callbacks for class system (index = 0)
    public void receive_system_get_info(int major, int minor, int patch, int build, int ll_version, int protocol_version, int hw) 
    {
      bledConn = true;        
      System.err.println("Connected. BLED112:" + major + "." + minor + "." + patch + " (" + build + ") " + "ll=" + ll_version + " hw=" + hw);
    }

    // Callbacks for class attributes (index = 2)
    public void receive_attributes_value(int connection, int reason, int handle, int offset, byte[] value) {
      System.err.println("Attribute Value att=" + Integer.toHexString(handle) + " val = " + bytesToString(value));
    }

    // Callbacks for class connection (index = 3)
    public void receive_connection_status(int conn, int flags, BDAddr address, int address_type, int conn_interval, int timeout, int latency, int bonding) {
      System.err.println("[" + address.toString() + "] Conn = " + conn + " Flags = " + flags);
      if (flags != 0) 
      {
        connection = conn;
      }
      else 
      {
        System.err.println("Connection lost!");
        connection = -1;
//        bledevice = null;
      } 
    }
    
    public void receive_connection_update(int connection, int result) 
    {
      System.err.println( "Connection update result = " + result );
    }
    
    public void receive_connection_get_rssi(int connection, int rssi)
    {
      bleRemote.signalStrength = rssi;
    }
    
    public void receive_connection_disconnected(int conn, int reason)
    {
      connection = -1;
      System.err.println( "Disconnected with reason code 0x" + Integer.toHexString( reason ) );
      if ( !disconnecting )
      {
        disconnecting = true;
        owner.disconnectBLE();
      }
    }

    // Callbacks for class attclient (index = 4)
    public void receive_attclient_read_by_group_type(int connection, int result) {
      System.err.println( "Read by group type returned connection="+connection+", result=" + result );
    }

    public void receive_attclient_procedure_completed(int connection, int result, int chrhandle) {
      if (discovery_state != IDLE && bleRemote != null) {
        if (discovery_state == SERVICES) { // services have been discovered
          discovery_it = bleRemote.services.values().iterator();
          discovery_state = ATTRIBUTES;
        }
        if (discovery_state == ATTRIBUTES) {
          if (discovery_it.hasNext()) {
            discovery_srv = discovery_it.next();
            bgapi.send_attclient_find_information(connection, discovery_srv.getStart(), discovery_srv.getEnd());
          }
          else { // Discovery is done
            System.err.println("Discovery completed:");
            System.err.println(bleRemote.getGATTDescription());
            discovery_state = IDLE;
          }
        }
      }
      if (result != 0) {
        System.err.println("ERROR: Attribute Procedure Completed with error code 0x" + Integer.toHexString(result));
      }
    }
    
    public void receive_attclient_group_found(int connection, int start, int end, byte[] uuid) {
      if (bleRemote != null) {
        System.err.println( "Group found" );
        BLEService srv = new BLEService(uuid, start, end);
        bleRemote.services.put(srv.getUuidString(), srv); 
      }
    }
    
    public void receive_attclient_find_information_found(int connection, int chrhandle, byte[] uuid) {
      if (discovery_state == ATTRIBUTES && discovery_srv != null) {
        BLEAttribute att = new BLEAttribute(uuid, chrhandle);
        discovery_srv.getAttributes().add(att);
        bleRemote.attributeHandles.put( att.getUuidString(), chrhandle );
      }
    }
    
    public void receive_attclient_attribute_value(int connection, int atthandle, int type, byte[] value) {
      System.err.println("Attclient Value att=" + Integer.toHexString(atthandle) + " val = " + bytesToString(value));
      Integer inHandle = bleRemote.attributeHandles.get( "0xFFE2" );
      UEIPacket upkt = null;
      boolean ueiInOk = true;
      if ( inHandle != null && atthandle == inHandle )
      {
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
        }
        else if ( state == UEIPacket.getFrameType( "FragmentStart" ) )
        {
          ueiInOk = true;
          ueiInStart = Calendar.getInstance().getTimeInMillis();
          upkt = new UEIPacket( frameType, sequence, value[ 2 ], 
              value[ 4 ], value[ 3 ], Arrays.copyOfRange( value, 5, value.length ) );
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
          }
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
          }
          else if ( ueiInOk )
          {
            System.err.println( upkt.toString() );
            incoming.add( upkt );
          }
        }
      }
    }
    
    public void receive_attclient_write_command(int connection, int result)
    {
      sentState = 1 + result;
    }

    // Callbacks for class gap (index = 6)
    public void receive_gap_end_procedure(int result)
    {
      if ( result == 0 ) scanning = false;
    }

    public void receive_gap_scan_response(int rssi, int packet_type, BDAddr sender, int address_type, int bond, byte[] data)
    {
      byte[] b = sender.getByteAddr();
      String addr = String.format( "%02x:%02x:%02x:%02x:%02x:%02x", b[5],b[4],b[3],b[2],b[1],b[0] );
      if ( addr.startsWith( "48:d0:cf" ) )
      {
        System.err.println( "Found " + addr );
        if ( !bleMap.containsKey( addr ) )
        {
          String ueiName = getNameFromScanData( data );
          BLERemote dev = new BLERemote( ueiName + " " + addr.substring( 9 ), ueiName, addr );
          dev.found = true;
          dev.rssi = rssi;
          System.err.println("Create remote: " + dev.toString());;
          bleMap.put( addr, dev );
        }
        else
        {
          bleMap.get( addr ).found = true;
        }
      }
    }
  };
  
  /*
   * END OF GATT DISCOVERY
   */
  
  /** 
   * Returns -1 if sending timed out, else UEI error code, 0 on success
   */
  public int sendUEIPacket( int connection, int atthandle, UEIPacket upkt )
  {
    int n = 0;
    List< BGAPIPacket > bpktList = upkt.toBGAPI( connection, atthandle );
    for ( BGAPIPacket bpkt : bpktList )
    {
      n++;
      sentState = 0;
      transport.sendPacket( bpkt );
      long waitStart = Calendar.getInstance().getTimeInMillis();
      long delay = 0;
      while ( sentState == 0 )
      {
        // wait for acknowledgement of save receipt
        delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
        if ( delay > 2000 )
        {
          System.err.println( "Sending UEI packet timed out at BPI packet " + n + " of " + bpktList.size() );
          return -1;
        }
      }
      if ( sentState > 1 )
      {
        System.err.println( "Sending UEI packet returned error code " + ( sentState-1) + " at BPI packet " + n + " of " + bpktList.size() );
      }
      if ( n < bpktList.size() )
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
    return sentState-1;
  }
  
  public UEIPacket getUEIPacketResponse( UEIPacket upkt, int progress )
  {
    // Progress updater is only updated when value supplied is > 0
    int ueiOut = bleRemote.attributeHandles.get( "0xFFE1" );
    if ( sendUEIPacket( connection, ueiOut, upkt ) != 0 )
      return null;
    if ( progressUpdater != null && progress > 0 )
      progressUpdater.updateProgress( progress );
    UEIPacket upktRcvd = getUEIPacketIn();
    if ( upktRcvd == null )
      return null;
    if ( progressUpdater != null && progress > 0 )
      progressUpdater.updateProgress( progress + 2 );
    return upktRcvd;
  }
  
  private String getNameFromScanData( byte[] data )
  {
    // See Bluetooth Spec. v5.0, vol. 3, part C, sect. 11
    // for scan response data format
    int pos = 0;
    String name = "";
    while ( pos < data.length )
    {
      int adSize = data[ pos++ ];
      int adType = data[ pos++ ];
      if ( adType == 8 || adType == 9 )
      {
        // Type 8 is short name, 9 is complete name
        name = new String( Arrays.copyOfRange( data, pos, pos + adSize - 1 ) );
        break;
      }
      pos += adSize - 1;
    }
    return name;
  }
  
  public UEIPacket getUEIPacketIn()
  {
    ueiInStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    while ( incoming.isEmpty() )
    {
      delay = Calendar.getInstance().getTimeInMillis() - ueiInStart;
      if ( delay > 6000 )
      {
        System.err.println( "Incoming UEI packet timed out" );
        return null;
      }
    }
    System.err.println( "Incoming UEI packet received" );
    UEIPacket upkt = incoming.remove( 0 );
    return upkt;
  }
  
  public void finderOn( boolean setOn )
  {
    int ueiOut = bleRemote.attributeHandles.get( "0xFFE1" );
    byte[] args = { 5, setOn ? ( byte )0xa0 : 8, 0 };
    UEIPacket p = new UEIPacket( 0, sequence++, 0x25, 0x44, args );
    if ( sendUEIPacket( connection, ueiOut, p ) == 0 )
      System.err.println( "Finder turned " + ( setOn ? "On" : "Off") );
    else
      System.err.println( "Error in setting finder " + ( setOn ? "On" : "Off") );
  }
  
  public byte[] readRemoteBlock( int address, int length )
  {
    // Args are 4-byte msb address and 2-byte msb length
    byte[] args = new byte[ 6 ];
    for ( int i = 0; i < 4; i++ )
      args[ 3 - i ] = ( byte )( ( address >> 8*i ) & 0xFF );
    for( int i = 0; i < 2; i++ )
      args[ 5 - i ] = ( byte )( ( length >> 8*i ) & 0xFF );
    CmdPacket cpkt = new CmdPacket( "DATAREAD", args );
    int ueiOut = bleRemote.attributeHandles.get( "0xFFE1" );
    if ( sendUEIPacket( connection, ueiOut, cpkt.getUEIPacket( sequence++ ) ) != 0 )
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
    System.err.println( bytesToString( result ) );
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
    int ueiOut = bleRemote.attributeHandles.get( "0xFFE1" );
    if ( sendUEIPacket( connection, ueiOut, cpkt.getUEIPacket( sequence++ ) ) != 0 )
    {
      System.err.println( "Outgoing UEI packet failed to send" );
      return -1;
    }

    UEIPacket upkt = getUEIPacketIn();
    if ( upkt == null )
      return -2;
    byte[] result = upkt.getCmdArgs();
    System.err.println( "Erase args rcvd: " + bytesToString( result ) );
    int n = upkt.isValidCmd();
    return n;
  }
  
  public int sendRecord( Hex record )
  {
//    System.err.println( "Start sending record" );
    long recordStart = Calendar.getInstance().getTimeInMillis();
    byte[] args = new byte[ record.length() + 2 ];
    args[ 0 ] = args[ 1 ] = 0;
    System.arraycopy( record.toByteArray(), 0, args, 2, record.length() );
    CmdPacket cpkt = new CmdPacket( "RECORDSET", args );
    int ueiOut = bleRemote.attributeHandles.get( "0xFFE1" );
    if ( sendUEIPacket( connection, ueiOut, cpkt.getUEIPacket( sequence++ ) ) != 0 )
    {
      System.err.println( "Record failed to send" );
      return -1;
    }
    long duration = Calendar.getInstance().getTimeInMillis() - recordStart;
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
        return -4;
      }
    }
    if ( upkt == null )
      return -2;
    byte[] result = upkt.getCmdArgs();
    if ( result == null )
      return -3;
    System.err.println( "Send args rcvd: " + bytesToString( result ) );
    int n = upkt.isValidCmd();
    duration = Calendar.getInstance().getTimeInMillis() - recordStart;
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
    int ueiOut = bleRemote.attributeHandles.get( "0xFFE1" );
    if ( sendUEIPacket( connection, ueiOut, cpkt.getUEIPacket( sequence++ ) ) != 0 )
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
      delay = Calendar.getInstance().getTimeInMillis() - ueiInStart;
      if ( delay > 6000 )
      {
        System.err.println( "Write Block timed out after delay of " + delay + "ms" );
        return -2;
      }
    }
    byte[] result = upkt.getCmdArgs();
    System.err.println( "Send args rcvd: " + bytesToString( result ) );
    return upkt.isValidCmd();
  }

  public boolean isScanning()
  {
    return scanning;
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

  private long ueiInStart = 0;  // Timer start for incoming UEI packet fragments
  private boolean bledConn = false;
  private boolean scanning = false;
  public BGAPI bgapi;
  private LinkedHashMap< Integer, UEIPacket > ueiIn = new LinkedHashMap< Integer, UEIPacket >();
  private BGAPITransport transport = null;
  private int sentState = 0;  // 0=sent, 1=ack rcvd, 2=(error code)+1
  public int connection = -1;
  public boolean disconnecting = false;
  public BLERemote bleRemote = null;
  private LinkedHashMap< String, BLERemote > bleMap = new LinkedHashMap< String, BLERemote >();
  protected SerialPort port = null;
  public int sequence = 1;
  private RemoteMaster owner = null;
  private ArrayList< UEIPacket > incoming = new ArrayList< UEIPacket >();
}
