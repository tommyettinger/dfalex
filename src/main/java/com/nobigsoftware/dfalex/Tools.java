package com.nobigsoftware.dfalex;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Created by Tommy Ettinger on 12/13/2016.
 */
public class Tools {
    public static final Json json = new Json(JsonWriter.OutputType.javascript);
    /*
    static {
        json.setSerializer(PackedTreeDfaPlaceholder.class, new Json.Serializer<PackedTreeDfaPlaceholder>() {
            @Override
            public void write(Json json, PackedTreeDfaPlaceholder object, Class knownType) {
                if(object == null)
                {
                    json.writeValue(null);
                    return;
                }
                json.writeValue(object.serializeToString());
            }

            @Override
            public PackedTreeDfaPlaceholder read(Json json, JsonValue jsonData, Class type) {
                if(jsonData == null || jsonData.isNull()) return null;
                String data = jsonData.asString();
                if(data == null || data.length() < 3) return null;
                return PackedTreeDfaPlaceholder.deserializeFromString(data);
            }
        });
    }
    */
    public static StringBuilder append(StringBuilder sb, int[] data)
    {
        if(data == null)
        {
            return sb.append('\t');
        }
        int t = data.length, len = t;
        sb.append((char)((t & 0x7ff)+32)).append((char)((t >>> 11 & 0x7ff)+32)).append((char)((t >>> 22 & 0x7ff)+32));
        for (int i = 0; i < len; i++) {
            sb.append((char)(((t = data[i]) & 0x7ff)+32)).append((char)((t >>> 11 & 0x7ff)+32)).append((char)((t >>> 22 & 0x7ff)+32));
        }
        return sb;
    }
    public static StringBuilder append(StringBuilder sb, int n)
    {
        return sb.append((char)((n & 0x7ff)+32)).append((char)((n >>> 11 & 0x7ff)+32)).append((char)((n >>> 22 & 0x7ff)+32));
    }
    public static int readInt(CharSequence text, int start)
    {
        return (text.charAt(start++) - 32) | (text.charAt(start++) - 32 << 11) | (text.charAt(start) - 32 << 22);
    }
    public static int[] readInts(CharSequence text, int start)
    {
        int len = text.length() - start;
        if(len < 3)
            return null;
        len /= 3;
        int size = readInt(text, start);
        int[] v = new int[size];
        for (int i = 0; i < size && i < len; i++) {
            v[i] = readInt(text, (start += 3));
        }
        return v;
    }
}
