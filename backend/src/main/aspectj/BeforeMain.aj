import akka.viz.server.Server;

public aspect BeforeMain {
    private pointcut mainMethod () :
            execution(public static void main(String[]));

    before () : mainMethod() {
        Server.start();
    }

}