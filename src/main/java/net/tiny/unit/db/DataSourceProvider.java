package net.tiny.unit.db;

import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;


//Create a SQL Datasource argument for embedded H2 database
public class DataSourceProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        DataSource dataSource = getDataSource(context);
        return Stream.of(
                Arguments.of(dataSource)
        );
    }

    private DataSource getDataSource(ExtensionContext context) {
        return context.getStore(DatabaseExtension.NAMESPACE)
                      .get(DatabaseExtension.getStoreKey(context, DatabaseExtension.StoreKeyType.DS_CLASS),
                              DatabaseExtension.EmbeddedDataSource.class);
    }
}
