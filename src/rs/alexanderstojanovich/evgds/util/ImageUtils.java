/*
 * Copyright (C) 2021 Alexander Stojanovich <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evgds.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import rs.alexanderstojanovich.evgds.main.Game;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class ImageUtils {

    /**
     * Loads image from zip archive or external content.
     *
     * @param dirEntry directory where image is located
     * @param fileName image filename
     * @return read image
     */
    public static BufferedImage loadImage(String dirEntry, String fileName) {
        File extern = new File(dirEntry + fileName);
        File archive = new File(Game.DATA_ZIP);
        ZipFile zipFile = null;
        InputStream imgInput = null;
        if (extern.exists()) {
            try {
                imgInput = new FileInputStream(extern);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else if (archive.exists()) {
            try {
                zipFile = new ZipFile(archive);
                for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                    if (zipEntry.getName().equals(dirEntry + fileName)) {
                        imgInput = zipFile.getInputStream(zipEntry);
                        break;
                    }
                }
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + " or relevant ingame files!", null);
        }
        //----------------------------------------------------------------------
        if (imgInput == null) {
            DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
            return null;
        } else {
            try {
                return ImageIO.read(imgInput);
            } catch (IOException ex) {
                DSLogger.reportError("Error during loading image " + dirEntry + fileName + "!", null);
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }

        return null;
    }

}
