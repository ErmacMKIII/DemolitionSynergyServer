package rs.alexanderstojanovich.evgds.level;

import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import org.magicwerk.brownies.collections.Key1List;
import rs.alexanderstojanovich.evgds.chunk.Chunk;
import rs.alexanderstojanovich.evgds.models.Model;

/**
 * Item system. Contains rendered items on the ground (on blocks).
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class ItemSystem {

    /**
     * Weapons on the ground. Contains all the weapons.
     */
    public final Key1List<Model, Integer> allWeaponItems = new Key1List.Builder<Model, Integer>()
            .withKey1Duplicates(true)
            .withKey1Map(x -> Chunk.chunkFunc(x.pos))
            .build();
    /**
     * Selected (visible) weapons on the ground. Visible based on player position.
     */
    public final IList<Model> selectedWeaponItems = new GapList<>();

    /**
     * Clear both all weapon list and selected weapon list
     */
    public void clear() {
        allWeaponItems.clear();
        selectedWeaponItems.clear();
    }

    /**
     * Get list of all weapon item (on the ground)
     *
     * @return all weapon item list
     */
    public IList<Model> getAllWeaponItems() {
        return allWeaponItems;
    }

    /**
     * Get list of selected weapon items. Preprocessed.
     *
     * @return selected weapon item list.
     */
    public IList<Model> getSelectedWeaponItems() {
        return selectedWeaponItems;
    }

}
