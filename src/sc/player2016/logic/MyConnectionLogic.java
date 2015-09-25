package sc.player2016.logic;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sc.player2016.Starter;
import sc.plugin2016.Board;
import sc.plugin2016.Field;
import sc.plugin2016.FieldType;
import sc.plugin2016.GameState;
import sc.plugin2016.IGameHandler;
import sc.plugin2016.Move;
import sc.plugin2016.Player;
import sc.plugin2016.PlayerColor;
import sc.shared.GameResult;
/**
 * 
 * @author Tobi
 * 
 * This Logic builds simple Connections, which dont need to get in a Line
 *
 */
public class MyConnectionLogic implements IGameHandler {

    private Starter client;
    private GameState gameState;
    private Player currentPlayer;
    private PlayerColor color;

    /*
     * Klassenweit verfuegbarer Zufallsgenerator der beim Laden der klasse
     * einmalig erzeugt wird und dann immer zur Verfuegung steht.
     */
    private static final Random rand = new SecureRandom();

    /**
     * Erzeugt ein neues Strategieobjekt.
     * 
     * @param client
     *            Der Zugrundeliegende Client der mit dem Spielserver
     *            kommunizieren kann.
     */
    public MyConnectionLogic(Starter client) {
	this.client = client;
	this.color = client.getMyColor();
	System.out.println("My Color has been set to " + color);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gameEnded(GameResult data, PlayerColor color,
	    String errorMessage) {

	System.out.println("*** Das Spiel ist beendet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestAction() {
	System.out.println("*** Es wurde ein Zug angefordert");
	List<Move> possibleMoves = gameState.getPossibleMoves();
	System.out.println("*** sende zug: ");
	if (color == null) {
	    color = currentPlayer.getPlayerColor();
	    System.out.println("Color set manually to "
		    + currentPlayer.getPlayerColor() + " :(");
	}
	Board gameBoard = gameState.getBoard();
	ArrayList<Field> myFields = new ArrayList<>();
	for (int i = 0; i < gameBoard.getFields().length; i++) {
	    for (int j = 0; j < gameBoard.getFields().length; j++) {
		if (gameBoard.getOwner(i, j) == color) {
		    myFields.add(gameBoard.getField(i, j));
		}
	    }
	}
	for (Field field : myFields) {
	    System.out.println(field);
	}
	ArrayList<Move> startMoves = new ArrayList<>();
	for (Move move : possibleMoves) {
	    if (gameBoard.getField(move.getX(), move.getY()).getType() == FieldType.BLUE) {
		startMoves.add(move);
	    }
	}
	ArrayList<Move> lineMoves = new ArrayList<>();
	Move selection = possibleMoves.get(rand.nextInt(possibleMoves.size()));
	for (Field field : myFields) {
	    int x1 = field.getX();
	    int y1 = field.getY();
	    int x2 = -1;
	    int y2 = -1;
	    for (Move move : possibleMoves) {
		x2 = move.getX();
		y2 = move.getY();
		if (checkPossibleWire(x1, y1, x2, y2)) {
		    lineMoves.add(move);
		}
	    }

	}
	Move selectionFirst = startMoves.get(rand.nextInt(startMoves.size()));
	System.out.println("*** setze Strommast auf x=" + selection.getX()
		+ ", y=" + selection.getY());
	if (myFields.isEmpty()) {
	    sendAction(selectionFirst);
	} else {
	    if(!lineMoves.isEmpty()) {
		sendAction(lineMoves.get(rand.nextInt(lineMoves.size())));
	    } else {
		sendAction(selection);
	    }
	}
    }

    public boolean checkPossibleWire(int x1, int y1, int x2, int y2) {
	if ((Math.abs(x1-x2)==2 && Math.abs(y1-y2)==1) || (Math.abs(x1-x2)==1 && Math.abs(y1-y2)==2)) {
	    return true;
	}
	return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(Player player, Player otherPlayer) {
	currentPlayer = player;

	System.out.println("*** Spielerwechsel: " + player.getPlayerColor());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(GameState gameState) {
	this.gameState = gameState;
	currentPlayer = gameState.getCurrentPlayer();

	System.out.print("*** Das Spiel geht vorran: Zug = "
		+ gameState.getTurn());
	System.out.println(", Spieler = " + currentPlayer.getPlayerColor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendAction(Move move) {
	client.sendMove(move);
    }

}
