# **Sign This Image \- App Specification**

## **1\. Summary**

"Sign This Image" is a simple mobile utility that allows a user to draw a signature or other markings directly onto an image and save the result as a new file.

## **2\. Core User Workflow**

The app has two primary entry points for loading an image:

1. **Share Intent:** The user selects an image in another application (typically tapping the "Share" button just after taking a screenshot in the Google Sheets app) and uses the system's "Share" functionality to send it directly to "Sign This Image".  
2. **In-App Image Picker:** The user opens the app directly and is presented with an option to open an image from their device's photo gallery or file system.

Once an image is loaded, the workflow is as follows:

1. **Display:** The selected image is displayed fullscreen, serving as a background canvas.  
2. **Draw:** The user can use their finger (or a stylus) to draw directly on top of the image. The "ink" is rendered over the background.  
3. **Save:** After drawing their signature, the user taps a "Save" Floating Action Button (FAB) located in a corner of the screen (e.g., bottom-right).  
4. **Output:** The app composites the original background image and the user's signature drawing into a single, flattened image. This new image is then saved to the device.

## **3\. UI/UX Components**

* **Main View:** The primary interface is the image canvas itself. There are no unnecessary toolbars or menus visible during the signing process to maximize screen real estate.  
* **Drawing Interface:**  
  * **Tool:** A simple, pressure-sensitive pen tool is the default.  
  * **Color:** The default ink color is black to provide strong contrast against typical document backgrounds.
* **Floating Action Button (FAB):**  
  * A circular button with a "Save" or "Checkmark" icon.  
  * It remains visible but unobtrusive while the user is drawing.

## **4\. Saving Logic**

* **File Format:** The output image should be saved in PNG to preserve the quality of the signature.  
* **Filename Convention:** The saved file's name will be derived from the original.  
  * **Convention:** \[original\_filename\]\_signed.png  
  * **Example:** If the input is screenshot-2023-10-27.png, the output will be screenshot-2023-10-27\_signed.png.  
* **Save Location:**  
  * **Case 1 (Picker):** If the original image was opened via the in-app picker and the app has the necessary permissions, it will attempt to save the new signed image in the same directory as the original file.  
  * **Case 2 (Gallery):** If the image was received via a share intent, or if the app cannot write to the original directory, the signed image will be saved to the device's primary public picture directory (typically the "DCIM/Camera" folder), making it immediately visible in the main gallery app.

## **5\. Permissions Required**

* **Storage Access:** The app will require permissions to read from and write to the user's device storage to open and save images.
