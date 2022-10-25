import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MusicServer {
	private final List<ObjectOutputStream> clientOutputStreams = new ArrayList<>();

	public static void main(String[] args) {
		new MusicServer().go();
	}
	
	public void go() {
		try (ServerSocket serverSocket = new ServerSocket(4242)) {
			ExecutorService threadPool = Executors.newCachedThreadPool();
			
			while(!serverSocket.isClosed()) {
				Socket clientSocket = serverSocket.accept();
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				clientOutputStreams.add(out);
				
				ClientHandler clientHandler = new ClientHandler(clientSocket);
				threadPool.execute(clientHandler);
				System.out.println("Got a Connection");
				//serverSocket.close();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void tellEveryone(Object usernameAndMessage, Object beatSequence) {
		for(ObjectOutputStream clientOutputStream : clientOutputStreams) {
			try {
				clientOutputStream.writeObject(usernameAndMessage);
				clientOutputStream.writeObject(beatSequence);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class ClientHandler implements Runnable{
		private ObjectInputStream in;
		
		public ClientHandler(Socket socket) {
			try {
				in = new ObjectInputStream(socket.getInputStream());
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			Object usernameAndMessage;
			Object beatSequence;
			try {
				while((usernameAndMessage = in.readObject()) != null) {
					beatSequence = in.readObject();
					
					System.out.println("read two objects");
					tellEveryone(usernameAndMessage, beatSequence);
				}
			}
			catch(IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

}
