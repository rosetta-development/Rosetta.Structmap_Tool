package com.exlibris.dps.createRosettaLogStructMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.awt.datatransfer.StringSelection;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

public class CreateRosettaLogStructMap 
{
	static boolean copyxmltoclipboard = false;
	static boolean debug = false;
	static boolean askforconfirmation = true;
	static boolean beiccorrection = false;
	static boolean windows = false;
	static boolean missinguser = false;
	static boolean missingpassword = false;
	static boolean missinginstitution = false;
	static String repid;
	static String csvreadyforxml;
	static String userid;
	static String password;
	static String downloadedxml;
	static String csvfromxml;
	static String xmlreadyforupload;
	static String repidsfordownloadphysical;
	static String repidsfordownloadlogical;
	static String currentDir;
	static ArrayList<String> tooloptions = new ArrayList<String>();

	static final Logger logger = Logger.getLogger(CreateRosettaLogStructMap.class);
	static final String separator = "\\";
	
	static private enum SECTIONS { general, csvGeneration, xmlGeneration, dialogtext };
	static private int addlevels;
	static private XMLProperty properties;
	static private String iepidfromrep = null;
	static private String srubase;
	static private String wsurl;
	static private String institutioncode;
	static private String labelregex;
	static private String donefolder;

	public CreateRosettaLogStructMap() throws Exception
	{
		get_properties();
		for (int i=0; i<9; i++) {
			String tmp = properties.getProperty(SECTIONS.dialogtext.toString(), "tooloption"+Integer.toString(i+1));
			if (tmp != null) {
				tooloptions.add(Integer.toString(i+1) + " - " + properties.getProperty(SECTIONS.dialogtext.toString(), "tooloption"+Integer.toString(i+1)));
			}
		}

	}

	//	@SuppressWarnings({ "deprecation", "static-access" })
	protected void finalize() throws Exception
	{
		logger.removeAllAppenders();
	}

	/*
	 * Unique exception formatting
	 */
	private void format_exception(Exception e)
	{
		StackTraceElement[] st = e.getStackTrace();
		for (StackTraceElement se : st)
		{
			if(se.getClassName().contains(this.getClass().getSimpleName()))
			{
				logger.error(e.getClass().getSimpleName() + " = \"" + e.getMessage() + "\" (triggered by " + se.getClassName() + ":" + se.getLineNumber() + ")");
				break;
			}
		}
	}

	/*
	 * Read properties from external file
	 */
	private	void get_properties() throws Exception
	{
		properties = new XMLProperty();
		String pn = "conf/" + this.getClass().getSimpleName() + ".xml";
		properties.load(pn);
		List<String> lines = Files.readAllLines(Paths.get(pn));
		currentDir = System.getProperty("user.dir");
		debug = Boolean.parseBoolean(properties.getProperty(SECTIONS.general.toString(), "debug"));
		logger.info("Following parameters have been set in ");
		logger.info("==> " + currentDir + "/" + pn + ":");
		logger.info("------------------------------------------------------");
		for(String line : lines) {
			if (line.replaceAll(" ", "").startsWith("<") && !line.replaceAll(" ", "").startsWith("<!")) {
				logger.info(line);
			}
		}
		logger.info("------------------------------------------------------" + System.lineSeparator());
	}

	/*
	 * Check arguments
	 * Read parameters from configuration file
	 */
	private	void get_parameters() throws Exception
	{
		if (System.getProperty("os.name").startsWith("Windows")) {
			windows = true;
		}			

		askforconfirmation = Boolean.parseBoolean(properties.getProperty(SECTIONS.general.toString(), "askforconfirmation"));

		downloadedxml = properties.getProperty(SECTIONS.csvGeneration.toString(), "downloadedxml");
		if (downloadedxml==null || (downloadedxml!=null && downloadedxml.isEmpty())) {
			String info = "Folder for downloaded XML files is not configured - tool stopped processing";
			add_loginfo("ERROR - " + info, true);
			throw new Exception(info);
		} else {
			downloadedxml = correct_folder(downloadedxml);
			add_folder(downloadedxml);
		}
		
		csvfromxml = properties.getProperty(SECTIONS.csvGeneration.toString(), "csvfromxml");
		if (csvfromxml==null || (csvfromxml!=null && csvfromxml.isEmpty())) {
			String info = "Folder for CSV files is not configured - tool stopped processing";
			add_loginfo("ERROR - " + info, true);
			throw new Exception(info);
		} else {
			csvfromxml = correct_folder(csvfromxml);
			add_folder(csvfromxml);
		}
		
		addlevels = Integer.parseInt(properties.getProperty(SECTIONS.csvGeneration.toString(), "addlevels"));
		
		labelregex = properties.getProperty(SECTIONS.csvGeneration.toString(), "labelregex");

		copyxmltoclipboard = Boolean.parseBoolean(properties.getProperty(SECTIONS.csvGeneration.toString(), "copyxmltoclipboard"));

		beiccorrection = Boolean.parseBoolean(properties.getProperty(SECTIONS.csvGeneration.toString(), "beiccorrection"));

		repidsfordownloadphysical = properties.getProperty(SECTIONS.csvGeneration.toString(), "repidsfordownloadphysical"); //list of REP IDs for batch processing (physical)
		repidsfordownloadlogical = properties.getProperty(SECTIONS.csvGeneration.toString(), "repidsfordownloadlogical"); //list of REP IDs for batch processing (logical)
		
		csvreadyforxml = properties.getProperty(SECTIONS.xmlGeneration.toString(), "csvreadyforxml");
		if (csvreadyforxml==null || (csvreadyforxml!=null && csvreadyforxml.isEmpty())) {
			String info = "Folder containing CSV files for conversion to XML is not configured - tool stopped processing";
			add_loginfo("ERROR - " + info, true);
			throw new Exception(info);
		} else {
			csvreadyforxml = correct_folder(csvreadyforxml);
			add_folder(csvreadyforxml);
		}

		xmlreadyforupload = properties.getProperty(SECTIONS.xmlGeneration.toString(), "xmlreadyforupload");
		if (xmlreadyforupload==null || (xmlreadyforupload!=null && xmlreadyforupload.isEmpty())) {
			String info = "Target folder for XML files is not configured - tool stopped processing";
			add_loginfo("ERROR - " + info, true);
			throw new Exception(info);
		} else {
			xmlreadyforupload = correct_folder(xmlreadyforupload);
			add_folder(xmlreadyforupload);
			//create DONE folder
			donefolder = correct_folder(xmlreadyforupload + "DONE");
			add_folder(donefolder);
		}

		srubase = properties.getProperty(SECTIONS.general.toString(), "srubase");
		if (srubase==null || (srubase!=null && srubase.isEmpty())) {
			String info = "Base URL for SRU needs to be configured - tool stopped processing";
			add_loginfo("ERROR - " + info, true);
			throw new Exception(info);
		}

		wsurl = properties.getProperty(SECTIONS.general.toString(), "wsurl");
		if (wsurl==null || (wsurl!=null && wsurl.isEmpty())) {
			String info = "WS URL needs to be configured - tool stopped processing";
			add_loginfo("ERROR - " + info, true);
			throw new Exception(info);
		}

		userid = properties.getProperty(SECTIONS.general.toString(), "userid");
		if (userid==null || (userid!=null && userid.isEmpty())) {
			missinguser = true;
		}

		password = properties.getProperty(SECTIONS.general.toString(), "password");
		if (password==null || (password!=null && password.isEmpty())) {
			missingpassword = true;
		}

		institutioncode = properties.getProperty(SECTIONS.general.toString(), "institutioncode");
		if (institutioncode==null || (institutioncode!=null && institutioncode.isEmpty())) {
			missinginstitution = true;
		}
	}

	private static String correct_folder(String folder) throws Exception
	{
		if (folder.endsWith("\\")||folder.endsWith("/")) {
			folder = folder.substring(0, (folder.length()-1));
		}

		return folder + separator;
	}	

	public static void add_loginfo(String info, boolean newline) throws Exception
	{
		if (windows) {
			if (newline) {
				ToolMenu.showInfo(info, true);
			} else {
				ToolMenu.showInfo(info, false);
			}
		} else {
			if (newline) {
				logger.info(info + System.lineSeparator());
			} else {
				logger.info(info);
			}
		}
	}	

