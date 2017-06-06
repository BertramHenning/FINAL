package controller;

import java.text.DecimalFormat;
//import java.util.regex.Pattern;

//import org.omg.Messaging.SyncScopeHelper;


//import com.sun.xml.internal.ws.resources.SenderMessages;

import socket.ISocketController;
import socket.ISocketObserver;
import socket.SocketInMessage;
import socket.SocketOutMessage;
import weight.IWeightInterfaceController;
import weight.IWeightInterfaceObserver;
import weight.KeyPress;
/**
 * MainController - integrating input from socket and ui. Implements ISocketObserver and IUIObserver to handle this.
 * @author Christian Budtz
 * @version 0.1 2017-01-24
 *
 */
public class MainController implements IMainController, ISocketObserver, IWeightInterfaceObserver {

	private ISocketController socketHandler;
	private IWeightInterfaceController weightController;
	private KeyState keyState = KeyState.K1;
	private Double weight = 0.000;
	private Double totalWeight = 0.000;
	private String currDisplay = "";
	private boolean rm20 = false;
	DecimalFormat df = new DecimalFormat("#0.0000");

	public MainController(ISocketController socketHandler, IWeightInterfaceController weightInterfaceController) {
		this.init(socketHandler, weightInterfaceController);
	}

	@Override
	public void init(ISocketController socketHandler, IWeightInterfaceController weightInterfaceController) {
		this.socketHandler = socketHandler;
		this.weightController = weightInterfaceController;
	}

	@Override
	public void start() {
		if (socketHandler != null && weightController != null){
			//Makes this controller interested in messages from the socket
			socketHandler.registerObserver(this);
			//Starts socketHandler in own thread
			new Thread(socketHandler).start();
			//TODO set up weightController - Look above for inspiration (Keep it simple ;))
			weightController.registerObserver(this);
			new Thread(weightController).start();
			

		} else {
			System.err.println("No controllers injected!");
		}
	}

	//Listening for socket input
	@Override
	public void notify(SocketInMessage message) {
		switch (message.getType()) {
		case B:
			//Checking if command is valid.
			if (checkB(message.getMessage())){
				//Changing weight's screen to match new value.
				String tempB = message.getMessage() + " kg";
				weightController.showMessagePrimaryDisplay(tempB);
				System.out.println(tempB);
				this.notifyWeightChange(Double.parseDouble(message.getMessage()));
				socketHandler.sendMessage(new SocketOutMessage("B A \r\n"));
				
			} else{
				// If command is not a double value ES will be returned.
				socketHandler.sendMessage(new SocketOutMessage("ES\r\n"));
		}
			break;
			
		case D:			
			weightController.showMessagePrimaryDisplay(message.getMessage()); 
			socketHandler.sendMessage(new SocketOutMessage("D A\r\n"));
			break;
			
		case Q:
			socketHandler.sendMessage(new SocketOutMessage("Closing...\r\n"));
			System.exit(0);
			break;
			
		case RM204:
			weightController.showMessageSecondaryDisplay(message.getMessage());
			socketHandler.sendMessage(new SocketOutMessage("RM20 B\r\n"));
			
			
			
			break;
			
		case RM208:
			weightController.showMessageSecondaryDisplay(message.getMessage());
			socketHandler.sendMessage(new SocketOutMessage("RM20 B\r\n"));
			
			currDisplay = "";
			rm20 = true;
			
			break;
			
		case S:
			socketHandler.sendMessage(new SocketOutMessage("S S " + weight.toString()+" kg \r\n"));
			break;
			
		case T:

			socketHandler.sendMessage(new SocketOutMessage("T S " + weight + " kg\r\n"));
			
			weight = 0.0;
			weightController.showMessagePrimaryDisplay(weight + " kg");
			break;
		case DW:
			weightController.showMessagePrimaryDisplay(df.format(weight)
					.toString().replace(",", ".") + " kg"); 
			socketHandler.sendMessage(new SocketOutMessage("DW A \r\n"));
			break;
			
			
		case K:
			handleKMessage(message);
			break;
			
		case P111:
			if(message.getMessage().length() > 30){
				socketHandler.sendMessage(new SocketOutMessage("ES\r\n"));
				break;
			}
			weightController.showMessageSecondaryDisplay(message.getMessage()+"");
			socketHandler.sendMessage(new SocketOutMessage("P111 A\r\n"));
			
			
			
			break;
			
		case def:
			socketHandler.sendMessage(new SocketOutMessage("ES\r\n"));
			break;

		}

	}

