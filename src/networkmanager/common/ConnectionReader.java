package networkmanager.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import configurationutil.type.Table;
import networkmanager.common.security.EncryptionManager;



public class ConnectionReader implements Runnable {
	
	public static int REFRESH_RATE = 1000;
	
	private final ArrayList<Table> senderQueue = new ArrayList<Table>();
	
	private final BufferedReader reader;
	private final PrintWriter writer;
	
	private final EncryptionManager manager;
	
	private final Socket socket;
	private final INetworkSide parent;
	
	private volatile boolean initialized = false;
	
	private volatile boolean closer = false;
	
	public ConnectionReader(Socket socket, INetworkSide side) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		this.socket = socket;
		this.parent = side;
		this.manager = new EncryptionManager();
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) { 
			System.err.println("Could not initialize ConnectionReader to: " + socket.getInetAddress().toString());
			e.printStackTrace();
			try {
				System.err.println("Terminating connection...");
				socket.close();
			} catch (IOException e1) {
				System.err.println("Could not close bugged-out socket to: " + socket.getInetAddress().toString());
				e1.printStackTrace();
			}
			throw new IOException();
		}
		
		new Thread(this).start();
		Thread.currentThread().setName("[ConnectionReader to " + socket.toString() + "Thread]");
	}
	
	public void stopConnection() {
		closer = true;
	}
	
	public void flushQueue() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		for(Table t : this.senderQueue) {
			for(String line : t.toString().split(Pattern.quote("\n"))) {
				writer.println(manager.encrypt(line));
			}
			writer.println("ETM");
			writer.flush();
			if(writer.checkError()) {
				System.err.println("Error writing to socket. Unfortiounately, PrintWriters do not support outputting the Error that happened, so your guess is as good as mine. Chances are it's  one-time error!");
			}
		}
		this.senderQueue.clear();
	}
	
	public boolean handshake() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		System.out.println("[NetworkManager] [Connection from local port " + socket.getLocalPort() + " ] Establishing connection, handshake...");
		String time = Long.toString(System.nanoTime());
		
		
		writer.println(manager.getPublicKeyAsString());
		writer.flush();
		String read = reader.readLine();
				
		manager.setReceiverFromString(read);
		
		writer.println(time);
		writer.flush();
		String readTime = reader.readLine();
		
		writer.println(readTime);
		writer.flush();
		String readReturnedTime = reader.readLine();
		
		System.out.println("[NetworkManager] [Connection from local port " + socket.getLocalPort() + " ] Handshake complete, checking if handshake was successful...");
		
		if(time.equals(readReturnedTime)) return true;
		
		return false;
	}
	
	@Override
	public void run() {
		
		try {
			if(!this.handshake()) throw new Exception("Handshake failed.");
			System.out.println("[NetworkManager] [Connection from local port " + socket.getLocalPort() + " ] Handshake successful, any further communication is encrypted.");
			initialized = true;
			this.flushQueue();
		} catch(Exception e) {
			System.err.println("[NetworkManager] [Connection from local port " + socket.getLocalPort() + " ] Handshake failed!");
			e.printStackTrace();
			return;
		}
		
		String current = "";
		while(!socket.isClosed()) {
			
			String readLine = null;
			try {
				readLine = reader.readLine();
				if(!readLine.equals("ETM")) {
					readLine = manager.decrypt(readLine);
				}
			} catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e1) {
				e1.printStackTrace();
			}
			
			if(readLine.equals("ETM")) {
				try {
					parent.process(Table.fromString(current));
					current = "";
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			} else {
				current = current + readLine + "\n";
			}
		
			if(closer) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return;
	}
	
	public void sendQueued(Table table) {
		this.senderQueue.add(table);
	}
	
	public void send(Table table) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		if(!initialized) {
			this.senderQueue.add(table);
		} else {
			for(String line : table.toString().split(Pattern.quote("\n"))) {
				writer.println(manager.encrypt(line));
			}
			writer.println("ETM");
			writer.flush();
			if(writer.checkError()) {
				System.err.println("Error writing to socket. Unfortiounately, PrintWriters do not support outputting the Error that happened, so your guess is as good as mine. Chances are it's  one-time error!");
			}
		}
	}
}
