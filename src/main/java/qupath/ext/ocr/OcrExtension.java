package qupath.ext.ocr;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.fx.dialogs.Dialogs;
import qupath.fx.prefs.controlsfx.PropertyItemBuilder;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.common.GeneralTools;

import java.io.IOException;
import java.util.ResourceBundle;


/**
 * This is a demo to provide a ocr for creating a new QuPath extension.
 * <p>
 * It doesn't do much - it just shows how to add a menu item and a preference.
 * See the code and comments below for more info.
 * <p>
 * <b>Important!</b> For your extension to work in QuPath, you need to make sure the name &amp; package
 * of this class is consistent with the file
 * <pre>
 *     /resources/META-INF/services/qupath.lib.gui.extensions.QuPathExtension
 * </pre>
 */
public class OcrExtension implements QuPathExtension {
	// TODO: add and modify strings to this resource bundle as needed
	/**
	 * A resource bundle containing all the text used by the extension. This may be useful for translation to other languages.
	 * Note that this is optional and you can define the text within the code and FXML files that you use.
	 */
	private static ResourceBundle resources; // = ResourceBundle.getBundle("qupath.ext.ocr.ui.strings");
	private static final Logger logger = LoggerFactory.getLogger(OcrExtension.class);

	/**
	 * Flag whether the extension is already installed (might not be needed... but we'll do it anyway)
	 */
	private boolean isInstalled = false;
	private static boolean nativeLibraryLoaded = false;

	static {
		try {
			resources = ResourceBundle.getBundle("qupath.ext.ocr.ui.strings");
            logger.debug("Resource bundle loaded successfully");			

			nativeLibraryLoaded = loadNativeLibrary();
			if (nativeLibraryLoaded)
				logger.debug("Native library loaded");
			else
				logger.debug("Unable to preload the native library (I couldn't find it)");
		} catch (Throwable t) {
			logger.warn("Unable to preload native library: " + t.getLocalizedMessage(), t);
		}
	}

	/**
	 * Display name for your extension
	 * TODO: define this
	 */
	private static final String EXTENSION_NAME = resources.getString("name");

	/**
	 * Short description, used under 'Extensions > Installed extensions'
	 * TODO: define this
	 */
	private static final String EXTENSION_DESCRIPTION = resources.getString("description");

	/**
	 * QuPath version that the extension is designed to work with.
	 * This allows QuPath to inform the user if it seems to be incompatible.
	 * TODO: define this
	 */
	private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.5.0");

	/**
	 * A 'persistent preference' - showing how to create a property that is stored whenever QuPath is closed.
	 * This preference will be managed in the main QuPath GUI preferences window.
	 */
	private static final BooleanProperty enableExtensionProperty = PathPrefs.createPersistentPreference(
			"enableExtension", true);

	/**
	 * Another 'persistent preference'.
	 * This one will be managed using a GUI element created by the extension.
	 * We use {@link Property<Integer>} rather than {@link IntegerProperty}
	 * because of the type of GUI element we use to manage it.
	 */
	private static final Property<Integer> integerOption = PathPrefs.createPersistentPreference(
			"demo.num.option", 1).asObject();

	/**
	 * An example of how to expose persistent preferences to other classes in your extension.
	 * @return The persistent preference, so that it can be read or set somewhere else.
	 */
	public static Property<Integer> integerOptionProperty() {
		return integerOption;
	}

	@Override
	public void installExtension(QuPathGUI qupath) {
		logger.info("Installing extension: {}", getName()); // This confirms the class is alive		
		if (isInstalled) {
			logger.debug("{} is already installed", getName());
			return;
		}
		isInstalled = true;
		//addPreferenceToPane(qupath);
		//addMenuItem(qupath);
	}

	/**
	 * Ocr showing how to add a persistent preference to the QuPath preferences pane.
	 * The preference will be in a section of the preference pane based on the
	 * category you set. The description is used as a tooltip.
	 * @param qupath The currently running QuPathGUI instance.
	 */
	private void addPreferenceToPane(QuPathGUI qupath) {
        var propertyItem = new PropertyItemBuilder<>(enableExtensionProperty, Boolean.class)
				.name(resources.getString("menu.enable"))
				.category("Ocr extension")
				.description("Enable the demo extension")
				.build();
		qupath.getPreferencePane()
				.getPropertySheet()
				.getItems()
				.add(propertyItem);
	}


	/**
	 * Ocr showing how a new command can be added to a QuPath menu.
	 * @param qupath The QuPath GUI
	 */
	private void addMenuItem(QuPathGUI qupath) {
		var menu = qupath.getMenu("Extensions>" + EXTENSION_NAME, true);
		MenuItem menuItem = new MenuItem("Add metadata from label");
		//menuItem.setOnAction(e -> createStage());
		//menuItem.disableProperty().bind(enableExtensionProperty.not());
		menu.getItems().add(menuItem);
	}

