package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.hifiremote.jp1.JP1Table;
import com.hifiremote.jp1.ProtocolDataPanel.DisplayArea;

public class RFVendorPanel extends KMPanel implements ActionListener
{
  public RFVendorPanel( DeviceUpgrade upgrade )
  {
    super( "RF Vendor Data", upgrade );
    setLayout( new BorderLayout() );
    RFVendorTableModel vendorTableModel = new RFVendorTableModel();
    vendorTable = new JP1Table( vendorTableModel );
    vendorTable.initColumns();
    JScrollPane scrollPane = new JScrollPane( vendorTable );
    add( scrollPane, BorderLayout.CENTER );
    setData( upgrade );
    String msg = "NOTE:  Vendor String and User String are not required to be text strings.  Their "
        + "length in bytes is fixed but the bytes can have any hex value.  For this reason "
        + "the only editable column in the table is the Hex Value column.";
    JPanel msgPanel = new JPanel( new BorderLayout() );
    msgPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createLineBorder( Color.GRAY ),
        BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
    DisplayArea msgArea = new DisplayArea( msg, null );
    msgPanel.add( msgArea, BorderLayout.CENTER );
    add( msgPanel, BorderLayout.PAGE_END );
  }

  public void setData( DeviceUpgrade upgrade )
  {
    RFVendorTableModel model = ( RFVendorTableModel )vendorTable.getModel();
    model.setDeviceUpgrade( upgrade );
    model.fireTableDataChanged();
  }

  @Override
  public void actionPerformed( ActionEvent arg0 )
  {
    // TODO Auto-generated method stub

  }
  
  private JP1Table vendorTable = null;
}
