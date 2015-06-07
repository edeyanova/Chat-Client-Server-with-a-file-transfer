import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.eclipse.swt.widgets.DirectoryDialog;

public class Client {

	private ObjectInputStream sInput; // to read from the socket
	private ObjectOutputStream sOutput; // to write on the socket
	protected Socket socket;

	private ClientGUI cg;

	private String server, username;
	private int port;

	// This is a constructor called by console mode
	Client(String server, int port, String username) {
		this(server, port, username, null);
	}

	// Constructor used from GUI
	Client(String server, int port, String username, ClientGUI cg) {
		this.server = server;
		this.port = port;
		this.username = username;
		this.cg = cg;
	}

	public boolean start() {

		try {
			socket = new Socket(server, port);
		}

		catch (Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}

		String msg = "Connection accepted " + socket.getInetAddress() + ":"
				+ socket.getPort();
		display(msg);

		try {
			sInput = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		new ListenFromServer().start();

		try {
			sOutput.writeObject(username);
		} catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}

		return true;
	}

	// To send a message to the console or the GUI
	private void display(String msg) {
		if (cg == null)
			System.out.println(msg);
		else
			cg.append(msg + "\n"); // append to the ClientGUI JTextArea
	}

	// To send a message to the server

	void sendMessage(ChatMessage msg) {
		try {
			sOutput.writeObject(msg);
		} catch (IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	private void disconnect() {
		try {
			if (sInput != null)
				sInput.close();
		} catch (Exception e) {
		}
		try {
			if (sOutput != null)
				sOutput.close();
		} catch (Exception e) {
		}
		try {
			if (socket != null)
				socket.close();
		} catch (Exception e) {
		}

		if (cg != null)
			cg.connectionFailed();

	}

	public static void main(String[] args) {

		int portNumber = 1500;
		String serverAddress = "localhost";
		String userName = "Anonymous";

		switch (args.length) {

		case 3:
			serverAddress = args[2];

		case 2:
			try {
				portNumber = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out
						.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
				return;
			}

		case 1:
			userName = args[0];

		case 0:
			break;

		default:
			System.out
					.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
			return;
		}

		Client client = new Client(serverAddress, portNumber, userName);

		if (!client.start())
			return;

		Scanner scan = new Scanner(System.in);

		while (true) {
			System.out.print("> ");

			String msg = scan.nextLine();

			if (msg.equalsIgnoreCase("LOGOUT")) {
				client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));

				break;
			}

			else if (msg.equalsIgnoreCase("WHOISIN")) {
				client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
			} else if (msg.equalsIgnoreCase("MESSAGE")) {
				client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
			}

		}

		client.disconnect();
	}

	// a class that waits for the message from the server and append them to the
	// JTextArea
	// if we have a GUI or simply System.out.println() it in console mode

	class ListenFromServer extends Thread {

		public void run() {
			while (true) {
				FileOutputStream out = null;
				try {

					Object object = sInput.readObject();

					if (object instanceof String) {
						String msg = (String) object;

						if (cg == null) {

							System.out.println(msg);
							System.out.print("> ");
						} else {
							cg.append(msg);
						}
					} else if (object instanceof ChatMessage) {
						ChatMessage message = (ChatMessage) object;
						byte[] file = message.getFile();
						Path path = Paths.get(username);
						if (!Files.exists(path)) {
							Files.createDirectory(path);
						}
						out = new FileOutputStream(new File(username + "\\"
								+ message.getMessage()));
						out.write(file);
						out.flush();

					}
				} catch (IOException e) {
					display("Server has close the connection: " + e);
					if (cg != null)
						cg.connectionFailed();
					break;
				}

				catch (ClassNotFoundException e2) {

				}

			}
		}
	}
}
