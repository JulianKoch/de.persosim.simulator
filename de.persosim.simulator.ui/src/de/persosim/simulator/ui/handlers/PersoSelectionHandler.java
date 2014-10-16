package de.persosim.simulator.ui.handlers;

import java.io.PrintWriter;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import de.persosim.simulator.PersoSim;
import de.persosim.simulator.ui.parts.PersoSimGuiMain;

/**
 * This class implements the handler for the personalization select from template menu entries.
 * 
 * @author slutters
 *
 */
public class PersoSelectionHandler {
	
	public static final String persoPath = "personalization/profiles/";
	public static final String persoFilePrefix = "Profile";
	public static final String persoFilePostfix = ".xml";
	
	protected PrintWriter inWriter;
	
	@Inject
	private EPartService partService;
	
	@Execute
	public void execute(@Named("de.persosim.simulator.ui.commandparameter.persoSet") String param) {
		String persoCmdString = PersoSim.CMD_LOAD_PERSONALIZATION + " " + param;
		
		System.out.println("Perso Selection Handler called with param: " + param);
		System.out.println("executing command: " + persoCmdString);
		
		// ID of part as defined in fragment.e4xmi application model
		MPart readerPart = partService.findPart("de.persosim.simulator.ui.parts.pinPad");
		
		if (readerPart.getObject() instanceof PersoSimGuiMain) {
			((PersoSimGuiMain) readerPart.getObject()).write(persoCmdString);
		}
		
		System.out.println("finished setting of personalization from template");
	}

}
