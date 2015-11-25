package com.polyfx.jssembly;

public class Jssembly {
	static {
		// Jssembly.x86.dll (Windows) or libJssembly.arm64.so (*NIX)
		// TODO: probably needs some tweaking and architecture consolidation
		System.loadLibrary("Jssembly."+System.getProperty("os.arch"));
	}
	
	protected static native long invoke(byte[] binary, Object... args);

	public Block define(byte[] assembly) {
		return new Block(assembly);
	}
	
	public Block define(Block blok) {
		return blok;
	}
}
