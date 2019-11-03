package com.hifiremote.jp1;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.hifiremote.jp1.GeneralFunction.IconPanel;
import com.hifiremote.jp1.GeneralFunction.IconRenderer;
import com.hifiremote.jp1.GeneralFunction.RMIcon;
import com.hifiremote.jp1.RemoteConfiguration.ShareType;

// TODO: Auto-generated Javadoc
/**
 * The Class DeviceButtonTableModel.
 */
public class DeviceButtonTableModel extends JP1TableModel< DeviceButton >
{

  /**
   * Instantiates a new device button table model.
   */

  public DeviceButtonTableModel()
  {
    deviceTypeEditor = new DeviceTypeEditor( deviceTypeBox, softHT );
    deviceTypeEditor.setClickCountToStart( RMConstants.ClickCountToStart );
    punchThroughEditor = new DefaultCellEditor( deviceButtonBox );
    punchThroughEditor.setClickCountToStart( RMConstants.ClickCountToStart );
    sequenceEditor = new DefaultCellEditor( sequenceBox );
    sequenceEditor.setClickCountToStart( RMConstants.ClickCountToStart );
    deviceLockEditor = new DefaultCellEditor( deviceLockBox );
    deviceLockEditor.setClickCountToStart( RMConstants.ClickCountToStart );
  }

  /**
   * Sets the.
   * 
   * @param remoteConfig
   *          the remote config
   */
  public void set( RemoteConfiguration remoteConfig )
  {
    this.remoteConfig = remoteConfig;
    if ( remoteConfig != null )
    {
      colorEditor = new RMColorEditor( remoteConfig.getOwner() );
      Remote remote = remoteConfig.getRemote();
      if ( remote.usesEZRC() )
      {
        setData( remoteConfig.getDeviceButtonList() );
      }
      else
      {
        setData( remote.getDeviceButtons() );
      }
      SoftDevices softDevices = remote.getSoftDevices();
      if ( remote.getSoftHomeTheaterType() >= 0 )
      {
        // Set the values passed to DeviceTypeEditor
        softHT.setUse( true );
        softHT.setDeviceType( remote.getSoftHomeTheaterType() );
        softHT.setDeviceCode( remote.getSoftHomeTheaterCode() );
      }
      DefaultComboBoxModel comboModel = new DefaultComboBoxModel( remote.getAllDeviceTypes() );
      if ( softDevices != null && softDevices.inUse() && !softDevices.isSetupCodesOnly() )
      {
        comboModel.addElement( new DeviceType( "", 0, 0xFFFF ) );
      }
      deviceTypeBox.setModel( comboModel );
      comboModel = new DefaultComboBoxModel( remote.getDeviceButtons() );
      comboModel.insertElementAt( DeviceButton.noButton, 0 );
      deviceButtonBox.setModel( comboModel );
      if ( softDevices != null && softDevices.usesSequence() )
      {
        adjustSequenceRange();
      }
      setupCodeRenderer = new SetupCodeRenderer( remoteConfig );
      setupCodeEditor = new SetupCodeEditor( setupCodeRenderer );
      if ( remote.isSSD() )
      {
        iconEditor = new RMSetterEditor< RMIcon, IconPanel >( IconPanel.class );
        iconEditor.setRemoteConfiguration( remoteConfig );
        iconEditor.setTitle( "Icon Editor" );
        iconRenderer = new IconRenderer();
      }
      if ( remote.usesLedColor() )
      {
        int max = Remote.colorHex.getData().length / 3;
        spinnerEditor = new SpinnerCellEditor( 1, max, 1 );
        spinnerEditor.setColorHex( Remote.colorHex );
      }
    }
  }

  private int getDeviceCount()
  {
    int len = 0;
    for ( int i = 0; i < getRowCount(); i++ )
    {
      if ( getExtendedTypeIndex( i ) != 0xFF )
      {
        len++ ;
      }
    }
    return len;
  }

