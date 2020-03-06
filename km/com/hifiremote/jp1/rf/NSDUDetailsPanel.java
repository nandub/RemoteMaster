package com.hifiremote.jp1.rf;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.hifiremote.jp1.JP1Table;
import com.hifiremote.jp1.rf.Npdu.NSPrimitive;

public class NSDUDetailsPanel extends JPanel implements ActionListener
{
  public NSDUDetailsPanel()
  {
    setLayout( new BorderLayout() );
    data = new NSPrimitive[ 1 ];
    NSDUDetailsTableModel detailsTableModel = new NSDUDetailsTableModel();
    detailsTableModel.setData( data );
    detailsTable = new JP1Table( detailsTableModel );
    detailsTable.initColumns();
    JScrollPane scrollPane = new JScrollPane( detailsTable );
    add( scrollPane, BorderLayout.CENTER );
  }

  public void setData( NSPrimitive prim )
  {
    NSDUDetailsTableModel model = ( NSDUDetailsTableModel )detailsTable.getModel();
    model.setNSPrimitive( prim );
    model.fireTableDataChanged();
  }

  @Override
  public void actionPerformed( ActionEvent arg0 )
  {
    // TODO Auto-generated method stub

  }
  
  private JP1Table detailsTable = null;
  private NSPrimitive[] data = null;
}
