package jzip;
public class MyZipInputStream{
private byte[] zipFile;
	public MyZipInputStream(byte[] zipFile) {
		this.zipFile = zipFile;
	}

	/*@ requires true;
	  @ determines \result \by zipFile; */
public byte[] read() {
		return zipFile;
	}

}
