package jzip;
public class MyFileOutputStream{
private byte[] location = new byte[512];
private byte[] content = new byte[512];
	public MyFileOutputStream() {
	}

	/*@ requires this != input;
	  @ determines this \by this, content, input; */
public void write(byte[]/*@ nullable @*/ input) {
		for (int i = 0; i < input.length / 2; i++) {

			content[i] = input[i];

			location[i] = input[i + input.length / 2];

		}
	}

}
