package com.hifiremote.jp1;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

public class ProtocolManager
{

  public static class QualifiedID
  {
    public Hex pid = null;
    public String variantName = "";
    
    public QualifiedID( Hex pid, String variantName )
    {
      this.pid = pid;
      if ( variantName == null || variantName.trim().isEmpty() )
        return;
      this.variantName = variantName.trim();
    }
    
    public QualifiedID( String reference )
    {
      variantName = "";
      if ( reference == null )
      {
        pid = null;
        return;
      }
      int colon = reference.indexOf( ':' );
      if ( colon != -1 )
      {
        variantName = reference.substring( colon + 1 ).trim();
        reference = reference.substring( 0, colon );
      }
      pid = new Hex( reference );
    }
    
    public String toReference()
    {
      if ( pid == null )
        return null;
      String ref = pid.toString().replace( " ", "" );
      if ( variantName != null && !variantName.trim().isEmpty() )
        ref += ":" + variantName.trim();
      return ref;
    }
    
    @Override
    public boolean equals( Object obj )
    {
      if ( obj == null )
        return false;
      QualifiedID q = ( QualifiedID )obj;
      return q.pid.equals( pid ) && q.variantName.equals( variantName );
    }
  }

  /**
   * Instantiates a new protocol manager.
   */
  protected ProtocolManager()
  {}

  /**
   * Gets the protocol manager.
   * 
   * @return the protocol manager
   */
  public static ProtocolManager getProtocolManager()
  {
    return protocolManager;
  }

  /**
   * Load.
   * 
   * @param f
   *          the f
   * @throws Exception
   *           the exception
   */
  public void load( File f, PropertyFile properties ) throws Exception
  {
    if ( loaded )
    {
      return;
    }

    while ( !f.canRead() )
    {
      JOptionPane.showMessageDialog( null, "Couldn't read " + f.getName() + "!", "Error", JOptionPane.ERROR_MESSAGE );
      RMFileChooser chooser = new RMFileChooser( f.getParentFile() );
      chooser.setFileSelectionMode( RMFileChooser.FILES_ONLY );
      chooser.setDialogTitle( "Pick the file containing the protocol definitions" );
      int returnVal = chooser.showOpenDialog( null );
      if ( returnVal != RMFileChooser.APPROVE_OPTION )
      {
        System.exit( -1 );
      }
      else
      {
        f = chooser.getSelectedFile();
      }
    }
    
    System.err.println( "Loading protocols from '" + f.getAbsolutePath() + "'" );
    codeMap.clear();
    extra = false;
    showSlingboxProtocols = Boolean.parseBoolean( properties.getProperty( "ShowSlingboxProtocols", "false" ) );
    System.err.println( "Option to include Slingbox protocols is " + ( showSlingboxProtocols ? "On" : "Off" ) );
    loadProtocolFile( f, false );
    File addonDir = RemoteMaster.getAddonDir();
    if ( addonDir.exists() && addonDir.isDirectory() )
    {
      File[] files = addonDir.listFiles( new FilenameFilter() 
      {
        public boolean accept( File dir, String name )
        {
          return name.toLowerCase().endsWith( ".prot" );
        }
      } );
      for ( File protFile : files )
      {
        System.err.println( "Loading add-on protocols from '" + protFile.getAbsolutePath() + "'" );
        loadProtocolFile( protFile, true );
      }
    }
    
    oldRefMap.clear();
    for ( List< Protocol > pList : byName.values() )
    {
      for ( Protocol p : pList )
      {
        // The check on alt pids is commented out as of the four used in protocols.ini, three
        // conflict.  It seems better to leave them than to remove this protocol feature.
        //
        // Hex altPid = p.getAlternatePID();
        // if ( altPid != null && byPID.keySet().contains( altPid ) )
        // {
        //   System.err.println( "**** Warning: Alternate PID " + altPid + " conflicts with a main PID of this value" );
        // }
        
        for ( String oldName : p.getOldNames() )
        {
          if ( byName.keySet().contains( oldName ) )
          {
            System.err.println( "**** Warning:  Protocol old name " + oldName + " is also in use as a current name" );
          }
        }
        
        int cmdLen = p.getDefaultCmdLength();
        int devLen = p.getFixedDataLength();
        for ( String proc : p.getCode().keySet() )
        {
          Hex code = p.getCode().get( proc );
          if ( Protocol.getCmdLengthFromCode( proc, code ) != cmdLen 
              || Protocol.getFixedDataLengthFromCode( proc, code ) != devLen )
          {
            String pvName = p.getVariantName();
            Hex id = p.getID();
            System.err.println( "**** Warning: Inconsistent cmd or fixed data lengths for protocol "
                + "with PID " + id + " and variantName " + ( pvName.isEmpty() ? "null" : pvName ) );
            break;
          }
        }
        if  ( p.getOldRefList() != null )
        {
          String newRef = ( new QualifiedID( p.getID(), p.getVariantName() )).toReference();
          for ( String oldRef : p.getOldRefList() )
          {
            if ( oldRefMap.containsKey( oldRef ) )
              System.err.println( "**** Warning: multiple protocols with old reference " + oldRef );
            QualifiedID qid = new QualifiedID( oldRef );
            List< Protocol > qList = byPID.get( qid.pid );
            if ( qList != null )
            {
              for ( Protocol q : qList )
              {
                String qvName = q.getVariantName().trim();
                if ( ( qvName == null || qvName.isEmpty() ) && qid.variantName.isEmpty() || qvName != null && qvName.equalsIgnoreCase( qid.variantName ) )
                  System.err.println( "**** Warning:  old reference " + oldRef + " is current reference of protocol " + q.getName() );
                break;
              }
            }
            oldRefMap.put( oldRef, newRef );
          }
        }
      }
    }
    
    ManualProtocol manualProtocol = new ManualProtocol( new Hex( "FF FF" ), null );
    manualProtocol.setName( manualProtocol.getName() );
    add( manualProtocol );
    extra = true;

    if ( byName.size() < 2 )
    {
      JOptionPane.showMessageDialog( null, "No protocols were loaded!", "Error", JOptionPane.ERROR_MESSAGE );
      System.exit( -1 );
    }

    // Sort the names array
    String[] temp = new String[ 0 ];
    temp = names.toArray( temp );
    Arrays.sort( temp );
    names = new ArrayList< String >( temp.length );
    for ( int i = 0; i < temp.length; i++ )
    {
      names.add( temp[ i ] );
    }

    loaded = true;
  }
  
