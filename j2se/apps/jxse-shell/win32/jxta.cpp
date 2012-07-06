/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
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
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
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
 * $Id: jxta.cpp,v 1.19 2003/02/14 20:09:11 akhil Exp $
 */

/*
  This is a win32 launcher to avoid the launching of an extra DOS window
  when launching a jre

*/


#include <windows.h> 
#include <process.h> 
#include <stdio.h>   
#include <malloc.h>   


#define JXTA_HOME_KEY   "JXTA_HOME"
#define PRINCIPAL_KEY   "net.jxta.tls.principal"
#define PASSWORD_KEY    "net.jxta.tls.password"


static BOOL GetPublicJREHome(char *buf, int bufsize);
static BOOL GetProxy(char *buf, int bufsize);
static void AddJarsToClasspath(char* classpath, const char* dir);


static BOOL
exist(char *filename)
{
    FILE *f;

    if ((f = fopen(filename, "r")) == NULL) {
        return FALSE;
    } else {
        fclose(f);
        return TRUE;
    }
}


int
main(int argc, char *argv[])
{
    char quoted_command[10*MAX_PATH + 1 + 2];
    char classpath[10*MAX_PATH + 1];
    char command[MAX_PATH + 1];
    char proxy[MAX_PATH + 1];
    char mainclass[MAX_PATH + 1];
    char windir[MAX_PATH + 1];
    char cp[4];
    char *jxta_home;
    char *principal;
    char *password;
    char **progargv, **argseq;

    GetWindowsDirectory(windir, sizeof(windir));

    command[0] = 0;

    if (GetPublicJREHome(command, sizeof(command))) {
        strcat(command, "\\bin\\javaw.exe");
    }

    if (!exist(command)) {
        fprintf(stderr, "Error finding Java, aborting");
        return 1;
    }

    // javaw does not like spaces in command strings, so quote
    if (GetProxy(proxy, sizeof(proxy))) {
      sprintf(quoted_command, "\"%s\" -Djxta.proxy=\"%s\"", command, proxy);
    } else sprintf(quoted_command, "\"%s\"", command, proxy);

    jxta_home = getenv(JXTA_HOME_KEY);
    if (jxta_home != NULL) {
        strcat(quoted_command, " -D");
        strcat(quoted_command, JXTA_HOME_KEY);
        strcat(quoted_command, "=");
        strcat(quoted_command, jxta_home);
    }

    principal = getenv(PRINCIPAL_KEY);
    if (principal != NULL) {
        strcat(quoted_command, " -D");
        strcat(quoted_command, PRINCIPAL_KEY);
        strcat(quoted_command, "=");
        strcat(quoted_command, principal);
    }

    password  = getenv(PASSWORD_KEY);
    if (password != NULL) {
        strcat(quoted_command, " -D");
        strcat(quoted_command, PASSWORD_KEY);
        strcat(quoted_command, "=");
        strcat(quoted_command, password);
    }

    // Make sure we are running from the right directory.
    DWORD fattrs1 = GetFileAttributes("..\\dist\\jxtashell.jar");
    DWORD fattrs2 = GetFileAttributes("..\\lib\\jxtashell.jar");
    if (fattrs1 == 0xFFFFFFFF && fattrs2 == 0xFFFFFFFF) {
        MessageBox(NULL, 
                   "Sorry, cannot locate jxtashell.jar in \"..\\dist\" or "
                   "\"..\\lib\" \n"
                   "Did you run this program from the correct directory?", 
                   "JXTA launcher", MB_OK);
        return 1;
    }

    sprintf(cp, "-cp");

    sprintf(classpath, ".");

    // look for jars in all probable dirs

    AddJarsToClasspath(classpath, ".");
    AddJarsToClasspath(classpath, ".\\lib");
    AddJarsToClasspath(classpath, ".\\dist");

    AddJarsToClasspath(classpath, "..\\lib");
    AddJarsToClasspath(classpath, "..\\dist");

    sprintf(mainclass, "net.jxta.impl.peergroup.Boot");

    // echo command being executed, invaluable for helping newbies
    printf("%s %s %s %s\n", quoted_command, cp, classpath, mainclass);

    progargv = argseq = (char **) calloc(5, sizeof(char *));
    *argseq++ = quoted_command;
    *argseq++ = cp;
    *argseq++ = classpath;
    *argseq++ = mainclass;

#ifdef DEBUG
    char buff[2000] = {0};
    for(int i=0; progargv[i] != NULL; ++i) {
        strcat(buff, progargv[i]);
        strcat(buff, "\n");
    }

    MessageBox(NULL, buff, "JXTA launcher debug", MB_OK);
#endif
	
    return(execv(command, progargv));
}

