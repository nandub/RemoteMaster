package com.hifiremote.jp1.rf;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.hifiremote.jp1.JP1Table;

public class RfRemotePanel extends JPanel implements ActionListener
{
  public RfRemotePanel( RfTools owner )
  {
    double b = 10; // space around border/columns
    double i = 5; // space between rows
    double f = TableLayout.FILL;
    double p = TableLayout.PREFERRED;
    double size[][] =
      {
        {
          b, p, b, f, b, p, b, 100, b
        }, // cols
        {
          b, p, b
        }  // rows
      };
    
    this.owner = owner;
    setLayout( new BorderLayout() );
    TableLayout tl = new TableLayout( size );
    JPanel identPanel = new JPanel( tl );
    identPanel.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
    JLabel label = new JLabel( "RF Remote:", SwingConstants.RIGHT );
    identPanel.add( label, "1, 1" );
    RfRemote[] rfRemotes = owner.getRfRemotesList().toArray( new RfRemote[ 0 ] );
    remotesBox = new JComboBox< RfRemote >( rfRemotes );
    remotesBox.addActionListener( this );
    pairingsBox = new JComboBox< Integer >();
    identPanel.add( remotesBox, "3, 1" );
    label = new JLabel( "Pairing ref:", SwingConstants.RIGHT );
    identPanel.add( label, "5, 1" );
    identPanel.add( pairingsBox, "7, 1" );
    add( identPanel, BorderLayout.PAGE_START );
    
    RfRemoteTableModel remoteTableModel = new RfRemoteTableModel();
    remoteTable = new JP1Table( remoteTableModel );
    remoteTable.initColumns();
    JScrollPane scrollPane = new JScrollPane( remoteTable );
    add( scrollPane, BorderLayout.CENTER );
    
    if ( rfRemotes.length > 0 )
    {
      remotesBox.setSelectedIndex( 0 );
    }
  }
  
  @Override
  public void actionPerformed( ActionEvent e )
  {
    try
    {
      Object source = e.getSource();
      if ( source == remotesBox )
      {
        RfRemote rfRemote = ( RfRemote ) remotesBox.getSelectedItem();
        setData( rfRemote );
        Integer[] pairIndices = new Integer[ rfRemote.pairings.size() ];
        for ( int i = 0; i < pairIndices.length; i++ )
        {
          pairIndices[ i ] = i;
        }
        pairingsBox.setModel( new DefaultComboBoxModel< Integer >( pairIndices ) );
        if ( pairIndices.length > 0 )
        {
          pairingsBox.setSelectedIndex( 0 );
        }
        update( true );
      }
      else if ( source == pairingsBox )
      {
        update( true );
      }
    }
    catch ( Exception ex )
    {
      ex.printStackTrace( System.err );
    }

  }
  
  public void update( boolean tableOnly )
  {
    RfRemoteTableModel model = ( RfRemoteTableModel )remoteTable.getModel();
    model.setPairIndex( ( Integer )pairingsBox.getSelectedItem() );
    model.fireTableDataChanged();
    if ( !tableOnly )
    {
      RfRemote rfRemote = ( RfRemote) remotesBox.getSelectedItem();
      List< RfRemote > list = owner.getRfRemotesList();
      RfRemote[] rfRemotes = list.toArray( new RfRemote[ 0 ] );
      remotesBox.setModel( new DefaultComboBoxModel< RfRemote >( rfRemotes ) );
      if ( list.contains( rfRemote ) )
      {
        remotesBox.setSelectedItem( rfRemote );
      }
      else if ( list.size() > 0 )
      {
        remotesBox.setSelectedIndex( 0 );
      }
      else
      {
        remotesBox.setSelectedIndex( -1 );  // set no selection
      }
    }
  }
  
  private void setData( RfRemote rfRemote )
  {
    RfRemoteTableModel model = ( RfRemoteTableModel )remoteTable.getModel();
    model.setRfRemote( rfRemote );
  }
  
  RfTools owner = null;
  JComboBox< RfRemote > remotesBox = null;
  JComboBox< Integer > pairingsBox = null;
  JP1Table remoteTable = null;

}
