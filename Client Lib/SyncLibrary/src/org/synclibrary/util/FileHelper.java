package org.synclibrary.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.os.Environment;

public class FileHelper {

	private static final String ERR_SD_MISSING_MSG = "CITY cannot see your SD card. Please reinstall it and do not remove it.";
	private static final String ERR_SD_UNREADABLE_MSG = "CITY cannot read your SD (memory) card. This is probably because your phone is plugged into your computer. Please unplug it and try again.";

	public static File getSDCard() throws Exception {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED))
			throw new Exception(ERR_SD_MISSING_MSG);
		else if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			throw new Exception(ERR_SD_UNREADABLE_MSG);

		File sdCard = Environment.getExternalStorageDirectory();
		if (!sdCard.exists())
			throw new Exception(ERR_SD_MISSING_MSG);
		if (!sdCard.canRead())
			throw new Exception(ERR_SD_UNREADABLE_MSG);
		return sdCard;
	}

	public static OutputStream openFileForAppending(String fileName, String fileDesc) throws Exception {
		File sdCard = new File("/sdcard/");
		if (!sdCard.exists())
			throw new Exception("SD card not installed");
		if (!sdCard.canWrite())
			throw new Exception("Cannot write to SD card");

		File file = new File(sdCard.getAbsolutePath() + "/" + fileName);
		if (file.exists() && !file.canWrite())
			throw new Exception("Cannot write " + fileDesc.toLowerCase() + " file to SD card");

		try {
			return new FileOutputStream(file, true);
		} catch (FileNotFoundException e) {
			throw new Exception("Error in opening " + fileDesc.toLowerCase() + " file from SD card");
		}
	}

	public static InputStream openInternalFileForRead(Context c, String fileName, String fileDesc,
			boolean exceptionIfFileNotFound) throws Exception {

		File file = new File(c.getFilesDir() + "/" + fileName);
		if (!file.exists()) {
			if (exceptionIfFileNotFound)
				throw new Exception(fileDesc + " file not found in internal storage");
			return null;
		}
		if (!file.canRead()) {
			if (exceptionIfFileNotFound)
				throw new Exception("Cannot read " + fileDesc.toLowerCase() + " file from internal storage");
			return null;
		}

		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new Exception("Error in opening " + fileDesc.toLowerCase() + " file from internal storage");
		}
	}

	public static InputStream openFileForRead(String fileName, String fileDesc, boolean exceptionIfFileNotFound)
			throws Exception {

		File sdCard = getSDCard();

		File file = new File(sdCard.getAbsolutePath() + "/" + fileName);
		if (!file.exists()) {
			if (exceptionIfFileNotFound)
				throw new Exception(fileDesc + " file not found in SD card");
			return null;
		}
		if (!file.canRead()) {
			if (exceptionIfFileNotFound)
				throw new Exception("Cannot read " + fileDesc.toLowerCase() + " file from SD card");
			return null;
		}

		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new Exception("Error in opening " + fileDesc.toLowerCase() + " file from SD card");
		}
	}

	public static OutputStream openFileForWriting(String fileName, String fileDesc, boolean append)
			throws Exception {
		File sdCard = getSDCard();

		File file = new File(sdCard.getAbsolutePath() + "/" + fileName);
		if (file.exists() && !file.canWrite())
			throw new Exception("Cannot write " + fileDesc.toLowerCase() + " file to SD card");

		try {
			return new FileOutputStream(file, append);
		} catch (FileNotFoundException e) {
			throw new Exception("Error in opening " + fileDesc.toLowerCase() + " file from SD card");
		}
	}

	public static void writeToInternalFile(Context context, String sourceFile, String destinationFile, boolean isAppend) {
		InputStream myInput;
		try {
			myInput = new FileInputStream(FileHelper.getSDCard().getAbsolutePath() + "/" + sourceFile);
			OutputStream myOutput = new BufferedOutputStream(new FileOutputStream(destinationFile, isAppend));
			byte[] buffer = new byte[8192];
			int length;
			while ((length = myInput.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
			}
			myOutput.flush();
			myInput.close();
			myOutput.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void appendToInternalFile(Context c, String data, String fileName, String fileDesc)
			throws Exception {
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(c.getFilesDir() + "/" + fileName, true));
			os.write(data.getBytes());
		} catch (IOException e) {
			throw new Exception("Error in writing " + fileDesc.toLowerCase() + " file to internal storage");
		} finally {
			try {
				os.close();
			} catch (IOException e) {
			}
		}
	}

	public static void appendToFile(String data, String fileName, String fileDesc) throws Exception {
		OutputStream os = openFileForWriting(fileName, fileDesc, true);
		try {
			os.write(data.getBytes());
		} catch (IOException e) {
			throw new Exception("Error in writing " + fileDesc.toLowerCase() + " file to SD card");
		} finally {
			try {
				os.close();
			} catch (IOException e) {
			}
		}
	}

	public static void overwriteFile(String data, String fileName, String fileDesc) throws Exception {
		OutputStream os = openFileForWriting(fileName, fileDesc, false);
		try {
			os.write(data.getBytes());
		} catch (IOException e) {
			throw new Exception("Error in writing " + fileDesc.toLowerCase() + " file to SD card");
		} finally {
			try {
				os.close();
			} catch (IOException e) {
			}
		}
	}

	public static void createInternalDir(Context c, String path) throws Exception {
		File dir = new File(c.getFilesDir() + "/" + path);
		if (dir.exists())
			return;
		if (!dir.mkdirs())
			throw new Exception("Error in creating internal directory " + path);
	}

	public static void createDir(String path) throws Exception {
		File sdCard = getSDCard();

		File dir = new File(sdCard.getAbsolutePath() + "/" + path);
		if (dir.exists())
			return;
		if (!dir.mkdirs())
			throw new Exception("Error in creating directory " + path);
	}

	public static boolean internalFileExists(Context c, String path) {
		File file = new File(c.getFilesDir() + "/" + path);
		if (file.exists())
			return true;
		return false;
	}

	public static boolean fileExists(String path) {
		File sdCard;
		try {
			sdCard = getSDCard();
			if (!sdCard.exists())
				return false;
		} catch (Exception e) {
			return false;
		}

		File file = new File(sdCard.getAbsolutePath() + "/" + path);
		if (file.exists())
			return true;
		return false;
	}
}