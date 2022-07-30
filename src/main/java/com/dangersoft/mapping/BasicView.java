package com.dangersoft.mapping;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.dangersoft.mapping.xmeld243.FortschreibungDokument0065;

@ManagedBean(name = "treeBasicView")
@ViewScoped
public class BasicView implements Serializable {
	private static final long serialVersionUID = 2125209132333903734L;

	private transient TreeNode root;

	@PostConstruct
	public void init() {
		root = new DefaultTreeNode("Files", null);
		ClassScanner scanner = new ClassScanner();
		ClassTree treeFromClasses = scanner.getClassAsTree(FortschreibungDokument0065.class, null);
		getPrimeFacesTree(treeFromClasses, root);
	}

	private TreeNode getPrimeFacesTree(ClassTree tree, TreeNode parent) {
		TreeNode node = new DefaultTreeNode("customType1", tree.getSelf().getName(), parent);
		for (ClassTree child : tree.getChildren()) {
			node.getChildren().add(getPrimeFacesTree(child, node));
		}
		return node;
	}

	public TreeNode getRoot() {
		return root;
	}
}