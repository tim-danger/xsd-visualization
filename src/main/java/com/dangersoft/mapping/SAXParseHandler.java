package com.dangersoft.mapping;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParseHandler extends DefaultHandler {

	private List<String> urls = new ArrayList<>();

	public void initHandler() {
		urls.clear();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (qName.contains("import")) {
			if (attributes.getValue("schemaLocation") != null) {
				urls.add(attributes.getValue("schemaLocation"));
			}
		}
	}

	public List<String> getUrls() {
		return urls;
	}

	public void setUrls(List<String> urls) {
		this.urls = urls;
	}

}