/*
 * Add all jar found in the specified directory to the classpath
 */
static void 
AddJarsToClasspath(char* classpath, const char* dir) 
{
    WIN32_FIND_DATA ffd;
    char path[MAX_PATH + 1];

    sprintf(path, "%s\\*.jar", dir);
    HANDLE ff = FindFirstFile(path, &ffd);
    if (ff == INVALID_HANDLE_VALUE) {
        // not fatal, keep trying other directories
        return;
    }
       
    do {
        strcat(classpath, ";");
        strcat(classpath, dir);
        strcat(classpath, "\\");
        strcat(classpath, ffd.cFileName);
    } while (FindNextFile(ff, &ffd));

    FindClose(ff); 
}


/*
 * Helper to look in the registry for a public JRE.
 */
static BOOL
GetStringFromRegistry(HKEY key, const char *name, char *buf, int bufsize)
{
    DWORD type, size;

    if (RegQueryValueEx(key, name, 0, &type, 0, &size) == 0
        && type == REG_SZ
        && (size < (unsigned int)bufsize)) {
        if (RegQueryValueEx(key, name, 0, 0, (PUCHAR)buf, &size) == 0) {
            return TRUE;
        }
    }
    return FALSE;
}


#define JRE_KEY   "Software\\JavaSoft\\Java Runtime Environment"
#define PROXY_KEY "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings"


/*
 * The algorithm for determining JRE location is based on the following
 * registry information:
 *
 * Software\JavaSoft\Java Runtime Environment\CurrentVersion -> "1.3"
 *
 * Software\JavaSoft\Java Runtime Environment\$CurrentVersion\JavaHome -> 
 *     "c:\program files\jdk1.3.0_02"
 */
static BOOL
GetPublicJREHome(char *buf, int bufsize)
{
    HKEY key, subkey;
    char version[MAX_PATH + 1];

    /* Find the current version of the JRE. */
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, JRE_KEY, 0, KEY_READ, &key) != 0) {
        fprintf(stderr, "Error opening registry key '" JRE_KEY "'\n");
        return FALSE;
    }

    if (!GetStringFromRegistry(key, "CurrentVersion",
                               version, sizeof(version))) {
        fprintf(stderr, "Failed reading value of registry key:\n\t"
                JRE_KEY "\\CurrentVersion\n");
        RegCloseKey(key);
        return FALSE;
    }

    /* Find directory where the current version is installed. */
    if (RegOpenKeyEx(key, version, 0, KEY_READ, &subkey) != 0) {
        fprintf(stderr, "Error opening registry key '"
                JRE_KEY "\\%s'\n", version);
        RegCloseKey(key);
        return FALSE;
    }

    if (!GetStringFromRegistry(subkey, "JavaHome", buf, bufsize)) {
        fprintf(stderr, "Failed reading value of registry key:\n\t"
                JRE_KEY "\\%s\\JavaHome\n", version);
        RegCloseKey(key);
        RegCloseKey(subkey);
        return FALSE;
    }

    RegCloseKey(key);
    RegCloseKey(subkey);
    return TRUE;
}

/*
 * The algorithm for getting the proxy setting in windows
 * *
 * Software\Microsoft\Windows\CurrentVersion\Internet Settings\ProxyServer -> *     "proxyserver:8080"
 */
static BOOL
GetProxy(char *buf, int bufsize)
{
        HKEY key;
        char enabled[MAX_PATH + 1];


        if (RegOpenKeyEx(HKEY_CURRENT_USER, PROXY_KEY, 0, KEY_READ, &key) != 0)
        {
                fprintf(stderr, "Error opening registry key '" PROXY_KEY "'\n\n");
                return FALSE;
        }
        
        if (!GetStringFromRegistry(key, "ProxyServer", buf, bufsize))
        {
                fprintf(stderr, "Failed reading value of registry key:\n\t"
                        PROXY_KEY "\\%s\\ProxyServer\n", "ProxyServer");
                RegCloseKey(key);
                return FALSE;
        }
        RegCloseKey(key);
        return TRUE;
}

int APIENTRY
WinMain(HINSTANCE hInstance,
        HINSTANCE hPrevInstance,
        LPSTR     lpCmdLine,
        int       nCmdShow)
{
    int ret;
    ret = main(__argc, __argv);
    exit(ret);
    return 0;
}
