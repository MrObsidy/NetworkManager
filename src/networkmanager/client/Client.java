package networkmanager.client;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.SocketFactory;

import configurationutil.type.Table;
import networkmanager.common.ConnectionReader;
import networkmanager.common.INetworkReceiver;
import networkmanager.common.INetworkSide;

public class Client implements INetworkSide {
	
	private final Socket socket;
	
	private final ArrayList<INetworkReceiver> receivers = new ArrayList<INetworkReceiver>();
	
	//Crappy workaround around ConcurrentModificationExceptions
	// {
	private final ArrayList<INetworkReceiver> addreceivers = new ArrayList<INetworkReceiver>();
	private final ArrayList<INetworkReceiver> removereceivers = new ArrayList<INetworkReceiver>();
	// }
	
	private final ConnectionReader reader;
	
	public Client(String host, int port) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		socket = SocketFactory.getDefault().createSocket(host, port);
		reader = new ConnectionReader(this.socket, this);
	}
	
	public void addReceiver(INetworkReceiver rec) {
		this.addreceivers.add(rec);
	}
	
	public void removeReceiver(INetworkReceiver rec) {
		this.removereceivers.add(rec);
	}
	
	public void send(Table table) {
		try {
			reader.send(table);
		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
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
}