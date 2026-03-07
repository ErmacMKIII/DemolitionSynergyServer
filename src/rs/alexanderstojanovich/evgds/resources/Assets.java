
/*
 * Copyright (C) 2024 Aleksandar Stojanovic <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evgds.resources;

import rs.alexanderstojanovich.evgds.models.Material;
import rs.alexanderstojanovich.evgds.models.Model;
import rs.alexanderstojanovich.evgds.texture.TextureIfc;
import rs.alexanderstojanovich.evgds.util.GlobalColors;

/**
 * Game assets.
 * All game textures and models.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Assets {

    /**
     * Array of world texture names. These are textures used for blocks and
     * world objects.
     */
    public static final String[] TEX_WORLD = {"crate", "doom0", "stone", "water", "reflc"};

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    /**
     * Model of player unarmed (Default)
     */
    public final Model ALEX_BODY_DEFAULT = new Model(Model.MODEL_NONE);

    // -------------------------------------------------------------------------
    /**
     * Model of player with one-handed small guns (W01M9)
     */
    public final Model ALEX_BODY_1H_SG_W01M9 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with one-handed small guns (W02M1)
     */
    public final Model ALEX_BODY_1H_SG_W02M1 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with one-handed small guns (W03DE)
     */
    public final Model ALEX_BODY_1H_SG_W03DE = new Model(Model.MODEL_NONE);

    /**
     * Model of player with one-handed small guns (W04UZ)
     */
    public final Model ALEX_BODY_1H_SG_W04UZ = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed big guns (W10M6)
     */
    public final Model ALEX_BODY_2H_BG_W10M6 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed big guns (W11MS)
     */
    public final Model ALEX_BODY_2H_BG_W11MS = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed big guns (W16M8)
     */
    public final Model ALEX_BODY_2H_BG_W16M8 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W05M5)
     */
    public final Model ALEX_BODY_2H_SG_W05M5 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W06P9)
     */
    public final Model ALEX_BODY_2H_SG_W06P9 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W07AK)
     */
    public final Model ALEX_BODY_2H_SG_W07AK = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W08M4)
     */
    public final Model ALEX_BODY_2H_SG_W08M4 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W09G3)
     */
    public final Model ALEX_BODY_2H_SG_W09G3 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W12W2)
     */
    public final Model ALEX_BODY_2H_SG_W12W2 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W13B9)
     */
    public final Model ALEX_BODY_2H_SG_W13B9 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W14R7)
     */
    public final Model ALEX_BODY_2H_SG_W14R7 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W15DR)
     */
    public final Model ALEX_BODY_2H_SG_W15DR = new Model(Model.MODEL_NONE);

    //--------------------------------------------------------------------------
    /**
     * Model of player unarmed (Default)
     */
    public final Model STEVE_BODY_DEFAULT = new Model(Model.MODEL_NONE);

    // -------------------------------------------------------------------------
    /**
     * Model of player with one-handed small guns (W01M9)
     */
    public final Model STEVE_BODY_1H_SG_W01M9 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with one-handed small guns (W02M1)
     */
    public final Model STEVE_BODY_1H_SG_W02M1 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with one-handed small guns (W03DE)
     */
    public final Model STEVE_BODY_1H_SG_W03DE = new Model(Model.MODEL_NONE);

    /**
     * Model of player with one-handed small guns (W04UZ)
     */
    public final Model STEVE_BODY_1H_SG_W04UZ = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed big guns (W10M6)
     */
    public final Model STEVE_BODY_2H_BG_W10M6 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed big guns (W11MS)
     */
    public final Model STEVE_BODY_2H_BG_W11MS = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed big guns (W16M8)
     */
    public final Model STEVE_BODY_2H_BG_W16M8 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W05M5)
     */
    public final Model STEVE_BODY_2H_SG_W05M5 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W06P9)
     */
    public final Model STEVE_BODY_2H_SG_W06P9 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W07AK)
     */
    public final Model STEVE_BODY_2H_SG_W07AK = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W08M4)
     */
    public final Model STEVE_BODY_2H_SG_W08M4 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W09G3)
     */
    public final Model STEVE_BODY_2H_SG_W09G3 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W12W2)
     */
    public final Model STEVE_BODY_2H_SG_W12W2 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W13B9)
     */
    public final Model STEVE_BODY_2H_SG_W13B9 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W14R7)
     */
    public final Model STEVE_BODY_2H_SG_W14R7 = new Model(Model.MODEL_NONE);

    /**
     * Model of player with two-handed small guns (W15DR)
     */
    public final Model STEVE_BODY_2H_SG_W15DR = new Model(Model.MODEL_NONE);

    /**
     * Position of the weapon in the game world
     */
