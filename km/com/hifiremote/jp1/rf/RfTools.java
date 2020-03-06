package com.hifiremote.jp1.rf;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import com.hifiremote.jp1.EndingFileFilter;
import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.JP1Frame;
import com.hifiremote.jp1.PropertyFile;
import com.hifiremote.jp1.RMFileChooser;
import com.hifiremote.jp1.RemoteMaster;
import com.hifiremote.jp1.rf.Mpdu.MSPrimitive;
import com.hifiremote.jp1.rf.Npdu.NSPrimitive;
import com.hifiremote.jp1.rf.Rf4ceAuthenticator.Source;
import com.hifiremote.jp1.rf.RfRemote.Pairing;

public class RfTools extends JP1Frame implements ActionListener
{
  public class RFAction extends AbstractAction
  {
    public RFAction( String text, String action, ImageIcon icon, String description, Integer mnemonic )
    {
      super( text, icon );
      putValue( ACTION_COMMAND_KEY, action );
      putValue( SHORT_DESCRIPTION, description );
      putValue( MNEMONIC_KEY, mnemonic );
    }

    @Override
    public void actionPerformed( ActionEvent event )
    {
      try
      {
        String command = event.getActionCommand();
        if ( command.equals( "OPEN" ) )
        {
          File file = getPSDFile( RfTools.this );
          try
          {
            openPSDfile( file );
          }
          catch ( Exception e1 )
          {
            e1.printStackTrace();
          }
        }
      }
      catch ( Exception ex )
      {
        ex.printStackTrace( System.err );
      }
    }
  }
  
  private void savePreferences() throws Exception
  {
    int state = getExtendedState();
    if ( state != Frame.NORMAL )
    {
      setExtendedState( Frame.NORMAL );
    }
    preferences.setRFBounds( getBounds() );

  }
  
  public RfTools( PropertyFile properties )
  {
    super( "RF Tools", properties );
    System.err.println( "RfTools opening" );
    setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
    
    addWindowListener( new WindowAdapter()
    {
      @Override
      public void windowClosing( WindowEvent event )
      {
        boolean doDispose = true;
        try
        {
          System.err.println( "RfTools.windowClosing() entered" );
          savePreferences();
        }
        catch ( Exception e )
        {
          System.err.println( "RfTools.windowClosing() caught an exception!" );
          e.printStackTrace( System.err );
        }
        finally
        {
          if ( doDispose )
          {
            dispose();
          }
        }
      }
    } );
    
    toolBar = new JToolBar();
    toolBar.setFloatable( false );
    add( toolBar, BorderLayout.PAGE_START );
    createMenus();
    createToolbar();
    setRfRemotesList();
    RfEditorPanel editorPanel = new RfEditorPanel( this );
    capturePanel = editorPanel.getCapturePanel();
    remotePanel = editorPanel.getRemotePanel();
    add( editorPanel, BorderLayout.CENTER );
    
    pack();
    Rectangle bounds = preferences.getRFBounds();
    if ( bounds != null )
    {
      setBounds( bounds );
    }
    setVisible( true );
  }
  
  public List< RfRemote > getRfRemotesList()
  {
    return rfRemotesList;
  }

  public List< RfRemote > setRfRemotesList()
  {
    rfRemotesList = new ArrayList< RfRemote >();
    String temp = null;
    for ( int ndx : getIndexMap().values() )
    {
      RfRemote rfRemote = new RfRemote();
      rfRemote.name = properties.getProperty( "RfRemote." + ndx + ".name" );
      temp = properties.getProperty( "RfRemote." + ndx + ".extAddr" );
      if ( temp != null )
      {
        rfRemote.extAddr = new Hex( temp );
      }
      temp = properties.getProperty( "RfRemote." + ndx + ".data" );
      if ( temp != null )
      {
        rfRemote.importRfData( new Hex( temp ), Source.CONTROLLER, 0 );
      }
      int n = 0;
      while ( ( temp = properties.getProperty( "RfRemote." + ndx + ".pair." + n ) ) != null )
      {
        rfRemote.pairings.add( new Pairing( new Hex( temp ) ) );
        String extra = properties.getProperty( "RfRemote." + ndx + ".extra." + n );
        if ( extra != null )
        {
          rfRemote.importRfData( new Hex( extra ), Source.TARGET, n );
        }
        n++;
      }
      rfRemotesList.add( rfRemote );
      ndx++;
    }
    return rfRemotesList;
  }
  
