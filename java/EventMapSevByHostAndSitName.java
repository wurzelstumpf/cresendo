//**********************************************************************
// cresendo - EventMapSeverityByHostAndSitName
//
//  Copyright (C) 2008,2009 Mark Matthews
//  Distributed under the terms of the GNU General Public License
//
//  This file is part of Cresendo.
//  Cresendo is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//  
//  Cresendo is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with Cresendo.  If not, see <http://www.gnu.org/licenses/>.
//***********************************************************************

import java.io.File;
import com.tivoli.tec.event_delivery.TECEvent;
import com.ibm.logging.Logger;
import com.ibm.logging.LogRecord;

public class EventMapSevByHostAndSitName implements IEventHandler
{
  // Note that the itm6 informational severity appears
  // to be mapped to harmless by itm6 when a corresponding
  // event is sent to TEC
  //
  private String [] tecSev = {
                               "fatal", 
                               "critical",
                               "minor",
                               "warning",
                               "harmless",
                               "unknown"
                             };

  private File mapDir = null;               // Map directory
  private LogRecord msg = null;             // Message record
  private LogRecord trc = null;             // Trace record

  private void initMsgAndTrcRecord()
  {
    msg = new LogRecord(LogRecord.TYPE_WARN, "EventMapSevByHostAndSitName", "processEvent", "", "", "", "", "");
    trc = new LogRecord(LogRecord.TYPE_ENTRY, "EventMapSevByHostAndSitName", "processEvent", "", "", "", "", "");
  }

  private void stdErr(String m)
  {
    String message = "EventMapSevByHostAndSitName: " + m;
    System.err.println("Cresendo: " + message);
    System.exit(1);
  }

  public EventMapSevByHostAndSitName(String d) throws Exception
  {
    File baseDir = new File(d);

    initMsgAndTrcRecord();

    // Test whether the base directory exists
    //
    if (! baseDir.exists())
    {
      stdErr("Base directory does not exist: '" + baseDir.getPath() + "'");
    }

    // Test whether the base directory is actually a directory
    //
    if (! baseDir.isDirectory())
    {
      stdErr("Base directory does not appear to be a directory: '" + baseDir.getPath() + "'");
    }

    // Test that base directory is writeable
    //
    if (! baseDir.canWrite())
    {
      stdErr("Unable to write to base directory: '" + baseDir.getPath() + "'");
    }

    // Create map directory 
    //
    try
    {
      mapDir = new File(baseDir, "MapSevByHostAndSitName");

      if (! mapDir.exists())      // If the directory does not exist
      {
        if (! mapDir.mkdir())     // Then create the directory
        {
          stdErr("Unable to create directory: '" + mapDir.getPath() + "'");
        }
      }
    }
    catch (Exception e)
    {
      stdErr("Exception: " + e.getMessage());
    }

    // Create the severity directories as subdirectories of the map directory
    //
    for (int i = 0; i < tecSev.length ; i++)
    {
      try
      {
        File sevDir = new File(mapDir, tecSev[i]);

        if (! sevDir.exists())      // If the directory does not exist
        {
          if (! sevDir.mkdir())     // Then create the directory
          {
            stdErr("Unable to create directory: '" + sevDir.getPath() + "'");
          }
        }
      }
      catch (Exception e)
      {
        stdErr("Exception: " + e.getMessage());
      }
    }
  }

