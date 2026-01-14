# QuPath OCR Extension

A QuPath extension that provides Optical Character Recognition (OCR) capabilities for reading text from slide label images using Tesseract.

## Features

- **Automatic Label Detection**: Automatically finds and uses the best available label image (label > macro > thumbnail)
- **Flexible OCR Configuration**: Configure Tesseract parameters from Groovy scripts for optimal results
- **Image Rotation Support**: Built-in support for rotating images by 90, 180, or 270 degrees
- **Region-Based OCR**: Extract text from specific regions of label images
- **Cross-Platform**: Includes native Tesseract libraries for Windows, Linux, and macOS

## Installation

1. Download the latest release from the [Releases](../../releases) page
2. Drag and drop the `.jar` file into QuPath
3. Restart QuPath

## Basic Usage

### Simple OCR on Label Image

```groovy
import qupath.ext.ocr.OcrTools

def imageData = getCurrentImageData()

// Read text from label with no rotation
def text = OcrTools.readLabelText(imageData, 0)
print(text)

// Read text from rotated label (90 degrees)
def rotatedText = OcrTools.readLabelText(imageData, 90)
print(rotatedText)
```

### OCR on Specific Label Type

```groovy
import qupath.ext.ocr.OcrTools

def imageData = getCurrentImageData()

// Explicitly use the "macro" image
def text = OcrTools.readLabelText(imageData, "macro", 0)
print(text)
```

### OCR on Cropped Region

This example will actually return a plausible output for the [OS-2.ndpi](https://openslide.cs.cmu.edu/download/openslide-testdata/Hamamatsu/) image.

```groovy
import qupath.ext.ocr.OcrTools

def imageData = getCurrentImageData()

// Read text from a specific region (x, y, width, height, angle)
def text = OcrTools.readLabelText(imageData, "macro", 0, 0, 70, 400, 90)
print(text)
```

## Advanced Configuration

The real power of this extension comes from being able to configure Tesseract parameters directly from Groovy scripts to improve detection accuracy.

### Custom Tesseract Configuration

```groovy
import qupath.ext.ocr.OcrTools

def imageData = getCurrentImageData()

// initialize / reinitialize the Tesseract instance
def tesseract = OcrTools.initTesseract()

// Configure it as needed
//tesseract.setPageSegMode(3)   // Different page segmentation mode
tesseract.setPageSegMode(6)  // Assume uniform block of text
tesseract.setOcrEngineMode(1) // Use LSTM engine only
tesseract.setVariable("tessedit_char_whitelist", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZmil-.;_ "); //Limit the characters

// Then use the OCR functions as normal
def text = OcrTools.readLabelText(getCurrentImageData(),"macro", 0, 0, 70, 400, 90)
print(text)
```

### Common Tesseract Parameters

**Page Segmentation Modes** (`setPageSegMode`):
- `0` - Orientation and script detection (OSD) only
- `1` - Automatic page segmentation with OSD (default)
- `3` - Fully automatic page segmentation, but no OSD
- `4` - Assume a single column of text of variable sizes
- `6` - Assume a single uniform block of text
- `7` - Treat the image as a single text line
- `11` - Sparse text - find as much text as possible in no particular order
- `13` - Raw line - treat image as a single text line, bypassing hacks specific to Tesseract

**OCR Engine Modes** (`setOcrEngineMode`):
- `0` - Legacy engine only
- `1` - Neural nets LSTM engine only (default)
- `2` - Legacy + LSTM engines
- `3` - Default, based on what is available

**Languages** (`setLanguage`):
- `"eng"` - English (default)
- `"deu"` - German
- `"fra"` - French
- `"spa"` - Spanish
- Multiple languages: `"eng+deu"`

### Example: Optimized Configuration for Dense Text

```groovy
import qupath.ext.ocr.OcrTools

def tesseract = OcrTools.getTesseract()

// Configure for dense, uniform text blocks
tesseract.setPageSegMode(6)    // Single uniform block
tesseract.setOcrEngineMode(1)  // LSTM only for better accuracy

def imageData = getCurrentImageData()
def text = OcrTools.readLabelText(imageData, 0)
print(text)
```

## API Reference

### `OcrTools` Methods

#### `initTesseract()`
Initialize or reinitialize Tesseract with default settings.

**Returns**: `ITesseract` - The initialized Tesseract instance

#### `getTesseract()`
Get the current Tesseract instance. Automatically initializes if not already done.

**Returns**: `ITesseract` - The Tesseract instance for configuration

#### `readLabelText(ImageData<?> imageData, int angle)`
Read text from the best available label image.

**Parameters**:
- `imageData` - The current image data
- `angle` - Rotation angle (0, 90, 180, or 270)

**Returns**: `String` - Extracted text

#### `readLabelText(ImageData<?> imageData, String labelName, int angle)`
Read text from a specific named label image.

**Parameters**:
- `imageData` - The current image data
- `labelName` - Name of the associated image ("label", "macro", "thumbnail", etc.)
- `angle` - Rotation angle (0, 90, 180, or 270)

**Returns**: `String` - Extracted text

#### `readLabelText(ImageData<?> imageData, String labelName, int x, int y, int w, int h, int angle)`
Read text from a cropped region of a label image.

**Parameters**:
- `imageData` - The current image data
- `labelName` - Name of the associated image (null for auto-detect)
- `x` - X coordinate of crop region
- `y` - Y coordinate of crop region
- `w` - Width of crop region
- `h` - Height of crop region
- `angle` - Rotation angle (0, 90, 180, or 270)

**Returns**: `String` - Extracted text

#### `getLabelImage(ImageData<?> imageData, String labelName)`
Get the label image as a BufferedImage.

**Returns**: `BufferedImage` - The label image, or null if not found

#### `getLabelDimensions(ImageData<?> imageData, String labelName)`
Get the dimensions of the label image.

**Returns**: `Dimension` - Image dimensions, or null if image not found

## Requirements

- QuPath v0.5.0 or later
- Java 11 or later

## Building from Source

```bash
./gradlew clean shadowJar
```

The extension JAR will be created in `build/libs/`.

## License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgments

- Built with [Tesseract OCR](https://github.com/tesseract-ocr/tesseract)
- Uses [Tess4J](https://github.com/nguyenq/tess4j) Java wrapper
- Designed for [QuPath](https://qupath.github.io/)