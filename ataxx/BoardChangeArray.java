package ataxx;

/**
 * An array of a board change.
 * @author Carson Trinh
 */
public class BoardChangeArray {

    /**
     * A new array representing a board change.
     * INDEX - linearized index
     * UPDATED - new color
     * OLD - old color
     * NUMJUMPS - number of jumps
     */
    public BoardChangeArray(int index, PieceColor updated, PieceColor old,
                            int numJumps) {
        _changeArray = new Object[]{index, updated, old, numJumps};
    }

    /** A copy of ARR. */
    public BoardChangeArray(BoardChangeArray arr) {
        this(arr.index(), arr.newColor(), arr.oldColor(), arr.numJumps());
    }

    /** Return the linearized index of the board change. */
    int index() {
        return (int) _changeArray[0];
    }

    /** Return the new color of the board change. */
    PieceColor newColor() {
        return (PieceColor) _changeArray[1];
    }

    /** Return the old color of the board change. */
    PieceColor oldColor() {
        return (PieceColor) _changeArray[2];
    }

    /** Return the number of jumps at the time of the board change. */
    int numJumps() {
        return (int) _changeArray[3];
    }

    /**
     * An array containing information about a board change.
     * An array containing information about a board change.
     * index 0 - linearized index
     * index 1 - new color
     * index 2 - old color
     * index 3 - number of jumps
     */
    private Object[] _changeArray;
}
