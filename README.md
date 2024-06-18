# Demolition Synergy Server
Demolition Synergy Dedicated Server

Dedicated server is with Java Swing graphical user interface (GUI) 
and dark mode (darcula library).

Split into four segments.
- Network interface,
- World operations,
- General information,
- Console output (all information).

All information is provided in game time.

Project originated by removing code from Demolition Synergy client/server.
Client interface is replaced with server graphical user interface (GUI).

![Alt text](/misc/Screenshot.png?raw=true "Demolition Synergy Server")

# How To Build
Build was coded in Apache NetBeans IDE 21. Requires Java JDK 11 (or later).
In order to build the project you are gonna need NetBeans IDE 16 (or later) and following libraries:
1. JOML (installed),
2. GapList & BigList (for Block lists) (installed),
3. Apache logger (Log4J) with dependecies (installed),
4. Google Gson (installed),
5. Darcula library, for dark theme (installed)

Project could be compiled and run immediately.
(No additional steps are needed.)

Project structure:
```
# Source Code Structure
└───rs
    └───alexanderstojanovich
        └───evg
            ├───chunk
            ├───core
            ├───critter
            ├───level
            ├───light
            ├───location
            ├───main
            ├───models
            ├───net
            ├───resources
            ├───shaders
            ├───texture
            ├───util
            └───weapons			
```
Server GUI is contained in `Window` class.

# Game Assets
Project may use assets from Demolition Synergy.

# Mentions
Author: Ermac(MKIII); 
Testers: 13;
Credits: Erokia
