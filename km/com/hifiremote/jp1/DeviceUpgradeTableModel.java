package com.hifiremote.jp1;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.ListIterator;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

// TODO: Auto-generated Javadoc
/**
 * The Class DeviceUpgradeTableModel.
 */
public class DeviceUpgradeTableModel extends JP1TableModel< DeviceUpgrade > implements PropertyChangeListener
{

  /**
   * Instantiates a new device upgrade table model.
   */
  public DeviceUpgradeTableModel()
  {}

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
      for ( DeviceUpgrade upgrade : remoteConfig.getDeviceUpgrades() )
      {
        upgrade.removePropertyChangeListener( this );
      }

      for ( DeviceUpgrade upgrade : remoteConfig.getDeviceUpgrades() )
      {
        upgrade.addPropertyChangeListener( this );
      }
      setData( remoteConfig.getDeviceUpgrades() );

      Remote remote = remoteConfig.getRemote();
      if ( remote.hasDeviceDependentUpgrades() > 0 )
      {
        DefaultComboBoxModel comboModel = new DefaultComboBoxModel( remote.getDeviceButtons() );
        if ( remote.hasDeviceDependentUpgrades() == 2 || remote.getDeviceButtons().length == 0 )
        {
          comboModel.insertElementAt( DeviceButton.noButton, 0 );
        }
        deviceButtonBox.setModel( comboModel );
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount()
  {
    int count = 7;

    if ( remoteConfig != null )
    {
      Remote remote = remoteConfig.getRemote();
      count += remote.hasDeviceDependentUpgrades();
      if ( remoteConfig.allowHighlighting() )
      {
        count += 2;
      }
    }
    return count;
  }

  public int getEffectiveColumn( int col )
  {
    if ( ( remoteConfig == null || remoteConfig.getRemote().hasDeviceDependentUpgrades() == 0 )
        && col > 2 )
    {
      return col + 2;
    }
    else if ( remoteConfig != null && remoteConfig.getRemote().hasDeviceDependentUpgrades() == 1  && col > 3 )
    {
      return col + 1;
    }
    return col;
  }

  /** The Constant colNames. */
  private static final String[] colNames =
  {
      "#", "<html>Device<br>Type</html>", "<html>Setup<br>Code</html>", "<html>Specific to<br>Device Button</html>",
      "<html>Available on<br>Other Buttons?</html>", "PID", "Variant", "Protocol", "Description",
      "<html>Dev.<br>Size &amp<br>Color</html>", "<html>Prot.<br>Size &amp<br>Color</html>"
  };

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName( int col )
  {
    return colNames[ getEffectiveColumn( col ) ];
  }

  /** The Constant colPrototypeNames. */
  private static final String[] colPrototypeNames =
  {
      " 00 ", "CBL/SAT__", "Setup ", "Device Button__", "Other Buttons?__", "0000__", "Variant_____",
      "Panasonic Mixed Combo___", "A relatively long description and then some more___",
      "Color_", "Color_"
  };

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.JP1TableModel#getColumnPrototypeName(int)
   */
  @Override
  public String getColumnPrototypeName( int col )
  {
    return colPrototypeNames[ getEffectiveColumn( col ) ];
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.JP1TableModel#isColumnWidthFixed(int)
   */
  @Override
  public boolean isColumnWidthFixed( int col )
  {
    col = getEffectiveColumn( col );
    if ( col < 7 || col > 8 )
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  /** The Constant colClasses. */
  private static final Class< ? >[] colClasses =
  {
      Integer.class, String.class, SetupCode.class, String.class, Boolean.class, Protocol.class, Protocol.class,
      Protocol.class, String.class, Color.class, Color.class
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
    col = getEffectiveColumn( col );
    if ( col == 10 )
    {
      return getRow( row ).needsProtocolCode();
    }
    else if ( col > 2 )
    {
      return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt( int row, int column )
  {
    DeviceUpgrade device = getRow( row );
    switch ( getEffectiveColumn( column ) )
    {
      case 0:
        return new Integer( row + 1 );
      case 1:
        return device.getDeviceTypeAliasName();
      case 2:
        int value = device.getSetupCode();
        return value < 0 ? null : new SetupCode( device.getSetupCode() );
      case 3:
        return device.getButtonRestriction().getName();
      case 4:
        return device.getButtonIndependent();
      case 5:
      case 6:
      case 7:
        // The true values for columns 5 and 6 are created by the renderer
        return device.getSetupCode() < 0 ? null : device.getProtocol();
      case 8:
        return device.getDescription();
      case 9:
        return device.getHighlight();
      case 10:
        return device.getProtocolHighlight();
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
   */
  @Override
  public void setValueAt( Object value, int row, int col )
  {
    DeviceUpgrade device = getRow( row );
    Remote remote = remoteConfig.getRemote();
    switch ( getEffectiveColumn( col ) )
    {
      case 3:
        DeviceButton devBtn = ( DeviceButton )value;
        device.setButtonRestriction( devBtn );
        if ( devBtn != DeviceButton.noButton )
        {
          devBtn.setUpgrade( device );
        }
        propertyChangeSupport.firePropertyChange( "device", null, null );
        break;
      case 4:
        device.setButtonIndependent( ( Boolean )value );
        propertyChangeSupport.firePropertyChange( "device", null, null );
        break;
      case 5:
      case 6:
      case 7:
        Protocol p = ( Protocol )value;
        device.setProtocol( p, false );       
        String proc = remote.getProcessor().getEquivalentName();
        Hex code = p.customCode.get( proc );
        if ( code != null )
        {
          // Update the custom code of any other device upgrade with same pid
          for ( DeviceUpgrade du : remoteConfig.getDeviceUpgrades() )
          {
            if ( du != device )
            {
              Protocol temp = du.getProtocol();
              if ( temp.getID( remote ).equals( p.getID( remote ) ) )
              {
                temp.customCode.put( proc, code );
              }
            }
          }
        }
        else
        {
          // Remove the custom code of any other device upgrade with same pid
          for ( DeviceUpgrade du : remoteConfig.getDeviceUpgrades() )
          {
            if ( du != device )
            {
              Protocol temp = du.getProtocol();
              if ( temp.getID( remote ).equals( p.getID( remote ) ) )
              {
                temp.customCode.remove( proc );
              }
            }
          }
        }
        if ( p instanceof ManualProtocol )
        {
          // If a manual protocol has been edited, it will have been replaced by a new manual
          // protocol with the same name, and any other uses of the old protocol also need to
          // be replaced.
          for ( DeviceUpgrade du : remoteConfig.getDeviceUpgrades() )
          {
            Protocol temp = du.getProtocol();
            if ( temp instanceof ManualProtocol && temp.getName().equals( p.getName() ) && temp != p )
            {
              du.setProtocol( p, false );
            }
          }
        }

        propertyChangeSupport.firePropertyChange( "device", null, null );
        fireTableDataChanged();
        break;
      case 8:
        device.setDescription( ( String )value );
        propertyChangeSupport.firePropertyChange( "device", null, null );
        break;
      case 9:
        device.setHighlight( ( Color )value );
        propertyChangeSupport.firePropertyChange( "highlight", null, null );
        break;
      case 10:
        device.setProtocolHighlight( ( Color )value );
        // If any other device upgrade in the device-independent section uses a protocol upgrade with
        // the same PID and code, so that it will share that code, it should be set to have the same protocol
        // highlight.
        boolean updateRequired = false;
        if ( device.getButtonIndependent() && device.needsProtocolCode() )
        {
          Hex pid = device.getProtocol().getID( remote );
          Hex pCode = device.getCode();
          for ( DeviceUpgrade du : remoteConfig.getDeviceUpgrades() )
          {
            if ( du.getButtonIndependent() && du.needsProtocolCode() &&
                du.getProtocol().getID( remote ).equals( pid ) && du.getCode().equals( pCode ) )
            {
              du.setProtocolHighlight( ( Color )value );
              updateRequired = true;
            }
          }
        }
        // Now do the same for the device-dependent section where it exists.  These are
        // distinguished by value of remote.hasDeviceDependentUpgrades().
        if ( ( device.getButtonRestriction() != DeviceButton.noButton ) && device.needsProtocolCode()
            && remote.hasDeviceDependentUpgrades() == 2 )
        {
          short[] data = remoteConfig.getData();
          int offset = device.getDependentOffset();
          int protOffset = offset + data[ offset + 1 ];
          for ( DeviceUpgrade du : remoteConfig.getDeviceUpgrades() )
          {
            if ( ( du.getButtonRestriction() != DeviceButton.noButton ) && du.needsProtocolCode() )
            {
              int duOffset = du.getDependentOffset();
              int duProtOffset = duOffset + data[ duOffset + 1 ];
              if ( duProtOffset == protOffset )
              {
                du.setProtocolHighlight( ( Color )value );
                updateRequired = true;
              }
            }
          }
        }
        if ( updateRequired )
        {
          fireTableDataChanged();
        }
        propertyChangeSupport.firePropertyChange( "highlight", null, null );
        break;
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
    if ( col == 0 )
    {
      return new RowNumberRenderer();
    }
    else if ( col == 5 )
    {
      return new DefaultTableCellRenderer()
      {
        @Override
        public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int col )
        {
          TableSorter ts = ( TableSorter )table.getModel();
          row = ts.modelIndex( row );
          DeviceUpgrade du = getRow( row );
          String starredID = du.getSetupCode() < 0 ? null : du.getStarredID();
          return super.getTableCellRendererComponent( table, starredID, isSelected, false, row, col );
        }
      };
    }
    else if ( col == 6 )
    {
      return new DefaultTableCellRenderer()
      {
        @Override
        public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int col )
        {
          TableSorter ts = ( TableSorter )table.getModel();
          row = ts.modelIndex( row );
          DeviceUpgrade du = getRow( row );
          Protocol protocol = du.getSetupCode() < 0 ? null : ( Protocol )value;
          String variant = protocol == null ? null : protocol.getVariantDisplayName( remoteConfig.getRemote().getProcessor() );
          return super.getTableCellRendererComponent( table, variant, isSelected, false, row, col );
        }
      };
    }
    else if ( col > 8 )
    {
      return colorRenderer;
    }
    return null;
  }

  @Override
  public TableCellEditor getColumnEditor( int col )
  {
    col = getEffectiveColumn( col );
    switch ( col )
    {
      case 3:
        DefaultCellEditor editor = new DefaultCellEditor( deviceButtonBox );
        editor.setClickCountToStart( RMConstants.ClickCountToStart );
        return editor;
      case 5:
      case 6:
      case 7:
        if ( remoteConfig != null )
        {
          return new ManualSettingsEditor( remoteConfig.getRemote(), col );
        }
        else
        {
          return null;
        }
      case 8:
        return descriptionEditor;
      case 9:
      case 10:
        return colorEditor;
    }
    return null;
  }

  // private JCheckBox otherAvailabilityBox = new JCheckBox();

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.JP1TableModel#removeRow(int)
   */
  @Override
  public void removeRow( int row )
  {
    DeviceUpgrade du = getRow( row );
    int devTypeIndex = du.getDeviceType().getNumber();
    int setupCode = du.getSetupCode();
    boolean hasLinkedKeyMoves = false;
    for ( KeyMove km : remoteConfig.getKeyMoves() )
    {
      if ( km.getDeviceType() == devTypeIndex && km.getSetupCode() == setupCode )
      {
        hasLinkedKeyMoves = true;
        break;
      }
    }
    if ( hasLinkedKeyMoves )
    {
      String message = "There are key moves that send signals of the device upgrade that you are\n"
          + "deleteing.  Do you also want to delete these key moves?";
      String title = "Key Moves of a Device Upgrade";
      if ( JOptionPane.showConfirmDialog( null, message, title, JOptionPane.YES_NO_OPTION, 
          JOptionPane.QUESTION_MESSAGE ) == JOptionPane.YES_OPTION )
      {
        for ( ListIterator< KeyMove > li = remoteConfig.getKeyMoves().listIterator(); li.hasNext(); )
        {
          KeyMove km = li.next();
          if ( km.getDeviceType() == devTypeIndex && km.getSetupCode() == setupCode ) 
          {
            li.remove();
          }
        }
      }
    }

    DeviceButton db = du.getButtonRestriction();
    if ( db != null && db != DeviceButton.noButton )
    {
      db.setUpgrade( null );
    }
    Remote remote = remoteConfig.getRemote();
    if ( remote.usesEZRC() )
    {
      // Delete the device that is linked to this upgrade
      remoteConfig.getDeviceButtonList().remove( db );
      db.setDefaultName();
      Hex hex = new Hex( 12 );
      short[] data = hex.getData();
      Arrays.fill( data, ( short )0xFF );
      data[ 0 ] = ( short )db.getButtonIndex();
      data[ 1 ] = ( short )0;
      if ( remote.isSSD() )
      {
        db.setSegment( null );
      }
      else
      {
        db.setSegment( new Segment( 0, 0xFF, hex  ) );
      }
    }
    checkProtocolRemoval( du, true );
    du.removePropertyChangeListener( this );
    super.removeRow( row );
  }
  
  /**
   * When a device upgrade edit changes its protocol or an upgrade is
   * deleted, check whether the old protocol should be retained and 
   * delete it if so required.  In the case of upgrade deletion, seek
   * confirmation before deleting a protocol that is no longer used
   * but in the case of an edit, always retain it.
   */
  public void checkProtocolRemoval( DeviceUpgrade du, boolean isDeletion )
  {
    Protocol p = du.getProtocol();
    Remote remote = remoteConfig.getRemote();
    if ( p != null )
    {
      boolean pidUsed = false;
      boolean pUsed = false;
      for ( DeviceUpgrade temp : remoteConfig.getDeviceUpgrades() )
      {
        // Test separately on pid and on protocol itself as it is possible for two protocols
        // to be present with same pid, eg Denon-K and Panasonic Combo.
        if ( temp.getProtocol() == null )
        {
          continue;
        }
        if ( temp != du && p == temp.getProtocol() )
        {
          pUsed = true;
          pidUsed = true;
          break;
        }
        else if ( temp != du && temp.getProtocol().getID( remote ).equals( p.getID( remote ) )  )
        {
          pidUsed = true;
        }
      }
      if ( du.needsProtocolCode() && !pidUsed )
      {
        int confirm = JOptionPane.YES_OPTION;
        if ( isDeletion )
        {
          if ( !remoteConfig.hasSegments() || remote.getSegmentTypes().contains( 0x0F ) )
          {
            String title = "Device Upgrade Deletion";
            String message = "The protocol used by the device upgrade being deleted is a protocol\n"
                + "upgrade that is not used by any other device upgrade and so will \n"
                + "normally also be deleted.  Do you wish to keep the protocol upgrade?";
            confirm = JOptionPane.showConfirmDialog( null, message, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
          }
          else
          {
            // if remote has segments and stores protocol as part of device upgrade, it should
            // always be deleted
            confirm = JOptionPane.NO_OPTION;
          }
        }
        if ( confirm == JOptionPane.CANCEL_OPTION )
        {
          return;
        }
        else if ( confirm == JOptionPane.YES_OPTION )
        {
          p.saveCode( remoteConfig, du.getCode() );
        }
        else if ( p instanceof ManualProtocol )
        {
          // If not to be kept and it is a Manual Protocol, complete its removal by deleting
          // it from ProtocolManager
          ProtocolManager.getProtocolManager().remove( p );
        }
        // In all cases other than Cancel, any custom code is no longer required
        p.customCode.clear();
      }
      if ( !pUsed )
      {
        p.removeAltPID( remote );
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.JP1TableModel#insertRow(int, java.lang.Object)
   */
  @Override
  public void insertRow( int row, DeviceUpgrade upgrade )
  {
    upgrade.addPropertyChangeListener( this );
    super.insertRow( row, upgrade );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.JP1TableModel#addRow(java.lang.Object)
   */
  @Override
  public void addRow( DeviceUpgrade upgrade )
  {
    upgrade.addPropertyChangeListener( this );
    super.addRow( upgrade );
  }
  
//  @Override
//  public void editRowProtocol( int row )
//  {
//    getRow( row ).getProtocol().editProtocol( remote, locator )
//  }
//  

  // PropertyChangeListener
  /*
   * (non-Javadoc)
   * 
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange( PropertyChangeEvent e )
  {
    // Change of PID of manual protocol may affect other rows sharing same protocol, so
    // cannot just update the source row.
    fireTableDataChanged();
  }

  /** The remote config. */
  private RemoteConfiguration remoteConfig = null;

  private SelectAllCellEditor descriptionEditor = new SelectAllCellEditor();

  private JComboBox deviceButtonBox = new JComboBox();
  
  private RMColorEditor colorEditor = null;
  private RMColorRenderer colorRenderer = new RMColorRenderer();
}
