//
//package tools;
//
//
//
//import database.DatabaseConnection;
//import database.DatabaseConnection.ThreadLocalConnection;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.Set;
//import org.apache.mina.util.ConcurrentHashSet;
//
///**
// * 
// * To optimize the server's database connectivity by initializing (and operating) with a connection pool instead of a singleton connection
// * 
// * @author Soulfist
// * @idea JvlapleX 
// */
//
//public final class ConnectionManager implements java.io.Serializable {
//    
//    private static transient Set<ThreadLocalConnection> conPool;
//    private static final ConnectionManager instance = new ConnectionManager();
//    
//    //change initial loaded connections & cache check settings here
//    private static final int DEFAULT_CONNECTION_COUNT = 10,
//                             SAFE_CACHING_LIMIT = 50;
//    
//    
//    /* 
//     * This class should never been instantiated, unnecessary and dangerous to have multiple copies
//     */ 
//    private ConnectionManager() {
//        
//    }
//    
//    public static ConnectionManager getInstance() {
//        return instance;
//    }
//    
//    /*
//     * Initializes on first load-up from JVM, and not ever again
//     */
//    static {
//        conPool = new ConcurrentHashSet<ThreadLocalConnection>();
//        for (int i = 0; i < DEFAULT_CONNECTION_COUNT; conPool.add(DatabaseConnection.getNewConnection()), i++);
//    }
//    
//    /*
//     * Searches the pool for an available connection
//     * Returns either an instance from the pool, or a new connection.
//     * If a new connection is returned, it's immediately added to the cache.
//     * Resets the timeout factor of the connection that is returned so it doesn't get implicitly removed
//     */
//    public Connection getNextAvailableConnection() throws SQLException {
//        ensureCacheOverflowSafety();
//        ThreadLocalConnection ret = null;
//        for (ThreadLocalConnection c : conPool) {
//            //Gives a 1 second wait time, checking viability (accessed)
//            if (!c.get().isValid(1)) continue;
//            ret = c;
//        }
//        if (ret == null) {
//            ret = DatabaseConnection.getNewConnection();
//            conPool.add(ret);
//        }
//        ret.resetTimeout();
//        return ret.get();
//    }
//    
//    /*
//     * Closes and removes Connection objects based on their unique hash code
//     * Call this once upon server shutdown
//     * Do not substitute this for closing other chained connections (PreparedStatement, ResultSet)
//     */
//    public void emptyConnections() throws SQLException {
//        for (ThreadLocalConnection c : conPool) {
//            //indiscriminate removal of all database transaction streams
//            c.get().close();
//            conPool.remove(c);
//            c = null;
//            
//        }
//    }
//    
//    /*
//     * Checks the possibilities of timed out, unnecessary, or invalid connections;
//     * If the pool's size grows too massive, it will attempt to remove all unneeded instances,
//     * until a reasonable size is reached
//     */
//    private void ensureCacheOverflowSafety() throws SQLException {
//        for (ThreadLocalConnection con : conPool) {
//            //assess viability of each connection
//            if ((con.get().isClosed() || !con.get().isValid(2)) && conPool.size() >= SAFE_CACHING_LIMIT) {
//                con.get().close();
//                conPool.remove(con);
//            }
//            if (conPool.size() < SAFE_CACHING_LIMIT) break;
//        }
//    }
//}
