package com.dangersoft.mapping;

public class ClassItem {

	private String type;

	private String name;

	private boolean isComplex;

	private boolean isList;

	public ClassItem(String type, String name, boolean isComplex, boolean isList) {
		this.type = type;
		this.name = name;
		this.isComplex = isComplex;
		this.setList(isList);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isComplex() {
		return isComplex;
	}

	public void setComplex(boolean isComplex) {
		this.isComplex = isComplex;
	}

	@Override
	public String toString() {
		return name + " : " + simpleType(type);
	}

	private String simpleType(String type) {
		int lastIndexOfDot = type.lastIndexOf('.');
		return lastIndexOfDot > -1 ? type.substring(lastIndexOfDot + 1) : type;
	}

	public boolean isList() {
		return isList;
	}

	public void setList(boolean isList) {
		this.isList = isList;
	}

}
