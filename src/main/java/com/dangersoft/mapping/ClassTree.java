package com.dangersoft.mapping;

import java.util.ArrayList;
import java.util.List;

public class ClassTree {

	private ClassItem self;

	private List<ClassTree> children = new ArrayList<>();

	private ClassTree father;

	private boolean isRoot;

	public List<ClassTree> getChildren() {
		return this.children;
	}

	public ClassTree(ClassItem self, ClassTree father) {
		this.self = self;
		this.father = father;
		this.isRoot = father == null;
	}

	public boolean isRoot() {
		return this.isRoot;
	}

	public ClassItem getSelf() {
		return self;
	}

	public void addChild(ClassTree child) {
		this.children.add(child);
	}

	public void setSelf(ClassItem self) {
		this.self = self;
	}

	public ClassTree getFather() {
		return father;
	}

	public void setFather(ClassTree father) {
		this.father = father;
	}

}
