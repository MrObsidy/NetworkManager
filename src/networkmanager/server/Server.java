package networkmanager.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ServerSocketFactory;

import configurationutil.type.Table;
import networkmanager.common.ConnectionReader;
import networkmanager.common.INetworkReceiver;
import networkmanager.common.INetworkSide;

public class Server implements INetworkSide, Runnable {
	
	private final ServerSocket socket;
	private final ArrayList<ConnectionReader> clients = new ArrayList<ConnectionReader>();
	private final ArrayList<INetworkReceiver> receivers = new ArrayList<INetworkReceiver>();
	
	//Crappy workaround around ConcurrentModificationExceptions
	// {
	private final ArrayList<ConnectionReader> addclients = new ArrayList<ConnectionReader>();
	private final ArrayList<INetworkReceiver> addreceivers = new ArrayList<INetworkReceiver>();
	
	private final ArrayList<ConnectionReader> removeclients = new ArrayList<ConnectionReader>();
	private final ArrayList<INetworkReceiver> removereceivers = new ArrayList<INetworkReceiver>();
	// }
	
	private volatile boolean shutdown = false;
	
	public Server(int port) throws IOException {
		socket = ServerSocketFactory.getDefault().createServerSocket(port);

		new Thread(this).start();
	}
	
	@Override
	public void process(Table table) {
		if(addreceivers.size() != 0) {
			for(INetworkReceiver rec : addreceivers) {
				receivers.add(rec);
			}
		}
		if(removereceivers.size() != 0) {
			for(INetworkReceiver rec : removereceivers) {
				receivers.remove(rec);
			}
		}
		for(INetworkReceiver rec : receivers) {
			rec.receive(table);
		}
	}
	
	public void send(Table table) {
		if(addclients.size() != 0) {
			for(ConnectionReader rec : addclients) {
				clients.add(rec);
			}
		}
		if(removeclients.size() != 0) {
			for(ConnectionReader rec : removeclients) {
				clients.remove(rec);
			}
		}
		for(ConnectionReader conn : clients) {
			try {
				conn.send(table);
			} catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
				e.printStackTrace();
			}
		}
	}

	public void addReceiver(INetworkReceiver rec) {
		this.addreceivers.add(rec);
	}
	
	public void removeReceiver(INetworkReceiver rec) {
		this.removereceivers.add(rec);
	}
	
	public void removeReader(ConnectionReader read) {
		this.removeclients.add(read);
	}
	
	@Override
	public void run() {
		while(!shutdown) {
			synchronized(this) {
				Socket clSock = null;
				try {
					clSock = (Socket) socket.accept();
					addclients.add(new ConnectionReader(clSock, this));
				} catch (IOException | InvalidKeyException | NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void shutdown() {
		shutdown = true;
	}
}
