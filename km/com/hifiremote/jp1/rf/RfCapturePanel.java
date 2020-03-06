package com.hifiremote.jp1.rf;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.hifiremote.jp1.JP1Table;
import com.hifiremote.jp1.rf.Npdu.NSPrimitive;

public class RfCapturePanel extends JPanel implements ActionListener
{
  public RfCapturePanel()
  {
    setLayout( new BorderLayout() );
    data = new ArrayList< NSPrimitive >();
    NSPrimitiveTableModel nsTableModel = new NSPrimitiveTableModel();
    nsTableModel.setData( data );
    nsTable = new JP1Table( nsTableModel );
    nsTable.initColumns();
    JScrollPane scrollPane = new JScrollPane( nsTable );
    add( scrollPane, BorderLayout.CENTER );
  }

  public List< NSPrimitive > getData()
  {
    return data;
  }

  public void update()
  {
    NSPrimitiveTableModel model = ( NSPrimitiveTableModel )nsTable.getModel();
    model.fireTableDataChanged();
  }
  
  @Override
  public void actionPerformed( ActionEvent arg0 )
  {
    // TODO Auto-generated method stub

  }
  
  public NSPrimitive getSelectedRow()
  {
    int row = nsTable.getSelectedRow();
    if ( row < 0 ) return null;
    NSPrimitiveTableModel model = ( NSPrimitiveTableModel )nsTable.getModel();
    return model.getRow( row );
  }
  
  private JP1Table nsTable = null;
  private List< NSPrimitive > data = null;

}
