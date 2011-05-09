
public class StationDatabase
{
    private boolean remote = false;
    private String  server = null;
    private int     port   = -1;
    private File    file   = null;

    private Connection connection = null;

    public StationDatabase(File file) {
        remote = false;
        this.file = file;
    }

    public StationDatabase(String file) {
        remote = false;
        this.file = new File(file);
    }

    public StationDatabase(String server, int port, String type) {
        this.server = server;
        this.port   = port;
        this.type   = type;
    }
}