  private void createMenus()
  {
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar( menuBar );

    JMenu menu = new JMenu( "File" );
    menu.setMnemonic( KeyEvent.VK_F );
    menuBar.add( menu );
    
    openAction = new RFAction( "Open...", "OPEN", RemoteMaster.createIcon( "RMOpen24" ), "Open a file", KeyEvent.VK_O );
    menu.add( openAction ).setIcon( null );
    
    menu.addSeparator();

    exitItem = new JMenuItem( "Exit" );
    exitItem.setMnemonic( KeyEvent.VK_X );
    exitItem.addActionListener( this );
    menu.add( exitItem );
    
    menu = new JMenu( "RFRemote" );
    menu.setMnemonic( KeyEvent.VK_R );
    menuBar.add( menu );
    
    renameRegItem = new JMenuItem( "Rename RF Remote" );
    renameRegItem.setMnemonic( KeyEvent.VK_N );
    renameRegItem.addActionListener( this );
    renameRegItem.setToolTipText( "Rename a registered RF Remote" );
    menu.add( renameRegItem );
    
    deleteRegItem = new JMenuItem( "Delete registration" );
    deleteRegItem.setMnemonic( KeyEvent.VK_D );
    deleteRegItem.addActionListener( this );
    deleteRegItem.setToolTipText( "Delete the registration of an RF Remote" );
    menu.add( deleteRegItem );
  }
  
  private void createToolbar()
  {
    toolBar.add( openAction );
  }
  
  @Override
  public void actionPerformed( ActionEvent e )
  {
    try
    {
      Object source = e.getSource();
      if ( source == exitItem )
      {
        dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
      }
      else if ( source == deleteRegItem )
      {
        String title = "Delete registration";
        String message = "Please select an RF Remote to delete";
//        setRfRemotesList();
        RfRemote rfRemote = ( RfRemote ) JOptionPane.showInputDialog( this, message, title, JOptionPane.QUESTION_MESSAGE, null, 
            rfRemotesList.toArray( new RfRemote[ 0 ] ), rfRemotesList.get( 0 ) );
        if ( rfRemote == null )
        {
          return;
        }
        int index = getIndexMap().get( rfRemote.name );
        properties.remove( "RfRemote." + index + ".name" );
        properties.remove( "RfRemote." + index + ".extAddr" );
        properties.remove( "RfRemote." + index + ".data" );
        properties.remove( "RfRemote." + index + ".name" );
        int n = 0;
        while ( properties.remove( "RfRemote." + index + ".pair." + n ) != null )
        {
          properties.remove( "RfRemote." + index + ".extra." + n++ );
        };
        setRfRemotesList();
        remotePanel.update( false );
        message = "RF Remote " + rfRemote.name + " has been deleted.";
        JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
      }
      else if ( source == renameRegItem )
      {
        String title = "Rename RF Remote";
        String message = "Please select an RF Remote to rename";
        RfRemote rfRemote = ( RfRemote ) JOptionPane.showInputDialog( this, message, title, JOptionPane.QUESTION_MESSAGE, null, 
            rfRemotesList.toArray( new RfRemote[ 0 ] ), rfRemotesList.get( 0 ) );
        if ( rfRemote == null )
        {
          return;
        }
        int index = getIndexMap().get( rfRemote.name );
        message = "Please enter a new name for RF Remote " + rfRemote.name + ".";
        String name = ( String ) JOptionPane.showInputDialog( this, message, title, JOptionPane.QUESTION_MESSAGE );
        if ( name == null )
        {
          return;
        }
        properties.put( "RfRemote." + index + ".name", name );
        rfRemote.name = name;
      }
    }
    catch ( Exception ex )
    {
      ex.printStackTrace( System.err );
    }
  }
  
