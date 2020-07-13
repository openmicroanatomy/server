package fi.ylihallila.server.repositories;

import fi.ylihallila.server.util.Constants;

import java.util.List;
import java.util.Optional;

public interface Repository<T> {

    Optional<T> getById(String id);

    List<T> list();

    void insert(T t);

    void update(T t);

    void delete(T t);

    void deleteById(String id);

    boolean contains(String id);

    void commit();

    default String getBackupFile(String fileName) {
        return String.format(Constants.BACKUP_FILE_FORMAT, fileName, System.currentTimeMillis());
    }
}
