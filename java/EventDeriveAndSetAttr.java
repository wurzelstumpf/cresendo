//**********************************************************************
//
// cresendo - EventDeriveAndSetAttr
//
//***********************************************************************

import java.util.Hashtable;
import com.ibm.logging.Logger;
import com.ibm.logging.LogRecord;
import com.tivoli.tec.event_delivery.TECEvent;

public class EventDeriveAndSetAttr implements IEventHandler
{
  private String srcAttr = null;      // Attribute value to split
  private String delimiter = null;    // String to split on
  private int index = 0;              // Index position into split string
  private String setAttr = null;      // Attribute to set
  private int setCase = 0;            // Set the case of result (0=leave as is, 1=upper, 2=lower)
  private boolean rindex = false;     // Interpret index as from the end of the string
  private LogRecord msg = null;       // Message record
  private LogRecord trc = null;       // Trace record

  private void initMsgAndTrcRecord()
  {
    msg = new LogRecord(LogRecord.TYPE_WARN, "EventDeriveAndSetAttr", "processEvent", "", "", "", "", "");
    trc = new LogRecord(LogRecord.TYPE_ENTRY, "EventDeriveAndSetAttr", "processEvent", "", "", "", "", "");
  }

  public EventDeriveAndSetAttr(String f, String d, int p, String s)
  {
    srcAttr = f;
    delimiter = d;
    index = p;
    setAttr = s;
    initMsgAndTrcRecord();
  }

  public EventDeriveAndSetAttr(String f, String d, int p, String s, int c)
  {
    srcAttr = f;
    delimiter = d;
    index = p;
    setAttr = s;
    setCase = c;
    initMsgAndTrcRecord();
  }

  public EventDeriveAndSetAttr(String f, String d, int p, String s, int c, boolean r)
  {
    srcAttr = f;
    delimiter = d;
    index = p;
    setAttr = s;
    setCase = c;
    rindex = r;
    initMsgAndTrcRecord();
  }

  public boolean processEvent(TECEvent e)
  {
    // Clear previous text in messaage and trace records
    //
    msg.setText("");
    trc.setText("");

    if (Cresendo.trcLogger.isLogging)
    {
      trc.setText("Received event as -----> \n  " + e.toString() +
                  "\n  Source attribute is: '" + srcAttr + "'\n" +
                  "\n         Delimiter is: '" + delimiter + "'\n" +
                  "\n             Index is: '" + index + "'\n" +
                  "\n    Setting attribute: '" + setAttr + "'\n" +
                  "\n          Set Case is: '" + setCase + "'\n" +
                  "\n     Reverse Index is: '" + rindex + "'\n"
                  );
    }

    String srcValue = e.getSlot(srcAttr);

    // Bail out if the source attribute doesn't have a value (ie may not exist
    // as an attribute in the event
    //
    if (srcValue == null)
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  WARNING: Bailing out because source attribute '" + srcAttr + "' has no value\n");
        Cresendo.trcLogger.log(trc);
      }

      return true;  // Allow event handlers to continue processing events
    }

    // Split the source value string using the delimiter and extract the sub-string component
    // indicated by the value of the index
    //
    String[] result = srcValue.split(delimiter);

    if (index >= result.length)
    {
      String text = "\n  WARNING: Bailing out because the index: '" + index +
        "' is greater than or equal to the number of resultant components: '" + result.length + "'\n";
      
      // Log to trace file
      //
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + text);
        Cresendo.trcLogger.log(trc);
      }

      // Log to message file
      //
      msg.setText(text); 
      Cresendo.msgLogger.log(msg);

      return true;  // Allow event handlers to continue processing events
    }

    int i = 0;    // The index into the result array

    if (rindex)   // Interpret index to work from right to left
    {
      i = result.length - 1 - index;
    }
    else         // Interpret index to work from left to right
    {
      i = index;
    }

    String setValue = result[i];

    // Protect against null, "" or " " resultant string values
    //
    if (setValue == null ||                    // Resultant value is null
        setValue.length() == 0 ||              // Resultant value is ""
        setValue.trim().length() == 0          // Resultant value is only white space
        )
    {
      String text = "\n  WARNING: Bailing out because the resultant value of the component is either null," +
        " of zero length or consists purely of white space characters \n";
      
      
      // Log to trace file
      //
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + text);
        Cresendo.trcLogger.log(trc);
      }

      // Log to message file
      //
      msg.setText(text); 
      Cresendo.msgLogger.log(msg);

      return true;  // Allow event handlers to continue processing events
    }

    switch (setCase)
    {
    case 0: break;                                        // Leave as is
    case 1: setValue = setValue.toUpperCase(); break ;    // Change to upper case
    case 2: setValue = setValue.toLowerCase(); break ;    // Change to lower case
    default: break;                                       // Should probably log this (maybe in the next version ;-)
    }

    // Get rid of any quote characters (hanging or otherwise) and re-quote to avoid evil characters
    // 
    boolean validNameAndValue = e.setSlot(setAttr, "'" + setValue.replaceAll("[\'\"]", "") + "'");

    if (validNameAndValue != true)
    {
      // This is where we can register the fact that the attribute wasn't set because
      // there were some invalid characters in the attribute's name and/or value
      //
      String text = "\n  WARNING: Attribute not set due to invalid characters in either the name or value\n" +
        "Attribute  name: '" + setAttr + "'\n" +
        "Attribute value: '" + setValue + "'\n";

      // Write trace record
      //
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + text);
      }

      // Write Message Record
      //
      msg.setText(text);
    }

    // Write log file
    //
    String msgText = msg.getText();
    if (msgText != null && msgText != "")
    {
      Cresendo.msgLogger.log(msg);
    }

    // Write trace file
    //
    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Transformed event to -----> \n  " + e.toString());
      Cresendo.trcLogger.log(trc);
    }

    return true;   // Allow event handlers to continue processing events
  }
}
