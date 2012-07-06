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
 * $Id: route.java,v 1.11 2007/02/09 23:12:50 hamada Exp $
 */
package net.jxta.impl.shell.bin.route;

import net.jxta.discovery.DiscoveryService;
import net.jxta.endpoint.EndpointService;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.endpoint.router.EndpointRouter;
import net.jxta.impl.endpoint.router.RouteControl;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peer.PeerID;
import net.jxta.protocol.RouteAdvertisement;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * route command list the current routes available
 */
public class route extends ShellApp {

    /**
     * "Enumeration" of operations
     */
    private enum RouterOperation {
        ROUTER_STATUS,
        GET_ALL_ROUTES_INFO,
        GET_ROUTE_INFO,
        ADD_ROUTE,
        DELETE_ROUTE,
        ENABLE_ROUTE_CM,
        DISABLE_ROUTE_CM,
        ENABLE_ROUTE_RESOLVER,
        DISABLE_ROUTE_RESOLVER,
        GET_MY_LOCAL_ROUTE
    }

    DiscoveryService discovery;

    ShellEnv myEnv;

    final Map<ID,String> names = new HashMap<ID,String>();

    public int startApp(String[] args) {

        int i;
        RouterOperation op = RouterOperation.GET_ALL_ROUTES_INFO;
        ShellObject obj;
        RouteAdvertisement routeAdv = null;
        PeerID pId = null;
        int ret;

        discovery = getGroup().getDiscoveryService();

        myEnv = getEnv();

        if (args != null) {
            for (i = 0; i < args.length; ++i) {
                if (args[i].equals("status")) { // router status

                    // no params
                    if (args.length != 1) {
                        return syntaxError();
                    }

                    op = RouterOperation.ROUTER_STATUS;
                    break;

                } else if (args[i].equals("enableCM")) { // router status

                    // no params
                    if (args.length != 1) {
                        return syntaxError();
                    }

                    op = RouterOperation.ENABLE_ROUTE_CM;
                    break;

                } else if (args[i].equals("disableCM")) { // router status

                    // no params
                    if (args.length != 1) {
                        return syntaxError();
                    }

                    op = RouterOperation.DISABLE_ROUTE_CM;
                    break;

                } else if (args[i].equals("enableResolver")) { // router status

                    // no params
                    if (args.length != 1) {
                        return syntaxError();
                    }

                    op = RouterOperation.ENABLE_ROUTE_RESOLVER;
                    break;

                } else if (args[i].equals("disableResolver")) { // router status

                    // no params
                    if (args.length != 1) {
                        return syntaxError();
                    }

                    op = RouterOperation.DISABLE_ROUTE_RESOLVER;
                    break;

                } else if (args[i].equals("delete")) { // delete a route

                    // need a PeerID :-)
                    if (args.length != 2) {
                        return syntaxError();
                    }

                    op = RouterOperation.DELETE_ROUTE;
                    // obtain the PeerID
                    pId = convertPeerID(args[i + 1]);

                    if (pId == null) {
                        consoleMessage("Error - Invalid PeerID argument");
                        return ShellApp.appParamError;
                    }
                    break;
                } else if (args[i].equals("add")) { // add a route

                    // need a PeerID :-)
                    if (args.length != 2) {
                        return syntaxError();
                    }

                    op = RouterOperation.ADD_ROUTE;

                    // obtain the RouteAdv to add the route
                    try {
                        obj = myEnv.get(args[i + 1]);
                        if (obj == null) {
                            consoleMessage("Error - Invalid RouteAdv argument");
                            return ShellApp.appParamError;
                        }
                        routeAdv = (RouteAdvertisement) obj.getObject();
                    } catch (ClassCastException ex) {
                        consoleMessage("Error - Invalid RouteAdv type argument");
                        return ShellApp.appParamError;
                    }

                    if (routeAdv == null) {
                        consoleMessage("Error - Invalid RouteAdv argument");
                        return ShellApp.appParamError;
                    }
                    break;

                } else if (args[i].equals("info")) { // get info for a  route

                    // need a PeerID :-)
                    if (args.length != 2) {
                        return syntaxError();
                    }

                    op = RouterOperation.GET_ROUTE_INFO;
                    // obtain the PeerID
                    pId = convertPeerID(args[i + 1]);

                    if (pId == null) {
                        consoleMessage("Error - Invalid PeerID argument");
                        return ShellApp.appParamError;
                    }
                    break;

                } else if (args[i].equals("local")) { // get info for my local route

                    if (args.length != 1) {
                        return syntaxError();
                    }

                    op = RouterOperation.GET_MY_LOCAL_ROUTE;
                    break;
                }

            }

            if (op == RouterOperation.GET_ALL_ROUTES_INFO && args.length != 0) {
                return syntaxError();
            }
        }

        // get the Router service
        try {
            EndpointService endpoint = getGroup().getEndpointService();
            EndpointRouter er = (EndpointRouter) endpoint.getMessageTransport("jxta");

            if (er == null) {
                consoleMessage("Error - Could not find the Router transport");
                return ShellApp.appMiscError;
            }

            // Get the RouteControl object
            RouteControl routeControl = (RouteControl) er.transportControl(EndpointRouter.GET_ROUTE_CONTROL, null);

            if (routeControl == null) {
                consoleMessage("Error - Could not obtain the Router routeControl Interface");
                return ShellApp.appMiscError;
            }

            if (RouterOperation.GET_MY_LOCAL_ROUTE == op) {
                // Get my local route

                println(routeControl.getMyLocalRoute().display());
            } else if (RouterOperation.ROUTER_STATUS == op) {
                // Get current router config

                println("use CM :" + routeControl.useRouteCM());
                println("use RouteResolver :" + routeControl.useRouteResolver());
            } else if (RouterOperation.ENABLE_ROUTE_CM == op) {
                // Enable routeCM

                routeControl.enableRouteCM();
            } else if (RouterOperation.DISABLE_ROUTE_CM == op) {
                // Disable routeCM

                routeControl.disableRouteCM();
            } else if (RouterOperation.ENABLE_ROUTE_RESOLVER == op) {
                // Enable routeResolver

                routeControl.enableRouteResolver();
            } else if (RouterOperation.DISABLE_ROUTE_RESOLVER == op) {
                // Disable routeResolver

                routeControl.disableRouteResolver();
            } else if (RouterOperation.GET_ALL_ROUTES_INFO == op) {
                // Get all route info

                List routes = routeControl.getAllRoutesInfo();

                if (routes == null) {
                    consoleMessage("Error - get all routes info failed");
                    return ShellApp.appMiscError;
                }

                println("Routes for PeerGroup : " + getGroup());
                for (Object route : routes) {
                    RouteAdvertisement aRouteAdv = (RouteAdvertisement) route;
                    println(aRouteAdv.display());
                }
            } else if (RouterOperation.GET_ROUTE_INFO == op) {
                // Get route info for a destination

                routeAdv = routeControl.getRouteInfo(pId);

                if (routeAdv == null) {
                    consoleMessage("Error - No route found");
                    return ShellApp.appNoError;
                }

                println(routeAdv.display());
            } else if (RouterOperation.DELETE_ROUTE == op) {
                // Delete a route

                ret = routeControl.deleteRoute(pId);

                if (ret == RouteControl.FAILED) {
                    consoleMessage("Error - Route delete failed");
                    return ShellApp.appMiscError;
                }

                if (ret == RouteControl.DIRECT_ROUTE) {
                    consoleMessage("Error - Route delete failed as this is a direct route");
                    return ShellApp.appMiscError;
                }

                if (ret == RouteControl.INVALID_ROUTE) {
                    consoleMessage("Error - Route delete failed as this is the peer route");
                    return ShellApp.appMiscError;
                }

                println("Delete route succeeded");
            } else if (RouterOperation.ADD_ROUTE == op) {
                // Add route

                ret = routeControl.addRoute(routeAdv);

                if (ret == RouteControl.FAILED) {
                    consoleMessage("Error - Invalid route could not establish route");
                    return ShellApp.appMiscError;
                }

                if (ret == RouteControl.ALREADY_EXIST) {
                    consoleMessage("Error - Route add failed as we already have a direct route");
                    return ShellApp.appMiscError;
                }

                if (ret == RouteControl.INVALID_ROUTE) {
                    consoleMessage("Error - Route add failed cannot change our peer route");
                    return ShellApp.appMiscError;
                }

                println("Add route succeeded");
            } else {
                consoleMessage("Error - unrecognized operation");
            }
        } catch (Exception ex) {
            printStackTrace("Error - failure performing operation", ex);
        }

        return ShellApp.appNoError;
    }

