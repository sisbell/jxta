/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: ShellCmds.java,v 1.23 2007/05/23 22:37:54 bondolo Exp $
 */


package net.jxta.impl.shell;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

/**
 * This  class has methods for getting an array of all available Shell commands
 * and for instantiating of a specific Shell command (the standard constructor is used!).
 * It uses a ClassLoader that can load class-files from an arbitrary number of jar-files.
 * The list of jar-files is encoded in the INST_JARS environment variable of the ShellApp
 * that instantiated this class.
 * The encoded String has following format:
 * <path1><PATH_SEPARATOR><path2><PATH_SEPARATOR>...
 * <p/>
 * Whenever a ShellApp needs access to the Shell commands this class should be used.
 * For example to get all available Shell commands use the following code:
 * <code>
 * ShellCmds cmds = new ShellCmds((ShellApp)this);
 * String[] cmdList = cmds.list();
 * </code>
 * To instantiate a command you could use following code:
 * <code>
 * // Instantiate the 'man' command.
 * ShellApp app = cmds.getInstance("man");
 * // Now set the group and the input and output pipes if neccessary.
 * ...
 * // For example we want to print the short description of the app.
 * println(app.getDescription());
 * </code>
 */
public final class ShellCmds {
    /**
     * For debugging purposes.
     */
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ShellCmds.class.getName());
    
    /**
     * Name of environment variable.
     */
    public static final String INST_JARS = "instjars";
    
    /**
     * Char used to separate paths.
     */
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");
    
    static final String BIN_PACKAGE = "net.jxta.impl.shell.bin";
    static final String BIN_PATH = "net/jxta/impl/shell/bin/";
    static final int BIN_PATH_LEN = BIN_PATH.length();
    
    /**
     * Reference to the ShellApp that instantiated this class.
     */
    private final ShellEnv env;
    
    
    /**
     * Initializes a newly created ShellCmds object with a reference to the instantiating ShellApp.
     * This reference will be used to obtain its current ShellEnv object which serves for reading
     * and storing the INST_JARS environment variable.
     *
     * @param env the environment to use.
     */
    public ShellCmds(ShellEnv env) {
        this.env = env;
    }
    
    /**
     * Static method to extract the list of paths out of a String. The format of the String is:
     * <path1><PATH_SEPARATOR><path2>...
     * Only correct paths are added to the later returned list.
     *
     * @param pathList the String the paths will be extracted from.
     * @return an array of all valid paths. If the given parameter contains no path or none
     *         of the given paths is valid then an empty list is returned.
     */
    public static URL[] parseClassPath(String pathList) {
        
        List<URL> pathURLS = new ArrayList<URL>();
        
        StringTokenizer tok = new StringTokenizer(pathList, PATH_SEPARATOR, false);
        while (tok.hasMoreTokens()) {
            String fileName = tok.nextToken();
            File file = new File(fileName);
            try {
                pathURLS.add(file.toURI().toURL());
            } catch (MalformedURLException bad) {
                //ignored
            }
        }
        
        return pathURLS.toArray(new URL[pathURLS.size()]);
    }
    
    /**
     * Static method to encode a list of paths into its corresponding String representation.
     * The format of the String is: <path1><PATH_SEPARATOR><path2>...
     *
     * @param pathList the list of paths to be encoded.
     * @return a String representation of the given list.
     */
    public static String encodeInstJars(List pathList) {
        StringBuilder buf = new StringBuilder();
        
        for (int i = 0, size = pathList.size(); i < size; i++) {
            buf.append(pathList.get(i));
            if (i < size - 1)
                buf.append(PATH_SEPARATOR);
        }
        return buf.toString();
    }
    
    /**
     * Stores the String representation of the given list of paths in the environment
     * of the ShellApp.
     *
     * @param pathList list of paths to be stored in the environment of the ShellApp.
     */
    public void setInstJars(List pathList) {
        // Convert list into its corresponding String representation,
        String instJars = encodeInstJars(pathList);
        // and set the new value of environment variable INST_JARS.
        env.add(INST_JARS, new ShellObject<String>("Installed Jar Files", instJars));
    }
    
    /**
     * Retrieves the list of paths encoded as a String from the ShellApp's environment.
     *
     * @return a list of path objects.
     */
    public URL[] getInstJars() {
        
        if (env.contains(INST_JARS)) {
            // If environment variable INST_JARS already exists, then
            // get environment variable INST_JARS, and
            // extract zip-files and directories and construct a list containing them.
            String instJars = (String) env.get(INST_JARS).getObject();
            return parseClassPath(instJars);
        } else {
            // else return an empty list.
            return new URL[0];
        }
    }
    
    /**
     * Returns an array of all available Shell commands.
     * <p/>
     * <p/>The current implementation tries to locate commands on the classpath
     * pf the classloader
     *
     * @return an array of the names of all the available shell commands.
     */
    public String[] list() {
        List<String> list = cmdlist();
        
        return list.toArray(new String[list.size()]);
    }
    
    /**
     * Returns a list of all available Shell commands.
     * <p/>
     * <p/>The current implementation tries to locate commands on the classpath
     * pf the classloader
     *
     * @return list of the names of all the available shell commands.
     */
    public List<String> cmdlist() {
        List<URL> classpath = new ArrayList<URL>();
        // Retrieve the list of paths representing jar-files.
        classpath.addAll(Arrays.asList(parseClassPath(System.getProperty("java.class.path"))));
        classpath.addAll(Arrays.asList(getInstJars()));
        
        // Initialize a ClassLoader that also searches jar-files for classes.
        URLClassLoader scl = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        
        List<String> cmds = new ArrayList<String>();
        
        classpath.addAll(Arrays.asList(scl.getURLs()));
        
        for (URL aURL : classpath) {
            try {
                if (aURL.toString().endsWith("/")) {
                    // A Directory
                    if (!"file".equalsIgnoreCase(aURL.getProtocol())) {
                        continue;
                    }
                    File dir = new File(new File(aURL.getPath()), BIN_PATH);
                    if (dir.exists()) {
                        File[] dirCmds = dir.listFiles();
                        for (File file : Arrays.asList(dirCmds)) {
                            try {
                                if (file.isDirectory()) {
                                    File[] someCmds = file.listFiles();
                                    for (File file1 : Arrays.asList(someCmds)) {
                                        if (file1.getName().endsWith(".class")) {
                                            String filename = file1.getName();
                                            filename = filename.substring(0, filename.length() - 6);
                                            if (file.getName().equals(filename)) {
                                                if (null != getInstance(filename)) {
                                                    cmds.add(filename);
                                                }
                                            } else {
                                                if (null != getInstance(file.getName() + "." + filename)) {
                                                    cmds.add(file.getName() + "." + filename);
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ignored) {
                                //ignored
                            }
                        }
                    }
                } else {
                    // A class or Jar
                    String entryName = aURL.getPath();
                    if (entryName.endsWith(".class")) {
                        // A class file
                        entryName = entryName.substring(0, entryName.length() - 6);
                        int cmdPath = entryName.indexOf(BIN_PATH);
                        if (-1 != cmdPath) {
                            String cmdName = entryName.substring(cmdPath + BIN_PATH_LEN);
                            cmds.add(cmdName);
                        }
                    } else if (entryName.endsWith(".jar")) {
                        // A jar file
                        Enumeration<JarEntry> eachEntry;
                        
                        if ("jar".equalsIgnoreCase(aURL.getProtocol())) {
                            JarURLConnection jar = (JarURLConnection) aURL.openConnection();
                            JarFile jarfile = jar.getJarFile();
                            eachEntry = jarfile.entries();
                        } else {
                            URLConnection jar = aURL.openConnection();
                            JarInputStream jis = new JarInputStream(jar.getInputStream());
                            List<JarEntry> allEntries = new ArrayList<JarEntry>();
                            JarEntry anEntry = jis.getNextJarEntry();
                            while (null != anEntry) {
                                allEntries.add(anEntry);
                                anEntry = jis.getNextJarEntry();
                            }
                            eachEntry = Collections.enumeration(allEntries);
                        }
                        
                        while (eachEntry.hasMoreElements()) {
                            JarEntry anEntry = eachEntry.nextElement();
                            entryName = anEntry.getName();
                            if (entryName.endsWith(".class")) {
                                entryName = entryName.substring(0, entryName.length() - 6);
                                int cmdPath = entryName.indexOf(BIN_PATH);
                                if (-1 != cmdPath) {
                                    String leafPath = entryName.substring(cmdPath + BIN_PATH_LEN);
                                    String pkgName = leafPath.substring(0, leafPath.indexOf('/'));
                                    String cmdName = leafPath.substring(leafPath.indexOf('/') + 1);
                                    if (pkgName.equals(cmdName)) {
                                        cmds.add(cmdName);
                                    } else {
                                        cmds.add(pkgName + "." + cmdName);
                                    }
                                }
                            }
                        }
                    } else {
                        // Unknown file
                    }
                }
            } catch (Exception failure) {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, " Failure processing : " + ((null != aURL) ? aURL.toString() : ""), failure);
                }
            }
        }
        
        Collections.sort(cmds);
        
        return cmds;
    }
    
    /**
     * Instantiates a given Command and returns its reference. For this purpose this implementation
     * tries to load the class 'net.jxta.impl.shell.bin.pkg.cmd'. This class is then instantiated through
     * the standard constructor. No other initialization is done, so the caller of this method should
     * set the group, outputPipe, etc.
     *
     * @param cmd The command to execute. In the form <pkg> "." <cmd>.
     * @return an instance of the specified Shell command.
     */
    public ShellApp getInstance(String cmd) {
        URL[] pathURLs = getInstJars();
        
        // Initialize a ClassLoader that also searches jar-files for classes.
        URLClassLoader scl = new URLClassLoader(pathURLs, this.getClass().getClassLoader());
        
        // Make sure that the Context loader of the current thread
        // is properly set, so if the shell commands creates its own
        // thread, the Shell class loader will also be used.
        Thread.currentThread().setContextClassLoader(scl);
        
        // try just loading the class by name
        Class<ShellApp> appClass = null;
        
        try {
            appClass = (Class<ShellApp>) scl.loadClass(cmd);
        } catch (NoClassDefFoundError notFound) {
            //ignored
        } catch (ClassNotFoundException notFound) {
            //ignored
        }
        
        if (null == appClass) {
            String cmdPkg;
            String cmdcmdName;
            
            if (-1 == cmd.indexOf('.')) {
                cmdPkg = cmd;
                cmdcmdName = cmd;
            } else {
                cmdPkg = cmd.substring(0, cmd.lastIndexOf('.'));
                cmdcmdName = cmd.substring(cmd.lastIndexOf('.') + 1);
            }
            String cmdclassName = BIN_PACKAGE + "." + cmdPkg + "." + cmdcmdName;
            
            if (LOG.isLoggable(java.util.logging.Level.FINE))
                LOG.finer("Loading command : " + cmdclassName);
            // Load the class with the help of scl.
            try {
                appClass = (Class<ShellApp>) Class.forName(cmdclassName, true, scl);
            } catch (ClassNotFoundException failed) {
                return null;
            }
            if (!ShellApp.class.isAssignableFrom(appClass)) {
                return null;
            }
            try {
                if (null == appClass.getConstructor()) {
                    // missing required constructor.
                    return null;
                }
            } catch (NoSuchMethodException missingConstructor) {
                return null;
            }
        }
        
        // Instantiate loaded command class and return it.
        try {
            ShellApp app = appClass.newInstance();
            return app;
        } catch (java.lang.InstantiationException failed) {
            throw new UndeclaredThrowableException(failed);
        } catch (java.lang.IllegalAccessException failed) {
            return null;
        }
    }
}
