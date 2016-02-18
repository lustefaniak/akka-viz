package akkaviz.serialization;

import upickle.Js;

public interface AkkaVizSerializer {
    boolean canSerialize(Object obj);

    Js.Value serialize(Object obj, SerializationContext context);
}
