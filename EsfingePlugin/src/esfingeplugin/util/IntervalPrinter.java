package esfingeplugin.util;

public class IntervalPrinter {

	private long lastTime = 0;

	public void init() {
		lastTime = System.currentTimeMillis();
	}

	public void print(String name) {
		long currTime = System.currentTimeMillis();
		System.out.println(name + ": " + (currTime - lastTime) + "ms");
		lastTime = currTime;
	}

}
