package qirkat;

import static org.junit.Assert.*;
import org.junit.Test;

/** Test Board functions.
 * @author Mudabbir Khan
 */
public class MoreBoardTests {
    private final char[][] boardRepr = new char[][]{
        {'b', 'b', 'b', 'b', 'b'},
        {'b', 'b', 'b', 'b', 'b'},
        {'b', 'b', '-', 'w', 'w'},
        {'w', 'w', 'w', 'w', 'w'},
        {'w', 'w', 'w', 'w', 'w'}
    };

    private final PieceColor currMove = PieceColor.WHITE;

    /** @return the String representation of the initial state. This will
     * be a string in which we concatenate the values from the bottom of
     * board upwards, so we can pass it into setPieces. Read the comments
     * in Board#setPieces for more information.
     *
     * For our current boardRepr, the String returned by
     * getInitialRepresentation is
     * "  w w w w w\n  w w w w w\n  b b - w w\n  b b b b b\n  b b b b b"
     *
     * We use a StringBuilder to avoid recreating Strings (because Strings
     * are immutable).
     */
    private String getInitialRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (int i = boardRepr.length - 1; i >= 0; i--) {
            for (int j = 0; j < boardRepr[0].length; j++) {
                sb.append(boardRepr[i][j] + " ");
            }
            sb.deleteCharAt(sb.length() - 1);
            if (i != 0) {
                sb.append("\n  ");
            }
        }
        return sb.toString();
    }

    private Board getBoard() {
        Board b = new Board();
        b.setPieces(getInitialRepresentation(), currMove);
        return b;
    }

    private void resetToInitialState(Board b) {
        b.setPieces(getInitialRepresentation(), currMove);
    }

    @Test
    public void testGetMoves() {
        Board b = getBoard();
        b.setPieces("- - - - b - - - - - - - - - - - - - - - w - - - -",
                currMove);
        assertEquals(b.getMoves().size(), 0);

    }

    @Test
    public void testReset() {
        Board b1 = getBoard();
        Board b2 = getBoard();
        Move m = Move.move('c', '2', 'c', '3');
        b2.makeMove(m);
        resetToInitialState(b2);
        assertEquals(b2.toString(), b1.toString());
    }
}
