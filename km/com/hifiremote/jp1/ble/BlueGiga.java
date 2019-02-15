package com.hifiremote.jp1.ble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.JOptionPane;

import org.thingml.bglib.BDAddr;
import org.thingml.bglib.BGAPI;
import org.thingml.bglib.BGAPIDefaultListener;
import org.thingml.bglib.BGAPIListener;
import org.thingml.bglib.BGAPIPacket;
import org.thingml.bglib.BGAPITransport;

import rmirwin10ble.IBleInterface;

import com.fazecast.jSerialComm.SerialPort;
import com.hifiremote.jp1.RemoteMaster;
import com.hifiremote.jp1.io.UEIPacket;

public class BlueGiga implements IBleInterface
{
  /* *****************************************************************************
   * BGLIB is a Java implementation of the BGAPI binary protocol for Bluegiga
   *   BLE112 Bluetooth low energy modules.  It is available here:
   *   https://github.com/SINTEF-9012/bglib
   * 
   * Classes BLEService and BLEAttribute.
   * Copied from org.thingml.bglib.gui by <franck.fleurey@sintef.no>
   *   under GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007,
   *   obtainable at http://www.gnu.org/licenses/lgpl-3.0.txt
   *******************************************************************************/
  
  public static class BLEService 
  {
    /*
    public static Hashtable<String, String> profiles = new Hashtable<String, String>();
    static {
      profiles.put("0x180A", "");
    }
    */

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
    private int handle;

    public BLEAttribute(byte[] uuid, int handle) {
      this.uuid = uuid;
      this.setHandle( handle );
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
      return "ATT " + getUuidString() + " => 0x" + Integer.toHexString(getHandle()).toUpperCase();
    }

    public int getHandle()
    {
      return handle;
    }

    public void setHandle( int handle )
    {
      this.handle = handle;
    }
  }
  
  /* *****************************************************************************
   * End of copied classes.
   *******************************************************************************/

  public BlueGiga()
  { }
  
