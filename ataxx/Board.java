package ataxx;

/* Author: P. N. Hilfinger, (C) 2008. */

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Formatter;
import java.util.Observable;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** An Ataxx board.   The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Carson Trinh
 */
class Board extends Observable {

    /** Number of squares on a side of the board. */
    static final int SIDE = 7;

    /** Length of a side + an artificial 2-deep border region. */
    static final int EXTENDED_SIDE = SIDE + 4;

    /** Number of non-extending moves before game ends. */
    static final int JUMP_LIMIT = 25;

    /** A new, cleared board at the start of the game. */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        _changeStack = new Stack<BoardChangeArray>();
        _changeStack.push(null);
        _moveStack = new Stack<Move>();
        _numRedPieces = 0;
        _numBluePieces = 0;
        _numMoves = 0;
        _numJumps = 0;

        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        _board = b._board.clone();
        _changeStack = new Stack<BoardChangeArray>();
        for (int i = 0; i < b._changeStack.size(); i++) {
            BoardChangeArray copy;
            if (b._changeStack.get(i) != null) {
                copy = new BoardChangeArray(b._changeStack.get(i));
            } else {
                copy = null;
            }
            _changeStack.push(copy);
        }
        _moveStack = (Stack<Move>) b._moveStack.clone();
        _whoseMove = b.whoseMove();
        _numRedPieces = b.numPieces(RED);
        _numBluePieces = b.numPieces(BLUE);
        _numMoves = b.numMoves();
        _numJumps = b.numJumps();
    }

    /** Return the linearized index of square COL ROW. */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /** Return the linearized index of the square that is DC columns and DR
     *  rows away from the square with index SQ. */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions and no blocks. */
    void clear() {
        _whoseMove = RED;
        for (int i = 0; i < _board.length; i++) {
            _board[i] = BLOCKED;
        }

        char[] colBoard = {'a', 'b', 'c', 'd', 'e', 'f', 'g'};
        char[] rowBoard = {'1', '2', '3', '4', '5', '6', '7'};
        for (int i = 0; i < colBoard.length; i++) {
            for (int j = 0; j < rowBoard.length; j++) {
                unrecordedSet(colBoard[i], rowBoard[j], EMPTY);
            }
        }

        unrecordedSet('a', '7', RED);
        unrecordedSet('g', '1', RED);
        unrecordedSet('a', '1', BLUE);
        unrecordedSet('g', '7', BLUE);

        _numRedPieces = 2;
        _numBluePieces = 2;

        setChanged();
        notifyObservers();
    }

    /** Clears part of the board before a game start. */
    void clearStart() {
        _numJumps = 0;
        _numMoves = 0;
    }

    /** Return true iff the game is over: i.e., if neither side has
     *  any moves, if one side has no pieces, or if there have been
     *  MAX_JUMPS consecutive jumps without intervening extends. */
    boolean gameOver() {
        return _numJumps == JUMP_LIMIT || _numBluePieces == 0
                || _numRedPieces == 0 || !(canMove(RED) || canMove(BLUE));
    }

    /** Return number of red pieces on the board. */
    int redPieces() {
        return numPieces(RED);
    }

    /** Return number of blue pieces on the board. */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /** Return number of COLOR pieces on the board. */
    int numPieces(PieceColor color) {
        if (color == RED) {
            return _numRedPieces;
        } else if (color == BLUE) {
            return _numBluePieces;
        } else {
            return 0;
        }
    }

    /** Increment numPieces(COLOR) by K. */
    private void incrPieces(PieceColor color, int k) {
        if (color == RED) {
            _numRedPieces += k;
        } else if (color == BLUE) {
            _numBluePieces += k;
        }
    }

    /** The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     *  '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     *  BLOCKED.  Returns the same value as get(index(C, R)). */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /** Return the current contents of square with linearized index SQ. */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'g', and
     *  '1' <= R <= '7'. */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /** Set square with linearized index SQ to V.  This operation is
     *  undoable. */
    private void set(int sq, PieceColor v) {
        addUndo(sq, v, _numJumps);
        _board[sq] = v;
    }

    /** Set square at C R to V (not undoable). */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /** Set square at linearized index SQ to V (not undoable). */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Return true iff MOVE is legal on the current board. */
    boolean legalMove(Move move) {
        if (move.isPass()) {
            return true;
        }
        return get(move.col1(), move.row1()) == EMPTY
                && get(move.col0(), move.row0()) == _whoseMove
                && (move.isExtend() || move.isJump());
    }

    /** Return true iff player WHO can move, ignoring whether it is
     *  that player's move and whether the game is over. */
    boolean canMove(PieceColor who) {
        if (numPieces(who) == 0) {
            return false;
        }
        for (int i = 0; i < _board.length; i++) {
            if (get(i) == EMPTY) {
                for (int col = -2; col < 3; col++) {
                    for (int row = -2; row < 3; row++) {
                        int target = neighbor(i, col, row);
                        if (get(target) == who) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Return total number of moves and passes since the last
     *  clear or the creation of the board. */
    int numMoves() {
        return _numMoves;
    }

    /** Return number of non-pass moves made in the current game since the
     *  last extend move added a piece to the board (or since the
     *  start of the game). Used to detect end-of-game. */
    int numJumps() {
        return _numJumps;
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /** Make the MOVE on this Board, assuming it is legal. */
    void makeMove(Move move) {
        if (!legalMove(move)) {
            throw error("Illegal move.");
        }
        if (move.isPass()) {
            pass();
            startUndo();
            _moveStack.push(move);
            setChanged();
            notifyObservers();
            return;
        }
        startUndo();
        _moveStack.push(move);
        if (move.isJump()) {
            set(move.col0(), move.row0(), EMPTY);
            _numJumps += 1;
        } else if (move.isExtend()) {
            _numJumps = 0;
            incrPieces(_whoseMove, 1);
        }

        set(move.col1(), move.row1(), _whoseMove);
        int pos = index(move.col1(), move.row1());
        for (int col = -1; col < 2; col++) {
            for (int row = -1; row < 2; row++) {
                int target = neighbor(pos, col, row);
                if (get(target) == _whoseMove.opposite()) {
                    set(target, _whoseMove);
                    incrPieces(_whoseMove, 1);
                    incrPieces(_whoseMove.opposite(), -1);
                }
            }
        }

        _numMoves += 1;
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    /** Update to indicate that the current player passes, assuming it
     *  is legal to do so.  The only effect is to change whoseMove(). */
    void pass() {
        if (canMove(_whoseMove)) {
            throw error("Pass not allowed.");
        }
        _whoseMove = _whoseMove.opposite();
        _numMoves += 1;
        setChanged();
        notifyObservers();
    }

    /** Undo the last move. */
    void undo() {
        while (_changeStack.peek() != null) {
            BoardChangeArray change = _changeStack.pop();
            unrecordedSet(change.index(), change.oldColor());
            incrPieces(change.oldColor(), 1);
            incrPieces(change.newColor(), -1);
        }
        _changeStack.pop();
        _moveStack.pop();
        _whoseMove = _whoseMove.opposite();
        _numMoves -= 1;
        if (_changeStack.peek() != null) {
            _numJumps = _changeStack.peek().numJumps();
        } else {
            _numJumps = 0;
        }
        setChanged();
        notifyObservers();
    }

    /** Indicate beginning of a move in the undo stack. */
    private void startUndo() {
        _changeStack.push(null);
    }

    /** Add an undo action for changing SQ to NEWCOLOR on the current board,
     * with the appropriate NUMJUMPS. */
    private void addUndo(int sq, PieceColor newColor, int numJumps) {
        _changeStack.push(new BoardChangeArray(sq, newColor, get(sq),
                numJumps));
    }

    /** Return true iff it is legal to place a block at C R. */
    boolean legalBlock(char c, char r) {
        char cP = (char) ('h' - (c - '`'));
        char rP = (char) ('8' - (r - '0'));
        return (get(c, r) == EMPTY || get(c, r) == BLOCKED)
                && (get(c, rP) == EMPTY || get(c, rP) == BLOCKED)
                && (get(cP, r) == EMPTY || get(cP, r) == BLOCKED)
                && (get(cP, rP) == EMPTY || get(cP, rP) == BLOCKED);
    }

    /** Return true iff it is legal to place a block at CR. */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Set a block on the square C R and its reflections across the middle
     *  row and/or column, if that square is unoccupied and not
     *  in one of the corners. Has no effect if any of the squares is
     *  already occupied by a block.  It is an error to place a block on a
     *  piece. */
    void setBlock(char c, char r) {
        if (_moveStack.size() > 0) {
            throw error("Can only add blocks to the initial configuration.");
        }
        if (!legalBlock(c, r)) {
            throw error("Illegal block placement.");
        }
        char cP = (char) ('h' - (c - '`'));
        char rP = (char) ('8' - (r - '0'));

        unrecordedSet(c, r, BLOCKED);
        unrecordedSet(c, rP, BLOCKED);
        unrecordedSet(cP, r, BLOCKED);
        unrecordedSet(cP, rP, BLOCKED);

        setChanged();
        notifyObservers();
    }

    /** Place a block at CR. */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Return a list of all moves made since the last clear (or start of
     *  game). */
    List<Move> allMoves() {
        return new ArrayList(_moveStack);
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /* .equals used only for testing purposes. */
    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board)
                && _numRedPieces == other._numRedPieces
                && _numBluePieces == other._numBluePieces
                && _numMoves == other._numMoves
                && _numJumps == other._numJumps
                && _whoseMove == other._whoseMove
                && allMoves().equals(other.allMoves());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /** Return a text depiction of the board (not a dump).  If LEGEND,
     *  supply row and column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        for (char row = '7'; row >= '1'; row--) {
            if (legend) {
                out.format("%c", row);
            }
            out.format("  ", "");
            for (char col = 'a'; col <= 'g'; col++) {
                if (get(col, row) == EMPTY) {
                    out.format("%c ", '-');
                } else if (get(col, row) == BLOCKED) {
                    out.format("%c ", 'X');
                } else if (get(col, row) == RED) {
                    out.format("%c ", 'r');
                } else if (get(col, row) == BLUE) {
                    out.format("%c ", 'b');
                }
            }
            out.format("%n");
        }
        if (legend) {
            out.format("  a b c d e f g");
        }
        return out.toString();
    }

    /** For reasons of efficiency in copying the board,
     *  we use a 1D array to represent it, using the usual access
     *  algorithm: row r, column c => index(r, c).
     *
     *  Next, instead of using a 7x7 board, we use an 11x11 board in
     *  which the outer two rows and columns are blocks, and
     *  row 2, column 2 actually represents row 0, column 0
     *  of the real board.  As a result of this trick, there is no
     *  need to special-case being near the edge: we don't move
     *  off the edge because it looks blocked.
     *
     *  Using characters as indices, it follows that if 'a' <= c <= 'g'
     *  and '1' <= r <= '7', then row c, column r of the board corresponds
     *  to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     *  re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION]. */
    private final PieceColor[] _board;

    /** Player that is on move. */
    private PieceColor _whoseMove;
    /** Stack of undoable moves since the last clear (or start of game). Each
     *  item of the stack is a BoardChangeArray containing an index, a current
     *  PieceColor, a former PieceColor, and a number of undisturbed jumps. */
    private Stack<BoardChangeArray> _changeStack;

    /** Stack of all moves made since the last clear (or start of game). */
    private Stack<Move> _moveStack;

    /** Number of red pieces. */
    private int _numRedPieces;
    /** Number of blue pieces. */
    private int _numBluePieces;
    /** Number of total moves. */
    private int _numMoves;
    /** Number of consecutive jumps without intervening extends. */
    private int _numJumps;
}
