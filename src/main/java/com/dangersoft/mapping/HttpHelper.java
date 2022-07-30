package com.dangersoft.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpHelper {

	public String readFile(String urlAsString) {
		HttpURLConnection con = null;
		try {
			URL url = new URL(urlAsString);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			return readResponse(con);
		} catch (Exception e) {
			e.printStackTrace();
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
