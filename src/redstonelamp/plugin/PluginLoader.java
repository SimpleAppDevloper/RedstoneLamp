package redstonelamp.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import redstonelamp.RedstoneLamp;
import redstonelamp.Server;
import redstonelamp.event.Event;
import redstonelamp.event.EventException;
import redstonelamp.event.EventHandler;
import redstonelamp.event.EventPriority;
import redstonelamp.event.Listener;

public class PluginLoader {
	private String PLUGIN_CLASS_FOLDER = "";
	private HashMap<String, Plugin> pluginMap = new HashMap<String, Plugin>();
	private Iterable<String> options;
	private final String JAVA_HOME = "java.home";
	private URL pluginURL;
	private Set<String> clsNames = new TreeSet<String>();
	private Server server;
	JavaCompiler compiler = null;
	private Set<String> urlClsNames = new TreeSet<String>();
	
	public PluginLoader(final Server server) {
		this.server = server;
	}
	
	public Plugin loadPlugin(String plugin) {
		RedstoneLamp.logger.debug(": inside loadPlugin() method ");
		PluginBase base = null;
		try {
			URL[] classUrls = {
				pluginURL
			};
			URLClassLoader ucl = new URLClassLoader(classUrls);
			Class<?> c = ucl.loadClass(plugin);
			if(Plugin.class.isAssignableFrom(c)) {
				RedstoneLamp.logger.info(": Loading " + plugin);
				base = (PluginBase) c.newInstance();
				String name = removeDotFromString(plugin);
				base.setName(name);
				initPlugin(base);
				enablePlugin(base);
				pluginMap.put(name, base);
				ucl.close();
			} else {
				RedstoneLamp.logger.info(": " + plugin + " is not a valid plugin. It should implement Plugin interface ");
			}
		} catch(Exception e) {
			e.printStackTrace();
			RedstoneLamp.logger.error("Unable to load plugin " + plugin + " (Plugins not supported)");
		}
		RedstoneLamp.logger.debug(": returns from loadPlugin() method ");
		return base;
	}
	
	private void initPlugin(PluginBase base) {
		base.init(this, server, null, null, null);
		server.getCommandRegistrationManager().setPlugin(base);
		base.onLoad();
	}
	
	public void enablePlugin(PluginBase base) {
		RedstoneLamp.logger.debug(":  Enabling " + base.getName());
		base.setEnabled(true);
	}
	
	public void disablePlugin(Plugin base) {
		((PluginBase) base).setEnabled(false);
	}
	
	public Plugin getPlugin(final String name) {
		Plugin plugin = pluginMap.get(name);
		if(plugin.isEnabled())
			return pluginMap.get(name);
		else
			return null;
	}
	
	private String removeDotFromString(String str) {
		if(str.lastIndexOf('.') > 0) {
			str = str.substring(str.lastIndexOf('.') + 1, str.length());
			return str;
		}
		return str;
	}
	
