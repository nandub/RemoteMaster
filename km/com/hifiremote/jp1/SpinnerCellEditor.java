package com.hifiremote.jp1;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

// TODO: Auto-generated Javadoc
/**
 * The Class SpinnerCellEditor.
 */
public class SpinnerCellEditor
  extends AbstractCellEditor
  implements TableCellEditor, ChangeListener
{
  
  /**
   * Instantiates a new spinner cell editor.
   * 
   * @param min the min
   * @param max the max
   * @param step the step
   */
  public SpinnerCellEditor( int min, int max, int step )
  {
    spinner = new JSpinner( new SpinnerNumberModel( max, min, max, step ));
    spinner.addChangeListener( this );
    javax.swing.SwingUtilities.invokeLater( new Runnable()
    {
      public void run()
      {
        for ( Component c : spinner.getEditor().getComponents() )
        {
          if ( c instanceof JTextField )
          {
            textField = ( JTextField )c;
            break;
          }
        }
      }
    } );
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
   */
  public Component getTableCellEditorComponent( JTable table,
                                                Object value,
                                                boolean isSelected,
                                                int row,
                                                int column )
  {
    spinner.setValue( value );
    setColor( value );
    return spinner;
  }

  /* (non-Javadoc)
   * @see javax.swing.CellEditor#getCellEditorValue()
   */
  public Object getCellEditorValue()
  {
    return spinner.getValue();
  }

  public void setColorHex( Hex colorHex )
  {
    this.colorHex = colorHex;
  }
  
  @Override
  public void stateChanged( ChangeEvent e )
  {
    setColor( spinner.getValue() );
  }
  
  private void setColor( Object value )
  {
    if ( colorHex != null )
    {
      int ndx = ( Integer )value - 1;
      int r = Remote.colorHex.getData()[ 3*ndx ] * 6;
      int g = Remote.colorHex.getData()[ 3*ndx + 1 ] * 6;
      int b = Remote.colorHex.getData()[ 3*ndx + 2 ] * 6;
      Color color = new Color( r, g, b );
      if ( textField != null )
        textField.setBackground( color );
    }
  }
  
  /** The spinner. */
  private JSpinner spinner = null;
  private Hex colorHex = null;
  private JTextField textField = null;

}

