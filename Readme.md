# Cresendo

Cresendo is a long running daemon process, written in java, which
receives, stores and forwards EIF events:

A great deal of software still generates EIF events, such as IBM
Tivoli Monitoring, and in complex firewall environments these events
often need to collected, modified and forwarded to remote, possibly
3rd party, operations facilities.

A single Cresendo installation can support one or more Cresendo
Instances. Each instance listens on a distinct TCP/IP Port and has
it's own defined Cresendo Event Engine. The Event Engine is configured
at start-up via an XML file and consists of zero or more Event
Handlers.

Upon receiving a TEC event, the Cresendo Instance will pass the event
to it's Engine, where it will be passed from one defined Event Handler
to the next in a serial fashion. The final Event Handler is typically
configured to send the event to a receiver such as the Tivoli
Enterprise Console.

  > See the [pdf manual](doc/cresendo-1.1-manual-20080704.pdf) in the
    doc subdirectory for more details.

This repository consists of the following subdirectories:

  > ** bin ** -- contains the wrapper script that controls cresendo instances
  > ** doc ** -- contains the manual in pdf format
  > ** src ** -- contains the java source files

### TODO

  - Maven-ise the build process

