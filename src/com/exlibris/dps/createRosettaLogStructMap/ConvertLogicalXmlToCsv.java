package com.exlibris.dps.createRosettaLogStructMap;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ConvertLogicalXmlToCsv {

	public static int depthxml = 0;
	private static String csvLineFromXml02 = new String();
	private static ArrayList<String> csvLineFromXml01 = new ArrayList<>();

	private static void printNode(NodeList nodeList, int level) {
		level++;
		if (nodeList != null && nodeList.getLength() > 0) {
			String fileid = "";
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					if (node.getAttributes()!=null) {
						if (node.getAttributes().getNamedItem("FILEID")!=null) {
							fileid = node.getAttributes().getNamedItem("FILEID").getNodeValue();
						}
					}
					if (level > 0) {
						String twodigitlevel = String.format("%02d", level);
						if (node.getAttributes().getNamedItem("LABEL")!=null) {
							String labeltmp = node.getAttributes().getNamedItem("LABEL").getNodeValue();
							String tmp = String.format(
									"%s%s::::%s;;;;%n",twodigitlevel, labeltmp, fileid);
							csvLineFromXml01.add(tmp);
						} else {
							String tmp = String.format(
									"%s%s::::%s;;;;%n",twodigitlevel, node.getAttributes().getNamedItem("LABEL"), fileid);
							csvLineFromXml01.add(tmp);
						}
					}
					printNode(node.getChildNodes(), level);
				}
			}
		}
	}

	public static boolean getMaxDepthXML(NodeList nodeList, int level) {
		level++;
		if (nodeList != null && nodeList.getLength() > 0) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					getMaxDepthXML(node.getChildNodes(), level);
					if (level > depthxml) {
						depthxml = level;
					}
				}
			}
		}
		boolean gotdepthxml = true;
		return gotdepthxml;
	}

	public static NodeList getNodeList(String xmlinput, String type) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		NodeList nl = null;
		try (InputStream is = new FileInputStream(xmlinput)) {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);
			
			/*
			 * extract nodes below 
			 * <?xml version="1.0" encoding="UTF-8"?>
			 *   <mets:structMap ID="REP11599-1-MID-REP11599-1" TYPE="LOGICAL">
			 */
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			XPathExpression expr = xpath.compile("//*[local-name()='mets']/*[local-name()='structMap'][@TYPE=\""+type.toUpperCase()+"\"]");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET); //nodes of physical structure map
		}
		catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
			e.printStackTrace();
		}
		return nl;
	}

	public static ArrayList<String> convertLogicalXmlToCsv(String pathtoxmlfile, String type) {
		NodeList childNodes = getNodeList(pathtoxmlfile, type).item(0).getChildNodes();
		boolean gotdepthxml = getMaxDepthXML(childNodes, 0);
		if (gotdepthxml) {
			csvLineFromXml01.clear();
			printNode(childNodes, 0);
		}
		for (String tmp : csvLineFromXml01) {
			tmp = tmp.replace("null::::", "::::");
			tmp = tmp.replace("::::;;;;", "::::");
			tmp = tmp.replace("::::FL", "FL");
			csvLineFromXml02 = csvLineFromXml02 + tmp;
		}
		String[] csvLineFromXml03 = csvLineFromXml02.split(";;;;");
		//create array of columns
		ArrayList<String[]> csvLineFromXml04 = new ArrayList<>();
		for (int i=0; i<csvLineFromXml03.length; i++) {
			String[] tmp = csvLineFromXml03[i].toString().split("::::"); //toString() is essential for splitting
			if (tmp.length>1) csvLineFromXml04.add(tmp); //without this condition, an empty line would be added to the end of the list
		}
		//put cell in hyphens if contains comma ','
		ArrayList<String[]> csvLineFromXml05 = new ArrayList<>();
		for (String[] tmp : csvLineFromXml04) {
			String[] tmpxnew = new String[tmp.length];
			for (int j=0;j<tmp.length;j++) {
				String tmpj = tmp[j].trim(); //trim() is essential to remove some invisible characters from start of string (removes line breaks)
				if (tmpj.length()>=1 && tmpj.contains(",")) {
					tmpj = "\"" + tmpj + "\""; // put cell in hyphens if contains comma ','
				}
				tmpxnew[j] = tmpj;
			}
			csvLineFromXml05.add(tmpxnew);
 		}

		//rearrange data from csvLineFromXml05 into csvLineFromXmlFile
		/*
		 * Example:
			csvLineFromXml05: [011, 0211, 03111, 04111f1, 05FL12110]
			csvLineFromXml05: [04111f2, 05FL12111]
			csvLineFromXml05: [0212-f, 03FL12117]            --> file ID not in last position
			csvLineFromXml05: [0213-f, 03FL12118]            --> file ID not in last position
			csvLineFromXml05: [0214, 0314f1, 04FL12119]      --> file ID not in last position
			
			maximal depth (depthxml): 5
			--> that means, all file IDs must get prefix 05 to be placed in last position
		 */

		ArrayList<String> csvLineFromXmlFile = new ArrayList<>();
		for (String[] tmp : csvLineFromXml05) {
			if (depthxml >= 2) {
				String[] newtmp = new String[depthxml]; //create string array in the size of node depth of XML
				int prefix = 0;
				boolean commaexists = false;

				for (int j=0;j<tmp.length;j++) {
					if (tmp[j] != null) {
//						System.out.println("tmp["+(j)+"]: " + tmp[j]);
						commaexists = false;
						String tmpj = tmp[j].trim(); //trim() is essential to remove some invisible characters from start of string
						//get prefix (01,02,...,tmp.length)
						if (tmpj.startsWith("\"")) {
							prefix = Integer.parseInt(tmpj.substring(1,3))-1; //e.g. original prefix 03 becomes 2
							commaexists = true;
						} else {
							prefix = Integer.parseInt(tmpj.substring(0,2))-1;
//							System.out.println("tmp["+(j)+"] prefix: " + prefix);
						}
						if (commaexists) {
							tmpj = "\"" + tmpj.substring(3); // remove prefix but keep "
						} else {
							tmpj = tmpj.substring(2); // remove prefix
//							System.out.println("tmp["+(j)+"] without prefix: " + tmpj);
						}

						if (j < (tmp.length-1)) { // all entries but last
								newtmp[prefix] = tmpj;
						}
						if (j == (tmp.length-1)) { // last entry (file ID)
							newtmp[newtmp.length-1] = tmpj; // put into last position of newtmp[]  
						}
					}
				}
//				System.out.println("newtmp: "+Arrays.toString(newtmp));


				for (int k=0;k<newtmp.length-1;k++) {
					if (newtmp[k]==null) {
						newtmp[k] = ",";
					} else {
						newtmp[k] = newtmp[k] + ",";
					}
				}
					StringBuilder builder = new StringBuilder ();
					String csvfileline;

					for (String s: newtmp) {
						builder.append(s);
					}
					csvfileline = builder.toString();
					csvLineFromXmlFile.add(csvfileline);
			}
		}			
		
//		TEST
//		System.out.println("pathtoxmlfile: "+pathtoxmlfile);
//		System.out.println("depthxml (logical): "+depthxml);
//		for (String tmp : csvLineFromXml01) {
//			System.out.println("csvLineFromXml01: "+tmp);
//		}
//		for (int i=0; i<csvLineFromXml03.length; i++) {
//			String tmp = csvLineFromXml03[i].toString();
//			System.out.println("csvLineFromXml03: "+tmp);
//		}
//		for (String[] tmp : csvLineFromXml04) {
//			for (int j=0;j<tmp.length;j++) {
//				System.out.println("csvLineFromXml04: "+tmp[j]);
//		System.out.println("***********");
//			}
//		}
//		for (String[] tmp : csvLineFromXml04) {
//				System.out.println("csvLineFromXml04: "+Arrays.toString(tmp));
//		}
//		for (String[] tmp : csvLineFromXml05) {
//			for (int j=0;j<tmp.length;j++) {
//				System.out.println("csvLineFromXml05: "+tmp[j]);
//			}
//			System.out.println("-----");
//		}
//		for (String[] tmp : csvLineFromXml05) {
//			System.out.println("csvLineFromXml05: "+Arrays.toString(tmp));
//		}
		
		//set all temp arrays to null
		csvLineFromXml02 = new String();
		csvLineFromXml04 =  new ArrayList<>();
		csvLineFromXml05 =  new ArrayList<>();
		return csvLineFromXmlFile;
	}

	public static void main(String args[]) {
	}
}