  public static SerialPort connectSerial(String portName) 
  {
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

  
  public String ConnectBLE( String portName )
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
            DisconnectBLE();
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
  
  public void DisconnectBLE()
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
  
  public void DiscoverUEI( boolean start )
  {
    if ( start )
    {
      addressList.clear();
      nameList.clear();
      rssiList.clear();
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
  
  public boolean DiscoverServices()
  {
    System.err.println( "Starting service discovery" );
    discovery_state = SERVICES;
    long waitStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
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
    for ( BLEService serv : services.values() )
    {
      System.err.println( serv.toString() );
    }
    return true;
  }
  
  public boolean GetFeatures()
  {
    stage = 5;
    StringBuffer sb = new StringBuffer();
    int handle = findDescriptor( "0xFFE0", "0xFFE1", "0x2901", true );
    if ( handle != 0 )
      for( byte b : receivedValue ) sb.append( (char)( b & 0xFF ) );
    System.err.println( "FFE1 description = \"" + sb.toString() + "\"" );

    sb = new StringBuffer();
    handle = findDescriptor( "0xFFE0", "0xFFE2", "0x2901", true );
    if ( handle != 0 )
      for( byte b : receivedValue ) sb.append( (char)( b & 0xFF ) );
    System.err.println( "FFE2 description = \"" + sb.toString() + "\"" );

    handle = findDescriptor( "0xFFE0", "0xFFE2", "0x2902", false );
    if ( handle != 0 )
    {
      hasCCCD = true;
      System.err.println( "Subscribing to notification for characteristic 0xFFE2" );
      long waitStart = Calendar.getInstance().getTimeInMillis();
      completed = false;
      bgapi.send_attclient_attribute_write( connection, handle, new byte[]{ 0x01, 0x00 } );
      while ( !completed )
      {
        long delay = Calendar.getInstance().getTimeInMillis() - waitStart;
        if ( delay > 2000 )
        {
          System.err.println( "Unable to write 0xFFE2 CCCD" );
          return false;
        }
      }
      findDescriptor( "0xFFE0", "0xFFE2", "0x2902", true );
      int val = ( receivedValue[ 1 ] << 8 ) + receivedValue[ 0 ];
      String[] cccdValues = new String[]{ "None", "Notify", "Indicate" };
      subscription = "CCCD state: " + cccdValues[ val ];
    }
    else
    {
      subscription = "CCCD absent";
    }
    return true;
  }
  
  public String getGATTDescription() 
  {
    String result = toString();
    for (BLEService s : services.values()) 
      result += "\n" + s.getDescription();
    return result;
  }
  
  public boolean IsConnected()
  {
    return connection >= 0;
  }

  public boolean ConnectUEI( String address )
  {
    reserved_connection = -1;
    connection = -1;
    stage = 1;
    incoming.clear();
    BDAddr addr = BDAddr.fromString( address );
    bgapi.send_gap_connect_direct( addr, 0, 60, 76, 500, 0 );
    long waitStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    while ( connection == -1 )
    {
      delay = Calendar.getInstance().getTimeInMillis() - waitStart; 
      if ( delay > 5000 )
        return false;
    }
    stage = 2;
    return true;
  }

  
  public String DisconnectUEI()
  {
    if ( connection < 0 ) 
      return "Already disconnected";
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
    return connection >= 0 ? "Disconnection failed" : "Disconnection succeeded";
  }

  
  private int findDescriptor( String uuidServ, String uuidChstic, String uuidDesc, boolean doRead )
  {
    int handle = 0;
    boolean atChstic = false;
    BLEService ueiService = services.get( uuidServ );
    for ( BLEAttribute att : ueiService.getAttributes() )
    {
      if ( !atChstic && !att.getUuidString().equals( uuidChstic ) )
        continue;
      atChstic = true;
      if ( att.getUuidString().equals( uuidDesc ) )
      {
        handle = att.getHandle();
        break;
      }
    }
    
    if ( !doRead )
      return handle;
    
    receivedValue = null;
    if ( handle != 0 )
    {
      System.err.println( "Reading handle 0x" + Integer.toHexString( handle ).toUpperCase() );
      long waitStart = Calendar.getInstance().getTimeInMillis();
      long delay = 0;
      receivedValue = null;
      bgapi.send_attclient_read_by_handle( connection, handle );
      while ( receivedValue == null )
      {
        delay = Calendar.getInstance().getTimeInMillis() - waitStart;
        if ( delay > 2000 )
        {
          System.err.println( "Unable to read descriptor with handle " + handle );
          return 0;
        }
      }
    }
    return handle;
  }

  
  public boolean IsScanning()
  {
    return scanning;
  }
  
  public void WritePacket( byte[] pkt )
  {
    int ueiOut = attributeHandles.get( "0xFFE1" );
    BGAPIPacket bpkt = new BGAPIPacket(0, 4, 6);
    bpkt.w_uint8( connection);
    bpkt.w_uint16( ueiOut );
    bpkt.w_uint8array( pkt );
    System.err.println( "Writing bpkt: " + bpkt.toString() );
    transport.sendPacket( bpkt );
  }

  
  BGAPIListener bgl = new BGAPIDefaultListener(){
    // Callbacks for class system (index = 0)
    public void receive_system_get_info(int major, int minor, int patch, int build, int ll_version, int protocol_version, int hw) 
    {
      bledConn = true;        
      System.err.println("Connected. BLED112:" + major + "." + minor + "." + patch + " (" + build + ") " + "ll=" + ll_version + " hw=" + hw);
    }

    // Callbacks for class attributes (index = 2)
    public void receive_attributes_value(int connection, int reason, int handle, int offset, byte[] value) {
      receivedValue = value;
//      System.err.println("Attribute Value att=" + Integer.toHexString(handle) + " val = " + bytesToString(value));
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
      } 
    }
    
    public void receive_connection_update(int connection, int result) 
    {
      System.err.println( "Connection update result = " + result );
    }
    
    public void receive_connection_get_rssi(int connection, int rssi)
    {
      signalStrength = rssi;
    }
    
    public void receive_connection_disconnected(int conn, int reason)
    {
      connection = -1;
      System.err.println( "Disconnected with reason code 0x" + Integer.toHexString( reason ) );
      if ( !disconnecting )
      {
        disconnecting = true;
//        owner.disconnectBLE();
      }
    }

    // Callbacks for class attclient (index = 4)
    public void receive_attclient_read_by_group_type(int connection, int result) {
      System.err.println( "Read by group type returned connection="+connection+", result=" + result );
    }

    public void receive_attclient_procedure_completed(int connection, int result, int chrhandle) {
      if (discovery_state != IDLE /*&& bleRemote != null*/) 
      {
        if (discovery_state == SERVICES) 
        { // services have been discovered
          stage = 3;
          discovery_it = services.values().iterator();
          discovery_state = ATTRIBUTES;
        }
        if (discovery_state == ATTRIBUTES) 
        {
          if (discovery_it.hasNext()) 
          {
            stage = 4;
            discovery_srv = discovery_it.next();
            bgapi.send_attclient_find_information(connection, discovery_srv.getStart(), discovery_srv.getEnd());
          }
          else 
          { // Discovery is done
            System.err.println("Discovery completed:");
//            System.err.println(bleRemote.getGATTDescription());
            discovery_state = IDLE;
          }
        }
      }
      else
      {
        completed = true;
      }
      
      if (result != 0) 
      {
        System.err.println("ERROR: Attribute Procedure Completed with error code 0x" + Integer.toHexString(result));
      }
    }
    
    public void receive_attclient_group_found(int connection, int start, int end, byte[] uuid) {
//      if (bleRemote != null) {
//        System.err.println( "Group found" );
        BLEService srv = new BLEService(uuid, start, end);
        services.put(srv.getUuidString(), srv); 
//      }
    }
    
    public void receive_attclient_find_information_found(int connection, int chrhandle, byte[] uuid) {
      if (discovery_state == ATTRIBUTES && discovery_srv != null) {
        BLEAttribute att = new BLEAttribute(uuid, chrhandle);
        discovery_srv.getAttributes().add(att);
        attributeHandles.put( att.getUuidString(), chrhandle );
      }
    }
    
    public void receive_attclient_attribute_value(int connection, int atthandle, int type, byte[] value) {
//      System.err.println("Attclient Value att=" + Integer.toHexString(atthandle) + " val = " + bytesToString(value));
      Integer inHandle = attributeHandles.get( "0xFFE2" );
      if ( inHandle != null && atthandle == inHandle )
      {
        inCount++;
        synced = false;
        inData.add( value );
        synced = true;
      }
      else
      {
        receivedValue = value;
      }
    }
    
    public void receive_attclient_write_command(int connection, int result)
    {
      System.err.println( "Write command ack returned result " + result );
      sentState = 1 + result;
    }

    // Callbacks for class gap (index = 6)
    public void receive_gap_connect_direct(int result, int connection_handle) 
    {
      System.err.println( "Connect direct returned result " + result + ", handle " +  connection_handle );
      reserved_connection = connection_handle;
    }
    
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
//        System.err.println( "Found " + addr );
        if ( addressList.indexOf( addr ) < 0 )
        {
          addressList.add( addr );
          String ueiName = getNameFromScanData( data );
          nameList.add( ueiName );
          rssiList.add( rssi );
        }
      }
    }
  };
  
