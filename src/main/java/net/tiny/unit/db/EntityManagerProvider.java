package net.tiny.unit.db;

import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

//Create a JPA EntityManager argument for test case
public class EntityManagerProvider implements ArgumentsProvider {

    private final static Logger LOGGER = Logger.getLogger(EntityManagerProvider.class.getName());

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        EntityManager em = getEntityManager(context);
        return Stream.of(
                Arguments.of(em)
        );
    }

    private EntityManager getEntityManager(ExtensionContext context) {
        JpaHelper helper = context.getStore(DatabaseExtension.NAMESPACE)
                .get(DatabaseExtension.getStoreKey(context, DatabaseExtension.StoreKeyType.JPA_CLASS), JpaHelper.class);
        if (helper == null) {
            LOGGER.warning("[JPA-UNIT] Check '@Database(jpa=true)'");
            return null;
        }
        return helper.getEntityManager();
    }


}
