package fr.lab.lissi.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import cleode.Cleodes;
import cleode.TestZCL;
import fr.lab.lissi.general.Constants;

public class ServletContextClass implements ServletContextListener {

	private static Properties myProperties = new Properties();
	private String deviceId = "deviceId";




	public void contextInitialized(ServletContextEvent servletContextEvent) {

		//################### DONT CHANGE THIS CODE - BEGIN ###################
		Constants.APP_ROOT_PATH = servletContextEvent.getServletContext().getRealPath("/").trim();

		// verfiy if config file exist
		File f = new File(Constants.APP_ROOT_PATH + Constants.CONFIG_FILE_PATH);
		if (!f.exists()) {
			System.err.println("ERROR : Configuration file '" + f.getName() + "' doesn't exist.");
			return;
		}
		Constants.CONFIG_FILE_PATH = Constants.APP_ROOT_PATH + Constants.CONFIG_FILE_PATH;
		
		try {
			myProperties.load(new FileInputStream(Constants.CONFIG_FILE_PATH));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		deviceId = myProperties.getProperty("deviceID");
		
		System.out.println( deviceId +" service init start.".toUpperCase());
		System.out.println(deviceId + " Configuration file ok.".toUpperCase()+"->"+Constants.CONFIG_FILE_PATH);
		
		//################### DONT CHANGE THIS CODE - END ###################
		
		
		//################### Do your initialisation here ###################
	

		Cleodes cleodes= Cleodes.getInstance();

	}// end contextInitialized method
	
	
	

	public void contextDestroyed(ServletContextEvent arg0) {
		//################### Do your cleanning here ###################
		
		
		System.out.println(deviceId + " service stopped".toUpperCase());
	}// end constextDestroyed method

}
