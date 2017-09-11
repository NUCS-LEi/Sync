package org.synclibrary.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Vibrator;
import android.view.Display;

public class Util {

	private static NumberFormat numberFormat = new DecimalFormat("#.##");

	public static String formatFloat(float number) {
		return numberFormat.format(number);
	}

	/**
	 * Vibrate the phone.
	 * @param numMS Milliseconds to vibrate for.
	 * @param aContext Requires a context object
	 */
	public static void vibratePhone(int numMS, Context aContext) {
		((Vibrator) aContext.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(numMS);
	}

	public static Boolean hasCityWallpaper(Context c) {
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(c);
		WallpaperInfo info = wallpaperManager.getWallpaperInfo();
		if (info != null) {
			return info.getPackageName().startsWith("edu.mit.android.cityver1");
		}
		return false;
	}
	public static String getVersion(Context c){
		PackageManager pm = c.getPackageManager();
        PackageInfo pi = null; 
        try {
        	if (pm != null)
        		pi =  pm.getPackageInfo(c.getApplicationInfo().packageName, 0);
            if (pi != null)
            	return String.valueOf(pi.versionName);
           	return ""; 
        } catch (NameNotFoundException e) {
        	return ""; 
        }
	}
	
	public static boolean isAria(Context c) {
		Display d = ((android.view.WindowManager) c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		return ((d.getWidth() == 320) && (d.getHeight() == 480)); 
	}
	public static String convertStreamToString(InputStream is) throws IOException {
		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}
	
}