  public boolean processEvent(TECEvent ev)
  {
    // Clear previous text in messaage and trace records
    //
    msg.setText("");
    trc.setText("");

    if (Cresendo.trcLogger.isLogging)
    {
      trc.setText("Received event -----> \n  " + ev.toString() );
    }

    // Retrieve the value of the hostname attribute from the event
    //
    String fqhost = ev.getSlot("hostname");

    // Check hostname attribute is not null or empty
    //
    if (fqhost == null || fqhost.length() == 0)
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  BAILING as value of hostname attribute is null or string length is zero \n");
        Cresendo.trcLogger.log(trc);
      }
      return true;    // Allow event handlers to continue processing event
    }

    // Strip any quotes
    //
    fqhost = fqhost.replaceAll("[\'\"]", "");
    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Removed any single or double quotes from value of hostname attribute\n");
      Cresendo.trcLogger.log(trc);
    }

    // Check hostname attribute is not null or empty
    //
    if (fqhost == null || fqhost.length() == 0 || fqhost.matches("^\\s*$"))
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  BAILING as value of hostname attribute is empty after removal of quotes \n");
        Cresendo.trcLogger.log(trc);
      }
      return true;    // Allow event handlers to continue processing event
    }
    
    // Strip off domain name (ie everthing after and including first dot character)
    //
    String host = null;
    int idot = fqhost.indexOf('.');
    if (idot >= 0)                         // A dot was found in the host name
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  Dot character found in hostname  -----> '" + fqhost  + "'\n");
      }

      host = fqhost.substring(0, idot);    // Strip domain name 
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  Stripped domain name from hostname  -----> '" + host + "'\n");
      }
    }
    else
    {
      host = fqhost;
    }

    host = host.toLowerCase();   // Convert host name to lower case

    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Converted hostname to lower case -----> '" + host + "'\n");
    }

    // Now get the value of the situation name attribute
    //
    String sitName = ev.getSlot("situation_name");

    // Check attribute is not null or empty
    //
    if (sitName == null || sitName.length() == 0)
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  BAILING as value of situation_name attribute is null or string length is zero \n");
        Cresendo.trcLogger.log(trc);
      }
      return true;    // Allow event handlers to continue processing event
    }

    // Strip any quotes
    //
    sitName = sitName.replaceAll("[\'\"]", "");
    
    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Removed any single or double quotes from value of situation_name attribute\n");
      Cresendo.trcLogger.log(trc);
    }

    // Check attribute is not null or empty
    //
    if (sitName == null || sitName.length() == 0 || sitName.matches("^\\s*$"))
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  BAILING as value of situation_name attribute is empty after removal of quotes \n");
        Cresendo.trcLogger.log(trc);
      }
      return true;    // Allow event handlers to continue processing event
    }

    // Check to see if event severity should be remapped based on the situation
    // name and host name.  This search starts from highest to lowest severity
    // and stops as soon as the first match is found.
    //
    for (int i = 0; i < tecSev.length ; i++)
    {
      try
      {
        File sevDir = new File(mapDir, tecSev[i]);  // eg var/MapSevByHostAndSitName/fatal
        File hostDir = new File(sevDir, host);      // eg var/MapSevByHostAndSitName/fatal/serv01
        File sevMap = new File(hostDir, sitName);   // eg var/MapSevByHostAndSitName/fatal/serv01/MemLow

        if (Cresendo.trcLogger.isLogging)
        {
          String tmpText = trc.getText();
          trc.setText(tmpText + "\n  Testing if map file exists '" + sevMap.getPath() + "'\n" );  
        }

        if (sevMap.exists())      // If the map file exists
        {
          StatusLogger.mapSevByHostAndSitName++;

          if (Cresendo.trcLogger.isLogging)
          {
            String tmpText = trc.getText();
            trc.setText(tmpText + "\n  Found map file '" + sevMap.getPath() + "'\n" +
                                  "\n  Setting event severity to '" + tecSev[i].toUpperCase() + "'\n");
          }

          if (! ev.setSlot("severity", tecSev[i].toUpperCase()))     // Then set the severity of the event
          {
            if (Cresendo.trcLogger.isLogging)
            {
              String tmpText = trc.getText();
              trc.setText(tmpText + "\n  Unable to map severity of event to '" +
                          tecSev[i].toUpperCase() + "' -----> " + ev.toString());
            }
          }

          break;  // Break as soon as a map file is found
        }
      }
      catch (Exception e)
      {
        if (Cresendo.trcLogger.isLogging)
        {
          String tmpText = trc.getText();
          trc.setText(tmpText + "\n  Exception -----> \n  " + e.getMessage());
        }
      }
    }

    // Write trace file
    //
    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Passing event to next handler -----> \n  " + ev.toString());
      Cresendo.trcLogger.log(trc);
    }

    return true;    // Allow event handlers to continue processing event
  }
}
