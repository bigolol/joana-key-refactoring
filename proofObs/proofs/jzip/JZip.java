package jzip;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
public class JZip{
List<String> fileList;
private boolean run;
private String[] args;
private String commandline = "";
	/*@ requires this != file && this != sourceFolder;
	  @ determines this \by this, sourceFolder, detailMessage, value; */
private String generateZipEntry(String/*@ nullable @*/ file, String/*@ nullable @*/ sourceFolder) {
		return file.substring(sourceFolder.length(), file.length());
	}

}
