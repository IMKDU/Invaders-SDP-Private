package entity; // ◀ Must match the original package

// Import required classes for JUnit and AWT Color
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.awt.Color;

/**
 * Unit tests for the Ship class.
 * (Testing object creation and movement)
 */
class ShipTest {

    /**
     * [Creation Test]
     * Checks if the initial position and color are set correctly when a Ship object is created.
     */
    @Test
    void testShipCreation() {
        // --- 1. Arrange ---
        int startX = 100;
        int startY = 150;
        Color testColor = Color.CYAN; // A test color

        // --- 2. Act ---
        // Create the Ship object.
        Ship ship = new Ship(startX, startY, testColor);

        // --- 3. Assert ---
        // Verify that the values in the object match the values passed to the constructor.
        assertEquals(startX, ship.getPositionX(), "The X coordinate was not set correctly.");
        assertEquals(startY, ship.getPositionY(), "The Y coordinate was not set correctly.");
        assertEquals(testColor, ship.getColor(), "The Color was not set correctly.");
    }

    /**
     * [Movement Test - Right]
     * Checks if the X coordinate increases by the speed value when moveRight() is called.
     */
    @Test
    void testShipMovement_MoveRight() {
        // --- 1. Arrange ---
        Ship ship = new Ship(100, 100, Color.GREEN);
        int initialX = ship.getPositionX();
        int speed = ship.getSpeed(); // Speed inherited from Entity

        // --- 2. Act ---
        ship.moveRight();

        // --- 3. Assert ---
        // Verify that (initial X + speed) matches the current X.
        assertEquals(initialX + speed, ship.getPositionX(), "Rightward movement is incorrect.");
    }

    /**
     * [Movement Test - Left]
     * Checks if the X coordinate decreases by the speed value when moveLeft() is called.
     */
    @Test
    void testShipMovement_MoveLeft() {
        // --- 1. Arrange ---
        Ship ship = new Ship(100, 100, Color.GREEN);
        int initialX = ship.getPositionX();
        int speed = ship.getSpeed();

        // --- 2. Act ---
        ship.moveLeft();

        // --- 3. Assert ---
        assertEquals(initialX - speed, ship.getPositionX(), "Leftward movement is incorrect.");
    }

    /**
     * [Movement Test - Up]
     * Checks if the Y coordinate decreases by the speed value when moveUp() is called.
     */
    @Test
    void testShipMovement_MoveUp() {
        // --- 1. Arrange ---
        Ship ship = new Ship(100, 100, Color.GREEN);
        int initialY = ship.getPositionY();
        int speed = ship.getSpeed();

        // --- 2. Act ---
        ship.moveUp();

        // --- 3. Assert ---
        assertEquals(initialY - speed, ship.getPositionY(), "Upward movement is incorrect.");
    }

    /**
     * [Movement Test - Down]
     * Checks if the Y coordinate increases by the speed value when moveDown() is called.
     */
    @Test
    void testShipMovement_MoveDown() {
        // --- 1. Arrange ---
        Ship ship = new Ship(100, 100, Color.GREEN);
        int initialY = ship.getPositionY();
        int speed = ship.getSpeed();

        // --- 2. Act ---
        ship.moveDown();

        // --- 3. Assert ---
        assertEquals(initialY + speed, ship.getPositionY(), "Downward movement is incorrect.");
    }
}