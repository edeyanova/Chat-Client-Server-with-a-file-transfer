import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import javax.swing.JComboBox;

public class ClientGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JLabel label;
	private JTextField tf;
	private JTextField tfServer, tfPort;
	private JButton login, logout, whoIsIn, SendFile;
	private JTextArea ta;
	private boolean connected;
	private Client client;
	private int defaultPort;
	private String defaultHost;

	ClientGUI(String host, int port) {

		super("Chat Client");
		defaultPort = port;
		defaultHost = host;

		JPanel northPanel = new JPanel(new GridLayout(3, 1));
		JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));
		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		serverAndPort.add(new JLabel("Server Address:  "));
		serverAndPort.add(tfServer);
		serverAndPort.add(new JLabel("Port Number:  "));
		serverAndPort.add(tfPort);
		serverAndPort.add(new JLabel(""));
		northPanel.add(serverAndPort);

		label = new JLabel("Enter your username below", SwingConstants.LEFT);
		northPanel.add(label);
		tf = new JTextField("Anonymous");
		tf.setHorizontalAlignment(SwingConstants.LEFT);
		tf.setBackground(Color.WHITE);

		northPanel.add(tf);
		tf.requestFocus();
		getContentPane().add(northPanel, BorderLayout.NORTH);

		ta = new JTextArea("Welcome to the Chat room\n", 80, 80);
		JPanel centerPanel = new JPanel(new GridLayout(1, 1));
		JScrollPane scrollPane = new JScrollPane(ta);
		centerPanel.add(scrollPane);
		ta.setEditable(false);
		getContentPane().add(centerPanel, BorderLayout.CENTER);

		login = new JButton("Login");
		login.addActionListener(this);
		logout = new JButton("Logout");
		logout.addActionListener(this);
		logout.setEnabled(false);
		whoIsIn = new JButton("Who is in");
		whoIsIn.addActionListener(this);
		whoIsIn.setEnabled(false);
		SendFile = new JButton("Send File");
		SendFile.setEnabled(false);
		SendFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (connected) {
					JFileChooser chooser = new JFileChooser();
					int returnVal = chooser.showOpenDialog(SendFile);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File myFile = new File(chooser.getSelectedFile()
								.getAbsolutePath());
						String fileName = chooser.getSelectedFile()
								.getAbsolutePath();
						try {
							FileInputStream file = new FileInputStream(fileName);
							byte[] buffer = new byte[(int) myFile.length()];

							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							int readNum;
							while ((readNum = file.read(buffer)) != -1) {
								bos.write(buffer, 0, readNum);

							}
							byte[] bytes = bos.toByteArray();

							client.sendMessage(new ChatMessage(bytes, chooser
									.getSelectedFile().getName()));
						} catch (FileNotFoundException e) {

							e.printStackTrace();
						} catch (IOException e) {

							e.printStackTrace();
						}

					}
				}
			}
		});

		JPanel southPanel = new JPanel();
		southPanel.add(login);
		southPanel.add(logout);
		southPanel.add(whoIsIn);
		southPanel.add(SendFile);
		getContentPane().add(southPanel, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);

	}

	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}

	void connectionFailed() {
		login.setEnabled(true);
		logout.setEnabled(false);
		whoIsIn.setEnabled(false);
		SendFile.setEnabled(false);
		label.setText("Enter your username below");
		tf.setText("Anonymous");

		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);

		tfServer.setEditable(false);
		tfPort.setEditable(false);

		tf.removeActionListener(this);
		connected = false;
	}

	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == logout) {
			client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
			return;
		}

		if (o == whoIsIn) {
			client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
			return;
		}

		if (connected) {

			client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, tf
					.getText()));
			tf.setText("");
			return;
		}

		if (o == login) {

			String username = tf.getText().trim();
			if (username.length() == 0)
				return;
			String server = tfServer.getText().trim();
			if (server.length() == 0)
				return;

			String portNumber = tfPort.getText().trim();
			if (portNumber.length() == 0)
				return;
			int port = 0;
			try {
				port = Integer.parseInt(portNumber);
			} catch (Exception en) {
				return;
			}

			client = new Client(server, port, username, this);

			if (!client.start())
				return;
			tf.setText("");
			label.setText("Enter your message below");
			connected = true;

			login.setEnabled(false);

			logout.setEnabled(true);
			whoIsIn.setEnabled(true);

			tfServer.setEditable(false);
			tfPort.setEditable(false);
			SendFile.setEnabled(true);
			tf.addActionListener(this);

		}

	}

	public static void main(String[] args) {
		new ClientGUI("localhost", 1500);
	}

}
