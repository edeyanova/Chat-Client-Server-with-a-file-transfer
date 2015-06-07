import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

public class Server {
	private static int uniqueId;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;

	private ServerGUI sg;

	private SimpleDateFormat sdf;

	private int port;

	private boolean keepGoing;

	// server constructor that receive the port to listen to for connection as
	// parameter
	// in console
	public Server(int port) {
		this(port, null);
	}

	public Server(int port, ServerGUI sg) {

		this.sg = sg;

		this.port = port;

		sdf = new SimpleDateFormat("HH:mm:ss");

		al = new ArrayList<ClientThread>();

	}

	public void start() {
		keepGoing = true;

		try {

			ServerSocket serverSocket = new ServerSocket(port);

			while (keepGoing) {

				display("Server waiting for Clients on port " + port + ".");

				Socket socket = serverSocket.accept();

				if (!keepGoing)
					break;
				ClientThread t = new ClientThread(socket);
				boolean isUserInList = false;
				for (int i = 0; i < al.size(); i++) {
					if (al.get(i).username.equals(t.username)) {
						isUserInList = true;
						break;
					}
				}
				if (!isUserInList) {
					al.add(t);
					t.start();
				}
			}

			try {
				serverSocket.close();
				for (int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ioE) {

					}
				}
			} catch (Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}

		catch (IOException e) {
			String msg = sdf.format(new Date())
					+ " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}

	protected void stop() {
		keepGoing = false;

		try {
			new Socket("localhost", port);
		} catch (Exception e) {

		}
	}

	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		if (sg == null)
			System.out.println(time);
		else
			sg.appendEvent(time + "\n");
	}

	private synchronized void broadcast(String message) {

		String time = sdf.format(new Date());
		String messageLf = time + " " + message + "\n";

		if (sg == null)
			System.out.print(messageLf);
		else
			sg.appendRoom(messageLf);

		for (int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);

			if (!ct.writeMsg(messageLf)) {
				al.remove(i);
				display("Disconnected Client " + ct.username
						+ " removed from list.");
			}
		}
	}

	private synchronized void broadcast(ChatMessage message) {

		if (message.getType() == ChatMessage.MESSAGE) {
			String time = sdf.format(new Date());
			String messageLf = time + " " + message + "\n";

			if (sg == null)
				System.out.print(messageLf);
			else
				sg.appendRoom(messageLf);
		}

		for (int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);

			if (!ct.writeMessage(message)) {
				al.remove(i);
				display("Disconnected Client " + ct.username
						+ " removed from list.");
			}
		}
	}

	private synchronized void broadcast(byte[] file) {

		for (int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);

			if (!ct.writeFile(file)) {
				al.remove(i);
				display("Disconnected Client " + ct.username
						+ " removed from list.");
			}
		}
	}

	synchronized void remove(int id) {

		for (int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);

			if (ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}

	public static void main(String[] args) {

		int portNumber = 1500;
		switch (args.length) {
		case 1:
			try {
				portNumber = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java Server [portNumber]");
				return;
			}
		case 0:
			break;
		default:
			System.out.println("Usage is: > java Server [portNumber]");
			return;

		}

		Server server = new Server(portNumber);
		server.start();
	}

	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {

		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;

		int id;

		String username;

		ChatMessage cm;

		String date;

		ClientThread(Socket socket) {

			id = ++uniqueId;
			this.socket = socket;

			System.out
					.println("Thread trying to create Object Input/Output Streams");
			try {

				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());

				username = (String) sInput.readObject();
				display(username + " just connected.");

			} catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}

			catch (ClassNotFoundException e) {
			}
			date = new Date().toString() + "\n";
		}

		public void run() {

			boolean keepGoing = true;
			while (keepGoing) {

				try {
					cm = (ChatMessage) sInput.readObject();
				} catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;
				} catch (ClassNotFoundException e2) {
					break;
				}

				switch (cm.getType()) {

				case ChatMessage.MESSAGE:
					String message = cm.getMessage();
					broadcast(username + ": " + message);
					break;
				case ChatMessage.LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;
				case ChatMessage.WHOISIN:
					writeMsg("List of the users connected at "
							+ sdf.format(new Date()) + "\n");

					for (int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						writeMsg((i + 1) + ") " + ct.username + " since "
								+ ct.date);
					}
					break;
				case ChatMessage.SENDING_FILE:

					broadcast(cm);
					break;

				}
			}

			remove(id);
			close();
		}

		private void close() {

			try {
				if (sOutput != null)
					sOutput.close();
			} catch (Exception e) {
			}
			try {
				if (sInput != null)
					sInput.close();
			} catch (Exception e) {
			}
			;
			try {
				if (socket != null)
					socket.close();
			} catch (Exception e) {
			}
		}

		private boolean writeMsg(String msg) {

			if (!socket.isConnected()) {
				close();
				return false;
			}

			try {
				sOutput.writeObject(msg);
			}

			catch (IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}

		private boolean writeFile(byte[] file) {

			if (!socket.isConnected()) {
				close();
				return false;
			}

			try {
				sOutput.write(file);
			}

			catch (IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}

		private boolean writeMessage(ChatMessage message) {

			if (!socket.isConnected()) {
				close();
				return false;
			}

			try {
				sOutput.writeObject(message);
			}

			catch (IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}
}
