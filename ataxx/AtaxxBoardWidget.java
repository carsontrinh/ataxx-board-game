package ataxx;

import ucb.gui2.Pad;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.util.Observer;
import java.util.Observable;

import java.awt.event.MouseEvent;

import static ataxx.PieceColor.*;

/** Widget for displaying an Ataxx board.
 *  @author Carson Trinh
 */
class AtaxxBoardWidget extends Pad implements Observer {

    /** Length of side of one square, in pixels. */
    static final int SQDIM = 50;
    /** Margin around the grid in pixels. */
    static final int MARGIN = 30;
    /** Number of squares on a side. */
    static final int SIDE = Board.SIDE;
    /** Radius of circle representing a piece. */
    static final int PIECE_RADIUS = 15;

    /** Color of red pieces. */
    private static final Color RED_COLOR = Color.RED;
    /** Color of blue pieces. */
    private static final Color BLUE_COLOR = Color.BLUE;
    /** Color of blank squares. */
    private static final Color BLOCK_COLOR = Color.GREEN;
    /** Color of blocks. */
    private static final Color BLANK_COLOR = Color.WHITE;


    /** Color of painted lines. */
    private static final Color LINE = Color.BLACK;

    /** Stroke for lines. */
    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);

    /** Model being displayed. */
    private static Board _model;

    /** A new widget displaying MODEL. */
    AtaxxBoardWidget(Board model) {
        _model = model;
        setMouseHandler("click", this::readMove);
        _model.addObserver(this);
        _dim = SQDIM * SIDE;
        setPreferredSize(_dim, _dim);
    }

    @Override
    public synchronized void paintComponent(Graphics2D g) {
        g.setColor(BLANK_COLOR);
        g.fillRect(0, 0, _dim, _dim);

        int n = SIDE;
        int D = SQDIM;

        for (char c = 'a'; c < 'a' + n; c += 1) {
            for (char r = '1'; r < '1' + n; r += 1) {
                int x0 = (c - 'a') * SQDIM;
                int y0 = (n - (r - '1')) * SQDIM;
                if (_model.get(c, r) == RED) {
                    drawPiece(g, x0, y0, RED);
                } else if (_model.get(c, r) == BLUE) {
                    drawPiece(g, x0, y0, BLUE);
                } else if (_model.get(c, r) == BLOCKED) {
                    drawBlock(g, x0, y0);
                } else {
                    g.setColor(BLANK_COLOR);
                }
                drawQuad(g, x0, y0, 0,
                        x0 + D, y0, 0,
                        x0 + D, y0 - D, 0,
                        x0, y0 - D, 0);
            }
        }
    }

    /** Return mouse's row at last click (may be out of range if mouse off
     *  the board). */
    int mouseRow() {
        return _mouseRow;
    }

    /** Return mouse's column at last click (may be out of range if mouse off
     *  the board). */
    int mouseCol() {
        return _mouseCol;
    }

    /** Draw a block centered at (CX, CY) on G. */
    void drawBlock(Graphics2D g, int cx, int cy) {
        g.setColor(BLOCK_COLOR);
        g.fillRect(cx, cy, SQDIM, SQDIM);
    }


    /** Draw a circle centered at (CX, CY) on G for PLAYER. */
    void drawPiece(Graphics2D g, int cx, int cy, PieceColor player) {
        if (player == RED) {
            g.setColor(RED_COLOR);
        }
        if (player == BLUE) {
            g.setColor(BLUE_COLOR);
        }
        g.drawOval(cx + (SQDIM / 4), cy + (SQDIM / 4) - SQDIM, PIECE_RADIUS * 2,
                PIECE_RADIUS * 2);
        g.fillOval(cx + (SQDIM / 4), cy + (SQDIM / 4) - SQDIM, PIECE_RADIUS * 2,
                PIECE_RADIUS * 2);
    }

    /** Notify observers of mouse's current position from click event WHERE. */
    private void readMove(String unused, MouseEvent where) {
        int x = where.getX(), y = where.getY();
        char mouseCol, mouseRow;
        if (where.getButton() == MouseEvent.BUTTON1) {
            mouseCol = (char) (x / SQDIM + 'a');
            mouseRow = (char) ((SQDIM * SIDE - y) / SQDIM + '1');
            if (mouseCol >= 'a' && mouseCol <= 'g'
                && mouseRow >= '1' && mouseRow <= '7') {
                setChanged();
                notifyObservers("" + mouseCol + mouseRow);
            }
        }
    }

    @Override
    public synchronized void update(Observable model, Object arg) {
        repaint();
    }

    /** Draw outlined and filled quadrilateral on G at coplanar points
     *  (P[0], P[1], P[2]), ..., (P[9], P[10], P[11]). */
    private void drawQuad(Graphics2D g, Integer... P) {
        int[] xpoints = new int[4];
        int[] ypoints = new int[4];

        for (int i = 0; i < 4; i += 1) {
            xpoints[i] = P[i * 3];
            ypoints[i] = P[i * 3 + 1];
        }
        g.fillPolygon(xpoints, ypoints, 4);
        g.setColor(LINE);
        g.drawPolygon(xpoints, ypoints, 4);
    }

    /** Dimension of current drawing surface in pixels. */
    private int _dim;

    /** row and column of last mouse click (may be off the board). */
    private int _mouseRow, _mouseCol;
}
