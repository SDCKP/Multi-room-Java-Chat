Multi-room-Java-Chat
====================

A multi-room and multi-user network-based Java chat

This is a project I made during my Java learning period in 2013

Some key functionalities:

* Server-Client architecture
* Network-based (TCP/IP)
* Custom application-level protocol (detailed on the docs)
* Multi-room system (every room is implemented as an unique instance)
* Multi-user system (every user connected is handled by an unique thread created during runtime on demand)
* User name reservation system (using passwords)
* Chat commands (detailed on the docs)
* Super-user auth and commands (detailed on the docs)
* Multi-platform (Windows/Mac/Linux)

Running
-------

This is a NetBeans project.

The project contains both the client and the server part. 

- To start the server import the project on NetBeans and run the file *src/servidor/Servidor.java*

- To start the client import the project on NetBeans and run the file *src/cliente/VentanaChat.java*

- To generate an executable .jar just change the main class when building the project

---------------------

This is a public domain project, although it is more suitable for learning purposes
