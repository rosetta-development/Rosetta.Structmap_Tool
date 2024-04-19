//package com.exlibris.dps.createRosettaLogStructMap;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//import org.apache.logging.log4j.core.appender.FileAppender;//.log4j.FileAppender;
//import org.apache.logging.log4j.core.config.plugins.Plugin;
//
//public class CustomFileAppender extends FileAppender {
////@Plugin(name = "CustomFileAppender", category = "Core", 
////elementType = "appender", printObject = true)
////public final class CustomFileAppender extends FileAppender {
// @Override
// public void setFile(String fileName) {
//  if (fileName.indexOf("%timestamp") >= 0) {
//   Date d = new Date();
//   SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
//   fileName = fileName.replaceAll("%timestamp", format.format(d));
//  }
//  super.setFile(fileName);
// }
//}
