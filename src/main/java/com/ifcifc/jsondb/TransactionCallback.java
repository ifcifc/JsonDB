package com.ifcifc.jsondb;

@FunctionalInterface
public interface TransactionCallback <T extends BaseModel> {
    boolean transaction(BaseDB<T> db);
}
