package qirkat;

import ucb.gui2.Pad;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.util.Observer;
import java.util.Observable;
import java.awt.geom.Line2D;

import java.awt.event.MouseEvent;

import static qirkat.PieceColor.*;


/** Widget for displaying a Qirkat board.
 *  @author Mudabbir Khan
 */
class BoardWidget extends Pad implements Observer {

    /** Length of side of one square, in pixels. */
    static final int SQDIM = 50;
    /** Number of squares on a side. */
    static final int SIDE = Move.SIDE;
    /** Radius of circle representing a piece. */
    static final int PIECE_RADIUS = 15;

    /** Color of white pieces. */
    private static final Color WHITE_COLOR = Color.WHITE;
    /** Color of "phantom" white pieces. */
    /** Color of black pieces. */
    private static final Color BLACK_COLOR = Color.BLACK;
    /** Color of painted lines. */
    private static final Color LINE_COLOR = Color.BLACK;
    /** Color of blank squares. */
    private static final Color BLANK_COLOR = new Color(100, 100, 100, 0);

    /** Comment. */
    private static final Color HIGHLIGHT_COLOR = new Color(255, 165, 0, 1 / 2);

    /** Stroke for lines.. */
    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);

    /** Stroke for outlining pieces. */
    private static final BasicStroke OUTLINE_STROKE = LINE_STROKE;

    /** Model being displayed. */
    private static Board _model;

    /** A new widget displaying MODEL. */
    BoardWidget(Board model) {
        _model = model;
        setMouseHandler("click", this::readMove);
        _model.addObserver(this);
        _dim = SQDIM * SIDE;
        setPreferredSize(_dim, _dim);
        _col = 0;
        _row = 0;
        _set = false;
    }

    /** Indicate that the squares indicated by MOV are the currently selected
     *  squares for a pending move. */
    void indicateMove(Move mov) {
        _selectedMove = mov;
        repaint();
    }

    @Override
    public synchronized void paintComponent(Graphics2D g) {
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, _dim, _dim);

        for (int i = 0; i <= SIDE - 2; i += 2) {
            g.setColor(LINE_COLOR);
            g.setStroke(LINE_STROKE);
            g.draw(new Line2D.Double(SIDE * SQDIM - i * SQDIM - SQDIM / 2,
                    SQDIM * SIDE - SQDIM / 2,
                    SIDE * SQDIM - SQDIM / 2,
                    SQDIM * SIDE - i * SQDIM - SQDIM / 2));
            g.draw(new Line2D.Double(SQDIM / 2,
                    SIDE * SQDIM - i * SQDIM - SQDIM / 2,
                    SIDE * SQDIM - i * SQDIM - SQDIM / 2, SQDIM / 2));
            g.draw(new Line2D.Double(SQDIM / 2, SQDIM / 2 + i * SQDIM,
                    SIDE * SQDIM - i * SQDIM - SQDIM / 2,
                    SIDE * SQDIM - SQDIM / 2));
            g.draw(new Line2D.Double(SQDIM / 2 + i * SQDIM, SQDIM / 2,
                    SIDE * SQDIM - SQDIM / 2,
                    SIDE * SQDIM - i * SQDIM - SQDIM / 2));
        }

        for (int i = 0; i < SIDE; i++) {
            g.setColor(LINE_COLOR);
            g.setStroke(LINE_STROKE);
            g.draw(new Line2D.Double(SQDIM / 2, SQDIM / 2 + i * SQDIM,
                    SIDE * SQDIM - SQDIM / 2,
                    SQDIM / 2 + i * SQDIM));
            g.draw(new Line2D.Double(SQDIM / 2 + i * SQDIM, SQDIM / 2,
                    SQDIM / 2 + i * SQDIM, SIDE * SQDIM - SQDIM / 2));
        }

        int pIndex = 0;
        for (PieceColor p : _model.getBoard()) {
            render(g, p, pIndex);
            pIndex += 1;
        }
    }

    /** Comment G to P to INDEX. */
    private void render(Graphics2D g, PieceColor p, int index) {
        if (p == WHITE) {
            g.setColor(WHITE_COLOR);
        } else if (p == BLACK) {
            g.setColor(BLACK_COLOR);
        } else {
            g.setColor(BLANK_COLOR);
        }
        g.fillOval((SQDIM * SIDE / (SIDE * 4)) + (index % SIDE) * SQDIM,
                SQDIM * SIDE - (Math.floorDiv(index, SIDE) * SQDIM)
                        - SQDIM + (SQDIM * SIDE / (SIDE * 4)),
                PIECE_RADIUS * 2, PIECE_RADIUS * 2);
    }

    /** Comment G to P to INDEX. */
    private void render1(Graphics2D g, PieceColor p, int index) {
        if (p == WHITE || p == BLACK) {
            g.setBackground(HIGHLIGHT_COLOR);
        }
        g.fillRect((SQDIM * SIDE / (SIDE * 4)) + (index % SIDE) * SQDIM,
                SQDIM * SIDE - (Math.floorDiv(index, SIDE) * SQDIM)
                        - SQDIM + (SQDIM * SIDE / (SIDE * 4)),
                PIECE_RADIUS * 2, PIECE_RADIUS * 2);
    }

    /** Notify observers of mouse's current position from click event WHERE. */
    private void readMove(String unused, MouseEvent where) {
        int x = where.getX(), y = where.getY();
        char mouseCol, mouseRow;
        if (where.getButton() == MouseEvent.BUTTON1) {
            mouseCol = (char) (x / SQDIM + 'a');
            mouseRow = (char) ((SQDIM * SIDE - y) / SQDIM + '1');
            if (mouseCol >= 'a' && mouseCol <= 'e'
                && mouseRow >= '1' && mouseRow <= '5') {

                if (_set) {
                    if (_model.get(mouseCol, mouseRow) == WHITE) {
                        _model.set(Move.index(mouseCol, mouseRow), BLACK);
                    } else if (_model.get(mouseCol, mouseRow) == BLACK) {
                        _model.set(Move.index(mouseCol, mouseRow), EMPTY);
                    } else {
                        _model.set(Move.index(mouseCol, mouseRow), WHITE);
                    }
                } else {
                    if (_col == 0 && _row == 0) {
                        _col = mouseCol;
                        _row = mouseRow;
                    } else if (_row == mouseRow && _col == mouseCol) {
                        _col = 0;
                        _row = 0;
                    } else {
                        Move mov = Move.move(_col, _row, mouseCol, mouseRow);
                        _model.makeMove(mov);
                        _row = 0;
                        _col = 0;
                    }
                }


                setChanged();
                notifyObservers("" + mouseCol + mouseRow);
            }
        }
    }


    @Override
    public synchronized void update(Observable model, Object arg) {
        repaint();
    }

    /** Col for mouse click. */
    private char _col;

    /** Row for mouse click. */
    private char _row;

    /** For setting pieces in setup mode. */
    private static boolean _set;

    /** Dimension of current drawing surface in pixels. */
    private int _dim;

    /** A partial Move indicating selected squares. */
    private Move _selectedMove;

    /** Change the state between setup and playing. */
    public static void activateSetup() {
        if (_set) {
            _set = false;
        } else {
            _set = true;
        }
    }
}
