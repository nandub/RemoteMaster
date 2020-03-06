package com.hifiremote.jp1.rf;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.hifiremote.jp1.JP1Table;
import com.hifiremote.jp1.rf.Npdu.NSPrimitive;

public class NSAddressPanel extends JPanel implements ActionListener
{
  public NSAddressPanel()
  {
    setLayout( new BorderLayout() );
    NSAddressTableModel addressTableModel = new NSAddressTableModel();
    addressTable = new JP1Table( addressTableModel );
    addressTable.initColumns();
    JScrollPane scrollPane = new JScrollPane( addressTable );
    add( scrollPane, BorderLayout.CENTER );
  }

  public void setData( NSPrimitive prim )
  {
    NSAddressTableModel model = ( NSAddressTableModel )addressTable.getModel();
    model.setNSPrimitive( prim );
    model.fireTableDataChanged();
  }

  @Override
  public void actionPerformed( ActionEvent arg0 )
  {
    // TODO Auto-generated method stub

  }
  
  private JP1Table addressTable = null;
}
