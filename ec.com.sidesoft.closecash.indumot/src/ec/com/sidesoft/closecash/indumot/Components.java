package ec.com.sidesoft.closecash.indumot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Components { 
  
    /**
     * Uso controlado de modo admin para operación técnica interna.
     * Contexto restaurado inmediatamente.
     */  
    public void executeAsAdminForInternalProcess() {  
         
        try { 
           OBContext.setAdminMode();   
           OBContext.setAdminMode();    
        } catch(Exception e){
            throw new OBException("Error ejecutando lógica administrativa", e);
        }finally {
            OBContext.restorePreviousMode();   
        }  
    } 

    public int multiplicarAux(int a, int b) {  
        return a * b;
    }


}