	private void handleKMessage(SocketInMessage message) {
		System.out.println(message.getMessage());
		switch (message.getMessage()) {
		case "1" :
			this.keyState = KeyState.K1;
			break;
		case "2" :
			this.keyState = KeyState.K2;
			break;
		case "3" :
			this.keyState = KeyState.K3;
			break;
		case "4" :
			this.keyState = KeyState.K4;
			break;
		default:
			socketHandler.sendMessage(new SocketOutMessage("ES\n\r"));
			break;
		}
	}
	//Listening for UI input
	@Override
	public void notifyKeyPress(KeyPress keyPress) {
		//TODO implement logic for handling input from ui
		switch (keyPress.getType()) {
		case SOFTBUTTON:
			System.out.println(keyPress.getCharacter() + " +- " + keyPress.getKeyNumber()+ "    " + keyPress.getType());
			break;
		case TARA:
			if (!rm20) {
				weight = 0.0;
				weightController.showMessagePrimaryDisplay(weight + " kg");
			}
			System.out.println(keyPress.getCharacter() + " +- " + keyPress.getKeyNumber());
			break;
		case TEXT:
			if (rm20) {
				char bogstav = keyPress.getCharacter();
				currDisplay += bogstav;
				if (currDisplay.matches(".*[a-z].*")) {
					weightController.showMessagePrimaryDisplay(currDisplay);
				} else {
					weightController.showMessagePrimaryDisplay(currDisplay + "");
				}
			}
			System.out.println(keyPress.getCharacter() + " +- " + keyPress.getKeyNumber());
			break;
		case ZERO:
			if (!rm20) {
				weight = 0.0;
				totalWeight = 0.0;
				weightController.showMessagePrimaryDisplay(weight + " kg");
			}
			System.out.println(keyPress.getCharacter() + " +- " + keyPress.getKeyNumber());
			break;
		case C:
			if (!rm20) {
				currDisplay = "";
				weightController.showMessagePrimaryDisplay(weight.toString());
				weightController.showMessageSecondaryDisplay("");
			}
			System.out.println(keyPress.getCharacter() + " +- " + keyPress.getKeyNumber()+ "    " + keyPress.getType());
			break;
		case EXIT:
			System.out.println(keyPress.getCharacter() + " +- " + keyPress.getKeyNumber()+ "    " + keyPress.getType());
			System.exit(0);
			break;
		case SEND:
			if (rm20) {
				socketHandler.sendMessage(new SocketOutMessage("RM20 A "+ currDisplay + "\r\n"));
				currDisplay = "";
				weightController.showMessagePrimaryDisplay(df.format(weight).replace(",", ".") + " kg");
				rm20 = false;
			} else {

				if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {
					socketHandler.sendMessage(new SocketOutMessage("K A 3"));
				} else {
					socketHandler.sendMessage(new SocketOutMessage(" \r\n"));
				}
				weightController.showMessagePrimaryDisplay(weight + " kg");
			}
			System.out.println(keyPress.getCharacter() + " +- " + keyPress.getKeyNumber() + "    " + keyPress.getType());
			break;
		}

	}

	@Override
	public void notifyWeightChange(double newWeight) {
		double temp = totalWeight - weight;
		weight = newWeight - temp;
		totalWeight = newWeight;
		weightController.showMessagePrimaryDisplay(df.format(weight).replace(",", ".") + " kg");
		
	}
	
	public boolean checkB(String str) {
		try {
			if (Double.parseDouble(str) <= 6 && str.length() <= 6 && str.length() >= 1) {
				if (str.contains(".") && str.length() >= 3) {
					return true;
				}
			}
			return false;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	
	
}
