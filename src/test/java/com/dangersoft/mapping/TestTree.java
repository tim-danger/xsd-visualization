package com.dangersoft.mapping;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.dangersoft.mapping.xmeld243.FortschreibungDokument0065;

public class TestTree {

	List<String> differences = new ArrayList<>();
	List<String> notFoundAttributes = new ArrayList<>();

	@Test
	public void testTreeCreation() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			ClassCastException, FileNotFoundException {

		ClassScanner scanner = new ClassScanner();
		ClassTree treeFromClasses = scanner.getClassAsTree(FortschreibungDokument0065.class, null);
		ClassTree treeFromSchemes = scanner.getClassAsTree(
				"src/main/resources/xsd/xmeld243/xmeld-nachrichten-fortschreibung.xsd", "fortschreibung.dokument.0065");
		compareTrees(treeFromClasses, treeFromSchemes, "root ");
		differences.stream().forEach(c -> System.out.println(c));
		// notFoundAttributes.stream().forEach(c -> System.out.println(c));
	}

	private void compareTrees(ClassTree a, ClassTree b, String currentPath) {

		for (ClassTree childA : a.getChildren()) {
			String nameA = childA.getSelf().getName();
			boolean nameFound = false;
			for (ClassTree childB : b.getChildren()) {
				String nameB = childB.getSelf().getName();
				if (nameA.equals(nameB)) {
					nameFound = true;
					currentPath += " -> " + nameA;
					// Type vergleichen
					String typeA = childA.getSelf().getType();
					String typeB = childB.getSelf().getType();
					typeA = simpleType(typeA);
					typeB = simpleType(typeB);
					if (!typeA.toLowerCase().equals(typeB.toLowerCase())) {
						differences.add(currentPath + ": " + typeA + " verschieden von " + typeB);
					}
					compareTrees(childA, childB, currentPath);
				}
			}

			// Name nicht gefunden
			if (!nameFound) {
				notFoundAttributes.add(childA.getSelf().getName() + " in " + currentPath);
			}
		}
	}

	private String simpleType(String type) {
		int lastIndexOfDot = type.lastIndexOf('.');
		return lastIndexOfDot > -1 ? type.substring(lastIndexOfDot + 1) : type;
	}

}
