package com.hifiremote.jp1.rf;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RfEditorPanel extends JPanel implements ActionListener, ChangeListener
{
  public RfEditorPanel( RfTools owner )
  {
    setLayout( new BorderLayout() );
    capturePanel = new RfCapturePanel();
    remotePanel = new RfRemotePanel( owner );
    detailsPanel = new NSDUDetailsPanel();
    addressPanel = new NSAddressPanel();
    tabbedPane = new JTabbedPane();
    add( tabbedPane, BorderLayout.CENTER );
    tabbedPane.addTab( "Pairings", remotePanel );
    tabbedPane.addTab( "Capture", capturePanel );
    tabbedPane.addTab( "NSDU Details", detailsPanel );
    tabbedPane.addTab( "NSDU Addressing", addressPanel );
    tabbedPane.addChangeListener( this );
  }
  
  public RfCapturePanel getCapturePanel()
  {
    return capturePanel;
  }

  public RfRemotePanel getRemotePanel()
  {
    return remotePanel;
  }

  public NSDUDetailsPanel getDetailsPanel()
  {
    return detailsPanel;
  }

  public NSAddressPanel getAddressPanel()
  {
    return addressPanel;
  }

  @Override
  public void actionPerformed( ActionEvent arg0 )
  {
    // TODO Auto-generated method stub

  }
  
  @Override
  public void stateChanged( ChangeEvent arg0 )
  {
    if ( tabbedPane.getSelectedComponent() == detailsPanel )
    {
      detailsPanel.setData( capturePanel.getSelectedRow() );   
    }
    else if ( tabbedPane.getSelectedComponent() == addressPanel )
    {
      addressPanel.setData( capturePanel.getSelectedRow() );
    }
  }
  
  private JTabbedPane tabbedPane = null;
  private RfCapturePanel capturePanel = null;
  private RfRemotePanel remotePanel = null;
  private NSDUDetailsPanel detailsPanel = null;
  private NSAddressPanel addressPanel = null;
}
