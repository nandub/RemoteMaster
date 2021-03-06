package com.hifiremote.jp1;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import com.hifiremote.jp1.GeneralFunction.RMIcon;
import com.hifiremote.jp1.ProtocolManager.QualifiedID;

// TODO: Auto-generated Javadoc
/**
 * The Class Remote.
 */
public class Remote implements Comparable< Remote >
{

  public class KeyButtonGroup
  {
    public String name;
    public ButtonShape shape;
    public List< Button > buttons;

    public KeyButtonGroup( String name, List< Button > buttons )
    {
      this.name = name;
      this.buttons = buttons;
    }
    
    public void setButtonShape()
    {
      if ( shape != null || buttons == null )
        return;
      
      ImageMap map = getImageMaps( getDeviceTypes()[ 0 ] )[ 0 ];
      List< ButtonShape > shapes = map.getShapes();
      for ( ButtonShape bs : shapes )
      {
        if ( buttons.contains( bs.getButton() ) && !getPhantomShapes().contains( bs ) )
        {
          shape = bs;
          return;
        }
      }
    }
  }
  
  public static class RFSelector
  {
    public Button btn = null;
    public DeviceType irDevType = null;
    public SetupCode irCode = null;
    public DeviceType rfDevType = null;
    public SetupCode rfCode = null;
  }

  public enum SetupValidation
  {
    OFF, WARN, ENFORCE
  };
  
  public enum BlockFormat
  {
    NO, YES, DEFAULT
  }

  /**
   * Instantiates a new remote.
   * 
   * @param aRemote
   *          the a remote
   * @param index
   *          the index
   */
  public Remote( Remote aRemote, int index )
  {
    this.file = aRemote.file;
    this.signature = aRemote.signature;
    supportsBinaryUpgrades = aRemote.supportsBinaryUpgrades;
    this.names = aRemote.names;
    nameIndex = index;
  }

  public Remote()
  {};

  /**
   * Instantiates a new remote.
   * 
   * @param rdf
   *          the rdf
   */
  public Remote( File rdf )
  {
    file = rdf;
    String rdfName = rdf.getName();
    StringTokenizer st = new StringTokenizer( rdfName );
    signature = st.nextToken(); // upto the 1st space
    supportsBinaryUpgrades = signature.startsWith( "BIN" );
    int openParen = rdfName.indexOf( '(' );
    int closeParen = rdfName.lastIndexOf( ')' );
    String name = rdfName.substring( openParen + 1, closeParen );
    st = new StringTokenizer( name, " -", true );
    String prefix = "";
    String postfix = "";
    String[] middles = null;
    boolean foundUnderscore = false;
    while ( st.hasMoreTokens() )
    {
      String token = st.nextToken();
      if ( token.length() > 3 && token.indexOf( '_' ) != -1 )
      {
        foundUnderscore = true;
        StringTokenizer st2 = new StringTokenizer( token, "_" );
        middles = new String[ st2.countTokens() ];
        for ( int i = 0; i < middles.length; i++ )
        {
          middles[ i ] = st2.nextToken();
        }
      }
      else
      {
        token = token.replace( '_', '/' );
        if ( foundUnderscore )
        {
          postfix = postfix + token;
        }
        else
        {
          prefix = prefix + token;
        }
      }
    }
    if ( middles == null )
    {
      names[ 0 ] = prefix;
    }
    else
    {
      names = new String[ middles.length ];
      for ( int i = 0; i < middles.length; i++ )
      {
        if ( middles[ i ].length() < middles[ 0 ].length() )
        {
          names[ i ] = middles[ i ] + postfix;
        }
        else
        {
          names[ i ] = prefix + middles[ i ] + postfix;
        }
      }
    }
  }

  /**
   * Gets the file.
   * 
   * @return the file
   */
  public File getFile()
  {
    return file;
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  /**
   * Load.
   */
  public void load()
  // throws Exception
  {
    try
    {
      if ( loaded )
      {
        SetupCode.setMax( this );
        KeyMove.setSetupCodeIndex( segmentTypes == null ? 0 : 1 );
        KeyMove.setCmdIndex( segmentTypes == null ? 2 : 3 );
        return;
      }
      loaded = true;
      settingAddresses.clear();
      settingMasks.clear();
      settingBytes.clear();
      RDFReader rdr = new RDFReader( file );
      String line = rdr.readLine();
      while ( line != null )
      {
        if ( line.length() == 0 )
        {
          line = rdr.readLine();
        }
        else if ( line.charAt( 0 ) == '[' )
        {
          StringTokenizer st = new StringTokenizer( line, "[]" );
          line = st.nextToken();

          if ( line.equals( "General" ) || line.equals( "General+" ) )
          {
            line = parseGeneralSection( rdr );
          }
          else if ( line.equals( "Extender" ) )
          {
            line = parseExtender( rdr );
          }
          else if ( ( line.equals( "SpecialProtocols" ) || line.equals( "SpecialProtocols+" ) )
              && specialProtocols.isEmpty() )
          {
            line = parseSpecialProtocols( rdr );
          }
          else if ( line.equals( "Checksums" ) )
          {
            line = parseCheckSums( rdr );
          }
          else if ( line.equals( "Settings" ) || line.equals( "Settings+" ) )
          {
            line = parseSettings( rdr );
          }
          else if ( ( line.equals( "FixedData" ) || line.equals( "FixedData+" ) )
              && fixedData.length == 0 )
          {
            fixedData = FixedData.parse( rdr );
            rawFixedData = fixedData;
            line = "";
          }
          else if ( line.equals( "AutoSet" ) )
          {
            autoSet = FixedData.parse( rdr );
            line = "";
          }
          else if ( ( line.equals( "DeviceButtons" ) || line.equals( "DeviceButtons+" ) ) && deviceButtons.length == 0 )
          {
            line = parseDeviceButtons( rdr );
          }
          else if ( line.equals( "DigitMaps" ) )
          {
            line = parseDigitMaps( rdr );
          }
          else if ( ( line.equals( "DeviceTypes" ) || line.equals( "DeviceTypes+" ) )
              && deviceTypeList.isEmpty() )
          {
            line = parseDeviceTypes( rdr );
          }
          else if ( line.equals( "DeviceAbbreviations" ) )
          {
            line = parseDeviceAbbreviations( rdr );
          }
          else if ( line.equals( "DeviceTypeAliases" ) )
          {
            line = parseDeviceTypeAliases( rdr );
          }
          else if ( line.equals( "DeviceTypeImageMaps" ) )
          {
            line = parseDeviceTypeImageMaps( rdr );
          }
          else if ( line.equals( "Buttons" ) )
          {
            line = parseButtons( rdr );
          }
          else if ( line.equals( "MultiMacros" ) )
          {
            line = parseMultiMacros( rdr );
          }
          else if ( line.equals( "ButtonMaps" ) )
          {
            line = parseButtonMaps( rdr );
          }
          else if ( line.equals( "Protocols" ) )
          {
            line = parseProtocols( rdr );
          }
          else if ( line.equals( "SetupCodes" ) || line.equals( "SetupCodes+" ) )
          {
            line = parseSetupCodes( rdr );
          }
          else if ( line.equals( "ActivityControl" ) )
          {
            line = parseActivityControl( rdr );
          }
          else
          {
            line = rdr.readLine();
          }
        }
        else
        {
          line = rdr.readLine();
        }
      }
      rdr.close();
      
      if ( specialProtocols.size() > 0 )
      {
        int maxSerial = 0;
        for ( SpecialProtocol sp : specialProtocols )
        {
          if ( sp.isInternal() && maxSerial < sp.getInternalSerial() )
          {
            maxSerial = sp.getInternalSerial();
          }
        }
        if ( maxSerial > 0 )
        {
          int serialMask = ( maxSerial < 2 ) ? 0x01 : ( maxSerial < 4 ) ? 0x03 : 
            ( maxSerial < 8 ) ? 0x07 : 0x0F;
          
          int dbMask = 0;
          for ( DeviceButton db : deviceButtons )
          {
            dbMask |= db.getButtonIndex();
          }
          if ( ( dbMask & ( serialMask << 4 ) ) == 0 )
          {
            seqShift = 4;
          }
          else if ( ( dbMask & ( serialMask << 3 ) ) == 0 )
          {
            seqShift = 3;
          }
          else
          {
            String title = "Special Function Error";
            String message = "Error in RDF.  This remote cannot support an internal Special Function\n"
                + "with a serial value " + maxSerial;
            JOptionPane.showMessageDialog( null, message, title, JOptionPane.WARNING_MESSAGE );
          }
        }
      }

      if ( buttonMaps.length == 0 )
      {
        System.err.println( "ERROR: " + file.getName() + " does not specify any ButtonMaps!" );
        buttonMaps = new ButtonMap[ 1 ];
        buttonMaps[ 0 ] = new ButtonMap( 0, new short[ 0 ][ 0 ] );
      }
      for ( int i = 0; i < buttonMaps.length; i++ )
      {
        buttonMaps[ i ].setButtons( this );
      }

      for ( DeviceType type : deviceTypes.values() )
      {
        int map = type.getMap();
        if ( map == -1 )
        {
          System.err.println( "ERROR:" + file.getName() + ": DeviceType " + type.getName() + " doesn't have a map." );
        }
        if ( map >= buttonMaps.length )
        {
          System.err.println( "ERROR:" + file.getName() + ": DeviceType " + type.getName()
              + " uses an undefined map index." );
          map = buttonMaps.length - 1;
        }
        if ( map != -1 && buttonMaps.length > 0 )
        {
          type.setButtonMap( buttonMaps[ map ] );
        }
      }

      if ( deviceTypeAliasNames == null )
      {
        java.util.List< String > v = new ArrayList< String >();
        DeviceType vcrType = null;
        boolean hasPVRalias = false;
        for ( DeviceType type : deviceTypes.values() )
        {
          String typeName = type.getName();
          if ( typeName.startsWith( "VCR" ) )
          {
            vcrType = type;
          }
          if ( typeName.equals( "PVR" ) )
          {
            hasPVRalias = true;
          }
          deviceTypeAliases.put( typeName, type );
          v.add( typeName );
        }
        if ( !hasPVRalias && vcrType != null )
        {
          v.add( "PVR" );
          deviceTypeAliases.put( "PVR", vcrType );
        }
        deviceTypeAliasNames = new String[ 0 ];
        deviceTypeAliasNames = v.toArray( deviceTypeAliasNames );
        Arrays.sort( deviceTypeAliasNames );
      }

      if ( settings != null )
      {
        for ( Setting setting : settings )
        {
          setting.optionsFromButtonGroup( this );
        }
      }
      
      // find the longest button map
      ButtonMap longestMap = null;
      for ( DeviceType type : deviceTypes.values() )
      {
        ButtonMap thisMap = type.getButtonMap();
        if ( longestMap == null || longestMap.size() < thisMap.size() )
        {
          longestMap = thisMap;
        }
      }
      
      // Sort the buttons lists into same order used for bindable buttons
      Collections.sort( buttons, longestMap.mapSort );
      if ( activityButtonGroups != null )
      {
        for ( int i = 0; i < activityButtonGroups.length; i++ )
        {
          if ( activityButtonGroups[ i ] != null )
          {
            Arrays.sort( activityButtonGroups[ i ], longestMap.mapSort );
          }
        }
      }

      // Now figure out which buttons are bindable
      List< Button > keyMoveBindableButtons = new ArrayList< Button >();
      List< Button > baseKeyMoveBindableButtons = new ArrayList< Button >();
      List< Button > macroBindableButtons = new ArrayList< Button >();
      List< Button > learnBindableButtons = new ArrayList< Button >();

      // first copy the bindable buttons from the longest map
      int index = 0;
      while ( index < longestMap.size() )
      {
        Button b = longestMap.get( index++ );
        
        if ( ( b.allowsKeyMove() || b.allowsShiftedKeyMove() || b.allowsXShiftedKeyMove() )
            && !keyMoveBindableButtons.contains( b ) )
        {
          keyMoveBindableButtons.add( b );
        }
        
        if ( ( b.getIsShifted() && b.getBaseButton() != null && b.getName().equals( getShiftLabel() + '-' + b.getBaseButton().getName() ) )
            || ( b.getIsXShifted() && b.getBaseButton() != null && b.getName().equals( getXShiftLabel() + '-' + b.getBaseButton().getName() ) ) )
        {
          b = b.getBaseButton();
        }
        
        if ( !distinctButtons.contains( b ) )
        {
          distinctButtons.add( b );
        }

        if ( ( b.allowsKeyMove() || b.allowsShiftedKeyMove() || b.allowsXShiftedKeyMove() )
            && !baseKeyMoveBindableButtons.contains( b ) )
        {
          baseKeyMoveBindableButtons.add( b );
        }
        if ( ( b.allowsMacro() || b.allowsShiftedMacro() || b.allowsXShiftedMacro() )
            && !macroBindableButtons.contains( b ) )
        {
          macroBindableButtons.add( b );
        }        
        if ( ( b.allowsLearnedSignal() || b.allowsShiftedLearnedSignal() || b.allowsXShiftedLearnedSignal() )
            && !learnBindableButtons.contains( b ) )
        {
          learnBindableButtons.add( b );
        }       
      }

      // now copy the rest of the bindable buttons, skipping those already added
      for ( Button b : buttons )
      {
        if ( ( b.allowsKeyMove() || b.allowsShiftedKeyMove() || b.allowsXShiftedKeyMove() )
            && !keyMoveBindableButtons.contains( b ) )
        {
          keyMoveBindableButtons.add( b );
        }
        
        if ( ( b.getIsShifted() && b.getBaseButton() != null && b.getName().equals( getShiftLabel() + '-' + b.getBaseButton().getName() ) )
            || ( b.getIsXShifted() && b.getBaseButton() != null && b.getName().equals( getXShiftLabel() + '-' + b.getBaseButton().getName() ) ) )
        {
          b = b.getBaseButton();
        }
        
        if ( !distinctButtons.contains( b ) )
        {
          distinctButtons.add( b );
        }
        
        if ( ( b.allowsKeyMove() || b.allowsShiftedKeyMove() || b.allowsXShiftedKeyMove() )
            && !baseKeyMoveBindableButtons.contains( b ) )
        {
          baseKeyMoveBindableButtons.add( b );
        }
        if ( ( b.allowsMacro() || b.allowsShiftedMacro() || b.allowsXShiftedMacro() )
            && !macroBindableButtons.contains( b ) )
        {
          macroBindableButtons.add( b );
        }
        if ( ( b.allowsLearnedSignal() || b.allowsShiftedLearnedSignal() || b.allowsXShiftedLearnedSignal() )
            && !learnBindableButtons.contains( b ) )
        {
          learnBindableButtons.add( b );
        }
      }
      baseUpgradeButtons = baseKeyMoveBindableButtons.toArray( baseUpgradeButtons );
      upgradeButtons = keyMoveBindableButtons.toArray( upgradeButtons );
      macroButtons = macroBindableButtons.toArray( macroButtons );
      learnButtons = learnBindableButtons.toArray( learnButtons );

      if ( !needsLayoutWarning() )
      {      
        imageMaps[ mapIndex ].parse( this );
      }

      for ( DeviceType type : deviceTypes.values() )
      {
        ImageMap[][] maps = type.getImageMaps();
        if ( maps.length > 0 )
        {
          ImageMap[] a = maps[ mapIndex ];
          for ( int i = 0; i < a.length; ++i )
          {
            a[ i ].parse( this );
          }
        }
      }

      setPhantomShapes();

      loaded = true;
    }
    catch ( FileNotFoundException fnfe )
    {
      JOptionPane.showMessageDialog( RemoteMaster.getFrame(), fnfe.getMessage(), "Remote Load Error",
          JOptionPane.ERROR_MESSAGE );
    }
    catch ( Exception e )
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter( sw );
      e.printStackTrace( pw );
      pw.flush();
      pw.close();
      JOptionPane.showMessageDialog( RemoteMaster.getFrame(), sw.toString(), "Remote Load Error",
          JOptionPane.ERROR_MESSAGE );
      System.err.println( sw.toString() );
    }
    if ( usesEZRC() )
    {
      gidMap = new LinkedHashMap< String, Integer >();
      for ( int i = 0; i < ueiNames.length; i++ )
      {
        gidMap.put(  ueiNames[ i ], ueiGids[ i ] );
      }
    }
  }
  