    public int syntaxError() {
        consoleMessage("Error - incorrect arguments");
        return ShellApp.appParamError;
    }

    @Override
    public String getDescription() {
        return "Display information about a peer's route info";
    }

    @Override
    public void help() {
        println("NAME");
        println("     route - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     route [-l] [info <peerID>|add <routeAdv>|delete <peerID>|status|enableCM|disableCM|enableResolver|disableResolver]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("route displays and manipulates information about the peer's ");
        println("route information.");
        println(" ");
        println("Direct routes are routes which the peer has a direct");
        println("connection with. Gateway routes are routes that involved");
        println("intermediary gateways (hops).");
        println("");
        println("OPTIONS");
        println(" ");
        println(" [-l]            Dispaly peer IDs as well as peer names");
        println(" ");
        println("COMMANDS");
        println(" ");
        println(" status          : Router status info ");
        println(" local           : Print local peer route info ");
        println(" info <peerID>   : Obtain route information about a specific destination ");
        println(" add <env1>      : Add new route information specified as a route advertisement");
        println("                   stored in the environment variable env1");
        println(" delete <peerID> : Delete route information for that destination ");
        println(" enableCM        : enable persistent route CM cache ");
        println(" disableCM       : disable persistent route CM cache ");
        println(" enableResolver  : enable dynamic route resolution ");
        println(" disableResolver : disable dynamic route resolution ");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>route");
        println(" ");
        println(" JXTA>route del urn:jxta:uuid-59616261646162614A787461503250330F5CE9DD8DE84079AA6F8C2C0F8B16DA03");
        println("      Delete route succeeded");
        println(" ");
        println(" ");
        println("This example displays all the route information");
        println(" ");
        println("SEE ALSO");
        println("    whoami peers");
    }
    // from a string get a PeerID
    private static PeerID convertPeerID(String pid) {

        PeerID id;

        try {
            URI pID = new URI(pid);
            id = (PeerID) IDFactory.fromURI(pID);
        } catch (URISyntaxException badID) {
            return null;
        }

        return id;
    }
}
