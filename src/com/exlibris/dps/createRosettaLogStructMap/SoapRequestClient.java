package com.exlibris.dps.createRosettaLogStructMap;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SoapRequestClient {

	private static ArrayList<String> fileidFilename = new ArrayList<String>();
	public static ArrayList<String> repLabels = new ArrayList<>();

	static String getIeXmlViaSoap(String wsurl, String user, String password, String institution, String iepid) throws MalformedURLException, IOException {
		//HTTP request
		String responseString = "";
		String outputString = "";
		URL url = new URL(wsurl);
		URLConnection connection = url.openConnection();
		HttpURLConnection httpConn = (HttpURLConnection)connection;

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		String xmlOutput = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">"+
				"<Body><getIE xmlns=\"http://dps.exlibris.com/\"><pdsHandle xmlns=\"\"></pdsHandle><iePid xmlns=\"\">"+
				iepid+
				"</iePid><flags xmlns=\"\">2</flags></getIE></Body></Envelope>";
		/*
		 * flags -
           0 - current IE version, including reps and files without derivative copy representations
           1 - include all revisions and not just the latest (deprecated)
           2 - exclude the files 
           4 - enrich with shared CMS metadata
           8 - enrich with shared access rights metadata
           16 - include minimal file information (file size, mimetype and FLocat)
           32 - current IE version, including reps and files with derivative copy representations
		 */
		
		//System.out.println("xmlOutput: "+xmlOutput);
		byte[] buffer = new byte[xmlOutput.length()];
		buffer = xmlOutput.getBytes();
		outputStream.write(buffer);
		byte[] b = outputStream.toByteArray();

		//set HTTP parameters
		/*	  SOAP action MUST NOT(!) be used:	    httpConn.setRequestProperty("SOAPAction", ...); */
		httpConn.setReadTimeout(120000); //set timeout to 2 minutes
		httpConn.setRequestProperty("Accept", "application/xml");
		httpConn.setRequestProperty("Authorization",
				"Basic " + Base64.getEncoder().encodeToString((user+"-institutionCode-"+institution+":"+password).getBytes()));

		httpConn.setRequestProperty("Content-Length",String.valueOf(b.length));
		httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		httpConn.setRequestMethod("POST");
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		OutputStream out = httpConn.getOutputStream(); //write content of request to output stream of HTTP connection
		out.write(b);
		out.close(); //request sent

		//read response
		InputStreamReader isr = null;
		if (httpConn.getResponseCode() == 200) {
			isr = new InputStreamReader(httpConn.getInputStream());
		} else {
			isr = new InputStreamReader(httpConn.getErrorStream());
		}

		BufferedReader in = new BufferedReader(isr);

		//write response to a string
		while ((responseString = in.readLine()) != null) {
			outputString = outputString + responseString;
		}
		if (!outputString.isEmpty()) {
			try {
				outputString = extractSectionFromXml(outputString, "getIE");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return outputString;
	}


	static String getSruResponse(String sruurl, String repid) throws MalformedURLException, IOException {

		//HTTP request
		String responseString = "";
		String outputString = "";
		URL url = new URL(sruurl + repid);
		URLConnection connection = url.openConnection();
		HttpURLConnection httpConn = (HttpURLConnection)connection;
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		OutputStream out = httpConn.getOutputStream(); //write content of request to output stream of HTTP connection
		out.close(); //request sent

		//read response
		InputStreamReader isr = null;
		if (httpConn.getResponseCode() == 200) {
			isr = new InputStreamReader(httpConn.getInputStream());
		} else {
			isr = new InputStreamReader(httpConn.getErrorStream());
		}

		BufferedReader in = new BufferedReader(isr);

		//write response to a string
		while ((responseString = in.readLine()) != null) {
			outputString = outputString + responseString;
		}
		return outputString;
	}


	/* 
	 * extract XML section from SOAP response
	 * either 'getIE' (for physical structure map) or 'getMD' (for logical)
	 */
	private static String extractSectionFromXml(String xml, String element) throws Exception
	{
		String xmlsection = new String();
		if (!xml.contains("<soap:Fault>")) {

			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setNamespaceAware(true);
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new InputSource(new StringReader(xml))); 

			NodeList nl = doc.getElementsByTagName(element);

			if (nl != null) {
				xmlsection = nl.item(0).getTextContent();
			}
		}
		return xmlsection;
	}

	/* 
	 * extract physical structure map from IE XML for given REP ID
	 * build array of filename and ID
	 */
	static ArrayList<String> getFileNameIdFromPhysicalStructMap(String pathtoxmlfile, String repId) throws Exception
	{
		//clear for batch processing
		fileidFilename.clear();
		repLabels.clear();
		
		NodeList nl = ConvertLogicalXmlToCsv.getNodeList(pathtoxmlfile, "physical"); //nodes of physical structure map
		Node root = nl.item(0);
		//check if REP ID of structure map is the requested representation
//		if (root.getAttributes().getNamedItem("ID").getNodeValue().equalsIgnoreCase(repId+"-1")) {
		if (root.getAttributes().getNamedItem("TYPE").getNodeValue().equalsIgnoreCase("PHYSICAL")) {
			printTags(root, true);
		}
		return fileidFilename;   
	}


	/*
	 * build two lists: repLabels and fileidFilename
	 */
	public static void printTags(Node nodes, boolean from_physical){
		String fileid = null;
		String filename = null;
		Node nlabel;
		Node nfileid;
		Node ntype;
		if(nodes.hasChildNodes()  || nodes.getNodeType()!=3){
			nlabel = nodes.getAttributes().getNamedItem("LABEL");
			nfileid = nodes.getAttributes().getNamedItem("FILEID");
			ntype = nodes.getAttributes().getNamedItem("TYPE");
			if (nlabel!=null && ntype==null) {
				String repLabel = nodes.getAttributes().getNamedItem("LABEL").getNodeValue();
				repLabels.add(repLabel);
			}
			if (from_physical && nlabel!=null && ntype!=null) {
				filename = nodes.getAttributes().getNamedItem("LABEL").getNodeValue();
				fileidFilename.add(filename);
			}
			if (nfileid!=null) {
				fileid = nodes.getAttributes().getNamedItem("FILEID").getNodeValue();
				fileidFilename.add(fileid);
			}
			
			NodeList nl=nodes.getChildNodes();
			for(int j=0;j<nl.getLength();j++) {
                printTags(nl.item(j), true);
			}
		}
	}

	static String addMdViaSoap(String wsurl, String user, String password, String institution, 
			String objectid, String contentxml, String commit, String type, String subType)
					throws MalformedURLException, IOException, Exception {
		//HTTP request
		String responseString = new String();
		String outputString = new String();
		URL url = new URL(wsurl);
		URLConnection connection = url.openConnection();
		HttpURLConnection httpConn = (HttpURLConnection)connection;

		// ADDING new logical structure map (starting point was physical structure map):
		// - <mid> MUST NOT exist for adding a structure map, otherwise error in server.log: "updateMD has thrown exception, unwinding now: 
		//   com.exlibris.core.sdk.exceptions.InvalidMIDException: Cannot Update IE, PID:REP53308 and MetaData, MID: since There isnt any MetaData with specified MID and Type"
		// - <PID> == objectid, e.g. REP1000 
		// UPDATING logical structure map (starting point was logical structure map):
		// - <mid> == objectid, e.g. REP2000-3
		// - <PID> == REP ID part of objectid, e.g. REP2000 

		String envelope = "";
		String envelope_mid = "";
		String envelope_pid = "";
		if (objectid.contains("-")) {
			envelope_mid = "<mid>" + objectid + "</mid>";
			envelope_pid = "<PID xmlns=\"\">" + objectid.substring(0, objectid.lastIndexOf("-")) + "</PID>";
		} else {
			envelope_mid = "";
			envelope_pid = "<PID xmlns=\"\">" + objectid + "</PID>";
		}
		String envelope_part1 = 
				"<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">"
				+"<Body>"
				+ "<updateMD xmlns=\"http://dps.exlibris.com/\">"
				+  "<commit xmlns=\"\">" + commit + "</commit>" //should better be 'true' because otherwise IEs will be locked until confirmation
				+  "<metadata xmlns=\"\">"
				+   "<content><![CDATA[" + contentxml + "]]></content>" //actual MD XML must be in CDATA
				;
		String envelope_part2 = 
				"<type>" + type + "</type>"
				+   "<subType>" + subType + "</subType>"
				+  "</metadata>"
				;
		String envelope_part3 = 
				"<pdsHandle xmlns=\"\"></pdsHandle>" // is empty because of '_Basic_ authentication'
				+ "</updateMD>"
				+"</Body></Envelope>";
		
		envelope = envelope_part1 + envelope_mid + envelope_part2 + envelope_pid + envelope_part3;

		//set HTTP parameters
		/*	  SOAP action MUST NOT(!) be used:	    httpConn.setRequestProperty("SOAPAction", ...); */
		httpConn.setRequestProperty("Accept", "application/xml");
		httpConn.setRequestProperty("Authorization",
				"Basic " + Base64.getEncoder().encodeToString((user+"-institutionCode-"+institution+":"+password).getBytes()));
		httpConn.setRequestProperty("Content-Length",String.valueOf(envelope.length()));
		httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		httpConn.setRequestMethod("POST");
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpConn.getOutputStream(),"UTF-8"); //send request as UTF-8
		outputStreamWriter.write(envelope);
		outputStreamWriter.close(); //request has been sent
		

		//read response
		InputStreamReader isr = null;
		if (httpConn.getResponseCode() == 200) {
			isr = new InputStreamReader(httpConn.getInputStream());
		} else {
			isr = new InputStreamReader(httpConn.getErrorStream());
		}

		BufferedReader in = new BufferedReader(isr);

		//write response to a string
		while ((responseString = in.readLine()) != null) {
			outputString = outputString + responseString;
		}
		return outputString;
	}


	static String getLogicalStructmapViaSoap(String wsurl, String user, String password, String institution, String repid, String repmid) throws MalformedURLException, IOException {
		//HTTP request
		String responseString = "";
		String outputString = "";
		String type = "mets_section";
		String subType = "structMap";
		URL url = new URL(wsurl);
		URLConnection connection = url.openConnection();
		HttpURLConnection httpConn = (HttpURLConnection)connection;

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		String xmlOutput = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">"
		        +"<Body>"
				+ "<getMD xmlns=\"http://dps.exlibris.com/\">";
		if (repmid != null) {
			xmlOutput = xmlOutput + "<mid xmlns=\"\">" + repmid + "</mid>";
		}
		
		xmlOutput = xmlOutput
				+   "<PID xmlns=\"\">" + repid + "</PID>"
				+   "<pdsHandle xmlns=\"\"></pdsHandle>"
				+   "<type xmlns=\"\">" + type + "</type>"
				+   "<subType xmlns=\"\">" + subType + "</subType>"
				+ "</getMD>"
				+"</Body></Envelope>";

		if (CreateRosettaLogStructMap.debug) {
			CreateRosettaLogStructMap.logger.debug("getLogicalStructmapViaSoap - request:");
			CreateRosettaLogStructMap.logger.debug(xmlOutput);
		}
		
		byte[] buffer = new byte[xmlOutput.length()];
		buffer = xmlOutput.getBytes();
		outputStream.write(buffer);
		byte[] b = outputStream.toByteArray();

		//set HTTP parameters
		/*	  SOAP action MUST NOT(!) be used:	    httpConn.setRequestProperty("SOAPAction", ...); */
		httpConn.setReadTimeout(120000); //set timeout to 2 minutes
		httpConn.setRequestProperty("Accept", "application/xml");
		httpConn.setRequestProperty("Authorization",
				"Basic " + Base64.getEncoder().encodeToString((user+"-institutionCode-"+institution+":"+password).getBytes()));

		httpConn.setRequestProperty("Content-Length",String.valueOf(b.length));
		httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		httpConn.setRequestMethod("POST");
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		OutputStream out = httpConn.getOutputStream(); //write content of request to output stream of HTTP connection
		out.write(b);
		out.close(); //request sent

		//read response
		InputStreamReader isr = null;
		if (httpConn.getResponseCode() == 200) {
			isr = new InputStreamReader(httpConn.getInputStream());
		} else {
			isr = new InputStreamReader(httpConn.getErrorStream());
		}

		BufferedReader in = new BufferedReader(isr);

		//write response to a string
		while ((responseString = in.readLine()) != null) {
			outputString = outputString + responseString;
		}
		
		if (CreateRosettaLogStructMap.debug) {
			CreateRosettaLogStructMap.logger.debug("getLogicalStructmapViaSoap - response:");
			CreateRosettaLogStructMap.logger.debug(outputString);
		}
		
		if (!outputString.isEmpty()) {
			try {
				if (!outputString.contains("<soap:Fault>")) {
					outputString = extractSectionFromXml(outputString, "getMD");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return outputString;
	}


	  public static void main(String args[]) {
	}
}