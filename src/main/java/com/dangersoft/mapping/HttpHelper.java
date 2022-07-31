package com.dangersoft.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpHelper {

	private static Logger logger = LoggerFactory.getLogger(HttpHelper.class);

	public String readFile(String urlAsString) {
		HttpURLConnection con = null;
		try {
			URL url = new URL(urlAsString);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			return readResponse(con);
		} catch (Exception e) {
			String message = e.getMessage();
			logger.error("Could not read file, message: {}", message);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return null;
	}

	// tag::readResponse[]
	private String readResponse(HttpURLConnection con) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) { // <1>
			String inputLine;
			StringBuilder content = new StringBuilder();
			while ((inputLine = in.readLine()) != null) { // <2>
				content.append(inputLine); // <3>
			}
			return content.toString(); // <4>
		} catch (IOException e) {
			return null; // <5>
		}
	}
	// end::readResponse[]

}
