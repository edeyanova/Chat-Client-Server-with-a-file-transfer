import java.io.Serializable;

public class ChatMessage implements Serializable {

	protected static final long serialVersionUID = 1112122200L;

	static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2;
	static final int SENDING_FILE = 3;

	private int type;
	private String message;
	private byte[] file;

	ChatMessage(int type, String message) {
		this.type = type;
		this.message = message;
	}

	public ChatMessage(byte[] buffer, String fileName) {

		file = buffer;
		type = SENDING_FILE;
		message = fileName;
	}

	byte[] getFile() {
		return file;
	}

	int getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}
