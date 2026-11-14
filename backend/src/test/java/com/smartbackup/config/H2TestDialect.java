package com.smartbackup.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.EnumJdbcType;

public class H2TestDialect extends H2Dialect {

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        typeContributions.getTypeConfiguration()
                .getJdbcTypeRegistry()
                .addDescriptor(SqlTypes.NAMED_ENUM, EnumJdbcType.INSTANCE);
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        if (sqlTypeCode == SqlTypes.NAMED_ENUM) {
            return "varchar(255)";
        }
        return super.columnType(sqlTypeCode);
    }
}