  private void loadProtocolFile( File f, boolean deleteConflicting )
  {
    Properties props = null;
    String name = null;
    Hex id = null;
    String type = null;
    String variant = null;
    boolean suppress = false;
    int lineNumber = 0;
    extra = false;
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter( sw );
    
    try
    {
      LineNumberReader rdr = new LineNumberReader( new FileReader( f ) );
      rdr.setLineNumber( 1 );
      while ( true )
      {
        String line = rdr.readLine();
        String rawLine = line;
        lineNumber = rdr.getLineNumber();
        if ( line == null )
        {
          break;
        }

        line = line.trim();

        if ( line.length() == 0 || line.charAt( 0 ) == '#' )
        {
          continue;
        }
        suppress = line.startsWith( "Code." ) || line.startsWith( "[" )
            || line.startsWith( "PID" ) || line.startsWith( "VariantName" );
        
        line = line.replaceAll( "\\\\n", "\n" );
        line = line.replaceAll( "\\\\t", "\t" );
        while ( line.endsWith( "\\" ) )
        {
          String temp = rdr.readLine();
          if ( !suppress )
          {
            pw.println( rawLine );
            rawLine = temp;
          }
          temp = temp.trim();
          temp = temp.replaceAll( "\\\\n", "\n" );
          temp = temp.replaceAll( "\\\\t", "\t" );
          line = line.substring( 0, line.length() - 1 ) + temp;
        }

        if ( line.charAt( 0 ) == '[' ) // begin new protocol
        {
          // Add the previous protocol, if any
          variant = props != null ? props.getProperty( "VariantName", "" ) : "";
          if ( name != null && ( showSlingboxProtocols || !variant.equalsIgnoreCase( "slingbox" ) ) )
          {
            Protocol protocol = ProtocolFactory.createProtocol( name, id, type, props );
            protocol.iniIntro = sw.toString();
            pw.close();
            if ( protocol != null )
            { 
              addWithConflictCheck( protocol, deleteConflicting );
            }
          }
          // Now start the new one
          sw = new StringWriter();
          pw = new PrintWriter( sw );
          if ( !suppress )
          {
            pw.println( rawLine );
          }
          name = line.substring( 1, line.length() - 1 ).trim();
          props = new Properties();
          id = null;
          type = "Protocol";
        }
        else
        {
          if ( !suppress )
          {
            pw.println( rawLine );
          }
          StringTokenizer st = new StringTokenizer( line, "=", true );
          String parmName = st.nextToken().trim();
          String parmValue = null;
          st.nextToken(); // skip the =
          if ( !st.hasMoreTokens() )
          {
            continue;
          }
          else
          {
            parmValue = st.nextToken( "" ); // .trim();
          }

          if ( parmName.equals( "PID" ) )
          {
            id = new Hex( parmValue );
          }
          else if ( parmName.equals( "Type" ) )
          {
            type = parmValue;
          }
          else
          {
            props.setProperty( parmName, parmValue );
          }
        }
      }
      rdr.close();
    }
    catch ( Exception e )
    {
      System.err.println( "Error in reading protocol from file " + f.getName() + " at line " + lineNumber );
      return;
    }
    
    // Now add the final protocol
    variant = props != null ? props.getProperty( "VariantName", "" ) : "";
    if ( name != null && ( showSlingboxProtocols || !variant.equalsIgnoreCase( "slingbox" ) ) )
    {
      Protocol protocol = ProtocolFactory.createProtocol( name, id, type, props );
      protocol.iniIntro = sw.toString();
      pw.close();
      if ( protocol != null )
      {
        addWithConflictCheck( protocol, deleteConflicting );
      }
    }
  }
  
