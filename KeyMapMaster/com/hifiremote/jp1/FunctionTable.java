package com.hifiremote.jp1;

import java.awt.Insets;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class FunctionTable
  extends JTable
{
  private FunctionTableModel model;

  public FunctionTable( Vector functions )
  {
    try
    {
      model = new FunctionTableModel( functions );
      setModel( model );
      getTableHeader().setReorderingAllowed( false );
//      setAutoResizeMode( AUTO_RESIZE_LAST_COLUMN );
    }
    catch ( Exception e )
    {
      System.err.println( "FunctionTable.FunctionTable() caught an exception!" );
      e.printStackTrace( System.err );
    }
  }

  public void setFunctions( Vector functions )
  {
    if ( model == null )
      model = new FunctionTableModel( functions );
    else
      model.setFunctions( functions );
  }

  public void setProtocol( Protocol protocol )
  {
    model.setProtocol( protocol );
//    TableCellRenderer r = getTableHeader().getDefaultRenderer();
    JLabel label = new JLabel();
    label.setBorder( BorderFactory.createEmptyBorder( 0, 5, 0, 5 ));
    
    TableColumnModel columnModel = getColumnModel();
    TableColumn column;
    int width;

    int cols = model.getColumnCount();
    int lastCol = cols - 1;
    for ( int i = 1; i < lastCol; i++ )
    {
      column = columnModel.getColumn( i );
//      column.setHeaderRenderer( r );
//      column.sizeWidthToFit();
      label.setText( model.getColumnName( i ));
      int w = label.getPreferredSize().width;
      column.setPreferredWidth( w );
      column.setMaxWidth( w );
      column.setWidth( w );

      TableCellEditor editor = model.getColumnEditor( i );
      if ( editor != null )
        column.setCellEditor( editor );

      TableCellRenderer renderer = model.getColumnRenderer( i );
      if ( renderer != null )
        column.setCellRenderer( renderer );
    }
    doLayout();
  }
}
