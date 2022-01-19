package com.exlibris.dps.createRosettaLogStructMap;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang.StringUtils;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class ToolMenu extends JFrame implements ActionListener{    
	private static final String usercanceled = "User canceled - tool stopped processing";
	private static final long serialVersionUID  =  1L;
	private static Instant startTime = null;
	static JFrame f;    
	JMenuBar mb;    
	JMenu single,batch;
	static JMenuItem mi1,mi2,mi3,mi4,mi5,mi6,mi7,mi8,mi9;
	static JTextArea ta;
	JScrollPane sp;
    JPanel middlePanel;

	ToolMenu() throws Throwable {    
		f = new JFrame();
		middlePanel  =  new JPanel();
	    middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "Processing information"));
		
	    create_submenus();
		mb = new JMenuBar();    
		single = new JMenu("Single REP");
		batch = new JMenu("Batch processing");    
		single.add(mi1);
		single.add(mi2);
		single.add(mi3);
		single.add(mi4);
		batch.add(mi5);
		batch.add(mi6);
		batch.add(mi7);
		batch.add(mi8);
		batch.add(mi9);
		mb.add(single);
		mb.add(batch);
		ta = new JTextArea(28,120);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.setFont(new Font("monospaced", Font.PLAIN, 12));
		ta.setEditable(false);
		String loginfo = "Log files: '" + CreateRosettaLogStructMap.currentDir + CreateRosettaLogStructMap.separator + "logs" + CreateRosettaLogStructMap.separator+ "'";
		showInfo(loginfo, true);
		sp = new JScrollPane(ta);
		sp.setVerticalScrollBarPolicy ( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		middlePanel.add(sp);
		f.add(middlePanel);
		f.add(mb);    
		f.setJMenuBar(mb);  
		f.setLayout(new BorderLayout());    
		f.setSize(850,400);    
		f.setTitle("Rosetta Structure Map Tool");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    f.add (middlePanel);
	    f.pack();
	    f.setLocationRelativeTo(null);
	    f.setVisible(true);
	    //check for missing configuration
	    String param = null;
    	String paramtocheck = "";
    	boolean newparamset = false;
    	if (CreateRosettaLogStructMap.missinguser) {
    		paramtocheck = "user";
        	param = get_missing_parameter(paramtocheck);
        	newparamset = true;
    	}
    	if (CreateRosettaLogStructMap.missingpassword) {
    		paramtocheck = "password";
        	param = get_missing_parameter(paramtocheck);
        	newparamset = true;
    	}
    	if (CreateRosettaLogStructMap.missinginstitution) {
    		paramtocheck = "institution";
        	param = get_missing_parameter(paramtocheck);
        	newparamset = true;
    	}
    	if ((param==null || (param!=null && param.isEmpty())) && newparamset) {
    		String info = "ERROR - Mandatory parameter is missing - tool stopped processing.";
    		showInfo(info, true);
    		close_frame();
    	} else {
    		String info = "";
    		if (paramtocheck.equalsIgnoreCase("user")) {
    			CreateRosettaLogStructMap.userid = param;
    			info = "User ID " + param + " has been entered.";
    		}
    		if (paramtocheck.equalsIgnoreCase("password")) {
    			CreateRosettaLogStructMap.password = param;
    			info = "User password has been entered.";
    		}
    		if (paramtocheck.equalsIgnoreCase("institution")) {
    			CreateRosettaLogStructMap.password = param;
    			info = "Institution code " + param + " has been entered.";
    		}
    		showInfo(info, true);
    	}
	}     

	private String get_missing_parameter(String missing) {    
    	String param = null;
    	String paramstillmissing = null;
		if (missing.equalsIgnoreCase("user")) {
    		param = JOptionPane.showInputDialog("Please enter the staff user ID:");
    	}
		if (missing.equalsIgnoreCase("password")) {
			Box box = Box.createHorizontalBox();

			JLabel jl = new JLabel("Password: ");
			box.add(jl);

			JPasswordField jpf = new JPasswordField(24);
			box.add(jpf);
			box.setCursor(getCursor());

			int button = JOptionPane.showConfirmDialog(null, box, "Please enter the staff user password:", JOptionPane.OK_CANCEL_OPTION);

			if (button == JOptionPane.OK_OPTION) {
			    char[] input = jpf.getPassword();
				param = new String(input);
			}
    	}
		if (missing.equalsIgnoreCase("institution")) {
    		param = JOptionPane.showInputDialog("Please enter the institution code:");
    	}
		if (param==null || (param!=null && param.isEmpty())) {
			showInfo("ERROR - " + usercanceled, true);
			paramstillmissing = JOptionPane.showInputDialog("Please enter the required parameter or cancel to close the tool.");
			param = paramstillmissing;
		}
		return param;
	}     

	public String get_option_name(String option) {    
		String optionname = CreateRosettaLogStructMap.tooloptions.get(Integer.parseInt(option.replaceAll("mi", ""))-1);
		return optionname;
	}     


	public void create_submenus() {
	    mi1 = new JMenuItem(get_option_name("mi1"));    
	    mi2 = new JMenuItem(get_option_name("mi2"));    
	    mi3 = new JMenuItem(get_option_name("mi3"));    
	    mi4 = new JMenuItem(get_option_name("mi4"));    
	    mi5 = new JMenuItem(get_option_name("mi5"));    
	    mi6 = new JMenuItem(get_option_name("mi6"));    
	    mi7 = new JMenuItem(get_option_name("mi7"));    
	    mi8 = new JMenuItem(get_option_name("mi8"));    
	    mi9 = new JMenuItem(get_option_name("mi9"));    
		mi1.addActionListener(this);           
		mi2.addActionListener(this);           
		mi3.addActionListener(this);           
		mi4.addActionListener(this);           
		mi5.addActionListener(this);           
		mi6.addActionListener(this);           
		mi7.addActionListener(this);           
		mi8.addActionListener(this);           
		mi9.addActionListener(this);
	}     

	public void actionPerformed(ActionEvent e) {    
		try {
			//single REP processing
			if(e.getSource() == mi1) {//Create CSV file from physical structure map (via API)
				String id = JOptionPane.showInputDialog("Please enter Representation ID:");
				process_input_option(mi1, id);//TEST REP1281, REP25061
			}
			if(e.getSource() == mi2) {//Create CSV file from logical structure map (via API)
				String id = JOptionPane.showInputDialog("Please enter Representation ID or Structure Map MID:");
				process_input_option(mi2, id);//TEST REP11599-1, REP9754-1 (with comma ',' in label)
			}
			if(e.getSource() == mi3) {//Create structure map XML from single prepared CSV (offline)
				String id = JOptionPane.showInputDialog("Please enter Representation ID or Structure Map MID:");
				process_input_option(mi3, id);// REP9754-1 (logical), REP1281 (physical)
			}
			if(e.getSource() == mi4) {//Add/update logical structure map for representation (via API)
				String id = JOptionPane.showInputDialog("Please enter Representation ID or Structure Map MID:");
				process_input_option(mi4, id);// REP9754-1 (logical), REP1281 (physical)
			}
			//batch processing
			if(e.getSource() == mi5) {//Create CSVs from physical structure map for list of REP IDs (via API)
				process_folder_option(mi5, CreateRosettaLogStructMap.repidsfordownloadphysical);
			}
			if(e.getSource() == mi6) {//Create CSVs from logical structure map for list of REP IDs or MIDs (via API)
				process_folder_option(mi6, CreateRosettaLogStructMap.repidsfordownloadlogical);
			}
			if(e.getSource() == mi7) {//Create CSVs for all XMLs in folder 'downloadedxml' (offline)
				process_folder_option(mi7, CreateRosettaLogStructMap.downloadedxml);
			}
			if(e.getSource() == mi8) {//Create structure map XMLs from all prepared CSVs (offline)
				process_folder_option(mi8, CreateRosettaLogStructMap.csvreadyforxml);
			}
			if(e.getSource() == mi9) {//Add/update structure maps for files in folder 'xmlreadyforupload' (via API)
				process_folder_option(mi9, CreateRosettaLogStructMap.xmlreadyforupload);
			}
			write_processing_information();
			f.setCursor(Cursor.getDefaultCursor());
		} catch (IOException e1) {
			CreateRosettaLogStructMap.logger.error(e1.getLocalizedMessage());
		} catch (Exception e1) {
			CreateRosettaLogStructMap.logger.error(e1.getLocalizedMessage());
		}
	}     

	private static void process_input_option(JMenuItem submenu, String id) throws Exception
	{
		String selectedoption = submenu.getText();
		display_selected_option(selectedoption);
		if (id!=null) {
			if (id.startsWith("REP") || id.startsWith("rep")) {//could be valid ID
				startTime = Instant.now();
				f.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				if (submenu.equals(mi1)) CreateRosettaLogStructMap.create_csv_from_physical_structmap(id.trim().toUpperCase());
				if (submenu.equals(mi2)) {
					id = id.trim().toUpperCase();
					if (id.contains("-")) {
						CreateRosettaLogStructMap.create_csv_from_logical_structmap(null,id);
					} else {
						CreateRosettaLogStructMap.create_csv_from_logical_structmap(id,null);
					}
				}
				if (submenu.equals(mi3)) CreateRosettaLogStructMap.create_xml_files(id.trim().toUpperCase(), false);
				if (submenu.equals(mi4)) {
					CreateRosettaLogStructMap.update_repository_object(id.trim().toUpperCase());
					f.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
			} else {
				String info = "ID '" + id + "' is not valid - tool stopped processing";
				stop_with_message(info);
			}			
		} else {
			stop_with_message(usercanceled);
		}
	}

	private static void process_folder_option(JMenuItem submenu, String folder) throws Exception
	{
		String selectedoption = submenu.getText();
		display_selected_option(selectedoption);
		int confirm = 0;
		if (folder!=null && !folder.isEmpty()) {
			if (CreateRosettaLogStructMap.askforconfirmation) {
				confirm = JOptionPane.showConfirmDialog(null, "Processing '"+folder+"'.");
			}
			if (confirm==0) {
				info_process_file(folder);
				startTime = Instant.now();
				f.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				if (submenu.equals(mi5)) CreateRosettaLogStructMap.bulkcreate_csv(folder, true);
				if (submenu.equals(mi6)) CreateRosettaLogStructMap.bulkcreate_csv(folder, false);
				if (submenu.equals(mi7)) {
					if (JOptionPane.showConfirmDialog(null, "Please note that files with the same name in '"+CreateRosettaLogStructMap.csvfromxml+"' will be overwritten. Click OK to continue.") == 0) {
						CreateRosettaLogStructMap.run_convert_xml_to_csv(folder);
					} else {
						stop_with_message(usercanceled);
					}
				}
				if (submenu.equals(mi8)) CreateRosettaLogStructMap.create_xml_files(null, true);
				if (submenu.equals(mi9)) {
					if (JOptionPane.showConfirmDialog(null, "Please confirm to update Rosetta.") == 0) {
						CreateRosettaLogStructMap.update_repository_object(null);
					} else {
						stop_with_message(usercanceled);
					}
				}
			} else {
				stop_with_message(usercanceled);
			}
		} else {
			String info = "Files are missing or can't be accessed - tool stopped processing";;
			stop_with_message(info);
		}
	}

	private static void stop_with_message(String info) throws Exception
	{
		showInfo("ERROR - " + info, true);
		f.setCursor(Cursor.getDefaultCursor());
		throw new Exception(info);
	}

	
	private static void display_selected_option(String selectedoption) throws Exception
	{
		String info = StringUtils.repeat("_",100);
		showInfo(info, true);
		info = "Selected option: " + selectedoption;
		showInfo(info, true);
		info = StringUtils.repeat("\u00AF",20);
		showInfo(info, true);
	}

	/*
	 * add info to log file and window
	 */
	static void info_process_file(String filename) throws Exception
	{
		String info = "Reading " + filename;
		showInfo(info, true);
	}

	public static void showInfo(String info, boolean newline) {    
		CreateRosettaLogStructMap.logger.info(info);
		if (newline) {
			info = info + System.lineSeparator();
		}
		ta.append(info);
		ta.validate();
	}     

	/*
	 * calculate processing time, write information to log
	 */
	private static void write_processing_information() throws Exception
	{
		String processingtime = "";
		if (startTime!=null) {
			Instant endTime = Instant.now();
			Duration d = Duration.between(startTime, endTime);
			long hoursPart = d.toHours(); 
			long minutesPart = d.minusHours( hoursPart ).toMinutes(); 
			long secondsPart = d.minusMinutes( minutesPart ).getSeconds() ;
			long millisecondsPart = d.minusSeconds(secondsPart).toMillis();
			processingtime = " (" + hoursPart + "h:" + minutesPart +"min:"+ secondsPart + "." + millisecondsPart + "s)";
		}
		showInfo("", true);
		showInfo("              ------------ FINISHED processing ------------" + processingtime, true);
		showInfo(StringUtils.repeat("\u00AF",100), true);
		ta.setCaretPosition(ta.getDocument().getLength());
	}

	/*
	 * close the frame and exit the application
	 */
	static void close_frame()
	{
		f.setVisible(false);
		f.dispatchEvent(new WindowEvent(f, WindowEvent.WINDOW_CLOSING));
	}

	public static void main(String[] args) {    
//		try {
//			new ToolMenu();
//		} catch (Throwable e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}    
	}    
}