  private void addWithConflictCheck( Protocol protocol, boolean deleteConflicting )
  {
    String name = protocol.getName();
    String vn = protocol.getVariantName();
    if ( deleteConflicting && findByName( name ) != null )
    {
      for ( Iterator< Protocol > ip = findByName( name ).iterator(); ip.hasNext(); )
      {
        Protocol p = ip.next();
        String pvn = p.getVariantName();
        if ( vn == null && pvn != null 
            || vn != null && !vn.equalsIgnoreCase( pvn ) )
        {
          continue;
        }
        for ( String codeName : protocol.getCode().keySet() )
        {
          if ( p.getCode().get( codeName ) != null )
          {
            ip.remove();;
            break;
          }
        }
      }
    }
    add( protocol );
  }

  /**
   * Adds the.
   * 
   * @param p
   *          the p
   */
  public void add( Protocol p )
  {
    /*
     * if ( p.getClass() == ManualProtocol.class ) { manualProtocol = ( ManualProtocol )p; return; }
     */

    // Add the protocol to the byName hashtable
    String name = p.getName();
    List< Protocol > v = byName.get( name );
    if ( v == null )
    {
      v = new ArrayList< Protocol >();
      byName.put( name, v );
      names.add( name );
    }
    v.add( p );

    // add the protocol to the byPID hashtable
    Hex id = p.getID();
    String pvName = p.getVariantName();
    QualifiedID qid = codeMapKey( new QualifiedID( id, pvName ) );
    v = byPID.get( id );
    if ( v == null )
    {
      v = new ArrayList< Protocol >();
      byPID.put( id, v );
    }
    else if ( !loaded )
    {
      // This error check is only made during loading of protocols.ini and any 
      // add-on .prot files, during which "loaded" is false.
      for ( Protocol tryit : v )
      {
        String tryName = tryit.getVariantName();
        if ( ( pvName == null && tryName == null || pvName != null && pvName.equals( tryName ) )
            && !testProtocolCode( qid, p ) )
        {
          System.err.println( "**** Warning: protocols with PID " + id + " and variantName " 
              + ( pvName.isEmpty() ? "null" : pvName ) + " have conflicting code" );
          break;
        }
      }
    }
    v.add( p );
    if ( !codeMap.containsKey( qid ) )
    {
      codeMap.put( qid, new HashMap< String, Hex >() );
    }
    HashMap< String, Hex > cm = codeMap.get( qid );
    for ( String proc : p.getCode().keySet() )
      if ( !cm.containsKey( proc ) )
        cm.put( proc, p.getCode().get( proc ) );
       
    if ( p instanceof ManualProtocol )
    {
      int nameIndex = ( ( ManualProtocol )p ).getNameIndex();
      Integer index = manualSettingsIndex.get( id );
      if ( nameIndex > 0 && ( index == null || nameIndex > index) )
      {
        manualSettingsIndex.put( id, nameIndex );
      }
    }   

    id = p.getAlternatePID();
    if ( id != null )
    {
      v = byAlternatePID.get( id );
      if ( v == null )
      {
        v = new ArrayList< Protocol >();
        byAlternatePID.put( id, v );
      }
      v.add( p );
    }
    
    if ( extra )
    {
      extras.add( p );
    }
  }
  
