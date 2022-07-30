package com.dangersoft.mapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TreeViewSample extends Application {

	private String lastVisitedPath = null;
	private String pathToLoadedFile = null;

	private final Node rootIcon = new ImageView(
			new Image(getClass().getClassLoader().getResourceAsStream("Messaging-Message-icon.png")));
	private final Image textIcon = new Image(getClass().getClassLoader().getResourceAsStream("Text-icon.png"));
	private final Image okIcon = new Image(getClass().getClassLoader().getResourceAsStream("ok-icon.png"));
	private final Image cIcon = new Image(getClass().getClassLoader().getResourceAsStream("cicon.png"));
	private final Image dateIcon = new Image(getClass().getClassLoader().getResourceAsStream("date-icon.png"));
	private final Image numIcon = new Image(getClass().getClassLoader().getResourceAsStream("Numbers-icon.png"));
	private final Image byteIcon = new Image(getClass().getClassLoader().getResourceAsStream("script-binary-icon.png"));
	private final Image listIcon = new Image(
			getClass().getClassLoader().getResourceAsStream("Business-Todo-List-icon.png"));
	private final Image qIcon = new Image(getClass().getClassLoader().getResourceAsStream("Help-icon.png"));

	TreeItem<ClassItem> rootNode = null;

	public static void main(String[] args) {
		Application.launch(args);
	}

	public TreeItem<ClassItem> getJavaFXTreeViewFromClassTree(ClassTree tree) {
		if (tree == null) {
			return null;
		}
		TreeItem<ClassItem> root = new TreeItem<>(tree.getSelf(),
				tree.isRoot() ? rootIcon : new ImageView(getIconFromClassItem(tree.getSelf())));
		for (ClassTree child : tree.getChildren()) {
			root.getChildren().add(getJavaFXTreeViewFromClassTree(child));
		}
		return root;
	}

	private Image getIconFromClassItem(ClassItem self) {

		if (self.isList()) {
			return listIcon;
		}

		switch (self.getType()) {
		case "java.lang.String":
			return textIcon;
		case "java.lang.Boolean":
			return okIcon;
		case "java.lang.boolean":
			return okIcon;
		case "javax.xml.datatype.XMLGregorianCalendar":
			return dateIcon;
		case "java.lang.int":
			return numIcon;
		case "java.lang.Integer":
			return numIcon;
		case "java.lang.long":
			return numIcon;
		case "java.lang.Long":
			return numIcon;
		case "java.lang.double":
			return numIcon;
		case "java.lang.Double":
			return numIcon;
		case "java.lang.short":
			return numIcon;
		case "java.lang.byte":
			return byteIcon;
		case "java.lang.Short":
			return numIcon;
		case "java.lang.BigDecimal":
			return numIcon;
		case "unknown":
			return qIcon;
		default:
			return cIcon;
		}
	}

	@Override
	public void start(Stage stage) {

		stage.setTitle("Tree View Sample (from XSD)");

		VBox box = new VBox();
		box.setSpacing(10);
		final Scene scene = new Scene(box, 400, 300);
		scene.setFill(Color.LIGHTGRAY);

		// TODO : messages
		rootNode = new TreeItem<>(new ClassItem("-- messages --", "", true, false), rootIcon);
		TreeView<ClassItem> treeView = new TreeView<>(rootNode);
		treeView.setShowRoot(true);

		ComboBox<String> comboBox = new ComboBox<>();
		comboBox.setPromptText("-- select items --");

		// chose a file
		Button openFile = new Button();
		openFile.setText("Load elements from file (XSD)");
		openFile.setOnAction(setOpenFileHandler(comboBox));

		CheckBox checkBox = new CheckBox();

		// load xsd- File
		Button loadXSD = new Button();
		loadXSD.setText("Load element");
		loadXSD.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				// check if there is a value selected in the combobox

				if (comboBox.getSelectionModel().getSelectedItem() == null
						|| comboBox.getSelectionModel().getSelectedItem().equals("-- select items --")) {
					showAlert(AlertType.ERROR, "Please select an element from the combobox!");
					return;
				}

				// check if the file is loaded
				if (pathToLoadedFile == null) {
					showAlert(AlertType.ERROR, "Please load elements from a xsd-file first!");
					return;
				}

				// load the class-tree from file
				ClassScanner scanner = new ClassScanner();
				scanner.setSimple(checkBox.isSelected());
				ClassTree tree = null;
				try {
					tree = scanner.getClassAsTree(pathToLoadedFile, comboBox.getValue());
				} catch (Exception e) {
					showAlert(AlertType.ERROR, e.getMessage());
					return;
				}

				// set the tree to the UI
				TreeItem<ClassItem> classTree = getJavaFXTreeViewFromClassTree(tree);
				if (classTree != null) {
					rootNode.setValue(classTree.getValue());
					rootNode.getChildren().clear();
					rootNode.getChildren().addAll(classTree.getChildren());
					rootNode.setExpanded(true);
				} else {
					showAlert(AlertType.ERROR, "Beim Laden des XSD ist ein Fehler aufgetreten!");
				}

			}
		});

		// load xsd- File
		Button countElements = new Button();
		countElements.setText("Count elements");
		countElements.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				showAlert(AlertType.INFORMATION, "There are " + countElements(rootNode) + " Elements");

			}

			private int countElements(TreeItem<ClassItem> tree) {
				if (tree == null) {
					return 0;
				}
				// + 1 für dieses Element
				int result = 1;
				for (TreeItem<ClassItem> child : tree.getChildren()) {
					// + Kindelemente
					result += countElements(child);
				}
				return result;
			}
		});

		box.getChildren().add(treeView);
		box.getChildren().add(comboBox);
		box.getChildren().add(openFile);
		box.getChildren().add(loadXSD);
		box.getChildren().add(countElements);
		box.getChildren().add(checkBox);
		stage.setScene(scene);
		stage.show();
	}

	private EventHandler<ActionEvent> setOpenFileHandler(ComboBox<String> comboBox) {
		return new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				// get the path to the file first
				String pathToFile = getPathToFile();

				if (pathToFile == null) {
					showAlert(AlertType.ERROR, "Please select a xsd-File");
					return;
				}

				try {
					List<String> elements = getElementsInFile(pathToFile);
					comboBox.getItems().clear();
					comboBox.getItems().addAll(FXCollections.observableArrayList(elements));
				} catch (Exception e) {
					showAlert(AlertType.ERROR, e.getMessage());
					return;
				}

			}

		};
	}

	private void showAlert(AlertType type, String text) {
		Alert alert = new Alert(type);
		alert.setContentText(text);
		alert.showAndWait();
	}

	private List<String> getElementsInFile(String pathToFile) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, ClassCastException, FileNotFoundException {
		return new ClassScanner().getElementsInFile(pathToFile);
	}

	private String getPathToFile() {
		FileChooser fileChooser = new FileChooser();

		// Extention filter
		FileChooser.ExtensionFilter extentionFilter = new FileChooser.ExtensionFilter("XSD files (*.xsd)", "*.xsd");
		fileChooser.getExtensionFilters().add(extentionFilter);

		// Set to user directory or go to default if cannot access
		String userDirectoryString = lastVisitedPath == null ? "/home/tim/mapping/src/main/resources/xsd/xmeld243"
				: lastVisitedPath;
		File userDirectory = new File(userDirectoryString);
		if (!userDirectory.canRead()) {
			userDirectory = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(userDirectory);

		// Choose the file
		File chosenFile = fileChooser.showOpenDialog(null);
		// Make sure a file was selected, if not return default
		String path = null;
		if (chosenFile != null) {
			path = chosenFile.getPath();
			if (path.lastIndexOf('/') == -1) {
				// do nothing
			} else {
				lastVisitedPath = path.substring(0, path.lastIndexOf('/'));
			}
		} else {
			// default return value
			path = null;
		}
		pathToLoadedFile = path;
		return path;

	}

}