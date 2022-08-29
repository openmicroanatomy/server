package fi.ylihallila.server.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.Util;
import io.javalin.plugin.json.ToJsonMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JsonMapper which removes any fields annotated with {@link Filters.VisibleToWriteOnly}
 * or {@link Filters.VisibleToReadOnly} from the resulting JSON.
 */
public class FilteredJsonMapper implements ToJsonMapper {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final User user;

    public FilteredJsonMapper(User user) {
        this.user = user;
    }

    @NotNull
    @Override
    public String map(@NotNull Object o) {
        ObjectMapper mapper = Util.getMapper();

        try {
            return mapper
                    .setFilterProvider(new SimpleFilterProvider().addFilter("ReadWriteFilter", new ReadWritePropertyFilter(user)))
                    .writeValueAsString(o);
        } catch (JsonProcessingException e) {
            logger.error("Error while parsing Json", e);
        }

        return "";
    }
}