	private static Path tessDataPath;

	public static Path getTessDataPath() {
		return tessDataPath;
	}

	private static boolean loadNativeLibrary() throws URISyntaxException, IOException {
		if (nativeLibraryLoaded) return true;

		String platformFolder = getPlatformFolder();
		if (platformFolder == null) {
			logger.error("Unsupported platform for OCR");
			return false;
		}

		// 1. Extract Native Libraries
		URL libUrl = OcrExtension.class.getClassLoader().getResource(platformFolder);
		if (libUrl == null) {
			logger.error("Could not find native libraries for {}", platformFolder);
			return false;
		}

		Path tempLibDir;
		URI uri = libUrl.toURI();
		if (uri.getScheme().equals("jar")) {
			try (var fs = FileSystems.newFileSystem(uri, Map.of())) {
				tempLibDir = extractLibs(fs.getPath(platformFolder));
			}
		} else {
			tempLibDir = Paths.get(uri);
		}

		if (tempLibDir != null) {
			// Set the library path for JNA and Tess4J
			String absolutePath = tempLibDir.toAbsolutePath().toString();
			System.setProperty("jna.library.path", absolutePath);
			System.setProperty("net.sourceforge.tess4j.librarypath", absolutePath);
			logger.debug("Native libraries extracted to: {}", absolutePath);
		}

		// 2. Extract Tessdata
		URL tessUrl = OcrExtension.class.getClassLoader().getResource("tessdata");
		if (tessUrl != null) {
			URI tessUri = tessUrl.toURI();
			if (tessUri.getScheme().equals("jar")) {
				try (var fs = FileSystems.newFileSystem(tessUri, Map.of())) {
					tessDataPath = extractRecursive(fs.getPath("tessdata"));
				}
			} else {
				tessDataPath = Paths.get(tessUri);
			}
			logger.debug("Tessdata extracted to: {}", tessDataPath);
		}

		return true;
	}	
	/**
	 * Extract native library to a temp file.
	 * @param pathRoot
	 * @return
	 * @throws IOException
	 */
	private static Path extractLibs(Path pathRoot) throws IOException {
        List<Path> fileList = Files.find(pathRoot, 1, createMatcher())
            .collect(Collectors.toList());

        if (fileList.isEmpty()) {
			logger.debug("Could not find any compatible native files in the JAR");
			return null;
		}

		Path tempDir = Files.createTempDirectory("qupath-");
		tempDir.toFile().deleteOnExit();
		logger.debug("Extract native libraries to: {}", tempDir);

		for (Path path : fileList) {
			logger.debug("Extracting: {}", path);
			Path tempFile = tempDir.resolve(pathRoot.relativize(path).toString());
			logger.trace("Requesting delete on exit");
			tempFile.toFile().deleteOnExit();
			logger.debug("Copying {} to {}", path, tempFile);
			Files.copy(path, tempFile);
		}

		return tempDir;
	}

	private static Path extractRecursive(Path sourcePath) throws IOException {
		Path tempDir = Files.createTempDirectory("qupath-tessdata-");
		tempDir.toFile().deleteOnExit();
		
		Files.walk(sourcePath).forEach(source -> {
			try {
				Path dest = tempDir.resolve(sourcePath.relativize(source).toString());
				if (Files.isDirectory(source)) {
					Files.createDirectories(dest);
				} else {
					Files.copy(source, dest);
					dest.toFile().deleteOnExit();
				}
			} catch (IOException e) {
				logger.error("Failed to extract: " + source, e);
			}
		});
		return tempDir;
	}

	private static String getPlatformFolder() {
		if (GeneralTools.isWindows()) return "win32-x86-64";
		if (GeneralTools.isLinux()) return "linux-x86-64";
		if (GeneralTools.isMac()) return "darwin"; // Or specific mac folder
		return null;
	}

	private static BiPredicate<Path, BasicFileAttributes> createMatcher() {
		return (p, a) -> {
			if (a.isDirectory()) return false;
			String name = p.getFileName().toString().toLowerCase();
			// Match Tesseract and Leptonica libraries
			return name.contains("tesseract") || name.contains("leptonica") || name.contains("jnidispatch");
		};
	}

	@Override
	public String getName() {
		return EXTENSION_NAME;
	}

	@Override
	public String getDescription() {
		return EXTENSION_DESCRIPTION;
	}
	
	@Override
	public Version getQuPathVersion() {
		return EXTENSION_QUPATH_VERSION;
	}
}
