//Import all the classes and interfaces that'll be used
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.sound.midi.*;
import java.util.*;
import java.util.Random;
import java.util.List;
import java.util.concurrent.*;

import static javax.sound.midi.ShortMessage.*;

//The main class
public class BeatBox {
	//Declare all the instance variables as private
	private final List<BeatInstrument> instruments;		//TO STORE THE INSTRUMENT INFO
	
	
	//for GUI setup
	private JFrame frame;
	private JFrame colorframe;
	
	//to make the actual beat patterns
	private ArrayList<JCheckBox> checkboxList;
	private Sequencer sequencer;
	private Sequence sequence;
	private Track track;
	
	//to display the incoming and outgoing messages
	private JList<String> incomingList;
	private JTextArea userMessage;
	private Vector<String> listVector = new Vector<>();
	private HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
	
	
	//for username and message's number
	private String username;
	private int nextNum;
	
	//for reading and writing data over network
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
	//fun element
	private MyDrawPanel drawPanel;
	private Random random = new Random();
	
	//actual list of instruments
	public BeatBox() {
	    instruments = List.of(
	            new BeatInstrument("Bass Drum", 35),
	            new BeatInstrument("Closed Hi-Hat", 42),
	            new BeatInstrument("Open Hi-Hat", 46),
	            new BeatInstrument("Acoustic Snare", 38),
	            new BeatInstrument("Crash Cymbal", 49),
	            new BeatInstrument("Hand Clap", 39),
	            new BeatInstrument("High Tom", 50),
	            new BeatInstrument("Hi Bongo", 60),
	            new BeatInstrument("Maracas", 70),
	            new BeatInstrument("Whistle", 72),
	            new BeatInstrument("Low Conga", 64),
	            new BeatInstrument("Cowbell", 56),
	            new BeatInstrument("Vibraslap", 58),
	            new BeatInstrument("Low-mid Tom", 47),
	            new BeatInstrument("High Agogo", 67),
	            new BeatInstrument("Open Hi Conga", 63));
	  }
	
	//main method
	public static void main(String[] args) {
		new BeatBox().startUp("Anonymous");
	}
	
	//starting up the application
	public void startUp(String name) {
		username = name;
		//open connection to server
		try {
			Socket socket = new Socket("127.0.0.1", 4242);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			//Other way to make threads
			//Thread remote = new Thread(new RemoteReader());
			//remote.start();
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(new RemoteReader());
		}
		catch(Exception ex) {
			System.out.println("Couldn't connect - you'll have to play alone.");
		}
		setUpMidi();
		buildGUI();
	}
	
	//setting up the device to use
	private void setUpMidi(){
		try {
		sequencer = MidiSystem.getSequencer();
		sequencer.open();
		//add event listener to get a surprise!!
		sequencer.addControllerEventListener(drawPanel, new int[] {127});
		
		sequence = new Sequence(Sequence.PPQ, 4);
		track = sequence.createTrack();
		sequencer.setTempoInBPM(120);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	//the GUI setup 
	public void buildGUI() {
		//main frame
		frame = new JFrame(" Cyber Beatbox");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//frame to display random colorful boxes
		colorframe = new JFrame("Mini Music Player");
		colorframe.setVisible(false);
		drawPanel = new MyDrawPanel();
		colorframe.setContentPane(drawPanel);
		colorframe.setBounds(30, 30, 500, 500);
		colorframe.setLocation(950, 55);
		
		//create menu bar for new setup and saving & restoring patterns
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		
		JMenuItem newPattern = new JMenuItem("New");
		newPattern.addActionListener(e-> clearAll());
		fileMenu.add(newPattern);
		
		JMenuItem savePattern = new JMenuItem("Save Pattern");
		savePattern.addActionListener(e-> writeFile());
		fileMenu.add(savePattern);
		
		JMenuItem choosePattern = new JMenuItem("Choose Pattern");
		choosePattern.addActionListener(e-> openFile());
		fileMenu.add(choosePattern);
		
		menuBar.add(fileMenu);
		frame.setJMenuBar(menuBar);
		
		
		BorderLayout layout = new BorderLayout(20, 20);
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		
		//box layout for instrument labels
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for(int i=0; i<16; i++) {
			JLabel nameLabel = new JLabel(instruments.get(i).getInstrumentName());
			nameLabel.setBorder(BorderFactory.createEmptyBorder(6,4,6,4));
			nameBox.add(nameLabel);
		}
		
		//box layout for buttons and messages
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		
		JButton start = new JButton("Start");
		start.addActionListener(e-> buildTrackAndPlay());
		buttonBox.add(start);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 7)));
		