  public void remove( Protocol p )
  {
    String name = p.getName();
    List< Protocol > vn = byName.get( name );
    Hex id = p.getID();
    List< Protocol > vp = byPID.get( id );
    if ( vn == null || vp == null )
    {
      return;
    }
    
    if ( vn.size() == 1 )
    {
      names.remove( name );
      byName.remove( name );
    }
    else
    {
      vn.remove( p );
    }
  
    if ( vp.size() == 1 )
    {
      byPID.remove( id );
    }
    else
    {
      vp.remove( p );
    }
    
    if ( p instanceof ManualProtocol )
    {
      int nameIndex = ( ( ManualProtocol )p ).getNameIndex();
      Integer index = manualSettingsIndex.get( id );
      if ( index != null && nameIndex == index )
      {
        // Reset manualSettingsIndex to largest index remaining after p deleted
        vp = byPID.get( id ); // Value after removal of p
        if ( vp == null )
        {
          manualSettingsIndex.remove( id );
        }
        else
        {
          index = 0;
          for ( Protocol pp : vp )
          {
            if ( pp instanceof ManualProtocol )
            {
              nameIndex = ( ( ManualProtocol )pp ).getNameIndex();
              if ( nameIndex > index ) index = nameIndex;
            }
          }
          if ( index == 0 )
          {
            manualSettingsIndex.remove( id );
          }
          else
          {
            manualSettingsIndex.put( id, index );
          }
        }
      }
    }   

    id = p.getAlternatePID();
    if ( id != null )
    {
      vp = byAlternatePID.get( id );
      if ( vp != null && vp.size() == 1 )
      {
        byAlternatePID.remove( id );
      }
      else if ( vp != null )
      {
        vp.remove( p );
      }
    }
    
    Hashtable< String, List< Hex >> rp = p.getRemoteAltPIDs();
    for ( String sig : rp.keySet() )
    {
      for ( Hex hex : rp.get( sig ) )
      {
        if ( byAltPIDRemote.get( sig ) != null && byAltPIDRemote.get( sig ).get( hex ) != null )
        {
          byAltPIDRemote.get( sig ).get( hex ).remove( p );
          if ( byAltPIDRemote.get( sig ).get( hex ).size() == 0 )
          {
            byAltPIDRemote.get( sig ).remove( hex );
          }
          if ( byAltPIDRemote.get( sig ).size() == 0 )
          {
            byAltPIDRemote.remove( sig );
          }
        }
      }
    }
    
    if ( extras.contains( p ) )
    {
      extras.remove( p );
    }
  }

  /**
   * Gets the names.
   * 
   * @return the names
   */
  public List< String > getNames()
  {
    return names;
  }

  public LinkedHashMap< String, String > getOldRefMap()
  {
    return oldRefMap;
  }

  /**
   * Gets the protocols for remote.
   * 
   * @param remote
   *          the remote
   * @return the protocols for remote
   */
  public List< Protocol > getProtocolsForRemote( Remote remote )
  {
    return getProtocolsForRemote( remote, true );
  }

  /**
   * Gets the protocols for remote.
   * 
   * @param remote
   *          the remote
   * @param allowUpgrades
   *          the allow upgrades
   * @return the protocols for remote
   */
  public List< Protocol > getProtocolsForRemote( Remote remote, boolean allowUpgrades )
  {
    List< Protocol > rc = new ArrayList< Protocol >();
    for ( String name : names )
    {
      Protocol p = findProtocolForRemote( remote, name, allowUpgrades );
      if ( p != null )
      {
        rc.add( p );
      }
    }
    /*
     * if ( allowUpgrades && manualProtocol.hasCode( remote )) rc.add( manualProtocol );
     */
    return rc;
  }

  public Hashtable< String, List< Protocol >> getByName()
  {
    return byName;
  }
  
  public Hashtable< Hex, List< Protocol >> getByPID()
  {
    return byPID;
  }

  public Hashtable< String, List< Protocol >> getByNameForRemote( Remote remote )
  {
    Hashtable< String, List< Protocol >> byNameForRemote = new Hashtable< String, List< Protocol >>();
    for ( Protocol p : getProtocolsForRemote( remote ) )
    {
      byNameForRemote.put( p.getName(), Arrays.asList( p ) );
    }
    return byNameForRemote;
  }
  

  /**
   * Find by name.
   * 
   * @param name
   *          the name
   * @return the list< protocol>
   */
  public List< Protocol > findByName( String name )
  {
    List< Protocol > v = byName.get( name );
    /*
     * if (( v == null ) && name.equals( manualProtocol.getName())) { v = new ArrayList< Protocol >(); v.add(
     * manualProtocol ); }
     */
    return v;
  }

  /**
   * Find by pid.
   * 
   * @param id
   *          the id
   * @return the list< protocol>
   */
  public List< Protocol > findByPID( Hex id )
  {
    List< Protocol > rc = null;
    List< Protocol > list = byPID.get( id );
    if ( list == null )
    {
      rc = new ArrayList< Protocol >( 0 );
    }
    else
    {
      rc = new ArrayList< Protocol >( list.size() );
      rc.addAll( list );
    }
    return rc;
  }

  public List< Protocol > getBuiltinProtocolsForRemote( Remote remote, Hex pid )
  {
    List< Protocol > results = new ArrayList< Protocol >();
    for ( Protocol protocol : findByPID( pid ) )
    {
      if ( remote.supportsVariant( pid, protocol.getVariantName() ) )
      {
        results.add( protocol );
      }
    }
    return results;
  }