  public int GetListSize()
  {
    int size = Math.min( addressList.size(), nameList.size() );
    return Math.min( size, rssiList.size() );
  }
  
  public String GetListItem( int ndx )
  {
    return addressList.get(  ndx );
  }
  
  public String GetItemName( int ndx )
  {
    return nameList.get( ndx );
  }
  
  public int GetRssi( int ndx )
  {
    return rssiList.get( ndx );
  }
  
  public int GetInDataSize()
  {
    return inData.size();
  }
  
  public byte[] GetInData( int ndx )
  {
    while ( !synced ) {};
    return inData.remove( ndx );
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
  
  public boolean IsDisconnecting()
  {
    return disconnecting;
  }

  public void SetDisconnecting( boolean disconnecting )
  {
    this.disconnecting = disconnecting;
  }

  public int ReadSignalStrength()
  {
    signalStrength = 0;
    long waitStart = Calendar.getInstance().getTimeInMillis();
    long delay = 0;
    bgapi.send_connection_get_rssi( connection );
    while ( signalStrength == 0 )
    {
      delay = Calendar.getInstance().getTimeInMillis() - waitStart;
      if ( delay > 1000 )
      {
        System.err.println( "Unable to read signal strength" );
        return 0;
      }
    }
    return signalStrength;
  }
  
  public void SetSentState( int state )
  {
    sentState = state;
  }
  
  public int GetSentState()
  {
    return sentState;
  }
  
  @Override
  public String GetSubscription()
  {
    return subscription;
  }

  @Override
  public int GetStage()
  {
    return stage;
  }
  
  public boolean HasCCCD()
  {
    return hasCCCD;
  }
  
  public boolean NeedsCCCD()
  {
    return false;
  }
  
  public int GetInCount()
  {
    return inCount;
  }
  
  public void UpdateConnection(int interval_min, int interval_max, int latency, int timeout)
  {
    bgapi.send_connection_update( connection, interval_min, interval_max, latency, timeout );
  }

  
  private static final int IDLE = 0;
  private static final int SERVICES = 1;
  private static final int ATTRIBUTES = 2;
  private Iterator<BLEService> discovery_it = null;
  private BLEService discovery_srv = null;
  private int discovery_state = IDLE;
  private LinkedHashMap<String, BLEService> services = new LinkedHashMap<String, BLEService>();
  private LinkedHashMap<String, Integer> attributeHandles = new LinkedHashMap<String, Integer>();
  private int signalStrength;
  
  private boolean bledConn = false;
  private boolean scanning = false;
  private boolean hasCCCD = false;
  private String subscription = null;
  private BGAPI bgapi;
  private boolean synced = true;
  private BGAPITransport transport = null;
  private int sentState = 0;  // 0=sent, 1=ack rcvd, 2=(error code)+1
  public int reserved_connection = -1;
  public int connection = -1;
  private int stage = 0;
  private int inCount = 0;
  private boolean disconnecting = false;
  protected SerialPort port = null;
  private ArrayList< UEIPacket > incoming = new ArrayList< UEIPacket >();
  private byte[] receivedValue = null;
  private boolean completed = false;
  public String win10n = "Win10 Native";
  private ArrayList<String> addressList = new ArrayList<String>();
  private ArrayList<String> nameList = new ArrayList<String>();
  private ArrayList<Integer> rssiList = new ArrayList<Integer>();
  private ArrayList<byte[]> inData = new ArrayList< byte[] >();

}
