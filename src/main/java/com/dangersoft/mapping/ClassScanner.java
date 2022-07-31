package com.dangersoft.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.LSInput;

/**
 * Helper class to scan classes and build a tree out of those classes
 * 
 * @author tim
 *
 */
public class ClassScanner {
	private int limitCounter = 0;
	private int limit = 5000;
	private boolean isSimple = false;

	private static final String JAVA_LANG_BOOLEAN_WRAPPER = "java.lang.Boolean";
	private static final String JAVA_LANG_BOOLEAN = "java.lang.boolean";
	private static final String JAVA_LANG_STRING = "java.lang.String";
	private static final String JAVA_XML_GREGORIAN = "javax.xml.datatype.XMLGregorianCalendar";
	private static final String UNKNOWN = "unknown";

	private static final Map<String, String> TYPE_MAP = new HashMap<>();
	private static Logger logger = LoggerFactory.getLogger(ClassScanner.class);

	static {
		TYPE_MAP.put("string", JAVA_LANG_STRING);
		TYPE_MAP.put("String", JAVA_LANG_STRING);
		TYPE_MAP.put("anyURI", JAVA_LANG_STRING);
		TYPE_MAP.put("date", JAVA_XML_GREGORIAN);
		TYPE_MAP.put("boolean", JAVA_LANG_BOOLEAN);
		TYPE_MAP.put("Boolean", JAVA_LANG_BOOLEAN_WRAPPER);
		TYPE_MAP.put("gYear", JAVA_XML_GREGORIAN);
		TYPE_MAP.put("base64", "java.lang.byte");
		TYPE_MAP.put("decimal", "java.lang.BigDecimal");
		TYPE_MAP.put("int", "java.lang.int");
		TYPE_MAP.put("long", "java.lang.long");
	}

