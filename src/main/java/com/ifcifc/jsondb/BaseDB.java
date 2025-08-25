package com.ifcifc.jsondb;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Stream;

public class BaseDB<T extends BaseModel> {
    //Donde se almacenan los registros
    protected HashMap<UUID, T> db;

    //La clase del modelo que se esta trabajando
    protected Class<T> clazz;

    //Lock para evitar condiciones de carrera
    protected  SecureLock secureLock;

    public BaseDB(Class<T> clazz){
        this.clazz = clazz;
        this.secureLock = new SecureLock();
        this.db = new HashMap<>();
    }

    protected BaseDB(HashMap<UUID, T> db, Class<T> clazz) throws CloneNotSupportedException {
        this(clazz);
        //Clona todas las entidades
        db.forEach((key, value)->this.db.put(key, (T)value.clone()));
    }

    public T get(UUID id){
        return  this.secureLock.secureCall(()->db.getOrDefault(id, null));
    }

    public T update(T entity){
        return this.secureLock.secureCall(()->{
            if(!this.db.containsKey(entity.getId())){
                throw new RuntimeException("Esta entidad no existe en la db");
            }

            this.db.replace(entity.getId(), entity);
            return entity;
        });
    }

    //Obtiene una copia de las entidades
    public Stream<T> all() {
        return this.secureLock.secureCall(()-> this.db.values().stream());
    }

    //Crea una nueva endidad
    public T create(T entity) throws RuntimeException{
        return this.secureLock.secureCall(()->{
            try {
                //Genero una nueva instancia del modelo
                T instance = this.clazz.getDeclaredConstructor().newInstance();

                //Busca un id que no este en uso
                UUID id;
                do{
                    id = UUID.randomUUID();
                }while (this.db.containsKey(id));

                //Le paso el id a la entidad nueva
                Field idField = BaseModel.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(instance, id);

                //Copio los valores a la nueva entidad
                for (Field field : clazz.getDeclaredFields()) {
                    if ("id".equals(field.getName())) continue;
                    field.setAccessible(true);
                    field.set(instance, field.get(entity));
                }

                //Retorno la nueva entidad
                this.db.put(instance.getId(), instance);
                return instance;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    //Elimina una entidad
    public boolean delete(T entity){
        return this.secureLock.secureCall(()-> {
            if (!this.db.containsKey(entity.getId())) return false;
            this.db.remove(entity.getId());
            return true;
        });
    }

    //Devuelve la cantidad de elementos almacenados
    public int count(){
        return this.secureLock.secureCall(()->this.db.size());
    }

    //Genera una "transaccion"
    public boolean transaction(TransactionCallback<T> clk){
        return this.secureLock.secureCall(()->{
            try{
                //Crea una copia de la db
                BaseDB<T> transaction_db = new BaseDB<T>(this.db, this.clazz);
                //Ejecuta el callback
                boolean result = clk.transaction(transaction_db);
                //Si el resultado es true, aplica los cambios a la db
                if(result){
                    this.db.putAll(transaction_db.db);
                }
                return result;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        });
    }
}
