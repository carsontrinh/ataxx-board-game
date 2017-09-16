package ataxx;

import static ataxx.PieceColor.*;
import static java.lang.Math.min;
import static java.lang.Math.max;

import java.util.Stack;

/** A Player that computes its own moves.
 *  @author Carson Trinh
 *  Took inspiration for implementing findMove from Paul Hilfinger's CS61B
 *  Lecture 22 Slides (pseudocode)
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 4;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        if (!board().canMove(myColor())) {
            System.out.println(myColor().toString() + " passes.");
            return Move.pass();
        }
        Move move = findMove();
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == RED) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        System.out.println(myColor().toString() + " moves " + _lastFoundMove
                .toString() + ".");
        return _lastFoundMove;
    }

    /** Used to communicate best moves found by findMove, when asked for. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels before using a static estimate. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        if (depth == 0 || board.gameOver()) {
            return simpleFindMove(board, saveMove, sense, alpha, beta);
        }
        if (sense == 1) {
            Move bestMoveSoFar = null;
            int bestScoreSoFar = -INFTY;
            Stack<Move> possibleMoves = findPossibleMoves(RED, board);
            for (Move move : possibleMoves) {
                board.makeMove(move);
                int respondingScore = findMove(board, depth - 1, false, -1,
                        alpha, beta);
                board.undo();
                if (respondingScore > bestScoreSoFar) {
                    bestMoveSoFar = move;
                    bestScoreSoFar = respondingScore;
                    alpha = max(alpha, respondingScore);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            if (saveMove) {
                _lastFoundMove = bestMoveSoFar;
            }
            return bestScoreSoFar;
        } else if (sense == -1) {
            Move bestMoveSoFar = null;
            int bestScoreSoFar = INFTY;
            Stack<Move> possibleMoves = findPossibleMoves(BLUE, board);
            for (Move move : possibleMoves) {
                board.makeMove(move);
                int respondingScore = findMove(board, depth - 1, false, 1,
                        alpha, beta);
                board.undo();
                if (respondingScore < bestScoreSoFar) {
                    bestMoveSoFar = move;
                    bestScoreSoFar = respondingScore;
                    beta = min(beta, respondingScore);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            if (saveMove) {
                _lastFoundMove = bestMoveSoFar;
            }
            return bestScoreSoFar;
        }
        System.out.println("Should never hit this case...(not simple)");
        return 0;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches one level
     *  deep. */
    private int simpleFindMove(Board board, boolean saveMove, int sense, int
            alpha, int beta) {
        boolean gameOver = board.gameOver();
        if (staticScore(board) > 0 && gameOver) {
            return WINNING_VALUE;
        } else if (staticScore(board) < 0 && gameOver) {
            return -WINNING_VALUE;
        } else if (staticScore(board) == 0 && gameOver) {
            return 0;
        }
        if (sense == 1) {
            Move bestMoveSoFar = null;
            int bestScoreSoFar = -INFTY;
            Stack<Move> possibleMoves = findPossibleMoves(RED, board);
            for (Move move : possibleMoves) {
                board.makeMove(move);
                int respondingScore = staticScore(board);
                board.undo();
                if (respondingScore > bestScoreSoFar) {
                    bestMoveSoFar = move;
                    bestScoreSoFar = respondingScore;
                    alpha = max(alpha, respondingScore);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            if (saveMove) {
                _lastFoundMove = bestMoveSoFar;
            }
            return bestScoreSoFar;
        } else if (sense == -1) {
            Move bestMoveSoFar = null;
            int bestScoreSoFar = INFTY;
            Stack<Move> possibleMoves = findPossibleMoves(BLUE, board);
            for (Move move : possibleMoves) {
                board.makeMove(move);
                int respondingScore = staticScore(board);
                board.undo();
                if (respondingScore < bestScoreSoFar) {
                    bestMoveSoFar = move;
                    bestScoreSoFar = respondingScore;
                    beta = min(beta, respondingScore);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            if (saveMove) {
                _lastFoundMove = bestMoveSoFar;
            }
            return bestScoreSoFar;
        }
        System.out.println("Should never hit this case...(simple)");
        return 0;
    }

    /** Return a stack of all possible moves for PLAYER on the BOARD. */
    private Stack<Move> findPossibleMoves(PieceColor player, Board board) {
        Stack<Move> possibleMoves = new Stack<Move>();
        if (!board.canMove(player)) {
            possibleMoves.push(Move.pass());
            return possibleMoves;
        }
        PieceColor subject;
        PieceColor objective;
        if (isLessEmpty(player, board)) {
            subject = EMPTY;
            objective = player;
        } else {
            subject = player;
            objective = EMPTY;
        }
        for (char r = '7'; r >= '1'; r--) {
            for (char c = 'a'; c <= 'g'; c++) {
                if (board.get(c, r) == subject) {
                    for (char c1 = (char) (c - 2); c1 <= c + 2; c1++) {
                        for (char r1 = (char) (r - 2); r1 <= r + 2; r1++) {
                            if (board.get(c1, r1) == objective) {
                                if (subject == player) {
                                    possibleMoves.push(Move.move(c, r, c1, r1));
                                } else {
                                    possibleMoves.push(Move.move(c1, r1, c, r));
                                }
                            }
                        }
                    }
                }
            }
        }
        return possibleMoves;
    }

    /**
     * Returns true there are less EMPTY PieceColors than PLAYER PieceColors
     * in BOARD.
     */
    private boolean isLessEmpty(PieceColor player, Board board) {
        int playerPieces = 0;
        int emptyPieces = 0;
        for (char r = '7'; r >= '1'; r--) {
            for (char c = 'a'; c <= 'g'; c++) {
                if (board.get(c, r) == player) {
                    playerPieces += 1;
                } else  if (board.get(c, r) == EMPTY) {
                    emptyPieces += 1;
                }
            }
        }
        return emptyPieces < playerPieces;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        int score = board.redPieces() - board.bluePieces();
        return score;
    }
}