//    public static final Vector3f WEAPON_POS = new Vector3f(1.0f, -1.0f, 3.0f);
    // WeaponConstants for weapon texture names
    public static final String W01M9 = "W01M9";
    public static final String W02M1 = "W02M1";
    public static final String W03DE = "W03DE";
    public static final String W04UZ = "W04UZ";
    public static final String W05M5 = "W05M5";
    public static final String W06P9 = "W06P9";
    public static final String W07AK = "W07AK";
    public static final String W08M4 = "W08M4";
    public static final String W09G3 = "W09G3";
    public static final String W10M6 = "W10M6";
    public static final String W11MS = "W11MS";
    public static final String W12W2 = "W12W2";
    public static final String W13B9 = "W13B9";
    public static final String W14R7 = "W14R7";
    public static final String W15DR = "W15DR";
    public static final String W16M8 = "W16M8";

    public final Model M9_PISTOL = new Model(Model.MODEL_NONE);
    public final Model M1911_PISTOL = new Model(Model.MODEL_NONE);
    public final Model DESERT_EAGLE = new Model(Model.MODEL_NONE);
    public final Model MINI_UZI_SMG = new Model(Model.MODEL_NONE);
    public final Model MP5_SMG = new Model(Model.MODEL_NONE);
    public final Model P90_SMG = new Model(Model.MODEL_NONE);
    public final Model AK47_RIFLE = new Model(Model.MODEL_NONE);
    public final Model M4A1_RIFLE = new Model(Model.MODEL_NONE);
    public final Model G36_RIFLE = new Model(Model.MODEL_NONE);
    public final Model M60_MG = new Model(Model.MODEL_NONE);
    public final Model SAW_MG = new Model(Model.MODEL_NONE);
    public final Model WINCHESTER_1200_SHOTGUN = new Model(Model.MODEL_NONE);
    public final Model BENELLI_SUPER_90_SHOTGUN = new Model(Model.MODEL_NONE);
    public final Model REMINGTON_700_SNIPER = new Model(Model.MODEL_NONE);
    public final Model DRAGUNOV_SNIPER = new Model(Model.MODEL_NONE);
    public final Model M82_SNIPER = new Model(Model.MODEL_NONE);

    /**
     * * Array of weapon texture names
     */
    public static final String[] TEX_WEAPONS = {
            W01M9,
            W02M1,
            W03DE,
            W04UZ,
            W05M5,
            W06P9,
            W07AK,
            W08M4,
            W09G3,
            W10M6,
            W11MS,
            W12W2,
            W13B9,
            W14R7,
            W15DR,
            W16M8
    };

    /**
     * Array of player texture names. Armed with weapon or unarmed without
     * weapon (default).
     */
    public static final String[] TEX_PLAYER = {
            "alex",
            "steve",
            W01M9,
            W02M1,
            W03DE,
            W04UZ,
            W05M5,
            W06P9,
            W07AK,
            W08M4,
            W09G3,
            W10M6,
            W11MS,
            W12W2,
            W13B9,
            W14R7,
            W15DR,
            W16M8
    };

    /**
     * Array of all ingame textures. These are textures used for blocks, world
     * objects, player and weapons.
     */
    public Assets() {
        // Set default material for all models. This will be overridden by the level's material when the level is loaded, but it ensures that all models have a valid material.
        Material material = new Material(TextureIfc.getOrDefault(Model.MODEL_NONE.texName));

        // Add default material to all models. This is necessary because the level's material will override it, but we need to ensure that all models have a valid material before the level is loaded.
        ALEX_BODY_DEFAULT.materials.add(material);
        ALEX_BODY_1H_SG_W01M9.materials.add(material);
        ALEX_BODY_1H_SG_W02M1.materials.add(material);
        ALEX_BODY_1H_SG_W03DE.materials.add(material);
        ALEX_BODY_1H_SG_W04UZ.materials.add(material);
        ALEX_BODY_2H_BG_W10M6.materials.add(material);
        ALEX_BODY_2H_BG_W11MS.materials.add(material);
        ALEX_BODY_2H_BG_W16M8.materials.add(material);
        ALEX_BODY_2H_SG_W05M5.materials.add(material);
        ALEX_BODY_2H_SG_W06P9.materials.add(material);
        ALEX_BODY_2H_SG_W07AK.materials.add(material);
        ALEX_BODY_2H_SG_W08M4.materials.add(material);
        ALEX_BODY_2H_SG_W09G3.materials.add(material);
        ALEX_BODY_2H_SG_W12W2.materials.add(material);
        ALEX_BODY_2H_SG_W13B9.materials.add(material);
        ALEX_BODY_2H_SG_W14R7.materials.add(material);
        ALEX_BODY_2H_SG_W15DR.materials.add(material);

        STEVE_BODY_DEFAULT.materials.add(material);
        STEVE_BODY_1H_SG_W01M9.materials.add(material);
        STEVE_BODY_1H_SG_W02M1.materials.add(material);
        STEVE_BODY_1H_SG_W03DE.materials.add(material);
        STEVE_BODY_1H_SG_W04UZ.materials.add(material);
        STEVE_BODY_2H_BG_W10M6.materials.add(material);
        STEVE_BODY_2H_BG_W11MS.materials.add(material);
        STEVE_BODY_2H_BG_W16M8.materials.add(material);
        STEVE_BODY_2H_SG_W05M5.materials.add(material);
        STEVE_BODY_2H_SG_W06P9.materials.add(material);
        STEVE_BODY_2H_SG_W07AK.materials.add(material);
        STEVE_BODY_2H_SG_W08M4.materials.add(material);
        STEVE_BODY_2H_SG_W09G3.materials.add(material);
        STEVE_BODY_2H_SG_W12W2.materials.add(material);
        STEVE_BODY_2H_SG_W13B9.materials.add(material);
        STEVE_BODY_2H_SG_W14R7.materials.add(material);
        STEVE_BODY_2H_SG_W15DR.materials.add(material);

        M9_PISTOL.texName = W01M9;
        M1911_PISTOL.texName = W02M1;
        DESERT_EAGLE.texName = W03DE;
        MINI_UZI_SMG.texName = W04UZ;
        MP5_SMG.texName = W05M5;
        P90_SMG.texName = W06P9;
        AK47_RIFLE.texName = W07AK;
        M4A1_RIFLE.texName = W08M4;
        G36_RIFLE.texName = W09G3;
        M60_MG.texName = W10M6;
        SAW_MG.texName = W11MS;
        WINCHESTER_1200_SHOTGUN.texName = W12W2;
        BENELLI_SUPER_90_SHOTGUN.texName = W13B9;
        REMINGTON_700_SNIPER.texName = W14R7;
        DRAGUNOV_SNIPER.texName = W15DR;
        M82_SNIPER.texName = W16M8;
        M9_PISTOL.materials.add(material);
        M1911_PISTOL.materials.add(material);
        DESERT_EAGLE.materials.add(material);
        MINI_UZI_SMG.materials.add(material);
        MP5_SMG.materials.add(material);
        P90_SMG.materials.add(material);
        AK47_RIFLE.materials.add(material);
        M4A1_RIFLE.materials.add(material);
        G36_RIFLE.materials.add(material);
        M60_MG.materials.add(material);
        SAW_MG.materials.add(material);
        WINCHESTER_1200_SHOTGUN.materials.add(material);
        BENELLI_SUPER_90_SHOTGUN.materials.add(material);
        REMINGTON_700_SNIPER.materials.add(material);
        DRAGUNOV_SNIPER.materials.add(material);
        M82_SNIPER.materials.add(material);
    }

}