		JButton stop = new JButton("Stop");
		stop.addActionListener(e-> stopTrack());
		buttonBox.add(stop);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 7)));
		
		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(e-> changeTempo(1.03F));
		buttonBox.add(upTempo);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 7)));
		
		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(e-> changeTempo(0.97F));
		buttonBox.add(downTempo);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 7)));
		
		//List for displaying incoming messages
		incomingList = new JList<>();
		incomingList.addListSelectionListener(new MyListSelectionListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane theList = new JScrollPane(incomingList);
		buttonBox.add(theList);
		incomingList.setListData(listVector);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 7)));
		
		//to write message
		userMessage = new JTextArea();
		userMessage.setLineWrap(true);
		userMessage.setWrapStyleWord(true);
		JScrollPane messageScroller = new JScrollPane(userMessage);
		buttonBox.add(messageScroller);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 5)));
		
		//send button
		JButton sendIt = new JButton("Send");
		sendIt.addActionListener(e-> sendMessageAndTracks());
		buttonBox.add(sendIt);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 7)));
		
		//grid layout for check boxes
		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(5);
		grid.setHgap(5);
		JPanel mainPanel = new JPanel(grid);
		
		checkboxList = new ArrayList<>();
		for(int i=0; i<256; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}
		
		//add all the elements to the background panel
		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);
		background.add(BorderLayout.CENTER, mainPanel);
		
		frame.getContentPane().add(background);
		
		setUpMidi();
		
		frame.setBounds(50, 50, 800, 800);
		frame.pack();
		frame.setVisible(true);
	}
	
	//clear all the check boxes for new pattern
	private void clearAll() {
		for(int check=0; check<256; check++) {
			JCheckBox c = checkboxList.get(check);
			c.setSelected(false);
		}
	}
	
	//making the other frame visible
	public void createPattern() {
		colorframe.setVisible(true);
	}
	
	//build Midi track and play
	private void buildTrackAndPlay() {
		createPattern();
		List<Integer> trackList;
		
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for(int i=0; i<16; i++) {
			trackList = new ArrayList<>();
			
			for(int j=0; j<16; j++) {
				JCheckBox c = checkboxList.get(j+(16*i));
				if(c.isSelected()) {
					int key = instruments.get(i).getMidiValue();
					trackList.add(key);
				}
				else {
					trackList.add(null);				
					}
			}
			
			makeTrack(trackList);
			track.add(makeEvent(CONTROL_CHANGE, 1, 127, 0, 16));
		}
		
		track.add(makeEvent(PROGRAM_CHANGE, 9, 1, 0, 15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
			sequencer.setTempoInBPM(220);
			sequencer.start();
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void stopTrack() {
		sequencer.stop();
		colorframe.setVisible(false);
	}
	
	private void changeTempo(float tempoChange) {
		float tempoFactor = sequencer.getTempoFactor();
		sequencer.setTempoFactor(tempoFactor*tempoChange);
	}
	
	private void makeTrack(List<Integer> list) {
		Iterator<Integer> it = list.iterator();
		for(int i=0; i<16; i++) {
			Integer key = it.next();
			if(key != null) {
				track.add(makeEvent(NOTE_ON, 9, key, 100, i));
				track.add(makeEvent(CONTROL_CHANGE, 1, 127, 0, i));    //calls event listener
				track.add(makeEvent(NOTE_OFF, 9, key, 100, i+1));
			}
		}
	}
	
	//wrapper method for saving file
	private void writeFile() {
		JFileChooser fileSave = new JFileChooser();
		fileSave.showSaveDialog(frame);
		saveFile(fileSave.getSelectedFile());
		
	}
	
	//saves the current beat pattern
	private void saveFile(File file) {
        boolean[] checkboxState = new boolean[256];
		
		for(int i=0; i<256; i++) {
			JCheckBox check = checkboxList.get(i);
			if(check.isSelected()) {
				checkboxState[i] = true;
			}
		}
		
		try(ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file))){
			os.writeObject(checkboxState);
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}	
	}
	
	//restores local file and play
	private void restoreFile(File file) {
		boolean[] checkboxState = null;
		try(ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))){
			checkboxState = (boolean[])is.readObject();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		for(int i=0; i<256; i++) {
			JCheckBox check = checkboxList.get(i);
			check.setSelected(checkboxState[i]);
		}
		
		sequencer.stop();
		buildTrackAndPlay();
	}
	
	//wrapper class for opening new file
	private void openFile() {
		JFileChooser fileOpen = new JFileChooser();
		fileOpen.showOpenDialog(frame);
		restoreFile(fileOpen.getSelectedFile());
	}
	
	//sending messages over the network
	private void sendMessageAndTracks() {
		boolean[] checkboxState = new boolean[256];
		for(int i=0; i<256; i++) {
			JCheckBox check = checkboxList.get(i);
			if(check.isSelected()) {
				checkboxState[i] = true;
			}
		}
		try {
			out.writeObject(username +" " + nextNum++ + " : " + userMessage.getText());
			out.writeObject(checkboxState);
		}
		catch(IOException e) {
			System.out.println("Terribly sorry. Could not send it to the server.");
			e.printStackTrace();
		}
		userMessage.setText("");
	}
	
	//event listener inner class
	public class MyListSelectionListener implements ListSelectionListener{
		public void valueChanged(ListSelectionEvent lse) {
			if(!lse.getValueIsAdjusting()) {
				String selected = incomingList.getSelectedValue();
				if(selected != null) {
					boolean[] selectedState = otherSeqsMap.get(selected);
					changeSequence(selectedState);
					sequencer.stop();
					buildTrackAndPlay();
				}
			}
		}
	}
	
	public class RemoteReader implements Runnable{
		boolean[] checkboxState = null;
		Object obj = null;
		
		public void run() {
			try {
				while((obj = in.readObject()) != null) {
					System.out.println("Got an object from server");
					System.out.println(obj.getClass());
					String nameToShow = (String)obj;
					checkboxState = (boolean[])in.readObject();
					otherSeqsMap.put(nameToShow, checkboxState);
					listVector.add(nameToShow);
					incomingList.setListData(listVector);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void changeSequence(boolean[] checkboxState) {
		for(int i=0; i<256; i++) {
			JCheckBox check = checkboxList.get(i);
			check.setSelected(checkboxState[i]);
		}
	}
	
	public static MidiEvent makeEvent(int cmd, int chnl, int one, int two, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage msg = new ShortMessage();
			msg.setMessage(cmd, chnl, one, two);
			event = new MidiEvent(msg, tick);
		}
		catch(Exception e) {
			e.printStackTrace();	
		}
		return event;
	}
	
	
	class MyDrawPanel extends JPanel implements ControllerEventListener{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1604412011350752922L;
		private boolean msg = false;
		
		public void controlChange(ShortMessage event) {
			msg = true;
			repaint();
		}
		
		public void paintComponent(Graphics g) {
			//super.paintComponent(g);
			if(msg) {
				int r = random.nextInt(256);
				int gr = random.nextInt(256);
				int b = random.nextInt(256);
				g.setColor(new Color(r, gr, b));
				
				int height = random.nextInt(150)+10;
				int width = random.nextInt(150)+10;
				
				int xPos = random.nextInt(300)+10;
				int yPos = random.nextInt(300)+10;
				
				g.fillRect(xPos, yPos, width, height);
				msg = false;
			}
		}
	}
	
	 private class BeatInstrument {
		    private final String instrumentName;
		    private final int midiValue;

		    BeatInstrument(String instrumentName, int midiValue) {
		      this.instrumentName = instrumentName;
		      this.midiValue = midiValue;
		    }

		    public String getInstrumentName() {
		      return instrumentName;
		    }

		    public int getMidiValue() {
		      return midiValue;
		    }
	}
}