  public void openPSDfile( File file )
  {
    if ( file == null )
    {
      return;
    }
    System.err.println( "Reading PSD file");
    int len = ( int )file.length();
    System.err.println( "File size: " + len );
    byte[] psdPacket = new byte[ PSDENTRYSIZE ];
    int packetCount = len / PSDENTRYSIZE;
    LinkedHashMap< String, NSPrimitive > map = new LinkedHashMap< String, NSPrimitive >();    
    Rf4ceAuthenticator authenticator = new Rf4ceAuthenticator( rfRemotesList, this );
    try 
    {
      InputStream input = null;
      try 
      {
        input = new BufferedInputStream( new FileInputStream( file ) );
        for ( int i = 0; i < packetCount; i++ )
        {
          // Read one packet from the .psd file.  Entries have a fixed length of 151 bytes,
          // set as PSDENTRYSIZE.
          int loopBytesRead = 0;
          while( loopBytesRead < PSDENTRYSIZE )
          {
            int bytesRemaining = PSDENTRYSIZE - loopBytesRead;
            int bytesRead = input.read( psdPacket, loopBytesRead, bytesRemaining ); 
            if ( bytesRead > 0 )
            {
              loopBytesRead += bytesRead;
            }
          }
          // The packet starts with a 1-byte info field, a 4-byte packet number and an
          // 8-byte timestamp, none of which is relevant to interpreting the packet.  This
          // is followed by a 2-byte lsb length field, then a packet payload of this length.
          // Packet bytes beyond the payload are padding.  Now extract the payload.
          int payloadLen = psdPacket[ 13 ] + ( psdPacket[ 14 ] << 8 );
          Hex payload = ( new Hex( psdPacket ) ).subHex( 15, payloadLen );
          // The packet payload consists of the PPDU (PHY-layer PDU) beyond its start-of-frame
          // delimiter.  This in turn consists of a length byte followed by an MPDU (MAC-layer
          // PDU) of this length.  Check for length consistency and extract the MPDU if
          // consistent.
          int mpduLen = payload.getData()[ 0 ];
          if ( payloadLen != mpduLen + 1 )
          {
            System.err.println( "Inconsistent MPDU length, file read aborting" );
            return;
          }
          Mpdu mpdu = new Mpdu( payload, 1 );
          MSPrimitive msPrim = mpdu.parse();
          Npdu npdu = msPrim.npdu;
          if ( npdu == null )
            continue;
          String frameCtr = RfTools.getAddrString( npdu.subHex( 1, 4 ) );
          if ( map.containsKey( frameCtr ) )
          {
            map.get( frameCtr ).rptCount++;
            continue;
          }
          NSPrimitive nsPrim = npdu.parse();
          map.put( frameCtr, nsPrim );
          nsPrim.addrData = msPrim.addrData;
          nsPrim.process( authenticator );
          capturePanel.getData().add( nsPrim );
          RfRemote rfRemote = authenticator.getRfRemote();
          if ( rfRemote.changed )
          {
            // When updating a provisional registration from a DISCOVERY_REQ, no pairing
            // is involved and so the authenticator pairNdx is -1.
            if ( authenticator.getPairNdx() < 0 
                || rfRemote.pairings.get( authenticator.getPairNdx() ).getPairRef()
                  == authenticator.getPairNdx() )
            {
              updateRegistration( rfRemote );
            }
            else
            {
              String title = "RF Remote registration error";
              String addrStr = RfTools.getAddrString( rfRemote.extAddr );
              String msg = rfRemote.name == null ? 
                  "Registration of RF Remote with IEEE address " + addrStr + " failed." :
                  "Change of registration for RF Remote named\n  " + rfRemote.name
                  + "\nfailed.  The registration is retained unchanged.";
              JOptionPane.showMessageDialog( this, msg, title, JOptionPane.ERROR_MESSAGE );
              System.err.println( "Pairing change failed" );
              return;
            }
//            System.err.println( "Remote data: " + rfRemote.getRfData( Source.CONTROLLER, 0) );
          }
        }          
        capturePanel.update();
        remotePanel.update( false );
      }
      finally 
      {
        input.close();
      }
    }
    catch ( FileNotFoundException ex ) 
    {
      System.err.println( "File not found." );
    }
    catch ( IOException ex ) 
    {
      System.err.println( ex );
    }
    return;
  }
  
  public void updateRegistration( RfRemote rfRemote )
  {
    if ( !rfRemote.changed )
    {
      return;
    }

    rfRemote.changed = false;
    String addrStr = rfRemote.extAddr != null ? RfTools.getAddrString( rfRemote.extAddr ) : null;
    int ndx = 0;
    LinkedHashMap< String, Integer > indexMap = getIndexMap();
    String name = rfRemote.name;
    String extAddrProperty = null;
    if ( name !=  null && indexMap.get( name ) != null )
    {
      ndx = indexMap.get( name );
      extAddrProperty = properties.getProperty( "RfRemote." + ndx + ".extAddr" );
    }
    else
    {
      rfRemotesList.add( rfRemote );
      for ( ndx = 1; indexMap.values().contains( ndx ); ndx++ ){}
    }

    if ( name == null )
    {
      String title = "Registration of RF Remote";
      String msg = 
          "Loading this file has created a pairing entry for an unregistered\n"
              + "RF Remote with IEEE address " + addrStr + ".  To register this\n"
              + "RF Remote in RMIR so that its data signals can be decrypted, please\n"
              + "enter a name for it in the box below.";
      String reply = "";
      while ( reply != null && reply.trim().isEmpty() )
      {
        reply = ( String )JOptionPane.showInputDialog( this, msg, title, JOptionPane.PLAIN_MESSAGE );
      }
      if ( reply == null )
      {
        msg = "Registration aborted.";
        JOptionPane.showMessageDialog( this, msg, title, JOptionPane.WARNING_MESSAGE );
        return;
      }
      rfRemote.name = reply.trim();
    }
    else if ( addrStr != null && extAddrProperty != null )
    {
      String title = "Update of RF Remote registration";
      String msg = "The pairings of RF Remote named\n  "
          + rfRemote.name
          + "\nhave been updated successfully.";
      JOptionPane.showMessageDialog( this, msg, title, JOptionPane.WARNING_MESSAGE );
    }
    else if ( addrStr != null )
    {
      // The null case is that of provisional registration, where the message is shown
      // elsewhere.
      String title = "Completion of provisional registration";
      String msg = 
          "Registration of RF Remote named " + name + " has been completed.\n"
        + "Its IEEE address is " + addrStr + ".";
      JOptionPane.showMessageDialog( this, msg, title, JOptionPane.INFORMATION_MESSAGE );
    }

    properties.put( "RfRemote." + ndx + ".name", rfRemote.name );
    if ( rfRemote.extAddr != null )
    {
      properties.put( "RfRemote." + ndx + ".extAddr", rfRemote.extAddr.toString() );
    }
    properties.put( "RfRemote." + ndx + ".data", rfRemote.getRfData( Source.CONTROLLER, 0 ).toString() );
    for ( int i = 0; i < rfRemote.pairings.size(); i++ )
    {
      properties.put( "RfRemote." + ndx + ".pair." + i, rfRemote.pairings.get( i ).getTable().toString() );
      Hex extra = rfRemote.getRfData( Source.TARGET, i );
      if ( extra != null )
      {
        properties.put( "RfRemote." + ndx + ".extra." + i, rfRemote.getRfData( Source.TARGET, i ).toString() );
      }
    }
    System.err.println( "Pairing change succeeded" );
    System.err.println( "Remote data: " + rfRemote.getRfData( Source.CONTROLLER, 0 ) );
    capturePanel.update();
    remotePanel.update( false );
  }

