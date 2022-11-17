Inside this zip, there are src code folder, screenshots folder, Docker folder, and an executable summary PDF.

Before running, it is recommended to see the testing screenshots to have an overview.
Testing screenshots are done for a previous beta-version, and it is only for overview and reference.

For inputting commands, please strictly follow this README.

There are three ways to run/test the code:

1. Put src folder's code into a java IDE, edit configurations for each server and client:

   `a. Start a new server: ServerController <port number> <host name> <coordinator hostname>
   e.g. 8885 localhost localhost`

   `b. Restart a server: ServerController <port number> <host name> <live peer server port number> <live peer server host name> <coordinator-hostname>
   e.g. 8885 localhost 8888 localhost localhost`

   `c. Start a client: ClientController <port number> <IP/hostname> e.g. 8885 localhost`

2. Use command line/terminal to compile src code, run CoordinatorController, ServerController and ClientController respectively with arguments:

- cd to the src folder, compile all the files first by `javac server/*.java client/*.java coordinator/*.java utils/*.java`
- To run the coordinator, simply run it `java coordinator.CoordinatorController`
- To run a new server, run `java server.ServerController 8885 localhost localhost`
- To restart a server, run `java server.ServerController 8885 localhost 8888 localhost localhost`
- To run a client, run `java client.ClientController 8885 localhost`

3. In docker folder, use Dockerfile and shell files.