	/*
	 * for testing
	 * transforms XML W3C document to string
	 * */
	public static String getStringFromDocument(Document doc) throws TransformerException {
		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);
		return writer.toString();
	}

	/*
	 * read XML
	 * get content and set variable
	 */
	public static void getContentFromDocument(String responsexml, String tagtoextract) throws Exception
	{
		Document xmlDocument = readXmlFromString(responsexml);
		Element root = xmlDocument.getDocumentElement();
		NodeList nl = root.getChildNodes();

		iepidfromrep=null;
		if (tagtoextract.equalsIgnoreCase("getiepidforrep")) {
			for(int k=0;k<nl.getLength();k++){
				getIePid((Node)nl.item(k));
			}
		}
	}	

	/*
	 * parse SRU response XML snippet
	 * identify IE PID for given REP ID
	 */
	public static void getIePid(Node nodes){
		if(nodes.hasChildNodes()&&iepidfromrep==null) {
			if (nodes.getNodeName().equals("dc:identifier") && nodes.getTextContent().matches("IE[0-9].*")) {
				iepidfromrep = nodes.getTextContent(); //set value of variable
				if (debug) logger.debug("iepidfromrep: "+iepidfromrep);
			}
			NodeList nl=nodes.getChildNodes();
			for(int j=0;j<nl.getLength();j++) {
				getIePid(nl.item(j));
			}
		}
	}

	/* 
	 * read XML from string (get DocumentBuilder object from string)
	 */
	public static Document readXmlFromString(String xml) throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}

	/*
	 *  Check and create directory if needed
	 */
	private void add_folder(String foldername) throws Exception
	{
		File folder = new File(foldername);
		if (!folder.exists())
		{
			String info = "Creating directory " + folder;
			logger.info(info);
			folder.mkdirs();
		}
	}

	/*
	 * create file lines for CSV (physical)
	 */
	private static ArrayList<String> get_csv_line_physical(ArrayList<String> alFileNameId) throws Exception
	{
		/*
		 * new CSV lines are added to string array 'NewCSV'
		 * 'NewCSV' is written to file 'csvname' in 'csvfromxml'
		 */
		ArrayList<String> FileLines = new ArrayList<String>();
		ArrayList<String> repLabels = new ArrayList<String>();
		String replabels = new String();
		String fileline = new String();

		repLabels = SoapRequestClient.repLabels;
		for (String tmp : repLabels) {
			replabels = replabels + tmp + ",";
		}
		
		//create and add file lines
		String filecommas = StringUtils.repeat(",", addlevels);
		String repcommas = StringUtils.repeat(",", repLabels.size());

		fileline = replabels+filecommas+get_label_from_filename(alFileNameId.get(0))+","+alFileNameId.get(1);//only the first line should contain "replabels"
		FileLines.add(fileline);

		for(int j=2;j<alFileNameId.size()-1;j++) {
			fileline = repcommas+filecommas+get_label_from_filename(alFileNameId.get(j))+","+alFileNameId.get(j+1);
			FileLines.add(fileline);
			j = j+1;
		}

		return FileLines;
	}

	/*
	 * Build and write CSV from LOGICAL or PHYSICAL
	 */
	private static void build_and_write_csv(String repmid, ArrayList<String> csvLinesFromXmlFile, String type, ArrayList<String> alFileNameId) throws Exception
	{
		/*
		 * part for file line was generated via ConvertLogicalXmlToCsv.createCsvFromLogicalXML(pathtoxmlfile)
		 * new CSV lines are added to string array 'NewCSV'
		 * 'NewCSV' is written to file 'csvname' in 'csvfromxml'
		 */
		ArrayList<String> NewCSV = new ArrayList<String>();
		NewCSV.clear();
		String headerline = new String();
		String csvname = new String();

		String newline = System.getProperty("line.separator"); // this will take proper line break depending on OS

		//calculate header line - Level 1 to Level n + File ID
		if (type.equalsIgnoreCase("physical")) {
			for (int i=0; i<ConvertLogicalXmlToCsv.depthxml-1+addlevels; i++) {
				headerline = headerline + "Level " + i + ",";
			}
		}
		if (type.equalsIgnoreCase("logical")) {
			for (int i=0; i<ConvertLogicalXmlToCsv.depthxml-1; i++) {
				headerline = headerline + "Level " + i + ",";
			}
		}
		headerline = headerline + "File ID";
		NewCSV.add(headerline);
		
		//add file lines (physical)
		if (type.equalsIgnoreCase("physical") && alFileNameId!=null) {
			ArrayList<String> FileLines = get_csv_line_physical(alFileNameId);
			for(String fileline : FileLines) {
				NewCSV.add(fileline);
			}
		}

		//add file lines (logical)
		if (type.equalsIgnoreCase("logical") && csvLinesFromXmlFile!=null) {
			for(String fileline : csvLinesFromXmlFile) {
				NewCSV.add(fileline);
			}
			csvLinesFromXmlFile.clear();
		}

		csvname = repmid+".csv";
		String inforepmid = String.format("%-15s", repmid);
		FileWriter writecsv;
		try {
			writecsv = new FileWriter(csvfromxml + csvname);
			String info = inforepmid + " - SUCCESS";
		
		add_loginfo(info, true);
		for (int k=0; k<NewCSV.size(); k++)
		{
			if (debug) logger.debug("add line: " + NewCSV.get(k));
			writecsv.write(NewCSV.get(k)+ newline);
		}
		writecsv.close();
		NewCSV.clear();
		NewCSV = null;
		} catch (IOException e) {
			String info = inforepmid + " - ERROR - : " + e.getMessage();
			add_loginfo(info, true);
		}
	}

	/*
	 * extract label from filename via regular expression
	 * if no regex is configured, copy complete filename
	 */
	private static String get_label_from_filename(String filename) throws Exception
	{
		String filelabel = filename;
		try {
			if (labelregex != null) {
				Pattern p = Pattern.compile(labelregex);
				Matcher m = p.matcher(filename);
				while (m.matches()) {
					filelabel = m.replaceAll("$1");
					if (debug) {
						logger.debug("labelregex - filename / filelabel: " + filename + " / " + filelabel);
					}
					return filelabel;
				}
			}
		} catch (PatternSyntaxException e) {
			e.printStackTrace();
			String info = "Regex pattern syntax exception. Please check <labelregex>" 
					+ labelregex + "</labelregex> in CreateRosettaLogStructMap.xml.";
			add_loginfo(info, true);
		}
		return filelabel;
	}		


	/*
	 * write info into log
	 */
	private static void writing_into(String folder) throws Exception
	{
		String info = "Writing into " + folder;
		add_loginfo(info, true);
	}		


	/*
	 * Start creating structmap xml
	 * search folder that contains adjusted CSV file
	 * read CSV into list if string arrays 'contentcsv'
	 */
	static void create_xml_files(String repidormid, boolean batch) throws Exception
	{
//		String basename = new String();
		File files_dir = new File(csvreadyforxml);
		File[] files = files_dir.listFiles();
		String filename = null;
		String info = null;
//		boolean processing = false;

		if (files==null) { //directory is empty
			info = "There are no CSV files in " + csvreadyforxml + ". Please check!";
			ToolMenu.showInfo("ERROR - " + info, true);
			ToolMenu.f.setCursor(Cursor.getDefaultCursor());
			throw new Exception(info);
		} else {
			if (repidormid!=null) { //single REP processing
				info = "";
				filename = repidormid.toUpperCase()+".csv";
				File file = new File(csvreadyforxml, filename);
				boolean fileexists = file.exists();
				if (fileexists) {
					List<String[]> contentcsv = read_csv_file(file); // read one CSV
					if (debug) {
						info = "Processing CSV '" + csvreadyforxml + filename + "'.";
						logger.debug(info);
					}
					if (!batch) {
						writing_into(xmlreadyforupload);
						write_xml(contentcsv, repidormid, true); // write one XML as part of single processing
					} else {
						write_xml(contentcsv, repidormid, false); // write one XML as part of batch processing
					}
				}
				else {
					info = "There is no CSV file for " + repidormid + " in " + csvreadyforxml + ". Please check!";
					ToolMenu.showInfo("ERROR - " + info, true);
					throw new Exception(info);
				}
			} else { //batch processing
				filename = null;
				writing_into(xmlreadyforupload);
				for( File file : files ) {
					filename = file.getName();
					if (filename.matches("REP.*\\.csv")) {
						repidormid = filename.substring(0, filename.lastIndexOf("."));
						create_xml_files(repidormid, true);
					}
				}
			}
		}
	}

	/*
	 * read in the CSV and transform to array
	 */
	private static List<String[]> read_csv_file(File file) throws Exception
	{
		List<String[]> contentcsv = new ArrayList<>();		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		reader.readLine();//ignore first line (header line)
		while ((line = reader.readLine()) != null) {
			//handle comma in quotes: replace by '\u0333'
			StringBuilder builder = new StringBuilder(line);
			boolean inquotes = false;
			for (int currentIndex = 0; currentIndex < builder.length(); currentIndex++) {
			    char currentChar = builder.charAt(currentIndex);
			    if (currentChar == '\"') inquotes = !inquotes; // toggle state
			    if (currentChar == ',' && inquotes) {
			        builder.setCharAt(currentIndex, '\u0333');  // replace this character (combining double low line) back to comma ',' in write_xml()
			    }
			}
			line = builder.toString();
			line = line.replaceAll("^\"", ""); // remove quote from start
			line = line.replaceAll("\",", ","); // remove quote before comma
			line = line.replaceAll(",\"", ","); // remove quote after comma
			line = line.replaceAll("\"$", ""); // remove quote from end
			contentcsv.add(line.split(","));
		}
		reader.close();
		return contentcsv;
	}


	private static void write_xml(List<String[]> contentcsv, String basename, boolean singlexml) throws Exception
	{
		String xmlname = basename + ".xml"; //basename equals the REP (M)ID
		ArrayList<String> NewXML = new ArrayList<String>();
		String closediv = "</mets:div>";
		String closestructmap = "</mets:structMap>";

		String info = "";
		String inforepid = String.format("%-15s", basename);

		boolean errorexists = false;
		boolean donefileexists = new File(donefolder, xmlname).exists();
		if (donefileexists) {
			info = info + " (NOTE: Update done before.)";
		}

		//initiate current DIVs matrix: no DIVs are open
		String[] CurrentFileDivMatrix = new String[contentcsv.get(0).length];
		for(int l=0;l<CurrentFileDivMatrix.length;l++) {
			CurrentFileDivMatrix[l] = "no";
		}

		String structmapline = "<mets:structMap xmlns:mets=\"http://www.loc.gov/METS/\" ID=\""+basename+"\" TYPE=\"LOGICAL\">";
		NewXML.add(structmapline);
		for(int j=0;j<contentcsv.size();j++) { //process complete CSV
			String[] csvline = contentcsv.get(j);
			
			String[] filelinematrix = new String[contentcsv.get(0).length-2];
			
			if (csvline.length-2<=filelinematrix.length) {

				for (int n=0; n<csvline.length-2; n++) {//iterate through all columns except of last (i.e. 'File ID')
					if (csvline[n].isEmpty()) {
						filelinematrix[n] = "no"; // nothing to do
					} else {
						filelinematrix[n] = "yes"; // DIV is opened on this level
					}
				}
			} else { //that's the case if the CSV line has too many commas
				info = inforepid + " - ERROR (CSV file is invalid. Please check!)";
				ToolMenu.showInfo(info, true);
				ToolMenu.f.setCursor(Cursor.getDefaultCursor());
				contentcsv.clear();
				errorexists = true;
				if (singlexml) {
					throw new Exception(info);
				}
			}

			/*
			 * compare filelinematrix with CurrentFileDivMatrix:
			 * if DIV in line is opened, check if there are any open DIVs on same or lower level, and close them
			 */
			String filetag = "";//file
			String completeline = "";//complete line
			int filedivs = filelinematrix.length;
			for (int n=0; n<filedivs; n++) {
				if (filelinematrix[n]=="yes") {
					completeline = completeline + write_metsdiv(csvline[n], n); // write <mets:div LABEL="{csvline[n]}">
					for (int p=n; p<filedivs; p++) {//check against current and following (i.e. lower) levels
						if (CurrentFileDivMatrix[p]=="yes") {
							completeline = closediv + completeline; //close this level
							CurrentFileDivMatrix[p] = "no"; //indicate that this level was closed
						}
					}
					CurrentFileDivMatrix[n] = "yes";
				}
			}

			if (!csvline[csvline.length-2].isEmpty()) {//file label exists in last column before File ID
				String csvcell = csvline[csvline.length-2];
				filetag = "<mets:div LABEL=\"" + csvcell + "\" TYPE=\"FILE" + "\">" 
						+ "<mets:fptr FILEID=\"" + csvline[csvline.length-1] + "\"/>" + closediv;
			} else {
				/*
				 * special case: file label is also used as structmap label (e.g. 'Frontespizio')
				 * find last label and transform it to file label
				 */
				for (int n=csvline.length-2; n>0; n--) {
					if (!csvline[n].isEmpty()) {
						filetag = "<mets:fptr FILEID=\"" + csvline[csvline.length-1] + "\"/>" + closediv;
						completeline = completeline.replaceFirst(">$", " TYPE=\"FILE\""+">");
						CurrentFileDivMatrix[n] = "no";
						break;
					}
					continue;		
				}
			}
			NewXML.add(completeline + filetag);
		}
		/*
		 * close all open DIVs
		 */
		String closingdivs="";
		for (int r=0; r<CurrentFileDivMatrix.length; r++) {//check for open DIVs
			if (CurrentFileDivMatrix[r]=="yes") {
				closingdivs = closingdivs + closediv; //close this level
			}
		}
		NewXML.add(closingdivs);
		NewXML.add(closestructmap);//close structure map

		if (!errorexists) {
			
			ArrayList<String> lines = new ArrayList<>();
			for (int k=0; k<NewXML.size()-1; k++)//write XML (except last line)
			{
				String tmpline = NewXML.get(k).replaceAll("\u0333", ","); //put comma back
				lines.add(tmpline);
			}
			lines.add(NewXML.get(NewXML.size()-1));//write last line
			
			Path xmlFile = Paths.get(xmlreadyforupload + xmlname);
		    Files.write(xmlFile, lines);

			contentcsv.clear();

			//copy to clipboard
			if (singlexml && copyxmltoclipboard && !NewXML.isEmpty()) {
				String structmapxml = NewXML.toString();
				structmapxml = structmapxml.substring(1, structmapxml.length()-1).replaceAll(">, <", "><");
				StringSelection stringSelection = new StringSelection(structmapxml);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
			}
			info = inforepid + " - SUCCESS";
			add_loginfo(info, true);
		}
	}

	private static String write_metsdiv(String csvcell, int n) throws Exception
	{
		if (!csvcell.isEmpty()) {
			if (beiccorrection) {
				if (csvcell.matches(".*&amp;#.[0-9].*;.*$")) { //correction for wrong source data coming from Rosetta, e.g. &amp;#x2026; -> must be &#x2026;
					csvcell = csvcell.replaceAll("&amp;#(.[0-9].*?;)", "&#$1");
				}
			}
			csvcell = StringEscapeUtils.escapeXml10(csvcell); //escape XML characters (<>'"&)
			if (csvcell.matches(".*&amp;#.[0-9].*;.*$")) { //special character had been entered as HTML entity into the CSV, e.g. &#x2026;
				csvcell = csvcell.replaceAll("&amp;#(.[0-9].*?;)", "&#$1");
			}
			if (csvcell.matches("^\".*\"$")) { //content is in hyphens "..."
				csvcell = csvcell.replaceAll("^\"", "");
				csvcell = csvcell.replaceAll("\"$", "");
			}
			if (debug) {
				String metsdivopenerstart = "<mets:div X"+n+"LABEL=\"";
				String metsdivopenerend = "\">";
				String metsdivdebug = metsdivopenerstart+csvcell+metsdivopenerend;
				logger.debug("metsdiv with level: "+metsdivdebug);
			}
			String metsdivopenerstart = "<mets:div LABEL=\"";
			String metsdivopenerend = "\">";
			String metsdiv = metsdivopenerstart+csvcell+metsdivopenerend;
			return metsdiv;
		} else return csvcell;
	}


	/*
	 * Start update of REPs
	 * search folder that contains created structure map XML files
	 * read XML, call update via soap, check result
	 * if success, move XML file to donefolder
	 */
	static void update_repository_object(String repidormid) throws Exception
	{
		String updateresult = new String();
		File files_dir = new File(xmlreadyforupload);
		File[] files = files_dir.listFiles();
		String filename = null;

		if (files==null || files.length<=1) { //directory is empty (there is only DONE folder)
			String info = "There are no XML files in " + xmlreadyforupload + ". Please check!";
			ToolMenu.showInfo("ERROR - " + info, true);
			ToolMenu.f.setCursor(Cursor.getDefaultCursor());
			throw new Exception(info);
		} else {
			if (repidormid!=null) { //single REP processing
				String info = "";
				String inforepidormid = String.format("%-15s", repidormid);
				filename = repidormid.toUpperCase()+".xml";
				boolean fileexists = new File(xmlreadyforupload, filename).exists();
				if (fileexists) {
					String structmapxml = Files.lines(Paths.get(xmlreadyforupload, filename)).collect(Collectors.joining("\n"));
					updateresult = SoapRequestClient.addMdViaSoap(wsurl, userid, 
							password, institutioncode, repidormid, structmapxml, "true", "mets_section", "structMap"); // update REP

					if (debug) {
						logger.info("updateresult: " + updateresult);
					}
					String updateerror = check_result_of_update(updateresult);
					if (debug) {
						logger.info("updateerror: " + updateerror);
					}
					if (updateerror != null) {
						info = inforepidormid + " - ERROR (" + updateerror + ")";
						add_loginfo(info, true);
					} else {
						info = inforepidormid + " - SUCCESS";
						Path fileToMovePath = Paths.get(xmlreadyforupload, filename);
						Path targetPath = Paths.get(donefolder, filename);
						try {
							boolean donefileexists = new File(donefolder, filename).exists();
							if (donefileexists) {
								info = info + " (NOTE: Update done before.)";
								Files.deleteIfExists(targetPath);
							}
							add_loginfo(info, true);
							renameFile(fileToMovePath, targetPath); //move file to avoid double processing
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				else {
					info = "There is no XML file for " + repidormid + " in " + xmlreadyforupload + ". Please check!";
					ToolMenu.showInfo("ERROR - " + info, true);
					ToolMenu.f.setCursor(Cursor.getDefaultCursor());
					throw new Exception(info);
				}
			} else { //batch processing
				filename = null;
				for( File file : files ) {
					filename = file.getName();
					if (filename.matches("REP.*\\.xml")) {
						repidormid = filename.substring(0, filename.lastIndexOf("."));
						update_repository_object(repidormid);
					}
				}
			}
		}
	}

	private static String check_result_of_update(String updateresult) throws Exception
	{
		//...<soap:Body><ns2:updateMDResponse xmlns:ns2="http://dps.exlibris.com/"/></soap:Body>... indicates successful update
		if (updateresult.contains("updateMDResponse")) {
			updateresult = null;
			return updateresult;
		}
		if (updateresult.contains("<faultstring>")) {
			updateresult = updateresult.substring(updateresult.indexOf("<faultstring>")+13, updateresult.indexOf("</faultstring>"));
			return updateresult;
		}
		return updateresult;
	}

	private static void renameFile(Path fileToMovePath, Path targetPath) throws IOException 
	{
		Files.move(fileToMovePath, targetPath); //rename file to avoid double processing
	}

	private static void tmpout(String name, String value) throws IOException 
	{
		System.out.println(name+": "+value);
	}

	private static void writeFile(String folder, String filename, String content) throws Exception, IOException 
	{
		List<String> lines = Arrays.asList(content);
		
		Path textFile = Paths.get(folder + filename);
	    Files.write(textFile, lines);
	}

//	private static void writeFile(String folder, String filename, String content) throws Exception, IOException 
//	{
//		FileWriter writexml = new FileWriter(folder + filename);
//		
//		writexml.append(content);
//		writexml.close();
//	}

	/*
	 * request structure map(s) via SOAP
	 * via representation ID: get all logical structure maps and select latest one (highest version number)
	 * via structure map MID: get specific logical structure map
	 * write response xml into folder <downloadedxml>
	 * call convert_xml_to_csv()
	 */
	static boolean create_csv_from_logical_structmap(String repid, String repmid) throws Exception, IOException
	{
		String soapresponse = null;
		boolean errorexists = false;
		String usecase = new String();
		String inforepmid = new String();
		String msg = new String();
		String info = new String();

		if (repid == null && repmid != null) {
			usecase = "userepMid";
		} else if (repid != null && repmid == null) {
			usecase = "userepId";
		} else {
			return false;
		}
		
		if (!errorexists) {

			switch (usecase) {
			case "userepMid":
				inforepmid = String.format("%-15s", repmid);
				repid = repmid.substring(0, repmid.indexOf("-"));
				break;
			case "userepId":
				inforepmid = String.format("%-15s", repid);
				break;
			default:break;
			}
			try {
				soapresponse = SoapRequestClient.getLogicalStructmapViaSoap(wsurl, userid, password, institutioncode, repid, repmid);//repid or repmid can be null
				if (debug) logger.debug("create_csv_from_logical_structmap() soapresponse: " + soapresponse);
			} catch (IOException e) {
				info = "ERROR - " + e.getMessage();
				add_loginfo(info, true);
				return false;
			}

			if (soapresponse != null && !soapresponse.contains("<faultstring>") && soapresponse.contains("structMap")) {
				//get structure map with highest version number from response and write xml into folder <downloadedxml>
				repmid = extract_structmap(repid, soapresponse, "logical");
				if (repmid == null || !repmid.matches("REP[0-9].*-[0-9].*")) errorexists = true;
			} else {
				if (soapresponse.contains("<faultstring>")) {
					msg = soapresponse.substring(soapresponse.indexOf("<faultstring>")+13, soapresponse.indexOf("</faultstring>"));
					if (msg.contains("is not related to this pid")) {
						logger.info(msg);
						msg = "Logical structure map doesn't exist";
					}
					if (msg.contains("since it isn't in the Permanent")) {
						logger.info(msg);
						msg = "Logical structure map doesn't exist";
					}
					info = inforepmid + " - ERROR (" + msg + ")";
					add_loginfo(info, true);
					errorexists = true;
				} else {
					msg = "Logical structure map doesn't exist";
					info = inforepmid + " - ERROR (" + msg + ")";
					add_loginfo(info, true);
					return false;
				}
			}
			//convert XML to CSV
			if (!errorexists) {
				convert_xml_to_csv(repmid, "logical");
				return true;
			}
		}
		return false;
	}

	public static void create_csv_from_physical_structmap(String repid) throws Exception, IOException
	{
		String inforepidormid = String.format("%-15s", repid);
		String sruurl;
		String sruoperation = "operation=searchRetrieve&startRecord=1";
		String sruquery = "&query=REP.internalIdentifier.internalIdentifierType.PID=";
		sruurl = srubase+sruoperation+sruquery;
		if (debug) logger.debug("sruurl: "+sruurl);
		if (debug) logger.debug("repid: "+repid);

		
		String soapresponse = "";
		String sruresponse = SoapRequestClient.getSruResponse(sruurl, repid);
		if (debug) logger.debug("sruresponse: "+sruresponse);
		if (sruresponse!=null && sruresponse.contains("<numberOfRecords>1</numberOfRecords>")) {
			getContentFromDocument(sruresponse,"getiepidforrep");
		} else {
			String info = inforepidormid + " - ERROR (Couldn't get structure map XML - tool stopped processing)";
			stop_with_message(info);
		}

		try {
			soapresponse = SoapRequestClient.getIeXmlViaSoap(wsurl, userid, password, institutioncode, iepidfromrep);
		} catch (IOException e) {
			String info = inforepidormid + " - ERROR (" + e.getMessage() + ")";
			add_loginfo(info, true);
		}
		if (soapresponse != null && !soapresponse.contains("<faultstring>")) {
			if (debug) logger.debug("soapresponse: "+soapresponse);
			repid = extract_structmap(repid, soapresponse, "physical");//extract the right structure map
			if (repid != null) {//if null, extract_structmap() failed
				convert_xml_to_csv(repid, "physical");//convert XML to CSV
			}
		} else {
			String info = inforepidormid + " - ERROR (Couldn't get structure map XML - tool stopped processing)";
			stop_with_message(info);
		}
	}

	
	/*
	 * extract proper structure map from list in soapresponse
	 * physical: get PHYSICAL
	 * logical: get latest LOGICAL (highest version)
	 */
	static String extract_structmap(String repid, String soapresponse, String type) throws Exception
	{
		String structmap = "";
		String inforepidormid = String.format("%-15s", repid);
		boolean physical = false;
		if (type.equalsIgnoreCase("physical")) {
			physical = true;
		}
		try {
			//TEST
//			repid = "REP53308"; (physical)
//			soapresponse =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>  <mets:mets xmlns:mets=\"http://www.loc.gov/METS/\">    <mets:dmdSec ID=\"ie-dmd\">      <mets:mdWrap MDTYPE=\"DC\">        <mets:xmlData>          <dc:record xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">            <dc:title>Embedding Digital Preservation across the Organisation: A Case Study of Internal Collaboration in the National Library of New Zealand</dc:title>            <dc:creator>Wu, C.</dc:creator>            <dc:date>2012</dc:date>          </dc:record>        </mets:xmlData>      </mets:mdWrap>    </mets:dmdSec>    <mets:amdSec ID=\"REP53308-amd\">      <mets:techMD ID=\"REP53308-amd-tech\">        <mets:mdWrap MDTYPE=\"OTHER\" OTHERMDTYPE=\"dnx\">          <mets:xmlData>            <dnx xmlns=\"http://www.exlibrisgroup.com/dps/dnx\">              <section id=\"generalRepCharacteristics\">                <record>                  <key id=\"preservationType\">PRESERVATION_MASTER</key>                  <key id=\"usageType\">VIEW</key>                  <key id=\"RevisionNumber\">1</key>                </record>              </section>              <section id=\"internalIdentifier\">                <record>                  <key id=\"internalIdentifierType\">SIPID</key>                  <key id=\"internalIdentifierValue\">997</key>                </record>                <record>                  <key id=\"internalIdentifierType\">PID</key>                  <key id=\"internalIdentifierValue\">REP53308</key>                </record>                <record>                  <key id=\"internalIdentifierType\">DepositSetID</key>                  <key id=\"internalIdentifierValue\">10452</key>                </record>              </section>              <section id=\"objectCharacteristics\">                <record>                  <key id=\"objectType\">REPRESENTATION</key>                  <key id=\"creationDate\">2015-08-02 09:52:41</key>                  <key id=\"createdBy\">admin2</key>                  <key id=\"modificationDate\">2015-08-02 09:52:41</key>                  <key id=\"modifiedBy\">admin2</key>                  <key id=\"owner\">CRS00.INS00.DPR00</key>                </record>              </section>            </dnx>          </mets:xmlData>        </mets:mdWrap>      </mets:techMD>      <mets:rightsMD ID=\"REP53308-amd-rights\">        <mets:mdWrap MDTYPE=\"OTHER\" OTHERMDTYPE=\"dnx\">          <mets:xmlData>            <dnx xmlns=\"http://www.exlibrisgroup.com/dps/dnx\"/>          </mets:xmlData>        </mets:mdWrap>      </mets:rightsMD>      <mets:digiprovMD ID=\"REP53308-amd-digiprov\">        <mets:mdWrap MDTYPE=\"OTHER\" OTHERMDTYPE=\"dnx\">          <mets:xmlData>            <dnx xmlns=\"http://www.exlibrisgroup.com/dps/dnx\"/>          </mets:xmlData>        </mets:mdWrap>      </mets:digiprovMD>    </mets:amdSec>    <mets:amdSec ID=\"FL53309-amd\">      <mets:techMD ID=\"FL53309-amd-tech\">        <mets:mdWrap MDTYPE=\"OTHER\" OTHERMDTYPE=\"dnx\">          <mets:xmlData>            <dnx xmlns=\"http://www.exlibrisgroup.com/dps/dnx\">              <section id=\"objectCharacteristics\">                <record>                  <key id=\"objectType\">FILE</key>                  <key id=\"creationDate\">2015-08-02 09:52:41</key>                  <key id=\"createdBy\">admin2</key>                  <key id=\"modificationDate\">2015-08-02 09:52:41</key>                  <key id=\"modifiedBy\">admin2</key>                  <key id=\"owner\">CRS00.INS00.DPR00</key>                </record>              </section>              <section id=\"internalIdentifier\">                <record>                  <key id=\"internalIdentifierType\">SIPID</key>                  <key id=\"internalIdentifierValue\">997</key>                </record>                <record>                  <key id=\"internalIdentifierType\">PID</key>                  <key id=\"internalIdentifierValue\">FL53309</key>                </record>                <record>                  <key id=\"internalIdentifierType\">DepositSetID</key>                  <key id=\"internalIdentifierValue\">10452</key>                </record>              </section>              <section id=\"fileFixity\">                <record>                  <key id=\"agent\">REG_SA_JAVA5_FIXITY</key>                  <key id=\"fixityType\">MD5</key>                  <key id=\"fixityValue\">dd71946697650bdcaef8b1b8e60179d0</key>                </record>                <record>                  <key id=\"agent\">REG_SA_JAVA5_FIXITY</key>                  <key id=\"fixityType\">SHA1</key>                  <key id=\"fixityValue\">b30bfd5f434bddbf275ddfaa0753dc23e692747a</key>                </record>                <record>                  <key id=\"agent\">REG_SA_JAVA5_FIXITY</key>                  <key id=\"fixityType\">CRC32</key>                  <key id=\"fixityValue\">ff96b3f7</key>                </record>              </section>              <section id=\"vsOutcome\">                <record>                  <key id=\"checkDate\">Sun Aug 02 09:53:03 CEST 2015</key>                  <key id=\"type\">CHECKSUM</key>                  <key id=\"vsAgent\">REG_SA_JAVA5_FIXITY</key>                  <key id=\"result\">PASSED</key>                  <key id=\"vsEvaluation\">PASSED</key>                </record>                <record>                  <key id=\"checkDate\">Sun Aug 02 09:53:03 CEST 2015</key>                  <key id=\"type\">VIRUSCHECK</key>                  <key id=\"vsAgent\">REG_SA_DPS</key>                  <key id=\"result\">PASSED</key>                  <key id=\"vsEvaluation\">PASSED</key>                </record>                <record>                  <key id=\"checkDate\">Sun Aug 02 09:53:03 CEST 2015</key>                  <key id=\"type\">FILE_FORMAT</key>                  <key id=\"vsAgent\">REG_SA_DROID , Version 6.01 , Signature version Binary SF v.81/ Container SF v.1</key>                  <key id=\"result\">PASSED</key>                  <key id=\"vsEvaluation\">PASSED</key>                </record>                <record>                  <key id=\"checkDate\">Sun Aug 02 09:53:03 CEST 2015</key>                  <key id=\"type\">TECHMD</key>                  <key id=\"vsAgent\">JHOVE , PDF-hul 1.7 , Plugin Version 3.0</key>                  <key id=\"result\">PASSED</key>                  <key id=\"vsEvaluation\">PASSED</key>                </record>                <record>                  <key id=\"checkDate\">Sun Aug 02 09:53:03 CEST 2015</key>                  <key id=\"type\">RISK_ANALYSIS</key>                  <key id=\"vsAgent\">REG_SA_DPS</key>                  <key id=\"result\">PASSED</key>                  <key id=\"vsEvaluation\">PASSED</key>                </record>              </section>              <section id=\"fileVirusCheck\">                <record>                  <key id=\"status\">PASSED</key>                  <key id=\"agent\">REG_SA_DPS</key>                  <key id=\"content\">/exlibris/dps/d4_1/profile/repository/storage1/2015/08/02/file_1/V1-FL53309.pdf is Virus Free</key>                </record>              </section>              <section id=\"fileFormat\">                <record>                  <key id=\"agent\">REG_SA_DROID</key>                  <key id=\"formatRegistry\">PRONOM</key>                  <key id=\"formatRegistryId\">fmt/18</key>                  <key id=\"formatName\">fmt/18</key>                  <key id=\"formatVersion\">1.4</key>                  <key id=\"formatDescription\">Portable Document Format</key>                  <key id=\"exactFormatIdentification\">true</key>                  <key id=\"mimeType\">application/pdf</key>                  <key id=\"agentVersion\">6.01</key>                  <key id=\"agentSignatureVersion\">Binary SF v.81/ Container SF v.1</key>                  <key id=\"formatLibraryVersion\">4.1081</key>                </record>              </section>              <section id=\"generalFileCharacteristics\">                <record>                  <key id=\"label\">Archiving2012-CynthiaWu-Internal-Collab-Paper.pdf</key>                  <key id=\"fileLocationType\">FILE</key>                  <key id=\"fileOriginalName\">Archiving2012-CynthiaWu-Internal-Collab-Paper.pdf</key>                  <key id=\"fileOriginalPath\">Archiving2012-CynthiaWu-Internal-Collab-Paper.pdf</key>                  <key id=\"fileOriginalID\">/exlibris/dps/d4_1/profile/units/DTL01/deposit_area_1//10001-11000/dep_10452/deposit/content/streams/Archiving2012-CynthiaWu-Internal-Collab-Paper.pdf</key>                  <key id=\"fileExtension\">pdf</key>                  <key id=\"fileMIMEType\">application/pdf</key>                  <key id=\"fileSizeBytes\">716523</key>                  <key id=\"formatLibraryId\">fmt/18</key>                </record>              </section>              <section id=\"significantProperties\">                <record>                  <key id=\"significantPropertiesType\">pdf.filterPipeline</key>                  <key id=\"significantPropertiesValue\">FlateDecode</key>                </record>                <record>                  <key id=\"significantPropertiesType\">pdf.freeObjects</key>                  <key id=\"significantPropertiesValue\">1</key>                </record>                <record>                  <key id=\"significantPropertiesType\">pdf.ID</key>                  <key id=\"significantPropertiesValue\">[0xa35c24d4ff0cfab178d1adbc5b30a38729, 0xa35c24d4ff0cfab178d1adbc5b30a38729]</key>                </record>                <record>                  <key id=\"significantPropertiesType\">pdf.incrementalUpdates</key>                  <key id=\"significantPropertiesValue\">0</key>                </record>                <record>                  <key id=\"significantPropertiesType\">object.author</key>                  <key id=\"significantPropertiesValue\">wuc</key>                </record>                <record>                  <key id=\"significantPropertiesType\">pdf.creationDate</key>                  <key id=\"significantPropertiesValue\">Thu Nov 14 09:58:42 CET 2013</key>                </record>                <record>                  <key id=\"significantPropertiesType\">pdf.originalFormatCreatingApplication</key>                  <key id=\"significantPropertiesValue\">PScript5.dll Version 5.2.2</key>                </record>                <record>                  <key id=\"significantPropertiesType\">pdf.modifiedDate</key>                  <key id=\"significantPropertiesValue\">Thu Nov 14 09:58:42 CET 2013</key>                </record>                <record>                  <key id=\"significantPropertiesType\">object.creatingApplication</key>                  <key id=\"significantPropertiesValue\">GPL Ghostscript 8.15</key>                </record>                <record>                  <key id=\"significantPropertiesType\">object.title</key>                  <key id=\"significantPropertiesValue\">Microsoft Word - Archiving2012_CynthiaWu_Internal_Collab_Paper.doc</key>                </record>                <record>                  <key id=\"significantPropertiesType\">pdf.objectsCount</key>                  <key id=\"significantPropertiesValue\">73</key>                </record>              </section>              <section id=\"fileValidation\">                <record>                  <key id=\"agent\">JHOVE , PDF-hul 1.7 , Plugin Version 3.0</key>                  <key id=\"pluginName\">PDF-hul-1.10</key>                  <key id=\"format\">PDF</key>                  <key id=\"version\">1.4</key>                  <key id=\"mimeType\">application/pdf</key>                  <key id=\"isValid\">true</key>                  <key id=\"isWellFormed\">true</key>                </record>              </section>            </dnx>          </mets:xmlData>        </mets:mdWrap>      </mets:techMD>      <mets:rightsMD ID=\"FL53309-amd-rights\">        <mets:mdWrap MDTYPE=\"OTHER\" OTHERMDTYPE=\"dnx\">          <mets:xmlData>            <dnx xmlns=\"http://www.exlibrisgroup.com/dps/dnx\"/>          </mets:xmlData>        </mets:mdWrap>      </mets:rightsMD>      <mets:digiprovMD ID=\"FL53309-amd-digiprov\">        <mets:mdWrap MDTYPE=\"OTHER\" OTHERMDTYPE=\"dnx\">          <mets:xmlData>            <dnx xmlns=\"http://www.exlibrisgroup.com/dps/dnx\">              <section id=\"event\">                <record>                  <key id=\"eventDateTime\">2015-08-02 09:53:03</key>                  <key id=\"eventType\">VALIDATION</key>                  <key id=\"eventIdentifierType\">DPS</key>                  <key id=\"eventIdentifierValue\">27</key>                  <key id=\"eventOutcome1\">SUCCESS</key>                  <key id=\"eventOutcomeDetail1\">PROCESS_ID=;TASK_ID=1;MF_ID=11222779;STATUS=SUCCESS;ALGORITHM_NAME=MD5;IE_PID=IE53307;FILE_NAME=Archiving2012-CynthiaWu-Internal-Collab-Paper.pdf;COPY_ID=null;FILE_PID=FL53309;REP_PID=REP53308;SIP_ID=997;DATE=02 08 2015 09:53:03;DEPOSIT_ACTIVITY_ID=10452;PRODUCER_ID=163064;</key>                  <key id=\"eventDescription\">Fixity check performed on file</key>                  <key id=\"linkingAgentIdentifierType1\">SOFTWARE</key>                  <key id=\"linkingAgentIdentifierValue1\">REG_SA_JAVA5_FIXITY</key>                </record>                <record>                  <key id=\"eventDateTime\">2015-08-02 09:53:03</key>                  <key id=\"eventType\">VALIDATION</key>                  <key id=\"eventIdentifierType\">DPS</key>                  <key id=\"eventIdentifierValue\">27</key>                  <key id=\"eventOutcome1\">SUCCESS</key>                  <key id=\"eventOutcomeDetail1\">PROCESS_ID=;TASK_ID=1;MF_ID=11222779;STATUS=SUCCESS;ALGORITHM_NAME=SHA1;IE_PID=IE53307;FILE_NAME=Archiving2012-CynthiaWu-Internal-Collab-Paper.pdf;COPY_ID=null;FILE_PID=FL53309;REP_PID=REP53308;SIP_ID=997;DATE=02 08 2015 09:53:03;DEPOSIT_ACTIVITY_ID=10452;PRODUCER_ID=163064;</key>                  <key id=\"eventDescription\">Fixity check performed on file</key>                  <key id=\"linkingAgentIdentifierType1\">SOFTWARE</key>                  <key id=\"linkingAgentIdentifierValue1\">REG_SA_JAVA5_FIXITY</key>                </record>                <record>                  <key id=\"eventDateTime\">2015-08-02 09:53:03</key>                  <key id=\"eventType\">VALIDATION</key>                  <key id=\"eventIdentifierType\">DPS</key>                  <key id=\"eventIdentifierValue\">27</key>                  <key id=\"eventOutcome1\">SUCCESS</key>                  <key id=\"eventOutcomeDetail1\">PROCESS_ID=;TASK_ID=1;MF_ID=11222779;STATUS=SUCCESS;ALGORITHM_NAME=CRC32;IE_PID=IE53307;FILE_NAME=Archiving2012-CynthiaWu-Internal-Collab-Paper.pdf;COPY_ID=null;FILE_PID=FL53309;REP_PID=REP53308;SIP_ID=997;DATE=02 08 2015 09:53:03;DEPOSIT_ACTIVITY_ID=10452;PRODUCER_ID=163064;</key>                  <key id=\"eventDescription\">Fixity check performed on file</key>                  <key id=\"linkingAgentIdentifierType1\">SOFTWARE</key>                  <key id=\"linkingAgentIdentifierValue1\">REG_SA_JAVA5_FIXITY</key>                </record>                <record>                  <key id=\"eventDateTime\">2015-08-02 09:53:03</key>                  <key id=\"eventType\">VALIDATION</key>                  <key id=\"eventIdentifierType\">DPS</key>                  <key id=\"eventIdentifierValue\">24</key>                  <key id=\"eventOutcome1\">SUCCESS</key>                  <key id=\"eventOutcomeDetail1\">PROCESS_ID=;PID=FL53309;SIP_ID=997;DEPOSIT_ACTIVITY_ID=10452;MF_ID=11222779;TASK_ID=7;PRODUCER_ID=163064;</key>                  <key id=\"eventDescription\">Virus check performed on file</key>                  <key id=\"linkingAgentIdentifierType1\">SOFTWARE</key>                  <key id=\"linkingAgentIdentifierValue1\">REG_SA_DPS</key>                </record>                <record>                  <key id=\"eventDateTime\">2015-08-02 09:53:03</key>                  <key id=\"eventType\">VALIDATION</key>                  <key id=\"eventIdentifierType\">DPS</key>                  <key id=\"eventIdentifierValue\">25</key>                  <key id=\"eventOutcome1\">SUCCESS</key>                  <key id=\"eventOutcomeDetail1\">PROCESS_ID=;PID=FL53309;FILE_EXTENSION=pdf;SIP_ID=997;DEPOSIT_ACTIVITY_ID=10452;MF_ID=11222779;TASK_ID=48;IDENTIFICATION_METHOD=SIGNATURE;PRODUCER_ID=163064;FORMAT_ID=fmt/18;</key>                  <key id=\"eventDescription\">Format Identification performed on file</key>                  <key id=\"linkingAgentIdentifierType1\">SOFTWARE</key>                  <key id=\"linkingAgentIdentifierValue1\">REG_SA_DROID , Version 6.01 , Signature version Binary SF v.81/ Container SF v.1</key>                </record>                <record>                  <key id=\"eventDateTime\">2015-08-02 09:53:03</key>                  <key id=\"eventType\">VALIDATION</key>                  <key id=\"eventIdentifierType\">DPS</key>                  <key id=\"eventIdentifierValue\">165</key>                  <key id=\"eventOutcome1\">SUCCESS</key>                  <key id=\"eventOutcomeDetail1\">PROCESS_ID=;PID=FL53309;SIP_ID=997;DEPOSIT_ACTIVITY_ID=10452;MF_ID=11222779;TASK_ID=49;PRODUCER_ID=163064;</key>                  <key id=\"eventDescription\">Technical Metadata extraction performed on file</key>                  <key id=\"linkingAgentIdentifierType1\">SOFTWARE</key>                  <key id=\"linkingAgentIdentifierValue1\">JHOVE , PDF-hul 1.7 , Plugin Version 3.0</key>                </record>              </section>            </dnx>          </mets:xmlData>        </mets:mdWrap>      </mets:digiprovMD>    </mets:amdSec>    <mets:amdSec ID=\"ie-amd\">      <mets:techMD ID=\"ie-amd-tech\">        <mets:mdWrap MDTYPE=\"OTHER\" OTHERMDTYPE=\"dnx\">          <mets:xmlData>            <dnx xmlns=\"http://www.exlibrisgroup.com/dps/dnx\">              <section id=\"internalIdentifier\">                <record>                  <key id=\"internalIdentifierType\">SIPID</key>                  <key id=\"internalIdentifierValue\">997</key>                </record>                <record>                  <key id=\"internalIdentifierType\">PID</key>                  <key id=\"internalIdentifierValue\">IE53307</key>                </record>                <record>                  <key id=\"internalIdentifierType\">DepositSetID</key>                  <key id=\"internalIdentifierValue\">10452</key>                </record>              </section>              <section id=\"objectCharacteristics\">                <record>                  <key id=\"objectType\">INTELLECTUAL_ENTITY</key>                  <key id=\"creationDate\">2015-08-02 09:52:41</key>                  <key id=\"createdBy\">admin2</key>                  <key id=\"modificationDate\">2016-01-10 10:00:20</key>                  <key id=\"modifiedBy\">admin1</key>                  <key id=\"owner\">CRS00.INS00.DPR00</key>                </record>              </section>              <section id=\"generalIECharacteristics\">                <record>                  <key id=\"status\">ACTIVE</key>                  <key id=\"IEEntityType\">Article</key>                  <key id=\"Version\">6</key>                </record>              </section>              <section id=\"Collection\">                <record>                  <key id=\"collectionId\">16010782</key>                </record>              </section>            </dnx>          </mets:xmlData>        </mets:mdWrap>      </mets:techMD>      <mets:rightsMD ID=\"ie-amd-rights\">        <mets:mdWrap MDTYPE=\"OTHER\" OTHERMDTYPE=\"dnx\">          <mets:xmlData>            <dnx xmlns=\"http://www.exlibrisgroup.com/dps/dnx\">              <section id=\"accessRightsPolicy\">                <record>                  <key id=\"policyId\">AR_EVERYONE</key>                  <key id=\"policyDescription\">No restrictions</key>                </record>              </section>            </dnx>          </mets:xmlData>        </mets:mdWrap>      </mets:rightsMD>      <mets:digiprovMD ID=\"ie-amd-digiprov\">        <mets:mdWrap MDTYPE=\"OTHER\" OTHERMDTYPE=\"dnx\">          <mets:xmlData>            <dnx xmlns=\"http://www.exlibrisgroup.com/dps/dnx\">              <section id=\"producer\">                <record>                  <key id=\"address1\">4 Michigan Av.</key>                  <key id=\"address3\">Chicago</key>                  <key id=\"address4\">UnitedStates</key>                  <key id=\"defaultLanguage\">en</key>                  <key id=\"emailAddress\">ido.peled@exlibrisgroup.com</key>                  <key id=\"firstName\">INS00</key>                  <key id=\"lastName\">DPR00</key>                  <key id=\"telephone1\">03-9668990</key>                  <key id=\"authorativeName\">Ex Libris Demo Producer</key>                  <key id=\"producerId\">163064</key>                  <key id=\"userIdAppId\">163060</key>                  <key id=\"zip\">85675</key>                </record>              </section>              <section id=\"producerAgent\">                <record>                  <key id=\"firstName\">Victoria</key>                  <key id=\"lastName\">Holmes</key>                </record>              </section>              <section id=\"event\">                <record>                  <key id=\"eventDateTime\">2016-01-10 09:48:17</key>                  <key id=\"eventType\">ENRICHMENT</key>                  <key id=\"eventIdentifierType\">DPS</key>                  <key id=\"eventIdentifierValue\">401</key>                  <key id=\"eventOutcome1\">SUCCESS</key>                  <key id=\"eventOutcomeDetail1\">COLLECTION_NAME=scholar;COLLECTION_ID=16010782;PID=IE53307;</key>                  <key id=\"eventDescription\">IE has been added to collection</key>                  <key id=\"linkingAgentIdentifierType1\">USER</key>                  <key id=\"linkingAgentIdentifierValue1\">admin1</key>                </record>                <record>                  <key id=\"eventDateTime\">2016-01-10 10:00:20</key>                  <key id=\"eventType\">PROCESSING</key>                  <key id=\"eventIdentifierType\">DPS</key>                  <key id=\"eventIdentifierValue\">414</key>                  <key id=\"eventOutcome1\">SUCCESS</key>                  <key id=\"eventOutcomeDetail1\">IE_PID=IE53307;TASK_ID=62;PROCESS_ID=16011350;</key>                  <key id=\"eventDescription\">Representation metadata has been updated</key>                </record>              </section>            </dnx>          </mets:xmlData>        </mets:mdWrap>      </mets:digiprovMD>    </mets:amdSec>    <mets:fileSec>      <mets:fileGrp ID=\"REP53308\" ADMID=\"REP53308-amd\">        <mets:file ID=\"FL53309\" ADMID=\"FL53309-amd\">          <mets:FLocat LOCTYPE=\"URL\" xlin:href=\"/exlibris/dps/d4_1/profile/permanent/file/storage1/2015/08/02/file_1/V1-FL53309.pdf\" xmlns:xlin=\"http://www.w3.org/1999/xlink\"/>        </mets:file>      </mets:fileGrp>    </mets:fileSec> <mets:structMap ID=\"REP53308-1\" TYPE=\"LOGICAL\"><mets:div LABEL=\"REP53308-1 logical\"><mets:div LABEL=\"Table of Contents\"><mets:div LABEL=\"111.pdf\" TYPE=\"FILE\"><mets:fptr FILEID=\"FL0001\"/></mets:div><mets:div LABEL=\"222.pdf\" TYPE=\"FILE\"><mets:fptr FILEID=\"FL0002\"/></mets:div><mets:div LABEL=\"333.pdf\" TYPE=\"FILE\"><mets:fptr FILEID=\"FL0003\"/></mets:div></mets:div></mets:div></mets:structMap><mets:structMap ID=\"REP53308-2\" TYPE=\"PHYSICAL\"><mets:div LABEL=\"REP53308-2 physical\"><mets:div LABEL=\"Table of Contents\"><mets:div LABEL=\"111.pdf\" TYPE=\"FILE\"><mets:fptr FILEID=\"FL0001\"/></mets:div><mets:div LABEL=\"222.pdf\" TYPE=\"FILE\"><mets:fptr FILEID=\"FL0002\"/></mets:div><mets:div LABEL=\"333.pdf\" TYPE=\"FILE\"><mets:fptr FILEID=\"FL0003\"/></mets:div></mets:div></mets:div></mets:structMap><mets:structMap ID=\"REP53308-3\" TYPE=\"LOGICAL\"><mets:div LABEL=\"REP53308-3 logical\"><mets:div LABEL=\"Table of Contents\"><mets:div LABEL=\"111.pdf\" TYPE=\"FILE\"><mets:fptr FILEID=\"FL0001\"/></mets:div><mets:div LABEL=\"222.pdf\" TYPE=\"FILE\"><mets:fptr FILEID=\"FL0002\"/></mets:div><mets:div LABEL=\"333.pdf\" TYPE=\"FILE\"><mets:fptr FILEID=\"FL0003\"/></mets:div></mets:div></mets:div></mets:structMap>  </mets:mets>";

			String tmp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><mets:mets xmlns:mets=\"http://www.loc.gov/METS/\">";
			String mapsections = soapresponse.substring(soapresponse.indexOf("<mets:structMap"),soapresponse.lastIndexOf("</mets:structMap>")) + "</mets:structMap>"; //get all structure maps
			//check if structure map with ID and type exists
			if (!mapsections.contains("TYPE=\"" + type.toUpperCase()) && !mapsections.contains("ID=\"" + repid)) {
				String info = inforepidormid + " - ERROR (Couldn't get structure map XML - tool stopped processing)";
				stop_with_message(info);
			}
				//create string array of structure map sections
				mapsections = mapsections.replaceAll("> .*?<", "><");
				mapsections = mapsections.replaceAll("</mets:structMap><mets:structMap", "</mets:structMap>§§§§§<mets:structMap");
				String[] mapsectionslist = mapsections.split("§§§§§");
				if (physical) {
					if (mapsectionslist.length>0) {
						for (int i=0; i<mapsectionslist.length; i++) {
							String mapsection = mapsectionslist[i];
							if (mapsection.contains("TYPE=\"PHYSICAL\"") && mapsection.contains("ID=\"" + repid)) {//select only type 'physical'
								structmap = mapsection;
								break;
							}
						}
					} else {
						structmap = mapsections;
					}
				} else {//logical structure map
					if (mapsectionslist.length>0) {
						String version = "";
						int latestversion = 0;
						for (int i=0; i<mapsectionslist.length; i++) {//find latest version
							version = mapsectionslist[i];
							if (version.contains("TYPE=\"LOGICAL\"") && version.contains("ID=\"" + repid)) {//select only type 'physical'
								version = version.substring(0, version.indexOf("ID=")+40).replaceFirst(".*ID=\"", "").replaceFirst("\".*", "");
								version = version.replaceFirst("-MID.*", "").replaceFirst(".*-", "");//if responded ID is like 'REP12596-1-MID-REP12596-1' -> gets '1'
								if (Integer.parseInt(version) > latestversion) {
									latestversion = Integer.parseInt(version);
								}
							}
						}
						repid = repid + "-" + Integer.toString(latestversion);
						for (int i=0; i<mapsectionslist.length; i++) {
							String mapsection = mapsectionslist[i];
							if (mapsection.contains("ID=\"" + repid)) {//select only latest version
								structmap = mapsection;
								break;
							}
						}
					} else {
						structmap = mapsections;
					}
				}
				structmap = tmp + structmap	+ "</mets:mets>";
			//write response to xml into folder <downloadedxml>
			writeFile(downloadedxml, repid + ".xml", structmap);
		} 
		catch (Exception e) {
			if (debug) {
				String info = "ERROR - " + e.getMessage();
				add_loginfo(info, true);
			}
			String info = inforepidormid + " - ERROR (Couldn't get structure map XML)";
			add_loginfo(info, true);
			ToolMenu.f.setCursor(Cursor.getDefaultCursor());
			return null;
		}
		return repid;
	}
	

	private static void stop_with_message(String info) throws Exception
	{
		add_loginfo(info, true);
		ToolMenu.f.setCursor(Cursor.getDefaultCursor());
		throw new Exception(info);
	}
	

	/*
	 * convert downloaded XML in folder <downloadedxml> to CSV
	 */
	private static void convert_xml_to_csv(String repid, String type) throws Exception
	{
		//convert XML to CSV
		ArrayList<String> alFilenameId = new ArrayList<String>(); //single string with iteration of file id and name
		ArrayList<String> csvLineFromXmlFile = new ArrayList<String>();
		String pathtoxmlfile = downloadedxml + repid + ".xml";	
		if (type.equalsIgnoreCase("logical")) {
			ConvertLogicalXmlToCsv.depthxml = 0;	
			csvLineFromXmlFile = ConvertLogicalXmlToCsv.convertLogicalXmlToCsv(pathtoxmlfile,type);
			if (csvLineFromXmlFile != null) {
				build_and_write_csv(repid, csvLineFromXmlFile, type, null);
				csvLineFromXmlFile = null;
			}
		}
		if (type.equalsIgnoreCase("physical")) {
			ConvertLogicalXmlToCsv.depthxml = 0;	
			try {
					NodeList childNodes = ConvertLogicalXmlToCsv.getNodeList(pathtoxmlfile, type).item(0).getChildNodes();
					boolean gotdepthxml = ConvertLogicalXmlToCsv.getMaxDepthXML(childNodes, 0);//get hierarchy level
					if (gotdepthxml) {
						alFilenameId = SoapRequestClient.getFileNameIdFromPhysicalStructMap(pathtoxmlfile, repid);
					}
				} catch (Exception e) {
					String info = "";
					if (e.getMessage() != null) {
						info = "ERROR - " + e.getMessage();
					} else {
						info = "ERROR - " + "Data for " + repid + " could not be received - tool stopped processing. "
								+ "Please check if the requested structure map exists.";
					}
					add_loginfo(info, true);
					ToolMenu.f.setCursor(Cursor.getDefaultCursor());
					throw new Exception("Data for " + repid + " could not be received - tool stopped processing");
				}
//				System.out.println("pathtoxmlfile (physical): "+pathtoxmlfile);
//				System.out.println("depthxml (physical): "+ConvertLogicalXmlToCsv.depthxml);
			build_and_write_csv(repid, null, type, alFilenameId);
		}
	}
	
	
	/*
	 * check files in <downloadedxml> and initiate convert_xml_to_csv()
	 */
	static void run_convert_xml_to_csv(String downloadedxml) throws Exception
	{
		ArrayList<String> fileList = list_files(downloadedxml);//list of files prefixed by type
		String repid = "";
		String type = "";
		String info = "Writing into " + csvfromxml;
		add_loginfo(info, true);
		
		for (String file : fileList) {
			repid = file.substring(file.indexOf("_")+1,file.indexOf(".xml"));
			type = file.substring(0,file.indexOf("_"));
			convert_xml_to_csv(repid, type);
		}
	}
	
	
	private static ArrayList<String> list_files(String downloadedxml) throws IOException {
		ArrayList<String> fileList = new ArrayList<>();
		try (DirectoryStream<Path> directory = Files.newDirectoryStream(Paths.get(downloadedxml))) {
			for (Path file : directory) {
				if (!Files.isDirectory(file)) {
					String filename = file.getFileName().toString();
					if (filename.matches("^REP[0-9].*\\.xml$")) {
						if (filename.matches("^REP[0-9]*-[0-9]*\\.xml$")) {
							filename = "logical_" + filename;
						}
						if (filename.matches("^REP[0-9]*\\.xml$")) {
							filename = "physical_" + filename;
						}
						fileList.add(filename);
					} else continue;
				}
			}
		}
		return fileList;
	}
	
	/*
	 * read file with REP IDs (one per line)
	 * start creation of CSV files
	 */
	static void bulkcreate_csv(String repidsfile, boolean fromphysical) throws Exception
	{
		boolean worked = true;
		try {
			if (repidsfile != null) {
				BufferedReader reader = new BufferedReader(new FileReader(repidsfile));
				String id;
				String info = "Writing into " + csvfromxml;
				add_loginfo(info, true);
				while ((id = reader.readLine()) != null) {
					id = id.toUpperCase();
					if (fromphysical) {
						create_csv_from_physical_structmap(id);
					} else {
						if (id.matches("REP[0-9].*-[0-9].*")) { //id is representation mid
							worked = create_csv_from_logical_structmap(null, id);
							if (!worked) continue;
						} else if (id.matches("REP[0-9].*")) { //id is representation id
							worked = create_csv_from_logical_structmap(id, null);
							if (!worked) continue;
						} else {
							continue;
						}
					}
				}
				reader.close();
			} else {
				String info = "ERROR - File " + repidsfile + " could not be found - tool stopped processing";
				add_loginfo(info, true);
				ToolMenu.f.setCursor(Cursor.getDefaultCursor());
				throw new Exception(info);
			}
		} catch (Exception e) {
			String info = "ERROR - " + e.getMessage();
			add_loginfo(info, true);
		}
	}
	
	
	public static void main(String[] args) {

		org.apache.log4j.helpers.LogLog.setQuietMode(true);
		CreateRosettaLogStructMap myself = null;
		try {
			myself = new CreateRosettaLogStructMap();
			myself.get_parameters();
			if (windows) {
				try {
					new ToolMenu();
				} catch (Throwable e) {
						logger.error("ERROR: " + e.getLocalizedMessage());
				}
			} //else {
				//start_unix_dialog();
			//}
		} catch (Exception e) {
			if (myself != null) {
				myself.format_exception(e);
				if (windows) {
					logger.error("ERROR: The log window (JFrame) couldn't be started. Please check the configuration, e.g. that all mandatory elements exist");
					logger.error("ERROR: " + e.getLocalizedMessage());
					if (ToolMenu.getFrames()!=null) ToolMenu.showInfo("ERROR: " + e.getLocalizedMessage(), true);
				}
			}
			else {
				logger.error(e.getLocalizedMessage()); 
				if (windows) {
					logger.error("ERROR: The log window (JFrame) couldn't be started. Please check the configuration, e.g. that all mandatory elements exist");
					logger.error("ERROR: " + e.getLocalizedMessage());
					if (ToolMenu.getFrames()!=null) ToolMenu.showInfo("ERROR: " + e.getLocalizedMessage(), true);
				}
			}
		}
	}
}

