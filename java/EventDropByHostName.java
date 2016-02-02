//**********************************************************************
// cresendo - EventDropByHostName
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

public class EventDropByHostName implements IEventHandler
{
  private File dropDir = null;               // Drop directory
  private LogRecord msg = null;              // Message record
  private LogRecord trc = null;              // Trace record

  private void initMsgAndTrcRecord()
  {
    msg = new LogRecord(LogRecord.TYPE_WARN, "EventDropByHostName", "processEvent", "", "", "", "", "");
    trc = new LogRecord(LogRecord.TYPE_ENTRY, "EventDropByHostName", "processEvent", "", "", "", "", "");
  }

  private void stdErr(String m)
  {
    String message = "EventDropByHostName: " + m;
    System.err.println("Cresendo: " + message);
    System.exit(1);
  }

  public EventDropByHostName(String d) throws Exception
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

    // Create drop directory which will contain zero length file names
    // corresponding to the short host name of those systems for which
    // events should be dropped.
    //
    try
    {
      dropDir = new File(baseDir, "DropByHostName");

      if (! dropDir.exists())      // If the directory does not exist
      {
        if (! dropDir.mkdir())     // Then create the directory
        {
          stdErr("Unable to create drop directory: '" + dropDir.getPath() + "'");
        }
      }
    }
    catch (Exception e)
    {
      stdErr("Exception: " + e.getMessage());
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
    if (fqhost == null || fqhost.length() == 0 )
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  BAILING as value of hostname attribute is either null or string length is zero \n");
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
        trc.setText(tmpText + "\n  Dot character found in hostname  -----> '" + fqhost + "'\n");
      }

      host = fqhost.substring(0, idot);    // Strip domain name 
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  Stripped domain name from hostname  -----> '" + host + "'\n" );
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

    // Set up path to file corresponding to host name
    //
    File hostFile = new File(dropDir, host);

    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Testing if drop file exists '" + hostFile.getPath() + "'\n" );  
    }

    if (hostFile.exists())
    {
      StatusLogger.dropByHostName++;

      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  Dropping event as host drop file exists:  '" + hostFile.getPath() + "'\n");
        Cresendo.trcLogger.log(trc);
      }

      return false;  // Prevent subsequent event handlers from processing event
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
