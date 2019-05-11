package com.hifiremote.jp1;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

public class RMColorRenderer extends DefaultTableCellRenderer
{
  @Override
  public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int col )
  {     
    component = super.getTableCellRendererComponent( table, value, false, false, row, col );
    this.isSelected = isSelected;
    boolean editable = true;
    String usage = "";
    
    TableModel model = table.getModel();
    if ( model instanceof TableSorter )
    {
      row = ( (  TableSorter )model ).modelIndex( row );
      model = ( (  TableSorter )model ).getTableModel();
    }
    if ( model instanceof JP1TableModel< ? > )
    {
      Object item = ( ( JP1TableModel< ? > )model ).getRow( row );
      int mem = 0;
      
      if ( model instanceof DeviceButtonTableModel
          && ( ( DeviceButtonTableModel )model ).getEffectiveColumn( col ) == 8 )
      {
        DeviceButton db = ( DeviceButton )item;
        int ndx = db.getColorIndex();
        if ( ndx < 0 )
        {
          // Color is not editable and its color parameters need to be read from
          // the color table
          ndx = - ndx;
          usage = "" + ndx;
          value = Remote.getColorByIndex( ndx );
        }
        else if ( ndx == 0 )
        {
          // This should not occur and sets color WHITE
          usage = "0";
          value = Remote.getColorByIndex( 1 );
        }
        else // ndx > 0
        {
          // Color parameters are given by colorParams of the device
          usage = "" + ndx;
          int[] params = db.getColorParams();
          if ( params != null )
          {
            int r = params[ 0 ] * 6;
            int g = params[ 1 ] * 6;
            int b = params[ 2 ] * 6;
            value = new Color( r, g, b );
          }
          else
          {
            value = Remote.getColorByIndex( ndx );
          }
        }
      }
      else if ( item instanceof Highlight )
      {
        mem = ( ( Highlight )item ).getMemoryUsage();
        usage = Integer.toString( mem < 0 ? ( - mem ) : mem );
      }

      if ( model instanceof DeviceUpgradeTableModel )
      {
        editable = ( ( DeviceUpgradeTableModel )model ).isCellEditable( row, col );
        if ( ( ( DeviceUpgradeTableModel )model ).getEffectiveColumn( col ) == 10 )
        {
          usage = Integer.toString( ( ( DeviceUpgrade )item ).getProtocolMemoryUsage() );
        }
      }
      else if ( model instanceof SettingsTableModel )
      {
        if ( mem < 0 )
        {
          usage += mem == -1 ? " bit" : " bits";
        }
        else
        {
          usage += mem == 1 ? " byte" : " bytes";
        }
      }
    }
    
    color = editable ? ( Color )value : Color.WHITE;
    setText( editable ? usage : "n/a" );
    setForeground( editable ? Color.BLACK : Color.GRAY );
    return component;
  }
  
  @Override
  public void paint( Graphics g )
  {
    Dimension d = component.getSize();
    if ( isSelected )
    {
      g.setColor( Color.BLACK );
      g.fillRect( 0, 0, d.width, d.height );
      g.setColor( color );
      g.fillRect( 2, 2, d.width-4, d.height-4 );
    }
    else
    {
      g.setColor( color );
      g.fillRect( 0, 0, d.width, d.height );
    }
    super.paint( g );
  }
  
  private Component component;
  private boolean isSelected;
  private Color color;

}
