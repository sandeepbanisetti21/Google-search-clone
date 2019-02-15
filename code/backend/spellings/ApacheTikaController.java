package com.src.spellings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class ApacheTikaController {

	public static void main(String[] args) throws IOException, SAXException, TikaException {
		File htmlDir = new File("/home/sandeep/Desktop/3rd Semester/IR - csci 572/Assignments/Assignment4/latimes");
		StringBuffer sb = new StringBuffer();
		int count = 0;
		for (File file : htmlDir.listFiles()) {
			System.out.println(count++);
			FileInputStream fis = new FileInputStream(file);
			BodyContentHandler textHandler = new BodyContentHandler(-1);
			Metadata metadata = new Metadata();
			ParseContext parseContext = new ParseContext();
			HtmlParser parser = new HtmlParser();
			parser.parse(fis, textHandler, metadata, parseContext);
			sb.append(textHandler.toString().trim());
			sb.append("\n");
		}

		BufferedWriter br = new BufferedWriter(new FileWriter("big.txt"));
		br.write(sb.toString());
		br.close();
	}
}
