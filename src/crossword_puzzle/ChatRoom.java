package crossword_puzzle;
import crossword_puzzle.BoardRenderer;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChatRoom
{
	protected static boolean gameEnd;
	private static int numOfExistingPlayers;
	protected static int numOfTotalPlayers;
	private Vector<Lock> locks;
	private Vector<Condition> conditions;
	private Vector<ServerThread> serverThreads;
	private BoardRenderer builder;
	private ServerSocket ss;
	public ChatRoom(int port)
	{
		try
		{
			System.out.println("Listening on port " + port + ".");
			ss = new ServerSocket(port);
			gameEnd = true;
			startNewGame();
		}
		catch (IOException ioe)
		{
			System.out.println("ioe in ChatRoom constructor: " + ioe.getMessage());
		}
		
	}
	
	private void startNewGame()
	{
		try
		{
			gameEnd = false;
			numOfTotalPlayers = 0;
			numOfExistingPlayers = 0;
			builder = new BoardRenderer();
			System.out.println("Waiting for players...");
			serverThreads = new Vector<ServerThread>();
			locks = new Vector<Lock>();
			conditions = new Vector<Condition>();
			Socket s = ss.accept(); // blocking
			numOfExistingPlayers = 1;
			System.out.println("Connection from: " + s.getInetAddress());
			Lock lock = new ReentrantLock();
			locks.add(lock);
			Condition condition = lock.newCondition();
			conditions.add(condition);
			ServerThread st = new ServerThread(s, this, lock, condition, 0);
			serverThreads.add(st);
			st.sendMessage("How many players will there be? ");
			String num = st.br.readLine();
			numOfTotalPlayers = Integer.parseInt(num);
			System.out.println("Number of players: " + numOfTotalPlayers + ".");
			if (numOfTotalPlayers > 1)
			{
				System.out.println("Waiting for player 2.");
				serverThreads.get(0).sendMessage("Waiting for player 2.");
			}
			System.out.println("Reading random game file.");
			boolean validBoard;
			validBoard = builder.createBoard();
			while (!validBoard) {
			    if (BoardRenderer.fileCount == 0) {
			        throw new Exception("No valid game data file exists.");
			    }

			    validBoard = builder.createBoard();
			}
			
			if (numOfTotalPlayers == 2)
			{
				Lock lock1 = new ReentrantLock();
				Condition condition1 = lock1.newCondition();
				locks.add(lock1);
				conditions.add(condition1);
				Socket s1 = ss.accept();
				numOfExistingPlayers = 2;
				System.out.println("Connection from: " + s1.getInetAddress());
				ServerThread st1 = new ServerThread(s1, this, lock1, condition1, 1);
				serverThreads.add(st1);
			}
			else if (numOfTotalPlayers == 3)
			{
				Lock lock1 = new ReentrantLock();
				Lock lock2 = new ReentrantLock();
				Condition condition1 = lock1.newCondition();
				Condition condition2 = lock2.newCondition();
				
				Socket s1 = ss.accept();
				System.out.println("Connection from: " + s1.getInetAddress());
				ServerThread st1 = new ServerThread(s1, this, lock1, condition1, 1);
				serverThreads.add(st1);
				locks.add(lock1);
				conditions.add(condition1);
				numOfExistingPlayers = 2;
				
				Socket s2 = ss.accept();
				System.out.println("Connection from: " + s2.getInetAddress());
				ServerThread st2 = new ServerThread(s2, this, lock2, condition2, 2);
				serverThreads.add(st2);
				locks.add(lock2);
				conditions.add(condition2);
				numOfExistingPlayers = 3;
			}
			System.out.println("Game can now begin.");
			for (ServerThread threads : serverThreads)
			{
				threads.sendMessage("The game is beginning.");
			}
			System.out.println("Sending game board.");
			
			locks.get(0).lock();
			conditions.get(0).signal();
			locks.get(0).unlock();
			for (ServerThread thread : serverThreads)
			{
				if (thread.index != 0)
				{
					thread.sendMessage("Player 1's turn.");
				}
			}
		}
		catch(Exception e )
		{
			System.out.println(e.getMessage());
			for (ServerThread thread : serverThreads)
			{
				thread.sendMessage(e.getMessage());
			}
		}
		
	}
	
	public void sendBoard()
	{
		for (ServerThread thread : serverThreads)
		{
			thread.sendBoard();
		}
	}
	
	protected void sendFinalScore(ServerThread st)
	{
		System.out.println("The game has concluded.");
		System.out.println("Sending scores.");
		gameEnd = true;
		for (ServerThread thread : serverThreads)
		{
			thread.sendMessage("Final Score");
			int winnerScore = 0;
			for (int i = 0; i < numOfExistingPlayers; ++i)
			{
				thread.sendMessage("Player " + (i + 1) + " - " + serverThreads.get(i).score + " correct answers.");
				if (serverThreads.get(i).score > winnerScore)
				{
					winnerScore = serverThreads.get(i).score;
				}
			}
			for (int i = 0; i < numOfExistingPlayers; ++i)
			{
				if (serverThreads.get(i).score == winnerScore)
				{
					thread.sendMessage("Player " + (i + 1) + " is the winner.");
				}
			}
			thread.interrupt();
			thread.lock.lock();
		}
		startNewGame();
		
	}
	
	public void broadcast(String message, ServerThread st)
	{
		if (message == "wait for 3")
		{
			System.out.println("Waiting for player 3.");
			serverThreads.get(0).sendMessage("Waiting for player 3.");
			serverThreads.get(1).sendMessage("Waiting for player 3.");
		}
		else if (message != null)
		{
			System.out.println(message);
			for (ServerThread threads : serverThreads)
			{
				if (st != threads)
				{
					threads.sendMessage(message);
				}
			}
		}
	}
	
	public void signalCLient(Lock lock)
	{
		Condition nextCondition = null;
		Lock nextLock = null;
		for (int i = 0; i < locks.size(); ++i)
		{
			if (locks.get(i) == lock)
			{
				if (i == locks.size() - 1)
				{
					nextLock = locks.get(0);
					nextLock.lock();
					nextCondition = conditions.get(0);
					nextCondition.signal();
					nextLock.unlock();
					break;
				}
				else
				{
					nextLock = locks.get(i + 1);
					nextLock.lock();
					nextCondition = conditions.get(i + 1);
					nextCondition.signal();
					nextLock.unlock();
					break;
				}
			}
		}
		
	}
	
	public static void main(String[] args)
	{
		ChatRoom cr = new ChatRoom(3456);
	}
}