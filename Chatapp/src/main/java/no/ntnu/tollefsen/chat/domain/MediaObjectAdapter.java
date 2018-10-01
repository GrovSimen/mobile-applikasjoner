package no.ntnu.tollefsen.chat.domain;

import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.bind.adapter.JsonbAdapter;

/**
 *
 * @author mikael
 */
public class MediaObjectAdapter implements JsonbAdapter<List<MediaObject>, JsonArray> {
    @Override
    public JsonArray adaptToJson(List<MediaObject> mos) throws Exception {
        JsonArrayBuilder result = Json.createArrayBuilder();
        mos.forEach(mo -> result.add(mo.getId()));
        return result.build();
    }

    @Override
    public List<MediaObject> adaptFromJson(JsonArray mediaid) throws Exception {
        return null;
    }
}