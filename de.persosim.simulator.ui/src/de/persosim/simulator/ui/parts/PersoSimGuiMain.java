package de.persosim.simulator.ui.parts;

import static de.persosim.simulator.utils.PersoSimLogger.log;
import static de.persosim.simulator.utils.PersoSimLogger.logException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import de.persosim.simulator.PersoSim;
import de.persosim.simulator.ui.Activator;
import de.persosim.simulator.ui.utils.LinkedListLogListener;

/**
 * @author slutters
 *
 */
public class PersoSimGuiMain {
	
	public static final int LOG_LIMIT = 1000;
	
	// get UISynchronize injected as field
	@Inject UISynchronize sync;
	
	private Text txtInput, txtOutput;
	
	//flag to prevent the console from unnecessary refreshing 
	Boolean updateNeeded = false;
	
	private final InputStream originalSystemIn = System.in;
	
	//Buffer for old console outputs
	private LinkedList<String> consoleStrings = new LinkedList<String>();
	
	//maximum amount of strings saved in the buffer
	private int maxLines = 2000;
	
	//maximum of lines the text field can show
	int maxLineCount=0;
	
	private final PipedInputStream inPipe = new PipedInputStream();
	
	private PrintWriter inWriter;
	
	Composite parent;
	private Button lockScroller;
	Boolean locked = false;
	Slider slider;
	
