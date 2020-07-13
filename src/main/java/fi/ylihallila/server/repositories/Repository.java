package fi.ylihallila.server.repositories;

import fi.ylihallila.server.util.Constants;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Repository represents some data. All methods should return mutable objects, which can be modified & saved via
 * {@link #commit()}. All methods that modify the data should commit automatically, such as delete(T t); and insert(T t);
 *
 * @param <T> model type.
 */
public interface Repository<T> {

    Optional<T> getOne(Predicate<T> filter);

    List<T> getMany(Predicate<T> filter);

    Optional<T> getById(String id);

    List<T> list();

    void insert(T t);

    void update(T t);

    void delete(T t);

    void deleteById(String id);

    boolean contains(String id);

    void commit();

    void refresh();

    default String getBackupFile(String fileName) {
        return String.format(Constants.BACKUP_FILE_FORMAT, fileName, System.currentTimeMillis());
    }
}