  /**
   * Find by alternate pid.
   * 
   * @param id
   *          the id
   * @return the list< protocol>
   */
  public List< Protocol > findByAlternatePID( Remote remote, Hex id )
  {
    return findByAlternatePID( remote, id, false );
  }
  
  
  public List< Protocol > findByAlternatePID( Remote remote, Hex id, boolean checkUserAltPIDs )
  {
    List< Protocol > list = new ArrayList< Protocol >();
    List< Protocol > l = byAlternatePID.get( id );
    if ( l != null ) list.addAll( l );
    if ( checkUserAltPIDs && byAltPIDRemote.get( remote.getSignature() ) != null )
    {
      l = byAltPIDRemote.get( remote.getSignature() ).get( id );
      if ( l != null ) list.addAll( l );
    }
    return list.size() == 0 ? null : list;
  }
  
  public void putAltPIDRemote( Hex id, Remote remote, Protocol p )
  {
    Hashtable< Hex, List<Protocol> > table = byAltPIDRemote.get( remote.getSignature() );
    if ( table == null )
    {
      table = new Hashtable< Hex, List<Protocol> >();
      byAltPIDRemote.put( remote.getSignature(), table );
    }
    List< Protocol > list = table.get( id );
    if ( list == null )
    {
      list = new ArrayList< Protocol >();
      table.put( id, list );
    }
    if ( !list.contains( p ) )
    {
      list.add( p );
    }
  }
  
  public int countAltPIDRemoteEntries()
  {
    int n = 0;
    for ( String sig : byAltPIDRemote.keySet() )
    {
      Hashtable< Hex, List<Protocol> > table = byAltPIDRemote.get( sig );
      for ( Hex h : table.keySet() )
      {
        n += table.get( h ).size();
      }
    }
    return n;
  }
  
  public void clearAltPIDRemoteEntries()
  {
    for ( Hex h : byPID.keySet() )
    {
      for ( Protocol p : byPID.get( h ) )
      {
        p.getRemoteAltPIDs().clear();
      }
    }
    byAltPIDRemote.clear();
  }
  
  public void setAltPIDRemoteProperties( PropertyFile properties )
  {
    for ( String key : properties.stringPropertyNames() )
    {
      if ( key.startsWith( "RemoteAltPID" ) )
      {
        properties.remove( key );
      }
    }
    int n = 1;
    for ( String sig : byAltPIDRemote.keySet() )
    {
      Set< Protocol > prots = new HashSet< Protocol >();
      for ( Hex id : byAltPIDRemote.get( sig ).keySet() )
      {
        for ( Protocol p : byAltPIDRemote.get( sig ).get( id ) )
        {
          prots.add( p );
        }
      }

      for ( Protocol p : prots )
      {
        String key = "RemoteAltPID." + n++;
        String val = sig + " [" + p.getName() + "] ";
        for ( Hex id : p.getRemoteAltPIDs().get( sig ) )
        {
          val += id.toString() + " ";
        }
        val = val.substring( 0, val.length() - 1 );
        properties.setProperty( key, val );
      }
    }
  }
  
  public void loadAltPIDRemoteProperties( PropertyFile properties )
  {
    int n = 1;
    String value = null;
    while ( ( value = properties.getProperty( "RemoteAltPID." + n++ )) != null )
    {
      int pos = value.indexOf( " [" );
      if ( pos < 0 ) continue;    // should not occur
      String sig = value.substring( 0, pos ).trim();
      value = value.substring( pos + 2 );
      pos = value.indexOf( "] " );
      if ( pos < 0 ) continue;    // should not occur
      String pName = value.substring( 0, pos ).trim();
      value = value.substring( pos + 2 );
      List< Protocol > pList = byName.get( pName );
      if ( pList == null ) continue;
      Hex hex = new Hex( value );
      List< Remote > remotes = RemoteManager.getRemoteManager().findRemoteBySignature( sig );
      if ( remotes.size() == 0 ) continue;
      // Assume all remotes with same signature have same processor
      Remote remote = remotes.get( 0 );
      Iterator< Protocol > it = pList.iterator();
      while ( it.hasNext() )
      {
        if ( !it.next().hasCode( remote ) ) it.remove();
      }
      for ( Protocol p : pList )
      {
        for ( int i = 0; i < hex.length()/2; i++ )
        {
          p.putAlternatePID( remote, hex.subHex( 2 * i, 2 ) );
        }
      }
    }
  }

  /**
   * Find protocol for remote.
   * 
   * @param remote
   *          the remote
   * @param name
   *          the name
   * @return the protocol
   */
  public Protocol findProtocolForRemote( Remote remote, String name )
  {
    return findProtocolForRemote( remote, name, true );
  }