	private List<String> visitedElements = new ArrayList<>();

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
				tree.addChild(getClassAsTree(listClass, tree, field.getName() + " (List)"));
			} else
			// no recursive call
			if (isSimple(field.getType())) {
				tree.addChild(
						new ClassTree(new ClassItem(field.getType().getName(), field.getName(), false, false), tree));
			} else {
				tree.addChild(getClassAsTree(field.getType(), tree, field.getName()));
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
		List<String> typeList = Arrays.asList(JAVA_LANG_STRING, "java.lang.Integer", JAVA_LANG_BOOLEAN_WRAPPER,
				JAVA_XML_GREGORIAN, "java.lang.Short", "java.lang.Byte", "java.lang.Long",
				"java.lang.int", "java.lang.short", JAVA_LANG_BOOLEAN, "java.lang.byte");
		return className.isPrimitive() || typeList.contains(className.getName()) || className.isArray();
	}

	public List<String> listRootClasses(String packageName) {

		List<String> classNames = new ArrayList<>();

		List<ClassLoader> classLoadersList = new LinkedList<>();
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

	public List<String> getElementsInFile(String pathToScheme) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, FileNotFoundException {

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

	public ClassTree getClassAsTree(String pathToScheme, String elementName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			FileNotFoundException, LimitExceededException {

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

	private ClassTree getClassAsTree(QName name, XSModel model) throws LimitExceededException {
		limitCounter = 0;
		XSElementDeclaration element = model.getElementDeclaration(name.getLocalPart(), name.getNamespaceURI());
		return getClassAsTree(new XMLElement(element, 1, 1), null);

	}

	private ClassTree getClassAsTree(XMLElement xmlElement, ClassTree parent) throws LimitExceededException {

		limitCounter++;
		if (limitCounter > limit) {
			throw new LimitExceededException("The limit of " + limit + " is reached. XSD is too big!");
		}
		// TODO : AnonType entsprechend berücksichtigen
		XSElementDeclaration element = xmlElement.getElement();
		XSTypeDefinition definition = element.getTypeDefinition();

		if (definition instanceof XSSimpleTypeDefinition) {

			return handleSimpleTypeDefintion(xmlElement, parent, element, definition);

		} else if (definition instanceof XSComplexTypeDecl) {

			ClassTree result = handleComplexTypeDefinition(xmlElement, parent, element, definition);
			if (result != null) return result;
		} else {
			String unknownType = definition.getClass().toString();
			logger.error("Unknown type: {}", unknownType);
		}
		return null;
	}

	private ClassTree handleComplexTypeDefinition(XMLElement xmlElement, ClassTree parent, XSElementDeclaration element, XSTypeDefinition definition) throws LimitExceededException {
		ClassTree tree;
		String complexName = camelCaseWithoutDot(element.getName(), false);
		XSComplexTypeDecl complexDef = (XSComplexTypeDecl) definition;
		String typeName = complexDef.getTypeName();
		String complexNameAndType = complexName + typeName;
		typeName = camelCaseWithoutDot(typeName, true);
		ClassItem item = new ClassItem(typeName, complexName, true,
				xmlElement.getMaxoccurs() == -1 || xmlElement.getMaxoccurs() > 1);
		tree = new ClassTree(item, parent);
		if (visitedElements.contains(complexNameAndType)) {
			return tree;
		} else {
			visitedElements.add(complexNameAndType);
		}
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
				visitedElements.remove(complexNameAndType);
				return tree;
			} else {
				String unknownType = term.getClass().toString();
				logger.error("Unknown type: {}", unknownType);
				visitedElements.remove(complexNameAndType);
			}

		} else {
			// keine Kindelemente
			addAttributes(definition, tree);
			visitedElements.remove(complexNameAndType);
			return tree;
		}
		return null;
	}

	private ClassTree handleSimpleTypeDefintion(XMLElement xmlElement, ClassTree parent, XSElementDeclaration element, XSTypeDefinition definition) {
		ClassTree tree;
		XSSimpleTypeDefinition simple = (XSSimpleTypeDefinition) definition;
		String name = getTypeName(xmlElement, definition, simple);
		ClassItem item = new ClassItem(name, element.getName(), false,
				xmlElement.getMaxoccurs() == -1 || xmlElement.getMaxoccurs() > 1);
		tree = new ClassTree(item, parent);
		addAttributes(definition, tree);
		return tree;
	}

	private String getTypeName(XMLElement xmlElement, XSTypeDefinition definition, XSSimpleTypeDefinition simple) {
		String name = null;
		if (definition.getName() != null) {
			name = getType(definition.getName(), xmlElement.getMinoccurs() == 0);
			if (name.equals(UNKNOWN)) {
				if (simple == null || simple.getPrimitiveType() == null) {
					name = UNKNOWN;
				} else {
					name = getType(simple.getPrimitiveType().toString(), xmlElement.getMinoccurs() == 0);
				}
			}
		} else {
			name = getType(simple.getPrimitiveType().toString(), xmlElement.getMinoccurs() == 0);
		}
		return name;
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

		for (Map.Entry<String, String> entry : TYPE_MAP.entrySet()) {
			if (string.contains(entry.getKey())) {
				return entry.getValue();
			}
		}
		if (string.contains("boolean") && isWrapper) {
			return JAVA_LANG_BOOLEAN_WRAPPER;
		} else if (string.contains("int") && isWrapper) {
			return "java.lang.Integer";
		} else if (string.contains("long") && isWrapper) {
			return "java.lang.Long";
		}
		return UNKNOWN;
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

		if (visitedElements.contains(modelgroup.toString())) {
			return result;
		} else {
			visitedElements.add(modelgroup.toString());
		}
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
				XSModelGroup mg = (XSModelGroup) pterm;
				result.addAll(getElementDeclarations(mg));
			} else {
				// unknown instances
				String modelGroupAsString = modelgroup.toString();
				String classAsString = pterm.getClass().toString();
				logger.info("Name of Modelgroup: {}", modelGroupAsString);
				logger.error("Unknown instance: {}", classAsString);
			}
		}

		// wenn man den simplen Ansatz wählt, werden sich im Baum wiederholende Elemente
		// nicht mehr gesetzt (das COLLADA-Scheme enthält bspw. weit mehr als 5.000
		// Elemente, was dazu führt, dass der Heap-Speicher nicht mehr ausreicht)
		if (!isSimple) {
			visitedElements.remove(modelgroup.toString());
		}
		return result;
	}

	public boolean isSimple() {
		return isSimple;
	}

	public void setSimple(boolean isSimple) {
		this.isSimple = isSimple;
	}
}
