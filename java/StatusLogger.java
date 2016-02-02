//**********************************************************************
//
// Cresendo  StatusLogger
//
//   Author  Mark Matthews
//
//***********************************************************************

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

public class StatusLogger
{
  private int logInterval = 600;         // Logging interval in seconds 
  private boolean doLog = true;          // To log or not to log that is the question
  private Date startDate;                // Date and time the object was instantiated
  private String statusFile = null;      // Path to status file log
  public static long receivedCount = 0;  // Total number of events successfully received
  public static long sentFailed = 0;     // Number of events where sent failed (see EventSend)
  public static long sentFiltered = 0;   // Number of events filtered out (see EventSend)
  public static long sentSuccess = 0;    // Number of events successfully sent (see EventSend)
  public static long dropByHostName = 0;     // Number of events dropped based on hostname
  public static long mapSevByHostName = 0;   // Number of events where severity has been remapped based on host name
  public static long mapSevByHostAndSitName = 0;   // Number of events where severity has been remapped based on situation and host name
  public static long mapSevBySitName = 0;    // Number of events where severity has been remapped based on situation name

  public StatusLogger(String sfl) 
  {
    startDate = new Date();
    statusFile = sfl;
  }

  public void setInterval(int i)
  {
    logInterval = i;
  }

  public int getInterval()
  {
    return logInterval;
  }

  public void setLogging(boolean l)
  {
    doLog = l;
  }

  public boolean isLogging()
  {
    return doLog;
  }

  public void log() throws IOException, NullPointerException, IllegalArgumentException
  {
    Date nowDate = new Date();
    SimpleDateFormat sdf = null;

    sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    long upTime = ((nowDate.getTime() - startDate.getTime()) / 1000);  // Integer division by 1000 to get rid of milli-seconds

    int daySeconds = 24 * 60 * 60;
    int hourSeconds = 60 * 60;
    int minSeconds = 60;

    long upDays = upTime / daySeconds;
    long upHours = ( upTime % daySeconds ) / hourSeconds;
    long upMins = (upTime % hourSeconds) / minSeconds;

    FileWriter sfw = new FileWriter(statusFile, false);  // Don't append to file at each write

    sfw.write("\n             Start date: '" + sdf.format(startDate) + "'");
    sfw.write("\n           Current date: '" + sdf.format(nowDate) + "'");
    sfw.write("\n            Running for: '" + upDays + "' Days '" + upHours + "' Hours and '" + upMins + "' Minutes");
    sfw.write("\n        Events received: '" + receivedCount + "'");
    sfw.write("\n            Sent Failed: '" + sentFailed + "'");
    sfw.write("\n          Sent Filtered: '" + sentFiltered + "'");
    sfw.write("\n           Sent Success: '" + sentSuccess + "'");
    sfw.write("\n         DropByHostName: '" + dropByHostName + "'");
    sfw.write("\n       MapSevByHostName: '" + mapSevByHostName + "'");
    sfw.write("\n        MapSevBySitName: '" + mapSevBySitName + "'");
    sfw.write("\n MapSevByHostAndSitName: '" + mapSevByHostAndSitName + "'");
    sfw.write("\n\n");

    sfw.flush();
    sfw.close();
  }
    
}