  /**
   * Find protocol for remote.
   * 
   * @param remote
   *          the remote
   * @param name
   *          the name
   * @param allowUpgrades
   *          the allow upgrades
   * @return the protocol
   */
  public Protocol findProtocolForRemote( Remote remote, String name, boolean allowUpgrades )
  {
    Protocol protocol = null;
    Protocol tentative = null;

    List< Protocol > protocols = findByName( name );
    if ( protocols == null )
    {
      return null;
    }
    for ( Protocol p : protocols )
    {
      if ( remote.supportsVariant( p.getID(), p.getVariantName() ) )
      {
        protocol = p;
        break;
      }

      if ( tentative == null )
      {
        if ( allowUpgrades && p.hasCode( remote ) )
        {
          tentative = p;
        }
      }
    }
    if ( protocol == null )
    {
      protocol = tentative;
    }

    return protocol;
  }

  /**
   * Find protocol for remote.
   * 
   * @param remote
   *          the remote
   * @param id
   *          the id
   * @param fixedData
   *          the fixed data
   * @return the protocol
   */
  public Protocol findProtocolForRemote( Remote remote, Hex id, Hex fixedData )
  {
    List< Protocol > protocols = protocolManager.findByPID( id );
    for ( Protocol p : protocols )
    {
      if ( !remote.supportsVariant( id, p.getVariantName() ) )
      {
        continue;
      }
      Value[] vals = p.importFixedData( fixedData );
      Hex calculatedFixedData = p.getFixedData( vals );
      if ( calculatedFixedData.equals( fixedData ) )
      {
        return p;
      }
    }
    return null;
  }

  /**
   * Find protocol for remote.
   * 
   * @param remote
   *          the remote
   * @param id
   *          the id
   * @return the protocol
   */
  public Protocol findProtocolForRemote( Remote remote, Hex id )
  {
    return findProtocolForRemote( remote, id, true );
  }

  /**
   * Find protocol for remote.
   * 
   * @param remote
   *          the remote
   * @param id
   *          the id
   * @param allowUpgrades
   *          the allow upgrades
   * @return the protocol
   */
  public Protocol findProtocolForRemote( Remote remote, Hex id, boolean allowUpgrades )
  {
    List< Protocol > protocols = findByPID( id );
    if ( protocols == null )
    {
      protocols = findByAlternatePID( remote, id );
    }

    if ( protocols == null )
    {
      return null;
    }
    
    if ( allowUpgrades )
    {
      // Within the remote, an upgrade for a particular PID takes precedence 
      // over a built-in protocol
      for ( Protocol p : protocols )
      {
        if ( p.getCustomCode( remote.getProcessor() ) != null
            && p.getID( remote ).equals( id ) )
        {  
          return p;
        }
      }
    }

    for ( Protocol p : protocols )
    {
      if ( remote.supportsVariant( id, p.getVariantName() ) )
      {
        return p;
      }
    }
    return null;
  }

  /**
   * Find protocol by old name.
   * 
   * @param remote
   *          the remote
   * @param name
   *          the name
   * @param pid
   *          the pid
   * @return the protocol
   */
  public Protocol findProtocolByOldName( Remote remote, String name, Hex pid )
  {
    Protocol matchByName = null;
    List< Protocol > protocols = getProtocolsForRemote( remote );
    if ( protocols == null )
    {
      return null;
    }
    for ( Protocol p : protocols )
    {
      for ( String oldName : p.getOldNames() )
      {
        if ( name.equals( oldName ) )
        {
          if ( matchByName == null )
          {
            matchByName = p;
          }
          if ( p.getID().equals( pid ) )
          {
            return p;
          }
        }
      }
    }

    return matchByName;
  }

  /**
   * Find protocol.
   * 
   * @param name
   *          the name
   * @param id
   *          the id
   * @param variantName
   *          the variant name
   * @return the protocol
   */
  public Protocol findProtocol( String name, Hex id, String variantName )
  {
    List< Protocol > protocols = findByPID( id );
    if ( protocols == null )
    {
      return null;
    }
    for ( Protocol p : protocols )
    {
      if ( p.getName().equals( name ) && p.getVariantName().equals( variantName ) )
      {
        return p;
      }
    }
    return null;
  }
  
