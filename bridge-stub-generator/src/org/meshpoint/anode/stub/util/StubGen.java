package org.meshpoint.anode.stub.util;

import java.io.File;
import java.io.IOException;

import org.meshpoint.anode.idl.IDLInterface;
import org.meshpoint.anode.idl.InterfaceManager;
import org.meshpoint.anode.stub.ExportStubGenerator;
import org.meshpoint.anode.stub.ImportStubGenerator;
import org.meshpoint.anode.stub.StubGenerator;
import org.meshpoint.anode.stub.StubGenerator.GeneratorException;

/**
 * A command line utility for running interface stub generators
 * @author paddy
 */
public class StubGen {

	private enum MODE { NONE, IMPORT, EXPORT, VALUE }
	
	private static final String defaultOutpath = ".";
	private static final String[] defaultClasspath = new String[]{"."};
	private static boolean verbose = false;
	private static File outDir = null;
	private static MODE mode = MODE.NONE;

	public static void main(String[] args) {
		String[] classPath = defaultClasspath;

		/* process options */
		int firstNonoptionArg = 0, res;
		String currentArg;
		
		while((currentArg = args[firstNonoptionArg]).startsWith("-")) {
			firstNonoptionArg++;
			if(currentArg.equals("--classpath")) {
				classPath = processClasspath(args[firstNonoptionArg++]);
				continue;
			}
			if(currentArg.equals("--out")) {
				res = processOutpath(args[firstNonoptionArg++]);
				if(res == 0) continue;
				System.exit(res);
			}
			if(currentArg.equals("--mode")) {
				res = processMode(args[firstNonoptionArg++]);
				if(res == 0) continue;
				System.exit(res);
			}
			if(currentArg.equals("--verbose")) {
				verbose = true;
				continue;
			}
			if(currentArg.equals("--help")) {
				processHelp();
				System.exit(0);
			}
			unrecognisedArg(currentArg);
			System.exit(1);
		}
		
		/* setup classloader etc */
		if(outDir == null) {
			processOutpath(defaultOutpath);
		}
		InterfaceManager mgr = setupLoader(classPath);		

		/* process classes */
		for(int i = firstNonoptionArg; i < args.length; i++) {
			int result = processStub(mgr, mode, args[i]);
			if(result != 0) {
				System.err.println("Error processing stub: name = " + args[i] + "; error code = " + result);
			}
		}
	}

	private static String[] processClasspath(String path) {
		return path.split("\\:");
	}

	private static int processOutpath(String path) {
		outDir = new File(path);
		if(outDir.isDirectory()) return 0;
		if(outDir.exists()) {
			System.err.println("StubGen: unable to create output dir (" + path + "): specified output path is a file");
			return 5;
		}
		if(!outDir.mkdirs()) {
			System.err.println("StubGen: unable to create output dir (" + path + ")");
			return 6;
		}
		return 0;
	}

	private static int processMode(String modeStr) {
		if(modeStr == null) {
			System.err.println("Internal error: mode is null");
			return 7;
		}
		if(modeStr.equals("import"))
			mode = MODE.IMPORT;
		else if(modeStr.equals("export"))
			mode = MODE.EXPORT;
		else if(modeStr.equals("value"))
			mode = MODE.VALUE;
		else {
			System.err.println("StubGen: unrecognised mode option (" + modeStr + ")");
			return 8;
		}
		return 0;	
	}

	private static void processHelp() {
		System.err.println("StubGen");
		System.err.println("Usage: StubGen [--classpath <classpath>] [--out <outpath>] [--mode <mode>] [--verbose] classes");
		System.err.println("  classpath is path used to locate classe; multiple elements are separated by ':'");
		System.err.println("  outpath is path save stubs");
		System.err.println("  mode specifies the stub type; supported options are \"import\", \"export\", \"value\"");
		System.err.println("  each class argument must be fully qualified package + classname; '.' and '/' separators are supported");
	}

	private static void unrecognisedArg(String arg) {
		System.err.println("StubGen: unrecognised option: " + arg);
	}

	private static InterfaceManager setupLoader(String[] classPath) {
		ClassLoader loader = new DirectoryClassLoader(classPath);
		return new InterfaceManager(loader);
	}

	private static int processStub(InterfaceManager mgr, MODE mode, String name) {
		if(verbose) System.out.println("Loading class: " + name);
		IDLInterface iface = mgr.getByName(name);
		if(iface == null) {
			if(verbose) System.out.println("Class not found: " + name);
			return 1;
		}
		
		StubGenerator generator = null;
		switch(mode) {
		default:
		case IMPORT:
			generator = new ImportStubGenerator(mgr, iface, outDir);
			break;
		case EXPORT:
			generator = new ExportStubGenerator(mgr, iface, outDir);
			break;
		case VALUE:
			System.err.println("Unsupported mode");
			return 2;
		}
		try {
			generator.generate();
		} catch(IOException ioe) {
			System.err.println("StubGen: error processing class (" + name + ")");
			ioe.printStackTrace();
			return 3;
		} catch (GeneratorException ge) {
			System.err.println("StubGen: error processing class (" + name + ")");
			ge.printStackTrace();
			return 4;
		}
		return 0;
	}
}
