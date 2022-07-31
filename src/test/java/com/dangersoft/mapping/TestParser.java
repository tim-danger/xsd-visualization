package com.dangersoft.mapping;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class TestParser {

	private List<String> visitedURLs = new ArrayList<>();

	@Test
	public void testTreeCreation() throws IOException {

		Path path = Paths.get("src/main/resources/xsd/xbau22");
		List<Path> paths = listFiles(path);
		paths.forEach(x -> parseXML(x.toString()));
	}

	private List<Path> listFiles(Path path) throws IOException {

		List<Path> result;
		try (Stream<Path> walk = Files.walk(path)) {
			result = walk.filter(Files::isRegularFile).collect(Collectors.toList());
		}
		return result;

	}

	private void parseXML(String rootPath) {
		SAXParseHandler handler = new SAXParseHandler();
		parseXml(new File(rootPath), handler);
		List<String> childPaths = new ArrayList<>();
		for (String url : handler.getUrls()) {

			if (url == null || url.isEmpty()) {
				System.err.println("URL is null or empty (" + url + ")");
				return;
			}

			// der Dateiname leitet sich aus der URL ab
			String path = getPath(url);
			if (!visitedURLs.contains(path)) {
				visitedURLs.add(path);
			} else {
				// wurde der Pfad schon besucht, dann weiter mit dem Nächsten
				continue;
			}

			String fileContent = downloadFileContent(url);

			// die referenzierten Schemata werden dann wieder rekursiv durchlaufen
			childPaths.add(path);
			try {
				createFile(path);
			} catch (IOException e) {
				System.err.println(e.getMessage());
				return;
			}
			saveFile(fileContent, path);
		}

		for (String path : childPaths) {
			parseXML(path);
		}

	}

	// TODO : den Output-Pfad parametrierbar machen
	private String getPath(String url) {
		String path = "";
		if (url.startsWith("http://")) {
			path = "target/" + url.substring("http://".length());
		} else if (url.startsWith("https://")) {
			path = "target/" + url.substring("https://".length());
		}
		return path;
	}

	private String downloadFileContent(String url) {
		HttpHelper helper = new HttpHelper();
		return helper.readFile(url);
	}

	private void saveFile(String fileContent, String path) {
		try {
			URL target = Paths.get(path).toUri().toURL();

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(target.getFile(), false))) {
				writer.append(fileContent);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void createFile(String path) throws IOException {
		File file = new File(path);
		if (!file.exists()) {
			if (new File(file.getParent()).mkdirs()) {
				file.createNewFile();
			}
		}
	}

	private void parseXml(File file, SAXParseHandler handler) {

		// zunächst die Liste der URLs wieder löschen
		handler.initHandler();

		// Create a empty link of users initially
		try {

			// Create parser from factory
			XMLReader parser = XMLReaderFactory.createXMLReader();

			// Register handler with parser
			parser.setContentHandler(handler);

			// Create an input source from the XML input stream
			InputSource source = new InputSource(file.getAbsolutePath());

			// parse the document
			parser.parse(source);

		} catch (SAXException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} finally {

		}
	}
}
