# Demolition Synergy Server
Demolition Synergy Dedicated Server

Dedicated server is with Java Swing graphical user interface (GUI) 
and dark mode (darcula library).

Split into four segments.
- Network interface,
- World operations,
- General information (tabular),
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
5. Darcula library, for dark theme (installed),
6. Sigar library, for monitoring (installed)

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

# Additional notes

Tested to work with Ubuntu 14.04.6 LTS.

Install Open JDK 11 with following commands (in Terminal)
```
sudo add-apt-repository ppa:openjdk-r/ppa`
sudo apt-get update`
sudo apt install openjdk-11-jdk`
```
![Alt text](/misc/Screenshot2.png?raw=true "DSS Ubuntu 14.04 LTS")

# Game Assets
Project may use assets from Demolition Synergy.

# Mentions
Author: Ermac(MKIII); 
Testers: 13;
Credits: Erokia
