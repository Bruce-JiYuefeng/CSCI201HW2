package crossword_puzzle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient extends Thread
{
	
	private BufferedReader br;
	private PrintWriter pw;
	
	public ChatClient(String hostname, int port)
	{
		try
		{
			Socket s = new Socket(hostname, port);
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			pw = new PrintWriter(s.getOutputStream());
			this.start();
			Scanner scan = new Scanner(System.in);
			while (true)
			{
				String line = scan.nextLine();
				pw.println(line);
				pw.flush();
			}
			
		} catch (IOException ioe)
		{
			System.out.println("ioe in ChatClient constructor: " + ioe.getMessage());
		}
	}
	
	public void run()
	{
		try
		{
			while (true)
			{
				String line = br.readLine();
				String inputLine = line;
				switch (inputLine) {
				    case "How many players will there be? ":
				        System.out.print(inputLine);
				        break;
				    case "Would you like to answer a question across (a) or down (d)? ":
				        System.out.print(inputLine);
				        break;
				    default:
				        System.out.println(inputLine);
				        break;
				}
				if (ChatRoom.gameEnd == true) {
				    break;
				}
			}
		} catch (IOException ioe)
		{
			System.out.println("ioe in ChatClient.run(): " + ioe.getMessage());
		}
	}
	
	public static void main(String[] args)
	{
		System.out.println("Welcome to 201 Crossword!");
		System.out.print("Enter the server hostname: ");
		Scanner scan = new Scanner(System.in);
		String hostname = scan.nextLine();
		System.out.print("Enter the server port: ");
		int port = scan.nextInt();
		try
		{
			ChatClient cc = new ChatClient(hostname, port);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		scan.close();
	}
}