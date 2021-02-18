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

	private TreeNode root;

	@PostConstruct
	public void init() {
		root = new DefaultTreeNode("Files", null);
		ClassScanner scanner = new ClassScanner();
		ClassTree treeFromClasses = scanner.getClassAsTree(FortschreibungDokument0065.class, null);
		getPrimeFacesTree(treeFromClasses, root);
//		TreeNode node0 = new DefaultTreeNode("Documents", root);
//		TreeNode node1 = new DefaultTreeNode("Events", root);
//		TreeNode node2 = new DefaultTreeNode("Movies", root);
//
//		TreeNode node00 = new DefaultTreeNode("Work", node0);
//		TreeNode node01 = new DefaultTreeNode("Home", node0);
//
//		node00.getChildren().add(new DefaultTreeNode("Expenses.doc"));
//		node00.getChildren().add(new DefaultTreeNode("Resume.doc"));
//		node01.getChildren().add(new DefaultTreeNode("Invoices.txt"));
//
//		TreeNode node10 = new DefaultTreeNode("Meeting", node1);
//		TreeNode node11 = new DefaultTreeNode("Product Launch", node1);
//		TreeNode node12 = new DefaultTreeNode("Report Review", node1);
//
//		TreeNode node20 = new DefaultTreeNode("Al Pacino", node2);
//		TreeNode node21 = new DefaultTreeNode("Robert De Niro", node2);
//
//		node20.getChildren().add(new DefaultTreeNode("Scarface"));
//		node20.getChildren().add(new DefaultTreeNode("Serpico"));
//
//		node21.getChildren().add(new DefaultTreeNode("Goodfellas"));
//		node21.getChildren().add(new DefaultTreeNode("Untouchables"));

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