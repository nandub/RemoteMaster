package com.hifiremote.jp1;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

public class SetupCodeRenderer extends DefaultTableCellRenderer
{
  public SetupCodeRenderer( RemoteConfiguration remoteConfig )
  {
    this.remoteConfig = remoteConfig;
  }
  
  public Component getTableCellRendererComponent( JTable table, Object value, 
      boolean isSelected, boolean hasFocus,
      int row, int col )
  {
    Component c = super.getTableCellRendererComponent( table, value, isSelected, false, row, col );
    TableModel model = table.getModel();
    if ( model instanceof DeviceButtonTableModel )
    {
      DeviceButtonTableModel dbTableModel = ( DeviceButtonTableModel )model;
      deviceType = ( DeviceType )dbTableModel.getValueAt( row, 2 );
      deviceButton = dbTableModel.getRow( row );
      isRf = deviceButton.isRf();
    }
    else if ( model instanceof RFSelectorTableModel )
    {
      RFSelectorTableModel rfTableModel = ( RFSelectorTableModel )model;
      deviceType = ( DeviceType )rfTableModel.getValueAt( row, col - 1 );
      deviceButton = rfTableModel.getDevBtn();
      isRf = col > 3;
    }

    SetupCode setupCode = ( SetupCode )value;
    if ( col == 5 && setupCode.getValue() == 1877 )
    {
      int x = 0;
    }
    if ( deviceType != null && setupCode != null )
    {
      c.setForeground( getTextColor( setupCode.getValue(), isSelected ) );
    }
    return c;
  }
  
  private boolean isValidUpgrade( int setupCodeValue )
  {
    for ( DeviceUpgrade devUpgrade : remoteConfig.getDeviceUpgrades() )
    {
      if ( deviceType.getNumber() == devUpgrade.getDeviceType().getNumber()
          && setupCodeValue == devUpgrade.getSetupCode()
          && isRf == devUpgrade.isRfUpgrade()
          && ( devUpgrade.getButtonIndependent() 
              || deviceButton.getButtonIndex() == devUpgrade.getButtonRestriction().getButtonIndex() ) )
      {
        return true;
      }
    }
    return false;    
  }
  
  public boolean isValid( int setupCodeValue )
  {
    if ( setupCodeValue > SetupCode.getMax() )
    {
      return false;
    }
    Remote remote = remoteConfig.getRemote();
    if ( remote.getSetupValidation() == Remote.SetupValidation.OFF 
        || deviceButton.isConstructed() )
    {
      return true;
    }
    boolean isBuiltIn = isRf ? remote.hasRfSetupCode( deviceType, setupCodeValue ) 
        : remote.hasSetupCode( deviceType, setupCodeValue );
    return isBuiltIn || isValidUpgrade( setupCodeValue );
  }
  
  public Color getTextColor( int setupCodeValue, boolean isSelected )
  {
    if ( isValid( setupCodeValue ) )
    {
      return isSelected ? Color.WHITE : Color.BLACK;
    }
    else
    {
      return isSelected ? Color.YELLOW : Color.RED;
    }    
  }
  
  public void setDeviceButton( DeviceButton deviceButton )
  {
    this.deviceButton = deviceButton;
    isRf = deviceButton.isRf();
  }

  public void setDeviceType( DeviceType deviceType )
  {
    this.deviceType = deviceType;
  }

  private RemoteConfiguration remoteConfig = null;
  private DeviceButton deviceButton = null;
  private DeviceType deviceType = null;
  private boolean isRf = false;
  
}
