package com.dangersoft.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import org.apache.xerces.dom.DOMInputImpl;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.LSInput;

/**
 * Helper class to scan classes and build a tree out of those classes
 * 
 * @author tim
 *
 */
public class ClassScanner {

	private int counter = 0;

	public static void main(String[] args) {
		ClassScanner scanner = new ClassScanner();
		// ClassTree tree = scanner.getClassAsTree(FortschreibungName0091.class, null);
		// scanner.traverse(tree);
		// scanner.listRootClasses("com.dangersoft.mapping.xmeld243").stream().forEach(c
		// -> System.out.println(c));
		try {
			ClassTree tree = scanner.getClassAsTree(
					"src/main/resources/xsd/xmeld243/xmeld-nachrichten-fortschreibung.xsd",
					"fortschreibung.dokument.0065");
			if (tree != null) {
				scanner.traverse(tree);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void traverse(ClassTree tree) {
		if (tree.isRoot()) {
			counter = 0;
		}
		for (int i = 0; i < counter; i++) {
			System.out.print("-");
		}
		System.out.println(tree.getSelf().getName() + " / " + tree.getSelf().getType());
		counter++;
		for (ClassTree son : tree.getChildren()) {
			traverse(son);
		}
		counter--;
	}

	public ClassTree getClassAsTree(Class<?> className, ClassTree parent, String... name) {

		ClassTree tree = new ClassTree(
				new ClassItem(className.getTypeName(), name != null && name.length > 0 ? name[0] : className.getName(),
						true, name != null && name.length > 0 && name[0].contains("List")),
				parent);

		// get ALL fields (from the class as well as from the parents and their parents)
		Field[] declaredFields = getAllFieldsUpToObject(className);
		for (Field field : declaredFields) {

			// handelt es sich um einen Listentyp, dann rekursiver Aufruf
			if (field.getType().isAssignableFrom(List.class)) {
				ParameterizedType listType = (ParameterizedType) field.getGenericType();
				Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
				tree.addChild(getClassAsTree(listClass, tree, new String[] { field.getName() + " (List)" }));
			} else
			// no recursive call
			if (isSimple(field.getType())) {
				tree.addChild(
						new ClassTree(new ClassItem(field.getType().getName(), field.getName(), false, false), tree));
			} else {
				tree.addChild(getClassAsTree(field.getType(), tree, new String[] { field.getName() }));
			}
		}
		return tree;
	}

	private Field[] getAllFieldsUpToObject(Class<?> className) {

		// every field including parents and their parents
		List<Field> fields = new ArrayList<>();

		// first the declared fields in the class
		fields.addAll(Arrays.asList(className.getDeclaredFields()));

		Class<?> parent = className.getSuperclass();
		while (parent != null && !parent.equals(Object.class)) {
			fields.addAll(Arrays.asList(parent.getDeclaredFields()));
			parent = parent.getSuperclass();
		}

		// return the fields as an array
		return fields.toArray(new Field[fields.size()]);
	}

	private boolean isSimple(Class<?> className) {
		// the list of simple types and / or wrapper types
		List<String> typeList = Arrays.asList("java.lang.String", "java.lang.Integer", "java.lang.Boolean",
				"javax.xml.datatype.XMLGregorianCalendar", "java.lang.Short", "java.lang.Byte", "java.lang.Long",
				"java.lang.int", "java.lang.short", "java.lang.boolean", "java.lang.byte");
		return className.isPrimitive() || typeList.contains(className.getName()) || className.isArray();
	}

	public List<String> listRootClasses(String packageName) {

		List<String> classNames = new ArrayList<>();

		List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
		classLoadersList.add(ClasspathHelper.contextClassLoader());
		classLoadersList.add(ClasspathHelper.staticClassLoader());

		Reflections reflections = new Reflections(new ConfigurationBuilder()
				.setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
				.setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
				.filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName))));

		Set<Class<? extends Object>> allClasses = reflections.getSubTypesOf(Object.class);
		for (Class<?> clazz : allClasses) {
			if (clazz.isAnnotationPresent(XmlRootElement.class)) {
				classNames.add(clazz.getName());
			}
		}
		return classNames;
	}

