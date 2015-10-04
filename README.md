SageTV-JettyStarter
-------------------

SageTV-JettyStarter is a plugin for SageTV that embeds the Jetty web server in the SageTV process. It is installed and configured using SageTV's plugin manager.

The plugin hosts other plugins that are web applications such as the Desktop (aka nielm's) Web Server, Mobile Web Server, Sagex Remote APIs, SageAlert, Sage Recording Extender, and Batch Metadata Tools Web Interface.

Installation
------------

* Navigate to the Plugin Manager
* 


Development
-----------
1. Install the Apache Ant build tool
2. Fork the repository at https://github.com/jreicheneker/SageTV-JettyStarter
3. Clone the forked repository to your machine
4. Open a terminal and change to the directory of the cloned repository
5. Run `ant`. It will take a few seconds to build, when the build finishes check for the message 'BUILD SUCCESSFUL' near the end of the output.
    - The default target is `dist`, which compiles the source code and packages all the artifacts for the SageTV Plugin Manager. The files are stored in the project directory, but they are not deployed anywhere.
6. Deploy the package to your SageTV server using `ant deploy-test-package.xml`.
7. Install 
