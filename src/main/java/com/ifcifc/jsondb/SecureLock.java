package com.ifcifc.jsondb;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/*
* Garantiza que solo un thread pueda ejecutar el código al mismo tiempo.
*/
//se podria reemplazar usando synchronized en lugar de usar esta clase
public class SecureLock {
    private final ReentrantLock lock;

    public SecureLock() {
         this.lock = new ReentrantLock();
    }

    /**
     * Ejecuta el callback de forma sincronizada.
     * No dejara ejecutarse almenos que se libere el lock
     * @param runnable Runnable ()=>{...}
     */
    public void secureRun(Runnable runnable){
        lock.lock();
        try {
            runnable.run();
        }finally {
            lock.unlock();
        }
    }

    /**
     * Ejecuta el callback de forma sincronizada.
     * No dejara ejecutarse almenos que se libere el lock
     * @param supplier función que retorna un valor
     * @return el valor retornado por el supplier
     */
    public <T> T secureCall(Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }
}
