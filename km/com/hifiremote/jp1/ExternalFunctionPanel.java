package com.hifiremote.jp1;

import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

public class ExternalFunctionPanel
  extends TablePanel
{
  public ExternalFunctionPanel( DeviceUpgrade devUpgrade )
  {
    super( "External Functions", devUpgrade,
           new ExternalFunctionTableModel( devUpgrade ));

    ActionListener al = new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        File file = KeyMapMaster.promptForUpgradeFile( null );
        DeviceUpgrade importedUpgrade = new DeviceUpgrade();
        try
        {
          importedUpgrade.load( file );
          FunctionImportDialog d = new FunctionImportDialog( null, importedUpgrade );
          d.show();
          if ( d.getUserAction() == JOptionPane.OK_OPTION )
          {
            Vector importedFunctions = d.getSelectedFunctions();
            if ( importedFunctions.size() > 0 )
            {
              Vector externalFunctions = deviceUpgrade.getExternalFunctions();
              int firstRow =  externalFunctions.size();
              for ( Enumeration e = importedFunctions.elements(); e.hasMoreElements(); )
              {
                Function f = ( Function )e.nextElement();
                ExternalFunction ef = new ExternalFunction();
                ef.setName( f.getName());
                Hex hex = f.getHex();
                ef.setHex( hex );
                if ( hex.length() == 1 )
                  ef.setType( ExternalFunction.EFCType );
                else
                  ef.setType( ExternalFunction.HexType );
                ef.setSetupCode( importedUpgrade.getSetupCode());
                ef.setDeviceTypeAliasName( importedUpgrade.getDeviceTypeAliasName());
                ef.setNotes( f.getNotes());
                externalFunctions.add( ef );
              }
              (( AbstractTableModel )table.getModel()).fireTableRowsInserted( firstRow, externalFunctions.size() - 1 );
            }
          }
        }
        catch ( Exception ex )
        {
          JOptionPane.showMessageDialog( null,
                                         "An error occurred loading the device upgrade from " +
                                         file.getName() + ".  Please see rmaster.err for more details.",
                                         "Device Upgrade Load Error",
                                         JOptionPane.ERROR_MESSAGE );
        }
      }
    };

    importItem = new JMenuItem( "Import" );
    importItem.setToolTipText( "Import function(s) from an existing device upgrade." );
    importItem.addActionListener( al );
    popup.add( importItem );

    importButton = new JButton( "Import" );
    importButton.addActionListener( al );
    importButton.setToolTipText( "Import function(s) from an existing device upgrade." );
    buttonPanel.add( importButton );

    initColumns();
  }

  public void update()
  {
    (( ExternalFunctionTableModel ) model ).update();
  }

  protected Object createRowObject()
  {
    return new ExternalFunction();
  }

  protected boolean canDelete( Object o )
  {
    return !(( Function ) o).assigned();
  }

  protected void doNotDelete( Object o )
  {
    String message = "Function is assigned to a button, it can not be deleted.";
    KeyMapMaster.showMessage( message );
    throw new IllegalArgumentException( message );
  }

  private JMenuItem importItem = null;
  private JButton importButton = null;
}
