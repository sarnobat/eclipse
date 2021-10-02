import java.net.UnknownHostException;
import java.util.Date;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.QueryBuilder;

public class MongoDbCache {

	private static final String HOST = "192.168.1.2";
	private static final int PORT = 27017;

	private static final String VALUE = "value";
	private static final String ID = "_id";

	private static final String COLLECTION = "items";
	private static final String CACHE = "cache";

	public static void main(String[] args) throws UnknownHostException {
		
		System.out.println("MongoDbClient.main() exists = " + exists("Sridhar"));
		if (exists("Sridhar")) {
			delete("Sridhar");
			System.out.println("MongoDbClient.main() exists = " + exists("Sridhar"));
		}
		put("Sridhar", "Yurl object");
		System.out.println("MongoDbClient.main() exists = " + exists("Sridhar"));
		System.out.println("MongoDbClient.main() value = " + get("Sridhar"));

	}

	static boolean delete(String key) {
		MongoClient mongo;
		try {
			mongo = new MongoClient(HOST, PORT);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		DB db = mongo.getDB(CACHE);
		DBCollection table = db.getCollection(COLLECTION);
		BasicDBObject searchQuery = new BasicDBObject(ID, key);
		return table.remove(searchQuery).getN() > 0;
	}

	static boolean exists(String key) {
		MongoClient mongo;
		try {
			mongo = new MongoClient(HOST, PORT);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		DB db = mongo.getDB(CACHE);
		DBCollection table = db.getCollection(COLLECTION);
		BasicDBObject searchQuery = new BasicDBObject(ID, key);
		return table.find(searchQuery).size() > 0;
	}

	static void put(String key, String value) throws UnknownHostException {
		MongoClient mongo = new MongoClient(HOST, PORT);
		DB db = mongo.getDB(CACHE);
		DBCollection collection = db.getCollection(COLLECTION);
		BasicDBObject document = new BasicDBObject(ID, key);
		document.put(VALUE, value);
		collection.insert(document);
	}

	static String get(String key) {
		MongoClient mongo;
		try {
			mongo = new MongoClient(HOST, PORT);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		DB db = mongo.getDB(CACHE);
		DBCollection collection = db.getCollection(COLLECTION);
		BasicDBObject searchQuery = new BasicDBObject(ID, key);
		DBCursor cursor = collection.find(searchQuery);
		DBObject next = cursor.next();
		return (String) next.get(VALUE);
	}

}
