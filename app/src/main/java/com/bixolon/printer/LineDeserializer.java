package com.bixolon.printer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class LineDeserializer  implements JsonDeserializer<Line>
{
    @Override
    public Line deserialize(JsonElement json, Type typeOfT,
                                JsonDeserializationContext context) throws JsonParseException
    {
        return new Line();
        /* JsonObject jsonObject = json.getAsJsonObject();

        return new Line (
                jsonObject.get("Type").getAsString(),
                jsonObject.get("Value").getAsString(),
                jsonObject.get("Alignment").getAsInt(),
                jsonObject.get("TextSizeHeight").getAsInt(),
                jsonObject.get("TextSizeWidth").getAsInt(),
                jsonObject.get("Attribute").getAsInt(),
                jsonObject.get("Symbology").getAsInt(),
                jsonObject.get("TextPosition").getAsInt());*/
    }
}