  public boolean needsLayoutWarning()
  {
    boolean warn = false;
    if ( imageMaps.length > mapIndex )
    {
      ImageMap map = imageMaps[ mapIndex ];
      if ( map == null || map.getMapFile() == null || !map.getMapFile().exists() )
      {
        warn = true;
        if ( !prelimLoad )
        {
          String message = map == null || map.getMapFile() == null ? "No map file specified in RDF.\n" : "Map file " + map.getMapFile().getName() + " does not exist.\n";
          message += "A default button layout will be used.";
          JOptionPane.showMessageDialog( RemoteMaster.getFrame(), message, "Remote Load Error", JOptionPane.WARNING_MESSAGE );
        }
      }
    }
    else
    {
      warn = true;
    }
    return warn;
  }

  private String parseActivityControl( RDFReader rdr ) throws Exception
  {
    String line;
    List< Button > activityBtns = buttonGroups.get( "Activity" );
    List< DeviceButton > ctrlBtns = new ArrayList< DeviceButton >();;
    String control = null;
    int activityIndex = -1;
    int groupIndex = -1;
    if ( activityBtns == null || activityButtonGroups == null )
    {
      return "";
    }
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null || line.length() == 0 || line.charAt( 0 ) == '[' )
      {
        break;
      }

      StringTokenizer st = new StringTokenizer( line, "," );
      while ( st.hasMoreTokens() )
      {
        String token = st.nextToken();
        ctrlBtns.clear();
        int pos = token.indexOf( '=' );
        if ( pos != -1 )
        {
          if ( activityControl == null || activityControl.length == 0 )
          {
//            activityControl = new DeviceButton[ activityBtns.size() ][ activityButtonGroups.length][] ;
            activityControl = new Activity.Control[ activityBtns.size() ];
            for ( int i = 0; i < activityControl.length; i++ )
            {
              Activity.Control ac = new Activity.Control();
              ac.devices = new DeviceButton[ activityButtonGroups.length][];
              ac.overrides = new DeviceButton[ activityButtonGroups.length ];
              activityControl[ i ] = ac;
            }
          }
          control = token.substring( pos + 1 );
          token = token.substring( 0, pos ).trim();          
          activityIndex = activityBtns.indexOf( getButton( token ) );
        }
        else
        {
          control = token;
        }
        
        String override = null;
        pos = control.indexOf( "::" );
        if ( pos != -1 )
        {
          override = control.substring( pos + 2 ).trim();
          control = control.substring( 0, pos );
        }

        pos = control.indexOf( ":" );
        if ( pos != -1 )
        {
          String group = control.substring( 0, pos ).trim();
          control = control.substring( pos + 1 );

          if ( group.toLowerCase().startsWith( "group" ) )
          {
            groupIndex = Integer.parseInt( group.substring( 5 ) );
            StringTokenizer st2 = new StringTokenizer( control, "+" );
            while ( st2.hasMoreTokens() )
            {
              token = st2.nextToken().trim();
              Button btn = getButton( token );
              if ( btn != null )
              {
                DeviceButton dev = getDeviceButton( btn.getKeyCode() );
                ctrlBtns.add( dev );
              }
            }
            activityControl[ activityIndex ].devices[ groupIndex ] = new DeviceButton[ ctrlBtns.size() ];
            for ( int i = 0; i < ctrlBtns.size(); i++ )
            {
              activityControl[ activityIndex ].devices[ groupIndex ][ i ] = ctrlBtns.get( i ); 
            }
            if ( override != null )
            {
              Button btn = getButton( override );
              if ( btn != null )
              {
                DeviceButton dev = getDeviceButton( btn.getKeyCode() );
                activityControl[ activityIndex ].overrides[ groupIndex ] = dev;
              }
            }
          }
          else if ( group.equalsIgnoreCase( "maps" ) )
          {
            StringTokenizer st2 = new StringTokenizer( control, "+" );
            List< Integer > mapList = new ArrayList< Integer >();
            while ( st2.hasMoreTokens() )
            {
              try
              {
                token = st2.nextToken().trim();
                int map = RDFReader.parseNumber( token );
                mapList.add( map );
              }
              catch ( Exception ex )
              {
                mapList.add( 0 );
                System.err.println( "RDF error in [ActivityControl]: " + token + " is not an integer" );
              }
            }
            activityControl[ activityIndex ].maps = mapList.toArray( new Integer[ 0 ] );
          }
          else
          {
            activityControl = new Activity.Control[ 0 ];
            return line;
          }
        }
        else
        {
          activityControl = new Activity.Control[ 0 ];
          return line;
        }
      }
    }
    return line;
  }

  /**
   * Sets the phantom shapes.
   */
  private void setPhantomShapes()
  {
    double radius = 8;
    double gap = 6;

    double diameter = 2 * radius;
    double x = gap;
    java.util.List< ImageMap > maps = new ArrayList< ImageMap >();
    if ( imageMaps.length > 0 && imageMaps[ mapIndex ] != null && imageMaps[ mapIndex ].getMapFile() != null 
        && imageMaps[ mapIndex ].getMapFile().exists() )
    {
      maps.add( imageMaps[ mapIndex ] );
    }
    for ( DeviceType type : deviceTypes.values() )
    {
      if ( type.getImageMaps().length == 0 )
      {
        continue;
      }
      ImageMap[] devMaps = type.getImageMaps()[ mapIndex ];
      for ( int i = 0; i < devMaps.length; ++i )
      {
        maps.add( devMaps[ i ] );
      }
    }

    for ( ImageMap map : maps )
    {
      ImageIcon icon = new ImageIcon( map.getImageFile().getAbsolutePath() );
      int h = icon.getIconHeight();
      int w = icon.getIconWidth();
      if ( h > height )
      {
        height = h;
      }
      if ( w > width )
      {
        width = w;
      }
    }
    double y = height + gap;
    if ( width == 0 )
    {
      width = ( int )( 6 * diameter + 5 * gap );
    }

    for ( int i = 0; i < upgradeButtons.length; i++ )
    {
      Button b = upgradeButtons[ i ];
      if ( isSSD() && isSoftButton( b ) && b.getKeyCode() > 0x36 )
      {
        continue;
      }
      if ( !b.getHasShape() && !b.getIsShifted() && !b.getIsXShifted() )
      {
        if ( x + diameter + gap > width )
        {
          x = gap;
          y += gap + diameter;
        }
        Shape shape = new Ellipse2D.Double( x, y, diameter, diameter );
        x += diameter + gap;
        ButtonShape buttonShape = new ButtonShape( shape, b );
        phantomShapes.add( buttonShape );
        b.setHasShape( true );
      }
    }
    height = ( int )( y + gap + diameter );
    for ( ImageMap map : maps )
    {
      map.getShapes().addAll( phantomShapes );
    }
    if ( maps.size() == 0 && ( imageMaps[ mapIndex ].getMapFile() == null || !imageMaps[ mapIndex ].getMapFile().exists() ) )
    {
      imageMaps[ mapIndex ].getShapes().addAll( phantomShapes );
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return getName();
  }

  /**
   * Gets the signature.
   * 
   * @return the signature
   */
  public String getSignature()
  {
    return signature;
  }
  
  public boolean isSSD()
  {
    load();
    return signature.startsWith( "USB" ) && processor.getName().equals( "S3F80" );
  }
  
  public boolean isFDRA()
  {
    load();
    List< String > pids = Arrays.asList( "8008", "8009", "8010", "8011", "0007" );
    return signature.startsWith( "USB" ) && pids.contains( signature.substring( 3 ) );
  }
  
  public boolean hasProfiles()
  {
    return isSSD();
  }

  public int getSigAddress()
  {
    return sigAddress;
  }

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName()
  {
    return names[ nameIndex ];
  }

  public int getNameIndex()
  {
    return nameIndex;
  }

  /**
   * Gets the name count.
   * 
   * @return the name count
   */
  public int getNameCount()
  {
    return names.length;
  }

  /**
   * Gets the base address.
   * 
   * @return the base address
   */
  public int getBaseAddress()
  {
    load();
    return baseAddress;
  }

  /**
   * Gets the eeprom size.
   * 
   * @return the eeprom size
   */
  public int getEepromSize()
  {
    load();
    return eepromSize;
  }

  /**
   * Gets the device code offset.
   * 
   * @return the device code offset
   */
  public int getDeviceCodeOffset()
  {
    return deviceCodeOffset;
  }

  public int getMaxBuiltInCode()
  {
    return maxBuiltInCode;
  }

  /**
   * Gets the device types.
   * 
   * @return the device types
   */
  public DeviceType[] getDeviceTypes()
  {
    // This construction ensures that the device types returned are those returned
    // by getDeviceTypeByIndex(), where there is ambiguity (which should not happen,
    // but can do with a poorly constructed RDF.
    int maxNum = 0;
    for ( DeviceType type : deviceTypeList )
    {
      maxNum = Math.max( maxNum, type.getNumber() );
    }
    DeviceType[] types = new DeviceType[ maxNum + 1 ];
    List< DeviceType > list = new ArrayList< DeviceType >();
    for ( DeviceType type : deviceTypeList )
    {
      int num = type.getNumber();
      if ( types[ num ] == null )
      {
        types[ num ] = type;
        list.add( type );
      }
    }
    for ( DeviceType type : types )
    {
      if ( type == null )
      {
        // This is the case where the type numbers are not consecutive, such as XSight
        // remotes that use letters instead of numbers (with the type number being the
        // ASCII value).  In this case return the device types in the order listed in
        // the RDF.
        return list.toArray( new DeviceType[ 0 ] ); 
      }
    }
    // Here the type numbers are consecutive from 0.  Return the device types in the
    // order of their type numbers.  This is needed for key move support to work
    // correctly.
    return types; 
  }

  public DeviceType[] getAllDeviceTypes()
  {
    ArrayList< DeviceType > tempList = new ArrayList< DeviceType >();
    DeviceType d = null;
    for ( DeviceType deviceType : deviceTypeList )
    {
      // Ensure that duplicate entries in the list are described by the same
      // DeviceType, so that the "contains" works as desired.
      d = devicesByType.get( deviceType.get_Type() );
      if ( !tempList.contains( d ) )
      {
        tempList.add( d );
      }
    }
    DeviceType[] types = tempList.toArray( new DeviceType[ 0 ] );
    return types;
  }

  /**
   * Gets the device type.
   * 
   * @param typeName
   *          the type name
   * @return the device type
   */
  public DeviceType getDeviceType( String typeName )
  {
    DeviceType devType = deviceTypes.get( typeName );
    if ( devType == null )
    {
      for ( Map.Entry< String, DeviceType > entry : deviceTypes.entrySet() )
      {
        String name = entry.getKey();
        int slash = name.indexOf( '/' );
        if ( slash != -1 )
        {
          if ( typeName.equals( name.substring( 0, slash ) ) || typeName.equals( name.substring( slash + 1 ) ) )
          {
            devType = entry.getValue();
            break;
          }
        }
      }
    }
    return devType;
  }

  /**
   * Gets the device type by alias name.
   * 
   * @param aliasName
   *          the alias name
   * @return the device type by alias name
   */
  public DeviceType getDeviceTypeByAliasName( String aliasName )
  {
    DeviceType type = deviceTypeAliases.get( aliasName );
    if ( type != null )
    {
      return type;
    }
    return getDeviceType( aliasName );
  }

  /**
   * Gets the device type by index.
   * 
   * @param index
   *          the index
   * @return the device type by index
   */
  public DeviceType getDeviceTypeByIndex( int index )
  {
    // Why not just return getDeviceTypes()[ index ]?  Is it to cover indexes out of range?
    for ( DeviceType type : deviceTypes.values() )
    {
      if ( type.getNumber() == index )
      {
        return type;
      }
    }
    return null;
  }

  public DeviceType getDeviceTypeByIndexAndGroup( int index, int group )
  {
    if ( group == -1 )
    {
      return getDeviceTypeByIndex( index );
    }
    else
    {
      int fullType = index | group << 8;
      return devicesByType.get( fullType );
    }
  }

  /**
   * Gets the device type alias.
   * 
   * @param type
   *          the type
   * @return the device type alias
   */
  public String getDeviceTypeAlias( DeviceType type )
  {
    String tentative = null;
    for ( String alias : deviceTypeAliasNames )
    {
      if ( getDeviceTypeByAliasName( alias ) != type )
      {
        continue;
      }
      String typeName = type.getName();
      if ( typeName.equals( alias ) )
      {
        return alias;
      }
      if ( ( typeName.contains( alias ) || alias.contains( typeName ) ) && tentative == null )
      {
        tentative = alias;
      }
    }
    if ( tentative != null )
    {
      return tentative;
    }
    for ( String alias : deviceTypeAliasNames )
    {
      if ( getDeviceTypeByAliasName( alias ) == type )
      {
        tentative = alias;
        break;
      }
    }
    return tentative;
  }

  public java.util.List< DeviceType > getDeviceTypeList()
  {
    return deviceTypeList;
  }

  /**
   * Gets the device buttons.
   * 
   * @return the device buttons
   */
  public DeviceButton[] getDeviceButtons()
  {
    load();
    return deviceButtons;
  }
  
  public DeviceButton getDeviceButton( int index )
  {
    load();
    for ( DeviceButton devBtn : deviceButtons )
    {
      if ( devBtn.getButtonIndex() == index )
      {
        return devBtn;
      }
    }
    return null;
  }

  /**
   * Gets the buttons.
   * 
   * @return the buttons
   */
  public java.util.List< Button > getButtons()
  {
    load();
    return buttons;
  }

  public LinkedHashMap< String, List< Button >> getButtonGroups()
  {
    return buttonGroups;
  }

  public Button[][] getActivityButtonGroups()
  {
    return activityButtonGroups;
  }

  public KeyButtonGroup[] getKeyButtonGroups()
  {
    return keyButtonGroups;
  }

  /**
   * Gets the upgrade buttons.
   * 
   * @return the upgrade buttons
   */
  public Button[] getUpgradeButtons()
  {
    load();
    return upgradeButtons;
  }

  public Button[] getBaseUpgradeButtons()
  {
    return baseUpgradeButtons;
  }

  public Button[] getMacroButtons()
  {
    load();
    return macroButtons;
  }

  public Button[] getLearnButtons()
  {
    load();
    return learnButtons;
  }
  
  public List< Button > getFunctionButtons()
  {
    return functionButtons;
  }
  
  public List< Button > getDistinctButtons()
  {
    return distinctButtons;
  }

  public List< Integer > getSegmentTypes()
  {
    // The condition !loaded is needed to prevent an infinite loop, as load() calls
    // this function even when loaded==true
    if ( !loaded ) load();
    return segmentTypes;
  }

  public Activity.Control[] getActivityControl()
  {
    return activityControl;
  }

  /**
   * Gets the phantom shapes.
   * 
   * @return the phantom shapes
   */
  public java.util.List< ButtonShape > getPhantomShapes()
  {
    load();
    return phantomShapes;
  }

  /**
   * Gets the processor.
   * 
   * @return the processor
   */
  public Processor getProcessor()
  {
    load();
    return processor;
  }
  
  /*
   *  Some newer remotes have a block structure for the entire flash, with each
   *  block starting with a two-byte checksum followed by a four-byte (32-bit) block
   *  length.  This returns the length (6) of that block header, or -1 if the remote
   *  does not have a block structure.  In general the presence or absence of
   *  block structure correlates with other features and so does not need to be
   *  set explicitly in the RDF, but there is a BlockFormat parameter that can be
   *  set in the [General] structure of the RDF to override the default.
   */
  public int getE2FormatOffset()
  {
    if ( blockFormat == BlockFormat.DEFAULT )
    {
      if ( processor.getName().equals( "MAXQ622" ) 
          || checkSums.length > 0 && checkSums[ 0 ] instanceof Xor16CheckSum )
      {
        return 6;
      }
      return -1;
    }
    else return blockFormat == BlockFormat.YES ? 6 : -1;
  }

  /**
   * Gets the rAM address.
   * 
   * @return the rAM address
   */
  public int getRAMAddress()
  {
    load();
    return RAMAddress;
  }

  /**
   * Gets the digit maps.
   * 
   * @return the digit maps
   */
  public short[] getDigitMaps()
  {
    load();
    return digitMaps;
  }

  /**
   * Gets the omit digit map byte.
   * 
   * @return the omit digit map byte
   */
  public boolean getOmitDigitMapByte()
  {
    load();
    return omitDigitMapByte;
  }

  public boolean hasGlobalSpecialFunctions()
  {
    load();
    return globalSpecialFunctions;
  }

  /**
   * Gets the image maps.
   * 
   * @param type
   *          the type
   * @return the image maps
   */
  public ImageMap[] getImageMaps( DeviceType type )
  {
    load();
    ImageMap[][] maps = type.getImageMaps();
    if ( maps != null && maps.length != 0 )
    {
      return maps[ mapIndex ];
    }
    else
    {
      ImageMap[] rc = new ImageMap[ 1 ];
      rc[ 0 ] = imageMaps[ mapIndex ];
      return rc;
    }
  }
/*
  public ButtonShape getInputButtonShape()
  {
    List< Button > inputButtons = getButtonGroups().get( "Input" );
    if ( inputButtons == null )
      return null;
    if ( inputButtonShape != null )
      return inputButtonShape;
    
    ImageMap map = getImageMaps( getDeviceTypes()[ 0 ] )[ 0 ];
    List< ButtonShape > shapes = map.getShapes();
    ButtonShape inputShape = null;
    for ( ButtonShape bs : shapes )
    {
      if ( inputButtons.contains( bs.getButton() ) && !getPhantomShapes().contains( bs ) )
      {
        inputShape = bs;
        break;
      }
    }
    inputButtonShape = inputShape;
    return inputShape;
  }
*/

  /**
   * Gets the adv code format.
   * 
   * @return the adv code format
   */
  public AdvancedCode.Format getAdvCodeFormat()
  {
    load();
    return advCodeFormat;
  }

  /**
   * Gets the adv code bind format.
   * 
   * @return the adv code bind format
   */
  public AdvancedCode.BindFormat getAdvCodeBindFormat()
  {
    load();
    return advCodeBindFormat;
  }

  public boolean supportsKeyCodeKeyMoves()
  {
    return getAdvCodeBindFormat() == AdvancedCode.BindFormat.LONG;
  }

  /**
   * Gets the eFC digits.
   * 
   * @return the eFC digits
   */
  public int getEFCDigits()
  {
    load();
    return efcDigits;
  }

  public void check( AddressRange addressRange, String name ) throws IllegalArgumentException
  {
    int bound = addressRange.getStart();
    if ( bound >= eepromSize )
    {
      throw new IllegalArgumentException( "RDF Error: " + name + " starts at $" + Integer.toString( bound, 16 )
          + ", beyond the eepromSize of $" + Integer.toString( eepromSize, 16 ) );
    }
    bound = addressRange.getEnd();
    if ( bound >= eepromSize )
    {
      throw new IllegalArgumentException( "RDF Error: " + name + " ends at $" + Integer.toString( bound, 16 )
          + ", beyond the eepromSize of $" + Integer.toString( eepromSize, 16 ) );
    }
  }

  /**
   * Parses the general section.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseGeneralSection( RDFReader rdr ) throws Exception
  {
    String processorName = processor == null ? "S3C80" : processor.getName();
    String processorVersion = null;
    String line = null;
    String parm = null;
    String value = null;
    String rawValue = null;
    if ( processor == null )
      colorHex = null;  // Only re-initialize on first call
    boolean hasForceEvenStartsEntry = false;
    while ( true )
    {
      line = rdr.readLine();

      if ( line == null || line.length() == 0 )
      {
        break;
      }

      {
        StringTokenizer st = new StringTokenizer( line, "=" );

        parm = st.nextToken().trim();
        rawValue = st.nextToken();
        value = rawValue.trim();
      }

      if ( parm.equals( "Name" ) )
      {
        rdfName = value;
      }
      else if ( parm.equals( "Identification" ) )
      {
        rdfIdentification = value;
      }
      else if ( parm.equals( "BaseAddr" ) )
      {
        baseAddress = RDFReader.parseNumber( value );
        if ( baseAddress > 0 )
        {
          sigAddress = baseAddress;
        }
      }
      else if ( parm.equals( "EepromSize" ) )
      {
        eepromSize = RDFReader.parseNumber( value );
        usageRange = new AddressRange( 0, eepromSize - 1 );
      }
      else if ( parm.equals( "DevCodeOffset" ) )
      {
        deviceCodeOffset = RDFReader.parseNumber( value );
      }
      else if ( parm.equals( "FavKey" ) )
      {
        favKey = new FavKey();
        favKey.parse( value, this );
      }
      else if ( parm.equals( "OEMDevice" ) )
      {
        oemDevice = new OEMDevice();
        oemDevice.parse( value, this );
      }
      else if ( parm.equals( "OEMControl" ) )
      {
        oemControl = RDFReader.parseNumber( value );
      }
      else if ( parm.equals( "UpgradeBug" ) )
      {
        upgradeBug = RDFReader.parseNumber( value ) != 0;
      }
      else if ( parm.equals( "AdvCodeAddr" ) )
      {
        advancedCodeAddress = new AddressRange( value, this );
        check( advancedCodeAddress, "AdvCodeAddr" );
      }
      else if ( parm.equals( "KeyMoveSupport" ) )
      {
        keyMoveSupport = RDFReader.parseFlag( value );
      }
      else if ( parm.equals( "DeviceSelection" ) )
      {
        if ( value.equals( "2" ) )
        {
          deviceSelectionMessage = true;
          deviceSelection = true; 
        }
        else
        {
          deviceSelection = RDFReader.parseFlag( value );
          deviceSelectionMessage = !deviceSelection;
        }
      }
      else if ( parm.equalsIgnoreCase( "LEDColor" ) )
      {
        // This entry is a list of colorIndex values, one per device in the order of
        // devices in the [DeviceButtons] section.  If there are more values than devices
        // then they are alternates, again in order of the devices.  A value > 0 is the
        // uneditable colorIndex for that device, a value < 0 signifies that the value is
        // editable and the absolute value is the default colorIndex, non-default values 
        // being passed to the remote in a type 0x2E segment.  All values are
        // negated when assigned to the colorIndex of the device, with values
        // set by the segment or editor being > 0.
        // The values are stored in the array ledParams as this RDF entry is read
        // before [DeviceButtons].  They are assigned to devices by parseDeviceButtons.
        int paren = value.indexOf( "(" );
        String temp = paren >= 0 ? value.substring( paren ).trim() : null;
        if ( paren >= 0 )
          value = value.substring( 0, paren ).trim();
        StringTokenizer st = new StringTokenizer( value, ", \t" );
        List< Integer > paramList = new ArrayList< Integer >();
        while ( st.hasMoreTokens() )
        {
          String token = st.nextToken().trim();
          int n = 1;  // uneditable WHITE
          try
          {
            n = RDFReader.parseNumber( token );
          }
          catch ( Exception e ){};
          paramList.add( -n );
        }
        
        if ( temp != null )
        {
          temp = temp.substring( 1, temp.length() - 1 );
          List< String > strList = new ArrayList< String >();
          st = new StringTokenizer( temp, ",\t" );
          while ( st.hasMoreTokens() )
            strList.add( st.nextToken().trim() );
          ledSettings = strList.toArray( new String[ 0 ] );
        }
        
        ledParams = paramList.toArray( new Integer[ 0 ] );
        ledColor = true;
        colorHex = new Hex( colorData );
        
      }
      else if ( parm.equals( "MacroSupport" ) )
      {
        macroSupport = RDFReader.parseFlag( value );
      }
      else if ( parm.equalsIgnoreCase( "ForceEvenStarts" ) )
      {
        forceModulus = 2;
        hasForceEvenStartsEntry = true;
        int valLen = value.length();
        if ( valLen > 1 )
        {
          // Get final character and test if it is a digit.  
          // If so, set forceModulus to this value and remove it from the value.
          String modString = value.substring( valLen - 1 );
          if ( modString.matches( "\\d" ) )
          {
            forceModulus = RDFReader.parseNumber( modString );
            value = value.substring( 0, valLen - 1 ).trim();
          }
        }
        forceEvenStarts = RDFReader.parseFlag( value );
      }
      else if ( parm.equalsIgnoreCase( "MasterPowerSupport" ) )
      {
        masterPowerSupport = RDFReader.parseFlag( value );
      }
      else if ( parm.equalsIgnoreCase( "PunchThru" ) )
      {
        if ( value.equalsIgnoreCase( "none" ) )
        {
          punchThru = "";
        }
        else
        {
          punchThru = value;
        }
      }
      else if ( parm.equals( "UpgradeAddr" ) )
      {
        upgradeAddress = new AddressRange( value, this );
        check( upgradeAddress, "UpgradeAddr" );
      }
      else if ( parm.equals( "DevUpgradeAddr" ) )
      {
        deviceUpgradeAddress = new AddressRange( value, this );
        check( deviceUpgradeAddress, "DevUpgradeAddr" );
      }
      else if ( parm.equals( "TimedMacroAddr" ) )
      {
        timedMacroAddress = new AddressRange( value, this );
        check( timedMacroAddress, "TimedMacroAddr" );
      }
      else if ( parm.equals( "TimedMacroWarning" ) )
      {
        timedMacroWarning = RDFReader.parseNumber( value ) != 0;
      }
      else if ( parm.equals( "LearnedAddr" ) )
      {
        learnedAddress = new AddressRange( value, this );
        check( learnedAddress, "LearnedAddr" );
      }
      else if ( parm.equals( "LearnedFormat" ) )
      {
        learnedFormat = RDFReader.parseNumber( value );
      }
      else if ( parm.equalsIgnoreCase( "SegmentTypes" ) )
      {
        StringTokenizer st = new StringTokenizer( value, ", " );
        segmentTypes = new ArrayList< Integer >();
        while ( st.hasMoreTokens() )
        {
          segmentTypes.add( RDFReader.parseNumber( st.nextToken().trim() ) );
        }
      }
      else if ( parm.equals( "Processor" ) || parm.equals( "Processor+" ) )
      {
        processorName = value;
        if ( processorName.equals( "6805" ) && processorVersion == null )
        {
          processorVersion = "C9";
        }
      }
      else if ( parm.equals( "ProcessorVersion" ) )
      {
        processorVersion = value;
      }
      else if ( parm.equalsIgnoreCase( "RAMAddr" ) )
      {
        RAMAddress = RDFReader.parseNumber( value );
      }
      else if ( ( parm.equals( "TimeAddr" ) || parm.equals( "TimeAddr+" ) ) && autoClockSet == null )
      {
        autoClockSet = new AutoClockSet();
        autoClockSet.parse( value, this );
      }
      else if ( parm.equals( "RDFSync" ) )
      {
        RDFSync = RDFReader.parseNumber( value );
      }
      else if ( parm.equals( "PunchThruBase" ) )
      {
        punchThruBase = RDFReader.parseNumber( value );
      }
      else if ( parm.equals( "ScanBase" ) )
      {
        scanBase = RDFReader.parseNumber( value );
      }
      else if ( parm.equals( "SleepStatusBit" ) )
      {
        sleepStatusBit = new StatusBit();
        sleepStatusBit.parse( value, this );
      }
      else if ( parm.equals( "VPTStatusBit" ) )
      {
        vptStatusBit = new StatusBit();
        vptStatusBit.parse( value, this );
      }
      else if ( parm.equals( "OmitDigitMapByte" ) )
      {
        omitDigitMapByte = RDFReader.parseFlag( value );
      }
      else if ( parm.equals( "GlobalSpecialFunctions" ) )
      {
        globalSpecialFunctions = RDFReader.parseFlag( value );
      }
      else if ( parm.equals( "ImageMap" ) )
      {
        PropertyFile properties = JP1Frame.getProperties();
        File imageDir = properties.getFileProperty( "ImagePath" );
        if ( imageDir == null )
        {
          imageDir = new File( properties.getFile().getParentFile(), "Images" );
        }

        if ( !imageDir.exists() )
        {
          JOptionPane.showMessageDialog( null, "Images folder not found!", "Error", JOptionPane.ERROR_MESSAGE );
          RMFileChooser chooser = new RMFileChooser( imageDir.getParentFile() );
          chooser.setFileSelectionMode( RMFileChooser.DIRECTORIES_ONLY );
          chooser.setDialogTitle( "Choose the directory containing the remote images and maps" );
          if ( chooser.showOpenDialog( null ) != RMFileChooser.APPROVE_OPTION )
          {
            System.exit( -1 );
          }

          imageDir = chooser.getSelectedFile();
          properties.setProperty( "ImagePath", imageDir );
        }

        String mapList = value;
        StringTokenizer mapTokenizer = new StringTokenizer( mapList, "," );
        int mapCount = mapTokenizer.countTokens();
        imageMaps = new ImageMap[ mapCount ];
        imageMapNames = new String[ mapCount ];
        for ( int m = 0; m < mapCount; ++m )
        {
          imageMapNames[ m ] = mapTokenizer.nextToken();
          imageMaps[ m ] = new ImageMap( new File( imageDir, imageMapNames[ m ] ) );
        }

        if ( nameIndex >= mapCount )
        {
          mapIndex = mapCount - 1;
        }
        else
        {
          mapIndex = nameIndex;
        }
      }
      else if ( parm.equals( "DefaultRestrictions" ) )
      {
        defaultRestrictions = parseRestrictions( value, null );
      }
      else if ( parm.equals( "Shift" ) )
      {
        StringTokenizer st = new StringTokenizer( value, "=," );
        shiftMask = RDFReader.parseNumber( st.nextToken() );
        shiftEnabled = shiftMask != 0;
        if ( st.hasMoreTokens() )
        {
          shiftLabel = st.nextToken().trim();
        }
      }
      else if ( parm.equals( "XShift" ) )
      {
        StringTokenizer st = new StringTokenizer( value, "=," );
        xShiftMask = RDFReader.parseNumber( st.nextToken() );
        xShiftEnabled = xShiftMask != 0;
        if ( st.hasMoreTokens() )
        {
          xShiftLabel = st.nextToken().trim();
        }
      }
      else if ( parm.equals( "AdvCodeFormat" ) )
      {
        advCodeFormat = AdvancedCode.Format.valueOf( value.toUpperCase() );
      }
      else if ( parm.equals( "AdvCodeBindFormat" ) )
      {
        advCodeBindFormat = AdvancedCode.BindFormat.valueOf( value.toUpperCase() );
      }
      else if ( parm.equals( "EFCDigits" ) )
      {
        efcDigits = RDFReader.parseNumber( value );
      }
      else if ( parm.equals( "DevComb" ) )
      {
        devCombAddress = new int[ 7 ];
        for ( int i = 0; i < 7; i++ )
        {
          devCombAddress[ i ] = -1;
        }
        List< String > addrs = LineTokenizer.tokenize( value, "," );
        int i = 0;
        for ( String addr : addrs )
        {
          if ( addr != null )
          {
            devCombAddress[ i ] = RDFReader.parseNumber( addr );
          }
          i++ ;
        }
      }
      else if ( parm.equals( "ProtocolVectorOffset" ) )
      {
        protocolVectorOffset = RDFReader.parseNumber( value );
      }
      else if ( parm.equals( "ProtocolDataOffset" ) )
      {
        protocolDataOffset = RDFReader.parseNumber( value );
      }
      else if ( parm.equals( "EncDec" ) )
      {
        encdec = EncrypterDecrypter.createInstance( value );
      }
      else if ( parm.equals( "MaxUpgradeLength" ) )
      {
        maxUpgradeLength = new Integer( RDFReader.parseNumber( value ) );
      }
      else if ( parm.equals( "MaxProtocolLength" ) )
      {
        maxProtocolLength = new Integer( RDFReader.parseNumber( value ) );
      }
      else if ( parm.equals( "MaxCombinedUpgradeLength" ) )
      {
        maxCombinedUpgradeLength = new Integer( RDFReader.parseNumber( value ) );
      }
      else if ( parm.equals( "SectionTerminator" ) )
      {
        sectionTerminator = ( short )RDFReader.parseNumber( value );
      }
      else if ( parm.equalsIgnoreCase( "2BytePid" ) )
      {
        twoBytePID = RDFReader.parseFlag( value );
      }
      else if ( parm.equalsIgnoreCase( "16BitSetupCode" ) )
      {
        twoByteSetupCode = RDFReader.parseFlag( value );
      }
      else if ( parm.equalsIgnoreCase( "LearnedDevBtnSwapped" ) )
      {
        learnedDevBtnSwapped = RDFReader.parseFlag( value );
      }
      else if ( ( parm.equalsIgnoreCase( "Labels" ) || parm.equalsIgnoreCase( "Labels+" ) ) && labels == null )
      {
        labels = new DeviceLabels();
        labels.parse( value, this );
      }
      else if ( parm.equalsIgnoreCase( "SoftDev" ) )
      {
        softDevices = new SoftDevices();
        softDevices.parse( value, this );
        if ( !softDevices.inUse() )
        {
          softDevices = null;
        }
      }
      else if ( parm.equalsIgnoreCase( "SoftHT" ) )
      {
        softHomeTheater = new SoftHomeTheater();
        softHomeTheater.parse( value, this );
        if ( !softHomeTheater.inUse() )
        {
          softHomeTheater = null;
        }
      }
      else if ( parm.equalsIgnoreCase( "MacroCodingType" ) )
      {
        macroCodingType = new MacroCodingType();
        macroCodingType.parse( value, this );
      }
      else if ( parm.equalsIgnoreCase( "StartReadOnlySettings" ) )
      {
        startReadOnlySettings = RDFReader.parseNumber( value );
      }
      else if ( parm.equalsIgnoreCase( "PauseParams" ) )
      {
        PauseParameters parms = new PauseParameters();
        parms.parse( value, this );
        pauseParameters.put( parms.getUserName(), parms );
      }
      else if ( parm.equalsIgnoreCase( "PowerButtons" ) )
      {
        StringTokenizer st = new StringTokenizer( value, ", " );
        int len = st.countTokens();
        powerButtons = new short[ len ];
        int i = 0;
        while ( st.hasMoreElements() )
        {
          powerButtons[ i++ ] = ( short )RDFReader.parseNumber( st.nextToken() );
        }
      }
      else if ( parm.equalsIgnoreCase( "WavUpgrade" ) )
      {
        // Note that the item name in the RDF Spec is WavUpgrade, not WaveUpgrade
        waveUpgrade = RDFReader.parseFlag( value );
      }
      else if ( parm.equalsIgnoreCase( "SetupValidation" ) )
      {
        setupValidation = SetupValidation.valueOf( value.toUpperCase() );
      }
      else if ( parm.equalsIgnoreCase( "AdvCodeTypes" ) )
      {
        parseAdvCodeTypes( value, rdr );
      }
      else if ( ( parm.equalsIgnoreCase( "ExtenderVersionAddr" ) || parm.equalsIgnoreCase( "ExtenderVersionAddr+" ) )
          && extenderVersionParm == null )
      {
        extenderVersionParm = new ExtenderVersionParm();
        extenderVersionParm.parse( value, this );
        if ( !extenderVersionParm.displayExtenderVersion() )
        {
          extenderVersionParm = null;
        }
      }
      else if ( parm.equalsIgnoreCase( "RDFVersionAddr" ) )
      {
        rdfVersionAddress = RDFReader.parseNumber( value );
      }
      else if ( parm.equalsIgnoreCase( "ActivityMapIndex" ) )
      {
        activityMapIndex = RDFReader.parseNumber( value );
      }
      else if ( parm.equalsIgnoreCase( "Notes" ) )
      {
        notes = parseNotes( rdr, rawValue );
      }
      else if ( parm.equalsIgnoreCase( "BlockFormat" ) )
      {
        boolean bf = RDFReader.parseFlag( value );
        blockFormat = bf ? BlockFormat.YES : BlockFormat.NO;
      }

      // A SoftHT entry should be ignored unless SoftDevices is used.
      if ( softDevices == null )
      {
        softHomeTheater = null;
      }

      // A TimedMacroAddr entry should be ignored if timed macros are stored in the
      // Advanced Codes section.
      if ( macroCodingType.hasTimedMacros() )
      {
        timedMacroAddress = null;
      }
    }
    
    processor = ProcessorManager.getProcessor( processorName, processorVersion );
    if ( processor == null )
    {
      processor = ProcessorManager.getProcessor( "S3C80" );
    }
    
    if ( segmentTypes != null )
    {
      macroSupport = ( segmentTypes.contains( 1 ) || segmentTypes.contains( 2 ) || segmentTypes.contains( 3 ) || isSSD() );
      keyMoveSupport = ( segmentTypes.contains( 7 ) || segmentTypes.contains( 8 ) );
      twoBytePID = true;
      advCodeBindFormat = AdvancedCode.BindFormat.LONG;
      efcDigits = 5;
    }
    
    // Set values for RAMAddr for processors where it does not need to be specified
    if ( !processorName.equals( "S3C80" ) ||  RAMAddress != S3C80Processor.newRAMAddress )
    {
      RAMAddress = processor.getRAMAddress();
    }
    if ( processor.getEquivalentName().equals( "MAXQ610" ) && !hasForceEvenStartsEntry )
    {
      forceEvenStarts = true;
      forceModulus = 2;
    }
    else if ( processor.getEquivalentName().equals( "TI2541" ) && !hasForceEvenStartsEntry )
    {
      forceEvenStarts = true;
      forceModulus = 4;
    }
    
    if ( imageMaps.length == 0 )
    {
      imageMaps = new ImageMap[] { new ImageMap( null ) };
      imageMapNames = new String[ 0 ];
      mapIndex = 0;
    }

    return line;
  }
  
  public void resetImageMaps( File path ) throws Exception
  {
    for ( int m = 0; m < imageMaps.length; ++m )
    {
      imageMaps[ m ] = m < imageMapNames.length ? new ImageMap( new File( path, imageMapNames[ m ] ) ): null;
      if ( imageMaps[ m ] != null )
      {
        imageMaps[ m ].parse( this );
      }
    }
  }

  public AutoClockSet getAutoClockSet()
  {
    return autoClockSet;
  }

  /**
   * Gets the dev comb addresses.
   * 
   * @return the dev comb addresses
   */
  public int[] getDevCombAddresses()
  {
    load();
    return devCombAddress;
  }

  /**
   * Parses the restrictions.
   * 
   * @param str
   *          the str
   * @return the int
   */
  private int parseRestrictions( String str, List< String > groupNames )
  {
    int rc = 0;
    if ( restrictionTable == null )
    {
      restrictionTable = new Hashtable< String, Integer >( 46 );
      restrictionTable.put( "MoveBind", new Integer( Button.MOVE_BIND ) );
      restrictionTable.put( "ShiftMoveBind", new Integer( Button.SHIFT_MOVE_BIND ) );
      restrictionTable.put( "XShiftMoveBind", new Integer( Button.XSHIFT_MOVE_BIND ) );
      restrictionTable.put( "AllMoveBind", new Integer( Button.ALL_MOVE_BIND ) );
      restrictionTable.put( "MacroBind", new Integer( Button.MACRO_BIND ) );
      restrictionTable.put( "ShiftMacroBind", new Integer( Button.SHIFT_MACRO_BIND ) );
      restrictionTable.put( "XShiftMacroBind", new Integer( Button.XSHIFT_MACRO_BIND ) );
      restrictionTable.put( "AllMacroBind", new Integer( Button.ALL_MACRO_BIND ) );
      restrictionTable.put( "LearnBind", new Integer( Button.LEARN_BIND ) );
      restrictionTable.put( "ShiftLearnBind", new Integer( Button.SHIFT_LEARN_BIND ) );
      restrictionTable.put( "XShiftLearnBind", new Integer( Button.XSHIFT_LEARN_BIND ) );
      restrictionTable.put( "AllLearnBind", new Integer( Button.ALL_LEARN_BIND ) );
      restrictionTable.put( "MacroData", new Integer( Button.MACRO_DATA ) );
      restrictionTable.put( "ShiftMacroData", new Integer( Button.SHIFT_MACRO_DATA ) );
      restrictionTable.put( "XShiftMacroData", new Integer( Button.XSHIFT_MACRO_DATA ) );
      restrictionTable.put( "AllMacroData", new Integer( Button.ALL_MACRO_DATA ) );
      restrictionTable.put( "TMacroData", new Integer( Button.TMACRO_DATA ) );
      restrictionTable.put( "ShiftTMacroData", new Integer( Button.SHIFT_TMACRO_DATA ) );
      restrictionTable.put( "XShiftMacroData", new Integer( Button.XSHIFT_TMACRO_DATA ) );
      restrictionTable.put( "AllTMacroData", new Integer( Button.ALL_TMACRO_DATA ) );
      restrictionTable.put( "FavData", new Integer( Button.FAV_DATA ) );
      restrictionTable.put( "ShiftFavData", new Integer( Button.SHIFT_FAV_DATA ) );
      restrictionTable.put( "XShiftFavData", new Integer( Button.XSHIFT_FAV_DATA ) );
      restrictionTable.put( "AllFavData", new Integer( Button.ALL_FAV_DATA ) );      
      restrictionTable.put( "PwrMacroData", new Integer( Button.PWRMACRO_DATA ) );
      restrictionTable.put( "ShiftPwrMacroData", new Integer( Button.SHIFT_PWRMACRO_DATA ) );
      restrictionTable.put( "XShiftPwrMacroData", new Integer( Button.XSHIFT_PWRMACRO_DATA ) );
      restrictionTable.put( "AllPwrMacroData", new Integer( Button.ALL_PWRMACRO_DATA ) );
      restrictionTable.put( "Bind", new Integer( Button.BIND ) );
      restrictionTable.put( "ShiftBind", new Integer( Button.SHIFT_BIND ) );
      restrictionTable.put( "XShiftBind", new Integer( Button.XSHIFT_BIND ) );
      restrictionTable.put( "Data", new Integer( Button.DATA ) );
      restrictionTable.put( "ShiftData", new Integer( Button.SHIFT_DATA ) );
      restrictionTable.put( "XShiftData", new Integer( Button.XSHIFT_DATA ) );
      restrictionTable.put( "AllBind", new Integer( Button.ALL_BIND ) );
      restrictionTable.put( "AllData", new Integer( Button.ALL_DATA ) );
      restrictionTable.put( "Shift", new Integer( Button.SHIFT ) );
      restrictionTable.put( "XShift", new Integer( Button.XSHIFT ) );
      restrictionTable.put( "All", new Integer( Button.ALL ) );
    }
    StringTokenizer st = new StringTokenizer( str, "+-", true );
    boolean isAdd = true;
    while ( st.hasMoreTokens() )
    {
      String token = st.nextToken();
      if ( token.equals( "+" ) )
      {
        isAdd = true;
      }
      else if ( token.equals( "-" ) )
      {
        isAdd = false;
      }
      else if ( isAdd && token.toUpperCase().startsWith( "GROUP" ) )
      {
        groupNames.add( token.substring( 5 ) );
      }
      else
      {
        Integer value = restrictionTable.get( token );
        if ( value == null )
        {
          continue;
        }
        if ( isAdd )
        {
          rc |= value.intValue();
        }
        else
        {
          rc &= ~value.intValue();
        }
      }
    }
    return rc;
  }

  /**
   * Parses the special protocols.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseSpecialProtocols( RDFReader rdr ) throws Exception
  {
    String line;
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null || line.length() == 0 )
      {
        break;
      }

      StringTokenizer st = new StringTokenizer( line, "=" );
      String name = st.nextToken().trim();
      String value = st.nextToken().trim();
      /*
       * GD: The lines commented out below appear to be the start of an attempt to handle device specific macros, or
       * certain of them, through the Macro tab rather than the Special Functions tab. It seems to be unfinished and
       * deviceIndexMask seems to be unused. I have commented them out, now that internal special protocols are fully
       * implemented. This is not intended to imply any disagreement with that approach, merely that DSM = Internal:0
       * can now be handled through special protocols while it cannot be handled through the Macros tab even if these
       * lines are left active.
       */
      // if ( name.equals( "DSM" ) && value.startsWith( "Internal:0" ) )
      // {
      // deviceIndexMask = 0x0F;
      // }
      // else
      // {
      specialProtocols.add( SpecialProtocol.create( name, value, this ) );
      // }
    }
    return line;
  }

  /**
   * Parses the check sums.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseCheckSums( RDFReader rdr ) throws Exception
  {
    java.util.List< CheckSum > work = new ArrayList< CheckSum >();
    String line;
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null || line.length() == 0 )
      {
        break;
      }
      
      int pos = 0;
      boolean comp = false;
      char ch = line.charAt( pos++ );
      if ( ch == '~' )
      {
        comp = true;
        ch = line.charAt( pos++ );
      }
      line = line.substring( pos );
      StringTokenizer st = new StringTokenizer( line, ":" );
      int addr = RDFReader.parseNumber( st.nextToken() );
      AddressRange range = new AddressRange();
      range.parse( st.nextToken(), this );
      check( range, "CheckSums" );
      CheckSum sum = null;
      if ( ch == '+' )
      {
        sum = new AddCheckSum( addr, range, comp );
      }
      else if ( ch == '*' )
      {
        sum = new Xor16CheckSum( addr, range, comp );
      }
      else
      {
        sum = new XorCheckSum( addr, range, comp );
      }
      work.add( sum );
    }
    checkSums = work.toArray( checkSums );
    return line;
  }

  /**
   * Gets the check sums.
   * 
   * @return the check sums
   */
  public CheckSum[] getCheckSums()
  {
    return checkSums;
  }

  /**
   * Parses the settings.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseSettings( RDFReader rdr ) throws Exception
  {
    String line;
    List< Setting > work = new ArrayList< Setting >();
    work.addAll( Arrays.asList( settings ) );
    int index = settingBytes.size();
    while ( true )
    {
      line = rdr.readLine();

      if ( line == null || line.length() == 0 )
      {
        break;
      }

      StringTokenizer st = new StringTokenizer( line, "=" );
      String title = st.nextToken();
      String value = st.nextToken().trim();
      int pos = value.indexOf( " " );
      String init = null;
      int segmentType = -1;
      if ( pos > 0 && !( init =  value.substring( 0, pos ) ).contains( "." ) )
      {
        // Standard setting value is preceded by segment type
        segmentType = RDFReader.parseNumber( init );
        value = value.substring( pos ).trim();
      }

      st = new StringTokenizer( value, ".= \t" );
      int byteAddress = RDFReader.parseNumber( st.nextToken() );
      int bitNumber = RDFReader.parseNumber( st.nextToken() );
      int numberOfBits = RDFReader.parseNumber( st.nextToken() );
      int initialValue = RDFReader.parseNumber( st.nextToken() );
      boolean inverted = RDFReader.parseNumber( st.nextToken() ) != 0;

      java.util.List< String > options = null;
      String sectionName = null;

      if ( st.hasMoreTokens() )
      {
        String token = st.nextToken( ",;)" ).trim();
        if ( token.charAt( 0 ) == '(' )
        {
          options = new ArrayList< String >();
          options.add( token.substring( 1 ) );
          while ( st.hasMoreTokens() )
          {
            options.add( st.nextToken() );
          }
        }
        else
        {
          sectionName = token.trim();
        }
      }
      String[] optionsList = null;
      if ( options != null )
      {
        optionsList = options.toArray( new String[ 0 ] );
      }
      if ( segmentTypes != null )
      {
        byteAddress += 0x100 * ( segmentType + 1 );
      }
      
      Setting setting = new Setting( title, byteAddress, bitNumber, numberOfBits, initialValue, 
          inverted, optionsList, sectionName );
      work.add( setting );
      if ( ! settingAddresses.containsKey( byteAddress ) )
      {
        settingBytes.add( byteAddress );
        settingAddresses.put( byteAddress, index++ );
        int[] masks = setting.getMasks();
        for ( int i = 0; i < masks.length; i++ )
        {
          settingMasks.put( byteAddress + i, masks[ i ] );
        }
      }
      else if ( setting.getMasks().length == 1 )
      {
        // This is the case where two settings set different bits of same byte
        // and so can only occur with single-byte settings
        int mask = settingMasks.get( byteAddress );
        mask &= setting.getMasks()[ 0 ];
        settingMasks.put( byteAddress, mask );
      }
    }
    settings = work.toArray( settings );
    return line;
  }

  /**
   * Gets the settings.
   * 
   * @return the settings
   */
  public Setting[] getSettings()
  {
    return settings;
  }
  
  public Setting getSetting( String title )
  {
    for ( Setting setting : settings )
    {
      if ( setting.getTitle().trim().equals( title.trim() ) )
      {
        return setting;
      }
    }
    return null;  
  }

  /**
   * Gets the section.
   * 
   * @param name
   *          the name
   * @return the section
   */
  public Object[] getSection( String name )
  {
    if ( name.equals( "DeviceButtons" ) )
    {
      return getDeviceButtons();
    }
    else if ( name.equals( "DeviceTypes" ) )
    {
      return getDeviceTypes();
    }

    return null;
  }

  /**
   * Parses the device buttons.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseDeviceButtons( RDFReader rdr ) throws Exception
  {
    java.util.List< DeviceButton > work = new ArrayList< DeviceButton >();
    String line;
    int index = 0;
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null || line.length() == 0 )
      {
        break;
      }

      StringTokenizer st = new StringTokenizer( line, "," );
      int defaultSetupCode = 0;
      line = st.nextToken();
      if ( st.hasMoreTokens() )
      {
        defaultSetupCode = RDFReader.parseNumber( st.nextToken().trim() );
      }

      st = new StringTokenizer( line, "= \t" );
      String name = st.nextToken();

      int hiAddr = 0;
      int lowAddr = 0;
      int typeAddr = 0;
      boolean rf = false;
      List< Integer > ptDefList = new ArrayList< Integer >();
      if ( segmentTypes == null )
      {
        hiAddr = RDFReader.parseNumber( st.nextToken() );
        lowAddr = RDFReader.parseNumber( st.nextToken() );
        if ( st.hasMoreTokens() )
        {
          typeAddr = RDFReader.parseNumber( st.nextToken() );
        }
      }
      else
      {
        String token = st.nextToken();
        int starNdx = token.indexOf( '*' );
        if ( starNdx >= 0 )
        {
          rf = true;
          token = token.substring( 0, starNdx );
        }
        index = RDFReader.parseNumber( token );
        // Punch-through bytes with a non-standard use can be set in the RDF
        while ( st.hasMoreTokens() )
        {
          try
          {
            token = st.nextToken();
            if ( !token.startsWith( "$" ) )
            {
              // Values are interpreted as punchthrough bytes only if they have hex form with $ prefix
              break;
            }
            int val = RDFReader.parseNumber( token );
            ptDefList.add( val );
          }
          catch ( Exception ex )
          {
            break;
          }
        }
      }
      DeviceButton db = new DeviceButton( name, hiAddr, lowAddr, typeAddr, defaultSetupCode, index, deviceCodeOffset );
      db.setRf( rf );
      if ( ptDefList.size() > 0 )
      { 
        short[] ptDefaults = new short[ ptDefList.size() ];
        for ( int i = 0; i < ptDefList.size(); i++ )
        {
          ptDefaults[ i ] = ( short )( ptDefList.get( i ) & 0xFF );
        }
        db.setPTdefaults( ptDefaults );
      }
      if ( isSSD() )
      {
        // System icons seem to have type 5
        db.icon = new RMIcon( 5 );
        db.setSerial( index - 0x50 );
      }
      work.add( db );
      index++ ;
    }
    deviceButtons = work.toArray( deviceButtons );
    if ( ledParams != null )
    {
      // Assign the colorIndex values that have been read from the LEDColor entry
      // in the [General] section.
      for ( int i = 0; i < deviceButtons.length && i < ledParams.length; i++ )
      {
        deviceButtons[ i ].setColorIndex( ledParams[ i ] );
      }
    }
    return line;
  }

  /**
   * Parses the device abbreviations.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseDeviceAbbreviations( RDFReader rdr ) throws Exception
  {
    String line;
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null )
      {
        break;
      }
      if ( line.length() == 0 || line.charAt( 0 ) == '[' )
      {
        break;
      }
      StringTokenizer st = new StringTokenizer( line, "," );
      while ( st.hasMoreTokens() )
      {
        String token = st.nextToken().trim();
        int equal = token.indexOf( '=' );
        if ( equal == -1 )
        {
          continue;
        }

        String devName = token.substring( 0, equal );
        String abbreviation = token.substring( equal + 1 );
        DeviceType devType = getDeviceType( devName );
        if ( devType != null )
        {
          devType.setAbbreviation( abbreviation );
        }
      }
    }
    return line;
  }

  /**
   * Parses the digit maps.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseDigitMaps( RDFReader rdr ) throws Exception
  {
    java.util.List< Integer > work = new ArrayList< Integer >();
    String line;
    while ( true )
    {
      line = rdr.readLine();

      if ( line == null || line.length() == 0 )
      {
        break;
      }

      StringTokenizer st = new StringTokenizer( line, ",; \t" );
      while ( st.hasMoreTokens() )
      {
        work.add( new Integer( RDFReader.parseNumber( st.nextToken() ) ) );
      }
    }

    digitMaps = new short[ work.size() ];
    int i = 0;
    for ( Integer v : work )
    {
      digitMaps[ i++ ] = v.shortValue();
    }
    return line;
  }

  /**
   * Parses the device types.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseDeviceTypes( RDFReader rdr ) throws Exception
  {
    String line;
    int type = 0;
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null || line.length() == 0 )
      {
        break;
      }

      StringTokenizer st = new StringTokenizer( line, "=, \t" );
      String name = st.nextToken();
      int map = 0;
      if ( st.hasMoreTokens() )
      {
        map = RDFReader.parseNumber( st.nextToken() );
        if ( st.hasMoreTokens() )
        {
          String token = st.nextToken();
          try
          {
            type = RDFReader.parseNumber( token );
          }
          catch ( Exception e )
          {
            type = token.charAt( 0 );
          }
        }
      }
      DeviceType devType = new DeviceType( name, map, type );
      // Note that each of the next three collections may contain more elements than
      // the preceding one, as the RDF may contain several Device Type entries with the
      // same name and type number (i.e. low byte of type) but different groups (i.e.
      // high byte of type) and also may contain entirely duplicate entries.
      deviceTypes.put( name, devType );
      devicesByType.put( type, devType );
      deviceTypeList.add( devType );
      type += 0x0101;
    }

    // If [SpecialProtocols] occurs before [DeviceTypes] in the RDF, this check is needed
    for ( SpecialProtocol sp : specialProtocols )
    {
      sp.checkSpecialProtocol( this );
    }

    return line;
  }

  /**
   * Parses the device type aliases.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseDeviceTypeAliases( RDFReader rdr ) throws Exception
  {
    String line;
    java.util.List< String > v = new ArrayList< String >();
    DeviceType vcrType = null;
    boolean hasPVRalias = false;
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null || line.length() == 0 )
      {
        break;
      }

      StringTokenizer st = new StringTokenizer( line, "=," );
      String typeName = st.nextToken().trim();
      DeviceType type = getDeviceType( typeName );
      while ( st.hasMoreTokens() )
      {
        String aliasName = st.nextToken().trim();
        if ( aliasName.equals( "VCR" ) )
        {
          vcrType = type;
        }
        if ( aliasName.equals( "PVR" ) )
        {
          hasPVRalias = true;
        }
        deviceTypeAliases.put( aliasName, type );
        v.add( aliasName );
      }
    }
    if ( !hasPVRalias && vcrType != null )
    {
      v.add( "PVR" );
      deviceTypeAliases.put( "PVR", vcrType );
    }
    deviceTypeAliasNames = new String[ 0 ];
    deviceTypeAliasNames = v.toArray( deviceTypeAliasNames );
    Arrays.sort( deviceTypeAliasNames );
    return line;
  }

  /**
   * Gets the device type alias names.
   * 
   * @return the device type alias names
   */
  public String[] getDeviceTypeAliasNames()
  {
    load();
    return deviceTypeAliasNames;
  }

  /**
   * Parses the device type image maps.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseDeviceTypeImageMaps( RDFReader rdr ) throws Exception
  {
    String line;
    DeviceType type = null;
    java.util.List< java.util.List< ImageMap >> outer = new ArrayList< java.util.List< ImageMap >>();
    java.util.List< ImageMap > inner = null;
    boolean nested = false;
    PropertyFile properties = JP1Frame.getProperties();
    File imageDir = properties.getFileProperty( "ImagePath" );
    if ( imageDir == null )
    {
      imageDir = new File( properties.getFile().getParentFile(), "Images" );
    }

    while ( true )
    {
      line = rdr.readLine();
      if ( line == null || line.length() == 0 )
      {
        break;
      }

      StringTokenizer st = new StringTokenizer( line, "=, \t" );
      String typeName = st.nextToken();
      type = getDeviceType( typeName );

      while ( st.hasMoreTokens() )
      {
        String token = st.nextToken();
        if ( token.charAt( 0 ) == '(' ) // it's a list
        {
          nested = true;
          token = token.substring( 1 );
          inner = new ArrayList< ImageMap >();
          outer.add( inner );
        }

        if ( !nested )
        {
          inner = new ArrayList< ImageMap >();
          outer.add( inner );
        }

        int closeParen = token.indexOf( ')' );
        if ( closeParen != -1 )
        {
          nested = false;
          token = token.substring( 0, closeParen );
        }

        inner.add( new ImageMap( new File( imageDir, token ) ) );
      }
      ImageMap[][] outerb = new ImageMap[ outer.size() ][];
      int o = 0;
      for ( java.util.List< ImageMap > maps : outer )
      {
        ImageMap[] innerb = new ImageMap[ maps.size() ];
        outerb[ o++ ] = innerb;
        int i = 0;
        for ( ImageMap map : maps )
        {
          innerb[ i++ ] = map;
        }
        maps.clear();
      }
      outer.clear();
      type.setImageMaps( outerb );
    }
    return line;
  }

  /**
   * Parses the buttons.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseButtons( RDFReader rdr ) throws Exception
  {
    String line;
    short keycode = 1;
    int restrictions = defaultRestrictions;
    List< String > groupNames = new ArrayList< String >();
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null )
      {
        break;
      }
      if ( line.length() == 0 || line.charAt( 0 ) == '[' )
      {
        break;
      }
      StringTokenizer st = new StringTokenizer( line, "," );
      while ( st.hasMoreTokens() )
      {
        String token = st.nextToken().trim();
        int equal = token.indexOf( '=' );
        if ( equal != -1 )
        {
          groupNames = new ArrayList< String >();
          String keycodeStr = token.substring( equal + 1 );
          token = token.substring( 0, equal );
          int pos = keycodeStr.indexOf( ':' );
          if ( pos != -1 )
          {
            String restrictStr = keycodeStr.substring( pos + 1 );
            restrictions = parseRestrictions( restrictStr, groupNames );
            keycodeStr = keycodeStr.substring( 0, pos );
          }
          else
          {
            restrictions = defaultRestrictions;
          }
          keycode = ( short )RDFReader.parseNumber( keycodeStr.trim() );
        }

        String ueiName = null;
        int doubleColon = token.indexOf( "::" );
        if ( doubleColon != -1 )
        {
          ueiName = token.substring( 0, doubleColon );
          char ch = ueiName.charAt( 0 );
          if ( ch == '\'' || ch == '"' )
          {
            int end = ueiName.lastIndexOf( ch );
            ueiName = ueiName.substring( 1, end );
          }
          token = token.substring( doubleColon + 2 );
        }
        
        int colon = token.indexOf( ':' );
        String name = token;
        if ( colon != -1 )
        {
          name = token.substring( colon + 1 );
          token = token.substring( 0, colon );
          char ch = token.charAt( 0 );
          if ( ch == '\'' || ch == '"' )
          {
            int end = token.lastIndexOf( ch );
            token = token.substring( 1, end );
          }
        }
        char ch = name.charAt( 0 );
        if ( ch == '\'' || ch == '"' )
        {
          int end = name.lastIndexOf( ch );
          name = name.substring( 1, end );
        }
        Button b = new Button( token, name, keycode, this );
        if ( ueiName != null )
        {
          b.setUeiName( ueiName );
        }
        // The Button constructor sets restrictions itself under certain circumstances, so
        // we need to make sure we retain these.
        b.setRestrictions( b.getRestrictions() | restrictions );
        if ( groupNames.size() > 0 )
        {
          if ( buttonGroups == null )
          {
            buttonGroups = new LinkedHashMap< String, List<Button> >();
          }
          for ( String groupName : groupNames )
          {
            List< Button > group = buttonGroups.get( groupName );
            if ( group == null )
            {
              group = new ArrayList< Button >();
              buttonGroups.put( groupName, group );
            }
            group.add( b );
          }
          if ( hasActivityAlgorithm() )
          {
            // Remotes such as URC7935, with a type 0x2F segment, do not use activities in
            // the normal way.  Instead, a single activity is used to represent the assignment
            // of devices to buttons that is made algorithmically, depending on setting flags
            // in this segment.  A null activity button is used to represent this activity.
            List< Button > list = new ArrayList< Button >();
            list.add( null );
            buttonGroups.put( "Activity", list );
          }
        }
        keycode++ ;
        addButton( b );
      }
    }
    if ( buttonGroups != null )
    {
      int i = 0;
      List< Button[] > groupList = new ArrayList< Button[] >();
      while ( true )
      {
        List< Button > list = buttonGroups.get( "" + i++ );
        if ( list == null )
        {
          break;
        }
        groupList.add( list.toArray( new Button[ 0 ] ) );
      }
      activityButtonGroups = new Button[ groupList.size()][];
      for ( i = 0; i < groupList.size(); i++ )
      {
        activityButtonGroups[ i ] = groupList.get( i );
      }
      
      List< KeyButtonGroup > keyGroups = new ArrayList< KeyButtonGroup >();
      for ( String name : buttonGroups.keySet() )
      {
        if ( name.startsWith( "key" ) )
        {
          KeyButtonGroup kbg = new KeyButtonGroup( name.substring( 3 ), buttonGroups.get( name ) );
          keyGroups.add( kbg );
        }
      }
      keyButtonGroups = keyGroups.toArray( new KeyButtonGroup[ 0 ] );
    }
//    
//    Button favBtn = getButtonByStandardName( "fav/scan" );
//    if ( usesEZRC() && favKey == null && favBtn != null )
//    {
//      favKey = new FavKey( favBtn.getKeyCode() );
//    }

    if ( isSSD() && favKey != null )
    {
      favKey.setProfiles( new ArrayList< Activity >() );
    }
    
    return line;
  }

  /**
   * Gets the button.
   * 
   * @param keyCode
   *          the key code
   * @return the button
   */
  public Button getButton( int keyCode )
  {
    load();
    return buttonsByKeyCode.get( new Integer( keyCode ) );
  }

  /**
   * Gets the button name.
   * 
   * @param keyCode
   *          the key code
   * @return the button name
   */
  public String getButtonName( int keyCode )
  {
    if ( usesEZRC() )
    {
      DeviceButton db = getDeviceButton( keyCode );
      if ( db != null )
      {
        return db.getName();
      }
    }

    Button b = getButton( keyCode );

    if ( b == null )
    {
      int baseCode = keyCode & 0x3F;
      if ( baseCode != 0 )
      {
        b = getButton( baseCode );
        if ( ( baseCode | shiftMask ) == keyCode )
        {
          return b.getShiftedName();
        }
        if ( xShiftEnabled && ( baseCode | xShiftMask ) == keyCode )
        {
          return b.getXShiftedName();
        }
      }
      baseCode = keyCode & ~shiftMask;
      b = getButton( baseCode );
      if ( b != null )
      {
        return b.getShiftedName();
      }
      baseCode = keyCode & ~xShiftMask;
      b = getButton( baseCode );
      if ( b != null )
      {
        return b.getXShiftedName();
      }
    }

    if ( b == null )
    {
      System.err.println( "ERROR: Unknown keycode $" + Integer.toHexString( keyCode & 0xFF ) + ", Creating button!" );
      String name = "button" + Integer.toHexString( keyCode & 0xFF ).toUpperCase();
      b = new Button( name, name, ( short )keyCode, this );
      if ( b.getIsShifted() )
      {
        Button baseButton = getButton( keyCode & 0x3F );
        if ( baseButton != null )
        {
          b.setBaseButton( baseButton );
          baseButton.setShiftedButton( b );
        }
      }
      else if ( b.getIsXShifted() )
      {
        Button baseButton = getButton( keyCode & 0x3F );
        if ( baseButton != null )
        {
          b.setBaseButton( baseButton );
          baseButton.setXShiftedButton( b );
        }
      }
      addButton( b );
    }

    return b.getName();
  }

  /**
   * Gets the button.
   * 
   * @param name
   *          the name
   * @return the button
   */
  public Button getButton( String name )
  {
    load();
    return buttonsByName.get( name.toLowerCase() );
  }
  
  public Button getButtonByStandardName( String name )
  {
    load();
    return buttonsByStandardName.get( name.toLowerCase() );
  }

  /**
   * Adds the button.
   * 
   * @param b
   *          the b
   */
  public void addButton( Button b )
  {
    int keycode = b.getKeyCode();
    int unshiftedCode = keycode & 0x3f;
    if ( b.getIsShifted() )
    {
      Button c = getButton( unshiftedCode );
      if ( c != null )
      {
        c.setShiftedButton( b );
        b.setBaseButton( c );
        if ( b.getName() == null )
        {
          String name = shiftLabel + '-' + c.getName();
          b.setName( name );
          b.setStandardName( name );
        }
      }
    }
    else if ( b.getIsXShifted() )
    {
      Button c = getButton( unshiftedCode );
      if ( c != null )
      {
        c.setXShiftedButton( b );
        b.setBaseButton( c );
        if ( b.getName() == null )
        {
          String name = xShiftLabel + '-' + c.getName();
          b.setName( name );
          b.setStandardName( name );
        }
      }
    }
    else
    {
      // Look for a shifted button for which this is the base.
      int shiftedCode = keycode + shiftMask;
      Button c = getButton( shiftedCode );
      if ( c != null )
      {
        c.setBaseButton( b );
        b.setShiftedButton( c );
      }
      if ( xShiftEnabled )
      {
        // Look for an xshifted button for which this is the base.
        shiftedCode = keycode + xShiftMask;
        c = getButton( shiftedCode );
        if ( c != null )
        {
          c.setBaseButton( b );
          b.setXShiftedButton( c );
        }
      }
    }
    if ( b.getName() == null )
    {
      String name = "unknown" + Integer.toHexString( keycode );
      b.setName( name );
      b.setStandardName( name );
    }
    buttons.add( b );
    buttonsByName.put( b.getName().toLowerCase(), b );
    buttonsByStandardName.put( b.getStandardName().toLowerCase(), b );
    buttonsByKeyCode.put( new Integer( keycode ), b );
  }

  /**
   * Parses the multi macros.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseMultiMacros( RDFReader rdr ) throws Exception
  {
    String line;
    if ( sequenceNumberMask == 0 )
    {
      sequenceNumberMask = 0x70;
    }
    if ( maxMultiMacros == 0 )
    {
      if ( advCodeBindFormat == AdvancedCode.BindFormat.NORMAL )
      {
        maxMultiMacros = 3;
      }
      else
      {
        maxMultiMacros = 5;
      }
    }
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null || line.length() == 0 )
      {
        break;
      }

      MultiMacro multiMacro = new MultiMacro();
      multiMacro.parse( line, this );
    }
    return line;
  }

  /**
   * Find by standard name.
   * 
   * @param b
   *          the b
   * @return the button
   */
  public Button findByStandardName( Button b )
  {
    load();
    return buttonsByStandardName.get( b.getStandardName().toLowerCase() );
  }

  /**
   * Parses the button maps.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseButtonMaps( RDFReader rdr ) throws Exception
  {
    java.util.List< ButtonMap > work = new ArrayList< ButtonMap >();
    String line;
    // ButtonMap map = null;
    int name = -1;
    java.util.List< java.util.List< Integer >> outer = new ArrayList< java.util.List< Integer >>();
    java.util.List< Integer > inner = null;
    boolean nested = false;

    while ( true )
    {
      line = rdr.readLine();
      if ( line == null || line.length() == 0 )
      {
        break;
      }

      StringTokenizer st = new StringTokenizer( line, "=, \t" );
      if ( line.indexOf( '=' ) != -1 )
      {
        if ( name != -1 )
        {
          short[][] outerb = new short[ outer.size() ][];
          int o = 0;
          for ( java.util.List< Integer > maps : outer )
          {
            short[] innerb = new short[ maps.size() ];
            outerb[ o++ ] = innerb;
            int i = 0;
            for ( Integer v : maps )
            {
              innerb[ i++ ] = v.shortValue();
            }
            maps.clear();
          }
          outer.clear();
          work.add( new ButtonMap( name, outerb ) );
        }
        name = RDFReader.parseNumber( st.nextToken() );
      }

      while ( st.hasMoreTokens() )
      {
        String token = st.nextToken();
        if ( token.charAt( 0 ) == '(' ) // it's a list
        {
          nested = true;
          token = token.substring( 1 );
          inner = new ArrayList< Integer >();
          outer.add( inner );
        }

        if ( !nested )
        {
          inner = new ArrayList< Integer >();
          outer.add( inner );
        }

        int closeParen = token.indexOf( ')' );
        if ( closeParen != -1 )
        {
          nested = false;
          token = token.substring( 0, closeParen );
        }
        if ( !token.isEmpty() )
          inner.add( new Integer( RDFReader.parseNumber( token ) ) );
      }
    }
    {
      short[][] outerb = new short[ outer.size() ][];
      int o = 0;
      for ( java.util.List< Integer > maps : outer )
      {
        short[] innerb = new short[ maps.size() ];
        outerb[ o++ ] = innerb;
        int i = 0;
        for ( Integer v : maps )
        {
          innerb[ i++ ] = v.shortValue();
        }
        maps.clear();
      }
      outer.clear();
      work.add( new ButtonMap( name, outerb ) );
    }
    buttonMaps = work.toArray( buttonMaps );
    return line;
  }
  
  private String parseNotes( RDFReader rdr, String line ) throws Exception
  {
    line = line.replaceAll("\\s+$","");   // right trim
    boolean more = line.endsWith( "\\" );
    StringBuilder sb = new StringBuilder();
    while ( more )
    {
      sb.append( line.substring( 0, line.length() - 1 ) ); 
      line = rdr.readLine();
      if ( line == null || ( line = line.trim() ).length() == 0 )
      {
        break;
      }
      more = line.endsWith( "\\" );
    }
    sb.append( line );
    return sb.toString().replaceAll( "\\\\n", "\n" );
  }

  /**
   * Parses the protocols.
   * 
   * @param rdr
   *          the rdr
   * @return the string
   * @throws Exception
   *           the exception
   */
  private String parseProtocols( RDFReader rdr ) throws Exception
  {
    String line;
    while ( true )
    {
      line = rdr.readLine();
      if ( line == null )
      {
        break;
      }
      if ( line.length() != 0 )
      {
        if ( line.charAt( 0 ) == '[' )
        {
          break;
        }
        StringTokenizer st = new StringTokenizer( line, "," );
        while ( st.hasMoreTokens() )
        {
          String token = st.nextToken().trim();
          ProtocolManager pm = ProtocolManager.getProtocolManager();
          // Get current version of protocol entry if different
          String ref = pm.getOldRefMap().get( token );
          if ( ref != null )
            token = ref;
          QualifiedID qid = new QualifiedID( token );
          String variantName = qid.variantName;
          Hex pid = qid.pid;
          List< String > v = protocolVariantNames.get( pid );
          if ( v == null )
          {
            v = new ArrayList< String >();
            protocolVariantNames.put( pid, v );
          }
          v.add( variantName );
        }
      }
    }
    return line;
  }
  
  private String parseExtender( RDFReader rdr ) throws Exception
  {
    String line;
    while ( true )
    {
      line = rdr.readLine();

      if ( line == null || line.length() == 0 )
      {
        break;
      }

      if ( oemSignatures == null )
      {
        oemSignatures = new ArrayList< String >();
      }
      StringTokenizer st = new StringTokenizer( line, "=, \t" );
      String name = st.nextToken().trim();
      if ( name.equalsIgnoreCase( "OEMSignature" ) )
      {
        while ( st.hasMoreTokens() )
        {
          oemSignatures.add( st.nextToken() );
        }
      }
    }
    return line;
  }

  /**
   * Gets the height.
   * 
   * @return the height
   */
  public int getHeight()
  {
    load();
    return height;
  }

  /** The height. */
  private int height;

  /**
   * Gets the width.
   * 
   * @return the width
   */
  public int getWidth()
  {
    load();
    return width;
  }

  /** The width. */
  private int width;

  /**
   * Supports variant.
   * 
   * @param pid
   *          the pid
   * @param name
   *          the name
   * @return true, if successful
   */
  public boolean supportsVariant( Hex pid, String variantName )
  {
    load();
    java.util.List< String > v = protocolVariantNames.get( pid );
    if ( v == null || v.isEmpty() )
    {
      return false;
    }

    return v.contains( variantName );
  }

  /**
   * Gets the supported variant names.
   * 
   * @param pid
   *          the pid
   * @return the supported variant names
   */
  public java.util.List< String > getSupportedVariantNames( Hex pid )
  {
    load();
    return protocolVariantNames.get( pid );
  }

  /*
   * public void clearButtonAssignments() { load(); for ( Enumeration e = buttons.elements(); e.hasMoreElements(); ) {
   * (( Button )e.nextElement()).setFunction( null ).setShiftedFunction( null ).setXShiftedFunction( null ); } }
   */

  /**
   * Sets the protocols.
   * 
   * @param protocols
   *          the new protocols
   */
  public void setProtocols( java.util.List< Protocol > protocols )
  {
    load();
    this.protocols = protocols;
  }

  /**
   * Gets the protocols.
   * 
   * @return the protocols
   */
  public java.util.List< Protocol > getProtocols()
  {
    load();
    return protocols;
  }

  private HashMap< Integer, HashMap< Integer, Integer >> setupCodes = new HashMap< Integer, HashMap< Integer, Integer >>();
  private HashMap< Integer, HashMap< Integer, Integer >> rfSetupCodes = null;

  private String parseSetupCodes( RDFReader rdr ) throws IOException
  {
    String line = null;
    HashMap< Integer, Integer > map = null;
    while ( true )
    {
      line = rdr.readLine();

      if ( line == null || line.length() == 0 )
      {
        break;
      }

      int pos = line.indexOf( '=' );
      int devTypeIndex = 0;
      if ( pos != -1 )
      {
        StringTokenizer st = new StringTokenizer( line, "=" );
        String token = st.nextToken().trim();
        try
        {
          devTypeIndex = Integer.parseInt( token );
        }
        catch ( Exception e )
        {
          devTypeIndex = token.charAt( 0 );
        }
        map = setupCodes.get( devTypeIndex );
        if ( map == null )
        {
          map = new HashMap< Integer, Integer >();
          setupCodes.put( devTypeIndex, map );
        }
        line = st.nextToken().trim();
      }
      StringTokenizer st = new StringTokenizer( line, " ," );
      while ( st.hasMoreTokens() )
      {
        String token = st.nextToken();
        int rf = token.indexOf( '*' );
        if ( rf >= 0 )
          token = token.substring( 0, rf );
        Integer code = new Integer( token );
        code += deviceCodeOffset;
        maxBuiltInCode = Math.max( code, maxBuiltInCode );
        if ( rf < 0 )
        {
          map.put( code, code );
        }
        else
        {
          if ( rfSetupCodes == null )
          {
            rfSetupCodes = new HashMap< Integer, HashMap<Integer,Integer> >();
          }
          if ( rfSetupCodes.get( devTypeIndex ) == null )
          {
            rfSetupCodes.put( devTypeIndex, new HashMap<Integer,Integer>() );
          }
          rfSetupCodes.get( devTypeIndex ).put( code, code );
        }
      }
    }

    return line;
  }

  public HashMap< Integer, HashMap< Integer, Integer >> getSetupCodes()
  {
    return setupCodes;
  }

  public HashMap< Integer, HashMap< Integer, Integer >> getRfSetupCodes()
  {
    return rfSetupCodes;
  }

  public boolean hasSetupCode( int deviceTypeIndex, int setupCode )
  {
    if ( setupCodes.size() == 0 )
    {
      return true;
    }
    HashMap< Integer, Integer > map = setupCodes.get( deviceTypeIndex );
    if ( map == null )
    {
      return false;
    }
    return map.containsKey( setupCode );
  }

  public boolean hasSetupCode( DeviceType deviceType, int setupCode )
  {
    return hasSetupCode( deviceType.getNumber(), setupCode );
  }
  
  public boolean hasRfSetupCode( int deviceTypeIndex, int setupCode )
  {
    if ( rfSetupCodes == null )
    {
      return false; 
    }
    HashMap< Integer, Integer > map = rfSetupCodes.get( deviceTypeIndex );
    if ( map == null )
    {
      return false;
    }
    return map.containsKey( setupCode );
  }

  public boolean hasRfSetupCode( DeviceType deviceType, int setupCode )
  {
    return hasRfSetupCode( deviceType.getNumber(), setupCode );
  }

  /**
   * Gets the encrypter decrypter.
   * 
   * @return the encrypter decrypter
   */
  public EncrypterDecrypter getEncrypterDecrypter()
  {
    load();
    return encdec;
  }

  /**
   * Creates the key move key.
   * 
   * @param keyCode
   *          the key code
   * @param deviceIndex
   *          the device index
   * @param deviceType
   *          the device type
   * @param setupCode
   *          the setup code
   * @param movedKeyCode
   *          the moved key code
   * @param notes
   *          the notes
   * @return the key move
   */
  public KeyMove createKeyMoveKey( int keyCode, int deviceIndex, int deviceType, int setupCode, int movedKeyCode,
      String notes )
  {
    KeyMove keyMove = null;
    keyMove = new KeyMoveKey( keyCode, deviceIndex, deviceType, setupCode, movedKeyCode, notes );
    return keyMove;
  }

  /**
   * Creates the key move.
   * 
   * @param keyCode
   *          the key code
   * @param deviceIndex
   *          the device index
   * @param deviceType
   *          the device type
   * @param setupCode
   *          the setup code
   * @param cmd
   *          the cmd
   * @param notes
   *          the notes
   * @return the key move
   */
  public KeyMove createKeyMove( int keyCode, int deviceIndex, int deviceType, int setupCode, Hex cmd, String notes )
  {
    KeyMove keyMove = null;
    if ( advCodeFormat == AdvancedCode.Format.HEX )
    {
      // KeyMoveLong should only be used for 1-byte commands when bind format is LONG
      // so added test of cmd length.
      if ( advCodeBindFormat == AdvancedCode.BindFormat.LONG && cmd.length() == 1 )
      {
        keyMove = new KeyMoveLong( keyCode, deviceIndex, deviceType, setupCode, cmd, notes );
      }
      else
      {
        keyMove = new KeyMove( keyCode, deviceIndex, deviceType, setupCode, cmd, notes );
      }
    }
    else if ( efcDigits == 3 )
    {
      keyMove = new KeyMoveEFC( keyCode, deviceIndex, deviceType, setupCode, EFC.parseHex( cmd ), notes );
    }
    else
    {
      // EFCDigits == 5
      keyMove = new KeyMoveEFC5( keyCode, deviceIndex, deviceType, setupCode, EFC5.parseHex( cmd ), notes );
    }
    if ( keyMove != null )
    {
      keyMove.setMemoryUsage( keyMove.getSize( this ) );
    }
    return keyMove;
  }

  /**
   * Creates the key move.
   * 
   * @param keyCode
   *          the key code
   * @param deviceIndex
   *          the device index
   * @param deviceType
   *          the device type
   * @param setupCode
   *          the setup code
   * @param efc
   *          the efc
   * @param notes
   *          the notes
   * @return the key move
   */
  public KeyMove createKeyMove( int keyCode, int deviceIndex, int deviceType, int setupCode, int efc, String notes )
  {
    KeyMove keyMove = null;
    if ( advCodeFormat == AdvancedCode.Format.HEX )
    {
      if ( efcDigits == 3 )
      {
        keyMove = new KeyMove( keyCode, deviceIndex, deviceType, setupCode, EFC.toHex( efc ), notes );
      }
      else
      {
        // EFCDigits == 5
        keyMove = new KeyMove( keyCode, deviceIndex, deviceType, setupCode, EFC5.toHex( efc ), notes );
      }
    }
    else if ( efcDigits == 3 )
    {
      keyMove = new KeyMoveEFC( keyCode, deviceIndex, deviceType, setupCode, efc, notes );
    }
    else
    {
      // EFCDigits == 5
      keyMove = new KeyMoveEFC5( keyCode, deviceIndex, deviceType, setupCode, efc, notes );
    }
    return keyMove;
  }

  public String getInterfaceType()
  {
    String name = getProcessor().getName();
    if ( name.equals( "HCS08" ) )
    {
      return segmentTypes == null ? "JP1.2" : "JPUSB";
    }
    else if ( name.equals( "S3F80" ) )
    {
      return  isSSD() ? "JPUSB" : segmentTypes == null ? "JP1.3" 
          : getE2FormatOffset() > 0 ? "JP1.4N" : "JP1.4";
    }
    else if ( name.equals( "SST" ) )
    {
      return "JP1.1";
    }
    else if ( name.equals( "MAXQ610" ) )
    {
      return getE2FormatOffset() > 0 ? "JP2N" : "JP2";
    }
    else if ( name.equals( "MAXQ612" ) )
    {
      return "JP3";
    }
    else if ( name.equals( "MAXQ622" ) )
    {
      return usesSimpleset() ? "JPS" : "JPUSB";
    }
    else if ( name.equals( "TI2541" ) || name.equals( "TI2530" ) )
    {
      return "JP3.1";
    }
    else
    {
      return "JP1";
    }
  }

  public String getProcessorDescription()
  {
    String name = getProcessor().getName();
    if ( name.equals( "S3C80" ) )
    {
      if ( RAMAddress == 0xFF00 )
      {
        return "Samsung S3C8+";
      }
      else
      {
        return "Samsung S3C8";
      }
    }
    else if ( name.equals( "S3F80" ) )
    {
      return "Samsung S3F8";
    }
    else if ( name.equals( "HCS08" ) )
    {
      return "Freescale HCS08";
    }
    else if ( name.equals( "6805" ) )
    {
      if ( getProcessor().getVersion().equals( "RC16/18" ) )
      {
        return "Motorola 6805RC16/18";
      }
      else
      {
        return "Motorola 6805C9";
      }
    }
    else if ( name.equals( "SST" ) )
    {
      return "SST SST65";
    }
    else if ( name.equals( "740" ) )
    {
      return "Mitsubishi P8/740";
    }
    else if ( name.equals( "MAXQ610" ) )
    {
      return "Maxim MAXQ610";
    }
    else if ( name.equals( "MAXQ612" ) )
    {
      return "Maxim MAXQ612";
    }
    else if ( name.equals( "MAXQ622" ) )
    {
      return "Maxim MAXQ622";
    }
    else if ( name.equals( "TI2541" ) )
    {
      return "Texas Instruments CC2541 F256";
    }
    else if ( name.equals( "TI2530" ) )
    {
      return "Texas Instruments CC2530 F128";
    }
    else if ( name.equals( "GP565" ) )
    {
      return "Qorvo GP565 NJC6";
    }
    else
    {
      return "<Unknown>";
    }
  }

  /**
   * Gets the max upgrade length.
   * 
   * @return the max upgrade length
   */
  public Integer getMaxUpgradeLength()
  {
    return maxUpgradeLength;
  }

  /**
   * Gets the max protocol length.
   * 
   * @return the max protocol length
   */
  public Integer getMaxProtocolLength()
  {
    return maxProtocolLength;
  }

  /**
   * Gets the max combined upgrade length.
   * 
   * @return the max combined upgrade length
   */
  public Integer getMaxCombinedUpgradeLength()
  {
    return maxCombinedUpgradeLength;
  }

  // Interface Comparable
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo( Remote o )
  {
    return names[ nameIndex ].compareTo( o.names[ o.nameIndex ] );
  }

  /**
   * Gets the shift mask.
   * 
   * @return the shift mask
   */
  public int getShiftMask()
  {
    return shiftMask;
  }

  public boolean getShiftEnabled()
  {
    return shiftEnabled;
  }

  /**
   * Gets the x shift mask.
   * 
   * @return the x shift mask
   */
  public int getXShiftMask()
  {
    return xShiftMask;
  }

  /**
   * Gets the x shift enabled.
   * 
   * @return the x shift enabled
   */
  public boolean getXShiftEnabled()
  {
    return xShiftEnabled;
  }

  /**
   * Sets the x shift enabled.
   * 
   * @param flag
   *          the new x shift enabled
   */
  public void setXShiftEnabled( boolean flag )
  {
    xShiftEnabled = flag;
  }

  /**
   * Gets the shift label.
   * 
   * @return the shift label
   */
  public String getShiftLabel()
  {
    return shiftLabel;
  }

  /**
   * Gets the x shift label.
   * 
   * @return the x shift label
   */
  public String getXShiftLabel()
  {
    return xShiftLabel;
  }

  /**
   * Gets the protocol vector offset.
   * 
   * @return the protocol vector offset
   */
  public int getProtocolVectorOffset()
  {
    return protocolVectorOffset;
  }

  /**
   * Gets the protocol data offset.
   * 
   * @return the protocol data offset
   */
  public int getProtocolDataOffset()
  {
    return protocolDataOffset;
  }

  public int getSeqShift()
  {
    return seqShift;
  }

  /**
   * Gets the supports binary upgrades.
   * 
   * @return the supports binary upgrades
   */
  public boolean getSupportsBinaryUpgrades()
  {
    return supportsBinaryUpgrades;
  }

  public int getSoftHomeTheaterType()
  {
    // For remotes with soft Home Theater, returns the internal device index used
    // for this device type, as distinct from the index in the RDF entry that is the
    // position of Home Theater in the [DeviceTypes] section.
    if ( softHomeTheater == null )
    {
      return -1;
    }
    return deviceTypeList.get( softHomeTheater.getDeviceType() ).getNumber();
  }

  public int getSoftHomeTheaterCode()
  {
    // For remotes with soft Home Theater, returns the setup code used
    // for this device type.
    if ( softHomeTheater == null )
    {
      return -1;
    }
    return softHomeTheater.getDeviceCode();
  }

  /** The file. */
  private File file = null;

  /** The signature. */
  private String signature = null;
  
  private int sigAddress = 2;

  /** The names. */
  private String[] names = new String[ 1 ];

  /** The name index. */
  private int nameIndex = 0;

  /** The loaded. */
  private boolean loaded = false;

  /** The base address. */
  private int baseAddress = 0;

  /** The eeprom size. */
  private int eepromSize;

  /** The device code offset. */
  private int deviceCodeOffset;
  
  private int maxBuiltInCode = 0;
  
  private int learnedFormat = -1;
  
  private String notes = null;

  public String getNotes()
  {
    return notes;
  }

  /** The fav key. */
  private FavKey favKey = null;

  public boolean hasFavKey()
  {
    if ( favKey == null )
    {
      return false;
    }
    return favKey.getDeviceButtonAddress() != 0 || advCodeBindFormat == AdvancedCode.BindFormat.LONG;
  }
  
  public boolean hasFavorites()
  {
    return favKey != null && ( segmentTypes != null && segmentTypes.contains( 0x1D ) || isSSD() );
  }

  /**
   * Gets the fav key.
   * 
   * @return the fav key
   */
  public FavKey getFavKey()
  {
    return favKey;
  }

  public boolean hasTimedMacroSupport()
  {
    return timedMacroAddress != null || macroCodingType.hasTimedMacros();
  }
  
  public boolean hasLearnedSupport()
  {
    return learnedAddress != null || ( segmentTypes != null && segmentTypes.contains( 9 ) ) || isSSD();
  }
  
  public boolean hasFreeProtocols()
  {
    return segmentTypes == null || segmentTypes.contains( 0x0F );
  }
  
  public boolean hasActivitySupport()
  {
    return segmentTypes != null && ( segmentTypes.contains( 0xDB ) || segmentTypes.contains( 0xE9 )
        || segmentTypes.contains( 0xCD ) || segmentTypes.contains( 0x1E ) || hasActivityAlgorithm()
        || usesSimpleset() || isSSD() );
  }
  
  public boolean hasActivityAlgorithm()
  {
    return segmentTypes != null && segmentTypes.contains( 0x2F );
  }
  
  public boolean hasActivityInitialMacro()
  {
    return segmentTypes != null && ( segmentTypes.contains( 0x1E ) || isSSD() );
  }
  
  public boolean hasRf4ceSupport()
  {
    return segmentTypes != null && segmentTypes.contains( 0x2D );
  }
  
  public boolean hasSettings()
  {
    if ( segmentTypes == null )
    {
      return true;
    }
    for ( Setting s : settings )
    {
      int segmentType = ( s.getByteAddress() >> 8 ) - 1;
      if ( segmentType >= 0 )
      {
        return true;
      }
    }
    return false;
  }

  /** The oem device. */
  private OEMDevice oemDevice = null;

  /** The oem control. */
  @SuppressWarnings( "unused" )
  private int oemControl = 0;

  /** The upgrade bug. */
  private boolean upgradeBug = false;

  public boolean hasUpgradeBug()
  {
    return upgradeBug;
  }

  /** The advanced code address. */
  private AddressRange advancedCodeAddress = null;
  private AddressRange usageRange = null;
  /**
   * Gets the advanced code address.
   * 
   * @return the advanced code address
   */
  public AddressRange getAdvancedCodeAddress()
  {
    return advancedCodeAddress;
  }
  
  public AddressRange getUsageRange()
  {
    return usageRange;
  }

  // Only used with remotes that have FavScan area segregated.
  private AddressRange favScanAddress = null;

  public AddressRange getFavScanAddress()
  {
    return favScanAddress;
  }

  public void setFavScanAddress( AddressRange favScanAddress )
  {
    this.favScanAddress = favScanAddress;
  }

  /** The macro support. */
  private boolean macroSupport = true;

  public boolean hasMacroSupport()
  {
    return macroSupport;
  }

  private boolean keyMoveSupport = true;

  public boolean hasKeyMoveSupport()
  {
    return keyMoveSupport;
  }
  
  public boolean ledColor = false;
  private Integer[] ledParams = null;
  private String[] ledSettings = null;
  
  public Integer[] getLedParams()
  {
    return ledParams;
  }

  public String[] getLedSettings()
  {
    return ledSettings;
  }

  private boolean deviceSelection = true;
  
  private boolean deviceSelectionMessage = false;
  
  /**
   *  Certain remotes, such as the URC-6820Z Zapper+, support more than one device
   *  but have no means of selecting between them.  Instead there are fixed punchthroughs
   *  that assign certain buttons to each device that has an assigned setup code.  For
   *  such remotes, hasDeviceSelection() will return false.  The URC-7935 is of this type
   *  but needs hasDeviceSelection() returning true, as it uses device button values
   *  in learned signal segments even though it has no means of device selection.  To
   *  handle this, there is also deviceSelectionMessage, which when true means that the
   *  General Panel message about no device selection is displayed while deviceSelection
   *  remains true.  
   */
  public boolean hasDeviceSelection()
  {
    return deviceSelection;
  }

  public boolean needsDeviceSelectionMessage()
  {
    return deviceSelectionMessage;
  }

  public boolean usesLedColor()
  {
    return ledColor;
  }

  /** The upgrade address. */
  private AddressRange upgradeAddress = null;

  /**
   * Gets the upgrade address.
   * 
   * @return the upgrade address
   */
  public AddressRange getUpgradeAddress()
  {
    return upgradeAddress;
  }

  public AddressRange getDeviceUpgradeAddress()
  {
    return deviceUpgradeAddress;
  }
  
  /** Returns:
   *  0 if only device independent upgrades,
   *  1 if only dependent upgrades (JPUSB remotes),
   *  2 if both dependent and independent upgrades.
   *  
   *  This value is the number of additional columns required in device upgrade panel.
   */
  public int hasDeviceDependentUpgrades()
  {
    return deviceUpgradeAddress != null ? 2 : usesEZRC() ? 1 : 0;
  }

  /** The device upgrade address. */
  private AddressRange deviceUpgradeAddress = null;

  /** The timed macro address. */
  private AddressRange timedMacroAddress = null;

  /** The timed macro warning. */
  private boolean timedMacroWarning = false;

  /** The learned address. */
  private AddressRange learnedAddress = null;

  /**
   * Gets the learned address.
   * 
   * @return the learned address
   */
  public AddressRange getLearnedAddress()
  {
    return learnedAddress;
  }
  
  public int getLearnedFormat()
  {
    if ( learnedFormat >= 0 )
    {
      return learnedFormat;
    }
    // JP1.3 and earlier are format 0, S3F80 remotes with segments are type 4 by default
    String procName = getProcessor().getEquivalentName();
    return procName.equals( "MAXQ610" ) ? 1 : procName.equals( "TI2541" ) ? 2 
        : segmentTypes != null ? 4 : 0;
  }

  public AddressRange getTimedMacroAddress()
  {
    return timedMacroAddress;
  }

  public boolean hasTimedMacroWarning()
  {
    return timedMacroWarning;
  }
  
  public boolean hasActivityControl()
  {
    return activityControl != null && activityControl.length > 0;
  }

  public ImageIcon getImage()
  {
    if ( imageMaps == null || mapIndex >= imageMaps.length )
    {
      return null;
    }

    ImageMap map = imageMaps[ mapIndex ];
    if ( map == null || map.getImageFile() == null )
    {
      return null;
    }
    return new ImageIcon( map.getImageFile().getAbsolutePath() );
  }

  /** The processor. */
  private Processor processor = null;
  // private String processorVersion = null;
  
  /** The RAM address. */
  private int RAMAddress = 0;  // unset

  private AutoClockSet autoClockSet = null;

  private ExtenderVersionParm extenderVersionParm = null;

  public ExtenderVersionParm getExtenderVersionParm()
  {
    return extenderVersionParm;
  }
  
  private List< String > oemSignatures = null;

  public List< String > getOemSignatures()
  {
    return oemSignatures;
  }

  /** The RDF sync. */
  @SuppressWarnings( "unused" )
  private int RDFSync;

  /** The punch thru base. */
  @SuppressWarnings( "unused" )
  private int punchThruBase;

  /** The scan base. */
  @SuppressWarnings( "unused" )
  private int scanBase = 0;

  /** The sleep status bit. */
  private StatusBit sleepStatusBit = null;

  /** The vpt status bit. */
  private StatusBit vptStatusBit = null;

  /** The check sums. */
  private CheckSum[] checkSums = new CheckSum[ 0 ];

  /** The settings. */
  private Setting[] settings = new Setting[ 0 ];
  
  private HashMap< Integer, Integer > settingAddresses = new HashMap< Integer, Integer >();
  private HashMap< Integer, Integer > settingMasks = new HashMap< Integer, Integer >();
  private List< Integer > settingBytes = new ArrayList< Integer >();

  public HashMap< Integer, Integer > getSettingAddresses()
  {
    return settingAddresses;
  }

  public HashMap< Integer, Integer > getSettingMasks()
  {
    return settingMasks;
  }

  public List< Integer > getSettingBytes()
  {
    return settingBytes;
  }

  /** The fixed data. */
  private FixedData[] fixedData = new FixedData[ 0 ];
  
  private FixedData[] rawFixedData = new FixedData[ 0 ];
  
  private String punchThru = "VTC";

  /** The auto set data */
  private FixedData[] autoSet = new FixedData[ 0 ];

  /** The device buttons. */
  private DeviceButton[] deviceButtons = new DeviceButton[ 0 ];
  private int seqShift = 4;

  /** The device types. */
  private LinkedHashMap< String, DeviceType > deviceTypes = new LinkedHashMap< String, DeviceType >();
  private Hashtable< Integer, DeviceType > devicesByType = new Hashtable< Integer, DeviceType >();

  /** The device types as an array in the order given in the RDF. */
  private java.util.List< DeviceType > deviceTypeList = new ArrayList< DeviceType >();

  /** The device type aliases. */
  private Hashtable< String, DeviceType > deviceTypeAliases = new Hashtable< String, DeviceType >();

  /** The device type alias names. */
  private String[] deviceTypeAliasNames = null;

  /** The buttons. */
  private java.util.List< Button > buttons = new ArrayList< Button >();

  /** The buttons by key code. */
  private Hashtable< Integer, Button > buttonsByKeyCode = new Hashtable< Integer, Button >();

  /** The buttons by name. */
  private Hashtable< String, Button > buttonsByName = new Hashtable< String, Button >();

  /** The buttons by standard name. */
  private Hashtable< String, Button > buttonsByStandardName = new Hashtable< String, Button >();

  /** The upgrade buttons - bindable in key moves. */
  private Button[] upgradeButtons = new Button[ 0 ];
  
  /** The upgrade buttons - bindable in key moves, omitting shifted/XShifted forms. */
  private Button[] baseUpgradeButtons = new Button[ 0 ];
  
  /** Buttons bindable in macros. */
  private Button[] macroButtons = new Button[ 0 ];
  
  /** Buttons bindable in learned signals. */
  private Button[] learnButtons = new Button[ 0 ];
  
  /** Buttons created for XSight Touch to represent functions unassigned to a button */
  private List< Button > functionButtons = new ArrayList< Button >();
  
  /** All buttons other than those that are shifted or XShifted forms of base buttons */
  private List< Button > distinctButtons = new ArrayList< Button >();

  /** The phantom shapes. */
  private java.util.List< ButtonShape > phantomShapes = new ArrayList< ButtonShape >();

  /** The digit maps. */
  private short[] digitMaps = new short[ 0 ];
  
  private List< Integer > segmentTypes = null;

  /** The button maps. */
  private ButtonMap[] buttonMaps = new ButtonMap[ 0 ];
  
  private LinkedHashMap< String, List< Button > > buttonGroups = null;
  
  private ButtonShape inputButtonShape = null;
  
  private Button[][] activityButtonGroups = null;
  private KeyButtonGroup[] keyButtonGroups = null;
  