  public Protocol createMissingProtocol( Hex pid, String variant, int fixedDataLength, int cmdLength )
  {
    System.err.println( "Creating Protocol Manager entry for missing built-in protocol" );
    List< Protocol > prots = findByPID( pid );
    String name = null;
    if ( !prots.isEmpty() )
    {
      // A protocol with this pid is in protocols.ini.  Treat as new variant of this protocol.
      name = prots.get( 0 ).getName();
    }
    else
    {
      // There is no protocol with this pid in protocols.ini, so use the name format
      // that protocols.ini uses for protocols with code that are otherwise unidentified.
      name = "pid: " + pid;
    }
    Properties props = new Properties();
    if ( variant.length() > 0 )
    {
      props.put( "VariantName", variant );
    }
    
    // Create a single temporary code entry just to enable ProtocolFactory to extract
    // fixed and variable data lengths.
    Hex tempCode = new Hex( 3 );
    tempCode.getData()[ 2 ] = ( short )( ( fixedDataLength << 4 ) | cmdLength );
    props.put( "Code.MAXQ610", tempCode.toString() );
    String notes = "This built-in protocol is missing from protocols.ini so although hex values "
        + "for fixed data and function commands is correct, device parameters and OBC "
        + "data are unreliable.";
    props.put(  "Notes", notes );
    Protocol p = ProtocolFactory.createProtocol( name, pid, "Protocol", props );
    // Delete the MAXQ610 code that ProtocolFactory will have created.
    p.code.clear();
    add( p );
    return p;
  }

  /**
   * Find nearest protocol.
   * 
   * @param name
   *          the name
   * @param id
   *          the id
   * @param variantName
   *          the variant name
   * @return the protocol
   */
  public Protocol findNearestProtocol( Remote remote, String name, Hex id, String variantName )
  {
    System.err
        .println( "ProtocolManager.findNearestProtocol( " + remote + ", " + name + ", " + id + ", " + variantName + " )" );
    Protocol near = null;
    Protocol derived = null;
    List< Protocol > protocols = findByPID( id );
    if ( protocols == null )
    {
      protocols = findByAlternatePID( remote, id );
    }
    if ( protocols == null )
    {
      System.err.println( "No protocol found" );
      return null;
    }
    
    for ( Protocol p : protocols )
    {    
      if ( ( variantName == null || variantName.equals( p.getVariantName() ) )
          && remote.supportsVariant( id, p.getVariantName() ) )
      {
        if ( p.getName().equals( name ) )
        {
          System.err.println( "Found built-in protocol " + p );
          return p;
        }
        else if ( derived == null && name.equals( "pid: " + id.toString() ) )
        {
          // Since protocols.ini contains protocols with this name form, continue to look for
          // name match and only use derived protocol if no match found
          System.err.println( "Recreating derived protocol from " + p );
          Properties props = new Properties();
          for ( Processor pr : ProcessorManager.getProcessors() )
          {
            Hex hCode = p.getCode( pr );
            if ( hCode != null )
            {
              props.put( "Code." + pr.getEquivalentName(), hCode.toString() );
            }
          }
          String variant = p.getVariantName();
          if ( variant != null && variant.length() > 0 )
          {
            props.put( "VariantName", variant );
          }
          derived = ProtocolFactory.createProtocol( "pid: " + id.toString(), id, "Protocol", props );
        }
      }
      
      if ( p.getName().equals( name ) && p.hasCode( remote ) 
          && ( near == null || !near.getVariantName().equals( variantName )
              && p.getVariantName().equals( variantName ) ) )
      {
        near = p;
      }      
    }
    
    if ( derived != null )
    {
      // No name match found, so use derived protocol
      ProtocolManager.getProtocolManager().add( derived );
      System.err.println( "Using recreated protocol " + derived );
      return derived;
    }
    
    if ( remote.supportsVariant( id, variantName ) )
    {
      // Built-in protocol missing from protocols.ini
      System.err.println( "Protocol is built-in but missing from protocols.ini" );
      return null;
    }
    
    if ( near != null )
    {
      System.err.println( "Found protocol " + near );
      return near;
    }
    protocols = findByName( name );
    if ( protocols != null )
    {
      near = protocols.get( 0 );
    }
    if ( near != null )
    {
      System.err.println( "Found protocol " + near );
      return near;
    }
    near = findProtocolByOldName( remote, name, id );
    if ( near != null )
    {
      System.err.println( "Found protocol " + near );
      return near;
    }    
    System.err.println( "No protocol found" );
    return null;
  }
  
  public QualifiedID getCurrentQID( Hex pid, String variantName )
  {
    QualifiedID qid = new QualifiedID( pid, variantName );
    String newRef = oldRefMap.get( qid.toReference() );
    return newRef == null ? qid : new QualifiedID( newRef );
  }
  
  public Hex getCurrentPID( String name, Hex pid )
  {
    // This is needed only for old KM upgrades and PB protocols where the .txt file
    // does not contain a variant name.
    Hex temp = testProtocolList( byName.get( name ), pid );
    if ( temp != null )
      return temp;
    
    for ( List< Protocol > pList : byName.values() )
    {
      for ( Protocol p : pList )
      {
        if  ( p.getOldNames().contains( name ) )
        {
          temp = testProtocolList( pList, pid );
          if ( temp != null )
            return temp;
        }
      }
    }
    return pid;
  }
        