  public static LinkedHashMap< String, Integer > getIndexMap()
  {
    LinkedHashMap< String, Integer > map = new LinkedHashMap< String, Integer >();
    for ( String key : properties.stringPropertyNames() )
    {
      if ( key.startsWith( "RfRemote." ) )
      {
        String s = key.substring( 9 );
        s = s.substring( 0, s.indexOf( '.' ) );
        int index = new Integer( s );
        if ( !map.values().contains( index ) )
        {
          String name = properties.getProperty( "RfRemote." + index + ".name" );
          if ( name != null )
          {
            map.put( name, index );
          }
        }
      }
    }
    return map;
  }
  
  public static String getASCIIString( Hex hex )
  {
    return new String( hex.toByteArray() );
  }

  public static String getAddrString( Hex addr )
  {
    if ( addr == null )
    {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    int len = addr.length();
    for ( int i = 0; i < len; i++ )
    {
      sb.append( String.format( "%02X", addr.getData()[ len - i - 1 ] ) );
    }
    return sb.toString();
  }
  
  public static Hex getAddrHex( String addr )
  {  
    StringBuilder sb = new StringBuilder();
    int len = addr.length() / 2;
    for ( int i = 0; i < len; i++ )
    {
      if ( i > 0 ) sb.append( " " );
      sb.append( addr.substring( 2*(len-i-1), 2*(len-i) ) );
    }
    return new Hex( sb.toString() );
  }
  
  public static File getPSDFile( Component parent )
  {
    File path = preferences.getRFPath();
    File file = null;
    RMFileChooser chooser = new RMFileChooser( path );
    try
    {
      chooser.setAcceptAllFileFilterUsed( false );
    }
    catch ( Exception e )
    {
      e.printStackTrace( System.err );
    }
    EndingFileFilter filter = null;
    filter = new EndingFileFilter( "RF Packet Sniffer files (*.psd)", RemoteMaster.snifferEndings );
    chooser.addChoosableFileFilter( filter );
    chooser.setFileFilter( filter );

    int returnVal = chooser.showOpenDialog( parent );
    if ( returnVal == RMFileChooser.APPROVE_OPTION )
    {
      file = chooser.getSelectedFile();

      if ( !file.exists() )
      {
        JOptionPane.showMessageDialog( parent, file.getName() + " doesn't exist.", "File doesn't exist.",
            JOptionPane.ERROR_MESSAGE );
      }
      else if ( file.isDirectory() )
      {
        JOptionPane.showMessageDialog( parent, file.getName() + " is a directory.", "File doesn't exist.",
            JOptionPane.ERROR_MESSAGE );
      }
      else
      {
        preferences.setRFPath( file.getParentFile() );
      }
    }
    return file;
  }

  private final static int PSDENTRYSIZE = 151; // from packet sniffer documentation

  private List< RfRemote > rfRemotesList = null;
  private JToolBar toolBar = null;
  private RFAction openAction= null;
  private JMenuItem exitItem = null;
  private JMenuItem deleteRegItem = null;
  private JMenuItem renameRegItem = null;
  private RfCapturePanel capturePanel = null;
  private RfRemotePanel remotePanel = null;
  
}
