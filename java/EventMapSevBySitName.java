//**********************************************************************
//
// cresendo - EventMapSeverityByHostAndSitName
//
//***********************************************************************

import java.io.File;
import com.tivoli.tec.event_delivery.TECEvent;
import com.ibm.logging.Logger;
import com.ibm.logging.LogRecord;

public class EventMapSevBySitName implements IEventHandler
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
    msg = new LogRecord(LogRecord.TYPE_WARN, "EventMapSevBySitName", "processEvent", "", "", "", "", "");
    trc = new LogRecord(LogRecord.TYPE_ENTRY, "EventMapSevBySitName", "processEvent", "", "", "", "", "");
  }

  private void stdErr(String m)
  {
    String message = "EventMapSevBySitName: " + m;
    System.err.println("Cresendo: " + message);
    System.exit(1);
  }

  public EventMapSevBySitName(String d) throws Exception
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
      mapDir = new File(baseDir, "MapSevBySitName");

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

    // Check to see if event severity should be remapped based on the
    // situation name This search starts from highest to lowest severity
    // and stops as soon as the first match is found.
    //
    for (int i = 0; i < tecSev.length ; i++)
    {
      try
      {
        File sevDir = new File(mapDir, tecSev[i]);    // eg var/MapSevBySitName/fatal
        File sevMap = new File(sevDir, sitName);      // eg var/MapSevBySitName/fatal/DiskSpaceLow

        if (Cresendo.trcLogger.isLogging)
        {
          String tmpText = trc.getText();
          trc.setText(tmpText + "\n  Testing if map file exists '" + sevMap.getPath() + "'\n" );  
        }

        if (sevMap.exists())      // If the map file exists
        {
          StatusLogger.mapSevBySitName++;

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
