package engine;

import java.util.Arrays;
import java.util.List;

/**
 * Manages the state and navigation of a menu.
 */
public class MenuManager {

    private final List<Integer> menuItems;
    private int currentIndex;

    /**
     * Constructor for MenuManager.
     * @param initialSelection The initial selected menu item's code.
     * @param items The ordered list of menu item codes.
     */
    public MenuManager(int initialSelection, Integer... items) {
        this.menuItems = Arrays.asList(items);
        this.currentIndex = this.menuItems.indexOf(initialSelection);
        // If initialSelection is not found, default to the first item.
        if (this.currentIndex == -1) {
            this.currentIndex = 0;
        }
    }

    /**
     * Moves the selection to the next item in the menu, wrapping around if necessary.
     */
    public void next() {
        this.currentIndex = (this.currentIndex + 1) % this.menuItems.size();
    }

    /**
     * Moves the selection to the previous item in the menu, wrapping around if necessary.
     */
    public void previous() {
        this.currentIndex = (this.currentIndex - 1 + this.menuItems.size()) % this.menuItems.size();
    }

    /**
     * Gets the code of the currently selected menu item.
     * @return The integer code for the current menu selection.
     */
    public int getCurrentSelection() {
        return this.menuItems.get(this.currentIndex);
    }

    /**
     * Sets the current selection directly by its code.
     * @param itemCode The code of the item to select.
     */
    public void setCurrentSelection(int itemCode) {
        int index = this.menuItems.indexOf(itemCode);
        if (index != -1) {
            this.currentIndex = index;
        }
    }
}