  private Hex testProtocolList( List< Protocol > pList, Hex pid )
  {
    Hex temp = null;
    if ( pList == null )
      return null;
    for ( Protocol p : pList )
    {
      if ( p.getID().equals( pid ) )
        return pid;
      List< String > oldRefs = p.getOldRefList();
      if ( temp == null && oldRefs != null )
      {
        for ( String ref : oldRefs )
        {
          QualifiedID qid = new QualifiedID( ref );
          if ( qid.pid.equals( pid ) )
            temp = p.getID();
        }
      }
    }
    return temp;
  }
  
  private QualifiedID codeMapKey( QualifiedID qid )
  {
    // This workaround seems necessary as codeMap.get(..) does not seem to use the
    // override of equals defined in QualifiedID 
    for ( QualifiedID q : codeMap.keySet() )
    {
      if ( q.equals( qid ) )
      {
        return q;
      }
    }
    return qid;
  }
  
  private boolean testProtocolCode( QualifiedID qid, Protocol p )
  {
    HashMap< String, Hex > cMap = codeMap.get( codeMapKey( qid ) );
    if ( cMap == null )
      return true;
    
    HashMap< String, Hex > pMap = p.getCode();
    for ( String proc : pMap.keySet() )
    {
      if ( cMap.containsKey( proc ) && !cMap.get( proc ).equals( pMap.get( proc ) ) )
        return false;
    }
    return true;
  }
  
  
  
  public static int getManualSettingsIndex( Hex pid )
  {
    Integer index = manualSettingsIndex.get( pid );
    return ( index == null ) ? 0 : index;
  }
  
  /**
   * A selective reset that only removes protocols whose pid is in the given list.
   */
  public void reset( List< Integer > pids )
  {
    // Remove extra protocols.  Clone first as extras is modified by remove().
    List< Protocol > extrasClone = new ArrayList< Protocol >( extras );
    for ( Protocol p : extrasClone )
    {
      if ( pids.contains( p.getID().get( 0 ) ) )
      {
        remove( p );
      }
    }
    // Remove custom code
    for ( List< Protocol > l : byName.values() )
    {
      for ( Protocol p : l )
      {
        if ( pids.contains( p.getID().get( 0 ) ) )
        {
          p.customCode.clear();
        }
      }
    }
  }
  
  public void reset()
  {
    // Remove extra protocols.  Clone first as extras is modified by remove().
    List< Protocol > extrasClone = new ArrayList< Protocol >( extras );
    for ( Protocol p : extrasClone )
    {
      remove( p );
    }
    // Remove all custom code
    for ( List< Protocol > l : byName.values() )
    {
      for ( Protocol p : l )
      {
        p.customCode.clear();
      }
    }
    // Reset all manual settings indexes
    manualSettingsIndex.clear();
  }
  
  /*
   * public ManualProtocol getManualProtocol() { System.err.println( "ProtocolManager.getManualProtocol(): " +
   * manualProtocol ); return manualProtocol; }
   */
  /** The protocol manager. */
  private static ProtocolManager protocolManager = new ProtocolManager();
  // private static ManualProtocol manualProtocol = null;
  /** The loaded. */
  private boolean loaded = false;

  /** The names. */
  private List< String > names = new ArrayList< String >();

  /** The by name. */
  private Hashtable< String, List< Protocol >> byName = new Hashtable< String, List< Protocol >>();

  /** The by pid. */
  private Hashtable< Hex, List< Protocol >> byPID = new Hashtable< Hex, List< Protocol >>();

  /** The by alternate pid. */
  private Hashtable< Hex, List< Protocol >> byAlternatePID = new Hashtable< Hex, List< Protocol >>();
  
  /** By remote-specific alt PID, remote keyed by signature */
  private Hashtable< String, Hashtable< Hex, List< Protocol > > > byAltPIDRemote = new Hashtable< String, Hashtable< Hex, List<Protocol > > >();

  private HashMap< QualifiedID, HashMap< String, Hex > > codeMap = new HashMap< QualifiedID, HashMap<String,Hex> >();
  
  private LinkedHashMap< String, String > oldRefMap = new LinkedHashMap< String, String >();
  
  private boolean extra = true;
  private boolean showSlingboxProtocols = false;
  
  private List< Protocol > extras = new ArrayList< Protocol >();
  
  /** An index for each manual protocol PID that is maintained by add(Protocol) and
   *  that can be used to create a unique default name even with multiple such protocols
   *  with the same PID.
   */
  private static Hashtable< Hex, Integer > manualSettingsIndex = new Hashtable< Hex, Integer >();
}