	public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, final Plugin plugin) {
		Map<Class<? extends Event>, Set<RegisteredListener>> ret = new HashMap<Class<? extends Event>, Set<RegisteredListener>>();
		Set<Method> methods;
		try {
			Method[] publicMethods = listener.getClass().getMethods();
			methods = new HashSet<Method>(publicMethods.length, Float.MAX_VALUE);
			for(Method method : publicMethods) {
				methods.add(method);
			}
			for(Method method : listener.getClass().getDeclaredMethods()) {
				methods.add(method);
			}
		} catch(NoClassDefFoundError e) {
			RedstoneLamp.logger.error("Plugin " + plugin.getName() + " has failed to register events for " + listener.getClass() + " because " + e.getMessage() + " does not exist.");
			return ret;
		}
		
		for(final Method method : methods) {
			final EventHandler eh = method.getAnnotation(EventHandler.class);
			EventPriority ep = EventPriority.HIGH;
			final Class<?> checkClass;
			if(method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
				//RedstoneLamp.logger.error(plugin.getName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + listener.getClass());
				continue;
			} else {
				RedstoneLamp.logger.info(plugin.getName() + " registered a method signature \"" + method.toGenericString() + "\" in " + listener.getClass());
			}
			final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
			method.setAccessible(true);
			Set<RegisteredListener> eventSet = ret.get(eventClass);
			if(eventSet == null) {
				eventSet = new HashSet<RegisteredListener>();
				ret.put(eventClass, eventSet);
			}
			
			EventExecutor executor = new EventExecutor() {
				@Override
				public void execute(Listener listener, Event event) throws EventException {
					try {
						if(!eventClass.isAssignableFrom(event.getClass())) { return; }
						method.invoke(listener, event);
					} catch(IllegalAccessException e) {
						e.printStackTrace();
						RedstoneLamp.logger.error(e.getMessage());
					} catch(IllegalArgumentException e) {
						e.printStackTrace();
						RedstoneLamp.logger.error(e.getMessage());
					} catch(InvocationTargetException e) {
						e.printStackTrace();
						RedstoneLamp.logger.error(e.getMessage());
					}
					
				}
			};
			eventSet.add(new RegisteredListener(listener, executor, ep, plugin, false));
			
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	private void generatePluginJavaClassFile(final File file) {
		if(compiler == null) { return; }
		PluginDiagnosticListener plistener = new PluginDiagnosticListener();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(Arrays.asList(file.getPath()));
		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, plistener, options, null, compilationUnits);
		boolean success = task.call();
		try {
			fileManager.close();
		} catch(IOException e) {
			e.printStackTrace();
			RedstoneLamp.logger.error(e.getMessage());
		}
		if(success) {
			RedstoneLamp.logger.info(" :Compilation Success! Class files are generated in in-use folder");
		} else {
			RedstoneLamp.logger.error(" :Plug-in Compilation Failed");
		}
	}
	
	public ArrayList<Plugin> loadJavaPlugins() {
		getFullyQualifiedName();
		ArrayList<Plugin> list = new ArrayList<Plugin>();
		for(String plugin : clsNames) {
			Plugin p = loadPlugin(plugin);
			if(p != null)
			list.add(p);
		}
		return list;
	}
	
	/*
	 * reloads a plug-in file
	 */
	public void loadJavaPlugin(String file, String pkg) {
		getFullyQualifiedName();
		file = file.substring(file.lastIndexOf("\\")+1, file.indexOf(".java"));
		if( pkg != null && pkg.trim().length()>0) {
			pkg = pkg + "." + file;
			pkg = pkg.trim();
		}else {
			pkg = file;
			pkg = pkg.trim();
		}
		for(String plugin : clsNames) {
			if( plugin.equals(pkg)) {
				loadPlugin(plugin);
				break;
			}
		}
	}
	
	public void preparePluginFiles(File file) {
		generatePluginJavaClassFile(file);
	}
	
	@SuppressWarnings("deprecation")
	public void setPluginOption(final String pfolder, final String folder) {
		PLUGIN_CLASS_FOLDER = folder;
		options = Arrays.asList(new String[]{
				"-d", folder
		});
		try {
			pluginURL = new File(PLUGIN_CLASS_FOLDER).toURL();
		} catch(MalformedURLException e) {
			e.printStackTrace();
		}
		if(System.getenv().get("JAVA_HOME") != null) {
			System.setProperty(JAVA_HOME, System.getenv().get("JAVA_HOME"));
			compiler = ToolProvider.getSystemJavaCompiler();
			if(compiler == null) {
				RedstoneLamp.logger.error(": No Java SDK path is set. Please set one.");
			}
		} else {
			RedstoneLamp.logger.error(": No Java SDK path is set. Please set one.");
		}
		compiler = ToolProvider.getSystemJavaCompiler();
	}
	
	private void getFullyQualifiedName() {
		File f = new File(PLUGIN_CLASS_FOLDER);
		String path = f.getAbsolutePath();
		listFiles(path, path);
		if(!(compiler == null))
			RedstoneLamp.logger.info("Fully qualified plugins " + clsNames);
	}
	
	private void listFiles(String path, String orig) {
		File root = new File(path);
		File[] list = root.listFiles();
		if(list == null)
			return;
		for(File f : list) {
			if(f.isDirectory()) {
				listFiles(f.getAbsolutePath(), orig);
			} else if(f.isFile()) {
				String dir = f.getAbsolutePath();
				String pkg = dir.substring(dir.indexOf(orig) + orig.length() + 1, dir.length());
				pkg = pkg.substring(0, pkg.indexOf(".class")).replaceAll("\\\\", ".");
				clsNames.add(pkg);
//					try {
//						System.out.println(f.toURL());
//						urlClsNames.add(refineFilePath(f.toURL().toExternalForm()));
//					} catch (MalformedURLException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	class PluginDiagnosticListener implements DiagnosticListener {
		@Override
		public void report(Diagnostic diagnostic) {
			RedstoneLamp.logger.info("Code->" + diagnostic.getCode());
			RedstoneLamp.logger.info("Column Number->" + diagnostic.getColumnNumber());
			RedstoneLamp.logger.info("End Position->" + diagnostic.getEndPosition());
			RedstoneLamp.logger.info("Kind->" + diagnostic.getKind());
			RedstoneLamp.logger.info("Line Number->" + diagnostic.getLineNumber());
			RedstoneLamp.logger.info("Message->" + diagnostic.getMessage(Locale.ENGLISH));
			RedstoneLamp.logger.info("Position->" + diagnostic.getPosition());
			RedstoneLamp.logger.info("Source" + diagnostic.getSource());
			RedstoneLamp.logger.info("Start Position->" + diagnostic.getStartPosition());
			RedstoneLamp.logger.info("\n");
		}
	}
	
	private String refineFilePath(String str) {
	   if(str.contains("/./plugins")) {
	       String f1 = str.substring(0, str.indexOf("/./"));
	       String f2 = str.substring(str.indexOf("/./")+2, str.length());
	       String f3 = f1.substring(0, f1.indexOf("/"));
	       String f4 = f1.substring(f1.indexOf("/") + 1, f1.length());
	       str = f3+f4+f2;
	       System.out.println(" str: " + str);
	   }
	   return str;
	}
}
