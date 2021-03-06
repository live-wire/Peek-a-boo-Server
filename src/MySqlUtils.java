import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Hello world!
 *
 */

public class MySqlUtils {

    public static Connection connect = null;
    public static String sqlText = "";

    public static void connection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("Worked!");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void ConnectionToMySql() {
        connection();
        String host = "jdbc:mysql://localhost:3306/peekaboo";
        String username = "root";
        String password = "happyhours";
        try {
            connect = (Connection) DriverManager.getConnection(host, username,
                    password);
            System.out.println("MySQL Works!!");
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void QueryDB(String query) throws SQLException {
        Statement statement = null;

        try {

            statement = (Statement) connect.createStatement();
            ResultSet result = null;
            try {
                result = statement.executeQuery(query);

                while (result.next()) {

                    int name = result.getInt("route_id");
                    String age = result.getString("route_name");
                    sqlText = sqlText + name + age + "\n";
                    System.out.println(name + " " + age);

                }
            } finally {
                if (result != null)
                    result.close();
            }
        } finally {
            if (statement != null)
                statement.close();
        }

    }

    public static void handleAddressUpdate(JSONObject obj) throws SQLException {
        String busNumber = null, busName = null, latitude = null, longitude = null, driverContact = null, lastUpdated = null;
        ;
        busNumber = (String) obj.get("BusNumber");
        busName = (String) obj.get("BusName");
        latitude = (String) obj.get("Latitude");
        longitude = (String) obj.get("Longitude");
        driverContact = (String) obj.get("Contact");
        lastUpdated = (String) obj.get("LastUpdated");
        Statement statement = null;
        if (busNumber != null && busName != null) {
            try {

                statement = (Statement) connect.createStatement();
                int result = 0;

                // String sql
                // ="INSERT INTO bus (number_plate,latitude,longitude,contact) VALUES ('"+busNumber+"',"+latitude+","+longitude+",'"+driverContact+"');";
                String sql = "UPDATE bus SET latitude=" + latitude
                        + ",longitude=" + longitude + ",bus_name='" + busName
                        + "',last_update='" + lastUpdated
                        + "' WHERE number_plate='" + busNumber + "';";
                String sqlExhaustivePaths = "INSERT INTO path_exhaustive (number_plate,latitude,longitude,last_update) VALUES ('"
                        + busNumber
                        + "',"
                        + latitude
                        + ","
                        + longitude
                        + ",'"
                        + lastUpdated + "');";
                result = statement.executeUpdate(sql);
                result = statement.executeUpdate(sqlExhaustivePaths);

                // while(result.next()) {
                //
                // int name = result.getInt("route_id");
                // String age = result.getString("route_name");
                // sqlText=sqlText+name+age+"\n";
                // System.out.println(name+" "+age);
                //
                // }
            } catch (SQLException se) {
                // Handle errors for JDBC
                se.printStackTrace();
            } catch (Exception e) {
                // Handle errors for Class.forName
                e.printStackTrace();
            } finally {
                if (statement != null) {
                }
            }// end try
        }

    }

    public static void handleLocationUpdate(JSONObject obj) throws SQLException {
    	
        String username = (String)obj.get("Username");
        Statement statement = null;
        if (username != null) {
            try {
                statement = (Statement) connect.createStatement();
                String sql = "UPDATE Users SET latitude=" + obj.get("Latitude")
                        + ",longitude=" + obj.get("Longitude") + ",last_updated='" + obj.get("LastUpdated")
                        + "' WHERE user_name='" + username + "';";

                statement.executeUpdate(sql);
            } catch (SQLException se) {
                se.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }

    }

    public static void handleAppOpen(JSONObject obj) throws SQLException {
        String username = (String) obj.get("Username");
        sqlText = "{\"ResponseType\":\"AppOpen\",\"Response\":{\"Friends\":[";
        Statement statement = null, statement1 = null;
        if (username != null && username != "") {
            try {
                String sql = "";
                ResultSet result = null, resultNames = null;
                try {
                    sql = "SELECT friend_name, expire_time FROM contacts "
                        + "WHERE user_name='" + username + "';";
                    statement = (Statement) connect.createStatement();
                    result = statement.executeQuery(sql);
                    while (result.next()) {
                        sql = "SELECT user_name, first_name, last_name FROM Users "
                                + "WHERE user_name='" + result.getString("friend_name") + "';";
                        statement1 = (Statement) connect.createStatement();
                        resultNames = statement1.executeQuery(sql);
                        while (resultNames.next()) {
                            sqlText = sqlText + "{\"Username\":\"" + resultNames.getString("user_name") + "\",\"FirstName\":\"" + resultNames.getString("first_name") + "\",\"LastName\":\"" + resultNames.getString("last_name") + "\",\"LocationSharedTill\":\"" + result.getString("expire_time") + "\"},";
                        }
                    }
                    sqlText = sqlText.substring(0, sqlText.length()-1);
                    sqlText = sqlText + "]}}";
                } catch (SQLException se) {
                    se.printStackTrace();
                } finally {
                    if (result != null)
                        result.close();
                }
            } finally {
                if (statement != null)
                    statement.close();
            }
        }

    }

    public static void handleTrackAll(JSONObject obj) throws SQLException {
        String username = (String) obj.get("Username");
        sqlText = "{\"ResponseType\":\"TrackAll\",\"Response\":{\"Friends\":[";
        Statement statement = null, statement1 = null;
        if (username != null && username != "") {
            try {
                String sql = "";
                ResultSet result = null, resultNames = null;
                try {
                    sql = "SELECT friend_name, expire_time FROM contacts "
                            + "WHERE user_name='" + username + "';";
                    statement = (Statement) connect.createStatement();
                    result = statement.executeQuery(sql);
                    while (result.next()) {
                        sql = "SELECT user_name, first_name, last_name, latitude, longitude, last_updated FROM Users "
                                + "WHERE user_name='" + result.getString("friend_name") + "';";
                        statement1 = (Statement) connect.createStatement();
                        resultNames = statement1.executeQuery(sql);
                        while (resultNames.next()) {
                            sqlText = sqlText + "{\"Username\":\"" + resultNames.getString("user_name") + "\",\"FirstName\":\"" + resultNames.getString("first_name") + "\",\"LastName\":\"" + resultNames.getString("last_name") + "\", \"Latitude\":\"" + resultNames.getDouble("latitude")+ "\", \"Longitude\":\"" + resultNames.getDouble("longitude")+ "\", \"LastUpdated\":\"" + resultNames.getString("last_updated")+ "\", \"LocationSharedTill\":\"" + result.getString("expire_time") + "\"},";
                        }
                    }
                    sqlText = sqlText.substring(0, sqlText.length()-1);
                    sqlText = sqlText + "]}}";
                } catch (SQLException se) {
                    se.printStackTrace();
                } finally {
                    if (result != null)
                        result.close();
                }
            } finally {
                if (statement != null)
                    statement.close();
            }
        }

    }
    public static void handleUpdateRegId(JSONObject obj) throws SQLException {
    	String username = (String) obj.get("Username");
    	String regId = (String) obj.get("RegId");
    	Statement statement = null;
    	if (username != null) {
            try {
                statement = (Statement) connect.createStatement();
                String sql = "UPDATE Users SET reg_id='" + regId
                        + "' WHERE user_name='" + username + "';";

                statement.executeUpdate(sql);
            } catch (SQLException se) {
                se.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
    	
    	
    	
    	
    }

    public static void handleGetLocation(JSONObject obj) throws SQLException {
        String username = (String) obj.get("Friend");
        Statement statement = null;
        double lat=0.0, lon=0.0;
        String lastUpd="";
        if (username != null && username != "") {
            try {
                String sql = "";
                ResultSet result = null;
                try {
                    sql = "SELECT latitude, longitude, last_updated FROM Users "
                            + "WHERE user_name='" + username + "';";
                    statement = (Statement) connect.createStatement();
                    result = statement.executeQuery(sql);
                    while (result.next()) {
                        lat = result.getDouble("latitude");
                        lon = result.getDouble("longitude");
                        lastUpd = result.getString("last_updated");
                    }
                    sqlText = "{\"Response\":{\"Friend\":\"" + username +"\",\"Latitude\":\"" + lat + "\",\"Longitude\":\"" + lon + "\",\"LastUpdated\":\"" + lastUpd + "\"},\"ResponseType\":\"GetLocation\"}";
                    //System.out.println(lat + " " + lon);
                } finally {
                    if (result != null)
                        result.close();
                }
            } finally {
                if (statement != null)
                    statement.close();
            }
        }
    }

    public static void handleLiveTracking(JSONObject obj) throws SQLException, ClientProtocolException, IOException 
    {
    	
    	String username = (String) obj.get("Username");
    	String friend = (String) obj.get("Friend");
    	/*Fetch from DB if location updates allowed , reID */
    	MySqlUtils.triggerLocationUpdates("blah","blah2","blah3");
    	
    	
    }
    
    public static void triggerLocationUpdates(String regId,String uname,String time) throws ClientProtocolException, IOException
    {
    	String url = "https://android.googleapis.com/gcm/send";
    	 
    	HttpClient client = HttpClientBuilder.create().build();
    	HttpPost post = new HttpPost(url);
    	
    	
    	// add headers
    	post.setHeader("Content-Type","application/json");
    	post.setHeader("Authorization","key=AIzaSyDvk8_GLWTn-u_GrlvBkMHhkPR8XLtabEg");
    	

    	JSONArray jsonArr = new JSONArray();
    	
    	//Hard Coded Stuff
    	regId="APA91bEg8w7wjckdq3Bzwo4x5iaXPMqmw62C_sJ23NxzoVksSnqwUiBpU5Axplj2Ca33ttBRsU-gAvI7ikBJPeoMe-Qt3eoJLQrhiK5Sb_Z63PLee9D6tqKMgGWVoHNYJiNwva-L983WhdGRd2qXSrJ-cgdTK21fxg";
    	uname = "gulati.karishma";
    	time="blahTIME";
    	
    	jsonArr.add(regId);
    	JSONObject data = new JSONObject();
    	data.put("uname",uname);
    	data.put("time",time);
    	data.put("type","trigger");
    	
    	JSONObject jsonObj = new JSONObject();
    	jsonObj.put("registration_ids", jsonArr);
    	jsonObj.put("data", data);
    	
    	StringEntity entity = new StringEntity(jsonObj.toString(),"UTF-8");
    	
    	post.setEntity(entity);
    	HttpResponse response = client.execute(post);
    	System.out.println("Response Code : " 
                    + response.getStatusLine().getStatusCode());
     
    	BufferedReader rd = new BufferedReader(
    	        new InputStreamReader(response.getEntity().getContent()));
     
    	StringBuffer result = new StringBuffer();
    	String line = "";
    	while ((line = rd.readLine()) != null) {
    		result.append(line);
    	}
    }
    
    public static void main(String[] args) throws IOException, SQLException {
        ConnectionToMySql();
        InetSocketAddress addr = new InetSocketAddress(8080);
        HttpServer server = HttpServer.create(addr, 0);

        server.createContext("/", new MyHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Server is listening on port 8080");
    }
}

class MyHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equalsIgnoreCase("POST")) {
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set("Content-Type", "application/JSON");
            exchange.sendResponseHeaders(200, 0);

            OutputStream responseBody = exchange.getResponseBody();
            Headers requestHeaders = exchange.getRequestHeaders();
            InputStream input = exchange.getRequestBody();
            String json = MyHandler.convertStreamToString(input);

            Set<String> keySet = requestHeaders.keySet();
            // Iterator<String> iter = keySet.iterator();
            // while (iter.hasNext()) {
            // String key = iter.next();
            // List values = requestHeaders.get(key);
            // String s = key + " = " + values.toString() + "\n";
            // responseBody.write(s.getBytes());
            // }
            try {

                JSONParser parser = new JSONParser();
                Object request = null;
                String type = "";
                Object category = null;
                Object resultObject = parser.parse(json);
                if (resultObject instanceof JSONArray) {
                    JSONArray array = (JSONArray) resultObject;
                    for (Object object : array) {
                        JSONObject obj = (JSONObject) object;
                        System.out.println(obj.get("Request"));
                        System.out.println(obj.get("Category"));
                    }

                } else if (resultObject instanceof JSONObject) {
                    JSONObject obj = (JSONObject) resultObject;
                    if (obj != null) {
                        type = (String) obj.get("RequestType");
                        if (type != null && type.equals("AppOpen")) {
                            request = obj.get("Request");
                            if (request != null
                                    && request instanceof JSONObject) {
                                obj = (JSONObject) request;
                                MySqlUtils.handleAppOpen(obj);
                            }
                        }else if (type != null && type.equals("GetLocation")) {
                            request = obj.get("Request");
                            if (request != null
                                    && request instanceof JSONObject) {
                                obj = (JSONObject) request;
                                MySqlUtils.handleGetLocation(obj);
                            }
                        }else if (type != null && type.equals("LocationUpdate")) {
                            request = obj.get("Request");
                            if (request != null
                                    && request instanceof JSONObject) {
                                obj = (JSONObject) request;
                                MySqlUtils.handleLocationUpdate(obj);
                            }
                        }else if (type != null && type.equals("TrackAll")) {
                            request = obj.get("Request");
                            if (request != null
                                    && request instanceof JSONObject) {
                                obj = (JSONObject) request;
                                MySqlUtils.handleTrackAll(obj);
                            }
                        }else if(type !=null && type.equals("UpdateRegId")){
                        	request = obj.get("Request");
                            if (request != null
                                    && request instanceof JSONObject) {
                                obj = (JSONObject) request;
                                MySqlUtils.handleUpdateRegId(obj);
                            }
                        }
                        else if(type !=null && type.equals("LiveTracking")){
                        	request = obj.get("Request");
                            if (request != null
                                    && request instanceof JSONObject) {
                                obj = (JSONObject) request;
                                MySqlUtils.handleLiveTracking(obj);
                            }
                        }
                    }
                }
            }catch (SQLException e){
                e.printStackTrace();
            }catch (ParseException e){
                e.printStackTrace();
            }
            responseBody.write(MySqlUtils.sqlText.getBytes());
            System.out.println(json);
            responseBody.close();
        }

    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
//key=AIzaSyDvk8_GLWTn-u_GrlvBkMHhkPR8XLtabEg
//Project ID - 214619967549