  private void adjustSequenceRange()
  {
    int len = getDeviceCount();
    Integer[] values = new Integer[ len ];
    for ( int i = 0; i < len; i++ )
    {
      values[ i ] = i + 1;
    }
    sequenceBox.setModel( new DefaultComboBoxModel( values ) );
  }

  /**
   * Sets the editable.
   * 
   * @param flag
   *          the new editable
   */
  public void setEditable( boolean flag )
  {
    editable = flag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount()
  {
    int count = 5;

    if ( remoteConfig != null )
    {
      Remote remote = remoteConfig.getRemote();
      DeviceLabels labels = remote.getDeviceLabels();
      if ( labels != null )
      {
        for ( int i = 0; i < 3; i++ )
        {
          count += labels.columnNames[ i ] != null ? 1 : 0;
        }
      }
      SoftDevices softDevices = remote.getSoftDevices();
      if ( softDevices != null && softDevices.usesSequence() )
      {
        ++count;
      }
      if ( remoteConfig.hasSegments() )
      {
        if ( remote.isSSD() )
        {
          count++;
        }
        else
        {
          count++;  // add Setup Lock
          String pt = remote.getPunchThru();
          for ( int i = 0; i < 6; i++ )
          {
            if ( pt.indexOf( "VTCXYZ".charAt( i ) ) >= 0 )
            {
              count++;
            }
          }
        }
      }
      if ( remote.usesIcons() )
      {
        count += 1;
      }
      if ( remote.usesLedColor() )
      {
        count += 1;
      }
      if ( remoteConfig.allowHighlighting() )
      {
        ++count;
      }
    }
    
    return count;
  }

  /*
   * A remote can have a Sequence column (index 6) but no Label column (index 5), so map actual column number to an
   * effective column number
   */
  public int getEffectiveColumn( int col )
  {
    if ( remoteConfig != null )
    {
      Remote remote = remoteConfig.getRemote();
      DeviceLabels labels = remote.getDeviceLabels();
      if ( col > 3 )
      {
        if ( remote.isSSD() || !remoteConfig.hasSegments() )
        {
          // Skip the setup lock and punchthrough columns
          col += 7;
        }
        else if ( col > 4 )
        {
          // Check which punchthrough columns are required
          String pt = remote.getPunchThru();
          if ( pt.indexOf( 'V' ) < 0 )
          {
            col++;
          }
          if ( col > 5 && pt.indexOf( 'T' ) < 0 )
          {
            col++;
          }
          if ( col > 6 && pt.indexOf( 'C' ) < 0 )
          {
            col++;
          }
          if ( col > 7 && pt.indexOf( 'X' ) < 0 )
          {
            col++;
          }
          if ( col > 8 && pt.indexOf( 'Y' ) < 0 )
          {
            col++;
          }
          if ( col > 9 && pt.indexOf( 'Z' ) < 0 )
          {
            col++;
          }
        }
      }
      if ( !remote.isSSD() && col > 10 )
      {
        // Skip the Volume Lock column
        col++;
      }
      if ( col > 11 && !remote.usesLedColor() )
      {
        // Skip the Ring Color column
        col++;
      }
      for ( int i = 0; i < 3; i++ )
      {
        // Skip unused labels columns
        col += col >= i + 14 && ( labels == null || labels.columnNames[ i ] == null ) ? 1 : 0;
      }
      if ( !remote.usesIcons() && col >= 17 )
      {
        // Skip IconRef column
        col++;
      }
      SoftDevices softDevices = remote.getSoftDevices();
      if ( ( softDevices == null || !softDevices.usesSequence() ) && col >= 18 )
      {
        // Skip the Sequence Number column
        col++;
      }
    }
    else if ( col > 3 )
    {
      col += 9;
    }
    return col;
  }

  private int getExtendedTypeIndex( int row )
  {
    // This extends the range of values of the device type index beyond 0x0F to use a distinctive
    // value, 0xFF, to signify an empty device slot in a remote that uses soft devices.
    short[] data = getData( row );
    if ( data == null )
    {
      return 0xFF;
    }
    Remote remote = remoteConfig.getRemote();
    DeviceButton db = getRow( row );
    SoftDevices softDevices = remote.getSoftDevices();
    if ( softDevices == null || softDevices.isSetupCodesOnly() || db.getDeviceSlot( data ) != 0xFFFF )
    {
      return db.getDeviceTypeIndex( data );
    }
    else
    {
      // if remote uses soft devices, a full setup code of 0xFFFF marks an empty
      // device slot, for which we use a special type index of 0xFF
      return 0xFF;
    }
  }

  /** The Constant colNames. */
  private static final String[] colNames =
  {
      "#", "Device Button", "Type", "<html>Setup<br>Code</html>", "<html>Setup<br>Lock</html>", 
      "<html>Volume<br>PunchThrough</html>", 
      "<html>Transport<br>PunchThrough</html>", "<html>Channel<br>PunchThrough</html>",
      "<html>X<br>PunchThrough</html>", "<html>Input<br>PunchThrough</html>",
      "<html>Z<br>PunchThrough</html>","<html>Volume<br>Lock</html>", 
      "<html>Ring<br>Color</html>", "Note", "", "", "", "Icon?", "Seq", "<html>Size &amp<br>Color</html>"
  };

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName( int col )
  {
    DeviceLabels labels = remoteConfig != null ? remoteConfig.getRemote().getDeviceLabels() : null;
    col = getEffectiveColumn( col );
    if ( col >= 14 && col <= 16 )
    {
      return labels.columnNames[ col - 14 ];
    }
    return colNames[ col ];
  }

  /** The col prototype names. */
  private static String[] colPrototypeNames =
  {
      " 00 ", "Device Button", "__VCR/DVD__", "Setup", "Setup", "PunchThrough_", "PunchThrough_", 
      "PunchThrough_", "PunchThrough_", "PunchThrough_", "PunchThrough_", "Master_", 
      "Color_", "A Meaningful, Reasonable Note", "Label", "Model", "Remote", 
      "Icon?_", "Seq", "Color_"
  };

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.JP1TableModel#getColumnPrototypeName(int)
   */
  @Override
  public String getColumnPrototypeName( int col )
  {
    col = getEffectiveColumn( col );
    if ( ( col == 14 || col == 15 || col == 16 ) && remoteConfig.getRemote().usesEZRC() )
    {
      return "Long Label___";
    }
    return colPrototypeNames[ col ];
  }

  /** The Constant colClasses. */
  private static final Class< ? >[] colClasses =
  {
      Integer.class, String.class, DeviceType.class, SetupCode.class, Boolean.class, 
      DeviceButton.class, DeviceButton.class, DeviceButton.class, DeviceButton.class, 
      DeviceButton.class, DeviceButton.class, String.class, Integer.class, String.class, String.class, String.class, 
      String.class, Integer.class, RMIcon.class, Color.class
  };

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class< ? > getColumnClass( int col )
  {
    return colClasses[ getEffectiveColumn( col ) ];
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
   */
  @Override
  public boolean isCellEditable( int row, int col )
  {
    // If remote uses soft devices, device type must be set before other columns can be edited.
    // If remote uses soft home theater, the setup code is left blank and is not editable.
    SoftDevices softDevices = remoteConfig.getRemote().getSoftDevices();
    Remote remote = remoteConfig.getRemote();
    boolean ezrc = remote.usesEZRC();
    col = getEffectiveColumn( col );
    return editable && col > 0 && ( col > 1 || ezrc ) && ( col < 2 || col > 3 || !ezrc )
        && ( col == 2 || col == 11 || getExtendedTypeIndex( row ) != 0xFF )
        && ( col != 3 || ( softDevices != null && softDevices.isSetupCodesOnly() ) || getValueAt( row, col ) != null )
        && ( col != 12 || getRow( row ).getColorIndex() > 0 );
  }
  
  private short[] getData( int row )
  {
    short[] data = null;
    if ( remoteConfig.hasSegments() )
    {
      Segment seg = getRow( row ).getSegment();
      data = seg != null ? seg.getHex().getData() : null;
    }
    else
    {
      data = remoteConfig.getData();
    }
    return data;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt( int row, int column )
  {
    Remote remote = remoteConfig.getRemote();
    DeviceButton db = getRow( row );
    int typeIndex = getExtendedTypeIndex( row );
    column = getEffectiveColumn( column );
    if ( typeIndex == 0xFF && column > 1 && ( column < 7 || remote.isSSD() ) )
    {
      return null;
    }
    short[] data = getData( row );
    int group = db.getDeviceGroup( data );
    switch ( column )
    {
      case 0:
        return new Integer( row + 1 );
      case 1:
        return db.getName();
      case 2:
      {
//        return remote.getDeviceTypeByIndex( typeIndex );
        return remote.getDeviceTypeByIndexAndGroup( typeIndex, group );
      }
      case 3:
      {
        // For remotes that use soft home theater, the HT setup code is specified in the RDF,
        // is not editable and so should be hidden.
        if ( softHT.inUse() && typeIndex == softHT.getDeviceType() )
        {
          return null;
        }
        short value = db.getSetupCode( data );
        return value < 0 ? null : new SetupCode( value );
      }
      case 4:
      {
        return db.getSetupLock( data );
      }
      case 5:
      {
        return db.getVolumePT();
      }
      case 6:
      {
        return db.getTransportPT();
      }
      case 7:
      {
        return db.getChannelPT();
      }
      case 8:
      {
        return db.getxPT();
      }
      case 9:
      {
        return db.getInputPT();
      }
      case 10:
      {
        return db.getzPT();
      }
      
      case 11:
      {
        return lockStates[ db.getVpt() ];
      }
      case 12:
      {
        return Math.abs( db.getColorIndex() );
      }
      case 13:
      {
        String[] notes = remoteConfig.getDeviceButtonNotes();
        String note = null;
        if ( notes != null )
        {
          note = notes[ row ];
        }
        if ( note == null )
        {
          DeviceUpgrade deviceUpgrade = remoteConfig.getAssignedDeviceUpgrade( db );
          if ( deviceUpgrade != null )
          {
            note = deviceUpgrade.getDescription();
          }
        }
        if ( note == null )
        {
          return "";
        }
        else
        {
          return note;
        }
      }
      case 14:
      {
        DeviceLabels labels = remote.getDeviceLabels();
        return labels.getText( data, row );
      }
      case 15:
      case 16:
      {
        DeviceLabels labels = remote.getDeviceLabels();
        return labels.getText2( data, column - 13 );
      }
      case 17:
      {
        return db.icon;
      }
      case 18:
      {
        SoftDevices softDevices = remote.getSoftDevices();
        int seq = softDevices.getSequencePosition( row, getRowCount(), data );
        if ( seq == -1 )
        {
          return null;
        }
        else
        {
          return seq + 1;
        }
      }
      case 19:
      {
        return db.getHighlight();
      }
      default:
        return null;
    }
  }
  
  
  /**
   * Returns a set of flags indicating what invalidities are present
   * in the device button codes:
   * bit 0 set = setup code is neither built-in or that of an upgrade
   * bit 1 set = setup code exceeds maximum for the remote
   * bit 2 set = XSight remote (i.e. uses EZ-RC) with setup code that has no upgrade
   */
  public int hasInvalidCodes()
  {
    int result = 0;
    Remote remote = remoteConfig.getRemote();    
    for ( int i = 0; i < getRowCount(); i++ )
    {
      DeviceButton deviceButton = getRow( i );
      DeviceType deviceType = ( DeviceType )getValueAt( i, 2 );
      SetupCode setupCode = ( SetupCode )getValueAt( i, 3 );
      if ( deviceType != null && setupCode != null )
      {
        setupCodeRenderer.setDeviceButton( deviceButton );
        setupCodeRenderer.setDeviceType( deviceType );
        if ( !setupCodeRenderer.isValid( setupCode.getValue() ) )
        {
          // isValid() always returns true if remote.getSetupValidation() is OFF
          result |= 1;
        }
        if ( setupCode.getValue() > SetupCode.getMax() )
        {
          result |= 2;
        }
        if ( remote.usesEZRC() && !deviceButton.isConstructed() && deviceButton.getUpgrade() == null )
        {
          result |= 4;
        }
      }
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
   */
  @Override
  public void setValueAt( Object value, int row, int col )
  {
    short[] data = getData( row );
    Remote remote = remoteConfig.getRemote();
    DeviceButton db = getRow( row );
    SoftDevices softDevices = remote.getSoftDevices();
    DeviceType oldDevType = null;
    SetupCode oldSetupCode = null;
    col = getEffectiveColumn( col );

    if ( col == 2 || col == 3 )
    {
      oldDevType = ( DeviceType )getValueAt( row, 2 );
      oldSetupCode = ( SetupCode )getValueAt( row, 3  );
    }
    if ( col == 1 )
    {
      db.setName( ( String )value );
    }
    else if ( col == 2 )
    {
      DeviceType newDevType = ( DeviceType )value;
      if ( ! isValidDevice( row, newDevType, oldSetupCode ) )
      {
        return;
      }

      int newIndex = newDevType.getNumber();
      int newGroup = newDevType.getGroup();
      int oldIndex = getExtendedTypeIndex( row );
      int oldGroup = db.getDeviceGroup( data );
      
      DeviceLabels labels = remote.getDeviceLabels();

      if ( ( oldIndex == newIndex ) && ( oldGroup == -1 || oldGroup == newGroup ) )
      {
        return;
      }
      
      if ( remoteConfig.isUpgradeWithKeymoves( row, oldDevType, oldSetupCode, true ) )
      {
        preserveKeyMoves( row, oldDevType, oldSetupCode );
      }
      
      if ( softHT.inUse() && newIndex == softHT.getDeviceType() )
      {
        db.zeroDeviceSlot( data );
        db.setDeviceTypeIndex( ( short )newIndex, data );
        db.setSetupCode( ( short )softHT.getDeviceCode(), data );
      }
      else
      {
        if ( oldIndex == 0xFF )
        {
          db.zeroDeviceSlot( data );
        }
        if ( remoteConfig.hasSegments() )
        {
          if ( newIndex == 0xFF )
          {
            // Only applies so far to XSight remotes
            db.setDefaultName();
          }
          // XSight remotes use 0 as a fill value if device slot not empty; other remotes always use 0xFF
          int fillValue = remote.usesEZRC() && newIndex != 0xFF ? 0 : 0xFF;
          Arrays.fill( db.getSegment().getHex().getData(), 9, 12, ( short )fillValue );
        }
        db.setDeviceTypeIndex( ( short )newIndex, data );
        db.setDeviceGroup( ( short )newGroup, data );
      }

      if ( labels != null )
      {
        if ( remoteConfig.hasSegments() )
        {
          if ( newIndex == 0xFF )
          {
            Hex hex = new Hex( db.getSegment().getHex(), 0, 12 );
            data = hex.getData();
            db.getSegment().setHex( hex );
            remote.getDeviceLabels().setText( "", 0, data );
          }
        }
        else
        {
          String name = newIndex == 0xFF ? "" : remote.getDeviceTypeByIndex( newIndex ).getName();
          labels.setText( name, row, data );
          
          if ( labels.usesDefaultLabels() )
          {
            labels.setDefaultText( name, row, data );
          }
        }
      }

      if ( softDevices != null && softDevices.usesFilledSlotCount() )
      {
        softDevices.setFilledSlotCount( getDeviceCount(), data );
      }

      if ( softDevices != null && softDevices.usesSequence() )
      {
        adjustSequenceRange();
        if ( oldIndex == 0xFF )
        {
          softDevices.setSequenceIndex( row, sequenceBox.getItemCount() - 1, data );
        }
        else if ( newIndex == 0xFF )
        {
          softDevices.deleteSequenceIndex( row, getRowCount(), data );
        }
      }
      fireTableRowsUpdated( row, row );
    }
    else if ( col == 3 )
    {
      SetupCode newSetupCode = null;
      if ( value.getClass() == String.class )
      {
        newSetupCode = new SetupCode( ( String )value, softDevices != null && softDevices.isSetupCodesOnly() );
      }
      else
      {
        newSetupCode = ( SetupCode )value;
      }
      
      if ( oldSetupCode != null )
      {
        if ( newSetupCode.getValue() == oldSetupCode.getValue() 
            || ! isValidDevice( row, oldDevType, newSetupCode ) )
        {
          return;
        }

        if ( remoteConfig.isUpgradeWithKeymoves( row, oldDevType, oldSetupCode, true ) )
        {
          preserveKeyMoves( row, oldDevType, oldSetupCode );
        }
      }
      db.setSetupCode( ( short )newSetupCode.getValue(), data );
    }
    else if ( col == 4 )
    {
      db.setSetupLock( ( Boolean )value, data );
    }
    else if ( col == 5 )
    {
      db.setVolumePT( ( DeviceButton )value );
    }
    else if ( col == 6 )
    {
      db.setTransportPT( ( DeviceButton )value );
    }
    else if ( col == 7 )
    {
      db.setChannelPT( ( DeviceButton )value );
    }
    else if ( col == 8 )
    {
      db.setxPT( ( DeviceButton )value );
    }
    else if ( col == 9 )
    {
      db.setInputPT( ( DeviceButton )value );
    }
    else if ( col == 10 )
    {
      db.setzPT( ( DeviceButton )value );
    }
    else if ( col == 11 )
    {
      int vpt = Arrays.asList( lockStates ).indexOf( ( String )value );
      db.setVpt( vpt );
      if ( vpt == 2 )
      {
        for ( DeviceButton dev : remote.getDeviceButtons() )
        {
          if ( dev != db && dev.getVpt() == 2 )
          {
            dev.setVpt( 1 );
            fireTableDataChanged();
          }
        }
      }
    }
    else if ( col == 12 )
    {
      int ndx = db.getColorIndex();
      int val = ( Integer )value;
      if ( ndx >= 0 && val > 0 )
      {
        // Color is editable
        ndx = val - 1;
        db.setColorIndex( val );
        int[] params = new int[ 3 ];
        for ( int i = 0; i < 3; i++ )
          params[ i ] = Remote.colorHex.getData()[ 3*ndx + i ];
        db.setColorParams( params );
      }
      else if ( ndx < 0 )
      {
        // Color is not editable, value is irrelevant
        db.setColorIndex( - ndx );
      }
    }
    else if ( col == 13 )
    {
      String strValue = ( ( String )value ).trim();
      if ( "".equals( strValue ) )
      {
        strValue = null;
      }

      remoteConfig.getDeviceButtonNotes()[ row ] = strValue;
    }
    else if ( col == 14 || col == 15 || col == 16 )
    {
      String text = ( String )value;
      if ( remoteConfig.hasSegments() )
      {
        Hex hex = db.getSegment().getHex();
        DeviceLabels lbls = remote.getDeviceLabels();
        String[] texts = new String[ 3 ];
        int hexLen = ( remote.isSSD() ? 15 : 14 );
        for ( int i = 0; i < 3; i++ )
        {
          String s = lbls.getText2( data, i + 1 );
          texts[ i ] = i == col - 14 ? text : s == null ? "" : s;
          hexLen += texts[ i ].length();
        }
        int lenMod = hexLen & ( remote.getForceModulus() - 1 );
        hexLen += remote.doForceEvenStarts() && lenMod != 0 ? remote.getForceModulus() - lenMod : 0;
        hex = new Hex( db.getSegment().getHex().subHex( 0, 12 ), 0, hexLen );
        data = hex.getData();
        data[ hexLen - 1 ] = ( short )0;
        for ( int i = 0; i < 3; i++ )
        {
          lbls.setText2( texts[ i ], data, i + 1 );
        }
        db.getSegment().setHex( hex );
      }
      else
      {
        remote.getDeviceLabels().setText( text, row, data );
      }
    }
    else if ( col == 17 )
    {
      db.icon = ( RMIcon )value;
    }
    else if ( col == 18 )
    {
      int rows = getRowCount();
      int newSeq = ( ( Integer )value ).intValue() - 1;
      int oldSeq = softDevices.getSequencePosition( row, rows, data );

      if ( newSeq == oldSeq )
      {
        return;
      }
      softDevices.deleteSequenceIndex( row, rows, data );
      softDevices.insertSequenceIndex( row, newSeq, rows, data );
      fireTableDataChanged();
    }
    else if ( col == 19 )
    {
      db.setHighlight( ( Color )value );
    }
    propertyChangeSupport.firePropertyChange( col == 19 ? "highlight" : "value", null, null );
  }
  
  public boolean isValidDevice( int row, DeviceType devType, SetupCode setupCode )
  {
    DeviceButton db = getRow( row );
    ShareType share = remoteConfig.shareType( db, devType, setupCode );
    
    setupCodeRenderer.setDeviceButton( db );
    setupCodeRenderer.setDeviceType( devType );
    if ( setupCode == null || !setupCodeRenderer.isValid( setupCode.getValue() ) )
    {
      // do not stop an invalid device type / setup code combination being
      // input as it may be an intermediate step in setting a valid value
      return true;
    }
    
    if ( share == ShareType.UNSHARED )
    {
      return true;
    }
    List< DeviceButton > devBtns = Arrays.asList( remoteConfig.getRemote().getDeviceButtons() );
    int index = devBtns.indexOf( db );
    DeviceButton sharedDB = devBtns.get( index - 8 );
    if ( share == ShareType.SHARED )
    {
      String message = "Device " + devType.getName() + " " + setupCode.getValue()
          + " cannot be assigned to button " + db.getName() + "\nas it is an upgrade "
          + "with key moves that would appear\nalso on device button " + sharedDB.getName() + ".";
      String title = "Device Button Assignment";
      JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
      return false;
    }
    else if ( share == ShareType.SHAREABLE )
    {
      String message = "Please be aware that device " + devType.getName() + " " + setupCode.getValue()
          + " on button " + db.getName() + "\nwill share key moves with the device on "
          + "button " + sharedDB.getName() + ", which\nwill override the functions assigned by this "
          + "device to the\nbuttons concerned.";
      String title = "Device Button Assignment";
      JOptionPane.showMessageDialog( null, message, title, JOptionPane.WARNING_MESSAGE );
      return true;
    }
    return true;
  }
  
  private void preserveKeyMoves( int devButtonIndex, DeviceType devType, SetupCode setupCode )
  {
    if ( devType == null || setupCode == null )
    {
      return;
    }
    DeviceUpgrade du = remoteConfig.findDeviceUpgrade( devType.getNumber(), setupCode.getValue() );
    if ( du == null )
    {
      return;
    }    
    for ( KeyMove keyMove : du.getKeyMoves( devButtonIndex ) )
    {
      DeviceButton db = remoteConfig.getRemote().getDeviceButtons()[ devButtonIndex ];
      keyMove.setDeviceButtonIndex( db.getButtonIndex() );
      remoteConfig.getKeyMoves().add( keyMove );
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.JP1TableModel#getColumnRenderer(int)
   */
  @Override
  public TableCellRenderer getColumnRenderer( int col )
  {
    col = getEffectiveColumn( col );
    Remote remote = remoteConfig == null ? null : remoteConfig.getRemote();
    if ( col == 0 )
    {
      return new RowNumberRenderer();
    }
    else if ( col == 1 )
    {
      return ( remote != null && remote.usesEZRC() ) ? nameRenderer : null;
    }
    else if ( col == 3 )
    {
      return setupCodeRenderer;
    }
    else if ( col == 17 )
    {
      return iconRenderer;
    }
    else if ( col == 19 || col == 12 )
    {
      return colorRenderer;
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.JP1TableModel#getColumnEditor(int)
   */
  @Override
  public TableCellEditor getColumnEditor( int col )
  {
    if ( !editable )
    {
      return null;
    }
    
    switch ( getEffectiveColumn( col ) )
    {
      case 2:
        return deviceTypeEditor;
      case 3:
        return setupCodeEditor;
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
        return punchThroughEditor;
      case 11:
        return deviceLockEditor;
      case 12:
        return spinnerEditor;
      case 13:
      case 14:
      case 15:
      case 16:
        return selectAllEditor;
      case 17:
        return iconEditor;
      case 18:
        return sequenceEditor;
      case 19:
        return colorEditor;
      default:
        return null;
    }
  }
  
  private class DeviceNameRenderer extends DefaultTableCellRenderer
  {
    DeviceType deviceType = null;
    
    @Override
    public Component getTableCellRendererComponent( JTable table, Object value, 
        boolean isSelected, boolean hasFocus,
        int row, int col )
    {
      Component c = super.getTableCellRendererComponent( table, value, isSelected, false, row, col );
      deviceType = ( DeviceType )getValueAt( row, 2 );
      DeviceButton db = getRow( row );
      c.setForeground( getTextColor( db, isSelected ) );
      return c;
    }
    
    public Color getTextColor( DeviceButton db, boolean isSelected )
    {
      if ( deviceType == null || db.isConstructed() || db.getUpgrade() != null )
      {
        return isSelected ? Color.WHITE : Color.BLACK;
      }
      else
      {
        return isSelected ? Color.YELLOW : Color.RED;
      }    
    }
  }
  
  @Override
  public String getToolTipText( int row, int col )
  {
    col = getEffectiveColumn( col );
    // For reasons unknown, the tooltip repeats if not checked in this way.
    // It seems that it may be something to do with the cell being a check box.
    int thisCell = row + col * 0x100;
    if ( thisCell == lastCell )
    {
      return null;
    }
    lastCell = thisCell;
    if ( col == 17 )
    {
      return "<html>Double click this column to open Icon Editor to set or<br>"
          + "remove a system icon from this device.</html>";
    }
    return null;
  }
  
  private int lastCell = 0;

  /** The remote config. */
  private RemoteConfiguration remoteConfig = null;

  /** The device type box. */
  private DefaultCellEditor deviceTypeEditor = null;
  private JComboBox deviceTypeBox = new JComboBox();
  
  private DefaultCellEditor punchThroughEditor = null;
  private JComboBox deviceButtonBox = new JComboBox();

  private DefaultCellEditor deviceLockEditor = null;
  private String[] lockStates = new String[]{ "Off", "On", "Master" };
  private JComboBox deviceLockBox = new JComboBox( lockStates );
  
  private SpinnerCellEditor spinnerEditor = null;
  
  /** The setup code editor */
  private SelectAllCellEditor selectAllEditor = new SelectAllCellEditor();
  private RMSetterEditor< RMIcon, IconPanel > iconEditor = null;
  
  private SetupCodeRenderer setupCodeRenderer = null;
  private SetupCodeEditor setupCodeEditor = null;
  private RMColorEditor colorEditor = null;
  private RMColorRenderer colorRenderer = new RMColorRenderer();
  private DeviceNameRenderer nameRenderer = new DeviceNameRenderer();
  private IconRenderer iconRenderer = null;

  private DefaultCellEditor sequenceEditor = null;
  private JComboBox sequenceBox = new JComboBox();

  /** The editable. */
  private boolean editable = true;

  private SoftHomeTheater softHT = new SoftHomeTheater();
}
