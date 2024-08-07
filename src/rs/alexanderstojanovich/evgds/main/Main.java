/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.evgds.main;

import com.bulenkov.darcula.DarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.plaf.metal.MetalLookAndFeel;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evgds.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        List<String> argList = Arrays.asList(args);

        // Init Config and Logging
        Configuration inCfg = Configuration.getInstance();
        inCfg.readConfigFile(); // this line reads if input file exists otherwise uses defaults
        IList<String> argsList = new GapList();
        argsList.addAll(Arrays.asList(args));
        final boolean logToFile = (argsList.contains("-logtofile") || inCfg.isLogToFile()); // determine debug flag (write in a log file or not)
        String arg = argsList.getIf(a -> a.equals("-" + DSLogger.DSLogLevel.ERR.name()) || a.equals("-" + DSLogger.DSLogLevel.DEBUG.name()) || a.equals("-" + DSLogger.DSLogLevel.ALL.name()));
        final DSLogger.DSLogLevel logLevel;
        if (arg == null) {
            logLevel = inCfg.getLogLevel();
        } else {
            logLevel = DSLogger.DSLogLevel.valueOf(arg.replaceFirst("-", ""));
        }
        DSLogger.init(logLevel, logToFile); // this is important initializing Apache logger
        DSLogger.INTERNAL_LOGGER.log(DSLogger.INTERNAL_LOGGER.getLevel(), "Logging level: " + logLevel);
        if (logToFile) {
            DSLogger.reportDebug("Logging to file set.", null);
        }
        // Set Look and feel for Swing GUI App
        String guiTheme = inCfg.getTheme();
        switch (guiTheme) {
            case "light":
                FlatLightLaf.setup();
                break;
            case "dark":
                FlatDarkLaf.setup();
                break;
            case "default":
            case "metal":
                FlatDarkLaf.setup(new MetalLookAndFeel());
                break;
            case "darcula":
                FlatLaf.setup(new DarculaLaf());
                break;
        }
        // initialize game creation (only creates window)
        try {
            final GameObject gameObject = new GameObject(); // throws ex
            // parse arguments
            if (argList.contains("-runonstart")) {
                gameObject.WINDOW.startServerAndUpdate();

                if (argList.contains("-genworld")) {
                    if (argList.contains("-size")) {
                        String someString = argList.get(argsList.indexOf("-size") + 1);
                        if (!someString.isEmpty()) {
                            gameObject.WINDOW.setWorldLevelSize(someString.toUpperCase());
                        }
                    }

                    if (argList.contains("-seed")) {
                        String someString2 = argList.get(argsList.indexOf("-seed") + 1);
                        if (someString2.matches("^-?\\d{1,19}$")) {
                            long seed = Long.parseLong(someString2);
                            gameObject.randomLevelGenerator.setSeed(seed);
                            gameObject.WINDOW.getSpinMapSeed().setValue(seed);
                        }
                    }

                    gameObject.WINDOW.generateWorld();
                }
            }

            // start the game loop
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    Configuration outCfg = Game.makeConfig(gameObject); // makes configuration from ingame settings
                    outCfg.writeConfigFile();  // writes configuration to the output file
                }
            });

            // Schedule timer task to monitor CPU, RAM and Network
            long[] time = {System.nanoTime(), 0L}; // lastTime, currTime in Array
            Timer timer0 = new Timer("Timer Utils");
            TimerTask task1 = new TimerTask() {
                @Override
                public void run() {
                    // Set ups to 0
                    Game.setUps(0);

                    // assign current time
                    time[1] = System.nanoTime();
                    // retrieve delta time
                    double deltaTime = (time[1] - time[0]) / 1E9d;
                    // reassign so it is calc again in next interval
                    time[0] = time[1];

//                    DSLogger.reportInfo("deltaTime" + deltaTime, null);
                    gameObject.WINDOW.checkHealthMini(deltaTime);
                }
            };
            timer0.scheduleAtFixedRate(task1, 1000L, 1000L);
        } catch (Exception ex) {
            DSLogger.reportFatalError("Unable to create game object - Application will exit!", null);
            System.exit(1);
        }
    }

}
