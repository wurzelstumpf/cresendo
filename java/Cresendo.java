//**********************************************************************
// cresendo - teC REceive SEND
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
import java.io.Reader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.String;
import java.lang.Class;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.tree.ConfigurationNode;
import com.tivoli.tec.event_delivery.TECAgent;
import com.tivoli.tec.event_delivery.EDException;
import com.ibm.logging.FileHandler;
import com.ibm.logging.MessageLogger;
import com.ibm.logging.TraceLogger;
import com.ibm.logging.LogRecord;

public class Cresendo
{
  public static String instanceName = "default";  // Cresendo instance name 
  public static String configDir = null;          // Path to configuration directory
  public static String logDir = null;             // Path to log directory
  public static String dirSep = null;             // Platform directory separator
  public static String logPath = null;            // Path to message file log
  public static String tracePath = null;          // Path to trace file log
  public static String statusPath = null;         // Path to status file log
  public static FileHandler msgHandler = null;    // Message file handler object
  public static FileHandler trcHandler = null;    // Trace file handler object
  public static MessageLogger msgLogger = null;   // Message logging object
  public static TraceLogger trcLogger = null;     // Trace logging object
  public static StatusLogger statLogger = null;   // Status logging object
  
  private static void checkConfigFile(String cfgFile)
  {
    File f = new File(cfgFile);

    if (!f.exists())
    {
      System.err.println("Error: File does not exist: " + cfgFile);
      System.exit(1);
    }

    if (!f.canRead())
    {
      System.err.println("Error: Unable to read file: " + cfgFile);
      System.exit(1);
    }
  }