	@PostConstruct
	public void createComposite(Composite parentComposite) {
		parent = parentComposite;
		grabSysIn();
		
		parent.setLayout(new GridLayout(2, false));
		
		//configure console field
		txtOutput = new Text(parent, SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.MULTI);
		
		LinkedListLogListener listener = Activator.getListLogListener();
		if (listener != null){
			listener.setLinkedList(consoleStrings);
			listener.setMaxLines(maxLines);
			listener.addFilter("de.persosim.simulator");	
		}
		
		txtOutput.setText("PersoSim GUI" + System.lineSeparator());
		txtOutput.setEditable(false);
		txtOutput.setCursor(null);
		txtOutput.setLayoutData(new GridData(GridData.FILL_BOTH));
		txtOutput.setSelection(txtOutput.getText().length());
		txtOutput.setTopIndex(txtOutput.getLineCount() - 1);
		
		//configure the slider
		slider = new Slider(parent, SWT.V_SCROLL);
		slider.setIncrement(1);
		slider.setPageIncrement(10);
		slider.setMaximum(consoleStrings.size()+slider.getThumb());
		slider.setMinimum(0);
		slider.setLayoutData(new GridData(GridData.FILL_VERTICAL));		
		
		SelectionListener sliderListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
				buildNewConsoleContent();

			}
		};

		slider.addSelectionListener(sliderListener);
		
		txtOutput.addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseScrolled(MouseEvent e) {
				int count = e.count;
				slider.setSelection(slider.getSelection()-count);
				
				buildNewConsoleContent();					
			}
		});
		
		parent.setLayout(new GridLayout(2, false));		
		
		txtInput = new Text(parent, SWT.BORDER);
		txtInput.setMessage("Enter command here");
		
		txtInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if((e.character == SWT.CR) || (e.character == SWT.LF)) {
					String line = txtInput.getText();
					
					txtOutput.append(line + System.lineSeparator());
					inWriter.println(line);
					inWriter.flush();
					
					txtInput.setText("");
				}
			}
		});

		txtInput.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		lockScroller = new Button(parent, SWT.TOGGLE);
		lockScroller.setText(" lock ");
		lockScroller.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent (Event e) {
				
				if(locked){
					lockScroller.setText(" lock ");
					locked=false;
				}else{
					lockScroller.setText("unlock");
					locked=true;
				}
			}
		});
		
		
		PersoSim sim = new PersoSim();
		sim.loadPersonalization("1"); //load default perso with valid TestPKI EF.CardSec etc. (Profile01)
		Thread simThread = new Thread(sim);
		simThread.start();		
		
		
		final Thread uiThread = Display.getCurrent().getThread();
		
		Thread updateThread = new Thread() {
			public void run() {				
				
				while (uiThread.isAlive()) {					
					sync.syncExec(new Runnable() {

						@Override
						public void run() {
							
							 if(updateNeeded) {
								 updateNeeded=false;
								 buildNewConsoleContent();
								 showNewOutput();
							 }
						}
					});
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// sleep interrupted, doesn't matter
					}
				}
			}
		};
		updateThread.setDaemon(true);
	    updateThread.start();
		
	}
	
	/**
	 * changes the maximum of the slider and selects the 
	 * new Maximum to display the latest log messages
	 */
	private void rebuildSlider(){
		sync.syncExec(new Runnable() {
			
			@Override
			public void run() {
				
				slider.setMaximum(consoleStrings.size()+slider.getThumb()-maxLineCount+1);
				slider.setSelection(slider.getMaximum());	
			}
		});
	}
	
	
	/**
	 * takes the selected value from the Slider and prints the fitting messages
	 * from the LinkedList until the Text field is full
	 */
	private void buildNewConsoleContent() {

		// clean text field before filling it with the requested data
		final StringBuilder strConsoleStrings = new StringBuilder();

		// calculates how many lines can be shown without cutting
		maxLineCount = ( txtOutput.getBounds().height - txtOutput.getHorizontalBar().getThumbBounds().height ) / txtOutput.getLineHeight();
		
		//synchronized is used to avoid IndexOutOfBoundsExceptions 
		synchronized (consoleStrings) {
			int listSize = consoleStrings.size();

			// value is needed to stop writing in the console when the end in
			// the list is reached
			int linesToShow = maxLineCount;
			linesToShow = listSize - slider.getMaximum() + slider.getThumb();

			// Fill text field with selected data
			for (int i = 0; i < linesToShow; i++) {

				strConsoleStrings.append(consoleStrings.get(slider
						.getSelection() + i));
				strConsoleStrings.append("\n");

			}
		}
		// send the StringBuilder data to the console field
		sync.syncExec(new Runnable() {

			@Override
			public void run() {

				txtOutput.setText(strConsoleStrings.toString());

			}
		});

	}
		

	/**
	 * saves and manages Strings grabbed by {@link #grabSysOut()} in a
	 * LinkedList. This is necessary because they are not saved in the text
	 * field all the time. Writing all of them directly in the text field would
	 * slow down the whole application. Taking the Strings from the List and
	 * adding them to the text field (if needed) is done by
	 * {@link #buildNewConsoleContent()} which is called by {@link #showNewOutput()}.
	 * 
	 * @param s is the String that should be saved in the List
	 */
	/*
	protected void saveConsoleStrings(final String s) {

		String[] splitResult = s.split("(?=/n|/r)");

		for (int i = 0; i < splitResult.length; i++) {

			if (consoleStrings.size() > maxLines) {
				
				//synchronized is used to avoid IndexOutOfBoundsExceptions 
				synchronized (consoleStrings) {
					consoleStrings.removeFirst();
					consoleStrings.add(splitResult[i]);	
				}

			}else{
			consoleStrings.add(splitResult[i]);
			}
			
			
			
			if (!locked) {
				updateNeeded=true;
				rebuildSlider();
			}
		}
	}*/

	/**
	 * controls slider selection (auto scrolling)
	 */
	public void showNewOutput() {
		
		sync.syncExec(new Runnable() {
			@Override
			public void run() {
				rebuildSlider();
				slider.setSelection(slider.getMaximum());							
			}
		});

	}
	
	
	public void write(String line) {
		inWriter.println(line);
		inWriter.flush();
	}
	
	/**
	 * This method activates redirection of System.in.
	 */
	private void grabSysIn() {
		// XXX check if redirecting the system in is actually necessary
		try {
	    	inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
	    	System.setIn(inPipe);
	    	log(this.getClass(), "activated redirection of System.in");
	    }
	    catch(IOException e) {
	    	logException(this.getClass(), e);
	    	return;
	    }
	}
	
	/**
	 * This method deactivates redirection of System.in.
	 */
	private void releaseSysIn() {
		if(originalSystemIn != null) {
			System.setIn(originalSystemIn);
			log(this.getClass(), "deactivated redirection of System.in");
		}
	}
	
	@PreDestroy
	public void cleanUp() {
		releaseSysIn();
		System.exit(0);
	}

	@Focus
	public void setFocus() {
		txtOutput.setFocus();
	}
	
	@Override
	public String toString() {
		return "OutputHandler here!";
	}
	
}