	// es gibt vermutlich 3 kostspielige Operationen, die sehr lange dauern, das
	// müsste per Zeitmessung geprüft werden:
	// 1. schemaLoader.loadURI(...) -> 95 % -> wegen Festplattenzugriff
	// 2. ClassTree aus dem XSModel -> 4 % -> wegen Rekursion
	// 3. TreeViewItem für JavaFX -> 1 % -> wegen Rekursion
	// TODO : Untersuchen, ob Vermutung stimmt
	// Lösungsstrategie: beim Öffnen des Programms werden im Hintergrund die 5
	// zuletzt geöffneten Schemata geparst und das XSModel für diese Schemata
	// gechacht, sofern möglich

	public List<String> getElementsInFile(String pathToScheme) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, ClassCastException, FileNotFoundException {

		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		XSImplementation impl = (XSImplementation) registry.getDOMImplementation("XS-Loader");

		XSLoader schemaLoader = impl.createXSLoader(null);
		LSInput input = new DOMInputImpl();
		File file = new File(pathToScheme);
		InputStream is = new FileInputStream(file);
		input.setByteStream(is); // XSD Scheam file in an InpuStream
		XSModel model = schemaLoader.loadURI(file.toURI().toString());
		XSNamedMap map = model.getComponents(XSConstants.ELEMENT_DECLARATION);
		List<String> elements = new ArrayList<>();
		for (Object entry : map.keySet()) {
			if (entry instanceof QName) {
				QName name = (QName) entry;
				elements.add(name.getLocalPart());
			}
		}
		return elements;
	}

	public ClassTree getClassAsTree(String pathToScheme, String elementName) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException, ClassCastException, FileNotFoundException {

		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		XSImplementation impl = (XSImplementation) registry.getDOMImplementation("XS-Loader");

		XSLoader schemaLoader = impl.createXSLoader(null);
		LSInput input = new DOMInputImpl();
		File file = new File(pathToScheme);
		InputStream is = new FileInputStream(file);
		input.setByteStream(is); // XSD Scheam file in an InpuStream
		XSModel model = schemaLoader.loadURI(file.toURI().toString());
		XSNamedMap map = model.getComponents(XSConstants.ELEMENT_DECLARATION);
		for (Object entry : map.keySet()) {
			if (entry instanceof QName) {
				QName name = (QName) entry;
				if (name.getLocalPart().equals(elementName)) {
					return getClassAsTree(name, model);
				}

			}
		}
		return null;

	}

	private ClassTree getClassAsTree(QName name, XSModel model) {
		XSElementDeclaration element = model.getElementDeclaration(name.getLocalPart(), name.getNamespaceURI());
		return getClassAsTree(new XMLElement(element, 1, 1), null);

	}

	private ClassTree getClassAsTree(XMLElement xmlElement, ClassTree parent) {

		// TODO : AnonType entsprechend berücksichtigen
		ClassTree tree = null;
		XSElementDeclaration element = xmlElement.getElement();
		XSTypeDefinition definition = element.getTypeDefinition();

		if (definition instanceof XSSimpleTypeDefinition) {

			XSSimpleTypeDefinition simple = (XSSimpleTypeDefinition) definition;
			String name = null;
			if (definition.getName() != null) {
				name = getType(definition.getName(), xmlElement.getMinoccurs() == 0);
				if (name.equals("unknown")) {
					name = getType(simple.getPrimitiveType().toString(), xmlElement.getMinoccurs() == 0);
				}
			} else {
				name = getType(simple.getPrimitiveType().toString(), xmlElement.getMinoccurs() == 0);
			}
			ClassItem item = new ClassItem(name, element.getName(), false,
					xmlElement.getMaxoccurs() == -1 || xmlElement.getMaxoccurs() > 1);
			tree = new ClassTree(item, parent);
			addAttributes(definition, tree);
			return tree;

		} else if (definition instanceof XSComplexTypeDecl) {

			XSComplexTypeDecl complexDef = (XSComplexTypeDecl) definition;
			String typeName = complexDef.getTypeName();
			typeName = camelCaseWithoutDot(typeName, true);
			ClassItem item = new ClassItem(typeName, camelCaseWithoutDot(element.getName(), false), true,
					xmlElement.getMaxoccurs() == -1 || xmlElement.getMaxoccurs() > 1);
			tree = new ClassTree(item, parent);
			XSParticle particle = complexDef.getParticle();
			if (particle != null) {
				XSTerm term = particle.getTerm();
				if (term instanceof XSModelGroup) {
					XSModelGroup xsModelGroup = (XSModelGroup) term;
					List<XMLElement> elementDeclarations = getElementDeclarations(xsModelGroup);
					for (XMLElement elem : elementDeclarations) {
						tree.addChild(getClassAsTree(elem, tree));
					}
					addAttributes(definition, tree);
					return tree;
				} else {
					System.err.println("Unknown type: " + term.getClass().toString());
				}

			} else {
				// keine Kindelemente
				addAttributes(definition, tree);
				return tree;
			}
		} else {
			System.err.println("Unknown type " + definition.getClass().toString());
		}
		return null;
	}

