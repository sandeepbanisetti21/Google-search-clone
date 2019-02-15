package com.src.listener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.src.service.HttpClient;

@WebListener
public class ApplicatonListener implements ServletContextListener {

	public static Map<String, String> csvFileContens = new HashMap<>();
	private static final String csvFilePath = "../URLtoHTML_latimes.csv";

	public static HttpClient httpclient = new HttpClient();

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		extractCsvFile();
		initilizeHttpClient();

		System.out.println("Application is started.");
	}

	private static void extractCsvFile() {
		String absolute = ApplicatonListener.class.getClassLoader().getResource("").getPath();
		System.out.println(absolute);
		BufferedReader br = null;
		String line = "";
		try {
			// br = new BufferedReader(new FileReader(absolute+File.separator+csvFilePath));
			br = new BufferedReader(new FileReader(absolute + File.separator + csvFilePath));

			while ((line = br.readLine()) != null) {
				String[] names = line.split(",");
				csvFileContens.put(names[0], names[1]);
			}
			System.out.println("Read " + csvFileContens.size() + " html entries.");
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	private static void initilizeHttpClient() {
		httpclient.initialize();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {

		System.out.println("Application is being stopped.");
	}

	public static void main(String[] args) {
		extractCsvFile();
	}

}