//  private DeviceButton[][][] activityControl = new DeviceButton[ 0 ][ 0 ][ 0 ];
  private Activity.Control[] activityControl = new Activity.Control[ 0 ];
  
  /** The omit digit map byte. */
  private boolean omitDigitMapByte = false;
  
  private boolean globalSpecialFunctions = false;

  /** The protocol variant names. */
  private Hashtable< Hex, java.util.List< String >> protocolVariantNames = new Hashtable< Hex, java.util.List< String >>();

  /** The protocols. */
  private java.util.List< Protocol > protocols = null;

  /** The image maps. */
  private ImageMap[] imageMaps = new ImageMap[ 0 ];
  
  private String[] imageMapNames = new String[ 0 ];

  /** The map index. */
  private int mapIndex = 0;

  /** The shift mask. */
  private int shiftMask = 0x80; 
  
  private boolean shiftEnabled = true;;

  /** The x shift mask. */
  private int xShiftMask = 0xC0;

  /** The x shift enabled. */
  private boolean xShiftEnabled = false;

  /** The shift label. */
  private String shiftLabel = "Shift";

  /** The x shift label. */
  private String xShiftLabel = "XShift";

  /** The default restrictions. */
  private int defaultRestrictions = 0;

  /** The adv code format. */
  private AdvancedCode.Format advCodeFormat = AdvancedCode.Format.HEX;

  /** The adv code bind format. */
  private AdvancedCode.BindFormat advCodeBindFormat = AdvancedCode.BindFormat.NORMAL;

  /** The efc digits. */
  private int efcDigits = 3;

  /** The dev comb address. */
  private int[] devCombAddress = null;

  /** The protocol vector offset. */
  private int protocolVectorOffset = 0;

  /** The protocol data offset. */
  private int protocolDataOffset = 0;

  /** The encdec. */
  private EncrypterDecrypter encdec = null;

  /** The supports binary upgrades. */
  private boolean supportsBinaryUpgrades = false;

  /** The max protocol length. */
  private Integer maxProtocolLength = null;

  /** The max upgrade length. */
  private Integer maxUpgradeLength = null;

  /** The max combined upgrade length. */
  private Integer maxCombinedUpgradeLength = null;

  /** The section terminator. */
  private short sectionTerminator = 0;
  
  private int activityMapIndex = 0;

  private int rdfVersionAddress = 0;
  
  private BlockFormat blockFormat = BlockFormat.DEFAULT;

  public int getRdfVersionAddress()
  {
    return rdfVersionAddress;
  }

  /**
   * Gets the section terminator.
   * 
   * @return the section terminator
   */
  public short getSectionTerminator()
  {
    return sectionTerminator;
  }

  /** The special protocols. */
  public java.util.List< SpecialProtocol > specialProtocols = new ArrayList< SpecialProtocol >();

  /**
   * Gets the special protocols.
   * 
   * @return the special protocols
   */
  public java.util.List< SpecialProtocol > getSpecialProtocols()
  {
    return specialProtocols;
  }

  /** The two byte pid. */
  private boolean twoBytePID = false;
  
  /**
   * Set when setup codes can be more than 12 bits
   */
  private boolean twoByteSetupCode = false;

  /**
   * Uses two byte pid.
   * 
   * @return true, if successful
   */
  public boolean usesTwoBytePID()
  {
    return twoBytePID;
  }
  
  public boolean usesTwoByteSetupCode()
  {
    return twoByteSetupCode;
  }
  
  public boolean usesEZRC()
  {
    return signature.startsWith( "USB" );
  }
  
  public boolean usesSimpleset()
  {
    return processor.getName().equals( "MAXQ622" ) && !signature.startsWith( "USB" );
  }
  
  /**
   * JP2style remotes are those which have segments and which use jp12serial.
   * There is no direct test for using jp12serial so it is done here by excluding
   * EZRC and Simpleset remotes.  In RMIR, this is the category of remotes that are
   * allowed to have an EEPROM size that is not a whole number of flash pages.
   * At the time of writing, the only known example is the URC7955, where the EEPROM
   * area is 4 bytes short of two 0x800-byte flash pages.
   */
  public boolean isJP2style()
  {
    return getSegmentTypes() != null && !usesEZRC() && !usesSimpleset();
  }
  
  public boolean usesIcons()
  {
    return isSSD();
  }

  /** The learned dev btn swapped. */
  private boolean learnedDevBtnSwapped = false;

  /**
   * Gets the learned dev btn swapped.
   * 
   * @return the learned dev btn swapped
   */
  public boolean getLearnedDevBtnSwapped()
  {
    return learnedDevBtnSwapped;
  }

  /** The restriction table. */
  private static Hashtable< String, Integer > restrictionTable = null;

  private DeviceLabels labels = null;

  public DeviceLabels getDeviceLabels()
  {
    return labels;
  }

  private SoftDevices softDevices = null;

  public SoftDevices getSoftDevices()
  {
    return softDevices;
  }

  private SoftHomeTheater softHomeTheater = null;

  public SoftHomeTheater getSoftHomeTheater()
  {
    return softHomeTheater;
  }

  private MacroCodingType macroCodingType = new MacroCodingType();

  public MacroCodingType getMacroCodingType()
  {
    return macroCodingType;
  }

  private int startReadOnlySettings = Integer.MAX_VALUE;

  public int getStartReadOnlySettings()
  {
    return startReadOnlySettings;
  }

  private Hashtable< String, PauseParameters > pauseParameters = new Hashtable< String, PauseParameters >();

  public Hashtable< String, PauseParameters > getPauseParameters()
  {
    return pauseParameters;
  }

  private short[] powerButtons = new short[ 0 ];

  public short[] getPowerButtons()
  {
    return powerButtons;
  }

  private boolean waveUpgrade = false;

  public boolean supportWaveUpgrade()
  {
    load();
    return waveUpgrade;
  }
  
  private boolean forceEvenStarts = false;
  private int forceModulus = 1;

  public boolean doForceEvenStarts()
  {
    return forceEvenStarts;
  }
  
  public int getForceModulus()
  {
    return forceModulus;
  }

  public boolean isSoftButton( Button btn )
  {
    return buttonGroups.get( "Soft" ) != null && buttonGroups.get( "Soft" ).contains( btn )
      || buttonGroups.get( "User" ) != null && buttonGroups.get( "User" ).contains( btn );
  }

  private boolean masterPowerSupport = false;

  public boolean hasMasterPowerSupport()
  {
    return masterPowerSupport;
  }
  
  public String getPunchThru()
  {
    return punchThru;
  }

  public FixedData[] getFixedData()
  {
    // Note that fixedData can be set to null after being parsed
    return fixedData;
  }
  
  public FixedData[] getRawFixedData()
  {
    // This will always return the non-null, original, fixedData
    return rawFixedData;
  }

  public void setFixedData( FixedData[] fixedData )
  {
    this.fixedData = fixedData;
  }

  public FixedData[] getAutoSet()
  {
    return autoSet;
  }

  private SetupValidation setupValidation = SetupValidation.OFF;

  public SetupValidation getSetupValidation()
  {
    return setupValidation;
  }

  private int keyMoveCode = 0;

  public int getKeyMoveCode()
  {
    return keyMoveCode;
  }

  private int macroCode = 0x10;

  public int getMacroCode()
  {
    return macroCode;
  }

  private int deviceIndexMask = 0x0F;

  public int getDeviceIndexMask()
  {
    return deviceIndexMask;
  }

  private int sequenceNumberMask = 0x70;

  public int getSequenceNumberMask()
  {
    return sequenceNumberMask;
  }

  private int maxMultiMacros = 3;

  public int getMaxMultiMacros()
  {
    return maxMultiMacros;
  }

  private String rdfName = "Not Specified";

  public String getRdfName()
  {
    return rdfName;
  }

  private String rdfIdentification = "None";

  public String getRdfIdentification()
  {
    return rdfIdentification;
  }

  private void parseAdvCodeTypes( String text, RDFReader rdr ) throws Exception
  {
    // AdvCodeTypes=KeyMoveCode,MacroCode[:DeviceIndexMask[:SequenceNumberMask:MaxMultiMacros]][,FavScanCode]
    Iterator< String > iterator = LineTokenizer.tokenize( text, "," ).iterator();

    String code = null;

    // The key move code
    if ( iterator.hasNext() && ( code = iterator.next() ) != null )
    {
      keyMoveCode = RDFReader.parseNumber( code );
      keyMoveSupport = true;
    }
    else
    {
      keyMoveSupport = false;
    }

    // The Macro code and sub-types
    if ( iterator.hasNext() && ( code = iterator.next() ) != null )
    {
      macroSupport = true;
      String subCode = null;

      Iterator< String > subIterator = LineTokenizer.tokenize( code, ":" ).iterator();

      // The macro code
      if ( iterator.hasNext() && ( subCode = subIterator.next() ) != null )
      {
        macroCode = RDFReader.parseNumber( code );
      }

      // The device index mask
      if ( iterator.hasNext() && ( subCode = subIterator.next() ) != null )
      {
        deviceIndexMask = RDFReader.parseNumber( code );
      }

      // The multi macro sequence number mask
      if ( iterator.hasNext() && ( subCode = subIterator.next() ) != null )
      {
        sequenceNumberMask = RDFReader.parseNumber( code );
      }

      // The maximum number of macros that can be assigned to a multi macro key
      if ( iterator.hasNext() && ( subCode = subIterator.next() ) != null )
      {
        sequenceNumberMask = RDFReader.parseNumber( code );
      }
    }
    else
    {
      macroSupport = false;
    }
  }
  
  public void setDeviceComboBox( JComboBox deviceBox )
  {
    DeviceButton[] allDB = getDeviceButtons();
    List< DeviceButton > dbList = new ArrayList< DeviceButton >();
    for ( DeviceButton db : allDB )
    {
      if ( db.getUpgrade() != null )
      {
        dbList.add( db );
      }
    }
    DefaultComboBoxModel comboModel = new DefaultComboBoxModel( dbList.toArray() );
    deviceBox.setModel( comboModel );
    if ( dbList.size() > 0 )
    {
      deviceBox.setSelectedIndex( 0 );
    }
  }
  
  public List< Short > getActivityOrder()
  {
    return buttonMaps[ activityMapIndex ].getKeyCodeSingleList();
  }
  
  public void correctType04Macro( Macro macro )
  {
    // This corrects an ill-formed type 04 macro created by early versions
    // of Simpleset support in RMIR
    
    short[] data = macro.getData().getData();
    int len = data.length;
    List< DeviceButton > devs = new ArrayList< DeviceButton >();
    DeviceButton db = null;
    for ( int i = 0; i < len / 2; i++ )
    {
      db = getDeviceButton( data[ 2 * i ] );
      if ( db != null )
      {
        devs.add( db );
      }
    }
    Hex hex = new Hex( 2 * deviceButtons.length );
    for ( int i = 0; i < deviceButtons.length; i++ )
    {
      db = deviceButtons[ i ];
      hex.set( ( short )( devs.contains( db ) ? db.getButtonIndex() : 0xFF ), 2*i );
      hex.set( getButtonByStandardName( "Power" ).getKeyCode(), 2*i + 1 );
    }
    macro.setData( hex );
  }
  
  public final static String[] userFilenames = {
    "home.xcf", "system.xcf", "devices.xcf", "activities.xcf", "profiles.xcf",
    "favorites.xcf", "macros.xcf", "snstest.xcf", "usericons.pkg", "SysIcons.pkg" 
  };
  
  private LinkedHashMap< String, Integer > gidMap = null;
  
  public LinkedHashMap< String, Integer > getGidMap()
  {
    return gidMap;
  }
  
  public static int scaleColor( int val )
  {
    double top = 40.0;
    double frac = val*top/40.0;
    frac = Math.log1p( frac );
    return (int)Math.round( 255*frac/Math.log1p( top ) );
  }
  
  public static Color getColorByIndex( int ndx )
  {
    ndx--;
    int r = scaleColor( colorHex.getData()[ 3*ndx ] );
    int g = scaleColor( colorHex.getData()[ 3*ndx + 1] );
    int b = scaleColor( colorHex.getData()[ 3*ndx + 2 ] );
    return new Color( r, g, b );
  }
  
  public static Hex colorHex = null;

  public final static String[] ueiNames = {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    "Volume Up", "Volume Down", "Mute", "Channel Up", "Channel Down",
    "Menu", "Menu Up", "Menu Down", "Menu Left", "Menu Right",
    "Guide", "Exit", "OK", "Power", "i", "Input", "Enter",
    "Red", "Green", "Yellow", "Blue", "Play", "Pause", "Stop",
    "Search Reverse", "Search Forward", "Record", "Last Channel",
    "Skip Reverse", "Skip Forward", "List"

  };
  
  public final static Integer[] ueiGids = {
    0x7050, 0x7051, 0x7052, 0x7053, 0x7054, 0x7055, 0x7056, 0x7057, 0x7058, 0x7059,
    0x709C, 0x709B, 0x707C, 0x7061, 0x7060,
    0x707B, 0x7096, 0x7069, 0x7079, 0x708C,
    0x722F, 0x706C, 0x708E, 0x7086, 0x7A0D, 0x727B, 0x7227,
    0x7070, 0x706F, 0x7071, 0x7324, 0x7085, 0x707D, 0x7092,
    0x708B, 0x706D, 0x708A, 0x72FB,
    0x7391, 0x7390, 0x707A
  };
  
  public static boolean prelimLoad = false;
  
  private final static String colorData = "28 28 28 28 00 00 00 28 00 00 00 28 28 28 00 00 28 28 28 00 "
      + "28 1E 1E 1E 14 14 14 14 00 00 14 14 00 00 14 00 14 00 14 00 14 14 00 00 14 28 05 00 16 00 00 1A "
      + "07 07 1C 05 05 22 03 09 28 0F 0B 28 14 0D 20 0E 0E 26 14 14 24 17 13 27 14 11 28 19 13 28 0A 00 "
      + "28 16 00 28 1A 00 28 22 00 1D 15 02 22 1A 05 25 24 1B 1E 1D 11 26 24 16 18 20 08 0D 11 07 11 16 "
      + "05 12 1D 00 13 27 00 14 28 00 1B 28 07 02 13 00 00 10 00 00 14 00 05 16 05 08 20 08 17 25 17 18 "
      + "27 18 16 1D 16 00 27 18 00 28 14 07 16 0E 10 20 1B 09 1C 12 05 1C 1B 07 0C 0C 00 14 14 00 16 16 "
      + "23 28 28 00 20 21 0A 23 21 0B 20 21 1B 25 25 14 28 21 1C 23 24 0F 1B 19 0B 14 1C 10 17 25 00 1E "
      + "28 05 17 28 1B 22 24 15 20 25 15 20 27 04 04 12 00 00 16 00 00 20 0A 10 23 16 07 23 0C 00 14 0B "
      + "0A 16 11 0E 20 13 10 25 17 12 22 16 00 16 17 00 21 18 08 20 1D 0D 21 22 1E 22 23 19 23 25 14 25 "
      + "22 12 21 1F 03 15 22 12 17 28 03 17 28 10 1C 28 1E 20 27 25 22 26 26 18 16 0B 03 1D 16 16 12 14 "
      + "17 1C 1F 23 24 24 27 28 27 26 26 27 28";

}
