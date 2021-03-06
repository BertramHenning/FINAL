package socket;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import controller.MainController;
import socket.SocketInMessage.SocketMessageType;


public class SocketController implements ISocketController {
	Set<ISocketObserver> observers = new HashSet<ISocketObserver>();
	//TODO Maybe add some way to keep track of multiple connections?
	private BufferedReader inStream;
	private DataOutputStream outStream;
	private static int newPort = Port;


	@Override
	public void registerObserver(ISocketObserver observer) {
		observers.add(observer);
	}

	@Override
	public void unRegisterObserver(ISocketObserver observer) {
		observers.remove(observer);
	}

	@Override
	public void sendMessage(SocketOutMessage message) {
		if (outStream != null){
			try {
				outStream.writeBytes(message.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("Connection is closed. Message Not sent.");
		}
	}
	
	public static void changePort(int port){
		newPort = port;
	}

	@Override
	public void run() {
		//TODO some logic for listening to a socket //(Using try with resources for auto-close of socket)
		try (ServerSocket listeningSocket = new ServerSocket(newPort)){ 
			System.out.println("Server running on port: "+newPort);
			
			while (true){
				waitForConnections(listeningSocket); 	
			}		
		} catch (IOException e1) {
			// TODO Maybe notify MainController?
			MainController.class.notify();
			e1.printStackTrace();
		} 


	}

	private void waitForConnections(ServerSocket listeningSocket) {
		try {
			Socket activeSocket = listeningSocket.accept(); //Blocking call
			inStream = new BufferedReader(new InputStreamReader(activeSocket.getInputStream()));
			outStream = new DataOutputStream(activeSocket.getOutputStream());
			String inLine;
			//.readLine is a blocking call 
			//TODO How do you handle simultaneous input and output on socket?
			//TODO this only allows for one open connection - how would you handle multiple connections?
			
			while (true){
				inLine = inStream.readLine();
				System.out.println(inLine);
				if (inLine==null) break;
					if(inLine.contains("RM20")){
						inLine = inLine.replaceFirst(" ", "");
					}
					try{
				switch (inLine.split(" ")[0]) {
				case "RM208": // Display a message in the secondary display and wait for response
					//TODO implement logic for RM command
					notifyObservers(new SocketInMessage(SocketMessageType.RM208, inLine.split(" ")[1]));
					break;
				case "RM204":
					notifyObservers(new SocketInMessage(SocketMessageType.RM204, inLine.split(" ")[1]));
					break;
				case "D":// Display a message in the primary display
					//TODO Refactor to make sure that faulty messages doesn't break the system
					notifyObservers(new SocketInMessage(SocketMessageType.D, inLine.split(" ")[1])); 			
					break;
				case "DW": //Clear primary display
					notifyObservers(new SocketInMessage(SocketMessageType.DW, " "));
					break;
				case "P111": //Show something in secondary display
					String temp = inLine.replaceFirst(" ", "_---_");
					notifyObservers(new SocketInMessage(SocketMessageType.P111, temp.split("_---_")[1]));
					break;
				case "T": // Tare the weight
					notifyObservers(new SocketInMessage(SocketMessageType.T, inLine.split(" ")[0]));
					break;
				case "S": // Request the current load
					notifyObservers(new SocketInMessage(SocketMessageType.S, ""));
					break;
				case "K":
					if (inLine.split(" ").length > 1){
						notifyObservers(new SocketInMessage(SocketMessageType.K, inLine.split(" ")[1]));
					}
					break;
				case "B": // Set the load
					notifyObservers(new SocketInMessage(SocketMessageType.B, inLine.split(" ")[1]));
					break;
				case "Q": // Quit
					notifyObservers(new SocketInMessage(SocketMessageType.Q, "Shutting down..."));
					break;
				default:
					notifyObservers(new SocketInMessage(SocketMessageType.def, ""));
					break;
				}
					}catch(ArrayIndexOutOfBoundsException AOBE){
						sendMessage(new SocketOutMessage("ES\r\n"));
					}
			}
		} catch (IOException e) {
			//TODO maybe notify mainController?
			MainController.class.notify();
			e.printStackTrace();
		}
	}

	private void notifyObservers(SocketInMessage message) {
		for (ISocketObserver socketObserver : observers) {
			socketObserver.notify(message);
		}
	}
}

