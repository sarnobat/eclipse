import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class HierarchyConstruct {

	public static void main(String[] args) throws IOException {
		List<String> lines = IOUtils.readLines(System.in);
		String in = String.join(" ", lines);
		JsonObject json = jsonFromString(in);

		Map<String, JSONObject> out = createNodeMap(json.getJsonObject("nodes").entrySet());
		{

			Set<Entry<String, JsonValue>> l = json.getJsonObject("edges").entrySet();
			for (Entry<String, JsonValue> e : l) {
				JSONObject child = out.get(e.getKey());
				String value = ((JsonString) e.getValue()).getString();
				// TODO: hack
				if (e.getKey().equals(value)) {
					continue;
				}
				if (!out.containsKey(value)) {
					System.err.println("HierarchyConstruct.main() - Couldn't find a node with key "
							+ value);
					continue;
				}
				JSONObject parent = checkNotNull(out.get(value));
				parent.getJSONArray("children").put(new JSONObject(child.toString()));
			}
		}

		System.out.println(out.get("/").toString(2));
	}

	private static Map<String, JSONObject> createNodeMap(Set<Entry<String, JsonValue>> nodes) {
		Map<String, JSONObject> m = new HashMap<String, JSONObject>();
		for (Entry<String, JsonValue> e : nodes) {
			m.put(e.getKey(), new JSONObject(e.getValue().toString()));
		}
		return m;
	}

	private static JsonObject jsonFromString(String jsonObjectStr) {

		JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
		JsonObject object = jsonReader.readObject();
		jsonReader.close();

		return object;
	}
}
