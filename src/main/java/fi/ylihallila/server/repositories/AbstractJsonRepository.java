package fi.ylihallila.server.repositories;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class AbstractJsonRepository<T> implements IRepository<T> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Path jsonPath;
    private List<T> data;

    public AbstractJsonRepository(final Path jsonPath, JavaType type) {

        try {
            this.jsonPath = jsonPath;
            this.data = mapper.readValue(jsonPath.toFile(), type);
        } catch (IOException e) {
            this.jsonPath = null;
            this.data = Collections.emptyList();
            logger.error("Error when constructing " + getClass().getName() + " repository", e);
        }
    }

    public List<T> getData() {
        return data;
    }

    @Override
    public List<T> list() {
        return new ArrayList<>(data);
    }

    @Override
    public void insert(T t) {
        this.data.add(t);
        commit();
    }

    @Override
    public void delete(T t) {
        this.data.remove(t);
        commit();
    }

    @Override
    public void deleteById(String id) {
        Optional<T> t = getById(id);

        t.ifPresent(this::delete);
    }

    @Override
    public void commit() {
        try {
            Files.write(jsonPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.data));
            Files.copy(jsonPath, Path.of(getBackupFile(jsonPath.getFileName().toString())));
        } catch (IOException e) {
            logger.error("Error while saving JSON data file", e);
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }
}
