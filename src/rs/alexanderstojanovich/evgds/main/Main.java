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

import java.util.Arrays;
import javax.swing.UnsupportedLookAndFeelException;
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
        try {
            javax.swing.UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    try {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex1) {
                        DSLogger.reportError("Unable to set Nimbus look & feel!", ex);
                    }
                }
            }
            DSLogger.reportError("Unable to set Darcula look & feel!", ex);
        }
        // initialize game creation (only creates window)
        try {
            final GameObject gameObject = new GameObject(); // throws ex
            // start the game loop
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    Configuration outCfg = Game.makeConfig(gameObject); // makes configuration from ingame settings
                    outCfg.writeConfigFile();  // writes configuration to the output file
                }
            });
        } catch (Exception ex) {
            DSLogger.reportFatalError("Unable to create game object - Application will exit!", null);
        }
    }

}
