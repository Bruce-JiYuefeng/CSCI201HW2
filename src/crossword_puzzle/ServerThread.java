package crossword_puzzle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ServerThread extends Thread
{
	private Socket s;
	private static boolean gameEnd;
	public int score;
	public Lock lock;
	private Condition condition;
	private PrintWriter pw;
	protected BufferedReader br;
	private ChatRoom cr;
	protected int index;
	
	public ServerThread(Socket s, ChatRoom cr, Lock lock, Condition condition, int index)
	{
		try
		{
			this.s = s;
			gameEnd = false;
			score = 0;
			this.index = index;
			this.cr = cr;
			this.lock = lock;
			this.condition = condition;
			pw = new PrintWriter(s.getOutputStream());
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			this.start();
		}
		catch (IOException ioe)
		{
			System.out.println("ioe in ServerThread constructor: " + ioe.getMessage());
		}
	}
	
	public void sendMessage(String message)
	{
		pw.println(message);
		pw.flush();
	}
	
	public void sendBoard()
	{
		for (int j = BoardRenderer.lowerbound; j <= BoardRenderer.upperbound; ++j)
		{
			String row = "";
			for (int i = BoardRenderer.righterbound; i <= BoardRenderer.lefterbound; ++i)
			{
				if (BoardRenderer.grids[i][j].letter == 0)
				{
					row += "    ";
				}
				else
				{
					if (BoardRenderer.grids[i][j].index1 != -1)
					{
						if (BoardRenderer.answers[BoardRenderer.grids[i][j].index1].num >= 10)
						{
							row += BoardRenderer.answers[BoardRenderer.grids[i][j].index1].num;
						}
						else
						{
							row += " ";
							row += BoardRenderer.answers[BoardRenderer.grids[i][j].index1].num;
						}
						
					}
					else
					{
						row += "  ";
					}
					if (BoardRenderer.grids[i][j].answered == true)
					{
						row += Character.toString(BoardRenderer.grids[i][j].letter);
					}
					else
					{
						row += "_";
					}
					row+=" ";
				}
			}
			pw.println(row);
			pw.flush();
			pw.println();
			pw.flush();
		}
		boolean acrossFlag = false;
		boolean downFlag = false;
		for (int i = 0; i < BoardRenderer.totalSize; ++i)
		{
			if (BoardRenderer.answers[i].second == true && BoardRenderer.answers[i].answered == false)
			{
				acrossFlag = true;
				break;
			}
		}
		for (int i = 0; i < BoardRenderer.totalSize; ++i)
		{
			if (BoardRenderer.answers[i].second == false && BoardRenderer.answers[i].answered == false)
			{
				downFlag = true;
				break;
			}
		}
		if (acrossFlag == false && downFlag == false)
		{
			gameEnd = true;
			pw.println("Across");
			pw.flush();
			for (int i = 0; i < BoardRenderer.acrossSize; ++i)
			{
				String line = "";
				line += BoardRenderer.acrossNumber.get(i);
				line += " ";
				line += BoardRenderer.acrossQuestion.get(i);
				pw.println(line);
				pw.flush();
			}
			pw.println("Down");
			pw.flush();
			for (int i = 0; i < BoardRenderer.downSize; ++i)
			{
				String line = "";
				line += BoardRenderer.downNumber.get(i);
				line += " ";
				line += BoardRenderer.downQuestion.get(i);
				pw.println(line);
				pw.flush();
			}
		}
		else
		{
			if (acrossFlag == true)
			{
				pw.println("Across");
				pw.flush();
				for (int i = 0; i < BoardRenderer.acrossSize; ++i)
				{
					for (int j = 0; j < BoardRenderer.totalSize; ++j)
					{
						if (BoardRenderer.answers[j].num == BoardRenderer.acrossNumber.get(i) && BoardRenderer.answers[j].second == true
								&& BoardRenderer.answers[j].answered == false)
						{
							String line = "";
							line += BoardRenderer.acrossNumber.get(i);
							line += " ";
							line += BoardRenderer.acrossQuestion.get(i);
							pw.println(line);
							pw.flush();
						}
					}
				}
			}
			if (downFlag == true)
			{
				pw.println("Down");
				pw.flush();
				for (int i = 0; i < BoardRenderer.downSize; ++i)
				{
					for (int j = 0; j < BoardRenderer.totalSize; ++j)
					{
						if (BoardRenderer.answers[j].num == BoardRenderer.downNumber.get(i) && BoardRenderer.answers[j].second == false
								&& BoardRenderer.answers[j].answered == false)
						{
							String line = "";
							line += BoardRenderer.downNumber.get(i);
							line += " ";
							line += BoardRenderer.downQuestion.get(i);
							pw.println(line);
							pw.flush();
						}
					}
				}
			}
			
		}
		
	}
	
	public void run()
	{
		boolean exit = false;
		while (!exit)
		{
			
			try
			{
				lock.lock();
				if (index == 1)
				{
					sendMessage("There is a game waiting for you.");
					sendMessage("Player 1 has already joined.");
					if (ChatRoom.numOfTotalPlayers == 3)
					{
						cr.broadcast("wait for 3", this);
					}
				}
				else if (index == 2)
				{
					sendMessage("There is a game waiting for you.");
					sendMessage("Player 1 has already joined.");
					sendMessage("Player 2 has already joined.");
				}
				condition.await();
				String line;
				while (true)
				{
					cr.sendBoard();
					if (gameEnd == true)
					{
						cr.sendFinalScore(this);
						lock.unlock();
						exit = true;
						sendMessage("<Client terminates>");
						s.shutdownInput();
						s.shutdownOutput();
						break;
					}
					cr.broadcast("Player " + (index + 1) + "'s turn.", this);
					boolean validOption1 = false;
					boolean validOption2 = false;
					boolean chooseAcross = true;
					int number = 0;
					while (!validOption1)
					{
						sendMessage("Would you like to answer a question across (a) or down (d)? ");
						line = br.readLine();
						if (line == null) {
							sendMessage("That is not a valid option.");
							continue;
						}
						line = line.trim().toLowerCase();
						if (line.equals("a"))
						{
							boolean flag = false;
							for (int i = 0; i < BoardRenderer.totalSize; ++i)
							{
								if (BoardRenderer.answers[i].second == true && BoardRenderer.answers[i].answered == false)
								{
									flag = true;
									break;
								}
							}
							if (!flag)
							{
								sendMessage("That is not a valid option.");
								continue;
							}
							else
							{
								validOption1 = true;
								chooseAcross = true;
							}
						}
						else if (line.equals("d"))
						{
							boolean flag = false;
							for (int i = 0; i < BoardRenderer.totalSize; ++i)
							{
								if (BoardRenderer.answers[i].second == false && BoardRenderer.answers[i].answered == false)
								{
									flag = true;
									break;
								}
							}
							if (!flag)
							{
								sendMessage("That is not a valid option.");
								continue;
							}
							else
							{
								validOption1 = true;
								chooseAcross = false;
							}
						}
						else
						{
							sendMessage("That is not a valid option.");
						}
					}
					while (validOption2 == false)
					{
						sendMessage("Which number? ");
						line = br.readLine();
						if (line == null) {
							sendMessage("That is not a valid option.");
							continue;
						}
						line = line.trim();
						try
						{
							number = Integer.parseInt(line);
						}
						catch (Exception e)
						{
							sendMessage("That is not a valid option.");
							continue;
						}
						if (chooseAcross == true)
						{
							boolean flag = false;
							for (int i = 0; i < BoardRenderer.totalSize; ++i)
							{
								if (BoardRenderer.answers[i].second == true && BoardRenderer.answers[i].num == number
										&& BoardRenderer.answers[i].answered == false)
								{
									flag = true;
									break;
								}
							}
							if (flag == false)
							{
								sendMessage("That is not a valid option.");
								continue;
							}
							else
							{
								validOption2 = true;
							}
						}
						else
						{
							boolean flag = false;
							for (int i = 0; i < BoardRenderer.totalSize; ++i)
							{
								if (BoardRenderer.answers[i].second == false && BoardRenderer.answers[i].num == number
										&& BoardRenderer.answers[i].answered == false)
								{
									flag = true;
									break;
								}
							}
							if (flag == false)
							{
								sendMessage("That is not a valid option.");
								continue;
							}
							else
							{
								validOption2 = true;
							}
						}
					}
					if (chooseAcross == true)
					{
						sendMessage("What is your guess for " + number + " across?");
					}
					else
					{
						sendMessage("What is your guess for " + number + " down?");
					}
					
					line = br.readLine();
					line = line.trim();
					line = line.toLowerCase();
					if (chooseAcross == true)
					{
						cr.broadcast("Player " + (index + 1) + " guessed \"" + line + "\" for " + number + " across.",
								this);
					}
					else
					{
						cr.broadcast("Player " + (index + 1) + " guessed \"" + line + "\" for " + number + " down.",
								this);
					}
					

					if (chooseAcross == true)
					{
						for (int i = 0; i < BoardRenderer.acrossSize; ++i)
						{
							if (BoardRenderer.acrossNumber.get(i) == number)
							{
								if (BoardRenderer.acrossAnswer.get(i).equals(line))
								{
									++score;
									cr.broadcast("That is correct.", this);
									sendMessage("That is correct.");
									for (int j = 0; j < BoardRenderer.totalSize; ++j)
									{
										if (BoardRenderer.answers[j].second == true && BoardRenderer.answers[j].num == number)
										{
											BoardRenderer.answers[j].answered = true;
											for (int k = BoardRenderer.answers[j].x; k < (BoardRenderer.answers[j].x
													+ BoardRenderer.answers[j].length); ++k)
											{
												BoardRenderer.grids[k][BoardRenderer.answers[j].y].answered = true;
											}
										}
									}
									break;
								}
								else
								{
									cr.broadcast("That is incorrect.", this);
									sendMessage("That is incorrect.");
									if (ChatRoom.numOfTotalPlayers != 1)
									{
										cr.signalCLient(lock);
										condition.await();
									}
									break;
								}
							}
						}
					}
					else
					{
						for (int i = 0; i < BoardRenderer.downSize; ++i)
						{
							if (BoardRenderer.downNumber.get(i) == number)
							{
								if (BoardRenderer.downAnswer.get(i).equals(line))
								{
									++score;
									cr.broadcast("That is correct.", this);
									sendMessage("That is correct.");
									for (int j = 0; j < BoardRenderer.totalSize; ++j)
									{
										if (BoardRenderer.answers[j].second == false && BoardRenderer.answers[j].num == number)
										{
											BoardRenderer.answers[j].answered = true;
											for (int k = BoardRenderer.answers[j].y; k < (BoardRenderer.answers[j].y
													+ BoardRenderer.answers[j].length); ++k)
											{
												BoardRenderer.grids[BoardRenderer.answers[j].x][k].answered = true;
											}
										}
									}
									break;
								}
								else
								{
									cr.broadcast("That is incorrect.", this);
									sendMessage("That is incorrect.");
									if (ChatRoom.numOfTotalPlayers != 1)
									{
										cr.signalCLient(lock);
										condition.await();
									}
									break;
								}
							}
						}
					}

				}
			}
			catch (InterruptedException ie)
			{
				
			}
			catch (IOException ioe)
			{
				
			}
			finally
			{
			}
		}
	}
}