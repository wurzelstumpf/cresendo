//**********************************************************************
//
// cresendo - EventSetAttr
//
//***********************************************************************

import com.ibm.logging.Logger;
import com.ibm.logging.LogRecord;
import com.tivoli.tec.event_delivery.TECEvent;

public class EventSetAttr implements IEventHandler
{
  private boolean overWrite = true;   // Overwrite and existing attribute's value?
  private String setAttr = null;      // Name of attribute to set
  private String setValue = null;     // Value of attribute to set
  private LogRecord msg = null;       // Message record
  private LogRecord trc = null;       // Trace record

  private void initMsgAndTrcRecord()
  {
    msg = new LogRecord(LogRecord.TYPE_WARN, "EventSetAttr", "processEvent", "", "", "", "", "");
    trc = new LogRecord(LogRecord.TYPE_ENTRY, "EventSetAttr", "processEvent", "", "", "", "", "");
  }

  public EventSetAttr(String name, String data)
  {
    setAttr = name;
    setValue= data;
    initMsgAndTrcRecord();
  }

  public EventSetAttr(String name, String data, boolean ow)
  {
    setAttr = name;
    setValue= data;
    overWrite = ow;
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
                  "\n  Intend to set attribute '" + setAttr +
                  "' with value '" + setValue + "'\n");
    }

    // If we are not supposed to overwrite slots with existing values
    // then we should try and be careful
    //
    if (!overWrite)
    {
      String attrValue = null;

      attrValue = e.getSlot(setAttr);

      // Bail out if the slot exists and has a value other than null
      //
      if (attrValue != null)
      {
        if (Cresendo.trcLogger.isLogging)
        {
          String tmpText = trc.getText();
          trc.setText(tmpText + "\n  Not overwriting attribute '" + setAttr +
                      "' as it already has an  existing value '" + attrValue + "'\n");

          // Write to trace log as next statement is to exit method
          //
          Cresendo.trcLogger.log(trc);
        }

        return true;  // Allow event handlers to continue processing event
      }
    }

    // Protect against null, "" or " " string values
    //
    if (setValue == null ||                    // Value is null
        setValue.length() == 0 ||              // Value is ""
        setValue.trim().length() == 0          // Value is only white space
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

    // Get rid of any quote characters (hanging or otherwise) and re-quote to avoid evil characters
    // 
    boolean validNameAndValue = e.setSlot(setAttr, "'" + setValue.replaceAll("[\'\"]", "") + "'");

    if (validNameAndValue != true)
    {
      // This is where we can register the fact that the attribute wasn't set because
      // there were some invalid characters in the attribute's name and/or value
      //
      String text = "\n  WARNING: Attribute not set due to invalid characters in either the name or value\n" +
        "  Attribute  name: '" + setAttr + "'\n" +
        "  Attribute value: '" + setValue + "'\n";

      // Write trace record
      //
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + text);
      }

      // Write log record
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

    return true;  // Allow next event handler to process event
  }
}
