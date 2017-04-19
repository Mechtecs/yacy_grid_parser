/**
 *  Parser
 *  Copyright 1.04.2017 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.grid.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.log4j.BasicConfigurator;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIServer;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.mcp.MCP;
import net.yacy.grid.mcp.api.info.ServicesService;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.parser.api.ParserService;
import net.yacy.grid.tools.MapUtil;


public class Parser {
    
    public final static String DATA_PATH = "data";
    public final static String APP_PATH = "parser";
    
    // define services
    @SuppressWarnings("unchecked")
    public final static Class<? extends Servlet>[] services = new Class[]{
            // information services
            ServicesService.class,
            StatusService.class,
            ParserService.class
    };
    
    public static void main(String[] args) {
        // run in headless mode
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff

        // configure logging
        BasicConfigurator.configure();

        // define service port
        int port = YaCyServices.yacyparser.getDefaultPort();
        
        // load the config file(s);
        File conf_dir = FileSystems.getDefault().getPath("conf").toFile();
        File dataFile = new File(new File(FileSystems.getDefault().getPath(DATA_PATH).toFile(), APP_PATH + "-" + port), "conf");
        String confFileName = "config.properties";
        Map<String, String> config = null;
        try {
            config = MapUtil.readConfig(conf_dir, dataFile, confFileName);
        } catch (IOException e1) {
            e1.printStackTrace();
            System.exit(-1);
        }
        
        // read the port again and then read also the configuration again because the path of the custom settings may have moved
        port = Integer.parseInt(config.get("port"));
        dataFile = new File(new File(FileSystems.getDefault().getPath(DATA_PATH).toFile(), APP_PATH + "-" + port), "conf");
        try {
            config = MapUtil.readConfig(conf_dir, dataFile, confFileName);
        } catch (IOException e1) {
            e1.printStackTrace();
            System.exit(-1);
        }

        // start server
        APIServer.init(MCP.services);
        APIServer.init(Parser.services);
        try {
            
            // find data path
            File data = FileSystems.getDefault().getPath("data").toFile();
            Data.init(new File(data, APP_PATH + "-" + port), config);
            
            // open the server on available port
            boolean portForce = Boolean.getBoolean(config.get("port.force"));
            port = APIServer.open(port, portForce);

            // give positive feedback
            Data.logger.info("Service started at port " + port);

            // prepare shutdown signal
            File pid = new File(data, APP_PATH + "-" + port + ".pid");
            if (pid.exists()) pid.delete(); // clean up rubbish
            pid.createNewFile();
            pid.deleteOnExit();
            
            // wait for shutdown signal (kill on process)
            APIServer.join();
        } catch (IOException e) {
            Data.logger.error("Main fail", e);
        }
        
        Data.close();
    }
    
}