  public static void main(String[] args)
  {
    String cfgFileReceiver = null;     // Path to config file for eif receiver agent
    String cfgFileEngine = null;       // Path to config file for xml event engine
    Options opts = null;               // Command line options
    HelpFormatter hf = null;           // Command line help formatter

    // Setup the message record which will contain text written to the log file
    //
    // The message logger object is created when the "-l" is processed
    // as this object need to be associated with a log file
    //
    LogRecord msg = new LogRecord(LogRecord.TYPE_INFO, "Cresendo", "main", "", "", "", "", "");

    // Get the directory separator (defaults to "/")
    //
    dirSep = System.getProperty("file.separator", "/");  

    // Initialise the structure containing the event handler objects
    //
    Vector<IEventHandler> eventHandler = new Vector<IEventHandler>(10,10);

    // Process the command line arguments
    //
    try
    {
      opts = new Options();
      hf = new HelpFormatter();
      
      opts.addOption("h", "help", false, "Command line arguments help");
      opts.addOption("i", "instance name", true, "Name of cresendo instance");
      opts.addOption("l", "log dir", true, "Path to log file directory");
      opts.addOption("c", "config dir", true, "Path to configuarion file directory");
        
      opts.getOption("l").setRequired(true);
      opts.getOption("c").setRequired(true);

      BasicParser parser = new BasicParser();
      CommandLine cl = parser.parse(opts, args);

      // Print out some help and exit
      //
      if ( cl.hasOption('h') )
      {
        hf.printHelp("Options", opts);
        System.exit(0);
      }

      // Set the instance name
      //
      if ( cl.hasOption('i') )
      {
        instanceName = cl.getOptionValue('i');   // Set to something other than "default"
      }

      // Setup the message and trace logging objects for the EventEngine
      //
      if (cl.hasOption('l'))
      {
        // Setup the the paths to the message, trace and status log files
        //
        logDir = cl.getOptionValue("l");

        logPath = logDir + dirSep + instanceName + "-engine.log" ;
        tracePath = logDir + dirSep + instanceName + "-engine.trace" ;
        statusPath = logDir + dirSep + instanceName + "-engine.status" ;
      }
      else
      {
        // NOTE:  This should be picked up by the MissingOptionException catch below
        //        but I couldn't get this to work so I added the following code:
        //
        hf.printHelp("Option 'l' is a required option", opts);
        System.exit(1);
      }

      // Read the receiver and engine config files in the config directory
      //
      if (cl.hasOption('c'))
      {
        // Setup and check path to eif config file for TECAgent receiver object
        //
        configDir = cl.getOptionValue("c");
        cfgFileReceiver = configDir + dirSep + instanceName + ".conf" ;
        checkConfigFile(cfgFileReceiver);

        // Setup and check path to xml config file for the EventEngine
        //
        cfgFileEngine = cl.getOptionValue("c") + dirSep  + instanceName + ".xml";
        checkConfigFile(cfgFileEngine);

      }
      else
      {
        // NOTE:  This should be picked up by the MissingOptionException catch below
        //        but I couldn't get this to work so I added the following code:
        //
        hf.printHelp("Option 'c' is a required option", opts);
        System.exit(1);
      }
    }
    catch (UnrecognizedOptionException e)
    {
      hf.printHelp(e.toString(), opts);
      System.exit(1);
    }
    catch (MissingOptionException e)
    {
      hf.printHelp(e.toString(), opts);
      System.exit(1);
    }
    catch (MissingArgumentException e)
    {
      hf.printHelp(e.toString(), opts);
      System.exit(1);
    }
    catch (ParseException e)
    {
      e.printStackTrace();
      System.exit(1);
    }
    catch (Exception e) {
      System.err.println(e.toString());
      System.exit(1);
    }

    // Main program
    //
    try 
    {
      // =====================================================================
      // Setup the message, trace and status logger objects
      //
      try
      {
        msgHandler = new FileHandler("cresendo", "message handler", logPath);
        msgHandler.openDevice();

        msgLogger = new MessageLogger("cresendo", "message log");
        msgLogger.addHandler(msgHandler);

        trcHandler = new FileHandler("cresendo", "trace handler", tracePath);
        trcHandler.openDevice();

        trcLogger = new TraceLogger("cresendo", "trace log");
        trcLogger.addHandler(trcHandler);

        statLogger = new StatusLogger(statusPath);
      }
      catch (Exception e)
      {
        System.err.println(e.toString());
        System.exit(1);
      }

      // Add the shutdown hook
      //
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(msgLogger, instanceName));

      // ---------------------------------------------------------------------
      // =====================================================================
      // Load and parse the xml event engine configuration file
      //
      //
      msg.setText("Loading xml engine from: '" + cfgFileEngine + "'");

      try
      {
        XMLConfiguration xmlProcessor = new XMLConfiguration();
        xmlProcessor.setFileName(cfgFileEngine);

        // Validate the xml against a document type declaration
        //
        xmlProcessor.setValidating(true);

        // Don't interpolate the tag contents by splitting them on a delimiter
        // (ie by default a comma)
        //
        xmlProcessor.setDelimiterParsingDisabled(true); 

        // This will throw a ConfigurationException if the xml document does not
        // conform to its dtd.  By doing this we hopefully catch any errors left
        // behind after the xml configuration file has been edited.
        //
        xmlProcessor.load();

        // Setup the trace flag
        //
        ConfigurationNode engine = xmlProcessor.getRootNode();
        List rootAttribute = engine.getAttributes();

        for(Iterator it = rootAttribute.iterator(); it.hasNext();)
        {
          ConfigurationNode attr = (ConfigurationNode) it.next();
            
          String attrName  = attr.getName();
          String attrValue = (String) attr.getValue();

          if (attrValue == null || attrValue == "")
          {
              System.err.println("\n  Error: The value of the attribute '" + attrName + "'" +
                                 "\n         in the xml file '" + cfgFileEngine + "'" +
                                 "\n         is not set");
              System.exit(1);
          }

          if (attrName.matches("trace"))
          {
            if (attrValue.matches("true") || attrValue.matches("on"))
            {
              trcLogger.setLogging(true);
            }
          }

          if (attrName.matches("status"))
          {
            if (attrValue.matches("true") || attrValue.matches("on"))
            {
              statLogger.setLogging(true);
            }
            else
            {
              statLogger.setLogging(false);
            }
          }

          if (attrName.matches("interval"))
          {
            if (! attrValue.matches("[0-9]+"))
            {
              System.err.println("\n  Error: The value of the interval attribute in: '" + cfgFileEngine + "'" +
                                 "\n         should only contain digits from 0 to 9." +
                                 "\n         It currently contains: '" + attrValue + "'");
              System.exit(1);
            }

            statLogger.setInterval(Integer.parseInt(attrValue));
          }
        }

        // Now build and instantiate the list of classes that will process events
        // received by the TECAgent receiver in a chain like manner.
        //
        List classes = xmlProcessor.configurationsAt("class");

        for(Iterator it = classes.iterator(); it.hasNext();)
        {
          HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();

          // sub contains now all data contained in a single <class></class> tag set
          //
          String className = sub.getString("name");

          // Log message
          //
          msg.setText(msg.getText() + "\n  Instantiated event handler class: '" + className + "'");

          // The angle brackets describing the class of object held by the
          // Vector are implemented by Java 1.5 and have 2 effects.
          //
          // 1. The list accepts only elements of that class and nothing else
          // (Of course thanks to Auto-Wrap you can also add double-values)
          //
          // 2. the get(), firstElement() ... Methods don't return a Object, but
          //    they deliver an element of the class.
          //
          Vector<Class> optTypes  = new Vector<Class>(10, 10);
          Vector<Object> optValues = new Vector<Object>(10, 10);

          for (int i = 0; i <= sub.getMaxIndex("option"); i++)
          {
            Object optValue = null;
            String optVarName = sub.getString("option(" + i + ")[@varname]");
            String optJavaType = sub.getString("option(" + i + ")[@javatype]");

            // Use the specified java type in order to make the method call
            // to the heirarchical sub object [painful :-((]
            //
            if (optJavaType.matches("byte"))
            {
              optTypes.addElement(byte.class);
              optValue = sub.getByte("option(" + i + ")");

              if (optValue == null)  // Catch nulls
              {
                optValue = 0;  // Set to something nullish
              }
            }
            else if ( optJavaType.matches("short"))
            {
              optTypes.addElement(byte.class);
              optValue = sub.getShort("option(" + i + ")");

              if (optValue == null)  // Catch nulls
              {
                optValue = 0;  // Set to something nullish
              }
            }
            else if ( optJavaType.matches("int"))
            {
              optTypes.addElement(int.class);
              optValue = sub.getInt("option(" + i + ")");

              if (optValue == null)  // Catch nulls
              {
                optValue = 0;  // Set to something nullish
              }
            }
            else if ( optJavaType.matches("long"))
            {
              optTypes.addElement(long.class);
              optValue = sub.getLong("option(" + i + ")");

              if (optValue == null)  // Catch nulls
              {
                optValue = 0;  // Set to something nullish
              }
            }
            else if ( optJavaType.matches("float"))
            {
              optTypes.addElement(float.class);
              optValue = sub.getFloat("option(" + i + ")");

              if (optValue == null)  // Catch nulls
              {
                optValue = 0.0;  // Set to something nullish
              }
            }
            else if ( optJavaType.matches("double"))
            {
              optTypes.addElement(double.class);
              optValue = sub.getDouble("option(" + i + ")");

              if (optValue == null)  // Catch nulls
              {
                optValue = 0.0;  // Set to something nullish
              }
            }
            else if ( optJavaType.matches("boolean"))
            {
              optTypes.addElement(boolean.class);
              optValue = sub.getBoolean("option(" + i + ")");

              if (optValue == null)  // Catch nulls
              {
                optValue = false;  // Set to something nullish
              }
            }
            else if ( optJavaType.matches("String"))
            {
              optTypes.addElement(String.class);
              optValue = sub.getString("option(" + i + ")");

              if (optValue == null)  // Catch nulls
              {
                optValue = "";  // Set it to something nullish
              }
            }
            else
            {
              System.err.println("Error: Unsupported java type found in xml config: '" + optJavaType + "'");
              System.exit(1);
            }

            // Add option value element
            //
            //              System.out.println("Option value is: '" + optValue.toString() + "'\n");
            //
            optValues.addElement(optValue);

            // Append to message text
            //
            String msgTemp = msg.getText();
            msgTemp += "\n      option name: '" + optVarName + "'";
            msgTemp += "\n      option type: '" + optJavaType + "'";
            msgTemp += "\n     option value: '" + optValues.lastElement().toString() + "'";
            msg.setText(msgTemp);
          }

          try
          {
            // Instantiate the class with the java reflection api
            //
            Class klass = Class.forName(className);

            // Setup an array of paramater types in order to retrieve the matching constructor
            //
            Class[] types = optTypes.toArray(new Class[optTypes.size()]);  

            // Get the constructor for the class which matches the parameter types
            //
            Constructor konstruct = klass.getConstructor(types);

            // Create an instance of the event handler
            //
            IEventHandler eventProcessor = (IEventHandler) konstruct.newInstance(optValues.toArray());

            // Add the instance to the list of event handlers
            //
            eventHandler.addElement(eventProcessor);
              
          }
          catch (InvocationTargetException e)
          {
            System.err.println("Error: " + e.toString());
            System.exit(1);
          }
          catch (ClassNotFoundException e)
          {
            System.err.println("Error: class name not found: '" + className + "' \n" + e.toString());
            System.exit(1);
          }
          catch (Exception e)
          {
            System.err.println("Error: failed to instantiate class: '" + className + "' \n" + e.toString());
            System.exit(1);
          }
        }
      }
      catch(ConfigurationException cex)    // Something went wrong loading the xml file
      {
        System.err.println("\n" + "Error loading XML file: " + cfgFileEngine + "\n" + cex.toString());
        System.exit(1);
      }
      catch (Exception e)
      {
        System.err.println(e.toString());
        System.exit(1);
      }

      // ---------------------------------------------------------------------
      // =====================================================================
      // Setup the TECAgent receiver 
      // 
      Reader cfgIn = null; 
      
      try
      {
        cfgIn = new FileReader(cfgFileReceiver);
      }
      catch (Exception e)
      {
        System.err.println(e.toString());
        System.exit(1);
      }

      // Start the TECAgent receiver and register the event engine handler
      //
      TECAgent receiver = new TECAgent(cfgIn, TECAgent.RECEIVER_MODE, false);

      EventEngine ee = new EventEngine(eventHandler, msgLogger, trcLogger);

      receiver.registerListener(ee); 

      // Construct message and send it to the message log
      //
      String text = "\n  Cresendo instance '" + instanceName +
        "' listening for events on port '" + receiver.getConfigVal("ServerPort") + "'";

      msg.setText(msg.getText() + text); 
      msgLogger.log(msg);                  // Send message to log

      // ---------------------------------------------------------------------
      // =====================================================================
      // Initiate status logging
      //
      if (statLogger.isLogging())
      {
        int seconds = statLogger.getInterval();

        while (true)
        {
          try
          {
            statLogger.log();
          }
          catch (Exception ex)
          {
            System.err.println("\n  An error occurred while writing to '" + statusPath + "'" +
                               "\n  '" + ex.toString() + "'" 
                               );
          }

          Thread.sleep(seconds * 1000);  // Convert sleep time to milliseconds
        }
      }
      
      // ---------------------------------------------------------------------
    }
    catch (Exception e)
    {
      System.err.println(e.toString());
      System.exit(1);
    }
  }
}