	private void addAttributes(XSTypeDefinition definition, ClassTree tree) {
		// Attribute auslesen, Attribute werden direkt in den parent eingeklinkt
		if (definition instanceof XSComplexTypeDefinition) {
			XSObjectList xsAttrList = ((XSComplexTypeDefinition) definition).getAttributeUses();
			for (int i = 0; i < xsAttrList.getLength(); i++) {
				XSAttributeUse attrUse = ((XSAttributeUse) xsAttrList.item(i));
				XSAttributeDeclaration declaration = attrUse.getAttrDeclaration();
				if (declaration != null && declaration.getName() != null && declaration.getTypeDefinition() != null
						&& declaration.getTypeDefinition().getName() != null) {
					XSSimpleTypeDefinition simpleTypeDefinition = declaration.getTypeDefinition();
					ClassTree attr = new ClassTree(new ClassItem(getType(simpleTypeDefinition.getName(), false),
							declaration.getName(), false, false), tree);
					tree.addChild(attr);
				}
			}
		}

	}

	private String getType(String string, boolean isWrapper) {
		if (string.contains("string")) {
			return "java.lang.String";
		} else if (string.contains("date")) {
			return "javax.xml.datatype.XMLGregorianCalendar";
		} else if (string.contains("Boolean")) {
			return "java.lang.Boolean";
		} else if (string.contains("boolean")) {
			if (isWrapper) {
				return "java.lang.Boolean";
			}
			return "java.lang.boolean";
		} else if (string.contains("gYear")) {
			return "javax.xml.datatype.XMLGregorianCalendar";
		} else if (string.contains("String")) {
			return "java.lang.String";
		} else if (string.contains("anyURI")) {
			return "java.lang.String";
		} else if (string.contains("base64")) {
			return "java.lang.byte";
		} else if (string.contains("decimal")) {
			return "java.lang.BigDecimal";
		} else if (string.contains("int")) {
			if (isWrapper) {
				return "java.lang.Integer";
			}
			return "java.lang.int";
		} else if (string.contains("long")) {
			if (isWrapper) {
				return "java.lang.Long";
			}
			return "java.lang.long";
		}
		return "unknown";
	}

	private String camelCaseWithoutDot(String typeName, boolean firstLetterUppercase) {

		while (typeName.lastIndexOf('.') != -1) {
			typeName = replaceFirstOccurence(typeName);
		}
		return firstLetterUppercase ? firstLetterUppercase(typeName) : typeName;
	}

	private String replaceFirstOccurence(String typeName) {
		return typeName.substring(0, typeName.lastIndexOf('.'))
				+ firstLetterUppercase(typeName.substring(typeName.lastIndexOf('.') + 1));
	}

	private String firstLetterUppercase(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	private List<XMLElement> getElementDeclarations(XSModelGroup modelgroup) {
		List<XMLElement> result = new ArrayList<>();

		XSObjectList xsol = modelgroup.getParticles();
		for (Object p : xsol) {
			XSParticle part = (XSParticle) p;
			int minoccurs = part.getMinOccurs();
			int maxoccurs = part.getMaxOccurs();
			XSTerm pterm = part.getTerm();
			if (pterm instanceof XSElementDeclaration) {
				XSElementDeclaration elem = (XSElementDeclaration) pterm;
				result.add(new XMLElement(elem, minoccurs, maxoccurs));
			} else if (pterm instanceof XSModelGroup) {
				result.addAll(getElementDeclarations((XSModelGroup) pterm));
			} else {
				// TODO : instance org.apache.xerces.impl.xs.XSWildcardDecl, AZR
				System.err.println("Unknown instance " + pterm.getClass().toString());
			}
		}
		return result;
	}
}
