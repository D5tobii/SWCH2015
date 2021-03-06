package sc.player2016.logic;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sc.player2016.Starter;
import sc.plugin2016.Board;
import sc.plugin2016.Connection;
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
 *         This Logic is more aggressive and also avoids blocked paths, if
 *         they're directly in front The "Blocking" part is still random and
 *         needs further work
 *
 */
public class MyBlockLogic implements IGameHandler {

    private Starter client;
    private GameState gameState;
    private Player currentPlayer;
    private PlayerColor color;
    private String start = "";
    private Field myHighestField;
    private Field myLowestField;

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
    public MyBlockLogic(Starter client) {
	this.client = client;
	this.color = client.getMyColor();
	System.out.println("My Color has been set to " + color);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gameEnded(GameResult data, PlayerColor color, String errorMessage) {
	System.out.println("*** Das Spiel ist beendet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestAction() {
	setMyColor();
	
	List<Move> possibleMoves = gameState.getPossibleMoves();
	Board gameBoard = gameState.getBoard();
	
	ArrayList<Field> myFields = getMyFields(gameBoard);
	ArrayList<Move> startMoves = collectStartMoves(possibleMoves, gameBoard);
	checkMyFields(myFields);
	checkStart(myFields);
	checkDirection(myFields);
	ArrayList<Move> lineMoves = collectLineMoves(possibleMoves, myFields);
	Move selection = possibleMoves.get(rand.nextInt(possibleMoves.size()));
	Move selectionFirst = startMoves.get(rand.nextInt(startMoves.size()));
	decideMove(myFields, lineMoves, selection, selectionFirst);
    }
    
    private void setMyColor() {
	if (color == null) {
	    color = currentPlayer.getPlayerColor();
	    System.out.println("Color set manually to " + currentPlayer.getPlayerColor() + " :(");
	}
    }
    
    private ArrayList<Field> getMyFields(Board gameBoard) {
	ArrayList<Field> myFields = new ArrayList<>();
	for (int i = 0; i < gameBoard.getFields().length; i++) {
	    for (int j = 0; j < gameBoard.getFields().length; j++) {
		if (gameBoard.getOwner(i, j) == color) {
		    myFields.add(gameBoard.getField(i, j));
		}
	    }
	}
	return myFields;
    }
    
    private ArrayList<Move> collectLineMoves(List<Move> possibleMoves, ArrayList<Field> myFields) {
	ArrayList<Move> lineMoves = new ArrayList<>();
	ArrayList<Move> defensiveMoves = new ArrayList<>();
	if (!myFields.isEmpty()) {
	    if (color == PlayerColor.RED) {
		for (int k = 0; k < myFields.size(); k++) {
		    if (myFields.get(k).getY() == 2 || myFields.get(k).getY() == 21) {
			Field enemyField = null;
			for (int i = 0; i < gameState.getBoard().getFields().length; i++) {
			    for (int j = 0; j < gameState.getBoard().getFields().length; j++) {
				if (gameState.getBoard().getOwner(i, j) == color.opponent() && (j <= 3 || j >= 20)) {
				    enemyField = gameState.getBoard().getField(i, j);
				}
			    }
			}
			if (enemyField == null) {
			    findLineMoves(possibleMoves, myFields, lineMoves);
			} else if (enemyField.getY() <= 2) {
			    for (Move move : possibleMoves) {
				if (move.getY() == 0 && ((move.getX() == myFields.get(k).getX() - 1) || (move.getX() == myFields.get(k).getX() + 1))) {
				    if (checkPossibleWire(myFields.get(k).getX(), myFields.get(k).getY(), move.getX(), move.getY())) {
					defensiveMoves.add(move);
				    }
				}
			    }
			} else if (enemyField.getY() >= 21) {
			    for (Move move : possibleMoves) {
				if (move.getY() == 23 && ((move.getX() == myFields.get(k).getX() - 1) || (move.getX() == myFields.get(k).getX() + 1))) {
				    if (checkPossibleWire(myFields.get(k).getX(), myFields.get(k).getY(), move.getX(), move.getY())) {
					defensiveMoves.add(move);
				    }
				}
			    }
			} else {
			    findLineMoves(possibleMoves, myFields, lineMoves);
			}
		    } else {
			findLineMoves(possibleMoves, myFields, lineMoves);
		    }
		}
		if (!defensiveMoves.isEmpty() && !checkBorderField(myFields)) {
		    return defensiveMoves;
		} else {
		    return lineMoves;
		}
	    } else {
		if (gameState.getLastMove().getX() < myHighestField.getX()) {
		    start = "BOTTOM";
		}
		if (gameState.getLastMove().getX() >= myLowestField.getX()) {
		    start = "TOP";
		}
		findLineMoves(possibleMoves, myFields, lineMoves);
		return lineMoves;
	    }
	} else {
	    return null;
	}
    }
    
    private void checkMyFields(ArrayList<Field> myFields) {
	if (!myFields.isEmpty()) {
	    Field highField = myFields.get(0);
	    Field lowField = myFields.get(0);
	    for (Field field : myFields) {
		if (field.getX() <= highField.getX()) {
		    highField = field;
		}
		if (field.getX() >= lowField.getX()) {
		    lowField = field;
		}
	    }
	    myHighestField = highField;
	    myLowestField = lowField;
	}
    }

    private void decideMove(ArrayList<Field> myFields, ArrayList<Move> lineMoves, Move selection, Move selectionFirst) {
	if (myFields.isEmpty()) {
	    sendAction(selectionFirst);
	} else {
	    if (!lineMoves.isEmpty()) {
		Move selectionLine = findBestMove(lineMoves);
		sendAction(selectionLine);

	    } else {
		sendAction(selection);

	    }
	}
    }

    private ArrayList<Move> collectStartMoves(List<Move> possibleMoves, Board gameBoard) {
	ArrayList<Move> startMoves = new ArrayList<>();
	for (Move move : possibleMoves) {
	    if (color == PlayerColor.BLUE) {
		Field enemyField = null;
		for (int i = 0; i < gameBoard.getFields().length; i++) {
		    for (int j = 0; j < gameBoard.getFields().length; j++) {
			if (gameBoard.getOwner(i, j) == color.opponent()) {
			    enemyField = gameBoard.getField(i, j);
			}
		    }
		}
		if (enemyField.getY() <= 12) {
		    if (move.getX() == enemyField.getX()) {
			startMoves.add(move);
		    }
		} else {
		    if (move.getX() == enemyField.getX()) {
			startMoves.add(move);
		    }
		}
	    }
	    if (color == PlayerColor.RED) {
		if (gameBoard.getField(move.getX(), move.getY()).getY() == 2 || gameBoard.getField(move.getX(), move.getY()).getY() == 21) {
		    if (move.getX() >= 10 && move.getX() <= 14) {
			startMoves.add(move);
		    }
		}
	    }
	}
	return startMoves;
    }

    private void findLineMoves(List<Move> possibleMoves, ArrayList<Field> myFields, ArrayList<Move> lineMoves) {
	for (Field field3 : myFields) {
	    int x1 = field3.getX();
	    int y1 = field3.getY();
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
    }

    private boolean checkBorderField(ArrayList<Field> myFields) {
	boolean val = false;
	for (Field field : myFields) {
	    if (field.getOwner() == color && (field.getY() == 0 || field.getY() == 23 || field.getX() == 0 || field.getY() == 23)) {
		val = true;
	    }
	}
	return val;
    }

    private void checkDirection(ArrayList<Field> myFields) {
	if (color == PlayerColor.BLUE) {
	    for (int i = 0; i < myFields.size(); i++) {
		if (myFields.get(i).getType() == FieldType.BLUE) {
		    if (myFields.get(i).getX() == 0) {
			start = "TOP";
		    } else if (myFields.get(i).getX() == gameState.getBoard().getFields().length - 1) {
			start = "BOTTOM";
		    }
		}
	    }
	}
	if (color == PlayerColor.RED) {
	    for (int i = 0; i < myFields.size(); i++) {
		if (myFields.get(i).getType() == FieldType.RED) {
		    if (myFields.get(i).getY() == 0) {
			start = "LEFT";
		    } else if (myFields.get(i).getY() == gameState.getBoard().getFields().length - 1) {
			start = "RIGHT";
		    }
		}
	    }
	}
    }

    private void checkStart(ArrayList<Field> myFields) {
	if (start.equals("") && (!myFields.isEmpty())) {
	    Field firstField = myFields.get(0);
	    if (firstField.getY() <= 11) {
		start = "LEFT";
	    } else if (firstField.getY() > 11) {
		start = "RIGHT";
	    } else if (firstField.getX() <= 11) {
		start = "TOP";
	    } else if (firstField.getX() > 11) {
		start = "BOTTOM";
	    } else {
		if (color == PlayerColor.BLUE) {
		    start = "BOTTOM";
		} else {
		    start = "LEFT";
		}
	    }
	}
    }

    private Move findBestMove(ArrayList<Move> moves) {
	Move move = null;
	ArrayList<Move> returnMoves = new ArrayList<>();
	switch (start) {
	case "LEFT":
	    for (Move m : moves) {
		if (move == null) {
		    move = m;
		} else {
		    if (m.getY() > move.getY()) {
			move = m;
		    }
		}
	    }
	    for (Move m : moves) {
		if (m.getY() == move.getY()) {
		    returnMoves.add(m);
		}
	    }
	    return returnMoves.get(rand.nextInt(returnMoves.size()));
	case "RIGHT":
	    for (Move m : moves) {
		if (move == null) {
		    move = m;
		} else {
		    if (m.getY() < move.getY()) {
			move = m;
		    }
		}
	    }
	    for (Move m : moves) {
		if (m.getY() == move.getY()) {
		    returnMoves.add(m);
		}
	    }
	    return returnMoves.get(rand.nextInt(returnMoves.size()));
	case "BOTTOM":
	    for (Move m : moves) {
		if (move == null) {
		    move = m;
		} else {
		    if (m.getX() < move.getX()) {
			move = m;
		    }
		}
	    }
	    for (Move m : moves) {
		if (m.getY() == move.getY()) {
		    returnMoves.add(m);
		}
	    }
	    return returnMoves.get(rand.nextInt(returnMoves.size()));
	case "TOP":
	    for (Move m : moves) {
		if (move == null) {
		    move = m;
		} else {
		    if (m.getX() > move.getX()) {
			move = m;
		    }
		}
	    }
	    for (Move m : moves) {
		if (m.getY() == move.getY()) {
		    returnMoves.add(m);
		}
	    }
	    return returnMoves.get(rand.nextInt(returnMoves.size()));
	}

	return null;
    }

    public boolean checkPossibleWire(int x1, int y1, int x2, int y2) {
	if (((Math.abs(x1 - x2) == 2 && Math.abs(y1 - y2) == 1) || (Math.abs(x1 - x2) == 1 && Math.abs(y1 - y2) == 2))) {
	    boolean val = true;
	    // 2 rechts 1 runter
	    if (x2 == x1 + 1 && y2 == y1 + 2) {
		ArrayList<Connection> connections = (ArrayList<Connection>) gameState.getBoard().connections;
		for (Connection connection : connections) {
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 + 1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 1 && connection.x1 == x1 + 1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.1");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 + 2 && connection.y2 == y1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 1 && connection.x1 == x1 + 2 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.2");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 1 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 + 1 && connection.x1 == x1 - 1 && connection.y1 == y1 + 2)) {
			//System.out.println("Path blocked if No.3");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 + 1 && connection.x2 == x1 && connection.y2 == y1 + 3)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 + 1 && connection.x1 == x1 && connection.y1 == y1 + 3)) {
			//System.out.println("Path blocked if No.4");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 1 && connection.y2 == y1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 + 1 && connection.x1 == x1 - 1 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.5");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 && connection.y1 == y1 + 2)) {
			//System.out.println("Path blocked if No.6");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 + 2 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 && connection.y2 == y1 + 1 && connection.x1 == x1 + 2 && connection.y1 == y1 + 2)) {
			//System.out.println("Path blocked if No.7");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 - 1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 - 1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.8");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 + 2 && connection.x2 == x1 + 2 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 2 && connection.x1 == x1 + 2 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.9");
			val = false;
		    }
		}
	    }
	    // 2 rechts 1 hoch
	    if (x2 == x1 - 1 && y2 == y1 + 2) {
		ArrayList<Connection> connections = (ArrayList<Connection>) gameState.getBoard().connections;
		for (Connection connection : connections) {
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 1 && connection.x1 == x1 - 1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.10");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 2 && connection.y2 == y1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 1 && connection.x1 == x1 - 2 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.11");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 2 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 - 2 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.12");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 1 && connection.y2 == y1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 + 1 && connection.x1 == x1 - 1 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.13");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 + 2 && connection.x2 == x1 - 1 && connection.y2 == y1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 2 && connection.x1 == x1 - 1 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.14");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 + 2 && connection.x2 == x1 - 2 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 2 && connection.x1 == x1 - 2 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.15");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 + 1 && connection.x2 == x1 && connection.y2 == y1 + 3)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 + 1 && connection.x1 == x1 && connection.y1 == y1 + 3)) {
			//System.out.println("Path blocked if No.16");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 - 1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 - 1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.17");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 + 2 && connection.x2 == x1 - 1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 + 2 && connection.x1 == x1 - 1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.18");
			val = false;
		    }
		}
	    }
	    // 1 rechts 2 hoch
	    if (x2 == x1 - 2 && y2 == y1 + 1) {
		ArrayList<Connection> connections = (ArrayList<Connection>) gameState.getBoard().connections;
		for (Connection connection : connections) {
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 2 && connection.y2 == y1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 1 && connection.x1 == x1 - 2 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.19");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 1 && connection.x1 == x1 - 1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.20");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 && connection.x2 == x1 + 1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 && connection.x1 == x1 + 1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.21");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 && connection.x2 == x1 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 && connection.x1 == x1 && connection.y1 == y1 + 2)) {
			//System.out.println("Path blocked if No.22");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 && connection.x2 == x1 - 2 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 && connection.x1 == x1 - 2 && connection.y1 == y1 + 2)) {
			//System.out.println("Path blocked if No.23");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 + 1 && connection.x2 == x1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 + 1 && connection.x1 == x1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.24");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 2 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 + 1 && connection.x1 == x1 - 2 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.25");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 + 1 && connection.x2 == x1 - 3 && connection.y2 == y1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 + 1 && connection.x1 == x1 - 3 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.26");
			val = false;
		    }
		    if ((connection.x1 == x1 - 2 && connection.y1 == y1 && connection.x2 == x1 - 1 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 - 2 && connection.y2 == y1 && connection.x1 == x1 - 1 && connection.y1 == y1 + 2)) {
			//System.out.println("Path blocked if No.27");
			val = false;
		    }
		}
	    }
	    // 1 rechts 2 runter
	    if (x2 == x1 + 2 && y2 == y1 + 1) {
		ArrayList<Connection> connections = (ArrayList<Connection>) gameState.getBoard().connections;
		for (Connection connection : connections) {
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 + 1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 1 && connection.x1 == x1 + 1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.28");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 + 1 && connection.x2 == x1 + 2 && connection.y2 == y1)
			    || (connection.x2 == x1 && connection.y2 == y1 + 1 && connection.x1 == x1 + 2 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.29");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 + 1 && connection.x2 == x1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 + 1 && connection.x1 == x1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.30");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 + 1 && connection.x2 == x1 + 2 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 + 1 && connection.x1 == x1 + 2 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.31");
			val = false;
		    }
		    if ((connection.x1 == x1 + 2 && connection.y1 == y1 && connection.x2 == x1 + 1 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 + 2 && connection.y2 == y1 && connection.x1 == x1 + 1 && connection.y1 == y1 + 2)) {
			//System.out.println("Path blocked if No.32");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 + 2 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 + 2 && connection.y1 == y1 + 2)) {
			//System.out.println("Path blocked if No.33");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 && connection.y2 == y1 + 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 && connection.y1 == y1 + 2)) {
			//System.out.println("Path blocked if No.34");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 - 1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 - 1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.35");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 + 1 && connection.x2 == x1 + 3 && connection.y2 == y1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 + 1 && connection.x1 == x1 + 3 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.36");
			val = false;
		    }
		}
	    }
	    // 2 links 1 hoch
	    if (x2 == x1 - 1 && y2 == y1 - 2) {
		ArrayList<Connection> connections = (ArrayList<Connection>) gameState.getBoard().connections;
		for (Connection connection : connections) {
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 && connection.x2 == x1 + 1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 && connection.x1 == x1 + 1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.37");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 && connection.x2 == x1 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 && connection.x1 == x1 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.38");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 - 1 && connection.x2 == x1 + 1 && connection.y2 == y1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 - 1 && connection.x1 == x1 + 1 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.39");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 - 1 && connection.x2 == x1 + 1 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 - 1 && connection.x1 == x1 + 1 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.40");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 - 1 && connection.x2 == x1 && connection.y2 == y1 - 3)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 - 1 && connection.x1 == x1 && connection.y1 == y1 - 3)) {
			//System.out.println("Path blocked if No.41");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 - 2 && connection.y2 == y1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 1 && connection.x1 == x1 - 2 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.42");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 - 2 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 && connection.y2 == y1 - 1 && connection.x1 == x1 - 2 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.43");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 - 1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 1 && connection.x1 == x1 - 1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.44");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 2 && connection.x2 == x1 - 2 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 2 && connection.x1 == x1 - 2 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.45");
			val = false;
		    }
		}
	    }
	    // 2 links 1 runter
	    if (x2 == x1 + 1 && y2 == y1 - 2) {
		ArrayList<Connection> connections = (ArrayList<Connection>) gameState.getBoard().connections;
		for (Connection connection : connections) {
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 + 2 && connection.y2 == y1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 1 && connection.x1 == x1 + 2 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.46");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 + 1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 1 && connection.x1 == x1 + 1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.47");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 + 2 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 - 1 && connection.x1 == x1 + 2 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.48");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 - 1 && connection.x2 == x1 - 1 && connection.y2 == y1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 - 1 && connection.x1 == x1 - 1 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.49");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 - 1 && connection.x2 == x1 - 1 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 - 1 && connection.x1 == x1 - 1 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.50");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 - 1 && connection.x2 == x1 && connection.y2 == y1 - 3)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 - 1 && connection.x1 == x1 && connection.y1 == y1 - 3)) {
			//System.out.println("Path blocked if No.51");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 - 1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 - 1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.52");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.53");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 2 && connection.x2 == x1 + 2 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 2 && connection.x1 == x1 + 2 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.54");
			val = false;
		    }
		}
	    }
	    // 1 links 2 hoch
	    if (x2 == x1 - 2 && y2 == y1 - 1) {
		ArrayList<Connection> connections = (ArrayList<Connection>) gameState.getBoard().connections;
		for (Connection connection : connections) {
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 && connection.x2 == x1 + 1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 && connection.x1 == x1 + 1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.55");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 && connection.x2 == x1 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 && connection.x1 == x1 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.56");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 && connection.x2 == x1 - 2 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 && connection.x1 == x1 - 2 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.57");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 - 1 && connection.x2 == x1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 - 1 && connection.x1 == x1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.58");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 - 1 && connection.x2 == x1 - 2 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 - 1 && connection.x1 == x1 - 2 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.59");
			val = false;
		    }
		    if ((connection.x1 == x1 - 1 && connection.y1 == y1 - 1 && connection.x2 == x1 - 3 && connection.y2 == y1)
			    || (connection.x2 == x1 - 1 && connection.y2 == y1 - 1 && connection.x1 == x1 - 3 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.60");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 - 1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 1 && connection.x1 == x1 - 1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.61");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 - 2 && connection.y2 == y1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 1 && connection.x1 == x1 - 2 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.62");
			val = false;
		    }
		    if ((connection.x1 == x1 - 2 && connection.y1 == y1 && connection.x2 == x1 - 1 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 - 2 && connection.y2 == y1 && connection.x1 == x1 - 1 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.63");
			val = false;
		    }
		}
	    }
	    // 1 links 2 runter
	    if (x2 == x1 + 2 && y2 == y1 - 1) {
		ArrayList<Connection> connections = (ArrayList<Connection>) gameState.getBoard().connections;
		for (Connection connection : connections) {
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 - 1 && connection.y2 == y1 - 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 - 1 && connection.y1 == y1 - 1)) {
			//System.out.println("Path blocked if No.64");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.65");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 && connection.x2 == x1 + 2 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 && connection.x1 == x1 + 2 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.66");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 - 1 && connection.x2 == x1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 - 1 && connection.x1 == x1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.67");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 - 1 && connection.x2 == x1 + 2 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 - 1 && connection.x1 == x1 + 2 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.68");
			val = false;
		    }
		    if ((connection.x1 == x1 + 1 && connection.y1 == y1 - 1 && connection.x2 == x1 + 3 && connection.y2 == y1)
			    || (connection.x2 == x1 + 1 && connection.y2 == y1 - 1 && connection.x1 == x1 + 3 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.69");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 + 1 && connection.y2 == y1 + 1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 1 && connection.x1 == x1 + 1 && connection.y1 == y1 + 1)) {
			//System.out.println("Path blocked if No.70");
			val = false;
		    }
		    if ((connection.x1 == x1 && connection.y1 == y1 - 1 && connection.x2 == x1 + 2 && connection.y2 == y1)
			    || (connection.x2 == x1 && connection.y2 == y1 - 1 && connection.x1 == x1 + 2 && connection.y1 == y1)) {
			//System.out.println("Path blocked if No.71");
			val = false;
		    }
		    if ((connection.x1 == x1 + 2 && connection.y1 == y1 && connection.x2 == x1 + 1 && connection.y2 == y1 - 2)
			    || (connection.x2 == x1 + 2 && connection.y2 == y1 && connection.x1 == x1 + 1 && connection.y1 == y1 - 2)) {
			//System.out.println("Path blocked if No.72");
			val = false;
		    }
		}
	    }
	    return val;
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

	System.out.print("*** Das Spiel geht vorran: Zug = " + gameState.getTurn());
	System.out.println(", Spieler = " + currentPlayer.getPlayerColor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendAction(Move move) {
	System.out.println("*** setze Strommast auf x=" + move.getX() + ", y=" + move.getY());
	client.sendMove(move);
    }

}
