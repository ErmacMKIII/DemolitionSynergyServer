# Notice About Demolition Synergy (Very Important!)

This repository ErmacMKIII/DemolitionSynergy is renewed.
Old one is taken down. Because of severe malware infection.
Please see ***Very Important Notice*** located at the bottom for more info.

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
6. OSHI library, for monitoring (installed),
7. Apache MINA, for networking (installed).

Project could be compiled and run immediately.
(No additional steps are needed.)

Project structure:
```
# Source Code Structure
└───rs
    └───alexanderstojanovich
        └───evgds
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

# Additional Notes

Tested to work with Ubuntu 14.04.6 LTS.

Install Open JDK 11 with following commands (in Terminal)
```
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update
sudo apt install openjdk-11-jdk
```
![Alt text](/misc/Screenshot2.png?raw=true "DSS Ubuntu 14.04 LTS")

# Game Server Arguments 

Configure Server in dsynergy.ini.
Make sure is configured first.
Double check that local IP (ipconfig on Windows) and server port (default 13667)
are okey.

There are four arguments:
1) Run server on start '-runonstart'
2) If above is used '-genworld' could be used to generate world after the server has started.
3) In conjuction with '-genworld' seed could be provided as numeric value.
4) In conjuction with '-genworld' size could be provided as string value. One of the following {SMALL, MEDIUM, LARGE, HUGE}
	
All in one example (default):
`-runonstart -genworld -size small -seed 305419896`

So program (server binary) would be run with (default):
`java -jar DSS.jar -runonstart -genworld -size small -seed 305419896`

Which would run the server on designated local IP and port and
generate world with supplied seed.

Done.

# Game Assets
Project may use assets from Demolition Synergy.

# Mentions
Author: Ermac(MKIII); 
Testers: 13;
Credits: Erokia

# Very Important Notice
Old repository of ErmacMKIII/DemolitionSynergy suffered severe malware Trojan infection and was unsalvagable.
Author is certain that malware was not prior date July 12, 2024.
ErmacMKIII/DemolitionSynergy as renewed repository stands since August 6, 2024.
Author apologizes for concerns and any damage.
