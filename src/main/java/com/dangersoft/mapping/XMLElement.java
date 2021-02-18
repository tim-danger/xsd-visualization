package com.dangersoft.mapping;

import org.apache.xerces.xs.XSElementDeclaration;

public class XMLElement {

	private XSElementDeclaration element;

	private int minoccurs = 1;

	private int maxoccurs = 1;

	public XMLElement(XSElementDeclaration element, int minoccurs, int maxoccurs) {
		this.element = element;
		this.minoccurs = minoccurs;
		this.maxoccurs = maxoccurs;
	}

	public XSElementDeclaration getElement() {
		return element;
	}

	public void setElement(XSElementDeclaration element) {
		this.element = element;
	}

	public int getMinoccurs() {
		return minoccurs;
	}

	public void setMinoccurs(int minoccurs) {
		this.minoccurs = minoccurs;
	}

	public int getMaxoccurs() {
		return maxoccurs;
	}

	public void setMaxoccurs(int maxoccurs) {
		this.maxoccurs = maxoccurs;
	}

}
